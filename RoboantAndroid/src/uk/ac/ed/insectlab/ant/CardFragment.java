package uk.ac.ed.insectlab.ant;

import uk.co.ed.insectlab.ant.R;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;

public class CardFragment extends Fragment {

	private TextView mLabel;
	private ProgressBar mLoading;
	private TextView mStatusOK;
	private TextView mStatusError;

	RelativeLayout.LayoutParams mCardViewParams;

	public enum CardStatus {
		LOADING, OK, ERROR, NONE;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCardViewParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		mCardViewParams.addRule(RelativeLayout.BELOW, R.id.label);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		RelativeLayout view = (RelativeLayout)inflater.inflate(R.layout.fragment_card, container, false);

		mLabel = (TextView)view.findViewById(R.id.label);
		mLoading = (ProgressBar)view.findViewById(R.id.status);
		mStatusOK = (TextView)view.findViewById(R.id.statusOK);
		mStatusError = (TextView)view.findViewById(R.id.statusError);

		mLoading.setVisibility(View.INVISIBLE);
		mStatusError.setVisibility(View.INVISIBLE);
		mStatusOK.setVisibility(View.INVISIBLE);

		View cardView = onCreateCardView(inflater, view, savedInstanceState);

		if (cardView != null) {
			view.addView(cardView, mCardViewParams);
		}

		return view;
	}

	public void setLabel(final String label) {
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mLabel.setText(label);
			}
		});

	}

	public void setStatus(final CardStatus status) {
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mLoading.setVisibility(View.INVISIBLE);
				mStatusError.setVisibility(View.INVISIBLE);
				mStatusOK.setVisibility(View.INVISIBLE);

				switch (status) {
				case LOADING:
					mLoading.setVisibility(View.VISIBLE);
					break;
				case ERROR:
					mStatusError.setVisibility(View.VISIBLE);
					break;
				case OK:
					mStatusOK.setVisibility(View.VISIBLE);
					break;
				default:

				}

			}
		});

	}

	public View onCreateCardView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return null;
	}

}
