package org.mrpdaemon.android.encdroidmc.asyncTasks;

import java.io.File;

import org.mrpdaemon.android.encdroidmc.EDAsyncTask;
import org.mrpdaemon.android.encdroidmc.EDVolumeBrowserActivity;
import org.mrpdaemon.android.encdroidmc.MimeManagement;
import org.mrpdaemon.android.encdroidmc.MusicPlayer;
import org.mrpdaemon.android.encdroidmc.R;
import org.mrpdaemon.sec.encfs.EncFSFile;
import org.paulmach.textedit.pmTextEdit;

import uk.co.senab.photoview.ViewPagerActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import fr.starn.PersistantInstanceManager;
import fr.starn.ThumbnailManager;
import fr.starn.webdavServer.HttpServer;

public class ViewFileTask extends EDAsyncTask<Void, Void, Boolean> {
	// Logger tag
	private final static String TAG = "SyncFileTask";
	
	// Source file
	private EncFSFile srcFile;

	// Destination file
	private File dstFile;
	
	private boolean doNotLaunchPostExecute;
	private EncFSFile[] childEncFSFiles;
	private EDVolumeBrowserActivity volumeActivity;
	private boolean sendToExternalApp;
	
	public ViewFileTask(EDVolumeBrowserActivity volumeActivity,  EncFSFile[] childEncFSFiles, NotificationHelper dialog, EncFSFile srcFile,
			File dstFile) {
		super();
		setProgressDialog(dialog);
		this.srcFile = srcFile;
		this.dstFile = dstFile;
		this.childEncFSFiles=childEncFSFiles;
		// Set dialog max in KB
		myDialog.setMax((int) srcFile.getLength());
		this.volumeActivity=volumeActivity;
	}
	
	public ViewFileTask(EDVolumeBrowserActivity volumeActivity, NotificationHelper dialog, EncFSFile srcFile) {
		super();
		setProgressDialog(dialog);
		this.srcFile = srcFile;
		
		// Create sdcard dir if it doesn't exist
		File encDroidDir = new File(Environment.getExternalStorageDirectory(), EDVolumeBrowserActivity.ENCDROID_SD_DIR_NAME);
		if (!encDroidDir.exists()) {
			encDroidDir.mkdir();
		}
		dstFile = new File(encDroidDir, srcFile.getName());
		// Set dialog max in KB
		myDialog.setMax((int) srcFile.getLength());
		this.volumeActivity=volumeActivity;
		sendToExternalApp=true;
	}

	
	@Override
	protected Boolean doInBackground(Void... args) {
		if (sendToExternalApp){
			return AsyncTasksUtil.exportFile(srcFile, dstFile, this,srcFile.getLength());
		}
		else {
			if (srcFile.getName().endsWith(".mp3")){
				Intent intent = new Intent(volumeActivity.getApplicationContext(), MusicPlayer.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				volumeActivity.startActivity(intent);
				
				doNotLaunchPostExecute = true;
				return true;
		        
			}
			if (MimeManagement.isImage(srcFile.getName())){
				Intent intent = new Intent(volumeActivity.getApplicationContext(), ViewPagerActivity.class);
				intent.putExtra("selectedFile", srcFile.getName());
				volumeActivity.startActivity(intent);
				
				doNotLaunchPostExecute = true;
				return true;				
			}
			if (MimeManagement.isText(srcFile.getName())){
				PersistantInstanceManager.addEncfsFile(srcFile);
				Intent intent = new Intent(volumeActivity.getApplicationContext(), pmTextEdit.class);
				intent.putExtra("selectedFile", srcFile.getName());
				byte[]  fileContent = new byte[0];
				try {
					 fileContent = ThumbnailManager.inputStreamToByteArray( srcFile.openInputStream(0));
				} catch (Exception e){
					e.printStackTrace();
				}
				intent.putExtra("fileContent",fileContent);
				volumeActivity.startActivityForResult(intent,EDVolumeBrowserActivity.EDIT_TEXT_FILE);
				doNotLaunchPostExecute = true;
				return true;				
			}
			if (MimeManagement.isAudio(srcFile.getName()) || MimeManagement.isVideo(srcFile.getName())){
				try {
					
					HttpServer server = HttpServer.getInstance();
					String id = server.setFile(srcFile);
					
		            Intent intent = new Intent(Intent.ACTION_VIEW);
		            if (MimeManagement.isVideo(srcFile.getName())){
		            	intent.setDataAndType(Uri.parse("http://127.0.0.1:8080/"+id+"?nocache="+srcFile.getLength()), "video/*");
		            } else {
		            	intent.setDataAndType(Uri.parse("http://127.0.0.1:8080/"+id+"?nocache="+srcFile.getLength()), "audio/*");
		            }
		            volumeActivity.startActivity(Intent.createChooser(intent, "Complete action using"));					
				} catch (Exception e){
					e.printStackTrace();
				}
				doNotLaunchPostExecute = true;
				return true;
			}else {
				return AsyncTasksUtil.exportFile(srcFile, dstFile, this,srcFile.getLength());
			}
		}
	}

	// Run after the task is complete
	
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);

		if (myDialog.isShowing()) {
			myDialog.dismiss();
		}

		if (sendToExternalApp){
			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND);
			shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(dstFile));
			shareIntent.setType("image/jpeg");
			volumeActivity.startActivity(Intent.createChooser(shareIntent,"Send to"));

		}
		else if (!doNotLaunchPostExecute && !isCancelled()) {
			if (result == true) {
				// Set up a file observer
				volumeActivity.mFileObserver = new EDFileObserver(
						dstFile.getAbsolutePath());
				//mFileObserver.startWatching();

				// Figure out the MIME type
				String fileName = dstFile.getName();
				

				String mimeType =MimeManagement.getMimeType(fileName);


				// Launch viewer app
				Intent openIntent = new Intent(Intent.ACTION_VIEW);

				if (mimeType == null) {
					openIntent.setDataAndType(Uri.fromFile(dstFile),
							"application/unknown");
				} else {
					openIntent.setDataAndType(Uri.fromFile(dstFile),
							mimeType);
				}

				try {
					volumeActivity.startActivityForResult(openIntent, volumeActivity.VIEW_FILE_REQUEST);
				} catch (ActivityNotFoundException e) {
					volumeActivity.mErrDialogText = String.format(
							volumeActivity.getString(R.string.error_no_viewer_app),
							srcFile.getPath());
					Log.e(TAG, volumeActivity.mErrDialogText);
					volumeActivity.showDialog(volumeActivity.DIALOG_ERROR);
				}
			} else {
				volumeActivity.showDialog(volumeActivity.DIALOG_ERROR);
			}
		}
	}
}
