package org.mrpdaemon.android.encdroidmc.fileProvider;

public class EncdroidProviderParameter {
	private String key;
	private String label;
	private String title;
	private boolean isPassword;
	
	
	
	public EncdroidProviderParameter(String key, String title,String label) {
		super();
		this.key = key;
		this.title = title;
		this.label=label;
		this.isPassword=false;
	}
	
	public EncdroidProviderParameter(String key, String title,String label,boolean isPassword) {
		super();
		this.key = key;
		this.title = title;
		this.label=label;
		this.isPassword=isPassword;
	}
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	public boolean isPassword() {
		return isPassword;
	}

	public void setPassword(boolean isPassword) {
		this.isPassword = isPassword;
	}
	
	
}
