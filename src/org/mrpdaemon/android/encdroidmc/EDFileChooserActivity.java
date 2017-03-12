/*
 * encdroid - EncFS client application for Android
 * Copyright (C) 2012  Mark R. Pariente
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mrpdaemon.android.encdroidmc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mrpdaemon.android.encdroidmc.fileProvider.EncdroidFileProvider;
import org.mrpdaemon.android.encdroidmc.forceCloseManagement.ExceptionHandler;
import org.mrpdaemon.sec.encfs.EncFSFileInfo;
import org.mrpdaemon.sec.encfs.EncFSVolume;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class EDFileChooserActivity extends ListActivity {
	Handler threadErrorHandler;
	
	
	

	// Supported modes
	public final static int VOLUME_PICKER_MODE = 0;
	public final static int FILE_PICKER_MODE = 1;
	public final static int EXPORT_FILE_MODE = 2;
	public final static int CREATE_VOLUME_MODE = 3;
	public final static int FOLDER_PICKER_MODE = 4;

	// Parameter key for export file name
	public final static String EXPORT_FILE_KEY = "export_file";

	
	//public final static String REQUEST_KEY = "request";

	// Valid FS types
	public final static int LOCAL_FS = 0;
	public final static int EXT_SD_FS = 1;
	public final static int DROPBOX_FS = 2;
	//public final static int WEBDAV_FS = 3;

	
	// Parameter key for FS type
	//public final static String FILE_PROVIDER_INSTANCE_KEY = "fs_instance";
	// Result key for the path returned by this activity
	public final static String PARAM_KEY_PATH = "result_path";
	public final static String PARAM_KEY_PROVIDER = "result_provider";
	public final static String PARAM_KEY_MODE = "mode";
	//param sent by the sender and returned to it (to identify that we come from EDFileChooserActivity)
	public final static int REQUEST_CODE = 456;
	
	
	// Result key for the path returned by this activity
	public final static String ERROR_KEY = "result_eror";
	

	// Name of the SD card directory for copying files into
	public final static String ENCDROID_SD_DIR_NAME = "Encdroid";

	// Instance state bundle key for current directory
	public final static String CUR_DIR_KEY = "current_dir";

	// Logger tag
	private static final String TAG = "EDFileChooserActivity";

	// Dialog ID's
	private final static int DIALOG_AUTO_IMPORT = 0;

	// Adapter for the list
	private EDFileChooserAdapter mAdapter = null;

	// List that is currently being displayed
	private List<EDFileChooserItem> mCurFileList;

	// What mode we're running in
	private int mMode;

	// Current directory
	private String mCurrentDir;

	// The underlying FS type for this chooser
	//private int mFsType;

	// File provider
	private EncdroidFileProvider mFileProvider;

	// Application object
	private EDApplication mApp;

	// Action bar wrapper
	private EDActionBar mActionBar = null;

	// Text view for list header
	private TextView mListHeader = null;

	// File name being exported
	private String mExportFileName = null;

	// Shared preferences
	private SharedPreferences mPrefs = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
		threadErrorHandler = new ThreadErrorHandler(this.getApplicationContext()) ;
		
		setContentView(R.layout.file_chooser);

		//we come back from another activity (not from an android app restoration)
		if (savedInstanceState == null) {
			// New activity creation
			Bundle params = getIntent().getExtras();
			mMode = params.getInt(PARAM_KEY_MODE);
			mFileProvider = (EncdroidFileProvider)params.getSerializable(PARAM_KEY_PROVIDER);
			mExportFileName = params.getString(EXPORT_FILE_KEY);
			mCurrentDir = "/";
		} else {
			// Restoring previously killed activity
			mMode = savedInstanceState.getInt(PARAM_KEY_MODE);
			mFileProvider = (EncdroidFileProvider)savedInstanceState.getSerializable(PARAM_KEY_PROVIDER);
			mExportFileName = savedInstanceState.getString(EXPORT_FILE_KEY);
			mCurrentDir = savedInstanceState.getString(CUR_DIR_KEY);
		}

		mCurFileList = new ArrayList<EDFileChooserItem>();

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);


//		//we are be here:
//		//- when creating a new volume (we have to select the parent folder of the container on the distant filesystem)
//		//- when importing a new volume (we have to select the encrypted container on the distant filesystem)
//		//- when choosing a file from the local fs to upload it (with mFsType==0)
//		//starn test mFileProvider = EncdroidFileProvider.getNewProvider(mFsType, this);
//		if (mFsType==0){
//			mFileProvider=EncdroidFileProvider.getProvider(0, "", "/");
//			launchFillTask();
//		} else {
//			Intent i = new Intent(this, EDPreferenceActivity.class);
//			i.putExtra("idProvider", mFsType);
//			this.startActivityForResult(i,999);
//			//launchFillTask() is called by the intent
//		}

		launchFillTask();
		registerForContextMenu(this.getListView());

		mApp = (EDApplication) getApplication();

		if (mApp.isActionBarAvailable()) {
			mActionBar = new EDActionBar(this);
			mActionBar.setDisplayHomeAsUpEnabled(true);
		}

		mListHeader = new TextView(this);
		mListHeader.setTypeface(null, Typeface.BOLD);
		mListHeader.setTextSize(16);
		this.getListView().addHeaderView(mListHeader);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		throw new RuntimeException("EDFileChooserActivity.onActivityResult not used");
//		if(requestCode == 999) {//return from getNewProvider
//			if(resultCode == RESULT_OK) {
//				mFileProvider = (EncdroidFileProvider)data.getSerializableExtra("providerInstance");
//				launchFillTask();
//			}
//		}
	}

	
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(PARAM_KEY_MODE, mMode);
		outState.putSerializable(PARAM_KEY_PROVIDER, mFileProvider);
		outState.putString(EXPORT_FILE_KEY, mExportFileName);
		outState.putString(CUR_DIR_KEY, mCurrentDir);
		super.onSaveInstanceState(outState);
	}

	// Create the options menu
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.file_chooser_menu, menu);

		// Make the export item visible and hide the refresh item
		if (mMode == EXPORT_FILE_MODE || mMode == CREATE_VOLUME_MODE) {
			MenuItem item = menu.findItem(R.id.file_chooser_menu_export);
			item.setVisible(true);
			if (mMode == EXPORT_FILE_MODE) {
				item.setIcon(R.drawable.ic_menu_import_file);
			} else {
				item.setIcon(R.drawable.ic_menu_newvolume);
			}

			item = menu.findItem(R.id.file_chooser_menu_refresh);
			item.setVisible(false);
		}

		return super.onCreateOptionsMenu(menu);
	}

	// Modify options menu items
	
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mMode == CREATE_VOLUME_MODE) {
			MenuItem exportItem = menu.findItem(R.id.file_chooser_menu_export);
			exportItem.setTitle(R.string.menu_create_vol);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		AlertDialog alertDialog = null;

		switch (id) {
		case DIALOG_AUTO_IMPORT: // Auto import volume
			alertBuilder.setTitle(String
					.format(getString(R.string.auto_import_vol_dialog_str),
							mCurrentDir));
			alertBuilder.setPositiveButton(getString(R.string.btn_ok_str),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							returnResult(mCurrentDir);
						}
					});
			// Cancel button
			alertBuilder.setNegativeButton(getString(R.string.btn_cancel_str),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							dialog.cancel();
						}
					});
			alertDialog = alertBuilder.create();
			break;
		default:
			Log.d(TAG, "Unknown dialog ID requested " + id);
			return null;
		}

		return alertDialog;
	}

	// Handler for options menu selections
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.file_chooser_menu_export:
			returnResult(mCurrentDir);
			return true;
		case R.id.file_chooser_menu_refresh:
			launchFillTask();
			return true;
		case android.R.id.home:
			Log.v(TAG, "Home icon clicked");
			if (!mCurrentDir.equalsIgnoreCase("/")) {

				if (mCurrentDir.lastIndexOf("/") == 0) {
					mCurrentDir = "/";
				} else {
					mCurrentDir = mCurrentDir.substring(0,
							mCurrentDir.lastIndexOf("/"));
				}

				launchFillTask();
			} else {
				finish();
			}
		default:
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (!mCurrentDir.equalsIgnoreCase("/")) {

				if (mCurrentDir.lastIndexOf("/") == 0) {
					mCurrentDir = "/";
				} else {
					mCurrentDir = mCurrentDir.substring(0,
							mCurrentDir.lastIndexOf("/"));
				}

				launchFillTask();
			} else {
				finish();
			}

			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void returnResult(String path) {
		Intent intent = this.getIntent();
		intent.putExtra(PARAM_KEY_PATH, path);
		intent.putExtra(PARAM_KEY_PROVIDER,mFileProvider);
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	private void returnFailure() {
		Intent intent = this.getIntent();
		setResult(Activity.RESULT_FIRST_USER, intent);
		finish();
	}

	public boolean fill() {

		boolean configFileFound = false;
		List<EncFSFileInfo> childFiles = new ArrayList<EncFSFileInfo>();

		try {
			if (mFileProvider != null && mFileProvider.isReady()){
				childFiles = mFileProvider.listFiles(mCurrentDir);
			}
		} catch (Exception e) {
			//temp
			//if (1==1) throw new RuntimeException(e);
			
			//e.printStackTrace();
			Intent intent = this.getIntent();
			intent.putExtra(ERROR_KEY, e.getMessage()+": "+e.toString());
			setResult(Activity.RESULT_CANCELED, intent);
			
			
			finish();
			
			EDLogger.logException(TAG, e);
			return false;
		}

		// Set title from UI thread
		runOnUiThread(new Runnable() {
			
			public void run() {
				switch (mMode) {
				case VOLUME_PICKER_MODE:
//					if (mFsType == DROPBOX_FS) {
//						setTitle(getString(R.string.menu_import_vol) + " ("
//								+ getString(R.string.dropbox) + ")");
//					} else if (mFsType == EXT_SD_FS) {
//						setTitle(getString(R.string.menu_import_vol) + " ("
//								+ getString(R.string.fs_dialog_ext_sd) + ")");
//					} else {
//						setTitle(getString(R.string.menu_import_vol) + " ("
//								+ getString(R.string.fs_dialog_local) + ")");
//					}
					setTitle(getString(R.string.menu_import_vol) + " ("
							+ "add a title here" + ")");
					break;
				case FILE_PICKER_MODE:
					setTitle(getString(R.string.menu_import_files));
					break;
				case EXPORT_FILE_MODE:
					setTitle(String.format(getString(R.string.export_file),
							mExportFileName));
					break;
				case CREATE_VOLUME_MODE:
					setTitle(getString(R.string.menu_choose_folder) + ")");
					break;
				case FOLDER_PICKER_MODE:
					setTitle(getString(R.string.menu_choose_folder));
				break;
				}

				mListHeader.setText(mCurrentDir);
			}
		});

		List<EDFileChooserItem> directories = new ArrayList<EDFileChooserItem>();
		List<EDFileChooserItem> files = new ArrayList<EDFileChooserItem>();

		if (childFiles != null) {
			for (EncFSFileInfo file : childFiles) {
				if (file.isDirectory() && file.isReadable()) {
					directories.add(new EDFileChooserItem(file.getName(), true, file.getPath(), 0,BitmapFactory.decodeResource(getResources(), R.drawable.ic_folder)));
				} else {
					if (mMode == VOLUME_PICKER_MODE) {
						if (EncFSVolume.isConfigFile(file.getName()) && file.isReadable()) {
							files.add(new EDFileChooserItem(file.getName(), false, file.getPath(), file.getSize(),EDVolumeBrowserActivity.getIconForFile(this, file.getName())));
							configFileFound = true;
						}
					} else if (mMode == FILE_PICKER_MODE || mMode == EXPORT_FILE_MODE || mMode == CREATE_VOLUME_MODE) {
						if (file.isReadable()) {
							files.add(new EDFileChooserItem(file.getName(),	false, file.getPath(), file.getSize(),EDVolumeBrowserActivity.getIconForFile(this, file.getName())));
						}
					}
				}
			}
		}

		// Sort directories and files separately
		Collections.sort(directories);
		Collections.sort(files);

		// Merge directories + files into current file list
		mCurFileList.clear();
		mCurFileList.addAll(directories);
		mCurFileList.addAll(files);

		/*
		 * Add an item for the parent directory (..) in case where no ActionBar
		 * is present (API < 11). With ActionBar we use the Up icon for
		 * navigation.
		 */
		if ((mActionBar == null) && (!mCurrentDir.equalsIgnoreCase("/"))) {
			String parentPath;

			if (mCurrentDir.lastIndexOf("/") == 0) {
				parentPath = "/";
			} else {
				parentPath = mCurrentDir.substring(0, mCurrentDir.lastIndexOf("/"));
			}

			mCurFileList.add(0,new EDFileChooserItem("..", true, parentPath, 0, BitmapFactory.decodeResource(getResources(), R.drawable.ic_folder)));
		}

		if (mAdapter == null) {
			mAdapter = new EDFileChooserAdapter(this,
					R.layout.file_chooser_item, mCurFileList);

			// Set list adapter from UI thread
			runOnUiThread(new Runnable() {
				
				public void run() {
					setListAdapter(mAdapter);
				}
			});
		} else {
			// Notify data set change from UI thread
			runOnUiThread(new Runnable() {
				
				public void run() {
					mAdapter.notifyDataSetChanged();
				}
			});
		}

		return configFileFound;
	}

	// Show a progress spinner and launch the fill task
	public void launchFillTask() {
		new EDFileChooserFillTask().execute();
	}

	/*
	 * Task to fill the file chooser list. This is needed because fill() can end
	 * up doing network I/O with certain file providers and starting with API
	 * version 13 doing so results in a NetworkOnMainThreadException.
	 */
	private class EDFileChooserFillTask extends AsyncTask<Void, Void, Boolean> {

		private ProgressBar mProgBar;
		private ListView mListView;
		private LinearLayout mLayout;
		

		public EDFileChooserFillTask() {
			super();
		}

		
		protected void onPreExecute() {
			super.onPreExecute();

			// Replace the ListView with a ProgressBar
			mProgBar = new ProgressBar(EDFileChooserActivity.this, null,
					android.R.attr.progressBarStyleLarge);

			// Set the layout to fill the screen
			mListView = EDFileChooserActivity.this.getListView();
			mLayout = (LinearLayout) mListView.getParent();
			mLayout.setGravity(Gravity.CENTER);
			mLayout.setLayoutParams(new FrameLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

			// Set the ProgressBar in the center of the layout
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			layoutParams.gravity = Gravity.CENTER;
			mProgBar.setLayoutParams(layoutParams);

			// Replace the ListView with the ProgressBar
			mLayout.removeView(mListView);
			mLayout.addView(mProgBar);
			mProgBar.setVisibility(View.VISIBLE);
		}

		
		protected Boolean doInBackground(Void... arg0) {
//			try {
				return fill();
//			} catch (Exception e){
//				Message msg = new Message();
//				String textTochange = e.getMessage();
//				msg.obj = textTochange;
//				threadErrorHandler.sendMessage(msg);
//				return false;
//			}
		}

		
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);

			// Restore the layout parameters
			mLayout.setLayoutParams(new FrameLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			mLayout.setGravity(Gravity.TOP);

			// Remove the progress bar and replace it with the list view
			mLayout.removeView(mProgBar);
			mLayout.addView(mListView);

			if (result == true) {
				if (mPrefs.getBoolean("auto_import", true)) {
					showDialog(DIALOG_AUTO_IMPORT);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView,
	 * android.view.View, int, long)
	 */
	
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		// Use position - 1 since we have a list header
		if (position == 0) {
			return;
		}
		EDFileChooserItem selected = mAdapter.getItem(position - 1);

		if (selected.isDirectory()) {
			mCurrentDir = selected.getPath();
			launchFillTask();
		} else {
			if (mMode == VOLUME_PICKER_MODE) {
				returnResult(mCurrentDir);
			} else if (mMode == FILE_PICKER_MODE) {
				returnResult(selected.getPath());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
			case R.id.file_chooser_menu_select:
			case R.id.file_picker_menu_import:
				if (info.id >= 0) {
					EDFileChooserItem selected = mAdapter.getItem((int) info.id);
					returnResult(selected.getPath());
				}
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
	 * android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		if (mMode == FILE_PICKER_MODE) {
			inflater.inflate(R.menu.file_picker_context, menu);
		} else {
			inflater.inflate(R.menu.file_chooser_context, menu);
		}
	}
}