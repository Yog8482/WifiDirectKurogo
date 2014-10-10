package jp.ac.tuat.cs.wifidirectkurogo.message;

import java.io.Serializable;

public class LogContent implements Serializable {
	private String title;
	private String body;

	public LogContent(String title, String body) {
		super();
		this.title = title;
		this.body = body;
	}

	public String getTitle() {
		return title;
	}

	public String getBody() {
		return body;
	}

}
