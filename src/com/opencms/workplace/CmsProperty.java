package com.opencms.workplace;

/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsProperty.java,v $
 * Date   : $Date: 2000/08/08 14:08:32 $
 * Version: $Revision: 1.18 $
 *
 * Copyright (C) 2000  The OpenCms Group 
 * 
 * This File is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * For further information about OpenCms, please see the
 * OpenCms Website: http://www.opencms.com
 * 
 * You should have received a copy of the GNU General Public License
 * long with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.opencms.file.*;
import com.opencms.core.*;
import com.opencms.util.*;
import com.opencms.template.*;

import javax.servlet.http.*;

import java.util.*;

/**
 * Template class for displaying the property screens of the OpenCms workplace.<P>
 * Reads template files of the content type <code>CmsXmlWpTemplateFile</code>.
 * 
 * @author Michael Emmerich
 * @version $Revision: 1.18 $ $Date: 2000/08/08 14:08:32 $
 */
public class CmsProperty extends CmsWorkplaceDefault implements I_CmsWpConstants,
															 I_CmsConstants {
		   

	 /**
	 * Gets all  propertydefintions for the file.
	 * <P>
	 * The given vectors <code>names</code> and <code>values</code> will 
	 * be filled with the appropriate information to be used for building
	 * a select box.
	 * 
	 * @param cms CmsObject Object for accessing system resources.
	 * @param names Vector to be filled with the appropriate values in this method.
	 * @param values Vector to be filled with the appropriate values in this method.
	 * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
	 * @return Index representing the current value in the vectors.
	 * @exception CmsException
	 */
	public Integer getAllPropertydef(CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values, Hashtable parameters) 
		throws CmsException {
	
		int retValue = -1;
		I_CmsSession session= cms.getRequestContext().getSession(true);
	   
		String filename=(String)session.getValue(C_PARA_FILE);       
		if (filename != null) {
			 CmsResource file=(CmsResource)cms.readFileHeader(filename);
			 CmsResourceType type=cms.getResourceType(file.getType());
			 // get all propertydefinitions for this type
			 Vector propertydef =cms.readAllPropertydefinitions(type.getResourceName());
		
			 Enumeration enu=propertydef.elements();
			 while (enu.hasMoreElements()) {
				CmsPropertydefinition prop=(CmsPropertydefinition)enu.nextElement();                         
				names.addElement(prop.getName());
		    	values.addElement(prop.getName());
			 }
		}
		
		// no current user, set index to -1
		return new Integer(retValue);
	}
	/**
	 * Overwrites the getContent method of the CmsWorkplaceDefault.<br>
	 * Gets the content of the property template and processed the data input.
	 * @param cms The CmsObject.
	 * @param templateFile The property template file
	 * @param elementName not used
	 * @param parameters Parameters of the request and the template.
	 * @param templateSelector Selector of the template tag to be displayed.
	 * @return Bytearre containgine the processed data of the template.
	 * @exception Throws CmsException if something goes wrong.
	 */
	public byte[] getContent(CmsObject cms, String templateFile, String elementName, 
							 Hashtable parameters, String templateSelector)
		throws CmsException {
		I_CmsSession session= cms.getRequestContext().getSession(true);
		
		// the template to be displayed
		String template=null;
 		
		// clear session values on first load
		String initial=(String)parameters.get(C_PARA_INITIAL);
		if (initial!= null) {
			// remove all session values
			session.removeValue(C_PARA_FILE);
			session.removeValue(C_PARA_PROPERTYDEF);
			session.removeValue("lasturl");
		}
									 
		String lasturl = getLastUrl(cms, parameters); 
		
		// get all parameters and put them into the session
		String filename=(String)parameters.get(C_PARA_FILE);
		if (filename != null) {
			session.putValue(C_PARA_FILE,filename);        
		}
		String propertydef =(String)parameters.get("selectproperty");
		if (propertydef != null) {
			session.putValue(C_PARA_PROPERTYDEF,propertydef);        
		} 
		
		filename=(String)session.getValue(C_PARA_FILE);
		propertydef=(String)session.getValue(C_PARA_PROPERTYDEF);    
		
		CmsResource file=(CmsResource)cms.readFileHeader(filename);
		
		String edit=(String)parameters.get("EDIT");
		String delete=(String)parameters.get("DELETE");
		String newproperty=(String)parameters.get("NEWPROPERTY");
		String newpropertydef=(String)parameters.get("ORGANIZE"); 
		
		
		// select the displayed template
		
		// check if the file is locked by the current user.
		// if so, display a different dialog with more functions is shown
		if (file.isLockedBy()==cms.getRequestContext().currentUser().getId()) {                       
			if (edit != null) {
				// display the edit property dialog  
				template="editproperty";
			} else if (delete != null) {
				// display the delete property dialog  
				template="deleteproperty";
			 } else if (newproperty != null) {
				// display the newproperty  dialog  
				template="newproperty";
			 } else if (newpropertydef != null) {
				// display the newpropertydef dialog  
				template="newpropertydef";
			} else {
				// set the default display
				template="ownlocked";      
			}
		} 
	  
		CmsXmlWpTemplateFile xmlTemplateDocument = new CmsXmlWpTemplateFile(cms,templateFile);
	 
		// now process the data taken form the dialog
		
		// edit was selected
		if (edit != null) {
			// check if a property was selected          
			if (propertydef != null) {
				xmlTemplateDocument.setData("PROPERTYDEF",propertydef);
				// check if a edited property is available
				String newValue=(String)parameters.get("EDITEDPROPERTY");
				if (newValue != null) {
					// update the property
					cms.writeProperty(filename,propertydef,newValue);
					template="ownlocked";    
					//session.removeValue(C_PARA_FILE);
					//session.removeValue(C_PARA_PROPERTYDEF);  
				}
			} else {
				template="ownlocked";    
			}
		}
		
		// delete was selected
		if (delete != null) {
			// check if the ok button was selected
			if (delete.equals("true")) {
				// delete the propertydefinition
				if (propertydef != null) {            
					cms.deleteProperty(filename,propertydef);
					template="ownlocked";    
					//session.removeValue(C_PARA_FILE);
					//session.removeValue(C_PARA_PROPERTYDEF);
				}
			}
		}
		
		// new property was selected
		if (newproperty != null) {
			// check if the ok button was selected
			if (newproperty.equals("true")) {
				String newValue=(String)parameters.get("NEWPROPERTYVALUE");
				if ((newValue != null) && (propertydef !=null)) {
					// test if this property is already existing
					String testValue=cms.readProperty(filename,propertydef);
					if (testValue == null) {
						// add the property                    
						cms.writeProperty(filename,propertydef,newValue);
						template="ownlocked";    
						//session.removeValue(C_PARA_FILE);
						//session.removeValue(C_PARA_PROPERTYDEF); 
					} else {
						// todo: add an error message that this key is already exisitng
					}
 
				} else {
				 template="ownlocked";    
				 //session.removeValue(C_PARA_FILE);
				 //session.removeValue(C_PARA_PROPERTYDEF);  
				}                           
			}
		}
		
		// new propertydef was selected
		if (newpropertydef != null) {
			 // check if the ok button was selected
			if (newpropertydef.equals("true")) {
				 String newValue=(String)parameters.get("NEWPROPERTYDEF");
				 if (newValue != null) {
					// test if this property is already existing
					String testValue=cms.readProperty(filename,propertydef);
					if (testValue == null) {
						// add the property   
						CmsResourceType type=cms.getResourceType(file.getType());
						CmsPropertydefinition def=cms.createPropertydefinition(newValue,
																		  type.getResourceName(),
																		  C_PROPERTYDEF_TYPE_NORMAL);
						cms.writePropertydefinition(def);
						template="ownlocked";    
						//session.removeValue(C_PARA_FILE);
						//session.removeValue(C_PARA_PROPERTYDEF);
					} else {
						// todo: add an error message that this key is already exisitng
					}
					 
				 } else {
				 template="ownlocked";    
				 //session.removeValue(C_PARA_FILE);
				 //session.removeValue(C_PARA_PROPERTYDEF); 
				 }
			}
			
		}
		
		 // set the required datablocks
		String title=cms.readProperty(file.getAbsolutePath(),C_PROPERTY_TITLE);
		if (title==null) {
			title="";
		}
		CmsUser owner=cms.readOwner(file);
		xmlTemplateDocument.setData("TITLE",title);
		xmlTemplateDocument.setData("STATE",getState(cms,file,new CmsXmlLanguageFile(cms)));
		xmlTemplateDocument.setData("OWNER",owner.getFirstname()+" "+owner.getLastname()+"("+owner.getName()+")");
		xmlTemplateDocument.setData("GROUP",cms.readGroup(file).getName());
		xmlTemplateDocument.setData("FILENAME",file.getName());
		xmlTemplateDocument.setData("lasturl", lasturl);

		// process the selected template 
		return startProcessing(cms,xmlTemplateDocument,"",parameters,template);
	}
	 /**
	 * Gets all property the file.
	 * <P>
	 * The given vectors <code>names</code> and <code>values</code> will 
	 * be filled with the appropriate information to be used for building
	 * a select box.
	 * 
	 * @param cms CmsObject Object for accessing system resources.
	 * @param names Vector to be filled with the appropriate values in this method.
	 * @param values Vector to be filled with the appropriate values in this method.
	 * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
	 * @return Index representing the current value in the vectors.
	 * @exception CmsException
	 */
	public Integer getProperty(CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values, Hashtable parameters) 
		throws CmsException {
	
		int retValue = -1;
		I_CmsSession session= cms.getRequestContext().getSession(true);
	   
		String filename=(String)session.getValue(C_PARA_FILE);       
		if (filename != null) {
			 Hashtable property = cms.readAllProperties(filename);
	
			 Enumeration enu=property.keys();
			 while (enu.hasMoreElements()) {
				String key=(String)enu.nextElement();
				String value=(String)property.get(key);
	   
				names.addElement(key+":"+value);
		    	values.addElement(key);
			 }
		    
		}
		
		// no current user, set index to -1
		return new Integer(retValue);
	}
	 /**
	 * Gets all unused propertydefintions for the file.
	 * <P>
	 * The given vectors <code>names</code> and <code>values</code> will 
	 * be filled with the appropriate information to be used for building
	 * a select box.
	 * 
	 * @param cms CmsObject Object for accessing system resources.
	 * @param names Vector to be filled with the appropriate values in this method.
	 * @param values Vector to be filled with the appropriate values in this method.
	 * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
	 * @return Index representing the current value in the vectors.
	 * @exception CmsException
	 */
	public Integer getPropertydef(CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values, Hashtable parameters) 
		throws CmsException {
	
		int retValue = -1;
		I_CmsSession session= cms.getRequestContext().getSession(true);
	   
		String filename=(String)session.getValue(C_PARA_FILE);       
		if (filename != null) {
			 CmsResource file=(CmsResource)cms.readFileHeader(filename);
			 CmsResourceType type=cms.getResourceType(file.getType());
			 // get all propertydefinitions for this type
			 Vector propertydef =cms.readAllPropertydefinitions(type.getResourceName());
			 // get all existing properties of this file
			 Hashtable property = cms.readAllProperties(filename);   
	
			 Enumeration enu=propertydef.elements();
			 while (enu.hasMoreElements()) {
				CmsPropertydefinition prop=(CmsPropertydefinition)enu.nextElement();
				 
				String propertyvalue=(String)property.get(prop.getName());
				if (propertyvalue == null ) {                  
					names.addElement(prop.getName());
		    	    values.addElement(prop.getName());
				}
			 }
		}
		
		// no current user, set index to -1
		return new Integer(retValue);
	}
	 /**
	 * Gets the value of selected property and sets it in the input field of the dialog.
	 * This method is directly called by the content definiton.
	 * @param Cms The CmsObject.
	 * @param lang The language file.
	 * @param parameters User parameters.
	 * @return Value that is set into the input field.
	 * @exception CmsExeption if something goes wrong.
	 */
	public String getPropertyValue(CmsObject cms, CmsXmlLanguageFile lang, Hashtable parameters)
		throws CmsException {
		
		String propertyValue=null;
		
		I_CmsSession session= cms.getRequestContext().getSession(true);
		
		// get the filename
		String filename=(String)session.getValue(C_PARA_FILE);        
		if (filename != null) {
			 //get the propertydefinition
			String propertydef=(String)session.getValue(C_PARA_PROPERTYDEF);  
			if (propertydef != null) {
				// everything is there, so try to read the meteainfo
				propertyValue=cms.readProperty(filename,propertydef);
				if (propertyValue == null) {
					propertyValue="";
				}   
			}            
		}        
	  return propertyValue;
	}
	 /**
	 * Gets a formated file state string.
	 * @param cms The CmsObject.
	 * @param file The CmsResource.
	 * @param lang The content definition language file.
	 * @return Formated state string.
	 */
	 private String getState(CmsObject cms, CmsResource file,CmsXmlLanguageFile lang)
		 throws CmsException {
		 StringBuffer output=new StringBuffer();
		 
		 if (file.inProject(cms.getRequestContext().currentProject())) {
			int state=file.getState();
			output.append(lang.getLanguageValue("explorer.state"+state));
		 } else {
			output.append(lang.getLanguageValue("explorer.statenip"));
		 }
		 return output.toString();
	 } 
	 /**
	 * Indicates if the results of this class are cacheable.
	 * 
	 * @param cms CmsObject Object for accessing system resources
	 * @param templateFile Filename of the template file 
	 * @param elementName Element name of this template in our parent template.
	 * @param parameters Hashtable with all template class parameters.
	 * @param templateSelector template section that should be processed.
	 * @return <EM>true</EM> if cacheable, <EM>false</EM> otherwise.
	 */
	public boolean isCacheable(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
		return false;
	}
}
