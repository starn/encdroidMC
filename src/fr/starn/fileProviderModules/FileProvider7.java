package fr.starn.fileProviderModules;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.mrpdaemon.android.encdroidmc.fileProvider.EncdroidFileProvider;
import org.mrpdaemon.android.encdroidmc.fileProvider.EncdroidProviderParameter;
import org.mrpdaemon.android.encdroidmc.fileProvider.IStatefullSession;
import org.mrpdaemon.sec.encfs.EncFSFileInfo;

import android.util.Log;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.conn.ClientConnectionManager;
import ch.boye.httpclientandroidlib.conn.scheme.Scheme;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeRegistry;
import ch.boye.httpclientandroidlib.conn.ssl.SSLSocketFactory;
import ch.boye.httpclientandroidlib.impl.client.AbstractHttpClient;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import de.aflx.sardine.DavResource;
import de.aflx.sardine.Sardine;
import de.aflx.sardine.impl.SardineException;
import de.aflx.sardine.impl.SardineImpl;

public class FileProvider7 extends EncdroidFileProvider implements IStatefullSession {


	protected final static String hostnameKey = "hostname";
	protected final static String loginKey = "login";
	protected final static String pwdKey = "pwdKey";

	// Logger tag
	private final static String TAG = "EDWebdavFileProvider";
	protected transient Sardine sardine_;

	//private String webdavServer;
	// private String webdavRootFolder;
	String connexionURL = "";
	
	public FileProvider7() {


	}
	
	protected Sardine getSardine(){
		if (sardine_==null){
	
			// transform into http://myserver/myfolder
			connexionURL=connexionURL.replaceAll(" ", "%20");
			connexionURL = getURLWithoutSlash(connexionURL);
			// webdavServer is a string like "http://myserver"
			//webdavServer = connexionURL.substring(0, connexionURL.lastIndexOf("/"));
			// webdavRootFolder is a string like "/myfolder/"
	
	
	
	
			AbstractHttpClient client = wrapClient(new DefaultHttpClient());
			sardine_ = new SardineImpl(client, this.getParamValues().get(loginKey),
					this.getParamValues().get(pwdKey));
		}
		return sardine_;
	}

	public void initialize(String rootPath) throws Exception {
		// get a string like "http://myserver/myfolder/"
		connexionURL = this.getParamValues().get(hostnameKey);

		if (!connexionURL.startsWith("http://") && !connexionURL.startsWith("https://")) {
			throw new RuntimeException("hostname must start with 'http://' or 'https://'");
		}
	}

	public boolean copy(String srcPath, String dstPath) throws IOException {
		System.out.println("copy " + srcPath + " => " + dstPath);
		try {

			getSardine().copy(getAbsolutePath(srcPath), getAbsolutePath(dstPath));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public EncFSFileInfo createFile(String path) throws IOException {
		System.out.println("createfile " + getAbsolutePath(path));
		getSardine().put(getAbsolutePath(path), new byte[0]);
		return this.getFileInfo(path);
	}

	public boolean delete(String path) throws IOException {
		System.out.println("delete " + getAbsolutePath(path));
		try {
			if ("/".equals(path))
				path = "";
			getSardine().delete(getAbsolutePath(path));
			return true;
		} catch (IOException e) {
			try {
				// try as a directory path
				getSardine().delete(getAbsolutePath(path) + "/");
				return true;
			} catch (IOException e2) {
				return false;
			}
		}
	}

	public boolean exists(String path) throws IOException {
		System.out.println("exist:" + getAbsolutePath(path));
		boolean exists = getSardine().exists(getAbsolutePath(path));
		if (!exists)
			exists = getSardine().exists(getAbsolutePath(path) + "/");
		return exists;
	}

	public EncFSFileInfo getFileInfo(String path) throws IOException {
		
		System.out.println("getFileInfo:" + path);

		// a strange bug with apache http server: if there is no slash in the
		// end
		// for a folder, it does not list it.
		// but to have a file property, it must not have a slash
		// as we cannot know if it is a folder or not, we have to try both
		// I start to test with a slash (we more often use propfind for listing
		// files)
		// 1) first try with the path as folder (with slash at the end)
		List<DavResource> resources = null;
		try {
			resources = getSardine().list(getAbsolutePath(path));
		} catch (SardineException e) {
			e.printStackTrace();
		}
		if ((resources == null || resources.size() == 0) && !path.endsWith("/")) {
			if (path.endsWith("/"))
				path = path.substring(0, path.length() - 1);
			resources = getSardine().list(getAbsolutePath(path) + "/");
		}
		if (resources == null || resources.size() == 0)
			return null;
		return davResource2EncFSFileInfo(resources.get(0));
	}

	public boolean isDirectory(String path) throws IOException {
		System.out.println("isDirectory:" + path);
		EncFSFileInfo file = getFileInfo(path);
		return file.isDirectory();
	}

	public EncFSFileInfo davResource2EncFSFileInfo(DavResource dr) {

		String name = dr.getName();
		
		//search the next "/" after "http(s)://". 
		//then extract the serveur name like "http://server/" without the share/folders name
		int indexEndOfProtocol = this.getUrlPrefix().indexOf("://");
		int endOfServerURL=this.getUrlPrefix().indexOf('/', indexEndOfProtocol+3);
		if (endOfServerURL==-1) endOfServerURL=indexEndOfProtocol;
		String serverURL=this.getUrlPrefix().substring(0,endOfServerURL);
		String relativePath=getRelativePathFromAbsolutePath(serverURL+getParentPath(dr.getPath()));
		
		EncFSFileInfo effi = new EncFSFileInfo(name, relativePath,
				dr.isDirectory(), dr.getModified().getTime(), dr
						.getContentLength().longValue(), true, true, true);
		return effi;
	}


	
	protected String getParentPath(String path) {
		// boolean isFolder = path.endsWith("/");
		path = path.substring(0, getURLWithoutSlash(path).lastIndexOf('/') + 1);
		// if (isFolder) path+="/";
		return path;
	}
	


	private String getURLWithSlash(String url) {
		if (url == null)
			return null;
		if (url.endsWith("/"))
			return url;
		return url + "/";
	}

	protected String getURLWithoutSlash(String url) {
		if (url == null)
			return null;
		if (url.endsWith("/"))
			return url.substring(0, url.length() - 1);
		return url;
	}

	public List<EncFSFileInfo> fsList(String path) throws IOException {
		List<EncFSFileInfo> list = new ArrayList<EncFSFileInfo>();

		// the first propfind, encdroid ask for path=/ without root path.
		// so i set the default webdav path

		List<DavResource> resources = null;
		try {
			System.out.println("*** list: "+getAbsolutePath(path));
			resources = getSardine().list(getAbsolutePath(path));
		} catch (SardineException e) {
			e.printStackTrace();
		}
		if ((resources == null || resources.size() == 0)) {
			if (path.endsWith("/"))
				path = path.substring(0, path.length() - 1);
			resources = getSardine().list(getAbsolutePath(path) + "/");
		}

		// Add entries to list
		// ignore the first result because it is the parent folder
		boolean firstResult = true;
		for (DavResource childEnt : resources) {
			if (firstResult) {
				firstResult = false;
				continue;// i don t know why but the first result is the parent
							// folder. i ignore it
			}
			list.add(davResource2EncFSFileInfo(childEnt));
		}

		return list;

	}



	public boolean mkdir(String path) throws IOException {
		System.out.println("mkdir:" + getURLWithSlash(getAbsolutePath(path)));
		try {
			getSardine().createDirectory(getURLWithSlash(getAbsolutePath(path)));
		} catch (IOException e) {
			// throw new RuntimeException(e);
			return false;
		}

		return true;
	}



	public boolean mkdirs(String path) throws IOException {
		// XXX: Not implemented
		IOException ioe = new IOException("NOT IMPLEMENTED");
		Log.e(TAG, ioe.toString() + "\n" + Log.getStackTraceString(ioe));
		throw ioe;
	}

	public boolean move(String srcPath, String dstPath) throws IOException {
		try {
			getSardine().move(getAbsolutePath(srcPath), getAbsolutePath(dstPath));
			return true;
		} catch (IOException e) {
			return false;
		}
	}



	@Override
	public InputStream fsDownload(String path, long startIndex) throws IOException {
		System.out.println("******** get "+getURLWithoutSlash(getAbsolutePath(path)));
		InputStream is =  getSardine().get(getURLWithoutSlash(getAbsolutePath(path)));
		if (startIndex>0) is.skip(startIndex);
		return is;
	}




	public static AbstractHttpClient wrapClient(HttpClient base) {
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			X509TrustManager tm = new X509AlwaysTrust();

			ctx.init(null, new TrustManager[] { tm }, null);
			SSLSocketFactory ssf = new ch.boye.httpclientandroidlib.conn.ssl.SSLSocketFactory(
					ctx);
			ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			ClientConnectionManager ccm = base.getConnectionManager();
			SchemeRegistry sr = ccm.getSchemeRegistry();
			sr.register(new Scheme("https", ssf, 443));
			return new DefaultHttpClient(ccm, base.getParams());
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	@Override
	public String getProviderName() {
		return "Webdav";
	}

	@Override
	public List<EncdroidProviderParameter> getParamsToAsk() {
		List<EncdroidProviderParameter> params = new ArrayList<EncdroidProviderParameter>();
		params.add(new EncdroidProviderParameter(hostnameKey, "Webdav url: ",
				"example: https://myserver/path/"));
		params.add(new EncdroidProviderParameter(loginKey, "Webdav login: ",
				"example: myusername"));
		params.add(new EncdroidProviderParameter(pwdKey, "Webdav password: ",
				"example: mypassword",true));
		return params;
	}

	@Override
	public void fsUpload(String path, PipedInputStream inputStream,
			long length) throws IOException {
		// exist method is call to avoid this bug:
		// http://code.google.com/p/sardine/issues/detail?id=103
		// do not remove it!
		boolean exists = getSardine().exists(getAbsolutePath(path));
		// upload the file
		getSardine().put(getAbsolutePath(path), inputStream, length);
	}

	@Override
	protected String getUrlPrefix() {
//		String webdavRootFolder = connexionURL.substring(connexionURL.lastIndexOf("/"));
//		if (webdavRootFolder.endsWith("/"))
//			webdavRootFolder = webdavRootFolder.substring(0,
//					webdavRootFolder.length() - 1);
//		return webdavRootFolder;
		return connexionURL;
	}

	@Override
	public void clearSession() {
		sardine_=null;
		
	}

	@Override
	public int getID() {
		return 1;
	}

	
	@Override
	public String getFilesystemRootPath() {
		return "/";
	}
	



	
	
	
	
}
