package uk.ac.ed.insectlab.ant;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.SimpleCursorAdapter;

public class RouteSelectionDialogFragment extends DialogFragment implements LoaderCallbacks<Cursor> {

	private static final String TAG = RouteSelectionDialogFragment.class.getSimpleName();
	private static final int[] LAYOUT_IDS = {android.R.id.text1};
	private static final String[] FILE_COLUMNS = {MediaStore.Files.FileColumns.TITLE};
	private SimpleCursorAdapter mAdapter;
	private RouteSelectedListener mListener;

	interface RouteSelectedListener {
		public void onRouteSelected(List<Bitmap> bitmap);
		public void onRecordRoute();
	}

	@Override
	public void onAttach(Activity activity) {
		mListener = (RouteSelectedListener)activity;
		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getLoaderManager().initLoader(0, null, this);
		mAdapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_1, null, FILE_COLUMNS, LAYOUT_IDS, SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
	}
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setAdapter(mAdapter, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Cursor cursor = (Cursor)mAdapter.getItem(which);
				String filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
				Log.i(TAG, "Clicked " + filePath);
				long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
				if (id == -1) {
					mListener.onRecordRoute();
					return;
				}

//				BitmaBitmapFactory.decodeFile(filePath);
				new LoadRecordedRouteTask(mListener).execute(filePath);
			}
		});
		return builder.create();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
//		Log.i(TAG, "onCreateLoader " + MediaStore.Files.getContentUri("external") + " " + Util.getOutputMediaDirUri(this));
		String[] projection = {
				//				MediaStore.Images.ImageColumns.TITLE
				MediaStore.Files.FileColumns.TITLE, MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DATA
		};
		//		String selection = 
		//				MediaStore.Files.FileColumns.TITLE + " LIKE ?";

		//		String[] selection_args = {
		//				"ROUTE_%"
		//		};
		String selection = 
				MediaStore.Files.FileColumns.TITLE + " LIKE ?";

		String[] selection_args = {
				"ROUTE_%"
		};
		//		return new CursorLoader(this, Util.getOutputMediaDirUri(this), projection, null, null, null);
		return new CursorLoader(getActivity(), MediaStore.Files.getContentUri("external"), projection, selection, selection_args, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onLoadFinished");
		String[] projection = {
				//				MediaStore.Images.ImageColumns.TITLE
				MediaStore.Files.FileColumns.TITLE, MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DATA
		};
		MatrixCursor extras = new MatrixCursor(projection);
		extras.addRow(new String[] {"Record new...", "-1", ""});
		mAdapter.swapCursor(new MergeCursor(new Cursor[] {extras, data}));
//		while (data.moveToNext()) {
//			String title = data.getString(data.getColumnIndex(MediaStore.Files.FileColumns.TITLE));
//			if (title != null) {
//				Log.i(TAG, title);
//			}
//			else {
//				Log.i(TAG, "title is null");
//			}
//		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub

	}
}