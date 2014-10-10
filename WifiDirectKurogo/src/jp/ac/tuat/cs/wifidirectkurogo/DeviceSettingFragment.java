package jp.ac.tuat.cs.wifidirectkurogo;

import java.util.List;

import jp.ac.tuat.cs.wifidirectkurogo.peer.Peer;
import jp.ac.tuat.cs.wifidirectkurogo.peer.PeerManager;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

/**
 * PeerManager#getList を元に端末一覧と，設定機能を提供する Fragment
 */
public class DeviceSettingFragment extends ListFragment {
	private DeviceListAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.device_setting_fragment, container, false);

		// 使い捨て PeerManager を通して初期項目を読み込む
		PeerManager peerManager = new PeerManager(getActivity());
		// adapterを初期化
		adapter = new DeviceListAdapter(getActivity(), R.layout.device_setting_cell, peerManager.getList());
		setListAdapter(adapter);

		return view;
	}

	public void update(List<Peer> peers) {
		if (adapter == null) {
			return;
		}

		if (peers == null || peers.size() == 0) {
			return;
		}

		adapter.clear();
		adapter.addAll(peers);
		adapter.notifyDataSetChanged();
	}

	class DeviceListAdapter extends ArrayAdapter<Peer> {
		private List<Peer> items;
		private LayoutInflater inflater;
		private int cellResourceId;

		public DeviceListAdapter(Context context, int cellResourceId, List<Peer> items) {
			super(context, cellResourceId, items);
			this.items = items;
			this.cellResourceId = cellResourceId;
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final Peer peer = items.get(position);
			ViewHolder holder;
			if (convertView == null) {
				convertView = inflater.inflate(cellResourceId, null);
				holder = new ViewHolder();
				holder.nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);
				holder.statusTextView = (TextView) convertView.findViewById(R.id.statusTextView);
				holder.macAddressTextView = (TextView) convertView.findViewById(R.id.macAddressTextView);
				holder.nextMACAddressTextView = (TextView) convertView.findViewById(R.id.nextMACAddressTextView);
				holder.hostAddressTextView = (TextView) convertView.findViewById(R.id.hostAddressTextView);
				holder.authCheckBox = (CheckBox) convertView.findViewById(R.id.authCheckBox);

				if (peer.isMe()) {
					holder.statusTextView.setVisibility(View.GONE);
					holder.authCheckBox.setVisibility(View.GONE);
					holder.nextMACAddressTextView.setVisibility(View.GONE);
				}

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.nameTextView.setText(peer.getName());
			holder.statusTextView.setText(peer.isAvailable() ? "AVAILABLE" : "UNAVAILABLE");
			holder.macAddressTextView.setText(peer.getMacAddress());
			if (peer.getNextMACAddress().equals("")) {
				holder.nextMACAddressTextView.setText("");
			}else{
				holder.nextMACAddressTextView.setText("Next:" + peer.getNextMACAddress());
			}
			holder.hostAddressTextView.setText(peer.getHostAddress());
			holder.authCheckBox.setChecked(peer.isAuthed());
			holder.authCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// 使い捨て PeerManager
					PeerManager peerManager = new PeerManager(getContext());
					peerManager.update(peer.getMacAddress(), isChecked);
				}
			});

			return convertView;
		}

		class ViewHolder {
			public TextView nameTextView;
			public TextView statusTextView;
			public TextView macAddressTextView;
			public TextView nextMACAddressTextView;
			public TextView hostAddressTextView;
			public CheckBox authCheckBox;

		}
	}
}
