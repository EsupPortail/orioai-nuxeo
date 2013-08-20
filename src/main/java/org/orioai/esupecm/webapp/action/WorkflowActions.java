package org.orioai.esupecm.webapp.action;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.ecm.webapp.versioning.VersionedActions;
import org.nuxeo.runtime.api.Framework;
import org.orioai.esupecm.OriOaiMetadataType;
import org.orioai.esupecm.nuxeo2xml.service.OriOaiNuxeo2XmlService;
import org.orioai.esupecm.relations.OriOaiRelationActionsBean;
import org.orioai.esupecm.workflow.ActionInfo;
import org.orioai.esupecm.workflow.EditorInfo;
import org.orioai.esupecm.workflow.OriOaiDocumentInfo;
import org.orioai.esupecm.workflow.service.OriOaiWorkflowService;

/**
 * This Seam bean manages the referencing tab in ORI-OAI Esup-ECM. TODO : add
 * event listener for relation deletion
 * 
 * @author Vincent Bonamy
 * @author François Jannin
 * @author Yohan Colmant
 */

@Scope(ScopeType.CONVERSATION)
@Name("oriManager")
@Transactional
public class WorkflowActions implements Serializable {

	private boolean useCache = true;
	
	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(WorkflowActions.class);

	protected static final String INIT_ORIWORKFLOW = "INIT_ORIWORKFLOW";

	@PrePassivate
	public void prePassivate() {
		log.debug("prePassivate :: ");
	}

	@PostActivate
	public void postActivate() {
		log.debug("postActivate :: ");
	}

	@Remove
	@Destroy
	public void destroy() {
		log.debug("destroy :: ");
	}

	@In(create = true)
	protected transient NavigationContext navigationContext;

	@In(create = true)
	protected transient WebActions webActions;

	@In(create = true)
	protected transient VersionedActions versionedActions;

	@In(create = true, required = false)
	protected transient CoreSession documentManager;

	@In(create = true)
	protected transient VersioningManager versioningManager;

	@In(create = true)
	private OriOaiRelationActionsBean referencingManager;

	@In(create = true)
	protected transient FacesMessages facesMessages;

	@In(create = true)
	protected transient ResourcesAccessor resourcesAccessor;

	@In
	protected transient Principal currentUser;


	/**
	 * id of ori instance for selected action
	 */
	@RequestParameter
	private String oriIdParam;

	/**
	 * Idp for selected action
	 */
	@RequestParameter
	private String oriIdpParam;

	/**
	 * id of action to perform
	 */
	@RequestParameter
	private String actionIdParam;
	/**
	 * name of action to perform
	 */
	@RequestParameter
	private String actionNameParam;

	
	
	
	
	/**
	 * Handle for Workflow WS Invoked first in getVersionsReferencedModel
	 */
	private OriOaiWorkflowService oriOaiWorkflowService = null;

	/**
	 * List of available types in Workflow WS Invoked first in
	 * getVersionsReferencedModel
	 */
	private Map<String, List<OriOaiMetadataType>> metadataTypes = null;

	/**
	 * Cache for ori infos fetched from WS Invoked first in getOriInfos(), and
	 * updated by updateOriInfoCache() Map of document.id / ori infos list pairs
	 */
	Map<String, List<OriOaiDocumentInfo>> oriInfosCache = new HashMap<String, List<OriOaiDocumentInfo>>();

	

	/**
	 * Get the ori-oai-workflow service
	 * 
	 * @return
	 * @throws ClientException
	 */
	private OriOaiWorkflowService getOriOaiWorkflowService()
			throws ClientException {
		boolean fail = false;
		if (oriOaiWorkflowService == null) {
			try {
				oriOaiWorkflowService = Framework
						.getService(OriOaiWorkflowService.class);
				if (oriOaiWorkflowService == null) {
					fail = true;
				}
			} catch (Exception e) {
				log.error("can't access Workflow WS", e);
				fail = true;
			}
		}
		if (fail == true) {
			facesMessages.add(FacesMessage.SEVERITY_ERROR, resourcesAccessor
					.getMessages().get(
							"oriref.error.cantaccess.workflow.service"));
			return null;

		}
		return oriOaiWorkflowService;
	}

	/**
	 * Get the XML generator service
	 * 
	 * @return
	 * @throws ClientException
	 */
	private OriOaiNuxeo2XmlService getOriOaiNuxeo2XmlService()
			throws ClientException {

		OriOaiNuxeo2XmlService service;
		try {
			service = Framework.getService(OriOaiNuxeo2XmlService.class);
		} catch (Exception e) {
			throw new ClientException(e);
		}

		return service;
	}


	/**
	 * Return the ORI infos for all publishing proxies for a given version
	 * document
	 * 
	 * @param versionDoc
	 *            a version document model
	 * @return list of all ORI infos
	 * @throws ClientException
	 *             TODO : sort list by section
	 */
	public List<OriOaiDocumentInfo> getVersionOriInfos(DocumentModel versionDoc) throws ClientException {
		
		if (useCache && oriInfosCache != null && oriInfosCache.containsKey(versionDoc.getId())) {
			if (log.isDebugEnabled())
				log.debug("getVersionOriInfos :: get from cache :: versionDoc.getId()=" + versionDoc.getId());
			
			return oriInfosCache.get(versionDoc.getId());
		}
		else {	
			List<String> oriIdProp = referencingManager.getOriIdsForDocument(versionDoc);
			if (log.isDebugEnabled())
				log.debug("getVersionOriInfos :: update cache :: versionDoc.getId()=" + versionDoc.getId()+", oriIdProp=" + oriIdProp);
			
			return updateOriInfoCache(versionDoc, oriIdProp);
		}
	}

	
	/**
	 * Fetch infos from workflow WS for each ori ID related to a given document
	 * 
	 * @param currentDoc
	 *            a DocumentModel
	 * @param oriIdProp
	 *            a list of ORI ids
	 * @return
	 * @throws ClientException
	 *             Note : store current idps to avoid time consuming WS requests
	 */
	public List<OriOaiDocumentInfo> fetchOriInfos(DocumentModel currentDoc,
			List<String> oriIdProp) throws ClientException {

		// Set the ori infos list
		List<OriOaiDocumentInfo> oriInfos = new ArrayList<OriOaiDocumentInfo>();

		// the ori-oai-workflow service
		OriOaiWorkflowService oriOaiWorkflowService = getOriOaiWorkflowService();
		if (oriIdProp != null) {
			for (String oriIdString : oriIdProp) {
				// the ori id
				Long oriId = Long.parseLong(oriIdString);
				OriOaiMetadataType metadataType = null;
				List<String> states = null;
				List<String> informations = null;
				List<EditorInfo> mdEditorUrls = null;
				List<ActionInfo> actions = null;
				boolean deletableRelation = false;
				String idp = null;
				if (oriOaiWorkflowService != null) {
					// the workflow idp
					idp = oriOaiWorkflowService.getIdp(currentUser.getName(), oriId);
					if (idp != null) {
						// the metadata type for this idp
						metadataType = oriOaiWorkflowService
								.getMetadataType(currentUser.getName(), idp);
						if (metadataType!=null) {
							
							// user language
							String language = FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage();
							if (log.isDebugEnabled())
								log.debug("fetchOriInfos :: language=" + language);
							
							// the workflow states for this idp
							Map<String, String> statesMap = oriOaiWorkflowService
									.getCurrentStates(currentUser.getName(), idp, language);
							states = new Vector<String>();
							for (String key : statesMap.keySet())
								states.add(statesMap.get(key));
							// the workflow informations for this idp
							informations = oriOaiWorkflowService.getCurrentInformations(currentUser.getName(), idp, language);
							// the md editors to edit this XML medatata content
							Map<String, String> mdEditorUrlsMap = oriOaiWorkflowService
									.getMdeditorUrls(currentUser.getName(), idp);
							mdEditorUrls = getMdEditorInfos(mdEditorUrlsMap);
							Map<String, String> actionsMap = oriOaiWorkflowService
									.getAvailableActions(currentUser.getName(), idp, language);
							actions = getActions(actionsMap);
						}
						else {
							log.warn("fetchOriInfos :: metadataType is null for ORI ID " + oriId);
							deletableRelation = true;
						}
					}
					else {
						log.warn("fetchOriInfos :: idp is null for ORI ID " + oriId);
						deletableRelation = true;
					}
				}
				else {
					log.warn("fetchOriInfos :: Workflow Web Service is NULL : ori infos unavailable");
				}
				// build info
				
				if (!deletableRelation) {
					OriOaiDocumentInfo newOriInfo = new OriOaiDocumentInfo(oriId,
						metadataType, states, idp, mdEditorUrls, actions, informations);
					newOriInfo.setProxyTitle(currentDoc.getTitle());
					DocumentRef parentRef = currentDoc.getParentRef();
					DocumentModel section = documentManager.getDocument(parentRef);
					newOriInfo.setSectionTitle(section.getTitle());
					newOriInfo.setDeletableRelation(deletableRelation);
					// add info
					oriInfos.add(newOriInfo);
				}
				else {

					log.warn("fetchOriInfos :: delete relation for ORI ID " + oriId);
					try {
						referencingManager.deleteOriRelation(oriId, currentDoc);
						Object[] params = new Object[2];
						params[0] = versioningManager.getVersionLabel(currentDoc);
						facesMessages.add(FacesMessage.SEVERITY_WARN, resourcesAccessor.getMessages().get("oriref.error.relation.deleted"), params);
					}
					catch(Exception e) {
						log.error("fetchOriInfos :: can't delete relation for ORI ID " + oriId, e);
					}
				}
			}
		}
		else {
			log.warn("fetchOriInfos :: list of ORI ID is NULL");
		}
		return oriInfos;
	}

	/**
	 * Update cache for ORI infos for this document
	 * 
	 * @param currentDoc
	 *            a DocumentModel
	 * @param oriIdProp
	 *            a list of ORI ids
	 * @return
	 * @throws ClientException
	 *             Note : store current idps to avoid time consuming WS requests
	 */
	public List<OriOaiDocumentInfo> updateOriInfoCache(DocumentModel currentDoc, List<String> oriIdProp)
			throws ClientException {
		
		if (log.isDebugEnabled()) {
			log.debug("updateOriInfoCache :: currentDoc.getId()=" + currentDoc.getId());
			log.debug("updateOriInfoCache :: oriIdProp=" + oriIdProp);
		}
		
		/*try {
			String s = null;
			s.substring(2, 3);
		}
		catch(Exception e) {
			log.error("getOutgoingStatementsInfo", e);
		}*/
		
		
		List<OriOaiDocumentInfo> oriInfos = fetchOriInfos(currentDoc, oriIdProp);
		if (log.isDebugEnabled())
			log.debug("updateOriInfoCache :: oriInfos=" + oriInfos);
		
		if (!useCache) {
			return oriInfos;
		}
		
		// update cache
		if (log.isDebugEnabled())
			log.debug("updateOriInfoCache :: oriInfosCache=" + oriInfosCache);
		if (oriInfosCache == null) {
			oriInfosCache = new HashMap<String, List<OriOaiDocumentInfo>>();
		}
		if (oriInfosCache.containsKey(currentDoc.getId())) {
			if (log.isDebugEnabled())
				log.debug("updateOriInfoCache :: oriInfosCache contains " + currentDoc.getId());
			
			List<OriOaiDocumentInfo> oldInfos = oriInfosCache.remove(currentDoc.getId());
			if (log.isDebugEnabled())
				log.debug("updateOriInfoCache :: oldInfos=" + oldInfos);
			
			// merge with new infos
			for (int old=0; oldInfos!=null && old<oldInfos.size(); old++) {
				OriOaiDocumentInfo oldInfo = oldInfos.get(old);
				
				boolean found = false;
				for (int up=0; !found && oriInfos!=null && up<oriInfos.size(); up++) {
					OriOaiDocumentInfo docInfo = oriInfos.get(up);
					found = docInfo.getOriId().equals(oldInfo.getOriId());
				}
				if (!found) {
					if (log.isDebugEnabled())
						log.debug("updateOriInfoCache :: add to oriInfos oldInfo=" + oldInfo);
					oriInfos.add(oldInfo);
				}
			}
		}
		else {
			if (log.isDebugEnabled())
				log.debug("updateOriInfoCache :: oriInfosCache doesn't contain " + currentDoc.getId());
		}
		
		if (log.isDebugEnabled())
			log.debug("updateOriInfoCache :: put to oriInfosCache currentDoc.getId()=" + currentDoc.getId()+", oriInfos="+oriInfos);
		oriInfosCache.put(currentDoc.getId(), oriInfos);
		
		// return the list
		return oriInfos;
	}

	
	
	
	protected List<EditorInfo> getMdEditorInfos(
			Map<String, String> mdEditorUrlsMap) {
		List<EditorInfo> result = new ArrayList<EditorInfo>();

		for (Map.Entry<String, String> entry : mdEditorUrlsMap.entrySet()) {
			if (entry.getKey() != null) {
				result.add(new EditorInfo(entry.getKey(), entry.getValue()));
			}
		}

		return result;
	}

	/**
	 * Convert Map into ActionInfo pojo for display in ori_action_view
	 * 
	 * @param actionsMap
	 *            contains id/name entries
	 * @return list of ActionInfo Note : returned names are prefixed for message
	 *         bundle translation
	 */
	protected List<ActionInfo> getActions(Map<String, String> actionsMap) {
		List<ActionInfo> result = new ArrayList<ActionInfo>();

		for (Map.Entry<String, String> entry : actionsMap.entrySet()) {
			if (entry.getKey() != null) {
				result.add(new ActionInfo(entry.getKey(), entry.getValue()));
			}
		}

		return result;
	}

	/**
	 * Return the possible actions for INIT_ORIWORKFLOW category Called by
	 * ori_view.xhtml
	 * 
	 * @return
	 */
	public List<Action> getActionsForInitOriWorkflow() throws ClientException {

		// Get all actions for the category INIT_ORIWORKFLOW
		List<Action> actions = webActions.getActionsList(INIT_ORIWORKFLOW);

		// We check if each action is available
		for (Action action : actions) {

			// default is available
			action.setAvailable(true);

		}
		return actions;
	}

	/**
	 * Check if selected document has ori:oriId property set
	 * 
	 * @return
	 * @throws ClientException
	 */
	public boolean getCanUnpublish() throws ClientException {
		log.debug("getCanUnpublish :: CANUNPUBLISH evaluation");
		DocumentModel currentDoc = navigationContext.getCurrentDocument();
		List<String> oriIds = getOriIds(currentDoc);
		return oriIds == null || oriIds.isEmpty();
	}

	/**
	 * Return oriIds for selected document (ori:oriId) as String[]
	 * 
	 * @return
	 * @throws ClientException
	 */
	public List<String> getOriIds(DocumentModel doc) throws ClientException {
		return referencingManager.getOriIdsForDocument(doc);
	}
	

	protected OriOaiMetadataType getMetadataType(String metaId)
			throws ClientException {
		if (log.isDebugEnabled())
			log.debug("getMetadataType :: metaId=" + metaId);
		
		for (OriOaiMetadataType type : getMetadataTypes()) {
			if (type.getId().equals(metaId)) {
				return type;
			}
		}
		return null;
	}


	
	/**
	 * Process remotely an available action for given actionIndex and oriId
	 * 
	 * @return
	 * @throws ClientException
	 */
	public String processWorkflowAction(DocumentModel versionDoc) throws ClientException {

		if (log.isDebugEnabled())
			log.debug("processWorkflowAction :: actionNameParam=" + actionNameParam+", oriIdParam=" + oriIdParam);
		
		if (actionIdParam != null && oriIdParam != null) {

			// the ori-oai-workflow service
			OriOaiWorkflowService oriOaiWorkflowService = getOriOaiWorkflowService();
			int actionId = Integer.parseInt(actionIdParam);
			if (log.isDebugEnabled())
				log.debug("processWorkflowAction :: actionId=" + actionId);
			
			String observation = resourcesAccessor.getMessages().get("oriref.action.performed.observation");
			if (log.isDebugEnabled())
				log.debug("processWorkflowAction :: observation=" + observation);
			
			boolean performActionResult = oriOaiWorkflowService.performAction(currentUser.getName(), oriIdpParam, actionId, observation);
			
			// the oriId
			List<String> oriIdProp = new ArrayList<String>();
			oriIdProp.add(oriIdParam);
			if (log.isDebugEnabled())
				log.debug("processWorkflowAction :: oriIdProp=" + oriIdProp);
			
			// result
			if (performActionResult) {
				String msg = resourcesAccessor.getMessages().get("oriref.action.performed.success");
				facesMessages.add(FacesMessage.SEVERITY_INFO, msg + actionNameParam);
				if (log.isDebugEnabled())
					log.debug("processWorkflowAction :: performActionResult=" + performActionResult+" --> add message=" + msg + actionNameParam);
				
				updateOriInfoCache(versionDoc, oriIdProp);
				
				return null;
				//return "OK";

			}
			else {
				String msg = resourcesAccessor.getMessages().get("oriref.action.performed.failure");
				facesMessages.add(FacesMessage.SEVERITY_WARN, msg + actionNameParam);
				if (log.isDebugEnabled())
					log.debug("processWorkflowAction :: performActionResult=" + performActionResult+" --> add message=" + msg + actionNameParam);
				
				updateOriInfoCache(versionDoc, oriIdProp);
				
				return null;
			}
		}

		return null;
		//return "OK";
	}
	
	
	
	
	

	/**
	 * Action called by action INIT_ORIWORKFLOW.
	 * 
	 * @return non-null if success, or null if failed
	 * @throws ClientException
	 */
	public String initOriWorkflow(DocumentModel versionToRef, String selectedMetadataType, String callEditor)
			throws ClientException {

		if (log.isDebugEnabled())
			log.debug("initOriWorkflow :: selectedMetatypes=" + selectedMetadataType+", callEditor="+callEditor);
		
		Map<String, String> oriIdsMap = new HashMap<String, String>();
		
		Long oriId = initWorkflow(versionToRef, selectedMetadataType);
		if (oriId != null) {
			oriIdsMap.put(oriId.toString(), selectedMetadataType);
		}
		/*String createRelationsMessage = */createNewReferencedRelations(versionToRef, oriIdsMap);
		//facesMessages.add(FacesMessage.SEVERITY_INFO, createRelationsMessage);
		
		// update oriinfo cache
		updateOriInfoCache(versionToRef, new ArrayList<String>(oriIdsMap.keySet()));
		
		Object[] params = new Object[2];
		params[0] = versionToRef.getName();
		params[1] = getMetadataType(selectedMetadataType).getLabel();
		facesMessages.add(FacesMessage.SEVERITY_INFO, resourcesAccessor.getMessages().get("oriref.report"), params);
		
		return "OK";
	}
	

	/**
	 * Create a relation between document and each of ori ID contained in the
	 * orIdsToRef map Note : labels needed for report message
	 * 
	 * @param versionDocu
	 * @param versionModel
	 * @param orIdsToRef
	 *            a map of ( ori Id / metadata type label) pairs
	 * @return
	 * @throws ClientException
	 */
	protected String createNewReferencedRelations(DocumentModel versionDocu, Map<String, String> orIdsToRef) throws ClientException {
		if (log.isDebugEnabled())
			log.debug("initOriWorkflow :: set the property oriId and create a version" + "proxyDoc.ref : " + versionDocu.getRef()	+ " oriIdsToRef : " + orIdsToRef.toString());
		String refMessage = "";
		
		try {
			// 2. create a relation on each id
			for (Map.Entry<String, String> oriIdToRef : orIdsToRef.entrySet()) {
				String mesgStatus = referencingManager.createOriRelation(oriIdToRef.getKey(), versionDocu, oriIdToRef.getValue());
				refMessage += "\n" + resourcesAccessor.getMessages().get(mesgStatus);
			}
			
		}
		catch (Exception e) {
			log.error("createNewReferencedRelations :: can't create relation for referencing", e);
		}

		return refMessage;
	}

	/**
	 * Init a workflow instance with given doc and metatype id
	 * 
	 * @param proxyDoc :
	 *            proxy of the version from which document will be cloned,
	 *            oriIds added
	 * @param type
	 * @return idp of the workflow instance or null if failed
	 * @throws ClientException
	 */
	protected Long initWorkflow(DocumentModel proxyDoc, String type)
			throws ClientException {
		String debug = "proxyDoc.ref : " + proxyDoc.getRef()
				+ " metadata Type id  : " + type;

		String metadataTypeId = type;
		log.info("initWorkflow :: try to init a new ori-oai workflow (metadataTypeId=" + metadataTypeId+") ...");

		// check if document is locked
		if (documentManager.getLock(proxyDoc.getRef()) != null) {
			facesMessages.add(FacesMessage.SEVERITY_WARN, resourcesAccessor
					.getMessages().get("error.document.locked.for.publish"));
			return null;
		}
		try {
			// the ori-oai-workflow service
			OriOaiWorkflowService oriOaiWorkflowService = getOriOaiWorkflowService();

			// create a new workflow instance and get the workflow
			// ID
			Long oriWorkflowId = oriOaiWorkflowService.newWorkflowInstance(currentUser.getName(), metadataTypeId);
			if (log.isDebugEnabled())
				log.debug("initWorkflow :: metadataTypeId="+metadataTypeId+", currentUser.getName()="+currentUser.getName()+" --> oriWorkflowId=" + oriWorkflowId);
			
			// get the idp
			String idp = oriOaiWorkflowService.getIdp(currentUser.getName(), oriWorkflowId);
			if (log.isDebugEnabled())
				log.debug("initWorkflow :: idp=" + idp);
			
			// get the original XML from the workflow
			String xmlFromWorkflow = oriOaiWorkflowService.getXMLForms(currentUser.getName(), idp);
			
			// the XML generator service
			OriOaiNuxeo2XmlService oriOaiNuxeo2XmlService = getOriOaiNuxeo2XmlService();

			// get metadata namespace
			String metadataSchemaNamespace = oriOaiWorkflowService.getMetadataSchemaNamespace(currentUser.getName(), metadataTypeId);
			
			// create XML metadata content
			String mergedXmlContent = oriOaiNuxeo2XmlService.mergeXmlDoc(documentManager, proxyDoc, metadataSchemaNamespace, xmlFromWorkflow);
			if (log.isDebugEnabled())
				log.debug("initWorkflow :: mergedXmlContent=" + mergedXmlContent);
			
			// return this XML to workflow
			oriOaiWorkflowService.saveXML(currentUser.getName(), idp, mergedXmlContent);
			log.debug("initWorkflow :: mergedXmlContent saved");
			
			// get the md editor url
			Map<String, String> mdEditorUrlsMap = oriOaiWorkflowService.getMdeditorUrls(currentUser.getName(), idp);
			if (log.isDebugEnabled())
				log.debug("initWorkflow :: mdEditorUrlsMap=" + mdEditorUrlsMap);
			
			// set variables to call editor
			processCallEditor(idp, oriWorkflowId, proxyDoc, getMdEditorInfos(mdEditorUrlsMap));
			
			/*
			 * for (int i=0; currentMdEditorUrls!=null && i<currentMdEditorUrls.size();
			 * i++) { String url = currentMdEditorUrls.get(i).getUrl(); url =
			 * url + "?idp=" + idp; currentMdEditorUrls.get(i).setUrl(url); }
			 */

			return oriWorkflowId;

		} catch (Exception e) {
			log.error("fail to init ori workflow : " + debug, e);
			return null;
		}
	}

	public int getVersionsCount() throws ClientException {
		DocumentModel doc = navigationContext.getCurrentDocument();
		List<VersionModel> versions = documentManager
				.getVersionsForDocument(doc.getRef());
		if (versions != null) {
			return versions.size();
		} else {
			return 0;
		}
	}

	/**
	 * Build a map of ReferencingVersion holding infos for ori-oai workflow
	 * instances
	 * 
	 * @return list of ReferencingVersion
	 * @throws ClientException
	 */
	public Collection<ReferencingVersion> getVersionsReferencedModel() throws ClientException {

		Map<String, ReferencingVersion> versionsProxiesModelMap = new HashMap<String, ReferencingVersion>();
		//TODO
		log.debug("");
		log.debug("");
		log.debug("getVersionsReferencedModel :: BEGIN METHOD");
		
		DocumentModel doc = navigationContext.getCurrentDocument();
		if (log.isDebugEnabled()) {
			log.debug("getVersionsReferencedModel :: doc.getId()=" + doc.getId());
			log.debug("getVersionsReferencedModel :: doc=" + doc);
		}
		
		List<VersionModel> versions = documentManager.getVersionsForDocument(doc.getRef());
		if (log.isDebugEnabled())
			log.debug("getVersionsReferencedModel :: versions=" + versions);
		
		for (VersionModel versionModel : versions) {
			if (log.isDebugEnabled()) {
				log.debug("getVersionsReferencedModel");
				log.debug("getVersionsReferencedModel :: doc.getRef()=" + doc.getRef());
			}
			
			DocumentModel versionDoc = documentManager.getDocumentWithVersion(doc.getRef(), versionModel);
			if (log.isDebugEnabled())
				log.debug("getVersionsReferencedModel :: versionDoc.getId()=" + versionDoc.getId());
			
			if (versionDoc != null) {
				
				String versionLabel = versioningManager.getVersionLabel(versionDoc);
				if (log.isDebugEnabled())
					log.debug("getVersionsReferencedModel :: versionLabel=" + versionLabel);
				
				if (!versionsProxiesModelMap.containsKey(versionDoc.getId())) {
					List<OriOaiDocumentInfo> allProxyOriInfos = getVersionOriInfos(versionDoc);
					if (log.isDebugEnabled())
						log.debug("getVersionsReferencedModel :: allProxyOriInfos=" + allProxyOriInfos);
					ReferencingVersion referencingVersion = new ReferencingVersion(versionLabel, versionDoc, versionModel,allProxyOriInfos);
					if (log.isDebugEnabled())
						log.debug("getVersionsReferencedModel :: versionDoc.getId()=" + versionDoc.getId());
					versionsProxiesModelMap.put(versionDoc.getId(),	referencingVersion);
				}
			}
			log.debug("getVersionsReferencedModel");
		}
		
		log.debug("getVersionsReferencedModel :: END METHOD");
		log.debug("");
		log.debug("");
		
		
		Collection<ReferencingVersion> result = versionsProxiesModelMap.values(); 
		List<ReferencingVersion> versionsList = new ArrayList<ReferencingVersion>(result);
		Collections.sort(versionsList);
		return versionsList;
	}

	
	
	public List<OriOaiMetadataType> getReferencableMetadataTypes(DocumentModel versionDoc) throws ClientException {
		// build availables metadata types model
		log.debug("getReferencableMetadataTypes :: ");
		List<OriOaiDocumentInfo> selectedProxyOriInfos = getVersionOriInfos(versionDoc);
		if (log.isDebugEnabled())
			log.debug("getReferencableMetadataTypes :: selectedProxyOriInfos="+selectedProxyOriInfos);
		List<OriOaiMetadataType> availTypes = new ArrayList<OriOaiMetadataType>(getMetadataTypes());
		if (log.isDebugEnabled())
			log.debug("getReferencableMetadataTypes :: availTypes="+availTypes);
		for (OriOaiDocumentInfo oriInfo : selectedProxyOriInfos) {
			// doesn't work (bad comparator) :  availTypes.remove(oriInfo.getMetadataType());
			for(OriOaiMetadataType availType : availTypes){
				if(availType.getId().equals(oriInfo.getMetadataType().getId())){
					availTypes.remove(availType);
					break;
				}
			}
		}
		if (log.isDebugEnabled())
			log.debug("getReferencableMetadataTypes :: availTypes="+availTypes);
		return availTypes;
	}

	/**
	 * Returns a list of metadata types available as a Map
	 * 
	 * @param versionDoc
	 * @return
	 * @throws ClientException
	 */
	public Map<String, String> getReferencableMetadataTypesMap(DocumentModel versionDoc) throws ClientException {
		
		log.debug("getReferencableMetadataTypesMap :: BEGIN");
		
		List<OriOaiMetadataType> metadataTypes = getReferencableMetadataTypes(versionDoc);
		if (log.isDebugEnabled())
			log.debug("getReferencableMetadataTypesMap :: metadataTypes="+metadataTypes);
		
		Map<String, String> metadataTypesMap = new HashMap<String, String>(metadataTypes.size());
		
		for(OriOaiMetadataType metadataType: metadataTypes) {
			metadataTypesMap.put(metadataType.getLabel(), metadataType.getId());
		}
		if (log.isDebugEnabled())
			log.debug("getReferencableMetadataTypesMap :: metadataTypesMap="+metadataTypesMap);
		
		log.debug("getReferencableMetadataTypesMap :: END");
		return metadataTypesMap;
	}

	public List<OriOaiMetadataType> getMetadataTypes() throws ClientException {
		
		if (log.isDebugEnabled())
			log.debug("getMetadataTypes :: 1 metadataTypes="+metadataTypes);
		
		if (metadataTypes == null) {
			metadataTypes = new HashMap<String, List<OriOaiMetadataType>>();
		}
		
		if (log.isDebugEnabled())
			log.debug("getMetadataTypes :: 2 metadataTypes="+metadataTypes);
		
		if (log.isDebugEnabled())
			log.debug("getMetadataTypes :: 3 currentUser.getName()="+currentUser.getName());
		
		List<OriOaiMetadataType> userMetadataTypes = metadataTypes.get(currentUser.getName());
		
		if (log.isDebugEnabled())
			log.debug("getMetadataTypes :: 4 userMetadataTypes="+userMetadataTypes);
		
		if (userMetadataTypes == null) {
			userMetadataTypes = getOriOaiWorkflowService().getMetadataTypes(currentUser.getName());
			metadataTypes.put(currentUser.getName(), userMetadataTypes);
		}
		
		if (log.isDebugEnabled())
			log.debug("getMetadataTypes :: 5 userMetadataTypes="+userMetadataTypes);
		
		return userMetadataTypes;
	}


	/*
	 * Called by ori_actions_view.xhtml
	 * 
	 */
	public boolean getCanReferencePublication(DocumentModel versionDocument)
			throws ClientException {
		List<OriOaiMetadataType> avails = getReferencableMetadataTypes(versionDocument);
		return !(avails == null || avails.isEmpty());

	}




	/***************************************************************************
	 * METHODS FOR POP-UP
	 */
	
	/**
	 * if we have to call the editor
	 */
	private boolean callEditor;

	
	
	
	/**
	 * The md editor urls we have to call
	 */
	private List<EditorInfo> currentMdEditorUrls;
	private String currentMdEditorIdp;
	private Long currentMdEditorId;
	private DocumentModel currentMdEditorVersionDoc;

	
	
	public String processCallEditor(String idp, Long id, DocumentModel versionDoc, EditorInfo mdEditorUrl) {
		List<EditorInfo> urls = new ArrayList<EditorInfo>();
		urls.add(mdEditorUrl);
		processCallEditor(idp, id, versionDoc, urls);
		return null;
	}
	
	
	public String processCallEditor(String idp, Long id, DocumentModel versionDoc, List<EditorInfo> urls) {
		this.currentMdEditorIdp = idp;
		this.currentMdEditorId = id;
		this.currentMdEditorVersionDoc = versionDoc;
		this.callEditor = true;
		this.currentMdEditorUrls = urls;
		if (log.isDebugEnabled())
			log.debug("processCallEditor :: this.callEditor="+this.callEditor+"; this.currentMdEditorId="+this.currentMdEditorId+"; this.currentMdEditorIdp="+this.currentMdEditorIdp);
		return null;
	}
	
	
	public boolean getCallEditor() {
		if (log.isDebugEnabled())
			log.debug("getCallEditor :: this.callEditor="+this.callEditor+"; this.currentMdEditorId="+this.currentMdEditorId+"; this.currentMdEditorIdp="+this.currentMdEditorIdp);
		if (this.callEditor) {
			this.callEditor = false;
			//this.currentMdEditorIdp = null;
			return true;
		}
		else {
			return false;
		}
	}
	

	public List<EditorInfo> getCurrentMdEditorUrls() throws ClientException {
		if (this.currentMdEditorUrls != null) {
			if (log.isDebugEnabled())
				log.debug("getCurrentMdEditorUrls :: currentMdEditorUrls is not null :: currentMdEditorUrls=" + this.currentMdEditorUrls);
		}
		else {
			log.debug("getCurrentMdEditorUrls :: currentMdEditorUrls is null");
			OriOaiWorkflowService oriOaiWorkflowService = getOriOaiWorkflowService();

			// get the md editor url
			if (log.isDebugEnabled()) {
				log.debug("getCurrentMdEditorUrls :: this.currentMdEditorId=" + this.currentMdEditorId);
				log.debug("getCurrentMdEditorUrls :: this.currentMdEditorIdp=" + this.currentMdEditorIdp);
			}
			Map<String, String> mdEditorUrlsMap = oriOaiWorkflowService.getMdeditorUrls(currentUser.getName(), this.currentMdEditorIdp);
			if (log.isDebugEnabled())
				log.debug("getCurrentMdEditorUrls :: mdEditorUrlsMap=" + mdEditorUrlsMap);
			
			this.currentMdEditorUrls = getMdEditorInfos(mdEditorUrlsMap);
		}

		return this.currentMdEditorUrls;
	}

	
	public String getCurrentMdEditorIdp() {
		if (log.isDebugEnabled())
			log.debug("getCurrentMdEditorIdp :: this.currentMdEditorIdp=" + this.currentMdEditorIdp);
		return this.currentMdEditorIdp;
	}
	
	public Long getCurrentMdEditorId() {
		if (log.isDebugEnabled())
			log.debug("getCurrentMdEditorId :: this.currentMdEditorId=" + this.currentMdEditorId);
		return this.currentMdEditorId;
	}
	
	public DocumentModel getCurrentMdEditorVersionDoc() {
		if (log.isDebugEnabled())
			log.debug("getCurrentMdEditorVersionDoc :: this.currentMdEditorVersionDoc=" + this.currentMdEditorVersionDoc);
		return this.currentMdEditorVersionDoc;
	}
	
	
	public String backFromEditor() throws ClientException {
		if (log.isDebugEnabled())
			log.debug("backFromEditor :: currentMdEditorId="+currentMdEditorId);
		
		List<String> oriIdProp = new ArrayList<String>();
		oriIdProp.add(currentMdEditorId+"");
		if (log.isDebugEnabled()) {
			log.debug("backFromEditor :: oriIdProp=" + oriIdProp);
			log.debug("backFromEditor :: currentMdEditorVersionDoc=" + currentMdEditorVersionDoc);
		}
		
		updateOriInfoCache(currentMdEditorVersionDoc, oriIdProp);
		log.debug("backFromEditor :: updated cache");
		
		return null;
	}

}
