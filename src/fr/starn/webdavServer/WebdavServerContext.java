package fr.starn.webdavServer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.net.ssl.SSLServerSocket;



public class WebdavServerContext {
	
	//network
	private String clientAdress;
	private String serverAdress;
	private int serverPort;
	boolean ssl;
	
	//data
	private InputStream inputFromClient;
	private OutputStream outputFromServer;
	
	
	//http
	private  Map<String,String> clientHttpParams;
	private String[] command;
	
	public WebdavServerContext(ServerSocket socket, BufferedInputStream input,OutputStream output){
		this.inputFromClient=input;
		this.outputFromServer=output;
		try {
			this.serverAdress=socket.getLocalSocketAddress().toString();
			this.serverPort=socket.getLocalPort();
			this.clientAdress=socket.getInetAddress().getHostName();
			if (socket instanceof SSLServerSocket) ssl= true;
			
			
			StringBuffer[] headerLines = new StringBuffer[50];
			int indexHeaderLines = 0;
			headerLines[indexHeaderLines]=new StringBuffer();
			char lastChar=0;
			for( int c = input.read(); c !=-1; c = input.read()){
				if (c!='\n' && c!='\r') headerLines[indexHeaderLines].append((char)c);
				if (lastChar == '\r' && c=='\n'){
					if ("".equals(headerLines[indexHeaderLines].toString())){
						//if there is an empty line in the header, it is the end of the header
						//the input stream's position is the start of data
						break;
					}
					//headerLines[indexHeaderLines].append(c);
					indexHeaderLines++;
					headerLines[indexHeaderLines]=new StringBuffer();
				}
					
				lastChar=(char)c;
			}
			
			String clientCommand = null;
			clientHttpParams = new HashMap<String, String>();
			for (int i = 0;i<indexHeaderLines;i++){
				String sCurrentLine=headerLines[i].toString();
				if (i==0) clientCommand=sCurrentLine;
				else {
					int separatorIndex=sCurrentLine.indexOf(':');
					String paramKey = sCurrentLine.substring(0,separatorIndex);
					String paramValue = sCurrentLine.substring(separatorIndex+2);
					clientHttpParams.put(paramKey, paramValue);

				}
			}
			
			/////////////////////////////////////////////////////////////////////////
//			String clientCommand = input.readLine();
//			clientHttpParams = new HashMap<String, String>();
//			
//	       //read from the stream
//			String sCurrentLine;
//			int browsedLine = 0;
//			while ((sCurrentLine = input.readLine()) != null && !"".equals(sCurrentLine)) {
//				//clientHttpRequest += sCurrentLine;
//				int separatorIndex=sCurrentLine.indexOf(':');
//				String paramKey = sCurrentLine.substring(0,separatorIndex);
//				String paramValue = sCurrentLine.substring(separatorIndex+2);
//				clientHttpParams.put(paramKey, paramValue);
//				//System.out.println("client params: "+paramKey+"=>"+paramValue);
//				browsedLine++;
//			}
			//////////////////////////////////////////////////////////////////////////
			
			
			
			//System.out.println("client asked: "+clientCommand);
			
		      //clientHttpRequest=clientHttpRequest.toUpperCase(); //convert it to uppercase
		      
		    if (clientCommand!=null){ 
		    	command = new String[10];
		    	StringTokenizer st = new StringTokenizer(clientCommand," ");
				int i = 0;
				while (i<10 && st.hasMoreTokens()){
					command[i]=XMLHelper.getStringFromURL( st.nextToken() );
					i++;
				}
		    }
		} catch (IOException e){
			throw new RuntimeException(e);
		}

      

		
		
	}
	
	public void close(){
		try {
			inputFromClient.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		try {
			outputFromServer.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	

	public String getClientAdress() {
		return clientAdress;
	}

	public void setClientAdress(String clientAdress) {
		this.clientAdress = clientAdress;
	}

	public String getServerAdress() {
		return serverAdress;
	}

	public void setServerAdress(String serverAdress) {
		this.serverAdress = serverAdress;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}



	public Map<String, String> getClientHttpParams() {
		return clientHttpParams;
	}






	public String[] getCommand() {
		return command;
	}



	
	
	public InputStream getInputFromClient() {
		return inputFromClient;
	}

	public void setInputFromClient(InputStream inputFromClient) {
		this.inputFromClient = inputFromClient;
	}

	public OutputStream getOutputFromServer() {
		return outputFromServer;
	}

	public void setOutputFromServer(OutputStream outputFromServer) {
		this.outputFromServer = outputFromServer;
	}

	public String getURL(){
		String result = "";
		if (ssl) result+="https://";
		else result +="http://";
		
		result+=clientHttpParams.get("Host");
		result+=":"+getServerPort();
		result+=command[1];
		return result;
	}
	
	public String displayParamsToString(){
		String result ="*** HEADER ***\n";
		for (String key : clientHttpParams.keySet()){
			result+=(key+"=>"+clientHttpParams.get(key)+"\n");
		}
		return result+"*** END HEADER\n";
	}
}
