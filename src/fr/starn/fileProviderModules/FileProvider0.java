/*
 * encdroid - EncFS client application for Android
 * Copyright (C) 2012  Mark R. Pariente
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.starn.fileProviderModules;



import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.util.LinkedList;
import java.util.List;

import org.mrpdaemon.android.encdroidmc.fileProvider.EncdroidFileProvider;
import org.mrpdaemon.android.encdroidmc.fileProvider.EncdroidProviderParameter;
import org.mrpdaemon.android.encdroidmc.fileProvider.UploadableWithOutputStream;
import org.mrpdaemon.sec.encfs.EncFSFileInfo;
import org.mrpdaemon.sec.encfs.EncFSLocalFileProvider;

import android.os.Environment;

/**
 * 
 * @author starn
 * this class is the local file browser
 * DO NOT RENAME THE ID 0 OF THIS PROVIDER:
 * because a reference is hard coded in EDVolumeBrowserActivity.onOptionsItemSelected
 * to import a local file into the volume 
 */
public class FileProvider0 extends EncdroidFileProvider implements UploadableWithOutputStream  {

	private EncFSLocalFileProvider encFSLocalFileProvider;
	
	
	public FileProvider0(){}
	
	public void initialize(String rootPath){
		encFSLocalFileProvider=new EncFSLocalFileProvider(new File(rootPath));
	}
	
	@Override
	public String getProviderName() {
		return "Local";
	}
	@Override
	public List<EncdroidProviderParameter> getParamsToAsk() {
//		List<EncdroidProviderParameter> params= new ArrayList<EncdroidProviderParameter>();
//		params.add(new EncdroidProviderParameter("test","test: ","test" ));
//		return params;
		return null;
	}


	
	
	
	
	
	@Override
	public boolean copy(String s, String s1) throws IOException {
		return encFSLocalFileProvider.copy(getAbsolutePath(s), getAbsolutePath(s1));
	}

	@Override
	public EncFSFileInfo createFile(String s) throws IOException {
		EncFSFileInfo f = encFSLocalFileProvider.createFile(getAbsolutePath(s));
		return getFileInfo(s);
	}

	@Override
	public boolean delete(String s) throws IOException {
		return encFSLocalFileProvider.delete(getAbsolutePath(s));
	}

	@Override
	public boolean exists(String s) throws IOException {
		return encFSLocalFileProvider.exists(getAbsolutePath(s));
	}

	@Override
	public EncFSFileInfo getFileInfo(String s) throws IOException {
		EncFSFileInfo file = encFSLocalFileProvider.getFileInfo(getAbsolutePath(s));
		String parentPath = file.getParentPath();
		if (!parentPath.startsWith("/")) parentPath="/"+parentPath;
		parentPath=getRelativePathFromAbsolutePath( parentPath);
		EncFSFileInfo newEntry = new EncFSFileInfo(file.getName(),parentPath,file.isDirectory(),file.getLastModified(),file.getSize(),file.isReadable(),file.isWritable(),file.isExecutable());
		return newEntry;
	}



	@Override
	public boolean isDirectory(String s) throws IOException {
		return encFSLocalFileProvider.isDirectory(getAbsolutePath(s));
	}

	@Override
	public List<EncFSFileInfo> fsList(String s) throws IOException {
		List<EncFSFileInfo> files = encFSLocalFileProvider.listFiles(getAbsolutePath(s));
		
		List<EncFSFileInfo> result = new LinkedList<EncFSFileInfo>();
		for (EncFSFileInfo file: files){
			String parentPath = file.getParentPath();
			if (!parentPath.startsWith("/")) parentPath="/"+parentPath;
			parentPath=getRelativePathFromAbsolutePath( parentPath);
			EncFSFileInfo newEntry = new EncFSFileInfo(file.getName(),parentPath,file.isDirectory(),file.getLastModified(),file.getSize(),file.isReadable(),file.isWritable(),file.isExecutable());
			result.add(newEntry);
		}
		return result;
	}

	@Override
	public boolean mkdir(String s) throws IOException {
		return encFSLocalFileProvider.mkdir(getAbsolutePath(s));
	}

	@Override
	public boolean mkdirs(String s) throws IOException {
		return encFSLocalFileProvider.mkdirs(getAbsolutePath(s));
	}

	@Override
	public boolean move(String s, String s1) throws IOException {
		return encFSLocalFileProvider.move(getAbsolutePath(s),getAbsolutePath(s1));
	}




	@Override
	public OutputStream fsUpload(String path, long length) {
		try {
			return encFSLocalFileProvider.openOutputStream(getAbsolutePath(path),length);
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}

	@Override
	public void fsUpload(String path, PipedInputStream inputStream, long length)
			throws IOException {
		throw new RuntimeException("method not implemented: use 'public OutputStream fsUpload(String path, long length)' instead");
		
	}

	@Override
	public InputStream fsDownload(String path, long startIndex) throws IOException {
		return encFSLocalFileProvider.openInputStream(getAbsolutePath(path),startIndex);
	}
	
	

	@Override
	public String getUrlPrefix() {
		return Environment.getExternalStorageDirectory().getAbsolutePath();
	}

	@Override
	public int getID() {
		return 0;
	}

	@Override
	public String getFilesystemRootPath() {
		return "/";
	}
	
}
