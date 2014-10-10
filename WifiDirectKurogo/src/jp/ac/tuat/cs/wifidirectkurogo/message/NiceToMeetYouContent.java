package jp.ac.tuat.cs.wifidirectkurogo.message;

import java.io.Serializable;

public class NiceToMeetYouContent implements Serializable {
	private int myAvailableCount;

	public NiceToMeetYouContent(int myAvailableCount) {
		super();
		this.myAvailableCount = myAvailableCount;
	}

	public int getMyAvailableCount() {
		return myAvailableCount;
	}

}
