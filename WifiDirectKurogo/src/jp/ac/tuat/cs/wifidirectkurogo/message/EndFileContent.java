package jp.ac.tuat.cs.wifidirectkurogo.message;

import java.io.Serializable;

/**
 * ファイル転送の終了を表す
 */
public class EndFileContent implements Serializable {
	long fileID;

	public EndFileContent(long fileID) {
		super();
		this.fileID = fileID;
	}

	public long getFileID() {
		return fileID;
	}

}
