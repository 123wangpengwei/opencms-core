/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsTaskNew.java,v $
 * Date   : $Date: 2000/03/13 15:40:30 $
 * Version: $Revision: 1.11 $
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

package com.opencms.workplace;

import com.opencms.file.*;
import com.opencms.core.*;
import com.opencms.util.*;
import com.opencms.template.*;

import java.util.*;

import javax.servlet.http.*;

/**
 * Template class for displaying OpenCms workplace task new screens.
 * <P>
 * 
 * @author Andreas Schouten
 * @version $Revision: 1.11 $ $Date: 2000/03/13 15:40:30 $
 * @see com.opencms.workplace.CmsXmlWpTemplateFile
 */
public class CmsTaskNew extends CmsWorkplaceDefault implements I_CmsConstants {
	
	/**
	 * Constant for generating user javascriptlist
	 */
	private final static String C_ALL_ROLES = "___all";

	/**
	 * Constant for generating user javascriptlist
	 */
	private static String C_ROLE = null;

	/**
	 * Constant for generating user javascriptlist
	 */
	private static String C_ROLE_1 = null;

	/**
	 * Constant for generating user javascriptlist
	 */
	private static String C_ROLE_2 = null;

	/**
	 * Constant for generating user javascriptlist
	 */
	private static String C_USER_1 = null;

	/**
	 * Constant for generating user javascriptlist
	 */
	private static String C_USER_2 = null;

	/**
	 * Constant for generating user javascriptlist
	 */
	private static String C_USER_3 = null;

	/**
	 * Constant for generating user javascriptlist
	 */
	private static String C_USER_4 = null;
	
	/**
	 * Constant for generating user javascriptlist
	 */
	private static String C_USER_5 = null;
	
	/**
     * Indicates if the results of this class are cacheable.
     * 
     * @param cms A_CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file 
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return <EM>true</EM> if cacheable, <EM>false</EM> otherwise.
     */
    public boolean isCacheable(A_CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
        return false;
    }    

    /**
     * Gets the content of a defined section in a given template file and its subtemplates
     * with the given parameters. 
     * 
     * @see getContent(A_CmsObject cms, String templateFile, String elementName, Hashtable parameters)
     * @param cms A_CmsObject Object for accessing system resources.
     * @param templateFile Filename of the template file.
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     */
    public byte[] getContent(A_CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        if(C_DEBUG && A_OpenCms.isLogging()) {
            A_OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "getting content of element " + ((elementName==null)?"<root>":elementName));
            A_OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "template file is: " + templateFile);
            A_OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "selected template section is: " + ((templateSelector==null)?"<default>":templateSelector));
        }
		
		CmsXmlTemplateFile xmlTemplateDocument = getOwnTemplateFile(cms, templateFile, elementName, parameters, templateSelector);

		// are the constants read from the cms already?
		if( C_ROLE == null ) {
			// declare the constants
			initConstants((CmsXmlWpTemplateFile) xmlTemplateDocument);
		}
		
		// create task, if ok was pressed
		if(parameters.get("ok") != null) {
			// get the parameters
			String agentName = (String)parameters.get("USER");
			String roleName = (String)parameters.get("TEAM");
			if( roleName.equals(C_ALL_ROLES) ) {
				roleName = cms.readUser(agentName).getDefaultGroup().getName();
			}
			String taskName = (String)parameters.get("TASKNAME");
			String taskcomment = (String)parameters.get("DESCRIPTION");
			String timeoutString = (String)parameters.get("DATE");
			String priorityString = (String)parameters.get("PRIO");
			String paraAcceptation = (String)parameters.get("MSG_ACCEPTATION");
			String paraAll = (String)parameters.get("MSG_ALL");
			String paraCompletion = (String)parameters.get("MSG_COMPLETION");
			String paraDelivery = (String)parameters.get("MSG_DELIVERY");
			
			// try to create the task
			try {
				int priority = Integer.parseInt(priorityString);
				// create a long from the overgiven date.
				String splittetDate[] = Utils.split(timeoutString, ".");
				GregorianCalendar cal = new GregorianCalendar(Integer.parseInt(splittetDate[2]),
															  Integer.parseInt(splittetDate[1]) - 1,
															  Integer.parseInt(splittetDate[0]), 0, 0, 0);
				long timeout = cal.getTime().getTime();
                		
				A_CmsTask task = cms.createTask(agentName, roleName, taskName, 
												taskcomment, timeout, priority);
				cms.setTaskPar(task.getId(),C_TASKPARA_ACCEPTATION, paraAcceptation);
				cms.setTaskPar(task.getId(),C_TASKPARA_ALL, paraAll);
				cms.setTaskPar(task.getId(),C_TASKPARA_COMPLETION, paraCompletion);
				cms.setTaskPar(task.getId(),C_TASKPARA_DELIVERY, paraDelivery);
				CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);
				String comment = lang.getLanguageValue("task.label.forrole") + ": " + roleName + "\n";
				comment += lang.getLanguageValue("task.label.editor") + ": " +  Utils.getFullName(cms.readUser(agentName)) + "\n";
				comment += taskcomment;
				cms.writeTaskLog(task.getId(), comment, C_TASKLOGTYPE_CREATED);

				templateSelector = "done";
			} catch( Exception exc ) {
				A_OpenCms.log(C_MODULE_INFO, "Could not create task. " + exc.getMessage());
				templateSelector = "error";
			}

		}
		
		// Now load the template file and start the processing
		return startProcessing(cms, xmlTemplateDocument, elementName, parameters, templateSelector);
    }
	
    /**
     * Gets all groups, that may work for a project.
     * <P>
     * The given vectors <code>names</code> and <code>values</code> will 
     * be filled with the appropriate information to be used for building
     * a select box.
     * 
     * @param cms A_CmsObject Object for accessing system resources.
     * @param names Vector to be filled with the appropriate values in this method.
     * @param values Vector to be filled with the appropriate values in this method.
     * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
     * @return Index representing the current value in the vectors.
     * @exception CmsException
     */
    public Integer getTeams(A_CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values, Hashtable parameters) 
		throws CmsException {
		// get all groups
		Vector groups = cms.getGroups();
		
		names.addElement(lang.getLanguageValue("task.label.emptyrole"));
		values.addElement(lang.getLanguageValue("task.label.emptyrole"));

		// fill the names and values
		for(int z = 0; z < groups.size(); z++) {
			String name = ((A_CmsGroup)groups.elementAt(z)).getName();
			names.addElement(name);
			values.addElement(((A_CmsGroup)groups.elementAt(z)).getName());
		}
		
		names.addElement(lang.getLanguageValue("task.label.allroles"));
		values.addElement(C_ALL_ROLES);

		// no current group, set index to -1
        return new Integer(-1);
    }

    /**
     * @param cms A_CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document <em>(not used here)</em>.  
     * @param userObj Hashtable with parameters <em>(not used here)</em>.
     * @return String with the pics URL.
     * @exception CmsException
     */    
    public Object getUsers(A_CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObj) 
		throws CmsException {
		
		StringBuffer retValue = new StringBuffer();
		retValue.append(C_ROLE);

		// get the language for choose-user
		String chooseUser = (new CmsXmlLanguageFile(cms)).getLanguageValue("task.label.emptyuser");
		
		// get all groups
		Vector groups = cms.getGroups();
		
		for(int n = 0; n < groups.size(); n++) {
			String groupname = ((A_CmsGroup)groups.elementAt(n)).getName();
			// get users of this group
			Vector users = cms.getUsersOfGroup(groupname);

			// create entry for role
			retValue.append( C_ROLE_1 + groupname + C_ROLE_2 );
			
			retValue.append(C_USER_1 + groupname + C_USER_2 + 0 + C_USER_3 + 
							chooseUser + C_USER_4 + C_USER_5);
			
			for(int m = 0; m < users.size(); m++) {
				A_CmsUser user = (A_CmsUser)users.elementAt(m);
				
				// create entry for user
				retValue.append(C_USER_1 + groupname + C_USER_2 + (m + 1) + C_USER_3 + 
								user.getName() + C_USER_4 + user.getName() + C_USER_5);
			}			
		}
		
		// generate output for all users
		
		retValue.append( C_ROLE_1 + C_ALL_ROLES + C_ROLE_2 );
		
		retValue.append(C_USER_1 + C_ALL_ROLES + C_USER_2 + 0 + C_USER_3 + 
						chooseUser + C_USER_4 + C_USER_5);
		
		Vector users = cms.getUsers();
		
		for(int m = 0; m < users.size(); m++) {
			A_CmsUser user = (A_CmsUser)users.elementAt(m);
				
			// create entry for user
			retValue.append(C_USER_1 + C_ALL_ROLES + C_USER_2 + (m + 1) + C_USER_3 + 
							user.getName() + C_USER_4 + user.getName() + C_USER_5);
		}			
		
        return retValue.toString();
    }
	
	/**
	 * This method initializes all constants, that are needed for genrating this pages.
	 * 
	 * @param document The xml-document to get the constant content from.
	 */
	private void initConstants(CmsXmlWpTemplateFile document) {
		try {
			C_ROLE = document.getXmlDataValue("role");
			C_ROLE_1 = document.getXmlDataValue("role_1");
			C_ROLE_2 = document.getXmlDataValue("role_2");
			C_USER_1 = document.getXmlDataValue("user_1");
			C_USER_2 = document.getXmlDataValue("user_2");
			C_USER_3 = document.getXmlDataValue("user_3");
			C_USER_4 = document.getXmlDataValue("user_4");
			C_USER_5 = document.getXmlDataValue("user_5");
		} catch (CmsException exc) {
			A_OpenCms.log(C_MODULE_CRITICAL, "Couldn't get xml datablocks for CmsTaskNew");
		}
	}

}
