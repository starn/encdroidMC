package org.slf4j;

public class LoggerFactory {
	public static Logger getLogger(String className){
		return new LoggerImpl();
	}
	
	public static Logger getLogger(Class classz){
		return new LoggerImpl();
	}

}
