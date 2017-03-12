package org.mrpdaemon.android.encdroidmc.asyncTasks;

import org.mrpdaemon.android.encdroidmc.EDAsyncTask;
import org.mrpdaemon.android.encdroidmc.EDLogger;
import org.mrpdaemon.android.encdroidmc.EDProgressListener;
import org.mrpdaemon.android.encdroidmc.EDVolumeBrowserActivity;
import org.mrpdaemon.android.encdroidmc.R;
import org.mrpdaemon.sec.encfs.EncFSVolume;

import android.app.ProgressDialog;

public class PasteFileTask extends EDAsyncTask<Void, Void, Boolean> {
	// Logger tag
	private final static String TAG = "PasteFileTask";
	private EDVolumeBrowserActivity volumeActivity;
	
	public PasteFileTask(EDVolumeBrowserActivity volumeActivity, NotificationHelper dialog) {
		super();
		this.volumeActivity=volumeActivity;
		setProgressDialog(dialog);
	}

	
	@Override
	protected Boolean doInBackground(Void... args) {

		try {
			boolean result;
			if (volumeActivity.mPasteMode == volumeActivity.PASTE_OP_CUT) {
				result = volumeActivity.getmVolume().movePath(volumeActivity.mPasteFile.getPath(),
						EncFSVolume.combinePath(volumeActivity.mCurEncFSDir, volumeActivity.mPasteFile),
						new EDProgressListener(this));
			} else {
				// If destination path exists, use a duplicate name
				String combinedPath = EncFSVolume.combinePath(volumeActivity.mCurEncFSDir,
						volumeActivity.mPasteFile);
				if (volumeActivity.getmVolume().pathExists(combinedPath)) {
					// Bump up a counter until path doesn't exist
					int counter = 0;
					do {
						counter++;
						combinedPath = EncFSVolume.combinePath(
								volumeActivity.mCurEncFSDir, "(Copy " + counter + ") "
										+ volumeActivity.mPasteFile.getName());
					} while (volumeActivity.getmVolume().pathExists(combinedPath));

					result = volumeActivity.getmVolume().copyPath(volumeActivity.mPasteFile.getPath(),
							combinedPath, new EDProgressListener(this));
				} else {
					result = volumeActivity.getmVolume().copyPath(volumeActivity.mPasteFile.getPath(),
							volumeActivity.mCurEncFSDir.getPath(), new EDProgressListener(
									this));
				}
			}

			if (result == false) {
				if (volumeActivity.mPasteMode == volumeActivity.PASTE_OP_CUT) {
					volumeActivity.mErrDialogText = String.format(
							volumeActivity.getString(R.string.error_move_fail),
							volumeActivity.mPasteFile.getName(), volumeActivity.mCurEncFSDir.getPath());
				} else {
					volumeActivity.mErrDialogText = String.format(
							volumeActivity.getString(R.string.error_copy_fail),
							volumeActivity.mPasteFile.getName(), volumeActivity.mCurEncFSDir.getPath());
				}

				return false;
			}
		} catch (Exception e) {
			if (e.getMessage() == null) {
				volumeActivity.mErrDialogText = volumeActivity.getString(R.string.paste_fail);
			} else {
				EDLogger.logException(TAG, e);
				volumeActivity.mErrDialogText = e.getMessage();
			}
			return false;
		}

		return true;
	}

	// Run after the task is complete
	
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);

		if (myDialog.isShowing()) {
			myDialog.dismiss();
		}

		EDVolumeBrowserActivity myActivity = (EDVolumeBrowserActivity) getActivity();
		myActivity.mPasteFile = null;
		myActivity.mPasteMode = volumeActivity.PASTE_OP_NONE;

		if (volumeActivity.mApp.isActionBarAvailable()) {
			myActivity.mActionBar.invalidateOptionsMenu(myActivity);
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
