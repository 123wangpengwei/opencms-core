/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsTaskNew.java,v $
* Date   : $Date: 2003/09/17 18:08:07 $
* Version: $Revision: 1.35 $
*
* This library is part of OpenCms -
* the Open Source Content Mananagement System
*
* Copyright (C) 2001  The OpenCms Group
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* For further information about OpenCms, please see the
* OpenCms Website: http://www.opencms.org 
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/


package com.opencms.workplace;

import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;

import com.opencms.core.CmsException;
import com.opencms.file.CmsGroup;
import com.opencms.file.CmsObject;
import com.opencms.file.CmsUser;
import com.opencms.template.A_CmsXmlContent;
import com.opencms.util.Utils;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Template class for displaying OpenCms workplace task new screens.
 * <P>
 *
 * @author Andreas Schouten
 * @version $Revision: 1.35 $ $Date: 2003/09/17 18:08:07 $
 * @see com.opencms.workplace.CmsXmlWpTemplateFile
 */

public class CmsTaskNew extends CmsWorkplaceDefault {


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
     * Gets the content of a defined section in a given template file and its subtemplates
     * with the given parameters.
     *
     * @see #getContent(CmsObject, String, String, Hashtable, String)
     * @param cms CmsObject Object for accessing system resources.
     * @param templateFile Filename of the template file.
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     */

    public byte[] getContent(CmsObject cms, String templateFile, String elementName,
            Hashtable parameters, String templateSelector) throws CmsException {
        if(OpenCms.getLog(CmsLog.CHANNEL_WORKPLACE_XML).isDebugEnabled() && C_DEBUG) {
            OpenCms.getLog(CmsLog.CHANNEL_WORKPLACE_XML).debug("Getting content of element " + ((elementName==null)?"<root>":elementName));
            OpenCms.getLog(CmsLog.CHANNEL_WORKPLACE_XML).debug("Template file is: " + templateFile);
            OpenCms.getLog(CmsLog.CHANNEL_WORKPLACE_XML).debug("Selected template section is: " + ((templateSelector==null)?"<default>":templateSelector));
        }
        CmsXmlWpTemplateFile xmlTemplateDocument = (CmsXmlWpTemplateFile)getOwnTemplateFile(cms,
                templateFile, elementName, parameters, templateSelector);
        String paraAcceptation = "checked";
        String paraAll = "checked";
        String paraCompletion = "checked";
        String paraDelivery = "checked";
        Hashtable taskSettings = (Hashtable)(cms.getRequestContext().currentUser().getAdditionalInfo()).get(C_ADDITIONAL_INFO_TASKSETTINGS);
        if(taskSettings != null) {

            // the tasksettings exists - use them
            int messageAt = ((Integer)taskSettings.get(C_TASK_MESSAGES)).intValue();
            if((messageAt & C_TASK_MESSAGES_ACCEPTED) != C_TASK_MESSAGES_ACCEPTED) {
                paraAcceptation = "";
            }
            if((messageAt & C_TASK_MESSAGES_COMPLETED) != C_TASK_MESSAGES_COMPLETED) {
                paraCompletion = "";
            }
            if((messageAt & C_TASK_MESSAGES_FORWARDED) != C_TASK_MESSAGES_FORWARDED) {
                paraDelivery = "";
            }
            if((messageAt & C_TASK_MESSAGES_MEMBERS) != C_TASK_MESSAGES_MEMBERS) {
                paraAll = "";
            }
        }
        xmlTemplateDocument.setData(C_TASKPARA_ACCEPTATION, paraAcceptation);
        xmlTemplateDocument.setData(C_TASKPARA_ALL, paraAll);
        xmlTemplateDocument.setData(C_TASKPARA_COMPLETION, paraCompletion);
        xmlTemplateDocument.setData(C_TASKPARA_DELIVERY, paraDelivery);

        // are the constants read from the cms already?
        if(C_ROLE == null) {

            // declare the constants
            initConstants(xmlTemplateDocument);
        }

        // create task, if ok was pressed
        if(parameters.get("ok") != null) {

            // try to create the task
            try {
                CmsTaskAction.create(cms, (String)parameters.get("USER"),
                        (String)parameters.get("TEAM"), (String)parameters.get("TASKNAME"),
                        (String)parameters.get("DESCRIPTION"), (String)parameters.get("DATE"),
                        (String)parameters.get("PRIO"), (String)parameters.get("MSG_ACCEPTATION"),
                        (String)parameters.get("MSG_ALL"), (String)parameters.get("MSG_COMPLETION"),
                        (String)parameters.get("MSG_DELIVERY"));
                templateSelector = "done";
            }
            catch(Exception exc) {
                if(OpenCms.getLog(CmsLog.CHANNEL_WORKPLACE_XML).isWarnEnabled() ) {
                    OpenCms.getLog(CmsLog.CHANNEL_WORKPLACE_XML).warn("Could not create task", exc);
                }
                xmlTemplateDocument.setData("details", Utils.getStackTrace(exc));
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
     * @param cms CmsObject Object for accessing system resources.
     * @param names Vector to be filled with the appropriate values in this method.
     * @param values Vector to be filled with the appropriate values in this method.
     * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
     * @return Index representing the current value in the vectors.
     * @throws CmsException
     */

    public Integer getTeams(CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values,
            Hashtable parameters) throws CmsException {

        // get all groups
        Vector groups = cms.getGroups();
        CmsGroup group;
        names.addElement(lang.getLanguageValue("task.label.emptyrole"));
        values.addElement(lang.getLanguageValue("task.label.emptyrole"));

        // fill the names and values
        for(int z = 0;z < groups.size();z++) {
            group = (CmsGroup)groups.elementAt(z);

            // is the group a role?
            if(group.getRole()) {
                String name = group.getName();
                names.addElement(name);
                values.addElement(name);
            }
        }
        names.addElement(lang.getLanguageValue("task.label.allroles"));
        values.addElement(C_ALL_ROLES);

        // no current group, set index to -1
        return new Integer(-1);
    }

    /**
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document <em>(not used here)</em>.
     * @param userObj Hashtable with parameters <em>(not used here)</em>.
     * @return String with the pics URL.
     * @throws CmsException
     */

    public Object getUsers(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObj) throws CmsException {
        StringBuffer retValue = new StringBuffer();
        retValue.append(C_ROLE);

        // get the language for choose-user
        String chooseUser = (new CmsXmlLanguageFile(cms)).getLanguageValue("task.label.emptyuser");

        // get all groups
        Vector groups = cms.getGroups();
        for(int n = 0;n < groups.size();n++) {
            if(((CmsGroup)groups.elementAt(n)).getRole()) {
                String groupname = ((CmsGroup)groups.elementAt(n)).getName();

                // get users of this group
                Vector users = cms.getUsersOfGroup(groupname);

                // create entry for role
                retValue.append(C_ROLE_1 + groupname + C_ROLE_2);
                retValue.append(C_USER_1 + groupname + C_USER_2 + 0 + C_USER_3 + chooseUser + C_USER_4 + C_USER_5);
                for(int m = 0;m < users.size();m++) {
                    CmsUser user = (CmsUser)users.elementAt(m);

                    // create entry for user
                    retValue.append(C_USER_1 + groupname + C_USER_2 + (m + 1) + C_USER_3 + user.getName() + C_USER_4 + user.getName() + C_USER_5);
                }
            }
        }

        // generate output for all users
        retValue.append(C_ROLE_1 + C_ALL_ROLES + C_ROLE_2);
        retValue.append(C_USER_1 + C_ALL_ROLES + C_USER_2 + 0 + C_USER_3 + chooseUser + C_USER_4 + C_USER_5);
        Vector users = cms.getUsers();
        for(int m = 0;m < users.size();m++) {
            CmsUser user = (CmsUser)users.elementAt(m);

            // create entry for user
            retValue.append(C_USER_1 + C_ALL_ROLES + C_USER_2 + (m + 1) + C_USER_3 + user.getName() + C_USER_4 + user.getName() + C_USER_5);
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
            C_ROLE = document.getDataValue("role");
            C_ROLE_1 = document.getDataValue("role_1");
            C_ROLE_2 = document.getDataValue("role_2");
            C_USER_1 = document.getDataValue("user_1");
            C_USER_2 = document.getDataValue("user_2");
            C_USER_3 = document.getDataValue("user_3");
            C_USER_4 = document.getDataValue("user_4");
            C_USER_5 = document.getDataValue("user_5");
        }
        catch(CmsException exc) {
            if(OpenCms.getLog(CmsLog.CHANNEL_WORKPLACE_XML).isWarnEnabled() ) {
                OpenCms.getLog(CmsLog.CHANNEL_WORKPLACE_XML).warn("Couldn't get xml datablocks for CmsTaskNew", exc);
            }
        }
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
