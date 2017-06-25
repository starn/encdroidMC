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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.mrpdaemon.android.encdroidmc.fileProvider.EncdroidFileProvider;
import org.mrpdaemon.android.encdroidmc.fileProvider.FileProviderActivity;
import org.mrpdaemon.android.encdroidmc.forceCloseManagement.ExceptionHandler;
import org.mrpdaemon.sec.encfs.EncFSConfig;
import org.mrpdaemon.sec.encfs.EncFSFileProvider;
import org.mrpdaemon.sec.encfs.EncFSFilenameEncryptionAlgorithm;
import org.mrpdaemon.sec.encfs.EncFSInvalidPasswordException;
import org.mrpdaemon.sec.encfs.EncFSVolume;
import org.mrpdaemon.sec.encfs.EncFSVolumeBuilder;
import org.paulmach.textedit.pmTextEdit;

import fr.starn.FileSynchronizerService;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.text.Editable;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
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
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class EDVolumeListActivity extends ListActivity {


	
	// Dialog ID's
	private final static int DIALOG_VOL_PASS = 0;
	private final static int DIALOG_VOL_NAME = 1;
	private final static int DIALOG_VOL_RENAME = 2;
	private final static int DIALOG_VOL_CREATE = 3;
	private final static int DIALOG_VOL_CREATEPASS = 4;
	private final static int DIALOG_VOL_DELETE = 5;
	private final static int DIALOG_FS_TYPE = 6;
	private final static int DIALOG_ERROR = 7;

	// Volume operation types
	private final static int VOLUME_OP_IMPORT = 0;
	private final static int VOLUME_OP_CREATE = 1;

	// Async task types
	private final static int ASYNC_TASK_UNLOCK_CACHE = 0;
	private final static int ASYNC_TASK_UNLOCK_PBKDF2 = 1;
	private final static int ASYNC_TASK_CREATE = 2;
	private final static int ASYNC_TASK_DELETE = 3;

	// Saved instance state keys
	private final static String SAVED_VOL_IDX_KEY = "vol_idx";
	private final static String SAVED_VOL_PICK_RESULT_KEY = "vol_pick_result";
	private final static String SAVED_CREATE_VOL_NAME_KEY = "create_vol_name";
	private final static String SAVED_VOL_OP_KEY = "vol_op";
	//private final static String SAVED_VOL_TYPE_KEY = "vol_type";
	private final static String SAVED_FILE_PROVIDER_KEY = "file_provider";
	private final static String SAVED_ASYNC_TASK_ID_KEY = "async_task_id";
	private final static String SAVED_PROGRESS_BAR_STR_ARG_KEY = "prog_bar_str";

	// Logger tag
	private final static String TAG = "EDVolumeListActivity";

	// Suffix for newly created volume directories
	private final static String NEW_VOLUME_DIR_SUFFIX = ".encdroid";

	// List adapter
	private EDVolumeListAdapter mAdapter = null;

	// Application object
	private EDApplication mApp;

	// Currently selected EDVolumeList item
	private EDVolume mSelectedVolume;
	private int mSelectedVolIdx;

	// Async task object for running volume key derivation
	private EDAsyncTask<String, ?, ?> mAsyncTask = null;

	// Async task ID
	private int mAsyncTaskId = -1;

	// Progress dialog for async progress
	private ProgressDialog mProgDialog = null;

	// Result from the volume picker activity
	private String mVolPickerResult = null;

	// Text for the error dialog
	private String mErrDialogText = "";

	// Name of the volume being created
	private String mCreateVolumeName = "";

	// Current volume operation (import/create)
	private int mVolumeOp = -1;

	// FS type for current volume operation
	//private int mVolumeType = -1;
	private EncdroidFileProvider mSelectedFileProvider2;

	// Shared preferences
	private SharedPreferences mPrefs = null;

	// Saved instance state for progress bar string argument
	private String mSavedProgBarStrArg = null;
	
	public static InputStream fileISImportedFromExternalApp;
	public static String fileNameImportedFromExternalApp;
	public static long fileSizeImportedFromExternalApp;

	
	// Restore context
	private class ActivityRestoreContext {
		public EDVolume savedVolume;
		public EDAsyncTask<String, ?, ?> savedTask;
	}

	// Called when the activity is first created.
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    Intent intent = getIntent();
	    System.out.println("on create");
	    
	    //display changelog if this app version has been updated
	    try {
	    	PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
	    	String version = pInfo.versionName;
	    	EDDBHelper dbHelper = new EDDBHelper(this);
	    	String value = dbHelper.getKeyValueValue("lastChangeLog");
	    	if (!version.equals(value)){
	    		dbHelper.removeKeyValue("lastChangeLog");
	    		dbHelper.insertKeyValue("lastChangeLog",version);
				Intent showChangeLog = new Intent(this, pmTextEdit.class);
				StringBuffer changeLogTxt = new StringBuffer();
				changeLogTxt.append("ChangeLog:\n\n");
				changeLogTxt.append("2.0.15\n");
				changeLogTxt.append("- fix error when creating a new volume (sorry for this regression)\n");
				changeLogTxt.append("- add project to github: https://github.com/starn/encdroidMC\n");
				changeLogTxt.append("\n");
				changeLogTxt.append("2.0.14\n");
				changeLogTxt.append("- Add german translation (thanks to Thomas R. for this translation !)\n");
				changeLogTxt.append("\n");
				changeLogTxt.append("2.0.13\n");
				changeLogTxt.append("- Fix a very important bug: the application crashed at startup on new installation.\n");
				changeLogTxt.append("- new 'Create file' menu, to create a new empty file\n");
				changeLogTxt.append("- Generate thumbnail video (to generate video thumbnail). I separate picture and video thumnails because video thumbnails may use lot of CPU and data.\n\n");
				changeLogTxt.append("2.0.12\n");
				changeLogTxt.append("- Display this change log after updating this app\n");
				changeLogTxt.append("- Support PIN code security to open this application (volume list menu => parameter)\n");
				changeLogTxt.append("- Add folder synchronisation background service\n");
				changeLogTxt.append("- Increase video streaming performance when skipping to a position\n");
				changeLogTxt.append("- Add internal sliding image gallery for pictures (you can easily browse a picture folder)\n");
				changeLogTxt.append("- Add internal text editor (more secure than temporaly saving on storage)\n");
				changeLogTxt.append("- Add french translation (except for this text sorry)\n");
				changeLogTxt.append("- Add thumbnail for pictures and video (menu => generate thumbnail)\n");
				changeLogTxt.append("- Download/upload are now done in background. you can follow up on status bar.\n");
				changeLogTxt.append("- Increase windows share (samba) speed");
				changeLogTxt.append("- Fix file corrupted on particular conditions");
				changeLogTxt.append("- Fix external sdcard issue");
				changeLogTxt.append("\n");
				changeLogTxt.append("\n\n 2.0.11\n");
				changeLogTxt.append("- Play video with random access: you can skip to any position\n");
				changeLogTxt.append("\n");
				showChangeLog.putExtra("fileContent",changeLogTxt.toString().getBytes());
				showChangeLog.putExtra("selectedFile","change_log.txt");
				showChangeLog.putExtra("readonly", "true");
				startActivity(showChangeLog);
	    	}
	    } catch (Exception e){
	    	//do nothing
	    	e.printStackTrace();
	    }
		
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
		
		setContentView(R.layout.volume_list);
		registerForContextMenu(this.getListView());

		setTitle(getString(R.string.volume_list));

		mApp = (EDApplication) getApplication();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (savedInstanceState != null) {
			// Activity being recreated

			mVolPickerResult = savedInstanceState
					.getString(SAVED_VOL_PICK_RESULT_KEY);
			mCreateVolumeName = savedInstanceState
					.getString(SAVED_CREATE_VOL_NAME_KEY);
			mVolumeOp = savedInstanceState.getInt(SAVED_VOL_OP_KEY);
			mSelectedFileProvider2 = (EncdroidFileProvider)savedInstanceState.getSerializable(SAVED_FILE_PROVIDER_KEY);

			ActivityRestoreContext restoreContext = (ActivityRestoreContext) getLastNonConfigurationInstance();
			if (restoreContext != null) {
				mSelectedVolume = restoreContext.savedVolume;
				mSelectedVolIdx = savedInstanceState.getInt(SAVED_VOL_IDX_KEY);

				// Restore async task
				mAsyncTask = restoreContext.savedTask;
				mAsyncTaskId = savedInstanceState
						.getInt(SAVED_ASYNC_TASK_ID_KEY);

				if (mAsyncTask != null) {
					// Create new progress dialog and replace the old one
					createProgressBarForTask(mAsyncTaskId,
							savedInstanceState
									.getString(SAVED_PROGRESS_BAR_STR_ARG_KEY));
					mAsyncTask.setProgressDialog(mProgDialog);

					// Fix the activity for the task
					mAsyncTask.setActivity(this);
				}
			}
		}
		
		refreshList();
		
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
		    getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
		}
	}

	public String getFileName(Uri uri) {
		  String result = null;
		  if (uri.getScheme().equals("content")) {
		    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
		    try {
		      if (cursor != null && cursor.moveToFirst()) {
		        result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
		      }
		    } finally {
		      cursor.close();
		    }
		  }
		  if (result == null) {
		    result = uri.getPath();
		    int cut = result.lastIndexOf('/');
		    if (cut != -1) {
		      result = result.substring(cut + 1);
		    }
		  }
		  return result;
		}
	
	public long getFileSize(Uri uri) {
		  long result = 0;
		  if (uri.getScheme().equals("content")) {
		    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
		    try {
		      if (cursor != null && cursor.moveToFirst()) {
		        result = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
		      }
		    } finally {
		      cursor.close();
		    }
		  }
		  return result;
		}
	
	public Object onRetainNonConfigurationInstance() {
		ActivityRestoreContext restoreContext = new ActivityRestoreContext();
		restoreContext.savedVolume = mSelectedVolume;
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

	
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(SAVED_VOL_IDX_KEY, mSelectedVolIdx);
		outState.putString(SAVED_VOL_PICK_RESULT_KEY, mVolPickerResult);
		outState.putString(SAVED_CREATE_VOL_NAME_KEY, mCreateVolumeName);
		outState.putInt(SAVED_VOL_OP_KEY, mVolumeOp);
		outState.putSerializable(SAVED_FILE_PROVIDER_KEY, mSelectedFileProvider2);
		outState.putInt(SAVED_ASYNC_TASK_ID_KEY, mAsyncTaskId);
		outState.putString(SAVED_PROGRESS_BAR_STR_ARG_KEY, mSavedProgBarStrArg);
		super.onSaveInstanceState(outState);
	}

	// Create the options menu
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.volume_list_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	// Handler for options menu selections
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.volume_list_menu_import:
			mVolumeOp = VOLUME_OP_IMPORT;
			showDialog(DIALOG_FS_TYPE);
			return true;
		case R.id.volume_list_menu_create:
			mVolumeOp = VOLUME_OP_CREATE;
			showDialog(DIALOG_FS_TYPE);
			return true;
//		case R.id.volume_list_menu_accounts:
//			Intent showAccounts = new Intent(this, EDAccountsActivity.class);
//			startActivity(showAccounts);
//			return true;
		case R.id.volume_list_menu_settings:
			Intent showPrefs = new Intent(this, EDPreferenceActivity.class);
			startActivity(showPrefs);
			return true;
		case R.id.volume_list_exit:
			System.exit(0);
			return true;
		default:
			return false;
		}
	}

	
	protected void onResume() {
		super.onResume();
		
		if (PinCodeActivity.needPinCode(this)){
			Intent pinCode = new Intent(this, PinCodeActivity.class);
			startActivity(pinCode);
		}
		
		System.out.println("******* on resume");
		Intent intent = getIntent();
		if ("fileSynchronizer".equals( intent.getStringExtra("ctx"))){
			//int fileProvider = intent.getIntExtra("fileProviderID");
			//launchCreateNewProvider(EDFileChooserActivity.VOLUME_PICKER_MODE, 0);
			String volumeName = intent.getStringExtra("volumeName");
			launchLoadExistingFileProviderForUnlock(volumeName);
			intent.putExtra("ctx", "fileSynchronizerOpenVolume");
			//finish();
		} 
		
		
		
		//check if a file from another application wait for being saved in encdroidMC
	    String action = intent.getAction();
	    String type = intent.getType();
	    if (Intent.ACTION_SEND.equals(action) && type != null) {
	    	System.out.println("action:"+action);
	    	System.out.println("type="+type);
	        Uri _uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
	        String filePath = "";
	        if (_uri!=null){
	        	try {
	        		fileNameImportedFromExternalApp = getFileName(_uri);
	        		fileSizeImportedFromExternalApp= getFileSize(_uri);
	        		fileISImportedFromExternalApp = getContentResolver().openInputStream(_uri);
	        		System.out.println("receive inputStream for file:"+fileNameImportedFromExternalApp+ "and size "+ fileSizeImportedFromExternalApp);
	        	} catch (Exception e2) {
	        	    e2.printStackTrace();
	        	}
	        }
	        if ("text/plain".equals(type)){
	        	try {
		        	String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
		        	System.out.println("receive text: "+sharedText);
		        	if (sharedText.startsWith("http")){
		        		new CallUrlThread(sharedText);
		        		do {
		        			Thread.sleep(200);
		        		}while (!CallUrlThread.finished);
		        		if (CallUrlThread.error!=null)  {
		        			Toast.makeText(this.getApplicationContext(),CallUrlThread.error, Toast.LENGTH_LONG).show();
		        		} else {
		        			fileSizeImportedFromExternalApp= CallUrlThread.size;
		        			fileISImportedFromExternalApp = CallUrlThread.is;
		        			fileNameImportedFromExternalApp=CallUrlThread.name;
		        		}
		        	}
	        	} catch (Exception e){
	        		e.printStackTrace();
	        	}

	        	}
	    } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
	    	Toast.makeText(this.getApplicationContext(),"Multiple file import not yet implemented !!", Toast.LENGTH_LONG).show();
	    	 //ArrayList<Uri> filesUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
	    	 
        }
		if (fileNameImportedFromExternalApp!=null) this.setTitle("Location for: "+fileNameImportedFromExternalApp);
		//END OF EXTERNAL APP FILE IMPORT
		
		
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		EDVolume selected = mAdapter.getItem((int) info.id);

		switch (item.getItemId()) {
		case R.id.volume_list_menu_lock:
			if (selected.isLocked()) {
				mSelectedVolume = selected;
				mSelectedVolIdx = info.position;
				unlockSelectedVolume();
			} else {
				selected.lock();
			}
			mAdapter.notifyDataSetChanged();
			return true;
		case R.id.volume_list_menu_rename:
			this.mSelectedVolume = selected;
			showDialog(DIALOG_VOL_RENAME);
			return true;
		case R.id.volume_list_menu_delete:
			this.mSelectedVolume = selected;
			showDialog(DIALOG_VOL_DELETE);
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
		inflater.inflate(R.menu.volume_list_context, menu);

		// Change the text of the lock/unlock item based on volume status
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		EDVolume selected = mAdapter.getItem((int) info.id);

		MenuItem lockItem = menu.findItem(R.id.volume_list_menu_lock);

		if (selected.isLocked()) {
			lockItem.setTitle(getString(R.string.menu_unlock_volume));
		} else {
			lockItem.setTitle(getString(R.string.menu_lock_volume));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
	 */
	
	protected void onPrepareDialog(int id, final Dialog dialog) {
		final EditText input;
		boolean rename = false;

		switch (id) {
		case DIALOG_VOL_RENAME:
			rename = true;
		case DIALOG_VOL_PASS:
		case DIALOG_VOL_CREATEPASS:
		case DIALOG_VOL_NAME:
		case DIALOG_VOL_CREATE:
			input = (EditText) dialog.findViewById(R.id.dialog_edit_text);
			if (input != null) {
				if (rename && mSelectedVolume != null) {
					input.setText(mSelectedVolume.getName());
				} else {
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
				
				public boolean onEditorAction(TextView v, int actionId,
						KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						Button button = ((AlertDialog) dialog)
								.getButton(Dialog.BUTTON_POSITIVE);
						button.performClick();
						return true;
					}
					return false;
				}
			});

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
	
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		AlertDialog alertDialog = null;

		LayoutInflater inflater = LayoutInflater.from(this);

		final EditText input;
		final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		final int myId = id;

		switch (id) {
		case DIALOG_VOL_PASS: // Password dialog
			if (mSelectedVolume == null) {
				// Can happen when restoring a killed activity
				return null;
			}
			// Fall through
		case DIALOG_VOL_CREATEPASS: // Create volume password
			

			
			input = (EditText) inflater.inflate(R.layout.dialog_edit, null);

			// Hide password input
			input.setTransformationMethod(PasswordTransformationMethod.getInstance());
			input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); 

			alertBuilder.setTitle(getString(R.string.pwd_dialog_title_str));
			alertBuilder.setView(input);
			alertBuilder.setPositiveButton(getString(R.string.btn_ok_str),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {

							// Hide soft keyboard
							imm.hideSoftInputFromWindow(input.getWindowToken(),
									0);

							Editable value = input.getText();

							switch (myId) {
							case DIALOG_VOL_PASS:
								// Show progress dialog
								createProgressBarForTask(
										ASYNC_TASK_UNLOCK_PBKDF2, null);

								// Launch async task to import volume after user type its password in dialog
								mAsyncTask = new UnlockVolumeTask(mProgDialog, 	mSelectedFileProvider2, null);
								mAsyncTaskId = ASYNC_TASK_UNLOCK_PBKDF2;
								mAsyncTask.setActivity(EDVolumeListActivity.this);
								mAsyncTask.execute(mSelectedVolume.getPath(), value.toString());
								break;
							case DIALOG_VOL_CREATEPASS:
								// Show progress dialog
								createProgressBarForTask(ASYNC_TASK_CREATE,	mCreateVolumeName);

								// Launch async task to create volume
								mAsyncTask = new CreateVolumeTask(mProgDialog,	mSelectedFileProvider2);
								mAsyncTaskId = ASYNC_TASK_CREATE;
								mAsyncTask.setActivity(EDVolumeListActivity.this);
								mAsyncTask.execute(mVolPickerResult,
										mCreateVolumeName, value.toString());
								break;
							}
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

			// Show keyboard
			alertDialog.setOnShowListener(new OnShowListener() {

				
				public void onShow(DialogInterface dialog) {
					imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
				}
			});
			break;

		case DIALOG_VOL_RENAME:
			if (mSelectedVolume == null) {
				// Can happen when restoring a killed activity
				return null;
			}
			// Fall through
		case DIALOG_VOL_CREATE:
		case DIALOG_VOL_NAME: // Volume name dialog

			input = (EditText) inflater.inflate(R.layout.dialog_edit, null);

			alertBuilder.setTitle(getString(R.string.voladd_dialog_title_str));
			alertBuilder.setView(input);
			alertBuilder.setPositiveButton(getString(R.string.btn_ok_str),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							Editable value = input.getText();
							switch (myId) {
							case DIALOG_VOL_NAME:
								//starn: recupere le provider ici pour sauvegarder ses infos (url login et pwd)
								importVolume(value.toString(),mVolPickerResult, mSelectedFileProvider2);
								break;
							case DIALOG_VOL_RENAME:
								renameVolume(mSelectedVolume, value.toString());
								break;
							case DIALOG_VOL_CREATE:
								mCreateVolumeName = value.toString();
								showDialog(DIALOG_VOL_CREATEPASS);
								break;
							default:
								break;
							}
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

			// Show keyboard
			alertDialog.setOnShowListener(new OnShowListener() {

				
				public void onShow(DialogInterface dialog) {
					imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
				}
			});
			break;
		case DIALOG_FS_TYPE:
			boolean extSd = mPrefs.getBoolean("ext_sd_enabled", false);
			CharSequence[] fsTypes;
//			if (extSd == true) {
//				fsTypes = new CharSequence[4];
//				fsTypes[0] = getString(R.string.fs_dialog_local);
//				fsTypes[1] = "Dropbox";
//				fsTypes[2] = "Webdav";
//				fsTypes[3] = getString(R.string.fs_dialog_ext_sd);
//			} else {
//				fsTypes = new CharSequence[3];
//				fsTypes[0] = getString(R.string.fs_dialog_local);
//				fsTypes[1] = "Dropbox";
//				fsTypes[2] = "Webdav";
//			}
			List<EncdroidFileProvider> providers = EncdroidFileProvider.getAvailableProviders();
			fsTypes = new CharSequence[providers.size()];
			int i =0;
			for (EncdroidFileProvider p: providers){
				fsTypes[i] = p.getProviderName();
				i++;
			}
			
			alertBuilder.setTitle(getString(R.string.fs_type_dialog_title_str));
			alertBuilder.setItems(fsTypes,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
						if (mVolumeOp == VOLUME_OP_IMPORT) {
//							launchFileChooser(
//									EDFileChooserActivity.VOLUME_PICKER_MODE,
//									item);
							launchCreateNewProvider(EDFileChooserActivity.VOLUME_PICKER_MODE,item);
						} else {
//							launchFileChooser(
//									EDFileChooserActivity.CREATE_VOLUME_MODE,
//									item);
							launchCreateNewProvider(EDFileChooserActivity.CREATE_VOLUME_MODE,item);
						}							

						}
					});
							
			alertDialog = alertBuilder.create();
			break;
		case DIALOG_VOL_DELETE:
			if (mSelectedVolume == null) {
				// Can happen when restoring a killed activity
				return null;
			}

			//final CharSequence[] items = { getString(R.string.delete_vol_dialog_disk_str) };
			final boolean[] states = { false };

			alertBuilder.setTitle(String.format(
					getString(R.string.delete_vol_dialog_confirm_str),
					mSelectedVolume.getName()));
//			alertBuilder.setMultiChoiceItems(items, states,
//					new DialogInterface.OnMultiChoiceClickListener() {
//						public void onClick(DialogInterface dialogInterface,
//								int item, boolean state) {
//						}
//					});
			alertBuilder.setPositiveButton(getString(R.string.btn_ok_str),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
//							SparseBooleanArray checked = ((AlertDialog) dialog)
//									.getListView().getCheckedItemPositions();
							//if ( checked.get(0)) {
							if (0==1 ) {
								// Delete volume from disk
								createProgressBarForTask(ASYNC_TASK_DELETE,
										mSelectedVolume.getName());

								// Dropbox auth if needed
//								if (mSelectedVolume.getType() == EDVolume.DROPBOX_VOLUME) {
//									EDDropbox dropbox = mApp.getDropbox();
//
//									if (!dropbox.isAuthenticated()) {
//										dropbox.startLinkorAuth(EDVolumeListActivity.this);
//										if (!dropbox.isAuthenticated()) {
//											return;
//										}
//									}
//								}

								// Launch async task to delete volume
								mAsyncTask = new DeleteVolumeTask(mProgDialog, 	mSelectedVolume,mSelectedFileProvider2);
								mAsyncTaskId = ASYNC_TASK_DELETE;
								mAsyncTask
										.setActivity(EDVolumeListActivity.this);
								mAsyncTask.execute();
							} else {
								// Just remove from the volume list
								deleteVolume(mSelectedVolume);
							}
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
		case DIALOG_ERROR:
		
			
			alertBuilder.setMessage(mErrDialogText);
			alertBuilder.setCancelable(false);
			alertBuilder.setNeutralButton(getString(R.string.btn_ok_str),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							dialog.dismiss();
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView,
	 * android.view.View, int, long)
	 */
	
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		mSelectedVolume = mAdapter.getItem(position);
		mSelectedVolIdx = position;

		// Unlock via password
		if (mSelectedVolume.isLocked()) {
			unlockSelectedVolume();
		} else {
			launchVolumeBrowser(position);
		}
	}

	// Handler for results from called activities
	
	protected void onActivityResult(int requestCode, int resultCode,
			final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK) {

			//we come from FileProviderActivity for volume creation
			if (requestCode==FileProviderActivity.CONTEXT_ACION_CREATE_NEW_PROVIDER_INSTANCE_FOR_VOLUME_CREATION){
				EncdroidFileProvider fileProvider = (EncdroidFileProvider)data.getSerializableExtra("providerInstance");
				launchFileChooser(EDFileChooserActivity.CREATE_VOLUME_MODE, fileProvider);
			}
			
			//we come from FileProviderActivity for volume import			
			if (requestCode==FileProviderActivity.CONTEXT_ACION_CREATE_NEW_PROVIDER_INSTANCE_FOR_VOLUME_IMPORT){
				EncdroidFileProvider fileProvider = (EncdroidFileProvider)data.getSerializableExtra("providerInstance");
				launchFileChooser(EDFileChooserActivity.VOLUME_PICKER_MODE, fileProvider);
			}
			
			if (requestCode==EDFileChooserActivity.REQUEST_CODE){
				int mode = data.getIntExtra(EDFileChooserActivity.PARAM_KEY_MODE, -1);
				mSelectedFileProvider2 = (EncdroidFileProvider)data.getSerializableExtra(EDFileChooserActivity.PARAM_KEY_PROVIDER);
				mVolPickerResult = data.getStringExtra(EDFileChooserActivity.PARAM_KEY_PATH);
				if (EDFileChooserActivity.VOLUME_PICKER_MODE == mode){
					showDialog(DIALOG_VOL_NAME);
				}
				if (EDFileChooserActivity.CREATE_VOLUME_MODE == mode){
					showDialog(DIALOG_VOL_CREATE);
				}				
			}
			if (requestCode==FileProviderActivity.CONTEXT_ACION_LOAD_EXISTING_PROVIDER_INSTANCE_FOR_UNLOCK_VOLUME){
				mSelectedFileProvider2 = (EncdroidFileProvider)data.getSerializableExtra("providerInstance");
				
				// If key caching is enabled, see if a key is cached
				byte[] cachedKey = null;
				if (mPrefs.getBoolean("cache_key", false)) {
					cachedKey = mApp.getDbHelper().getCachedKey(mSelectedVolume);
				}

				if (cachedKey == null) {
					if (getIntent()!=null && getIntent().getStringExtra("password") != null){
						String passwordFromFileSyncRule = getIntent().getStringExtra("password");
						// Launch async task to import volume after user type its password in dialog
						mAsyncTask = new UnlockVolumeTask(mProgDialog, 	mSelectedFileProvider2, null);
						mAsyncTaskId = ASYNC_TASK_UNLOCK_PBKDF2;
						mAsyncTask.setActivity(EDVolumeListActivity.this);
						mAsyncTask.execute(mSelectedVolume.getPath(), passwordFromFileSyncRule);
					} else {
						showDialog(DIALOG_VOL_PASS);
					}
				} else {
					createProgressBarForTask(ASYNC_TASK_UNLOCK_CACHE, null);

					mAsyncTask = new UnlockVolumeTask(null, mSelectedFileProvider2, cachedKey);
					mAsyncTaskId = ASYNC_TASK_UNLOCK_CACHE;
					mAsyncTask.setActivity(EDVolumeListActivity.this);
					mAsyncTask.execute(mSelectedVolume.getPath(), null);
				}
			}

		} else {
			if (data!=null && data.getExtras() != null){
				String error = data.getExtras().getString(
						EDFileChooserActivity.ERROR_KEY);
				if (error==null) {
					Toast.makeText(this.getApplicationContext(),"No error...", Toast.LENGTH_LONG).show();
					return;
				}
				
				
				AlertDialog ad = new AlertDialog.Builder(this).create();  
				ad.setCancelable(false); // This blocks the 'BACK' button  
				ad.setMessage(error);  
				ad.setButton("OK", new DialogInterface.OnClickListener() {  
				    @Override  
				    public void onClick(DialogInterface dialog, int which) {  
				        dialog.dismiss();                      
				    }  
				});  
				ad.show(); 				
				
				Log.e(TAG, "File chooser returned unexpected return code: "
						+ resultCode);
			}
		}

	}

	// Launch the file chooser activity in the requested mode
	private void launchFileChooser(int mode, EncdroidFileProvider fileProvider) {
		Intent startFileChooser = new Intent(this, EDFileChooserActivity.class);

		//Bundle fileChooserParams = new Bundle();
		startFileChooser.putExtra(EDFileChooserActivity.PARAM_KEY_MODE, mode);
		startFileChooser.putExtra(EDFileChooserActivity.PARAM_KEY_PROVIDER, fileProvider);
		//starn: on peut arriver ici lorsqu'on cr�� un nouveau voume apres avoir selectionn� le providerType
		//startFileChooser.putExtras(fileChooserParams);
		startActivityForResult(startFileChooser, EDFileChooserActivity.REQUEST_CODE);
	}

	//when selected "create volume" or "import volume" , and before launching "launchFileChooser(...)"
	private void launchCreateNewProvider(int mode, int indexInListOfProviders) {
		Intent i = new Intent(this, FileProviderActivity.class);
		i.putExtra("idProvider", EncdroidFileProvider.getProviderByIndex(indexInListOfProviders).getID());
		if (mode==EDFileChooserActivity.VOLUME_PICKER_MODE){
			this.startActivityForResult(i,FileProviderActivity.CONTEXT_ACION_CREATE_NEW_PROVIDER_INSTANCE_FOR_VOLUME_IMPORT);
		}
		if (mode==EDFileChooserActivity.CREATE_VOLUME_MODE){
			this.startActivityForResult(i,FileProviderActivity.CONTEXT_ACION_CREATE_NEW_PROVIDER_INSTANCE_FOR_VOLUME_CREATION);
		}
	}
	
	/**
	 * load existing provider, set its volume params, (and set instance variable "mSelectedProvider" in onActivityResultCode
	 * @param volume
	 */
	private void launchLoadExistingFileProviderForUnlock(EDVolume volume){
		Intent i = new Intent(this, FileProviderActivity.class);
		i.putExtra("idProvider", volume.getFileProviderId());
		i.putExtra("providerParams", volume.getSerializedFileProviderParams());
		this.startActivityForResult(i,FileProviderActivity.CONTEXT_ACION_LOAD_EXISTING_PROVIDER_INSTANCE_FOR_UNLOCK_VOLUME);
	}
	
	private void launchLoadExistingFileProviderForUnlock(String volumeName){
		List<EDVolume>  volumes = EDApplication.volumeList;
		for (EDVolume v: volumes){
			if (v.getName().equals(volumeName)){
				mSelectedVolume=v;
				launchLoadExistingFileProviderForUnlock(v);
				break;
			}
		}
	}
	
	@Override
    public void startActivityForResult(Intent intent, int requestCode) {
        intent.putExtra("requestCode", requestCode);
        super.startActivityForResult(intent, requestCode);
    }	
	


	// Launch the volume browser activity for the given volume
	private void launchVolumeBrowser(int volIndex) {
		Intent startVolumeBrowser = new Intent(this,EDVolumeBrowserActivity.class);

		Bundle volumeBrowserParams = new Bundle();
		volumeBrowserParams.putInt(EDVolumeBrowserActivity.VOL_ID_KEY, volIndex);
		startVolumeBrowser.putExtras(volumeBrowserParams);
		
		startActivity(startVolumeBrowser);
	}

	private void refreshList() {
		if (mAdapter == null) {
			mAdapter = new EDVolumeListAdapter(this, R.layout.volume_list_item,	mApp.getVolumeList());
			this.setListAdapter(mAdapter);
		} else {
			mAdapter.notifyDataSetChanged();
		}
	}

	private void importVolume(String volumeName, String volumePath,
			EncdroidFileProvider fileProvider) {
		EDVolume volume = new EDVolume(volumeName, volumePath, fileProvider.getID(),fileProvider.serializeParams());
		mApp.getVolumeList().add(volume);
		mApp.getDbHelper().insertVolume(volume);
		refreshList();
	}

	private void deleteVolume(EDVolume volume) {
		mApp.getVolumeList().remove(volume);
		mApp.getDbHelper().deleteVolume(volume);
		refreshList();
	}

	private void renameVolume(EDVolume volume, String newName) {
		mApp.getDbHelper().renameVolume(volume, newName);
		volume.setName(newName);
		refreshList();
	}
	


	/**
	 * Unlock the currently selected volume
	 */
	private void unlockSelectedVolume() {
		launchLoadExistingFileProviderForUnlock(mSelectedVolume);
	}

	private void createProgressBarForTask(int taskId, String strArg) {
		mProgDialog = new ProgressDialog(EDVolumeListActivity.this);
		switch (taskId) {
		case ASYNC_TASK_CREATE:
			mProgDialog.setTitle(String.format(
					getString(R.string.mkvol_dialog_title_str), strArg));
			break;
		case ASYNC_TASK_DELETE:
			mProgDialog.setTitle(String.format(
					getString(R.string.delvol_dialog_title_str), strArg));
			break;
		case ASYNC_TASK_UNLOCK_CACHE:
			mProgDialog.setTitle(getString(R.string.unlocking_volume));
			break;
		case ASYNC_TASK_UNLOCK_PBKDF2:
			mProgDialog.setTitle(getString(R.string.pbkdf_dialog_title_str));
			mProgDialog.setMessage(getString(R.string.pbkdf_dialog_msg_str));
			break;
		default:
			Log.e(TAG, "Unknown task ID: " + taskId);
			break;
		}

		mProgDialog.setCancelable(false);
		mProgDialog.show();
		mSavedProgBarStrArg = strArg;
	}


	private class UnlockVolumeTask extends
			EDAsyncTask<String, Void, EncFSVolume> {

		// Volume type
		private EncdroidFileProvider volumeProvider;

		// Cached key
		private byte[] cachedKey;

		// Invalid cached key
		boolean invalidCachedKey = false;

		public UnlockVolumeTask(ProgressDialog dialog, EncdroidFileProvider _volumeProvider, byte[] cachedKey) {
			super();
			setProgressDialog(dialog);
			this.volumeProvider = _volumeProvider;
			this.cachedKey = cachedKey;
		}

		@SuppressLint("Wakelock")
		
		protected EncFSVolume doInBackground(String... args) {

			WakeLock wl = null;
			EncFSVolume volume = null;

			if (cachedKey == null) {
				PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
				wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);

				// Acquire wake lock to prevent screen from dimming/timing out
				wl.acquire();
			}

			

			// Get file provider for this volume
			EncdroidFileProvider fileProvider = volumeProvider;
			fileProvider.changeRootPath(args[0]);
			
			// Unlock the volume, takes long due to PBKDF2 calculation
			try {
				if (cachedKey == null) {
					volume = new EncFSVolumeBuilder()
							.withFileProvider(fileProvider,mSelectedVolume.getName())
							.withPbkdf2Provider(mApp.getNativePBKDF2Provider())
							.withPassword(args[1]).buildVolume();
				} else {
					volume = new EncFSVolumeBuilder()
							.withFileProvider(fileProvider,mSelectedVolume.getName())
							.withDerivedKeyData(cachedKey).buildVolume();
				}
			} catch (EncFSInvalidPasswordException e) {
				if (cachedKey != null) {
					invalidCachedKey = true;
				} else {
					((EDVolumeListActivity) getActivity()).mErrDialogText = getString(R.string.incorrect_pwd_str);
				}
			} catch (Exception e) {
				EDLogger.logException(TAG, e);
				((EDVolumeListActivity) getActivity()).mErrDialogText = "Error during volume unlocking: connection is ko ? - "+e
						.getMessage();
				if (mProgDialog!=null) mProgDialog.dismiss();
			} catch (Throwable t) {
				((EDVolumeListActivity) getActivity()).mErrDialogText = "Error during volume unlocking: connection is ko ? - "+t
						.getMessage();
				if (mProgDialog!=null) mProgDialog.dismiss();
			}

			if (cachedKey == null) {
				// Release the wake lock
				wl.release();
			}

			return volume;
		}

		// Run after the task is complete
		
		protected void onPostExecute(EncFSVolume result) {
			super.onPostExecute(result);

			final EDVolumeListActivity mActivity = (EDVolumeListActivity) getActivity();

			if (mProgDialog != null) {
				if (mProgDialog.isShowing()) {
					mProgDialog.dismiss();
				}
			}

			if (!isCancelled()) {
				if (invalidCachedKey) {
					// Show toast for invalid password
					Toast.makeText(getApplicationContext(),
							getString(R.string.save_pass_invalid_str),
							Toast.LENGTH_SHORT).show();

					// Invalidate cached key from DB
					mActivity.mApp.getDbHelper().clearKey(mSelectedVolume);

					// Kick off password dialog
					mActivity.showDialog(DIALOG_VOL_PASS);

					return;
				}

				if (result != null) {
					mActivity.mSelectedVolume.unlock(result);

					// Notify list adapter change from UI thread
					runOnUiThread(new Runnable() {
						
						public void run() {
							mActivity.mAdapter.notifyDataSetChanged();
						}
					});

					if (cachedKey == null) {
						// Cache key in DB if preference is enabled
						if (mActivity.mPrefs.getBoolean("cache_key", false)) {
							byte[] keyToCache = result.getDerivedKeyData();
							mActivity.mApp.getDbHelper().cacheKey(
									mSelectedVolume, keyToCache);
						}
					}
					if ("fileSynchronizerOpenVolume".equals( getIntent().getStringExtra("ctx"))){
						//we come here from file synchroniser. close activity.
						getIntent().putExtra("ctx", "");
						FileSynchronizerService.getInstance().sync(getIntent().getExtras().getInt("ruleID"));
						finish();
					} else {
						mActivity.launchVolumeBrowser(mSelectedVolIdx);
					}
				} else {
					mActivity.showDialog(DIALOG_ERROR);
				}
			}
		}
	}

	private class CreateVolumeTask extends EDAsyncTask<String, Void, Boolean> {

		// Name of the volume being created
		private String volumeName;

		// Path of the volume
		private String volumePath;

		// Volume type
		//private int volumeType;
		private EncdroidFileProvider fileProvider;

		// Password
		private String password;

		public CreateVolumeTask(ProgressDialog dialog, EncdroidFileProvider _fileProvider) {
			super();
			setProgressDialog(dialog);
			this.fileProvider = _fileProvider;
		}

		
		protected Boolean doInBackground(String... args) {

			volumeName = args[1];
			password = args[2];
			

//			EncFSFileProvider rootProvider = ((EDVolumeListActivity) getActivity())
//					.getFileProvider(volumeType, "/");
			
			EncFSFileProvider rootProvider = fileProvider;
			fileProvider.changeRootPath("/");
					
			try {
				if (!rootProvider.exists(args[0])) {
					mErrDialogText = String.format(
							getString(R.string.error_dir_not_found), args[0]);
					return false;
				}

				volumePath = EncFSVolume.combinePath(args[0], volumeName
						+ NEW_VOLUME_DIR_SUFFIX);

				if (rootProvider.exists(volumePath)) {
					mErrDialogText = getString(R.string.error_file_exists);
					return false;
				}

				// Create the new directory
				if (!rootProvider.mkdir(volumePath)) {
					mErrDialogText = String.format(
							getString(R.string.error_mkdir_fail), volumePath);
					return false;
				}
			} catch (Exception e) {
				mErrDialogText = e.getMessage();
				return false;
			}


//			EncFSFileProvider fileProvider = ((EDVolumeListActivity) getActivity())
//					.getFileProvider(volumeType, volumePath);
			//EncFSFileProvider fileProvider=fileProvider
			fileProvider.changeRootPath(volumePath);
			// Create the volume
			try {
				
				//set config compatible with boxcryptor
				EncFSConfig config = new EncFSConfig();
				config.setFilenameAlgorithm(EncFSFilenameEncryptionAlgorithm.STREAM);
				config.setVolumeKeySizeInBits(256);
				config.setEncryptedFileBlockSizeInBytes(1024);
				config.setUseUniqueIV(false);
				config.setChainedNameIV(false);
				config.setHolesAllowedInFiles(true);
				config.setIterationForPasswordKeyDerivationCount(5000);
				config.setNumberOfMACBytesForEachFileBlock(0);
				config.setNumberOfRandomBytesInEachMACHeader(0);
				config.setSupportedExternalIVChaining(false);
							
				
				
				new EncFSVolumeBuilder().withFileProvider(fileProvider,volumeName)
						.withConfig(config)
						.withPbkdf2Provider(mApp.getNativePBKDF2Provider())
						.withPassword(password).writeVolumeConfig();
			} catch (Exception e) {
				mErrDialogText = e.getMessage();
				return false;
			}

			return true;
		}

		// Run after the task is complete
		
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);

			if (myDialog != null && myDialog.isShowing()) {
				myDialog.dismiss();
			}

			if (mProgDialog != null && mProgDialog.isShowing()){
				mProgDialog.dismiss();
			}

			if (!isCancelled()) {
				if (result) {
					((EDVolumeListActivity) getActivity()).importVolume(
							volumeName, volumePath, fileProvider);
				} else {
					((EDVolumeListActivity) getActivity())
							.showDialog(DIALOG_ERROR);
				}
			}
		}
	}

	private class DeleteVolumeTask extends EDAsyncTask<String, Void, Boolean> {

		// Volume being deleted
		private EDVolume volume;
		private EncdroidFileProvider fileProvider;

		public DeleteVolumeTask(ProgressDialog dialog, EDVolume volume, EncdroidFileProvider _fileProvider) {
			super();
			setProgressDialog(dialog);
			this.volume = volume;
			this.fileProvider=_fileProvider;
		}

		
		protected Boolean doInBackground(String... args) {

//			EncFSFileProvider rootProvider = ((EDVolumeListActivity) getActivity())
//					.getFileProvider(volume.getFileProviderType(), "/");

			EncFSFileProvider rootProvider = fileProvider;
			fileProvider.changeRootPath("/");
			
			try {
				if (!rootProvider.exists(volume.getPath())) {
					mErrDialogText = String.format(
							getString(R.string.error_dir_not_found),
							volume.getPath());
					return false;
				}

				// Delete the volume
				if (!rootProvider.delete(volume.getPath())) {
					mErrDialogText = String.format(
							getString(R.string.error_delete_fail),
							volume.getPath());
					return false;
				}
			} catch (IOException e) {
				mErrDialogText = e.getMessage();
				return false;
			}

			return true;
		}

		// Run after the task is complete
		
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);

			if (myDialog != null && myDialog.isShowing()) {
				myDialog.dismiss();
			}

			if (!isCancelled()) {
				if (result) {
					((EDVolumeListActivity) getActivity()).deleteVolume(volume);
				} else {
					((EDVolumeListActivity) getActivity())
							.showDialog(DIALOG_ERROR);
				}
			}
		}
	}
}