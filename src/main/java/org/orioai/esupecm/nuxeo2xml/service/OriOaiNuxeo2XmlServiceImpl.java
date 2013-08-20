package org.orioai.esupecm.nuxeo2xml.service;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.jaxen.JaxenException;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.dom4j.Dom4jXPath;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.impl.plugins.SingleDocumentReader;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.RuntimeContext;
import org.orioai.esupecm.nuxeo2xml.Nuxeo2XmlDescriptor;
import org.orioai.esupecm.nuxeo2xml.Nuxeo2XmlDescriptorMetadata;
import org.orioai.esupecm.nuxeo2xml.Nuxeo2XmlDescriptorMetadataTranslation;
import org.orioai.esupecm.nuxeo2xml.Nuxeo2XmlDescriptorNamespace;
import org.orioai.esupecm.nuxeo2xml.NuxeoDocumentUrlDescriptor;

import de.schlichtherle.io.FileInputStream;

public class OriOaiNuxeo2XmlServiceImpl extends DefaultComponent implements OriOaiNuxeo2XmlService {

	private static final Log log = LogFactory.getLog(OriOaiNuxeo2XmlServiceImpl.class);

	protected List<Nuxeo2XmlDescriptor> config = new ArrayList<Nuxeo2XmlDescriptor>();
	protected NuxeoDocumentUrlDescriptor nuxeoUrlConfig;
	
	protected Map<String, Nuxeo2XmlDescriptor> nuxeo2XmlDescriptorMap = new HashMap<String, Nuxeo2XmlDescriptor>();
	
	protected RuntimeContext context;
    
	// variables
	private static final String NUXEO_URL = "NUXEO_URL"; 
	private static final String nuxeoIdXpath = "normalize-space(//@id)";
	
	
	
	/**
	 * @return the xslMap
	 */
	public Map<String, Nuxeo2XmlDescriptor> getNuxeo2XmlDescriptorMap() {
		return nuxeo2XmlDescriptorMap;
	}

	
	

	public void activate(ComponentContext context) {
		this.context = context.getRuntimeContext();
	}
	 
	
	public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
		if (log.isDebugEnabled())
			log.debug("registerContribution :: " + "add "+contribution+", extensionPoint="+extensionPoint+", contributor="+contributor);
		
		if ("nuxeoDocumentUrl".equals(extensionPoint)) {
			nuxeoUrlConfig = (NuxeoDocumentUrlDescriptor)contribution;
		}
		else {
			config.add((Nuxeo2XmlDescriptor) contribution);
		}
	}

	
	public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
		if (log.isDebugEnabled())
			log.debug("unregisterContribution :: " + "remove "+contribution+", extensionPoint="+extensionPoint+", contributor="+contributor);
		
		if ("nuxeoDocumentUrl".equals(extensionPoint)) {
			nuxeoUrlConfig = null;
		}
		else {
			config.remove(contribution);
		}
	}
	
	
	public void registerExtension(Extension extension) throws Exception {
		super.registerExtension(extension);
		
		if (log.isDebugEnabled())
			log.debug("registerExtension :: " + "register");
		
      	for(Nuxeo2XmlDescriptor desc: config) {
      		if (log.isDebugEnabled())
    			log.debug("registerExtension :: desc = " + desc);
      		
      		nuxeo2XmlDescriptorMap.put(desc.getMetadataSchemaNamespace(), desc);
      	}
	}

	
	
	
	/** @param filePath the name of the file to open. Not sure if it can accept URLs or just filenames. Path handling could be better, and buffer sizes are hardcoded
	*/ 
	public static String readFileAsString(String filePath) throws IOException{
		StringBuffer fileData = new StringBuffer(1000);
		BufferedInputStream reader = new BufferedInputStream(new FileInputStream(filePath));
		byte[] buf = new byte[1024];
		int numRead=0;
		while((numRead=reader.read(buf)) != -1){
			String readData = new String(buf, 0, numRead, "utf-8");
			fileData.append(readData);
			buf = new byte[1024];
		}
		reader.close();
		
		
		
		return fileData.toString();
	}


	
	
	
	
	
	private String getEmbeddedXmlMetadataFilePath(DocumentModel doc, Nuxeo2XmlDescriptor nuxeoDescriptor) throws ClientException {
		
		if (nuxeoDescriptor!=null) {
		
			if (doc.isDownloadable()) {
				org.nuxeo.ecm.core.api.model.Property propertyFile = doc.getProperty("file:content");
				if (propertyFile != null) {
	
					Blob requestedBlob = (Blob) propertyFile.getValue();
					if (requestedBlob != null) {
						String requestedFilename = requestedBlob.getFilename();
						
						if (requestedFilename.endsWith(".zip")) {
							try {
	
								String versionUid = doc.getId();
								if (log.isDebugEnabled())
									log.debug("getEmbeddedXmlMetadataFilePath :: versionUid="+versionUid);
								
								String tempdir = System.getProperty("java.io.tmpdir");
								if (log.isDebugEnabled())
									log.debug("getEmbeddedXmlMetadataFilePath :: tempdir="+tempdir);
								File zipFile = new java.io.File(tempdir, "nuxeo-md-to-wf-" + versionUid + ".zip");
								File zipDirectory = new java.io.File(tempdir, "nuxeo-md-to-wf-" + versionUid);
								
								if(!zipDirectory.exists()) {
									InputStream inputStream = requestedBlob.getStream();
									OutputStream out = new FileOutputStream(zipFile);
									byte buf[] = new byte[1024];
									int len;
									while ((len = inputStream.read(buf)) > 0) {
										out.write(buf, 0, len);
									}
									out.close();
									inputStream.close();
	
									de.schlichtherle.io.File trueZipFile = new de.schlichtherle.io.File(zipFile);
									trueZipFile.copyAllTo(zipDirectory);
								}
									
								File[] files = zipDirectory.listFiles();
									
								// get the embeddedMetadataFilePath
								
								// if one directory
								if (files!=null && files.length==1 && files[0].isDirectory()) {
									String rootDirecotoryName = files[0].getName();
									if (log.isDebugEnabled())
										log.debug("getEmbeddedXmlMetadataFilePath :: one directory element :: "+rootDirecotoryName);
									zipDirectory = files[0];
								}
								
								// check if path exists
								File embeddedMetadataFile = new File(zipDirectory, nuxeoDescriptor.getEmbeddedMetadataFilePath());
								if (log.isDebugEnabled())
									log.debug("getEmbeddedXmlMetadataFilePath :: embeddedMetadataFile="+embeddedMetadataFile);
								boolean exists = embeddedMetadataFile.exists();
								if (log.isDebugEnabled())
									log.debug("getEmbeddedXmlMetadataFilePath :: exists="+exists);
								
								if (exists) {
									String filePath = embeddedMetadataFile.getAbsolutePath();
									return filePath;
								}
							}
							catch (Exception ie) {
								log.error("getEmbeddedXmlMetadataFilePath :: problem unziping zip file from document :" + doc.getId(), ie);
							}
						} 
					}
				}
			}
		}
		
		return null;	
	}
		
	
	public String mergeXmlDoc(CoreSession documentManager, DocumentModel doc, String metadataSchemaNamespace, String workflowXmlDocument) throws ClientException {
		
		StringBuffer xmlContent = new StringBuffer();
		
		try {
			if (log.isDebugEnabled()) {
				log.debug("mergeXmlDoc :: "+"documentRef : " + doc.getRef() + " metadataSchemaNamespace : "+ metadataSchemaNamespace);
				log.debug("mergeXmlDoc :: nuxeo2XmlDescriptorMap = " + nuxeo2XmlDescriptorMap);
				log.debug("mergeXmlDoc :: metadataSchemaNamespace = " + metadataSchemaNamespace);
			}

			
			// the nuxeo descriptor
			Nuxeo2XmlDescriptor nuxeoDescriptor = nuxeo2XmlDescriptorMap.get(metadataSchemaNamespace);
			if (nuxeoDescriptor == null) {
				throw new ClientException(metadataSchemaNamespace + " not found in extension point nuxeoToxml of OriOaiNuxeo2XmlService. Probably in orioainuxeo2xml-contrib.xml");
			}
			if (log.isDebugEnabled())
				log.debug("mergeXmlDoc :: used nuxeoDescriptor = " + nuxeoDescriptor);
			
			// the metadatas translations
			Nuxeo2XmlDescriptorMetadata[] nuxeoDescriptorMetadatas = nuxeoDescriptor.getMetadatas();
			
			if (nuxeoDescriptorMetadatas==null) {
				return workflowXmlDocument;
			}
			
			
			// ******************************
			// the workflow document
			
			// XML 
			SAXReader workflowReader = new SAXReader();
			Document workflowRootDocument = workflowReader.read(new StringReader(workflowXmlDocument));
			
			// namespaces
			Nuxeo2XmlDescriptorNamespace[] nuxeoDescriptorNamespaces = nuxeoDescriptor.getNamespaces();
			HashMap<String, String> nsWorkflowMap = new HashMap<String, String>();
			for(int n=0; nuxeoDescriptorNamespaces!=null && n<nuxeoDescriptorNamespaces.length; n++) {
				Nuxeo2XmlDescriptorNamespace nuxeoDescriptorNamespace = nuxeoDescriptorNamespaces[n];
				nsWorkflowMap.put(nuxeoDescriptorNamespace.getPrefix(), nuxeoDescriptorNamespace.getNamespace());
			}

			// ******************************
			// the embedded document
			
			// get the embedded XML metadata file content
			String embeddedXmlMetadataFilePath = getEmbeddedXmlMetadataFilePath(doc, nuxeoDescriptor);
			if (embeddedXmlMetadataFilePath!=null) {
				
				// get the root document element
				SAXReader embeddedReader = new SAXReader();
				Document embeddedRootDocument = embeddedReader.read(new File(embeddedXmlMetadataFilePath));
				
				// merge
				workflowRootDocument = getMergedXmlDocs(nuxeoDescriptorMetadatas, Nuxeo2XmlDescriptorMetadata.TYPE_FILE, embeddedRootDocument, nsWorkflowMap, workflowRootDocument, nsWorkflowMap);
				
				if (log.isDebugEnabled())
					log.debug("mergeXmlDoc :: embedded xml == "+workflowRootDocument.asXML());
			}
			

			// ******************************
			// the nuxeo document
			
			// generate the temporary xml file from nuxeo data
			DocumentReader documentReader = new SingleDocumentReader(documentManager, doc);
			String nuxeoXmlDocument = documentReader.read().getDocument().asXML();
			if (log.isDebugEnabled())
				log.debug("mergeXmlDoc :: nuxeoXmlDocument = " + nuxeoXmlDocument);
			

			// namespaces
			HashMap<String, String> nsNuxeoMap = new HashMap<String, String>();
			nsNuxeoMap.put("dc", "http://purl.org/dc/elements/1.1/");
			nsNuxeoMap.put("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");
			nsNuxeoMap.put("nx_dc", "http://www.nuxeo.org/ecm/schemas/dublincore/");
			nsNuxeoMap.put("nx_uid", "http://project.nuxeo.com/geide/schemas/uid/");
			nsNuxeoMap.put("nx_common", "http://www.nuxeo.org/ecm/schemas/common/");
			nsNuxeoMap.put("nx_file", "http://www.nuxeo.org/ecm/schemas/file/");
			nsNuxeoMap.put("nx_picture", "http://www.nuxeo.org/ecm/schemas/picture");
						
			// get the root document element
			SAXReader nuxeoReader = new SAXReader();
			Document nuxeoRootDocument = nuxeoReader.read(new StringReader(nuxeoXmlDocument));
			
			// merge
			workflowRootDocument = getMergedXmlDocs(nuxeoDescriptorMetadatas, Nuxeo2XmlDescriptorMetadata.TYPE_NUXEO, nuxeoRootDocument, nsNuxeoMap, workflowRootDocument, nsWorkflowMap);
			
			

			// ******************************
			// the static translations
			
			// merge
			workflowRootDocument = getMergedXmlDocs(nuxeoDescriptorMetadatas, Nuxeo2XmlDescriptorMetadata.TYPE_STATIC, null, null, workflowRootDocument, nsWorkflowMap);
			
			
			
			
			
			// get XML from document
			String newXml = workflowRootDocument.asXML();
			xmlContent.append(newXml);
			if (log.isDebugEnabled())
				log.debug("mergeXmlDoc :: newXml == "+newXml);
		}
		
		catch (IOException e) {
			log.error("mergeXmlDoc :: IO error : documentRef : " + doc.getRef() + " metadataSchemaNamespace : "+ metadataSchemaNamespace,e);
			xmlContent.append(workflowXmlDocument);
		}
		catch (DocumentException e) {
			log.error("mergeXmlDoc :: IO error : documentRef : " + doc.getRef() + " metadataSchemaNamespace : "+ metadataSchemaNamespace,e);
			xmlContent.append(workflowXmlDocument);
		}
		catch (JaxenException e) {
			log.error("mergeXmlDoc :: IO error : documentRef : " + doc.getRef() + " metadataSchemaNamespace : "+ metadataSchemaNamespace,e);
			xmlContent.append(workflowXmlDocument);
		}
		
		if (log.isDebugEnabled())
			log.debug(xmlContent.toString());
		

		///////////////////////
		// debut bidouille TEF
		//return initWorkflowXmlDocument;
		// fin bidouille TEF
		///////////////////////
		
		
		return xmlContent.toString();
	}
	
	
	
	
	
	public Document getMergedXmlDocs(Nuxeo2XmlDescriptorMetadata[] nuxeoDescriptorMetadatas, String type, Document fromRootDocument, HashMap<String, String> nsFromMap, Document workflowRootDocument, HashMap<String, String> nsWorkflowMap) throws JaxenException{
		
		for(Nuxeo2XmlDescriptorMetadata nuxeoDescriptorMetadata : nuxeoDescriptorMetadatas) {
			
			try {
				if (type.equals(nuxeoDescriptorMetadata.getType())) {
				
					boolean addIfEmpty = Nuxeo2XmlDescriptorMetadata.CONDITION_EMPTY.equals(nuxeoDescriptorMetadata.getCondition());
					
					// get the final value
					List<String> elementValues = new ArrayList<String>();
					
					// extract element
					String fromXpath = nuxeoDescriptorMetadata.getFromXpath();
					if (fromXpath!=null) {
						if (log.isDebugEnabled())
							log.debug("getMergedXmlDocs :: fromXpath="+fromXpath);
						if (NUXEO_URL.equals(fromXpath)) {
							fromXpath = nuxeoIdXpath;
						}
						Dom4jXPath fromXpathExpression = new Dom4jXPath(fromXpath);
						fromXpathExpression.setNamespaceContext(new SimpleNamespaceContext(nsFromMap));
						List fromElements = fromXpathExpression.selectNodes(fromRootDocument);
						
						if (fromElements!=null) {
							for (Object fromElement : fromElements) {
								String elementValue = fromElement.toString();
								
								if (fromElement instanceof Element) {
									elementValue = ((Element)fromElement).getText();
								}
								else if (fromElement instanceof Attribute) {
									Attribute attrFromElement = (Attribute)fromElement;
									elementValue = attrFromElement.getText();
								}
								
								if (NUXEO_URL.equals(nuxeoDescriptorMetadata.getFromXpath())) {
									elementValue = nuxeoUrlConfig.getDocumentUrl() + elementValue;
									if (log.isDebugEnabled())
										log.debug("getMergedXmlDocs :: nuxeoUrlConfig="+nuxeoUrlConfig);
								}
								if (log.isDebugEnabled())
									log.debug("getMergedXmlDocs :: elementValue="+elementValue);
								
								if (elementValue!=null && !"".equals(elementValue))
									elementValues.add(elementValue);
							}
						}
					}
					else {
						elementValues.add("default");
					}
					
					// the used xpath
					String workflowXpath = nuxeoDescriptorMetadata.getWorkflowXpath();
					String workflowXpathRoot = nuxeoDescriptorMetadata.getWorkflowXpathRoot();
					
					
					// if we have elements to add
					if (elementValues!=null) {
					
						// if root workflow xpath doesn't exist, we create it
						if (workflowXpathRoot!=null) {
							Dom4jXPath workflowXpathRootExpression = new Dom4jXPath(workflowXpathRoot);
							if (log.isDebugEnabled())
								log.debug("getMergedXmlDocs :: workflowXpathRoot="+workflowXpathRoot);
							workflowXpathRootExpression.setNamespaceContext(new SimpleNamespaceContext(nsWorkflowMap));
							Object workflowRootElementObject = workflowXpathRootExpression.selectSingleNode(workflowRootDocument);
							if (log.isDebugEnabled())
								log.debug("getMergedXmlDocs :: workflowRootElementObject="+workflowRootElementObject);
							
							if (workflowRootElementObject == null) {
								Node workflowRootNode = XmlTool.createNodeFromXpath(null, workflowXpathRoot, workflowRootDocument);
								try {
									workflowRootDocument = new SAXReader().read(new StringReader(workflowRootNode.getDocument().asXML()));
								}
								catch(Exception e) {
									e.printStackTrace();
									log.error("getMergedXmlDocs :: "+e, e);
								}
								
								//workflowRootDocument = workflowRootNode.getDocument();
							}
							if (log.isDebugEnabled())
								log.debug("getMergedXmlDocs :: workflowRootDocument="+workflowRootDocument.asXML());
						}
						
						// the translations
						Nuxeo2XmlDescriptorMetadataTranslation[] translations = nuxeoDescriptorMetadata.getTranslations();
						
						// for each element
						for (String elementValue : elementValues) {
								
							// translation
							elementValue = translateValue(elementValue, translations);
							
							// search element into workflow xml
							Dom4jXPath workflowXpathExpression = new Dom4jXPath(workflowXpath);
							workflowXpathExpression.setNamespaceContext(new SimpleNamespaceContext(nsWorkflowMap));
							List workflowElementList = workflowXpathExpression.selectNodes(workflowRootDocument);
							
							if (log.isDebugEnabled())
								log.debug("getMergedXmlDocs :: workflowElementList="+workflowElementList);
							
							// for each result node
							boolean foundSameValue = false;
							int firstEmptyElementIndex = -1;
							boolean empty = true;
							for (int w=0; !foundSameValue && workflowElementList!=null && w<workflowElementList.size(); w++) {
								Object workflowElementObject = workflowElementList.get(w);
								
								if (workflowElementObject instanceof Element) {
									Element workflowElement = (Element)workflowElementObject;
									
									String workflowElementValue = workflowElement.getText();
									if (log.isDebugEnabled())
										log.debug("getMergedXmlDocs :: workflowElementValue="+workflowElementValue);
									
									if (workflowElementValue.equals(elementValue)) {
										foundSameValue = true;
									}
									if (firstEmptyElementIndex==-1 && workflowElementValue.equals("")) {
										firstEmptyElementIndex = w;
									}
									
									if (workflowElementValue!=null && !workflowElementValue.equals("")) {
										empty = false;
									}
								}
								else if (workflowElementObject instanceof Attribute) {
									Attribute workflowElement = (Attribute)workflowElementObject;
									
									String workflowElementValue = workflowElement.getText();
									if (log.isDebugEnabled())
										log.debug("getMergedXmlDocs :: workflowElementValue="+workflowElementValue);
									
									if (workflowElementValue.equals(elementValue)) {
										foundSameValue = true;
									}
									if (firstEmptyElementIndex==-1 && workflowElementValue.equals("")) {
										firstEmptyElementIndex = w;
									}
									
									if (workflowElementValue!=null && !workflowElementValue.equals("")) {
										empty = false;
									}
								}
							}
							
							// we add the value
							if (!foundSameValue && (!addIfEmpty || (addIfEmpty && empty))) {
								
								// we set the value in the empty node
								if (firstEmptyElementIndex!=-1) {
									
									Object workflowElementObject = workflowElementList.get(firstEmptyElementIndex);
									
									if (workflowElementObject instanceof Element) {
										Element workflowElement = (Element)workflowElementObject;
										if (nuxeoDescriptorMetadata.isCDATA()) {
											workflowElement.add(DocumentHelper.createCDATA(elementValue));
										}
										else {
											workflowElement.setText(elementValue.trim());
										}
									}
									else if (workflowElementObject instanceof Attribute) {
										Attribute workflowElement = (Attribute)workflowElementObject;
										workflowElement.setText(elementValue.trim());
									}
								}
								
								// we add a node
								else {
									if (log.isDebugEnabled())
										log.debug("getMergedXmlDocs :: add a node");
									
									// add the node
									if (log.isDebugEnabled()) {
										log.debug("workflowXpathRoot="+workflowXpathRoot);
										log.debug("workflowXpath="+workflowXpath);
									}
									//Node created = XmlTool.createNodeFromXpath(workflowXpath, workflowRootNode);
									Node created = XmlTool.createNodeFromXpath(workflowXpathRoot, workflowXpath, workflowRootDocument);
									
									if (nuxeoDescriptorMetadata.isCDATA() && created instanceof Element) {
										Element createdElement = (Element)created;
										createdElement.add(DocumentHelper.createCDATA(elementValue));
									}
									else {
										created.setText(elementValue.trim());
									}
									workflowRootDocument = created.getDocument();
								}
								
							}
						}
					}
				}
			}
			catch(Exception e) {
				log.error("getMergedXmlDocs :: "+e, e);
			}
		}
		
		return workflowRootDocument;
	}
	
	
	
	
	private String translateValue(String fromValue, Nuxeo2XmlDescriptorMetadataTranslation[] translations) {
		String toValue = null;
		
		Nuxeo2XmlDescriptorMetadataTranslation defaultTranslation = null;
		
		for (int i=0; translations!=null && toValue==null && i<translations.length; i++) {
			Nuxeo2XmlDescriptorMetadataTranslation translation = translations[i];
			
			if (fromValue.equals(translation.getFrom())) {
				toValue = translation.getTo();
			}
			
			if (translation.getFrom()==null) {
				defaultTranslation = translation;
			}
		}
		
		if (toValue==null) {
			
			if (defaultTranslation!=null) {
				return defaultTranslation.getTo();
			}
			else {			
				return fromValue;
			}
		}
		else {
			return toValue;
		}
	}
	
	
}
