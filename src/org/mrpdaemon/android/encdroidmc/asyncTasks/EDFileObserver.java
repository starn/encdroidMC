package org.mrpdaemon.android.encdroidmc.asyncTasks;

import java.io.File;

public class EDFileObserver  {


	File f;
	//private long size;
	private long lastModif;

	public EDFileObserver(String path) {
		f = new File(path);
		lastModif= f.lastModified();
		//size=f.length();
	}

	public boolean wasModified() {
		long lastModifAfter = f.lastModified();
		long sizeAfter=f.length();
		//if (lastModifAfter!=lastModif || size!=sizeAfter) return true;
		if (lastModifAfter!=lastModif ) return true;
		return false;
	}


	public String getPath(){
		return f.getAbsolutePath();
	}
	

}
