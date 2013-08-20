package org.orioai.esupecm.nuxeo2xml.service;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.jaxen.JaxenException;
import org.jaxen.dom4j.Dom4jXPath;
import org.jaxen.expr.Expr;
import org.jaxen.expr.LocationPath;
import org.jaxen.expr.Predicate;
import org.jaxen.expr.Step;
import org.jaxen.saxpath.Axis;


public class XmlTool {

	private static final Log log = LogFactory.getLog(XmlTool.class);
	
	
	
	
	public static Node createNodeFromXpath(String rootXpath, String xpath, Document document) {
		
		//System.out.println("\ncreateNodeFromXpath :: rootXpath="+rootXpath);
		//System.out.println("createNodeFromXpath :: xpath="+xpath);
		
		String parentXpathRoot = null;
		if (rootXpath!=null) {
			LocationPath lpRoot=getLocationPath(rootXpath);
			List<Step> stepsRoot=lpRoot.getSteps();
			parentXpathRoot=getXPathFromSteps(stepsRoot, lpRoot.isAbsolute());
			//System.out.println("createNodeFromXpath :: parentXpathRoot="+parentXpathRoot);
		}
		
		org.dom4j.Node result=null;
		LocationPath lp=getLocationPath(xpath);
		
		if (lp!=null) {
			List<Step> steps=lp.getSteps();
			//System.out.println("\nsteps :: "+steps);
			Step lastStep=steps.remove(steps.size()-1);
			//System.out.println("lastStep :: "+lastStep);
			String parentXpath=getXPathFromSteps(steps, lp.isAbsolute());
			//System.out.println("parentXpath :: "+parentXpath);
			org.dom4j.Node parent=document.selectSingleNode(parentXpath);
			//System.out.println("parent :: "+parent);
			
			// if the parent of this parent is root,we have to add
			boolean haveToCreate = false;
			if (parent==null) {
				haveToCreate = true;
			}
			if (!haveToCreate && steps.size()>0) {
				steps.remove(steps.size()-1);
				String grandParentXpath=getXPathFromSteps(steps, lp.isAbsolute());
				//System.out.println("grandParentXpath :: "+grandParentXpath);
				
				if (parentXpathRoot!=null && parentXpathRoot.equals(grandParentXpath)) {
					haveToCreate = true;
				}
			}
			
			if (haveToCreate) {
				//System.out.println("create parent :: "+parentXpath);
				parent=createNodeFromXpath(rootXpath, parentXpath,document);
			}
			if (parent==null) {
				log.error("createNodeFromXpath :: can not create node from xpath");
			}
			else {
				//System.out.println("lastStep :: "+lastStep);
				
				String txt=lastStep.getText();
				//System.out.println("txt :: "+txt);
				String name=txt.substring(txt.indexOf("::")+"::".length());
				if (name.indexOf("[")!=-1) {
					name = name.substring(0, name.indexOf("["));
				}
				//System.out.println("name :: "+name);
				if (parent instanceof Element) {
					Element parentElement=(Element)parent;
					Element element = null;
					switch(lastStep.getAxis()) {
						case Axis.ATTRIBUTE:
							Attribute newAttribute=DocumentHelper.createAttribute(null, name, "");
							//System.out.println("createAttribute :: name="+name+", newAttribute="+newAttribute);
							parentElement.add(newAttribute);
							result=newAttribute;
							break;
						default:
							element=DocumentHelper.createElement(name);
							//System.out.println("createElement :: name="+name+", element="+element);
							parentElement.add(element);
							result=element;
							break;
					}
					
					// if predicates
					List<Predicate> predicates = lastStep.getPredicates();
					if (predicates!=null) {
						for (int i=0; i<predicates.size(); i++) {
							Predicate predicate = predicates.get(i);
							
							if (element!=null) {
								addPredicates(element, predicate);
							}
						}
					}
				}
				else {
					log.error("createNodeFromXpath :: can not create child :: xpath="+xpath);
				}
			}
		}
		else {
			log.error("createNodeFromXpath :: can not resolve xpath");
		}
		return result;
	}   
	
	
	
	private static void addPredicates(Element element, Predicate predicate) {
		//System.out.println("   element = "+element);
		
		String predicateText = predicate.getExpr().simplify().getText();
		if (predicateText.startsWith("(") && predicateText.endsWith(")")) {
			predicateText = predicateText.substring(1, predicateText.length()-1);
		}
		//System.out.println("   predicateText = "+predicateText);
		String[] predicateStepTexts = predicateText.split("/");
		for (int j=0; predicateStepTexts!=null && j<predicateStepTexts.length; j++) {
			String predicateStepText = predicateStepTexts[j];
			String predicateStepTextType = predicateStepText.substring(0, predicateStepTexts[j].indexOf("::"));
			String predicateStepTextValue = predicateStepText.substring(predicateStepTextType.length()+"::".length());
			//System.out.println("   predicateStepText = "+predicateStepText);
			//System.out.println("   predicateStepTextType = "+predicateStepTextType);
			//System.out.println("   predicateStepTextValue = "+predicateStepTextValue);
			
			String predicateStepTextAttributeName = null;
			String predicateStepTextAttributeValue = null;
			if (predicateStepTextValue.indexOf("=")!=-1) {
				predicateStepTextAttributeName = predicateStepTextValue.substring(0, predicateStepTextValue.indexOf("=")).trim();
				predicateStepTextAttributeValue = predicateStepTextValue.substring(predicateStepTextValue.indexOf("=")+1).trim();
				predicateStepTextAttributeValue = predicateStepTextAttributeValue.replaceAll("\"", "");
				predicateStepTextAttributeValue = predicateStepTextAttributeValue.replaceAll("'", "");
			}
			else {
				predicateStepTextAttributeName = predicateStepTextValue;
			}
			
			if ("child".equals(predicateStepTextType)) {
				element = element.addElement(predicateStepTextAttributeName);
				if (predicateStepTextAttributeValue!=null) {
					element.setText(predicateStepTextAttributeValue);
				}
			}
			else {
				//System.out.println("   predicateStepTextAttributeName = "+predicateStepTextAttributeName);
				//System.out.println("   predicateStepTextAttributeValue = "+predicateStepTextAttributeValue);
				
				element = element.addAttribute(predicateStepTextAttributeName, predicateStepTextAttributeValue);
			}
		}
		//System.out.println("   getLocationPath = "+getLocationPath("@language"));
		
	}
	
	
	
	private static LocationPath getLocationPath(String xpath) {
		LocationPath lp=null;
		try {
			Expr xpathExpr=new Dom4jXPath(xpath).getRootExpr();
			if (xpathExpr instanceof LocationPath) {
				lp=(LocationPath)xpathExpr;
			}
		}
		catch (JaxenException e) {
			log.error("getLocationPath :: "+e, e);
		}
		return lp;
	} 
	
	
	private static String getXPathFromSteps(List<Step> steps, boolean absolutePath) {
		//System.out.println("getXPathFromSteps :: steps="+steps);
		StringBuffer sb=new StringBuffer();
		if (absolutePath) {
			sb.append("/");
		}
		for (Step step : steps) {
			sb.append(step.getText()).append("/");
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();   
	}

}
