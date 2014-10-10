package jp.ac.tuat.cs.wifidirectkurogo.connection;

import java.util.List;

import jp.ac.tuat.cs.wifidirectkurogo.message.Message;
import jp.ac.tuat.cs.wifidirectkurogo.message.MinimalMessage;
import jp.ac.tuat.cs.wifidirectkurogo.peer.Peer;

public interface P2PListener {
	public void onPeersChanged(List<Peer> peers);
	public void onMessageReceived(Message message);

	public void comein(MinimalMessage minimalMessage);
	public void goout(MinimalMessage minimalMessage);
}
