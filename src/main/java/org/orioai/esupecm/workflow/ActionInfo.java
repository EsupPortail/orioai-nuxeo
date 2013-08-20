package org.orioai.esupecm.workflow;

public class ActionInfo {
	
	private String id;
	private String name;
	private String label;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	
	public ActionInfo(String id, String name) {
		super();
		this.id = id;
		this.name = name;
	}
	
	public ActionInfo(String id, String name, String label) {
		super();
		this.id = id;
		this.name = name;
		this.label = label;
	}
}
