package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class OnLDumpClickListener implements OnClickListener {

	private static final String TAG = OnLDumpClickListener.class.getName();

	private final TextView mTextView;
	private final ContentResolver mContentResolver;
	private final Uri mUri;

	public OnLDumpClickListener(TextView _tv, ContentResolver _cr) {
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


			Cursor resultCursor = mContentResolver.query(mUri, null,
					Constants.LOCAL_INDICATOR, null, null);

			int count = 1;

			if (resultCursor.moveToFirst()) {
				while (!resultCursor.isAfterLast()) {
					String data = count++ + ". <"+ resultCursor.getString(resultCursor.getColumnIndex(Constants.KEY_FIELD)) +
							"," + resultCursor.getString(resultCursor.getColumnIndex(Constants.VALUE_FIELD)) +
							">\n";


					publishProgress(data);

					resultCursor.moveToNext();
				}
			}
			resultCursor.close();
			
			return null;
		}
		
		protected void onProgressUpdate(String...strings) {
			mTextView.append(strings[0]);

			return;
		}
	}
}
