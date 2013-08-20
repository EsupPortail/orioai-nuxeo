package org.orioai.esupecm.filter;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;


@Name("oriCustomFiltersBean")
@Scope(ScopeType.CONVERSATION)
public class OriCustomFiltersBean {
	
    private static final Log log = LogFactory.getLog(OriCustomFiltersBean.class);
    
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
    protected transient CoreSession documentManager;
   
    

    public boolean isInSection() throws ClientException {
    	DocumentModel currentDocument = navigationContext.getCurrentDocument();
    	return documentManager.getSuperParentType(currentDocument).equals("Section");
    }

}
