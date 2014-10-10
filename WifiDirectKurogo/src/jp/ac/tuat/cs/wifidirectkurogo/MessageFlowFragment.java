package jp.ac.tuat.cs.wifidirectkurogo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import jp.ac.tuat.cs.wifidirectkurogo.message.MinimalMessage;
import jp.ac.tuat.cs.wifidirectkurogo.message.MinimalMessage.Type;
import android.app.ListFragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MessageFlowFragment extends ListFragment {
	private MinimalMessageListAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.message_flow_fragment, container, false);

		// adapterを初期化
		adapter = new MinimalMessageListAdapter(getActivity(), R.layout.message_flow_cell,
				new ArrayList<MinimalMessage>());
		setListAdapter(adapter);

		return view;
	}
	
	public void add(MinimalMessage message, Type type){
		if(message == null){
			return;
		}
		message.setType(type);
		
		// リストの項目数を10-20程度に限定する
		if(adapter.getCount() > 20){
			// リストの先頭から10項目を削除
			for(int i=0; i<10; i++){
				adapter.remove(adapter.getItem(0));
			}
		}
		
		adapter.add(message);
		adapter.notifyDataSetChanged();
		
		// 末尾までスムーズにスクロール
		getListView().smoothScrollToPosition(adapter.getCount());
	}
	

	class MinimalMessageListAdapter extends ArrayAdapter<MinimalMessage> {
		private List<MinimalMessage> items;
		private LayoutInflater inflater;
		private int cellResourceId;
		private SimpleDateFormat sdf;

		public MinimalMessageListAdapter(Context context, int cellResourceId, List<MinimalMessage> items) {
			super(context, cellResourceId, items);
			this.items = items;
			this.cellResourceId = cellResourceId;
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final MinimalMessage mes = items.get(position);
			ViewHolder holder;
			if (convertView == null) {
				convertView = inflater.inflate(cellResourceId, null);
				holder = new ViewHolder();
				holder.rootLayout = (RelativeLayout) convertView.findViewById(R.id.rootLayout);
				holder.contentNameTextView = (TextView) convertView.findViewById(R.id.contentNameTextView);
				holder.toTextView = (TextView) convertView.findViewById(R.id.toTextView);
				holder.dateTextView = (TextView) convertView.findViewById(R.id.dateTextView);
				holder.fromTextView = (TextView) convertView.findViewById(R.id.fromTextView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.contentNameTextView.setText(mes.getContentClassName());
			if (mes.isFlooding()) {
				holder.toTextView.setText("to=(flooding)");
			} else {
				holder.toTextView.setText("to=" + mes.getTo());
			}

			holder.fromTextView.setText("from = " + mes.getFromString());

			if (mes.getType() == Type.UP) {
				holder.rootLayout.setBackgroundColor(Color.argb(25, 255, 200, 200));
			} else {
				holder.rootLayout.setBackgroundColor(Color.argb(100, 240, 255, 255));
			}
			
			holder.dateTextView.setText(sdf.format(mes.getID()));

			return convertView;
		}

		class ViewHolder {
			public RelativeLayout rootLayout;
			public TextView contentNameTextView;
			public TextView toTextView;
			public TextView dateTextView;
			public TextView fromTextView;
		}
	}
}
