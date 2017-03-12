package org.mrpdaemon.android.encdroidmc.asyncTasks;

import java.util.Stack;

import org.mrpdaemon.android.encdroidmc.EDAsyncTask;
import org.mrpdaemon.android.encdroidmc.EDLogger;
import org.mrpdaemon.android.encdroidmc.EDVolumeBrowserActivity;
import org.mrpdaemon.sec.encfs.EncFSFile;
import org.mrpdaemon.sec.encfs.EncFSVolume;

import android.app.ProgressDialog;
import android.os.Bundle;

public class ActivityRestoreTask extends EDAsyncTask<Void, Void, Boolean> {
	// Logger tag
	private final static String TAG = "MetadataOpTask";
	
	private EDVolumeBrowserActivity volumeActivity;
	// Saved instance state to restore from
	private Bundle savedInstanceState;

	public ActivityRestoreTask(EDVolumeBrowserActivity volumeActivity, NotificationHelper dialog,
			Bundle savedInstanceState) {
		super();
		this.savedInstanceState = savedInstanceState;
		setProgressDialog(dialog);
		this.volumeActivity=volumeActivity;
	}

	
	@Override
	protected Boolean doInBackground(Void... arg0) {
		// Activity restored after being killed
		try {
			// XXX: volume.getFile("/") should return volume.getRootDir()
			if (volumeActivity.mSavedCurDirPath.equals(EncFSVolume.ROOT_PATH)) {
				volumeActivity.mCurEncFSDir = volumeActivity.getmVolume().getRootDir();
			} else {
				volumeActivity.mCurEncFSDir = volumeActivity.getmVolume().getFile(savedInstanceState
						.getString(volumeActivity.SAVED_CUR_DIR_PATH_KEY));
			}
		} catch (Exception e) {
			EDLogger.logException(TAG, e);
			volumeActivity.exitToVolumeList();
		}
		volumeActivity.mDirStack = getFileStackForEncFSDir(volumeActivity.mCurEncFSDir);

		// Restore paste state
		if (volumeActivity.mPasteMode != volumeActivity.PASTE_OP_NONE) {
			try {
				volumeActivity.mPasteFile = volumeActivity.getmVolume().getFile(savedInstanceState
						.getString(volumeActivity.SAVED_PASTE_FILE_PATH_KEY));
			} catch (Exception e) {
				EDLogger.logException(TAG, e);
				volumeActivity.exitToVolumeList();
			}
		}

		return true;
	}

	
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);

		if (myDialog != null && myDialog.isShowing()) {
			myDialog.dismiss();
		}

		volumeActivity.launchFillTask(false);
	}
	
	/*
	 * Given an EncFSFile for a directory create a stack of all directories
	 * starting from root and leading to it.
	 */
	private Stack<EncFSFile> getFileStackForEncFSDir(EncFSFile dir) {
		Stack<EncFSFile> tmpStack, resultStack;
		EncFSFile curDir;

		/*
		 * XXX: We should just compare against mVolume.getRootDir() - pending
		 * encfs-java issue XXX
		 */
		if (dir.getPath().equals(EncFSVolume.ROOT_PATH)) {
			return new Stack<EncFSFile>();
		}

		// Get the parent of the input directory
		try {
			curDir = volumeActivity.getmVolume().getFile(dir.getParentPath());
		} catch (Exception e) {
			EDLogger.logException(TAG, e);
			return new Stack<EncFSFile>();
		}

		// Work backwards until we hit the root directory
		tmpStack = new Stack<EncFSFile>();
		/*
		 * XXX: We should just compare against mVolume.getRootDir() - pending
		 * encfs-java issue XXX
		 */
		while (!curDir.getPath().equals(EncFSVolume.ROOT_PATH)) {
			tmpStack.push(curDir);
			try {
				curDir = volumeActivity.getmVolume().getFile(curDir.getParentPath());
			} catch (Exception e) {
				EDLogger.logException(TAG, e);
				return new Stack<EncFSFile>();
			}
		}

		// Add root directory to the stack
		tmpStack.push(volumeActivity.getmVolume().getRootDir());

		// Reverse tmpStack into resultStack
		resultStack = new Stack<EncFSFile>();
		if (!tmpStack.isEmpty()) {
			curDir = tmpStack.pop();
		} else {
			curDir = null;
		}
		while (curDir != null) {
			resultStack.push(curDir);
			if (!tmpStack.isEmpty()) {
				curDir = tmpStack.pop();
			} else {
				curDir = null;
			}
		}
		return resultStack;
	}
}