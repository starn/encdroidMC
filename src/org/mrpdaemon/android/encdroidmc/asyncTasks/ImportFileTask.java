package org.mrpdaemon.android.encdroidmc.asyncTasks;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.mrpdaemon.android.encdroidmc.EDAsyncTask;
import org.mrpdaemon.android.encdroidmc.EDLogger;
import org.mrpdaemon.android.encdroidmc.EDVolumeBrowserActivity;
import org.mrpdaemon.android.encdroidmc.EDVolumeListActivity;
import org.mrpdaemon.android.encdroidmc.R;
import org.mrpdaemon.sec.encfs.EncFSFile;
import org.mrpdaemon.sec.encfs.EncFSVolume;

import android.content.Context;
import fr.starn.FileSynchronizerService;

public class ImportFileTask extends EDAsyncTask<Void, Void, Boolean> {
	// Logger tag
	private final static String TAG = "SyncFileTask";
	//private EDVolumeBrowserActivity volumeActivity;
	private EncFSVolume volume;
	
	// Source file
	private File srcFile;

	// Destination file
	private EncFSFile dstFile;
	private String destFolder;
	Context ctx;
	
	private int nbFilesImported;
	private int nbFilesRemoved;
	private boolean removeOriginalFile;
	
	//if import launched from volumeActivity, will reload displayed files
	//else keep this variable null
	private EDVolumeBrowserActivity volumeActivity;
	
	/**
	 * if srcFile is null we import EDVolumeListActivity.fileNameImportedFromExternalApp
	 * @param dialog
	 * @param srcFile
	 */
	//public ImportFileTask(EDVolumeBrowserActivity volumeActivity, NotificationHelper dialog, File srcFile) {
	public ImportFileTask(Context ctx, EncFSVolume volume, File srcFile, String destFolder, NotificationHelper dialog, boolean removeOriginalFile) {
		super();
		setProgressDialog(dialog);
		
		this.srcFile = srcFile;
		this.volume=volume;
		this.destFolder=destFolder;
		this.ctx=ctx;
		this.removeOriginalFile=removeOriginalFile;
	}
	
	public ImportFileTask(EDVolumeBrowserActivity volumeActivity, EncFSVolume volume, File srcFile, String destFolder, NotificationHelper dialog) {
		this(volumeActivity.getApplicationContext(), volume, srcFile, destFolder, dialog,false);
		this.volumeActivity=volumeActivity;
	}
	
	@Override
	protected void onPreExecute() {
		// TODO Auto-generated method stub
		super.onPreExecute();
		//myDialog = new NotificationHelper(volumeActivity.getApplicationContext());
	}

	
	@Override
	protected Boolean doInBackground(Void... args) {
		if (srcFile!=null){
			// Create destination encFS file or directory
			try {
				//String dstPath = EncFSVolume.combinePath(volumeActivity.mCurEncFSDir, srcFile.getName());
				String dstPath = EncFSVolume.combinePath(destFolder, srcFile.getName());
				if (!srcFile.exists()) {
					showMessage("Source file "+srcFile.getAbsolutePath()+" does not exist");
					return false;
				}
				if (srcFile.isDirectory()) {
					if (volume.pathExists(dstPath) && volume.isDir(dstPath)){
						dstFile = volume.getFile(dstPath);
					}
					else if (volume.makeDir(dstPath)) {
						dstFile = volume.getFile(dstPath);
					} else {
						showMessage( String.format(ctx.getString(R.string.error_mkdir_fail), dstPath) );
						return false;
					}
				} else {
//					dstFile = volume.createFile(dstPath);
					boolean fileExists = volume.exists(dstPath);
					//boolean fileExistAndIdentical = fileExists && volume.getFile(dstPath).getLength()==srcFile.length();
					if (!fileExists) dstFile = volume.createFile(dstPath);
				}
			} catch (Exception e) {
				EDLogger.logException(TAG, e);
				showMessage( e.getMessage() );
				return false;
			}

			if (srcFile.isDirectory()) {
				if (myDialog!=null) myDialog.setMax(countFiles(srcFile));
				return recursiveImport(srcFile, dstFile, this);
			} else {
				// Use size of the file
				if (myDialog!=null) myDialog.setMax((int) srcFile.length());
				try {
					FileInputStream fis = new FileInputStream(srcFile);
					long size = srcFile.length();
					showMessage("import file "+ srcFile.getName());
					boolean result = AsyncTasksUtil.importFile(fis, size, dstFile, this);
					nbFilesImported++;
					if (removeOriginalFile) {
						srcFile.delete();
						nbFilesRemoved++;
					}
					return result;
				} catch (Exception e){
					e.printStackTrace();
					return false;
				}
			}
		}
		
		
		//case srcFile is null because sourceFile is on variable EDVolumeListActivity.fileNameImportedFromExternalApp
		if (EDVolumeListActivity.fileNameImportedFromExternalApp!=null && EDVolumeListActivity.fileISImportedFromExternalApp!=null){
			try {
				
				String combinedPath = EncFSVolume.combinePath(destFolder,EDVolumeListActivity.fileNameImportedFromExternalApp);
				if (volume.pathExists(combinedPath)) {
					// Bump up a counter until path doesn't exist
					int counter = 0;
					do {
						counter++;
						combinedPath = EncFSVolume.combinePath(destFolder,EDVolumeListActivity.fileNameImportedFromExternalApp);
						String filenameWithoutExt = combinedPath.substring(0,combinedPath.lastIndexOf('.'));
						String ext = combinedPath.substring(combinedPath.lastIndexOf('.'));
						combinedPath = filenameWithoutExt+counter+ext;
					} while (volume.pathExists(combinedPath));

				}
				
				
				String dstPath = combinedPath;
				dstFile = volume.createFile(dstPath);
				if (srcFile!=null && myDialog!=null) myDialog.setMax((int) srcFile.length());
				if (EDVolumeListActivity.fileISImportedFromExternalApp!=null && myDialog!=null) myDialog.setMax((int) EDVolumeListActivity.fileSizeImportedFromExternalApp);
				boolean importOK = AsyncTasksUtil.importFile(EDVolumeListActivity.fileISImportedFromExternalApp,EDVolumeListActivity.fileSizeImportedFromExternalApp, dstFile, this);
				if  (importOK){
					EDVolumeBrowserActivity.isCtxImportFromExternalApp=false;
					nbFilesImported++;
					return true;
				}
				return false;
			} catch (Exception e){
				e.printStackTrace();
			}
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
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
				showMessage(sdf.format(new Date())+" - "+ctx.getString(R.string.toast_files_imported)+": "+nbFilesImported);
				if (volumeActivity!=null) volumeActivity.launchFillTask(false);
			} else {
				//volumeActivity.showDialog(volumeActivity.DIALOG_ERROR);
				if (volumeActivity!=null) volumeActivity.launchFillTask(false);
			}
		}
	}
	
	// Count files and directories under the given file
	private int countFiles(File file) {
		if (file.isDirectory()) {
			int dirCount = 1;
			for (File subFile : file.listFiles()) {
				dirCount += countFiles(subFile);
			}
			return dirCount;
		} else {
			return 1;
		}
	}
	
	
	// Import all files/dirs under the given file to the given EncFS dir
	private boolean recursiveImport(File srcDir, EncFSFile dstDir,
			EDAsyncTask<Void, Void, Boolean> task) {
		for (File file : srcDir.listFiles()) {

			String dstPath = EncFSVolume.combinePath(dstDir, file.getName());

			try {
				if (file.isDirectory()) { // Directory
					if (volume.pathExists(dstPath) && volume.isDir(dstPath)){
						task.incrementProgressBy(1);
						// Import all files/folders under this dir
						if (recursiveImport(file, volume.getFile(dstPath),
								task) == false) {
							return false;
						}
					}
					else if (volume.makeDir(dstPath)) {
						task.incrementProgressBy(1);
						// Import all files/folders under this dir
						if (!recursiveImport(file, volume.getFile(dstPath),task)) {
							return false;
						}
					} 
					else {
						showMessage( String.format(ctx.getString(R.string.error_mkdir_fail), dstPath) );
						return false;
					}
				} else { // Import an individual file
					FileInputStream fis = new FileInputStream(file);
					long size = file.length();
					boolean fileExists = volume.exists(dstPath);
					boolean fileExistAndIdentical = fileExists && volume.getFile(dstPath).getLength()==size;
					
					if (fileExists && !fileExistAndIdentical)  volume.deletePath(dstPath, false);
					if (!fileExistAndIdentical) {
						//Toast.makeText(ctx,file.getName()+"...", Toast.LENGTH_LONG).show();
						showMessage("import file "+file.getName());
						if (!AsyncTasksUtil.importFile(fis,size, volume.createFile(dstPath), null) ) {
							return false;
						}
						else {
							nbFilesImported++;
							if (removeOriginalFile) file.delete();
						}
					}
					task.incrementProgressBy(1);
				}
			} catch (Exception e) {
				EDLogger.logException(TAG, e);
				showMessage( e.getMessage() );
				return false;
			}
		}
		return true;
	}
	
	private void showMessage(String msg){
//		try {
//			Toast.makeText(ctx,msg, Toast.LENGTH_LONG).show();
//		} catch (Throwable t){
//			t.printStackTrace();
//		}
		if (FileSynchronizerService.getInstance()!=null) FileSynchronizerService.getInstance().refreshNotifyBar(msg);
	}

}
