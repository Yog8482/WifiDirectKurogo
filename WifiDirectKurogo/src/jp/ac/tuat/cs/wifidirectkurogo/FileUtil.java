package jp.ac.tuat.cs.wifidirectkurogo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class FileUtil {
	public static void writeObjectToFile(Context context, Object object, String filename) {
		ObjectOutputStream objectOut = null;
		try {
			FileOutputStream fileOut = context.openFileOutput(filename, Activity.MODE_PRIVATE);
			objectOut = new ObjectOutputStream(fileOut);
			objectOut.writeObject(object);
			fileOut.getFD().sync();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (objectOut != null) {
				try {
					objectOut.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static Object readObjectFromFile(Context context, String filename) {
		ObjectInputStream objectIn = null;
		Object object = null;
		try {
			FileInputStream fileIn = context.getApplicationContext().openFileInput(filename);
			objectIn = new ObjectInputStream(fileIn);
			object = objectIn.readObject();
		} catch (FileNotFoundException e) {
			// Do nothing
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (objectIn != null) {
				try {
					objectIn.close();
				} catch (IOException e) {
				}
			}
		}

		return object;
	}

	public static boolean copyFile(InputStream inputStream, OutputStream out) {
		byte buf[] = new byte[1024];
		int len;
		try {
			while ((len = inputStream.read(buf)) != -1) {
				out.write(buf, 0, len);
			}
			out.flush();
			out.close();
			inputStream.close();
		} catch (IOException e) {
			Log.d("FileUtil", e.toString());
			return false;
		}
		return true;
	}

	public static String extractFileNameFromUri(Uri uri) {
		if (uri == null) {
			return null;
		}
		return extractFileNameFromString(uri.toString());
	}

	public static String extractFileNameFromString(String str) {
		if (str == null) {
			return null;
		}
		if (str.contains("/")) {
			return str.substring(str.lastIndexOf("/") + 1);
		}
		return null;
	}
}
