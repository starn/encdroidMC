package fr.starn.webdavServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.mrpdaemon.android.encdroidmc.MimeManagement;
import org.mrpdaemon.sec.encfs.EncFSFile;
import org.mrpdaemon.sec.encfs.EncFSFileInputStream;

import fr.starn.ThumbnailManager;
import android.provider.MediaStore.Files;

public class RequestHandler implements Runnable{
	WebdavServerContext ctx;
	Map<Integer,EncFSFile> srcFiles;
	
	static EncFSFile test;
	static byte[] fis13;
	
	public RequestHandler(WebdavServerContext ctx,Map<Integer,EncFSFile> srcFiles){
		this.ctx=ctx;
		this.srcFiles=srcFiles;
	}
	
	@Override
	public void run() {
		http_handler(ctx);
		
		
	}
	
	   private void http_handler(WebdavServerContext ctx) {
		   Map<String,String> clientParams;
		    try {
		    	
	    	    
			        if (ctx.getCommand()==null){
			      	  new HeaderManagement(501,HeaderManagement.contentTypeHTML).construct_http_header(ctx.getOutputFromServer());
			      	  return;
			        }
			        
					
					
					
					
					//extract path and get params
					String askedPath=ctx.getCommand()[1];
					StringTokenizer st = new StringTokenizer(ctx.getCommand()[1],"&?=");
					int nbToken = 0;
					String path = "";
					List<KeyValueBean> params = new ArrayList<KeyValueBean>();
					KeyValueBean current= null;
					while (st.hasMoreTokens()){
						if (nbToken==0) path=st.nextToken();
						if (nbToken%2==1)  {
							current = new KeyValueBean();
							current.setKey(st.nextToken());
						}
						if (nbToken!=0 && nbToken%2==0) {
							if (current!=null) {
								current.setValue(st.nextToken());
								params.add(current);
							}
						}
						nbToken++;
					}
					
					//System.out.println("chemin fourni:"+ctx.getCommand()[1]);
					if (path.equals("/")) {
						System.out.println("404 for URL: "+ctx.getCommand()[1]);
			        	new HeaderManagement(404,HeaderManagement.contentTypeHTML).construct_http_header(ctx.getOutputFromServer());
				          return;
					}
					
					
					//extract fileid and return the associated file
					EncFSFile srcFile;
					int fileID = -1;
					try {
						fileID = Integer.parseInt(path.substring(path.lastIndexOf("/")+1));
						srcFile = srcFiles.get(fileID);
					} catch (Exception e){
			        	new HeaderManagement(404,HeaderManagement.contentTypeHTML).construct_http_header(ctx.getOutputFromServer());
				          return;
					}
					if (srcFile==null){
			        	new HeaderManagement(404,HeaderManagement.contentTypeHTML).construct_http_header(ctx.getOutputFromServer());
				          return;
					}
			      
					System.out.println("file:"+srcFile.getName()+" , url="+ctx.getURL());
					
			      if (ctx.getCommand()[0].toUpperCase().startsWith("GET")) { //compare it is it GET
				      path = ctx.getCommand()[1]; //fill in the path
				      //path = URLDecoder.decode(path);
				      //System.out.println("path="+path);
			    	  
			    	  //send the file
				      
				      long startingByte = 0;
				      long fileLength=srcFile.getLength();
				      long endingByte = fileLength;
				      if (ctx.getClientHttpParams().get("Range")!=null){
				      //manage range param like: Range=bytes=2276625-
				    	  String rangeStr=ctx.getClientHttpParams().get("Range");
				    	  if (rangeStr.indexOf('=')!=-1){
				    		  String rangeValue=rangeStr.substring(rangeStr.indexOf('=')+1);
				    		  String[] values = rangeValue.split("-");
				    		  if (values.length>0){
				    			  try  {
				    				  startingByte=Long.parseLong(values[0]);
				    				  if (values.length>=2) endingByte=Long.parseLong(values[1]);
				    			  } catch (Exception e){
				    				  e.printStackTrace();
				    			  }
				    		  }
				    	  }
				    	  if (startingByte>endingByte) {
				    		  System.err.println("********* probleme:"+rangeStr);
					        	new HeaderManagement(416,HeaderManagement.contentTypeHTML).construct_http_header(ctx.getOutputFromServer());
						          return;
				    	  }
				      }
			    	 
			    	  
			    	  //System.out.println("range = "+startingByte+" to "+endingByte);
				      String mimeType = MimeManagement.getMimeType(srcFile.getName());
			    	  
				      HeaderManagement header=null;
				      if (ctx.getClientHttpParams().get("Range")!=null){
				    	  header = new HeaderManagement(206,mimeType);
				    	  header.addProperties("Content-Range", "bytes "+startingByte+"-"+endingByte+"/"+fileLength);
				      } else {
				    	  header = new HeaderManagement(200,mimeType);
				      }
		        	  header.addProperties("Keep-Alive", "timeout=5, max=100");
		        	  header.addProperties("Accept-Ranges", "bytes");
		        	  //header.addProperties("Content-Length", ""+(endingByte-startingByte+1));
		        	  //TODO voir si faut mettre le plus 1 ou pas
		        	  header.addProperties("Content-Length", ""+(endingByte-startingByte));
		        	  
		        	  header.construct_http_header(ctx.getOutputFromServer());
		        	  //System.out.println("send file ");
		        	  OutputStream output = ctx.getOutputFromServer();
		        	  
		        	  int blockSize = 512*1024;
		        	  byte buffer[]=new byte[blockSize];
		        	  int nbLecture;
		        	  long sizeSent  = 0;
		        	  InputStream fis = null;
		        	  try{
//		        		  //////////////////// TEST //////////////////////
//		        		  test = srcFile;
//		        		  
//		        		  InputStream fis1 = test.openInputStream(0);
//		        		  byte[] fis1B = ThumbnailManager.inputStreamToByteArray(fis1);
//		        		  Thread.sleep(2000);
//		        		  
//		        		  new Thread(new Runnable() {
//							
//							@Override
//							public void run() {
//								// TODO Auto-generated method stub
//								try {
//				        		  InputStream fis3 = test.openInputStream(0);
//				        		  fis13 = ThumbnailManager.inputStreamToByteArray(fis3);
//								} catch (Exception e){
//									e.printStackTrace();
//								}
//
//							}
//						}).start();
//		        		  new Thread(new Runnable() {
//								
//							@Override
//							public void run() {
//								// TODO Auto-generated method stub
//								try {
//				        		  InputStream fis2 = test.openInputStream(0);
//				        		  byte[] fis2B = ThumbnailManager.inputStreamToByteArray(fis2);
//								} catch (Exception e){
//									e.printStackTrace();
//								}
//
//							}
//						}).start();		        		  
//		        		  
//		        		  Thread.sleep(2000);
//		        		  System.out.println("********* verif ");
//		        		  if (fis1B.length !=fis13.length){
//		        			  System.out.println("*************"+fis1B.length+" VS "+fis13.length);
//		        		  } else {
//		        			  for (int i = 0 ; i < fis1B.length; i++){
//		        				  if (fis1B[i] != fis13[i]) {
//		        					  System.out.println("************* ça foire à "+i);
//		        					  break;
//		        				  }
//		        			  }
//		        			  System.out.println("********* fin verif");
//		        		  }
//		        		  
//		        		  
//		//					//////////////// TEST //////////////////////
		        		  
		        		  
		        		  
			        	  fis = srcFile.openInputStream(startingByte);//new EncFSFileInputStream(srcFile,startingByte);
			        	  if (params.size()>0) fis=new FileInputStream("/sdcard/7.jpg");
//			        	  System.out.println("skip to "+startingByte);
//			        	  fis.skip(startingByte);
			        	  System.out.println("***********************"+fis);
			        	  //if (buffer.length>((endingByte-nbLecture)))buffer = new byte[(int)(endingByte-nbLecture)];
			        	  long restingBytesToRead = (int)(endingByte-startingByte-sizeSent+1);
			        	  long startTime = new Date().getTime();
			        	  while( (nbLecture = fis.read(buffer,0,buffer.length))!= -1 ) {
			        		  if (restingBytesToRead<0) {
			        			  System.out.println("interruption prematuree a cause du range !!");
			        			  break;
			        		  }
			        		  try {
			        			  output.write(buffer, 0, nbLecture);
			        		  } catch (Exception se){
			        			  System.err.println("**=> "+se.getMessage()+"\n\n buffer.length="+buffer.length+" nbL="+nbLecture+" restingBytesToRead="+restingBytesToRead);
			        			  //se.printStackTrace();
			        			  return;
			        		  }
			        		  sizeSent+=nbLecture;
			        		  
			        		  long endTime = new Date().getTime();
			        		  //System.out.println(sizeSent/1000+"kb  "+((sizeSent)/(endTime-startTime))+"ko/s");
			        		  output.flush();
			        		  restingBytesToRead = (int)(endingByte-startingByte-sizeSent+1);
			        		  //Thread.sleep(1000);
			        	  } 
			        	  System.out.println("[ok] send file ");
		        	  } catch (Exception e){
		        		  System.out.println("[ko] send file ");
		        		  e.printStackTrace();
		        	  }	finally {
		        		  if (fis!=null) fis.close();
		        	  }
		        	  System.out.println(srcFile.getName()+" - "+fileID+" sent: "+sizeSent);
		        	  output.flush();
		        	  output.close();
			    	  
			      } //if we set it to method 1
			      
			      
			      
		
			      else { // not supported
			        try {
			        	System.out.println("command "+ctx.getCommand()[0]+" not implemented");
			        	new HeaderManagement(501,HeaderManagement.contentTypeHTML).construct_http_header(ctx.getOutputFromServer());
			          return;
			        }
			        catch (Exception e3) { //if some error happened catch it
			          System.err.println("error:" + e3.getMessage());
			          e3.printStackTrace();
			        } //and display error
			      }
		      
		    } catch (Exception e){
		    	e.printStackTrace();
		    }
		    
		    ctx.close();
		  }	

}
