package org.orioai.esupecm.nuxeo2xml;


import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.RuntimeContext;

@XObject("ns")
public class Nuxeo2XmlDescriptorNamespace {

    // this is set by the type service to the context that knows how to locate
    // the definitions
    public RuntimeContext context;
    
    @XNode(value = "@prefix")
    protected String prefix;
    
    @XNode(value = "@namespace")
    protected String namespace;

    
   
    
	/**
	 * @return the prefix
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * @return the workflowXpath
	 */
	public String getNamespace() {
		return namespace;
	}

    public String toString() {
    	return "prefix="+prefix+", namespace="+namespace;
    }
    
    
}
