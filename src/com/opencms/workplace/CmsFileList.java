/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsFileList.java,v $
* Date   : $Date: 2004/06/21 09:53:52 $
* Version: $Revision: 1.85 $
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

import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.file.types.I_CmsResourceType;
import org.opencms.i18n.CmsEncoder;
import org.opencms.lock.CmsLock;
import org.opencms.main.CmsException;
import org.opencms.main.I_CmsConstants;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsUUID;
import org.opencms.workplace.CmsWorkplaceAction;
import org.opencms.workplace.I_CmsWpConstants;

import com.opencms.legacy.CmsXmlTemplateLoader;
import com.opencms.template.A_CmsXmlContent;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;

/**
 * Class for building a file list. <BR>
 * Called by CmsXmlTemplateFile for handling the special XML tag <code>&lt;FILELIST&gt;</code>.
 * <P>
 * Any template class using the workplace filelist has to implement <code>I_CmsFileListUsers</code>.
 * The given argument <code>callingObject</code> will be checked for this implementation
 * before starting to generate the file list. See <code>I_CmsFileListUsers</code> for
 * more details.
 *
 * @author Michael Emmerich
 * @author Alexander Lucas
 * @author Mario Stanke
 * @version $Revision: 1.85 $ $Date: 2004/06/21 09:53:52 $
 * @see com.opencms.workplace.CmsXmlWpTemplateFile
 */

public class CmsFileList extends A_CmsWpElement {


    /** The head of the file list */
    private final static String C_LIST_HEAD = "list_head";


    /** The entry of the file list */
    private final static String C_LIST_ENTRY = "list_entry";


    /** The lockedby value column */
    private final static String C_LOCKED_VALUE_NOLOCK = "column_lock_value_nolock";


    /** The lockedby value column */
    private final static String C_LOCKED_VALUE_OWN = "column_lock_value_own";


    /** The lockedby value column */
    private final static String C_LOCKED_VALUE_USER = "column_lock_value_user";


    /** The name value column */
    private final static String C_NAME_VALUE_FILE = "column_name_value_file";


    /** The name value column */
    private final static String C_NAME_VALUE_FOLDER = "column_name_value_folder";


    /** The lockedby key */
    private final static String C_LOCKEDBY = "lockedby";


    /** The filefolder key */
    private final static String C_NAME_FILEFOLDER = "name_filefolder";


    /** The style for unchanged files or folders */
    private final static String C_STYLE_UNCHANGED = "dateingeandert";


    /** The style for files or folders not in project*/
    private final static String C_STYLE_NOTINPROJECT = "dateintprojekt";


    /** The style for new files or folders */
    private final static String C_STYLE_NEW = "dateineu";


    /** The style for deleted files or folders */
    private final static String C_STYLE_DELETED = "dateigeloescht";


    /** The style for changed files or folders */
    private final static String C_STYLE_CHANGED = "dateigeaendert";


    /** The default context menu */
    private final static String C_DEFAULT_CONTEXTMENU = "online";


    /** The default context menu */
    private final static String C_DEFAULT_CONTEXTMENUFOLDER = "onlinefolder";


    /** The context link */
    private final static String C_CONTEXT_LINK = "context_link";


    /** The context menu */
    private final static String C_CONTEXT_MENU = "context_menu";


    /** The context menu number */
    private final static String C_CONTEXT_NUMBER = "context_number";


    /** The context menu postfix for lock*/
    private final static String C_CONTEXT_LOCK = "lock";


    /** The context menu postfix for lock user*/
    private final static String C_CONTEXT_LOCKUSER = "user";


    /** Storage for caching icons */
    private Hashtable m_iconCache = new Hashtable();

    /**
     * Check if this resource should be displayed in the filelist.
     * @param cms The CmsObject
     * @param res The resource to be checked.
     * @return True or false.
     * @throws CmsException if something goes wrong.
     */

    private boolean checkAccess(CmsObject cms, CmsResource res) throws CmsException {

		return cms.hasPermissions(res, I_CmsConstants.C_VIEW_ACCESS);
		
    }

    /**
     * Checks which columns in the file list must be displayed.
     * Tests which flags in the user preferences are NOT set and delete those columns in
     * the table generating the file list.
     * @param filelist The filelist flags of the user.
     * @param template The file list template
     * @return Updated file list template.
     */

    private CmsXmlWpTemplateFile checkDisplayedColumns(int filelist,
            CmsXmlWpTemplateFile template, String suffix) {
        if((filelist & C_FILELIST_NAME) == 0) {
            template.setData(C_FILELIST_COLUMN_NAME + suffix, "");
        }
        if((filelist & C_FILELIST_TITLE) == 0) {
            template.setData(C_FILELIST_COLUMN_TITLE + suffix, "");
        }
        if((filelist & C_FILELIST_TYPE) == 0) {
            template.setData(C_FILELIST_COLUMN_TYPE + suffix, "");
        }
        if((filelist & C_FILELIST_STATE) == 0) {
            template.setData(C_FILELIST_COLUMN_STATE + suffix, "");
        }
        if((filelist & C_FILELIST_DATE_LASTMODIFIED) == 0) {
            template.setData(C_FILELIST_COLUMN_CHANGED + suffix, "");
        }
        if((filelist & C_FILELIST_SIZE) == 0) {
            template.setData(C_FILELIST_COLUMN_SIZE + suffix, "");
        }
        if((filelist & C_FILELIST_USER_CREATED) == 0) {
            template.setData(C_FILELIST_COLUMN_OWNER + suffix, "");
        }
        if((filelist & C_FILELIST_GROUP) == 0) {
            template.setData(C_FILELIST_COLUMN_GROUP + suffix, "");
        }
        if((filelist & C_FILELIST_PERMISSIONS) == 0) {
            template.setData(C_FILELIST_COLUMN_ACCESS + suffix, "");
        }
        if((filelist & C_FILELIST_LOCKEDBY) == 0) {
            template.setData(C_FILELIST_COLUMN_LOCKED + suffix, "");
        }
        return template;
    }

    /**
     * Gets a formated access right string form a int access value.
     * @param time The access value as an int.
     * @return Formated access right string.
     */
    // TODO: cw remove after switch to acl
    /*
    private String getAccessFlags(int access) {
        StringBuffer accessFlags = new StringBuffer();
        if((access & C_PERMISSION_READ) > 0) {
            accessFlags.append("r");
        }
        else {
            accessFlags.append("-");
        }
        if((access & C_PERMISSION_WRITE) > 0) {
            accessFlags.append("w");
        }
        else {
            accessFlags.append("-");
        }
        if((access & C_PERMISSION_VIEW) > 0) {
            accessFlags.append("v");
        }
        else {
            accessFlags.append("-");
        }
        if((access & C_ACCESS_GROUP_READ) > 0) {
            accessFlags.append("r");
        }
        else {
            accessFlags.append("-");
        }
        if((access & C_ACCESS_GROUP_WRITE) > 0) {
            accessFlags.append("w");
        }
        else {
            accessFlags.append("-");
        }
        if((access & C_ACCESS_GROUP_VISIBLE) > 0) {
            accessFlags.append("v");
        }
        else {
            accessFlags.append("-");
        }
        if((access & C_ACCESS_PUBLIC_READ) > 0) {
            accessFlags.append("r");
        }
        else {
            accessFlags.append("-");
        }
        if((access & C_ACCESS_PUBLIC_WRITE) > 0) {
            accessFlags.append("w");
        }
        else {
            accessFlags.append("-");
        }
        if((access & C_ACCESS_PUBLIC_VISIBLE) > 0) {
            accessFlags.append("v");
        }
        else {
            accessFlags.append("-");
        }
        if((access & C_ACCESS_INTERNAL_READ) > 0) {
            accessFlags.append("i");
        }
        else {
            accessFlags.append("-");
        }
        return accessFlags.toString();
    }
	*/
    /**
     * Selects the context menue displayed for this entry iin the file list.
     * @param cms The CmsObject.
     * @param res The resource displayed in this entry.
     * @param template The file list template.
     * @return String containing the name of the context menu.
     * @throws Throws CmsException if something goes wrong.
     *
     */

    private String getContextMenue(CmsObject cms, CmsResource res,
            CmsXmlWpTemplateFile template) throws CmsException {
        String contextMenu = null;
        if(res.getState() != I_CmsConstants.C_STATE_DELETED) {

            // test if the resource is in the project or if the online project is displayed
            if((cms.getRequestContext().currentProject().isOnlineProject())
                    //|| (!res.inProject(cms.getRequestContext().currentProject()))) {
                    || (!cms.isInsideCurrentProject(res))) {
                if(res.isFile()) {
                    contextMenu = C_DEFAULT_CONTEXTMENU;
                }
                else {
                    contextMenu = C_DEFAULT_CONTEXTMENUFOLDER;
                }
            }
            else {

                // get the type of the resource
                I_CmsResourceType type = OpenCms.getLoaderManager().getResourceType(res.getTypeId());

                // get the context menu
                contextMenu = type.getTypeName();
                
                CmsLock lock = cms.getLock(res);

                // test if this resource is locked
                if(!lock.isNullLock()) {
                    contextMenu += C_CONTEXT_LOCK;

                    // is this resource locked by the current user
                    if(cms.getRequestContext().currentUser().getId().equals(lock.getUserId())) {
                        contextMenu += C_CONTEXT_LOCKUSER;
                    }
                }
            }
        }
        else {
            contextMenu = "";
        }
        return contextMenu;
    }

    /**
     * Sets the default preferences for the current user if those values are not available.
     * @return Hashtable with default preferences.
     */

    private int getDefaultPreferences(CmsObject cms) {
        int filelist;
        String explorerSettings = (String)cms.getRequestContext().currentUser().getAdditionalInfo(I_CmsConstants.C_ADDITIONAL_INFO_EXPLORERSETTINGS);
        if(explorerSettings != null) {
            filelist = new Integer(explorerSettings).intValue();
        }
        else {
            filelist = C_FILELIST_NAME + C_FILELIST_TITLE + C_FILELIST_TYPE + C_FILELIST_DATE_LASTMODIFIED;
        }
        return filelist;
    }

    /**
     * Gets a list of file and folders of a given vector of folders and files.
     * @param cms The CmsObject.
     * @param list Vector of folders and files.
     * @param doc The Template containing the list definitions.
     * @param lang The language defintion template.
     * @param parameters  Hashtable containing all user parameters.
     * @param callingObject The object calling this class.
     * @param config The config file.
     * @param columnsMethod.
     * @return HTML-Code of the file list.
     */

    private Object getFilelist(CmsObject cms, List list, A_CmsXmlContent doc,
            CmsXmlLanguageFile lang, Hashtable parameters, I_CmsFileListUsers callingObject,
            CmsXmlWpConfigFile config) throws CmsException {
        StringBuffer output = new StringBuffer();
        String title = null;
        int contextNumber = 0;
        String[] tagList =  {
            C_CONTEXT_LINK, C_CONTEXT_MENU, C_CONTEXT_NUMBER, C_FILELIST_ICON_VALUE,
            C_FILELIST_LINK_VALUE, C_FILELIST_LOCK_VALUE, C_FILELIST_NAME_VALUE,
            C_FILELIST_TITLE_VALUE, C_FILELIST_TYPE_VALUE, C_FILELIST_CHANGED_VALUE,
            C_FILELIST_SIZE_VALUE, C_FILELIST_STATE_VALUE, C_FILELIST_OWNER_VALUE,
            C_FILELIST_GROUP_VALUE, C_FILELIST_ACCESS_VALUE, C_FILELIST_LOCKED_VALUE,
            C_NAME_FILEFOLDER, C_LOCKEDBY, C_FILELIST_CLASS_VALUE
        };
        String servlets = CmsXmlTemplateLoader.getRequest(cms.getRequestContext()).getServletUrl();
        // I_CmsSession session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
        // String currentFilelist = (String)session.getValue(C_PARA_FILELIST);
        String currentFilelist = CmsWorkplaceAction.getCurrentFolder(CmsXmlTemplateLoader.getRequest(cms.getRequestContext()).getOriginalRequest());
        
        if((currentFilelist == null) || (currentFilelist.length() == 0)) {
            currentFilelist = "/";
        }

        //get the template
        CmsXmlWpTemplateFile template = (CmsXmlWpTemplateFile)doc;
        Iterator en = null;

        // file and folder object required to create the file list.
        // CmsFile file;
        CmsResource file;
        CmsResource res;

        // show the table head with all required columns.
        // Check which flags in the user preferences are NOT set and delete those columns in
        // the table generating the file list.
        int filelist = getDefaultPreferences(cms);

        // filelist contains a bit pattern indicating which columns have to be shown or hidden.
        // The calling object may now modify this pattern
        filelist = callingObject.modifyDisplayedColumns(cms, filelist);
        template = checkDisplayedColumns(filelist, template, "");
        template = checkDisplayedColumns(filelist, template, C_FILELIST_SUFFIX_VALUE);

        // add the list header to the output.
        output.append(template.getProcessedDataValue(C_LIST_HEAD, callingObject));

        // set all Xml tags somehow so that we can use fastSetXmlData below
        for(int i = 0;i < tagList.length;i++) {
            template.setData(tagList[i], "");
        }

        // go through all folders and files
        en = list.iterator();
        while(en.hasNext()) {
            res = (CmsResource)en.next();
            if(checkAccess(cms, res)) {
                template.setData("PREVIOUS", currentFilelist);
                if(res.isFolder()) {

                    // Set output style class according to the project and state of the file.
                    template.setData(C_FILELIST_CLASS_VALUE, getStyle(cms, res));

                    // set the icon
                    template.fastSetXmlData(C_CONTEXT_LINK, cms.readAbsolutePath(res));
                    template.fastSetXmlData(C_CONTEXT_MENU, getContextMenue(cms, res, template));
                    template.fastSetXmlData(C_CONTEXT_NUMBER, new Integer(contextNumber++).toString());
                    I_CmsResourceType type = OpenCms.getLoaderManager().getResourceType(res.getTypeId());
                    String icon = getIcon(cms, type, config);
                    template.fastSetXmlData(C_FILELIST_ICON_VALUE, CmsXmlTemplateLoader.getRequest(cms.getRequestContext()).getServletUrl() + config.getWpPicturePath() + icon);

                    // set the link, but only if the folder is not deleted
                    if(res.getState() != I_CmsConstants.C_STATE_DELETED) {
                        template.fastSetXmlData(C_FILELIST_LINK_VALUE, cms.readAbsolutePath(res));
                    }
                    else {
                        template.fastSetXmlData(C_FILELIST_LINK_VALUE, "#");
                    }

                    // set the lock icon if nescessary
                    template.fastSetXmlData(C_FILELIST_LOCK_VALUE, template.getProcessedDataValue(getLock(cms,
                            res, template, lang), callingObject));
                    if((filelist & C_FILELIST_NAME) != 0) {

                        // set the folder name
                        template.fastSetXmlData(C_FILELIST_NAME_VALUE, res.getName());
                    }
                    if((filelist & C_FILELIST_TITLE) != 0) {

                        // set the folder title
                        title = "";
                        try {
                            title = CmsEncoder.escapeXml(cms.readProperty(cms.readAbsolutePath(res), I_CmsConstants.C_PROPERTY_TITLE));
                        }
                        catch(CmsException e) {

                        }
                        if(title == null) {
                            title = "";
                        }
                        template.fastSetXmlData(C_FILELIST_TITLE_VALUE, title);
                    }
                    if((filelist & C_FILELIST_TYPE) != 0) {
                        String typename = type.getTypeName();
                        typename = lang.getLanguageValue("fileicon." + typename);
                        template.fastSetXmlData(C_FILELIST_TYPE_VALUE, typename);
                    }
                    if((filelist & C_FILELIST_DATE_LASTMODIFIED) != 0) {

                        // get the folder date
                        long time = res.getDateLastModified();
                        template.fastSetXmlData(C_FILELIST_CHANGED_VALUE, getNiceDate(time));
                    }
                    if((filelist & C_FILELIST_SIZE) != 0) {

                        // get the folder size
                        template.fastSetXmlData(C_FILELIST_SIZE_VALUE, "");
                    }
                    if((filelist & C_FILELIST_STATE) != 0) {

                        // get the folder state
                        template.fastSetXmlData(C_FILELIST_STATE_VALUE, getState(cms, res, lang));
                    }
                    // TODO: Check if this is still needed
                    /*
                    if((filelist & C_FILELIST_USER_CREATED) != 0) {

                        // get the owner of the folder
                        CmsUser owner = cms.readOwner(res);
                        template.fastSetXmlData(C_FILELIST_OWNER_VALUE, owner.getName());
                    }
                    */
                    // TODO: Check if this is still needed
                    /*
                    if((filelist & C_FILELIST_GROUP) != 0) {

                        // get the group of the folder
                        CmsGroup group = cms.readGroup(res);
                        template.fastSetXmlData(C_FILELIST_GROUP_VALUE, group.getName());
                    }
                    */
                    // TODO: remove later
                    /*
                    if((filelist & C_FILELIST_ACCESS) != 0) {

                        // get the access flags
//                        int access = res.getAccessFlags();
                        template.fastSetXmlData(C_FILELIST_ACCESS_VALUE, cms.getPermissionString(res.getName()));
                    }
                    */
                    if((filelist & C_FILELIST_LOCKEDBY) != 0) {

                        // get the locked by
                        CmsLock lock = cms.getLock(res);
                        if(lock.isNullLock()) {
                        template.fastSetXmlData(C_FILELIST_LOCKED_VALUE, "");
                    }
                        else {
                            template.fastSetXmlData(C_FILELIST_LOCKED_VALUE, cms.readUser(lock.getUserId()).getName());
                        }
                    }

                    // Get all customized column values
                    callingObject.getCustomizedColumnValues(cms, template, res, lang);
                    template.fastSetXmlData(C_NAME_FILEFOLDER, template.getProcessedDataValue(getName(cms, res), this));
                }
                else {

                    // file=(CmsFile)res;
                    file = res;

                    // Set output style class according to the project and state of the file.
                    template.fastSetXmlData(C_FILELIST_CLASS_VALUE, getStyle(cms, file));

                    // set the icon
                    template.fastSetXmlData(C_CONTEXT_LINK, cms.readAbsolutePath(res));
                    template.fastSetXmlData(C_CONTEXT_MENU, getContextMenue(cms, res, template));
                    template.fastSetXmlData(C_CONTEXT_NUMBER, new Integer(contextNumber++).toString());
                    I_CmsResourceType type = OpenCms.getLoaderManager().getResourceType(file.getTypeId());
                    String icon = getIcon(cms, type, config);
                    template.fastSetXmlData(C_FILELIST_ICON_VALUE, config.getWpPicturePath() + icon);

                    // set the link, but only if the resource is not deleted
                    if(res.getState() != I_CmsConstants.C_STATE_DELETED) {
                        template.fastSetXmlData(C_FILELIST_LINK_VALUE, servlets + cms.readAbsolutePath(file));
                    }
                    else {
                        template.fastSetXmlData(C_FILELIST_LINK_VALUE, "#");
                    }

                    // set the lock icon if nescessary
                    template.fastSetXmlData(C_FILELIST_LOCK_VALUE, template.getProcessedDataValue(getLock(cms, file, template, lang), callingObject));
                    if((filelist & C_FILELIST_NAME) != 0) {

                        // set the filename
                        template.fastSetXmlData(C_FILELIST_NAME_VALUE, file.getName());
                    }
                    if((filelist & C_FILELIST_TITLE) != 0) {

                        // set the file title
                        title = "";
                        try {
                            title = CmsEncoder.escapeXml(cms.readProperty(cms.readAbsolutePath(file), I_CmsConstants.C_PROPERTY_TITLE));
                        }
                        catch(CmsException e) {

                        }
                        if(title == null) {
                            title = "";
                        }
                        template.fastSetXmlData(C_FILELIST_TITLE_VALUE, title);
                    }
                    if((filelist & C_FILELIST_TYPE) != 0) {

                        // set the file type
                        String typename = type.getTypeName();
                        typename = lang.getLanguageValue("fileicon." + typename);
                        template.fastSetXmlData(C_FILELIST_TYPE_VALUE, typename);
                    }
                    if((filelist & C_FILELIST_DATE_LASTMODIFIED) != 0) {

                        // get the file date
                        long time = file.getDateLastModified();
                        template.fastSetXmlData(C_FILELIST_CHANGED_VALUE, getNiceDate(time));
                    }
                    if((filelist & C_FILELIST_SIZE) != 0) {

                        // get the file size
                        template.fastSetXmlData(C_FILELIST_SIZE_VALUE, new Integer(file.getLength()).toString());
                    }
                    if((filelist & C_FILELIST_STATE) != 0) {

                        // get the file state
                        template.fastSetXmlData(C_FILELIST_STATE_VALUE, getState(cms, file, lang));
                    }
                    // TODO: check if this is still needed
                    /*
                    if((filelist & C_FILELIST_USER_CREATED) != 0) {

                        // get the owner of the file
                        CmsUser owner = cms.readOwner(file);
                        template.fastSetXmlData(C_FILELIST_OWNER_VALUE, owner.getName());
                    }
                    */
                    // TODO: check if this is still needed
                    /*
                    if((filelist & C_FILELIST_GROUP) != 0) {

                        // get the group of the file
                        CmsGroup group = cms.readGroup(file);
                        template.fastSetXmlData(C_FILELIST_GROUP_VALUE, group.getName());
                    }
                    */
                    // TODO: remove later
                    /*
                    if((filelist & C_FILELIST_ACCESS) != 0) {

                        // get the access flags
//                        int access = file.getAccessFlags();
                        template.fastSetXmlData(C_FILELIST_ACCESS_VALUE, cms.getPermissionString(res.getName()));
                    }
                    */
                    if((filelist & C_FILELIST_PERMISSIONS) != 0) {

                        // get the locked by
                        CmsLock lock = cms.getLock(file);
                        if(lock.isNullLock()) {
                        template.fastSetXmlData(C_FILELIST_LOCKED_VALUE, "");
                    }
                        else {
                            template.fastSetXmlData(C_FILELIST_LOCKED_VALUE, cms.readUser(lock.getUserId()).getName());
                        }
                    }

                    // Get all customized column values
                    callingObject.getCustomizedColumnValues(cms, template, res, lang);
                    template.fastSetXmlData(C_NAME_FILEFOLDER, template.getProcessedDataValue(getName(cms, file), this));
                }
                output.append(template.getProcessedDataValue(C_LIST_ENTRY, callingObject));
            }
        }
        return output.toString();
    }

    /**
     * Selects the icon that is displayed in the file list.<br>
     * This method includes cache to prevent to look up in the filesystem for each
     * icon to be displayed
     * @param cms The CmsObject.
     * @param type The resource type of the file entry.
     * @param config The configuration file.
     * @return String containing the complete name of the iconfile.
     * @throws Throws CmsException if something goes wrong.
     */
    private String getIcon(CmsObject cms, I_CmsResourceType type, CmsXmlWpConfigFile config) throws CmsException {
        // check if this icon is in the cache already
        String icon = (String)m_iconCache.get(type.getTypeName());
        // no icon was found, so check if there is a icon file in the filesystem
        if(icon == null) {
            String filename = C_ICON_PREFIX + type.getTypeName().toLowerCase() + C_ICON_EXTENSION;
            try {
                // read the icon file
                cms.readFileHeader(I_CmsWpConstants.C_VFS_PATH_SYSTEMPICS + filename);
                // add the icon to the cache
                icon = filename;
                m_iconCache.put(type.getTypeName(), icon);
            }
            catch(CmsException e) {
                // no icon was found, so use the default
                icon = C_ICON_DEFAULT;
                m_iconCache.put(type.getTypeName(), icon);
            }
        }
        return icon;
    }

    /**
     * Select which lock icon (if nescessary) is selected for a entry in the file list.
     * @param cms The CmsObject.
     * @param file The CmsResource displayed in the the file list.
     * @param template The file list template
     * @param lang The content definition language file.
     * @return HTML code for selecting a lock icon.
     */

    private String getLock(CmsObject cms, CmsResource file, CmsXmlWpTemplateFile template, CmsXmlLanguageFile lang) throws CmsException {
        StringBuffer output = new StringBuffer();
        CmsLock lock = cms.getLock(file);

        // the file is locked
        if(!lock.isNullLock()) {
            CmsUUID locked = lock.getUserId();

            // it is locked by the actuel user
            if(cms.getRequestContext().currentUser().getId().equals(locked)) {
                template.fastSetXmlData(C_LOCKEDBY, lang.getLanguageValue("explorer.lockedby") + cms.getRequestContext().currentUser().getName());
                output.append(C_LOCKED_VALUE_OWN);
            }
            else {
                template.fastSetXmlData(C_LOCKEDBY, lang.getLanguageValue("explorer.lockedby") + cms.readUser(lock.getUserId()).getName());
                output.append(C_LOCKED_VALUE_USER);
            }
        }
        else {
            output.append(C_LOCKED_VALUE_NOLOCK);
        }

        //  output.append(C_LOCKED_VALUE_NOLOCK);
        return output.toString();
    }

    /**
     * Gets the name (including link) for a entry in the file list.
     * @param cms The CmsObject.
     * @param file The CmsResource displayed in the the file list.
     * @return The name used for the actual entry.
     */

    private String getName(CmsObject cms, CmsResource file) {
        StringBuffer output = new StringBuffer();
        if(file.isFile()) {
            output.append(C_NAME_VALUE_FILE);
        }
        else {
            output.append(C_NAME_VALUE_FOLDER);
        }
        return output.toString();
    }

    /**
     * Gets a formated time string form a long time value.
     * @param time The time value as a long.
     * @return Formated time string.
     */

    private String getNiceDate(long time) {
        StringBuffer niceTime = new StringBuffer();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date(time));
        String day = "0" + new Integer(cal.get(Calendar.DAY_OF_MONTH)).intValue();
        String month = "0" + new Integer(cal.get(Calendar.MONTH) + 1).intValue();
        String year = new Integer(cal.get(Calendar.YEAR)).toString();
        String hour = "0" + new Integer(cal.get(Calendar.HOUR) + 12 * cal.get(Calendar.AM_PM)).intValue();
        String minute = "0" + new Integer(cal.get(Calendar.MINUTE));
        if(day.length() == 3) {
            day = day.substring(1, 3);
        }
        if(month.length() == 3) {
            month = month.substring(1, 3);
        }
        if(hour.length() == 3) {
            hour = hour.substring(1, 3);
        }
        if(minute.length() == 3) {
            minute = minute.substring(1, 3);
        }
        niceTime.append(day + ".");
        niceTime.append(month + ".");
        niceTime.append(year + " ");
        niceTime.append(hour + ":");
        niceTime.append(minute);
        return niceTime.toString();
    }

    /**
     * Gets a formated file state string for a entry in the file list.
     * @param cms The CmsObject.
     * @param file The CmsResource displayed in the the file list.
     * @param lang The content definition language file.
     * @return Formated state string.
     */

    private String getState(CmsObject cms, CmsResource file, CmsXmlLanguageFile lang) throws CmsException {
        StringBuffer output = new StringBuffer();
        //if(file.inProject(cms.getRequestContext().currentProject())) {
        if (cms.isInsideCurrentProject(file)) {
            int state = file.getState();
            output.append(lang.getLanguageValue("explorer.state" + state));
        }
        else {
            output.append(lang.getLanguageValue("explorer.statenip"));
        }
        return output.toString();
    }

    /**
     * Gets the style for a entry in the file list.
     * @param cms The CmsObject.
     * @param file The CmsResource displayed in the the file list.
     * @return The style used for the actual entry.
     */

    private String getStyle(CmsObject cms, CmsResource file) throws CmsException {
        StringBuffer output = new StringBuffer();

        // check if the resource is in the actual project
        //if(!file.inProject(cms.getRequestContext().currentProject())) {
        if (!cms.isInsideCurrentProject(file)) {
            output.append(C_STYLE_NOTINPROJECT);
        }
        else {
            if(cms.getRequestContext().currentProject().isOnlineProject()) {

                // check if the actual project is the online project
                output.append(C_STYLE_UNCHANGED);
            }
            else {
                int style = file.getState();
                switch(style) {
                case 0:
                    output.append(C_STYLE_UNCHANGED);
                    break;

                case 1:
                    output.append(C_STYLE_CHANGED);
                    break;

                case 2:
                    output.append(C_STYLE_NEW);
                    break;

                case 3:
                    output.append(C_STYLE_DELETED);
                    break;

                default:
                    output.append(C_STYLE_UNCHANGED);
                }
            }
        }
        return output.toString();
    }

    /**
     * Handling of the special workplace <CODE>&lt;FILELIST&gt;</CODE> tags.
     * <P>
     * Reads the code of a file list from the file list definition file
     * and returns the processed code with the actual elements.
     *
     * @param cms CmsObject Object for accessing resources.
     * @param n XML element containing the <code>&lt;FILELIST&gt;</code> tag.
     * @param callingObject reference to the calling object.
     * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
     * @param lang CmsXmlLanguageFile conataining the currently valid language file.
     * @return Processed button.
     * @throws CmsException
     */

    public Object handleSpecialWorkplaceTag(CmsObject cms, Element n, A_CmsXmlContent doc,
            Object callingObject, Hashtable parameters, CmsXmlLanguageFile lang) throws CmsException {
        String template = n.getAttribute(C_FILELIST_TEMPLATE);
        String customizedTemplate = n.getAttribute(C_FILELIST_CUSTOMTEMPLATE);
        CmsXmlWpTemplateFile filelistTemplate = new CmsXmlWpTemplateFile(cms, template);
        filelistTemplate.setData(C_FILELIST_COLUMN_CUSTOMIZED, "");
        filelistTemplate.setData(C_FILELIST_COLUMN_CUSTOMIZED_VALUE, "");

        // Include the template file for the customized columns
        if(customizedTemplate != null && !"".equals(customizedTemplate)) {
            filelistTemplate.readIncludeFile(customizedTemplate);
        }

        // Check if the callingObject implements our interface
        if(!(callingObject instanceof I_CmsFileListUsers)) {
            throwException("Class " + callingObject.getClass().getName() + " is using a \"FILELIST\" tag in its "
                    + "template file " + doc.getAbsoluteFilename() + ", but does not implement I_CmsFileListUsers. ", CmsException.C_XML_WRONG_TEMPLATE_CLASS);
        }
        CmsXmlWpConfigFile configFile = this.getConfigFile(cms);
        I_CmsFileListUsers filelistUser = (I_CmsFileListUsers)callingObject;
        List filelist = filelistUser.getFiles(cms);
        return getFilelist(cms, filelist, filelistTemplate, lang, parameters, filelistUser, configFile);
    }
}
