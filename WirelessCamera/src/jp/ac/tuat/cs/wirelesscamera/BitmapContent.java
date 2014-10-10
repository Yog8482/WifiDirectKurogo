package jp.ac.tuat.cs.wirelesscamera;

import java.io.Serializable;

import android.graphics.YuvImage;

public class BitmapContent implements Serializable {
	private byte[] data;
	private int width;
	private int height;

	public BitmapContent(byte[] data, int width, int height) {
		this.data = data;
		this.width = width;
		this.height = height;
	}

	public byte[] getData() {
		return data;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

}
