package fr.starn.fileProviderModules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.mrpdaemon.android.encdroidmc.fileProvider.EncdroidProviderParameter;
import org.mrpdaemon.sec.encfs.EncFSFileInfo;

import ch.boye.httpclientandroidlib.impl.client.AbstractHttpClient;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import de.aflx.sardine.DavResource;
import de.aflx.sardine.Sardine;
import de.aflx.sardine.impl.SardineException;
import de.aflx.sardine.impl.SardineImpl;

public class FileProvider5 extends FileProvider7{
	
	public void initialize(String rootPath) throws Exception {
		// get a string like "http://myserver/myfolder/"
		connexionURL = "https://dav.box.com/dav/";
	}	
	
	protected Sardine getSardine(){
		if (sardine_==null){
	
			connexionURL = getURLWithoutSlash(connexionURL);
	
			AbstractHttpClient client = wrapClient(new DefaultHttpClient());
			sardine_ = new SardineImpl(client, this.getParamValues().get(loginKey),
					this.getParamValues().get(pwdKey));
		}
		return sardine_;
	}
	
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
		
		if (path.equals(".encfs6.xml")){
			path="Keyfile.bcx";
		}

		String result =urlStart+rootPath+path; 
		return result;
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
			if (childEnt.getName().startsWith("!IMPORTANT ")) {
				//ignore unencrypted avertissement file
				continue;
			}
			list.add(davResource2EncFSFileInfo(childEnt));
		}

		return list;

	}	
	
	public EncFSFileInfo davResource2EncFSFileInfo(DavResource dr) {

		String name = dr.getName();
		if (name.equals("Keyfile.bcx")){
			name=".encfs6.xml";
		}

		
		//search the next "/" after "http(s)://". 
		//then extract the serveur name like "http://server/" without the share/folders name
		int indexEndOfProtocol = this.getUrlPrefix().indexOf("://");
		int endOfServerURL=this.getUrlPrefix().indexOf('/', indexEndOfProtocol+3);
		String serverURL=this.getUrlPrefix().substring(0,endOfServerURL);
		String relativePath=getRelativePathFromAbsolutePath(serverURL+getParentPath(dr.getPath()));
		
		EncFSFileInfo effi = new EncFSFileInfo(name, relativePath,
				dr.isDirectory(), dr.getModified().getTime(), dr
						.getContentLength().longValue(), true, true, true);
		return effi;
	}	
	
	
	@Override
	public List<EncdroidProviderParameter> getParamsToAsk() {
		List<EncdroidProviderParameter> params = new ArrayList<EncdroidProviderParameter>();

		params.add(new EncdroidProviderParameter(loginKey, "Box.net login",
				"example: myusername"));
		params.add(new EncdroidProviderParameter(pwdKey, "Box.net password: ",
				"example: mypassword",true));
		return params;
	}
	

	@Override
	public String getProviderName() {
		return "Box.net";
	}
	
	@Override
	public int getID() {
		return 5;
	}
	
	@Override
	public String getFilesystemRootPath() {
		return "/";
	}
}
