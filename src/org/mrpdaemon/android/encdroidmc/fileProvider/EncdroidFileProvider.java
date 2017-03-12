package org.mrpdaemon.android.encdroidmc.fileProvider;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mrpdaemon.sec.encfs.EncFSFileInfo;
import org.mrpdaemon.sec.encfs.EncFSFileProvider;

import fr.starn.fileProviderModules.FileProvider0;

public abstract class EncdroidFileProvider implements EncFSFileProvider,Serializable {
    public static List<EncdroidFileProvider> providerListCache;
	public static final String providerPackage = "fr.starn.fileProviderModules.";
	public static final String providerClassStartingName = "FileProvider";
	
	//to access file provider after volume creation or import (not very clean...)
	public static EncdroidFileProvider lastCreatedProvider = null;
	
	private Map<String,String> paramValues;
	private boolean ready;
	protected String rootPath;
	
	private transient static Map<String,List<EncFSFileInfo>> listFilesCache;
	
	
	public abstract List<EncdroidProviderParameter> getParamsToAsk();

	
	public abstract void initialize(String path) throws Exception;
	public abstract List<EncFSFileInfo> fsList(String path)  throws IOException;
	public abstract void fsUpload(String path,PipedInputStream inputStream, long length)  throws IOException;
	public abstract InputStream fsDownload(String path, long startIndex)  throws IOException;
	
	//disable multiple network operations, it make crash the program
	private static boolean working;
	
	/** 
	 * empty constructor
	 */
	public EncdroidFileProvider(){

		System.out.println("contructor EncdroidFileProvider()");
		listFilesCache = new HashMap<String,List<EncFSFileInfo>>();
	}
	

	


	public void changeRootPath(String rootPath){
		if (rootPath==null || "".equals(rootPath)) rootPath="/";
		this.rootPath=rootPath;
	}
	
	public final void init(String rootPath){
		this.rootPath=rootPath;
		try {
			this.initialize(rootPath);
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * if:
	 *  the server is 192.168.1.10
	 *  the container path (rootPath) is is /myencryptedFolder/
	 *  the localPath is /pictures/hollidays
	 *  => return   192.168.1.10/myencryptedFolder/pictures/hollidays/
	 */
	public String getAbsolutePath(String path){
		String urlStart = this.getUrlPrefix();
		if (!urlStart.endsWith("/")) urlStart+="/";
		if ("".equals(urlStart)) urlStart="/";

		if (!rootPath.endsWith("/")) rootPath+="/";
		if (rootPath.startsWith("/")) rootPath=rootPath.substring(1);
		if ("/".equals(rootPath)) rootPath="";
		
		//String suffix = ("/"+path).replaceAll("//", "/");
		if (path.startsWith("/")) path=path.substring(1);
		//if (!path.endsWith("/")) path+="/";
		if ("/".equals(path)) path="";
		
		
		
		String result =urlStart+rootPath+path; 
		return result;
	}
	
	
	/**
	 * return a path relative to the absolute path
	 */
	protected String getRelativePathFromAbsolutePath(String absolutePath){
		String serverCurrentPath=getAbsolutePath("/");//= URLPrefix + rootPath
		
		//if we try to go upper than the root folder, return the root folder
		if (absolutePath.length()<serverCurrentPath.length()) return "/";
		
		String relativePath = "/";
		
		if (absolutePath.startsWith(serverCurrentPath)){
			relativePath = absolutePath.substring(serverCurrentPath.length()-1);//server url length -1 to keep the "/"
		} else {
			relativePath=rootPath;
		}
		if ("".equals(relativePath)) relativePath="/";
		return relativePath;
	}
	

	public void clearSession(){
		if (this instanceof IStatefullSession){
			((IStatefullSession)this).clearSession();
		}
		listFilesCache.clear();
	}
	
	public final InputStream openInputStream(String path, long startIndex) throws IOException{
		clearSession();
		
		//clear the provider to avoid some error when downloading the music

		
		if (this instanceof DownloadableWithOutputStream){
			try {
				PipedOutputStream pipeTarget;
				pipeTarget = new PipedOutputStream();
				PipedInputStream pipeToWrite = new PipedInputStream(pipeTarget);
				Thread thread = new DownloadStreamThread(pipeTarget,path,startIndex) ;
				thread.start();	
				
				
				return pipeToWrite;	
			} catch (Exception e){
				throw new RuntimeException(e);
			}
		}else {
			return this.fsDownload(path,startIndex);
		}
	}
	

	
	private class DownloadStreamThread extends Thread{
		PipedOutputStream outputStream;
		String path;
		long startIndex;
		
		public DownloadStreamThread(PipedOutputStream outputStream,String mypath, long startIndex){
			this.path = mypath;
			this.outputStream=outputStream;
			this.startIndex=startIndex;
		}
		
		public void run() {
			try{
				((DownloadableWithOutputStream)(EncdroidFileProvider.this)).fsDownload(path,outputStream,startIndex);
				outputStream.close();
			} catch (Exception e){
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		
	}		
	
	public final OutputStream openOutputStream(String path, long length){
		if ("/".equals(path))
			path = "";

		if (this instanceof UploadableWithOutputStream){
			return ((UploadableWithOutputStream)this).fsUpload(path, length);
		}
		else {
			try {
				PipedInputStream pipeTarget;
		
				// Pipe's output end that this class writes to
				PipedOutputStream pipeToWrite;
				pipeTarget = new PipedInputStream();
				pipeToWrite = new PipedOutputStream(pipeTarget);
		
				Thread thread = new StreamThread(pipeTarget, path, length);
				thread.start();
				return pipeToWrite;
			} catch (Exception e){
				System.out.println("Error on provider: "+this.getProviderName()+" on path "+path);
				throw new RuntimeException(e);
			}
		}
	}
	
	private class StreamThread extends Thread {
		PipedInputStream stream;
		String path;
		long length;

		public StreamThread(PipedInputStream mystream, String mypath, long length) {
			path = mypath;
			stream = mystream;
			this.length = length;
		}

		public void run() {
			try {

				working=true;
				fsUpload(path,stream,length);
				working=false;

			} catch (Exception e) {
				System.out.println("error when writing to path "+this.path);
				throw new RuntimeException(e);
			} finally{
				System.out.println("***************** fin thread Stream");
			}
		}

	}	
	
	public static EncdroidFileProvider getProvider(int id, String serializedParams,String rootPath){
		Class providerClass = null;
		try {
			providerClass = Class.forName(providerPackage+providerClassStartingName+id);
		} catch (ClassNotFoundException e){
			throw new RuntimeException(e);
		}
		
		EncdroidFileProvider instance = null;
		try {
				Constructor c = providerClass.getConstructor();
				instance = (EncdroidFileProvider)c.newInstance();
			} catch (Exception e){
				throw new RuntimeException(e);
			}
		instance.paramValues=unserializeParams(serializedParams);
		instance.ready=true;
		instance.init(rootPath);
		return instance;
		
	}
	

	
	public final  List<EncFSFileInfo> listFiles(String path) throws IOException{
		System.out.println("***listFiles:"+path);
		waitConnexionAvailability();
		List<EncFSFileInfo> result = this.fsList(path);
		String key = this.getClass().getName()+this.serializeParams()+path;
		listFilesCache.put(key, result);
		return result;
	}
	
	public final List<EncFSFileInfo> listFiles(String path,boolean useCache) throws IOException{
		if (!useCache) return listFiles(path);
		String key = this.getClass().getName()+this.serializeParams()+path;
		List<EncFSFileInfo> cacheValue = listFilesCache.get(key);
		if (cacheValue!=null ) return cacheValue;
		return this.listFiles(path);
	}
	
	

	
	public static EncdroidFileProvider getProviderById(int id){
		for (EncdroidFileProvider provider:getAvailableProviders()){
			if (provider.getID()==id) {
				//provider.clearSession();
				return provider;
			}
		}
		return null;
		
	}
	
	public static EncdroidFileProvider getProviderByIndex(int index){
		return getAvailableProviders().get(index);
	}

	
	
	public static EncdroidFileProvider getLocalFileSystemProvider(){
		FileProvider0 local = new FileProvider0();
		local.init("/");
		local.setReady(true);
		return local;
	}
	
	public static EncdroidFileProvider getExternalFileSystemProvider(){
		EncdroidFileProvider local = getProviderById(7);
		local.init("/");
		local.setReady(true);
		return local;
	}
	
	
	public static List<EncdroidFileProvider> getAvailableProviders(){
		if (providerListCache!=null) return providerListCache;
		//To avoid proguard to remove these class when generating apk
		if (0==1){
			try {
				Class.forName("org.mrpdaemon.android.encdroidmc.fileProvider.Provider0");
				Class.forName("org.mrpdaemon.android.encdroidmc.fileProvider.Provider1");
				Class.forName("org.mrpdaemon.android.encdroidmc.fileProvider.Provider2");
				Class.forName("org.mrpdaemon.android.encdroidmc.fileProvider.Provider3");
				Class.forName("org.mrpdaemon.android.encdroidmc.fileProvider.Provider4");
				Class.forName("org.mrpdaemon.android.encdroidmc.fileProvider.Provider5");
				Class.forName("org.mrpdaemon.android.encdroidmc.fileProvider.Provider6");
				Class.forName("org.mrpdaemon.android.encdroidmc.fileProvider.Provider7");
				Class.forName("org.mrpdaemon.android.encdroidmc.fileProvider.Provider8");
				Class.forName("org.mrpdaemon.android.encdroidmc.fileProvider.Provider9");
				Class.forName("org.mrpdaemon.android.encdroidmc.fileProvider.Provider10");
				Class.forName("org.mrpdaemon.android.encdroidmc.fileProvider.Provider11");
				Class.forName("org.mrpdaemon.android.encdroidmc.fileProvider.Provider12");
				Class.forName("org.mrpdaemon.android.encdroidmc.fileProvider.Provider13");
				Class.forName("org.mrpdaemon.android.encdroidmc.fileProvider.Provider14");
				Class.forName("com.jcraft.jsch.UserAuthNone");
			} catch (Exception e){
				e.printStackTrace();
			}
		}	
		
		
		List<EncdroidFileProvider> result = new ArrayList<EncdroidFileProvider>();
		//int i = 0;
		
		EncdroidFileProvider provider = null;
		for (int i = 0;i<15;i++){
			
			try {
				Class providerClass = null;
				providerClass = Class.forName(providerPackage+providerClassStartingName+i);
				Constructor c = providerClass.getConstructor();
				provider = (EncdroidFileProvider)c.newInstance();
				result.add(provider);
			} catch (ClassNotFoundException e){
				//if the class is not found, do not add the class in the list of available providers
			}
			catch (Exception e){
				throw new RuntimeException(e);
			}
			//i++;
		} 
		providerListCache=result;
		return result;
	}
	
	

	
	public Map<String, String> getParamValues() {
		if (paramValues==null) paramValues= new HashMap<String, String>();
		return paramValues;
	}
	public void setParamValues(Map<String, String> paramValues) {
		this.paramValues = paramValues;
	}
	
	

	

	
	public boolean isReady(){
		return ready;
	}
	
	public void setReady(boolean r){
		ready=r;
	}
	
	/**
	 * export params like : "key1|value1|key2|value2|key3|value3|"
	 * @return
	 */
	public String serializeParams(){
		StringBuffer result = new StringBuffer();
		Map<String,String> mapToPersist = getParamValues();
		Iterator<String> it = mapToPersist.keySet().iterator();
		while (it.hasNext()){
			String key = it.next();
			String value = mapToPersist.get(key);
			result.append( key).append("|").append( value).append("|");
		}
		
		
		return result.toString();
	}
	
	/**
	 * return a map<String,String> from params like : "key1|value1|key2|value2|key3|value3|"
	 * @return
	 */	
	public static Map<String,String> unserializeParams(String serializedParams){
		Map<String,String> result = new HashMap<String,String>();
		if (serializedParams==null || "".equals(serializedParams)) return result;
//		StringTokenizer st = new  StringTokenizer(serializedParams,"|");
//		while (st.hasMoreTokens()){
//			String key = st.nextToken();
//			if (key==null || "".equals(key)) break;
//			String value = st.nextToken();
//			result.put(key, value);
//		}
		String[] tokens = serializedParams.split("\\|");
		for (int i =0;i<tokens.length;i+=2){
			String key =tokens[i];
			String value = "";
			if (i+1<tokens.length) value=tokens[i+1];
			result.put(key, value);
		}
		
		return result;
		
	}
	
	
	public boolean allowRemoteCopy(){
		return true;
	}
	
	protected void waitConnexionAvailability(){
		do {
			sleep(200);
		}while (working);
	}
	
	protected void sleep(long duration) {
		try {
			Thread.sleep(duration);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	

	
	protected abstract String getUrlPrefix();
	
	

}
