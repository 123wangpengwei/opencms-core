/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/CmsWorkplaceManager.java,v $
 * Date   : $Date: 2004/03/19 14:15:16 $
 * Version: $Revision: 1.13 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2004 Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.workplace;

import org.opencms.db.CmsExportPoint;
import org.opencms.file.CmsFolder;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsUser;
import org.opencms.i18n.CmsAcceptLanguageHeaderParser;
import org.opencms.i18n.CmsLocaleManager;
import org.opencms.i18n.I_CmsLocaleHandler;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.I_CmsConstants;
import org.opencms.main.OpenCms;
import org.opencms.workplace.editor.CmsEditorHandler;
import org.opencms.workplace.editor.CmsWorkplaceEditorManager;
import org.opencms.workplace.editor.I_CmsEditorActionHandler;
import org.opencms.workplace.editor.I_CmsEditorHandler;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Manages the global OpenCms workplace settings for all users.<p>
 * 
 * This class reads the settings from the "opencms.properties" and stores them in member variables.
 * For each setting one or more get methods are provided.<p>
 * 
 * @author Andreas Zahner (a.zahner@alkacon.com)
 * @version $Revision: 1.13 $
 * 
 * @since 5.3.1
 */
public final class CmsWorkplaceManager implements I_CmsLocaleHandler {
    
    /** The name of the temp file project */
    public static final String C_TEMP_FILE_PROJECT_NAME = "tempFileProject";
    
    /** The description of the temp file project */
    public static final String C_TEMP_FILE_PROJECT_DESCRIPTION = "The project for temporary Workplace files";
    
    /** Indicates if auto-locking of resources is enabled or disabled */
    private boolean m_autoLockResources;
    
    /** The default locale of the workplace*/
    private Locale m_defaultLocale;
    
    /** The configured dialog handlers */
    private Map m_dialogHandler;
    
    /** The edit action handler */
    private I_CmsEditorActionHandler m_editorAction;
    
    /** The editor handler */
    private I_CmsEditorHandler m_editorHandler;
    
    /** The editor manager */
    private CmsWorkplaceEditorManager m_editorManager;
    
    /** The explorer type settings */
    private List m_explorerTypeSettings;
    
    /** The explorer type settings as Map with resource type name as key */
    private Map m_explorerTypeSettingsMap;
    
    /** The workplace export points */
    private Set m_exportPoints;
    
    /** Maximum size of an upload file */
    private int m_fileMaxUploadSize;
    
    /** Contains all folders that should be labled if siblings exist */
    private List m_labelSiteFolders;
    
    /** Set of installed workplace locales */
    private Set m_locales;
    
    /** Indicates if the user managemet icon should be displayed in the workplace */
    private boolean m_showUserGroupIcon;
    
    /** The configured workplace views */
    private List m_views;
        
    /** The temporary file project used by the editors */
    private CmsProject m_tempFileProject;    
    
    /**
     * Creates a new instance for the workplace manager, will be called by the workplace configuration manager.<p>
     */
    public CmsWorkplaceManager() {
        if (OpenCms.getLog(CmsLog.CHANNEL_INIT).isInfoEnabled()) {
            OpenCms.getLog(CmsLog.CHANNEL_INIT).info(". Workplace init       : starting");
        }
        m_locales = new HashSet();
        m_labelSiteFolders = new ArrayList();
        m_autoLockResources = true;
        m_showUserGroupIcon = true;
        m_dialogHandler = new HashMap();
        m_views = new ArrayList();
        m_exportPoints = new HashSet();
        m_editorHandler = new CmsEditorHandler();
        m_fileMaxUploadSize = -1;
        m_explorerTypeSettings = new ArrayList();
        m_explorerTypeSettingsMap = new HashMap();
    }
    
    /**
     * Returns the id of the temporary file project required by the editors.<p>
     * 
     * @return the id of the temporary file project required by the editors
     */
    public int getTempFileProjectId() {
        if (m_tempFileProject != null) {
            return m_tempFileProject.getId();
        } else {
            return -1;
        }
    }
    
    /**
     * Adds a dialog handler instance to the list of configured dialog handlers.<p>
     * 
     * @param clazz the instanciated dialog handler to add
     */
    public void addDialogHandler(I_CmsDialogHandler clazz) {
        m_dialogHandler.put(clazz.getDialogHandler(), clazz);
        if (OpenCms.getLog(CmsLog.CHANNEL_INIT).isInfoEnabled()) {
            OpenCms.getLog(CmsLog.CHANNEL_INIT).info(". Adding dialog handler: " + clazz.getDialogHandler() + " - " + clazz.getClass().getName());
        }             
    }
    
    /** 
     * Adds an explorer type setting object to the list of type settings.<p>
     * 
     * Adds the type setting as well to a map with the resource type name as key.
     * This map is handy to get the settings for a known resource type.<p>
     * 
     * @param settings the explorer type settings
     */
    public void addExplorerTypeSetting(CmsExplorerTypeSettings settings) {
        m_explorerTypeSettings.add(settings);
        m_explorerTypeSettingsMap.put(settings.getName(), settings);
        if (OpenCms.getLog(CmsLog.CHANNEL_INIT).isInfoEnabled()) {
            OpenCms.getLog(CmsLog.CHANNEL_INIT).info(". Adding type setting  : " + settings.getName());
        }  
    }
        
    /**
     * Adds newly created export point to the workplace configuration.<p>
     * 
     * @param uri the export point uri
     * @param destination the export point destination
     */
    public void addExportPoint(String uri, String destination) {
        CmsExportPoint point = new CmsExportPoint(uri, destination);
        m_exportPoints.add(point);
        if (OpenCms.getLog(CmsLog.CHANNEL_INIT).isInfoEnabled()) {
            OpenCms.getLog(CmsLog.CHANNEL_INIT).info(". Adding export point  : " + point.getUri() + " --> " + point.getDestination());
        }            
    }
    
    /**
     * Adds a folder to the list of labeled folders.<p>
     * 
     * @param uri the folder uri to add
     */
    public void addLabeledFolder(String uri) {
        m_labelSiteFolders.add(uri);
        if (OpenCms.getLog(CmsLog.CHANNEL_INIT).isInfoEnabled()) {
            OpenCms.getLog(CmsLog.CHANNEL_INIT).info(". Label links in folder: " + uri);
        }              
    }
    
    /**
     * Adds a new view to the workplace configuration.<p>
     * 
     * @param key the view key
     * @param uri the view uri
     * @param order the view order
     */
    public void addView(String key, String uri, String order) {
        CmsWorkplaceView view = new CmsWorkplaceView(key, uri, Integer.valueOf(order));
        m_views.add(view);
        if (OpenCms.getLog(CmsLog.CHANNEL_INIT).isInfoEnabled()) {
            OpenCms.getLog(CmsLog.CHANNEL_INIT).info(". Workplace view       : " + view.getUri());
        }          
    }
    
    /**
     * Returns if the autolock resources feature is enabled.<p>
     * 
     * @return true if the autolock resources feature is enabled, otherwise false
     */
    public boolean autoLockResources() {
        return m_autoLockResources;
    }
    
    /**
     * Returns the Workplace default locale.<p>
     * 
     * @return  the Workplace default locale
     */
    public Locale getDefaultLocale() {
        return m_defaultLocale;
    }      
    
    /**
     * Returns all instanciated dialog handlers for the workplace.<p>
     * 
     * @return all instanciated dialog handlers for the workplace
     */
    public Map getDialogHandler() {
        return m_dialogHandler;
    }
    
    /**
     * Returns the instanciated dialog handler class for the key or null, if there is no mapping for the key.<p>
     *  
     * @param key the key whose associated value is to be returned
     * @return the instanciated dialog handler class for the key
     */
    public Object getDialogHandler(String key) {
        return m_dialogHandler.get(key);
    }
    
    /**
     * Returns the instanciated editor action handler class.<p>
     * 
     * @return the instanciated editor action handler class
     */
    public I_CmsEditorActionHandler getEditorActionHandler() {
        return m_editorAction;
    }
    
    /**
     * Returns the instanciated editor handler class.<p>
     * 
     * @return the instanciated editor handler class
     */
    public I_CmsEditorHandler getEditorHandler() {
        return m_editorHandler;
    }
    
    /**
     * Returns the explorer type settings for the specified resource type.<p>
     * 
     * @param type the resource type for which the settings are required
     * @return the explorer type settings for the specified resource type
     */
    public CmsExplorerTypeSettings getEplorerTypeSetting(String type) {
        return (CmsExplorerTypeSettings)m_explorerTypeSettingsMap.get(type);
        
    }
    
    /**
     * Returns the list of explorer type settings.<p>
     * 
     * These settings provide information for the new resource dialog and the context menu appearance.<p>
     * 
     * @return the list of explorer type settings
     */
    public List getExplorerTypeSettings() {
        return m_explorerTypeSettings;
    }
    
    /**
     * Returns the set of configured export points for the workplace.<p>
     *
     * @return the set of configured export points for the workplace
     */
    public Set getExportPoints() {
        return m_exportPoints;
    }
    
    /**
     * Returns the value (in bytes) for the maximum file upload size of the current user.<p>
     * 
     * @param cms the initialized CmsObject
     * @return the value (in bytes) for the maximum file upload size
     */
    public long getFileBytesMaxUploadSize(CmsObject cms) {
        int maxFileSize = getFileMaxUploadSize();
        long maxFileSizeBytes = maxFileSize * 1024;
        boolean isAdmin = false;
        try {
            isAdmin = cms.userInGroup(cms.getRequestContext().currentUser().getName(), OpenCms.getDefaultUsers().getGroupAdministrators());
        } catch (CmsException e) {
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Error checking groups of user " + cms.getRequestContext().currentUser().getName());
            }      
        }
        // check if current user belongs to Admin group, if so no file upload limit
        if ((maxFileSize <= 0) || isAdmin) {
            maxFileSizeBytes = -1;
        }
        return maxFileSizeBytes;
    }
    
    /**
     * Returns the value (in kb) for the maximum file upload size.<p>
     * 
     * @return the value (in kb) for the maximum file upload size
     */
    public int getFileMaxUploadSize() {
        return m_fileMaxUploadSize;
    }
    
    
    /**
     * Returns a list of site folders which generate labeled links.<p>
     * 
     * @return a list of site folders which generate labeled links
     */
    public List getLabelSiteFolders() {
        return m_labelSiteFolders;
    }
    
    /**
     * @see org.opencms.i18n.I_CmsLocaleHandler#getLocale(javax.servlet.http.HttpServletRequest, org.opencms.file.CmsUser, org.opencms.file.CmsProject, java.lang.String)
     */
    public Locale getLocale(HttpServletRequest req, CmsUser user, CmsProject project, String resource) {
        
        // try to read locale from session
        if (req != null) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                CmsWorkplaceSettings settings = (CmsWorkplaceSettings)session.getAttribute(CmsWorkplace.C_SESSION_WORKPLACE_SETTINGS);
                if (settings != null) {
                    return settings.getUserSettings().getLocale();
                }
            }    
        }
        
        // no session available, try to read the locale form the user additional info
        Locale locale = null;
        if (! user.isGuestUser()) {
            // check user settings only for "real" users
            Hashtable userInfo = (Hashtable)user.getAdditionalInfo(I_CmsConstants.C_ADDITIONAL_INFO_STARTSETTINGS);  
            if (userInfo != null) {
                locale = CmsLocaleManager.getLocale((String)userInfo.get(I_CmsConstants.C_START_LOCALE));
            }    
        }
        List acceptedLocales = (new CmsAcceptLanguageHeaderParser(req, getDefaultLocale())).getAcceptedLocales();
        if ((locale != null) && (! acceptedLocales.contains(locale))) {
            acceptedLocales.add(0, locale);
        }
        locale = OpenCms.getLocaleManager().getFirstMatchingLocale(acceptedLocales, m_locales);
        
        // if no locale was found, use the default
        if (locale == null) {
            locale = getDefaultLocale();
        }
        return locale;
    }
    
    /**
     * Returns the set of available workplace locales.<p>
     * 
     * Please note: Be careful not to modify the returned Set as it is not a clone.<p>
     * 
     * @return the set of available workplace locales
     */
    public Set getLocales() {
        return m_locales;        
    }
    
    /**
     * Returns the map of configured workplace views.<p>
     * 
     * @return the map of configured workplace views
     */
    public List getViews() {
        return m_views;
    }
    
    /**
     * Returns the instanciated workplace editor manager class.<p>
     * 
     * @return the instanciated workplace editor manager class
     */
    public CmsWorkplaceEditorManager getWorkplaceEditorManager() {
        return m_editorManager;
    }

    /**
     * @see org.opencms.i18n.I_CmsLocaleHandler#initHandler(org.opencms.file.CmsObject)
     */
    public void initHandler(CmsObject cms) {
        // initialize the workplace locale set
        m_locales = initWorkplaceLocales(cms);
    }    
    
    /**
     * Initializes the workplace manager with the OpenCms system configuration.<p>
     * 
     * @param cms an OpenCms context object that must have been initialized with "Admin" permissions
     * @throws CmsException if something goes wrong
     */    
    public void initialize(CmsObject cms) throws CmsException {
        // sort the views
        Collections.sort(m_views);
        // initialize the workplace editor manager
        m_editorManager = new CmsWorkplaceEditorManager(cms);
        // initialize the locale handler
        initHandler(cms);  
        // sort the explorer type settings
        Collections.sort(m_explorerTypeSettings);
        // create the access control lists for each explorer type
        Iterator i = m_explorerTypeSettings.iterator();
        while (i.hasNext()) {
            CmsExplorerTypeSettings settings = (CmsExplorerTypeSettings)i.next();
            settings.createAccessControlList(cms);
        }
        if (OpenCms.getLog(CmsLog.CHANNEL_INIT).isInfoEnabled()) {
            OpenCms.getLog(CmsLog.CHANNEL_INIT).info(". Workplace config     : vfs access initialized");
        }
        try {
            // read the temporary file project
            m_tempFileProject = cms.readProject(C_TEMP_FILE_PROJECT_NAME);
        } catch (CmsException e) {
            // during initial setup of OpenCms the temp file project does not yet exist...
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Workplace temporary file project does not yet exist!");
            }
        }
    }
    
    /**
     * Initilizes the workplace locale set.<p>
     * 
     * Currently, this is defined by the existence of a special folder 
     * <code>/system/workplace/locales/{locale-name}/".
     * This is likley to change in future implementations.
     * 
     * @param cms an OpenCms context object that must have been initialized with "Admin" permissions
     * @return the workplace locale set
     */
    private Set initWorkplaceLocales(CmsObject cms) {
        m_locales = new HashSet();
        List localeFolders;
        try {
            localeFolders = cms.getSubFolders(I_CmsWpConstants.C_VFS_PATH_LOCALES);
        } catch (CmsException e) {
            OpenCms.getLog(this).error("Unable to read locales folder " + I_CmsWpConstants.C_VFS_PATH_LOCALES, e);
            localeFolders = new ArrayList();
        }
        Iterator i = localeFolders.iterator();
        while (i.hasNext()) {
            CmsFolder folder = (CmsFolder)i.next();
            Locale locale = CmsLocaleManager.getLocale(folder.getName());
            // add locale
            m_locales.add(locale);
            // add less specialized locale
            m_locales.add(new Locale(locale.getLanguage(), locale.getCountry()));    
            // add even less specialized locale            
            m_locales.add(new Locale(locale.getLanguage()));         
        }        
        return m_locales;
    }
    
    
    /**
     * Sets if the autolock resources feature is enabled.<p>
     * 
     * @param value "true" if the autolock resources feature is enabled, otherwise false
     */
    public void setAutoLock(String value) {
        m_autoLockResources = Boolean.valueOf(value).booleanValue();
        if (OpenCms.getLog(CmsLog.CHANNEL_INIT).isInfoEnabled()) {
            OpenCms.getLog(CmsLog.CHANNEL_INIT).info(". Auto lock feature    : " + (m_autoLockResources?"enabled":"disabled"));
        }        
    }

    /**
     * Sets the Workplace default locale.<p>
     * 
     * @param locale the locale to set
     */
    public void setDefaultLocale(String locale) {
        try {
            m_defaultLocale = CmsLocaleManager.getLocale(locale);
        } catch (Exception e) {
            if (OpenCms.getLog(CmsLog.CHANNEL_INIT).isWarnEnabled()) {
                OpenCms.getLog(CmsLog.CHANNEL_INIT).warn(". Workplace init       : non-critical error " + e.toString());
            }
        }        
        if (OpenCms.getLog(CmsLog.CHANNEL_INIT).isInfoEnabled()) {
            OpenCms.getLog(CmsLog.CHANNEL_INIT).info(". Workplace init       : Default locale is '" + m_defaultLocale + "'");
        }        
    }
    
    /**
     * Sets the editor action class.<p>
     * 
     * @param clazz the editor action class to set
     */
    public void setEditorAction(I_CmsEditorActionHandler clazz) {
        m_editorAction = clazz;
        if (OpenCms.getLog(CmsLog.CHANNEL_INIT).isInfoEnabled()) {
            OpenCms.getLog(CmsLog.CHANNEL_INIT).info(". Editor action class  : " + m_editorAction.getClass().getName());
        }        
    }
    
    /**
     * Sets the editor handler class.<p>
     * 
     * @param clazz the editor handler class to set
     */
    public void setEditorHandler(I_CmsEditorHandler clazz) {            
        m_editorHandler = clazz;
        if (OpenCms.getLog(CmsLog.CHANNEL_INIT).isInfoEnabled()) {
            OpenCms.getLog(CmsLog.CHANNEL_INIT).info(". Editor handler class : " + m_editorHandler.getClass().getName());
        }
    }

    /**
     * Sets the value (in kb) for the maximum file upload size.<p>
     * 
     * @param value the value (in kb) for the maximum file upload size
     */
    public void setFileMaxUploadSize(String value) {
        try {
            m_fileMaxUploadSize = Integer.valueOf(value).intValue();
        } catch (NumberFormatException e) {
            m_fileMaxUploadSize = -1;
        }
        if (OpenCms.getLog(CmsLog.CHANNEL_INIT).isInfoEnabled()) {
            OpenCms.getLog(CmsLog.CHANNEL_INIT).info(". File max. upload size: " + (m_fileMaxUploadSize > 0 ? (m_fileMaxUploadSize + " KB") : "unlimited"));
        }        
    }
    
    /**
     * Controls if the user/group icon in the administration view should be shown.<p>
     * 
     * @param value "true" if the user/group icon in the administration view should be shown, otherwise false
     */
    public void setUserManagementEnabled(String value) {
        m_showUserGroupIcon = Boolean.valueOf(value).booleanValue();
        if (OpenCms.getLog(CmsLog.CHANNEL_INIT).isInfoEnabled()) {
            OpenCms.getLog(CmsLog.CHANNEL_INIT).info(". User managememnt icon: " + (m_showUserGroupIcon?"enabled":"disabled"));
        }         
    }
    
    /**
     * Returns if the user/group icon in the administration view should be shown.<p>
     * 
     * @return true if the user/group icon in the administration view should be shown, otherwise false
     */
    public boolean showUserGroupIcon() {
        return m_showUserGroupIcon;
    }
}
