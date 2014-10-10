package jp.ac.tuat.cs.anonymoustwitter;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

public class PostFragment extends Fragment {
	//
	private Button postButton;
	private EditText editText;
	private TextView Textcount;
	private TextView Preview;
	private Handler myHandler;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.post_fragment, container, false);

		myHandler = new Handler();

		// UI
		final ToggleButton ToggleButton[] = { (ToggleButton) view.findViewById(R.id.toggleButton1),
				(ToggleButton) view.findViewById(R.id.toggleButton2),
				(ToggleButton) view.findViewById(R.id.toggleButton3),
				(ToggleButton) view.findViewById(R.id.toggleButton4),
				(ToggleButton) view.findViewById(R.id.toggleButton5) };

		postButton = (Button) view.findViewById(R.id.postButton);
		postButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// つぶやき一覧画面に投稿
				String body = editText.getText().toString();
				TweetListFragment messageListFragment = ((MainActivity) getActivity()).getMessageListFragment();
				if (messageListFragment != null && body.length() != 0) {
					TweetContent tweet = createTweet(body);
					messageListFragment.add(tweet);

					// ミドルウェアを使って投稿
					((MainActivity) getActivity()).getManager().broadcast(tweet);

					// editTextをクリア
					editText.setText("");
					for (int i = 0; i < 5; i++) {
						ToggleButton[i].setChecked(false);
					}

					// ソフトウェアキーボードを閉じる
					InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(
							Context.INPUT_METHOD_SERVICE);
					inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
				}

				// ソフトウェアキーボードを閉じるアニメーションが終わった後につぶやき一覧画面に戻りたい
				// そのアニメーション終了のコールバックなどは取れないので，適当に500ms遅らせる
				myHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						// つぶやき一覧画面に戻る
						ViewPager viewPager = ((MainActivity) getActivity()).getViewPager();
						if (viewPager != null) {
							viewPager.arrowScroll(View.FOCUS_LEFT);
						}
					}
				}, 500);
			}
		});

		editText = (EditText) view.findViewById(R.id.editText);
		Textcount = (TextView) view.findViewById(R.id.countTextView);
		Preview = (TextView) view.findViewById(R.id.previewTextView);

		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				final int textColor;
				int length = s.length();
				if (length > 140) {
					textColor = Color.RED;
				} else {
					textColor = Color.WHITE;
				}
				Textcount.setTextColor(textColor);
				Textcount.setText(String.valueOf(length));

				String tmp = editText.getText().toString();
				tmp = tmp.replace("\n", "<br>");
				// Preview.setText("");
				Preview.setText(Html.fromHtml(tmp));
				// Preview.setText(tmp);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// TODO Auto-generated method stub
			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
			}
		});

		ToggleButton[0].setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (((ToggleButton) v).isChecked()) {
					editText.append("<b>");
				} else {
					editText.append("</b>");
				}

			}
		});

		ToggleButton[1].setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (((ToggleButton) v).isChecked()) {
					editText.append("<i>");
				} else {
					editText.append("</i>");
				}

			}
		});

		ToggleButton[2].setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (((ToggleButton) v).isChecked()) {
					editText.append("<u>");
				} else {
					editText.append("</u>");
				}

			}
		});

		ToggleButton[3].setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (((ToggleButton) v).isChecked()) {
					if (ToggleButton[4].isChecked()) {
						ToggleButton[4].toggle();
						editText.append("</sub>");
					}
					editText.append("<sup>");
				} else {
					editText.append("</sup>");
				}

			}
		});

		ToggleButton[4].setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (((ToggleButton) v).isChecked()) {
					if (ToggleButton[3].isChecked()) {
						ToggleButton[3].toggle();
						editText.append("</sup>");
					}
					editText.append("<sub>");
				} else {
					editText.append("</sub>");
				}

			}
		});

		return view;
	}

	private TweetContent createTweet(String body) {
		body = body.replace("\n", "<br>");
		return TweetContent.create(getActivity(), body);
	}

}
