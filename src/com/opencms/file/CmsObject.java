/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/Attic/CmsObject.java,v $
* Date   : $Date: 2003/08/28 11:19:15 $
* Version: $Revision: 1.389 $
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
import org.opencms.loader.CmsXmlTemplateLoader;
import org.opencms.lock.CmsLock;
import org.opencms.main.OpenCms;
import org.opencms.security.CmsAccessControlEntry;
import org.opencms.security.CmsAccessControlList;
import org.opencms.security.CmsPermissionSet;
import org.opencms.security.CmsSecurityException;
import org.opencms.security.I_CmsPrincipal;
import org.opencms.synchronize.CmsSynchronize;

import com.opencms.boot.I_CmsLogChannels;
import com.opencms.core.CmsCoreSession;
import com.opencms.core.CmsException;
import com.opencms.core.CmsExportRequest;
import com.opencms.core.CmsExportResponse;
import com.opencms.core.I_CmsConstants;
import com.opencms.core.I_CmsRequest;
import com.opencms.core.I_CmsResponse;
import com.opencms.flex.CmsEvent;
import com.opencms.flex.I_CmsEventListener;
import com.opencms.flex.util.CmsResourceTranslator;
import com.opencms.flex.util.CmsUUID;
import com.opencms.linkmanagement.CmsPageLinks;
import com.opencms.linkmanagement.LinkChecker;
import com.opencms.report.CmsShellReport;
import com.opencms.report.I_CmsReport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import source.org.apache.java.util.Configurations;

/**
 * This class provides access to the OpenCms and its resources.
 * <br>
 * The CmsObject encapsulates user identification and client requests and
 * is the central object to transport information in the Cms Servlet.
 * <br>
 * All operations on the CmsObject are forwarded to the class which extends A_CmsRessourceBroker
 * to ensure user authentification in all operations.
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 * @version $Revision: 1.389 $
 */
public class CmsObject {

    /** Internal debug flag, set to 9 for maximum verbosity */
    private static final int DEBUG = 0;

    /**
     * The request context.
     */
    private CmsRequestContext m_context;

    /**
     * The driver manager to access the cms.
     */
    private CmsDriverManager m_driverManager;

    /**
     * The class for linkmanagement.
     */
    private LinkChecker m_linkChecker;

    /**
     * the modus the cmsObject runs in (used i.e. for static export)
     */
    private int m_mode = I_CmsConstants.C_MODUS_AUTO;

    /**
     * Method that can be invoked to find out all currently logged in users.
     */
    private CmsCoreSession m_sessionStorage;

    /**
     * The default constructor.
     */
    public CmsObject() {
    }

    /**
     * Initializes the CmsObject without a request-context (current-user,
     * current-group, current-project).
     *
     * @param driverManager the driver manager to access the database.
     * @throws CmsException if operation was not successful.
     */
    public void init(CmsDriverManager driverManager) throws CmsException {
        m_driverManager = driverManager;
    }

    /**
     * Initializes the CmsObject for each request.
     *
     * @param driverManager the driver manager to access the database.
     * @param req the CmsRequest
     * @param resp the CmsResponse
     * @param user the current user for this request
     * @param projectId the current projectId for this request
     * @param site the current site root of the user
     * @param directoryTranslator Translator for directories (file with full path)
     * @param fileTranslator Translator for new file names (without path)
     * @throws CmsException if operation was not successful.
     */
    public void init(
        CmsDriverManager driverManager,
        I_CmsRequest req, 
        I_CmsResponse resp, 
        String user, 
        int projectId, 
        String site, 
        CmsCoreSession sessionStorage, 
        CmsResourceTranslator directoryTranslator, 
        CmsResourceTranslator fileTranslator
    ) throws CmsException {
        m_sessionStorage = sessionStorage;
        m_driverManager = driverManager;
        m_context = new CmsRequestContext();
        m_context.init(m_driverManager, req, resp, user, projectId, site, directoryTranslator, fileTranslator);
        try {
            m_linkChecker = new LinkChecker();
        } catch (java.lang.NoClassDefFoundError error) {
            // ignore this error - no substitution is needed here
        }
    }
    
    /**
     * Accept a task from the Cms.
     *
     * @param taskid the id of the task to accept.
     *
     * @throws CmsException if operation was not successful.
     */
    public void acceptTask(int taskId) throws CmsException {
        m_driverManager.acceptTask(m_context, taskId);
    }

    /**
     * Checks if the user can access the project.
     *
     * @param projectId the id of the project.
     * @return <code>true</code>, if the user may access this project; <code>false</code> otherwise
     *
     * @throws CmsException if operation was not successful.
     */
    public boolean accessProject(int projectId) throws CmsException {
        return (m_driverManager.accessProject(m_context, projectId));
    }

    /**
     * Adds a file extension to the list of known file extensions.
     * <p>
     * <b>Security:</b>
     * Only members of the group administrators are allowed to add a file extension.
     *
     * @param extension a file extension like "html","txt" etc.
     * @param resTypeName name of the resource type associated with the extension.
     *
     * @throws CmsException if operation was not successful.
     */

    public void addFileExtension(String extension, String resTypeName) throws CmsException {
        m_driverManager.addFileExtension(m_context, extension, resTypeName);
    }

    /**
     * Adds a user to the Cms by import.
     * <p>
     * <b>Security:</b>
     * Only members of the group administrators are allowed to add a user.
     *
     * @param id the id of the user
     * @param name the new name for the user.
     * @param password the new password for the user.
     * @param recoveryPassword the new password for the user.
     * @param description the description for the user.
     * @param firstname the firstname of the user.
     * @param lastname the lastname of the user.
     * @param email the email of the user.
     * @param flags the flags for a user (e.g. C_FLAG_ENABLED).
     * @param additionalInfos a Hashtable with additional infos for the user. These
     * Infos may be stored into the Usertables (depending on the implementation).
     * @param defaultGroup the default groupname for the user.
     * @param address the address of the user.
     * @param section the section of the user.
     * @param type the type of the user.
     *
     * @return a <code>CmsUser</code> object representing the added user.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsUser addImportUser(String id, String name, String password, String recoveryPassword, String description, String firstname, String lastname, String email, int flags, Hashtable additionalInfos, String defaultGroup, String address, String section, int type) throws CmsException {
        return (m_driverManager.addImportUser(m_context, id, name, password, recoveryPassword, description, firstname, lastname, email, flags, additionalInfos, defaultGroup, address, section, type));
    }

    /**
     * Returns the name of a resource with the complete site root name,
     * (e.g. /default/vfs/index.html) by adding the currently set site root prefix.<p>
     *
     * @param resourcename the resource name
     * @return the resource name including site root
     */
    private String addSiteRoot(String resourcename) {
        return getRequestContext().addSiteRoot(resourcename);
    }

    /**
     * Adds a user to the Cms.
     * <p>
     * <b>Security:</b>
     * Only members of the group administrators are allowed to add a user.
     *
     * @param name the new name for the user.
     * @param password the new password for the user.
     * @param group the default groupname for the user.
     * @param description the description for the user.
     * @param additionalInfos a Hashtable with additional infos for the user. These
     * Infos may be stored into the Usertables (depending on the implementation).
     * @param flags the flags for a user (e.g. C_FLAG_ENABLED).
     *
     * @return a <code>CmsUser</code> object representing the added user.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsUser addUser(String name, String password, String group, String description, Hashtable additionalInfos, int flags) throws CmsException {
        return (m_driverManager.addUser(this, m_context, name, password, group, description, additionalInfos, flags));
    }

    /**
     * Adds a user to a group.
     * <p>
     * <b>Security:</b>
     * Only members of the group administrators are allowed to add a user to a group.
     *
     * @param username the name of the user that is to be added to the group.
     * @param groupname the name of the group.
     * @throws CmsException if operation was not successful.
     */
    public void addUserToGroup(String username, String groupname) throws CmsException {
        m_driverManager.addUserToGroup(m_context, username, groupname);
    }

    /**
     * Adds a web user to the Cms.
     * <br>
     * A web user has no access to the workplace but is able to access personalized
     * functions controlled by the OpenCms.
     *
     * @param name the new name for the user.
     * @param password the new password for the user.
     * @param group the default groupname for the user.
     * @param description the description for the user.
     * @param additionalInfos a Hashtable with additional infos for the user. These
     * Infos may be stored into the Usertables (depending on the implementation).
     * @param flags the flags for a user (e.g. C_FLAG_ENABLED)
     *
     * @return a <code>CmsUser</code> object representing the newly created user.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsUser addWebUser(String name, String password, String group, String description, Hashtable additionalInfos, int flags) throws CmsException {
        return (m_driverManager.addWebUser(this, m_context, name, password, group, description, additionalInfos, flags));
    }

    /**
     * Adds a web user to the Cms.
     * <br>
     * A web user has no access to the workplace but is able to access personalized
     * functions controlled by the OpenCms.
     *
     * @param name the new name for the user.
     * @param password the new password for the user.
     * @param group the default groupname for the user.
     * @param additionalGroup An additional group for the user.
     * @param description the description for the user.
     * @param additionalInfos a Hashtable with additional infos for the user. These
     * Infos may be stored into the Usertables (depending on the implementation).
     * @param flags the flags for a user (e.g. C_FLAG_ENABLED)
     *
     * @return a <code>CmsUser</code> object representing the newly created user.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsUser addWebUser(String name, String password, String group, String additionalGroup, String description, Hashtable additionalInfos, int flags) throws CmsException {
        CmsUser newWebUser = m_driverManager.addWebUser(this, m_context, name, password, group, additionalGroup, description, additionalInfos, flags);
        return newWebUser;
    }

    /**
     * Creates a backup of the published project
     *
     * @param project The project in which the resource was published.
     * @param projectresources The resources of the project
     * @param versionId The version of the backup
     * @param publishDate The date of publishing
     * @param userId The id of the user who had published the project
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public void backupProject(int projectId, int versionId, long publishDate) throws CmsException {
        CmsProject backupProject = m_driverManager.readProject(projectId);
        m_driverManager.backupProject(m_context, backupProject, versionId, publishDate, getRequestContext().currentUser());
    }

    /**
     * Changes the access control for a given resource and a given principal(user/group).
     * 
     * @param resourceName			name of the resource
     * @param principalType			the type of the principal (currently group or user)
     * @param principalName			name of the principal
     * @param allowedPermissions	bitset of allowed permissions
     * @param deniedPermissions		bitset of denied permissions
     * @param flags					flags
     * @throws CmsException			if something goes wrong
     */
    // TODO: find a better mechanism to select the principalType
    public void chacc(String resourceName, String principalType, String principalName, int allowedPermissions, int deniedPermissions, int flags) throws CmsException {
        CmsResource res = readFileHeader(resourceName);
        CmsAccessControlEntry acEntry = null;
        I_CmsPrincipal principal = null;

        if ("group".equals(principalType.toLowerCase())) {
            principal = readGroup(principalName);
            acEntry = new CmsAccessControlEntry(res.getResourceAceId(), principal.getId(), allowedPermissions, deniedPermissions, flags);
            acEntry.setFlags(I_CmsConstants.C_ACCESSFLAGS_GROUP);
        } else if ("user".equals(principalType.toLowerCase())) {
            principal = readUser(principalName);
            acEntry = new CmsAccessControlEntry(res.getResourceAceId(), principal.getId(), allowedPermissions, deniedPermissions, flags);
            acEntry.setFlags(I_CmsConstants.C_ACCESSFLAGS_USER);
        }

        m_driverManager.writeAccessControlEntry(m_context, res, acEntry);
    }

    /**
     * Changes the access control for a given resource and a given principal(user/group).
     * 
     * @param resourceName	name 	of the resource
     * @param principalType			the type of the principal (group or user)
     * @param principalName	name 	of the principal
     * @param permissionString		the permissions in the format ((+|-)(r|w|v|c|i))*
     * @throws CmsException			if something goes wrong
     */
    // TODO: find a better mechanism to select the principalType
    public void chacc(String resourceName, String principalType, String principalName, String permissionString) throws CmsException {
        CmsResource res = readFileHeader(resourceName);
        CmsAccessControlEntry acEntry = null;
        I_CmsPrincipal principal = null;

        if ("group".equals(principalType.toLowerCase())) {
            principal = readGroup(principalName);
            acEntry = new CmsAccessControlEntry(res.getResourceAceId(), principal.getId(), permissionString);
            acEntry.setFlags(I_CmsConstants.C_ACCESSFLAGS_GROUP);
        } else if ("user".equals(principalType.toLowerCase())) {
            principal = readUser(principalName);
            acEntry = new CmsAccessControlEntry(res.getResourceAceId(), principal.getId(), permissionString);
            acEntry.setFlags(I_CmsConstants.C_ACCESSFLAGS_USER);
        }

        m_driverManager.writeAccessControlEntry(m_context, res, acEntry);
    }

    /**
     * Changes the project-id of a resource to the new project
    	 * for publishing the resource directly.<p>
     *
    	 * @param projectId The new project-id
     * @param resourcename The name of the resource to change
    	 * @throws CmsException if something goes wrong
     */
    public void changeLockedInProject(int projectId, String resourcename) throws CmsException {
        // must include files marked as deleted when publishing
        getResourceType(readFileHeader(resourcename, true).getType()).changeLockedInProject(this, projectId, resourcename);
    }

    /**
     * Changes the type of the user
     *
     * @param userId The id of the user to change
     * @param userType The new type of the user
     */
    public void changeUserType(CmsUUID userId, int userType) throws CmsException {
        m_driverManager.changeUserType(m_context, userId, userType);
    }

    /**
     * Changes the type of the user to webusertype
     *
     * @param username The name of the user to change
     * @param userType The new type of the user
     */
    public void changeUserType(String username, int userType) throws CmsException {
        m_driverManager.changeUserType(m_context, username, userType);
    }

    /**
     * Does nothing.<p>
     *
     * @deprecated the VFS now uses ACL's instead of user/group/permissions
     */
    public void chgrp(String filename, String newGroup) throws CmsException {}

    /**
     * Does nothing.<p>
     *
     * @deprecated the VFS now uses ACL's instead of user/group/permissions
     */
    public void chgrp(String filename, String newGroup, boolean chRekursive) throws CmsException {}

    /**
     * Does nothing.<p>
     *
     * @deprecated the VFS now uses ACL's instead of user/group/permissions
     */
    public void chmod(String filename, int flags) throws CmsException {}

    /**
     * Does nothing.<p>
     *
     * @deprecated the VFS now uses ACL's instead of user/group/permissions
     */
    public void chmod(String filename, int flags, boolean chRekursive) throws CmsException {}

    /**
     * Does nothing.<p>
     *
     * @deprecated the VFS now uses ACL's instead of user/group/permissions
     */
    public void chown(String filename, String newOwner) throws CmsException {}

    /**
     * Does nothing.<p>
     *
     * @deprecated the VFS now uses ACL's instead of user/group/permissions
     */
    public void chown(String filename, String newOwner, boolean chRekursive) throws CmsException {}

    /**
     * Changes the resourcetype of a resource.
     * <br>
     * Only the resourcetype of a resource in an offline project can be changed. The state
     * of the resource is set to CHANGED (1).
     * If the content of this resource is not exisiting in the offline project already,
     * it is read from the online project and written into the offline project.
     * The user may change this, if he is admin of the resource.
     * <p>
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user is owner of the resource or is admin</li>
     * <li>the resource is locked by the callingUser</li>
     * </ul>
     *
     * @param filename the complete path to the resource.
     * @param newType the name of the new resourcetype for this resource.
     *
     * @throws CmsException if operation was not successful.
     */
    public void chtype(String filename, int newType) throws CmsException {
        getResourceType(readFileHeader(filename).getType()).chtype(this, filename, newType);
    }

    /**
     * Clears all internal DB-Caches.
     */
    public void clearcache() {
        m_driverManager.clearcache();
        System.gc();
    }

    /**
     * Copies a file.
     *
     * @param source the complete path of the sourcefile.
     * @param destination the complete path of the destinationfolder.
     *
     * @throws CmsException if the file couldn't be copied, or the user
     * has not the appropriate rights to copy the file.
     *
     * @deprecated Use copyResource instead.
     */
    public void copyFile(String source, String destination) throws CmsException {
        copyResource(source, destination, false, true, I_CmsConstants.C_COPY_PRESERVE_LINK);
    }

    /**
     * Copies a folder.
     *
     * @param source the complete path of the sourcefolder.
     * @param destination the complete path of the destinationfolder.
     *
     * @throws CmsException if the folder couldn't be copied, or if the
     * user has not the appropriate rights to copy the folder.
     *
     * @deprecated Use copyResource instead.
     */
    public void copyFolder(String source, String destination) throws CmsException {
        copyResource(source, destination, false, false, I_CmsConstants.C_COPY_PRESERVE_LINK);
    }

    /**
     * Copies a file.
     *
     * @param source the complete path of the sourcefile.
     * @param destination the complete path of the destinationfolder.
     *
     * @throws CmsException if the file couldn't be copied, or the user
     * has not the appropriate rights to copy the file.
     */
    public void copyResource(String source, String destination) throws CmsException {
        getResourceType(readFileHeader(source).getType()).copyResource(this, source, destination, false, true,I_CmsConstants.C_COPY_PRESERVE_LINK);
    }

    /**
     * Copies a file.
     *
     * @param source the complete path of the sourcefile.
     * @param destination the complete path of the destinationfolder.
     * @param keepFlags <code>true</code> if the copy should keep the source file's flags,
     *        <code>false</code> if the copy should get the user's default flags.
     * @param copyMode mode of the copy operation, described how to handle linked resourced during copy.
     * Possible values are: 
     * <ul>
     * <li>C_COPY_AS_NEW</li>
     * <li>C_COPY_AS_LINK</li>
     * <li>C_COPY_PRESERVE_LINK</li>
     * </ul>
     * @throws CmsException if the file couldn't be copied, or the user
     * has not the appropriate rights to copy the file.
     */
    public void copyResource(String source, String destination, boolean keepFlags, boolean lockCopy, int copyMode) throws CmsException {
        getResourceType(readFileHeader(source).getType()).copyResource(this, source, destination, keepFlags, lockCopy, copyMode);
    }

    /**
     * Copies a resource from the online project to a new, specified project.
     * <br>
     * Copying a resource will copy the file header or folder into the specified
     * offline project and set its state to UNCHANGED.
     *
     * @param resource the name of the resource.
         * @throws CmsException if operation was not successful.
     */
    public void copyResourceToProject(String resource) throws CmsException {
        getResourceType(readFileHeader(resource).getType()).copyResourceToProject(this, resource);
    }

    /**
     * Counts the locked resources in a project.
     *
     * @param id the id of the project
     * @return the number of locked resources in this project.
     *
     * @throws CmsException if operation was not successful.
     */
    public int countLockedResources(int id) throws CmsException {
        return m_driverManager.countLockedResources(m_context, id);
    }

    /**
     * @param sourceName	the name of the resource of which the access control entries are copied
     * @param destName		the name of the resource to which the access control entries are applied
     * @throws CmsException	if something goes wrong
     */
    public void cpacc(String sourceName, String destName) throws CmsException {
        CmsResource source = readFileHeader(sourceName);
        CmsResource dest = readFileHeader(destName);
        m_driverManager.copyAccessControlEntries(m_context, source, dest);
    }

    /**
     * Creates a new channel.
     *
     * @param parentChannel the complete path to the channel in which the new channel
     * will be created.
     * @param newChannelName the name of the new channel.
     *
     * @return folder a <code>CmsFolder</code> object representing the newly created channel.
     *
     * @throws CmsException if the channelname is not valid, or if the user has not the appropriate rights to create
     * a new channel.
     *
     */
    public CmsFolder createChannel(String parentChannel, String newChannelName) throws CmsException {
        getRequestContext().saveSiteRoot();
        try {
            setContextToCos();
            Hashtable properties = new Hashtable();
            int newChannelId = org.opencms.db.CmsIdGenerator.nextId(I_CmsConstants.C_TABLE_CHANNELID);
            properties.put(I_CmsConstants.C_PROPERTY_CHANNELID, newChannelId + "");
            return (CmsFolder)createResource(parentChannel, newChannelName, CmsResourceTypeFolder.C_RESOURCE_TYPE_ID, properties);
        } finally {
            getRequestContext().restoreSiteRoot();
        }
    }

    /**
     * Creates a new file with the given content and resourcetype.<br>
     *
     * @param folder the complete path to the folder in which the file will be created.
     * @param filename the name of the new file.
     * @param contents the contents of the new file.
     * @param type the resourcetype of the new file.
     *
     * @return file a <code>CmsFile</code> object representing the newly created file.
     *
     * @throws if the resourcetype is set to folder. The CmsException is also thrown, if the
     * filename is not valid or if the user has not the appropriate rights to create a new file.
     *
     * @deprecated Use createResource instead.
     */
    public CmsFile createFile(String folder, String filename, byte[] contents, int type) throws CmsException {
        return (CmsFile)createResource(folder, filename, type, null, contents);
    }
    /**
     * Creates a new file with the given content and resourcetype.
     *
     * @param folder the complete path to the folder in which the file will be created.
     * @param filename the name of the new file.
     * @param contents the contents of the new file.
     * @param type the resourcetype of the new file.
     * @param properties A Hashtable of properties, that should be set for this file.
     * The keys for this Hashtable are the names for properties, the values are
     * the values for the properties.
     *
     * @return file a <code>CmsFile</code> object representing the newly created file.
     *
     * @throws CmsException or if the resourcetype is set to folder.
     * The CmsException is also thrown, if the filename is not valid or if the user
     * has not the appropriate rights to create a new file.
     *
     * @deprecated Use createResource instead.
     */
    public CmsFile createFile(String folder, String filename, byte[] contents, int type, Hashtable properties) throws CmsException {
        return (CmsFile)createResource(folder, filename, type, properties, contents);
    }

    /**
     * Creates a new folder.
     *
     * @param folder the complete path to the folder in which the new folder
     * will be created.
     * @param newFolderName the name of the new folder.
     *
     * @return folder a <code>CmsFolder</code> object representing the newly created folder.
     *
     * @throws CmsException if the foldername is not valid, or if the user has not the appropriate rights to create
     * a new folder.
     *
     * @deprecated Use createResource instead.
     */
    public CmsFolder createFolder(String folder, String newFolderName) throws CmsException {
        return (CmsFolder)createResource(folder, newFolderName, CmsResourceTypeFolder.C_RESOURCE_TYPE_ID);
    }
    /**
     * Adds a new group to the Cms.<p>
     * 
     * <b>Security:</b>
     * Only members of the group administrators are allowed to add a new group.
     *
     * @param name the name of the new group
     * @param description the description of the new group
     * @param flags the flags for the new group
     * @param parent the parent group
     *
     * @return a <code>CmsGroup</code> object representing the newly created group.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsGroup createGroup(String name, String description, int flags, String parent) throws CmsException {
        return (m_driverManager.createGroup(m_context, name, description, flags, parent));
    }

    /**
     * Adds a new group to the Cms.<p>
     * 
     * <b>Security:</b>
     * Only members of the group administrators are allowed to add a new group.
     * 
     * @param id			the id of the group
     * @param name			the name of the new group
     * @param description	the description of the new group
     * @param flags			the flags for the new group
     * @param parent		the parent group
     * @return				a <code>CmsGroup</code> object representing the newly created group.
     * @throws CmsException if something goes wrong
     */
    public CmsGroup createGroup(String id, String name, String description, int flags, String parent) throws CmsException {
        return m_driverManager.createGroup(m_context, id, name, description, flags, parent);
    }

    /**
     * creates a link entry for each of the link targets in the linktable.
     *
     * @param pageId The resourceId (offline) of the page whose liks should be traced.
     * @param linkTarget A vector of strings (the linkdestinations).
     */
    public void createLinkEntrys(CmsUUID pageId, Vector linkTargets) throws CmsException {
        m_driverManager.createLinkEntrys(pageId, linkTargets);
    }
    /**
      * Creates a new project for task handling.
      *
      * @param projectname the name of the project
      * @param projectType the type of the Project
      * @param role a Usergroup for the project
      * @param timeout the time when the Project must finished
      * @param priority  a Priority for the Project
      *
      * @return a <code>CmsTask</code> object representing the newly created task.
      *
      * @throws CmsException if operation was not successful.
      */
    public CmsTask createProject(String projectname, int projectType, String roleName, long timeout, int priority) throws CmsException {
        return m_driverManager.createProject(m_context, projectname, projectType, roleName, timeout, priority);
    }

    /**
     * Creates a new project.
     *
     * @param name the name of the project to create
     * @param description the description for the new project
     * @param groupname the name of the project user group
     * @param managergroupname the name of the project manager group
     * @return the created project
     * @throws CmsException if operation was not successful.
     */
    public CmsProject createProject(String name, String description, String groupname, String managergroupname) throws CmsException {
        CmsProject newProject = m_driverManager.createProject(m_context, name, description, groupname, managergroupname, I_CmsConstants.C_PROJECT_TYPE_NORMAL);
        return (newProject);
    }

    /**
     * Creates a new project.
     *
     * @param name the name of the project to create
     * @param description the description for the new project
     * @param groupname the name of the project user group
     * @param managergroupname the name of the project manager group
     * @param projecttype the type of the project (normal or temporary)
     * @return the created project
     * @throws CmsException if operation was not successful.
     */
    public CmsProject createProject(String name, String description, String groupname, String managergroupname, int projecttype) throws CmsException {
        CmsProject newProject = m_driverManager.createProject(m_context, name, description, groupname, managergroupname, projecttype);
        return (newProject);
    }

    /**
     * Creates the property-definition for a resource type.
     *
     * @param name the name of the property-definition to overwrite.
     * @param resourcetype the name of the resource-type for the property-definition.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsPropertydefinition createPropertydefinition(String name, int resourcetype) throws CmsException {
        return (m_driverManager.createPropertydefinition(m_context, name, resourcetype));
    }

    public CmsResource createResource(String newResourceName, int type, Map properties, byte[] contents, Object parameter) throws CmsException {
        return getResourceType(type).createResource(this, newResourceName, properties, contents, parameter);
    }

    public CmsResource createResource(String folder, String name, int type) throws CmsException {
        return createResource(folder + name, type, new HashMap(), new byte[0], null);
    }

    public CmsResource createResource(String folder, String name, int type, Map properties) throws CmsException {
        return createResource(folder + name, type, properties, new byte[0], null);
    }

    public CmsResource createResource(String folder, String name, int type, Map properties, byte[] contents) throws CmsException {
        return createResource(folder + name, type, properties, contents, null);
    }

    /**
      * Creates a new task.
      * <p>
      * <B>Security:</B>
      * All users can create a new task.
      *
      * @param projectid the Id of the current project task of the user.
      * @param agentname the User who will edit the task.
      * @param rolename a Usergroup for the task.
      * @param taskname a Name of the task.
      * @param tasktype the type of the task.
      * @param taskcomment a description of the task.
      * @param timeout the time when the task must finished.
      * @param priority the Id for the priority of the task.
      *
      * @return a <code>CmsTask</code> object representing the newly created task.
      *
      * @throws CmsException Throws CmsException if something goes wrong.
      */
    public CmsTask createTask(int projectid, String agentName, String roleName, String taskname, String taskcomment, int tasktype, long timeout, int priority) throws CmsException {
        return m_driverManager.createTask(m_context.currentUser(), projectid, agentName, roleName, taskname, taskcomment, tasktype, timeout, priority);
    }

    /**
      * Creates a new task.
      * <p>
      * <B>Security:</B>
      * All users can create a new task.
      * @param agent the User who will edit the task.
      * @param role a Usergroup for the task.
      * @param taskname the name of the task.
      * @param taskcomment a description of the task.
      * @param timeout the time when the task must finished.
      * @param priority the Id for the priority of the task.
      *
      * @return a <code>CmsTask</code> object representing the newly created task.
      *
      * @throws CmsException if operation was not successful.
      */
    public CmsTask createTask(String agentName, String roleName, String taskname, String taskcomment, long timeout, int priority) throws CmsException {
        return (m_driverManager.createTask(m_context, agentName, roleName, taskname, taskcomment, timeout, priority));
    }

    /**
     * Creates a new project for the temporary files.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsProject createTempfileProject() throws CmsException {
        CmsProject newProject = m_driverManager.createTempfileProject(this, m_context);
        return (newProject);
    }

    public CmsResource createVfsLink(String linkName, String targetName, Map linkProperties) throws CmsException {
        return m_driverManager.createVfsLink(m_context, addSiteRoot(linkName), addSiteRoot(targetName), linkProperties, true);
    }
    /**
     * Deletes all properties for a file or folder.
     *
     * @param resourcename the name of the resource for which all properties should be deleted.
     *
     * @throws CmsException if operation was not successful.
     */
    public void deleteAllProperties(String resourcename) throws CmsException {
        m_driverManager.deleteAllProperties(m_context, addSiteRoot(resourcename));
    }

    /**
     * Deletes the versions from the backup tables that are older then the given weeks
     * 
     * @param weeks The number of weeks: the max age of the remaining versions
     * @return int The oldest remaining version
     */
    public int deleteBackups(int weeks) throws CmsException {
        return m_driverManager.deleteBackups(this, m_context, weeks);
    }

    /**
     * Deletes a folder.
     * <br>
     * This is a very complex operation, because all sub-resources may be
     * deleted too.
     *
     * @param foldername the complete path of the folder.
     *
     * @throws CmsException if the folder couldn't be deleted, or if the user
     * has not the rights to delete this folder.
     *
     */
    public void deleteEmptyFolder(String foldername) throws CmsException {
        m_driverManager.deleteFolder(m_context, addSiteRoot(foldername));
    }

    /**
     * Deletes a file.
     *
     * @param filename the complete path of the file.
     *
     * @throws CmsException if the file couldn't be deleted, or if the user
     * has not the appropriate rights to delete the file.
     *
     * @deprecated Use deleteResource instead.
     */
    public void deleteFile(String filename) throws CmsException {
        deleteResource(filename, I_CmsConstants.C_DELETE_OPTION_IGNORE_VFS_LINKS);
    }

    /**
     * Deletes a folder.
     * <br>
     * This is a very complex operation, because all sub-resources may be
     * deleted too.
     *
     * @param foldername the complete path of the folder.
     *
     * @throws CmsException if the folder couldn't be deleted, or if the user
     * has not the rights to delete this folder.
     *
     * @deprecated Use deleteResource instead.
     */
    public void deleteFolder(String foldername) throws CmsException {
        deleteResource(foldername, I_CmsConstants.C_DELETE_OPTION_IGNORE_VFS_LINKS);
    }

    /**
     * Deletes a group.
     * <p>
     * <b>Security:</b>
     * Only the admin user is allowed to delete a group.
     *
     * @param delgroup the name of the group.
     * @throws CmsException  if operation was not successful.
     */
    public void deleteGroup(String delgroup) throws CmsException {
        m_driverManager.deleteGroup(m_context, delgroup);
    }

    /****************     methods for link management            ****************************/

    /**
     * deletes all entrys in the link table that belong to the pageId
     *
     * @param pageId The resourceId (offline) of the page whose links should be deleted
     */
    public void deleteLinkEntrys(CmsUUID pageId) throws CmsException {
        m_driverManager.deleteLinkEntrys(pageId);
    }

    /**
     * Deletes a project.
     *
     * @param id the id of the project.
     *
     * @throws CmsException if operation was not successful.
     */
    public void deleteProject(int id) throws CmsException {
        m_driverManager.deleteProject(m_context, id);
    }

    /**
     * Deletes a property for a file or folder.
     *
     * @param resourcename the name of a resource for which the property should be deleted.
     * @param property the name of the property.
     *
     * @throws CmsException Throws if operation was not successful.
     */
    public void deleteProperty(String resourcename, String property) throws CmsException {
        m_driverManager.deleteProperty(m_context, addSiteRoot(resourcename), property);
    }

    /**
     * Deletes the property-definition for a resource type.
     *
     * @param name the name of the property-definition to delete.
     * @param resourcetype the name of the resource-type for the property-definition.
     *
     * @throws CmsException if operation was not successful.
     */
    public void deletePropertydefinition(String name, int resourcetype) throws CmsException {
        m_driverManager.deletePropertydefinition(m_context, name, resourcetype);
    }

    /**
     * Deletes a resource.<p>
     *
     * @param filename the filename of the resource exlucing the site root
     * @param deleteOption signals how VFS links pointing to this resource should be handled 
     * @throws CmsException if the user has insufficient acces right to delete the resource
     * @see com.opencms.core.I_CmsConstants#C_DELETE_OPTION_DELETE_VFS_LINKS
     * @see com.opencms.core.I_CmsConstants#C_DELETE_OPTION_IGNORE_VFS_LINKS
     * @see com.opencms.core.I_CmsConstants#C_DELETE_OPTION_PRESERVE_VFS_LINKS
     */
    public void deleteResource(String filename, int deleteOption) throws CmsException {
        getResourceType(readFileHeader(filename).getType()).deleteResource(this, filename, deleteOption);
    }
    /**
     * Deletes a user from the Cms.
     * <p>
     * <b>Security:</b>
     * Only a admin user is allowed to delete a user.
     *
     * @param name the Id of the user to be deleted.
     *
     * @throws CmsException if operation was not successful.
     */
    public void deleteUser(CmsUUID userId) throws CmsException {
        m_driverManager.deleteUser(m_context, userId);
    }

    /**
     * Deletes a user from the Cms.
     * <p>
     * <b>Security:</b>
     * Only a admin user is allowed to delete a user.
     *
     * @param name the name of the user to be deleted.
     *
     * @throws CmsException if operation was not successful.
     */
    public void deleteUser(String username) throws CmsException {
        m_driverManager.deleteUser(m_context, username);
    }

    /**
     * Deletes a web user from the Cms.
     *
     * @param name the id of the user to be deleted.
     *
     * @throws CmsException if operation was not successful.
     */
    public void deleteWebUser(CmsUUID userId) throws CmsException {
        m_driverManager.deleteWebUser(m_context, userId);
    }

    /**
     * Method to encrypt the passwords.
     *
     * @param value The value to encrypt.
     * @return The encrypted value.
     */
    public String digest(String value) {
        return m_driverManager.digest(value);
    }

    /**
     * Changes the project-id of a resource to the new project
    	 * for publishing the resource directly.<p>
     *
    	 * @param projectId The new project-id
     * @param resourcename The name of the resource to change
    	 * @throws CmsException if something goes wrong
     */
    protected void doChangeLockedInProject(int projectId, String resourcename) throws CmsException {
        m_driverManager.changeLockedInProject(m_context, projectId, addSiteRoot(resourcename));
    }

    /**
     * Changes the resourcetype of a resource.
     * <br>
     * Only the resourcetype of a resource in an offline project can be changed. The state
     * of the resource is set to CHANGED (1).
     * If the content of this resource is not exisiting in the offline project already,
     * it is read from the online project and written into the offline project.
     * The user may change this, if he is admin of the resource.
     * <p>
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user is owner of the resource or is admin</li>
     * <li>the resource is locked by the callingUser</li>
     * </ul>
     *
     * @param filename the complete path to the resource.
     * @param newType the name of the new resourcetype for this resource.
     *
     * @throws CmsException if operation was not successful.
     */
    protected void doChtype(String filename, int newType) throws CmsException {
        m_driverManager.chtype(m_context, addSiteRoot(filename), newType);
    }

    /**
     * Copies a file.
     *
     * @param source the complete path of the sourcefile.
     * @param destination the complete path of the destinationfolder.
     * @param lockCopy flag to lock the copied resource
     * @param copyMode mode of the copy operation, described how to handle linked resourced during copy.
     * Possible values are: 
     * <ul>
     * <li>C_COPY_AS_NEW</li>
     * <li>C_COPY_AS_LINK</li>
     * <li>C_COPY_PRESERVE_LINK</li>
     * </ul>
     * @throws CmsException if the file couldn't be copied, or the user
     * has not the appropriate rights to copy the file.
     */
    protected void doCopyFile(String source, String destination, boolean lockCopy, int copyMode) throws CmsException {
        m_driverManager.copyFile(m_context, addSiteRoot(source), addSiteRoot(destination), lockCopy, false, copyMode);
    }

    /**
     * Copies a folder.
     *
     * @param source the complete path of the sourcefolder.
     * @param destination the complete path of the destinationfolder.
     * @param preserveTimestamps true if the timestamps and users of the folder should be kept
     * @throws CmsException if the folder couldn't be copied, or if the
     * user has not the appropriate rights to copy the folder.
     */
    protected void doCopyFolder(String source, String destination, boolean lockCopy, boolean preserveTimestamps) throws CmsException {
        m_driverManager.copyFolder(m_context, addSiteRoot(source), addSiteRoot(destination), lockCopy, false, preserveTimestamps);
    }

    /**
     * Copies a resource from the online project to a new, specified project.
     * <br>
     * Copying a resource will copy the file header or folder into the specified
     * offline project and set its state to UNCHANGED.
     *
     * @param resource the name of the resource.
         * @throws CmsException if operation was not successful.
     */
    protected void doCopyResourceToProject(String resource) throws CmsException {
        m_driverManager.copyResourceToProject(m_context, addSiteRoot(resource));
    }

    /**
     * Creates a new file with the given content and resourcetype.<br>
     *
     * @param folder the complete path to the folder in which the file will be created.
     * @param filename the name of the new file.
     * @param contents the contents of the new file.
     * @param type the resourcetype of the new file.
     *
     * @return file a <code>CmsFile</code> object representing the newly created file.
     *
     * @throws CmsException if the resourcetype is set to folder. The CmsException is also thrown, if the
     * filename is not valid or if the user has not the appropriate rights to create a new file.
     */
    protected CmsFile doCreateFile(String newFileName, byte[] contents, String type) throws CmsException {
        CmsFile file = m_driverManager.createFile(m_context, addSiteRoot(newFileName), contents, type, new HashMap());
        return file;
    }

    /**
     * Creates a new file with the given content and resourcetype.
     *
     * @param folder the complete path to the folder in which the file will be created.
     * @param filename the name of the new file.
     * @param contents the contents of the new file.
     * @param type the resourcetype of the new file.
     * @param properties A Hashtable of properties, that should be set for this file.
     * The keys for this Hashtable are the names for properties, the values are
     * the values for the properties.
     *
     * @return file a <code>CmsFile</code> object representing the newly created file.
     *
     * @throws CmsException if the wrong properties are given, or if the resourcetype is set to folder.
     * The CmsException is also thrown, if the filename is not valid or if the user
     * has not the appropriate rights to create a new file.
     */
    protected CmsFile doCreateFile(String newFileName, byte[] contents, String type, Map properties) throws CmsException {
        // avoid null-pointer exceptions
        if (properties == null) {
            properties = new Hashtable();
        }
        CmsFile file = m_driverManager.createFile(m_context, addSiteRoot(newFileName), contents, type, properties);
        return file;
    }

    /**
     * Creates a new folder.
     *
     * @param folder the complete path to the folder in which the new folder will
     * be created.
     * @param newFolderName the name of the new folder.
     * @param properties A Hashtable of properties, that should be set for this folder.
     * The keys for this Hashtable are the names for property-definitions, the values are
     * the values for the properties.
     *
     * @return a <code>CmsFolder</code> object representing the newly created folder.
     * @throws CmsException if the foldername is not valid, or if the user has not the appropriate rights to create
     * a new folder.
     *
     */
    protected CmsFolder doCreateFolder(String newFolderName, Map properties) throws CmsException {
        CmsFolder cmsFolder = m_driverManager.createFolder(m_context, addSiteRoot(newFolderName), properties);
        return cmsFolder;
    }

    /**
     * Creates a new folder.
     *
     * @param folder the complete path to the folder in which the new folder
     * will be created.
     * @param newFolderName the name of the new folder.
     *
     * @return folder a <code>CmsFolder</code> object representing the newly created folder.
     *
     * @throws CmsException if the foldername is not valid, or if the user has not the appropriate rights to create
     * a new folder.
     */
    protected CmsFolder doCreateFolder(String folder, String newFolderName) throws CmsException {
        CmsFolder cmsFolder = m_driverManager.createFolder(m_context, addSiteRoot(folder + newFolderName + I_CmsConstants.C_FOLDER_SEPARATOR), new Hashtable());
        return cmsFolder;
    }

    /**
     * Decrement the VFS link counter for a resource. 
     * The link counter is saved in the RESOURCE_FLAGS table attribute.
     * 
     * @param theResourceName the name of the resource for which the link count is decremented
     * @return the current link count of the specified resource
     * @throws CmsException
     */
    public int doDecrementLinkCountForResource(String theResourceName) throws CmsException {
        //System.err.println( this.getClass().getName() + " decrementing link count of: " + theResourceName );

        // TODO: the following code requires a change of the database schema first!       
        //return m_driverManager.decrementLinkCountForResource( m_context.currentProject(), this.getSiteRoot(theResourceName) );

        return 0;
    }

    /**
     * Deletes a file.
     *
     * @param filename the complete path of the file.
     *
     * @throws CmsException if the file couldn't be deleted, or if the user
     * has not the appropriate rights to delete the file.
     */
    protected void doDeleteFile(String filename, int deleteOption) throws CmsException {
        m_driverManager.deleteFile(m_context, addSiteRoot(filename), deleteOption);
    }

    /**
     * Deletes a folder.
     * <br>
     * This is a very complex operation, because all sub-resources may be
     * deleted too.
     *
     * @param foldername the complete path of the folder.
     *
     * @throws CmsException if the folder couldn't be deleted, or if the user
     * has not the rights to delete this folder.
     */
    protected void doDeleteFolder(String foldername) throws CmsException {
        m_driverManager.deleteFolder(m_context, addSiteRoot(foldername));
    }

    /**
     * Creates a new resource.<p>
     *
     * @param newResourceName the name of the new resource
     * @param uuid  the structure uuid of the resource
     * @param uuidfile  the file uuid of the resource
     * @param uuidresource  the resource uuid of the resource
     * @param resourceType The resourcetype of the new resource
     * @param properties A Hashtable of propertyinfos, that should be set for this folder
     * The keys for this Hashtable are the names for propertydefinitions, the values are
     * the values for the propertyinfos
     * @param accessFlags The accessFlags of the new resource
     * @param lastmodified the last modification date of the resource
     * @param filecontent The content of the resource if it is of type file 
     *
     * @return a <code>CmsFolder</code> object representing the newly created folder
     * @throws CmsException if the resourcename is not valid, or if the user has not the appropriate rights to create
     * a new resource
     *
     */
    protected CmsResource doImportResource(CmsResource resource, byte content[], Map properties, String destination)
        throws CmsException
    {
        CmsResource cmsResource = m_driverManager.importResource(m_context, addSiteRoot(destination), resource, content, properties);
        return cmsResource;
    }
    /**
     * Increment the VFS link counter for a resource. 
     * The link counter is saved in the RESOURCE_FLAGS table attribute.
     * 
     * @param theResourceName the name of the resource for which the link count is incremented
     * @return the current link count of the specified resource
     * @throws CmsException
     */
    public int doIncrementLinkCountForResource(String theResourceName) throws CmsException {
        //System.err.println( this.getClass().getName() + " incrementing link count of: " + theResourceName );

        // TODO: the following code requires a change of the database schema first!              
        //return m_driverManager.incrementLinkCountForResource( m_context.currentProject(), this.getSiteRoot(theResourceName) );

        return 0;
    }

    /**
     * Locks a given resource.
     * <br>
     * A user can lock a resource, so he is the only one who can write this
     * resource.
     *
     * @param resource the complete path to the resource to lock.
     * @param force if force is <code>true</code>, a existing locking will be overwritten.
     *
     * @throws CmsException if the user has not the rights to lock this resource.
     * It will also be thrown, if there is a existing lock and force was set to false.
     */
    protected void doLockResource(String resource, boolean force) throws CmsException {
        m_driverManager.lockResource(m_context, addSiteRoot(resource));
    }

    /**
     * Moves a file to the given destination.
     *
     * @param source the complete path of the sourcefile.
     * @param destination the complete path of the destinationfile.
     *
     * @throws CmsException if the user has not the rights to move this resource,
     * or if the file couldn't be moved.
     */
    protected void doMoveResource(String source, String destination) throws CmsException {
        m_driverManager.moveResource(m_context, addSiteRoot(source), addSiteRoot(destination));
    }

    /**
    * Moves a resource to the lost and found folder
    *
    * @param resourcename the complete path of the sourcefile
    * @param copyResource true, if the resource should be copied to its destination inside the lost+found folder
    * @return location of the moved resource
    * @throws CmsException if the user has not the rights to move this resource,
    * or if the file couldn't be moved.
    */
    protected String doCopyToLostAndFound(String resourcename, boolean copyResource) throws CmsException {
        return m_driverManager.copyToLostAndFound(m_context, addSiteRoot(resourcename), copyResource);
    }
       


    /**
     * Renames the resource to the new name.
     *
     * @param oldname the complete path to the file which will be renamed.
     * @param newname the new name of the file.
     *
     * @throws CmsException if the user has not the rights
     * to rename the file, or if the file couldn't be renamed.
     */
    protected void doRenameResource(String oldname, String newname) throws CmsException {
        m_driverManager.renameResource(m_context, addSiteRoot(oldname), newname);
    }

    protected CmsResource doReplaceResource(String resName, byte[] newResContent, int newResType, Map newResProps) throws CmsException {
        CmsResource res = null;

        res = m_driverManager.replaceResource(m_context, addSiteRoot(resName), newResType, newResProps, newResContent);
        return res;
    }

    /**
     * Restores a file in the current project with a version in the backup
     *
     * @param versionId The version id of the resource
     * @param filename The name of the file to restore
     *
     * @throws CmsException  Throws CmsException if operation was not succesful.
     */
    protected void doRestoreResource(int versionId, String filename) throws CmsException {
        m_driverManager.restoreResource(m_context, versionId, addSiteRoot(filename));
    }

    /**
     * Access the driver manager underneath to change the timestamp of a resource.
     * 
     * @param resourceName the name of the resource to change
     * @param timestamp timestamp the new timestamp of the changed resource
     * @param user the user who is inserted as userladtmodified 
     */
    protected void doTouch(String resourceName, long timestamp, CmsUUID user) throws CmsException {
        m_driverManager.touch(m_context, addSiteRoot(resourceName), timestamp, user);
    }

    /**
     * Undeletes a file.
     *
     * @param filename the complete path of the file.
     *
     * @throws CmsException if the file couldn't be undeleted, or if the user
     * has not the appropriate rights to undelete the file.
     */
    protected void doUndeleteFile(String filename) throws CmsException {
        m_driverManager.undeleteResource(m_context, addSiteRoot(filename));
    }

    /**
     * Undeletes a folder.
     * <br>
     * This is a very complex operation, because all sub-resources may be
     * undeleted too.
     *
     * @param foldername the complete path of the folder.
     *
     * @throws CmsException if the folder couldn't be undeleted, or if the user
     * has not the rights to undelete this folder.
     */
    protected void doUndeleteFolder(String foldername) throws CmsException {
        m_driverManager.undeleteResource(m_context, addSiteRoot(foldername));
    }

    /**
     * Undo changes in a file.
     * <br>
     *
     * @param resource the complete path to the resource to be unlocked.
     *
     * @throws CmsException if the user has not the rights
     * to write this resource.
     */
    protected void doUndoChanges(String resource) throws CmsException {
        m_driverManager.undoChanges(m_context, addSiteRoot(resource));
    }

    /**
     * Unlocks a resource.
     * <br>
     * A user can unlock a resource, so other users may lock this file.
     *
     * @param resource the complete path to the resource to be unlocked.
     *
     * @throws CmsException if the user has not the rights
     * to unlock this resource.
     */
    protected void doUnlockResource(String resource) throws CmsException {
        m_driverManager.unlockResource(m_context, addSiteRoot(resource));
    }

    /**
    * Writes a resource and its properties to the Cms.<br>
    *
    * @param resourcename The name of the resource to write.
    * @param properties The properties of the resource.
    * @param username The name of the new owner of the resource
    * @param resourceType The new type of the resource
    * @param filecontent The new filecontent of the resource
    *
    * @throws CmsException  Throws CmsException if operation was not succesful.
    */
    protected void doWriteResource(String resourcename, Map properties, String username, int resourceType, byte[] filecontent) throws CmsException {
        m_driverManager.writeResource(m_context, addSiteRoot(resourcename), properties, username, resourceType, filecontent);
    }

    /**
     * Ends a task of the Cms.
     *
     * @param taskid the ID of the task to end.
     *
     * @throws CmsException if operation was not successful.
     */
    public void endTask(int taskid) throws CmsException {
        m_driverManager.endTask(m_context, taskid);
    }


    /**
     * Tests if a resource with the given resourceId does already exist in the Database.<p>
     * 
     * @param resourceId the resource id to test for
     * @return true if a resource with the given id was found, false otherweise
     * @throws CmsException if something goes wrong
     */
      public boolean existsResourceId (CmsUUID resourceId) throws CmsException {
          return m_driverManager.existsResourceId(m_context, resourceId);          
      }


    /**
     * Exports channels and moduledata to zip.
     *
     * <B>Security:</B>
     * only Administrators can do this;
     *
     * @param currentUser user who requestd themethod
     * @param currentProject current project of the user
     * @param exportFile the name (absolute Path) of the export resource (zip)
     * @param exportChannels the names (absolute Path) of channels from which should be exported
     * @param exportModules the names of modules from which should be exported
     * @param cms the cms-object to use for the export.
     *
     * @throws Throws CmsException if something goes wrong.
     */
    public void exportModuledata(String exportFile, String[] exportChannels, String[] exportModules, I_CmsReport report) throws CmsException {
        m_driverManager.exportModuledata(this, m_context, exportFile, exportChannels, exportModules, report);
    }

    /**
     * Exports a resource.
     */
    public CmsFile exportResource(CmsFile file) throws CmsException {
        return getResourceType(file.getType()).exportResource(this, file);
    }

    /**
     * Exports cms-resources to a zip-file.
     *
     * @param exportFile the name (absolute Path) of the export resource (zip-file).
     * @param exportPath the name (absolute Path) of folder from which should be exported.
     *
     * @throws CmsException if operation was not successful.
     */
    public void exportResources(String exportFile, String[] exportPaths) throws CmsException {
        // export the resources
        m_driverManager.exportResources(this, m_context, exportFile, exportPaths);
    }

    /**
     * Exports cms-resources to a zip-file.
     *
     * @param exportFile the name (absolute Path) of the export resource (zip-file).
     * @param exportPath the name (absolute Path) of folder from which should be exported.
     * @param includeSystem indicates if the system resources will be included in the export.
     * @param excludeUnchanged <code>true</code>, if unchanged files should be excluded.
     *
     * @throws CmsException if operation was not successful.
     */
    public void exportResources(String exportFile, String[] exportPaths, boolean includeSystem, boolean excludeUnchanged) throws CmsException {
        // export the resources
        m_driverManager.exportResources(this, m_context, exportFile, exportPaths, includeSystem, excludeUnchanged);
    }

    /**
     * Exports cms-resources to a zip-file.
     *
     * @param exportFile the name (absolute Path) of the export resource (zip-file).
     * @param exportPath the name (absolute Path) of folder from which should be exported.
     * @param includeSystem indicates if the system resources will be included in the export.
     * @param excludeUnchanged <code>true</code>, if unchanged files should be excluded.
     *
     * @throws CmsException if operation was not successful.
     */
    public void exportResources(String exportFile, String[] exportPaths, boolean includeSystem, boolean excludeUnchanged, boolean exportUserdata) throws CmsException {
        // call the export with the standard report object.
        exportResources(exportFile, exportPaths, includeSystem, excludeUnchanged, exportUserdata, 0, new CmsShellReport());
    }

    /**
     * Exports cms-resources to a zip-file.
     *
     * @param exportFile the name (absolute Path) of the export resource (zip-file).
     * @param exportPath the name (absolute Path) of folder from which should be exported.
     * @param includeSystem indicates if the system resources will be included in the export.
     * @param excludeUnchanged <code>true</code>, if unchanged files should be excluded.
     * @param report the cmsReport to handle the log messages.
     *
     * @throws CmsException if operation was not successful.
     */
    public void exportResources(String exportFile, String[] exportPaths, boolean includeSystem, boolean excludeUnchanged, boolean exportUserdata, long contentAge, I_CmsReport report) throws CmsException {
        // export the resources
        m_driverManager.exportResources(this, m_context, exportFile, exportPaths, includeSystem, excludeUnchanged, exportUserdata, contentAge, report);
    }

    /**
     * Gets all hard and soft links pointing to a specified resource.<p>
     * 
     * @param resourcename the name of the resource
     * @return a List with CmsResources
     * @throws CmsException if something goes wrong
     */
    public List getAllVfsLinks(String resourcename) throws CmsException {       
        return m_driverManager.getAllVfsLinks( m_context, addSiteRoot(resourcename) );
    }

    /**
     * Gets all soft links pointing to a specified resource, excluding the
     * resource itself if it is a soft link and its hard link.<p>
     * 
     * @param resourcename the name of the resource
     * @return a List with CmsResources
     * @throws CmsException if something goes wrong
     */
    public List getAllVfsSoftLinks(String resourcename) throws CmsException {       
        return m_driverManager.getAllVfsSoftLinks( m_context, addSiteRoot(resourcename) );
    }    

    /**
     * Fires a CmsEvent
     *
     * @param type The type of the event
     * @param data A data object that contains data used by the event listeners
     */
    private void fireEvent(int type, Object data) {
        OpenCms.fireCmsEvent(this, type, Collections.singletonMap("data", data));
    }

    /**
     * Forwards a task to a new user.
     *
     * @param taskid the id of the task which will be forwarded.
     * @param newRole the new group for the task.
     * @param newUser the new user who gets the task.
     *
     * @throws CmsException if operation was not successful.
     */
    public void forwardTask(int taskid, String newRoleName, String newUserName) throws CmsException {
        m_driverManager.forwardTask(m_context, taskid, newRoleName, newUserName);
    }

    /**
     * Returns the vector of access control entries of a resource.
     * 
     * @param resourceName	the name of the resource.
     * @return				a vector of access control entries
     * @throws CmsException	if something goes wrong
     */
    public Vector getAccessControlEntries(String resourceName) throws CmsException {
        return getAccessControlEntries(resourceName, true);
    }

    /**
     * Returns the vector of access control entries of a resource.
     * 
     * @param resourceName	the name of the resource.
     * @param getInherited	true, if inherited access control entries should be returned, too
     * @return				a vector of access control entries
     * @throws CmsException	if something goes wrong
     */
    public Vector getAccessControlEntries(String resourceName, boolean getInherited) throws CmsException {
        CmsResource res = readFileHeader(resourceName);
        return m_driverManager.getAccessControlEntries(m_context, res, getInherited);
    }

    /**
     * Returns the access control list (summarized access control entries) of a given resource.
     * 
     * @param resourceName	the name of the resource
     * @return				the access control list of the resource
     * @throws CmsException	if something goes wrong
     */
    public CmsAccessControlList getAccessControlList(String resourceName) throws CmsException {
        return getAccessControlList(resourceName, false);
    }

    /**
     * Returns the access control list (summarized access control entries) of a given resource.
     * 
     * @param resourceName	the name of the resource
     * @param inheritedOnly	if set, the non-inherited entries are skipped
     * @return				the access control list of the resource
     * @throws CmsException	if something goes wrong
     */
    public CmsAccessControlList getAccessControlList(String resourceName, boolean inheritedOnly) throws CmsException {
        CmsResource res = readFileHeader(resourceName);
        return m_driverManager.getAccessControlList(m_context, res, inheritedOnly);
    }

    /**
     * Returns all projects, which the current user can access.
     *
     * @return a Vector of objects of type <code>CmsProject</code>.
     *
     * @throws CmsException if operation was not successful.
     */
    public Vector getAllAccessibleProjects() throws CmsException {
        return (m_driverManager.getAllAccessibleProjects(m_context));
    }

    /**
    * Returns a Vector with all projects from history
    *
    * @return Vector with all projects from history.
    *
    * @throws CmsException  Throws CmsException if operation was not succesful.
    */
    public Vector getAllBackupProjects() throws CmsException {
        return m_driverManager.getAllBackupProjects();
    }

    /**
     * Returns a Vector with all export links
     *
     * @return Vector (Strings) with all export links.
     *
     * @throws CmsException  Throws CmsException if operation was not succesful.
     */
    public Vector getAllExportLinks() throws CmsException {
        return m_driverManager.getAllExportLinks();
    }

    /**
     * Returns all projects which are owned by the current user or which are manageable
     * for the group of the user.
     *
     * @return a Vector of objects of type <code>CmsProject</code>.
     *
     * @throws CmsException if operation was not successful.
     */
    public Vector getAllManageableProjects() throws CmsException {
        return (m_driverManager.getAllManageableProjects(m_context));
    }

    /**
     * Returns a List with all initialized resource types.<p>
     *
     * @return a List with all initialized resource types
     * @throws CmsException if something goes wrong
     */
    public List getAllResourceTypes() throws CmsException {
        I_CmsResourceType resourceTypes[] = m_driverManager.getAllResourceTypes();
        List result = new ArrayList(resourceTypes.length);
        for (int i = 0; i < resourceTypes.length; i++)
            if (resourceTypes[i] != null)
                result.add(resourceTypes[i]);

        return result;
    }

    /**
     * Get the next version id for the published backup resources
     *
     * @return int The new version id
     */
    public int getBackupVersionId() {
        return m_driverManager.getBackupVersionId();
    }

    /**
     * checks a project for broken links that would appear if the project is published.
     *
     * @param projectId
     * @param report A cmsReport object for logging while the method is still running.
     * @param changed A vecor (of CmsResources) with the changed resources in the project.
     * @param deleted A vecor (of CmsResources) with the deleted resources in the project.
     * @param newRes A vecor (of CmsResources) with the new resources in the project.
     */
    public void getBrokenLinks(int projectId, I_CmsReport report, Vector changed, Vector deleted, Vector newRes) throws CmsException {
        m_driverManager.getBrokenLinks(projectId, report, changed, deleted, newRes);
    }

    /**
    * Gets information about the cache size.
    * <br>
    * The size of the following caching areas is returned:
    * <ul>
    *  <li>GroupCache</li>
    *  <li>UserGroupCache</li>
    *  <li>ResourceCache</li>
    *  <li>SubResourceCache</li>
    *  <li>ProjectCache</li>
    *  <li>PropertyCache</li>
    *  <li>PropertyDefinitionCache</li>
    *  <li>PropertyDefinitionVectorCache</li>
    * </ul>
    * @return a Hashtable with information about the size of the various cache areas.
    */
    public Hashtable getCacheInfo() {
        return m_driverManager.getCacheInfo();
    }

    /**
     * Returns all child groups of a group.
     *
     * @param groupname the name of the group.
     * @return groups a Vector of all child groups or null.
     * @throws CmsException if operation was not successful.
     */
    public Vector getChild(String groupname) throws CmsException {
        return (m_driverManager.getChild(m_context, groupname));
    }

    /**
     * Returns all child groups of a group.
     * <br>
     * This method also returns all sub-child groups of the current group.
     *
     * @param groupname the name of the group.
     * @return groups a Vector of all child groups or null.
     * @throws CmsException if operation was not successful.
     */
    public Vector getChilds(String groupname) throws CmsException {
        return (m_driverManager.getChilds(m_context, groupname));
    }

    /**
     * Creates a special CmsObject for the static export.
     *
     * @param .
     * @throws CmsException if operation was not successful.
     */
    public CmsObject getCmsObjectForStaticExport(CmsExportRequest dReq, CmsExportResponse dRes) throws CmsException {

        CmsObject cmsForStaticExport = new CmsObject();
        cmsForStaticExport.init(m_driverManager, dReq, dRes, OpenCms.getDefaultUsers().getUserGuest(), I_CmsConstants.C_PROJECT_ONLINE_ID, getRequestContext().getSiteRoot(), null, m_context.getDirectoryTranslator(), m_context.getFileTranslator());
        return cmsForStaticExport;
    }

    /**
     * Gets the configurations of the properties-file.
     * @return the configurations of the properties-file.
     */
    public Configurations getConfigurations() {
        return m_driverManager.getConfigurations();
    }

    /**
     * Reads all export links that depend on the resource.
     * @param res. The resourceName() of the resources that has changed (or the String
     *              that describes a contentdefinition).
     * @return a Vector(of Strings) with the linkrequest names.
     */
    public Vector getDependings(Vector res) throws CmsException {
        return m_driverManager.getDependingExportLinks(res);
    }

    /**
     * Gets all groups to which a given user directly belongs.
     *
     * @param username the name of the user to get all groups for.
     * @return a Vector of all groups of a user.
     *
     * @throws CmsException if operation was not successful.
     */
    public Vector getDirectGroupsOfUser(String username) throws CmsException {
        return (m_driverManager.getDirectGroupsOfUser(m_context, username));
    }

    /**
     * Returns the generic driver objects.<p>
     * 
     * @return a mapping of class names to driver objects
     */
    public Map getDrivers() {
        HashMap drivers = new HashMap();
        drivers.put(this.m_driverManager.getVfsDriver().getClass().getName(), this.m_driverManager.getVfsDriver());
        drivers.put(this.m_driverManager.getUserDriver().getClass().getName(), this.m_driverManager.getUserDriver());
        drivers.put(this.m_driverManager.getProjectDriver().getClass().getName(), this.m_driverManager.getProjectDriver());
        drivers.put(this.m_driverManager.getWorkflowDriver().getClass().getName(), this.m_driverManager.getWorkflowDriver());
        drivers.put(this.m_driverManager.getBackupDriver().getClass().getName(), this.m_driverManager.getBackupDriver());    
        return drivers;
    }
    
    /**
     * Returns a Vector with all files of a given folder.
     * (only the direct subfiles, not the files in subfolders)
     * <br>
     * Files of a folder can be read from an offline Project and the online Project.
     *
     * @param foldername the complete path to the folder.
     *
     * @return subfiles a Vector with all files of the given folder.
     *
     * @throws CmsException if the user has not hte appropriate rigths to access or read the resource.
     */
    public List getFilesInFolder(String foldername) throws CmsException {
        return (m_driverManager.getSubFiles(m_context, addSiteRoot(foldername), false));
    }

    /**
     * Returns a Vector with all files of a given folder.
     * <br>
     * Files of a folder can be read from an offline Project and the online Project.
     *
     * @param foldername the complete path to the folder.
     * @param includeDeleted Include if the folder is marked as deleted
     *
     * @return subfiles a Vector with all files of the given folder.
     *
     * @throws CmsException if the user has not hte appropriate rigths to access or read the resource.
     */
    public List getFilesInFolder(String foldername, boolean includeDeleted) throws CmsException {
        return (m_driverManager.getSubFiles(m_context, addSiteRoot(foldername), includeDeleted));
    }

    /**
     * Returns a Vector with all resource-names of the resources that have set the given property to the given value.
     *
     * @param propertydef the name of the property-definition to check.
     * @param property the value of the property for the resource.
     *
     * @return a Vector with all names of the resources.
     *
     * @throws CmsException if operation was not successful.
     */
    public Vector getFilesWithProperty(String propertyDefinition, String propertyValue) throws CmsException {
        return m_driverManager.getFilesWithProperty(m_context, propertyDefinition, propertyValue);
    }

    /**
     * This method can be called, to determine if the file-system was changed in the past.
     * <br>
     * A module can compare its previously stored number with the returned number.
     * If they differ, the file system has been changed.
     *
     * @return the number of file-system-changes.
     */
    public long getFileSystemFolderChanges() {
        return (m_driverManager.getFileSystemFolderChanges());
    }

    /**
     * Returns all groups in the Cms.
     *
     * @return a Vector of all groups in the Cms.
     *
     * @throws CmsException if operation was not successful
     */
    public Vector getGroups() throws CmsException {
        return (m_driverManager.getGroups(m_context));
    }

    /**
     * Gets all groups of a user.
     *
     * @param username the name of the user to get all groups for.
     * @return Vector of all groups of a user.
     *
     * @throws CmsException if operation was not succesful.
     */
    public Vector getGroupsOfUser(String username) throws CmsException {
        return (m_driverManager.getGroupsOfUser(m_context, username));
    }

    /**
     * This is the port the workplace access is limited to. With the opencms.properties
     * the access to the workplace can be limited to a user defined port. With this
     * feature a firewall can block all outside requests to this port with the result
     * the workplace is only available in the local net segment.
     * @return the portnumber or -1 if no port is set.
     */
    public int getLimitedWorkplacePort() {
        return m_driverManager.getLimitedWorkplacePort();
    }

    /**
     * Returns a list of all currently logged in users.
     * This method is only allowed for administrators.
     */
    public Vector getLoggedInUsers() throws CmsException {
        if (isAdmin()) {
            if (m_sessionStorage != null) {
                return m_sessionStorage.getLoggedInUsers();
            } else {
                return null;
            }
        } else {
            throw new CmsSecurityException("[" + this.getClass().getName() + "] getLoggedInUsers()", CmsSecurityException.C_SECURITY_ADMIN_PRIVILEGES_REQUIRED);
        }
    }

    /**
     * Returns the mode this cmsObject is runnig in. AUTO mode (-1) means
     * it is no special case and returns online ore offline depending on the
     * current project.
     *
     * @return int The modus of this cmsObject.
     */
    public int getMode() {
        if (m_mode == I_CmsConstants.C_MODUS_AUTO) {
            if (getRequestContext().currentProject().isOnlineProject()) {
                return I_CmsConstants.C_MODUS_ONLINE;
            } else {
                return I_CmsConstants.C_MODUS_OFFLINE;
            }
        }
        return m_mode;
    }

    /**
     * serches for broken links in the online project.
     *
     * @return A Vector with a CmsPageLinks object for each page containing broken links
     *          this CmsPageLinks object contains all links on the page withouth a valid target.
     */
    public Vector getOnlineBrokenLinks() throws CmsException {
        return m_driverManager.getOnlineBrokenLinks();
    }

    /**
     * extracts the links of the page and returns them in a CmsPageLinks object.
     *
     * @param page. The page to process.
     * @return CmsPageLinks The link destinations on the page.
     */
    public CmsPageLinks getPageLinks(String page) throws CmsException {
        return m_linkChecker.extractLinks(this, page);
    }

    /**
     * Returns the parent group of a group.
     *
     * @param groupname the name of the group.
     * @return group the parent group or null.
     * @throws CmsException if operation was not successful.
     */
    public CmsGroup getParent(String groupname) throws CmsException {
        return (m_driverManager.getParent(m_context, groupname));
    }

    /**
     * Returns the set set of permissions of the current user for a given resource. 
     * 
     * @param resourceName		the name of the resource
     * @return					the set of the permissions of the current user
     * @throws CmsException		if something goes wrong
     */
    public CmsPermissionSet getPermissions(String resourceName) throws CmsException {
        // reading permissions is allowed even if the resource is marked as deleted
        CmsResource resource = readFileHeader(resourceName, true);
        CmsUser user = m_context.currentUser();

        return m_driverManager.getPermissions(m_context, resource, user);
    }

    /**
     * Returns the set set of permissions of a given user for a given resource.
     * 
     * @param resourceName	the name of the resource
     * @param userName		the name of the user
     * @return				the current permissions on this resource
     * @throws CmsException	if something goes wrong
     */
    public CmsPermissionSet getPermissions(String resourceName, String userName) throws CmsException {
        CmsAccessControlList acList = getAccessControlList(resourceName);
        CmsUser user = readUser(userName);
        return acList.getPermissions(user, getGroupsOfUser(userName));
    }

    /**
     * Gets the Registry.
     *
     *
     * @throws CmsException if access is not allowed.
     */

    public I_CmsRegistry getRegistry() throws CmsException {
        return (m_driverManager.getRegistry(this));
    }

    /**
     * Returns the current request-context.
     *
     * @return the current request-context.
     */
    public CmsRequestContext getRequestContext() {
        return (m_context);
    }

    /**
     * Returns a Vector with the subresources for a folder.<br>
     *
     * @param folder The name of the folder to get the subresources from.
     *
     * @return subfolders A Vector with resources.
     *
     * @throws CmsException  Throws CmsException if operation was not succesful.
     */
    public Vector getResourcesInFolder(String folder) throws CmsException {
        return m_driverManager.getResourcesInFolder(m_context, addSiteRoot(folder));
    }

    /**
    * Returns a List with all sub resources of a given folder that have benn modified
    * in a given time range.<p>
    * 
    * The rertuned list is sorted descending (newest resource first).
    *
    * <B>Security:</B>
    * All users are granted.
    *
    * @param folder the folder to get the subresources from
    * @param starttime the begin of the time range
    * @param endtime the end of the time range
    * @return List with all resources
    *
    * @throws CmsException if operation was not succesful 
    */
    public List getResourcesInTimeRange(String folder, long starttime, long endtime) throws CmsException {
        return m_driverManager.getResourcesInTimeRange(m_context, addSiteRoot(folder), starttime, endtime);
    }

    /**
      * Returns a Vector with all resources of the given type that have set the given property.
      *
      * <B>Security:</B>
      * All users are granted.
      *
      * @param propertyDefinition, the name of the propertydefinition to check.
      * @param propertyValue, the value of the property for the resource.
      * @param resourceType The resource type of the resource
      *
      * @return Vector with all resources.
      *
      * @throws CmsException Throws CmsException if operation was not succesful.
      */
    public Vector getResourcesWithPropertyDefinition(String propertyDefinition) throws CmsException {
        return m_driverManager.getResourcesWithPropertyDefinition(m_context, propertyDefinition);
    }

    /**
     * Returns a List with the complete sub tree of a given folder that have set the given property.<p>
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param folder the folder to get the subresources from
     * @param propertyDefinition the name of the propertydefinition to check
     * @return List with all resources
     *
     * @throws CmsException if operation was not succesful
     */
    public List getResourcesWithProperty(String folder, String propertyDefinition) throws CmsException {
        return m_driverManager.getResourcesWithProperty(m_context, addSiteRoot(folder), propertyDefinition);
    }

    /**
     * Returns a List with resources that have set the given property.<p>
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param propertyDefinition the name of the propertydefinition to check
     * @return List with all resources
     *
     * @throws CmsException if operation was not succesful
     */
    public List getResourcesWithProperty(String propertyDefinition) throws CmsException {
        return m_driverManager.getResourcesWithProperty(m_context, "/", propertyDefinition);
    }

    /**
      * Returns a Vector with all resources of the given type that have set the given property to the given value.
      *
      * <B>Security:</B>
      * All users are granted.
      *
      * @param propertyDefinition, the name of the propertydefinition to check.
      * @param propertyValue, the value of the property for the resource.
      * @param resourceType The resource type of the resource
      *
      * @return Vector with all resources.
      *
      * @throws CmsException Throws CmsException if operation was not succesful.
      */
    public Vector getResourcesWithPropertyDefintion(String propertyDefinition, String propertyValue, int resourceType) throws CmsException {
        return m_driverManager.getResourcesWithPropertyDefintion(m_context, propertyDefinition, propertyValue, resourceType);
    }

    /**
     * Returns the initialized resource type instance for the given id.<p>
     * 
     * @param resourceType the id of the resourceType to get
     * @return the initialized resource type instance for the given id
     * @throws CmsException if something goes wrong
     */
    public I_CmsResourceType getResourceType(int resourceType) throws CmsException {
        return m_driverManager.getResourceType(resourceType);
    }

    /**
     * Returns the resource type id for the given resource type name.<p>
     * 
     * @param resourceType the name of the resourceType to get the id for
     * @return the resource type id for the given resource type name
     * @throws CmsException if something goes wrong
     */
    public int getResourceTypeId(String resourceType) throws CmsException {
        I_CmsResourceType type = m_driverManager.getResourceType(resourceType);
        if (type != null) {
            return type.getResourceType();
        } else {
            return I_CmsConstants.C_UNKNOWN_ID;
        }
    }

    /**
     * Returns a Vector with all subfolders of a given folder.
     *
     * @param foldername the complete path to the folder.
     *
     * @return subfolders a Vector with all subfolders for the given folder.
     *
     * @throws CmsException if the user has not the rights to access or read the resource.
     */
    public List getSubFolders(String foldername) throws CmsException {
        return (m_driverManager.getSubFolders(m_context, addSiteRoot(foldername), false));
    }

    /**
     * Returns a Vector with all subfolders of a given folder.
     *
     * @param foldername the complete path to the folder.
     * @param includeDeleted Include if the folder is marked as deleted
     *
     * @return subfolders a Vector with all subfolders (CmsFolder Objects) for the given folder.
     *
     * @throws CmsException if the user has not the rights to access or read the resource.
     */
    public List getSubFolders(String foldername, boolean includeDeleted) throws CmsException {
        return (m_driverManager.getSubFolders(m_context, addSiteRoot(foldername), includeDeleted));
    }

    /**
      * Get a parameter value for a task.
      *
      * @param taskid the id of the task.
      * @param parname the name of the parameter.
      * @return the parameter value.
      *
      * @throws CmsException if operation was not successful.
      */
    public String getTaskPar(int taskid, String parname) throws CmsException {
        return (m_driverManager.getTaskPar(m_context, taskid, parname));
    }

    /**
     * Get the template task id fo a given taskname.
     *
     * @param taskname the name of the task.
     *
     * @return the id of the task template.
     *
     * @throws CmsException if operation was not successful.
     */
    public int getTaskType(String taskname) throws CmsException {
        return m_driverManager.getTaskType(taskname);
    }

    /**
     * Returns all users in the Cms.
     *
     * @return a Vector of all users in the Cms.
     *
     * @throws CmsException if operation was not successful.
     */
    public Vector getUsers() throws CmsException {
        return (m_driverManager.getUsers(m_context));
    }

    /**
     * Returns all users of the given type in the Cms.
     *
     * @param type the type of the users.
     *
     * @return vector of all users of the given type in the Cms.
     *
     * @throws CmsException if operation was not successful.
     */
    public Vector getUsers(int type) throws CmsException {
        return (m_driverManager.getUsers(m_context, type));
    }

    /**
    * Returns all users from a given type that start with a specified string<P/>
    *
    * @param type the type of the users.
    * @param namestart The filter for the username
    * @return vector of all users of the given type in the Cms.
    *
    * @throws CmsException if operation was not successful.
    */
    public Vector getUsers(int type, String namefilter) throws CmsException {
        return m_driverManager.getUsers(m_context, type, namefilter);
    }

    /**
     * Gets all users with a certain Lastname.
     *
     * @param Lastname      the start of the users lastname
     * @param UserType      webuser or systemuser
     * @param UserStatus    enabled, disabled
     * @param wasLoggedIn   was the user ever locked in?
     * @param nMax          max number of results
     *
     * @return the users.
     *
     * @throws CmsException if operation was not successful.
     *
     */
    public Vector getUsersByLastname(String Lastname, int UserType, int UserStatus, int wasLoggedIn, int nMax) throws CmsException {

        return m_driverManager.getUsersByLastname(m_context, Lastname, UserType, UserStatus, wasLoggedIn, nMax);
    }

    /**
     * Gets all users of a group.
     *
     * @param groupname the name of the group to get all users for.
     * @return all users in the group.
     *
     * @throws CmsException if operation was not successful.
     */
    public Vector getUsersOfGroup(String groupname) throws CmsException {
        return (m_driverManager.getUsersOfGroup(m_context, groupname));
    }

    /**
     * Returns a Vector with all resources of the given type that have set the given property to the given value.
     *
     * <B>Security:</B>
     * All users that have read and view access are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param propertyDefinition, the name of the propertydefinition to check.
     * @param propertyValue, the value of the property for the resource.
     * @param resourceType The resource type of the resource
     *
     * @return Vector with all resources.
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getVisibleResourcesWithProperty(String propertyDefinition, String propertyValue, int resourceType) throws CmsException {
        return m_driverManager.getVisibleResourcesWithProperty(m_context, propertyDefinition, propertyValue, resourceType);
    }

    /**
     * Checks if the current user has required permissions to access a given resource in a given project.
     * 
     * @param project				the project 
     * @param resource				the resource that will be accessed
     * @param requiredPermissions	the set of required permissions
     * @return						true if the required permissions are satisfied
     * @throws CmsException			if something goes wrong
     */
    public boolean hasPermissions(CmsProject project, CmsResource resource, CmsPermissionSet requiredPermissions) throws CmsException {
        // TODO: Check if this method still works after inserting the context instead of the project
        return m_driverManager.hasPermissions(m_context, resource, requiredPermissions, false);
    }

    /**
     * Checks if the current user has required permissions to access a given resource.
     * 
     * @param resource				the resource that will be accessed
     * @param requiredPermissions	the set of required permissions
     * @return						true if the required permissions are satisfied
     * @throws CmsException			if something goes wrong
     */
    public boolean hasPermissions(CmsResource resource, CmsPermissionSet requiredPermissions) throws CmsException {
        return m_driverManager.hasPermissions(m_context, resource, requiredPermissions, false);
    }

    /**
     * Checks if the current user has required permissions to access a given resource
     * 
     * @param resourceName			the name of the resource that will be accessed
     * @param requiredPermissions	the set of required permissions
     * @return						true if the required permissions are satisfied
     * @throws CmsException			if something goes wrong
     */
    public boolean hasPermissions(String resourceName, CmsPermissionSet requiredPermissions) throws CmsException {
        CmsResource resource = readFileHeader(resourceName);
        return m_driverManager.hasPermissions(m_context, resource, requiredPermissions, false);
    }

    /**
     * Imports a import-resource (folder or zipfile) to the cms.
     *
     * @param importFile the name (absolute Path) of the import resource (zipfile or folder).
     * @param importPath the name (absolute Path) of the folder in which should be imported.
     *
     * @throws CmsException if operation was not successful.
     */
    public void importFolder(String importFile, String importPath) throws CmsException {
        // import the resources
        clearcache();
        m_driverManager.importFolder(this, m_context, importFile, addSiteRoot(importPath));
        clearcache();
    }

    /**
     * Imports a resource to the cms.<p>
     *
     * @param resource the resource to be imported
     * @param content the content of the resource
     * @param properties the properties of the resource
     * @param importpath the name of the resource destinaition
     * @return the imported CmsResource
     * @throws CmsException if operation was not successful
     */
    public CmsResource importResource(CmsResource resource, byte[] content, Map properties, String importpath) throws CmsException {
        return getResourceType(resource.getType()).importResource(this, resource, content, properties, importpath);
    }

    /**
     * Imports a import-resource (folder or zip-file) to the cms.
     *
     * @param importFile the name (absolute Path) of the import resource (zipfile or folder).
     * @param importPath the name (absolute Path) of folder in which should be imported.
     *
     * @throws CmsException if operation was not successful.
     */
    public void importResources(String importFile, String importPath) throws CmsException {
        importResources(importFile, importPath, new CmsShellReport());
    }

    /**
     * Imports a import-resource (folder or zip-file) to the cms.
     *
     * @param importFile the name (absolute Path) of the import resource (zipfile or folder).
     * @param importPath the name (absolute Path) of folder in which should be imported.
     * @param report A report object to provide the loggin messages.
     *
     * @throws CmsException if operation was not successful.
     */
    public void importResources(String importFile, String importPath, I_CmsReport report) throws CmsException {
        clearcache();

        // import the resources
        m_driverManager.importResources(this, m_context, importFile, importPath, report);

        clearcache();
    }

    /**
     * Checks, if the users current group is the admin-group.
     *
     *
     * @return <code>true</code>, if the users current group is the admin-group; <code>false</code> otherwise.
     * @throws CmsException if operation was not successful.
     */
    public boolean isAdmin() throws CmsException {
        return m_driverManager.isAdmin(m_context);
    }

    /**
     * Check if the history is enabled
     *
     * @return boolean Is true if history is enabled
     */
    public boolean isHistoryEnabled() {
        return m_driverManager.isHistoryEnabled(this);
    }

    /**
     * Checks, if the user has management access to the project.
     *
     * @return <code>true</code>, if the users current group is the admin-group; <code>false</code> otherwise.
     * @throws CmsException if operation was not successful.
     */
    public boolean isManagerOfProject() throws CmsException {
        return m_driverManager.isManagerOfProject(m_context);
    }

    /**
     * Returns the user, who has locked a given resource.
     * <br>
     * A user can lock a resource, so he is the only one who can write this
     * resource. This methods checks, who has locked a resource.
     *
     * @param resource the resource to check.
     *
     * @return the user who has locked the resource.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsUser lockedBy(CmsResource resource) throws CmsException {
        return (m_driverManager.lockedBy(m_context, resource));
    }

    /**
        * Returns the user, who has locked a given resource.
        * <br>
        * A user can lock a resource, so he is the only one who can write this
        * resource. This methods checks, who has locked a resource.
        *
        * @param resource The complete path to the resource.
        *
        * @return the user who has locked a resource.
        *
        * @throws CmsException if operation was not successful.
        */
    public CmsUser lockedBy(String resource) throws CmsException {
        return (m_driverManager.lockedBy(m_context, addSiteRoot(resource)));
    }

    /**
     * Locks the given resource.
     * <br>
     * A user can lock a resource, so he is the only one who can write this
     * resource.
     *
     * @param resource The complete path to the resource to lock.
     *
     * @throws CmsException if the user has not the rights to lock this resource.
     * It will also be thrown, if there is an existing lock.
     *
     */
    public void lockResource(String resource) throws CmsException {
        // try to lock the resource, prevent from overwriting an existing lock
        lockResource(resource, false);
    }

    /**
     * Locks a given resource.
     * <br>
     * A user can lock a resource, so he is the only one who can write this
     * resource.
     *
     * @param resource the complete path to the resource to lock.
     * @param force if force is <code>true</code>, a existing locking will be overwritten.
     *
     * @throws CmsException if the user has not the rights to lock this resource.
     * It will also be thrown, if there is a existing lock and force was set to false.
     */
    public void lockResource(String resource, boolean force) throws CmsException {
        getResourceType(readFileHeader(resource).getType()).lockResource(this, resource, force);
    }
    
    public void changeLock(String resourcename) throws CmsException {
        m_driverManager.changeLock(m_context, addSiteRoot(resourcename));
    }

    /**
     * Logs a user into the Cms, if the password is correct.<p>
     *
     * @param username the name of the user
     * @param password the password of the user
     * @return the name of the logged in user
     *
     * @throws CmsException if operation was not successful
     */
    public String loginUser(String username, String password) throws CmsException {
        return loginUser(username, password, m_context.getRemoteAddress());
    }
    
    /**
     * Logs a user with a given ip address into the Cms, if the password is correct.<p>
     *
     * @param username the name of the user
     * @param password the password of the user
     * @param remoteAddress the ip address
     * @return the name of the logged in user
     *
     * @throws CmsException if operation was not successful
     */    
    public String loginUser(String username, String password, String remoteAddress) throws CmsException {
        // login the user
        CmsUser newUser = m_driverManager.loginUser(m_context, username, password, remoteAddress);
        // init the new user
        init(m_driverManager, m_context.getRequest(), m_context.getResponse(), newUser.getName(), I_CmsConstants.C_PROJECT_ONLINE_ID, m_context.getSiteRoot(), m_sessionStorage, m_context.getDirectoryTranslator(), m_context.getFileTranslator());

        this.fireEvent(com.opencms.flex.I_CmsEventListener.EVENT_LOGIN_USER, newUser);

        // return the user-name
        this.fireEvent(com.opencms.flex.I_CmsEventListener.EVENT_LOGIN_USER, newUser);
        return (newUser.getName());
    }

    /**
     * Logs a web user into the Cms, if the password is correct.
     *
     * @param username the name of the user.
     * @param password the password of the user.
     * @return the name of the logged in user.
     *
     * @throws CmsException if operation was not successful
     */
    public String loginWebUser(String username, String password) throws CmsException {
        // login the user
        CmsUser newUser = m_driverManager.loginWebUser(m_context, username, password, m_context.getRemoteAddress());
        // init the new user
        init(m_driverManager, m_context.getRequest(), m_context.getResponse(), newUser.getName(), I_CmsConstants.C_PROJECT_ONLINE_ID, m_context.getSiteRoot(), m_sessionStorage, m_context.getDirectoryTranslator(), m_context.getFileTranslator());
        // return the user-name
        return (newUser.getName());
    }

    /**
     * Lookup and reads the user or group with the given UUID.
     *   
     * @param principalId
     * @return
     */
    public I_CmsPrincipal lookupPrincipal(CmsUUID principalId) throws CmsException {
        return m_driverManager.lookupPrincipal(principalId);
    }

    /**
     * Lookup and reads the user or group with the given name.
     * 
     * @param principalName
     * @return
     * @throws CmsException	if something goes wrong
     */
    public I_CmsPrincipal lookupPrincipal(String principalName) throws CmsException {
        return m_driverManager.lookupPrincipal(principalName);
    }

    /**
     * Moves a resource to the given destination.
     *
     * @param source the complete path of the sourcefile.
     * @param destination the complete path of the destinationfile.
     *
     * @throws CmsException if the user has not the rights to move this resource,
     * or if the file couldn't be moved.
     */
    public void moveResource(String source, String destination) throws CmsException {
        getResourceType(readFileHeader(source).getType()).moveResource(this, source, destination);
    }


    /**
     * Moves a resource to the lost and found folder
     *
     * @param source the complete path of the sourcefile.
     * @return location of the moved resource
     * @throws CmsException if the user has not the rights to move this resource,
     * or if the file couldn't be moved.
     */
    public String copyToLostAndFound(String source) throws CmsException {
        return getResourceType(readFileHeader(source).getType()).copyToLostAndFound(this, source, true);
    }

    /**
     * Publishes a project.<p>
     *
     * @param id the id of the project to be published
     * @throws CmsException if operation was not successful
     */
    public void publishProject() throws CmsException {
        publishProject(new CmsShellReport());
    }

    /**
     * Publishes a project.<p>
     *
     * @param publishProjectId the id of the project to be published
     * @param report A report object to provide the loggin messages
     * @throws CmsException if operation was not successful
     */
    public void publishProject(I_CmsReport report) throws CmsException {
        Vector newResources = null;
        Vector deletedResources = null;
        Vector changedResources = null;
        Vector changedModuleMasters = null;
        boolean success = false;
        CmsPublishedResources publishedResources = null;

        clearcache();

        publishedResources = new CmsPublishedResources(m_context.currentProject());
        try {
           
            // first we remember the new resources for the link management
            newResources = m_driverManager.readPublishProjectView(m_context, m_context.currentProject().getId(), "new");
            
            deletedResources = m_driverManager.readPublishProjectView(m_context, m_context.currentProject().getId(), "deleted");
            
            changedResources = m_driverManager.readPublishProjectView(m_context, m_context.currentProject().getId(), "changed");

            updateOnlineProjectLinks(deletedResources, changedResources, null, CmsResourceTypePage.C_RESOURCE_TYPE_ID);
           
            publishedResources = m_driverManager.publishProject(this, m_context, report);

            // update the online links table for the new resources (now they are there)
            updateOnlineProjectLinks(null, null, newResources, CmsResourceTypePage.C_RESOURCE_TYPE_ID);

            changedResources.clear();
            changedResources = null;
            newResources = null;

            changedResources = publishedResources.getChangedResources();
            changedModuleMasters = publishedResources.getChangedModuleMasters();
 
            if (CmsXmlTemplateLoader.getOnlineElementCache() != null) {
                CmsXmlTemplateLoader.getOnlineElementCache().cleanupCache(changedResources, changedModuleMasters);
            }

            clearcache();
            success = true;
        } catch (Exception e) {
            String stamp1 = "[" + this.getClass().getName() + ".publishProject()/1] Project:" + m_context.currentProject().getId() + " Time:" + new Date();
            String stamp2 = "[" + this.getClass().getName() + ".publishProject()/1] User: " + m_context.currentUser().toString();
            if (DEBUG > 0) {
                System.err.println("###################################");
                System.err.println(stamp1);
                System.err.println(stamp2);
                e.printStackTrace();
                System.err.println("Vector of changed resources:");
                if (changedResources != null) {
                    for (int i = 0; i < changedResources.size(); i++) {
                        System.err.println("    -- " + i + " -->" + (String)changedResources.get(i) + "<--");
                    }
                }
            }
            if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, stamp1);
                OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, stamp2);
                OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishProject()/1] Exception: " + e);
            }
        } finally {
            if (changedResources == null || changedResources.size() < 1) {
                String stamp1 = "[" + this.getClass().getName() + ".publishProject()/2] Project:" + m_context.currentProject().getId() + " Time:" + new Date();
                String stamp2 = "[" + this.getClass().getName() + ".publishProject()/2] User: " + m_context.currentUser().toString();
                String stamp3 = "[" + this.getClass().getName() + ".publishProject()/2] Vector was null or empty";
                if (DEBUG > 0) {
                    System.err.println("###################################");
                    System.err.println(stamp1);
                    System.err.println(stamp2);
                    System.err.println(stamp3);
                }
                if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_DEBUG)) {
                    OpenCms.log(I_CmsLogChannels.C_OPENCMS_DEBUG, stamp1);
                    OpenCms.log(I_CmsLogChannels.C_OPENCMS_DEBUG, stamp2);
                    OpenCms.log(I_CmsLogChannels.C_OPENCMS_DEBUG, stamp3);
                }
                success = false;
            }
            if (!success) {
                if (CmsXmlTemplateLoader.getOnlineElementCache() != null)
                    CmsXmlTemplateLoader.getOnlineElementCache().clearCache();
            }
            // set current project to online project if the published project was temporary
            // and the published project is still the current project
            if (m_context.currentProject().getId() == m_context.currentProject().getId() && (m_context.currentProject().getType() == I_CmsConstants.C_PROJECT_TYPE_TEMPORARY)) {
                m_context.setCurrentProject(I_CmsConstants.C_PROJECT_ONLINE_ID);
            }
        }

        this.fireEvent(I_CmsEventListener.EVENT_PUBLISH_PROJECT, publishedResources);
    }

    /**
     * Publishes a single resource.
     *
     * @param resoucename the name (getAbsolutePath()) of the resource to be published.
     *
     * @throws CmsException if operation was not successful.
     */
    public void publishResource(String resourcename) throws CmsException {
        publishResource(resourcename, false);
    }

    /**
     * Publishes a single resource.
     *
     * @param id the id of the project to be published.
     * @param justPrepear, if true this method dont publish the temp project that is created.
     *          This is used for the workplace view were the temp-projcet is checkt for broken
     *          links before publish.
     * @return the project id of the created project.
     *
     * @throws CmsException if operation was not successful.
     */
    public int publishResource(String resourcename, boolean justPrepare) throws CmsException {
        return publishResource(resourcename, justPrepare, justPrepare ? null : new CmsShellReport());
    }

    public int publishResource(String resourcename, boolean justPrepare, I_CmsReport report) throws CmsException {
        int oldProjectId = m_context.currentProject().getId();
        int retValue = -1;
        
        CmsResource res = readFileHeader(resourcename, true);
        if (res.isFolder()) res = readFolder(resourcename, true);
        
        CmsLock lock = getLock(resourcename);
        
        if (!lock.isNullLock()) {
            throw new CmsSecurityException("[CmsObject] cannot publish locked resource", CmsSecurityException.C_SECURITY_NO_PERMISSIONS);
        }
        if (res.getState() == I_CmsConstants.C_STATE_NEW) {
            try {
                m_driverManager.readFolder(m_context, res.getParentId(), false);
            } catch (CmsException ex) {
                throw new CmsException("[CmsObject] cannot read parent folder in online project", CmsException.C_NOT_FOUND);
            }
        }
        if (oldProjectId != I_CmsConstants.C_PROJECT_ONLINE_ID) {
            // check access to project
            if (isAdmin() || isManagerOfProject()) {
                String projectName = justPrepare ? "Check direct publish" : "Direct publish";
                int newProjectId = m_driverManager.createDirectPublishProject(
                    m_context, projectName, 
                    "", 
                    OpenCms.getDefaultUsers().getGroupUsers(), 
                    OpenCms.getDefaultUsers().getGroupProjectmanagers(), 
                    I_CmsConstants.C_PROJECT_TYPE_TEMPORARY
                ).getId();
                retValue = newProjectId;
                getRequestContext().setCurrentProject(newProjectId);
                I_CmsResourceType rt = getResourceType(res.getType());
                // copy the resource to the project
                rt.copyResourceToProject(this, resourcename);
                // set the project_id of the resource to the current project
                rt.changeLockedInProject(this, newProjectId, resourcename);
                if (!justPrepare) {
                    // publish the temporary project
                    publishProject(report);
                }
                getRequestContext().setCurrentProject(oldProjectId);
            } else {
                throw new CmsSecurityException("[CmsObject] cannot publish resource in current project", CmsSecurityException.C_SECURITY_PROJECTMANAGER_PRIVILEGES_REQUIRED);
            }
        } else {
            throw new CmsSecurityException("[CmsObject] cannot publish resource in online project", CmsSecurityException.C_SECURITY_NO_MODIFY_IN_ONLINE_PROJECT);
        }
        
        //this.fireEvent(com.opencms.flex.I_CmsEventListener.EVENT_PUBLISH_RESOURCE, res);
        OpenCms.fireCmsEvent(new CmsEvent(this, I_CmsEventListener.EVENT_PUBLISH_RESOURCE, Collections.singletonMap("resource", res)));
        
        return retValue;
    }

    public String readAbsolutePath(CmsResource resource) {
        return readAbsolutePath(resource, false);
    }

    public String readAbsolutePath(CmsResource resource, boolean includeDeleted) {
        try {
            if (!resource.hasFullResourceName()) {
                m_driverManager.readPath(m_context, resource, includeDeleted);
            }
        } catch (CmsException e) {
            resource.setFullResourceName(null);
        }

        // adjust the resource path for the current site root
        return removeSiteRoot(resource.getFullResourceName());
    }

    /**
     * Reads the agent of a task from the OpenCms.
     *
     * @param task the task to read the agent from.
     * @return the owner of a task.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsUser readAgent(CmsTask task) throws CmsException {
        return (m_driverManager.readAgent(m_context, task));
    }

    /**
     * Reads all file headers of a file in the OpenCms.
     * <br>
     * This method returns a vector with the history of all file headers, i.e.
     * the file headers of a file, independent of the project they were attached to.<br>
     *
     * The reading excludes the filecontent.
     *
     * @param filename the name of the file to be read.
     *
     * @return a Vector of file headers read from the Cms.
     *
     * @throws CmsException  if operation was not successful.
     */
    public List readAllBackupFileHeaders(String filename) throws CmsException {
        return (m_driverManager.readAllBackupFileHeaders(m_context, addSiteRoot(filename)));
    }

    /**
     * select all projectResources from an given project
     *
     * @param project The project in which the resource is used.
     *
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Vector readAllProjectResources(int projectId) throws CmsException {
        return m_driverManager.readAllProjectResources(m_context, projectId);
    }

    /**
     * Returns a list of all properties of a file or folder.
     *
     * @param name the name of the resource for which the property has to be read
     *
     * @return a Vector of Strings
     *
     * @throws CmsException if operation was not succesful
     * @deprecated use readProperties(String) instead
     */
    public Map readAllProperties(String filename) throws CmsException {
        Map result = (Map)new HashMap();
        Map properties = readProperties(filename, false);
        if (properties != null)
            result.putAll(properties);
        return result;
    }

    /**
     * Reads all property-definitions for the given resource type.
     *
     * @param id the id of the resource type to read the property-definitions for.
     *
     * @return a Vector with property-defenitions for the resource type.
     * The Vector may be empty.
     *
     * @throws CmsException if operation was not successful.
     */
    public Vector readAllPropertydefinitions(int resourceType) throws CmsException {
        return (m_driverManager.readAllPropertydefinitions(m_context, resourceType));
    }

    /**
     * Reads all property-definitions for the given resource type.
     *
     * @param resourcetype the name of the resource type to read the
     * property-definitions for.
     *
     * @return a Vector with property-defenitions for the resource type.
     * The Vector may be empty.
     *
     * @throws CmsException if operation was not successful.
     */
    public Vector readAllPropertydefinitions(String resourcetype) throws CmsException {
        return (m_driverManager.readAllPropertydefinitions(m_context, resourcetype));
    }

    /**
     * Reads a file from the Cms for history.
     * <br>
     * The reading includes the filecontent.
     *
     * @param filename the complete path of the file to be read.
     * @param versionId the version id of the resource
     *
     * @return file the read file.
     *
     * @throws CmsException , if the user has not the rights
     * to read the file, or if the file couldn't be read.
     */
    public CmsBackupResource readBackupFile(String filename, int versionId) throws CmsException {
        return (m_driverManager.readBackupFile(m_context, versionId, addSiteRoot(filename)));
    }

    /**
     * Reads a file header from the Cms for history.
     * <br>
     * The reading excludes the filecontent.
     *
     * @param filename the complete path of the file to be read.
     * @param versionId the version id of the resource
     *
     * @return file the read file.
     *
     * @throws CmsException , if the user has not the rights
     * to read the file headers, or if the file headers couldn't be read.
     */
    public CmsResource readBackupFileHeader(String filename, int versionId) throws CmsException {
        return (m_driverManager.readBackupFileHeader(m_context, versionId, addSiteRoot(filename)));
    }

    /**
     * Reads a project from the Cms.
     *
     * @param task the task for which the project will be read.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsBackupProject readBackupProject(int versionId) throws CmsException {
        return (m_driverManager.readBackupProject(m_context.currentUser(), m_context.currentProject(), versionId));
    }

    /**
     * Gets the Crontable.
     *
     * <B>Security:</B>
     * All users are garnted<BR/>
     *
     * @return the crontable.
     */
    public String readCronTable() throws CmsException {
        return m_driverManager.readCronTable();
    }

    /**
     * Reads the package path of the system.
     * This path is used for db-export and db-import and all module packages.
     *
     * @return the package path
     * @throws CmsException if operation was not successful
     */
    public String readPackagePath() throws CmsException {
        return m_driverManager.readPackagePath();
    }

    /**
     * Reads a file from the Cms.
     *
     * @param filename the complete path to the file.
     *
     * @return file the read file.
     *
     * @throws CmsException if the user has not the rights to read this resource,
     * or if the file couldn't be read.
     */
    public CmsFile readFile(String filename) throws CmsException {
        return (m_driverManager.readFile(m_context, addSiteRoot(filename)));
    }

    /**
     * Reads a file from the Cms.
     *
     * @param filename the complete path to the file.
     * @param includeDeleted If true the deleted file will be returned.
     *
     * @return file the read file.
     *
     * @throws CmsException if the user has not the rights to read this resource,
     * or if the file couldn't be read.
     */
    public CmsFile readFile(String filename, boolean includeDeleted) throws CmsException {
        return (m_driverManager.readFile(m_context, addSiteRoot(filename), includeDeleted));
    }

    /**
     * Reads a file from the Cms.
     *
     * @param folder the complete path to the folder from which the file will be read.
     * @param filename the name of the file to be read.
     *
     * @return file the read file.
     *
     * @throws CmsException , if the user has not the rights
     * to read this resource, or if the file couldn't be read.
     */
    public CmsFile readFile(String folder, String filename) throws CmsException {
        return (m_driverManager.readFile(m_context, addSiteRoot(folder + filename)));
    }

    /**
     * Gets the known file extensions (=suffixes).
     *
     *
     * @return a Hashtable with all known file extensions as Strings.
     *
     * @throws CmsException if operation was not successful.
     */
    public Hashtable readFileExtensions() throws CmsException {
        return m_driverManager.readFileExtensions();
    }

    /**
     * Reads a file header from the Cms.
     * <br>
     * The reading excludes the filecontent.
     *
     * @param filename the complete path of the file to be read.
     *
     * @return file the read file.
     *
     * @throws CmsException , if the user has not the rights
     * to read the file headers, or if the file headers couldn't be read.
     */
    public CmsResource readFileHeader(String filename) throws CmsException {
        return (m_driverManager.readFileHeader(m_context, addSiteRoot(filename)));
    }

    /**
     * Reads a file header from the Cms.
     * <br>
     * The reading excludes the filecontent.
     *
     * @param filename the complete path of the file to be read
     * @param includeDeleted if <code>true</code>, deleted files (in offline projects) will 
     * also be read
     *
     * @return file the read file header
     *
     * @throws CmsException if the user has not the rights
     * to read the file headers, or if the file headers couldn't be read
     */
    public CmsResource readFileHeader(String filename, boolean includeDeleted) throws CmsException {
        return (m_driverManager.readFileHeader(m_context, addSiteRoot(filename), includeDeleted));
    }

    /**
     * Reads a file header from the Cms.
     * <br>
     * The reading excludes the filecontent.
     *
     * @param filename the complete path of the file to be read.
     * @param projectId the id of the project where the resource should belong to
     *
     * @return file the read file.
     *
     * @throws CmsException , if the user has not the rights
     * to read the file headers, or if the file headers couldn't be read.
     */
    public CmsResource readFileHeader(String filename, int projectId, boolean includeDeleted) throws CmsException {
        return (m_driverManager.readFileHeaderInProject(m_context, projectId, addSiteRoot(filename), includeDeleted));
    }

    /**
     * Reads a file header from the Cms.
     * <br>
     * The reading excludes the filecontent.
     *
     * @param folder the complete path to the folder from which the file will be read.
     * @param filename the name of the file to be read.
     *
     * @return file the read file.
     *
     * @throws CmsException if the user has not the rights
     * to read the file header, or if the file header couldn't be read.
     */
    public CmsResource readFileHeader(String folder, String filename) throws CmsException {
        return (m_driverManager.readFileHeader(m_context, addSiteRoot(folder + filename)));
    }

    /**
     * Reads all file headers of a project from the Cms.
     *
     * @param projectId the id of the project to read the file headers for.
     *
     * @return a Vector of resources.
     *
     * @throws CmsException if the user has not the rights
     * to read the file headers, or if the file headers couldn't be read.
     */
    public Vector readFileHeaders(int projectId) throws CmsException {
        return (m_driverManager.readFileHeaders(m_context, projectId));
    }

    /**
     * Reads all files from the Cms, that are of the given type.<BR/>
     *
     * @param projectId A project id for reading online or offline resources
     * @param resourcetype The type of the files.
     *
     * @return A Vector of files.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Vector readFilesByType(int projectId, int resourcetype) throws CmsException {
        return m_driverManager.readFilesByType(m_context, projectId, resourcetype);
    }

    /**
     * Reads a folder from the Cms.
     *
     * @param folderid the id of the folder to be read
     * @param includeDeleted Include the folder if it is marked as deleted
     *
     * @return folder the read folder
     *
     * @throws CmsException if the user does not have the permissions
     * to read this folder, or if the folder couldn't be read
     */
    public CmsFolder readFolder(CmsUUID folderId, boolean includeDeleted) throws CmsException {
        return (m_driverManager.readFolder(m_context, folderId, includeDeleted));
    }

    /**
     * Reads a folder from the Cms.
     *
     * @param folder the complete path to the folder from which the folder will be read
     * @param foldername the name of the folder to be read
     *
     * @return The read folder
     *
     * @throws CmsException if the user does not have the permissions
     * to read this folder, or if the folder couldn't be read
     */
    public CmsFolder readFolder(String folderName) throws CmsException {
        return (m_driverManager.readFolder(m_context, addSiteRoot(folderName)));
    }

    /**
     * Reads a folder from the Cms.
     *
     * @param folderName the complete path to the folder to be read
     * @param includeDeleted Include the folder if it is marked as deleted
     *
     * @return The read folder 
     *
     * @throws CmsException If the user does not have the permissions
     * to read this folder, or if the folder couldn't be read
     */
    public CmsFolder readFolder(String folderName, boolean includeDeleted) throws CmsException {
        return (m_driverManager.readFolder(m_context, addSiteRoot(folderName), includeDeleted));
    }

    /**
      * Reads all given tasks from a user for a project.
      *
      * @param projectId the id of the project in which the tasks are defined.
      * @param owner the owner of the task.
      * @param tasktype the type of task you want to read: C_TASKS_ALL, C_TASKS_OPEN, C_TASKS_DONE, C_TASKS_NEW.
      * @param orderBy specifies how to order the tasks.
      *
      * @throws CmsException if operation was not successful.
      */
    public Vector readGivenTasks(int projectId, String ownerName, int taskType, String orderBy, String sort) throws CmsException {
        return (m_driverManager.readGivenTasks(m_context, projectId, ownerName, taskType, orderBy, sort));
    }

    /**
     * Reads the group of a project from the OpenCms.
     *
     * @return the group of the given project.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsGroup readGroup(CmsProject project) throws CmsException {
        return (m_driverManager.readGroup(m_context, project));
    }

    /**
     * Reads the group of a resource from the Cms.
     *
     * @return the group of a resource.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsGroup readGroup(CmsResource resource) throws CmsException {
        return (m_driverManager.readGroup(m_context, resource));
    }

    /**
     * Reads the group (role) of a task from the Cms.
     *
     * @param task the task to read the role from.
     * @return the group of the task.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsGroup readGroup(CmsTask task) throws CmsException {
        return (m_driverManager.readGroup(m_context, task));
    }

    /**
     * Reads a group of the Cms.
     *
     * @param groupid the id of the group to be returned.
     * @return a group in the Cms.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsGroup readGroup(CmsUUID groupId) throws CmsException {
        return (m_driverManager.readGroup(groupId));
    }

    /**
     * Reads a group of the Cms.
     *
     * @param groupname the name of the group to be returned.
     * @return a group in the Cms.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsGroup readGroup(String groupName) throws CmsException {
        return (m_driverManager.readGroup(m_context, groupName));
    }

    /**
     * Gets the Linkchecktable.
     *
     * <B>Security:</B>
     * All users are garnted<BR/>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     *
     * @return the linkchecktable.
     */
    public Hashtable readLinkCheckTable() throws CmsException {
        return m_driverManager.readLinkCheckTable();
    }

    /**
     * returns a Vector (Strings) with the link destinations of all links on the page with
     * the pageId.
     *
     * @param pageId The resourceId (offline) of the page whose liks should be read.
     */
    public Vector readLinkEntrys(CmsUUID pageId) throws CmsException {
        return m_driverManager.readLinkEntrys(pageId);
    }

    /**
     * Reads the managergroup of a project from the Cms.
     *
     * @return the managergroup of a project.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsGroup readManagerGroup(CmsProject project) throws CmsException {
        return (m_driverManager.readManagerGroup(project));
    }

    /**
     * Gets all Mime-Types known by the system.
     *
     * @return  a Hashtable containing all mime-types.
     *
     * @throws CmsException if operation was not successful.
     */
    public Hashtable readMimeTypes() throws CmsException {
        return m_driverManager.readMimeTypes();
    }

    /**
     * returns a Vector (Strings) with the link destinations of all links on the page with
     * the pageId.
     *
     * @param pageId The resourceId (online) of the page whose liks should be read.
     */
    public Vector readOnlineLinkEntrys(CmsUUID pageId) throws CmsException {
        return m_driverManager.readOnlineLinkEntrys(pageId);
    }

    /**
     * Reads the original agent of a task from the Cms.
     *
     * @param task the task to read the original agent from.
     * @return the owner of a task.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsUser readOriginalAgent(CmsTask task) throws CmsException {
        return (m_driverManager.readOriginalAgent(m_context, task));
    }
    /**
     * Reads the owner of a project from the Cms.
     *
     * @return the owner of the given project.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsUser readOwner(CmsProject project) throws CmsException {
        return (m_driverManager.readOwner(m_context, project));
    }

    /**
     * Reads the owner of a resource from the Cms.
     *
     * @return the owner of a resource.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsUser readOwner(CmsResource resource) throws CmsException {
        return (m_driverManager.readOwner(m_context, resource));
    }

    /**
     * Reads the owner (initiator) of a task from the Cms.
     *
     * @param tasktThe task to read the owner from.
     * @return the owner of a task.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsUser readOwner(CmsTask task) throws CmsException {
        return (m_driverManager.readOwner(m_context, task));
    }

    /**
     * Reads the owner of a tasklog from the Cms.
     *
     * @return the owner of a resource.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsUser readOwner(CmsTaskLog log) throws CmsException {
        return (m_driverManager.readOwner(m_context, log));
    }
    
    /**
     * Builds a list of resources for a given path.<p>
     * 
     * Use this method if you want to select a resource given by it's full filename and path. 
     * This is done by climbing down the path from the root folder using the parent-ID's and
     * resource names. Use this method with caution! Results are cached but reading path's 
     * inevitably increases runtime costs.
     * 
     * @param path the requested path
     * @param includeDeleted include resources that are marked as deleted
     * @return List of CmsResource's
     * @throws CmsException if something goes wrong
     */
    public List readPath(String path, boolean includeDeleted) throws CmsException {
        return (m_driverManager.readPath(m_context, m_context.addSiteRoot(path), includeDeleted));
    }

    /**
     * Reads a project from the Cms.
     *
     * @param id the id of the project to read.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsProject readProject(CmsResource res) throws CmsException {
        return (m_driverManager.readProject(m_context.currentUser(), m_context.currentProject(), res));
    }

    /**
     * Reads a project from the Cms.
     *
     * @param name the resource for which the project will be read.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsProject readProject(CmsTask task) throws CmsException {
        return (m_driverManager.readProject(m_context, task));
    }

    /**
     * Reads a project from the Cms.
     *
     * @param task the task for which the project will be read.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsProject readProject(int id) throws CmsException {
        return (m_driverManager.readProject(m_context, id));
    }

    /**
      * Reads log entries for a project.
      *
      * @param projectId the id of the project for which the tasklog will be read.
      * @return a Vector of new TaskLog objects
      * @throws CmsException if operation was not successful.
      */
    public Vector readProjectLogs(int projectId) throws CmsException {
        return m_driverManager.readProjectLogs(m_context.currentUser(), m_context.currentProject(), projectId);
    }

    /**
     * Reads all file headers of a project from the Cms.
     *
     * @param projectId the id of the project to read the file headers for.
     * @param filter The filter for the resources (all, new, changed, deleted, locked)
     *
     * @return a Vector (of CmsResources objects) of resources.
     *
     */
    public Vector readProjectView(int projectId, String filter) throws CmsException {
        return m_driverManager.readProjectView(m_context, projectId, filter);
    }

    /**
     * Looks up all properties for a resource.<p>
     * 
     * @param resource the resource to look up the property for
     * @return Map of Strings representing all properties of the resource
     * @throws CmsException in case there where problems reading the properties
     */
    public Map readProperties(String resource) throws CmsException {
        return m_driverManager.readProperties(m_context, m_context.addSiteRoot(resource), m_context.getAdjustedSiteRoot(resource), false);
    }

    /**
     * Looks up all properties for a resource with optional direcory upward cascading.<p>
     * 
     * @param resource the resource to look up the property for
     * @param search if <code>true</code>, the properties will also be looked up on all parent folders
     *   and the results will be merged, if <code>false</code> not (ie. normal property lookup)
     * @return Map of Strings representing all properties of the resource
     * @throws CmsException in case there where problems reading the properties
     */
    public Map readProperties(String resource, boolean search) throws CmsException {
        return m_driverManager.readProperties(m_context, m_context.addSiteRoot(resource), m_context.getAdjustedSiteRoot(resource), search);
    }

    /**
     * Looks up a specified property from a resource.<p>
     *
     * @param resource the resource to look up the property for
     * @param property the name of the property to look up
     * @return the value of the property found, <code>null</code> if nothing was found
     * @throws CmsException in case there where problems reading the property
     */
    public String readProperty(String resource, String property) throws CmsException {
        return m_driverManager.readProperty(m_context, m_context.addSiteRoot(resource), m_context.getAdjustedSiteRoot(resource), property, false);
    }

    /**
     * Looks up a specified property with optional direcory upward cascading.<p>
     * 
     * @param resource the resource to look up the property for
     * @param property the name of the property to look up
     * @param search if <code>true</code>, the property will be looked up on all parent folders
     *   if it is not attached to the the resource, if false not (ie. normal 
     *   property lookup)
     * @return the value of the property found, <code>null</code> if nothing was found
     * @throws CmsException in case there where problems reading the property
     */
    public String readProperty(String resource, String property, boolean search) throws CmsException {
        return m_driverManager.readProperty(m_context, m_context.addSiteRoot(resource), m_context.getAdjustedSiteRoot(resource), property, search);
    }

    /**
     * Looks up a specified property with optional direcory upward cascading,
     * a default value will be returned if the property is not found on the
     * resource (or it's parent folders in case search is set to <code>true</code>).<p>
     * 
     * @param resource the resource to look up the property for
     * @param property the name of the property to look up
     * @param search if <code>true</code>, the property will be looked up on all parent folders
     *   if it is not attached to the the resource, if <code>false</code> not (ie. normal 
     *   property lookup)
     * @param propertyDefault a default value that will be returned if
     *   the property was not found on the selected resource
     * @return the value of the property found, if nothing was found the value of the <code>propertyDefault</code> parameter is returned
     * @throws CmsException in case there where problems reading the property
     */
    public String readProperty(String resource, String property, boolean search, String propertyDefault) throws CmsException {
        return m_driverManager.readProperty(m_context, m_context.addSiteRoot(resource), m_context.getAdjustedSiteRoot(resource), property, search, propertyDefault);
    }

    /**
     * Reads the property-definition for the resource type.
     *
     * @param name the name of the property-definition to read.
     * @param resourcetype the name of the resource type for the property-definition.
     * @return the property-definition.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsPropertydefinition readPropertydefinition(String name, int resourcetype) throws CmsException {
        return (m_driverManager.readPropertydefinition(m_context, name, resourcetype));
    }

    /**
     * Returns a Vector with the resources that contains the given part in the resourcename.<br>
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can read and view this resource</li>
     * </ul>
     *
     * @param resourcename A part of resourcename
     *
     * @return subfolders A Vector with resources.
     *
     * @throws CmsException  Throws CmsException if operation was not succesful.
     */
    public Vector readResourcesLikeName(String resourcename) throws CmsException {
        return m_driverManager.readResourcesLikeName(m_context, resourcename);
    }

    /**
     * Reads the task with the given id.
     *
     * @param id the id of the task to be read.
     *
     * @throws CmsException if operation was not successful.
     */
    public CmsTask readTask(int id) throws CmsException {
        return (m_driverManager.readTask(m_context.currentUser(), m_context.currentProject(), id));
    }

    /**
     * Reads log entries for a task.
     *
     * @param taskid the task for which the tasklog will be read.
     * @return a Vector of new TaskLog objects.
     * @throws CmsException if operation was not successful.
     */
    public Vector readTaskLogs(int taskid) throws CmsException {
        return m_driverManager.readTaskLogs(m_context.currentUser(), m_context.currentProject(), taskid);
    }

    /**
     * Reads all tasks for a project.
     *
     * @param projectId the id of the project in which the tasks are defined. Can be null to select all tasks.
     * @param tasktype the type of task you want to read: C_TASKS_ALL, C_TASKS_OPEN, C_TASKS_DONE, C_TASKS_NEW
     * @param orderBy specifies how to order the tasks.
     * @param sort sort order: C_SORT_ASC, C_SORT_DESC, or null.
     *
     * @throws CmsException if operation was not successful.
     */
    public Vector readTasksForProject(int projectId, int tasktype, String orderBy, String sort) throws CmsException {
        return (m_driverManager.readTasksForProject(projectId, tasktype, orderBy, sort));
    }

    /**
     * Reads all tasks for a role in a project.
     *
     * @param projectId the id of the Project in which the tasks are defined.
     * @param user the user who has to process the task.
     * @param tasktype the type of task you want to read: C_TASKS_ALL, C_TASKS_OPEN, C_TASKS_DONE, C_TASKS_NEW.
     * @param orderBy specifies how to order the tasks.
     * @param sort sort order C_SORT_ASC, C_SORT_DESC, or null
     * @throws CmsException if operation was not successful.
     */
    public Vector readTasksForRole(int projectId, String roleName, int tasktype, String orderBy, String sort) throws CmsException {
        return (m_driverManager.readTasksForRole(m_context, projectId, roleName, tasktype, orderBy, sort));
    }

    /**
     * Reads all tasks for a user in a project.
     *
     * @param projectId the id of the Project in which the tasks are defined.
     * @param role the user who has to process the task.
     * @param tasktype the type of task you want to read: C_TASKS_ALL, C_TASKS_OPEN, C_TASKS_DONE, C_TASKS_NEW.
     * @param orderBy specifies how to order the tasks.
     * @param sort sort order C_SORT_ASC, C_SORT_DESC, or null
     * @throws CmsException if operation was not successful.
     */
    public Vector readTasksForUser(int projectId, String userName, int tasktype, String orderBy, String sort) throws CmsException {
        return (m_driverManager.readTasksForUser(m_context, projectId, userName, tasktype, orderBy, sort));
    }

    /**
     * Returns a user in the Cms.
     *
     * @param id the id of the user to be returned.
     * @return a user in the Cms.
     *
     * @throws CmsException if operation was not successful
     */
    public CmsUser readUser(CmsUUID userId) throws CmsException {
        return (m_driverManager.readUser(userId));
    }

    /**
     * Returns a user in the Cms.
     *
     * @param username the name of the user to be returned.
     * @return a user in the Cms.
     *
     * @throws CmsException if operation was not successful
     */
    public CmsUser readUser(String username) throws CmsException {
        return m_driverManager.readUser(username);
    }

    /**
     * Returns a user in the Cms.
     *
     * @param username the name of the user to be returned.
     * @param type the type of the user.
     * @return a user in the Cms.
     *
     * @throws CmsException if operation was not successful
     */
    public CmsUser readUser(String username, int type) throws CmsException {
        return (m_driverManager.readUser(username, type));
    }

    /**
     * Returns a user in the Cms, if the password is correct.
     *
     * @param username the name of the user to be returned.
     * @param password the password of the user to be returned.
     * @return a user in the Cms.
     *
     * @throws CmsException if operation was not successful
     */
    public CmsUser readUser(String username, String password) throws CmsException {
        return (m_driverManager.readUser(username, password));
    }

    /**
     * Returns a user object if the password for the user is correct.<P/>
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param username The username of the user that is to be read.
     * @return User
     *
     * @throws CmsException  Throws CmsException if operation was not succesful
    */
    public CmsUser readWebUser(String username) throws CmsException {
        return (m_driverManager.readWebUser(username));
    }

    /**
     * Returns a user object if the password for the user is correct.<P/>
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param username The username of the user that is to be read.
     * @param password The password of the user that is to be read.
     * @return User
     *
     * @throws CmsException  Throws CmsException if operation was not succesful
     */
    public CmsUser readWebUser(String username, String password) throws CmsException {
        return (m_driverManager.readWebUser(username, password));
    }
    /**
     * Reactivates a task from the Cms.
     *
     * @param taskid the Id of the task to accept.
     *
     * @throws CmsException if operation was not successful.
     */
    public void reaktivateTask(int taskId) throws CmsException {
        m_driverManager.reaktivateTask(m_context, taskId);
    }

    /**
     * Sets a new password if the user knows his recovery-password.
     *
     * @param username the name of the user.
     * @param recoveryPassword the recovery password.
     * @param newPassword the new password.
     *
     * @throws CmsException if operation was not successfull.
     */
    public void recoverPassword(String username, String recoveryPassword, String newPassword) throws CmsException {
        m_driverManager.recoverPassword(this, m_context, username, recoveryPassword, newPassword);
    }

    /**
     * Removes the current site root prefix from the absolute path in the resource name,
     * i.e. adjusts the resource name for the current site root.<p> 
     * 
     * @param resourcename the resource name
     * @return the resource name adjusted for the current site root
     */
    private String removeSiteRoot(String resourcename) {
        return getRequestContext().removeSiteRoot(resourcename);
    }

    /**
     * Removes a user from a group.
     *
     * <p>
     * <b>Security:</b>
     * Only the admin user is allowed to remove a user from a group.
     *
     * @param username the name of the user that is to be removed from the group.
     * @param groupname the name of the group.
     * @throws CmsException if operation was not successful.
     */
    public void removeUserFromGroup(String username, String groupname) throws CmsException {
        m_driverManager.removeUserFromGroup(m_context, username, groupname);
    }

    /**
     * Renames the file to the new name.
     *
     * @param oldname the complete path to the file which will be renamed.
     * @param newname the new name of the file.
     *
     * @throws CmsException if the user has not the rights
     * to rename the file, or if the file couldn't be renamed.
     *
     * @deprecated Use renameResource instead.
     */
    public void renameFile(String oldname, String newname) throws CmsException {
        renameResource(oldname, newname);
    }

    /**
     * Renames the resource to the new name.
     *
     * @param oldname the complete path to the file which will be renamed.
     * @param newname the new name of the file.
     *
     * @throws CmsException if the user has not the rights
     * to rename the file, or if the file couldn't be renamed.
     */
    public void renameResource(String oldname, String newname) throws CmsException {
        getResourceType(readFileHeader(oldname).getType()).renameResource(this, oldname, newname);
    }

    /**
     * Replaces and existing resource by another file with different content
     * and different file type.
     * 
     * @param filename the resource to replace
     * @param type the type of the new resource
     * @param content the content of the new resource
     */
    public void replaceResource(String resName, int type, Map properties, byte[] content) throws CmsException {
        // read the properties of the existing file
        Map resProps = null;
        try {
            resProps = readAllProperties(resName);
        } catch (CmsException e) {
            resProps = (Map)new HashMap();
        }

        // add the properties that might have been collected during a file-upload
        if (properties != null) {
            resProps.putAll(properties);
        }

        getResourceType(readFileHeader(resName, true).getType()).replaceResource(this, resName, resProps, content, type);
    }

    /**
     * Restores a file in the current project with a version in the backup
     *
     * @param versionId The version id of the resource
     * @param filename The name of the file to restore
     *
     * @throws CmsException  Throws CmsException if operation was not succesful.
     */
    public void restoreResource(int versionId, String filename) throws CmsException {
        getResourceType(readFileHeader(filename).getType()).restoreResource(this, versionId, filename);
    }

    /**
     * Removes an access control entry of a griven principal from a given resource.
     * 
     * @param resourceName		name of the resource
     * @param principalType		the type of the principal (currently group or user)
     * @param principalName		name of the principal
     * @throws CmsException		if something goes wrong
     */
    public void rmacc(String resourceName, String principalType, String principalName) throws CmsException {

        CmsResource res = readFileHeader(resourceName);
        I_CmsPrincipal principal = null;

        if ("group".equals(principalType.toLowerCase())) {
            principal = readGroup(principalName);
        } else if ("user".equals(principalType.toLowerCase())) {
            principal = readUser(principalName);
        }

        m_driverManager.removeAccessControlEntry(m_context, res, principal.getId());
    }

    /**
     * Returns the root-folder object.
     *
     * @return the root-folder object.
     * @throws CmsException if operation was not successful.
     */
    public CmsFolder rootFolder() throws CmsException {
        return (readFolder(I_CmsConstants.C_ROOT));
    }

    /**
     * Returns a list of all currently logged in users.
     * This method is only allowed for administrators.
     */
    public void sendBroadcastMessage(String message) throws CmsException {
        if (isAdmin()) {
            if (m_sessionStorage != null) {
                m_sessionStorage.sendBroadcastMessage(message);
            }
        } else {
            throw new CmsSecurityException("[" + this.getClass().getName() + "] sendBroadcastMessage()", CmsSecurityException.C_SECURITY_ADMIN_PRIVILEGES_REQUIRED);
        }
    }

    /**
     * Sets the name of the current site root of the content objects system
     */
    public void setContextToCos() {
        getRequestContext().setSiteRoot(I_CmsConstants.VFS_FOLDER_COS);
    }

    /**
     * Sets the mode this CmsObject runs in. Used for static export.
     * @param mode the mode to set
     */
    public void setMode(int mode) {
        m_mode = mode;
    }
    /**
     * Set a new name for a task.
     *
     * @param taskid the id of the task.
     * @param name the new name of the task.
     *
     * @throws CmsException if operationwas not successful.
     */
    public void setName(int taskId, String name) throws CmsException {
        m_driverManager.setName(m_context, taskId, name);
    }

    /**
     * Sets a new parent-group for an already existing group in the Cms.
     *
     * @param groupName the name of the group that should be written to the Cms.
     * @param parentGroupName the name of the parentGroup to set, or null if the parent
     * group should be deleted.
     * @throws CmsException  if operation was not successfull.
     */
    public void setParentGroup(String groupName, String parentGroupName) throws CmsException {
        m_driverManager.setParentGroup(m_context, groupName, parentGroupName);
    }

    /**
     * Sets the password for a user.
     *
     * @param username the name of the user.
     * @param newPassword the new password.
     *
     * @throws CmsException if operation was not successful.
     */
    public void setPassword(String username, String newPassword) throws CmsException {
        m_driverManager.setPassword(this, m_context, username, newPassword);
    }

    /**
     * Sets the password for a user.
     *
     * @param username the name of the user.
     * @param oldPassword the old password.
     * @param newPassword the new password.
     *
     * @throws CmsException if operation was not successful.
     */
    public void setPassword(String username, String oldPassword, String newPassword) throws CmsException {
        m_driverManager.setPassword(this, m_context, username, oldPassword, newPassword);
    }

    /**
     * Sets the priority of a task.
     *
     * @param taskid the id of the task.
     * @param priority the new priority value.
     *
     * @throws CmsException if operation was not successful.
     */
    public void setPriority(int taskId, int priority) throws CmsException {
        m_driverManager.setPriority(m_context, taskId, priority);
    }

    /**
     * Sets the recovery password for a user.
     *
     * @param username the name of the user.
     * @param password the password.
     * @param newPassword the new recovery password.
     *
     * @throws CmsException if operation was not successful.
     */
    public void setRecoveryPassword(String username, String oldPassword, String newPassword) throws CmsException {
        m_driverManager.setRecoveryPassword(this, m_context.currentUser(), m_context.currentProject(), username, oldPassword, newPassword);
    }

    /**
     * Set a parameter for a task.
     *
     * @param taskid the Id of the task.
     * @param parname the ame of the parameter.
     * @param parvalue the value of the parameter.
     *
     * @return the id of the inserted parameter or 0 if the parameter already exists for this task.
     *
     * @throws CmsException if operation was not successful.
     */
    public void setTaskPar(int taskid, String parname, String parvalue) throws CmsException {
        m_driverManager.setTaskPar(m_context, taskid, parname, parvalue);
    }

    /**
     * Sets the timeout of a task.
     *
     * @param taskid the id of the task.
     * @param timeout the new timeout value.
     *
     * @throws CmsException if operation was not successful.
     */
    public void setTimeout(int taskId, long timeout) throws CmsException {
        m_driverManager.setTimeout(m_context, taskId, timeout);
    }

    /**
     * Synchronize cms-resources on virtual filesystem with the server filesystem.
     *
     * @param syncFile the name (absolute Path) of the resource that should be synchronized.
     * @param syncPath the name of path on server filesystem where the resource should be synchronized.
     *
     * @throws CmsException if operation was not successful.
     */
    public void syncFolder(String resourceName) throws CmsException {
        // synchronize the resources  
        new CmsSynchronize(this, resourceName);
    }


    /**
     * Change the timestamp of a resource.
     * 
     * @param resourceName the name of the resource to change
     * @param timestamp timestamp the new timestamp of the changed resource
     * @param boolean flag to touch recursively all sub-resources in case of a folder
     * @param user the user who is inserted as userladtmodified 
     */
    public void touch(String resourceName, long timestamp, boolean touchRecursive, CmsUUID user) throws CmsException {
        getResourceType(readFileHeader(resourceName).getType()).touch(this, resourceName, timestamp, touchRecursive, user);
    }

    /**
     * Change the timestamp of a resource.
     * 
     * @param resourceName the name of the resource to change
     * @param timestamp timestamp the new timestamp of the changed resource
     * @param boolean flag to touch recursively all sub-resources in case of a folder
     */
    public void touch(String resourceName, long timestamp, boolean touchRecursive) throws CmsException {
        touch(resourceName,timestamp,touchRecursive,getRequestContext().currentUser().getId());
    }

    /**
     * Undeletes a resource.
     *
     * @param filename the complete path of the file.
     *
     * @throws CmsException if the file couldn't be undeleted, or if the user
     * has not the appropriate rights to undelete the file.
     */
    public void undeleteResource(String filename) throws CmsException {
        //read the file header including deleted
        getResourceType(readFileHeader(filename, true).getType()).undeleteResource(this, filename);
    }

    /**
     * Undo changes in a file by copying the online file.
     *
     * @param filename the complete path of the file.
     *
     * @throws CmsException if the file couldn't be deleted, or if the user
     * has not the appropriate rights to write the file.
     */
    public void undoChanges(String filename) throws CmsException {
        //read the file header including deleted
        getResourceType(readFileHeader(filename, true).getType()).undoChanges(this, filename);
    }

    /**
     * Unlocks all resources of a project.
     *
     * @param id the id of the project to be unlocked.
     *
     * @throws CmsException if operation was not successful.
     */
    public void unlockProject(int id) throws CmsException {
        m_driverManager.unlockProject(m_context, id);
    }

    /**
     * Unlocks a resource.
     * <br>
     * A user can unlock a resource, so other users may lock this file.
     *
     * @param resource the complete path to the resource to be unlocked.
     *
     * @throws CmsException if the user has not the rights
     * to unlock this resource.
     */
    public void unlockResource(String resource, boolean forceRecursive) throws CmsException {
        getResourceType(readFileHeader(resource,true).getType()).unlockResource(this, resource, forceRecursive);
    }

    /**
     * When a project is published this method aktualises the online link table.
     *
     * @param projectId of the project that is published.
     */
    public void updateOnlineProjectLinks(Vector deleted, Vector changed, Vector newRes, int pageType) throws CmsException {
        m_driverManager.updateOnlineProjectLinks(deleted, changed, newRes, pageType);
    }


    /**
     * Tests, if a user is member of the given group.
     *
     * @param username the name of the user to test.
     * @param groupname the name of the group to test.
     * @return <code>true</code>, if the user is in the group; <code>else</code> false otherwise.
     *
     * @throws CmsException if operation was not successful.
     */
    public boolean userInGroup(String username, String groupname) throws CmsException {
        return (m_driverManager.userInGroup(m_context, username, groupname));
    }

    /**
     * Writes access control entries for a given resource
     * 
     * @param resourceName		the name of the resource
     * @param acEntries			a vector of access control entries
     * @throws CmsException		if something goes wrong
     */
    public void importAccessControlEntries(CmsResource resource, Vector acEntries) throws CmsException {
        // TODO: force the access control entries to belong to the given resource
        m_driverManager.importAccessControlEntries(m_context, resource, acEntries);
    }

    /**
     * Writes the Crontable.
     *
     * <B>Security:</B>
     * Only a administrator can do this<BR/>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     *
     * @return the crontable.
     */
    public void writeCronTable(String crontable) throws CmsException {
        m_driverManager.writeCronTable(m_context, crontable);
    }

    /**
     * Writes the package for the system.<p>
     * 
     * This path is used for db-export and db-import as well as module packages.<p>
     *
     * @param path the package path
     * @throws CmsException if operation ws not successful
     */
    public void writePackagePath(String path) throws CmsException {
        m_driverManager.writePackagePath(m_context, path);
    }

    /**
     * Writes a file to the Cms.
     *
     * @param file the file to write.
     *
     * @throws CmsException if resourcetype is set to folder. The CmsException will also be thrown,
     * if the user has not the rights write the file.
     */
    public void writeFile(CmsFile file) throws CmsException {
        m_driverManager.writeFile(m_context, file);
    }

    /**
     * Writes the file extensions.
     * <p>
     * <B>Security:</B>
     * Only the admin user is allowed to write file extensions.
     *
     * @param extensions holds extensions as keys and resourcetypes (Strings) as values.
     */

    public void writeFileExtensions(Hashtable extensions) throws CmsException {
        m_driverManager.writeFileExtensions(m_context, extensions);
    }

    /**
     * Writes a file-header to the Cms.
     *
     * @param file the file to write.
     *
     * @throws CmsException if resourcetype is set to folder. The CmsException will also be thrown,
     * if the user has not the rights to write the file header..
     */
    public void writeFileHeader(CmsFile file) throws CmsException {
        m_driverManager.writeFileHeader(m_context, file);
    }

    /**
     * Writes an already existing group to the Cms.
     *
     * @param group the group that should be written to the Cms.
     * @throws CmsException  if operation was not successful.
     */
    public void writeGroup(CmsGroup group) throws CmsException {
        m_driverManager.writeGroup(m_context, group);
    }

    /**
     * Writes the Linkchecktable.
     *
     * <B>Security:</B>
     * Only a administrator can do this<BR/>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param linkchecktable The hashtable that contains the links that were not reachable
     *
     * @return the linkchecktable.
     */
    public void writeLinkCheckTable(Hashtable linkchecktable) throws CmsException {
        m_driverManager.writeLinkCheckTable(m_context, linkchecktable);
    }

    /**
     * Writes a couple of Properties for a file or folder.
     *
     * @param name the resource-name of which the Property has to be set.
     * @param properties a Hashtable with property-definitions and property values as Strings.
     *
     * @throws CmsException if operation was not successful.
     */
    public void writeProperties(String name, Map properties) throws CmsException {
        m_driverManager.writeProperties(m_context, addSiteRoot(name), properties);
    }

    /**
     * Writes a property for a file or folder.
     *
     * @param name the resource-name for which the property will be set.
     * @param property the property-definition name.
     * @param value the value for the property to be set.
     *
     * @throws CmsException if operation was not successful.
     */
    public void writeProperty(String name, String property, String value) throws CmsException {
        m_driverManager.writeProperty(m_context, addSiteRoot(name), property, value);
    }

    /**
     * Writes the property-definition for the resource type.
     *
     * @param propertydef the property-definition to be written.
     *
     * @throws CmsException if operation was not successful.
     * @deprecated Do not use this method any longer because there is no type of propertydefinition
     */
    public CmsPropertydefinition writePropertydefinition(CmsPropertydefinition definition) throws CmsException {
        return readPropertydefinition(definition.getName(), definition.getType());
    }

    /**
     * Writes a new user tasklog for a task.
     *
     * @param taskid the Id of the task.
     * @param comment the description for the log.
     *
     * @throws CmsException if operation was not successful.
     */
    public void writeTaskLog(int taskid, String comment) throws CmsException {
        m_driverManager.writeTaskLog(m_context, taskid, comment);
    }

    /**
     * Writes a new user tasklog for a task.
     *
     * @param taskid the Id of the task .
     * @param comment the description for the log
     * @param tasktype the type of the tasklog. User tasktypes must be greater than 100.
     *
     * @throws CmsException if operation was not successful.
     */
    public void writeTaskLog(int taskid, String comment, int taskType) throws CmsException {
        m_driverManager.writeTaskLog(m_context, taskid, comment, taskType);
    }

    /**
     * Updates the user information.
     * <p>
     * <b>Security:</b>
     * Only the admin user is allowed to update the user information.
     *
     * @param user the user to be written.
     *
     * @throws CmsException if operation was not successful.
     */
    public void writeUser(CmsUser user) throws CmsException {
        m_driverManager.writeUser(m_context, user);
    }

    /**
     * Updates the user information of a web user.
     * <br>
     * Only a web user can be updated this way.
     *
     * @param user the user to be written.
     *
     * @throws CmsException if operation was not successful.
     */
    public void writeWebUser(CmsUser user) throws CmsException {
        m_driverManager.writeWebUser(m_context, user);
    }
    
    /**
     * Gets the lock state for a specified resource.<p>
     * 
     * @param resource the specified resource
     * @return the CmsLock object for the specified resource
     * @throws CmsException if somethong goes wrong
     */
    public CmsLock getLock(CmsResource resource) throws CmsException {
        return m_driverManager.getLock(m_context, resource);
    }    
    
    /**
     * Gets the lock state for a specified resource name.<p>
     * 
     * @param resourcename the specified resource name
     * @return the CmsLock object for the specified resource name
     * @throws CmsException if somethong goes wrong
     */
    public CmsLock getLock(String resourcename) throws CmsException {
        return m_driverManager.getLock(m_context, m_context.addSiteRoot(resourcename));
    }
    
    /**
     * Proves if a specified resource is inside the current project.<p>
     * 
     * @param resource the specified resource
     * @return true, if the resource name of the specified resource matches any of the current project's resources
     * @throws CmsException if somethong goes wrong
     */
    public boolean isInsideCurrentProject(CmsResource resource) {
        return m_driverManager.isInsideCurrentProject(m_context, resource);
    }      
    
    /**
     * Returns the list of all resources that define the "view" of the given project.<p>
     * 
     * @param project the project to get the project resources for
     * @return the list of all resources that define the "view" of the given project
     * @throws CmsException if something goes wrong
     */
    public List readProjectResources(CmsProject project) throws CmsException {
        return m_driverManager.readProjectResources(m_context, project);
    }
        
    /**
     * Recovers a resource from the online project back to the offline project as an unchanged resource.<p>
     * 
     * @param resourcename the name of the resource which is recovered
     * @return the recovered resource in the offline project
     * @throws CmsException if somethong goes wrong
     */
    public CmsResource recoverResource(String resourcename) throws CmsException {
        return m_driverManager.recoverResource(m_context, m_context.addSiteRoot(resourcename));        
    }

}
