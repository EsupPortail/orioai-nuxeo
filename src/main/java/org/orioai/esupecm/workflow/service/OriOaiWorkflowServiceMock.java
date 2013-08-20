package org.orioai.esupecm.workflow.service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.orioai.esupecm.OriOaiMetadataType;
import org.orioai.esupecm.workflow.WsDescriptor;

public class OriOaiWorkflowServiceMock extends DefaultComponent implements
	OriOaiWorkflowService {

    private List<WsDescriptor> config = new ArrayList<WsDescriptor>();

    public List<OriOaiMetadataType> getMetadataTypes(String username) {

        //String wsUrl = null;

        /*for (WsDescriptor d : config) {
            if (d.getWsUrl() != null) {
            	wsUrl = d.getWsUrl();
            }
        }*/

        List<OriOaiMetadataType> metadataTypes = new LinkedList<OriOaiMetadataType>();
        metadataTypes.add(new OriOaiMetadataType("ressource_pedago", "Ressource p�dagogique"));
        //metadataTypes.add(wsUrl);
        
        return metadataTypes;
    }

    
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        config.add((WsDescriptor) contribution);
    }

    
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        config.remove(contribution);
    }

	
	public Long newWorkflowInstance(String username, String metadataTypeId) {
		// TODO Auto-generated method stub
		return new Long(123);
	}

	
	public Map<String, String> getCurrentStates(String username, String idp, String language) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public List<String> getCurrentInformations(String username, String idp, String language) {
		// TODO Auto-generated method stub
		return null;
	}
	
	

	
	public String getMetadataSchemaNamespace(String username,
			String metadataTypeId) {
		// TODO Auto-generated method stub
		return null;
	}


	public OriOaiMetadataType getMetadataType(String username, String idp) {
		// TODO Auto-generated method stub
		return null;
	}

	
	public String getIdp(String username, Long id) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public String getMdeditorUrl(String username, String idp) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public String getXMLForms(String username, String idp) {
		String xmlContent = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">" + 
	     "<dc:identifier>HAL:sfo-00256974, version 1</dc:identifier>" + 
	     "<dc:identifier>http://hal-sfo.ccsd.cnrs.fr/sfo-00256974/en/</dc:identifier>" + 
	    "<dc:title>Analyse de la mesure</dc:title>" + 
	     "<dc:creator>Guellati, Saïda</dc:creator>" + 
	     "<dc:subject>Instrumentation optique, capteurs [681.25; 681.415]</dc:subject>" + 
	     "<dc:subject>Incertitude</dc:subject>" + 
	     "<dc:subject>Mesure</dc:subject>" + 
	     "<dc:subject>étalonnage</dc:subject>" + 
	     "<dc:description>Méthodologie de la mesure, estimation des incertitudes de mesures présentations des résultats.</dc:description>" + 
	    "<dc:publisher>HAL - CCSD</dc:publisher>" + 
	     "<dc:date>2007-05-30</dc:date>" + 
	     "<dc:type>lecture</dc:type>" + 
	     "<dc:language/>" + 
	     "<dc:relation>http://hal-sfo.ccsd.cnrs.fr/docs/00/25/69/74/PDF/Analyse_de_la_mesure.pdf</dc:relation>" + 
	     "<dc:rights/>" + 
	    " </oai_dc:dc>";
		
		return xmlContent;
	}
	
	
	public void saveXML(String username, String idp, String xmlContent) {
	}
	
	
	/**
	 * Return a map for availables md editors
	 * @param idp
	 * @param userId
	 * @return map formTitle:formUrl
	 */
	public Map<String,String> getMdeditorUrls(String username, String idp) {
		// TODO implement me !
		//IOriWorkflowService oriWorkflowService = getRemoteOriWorkflowService();
		//Map<String,String> formsMap = oriWorkflowService.getMdEditorUrl(idp);
		
		//return formsMap;
		
		return null;
	}


	public Map<String, String> getAvailableActions(String username, String idp, String language) {
		// TODO Auto-generated method stub
		return null;
	}


	public boolean performAction(String username, String idp, int actionId, String observation) {
		// TODO Auto-generated method stub
		return false;
	}

}
