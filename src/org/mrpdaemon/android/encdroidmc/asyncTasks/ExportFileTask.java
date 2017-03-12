package org.mrpdaemon.android.encdroidmc.asyncTasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.mrpdaemon.android.encdroidmc.EDAsyncTask;
import org.mrpdaemon.android.encdroidmc.EDLogger;
import org.mrpdaemon.android.encdroidmc.EDVolumeBrowserActivity;
import org.mrpdaemon.android.encdroidmc.R;
import org.mrpdaemon.sec.encfs.EncFSFile;
import org.mrpdaemon.sec.encfs.EncFSFileInputStream;
import org.mrpdaemon.sec.encfs.EncFSVolume;

import android.app.ProgressDialog;
import android.widget.Toast;

public class ExportFileTask extends EDAsyncTask<Void, Void, Boolean> {
	// Logger tag
	private final static String TAG = "ExportFileTask";
	// Source file
	private EncFSFile srcFile;

	// Destination file
	private File dstFile;
	private EDVolumeBrowserActivity volumeBrowserActivity;

	public ExportFileTask(EDVolumeBrowserActivity volumeBrowserActivity,NotificationHelper dialog, EncFSFile srcFile, File dstFile) {
		super();
		setProgressDialog(dialog);
		this.srcFile = srcFile;
		this.dstFile = dstFile;
		this.volumeBrowserActivity=volumeBrowserActivity;
	}

	
	@Override
	protected Boolean doInBackground(Void... args) {
		/*
		 * If the activity gets destroyed/recreated then we need to obtain
		 * srcFile again.
		 */
		if ((srcFile == null) && (volumeBrowserActivity.mOpenFilePath != null)) {
			try {
				srcFile = volumeBrowserActivity.getmVolume().getFile(volumeBrowserActivity.mOpenFilePath);
			} catch (Exception e) {
				EDLogger.logException(TAG, e);
				volumeBrowserActivity.exitToVolumeList();
			}
		}

		if (srcFile.isDirectory()) {
			myDialog.setMax(EncFSVolume.countFiles(srcFile));

			// Create destination dir
			if (dstFile.mkdir()) {
				myDialog.incrementProgressBy(1);
			} else {
				volumeBrowserActivity.mErrDialogText = String.format(
						volumeBrowserActivity.getString(R.string.error_mkdir_fail),
						dstFile.getAbsolutePath());
				return false;
			}

			return recursiveExport(srcFile, dstFile, this);
		} else {
			// Use size of the file
			return AsyncTasksUtil.exportFile(srcFile, dstFile, this,srcFile.getLength());
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
				// Show toast
				Toast.makeText(volumeBrowserActivity.getApplicationContext(),
						volumeBrowserActivity.getString(R.string.toast_files_exported),
						Toast.LENGTH_SHORT).show();
			} else {
				volumeBrowserActivity.showDialog(volumeBrowserActivity.DIALOG_ERROR);
			}
		}
	}
	
	// Export all files/dirs under the EncFS dir to the given dir
	public boolean recursiveExport(EncFSFile srcDir, File dstDir,
			EDAsyncTask<Void, Void, Boolean> task) {
		try {
			for (EncFSFile file : srcDir.listFiles()) {

				File dstFile = new File(dstDir, file.getName());

				if (file.isDirectory()) { // Directory
					if (dstFile.mkdir()) {
						task.incrementProgressBy(1);
						// Export all files/folders under this dir
						if (recursiveExport(file, dstFile, task) == false) {
							return false;
						}
					} else {
						volumeBrowserActivity.mErrDialogText = String.format(
								volumeBrowserActivity.getString(R.string.error_mkdir_fail),
								dstFile.getAbsolutePath());
						return false;
					}
				} else { // Export an individual file
					if (AsyncTasksUtil.exportFile(file, dstFile, null,file.getLength()) == false) {
						return false;
					}
					task.incrementProgressBy(1);
				}
			}
		} catch (Exception e) {
			EDLogger.logException(TAG, e);
			volumeBrowserActivity.mErrDialogText = e.getMessage();
			return false;
		}
		return true;
	}
	

}