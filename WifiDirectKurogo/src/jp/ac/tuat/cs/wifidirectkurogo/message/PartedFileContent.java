package jp.ac.tuat.cs.wifidirectkurogo.message;

import java.io.Serializable;

import android.util.Log;

/**
 * ファイル転送のために分割したファイルの一部
 */
public class PartedFileContent implements Serializable {
	private long fileID;

	// data.lengthでこの一部のファイルの容量[byte]が分かる
	private byte[] data;

	public PartedFileContent(long fileID, byte[] buf, int length) {
		super();
		this.fileID = fileID;
		if (buf != null) {
			this.data = new byte[length];
			System.arraycopy(buf, 0, data, 0, length);
			Log.d("PartedFileContent", " data.length = " + data.length);
		}
	}

	public long getFileID() {
		return fileID;
	}

	public byte[] getData() {
		return data;
	}

}
