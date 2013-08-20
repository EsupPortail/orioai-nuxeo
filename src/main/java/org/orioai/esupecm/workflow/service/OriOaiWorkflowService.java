package org.orioai.esupecm.workflow.service;

import java.util.List;
import java.util.Map;

import org.orioai.esupecm.OriOaiMetadataType;

public interface OriOaiWorkflowService {

	public String getIdp(String username, Long id);
	
	public String getMdeditorUrl(String username, String idp);
	
	public Map<String,String> getMdeditorUrls(String username, String idp);
	
	public Map<String,String> getAvailableActions(String username, String idp, String language);
	
	public List<OriOaiMetadataType> getMetadataTypes(String username);
    
    public Long newWorkflowInstance(String username, String metadataTypeId);
    
    public String getXMLForms(String username, String idp);
    
    public void saveXML(String username, String idp, String xmlContent);
    
    public Map<String, String> getCurrentStates(String username, String idp, String language);
    
    public List<String> getCurrentInformations(String username, String idp, String language);
    
    public OriOaiMetadataType getMetadataType(String username, String idp);
    
    public String getMetadataSchemaNamespace(String username, String metadataTypeId);
    
    /**
	 * Perform an available action
	 * @param idp
	 * @param actionId
	 * @param observation
	 * @return true if action was performed
	 */
	public boolean performAction(String username, String idp, int actionId, String observation);

}
