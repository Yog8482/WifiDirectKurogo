package jp.ac.tuat.cs.nitoryu;

import java.io.Serializable;
import java.util.List;

import jp.ac.tuat.cs.wifidirectkurogo.WifiDirectManager;
import jp.ac.tuat.cs.wifidirectkurogo.WifiDirectServiceListener;
import jp.ac.tuat.cs.wifidirectkurogo.connection.TransferListener;
import jp.ac.tuat.cs.wifidirectkurogo.message.MinimalMessage;
import jp.ac.tuat.cs.wifidirectkurogo.peer.Peer;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class IntentReceiverActivity extends Activity {
	private final static String TAG = "IntentReceiverActivity";
	private WifiDirectManager manager;
	private SelectFragment selectFragment;
	private Intent intent;
	private ComponentName componentName;

	private TransferListener listener = new TransferListener() {
		@Override
		public void onProgress(int arg0) {
		}

		@Override
		public void onCompleted() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), "データの送信に成功しました", Toast.LENGTH_SHORT).show();
				}
			});
		}

		@Override
		public void onCancelled() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), "データの送信に失敗しました", Toast.LENGTH_SHORT).show();
				}
			});

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.intent_receiver_activity);

		intent = getIntent();
		componentName = new ComponentName(getApplicationContext(), getClass());
		selectFragment = new SelectFragment();

		Button broadcastButton = (Button) findViewById(R.id.broadcastButton);
		broadcastButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showStartToast(manager.broadcast(getApplicationContext(), intent, componentName, listener));
			}
		});

		Button selectButton = (Button) findViewById(R.id.selectButton);
		selectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.rootLayout, selectFragment);
				ft.addToBackStack(null);
				ft.commit();
				// commit を即反映させる
				getFragmentManager().executePendingTransactions();
				updateDeviceList(manager.getPeers());

				//
				Button sendButton = (Button) findViewById(R.id.sendButton);
				sendButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						List<String> to = selectFragment.getSelectedMACAddresses();
						boolean result = false;
						for(String mac : to){
							result = result || manager.send(getApplicationContext(), intent, componentName, mac, listener);
						}
						showStartToast(result);
					}
				});
			}
		});

		manager = new WifiDirectManager();
		manager.bind(getApplicationContext());
		manager.setListener(new WifiDirectServiceListener() {
			@Override
			public void onPeersChanged(List<Peer> peers) {
				Log.d(TAG, "onPeersChanged peers.size() = " + peers.size());
				// selectFragment.update(peers);
				updateDeviceList(peers);
			}

			@Override
			public void onDataReceived(Serializable content) {
				Log.d(TAG, "onDataReceived");
			}

			@Override
			public void comein(MinimalMessage arg0) {
			}

			@Override
			public void goout(MinimalMessage arg0) {
			}
		});

	}

	protected void showStartToast(boolean result) {
		if (result) {
			Toast.makeText(getApplicationContext(), "データの送信をバックグラウンドで開始しました", Toast.LENGTH_SHORT).show();
			finish();
		} else {
			Toast.makeText(getApplicationContext(), "データの送信に失敗しました", Toast.LENGTH_SHORT).show();
		}

	}

	protected void updateDeviceList(List<Peer> peers) {
		if (selectFragment != null) {
			Log.d(TAG, "deviceSelectFragment.update(peers);");
			selectFragment.update(peers);
		} else {
			Log.d(TAG, "deviceSelectFragment = null");
		}

	}

	private void viewIntentBundle(Intent intent) {
		Log.d(TAG, "viewIntentBundle " + intent.toURI());
		Bundle bundle = intent.getExtras();
		if (bundle == null) {
			Log.d(TAG, "bundle = null");
			return;
		}
		for (String key : bundle.keySet()) {
			Log.d(TAG, "bundle.get(" + key + ") = " + bundle.get(key));

		}
	}

	@Override
	protected void onDestroy() {
		if (manager != null) {
			manager.unbind(getApplicationContext());
		}
		super.onDestroy();
	}
}
