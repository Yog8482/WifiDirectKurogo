package jp.ac.tuat.cs.wirelesscamera;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.List;

import jp.ac.tuat.cs.wifidirectkurogo.WifiDirectManager;
import jp.ac.tuat.cs.wifidirectkurogo.WifiDirectServiceListener;
import jp.ac.tuat.cs.wifidirectkurogo.message.MinimalMessage;
import jp.ac.tuat.cs.wifidirectkurogo.peer.Peer;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

public class ReceiveActivity extends Activity {
	private static final String TAG = "ReceiveActivity";
	//
	private WifiDirectManager manager;
	//
	private ImageView imageView;
	private boolean isDecoding;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		//
		setContentView(R.layout.activity_receive);
		imageView = (ImageView) findViewById(R.id.imageView);

		isDecoding = false;

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
				if (isDecoding) {
					return;
				}
				
				if (object instanceof BitmapContent) {
					//isDecoding = true;
					Log.d(TAG, "received BitmapContent");
					BitmapContent content = (BitmapContent) object;
					byte[] data = content.getData();
					Log.d(TAG, "data.length = " + data.length);
					Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
					imageView.setImageBitmap(bmp);
					//isDecoding = false;
				}
			}
		});
		manager.bind(getApplicationContext());

	}

	@Override
	protected void onDestroy() {
		manager.unbind(getApplicationContext());
		super.onDestroy();
	}

}
