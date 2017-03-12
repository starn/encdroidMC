package fr.starn.fileProviderModules;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

import org.mrpdaemon.android.encdroidmc.fileProvider.EncdroidFileProvider;
import org.mrpdaemon.android.encdroidmc.fileProvider.EncdroidProviderParameter;
import org.mrpdaemon.android.encdroidmc.fileProvider.IStatefullSession;
import org.mrpdaemon.android.encdroidmc.fileProvider.UploadableWithOutputStream;
import org.mrpdaemon.sec.encfs.EncFSFileInfo;


public class FileProvider3 extends EncdroidFileProvider implements UploadableWithOutputStream, IStatefullSession{
	private final static String hostnameKey = "hostname";
	private final static String loginKey = "login";
	private final static String pwdKey = "pwdKey";
	
    private NtlmPasswordAuthentication authentication;
    private String serverAdress;
    
	@Override
	public String getProviderName() {
		return "Windows Share (Samba)";
	}
	

	
	public void initialize(String rootPath) throws Exception {
		System.out.println("debut init");
		
//		this.getParamValues().put("server", "smb://192.168.1.21/Documents");
//		this.getParamValues().put("login", "anonymous");
//		this.getParamValues().put("pwd", "");
		
		serverAdress=this.getParamValues().get(hostnameKey);
		if (!serverAdress.startsWith("smb://")) serverAdress="smb://"+serverAdress;
		if (!serverAdress.endsWith("/")) serverAdress+="/";
		String userName = this.getParamValues().get(loginKey);
		String pwd = this.getParamValues().get(pwdKey);
		if (userName==null || "".equals(userName)){
			userName="guest";
			pwd="";
		}
		authentication = new NtlmPasswordAuthentication("",userName, pwd); // domain, user, password
		System.out.println("fin init");
	}

	



	@Override
	public boolean isDirectory(String srcPath) throws IOException {
		System.out.println("*** isDirectory "+getAbsolutePath(srcPath)+" => "+getAbsolutePath(srcPath));
		SmbFile currentFolder = new SmbFile(getAbsolutePath(srcPath), authentication);
		return currentFolder.isDirectory();
	}



	@Override
	public boolean exists(String srcPath) throws IOException {
		try {
			System.out.println("*** exists "+srcPath+" => "+getAbsolutePath(srcPath));
			SmbFile currentFolder = new SmbFile(getAbsolutePath(srcPath), authentication);
			return currentFolder.exists();
		} catch (Throwable t){
			t.printStackTrace();
			throw new IOException(t.getMessage());
		}
	}



	@Override
	public EncFSFileInfo getFileInfo(String srcPath) throws IOException {
		System.out.println("*** getFileInfo "+srcPath+" => "+getAbsolutePath(srcPath));
		SmbFile currentFolder = new SmbFile(getAbsolutePath(srcPath), authentication);
		return SmbFile2EncFSFileInfo(currentFolder);
	}



	@Override
	public boolean move(String srcPath, String dstPath) throws IOException {
		SmbFile currentFolder = new SmbFile(getAbsolutePath(srcPath), authentication);
		SmbFile targetFolder = new SmbFile(getAbsolutePath(dstPath), authentication);
		try {
			currentFolder.renameTo(targetFolder);
			return true;
		} catch (Exception e){
			return false;
		}
	}



	@Override
	public boolean delete(String srcPath) throws IOException {
		if (isDirectory(srcPath) && !srcPath.endsWith("/")){
			srcPath+="/";
		}
		SmbFile currentFolder = new SmbFile(getAbsolutePath(srcPath), authentication);

		try {
			currentFolder.delete();
			return true;
		} catch (Exception e){
			e.printStackTrace();
			return false;
		}
	}



	@Override
	public boolean mkdir(String dirPath) throws IOException {
		System.out.println("*** mkdir "+dirPath+" => "+getAbsolutePath(dirPath));
		SmbFile currentFolder = new SmbFile(getAbsolutePath(dirPath), authentication);
		try {
			 currentFolder.mkdir();
			return true;
		} catch (Exception e){
			return false;
		}		
	}



	@Override
	public boolean mkdirs(String dirPath) throws IOException {
		System.out.println("*** mkdirs "+dirPath+" => "+getAbsolutePath(dirPath));
		SmbFile currentFolder = new SmbFile(getAbsolutePath(dirPath), authentication);
		try {
			 currentFolder.mkdirs();
			return true;
		} catch (Exception e){
			return false;
		}	
	}



	@Override
	public EncFSFileInfo createFile(String dstFilePath) throws IOException {
		System.out.println("create file "+dstFilePath);
		SmbFile currentFolder = new SmbFile(getAbsolutePath(dstFilePath), authentication);
		currentFolder.createNewFile();
		return getFileInfo(dstFilePath);
	}



	@Override
	public boolean copy(String srcFilePath, String dstFilePath)
			throws IOException {
		SmbFile currentFolder = new SmbFile(getAbsolutePath(srcFilePath), authentication);
		SmbFile targetFolder = new SmbFile(getAbsolutePath(dstFilePath), authentication);
		try {
			currentFolder.copyTo(targetFolder);
			return true;
		} catch (Exception e){
			return false;
		}
	}



	@Override
	public List<EncdroidProviderParameter> getParamsToAsk() {
		List<EncdroidProviderParameter> params= new ArrayList<EncdroidProviderParameter>();
		params.add(new EncdroidProviderParameter(hostnameKey,"host ip: ","example: '192.168.1.10/myShare/' (Empty to browse the network)" ));
		params.add(new EncdroidProviderParameter(loginKey,"login: ","example: myusername. Empty for anonymous guest access"));
		params.add(new EncdroidProviderParameter(pwdKey,"password: ","example: mypassword. Empty for anonymous guest access",true ));
		return params;

	}

	
	///////////////
	// IMPORTANT
	///////////////////
	//retourner le parent sans le slash et le fichier sans le slash !!
	//du coup on travaille uniquement, et partout avec le chemin absolu, beaucoup plus simple
	//////////////////
	private EncFSFileInfo SmbFile2EncFSFileInfo(SmbFile smbFile){
		EncFSFileInfo result;
		try {
			String name = smbFile.getName();
			
			//transform parent absolute path into path relative to server path 
			String relativePath=this.getRelativePathFromAbsolutePath(smbFile.getParent());
			
			
			if (name.endsWith("/")) name=name.substring(0,name.length()-1);
			result = new EncFSFileInfo(name,relativePath,smbFile.isDirectory(),smbFile.getLastModified(),smbFile.length(),true,true,true);
			
		} catch (Exception e){
			throw new RuntimeException(e);
		}
		
		return result;
	}




	@Override
	public List<EncFSFileInfo> fsList(String path) throws IOException {
		System.out.println("*** fsList "+path+" => "+getAbsolutePath(path));
		String pathToList = getAbsolutePath(path);
		if (!pathToList.endsWith("/")) pathToList+="/";
		SmbFile currentFolder = new SmbFile(pathToList, authentication);
		SmbFile[] files = currentFolder.listFiles();
        List<EncFSFileInfo> fList = new LinkedList<EncFSFileInfo>();
        for(int a = 0; a < files.length; a++)
        {
        	EncFSFileInfo encFSFileInfo = SmbFile2EncFSFileInfo(files[a]);
            fList.add(encFSFileInfo);
        }
 
        return fList;
	}
	




	@Override
	public void fsUpload(String path, PipedInputStream inputStream, long length)
			throws IOException {

		throw new RuntimeException("this function use 'public OutputStream fsUpload(String path, long length)'");
			
		
	}



	@Override
	public InputStream fsDownload(String path, long startIndex) throws IOException {
		SmbFile currentFolder = new SmbFile(getAbsolutePath(path), authentication);
		//InputStream is = currentFolder.getInputStream();
		BufferedInputStream is = new BufferedInputStream(new SmbFileInputStream(currentFolder));
		if (startIndex>0) is.skip(startIndex);
		return is;
	}






	@Override
	public OutputStream fsUpload(String path, long length)  {
		try {
			SmbFile currentFolder = new SmbFile(getAbsolutePath(path), authentication);
			return currentFolder.getOutputStream();
		} catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}



	@Override
	protected String getUrlPrefix() {
		return serverAdress;
	}



	@Override
	public void clearSession() {
		authentication = null;
		serverAdress=this.getParamValues().get(hostnameKey);
		if (!serverAdress.startsWith("smb://")) serverAdress="smb://"+serverAdress;
		if (!serverAdress.endsWith("/")) serverAdress+="/";
		String userName = this.getParamValues().get(loginKey);
		String pwd = this.getParamValues().get(pwdKey);
		if (userName==null || "".equals(userName)){
			userName="guest";
			pwd="";
		}		
		authentication = new NtlmPasswordAuthentication("",userName, pwd); // domain, user, password
		
	}

	@Override
	public int getID() {
		return 3;
	}
	
	@Override
	public String getFilesystemRootPath() {
		return "/";
	}
}
