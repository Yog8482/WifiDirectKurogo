package jp.ac.tuat.cs.wifidirectkurogo;

import java.io.Serializable;
import java.util.List;

import jp.ac.tuat.cs.wifidirectkurogo.connection.TransferListener;
import jp.ac.tuat.cs.wifidirectkurogo.message.MinimalMessage;
import jp.ac.tuat.cs.wifidirectkurogo.message.MinimalMessage.Type;
import jp.ac.tuat.cs.wifidirectkurogo.peer.Peer;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class ManageServiceActivity extends Activity {
	private WifiDirectManager manager;
	private DeviceSettingFragment settingFragment;
	private MessageFlowFragment flowFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.manage_service_activity);

		settingFragment = (DeviceSettingFragment) getFragmentManager().findFragmentByTag("device_setting");
		flowFragment = (MessageFlowFragment) getFragmentManager().findFragmentByTag("message_flow");
		//
		manager = new WifiDirectManager();
		manager.setListener(new WifiDirectServiceListener() {
			@Override
			public void onPeersChanged(List<Peer> peers) {
				settingFragment.update(peers);
			}

			@Override
			public void onDataReceived(Serializable content) {
			}

			@Override
			public void comein(MinimalMessage minimalMessage) {
				flowFragment.add(minimalMessage, Type.DOWN);
			}

			@Override
			public void goout(MinimalMessage minimalMessage) {
				flowFragment.add(minimalMessage, Type.UP);
			}
		});
		manager.start(ManageServiceActivity.this);
		manager.bind(getApplicationContext());
	}

	@Override
	protected void onResume() {
		super.onResume();
		List<Peer> peers = manager.getPeers();
		if (peers != null) {
			settingFragment.update(peers);
		}
	}

	@Override
	protected void onDestroy() {
		manager.unbind(getApplicationContext());
		// あえて manager.stop しない
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.manage_service_activity, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_test:
			final Intent intent = new Intent();
			
			// Action barのテストボタンの設定
			switch (1) {
			case 0:
				intent.setAction(Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_TEXT, "テキストをばらまく");
				break;
			case 1:
				intent.setAction(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.parse("http://www.tuat.ac.jp/"), "text/plain");
				break;
			case 2:
				intent.setAction(Intent.ACTION_VIEW);
				intent.addCategory(Intent.CATEGORY_DEFAULT);
				intent.setDataAndType(Uri.parse("file:///sdcard/TestData/mabo.jpg"), "image/jpeg");
				break;
			case 3:
				intent.setAction(Intent.ACTION_VIEW);
				intent.addCategory(Intent.CATEGORY_DEFAULT);
				intent.setDataAndType(Uri.parse("file:///sdcard/TestData/メルシールー.mp3"), "audio/*");
				break;
			default:
				break;
			}

			final ProgressDialog dialog = new ProgressDialog(ManageServiceActivity.this);
			dialog.setTitle("インテント送信中");
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.show();

			manager.broadcast(getApplicationContext(), intent, new TransferListener() {
				@Override
				public void onProgress(int value) {
					Log.d("TEST", "onProgress");
				}

				@Override
				public void onCompleted() {
					Log.d("TEST", "onCompleted");
					dialog.dismiss();
				}

				@Override
				public void onCancelled() {
					Log.d("TEST", "onCancelled");
					dialog.dismiss();
				}
			});
			break;
		case R.id.menu_about:

			break;
		case R.id.menu_stop:
			showStopServiceDialog();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void showStopServiceDialog() {
		new AlertDialog.Builder(ManageServiceActivity.this).setTitle("WiFiDirectKurogo")
				.setMessage("WiFiDirectKurogoを終了しますか？").setPositiveButton("はい", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						manager.stop(ManageServiceActivity.this);
						Toast.makeText(getApplicationContext(), "WiFiDirectKurogoを終了しました", Toast.LENGTH_SHORT).show();

						NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
						notificationManager.cancelAll();

						finish();
					}
				}).setNegativeButton("いいえ", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}).setOnCancelListener(new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
					}
				}).show();
	}
}
