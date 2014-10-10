package jp.ac.tuat.cs.wifidirectkurogo;

import jp.ac.tuat.cs.wifidirectkurogo.IWifiDirectCallback;
import jp.ac.tuat.cs.wifidirectkurogo.ITransferCallback;
import jp.ac.tuat.cs.wifidirectkurogo.message.Message;
import jp.ac.tuat.cs.wifidirectkurogo.peer.Peer;

interface IWifiDirectService {
    int getPid();
	boolean isRunning();
	List<Peer> getPeerList();

	// AIDLのインタフェースは最小限にとどめる
	void send(in Message message, in String deviceMacAddress, in boolean isTCP, ITransferCallback cb);
	
	void registerCallback(IWifiDirectCallback cb);
    void unregisterCallback(IWifiDirectCallback cb); 
}