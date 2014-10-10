package jp.ac.tuat.cs.wifidirectkurogo;

import java.io.Serializable;
import java.util.List;

import jp.ac.tuat.cs.wifidirectkurogo.message.MinimalMessage;
import jp.ac.tuat.cs.wifidirectkurogo.peer.Peer;

public interface WifiDirectServiceListener {
	public void onPeersChanged(List<Peer> peers);
	public void onDataReceived(Serializable content);
	
	// データ通信の可視化用
	public void comein(MinimalMessage minimalMessage);
	public void goout(MinimalMessage minimalMessage);
}
