package org.orioai.esupecm.workflow;

public class EditorInfo {

	private String title;
	private String url;
	public EditorInfo(String title, String url) {
		super();
		this.title = title;
		this.url = url;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	public String toString() {
		return title+" --> "+url;
	}
	
}
