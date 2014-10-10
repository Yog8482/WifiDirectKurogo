package jp.ac.tuat.cs.wifidirectkurogo.message;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jp.ac.tuat.cs.wifidirectkurogo.message.MinimalMessage.Type;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * 容量が大きい場合もある data をAIDLを通して WifiDirectManager に配信したり，
 * その重たいデータを可視化のためのデータ構造に入れたままにすると非効率．
 * data を自由にミドルウェア利用者に見せるのは，セキュリティ的に問題がある．
 * Messageクラスのうち，可視化に必要なデータのみを抜き出したクラスを MinimalMessage とする．
 * あえて MinimalMessage を継承しない．
 *
 */
public class Message implements Parcelable, Serializable {
	private MinimalMessage mini;
	private byte[] data; // 自由に拡張可能なデータ領域．ここにContentを格納する．

	public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>() {
		public Message createFromParcel(Parcel in) {
			return new Message(in);
		}

		public Message[] newArray(int size) {
			return new Message[size];
		}
	};

	public Message(Serializable content) {
		mini  = new MinimalMessage(content.getClass().getName());

		// content を data に書き込み
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(baos));
			oos.writeObject(content);
			oos.flush();
			this.data = baos.toByteArray();
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Message(Parcel in) {
		mini  = new MinimalMessage();
		readFromParcel(in);
	}

	public Serializable getContent() {
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		try {
			ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(bais));
			return (Serializable) ois.readObject();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		Log.d("Message", "writeToParcel");
		mini.writeToParcel(dest, flags);
		Log.d("Message", "mini.writeToParcel(dest, flags)");
		if (data == null) {
			dest.writeInt(0);
		} else {
			dest.writeInt(data.length);
			dest.writeByteArray(data);
		}
	}

	public void readFromParcel(Parcel src) {
		Log.d("Message", "readFromParcel");
		mini.readFromParcel(src);
		Log.d("Message", "mini.readFromParcel(src)");
		int length = src.readInt();
		if (length == 0) {
			// do nothing
		} else {
			data = new byte[length];
			src.readByteArray(data);
		}
	}
	
	// 
	
	public long getID() {
		return mini.getID();
	}

	public boolean isDebug() {
		return false;
	}

	public List<String> getFrom() {
		return mini.getFrom();
	}

	public String getTo() {
		return mini.getTo();
	}

	public int getTTL() {
		return mini.getTTL();
	}

	public boolean isFlooding() {
		return mini.isFlooding();
	}

	public String getContentClassName() {
		return mini.getContentClassName();
	}

	public void addFrom(String fromAddress) {
		mini.addFrom(fromAddress);
	}

	public void setTo(String to) {
		mini.setTo(to);
	}

	public void minusTTL() {
		mini.minusTTL();
	}

	public void setFlooding(boolean isFlooding) {
		mini.setFlooding(isFlooding);
	}

	public MinimalMessage getMinimal() {
		return mini;
	}

	@Override
	public String toString() {
		return mini.toString();
	}
	
	

}
