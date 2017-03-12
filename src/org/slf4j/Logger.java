package org.slf4j;

//import org.danbrough.mega.APIError;

public interface Logger {
	//0 no logs
	//1 errors
	//2 warn/info/trace
	public static int level = 1;
	
	
	public abstract void info(String str);
	public abstract void error(String str);
	public abstract void debug(String str);
	
	//public void info();
	
	
	public void error(String s,Object t);
	

	
	public void error(String s,String... params);
	
	public void trace(String s,Object... params);
	
	public void debug(String s,Object... params);
	
	public void info(String s,Object... params);
	
	public void warn(String s,Object... params);
	
}
