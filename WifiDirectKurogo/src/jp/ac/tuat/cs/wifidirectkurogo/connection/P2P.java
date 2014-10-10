package jp.ac.tuat.cs.wifidirectkurogo.connection;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jp.ac.tuat.cs.wifidirectkurogo.FileUtil;
import jp.ac.tuat.cs.wifidirectkurogo.NetUtil;
import jp.ac.tuat.cs.wifidirectkurogo.message.BeginFileContent;
import jp.ac.tuat.cs.wifidirectkurogo.message.EndFileContent;
import jp.ac.tuat.cs.wifidirectkurogo.message.HelloContent;
import jp.ac.tuat.cs.wifidirectkurogo.message.IntentContent;
import jp.ac.tuat.cs.wifidirectkurogo.message.Message;
import jp.ac.tuat.cs.wifidirectkurogo.message.NiceToMeetYouContent;
import jp.ac.tuat.cs.wifidirectkurogo.message.PartedFileContent;
import jp.ac.tuat.cs.wifidirectkurogo.peer.Peer;
import jp.ac.tuat.cs.wifidirectkurogo.peer.PeerManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Environment;
import android.util.Log;

public class P2P {
	private final static String TAG = "P2P";
	public final static int UDP_LIMIT_SIZE = 1024; // [byte]
	private final static int TCP_PORT = 10082;
	private final static int UDP_PORT = 10083;
	//
	private Context context;
	//
	private String myMACAddress;
	//
	private boolean isRunning;
	//
	private PeerManager peerManager;
	private List<Long> receivedIDList;
	private P2PListener listener;
	//
	private long lastTimeIBroadcasted; // 最後にこのクラスがデータをブロードキャストした時刻
	// 現在やり取りしているファイル情報（fileID -> 実際のパス）
	private HashMap<Long, String> processingFileTable;
	//
	private ServerSocket tcpServerSocket;
	private DatagramSocket udpServerSocket;

	public P2P(Context context) {
		this.context = context;
		peerManager = new PeerManager(context);
		receivedIDList = new ArrayList<Long>();
		myMACAddress = NetUtil.getMACAddress(context);
		isRunning = false;
		processingFileTable = new HashMap<Long, String>();
	}

	public boolean start() {
		Log.d(TAG, "start isRunning = " + isRunning);
		if (isRunning) {
			return true;
		}
		lastTimeIBroadcasted = System.currentTimeMillis() - 5000;

		// initSockets
		try {
			tcpServerSocket = new ServerSocket(TCP_PORT);
			tcpServerSocket.setReuseAddress(true);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			return false;
		}
		try {
			udpServerSocket = new DatagramSocket(UDP_PORT);
			udpServerSocket.setReuseAddress(true);
			udpServerSocket.setReceiveBufferSize(UDP_LIMIT_SIZE);
		} catch (SocketException e) {
			Log.e(TAG, e.getMessage());
			return false;
		}
		Log.d(TAG, "tcpServerSocket.isClosed() = " + tcpServerSocket.isClosed());
		Log.d(TAG, "udpServerSocket.isClosed() = " + udpServerSocket.isClosed());

		isRunning = true;

		// TCP データ受信用スレッド
		new Thread(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "TCP receive thread started isRunning = " + isRunning);
				while (isRunning) {
					try {
						Socket client = tcpServerSocket.accept();
						// isRunning = false としてもここで accept
						// されているので，このスレッドは停止しない事に注意．
						// 必ず外から close して accept に SocketException (extends
						// IOException) を投げさせる．本来ならNew IOを使うべきだが，AndroidのNew
						// IOは怖いので避ける
						process(client.getInputStream(), true);
					} catch (IOException e) {
						Log.e(TAG, "TCP" + e.getMessage());
					}
				}
				Log.d(TAG, "TCP receive thread ended");
			}
		}).start();

		// UDP データ受信用スレッド
		new Thread(new Runnable() {
			@Override
			public void run() {
				byte receiveBuffer[] = new byte[UDP_LIMIT_SIZE];
				DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
				Log.d(TAG, "UDP receive thread started isRunning = " + isRunning);
				while (isRunning) {
					try {
						udpServerSocket.receive(receivePacket);
						ByteArrayInputStream bais = new ByteArrayInputStream(receivePacket.getData(),
								receivePacket.getOffset(), receivePacket.getLength());
						process(bais, false);
					} catch (IOException e) {
						Log.e(TAG, "UDP" + e.getMessage());
					}
				}
				Log.d(TAG, "UDP receive thread ended");
			}
		}).start();

		return true;
	}

	public void stop() {
		Log.d(TAG, "stop isRunning = " + isRunning);
		if (!isRunning) {
			return;
		}
		isRunning = false;

		// close sockets
		if (tcpServerSocket != null) {
			try {
				tcpServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}
		}

		if (udpServerSocket != null) {
			try {
				udpServerSocket.close();
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
		}
	}

	private void process(InputStream is, boolean isTCP) {
		if (is == null) {
			return;
		}

		// InputStreamからMessage復元，失敗したら何もせずにreturn
		Message message = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(is));
			message = (Message) ois.readObject();
			ois.close();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		if (message == null) {
			return;
		}

		// 過去に受信したことのある message ならスルー
		if (Collections.binarySearch(receivedIDList, message.getID()) >= 0) {
			return;
		}

		listener.comein(message.getMinimal());

		// この synchronized がパフォーマンス低下の原因になってるかも
		synchronized (receivedIDList) {
			receivedIDList.add(message.getID());
			Collections.sort(receivedIDList);
			if (receivedIDList.size() > 10) {
				receivedIDList.remove(0);
			}
		}

		// Message 処理
		// HelloContentとIntentContent, NiceToMeetYouContent はミドルウェアの領分
		Log.d(TAG, "process " + message.toString());
		if (message.getFrom().size() >= 2) {
			Log.e(TAG, "マルチホップされてきた！");
		}
		boolean changed = false;
		changed = peerManager.update(message.getFrom());

		// instanceof は避ける
		// （ここで readContent してミドルウェアの領分ではないクラスだった場合，
		// message をブロードキャストしてまた readContentする必要がある．）
		if (message.getContentClassName().equals(NiceToMeetYouContent.class.getName())) {
			Message ret = new Message(peerManager.createHelloContent());
			ret.setTo(message.getFrom().get(0));
			send(ret, null);
			return; // ここで止める
		}else if (message.getContentClassName().equals(HelloContent.class.getName())) {
			Log.d(TAG, "Received HelloContent");
			HelloContent content = (HelloContent) message.getContent();
			if (peerManager.update(content.getAddressTable()) || changed) {
				Log.d(TAG, "peers are changed");
				if (listener != null) {
					listener.onPeersChanged(peerManager.getList());
				}
			}
		} else if (message.getContentClassName().equals(BeginFileContent.class.getName())) {
			// ファイルを転送するときは最初に BeginFileContent が届く
			BeginFileContent content = (BeginFileContent) message.getContent();
			// table
			// 外部のアプリケーションから参照・削除できるようにするためSDカード上に保存する
			String path = Environment.getExternalStorageDirectory() + "/Download/" + content.getFileID() + "_"
					+ content.getFileName();
			processingFileTable.put(content.getFileID(), path);

			// 新しいファイルを用意，または，既存ファイルを削除する
			File out = new File(path);
			if (!out.getParentFile().exists()) {
				out.getParentFile().mkdir();
			}
			if (!out.exists()) {
				try {
					out.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				out.delete();
				try {
					out.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		} else if (message.getContentClassName().equals(PartedFileContent.class.getName())) {
			PartedFileContent content = (PartedFileContent) message.getContent();
			String path = processingFileTable.get(content.getFileID());
			if (path == null) {
				// ありえないはずなのに・・・
				Log.d(TAG, "BeginFileContent が届いていないのに PartedFileContent が届いた");
			} else {
				File out = new File(path);
				// 一旦Serializeすると，byte[]はnullからlength=0のデータに変わる??まさか??
				// Log.d(TAG, "(content.getData() == null)" + (content.getData()
				// ==
				// null));
				// Log.d(TAG, "content.getData().length = " +
				// content.getData().length);

				// TCPなので届く順番は保証されている
				// 届いた順にどんどん追記していく
				try {
					FileOutputStream fos = new FileOutputStream(out, true); // Append
					fos.write(content.getData());
					fos.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else if (message.getContentClassName().equals(EndFileContent.class.getName())) {
			// ファイルのダウンロードが終わった
			EndFileContent content = (EndFileContent) message.getContent();
			processingFileTable.remove(content.getFileID());
			// ミドルウェアとしてダウンロードの進行状況を通知するAPIを作るなら以降に記述
		} else if (message.getContentClassName().equals(IntentContent.class.getName())) {
			Log.d(TAG, "Received IntentContent");

			// for debug
			// Intent intent = new Intent();
			// intent.setAction(Intent.ACTION_VIEW);
			// intent.addCategory(Intent.CATEGORY_DEFAULT);
			// Uri uri = Uri.parse("file:///sdcard/Download/mabo.jpg");
			// intent.setDataAndType(uri, "image/jpeg");
			// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			// context.startActivity(intent); // startActivity は non-blocking

			if (message.isFlooding()
					|| (!message.isFlooding() && message.getTo().equals(NetUtil.getMACAddress(context)))) {

				IntentContent content = (IntentContent) message.getContent();
				Intent intent = content.getIntent();

				// intentがローカルのファイルを参照している場合は，
				// このcontentが届く前にファイルの転送が完了させてある
				if (content.getExtraStreamUri() != null) {
					// ローカルに参照しているファイルがある
					// 前に登録されていた uri を消す
					intent.removeExtra(Intent.EXTRA_STREAM);
					// 新しい uri を登録
					long id = content.getExtraStreamUri().hashCode();
					Uri newUri = Uri.parse("file:///sdcard/Download/" + id + "_"
							+ FileUtil.extractFileNameFromString(content.getExtraStreamUri()));
					Log.d(TAG, "new uri = " + newUri.toString());
					intent.putExtra(Intent.EXTRA_STREAM, newUri);
				}

				if (content.getDataUri() != null) {
					long id = content.getDataUri().hashCode();

					Uri newUri = Uri.parse("file:///sdcard/Download/" + id + "_"
							+ FileUtil.extractFileNameFromString(content.getDataUri()));
					// 前に登録されていた data を新しいもので上書き
					intent.setDataAndType(newUri, intent.getType());
				}

				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //
				Log.d(TAG, "intent = " + intent);

				context.startActivity(intent); // startActivity は non-blocking
				// Log.d(TAG, "context.startActivity");
			}
		} else {
			Log.d(TAG, "Received other Content");
			if (listener != null) {
				listener.onMessageReceived(message);
			}
		}

		if (message.isFlooding()) {
			broadcast(message, isTCP, null);
		} else {
			send(message, isTCP, null); // このmessageが到達したならこれ以上マルチホップされない（詳しくはsend内部を参照）
		}
	}

	/**
	 * broadcast はデフォルトはTCPで送信する．
	 * 
	 * @param message
	 */
	public void broadcast(Message message, TransferListener listener) {
		broadcast(message, true, listener);
	}

	public void broadcastUDP(Message message, TransferListener listener) {
		broadcast(message, false, listener);
	}

	public void broadcast(Message message, boolean isTCP, TransferListener listener) {
		if (!isRunning) {
			return;
		}
		if (message == null) {
			return;
		}
		message.setFlooding(true);

		// TTLが0より大きいならブロードキャスト
		if (message.getTTL() <= 0) {
			return;
		}
		//
		message.minusTTL();
		// 送信元に自分を追加
		message.addFrom(myMACAddress);

		boolean broadcasted = false;
		List<Peer> peers = peerManager.getList();
		for (Peer peer : peers) {
			// for debug
			// if (peer.getStatus() == WifiP2pDevice.CONNECTED) {
			// Log.d(TAG, "in send " + peer.getName() + ", peer.getMacAddress()"
			// + peer.getMacAddress());
			// for (String mac : message.getFrom()) {
			// Log.d(TAG, "mac = " + mac);
			// }
			// }

			if (peer.getStatus() == WifiP2pDevice.CONNECTED && !message.getFrom().contains(peer.getMacAddress())) {
				String hostAddress = peer.getHostAddress();

				if (hostAddress.equals("")) {
					Log.d(TAG, "I don't know his address");
					continue;
				}
				Log.d(TAG, "send message(" + message.toString() + ") to " + peer.getName());
				broadcasted = true;
				if (isTCP) {
					new MessageTransferTaskTCP(hostAddress, TCP_PORT, listener).execute(message);
				} else {
					new MessageTransferTaskUDP(hostAddress, UDP_PORT, listener).execute(message);
				}
			}
		}
		if (broadcasted) {
			// Log.d("TIME",
			// "lastTimeIBroadcasted = System.currentTimeMillis();");
			lastTimeIBroadcasted = System.currentTimeMillis();
			this.listener.goout(message.getMinimal());
		}
	}

	/**
	 * send はデフォルトはTCPで送信する．
	 * 
	 * @param message
	 */
	public void send(Message message, TransferListener listener) {
		send(message, true, listener);
	}

	public void sendUDP(Message message, TransferListener listener) {
		send(message, false, listener);
	}

	public void send(Message message, boolean isTCP, TransferListener listener) {
		if (!isRunning) {
			return;
		}
		if (message == null) {
			return;
		}
		if (listener != null) {
			listener.onProgress(0);
		}
		message.setFlooding(false);

		// 送信元に自分を追加
		message.addFrom(myMACAddress);

		// 送信元リストに宛先が含まれているなら，この message は既に到達できてる
		if (message.getFrom().contains(message.getTo())) {
			if (listener != null) {
				listener.onProgress(100);
				listener.onCompleted();
			}
			return;
		}

		try {
			Peer peer = peerManager.getPeer(message.getTo());
			Peer actualNextPeer = peerManager.getPeer(peer.getNextMACAddress());
			String hostAddress = actualNextPeer.getHostAddress();
			if (hostAddress != null) {
				this.listener.goout(message.getMinimal());
				if (isTCP) {
					new MessageTransferTaskTCP(hostAddress, TCP_PORT, listener).execute(message);
				} else {
					new MessageTransferTaskUDP(hostAddress, UDP_PORT, listener).execute(message);
				}
			} else {
				throw new Exception();
			}
		} catch (Exception e) {
			// どうしようもないので，ひとまずブロードキャストする
			// ブロードキャストすれば他の誰かが届けてくれるだろう・・・
			broadcast(message, isTCP, listener);
		}

	}

	public PeerManager getPeerManager() {
		return peerManager;
	}

	public void setListener(P2PListener listener) {
		this.listener = listener;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public long getLastTimeIBroadcasted() {
		return lastTimeIBroadcasted;
	}
}
