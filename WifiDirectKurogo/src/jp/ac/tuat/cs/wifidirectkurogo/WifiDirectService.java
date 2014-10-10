package jp.ac.tuat.cs.wifidirectkurogo;

import java.util.Collection;
import java.util.List;

import jp.ac.tuat.cs.wifidirectkurogo.connection.P2P;
import jp.ac.tuat.cs.wifidirectkurogo.connection.P2PListener;
import jp.ac.tuat.cs.wifidirectkurogo.connection.TransferListener;
import jp.ac.tuat.cs.wifidirectkurogo.message.Message;
import jp.ac.tuat.cs.wifidirectkurogo.message.MinimalMessage;
import jp.ac.tuat.cs.wifidirectkurogo.message.NiceToMeetYouContent;
import jp.ac.tuat.cs.wifidirectkurogo.peer.Peer;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class WifiDirectService extends Service implements PeerListListener, ConnectionInfoListener {
	private final static String TAG = "WifiDirectService";
	private final static int REPORT_MSG = 1;
	private final IntentFilter intentFilter = new IntentFilter();
	// Wi-Fi Direct
	private WifiP2pManager manager;
	private Channel channel;
	private WifiDirectBroadcastReceiver receiver = null;
	private boolean isRunning;
	// Wi-Fi Direct 制御用フラグ
	private boolean isDiscovering;
	private boolean isConnectingToAnotherDevice;
	//
	private P2P p2p;
	//
	private NotificationManager notificationManager;
	//
	final RemoteCallbackList<IWifiDirectCallback> callbacks = new RemoteCallbackList<IWifiDirectCallback>();

	private IWifiDirectService.Stub service = new IWifiDirectService.Stub() {
		@Override
		public boolean isRunning() throws RemoteException {
			return isRunning;
		}

		@Override
		public int getPid() throws RemoteException {
			return Process.myPid();
		}

		@Override
		public List<Peer> getPeerList() throws RemoteException {
			return p2p.getPeerManager().getList();
		}

		@Override
		public void send(Message message, String deviceMacAddress, boolean isTCP, final ITransferCallback cb)
				throws RemoteException {
			TransferListener transferListener = new TransferListener() {
				@Override
				public void onProgress(int value) {
					try {
						if (cb != null)
							cb.onProgress(value);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onCompleted() {
					try {
						if (cb != null)
							cb.onCompleted();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onCancelled() {
					try {
						if (cb != null)
							cb.onCancelled();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			};

			if (deviceMacAddress == null || deviceMacAddress.length() < 7) {
				// broadcast
				// deviceMacAddressが不正ならbroadcastとみなす
				// AIDLのインタフェースは出来るだけ少なくする
				p2p.broadcast(message, isTCP, transferListener);
			} else {
				// send
				message.setTo(deviceMacAddress);
				p2p.send(message, isTCP, transferListener);
			}
		}

		@Override
		public void registerCallback(IWifiDirectCallback cb) throws RemoteException {
			if (cb != null) {
				callbacks.register(cb);
				Log.d(TAG, "callbacks.register");
			}
		}

		@Override
		public void unregisterCallback(IWifiDirectCallback cb) throws RemoteException {
			if (cb != null) {
				callbacks.unregister(cb);
				Log.d(TAG, "callbacks.unregister");
			}
		}

	};

	private final Handler myHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case REPORT_MSG:
				// 定期処理でUIをいじるなら，Handlerを使ったほうがいいけど，使わないなら必要ない

				// int value = (int) (Math.random()*Integer.MAX_VALUE);
				// final int N = callbacks.beginBroadcast();
				// Log.d(TAG, "callbacks.beginBroadcast N = " + N);
				// for(int i=0; i<N; i++){
				// try {
				// callbacks.getBroadcastItem(i).valueChanged(value);
				// } catch (RemoteException e) {
				// e.printStackTrace();
				// }
				// }
				// callbacks.finishBroadcast();
				sendMessageDelayed(obtainMessage(REPORT_MSG), 1000);
				break;
			default:
				super.handleMessage(msg);
			}

		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		if (IWifiDirectService.class.getName().equals(intent.getAction())) {
			return service;
		}
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");
		//
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		//
		manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		channel = manager.initialize(this, getMainLooper(), null);
		//
		receiver = new WifiDirectBroadcastReceiver();
		registerReceiver(receiver, intentFilter);
		//
		isDiscovering = false;
		isConnectingToAnotherDevice = false;
		//
		isRunning = false;
		start();
		//
		myHandler.sendEmptyMessage(REPORT_MSG);
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		showNotification();
	}

	/**
	 * 各スレッドを開始する onCreateとonStartの両方から呼ばれる 内部のisRunningフラグによって多重起動を防止
	 * ライフサイクルにどのように割り込まれても，常に適切にスレッドが開始される
	 */
	private void start() {
		if (p2p == null) {
			p2p = new P2P(getBaseContext());
			p2p.setListener(new P2PListener() {
				@Override
				public void onPeersChanged(List<Peer> peers) {
					notifyPeersAreChanged();
				}

				@Override
				public void onMessageReceived(Message message) {
					notifyMessageReceived(message);
				}

				@Override
				public void comein(MinimalMessage minimalMessage) {
					notifyComein(minimalMessage);
				}

				@Override
				public void goout(MinimalMessage minimalMessage) {
					notifyGoout(minimalMessage);
				}
			});
			boolean result = p2p.start();
			if (result == false) {
				Toast.makeText(getApplicationContext(), "起動に失敗しました．ポートが既に使用されています", Toast.LENGTH_SHORT).show();
				p2p.stop();
				isRunning = false;
				stopSelf();
			}
		}

		if (isRunning) {
			return;
		} else {
			isRunning = true;
			// ピア探索用スレッド
			new Thread(new Runnable() {
				@Override
				public void run() {
					while (isRunning) {
						// Log.d(TAG, "manager.discoverPeers");
						if (isConnectingToAnotherDevice) {
							continue;
						}
						if (isDiscovering == false) {
							isDiscovering = true;
							manager.discoverPeers(channel, new ActionListener() {
								@Override
								public void onSuccess() {
									Log.d(TAG, "manager.discoverPeers onSuccess");
									isDiscovering = false;
								}

								@Override
								public void onFailure(int reason) {
									if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
										Log.d(TAG, "manager.discoverPeers onFailure because p2p is unsupported on the device.");
									} else if (reason == WifiP2pManager.ERROR) {
										Log.d(TAG, "manager.discoverPeers onFailure failed due to an internal error.");
									} else if (reason == WifiP2pManager.BUSY) {
										Log.d(TAG,
												"manager.discoverPeers onFailure  because the framework is busy and unable to service the request.");
									}
									isDiscovering = false;
								}
							});
						}

						// 定期的なHelloContentの送信
						// 前回のブロードキャストよりも5s以上経っているなら送信
						// （他のIntentContentやTweetContentなどを送信していて，このHelloMessageを送る必要がないかもしれない）
						// 3以上のホストがいる状況であればこの制限を有効にすべき
						// 2以下の状況でこれを有効にすると，正しく互いの存在状況を確認できないケースが出てくる
						// (2つの端末がKurogoを起動していて3つ目の端末が起動していない場合など)
						// 1/16変更
						// Availableな端末が0の時は5sに一回
						int n = p2p.getPeerManager().getAvailableCount();
						if (n == 0) {
							p2p.broadcastUDP(new Message(new NiceToMeetYouContent(n)), null);
							Message hello = new Message(p2p.getPeerManager().createHelloContent());
							p2p.broadcastUDP(hello, null);
						} else if (System.currentTimeMillis() - p2p.getLastTimeIBroadcasted() >= 30000 * n) {
							Message hello = new Message(p2p.getPeerManager().createHelloContent());
							p2p.broadcastUDP(hello, null);
						}

						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}).start();
		}
	}

	private void showNotification() {
		Notification notification = new Notification(R.drawable.service_ic_launcher, "WiFiDirectKurogoを開始しました",
				System.currentTimeMillis());

		Intent nextIntent = new Intent().setClass(this, ManageServiceActivity.class);
		PendingIntent pending = PendingIntent.getActivity(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		notification.setLatestEventInfo(this, "WiFiDirectKurogo", "タップすると管理画面を表示します", pending);

		notificationManager.notify(R.string.service_name, notification);

	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.d(TAG, "onStart");
		start(); // isRunning = true なら何もされない
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		notificationManager.cancelAll();
		callbacks.kill();
		isRunning = false;
		p2p.stop();
		myHandler.removeMessages(REPORT_MSG);
		unregisterReceiver(receiver);
		Log.d(TAG, "onDestroy reached");
	}

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers) {
		Log.d(TAG, "onPeersAvailable");
		Collection<WifiP2pDevice> list = peers.getDeviceList();
		if (p2p.getPeerManager().update(list)) {
			notifyPeersAreChanged();
		}

		boolean hasConnectedDevice = false;
		for (WifiP2pDevice device : list) {
			if (device.status == WifiP2pDevice.AVAILABLE) {
				// 自動接続する
				// connect(device.deviceAddress);
			} else if (device.status == WifiP2pDevice.CONNECTED) {
				// if (went == false) {
				// went = true;
				// connect(device.deviceAddress);
				// }
				// mes.append("My MAC address is " +
				// NetUtil.getMACAddress(getApplicationContext()) + "\n");
				// mes.append("My IP address is " +
				// NetUtil.getIPAddress(getApplicationContext()) + "\n");

				hasConnectedDevice = true;

			} else if (device.status == WifiP2pDevice.FAILED) {
			} else if (device.status == WifiP2pDevice.INVITED) {
			} else if (device.status == WifiP2pDevice.UNAVAILABLE) {
			}
		}
		if (hasConnectedDevice) {
			manager.requestConnectionInfo(channel, this);
		}
	}

	private void notifyPeersAreChanged() {
		synchronized (callbacks) {
			Log.d(TAG, "notifyPeersAreChanged()");
			int N = callbacks.beginBroadcast();
			Log.d(TAG, "callbacks.beginBroadcast N = " + N);
			for (int i = 0; i < N; i++) {
				try {
					callbacks.getBroadcastItem(i).onPeersChanged(p2p.getPeerManager().getList());
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			callbacks.finishBroadcast();
		}
	}

	private void notifyMessageReceived(Message message) {
		Log.d(TAG, "notifyMessageReceived()");
		synchronized (callbacks) {
			final int N = callbacks.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					callbacks.getBroadcastItem(i).onMessageReceived(message);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			callbacks.finishBroadcast();
		}
	}

	private void notifyComein(MinimalMessage mes) {
		Log.d(TAG, "notifyComein");
		synchronized (callbacks) {
			final int N = callbacks.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					callbacks.getBroadcastItem(i).comein(mes);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			callbacks.finishBroadcast();
		}
	}

	private void notifyGoout(MinimalMessage mes) {
		Log.d(TAG, "notifyGoout");
		synchronized (callbacks) {
			final int N = callbacks.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					callbacks.getBroadcastItem(i).goout(mes);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			callbacks.finishBroadcast();
		}
	}

	// for debug
	boolean did = false;
	int count = 0;
	long lastTimeIBroadcasted;
	long BROADCAST_HELLO_INTERVAL = 10 * 1000;

	/*
	 * 3.75 - 9.7 sに一回定期的に呼ばれる
	 */
	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		Log.d(TAG, "onConnectionInfoAvailable");
		if (!info.groupFormed) {
			return;
		}

		boolean changed = false;
		if (info.isGroupOwner) {
			Log.d(TAG, "Owner");
			changed = changed
					|| p2p.getPeerManager().update(NetUtil.getMACAddress(getApplicationContext()),
							info.groupOwnerAddress.getHostAddress());
		} else {
			Log.d(TAG, "Not owner");
			changed = changed
					|| p2p.getPeerManager().update(NetUtil.getMACAddress(getApplicationContext()), NetUtil.getLocalIPAddress());

			String ownerMACAddress = p2p.getPeerManager().getOwnerMACAddress();
			if (ownerMACAddress != null) {
				changed = changed || p2p.getPeerManager().update(ownerMACAddress, info.groupOwnerAddress.getHostAddress());
			}
		}
		if (changed) {
			notifyPeersAreChanged();
		}

		// if (count % 2 == 0) {
		// Message hello = new
		// HelloMessage(p2p.getPeerManager().toHelloMessageString());
		// p2p.broadcastUDP(hello);
		// }

		// Log.d(TAG, "info.groupOwnerAddress.getHostAddress() = " +
		// info.groupOwnerAddress.getHostAddress());
		// Log.d(TAG, "My local IP address is " + );

		count++;
	}

	/**
	 * Wi-Fi Direct で他の端末に接続要求を送る．セキュリティの観点から自動接続機能を無効にしたため，現在は使っていない
	 * 
	 * @param deviceAddress
	 */
	private void connect(final String deviceAddress) {
		if (isConnectingToAnotherDevice == true) {
			return;
		}
		isConnectingToAnotherDevice = true;
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = deviceAddress;
		config.wps.setup = WpsInfo.PBC;
		manager.connect(channel, config, new ActionListener() {
			@Override
			public void onSuccess() {
				Log.d(TAG, "connecting to " + deviceAddress + " onSuccess");
				isConnectingToAnotherDevice = false;
			}

			@Override
			public void onFailure(int reason) {
				Log.d(TAG, "connecting to " + deviceAddress + " onFailure");
				isConnectingToAnotherDevice = false;
			}
		});
	}

	//
	//
	//
	//
	public class WifiDirectBroadcastReceiver extends BroadcastReceiver {
		private final static String TAG = "WifiDirectBroadcastReceiver";

		public WifiDirectBroadcastReceiver() {
			super();
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
				int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
				if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
					Log.d(TAG, "Wifi Direct mode is enabled");
				} else {
					Log.d(TAG, "Wifi Direct mode is not enabled");
				}

			} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
				// Log.d(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
				if (manager != null) {
					manager.requestPeers(channel, WifiDirectService.this);
				}
			} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
				if (manager == null) {
					return;
				}
				NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

				if (networkInfo.isConnected()) {
					Log.d(TAG, "we are connected with the other device, request connection info to find group owner IP");
					manager.requestConnectionInfo(channel, WifiDirectService.this);
				} else {
					Log.d(TAG, "It's a disconnect");
				}
			} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
				Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
			} else {
				Log.d(TAG, "something happend");
			}

		}
	}
}
