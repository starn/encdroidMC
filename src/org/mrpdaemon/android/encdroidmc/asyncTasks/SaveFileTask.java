package org.mrpdaemon.android.encdroidmc.asyncTasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.mrpdaemon.android.encdroidmc.EDAsyncTask;
import org.mrpdaemon.android.encdroidmc.EDLogger;
import org.mrpdaemon.android.encdroidmc.EDVolumeBrowserActivity;
import org.mrpdaemon.android.encdroidmc.EDVolumeListActivity;
import org.mrpdaemon.android.encdroidmc.R;
import org.mrpdaemon.sec.encfs.EncFSFile;
import org.mrpdaemon.sec.encfs.EncFSVolume;

import fr.starn.PersistantInstanceManager;
import android.app.ProgressDialog;
import android.widget.Toast;

public class SaveFileTask extends EDAsyncTask<Void, Void, Boolean> {
	// Logger tag
	private final static String TAG = "SaveFileTask";
	private EDVolumeBrowserActivity volumeActivity;
	
	InputStream is;
	long length;
	
	// Destination file
	private EncFSFile dstFile;
	
	/**
	 * if srcFile is null we import EDVolumeListActivity.fileNameImportedFromExternalApp
	 * @param dialog
	 * @param srcFile
	 */
	public SaveFileTask(EDVolumeBrowserActivity volumeActivity, NotificationHelper dialog, EncFSFile dstFile, InputStream is, long length) {
		super();
		setProgressDialog(dialog);
		
		this.dstFile = dstFile;
		this.is=is;
		this.volumeActivity=volumeActivity;
		this.length=length;
	}
	
	@Override
	protected void onPreExecute() {
		// TODO Auto-generated method stub
		super.onPreExecute();
		//myDialog = new NotificationHelper(volumeActivity.getApplicationContext());
	}

	
	@Override
	protected Boolean doInBackground(Void... args) {
		try {
			
			//EncFSFile dest = PersistantInstanceManager.getEncfsFile(filename);
			boolean importOK = AsyncTasksUtil.importFile(is,length,dstFile,this);
			if  (importOK){
				return true;
			}
			return false;
		} catch (Exception e){
			e.printStackTrace();
		}
		return false;

	}

	// Run after the task is complete
	
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);

		if (myDialog!=null && myDialog.isShowing()) {
			myDialog.dismiss();
			myDialog=null;
		}

		if (!isCancelled()) {
			if (result == true) {
				// Show toast
				Toast.makeText(volumeActivity.getApplicationContext(),
						volumeActivity.getString(R.string.toast_files_imported),
						Toast.LENGTH_SHORT).show();
				volumeActivity.launchFillTask(false);
			} else {
				volumeActivity.showDialog(volumeActivity.DIALOG_ERROR);
				volumeActivity.launchFillTask(false);
			}
		}
	}
	

}
