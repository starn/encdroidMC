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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.mrpdaemon.android.encdroidmc.asyncTasks.ActivityRestoreTask;
import org.mrpdaemon.android.encdroidmc.asyncTasks.EDFileObserver;
import org.mrpdaemon.android.encdroidmc.asyncTasks.ExportFileTask;
import org.mrpdaemon.android.encdroidmc.asyncTasks.FillTask;
import org.mrpdaemon.android.encdroidmc.asyncTasks.ImportFileTask;
import org.mrpdaemon.android.encdroidmc.asyncTasks.MetadataOpTask;
import org.mrpdaemon.android.encdroidmc.asyncTasks.NotificationHelper;
import org.mrpdaemon.android.encdroidmc.asyncTasks.PasteFileTask;
import org.mrpdaemon.android.encdroidmc.asyncTasks.SaveFileTask;
import org.mrpdaemon.android.encdroidmc.asyncTasks.SaveFromFSFileTask;
import org.mrpdaemon.android.encdroidmc.asyncTasks.ThumbnailsTask;
import org.mrpdaemon.android.encdroidmc.asyncTasks.ViewFileTask;
import org.mrpdaemon.android.encdroidmc.fileProvider.EncdroidFileProvider;
import org.mrpdaemon.android.encdroidmc.fileProvider.IStatefullSession;
import org.mrpdaemon.android.encdroidmc.forceCloseManagement.ExceptionHandler;
import org.mrpdaemon.android.encdroidmc.tools.SecureDeleteThread;
import org.mrpdaemon.sec.encfs.EncFSFile;
import org.mrpdaemon.sec.encfs.EncFSFileProvider;
import org.mrpdaemon.sec.encfs.EncFSVolume;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import fr.starn.PersistantInstanceManager;
import fr.starn.webdavServer.HttpServer;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class EDVolumeBrowserActivity extends ListActivity {

	// Parameter key for specifying volume index
	public final static String VOL_ID_KEY = "vol_id";

	// Name of the SD card directory for copying files into
	public final static String ENCDROID_SD_DIR_NAME = "Encdroid";

	// Request ID's for calling into different activities
	public final static int VIEW_FILE_REQUEST = 0;
	public final static int PICK_FILE_REQUEST = 1;
	public final static int EXPORT_FILE_REQUEST = 2;
	
	public final static int EDIT_TEXT_FILE = 3;

	// Saved instance state keys
	public final static String SAVED_CUR_DIR_PATH_KEY = "cur_dir_path";
	private final static String SAVED_PASTE_MODE_KEY = "paste_mode";
	public final static String SAVED_PASTE_FILE_PATH_KEY = "paste_file_path";
	private final static String SAVED_OPEN_FILE_PATH_KEY = "open_file_path";
	private final static String SAVED_OPEN_FILE_NAME_KEY = "open_file_name";
	private final static String SAVED_IMPORT_FILE_NAME_KEY = "import_file_name";
	private final static String SAVED_ASYNC_TASK_ID_KEY = "async_task_id";
//	private final static String SAVED_PROGRESS_BAR_MAX_KEY = "prog_bar_max";
//	private final static String SAVED_PROGRESS_BAR_PROG_KEY = "prog_bar_prog";
//	private final static String SAVED_PROGRESS_BAR_STR_ARG_KEY = "prog_bar_strarg";

	// Dialog ID's
	public final static int DIALOG_ERROR = 0;
	private final static int DIALOG_FILE_RENAME = 1;
	private final static int DIALOG_FILE_DELETE = 2;
	private final static int DIALOG_CREATE_FOLDER = 3;
	private final static int DIALOG_CREATE_FILE = 4;

	// Async task ID's
	private final static int ASYNC_TASK_SYNC = 0;
	public final static int ASYNC_TASK_IMPORT = 1;
	private final static int ASYNC_TASK_DECRYPT = 2;
	private final static int ASYNC_TASK_EXPORT = 3;
	private final static int ASYNC_TASK_RENAME = 4;
	private final static int ASYNC_TASK_DELETE = 5;
	private final static int ASYNC_TASK_CREATE_DIR = 6;
	private final static int ASYNC_TASK_PASTE = 7;
	private final static int ASYNC_TASK_SAVE_EXTERNAL_APP_FILE_HERE = 8;
	private final static int ASYNC_TASK_CREATE_FILE= 9;
	
	public ThumbnailsTask thumbnailsTask;
	private static Bitmap defaultFileBitmap;
	private static Bitmap defaultFolderBitmap;

	// Logger tag
	private final static String TAG = "EDVolumeBrowserActivity";

	// Adapter for the list
	private EDFileChooserAdapter mAdapter = null;

	// List that is currently being displayed
	public List<EDFileChooserItem> mCurFileList;

	// EDVolume
	private EDVolume mEDVolume;

	// EncFS volume
	private EncFSVolume mVolume;

	// Directory stack
	public Stack<EncFSFile> mDirStack;

	// Current directory
	public EncFSFile mCurEncFSDir;

	// Application object
	public EDApplication mApp;

	// Text for the error dialog
	public String mErrDialogText = "";

	// Progress dialog for async progress
	//private ProgressDialog mProgDialog = null;
	private NotificationHelper mProgDialog;

	// Async task object
	private EDAsyncTask<Void, Void, Boolean> mAsyncTask = null;

	// Async task ID
	private int mAsyncTaskId = -1;

	// Fill task object
	private AsyncTask<Void, Void, Void> mFillTask = null;

	// File observer
	public EDFileObserver mFileObserver;

	// EncFSFile that is currently opened
	private EncFSFile mOpenFile;

	// Path to the opened file (used during restore of mOpenFile)
	public String mOpenFilePath = null;

	// Name of the opened file (used for display purposes)
	public String mOpenFileName = null;

	// Name of the file being imported
	private String mImportFileName;

	// File that is currently selected
	public EDFileChooserItem mSelectedFile;

	// EncFSFile that is being pasted
	public EncFSFile mPasteFile = null;

	// Paste operations
	public static final int PASTE_OP_NONE = 0;
	public static final int PASTE_OP_CUT = 1;
	public static final int PASTE_OP_COPY = 2;

	// Paste mode
	public int mPasteMode = PASTE_OP_NONE;

	// Broadcast receiver to monitor external storage state
	//BroadcastReceiver mExternalStorageReceiver;

	// Whether external storage is available
	boolean mExternalStorageAvailable = false;

	// Whether external storage is writable
	boolean mExternalStorageWriteable = false;

	// Action bar wrapper
	public EDActionBar mActionBar = null;

	// Text view for list header
	private TextView mListHeader = null;
	
	//files list displayed
	public static EncFSFile[] childEncFSFiles = null;
	
	//mediaplayer to play music in app
	//private MediaPlayer mediaPlayer;
	
	public static boolean isCtxImportFromExternalApp = EDVolumeListActivity.fileNameImportedFromExternalApp!=null;
	private MenuItem saveExternalFileMenuItem;

	// Class to hold context for restoring an activity after being recreated
	private class ActivityRestoreContext {
		public EDVolume savedVolume;
		public EDFileObserver savedObserver;
		public EDFileChooserItem savedSelectedFile;
		public EDAsyncTask<Void, Void, Boolean> savedTask;
	}

	// Saved instance state for current EncFS directory
	public String mSavedCurDirPath = null;

	// Saved instance state for paste file path
	private String mSavedPasteFilePath = null;

	// Saved instance state for progress bar string argument
	private String mSavedProgBarStrArg = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
		
		
		mApp = (EDApplication) getApplication();

		
		mCurFileList = new ArrayList<EDFileChooserItem>();

		// Start monitoring external storage state
//		mExternalStorageReceiver = new BroadcastReceiver() {
//			
//			public void onReceive(Context context, Intent intent) {
//				updateExternalStorageState();
//			}
//		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_REMOVED);
		//registerReceiver(mExternalStorageReceiver, filter);
		updateExternalStorageState();

//		if (mExternalStorageAvailable == false) {
//			Log.e(TAG, "No SD card is available");
//			mErrDialogText = getString(R.string.error_no_sd_card);
//			showDialog(DIALOG_ERROR);
//			finish();
//		}

		// Restore UI elements
		super.onCreate(savedInstanceState);

		setContentView(R.layout.file_chooser);

		registerForContextMenu(this.getListView());

		if (mApp.isActionBarAvailable()) {
			mActionBar = new EDActionBar(this);
			mActionBar.setDisplayHomeAsUpEnabled(true);
		}

		mListHeader = new TextView(this);
		mListHeader.setTypeface(null, Typeface.BOLD);
		mListHeader.setTextSize(16);
		this.getListView().addHeaderView(mListHeader);


		
		if (savedInstanceState == null) {
			// Activity being created for the first time
			Bundle params = getIntent().getExtras();
			int position = params.getInt(VOL_ID_KEY);
			mEDVolume = mApp.getVolumeList().get(position);
			mVolume = mEDVolume.getVolume();
			mCurEncFSDir = mVolume.getRootDir();
			mDirStack = new Stack<EncFSFile>();
			
			launchFillTask(true);
		} else {

			
			
			// Activity being recreated
			ActivityRestoreContext restoreContext = (ActivityRestoreContext) getLastNonConfigurationInstance();
			if (restoreContext == null) {
				/*
				 * If getLastNonConfigurationInstance() returned null the
				 * activity was killed due to low memory and is being recreated.
				 * Unfortunately we've lost all the volume state at that point
				 * so we don't have any choice but to exit back to the volume
				 * list and start over.
				 */
				exitToVolumeList();
				return;
			}
			mEDVolume = restoreContext.savedVolume;
			mVolume = mEDVolume.getVolume();
			mCurEncFSDir = mVolume.getRootDir();
			mDirStack = new Stack<EncFSFile>();
			mFileObserver = restoreContext.savedObserver;
			mSelectedFile = restoreContext.savedSelectedFile;
			mAsyncTask = restoreContext.savedTask;

			mSavedCurDirPath = savedInstanceState
					.getString(SAVED_CUR_DIR_PATH_KEY);

			// Restore open file state
			mOpenFile = null;
			String openFilePath = savedInstanceState
					.getString(SAVED_OPEN_FILE_PATH_KEY);
			String openFileName = savedInstanceState
					.getString(SAVED_OPEN_FILE_NAME_KEY);
			if (!openFilePath.equals("")) {
				mOpenFilePath = openFilePath;
				mOpenFileName = openFileName;
			} else {
				mOpenFilePath = null;
				mOpenFileName = null;
			}

			// Restore paste mode
			mPasteMode = savedInstanceState.getInt(SAVED_PASTE_MODE_KEY);
			mSavedPasteFilePath = savedInstanceState
					.getString(SAVED_PASTE_FILE_PATH_KEY);

			// Restore import file state
			mImportFileName = savedInstanceState
					.getString(SAVED_IMPORT_FILE_NAME_KEY);

			// Restore async task ID
			mAsyncTaskId = savedInstanceState.getInt(SAVED_ASYNC_TASK_ID_KEY);

			if (mAsyncTask != null) {
				// Create new progress dialog and replace the old one
//				createProgressBarForTask(mAsyncTaskId,
//						savedInstanceState
//								.getString(SAVED_PROGRESS_BAR_STR_ARG_KEY));
//				mProgDialog.setMax(savedInstanceState
//						.getInt(SAVED_PROGRESS_BAR_MAX_KEY));
//				mProgDialog.setProgress(savedInstanceState
//						.getInt(SAVED_PROGRESS_BAR_PROG_KEY));
				mAsyncTask.setProgressDialog(mProgDialog);

				// Fix the activity for the task
				mAsyncTask.setActivity(this);
			}

			// Execute async task to restore instance state
			if (mProgDialog == null || !mProgDialog.isShowing()) {
//				mProgDialog = new ProgressDialog(EDVolumeBrowserActivity.this);
//				mProgDialog.setTitle(getString(R.string.loading_contents));
//				mProgDialog.setCancelable(false);
//				mProgDialog.show();
				mProgDialog = new NotificationHelper(this.getApplicationContext());
				new ActivityRestoreTask(this,mProgDialog, savedInstanceState)
						.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				new ActivityRestoreTask(this,null, savedInstanceState).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		}
		
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
		    getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
		}
	}

	

	
	// Retain the EDVolume object through activity being killed
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		ActivityRestoreContext restoreContext = new ActivityRestoreContext();
		restoreContext.savedVolume = mEDVolume;
		restoreContext.savedObserver = mFileObserver;
		restoreContext.savedSelectedFile = mSelectedFile;
		if (mAsyncTask != null
				&& mAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
			// Clear progress bar so we don't leak it
			mProgDialog.dismiss();
			//mAsyncTask.setProgressDialog(null);
			// Clear the activity so we don't leak it
			mAsyncTask.setActivity(null);
			restoreContext.savedTask = mAsyncTask;
		} else {
			restoreContext.savedTask = null;
		}
		return restoreContext;
	}

	// Retain state information through activity being killed
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (mCurEncFSDir == null) {
			// Being recreated again before EDRestoreActivityTask ran
			outState.putString(SAVED_CUR_DIR_PATH_KEY, mSavedCurDirPath);
		} else {
			outState.putString(SAVED_CUR_DIR_PATH_KEY, mCurEncFSDir.getPath());
		}
		outState.putInt(SAVED_PASTE_MODE_KEY, mPasteMode);
		if (mPasteMode != PASTE_OP_NONE) {
			if (mPasteFile == null) {
				outState.putString(SAVED_PASTE_FILE_PATH_KEY,
						mSavedPasteFilePath);
			} else {
				outState.putString(SAVED_PASTE_FILE_PATH_KEY,
						mPasteFile.getPath());
			}
		}
		if (mOpenFile != null) {
			outState.putString(SAVED_OPEN_FILE_PATH_KEY, mOpenFile.getPath());
			outState.putString(SAVED_OPEN_FILE_NAME_KEY, mOpenFile.getName());
		} else {
			if (mOpenFilePath != null) {
				outState.putString(SAVED_OPEN_FILE_PATH_KEY, mOpenFilePath);
			} else {
				outState.putString(SAVED_OPEN_FILE_PATH_KEY, "");
			}
			if (mOpenFileName != null) {
				outState.putString(SAVED_OPEN_FILE_NAME_KEY, mOpenFileName);
			} else {
				outState.putString(SAVED_OPEN_FILE_NAME_KEY, "");
			}
		}
		outState.putString(SAVED_IMPORT_FILE_NAME_KEY, mImportFileName);
		outState.putInt(SAVED_ASYNC_TASK_ID_KEY, mAsyncTaskId);

//		if (mProgDialog != null) {
//			outState.putLong(SAVED_PROGRESS_BAR_MAX_KEY, mProgDialog.getMax());
//			outState.putLong(SAVED_PROGRESS_BAR_PROG_KEY,
//					mProgDialog.getProgress());
//			outState.putString(SAVED_PROGRESS_BAR_STR_ARG_KEY,
//					mSavedProgBarStrArg);
//		}

		super.onSaveInstanceState(outState);
	}

	// Clean stuff up
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
//		if (mExternalStorageAvailable) {
//			unregisterReceiver(mExternalStorageReceiver);
//		}
		if (mFillTask != null
				&& mFillTask.getStatus() == AsyncTask.Status.RUNNING) {
			mFillTask.cancel(true);
		}
		if (thumbnailsTask!=null) thumbnailsTask.cancel(true);
	}

	// Create the options menu
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.volume_browser_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	// Modify options menu items
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem pasteItem = menu.findItem(R.id.volume_browser_menu_paste);
		if (mPasteMode != PASTE_OP_NONE) {
			pasteItem.setVisible(true);
		} else {
			pasteItem.setVisible(false);
		}

		
		saveExternalFileMenuItem = menu.findItem(R.id.volume_browser_menu_saveHere);
		saveExternalFileMenuItem.setVisible(isCtxImportFromExternalApp);
		if (isCtxImportFromExternalApp) this.setTitle("Location for: "+EDVolumeListActivity.fileNameImportedFromExternalApp);
		return super.onPrepareOptionsMenu(menu);
	}

	// Handler for options menu selections
	// can be called when importing a new local file to the volume
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		
		//import a new file in the volume
		case R.id.volume_browser_menu_import:
			Intent startFileChooser = new Intent(this, EDFileChooserActivity.class);

			startFileChooser.putExtra(EDFileChooserActivity.PARAM_KEY_MODE, EDFileChooserActivity.FILE_PICKER_MODE);
			startFileChooser.putExtra(EDFileChooserActivity.PARAM_KEY_PROVIDER, EncdroidFileProvider.getLocalFileSystemProvider());
			startActivityForResult(startFileChooser, PICK_FILE_REQUEST);

			return true;
		case R.id.volume_browser_menu_import_external:
			Intent startFileChooserExternal = new Intent(this, EDFileChooserActivity.class);

			startFileChooserExternal.putExtra(EDFileChooserActivity.PARAM_KEY_MODE, EDFileChooserActivity.FILE_PICKER_MODE);
			startFileChooserExternal.putExtra(EDFileChooserActivity.PARAM_KEY_PROVIDER, EncdroidFileProvider.getExternalFileSystemProvider());
			startActivityForResult(startFileChooserExternal, PICK_FILE_REQUEST);

			return true;		
		case R.id.volume_browser_menu_saveHere:
			System.out.println("save here");
			launchAsyncTask(ASYNC_TASK_SAVE_EXTERNAL_APP_FILE_HERE, null, null);
			return true;
		//create a new folder in the volume
		case R.id.volume_browser_menu_mkdir:
			showDialog(DIALOG_CREATE_FOLDER);
			return true;
		case R.id.volume_browser_menu_createfile:
			showDialog(DIALOG_CREATE_FILE);
			return true;
		case R.id.volume_browser_menu_syncfolder:
			Intent configSyncFolder = new Intent(this, ConfigSyncActivity.class);
			configSyncFolder.putExtra("volumeName", this.getmVolume().getVolumeName());
			configSyncFolder.putExtra("volumePath", this.mCurEncFSDir.getPath());
			startActivity(configSyncFolder);
			return true;
		case R.id.volume_browser_menu_paste:
			// Launch async task to paste file
			createProgressBarForTask(ASYNC_TASK_PASTE, null);
			mAsyncTask = new PasteFileTask(this,mProgDialog);
			mAsyncTaskId = ASYNC_TASK_PASTE;
			mAsyncTask.setActivity(this);
			mAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

			return true;
		case R.id.volume_browser_menu_refresh:
			EncFSFileProvider fileProvider = mVolume.getFileProvider();
			if (fileProvider instanceof IStatefullSession){
				((IStatefullSession)fileProvider).clearSession();
			}
			
			launchFillTask(false);
			return true;
		case R.id.volume_browser_generate_thumbnails:
			//launchFillTask(false, true);
			thumbnailsTask = new ThumbnailsTask(this,null,false,false);
			thumbnailsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			return true;
		case R.id.volume_browser_generate_thumbnails_vid:
			//launchFillTask(false, true);
			thumbnailsTask = new ThumbnailsTask(this,null,false,true);
			thumbnailsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			return true;
		case R.id.volume_browser_exit:
			System.exit(0);
			return false;
		case android.R.id.home:
			if (mCurEncFSDir == mVolume.getRootDir()) {
				// Go back to volume list
				Intent intent = new Intent(this, EDVolumeListActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			} else {
				mCurEncFSDir = mDirStack.pop();
				launchFillTask(false);
			}
			return true;
		default:
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (thumbnailsTask!=null)  thumbnailsTask.cancel();
			if (mCurEncFSDir == mVolume.getRootDir()) {
				// Go back to volume list
				Intent intent = new Intent(this, EDVolumeListActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			} else {
				mCurEncFSDir = mDirStack.pop();
				launchFillTask(false);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
	 */
	
	@Override
	protected void onPrepareDialog(int id, final Dialog dialog) {
		EditText input;
		AlertDialog ad = (AlertDialog) dialog;

		switch (id) {
		case DIALOG_FILE_DELETE:
			ad.setTitle(String.format(getString(R.string.del_dialog_title_str),
					mSelectedFile.getName()));
			break;
		case DIALOG_FILE_RENAME:
		case DIALOG_CREATE_FOLDER:
			input = (EditText) dialog.findViewById(R.id.dialog_edit_text);
			if (input != null) {
				if (id == DIALOG_FILE_RENAME) {
					input.setText(mSelectedFile.getName());
				} else if (id == DIALOG_CREATE_FOLDER) {
					input.setText("");
				}
			} else {
				Log.e(TAG,
						"dialog.findViewById returned null for dialog_edit_text");
			}

			/*
			 * We want these dialogs to immediately proceed when the user taps
			 * "Done" in the keyboard, so we create an EditorActionListener to
			 * catch the DONE action and trigger the positive button's onClick()
			 * event.
			 */
			input.setImeOptions(EditorInfo.IME_ACTION_DONE);
			input.setOnEditorActionListener(new OnEditorActionListener() {
				
				@Override
				public boolean onEditorAction(TextView v, int actionId,
						KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						Button button = ((AlertDialog) dialog)
								.getButton(DialogInterface.BUTTON_POSITIVE);
						button.performClick();
						return true;
					}
					return false;
				}
			});

			break;
		case DIALOG_CREATE_FILE:
			input = (EditText) dialog.findViewById(R.id.dialog_edit_text);
			if (input != null) {
				input.setText("");
			} else {
				Log.e(TAG,
						"dialog.findViewById returned null for dialog_edit_text");
			}

			/*
			 * We want these dialogs to immediately proceed when the user taps
			 * "Done" in the keyboard, so we create an EditorActionListener to
			 * catch the DONE action and trigger the positive button's onClick()
			 * event.
			 */
			input.setImeOptions(EditorInfo.IME_ACTION_DONE);
			input.setOnEditorActionListener(new OnEditorActionListener() {
				
				@Override
				public boolean onEditorAction(TextView v, int actionId,
						KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						Button button = ((AlertDialog) dialog)
								.getButton(DialogInterface.BUTTON_POSITIVE);
						button.performClick();
						return true;
					}
					return false;
				}
			});

			break;
		case DIALOG_ERROR:
			ad.setMessage(mErrDialogText);
			break;
		default:
			break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	
	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		AlertDialog alertDialog = null;

		LayoutInflater inflater = LayoutInflater.from(this);

		final EditText input;
		final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

		switch (id) {
		case DIALOG_FILE_RENAME: // Rename file dialog

			input = (EditText) inflater.inflate(R.layout.dialog_edit, null);

			alertBuilder.setTitle(getString(R.string.frename_dialog_title_str));
			alertBuilder.setView(input);
			alertBuilder.setPositiveButton(getString(R.string.btn_ok_str),
					new DialogInterface.OnClickListener() {
						// Rename the file
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							Editable value = input.getText();
							launchAsyncTask(ASYNC_TASK_RENAME, value.toString());
						}
					});
			// Cancel button
			alertBuilder.setNegativeButton(getString(R.string.btn_cancel_str),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							dialog.cancel();
						}
					});
			alertDialog = alertBuilder.create();

			// Show keyboard
			alertDialog.setOnShowListener(new OnShowListener() {

				
				@Override
				public void onShow(DialogInterface dialog) {
					imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
				}
			});
			break;
		case DIALOG_CREATE_FOLDER: // Create folder dialog

			input = (EditText) inflater.inflate(R.layout.dialog_edit, null);

			alertBuilder.setTitle(getString(R.string.mkfile_dialog_input_str));
			alertBuilder.setView(input);
			alertBuilder.setPositiveButton(getString(R.string.btn_ok_str),
					new DialogInterface.OnClickListener() {
						// Create the folder
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							Editable value = input.getText();
							launchAsyncTask(ASYNC_TASK_CREATE_DIR,
									value.toString());
						}
					});
			// Cancel button
			alertBuilder.setNegativeButton(getString(R.string.btn_cancel_str),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							dialog.cancel();
						}
					});
			alertDialog = alertBuilder.create();

			// Show keyboard
			alertDialog.setOnShowListener(new OnShowListener() {

				
				@Override
				public void onShow(DialogInterface dialog) {
					imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
				}
			});
			break;
			
		case DIALOG_CREATE_FILE: // Create folder dialog

			input = (EditText) inflater.inflate(R.layout.dialog_edit, null);

			alertBuilder.setTitle(getString(R.string.mkdir_dialog_input_str));
			alertBuilder.setView(input);
			alertBuilder.setPositiveButton(getString(R.string.btn_ok_str),
					new DialogInterface.OnClickListener() {
						// Create the folder
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							Editable value = input.getText();
							launchAsyncTask(ASYNC_TASK_CREATE_FILE,
									value.toString());
						}
					});
			// Cancel button
			alertBuilder.setNegativeButton(getString(R.string.btn_cancel_str),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							dialog.cancel();
						}
					});
			alertDialog = alertBuilder.create();

			// Show keyboard
			alertDialog.setOnShowListener(new OnShowListener() {

				
				@Override
				public void onShow(DialogInterface dialog) {
					imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
				}
			});
			break;
			
			
		case DIALOG_FILE_DELETE:
			alertBuilder.setTitle(String.format(
					getString(R.string.del_dialog_title_str),
					mSelectedFile.getName()));
			alertBuilder.setCancelable(false);
			alertBuilder.setPositiveButton(getString(R.string.btn_yes_str),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							// Delete the file
							launchAsyncTask(ASYNC_TASK_DELETE, null);
						}
					});
			alertBuilder.setNegativeButton(getString(R.string.btn_no_str),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
			alertDialog = alertBuilder.create();
			break;
		case DIALOG_ERROR:
			alertBuilder.setMessage("Volume Browser Error: "+mErrDialogText);
			alertBuilder.setCancelable(false);
			alertBuilder.setNeutralButton(getString(R.string.btn_ok_str),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							dialog.dismiss();
						}
					});
			try {
				alertDialog = alertBuilder.create();
			} catch (RuntimeException e){
				e.printStackTrace();
			}
			break;

		default:
			Log.e(TAG, "Unknown dialog ID requested " + id);
			return null;
		}

		return alertDialog;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView,
	 * android.view.View, int, long)
	 */
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		// We use position - 1 since we have an extra header
		if (position == 0) {
			return;
		}
		EDFileChooserItem selected = mAdapter.getItem(position - 1);

		if (selected.isDirectory()) {
			if (selected.getName().equals("..")) {
				// Chdir up
				mCurEncFSDir = mDirStack.pop();
			} else {
				mDirStack.push(mCurEncFSDir);
				mCurEncFSDir = selected.getFile();
			}

			launchFillTask(false);
		} else {
			// Launch file in external application

//			if (mExternalStorageWriteable == false) {
//				mErrDialogText = getString(R.string.error_sd_readonly);
//				showDialog(DIALOG_ERROR);
//				return;
//			}

			// Create sdcard dir if it doesn't exist
			File encDroidDir = new File(
					Environment.getExternalStorageDirectory(),
					ENCDROID_SD_DIR_NAME);
			if (!encDroidDir.exists()) {
				encDroidDir.mkdir();
			}

			mOpenFile = selected.getFile();
			mOpenFileName = mOpenFile.getName();
			File dstFile = new File(encDroidDir, mOpenFileName);

			// Launch async task to decrypt the file
			launchAsyncTask(ASYNC_TASK_DECRYPT, dstFile, mOpenFile);
		}
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.volume_browser_menu_rename:
			mSelectedFile = mAdapter.getItem((int) info.id);
			showDialog(DIALOG_FILE_RENAME);
			return true;
		case R.id.volume_browser_menu_delete:
			mSelectedFile = mAdapter.getItem((int) info.id);
			showDialog(DIALOG_FILE_DELETE);
			return true;
		case R.id.volume_browser_menu_cut:
			mSelectedFile = mAdapter.getItem((int) info.id);
			mPasteFile = mSelectedFile.getFile();
			mPasteMode = PASTE_OP_CUT;

			if (mApp.isActionBarAvailable()) {
				mActionBar.invalidateOptionsMenu(this);
			}

			// Show toast
			Toast.makeText(
					getApplicationContext(),
					String.format(getString(R.string.toast_cut_file),
							mPasteFile.getName()), Toast.LENGTH_SHORT).show();

			return true;
		case R.id.volume_browser_menu_copy:
			mSelectedFile = mAdapter.getItem((int) info.id);
			mPasteFile = mSelectedFile.getFile();
			mPasteMode = PASTE_OP_COPY;

			if (mApp.isActionBarAvailable()) {
				mActionBar.invalidateOptionsMenu(this);
			}

			// Show toast
			Toast.makeText(
					getApplicationContext(),
					String.format(getString(R.string.toast_copy_file),
							mPasteFile.getName()), Toast.LENGTH_SHORT).show();

			return true;
		case R.id.volume_browser_menu_export:
			mSelectedFile = mAdapter.getItem((int) info.id);
			mOpenFile = mSelectedFile.getFile();
			mOpenFileName = mOpenFile.getName();
			Intent startFileChooser = new Intent(this, EDFileChooserActivity.class);
			startFileChooser.putExtra(EDFileChooserActivity.PARAM_KEY_MODE, EDFileChooserActivity.EXPORT_FILE_MODE);
			startFileChooser.putExtra(EDFileChooserActivity.PARAM_KEY_PROVIDER, EncdroidFileProvider.getLocalFileSystemProvider());
			startActivityForResult(startFileChooser, EXPORT_FILE_REQUEST);
			
			return true;
		case R.id.volume_browser_menu_export_external:
			mSelectedFile = mAdapter.getItem((int) info.id);
			mOpenFile = mSelectedFile.getFile();
			mOpenFileName = mOpenFile.getName();
			Intent startFileChooserExternal = new Intent(this, EDFileChooserActivity.class);
			startFileChooserExternal.putExtra(EDFileChooserActivity.PARAM_KEY_MODE, EDFileChooserActivity.EXPORT_FILE_MODE);
			startFileChooserExternal.putExtra(EDFileChooserActivity.PARAM_KEY_PROVIDER, EncdroidFileProvider.getExternalFileSystemProvider());
			startActivityForResult(startFileChooserExternal, EXPORT_FILE_REQUEST);
			return true;

		case R.id.volume_browser_menu_export_app:
			mSelectedFile = mAdapter.getItem((int) info.id);
			mOpenFile = mSelectedFile.getFile();
			mOpenFileName = mOpenFile.getName();

			
			mOpenFileName = mOpenFile.getName();
			mProgDialog = new NotificationHelper(EDVolumeBrowserActivity.this);
			mAsyncTask = new ViewFileTask(this, mProgDialog, mOpenFile);
			
			mAsyncTaskId = ASYNC_TASK_DECRYPT;
			mAsyncTask.setActivity(this);
			mAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			return true;
			
		case R.id.volume_browser_generage_thumbnail:
			mSelectedFile = mAdapter.getItem((int) info.id);
			
			thumbnailsTask = new ThumbnailsTask(this,mSelectedFile,false,false);
			thumbnailsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

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
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.volume_browser_context, menu);
//		if (!this.mEDVolume.getFileProvider().allowRemoteCopy()){
//			MenuItem MenuItem =(MenuItem )menu.findItem(R.id.volume_browser_menu_copy);
//			MenuItem.setVisible(false);
//		}
		
		  AdapterView.AdapterContextMenuInfo info =
		            (AdapterView.AdapterContextMenuInfo) menuInfo;

		  int pos = info.position;
		  
		  EDFileChooserItem selectedFile = mCurFileList.get(pos-1);
		  MenuItem MenuItem =(MenuItem )menu.findItem(R.id.volume_browser_generage_thumbnail);
		  if (selectedFile != null && (MimeManagement.isImage(selectedFile.getName()) || MimeManagement.isVideo(selectedFile.getName()))){
			  MenuItem.setVisible(true);
		  } else {
			  MenuItem.setVisible(false);
		  }
		
	}
	


	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case VIEW_FILE_REQUEST:
			// Don't need to watch any more
			//mFileObserver.stopWatching();

			File dstFile = new File(mFileObserver.getPath());

			// If the file was modified we need to sync it back
			if (mFileObserver.wasModified()) {
				// Sync file contents
				try {
					launchAsyncTask(ASYNC_TASK_SYNC, dstFile, mOpenFile);
				} catch (Exception e) {
					e.printStackTrace();
					mErrDialogText = e.getMessage();
					showDialog(DIALOG_ERROR);
				}
			} else {
				// File not modified, delete from SD
				//dstFile.delete();
				
				//overwrite all file bits with random data, and then delete it
				new SecureDeleteThread(dstFile).start();
			}

			// Clean up reference to the file observer
			mFileObserver = null;

			break;
		case EDIT_TEXT_FILE:
			if (data==null) break;
			String filename = data.getExtras().getString("fileModified");
			String content = data.getExtras().getString("newFileContent");
			EncFSFile file = PersistantInstanceManager.getEncfsFile(filename);
			SaveFileTask saveFileTask = new SaveFileTask(this,mProgDialog,file,new ByteArrayInputStream(content.getBytes()),content.length());
			saveFileTask.setActivity(this);
			saveFileTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			
			break;
		case PICK_FILE_REQUEST:
			if (resultCode == Activity.RESULT_OK) {
				String result = data.getExtras().getString(EDFileChooserActivity.PARAM_KEY_PATH);
				EncdroidFileProvider originProvider = (EncdroidFileProvider)data.getSerializableExtra(EDFileChooserActivity.PARAM_KEY_PROVIDER);
				String importPath = new File( originProvider.getAbsolutePath(result)).getAbsolutePath();
				Log.d(TAG, "Importing file: " + importPath);

				File importFile = new File(importPath);
				mImportFileName = importFile.getName();

				// Launch async task to complete importing
				launchAsyncTask(ASYNC_TASK_IMPORT, importFile, null);
			} else {
				Log.d(TAG, "File chooser returned unexpected return code: "
						+ resultCode);
			}
			break;
		case EXPORT_FILE_REQUEST:
			if (resultCode == Activity.RESULT_OK) {
				String result = data.getExtras().getString(EDFileChooserActivity.PARAM_KEY_PATH);
				EncdroidFileProvider originProvider = (EncdroidFileProvider)data.getSerializableExtra(EDFileChooserActivity.PARAM_KEY_PROVIDER);
				String exportPath = new File(originProvider.getAbsolutePath(result)).getAbsolutePath();
				Log.d(TAG, "Exporting file to: " + exportPath);

//				if (mExternalStorageWriteable == false) {
//					mErrDialogText = getString(R.string.error_sd_readonly);
//					showDialog(DIALOG_ERROR);
//					return;
//				}

				File exportFile = new File(exportPath, mOpenFileName);

				if (exportFile.exists()) {
					// Error dialog
					mErrDialogText = String.format(
							getString(R.string.error_file_exists),
							exportFile.getName());
					showDialog(DIALOG_ERROR);
				} else {
					// Launch async task to export the file
					launchAsyncTask(ASYNC_TASK_EXPORT, exportFile, mOpenFile);
				}
			} else {
				Log.e(TAG, "File chooser returned unexpected return code: "
						+ resultCode);
			}
			break;
		default:
			Log.e(TAG, "Unknown request: " + requestCode);
			break;
		}
	}

	// Bail out to the volume list
	public void exitToVolumeList() {
		Intent intent = new Intent(this, EDVolumeListActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		finish();
	}



	// Update the external storage state
	private void updateExternalStorageState() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
	}

	public void fill() {

		
		

		
		try {
			childEncFSFiles = mCurEncFSDir.listFiles();
			

			
			
		} catch (IOException e) {
			EDLogger.logException(TAG, e);
			mErrDialogText = "Unable to list files: " + e.getMessage();
			try {
				showDialog(DIALOG_ERROR);
				e.printStackTrace();
			} catch (RuntimeException e2){
				e2.printStackTrace();
			}
			return;
		}

		final boolean emptyDir = childEncFSFiles.length == 0 ? true : false;

		// Set title from UI thread
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				setTitle(mEDVolume.getName());

				if ((emptyDir == true)
						&& (mCurEncFSDir == mVolume.getRootDir())) {
					// Empty volume message
					mListHeader.setText(getString(R.string.no_files));
				} else {
					mListHeader.setText(mCurEncFSDir.getPath());
				}
			}
		});

		List<EDFileChooserItem> directories = new ArrayList<EDFileChooserItem>();
		List<EDFileChooserItem> files = new ArrayList<EDFileChooserItem>();

		
		if (defaultFolderBitmap==null) defaultFolderBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_folder);
		
		for (EncFSFile file : childEncFSFiles) {
			if (file.isDirectory()) {
				directories.add(new EDFileChooserItem(file.getName(), true,file, 0,defaultFolderBitmap));
			} else {
				
				if (!EncFSVolume.isConfigFile(file.getName())) {
					Bitmap mimeBitmap = getIconForFile(this, file.getName());
					files.add(new EDFileChooserItem(file.getName(), false, file, file.getLength(),mimeBitmap));
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
		if ((mActionBar == null) && (mCurEncFSDir != mVolume.getRootDir())) {
			mCurFileList.add(0, new EDFileChooserItem("..", true, "", 0, BitmapFactory.decodeResource(getResources(), R.drawable.ic_folder)));
		}

		refreshListViewGUI();
	}
	
	public void refreshListViewGUI(){
		if (mAdapter == null) {
			mAdapter = new EDFileChooserAdapter(this,
					R.layout.file_chooser_item, mCurFileList);

			// Set list adapter from UI thread
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					setListAdapter(mAdapter);
				}
			});
		} else {
			// Notify data set change from UI thread
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					mAdapter.notifyDataSetChanged();
				}
			});
		}
	}


	
	// Show a progress spinner and launch the fill task
	public void launchFillTask(boolean genThumbnails) {
		if (!isCtxImportFromExternalApp){
			this.setTitle(mEDVolume.getName());
			EDVolumeListActivity.fileISImportedFromExternalApp=null;
			EDVolumeListActivity.fileNameImportedFromExternalApp=null;
			EDVolumeListActivity.fileSizeImportedFromExternalApp=-1;
			if (saveExternalFileMenuItem!=null) saveExternalFileMenuItem.setVisible(isCtxImportFromExternalApp);
		}
		mFillTask = new FillTask(this,genThumbnails);
		mFillTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void launchAsyncTask(int taskId, File fileArg, EncFSFile encFSArg) {

		// Show a progress bar
		createProgressBarForTask(taskId, null);

		// Launch async task
		switch (taskId) {
		case ASYNC_TASK_DECRYPT:
			mAsyncTask = new ViewFileTask(this, childEncFSFiles,mProgDialog, encFSArg, fileArg);
			break;
		case ASYNC_TASK_IMPORT:
			mAsyncTask = new ImportFileTask(this,getmVolume(), fileArg,mCurEncFSDir.getPath(), mProgDialog);
			break;
		case ASYNC_TASK_SYNC:
			mAsyncTask = new SaveFromFSFileTask(this,mProgDialog, fileArg, encFSArg);
			break;
		case ASYNC_TASK_EXPORT:
			mAsyncTask = new ExportFileTask(this,mProgDialog, encFSArg, fileArg);
			break;
		case ASYNC_TASK_SAVE_EXTERNAL_APP_FILE_HERE:
			mAsyncTask = new ImportFileTask(this,getmVolume(), null ,mCurEncFSDir.getPath(), mProgDialog);
			break;
		default:
			Log.e(TAG, "1- Unknown task ID: " + taskId);
			break;
		}
		mAsyncTaskId = taskId;
		mAsyncTask.setActivity(this);
		mAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	


	private void launchAsyncTask(int taskId, String strArg) {

		// Show a progress bar
		createProgressBarForTask(taskId, strArg);

		// Launch async task
		switch (taskId) {
		case ASYNC_TASK_RENAME:
			mAsyncTask = new MetadataOpTask(this,mProgDialog,
					MetadataOpTask.RENAME_FILE, strArg);
			break;
		case ASYNC_TASK_DELETE:
			mAsyncTask = new MetadataOpTask(this, mProgDialog,
					MetadataOpTask.DELETE_FILE, strArg);
			break;
		case ASYNC_TASK_CREATE_DIR:
			mAsyncTask = new MetadataOpTask(this, mProgDialog,
					MetadataOpTask.CREATE_DIR, strArg);
			break;
		case ASYNC_TASK_CREATE_FILE:
			mAsyncTask = new MetadataOpTask(this, mProgDialog,
					MetadataOpTask.CREATE_FILE, strArg);
			break;
		default:
			Log.e(TAG, "2- Unknown task ID: " + taskId);
			break;
		}
		mAsyncTaskId = taskId;
		mAsyncTask.setActivity(this);
		mAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	// Create and show a progress dialog for the requested task ID
	private void createProgressBarForTask(int taskId, String strArg) {
		mProgDialog = new NotificationHelper(EDVolumeBrowserActivity.this);
		switch (taskId) {
		case ASYNC_TASK_SYNC:
//			mProgDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgDialog.setTitle(String
					.format(getString(R.string.encrypt_dialog_title_str),
							mOpenFileName));
			break;
		case ASYNC_TASK_IMPORT:
			//mProgDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgDialog.setTitle(String.format(
					getString(R.string.import_dialog_title_str),
					mImportFileName));
			break;
		case ASYNC_TASK_DECRYPT:
			//mProgDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgDialog.setTitle(String
					.format(getString(R.string.decrypt_dialog_title_str),
							mOpenFileName));
			break;
		case ASYNC_TASK_EXPORT:
			//mProgDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgDialog
					.setTitle(String.format(
							getString(R.string.export_dialog_title_str),
							mOpenFileName));
			break;
		case ASYNC_TASK_RENAME:
			//mProgDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgDialog.setTitle(String.format(
					getString(R.string.rename_dialog_title_str),
					mSelectedFile.getName(), strArg));
			break;
		case ASYNC_TASK_DELETE:
			//mProgDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgDialog.setTitle(String.format(
					getString(R.string.delete_dialog_title_str),
					mSelectedFile.getName()));
			break;
		case ASYNC_TASK_CREATE_DIR:
			mProgDialog.setTitle(String.format(
					getString(R.string.mkdir_dialog_title_str), strArg));
			break;
		case ASYNC_TASK_PASTE:
			//mProgDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			if (mPasteMode == PASTE_OP_COPY) {
				mProgDialog.setTitle(getString(R.string.copy_dialog_title_str));
			} else {
				mProgDialog.setTitle(getString(R.string.cut_dialog_title_str));
			}
			break;
		case ASYNC_TASK_SAVE_EXTERNAL_APP_FILE_HERE:
			mProgDialog.setTitle(String.format(
					getString(R.string.import_dialog_title_str), EDVolumeListActivity.fileNameImportedFromExternalApp));
			break;
		default:
			Log.e(TAG, "3- Unknown task ID: " + taskId);
			break;
		}
		//mProgDialog.setCancelable(false);
		mProgDialog.show();
		mSavedProgBarStrArg = strArg;
	}


	@Override
	protected void onRestart() {
		super.onRestart();


	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (PinCodeActivity.needPinCode(this)){
			Intent pinCode = new Intent(this, PinCodeActivity.class);
			startActivity(pinCode);
		}
		
		EncFSFileProvider fileProvider = mVolume.getFileProvider();
		if (fileProvider instanceof IStatefullSession){
			((IStatefullSession)fileProvider).clearSession();
		}
		

	
	}

	public static Bitmap getIconForFile(Activity activity, String filename){
		String[] types = {"apk","avi","doc","docx","flv","gif","gz","htm","html","jpg","mp3","mpg","pdf","png","txt","xls","zip","mp4"};
		for (String type: types){

			if (filename.toLowerCase().endsWith(type)) {
				int resource = activity.getResources().getIdentifier("mime_"+type, "drawable", activity.getPackageName());
				Bitmap folderBitmap = BitmapFactory.decodeResource(activity.getResources(), resource);
				return folderBitmap;
			}
		}
		if (defaultFileBitmap==null) defaultFileBitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.ic_file);
		return defaultFileBitmap;
	}




	public EncFSVolume getmVolume() {
		return mVolume;
	}




	public void setmVolume(EncFSVolume mVolume) {
		this.mVolume = mVolume;
	}
	


}