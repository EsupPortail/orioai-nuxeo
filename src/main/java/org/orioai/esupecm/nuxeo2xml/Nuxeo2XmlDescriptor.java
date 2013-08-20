package org.orioai.esupecm.nuxeo2xml;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.RuntimeContext;

@XObject("configuration")
public class Nuxeo2XmlDescriptor {

    // this is set by the type service to the context that knows how to locate
    // the definitions
    public RuntimeContext context;
    
    @XNode("metadataSchemaNamespace")
    protected String metadataSchemaNamespace;
    
    @XNodeList(value = "metadata", type = String[].class, componentType = Nuxeo2XmlDescriptorMetadata.class)
    protected Nuxeo2XmlDescriptorMetadata[] metadatas;

    @XNodeList(value = "ns", type = String[].class, componentType = Nuxeo2XmlDescriptorNamespace.class)
    protected Nuxeo2XmlDescriptorNamespace[] namespaces;
    
    @XNode("embeddedMetadataFilePath")
    protected String embeddedMetadataFilePath;
    
    
    
    
    
	/**
	 * @return the metadataSchemaNamespace
	 */
	public String getMetadataSchemaNamespace() {
		return metadataSchemaNamespace;
	}

	/**
	 * 
	 * @return the embeddedMetadataFilePath
	 */
	public String getEmbeddedMetadataFilePath() {
		return embeddedMetadataFilePath;
	}

	/**
	 * @return the metadatas
	 */
	public Nuxeo2XmlDescriptorMetadata[] getMetadatas() {
		return metadatas;
	}
	
	/**
	 * @return the namespaces
	 */
	public Nuxeo2XmlDescriptorNamespace[] getNamespaces() {
		return namespaces;
	}

    
	 public String toString() {
		 String metadatasString = "";
		 for (int i=0; metadatas!=null && i<metadatas.length; i++) {
			 metadatasString += metadatas[i];
			 if (i!=metadatas.length-1) {
				 metadatasString += ", ";
			 }
		 }
		 
		 String namespacesString = "";
		 for (int i=0; namespaces!=null && i<namespaces.length; i++) {
			 namespacesString += namespaces[i];
			 if (i!=namespaces.length-1) {
				 namespacesString += ", ";
			 }
		 }
		 
		 return metadataSchemaNamespace+" --> ["+metadatasString+"]"+", ["+namespacesString+"]"+", embeddedMetadataFilePath="+embeddedMetadataFilePath;
	 }
    
}
