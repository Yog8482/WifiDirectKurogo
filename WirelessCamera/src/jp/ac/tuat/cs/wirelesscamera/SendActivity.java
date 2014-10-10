package jp.ac.tuat.cs.wirelesscamera;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.List;

import jp.ac.tuat.cs.wifidirectkurogo.WifiDirectManager;
import jp.ac.tuat.cs.wifidirectkurogo.WifiDirectServiceListener;
import jp.ac.tuat.cs.wifidirectkurogo.connection.TransferListener;
import jp.ac.tuat.cs.wifidirectkurogo.message.MinimalMessage;
import jp.ac.tuat.cs.wifidirectkurogo.peer.Peer;
import android.app.Activity;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class SendActivity extends Activity {
	private static final String TAG = "SendActivity";
	private static final long MIN_SEND_INTERVAL = 1000; // ms
	//
	private CameraPreview preview;
	private WifiDirectManager manager;
	private boolean isSending;
	private PreviewCallback previewCallback;
	private long lastSended;
	//
	private int previewFormat;
	private int width, height;
	//
	private long frameCount;

	private TransferListener transferListener = new TransferListener() {
		@Override
		public void onProgress(int arg0) {
		}

		@Override
		public void onCompleted() {
			isSending = false;
			Log.d(TAG, "onCompleted isSending = " + isSending);
		}

		@Override
		public void onCancelled() {
			isSending = false;
			Log.d(TAG, "onCancelled isSending = " + isSending);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_send);

		isSending = false;
		preview = new CameraPreview(this, (SurfaceView) findViewById(R.id.surfaceView));
		preview.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		((FrameLayout) findViewById(R.id.frameLayout)).addView(preview);
		preview.setKeepScreenOn(true);

		frameCount = 0;
		lastSended = System.currentTimeMillis() - MIN_SEND_INTERVAL;
		previewCallback = new PreviewCallback() {
			@Override
			public void onPreviewFrame(final byte[] data, final Camera camera) {
				frameCount++;
				if (frameCount == 1) {
					Camera.Parameters params = camera.getParameters();
					Size size = params.getPreviewSize();
					previewFormat = params.getPreviewFormat();
					width = size.width;
					height = size.height;
					return;
				} else if (frameCount < 10) {
					return;
				}

				// Log.d(TAG, "star onPreviewFrame isSending = " + isSending);
				if (isSending || manager == null || data == null || data.length == 0) {
					return;
				}
				// Log.d(TAG, "data.length = " + data.length);
				if (System.currentTimeMillis() <= lastSended + MIN_SEND_INTERVAL) {
					return;
				}
				isSending = true;
				Log.d(TAG, "interval = " + (System.currentTimeMillis() - lastSended));
				lastSended = System.currentTimeMillis();

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				// Log.d(TAG, "onPreviewFrame thread 2");
				YuvImage image = new YuvImage(data, previewFormat, width, height, null);
				// Log.d(TAG, "onPreviewFrame thread 3");
				image.compressToJpeg(new Rect(0, 0, width, height), 50, out);
				// image.compressToJpeg(new Rect(0, 0, 2, 2), 20, out);
				// Log.d(TAG, "onPreviewFrame thread 4");
				byte[] imageBytes = out.toByteArray();
				// Log.d(TAG, "imageBytes.length = " + imageBytes.length);
				// byte[] part = new byte[256];
				// System.arraycopy(imageBytes, 0, part, 0, 256);
				// Log.d(TAG, "onPreviewFrame thread 5");
				BitmapContent content = new BitmapContent(imageBytes, width, height);
				// Log.d(TAG, "onPreviewFrame thread 6");
				manager.broadcast(content, transferListener);
				// Log.d(TAG, "end onPreviewFrame");
			}
		};

		//
		manager = new WifiDirectManager();
		manager.setListener(new WifiDirectServiceListener() {
			@Override
			public void onPeersChanged(List<Peer> peers) {
				// do nothing
			}

			@Override
			public void comein(MinimalMessage arg0) {
				// do nothing
			}

			@Override
			public void goout(MinimalMessage arg0) {
				// do nothing
			}

			@Override
			public void onDataReceived(Serializable object) {
			}
		});
		manager.bind(getApplicationContext());

	}

	@Override
	protected void onResume() {
		super.onResume();
		boolean result = preview.start(this);

		if (!result) {
			finish();
			return;
		}
		preview.setPreviewCallback(previewCallback);
	}

	@Override
	protected void onPause() {
		preview.stop();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		manager.unbind(getApplicationContext());
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
