package fr.starn.fileProviderModules;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.mrpdaemon.android.encdroidmc.fileProvider.*;
import org.mrpdaemon.sec.encfs.EncFSFileInfo;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;


public class FileProvider6 extends EncdroidFileProvider implements UploadableWithOutputStream, IStatefullSession{
	private final static String hostnameKey = "hostname";
	private final static String loginKey = "login";
	private final static String pwdKey = "pwdKey";
	
    private String serverAdressFull;
    
    String userName;
    String pwd;
    
    private static transient  Session sshSession;
    private static transient  ChannelSftp sftp;
    
	@Override
	public String getProviderName() {
		return "SSH / SFTP";
	}
	
	public ChannelSftp getNewSession(){
	 	ChannelSftp newSftp = null;
	 	Session newSshSession = null;
    	JSch jsch = new JSch();
    	try {
    		String serverAdress=serverAdressFull;
    		int port = 22;
    		if (serverAdressFull.contains(":")){
    			int indexOfdots = serverAdressFull.indexOf(":");
    			port = Integer.parseInt(serverAdressFull.substring(indexOfdots+1));
    			serverAdress=serverAdressFull.substring(0,indexOfdots);
    			
    		}
    		newSshSession = jsch.getSession(userName, serverAdress, port);
    		newSshSession.setPassword(pwd.getBytes(Charset.forName("ISO-8859-1")));
    		Properties config = new java.util.Properties();
    		config.put("StrictHostKeyChecking", "no");
    		newSshSession.setConfig(config);
    		newSshSession.connect();
    		
    		// Initializing a channel
    		Channel  channel = newSshSession.openChannel("sftp");
    		channel.connect();
    		newSftp = (ChannelSftp) channel;    		
    	} catch (JSchException e){
    		throw new RuntimeException(e);
    	}
    	return newSftp; 
	}
	
	 public ChannelSftp getSession(){
	    	if ( sshSession!=null && sshSession.isConnected() && sftp!=null && sftp.isConnected()) return sftp;
	    	JSch jsch = new JSch();
	    	try {
	    		String serverAdress=serverAdressFull;
	    		int port = 22;
	    		if (serverAdressFull.contains(":")){
	    			int indexOfdots = serverAdressFull.indexOf(":");
	    			port = Integer.parseInt(serverAdressFull.substring(indexOfdots+1));
	    			serverAdress=serverAdressFull.substring(0,indexOfdots);
	    			
	    		}
	    		sshSession = jsch.getSession(userName, serverAdress, port);
	    		sshSession.setPassword(pwd.getBytes(Charset.forName("ISO-8859-1")));
	    		Properties config = new java.util.Properties();
	    		config.put("StrictHostKeyChecking", "no");
	    		sshSession.setConfig(config);
	    		sshSession.connect();
	    		
	    		// Initializing a channel
	    		Channel  channel = sshSession.openChannel("sftp");
	    		channel.connect();
	    		sftp = (ChannelSftp) channel;    		
	    	} catch (JSchException e){
	    		throw new RuntimeException(e);
	    	}
	    	return sftp; 
	 }


	public void initialize(String rootPath) throws Exception {
		System.out.println("debut init");
		
		
		serverAdressFull=this.getParamValues().get(hostnameKey);
		
		userName = this.getParamValues().get(loginKey);
		pwd = this.getParamValues().get(pwdKey);
		System.out.println("fin init");
	}

	



	@Override
	public boolean isDirectory(String srcPath) throws IOException {
		EncFSFileInfo f = getFileInfo(srcPath);
		return f.isDirectory();
	}



	@Override
	public boolean exists(String srcPath) throws IOException {
		Vector<LsEntry> ls = null;
		try {
			ls = getSession().ls(getAbsolutePath(srcPath));
		} catch (SftpException e){
			return false;
		}
		return (ls.size()>0) ;
	}



	@Override
	public EncFSFileInfo getFileInfo(String srcPath) throws IOException {
		if ("/".equals(getAbsolutePath(srcPath))) {
			//return the root file info
			return new EncFSFileInfo("/","/",true,0,0,true,true,true);
		}
		
		Vector<LsEntry> ls = null;
		try {
			String parentPath = getParentPath(getAbsolutePath(srcPath));
			ls = getSession().ls(parentPath);
		} catch (SftpException e){
			throw new RuntimeException(e);
		}
		for (LsEntry lsEntry: ls){
			if (lsEntry.getFilename().equals(".") || lsEntry.getFilename().equals("..")) {
				continue;
			}
			
			String searchedFullPath = getAbsolutePath(srcPath);
			if (searchedFullPath.endsWith("/")) searchedFullPath=searchedFullPath.substring(0,searchedFullPath.length()-1);
			if (searchedFullPath.endsWith("/"+lsEntry.getFilename())) return LsEntry2EncFSFileInfo(getParentPath(getAbsolutePath(srcPath)),lsEntry);
		}
		return null;
		
	}



	@Override
	public boolean move(String srcPath, String dstPath) throws IOException {
		try {
			getSession().rename(getAbsolutePath(srcPath),getAbsolutePath(dstPath));
			return true;
		} catch (SftpException e){
			e.printStackTrace();
			return false;
		}
	}



	@Override
	public boolean delete(String srcPath) throws IOException {
		System.out.println("delete "+getAbsolutePath(srcPath));
		try {
			EncFSFileInfo file = getFileInfo(srcPath);
			if (!file.isDirectory()){
				getSession().rm(getAbsolutePath(srcPath));
				return true;
			}
			else {
				getSession().rmdir(getAbsolutePath(srcPath));
				return true;
			}
			
		} catch (SftpException e){
			e.printStackTrace();
			return false;
		}
	}



	@Override
	public boolean mkdir(String dirPath) throws IOException {
		try {
			getSession().mkdir(getAbsolutePath(dirPath));
			return true;
		} catch (SftpException e){
			e.printStackTrace();
			return false;
		}		
	}



	@Override
	public boolean mkdirs(String dirPath) throws IOException {
		try {
			getSession().mkdir(getAbsolutePath(dirPath));
			return true;
		} catch (SftpException e){
			e.printStackTrace();
			return false;
		}		
	}



	@Override
	public EncFSFileInfo createFile(String dstFilePath) throws IOException {
		try {
			getSession().put(getAbsolutePath(dstFilePath));
			return getFileInfo(dstFilePath);
		} catch (SftpException e){
			e.printStackTrace();
			return null;
		}
	}



	@Override
	public boolean copy(String srcFilePath, String dstFilePath)
			throws IOException {
		throw new RuntimeException("copy not implemented with sftp");
	}
	
	@Override
	public boolean allowRemoteCopy() {
		return false;
	}



	@Override
	public List<EncdroidProviderParameter> getParamsToAsk() {
		List<EncdroidProviderParameter> params= new ArrayList<EncdroidProviderParameter>();
		params.add(new EncdroidProviderParameter(hostnameKey,"host ip: ","example: '192.168.1.10' or '192.168.1.10:22'" ));
		params.add(new EncdroidProviderParameter(loginKey,"login: ","example: myusername."));
		params.add(new EncdroidProviderParameter(pwdKey,"password: ","example: mypassword.",true ));
		return params;

	}

	
	///////////////
	// IMPORTANT
	///////////////////
	//retourner le parent sans le slash et le fichier sans le slash !!
	//du coup on travaille uniquement, et partout avec le chemin absolu, beaucoup plus simple
	//////////////////
	private EncFSFileInfo LsEntry2EncFSFileInfo(String parentPath, LsEntry file){
		EncFSFileInfo result;
		try {
			String name = file.getFilename();

			SftpATTRS attrs = file.getAttrs();
            long mtime = attrs.getMTime();		
            boolean isDir = attrs.isDir();
            long length = attrs.getSize();
			String relativePath=this.getRelativePathFromAbsolutePath(parentPath);
            
			
			
			if (name.endsWith("/")) name=name.substring(0,name.length()-1);
			result = new EncFSFileInfo(name,relativePath,isDir,mtime,length,true,true,true);
			
		} catch (Exception e){
			throw new RuntimeException(e);
		}
		
		return result;
	}

	protected String getParentPath(String path) {
		path = path.substring(0, getURLWithoutSlash(path).lastIndexOf('/') + 1);
		if ("".equals(path)) path="/";
		return path;
	}
	
	protected String getURLWithoutSlash(String url) {
		if (url == null)
			return null;
		if (url.endsWith("/"))
			return url.substring(0, url.length() - 1);
		return url;
	}	


	@Override
	public List<EncFSFileInfo> fsList(String path) throws IOException {
		System.out.println("*** fsList "+path+" => "+getAbsolutePath(path));
		String pathToList = getAbsolutePath(path);
		Vector<LsEntry> files = null;
		try {
			files = getSession().ls(pathToList);
		} catch (SftpException e){
			System.err.println(e.getMessage());
			return new LinkedList<EncFSFileInfo>(); 
		} catch (RuntimeException e){
			System.err.println(e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
        List<EncFSFileInfo> fList = new LinkedList<EncFSFileInfo>();
        for (LsEntry lsEntry: files){
        	if (lsEntry.getFilename().equals(".") || lsEntry.getFilename().equals("..")) continue;
        	fList.add(LsEntry2EncFSFileInfo(pathToList,lsEntry));
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
		System.out.println("download file "+path);
		try {
			BufferedInputStream is = new BufferedInputStream(getNewSession().get(getAbsolutePath(path)));
			if (startIndex>0) is.skip(startIndex);
			return is;
		} catch (SftpException e){
			e.printStackTrace();
			return null;
		}
	}






	@Override
	public OutputStream fsUpload(String path, long length)  {
		try {
			return getNewSession().put(getAbsolutePath(path));
		} catch (SftpException e){
			e.printStackTrace();
			return null;
		}
	}



	@Override
	protected String getUrlPrefix() {
		return "";
	}


	@Override
	public void clearSession() {
		
//		try {
//			if (sshSession!=null)
//				sshSession.disconnect();
//			if (sftp!=null)
//				sftp.disconnect();
//		} catch (Exception e){
//			e.printStackTrace();
//		}
//		sshSession=null;
//		sftp=null;
		
	}

	@Override
	public int getID() {
		return 6;
	}
	
	@Override
	public String getFilesystemRootPath() {
		return "/";
	}
}
