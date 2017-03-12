package org.mrpdaemon.android.encdroidmc.asyncTasks;

import org.mrpdaemon.android.encdroidmc.EDAsyncTask;
import org.mrpdaemon.android.encdroidmc.EDLogger;
import org.mrpdaemon.android.encdroidmc.EDProgressListener;
import org.mrpdaemon.android.encdroidmc.EDVolumeBrowserActivity;
import org.mrpdaemon.android.encdroidmc.R;
import org.mrpdaemon.sec.encfs.EncFSFile;
import org.mrpdaemon.sec.encfs.EncFSVolume;

import android.app.ProgressDialog;

public class MetadataOpTask extends EDAsyncTask<Void, Void, Boolean> {
	// Logger tag
	private final static String TAG = "MetadataOpTask";
	
	private EDVolumeBrowserActivity volumeActivity;
	// Valid modes for the task
	public static final int DELETE_FILE = 0;
	public static final int RENAME_FILE = 1;
	public static final int CREATE_DIR = 2;
	public static final int CREATE_FILE = 3;

	// mode for the current task
	private int mode;

	// String argument for the task
	private String strArg;

	public MetadataOpTask(EDVolumeBrowserActivity volumeActivity, NotificationHelper dialog, int mode, String strArg) {
		super();
		setProgressDialog(dialog);
		this.mode = mode;
		this.strArg = strArg;
		this.volumeActivity=volumeActivity;
	}

	
	@Override
	protected Boolean doInBackground(Void... args) {
		switch (mode) {
		case DELETE_FILE:
			try {
				// boolean result = mSelectedFile.getFile().delete();
				boolean result = volumeActivity.getmVolume().deletePath(volumeActivity.mSelectedFile.getFile()
						.getPath(), true, new EDProgressListener(this));

				if (result == false) {
					volumeActivity.mErrDialogText = String.format(
							volumeActivity.getString(R.string.error_delete_fail),
							volumeActivity.mSelectedFile.getName());
					return false;
				}
			} catch (Exception e) {
				EDLogger.logException(TAG, e);
				volumeActivity.mErrDialogText = e.getMessage();
				return false;
			}
			return true;
		case RENAME_FILE:
			try {
				String dstPath = EncFSVolume.combinePath(volumeActivity.mCurEncFSDir,
						strArg);

				// Check if the destination path exists
				if (volumeActivity.getmVolume().pathExists(dstPath)) {
					volumeActivity.mErrDialogText = String.format(
							volumeActivity.getString(R.string.error_path_exists), dstPath);
					return false;
				}

				boolean result = volumeActivity.getmVolume().movePath(
						EncFSVolume.combinePath(volumeActivity.mCurEncFSDir,
								volumeActivity.mSelectedFile.getName()), dstPath,
						new EDProgressListener(this));

				if (result == false) {
					volumeActivity.mErrDialogText = String.format(
							volumeActivity.getString(R.string.error_rename_fail),
							volumeActivity.mSelectedFile.getName(), strArg);
					return false;
				}
			} catch (Exception e) {
				EDLogger.logException(TAG, e);
				volumeActivity.mErrDialogText = e.getMessage();
				return false;
			}
			return true;
		case CREATE_DIR:
			try {
				boolean result = volumeActivity.getmVolume().makeDir(EncFSVolume.combinePath(
						volumeActivity.mCurEncFSDir, strArg));

				if (result == false) {
					volumeActivity.mErrDialogText = String.format(
							volumeActivity.getString(R.string.error_mkdir_fail), strArg);
					return false;
				}
			} catch (Exception e) {
				EDLogger.logException(TAG, e);
				volumeActivity.mErrDialogText = e.getMessage();
				return false;
			}
			return true;
		case CREATE_FILE:
			try {
				EncFSFile result = volumeActivity.getmVolume().createFile(EncFSVolume.combinePath(volumeActivity.mCurEncFSDir, strArg));
			} catch (Exception e) {
				EDLogger.logException(TAG, e);
				volumeActivity.mErrDialogText = e.getMessage();
				return false;
			}
			return true;
		default:
			return false;
		}
	}

	// Run after the task is complete
	
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);

		if (myDialog.isShowing()) {
			myDialog.dismiss();
		}

		if (!isCancelled()) {
			if (result == true) {
				volumeActivity.launchFillTask(false);
			} else {
				volumeActivity.showDialog(volumeActivity.DIALOG_ERROR);
				volumeActivity.launchFillTask(false);
			}
		}
	}
}
