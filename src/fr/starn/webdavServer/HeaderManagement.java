package fr.starn.webdavServer;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;



public class HeaderManagement {
	public static final String contentTypeHTML = "text/html";
	public static final String contentTypeFile = "text/plain";
	public static final String contentTypeXML = "text/xml; charset=\"utf-8\"";
	
	String httpCode;
	//private Map<String,String> properties;
	List<KeyValueBean> properties;
	boolean hearderAlreadySent;
	
	public HeaderManagement(int httpCode, String contentType){
		properties = new ArrayList<KeyValueBean>();
		
	    switch (httpCode) {
	      case 200:
	          this.httpCode= "200 OK";
	          break;
	      case 201:
	          this.httpCode= "201 Created";
	          break;
	      case 206:
	          this.httpCode= "206 Partial Content";
	          break;
	      case 301:
	    	  this.httpCode= "301 Moved Permanently";
            break;
	      case 400:
	    	  this.httpCode= "400 Bad Request";
	        break;
	      case 403:
	    	  this.httpCode= "403 Forbidden";
	        break;
	      case 404:
	    	  this.httpCode= "404 Not Found";
	        break;
	      case 416:
	    	  this.httpCode= "416 Requested Range not satisfiable";
	        break;
	      case 500:
	    	  this.httpCode= "500 Internal Server Error";
	        break;
	      case 501:
	    	  this.httpCode= "501 Not Implemented";
	        break;
	      case 207:
	    	  this.httpCode= "207 Multi-Status";
		      break;		        
	    }
	    
	    //System.out.println("build header: "+this.httpCode);
	    
	    SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss", Locale.ENGLISH);
	    addProperties("Date", df.format(new Date())+" GMT");
	    addProperties("Server", "EncdroidMC Server");
	    addProperties("Connection", "Keep-Alive");
	    addProperties("Last-Modified", df.format(new Date())+" GMT");
	    //addProperties("Vary", "Accept-Encoding");
	    //addProperties("Content-Length", "0");
	    //addProperties("Keep-Alive", "timeout=5, max=100");
	    //addProperties("Connection", "Keep-Alive");
	    addProperties("Content-Type", contentType);
	    
	}
	
	public void addProperties(String key, String value){
		if (hearderAlreadySent) throw new RuntimeException("Header already sent");
		properties.add(new KeyValueBean(key,value));
	}
	
	public  void construct_http_header( OutputStream output) {
			hearderAlreadySent = true;
		    String s = "HTTP/1.1 ";
		    //you probably have seen these if you have been surfing the web a while

		    s=s+httpCode+"\r\n";
		    //s = s + "\r\n"; //other header fields,
		    
		    for (KeyValueBean prop: properties){
		    	s = s + prop.getKey()+": "+prop.getValue()+"\r\n";
		    }
		    
		    
//		    s = s + "Date: Sun, 07 Apr 2013 10:43:18 GMT\r\n";
//		    s = s + "Server: Apache/2.2.22 (Debian)\r\n";
//		    s = s + "Location: http://localhost/disk/\r\n";
//		    s = s + "Vary: Accept-Encoding\r\n";
//		    s = s + "Content-Length: 0\r\n";
//		    s = s + "Keep-Alive: timeout=5, max=100\r\n";
//		    s = s + "Connection: Keep-Alive\r\n"; //we can't handle persistent connections
		    //	s = s + "Server: StarnHttpServer v0\r\n"; //server name

		    //Construct the right Content-Type for the header.
		    //This is so the browser knows what to do with the
		    //file, you may know the browser dosen't look on the file
		    //extension, it is the servers job to let the browser know
		    //what kind of file is being transmitted. You may have experienced
		    //if the server is miss configured it may result in
		    //pictures displayed as text!
//		    switch (file_type) {
//		      //plenty of types for you to fill in
//		      case 0:
//		        break;
//		      case 1:
//		        s = s + "Content-Type: image/jpeg\r\n";
//		        break;
//		      case 2:
//		        s = s + "Content-Type: image/gif\r\n";
//		      case 3:
//		        s = s + "Content-Type: application/x-zip-compressed\r\n";
//		      case 4:
//			        s = s + "Content-Type: application/xml\r\n";		        
//		      default:
//		        s = s + "Content-Type: text/html\r\n";
//		        break;
//		    }

		    ////so on and so on......
		    s = s + "\r\n"; //this marks the end of the httpheader
		    //and the start of the body
		    //ok return our newly created header!
		    //return s;
		    try {
//		    	System.out.println("*** header response**\n");
//		    	System.out.println(s);//
//		    	System.out.println("*** fin header **\n");
		    	output.write(s.getBytes());
		    } catch (Exception e){
		    	throw new RuntimeException(e);
		    }
		  }   
}
