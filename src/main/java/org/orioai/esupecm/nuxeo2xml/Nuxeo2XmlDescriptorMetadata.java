package org.orioai.esupecm.nuxeo2xml;


import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.RuntimeContext;

@XObject("metadata")
public class Nuxeo2XmlDescriptorMetadata {

    // this is set by the type service to the context that knows how to locate
    // the definitions
    public RuntimeContext context;
    
    public static final String TYPE_NUXEO = "NUXEO";
    public static final String TYPE_FILE = "FILE";
    public static final String TYPE_STATIC = "STATIC";
    
    public static final String CONDITION_EMPTY = "empty";
    
    @XNode(value = "@type")
    protected String type;
    
    @XNode(value = "@fromXpath")
    protected String fromXpath;
    
    @XNode(value = "@workflowXpathRoot")
    protected String workflowXpathRoot;

    @XNode(value = "@workflowXpath")
    protected String workflowXpath;

    @XNode(value = "@CDATA")
    protected boolean CDATA;
    
    @XNode(value = "@condition")
    protected String condition;
    
    @XNodeList(value = "translation", type = String[].class, componentType = Nuxeo2XmlDescriptorMetadataTranslation.class)
    protected Nuxeo2XmlDescriptorMetadataTranslation[] translations;

   

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * @return the CDATA
	 */
	public boolean isCDATA() {
		return CDATA;
	}
	
	/**
	 * @return the fromXpath
	 */
	public String getFromXpath() {
		return fromXpath;
	}

	/**
	 * @return the workflowXpath
	 */
	public String getWorkflowXpath() {
		return workflowXpath;
	}
	
	/**
	 * @return the workflowXpathRoot
	 */
	public String getWorkflowXpathRoot() {
		return workflowXpathRoot;
	}
	
	
    public String getCondition() {
		return condition;
	}


	public Nuxeo2XmlDescriptorMetadataTranslation[] getTranslations() {
		return translations;
	}
    

    
    
    public void setType(String type) {
		this.type = type;
	}

	public void setFromXpath(String fromXpath) {
		this.fromXpath = fromXpath;
	}

	public void setWorkflowXpath(String workflowXpath) {
		this.workflowXpath = workflowXpath;
	}
	
	public void setWorkflowXpathRoot(String workflowXpathRoot) {
		this.workflowXpathRoot = workflowXpathRoot;
	}

	public void setTranslations(
			Nuxeo2XmlDescriptorMetadataTranslation[] translations) {
		this.translations = translations;
	}
	
	public void setCDATA(boolean CDATA) {
		this.CDATA = CDATA;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}
	

	public String toString() {
    	
    	String traducesString = "";
		 for (int i=0; translations!=null && i<translations.length; i++) {
			 traducesString += translations[i];
			 if (i!=translations.length-1) {
				 traducesString += ", ";
			 }
		 }
    	
    	return "type="+type+", fromXpath="+fromXpath+", workflowXpathRoot="+workflowXpathRoot+", workflowXpath="+workflowXpath+", translations="+traducesString;
    }
    
    
}
