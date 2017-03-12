package org.slf4j;

public class LoggerImpl implements Logger{

	@Override
	public void info(String str) {
		System.out.println(str);
		
	}

	@Override
	public void error(String str) {
		System.out.println(str);
		
	}

	@Override
	public void debug(String str) {
		System.out.println(str);
		
	}
	public void info(String s,String... params){
		if (level<2) return;
		System.out.println(s);
		for (String p:params){
			System.out.println(p);
		}
	}
	
	
	public void error(String s,Object t){
		if (level<1) return;
		System.out.println(s);
		System.out.println(t.toString());

		if (t instanceof Throwable){
			((Throwable) t).printStackTrace();
		}
		
	}	
	

	
	public void error(String s,String... params){
		if (level<1) return;
		System.out.println(s);
		for (String p:params){
			System.out.println(p);
		}
	}
	
	public void trace(String s,Object... params){
		if (level<2) return;
		System.out.println(s);
		for (Object p:params){
			System.out.println(p.toString());
		}
	}
	
	public void debug(String s,Object... params){
		if (level<2) return;
		System.out.println(s);
		for (Object p:params){
			if (p!=null){
				System.out.println(p.toString());
			}
		}
	}	
	
	public void info(String s,Object... params){
		if (level<2) return;
		System.out.println(s);
		for (Object p:params){
			System.out.println(p.toString());
		}
	}	
	
	public void warn(String s,Object... params){
		if (level<2) return;
		System.out.println(s);
		for (Object p:params){
			System.out.println(p.toString());
		}
	}	
}
