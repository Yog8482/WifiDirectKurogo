package jp.ac.tuat.cs.wifidirectkurogo.peer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jp.ac.tuat.cs.wifidirectkurogo.FileUtil;
import jp.ac.tuat.cs.wifidirectkurogo.NetUtil;
import jp.ac.tuat.cs.wifidirectkurogo.message.HelloContent;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

/**
 * 過去・現在のピア情報をDBに記録するためのラッパー ユーザのピア権限設定などをサービス終了後も保持し続けるためにローカルDBとして実装
 * 数台のピアしか想定していないのにSQLiteを使うのは無駄に感じたため， 設定情報はシリアライズして保存する
 */
public class PeerManager {
	private final static String TAG = "PeerManager";
	private Context context;
	private List<Peer> peers;

	public PeerManager(Context context) {
		this.context = context;
		resume();
	}

	private void resume() {
		peers = (List<Peer>) FileUtil.readObjectFromFile(context, "peers.dat");
		if (peers == null) {
			peers = new ArrayList<Peer>();
			Log.d(TAG, "Try to resume but null");
		} else {
			Log.d(TAG, "Successfully resumed");
			validatePeers();
		}
	}

	/**
	 * 誤っているデータを削除する
	 */
	private void validatePeers() {
		Iterator<Peer> it = peers.iterator();
		while (it.hasNext()) {
			Peer peer = it.next();
			if (peer.getName() == null || peer.getName().equals("")) {
				it.remove();
			} else if (peer.getMacAddress() == null || peer.getMacAddress().equals("")) {
				it.remove();
			}
		}
	}

	/**
	 * アプリごとのデータ保存領域に peers をシリアライズして保存
	 */
	public void save() {
		Collections.sort(peers);
		FileUtil.writeObjectToFile(context, peers, "peers.dat");
		// debug
		Log.d(TAG, "saved peers.size() = " + peers.size());
		for (Peer peer : peers) {
			Log.d(TAG, peer.toString());
		}
	}

	/**
	 * WifiP2pDeviceList#getDeviceList() を元にDBを更新する
	 * 
	 * @param list
	 * @return
	 */
	public boolean update(Collection<WifiP2pDevice> list) {
		boolean changed = false;

		// 渡された list に含まれていない peer の状態が，
		// UNAVAILABLE 以外なら UNAVAILABLE にする
		for (Peer peer : peers) {
			boolean contains = false;
			for (WifiP2pDevice device : list) {
				if (device.deviceAddress.equals(peer.getMacAddress())) {
					contains = true;
				}
			}
			if (!contains) {
				if (peer.getStatus() != WifiP2pDevice.UNAVAILABLE) {
					peer.setStatus(WifiP2pDevice.UNAVAILABLE);
					changed = true;
				}
			}
		}

		// list に含まれている要素を peers と比べて更新
		for (WifiP2pDevice device : list) {
			changed = changed || update(device);
		}

		if (changed) {
			save();
		}

		return changed;
	}

	/**
	 * WifiP2pDevice を元にDBを更新する
	 * 
	 * @param device
	 * @return DBへの変更があった場合は true を返し，変更が無かった場合は false を返す
	 */
	private boolean update(WifiP2pDevice device) {
		boolean changed = false;
		int pos = getIndex(device.deviceAddress);

		if (pos == -1) {
			// 新規登録
			peers.add(new Peer(device.deviceName, device.deviceAddress, "", device.isGroupOwner(), device.status, true,
					false));
			changed = true;

		} else {
			Peer peer = peers.get(pos);
			// 変化があれば変更
			if (!peer.getName().equals(device.deviceName)) {
				peer.setName(device.deviceName);
				changed = true;
			}
			if (peer.getStatus() != device.status) {
				peer.setStatus(device.status);
				changed = true;
			}
			if (peer.isGroupOwner() != device.isGroupOwner()) {
				peer.setGroupOwner(device.isGroupOwner());
				changed = true;
			}
			if (device.status == WifiP2pDevice.CONNECTED) {
				if (!peer.getNextMACAddress().equals(device.deviceAddress)) {
					changed = true;
				}
				peer.setNextMACAddress(device.deviceAddress);
			}

			changed = changed || peer.updateAvailavility();
		}

		// ここでsave()の必要はない

		return changed;
	}

	public boolean update(String macAddress, String hostAddress) {
		if(hostAddress == null){
			return false;
		}
		int pos = getIndex(macAddress);

		if (pos == -1) {
			// ありえない異常ケース
			// throw new IllegalStateException("存在しないピア(" + macAddress +
			// ")のIPアドレスを設定しようとしている");
			// 自分自身を登録しようとした場合新規登録
			if (macAddress.equals(NetUtil.getMACAddress(context))) {
				peers.add(new Peer("(me)", macAddress, hostAddress, false, WifiP2pDevice.CONNECTED, true, true));
			}
		} else {
			Peer peer = peers.get(pos);

			boolean changed = false;
			if (!hostAddress.equals(peer.getHostAddress())) {
				changed = true;
			}
			peer.setHostAddress(hostAddress);
			if (changed) {
				save();
			}
			return changed;
		}
		return false;
	}

	public boolean update(HashMap<String, String> addressTable) {
		boolean changed = false;

		Set<String> macs = addressTable.keySet();
		for(String mac : macs){
			String host = addressTable.get(mac);
			changed = changed || update(mac, host);
		}
		// ここでsave()の必要はない
		return changed;
	}

	/**
	 * Message#from を元にルーティングテーブルを構築
	 * 
	 * @param fromList
	 * @return
	 */
	public boolean update(List<String> fromList) {
		if (fromList == null || fromList.size() <= 0) {
			return false;
		}

		boolean changed = false;

		// ルーティングテーブル更新
		String lastMACAddress = fromList.get(fromList.size() - 1);
		for (Peer peer : peers) {
			if (fromList.contains(peer.getMacAddress())) {
				if (!peer.getNextMACAddress().equals(lastMACAddress)) {
					changed = true;
				}
				peer.setNextMACAddress(lastMACAddress);
			}
		}

		// fromList に含まれている peer は生きている（生存更新）
		for (String fromAddress : fromList) {
			Peer peer = getPeer(fromAddress);
			if (peer != null) {
				peer.updateLastTimeGotMessage();
				changed = changed || peer.updateAvailavility();
			}
		}

		if (changed) {
			save();
		}

		return changed;
	}

	public boolean update(String macAddress, boolean isAuthed) {
		int pos = getIndex(macAddress);

		if (pos == -1) {
			// ありえない異常ケース
			for (Peer peer : peers) {
				Log.d(TAG, peer.toString());
			}
			throw new IllegalStateException("存在しないピア(" + macAddress + ")の権限を設定しようとしている");
		} else {
			Peer peer = peers.get(pos);

			if (isAuthed != peer.isAuthed()) {
				peer.setAuthed(isAuthed);
				save();
				return true;
			}
		}
		return false;
	}

	public Peer selectConnectedPeerRandomly() {
		List<Peer> selected = new ArrayList<Peer>();
		for (Peer peer : peers) {
			if (peer.isMe() == false && peer.getStatus() == WifiP2pDevice.CONNECTED) {
				selected.add(peer);
			}
		}
		return selected.get((int) (Math.random() * selected.size()));
	}

	/**
	 * @return グループオーナーのMACアドレスを返す
	 */
	public String getOwnerMACAddress() {
		for (Peer peer : peers) {
			if (peer.isGroupOwner()) {
				return peer.getMacAddress();
			}
		}
		return null;
	}

	/**
	 * @param macAddress
	 * @return 指定されたmacAddressのピアがいるならそのindex．いないなら-1
	 */
	public int getIndex(String macAddress) {
		for (int i = 0; i < peers.size(); i++) {
			if (macAddress.equals(peers.get(i).getMacAddress())) {
				return i;
			}
		}
		return -1;
	}

	public Peer getPeer(String macAddress) {
		int pos = getIndex(macAddress);
		if (pos == -1) {
			return null;
		}
		return peers.get(pos);
	}

	public List<Peer> getList() {
		return peers;
	}
	
	public int getAvailableCount(){
		int count = 0;
		for(Peer peer : peers){
			count += peer.isAvailable() ? 1 : 0;
		}
		return count;
	}

	public HelloContent createHelloContent() {
		HelloContent content = new HelloContent();
		for (Peer peer : peers) {
			if (peer.getMacAddress() != null && peer.getMacAddress().length() > 10 && peer.getHostAddress() != null
					&& peer.getHostAddress().length() > 7) {
				content.add(peer.getMacAddress(), peer.getHostAddress());
			}
		}
		return content;
	}

}
