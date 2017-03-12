package org.mrpdaemon.android.encdroidmc.fileProvider;

import java.io.OutputStream;

public interface UploadableWithOutputStream {
	public abstract OutputStream fsUpload(String path,  long length);
		
}
