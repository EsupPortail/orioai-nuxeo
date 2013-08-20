package org.orioai.esupecm.nuxeo2xml;


import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.RuntimeContext;

@XObject("configuration")
public class NuxeoDocumentUrlDescriptor {

    // this is set by the type service to the context that knows how to locate
    // the definitions
    public RuntimeContext context;
    
    @XNode("documentUrl")
    protected String documentUrl;
    
    
    private String clearedUrl = null;
    
    
    
	/**
	 * @return the documentUrl
	 */
	public String getDocumentUrl() {
		
		if (clearedUrl==null) {
			clearedUrl = documentUrl.replaceFirst(":80/", "/");
		}
		
		return clearedUrl;
	}


    
	 public String toString() {
		 return documentUrl;
	 }
    
}
