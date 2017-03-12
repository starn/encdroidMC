package fr.starn.fileProviderModules;



import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.util.ArrayList;
import java.util.List;

import org.mrpdaemon.android.encdroidmc.fileProvider.DownloadableWithOutputStream;
import org.mrpdaemon.android.encdroidmc.fileProvider.EncdroidFileProvider;
import org.mrpdaemon.android.encdroidmc.fileProvider.EncdroidProviderParameter;
import org.mrpdaemon.android.encdroidmc.fileProvider.IStatefullSession;
import org.mrpdaemon.sec.encfs.EncFSFileInfo;

import android.util.Log;
import de.aflx.sardine.util.Logger;


public class FileProvider2 extends EncdroidFileProvider implements DownloadableWithOutputStream, IStatefullSession {

	private static Logger log = new Logger();
	
	private final static String hostnameKey = "hostname";
	private final static String portKey = "port";
	private final static String loginKey = "login";
	private final static String pwdKey = "pwdKey";
	
	// Logger tag
	private final static String TAG = "EDFtpFileProvider";
	
	
	private String hostname ;
	int port;
	private static FTPClient ftpClient; 
	
	public FileProvider2() {
	}
	
	
	public void initialize(String rootPath) throws Exception{
	}
	
	private FTPClient getFtpClient(){
		try
		{
			if (ftpClient!=null){
				boolean isConnected = false;
				try {
					String testConnection = ftpClient.currentDirectory();
					isConnected=true;
				} catch (Exception e){
					isConnected=false;
				}
				if (isConnected) {
					return ftpClient;
				}

				
			}
				
			ftpClient = new FTPClient();

			
			//get a string like "192.168.1.10" or "myhostname" or "ftp://myhostname"...
			hostname = this.getParamValues().get(hostnameKey);
			String portStr = this.getParamValues().get(portKey);
			String login=this.getParamValues().get(loginKey);
			String pwd = this.getParamValues().get(pwdKey);
			
		
			
			if (portStr==null || "".equals(portStr)) portStr="21";
			try {
				port = Integer.parseInt(portStr);
			}catch (NumberFormatException e){
				throw new RuntimeException("Invalid port. Must be a number");
			}
			

			ftpClient.connect(hostname, port);
			ftpClient.login(login , pwd);
			ftpClient.setPassive(true);
			ftpClient.setType(FTPClient.TYPE_BINARY);

			return ftpClient;
				
				
			}
			catch (Exception e)
			{
				e.printStackTrace();
			    throw new RuntimeException(e);
			}				
			

	}





	
	public boolean copy(String srcPath, String dstPath) throws IOException {
		throw new RuntimeException("remote copy not implemented for ftp");
	}

	
	public EncFSFileInfo createFile(String path) throws IOException {
		OutputStream outputstream = openOutputStream(path,0);
		outputstream.close();
		try {
			//sleep to avoid a problem (i dont remember what pb)
			Thread.sleep(500);
		} catch (Exception e){
			throw new RuntimeException(e);
		}
		return getFileInfo(path);
	}

	
	public boolean delete(String path) throws IOException {
		try {
			EncFSFileInfo info = getFileInfo(path);
			if (info.isDirectory()){
				getFtpClient().deleteDirectory(getAbsolutePath(path));
			} else {
				getFtpClient().deleteFile(getAbsolutePath(path));
			}
			return true;
		} catch (Exception e){
			e.printStackTrace();
			return false;
		}
	}

	
	public boolean exists(String path) throws IOException {
		try {
			
			
			
			String fullPathToList = getParentPath( getAbsolutePath( path));
			if (fullPathToList.endsWith("/")) fullPathToList = fullPathToList.substring(0,fullPathToList.length()-1);
			
			String filenameToSearch = getAbsolutePath( path);
			if (filenameToSearch.endsWith("/")) filenameToSearch=filenameToSearch.substring(0,filenameToSearch.lastIndexOf('/'));
			filenameToSearch=filenameToSearch.substring(filenameToSearch.lastIndexOf("/")+1);
			
			
			
			FTPFile[] remoteFiles = getFtpClient().list(fullPathToList);
		

			
			for (FTPFile file : remoteFiles){
				System.out.println("compare "+file.getName()+" avec "+filenameToSearch);
				if (file.getName().equals(filenameToSearch)) return true;
			}
		} catch (Exception e){
			throw new RuntimeException(e);
		}
		return false;
	}
	


	
protected void finalize() throws Throwable {
System.out.println("destroy");

}
	
	public EncFSFileInfo getFileInfo(String path) throws IOException {
		try {
			
			String fullPathToList = getParentPath( getAbsolutePath( path));
			if (fullPathToList.endsWith("/")) fullPathToList = fullPathToList.substring(0,fullPathToList.length()-1);
			
			String filenameToSearch = getAbsolutePath( path);
			if (filenameToSearch.endsWith("/")) filenameToSearch=filenameToSearch.substring(0,filenameToSearch.lastIndexOf('/'));
			filenameToSearch=filenameToSearch.substring(filenameToSearch.lastIndexOf("/")+1);
			
			
			
			FTPFile[] remoteFiles = getFtpClient().list(fullPathToList);
		

			
			for (FTPFile file : remoteFiles){
				System.out.println("compare "+file.getName()+" avec "+filenameToSearch);
				if (file.getName().equals(filenameToSearch)) return ftpResource2EncFSFileInfo(getParentPath(path),file);
			}
		} catch (Exception e){
			throw new RuntimeException(e);
		}
		return null;
	}

	
	public boolean isDirectory(String path) throws IOException {
		EncFSFileInfo file = getFileInfo(path);
		return file.isDirectory();
	}

	
	private EncFSFileInfo ftpResource2EncFSFileInfo(String parentPath , FTPFile file){
		//String distantPath = parentPath;//+"/"+file.getName();
		String relativePath = parentPath;//getRelativePathFromAbsolutePath(distantPath);
		if ("".equals(relativePath)) relativePath="/";
		System.out.println("celle ci retourne:"+relativePath+" => "+file.getName());
		EncFSFileInfo effi = new EncFSFileInfo(file.getName(),relativePath,file.getType()==1?true:false,file.getModifiedDate().getTime(),file.getSize(),true,true,true);
		return effi;
	}
	

	

	
	protected String getURLWithoutSlash(String url) {
		if (url == null)
			return null;
		if (url.endsWith("/"))
			return url.substring(0, url.length() - 1);
		return url;
	}	
	
	private String getParentPath(String path){
		String result=path.substring(0,getURLWithoutSlash(path).lastIndexOf('/')+1);
		return result;
	}
	



	
	


	

	
	public boolean mkdir(String path) throws IOException {
		try {
			getFtpClient().createDirectory(getAbsolutePath(path));
			return true;
		} catch (Exception e){
			return false;
		}
	}

	
	public boolean mkdirs(String path) throws IOException {
		// XXX: Not implemented
		IOException ioe = new IOException("NOT IMPLEMENTED 7");
		Log.e(TAG, ioe.toString() + "\n" + Log.getStackTraceString(ioe));
		throw ioe;
	}

	
	public boolean move(String srcPath, String dstPath) throws IOException {
		try {
			getFtpClient().rename(getAbsolutePath(srcPath), getAbsolutePath(dstPath));
			return true;
		} catch (Exception e){
			return false;
		}
	}

	
	

	
	public void fsUpload(String path, PipedInputStream inputStream,
			long length) throws IOException {
		try{
			getFtpClient().upload(getAbsolutePath(path), inputStream, 0, 0, null);
		} catch (Exception e){
			e.printStackTrace();
			throw new RuntimeException(e);
		}			
	}

	






	@Override
	public String getProviderName() {
		return "FTP";
	}	
	
	@Override
	public List<EncdroidProviderParameter> getParamsToAsk(){
		List<EncdroidProviderParameter> params= new ArrayList<EncdroidProviderParameter>();
		params.add(new EncdroidProviderParameter(hostnameKey,"Ftp url: ","example: '192.168.1.10' or 'myFtpHostName'" ));
		params.add(new EncdroidProviderParameter(loginKey,"Ftp login: ","example: myusername"));
		params.add(new EncdroidProviderParameter(pwdKey,"Ftp password: ","example: mypassword" ,true));
		return params;
	}

	@Override
	public boolean allowRemoteCopy(){
		return false;
	}


	@Override
	public List<EncFSFileInfo> fsList(String path) throws IOException {
		List<EncFSFileInfo> list = new ArrayList<EncFSFileInfo>();
		
		try {
			if (path.endsWith("/")) path = path.substring(0,path.length()-1);
			
			//i dont know why: but with listFile encFS provide the absolute url
			//I DO NOT have to concatenate with rootpath....
			
			FTPFile[] remoteFiles = getFtpClient().list(getAbsolutePath(path));
		
			for (FTPFile childEnt : remoteFiles) {
				if (".".equals(childEnt.getName()) || "..".equals(childEnt.getName())) continue;
				list.add(ftpResource2EncFSFileInfo(path,childEnt));
			}
			
			return list;
		} catch (Exception e){
			throw new IOException(e);
		}
	}


	@Override
	public void fsDownload(String path, OutputStream outputStream,long startIndex) {
		try {
			getFtpClient().download(getAbsolutePath(path), outputStream, startIndex, null);
			outputStream.close();
		} catch (Exception e){
			throw new RuntimeException(e);
		}
		
	}


	@Override
	public InputStream fsDownload(String path,long startIndex) throws IOException {
		throw new RuntimeException("not implemented: use 'public void fsDownload(String path, OutputStream outputStream)' instead.");
	}


	@Override
	protected String getUrlPrefix() {
		return "/";
	}


	@Override
	public void clearSession() {
		try {
			ftpClient.disconnect(true);
		} catch (Exception e){
			e.printStackTrace();
		}
		ftpClient=null;
		
	}


@Override
public int getID() {
	return 2;
}
@Override
public String getFilesystemRootPath() {
	return "/";
}
	
}
