package jp.ac.tuat.cs.wifidirectkurogo.connection;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import jp.ac.tuat.cs.wifidirectkurogo.message.Message;
import android.os.AsyncTask;
import android.util.Log;

public class MessageTransferTaskUDP extends AsyncTask<Message, Integer, Boolean> {
	private static final String TAG = "MessageTransferTaskUDP";
	private static final int SOCKET_TIMEOUT = 5000;
	private String hostAddress;
	private int port;
	private TransferListener listener;
	private static DatagramSocket datagramSocket;

	public MessageTransferTaskUDP(String hostAddress, int port, TransferListener listener) {
		super();
		this.hostAddress = hostAddress;
		this.port = port;
		this.listener = listener;
		if (datagramSocket == null) {
			try {
				datagramSocket = new DatagramSocket();
			} catch (SocketException e) {
			}
		}
	}

	@Override
	protected Boolean doInBackground(Message... params) {
		Message mes = params[0];

		InetSocketAddress remoteAddress = new InetSocketAddress(hostAddress, port);

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(baos));
			oos.writeObject(mes);
			oos.flush();
			byte[] sendBuffer = baos.toByteArray();
			oos.close();

			if (sendBuffer.length > P2P.UDP_LIMIT_SIZE) {
				Log.e(TAG, "送ろうとしてるMessageが制限を超えているのでUDPでは送れない");
				if (listener != null) {
					listener.onCancelled();
				}
				return false;
			}

			// 整合性確認
			// try {
			// ByteArrayInputStream bais = new ByteArrayInputStream(sendBuffer);
			// ObjectInputStream ois = new ObjectInputStream(bais);
			// Message mes2 = (Message) ois.readObject();
			// ois.close();
			// HelloContent content2 = (HelloContent) mes2.getContent();
			// content2.getAddressTable();
			//
			// } catch (IOException e) {
			// e.printStackTrace();
			// } catch (ClassNotFoundException e) {
			// e.printStackTrace();
			// }

			DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, remoteAddress);
			datagramSocket.send(sendPacket);
			if (listener != null) {
				listener.onCompleted();
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (listener != null) {
			listener.onCancelled();
		}

		return false;
	}

}
