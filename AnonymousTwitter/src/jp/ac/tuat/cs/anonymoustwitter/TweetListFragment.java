package jp.ac.tuat.cs.anonymoustwitter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TweetListFragment extends ListFragment {
	private TweetListAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.message_list_fragment, container, false);
		init();
		return view;
	}

	private void init() {
		ArrayList<TweetContent> ids = new ArrayList<TweetContent>();
		
		// デバッグ用項目
		long baseTime = System.currentTimeMillis() - (200000+50000);
		ids.add(new TweetContent((long) (Math.random()*Long.MAX_VALUE), baseTime, "テスト投稿"));
		ids.add(new TweetContent((long) (Math.random()*Long.MAX_VALUE), (long) (baseTime+50000*Math.random()), "文字コードテスト：①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳㍉㌔㌢㍍㌘㌧㌃㌶㍑㍗㌍㌦㌣㌫㍊㌻▂▅▇█▓▒░"));
		ids.add(new TweetContent((long) (Math.random()*Long.MAX_VALUE), (long) (baseTime+50000+50000*Math.random()), "複数行の投稿を認める<br>（ただし空行は削る）"));
		ids.add(new TweetContent((long) (Math.random()*Long.MAX_VALUE), (long) (baseTime+100000+50000*Math.random()), "一部のHTMLタグを認める：<br><u>下線</u><b>太字</b><br>&lt;&gt;&amp;&quot;&ndash;&mdash;&shy;&copy;&reg;&trade;&raquo;&Agrave;&Aacute;&Acirc;&Atilde;&Auml;&Aring;<sup>上付き</sup><sub>下付き</sub>"));
		ids.add(new TweetContent((long) (Math.random()*Long.MAX_VALUE), (long) (baseTime+150000+50000*Math.random()), "リンクは禁止：http://example.com<br>（テキストのコピーは可能）"));
		ids.add(new TweetContent((long) (Math.random()*Long.MAX_VALUE), (long) (baseTime+200000+50000*Math.random()), "アイコンは端末IDを元に紐付けられる"));
		ids.add(new TweetContent((long) (Math.random()*Long.MAX_VALUE), (long) (baseTime+250000+50000*Math.random()), "つぶやき一覧画面と投稿画面は左右スワイプで切り替える"));
		ids.add(new TweetContent((long) (Math.random()*Long.MAX_VALUE), (long) (baseTime+300000+50000*Math.random()), "android.support.v4.view.ViewPagerの呪い"));
		Collections.reverse(ids);

		adapter = new TweetListAdapter(getActivity(), R.layout.message_list_cell, ids);
		setListAdapter(adapter);
	}
	
	public void add(TweetContent tweet){
		if(tweet == null){
			return;
		}
		adapter.insert(tweet, 0);
		adapter.notifyDataSetChanged();
	}
	
	public void add(List<TweetContent> tweet){
		if(adapter == null){
			return;
		}
		if(tweet.size() == 0){
			return;
		}
		int tipCount = tweet.size();
		Collections.reverse(tweet); // 本当はadapter.insertしたいが・・・
		for(int i=0; i<adapter.getCount(); i++){
			if(tipCount + i > 500){
				break;
			}
			tweet.add(adapter.getItem(i));
		}
		adapter.clear();
		adapter.addAll(tweet);
		adapter.notifyDataSetChanged();
	}
}

class TweetListAdapter extends ArrayAdapter<TweetContent> {
	private List<TweetContent> items;
	private LayoutInflater inflater;
	private int cellResourceId;
	private SimpleDateFormat sdf;

	public TweetListAdapter(Context context, int cellResourceId, List<TweetContent> items) {
		super(context, cellResourceId, items);
		this.items = items;
		this.cellResourceId = cellResourceId;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TweetContent content = items.get(position);
		ViewHolder holder;
		if(convertView == null){
			 convertView = inflater.inflate(cellResourceId, null);
             holder = new ViewHolder(); convertView = inflater.inflate(cellResourceId, null);
             holder = new ViewHolder();
             holder.iconImageView = (ImageView) convertView.findViewById(R.id.iconImageView);
             holder.dateTextView = (TextView) convertView.findViewById(R.id.dateTextView);
             holder.bodyTextView = (TextView) convertView.findViewById(R.id.bodyTextView);
             convertView.setTag(holder);
		}else{
			holder = (ViewHolder) convertView.getTag();
		}
		int iconResourceId = content.getIconResourceId();
		if(iconResourceId != -1){
			holder.iconImageView.setImageResource(iconResourceId);
		}else{
			holder.iconImageView.setImageBitmap(null);
		}
		holder.dateTextView.setText(sdf.format(new Date(content.getDate())));
		holder.bodyTextView.setText(Html.fromHtml(content.getBody()));
		return convertView;
	}
	
	class ViewHolder{
		public ImageView iconImageView;
		public TextView dateTextView;
		public TextView bodyTextView;
	}
}