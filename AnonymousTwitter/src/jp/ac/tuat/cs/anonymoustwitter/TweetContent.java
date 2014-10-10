package jp.ac.tuat.cs.anonymoustwitter;

import java.io.Serializable;

import android.content.Context;

public class TweetContent implements Serializable {
	public final static int ICON_N = 63;
	public final static int[] RESOURCE_TABLE = { R.drawable.icon0, R.drawable.icon1, R.drawable.icon2,
			R.drawable.icon3, R.drawable.icon4, R.drawable.icon5, R.drawable.icon6, R.drawable.icon7, R.drawable.icon8,
			R.drawable.icon9, R.drawable.icon10, R.drawable.icon11, R.drawable.icon12, R.drawable.icon13,
			R.drawable.icon14, R.drawable.icon15, R.drawable.icon16, R.drawable.icon17, R.drawable.icon18,
			R.drawable.icon19, R.drawable.icon20, R.drawable.icon21, R.drawable.icon22, R.drawable.icon23,
			R.drawable.icon24, R.drawable.icon25, R.drawable.icon26, R.drawable.icon27, R.drawable.icon28,
			R.drawable.icon29, R.drawable.icon30, R.drawable.icon31, R.drawable.icon32, R.drawable.icon33,
			R.drawable.icon34, R.drawable.icon35, R.drawable.icon36, R.drawable.icon37, R.drawable.icon38,
			R.drawable.icon39, R.drawable.icon40, R.drawable.icon41, R.drawable.icon42, R.drawable.icon43,
			R.drawable.icon44, R.drawable.icon45, R.drawable.icon46, R.drawable.icon47, R.drawable.icon48,
			R.drawable.icon49, R.drawable.icon50, R.drawable.icon51, R.drawable.icon52, R.drawable.icon53,
			R.drawable.icon54, R.drawable.icon55, R.drawable.icon56, R.drawable.icon57, R.drawable.icon58,
			R.drawable.icon59, R.drawable.icon60, R.drawable.icon61, R.drawable.icon62

	};
	private static long myID;

	private int iconResourceId;
	private String body;
	private long date;

	public TweetContent(long id, long date, String body) {
		super();
		this.iconResourceId = RESOURCE_TABLE[(int) (id % ICON_N)];
		this.body = body;
		this.date = date;
	}

	public TweetContent(long id, String body) {
		this(id, System.currentTimeMillis(), body);
	}

	public int getIconResourceId() {
		return iconResourceId;
	}

	public String getBody() {
		return body;
	}

	public long getDate() {
		return date;
	}

	public static TweetContent create(Context context, String body) {
		if (myID == 0) {
			myID = (long) (Math.random() * ICON_N);
		}
		return new TweetContent(myID, System.currentTimeMillis(), body);
	}
}