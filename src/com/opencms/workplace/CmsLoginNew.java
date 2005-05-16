/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsLoginNew.java,v $
 * Date   : $Date: 2005/05/16 13:46:56 $
 * Version: $Revision: 1.40 $
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

import org.opencms.db.CmsDbEntryNotFoundException;
import org.opencms.db.CmsUserSettings;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsUser;
import org.opencms.i18n.CmsMessages;
import org.opencms.main.CmsException;
import org.opencms.main.I_CmsConstants;
import org.opencms.main.OpenCms;
import org.opencms.security.CmsSecurityException;
import org.opencms.workplace.CmsWorkplaceAction;
import org.opencms.workplace.CmsWorkplaceView;
import org.opencms.workplace.I_CmsWpConstants;

import com.opencms.core.I_CmsSession;
import com.opencms.legacy.CmsLegacyException;
import com.opencms.legacy.CmsXmlTemplateLoader;
import com.opencms.template.A_CmsXmlContent;
import com.opencms.template.CmsCacheDirectives;
import com.opencms.template.CmsXmlTemplate;
import com.opencms.template.CmsXmlTemplateFile;

import java.util.Hashtable;
import java.util.Iterator;

/**
 * Template class for displaying the login screen of the OpenCms workplace.<P>
 * Reads template files of the content type <code>CmsXmlWpTemplateFile</code>.
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * @version $Revision: 1.40 $ 
 * 
 * @deprecated Will not be supported past the OpenCms 6 release.
 */

public class CmsLoginNew extends CmsXmlTemplate {

    /** Debug flag, set to 9 for maximum verbosity */
    private static final int DEBUG = 0;

    /**
     * Gets the content of the login template and processes the data input.<p>
     * 
     * If the user has authenticated himself to the system, 
     * the login window is closed and the workplace is opened.
     * If the login was incorrect, an error message is displayed and the login
     * dialog is displayed again.
     * 
     * @param cms request initialized CmsObject
     * @param templateFile the login template file
     * @param elementName not used
     * @param parameters parameters of the request and the template
     * @param templateSelector selector of the template tag to be displayed
     * @return the processed data of the template
     * @throws CmsException if something goes wrong
     */
    public byte[] getContent(CmsObject cms, String templateFile, 
        String elementName, Hashtable parameters, String templateSelector) 
    throws CmsException {
                                              
        if (DEBUG > 1) System.err.println("\nCmsLoginNew: Login process started");
        
        // Initialize language and encoding
        CmsXmlLanguageFile langFile = new CmsXmlLanguageFile(cms);
        m_messages = langFile.getMessages();
        // Ensure encoding for the login page is set correctly accoring to selected language
        cms.getRequestContext().setEncoding(langFile.getEncoding());
        
        // Check if a "logout=true" parameter is present, if so trash the session
        boolean logout = (null != (String)parameters.get("logout"));
        
        I_CmsSession session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), false);
        // Check if there already is a session
        if (session != null) {
            // Old session found, must be invalidated
            if (logout) {
                session.invalidate();
                if (DEBUG > 2) System.err.println("CmsLoginNew: logout, trashed old session");
            } else {
                if (DEBUG > 2) System.err.println("CmsLoginNew: kept old session, no logout parameter found");
            }
        } else {
            if (DEBUG > 2) System.err.println("CmsLoginNew: no current active session");
        }

        // the template to be displayed
        CmsXmlTemplateFile xmlTemplateDocument = new CmsXmlTemplateFile(cms, templateFile);
                
        String username = null;
        CmsUser user;

        // get user name and password
        String name = (String)parameters.get("OPENCMSUSERNAME");
        String password = (String)parameters.get("OPENCMSPASSWORD");
        // get further startup parameters
        String startTaskId = (String)parameters.get(I_CmsWpConstants.C_PARA_STARTTASKID);
        String startProjectId = (String)parameters.get(I_CmsWpConstants.C_PARA_STARTPROJECTID);

        if (DEBUG > 1) System.err.println("CmsLoginNew: name=" + name + " password=" + password + " task=" + startTaskId + " project=" + startProjectId);

        if ((name != null) && (password != null)) {
            if (DEBUG > 1) System.err.println("CmsLoginNew: trying to log in");
            // user and password have been submitted, try to log in the user        
            boolean validLogin;
            try {
                username = cms.loginUser(name, password);
                validLogin = true;
                if (DEBUG > 1) System.err.println("CmsLoginNew: cms.loginUser() successfull");
            } catch (CmsException e) {
                // invalid login
                validLogin = false;
                if (DEBUG > 1) System.err.println("CmsLoginNew: cms.loginUser() failed");
            }

            if ((username != null) && (username.equals(OpenCms.getDefaultUsers().getUserGuest()))) {
                // please no Guest user in the workplace
                // use the same behaviour as if the access was unauthorized
                validLogin = false;
                if (DEBUG > 1) System.err.println("CmsLoginNew: user was guest user");
            } else if ((username != null) 
                && (! cms.userInGroup(username, OpenCms.getDefaultUsers().getGroupUsers())) 
                && (! cms.userInGroup(username, OpenCms.getDefaultUsers().getGroupProjectmanagers())) 
                && (! cms.userInGroup(username, OpenCms.getDefaultUsers().getGroupAdministrators()))) {
                // user MUST be in at last one of the default groups for administrators, users or project managers
                // use the same behaviour as if the access was unauthorized
                validLogin = false;
                if (DEBUG > 1) System.err.println("CmsLoginNew: user was not in default groups");
            }
            
            if (! validLogin) {
                if (OpenCms.getLog(this).isInfoEnabled()) {
                    OpenCms.getLog(this).info("Failed login attempt for user '" + name + "'");
                }                
                throw new CmsLegacyException("[OpenCms login failed]", CmsLegacyException.C_NO_USER);
            }
            if (DEBUG > 0) System.err.println("CmsLoginNew: user " + username + " logged in");

            // get a session for this user so that he is authentificated at the
            // end of this request
            session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
            if (OpenCms.getLog(this).isInfoEnabled()) {
                OpenCms.getLog(this).info("Login of user '" + username + "'");
            }

            // read the user data from the databsse
            user = cms.readUser(username);
            
            // set the startup project id
            setStartProjectId(cms, session, startProjectId);

            // set startup task view
            setStartTaskId(session, startTaskId);            
            
            // set the additional user preferences
            Hashtable preferences = (Hashtable)user.getAdditionalInfo(I_CmsConstants.C_ADDITIONAL_INFO_PREFERENCES);
            // check if preferences are existing, otherwise use defaults
            if (preferences == null) {
                preferences = getDefaultPreferences();
            }
            // check of the users language setting (if he has one)
            session.removeValue(I_CmsConstants.C_START_LOCALE);
            
            preferences.put(I_CmsConstants.C_START_LOCALE, cms.getRequestContext().getLocale().toString());
            session.putValue(I_CmsConstants.C_ADDITIONAL_INFO_PREFERENCES, preferences);    

            langFile = new CmsXmlLanguageFile(cms, cms.getRequestContext().getLocale().getLanguage());
            if (DEBUG > 1) System.err.println("CmsLoginNew: encoding: " + langFile.getEncoding());           
            cms.getRequestContext().setEncoding(langFile.getEncoding());        
            
            // trigger call of "login()" JavaScript in Template on page load
            xmlTemplateDocument.setData("onload", "onload='login();'");
        } else if ((! logout) && ((cms.getRequestContext().currentUser()) != null) 
            && (! OpenCms.getDefaultUsers().getUserGuest().equals(cms.getRequestContext().currentUser().getName())) 
            && ((cms.userInGroup(cms.getRequestContext().currentUser().getName(), OpenCms.getDefaultUsers().getGroupUsers())) 
                || (cms.userInGroup(cms.getRequestContext().currentUser().getName(), OpenCms.getDefaultUsers().getGroupProjectmanagers())) 
                || (cms.userInGroup(cms.getRequestContext().currentUser().getName(), OpenCms.getDefaultUsers().getGroupAdministrators())))) {
            // the user is already logged in and no logout parameter is present, open a new window
            if (DEBUG > 1) System.err.println("CmsLoginNew: re-using old login");            
            xmlTemplateDocument.setData("onload", "onload='login();'");        
        } else {
            // no user logged in, no call to "login()" JavaScript
            if (DEBUG > 1) System.err.println("CmsLoginNew: no login or logout, displaying template");            
            xmlTemplateDocument.setData("onload", "onload='init();'");
        }

        long id = System.currentTimeMillis();
        xmlTemplateDocument.setData("windowId", new Long(id).toString());
        xmlTemplateDocument.setData("startTaskId", startTaskId);
        xmlTemplateDocument.setData("startProjectId", startProjectId);

        if (DEBUG > 1) System.err.println("CmsLoginNew: Login process finished");

        // process the selected template
        return startProcessing(cms, xmlTemplateDocument, "", parameters, templateSelector);
    }

    /**
     * Sets the startup project to the one read from the user
     * preferences or the one from the request parameters.
     * 
     * @param cms the initialized CmsObject
     * @param session the initialized user session 
     * @param startProjectId the id value of the request parameter (might be null)
     * @throws CmsException in case of issues reading the registry
     */
    private void setStartProjectId(CmsObject cms, I_CmsSession session, String startProjectId) 
    throws CmsException {
        // set current project to the default online project or to 
        // project specified in the users preferences
        int currentProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        
        if ((startProjectId != null) && (! "".equals(startProjectId))) {
            // try to set project to id from parameters
            try {
                currentProject = (new Integer(startProjectId)).intValue();
            } catch (NumberFormatException e) {
                // currentProject will still have online project value
            }            
        } else {    
            // check out the user information if a default project is stored there.
            CmsUserSettings settings = new CmsUserSettings(cms.getRequestContext().currentUser());
            try {
                CmsProject project = cms.readProject(settings.getStartProject());
                currentProject = project.getId();
            } catch (CmsDbEntryNotFoundException e) {
                // the project does not exist, maybe it was deleted
                // set ID to online project
                currentProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
            }           
        }

        // try to set the current project
        try {
            CmsProject project = cms.readProject(currentProject);
            if (! cms.getAllAccessibleProjects().contains(project)) {
                // user has no (more) access to the project
                currentProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
            }
        } catch (Exception e) {
            // project will default to online project
            currentProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        }
        
        // set the current project id
        cms.getRequestContext().setCurrentProject(cms.readProject(currentProject));
    }

    /**
     * Sets the startup view to display the selected start task.
     * 
     * @param session the initialized user session 
     * @param startTaskId the id of the task to display
     */
    private void setStartTaskId(I_CmsSession session, String startTaskId) {
        if ((startTaskId == null) || ("".equals(startTaskId))) { 
            return;
        }
        String link = "";
        Iterator i = OpenCms.getWorkplaceManager().getViews().iterator();
        while (i.hasNext()) {
            CmsWorkplaceView view = (CmsWorkplaceView)i.next();
            if (view.getKey().equals("select.tasks")) {
                link = view.getUri();
                break;
            }            
        }
        session.putValue(I_CmsWpConstants.C_PARA_STARTTASKID, startTaskId);
        session.putValue(I_CmsWpConstants.C_PARA_VIEW, link);
    }

    private CmsMessages m_messages;

    /**
     * Sets the default preferences for the current user if those values are not available.
     * @return Hashtable with default preferences.
     */

    private Hashtable getDefaultPreferences() {
        Hashtable pref = new Hashtable();
        // set the default columns in the filelist
        int filelist = 4095 + 512;
        pref.put(I_CmsWpConstants.C_USERPREF_FILELIST, new Integer(filelist));
        return pref;
    }

    /**
     * Customized <code>getTitle()</code> method for adding the current 
     * version information to the title of the login screen.
     *
     * @param cms for accessing system resources
     * @param tagcontent (unused)
     * @param doc reference to the A_CmsXmlContent object of the initiating XML document.
     * @param userObject must ba a <code>java.util.Hashtable</code> with request parameters
     * @return String with customized title information
     * @throws CmsException in case of errors processing the template
     */
    public Object getTitle(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) 
    throws CmsException {
        String title = (String)super.getTitle(cms, tagcontent, doc, userObject);
        if (title == null) title = "";
        title += " - " + OpenCms.getSystemInfo().getVersionName();
        return title;
    }

    /**
     * Returns a String with the version information of this OpenCms instance
     *
     * @param cms for accessing system resources
     * @param tagcontent (unused)
     * @param doc reference to the A_CmsXmlContent object of the initiating XML document.
     * @param userObject must ba a <code>java.util.Hashtable</code> with request parameters
     * @return String with the version information of this OpenCms instance
     * @throws CmsException in case of errors processing the template
     */
    public Object version(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) 
    throws CmsException {
        return OpenCms.getSystemInfo().getVersionName();
    }

    /**
     * Returns a localized String for the key value given as <code>tagcontent</code> 
     * parameter.
     *
     * @param cms for accessing system resources
     * @param tagcontent key value for the resource bundle
     * @param doc reference to the A_CmsXmlContent object of the initiating XML document.
     * @param userObject must ba a <code>java.util.Hashtable</code> with request parameters
     * @return String with the version information of this OpenCms instance
     * @throws CmsException in case of errors processing the template
     */
    public Object message(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) 
    throws CmsException {
        return m_messages.key(tagcontent);
    }
    
    /**
     * Returns the path to the workplace top level uri.<p>
     *
     * @param cms for accessing system resources
     * @param tagcontent key value for the resource bundle
     * @param doc reference to the A_CmsXmlContent object of the initiating XML document.
     * @param userObject must ba a <code>java.util.Hashtable</code> with request parameters
     * @return String with the version information of this OpenCms instance
     * @throws CmsException in case of errors processing the template
     */
    public Object workplaceUri(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) 
    throws CmsException {
        try {
            cms.readResource(CmsWorkplaceAction.C_JSP_WORKPLACE_URI);
            return OpenCms.getLinkManager().substituteLink(cms, CmsWorkplaceAction.C_JSP_WORKPLACE_URI);
        } catch (CmsSecurityException se) {
            return OpenCms.getLinkManager().substituteLink(cms, CmsWorkplaceAction.C_JSP_WORKPLACE_URI);
        } catch (CmsException ce) {
            // return default xml uri
        }
        return OpenCms.getLinkManager().substituteLink(cms, CmsWorkplaceAction.C_XML_WORKPLACE_URI);
    }    

    /**
     * Prevent caching of this template
     *
     * @param cms for accessing system resources
     * @param templateFile filename of the template file
     * @param elementName element name of this template in our parent template.
     * @param parameters hash with all template class parameters
     * @param templateSelector template section that should be processed
     * @return false
     */
    public boolean isCacheable(CmsObject cms, String templateFile, String elementName,
            Hashtable parameters, String templateSelector) {
        return false;
    }
    
    /**
     * Prevent caching of this template in the element cache
     *
     * @param cms for accessing system resources
     * @param templateFile filename of the template file
     * @param elementName element name of this template in our parent template.
     * @param parameters hash with all template class parameters
     * @param templateSelector template section that should be processed
     * @return <code>new CmsCacheDirectives(false)</code>
     */
    public CmsCacheDirectives getCacheDirectives(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
        return new CmsCacheDirectives(false);
    }    
}
