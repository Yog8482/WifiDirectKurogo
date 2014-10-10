package jp.ac.tuat.cs.nitoryu;

import java.util.ArrayList;
import java.util.List;

import jp.ac.tuat.cs.wifidirectkurogo.peer.Peer;
import android.app.Fragment;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SelectFragment extends Fragment {
	private ArrayAdapter<String> adapter;
	private ListView listView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.select_fragment, container, false);

		listView = (ListView) view.findViewById(R.id.listView);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_multiple_choice);
		listView.setAdapter(adapter);

		return view;
	}

	public void update(List<Peer> peers) {
		if (adapter == null) {
			return;
		}

		if (peers == null || peers.size() == 0) {
			return;
		}

		synchronized (adapter) {
			adapter.clear();
			for (Peer peer : peers) {
				if (peer.getHostAddress() != null && !peer.getHostAddress().equals("")
						&& peer.getNextMACAddress() != null && !peer.getNextMACAddress().equals("")) {
					adapter.add(peer.getName() + " / " + peer.getMacAddress());
				}
			}
			adapter.notifyDataSetChanged();
		}

	}

	public List<String> getSelectedMACAddresses() {
		List<String> ret = new ArrayList<String>();
		SparseBooleanArray array = listView.getCheckedItemPositions();
		int n = adapter.getCount();
		for (int i = 0; i < n; i++) {
			if (array.get(i)) {
				String line = adapter.getItem(i);
				ret.add(line.substring(line.indexOf("/") + 2));
			}
		}
		return ret;
	}
}
