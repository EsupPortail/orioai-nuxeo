package org.orioai.esupecm.workflow;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.orioai.esupecm.OriOaiMetadataType;

/**
 * Class gathering informations from Web services about a workflow instance tied to a nuxeo document
 * Includes idp for maintaining autenticated remote session between different actions from nuxeo session
 * @author François Jannin
 *
 */
public class OriOaiDocumentInfo {

	private static final Log log = LogFactory.getLog(OriOaiDocumentInfo.class);
	
    private Long oriId;
    
    private OriOaiMetadataType metadataType;
    
    private List<String> states;
    
    private List<String> informations;
    
    private String idp;
    
    private List<EditorInfo>  mdeditorUrls;
    
    private List<ActionInfo> actions;

    private String sectionTitle;
    
    private String proxyTitle;
    
    private boolean deletableRelation = false;
    
    
    
    public OriOaiDocumentInfo(int i, Long oriId, OriOaiMetadataType metadataType, List<String> states) {
        this.oriId = oriId;
        this.states = states;
        this.metadataType = metadataType;
        this.idp = null;
        this.mdeditorUrls = null;
    }
     
    public OriOaiDocumentInfo(Long oriId, OriOaiMetadataType metadataType, List<String> states, String idp, List<EditorInfo> mdeditorUrls, List<ActionInfo> actions, List<String> informations) {
        this.oriId = oriId;
        this.states = states;
        this.metadataType = metadataType;
        this.idp = idp;
        this.mdeditorUrls = mdeditorUrls;
        this.actions = actions;
        this.informations = informations;
    }

   public Long getOriId() {
		return oriId;
	}

	public OriOaiMetadataType getMetadataType() {
		return metadataType;
	}

	public List<String> getStates() {
		if (log.isDebugEnabled())
			log.debug("getStates :: oriId="+oriId+" :: states=" + states);
        return states;
    }
	
	public List<String> getInformations() {
		return this.informations;
		
    }

	public String getIdp() {
		return idp;
	}

	public  List<EditorInfo>  getMdeditorUrls() {
		return mdeditorUrls;
	}

	public String getSectionTitle() {
		return sectionTitle;
	}

	public void setSectionTitle(String sectionTitle) {
		this.sectionTitle = sectionTitle;
	}

	public String getProxyTitle() {
		return proxyTitle;
	}

	public void setProxyTitle(String proxyTitle) {
		this.proxyTitle = proxyTitle;
	}


	public  List<ActionInfo> getActions() {
		return actions;
	}

	public void setActions(List<ActionInfo> actions) {
		this.actions = actions;
	}

	public boolean isDeletableRelation() {
		return deletableRelation;
	}

	public void setDeletableRelation(boolean deletableRelation) {
		this.deletableRelation = deletableRelation;
	}

	public String toString() {
    	return oriId/*+";"+idp*/+";"+states+";"+metadataType+";"+proxyTitle;
    }
}
