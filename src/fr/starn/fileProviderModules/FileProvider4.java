package fr.starn.fileProviderModules;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.util.ArrayList;
import java.util.List;

import org.mrpdaemon.android.encdroidmc.StaticConfig;
import org.mrpdaemon.android.encdroidmc.fileProvider.*;
import org.mrpdaemon.android.encdroidmc.tools.KeyValueBean;
import org.mrpdaemon.sec.encfs.EncFSFileInfo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.RESTUtility;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;


public class FileProvider4 extends EncdroidFileProvider implements Linkable{


	// API object
	public static DropboxAPI<AndroidAuthSession> api;



	public FileProvider4(){

	}
	
	@Override
	public void initialize(String path) throws Exception {


	}
	


//	// Generate absolute path for a given relative path
//	private String absPath(String relPath) {
//		// Take off leading '/' from relPath
//		if (relPath.charAt(0) == '/') {
//			relPath = relPath.substring(1);
//		}
//
//		if (rootPath.charAt(rootPath.length() - 1) == '/') {
//			return rootPath + relPath;
//		} else {
//			return rootPath + "/" + relPath;
//		}
//	}

	private void handleDropboxException(DropboxException e) throws IOException {
		if (e.getMessage() != null) {
			throw new IOException(e.getMessage());
		} else {
			throw new IOException(e.toString());
		}
	}

	
	public boolean copy(String srcPath, String dstPath) throws IOException {

		/*
		 * If destination path exists, delete it first. This is a workaround for
		 * encfs-java behavior without chainedNameIV, the file is
		 * touched/created before calling into this function causing the Dropbox
		 * API to return 403 Forbidden thinking that the file already exists.
		 */
		System.out.println("test if exist:"+getAbsolutePath( dstPath));
		if (exists( dstPath)) {
			delete(dstPath);
		}

		try {
			api.copy(getAbsolutePath(srcPath), getAbsolutePath(dstPath));
		} catch (DropboxException e) {
			handleDropboxException(e);
		}

		return true;
	}

	
	public EncFSFileInfo createFile(String path) throws IOException {
		Entry entry;

		try {
			entry = api.putFileOverwrite(getAbsolutePath(path), new FileInputStream(
					"/dev/zero"), 0, null);
		} catch (DropboxException e) {
			handleDropboxException(e);
			return null;
		}

		if (entry != null) {
			return entryToFileInfo(entry);
		}

		return null;
	}

	
	public boolean delete(String path) throws IOException {
		try {
			api.delete(getAbsolutePath(path));
		} catch (DropboxException e) {
			handleDropboxException(e);
		}

		return true;
	}

	
	public boolean exists(String path) throws IOException {

		try {
			String absolutePath=getAbsolutePath(path);
			Entry entry = api.metadata(absolutePath, 1, null, false, null);

			if (entry == null) {
				return false;
			}

			return !entry.isDeleted;
		} catch (DropboxServerException e) {
			// 404 NOT FOUND is a legitimate case
			if (e.error == DropboxServerException._404_NOT_FOUND) {
				return false;
			} else {
				handleDropboxException(e);
				return false;
			}
		} catch (DropboxException e) {
			handleDropboxException(e);
			return false;
		}
		catch (Exception e){
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private EncFSFileInfo entryToFileInfo(Entry entry) {
		String relativePath = getRelativePathFromAbsolutePath(entry.parentPath());

		return new EncFSFileInfo(entry.fileName(), relativePath, entry.isDir,
				RESTUtility.parseDate(entry.modified).getTime(), entry.bytes,
				true, true, true);
	}

	
	public EncFSFileInfo getFileInfo(String path) throws IOException {
		try {
			Entry entry = api.metadata(getAbsolutePath(path), 1, null, false, null);

			if (entry != null) {
				return entryToFileInfo(entry);
			}

			return null;
		} catch (DropboxException e) {
			handleDropboxException(e);
			return null;
		}
	}

	
	public boolean isDirectory(String path) throws IOException {
		try {
			Entry entry = api.metadata(getAbsolutePath(path), 1, null, false, null);
			return entry.isDir;
		} catch (DropboxException e) {
			handleDropboxException(e);
			return false;
		}
	}

	
	public List<EncFSFileInfo> fsList(String path) throws IOException {
		try {
			List<EncFSFileInfo> list = new ArrayList<EncFSFileInfo>();

			Entry dirEnt = api.metadata(getAbsolutePath(path), 0, null, true, null);

			if (!dirEnt.isDir) {
				IOException ioe = new IOException(path + " is not a directory");
				throw ioe;
			}

			// Add entries to list
			for (Entry childEnt : dirEnt.contents) {
				list.add(entryToFileInfo(childEnt));
			}

			return list;
		} catch (DropboxException e) {
			handleDropboxException(e);
			return null;
		}
	}

	
	public boolean mkdir(String path) throws IOException {
		try {
			api.createFolder(getAbsolutePath(path));
		} catch (DropboxException e) {
			handleDropboxException(e);
		}

		return true;
	}

	
	public boolean mkdirs(String path) throws IOException {
		// XXX: Not implemented
		IOException ioe = new IOException("NOT IMPLEMENTED");
		throw ioe;
	}

	
	public boolean move(String srcPath, String dstPath) throws IOException {
		try {
			api.move(getAbsolutePath(srcPath), getAbsolutePath(dstPath));
		} catch (DropboxException e) {
			handleDropboxException(e);
		}

		return true;
	}

	
//	public InputStream openInputStream(String path) throws IOException {
//		try {
//			return api.getFileStream(absPath(path), null);
//		} catch (DropboxException e) {
//			handleDropboxException(e);
//			return null;
//		}
//	}
//
//	
//	public OutputStream openOutputStream(String path, long length)
//			throws IOException {
//		return new EDDropboxOutputStream(api, absPath(path), length);
//	}
	
	@Override
	public InputStream fsDownload(String path, long startIndex) throws IOException {
		System.out.println("download"+getAbsolutePath(path));
		try {
			InputStream is = api.getFileStream(getAbsolutePath(path),null);
			if (startIndex>0) is.skip(startIndex);
			return is;
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void fsUpload(String path, PipedInputStream inputStream, long length)
			throws IOException {
		System.out.println("upload"+getAbsolutePath(path));
		try {
			api.putFileOverwrite(getAbsolutePath(path),inputStream,length,null);
		} catch (Exception e){
			throw new RuntimeException(e);
		}
		
	}

	


	@Override
	public String getProviderName() {
		return "Dropbox";
	}

	@Override
	public List<EncdroidProviderParameter> getParamsToAsk() {
		// TODO Auto-generated method stub
		return null;
	}

	
	
	
	@Override
	public void link(Activity activity) {


			//if (isLinked(activity)) return;
			AppKeyPair appKeys = new AppKeyPair(StaticConfig.dropboxAppKey, StaticConfig.dropboxAppSecret);
			AndroidAuthSession session = new AndroidAuthSession(appKeys, AccessType.DROPBOX);
			api = new DropboxAPI<AndroidAuthSession>(session);		
			
			KeyValueBean loginPwd = getLoginPwd(activity);
			AccessTokenPair atp = new AccessTokenPair(loginPwd.getKey(),loginPwd.getValue());
			api.getSession().setAccessTokenPair(atp);
			
			 //if (!api.getSession().isLinked()) {
	
			api.getSession().startAuthentication(activity);
		
	}	
	
	
	@Override
	public boolean onResume(Activity activity) {
	    if (api != null && api.getSession() != null && api.getSession().authenticationSuccessful()) {
	        try {
	            // MANDATORY call to complete auth.
	            // Sets the access token on the session
	        	api.getSession().finishAuthentication();

	            AccessTokenPair tokens = api.getSession().getAccessTokenPair();

	            // Provide your own storeKeys to persist the access token pair
	            // A typical way to store tokens is using SharedPreferences
	            //storeKeys(tokens.key, tokens.secret);
	            SharedPreferences prefs = activity.getSharedPreferences(
	            	      "org.mrpdaemon.android.encdroidmc", activity.MODE_PRIVATE);
				Editor edit = prefs.edit();
				edit.putBoolean("linked", true);
				edit.putString("accessKey", tokens.key);
				edit.putString("accessSecret", tokens.secret);
//				userName = mApi.accountInfo().displayName;
//				apiThread.start();
//				apiThread.join();
//
//				edit.putString(PREF_USER_NAME, userName);
				edit.commit();
				
				KeyValueBean loginPwd = getLoginPwd(activity);
				AccessTokenPair atp = new AccessTokenPair(loginPwd.getKey(),loginPwd.getValue());
				api.getSession().setAccessTokenPair(atp);
				
				return true;
		
				
				
	        } catch (Exception e) {
	            Log.i("DbAuthLog", "Error authenticating", e);
	            e.printStackTrace();
	        }
	        
	    }
	    return false;

	}

	public boolean isLinked(Activity activity) {
		try {

		AppKeyPair appKeys = new AppKeyPair(StaticConfig.dropboxAppKey, StaticConfig.dropboxAppSecret);
		AndroidAuthSession session = new AndroidAuthSession(appKeys, AccessType.DROPBOX);
		api = new DropboxAPI<AndroidAuthSession>(session);		
//		
		KeyValueBean loginPwd = getLoginPwd(activity);
		AccessTokenPair atp = new AccessTokenPair(loginPwd.getKey(),loginPwd.getValue());
		api.getSession().setAccessTokenPair(atp);
//		return api.getSession().isLinked();
		api.metadata("/", 1, null, false, null);
		return true;
		}
		catch (DropboxException e){
			return false;
		}
		

	}


	private KeyValueBean getLoginPwd(Activity activity){
        SharedPreferences prefs = activity.getSharedPreferences(
        	      "org.mrpdaemon.android.encdroidmc", activity.MODE_PRIVATE);
  		//boolean linked = prefs.getBoolean("linked",false);
  		String ak = prefs.getString("accessKey","");
  		String as = prefs.getString("accessSecret","");
  		return (new KeyValueBean(ak,as));
	}

	@Override
	protected String getUrlPrefix() {
		return "/";
	}

	@Override
	public int getID() {
		return 4;
	}
	
	@Override
	public String getFilesystemRootPath() {
		return "/";
	}
}
