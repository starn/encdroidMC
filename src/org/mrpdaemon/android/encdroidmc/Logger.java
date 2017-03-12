package org.mrpdaemon.android.encdroidmc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;

public class Logger {
	private static boolean enabled = false;
	
	public synchronized static void log(String str){
		if (!enabled) return;
		File encDroidDir = new File(
				Environment.getExternalStorageDirectory(),
				EDVolumeBrowserActivity.ENCDROID_SD_DIR_NAME);
		if (!encDroidDir.exists()) {
			encDroidDir.mkdir();
		}
		try{
			File logFile = new File(
					Environment.getExternalStorageDirectory(),
					EDVolumeBrowserActivity.ENCDROID_SD_DIR_NAME+"/log.txt");
			if (logFile.exists() && logFile.length()>10000){
				logFile.delete();
				logFile.createNewFile();
			}

			//FileOutputStream fos = new FileOutputStream(logFile);
			
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
			out.println((sdf.format(new Date())+" "+str));
			out.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		
		
	}
	
	public synchronized static void log(Throwable t){
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		log(sw.toString());
	}
	
}
