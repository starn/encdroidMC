package org.mrpdaemon.android.encdroidmc.fileProvider;

import java.io.OutputStream;

public interface DownloadableWithOutputStream {
	public void fsDownload(String path,OutputStream outputStream, long startIndex);
}
