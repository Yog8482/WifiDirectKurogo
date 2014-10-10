package jp.ac.tuat.cs.wifidirectkurogo.peer;

import java.io.Serializable;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Parcel;
import android.os.Parcelable;

public class Peer implements Parcelable, Comparable<Peer>, Serializable {
	private final static long EXPIRED_LIMIT_TIME = 60 * 1000; // 60秒以上更新されていなかったら無効にする
	private String name;
	private String macAddress;
	private String nextMACAddress; // このピアに達するためには，次にこのMACアドレスにデータを送ればよい
	private String hostAddress;
	private boolean isGroupOwner;
	private int rangeStatus; // WifiP2pDevice#status
	private boolean isAuthed;
	private boolean isMe = false;
	private long lastTimeGotMessage; // 最後にこの peer からの message を受け取った時刻
	private boolean isAvailable;

	public static final Parcelable.Creator<Peer> CREATOR = new Parcelable.Creator<Peer>() {
		public Peer createFromParcel(Parcel in) {
			return new Peer(in);
		}

		public Peer[] newArray(int size) {
			return new Peer[size];
		}
	};

	public Peer(String name, String macAddress, String hostAddress, boolean isGroupOwner, int status, boolean isAuthed,
			boolean isMe) {
		super();
		this.name = name;
		this.macAddress = macAddress;
		this.nextMACAddress = "";
		this.hostAddress = hostAddress;
		this.isGroupOwner = isGroupOwner;
		this.rangeStatus = status;
		this.isAuthed = isAuthed;
		this.isMe = isMe;
		this.lastTimeGotMessage = System.currentTimeMillis() - EXPIRED_LIMIT_TIME - 1;
		this.isAvailable = false;
	}

	public Peer(Parcel in) {
		readFromParcel(in);
	}

	public String getName() {
		return name;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public String getNextMACAddress() {
		return nextMACAddress;
	}

	public String getHostAddress() {
		return hostAddress;
	}

	public boolean isGroupOwner() {
		return isGroupOwner;
	}

	public int getStatus() {
		return rangeStatus;
	}

	public String getStatusString() {
		String statusString = null;
		if (rangeStatus == WifiP2pDevice.AVAILABLE) {
			statusString = "AVAILABLE";
		} else if (rangeStatus == WifiP2pDevice.CONNECTED) {
			statusString = "CONNECTED";
		} else if (rangeStatus == WifiP2pDevice.FAILED) {
			statusString = "FAILED";
		} else if (rangeStatus == WifiP2pDevice.INVITED) {
			statusString = "INVITED";
		} else if (rangeStatus == WifiP2pDevice.UNAVAILABLE) {
			statusString = "UNAVAILABLE";
		}
		return statusString;
	}

	public boolean isAuthed() {
		return isAuthed;
	}

	public boolean isMe() {
		return isMe;
	}

	public boolean isAvailable() {
		if (System.currentTimeMillis() - lastTimeGotMessage > EXPIRED_LIMIT_TIME) {
			return false;
		}
		if (hostAddress == null || hostAddress.equals("")) {
			return false;
		}
		if (macAddress == null || macAddress.equals("")) {
			return false;
		}
		if (nextMACAddress == null || nextMACAddress.equals("")) {
			return false;
		}

		return true;
	}

	public boolean updateAvailavility() {
		boolean nowAvailavility = isAvailable();
		if (isAvailable != nowAvailavility) {
			isAvailable = nowAvailavility;
			return true;
		}
		return false;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

	public void setNextMACAddress(String nextMACAddress) {
		this.nextMACAddress = nextMACAddress;
	}

	public void setHostAddress(String hostAddress) {
		this.hostAddress = hostAddress;
	}

	public void setGroupOwner(boolean isGroupOwner) {
		this.isGroupOwner = isGroupOwner;
	}

	public void setStatus(int status) {
		this.rangeStatus = status;
	}

	public void setAuthed(boolean isAuthed) {
		this.isAuthed = isAuthed;
	}

	public void setMe(boolean isMe) {
		this.isMe = isMe;
	}

	public void updateLastTimeGotMessage() {
		this.lastTimeGotMessage = System.currentTimeMillis();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeString(macAddress);
		dest.writeString(nextMACAddress);
		dest.writeString(hostAddress);
		dest.writeInt(isGroupOwner == true ? 1 : 0);
		dest.writeInt(rangeStatus);
		dest.writeInt(isAuthed == true ? 1 : 0);
		dest.writeInt(isMe == true ? 1 : 0);
		dest.writeLong(lastTimeGotMessage);
		dest.writeInt(isAvailable == true ? 1 : 0);
	}

	public void readFromParcel(Parcel src) {
		name = src.readString();
		macAddress = src.readString();
		nextMACAddress = src.readString();
		hostAddress = src.readString();
		isGroupOwner = src.readInt() == 1 ? true : false;
		rangeStatus = src.readInt();
		isAuthed = src.readInt() == 1 ? true : false;
		isMe = src.readInt() == 1 ? true : false;
		lastTimeGotMessage = src.readLong();
		isAvailable = src.readInt() == 1 ? true : false;
	}

	@Override
	public String toString() {
		return "Peer(name = " + name + ", macAddress = " + macAddress + ", nextMACAddress = " + nextMACAddress
				+ ", hostAddress = " + hostAddress + ", isGroupOwner = " + isGroupOwner + ", status = " + getStatusString()
				+ ", isAuthed = " + isAuthed + ", isMe = " + isMe + ", isAvailable = " + isAvailable()
				+ ", lastTimeGotMessage = " + lastTimeGotMessage + ")";
	}

	// ソートした時に name の辞書順
	@Override
	public int compareTo(Peer another) {
		return name.compareTo(another.name);
	}

}
