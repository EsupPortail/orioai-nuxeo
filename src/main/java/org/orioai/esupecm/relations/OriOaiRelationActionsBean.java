package org.orioai.esupecm.relations;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.web.StatementInfo;

/**
 * Bean assuming relation operations such as referencing a document in
 * ORI-OAI-workflow
 *   
 * @author François Jannin
 **/

@Name("referencingManager")
@Scope(CONVERSATION)
public class OriOaiRelationActionsBean extends CustomRelationActions implements OriOaiRelationActions {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(OriOaiRelationActionsBean.class);

	public static final String  RELATION_DC_REFERENCES = "http://purl.org/dc/terms/References";
	
	/**
	 * create a relation between document and ori ID
	 * 
	 * @param oriId
	 * @param docToRefer
	 * @return status message
	 * @throws ClientException
	 */
	public String createOriRelation(String oriId, DocumentModel docToRefer,String metadataTypeLabel)
			throws ClientException {
		this.setPredicateUri(RELATION_DC_REFERENCES);
		this.setObjectType("literal");
		this.setObjectLiteralValue(oriId);
		String comment = "ORIOAI Referencement : " + metadataTypeLabel;
		this.setComment(comment);
		String result = addStatement(docToRefer);
		if("document_relations".equals(result)){
			result = "ori_view";
		}
		result = "ori_view";
		return result;
	
	}
	
	
	/**
	 * delete the relation between document and ori ID
	 * @param oriId
	 * @param document
	 * @throws ClientException
	 */
	public void deleteOriRelation(Long oriId, DocumentModel document) throws ClientException {
		resetStatements();
		
		log.info("removeOriRelation :: oriId=" + oriId);
		
		List<StatementInfo> statementInfos = getOutgoingStatementsInfo(document);
		if (log.isDebugEnabled())
			log.debug("removeOriRelation :: statementInfos=" + statementInfos);
		
		if(statementInfos != null && statementInfos.size() > 0){
			
			List<StatementInfo> statementsToDelete = new ArrayList<StatementInfo>();
			
			for(int i=0; i<statementInfos.size(); i++) {
				StatementInfo info = statementInfos.get(i);
				
				Object object = info.getObject();
				if (log.isDebugEnabled())
					log.debug("removeOriRelation :: object=" + object);
				
				if(object!= null && object instanceof LiteralImpl){
					String oriIds = ""+oriId;
					String objectValue = ((LiteralImpl)object).getValue();
					if (log.isDebugEnabled())
						log.debug("removeOriRelation :: oriIds=" + oriIds+", objectValue="+objectValue);
					if(oriIds.equals(objectValue)){
						if (log.isDebugEnabled())
							log.debug("removeOriRelation :: EQUALS oriId=" + oriId);
						statementsToDelete.add(info);
					}
				}
			}
			
			for (StatementInfo info : statementsToDelete) {
				if (log.isDebugEnabled())
					log.debug("removeOriRelation :: delete " + info);
				this.deleteStatement(info, document);
			}
		}
	}
	
	
	
	

	public List<String> getOriIdsForDocument(DocumentModel document) throws ClientException{
		resetStatements();
		List<StatementInfo> statementInfos = getOutgoingStatementsInfo(document);
		if (log.isDebugEnabled())
			log.debug("getOriIdsForDocument :: statementInfos=" + statementInfos);
		List<String> result = new ArrayList<String>();
		if(statementInfos != null && statementInfos.size() > 0){
			for(StatementInfo info : statementInfos){
				if (log.isDebugEnabled())
					log.debug("getOriIdsForDocument :: info=" + info);
				Object object = info.getObject();
				if (log.isDebugEnabled())
					log.debug("getOriIdsForDocument :: object=" + object);
				if(object!= null && object instanceof LiteralImpl){
					String oriId = ((LiteralImpl)object).getValue();
					if(oriId != null){
						if (log.isDebugEnabled())
							log.debug("getOriIdsForDocument :: oriId=" + oriId);
						result.add(oriId);
					}
				}
				
			}
		}
		return result;
	}
	
}
