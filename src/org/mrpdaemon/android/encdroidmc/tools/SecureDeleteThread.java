package org.mrpdaemon.android.encdroidmc.tools;

import java.io.*;
import java.security.SecureRandom;

public class SecureDeleteThread extends Thread {
	private File file;
	
	public SecureDeleteThread(File file){
		this.file=file;
	}
	
	@Override
	public void run() {
		try {
			if (file.exists()) {
				long length = file.length();
				SecureRandom random = new SecureRandom();
				RandomAccessFile raf = new RandomAccessFile(file, "rws");
				raf.seek(0);
				raf.getFilePointer();
				byte[] data = new byte[512];
				int pos = 0;
				while (pos < length) {
					random.nextBytes(data);
					raf.write(data);
					pos += data.length;
				}
				raf.close();
				file.delete();
			}
		} catch (IOException e){
			e.printStackTrace();
		}
		file.delete();
	}
}
