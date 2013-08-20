package org.orioai.esupecm;

public class OriOaiMetadataType {

	private String id;
	private String label;
	
	
	
	
	public OriOaiMetadataType(String id, String label) {
		super();
		this.id = id;
		this.label = label;
	}
	
	
	
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}




	/**
	 * @return
	 * @see java.lang.String#toString()
	 */
	public String toString() {
		return id+" ... "+label;
	}
	
	
	
}
