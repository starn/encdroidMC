package org.mrpdaemon.android.encdroidmc.asyncTasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.mrpdaemon.android.encdroidmc.EDAsyncTask;
import org.mrpdaemon.android.encdroidmc.EDLogger;
import org.mrpdaemon.sec.encfs.EncFSFile;
import org.mrpdaemon.sec.encfs.EncFSFileInputStream;
import org.mrpdaemon.sec.encfs.EncFSFileOutputStream;

public class AsyncTasksUtil {
	// Logger tag
	private final static String TAG = "AsyncTasksUtil";
	
		public static boolean copyStreams(InputStream is, OutputStream os,
				EDAsyncTask<Void, Void, Boolean> task) {
			try {
				byte[] buf = new byte[8192];
				int bytesRead = 0;
				try {
					try {
						bytesRead = is.read(buf);
						int countBytes = 0;
						int countLoop = 0;
						while (bytesRead >= 0) {
							os.write(buf, 0, bytesRead);
							bytesRead = is.read(buf);
							if (task != null && countLoop%10==0) {
								task.setProgress(countBytes);
							}
							countBytes+=8192;
							countLoop++;
						}
					} finally {
						is.close();
					}
				} finally {
					os.close();
				}
			} catch (IOException e) {
				EDLogger.logException(TAG, e);
				//mErrDialogText = e.getMessage();
				e.printStackTrace();
				return false;
			}
	
			return true;
		}
		
		
		public static boolean exportFile(EncFSFile srcFile, File dstFile,
				EDAsyncTask<Void, Void, Boolean> task, long fileLength) {
			EncFSFileInputStream efis = null;
			try {
				efis = new EncFSFileInputStream(srcFile,0);
			} catch (Exception e) {
				EDLogger.logException(TAG, e);
				//volumeBrowserActivity.mErrDialogText = e.getMessage();
				e.printStackTrace();
				return false;
				
			}
			
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(dstFile);
			} catch (FileNotFoundException e) {
				EDLogger.logException(TAG, e);
				//volumeBrowserActivity.mErrDialogText = e.getMessage();
				try {
					efis.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
					//volumeBrowserActivity.mErrDialogText += ioe.getMessage();
				}
				return false;
			}
			task.setMaxProgress(fileLength);
			return AsyncTasksUtil.copyStreams(efis, fos, task);
		}
		
		public static boolean importFile(InputStream fis, long length, EncFSFile dstFile,
				EDAsyncTask<Void, Void, Boolean> task) {


			EncFSFileOutputStream efos = null;
			try {
				
				efos = new EncFSFileOutputStream(dstFile, length);
			} catch (Exception e) {
				EDLogger.logException(TAG, e);
				//mErrDialogText = e.getMessage();
				e.printStackTrace();
				try {
					fis.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
					//mErrDialogText += ioe.getMessage();
				}
				return false;
			}
			if (task!=null) task.setMaxProgress(length);
			return AsyncTasksUtil.copyStreams(fis, efos, task);
		}
}
