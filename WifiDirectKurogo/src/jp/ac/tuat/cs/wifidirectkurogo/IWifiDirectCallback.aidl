package jp.ac.tuat.cs.wifidirectkurogo;

import jp.ac.tuat.cs.wifidirectkurogo.message.Message;
import jp.ac.tuat.cs.wifidirectkurogo.message.MinimalMessage;
import jp.ac.tuat.cs.wifidirectkurogo.peer.Peer;

oneway interface IWifiDirectCallback {
    void onPeersChanged(in List<Peer> peers);
    void onMessageReceived(in Message message);
    
    // データのやりとりを可視化するために用意
    void comein(in MinimalMessage minimalMessage);
    void goout(in MinimalMessage minimalMessage);
}
