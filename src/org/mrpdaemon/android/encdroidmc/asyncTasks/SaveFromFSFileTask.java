package org.mrpdaemon.android.encdroidmc.asyncTasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.mrpdaemon.android.encdroidmc.EDAsyncTask;
import org.mrpdaemon.android.encdroidmc.EDLogger;
import org.mrpdaemon.android.encdroidmc.EDVolumeBrowserActivity;
import org.mrpdaemon.android.encdroidmc.R;
import org.mrpdaemon.android.encdroidmc.tools.SecureDeleteThread;
import org.mrpdaemon.sec.encfs.EncFSFile;
import org.mrpdaemon.sec.encfs.EncFSFileOutputStream;

import android.app.ProgressDialog;
import android.widget.Toast;

public class SaveFromFSFileTask extends EDAsyncTask<Void, Void, Boolean> {
	// Logger tag
	private final static String TAG = "SyncFileTask";
	// Source file
	private File srcFile;

	// Destination file
	private EncFSFile dstFile;
	private EDVolumeBrowserActivity volumeActivity;

	public SaveFromFSFileTask(EDVolumeBrowserActivity volumeActivity, NotificationHelper dialog, File srcFile,
			EncFSFile dstFile) {
		super();
		setProgressDialog(dialog);
		this.srcFile = srcFile;
		this.dstFile = dstFile;
		this.volumeActivity=volumeActivity;

		// Set dialog max in KB
		myDialog.setMax((int) srcFile.length());
	}

	
	@Override
	protected Boolean doInBackground(Void... args) {
		/*
		 * If the activity gets destroyed/recreated then we need to obtain
		 * dstFile again.
		 */
		if ((dstFile == null) && (volumeActivity.mOpenFilePath != null)) {
			try {
				dstFile = volumeActivity.getmVolume().getFile(volumeActivity.mOpenFilePath);
			} catch (Exception e) {
				EDLogger.logException(TAG, e);
				volumeActivity.exitToVolumeList();
			}
		}
		try {
			FileInputStream fis = new FileInputStream(srcFile);
			long size = srcFile.length();
			return AsyncTasksUtil.importFile(fis, size, dstFile, this);
		} catch (Exception e){
			e.printStackTrace();
			return null;
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
			if (result != null && result == true) {
				// Delete the file
				//srcFile.delete();
				
				//secure delete, remplace file content with random bits before delete
				new SecureDeleteThread(srcFile).start();

				

				// Show toast
				Toast.makeText(
						volumeActivity.getApplicationContext(),
						String.format(
								volumeActivity.getString(R.string.toast_encrypt_file),
								volumeActivity.mOpenFileName), Toast.LENGTH_SHORT).show();

				// Refresh view to get byte size changes
				volumeActivity.launchFillTask(false);
			} else {
				volumeActivity.showDialog(volumeActivity.DIALOG_ERROR);
			}
		}
	}
	


}
