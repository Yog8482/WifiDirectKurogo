package jp.ac.tuat.cs.wifidirectkurogo.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class MinimalMessage implements Parcelable, Serializable {
	public enum Type {
		UP, DOWN
	};

	protected long id; // System.currentTimeMillis
	protected List<String> from; // 送信元MACアドレスのリスト
	protected String to; // 送信先MACアドレス
	protected int ttl; // マルチホップ転送を許可しない場合はTTLを0にする
	protected boolean isFlooding; // フラッディングする場合は true
	protected String contentClassName; // P2P#process にて二重に getContent
										// が実行される事を避けるため，dataに格納するクラスの名前をこれに入れておく
	protected Type type;
	
	public static final Parcelable.Creator<MinimalMessage> CREATOR = new Parcelable.Creator<MinimalMessage>() {
		public MinimalMessage createFromParcel(Parcel in) {
			return new MinimalMessage(in);
		}

		public MinimalMessage[] newArray(int size) {
			return new MinimalMessage[size];
		}
	};
	
	public MinimalMessage() {
		this("");
	}
	
	public MinimalMessage(Parcel in) {
		readFromParcel(in);
	}

	public MinimalMessage(String contentClassName) {
		this.id = System.currentTimeMillis();
		this.from = new ArrayList<String>();
		this.to = "";
		this.ttl = 3;
		this.isFlooding = false;
		this.contentClassName = contentClassName;
	}

	public long getID() {
		return id;
	}

	public boolean isDebug() {
		return false;
	}

	public List<String> getFrom() {
		return from;
	}

	public String getTo() {
		return to;
	}

	public int getTTL() {
		return ttl;
	}

	public boolean isFlooding() {
		return isFlooding;
	}

	public String getContentClassName() {
		return contentClassName;
	}

	public String getFromString() {
		StringBuilder sb = new StringBuilder();
		for (String fromAddress : from) {
			sb.append(fromAddress + ",");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	public Type getType() {
		return type;
	}

	public void addFrom(String fromAddress) {
		if (fromAddress != null && fromAddress.length() > 0) {
			if (!from.contains(fromAddress)) {
				from.add(fromAddress);
			}

		}
	}

	public void setTo(String to) {
		if (to != null && to.length() > 0) {
			this.to = to;
		}
	}

	public void minusTTL() {
		ttl--;
	}

	public void setFlooding(boolean isFlooding) {
		this.isFlooding = isFlooding;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(id);
		dest.writeStringList(from);
		dest.writeString(to);
		dest.writeInt(ttl);
		dest.writeInt(isFlooding == true ? 1 : 0);
		dest.writeString(contentClassName);
	}

	public void readFromParcel(Parcel src) {
		id = src.readLong();
		from = new ArrayList<String>();
		src.readStringList(from);
		to = src.readString();
		ttl = src.readInt();
		isFlooding = src.readInt() == 1 ? true : false;
		contentClassName = src.readString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Message(");

		if (id != 0) {
			sb.append("id = " + id + ", ");
		}
		if (from.size() > 0) {
			sb.append("from = (");
			for (String fromAddress : from) {
				sb.append(fromAddress + ", ");
			}
			sb.append(")");
		}
		if (!to.equals("")) {
			sb.append("to = " + to + ", ");
		}
		sb.append("ttl = " + ttl + ", ");
		sb.append("isFlooding = " + isFlooding + ", ");
		sb.append("contentClassName = " + contentClassName + ")");

		return sb.toString();
	}
}