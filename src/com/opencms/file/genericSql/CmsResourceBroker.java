/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/genericSql/Attic/CmsResourceBroker.java,v $
* Date   : $Date: 2002/03/21 08:46:20 $
* Version: $Revision: 1.311 $
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

package com.opencms.file.genericSql;

import javax.servlet.http.*;
import java.util.*;
import java.net.*;
import java.io.*;
import source.org.apache.java.io.*;
import source.org.apache.java.util.*;
import com.opencms.boot.CmsClassLoader;
import com.opencms.boot.CmsBase;
import com.opencms.core.*;
import com.opencms.file.*;
import com.opencms.template.*;
import java.sql.SQLException;
import java.util.zip.*;
import org.w3c.dom.*;


/**
 * This is THE resource broker. It merges all resource broker
 * into one public class. The interface is local to package. <B>All</B> methods
 * get additional parameters (callingUser and currentproject) to check the security-
 * police.
 *
 * @author Andreas Schouten
 * @author Michaela Schleich
 * @author Michael Emmerich
 * @author Anders Fugmann
 * @version $Revision: 1.311 $ $Date: 2002/03/21 08:46:20 $
 *
 */
public class CmsResourceBroker implements I_CmsResourceBroker, I_CmsConstants {

    //create a compare class to be used in the vector.
    class Resource {
        private String path = null;
        public Resource(String path) {
            this.path = path;
        }
        public boolean equals(Object obj) {
            return ( (obj instanceof CmsResource) && path.equals( ((CmsResource) obj).getResourceName() ));
        }
    }

    /**
     * Constant to count the file-system changes.
     */
    protected long m_fileSystemChanges = 0;

    /**
     * Constant to count the file-system changes if Folders are involved.
     */
    protected long m_fileSystemFolderChanges = 0;

    /**
     * Hashtable with resource-types.
     */
    protected Hashtable m_resourceTypes = null;


    /**
     * The configuration of the property-file.
     */
    protected Configurations m_configuration = null;

    /**
     * The access-module.
     */
    protected CmsDbAccess m_dbAccess = null;

    /**
    * The Registry
    */
    protected I_CmsRegistry m_registry = null;

    /**
     * The portnumber the workplace access is limited to.
     */
    protected int m_limitedWorkplacePort = -1;

    /**
     *  Define the caches
    */
    protected CmsCache m_userCache = null;
    protected CmsCache m_groupCache = null;
    protected CmsCache m_usergroupsCache = null;
    protected CmsCache m_projectCache = null;
    protected CmsProject m_onlineProjectCache = null;
    protected CmsCache m_propertyCache = null;
    protected CmsCache m_propertyDefCache = null;
    protected CmsCache m_propertyDefVectorCache = null;
    protected CmsCache m_accessCache = null;
    protected int m_cachelimit = 0;
    protected String m_refresh = null;

    // cache for single resources
    protected CmsResourceCache m_resourceCache = null;
    // cache for resource lists
    protected CmsResourceCache m_subresCache = null; // resources in folder

    /**
    * backup published resources for history
    */
    protected boolean m_enableHistory = true;

/**
 * Accept a task from the Cms.
 *
 * <B>Security:</B>
 * All users are granted.
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param taskid The Id of the task to accept.
 *
 * @exception CmsException Throws CmsException if something goes wrong.
 */
public void acceptTask(CmsUser currentUser, CmsProject currentProject, int taskId) throws CmsException
{
    CmsTask task = m_dbAccess.readTask(taskId);
    task.setPercentage(1);
    task = m_dbAccess.writeTask(task);
    m_dbAccess.writeSystemTaskLog(taskId, "Task was accepted from " + currentUser.getFirstname() + " " + currentUser.getLastname() + ".");
}
    /**
     * Checks, if the user may create this resource.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The resource to check.
     *
     * @return wether the user has access, or not.
     */
    public boolean accessCreate(CmsUser currentUser, CmsProject currentProject,
                                CmsResource resource) throws CmsException {

        // check, if this is the onlineproject
        if(onlineProject(currentUser, currentProject).equals(currentProject)){
            // the online-project is not writeable!
            return(false);
        }

        // check the access to the project
        if( ! accessProject(currentUser, currentProject, currentProject.getId()) ) {
            // no access to the project!
            return(false);
        }

        // check if the resource belongs to the current project
        if(resource.getProjectId() != currentProject.getId()) {
            return false;
        }
        // is the resource locked?
        if( resource.isLocked() && (resource.isLockedBy() != currentUser.getId() ||
            (resource.getLockedInProject() != currentProject.getId() &&
            currentProject.getFlags() != C_PROJECT_STATE_INVISIBLE)) ) {
            // resource locked by anopther user, no creation allowed
            return(false);
        }

         // check the rights for the current resource
        if( ! ( accessOther(currentUser, currentProject, resource, C_ACCESS_PUBLIC_WRITE) ||
                accessOwner(currentUser, currentProject, resource, C_ACCESS_OWNER_WRITE) ||
                accessGroup(currentUser, currentProject, resource, C_ACCESS_GROUP_WRITE) ) ) {
            // no write access to this resource!
            return false;
        }

        // read the parent folder
        if(resource.getParent() != null) {
            // readFolder without checking access
            resource = m_dbAccess.readFolder(resource.getProjectId(), resource.getRootName()+resource.getParent());
        } else {
            // no parent folder!
            return true;
        }

        // check the rights and if the resource is not locked
        do {
            if( accessOther(currentUser, currentProject, resource, C_ACCESS_PUBLIC_READ) ||
                accessOwner(currentUser, currentProject, resource, C_ACCESS_OWNER_READ) ||
                accessGroup(currentUser, currentProject, resource, C_ACCESS_GROUP_READ) ) {

                // is the resource locked?
                if( resource.isLocked() && resource.isLockedBy() != currentUser.getId() ) {
                    // resource locked by anopther user, no creation allowed
                    return(false);
                }

                // read next resource
                if(resource.getParent() != null) {
                    // readFolder without checking access
                    resource = m_dbAccess.readFolder(resource.getProjectId(),  resource.getRootName()+resource.getParent());
                }
            } else {
                // last check was negative
                return(false);
            }
        } while(resource.getParent() != null);

        // all checks are done positive
        return(true);
    }
    /**
     * Checks, if the user may create this resource.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The resource to check.
     *
     * @return wether the user has access, or not.
     */
    public boolean accessCreate(CmsUser currentUser, CmsProject currentProject,
                                String resourceName) throws CmsException {

        CmsResource resource = m_dbAccess.readFileHeader(currentProject.getId(), resourceName);
        return accessCreate(currentUser, currentProject, resource);
    }
    /**
     * Checks, if the group may access this resource.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The resource to check.
     * @param flags The flags to check.
     *
     * @return wether the user has access, or not.
     */
    protected boolean accessGroup(CmsUser currentUser, CmsProject currentProject,
                                CmsResource resource, int flags)
        throws CmsException {

        // is the user in the group for the resource?
        if(userInGroup(currentUser, currentProject, currentUser.getName(),
                       readGroup(currentUser, currentProject,
                                 resource).getName())) {
            if( (resource.getAccessFlags() & flags) == flags ) {
                return true;
            }
        }
        // the resource isn't accesible by the user.

        return false;

    }
    /**
     * Checks, if the user may lock this resource.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The resource to check.
     *
     * @return wether the user may lock this resource, or not.
     */
    public boolean accessLock(CmsUser currentUser, CmsProject currentProject,
                              CmsResource resource) throws CmsException {
        // check, if this is the onlineproject
        if(onlineProject(currentUser, currentProject).equals(currentProject)){
            // the online-project is not writeable!
            return(false);
        }

        // check the access to the project
        if( ! accessProject(currentUser, currentProject, currentProject.getId()) ) {
            // no access to the project!
            return(false);
        }

        // check if the resource belongs to the current project
        if(resource.getProjectId() != currentProject.getId()) {
            return false;
        }

        // read the parent folder
        if(resource.getParent() != null) {
            // readFolder without checking access
            resource = m_dbAccess.readFolder(resource.getProjectId(), resource.getRootName()+resource.getParent());
        } else {
            // no parent folder!
            return true;
        }

        // check the rights and if the resource is not locked
        do {
            // is the resource locked?
            if( resource.isLocked() && (resource.isLockedBy() != currentUser.getId() ||
                (resource.getLockedInProject() != currentProject.getId() &&
                 currentProject.getFlags() != C_PROJECT_STATE_INVISIBLE)) ) {
                // resource locked by anopther user, no creation allowed
                return(false);
            }

            // read next resource
            if(resource.getParent() != null) {
                // readFolder without checking access
                resource = m_dbAccess.readFolder(resource.getProjectId(), resource.getRootName()+resource.getParent());
            }
        } while(resource.getParent() != null);

        // all checks are done positive
        return(true);
    }
    /**
     * Checks, if the user may lock this resource.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The resource to check.
     *
     * @return wether the user may lock this resource, or not.
     */
    public boolean accessLock(CmsUser currentUser, CmsProject currentProject,
                              String resourceName) throws CmsException {

        CmsResource resource = m_dbAccess.readFileHeader(currentProject.getId(), resourceName);
        return accessLock(currentUser,currentProject,resource);
    }
/**
 * Checks, if others may access this resource.
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param resource The resource to check.
 * @param flags The flags to check.
 *
 * @return wether the user has access, or not.
 */
protected boolean accessOther(CmsUser currentUser, CmsProject currentProject, CmsResource resource, int flags) throws CmsException
{
    if ((resource.getAccessFlags() & flags) == flags)
    {
        return true;
    }
    else
    {
        return false;
    }
}
        /**
     * Checks, if the owner may access this resource.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The resource to check.
     * @param flags The flags to check.
     *
     * @return wether the user has access, or not.
     */
    protected boolean accessOwner(CmsUser currentUser, CmsProject currentProject,
                                CmsResource resource, int flags)
        throws CmsException {
        // The Admin has always access
        if( isAdmin(currentUser, currentProject) ) {
            return(true);
        }
        // is the resource owned by this user?
        if(resource.getOwnerId() == currentUser.getId()) {
            if( (resource.getAccessFlags() & flags) == flags ) {
                return true ;
            }
        }
        // the resource isn't accesible by the user.
        return false;
    }
    // Methods working with projects

    /**
     * Tests if the user can access the project.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param projectId the id of the project.
     * @return true, if the user has access, else returns false.
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public boolean accessProject(CmsUser currentUser, CmsProject currentProject,
                                 int projectId)
        throws CmsException {


        CmsProject testProject = readProject(currentUser, currentProject, projectId);

        if (projectId==C_PROJECT_ONLINE_ID) {
            return true;
        }

        // is the project unlocked?
        if( testProject.getFlags() != C_PROJECT_STATE_UNLOCKED &&
            testProject.getFlags() != C_PROJECT_STATE_INVISIBLE) {
            return(false);
        }

        // is the current-user admin, or the owner of the project?
        if( (currentProject.getOwnerId() == currentUser.getId()) ||
            isAdmin(currentUser, currentProject) ) {
            return(true);
        }

        // get all groups of the user
        Vector groups = getGroupsOfUser(currentUser, currentProject,
                                        currentUser.getName());

        // test, if the user is in the same groups like the project.
        for(int i = 0; i < groups.size(); i++) {
            int groupId = ((CmsGroup) groups.elementAt(i)).getId();
            if( ( groupId == testProject.getGroupId() ) ||
                ( groupId == testProject.getManagerGroupId() ) ) {
                return( true );
            }
        }
        return( false );
    }
/**
 * Checks, if the user may read this resource.
 * NOTE: If the ressource is in the project you never have to fallback.
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param resource The resource to check.
 *
 * @return weather the user has access, or not.
 */
public boolean accessRead(CmsUser currentUser, CmsProject currentProject, CmsResource resource) throws CmsException
{
    Boolean access=(Boolean)m_accessCache.get(currentUser.getId()+":"+currentProject.getId()+":"+resource.getResourceName());
    if (access != null) {
            return access.booleanValue();
    } else {
    if ((resource == null) || !accessProject(currentUser, currentProject, resource.getProjectId()) ||
            (!accessOther(currentUser, currentProject, resource, C_ACCESS_PUBLIC_READ) && !accessOwner(currentUser, currentProject, resource, C_ACCESS_OWNER_READ) && !accessGroup(currentUser, currentProject, resource, C_ACCESS_GROUP_READ))) {
        m_accessCache.put(currentUser.getId()+":"+currentProject.getId()+":"+resource.getResourceName(), new Boolean(false));
        return false;
    }

    // check the rights for all
    CmsResource res = resource; // save the original resource name to be used if an error occurs.
    while (res.getParent() != null)
    {
        // readFolder without checking access
        res = m_dbAccess.readFolder(currentProject.getId(), res.getRootName()+res.getParent());
        if (res == null)
        {
            if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(A_OpenCms.C_OPENCMS_DEBUG, "Resource has no parent: " + resource.getAbsolutePath());
            }
            throw new CmsException(this.getClass().getName() + ".accessRead(): Cannot find \'" + resource.getName(), CmsException.C_NOT_FOUND);
        }
        if (!accessOther(currentUser, currentProject, res, C_ACCESS_PUBLIC_READ) && !accessOwner(currentUser, currentProject, res, C_ACCESS_OWNER_READ) && !accessGroup(currentUser, currentProject, res, C_ACCESS_GROUP_READ)) {
            m_accessCache.put(currentUser.getId()+":"+currentProject.getId()+":"+resource.getResourceName(), new Boolean(false));
            return false;
        }

    }
    m_accessCache.put(currentUser.getId()+":"+currentProject.getId()+":"+resource.getResourceName(), new Boolean(true));
    return true;
    }
}
/**
 * Checks, if the user may read this resource.
 * NOTE: If the ressource is in the project you never have to fallback.
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param resource The resource to check.
 *
 * @return weather the user has access, or not.
 */
public boolean accessRead(CmsUser currentUser, CmsProject currentProject, String resourceName) throws CmsException {
    CmsResource resource = m_dbAccess.readFileHeader(currentProject.getId(), resourceName);
    return accessRead(currentUser, currentProject, resource);
}
    /**
     * Checks, if the user may unlock this resource.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The resource to check.
     *
     * @return wether the user may unlock this resource, or not.
     */
    public boolean accessUnlock(CmsUser currentUser, CmsProject currentProject,
                                CmsResource resource)
        throws CmsException {
            // check, if this is the onlineproject
        if(onlineProject(currentUser, currentProject).equals(currentProject)){
            // the online-project is not writeable!
            return(false);
        }

        // check the access to the project
        if( ! accessProject(currentUser, currentProject, currentProject.getId()) ) {
            // no access to the project!
            return(false);
        }

        // check if the resource belongs to the current project
        if(resource.getProjectId() != currentProject.getId()) {
            return false;
        }

        // read the parent folder
        if(resource.getParent() != null) {
            // readFolder without checking access
            resource = m_dbAccess.readFolder(resource.getProjectId(), resource.getRootName()+resource.getParent());
        } else {
            // no parent folder!
            return true;
        }


        // check if the resource is not locked
        do {
            // is the resource locked?
            if( resource.isLocked() ) {
                // resource locked by anopther user, no creation allowed
                return(false);
            }

            // read next resource
            if(resource.getParent() != null) {
                // readFolder without checking access
                resource = m_dbAccess.readFolder(resource.getProjectId(), resource.getRootName()+resource.getParent());
            }
        } while(resource.getParent() != null);

        // all checks are done positive
        return(true);
    }
    /**
     * Checks, if the user may write this resource.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The resource to check.
     *
     * @return wether the user has access, or not.
     */
    public boolean accessWrite(CmsUser currentUser, CmsProject currentProject,
                               CmsResource resource) throws CmsException {


        // check, if this is the onlineproject

        if(onlineProject(currentUser, currentProject).equals(currentProject)){
            // the online-project is not writeable!
            return(false);
        }

        // check the access to the project
        if( ! accessProject(currentUser, currentProject, currentProject.getId()) ) {
            // no access to the project!
            return(false);
        }

        // check if the resource belongs to the current project
        if(resource.getProjectId() != currentProject.getId()) {
            return false;
        }

        // check, if the resource is locked by the current user
        if(resource.isLockedBy() != currentUser.getId()) {
            // resource is not locked by the current user, no writing allowed
            return(false);
        } else {
            //check if the project that has locked the resource is the current project
            if((resource.getLockedInProject() != currentProject.getId())){
                return (false);
            }
        }

        // check the rights for the current resource
        if( ! ( accessOther(currentUser, currentProject, resource, C_ACCESS_PUBLIC_WRITE) ||
                accessOwner(currentUser, currentProject, resource, C_ACCESS_OWNER_WRITE) ||
                accessGroup(currentUser, currentProject, resource, C_ACCESS_GROUP_WRITE) ) ) {
            // no write access to this resource!
            return false;
        }

        // read the parent folder
        if(resource.getParent() != null) {
            // readFolder without checking access
            resource = m_dbAccess.readFolder(resource.getProjectId(), resource.getRootName()+resource.getParent());
        } else {
            // no parent folder!
            return true;
        }


        // check the rights and if the resource is not locked
        // for parent folders only read access is needed
        do {
           if( accessOther(currentUser, currentProject, resource, C_ACCESS_PUBLIC_READ) ||
                accessOwner(currentUser, currentProject, resource, C_ACCESS_OWNER_READ) ||
                accessGroup(currentUser, currentProject, resource, C_ACCESS_GROUP_READ) ) {

                // is the resource locked?
                if( resource.isLocked() && (resource.isLockedBy() != currentUser.getId() ) ) {
                    // resource locked by anopther user, no creation allowed

                    return(false);
                }

                // read next resource
                if(resource.getParent() != null) {
                    // readFolder without checking access
                    resource = m_dbAccess.readFolder(resource.getProjectId(), resource.getRootName()+resource.getParent());
                }
            } else {
                // last check was negative
                return(false);
            }
        } while(resource.getParent() != null);

        // all checks are done positive
        return(true);
    }
    /**
     * Checks, if the user may write this resource.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The resource to check.
     *
     * @return wether the user has access, or not.
     */
    public boolean accessWrite(CmsUser currentUser, CmsProject currentProject,
                               String resourceName) throws CmsException {

        CmsResource resource = m_dbAccess.readFileHeader(currentProject.getId(), resourceName);
        return accessWrite(currentUser,currentProject,resource);
    }
    /**
     * Checks, if the user may write the unlocked resource.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The resource to check.
     *
     * @return wether the user has access, or not.
     */
    public boolean accessWriteUnlocked(CmsUser currentUser, CmsProject currentProject,
                               CmsResource resource) throws CmsException {


        // check, if this is the onlineproject

        if(onlineProject(currentUser, currentProject).equals(currentProject)){
            // the online-project is not writeable!
            return(false);
        }

        // check the access to the project
        if( ! accessProject(currentUser, currentProject, currentProject.getId()) ) {
            // no access to the project!
            return(false);
        }

        // check if the resource belongs to the current project
        if(resource.getProjectId() != currentProject.getId()) {
            return false;
        }

        // check the rights for the current resource
        if( ! ( accessOther(currentUser, currentProject, resource, C_ACCESS_PUBLIC_WRITE) ||
                accessOwner(currentUser, currentProject, resource, C_ACCESS_OWNER_WRITE) ||
                accessGroup(currentUser, currentProject, resource, C_ACCESS_GROUP_WRITE) ) ) {
            // no write access to this resource!
            return false;
        }

        // read the parent folder
        if(resource.getParent() != null) {
            // readFolder without checking access
            resource = m_dbAccess.readFolder(resource.getProjectId(), resource.getRootName()+resource.getParent());
        } else {
            // no parent folder!
            return true;
        }


        // check the rights and if the resource is not locked
        // for parent folders only read access is needed
        do {
           if( accessOther(currentUser, currentProject, resource, C_ACCESS_PUBLIC_READ) ||
                accessOwner(currentUser, currentProject, resource, C_ACCESS_OWNER_READ) ||
                accessGroup(currentUser, currentProject, resource, C_ACCESS_GROUP_READ) ) {

                // is the resource locked?
                if( resource.isLocked() && (resource.isLockedBy() != currentUser.getId() ) ) {
                    // resource locked by anopther user, no creation allowed
                    return(false);
                }

                // read next resource
                if(resource.getParent() != null) {
                    // readFolder without checking access
                    resource = m_dbAccess.readFolder(resource.getProjectId(), resource.getRootName()+resource.getParent());
                }
            } else {
                // last check was negative
                return(false);
            }
        } while(resource.getParent() != null);

        // all checks are done positive
        return(true);
    }
    /**
     * adds a file extension to the list of known file extensions
     *
     * <B>Security:</B>
     * Users, which are in the group "administrators" are granted.<BR/>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param extension a file extension like 'html'
     * @param resTypeName name of the resource type associated to the extension
     */

    public void addFileExtension(CmsUser currentUser, CmsProject currentProject,
                                 String extension, String resTypeName)
        throws CmsException {
        if (extension != null && resTypeName != null) {
            if (isAdmin(currentUser, currentProject)) {
                Hashtable suffixes=(Hashtable) m_dbAccess.readSystemProperty(C_SYSTEMPROPERTY_EXTENSIONS);
                if (suffixes == null) {
                    suffixes = new Hashtable();
                    suffixes.put(extension, resTypeName);
                    m_dbAccess.addSystemProperty(C_SYSTEMPROPERTY_EXTENSIONS, suffixes);
                } else {
                    suffixes.put(extension, resTypeName);
                    m_dbAccess.writeSystemProperty(C_SYSTEMPROPERTY_EXTENSIONS, suffixes);
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + extension,
                    CmsException.C_NO_ACCESS);
            }
        }
    }
    /**
     * Add a new group to the Cms.<BR/>
     *
     * Only the admin can do this.<P/>
     *
     * <B>Security:</B>
     * Only users, which are in the group "administrators" are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param name The name of the new group.
     * @param description The description for the new group.
     * @int flags The flags for the new group.
     * @param name The name of the parent group (or null).
     *
     * @return Group
     *
     * @exception CmsException Throws CmsException if operation was not succesfull.
     */
    public CmsGroup addGroup(CmsUser currentUser, CmsProject currentProject,
                               String name, String description, int flags, String parent)
        throws CmsException {
        // Check the security
        if( isAdmin(currentUser, currentProject) ) {
            name = name.trim();
            validFilename(name);
            // check the lenght of the groupname
            if(name.length() > 1) {
                return( m_dbAccess.createGroup(name, description, flags, parent) );
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + name, CmsException.C_BAD_NAME);
            }
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + name,
                CmsException.C_NO_ACCESS);
        }
    }

    /**
     * Adds a user to the Cms.
     *
     * Only a adminstrator can add users to the cms.<P/>
     *
     * <B>Security:</B>
     * Only users, which are in the group "administrators" are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param name The new name for the user.
     * @param password The new password for the user.
     * @param group The default groupname for the user.
     * @param description The description for the user.
     * @param additionalInfos A Hashtable with additional infos for the user. These
     * Infos may be stored into the Usertables (depending on the implementation).
     * @param flags The flags for a user (e.g. C_FLAG_ENABLED)
     *
     * @return user The added user will be returned.
     *
     * @exception CmsException Throws CmsException if operation was not succesfull.
     */
    public CmsUser addUser(CmsUser currentUser, CmsProject currentProject, String name,
                           String password, String group, String description,
                           Hashtable additionalInfos, int flags)
        throws CmsException {
        // Check the security
        if( isAdmin(currentUser, currentProject) ) {
            // no space before or after the name
            name = name.trim();
            // check the username
            validFilename(name);
            // check the password minimumsize
            if( (name.length() > 0) && (password.length() >= C_PASSWORD_MINIMUMSIZE) ) {
                CmsGroup defaultGroup =  readGroup(currentUser, currentProject, group);
                CmsUser newUser = m_dbAccess.addUser(name, password, description, " ", " ", " ", 0, 0, C_FLAG_ENABLED, additionalInfos, defaultGroup, " ", " ", C_USER_TYPE_SYSTEMUSER);
                addUserToGroup(currentUser, currentProject, newUser.getName(),defaultGroup.getName());
                return newUser;
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + name,
                    CmsException.C_SHORT_PASSWORD);
            }
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + name,
                CmsException.C_NO_ACCESS);
        }
    }

    /**
     * Adds a user to the Cms.
     *
     * Only a adminstrator can add users to the cms.<P/>
     *
     * <B>Security:</B>
     * Only users, which are in the group "administrators" are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param name The name for the user.
     * @param password The password for the user.
     * @param recoveryPassword The recoveryPassword for the user.
     * @param description The description for the user.
     * @param firstname The firstname of the user.
     * @param lastname The lastname of the user.
     * @param email The email of the user.
     * @param flags The flags for a user (e.g. C_FLAG_ENABLED)
     * @param additionalInfos A Hashtable with additional infos for the user. These
     * Infos may be stored into the Usertables (depending on the implementation).
     * @param defaultGroup The default groupname for the user.
     * @param address The address of the user
     * @param section The section of the user
     * @param type The type of the user
     *
     * @return user The added user will be returned.
     *
     * @exception CmsException Throws CmsException if operation was not succesfull.
     */
    public CmsUser addImportUser(CmsUser currentUser, CmsProject currentProject,
        String name, String password, String recoveryPassword, String description,
        String firstname, String lastname, String email, int flags, Hashtable additionalInfos,
        String defaultGroup, String address, String section, int type)
        throws CmsException {
        // Check the security
        if( isAdmin(currentUser, currentProject) ) {
            // no space before or after the name
            name = name.trim();
            // check the username
            validFilename(name);
            CmsGroup group =  readGroup(currentUser, currentProject, defaultGroup);
            CmsUser newUser = m_dbAccess.addImportUser(name, password, recoveryPassword, description, firstname, lastname, email, 0, 0, flags, additionalInfos, group, address, section, type);
            addUserToGroup(currentUser, currentProject, newUser.getName(), group.getName());
            return newUser;
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + name,
                CmsException.C_NO_ACCESS);
        }
    }

/**
 * Adds a user to a group.<BR/>
 *
 * Only the admin can do this.<P/>
 *
 * <B>Security:</B>
 * Only users, which are in the group "administrators" are granted.
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param username The name of the user that is to be added to the group.
 * @param groupname The name of the group.
 * @exception CmsException Throws CmsException if operation was not succesfull.
 */
public void addUserToGroup(CmsUser currentUser, CmsProject currentProject, String username, String groupname) throws CmsException {
    if (!userInGroup(currentUser, currentProject, username, groupname)) {
        // Check the security
        if (isAdmin(currentUser, currentProject)) {
            CmsUser user;
            CmsGroup group;
            try{
                user = readUser(currentUser, currentProject, username);
            } catch (CmsException e){
                if (e.getType() == CmsException.C_NO_USER){
                    user = readWebUser(currentUser, currentProject, username);
                } else {
                    throw e;
                }
            }
            //check if the user exists
            if (user != null) {
                group = readGroup(currentUser, currentProject, groupname);
                //check if group exists
                if (group != null) {
                    //add this user to the group
                    m_dbAccess.addUserToGroup(user.getId(), group.getId());
                    // update the cache
                    m_usergroupsCache.clear();
                } else {
                    throw new CmsException("[" + this.getClass().getName() + "]" + groupname, CmsException.C_NO_GROUP);
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + "]" + username, CmsException.C_NO_USER);
            }
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + username, CmsException.C_NO_ACCESS);
        }
    }
}
     /**
     * Adds a web user to the Cms. <br>
     *
     * A web user has no access to the workplace but is able to access personalized
     * functions controlled by the OpenCms.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param name The new name for the user.
     * @param password The new password for the user.
     * @param group The default groupname for the user.
     * @param description The description for the user.
     * @param additionalInfos A Hashtable with additional infos for the user. These
     * Infos may be stored into the Usertables (depending on the implementation).
     * @param flags The flags for a user (e.g. C_FLAG_ENABLED)
     *
     * @return user The added user will be returned.
     *
     * @exception CmsException Throws CmsException if operation was not succesfull.
     */
    public CmsUser addWebUser(CmsUser currentUser, CmsProject currentProject,
                             String name, String password,
                             String group, String description,
                             Hashtable additionalInfos, int flags)
        throws CmsException {
        // no space before or after the name
        name = name.trim();
        // check the username
        validFilename(name);
         // check the password minimumsize
        if( (name.length() > 0) && (password.length() >= C_PASSWORD_MINIMUMSIZE) ) {
                CmsGroup defaultGroup =  readGroup(currentUser, currentProject, group);
                CmsUser newUser = m_dbAccess.addUser(name, password, description, " ", " ", " ", 0, 0, C_FLAG_ENABLED, additionalInfos, defaultGroup, " ", " ", C_USER_TYPE_WEBUSER);
                CmsUser user;
                CmsGroup usergroup;

                user=m_dbAccess.readUser(newUser.getName(),C_USER_TYPE_WEBUSER);

                //check if the user exists
                if (user != null) {
                    usergroup=readGroup(currentUser,currentProject,group);
                    //check if group exists
                    if (usergroup != null){
                        //add this user to the group
                        m_dbAccess.addUserToGroup(user.getId(),usergroup.getId());
                        // update the cache
                        m_usergroupsCache.clear();
                    } else {
                        throw new CmsException("["+this.getClass().getName()+"]"+group,CmsException.C_NO_GROUP);
                    }
                } else {
                    throw new CmsException("["+this.getClass().getName()+"]"+name,CmsException.C_NO_USER);
                }

                return newUser;
        } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + name,
                    CmsException.C_SHORT_PASSWORD);
        }

    }

     /**
     * Adds a web user to the Cms. <br>
     *
     * A web user has no access to the workplace but is able to access personalized
     * functions controlled by the OpenCms.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param name The new name for the user.
     * @param password The new password for the user.
     * @param group The default groupname for the user.
     * @param additionalGroup An additional group for the user.
     * @param description The description for the user.
     * @param additionalInfos A Hashtable with additional infos for the user. These
     * Infos may be stored into the Usertables (depending on the implementation).
     * @param flags The flags for a user (e.g. C_FLAG_ENABLED)
     *
     * @return user The added user will be returned.
     *
     * @exception CmsException Throws CmsException if operation was not succesfull.
     */
    public CmsUser addWebUser(CmsUser currentUser, CmsProject currentProject,
                             String name, String password,
                             String group, String additionalGroup,
                             String description,
                             Hashtable additionalInfos, int flags)
        throws CmsException {
        // no space before or after the name
        name = name.trim();
        // check the username
        validFilename(name);
         // check the password minimumsize
        if( (name.length() > 0) && (password.length() >= C_PASSWORD_MINIMUMSIZE) ) {
            CmsGroup defaultGroup =  readGroup(currentUser, currentProject, group);
            CmsUser newUser = m_dbAccess.addUser(name, password, description, " ", " ", " ", 0, 0, C_FLAG_ENABLED, additionalInfos, defaultGroup, " ", " ", C_USER_TYPE_WEBUSER);
            CmsUser user;
            CmsGroup usergroup;
            CmsGroup addGroup;

            user=m_dbAccess.readUser(newUser.getName(),C_USER_TYPE_WEBUSER);
            //check if the user exists
            if (user != null) {
                usergroup=readGroup(currentUser,currentProject,group);
                //check if group exists
                if (usergroup != null && isWebgroup(usergroup)){
                    //add this user to the group
                    m_dbAccess.addUserToGroup(user.getId(),usergroup.getId());
                    // update the cache
                    m_usergroupsCache.clear();
                } else {
                    throw new CmsException("["+this.getClass().getName()+"]"+group,CmsException.C_NO_GROUP);
                }
                // if an additional groupname is given and the group does not belong to
                // Users, Administrators or Projectmanager add the user to this group
                if (additionalGroup != null && !"".equals(additionalGroup)){
                    addGroup = readGroup(currentUser, currentProject, additionalGroup);
                    if(addGroup != null && isWebgroup(addGroup)){
                        //add this user to the group
                        m_dbAccess.addUserToGroup(user.getId(), addGroup.getId());
                        // update the cache
                        m_usergroupsCache.clear();
                    } else {
                        throw new CmsException("["+this.getClass().getName()+"]"+additionalGroup,CmsException.C_NO_GROUP);
                    }
                }
            } else {
                throw new CmsException("["+this.getClass().getName()+"]"+name,CmsException.C_NO_USER);
            }
            return newUser;
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + name,
                                 CmsException.C_SHORT_PASSWORD);
        }
    }
/**
 * Returns the anonymous user object.<P/>
 *
 * <B>Security:</B>
 * All users are granted.
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @return the anonymous user object.
 * @exception CmsException Throws CmsException if operation was not succesful
 */
public CmsUser anonymousUser(CmsUser currentUser, CmsProject currentProject) throws CmsException
{
    return readUser(currentUser, currentProject, C_USER_GUEST);
}

     /**
     * Changes the group for this resource<br>
     *
     * Only the group of a resource in an offline project can be changed. The state
     * of the resource is set to CHANGED (1).
     * If the content of this resource is not exisiting in the offline project already,
     * it is read from the online project and written into the offline project.
     * The user may change this, if he is admin of the resource. <br>
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user is owner of the resource or is admin</li>
     * <li>the resource is locked by the callingUser</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param filename The complete path to the resource.
     * @param newGroup The name of the new group for this resource.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public void chgrp(CmsUser currentUser, CmsProject currentProject,
                      String filename, String newGroup)
        throws CmsException {

        CmsResource resource=null;
        // read the resource to check the access
        if (filename.endsWith("/")) {
            resource = readFolder(currentUser,currentProject,filename);
             } else {
            resource = (CmsFile)readFileHeader(currentUser,currentProject,filename);
        }

        // has the user write-access? and is he owner or admin?
        if( accessWrite(currentUser, currentProject, resource) &&
            ( (resource.getOwnerId() == currentUser.getId()) ||
              isAdmin(currentUser, currentProject))) {
            CmsGroup group = readGroup(currentUser, currentProject, newGroup);
            resource.setGroupId(group.getId());
            // write-acces  was granted - write the file.
            if (filename.endsWith("/")) {
                if (resource.getState()==C_STATE_UNCHANGED) {
                     resource.setState(C_STATE_CHANGED);
                }
                m_dbAccess.writeFolder(currentProject,(CmsFolder)resource,true, currentUser.getId());
                // update the cache
                this.clearResourceCache(filename);
            } else {
                m_dbAccess.writeFileHeader(currentProject,(CmsFile)resource,true, currentUser.getId());
                if (resource.getState()==C_STATE_UNCHANGED) {
                     resource.setState(C_STATE_CHANGED);
                }
                // update the cache
                this.clearResourceCache(filename);
            }
            // inform about the file-system-change
            fileSystemChanged(false);
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + filename,
                CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Changes the flags for this resource.<br>
     *
     * Only the flags of a resource in an offline project can be changed. The state
     * of the resource is set to CHANGED (1).
     * If the content of this resource is not exisiting in the offline project already,
     * it is read from the online project and written into the offline project.
     * The user may change the flags, if he is admin of the resource <br>.
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can write the resource</li>
     * <li>the resource is locked by the callingUser</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param filename The complete path to the resource.
     * @param flags The new accessflags for the resource.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public void chmod(CmsUser currentUser, CmsProject currentProject,
                      String filename, int flags)
        throws CmsException {

        CmsResource resource=null;
        // read the resource to check the access
        if (filename.endsWith("/")) {
            resource = readFolder(currentUser,currentProject,filename);
             } else {
            resource = (CmsFile)readFileHeader(currentUser,currentProject,filename);
        }

        // has the user write-access?
        if( accessWrite(currentUser, currentProject, resource)||
            ((resource.isLockedBy() == currentUser.getId() &&
              resource.getLockedInProject() == currentProject.getId()) &&
                (resource.getOwnerId() == currentUser.getId()||isAdmin(currentUser, currentProject))) ) {

            // write-acces  was granted - write the file.

            //set the flags
            resource.setAccessFlags(flags);
            //update file
            if (filename.endsWith("/")) {
                if (resource.getState()==C_STATE_UNCHANGED) {
                    resource.setState(C_STATE_CHANGED);
                }
                m_dbAccess.writeFolder(currentProject,(CmsFolder)resource,true, currentUser.getId());
                // update the cache
                this.clearResourceCache(filename);
            } else {
                m_dbAccess.writeFileHeader(currentProject,(CmsFile)resource,true, currentUser.getId());
                if (resource.getState()==C_STATE_UNCHANGED) {
                    resource.setState(C_STATE_CHANGED);
                }
                // update the cache
                this.clearResourceCache(filename);
            }
            m_accessCache.clear();
            // inform about the file-system-change
            fileSystemChanged(false);
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + filename,
                CmsException.C_NO_ACCESS);
        }
    }
/**
 * Changes the owner for this resource.<br>
 *
 * Only the owner of a resource in an offline project can be changed. The state
 * of the resource is set to CHANGED (1).
 * If the content of this resource is not exisiting in the offline project already,
 * it is read from the online project and written into the offline project.
 * The user may change this, if he is admin of the resource. <br>
 *
 * <B>Security:</B>
 * Access is cranted, if:
 * <ul>
 * <li>the user has access to the project</li>
 * <li>the user is owner of the resource or the user is admin</li>
 * <li>the resource is locked by the callingUser</li>
 * </ul>
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param filename The complete path to the resource.
 * @param newOwner The name of the new owner for this resource.
 *
 * @exception CmsException  Throws CmsException if operation was not succesful.
 */
public void chown(CmsUser currentUser, CmsProject currentProject, String filename, String newOwner) throws CmsException {
    CmsResource resource = null;
    // read the resource to check the access
    if (filename.endsWith("/")) {
        resource = readFolder(currentUser, currentProject, filename);
    } else {
        resource = (CmsFile) readFileHeader(currentUser, currentProject, filename);
    }

    // has the user write-access? and is he owner or admin?
    if (((resource.getOwnerId() == currentUser.getId()) || isAdmin(currentUser, currentProject)) &&
            (resource.isLockedBy() == currentUser.getId() &&
             resource.getLockedInProject() == currentProject.getId())) {
        CmsUser owner = readUser(currentUser, currentProject, newOwner);
        resource.setUserId(owner.getId());
        // write-acces  was granted - write the file.
        if (filename.endsWith("/")) {
            if (resource.getState() == C_STATE_UNCHANGED) {
                resource.setState(C_STATE_CHANGED);
            }
            m_dbAccess.writeFolder(currentProject, (CmsFolder) resource, true, currentUser.getId());
            // update the cache
            this.clearResourceCache(filename);
        } else {
            m_dbAccess.writeFileHeader(currentProject, (CmsFile) resource, true, currentUser.getId());
            if (resource.getState() == C_STATE_UNCHANGED) {
                resource.setState(C_STATE_CHANGED);
            }
            // update the cache
            this.clearResourceCache(filename);
        }
        m_accessCache.clear();
        // inform about the file-system-change
        fileSystemChanged(false);
    } else {
        throw new CmsException("[" + this.getClass().getName() + "] " + filename, CmsException.C_NO_ACCESS);
    }
}
     /**
     * Changes the state for this resource<BR/>
     *
     * The user may change this, if he is admin of the resource.
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user is owner of the resource or is admin</li>
     * <li>the resource is locked by the callingUser</li>
     * </ul>
     *
     * @param filename The complete path to the resource.
     * @param state The new state of this resource.
     *
     * @exception CmsException will be thrown, if the user has not the rights
     * for this resource.
     */
    public void chstate(CmsUser currentUser, CmsProject currentProject,
                        String filename, int state)
        throws CmsException {
        boolean isFolder=false;
        CmsResource resource=null;
        // read the resource to check the access
        if (filename.endsWith("/")) {
            isFolder=true;
            resource = readFolder(currentUser,currentProject,filename);
             } else {
            resource = (CmsFile)readFileHeader(currentUser,currentProject,filename);
        }

        // has the user write-access?
        if( accessWrite(currentUser, currentProject, resource)) {

            resource.setState(state);
            // write-acces  was granted - write the file.
            if (filename.endsWith("/")) {
                m_dbAccess.writeFolder(currentProject,(CmsFolder)resource,false, currentUser.getId());
                // update the cache
                this.clearResourceCache(filename);
            } else {
                m_dbAccess.writeFileHeader(currentProject,(CmsFile)resource,false, currentUser.getId());
                // update the cache
                this.clearResourceCache(filename);
            }
            // inform about the file-system-change
            fileSystemChanged(isFolder);
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + filename,
                CmsException.C_NO_ACCESS);
        }
    }
     /**
     * Changes the resourcetype for this resource<br>
     *
     * Only the resourcetype of a resource in an offline project can be changed. The state
     * of the resource is set to CHANGED (1).
     * If the content of this resource is not exisiting in the offline project already,
     * it is read from the online project and written into the offline project.
     * The user may change this, if he is admin of the resource. <br>
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user is owner of the resource or is admin</li>
     * <li>the resource is locked by the callingUser</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param filename The complete path to the resource.
     * @param newType The name of the new resourcetype for this resource.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public void chtype(CmsUser currentUser, CmsProject currentProject,
                      String filename, String newType)
        throws CmsException {

        I_CmsResourceType type = getResourceType(currentUser, currentProject, newType);

        // read the resource to check the access
        CmsResource resource = readFileHeader(currentUser,currentProject, filename);

        // has the user write-access? and is he owner or admin?
        if( accessWrite(currentUser, currentProject, resource) &&
            ( (resource.getOwnerId() == currentUser.getId()) ||
              isAdmin(currentUser, currentProject))) {

            // write-acces  was granted - write the file.
            resource.setType(type.getResourceType());
            resource.setLauncherType(type.getLauncherType());
            m_dbAccess.writeFileHeader(currentProject, (CmsFile)resource,true, currentUser.getId());
            if (resource.getState()==C_STATE_UNCHANGED) {
                resource.setState(C_STATE_CHANGED);
            }
            // update the cache
            this.clearResourceCache(filename);

            // inform about the file-system-change
            fileSystemChanged(false);
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + filename,
                CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Clears all internal DB-Caches.
     */
    public void clearcache() {
        m_userCache.clear();
        m_groupCache.clear();
        m_usergroupsCache.clear();
        m_projectCache.clear();
        m_resourceCache.clear();
        m_subresCache.clear();
        m_propertyCache.clear();
        m_propertyDefCache.clear();
        m_propertyDefVectorCache.clear();
        m_onlineProjectCache = null;
        m_accessCache.clear();

        CmsTemplateClassManager.clearCache();

    }
    /**
     * Copies a file in the Cms. <br>
     *
     * <B>Security:</B>
     * Access is cranted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can read the sourceresource</li>
     * <li>the user can create the destinationresource</li>
     * <li>the destinationresource dosn't exists</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param source The complete path of the sourcefile.
     * @param destination The complete path to the destination.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public void copyFile(CmsUser currentUser, CmsProject currentProject,
                         String source, String destination)
        throws CmsException {

        // the name of the new file.
        String filename;
        // the name of the folder.
        String foldername;

        // checks, if the destinateion is valid, if not it throws a exception
        validFilename(destination.replace('/', 'a'));

        // read the source-file, to check readaccess
        CmsResource file = readFileHeader(currentUser, currentProject, source);

        // split the destination into file and foldername
        if (destination.endsWith("/")) {
            filename = file.getName();
            foldername = destination;
        }else{
            foldername = destination.substring(0, destination.lastIndexOf("/")+1);
            filename = destination.substring(destination.lastIndexOf("/")+1,
                                             destination.length());
        }

        CmsFolder cmsFolder = readFolder(currentUser,currentProject, foldername);
        if( accessCreate(currentUser, currentProject, (CmsResource)cmsFolder) ) {
            if(( accessOther(currentUser, currentProject, file, C_ACCESS_PUBLIC_WRITE) ||
                accessOwner(currentUser, currentProject, file, C_ACCESS_OWNER_WRITE) ||
                accessGroup(currentUser, currentProject, file, C_ACCESS_GROUP_WRITE) )){
                // write-acces  was granted - copy the file and the metainfos
                m_dbAccess.copyFile(currentProject, onlineProject(currentUser, currentProject),
                              currentUser.getId(),source,cmsFolder.getResourceId(), foldername + filename);

                this.clearResourceCache(foldername + filename);
                // copy the metainfos
                lockResource(currentUser, currentProject, destination, true);
                writeProperties(currentUser,currentProject, destination,
                            readAllProperties(currentUser,currentProject,file.getResourceName()));
                m_accessCache.clear();
                // inform about the file-system-change
                fileSystemChanged(file.isFolder());
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + source,
                    CmsException.C_NO_ACCESS);
            }
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + destination,
                CmsException.C_NO_ACCESS);
        }
    }
     /**
     * Copies a folder in the Cms. <br>
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can read the sourceresource</li>
     * <li>the user can create the destinationresource</li>
     * <li>the destinationresource dosn't exists</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param source The complete path of the sourcefolder.
     * @param destination The complete path to the destination.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public void copyFolder(CmsUser currentUser, CmsProject currentProject,
                           String source, String destination)
        throws CmsException {

        // the name of the new file.
        String filename;
        // the name of the folder.
        String foldername;

        // checks, if the destinateion is valid, if not it throws a exception
        validFilename(destination.replace('/', 'a'));
        foldername = destination.substring(0, destination.substring(0,destination.length()-1).lastIndexOf("/")+1);
        CmsFolder cmsFolder = readFolder(currentUser,currentProject, foldername);
        if( accessCreate(currentUser, currentProject, (CmsResource)cmsFolder) ) {
            // write-acces  was granted - copy the folder and the properties
            CmsFolder folder=readFolder(currentUser,currentProject,source);
            // check write access to the folder that has to be copied
            if(( accessOther(currentUser, currentProject, (CmsResource)folder, C_ACCESS_PUBLIC_WRITE) ||
                accessOwner(currentUser, currentProject, (CmsResource)folder, C_ACCESS_OWNER_WRITE) ||
                accessGroup(currentUser, currentProject, (CmsResource)folder, C_ACCESS_GROUP_WRITE) )){
                m_dbAccess.createFolder(currentUser,currentProject,onlineProject(currentUser, currentProject),folder,cmsFolder.getResourceId(),destination);
                this.clearResourceCache(destination);
                // copy the properties
                lockResource(currentUser, currentProject, destination, true);
                writeProperties(currentUser,currentProject, destination,
                            readAllProperties(currentUser,currentProject,folder.getResourceName()));
                m_subresCache.clear();
                m_accessCache.clear();
                // inform about the file-system-change
                fileSystemChanged(true);
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + source,
                    CmsException.C_ACCESS_DENIED);
            }
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + destination,
                CmsException.C_ACCESS_DENIED);
        }

    }

    /**
     * Copies a resource from the online project to a new, specified project.<br>
     * Copying a resource will copy the file header or folder into the specified
     * offline project and set its state to UNCHANGED.
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user is the owner of the project</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The name of the resource.
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public void copyResourceToProject(CmsUser currentUser,
                                      CmsProject currentProject,
                                      String resource)
        throws CmsException {
        // read the onlineproject
        CmsProject online = onlineProject(currentUser, currentProject);
        // is the current project the onlineproject?
        // and is the current user the owner of the project?
        // and is the current project state UNLOCKED?
        if ((!currentProject.equals(online)) &&
            (isManagerOfProject(currentUser, currentProject))
            && (currentProject.getFlags() == C_PROJECT_STATE_UNLOCKED)) {
            // is offlineproject and is owner
            // try to read the resource from the offline project, include deleted
            CmsResource offlineRes = null;
            try{
                m_resourceCache.remove(resource);
                Vector subFiles = getFilesInFolder(currentUser, currentProject, resource, true);
                Vector subFolders = getSubFolders(currentUser, currentProject, resource, true);
                for(int i=0; i<subFolders.size(); i++){
                    String foldername = ((CmsResource)subFolders.elementAt(i)).getResourceName();
                    m_resourceCache.remove(foldername);
                }
                for(int i=0; i<subFiles.size(); i++){
                    String filename = ((CmsResource)subFiles.elementAt(i)).getResourceName();
                    m_resourceCache.remove(filename);
                }
                this.clearResourceCache(resource);
                m_accessCache.clear();
                offlineRes = readFileHeader(currentUser, currentProject, currentProject.getId(),resource);
            } catch (CmsException exc){
                // if the resource does not exist in the offlineProject - it's ok
            }
            // create the projectresource only if the resource is not in the current project
            if ((offlineRes == null) || (offlineRes.getProjectId() != currentProject.getId())){
                // check if there are already any subfolders of this resource
                if(resource.endsWith("/")){
                    Vector projectResources = m_dbAccess.readAllProjectResources(currentProject.getId());
                    for(int i=0; i<projectResources.size(); i++){
                        String resname = (String)projectResources.elementAt(i);
                        if(resname.startsWith(resource)){
                            // delete the existing project resource first
                            m_dbAccess.deleteProjectResource(currentProject.getId(), resname);
                        }
                    }
                }
                try {
                    m_dbAccess.createProjectResource(currentProject.getId(), resource);
                } catch (CmsException exc) {
                    // if the subfolder exists already - all is ok
                }
            }
        } else {
            // no changes on the onlineproject!
            throw new CmsException("[" + this.getClass().getName() + "] " + currentProject.getName(), CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Counts the locked resources in this project.
     *
     * <B>Security</B>
     * Only the admin or the owner of the project can do this.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param id The id of the project
     * @return the amount of locked resources in this project.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public int countLockedResources(CmsUser currentUser, CmsProject currentProject, int id)
        throws CmsException {
        // read the project.
        CmsProject project = readProject(currentUser, currentProject, id);

        // check the security
        if( isAdmin(currentUser, currentProject) ||
            isManagerOfProject(currentUser, project) ||
            (project.getFlags() == C_PROJECT_STATE_UNLOCKED )) {

            // count locks
            return m_dbAccess.countLockedResources(project);
        } else {
             throw new CmsException("[" + this.getClass().getName() + "] " + id,
                CmsException.C_NO_ACCESS);
        }
    }
/**
 * return the correct DbAccess class.
 * This method should be overloaded by all other Database Drivers
 * Creation date: (09/15/00 %r)
 * @return com.opencms.file.genericSql.CmsDbAccess
 * @param configurations source.org.apache.java.util.Configurations
 * @exception com.opencms.core.CmsException Thrown if CmsDbAccess class could not be instantiated.
 */
public com.opencms.file.genericSql.CmsDbAccess createDbAccess(Configurations configurations) throws CmsException
{
    return new com.opencms.file.genericSql.CmsDbAccess(configurations);
}
    /**
     * Creates a new file with the given content and resourcetype. <br>
     *
     * Files can only be created in an offline project, the state of the new file
     * is set to NEW (2). <br>
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can write the resource</li>
     * <li>the folder-resource is not locked by another user</li>
     * <li>the file dosn't exists</li>
     * </ul>
     *
     * @param currentUser The user who owns this file.
     * @param currentGroup The group who owns this file.
     * @param currentProject The project in which the resource will be used.
     * @param folder The complete path to the folder in which the new folder will
     * be created.
     * @param file The name of the new file (No pathinformation allowed).
     * @param contents The contents of the new file.
     * @param type The name of the resourcetype of the new file.
     * @param propertyinfos A Hashtable of propertyinfos, that should be set for this folder.
     * The keys for this Hashtable are the names for propertydefinitions, the values are
     * the values for the propertyinfos.
     * @return file The created file.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
     public CmsFile createFile(CmsUser currentUser, CmsGroup currentGroup,
                               CmsProject currentProject, String folder,
                               String filename, byte[] contents, String type,
                               Hashtable propertyinfos)

         throws CmsException {

        // checks, if the filename is valid, if not it throws a exception
        validFilename(filename);

        CmsFolder cmsFolder = readFolder(currentUser,currentProject, folder);
        if( accessCreate(currentUser, currentProject, (CmsResource)cmsFolder) ) {
            // write-access was granted - create and return the file.
            CmsFile file = m_dbAccess.createFile(currentUser, currentProject,
                                               onlineProject(currentUser, currentProject),
                                               folder + filename, 0, cmsFolder.getResourceId(),
                                               contents,
                                               getResourceType(currentUser, currentProject, type));

            // update the access flags
            Hashtable startSettings=null;
            Integer accessFlags=null;
            startSettings=(Hashtable)currentUser.getAdditionalInfo(C_ADDITIONAL_INFO_STARTSETTINGS);
            if (startSettings != null) {
                accessFlags=(Integer)startSettings.get(C_START_ACCESSFLAGS);
                if (accessFlags != null) {
                    file.setAccessFlags(accessFlags.intValue());
                }
            }
            if(currentGroup != null) {
                file.setGroupId(currentGroup.getId());
            }

            m_dbAccess.writeFileHeader(currentProject, file,false);

            this.clearResourceCache(folder + filename);

            // write the metainfos
            m_dbAccess.writeProperties(propertyinfos, currentProject.getId(),file, file.getType());

            // inform about the file-system-change
            fileSystemChanged(false);

            return file ;
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + folder + filename,
                CmsException.C_NO_ACCESS);
        }

     }
    /**
     * Creates a new folder.
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can write the resource</li>
     * <li>the resource is not locked by another user</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentGroup The group who requested this method.
     * @param currentProject The current project of the user.
     * @param folder The complete path to the folder in which the new folder will
     * be created.
     * @param newFolderName The name of the new folder (No pathinformation allowed).
     * @param propertyinfos A Hashtable of propertyinfos, that should be set for this folder.
     * The keys for this Hashtable are the names for propertydefinitions, the values are
     * the values for the propertyinfos.
     *
     * @return file The created file.
     *
     * @exception CmsException will be thrown for missing propertyinfos, for worng propertydefs
     * or if the filename is not valid. The CmsException will also be thrown, if the
     * user has not the rights for this resource.
     */
    public CmsFolder createFolder(CmsUser currentUser, CmsGroup currentGroup,
                                  CmsProject currentProject,
                                  String folder, String newFolderName,
                                  Hashtable propertyinfos)
        throws CmsException {

        // checks, if the filename is valid, if not it throws a exception
        validFilename(newFolderName);
        CmsFolder cmsFolder = readFolder(currentUser,currentProject, folder);
        if( accessCreate(currentUser, currentProject, (CmsResource)cmsFolder) ) {

            // write-acces  was granted - create the folder.
            CmsFolder newFolder = m_dbAccess.createFolder(currentUser, currentProject,
                                                          cmsFolder.getResourceId(),
                                                          C_UNKNOWN_ID,
                                                          folder + newFolderName +
                                                          C_FOLDER_SEPERATOR,
                                                          0);
            // update the access flags
            Hashtable startSettings=null;
            Integer accessFlags=null;
            startSettings=(Hashtable)currentUser.getAdditionalInfo(C_ADDITIONAL_INFO_STARTSETTINGS);
            if (startSettings != null) {
                accessFlags=(Integer)startSettings.get(C_START_ACCESSFLAGS);
                if (accessFlags != null) {
                    newFolder.setAccessFlags(accessFlags.intValue());
                }
            }
            if(currentGroup != null) {
                newFolder.setGroupId(currentGroup.getId());
            }
            newFolder.setState(C_STATE_NEW);

            m_dbAccess.writeFolder(currentProject, newFolder, false);
            this.clearResourceCache(folder + newFolderName + C_FOLDER_SEPERATOR);

            // write metainfos for the folder
            m_dbAccess.writeProperties(propertyinfos, currentProject.getId(), newFolder, newFolder.getType());

            // inform about the file-system-change
            fileSystemChanged(true);
            // return the folder
            return newFolder ;
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + folder + newFolderName,
                CmsException.C_NO_ACCESS);
        }
    }
/**
 * Creates a project.
 *
 * <B>Security</B>
 * Only the users which are in the admin or projectleader-group are granted.
 *
 * Changed: added the parent id
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param name The name of the project to read.
 * @param description The description for the new project.
 * @param group the group to be set.
 * @param managergroup the managergroup to be set.
 * @param parentId the parent project
 * @exception CmsException Throws CmsException if something goes wrong.
 * @author Martin Langelund
 */
public CmsProject createProject(CmsUser currentUser, CmsProject currentProject, String name, String description, String groupname, String managergroupname) throws CmsException
{
    if (isAdmin(currentUser, currentProject) || isProjectManager(currentUser, currentProject))
    {
        if (C_PROJECT_ONLINE.equals(name)){
            throw new CmsException ("[" + this.getClass().getName() + "] " + name, CmsException.C_BAD_NAME);
        }
        // read the needed groups from the cms
        CmsGroup group = readGroup(currentUser, currentProject, groupname);
        CmsGroup managergroup = readGroup(currentUser, currentProject, managergroupname);

        // create a new task for the project
        CmsTask task = createProject(currentUser, name, 1, group.getName(), System.currentTimeMillis(), C_TASK_PRIORITY_NORMAL);
        return m_dbAccess.createProject(currentUser, group, managergroup, task, name, description, C_PROJECT_STATE_UNLOCKED, C_PROJECT_TYPE_NORMAL);
    }
    else
    {
        throw new CmsException("[" + this.getClass().getName() + "] " + name, CmsException.C_NO_ACCESS);
    }
}

/**
 * Creates a project.
 *
 * <B>Security</B>
 * Only the users which are in the admin or projectleader-group are granted.
 *
 * Changed: added the project type
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param name The name of the project to read.
 * @param description The description for the new project.
 * @param group the group to be set.
 * @param managergroup the managergroup to be set.
 * @param project type the type of the project
 * @exception CmsException Throws CmsException if something goes wrong.
 * @author Edna Falkenhan
 */
public CmsProject createProject(CmsUser currentUser, CmsProject currentProject, String name, String description, String groupname, String managergroupname, int projecttype) throws CmsException
{
    if (isAdmin(currentUser, currentProject) || isProjectManager(currentUser, currentProject))
    {
        if (C_PROJECT_ONLINE.equals(name)){
            throw new CmsException ("[" + this.getClass().getName() + "] " + name, CmsException.C_BAD_NAME);
        }
        // read the needed groups from the cms
        CmsGroup group = readGroup(currentUser, currentProject, groupname);
        CmsGroup managergroup = readGroup(currentUser, currentProject, managergroupname);

        // create a new task for the project
        CmsTask task = createProject(currentUser, name, 1, group.getName(), System.currentTimeMillis(), C_TASK_PRIORITY_NORMAL);
        return m_dbAccess.createProject(currentUser, group, managergroup, task, name, description, C_PROJECT_STATE_UNLOCKED, projecttype);
    }
    else
    {
        throw new CmsException("[" + this.getClass().getName() + "] " + name, CmsException.C_NO_ACCESS);
    }
}

/**
 * Creates a project for the temporary files.
 *
 * <B>Security</B>
 * Only the users which are in the admin or projectleader-group are granted.
 *
 * Changed: added the project type
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @exception CmsException Throws CmsException if something goes wrong.
 * @author Edna Falkenhan
 */
public CmsProject createTempfileProject(CmsObject cms, CmsUser currentUser, CmsProject currentProject) throws CmsException
{
    String name = "tempFileProject";
    String description = "Project for temporary files";
    if (isAdmin(currentUser, currentProject))
    {
        // read the needed groups from the cms
        CmsGroup group = readGroup(currentUser, currentProject, "Users");
        CmsGroup managergroup = readGroup(currentUser, currentProject, "Administrators");

        // create a new task for the project
        CmsTask task = createProject(currentUser, name, 1, group.getName(), System.currentTimeMillis(), C_TASK_PRIORITY_NORMAL);
        CmsProject tempProject = m_dbAccess.createProject(currentUser, group, managergroup, task, name, description, C_PROJECT_STATE_INVISIBLE, C_PROJECT_STATE_INVISIBLE);
        m_dbAccess.createProjectResource(tempProject.getId(), "/");
        cms.getRegistry().setSystemValue("tempfileproject",""+tempProject.getId());
        return tempProject;
    }
    else
    {
        throw new CmsException("[" + this.getClass().getName() + "] " + name, CmsException.C_NO_ACCESS);
    }
}
    /**
     * Creates a new project for task handling.
     *
     * @param currentUser User who creates the project
     * @param projectName Name of the project
     * @param projectType Type of the Project
     * @param role Usergroup for the project
     * @param timeout Time when the Project must finished
     * @param priority Priority for the Project
     *
     * @return The new task project
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsTask createProject(CmsUser currentUser, String projectName,
                                 int projectType, String roleName,
                                 long timeout, int priority)
        throws CmsException {

        CmsGroup role = null;

        // read the role
        if(roleName!=null && !roleName.equals("")) {
            role = readGroup(currentUser, null, roleName);
        }
        // create the timestamp
        java.sql.Timestamp timestamp = new java.sql.Timestamp(timeout);
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        return m_dbAccess.createTask(0,0,
                                     1, // standart project type,
                                     currentUser.getId(),
                                     currentUser.getId(),
                                     role.getId(),
                                     projectName,
                                     now,
                                     timestamp,
                                     priority);
    }
    // Methods working with properties and propertydefinitions


    /**
     * Creates the propertydefinition for the resource type.<BR/>
     *
     * <B>Security</B>
     * Only the admin can do this.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param name The name of the propertydefinition to overwrite.
     * @param resourcetype The name of the resource-type for the propertydefinition.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsPropertydefinition createPropertydefinition(CmsUser currentUser,
                                                    CmsProject currentProject,
                                                    String name,
                                                    String resourcetype)
        throws CmsException {
        // check the security
        if( isAdmin(currentUser, currentProject) ) {
            // no space before or after the name
            name = name.trim();
            // check the name
            validFilename(name);
            m_propertyDefVectorCache.clear();
            return( m_dbAccess.createPropertydefinition(name,
                                                        getResourceType(currentUser,
                                                                        currentProject,
                                                                        resourcetype)) );
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + name,
                CmsException.C_NO_ACCESS);
        }
    }

    /**
     * Creates a new task.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param projectid The Id of the current project task of the user.
     * @param agentName User who will edit the task
     * @param roleName Usergroup for the task
     * @param taskName Name of the task
     * @param taskType Type of the task
     * @param taskComment Description of the task
     * @param timeout Time when the task must finished
     * @param priority Id for the priority
     *
     * @return A new Task Object
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsTask createTask(CmsUser currentUser, int projectid,
                              String agentName, String roleName,
                              String taskName, String taskComment,
                              int taskType, long timeout, int priority)
        throws CmsException {
        CmsUser agent = m_dbAccess.readUser(agentName, C_USER_TYPE_SYSTEMUSER);
        CmsGroup role = m_dbAccess.readGroup(roleName);
        java.sql.Timestamp timestamp = new java.sql.Timestamp(timeout);
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        validTaskname(taskName);   // check for valid Filename

        CmsTask task = m_dbAccess.createTask(projectid,
                                             projectid,
                                             taskType,
                                             currentUser.getId(),
                                             agent.getId(),
                                             role.getId(),
                                             taskName, now, timestamp, priority);
        if(taskComment!=null && !taskComment.equals("")) {
            m_dbAccess.writeTaskLog(task.getId(), currentUser.getId(),
                                    new java.sql.Timestamp(System.currentTimeMillis()),
                                    taskComment, C_TASKLOG_USER);
        }
        return task;
    }

    /**
     * Creates a new task.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param agent Username who will edit the task
     * @param role Usergroupname for the task
     * @param taskname Name of the task
     * @param taskcomment Description of the task.
     * @param timeout Time when the task must finished
     * @param priority Id for the priority
     *
     * @return A new Task Object
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsTask createTask(CmsUser currentUser, CmsProject currentProject,
                              String agentName, String roleName,
                              String taskname, String taskcomment,
                              long timeout, int priority)
        throws CmsException {
        CmsGroup role = m_dbAccess.readGroup(roleName);
        java.sql.Timestamp timestamp = new java.sql.Timestamp(timeout);
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
        int agentId = C_UNKNOWN_ID;
        validTaskname(taskname);   // check for valid Filename
        try {
            agentId = m_dbAccess.readUser(agentName, C_USER_TYPE_SYSTEMUSER).getId();
        } catch (Exception e) {
            // ignore that this user doesn't exist and create a task for the role
        }
        return m_dbAccess.createTask(currentProject.getTaskId(),
                                     currentProject.getTaskId(),
                                     1, // standart Task Type
                                     currentUser.getId(),
                                     agentId,
                                     role.getId(),
                                     taskname, now, timestamp, priority);
    }
    /**
     * Deletes all propertyinformation for a file or folder.
     *
     * <B>Security</B>
     * Only the user is granted, who has the right to write the resource.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The name of the resource of which the propertyinformations
     * have to be deleted.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public void deleteAllProperties(CmsUser currentUser,
                                          CmsProject currentProject,
                                          String resource)
        throws CmsException {

        // read the resource
        CmsResource res = readFileHeader(currentUser,currentProject, resource);
        // check the security
        if( ! accessWrite(currentUser, currentProject, res) ) {
             throw new CmsException("[" + this.getClass().getName() + "] " + resource,
                CmsException.C_NO_ACCESS);
        }

        //delete all Properties
        m_dbAccess.deleteAllProperties(currentProject.getId(),res);
        m_propertyCache.clear();
    }
    /**
     * Deletes a file in the Cms.<br>
     *
     * A file can only be deleteed in an offline project.
     * A file is deleted by setting its state to DELETED (3). <br>
     *
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can write the resource</li>
     * <li>the resource is locked by the callinUser</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param filename The complete path of the file.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public void deleteFile(CmsUser currentUser, CmsProject currentProject,
                           String filename)
        throws CmsException {

        // read the file
        CmsResource onlineFile;
        CmsResource file = readFileHeader(currentUser,currentProject, filename);
        try {
            onlineFile = readFileHeader(currentUser,onlineProject(currentUser, currentProject), filename);

        } catch (CmsException exc) {
            // the file dosent exist
            onlineFile = null;
        }

        // has the user write-access?
        if( accessWrite(currentUser, currentProject, file) ) {
            // write-acces  was granted - delete the file.
            // and the metainfos
            if(onlineFile == null) {
                // the onlinefile dosent exist => remove the file realy!
                deleteAllProperties(currentUser,currentProject,file.getResourceName());
                m_dbAccess.removeFile(currentProject.getId(), filename);
            } else {
                m_dbAccess.deleteFile(currentProject, filename);

            }
            // update the cache
            this.clearResourceCache(filename);
            m_accessCache.clear();

            // inform about the file-system-change
            fileSystemChanged(false);

        } else {
            if(file.getState() == C_STATE_DELETED){
                throw new CmsException("[" + this.getClass().getName() + "] " + filename,
                    CmsException.C_RESOURCE_DELETED);
            }
            throw new CmsException("[" + this.getClass().getName() + "] " + filename,
                CmsException.C_NO_ACCESS);
        }
    }
     /**
     * Deletes a folder in the Cms.<br>
     *
     * Only folders in an offline Project can be deleted. A folder is deleted by
     * setting its state to DELETED (3). <br>
     *
     * In its current implmentation, this method can ONLY delete empty folders.
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can read and write this resource and all subresources</li>
     * <li>the resource is not locked</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param foldername The complete path of the folder.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public void deleteFolder(CmsUser currentUser, CmsProject currentProject,
                             String foldername)
        throws CmsException {

        CmsResource onlineFolder;

        // read the folder, that shold be deleted
        CmsFolder cmsFolder = readFolder(currentUser,currentProject,foldername);
        try {
            onlineFolder = readFolder(currentUser,onlineProject(currentUser, currentProject), foldername);
        } catch (CmsException exc) {
            // the file dosent exist
            onlineFolder = null;
        }
        // check, if the user may delete the resource
        if( accessWrite(currentUser, currentProject, cmsFolder) ) {

            // write-acces  was granted - delete the folder and metainfos.
            if(onlineFolder == null) {
                // the onlinefile dosent exist => remove the file realy!
                deleteAllProperties(currentUser,currentProject, cmsFolder.getResourceName());
                m_dbAccess.removeFolder(currentProject.getId(),cmsFolder);
            } else {
                m_dbAccess.deleteFolder(currentProject,cmsFolder, false);
            }
            // update cache
            this.clearResourceCache(foldername);
            m_accessCache.clear();
            // inform about the file-system-change
            fileSystemChanged(true);

        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + foldername,
                CmsException.C_NO_ACCESS);
        }
    }

    /**
     * Undeletes a file in the Cms.<br>
     *
     * A file can only be undeleted in an offline project.
     * A file is undeleted by setting its state to CHANGED (1). <br>
     *
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can write the resource</li>
     * <li>the resource is locked by the callinUser</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param filename The complete path of the file.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public void undeleteResource(CmsUser currentUser, CmsProject currentProject,
                                String filename)
        throws CmsException {
        boolean isFolder=false;
        CmsResource resource=null;
        int state = C_STATE_CHANGED;
        // read the resource to check the access
        if (filename.endsWith("/")) {
            isFolder=true;
            resource = m_dbAccess.readFolder(currentProject.getId(),filename);
        } else {
            resource = (CmsFile)m_dbAccess.readFileHeader(currentProject.getId(),filename, true);
        }

        // has the user write-access?
        if( accessWriteUnlocked(currentUser, currentProject, resource)) {
            resource.setState(state);
            resource.setLocked(currentUser.getId());
            // write-access  was granted - write the file.
            if (filename.endsWith("/")) {
                m_dbAccess.writeFolder(currentProject,(CmsFolder)resource,false, currentUser.getId());
                // update the cache
                this.clearResourceCache(filename);
            } else {
                m_dbAccess.writeFileHeader(currentProject,(CmsFile)resource,false, currentUser.getId());
                // update the cache
                this.clearResourceCache(filename);
            }
            // inform about the file-system-change
            fileSystemChanged(isFolder);
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + filename,
                CmsException.C_NO_ACCESS);
        }
    }

    /**
     * Delete a group from the Cms.<BR/>
     * Only groups that contain no subgroups can be deleted.
     *
     * Only the admin can do this.<P/>
     *
     * <B>Security:</B>
     * Only users, which are in the group "administrators" are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param delgroup The name of the group that is to be deleted.
     * @exception CmsException  Throws CmsException if operation was not succesfull.
     */
    public void deleteGroup(CmsUser currentUser, CmsProject currentProject,
                            String delgroup)
        throws CmsException {
        // Check the security
        if( isAdmin(currentUser, currentProject) ) {
            Vector childs=null;
            Vector users=null;
            // get all child groups of the group
            childs=getChild(currentUser,currentProject,delgroup);
            // get all users in this group
            users=getUsersOfGroup(currentUser,currentProject,delgroup);
            // delete group only if it has no childs and there are no users in this group.
            if ((childs == null) && ((users == null) || (users.size() == 0))) {
                m_dbAccess.deleteGroup(delgroup);
                m_groupCache.remove(delgroup);
            } else {
                throw new CmsException(delgroup, CmsException.C_GROUP_NOT_EMPTY);
            }
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + delgroup,
                CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Deletes a project.
     *
     * <B>Security</B>
     * Only the admin or the owner of the project can do this.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param id The id of the project to be published.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void deleteProject(CmsUser currentUser, CmsProject currentProject,
                              int id)
        throws CmsException {
        Vector deletedFolders = new Vector();
        // read the project that should be deleted.
        CmsProject deleteProject = readProject(currentUser, currentProject, id);

        if((isAdmin(currentUser, currentProject) || isManagerOfProject(currentUser, deleteProject))
            && (id != C_PROJECT_ONLINE_ID)) {
            Vector allFiles = m_dbAccess.readFiles(deleteProject.getId(), false, true);
            Vector allFolders = m_dbAccess.readFolders(deleteProject.getId(), false, true);
            // first delete files or undo changes in files
            for(int i=0; i<allFiles.size();i++){
                CmsFile currentFile = (CmsFile)allFiles.elementAt(i);
                if(currentFile.getState() == C_STATE_NEW){
                    // delete the properties
                    m_dbAccess.deleteAllProperties(id, currentFile.getResourceId());
                    // delete the file
                    m_dbAccess.removeFile(id, currentFile.getResourceName());
                } else if (currentFile.getState() == C_STATE_CHANGED){
                    if(!currentFile.isLocked()){
                        // lock the resource
                        lockResource(currentUser,deleteProject,currentFile.getResourceName(),true);
                    }
                    // undo all changes in the file
                    undoChanges(currentUser, deleteProject, currentFile.getResourceName());
                } else if (currentFile.getState() == C_STATE_DELETED){
                    // first undelete the file
                    undeleteResource(currentUser, deleteProject, currentFile.getResourceName());
                    if(!currentFile.isLocked()){
                        // lock the resource
                        lockResource(currentUser,deleteProject,currentFile.getResourceName(),true);
                    }
                    // then undo all changes in the file
                    undoChanges(currentUser, deleteProject, currentFile.getResourceName());
                }
            }
            // now delete folders or undo changes in folders
            for(int i=0; i<allFolders.size();i++){
                CmsFolder currentFolder = (CmsFolder)allFolders.elementAt(i);
                if(currentFolder.getState() == C_STATE_NEW){
                    // delete the properties
                    m_dbAccess.deleteAllProperties(id, currentFolder.getResourceId());
                    // add the folder to the vector of folders that has to be deleted
                    deletedFolders.addElement(currentFolder);
                } else if (currentFolder.getState() == C_STATE_CHANGED){
                    if(!currentFolder.isLocked()){
                        // lock the resource
                        lockResource(currentUser,deleteProject,currentFolder.getResourceName(),true);
                    }
                    // undo all changes in the folder
                    undoChanges(currentUser, deleteProject, currentFolder.getResourceName());
                } else if (currentFolder.getState() == C_STATE_DELETED){
                    // undelete the folder
                    undeleteResource(currentUser, deleteProject, currentFolder.getResourceName());
                    if(!currentFolder.isLocked()){
                        // lock the resource
                        lockResource(currentUser,deleteProject,currentFolder.getResourceName(),true);
                    }
                    // then undo all changes in the folder
                    undoChanges(currentUser, deleteProject, currentFolder.getResourceName());
                }
            }
            // now delete the folders in the vector
            for (int i = deletedFolders.size() - 1; i > -1; i--){
                CmsFolder delFolder = ((CmsFolder) deletedFolders.elementAt(i));
                m_dbAccess.removeFolder(id, delFolder);
            }
            // unlock all resources in the project
            m_dbAccess.unlockProject(deleteProject);
            this.clearResourceCache();
            // delete the project
            m_dbAccess.deleteProject(deleteProject);
            m_projectCache.remove(id);
        } else {
             throw new CmsException("[" + this.getClass().getName() + "] " + id,
                CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Deletes a propertyinformation for a file or folder.
     *
     * <B>Security</B>
     * Only the user is granted, who has the right to write the resource.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The name of the resource of which the propertyinformation
     * has to be read.
     * @param property The propertydefinition-name of which the propertyinformation has to be set.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public void deleteProperty(CmsUser currentUser, CmsProject currentProject,
                                      String resource, String property)
        throws CmsException {
        // read the resource
        CmsResource res = readFileHeader(currentUser,currentProject, resource);

        // check the security
        if( ! accessWrite(currentUser, currentProject, res) ) {
             throw new CmsException("[" + this.getClass().getName() + "] " + resource,
                CmsException.C_NO_ACCESS);
        }
        // read the metadefinition
        I_CmsResourceType resType = getResourceType(currentUser,currentProject,res.getType());
        CmsPropertydefinition metadef = readPropertydefinition(currentUser,currentProject,property, resType.getResourceTypeName());

        if(  (metadef != null)  ) {
            m_dbAccess.deleteProperty(property,currentProject.getId(),res,res.getType());
            // set the file-state to changed
            if(res.isFile()){
                m_dbAccess.writeFileHeader(currentProject, (CmsFile) res, true, currentUser.getId());
                if (res.getState()==C_STATE_UNCHANGED) {
                    res.setState(C_STATE_CHANGED);
                }
            } else {
                if (res.getState()==C_STATE_UNCHANGED) {
                    res.setState(C_STATE_CHANGED);
                }
                m_dbAccess.writeFolder(currentProject, readFolder(currentUser,currentProject, resource), true, currentUser.getId());
            }
            // update the cache
            this.clearResourceCache(resource);
            m_propertyCache.clear();
        } else {
            // yes - throw exception
             throw new CmsException("[" + this.getClass().getName() + "] " + resource,
                CmsException.C_UNKNOWN_EXCEPTION);
        }
    }
    /**
     * Delete the propertydefinition for the resource type.<BR/>
     *
     * <B>Security</B>
     * Only the admin can do this.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param name The name of the propertydefinition to read.
     * @param resourcetype The name of the resource type for which the
     * propertydefinition is valid.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void deletePropertydefinition(CmsUser currentUser, CmsProject currentProject,
                                     String name, String resourcetype)
        throws CmsException {
        // check the security
        if( isAdmin(currentUser, currentProject) ) {
            // first read and then delete the metadefinition.
            m_propertyDefVectorCache.clear();
            m_propertyDefCache.remove(name + (getResourceType(currentUser,currentProject,resourcetype)).getResourceType());
            m_dbAccess.deletePropertydefinition(
                readPropertydefinition(currentUser,currentProject,name,resourcetype));
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + name,
                CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Deletes a user from the Cms.
     *
     * Only a adminstrator can do this.<P/>
     *
     * <B>Security:</B>
     * Only users, which are in the group "administrators" are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param userId The Id of the user to be deleted.
     *
     * @exception CmsException Throws CmsException if operation was not succesfull.
     */
    public void deleteUser(CmsUser currentUser, CmsProject currentProject,
                           int userId)
        throws CmsException {
        CmsUser user = readUser(currentUser,currentProject,userId);
        deleteUser(currentUser,currentProject,user.getName());
    }
    /**
     * Deletes a user from the Cms.
     *
     * Only a adminstrator can do this.<P/>
     *
     * <B>Security:</B>
     * Only users, which are in the group "administrators" are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param name The name of the user to be deleted.
     *
     * @exception CmsException Throws CmsException if operation was not succesfull.
     */
    public void deleteUser(CmsUser currentUser, CmsProject currentProject,
                           String username)
        throws CmsException {
        // Test is this user is existing
        CmsUser user=readUser(currentUser,currentProject,username);

        // Check the security
        // Avoid to delete admin or guest-user
        if( isAdmin(currentUser, currentProject) &&
            !(username.equals(C_USER_ADMIN) || username.equals(C_USER_GUEST))) {
            m_dbAccess.deleteUser(username);
            // delete user from cache
            m_userCache.remove(username+user.getType());
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + username,
                CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Deletes a web user from the Cms.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param userId The Id of the user to be deleted.
     *
     * @exception CmsException Throws CmsException if operation was not succesfull.
     */
    public void deleteWebUser(CmsUser currentUser, CmsProject currentProject,
                           int userId)
        throws CmsException {
        CmsUser user = readUser(currentUser,currentProject,userId);
        m_dbAccess.deleteUser(user.getName());
        // delete user from cache
        m_userCache.remove(user.getName()+user.getType());
    }
    /**
     * Destroys the resource broker and required modules and connections.
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void destroy()
        throws CmsException {
        // destroy the db-access.
        m_dbAccess.destroy();
    }
    /**
     * Ends a task from the Cms.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param taskid The ID of the task to end.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void endTask(CmsUser currentUser, CmsProject currentProject, int taskid)
        throws CmsException {

        m_dbAccess.endTask(taskid);
        if(currentUser == null) {
            m_dbAccess.writeSystemTaskLog(taskid, "Task finished.");

        } else {
            m_dbAccess.writeSystemTaskLog(taskid,
                                          "Task finished by " +
                                          currentUser.getFirstname() + " " +
                                          currentUser.getLastname() + ".");
        }
    }
    /**
     * Exports cms-resources to zip.
     *
     * <B>Security:</B>
     * only Administrators can do this;
     *
     * @param currentUser user who requestd themethod
     * @param currentProject current project of the user
     * @param exportFile the name (absolute Path) of the export resource (zip)
     * @param exportPath the names (absolute Path) of folders and files which should be exported
     * @param cms the cms-object to use for the export.
     *
     * @exception Throws CmsException if something goes wrong.
     */
    public void exportResources(CmsUser currentUser,  CmsProject currentProject, String exportFile, String[] exportPaths, CmsObject cms)
        throws CmsException {
        if(isAdmin(currentUser, currentProject)) {
            new CmsExport(exportFile, exportPaths, cms);
        } else {
             throw new CmsException("[" + this.getClass().getName() + "] exportResources",
                 CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Exports cms-resources to zip.
     *
     * <B>Security:</B>
     * only Administrators can do this;
     *
     * @param currentUser user who requestd themethod
     * @param currentProject current project of the user
     * @param exportFile the name (absolute Path) of the export resource (zip)
     * @param exportPath the name (absolute Path) of folder from which should be exported
     * @param excludeSystem, decides whether to exclude the system
     * @param excludeUnchanged <code>true</code>, if unchanged files should be excluded.
     * @param cms the cms-object to use for the export.
     *
     * @exception Throws CmsException if something goes wrong.
     */
    public void exportResources(CmsUser currentUser,  CmsProject currentProject, String exportFile, String[] exportPaths, CmsObject cms, boolean excludeSystem, boolean excludeUnchanged)
        throws CmsException {
        if(isAdmin(currentUser, currentProject)) {
            new CmsExport(exportFile, exportPaths, cms, excludeSystem, excludeUnchanged);
        } else {
             throw new CmsException("[" + this.getClass().getName() + "] exportResources",
                 CmsException.C_NO_ACCESS);
        }
    }

    /**
     * Exports cms-resources to zip.
     *
     * <B>Security:</B>
     * only Administrators can do this;
     *
     * @param currentUser user who requestd themethod
     * @param currentProject current project of the user
     * @param exportFile the name (absolute Path) of the export resource (zip)
     * @param exportPath the name (absolute Path) of folder from which should be exported
     * @param excludeSystem, decides whether to exclude the system
     * @param excludeUnchanged <code>true</code>, if unchanged files should be excluded.
     * @param cms the cms-object to use for the export.
     *
     * @exception Throws CmsException if something goes wrong.
     */
    public void exportResources(CmsUser currentUser,  CmsProject currentProject, String exportFile, String[] exportPaths, CmsObject cms, boolean excludeSystem, boolean excludeUnchanged, boolean exportUserdata)
        throws CmsException {
        if(isAdmin(currentUser, currentProject)) {
            new CmsExport(exportFile, exportPaths, cms, excludeSystem, excludeUnchanged, null, exportUserdata);
        } else {
             throw new CmsException("[" + this.getClass().getName() + "] exportResources",
                 CmsException.C_NO_ACCESS);
        }
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
     * @exception Throws CmsException if something goes wrong.
     */
    public void exportModuledata(CmsUser currentUser,  CmsProject currentProject, String exportFile, String[] exportChannels, String[] exportModules, CmsObject cms)
        throws CmsException {
        if(isAdmin(currentUser, currentProject)) {
            new CmsExportModuledata(exportFile, exportChannels, exportModules, cms);
        } else {
             throw new CmsException("[" + this.getClass().getName() + "] exportModuledata",
                 CmsException.C_NO_ACCESS);
        }
    }
    // now private stuff

    /**
     * This method is called, when a resource was changed. Currently it counts the
     * changes.
     */
    protected void fileSystemChanged(boolean folderChanged) {
        // count only the changes - do nothing else!
        // in the future here will maybe a event-story be added
        m_fileSystemChanges++;
        if(folderChanged){
            m_fileSystemFolderChanges++;
        }
    }
    /**
     * Forwards a task to a new user.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param taskid The Id of the task to forward.
     * @param newRole The new Group for the task
     * @param newUser The new user who gets the task. if its "" the a new agent will automatic selected
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void forwardTask(CmsUser currentUser, CmsProject currentProject, int taskid,
                            String newRoleName, String newUserName)
        throws CmsException {

        CmsGroup newRole = m_dbAccess.readGroup(newRoleName);
        CmsUser newUser = null;
        if(newUserName.equals("")) {
            newUser = m_dbAccess.readUser(m_dbAccess.findAgent(newRole.getId()));
        } else {
            newUser =   m_dbAccess.readUser(newUserName, C_USER_TYPE_SYSTEMUSER);
        }

        m_dbAccess.forwardTask(taskid, newRole.getId(), newUser.getId());
        m_dbAccess.writeSystemTaskLog(taskid,
                                      "Task fowarded from " +
                                      currentUser.getFirstname() + " " +
                                      currentUser.getLastname() + " to " +
                                      newUser.getFirstname() + " " +
                                      newUser.getLastname() + ".");
    }
    /**
     * Returns all projects, which are owned by the user or which are accessible
     * for the group of the user.
     *
     * <B>Security</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     *
     * @return a Vector of projects.
     */
     public Vector getAllAccessibleProjects(CmsUser currentUser,
                                            CmsProject currentProject)
         throws CmsException {
        // get all groups of the user
        Vector groups = getGroupsOfUser(currentUser, currentProject,
                                        currentUser.getName());

        // get all projects which are owned by the user.
        Vector projects = m_dbAccess.getAllAccessibleProjectsByUser(currentUser);

        // get all projects, that the user can access with his groups.
        for(int i = 0; i < groups.size(); i++) {
            Vector projectsByGroup;
            // is this the admin-group?
            if( ((CmsGroup) groups.elementAt(i)).getName().equals(C_GROUP_ADMIN) ) {
                 // yes - all unlocked projects are accessible for him
                 projectsByGroup = m_dbAccess.getAllProjects(C_PROJECT_STATE_UNLOCKED);
            } else {
                // no - get all projects, which can be accessed by the current group
                projectsByGroup = m_dbAccess.getAllAccessibleProjectsByGroup((CmsGroup) groups.elementAt(i));
            }

            // merge the projects to the vector
            for(int j = 0; j < projectsByGroup.size(); j++) {
                // add only projects, which are new
                if(!projects.contains(projectsByGroup.elementAt(j))) {
                    projects.addElement(projectsByGroup.elementAt(j));
                }
            }
        }
        // return the vector of projects
        return(projects);
     }
    /**
     * Returns all projects, which are owned by the user or which are manageable
     * for the group of the user.
     *
     * <B>Security</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     *
     * @return a Vector of projects.
     */
     public Vector getAllManageableProjects(CmsUser currentUser,
                                            CmsProject currentProject)
         throws CmsException {
        // get all groups of the user
        Vector groups = getGroupsOfUser(currentUser, currentProject,
                                        currentUser.getName());

        // get all projects which are owned by the user.
        Vector projects = m_dbAccess.getAllAccessibleProjectsByUser(currentUser);

        // get all projects, that the user can manage with his groups.
        for(int i = 0; i < groups.size(); i++) {
            // get all projects, which can be managed by the current group
            Vector projectsByGroup;
            // is this the admin-group?
            if( ((CmsGroup) groups.elementAt(i)).getName().equals(C_GROUP_ADMIN) ) {
                 // yes - all unlocked projects are accessible for him
                 projectsByGroup = m_dbAccess.getAllProjects(C_PROJECT_STATE_UNLOCKED);
            } else {
                // no - get all projects, which can be accessed by the current group
                projectsByGroup = m_dbAccess.getAllAccessibleProjectsByManagerGroup((CmsGroup)groups.elementAt(i));
            }

            // merge the projects to the vector
            for(int j = 0; j < projectsByGroup.size(); j++) {
                // add only projects, which are new
                if(!projects.contains(projectsByGroup.elementAt(j))) {
                    projects.addElement(projectsByGroup.elementAt(j));
                }
            }
        }
        // remove the online-project, it is not manageable!
        projects.removeElement(onlineProject(currentUser, currentProject));
        // return the vector of projects
        return(projects);
     }

    /**
     * Returns a Vector with all projects from history
     *
     * @return Vector with all projects from history.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
     public Vector getAllBackupProjects() throws CmsException{
        Vector projects = new Vector();
        projects = m_dbAccess.getAllBackupProjects();
        return projects;
     }

    /**
     * Returns a Vector with all I_CmsResourceTypes.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     *
     * Returns a Hashtable with all I_CmsResourceTypes.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public Hashtable getAllResourceTypes(CmsUser currentUser,
                                         CmsProject currentProject)
        throws CmsException {
        // check, if the resourceTypes were read bevore
        if(m_resourceTypes == null) {
            // get the resourceTypes from the registry
            m_resourceTypes = new Hashtable();
            Vector resTypeNames = new Vector();
            Vector launcherTypes = new Vector();
            Vector launcherClass = new Vector();
            Vector resourceClass = new Vector();
            int resTypeCount = m_registry.getResourceTypes(resTypeNames, launcherTypes, launcherClass, resourceClass);
            for (int i = 0; i < resTypeCount; i++){
                // add the resource-type
                try{
                    Class c = Class.forName((String)resourceClass.elementAt(i));
                    I_CmsResourceType resTypeClass = (I_CmsResourceType) c.newInstance();
                    resTypeClass.init(i, Integer.parseInt((String)launcherTypes.elementAt(i)),
                                         (String)resTypeNames.elementAt(i),
                                         (String)launcherClass.elementAt(i));
                    m_resourceTypes.put((String)resTypeNames.elementAt(i), resTypeClass);
                }catch(Exception e){
                    e.printStackTrace();
                    throw new CmsException("[" + this.getClass().getName() + "] Error while getting ResourceType: " + (String)resTypeNames.elementAt(i) + " from registry ", CmsException.C_UNKNOWN_EXCEPTION );
                }
            }
        }
        // return the resource-types.
        return(m_resourceTypes);
    }

    /**
     * Returns informations about the cache<P/>
     *
     * <B>Security:</B>
     * All users are granted, except the anonymous user.
     *
     * @return A hashtable with informations about the cache.
     */
    public Hashtable getCacheInfo() {
    Hashtable info = new Hashtable();
    info.put("UserCache",""+m_userCache.size());
    info.put("GroupCache",""+m_groupCache.size());
    info.put("UserGroupCache",""+m_usergroupsCache.size());
    info.put("ResourceCache",""+m_resourceCache.size());
    info.put("SubResourceCache",""+m_subresCache.size());
    info.put("ProjectCache",""+m_projectCache.size());
    info.put("PropertyCache",""+m_propertyCache.size());
    info.put("PropertyDefinitionCache",""+m_propertyDefCache.size());
    info.put("PropertyDefinitionVectorCache",""+m_propertyDefVectorCache.size());
    info.put("AccessCache",""+m_accessCache.size());

    return info;
    }
    /**
     * Returns all child groups of a group<P/>
     *
     * <B>Security:</B>
     * All users are granted, except the anonymous user.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param groupname The name of the group.
     * @return groups A Vector of all child groups or null.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getChild(CmsUser currentUser, CmsProject currentProject,
                           String groupname)
        throws CmsException {
        // check security
        if( ! anonymousUser(currentUser, currentProject).equals( currentUser ) ) {
            return m_dbAccess.getChild(groupname);
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + groupname,
                CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Returns all child groups of a group<P/>
     * This method also returns all sub-child groups of the current group.
     *
     * <B>Security:</B>
     * All users are granted, except the anonymous user.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param groupname The name of the group.
     * @return groups A Vector of all child groups or null.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getChilds(CmsUser currentUser, CmsProject currentProject,
                            String groupname)
        throws CmsException {
        // check security
        if( ! anonymousUser(currentUser, currentProject).equals( currentUser ) ) {
            Vector childs=new Vector();
            Vector allChilds=new Vector();
            Vector subchilds=new Vector();
            CmsGroup group=null;

            // get all child groups if the user group
            childs=m_dbAccess.getChild(groupname);
            if (childs!=null) {
                allChilds=childs;
                // now get all subchilds for each group
                Enumeration enu=childs.elements();
                while (enu.hasMoreElements()) {
                    group=(CmsGroup)enu.nextElement();
                    subchilds=getChilds(currentUser,currentProject,group.getName());
                    //add the subchilds to the already existing groups
                    Enumeration enusub=subchilds.elements();
                    while (enusub.hasMoreElements()) {
                        group=(CmsGroup)enusub.nextElement();
                        allChilds.addElement(group);
                }
            }
        }
        return allChilds;
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + groupname,
                CmsException.C_NO_ACCESS);
        }
    }
    // Method to access the configuration

    /**
     * Method to access the configurations of the properties-file.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @return The Configurations of the properties-file.
     */
    public Configurations getConfigurations(CmsUser currentUser, CmsProject currentProject) {
        return m_configuration;
    }
    /**
     * Returns the list of groups to which the user directly belongs to<P/>
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param username The name of the user.
     * @return Vector of groups
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public Vector getDirectGroupsOfUser(CmsUser currentUser, CmsProject currentProject,
                                        String username)
        throws CmsException {
        return m_dbAccess.getGroupsOfUser(username);
    }

/**
 * Returns a Vector with all files of a folder.<br>
 *
 * Files of a folder can be read from an offline Project and the online Project.<br>
 *
 * <B>Security:</B>
 * Access is granted, if:
 * <ul>
 * <li>the user has access to the project</li>
 * <li>the user can read this resource</li>
 * </ul>
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param foldername the complete path to the folder.
 *
 * @return A Vector with all subfiles for the overgiven folder.
 *
 * @exception CmsException  Throws CmsException if operation was not succesful.
 */
public Vector getFilesInFolder(CmsUser currentUser, CmsProject currentProject, String foldername) throws CmsException
{
    return getFilesInFolder(currentUser, currentProject, foldername, false);
}

/**
 * Returns a Vector with all files of a folder.<br>
 *
 * Files of a folder can be read from an offline Project and the online Project.<br>
 *
 * <B>Security:</B>
 * Access is granted, if:
 * <ul>
 * <li>the user has access to the project</li>
 * <li>the user can read this resource</li>
 * </ul>
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param foldername the complete path to the folder.
 * @param includeDeleted Include the folder if it is deleted
 *
 * @return A Vector with all subfiles for the overgiven folder.
 *
 * @exception CmsException  Throws CmsException if operation was not succesful.
 */
public Vector getFilesInFolder(CmsUser currentUser, CmsProject currentProject, String foldername, boolean includeDeleted) throws CmsException{
    Vector files;

    // Todo: add caching for getFilesInFolder
    files=(Vector)m_subresCache.get(currentUser.getId()+"FILES"+foldername, currentProject.getId());
    if ((files==null) || (files.size()==0)) {
        // try to get the files in the current project
        try {
            files = helperGetFilesInFolder(currentUser, currentProject, foldername, includeDeleted);
        } catch (CmsException e) {
            //if access is denied to the folder, dont try to read them from the online project.)
            if (e.getType() == CmsException.C_ACCESS_DENIED)
                return new Vector(); //an empty vector.
            else
                //can't handle it here.
                throw e;
        }
        if (files == null){
            //we are not allowed to read the folder (folder deleted)
            return new Vector();
        }
        Vector onlineFiles = null;
        if (!currentProject.equals(onlineProject(currentUser, currentProject))){
            // this is not the onlineproject, get the files
            // from the onlineproject, too
            try {
                onlineFiles = helperGetFilesInFolder(currentUser, onlineProject(currentUser, currentProject), foldername,includeDeleted);
                // merge the resources
            } catch (CmsException exc){
                if (exc.getType() != CmsException.C_ACCESS_DENIED)
                    //cant handle it.
                    throw exc;
            }
        }
        if(onlineFiles != null){
            //if it was null, the folder was marked deleted -> no files in online project.
            files = mergeResources(files, onlineFiles);
        }
        m_subresCache.put(currentUser.getId()+"FILES"+foldername, currentProject.getId(), files);
    }
    return files;
}
/**
 * Returns a Vector with all resource-names that have set the given property to the given value.
 *
 * <B>Security:</B>
 * All users are granted.
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param foldername the complete path to the folder.
 * @param propertydef, the name of the propertydefinition to check.
 * @param property, the value of the property for the resource.
 *
 * @return Vector with all names of resources.
 *
 * @exception CmsException Throws CmsException if operation was not succesful.
 */
public Vector getFilesWithProperty(CmsUser currentUser, CmsProject currentProject, String propertyDefinition, String propertyValue) throws CmsException {
    return m_dbAccess.getFilesWithProperty(currentProject.getId(), propertyDefinition, propertyValue);
}
    /**
     * This method can be called, to determine if the file-system was changed
     * in the past. A module can compare its previosly stored number with this
     * returned number. If they differ, a change was made.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     *
     * @return the number of file-system-changes.
     */
    public long getFileSystemChanges(CmsUser currentUser, CmsProject currentProject) {
        return m_fileSystemChanges;
    }
    /**
     * This method can be called, to determine if the file-system was changed
     * in the past. A module can compare its previosly stored number with this
     * returned number. If they differ, a change was made.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     *
     * @return the number of file-system-changes.
     */
    public long getFileSystemFolderChanges(CmsUser currentUser, CmsProject currentProject) {
        return m_fileSystemFolderChanges;
    }
/**
 * Returns a Vector with the complete folder-tree for this project.<br>
 *
 * Subfolders can be read from an offline project and the online project. <br>
 *
 * <B>Security:</B>
 * Access is granted, if:
 * <ul>
 * <li>the user has access to the project</li>
 * <li>the user can read this resource</li>
 * </ul>
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param rootName The name of the root, e.g. /default/vfs
 * @return subfolders A Vector with the complete folder-tree for this project.
 *
 * @exception CmsException  Throws CmsException if operation was not succesful.
 */
public Vector getFolderTree(CmsUser currentUser, CmsProject currentProject, String rootName) throws CmsException {
    // try to read from cache
    Vector retValue = (Vector)m_subresCache.get(currentUser.getId()+"TREE"+rootName, currentProject.getId());
    if (retValue == null || retValue.size() == 0){
        Vector resources = m_dbAccess.getFolderTree(currentProject.getId(), rootName);
        retValue = new Vector(resources.size());
        String lastcheck = "#"; // just a char that is not valid in a filename

        //make sure that we have access to all these.
        for (Enumeration e = resources.elements(); e.hasMoreElements();) {
            CmsResource res = (CmsResource) e.nextElement();
            if (!res.getAbsolutePath().startsWith(lastcheck)) {
                if (accessOther(currentUser, currentProject, res, C_ACCESS_PUBLIC_READ + C_ACCESS_PUBLIC_VISIBLE) ||
                    accessOwner(currentUser, currentProject, res, C_ACCESS_OWNER_READ + C_ACCESS_OWNER_VISIBLE) ||
                    accessGroup(currentUser, currentProject, res, C_ACCESS_GROUP_READ + C_ACCESS_GROUP_VISIBLE)) {

                    retValue.addElement(res);

                } else {
                    lastcheck = res.getAbsolutePath();
                }
            }
        }
        m_subresCache.put(currentUser.getId()+"TREE"+rootName, currentProject.getId(), retValue);
    }
    return retValue;
}
    /**
     * Returns all groups<P/>
     *
     * <B>Security:</B>
     * All users are granted, except the anonymous user.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @return users A Vector of all existing groups.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
     public Vector getGroups(CmsUser currentUser, CmsProject currentProject)
        throws CmsException {
        // check security
        if( ! anonymousUser(currentUser, currentProject).equals( currentUser ) ) {
            return m_dbAccess.getGroups();
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + currentUser.getName(),
                CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Returns a list of groups of a user.<P/>
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param username The name of the user.
     * @return Vector of groups
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public Vector getGroupsOfUser(CmsUser currentUser, CmsProject currentProject,
                                  String username)
        throws CmsException {

         Vector allGroups;

         allGroups=(Vector)m_usergroupsCache.get(C_USER+username);
         if ((allGroups==null) || (allGroups.size()==0)) {

         CmsGroup subGroup;
         CmsGroup group;
         // get all groups of the user
         Vector groups=m_dbAccess.getGroupsOfUser(username);
         allGroups = new Vector();
         // now get all childs of the groups
         Enumeration enu = groups.elements();
         while (enu.hasMoreElements()) {
             group=(CmsGroup)enu.nextElement();

             subGroup=getParent(currentUser, currentProject,group.getName());
             while((subGroup != null) && (!allGroups.contains(subGroup))) {

                 allGroups.addElement(subGroup);
                 // read next sub group
                 subGroup = getParent(currentUser, currentProject,subGroup.getName());
             }

             if(!allGroups.contains(group)) {
                allGroups.add(group);
             }
         }
         m_usergroupsCache.put(C_USER+username,allGroups);
         }
         return allGroups;
    }

    /**
     * Checks which Group can read the resource and all the parent folders.
     *
     * @param projectid the project to check the permission.
     * @param res The resource name to be checked.
     * @return The Group Id of the Group which can read the resource.
     *          null for all Groups and
     *          Admingroup for no Group.
     */
    public String getReadingpermittedGroup(int projectId, String resource) throws CmsException {
        return m_dbAccess.getReadingpermittedGroup(projectId, resource);
    }

/**
 * Returns the parent group of a group<P/>
 *
 * <B>Security:</B>
 * All users are granted.
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param groupname The name of the group.
 * @return group The parent group or null.
 * @exception CmsException Throws CmsException if operation was not succesful.
 */
public CmsGroup getParent(CmsUser currentUser, CmsProject currentProject, String groupname) throws CmsException
{
    CmsGroup group = readGroup(currentUser, currentProject, groupname);
    if (group.getParentId() == C_UNKNOWN_ID)
    {
        return null;
    }

    // try to read from cache
    CmsGroup parent = (CmsGroup) m_groupCache.get(group.getParentId());
    if (parent == null)
    {
        parent = m_dbAccess.readGroup(group.getParentId());
        m_groupCache.put(group.getParentId(), parent);
    }
    return parent;
}
    /**
     * Returns the parent resource of a resouce.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param filename The name of the file to be read.
     *
     * @return The file read from the Cms.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public CmsResource getParentResource(CmsUser currentUser, CmsProject currentProject,
                                         String resourcename)
        throws CmsException {

        // TODO: this can maybe done via the new parent id'd
        CmsResource theResource = readFileHeader(currentUser, currentProject, resourcename);
        String parentresourceName = theResource.getRootName()+theResource.getParent();
        return readFileHeader(currentUser, currentProject, parentresourceName);
    }
     /**
     * Gets the Registry.<BR/>
     *
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param cms The actual CmsObject
     * @exception Throws CmsException if access is not allowed.
     */

     public I_CmsRegistry getRegistry(CmsUser currentUser, CmsProject currentProject, CmsObject cms)
        throws CmsException {
        return m_registry.clone(cms);
     }
/**
 * Returns a Vector with the subresources for a folder.<br>
 *
 * <B>Security:</B>
 * Access is granted, if:
 * <ul>
 * <li>the user has access to the project</li>
 * <li>the user can read and view this resource</li>
 * </ul>
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param folder The name of the folder to get the subresources from.
 *
 * @return subfolders A Vector with resources.
 *
 * @exception CmsException  Throws CmsException if operation was not succesful.
 */
public Vector getResourcesInFolder(CmsUser currentUser, CmsProject currentProject, String folder) throws CmsException {
    CmsFolder offlineFolder = null;
    Vector resources = new Vector();
    Vector retValue = new Vector();
    try {
        offlineFolder = readFolder(currentUser, currentProject, folder);
        if (offlineFolder.getState() == C_STATE_DELETED) {
            offlineFolder = null;
        }
    } catch (CmsException exc) {
        // ignore the exception - folder was not found in this project
    }
    if (offlineFolder == null) {
        // the folder is not existent
        throw new CmsException("[" + this.getClass().getName() + "] " + folder, CmsException.C_NOT_FOUND);
    } else {
        // try to read from cache
        retValue = (Vector)m_subresCache.get(currentUser.getId()+"RESOURCES"+folder, currentProject.getId());
        if(retValue == null || retValue.size() == 0){
            resources = m_dbAccess.getResourcesInFolder(currentProject.getId(), offlineFolder);
            retValue = new Vector(resources.size());
            //make sure that we have access to all these.
            for (Enumeration e = resources.elements(); e.hasMoreElements();) {
                CmsResource res = (CmsResource) e.nextElement();
                if (accessOther(currentUser, currentProject, res, C_ACCESS_PUBLIC_READ + C_ACCESS_PUBLIC_VISIBLE) ||
                    accessOwner(currentUser, currentProject, res, C_ACCESS_OWNER_READ + C_ACCESS_OWNER_VISIBLE) ||
                    accessGroup(currentUser, currentProject, res, C_ACCESS_GROUP_READ + C_ACCESS_GROUP_VISIBLE)) {
                    retValue.addElement(res);
                }
            }
            m_subresCache.put(currentUser.getId()+"RESOURCES"+folder, currentProject.getId(), retValue);
        }
    }
    return retValue;
}

    /**
     * Returns a Vector with all resources of the given type that have set the given property to the given value.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param propertyDefinition, the name of the propertydefinition to check.
     * @param propertyValue, the value of the property for the resource.
     * @param resourceType The resource type of the resource
     *
     * @return Vector with all resources.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getResourcesWithProperty(CmsUser currentUser, CmsProject currentProject, String propertyDefinition,
                                           String propertyValue, int resourceType) throws CmsException {
        return m_dbAccess.getResourcesWithProperty(currentProject.getId(), propertyDefinition, propertyValue, resourceType);
    }

    /**
     * Returns a Vector with all resources of the given type that have set the given property to the given value.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param propertyDefinition, the name of the propertydefinition to check.
     *
     * @return Vector with all resources.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getResourcesWithProperty(CmsUser currentUser, CmsProject currentProject, String propertyDefinition)
                                           throws CmsException {
        return m_dbAccess.getResourcesWithProperty(currentProject.getId(), propertyDefinition);
    }

    /**
     * Returns a I_CmsResourceType.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resourceType the id of the resourceType to get.
     *
     * Returns a I_CmsResourceType.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public I_CmsResourceType getResourceType(CmsUser currentUser,
                                             CmsProject currentProject,
                                             int resourceType)
        throws CmsException {
        // try to get the resource-type
        Hashtable types = getAllResourceTypes(currentUser, currentProject);
        Enumeration keys = types.keys();
        I_CmsResourceType currentType;
        while(keys.hasMoreElements()) {
            currentType = (I_CmsResourceType) types.get(keys.nextElement());
            if(currentType.getResourceType() == resourceType) {
                return(currentType);
            }
        }
        // was not found - throw exception
        throw new CmsException("[" + this.getClass().getName() + "] " + resourceType,
            CmsException.C_NOT_FOUND);
    }
    /**
     * Returns a I_CmsResourceType.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resourceType the name of the resource to get.
     *
     * Returns a I_CmsResourceType.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public I_CmsResourceType getResourceType(CmsUser currentUser,
                                             CmsProject currentProject,
                                             String resourceType)
        throws CmsException {
        // try to get the resource-type
        try {
            I_CmsResourceType type = (I_CmsResourceType)getAllResourceTypes(currentUser, currentProject).get(resourceType);
            if(type == null) {
                throw new CmsException("[" + this.getClass().getName() + "] " + resourceType,
                    CmsException.C_NOT_FOUND);
            }
            return type;
        } catch(NullPointerException exc) {
            // was not found - throw exception
            throw new CmsException("[" + this.getClass().getName() + "] " + resourceType,
                CmsException.C_NOT_FOUND);
        }
    }

    /**
     * Returns a Vector with all subfolders.<br>
     *
     * Subfolders can be read from an offline project and the online project. <br>
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can read this resource</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param foldername the complete path to the folder.
     *
     * @return subfolders A Vector with all subfolders for the given folder.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public Vector getSubFolders(CmsUser currentUser, CmsProject currentProject,
                                String foldername) throws CmsException {
        return getSubFolders(currentUser, currentProject, foldername, false);
    }
    /**
     * Returns a Vector with all subfolders.<br>
     *
     * Subfolders can be read from an offline project and the online project. <br>
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can read this resource</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param foldername the complete path to the folder.
     * @param includeDeleted Include the folder if it is deleted
     *
     * @return subfolders A Vector with all subfolders for the given folder.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public Vector getSubFolders(CmsUser currentUser, CmsProject currentProject,
                                String foldername, boolean includeDeleted)
        throws CmsException {
        Vector folders = new Vector();

        // Todo: add caching for getSubFolders
        folders=(Vector)m_subresCache.get(currentUser.getId()+"FOLDERS"+foldername, currentProject.getId());

        if ((folders==null) || (folders.size()==0)){

            folders=new Vector();
            // try to get the folders in the current project
            try {
                folders = helperGetSubFolders(currentUser, currentProject, foldername);
            } catch (CmsException exc) {
                // no folders, ignoring them
            }
            if( !currentProject.equals(onlineProject(currentUser, currentProject))) {
                // this is not the onlineproject, get the files
                // from the onlineproject, too
                try {
                    Vector onlineFolders =
                    helperGetSubFolders(currentUser,
                                        onlineProject(currentUser, currentProject),
                                        foldername);
                    // merge the resources
                    folders = mergeResources(folders, onlineFolders);
                } catch(CmsException exc) {
                    // no onlinefolders, ignoring them
                }
            }
            m_subresCache.put(currentUser.getId()+"FOLDERS"+foldername, currentProject.getId(), folders);
        }

        // return the folders
        return(folders);
    }
    /**
     * Get a parameter value for a task.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param taskId The Id of the task.
     * @param parName Name of the parameter.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public String getTaskPar(CmsUser currentUser, CmsProject currentProject,
                             int taskId, String parName)
        throws CmsException {
        return m_dbAccess.getTaskPar(taskId, parName);
    }
    /**
     * Get the template task id fo a given taskname.
     *
     * @param taskName Name of the Task
     *
     * @return id from the task template
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public int getTaskType(String taskName)
        throws CmsException {
        return m_dbAccess.getTaskType(taskName);
    }
    /**
     * Returns all users<P/>
     *
     * <B>Security:</B>
     * All users are granted, except the anonymous user.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @return users A Vector of all existing users.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getUsers(CmsUser currentUser, CmsProject currentProject)
        throws CmsException {
        // check security
        if( ! anonymousUser(currentUser, currentProject).equals( currentUser ) ) {
            return m_dbAccess.getUsers(C_USER_TYPE_SYSTEMUSER);
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + currentUser.getName(),
                CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Returns all users from a given type<P/>
     *
     * <B>Security:</B>
     * All users are granted, except the anonymous user.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param type The type of the users.
     * @return users A Vector of all existing users.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getUsers(CmsUser currentUser, CmsProject currentProject, int type)
        throws CmsException {
        // check security
        if( ! anonymousUser(currentUser, currentProject).equals( currentUser ) ) {
            return m_dbAccess.getUsers(type);
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + currentUser.getName(),
                CmsException.C_NO_ACCESS);
        }
    }
     /**
     * Returns all users from a given type that start with a specified string<P/>
     *
     * <B>Security:</B>
     * All users are granted, except the anonymous user.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param type The type of the users.
     * @param namestart The filter for the username
     * @return users A Vector of all existing users.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getUsers(CmsUser currentUser, CmsProject currentProject, int type, String namestart)
        throws CmsException {
        // check security
        if( ! anonymousUser(currentUser, currentProject).equals( currentUser ) ) {
            return m_dbAccess.getUsers(type,namestart);
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + currentUser.getName(),
                CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Returns a list of users in a group.<P/>
     *
     * <B>Security:</B>
     * All users are granted, except the anonymous user.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param groupname The name of the group to list users from.
     * @return Vector of users.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getUsersOfGroup(CmsUser currentUser, CmsProject currentProject,
                                  String groupname)
        throws CmsException {
        // check the security
        if( ! anonymousUser(currentUser, currentProject).equals( currentUser ) ) {
            return m_dbAccess.getUsersOfGroup(groupname, C_USER_TYPE_SYSTEMUSER);
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + groupname,
                CmsException.C_NO_ACCESS);
        }
    }

    /**
     * Gets all users with a certain Lastname.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param Lastname      the start of the users lastname
     * @param UserType      webuser or systemuser
     * @param UserStatus    enabled, disabled
     * @param wasLoggedIn   was the user ever locked in?
     * @param nMax          max number of results
     *
     * @return the users.
     *
     * @exception CmsException if operation was not successful.
     */
    public Vector getUsersByLastname(CmsUser currentUser,
                                     CmsProject currentProject, String Lastname,
                                     int UserType, int UserStatus,
                                     int wasLoggedIn, int nMax)
        throws CmsException {
        // check security
        if( ! anonymousUser(currentUser, currentProject).equals( currentUser )){
            return m_dbAccess.getUsersByLastname(Lastname, UserType, UserStatus,
                                                 wasLoggedIn, nMax);
        } else {
            throw new CmsException(
                "[" + this.getClass().getName() + "] " + currentUser.getName(),
                CmsException.C_NO_ACCESS);
        }
    }

    /**
     * A helper method for this resource-broker.
     * Returns a Vector with all files of a folder.
     * The method does not read any files from the parrent folder,
     * and do also return deleted files.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param foldername the complete path to the folder.
     *
     * @return subfiles A Vector with all subfiles for the overgiven folder.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    protected Vector helperGetFilesInFolder(CmsUser currentUser,
                                          CmsProject currentProject,
                                          String foldername, boolean includeDeleted)
            throws CmsException {
            // get the folder
            CmsFolder cmsFolder = null;
            try {
                cmsFolder = m_dbAccess.readFolder(currentProject.getId(), foldername);
            } catch(CmsException exc) {
                if(exc.getType() == exc.C_NOT_FOUND) {
                    // ignore the exception - file dosen't exist in this project
                    return new Vector(); //just an empty vector.
                } else {
                    throw exc;
                }
            }

            if ((cmsFolder.getState() == I_CmsConstants.C_STATE_DELETED) && (!includeDeleted))
            {
                 //indicate that the folder was found, but deleted, and resources are not avaiable.
                 return null;
            }

                Vector _files = m_dbAccess.getFilesInFolder(currentProject.getId(),cmsFolder);
                Vector files = new Vector(_files.size());

                //make sure that we have access to all these.
                for (Enumeration e = _files.elements();e.hasMoreElements();)
                {
                    CmsFile file = (CmsFile) e.nextElement();
                    if( accessOther(currentUser, currentProject, (CmsResource)file, C_ACCESS_PUBLIC_READ) ||
                            accessOwner(currentUser, currentProject, (CmsResource)file, C_ACCESS_OWNER_READ) ||
                            accessGroup(currentUser, currentProject, (CmsResource)file, C_ACCESS_GROUP_READ) )
                    {
                        files.addElement(file);
                    }
                }
                return files;
            }
    /**
     * A helper method for this resource-broker.
     * Returns a Hashtable with all subfolders.<br>
     *
     * Subfolders can be read from an offline project and the online project. <br>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project to read the folders from.
     * @param foldername the complete path to the folder.
     *
     * @return subfolders A Hashtable with all subfolders for the given folder.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    protected Vector helperGetSubFolders(CmsUser currentUser,
                                       CmsProject currentProject,
                                       String foldername)
        throws CmsException{

        CmsFolder cmsFolder = m_dbAccess.readFolder(currentProject.getId(),foldername);
        if( accessRead(currentUser, currentProject, (CmsResource)cmsFolder) ) {
            // acces to all subfolders was granted - return the sub-folders.
            Vector folders = m_dbAccess.getSubFolders(currentProject.getId(),cmsFolder);
            CmsFolder folder;
            for(int z=0 ; z < folders.size() ; z++) {
                // read the current folder
                folder = (CmsFolder)folders.elementAt(z);
                // check the readability for the folder
                if( !( accessOther(currentUser, currentProject, (CmsResource)folder, C_ACCESS_PUBLIC_READ) ||
                       accessOwner(currentUser, currentProject, (CmsResource)folder, C_ACCESS_OWNER_READ) ||
                       accessGroup(currentUser, currentProject, (CmsResource)folder, C_ACCESS_GROUP_READ) ) ) {
                    // access to the folder was not granted delete him
                    folders.removeElementAt(z);
                    // correct the index
                    z--;
                }
            }
            return folders;
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + foldername,
                CmsException.C_ACCESS_DENIED);
        }
    }
    /**
     * Imports a import-resource (folder or zipfile) to the cms.
     *
     * <B>Security:</B>
     * only Administrators can do this;
     *
     * @param currentUser user who requestd themethod
     * @param currentProject current project of the user
     * @param importFile the name (absolute Path) of the import resource (zip or folder)
     * @param importPath the name (absolute Path) of folder in which should be imported
     * @param cms the cms-object to use for the import.
     *
     * @exception Throws CmsException if something goes wrong.
     */
    public void importFolder(CmsUser currentUser,  CmsProject currentProject, String importFile, String importPath, CmsObject cms)
        throws CmsException {
        if(isAdmin(currentUser, currentProject)) {
            new CmsImportFolder(importFile, importPath, cms);
        } else {
             throw new CmsException("[" + this.getClass().getName() + "] importResources",
                 CmsException.C_NO_ACCESS);
        }
    }
    // Methods working with database import and export

    /**
     * Imports a import-resource (folder or zipfile) to the cms.
     *
     * <B>Security:</B>
     * only Administrators can do this;
     *
     * @param currentUser user who requestd themethod
     * @param currentProject current project of the user
     * @param importFile the name (absolute Path) of the import resource (zip or folder)
     * @param importPath the name (absolute Path) of folder in which should be imported
     * @param cms the cms-object to use for the import.
     *
     * @exception Throws CmsException if something goes wrong.
     */
    public void importResources(CmsUser currentUser,  CmsProject currentProject, String importFile, String importPath, CmsObject cms)
        throws CmsException {
        if(isAdmin(currentUser, currentProject)) {
            // get the first node of the manifest to check if its an import of resources
            // or moduledata
            String firstTag = this.getFirstTagFromManifest(importFile);
            if(I_CmsConstants.C_EXPORT_TAG_MODULEXPORT.equals(firstTag)){
                CmsImportModuledata imp = new CmsImportModuledata(importFile, importPath, cms);
                imp.importModuledata();
            } else {
                CmsImport imp = new CmsImport(importFile, importPath, cms);
                imp.importResources();
            }
        } else {
             throw new CmsException("[" + this.getClass().getName() + "] importResources",
                 CmsException.C_NO_ACCESS);
        }
    }
    // Internal ResourceBroker methods

    /**
     * Initializes the resource broker and sets up all required modules and connections.
     * @param config The OpenCms configuration.
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void init(Configurations config)
        throws CmsException {

        // Store the configuration.
        m_configuration = config;

        // store the limited workplace port
        m_limitedWorkplacePort = config.getInteger("workplace.limited.port", -1);

        if (config.getString("history.enabled", "true").toLowerCase().equals("false")) {
            m_enableHistory = false;
        }
        // initialize the access-module.
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, "[CmsResourceBroker] init the dbaccess-module.");
        }
        m_dbAccess = createDbAccess(config);

        // initalize the caches
        m_userCache=new CmsCache(config.getInteger(C_CONFIGURATION_CACHE + ".user", 50));
        m_groupCache = new CmsCache(config.getInteger(C_CONFIGURATION_CACHE + ".group", 50));
        m_usergroupsCache = new CmsCache(config.getInteger(C_CONFIGURATION_CACHE + ".usergroups", 50));
        m_projectCache = new CmsCache(config.getInteger(C_CONFIGURATION_CACHE + ".project", 50));
        m_resourceCache=new CmsResourceCache(config.getInteger(C_CONFIGURATION_CACHE + ".resource", 1000));
        m_subresCache = new CmsResourceCache(config.getInteger(C_CONFIGURATION_CACHE + ".subres", 100));
        m_propertyCache = new CmsCache(config.getInteger(C_CONFIGURATION_CACHE + ".property", 1000));
        m_propertyDefCache = new CmsCache(config.getInteger(C_CONFIGURATION_CACHE + ".propertydef", 100));
        m_propertyDefVectorCache = new CmsCache(config.getInteger(C_CONFIGURATION_CACHE + ".propertyvectordef", 100));
        m_accessCache = new CmsCache(config.getInteger(C_CONFIGURATION_CACHE + ".access", 1000));
        m_cachelimit = config.getInteger(C_CONFIGURATION_CACHE + ".maxsize", 20000);
        m_refresh=config.getString(C_CONFIGURATION_CACHE + ".refresh", "");

        // initialize the registry#
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, "[CmsResourceBroker] init registry.");
        }

        try {
            m_registry= new CmsRegistry(CmsBase.getAbsolutePath(config.getString(C_CONFIGURATION_REGISTRY)));
        }
        catch (CmsException ex) {
            throw ex;
        }
        catch(Exception ex) {
            // init of registry failed - throw exception
            throw new CmsException("Init of registry failed", CmsException.C_REGISTRY_ERROR, ex);
        }
    }
    /**
     * Determines, if the users current group is the admin-group.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @return true, if the users current group is the admin-group,
     * else it returns false.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public boolean isAdmin(CmsUser currentUser, CmsProject currentProject)
        throws CmsException {
        return userInGroup(currentUser, currentProject,currentUser.getName(), C_GROUP_ADMIN);
    }

    /**
     * Determines, if the users may manage a project.<BR/>
     * Only the manager of a project may publish it.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @return true, if the may manage this project.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public boolean isManagerOfProject(CmsUser currentUser, CmsProject currentProject)
        throws CmsException {
        // is the user owner of the project?
        if( currentUser.getId() == currentProject.getOwnerId() ) {
            // YES
            return true;
        }
        if (isAdmin(currentUser, currentProject)){
            return true;
        }
        // get all groups of the user
        Vector groups = getGroupsOfUser(currentUser, currentProject,
                                        currentUser.getName());

        for(int i = 0; i < groups.size(); i++) {
            // is this a managergroup for this project?
            if( ((CmsGroup)groups.elementAt(i)).getId() ==
                currentProject.getManagerGroupId() ) {
                // this group is manager of the project
                return true;
            }
        }

        // this user is not manager of this project
        return false;
    }
    /**
     * Determines, if the users current group is the projectleader-group.<BR/>
     * All projectleaders can create new projects, or close their own projects.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @return true, if the users current group is the projectleader-group,
     * else it returns false.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public boolean isProjectManager(CmsUser currentUser, CmsProject currentProject)
        throws CmsException {
        return userInGroup(currentUser, currentProject,currentUser.getName(), C_GROUP_PROJECTLEADER);
    }

    /**
     * Determines, if the users current group is the projectleader-group.<BR/>
     * All projectleaders can create new projects, or close their own projects.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @return true, if the users current group is the projectleader-group,
     * else it returns false.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public boolean isUser(CmsUser currentUser, CmsProject currentProject)
        throws CmsException {
        return userInGroup(currentUser, currentProject,currentUser.getName(), C_GROUP_USERS);
    }
    /**
     * Returns the user, who had locked the resource.<BR/>
     *
     * A user can lock a resource, so he is the only one who can write this
     * resource. This methods checks, if a resource was locked.
     *
     * @param user The user who wants to lock the file.
     * @param project The project in which the resource will be used.
     * @param resource The resource.
     *
     * @return the user, who had locked the resource.
     *
     * @exception CmsException will be thrown, if the user has not the rights
     * for this resource.
     */
    public CmsUser lockedBy(CmsUser currentUser, CmsProject currentProject,
                              CmsResource resource)
        throws CmsException {
        return readUser(currentUser,currentProject,resource.isLockedBy() ) ;
    }
    /**
     * Returns the user, who had locked the resource.<BR/>
     *
     * A user can lock a resource, so he is the only one who can write this
     * resource. This methods checks, if a resource was locked.
     *
     * @param user The user who wants to lock the file.
     * @param project The project in which the resource will be used.
     * @param resource The complete path to the resource.
     *
     * @return the user, who had locked the resource.
     *
     * @exception CmsException will be thrown, if the user has not the rights
     * for this resource.
     */
    public CmsUser lockedBy(CmsUser currentUser, CmsProject currentProject,
                              String resource)
        throws CmsException {
        return readUser(currentUser,currentProject,readFileHeader(currentUser, currentProject, resource).isLockedBy() ) ;
    }
    /**
     * Locks a resource.<br>
     *
     * Only a resource in an offline project can be locked. The state of the resource
     * is set to CHANGED (1).
     * If the content of this resource is not exisiting in the offline project already,
     * it is read from the online project and written into the offline project.
     * A user can lock a resource, so he is the only one who can write this
     * resource. <br>
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can write the resource</li>
     * <li>the resource is not locked by another user</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The complete path to the resource to lock.
     * @param force If force is true, a existing locking will be oberwritten.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     * It will also be thrown, if there is a existing lock
     * and force was set to false.
     */
    public void lockResource(CmsUser currentUser, CmsProject currentProject,
                             String resourcename, boolean force)
        throws CmsException {

        CmsResource  cmsResource=null;

        // read the resource, that should be locked
        if (resourcename.endsWith("/")) {
              cmsResource = (CmsFolder)readFolder(currentUser,currentProject,resourcename);
             } else {
              cmsResource = (CmsFile)readFileHeader(currentUser,currentProject,resourcename);
        }
        // Can't lock what isn't there
        if (cmsResource == null) throw new CmsException(CmsException.C_NOT_FOUND);
        // check, if the resource is in the offline-project
        if(cmsResource.getProjectId() != currentProject.getId()) {
            // the resource is not in the current project and can't be locked - so ignore.
            return;
        }
        // check, if the user may lock the resource
        if( accessLock(currentUser, currentProject, cmsResource) ) {
            if(cmsResource.isLocked()) {
                // if the force switch is not set, throw an exception
                if (force==false) {
                    throw new CmsException("["+this.getClass().getName()+"] "+resourcename,CmsException.C_LOCKED);
                }
            }

            // lock the resource
            cmsResource.setLocked(currentUser.getId());
            cmsResource.setLockedInProject(currentProject.getId());
            //update resource
            m_dbAccess.updateLockstate(cmsResource, currentProject.getId());
            // update the cache
            this.clearResourceCache(resourcename);

            // if this resource is a folder -> lock all subresources, too
            if(cmsResource.isFolder()) {
                Vector files = getFilesInFolder(currentUser,currentProject, cmsResource.getResourceName());
                Vector folders = getSubFolders(currentUser,currentProject, cmsResource.getResourceName());
                CmsResource currentResource;

                // lock all files in this folder
                for(int i = 0; i < files.size(); i++ ) {
                    currentResource = (CmsResource)files.elementAt(i);
                    if (currentResource.getState() != C_STATE_DELETED) {
                        lockResource(currentUser, currentProject, currentResource.getResourceName(), true);
                    }
                }

                // lock all files in this folder
                for(int i = 0; i < folders.size(); i++) {
                    currentResource = (CmsResource)folders.elementAt(i);
                    if (currentResource.getState() != C_STATE_DELETED) {
                        lockResource(currentUser, currentProject, currentResource.getResourceName(), true);
                    }
                }
            }
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + resourcename,
                CmsException.C_NO_ACCESS);
        }
    }
    //  Methods working with user and groups

    /**
     * Logs a user into the Cms, if the password is correct.
     *
     * <B>Security</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param username The name of the user to be returned.
     * @param password The password of the user to be returned.
     * @return the logged in user.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public CmsUser loginUser(CmsUser currentUser, CmsProject currentProject,
                               String username, String password)
        throws CmsException {

        // we must read the user from the dbAccess to avoid the cache
        CmsUser newUser = m_dbAccess.readUser(username, password, C_USER_TYPE_SYSTEMUSER);

        // is the user enabled?
        if( newUser.getFlags() == C_FLAG_ENABLED ) {
            // Yes - log him in!
            // first write the lastlogin-time.
            newUser.setLastlogin(new Date().getTime());
            // write the user back to the cms.
            m_dbAccess.writeUser(newUser);
            // update cache
            m_userCache.put(newUser.getName()+newUser.getType(),newUser);
            return(newUser);
        } else {
            // No Access!
            throw new CmsException("[" + this.getClass().getName() + "] " + username,
                CmsException.C_NO_ACCESS );
        }
    }
     /**
     * Logs a web user into the Cms, if the password is correct.
     *
     * <B>Security</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param username The name of the user to be returned.
     * @param password The password of the user to be returned.
     * @return the logged in user.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public CmsUser loginWebUser(CmsUser currentUser, CmsProject currentProject,
                               String username, String password)
        throws CmsException {

        // we must read the user from the dbAccess to avoid the cache
        CmsUser newUser = m_dbAccess.readUser(username, password, C_USER_TYPE_WEBUSER);

        // is the user enabled?
        if( newUser.getFlags() == C_FLAG_ENABLED ) {
            // Yes - log him in!
            // first write the lastlogin-time.
            newUser.setLastlogin(new Date().getTime());
            // write the user back to the cms.
            m_dbAccess.writeUser(newUser);
            // update cache
            m_userCache.put(newUser.getName()+newUser.getType(),newUser);
            return(newUser);
        } else {
            // No Access!
            throw new CmsException("[" + this.getClass().getName() + "] " + username,
                CmsException.C_NO_ACCESS );
        }
    }
    /**
     * Merges two resource-vectors into one vector.
     * All offline-resources will be putted to the return-vector. All additional
     * online-resources will be putted to the return-vector, too. All online resources,
     * which are present in the offline-vector will be ignored.
     *
     *
     * @param offline The vector with the offline resources.
     * @param online The vector with the online resources.
     * @return The merged vector.
     */
    protected Vector mergeResources(Vector offline, Vector online) {


        //dont do anything if any of the given vectors are empty or null.
        if ((offline == null) || (offline.size() == 0)) return (online!=null)?online:new Vector();
        if ((online == null) || (online.size() == 0)) return (offline!=null)?offline:new Vector();

        // create a vector for the merged offline

        //remove all objects in the online vector that are present in the offline vector.
        for (Enumeration e=offline.elements();e.hasMoreElements();)
        {
            CmsResource cr = (CmsResource) e.nextElement();
            Resource r = new Resource(cr.getResourceName());
            online.removeElement(r);
        }

        //merge the two vectors. If both vectors were sorted, the mereged vector will remain sorted.

        Vector merged = new Vector(offline.size() + online.size());
      int offIndex = 0;
      int onIndex = 0;

      while ((offIndex < offline.size()) || (onIndex < online.size()))
      {
        if (offIndex >= offline.size())
        {
            merged.addElement(online.elementAt(onIndex++));
            continue;
        }
        if (onIndex >= online.size())
        {
            merged.addElement(offline.elementAt(offIndex++));
            continue;
        }
        String on =  ((CmsResource)online.elementAt(onIndex)).getResourceName();
        String off = ((CmsResource)offline.elementAt(offIndex)).getResourceName();

            if (on.compareTo(off) < 0)
                merged.addElement(online.elementAt(onIndex++));
            else
                merged.addElement(offline.elementAt(offIndex++));
        }
        return(merged);
    }
    /**
     * Moves the file.
     *
     * This operation includes a copy and a delete operation. These operations
     * are done with their security-checks.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param source The complete path of the sourcefile.
     * @param destination The complete path of the destinationfile.
     *
     * @exception CmsException will be thrown, if the file couldn't be moved.
     * The CmsException will also be thrown, if the user has not the rights
     * for this resource.
     */
    public void moveFile(CmsUser currentUser, CmsProject currentProject, String source, String destination) throws CmsException {

        // read the file to check access
        CmsResource file = readFileHeader(currentUser,currentProject, source);

        // has the user write-access?
        if (accessWrite(currentUser, currentProject, file)) {

            // first copy the file, this may ends with an exception
            copyFile(currentUser, currentProject, source, destination);

            // then delete the source-file, this may end with an exception
            // => the file was only copied, not moved!
            deleteFile(currentUser, currentProject, source);
            // inform about the file-system-change
            fileSystemChanged(file.isFolder());
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + source, CmsException.C_NO_ACCESS);
        }
    }
/**
 * Returns the onlineproject.  All anonymous
 * (CmsUser callingUser, or guest) users will see the resources of this project.
 *
 * <B>Security:</B>
 * All users are granted.
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @return the onlineproject object.
 * @exception CmsException Throws CmsException if something goes wrong.
 */
public CmsProject onlineProject(CmsUser currentUser, CmsProject currentProject) throws CmsException {
    CmsProject project = null;

    // try to get the online project for this offline project from cache
    if(m_onlineProjectCache != null){
        project = (CmsProject) m_onlineProjectCache.clone();
    }
    if (project == null) {
        // the project was not in the cache
        // lookup the currentProject in the CMS_SITE_PROJECT table, and in the same call return it.
        project = m_dbAccess.getOnlineProject(currentProject.getId());
        // store the project into the cache
        if(project != null){
            m_onlineProjectCache = (CmsProject)project.clone();
        }
    }
    return project;
}

/**
 * Creates a static export of a Cmsresource in the filesystem
 *
 * @param currentUser user who requestd themethod
 * @param currentProject current project of the user
 * @param cms the cms-object to use for the export.
 * @param startpoints the startpoints for the export.
 *
 * @exception CmsException if operation was not successful.
 */
public synchronized void exportStaticResources(CmsUser currentUser, CmsProject currentProject,
                     CmsObject cms, Vector startpoints, Vector projectResources,
                     CmsPublishedResources changedResources) throws CmsException {

    if(isAdmin(currentUser, currentProject) || isProjectManager(currentUser, currentProject) ||
        isUser(currentUser, currentProject)) {
        new CmsStaticExport(cms, startpoints, true, projectResources, changedResources);
    } else {
         throw new CmsException("[" + this.getClass().getName() + "] exportResources",
             CmsException.C_NO_ACCESS);
    }

}

    /**
     * Publishes a project.
     *
     * <B>Security</B>
     * Only the admin or the owner of the project can do this.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param id The id of the project to be published.
     * @return CmsPublishedResources The object includes the vectors of changed resources.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public synchronized CmsPublishedResources publishProject(CmsObject cms, CmsUser currentUser, CmsProject currentProject, int id) throws CmsException {

        CmsProject publishProject = readProject(currentUser, currentProject, id);
        CmsPublishedResources allChanged = new CmsPublishedResources();
        Vector changedResources = new Vector();
        Vector changedModuleMasters = new Vector();

        // check the security
        if ((isAdmin(currentUser, currentProject) || isManagerOfProject(currentUser, publishProject)) &&
            (publishProject.getFlags() == C_PROJECT_STATE_UNLOCKED) && (id != C_PROJECT_ONLINE_ID)) {
            // check, if we update class-files with this publishing
            ClassLoader loader = getClass().getClassLoader();
            boolean shouldReload = false;
            // check if we are using our own classloader
            // e.g. the cms-shell uses the default classloader
            if(loader instanceof CmsClassLoader) {
                // yes we have our own classloader
                Vector classFiles = ((CmsClassLoader)loader).getFilenames();
                shouldReload = shouldReloadClasses(id, classFiles);
            }
            try{
                changedResources = m_dbAccess.publishProject(currentUser, id, onlineProject(currentUser, currentProject), m_enableHistory);
                // now publish the module masters
                Vector publishModules = new Vector();
                cms.getRegistry().getModulePublishables(publishModules, null);
                int versionId = 0;
                long publishDate = System.currentTimeMillis();
                if(m_enableHistory){
                    versionId = m_dbAccess.getBackupVersionId();
                    // get the version_id for the currently published version
                    if(versionId > 1){
                        versionId--;
                    }
                    try{
                        publishDate = m_dbAccess.readBackupProject(versionId).getPublishingDate();
                    } catch (CmsException e){
                        // nothing to do
                    }
                    if(publishDate == 0){
                        publishDate = System.currentTimeMillis();
                    }
                }
                for(int i = 0; i < publishModules.size(); i++){
                    // call the publishProject method of the class with parameters:
                    // cms, m_enableHistory, project_id, version_id, publishDate, subId,
                    // the vector changedResources and the vector changedModuleMasters
                    try{
                        // The changed masters are added to the vector changedModuleMasters, so after the last module
                        // was published the vector contains the changed masters of all published modules
                        Class.forName((String)publishModules.elementAt(i)).getMethod("publishProject",
                                                new Class[] {CmsObject.class, Boolean.class, Integer.class, Integer.class,
                                                Long.class, Vector.class, Vector.class}).invoke(null, new Object[] {cms,
                                                new Boolean(m_enableHistory), new Integer(id), new Integer(versionId), new Long(publishDate),
                                                changedResources, changedModuleMasters});
                    } catch(Exception ex){
                    ex.printStackTrace();
                        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
                            A_OpenCms.log(A_OpenCms.C_OPENCMS_INFO, "Error when publish data of module "+(String)publishModules.elementAt(i)+"!: "+ex.getMessage());
                        }
                    }
                }
            } catch (CmsException e){
                throw e;
            } finally {
                this.clearResourceCache();
                // inform about the file-system-change
                fileSystemChanged(true);

                // the project was stored in the backuptables for history
                //new projectmechanism: the project can be still used after publishing
                // it will be deleted if the project_flag = C_PROJECT_STATE_TEMP
                if (publishProject.getType() == C_PROJECT_TYPE_TEMPORARY) {
                    m_dbAccess.deleteProject(publishProject);
                    try{
                        m_projectCache.remove(id);
                    } catch (Exception e){
                        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
                            A_OpenCms.log(A_OpenCms.C_OPENCMS_CACHE,"Could not remove project "+id+" from cache");
                        }
                    }
                    if(id == currentProject.getId()){
                        cms.getRequestContext().setCurrentProject(I_CmsConstants.C_PROJECT_ONLINE_ID);
                    }

                }

                // finally set the refrish signal to another server if nescessary
                if (m_refresh.length() > 0) {
                    try {
                        URL url = new URL(m_refresh);
                        URLConnection con = url.openConnection();
                        con.connect();
                        InputStream in = con.getInputStream();
                        in.close();
                        //System.err.println(in.toString());
                    } catch (Exception ex) {
                        throw new CmsException(0, ex);
                    }
                }
                // inform about the reload classes
                if(loader instanceof CmsClassLoader) {
                    ((CmsClassLoader)loader).setShouldReload(shouldReload);
                }
            }
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] could not publish project " + id, CmsException.C_NO_ACCESS);
        }
        allChanged.setChangedResources(changedResources);
        allChanged.setChangedModuleMasters(changedModuleMasters);
        return allChanged;
    }

    /**
     * This method checks, if there is a classFile marked as changed or deleted.
     * If that is so, we have to reload all classes and instances to get rid of old versions.
     */
    public boolean shouldReloadClasses(int projectId, Vector classFiles) {
        for(int i = 0; i < classFiles.size(); i++) {
            try {
                CmsFile file = m_dbAccess.readFileHeader(projectId, (String)classFiles.elementAt(i));
                if( (file.getState() == C_STATE_CHANGED) || (file.getState() == C_STATE_DELETED) ) {
                    // this class-file was changed or deleted - we have to reload
                    return true;
                }
            } catch(CmsException exc) {
                // the file couldn't be read - it is not in our project - ignore this file
            }
        }
        // no modified class-files are found - we can use the old classes
        return false;
    }

    /**
     * Reads the agent of a task from the OpenCms.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param task The task to read the agent from.
     * @return The owner of a task.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public CmsUser readAgent(CmsUser currentUser, CmsProject currentProject,
                               CmsTask task)
        throws CmsException {
        return readUser(currentUser,currentProject,task.getAgentUser());
    }

     /**
     * Reads all file headers of a file in the OpenCms.<BR>
     * This method returns a vector with all file headers, i.e.
     * the file headers of a file, independent of the project they were attached to.<br>
     *
     * The reading excludes the filecontent.
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user can read the resource</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param filename The name of the file to be read.
     *
     * @return Vector of file headers read from the Cms.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
     public Vector readAllFileHeaders(CmsUser currentUser, CmsProject currentProject,
                                      String filename)
         throws CmsException {
         CmsResource cmsFile = readFileHeader(currentUser,currentProject, filename);
         if( accessRead(currentUser, currentProject, cmsFile) ) {

            // access to all subfolders was granted - return the file-history.
            return(m_dbAccess.readAllFileHeaders(currentProject.getId(), filename));
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + filename,
                 CmsException.C_ACCESS_DENIED);
        }
     }

     /**
     * Reads all file headers of a file in the OpenCms.<BR>
     * This method returns a vector with the histroy of all file headers, i.e.
     * the file headers of a file, independent of the project they were attached to.<br>
     *
     * The reading excludes the filecontent.
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user can read the resource</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param filename The name of the file to be read.
     *
     * @return Vector of file headers read from the Cms.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
     public Vector readAllFileHeadersForHist(CmsUser currentUser, CmsProject currentProject,
                                      String filename)
         throws CmsException {
         CmsResource cmsFile = readFileHeader(currentUser,currentProject, filename);
         if( accessRead(currentUser, currentProject, cmsFile) ) {

            // access to all subfolders was granted - return the file-history.
            return(m_dbAccess.readAllFileHeadersForHist(currentProject.getId(), filename));
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + filename,
                 CmsException.C_ACCESS_DENIED);
        }
     }

    /**
     * select all projectResources from an given project
     *
     * @param project The project in which the resource is used.
     *
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public Vector readAllProjectResources(int projectId) throws CmsException {
        return m_dbAccess.readAllProjectResources(projectId);
    }

    /**
     * Returns a list of all propertyinformations of a file or folder.
     *
     * <B>Security</B>
     * Only the user is granted, who has the right to view the resource.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The name of the resource of which the propertyinformation has to be
     * read.
     *
     * @return Vector of propertyinformation as Strings.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public Hashtable readAllProperties(CmsUser currentUser, CmsProject currentProject,
                                             String resource)
        throws CmsException {
        CmsResource res;
        // read the resource from the currentProject, or the online-project
        try {
            res = readFileHeader(currentUser,currentProject, resource);
        } catch(CmsException exc) {
            // the resource was not readable
            if(currentProject.equals(onlineProject(currentUser, currentProject))) {
                // this IS the onlineproject - throw the exception
                throw exc;
            } else {
                // try to read the resource in the onlineproject
                res = readFileHeader(currentUser,onlineProject(currentUser, currentProject),
                                              resource);
            }
        }
        // check the security
        if( ! accessRead(currentUser, currentProject, res) ) {
             throw new CmsException("[" + this.getClass().getName() + "] " + resource,
                CmsException.C_NO_ACCESS);
        }
        Hashtable returnValue = null;
        returnValue = (Hashtable)m_propertyCache.get(currentProject.getId()+"_"+Integer.toString(res.getResourceId()) +"_"+ Integer.toString(res.getType()));
        if (returnValue == null){
            returnValue = m_dbAccess.readAllProperties(currentProject.getId(),res,res.getType());
            m_propertyCache.put(currentProject.getId()+"_"+Integer.toString(res.getResourceId()) +"_"+ Integer.toString(res.getType()),returnValue);
        }
        return returnValue;
    }
    /**
     * Reads all propertydefinitions for the given resource type.
     *
     * <B>Security</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resourceType The resource type to read the propertydefinitions for.
     *
     * @return propertydefinitions A Vector with propertydefefinitions for the resource type.
     * The Vector is maybe empty.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public Vector readAllPropertydefinitions(CmsUser currentUser, CmsProject currentProject,
                                         int resourceType)
        throws CmsException {
        Vector returnValue = null;
        returnValue = (Vector) m_propertyDefVectorCache.get(Integer.toString(resourceType));
        if (returnValue == null){
            returnValue = m_dbAccess.readAllPropertydefinitions(resourceType);
            m_propertyDefVectorCache.put(Integer.toString(resourceType), returnValue);
        }

        return returnValue;
    }
    /**
     * Reads all propertydefinitions for the given resource type.
     *
     * <B>Security</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resourcetype The name of the resource type to read the propertydefinitions for.
     *
     * @return propertydefinitions A Vector with propertydefefinitions for the resource type.
     * The Vector is maybe empty.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public Vector readAllPropertydefinitions(CmsUser currentUser, CmsProject currentProject,
                                              String resourcetype)
        throws CmsException {
        Vector returnValue = null;
        I_CmsResourceType resType = getResourceType(currentUser, currentProject, resourcetype);

        returnValue = (Vector)m_propertyDefVectorCache.get(resType.getResourceTypeName());
        if (returnValue == null){
            returnValue = m_dbAccess.readAllPropertydefinitions(resType);
            m_propertyDefVectorCache.put(resType.getResourceTypeName(), returnValue);
        }
        return returnValue;
    }

    // Methods working with system properties


    /**
     * Reads the export-path for the system.
     * This path is used for db-export and db-import.
     *
     * <B>Security:</B>
     * All users are granted.<BR/>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @return the exportpath.
     */
    public String readExportPath(CmsUser currentUser, CmsProject currentProject)
        throws CmsException  {
        return (String) m_dbAccess.readSystemProperty(C_SYSTEMPROPERTY_EXPORTPATH);
    }
    /**
     * Reads a exportrequest from the Cms.<BR/>
     *
     *
     * @param request The request to be read.
     *
     * @return The exportrequest read from the Cms.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
     public CmsExportLink readExportLink(String request) throws CmsException{
        return m_dbAccess.readExportLink(request);
     }
    /**
     * Reads a exportrequest without the dependencies from the Cms.<BR/>
     *
     *
     * @param request The request to be read.
     *
     * @return The exportrequest read from the Cms.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
     public CmsExportLink readExportLinkHeader(String request) throws CmsException{
        return m_dbAccess.readExportLinkHeader(request);
     }

    /**
     * Writes an exportlink to the Cms.
     *
     * @param link the cmsexportlink object to write.
     *
     * @exception CmsException if something goes wrong.
     */
    public void writeExportLink(CmsExportLink link) throws CmsException {
        m_dbAccess.writeExportLink(link);
    }
    /**
     * Deletes an exportlink in the database.
     *
     * @param link the name of the link
     */
    public void deleteExportLink(String link) throws CmsException {
        m_dbAccess.deleteExportLink(link);
    }
    /**
     * Deletes an exportlink in the database.
     *
     * @param link the cmsExportLink object to delete.
     */
    public void deleteExportLink(CmsExportLink link) throws CmsException {
        m_dbAccess.deleteExportLink(link);
    }
    /**
     * Sets one exportLink to procecced.
     *
     * @param link the cmsexportlink.
     *
     * @exception CmsException if something goes wrong.
     */
    public void writeExportLinkProcessedState(CmsExportLink link) throws CmsException {
        m_dbAccess.writeExportLinkProcessedState(link);
    }

    /**
     * Reads all export links that depend on the resource.
     * @param res. The resourceName() of the resource that has changed (or the String
     *              that describes a contentdefinition).
     * @return a Vector(of Strings) with the linkrequest names.
     */
     public Vector getDependingExportLinks(Vector res) throws CmsException{
        return m_dbAccess.getDependingExportLinks(res);
     }

/**
 * Reads a file from a previous project of the Cms.<BR/>
 *
 * <B>Security:</B>
 * Access is granted, if:
 * <ul>
 * <li>the user has access to the project</li>
 * <li>the user can read the resource</li>
 * </ul>
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param projectId The id of the project to read the file from.
 * @param filename The name of the file to be read.
 *
 * @return The file read from the Cms.
 *
 * @exception CmsException  Throws CmsException if operation was not succesful.
 * */
public CmsFile readFile(CmsUser currentUser, CmsProject currentProject, int projectId, String filename) throws CmsException
{
    CmsFile cmsFile = null;
    // read the resource from the projectId,
    try
    {
        //cmsFile=(CmsFile)m_resourceCache.get(projectId+C_FILECONTENT+filename);
        if (cmsFile == null) {
            cmsFile = m_dbAccess.readFileInProject(projectId, onlineProject(currentUser, currentProject).getId(), filename);
            // only put it in thecache until the size is below the max site
            /*if (cmsFile.getContents().length <m_cachelimit) {
                m_resourceCache.put(projectId+C_FILECONTENT+filename,cmsFile);
            } else {

            }*/

        }
        if (accessRead(currentUser, currentProject, (CmsResource) cmsFile))
        {

            // acces to all subfolders was granted - return the file.
            return cmsFile;
        }
        else
        {
            throw new CmsException("[" + this.getClass().getName() + "] " + filename, CmsException.C_ACCESS_DENIED);
        }
    }
    catch (CmsException exc)
    {
        throw exc;
    }
}
/**
 * Reads a file from the Cms.<BR/>
 *
 * <B>Security:</B>
 * Access is granted, if:
 * <ul>
 * <li>the user has access to the project</li>
 * <li>the user can read the resource</li>
 * </ul>
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param filename The name of the file to be read.
 *
 * @return The file read from the Cms.
 *
 * @exception CmsException  Throws CmsException if operation was not succesful.
 * */
public CmsFile readFile(CmsUser currentUser, CmsProject currentProject, String filename) throws CmsException
{
    CmsFile cmsFile = null;
    // read the resource from the currentProject, or the online-project
    try
    {
        //cmsFile=(CmsFile)m_resourceCache.get(currentProject.getId()+C_FILECONTENT+filename);
        if (cmsFile == null) {

            cmsFile = m_dbAccess.readFile(currentProject.getId(), onlineProject(currentUser, currentProject).getId(), filename);

            // only put it in thecache until the size is below the max site
            /*if (cmsFile.getContents().length <m_cachelimit) {
                m_resourceCache.put(currentProject.getId()+C_FILECONTENT+filename,cmsFile);
            } else {

            }*/
        }

        }
    catch (CmsException exc)
    {
        // the resource was not readable
        throw exc;
    }
    if (accessRead(currentUser, currentProject, (CmsResource) cmsFile))
    {
        // acces to all subfolders was granted - return the file.
        return cmsFile;
    }
    else
    {
        throw new CmsException("[" + this.getClass().getName() + "] " + filename, CmsException.C_ACCESS_DENIED);
    }
}
/**
 * Reads a file from the Cms.<BR/>
 *
 * <B>Security:</B>
 * Access is granted, if:
 * <ul>
 * <li>the user has access to the project</li>
 * <li>the user can read the resource</li>
 * </ul>
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param filename The name of the file to be read.
 *
 * @return The file read from the Cms.
 *
 * @exception CmsException  Throws CmsException if operation was not succesful.
 * */
public CmsFile readFile(CmsUser currentUser, CmsProject currentProject, String filename, boolean includeDeleted) throws CmsException
{
    CmsFile cmsFile = null;
    // read the resource from the currentProject, or the online-project
    try
    {
        //cmsFile=(CmsFile)m_resourceCache.get(currentProject.getId()+C_FILECONTENT+filename);
        if (cmsFile == null) {

            cmsFile = m_dbAccess.readFile(currentProject.getId(), onlineProject(currentUser, currentProject).getId(), filename, includeDeleted);
            // only put it in thecache until the size is below the max site
            /*if (cmsFile.getContents().length <m_cachelimit) {
                m_resourceCache.put(currentProject.getId()+C_FILECONTENT+filename,cmsFile);
            } else {

            }*/
        }

        }
    catch (CmsException exc)
    {
        // the resource was not readable
        throw exc;
    }
    if (accessRead(currentUser, currentProject, (CmsResource) cmsFile))
    {

        // acces to all subfolders was granted - return the file.
        return cmsFile;
    }
    else
    {
        throw new CmsException("[" + this.getClass().getName() + "] " + filename, CmsException.C_ACCESS_DENIED);
    }
}
    /**
     * Gets the known file extensions (=suffixes)
     *
     * <B>Security:</B>
     * All users are granted access<BR/>
     *
     * @param currentUser The user who requested this method, not used here
     * @param currentProject The current project of the user, not used here
     *
     * @return Hashtable with file extensions as Strings
     */

    public Hashtable readFileExtensions(CmsUser currentUser, CmsProject currentProject)
        throws CmsException {
        Hashtable res=(Hashtable) m_dbAccess.readSystemProperty(C_SYSTEMPROPERTY_EXTENSIONS);
        return ( (res!=null)? res : new Hashtable());
    }
     /**
     * Reads a file header a previous project of the Cms.<BR/>
     * The reading excludes the filecontent. <br>
     *
     * A file header can be read from an offline project or the online project.
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can read the resource</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param projectId The id of the project to read the file from.
     * @param filename The name of the file to be read.
     *
     * @return The file read from the Cms.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
     public CmsResource readFileHeader(CmsUser currentUser,
                                       CmsProject currentProject,
                                       int projectId,
                                       String filename)
         throws CmsException  {
         CmsResource cmsFile = null;
         // check if this method is misused to read a folder
         if (filename.endsWith("/")) {
             return (CmsResource) readFolder(currentUser, currentProject, projectId,filename);
         }
         // read the resource from the currentProject
         try {
             cmsFile=(CmsResource)m_resourceCache.get(filename, projectId);
             if (cmsFile==null) {
                cmsFile = m_dbAccess.readFileHeaderInProject(projectId, filename);
                m_resourceCache.put(filename,projectId,cmsFile);
             }
             if( accessRead(currentUser, currentProject, cmsFile) ) {
                // acces to all subfolders was granted - return the file-header.
                return cmsFile;
            } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + filename,
                 CmsException.C_ACCESS_DENIED);
           }
         } catch(CmsException exc) {
             throw exc;
         }

     }
    /**
     * Reads a file header from the Cms.<BR/>
     * The reading excludes the filecontent. <br>
     *
     * A file header can be read from an offline project or the online project.
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can read the resource</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param filename The name of the file to be read.
     *
     * @return The file read from the Cms.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
     public CmsResource readFileHeader(CmsUser currentUser,
                                         CmsProject currentProject, String filename)
         throws CmsException {
         CmsResource cmsFile = null;
         // check if this method is misused to read a folder
         if (filename.endsWith("/")) {
             return (CmsResource) readFolder(currentUser,currentProject,filename);
         }

         // read the resource from the currentProject, or the online-project
         try {
             // try to read form cache first
             cmsFile=(CmsResource)m_resourceCache.get(filename, currentProject.getId());
             if (cmsFile==null) {
                cmsFile = m_dbAccess.readFileHeader(currentProject.getId(), filename);
                m_resourceCache.put(filename,currentProject.getId(),cmsFile);
             }
         } catch(CmsException exc) {
             // the resource was not readable
             throw exc;
         }

         if( accessRead(currentUser, currentProject, cmsFile) ) {
            // acces to all subfolders was granted - return the file-header.
            return cmsFile;
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + filename,
                 CmsException.C_ACCESS_DENIED);
        }
     }

    /**
     * Reads a file header from the Cms.<BR/>
     * The reading excludes the filecontent. <br>
     *
     * A file header can be read from an offline project or the online project.
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can read the resource</li>
     * </ul>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param filename The name of the file to be read.
     *
     * @return The file read from the Cms.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
     public CmsResource readFileHeader(CmsUser currentUser,
                                         CmsProject currentProject, String filename,
                                         boolean includeDeleted)
         throws CmsException {
         CmsResource cmsFile = null;
         // check if this method is misused to read a folder
         if (filename.endsWith("/")) {
             return (CmsResource) readFolder(currentUser,currentProject,filename, includeDeleted);
         }

         // read the resource from the currentProject, or the online-project
         try {
             // try to read form cache first
             cmsFile=(CmsResource)m_resourceCache.get(filename,currentProject.getId());
             if (cmsFile==null) {
                cmsFile = m_dbAccess.readFileHeader(currentProject.getId(), filename, includeDeleted);
                m_resourceCache.put(filename,currentProject.getId(),cmsFile);
             }
         } catch(CmsException exc) {
             // the resource was not readable
             throw exc;
         }

         if( accessRead(currentUser, currentProject, cmsFile) ) {
            // acces to all subfolders was granted - return the file-header.
            return cmsFile;
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + filename,
                 CmsException.C_ACCESS_DENIED);
        }
     }

    /**
     * Reads a file header from the history of the Cms.<BR/>
     * The reading excludes the filecontent. <br>
     *
     * A file header is read from the backup resources.
     *
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param versionId The id of the version of the file.
     * @param filename The name of the file to be read.
     *
     * @return The file read from the Cms.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
     public CmsBackupResource readFileHeaderForHist(CmsUser currentUser,
                                                    CmsProject currentProject,
                                                    int versionId,
                                                    String filename)
         throws CmsException  {
         CmsBackupResource resource;
         // read the resource from the backup resources
         try {
            resource = m_dbAccess.readFileHeaderForHist(versionId, filename);
         } catch(CmsException exc) {
             throw exc;
         }
        return resource;
     }

    /**
     * Reads a file from the history of the Cms.<BR/>
     * The reading includes the filecontent. <br>
     *
     * A file is read from the backup resources.
     *
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param versionId The id of the version of the file.
     * @param filename The name of the file to be read.
     *
     * @return The file read from the Cms.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
     public CmsBackupResource readFileForHist(CmsUser currentUser,
                                              CmsProject currentProject,
                                              int versionId,
                                              String filename)
         throws CmsException  {
         CmsBackupResource resource;
         // read the resource from the backup resources
         try {
             resource = m_dbAccess.readFileForHist(versionId, filename);
         } catch(CmsException exc) {
             throw exc;
         }
        return resource;
     }

     /**
     * Reads all file headers for a project from the Cms.<BR/>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param projectId The id of the project to read the resources for.
     *
     * @return a Vector of resources.
     *
     * @exception CmsException will be thrown, if the file couldn't be read.
     * The CmsException will also be thrown, if the user has not the rights
     * for this resource.
     */
    public Vector readFileHeaders(CmsUser currentUser, CmsProject currentProject,
                                  int projectId)
        throws CmsException {
        CmsProject project = readProject(currentUser, currentProject, projectId);
        Vector resources = m_dbAccess.readResources(project);
        Vector retValue = new Vector();

        // check the security
        for(int i = 0; i < resources.size(); i++) {
            if( accessRead(currentUser, currentProject, (CmsResource) resources.elementAt(i)) ) {
                retValue.addElement(resources.elementAt(i));
            }
        }

        return retValue;
    }
/**
 * Reads a folder from the Cms.<BR/>
 *
 * <B>Security:</B>
 * Access is granted, if:
 * <ul>
 * <li>the user has access to the project</li>
 * <li>the user can read the resource</li>
 * </ul>
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param project the project to read the folder from.
 * @param foldername The complete path of the folder to be read.
 *
 * @return folder The read folder.
 *
 * @exception CmsException will be thrown, if the folder couldn't be read.
 * The CmsException will also be thrown, if the user has not the rights
 * for this resource
 */
protected CmsFolder readFolder(CmsUser currentUser, CmsProject currentProject, int project, String folder) throws CmsException {
    if (folder == null) return null;
    CmsFolder cmsFolder = (CmsFolder) m_resourceCache.get(folder,currentProject.getId());
    if (cmsFolder == null) {
        cmsFolder = m_dbAccess.readFolderInProject(project, folder);
        if (cmsFolder != null){
            m_resourceCache.put(folder, currentProject.getId(), (CmsFolder) cmsFolder);
        }
    }
    if (cmsFolder != null) {
        if (!accessRead(currentUser, currentProject, (CmsResource) cmsFolder))
            throw new CmsException("[" + this.getClass().getName() + "] " + folder, CmsException.C_ACCESS_DENIED);
    }
    return cmsFolder;
}

/**
 * Reads a folder from the Cms.<BR/>
 *
 * <B>Security:</B>
 * Access is granted, if:
 * <ul>
 * <li>the user has access to the project</li>
 * <li>the user can read the resource</li>
 * </ul>
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param foldername The complete path of the folder to be read.
 *
 * @return folder The read folder.
 *
 * @exception CmsException will be thrown, if the folder couldn't be read.
 * The CmsException will also be thrown, if the user has not the rights
 * for this resource.
 */
public CmsFolder readFolder(CmsUser currentUser, CmsProject currentProject, String folder) throws CmsException {
    return readFolder(currentUser, currentProject, folder, false);
}

/**
 * Reads a folder from the Cms.<BR/>
 *
 * <B>Security:</B>
 * Access is granted, if:
 * <ul>
 * <li>the user has access to the project</li>
 * <li>the user can read the resource</li>
 * </ul>
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param foldername The complete path of the folder to be read.
 * @param includeDeleted Include the folder it it is marked as deleted
 *
 * @return folder The read folder.
 *
 * @exception CmsException will be thrown, if the folder couldn't be read.
 * The CmsException will also be thrown, if the user has not the rights
 * for this resource.
 */
public CmsFolder readFolder(CmsUser currentUser, CmsProject currentProject, String folder, boolean includeDeleted) throws CmsException {
    CmsFolder cmsFolder = null;
    // read the resource from the currentProject, or the online-project
    try {
        cmsFolder = (CmsFolder) m_resourceCache.get(folder, currentProject.getId());
        if (cmsFolder == null) {
            cmsFolder = m_dbAccess.readFolder(currentProject.getId(), folder);
            if (cmsFolder.getState() != C_STATE_DELETED) {
                m_resourceCache.put(folder, currentProject.getId(), (CmsFolder) cmsFolder);
            }
        }
    } catch (CmsException exc) {
        throw exc;
    }
    if (accessRead(currentUser, currentProject, (CmsResource) cmsFolder)) {
        // acces to all subfolders was granted - return the folder.
        if ((cmsFolder.getState() == C_STATE_DELETED) && (!includeDeleted)) {
            throw new CmsException("[" + this.getClass().getName() + "]" + cmsFolder.getAbsolutePath(), CmsException.C_RESOURCE_DELETED);
        } else {
            return cmsFolder;
        }
    } else {
        throw new CmsException("[" + this.getClass().getName() + "] " + folder, CmsException.C_ACCESS_DENIED);
    }
}
/**
 * Reads a folder from the Cms.<BR/>
 *
 * <B>Security:</B>
 * Access is granted, if:
 * <ul>
 * <li>the user has access to the project</li>
 * <li>the user can read the resource</li>
 * </ul>
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param folder The complete path to the folder from which the folder will be
 * read.
 * @param foldername The name of the folder to be read.
 *
 * @return folder The read folder.
 *
 * @exception CmsException will be thrown, if the folder couldn't be read.
 * The CmsException will also be thrown, if the user has not the rights
 * for this resource.
 *
 * @see #readFolder(CmsUser, CmsProject, String)
 */
public CmsFolder readFolder(CmsUser currentUser, CmsProject currentProject, String folder, String folderName) throws CmsException {
    return readFolder(currentUser, currentProject, folder + folderName);
}

/**
 * Reads a folder from the Cms.<BR/>
 *
 * <B>Security:</B>
 * Access is granted, if:
 * <ul>
 * <li>the user has access to the project</li>
 * <li>the user can read the resource</li>
 * </ul>
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param folderid The id of the folder to be read.
 * @param includeDeleted Include the folder it it is marked as deleted
 *
 * @return folder The read folder.
 *
 * @exception CmsException will be thrown, if the folder couldn't be read.
 * The CmsException will also be thrown, if the user has not the rights
 * for this resource.
 */
public CmsFolder readFolder(CmsUser currentUser, CmsProject currentProject, int folderid, boolean includeDeleted) throws CmsException {
    CmsFolder cmsFolder = null;
    // read the resource from the currentProject, or the online-project
    try {
        if (cmsFolder == null) {
            cmsFolder = m_dbAccess.readFolder(currentProject.getId(), folderid);
        }
    } catch (CmsException exc) {
        throw exc;
    }
    if (accessRead(currentUser, currentProject, (CmsResource) cmsFolder)) {
        // acces to all subfolders was granted - return the folder.
        if ((cmsFolder.getState() == C_STATE_DELETED) && (!includeDeleted)) {
            throw new CmsException("[" + this.getClass().getName() + "]" + cmsFolder.getAbsolutePath(), CmsException.C_RESOURCE_DELETED);
        } else {
            return cmsFolder;
        }
    } else {
        throw new CmsException("[" + this.getClass().getName() + "] " + folderid, CmsException.C_ACCESS_DENIED);
    }
}
    /**
     * Reads all given tasks from a user for a project.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param projectId The id of the Project in which the tasks are defined.
     * @param owner Owner of the task.
     * @param tasktype Task type you want to read: C_TASKS_ALL, C_TASKS_OPEN, C_TASKS_DONE, C_TASKS_NEW.
     * @param orderBy Chooses, how to order the tasks.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public Vector readGivenTasks(CmsUser currentUser, CmsProject currentProject,
                                 int projectId, String ownerName, int taskType,
                                 String orderBy, String sort)
        throws CmsException {
        CmsProject project = null;

        CmsUser owner = null;

        if(ownerName != null) {
            owner = readUser(currentUser, currentProject, ownerName);
        }

        if(projectId != C_UNKNOWN_ID) {
            project = readProject(currentUser, currentProject, projectId);
        }

        return m_dbAccess.readTasks(project,null, owner, null, taskType, orderBy, sort);
    }
    /**
     * Reads the group of a project from the OpenCms.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @return The group of a resource.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public CmsGroup readGroup(CmsUser currentUser, CmsProject currentProject,
                                CmsProject project)
        throws CmsException {

        CmsGroup group=null;
        // try to read group form cache
        group=(CmsGroup)m_groupCache.get(project.getGroupId());

        if (group== null) {
            try {
                group=m_dbAccess.readGroup(project.getGroupId()) ;
            } catch(CmsException exc) {
                if(exc.getType() == exc.C_NO_GROUP) {
                    // the group does not exist any more - return a dummy-group
                    return new CmsGroup(C_UNKNOWN_ID, C_UNKNOWN_ID, project.getGroupId() + "", "deleted group", 0);
                }
            }
            m_groupCache.put(project.getGroupId(),group);
        }

        return group;
    }
    /**
     * Reads the group of a resource from the OpenCms.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @return The group of a resource.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public CmsGroup readGroup(CmsUser currentUser, CmsProject currentProject,
                               CmsResource resource)
        throws CmsException {
        CmsGroup group=null;
        // try to read group form cache
        group=(CmsGroup)m_groupCache.get(resource.getGroupId());

        if (group== null) {
            try {
                group=m_dbAccess.readGroup(resource.getGroupId()) ;
            } catch(CmsException exc) {
                if(exc.getType() == exc.C_NO_GROUP) {
                    return new CmsGroup(C_UNKNOWN_ID, C_UNKNOWN_ID, resource.getGroupId() + "", "deleted group", 0);
                }
            }
            m_groupCache.put(resource.getGroupId(),group);
        }
        return group;
    }
    /**
     * Reads the group (role) of a task from the OpenCms.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param task The task to read from.
     * @return The group of a resource.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public CmsGroup readGroup(CmsUser currentUser, CmsProject currentProject,
                               CmsTask task)
        throws CmsException {
        return m_dbAccess.readGroup(task.getRole());
    }
    /**
     * Returns a group object.<P/>
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param groupname The name of the group that is to be read.
     * @return Group.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful
     */
    public CmsGroup readGroup(CmsUser currentUser, CmsProject currentProject,
                                String groupname)
        throws CmsException {
        CmsGroup group=null;
        // try to read group form cache
        group=(CmsGroup)m_groupCache.get(groupname);
        if (group== null) {
            group=m_dbAccess.readGroup(groupname) ;
            m_groupCache.put(groupname,group);
        }
        return group;


    }

    /**
     * Returns a group object.<P/>
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param groupid The id of the group that is to be read.
     * @return Group.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful
     */
    public CmsGroup readGroup(CmsUser currentUser, CmsProject currentProject,
                              int groupid)
        throws CmsException {
        return m_dbAccess.readGroup(groupid);
    }
    /**
     * Reads the managergroup of a project from the OpenCms.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @return The group of a resource.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public CmsGroup readManagerGroup(CmsUser currentUser, CmsProject currentProject,
                                     CmsProject project)
        throws CmsException {
             CmsGroup group=null;
        // try to read group form cache
        group=(CmsGroup)m_groupCache.get(project.getManagerGroupId());
        if (group== null) {
            try {
                group=m_dbAccess.readGroup(project.getManagerGroupId()) ;
            } catch(CmsException exc) {
                if(exc.getType() == exc.C_NO_GROUP) {
                    // the group does not exist any more - return a dummy-group
                    return new CmsGroup(C_UNKNOWN_ID, C_UNKNOWN_ID, project.getManagerGroupId() + "", "deleted group", 0);
                }
            }
            m_groupCache.put(project.getManagerGroupId(),group);
        }
        return group;
    }

    /**
     * Gets the MimeTypes.
     * The Mime-Types will be returned.
     *
     * <B>Security:</B>
     * All users are garnted<BR/>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     *
     * @return the mime-types.
     */
    public Hashtable readMimeTypes(CmsUser currentUser, CmsProject currentProject)
        throws CmsException {
        // read the mimetype-properties as ressource from classloader and convert them
        // to hashtable
        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream("mimetypes.properties"));
        } catch(Exception exc) {
            if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[CmsResourceBroker] could not read mimetypes from properties. " + exc.getMessage());
            }
        }
        return(Hashtable) props;
    }

    /**
     * Reads the original agent of a task from the OpenCms.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param task The task to read the original agent from.
     * @return The owner of a task.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public CmsUser readOriginalAgent(CmsUser currentUser, CmsProject currentProject,
                                       CmsTask task)
        throws CmsException {
        return readUser(currentUser,currentProject,task.getOriginalUser());
    }
    /**
     * Reads the owner of a project from the OpenCms.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @return The owner of a resource.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public CmsUser readOwner(CmsUser currentUser, CmsProject currentProject,
                               CmsProject project)
        throws CmsException {
        return readUser(currentUser,currentProject,project.getOwnerId());
    }
    /**
     * Reads the owner of a resource from the OpenCms.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @return The owner of a resource.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public CmsUser readOwner(CmsUser currentUser, CmsProject currentProject,
                               CmsResource resource)
        throws CmsException {
        return readUser(currentUser,currentProject,resource.getOwnerId() );
    }
    /**
     * Reads the owner (initiator) of a task from the OpenCms.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param task The task to read the owner from.
     * @return The owner of a task.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public CmsUser readOwner(CmsUser currentUser, CmsProject currentProject,
                               CmsTask task)
        throws CmsException {
        return readUser(currentUser,currentProject,task.getInitiatorUser());
    }
    /**
     * Reads the owner of a tasklog from the OpenCms.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @return The owner of a resource.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public CmsUser readOwner(CmsUser currentUser, CmsProject currentProject, CmsTaskLog log)
        throws CmsException {
        return readUser(currentUser,currentProject,log.getUser());
    }
    /**
     * Reads a project from the Cms.
     *
     * <B>Security</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param id The id of the project to read.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
        public CmsProject readProject(CmsUser currentUser, CmsProject currentProject, int id) throws CmsException {
          CmsProject project=null;
          project=(CmsProject)m_projectCache.get(id);
          if (project==null) {
             project=m_dbAccess.readProject(id);
             m_projectCache.put(id,project);
         }
         return project;
     }
     /**
     * Reads a project from the Cms.
     *
     * <B>Security</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param res The resource to read the project of.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
     public CmsProject readProject(CmsUser currentUser, CmsProject currentProject,
                                   CmsResource res)
         throws CmsException {
         return readProject(currentUser, currentProject, res.getProjectId());
     }
    /**
     * Reads a project from the Cms.
     *
     * <B>Security</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param task The task to read the project of.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
     public CmsProject readProject(CmsUser currentUser, CmsProject currentProject,
                                   CmsTask task)
         throws CmsException {
         // read the parent of the task, until it has no parents.
         task = this.readTask(currentUser, currentProject, task.getId());

         while(task.getParent() != 0) {
             task = readTask(currentUser, currentProject, task.getParent());
         }
         return m_dbAccess.readProject(task);
     }

    /**
     * Reads all file headers of a project from the Cms.
     *
     * @param projectId the id of the project to read the file headers for.
     * @param filter The filter for the resources (all, new, changed, deleted, locked)
     *
     * @return a Vector of resources.
     */
    public Vector readProjectView(CmsUser currentUser, CmsProject currentProject, int projectId, String filter)
            throws CmsException {
        CmsProject project = readProject(currentUser, currentProject, projectId);
        Vector retValue = new Vector();
        String whereClause = new String();
        if("new".equalsIgnoreCase(filter)){
            whereClause = " AND STATE="+C_STATE_NEW;
        } else if ("changed".equalsIgnoreCase(filter)){
            whereClause = " AND STATE="+C_STATE_CHANGED;
        } else if ("deleted".equalsIgnoreCase(filter)){
            whereClause = " AND STATE="+C_STATE_DELETED;
        } else if ("locked".equalsIgnoreCase(filter)){
            whereClause = " AND LOCKED_BY != "+C_UNKNOWN_ID;
        } else {
            whereClause = " AND STATE != "+C_STATE_UNCHANGED;
        }
        Vector resources = m_dbAccess.readProjectView(currentProject.getId(), projectId, whereClause);
        // check the security
        for(int i = 0; i < resources.size(); i++) {
            if( accessRead(currentUser, project, (CmsResource) resources.elementAt(i)) ) {
                retValue.addElement(resources.elementAt(i));
            }
        }

        return retValue;
    }
     /**
     * Reads the backupinformation of a project from the Cms.
     *
     * <B>Security</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param versionId The versionId of the project.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
     public CmsBackupProject readBackupProject(CmsUser currentUser, CmsProject currentProject, int versionId)
         throws CmsException {
         return m_dbAccess.readBackupProject(versionId);
     }

    /**
     * Reads log entries for a project.
     *
     * @param projectId The id of the projec for tasklog to read.
     * @return A Vector of new TaskLog objects
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public Vector readProjectLogs(CmsUser currentUser, CmsProject currentProject,
                                  int projectid)
        throws CmsException {
        return m_dbAccess.readProjectLogs(projectid);
    }
    /**
     * Returns a propertyinformation of a file or folder.
     *
     * <B>Security</B>
     * Only the user is granted, who has the right to view the resource.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The name of the resource of which the propertyinformation has
     * to be read.
     * @param property The propertydefinition-name of which the propertyinformation has to be read.
     *
     * @return propertyinfo The propertyinfo as string.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public String readProperty(CmsUser currentUser, CmsProject currentProject,
                                      String resource, String property)
        throws CmsException {
        CmsResource res;
        // read the resource from the currentProject
        try {
            res = readFileHeader(currentUser,currentProject, resource, true);
        } catch(CmsException exc) {
            // the resource was not readable
            throw exc;
        }

        // check the security
        if( ! accessRead(currentUser, currentProject, res) ) {
            throw new CmsException("[" + this.getClass().getName() + "] " + resource,
                CmsException.C_NO_ACCESS);
        }
        String returnValue = null;

        returnValue = (String)m_propertyCache.get(currentProject.getId()+property +
                    Integer.toString(res.getResourceId()) +","+ Integer.toString(res.getType()));

        if (returnValue == null){
            returnValue = m_dbAccess.readProperty(property,currentProject.getId(),res,res.getType());

            if (returnValue == null) {
                returnValue="";
            }
            m_propertyCache.put(currentProject.getId()+property +Integer.toString(res.getResourceId()) +
                            ","+ Integer.toString(res.getType()), returnValue);

        }
        if (returnValue.equals("")){returnValue=null;}
        return returnValue;
    }
    /**
     * Reads a definition for the given resource type.
     *
     * <B>Security</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param name The name of the propertydefinition to read.
     * @param resourcetype The name of the resource type for which the propertydefinition
     * is valid.
     *
     * @return propertydefinition The propertydefinition that corresponds to the overgiven
     * arguments - or null if there is no valid propertydefinition.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsPropertydefinition readPropertydefinition(CmsUser currentUser,
                                                  CmsProject currentProject,
                                                  String name, String resourcetype)
        throws CmsException {

        I_CmsResourceType resType = getResourceType(currentUser,currentProject,resourcetype);
        CmsPropertydefinition returnValue = null;
        returnValue = (CmsPropertydefinition)m_propertyDefCache.get(name + resType.getResourceType());

        if (returnValue == null){
            returnValue = m_dbAccess.readPropertydefinition(name, resType);
            m_propertyDefCache.put(name + resType.getResourceType(), returnValue);
        }
        return returnValue;
    }
/**
 * Insert the method's description here.
 * Creation date: (09-10-2000 09:29:45)
 * @return java.util.Vector
 * @param project com.opencms.file.CmsProject
 * @exception com.opencms.core.CmsException The exception description.
 */
public Vector readResources(CmsProject project) throws com.opencms.core.CmsException
{
    return m_dbAccess.readResources(project);
}
    /**
     * Read a task by id.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param id The id for the task to read.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsTask readTask(CmsUser currentUser, CmsProject currentProject,
                            int id)
        throws CmsException {
        return m_dbAccess.readTask(id);
    }
    /**
     * Reads log entries for a task.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param taskid The task for the tasklog to read .
     * @return A Vector of new TaskLog objects
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public Vector readTaskLogs(CmsUser currentUser, CmsProject currentProject,
                               int taskid)
        throws CmsException {
        return m_dbAccess.readTaskLogs(taskid);;
    }
    /**
     * Reads all tasks for a project.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param projectId The id of the Project in which the tasks are defined. Can be null for all tasks
     * @tasktype Task type you want to read: C_TASKS_ALL, C_TASKS_OPEN, C_TASKS_DONE, C_TASKS_NEW
     * @param orderBy Chooses, how to order the tasks.
     * @param sort Sort order C_SORT_ASC, C_SORT_DESC, or null
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public Vector readTasksForProject(CmsUser currentUser, CmsProject currentProject,
                                      int projectId, int tasktype,
                                      String orderBy, String sort)
        throws CmsException {

        CmsProject project = null;

        if(projectId != C_UNKNOWN_ID) {
            project = readProject(currentUser, currentProject, projectId);
        }
        return m_dbAccess.readTasks(project, null, null, null, tasktype, orderBy, sort);
    }
    /**
     * Reads all tasks for a role in a project.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param projectId The id of the Project in which the tasks are defined.
     * @param user The user who has to process the task.
     * @param tasktype Task type you want to read: C_TASKS_ALL, C_TASKS_OPEN, C_TASKS_DONE, C_TASKS_NEW.
     * @param orderBy Chooses, how to order the tasks.
     * @param sort Sort order C_SORT_ASC, C_SORT_DESC, or null
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public Vector readTasksForRole(CmsUser currentUser, CmsProject currentProject,
                                   int projectId, String roleName, int tasktype,
                                   String orderBy, String sort)
        throws CmsException {

        CmsProject project = null;
        CmsGroup role = null;

        if(roleName != null) {
            role = readGroup(currentUser, currentProject, roleName);
        }

        if(projectId != C_UNKNOWN_ID) {
            project = readProject(currentUser, currentProject, projectId);
        }

        return m_dbAccess.readTasks(project, null, null, role, tasktype, orderBy, sort);
    }
    /**
     * Reads all tasks for a user in a project.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param projectId The id of the Project in which the tasks are defined.
     * @param userName The user who has to process the task.
     * @param taskType Task type you want to read: C_TASKS_ALL, C_TASKS_OPEN, C_TASKS_DONE, C_TASKS_NEW.
     * @param orderBy Chooses, how to order the tasks.
     * @param sort Sort order C_SORT_ASC, C_SORT_DESC, or null
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public Vector readTasksForUser(CmsUser currentUser, CmsProject currentProject,
                                   int projectId, String userName, int taskType,
                                   String orderBy, String sort)
        throws CmsException {

        CmsUser user = m_dbAccess.readUser(userName, C_USER_TYPE_SYSTEMUSER);
        return m_dbAccess.readTasks(currentProject, user, null, null, taskType, orderBy, sort);
    }
    /**
     * Returns a user object.<P/>
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param id The id of the user that is to be read.
     * @return User
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public CmsUser readUser(CmsUser currentUser, CmsProject currentProject,
                              int id)
        throws CmsException {

        try {
            CmsUser user=null;
            // try to read the user from cache
            user=(CmsUser)m_userCache.get(id);
            if (user==null) {
                user=m_dbAccess.readUser(id);
                m_userCache.put(id,user);
            }
            return user;
        } catch (CmsException ex) {
            return new CmsUser(C_UNKNOWN_ID, id + "", "deleted user");
        }
    }
    /**
     * Returns a user object.<P/>
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param username The name of the user that is to be read.
     * @return User
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public CmsUser readUser(CmsUser currentUser, CmsProject currentProject,
                              String username)
        throws CmsException {

        CmsUser user = null;
        // try to read the user from cache
        user = (CmsUser)m_userCache.get(username+C_USER_TYPE_SYSTEMUSER);
        if (user == null) {
            user = m_dbAccess.readUser(username, C_USER_TYPE_SYSTEMUSER);
            m_userCache.put(username+C_USER_TYPE_SYSTEMUSER,user);
        }
        return user;
    }
     /**
     * Returns a user object.<P/>
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param username The name of the user that is to be read.
     * @param type The type of the user.
     * @return User
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public CmsUser readUser(CmsUser currentUser, CmsProject currentProject,
                              String username,int type)
        throws CmsException {

        CmsUser user = null;
        // try to read the user from cache
        user = (CmsUser)m_userCache.get(username+type);
        if (user == null) {
            user = m_dbAccess.readUser(username, type);
            m_userCache.put(username+type,user);
        }
        return user;
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
     * @exception CmsException  Throws CmsException if operation was not succesful
     */
    public CmsUser readUser(CmsUser currentUser, CmsProject currentProject,
                              String username, String password)
        throws CmsException {

        CmsUser user = null;
        // ednfal: don't read user from cache because password may be changed
        //user = (CmsUser)m_userCache.get(username+C_USER_TYPE_SYSTEMUSER);
        // store user in cache
        if (user == null) {
            user = m_dbAccess.readUser(username, password, C_USER_TYPE_SYSTEMUSER);
            m_userCache.put(username+C_USER_TYPE_SYSTEMUSER, user);
        }
        return user;
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
     * @exception CmsException  Throws CmsException if operation was not succesful
     */
    public CmsUser readWebUser(CmsUser currentUser, CmsProject currentProject,
                              String username)
        throws CmsException {

        CmsUser user = null;
        user = (CmsUser)m_userCache.get(username+C_USER_TYPE_WEBUSER);
        // store user in cache
        if (user == null) {
            user = m_dbAccess.readUser(username, C_USER_TYPE_WEBUSER);
            m_userCache.put(username+C_USER_TYPE_WEBUSER, user);
        }
        return user;
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
     * @exception CmsException  Throws CmsException if operation was not succesful
     */
    public CmsUser readWebUser(CmsUser currentUser, CmsProject currentProject,
                              String username, String password)
        throws CmsException {

        CmsUser user = null;
        // ednfal: don't read user from cache because password may be changed
        user = (CmsUser)m_userCache.get(username+C_USER_TYPE_WEBUSER);
        // store user in cache
        if (user == null) {
            user = m_dbAccess.readUser(username, password, C_USER_TYPE_WEBUSER);
            m_userCache.put(username+C_USER_TYPE_WEBUSER,user);
        }
        return user;
    }
    /**
     * Reaktivates a task from the Cms.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param taskid The Id of the task to accept.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void reaktivateTask(CmsUser currentUser, CmsProject currentProject,
                               int taskId)
        throws CmsException {
        CmsTask task = m_dbAccess.readTask(taskId);
        task.setState(C_TASK_STATE_STARTED);
        task.setPercentage(0);
        task = m_dbAccess.writeTask(task);
        m_dbAccess.writeSystemTaskLog(taskId,
                                      "Task was reactivated from " +
                                      currentUser.getFirstname() + " " +
                                      currentUser.getLastname() + ".");


    }
    /**
     * Sets a new password only if the user knows his recovery-password.
     *
     * All users can do this if he knows the recovery-password.<P/>
     *
     * <B>Security:</B>
     * All users can do this if he knows the recovery-password.<P/>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param username The name of the user.
     * @param recoveryPassword The recovery password.
     * @param newPassword The new password.
     *
     * @exception CmsException Throws CmsException if operation was not succesfull.
     */
    public void recoverPassword(CmsUser currentUser, CmsProject currentProject,
                            String username, String recoveryPassword, String newPassword)
        throws CmsException {
        // check the length of the new password.
        if(newPassword.length() < C_PASSWORD_MINIMUMSIZE) {
            throw new CmsException("[" + this.getClass().getName() + "] " + username,
                CmsException.C_SHORT_PASSWORD);
        }

        // check the length of the recovery password.
        if(recoveryPassword.length() < C_PASSWORD_MINIMUMSIZE) {
            throw new CmsException("[" + this.getClass().getName() + "] no recovery password.");
        }

        m_dbAccess.recoverPassword(username, recoveryPassword, newPassword);
    }
    /**
     * Removes a user from a group.
     *
     * Only the admin can do this.<P/>
     *
     * <B>Security:</B>
     * Only users, which are in the group "administrators" are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param username The name of the user that is to be removed from the group.
     * @param groupname The name of the group.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public void removeUserFromGroup(CmsUser currentUser, CmsProject currentProject,
                                    String username, String groupname)
        throws CmsException {

        // test if this user is existing in the group
        if (!userInGroup(currentUser,currentProject,username,groupname)) {
           // user already there, throw exception
           throw new CmsException("[" + this.getClass().getName() + "] remove " +  username+ " from " +groupname,
                CmsException.C_NO_USER);
        }


        if( isAdmin(currentUser, currentProject) ) {
            CmsUser user;
            CmsGroup group;

            user=readUser(currentUser,currentProject,username);
            //check if the user exists
            if (user != null) {
                group=readGroup(currentUser,currentProject,groupname);
                //check if group exists
                if (group != null){
                  // do not remmove the user from its default group
                  if (user.getDefaultGroupId() != group.getId()) {
                    //remove this user from the group
                    m_dbAccess.removeUserFromGroup(user.getId(),group.getId());
                    m_usergroupsCache.clear();
                  } else {
                    throw new CmsException("["+this.getClass().getName()+"]",CmsException.C_NO_DEFAULT_GROUP);
                  }
                } else {
                    throw new CmsException("["+this.getClass().getName()+"]"+groupname,CmsException.C_NO_GROUP);
                }
            } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + username,
                CmsException.C_NO_ACCESS);
            }
        }
    }
/**
 * Renames the file to a new name. <br>
 *
 * Rename can only be done in an offline project. To rename a file, the following
 * steps have to be done:
 * <ul>
 * <li> Copy the file with the oldname to a file with the new name, the state
 * of the new file is set to NEW (2).
 * <ul>
 * <li> If the state of the original file is UNCHANGED (0), the file content of the
 * file is read from the online project. </li>
 * <li> If the state of the original file is CHANGED (1) or NEW (2) the file content
 * of the file is read from the offline project. </li>
 * </ul>
 * </li>
 * <li> Set the state of the old file to DELETED (3). </li>
 * </ul>
 *
 * <B>Security:</B>
 * Access is granted, if:
 * <ul>
 * <li>the user has access to the project</li>
 * <li>the user can write the resource</li>
 * <li>the resource is locked by the callingUser</li>
 * </ul>
 *
 * @param currentUser The user who requested this method.
 * @param currentProject The current project of the user.
 * @param oldname The complete path to the resource which will be renamed.
 * @param newname The new name of the resource (CmsUser callingUser, No path information allowed).
 *
 * @exception CmsException  Throws CmsException if operation was not succesful.
 */
public void renameFile(CmsUser currentUser, CmsProject currentProject, String oldname, String newname) throws CmsException {

    // read the old file
    CmsResource file = readFileHeader(currentUser, currentProject, oldname);

    // checks, if the newname is valid, if not it throws a exception
    validFilename(newname);

    // has the user write-access?
    if (accessWrite(currentUser, currentProject, file)) {
        String path = oldname.substring(0, oldname.lastIndexOf("/") + 1);
        copyFile(currentUser, currentProject, oldname, path + newname);
        deleteFile(currentUser, currentProject, oldname);
    } else {
        throw new CmsException("[" + this.getClass().getName() + "] " + oldname, CmsException.C_NO_ACCESS);
    }
}
    /**
     * This method loads old sessiondata from the database. It is used
     * for sessionfailover.
     *
     * @param oldSessionId the id of the old session.
     * @return the old sessiondata.
     */
    public Hashtable restoreSession(String oldSessionId)
        throws CmsException {

        return m_dbAccess.readSession(oldSessionId);
    }

    /**
     * Restores a file in the current project with a version in the backup
     *
     * @param currentUser The current user
     * @param currentProject The current project
     * @param versionId The version id of the resource
     * @param filename The name of the file to restore
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public void restoreResource(CmsUser currentUser, CmsProject currentProject,
                                   int versionId, String filename) throws CmsException {
        CmsBackupResource backupFile = null;
        CmsFile offlineFile = null;
        int state = C_STATE_CHANGED;
        // read the backup file
        backupFile = readFileForHist(currentUser, currentProject, versionId, filename);
        // try to read the owner and the group
        int ownerId = currentUser.getId();
        int groupId = currentUser.getDefaultGroupId();
        try{
            ownerId = readUser(currentUser, currentProject, backupFile.getOwnerName()).getId();
        } catch(CmsException exc){
            // user can not be read, set the userid of current user
        }
        try{
            groupId = readGroup(currentUser, currentProject, backupFile.getGroupName()).getId();
        } catch(CmsException exc){
            // group can not be read, set the groupid of current user
        }
        offlineFile = readFile(currentUser, currentProject, filename);
        if(offlineFile.getState() == C_STATE_NEW){
            state = C_STATE_NEW;
        }
        if (backupFile != null && offlineFile != null){
            CmsFile newFile = new CmsFile(offlineFile.getResourceId(), offlineFile.getParentId(),
                                      offlineFile.getFileId(), offlineFile.getResourceName(),
                                      backupFile.getType(), backupFile.getFlags(),
                                      ownerId, groupId,
                                      currentProject.getId(), backupFile.getAccessFlags(),
                                      state, offlineFile.isLockedBy(),
                                      backupFile.getLauncherType(), backupFile.getLauncherClassname(),
                                      offlineFile.getDateCreated(), offlineFile.getDateLastModified(),
                                      currentUser.getId(), backupFile.getContents(),
                                      backupFile.getLength(), currentProject.getId());
            writeFile(currentUser, currentProject, newFile);
        }
    }

    /**
     * Set a new name for a task
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param taskid The Id of the task to set the percentage.
     * @param name The new name value
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void setName(CmsUser currentUser, CmsProject currentProject,
                        int taskId, String name)
        throws CmsException {
        if( (name == null) || name.length() == 0) {
            throw new CmsException("[" + this.getClass().getName() + "] " +
                name, CmsException.C_BAD_NAME);
        }
        CmsTask task = m_dbAccess.readTask(taskId);
        task.setName(name);
        task = m_dbAccess.writeTask(task);
        m_dbAccess.writeSystemTaskLog(taskId,
                                      "Name was set to " + name + "% from " +
                                      currentUser.getFirstname() + " " +
                                      currentUser.getLastname() + ".");
    }
    /**
     * Sets a new parent-group for an already existing group in the Cms.<BR/>
     *
     * Only the admin can do this.<P/>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param groupName The name of the group that should be written to the Cms.
     * @param parentGroupName The name of the parentGroup to set, or null if the parent
     * group should be deleted.
     * @exception CmsException  Throws CmsException if operation was not succesfull.
     */
    public void setParentGroup(CmsUser currentUser, CmsProject currentProject,
                               String groupName, String parentGroupName)
        throws CmsException {

        // Check the security
        if( isAdmin(currentUser, currentProject) ) {
            CmsGroup group = readGroup(currentUser, currentProject, groupName);
            int parentGroupId = C_UNKNOWN_ID;

            // if the group exists, use its id, else set to unknown.
            if( parentGroupName != null ) {
                parentGroupId = readGroup(currentUser, currentProject, parentGroupName).getId();
            }

            group.setParentId(parentGroupId);

            // write the changes to the cms
            writeGroup(currentUser,currentProject,group);
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + groupName,
                CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Sets the password for a user.
     *
     * Only a adminstrator can do this.<P/>
     *
     * <B>Security:</B>
     * Users, which are in the group "administrators" are granted.<BR/>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param username The name of the user.
     * @param newPassword The new password.
     *
     * @exception CmsException Throws CmsException if operation was not succesfull.
     */
    public void setPassword(CmsUser currentUser, CmsProject currentProject,
                            String username, String newPassword)
        throws CmsException {
        // check the length of the new password.
        if(newPassword.length() < C_PASSWORD_MINIMUMSIZE) {
            throw new CmsException("[" + this.getClass().getName() + "] " + username,
                CmsException.C_SHORT_PASSWORD);
        }

        if( isAdmin(currentUser, currentProject) ) {
            m_dbAccess.setPassword(username, newPassword);
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + username,
                CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Sets the password for a user.
     *
     * Every user who knows the username and the password can do this.<P/>
     *
     * <B>Security:</B>
     * Users, who knows the username and the old password are granted.<BR/>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param username The name of the user.
     * @param oldPassword The new password.
     * @param newPassword The new password.
     *
     * @exception CmsException Throws CmsException if operation was not succesfull.
     */
    public void setPassword(CmsUser currentUser, CmsProject currentProject,
                            String username, String oldPassword, String newPassword)
        throws CmsException {
        // check the length of the new password.
        if(newPassword.length() < C_PASSWORD_MINIMUMSIZE) {
            throw new CmsException("[" + this.getClass().getName() + "] " + username,
                CmsException.C_SHORT_PASSWORD);
        }

        // read the user
        CmsUser user;
        try {
            user = m_dbAccess.readUser(username, oldPassword, C_USER_TYPE_SYSTEMUSER);
        } catch(CmsException exc) {
            // this is no system-user - maybe a webuser?
            try{
                user = m_dbAccess.readUser(username, oldPassword, C_USER_TYPE_WEBUSER);
            } catch(CmsException e) {
                throw exc;
            }
        }
        //if( !anonymousUser(currentUser, currentProject).equals( currentUser )) {
            m_dbAccess.setPassword(username, newPassword);
        //} else {
        //    throw new CmsException("[" + this.getClass().getName() + "] " + username,
        //        CmsException.C_NO_ACCESS);
        //}
    }

    /**
     * Set priority of a task
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param taskid The Id of the task to set the percentage.
     * @param new priority value
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void setPriority(CmsUser currentUser, CmsProject currentProject,
                            int taskId, int priority)
        throws CmsException {
        CmsTask task = m_dbAccess.readTask(taskId);
        task.setPriority(priority);
        task = m_dbAccess.writeTask(task);
        m_dbAccess.writeSystemTaskLog(taskId,
                                      "Priority was set to " + priority + " from " +
                                      currentUser.getFirstname() + " " +
                                      currentUser.getLastname() + ".");
    }
    /**
     * Sets the recovery password for a user.
     *
     * Only a adminstrator or the curretuser can do this.<P/>
     *
     * <B>Security:</B>
     * Users, which are in the group "administrators" are granted.<BR/>
     * Current users can change their own password.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param username The name of the user.
     * @param password The password of the user.
     * @param newPassword The new recoveryPassword to be set.
     *
     * @exception CmsException Throws CmsException if operation was not succesfull.
     */
    public void setRecoveryPassword(CmsUser currentUser, CmsProject currentProject,
                            String username, String password, String newPassword)
        throws CmsException {
        // check the length of the new password.
        if(newPassword.length() < C_PASSWORD_MINIMUMSIZE) {
            throw new CmsException("[" + this.getClass().getName() + "] " + username,
                CmsException.C_SHORT_PASSWORD);
        }

        // read the user
        CmsUser user;
        try {
            user = readUser(currentUser, currentProject, username, password);
        } catch(CmsException exc) {
            // this is no system-user - maybe a webuser?
            user = readWebUser(currentUser, currentProject, username, password);
        }
        //if( ! anonymousUser(currentUser, currentProject).equals( currentUser ) &&
        //    ( isAdmin(user, currentProject) || user.equals(currentUser)) ) {
            m_dbAccess.setRecoveryPassword(username, newPassword);
        //} else {
        //    throw new CmsException("[" + this.getClass().getName() + "] " + username,
        //        CmsException.C_NO_ACCESS);
        //}
    }
    /**
     * Set a Parameter for a task.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param taskId The Id of the task.
     * @param parName Name of the parameter.
     * @param parValue Value if the parameter.
     *
     * @return The id of the inserted parameter or 0 if the parameter already exists for this task.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void setTaskPar(CmsUser currentUser, CmsProject currentProject,
                           int taskId, String parName, String parValue)
        throws CmsException {
        m_dbAccess.setTaskPar(taskId, parName, parValue);
    }
    /**
     * Set timeout of a task
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param taskid The Id of the task to set the percentage.
     * @param new timeout value
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void setTimeout(CmsUser currentUser, CmsProject currentProject,
                           int taskId, long timeout)
        throws CmsException {
        CmsTask task = m_dbAccess.readTask(taskId);
        java.sql.Timestamp timestamp = new java.sql.Timestamp(timeout);
        task.setTimeOut(timestamp);
        task = m_dbAccess.writeTask(task);
        m_dbAccess.writeSystemTaskLog(taskId,
                                      "Timeout was set to " + timeout + " from " +
                                      currentUser.getFirstname() + " " +
                                      currentUser.getLastname() + ".");
    }
    /**
     * This method stores sessiondata into the database. It is used
     * for sessionfailover.
     *
     * @param sessionId the id of the session.
     * @param isNew determines, if the session is new or not.
     * @return data the sessionData.
     */
    public void storeSession(String sessionId, Hashtable sessionData)
        throws CmsException {

        // update the session
        int rowCount = m_dbAccess.updateSession(sessionId, sessionData);
        if(rowCount != 1) {
            // the entry dosn't exists - create it
            m_dbAccess.createSession(sessionId, sessionData);
        }
    }

    /**
     * Undo all changes in the resource, restore the online file.
     *
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resourceName The name of the resource to be restored.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void undoChanges(CmsUser currentUser, CmsProject currentProject, String resourceName)
        throws CmsException {
        CmsProject onlineProject = readProject(currentUser, currentProject, C_PROJECT_ONLINE_ID);
        // change folder or file?
        if (resourceName.endsWith("/")){
            // read the resource from the online project
            CmsFolder onlineFolder = readFolder(currentUser, onlineProject, resourceName);
            // read the resource from the offline project and change the data
            CmsFolder offlineFolder = readFolder(currentUser, currentProject, resourceName);
            CmsFolder restoredFolder = new CmsFolder(offlineFolder.getResourceId(), offlineFolder.getParentId(),
                                            offlineFolder.getFileId(), offlineFolder.getResourceName(),
                                            onlineFolder.getType(), onlineFolder.getFlags(),
                                            onlineFolder.getOwnerId(), onlineFolder.getGroupId(),
                                            currentProject.getId(), onlineFolder.getAccessFlags(),
                                            C_STATE_UNCHANGED, offlineFolder.isLockedBy(), offlineFolder.getDateCreated(),
                                            offlineFolder.getDateLastModified(), currentUser.getId(),
                                            currentProject.getId());
            // write the file in the offline project
            // has the user write-access?
            if( accessWrite(currentUser, currentProject, (CmsResource)restoredFolder) ) {
                // write-access  was granted - write the folder without setting state = changed
                m_dbAccess.writeFolder(currentProject, restoredFolder, false, currentUser.getId());
                // restore the properties in the offline project
                m_dbAccess.deleteAllProperties(currentProject.getId(),restoredFolder);
                Hashtable propertyInfos = m_dbAccess.readAllProperties(onlineProject.getId(),onlineFolder,onlineFolder.getType());
                m_dbAccess.writeProperties(propertyInfos,currentProject.getId(),restoredFolder,restoredFolder.getType());
                m_propertyCache.clear();
                // update the cache
                this.clearResourceCache(restoredFolder.getResourceName());
                m_accessCache.clear();
                // inform about the file-system-change
                fileSystemChanged(false);
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + restoredFolder.getAbsolutePath(),
                    CmsException.C_NO_ACCESS);
            }
        } else {
            // read the file from the online project
            CmsFile onlineFile = readFile(currentUser, onlineProject, resourceName);
            // read the file from the offline project and change the data
            CmsFile offlineFile = readFile(currentUser, currentProject, resourceName);
            CmsFile restoredFile = new CmsFile(offlineFile.getResourceId(), offlineFile.getParentId(),
                                            offlineFile.getFileId(), offlineFile.getResourceName(),
                                            onlineFile.getType(), onlineFile.getFlags(),
                                            onlineFile.getOwnerId(), onlineFile.getGroupId(),
                                            currentProject.getId(), onlineFile.getAccessFlags(),
                                            C_STATE_UNCHANGED, offlineFile.isLockedBy(), onlineFile.getLauncherType(),
                                            onlineFile.getLauncherClassname(), offlineFile.getDateCreated(),
                                            offlineFile.getDateLastModified(), currentUser.getId(),
                                            onlineFile.getContents(), onlineFile.getLength(), currentProject.getId());
            // write the file in the offline project
            // has the user write-access?
            if( accessWrite(currentUser, currentProject, (CmsResource)restoredFile) ) {
                // write-acces  was granted - write the file without setting state = changed
                m_dbAccess.writeFile(currentProject,
                               onlineProject(currentUser, currentProject), restoredFile, false);
                // restore the properties in the offline project
                m_dbAccess.deleteAllProperties(currentProject.getId(),restoredFile);
                Hashtable propertyInfos = m_dbAccess.readAllProperties(onlineProject.getId(),onlineFile,onlineFile.getType());
                m_dbAccess.writeProperties(propertyInfos,currentProject.getId(),restoredFile,restoredFile.getType());
                m_propertyCache.clear();
                // update the cache
                this.clearResourceCache(restoredFile.getResourceName());
                m_accessCache.clear();
                // inform about the file-system-change
                fileSystemChanged(false);
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + restoredFile.getAbsolutePath(),
                    CmsException.C_NO_ACCESS);
            }
        }
    }

    /**
     * Unlocks all resources in this project.
     *
     * <B>Security</B>
     * Only the admin or the owner of the project can do this.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param id The id of the project to be published.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void unlockProject(CmsUser currentUser, CmsProject currentProject, int id)
        throws CmsException {
        // read the project.
        CmsProject project = readProject(currentUser, currentProject, id);
        // check the security
        if( (isAdmin(currentUser, currentProject) ||
            isManagerOfProject(currentUser, project) ) &&
            (project.getFlags() == C_PROJECT_STATE_UNLOCKED )) {

            // unlock all resources in the project
            m_dbAccess.unlockProject(project);
            this.clearResourceCache();
            m_projectCache.clear();
        } else {
             throw new CmsException("[" + this.getClass().getName() + "] " + id,
                CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Unlocks a resource.<br>
     *
     * Only a resource in an offline project can be unlock. The state of the resource
     * is set to CHANGED (1).
     * If the content of this resource is not exisiting in the offline project already,
     * it is read from the online project and written into the offline project.
     * Only the user who locked a resource can unlock it.
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user had locked the resource before</li>
     * </ul>
     *
     * @param user The user who wants to lock the file.
     * @param project The project in which the resource will be used.
     * @param resourcename The complete path to the resource to lock.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public void unlockResource(CmsUser currentUser,CmsProject currentProject,
                               String resourcename)
        throws CmsException {

        CmsResource  cmsResource=null;

        // read the resource, that should be locked
        if (resourcename.endsWith("/")) {
              cmsResource = readFolder(currentUser,currentProject,resourcename);
             } else {
              cmsResource = (CmsFile)readFileHeader(currentUser,currentProject,resourcename);
        }

        // check, if the user may lock the resource
        if( accessUnlock(currentUser, currentProject, cmsResource) ) {
            // unlock the resource.
            if (cmsResource.isLocked()){
                // check if the resource is locked by the actual user
                if (cmsResource.isLockedBy()==currentUser.getId()) {

                // unlock the resource
                cmsResource.setLocked(C_UNKNOWN_ID);

                //update resource
                m_dbAccess.updateLockstate(cmsResource, cmsResource.getLockedInProject());

                // update the cache
                this.clearResourceCache(resourcename);
            } else {
                 throw new CmsException("[" + this.getClass().getName() + "] " +
                    resourcename + CmsException.C_NO_ACCESS);
            }
        }

            // if this resource is a folder -> lock all subresources, too
            if(cmsResource.isFolder()) {
                Vector files = getFilesInFolder(currentUser,currentProject, cmsResource.getResourceName());
                Vector folders = getSubFolders(currentUser,currentProject, cmsResource.getResourceName());
                CmsResource currentResource;

                // lock all files in this folder
                for(int i = 0; i < files.size(); i++ ) {
                    currentResource = (CmsResource)files.elementAt(i);
                    if (currentResource.getState() != C_STATE_DELETED) {
                        unlockResource(currentUser, currentProject, currentResource.getResourceName());
                    }
                }

                // lock all files in this folder
                for(int i = 0; i < folders.size(); i++) {
                    currentResource = (CmsResource)folders.elementAt(i);
                    if (currentResource.getState() != C_STATE_DELETED) {
                        unlockResource(currentUser, currentProject, currentResource.getResourceName());
                    }
                }
            }
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + resourcename,
                CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Checks if a user is member of a group.<P/>
     *
     * <B>Security:</B>
     * All users are granted, except the anonymous user.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param callingUser The user who wants to use this method.
     * @param nameuser The name of the user to check.
     * @param groupname The name of the group to check.
     * @return True or False
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public boolean userInGroup(CmsUser currentUser, CmsProject currentProject,
                               String username, String groupname)
        throws CmsException {
         Vector groups = getGroupsOfUser(currentUser,currentProject,username);
         CmsGroup group;
         for(int z = 0; z < groups.size(); z++) {
             group = (CmsGroup) groups.elementAt(z);
             if(groupname.equals(group.getName())) {
                 return true;
             }
         }
         return false;
    }

    /**
     * Checks ii characters in a String are allowed for filenames
     *
     * @param filename String to check
     *
     * @exception throws a exception, if the check fails.
     */
    protected void validFilename( String filename )
        throws CmsException {
        if (filename == null) {
            throw new CmsException("[" + this.getClass().getName() + "] " + filename,
                CmsException.C_BAD_NAME);
        }

        int l = filename.trim().length();

        if (l == 0 || filename.startsWith(".")) {
            throw new CmsException("[" + this.getClass().getName() + "] " + filename,
                CmsException.C_BAD_NAME);
        }

        for (int i=0; i<l; i++) {
            char c = filename.charAt(i);
            if (
                ((c < 'a') || (c > 'z')) &&
                ((c < '0') || (c > '9')) &&
                ((c < 'A') || (c > 'Z')) &&
                (c != '-') && (c != '.') &&
                (c != '_') && (c != '~') &&
                (c != '$')
                ) {
                throw new CmsException("[" + this.getClass().getName() + "] " + filename,
                    CmsException.C_BAD_NAME);
            }
        }
    }

    /**
     * Checks ii characters in a String are allowed for filenames
     *
     * @param filename String to check
     *
     * @exception throws a exception, if the check fails.
     */
    protected void validTaskname( String taskname )
        throws CmsException {
        if (taskname == null) {
            throw new CmsException("[" + this.getClass().getName() + "] " + taskname,
                CmsException.C_BAD_NAME);
        }

        int l = taskname.length();

        if (l == 0) {
            throw new CmsException("[" + this.getClass().getName() + "] " + taskname,
                CmsException.C_BAD_NAME);
        }

        for (int i=0; i<l; i++) {
            char c = taskname.charAt(i);
            if (
                ((c < '�') || (c > '�')) &&
                ((c < '�') || (c > '�')) &&
                ((c < 'a') || (c > 'z')) &&
                ((c < '0') || (c > '9')) &&
                ((c < 'A') || (c > 'Z')) &&
                (c != '-') && (c != '.') &&
                (c != '_') && (c != '~') &&
                (c != ' ') && (c != '�')
                ) {
                throw new CmsException("[" + this.getClass().getName() + "] " + taskname,
                    CmsException.C_BAD_NAME);
            }
        }
    }


/**
 * Checks ii characters in a String are allowed for names
 *
 * @param name String to check
 *
 * @exception throws a exception, if the check fails.
 */
protected void validName(String name, boolean blank) throws CmsException {
    if (name == null || name.length() == 0 || name.trim().length() == 0) {
        throw new CmsException("[" + this.getClass().getName() + "] " + name, CmsException.C_BAD_NAME);
    }
    // throw exception if no blanks are allowed
    if (!blank) {
        int l = name.length();
        for (int i = 0; i < l; i++) {
            char c = name.charAt(i);
            if (c == ' ') {
                throw new CmsException("[" + this.getClass().getName() + "] " + name, CmsException.C_BAD_NAME);
            }
        }
    }

    /*
    for (int i=0; i<l; i++) {
    char c = name.charAt(i);
    if (
    ((c < 'a') || (c > 'z')) &&
    ((c < '0') || (c > '9')) &&
    ((c < 'A') || (c > 'Z')) &&
    (c != '-') && (c != '.') &&
    (c != '_') &&   (c != '~')
    ) {
    throw new CmsException("[" + this.getClass().getName() + "] " + name,
    CmsException.C_BAD_NAME);
    }
    }
    */
}
    /**
     * Writes the export-path for the system.
     * This path is used for db-export and db-import.
     *
     * <B>Security:</B>
     * Users, which are in the group "administrators" are granted.<BR/>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param mountpoint The mount point in the Cms filesystem.
     */
    public void writeExportPath(CmsUser currentUser, CmsProject currentProject, String path)
        throws CmsException {
        // check the security
        if( isAdmin(currentUser, currentProject) ) {

            // security is ok - write the exportpath.
            if(m_dbAccess.readSystemProperty(C_SYSTEMPROPERTY_EXPORTPATH) == null) {
                // the property wasn't set before.
                m_dbAccess.addSystemProperty(C_SYSTEMPROPERTY_EXPORTPATH, path);
            } else {
                // overwrite the property.
                m_dbAccess.writeSystemProperty(C_SYSTEMPROPERTY_EXPORTPATH, path);
            }

        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + path,
                CmsException.C_NO_ACCESS);
        }
    }
     /**
     * Writes a file to the Cms.<br>
     *
     * A file can only be written to an offline project.<br>
     * The state of the resource is set to  CHANGED (1). The file content of the file
     * is either updated (if it is already existing in the offline project), or created
     * in the offline project (if it is not available there).<br>
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can write the resource</li>
     * <li>the resource is locked by the callingUser</li>
     * </ul>
     *
     * @param currentUser The user who own this file.
     * @param currentProject The project in which the resource will be used.
     * @param file The name of the file to write.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public void writeFile(CmsUser currentUser, CmsProject currentProject,
                          CmsFile file)
        throws CmsException {

        // has the user write-access?
        if( accessWrite(currentUser, currentProject, (CmsResource)file) ) {

            // write-acces  was granted - write the file.

            m_dbAccess.writeFile(currentProject,
                               onlineProject(currentUser, currentProject), file, true, currentUser.getId());

            if (file.getState()==C_STATE_UNCHANGED) {
                file.setState(C_STATE_CHANGED);
            }

            // update the cache
            this.clearResourceCache(file.getResourceName());
            m_accessCache.clear();
            // inform about the file-system-change
            fileSystemChanged(false);
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + file.getAbsolutePath(),
                CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Writes the file extensions
     *
     * <B>Security:</B>
     * Users, which are in the group "Administrators" are authorized.<BR/>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param extensions Holds extensions as keys and resourcetypes (Stings) as values
     */

    public void writeFileExtensions(CmsUser currentUser, CmsProject currentProject,
                                    Hashtable extensions)
        throws CmsException {
        if (extensions != null) {
            if (isAdmin(currentUser, currentProject)) {
                Enumeration enu=extensions.keys();
                while (enu.hasMoreElements()) {
                    String key=(String)enu.nextElement();
                    validFilename(key);
                }
                if (m_dbAccess.readSystemProperty(C_SYSTEMPROPERTY_EXTENSIONS) == null) {
                    // the property wasn't set before.
                    m_dbAccess.addSystemProperty(C_SYSTEMPROPERTY_EXTENSIONS, extensions);
                } else {
                    // overwrite the property.
                    m_dbAccess.writeSystemProperty(C_SYSTEMPROPERTY_EXTENSIONS, extensions);
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + extensions.size(),
                    CmsException.C_NO_ACCESS);
            }
        }
    }
     /**
     * Writes a fileheader to the Cms.<br>
     *
     * A file can only be written to an offline project.<br>
     * The state of the resource is set to  CHANGED (1). The file content of the file
     * is either updated (if it is already existing in the offline project), or created
     * in the offline project (if it is not available there).<br>
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can write the resource</li>
     * <li>the resource is locked by the callingUser</li>
     * </ul>
     *
     * @param currentUser The user who own this file.
     * @param currentProject The project in which the resource will be used.
     * @param file The file to write.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public void writeFileHeader(CmsUser currentUser, CmsProject currentProject,
                                CmsFile file)
        throws CmsException {
        // has the user write-access?
        if( accessWrite(currentUser, currentProject, (CmsResource)file) ) {
            // write-acces  was granted - write the file.
            m_dbAccess.writeFileHeader(currentProject, file,true, currentUser.getId());
            if (file.getState()==C_STATE_UNCHANGED) {
                file.setState(C_STATE_CHANGED);
            }
            // update the cache
            this.clearResourceCache(file.getResourceName());
            // inform about the file-system-change
            m_accessCache.clear();
            fileSystemChanged(false);
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + file.getAbsolutePath(),
                CmsException.C_NO_ACCESS);
        }
    }
     /**
     * Writes an already existing group in the Cms.<BR/>
     *
     * Only the admin can do this.<P/>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param group The group that should be written to the Cms.
     * @exception CmsException  Throws CmsException if operation was not succesfull.
     */
    public void writeGroup(CmsUser currentUser, CmsProject currentProject,
                           CmsGroup group)
        throws CmsException {
        // Check the security
        if( isAdmin(currentUser, currentProject) ) {
            m_dbAccess.writeGroup(group);
            m_groupCache.put(group.getName(),group);
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + group.getName(),
                CmsException.C_NO_ACCESS);
        }

    }
    /**
     * Writes a couple of propertyinformation for a file or folder.
     *
     * <B>Security</B>
     * Only the user is granted, who has the right to write the resource.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The name of the resource of which the propertyinformation
     * has to be read.
     * @param propertyinfos A Hashtable with propertydefinition- propertyinfo-pairs as strings.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public void writeProperties(CmsUser currentUser, CmsProject currentProject,
                                      String resource, Hashtable propertyinfos)
        throws CmsException {
        // read the resource


        CmsResource res = readFileHeader(currentUser,currentProject, resource);

        // check the security
        if( ! accessWrite(currentUser, currentProject, res) ) {
             throw new CmsException("[" + this.getClass().getName() + "] " + resource,
                CmsException.C_NO_ACCESS);
        }


        m_dbAccess.writeProperties(propertyinfos,currentProject.getId(),res,res.getType());
        m_propertyCache.clear();
        if (res.getState()==C_STATE_UNCHANGED) {
            res.setState(C_STATE_CHANGED);
        }
        if(res.isFile()){
            m_dbAccess.writeFileHeader(currentProject, (CmsFile) res, false, currentUser.getId());
        } else {
            m_dbAccess.writeFolder(currentProject, readFolder(currentUser,currentProject, resource), false, currentUser.getId());
        }
        // update the cache
        this.clearResourceCache(resource);
    }
    /**
     * Writes a propertyinformation for a file or folder.
     *
     * <B>Security</B>
     * Only the user is granted, who has the right to write the resource.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param resource The name of the resource of which the propertyinformation has
     * to be read.
     * @param property The propertydefinition-name of which the propertyinformation has to be set.
     * @param value The value for the propertyinfo to be set.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public void writeProperty(CmsUser currentUser, CmsProject currentProject,
                                     String resource, String property, String value)
        throws CmsException {

       // read the resource
        CmsResource res = readFileHeader(currentUser,currentProject, resource);

        // check the security
        if( ! accessWrite(currentUser, currentProject, res) ) {
             throw new CmsException("[" + this.getClass().getName() + "] " + resource,
                CmsException.C_NO_ACCESS);
        }

        m_dbAccess.writeProperty(property, currentProject.getId(),value, res,res.getType());
        m_propertyCache.clear();
        // set the file-state to changed
        if(res.isFile()){
            m_dbAccess.writeFileHeader(currentProject, (CmsFile) res, true, currentUser.getId());
            if (res.getState()==C_STATE_UNCHANGED) {
                res.setState(C_STATE_CHANGED);
            }
        } else {
            if (res.getState()==C_STATE_UNCHANGED) {
                res.setState(C_STATE_CHANGED);
            }
            m_dbAccess.writeFolder(currentProject, readFolder(currentUser,currentProject, resource), true, currentUser.getId());
        }
        // update the cache
        this.clearResourceCache(resource);
    }

    /**
     * Updates the propertydefinition for the resource type.<BR/>
     *
     * <B>Security</B>
     * Only the admin can do this.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param propertydef The propertydef to be deleted.
     *
     * @return The propertydefinition, that was written.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsPropertydefinition writePropertydefinition(CmsUser currentUser,
                                                   CmsProject currentProject,
                                                   CmsPropertydefinition propertydef)
        throws CmsException {
     // check the security
        if( isAdmin(currentUser, currentProject) ) {
            m_propertyDefVectorCache.clear();
            return( m_dbAccess.writePropertydefinition(propertydef) );
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + propertydef.getName(),
                CmsException.C_NO_ACCESS);
        }
    }
    /**
     * Writes a new user tasklog for a task.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param taskid The Id of the task .
     * @param comment Description for the log
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void writeTaskLog(CmsUser currentUser, CmsProject currentProject,
                             int taskid, String comment)
        throws CmsException  {

        m_dbAccess.writeTaskLog(taskid, currentUser.getId(),
                                new java.sql.Timestamp(System.currentTimeMillis()),
                                comment, C_TASKLOG_USER);
    }
    /**
     * Writes a new user tasklog for a task.
     *
     * <B>Security:</B>
     * All users are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param taskid The Id of the task .
     * @param comment Description for the log
     * @param tasktype Type of the tasklog. User tasktypes must be greater then 100.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void writeTaskLog(CmsUser currentUser, CmsProject currentProject,
                             int taskid, String comment, int type)
        throws CmsException {

        m_dbAccess.writeTaskLog(taskid, currentUser.getId(),
                                new java.sql.Timestamp(System.currentTimeMillis()),
                                comment, type);
    }
    /**
     * Updates the user information.<BR/>
     *
     * Only the administrator can do this.<P/>
     *
     * <B>Security:</B>
     * Only users, which are in the group "administrators" are granted.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param user The  user to be updated.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public void writeUser(CmsUser currentUser, CmsProject currentProject,
                          CmsUser user)
        throws CmsException {
        // Check the security
        if( isAdmin(currentUser, currentProject) || (currentUser.equals(user)) ) {

            // prevent the admin to be set disabled!
            if( isAdmin(user, currentProject) ) {
                user.setEnabled();
            }
            m_dbAccess.writeUser(user);
            // update the cache
            m_userCache.put(user.getName()+user.getType(),user);
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + user.getName(),
                CmsException.C_NO_ACCESS);
        }
    }
     /**
     * Updates the user information of a web user.<BR/>
     *
     * Only a web user can be updated this way.<P/>
     *
     * <B>Security:</B>
     * Only users of the user type webuser can be updated this way.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param user The  user to be updated.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public void writeWebUser(CmsUser currentUser, CmsProject currentProject,
                          CmsUser user)
        throws CmsException {
        // Check the security
        if( user.getType() == C_USER_TYPE_WEBUSER) {

            m_dbAccess.writeUser(user);
            // update the cache
            m_userCache.put(user.getName()+user.getType(),user);
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + user.getName(),
                CmsException.C_NO_ACCESS);
        }
    }

    /**
     * Changes the project-id of a resource to the new project
     * for publishing the resource directly
     *
     * @param newProjectId The new project-id
     * @param resourcename The name of the resource to change
     */
    public void changeLockedInProject(int projectId, String resourcename) throws CmsException{
        m_dbAccess.changeLockedInProject(projectId, resourcename);
        this.clearResourceCache(resourcename);
    }

    /**
     * Check if the history is enabled
     *
     * @return boolean Is true if history is enabled
     */
    public boolean isHistoryEnabled(){
        return this.m_enableHistory;
    }

    /**
     * Get the next version id for the published backup resources
     *
     * @return int The new version id
     */
    public int getBackupVersionId(){
        return m_dbAccess.getBackupVersionId();
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
     * @exception CmsException Throws CmsException if operation was not succesful.
     */

    public void backupProject(int projectId, int versionId,
                              long publishDate, CmsUser currentUser) throws CmsException{
        CmsProject project = m_dbAccess.readProject(projectId);
        m_dbAccess.backupProject(project, versionId, publishDate, currentUser);
    }

    /**
     * Checks if this is a valid group for webusers
     *
     * @param group The group to be checked
     * @return boolean If the group does not belong to Users, Administrators or Projectmanagers return true
     */
    protected boolean isWebgroup(CmsGroup group) throws CmsException{
        boolean result = true;
        try{
            int user = m_dbAccess.readGroup(C_GROUP_USERS).getId();
            int admin = m_dbAccess.readGroup(C_GROUP_ADMIN).getId();
            int manager = m_dbAccess.readGroup(C_GROUP_PROJECTLEADER).getId();
            if((group.getId() == user) || (group.getId() == admin) || (group.getId() == manager)){
                return false;
            } else {
                int parentId = group.getParentId();
                // check if the group belongs to Users, Administrators or Projectmanager
                if (parentId != C_UNKNOWN_ID){
                    // check is the parentgroup is a webgroup
                    return isWebgroup(m_dbAccess.readGroup(parentId));
                }
            }
        } catch (CmsException e){
            throw e;
        }
        return true;
    }


    /**
     * Gets the Crontable.
     *
     * <B>Security:</B>
     * All users are garnted<BR/>
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     *
     * @return the crontable.
     */
    public String readCronTable(CmsUser currentUser, CmsProject currentProject)
        throws CmsException {
        String retValue = (String) m_dbAccess.readSystemProperty(C_SYSTEMPROPERTY_CRONTABLE);
        if(retValue == null) {
            return "";
        } else {
            return retValue;
        }
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
    public void writeCronTable(CmsUser currentUser, CmsProject currentProject, String crontable)
        throws CmsException {
        if(isAdmin(currentUser, currentProject)) {
            if(m_dbAccess.readSystemProperty(C_SYSTEMPROPERTY_CRONTABLE) == null) {
                m_dbAccess.addSystemProperty(C_SYSTEMPROPERTY_CRONTABLE, crontable);
            } else {
                m_dbAccess.writeSystemProperty(C_SYSTEMPROPERTY_CRONTABLE, crontable);
            }
        } else {
            throw new CmsException("No access to write crontable", CmsException.C_NO_ACCESS);
        }
    }

    /**
     * Clears all the depending caches when a resource was changed
     *
     * @param resourcename The name of the changed resource
     */
    public void clearResourceCache(String resourcename){
        m_resourceCache.remove(resourcename);
        m_subresCache.clear();
    }

    /**
     * Clears all the depending caches when a resource was changed
     *
     */
    public void clearResourceCache(){
        m_resourceCache.clear();
        m_subresCache.clear();
    }

    /**
     * Method to encrypt the passwords.
     *
     * @param value The value to encrypt.
     * @return The encrypted value.
     */
    public String digest(String value) {
        return m_dbAccess.digest(value);
    }

    /**
     *
     */
    private String getFirstTagFromManifest(String importFile) throws CmsException{
        String firstTag = "";
        ZipFile importZip = null;
		Document docXml = null;
		BufferedReader xmlReader = null;
		// get the import resource
        File importResource = new File(CmsBase.getAbsolutePath(importFile));
        try {
            // if it is a file it must be a zip-file
            if(importResource.isFile()) {
                importZip = new ZipFile(importResource);
            }
            // is this a zip-file?
            if(importZip != null) {
                // yes
                ZipEntry entry = importZip.getEntry(C_EXPORT_XMLFILENAME);
                InputStream stream = importZip.getInputStream(entry);
                xmlReader =  new BufferedReader( new InputStreamReader(stream));
            } else {
                // no - use directory
                File xmlFile = new File(importResource, C_EXPORT_XMLFILENAME);
                xmlReader =  new BufferedReader(new FileReader(xmlFile));
            }
            docXml = A_CmsXmlContent.getXmlParser().parse(xmlReader);
		    xmlReader.close();
            firstTag = docXml.getDocumentElement().getNodeName();
		} catch(Exception exc) {
		     throw new CmsException(CmsException.C_UNKNOWN_EXCEPTION, exc);
        }
        if (importZip != null){
            try{
                importZip.close();
            } catch (IOException exc) {
                throw new CmsException(CmsException.C_UNKNOWN_EXCEPTION, exc);
            }
        }
        return firstTag;
    }

    /**
     * This is the port the workplace access is limited to. With the opencms.properties
     * the access to the workplace can be limited to a user defined port. With this
     * feature a firewall can block all outside requests to this port with the result
     * the workplace is only available in the local net segment.
     * @returns the portnumber or -1 if no port is set.
     */
    public int getLimitedWorkplacePort() {
        return m_limitedWorkplacePort;
    }

}
