package org.orioai.esupecm.relations;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.Subject;
import org.nuxeo.ecm.platform.relations.api.event.RelationEvents;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.RelationDate;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.web.NodeInfo;
import org.nuxeo.ecm.platform.relations.web.NodeInfoImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.relations.web.StatementInfo;
import org.nuxeo.ecm.platform.relations.web.StatementInfoComparator;
import org.nuxeo.ecm.platform.relations.web.StatementInfoImpl;
import org.nuxeo.ecm.platform.relations.web.listener.RelationActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextBoundActionBean;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

/**
 *Copy of @see org.nuxeo.ecm.platform.relations.web.listener.ejb.RelationActionsBean
 * MODIFIED FOR ORIOAI_nuxeo :
 * Light adaptations : addStatement and getOutgoingStatementsInfo with a Document as parameter
 * (because original implementation use currentDoc)
 * 
 *  Seam component that manages statements involving current document as well as
 * creation, edition and deletion of statements involving current document.
 * <p>
 * Current document is the subject of the relation. The predicate is resolved
 * thanks to a list of predicates URIs. The object is resolved using a type
 * (literal, resource, qname resource), an optional namespace (for qname
 * resources) and a value.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
@Name("customRelationActions")
@Scope(CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class CustomRelationActions extends DocumentContextBoundActionBean
        implements RelationActions, Serializable {

    private static final long serialVersionUID = 2336539966097558178L;

    private static final Log log = LogFactory.getLog(CustomRelationActions.class);

    protected static boolean includeStatementsInEvents = false;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected RelationManager relationManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(required = false)
    protected transient Principal currentUser;

    // statements lists
    protected List<Statement> incomingStatements;

    protected List<StatementInfo> incomingStatementsInfo;

    protected List<Statement> outgoingStatements;

    protected List<StatementInfo> outgoingStatementsInfo;

    // fields for relation creation

    protected String predicateUri;

    protected String objectType;

    protected String objectLiteralValue;

    protected String objectUri;

    protected String objectDocumentUid;

    protected String objectDocumentTitle;

    protected String comment;

    protected Boolean showCreateForm = false;

    protected List<DocumentModel> resultDocuments;

    protected boolean hasSearchResults = false;

    protected String searchKeywords;

    public DocumentModel getDocumentModel(Node node) throws ClientException {
        if (node.isQNameResource()) {
            QNameResource resource = (QNameResource) node;
            Map<String, Serializable> context = new HashMap<String, Serializable>();
            context.put(ResourceAdapter.CORE_SESSION_ID_CONTEXT_KEY,
                    documentManager.getSessionId());
            Object o = relationManager.getResourceRepresentation(
                    resource.getNamespace(), resource, context);
            if (o instanceof DocumentModel) {
                return (DocumentModel) o;
            }
        }
        return null;
    }

    // XXX AT: for BBB when repo name was not included in the resource uri
    @Deprecated
    private QNameResource getOldDocumentResource(DocumentModel document) {
        QNameResource documentResource = null;
        if (document != null) {
            documentResource = new QNameResourceImpl(
                    RelationConstants.DOCUMENT_NAMESPACE, document.getId());
        }
        return documentResource;
    }

    public QNameResource getDocumentResource(DocumentModel document)
            throws ClientException {
        QNameResource documentResource = null;
        if (document != null) {
            documentResource = (QNameResource) relationManager.getResource(
                    RelationConstants.DOCUMENT_NAMESPACE, document, null);
        }
        return documentResource;
    }

    protected List<StatementInfo> getStatementsInfo(List<Statement> statements)
            throws ClientException {
        if (statements == null) {
            return null;
        }
        List<StatementInfo> infoList = new ArrayList<StatementInfo>();
        for (Statement statement : statements) {
            Subject subject = statement.getSubject();
            // TODO: filter on doc visibility (?)
            NodeInfo subjectInfo = new NodeInfoImpl(subject,
                    getDocumentModel(subject), true);
            Resource predicate = statement.getPredicate();
            Node object = statement.getObject();
            NodeInfo objectInfo = new NodeInfoImpl(object,
                    getDocumentModel(object), true);
            StatementInfo info = new StatementInfoImpl(statement, subjectInfo,
                    new NodeInfoImpl(predicate), objectInfo);
            infoList.add(info);
        }
        return infoList;
    }

    public List<StatementInfo> getIncomingStatementsInfo()
            throws ClientException {
        if (incomingStatementsInfo != null) {
            return incomingStatementsInfo;
        }
        DocumentModel currentDoc = getCurrentDocument();
        Resource docResource = getDocumentResource(currentDoc);
        if (docResource == null) {
            incomingStatements = Collections.emptyList();
            incomingStatementsInfo = Collections.emptyList();
        } else {
            Statement pattern = new StatementImpl(null, null, docResource);
            incomingStatements = relationManager.getStatements(
                    RelationConstants.GRAPH_NAME, pattern);
            // add old statements, BBB
            Resource oldDocResource = getOldDocumentResource(currentDoc);
            Statement oldPattern = new StatementImpl(null, null, oldDocResource);
            incomingStatements.addAll(relationManager.getStatements(
                    RelationConstants.GRAPH_NAME, oldPattern));
            incomingStatementsInfo = getStatementsInfo(incomingStatements);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(new StatementInfoComparator());
            Collections.sort(incomingStatementsInfo, comp);
        }
        return incomingStatementsInfo;
    }

    //MODIFORI
    public List<StatementInfo> getOutgoingStatementsInfo() throws ClientException {
   	 DocumentModel currentDoc = navigationContext.getCurrentDocument();
   	 return getOutgoingStatementsInfo(currentDoc);
   }
   
   /**
	 * Return outgoing relations for a given DocumentModel
	 * Same as super.getOutgoingStatementsInfo(), except for document argument
	 * (@see org.nuxeo.ecm.platform.relations.web.listener.ejb.RelationActionsBean::getOutgoingStatementsInfo())
	 * @param currentDoc
	 * @return
	 * @throws ClientException
	 */
	public List<StatementInfo> getOutgoingStatementsInfo(DocumentModel currentDoc) throws ClientException {
		
		if (log.isDebugEnabled())
			log.debug("getOutgoingStatementsInfo :: currentDoc="+currentDoc);
   
		
		
        if (outgoingStatementsInfo != null) {
            return outgoingStatementsInfo;
        }
       
        Resource docResource = getDocumentResource(currentDoc);
        if (docResource == null) {
            outgoingStatements = Collections.emptyList();
            outgoingStatementsInfo = Collections.emptyList();
        } else {
            Statement pattern = new StatementImpl(docResource, null, null);
            outgoingStatements = relationManager.getStatements(
                    RelationConstants.GRAPH_NAME, pattern);
            // add old statements, BBB
            Resource oldDocResource = getOldDocumentResource(currentDoc);
            Statement oldPattern = new StatementImpl(oldDocResource, null, null);
            outgoingStatements.addAll(relationManager.getStatements(
                    RelationConstants.GRAPH_NAME, oldPattern));
            outgoingStatementsInfo = getStatementsInfo(outgoingStatements);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(new StatementInfoComparator());
            Collections.sort(outgoingStatementsInfo, comp);
        }
        return outgoingStatementsInfo;
    }
	
	
	
	

    public void resetStatements() {
        incomingStatements = null;
        incomingStatementsInfo = null;
        outgoingStatements = null;
        outgoingStatementsInfo = null;
    }

    // getters & setters for creation items

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getObjectDocumentTitle() {
        return objectDocumentTitle;
    }

    public void setObjectDocumentTitle(String objectDocumentTitle) {
        this.objectDocumentTitle = objectDocumentTitle;
    }

    public String getObjectDocumentUid() {
        return objectDocumentUid;
    }

    public void setObjectDocumentUid(String objectDocumentUid) {
        this.objectDocumentUid = objectDocumentUid;
    }

    public String getObjectLiteralValue() {
        return objectLiteralValue;
    }

    public void setObjectLiteralValue(String objectLiteralValue) {
        this.objectLiteralValue = objectLiteralValue;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectUri() {
        return objectUri;
    }

    public void setObjectUri(String objectUri) {
        this.objectUri = objectUri;
    }

    public String getPredicateUri() {
        return predicateUri;
    }

    public void setPredicateUri(String predicateUri) {
        this.predicateUri = predicateUri;
    }

    protected void notifyEvent(String eventId, DocumentModel source,
            Map<String, Serializable> options, String comment) {

        EventProducer evtProducer = null;

        try {
            evtProducer = Framework.getService(EventProducer.class);
        } catch (Exception e) {
            log.error("Unable to get EventProducer to send event notification",
                    e);
        }

        DocumentEventContext docCtx = new DocumentEventContext(documentManager,
                documentManager.getPrincipal(), source);
        options.put("category", RelationEvents.CATEGORY);
        options.put("comment", comment);

        try {
            evtProducer.fireEvent(docCtx.newEvent(eventId));
        } catch (ClientException e) {
            log.error("Error while trying to send notification message", e);
        }
    }

    //MODIFORI
    public String addStatement() throws ClientException {
    	DocumentModel currentDoc = navigationContext.getCurrentDocument();
    	return addStatement(currentDoc);
    }
    
    //MODIFORI
    public String addStatement(DocumentModel currentDoc) throws ClientException {
        Resource documentResource = getDocumentResource(currentDoc);
        if (documentResource == null) {
            throw new ClientException(
                    "Document resource could not be retrieved");
        }

        Resource predicate = new ResourceImpl(predicateUri);
        Node object = null;
        if (objectType.equals("literal")) {
            objectLiteralValue = objectLiteralValue.trim();
            object = new LiteralImpl(objectLiteralValue);
        } else if (objectType.equals("uri")) {
            objectUri = objectUri.trim();
            object = new ResourceImpl(objectUri);
        } else if (objectType.equals("document")) {
            objectDocumentUid = objectDocumentUid.trim();
            String repositoryName = navigationContext.getCurrentServerLocation().getName();
            String localName = repositoryName + "/" + objectDocumentUid;
            object = new QNameResourceImpl(
                    RelationConstants.DOCUMENT_NAMESPACE, localName);
        }

        // if outgoing statement is null
        if (outgoingStatements==null) {
        	getOutgoingStatementsInfo(currentDoc);
        }
        
        // create new statement
        Statement stmt = new StatementImpl(documentResource, predicate, object);
        if (!outgoingStatements.contains(stmt)) {
            // add statement to the graph
            List<Statement> stmts = new ArrayList<Statement>();
            String eventComment = null;
            if (comment != null) {
                comment = comment.trim();
                if (comment.length() > 0) {
                    stmt.addProperty(RelationConstants.COMMENT,
                            new LiteralImpl(comment));
                    eventComment = comment;
                }
            }
            Literal now = RelationDate.getLiteralDate(new Date());
            stmt.addProperty(RelationConstants.CREATION_DATE, now);
            stmt.addProperty(RelationConstants.MODIFICATION_DATE, now);
            if (currentUser != null) {
                stmt.addProperty(RelationConstants.AUTHOR, new LiteralImpl(
                        currentUser.getName()));
            }

            stmts.add(stmt);

            // notifications

            Map<String, Serializable> options = new HashMap<String, Serializable>();
            String currentLifeCycleState = currentDoc.getCurrentLifeCycleState();
            options.put(CoreEventConstants.DOC_LIFE_CYCLE,
                    currentLifeCycleState);
            if (includeStatementsInEvents) {
                putStatements(options, stmt);
            }
            options.put(RelationEvents.GRAPH_NAME_EVENT_KEY,
                    RelationConstants.GRAPH_NAME);

            // before notification
            notifyEvent(RelationEvents.BEFORE_RELATION_CREATION, currentDoc,
                    options, eventComment);

            // add statement
            relationManager.add(RelationConstants.GRAPH_NAME, stmts);

            // XXX AT: try to refetch it from the graph so that resources are
            // transformed into qname resources: useful for indexing
            if (includeStatementsInEvents) {
                putStatements(options, relationManager.getStatements(
                        RelationConstants.GRAPH_NAME, stmt));
            }

            // after notification
            notifyEvent(RelationEvents.AFTER_RELATION_CREATION, currentDoc,
                    options, eventComment);

            // make sure statements will be recomputed
            resetStatements();

            // commenté pour ne pas affiché Relation créée qui ne parle pas à l'utilisateur
            //facesMessages.add(FacesMessage.SEVERITY_INFO, resourcesAccessor.getMessages().get("label.relation.created"));
            resetCreateFormValues();
        } else {
            facesMessages.add(FacesMessage.SEVERITY_WARN, resourcesAccessor.getMessages().get("label.relation.already.exists"));
        }
        return "document_relations";
    }

    // for consistency for callers only
    private static void putStatements(Map<String, Serializable> options,
            List<Statement> statements) {
        options.put(RelationEvents.STATEMENTS_EVENT_KEY,
                (Serializable) statements);
    }

    private static void putStatements(Map<String, Serializable> options,
            Statement statement) {
        List<Statement> statements = new LinkedList<Statement>();
        statements.add(statement);
        options.put(RelationEvents.STATEMENTS_EVENT_KEY,
                (Serializable) statements);
    }

    public void toggleCreateForm(ActionEvent event) {
        showCreateForm = !showCreateForm;
    }

    private void resetCreateFormValues() {
        predicateUri = "";
        objectType = "";
        objectLiteralValue = "";
        objectUri = "";
        objectDocumentUid = "";
        objectDocumentTitle = "";
        comment = "";
        showCreateForm = false;
    }

    public String deleteStatement(StatementInfo stmtInfo, DocumentModel source)
            throws ClientException {
        if (stmtInfo != null && outgoingStatementsInfo != null
                && outgoingStatementsInfo.contains(stmtInfo)) {
            Statement stmt = stmtInfo.getStatement();
            // notifications
            Map<String, Serializable> options = new HashMap<String, Serializable>();
            //DocumentModel source = getCurrentDocument();
            String currentLifeCycleState = source.getCurrentLifeCycleState();
            options.put(CoreEventConstants.DOC_LIFE_CYCLE,
                    currentLifeCycleState);
            options.put(RelationEvents.GRAPH_NAME_EVENT_KEY,
                    RelationConstants.GRAPH_NAME);
            if (includeStatementsInEvents) {
                putStatements(options, stmt);
            }

            // before notification
            notifyEvent(RelationEvents.BEFORE_RELATION_REMOVAL, source,
                    options, null);

            // remove statement
            List<Statement> stmts = new ArrayList<Statement>();
            stmts.add(stmt);
            relationManager.remove(RelationConstants.GRAPH_NAME, stmts);

            // after notification
            notifyEvent(RelationEvents.AFTER_RELATION_REMOVAL, source, options,
                    null);

            // make sure statements will be recomputed
            resetStatements();

            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "label.relation.deleted"));
        }
        return "document_relations";
    }
    
    
    public String deleteStatement(StatementInfo stmtInfo) throws ClientException {
    	return deleteStatement(stmtInfo, getCurrentDocument());
    }
    
    

    public String getSearchKeywords() {
        return searchKeywords;
    }

    public void setSearchKeywords(String searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

    public String searchDocuments() throws ClientException {
    	if (log.isDebugEnabled())
			log.debug("Making call to get documents list for keywords: " + searchKeywords);
    	
        // reset existing search results
        resultDocuments = null;
        List<String> constraints = new ArrayList<String>();
        if (searchKeywords != null) {
            searchKeywords = searchKeywords.trim();
            if (searchKeywords.length() > 0) {
                if (!searchKeywords.equals("*")) {
                    // full text search
                    constraints.add(String.format("ecm:fulltext LIKE '%s'",
                            searchKeywords));
                }
            }
        }
        // no folderish doc nor hidden doc
        constraints.add("ecm:mixinType != 'Folderish'");
        constraints.add("ecm:mixinType != 'HiddenInNavigation'");
        // no archived revisions
        constraints.add("ecm:isCheckedInVersion = 0");
        // filter current document
        DocumentModel currentDocument = getCurrentDocument();
        if (currentDocument != null) {
            constraints.add(String.format("ecm:uuid != '%s'",
                    currentDocument.getId()));
        }
        // search keywords
        String query = String.format("SELECT * FROM Document WHERE %s",
                StringUtils.join(constraints.toArray(), " AND "));
        
        if (log.isDebugEnabled())
			log.debug("query: " + query);
        
        resultDocuments = documentManager.query(query, 100);
        hasSearchResults = !resultDocuments.isEmpty();
        
        if (log.isDebugEnabled())
        	log.debug("query result contains: " + resultDocuments.size() + " docs.");
        
        return "create_relation_search_document";
    }

    public List<DocumentModel> getSearchDocumentResults() {
        return resultDocuments;
    }

    public boolean getHasSearchResults() {
        return hasSearchResults;
    }

    public Boolean getShowCreateForm() {
        return showCreateForm;
    }

    public void initialize() {
        log.debug("Initializing...");
    }

    @Destroy
    @Remove
    public void destroy() {
        log.debug("Removing Seam action listener...");
    }

    @PrePassivate
    public void saveState() {
        log.debug("PrePassivate");
    }

    @PostActivate
    public void readState() {
        log.debug("PostActivate");
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        resetStatements();
    }

}





