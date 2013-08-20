package org.orioai.esupecm.nuxeo2xml;


import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.RuntimeContext;

@XObject("translation")
public class Nuxeo2XmlDescriptorMetadataTranslation {

    // this is set by the type service to the context that knows how to locate
    // the definitions
    public RuntimeContext context;
    
    
    @XNode(value = "@from")
    protected String from;
    
    @XNode(value = "@to")
    protected String to;

    
   
    
	
    public String getFrom() {
		return from;
	}


	public String getTo() {
		return to;
	}




	public void setFrom(String from) {
		this.from = from;
	}


	public void setTo(String to) {
		this.to = to;
	}

	

	public String toString() {
    	return "from="+from+", to="+to;
    }
    
    
}
