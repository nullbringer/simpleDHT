package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class OnDeleteClickListener implements OnClickListener {

	private static final String TAG = OnDeleteClickListener.class.getName();

	private final TextView mTextView;
	private final ContentResolver mContentResolver;
	private final Uri mUri;

	public OnDeleteClickListener(TextView _tv, ContentResolver _cr) {
		mTextView = _tv;
		mContentResolver = _cr;
		mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");

	}

	private Uri buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}



	@Override
	public void onClick(View v) {
		new Task().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private class Task extends AsyncTask<Void, String, Void> {

		@Override
		protected Void doInBackground(Void... params) {



            mContentResolver.delete(mUri, Constants.LOCAL_INDICATOR,null);

			publishProgress("");
			
			return null;
		}
		
		protected void onProgressUpdate(String...strings) {
			mTextView.setText(null);

			return;
		}
	}
}
