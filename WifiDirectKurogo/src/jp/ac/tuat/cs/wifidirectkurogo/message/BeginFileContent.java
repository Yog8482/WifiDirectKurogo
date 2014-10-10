package jp.ac.tuat.cs.wifidirectkurogo.message;

import java.io.Serializable;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * ファイル転送の開始を表す
 */
public class BeginFileContent implements Serializable {
	private long fileID;
	private String fileName;

	public BeginFileContent(long fileID, String fileName) {
		super();
		this.fileID = fileID;
		this.fileName = URLEncoder.encode(fileName);
	}

	public long getFileID() {
		return fileID;
	}

	public String getFileName() {
		return URLDecoder.decode(fileName);
	}

}
