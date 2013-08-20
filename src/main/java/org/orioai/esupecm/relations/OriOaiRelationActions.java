package org.orioai.esupecm.relations;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface OriOaiRelationActions {

	public String createOriRelation(String oriId, DocumentModel docToRefer,String metadataTypeLabel) throws ClientException;
	public void deleteOriRelation(Long oriId, DocumentModel document) throws ClientException;
	public List<String> getOriIdsForDocument(DocumentModel document) throws ClientException;
		
}
