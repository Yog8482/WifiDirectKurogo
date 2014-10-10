package jp.ac.tuat.cs.wifidirectkurogo;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Semaphore;

import jp.ac.tuat.cs.wifidirectkurogo.connection.TransferListener;
import jp.ac.tuat.cs.wifidirectkurogo.message.BeginFileContent;
import jp.ac.tuat.cs.wifidirectkurogo.message.EndFileContent;
import jp.ac.tuat.cs.wifidirectkurogo.message.IntentContent;
import jp.ac.tuat.cs.wifidirectkurogo.message.Message;
import jp.ac.tuat.cs.wifidirectkurogo.message.MinimalMessage;
import jp.ac.tuat.cs.wifidirectkurogo.message.PartedFileContent;
import jp.ac.tuat.cs.wifidirectkurogo.peer.Peer;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class WifiDirectManager {
	private final static String TAG = "WifiDirectManager";
	private final static int PEERS_CHANGED_MSG = 1;
	private final static int MESSAGE_RECEIVED_MSG = 2;
	private final static int COMEIN_MSG = 3;
	private final static int GOOUT_MSG = 4;

	private IWifiDirectService service;
	private boolean isBound;

	private WifiDirectServiceListener listener;

	private ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			Log.d(TAG, "onServiceConnected");
			service = IWifiDirectService.Stub.asInterface(binder);
			try {
				boolean isRunning = service.isRunning();
				Log.d(TAG, "connected isRunning = " + isRunning);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			try {
				service.registerCallback(callback);
				Log.d(TAG, "service.registerCallback");
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			service = null;
			Log.d(TAG, "disconnected");
		}
	};

	// 正直，IWifiDirectCallback の中で直接リスナーを呼び出して，
	// 各リスナーの中でUIを操作する場合は runOnUiThread するだけでもいいが，
	// 公式サンプルはこのように実装しているし，実際こっちの方が低コスト＆問題が起きにくいので
	// この方式にする．
	// 起きうる問題については次のページを参照
	// http://visible-true.blogspot.jp/2011/11/activityrunonuithreadrunnable.html
	private Handler myHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case PEERS_CHANGED_MSG:
				List<Peer> peers = (List<Peer>) msg.obj;
				Log.d(TAG, "PEERS_CHANGED_MSG");
				listener.onPeersChanged(peers);
				break;
			case MESSAGE_RECEIVED_MSG:
				Serializable object = (Serializable) msg.obj;
				listener.onDataReceived(object);
				break;
			case COMEIN_MSG: {
				MinimalMessage mes = (MinimalMessage) msg.obj;
				Log.d(TAG, "comein " + mes.toString());
				listener.comein(mes);
				break;
			}
			case GOOUT_MSG: {
				MinimalMessage mes = (MinimalMessage) msg.obj;
				Log.d(TAG, "goout " + mes.toString());
				listener.goout(mes);
				break;
			}
			default:
				super.handleMessage(msg);
			}
		}
	};

	private IWifiDirectCallback callback = new IWifiDirectCallback.Stub() {
		@Override
		public void onPeersChanged(List<Peer> peers) throws RemoteException {
			Log.d(TAG, "IWifiDirectCallback peersChanged");
			if (listener != null) {
				myHandler.sendMessage(myHandler.obtainMessage(PEERS_CHANGED_MSG, peers));
			}
		}

		@Override
		public void onMessageReceived(Message message) throws RemoteException {
			Log.d(TAG, "IWifiDirectCallback peersChanged");
			if (listener != null) {
				myHandler.sendMessage(myHandler.obtainMessage(MESSAGE_RECEIVED_MSG, message.getContent()));
			}
		}

		@Override
		public void comein(MinimalMessage minimalMessage) throws RemoteException {
			if (listener != null) {
				myHandler.sendMessage(myHandler.obtainMessage(COMEIN_MSG, minimalMessage));
			}
		}

		@Override
		public void goout(MinimalMessage minimalMessage) throws RemoteException {
			if (listener != null) {
				myHandler.sendMessage(myHandler.obtainMessage(GOOUT_MSG, minimalMessage));
			}
		}

	};

	public WifiDirectManager() {
		isBound = false;
	}

	/**
	 * WifiDirectService を開始する．このメソッドを実行したものは責任を持ってサービスを終了する．
	 * シングルトンなので，複数回呼び出してもシステムには常に1つのWifiDirectServiceが存在する． 
	 * @param context
	 */
	public void start(Context context) {
		Intent intent = new Intent(context, WifiDirectService.class);
		context.startService(intent);
	}

	
	/**
	 * 起動中のWifiDirectService と接続する．稼動している WifiDirectService がなかったら新たに起動する．
	 * @param context
	 */
	public void bind(Context context) {
		Intent intent = new Intent(IWifiDirectService.class.getName());
		// シングルトンなので何回呼んでも大丈夫
		context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
		isBound = true;
		Toast.makeText(context, "WiFiDirectKurogoをバインドしました", Toast.LENGTH_SHORT).show();

	}

	/**
	 * WifiDirectService との接続を切断する．
	 * @param context
	 */
	public void unbind(Context context) {
		if (isBound) {
			if (service != null) {
				try {
					service.unregisterCallback(callback);
					Log.d(TAG, "service.unregisterCallback");
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			try {
				context.unbindService(serviceConnection);
				Toast.makeText(context, "WiFiDirectKurogoをアンバインドしました", Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
			}
			isBound = false;
		}
	}

	/**
	 * 稼働中の WifiDirectService を終了する
	 * @param context
	 */
	public void stop(Context context) {
		Intent intent = new Intent(context, WifiDirectService.class);
		context.stopService(intent);
	}

	/**
	 * 稼働中の WifiDirectService を強制的に終了する
	 */
	public void kill() {
		if (service != null) {
			try {
				int pid = service.getPid();
				Process.killProcess(pid);
			} catch (RemoteException e) {
			}
		}
	}
	
	public boolean isRunning(){
		if(service == null){
			return false;
		}
		try {
			return service.isRunning();
		} catch (RemoteException e) {
		}
		return false;
	}
	

	/**
	 * object をAvailableなピア全てに送信する
	 * @param object
	 * @return 送信の成功・失敗を返す
	 */
	public boolean broadcast(Serializable object) {
		return broadcast(object, true, null);
	}

	public boolean broadcastUDP(Serializable object) {
		return broadcast(object, false, null);
	}

	public boolean broadcast(Serializable object, TransferListener listener) {
		return broadcast(object, true, listener);
	}

	public boolean broadcastUDP(Serializable object, TransferListener listener) {
		return broadcast(object, false, listener);
	}

	public boolean broadcast(Serializable object, boolean isTCP, TransferListener listener) {
		return send(object, null, isTCP, listener);
	}

	/**
	 * 指定された macAddress に object を送信する
	 * @param object
	 * @param macAddress
	 * @return 送信の成功・失敗を返す
	 */
	public boolean send(Serializable object, String macAddress) {
		return send(object, macAddress, true, null);
	}

	public boolean sendUDP(Serializable object, String macAddress) {
		return send(object, macAddress, false, null);
	}

	public boolean send(Serializable object, String macAddress, TransferListener listener) {
		return send(object, macAddress, true, listener);
	}

	public boolean sendUDP(Serializable object, String macAddress, TransferListener listener) {
		return send(object, macAddress, false, listener);
	}

	public boolean send(Serializable object, String macAddress, boolean isTCP, final TransferListener listener) {
		if (object == null) {
			return false;
		}
		Message message = new Message(object);
		if (service != null) {
			try {
				service.send(message, macAddress, isTCP, new ITransferCallback.Stub() {
					@Override
					public void onProgress(int value) throws RemoteException {
						if (listener != null)
							listener.onProgress(value);
					}

					@Override
					public void onCompleted() throws RemoteException {
						if (listener != null)
							listener.onCompleted();
					}

					@Override
					public void onCancelled() throws RemoteException {
						if (listener != null)
							listener.onCancelled();
					}
				});
				return true;
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * intent を接続中の端末全てに送信 TCPのみ用意 intent-filter で外から受け取った intent
	 * をさらに外部に転送する場合は， 無限ループを避けるために componentName で呼び出し元クラスを指定する
	 * 
	 * @param context
	 * @param intent
	 * @param componentName
	 * @return
	 */
	public boolean broadcast(Context context, Intent intent, ComponentName componentName) {
		return broadcast(context, intent, componentName, true, null);
	}

	public boolean broadcastUDP(Context context, Intent intent, ComponentName componentName) {
		return broadcast(context, intent, componentName, false, null);
	}

	public boolean broadcast(Context context, Intent intent, ComponentName componentName, TransferListener listener) {
		return broadcast(context, intent, componentName, true, listener);
	}

	public boolean broadcastUDP(Context context, Intent intent, ComponentName componentName, TransferListener listener) {
		return broadcast(context, intent, componentName, false, listener);
	}

	public boolean broadcast(Context context, Intent intent, ComponentName componentName, boolean isTCP,
			TransferListener listener) {
		return send(context, intent, componentName, null, isTCP, listener);
	}

	public boolean send(Context context, Intent intent, ComponentName componentName, String macAddress) {
		return send(context, intent, componentName, macAddress, true, null);
	}

	public boolean sendUDP(Context context, Intent intent, ComponentName componentName, String macAddress) {
		return send(context, intent, componentName, macAddress, false, null);
	}

	public boolean send(Context context, Intent intent, ComponentName componentName, String macAddress,
			TransferListener listener) {
		return send(context, intent, componentName, macAddress, true, listener);
	}

	public boolean sendUDP(Context context, Intent intent, ComponentName componentName, String macAddress,
			TransferListener listener) {
		return send(context, intent, componentName, macAddress, false, listener);
	}

	public boolean send(Context context, Intent intent, ComponentName componentName, final String macAddress,
			final boolean isTCP, final TransferListener listener) {
		if (intent == null) {
			return false;
		}

		final IntentContent content = new IntentContent(intent, componentName);

		TransferListener fileTransferListener = new TransferListener() {
			@Override
			public void onProgress(int value) {

			}

			@Override
			public void onCompleted() {
				// ファイルを転送し終えてから IntentContent を送る
				send(content, macAddress, isTCP, listener);
			}

			@Override
			public void onCancelled() {
				listener.onCancelled();
			}
		};

		if (content.getDataUri() != null && content.getExtraStreamUri() != null) {
			// TODO: listenerに未対応パターンです．ごめんなさい．
			send(context, Uri.parse(content.getDataUri()), macAddress, isTCP, null);
			send(context, Uri.parse(content.getExtraStreamUri()), macAddress, isTCP, fileTransferListener);
		}
		// ローカルに参照しているファイルがあれば転送
		if (content.getDataUri() != null && content.getExtraStreamUri() == null) {
			send(context, Uri.parse(content.getDataUri()), macAddress, isTCP, fileTransferListener);
		}
		if (content.getDataUri() == null && content.getExtraStreamUri() != null) {
			send(context, Uri.parse(content.getExtraStreamUri()), macAddress, isTCP, fileTransferListener);
		}
		// ローカルに参照しているファイルがない
		if (content.getDataUri() == null && content.getExtraStreamUri() == null) {
			send(content, macAddress, isTCP, listener);
		}

		return true;
	}

	/**
	 * intent を接続中の端末全てに送信する（二刀流のように外から受け取った intent をさらに外部に転送しない場合）
	 * 
	 * @param context
	 * @param intent
	 * @return
	 */
	public boolean broadcast(Context context, Intent intent) {
		return broadcast(context, intent, null, true, null);
	}

	public boolean broadcastUDP(Context context, Intent intent) {
		return broadcast(context, intent, null, false, null);
	}

	public boolean broadcast(Context context, Intent intent, TransferListener listener) {
		return broadcast(context, intent, null, true, listener);
	}

	public boolean broadcastUDP(Context context, Intent intent, TransferListener listener) {
		return broadcast(context, intent, null, false, listener);
	}

	public boolean broadcast(Context context, Intent intent, boolean isTCP, TransferListener listener) {
		return broadcast(context, intent, null, isTCP, listener);
	}

	public boolean send(Context context, Intent intent, String macAddress) {
		return send(context, intent, null, macAddress, true, null);
	}

	public boolean sendUDP(Context context, Intent intent, String macAddress) {
		return send(context, intent, null, macAddress, false, null);
	}

	public boolean send(Context context, Intent intent, String macAddress, TransferListener listener) {
		return send(context, intent, null, macAddress, true, listener);
	}

	public boolean sendUDP(Context context, Intent intent, String macAddress, TransferListener listener) {
		return send(context, intent, null, macAddress, false, listener);
	}

	public boolean send(Context context, Intent intent, String macAddress, boolean isTCP, TransferListener listener) {
		return send(context, intent, null, macAddress, isTCP, listener);
	}

	/**
	 * uri に格納されているファイルをbroadcastする
	 * uri の例："file://sdcard/mabo.jpg"
	 * 
	 * @param context
	 * @param uri
	 * @return
	 */
	public boolean broadcast(Context context, Uri uri) {
		return broadcast(context, uri, true, null);
	}

	public boolean broadcastUDP(Context context, Uri uri) {
		return broadcast(context, uri, false, null);
	}

	public boolean broadcast(Context context, Uri uri, TransferListener listener) {
		return broadcast(context, uri, true, listener);
	}

	public boolean broadcastUDP(Context context, Uri uri, TransferListener listener) {
		return broadcast(context, uri, false, listener);
	}

	public boolean broadcast(Context context, Uri uri, boolean isTCP, TransferListener listener) {
		return send(context, uri, null, isTCP, listener);
	}

	public boolean send(Context context, Uri uri, String macAddress) {
		return send(context, uri, macAddress, true, null);
	}

	public boolean sendUDP(Context context, Uri uri, String macAddress) {
		return send(context, uri, macAddress, false, null);
	}

	public boolean send(Context context, Uri uri, String macAddress, TransferListener listener) {
		return send(context, uri, macAddress, true, listener);
	}

	public boolean sendUDP(Context context, Uri uri, String macAddress, TransferListener listener) {
		return send(context, uri, macAddress, false, listener);
	}

	public boolean send(final Context context, final Uri uri, final String macAddress, final boolean isTCP,
			final TransferListener listener) {
		if (uri == null) {
			if (listener != null)
				listener.onCancelled();
			return false;
		}
		//
		final InputStream is;
		try {
			is = context.getContentResolver().openInputStream(uri);
		} catch (FileNotFoundException e) {
			if (listener != null)
				listener.onCancelled();
			return false;
		}
		//
		new Thread(new Runnable() {
			@Override
			public void run() {
				long fileID = uri.toString().hashCode();
				// ファイル名を取得
				String fileName = FileUtil.extractFileNameFromUri(uri);
				if (fileName == null) {
					fileName = Long.toString(fileID);
				}

				// begin
				send(new BeginFileContent(fileID, fileName), macAddress, isTCP, new TransferListener() {
					@Override
					public void onProgress(int value) {
					}

					@Override
					public void onCompleted() {
						Log.d(TAG, "BeginFileContent onCompleted");
					}

					@Override
					public void onCancelled() {
						Log.d(TAG, "BeginFileContent onCancelled");
					}
				});

				// ここで並行して発行する PartedFileContent は2つまでとする
				final Semaphore semaphore = new Semaphore(2);
				BufferedInputStream bis = new BufferedInputStream(is, 512 * 1024);
				byte buf[] = new byte[512 * 1024];
				int len;
				try {
					while ((len = bis.read(buf)) != -1) {
						semaphore.acquire();
						send(new PartedFileContent(fileID, buf, len), macAddress, isTCP, new TransferListener() {
							@Override
							public void onProgress(int value) {
							}

							@Override
							public void onCompleted() {
								semaphore.release();
							}

							@Override
							public void onCancelled() {
								// 本当はやり直すべき
								semaphore.release();
							}
						});
						// Log.d(TAG, "length = " + len);
					}
					bis.close();
				} catch (Exception e) {
					Log.d("TEST", e.toString());
					if (listener != null)
						listener.onCancelled();
				}
				// end
				send(new EndFileContent(fileID), macAddress, isTCP, null);

				if (listener != null)
					listener.onCompleted();
			}
		}).start();

		return true;
	}

	public void setListener(WifiDirectServiceListener listener) {
		this.listener = listener;
	}

	/**
	 * 基本的には onPeersChanged でピア一覧を取得する イベントを取れない状況や，リスナーをセットしたくない場合などに使う．
	 * 
	 * @return
	 */
	public List<Peer> getPeers() {
		if (service == null) {
			return null;
		}
		try {
			return service.getPeerList();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;
	}
}
