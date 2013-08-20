package org.orioai.esupecm.workflow;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("configuration")
public class WsDescriptor {

    @XNode("wsUrl")
    protected String wsUrl;
    

    @XNode("mdEditorFromUrl")
    protected String mdEditorFromUrl;
    

    @XNode("mdEditorToUrl")
    protected String mdEditorToUrl;

	/**
	 * @return the wsUrl
	 */
	public String getWsUrl() {
		return wsUrl;
	}

	
	public String getMdEditorFromUrl() {
		return mdEditorFromUrl;
	}

	public String getMdEditorToUrl() {
		return mdEditorToUrl;
	}

    public boolean isMdEditorTranslationSet() {
    	return mdEditorFromUrl!=null && mdEditorToUrl!=null && !"".equals(mdEditorFromUrl.trim()) && !"".equals(mdEditorToUrl.trim());
    }
    
    
}
