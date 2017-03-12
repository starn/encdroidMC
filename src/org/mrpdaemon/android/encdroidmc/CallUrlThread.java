package org.mrpdaemon.android.encdroidmc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class CallUrlThread extends Thread{
	private static long maxSizeForSynchronousDownload = 500000;
	
	private String url;
	
	public static boolean finished;
	public static long size;
	public static InputStream is;
	public static String name;
	public static String error = null;
	
	public CallUrlThread(String url){
		this.url=url;
		this.start();
	}
	
	@Override
	public void run() {
		super.run();
		
		try {
			
			//get name
			name = url.substring(url.lastIndexOf('/')+1);
			
			URL obj = new URL(url);
			
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			
			//get body input Stream
			is = con.getInputStream();
			
			//get size
			Map<String, List<String>> responseHeader=  con.getHeaderFields();
			size = -1;
			try {
				String contentType = responseHeader.get("Content-Type").get(0);
				if (contentType != null && contentType.startsWith("text/html") && !(name.endsWith("html") || name.endsWith("htm"))){
					name+=".html";
				}
				if ("image/jpeg".equals(contentType) && !(name.endsWith("jpg") || name.endsWith("jpeg"))){
					name+=".jpg";
				}
				if ("application/pdf".equals(contentType) && !(name.endsWith("pdf"))){
					name+=".pdf";
				}
				String sizeStr = responseHeader.get("Content-Length").get(0);
				size=Long.parseLong(sizeStr);
				finished=true;
			} catch (Exception e){
				e.printStackTrace();
			}
			//we cannot get size in header, we have to browse all the stream :(
			if (size==-1){
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				size = 0;
				int nbRead = 0;
				char[] buffer = new char[100];
				while ((nbRead = in.read(buffer)) >0 && size<maxSizeForSynchronousDownload) {
					size+=nbRead;
				}
				if (maxSizeForSynchronousDownload==size) {
					error = "No Content-Length and too big for synchronous download"; 
				}
				//create a new stream because we are in the end of the previously created
				 con = (HttpURLConnection) obj.openConnection();
				 is = con.getInputStream();
			}
			
	
		} catch (Exception e){
			error=e.getMessage();
			e.printStackTrace();
			finished=true;
		}
		
		finished=true;
	}
}
