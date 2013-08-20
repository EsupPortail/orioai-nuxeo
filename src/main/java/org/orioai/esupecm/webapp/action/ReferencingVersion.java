package org.orioai.esupecm.webapp.action;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.orioai.esupecm.workflow.OriOaiDocumentInfo;

/**
 * Class gathering ORI-OAI informations (from workflow instance) 
 * related to a given version of a Document
 * Used in ori-actions_view.xhtml
 * @author François Jannin
 *
 */
public class ReferencingVersion implements Comparable<ReferencingVersion>{
	
	private String versionLabel;
	
	private DocumentModel versionDoc;
	
	private VersionModel versionModel;
	
	
	
	private List<OriOaiDocumentInfo> oriInfos = new ArrayList<OriOaiDocumentInfo>();

	public ReferencingVersion(String versionLabel, DocumentModel versionDoc,
			VersionModel versionModel, List<OriOaiDocumentInfo> oriInfos) {
		super();
		this.versionLabel = versionLabel;
		this.versionDoc = versionDoc;
		this.versionModel = versionModel;
		this.oriInfos = oriInfos;
	}

	public DocumentModel getVersionDoc() {
		return versionDoc;
	}

	public void setVersionDoc(DocumentModel versionDoc) {
		this.versionDoc = versionDoc;
	}

	public String getVersionLabel() {
		return versionLabel;
	}

	public void setVersionLabel(String versionLabel) {
		this.versionLabel = versionLabel;
	}

	public List<OriOaiDocumentInfo> getOriInfos() {
		return oriInfos;
	}

	public void setOriInfos(List<OriOaiDocumentInfo> oriInfos) {
		this.oriInfos = oriInfos;
	}

	public VersionModel getVersionModel() {
		return versionModel;
	}

	public void setVersionModel(VersionModel versionModel) {
		this.versionModel = versionModel;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(ReferencingVersion o) {
		// TODO Auto-generated method stub
		int comp = this.versionLabel.compareTo(o.versionLabel);
		if (comp != 0) {
			return -comp;
		}
		else {
			return 1;
		}
	}
	
}
