/*
* File   : $Source: /alkacon/cvs/opencms/modules/org.opencms.legacy/src/com/opencms/defaults/Attic/A_CmsBackoffice.java,v $
* Date   : $Date: 2005/05/16 17:45:08 $
* Version: $Revision: 1.1 $
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

package com.opencms.defaults;

import org.opencms.db.CmsUserSettings;
import org.opencms.file.CmsGroup;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsUser;
import org.opencms.i18n.CmsEncoder;
import org.opencms.main.CmsException;
import org.opencms.main.I_CmsConstants;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsDateUtil;
import org.opencms.util.CmsUUID;

import com.opencms.core.I_CmsSession;
import com.opencms.defaults.master.CmsPlausibilizationException;
import com.opencms.legacy.CmsXmlTemplateLoader;
import com.opencms.template.A_CmsXmlContent;
import com.opencms.template.CmsXmlTemplateFile;
import com.opencms.workplace.CmsWorkplaceDefault;
import com.opencms.workplace.CmsXmlLanguageFile;
import com.opencms.workplace.CmsXmlWpTemplateFile;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/**
 * Abstract class for generic backoffice display. It automatically
 * generates the <ul><li>head section with filters and buttons,</li>
 *                  <li>body section with the table data,</li>
 *                  <li>lock states of the entries. if there are any,</li>
 *                  <li>delete dialog,</li>
 *                  <li>lock dialog.</li></ul>
 * calls the <ul><li>edit dialog of the calling backoffice class</li>
 *              <li>new dialog of the calling backoffice class</li></ul>
 * using the content definition class defined by the getContentDefinition method.
 * The methods and data provided by the content definition class
 * is accessed by reflection. This way it is possible to re-use
 * this class for any content definition class, that just has
 * to extend the A_CmsContentDefinition class!
 * Creation date: (27.10.00 10:04:42)
 * 
 * @author Michael Knoll
 * @author Michael Emmerich
 * @version $Revision: 1.1 $
 * 
 * @deprecated Will not be supported past the OpenCms 6 release.
 */
public abstract class A_CmsBackoffice extends CmsWorkplaceDefault {

    /** Value for state: not locked. */
    public static int C_NOT_LOCKED = -1;
    
    /** Value for no access. */
    public static int C_NO_ACCESS = -2;

    private static String C_DEFAULT_SELECTOR = "(default)";
    private static String C_DONE_SELECTOR = "done";

    /** The style for unchanged files or folders. */
    private static final String C_STYLE_UNCHANGED = "dateingeandert";

    /** The style for files or folders not in project. */
    private static final String C_STYLE_NOTINPROJECT = "dateintprojekt";

    /** The style for new files or folders. */
    private static final String C_STYLE_NEW = "dateineu";

    /** The style for deleted files or folders. */
    private static final String C_STYLE_DELETED = "dateigeloescht";

    /** The style for changed files or folders. */
    private static final String C_STYLE_CHANGED = "dateigeaendert";

    /** Default value of permission.*/
    protected static final int C_DEFAULT_PERMISSIONS = 383;

    /** Possible accessflags. */
    protected static final String[] C_ACCESS_FLAGS = {"1", "2", "4", "8", "16", "32", "64", "128", "256"};

    /**
    * Gets the backoffice url of the module.
    * 
    * @param cms the cms object
    * @param tagcontent the tag body content
    * @param doc the xml document
    * @param userObject additional parameter values
    * @return A string with the backoffice url
    * @throws Exception if something goes wrong
    */
    public abstract String getBackofficeUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws Exception;
    /**
    * Gets the create url of the module.
    * 
    * @param cms the cms object
    * @param tagcontent the tag body content
    * @param doc the xml document
    * @param userObject additional parameter values
    * @return a string with the create url
    * @throws Exception if something goes wrong
    */
    public abstract String getCreateUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws Exception;

    /**
    * Gets the edit url of the module.
    * 
    * @param cms the cms object
    * @param tagcontent the tag body content
    * @param doc the xml document
    * @param userObject additional parameter values
    * @return A string with the edit url
    * @throws Exception if something goes wrong
    */
    public abstract String getEditUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws Exception;

    /**
    * Gets the edit url of the module.
    * 
    * @param cms the cms object
    * @param tagcontent the tag body content
    * @param doc the xml document
    * @param userObject additional parameter values
    * @return A string with the edit url
    * @throws Exception if something goes wrong
    */
    public String getDeleteUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws Exception {

        return getBackofficeUrl(cms, tagcontent, doc, userObject);
    }

    /**
    * Gets the undelete url of the module.
    * 
    * @param cms the cms object
    * @param tagcontent the tag body content
    * @param doc the xml document
    * @param userObject additional parameter values
    * @return A string with the undelete url
    * @throws Exception if something goes wrong
    */
    public String getUndeleteUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws Exception {

        return getBackofficeUrl(cms, tagcontent, doc, userObject);
    }

    /**
    * Gets the publish url of the module.
    * 
    * @param cms the cms object
    * @param tagcontent the tag body content
    * @param doc the xml document
    * @param userObject additional parameter values
    * @return A string with the publish url
    * @throws Exception if something goes wrong
    */
    public String getPublishUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws Exception {

        return getBackofficeUrl(cms, tagcontent, doc, userObject);
    }

    /**
    * Gets the history url of the module.
    * 
    * @param cms the cms object
    * @param tagcontent the tag body content
    * @param doc the xml document
    * @param userObject additional parameter values
    * @return A string with the history url
    * @throws Exception if something goes wrong
    */
    public String getHistoryUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws Exception {

        return getBackofficeUrl(cms, tagcontent, doc, userObject);
    }

    /**
    * Gets the redirect url of the module. This URL is called, when an entry of the file list is selected.
    * 
    * @param cms the cms object
    * @param tagcontent the tag body content
    * @param doc the xml document
    * @param userObject additional parameter values
    * @return a string with the  url
    * @throws Exception if something goes wrong
    */
    public String getUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws Exception {
        return "";
    }

    /**
    * Gets the setup url of the module. This is the url of the setup page for this module.
    * 
    * @param cms the cms object
    * @param tagcontent the tag body content
    * @param doc the xml document
    * @param userObject additional parameter values
    * @return a string with the  setup url
    * @throws Exception if something goes wrong
    */
    public String getSetupUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws Exception {
        return "";
    }

    /**
    * Gets the preview url of the module. This is the url of the preview page for this module.
    * 
    * @param cms the cms object
    * @param tagcontent the tag body content
    * @param doc the xml document
    * @param userObject additional parameter values
    * @return a string with the  setup url
    * @throws Exception if something goes wrong
    */
    public String getPreviewUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws Exception {
        return "";
    }

    /**
     * Gets the content of a given template file.
     * This method displays any content provided by a content definition
     * class on the template. The used backoffice class does not need to use a
     * special getContent method. It just has to extend the methods of this class!
     * Using reflection, this method creates the table headline and table content
     * with the layout provided by the template automatically!
     * 
     * @param cms A_CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file
     * @param elementName <em>not used here</em>.
     * @param parameters <em>not used here</em>.
     * @param templateSelector template section that should be processed.
     * @return Processed content of the given template file.
     * @throws CmsException if something goes wrong
     */

    public byte[] getContent(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {

        //return var
        byte[] returnProcess = null;

        // the CD to be used
        A_CmsContentDefinition cd = null;

        // session will be created or fetched
        I_CmsSession session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
        session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
        //create new workplace templatefile object
        CmsXmlWpTemplateFile template = new CmsXmlWpTemplateFile(cms, templateFile);
        //get parameters
        String selectBox = (String)parameters.get("selectbox");
        String filterParam = (String)parameters.get("filterparameter");
        String id = (String)parameters.get("id");
        String idlock = (String)parameters.get("idlock");
        String iddelete = (String)parameters.get("iddelete");
        String idedit = (String)parameters.get("idedit");
        String action = (String)parameters.get("action");
        String parentId = (String)parameters.get("parentId");
        String ok = (String)parameters.get("ok");
        String setaction = (String)parameters.get("setaction");
        String idundelete = (String)parameters.get("idundelete");
        String idpublish = (String)parameters.get("idpublish");
        String idhistory = (String)parameters.get("idhistory");
        String idpermissions = (String)parameters.get("idpermissions");
        String idcopy = (String)parameters.get("idcopy");
        // debug-code
        /*
                System.err.println("### "+this.getContentDefinitionClass().getName());
                System.err.println("### PARAMETERS");
                Enumeration  enu=parameters.keys();
                while (enu.hasMoreElements()) {
                  String a=(String)enu.nextElement();
                  String b=parameters.get(a).toString();
                  System.err.println("## "+a+" -> "+b);
                }
        
                System.err.println("");
                System.err.println("+++ SESSION");
                String[] ses=session.getValueNames();
                for (int i=0;i<ses.length;i++) {
                  String a=ses[i];
                  String b=session.getValue(a).toString();
                  System.err.println("++ "+a+" -> "+b);
                }
                System.err.println("");
                System.err.println("-------------------------------------------------");
        */

        String hasFilterParam = (String)session.getValue("filterparameter");
        template.setData("filternumber", "0");

        //change filter
        if ((hasFilterParam == null) && (filterParam == null) && (setaction == null)) {
            if (selectBox != null) {
                session.putValue("filter", selectBox);
                template.setData("filternumber", selectBox);
            }
        } else {
            template.setData("filternumber", (String)session.getValue("filter"));
        }

        //move id values to id, remove old markers
        if (idlock != null) {
            id = idlock;
            session.putValue("idlock", idlock);
            session.removeValue("idedit");
            session.removeValue("idnew");
            session.removeValue("iddelete");
            session.removeValue("idundelete");
            session.removeValue("idpublish");
            session.removeValue("idhistory");
            session.removeValue("idpermissions");
            session.removeValue("idcopy");
        }
        if (idedit != null) {
            id = idedit;
            session.putValue("idedit", idedit);
            session.removeValue("idlock");
            session.removeValue("idnew");
            session.removeValue("iddelete");
            session.removeValue("idundelete");
            session.removeValue("idpublish");
            session.removeValue("idhistory");
            session.removeValue("idpermissions");
            session.removeValue("idcopy");
        }
        if (iddelete != null) {
            id = iddelete;
            session.putValue("iddelete", iddelete);
            session.removeValue("idedit");
            session.removeValue("idnew");
            session.removeValue("idlock");
            session.removeValue("idundelete");
            session.removeValue("idpublish");
            session.removeValue("idhistory");
            session.removeValue("idpermissions");
            session.removeValue("idcopy");
        }
        if (idundelete != null) {
            id = idundelete;
            session.putValue("idundelete", idundelete);
            session.removeValue("idedit");
            session.removeValue("idnew");
            session.removeValue("idlock");
            session.removeValue("iddelete");
            session.removeValue("idpublish");
            session.removeValue("idhistory");
            session.removeValue("idpermissions");
            session.removeValue("idcopy");
        }
        if (idpublish != null) {
            id = idpublish;
            session.putValue("idpublish", idpublish);
            session.removeValue("idedit");
            session.removeValue("idnew");
            session.removeValue("idlock");
            session.removeValue("iddelete");
            session.removeValue("idundelete");
            session.removeValue("idhistory");
            session.removeValue("idpermissions");
            session.removeValue("idcopy");
        }

        if (idhistory != null) {
            id = idhistory;
            session.putValue("idhistory", idhistory);
            session.removeValue("idedit");
            session.removeValue("idnew");
            session.removeValue("idlock");
            session.removeValue("iddelete");
            session.removeValue("idundelete");
            session.removeValue("idpublish");
            session.removeValue("idpermissions");
            session.removeValue("idcopy");
        }

        if (idpermissions != null) {
            id = idpermissions;
            session.putValue("idpermissions", idpermissions);
            session.removeValue("idedit");
            session.removeValue("idnew");
            session.removeValue("idlock");
            session.removeValue("iddelete");
            session.removeValue("idundelete");
            session.removeValue("idpublish");
            session.removeValue("idhistory");
            session.removeValue("idcopy");
        }

        if (idcopy != null) {
            id = idcopy;
            session.putValue("idcopy", idcopy);
            session.removeValue("idedit");
            session.removeValue("idnew");
            session.removeValue("idlock");
            session.removeValue("iddelete");
            session.removeValue("idundelete");
            session.removeValue("idpublish");
            session.removeValue("idhistory");
            session.removeValue("idpermissions");
        }

        if ((id != null) && (id.equals("new"))) {
            session.putValue("idnew", id);
            session.removeValue("idedit");
            session.removeValue("iddelete");
            session.removeValue("idlock");
            session.removeValue("idundelete");
            session.removeValue("idpublish");
            session.removeValue("idhistory");
            session.removeValue("idpermissions");
            session.removeValue("idcopy");
        }
        //get marker id from session
        String idsave = (String)session.getValue("idsave");
        if (ok == null) {

            idsave = null;
        }

        if (parentId != null) {
            session.putValue("parentId", parentId);
        }

        //get marker for accessing the new dialog

        // --- This is the part when getContentNew is called ---
        //access to new dialog
        if ((id != null) && (id.equals("new")) || ((idsave != null) && (idsave.equals("new")))) {
            if (idsave != null) {
                parameters.put("id", idsave);
            }
            if (id != null) {
                parameters.put("id", id);
                session.putValue("idsave", id);
            }
            return getContentNewInternal(cms, id, cd, session, template, elementName, parameters, templateSelector);

            // --- This was the part when getContentNew is called ---
        }
        //go to the appropriate getContent methods
        if ((id == null) && (idsave == null) && (action == null) && (idlock == null) && (iddelete == null) && (idedit == null) && (idundelete == null) && (idpublish == null) && (idhistory == null) && (idpermissions == null) && (idcopy == null)) {
            //process the head frame containing the filter
            returnProcess = getContentHead(cms, template, elementName, parameters, templateSelector);
            //finally return processed data
            return returnProcess;
        } else {
            //process the body frame containing the table
            if (action == null) {
                action = "";
            }
            if (action.equalsIgnoreCase("list")) {
                //process the list output
                // clear "idsave" here in case user verification of data failed and input has to be shown again ...
                session.removeValue("idsave");
                if (isExtendedList()) {
                    returnProcess = getContentExtendedList(cms, template, elementName, parameters, templateSelector);
                } else {
                    returnProcess = getContentList(cms, template, elementName, parameters, templateSelector);
                }
                //finally return processed data
                return returnProcess;
            } else {
                // --- This is the part where getContentEdit is called ---

                //get marker for accessing the edit dialog
                String ideditsave = (String)session.getValue("idedit");
                //go to the edit dialog
                if ((idedit != null) || (ideditsave != null)) {
                    if (idsave != null) {
                        parameters.put("id", idsave);
                    }
                    if (id != null) {
                        parameters.put("id", id);
                        session.putValue("idsave", id);
                    }
                    return getContentEditInternal(cms, id, cd, session, template, elementName, parameters, templateSelector);

                    // --- This was the part where getContentEdit is called ---

                } else {
                    //store id parameters for delete and lock
                    if (idsave != null) {
                        parameters.put("id", idsave);
                        session.removeValue("idsave");
                    } else {
                        parameters.put("id", id);
                        session.putValue("idsave", id);
                    }
                    //check if the cd should be undeleted
                    if (idundelete != null) {
                        returnProcess = getContentUndelete(cms, template, elementName, parameters, templateSelector);
                        return returnProcess;
                    }
                    //get marker for accessing the publish dialog
                    //check if the cd should be published
                    String idpublishsave = (String)session.getValue("idpublish");
                    if (idpublish != null || idpublishsave != null) {
                        returnProcess = getContentDirectPublish(cms, template, elementName, parameters, templateSelector);
                        return returnProcess;
                    }
                    //get marker for accessing the history dialog
                    //check if the history of cd should be dispayed
                    String idhistorysave = (String)session.getValue("idhistory");
                    if (idhistory != null || idhistorysave != null) {
                        returnProcess = getContentHistory(cms, template, elementName, parameters, templateSelector);
                        return returnProcess;
                    }

                    //get marker for accessing the change permissions dialog
                    //check if the permissions of cd should be dispayed
                    String idpermissionssave = (String)session.getValue("idpermissions");
                    if (idpermissions != null || idpermissionssave != null) {
                        returnProcess = getContentPermissions(cms, template, elementName, parameters, templateSelector);
                        return returnProcess;
                    }

                    //get marker for accessing the copy dialog
                    //check if the permissions of cd should be dispayed
                    String idcopysave = (String)session.getValue("idcopy");
                    if (idcopy != null || idcopysave != null) {
                        returnProcess = getContentCopy(cms, template, elementName, parameters, templateSelector);
                        return returnProcess;
                    }

                    //get marker for accessing the delete dialog
                    String iddeletesave = (String)session.getValue("iddelete");
                    //access delete dialog
                    if (((iddelete != null) || (iddeletesave != null)) && (idlock == null)) {
                        returnProcess = getContentDelete(cms, template, elementName, parameters, templateSelector);
                        return returnProcess;
                    } else {
                        //access lock dialog
                        returnProcess = getContentLock(cms, template, elementName, parameters, templateSelector);
                        //finally return processed data
                        return returnProcess;
                    }
                }
            }
        }
    }

    /**
    * Gets the content definition class
    * @return class content definition class
    * Must be implemented in the extending backoffice class!
    */
    public abstract Class getContentDefinitionClass();

    /**
    * Gets the content definition class method constructor
    * 
    * @param cms the cms object
    * @param contentClass the content class
    * @param contentId the id of the content
    * @return content definition object
    */
    protected Object getContentDefinition(CmsObject cms, Class contentClass, CmsUUID contentId) {
        Object o = null;
        try {
            Constructor c = contentClass.getConstructor(new Class[] {CmsObject.class, CmsUUID.class});
            o = c.newInstance(new Object[] {cms, contentId});
        } catch (InvocationTargetException ite) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Invocation target exception", ite);
            }
        } catch (NoSuchMethodException nsm) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Requested method was not found", nsm);
            }
        } catch (InstantiationException ie) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("The reflected class is abstract", ie);
            }
        } catch (Exception e) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Other exception", e);
            }
        }
        return o;
    }

    /**
     * Gets the content definition class method constructor.<p>
     * 
     * @param cms the cms object
     * @param cdClass the content definition class
     * @return content definition object
     */
    protected Object getContentDefinition(CmsObject cms, Class cdClass) {
        Object o = null;
        try {
            //Constructor c = cdClass.getConstructor(new Class[] {CmsObject.class, String.class});
            Constructor c = cdClass.getConstructor(new Class[] {CmsObject.class});
            o = c.newInstance(new Object[] {cms});
        } catch (InvocationTargetException ite) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Invocation target exception", ite);
            }
        } catch (NoSuchMethodException nsm) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Requested method was not found", nsm);
            }
        } catch (InstantiationException ie) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("The reflected class is abstract", ie);
            }
        } catch (Exception e) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Other exception", e);
            }
        }
        return o;
    }

    /**
    * Gets the content definition class method constructor.<p>
    * 
    * @param cms the cms object
    * @param cdClass the content definition class
    * @param id the id of the content
    * @return content definition object
    */
    protected Object getContentDefinition(CmsObject cms, Class cdClass, String id) {
        Object o = null;
        try {
            Constructor c = cdClass.getConstructor(new Class[] {CmsObject.class, String.class});
            o = c.newInstance(new Object[] {cms, id});
        } catch (InvocationTargetException ite) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Invocation target exception", ite);
            }
        } catch (NoSuchMethodException nsm) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Requested method was not found", nsm);
            }
        } catch (InstantiationException ie) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("The reflected class is abstract", ie);
            }
        } catch (Exception e) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Other exception", e);
            }
        }
        return o;
    }

    /**
     * @param cms A CmsObject to read the user with
     * @param userId The id of the user to read
     * @return The name of the user, or the id (as a String) in case the user has been deleted
     */
    public String readSaveUserName(CmsObject cms, CmsUUID userId) {
        String userName = null;
        try {
            userName = cms.readUser(userId).getName();
        } catch (Exception e) {
            userName = "" + userId;
        }
        return userName;
    }

    /**
    * @param cms A CmsObject to read the group with
    * @param groupId The id of the group to read
    * @return The name of the group, or the id (as a String) in case the group has been deleted
    */
    public String readSaveGroupName(CmsObject cms, CmsUUID groupId) {
        String groupName = null;
        try {
            groupName = cms.readGroup(groupId).getName();
        } catch (Exception e) {
            groupName = "" + groupId;
        }
        return groupName;
    }

    /**
    * Gets the content of a given template file.
    * <P>
    * While processing the template file the table entry
    * <code>entryTitle<code> will be displayed in the delete dialog
    *
    * @param cms A_CmsObject Object for accessing system resources
    * @param template the template file
    * @param elementName not used here
    * @param parameters get the parameters action for the button activity
    * and id for the used content definition instance object
    * @param templateSelector template section that should be processed.
    * @return Processed content of the given template file.
    * @throws CmsException if something goes wrong
    */

    public byte[] getContentDelete(CmsObject cms, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        // return var
        byte[] processResult = null;

        // session will be created or fetched
        I_CmsSession session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
        //get the class of the content definition
        Class cdClass = getContentDefinitionClass();

        //get (stored) id parameter
        String id = (String)parameters.get("id");
        if (id == null) {
            id = "";
        }

        // get value of hidden input field action
        String action = (String)parameters.get("action");

        //no button pressed, go to the default section!
        //delete dialog, displays the title of the entry to be deleted
        if (action == null || action.equals("")) {
            if (id != "") {
                //set template section
                templateSelector = "delete";

                //create new language file object
                CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);

                //get the dialog from the language file and set it in the template
                template.setData("deletetitle", lang.getLanguageValue("title.delete"));
                template.setData("deletedialog", lang.getLanguageValue("messagebox.delete"));
                template.setData("newsentry", id);
                template.setData("setaction", "default");
            }
            // confirmation button pressed, process data!
        } else {
            //set template section
            templateSelector = "done";
            //remove marker
            session.removeValue("iddelete");
            //delete the content definition instance
            CmsUUID contentId = new CmsUUID(id);
            //        try {
            //            idInteger = Integer.valueOf(id);
            //        } catch (Exception e) {
            //            //access content definition constructor by reflection
            //            Object o = null;
            //            o = getContentDefinition(cms, cdClass, id);
            //            //get delete method and delete content definition instance
            //            try {
            //                ((A_CmsContentDefinition) o).delete(cms);
            //            } catch (Exception e1) {
            //                if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
            //                    A_OpenCms.log(this);
            //                }
            //                templateSelector = "deleteerror";
            //                template.setData("deleteerror", e1.getMessage());
            //            }
            //            //finally start the processing
            //            processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
            //            return processResult;
            //        }

            //access content definition constructor by reflection
            Object o = null;
            o = getContentDefinition(cms, cdClass, contentId);
            //get delete method and delete content definition instance
            try {
                ((A_CmsContentDefinition)o).delete(cms);
            } catch (Exception e) {
                templateSelector = "deleteerror";
                template.setData("deleteerror", e.getMessage());
            }
        }

        //finally start the processing
        processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
        return processResult;
    }

    /**
     * Gets the content of a given template file.
     * <P>
     * While processing the template file the table entry
     * <code>entryTitle<code> will be displayed in the delete dialog
     *
     * @param cms A_CmsObject Object for accessing system resources
     * @param template the template
     * @param elementName not used here
     * @param parameters get the parameters action for the button activity
     * and id for the used content definition instance object
     * @param templateSelector template section that should be processed.
     * @return Processed content of the given template file.
     * @throws CmsException if something goes wrong
     */
    public byte[] getContentUndelete(CmsObject cms, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        //return var
        byte[] processResult = null;

        // session will be created or fetched
        I_CmsSession session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
        //get the class of the content definition
        Class cdClass = getContentDefinitionClass();

        //get (stored) id parameter
        String id = (String)parameters.get("id");
        if (id == null) {
            id = "";
        }

        //set template section
        templateSelector = "done";
        //remove marker
        session.removeValue("idundelete");
        //undelete the content definition instance
        CmsUUID contentId = new CmsUUID(id);
        //        Integer idInteger = null;
        //        try {
        //            idInteger = Integer.valueOf(id);
        //        } catch (Exception e) {
        //            //access content definition constructor by reflection
        //            Object o = null;
        //            o = getContentDefinition(cms, cdClass, id);
        //            //get undelete method and undelete content definition instance
        //            try {
        //                ((I_CmsExtendedContentDefinition) o).undelete(cms);
        //            } catch (Exception e1) {
        //                if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        //                    A_OpenCms.log(this);
        //                }
        //                templateSelector = "undeleteerror";
        //                template.setData("undeleteerror", e1.getMessage());
        //            }
        //            //finally start the processing
        //            processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
        //            return processResult;
        //        }
        //access content definition constructor by reflection
        Object o = null;
        o = getContentDefinition(cms, cdClass, contentId);
        //get undelete method and undelete content definition instance
        try {
            ((I_CmsExtendedContentDefinition)o).undelete(cms);
        } catch (Exception e) {
            templateSelector = "undeleteerror";
            template.setData("undeleteerror", e.getMessage());
        }

        //finally start the processing
        processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
        return processResult;
    }

    /**
     * Gets the content of a given template file.
     * <P>
     * While processing the template file the table entry
     * <code>entryTitle<code> will be displayed in the delete dialog
     *
     * @param cms A_CmsObject Object for accessing system resources
     * @param template the template file
     * @param elementName not used here
     * @param parameters get the parameters action for the button activity
     * and id for the used content definition instance object
     * @param templateSelector template section that should be processed.
     * @return Processed content of the given template file.
     * @throws CmsException if something goes wrong
     */
    public byte[] getContentCopy(CmsObject cms, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        //return var
        byte[] processResult = null;

        // session will be created or fetched
        I_CmsSession session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
        //get the class of the content definition
        Class cdClass = getContentDefinitionClass();

        //get (stored) id parameter
        String id = (String)parameters.get("id");
        if (id == null) {
            id = "";
        }
        // get value of hidden input field action
        String action = (String)parameters.get("action");

        //no button pressed, go to the default section!
        //copy dialog, displays information of the entry to be copy
        if (action == null || action.equals("")) {
            if (id != "") {
                //access content definition constructor by reflection
                Object o = getContentDefinition(cms, cdClass, new CmsUUID(id));
                // get owner and group of content definition
                String curOwner = readSaveUserName(cms, ((I_CmsExtendedContentDefinition)o).getOwner());
                String curGroup = readSaveGroupName(cms, ((I_CmsExtendedContentDefinition)o).getGroupId());

                //set template section
                templateSelector = "copy";

                //get the dialog from the language file and set it in the template
                template.setData("username", curOwner);
                template.setData("groupname", curGroup);
                template.setData("id", id);
                template.setData("setaction", "default");
            }
            // confirmation button pressed, process data!
        } else {
            //set template section
            templateSelector = "done";
            //remove marker
            session.removeValue("idcopy");
            //copy the content definition instance
            CmsUUID contentId = new CmsUUID(id);
            Object o = getContentDefinition(cms, cdClass, contentId);

            //            Integer contentId = null;
            //            Object o = null;
            //            try {
            //                contentId = Integer.valueOf(id);
            //                //access content definition constructor by reflection
            //                o = getContentDefinition(cms, cdClass, contentId);
            //            } catch (Exception e) {
            //                //access content definition constructor by reflection
            //                o = getContentDefinition(cms, cdClass, id);
            //            }

            //get copy method and copy content definition instance
            try {
                ((I_CmsExtendedContentDefinition)o).copy(cms);
            } catch (Exception e) {
                templateSelector = "copyerror";
                template.setData("copyerror", e.getMessage());
            }
        }
        //finally start the processing
        processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
        return processResult;
    }

    /**
     * Gets the content of a given template file.
     * <P>
     * While processing the template file the table entry
     * <code>entryTitle<code> will be displayed in the direct publish dialog
     *
     * @param cms A_CmsObject Object for accessing system resources
     * @param template the template
     * @param elementName not used here
     * @param parameters get the parameters action for the button activity
     * and id for the used content definition instance object
     * @param templateSelector template section that should be processed.
     * @return Processed content of the given template file.
     * @throws CmsException if something goes wrong
     */
    public byte[] getContentDirectPublish(CmsObject cms, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        //return var
        byte[] processResult = null;

        //create new language file object
        CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);

        if (cms.isAdmin() || cms.isManagerOfProject()) {
            // session will be created or fetched
            I_CmsSession session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
            //get the class of the content definition
            Class cdClass = getContentDefinitionClass();

            //get (stored) id parameter
            String id = (String)parameters.get("id");
            if (id == null) {
                id = "";
            }

            // get value of hidden input field action
            String action = (String)parameters.get("action");

            //no button pressed, go to the default section!
            //publish dialog, displays the title of the entry to be published
            if (action == null || action.equals("")) {
                if (id != "") {
                    //set template section
                    templateSelector = "publish";

                    //get the dialog from the langauge file and set it in the template
                    template.setData("publishtitle", lang.getLanguageValue("messagebox.title.publishresource"));
                    template.setData("publishdialog1", lang.getLanguageValue("messagebox.message1.publishresource"));
                    template.setData("newsentry", id);
                    template.setData("publishdialog2", lang.getLanguageValue("messagebox.message4.publishresource"));
                    template.setData("setaction", "default");
                }
                // confirmation button pressed, process data!
            } else {
                //set template section
                templateSelector = "done";
                //remove marker
                session.removeValue("idsave");
                //publish the content definition instance
                CmsUUID contentId = new CmsUUID(id);
                //                Integer contentId = null;
                //                try {
                //                    contentId = Integer.valueOf(id);
                //                } catch (Exception e) {
                //                    //access content definition constructor by reflection
                //                    Object o = null;
                //                    o = getContentDefinition(cms, cdClass, id);
                //                    //get publish method and publish content definition instance
                //                    try {
                //                        ((I_CmsExtendedContentDefinition) o).publishResource(cms);
                //                    } catch (Exception e1) {
                //                        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                //                            A_OpenCms.log(this);
                //                        }
                //                        templateSelector = "publisherror";
                //                        template.setData("publisherror", e1.getMessage());
                //                    }
                //                    //finally start the processing
                //                    processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
                //                    return processResult;
                //                }

                //access content definition constructor by reflection
                Object o = null;
                o = getContentDefinition(cms, cdClass, contentId);
                //get publish method and publish content definition instance
                try {
                    ((I_CmsExtendedContentDefinition)o).publishResource(cms);
                } catch (Exception e) {
                    templateSelector = "publisherror";
                    template.setData("publisherror", e.getMessage());
                }
            }
        } else {
            templateSelector = "publisherror";
            template.setData("publisherror", lang.getLanguageValue("error.message.publishresource") + "<br>" + lang.getLanguageValue("error.reason.publishresource"));
        }

        //finally start the processing
        processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
        return processResult;
    }

    /**
     * Gets the content of a given template file.
     * <P>
     * While processing the template file the table entry
     * <code>entryTitle<code> will be displayed in the history dialog
     *
     * @param cms A_CmsObject Object for accessing system resources
     * @param template the template
     * @param elementName not used here
     * @param parameters get the parameters action for the button activity
     * and id for the used content definition instance object
     * @param templateSelector template section that should be processed.
     * @return Processed content of the given template file.
     * @throws CmsException if something goes wrong
     */
    public byte[] getContentHistory(CmsObject cms, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        //return var
        byte[] processResult = null;

        // session will be created or fetched
        I_CmsSession session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
        //get the class of the content definition
        Class cdClass = getContentDefinitionClass();

        //get (stored) id parameter
        String id = (String)parameters.get("id");
        if (id == null) {
            id = "";
        }

        // get value of hidden input field action
        String action = (String)parameters.get("action");
        //no button pressed, go to the default section!
        //history dialog, displays the versions of the cd in the history
        if (action == null || action.equals("")) {
            if (id != "") {
                //set template section
                templateSelector = "history";
                //create new language file object
                CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);
                //get the dialog from the langauge file and set it in the template
                template.setData("historytitle", lang.getLanguageValue("messagebox.title.history"));
                // build the history list
                template.setData("id", id);
                template.setData("setaction", "detail");
            } else {
                //set template section
                templateSelector = "done";
                //remove marker
                session.removeValue("idhistory");
            }
            // confirmation button pressed, process data!
        } else if (action.equalsIgnoreCase("detail")) {
            String versionId = (String)parameters.get("version");
            if (versionId != null && !"".equals(versionId)) {
                templateSelector = "historydetail";
                //access content definition constructor by reflection
                Object o = null;
                o = getContentDefinition(cms, cdClass, new CmsUUID(id));
                //get the version from history
                try {
                    I_CmsExtendedContentDefinition curVersion = (I_CmsExtendedContentDefinition) ((I_CmsExtendedContentDefinition)o).getVersionFromHistory(cms, Integer.parseInt(versionId));
                    String projectName = "";
                    String projectDescription = "";
                    String userName = "";
                    try {
                        CmsProject theProject = cms.readBackupProject(curVersion.getLockedInProject());
                        projectName = theProject.getName();
                        projectDescription = theProject.getDescription();
                    } catch (CmsException ex) {
                        projectName = "";
                    }
                    try {
                        CmsUser theUser = cms.readUser(curVersion.getLastModifiedBy());
                        userName = theUser.getName() + " " + theUser.getFirstname() + " " + theUser.getLastname();
                    } catch (CmsException ex) {
                        userName = curVersion.getLastModifiedByName();
                    }
                    template.setData("histproject", projectName);
                    template.setData("version", versionId);
                    template.setData("id", id);
                    template.setData("histid", curVersion.getId().toString());
                    template.setData("histtitle", curVersion.getTitle());
                    template.setData("histlastmodified", CmsDateUtil.getDateTimeShort(curVersion.getDateLastModified()));
                    template.setData("histpublished", CmsDateUtil.getDateTimeShort(curVersion.getDateCreated()));
                    template.setData("histmodifiedby", userName);
                    template.setData("histdescription", projectDescription);
                    CmsUUID curUser = cms.getRequestContext().currentUser().getId();
                    int curProject = cms.getRequestContext().currentProject().getId();
                    if (((A_CmsContentDefinition)o).getLockstate().equals(curUser) && ((I_CmsExtendedContentDefinition)o).getLockedInProject() == curProject) {
                        // enable restore button
                        template.setData("BUTTONRESTORE", template.getProcessedDataValue("ENABLERESTORE", this));
                        template.setData("setaction", "restore");
                    } else {
                        template.setData("BUTTONRESTORE", template.getProcessedDataValue("DISABLERESTORE", this));
                        template.setData("setaction", "");
                    }
                } catch (Exception e) {
                    if (OpenCms.getLog(this).isWarnEnabled()) {
                        OpenCms.getLog(this).warn("Backoffice: history method caused an exception", e);
                    }
                    templateSelector = "historyerror";
                    template.setData("historyerror", e.getMessage());
                    //remove marker
                    session.removeValue("idhistory");
                }
            } else {
                // no version selected
                //set template section
                templateSelector = "history";
                //create new language file object
                CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);
                //get the dialog from the langauge file and set it in the template
                template.setData("historytitle", lang.getLanguageValue("messagebox.title.history"));
                // build the history list
                template.setData("id", id);
                template.setData("setaction", "detail");
            }
        } else if (action.equalsIgnoreCase("restore")) {
            String versionId = (String)parameters.get("version");
            //set template section
            templateSelector = "history";
            //create new language file object
            CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);
            //get the dialog from the langauge file and set it in the template
            template.setData("historytitle", lang.getLanguageValue("messagebox.title.history"));
            // build the history list
            template.setData("id", id);
            template.setData("setaction", "detail");
            if (versionId != null && !"".equals(versionId)) {
                //access content definition constructor by reflection
                Object o = null;
                o = getContentDefinition(cms, cdClass, new CmsUUID(id));
                //get restore method and restore content definition instance
                try {
                    ((I_CmsExtendedContentDefinition)o).restore(cms, Integer.parseInt(versionId));
                } catch (Exception e) {
                    if (OpenCms.getLog(this).isWarnEnabled()) {
                        OpenCms.getLog(this).warn("Restore method caused an exception", e);
                    }
                    templateSelector = "historyerror";
                    template.setData("historyerror", e.getMessage());
                    //remove marker
                    session.removeValue("idhistory");
                }
            }
        }
        //finally start the processing
        processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
        return processResult;
    }

    /**
     * Gets all versions of the resource from the history.
     * <P>
     * The given vectors <code>names</code> and <code>values</code> will
     * be filled with the appropriate information to be used for building
     * a select box.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param lang the language file
     * @param names Vector to be filled with the appropriate values in this method.
     * @param values Vector to be filled with the appropriate values in this method.
     * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
     * @return Index representing the current value in the vectors.
     * @throws CmsException if something goes wrong
     */

    public Integer getHistory(CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values, Hashtable parameters) throws CmsException {
        CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
        String id = (String)parameters.get("id");
        if (id != null && !"".equals(id)) {
            Vector cdHistory = new Vector();
            //get the class of the content definition
            Class cdClass = getContentDefinitionClass();
            //access content definition constructor by reflection
            Object o = null;
            o = getContentDefinition(cms, cdClass, new CmsUUID(id));
            //get history method and return the vector of the versions
            try {
                cdHistory = ((I_CmsExtendedContentDefinition)o).getHistory(cms);
            } catch (Exception e) {
                if (OpenCms.getLog(this).isErrorEnabled()) {
                    OpenCms.getLog(this).error("History reading history for class " + cdClass.getName(), e);
                }
            }
            // fill the names and values
            for (int i = 0; i < cdHistory.size(); i++) {
                try {
                    I_CmsExtendedContentDefinition curCd = ((I_CmsExtendedContentDefinition)cdHistory.elementAt(i));
                    long updated = curCd.getDateCreated();
                    String userName = readSaveUserName(cms, curCd.getLastModifiedBy());
                    long lastModified = curCd.getDateLastModified();
                    String output = CmsDateUtil.getDateTimeShort(lastModified) + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + CmsDateUtil.getDateTimeShort(updated) + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + userName;
                    names.addElement(output);
                    values.addElement(curCd.getVersionId() + "");
                } catch (Exception e) {
                    if (OpenCms.getLog(this).isErrorEnabled()) {
                        OpenCms.getLog(this).error(e);
                    }
                }
            }
        }
        return new Integer(-1);
    }

    /**
     * Gets the content of a given template file.
     * <P>
     * While processing the template file the table entry
     * <code>entryTitle<code> will be displayed in the delete dialog
     *
     * @param cms A_CmsObject Object for accessing system resources
     * @param template the template
     * @param elementName not used here
     * @param parameters get the parameters action for the button activity
     * and id for the used content definition instance object
     * @param templateSelector template section that should be processed.
     * @return Processed content of the given template file.
     * @throws CmsException if something goes wrong
     */
    public byte[] getContentPermissions(CmsObject cms, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        //return var
        byte[] processResult = null;
        Object o = null;
        // session will be created or fetched
        I_CmsSession session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
        //get the class of the content definition
        Class cdClass = getContentDefinitionClass();

        // get mode of permission changing
        String mode = (String)parameters.get("chmode");
        if (mode == null || "".equals(mode)) {
            mode = "";
        }
        //get (stored) id parameter
        String id = (String)parameters.get("id");
        if (id == null) {
            id = "";
        }
        // get value of hidden input field action
        String action = (String)parameters.get("action");
        // get values of input fields
        CmsUUID newOwner = CmsUUID.getNullUUID();
        CmsUUID newGroup = CmsUUID.getNullUUID();
        String newOwnerString = (String)parameters.get("owner");
        if (newOwnerString != null && !"".equals(newOwnerString)) {
            newOwner = new CmsUUID(newOwnerString);
        }
        String newGroupString = (String)parameters.get("groupId");
        if (newGroupString != null && !"".equals(newGroupString)) {
            newGroup = new CmsUUID(newGroupString);
        }
        int newAccessFlags = getAccessValue(parameters);
        //no button pressed, go to the default section!
        //change permissions dialog, displays owner, group and access flags of the entry to be changed
        if (action == null || action.equals("")) {
            if (id != "" && mode != "") {
                //access content definition constructor by reflection
                o = getContentDefinition(cms, cdClass, new CmsUUID(id));
                //set template section
                templateSelector = mode;
                CmsUUID curOwner = ((I_CmsExtendedContentDefinition)o).getOwner();
                CmsUUID curGroup = ((I_CmsExtendedContentDefinition)o).getGroupId();
                int curAccessFlags = ((I_CmsExtendedContentDefinition)o).getAccessFlags();
                // set the values in the dialog
                this.setOwnerSelectbox(cms, template, curOwner);
                this.setGroupSelectbox(cms, template, curGroup);
                this.setAccessValue(template, curAccessFlags);
                template.setData("id", id);
                template.setData("setaction", mode);
            }
            // confirmation button pressed, process data!
        } else {
            //set template section
            templateSelector = "done";
            //remove marker
            session.removeValue("idpermissions");
            //change the content definition instance
            CmsUUID contentId = new CmsUUID(id);
            o = getContentDefinition(cms, cdClass, contentId);
            //            try {
            //                contentId = Integer.valueOf(id);
            //                //access content definition constructor by reflection
            //                o = getContentDefinition(cms, cdClass, contentId);
            //            } catch (Exception e) {
            //                //access content definition constructor by reflection
            //                o = getContentDefinition(cms, cdClass, id);
            //            }

            //get change method and change content definition instance
            try {
                if ("chown".equalsIgnoreCase(action) && !newOwner.isNullUUID()) {
                    ((I_CmsExtendedContentDefinition)o).chown(cms, newOwner);
                } else if ("chgrp".equalsIgnoreCase(action) && !newGroup.isNullUUID()) {
                    ((I_CmsExtendedContentDefinition)o).chgrp(cms, newGroup);
                } else if ("chmod".equalsIgnoreCase(action)) {
                    ((I_CmsExtendedContentDefinition)o).chmod(cms, newAccessFlags);
                }
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Changing permissions method caused an exception", e);
                }
                templateSelector = "permissionserror";
                template.setData("permissionserror", e.getMessage());
            }
        }
        //finally start the processing
        processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
        return processResult;
    }

    /**
    * Gets the content of a given template file.
    * <P>
    *
    * @param cms A_CmsObject Object for accessing system resources
    * @param template the template
    * @param elementName not used here
    * @param parameters get the parameters action for the button activity
    * and id for the used content definition instance object
    * and the author, title, text content for setting the new/changed data
    * @param templateSelector template section that should be processed.
    * @return Processed content of the given template file.
    * @throws CmsException if something goes wrong
    */

    private byte[] getContentHead(CmsObject cms, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {

        //return var
        byte[] processResult = null;
        //get the class of the content definition

        //init vars

        //create new or fetch existing session
        I_CmsSession session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
        String uri = cms.getRequestContext().getUri();
        String sessionSelectBoxValue = uri + "selectBoxValue";
        //get filter method from session
        String selectBoxValue = (String)parameters.get("selectbox");
        if (selectBoxValue == null) {
            // set default value
            if ((String)session.getValue(sessionSelectBoxValue) != null) {
                // came back from edit or something ... redisplay last filter
                selectBoxValue = (String)session.getValue(sessionSelectBoxValue);
            } else {
                // the very first time here...
                selectBoxValue = "0";
            }
        }
        boolean filterChanged = true;
        if (selectBoxValue.equals(session.getValue(sessionSelectBoxValue))) {
            filterChanged = false;
        } else {
            filterChanged = true;
        }

        //get vector of filter names from the content definition
        Vector filterMethods = getFilterMethods(cms);

        if (Integer.parseInt(selectBoxValue) >= filterMethods.size()) {
            // the stored seclectBoxValue is does not exist any more, ...
            selectBoxValue = "0";
        }

        session.putValue(sessionSelectBoxValue, selectBoxValue); // store in session for Selectbox!
        session.putValue("filter", selectBoxValue); // store filter in session for getContentList!

        String filterParam = (String)parameters.get("filterparameter");
        // create the key for the filterparameter in the session ... should be unique to avoid problems...
        String sessionFilterParam = uri + selectBoxValue + "filterparameter";
        //store filterparameter in the session, new enty for every filter of every url ...
        if (filterParam != null) {
            session.putValue(sessionFilterParam, filterParam);
        }

        //create appropriate class name with underscores for labels
        String moduleName = "";
        moduleName = getClass().toString(); //get name
        moduleName = moduleName.substring(5); //remove 'class' substring at the beginning
        moduleName = moduleName.trim();
        moduleName = moduleName.replace('.', '_'); //replace dots with underscores

        //create new language file object
        CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);
        //set labels in the template
        template.setData("filterlabel", lang.getLanguageValue(moduleName + ".label.filter"));
        template.setData("filterparameterlabel", lang.getLanguageValue(moduleName + ".label.filterparameter"));

        //no filter selected so far, store a default filter in the session
        if (selectBoxValue == null) {
            CmsFilterMethod defaultFilter = (CmsFilterMethod)filterMethods.firstElement();
            session.putValue("selectbox", defaultFilter.getFilterName());
        }

        // show param box ?
        CmsFilterMethod currentFilter = (CmsFilterMethod)filterMethods.elementAt(Integer.parseInt(selectBoxValue));
        if (currentFilter.hasUserParameter()) {
            if (filterChanged) {
                template.setData("filterparameter", currentFilter.getDefaultFilterParam());
                // access default in getContentList() ....
                session.putValue(sessionFilterParam, currentFilter.getDefaultFilterParam());
            } else if (filterParam != null) {
                template.setData("filterparameter", filterParam);
            } else {
                // redisplay after edit or something like this ...
                template.setData("filterparameter", (String)session.getValue(sessionFilterParam));
            }
            // check if there is only one filtermethod, do not show the selectbox then
            if (filterMethods.size() < 2) {
                // replace the selectbox with a simple text output
                CmsFilterMethod defaultFilter = (CmsFilterMethod)filterMethods.firstElement();
                template.setData("filtername", defaultFilter.getFilterName());
                template.setData("insertFilter", template.getProcessedDataValue("noSelectboxWithParam", this, parameters));
            } else {
                template.setData("insertFilter", template.getProcessedDataValue("selectboxWithParam", this, parameters));
            }
            template.setData("setfocus", template.getDataValue("focus"));
        } else {
            // check if there is only one filtermethod, do not show the selectbox then
            if (filterMethods.size() < 2) {
                // replace the selectbox with a simple text output
                CmsFilterMethod defaultFilter = (CmsFilterMethod)filterMethods.firstElement();
                template.setData("filtername", defaultFilter.getFilterName());
                template.setData("insertFilter", template.getProcessedDataValue("noSelectbox", this, parameters));
            } else {
                template.setData("insertFilter", template.getProcessedDataValue("singleSelectbox", this, parameters));
            }
        }

        //if getCreateUrl equals null, the "create new entry" button
        //will not be displayed in the template
        String createButton = null;
        try {
            createButton = getCreateUrl(cms, null, null, null);
        } catch (Exception e) { }
        if (createButton == null) {
            String cb = template.getDataValue("nowand");
            template.setData("createbutton", cb);
        } else {
            boolean buttonActiv = true;
            if (isExtendedList() && (cms.getRequestContext().currentProject().isOnlineProject())) {
                buttonActiv = false;
            }
            if (buttonActiv) {
                String cb = template.getProcessedDataValue("wand", this, parameters);
                template.setData("createbutton", cb);
            } else {
                String cb = template.getProcessedDataValue("deactivwand", this, parameters);
                template.setData("createbutton", cb);
            }
        }

        //if getSetupUrl is empty, the module setup button will not be displayed in the template.
        String setupButton = null;
        try {
            setupButton = getSetupUrl(cms, null, null, null);
        } catch (Exception e) { }
        if ((setupButton == null) || (setupButton.equals(""))) {
            String sb = template.getDataValue("nosetup");
            template.setData("setupbutton", sb);
        } else {
            String sb = template.getProcessedDataValue("setup", this, parameters);
            template.setData("setupbutton", sb);
        }

        //finally start the processing
        processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
        return processResult;
    }
    /**
     * Gets the content of a given template file.
     * This method displays any content provided by a content definition
     * class on the template. The used backoffice class does not need to use a
     * special getContent method. It just has to extend the methods of this class!
     * Using reflection, this method creates the table headline and table content
     * with the layout provided by the template automatically!
     * @param cms CmsObjectfor accessing system resources
     * @param template the template
     * @param elementName <em>not used here</em>.
     * @param parameters <em>not used here</em>.
     * @param templateSelector template section that should be processed.
     * @return Processed content of the given template file.
     * @throws CmsException if something goes wrong
     */
    private byte[] getContentList(CmsObject cms, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        //return var
        byte[] processResult = null;
        // session will be created or fetched
        I_CmsSession session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
        //get the class of the content definition
        Class cdClass = getContentDefinitionClass();

        //read value of the selected filter
        String filterMethodName = (String)session.getValue("filter");
        if (filterMethodName == null) {
            filterMethodName = "0";
        }

        String uri = cms.getRequestContext().getUri();
        String sessionFilterParam = uri + filterMethodName + "filterparameter";
        //read value of the inputfield filterparameter
        String filterParam = (String)session.getValue(sessionFilterParam);
        if (filterParam == "") {
            filterParam = null;
        }

        //change template to list section for data list output
        templateSelector = "list";

        //init vars
        String tableHead = "";
        String singleRow = "";
        String allEntrys = "";
        String entry = "";
        String url = "";
        int columns = 0;

        // get number of columns
        Vector columnsVector = new Vector();
        String fieldNamesMethod = "getFieldNames";
        Class paramClasses[] = {CmsObject.class};
        Object params[] = {cms};
        columnsVector = (Vector)getContentMethodObject(cms, cdClass, fieldNamesMethod, paramClasses, params);
        columns = columnsVector.size();

        //create appropriate class name with underscores for labels
        String moduleName = "";
        moduleName = getClass().toString(); //get name
        moduleName = moduleName.substring(5); //remove 'class' substring at the beginning
        moduleName = moduleName.trim();
        moduleName = moduleName.replace('.', '_'); //replace dots with underscores

        //create new language file object
        CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);

        //create tableheadline
        for (int i = 0; i < columns; i++) {
            tableHead += (template.getDataValue("tabledatabegin")) + lang.getLanguageValue(moduleName + ".label." + columnsVector.elementAt(i).toString().toLowerCase().trim()) + (template.getDataValue("tabledataend"));
        }
        //set template data for table headline content
        template.setData("tableheadline", tableHead);

        // get vector of filterMethods and select the appropriate filter method,
        // if no filter is appropriate, select a default filter get number of rows for output
        Vector tableContent = new Vector();
        try {
            Vector filterMethods = (Vector)cdClass.getMethod("getFilterMethods", new Class[] {CmsObject.class}).invoke(null, new Object[] {cms});
            CmsFilterMethod filterMethod = null;
            CmsFilterMethod filterName = (CmsFilterMethod)filterMethods.elementAt(Integer.parseInt(filterMethodName));
            filterMethodName = filterName.getFilterName();
            //loop trough the filter methods and set the chosen one
            for (int i = 0; i < filterMethods.size(); i++) {
                CmsFilterMethod currentFilter = (CmsFilterMethod)filterMethods.elementAt(i);
                if (currentFilter.getFilterName().equals(filterMethodName)) {
                    filterMethod = currentFilter;
                    break;
                }
            }

            // the chosen filter does not exist, use the first one!
            if (filterMethod == null) {
                filterMethod = (CmsFilterMethod)filterMethods.firstElement();
            }
            // now apply the filter with the cms object, the filter method and additional user parameters
            tableContent = (Vector)cdClass.getMethod("applyFilter", new Class[] {CmsObject.class, CmsFilterMethod.class, String.class}).invoke(null, new Object[] {cms, filterMethod, filterParam});
        } catch (InvocationTargetException ite) {
            //error occured while applying the filter
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Apply filter caused an InvocationTargetException", ite);
            }
            templateSelector = "error";
            template.setData("filtername", filterMethodName);
            while (ite.getTargetException() instanceof InvocationTargetException) {
                ite = ((InvocationTargetException)ite.getTargetException());
            }
            template.setData("filtererror", ite.getTargetException().getMessage());
            session.removeValue(sessionFilterParam);
            //session.removeValue("filter");
        } catch (NoSuchMethodException nsm) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Apply filter method was not found", nsm);
            }
            templateSelector = "error";
            template.setData("filtername", filterMethodName);
            template.setData("filtererror", nsm.getMessage());
            session.removeValue(sessionFilterParam);
            //session.removeValue("filterparameter");
        } catch (Exception e) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Apply filter: Other Exception", e);
            }
            templateSelector = "error";
            template.setData("filtername", filterMethodName);
            template.setData("filtererror", e.getMessage());
            session.removeValue(sessionFilterParam);
            //session.removeValue("filterparameter");
        }

        //get the number of rows
        int rows = tableContent.size();

        // get the field methods from the content definition
        Vector fieldMethods = new Vector();
        try {
            fieldMethods = (Vector)cdClass.getMethod("getFieldMethods", new Class[] {CmsObject.class}).invoke(null, new Object[] {cms});
        } catch (Exception exc) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("getFieldMethods caused an exception", exc);
            }
            templateSelector = "error";
            template.setData("filtername", filterMethodName);
            template.setData("filtererror", exc.getMessage());
        }

        // create output from the table data
        String fieldEntry = "";
        String id = "";
        for (int i = 0; i < rows; i++) {
            //init
            entry = "";
            singleRow = "";
            Object entryObject = new Object();
            entryObject = tableContent.elementAt(i); //cd object in row #i

            //set data of single row
            for (int j = 0; j < columns; j++) {
                fieldEntry = "+++ NO VALUE FOUND +++";
                // call the field methods
                Method getMethod = null;
                try {
                    getMethod = (Method)fieldMethods.elementAt(j);

                } catch (Exception e) {
                    if (OpenCms.getLog(this).isWarnEnabled()) {
                        OpenCms.getLog(this).warn("Could not get field method for " + (String)columnsVector.elementAt(j) + " - check for correct spelling", e);
                    }
                }
                try {
                    //apply methods on content definition object
                    Object fieldEntryObject = null;

                    fieldEntryObject = getMethod.invoke(entryObject, new Object[0]);

                    if (fieldEntryObject != null) {
                        fieldEntry = fieldEntryObject.toString();
                    } else {
                        fieldEntry = null;
                    }
                } catch (InvocationTargetException ite) {
                    if (OpenCms.getLog(this).isWarnEnabled()) {
                        OpenCms.getLog(this).warn("Backoffice content definition object caused an InvocationTargetException", ite);
                    }
                } catch (Exception e) {
                    if (OpenCms.getLog(this).isWarnEnabled()) {
                        OpenCms.getLog(this).warn("Backoffice content definition object: Other exception", e);
                    }
                }

                try {
                    id = ((A_CmsContentDefinition)entryObject).getUniqueId(cms);
                } catch (Exception e) {
                    if (OpenCms.getLog(this).isWarnEnabled()) {
                        OpenCms.getLog(this).warn("Backoffice: getUniqueId caused an Exception, e");
                    }
                }

                //insert unique id in contextmenue
                if (id != null) {
                    template.setData("uniqueid", id);
                }
                //insert table entry
                if (fieldEntry != null) {
                    try {
                        Vector v = new Vector();
                        v.addElement(new CmsUUID(id));
                        v.addElement(template);
                        url = getUrl(cms, null, null, v);
                    } catch (Exception e) {
                        if (OpenCms.getLog(this).isErrorEnabled()) {
                            OpenCms.getLog(this).error("Error getting URL for ID " + id, e);
                        }                      
                        
                        url = "";
                    }
                    if (!url.equals("")) {
                        // enable url
                        entry += (template.getDataValue("tabledatabegin")) + (template.getProcessedDataValue("url", this, parameters)) + fieldEntry + (template.getDataValue("tabledataend"));
                    } else {
                        // disable url
                        entry += (template.getDataValue("tabledatabegin")) + fieldEntry + (template.getDataValue("tabledataend"));
                    }
                } else {
                    entry += (template.getDataValue("tabledatabegin")) + "" + (template.getDataValue("tabledataend"));
                }
            }
            //get the unique id belonging to an entry
            try {
                id = ((A_CmsContentDefinition)entryObject).getUniqueId(cms);
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Backoffice: getUniqueId caused an Exception", e);
                }
            }

            //insert unique id in contextmenue
            if (id != null) {
                template.setData("uniqueid", id);
            }

            //set the lockstates for the actual entry
            setLockstates(cms, template, cdClass, entryObject, parameters);

            //insert single table row in template
            template.setData("entry", entry);

            // processed row from template
            singleRow = template.getProcessedDataValue("singlerow", this, parameters);
            allEntrys += (template.getDataValue("tablerowbegin")) + singleRow + (template.getDataValue("tablerowend"));
        }

        //insert tablecontent in template
        template.setData("tablecontent", "" + allEntrys);

        //save select box value into session
        session.putValue("selectbox", filterMethodName);

        //finally start the processing
        processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
        return processResult;
    }

    /**
     * Gets the content of a given template file.
     * This method displays any content provided by a content definition
     * class on the template. The used backoffice class does not need to use a
     * special getContent method. It just has to extend the methods of this class!
     * Using reflection, this method creates the table headline and table content
     * with the layout provided by the template automatically!
     * @param cms CmsObjectfor accessing system resources
     * @param template the template
     * @param elementName <em>not used here</em>.
     * @param parameters <em>not used here</em>.
     * @param templateSelector template section that should be processed.
     * @return Processed content of the given template file.
     * @throws CmsException if something goes wrong
     */
    private byte[] getContentExtendedList(CmsObject cms, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {

        //return var
        byte[] processResult = null;
        // session will be created or fetched
        I_CmsSession session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
        //get the class of the content definition
        Class cdClass = getContentDefinitionClass();

        //read value of the selected filter
        String filterMethodName = (String)session.getValue("filter");
        if (filterMethodName == null) {
            filterMethodName = "0";
        }
        String uri = cms.getRequestContext().getUri();
        String sessionFilterParam = uri + filterMethodName + "filterparameter";
        //read value of the inputfield filterparameter
        String filterParam = (String)session.getValue(sessionFilterParam);
        if (filterParam == "") {
            filterParam = null;
        }
        //change template to list section for data list output
        templateSelector = "list";

        //init vars
        StringBuffer tableHead = new StringBuffer();
        String singleRow = new String();
        StringBuffer allEntrys = new StringBuffer();
        StringBuffer entry = new StringBuffer();
        String url = "";
        int columns = 0;
        String style = ">";
        String url_style = "";
        String tabledatabegin = template.getDataValue("tabledatabegin");
        String tabledataend = template.getDataValue("tabledataend");
        String tablerowbegin = template.getDataValue("tablerowbegin");
        String tablerowend = template.getDataValue("tablerowend");

        // get number of columns
        Vector columnsVector = new Vector();
        String fieldNamesMethod = "getFieldNames";
        Class paramClasses[] = {CmsObject.class};
        Object params[] = {cms};
        columnsVector = (Vector)getContentMethodObject(cms, cdClass, fieldNamesMethod, paramClasses, params);
        columns = columnsVector.size();
        //create appropriate class name with underscores for labels
        String moduleName = "";
        moduleName = getClass().toString(); //get name
        moduleName = moduleName.substring(5); //remove 'class' substring at the beginning
        moduleName = moduleName.trim();
        moduleName = moduleName.replace('.', '_'); //replace dots with underscores

        //create new language file object
        CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);
        //create tableheadline
        for (int i = 0; i < columns; i++) {
            tableHead.append(tabledatabegin);
            tableHead.append(style);
            tableHead.append(lang.getLanguageValue(moduleName + ".label." + columnsVector.elementAt(i).toString().toLowerCase().trim()));
            tableHead.append(tabledataend);
        }
        //set template data for table headline content
        template.setData("tableheadline", tableHead.toString());
        // get vector of filterMethods and select the appropriate filter method,
        // if no filter is appropriate, select a default filter get number of rows for output
        Vector tableContent = new Vector();
        try {
            Vector filterMethods = (Vector)cdClass.getMethod("getFilterMethods", new Class[] {CmsObject.class}).invoke(null, new Object[] {cms});
            CmsFilterMethod filterMethod = null;
            CmsFilterMethod filterName = (CmsFilterMethod)filterMethods.elementAt(Integer.parseInt(filterMethodName));
            filterMethodName = filterName.getFilterName();
            //loop trough the filter methods and set the chosen one
            for (int i = 0; i < filterMethods.size(); i++) {
                CmsFilterMethod currentFilter = (CmsFilterMethod)filterMethods.elementAt(i);
                if (currentFilter.getFilterName().equals(filterMethodName)) {
                    filterMethod = currentFilter;
                    break;
                }
            }
            // the chosen filter does not exist, use the first one!
            if (filterMethod == null) {
                filterMethod = (CmsFilterMethod)filterMethods.firstElement();
            }

            // now apply the filter with the cms object, the filter method and additional user parameters
            tableContent = (Vector)cdClass.getMethod("applyFilter", new Class[] {CmsObject.class, CmsFilterMethod.class, String.class}).invoke(null, new Object[] {cms, filterMethod, filterParam});
        } catch (InvocationTargetException ite) {
            //error occured while applying the filter
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Apply filter caused an InvocationTargetException", ite);
            }
            templateSelector = "error";
            template.setData("filtername", filterMethodName);
            while (ite.getTargetException() instanceof InvocationTargetException) {
                ite = ((InvocationTargetException)ite.getTargetException());
            }
            template.setData("filtererror", ite.getTargetException().getMessage());
            session.removeValue(sessionFilterParam);
            //session.removeValue("filter");
        } catch (NoSuchMethodException nsm) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Apply filter method was not found", nsm);
            }
            templateSelector = "error";
            template.setData("filtername", filterMethodName);
            template.setData("filtererror", nsm.getMessage());
            session.removeValue(sessionFilterParam);
            //session.removeValue("filterparameter");
        } catch (Exception e) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Apply filter caused an Exception", e);
            }
            templateSelector = "error";
            template.setData("filtername", filterMethodName);
            template.setData("filtererror", e.getMessage());
            session.removeValue(sessionFilterParam);
            //session.removeValue("filterparameter");
        }

        //get the number of rows
        int rows = tableContent.size();
        // get the field methods from the content definition
        Vector fieldMethods = new Vector();
        try {
            fieldMethods = (Vector)cdClass.getMethod("getFieldMethods", new Class[] {CmsObject.class}).invoke(null, new Object[] {cms});
        } catch (Exception exc) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("getFieldMethods caused an exception", exc);
            }
            templateSelector = "error";
            template.setData("filtername", filterMethodName);
            template.setData("filtererror", exc.getMessage());
        }

        // create output from the table data
        String fieldEntry = "";
        String id = "";

        for (int i = 0; i < rows; i++) {
            //init
            entry = new StringBuffer();
            singleRow = new String();
            Object entryObject = new Object();
            entryObject = tableContent.elementAt(i); //cd object in row #i

            // set the fontformat of the current row
            // each entry is formated depending on the state of the cd object
            style = this.getStyle(cms, template, entryObject) + ">";
            //style entry for backoffice with getUrl method
            url_style = this.getStyle(cms, template, entryObject);
            //style = ">";
            //set data of single row
            for (int j = 0; j < columns; j++) {
                fieldEntry = "+++ NO VALUE FOUND +++";
                // call the field methods
                Method getMethod = null;
                try {
                    getMethod = (Method)fieldMethods.elementAt(j);
                } catch (Exception e) {
                    if (OpenCms.getLog(this).isWarnEnabled()) {
                        OpenCms.getLog(this).warn("Could not get field method for " + (String)columnsVector.elementAt(j) + " - check for correct spelling", e);
                    }
                }
                try {
                    //apply methods on content definition object
                    Object fieldEntryObject = null;
                    fieldEntryObject = getMethod.invoke(entryObject, new Object[0]);
                    if (fieldEntryObject != null) {
                        fieldEntry = fieldEntryObject.toString();
                    } else {
                        fieldEntry = null;
                    }
                } catch (InvocationTargetException ite) {
                    if (OpenCms.getLog(this).isWarnEnabled()) {
                        OpenCms.getLog(this).warn("Backoffice content definition object caused an InvocationTargetException", ite);
                    }
                } catch (Exception e) {
                    if (OpenCms.getLog(this).isWarnEnabled()) {
                        OpenCms.getLog(this).warn("Backoffice content definition object: Other exception", e);
                    }
                }
                try {
                    id = ((A_CmsContentDefinition)entryObject).getUniqueId(cms);
                } catch (Exception e) {
                    if (OpenCms.getLog(this).isWarnEnabled()) {
                        OpenCms.getLog(this).warn("Backoffice getUniqueId caused an Exception", e);
                    }
                }

                //insert unique id in contextmenue
                if (id != null) {
                    template.setData("uniqueid", id);
                }
                //insert table entry
                if (fieldEntry != null) {
                    try {
                        Vector v = new Vector();
                        v.addElement(new Integer(id));
                        v.addElement(template);
                        url = getUrl(cms, null, null, v);
                    } catch (Exception e) {
                        url = "";
                    }
                    if (!url.equals("")) {
                        // enable url
                        template.setData("url_style", url_style);
                        entry.append(tabledatabegin);
                        entry.append(style);
                        entry.append(template.getProcessedDataValue("url", this, parameters));
                        entry.append(fieldEntry);
                        entry.append(tabledataend);
                    } else {
                        // disable url
                        entry.append(tabledatabegin);
                        entry.append(style);
                        entry.append(fieldEntry);
                        entry.append(tabledataend);
                    }
                } else {
                    entry.append(tabledatabegin);
                    entry.append("");
                    entry.append(tabledataend);
                }
            }
            //get the unique id belonging to an entry
            try {
                id = ((A_CmsContentDefinition)entryObject).getUniqueId(cms);
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Backoffice: getUniqueId caused an Exception", e);
                }
            }

            //insert unique id in contextmenue
            if (id != null) {
                template.setData("uniqueid", id);
            }

            //set the lockstates for the current entry
            setExtendedLockstates(cms, template, cdClass, entryObject, parameters);
            //set the projectflag for the current entry
            setProjectFlag(cms, template, cdClass, entryObject, parameters);
            // set the context menu of the current entry
            setContextMenu(cms, template, entryObject);
            //insert single table row in template
            template.setData("entry", entry.toString());
            // processed row from template
            singleRow = template.getProcessedDataValue("singlerow", this, parameters);
            allEntrys.append(tablerowbegin);
            allEntrys.append(singleRow);
            allEntrys.append(tablerowend);

        }

        //insert tablecontent in template
        template.setData("tablecontent", allEntrys.toString());

        //save select box value into session
        session.putValue("selectbox", filterMethodName);

        //finally start the processing
        processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
        return processResult;
    }

    /**
    * Gets the content of a given template file.
    * <P>
    * While processing the template file the table entry
    * <code>entryTitle<code> will be displayed in the delete dialog
    *
    * @param cms A_CmsObject Object for accessing system resources
    * @param template the template
    * @param elementName not used here
    * @param parameters get the parameters action for the button activity
    * and id for the used content definition instance object
    * @param templateSelector template section that should be processed.
    * @return Processed content of the given template file.
    * @throws CmsException if something goes wrong
    */

    private byte[] getContentLock(CmsObject cms, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {

        byte[] processResult = null;

        // now check if the "do you really want to lock" dialog should be shown.
        CmsUserSettings settings = new CmsUserSettings(cms.getRequestContext().currentUser());

        if (!settings.getDialogShowLock()) {
            parameters.put("action", "go");
        }

        // session will be created or fetched
        CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
        //get the class of the content definition
        Class cdClass = getContentDefinitionClass();

        CmsUUID actUserId = cms.getRequestContext().currentUser().getId();
        //get (stored) id parameter
        String id = (String)parameters.get("id");
        if (id == null)
            id = "";
        /*if (id != "") {
            session.putValue("idsave", id);
        } else {
            String idsave = (String) session.getValue("idsave");
            if (idsave == null)
                idsave = "";
            id = idsave;
            session.removeValue("idsave");
        } */

        parameters.put("idlock", id);

        // get value of hidden input field action
        String action = (String)parameters.get("action");
        //no button pressed, go to the default section!
        if (action == null || action.equals("")) {
            //lock dialog, displays the title of the entry to be changed in lockstate
            templateSelector = "lock";
            CmsUUID contentId = new CmsUUID(id);

            CmsUUID lockedByUserId = CmsUUID.getNullUUID();

            //      try {
            //        contentId = Integer.valueOf(id);
            //      } catch (Exception e) {
            //        lockedByUserId = CmsUUID.getNullUUID();
            //        //access content definition object specified by id through reflection
            //        Object o = null;
            //        o = getContentDefinition(cms, cdClass, id);
            //        try {
            //          lockedByUserId = ((A_CmsContentDefinition) o).getLockstate();
            //            /*Method getLockstateMethod = (Method) cdClass.getMethod("getLockstate", new Class[] {});
            //            ls = (int) getLockstateMethod.invoke(o, new Object[0]); */
            //        } catch (Exception exc) {
            //            exc.printStackTrace();
            //        }
            //      }

            //access content definition object specified by id through reflection
            Object o = null;
            if (contentId != null) {
                o = getContentDefinition(cms, cdClass, contentId);
                try {
                    lockedByUserId = ((A_CmsContentDefinition)o).getLockstate();
                } catch (Exception e) {
                    if (OpenCms.getLog(this).isWarnEnabled()) {
                        OpenCms.getLog(this).warn(e.getMessage());
                    }
                }
            } else {
                o = getContentDefinition(cms, cdClass, id);
                try {
                    lockedByUserId = ((A_CmsContentDefinition)o).getLockstate();
                } catch (Exception e) {
                    if (OpenCms.getLog(this).isWarnEnabled()) {
                        OpenCms.getLog(this).warn(e.getMessage());
                    }
                }
            }
            //create appropriate class name with underscores for labels
            String moduleName = "";
            moduleName = getClass().toString(); //get name
            moduleName = moduleName.substring(5); //remove 'class' substring at the beginning
            moduleName = moduleName.trim();
            moduleName = moduleName.replace('.', '_'); //replace dots with underscores
            //create new language file object
            CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);
            //get the dialog from the langauge file and set it in the template
            if (!lockedByUserId.isNullUUID() && !lockedByUserId.equals(actUserId)) {
                // "lock"
                template.setData("locktitle", lang.getLanguageValue("messagebox.title.lockchange"));
                template.setData("lockstate", lang.getLanguageValue("messagebox.message1.lockchange"));
            }
            if (lockedByUserId.isNullUUID()) {
                // "nolock"
                template.setData("locktitle", lang.getLanguageValue("messagebox.title.lock"));
                template.setData("lockstate", lang.getLanguageValue("messagebox.message1.lock"));
            }
            if (lockedByUserId.equals(actUserId)) {
                template.setData("locktitle", lang.getLanguageValue("messagebox.title.unlock"));
                template.setData("lockstate", lang.getLanguageValue("messagebox.message1.unlock"));
            }

            //set the title of the selected entry
            template.setData("newsentry", id);

            //go to default template section
            template.setData("setaction", "default");
            parameters.put("action", "done");

            // confirmation button pressed, process data!
        } else {
            templateSelector = "done";

            //access content definition constructor by reflection
            CmsUUID contentId = new CmsUUID(id);

            CmsUUID lockedByUserId = CmsUUID.getNullUUID();

            //call the appropriate content definition constructor
            Object o = null;
            o = getContentDefinition(cms, cdClass, contentId);
            try {
                lockedByUserId = ((A_CmsContentDefinition)o).getLockstate();
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn(e.getMessage());
                }
            }

            try {
                lockedByUserId = ((A_CmsContentDefinition)o).getLockstate();
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Method getLockstate caused an exception", e);
                }
            }

            //show the possible cases of a lockstate in the template
            //and change lockstate in content definition (and in DB or VFS)
            if (lockedByUserId.equals(actUserId)) {
                if (isExtendedList()) {
                    // if its an extended list check if the current project is the same as the
                    // project, in which the resource is locked
                    int curProjectId = cms.getRequestContext().currentProject().getId();
                    int lockedInProject = -1;
                    try {
                        lockedInProject = ((I_CmsExtendedContentDefinition)o).getLockedInProject();
                    } catch (Exception e) {
                        if (OpenCms.getLog(this).isWarnEnabled()) {
                            OpenCms.getLog(this).warn("Method getLockstate caused an exception", e);
                        }
                    }
                    if (curProjectId == lockedInProject) {
                        //unlock project
                        try {
                            ((A_CmsContentDefinition)o).setLockstate(CmsUUID.getNullUUID());
                        } catch (Exception e) {
                            if (OpenCms.getLog(this).isWarnEnabled()) {
                                OpenCms.getLog(this).warn("Method setLockstate caused an exception", e);
                            }
                        }
                    } else {
                        //steal lock
                        try {
                            ((A_CmsContentDefinition)o).setLockstate(actUserId);
                        } catch (Exception e) {
                            if (OpenCms.getLog(this).isWarnEnabled()) {
                                OpenCms.getLog(this).warn("Method setLockstate caused an exception", e);
                            }
                        }
                    }
                } else {
                    // this is not the extended list
                    //steal lock (userlock -> nolock)
                    try {
                        ((A_CmsContentDefinition)o).setLockstate(CmsUUID.getNullUUID());
                    } catch (Exception e) {
                        if (OpenCms.getLog(this).isWarnEnabled()) {
                            OpenCms.getLog(this).warn("Method setLockstate caused an exception", e);
                        }
                    }
                }
                //write to DB
                try {
                    ((A_CmsContentDefinition)o).write(cms); // reflection is not neccessary!
                } catch (Exception e) {
                    if (OpenCms.getLog(this).isWarnEnabled()) {
                        OpenCms.getLog(this).warn("Method write caused an exception", e);
                    }
                }
                templateSelector = "done";
            } else {
                if ((!lockedByUserId.isNullUUID()) && (!lockedByUserId.equals(actUserId))) {
                    //unlock (lock -> userlock)
                    try {
                        ((A_CmsContentDefinition)o).setLockstate(actUserId);
                    } catch (Exception e) {
                        if (OpenCms.getLog(this).isWarnEnabled()) {
                            OpenCms.getLog(this).warn("Could not set lockstate", e);
                        }
                    }
                    //write to DB
                    try {
                        ((A_CmsContentDefinition)o).write(cms);
                    } catch (Exception e) {
                        if (OpenCms.getLog(this).isWarnEnabled()) {
                            OpenCms.getLog(this).warn("Could not set lockstate", e);
                        }
                    }
                    templateSelector = "done";
                } else {
                    //lock (nolock -> userlock)
                    try {
                        ((A_CmsContentDefinition)o).setLockstate(actUserId);
                    } catch (Exception e) {
                        if (OpenCms.getLog(this).isWarnEnabled()) {
                            OpenCms.getLog(this).warn("Could not set lockstate", e);
                        }
                    }
                    //write to DB/VFS
                    try {
                        ((A_CmsContentDefinition)o).write(cms);
                    } catch (Exception e) {
                        if (OpenCms.getLog(this).isWarnEnabled()) {
                            OpenCms.getLog(this).warn("Could not write to content definition", e);
                        }
                    }
                }
            }
        }
        //finally start the processing
        processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
        return processResult;
    }
    /**
     * gets the content definition class method object.<p>
     * 
     * @param cms the cms object
     * @param cdClass the content definition class
     * @param method a method name of the class
     * @param paramClasses the parameter classes
     * @param params the parameter values
     * @return object content definition class method object
     */

    private Object getContentMethodObject(CmsObject cms, Class cdClass, String method, Class paramClasses[], Object params[]) {

        //return value
        Object retObject = null;
        if (method != "") {
            try {
                retObject = cdClass.getMethod(method, paramClasses).invoke(null, params);
            } catch (InvocationTargetException ite) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn(method + " caused an InvocationTargetException", ite);
                }
                ite.getTargetException().printStackTrace();
            } catch (NoSuchMethodException nsm) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn(method + ": Requested method was not found", nsm);
                }
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn(method + ": Other Exception", e);
                }
            }
        }
        return retObject;
    }

    /**
    * The old version of the getContentNew methos. Only available for compatilibility resons.
    * Per default, it uses the edit dialog. If you want to use a seperate input form, you have to create
    * a new one and write your own getContentNew method in your backoffice class.<p>
    * 
    * @param cms ths cms object
    * @param templateFile the template
    * @param elementName the element name
    * @param parameters the parameters
    * @param templateSelector the template selector
    * @return the content as byte array
    * @throws CmsException if something goes wrong
    */
    public byte[] getContentNew(CmsObject cms, CmsXmlWpTemplateFile templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        parameters.put("id", "new");
        return getContentEdit(cms, templateFile, elementName, parameters, templateSelector);
    }

    /**
    * The new version of the getContentNew methos. Only this one should be used from now on.
    * Per default, it uses the edit dialog. If you want to use a seperate input form, you have to create
    * a new one and write your own getContentNew method in your backoffice class.<p>
    * 
    * @param cms ths cms object
    * @param template the template
    * @param cd the content definition
    * @param elementName name of an element
    * @param keys the parameter keys
    * @param parameters the parameters
    * @param templateSelector the template selector
    * @return the content
    * @throws CmsException if something goes wrong
    */
    public String getContentNew(CmsObject cms, CmsXmlWpTemplateFile template, A_CmsContentDefinition cd, String elementName, Enumeration keys, Hashtable parameters, String templateSelector) throws CmsException {
        parameters.put("id", "new");
        return getContentEdit(cms, template, cd, elementName, keys, parameters, templateSelector);
    }

    /**
    * Gets the content of a edited entry form.
    * Has to be overwritten in your backoffice class if the old way of writeing BackOffices is used.
    * Only available for compatibility reasons.<p>
    * 
    * @param cms ths cms object
    * @param templateFile the template
    * @param elementName name of an element
    * @param parameters the parameters
    * @param templateSelector the template selector
    * @return the content
    * @throws CmsException if something goes wrong
    */
    public byte[] getContentEdit(CmsObject cms, CmsXmlWpTemplateFile templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        return null;
    }

    /**
    * Gets the content of a edited entry form.
    * Has to be overwritten in your backoffice class if the new way of writeing BackOffices is used.<p>
    * 
    * @param cms ths cms object
    * @param templateFile the template
    * @param cd the content definition
    * @param elementName name of an element
    * @param keys the parameter keys
    * @param parameters the parameters
    * @param templateSelector the template selector
    * @return the content
    * @throws CmsException if something goes wrong
    */
    public String getContentEdit(CmsObject cms, CmsXmlWpTemplateFile templateFile, A_CmsContentDefinition cd, String elementName, Enumeration keys, Hashtable parameters, String templateSelector) throws CmsException {
        return null;
    }

    /**
    * Set the correct lockstates in the list output.
    * Lockstates can be "unlocked", "locked", "locked by user" or "no access"
    * @param cms The current CmsObject.
    * @param template The actual template file.
    * @param cdClass The content defintion.
    * @param entryObject a content definition instance
    * @param parameters All template ands URL parameters.
    */
    private void setLockstates(CmsObject cms, CmsXmlWpTemplateFile template, Class cdClass, Object entryObject, Hashtable parameters) {

        //init lock state vars
        String la = "false";
        Object laObject = new Object();
        CmsUUID lockedByUserId = CmsUUID.getNullUUID();
        String lockString = null;
        CmsUUID actUserId = cms.getRequestContext().currentUser().getId();
        String isLockedBy = null;
        boolean hasWriteAccess = false;

        //is the content definition object (i.e. the table entry) lockable?
        try {
            //get the method
            Method laMethod = cdClass.getMethod("isLockable", new Class[] {});
            //get the returned object
            laObject = laMethod.invoke(null, null);
        } catch (InvocationTargetException ite) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Method isLockable caused an Invocation target exception", ite);
            }
        } catch (NoSuchMethodException nsm) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Requested method isLockable was not found", nsm);
            }
        } catch (Exception e) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Method isLockable caused an exception", e);
            }
        }

        //cast the returned object to a string
        la = laObject.toString();
        if (la.equals("false")) {
            try {
                //the entry is not lockable: use standard contextmenue
                template.setData("backofficecontextmenue", "backofficeedit");
                template.setData("lockedby", template.getDataValue("nolock"));
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Backoffice setLockstates:'not lockable' section caused an exception", e);
                }
            }
        } else {
            //...get the lockstate of an entry
            try {
                //get the method lockstate
                lockedByUserId = ((A_CmsContentDefinition)entryObject).getLockstate();
                hasWriteAccess = ((A_CmsContentDefinition)entryObject).hasWriteAccess(cms);
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Method getLockstate caused an exception", e);
                }
            }
            try {
                //show the possible cases of a lockstate in the template
                if (lockedByUserId.equals(actUserId)) {
                    // lockuser
                    isLockedBy = cms.getRequestContext().currentUser().getName();
                    template.setData("isLockedBy", isLockedBy); // set current users name in the template
                    lockString = template.getProcessedDataValue("lockuser", this, parameters);
                    template.setData("lockedby", lockString);
                    template.setData("backofficecontextmenue", "backofficelockuser");
                } else {
                    if (!lockedByUserId.isNullUUID()) {
                        // lock
                        // set the name of the user who locked the file in the template ...
                        if (!hasWriteAccess) {
                            lockString = template.getProcessedDataValue("noaccess", this, parameters);
                            template.setData("lockedby", lockString);
                            template.setData("backofficecontextmenue", "backofficenoaccess");
                        } else {
                            isLockedBy = readSaveUserName(cms, lockedByUserId);
                            template.setData("isLockedBy", isLockedBy);
                            lockString = template.getProcessedDataValue("lock", this, parameters);
                            template.setData("lockedby", lockString);
                            template.setData("backofficecontextmenue", "backofficelock");
                        }
                    } else {
                        // nolock
                        lockString = template.getProcessedDataValue("nolock", this, parameters);
                        template.setData("lockedby", lockString);
                        template.setData("backofficecontextmenue", "backofficenolock");
                    }
                }
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Backoffice setLockstates caused an exception", e);
                }
            }
        }
    }

    /**
    * Set the correct lockstates in the list output for the extended list.
    * Lockstates can be "unlocked", "locked", "locked by user" or "no access"
    * @param cms The current CmsObject.
    * @param template The actual template file.
    * @param cdClass The content defintion.
    * @param entryObject a content definition instance
    * @param parameters All template ands URL parameters.
    */
    private void setExtendedLockstates(CmsObject cms, CmsXmlWpTemplateFile template, Class cdClass, Object entryObject, Hashtable parameters) {

        //init lock state vars
        String la = "false";
        Object laObject = new Object();
        CmsUUID lockedByUserId = CmsUUID.getNullUUID();
        String lockString = null;
        CmsUUID actUserId = cms.getRequestContext().currentUser().getId();
        String isLockedBy = null;
        int lockedInProject = -1;
        int curProjectId = cms.getRequestContext().currentProject().getId();
        boolean hasWriteAccess = false;

        //is the content definition object (i.e. the table entry) lockable?
        try {
            //get the method
            Method laMethod = cdClass.getMethod("isLockable", new Class[] {});
            //get the returned object
            laObject = laMethod.invoke(null, null);
        } catch (InvocationTargetException ite) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Method isLockable caused an Invocation target exception", ite);
            }
        } catch (NoSuchMethodException nsm) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Requested method isLockable was not found", nsm);
            }
        } catch (Exception e) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Method isLockable caused an exception", e);
            }
        }

        //cast the returned object to a string
        la = laObject.toString();
        if (la.equals("false")) {
            try {
                //the entry is not lockable: use standard contextmenue
                template.setData("backofficecontextmenue", "backofficeedit");
                template.setData("lockedby", template.getDataValue("nolock"));
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Backoffice setLockstates:'not lockable' section caused an exception", e);
                }
            }
        } else {
            //...get the lockstate of an entry
            try {
                //get the method lockstate
                lockedByUserId = ((A_CmsContentDefinition)entryObject).getLockstate();
                hasWriteAccess = ((A_CmsContentDefinition)entryObject).hasWriteAccess(cms);
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Method getLockstate caused an exception", e);
                }
            }
            try {
                //show the possible cases of a lockstate in the template
                if (lockedByUserId.equals(actUserId)) {
                    // lockuser
                    isLockedBy = cms.getRequestContext().currentUser().getName();
                    template.setData("isLockedBy", isLockedBy); // set current users name in the template
                    // check if the resource is locked in another project
                    try {
                        //get the method lockstate
                        lockedInProject = ((I_CmsExtendedContentDefinition)entryObject).getLockedInProject();
                    } catch (Exception e) {
                        if (OpenCms.getLog(this).isWarnEnabled()) {
                            OpenCms.getLog(this).warn("Method getLockedInProject caused an exception", e);
                        }
                    }
                    if (lockedInProject == curProjectId) {
                        lockString = template.getProcessedDataValue("lockuser", this, parameters);
                    } else {
                        lockString = template.getProcessedDataValue("lock", this, parameters);
                    }
                    template.setData("lockedby", lockString);
                } else {
                    if (!lockedByUserId.isNullUUID()) {
                        // lock
                        // set the name of the user who locked the file in the template ...
                        if (!hasWriteAccess) {
                            lockString = template.getProcessedDataValue("noaccess", this, parameters);
                            template.setData("lockedby", lockString);
                        } else {
                            isLockedBy = readSaveUserName(cms, lockedByUserId);
                            template.setData("isLockedBy", isLockedBy);
                            lockString = template.getProcessedDataValue("lock", this, parameters);
                            template.setData("lockedby", lockString);
                        }
                    } else {
                        // nolock
                        lockString = template.getProcessedDataValue("nolock", this, parameters);
                        template.setData("lockedby", lockString);
                    }
                }
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Backoffice setLockstates caused an exception", e);
                }
            }
        }
    }
    /**
     * Set the correct project flag in the list output.
     * Lockstates can be "unlocked", "locked", "locked by user" or "no access"
     * @param cms The current CmsObject.
     * @param template The actual template file.
     * @param cdClass The content defintion.
     * @param entryObject a content definition instance
     * @param parameters All template ands URL parameters.
     */
    private void setProjectFlag(CmsObject cms, CmsXmlWpTemplateFile template, Class cdClass, Object entryObject, Hashtable parameters) {

        //init project flag vars
        int state = 0;
        int projectId = 1;
        String projectFlag = null;
        int actProjectId = cms.getRequestContext().currentProject().getId();
        String isInProject = null;
        // set the default projectflag
        try {
            template.setData("projectflag", template.getDataValue("noproject"));
        } catch (Exception e) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Backoffice setProjectFlag:'no project' section caused an exception", e);
            }
        }

        // get the state of an entry: if its unchanged do not show the flag
        try {
            state = ((I_CmsExtendedContentDefinition)entryObject).getState();
        } catch (Exception e) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Method getState caused an exception", e);
            }
        }

        if (state == 0) {
            try {
                //the entry is not changed, so do not set the project flag
                template.setData("projectflag", template.getDataValue("noproject"));
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Backoffice setProjectFlag:'no project' section caused an exception", e);
                }
            }
        } else {
            // the entry is new, changed or deleted
            //...get the project of the entry
            try {
                projectId = ((I_CmsExtendedContentDefinition)entryObject).getLockedInProject();
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Method getLockedInProject caused an exception", e);
                }
            }
            try {
                //show the possible cases of a lockstate in the template
                try {
                    isInProject = cms.readProject(projectId).getName();
                } catch (CmsException e) {
                    isInProject = "";
                }
                // set project name in the template
                template.setData("isInProject", isInProject);
                if (projectId == actProjectId) {
                    // changed in this project
                    projectFlag = template.getProcessedDataValue("thisproject", this, parameters);
                    template.setData("projectflag", projectFlag);
                } else {
                    // changed in another project
                    projectFlag = template.getProcessedDataValue("otherproject", this, parameters);
                    template.setData("projectflag", projectFlag);
                }
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Backoffice setLockstates caused an exception", e);
                }
            }
        }
    }

    /**
     * Return the format for the list output.
     * States can be "not in project", "unchanged", "changed", "new" or "deleted"
     *
     * @param cms The current CmsObject
     * @param template the template
     * @param entryObject a content definition instance
     * @return String The format tag for the entry
     */
    private String getStyle(CmsObject cms, CmsXmlWpTemplateFile template, Object entryObject) {

        //init project flag vars
        int state = 0;
        int projectId = 1;
        int actProjectId = cms.getRequestContext().currentProject().getId();
        String style = new String();

        // get the projectid of the entry
        try {
            projectId = ((I_CmsExtendedContentDefinition)entryObject).getProjectId();
        } catch (Exception e) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Method getProjectId caused an exception", e);
            }
        }

        if (actProjectId == I_CmsConstants.C_PROJECT_ONLINE_ID) {
            style = C_STYLE_UNCHANGED;
        } else if (projectId != actProjectId) {
            // not is in this project
            style = C_STYLE_NOTINPROJECT;
        } else {
            // get the lockstate of an entry: if its unlocked and changed enable direct publish
            try {
                ((A_CmsContentDefinition)entryObject).getLockstate();
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Method getLockstate caused an exception", e);
                }
            }
            // get the state of an entry: if its unchanged do not change the font
            try {
                state = ((I_CmsExtendedContentDefinition)entryObject).getState();
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Method getState caused an exception", e);
                }
            }

            switch (state) {
                case I_CmsConstants.C_STATE_NEW :
                    style = C_STYLE_NEW;
                    break;
                case I_CmsConstants.C_STATE_CHANGED :
                    style = C_STYLE_CHANGED;
                    break;
                case I_CmsConstants.C_STATE_DELETED :
                    style = C_STYLE_DELETED;
                    break;
                default :
                    style = C_STYLE_UNCHANGED;
                    break;
            }
        }
        // if there was an exception return an empty string
        return style;
    }

    /**
     * Set the context menu for the current list entry.<p>
     *
     * @param cms The current CmsObject
     * @param template The current template
     * @param entryObject a content definition instance
     */
    private void setContextMenu(CmsObject cms, CmsXmlWpTemplateFile template, Object entryObject) {

        //init project flag vars
        int state = 0;
        CmsUUID lockedByUserId = CmsUUID.getNullUUID();
        int projectId = 1;
        int lockedInProject = -1;
        int actProjectId = cms.getRequestContext().currentProject().getId();

        // get the projectid of the entry
        try {
            projectId = ((I_CmsExtendedContentDefinition)entryObject).getProjectId();
        } catch (Exception e) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Method getProjectId caused an exception", e);
            }
        }

        if (actProjectId == I_CmsConstants.C_PROJECT_ONLINE_ID) {
            template.setData("backofficecontextmenue", "backofficeonline");
        } else if (projectId != actProjectId) {
            // not is in this project
            template.setData("backofficecontextmenue", "backofficenoaccess");
        } else {
            // get the lockstate of an entry: if its unlocked and changed enable direct publish
            try {
                lockedByUserId = ((A_CmsContentDefinition)entryObject).getLockstate();
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Method getLockstate caused an exception", e);
                }
            }
            // get the state of an entry: if its unchanged do not change the font
            try {
                state = ((I_CmsExtendedContentDefinition)entryObject).getState();
            } catch (Exception e) {
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Method getState caused an exception", e);
                }
            }
            if (lockedByUserId.isNullUUID()) {
                if (state == I_CmsConstants.C_STATE_UNCHANGED) {
                    template.setData("backofficecontextmenue", "backofficenolock");
                } else if (state == I_CmsConstants.C_STATE_DELETED) {
                    template.setData("backofficecontextmenue", "backofficedeleted");
                } else {
                    template.setData("backofficecontextmenue", "backofficenolockchanged");
                }
            } else if (lockedByUserId.equals(cms.getRequestContext().currentUser().getId())) {
                // check if the resource is locked in another project
                try {
                    lockedInProject = ((I_CmsExtendedContentDefinition)entryObject).getLockedInProject();
                } catch (Exception e) {
                    if (OpenCms.getLog(this).isWarnEnabled()) {
                        OpenCms.getLog(this).warn("Method getLockedInProject caused an exception", e);
                    }
                }
                if (lockedInProject == actProjectId) {
                    template.setData("backofficecontextmenue", "backofficelockuser");
                } else {
                    template.setData("backofficecontextmenue", "backofficelock");
                }
            } else {
                template.setData("backofficecontextmenue", "backofficelock");
            }
        }
    }

    /**
     * Checks if the extended list should be used for displaying the cd
     *
     * @return boolean Is true the extended list should be used
     */
    public boolean isExtendedList() {
        return false;
    }

    /**
     * This method creates the selectbox in the head-frame
     * 
     * @param cms the cms object
     * @param lang the language file
     * @param names the names in the select box
     * @param values the values
     * @param parameters additional paramters
     * @return the current entry
     * @throws CmsException if something goes wrong
     */
    public Integer getFilter(CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values, Hashtable parameters) throws CmsException {
        I_CmsSession session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
        int returnValue = 0;
        String uri = cms.getRequestContext().getUri();
        String sessionSelectBoxValue = uri + "selectBoxValue";
        Vector filterMethods = getFilterMethods(cms);
        String tmp = (String)session.getValue(sessionSelectBoxValue);
        if (tmp != null) {
            returnValue = Integer.parseInt(tmp);
        }
        for (int i = 0; i < filterMethods.size(); i++) {
            CmsFilterMethod currentFilter = (CmsFilterMethod)filterMethods.elementAt(i);
            //insert filter in the template selectbox
            names.addElement(currentFilter.getFilterName());
            values.addElement("" + i);
        }
        return new Integer(returnValue);
    }

    /**
     * User method that handles a checkbox in the input form of the backoffice.<p>
     * 
     * @param cms the cms object
     * @param tagcontent the body content of the tag
     * @param doc the xml document
     * @param userObject additional data
     * @return the checkbox definition
     * @throws CmsException if something goes wrong
     */
    public Object handleCheckbox(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws CmsException {
        Hashtable parameters = (Hashtable)userObject;
        String returnValue = "";
        String selected = (String)parameters.get(tagcontent);
        if (selected != null && selected.equals("on")) {
            returnValue = "checked";
        }
        return returnValue;
    }

    /**
    * Get the all available filter methods.
    * @param cms The actual CmsObject
    * @return Vector of Filter Methods
    */
    private Vector getFilterMethods(CmsObject cms) {
        Vector filterMethods = new Vector();
        Class cdClass = getContentDefinitionClass();
        try {
            filterMethods = (Vector)cdClass.getMethod("getFilterMethods", new Class[] {CmsObject.class}).invoke(null, new Object[] {cms});
        } catch (InvocationTargetException ite) {
            //error occured while applying the filter
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("InvocationTargetException", ite);
            }
        } catch (NoSuchMethodException nsm) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Requested method was not found", nsm);
            }
        } catch (Exception e) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("Problem occured with your filter methods", e);
            }
        }
        return filterMethods;
    }

    /**
     * This method creates the selectbox with all avaiable Pages to select from.
     * 
     * @param cms the cms object
     * @param lang the language file
     * @param names the names of the options
     * @param values the values ogf the options
     * @param parameters additional parameters
     * @return the selected entry
     * @throws CmsException if something goes wrong
     */
    public Integer getSelectedPage(CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values, Hashtable parameters) throws CmsException {

        int returnValue = 0;
        I_CmsSession session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
        // get all aviable template selectors
        Vector templateSelectors = (Vector)session.getValue("backofficepageselectorvector");
        // get the actual template selector
        String selectedPage = (String)parameters.get("backofficepage");
        if (selectedPage == null) {
            selectedPage = "";
        }

        // now build the names and values for the selectbox
        if (templateSelectors != null) {
            for (int i = 0; i < templateSelectors.size(); i++) {
                String selector = (String)templateSelectors.elementAt(i);
                names.addElement(selector);
                values.addElement(selector);
                // check for the selected item
                if (selectedPage.equals(selector)) {
                    returnValue = i;
                }
            }
        }
        session.putValue("backofficeselectortransfer", values);
        session.putValue("backofficeselectedtransfer", new Integer(returnValue));
        return new Integer(returnValue);
    }

    /**
    * This method contains the code used by the getContent method when the new form of the backoffice is processed.
    * It automatically gets all the data from the "getXYZ" methods and sets it in the correct datablock of the
    * template.<p>
    * 
    * @param cms The actual CmsObject.
    * @param id The id of the content definition object to be edited.
    * @param cd the content definition.
    * @param session The current user session.
    * @param template The template file.
    * @param elementName The emelentName of the template mechanism.
    * @param parameters All parameters of this request.
    * @param templateSelector The template selector.
    * @return Content of the template, as an array of bytes.
    * @throws CmsException if something goes wrong
    */
    private byte[] getContentNewInternal(CmsObject cms, String id, A_CmsContentDefinition cd, I_CmsSession session, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {

        //return varlue
        byte[] returnProcess = null;
        //error storage
        String error = "";
        // action value of backoffice buttons
        String action = (String)parameters.get("action");

        // get the correct template selector
        templateSelector = getTemplateSelector(cms, template, parameters, templateSelector);

        //process the template for entering the data
        // first try to do it in the old way to be compatible to the existing modules.
        returnProcess = getContentNew(cms, template, elementName, parameters, templateSelector);

        // there was no returnvalue, so the BO uses the new getContentNew method
        if (returnProcess == null) {

            //try to get the CD form the session
            cd = (A_CmsContentDefinition)session.getValue(this.getContentDefinitionClass().getName());
            if (cd == null) {
                //get a new, empty content definition
                cd = (A_CmsContentDefinition)this.getContentDefinition(cms, this.getContentDefinitionClass());
            }
            //get all existing parameters
            Enumeration keys = parameters.keys();

            //call the new getContentEdit method
            error = getContentNew(cms, template, cd, elementName, keys, parameters, templateSelector);

            // get all getXVY methods to automatically get the data from the CD
            Vector methods = this.getGetMethods(cd);
            // get all setXVY methods to automatically set the data into the CD
            Hashtable setMethods = this.getSetMethods(cd);
            //set all data from the parameters into the CD
            this.fillContentDefinition(cms, cd, parameters, setMethods);

            // store the modified CD in the session
            session.putValue(this.getContentDefinitionClass().getName(), cd);

            // check if there was an error found in the input form
            if (!error.equals("")) {
                template.setData("error", template.getProcessedDataValue("errormsg") + error);
            } else {
                template.setData("error", "");
            }

            // now check if one of the exit buttons is used
            templateSelector = getContentButtonsInternal(cms, cd, session, template, parameters, templateSelector, action, error);

            //now set all the data from the CD into the template
            this.setDatablocks(cms, template, cd, methods);

            returnProcess = startProcessing(cms, template, "", parameters, templateSelector);
        }

        //finally retrun processed data
        return returnProcess;
    }

    /**
     * This method contains the code used by the getContent method when the edit form of the backoffice is processed.
     * It automatically gets all the data from the "getXYZ" methods and sets it in the correct datablock of the
     * template.
     * @param cms The actual CmsObject.
     * @param id The id of the content definition object to be edited.
     * @param cd the content definition.
     * @param session The current user session.
     * @param template The template file.
     * @param elementName The emelentName of the template mechanism.
     * @param parameters All parameters of this request.
     * @param templateSelector The template selector.
     * @return Content of the template, as an array of bytes.
     * @throws CmsException if something goes wrong
     */
    private byte[] getContentEditInternal(CmsObject cms, String id, A_CmsContentDefinition cd, I_CmsSession session, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {

        //return varlue
        byte[] returnProcess = null;
        //error storage
        String error = "";
        // action value of backoffice buttons
        String action = (String)parameters.get("action");

        // get the correct template selector
        templateSelector = getTemplateSelector(cms, template, parameters, templateSelector);

        //process the template for editing the data
        // first try to do it in the old way to be compatible to the existing modules.
        returnProcess = getContentEdit(cms, template, elementName, parameters, templateSelector);

        // there was no returnvalue, so the BO uses the new getContentEdit method
        if (returnProcess == null) {
            //try to get the CD form the session
            cd = (A_CmsContentDefinition)session.getValue(this.getContentDefinitionClass().getName());
            if (cd == null) {
                // not successful, so read new content definition with given id
                CmsUUID contentId = new CmsUUID(id);
                cd = (A_CmsContentDefinition)this.getContentDefinition(cms, this.getContentDefinitionClass(), contentId);
            }

            //get all existing parameters
            Enumeration keys = parameters.keys();
            //call the new getContentEdit method
            error = getContentEdit(cms, template, cd, elementName, keys, parameters, templateSelector);

            // get all getXVY methods to automatically get the data from the CD
            Vector getMethods = this.getGetMethods(cd);

            // get all setXVY methods to automatically set the data into the CD
            Hashtable setMethods = this.getSetMethods(cd);

            //set all data from the parameters into the CD
            this.fillContentDefinition(cms, cd, parameters, setMethods);

            // store the modified CD in the session
            session.putValue(this.getContentDefinitionClass().getName(), cd);

            //care about the previewbutten, if getPreviewUrl is empty, the preview button will not be displayed in the template
            String previewButton = null;
            try {
                previewButton = getPreviewUrl(cms, null, null, null);
            } catch (Exception e) { }
            if (!((previewButton == null) || (previewButton.equals("")))) {
                session.putValue("weShallDisplayThePreviewButton", previewButton + "?id=" + cd.getUniqueId(cms));
            }

            // check if there was an error found in the input form
            if (!error.equals("")) {
                template.setData("error", template.getProcessedDataValue("errormsg") + error);
            } else {
                template.setData("error", "");
            }

            // now check if one of the exit buttons is used
            templateSelector = getContentButtonsInternal(cms, cd, session, template, parameters, templateSelector, action, error);

            //now set all the data from the CD into the template
            this.setDatablocks(cms, template, cd, getMethods);

            returnProcess = startProcessing(cms, template, "", parameters, templateSelector);
        }

        //finally return processed data
        return returnProcess;
    }

    /** This method checks the three function buttons on the backoffice edit/new template.
     *  The following actions are excecuted: <ul>
     *  <li>Save: Save the CD to the database if plausi shows no error, return to the edit/new template.</li>
     *  <li>Save & Exit: Save the CD to the database if plausi shows no error, return to the list view. </li>
     *  <li>Exit: Return to the list view without saving. </li>
     *  </ul>
     * 
     * @param cms The actual CmsObject.
     * @param cd The acutal content definition.
     * @param session The current user session.
     * @param template The template file.
     * @param parameters All parameters of this request.
     * @param templateSelector The template selector.
     * @param action the selected action
     * @param error an error message
     * @return The template selector.
     * @throws CmsException if something goes wrong
     */
    private String getContentButtonsInternal(CmsObject cms, A_CmsContentDefinition cd, I_CmsSession session, CmsXmlWpTemplateFile template, Hashtable parameters, String templateSelector, String action, String error) throws CmsException {

        // storage for possible errors during plausibilization
        Vector errorCodes = null;
        // storage for a single error code
        String errorCode = null;
        // storage for the field where an error occured
        String errorField = null;
        // storage for the error type
        String errorType = null;
        // the complete error text displayed on the template
        //String error=null;
        //  check if one of the exit buttons is used

        if (action != null) {
            // there was no button selected, so the selectbox was used. Do a check of the input fileds.
            if ((!action.equals("save")) && (!action.equals("saveexit")) && (!action.equals("exit"))) {
                try {
                    cd.check(false);
                    // put value of last used templateselector in session
                    session.putValue("backofficepagetemplateselector", templateSelector);
                } catch (CmsPlausibilizationException plex) {
                    // there was an error during plausibilization, so create an error text
                    errorCodes = plex.getErrorCodes();
                    //loop through all errors
                    for (int i = 0; i < errorCodes.size(); i++) {
                        errorCode = (String)errorCodes.elementAt(i);
                        // try to get an error message that fits thos this error code exactly
                        if (template.hasData(I_CmsConstants.C_ERRPREFIX + errorCode)) {
                            error += template.getProcessedDataValue(I_CmsConstants.C_ERRPREFIX + errorCode);
                        } else {
                            // now check if there is a general error message for this field
                            errorField = errorCode.substring(0, errorCode.indexOf(I_CmsConstants.C_ERRSPERATOR));
                            if (template.hasData(I_CmsConstants.C_ERRPREFIX + errorField)) {
                                error += template.getProcessedDataValue(I_CmsConstants.C_ERRPREFIX + errorField);
                            } else {
                                // now check if there is at least a general error messace for the error type
                                errorType = errorCode.substring(errorCode.indexOf(I_CmsConstants.C_ERRSPERATOR) + 1, errorCode.length());
                                if (template.hasData(I_CmsConstants.C_ERRPREFIX + errorType)) {
                                    error += template.getProcessedDataValue(I_CmsConstants.C_ERRPREFIX + errorType);
                                } else {
                                    // no error dmessage was found, so generate a default one
                                    error += "[" + errorCode + "]";
                                }
                            }
                        }
                    }
                    //check if there is an introtext for the errors
                    if (template.hasData("errormsg")) {
                        error = template.getProcessedDataValue("errormsg") + error;
                    }
                    template.setData("error", error);
                    if (session.getValue("backofficepagetemplateselector") != null) {

                        templateSelector = (String)session.getValue("backofficepagetemplateselector");
                        parameters.put("backofficepage", templateSelector);

                    } else {
                        templateSelector = null;
                    }

                } // catch
            }
            // the same or save&exit button were pressed, so save the content definition to the
            // database
            if (((action.equals("save")) || (action.equals("saveexit"))) && (error.equals(""))) {
                try {
                    // first check if all plausibilization was ok

                    cd.check(true);

                    // unlock resource if save&exit was selected
                    /* if (action.equals("saveexit")) {
                      cd.setLockstate(-1);
                    } */

                    // write the data to the database
                    cd.write(cms);

                    //care about the previewbutten, if getPreviewUrl is empty, the preview button will not be displayed in the template
                    String previewButton = null;
                    try {
                        previewButton = getPreviewUrl(cms, null, null, null);
                    } catch (Exception e) { }
                    if (!((previewButton == null) || (previewButton.equals("")))) {
                        session.putValue("weShallDisplayThePreviewButton", previewButton + "?id=" + cd.getUniqueId(cms));
                    }

                } catch (CmsPlausibilizationException plex) {

                    // there was an error during plausibilization, so create an error text
                    errorCodes = plex.getErrorCodes();

                    //loop through all errors
                    for (int i = 0; i < errorCodes.size(); i++) {
                        errorCode = (String)errorCodes.elementAt(i);

                        // try to get an error message that fits thos this error code exactly
                        if (template.hasData(I_CmsConstants.C_ERRPREFIX + errorCode)) {
                            error += template.getProcessedDataValue(I_CmsConstants.C_ERRPREFIX + errorCode);
                        } else {
                            // now check if there is a general error message for this field
                            errorField = errorCode.substring(0, errorCode.indexOf(I_CmsConstants.C_ERRSPERATOR));
                            if (template.hasData(I_CmsConstants.C_ERRPREFIX + errorField)) {
                                error += template.getProcessedDataValue(I_CmsConstants.C_ERRPREFIX + errorField);
                            } else {
                                // now check if there is at least a general error messace for the error type
                                errorType = errorCode.substring(errorCode.indexOf(I_CmsConstants.C_ERRSPERATOR) + 1, errorCode.length());
                                if (template.hasData(I_CmsConstants.C_ERRPREFIX + errorType)) {
                                    error += template.getProcessedDataValue(I_CmsConstants.C_ERRPREFIX + errorType);
                                } else {
                                    // no error dmessage was found, so generate a default one
                                    error += "[" + errorCode + "]";
                                }
                            }
                        }

                    }

                    //check if there is an introtext for the errors
                    if (template.hasData("errormsg")) {
                        error = template.getProcessedDataValue("errormsg") + error;
                    }
                    template.setData("error", error);

                } catch (Exception ex) {
                    // there was an error saving the content definition so remove all nescessary values from the
                    // session
                    session.removeValue(this.getContentDefinitionClass().getName());
                    session.removeValue("backofficepageselectorvector");
                    session.removeValue("backofficepagetemplateselector");
                    session.removeValue("media");
                    session.removeValue("selectedmediaCD");
                    session.removeValue("media_position");
                    session.removeValue("weShallDisplayThePreviewButton");
                    if (OpenCms.getLog(this).isErrorEnabled()) {
                        OpenCms.getLog(this).error("Error while saving data to Content Definition", ex);
                    }
                    throw new CmsException(ex.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, ex);
                }
            } //if (save || saveexit)

            // only exit the form if there was no error
            if (errorCodes == null) {
                // the exit or save&exit buttons were pressed, so return to the list
                if ((action.equals("exit")) || ((action.equals("saveexit")) && (error.equals("")))) {
                    try {
                        // cleanup session
                        session.removeValue(this.getContentDefinitionClass().getName());
                        session.removeValue("backofficepageselectorvector");
                        session.removeValue("backofficepagetemplateselector");
                        session.removeValue("media");
                        session.removeValue("selectedmediaCD");
                        session.removeValue("media_position");
                        session.removeValue("weShallDisplayThePreviewButton");

                        //do the redirect
                        // to do: replace the getUri method with getPathInfo if aviable
                        //String uri=  cms.getRequestContext().getUri();
                        //uri = "/"+uri.substring(1,uri.lastIndexOf("/"));
                        //CmsXmlTemplateLoader.getResponse(cms.getRequestContext()).sendCmsRedirect(uri);
                        //return null;
                        // EF 08.11.01: return the templateselector "done"
                        // there the backoffice url of the module will be called
                        return "done";
                    } catch (Exception e) {
                        if (OpenCms.getLog(this).isErrorEnabled()) {
                            OpenCms.getLog(this).error("Error while doing redirect ", e);
                        }
                        throw new CmsException(e.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, e);
                    }
                } // if (exit || saveexit)
            } // noerror
        } // if (action!=null)
        return templateSelector;
    }

    /**
     * This methods collects all "getXYZ" methods of the actual content definition. This is used
     * to automatically preset the datablocks of the template with the values stored in the
     * content definition.
     * @param contentDefinition The actual Content Definition class
     * @return Vector of get-methods
     */
    private Vector getGetMethods(A_CmsContentDefinition contentDefinition) {

        Vector getMethods = new Vector();
        //get all methods of the CD
        Method[] methods = this.getContentDefinitionClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            String name = m.getName().toLowerCase();

            //now extract all methods whose name starts with a "get"
            // TESTFIX (a.kandzior@alkacon.com) Added "boignore" suffix to prevent BO from calling the method
            if (name.startsWith("get") && !(name.endsWith("boignore"))) {
                Class[] param = m.getParameterTypes();
                //only take those methods that have no parameter and return a String
                if (param.length == 0) {

                    Class retType = m.getReturnType();
                    if (retType.equals(java.lang.String.class)) {
                        getMethods.addElement(m);
                    }
                }
            }
        } // for
        return getMethods;
    } //getGetMethods

    /**
     * This methods collects all "setXYZ" methods of the actual content definition. This is used
     * to automatically insert the form parameters into the content definition.
     * @param contentDefinition The actual Content Definition class
     * @return Hashtable of set-methods.
     */
    private Hashtable getSetMethods(A_CmsContentDefinition contentDefinition) {
        Hashtable setMethods = new Hashtable();
        //get all methods of the CD
        Method[] methods = this.getContentDefinitionClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            String name = m.getName().toLowerCase();
            //now extract all methods whose name starts with a "set"
            // TESTFIX (a.kandzior@alkacon.com) Added "boignore" suffix to prevent BO from calling the method
            if (name.startsWith("set") && !(name.endsWith("boignore"))) {
                Class[] param = m.getParameterTypes();
                //only take those methods that have a single string parameter and return nothing
                if (param.length == 1) {
                    // check if the parameter is a string
                    if (param[0].equals(java.lang.String.class)) {
                        Class retType = m.getReturnType();
                        // the return value must be void
                        if (retType.toString().equals("void")) {
                            setMethods.put(m.getName().toLowerCase(), m);
                        }
                    }
                }
            }
        } // for
        return setMethods;
    } //getsetMethods

    /**
     * This method austomatically fills all datablocks in the template that fit to a special name scheme.
     * A datablock named "xyz" is filled with the value of a "getXYZ" method form the content defintion.
     * 
     * @param cms the cms object
     * @param template The template to set the datablocks in.
     * @param contentDefinition The actual content defintion.
     * @param methods A vector with all "getXYZ" methods to be used.
     * @throws CmsException if something goes wrong.
     */
    private void setDatablocks(CmsObject cms, CmsXmlWpTemplateFile template, A_CmsContentDefinition contentDefinition, Vector methods) throws CmsException {
        String methodName = "";
        String datablockName = "";
        String value = "";
        Method method;

        for (int i = 0; i < methods.size(); i++) {
            // get the method name
            method = (Method)methods.elementAt(i);
            methodName = method.getName();
            //get the datablock name - the methodname without the leading "get"
            datablockName = (methodName.substring(3, methodName.length())).toLowerCase();
            //check if the datablock name ends with a "string" if so, remove it from the datablockname
            if (datablockName.endsWith("string")) {
                datablockName = datablockName.substring(0, datablockName.indexOf("string"));
            }
            //now call the method to get the value
            try {
                Object result = method.invoke(contentDefinition, new Object[] {});
                // check if the get method returns null (default value of the CD), set the datablock to
                // to "" then.
                if (result == null) {
                    value = "";
                } else {
                    // cast the result to a string, only strings can be set into datablocks.
                    value = result + "";
                }
                template.setData(datablockName, value);

                // set the escaped value into datablock for unescaping
                String escapedValue = value;
                if (!"".equals(escapedValue.trim())) {
                    escapedValue = CmsEncoder.escape(escapedValue, cms.getRequestContext().getEncoding());
                }
                template.setData(datablockName + "escaped", escapedValue);
            } catch (Exception e) {
                if (OpenCms.getLog(this).isErrorEnabled()) {
                    OpenCms.getLog(this).error("Error during automatic call method '" + methodName, e);
                }
            } // try
        } //for
    } //setDatablocks

    /**
     * This method automatically fills the content definition with the values read from the template.
     * A method "setXYZ(value)" of the content defintion is automatically called and filled with the
     * value of a template input field named "xyz".
     * @param cms The current CmsObject.
     * @param contentDefinition The actual content defintion.
     * @param parameters A hashtable with all template and url parameters.
     * @param setMethods A hashtable with all "setXYZ" methods
     * @throws CmsException if something goes wrong
     */
    private void fillContentDefinition(CmsObject cms, A_CmsContentDefinition contentDefinition, Hashtable parameters, Hashtable setMethods) throws CmsException {

        Enumeration keys = parameters.keys();
        // loop through all values that were returned
        while (keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            String value = parameters.get(key).toString();

            // not check if this parameter might be an uploaded file
            boolean isFile = false;
            // loop through all uploaded files and check if one of those is equal to the value of
            // the tested parameter.
            Enumeration uploadedFiles = CmsXmlTemplateLoader.getRequest(cms.getRequestContext()).getFileNames();
            byte[] content = CmsXmlTemplateLoader.getRequest(cms.getRequestContext()).getFile(value);
            while (uploadedFiles.hasMoreElements()) {
                String filename = (String)uploadedFiles.nextElement();
                // there is a match, so this parameter represents a fileupload.
                if (filename.equals(value)) {
                    isFile = true;
                }
            }
            // try to find a set method for this parameter
            Method m = (Method)setMethods.get("set" + key);

            // there was no method found with this name. So try the name "setXYZString" as an alternative.
            if (m == null) {
                m = (Method)setMethods.get("set" + key + "string");
            }
            // there was a method found
            if (m != null) {
                //now call the method to set the value
                try {

                    m.invoke(contentDefinition, new Object[] {value});

                    // this parameter was the name of an uploaded file. Now try to set the file content
                    // too.
                    if (isFile) {
                        // get the name of the method. It should be "setXYZContent"
                        String methodName = m.getName() + "Content";
                        // now get the method itself.
                        Method contentMethod = this.getContentDefinitionClass().getMethod(methodName, new Class[] {byte[].class});
                        //finally invoke it.
                        contentMethod.invoke(contentDefinition, new Object[] {content});
                    }
                } catch (Exception e) {
                    if (OpenCms.getLog(this).isErrorEnabled()) {
                        OpenCms.getLog(this).error("Error during automatic call method '" + m.getName(), e);
                    }
                } // try
            } //if
        } // while
    } //fillContentDefiniton

    /**
     * Checks how many template selectors are available in the backoffice template and selects the
     * correct one.
     * @param cms The current CmsObject.
     * @param template The current template.
     * @param parameters All template and URL parameters.
     * @param templateSelector The current template selector.
     * @return The new template selector.
     * @throws CmsException if something goes wrong.
     */
    private String getTemplateSelector(CmsObject cms, CmsXmlWpTemplateFile template, Hashtable parameters, String templateSelector) throws CmsException {
        I_CmsSession session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
        //store the old templateSelector in the session
        if (templateSelector != null) {
            session.putValue("backofficepagetemplateselector", templateSelector);
        }
        // Vector to store all template selectors in the template
        Vector selectors = new Vector();
        // Vector to store all useful template selectory in the template
        Vector filteredSelectors = new Vector();
        selectors = template.getAllSections();
        // loop through all templateseletors and find the useful ones
        for (int i = 0; i < selectors.size(); i++) {
            String selector = (String)selectors.elementAt(i);
            if ((!selector.equals(C_DEFAULT_SELECTOR)) && (!selector.equals(C_DONE_SELECTOR))) {
                filteredSelectors.addElement(selector);
            }
        }

        String selectedPage = (String)parameters.get("backofficepage");

        // check if there is more than one useful template selector. If not, do not display the selectbox.
        if (selectors.size() < 4) {
            template.setData("BOSELECTOR", "");
        }

        if (selectedPage != null && selectedPage.length() > 0) {

            templateSelector = selectedPage;
        } else if (filteredSelectors.size() > 0) {
            templateSelector = (String)filteredSelectors.elementAt(0);
            session.putValue("backofficepagetemplateselector", templateSelector);
        }
        session.putValue("backofficepageselectorvector", filteredSelectors);

        return templateSelector;
    }

    /**
     * get the accessFlags from the template
     *
     * @param parameters ??
     * @return the access value
     */
    private int getAccessValue(Hashtable parameters) {
        int accessFlag = 0;
        for (int i = 0; i <= 8; i++) {
            String permissionsAtI = (String)parameters.get("permission_" + i);
            if (permissionsAtI != null) {
                if (permissionsAtI.equals("on")) {
                    accessFlag += new Integer(C_ACCESS_FLAGS[i]).intValue();
                }
            }
        }
        if (accessFlag == 0) {
            accessFlag = C_DEFAULT_PERMISSIONS;
        }
        return accessFlag;
    }

    /**
     * Set the accessFlags in the template
     *
     * @param template the template
     * @param accessFlags the access flags
     */
    private void setAccessValue(CmsXmlWpTemplateFile template, int accessFlags) {
        // permissions check boxes
        for (int i = 0; i <= 8; i++) {
            int accessValueAtI = new Integer(C_ACCESS_FLAGS[i]).intValue();
            if ((accessFlags & accessValueAtI) > 0) {
                template.setData("permission_" + i, "checked");
            } else {
                template.setData("permission_" + i, "");
            }
        }
    }

    /**
     * Set the groups in the template
     *
     * @param cms the cms object
     * @param template the template
     * @param groupId id of the group
     * @throws CmsException if something goes wrong
     */
    private void setGroupSelectbox(CmsObject cms, CmsXmlWpTemplateFile template, CmsUUID groupId) throws CmsException {
        //get all groups
        List cmsGroups = cms.getGroups();
        // select box of group
        String groupOptions = "";
        // name of current group
        String groupName = readSaveGroupName(cms, groupId);
        for (int i = 0; i < cmsGroups.size(); i++) {
            String currentGroupName = ((CmsGroup)cmsGroups.get(i)).getName();
            CmsUUID currentGroupId = ((CmsGroup)cmsGroups.get(i)).getId();
            template.setData("name", currentGroupName);
            template.setData("value", currentGroupId.toString());
            if (!groupId.isNullUUID() && groupName.equals(currentGroupName)) {
                template.setData("check", "selected");
            } else {
                template.setData("check", "");
            }
            groupOptions = groupOptions + template.getProcessedDataValue("selectoption", this);
        }
        template.setData("groups", groupOptions);
        template.setData("groupname", groupName);
    }

    /**
     * Set the owner in the template
     *
     * @param cms the cms object
     * @param template the template
     * @param ownerId the id of the owner
     * @throws CmsException if something goes wrong
     */
    private void setOwnerSelectbox(CmsObject cms, CmsXmlWpTemplateFile template, CmsUUID ownerId) throws CmsException {
        // select box of owner
        String userOptions = "";
        List cmsUsers = cms.getUsers();
        String ownerName = readSaveUserName(cms, ownerId);
        for (int i = 0; i < cmsUsers.size(); i++) {
            String currentUserName = ((CmsUser)cmsUsers.get(i)).getName();
            CmsUUID currentUserId = ((CmsUser)cmsUsers.get(i)).getId();
            template.setData("name", currentUserName);
            template.setData("value", currentUserId.toString());
            if (!ownerId.isNullUUID() && ownerName.equals(currentUserName)) {
                template.setData("check", "selected");
            } else {
                template.setData("check", "");
            }
            userOptions = userOptions + template.getProcessedDataValue("selectoption", this);
        }
        template.setData("users", userOptions);
        template.setData("username", ownerName);
    }

    /**
    * user-method to create a checkbox with the according hidden field
    * @param cms the CmsObject
    * @param tagcontent params of the user-method
    * @param doc the xml document
    * @param userObject additional data
    * @return Object the checkbox definition
    * @throws CmsException if something goes wrong
    */
    public Object checkbox(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws CmsException {
        String name = null;
        String param = "";
        String value = "";
        String checked = null;

        CmsXmlTemplateFile temp = (CmsXmlTemplateFile)doc;
        // separate name from other parameter (if given) ...
        int index = tagcontent.indexOf(",");
        if (index > -1 && index <= tagcontent.length()) {
            name = tagcontent.substring(0, index);
            param = tagcontent.substring(index + 1, tagcontent.length());
        } else {
            name = tagcontent;
        }
        // find out if the box was already selected ...
        value = temp.getDataValue(name);
        if (value.equals("on")) {
            checked = "checked";
        } else {
            checked = "";
        }

        String buf = "<input type=hidden name=\"" + name + "\" value=\"off\">" + "<input type=checkbox name=\"" + name + "\" " + checked + " " + param + ">\n";

        return buf.toString();
    }
}
