package org.mrpdaemon.android.encdroidmc;

import java.util.HashMap;
import java.util.Map;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;

public class MimeManagement {
	static Map<String, String> videosMimes = new HashMap<String, String>();
	static Map<String, String> audioMimes = new HashMap<String, String>();
	static Map<String, String> imageMimes = new HashMap<String, String>();
	static Map<String, String> textMimes = new HashMap<String, String>();
	
	static {
		textMimes.put("txt", "text/plain");
		textMimes.put("log", "text/plain");
		textMimes.put("xml", "text/plain");
		
		
		videosMimes.put("mpg","video/mpeg");
		videosMimes.put("mpeg","video/mpeg");
		videosMimes.put("avi","video/avi");
		videosMimes.put("flv","video/x-flv");
		videosMimes.put("mkv","video/x-matroska");
		videosMimes.put("3gp","video/3gpp");
		videosMimes.put("ogg","video/ogg");
		videosMimes.put("mp4","video/mp4");
		videosMimes.put("wmv","video/x-ms-wmv");
		
		
		audioMimes.put("wav","audio/x-pn-wav");
		audioMimes.put("mp3","audio/mp3");
		audioMimes.put("aac","audio/aac");
		
		imageMimes.put("jpg","image/jpeg");
		imageMimes.put("jpeg","image/jpeg");
		imageMimes.put("gif","image/gif");
		imageMimes.put("png","image/png");
	}
	
	public static String  getMimeType(String filename){
		String ext = getExtension(filename).toLowerCase();
		
		String mimeType = MimeTypeMap.getSingleton()
				.getMimeTypeFromExtension(ext);
		if (ext!=null) return mimeType;
		
		if (videosMimes.get(ext)!=null) return videosMimes.get(ext);
		if (audioMimes.get(ext)!=null) return audioMimes.get(ext);
		if (imageMimes.get(ext)!=null) return imageMimes.get(ext);
		return null;
	}
	
	public static boolean isVideo(String filename){
		String ext = getExtension(filename).toLowerCase();
		if (videosMimes.containsKey(ext)) return true;
		return false;
	}
	
	public static boolean isAudio(String filename){
		String ext = getExtension(filename).toLowerCase();
		if (audioMimes.containsKey(ext)) return true;
		return false;
	}
	
	public static boolean isImage(String filename){
		String ext = getExtension(filename).toLowerCase();
		if (imageMimes.containsKey(ext)) return true;
		return false;
	}	
	
	public static boolean isText(String filename){
		String ext = getExtension(filename).toLowerCase();
		if (textMimes.containsKey(ext)) return true;
		return false;
	}	
	
	public static String getExtension(String filename){
		String extension = MimeTypeMap.getFileExtensionFromUrl(filename);
		if (TextUtils.isEmpty(extension)) {
			/*
			 * getFileExtensionFromUrl doesn't work for files with
			 * spaces
			 */
			int dotIndex = filename.lastIndexOf('.');
			if (dotIndex >= 0) {
				extension = filename.substring(dotIndex + 1);
			}
		}
		return extension;
	}
}
