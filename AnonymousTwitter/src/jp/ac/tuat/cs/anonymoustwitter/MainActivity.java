package jp.ac.tuat.cs.anonymoustwitter;

import java.io.Serializable;
import java.util.List;

import jp.ac.tuat.cs.wifidirectkurogo.WifiDirectManager;
import jp.ac.tuat.cs.wifidirectkurogo.WifiDirectServiceListener;
import jp.ac.tuat.cs.wifidirectkurogo.message.Message;
import jp.ac.tuat.cs.wifidirectkurogo.message.MinimalMessage;
import jp.ac.tuat.cs.wifidirectkurogo.peer.Peer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends FragmentActivity {
	private WifiDirectManager manager;

	//
	private ViewPager viewPager;
	private TweetListFragment tweetListFragment;
	private PostFragment postFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

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
				Log.d("AnonymousTwitter","object = " + object);
				if(object instanceof TweetContent){
					tweetListFragment.add((TweetContent)object);
				}
				
			}
		});
		manager.bind(getApplicationContext());

		//
		viewPager = (ViewPager) findViewById(R.id.viewPager);
		tweetListFragment = (TweetListFragment) Fragment.instantiate(this, TweetListFragment.class.getName());
		postFragment = (PostFragment) Fragment.instantiate(this, PostFragment.class.getName());

		PagerAdapter adapter = new PagerAdapter(this);
		viewPager.setAdapter(adapter);
	}

	public WifiDirectManager getManager() {
		return manager;
	}
	
	public ViewPager getViewPager() {
		return viewPager;
	}

	public TweetListFragment getMessageListFragment() {
		return tweetListFragment;
	}

	public PostFragment getPostFragment() {
		return postFragment;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	// ViewPagerに設定するアダプタ
	class PagerAdapter extends FragmentPagerAdapter {
		public PagerAdapter(FragmentActivity activity) {
			super(activity.getSupportFragmentManager());
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public Fragment getItem(int position) {
			if(position == 0){
				return tweetListFragment;
			}else{
				return postFragment;
			}
		}
	}

	@Override
	protected void onDestroy() {
		manager.unbind(getApplicationContext());
		super.onDestroy();
	}
}
