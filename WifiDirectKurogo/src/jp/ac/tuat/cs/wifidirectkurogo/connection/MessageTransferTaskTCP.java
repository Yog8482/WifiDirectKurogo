package jp.ac.tuat.cs.wifidirectkurogo.connection;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import jp.ac.tuat.cs.wifidirectkurogo.message.Message;
import android.os.AsyncTask;
import android.os.Parcel;
import android.util.Log;

public class MessageTransferTaskTCP extends AsyncTask<Message, Integer, Boolean> {
	private static final String TAG = "MessageTransferTaskTCP";
	private static final int SOCKET_TIMEOUT = 5000;
	private String hostAddress;
	private int port;
	private TransferListener listener;
	private Socket socket;

	public MessageTransferTaskTCP(String hostAddress, int port, TransferListener listener) {
		super();
		this.hostAddress = hostAddress;
		this.port = port;
		this.listener = listener;
		socket = new Socket();
	}

	@Override
	protected Boolean doInBackground(Message... params) {
		Message mes = params[0];
		
		try {
			socket.bind(null);
			socket.connect((new InetSocketAddress(hostAddress, port)), SOCKET_TIMEOUT);
			ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			oos.writeObject(mes);
			oos.flush();
			oos.close();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			if(listener != null){
				listener.onCancelled();
			}
		} finally {
			closeSocket();
			if(listener != null){
				listener.onCompleted();
			}
		}

		return false;
	}
	
	@Override
	protected void onCancelled() {
		closeSocket();
	}

	private void closeSocket(){
		if (socket != null) {
			if (socket.isConnected()) {
				try {
					socket.close();
				} catch (IOException e) {
					// Give up
					e.printStackTrace();
				}
			}
		}
	}
}
