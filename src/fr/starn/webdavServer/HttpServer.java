package fr.starn.webdavServer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.mrpdaemon.android.encdroidmc.MimeManagement;
import org.mrpdaemon.sec.encfs.EncFSFile;
import org.mrpdaemon.sec.encfs.EncFSFileInputStream;



public class HttpServer extends Thread{
	private Map<Integer,EncFSFile> srcFile;
	private static HttpServer instance;

	//prepare an httpserver that only serve the file in parameter (to allow streaming a file)
	private HttpServer(){
		this.start();
	}
	
	public static HttpServer  getInstance(){
		if (instance==null) instance = new HttpServer();
		return instance;
	}
	
	public String setFile(EncFSFile srcFile){
		//System.out.println("share file "+srcFile.getName());
		int random = (int)Math.round(Math.random()*10000);
		//this.srcFile=srcFile;
		if (this.srcFile==null) this.srcFile =new HashMap<Integer, EncFSFile>();
		if (this.srcFile.size()>100) this.srcFile.clear();
		this.srcFile.put(random,srcFile);
		return ""+random;
	}
	
	
	
   public void run() {
	  ServerSocket s;
      try {
    	  s = new ServerSocket(8080);
      } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException(e);
       }
      
         while (true){
        	 try {
		         Socket c =  s.accept();
		         BufferedInputStream input = new BufferedInputStream(c.getInputStream());
		         
		         WebdavServerContext ctx = new WebdavServerContext(s,input,c.getOutputStream());
		         


		     //as the name suggest this method handles the http request, see further down.
		     //abstraction rules
		             //http_handler(context);
		         new Thread(new RequestHandler(ctx,srcFile)).start();
		             
             } catch (Exception e) {
                 e.printStackTrace();
             }
         }

   }
   
  


   
   

}