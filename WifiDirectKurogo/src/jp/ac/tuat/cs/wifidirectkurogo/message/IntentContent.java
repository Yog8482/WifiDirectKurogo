package jp.ac.tuat.cs.wifidirectkurogo.message;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;

import jp.ac.tuat.cs.wifidirectkurogo.FileUtil;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class IntentContent implements Serializable {
	private static final String TAG = "IntentContent";
	private String intentString;
	// private byte[] fileData;
	private String extraStreamUri;
	private String dataUri;

	public IntentContent(Intent intent, ComponentName componentName) {

		// Explicit Intents have specified a component らしい
		// intentのComponentNameがこのActivity自身だと無限ループに陥るので，同じなら消す
		// クラス名の末尾に$が付くので，それを除いて比較する
		ComponentName gotName = intent.getComponent();
//		Log.d(TAG, "gotName = " + gotName.toShortString());
//		Log.d(TAG, "gotName.getPackageName = " + gotName.getPackageName());
//		Log.d(TAG, "gotName.getShortClassName = " + gotName.getShortClassName());
//		Log.d(TAG, "gotName.flattenToShortString() = " + gotName.flattenToShortString());
//		Log.d(TAG, "gotName.equals(componentName) = " + gotName.equals(componentName));

		if (componentName != null) {
//			Log.d(TAG, "componentName = " + componentName.toShortString());
//			Log.d(TAG, "componentName.getPackageName = " + componentName.getPackageName());
//			Log.d(TAG, "componentName.getShortClassName = " + componentName.getShortClassName());
//			Log.d(TAG, "componentName.flattenToShortString = " + componentName.flattenToShortString());
			if (gotName.getPackageName().equals(componentName.getPackageName())) {
				String gotcname = gotName.getShortClassName();
				String comcname = componentName.getShortClassName();

				int gotpos = gotcname.lastIndexOf("$");
				if(gotpos != -1){
					gotcname = gotcname.substring(0, gotpos);
				}

				int compos = comcname.lastIndexOf("$");
				if(compos != -1){
					comcname = comcname.substring(0, compos);
				}
				
//				Log.d(TAG, "comcname = " + comcname);
//				Log.d(TAG, "gotcname = " + gotcname);
				if (gotcname.equals(comcname)) {
					intent.setComponent(null);
//					Log.d(TAG, "intent.setComponent(null)");
				}
			}
		}

		// android.intent.extra.STREAM でローカルのファイルを参照している場合
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			Uri exuri = (Uri) bundle.get(Intent.EXTRA_STREAM);
			if (exuri != null) {
				extraStreamUri = exuri.toString();
			}

		}
		Uri duri = intent.getData();
		if (duri != null && !intent.getType().contains("text")) {
			dataUri = duri.toString();
		}

		intentString = intent.toURI();
	}

	public IntentContent(Intent intent) {
		this(intent, null);
		// super(-1, "", System.currentTimeMillis(),
		// "#Intent;action=android.intent.action.SEND;type=text/plain;S.android.intent.extra.TEXT=hello;end");

		// ComponentName myName = new ComponentName(context, getClass());
		// String myPackageName = context.getPackageName();
		// ComponentName gotName = intent.getComponent();
		//
		// Log.d(TAG, "myName = " + myName.toShortString() + ", " +
		// myName.toString());
		// Log.d(TAG, "myPackageName = " + myPackageName);
		// Log.d(TAG, "gotName = " + gotName.toShortString() + ", " +
		// gotName.toString());

	}

	// /**
	// * 容量が大きすぎるとAIDLでプロセス間通信できない（TransactionTooLargeException）
	// * サービスに渡してからこのメソッドを使ってファイルを読み込む．
	// * @param context
	// * @return
	// */
	// public boolean loadExtraStream(Context context) {
	// if (fileUri != null) {
	// Log.d("IntentContent", fileUri.toString());
	// try {
	// ByteArrayOutputStream baos = new ByteArrayOutputStream();
	// InputStream is = context.getContentResolver().openInputStream(fileUri);
	// FileUtil.copyFile(new BufferedInputStream(is), new
	// BufferedOutputStream(baos));
	// fileData = baos.toByteArray();
	// return true;
	// } catch (FileNotFoundException e) {
	// e.printStackTrace();
	// }
	// }
	// return false;
	// }

	public Intent getIntent() {
		try {
			Intent decoded = Intent.getIntent(intentString);
			return decoded;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getExtraStreamUri() {
		return extraStreamUri;
	}

	public String getDataUri() {
		return dataUri;
	}

	// public byte[] getFileData() {
	// return fileData;
	// }

}
