package com.opencms.workplace;

/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsPreferencesPanels.java,v $
 * Date   : $Date: 2000/08/08 14:08:32 $
 * Version: $Revision: 1.22 $
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
 * Template class for displaying the preference panels screen of the OpenCms workplace.<P>
 * Reads template files of the content type <code>CmsXmlWpTemplateFile</code>.
 * 
 * TODO: use predefined constants in this class, clean up this class and add more comments!
 * 
 * @author Michael Emmerich
 * @version $Revision: 1.22 $ $Date: 2000/08/08 14:08:32 $
 */
public class CmsPreferencesPanels extends CmsWorkplaceDefault implements I_CmsWpConstants,
																		 I_CmsConstants {
		   
	/** Datablock value for checked */    
	private static final String C_CHECKED = "CHECKED";
	 
	/** Datablock value for checkbox title */    
	private static final String C_CHECKTITLE = "CHECKTITLE";
	
	/** Datablock value for checkbox type */    
	private static final String C_CHECKTYPE = "CHECKTYPE";
   
	/** Datablock value for checkbox changed */    
	private static final String C_CHECKCHANGED = "CHECKCHANGED";
	
	/** Datablock value for checkbox size */    
	private static final String C_CHECKSIZE = "CHECKSIZE";
	
	/** Datablock value for checkbox state */    
	private static final String C_CHECKSTATE = "CHECKSTATE";
	
	/** Datablock value for checkbox owner */    
	private static final String C_CHECKOWNER = "CHECKOWNER";
	
	/** Datablock value for checkbox group */    
	private static final String C_CHECKGROUP = "CHECKGROUP";
	
	/** Datablock value for checkbox access */    
	private static final String C_CHECKACCESS = "CHECKACCESS";
	
	/** Datablock value for checkbox lockedby */    
	private static final String C_CHECKLOCKEDBY = "CHECKLOCKEDBY";
  
	/** Datablock value for checkbox view all */    
	private static final String C_CHVIEWALL = "CHVIEWALL";
	
	/** Datablock value for checkbox message accepted */    
	private static final String C_CHMESSAGEACCEPTED = "CHMESSAGEACCEPTED";
  
	/** Datablock value for checkbox message forwared */    
	private static final String C_CHMESSAGEFORWARDED = "CHMESSAGEFORWARDED";
	
	/** Datablock value for checkbox message completed */    
	private static final String C_CHMESSAGECOMPLETED = "CHMESSAGECOMPLETED";
	  
	/** Datablock value for checkbox message memebers */    
	private static final String C_CHMESSAGEMEMEBERS = "CHMESSAGEMEMEBERS";
   
	/** Datablock value for checkbox user read */    
	private static final String C_CHECKUR = "CHECKUR";
   
	 /** Datablock value for checkbox user write */    
	private static final String C_CHECKUW = "CHECKUW";
	
	/** Datablock value for checkbox user visible */    
	private static final String C_CHECKUV = "CHECKUV";
	
	/** Datablock value for checkbox group read */    
	private static final String C_CHECKGR = "CHECKGR";
   
	/** Datablock value for checkbox group write */    
	private static final String C_CHECKGW = "CHECKGW";
	
	/** Datablock value for checkbox group visible */    
	private static final String C_CHECKGV = "CHECKGV";
	
	/** Datablock value for checkbox public read */    
	private static final String C_CHECKPR = "CHECKPR";
   
	/** Datablock value for checkbox public write */    
	private static final String C_CHECKPW = "CHECKPW";
	
	/** Datablock value for checkbox public visible */    
	private static final String C_CHECKPV = "CHECKPV";
	
	/** Datablock value for checkbox internal flag */    
	private static final String C_CHECKIF = "CHECKIF";
	
	
	/** Constant for filter */
	private static final String C_TASK_FILTER = "task.filter.";

	/** Constant for filter */
	private static final String C_SPACER = "------------------------------------------------";
	    
	/** Vector storing all view names */
	Vector m_viewNames = null;

	/** Vector storing all view values */
	Vector m_viewLinks = null;
	
	/**
	 * Overwrites the getContent method of the CmsWorkplaceDefault.<br>
	 * Gets the content of the preferences panels template and processed the data input.
	 * @param cms The CmsObject.
	 * @param templateFile The preferences panels template file
	 * @param elementName not used
	 * @param parameters Parameters of the request and the template.
	 * @param templateSelector Selector of the template tag to be displayed.
	 * @return Bytearre containgine the processed data of the template.
	 * @exception Throws CmsException if something goes wrong.
	 */
	public byte[] getContent(CmsObject cms, String templateFile, String elementName, 
							 Hashtable parameters, String templateSelector)
		throws CmsException {
		
		// get request context and session
		CmsRequestContext reqCont = cms.getRequestContext();     
		I_CmsSession session= cms.getRequestContext().getSession(true);
		
		// define varialbes to store template and panel
		String template="";
		String panel;
		String oldPanel;
		
		int explorerSettingsValue;            
		
		// clear session values on first load
		String initial=(String)parameters.get(C_PARA_INITIAL);
		if (initial!= null) {
			// remove all session values
			session.removeValue("EXPLORERSETTINGS");
			session.removeValue("TASKSETTINGS");
			session.removeValue("USERSETTINGS");
			session.removeValue("STARTSETTINGS");
			session.removeValue(C_PARA_OLDPANEL);   
			session.removeValue("lasturl");
		}
		
		CmsXmlWpTemplateFile xmlTemplateDocument = new CmsXmlWpTemplateFile(cms,templateFile);          
		
		// get the active panel value. This indicates which panel to be displayed.
		panel=(String)parameters.get( C_PARA_PANEL);  
		
		// check if the submit or ok button is selected. If so, update all values
		if ((parameters.get(C_PARA_SUBMIT) != null) || (parameters.get(C_PARA_OK) != null) ){
			
			// get the data values form the active panel and store them in the session.
			// this is nescessary to save this data in the next step.
			if (panel!= null) {
				
				// the active panel is the explorer settings, save its data
				if (panel.equals(C_PANEL_EXPLORER)) {
					int explorerSettings=getExplorerSettings(parameters);
				  
					session.putValue(C_PARA_EXPLORERSETTINGS,new Integer(explorerSettings).toString());        
				}
				
				// the active panel is the task settings, save its data
				if (panel.equals(C_PANEL_TASK)) {
					Hashtable taskSettings=getTaskSettings(parameters,session);
					if (taskSettings != null) {
					  
						session.putValue(C_PARA_TASKSETTINGS,taskSettings);
					}
				}
				
				// the active panel is the starup settings, save its data
				if (panel.equals(C_PANEL_START)) {
					Hashtable startSettings=getStartSettings(cms,parameters);
					if (startSettings != null) {
						session.putValue(C_PARA_STARTSETTINGS,startSettings);
					}
				}
				
				// the active panel is the user settings, save its data
				if (panel.equals(C_PANEL_USER)) {
				   /* String userSettings=getUserSettings(parameters);
					if (userSettings != null) {
					   
						session.putValue(C_PARA_USERSETTINGS,userSettings);
					}*/
				}                       
			}
			
			// update the actual user data with those values taken from the preferences
			// panles. All data set in the panels is now available in the session, so
			// check which data is available and set it.
			
			// now update the explorer settings
			String explorerSettings=(String)session.getValue(C_PARA_EXPLORERSETTINGS);
			if (explorerSettings != null) {
				reqCont.currentUser().setAdditionalInfo(C_ADDITIONAL_INFO_EXPLORERSETTINGS,explorerSettings);
			}
		 
			// now update the task settings
			Hashtable taskSettings=(Hashtable)session.getValue(C_PARA_TASKSETTINGS);
			if (taskSettings != null) {
				reqCont.currentUser().setAdditionalInfo(C_ADDITIONAL_INFO_TASKSETTINGS,taskSettings);
			}
			
			// now update the start settings
			Hashtable startSettings=(Hashtable)session.getValue(C_PARA_STARTSETTINGS);
			if (startSettings != null) {
				cms.getRequestContext().currentUser().setAdditionalInfo(C_ADDITIONAL_INFO_STARTSETTINGS,startSettings);
				String defaultGroup=(String)startSettings.get(C_START_DEFAULTGROUP);
				reqCont.currentUser().setDefaultGroup(cms.readGroup(defaultGroup));
			}
			
			// now update the user settings
			String userSettings=(String)session.getValue(C_PARA_USERSETTINGS);
			if (userSettings!= null) {
			   // reqCont.setCurrentGroup(userSettings);
			}
			
			// finally store the updated user object in the database  
			cms.writeUser(reqCont.currentUser());
		}

		// check which panel of the preferences should be displayed and update the
		// data on this panel with the default values or values already set in the
		// preferences before.
		if (panel != null) {
			
			// set the template to the active panel.
			// by doing this, the correct panel will be displayed when the template 
			// is processed.
			template=panel;
			
			if (panel.equals(C_PANEL_EXPLORER)) { 
			   // this is the panel for setting the explorer preferences
			   setExplorerSettings(session,parameters,reqCont,xmlTemplateDocument);         
			} else if (panel.equals(C_PANEL_TASK)) {     
			   // this is the panel for setting the task preferences
			   setTaskSettings(session,parameters,reqCont,xmlTemplateDocument);                     
			} else if (panel.equals(C_PANEL_START)) {     
			   // this is the panel for setting the start preferences
			   setStartSettings(session,parameters,reqCont,xmlTemplateDocument);                            
			} else if (panel.equals(C_PANEL_USER)) { 
			   // this is the panel for setting the user preferences
			   setUserSettings(session,parameters,reqCont,xmlTemplateDocument);                  
			}
				// finally store the given data into the session
				oldPanel=(String)session.getValue(C_PARA_OLDPANEL);
				if (oldPanel != null) {
					// the previous panel was the explorer panel, save all the data form there
					if (oldPanel.equals("explorer")) {
						int explorerSettings=getExplorerSettings(parameters);  
					
						session.putValue("EXPLORERSETTINGS",new Integer(explorerSettings).toString());
					}
					// the previous panel was the task panel, save all the data form there
					if (oldPanel.equals("task")) {
						Hashtable taskSettings=getTaskSettings(parameters,session);
						if (taskSettings != null) {
					
							session.putValue("TASKSETTINGS",taskSettings);    
						}
					}
					// the previous panel was the user panel, save all the data form there
					if (oldPanel.equals("user")) {  
					   /* String userSettings=getUserSettings(parameters);
						if (userSettings != null) {
				  
							session.putValue("USERSETTINGS",userSettings);
						}*/
					}
					// the previous panel was the start panel, save all the data form there
					if (oldPanel.equals("start")) {  
						 Hashtable startSettings=getStartSettings(cms,parameters);
						 if (startSettings != null) {
							session.putValue("STARTSETTINGS",startSettings);
						}
					}
				}

			session.putValue(C_PARA_OLDPANEL,panel);
		}
		
		// if the OK or cancel buttons are pressed return to the explorer and clear
		// the data in the session.
		if ((parameters.get("OK") != null) || (parameters.get("CANCEL") != null) ){
  
			session.removeValue("EXPLORERSETTINGS");
			session.removeValue("TASKSETTINGS");
			session.removeValue("USERSETTINGS");
			session.removeValue("STARTSETTINGS");
			session.removeValue(C_PARA_OLDPANEL);
   
			try {
				   cms.getRequestContext().getResponse().sendCmsRedirect( getConfigFile(cms).getWorkplaceActionPath()+C_WP_RELOAD);
				} catch (Exception e) {
					throw new CmsException("Redirect fails :"+ getConfigFile(cms).getWorkplaceActionPath()+C_WP_RELOAD,CmsException.C_UNKNOWN_EXCEPTION,e);
				}   
			return null;
		}
					
		return startProcessing(cms,xmlTemplateDocument,"",parameters,template);
	}
	 /**
	 * Gets all groups of the currently logged in user.
	 * <P>
	 * The given vectors <code>names</code> and <code>values</code> will 
	 * be filled with the appropriate information to be used for building
	 * a select box.
	 * <P>
	 * Both <code>names</code> and <code>values</code> will contain
	 * the group names after returning from this method.
	 * <P>
	 * 
	 * @param cms CmsObject Object for accessing system resources.
	 * @param lang reference to the currently valid language file
	 * @param names Vector to be filled with the appropriate values in this method.
	 * @param values Vector to be filled with the appropriate values in this method.
	 * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
	 * @return Index representing the user's current group in the vectors.
	 * @exception CmsException
	 */
	public Integer getDefaultGroup(CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values, Hashtable parameters) 
			throws CmsException {

		CmsRequestContext reqCont = cms.getRequestContext();
		I_CmsSession session = cms.getRequestContext().getSession(true);
	   
		String group=null;
	   
		// Get a vector of all of the user's groups by asking the request context
		CmsGroup currentGroup = reqCont.currentUser().getDefaultGroup();
		Vector allGroups = cms.getGroupsOfUser(reqCont.currentUser().getName());
				
		// try to get an existing value for the default value
		Hashtable startSettings=null;
		startSettings=(Hashtable)session.getValue("STARTSETTINGS");
		// if this fails, get the settings from the user obeject
		if (startSettings== null) {
			startSettings=(Hashtable)reqCont.currentUser().getAdditionalInfo(C_ADDITIONAL_INFO_STARTSETTINGS);                    
		}      
		if (startSettings != null) {
			group = (String)startSettings.get(C_START_DEFAULTGROUP);
		}
		if (group == null) {
			   group=currentGroup.getName();
		}
		
		// Now loop through all groups and fill the result vectors
		int numGroups = allGroups.size();
		int currentGroupNum = 0;
		for(int i=0; i<numGroups; i++) {
			CmsGroup loopGroup = (CmsGroup)allGroups.elementAt(i);
			String loopGroupName = loopGroup.getName();
			values.addElement(loopGroupName);
			names.addElement(loopGroupName);
			if(loopGroup.getName().equals(group)) {
				// Fine. The group of this loop is the user's current group. Save it!
				currentGroupNum = i;
			}
		}
		return new Integer(currentGroupNum);
	}
	/**
	 * Calculates the settings for the explorer filelist from the data submitted in
	 * the preference explorer panel.
	 * @param parameters Hashtable containing all request parameters
	 * @return Explorer filelist flags.
	 */
	private int getExplorerSettings(Hashtable parameters) {
		int explorerSettings=C_FILELIST_NAME;
		if (parameters.get("CBTITLE")!= null) {
			explorerSettings+=C_FILELIST_TITLE;
		}
		if (parameters.get("CBTYPE")!= null) {
			explorerSettings+=C_FILELIST_TYPE;
		}
		if (parameters.get("CBCHANGED")!= null) {
			explorerSettings+=C_FILELIST_CHANGED;
		}
		if (parameters.get("CBSIZE")!= null) {
			explorerSettings+=C_FILELIST_SIZE;
		}
		if (parameters.get("CBSTATE")!= null) {
			explorerSettings+=C_FILELIST_STATE;
		}
		if (parameters.get("CBOWNER")!= null) {
			explorerSettings+=C_FILELIST_OWNER;
		}
		if (parameters.get("CBGROUP")!= null) {
			explorerSettings+=C_FILELIST_GROUP;
		}
		if (parameters.get("CBACCESS")!= null) {
			explorerSettings+=C_FILELIST_ACCESS;
		}
		if (parameters.get("CBLOCKEDBY")!= null) {
			explorerSettings+=C_FILELIST_LOCKED;
		}
		return explorerSettings;
	}
	 /**
	 * Gets all filters available in the task screen.
	 * <P>
	 * The given vectors <code>names</code> and <code>values</code> will 
	 * be filled with the appropriate information to be used for building
	 * a select box.
	 * 
	 * @param cms CmsObject Object for accessing system resources.
	 * @param lang reference to the currently valid language file
	 * @param names Vector to be filled with the appropriate values in this method.
	 * @param values Vector to be filled with the appropriate values in this method.
	 * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
	 * @return Index representing the user's current filter view in the vectors.
	 * @exception CmsException
	 */
	public Integer getFilters(CmsObject cms, CmsXmlLanguageFile lang, Vector values, Vector names, Hashtable parameters) 
		throws CmsException {
		
		// Let's see if we have a session
		I_CmsSession session = cms.getRequestContext().getSession(true);
	  
		String filter= null;
		// try to get the default value
		Hashtable taskSettings=null;
		taskSettings=(Hashtable)session.getValue("TASKSETTINGS");
		// if this fails, get the settings from the user obeject
		if (taskSettings== null) {
			taskSettings=(Hashtable)cms.getRequestContext().currentUser().getAdditionalInfo(C_ADDITIONAL_INFO_TASKSETTINGS);                    
		}
		if (taskSettings != null) {
			filter = (String)taskSettings.get(C_TASK_FILTER);  
		} else {        
			filter = (String)session.getValue(C_SESSION_TASK_FILTER);
		}
	   
		int selected = 0;
		
		names.addElement("a1");
		values.addElement(lang.getLanguageValue(C_TASK_FILTER + "a1"));
		if("a1".equals(filter)) {
			selected = 0;
		}
		names.addElement("b1");
		values.addElement(lang.getLanguageValue(C_TASK_FILTER + "b1"));
		if("b1".equals(filter)) {
			selected = 1;
		}
		names.addElement("c1");
		values.addElement(lang.getLanguageValue(C_TASK_FILTER + "c1"));
		if("c1".equals(filter)) {
			selected = 2;
		}

		names.addElement("-");
		values.addElement(C_SPACER);		
		
		names.addElement("a2");
		values.addElement(lang.getLanguageValue(C_TASK_FILTER + "a2"));
		if("a2".equals(filter)) {
			selected = 4;
		}
		names.addElement("b2");
		values.addElement(lang.getLanguageValue(C_TASK_FILTER + "b2"));
		if("b2".equals(filter)) {
			selected = 5;
		}
		names.addElement("c2");
		values.addElement(lang.getLanguageValue(C_TASK_FILTER + "c2"));
		if("c2".equals(filter)) {
			selected = 6;
		}
		
		names.addElement("-");
		values.addElement(C_SPACER);		

		names.addElement("a3");
		values.addElement(lang.getLanguageValue(C_TASK_FILTER + "a3"));
		if("a3".equals(filter)) {
			selected = 8;
		}
		names.addElement("b3");
		values.addElement(lang.getLanguageValue(C_TASK_FILTER + "b3"));
		if("b3".equals(filter)) {
			selected = 9;
		}
		names.addElement("c3");
		values.addElement(lang.getLanguageValue(C_TASK_FILTER + "c3"));
		if("c3".equals(filter)) {
			selected = 10;
		}
		
		names.addElement("-");
		values.addElement(C_SPACER);		

		names.addElement("d1");
		values.addElement(lang.getLanguageValue(C_TASK_FILTER + "d1"));
		if("d1".equals(filter)) {
			selected = 12;
		}
		names.addElement("d2");
		values.addElement(lang.getLanguageValue(C_TASK_FILTER + "d2"));
		if("d2".equals(filter)) {
			selected = 13;
		}
		names.addElement("d3");
		values.addElement(lang.getLanguageValue(C_TASK_FILTER + "d3"));
		if("d3".equals(filter)) {
			selected = 14;
		}
		
		return(new Integer(selected));
	}
	  /**
	 * Gets all groups of the currently logged in user.
	 * <P>
	 * The given vectors <code>names</code> and <code>values</code> will 
	 * be filled with the appropriate information to be used for building
	 * a select box.
	 * <P>
	 * Both <code>names</code> and <code>values</code> will contain
	 * the group names after returning from this method.
	 * <P>
	 * 
	 * @param cms CmsObject Object for accessing system resources.
	 * @param lang reference to the currently valid language file
	 * @param names Vector to be filled with the appropriate values in this method.
	 * @param values Vector to be filled with the appropriate values in this method.
	 * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
	 * @return Index representing the user's current group in the vectors.
	 * @exception CmsException
	 */
	public Integer getGroups(CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values, Hashtable parameters) 
			throws CmsException {

		CmsRequestContext reqCont = cms.getRequestContext();
		I_CmsSession session = cms.getRequestContext().getSession(true);
		String group=(String)session.getValue("USERSETTINGS");
	   
		// Get a vector of all of the user's groups by asking the request context
		CmsGroup currentGroup = reqCont.currentGroup();
		Vector allGroups = cms.getGroupsOfUser(reqCont.currentUser().getName());
		 
		if (group == null) {
			   group=currentGroup.getName();
		}
		
		// Now loop through all groups and fill the result vectors
		int numGroups = allGroups.size();
		int currentGroupNum = 0;
		for(int i=0; i<numGroups; i++) {
			CmsGroup loopGroup = (CmsGroup)allGroups.elementAt(i);
			String loopGroupName = loopGroup.getName();
			values.addElement(loopGroupName);
			names.addElement(loopGroupName);
			if(loopGroup.getName().equals(group)) {
				// Fine. The group of this loop is the user's current group. Save it!
				currentGroupNum = i;
			}
		}
		return new Integer(currentGroupNum);
	}
	 /**
	 * Gets all available langages in the system.
	 * <P>
	 * The given vectors <code>names</code> and <code>values</code> will 
	 * be filled with the appropriate information to be used for building
	 * a select box.
	 * @param cms CmsObject Object for accessing system resources.
	 * @param lang reference to the currently valid language file
	 * @param names Vector to be filled with the appropriate values in this method.
	 * @param values Vector to be filled with the appropriate values in this method.
	 * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
	 * @return Index representing the user's current group in the vectors.
	 * @exception CmsException
	 */
	public Integer getLanguageFiles(CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values, Hashtable parameters) 
			throws CmsException {
		 
		CmsXmlWpConfigFile conf=new CmsXmlWpConfigFile(cms);
		
		Vector allLangFiles = null;
		// get all folders with language files
		Vector allLangFolders = cms.getSubFolders(conf.getLanguagePath());
	
		String langName=null;
		Hashtable startSettings=null;
		I_CmsSession session = cms.getRequestContext().getSession(true);
	   
		startSettings=(Hashtable)session.getValue("STARTSETTINGS");
		// if this fails, get the settings from the user obeject
		if (startSettings== null) {
			startSettings=(Hashtable)cms.getRequestContext().currentUser().getAdditionalInfo(C_ADDITIONAL_INFO_STARTSETTINGS);                    
		}
		if (startSettings != null) {
			langName = (String)startSettings.get(C_START_LANGUAGE);  
		} else {        
			langName = C_DEFAULT_LANGUAGE;
		}
		
		int select=0;
		// now go through all language files and add their name and reference to the
		// output vectors
		for (int i=0; i<allLangFolders.size(); i++) {
			
			CmsFolder folder = (CmsFolder)allLangFolders.elementAt(i);
			allLangFiles = cms.getFilesInFolder(folder.getAbsolutePath());
			CmsFile file=(CmsFile)allLangFiles.elementAt(0);
			CmsXmlLanguageFile langFile=new CmsXmlLanguageFile(cms,file.getAbsolutePath());
			names.addElement(langFile.getDataValue("name"));
			values.addElement(folder.getName()); 
			if (folder.getName().equals(langName)) {
				select=i;
			}

		}     
		return new Integer(select);
	}
	 /**
	 * Gets all projects of the currently logged in user.
	 * <P>
	 * The given vectors <code>names</code> and <code>values</code> will 
	 * be filled with the appropriate information to be used for building
	 * a select box.
	 * <P>
	 * Both <code>names</code> and <code>values</code> will contain
	 * the project names after returning from this method.
	 * <P>
	 * 
	 * @param cms CmsObject Object for accessing system resources.
	 * @param lang reference to the currently valid language file
	 * @param names Vector to be filled with the appropriate values in this method.
	 * @param values Vector to be filled with the appropriate values in this method.
	 * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
	 * @return Index representing the user's current project in the vectors.
	 * @exception CmsException
	 */
	public Integer getProjects(CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values, Hashtable parameters) 
			throws CmsException {
		// Get all project information
		CmsRequestContext reqCont = cms.getRequestContext();
		I_CmsSession session = cms.getRequestContext().getSession(true);

		Integer currentProject=null;
		Vector allProjects = cms.getAllAccessibleProjects();
	   
		Hashtable startSettings=null;
 
		startSettings=(Hashtable)session.getValue("STARTSETTINGS");
		// if this fails, get the settings from the user obeject
		if (startSettings== null) {
			startSettings=(Hashtable)reqCont.currentUser().getAdditionalInfo(C_ADDITIONAL_INFO_STARTSETTINGS);         
		}
		
		if (startSettings != null) {
		  currentProject = (Integer)startSettings.get(C_START_PROJECT);
		}
		
		// no project available in the user info, check out the current session
		if (currentProject == null) {
			 currentProject = new Integer( reqCont.currentProject().getId() );
		}  
			  
		// Now loop through all projects and fill the result vectors
		int numProjects = allProjects.size();
		int currentProjectNum = 0;
		for(int i=0; i<numProjects; i++) {
			CmsProject loopProject = (CmsProject)allProjects.elementAt(i);
			String loopProjectName = loopProject.getName();
			String loopProjectNameId = loopProject.getId() + "";
			values.addElement(loopProjectNameId);
			names.addElement(loopProjectName);
			if(loopProjectNameId.equals(currentProject + "")) {
				// Fine. The project of this loop is the user's current project. Save it!
				currentProjectNum = i;
			}
		}
		return new Integer(currentProjectNum);
	}
	/**
	* Calculates the start settings from the data submitted in
	* the preference task panel.
	* @param parameters Hashtable containing all request parameters
	* @return Hashtable containing the start settings.
	*/
	private Hashtable getStartSettings(CmsObject cms,Hashtable parameters) 
		throws CmsException {
		
		   Hashtable startSettings=new Hashtable();
	   
		   startSettings.put(C_START_LANGUAGE,(String)parameters.get("LANGUAGE"));
		   startSettings.put(C_START_PROJECT, new Integer(Integer.parseInt((String)parameters.get("project")))); 
		   startSettings.put(C_START_VIEW,(String)parameters.get("view"));
		   startSettings.put(C_START_DEFAULTGROUP,(String)parameters.get("dgroup"));
		   cms.getRequestContext().setCurrentProject( Integer.parseInt( (String)parameters.get("project") ) );
		   // get all access flags from the request
		   String ur=(String)parameters.get("ur");
		   String uw=(String)parameters.get("uw");
		   String uv=(String)parameters.get("uv");
		   String gr=(String)parameters.get("gr");
		   String gw=(String)parameters.get("gw");
		   String gv=(String)parameters.get("gv");
		   String pr=(String)parameters.get("pr");
		   String pw=(String)parameters.get("pw");
		   String pv=(String)parameters.get("pv");
		   String ir=(String)parameters.get("ir");
		   
		   int flag=0;
		   // now check and set all flags
		   if (ur!= null) {
			if (ur.equals("on")){
				flag+=C_ACCESS_OWNER_READ;
			}
		   }
		   if (uw != null) {
			if (uw.equals("on")){
				flag+=C_ACCESS_OWNER_WRITE;
			}
		   }           
		   if (uv != null) {
			if (uv.equals("on")){
				flag+=C_ACCESS_OWNER_VISIBLE;
			}
		   }     
		   if (gr != null) {
			 if (gr.equals("on")){
				flag+=C_ACCESS_GROUP_READ;
			 }
		   }
		   if (gw != null) {
			if (gw.equals("on")){
				flag+=C_ACCESS_GROUP_WRITE;
			}
		   }           
		   if (gv  != null) {
			if (gv.equals("on")){
				flag+=C_ACCESS_GROUP_VISIBLE;
			}
		   }   
		   if (pr != null) {
			if (pr.equals("on")){
				flag+=C_ACCESS_PUBLIC_READ;
			}
		   }
		   if (pw != null) {
			if (pw.equals("on")){
				flag+=C_ACCESS_PUBLIC_WRITE;
			}
		   }           
		   if (pv  != null) {
			if (pv.equals("on")){
				flag+=C_ACCESS_PUBLIC_VISIBLE;
			}
		   }  
		   if (ir != null) {
			if (ir.equals("on")){                        
				flag+=C_ACCESS_INTERNAL_READ;
			}
		   }  
		  
		   startSettings.put(C_START_ACCESSFLAGS,new Integer(flag));
	   return startSettings;
	}
	 /**
	 * Calculates the task settings from the data submitted in
	 * the preference task panel.
	 * @param parameters Hashtable containing all request parameters
	 * @return Explorer filelist flags.
	 */
	private Hashtable getTaskSettings(Hashtable parameters, I_CmsSession session) {
			  
		Hashtable taskSettings=new Hashtable();
			   
		if (parameters.get("CBALL")!= null) {
		 
			taskSettings.put(C_TASK_VIEW_ALL,new Boolean(true));            
		} else {
	
			taskSettings.put(C_TASK_VIEW_ALL,new Boolean(false));
		}
		
		session.putValue(C_SESSION_TASK_ALLPROJECTS,taskSettings.get(C_TASK_VIEW_ALL));      
		
		
		int taskMessages=0;
		
		if (parameters.get("CBMSGACCEPTED")!= null) {
			taskMessages+=C_TASK_MESSAGES_ACCEPTED;
		}

		if (parameters.get("CBMSGFORWAREDED")!= null) {
			taskMessages+=C_TASK_MESSAGES_FORWARDED;            
		}

		if (parameters.get("CBMSGCOMPLETED")!= null) {
			taskMessages+=C_TASK_MESSAGES_COMPLETED;            
		}
	  
		if (parameters.get("CBMSGMEMBERS")!= null) {
			taskMessages+=C_TASK_MESSAGES_MEMBERS;            
		}
	   
	   taskSettings.put(C_TASK_MESSAGES,new Integer(taskMessages));
			 
		String filter=(String)parameters.get("filter");
		if ((filter != null) && (!filter.equals("-"))) {  
		  
			taskSettings.put(C_TASK_FILTER,parameters.get("filter")); 
	 
			session.putValue(C_SESSION_TASK_FILTER,parameters.get("filter"));
		}

		return taskSettings;
	}
	  /**
	 * Gets all views available in the workplace screen.
	 * <P>
	 * The given vectors <code>names</code> and <code>values</code> will 
	 * be filled with the appropriate information to be used for building
	 * a select box.
	 * <P>
	 * <code>names</code> will contain language specific view descriptions
	 * and <code>values</code> will contain the correspondig URL for each
	 * of these views after returning from this method.
	 * <P>
	 * 
	 * @param cms CmsObject Object for accessing system resources.
	 * @param lang reference to the currently valid language file
	 * @param names Vector to be filled with the appropriate values in this method.
	 * @param values Vector to be filled with the appropriate values in this method.
	 * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
	 * @return Index representing the user's current workplace view in the vectors.
	 * @exception CmsException
	 */
	public Integer getViews(CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values, Hashtable parameters) 
			throws CmsException {
		
		// Let's see if we have a session
		CmsRequestContext reqCont = cms.getRequestContext();
		I_CmsSession session = cms.getRequestContext().getSession(true);

		Hashtable startSettings=null;
		String currentView = null;
		
		// try to get an existing value for the default value
		startSettings=(Hashtable)session.getValue("STARTSETTINGS");
		// if this fails, get the settings from the user obeject
		if (startSettings== null) {
			startSettings=(Hashtable)reqCont.currentUser().getAdditionalInfo(C_ADDITIONAL_INFO_STARTSETTINGS);                    
		}      
		if (startSettings != null) {
			currentView = (String)startSettings.get(C_START_VIEW);
		}
		// If there ist a session, let's see if it has a view stored
		if (currentView == null) {
			if(session != null) {
			currentView = (String)session.getValue(C_PARA_VIEW);
			}    
		}
		if (currentView == null) {
			currentView="";
		}
		
		// Check if the list of available views is not yet loaded from the workplace.ini
		if(m_viewNames == null || m_viewLinks == null) {
			m_viewNames = new Vector();
			m_viewLinks = new Vector();

			CmsXmlWpConfigFile configFile = new CmsXmlWpConfigFile(cms);            
			configFile.getWorkplaceIniData(m_viewNames, m_viewLinks,"WORKPLACEVIEWS","VIEW");            
		}
		
		// OK. Now m_viewNames and m_viewLinks contail all available
		// view information.
		// Loop through the vectors and fill the result vectors.
		int currentViewIndex = 0;
		int numViews = m_viewNames.size();        
		for(int i=0; i<numViews; i++) {
			String loopValue = (String)m_viewLinks.elementAt(i);
			String loopName = (String)m_viewNames.elementAt(i);
			values.addElement(loopValue);
			names.addElement(lang.getLanguageValue("select." + loopName));
			if(loopValue.equals(currentView)) {
				currentViewIndex = i;
			}
		}
		return new Integer(currentViewIndex);
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
	/**
	 * Sets all data in the preference panel explorer settings.
	 * Data is either taken form the default values, the current user or the current session.
	 * @param session The current session.
	 * @param parameters  Hashtable containing all request parameters.
	 * @param reqCont The request context.
	 * @param xmlTemplateDocument The template in which all data is added.
	 */    
	private void setExplorerSettings(I_CmsSession session, Hashtable parameters,
									 CmsRequestContext reqCont, CmsXmlWpTemplateFile xmlTemplateDocument) {
			 
		//get the actual user settings  
		// first try to read them from the session
		String explorerSettings=null;
		int explorerSettingsValue=0;
		explorerSettings=(String)session.getValue(C_PARA_EXPLORERSETTINGS);
				
		// if this fails, get the settings from the user obeject
		if (explorerSettings== null) {
			explorerSettings=(String)reqCont.currentUser().getAdditionalInfo(C_ADDITIONAL_INFO_EXPLORERSETTINGS);
		}
				
		//check if the default button was selected.
		// if so, delete all user settings so that they are set to the defaults later.
		if (parameters.get(C_PARA_DEFAULT) !=null) {
					explorerSettings=null;
		}
				
		// if the settings are still empty, set them to default
		if (explorerSettings!= null) {
			explorerSettingsValue=new Integer(explorerSettings).intValue();
		} else {
			explorerSettingsValue=C_FILELIST_TITLE+C_FILELIST_TYPE+C_FILELIST_CHANGED;
		}
											 
		// now update the datablocks in the template
		if ((explorerSettingsValue & C_FILELIST_TITLE) >0) {
			xmlTemplateDocument.setData(C_CHECKTITLE,C_CHECKED);
		} else {
			xmlTemplateDocument.setData(C_CHECKTITLE," ");
		}
				
		if ((explorerSettingsValue & C_FILELIST_TYPE) >0) {
			xmlTemplateDocument.setData(C_CHECKTYPE,C_CHECKED);
		} else {
			xmlTemplateDocument.setData(C_CHECKTYPE," ");
		}
			   
		if ((explorerSettingsValue & C_FILELIST_CHANGED) >0) {
			xmlTemplateDocument.setData(C_CHECKCHANGED,C_CHECKED);
		} else {
			xmlTemplateDocument.setData(C_CHECKCHANGED," ");
		}
				
		if ((explorerSettingsValue & C_FILELIST_SIZE) >0) {
			xmlTemplateDocument.setData(C_CHECKSIZE,C_CHECKED);
		} else {
			xmlTemplateDocument.setData(C_CHECKSIZE," ");
		}
			   
		if ((explorerSettingsValue & C_FILELIST_STATE) >0) {
			xmlTemplateDocument.setData(C_CHECKSTATE,C_CHECKED);
		} else {
			xmlTemplateDocument.setData(C_CHECKSTATE," ");
		}
			  
		if ((explorerSettingsValue & C_FILELIST_OWNER) >0) {
			xmlTemplateDocument.setData(C_CHECKOWNER,C_CHECKED);
		} else {
			xmlTemplateDocument.setData(C_CHECKOWNER," ");
		}
			   
		if ((explorerSettingsValue & C_FILELIST_GROUP) >0) {
			xmlTemplateDocument.setData(C_CHECKGROUP,C_CHECKED);
		} else {
			xmlTemplateDocument.setData(C_CHECKGROUP," ");
		}
			   
		if ((explorerSettingsValue & C_FILELIST_ACCESS) >0) {
			xmlTemplateDocument.setData(C_CHECKACCESS,C_CHECKED);
		} else {
			xmlTemplateDocument.setData(C_CHECKACCESS," ");
		}
			   
		if ((explorerSettingsValue & C_FILELIST_LOCKED) >0) {
			xmlTemplateDocument.setData(C_CHECKLOCKEDBY,C_CHECKED);
		} else {
			xmlTemplateDocument.setData(C_CHECKLOCKEDBY," ");
		}
	}
	 /**
	 * Calculates the settings for the user filelist from the data submitted in
	 * the preference explorer panel.
	 * @param parameters Hashtable containing all request parameters
	 * @return Sring containinb the new user group name.
	 */
  /*  private String getUserSettings(Hashtable parameters) {
		String group;
		group=(String)parameters.get("group");
		return group;
	}*/
	
	
	 /**
	 * User method to get the actual panel of the PReferences dialog.
	 * <P>
	 * @param cms CmsObject Object for accessing system resources.
	 * @param tagcontent Unused in this special case of a user method. Can be ignored.
	 * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document <em>(not used here)</em>.  
	 * @param userObj Hashtable with parameters <em>(not used here)</em>.
	 * @return String with the pics URL.
	 * @exception CmsException
	 */    
	public Object setPanel(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObj) 
		throws CmsException {
		
		I_CmsSession session= cms.getRequestContext().getSession(true);
		String panel=(String)session.getValue(C_PARA_OLDPANEL);
	  
		return panel;
	}
	 /**
	 * Sets all data in the preference panel start settings.
	 * Data is either taken form the default values, the current user or the current session.
	 * @param session The current session.
	 * @param parameters  Hashtable containing all request parameters.
	 * @param reqCont The request context.
	 * @param xmlTemplateDocument The template in which all data is added.
	 */    
	private void setStartSettings(I_CmsSession session, Hashtable parameters,
								 CmsRequestContext reqCont, CmsXmlWpTemplateFile xmlTemplateDocument) {
		   
		// get the actual user settings  
		// first try to read them from the session
		Hashtable startSettings=null;
		startSettings=(Hashtable)session.getValue(C_PARA_STARTSETTINGS);
		// if this fails, get the settings from the user object
		if (startSettings== null) {
			startSettings=(Hashtable)reqCont.currentUser().getAdditionalInfo(C_ADDITIONAL_INFO_STARTSETTINGS);
			if (startSettings != null) {
				startSettings.put(C_START_DEFAULTGROUP, reqCont.currentUser().getDefaultGroup().getName());    
			}
		}                 
		// if the settings are still empty, set them to default
		if (startSettings== null) {
			startSettings=new Hashtable();
				 
			startSettings.put(C_START_LANGUAGE,C_DEFAULT_LANGUAGE);
			startSettings.put(C_START_PROJECT,new Integer(reqCont.currentProject().getId())); 
			String currentView = (String)session.getValue(C_PARA_VIEW);
			if (currentView == null) {
				currentView="explorer.html";
			}        
			startSettings.put(C_START_VIEW,currentView);           
			startSettings.put(C_START_DEFAULTGROUP,
							  reqCont.currentUser().getDefaultGroup().getName());            
			startSettings.put(C_START_ACCESSFLAGS,new Integer(C_ACCESS_DEFAULT_FLAGS));
		}
				
		// now update the data in the template                                    
		int flags=((Integer)startSettings.get(C_START_ACCESSFLAGS)).intValue();
		if ((flags & C_ACCESS_OWNER_READ) >0 ) {
			xmlTemplateDocument.setData(C_CHECKUR,C_CHECKED);    
		} else {
			xmlTemplateDocument.setData(C_CHECKUR," ");    
		}
		if ((flags & C_ACCESS_OWNER_WRITE) >0 ) {
			xmlTemplateDocument.setData(C_CHECKUW,C_CHECKED);    
		} else {
			xmlTemplateDocument.setData(C_CHECKUW," ");    
		}
		if ((flags & C_ACCESS_OWNER_VISIBLE) >0 ) {
			xmlTemplateDocument.setData(C_CHECKUV,C_CHECKED);    
		} else {
			xmlTemplateDocument.setData(C_CHECKUV," ");    
		}     
		if ((flags & C_ACCESS_GROUP_READ) >0 ) {
			xmlTemplateDocument.setData(C_CHECKGR,C_CHECKED);    
		} else {
			xmlTemplateDocument.setData(C_CHECKGR," ");    
		}
		if ((flags & C_ACCESS_GROUP_WRITE) >0 ) {
			xmlTemplateDocument.setData(C_CHECKGW,C_CHECKED);    
		} else {
			xmlTemplateDocument.setData(C_CHECKGW," ");    
		}
		if ((flags & C_ACCESS_GROUP_VISIBLE) >0 ) {
			xmlTemplateDocument.setData(C_CHECKGV,C_CHECKED);    
		} else {
			xmlTemplateDocument.setData(C_CHECKGV," ");    
		}  
		if ((flags & C_ACCESS_PUBLIC_READ) >0 ) {
			xmlTemplateDocument.setData(C_CHECKPR,C_CHECKED);    
		} else {
			xmlTemplateDocument.setData(C_CHECKPR," ");    
		}
		if ((flags & C_ACCESS_PUBLIC_WRITE) >0 ) {
			xmlTemplateDocument.setData(C_CHECKPW,C_CHECKED);    
		} else {
			xmlTemplateDocument.setData(C_CHECKPW," ");    
		}
		if ((flags & C_ACCESS_PUBLIC_VISIBLE) >0 ) {
			xmlTemplateDocument.setData(C_CHECKPV,C_CHECKED);    
		} else {
			xmlTemplateDocument.setData(C_CHECKPV," ");    
		}  
		if ((flags & C_ACCESS_INTERNAL_READ) >0 ) {
			xmlTemplateDocument.setData(C_CHECKIF,C_CHECKED);    
		} else {
			xmlTemplateDocument.setData(C_CHECKIF," ");    
		}  
	}
	/**
	 * Sets all data in the preference panel task settings.
	 * Data is either taken form the default values, the current user or the current session.
	 * @param session The current session.
	 * @param parameters  Hashtable containing all request parameters.
	 * @param reqCont The request context.
	 * @param xmlTemplateDocument The template in which all data is added.
	 */    
	private void setTaskSettings(I_CmsSession session, Hashtable parameters,
								 CmsRequestContext reqCont, CmsXmlWpTemplateFile xmlTemplateDocument) {
		// get the actual user settings  
		// first try to read them from the session
		Hashtable taskSettings=null;
		taskSettings=(Hashtable)session.getValue(C_PARA_TASKSETTINGS);
		// if this fails, get the settings from the user obeject
		if (taskSettings== null) {
			taskSettings=(Hashtable)reqCont.currentUser().getAdditionalInfo(C_ADDITIONAL_INFO_TASKSETTINGS);                    
		 }
				
		// if the settings are still empty, set them to default
		if (taskSettings== null) {
			taskSettings=new Hashtable();
			taskSettings.put(C_TASK_VIEW_ALL,new Boolean(false));
			taskSettings.put(C_TASK_MESSAGES,new Integer(C_TASK_MESSAGES_ACCEPTED+
														 C_TASK_MESSAGES_FORWARDED+
														 C_TASK_MESSAGES_COMPLETED+
														 C_TASK_MESSAGES_MEMBERS));
			taskSettings.put(C_TASK_FILTER,new String("a1"));  
	   }
				
	   //now update the data in the template
	   if (((Boolean)taskSettings.get(C_TASK_VIEW_ALL)).booleanValue()) {
			xmlTemplateDocument.setData(C_CHVIEWALL,C_CHECKED);
	   } else {
			xmlTemplateDocument.setData(C_CHVIEWALL," ");
	   }
	   
	   int taskMessages=((Integer)taskSettings.get(C_TASK_MESSAGES)).intValue();
			   
	   if ((taskMessages & C_TASK_MESSAGES_ACCEPTED) >0){
			xmlTemplateDocument.setData(C_CHMESSAGEACCEPTED,C_CHECKED);
	   } else {
			xmlTemplateDocument.setData(C_CHMESSAGEACCEPTED," ");
	   }
		   
	   if ((taskMessages & C_TASK_MESSAGES_FORWARDED) >0){
			xmlTemplateDocument.setData(C_CHMESSAGEFORWARDED,C_CHECKED);
	   } else {
			xmlTemplateDocument.setData(C_CHMESSAGEFORWARDED," ");
	   }
				
	   if ((taskMessages & C_TASK_MESSAGES_COMPLETED) >0){
			xmlTemplateDocument.setData(C_CHMESSAGECOMPLETED,C_CHECKED);
	   } else {
			xmlTemplateDocument.setData(C_CHMESSAGECOMPLETED," ");
	   }
				
	   if ((taskMessages & C_TASK_MESSAGES_MEMBERS) >0){
			xmlTemplateDocument.setData(C_CHMESSAGEMEMEBERS,C_CHECKED);
	   } else {
			xmlTemplateDocument.setData(C_CHMESSAGEMEMEBERS," ");
	   }                          
	}
	 /**
	 * Sets all data in the preference panel user settings.
	 * Data is either taken form the default values, the current user or the current session.
	 * @param session The current session.
	 * @param parameters  Hashtable containing all request parameters.
	 * @param reqCont The request context.
	 * @param xmlTemplateDocument The template in which all data is added.
	 */    
	private void setUserSettings(I_CmsSession session, Hashtable parameters,
								 CmsRequestContext reqCont, CmsXmlWpTemplateFile xmlTemplateDocument) {
		
		// get the current user
		CmsUser user=reqCont.currentUser();

		//set the required datablocks
		xmlTemplateDocument.setData("USER",user.getName());
		xmlTemplateDocument.setData("FIRSTNAME",user.getFirstname());
		xmlTemplateDocument.setData("LASTNAME",user.getLastname());
		xmlTemplateDocument.setData("DESCRIPTION",user.getDescription());
		xmlTemplateDocument.setData("EMAIL",user.getEmail());
		xmlTemplateDocument.setData("ADRESS",user.getAddress());                
		xmlTemplateDocument.setData("CURRENTGROUP",reqCont.currentGroup().getName());   
	}
}
