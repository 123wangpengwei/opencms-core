/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/Attic/CmsRequestContext.java,v $
* Date   : $Date: 2003/07/31 13:19:37 $
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

package com.opencms.file;

import org.opencms.db.CmsDriverManager;

import com.opencms.boot.I_CmsLogChannels;
import com.opencms.core.A_OpenCms;
import com.opencms.core.CmsException;
import com.opencms.core.CmsExportRequest;
import com.opencms.core.CmsSession;
import com.opencms.core.I_CmsConstants;
import com.opencms.core.I_CmsRequest;
import com.opencms.core.I_CmsResponse;
import com.opencms.core.I_CmsSession;
import com.opencms.flex.util.CmsResourceTranslator;
import com.opencms.workplace.I_CmsWpConstants;

import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * This class provides access to the CmsRequestContext.
 * <br>
 * In the CmsRequestContext class are all methods bundled, which can inform about the
 * current request properties, like  the url or uri of the request.
 * <p>
 *
 * @author Andreas Schouten
 * @author Michael Emmerich
 * @author Anders Fugmann
 * @author Alexander Lucas
 *
 * @version $Revision: 1.85 $ $Date: 2003/07/31 13:19:37 $
 *
 */
public class CmsRequestContext {
    
    public static final String C_USER_SITEROOT = "CmsRequestContext.siteRoot";

    /** The rb to get access to the OpenCms */
    private CmsDriverManager m_driverManager;

    /** The current CmsRequest */
    private I_CmsRequest m_req;

    /** The current CmsResponse */
    private I_CmsResponse m_resp;

    /** The current user */
    private CmsUser m_user;

    /** The current group of the user */
    private CmsGroup m_currentGroup;

    /** The current project */
    private CmsProject m_currentProject;

    /** In export mode the links in pages will be stored in this vector for further processing */
    private Vector m_links;

    /** Flag to indicate that this request is event controlled */
    private boolean m_eventControlled = false;
    
    /** Flag to indicate that this context should not update the user session */
    private boolean m_updateSession = true;
    
    /**
     * In export mode this vector is used to store all dependencies this request
     * may have. It is saved to the database and if one of the dependencies changes
     * the request will be exported again.
     */
    private Vector m_dependencies;

    /** Current languages */
    private Vector m_language = new Vector();

    /** The name of the root, e.g. /site_a/vfs */
    private String m_siteRoot = I_CmsConstants.C_VFS_DEFAULT;

    /** Current encoding */
    private String m_encoding = null;

    /** The URI for getUri() in case it is "overwritten"  */
    private String m_uri = null;
    
    /** Directroy name translator */
    private CmsResourceTranslator m_directoryTranslator = null;

    /** File name translator */
    private CmsResourceTranslator m_fileTranslator = null;

    /** A map for storing (optional) request context attributes */
    private HashMap m_attributeMap = null; 
    
    /**
     * The default constructor.
     */
    public CmsRequestContext() {
        super();
    }
    
    /**
     * Initializes this RequestContext.<p>
     *
     * @param driverManager the driver manager
     * @param req the CmsRequest
     * @param resp the CmsResponse
     * @param user the current user for this request
     * @param group the current group for this request
     * @param projectId the id of the current project for this request
     * @param site the current site root
     * @param directoryTranslator Translator for directories (file with full path)
     * @param fileTranslator Translator for new file names (without path)
     * @throws CmsException if operation was not successful.
     */
    void init (
        CmsDriverManager driverManager, 
        I_CmsRequest req, 
        I_CmsResponse resp, 
        String user, 
        String group, 
        int projectId, 
        String site, 
        CmsResourceTranslator directoryTranslator, 
        CmsResourceTranslator fileTranslator
    ) throws CmsException {

        m_driverManager = driverManager;
        m_req = req;
        m_resp = resp;
        m_links = new Vector();
        m_dependencies = new Vector();
        m_siteRoot = site;
        
        //CmsProject project = null;
        
        try {
            m_user = m_driverManager.readUser(user);
        } catch (CmsException ex) {
        }
        // if no user found try to read webUser
        if (m_user == null) {
            m_user = m_driverManager.readWebUser(this, user);
        }

        // check, if the user is disabled
        if (m_user.getDisabled()) {
            m_user = null;
        }

        // set current project, group and streaming proerties for this request
        try {
            setCurrentProject(projectId);
        } catch (CmsException exc) {
            // there was a problem to set the needed project - using the online one
            setCurrentProject(I_CmsConstants.C_PROJECT_ONLINE_ID);
        }
        
        m_currentGroup = m_driverManager.readGroup(this, group);
        m_directoryTranslator = directoryTranslator;
        m_fileTranslator = fileTranslator;

        // Analyze the user's preferred languages coming with the request
        if (req != null) {
            try {
                HttpServletRequest httpReq =
                    (HttpServletRequest) req.getOriginalRequest();
                String accLangs = null;
                if (httpReq != null) {
                    accLangs = httpReq.getHeader("Accept-Language");
                }
                if (accLangs != null) {
                    StringTokenizer toks = new StringTokenizer(accLangs, ",");
                    while (toks.hasMoreTokens()) {
                        // Loop through all languages and cut off trailing extensions
                        String current = toks.nextToken().trim();
                        if (current.indexOf("-") > -1) {
                            current =
                                current.substring(0, current.indexOf("-"));
                        }
                        if (current.indexOf(";") > -1) {
                            current =
                                current.substring(0, current.indexOf(";"));
                        }
                        m_language.addElement(current);

                    }
                }
            } catch (UnsupportedOperationException e) {
                // noop
            }

            // Initialize encoding 
            initEncoding();
        }
    }

    /**
     * Adds a link for the static export.<p>
     * 
     * @param link the link to add
     */
    public void addLink(String link) {
        m_links.add(link);
    }

    /**
     * Returns all links that the template mechanism has registered.
     * 
     * @return all registered links
     */
    public Vector getLinkVector() {
        return m_links;
    }

    /**
     * Adds a dependency.
     * 
     * @param rootName The root name of the resource
     */
    public void addDependency(String rootName) {
        m_dependencies.add(rootName);
    }

    /**
     * Returns all dependencies the templatemechanism has registered.
     * 
     * @return all registered dependencies
     */
    public Vector getDependencies() {
        return m_dependencies;
    }

    /**
     * Returns the current folder object.
     *
     * @return the current folder object.
     * @throws CmsException if operation was not successful.
     */
    public CmsFolder currentFolder() throws CmsException {
        return (
            m_driverManager.readFolder(
                this,
                addSiteRoot(getFolderUri())
                ));
    }
    
    /**
     * Returns the current group of the current user.
     *
     * @return the current group of the current user.
     */
    public CmsGroup currentGroup() {
        return (m_currentGroup);
    }
    
    /**
     * Returns the current project of the current user.
     *
     * @return the current project of the current user.
     */
    public CmsProject currentProject() {
        return m_currentProject;
    }
    
    /**
     * Returns the current user object.
     *
     * @return the current user object.
     */
    public CmsUser currentUser() {
        return (m_user);
    }
    
    /**
     * Gets the name of the requested file without any path-information.
     *
     * @return the requested filename.
     */
    public String getFileUri() {
        String uri = m_req.getRequestedResource();
        uri = uri.substring(uri.lastIndexOf("/") + 1);
        return uri;
    }
    
    /**
     * Gets the remote ip address.<p>
     * 
     * @return the ip addresss as string
     */
    public String getRemoteAddress() {
        if ((m_req != null) && (m_req.getOriginalRequestType() == I_CmsConstants.C_REQUEST_HTTP))
            return ((HttpServletRequest)m_req.getOriginalRequest()).getRemoteAddr();
        else
            return null;
    }
    
   /**
    * Gets the name of the parent folder of the requested file
    *
    * @return the requested filename.
    */
    public String getFolderUri() {
        return getUri().substring(0, getUri().lastIndexOf("/") + 1);
    }
    
    /**
     * Gets the current request, if availaible.
     *
     * @return the current request, if availaible.
     */
    public I_CmsRequest getRequest() {
        return (m_req);
    }
    
    /**
     * Gets the current response, if availaible.
     *
     * @return the current response, if availaible.
     */
    public I_CmsResponse getResponse() {
        return (m_resp);
    }
    
    /**
     * Gets the Session for this request.<p>
     *
     * This method should be used instead of the originalRequest.getSession() method.
     * 
     * @param value indicates, if a session should be created when a session for the particular client does not already exist.
     * @return the CmsSession, or <code>null</code> if no session already exists and value was set to <code>false</code>
     *
     */
    public I_CmsSession getSession(boolean value) {
        HttpSession session =
            ((HttpServletRequest) m_req.getOriginalRequest()).getSession(value);
        if (session != null) {
            return (I_CmsSession) new CmsSession(session);
        } else {
            return null;
        }
    }
    
    /**
     * Gets the uri for the requested resource.<p>
     * 
     * For a http request, the name of the resource is extracted as follows:<br>
     * <CODE>http://{servername}/{servletpath}/{path to the cms resource}</CODE><br>
     * In the following example:<br>
     * <CODE>http://my.work.server/servlet/opencms/system/def/explorer</CODE><br>
     * the requested resource is <CODE>/system/def/explorer</CODE>.
     *
     * @return the path to the requested resource.
     */
    public String getUri() {
        if (m_uri != null) return m_uri;
        if (m_req != null) {
            return m_req.getRequestedResource();
        } else {
            return I_CmsConstants.C_ROOT;
        }
    }

    /**
     * Set the value that is returned by getUri()
     * to the provided String.<p>
     * 
     * This is required in a context where
     * a cascade of included XMLTemplates are combined with JSP or other
     * Templates that use the ResourceLoader interface.
     * You need to fake the URI because the ElementCache always
     * uses cms.getRequestContext().getUri() even if you called
     * CmsXmlLauncher.generateOutput() with a differnt file name.
     *
     * @param value The value to set the Uri to, must be a complete OpenCms path name like /system/workplace/stlye.css
     * @since 5.0 beta 1
     */
    public void setUri(String value) {
        m_uri = value;
    }

    /**
     * Determines if the users is in the admin-group.
     *
     * @return <code>true</code> if the users current group is the admin-group; <code>false</code> otherwise.
     * @throws CmsException if operation was not successful.
     */
    public boolean isAdmin() throws CmsException {
        return (m_driverManager.isAdmin(this));
    }

    /**
     * Determines if the users current group is the projectmanager-group.<p>
     * 
     * All projectmanagers can create new projects, or close their own projects.
     *
     * @return <code>true</code> if the users current group is the projectleader-group; <code>false</code> otherwise.
     * @throws CmsException if operation was not successful.
     */
    public boolean isProjectManager() throws CmsException {
        return (m_driverManager.isProjectManager(this));
    }

    /**
     * Sets the current group of the current user.
     *
     * @param groupname the name of the group to be set as current group.
     * @throws CmsException if operation was not successful.
     */
    public void setCurrentGroup(String groupname) throws CmsException {

        // is the user in that group?
        if (m_driverManager
            .userInGroup(
                this,
                m_user.getName(),
                groupname)) {
            // Yes - set it to the current Group.
            m_currentGroup =
                m_driverManager.readGroup(this, groupname);
        } else {
            // No - throw exception.
            throw new CmsException(
                "[" + this.getClass().getName() + "] " + groupname,
                CmsException.C_NO_ACCESS);
        }
    }

    /**
     * Sets the current project for the user.<p>
     *
     * @param projectId the id of the project to be set as current project
     * @return the CmsProject instance
     * @throws CmsException if operation was not successful
     */
    public CmsProject setCurrentProject(int projectId) throws CmsException {
        CmsProject newProject =
            m_driverManager.readProject(this, projectId);
        if (newProject != null) {
            m_currentProject = newProject;
        }
        return (m_currentProject);
    }
    
    /**
     * Get a Vector of all accepted languages for this request.
     * Languages are coded in international shortcuts like "en" or "de".
     * If the browser has sent special versions of languages (e.g. "de-ch" for Swiss-German)
     * these extensions will be cut off.
     * @return Vector of Strings with language codes or <code>null</code> if no request object is available.
     */
    public Vector getAcceptedLanguages() {
        return m_language;
    }
    
    /**
     * Returns the name of a resource with the complete site root name,
     * (e.g. /default/vfs/index.html) by adding the currently set site root prefix.<p>
     *
     * @param resourcename the resource name
     * @return the resource name including site root
     */
    public String addSiteRoot(String resourcename) {
        if (resourcename == null) return null;
        // TODO: hack - added logic to have a / between site root and resourcename if missing
        String siteRoot = getAdjustedSiteRoot(resourcename);
        resourcename =  siteRoot + ((siteRoot.endsWith("/") || resourcename.startsWith("/")) ? "" : "/") + resourcename;
        return m_directoryTranslator.translateResource(resourcename);
    }    
    
    /**
     * Removes the current site root prefix from the absolute path in the resource name,
     * i.e. adjusts the resource name for the current site root.<p> 
     * 
     * @param resourcename the resource name
     * @return the resource name adjusted for the current site root
     */   
    public String removeSiteRoot(String resourcename) {
        String siteRoot = getAdjustedFullSiteRoot(resourcename);
        if (resourcename.startsWith(siteRoot)) {
            resourcename = resourcename.substring(siteRoot.length());
        }
        return resourcename;
    }

    /**
     * Returns the adjusted site root for a resoure that has the full path 
     * set, e.g. /default/vfs/system/.<p>
     * 
     * @param resourcename the resource name to get the full adjusted site root for
     * @return the adjusted site root for a resoure
     */      
    public String getAdjustedFullSiteRoot(String resourcename) {
        if (resourcename.startsWith(I_CmsConstants.C_VFS_DEFAULT + "/system/")) {
            return I_CmsConstants.C_VFS_DEFAULT;
        } else {
            return m_siteRoot;
        }
    }
    
    /**
     * Returns the adjusted site root for a resoure that has not the full path
     * set, e.g. /system/.<p>
     * 
     * @param resourcename the resource name to get the adjusted site root for
     * @return the adjusted site root for a resoure
     */
    public String getAdjustedSiteRoot(String resourcename) {
        if (resourcename.startsWith("/system/")) {
            return I_CmsConstants.C_VFS_DEFAULT;
        } else {
            return m_siteRoot;
        }  
    }
    
    /**
     * Returns the current root directory in the virtual file system.<p>
     *      * @return the current root directory in the virtual file system
     */
    public String getSiteRoot() {
        return m_siteRoot;
    }

    /**
     * Sets the current root directory in the virtual file system.<p>
     * 
     * @param root the name of the new root directory
     */
    public void setSiteRoot(String root) {
        // site roots must never end with a "/"
        if (root.endsWith("/")) {
            m_siteRoot = root.substring(0, root.length()-1);
        } else {        
            m_siteRoot = root;
        }
    }
    
    /**
     * @return The directory name translator this context was initialized with
     */
    public CmsResourceTranslator getDirectoryTranslator() {
        return m_directoryTranslator;
    }
    
    /**
     * @return The file name translator this context was initialized with
     */
    public CmsResourceTranslator getFileTranslator() {
        return m_fileTranslator;
    }    
        
    /**
     * Detects current content encoding to be used in HTTP response
     * based on requested resource or session state.
     */
    public void initEncoding() {
        try {
            m_encoding = m_driverManager.readProperty(this, addSiteRoot(m_req.getRequestedResource()), getAdjustedSiteRoot(m_req.getRequestedResource()), I_CmsConstants.C_PROPERTY_CONTENT_ENCODING, true);
        } catch (CmsException e) {
            m_encoding = null;
        }
        if ((m_encoding != null) && ! "".equals(m_encoding)) {
            // encoding was read from resource property
            return;
        } else if ((getUri().startsWith(I_CmsWpConstants.C_VFS_PATH_SYSTEM)) && (! (m_req instanceof CmsExportRequest))) {
            // try to get encoding from session for special system folder only                
            if (I_CmsLogChannels.C_LOGGING && A_OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_DEBUG)) {                                
                A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_DEBUG,
                    "[" + getClass().getName() + "] can't get encoding property for resource "
                    + m_req.getRequestedResource() + ", trying to get it from session.");
            }                    
            I_CmsSession session = getSession(false);
            if (session != null) {
                m_encoding = (String)session.getValue(I_CmsConstants.C_SESSION_CONTENT_ENCODING);
            }
        }
        if (m_encoding == null || "".equals(m_encoding)) {
            // no encoding found - use default one
            if (I_CmsLogChannels.C_LOGGING && A_OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_DEBUG)) {                                
                A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_DEBUG,
                    "[" + getClass().getName() + "] no encoding found - using default: " + A_OpenCms.getDefaultEncoding());
            }                  
            m_encoding = A_OpenCms.getDefaultEncoding();
        }
    }

    /**
     * Returns the current content encoding to be used in HTTP response
     * 
     * @return the encoding
     */
    public String getEncoding() {
        return m_encoding;
    }

    /**
     * Sets the current content encoding to be used in HTTP response
     * 
     * @param encoding the encoding
     */
    public void setEncoding(String encoding) {
        setEncoding(encoding, false);
    }

    /**
     * Sets the current content encoding to be used in HTTP response
     * and store it in session if it is available
     * 
     * @param encoding the encoding
     * @param storeInSession flag to store the encoding
     */
    public void setEncoding(String encoding, boolean storeInSession) {
        m_encoding = encoding;
        if (!storeInSession) {
            return;
        }
        I_CmsSession session = getSession(false);
        if (session != null) {
            session.putValue(
                I_CmsConstants.C_SESSION_CONTENT_ENCODING,
                m_encoding);
        }
    }

    /**
     * Mark this request context as event controlled.<p>
     * 
     * @param value true if the request is event controlled, false otherwise
     */
    public void setEventControlled(boolean value) {
        m_eventControlled = value;
    }

    /**
     * Check if this request context is event controlled.<p>
     * 
     * @return true if the request context is event controlled, false otherwise
     */
    public boolean isEventControlled() {
        return m_eventControlled;
    }
    
    /**
     * Mark this request context to update the session or not.<p>
     *
     * @param value true if this request context will update the session, false otherwise
     */
    public void setUpdateSessionEnabled(boolean value) {
        m_updateSession = value;
    }

    /**
     * Check if this request context will update the session.<p>
     *
     * @return true if this request context will update the session, false otherwise
     */
    public boolean isUpdateSessionEnabled() {
        return m_updateSession;
    }   
    
    /**
     * Gets the value of an attribute from the OpenCms request context attribute list.<p>
     * 
     * @param attributeName the attribute name
     * @return Object the attribute value, or <code>null</code> if the attribute was not found
     */
    public Object getAttribute(String attributeName) {
        if (m_attributeMap == null) return null;
        return m_attributeMap.get(attributeName);
    }
    
    /**
     * Sets an attribute in the request context.<p>
     * 
     * @param key the attribute name
     * @param value the attribute value
     */
    public void setAttribute(String key, Object value) {
        if (m_attributeMap == null) m_attributeMap = new HashMap();
        m_attributeMap.put(key, value);
    } 
}
