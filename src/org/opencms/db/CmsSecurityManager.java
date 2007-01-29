/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/CmsSecurityManager.java,v $
 * Date   : $Date: 2007/01/29 09:44:54 $
 * Version: $Revision: 1.97.4.32 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
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
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.db;

import org.opencms.configuration.CmsConfigurationManager;
import org.opencms.configuration.CmsSystemConfiguration;
import org.opencms.file.CmsBackupProject;
import org.opencms.file.CmsBackupResource;
import org.opencms.file.CmsFile;
import org.opencms.file.CmsFolder;
import org.opencms.file.CmsGroup;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsRequestContext;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResource.CmsResourceCopyMode;
import org.opencms.file.CmsResource.CmsResourceDeleteMode;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.CmsResource.CmsResourceUndoMode;
import org.opencms.file.CmsUser;
import org.opencms.file.CmsVfsException;
import org.opencms.file.CmsVfsResourceAlreadyExistsException;
import org.opencms.file.CmsVfsResourceNotFoundException;
import org.opencms.file.types.CmsResourceTypeJsp;
import org.opencms.i18n.CmsMessageContainer;
import org.opencms.lock.CmsLock;
import org.opencms.lock.CmsLockException;
import org.opencms.lock.CmsLockFilter;
import org.opencms.lock.CmsLockManager;
import org.opencms.lock.CmsLockType;
import org.opencms.main.CmsException;
import org.opencms.main.CmsInitException;
import org.opencms.main.CmsLog;
import org.opencms.main.CmsMultiException;
import org.opencms.main.OpenCms;
import org.opencms.publish.CmsPublishEngine;
import org.opencms.relations.CmsRelationFilter;
import org.opencms.report.I_CmsReport;
import org.opencms.security.CmsAccessControlEntry;
import org.opencms.security.CmsAccessControlList;
import org.opencms.security.CmsOrganizationalUnit;
import org.opencms.security.CmsPermissionSet;
import org.opencms.security.CmsPermissionSetCustom;
import org.opencms.security.CmsPermissionViolationException;
import org.opencms.security.CmsRole;
import org.opencms.security.CmsRoleViolationException;
import org.opencms.security.CmsSecurityException;
import org.opencms.security.I_CmsPrincipal;
import org.opencms.util.CmsFileUtil;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.CmsUUID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.logging.Log;

/**
 * The OpenCms security manager.<p>
 * 
 * The security manager checks the permissions required for a user action invoke by the Cms object. If permissions 
 * are granted, the security manager invokes a method on the OpenCms driver manager to access the database.<p>
 * 
 * @author Thomas Weckert 
 * @author Michael Moossen 
 * 
 * @since 6.0.0
 */
public final class CmsSecurityManager {

    /** Indicates allowed permissions. */
    public static final int PERM_ALLOWED = 0;

    /** Indicates denied permissions. */
    public static final int PERM_DENIED = 1;

    /** Indicates a resource was filtered during permission check. */
    public static final int PERM_FILTERED = 2;

    /** Indicates a resource was not locked for a write / control operation. */
    public static final int PERM_NOTLOCKED = 3;

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsSecurityManager.class);

    /** Indicates allowed permissions. */
    private static final Integer PERM_ALLOWED_INTEGER = new Integer(PERM_ALLOWED);

    /** Indicates denied permissions. */
    private static final Integer PERM_DENIED_INTEGER = new Integer(PERM_DENIED);

    /** The factory to create runtime info objects. */
    protected I_CmsDbContextFactory m_dbContextFactory;

    /** The initialized OpenCms driver manager to access the database. */
    protected CmsDriverManager m_driverManager;

    /** The class used for cache key generation. */
    private I_CmsCacheKey m_keyGenerator;

    /** The lock manager. */
    private CmsLockManager m_lockManager;

    /** Cache for permission checks. */
    private Map m_permissionCache;

    /**
     * Default constructor.<p>
     */
    private CmsSecurityManager() {

        // intentionally left blank
    }

    /**
     * Creates a new instance of the OpenCms security manager.<p>
     * 
     * @param configurationManager the configuation manager
     * @param runtimeInfoFactory the initialized OpenCms runtime info factory
     * @param publishEngine the publish engine
     * 
     * @return a new instance of the OpenCms security manager
     * 
     * @throws CmsInitException if the securtiy manager could not be initialized
     */
    public static CmsSecurityManager newInstance(
        CmsConfigurationManager configurationManager,
        I_CmsDbContextFactory runtimeInfoFactory,
        CmsPublishEngine publishEngine) throws CmsInitException {

        if (OpenCms.getRunLevel() > OpenCms.RUNLEVEL_2_INITIALIZING) {
            // OpenCms is already initialized
            throw new CmsInitException(org.opencms.main.Messages.get().container(
                org.opencms.main.Messages.ERR_ALREADY_INITIALIZED_0));
        }

        CmsSecurityManager securityManager = new CmsSecurityManager();
        securityManager.init(configurationManager, runtimeInfoFactory, publishEngine);

        return securityManager;
    }

    /**
     * Adds a resource to the given organizational unit.<p>
     * 
     * @param context the current request context
     * @param orgUnit the organizational unit to add the resource to
     * @param resource the resource that is to be added to the organizational unit
     * 
     * @throws CmsException if something goes wrong
     * 
     * @see org.opencms.security.CmsOrgUnitManager#addResourceToOrgUnit(CmsObject, String, String)
     * @see org.opencms.security.CmsOrgUnitManager#removeResourceFromOrgUnit(CmsObject, String, String)
     */
    public void addResourceToOrgUnit(CmsRequestContext context, CmsOrganizationalUnit orgUnit, CmsResource resource)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkRoleForOrgUnit(dbc, CmsRole.ADMINISTRATOR, orgUnit.getName());
            m_driverManager.addResourceToOrgUnit(dbc, orgUnit, resource);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_ADD_RESOURCE_TO_ORGUNIT_2,
                orgUnit.getName(),
                dbc.removeSiteRoot(resource.getRootPath())), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Adds a user to a group.<p>
     *
     * @param context the current request context
     * @param username the name of the user that is to be added to the group
     * @param groupname the name of the group
     * @param readRoles if reading roles or groups
     *
     * @throws CmsException if operation was not succesfull
     */
    public void addUserToGroup(CmsRequestContext context, String username, String groupname, boolean readRoles)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkRoleForOrgUnit(dbc, CmsRole.ACCOUNT_MANAGER, getParentOrganizationalUnit(groupname));
            m_driverManager.addUserToGroup(dbc, username, groupname, readRoles);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_ADD_USER_GROUP_FAILED_2, username, groupname), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Creates a backup of the current project.<p>
     * 
     * @param context the current request context
     * @param tagId the version of the backup
     * @param publishDate the date of publishing
     *
     * @throws CmsException if operation was not succesful
     */
    public void backupProject(CmsRequestContext context, int tagId, long publishDate) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            m_driverManager.backupProject(dbc, tagId, publishDate);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_BACKUP_PROJECT_4,
                new Object[] {
                    new Integer(tagId),
                    dbc.currentProject().getName(),
                    new Integer(dbc.currentProject().getId()),
                    new Long(publishDate)}), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Changes the lock of a resource to the current user, that is "steals" the lock from another user.<p>
     * 
     * @param context the current request context
     * @param resource the resource to change the lock for
     * @throws CmsException if something goes wrong
     * @see org.opencms.file.types.I_CmsResourceType#changeLock(CmsObject, CmsSecurityManager, CmsResource)
     */
    public void changeLock(CmsRequestContext context, CmsResource resource) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        checkOfflineProject(dbc);
        try {
            m_driverManager.changeLock(dbc, resource, CmsLockType.EXCLUSIVE);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_CHANGE_LOCK_OF_RESOURCE_2,
                context.getSitePath(resource),
                " - " + e.getMessage()), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Returns a list with all sub resources of a given folder that have set the given property, 
     * matching the current property's value with the given old value and replacing it by a given new value.<p>
     *
     * @param context the current request context
     * @param resource the resource on which property definition values are changed
     * @param propertyDefinition the name of the propertydefinition to change the value
     * @param oldValue the old value of the propertydefinition
     * @param newValue the new value of the propertydefinition
     * @param recursive if true, change recursively all property values on sub-resources (only for folders)
     * 
     * @return a list with the <code>{@link CmsResource}</code>'s where the property value has been changed
     *
     * @throws CmsVfsException for now only when the search for the oldvalue failed. 
     * @throws CmsException if operation was not successful
     */
    public synchronized List changeResourcesInFolderWithProperty(
        CmsRequestContext context,
        CmsResource resource,
        String propertyDefinition,
        String oldValue,
        String newValue,
        boolean recursive) throws CmsException, CmsVfsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            result = m_driverManager.changeResourcesInFolderWithProperty(
                dbc,
                resource,
                propertyDefinition,
                oldValue,
                newValue,
                recursive);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_CHANGE_RESOURCES_IN_FOLDER_WITH_PROP_4,
                new Object[] {propertyDefinition, oldValue, newValue, context.getSitePath(resource)}), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Checks if the current user has management access to the given project.<p>
     * 
     * @param dbc the current database context
     * @param project the project to check
     *
     * @throws CmsRoleViolationException if the user does not have the required role permissions
     */
    public void checkManagerOfProjectRole(CmsDbContext dbc, CmsProject project) throws CmsRoleViolationException {

        if (!hasManagerOfProjectRole(dbc, project)) {
            throw new CmsRoleViolationException(org.opencms.security.Messages.get().container(
                org.opencms.security.Messages.ERR_NOT_MANAGER_OF_PROJECT_2,
                dbc.currentUser().getName(),
                dbc.currentProject().getName()));
        }
    }

    /**
     * Checks if the project in the given database context is not the "Online" project,
     * and throws an Exception if this is the case.<p>
     *  
     * This is used to ensure a user is in an "Offline" project
     * before write access to VFS resources is granted.<p>
     * 
     * @param dbc the current OpenCms users database context
     * 
     * @throws CmsVfsException if the project in the given database context is the "Online" project
     */
    public void checkOfflineProject(CmsDbContext dbc) throws CmsVfsException {

        if (dbc.currentProject().isOnlineProject()) {
            throw new CmsVfsException(org.opencms.file.Messages.get().container(
                org.opencms.file.Messages.ERR_NOT_ALLOWED_IN_ONLINE_PROJECT_0));
        }
    }

    /**
     * Performs a blocking permission check on a resource.<p>
     *
     * If the required permissions are not satisfied by the permissions the user has on the resource,
     * an exception is thrown.<p>
     * 
     * @param context the current request context
     * @param resource the resource on which permissions are required
     * @param requiredPermissions the set of permissions required to access the resource
     * @param checkLock if true, the lock status of the resource is also checked 
     * @param filter the filter for the resource
     * 
     * @throws CmsException in case of any i/o error
     * @throws CmsSecurityException if the required permissions are not satisfied
     * 
     * @see #checkPermissions(CmsRequestContext, CmsResource, CmsPermissionSet, int)
     */
    public void checkPermissions(
        CmsRequestContext context,
        CmsResource resource,
        CmsPermissionSet requiredPermissions,
        boolean checkLock,
        CmsResourceFilter filter) throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            // check the access permissions
            checkPermissions(dbc, resource, requiredPermissions, checkLock, filter);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Checks if the current user has the permissions to publish the given publish list 
     * (which contains the information about the resources / project to publish).<p>
     * 
     * @param dbc the current OpenCms users database context
     * @param publishList the publish list to check (contains the information about the resources / project to publish)
     * 
     * @throws CmsException if the user does not have the required permissions because of project lock state
     * @throws CmsMultiException if issues occur like a direct publish is attempted on a resource 
     *         whose parent folder is new or deleted in the offline project, 
     *         or if the current user has no management access to the current project
     */
    public void checkPublishPermissions(CmsDbContext dbc, CmsPublishList publishList)
    throws CmsException, CmsMultiException {

        // is the current project an "offline" project?
        checkOfflineProject(dbc);

        // check if the current project is unlocked
        if (dbc.currentProject().getFlags() != CmsProject.PROJECT_STATE_UNLOCKED) {
            CmsMessageContainer errMsg = org.opencms.security.Messages.get().container(
                org.opencms.security.Messages.ERR_RESOURCE_LOCKED_1,
                dbc.currentProject().getName());
            throw new CmsLockException(errMsg);
        }

        // check if this is a "direct publish" attempt        
        if (!publishList.isDirectPublish()) {
            // check if the user is a manager of the current project, in this case he has publish permissions
            checkManagerOfProjectRole(dbc, dbc.getRequestContext().currentProject());
        } else {
            // direct publish, create exception containers
            CmsMultiException resourceIssues = new CmsMultiException();
            CmsMultiException permissionIssues = new CmsMultiException();
            // iterate all resources in the direct publish list
            Iterator it = publishList.getDirectPublishResources().iterator();
            List parentFolders = new ArrayList();
            while (it.hasNext()) {
                CmsResource res = (CmsResource)it.next();
                // the parent folder must not be new or deleted
                String parentFolder = CmsResource.getParentFolder(res.getRootPath());
                if ((parentFolder != null) && !parentFolders.contains(parentFolder)) {
                    // check each parent folder only once
                    CmsResource parent = readResource(dbc, parentFolder, CmsResourceFilter.ALL);
                    if (parent.getState().isDeleted()) {
                        // parent folder is deleted - direct publish not allowed
                        resourceIssues.addException(new CmsVfsException(Messages.get().container(
                            Messages.ERR_DIRECT_PUBLISH_PARENT_DELETED_2,
                            dbc.getRequestContext().removeSiteRoot(res.getRootPath()),
                            parentFolder)));
                    }
                    if (parent.getState().isNew()) {
                        // parent folder is new - direct publish not allowed
                        resourceIssues.addException(new CmsVfsException(Messages.get().container(
                            Messages.ERR_DIRECT_PUBLISH_PARENT_NEW_2,
                            dbc.removeSiteRoot(res.getRootPath()),
                            parentFolder)));
                    }
                    // add checked parent folder to prevent duplicate checks
                    parentFolders.add(parentFolder);
                }
                // check if the user has the explicit permission to direct publish the selected resource
                if (PERM_ALLOWED != hasPermissions(
                    dbc.getRequestContext(),
                    res,
                    CmsPermissionSet.ACCESS_DIRECT_PUBLISH,
                    true,
                    CmsResourceFilter.ALL)) {

                    // the user has no "direct publish" permissions on the resource
                    permissionIssues.addException(new CmsSecurityException(Messages.get().container(
                        Messages.ERR_DIRECT_PUBLISH_NO_PERMISSIONS_1,
                        dbc.removeSiteRoot(res.getRootPath()))));
                }
            }

            if (permissionIssues.hasExceptions()) {
                // there have been permission issues
                if (hasManagerOfProjectRole(dbc, dbc.getRequestContext().currentProject())) {
                    // if user is a manager of the project, permission issues are void because he can publish anyway
                    permissionIssues = new CmsMultiException();
                }
            }
            if (resourceIssues.hasExceptions() || permissionIssues.hasExceptions()) {
                // there are issues, permission check has failed
                resourceIssues.addExceptions(permissionIssues.getExceptions());
                throw resourceIssues;
            }
        }
        // no issues have been found , permissions are granted
    }

    /**
     * Checks if the user of the given context has permissions to impersonate the given role.<p>
     * 
     * This method works only with role that are not organizational unit dependent.<p>
     *  
     * @param context the current request context
     * @param role the role to check
     * 
     * @throws CmsRoleViolationException if the user does not have the required role permissions
     * 
     * @see CmsRole#isOrganizationalUnitIndependent()
     */
    public void checkRole(CmsRequestContext context, CmsRole role) throws CmsRoleViolationException {

        if (!role.isOrganizationalUnitIndependent()) {
            throw new CmsRoleViolationException(Messages.get().container(
                Messages.ERR_ROLE_IS_ORGUNIT_DEPENDENT_1,
                role.getName(context.getLocale())));

        }
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkRoleForOrgUnit(dbc, role, null);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Checks if the user of the current database context has permissions to impersonate the given role
     * in the given organizational unit.<p>
     *  
     * If the organizational unit is <code>null</code>, this method will check if the
     * given user has the given role for at least one organizational unit.<p>
     *  
     * @param dbc the current OpenCms users database context
     * @param role the role to check
     * @param orgUnitFqn the organizational unit to check the role for, be may be <code>null</code>
     * 
     * @throws CmsRoleViolationException if the user does not have the required role permissions
     * 
     * @see org.opencms.security.CmsRoleManager#checkRoleForOrgUnit(CmsObject, CmsRole, String)
     */
    public void checkRoleForOrgUnit(CmsDbContext dbc, CmsRole role, String orgUnitFqn) throws CmsRoleViolationException {

        if (!hasRoleForOrgUnit(dbc, dbc.currentUser(), role, orgUnitFqn)) {
            if (orgUnitFqn != null) {
                throw role.createRoleViolationExceptionForOrgUnit(dbc.getRequestContext(), orgUnitFqn);
            } else {
                throw role.createRoleViolationException(dbc.getRequestContext());
            }
        }
    }

    /**
     * Checks if the user of the current context has permissions to impersonate the given role.<p>
     *  
     * If the organizational unit is <code>null</code>, this method will check if the
     * given user has the given role for at least one organizational unit.<p>
     *  
     * @param context the current request context
     * @param role the role to check
     * @param orgUnitFqn the organizational unit to check the role for
     * 
     * @throws CmsRoleViolationException if the user does not have the required role permissions
     */
    public void checkRoleForOrgUnit(CmsRequestContext context, CmsRole role, String orgUnitFqn)
    throws CmsRoleViolationException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkRoleForOrgUnit(dbc, role, orgUnitFqn);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Checks if the user of the current database context has permissions to impersonate the given role 
     * for the given resource.<p>
     *  
     * @param dbc the current OpenCms users database context
     * @param role the role to check
     * @param resource the resource to check the role for
     * 
     * @throws CmsRoleViolationException if the user does not have the required role permissions
     * 
     * @see org.opencms.security.CmsRoleManager#checkRoleForOrgUnit(CmsObject, CmsRole, String)
     */
    public void checkRoleForResource(CmsDbContext dbc, CmsRole role, CmsResource resource)
    throws CmsRoleViolationException {

        if (!hasRoleForResource(dbc, dbc.currentUser(), role, resource)) {
            throw role.createRoleViolationExceptionForResource(dbc.getRequestContext(), resource);
        }
    }

    /**
     * Checks if the user of the current context has permissions to impersonate the given role
     * for the given resource.<p>
     *  
     * @param context the current request context
     * @param role the role to check
     * @param resource the resource to check the role for
     * 
     * @throws CmsRoleViolationException if the user does not have the required role permissions
     */
    public void checkRoleForResource(CmsRequestContext context, CmsRole role, CmsResource resource)
    throws CmsRoleViolationException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkRoleForResource(dbc, role, resource);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Changes the resource flags of a resource.<p>
     * 
     * The resource flags are used to indicate various "special" conditions
     * for a resource. Most notably, the "internal only" setting which signals 
     * that a resource can not be directly requested with it's URL.<p>
     * 
     * @param context the current request context
     * @param resource the resource to change the flags for
     * @param flags the new resource flags for this resource
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if the user has insufficient permission for the given resource (({@link CmsPermissionSet#ACCESS_WRITE} required).
     * @see org.opencms.file.types.I_CmsResourceType#chflags(CmsObject, CmsSecurityManager, CmsResource, int)
     */
    public void chflags(CmsRequestContext context, CmsResource resource, int flags)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.ALL);
            m_driverManager.chflags(dbc, resource, flags);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_CHANGE_RESOURCE_FLAGS_1,
                context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Changes the resource type of a resource.<p>
     * 
     * OpenCms handles resources according to the resource type,
     * not the file suffix. This is e.g. why a JSP in OpenCms can have the 
     * suffix ".html" instead of ".jsp" only. Changing the resource type
     * makes sense e.g. if you want to make a plain text file a JSP resource,
     * or a binary file an image, etc.<p> 
     * 
     * @param context the current request context
     * @param resource the resource to change the type for
     * @param type the new resource type for this resource
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if the user has insufficient permission for the given resource (({@link CmsPermissionSet#ACCESS_WRITE} required)).
     * 
     * @see org.opencms.file.types.I_CmsResourceType#chtype(CmsObject, CmsSecurityManager, CmsResource, int)
     * @see CmsObject#chtype(String, int)
     */
    public void chtype(CmsRequestContext context, CmsResource resource, int type)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.ALL);
            m_driverManager.chtype(dbc, resource, type);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_CHANGE_RESOURCE_TYPE_1,
                context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Copies the access control entries of a given resource to a destination resorce.<p>
     *
     * Already existing access control entries of the destination resource are removed.<p>
     * 
     * @param context the current request context
     * @param source the resource to copy the access control entries from
     * @param destination the resource to which the access control entries are copied
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if the user has insufficient permission for the given resource ({@link CmsPermissionSet#ACCESS_CONTROL} required).
     */
    public void copyAccessControlEntries(CmsRequestContext context, CmsResource source, CmsResource destination)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, source, CmsPermissionSet.ACCESS_READ, true, CmsResourceFilter.ALL);
            checkPermissions(dbc, destination, CmsPermissionSet.ACCESS_CONTROL, true, CmsResourceFilter.ALL);
            m_driverManager.copyAccessControlEntries(dbc, source, destination, true);
        } catch (Exception e) {
            CmsRequestContext rc = context;
            dbc.report(null, Messages.get().container(
                Messages.ERR_COPY_ACE_2,
                rc.removeSiteRoot(source.getRootPath()),
                rc.removeSiteRoot(destination.getRootPath())), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Copies a resource.<p>
     * 
     * You must ensure that the destination path is an absolute, valid and
     * existing VFS path. Relative paths from the source are currently not supported.<p>
     * 
     * The copied resource will always be locked to the current user
     * after the copy operation.<p>
     * 
     * In case the target resource already exists, it is overwritten with the 
     * source resource.<p>
     * 
     * The <code>siblingMode</code> parameter controls how to handle siblings 
     * during the copy operation.<br>
     * Possible values for this parameter are: <br>
     * <ul>
     * <li><code>{@link org.opencms.file.CmsResource#COPY_AS_NEW}</code></li>
     * <li><code>{@link org.opencms.file.CmsResource#COPY_AS_SIBLING}</code></li>
     * <li><code>{@link org.opencms.file.CmsResource#COPY_PRESERVE_SIBLING}</code></li>
     * </ul><p>
     * 
     * @param context the current request context
     * @param source the resource to copy
     * @param destination the name of the copy destination with complete path
     * @param siblingMode indicates how to handle siblings during copy
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if resource could not be copied 
     * 
     * @see CmsObject#copyResource(String, String, CmsResourceCopyMode)
     * @see org.opencms.file.types.I_CmsResourceType#copyResource(CmsObject, CmsSecurityManager, CmsResource, String, CmsResourceCopyMode)
     */
    public void copyResource(
        CmsRequestContext context,
        CmsResource source,
        String destination,
        CmsResourceCopyMode siblingMode) throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, source, CmsPermissionSet.ACCESS_READ, true, CmsResourceFilter.ALL);
            if (source.isFolder() && destination.startsWith(source.getRootPath())) {
                throw new CmsVfsException(Messages.get().container(
                    Messages.ERR_RECURSIVE_INCLUSION_2,
                    dbc.removeSiteRoot(source.getRootPath()),
                    dbc.removeSiteRoot(destination)));
            }
            // target permissions will be checked later
            m_driverManager.copyResource(dbc, source, destination, siblingMode);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_COPY_RESOURCE_2,
                dbc.removeSiteRoot(source.getRootPath()),
                dbc.removeSiteRoot(destination)), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Copies a resource to the current project of the user.<p>
     * 
     * @param context the current request context
     * @param resource the resource to apply this operation to
     * @throws CmsException if something goes wrong
     * @throws CmsRoleViolationException if the current user does not have management access to the project.
     * @see org.opencms.file.types.I_CmsResourceType#copyResourceToProject(CmsObject, CmsSecurityManager, CmsResource)
     */
    public void copyResourceToProject(CmsRequestContext context, CmsResource resource)
    throws CmsException, CmsRoleViolationException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkManagerOfProjectRole(dbc, context.currentProject());
            if (dbc.currentProject().getFlags() != CmsProject.PROJECT_STATE_UNLOCKED) {
                throw new CmsLockException(org.opencms.lock.Messages.get().container(
                    org.opencms.lock.Messages.ERR_RESOURCE_LOCKED_1,
                    dbc.currentProject().getName()));
            }

            m_driverManager.copyResourceToProject(dbc, resource);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_COPY_RESOURCE_TO_PROJECT_2,
                context.getSitePath(resource),
                context.currentProject().getName()), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Counts the locked resources in this project.<p>
     *
     * @param context the current request context
     * @param id the id of the project
     * 
     * @return the amount of locked resources in this project
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsRoleViolationException if the current user does not have management access to the project.
     */
    public int countLockedResources(CmsRequestContext context, int id) throws CmsException, CmsRoleViolationException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsProject project = null;
        int result = 0;
        try {
            project = m_driverManager.readProject(dbc, id);
            checkManagerOfProjectRole(dbc, project);
            result = m_driverManager.countLockedResources(project);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_COUNT_LOCKED_RESOURCES_PROJECT_2,
                (project == null) ? "<failed to read>" : project.getName(),
                new Integer(id)), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Creates a new user group.<p>
     *
     * @param context the current request context
     * @param name the name of the new group
     * @param description the description for the new group
     * @param flags the flags for the new group
     * @param parent the name of the parent group (or <code>null</code>)
     * 
     * @return a <code>{@link CmsGroup}</code> object representing the newly created group
     * 
     * @throws CmsException if operation was not successful.
     * @throws CmsRoleViolationException if the  role {@link CmsRole#ACCOUNT_MANAGER} is not owned by the current user.
     * 
     */
    public CmsGroup createGroup(CmsRequestContext context, String name, String description, int flags, String parent)
    throws CmsException, CmsRoleViolationException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);

        CmsGroup result = null;
        try {
            checkRoleForOrgUnit(dbc, CmsRole.ACCOUNT_MANAGER, getParentOrganizationalUnit(name));
            result = m_driverManager.createGroup(dbc, new CmsUUID(), name, description, flags, parent);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_CREATE_GROUP_1, name), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Creates a new organizational unit.<p>
     * 
     * @param context the current request context
     * @param ouFqn the fully qualified name of the new organizational unit
     * @param description the description of the new organizational unit
     * @param flags the flags for the new organizational unit
     * @param resource the first associated resource
     *
     * @return a <code>{@link CmsOrganizationalUnit}</code> object representing 
     *          the newly created organizational unit
     *
     * @throws CmsException if operation was not successful
     * 
     * @see org.opencms.security.CmsOrgUnitManager#createOrganizationalUnit(CmsObject, String, String, int, String)
     */
    public CmsOrganizationalUnit createOrganizationalUnit(
        CmsRequestContext context,
        String ouFqn,
        String description,
        int flags,
        CmsResource resource) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsOrganizationalUnit result = null;
        try {
            checkRoleForOrgUnit(dbc, CmsRole.ADMINISTRATOR, getParentOrganizationalUnit(ouFqn));
            checkOfflineProject(dbc);
            result = m_driverManager.createOrganizationalUnit(dbc, ouFqn, description, flags, resource);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_CREATE_ORGUNIT_1, ouFqn), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Creates a project.<p>
     *
     * @param context the current request context
     * @param name the name of the project to create
     * @param description the description of the project
     * @param groupname the project user group to be set
     * @param managergroupname the project manager group to be set
     * @param projecttype the type of the project
     * 
     * @return the created project
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsRoleViolationException if the current user does not own the role {@link CmsRole#PROJECT_MANAGER}.
     */
    public CmsProject createProject(
        CmsRequestContext context,
        String name,
        String description,
        String groupname,
        String managergroupname,
        int projecttype) throws CmsException, CmsRoleViolationException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsProject result = null;
        try {
            checkRoleForOrgUnit(dbc, CmsRole.PROJECT_MANAGER, null);
            result = m_driverManager.createProject(dbc, name, description, groupname, managergroupname, projecttype);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_CREATE_PROJECT_1, name), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Creates a property definition.<p>
     *
     * Property definitions are valid for all resource types.<p>
     * 
     * @param context the current request context
     * @param name the name of the property definition to create
     * 
     * @return the created property definition
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if the current project is online.
     * @throws CmsRoleViolationException if the current user does not own the role {@link CmsRole#WORKPLACE_MANAGER}.
     */
    public CmsPropertyDefinition createPropertyDefinition(CmsRequestContext context, String name)
    throws CmsException, CmsSecurityException, CmsRoleViolationException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsPropertyDefinition result = null;

        try {
            checkOfflineProject(dbc);
            checkRoleForOrgUnit(dbc, CmsRole.WORKPLACE_MANAGER, null);
            result = m_driverManager.createPropertyDefinition(dbc, name);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_CREATE_PROPDEF_1, name), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Creates a new resource of the given resource type with the provided content and properties.<p>
     * 
     * If the provided content is null and the resource is not a folder, the content will be set to an empty byte array.<p>  
     * 
     * @param context the current request context
     * @param resourcename the name of the resource to create (full path)
     * @param type the type of the resource to create
     * @param content the content for the new resource
     * @param properties the properties for the new resource
     * @return the created resource
     * @throws CmsException if something goes wrong
     * 
     * @see org.opencms.file.types.I_CmsResourceType#createResource(CmsObject, CmsSecurityManager, String, byte[], List)
     */
    public CmsResource createResource(
        CmsRequestContext context,
        String resourcename,
        int type,
        byte[] content,
        List properties) throws CmsException {

        if (existsResource(context, resourcename, CmsResourceFilter.IGNORE_EXPIRATION)) {
            // check if the resource already exists by name
            throw new CmsVfsResourceAlreadyExistsException(org.opencms.db.generic.Messages.get().container(
                org.opencms.db.generic.Messages.ERR_RESOURCE_WITH_NAME_ALREADY_EXISTS_1,
                resourcename));
        }
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsResource newResource = null;
        try {
            checkOfflineProject(dbc);
            newResource = m_driverManager.createResource(dbc, resourcename, type, content, properties);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_CREATE_RESOURCE_1, resourcename), e);
        } finally {
            dbc.clear();
        }
        return newResource;
    }

    /**
     * Creates a new sibling of the source resource.<p>
     * 
     * @param context the current request context
     * @param source the resource to create a sibling for
     * @param destination the name of the sibling to create with complete path
     * @param properties the individual properties for the new sibling
     * 
     * @return the new created sibling
     * 
     * @throws CmsException if something goes wrong
     * 
     * @see org.opencms.file.types.I_CmsResourceType#createSibling(CmsObject, CmsSecurityManager, CmsResource, String, List)
     */
    public CmsResource createSibling(CmsRequestContext context, CmsResource source, String destination, List properties)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);

        CmsResource sibling = null;
        try {
            checkOfflineProject(dbc);
            sibling = m_driverManager.createSibling(dbc, source, destination, properties);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_CREATE_SIBLING_1,
                context.removeSiteRoot(source.getRootPath())), e);
        } finally {
            dbc.clear();
        }
        return sibling;
    }

    /**
     * Creates the project for the temporary workplace files.<p>
     *
     * @param context the current request context
     * 
     * @return the created project for the temporary workplace files
     * 
     * @throws CmsException if something goes wrong
     */
    public CmsProject createTempfileProject(CmsRequestContext context) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);

        CmsProject result = null;
        try {
            checkRoleForOrgUnit(dbc, CmsRole.PROJECT_MANAGER, null);
            result = m_driverManager.createTempfileProject(dbc);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_CREATE_TEMPFILE_PROJECT_0), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Creates a new user.<p>
     *
     * @param context the current request context
     * @param name the name for the new user
     * @param password the password for the new user
     * @param description the description for the new user
     * @param additionalInfos the additional infos for the user
     *
     * @return the created user
     * 
     * @see CmsObject#createUser(String, String, String, Map)
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsRoleViolationException if the current user does not own the rule {@link CmsRole#ACCOUNT_MANAGER}
     */
    public CmsUser createUser(
        CmsRequestContext context,
        String name,
        String password,
        String description,
        Map additionalInfos) throws CmsException, CmsRoleViolationException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);

        CmsUser result = null;
        try {
            checkRoleForOrgUnit(dbc, CmsRole.ACCOUNT_MANAGER, getParentOrganizationalUnit(name));
            result = m_driverManager.createUser(dbc, name, password, description, additionalInfos);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_CREATE_USER_1, name), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Deletes all entries in the published resource table.<p>
     * 
     * @param context the current request context
     * @param linkType the type of resource deleted (0= non-paramter, 1=parameter)
     * 
     * @throws CmsException if something goes wrong
     */
    public void deleteAllStaticExportPublishedResources(CmsRequestContext context, int linkType) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            m_driverManager.deleteAllStaticExportPublishedResources(dbc, linkType);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_DELETE_STATEXP_PUBLISHED_RESOURCES_0), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Deletes the versions from the backup tables that are older then the given timestamp  
     * and/or number of remaining versions.<p>
     * 
     * The number of verions always wins, i.e. if the given timestamp would delete more versions 
     * than given in the versions parameter, the timestamp will be ignored. <p>
     * 
     * Deletion will delete file header, content and properties. <p>
     * 
     * @param context the current request context
     * @param timestamp timestamp which defines the date after which backup resources must be deleted
     * @param versions the number of versions per file which should kept in the system
     * @param report the report for output logging
     * 
     * @throws CmsException if operation was not succesful
     * @throws CmsRoleViolationException if the current user does not own the rule {@link CmsRole#WORKPLACE_MANAGER}
     */
    public void deleteBackups(CmsRequestContext context, long timestamp, int versions, I_CmsReport report)
    throws CmsException, CmsRoleViolationException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkRoleForOrgUnit(dbc, CmsRole.WORKPLACE_MANAGER, null);
            m_driverManager.deleteBackups(dbc, timestamp, versions, report);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_DELETE_BACKUPS_2,
                new Date(timestamp),
                new Integer(versions)), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Deletes a group, where all permissions, users and childs of the group
     * are transfered to a replacement group.<p>
     * 
     * @param context the current request context
     * @param groupId the id of the group to be deleted
     * @param replacementId the id of the group to be transfered, can be <code>null</code>
     *
     * @throws CmsException if operation was not succesful
     * @throws CmsSecurityException if the group is a default group.
     * @throws CmsRoleViolationException if the current user does not own the rule {@link CmsRole#ACCOUNT_MANAGER}
     */
    public void deleteGroup(CmsRequestContext context, CmsUUID groupId, CmsUUID replacementId)
    throws CmsException, CmsRoleViolationException, CmsSecurityException {

        CmsGroup group = readGroup(context, groupId);
        if (group.isRole()) {
            throw new CmsSecurityException(Messages.get().container(Messages.ERR_DELETE_ROLE_GROUP_1, group.getName()));
        }
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            // catch own exception as special cause for general "Error deleting group". 
            checkRoleForOrgUnit(dbc, CmsRole.ACCOUNT_MANAGER, getParentOrganizationalUnit(group.getName()));
            // this is needed because 
            // I_CmsUserDriver#removeAccessControlEntriesForPrincipal(CmsDbContext, CmsProject, CmsProject, CmsUUID)
            // expects an offline project, if not, data will become inconsistent
            checkOfflineProject(dbc);
            m_driverManager.deleteGroup(dbc, group, replacementId);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_DELETE_GROUP_1, group.getName()), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Delete a user group.<p>
     *
     * Only groups that contain no subgroups can be deleted.<p> 
     * 
     * @param context the current request context
     * @param name the name of the group that is to be deleted
     *
     * @throws CmsException if operation was not succesful
     * @throws CmsSecurityException if the group is a default group.
     * @throws CmsRoleViolationException if the current user does not own the rule {@link CmsRole#ACCOUNT_MANAGER}
     * 
     */
    public void deleteGroup(CmsRequestContext context, String name)
    throws CmsException, CmsRoleViolationException, CmsSecurityException {

        CmsGroup group = readGroup(context, name);
        if (group.isRole()) {
            throw new CmsSecurityException(Messages.get().container(Messages.ERR_DELETE_ROLE_GROUP_1, name));
        }
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            // catch own exception as special cause for general "Error deleting group". 
            checkRoleForOrgUnit(dbc, CmsRole.ACCOUNT_MANAGER, getParentOrganizationalUnit(name));
            // this is needed because 
            // I_CmsUserDriver#removeAccessControlEntriesForPrincipal(CmsDbContext, CmsProject, CmsProject, CmsUUID)
            // expects an offline project, if not data will become inconsistent
            checkOfflineProject(dbc);
            m_driverManager.deleteGroup(dbc, group, null);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_DELETE_GROUP_1, name), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Deletes an organizational unit.<p>
     *
     * Only organizational units that contain no suborganizational unit can be deleted.<p>
     * 
     * The organizational unit can not be delete if it is used in the reuqest context, 
     * or if the current user belongs to it.<p>
     * 
     * All users and groups in the given organizational unit will be deleted.<p>
     * 
     * @param context the current request context
     * @param organizationalUnit the organizational unit to delete
     * 
     * @throws CmsException if operation was not successful
     * 
     * @see org.opencms.security.CmsOrgUnitManager#deleteOrganizationalUnit(CmsObject, String)
     */
    public void deleteOrganizationalUnit(CmsRequestContext context, CmsOrganizationalUnit organizationalUnit)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkRoleForOrgUnit(dbc, CmsRole.ADMINISTRATOR, getParentOrganizationalUnit(organizationalUnit.getName()));
            checkOfflineProject(dbc);
            m_driverManager.deleteOrganizationalUnit(dbc, organizationalUnit);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_DELETE_ORGUNIT_1, organizationalUnit.getName()), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Deletes a project.<p>
     *
     * All modified resources currently inside this project will be reset to their online state.<p>
     * 
     * @param context the current request context
     * @param projectId the ID of the project to be deleted
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsRoleViolationException if the current user does not own management access to the project
     */
    public void deleteProject(CmsRequestContext context, int projectId) throws CmsException, CmsRoleViolationException {

        if (projectId == CmsProject.ONLINE_PROJECT_ID) {
            // online project must not be deleted
            throw new CmsVfsException(org.opencms.file.Messages.get().container(
                org.opencms.file.Messages.ERR_NOT_ALLOWED_IN_ONLINE_PROJECT_0));
        }

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsProject deleteProject = null;
        try {
            // read the project that should be deleted
            deleteProject = m_driverManager.readProject(dbc, projectId);
            checkManagerOfProjectRole(dbc, deleteProject);
            m_driverManager.deleteProject(dbc, deleteProject);
        } catch (Exception e) {
            String projectName = deleteProject == null ? String.valueOf(projectId) : deleteProject.getName();
            dbc.report(null, Messages.get().container(Messages.ERR_DELETE_PROJECT_1, projectName), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Deletes a property definition.<p>
     * 
     * @param context the current request context
     * @param name the name of the property definition to delete
     *
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if the project to delete is the "Online" project
     * @throws CmsRoleViolationException if the current user does not own the role {@link CmsRole#WORKPLACE_MANAGER}
     */
    public void deletePropertyDefinition(CmsRequestContext context, String name)
    throws CmsException, CmsSecurityException, CmsRoleViolationException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkRoleForOrgUnit(dbc, CmsRole.WORKPLACE_MANAGER, null);
            m_driverManager.deletePropertyDefinition(dbc, name);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_DELETE_PROPERTY_1, name), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Deletes all relations for the given resource matching the given filter.<p>
     * 
     * @param context the current user context
     * @param resource the resource to delete the relations for
     * @param filter the filter to use for deletion
     * 
     * @throws CmsException if something goes wrong
     */
    public void deleteRelationsForResource(CmsRequestContext context, CmsResource resource, CmsRelationFilter filter)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            m_driverManager.deleteRelationsForResource(dbc, resource, filter);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_DELETE_RELATIONS_1,
                dbc.removeSiteRoot(resource.getRootPath())), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Deletes a resource given its name.<p>
     * 
     * The <code>siblingMode</code> parameter controls how to handle siblings 
     * during the delete operation.<br>
     * Possible values for this parameter are: <br>
     * <ul>
     * <li><code>{@link CmsResource#DELETE_REMOVE_SIBLINGS}</code></li>
     * <li><code>{@link CmsResource#DELETE_PRESERVE_SIBLINGS}</code></li>
     * </ul><p>
     * 
     * @param context the current request context
     * @param resource the name of the resource to delete (full path)
     * @param siblingMode indicates how to handle siblings of the deleted resource
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if the user does not have {@link CmsPermissionSet#ACCESS_WRITE} on the given resource. 
     * @see org.opencms.file.types.I_CmsResourceType#deleteResource(CmsObject, CmsSecurityManager, CmsResource, CmsResourceDeleteMode)
     */
    public void deleteResource(CmsRequestContext context, CmsResource resource, CmsResourceDeleteMode siblingMode)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.ALL);
            checkSystemLocks(dbc, resource);
            deleteResource(dbc, resource, siblingMode);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_DELETE_RESOURCE_1, context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Deletes an entry in the published resource table.<p>
     * 
     * @param context the current request context
     * @param resourceName The name of the resource to be deleted in the static export
     * @param linkType the type of resource deleted (0= non-paramter, 1=parameter)
     * @param linkParameter the parameters ofthe resource
     * 
     * @throws CmsException if something goes wrong
     */
    public void deleteStaticExportPublishedResource(
        CmsRequestContext context,
        String resourceName,
        int linkType,
        String linkParameter) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            m_driverManager.deleteStaticExportPublishedResource(dbc, resourceName, linkType, linkParameter);
        } catch (Exception e) {
            dbc.report(
                null,
                Messages.get().container(Messages.ERR_DELETE_STATEXP_PUBLISHES_RESOURCE_1, resourceName),
                e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Deletes a user.<p>
     *
     * @param context the current request context
     * @param userId the Id of the user to be deleted
     * 
     * @throws CmsException if something goes wrong
     */
    public void deleteUser(CmsRequestContext context, CmsUUID userId) throws CmsException {

        CmsUser user = readUser(context, userId);
        deleteUser(context, user, null);
    }

    /**
     * Deletes a user, where all permissions and resources attributes of the user
     * were transfered to a replacement user.<p>
     *
     * @param context the current request context
     * @param userId the id of the user to be deleted
     * @param replacementId the id of the user to be transfered
     *
     * @throws CmsException if operation was not successful
     */
    public void deleteUser(CmsRequestContext context, CmsUUID userId, CmsUUID replacementId) throws CmsException {

        CmsUser user = readUser(context, userId);
        CmsUser replacementUser = null;
        if ((replacementId != null) && !replacementId.isNullUUID()) {
            replacementUser = readUser(context, replacementId);
        }
        deleteUser(context, user, replacementUser);
    }

    /**
     * Deletes a user.<p>
     *
     * @param context the current request context
     * @param username the name of the user to be deleted
     * 
     * @throws CmsException if something goes wrong
     */
    public void deleteUser(CmsRequestContext context, String username) throws CmsException {

        CmsUser user = readUser(context, username);
        deleteUser(context, user, null);
    }

    /**
     * Destroys this security manager.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public synchronized void destroy() throws Throwable {

        try {
            if (m_driverManager != null) {
                try {
                    writeLocks();
                } catch (Throwable t) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error(org.opencms.lock.Messages.get().getBundle().key(
                            org.opencms.lock.Messages.ERR_WRITE_LOCKS_FINAL_0), t);
                    }
                }
                m_driverManager.destroy();
            }
        } catch (Throwable t) {
            if (LOG.isErrorEnabled()) {
                LOG.error(Messages.get().getBundle().key(Messages.LOG_ERR_DRIVER_MANAGER_CLOSE_0), t);
            }
        }

        m_driverManager = null;
        m_dbContextFactory = null;

        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(
                Messages.INIT_SECURITY_MANAGER_SHUTDOWN_1,
                this.getClass().getName()));
        }
    }

    /**
     * Checks the availability of a resource in the VFS,
     * using the <code>{@link CmsResourceFilter#DEFAULT}</code> filter.<p> 
     *
     * A resource may be of type <code>{@link CmsFile}</code> or 
     * <code>{@link CmsFolder}</code>.<p>  
     *
     * The specified filter controls what kind of resources should be "found" 
     * during the read operation. This will depend on the application. For example, 
     * using <code>{@link CmsResourceFilter#DEFAULT}</code> will only return currently
     * "valid" resources, while using <code>{@link CmsResourceFilter#IGNORE_EXPIRATION}</code>
     * will ignore the date release / date expired information of the resource.<p>
     * 
     * This method also takes into account the user permissions, so if 
     * the given resource exists, but the current user has not the required 
     * permissions, then this method will return <code>false</code>.<p>
     *
     * @param context the current request context
     * @param resourcePath the name of the resource to read (full path)
     * @param filter the resource filter to use while reading
     *
     * @return <code>true</code> if the resource is available
     * 
     * @see CmsObject#existsResource(String, CmsResourceFilter)
     * @see CmsObject#existsResource(String)
     */
    public boolean existsResource(CmsRequestContext context, String resourcePath, CmsResourceFilter filter) {

        boolean result = false;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            readResource(dbc, resourcePath, filter);
            result = true;
        } catch (Exception e) {
            result = false;
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Fills the given publish list with the the VFS resources that actually get published.<p>
     * 
     * Please refer to the source code of this method for the rules on how to decide whether a
     * new/changed/deleted <code>{@link CmsResource}</code> object can be published or not.<p>
     * 
     * @param context the current request context
     * @param publishList must be initialized with basic publish information (Project or direct publish operation)
     * 
     * @return the given publish list filled with all new/changed/deleted files from the current (offline) project 
     *      that will be published actually
     * 
     * @throws CmsException if something goes wrong
     * 
     * @see org.opencms.db.CmsPublishList
     */
    public CmsPublishList fillPublishList(CmsRequestContext context, CmsPublishList publishList) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            m_driverManager.fillPublishList(dbc, publishList);
            checkPublishPermissions(dbc, publishList);
        } catch (Exception e) {
            if (publishList.isDirectPublish()) {
                dbc.report(null, Messages.get().container(
                    Messages.ERR_GET_PUBLISH_LIST_DIRECT_1,
                    CmsFileUtil.formatResourceNames(context, publishList.getDirectPublishResources())), e);
            } else {
                dbc.report(null, Messages.get().container(
                    Messages.ERR_GET_PUBLISH_LIST_PROJECT_1,
                    context.currentProject().getName()), e);
            }
        } finally {
            dbc.clear();
        }
        return publishList;
    }

    /**
     * Returns the list of access control entries of a resource given its name.<p>
     * 
     * @param context the current request context
     * @param resource the resource to read the access control entries for
     * @param getInherited true if the result should include all access control entries inherited by parent folders
     * 
     * @return a list of <code>{@link CmsAccessControlEntry}</code> objects defining all permissions for the given resource
     * 
     * @throws CmsException if something goes wrong
     */
    public List getAccessControlEntries(CmsRequestContext context, CmsResource resource, boolean getInherited)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            result = m_driverManager.getAccessControlEntries(dbc, resource, getInherited);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_GET_ACL_ENTRIES_1, context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns the access control list (summarized access control entries) of a given resource.<p>
     * 
     * If <code>inheritedOnly</code> is set, only inherited access control entries are returned.<p>
     * 
     * @param context the current request context
     * @param resource the resource
     * @param inheritedOnly skip non-inherited entries if set
     * 
     * @return the access control list of the resource
     * 
     * @throws CmsException if something goes wrong
     */
    public CmsAccessControlList getAccessControlList(
        CmsRequestContext context,
        CmsResource resource,
        boolean inheritedOnly) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsAccessControlList result = null;
        try {
            result = m_driverManager.getAccessControlList(dbc, resource, inheritedOnly);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_GET_ACL_ENTRIES_1, context.getSitePath(resource)), e);

        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns all projects which are owned by the current user or which are 
     * accessible for the group of the user.<p>
     *
     * @param context the current request context
     * 
     * @return a list of objects of type <code>{@link CmsProject}</code>
     * 
     * @throws CmsException if something goes wrong
     */
    public List getAllAccessibleProjects(CmsRequestContext context) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            result = m_driverManager.getAllAccessibleProjects(dbc);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_GET_ALL_ACCESSIBLE_PROJECTS_1,
                dbc.currentUser().getName()), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns a list with all projects from history.<p>
     *
     * @param context the current request context
     * 
     * @return list of <code>{@link CmsBackupProject}</code> objects 
     *           with all projects from history.
     * 
     * @throws CmsException if operation was not succesful
     */
    public List getAllBackupProjects(CmsRequestContext context) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            result = m_driverManager.getAllBackupProjects(dbc);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_GET_ALL_ACCESSIBLE_PROJECTS_1,
                dbc.currentUser().getName()), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns all projects which are owned by the current user or which are manageable
     * for the group of the user.<p>
     *
     * @param context the current request context
     * 
     * @return a list of objects of type <code>{@link CmsProject}</code>
     * 
     * @throws CmsException if operation was not succesful
     */
    public List getAllManageableProjects(CmsRequestContext context) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            result = m_driverManager.getAllManageableProjects(dbc);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_GET_ALL_MANAGEABLE_PROJECTS_1,
                dbc.currentUser().getName()), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns the next version id for the published backup resources.<p>
     *
     * @param context the current request context
     * 
     * @return the new version id
     */
    public int getBackupTagId(CmsRequestContext context) {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        int result = 0;
        try {
            result = m_driverManager.getBackupTagId(dbc);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns all child groups of a group.<p>
     *
     * @param context the current request context
     * @param groupname the name of the group
     *
     * @return a list of all child <code>{@link CmsGroup}</code> objects or <code>null</code>
     * 
     * @throws CmsException if operation was not succesful
     */
    public List getChild(CmsRequestContext context, String groupname) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            result = m_driverManager.getChild(dbc, m_driverManager.readGroup(dbc, groupname));
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_GET_CHILD_GROUPS_1, groupname), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns all child groups of a group.<p>
     * 
     * This method also returns all sub-child groups of the current group.<p>
     *
     * @param context the current request context
     * @param groupname the name of the group
     *
     * @return a list of all child <code>{@link CmsGroup}</code> objects or <code>null</code>
     * 
     * @throws CmsException if operation was not succesful
     */
    public List getChilds(CmsRequestContext context, String groupname) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            result = m_driverManager.getChilds(dbc, m_driverManager.readGroup(dbc, groupname));
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_GET_CHILD_GROUPS_TRANSITIVE_1, groupname), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns all groups of the given organizational unit.<p>
     *
     * @param context the current request context
     * @param orgUnit the organizational unit to get the groups for
     * @param includeSubOus if all groups of sub-organizational units should be retrieved too
     * @param readRoles if to read roles or groups
     * 
     * @return all <code>{@link CmsGroup}</code> objects in the organizational unit
     *
     * @throws CmsException if operation was not successful
     * 
     * @see org.opencms.security.CmsOrgUnitManager#getResourcesForOrganizationalUnit(CmsObject, String)
     * @see org.opencms.security.CmsOrgUnitManager#getGroups(CmsObject, String, boolean)
     * @see org.opencms.security.CmsOrgUnitManager#getUsers(CmsObject, String, boolean)
     */
    public List getGroups(
        CmsRequestContext context,
        CmsOrganizationalUnit orgUnit,
        boolean includeSubOus,
        boolean readRoles) throws CmsException {

        List result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = m_driverManager.getGroups(dbc, orgUnit, includeSubOus, readRoles);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_ORGUNIT_GROUPS_1, orgUnit.getName()), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns the list of groups to which the user directly belongs to.<p>
     *
     * @param context the current request context
     * @param username The name of the user
     * @param ouFqn the fully qualified name of the organizational unit to restrict the result set for
     * @param includeChildOus include groups of child organizational units
     * @param readRoles if to read roles or groups
     * @param directGroupsOnly if set only the direct assigned groups will be returned, if not also indirect roles
     * @param remoteAddress the IP address to filter the groups in the result list 
     *
     * @return a list of <code>{@link CmsGroup}</code> objects filtered by the given IP address
     * 
     * @throws CmsException if operation was not succesful
     */
    public List getGroupsOfUser(
        CmsRequestContext context,
        String username,
        String ouFqn,
        boolean includeChildOus,
        boolean readRoles,
        boolean directGroupsOnly,
        String remoteAddress) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            result = m_driverManager.getGroupsOfUser(
                dbc,
                username,
                ouFqn,
                includeChildOus,
                readRoles,
                directGroupsOnly,
                remoteAddress);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_GET_GROUPS_OF_USER_2, username, remoteAddress), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns the lock state of a resource.<p>
     * 
     * @param context the current request context
     * @param resource the resource to return the lock state for
     * @return the lock state of the resource
     * @throws CmsException if something goes wrong
     */
    public CmsLock getLock(CmsRequestContext context, CmsResource resource) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsLock result = null;
        try {
            result = m_driverManager.getLock(dbc, resource);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_GET_LOCK_1, context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns all locked resources in a given folder.<p>
     *
     * @param context the current request context
     * @param resource the folder to search in
     * @param filter the lock filter
     * 
     * @return a list of locked resource paths (relative to current site)
     * 
     * @throws CmsException if something goes wrong
     */
    public List getLockedResources(CmsRequestContext context, CmsResource resource, CmsLockFilter filter)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_READ, false, CmsResourceFilter.ALL);
            result = m_driverManager.getLockedResources(dbc, resource, filter);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_COUNT_LOCKED_RESOURCES_FOLDER_1,
                context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns the lock manger.<p> 
     * 
     * @return the lock manager
     */
    public CmsLockManager getLockManager() {

        return m_lockManager;
    }

    /**
     * Returns all child organizational units of the given parent organizational unit including 
     * hierarchical deeper organization units if needed.<p>
     *
     * @param context the current request context
     * @param parent the parent organizational unit
     * @param includeChilds if hierarchical deeper organization units should also be returned
     * 
     * @return a list of <code>{@link CmsOrganizationalUnit}</code> objects
     * 
     * @throws CmsException if operation was not succesful
     * 
     * @see org.opencms.security.CmsOrgUnitManager#getOrganizationalUnits(CmsObject, String, boolean)
     */
    public List getOrganizationalUnits(CmsRequestContext context, CmsOrganizationalUnit parent, boolean includeChilds)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            result = m_driverManager.getOrganizationalUnits(dbc, parent, includeChilds);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_GET_ORGUNITS_1, parent.getName()), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns the parent group of a group.<p>
     *
     * @param context the current request context
     * @param groupname the name of the group
     * 
     * @return group the parent group or <code>null</code>
     * 
     * @throws CmsException if operation was not succesful
     */
    public CmsGroup getParent(CmsRequestContext context, String groupname) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsGroup result = null;
        try {
            result = m_driverManager.getParent(dbc, groupname);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_GET_PARENT_GROUP_1, groupname), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns the set of permissions of the current user for a given resource.<p>
     * 
     * @param context the current request context
     * @param resource the resource
     * @param user the user
     * 
     * @return bitset with allowed permissions
     * 
     * @throws CmsException if something goes wrong
     */
    public CmsPermissionSetCustom getPermissions(CmsRequestContext context, CmsResource resource, CmsUser user)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsPermissionSetCustom result = null;
        try {
            result = m_driverManager.getPermissions(dbc, resource, user);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_GET_PERMISSIONS_2,
                user.getName(),
                context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns all relations for the given resource mathing the given filter.<p> 
     * 
     * @param context the current user context
     * @param resource the resource to retrieve the relations for
     * @param filter the filter to match the relation 
     * 
     * @return all {@link org.opencms.relations.CmsRelation} objects for the given resource mathing the given filter
     * 
     * @throws CmsException if something goes wrong
     * 
     * @see CmsObject#getRelationsForResource(String, CmsRelationFilter)
     */
    public List getRelationsForResource(CmsRequestContext context, CmsResource resource, CmsRelationFilter filter)
    throws CmsException {

        List result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            // check the access permissions
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_VIEW, false, CmsResourceFilter.ALL);
            result = m_driverManager.getRelationsForResource(dbc, resource, filter);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_READ_RELATIONS_1,
                context.removeSiteRoot(resource.getRootPath())), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns all resources of the given organizational unit.<p>
     *
     * @param context the current request context
     * @param orgUnit the organizational unit to get all resources for
     * 
     * @return all <code>{@link CmsResource}</code> objects in the organizational unit
     *
     * @throws CmsException if operation was not successful
     * 
     * @see org.opencms.security.CmsOrgUnitManager#getResourcesForOrganizationalUnit(CmsObject, String)
     * @see org.opencms.security.CmsOrgUnitManager#getGroups(CmsObject, String, boolean)
     * @see org.opencms.security.CmsOrgUnitManager#getUsers(CmsObject, String, boolean)
     */
    public List getResourcesForOrganizationalUnit(CmsRequestContext context, CmsOrganizationalUnit orgUnit)
    throws CmsException {

        List result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = m_driverManager.getResourcesForOrganizationalUnit(dbc, orgUnit);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_ORGUNIT_RESOURCES_1, orgUnit.getName()), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns all resources associated to a given principal via an ACE with the given permissions.<p> 
     * 
     * If the <code>includeAttr</code> flag is set it returns also all resources associated to 
     * a given principal through some of following attributes.<p> 
     * 
     * <ul>
     *    <li>User Created</li>
     *    <li>User Last Modified</li>
     * </ul><p>
     * 
     * @param context the current request context
     * @param principalId the id of the principal
     * @param permissions a set of permissions to match, can be <code>null</code> for all ACEs
     * @param includeAttr a flag to include resources associated by attributes
     * 
     * @return a list of <code>{@link CmsResource}</code> objects
     * 
     * @throws CmsException if something goes wrong
     */
    public List getResourcesForPrincipal(
        CmsRequestContext context,
        CmsUUID principalId,
        CmsPermissionSet permissions,
        boolean includeAttr) throws CmsException {

        List dependencies;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            dependencies = m_driverManager.getResourcesForPrincipal(
                dbc,
                dbc.currentProject(),
                principalId,
                permissions,
                includeAttr);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_RESOURCES_FOR_PRINCIPAL_LOG_1, principalId), e);
            dependencies = new ArrayList();
        } finally {
            dbc.clear();
        }
        return dependencies;
    }

    /**
     * Returns an instance of the common sql manager.<p>
     * 
     * @return an instance of the common sql manager
     */
    public CmsSqlManager getSqlManager() {

        return m_driverManager.getSqlManager();
    }

    /**
     * Returns all users of the given organizational unit.<p>
     *
     * @param context the current request context
     * @param orgUnit the organizational unit to get the users for
     * @param recursive if all users of sub-organizational units should be retrieved too
     * 
     * @return all <code>{@link CmsUser}</code> objects in the organizational unit
     *
     * @throws CmsException if operation was not successful
     * 
     * @see org.opencms.security.CmsOrgUnitManager#getResourcesForOrganizationalUnit(CmsObject, String)
     * @see org.opencms.security.CmsOrgUnitManager#getGroups(CmsObject, String, boolean)
     * @see org.opencms.security.CmsOrgUnitManager#getUsers(CmsObject, String, boolean)
     */
    public List getUsers(CmsRequestContext context, CmsOrganizationalUnit orgUnit, boolean recursive)
    throws CmsException {

        List result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = m_driverManager.getUsers(dbc, orgUnit, recursive);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_ORGUNIT_USERS_1, orgUnit.getName()), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns a list of users in a group.<p>
     *
     * @param context the current request context
     * @param groupname the name of the group to list users from
     * @param includeOtherOuUsers include users of other organizational units
     * @param directUsersOnly if set only the direct assigned users will be returned, 
     *                          if not also indirect users, ie. members of child groups
     * @param readRoles if to read roles or groups
     *
     * @return all <code>{@link CmsUser}</code> objects in the group
     * 
     * @throws CmsException if operation was not succesful
     */
    public List getUsersOfGroup(
        CmsRequestContext context,
        String groupname,
        boolean includeOtherOuUsers,
        boolean directUsersOnly,
        boolean readRoles) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            result = m_driverManager.getUsersOfGroup(dbc, groupname, includeOtherOuUsers, directUsersOnly, readRoles);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_GET_USERS_OF_GROUP_1, groupname), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Checks if the current user has management access to the given project.<p>
     * 
     * @param dbc the current database context
     * @param project the project to check
     *
     * @return <code>true</code>, if the user has management access to the project
     */
    public boolean hasManagerOfProjectRole(CmsDbContext dbc, CmsProject project) {

        if (project.isOnlineProject()) {
            // no user is the project manager of the "Online" project
            return false;
        }

        if (dbc.currentUser().getId().equals(project.getOwnerId())) {
            // user is the owner of the current project
            return true;
        }

        if (hasRoleForOrgUnit(dbc, dbc.currentUser(), CmsRole.PROJECT_MANAGER, null)) {
            // user is admin            
            return true;
        }

        // get all groups of the user
        List groups;
        try {
            groups = m_driverManager.getGroupsOfUser(
                dbc,
                dbc.currentUser().getName(),
                CmsOrganizationalUnit.SEPARATOR,
                true,
                false,
                false,
                dbc.getRequestContext().getRemoteAddress());
        } catch (CmsException e) {
            // any exception: result is false
            return false;
        }

        for (int i = 0; i < groups.size(); i++) {
            // check if the user is a member in the current projects manager group
            if (((CmsGroup)groups.get(i)).getId().equals(project.getManagerGroupId())) {
                // this group is manager of the project
                return true;
            }
        }

        // the user is not manager of the current project
        return false;
    }

    /**
     * Performs a non-blocking permission check on a resource.<p>
     * 
     * This test will not throw an exception in case the required permissions are not
     * available for the requested operation. Instead, it will return one of the 
     * following values:<ul>
     * <li><code>{@link #PERM_ALLOWED}</code></li>
     * <li><code>{@link #PERM_FILTERED}</code></li>
     * <li><code>{@link #PERM_DENIED}</code></li></ul><p>
     * 
     * @param context the current request context
     * @param resource the resource on which permissions are required
     * @param requiredPermissions the set of permissions required for the operation
     * @param checkLock if true, a lock for the current user is required for 
     *      all write operations, if false it's ok to write as long as the resource
     *      is not locked by another user
     * @param filter the resource filter to use
     * 
     * @return <code>{@link #PERM_ALLOWED}</code> if the user has sufficient permissions on the resource
     *      for the requested operation
     * 
     * @throws CmsException in case of i/o errors (NOT because of insufficient permissions)
     * 
     * @see #hasPermissions(CmsDbContext, CmsResource, CmsPermissionSet, boolean, CmsResourceFilter)
     */
    public int hasPermissions(
        CmsRequestContext context,
        CmsResource resource,
        CmsPermissionSet requiredPermissions,
        boolean checkLock,
        CmsResourceFilter filter) throws CmsException {

        int result = 0;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = hasPermissions(dbc, resource, requiredPermissions, checkLock, filter);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Checks if the given user has the given role.<p>
     *  
     * This method can only be used for roles that are not organizational unit dependent.<p>
     *  
     * @param context the current OpenCms context
     * @param user the user to check the role for
     * @param role the role to check
     * 
     * @return <code>true</code> if the given user has the given role
     */
    public boolean hasRole(CmsRequestContext context, CmsUser user, CmsRole role) {

        if (!role.isOrganizationalUnitIndependent()) {
            return false;
        }
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        boolean result;
        try {
            result = hasRoleForOrgUnit(dbc, user, role, null);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Checks if the given user has the given role in the given organizational unit.<p>
     *  
     * If the organizational unit is <code>null</code>, this method will check if the
     * given user has the given role for at least one organizational unit.<p>
     *  
     * @param dbc the current OpenCms users database context
     * @param user the user to check the role for
     * @param role the role to check
     * @param orgUnitFqn the organizational unit the check the role for, may be <code>null</code>
     * 
     * @return <code>true</code> if the given user has the given role in the given organizational unit
     */
    public boolean hasRoleForOrgUnit(CmsDbContext dbc, CmsUser user, CmsRole role, String orgUnitFqn) {

        // read all roles of the current user
        List roles;
        try {
            roles = m_driverManager.getGroupsOfUser(
                dbc,
                user.getName(),
                CmsOrganizationalUnit.SEPARATOR,
                true,
                true,
                true,
                dbc.getRequestContext().getRemoteAddress());
        } catch (CmsException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e);
            }
            // any exception: return false
            return false;
        }
        return hasRole(role, roles, orgUnitFqn);
    }

    /**
     * Checks if the given user has the given role in the given organizational unit.<p>
     *  
     * If the organizational unit is <code>null</code>, this method will check if the
     * given user has the given role for at least one organizational unit.<p>
     *  
     * @param context the current request context
     * @param user the user to check the role for
     * @param role the role to check
     * @param orgUnitFqn the organizational unit to check the role for
     * 
     * @return <code>true</code> if the given user has the given role in the given organizational unit
     */
    public boolean hasRoleForOrgUnit(CmsRequestContext context, CmsUser user, CmsRole role, String orgUnitFqn) {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        boolean result;
        try {
            result = hasRoleForOrgUnit(dbc, user, role, orgUnitFqn);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Checks if the given user has the given role for the given resource.<p>
     *  
     * @param dbc the current OpenCms users database context
     * @param user the user to check the role for
     * @param role the role to check
     * @param resource the resource to check the role for
     * 
     * @return <code>true</code> if the given user has the given role for the given resource
     */
    public boolean hasRoleForResource(CmsDbContext dbc, CmsUser user, CmsRole role, CmsResource resource) {

        // read all roles of the current user in the given organizational unit
        List orgUnits;
        try {
            orgUnits = m_driverManager.getOrganizationalUnitsForResource(dbc, resource);
        } catch (CmsException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e);
            }
            // any exception: return false
            return false;
        }
        Iterator it = orgUnits.iterator();
        while (it.hasNext()) {
            String orgUnitFqn = (String)it.next();
            if (hasRoleForOrgUnit(dbc, user, role, orgUnitFqn)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given user has the given role for the given resource.<p>
     *  
     * @param context the current request context
     * @param user the user to check
     * @param role the role to check
     * @param resource the resource to check the role for
     * 
     * @return <code>true</code> if the given user has the given role for the given resource
     */
    public boolean hasRoleForResource(CmsRequestContext context, CmsUser user, CmsRole role, CmsResource resource) {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        boolean result;
        try {
            result = hasRoleForResource(dbc, user, role, resource);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Writes a list of access control entries as new access control entries of a given resource.<p>
     * 
     * Already existing access control entries of this resource are removed before.<p>
     * 
     * Access is granted, if:<p>
     * <ul>
     * <li>the current user has control permission on the resource</li>
     * </ul><p>
     * 
     * @param context the current request context
     * @param resource the resource
     * @param acEntries a list of <code>{@link CmsAccessControlEntry}</code> objects
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if the required permissions are not satisfied
     */
    public void importAccessControlEntries(CmsRequestContext context, CmsResource resource, List acEntries)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_CONTROL, true, CmsResourceFilter.ALL);
            m_driverManager.importAccessControlEntries(dbc, resource, acEntries);
        } catch (Exception e) {
            dbc.report(
                null,
                Messages.get().container(Messages.ERR_IMPORT_ACL_ENTRIES_1, context.getSitePath(resource)),
                e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Creates a new resource with the provided content and properties.<p>
     * 
     * The <code>content</code> parameter may be null if the resource id already exists.
     * If so, the created resource will be made a sibling of the existing resource,
     * the existing content will remain unchanged.
     * This is used during file import for import of siblings as the 
     * <code>manifest.xml</code> only contains one binary copy per file. 
     * If the resource id exists but the <code>content</code> is not null,
     * the created resource will be made a sibling of the existing resource,
     * and both will share the new content.<p>
     * 
     * Note: the id used to identify the content record (pk of the record) is generated
     * on each call of this method (with valid content) !
     * 
     * @param context the current request context
     * @param resourcePath the name of the resource to create (full path)
     * @param resource the new resource to create
     * @param content the content for the new resource
     * @param properties the properties for the new resource
     * @param importCase if true, signals that this operation is done while importing resource, causing different lock behaviour and potential "lost and found" usage
     * @return the created resource
     * @throws CmsException if something goes wrong
     */
    public CmsResource importResource(
        CmsRequestContext context,
        String resourcePath,
        CmsResource resource,
        byte[] content,
        List properties,
        boolean importCase) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsResource newResource = null;
        try {
            checkOfflineProject(dbc);
            newResource = m_driverManager.createResource(dbc, resourcePath, resource, content, properties, importCase);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_IMPORT_RESOURCE_2,
                context.getSitePath(resource),
                resourcePath), e);
        } finally {
            dbc.clear();
        }
        return newResource;
    }

    /**
     * Creates a new user by import.<p>
     * 
     * @param context the current request context
     * @param id the id of the user
     * @param name the new name for the user
     * @param password the new password for the user
     * @param description the description for the user
     * @param firstname the firstname of the user
     * @param lastname the lastname of the user
     * @param email the email of the user
     * @param address the address of the user
     * @param flags the flags for a user (for example <code>{@link I_CmsPrincipal#FLAG_ENABLED}</code>)
     * @param additionalInfos the additional user infos
     * 
     * @return the imported user
     *
     * @throws CmsException if something goes wrong
     * @throws CmsRoleViolationException if the  role {@link CmsRole#ACCOUNT_MANAGER} is not owned by the current user.
     */
    public CmsUser importUser(
        CmsRequestContext context,
        String id,
        String name,
        String password,
        String description,
        String firstname,
        String lastname,
        String email,
        String address,
        int flags,
        Map additionalInfos) throws CmsException, CmsRoleViolationException {

        CmsUser newUser = null;

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);

        try {
            checkRoleForOrgUnit(dbc, CmsRole.ACCOUNT_MANAGER, getParentOrganizationalUnit(name));
            newUser = m_driverManager.importUser(
                dbc,
                id,
                name,
                password,
                description,
                firstname,
                lastname,
                email,
                address,
                flags,
                additionalInfos);

        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_IMPORT_USER_8,
                new Object[] {
                    id,
                    name,
                    description,
                    firstname,
                    lastname,
                    email,
                    address,
                    new Integer(flags),
                    additionalInfos}), e);
        } finally {
            dbc.clear();
        }

        return newUser;
    }

    /**
     * Initializes this security manager with a given runtime info factory.<p>
     * 
     * @param configurationManager the configurationManager
     * @param dbContextFactory the initialized OpenCms runtime info factory
     * @param publishEngine the publish engine
     * 
     * @throws CmsInitException if the initialization fails
     */
    public void init(
        CmsConfigurationManager configurationManager,
        I_CmsDbContextFactory dbContextFactory,
        CmsPublishEngine publishEngine) throws CmsInitException {

        if (dbContextFactory == null) {
            throw new CmsInitException(org.opencms.main.Messages.get().container(
                org.opencms.main.Messages.ERR_CRITICAL_NO_DB_CONTEXT_0));
        }

        m_dbContextFactory = dbContextFactory;

        CmsSystemConfiguration systemConfiguation = (CmsSystemConfiguration)configurationManager.getConfiguration(CmsSystemConfiguration.class);
        CmsCacheSettings settings = systemConfiguation.getCacheSettings();

        String className = settings.getCacheKeyGenerator();
        try {
            // initialize the key generator
            m_keyGenerator = (I_CmsCacheKey)Class.forName(className).newInstance();
        } catch (Exception e) {
            throw new CmsInitException(org.opencms.main.Messages.get().container(
                org.opencms.main.Messages.ERR_CRITICAL_CLASS_CREATION_1,
                className));
        }

        LRUMap lruMap = new LRUMap(settings.getPermissionCacheSize());
        m_permissionCache = Collections.synchronizedMap(lruMap);
        if (OpenCms.getMemoryMonitor().enabled()) {
            OpenCms.getMemoryMonitor().register(this.getClass().getName() + ".m_permissionCache", lruMap);
        }

        // create the driver manager
        m_driverManager = CmsDriverManager.newInstance(configurationManager, this, dbContextFactory, publishEngine);

        try {
            // invoke the init method of the driver manager
            m_driverManager.init(configurationManager);
            if (CmsLog.INIT.isInfoEnabled()) {
                CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_DRIVER_MANAGER_START_PHASE4_OK_0));
            }
        } catch (Exception exc) {
            CmsMessageContainer message = Messages.get().container(Messages.LOG_ERR_DRIVER_MANAGER_START_0);
            if (LOG.isFatalEnabled()) {
                LOG.fatal(message.key(), exc);
            }
            throw new CmsInitException(message, exc);
        }

        // create a new lock manager
        m_lockManager = m_driverManager.getLockManager();

        try {
            // now read the persistent locks
            readLocks();
        } catch (CmsException e) {

            if (LOG.isErrorEnabled()) {
                LOG.error(
                    org.opencms.lock.Messages.get().getBundle().key(org.opencms.lock.Messages.ERR_READ_LOCKS_0),
                    e);
            }
        }

        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_SECURITY_MANAGER_INIT_0));
        }
    }

    /**
     * Checks if the specified resource is inside the current project.<p>
     * 
     * The project "view" is determined by a set of path prefixes. 
     * If the resource starts with any one of this prefixes, it is considered to 
     * be "inside" the project.<p>
     * 
     * @param context the current request context
     * @param resourcename the specified resource name (full path)
     * 
     * @return <code>true</code>, if the specified resource is inside the current project
     */
    public boolean isInsideCurrentProject(CmsRequestContext context, String resourcename) {

        boolean result = false;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = m_driverManager.isInsideCurrentProject(dbc, resourcename);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Checks if the current user has management access to the current project.<p>
     *
     * @param context the current request context
     *
     * @return <code>true</code>, if the user has management access to the current project
     */
    public boolean isManagerOfProject(CmsRequestContext context) {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        return hasManagerOfProjectRole(dbc, context.currentProject());
    }

    /**
     * Locks a resource.<p>
     *
     * The <code>type</code> parameter controls what kind of lock is used.<br>
     * Possible values for this parameter are: <br>
     * <ul>
     * <li><code>{@link org.opencms.lock.CmsLockType#EXCLUSIVE}</code></li>
     * <li><code>{@link org.opencms.lock.CmsLockType#TEMPORARY}</code></li>
     * <li><code>{@link org.opencms.lock.CmsLockType#WORKFLOW}</code></li>
     * <li><code>{@link org.opencms.lock.CmsLockType#PUBLISH}</code></li>
     * </ul><p>
     * 
     * @param context the current request context
     * @param resource the resource to lock
     * @param type type of the lock
     * 
     * @throws CmsException if something goes wrong
     * 
     * @see CmsObject#lockResource(String)
     * @see CmsObject#lockResourceTemporary(String)
     * @see org.opencms.file.types.I_CmsResourceType#lockResource(CmsObject, CmsSecurityManager, CmsResource, CmsLockType)
     */
    public void lockResource(CmsRequestContext context, CmsResource resource, CmsLockType type) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_WRITE, false, CmsResourceFilter.ALL);
            m_driverManager.lockResource(dbc, resource, type);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_LOCK_RESOURCE_2,
                context.getSitePath(resource),
                type.toString()), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Attempts to authenticate a user into OpenCms with the given password.<p>
     * 
     * @param context the current request context
     * @param username the name of the user to be logged in
     * @param password the password of the user
     * @param remoteAddress the ip address of the request
     * 
     * @return the logged in user
     *
     * @throws CmsException if the login was not succesful
     */
    public CmsUser loginUser(CmsRequestContext context, String username, String password, String remoteAddress)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsUser result = null;
        try {
            result = m_driverManager.loginUser(dbc, username, password, remoteAddress);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Lookup and read the user or group with the given UUID.<p>
     * 
     * @param context the current request context
     * @param principalId the UUID of the principal to lookup
     * 
     * @return the principal (group or user) if found, otherwise <code>null</code>
     */
    public I_CmsPrincipal lookupPrincipal(CmsRequestContext context, CmsUUID principalId) {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        I_CmsPrincipal result = null;
        try {
            result = m_driverManager.lookupPrincipal(dbc, principalId);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Lookup and read the user or group with the given name.<p>
     * 
     * @param context the current request context
     * @param principalName the name of the principal to lookup
     * 
     * @return the principal (group or user) if found, otherwise <code>null</code>
     */
    public I_CmsPrincipal lookupPrincipal(CmsRequestContext context, String principalName) {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        I_CmsPrincipal result = null;
        try {
            result = m_driverManager.lookupPrincipal(dbc, principalName);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Moves a resource.<p>
     * 
     * You must ensure that the destination path is an absolute, valid and
     * existing VFS path. Relative paths from the source are currently not supported.<p>
     * 
     * The moved resource will always be locked to the current user
     * after the move operation.<p>
     * 
     * In case the target resource already exists, it is overwritten with the 
     * source resource.<p>
     * 
     * @param context the current request context
     * @param source the resource to copy
     * @param destination the name of the copy destination with complete path
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if resource could not be copied 
     * 
     * @see CmsObject#moveResource(String, String)
     * 
     * @see org.opencms.file.types.I_CmsResourceType#moveResource(CmsObject, CmsSecurityManager, CmsResource, String)
     */
    public void moveResource(CmsRequestContext context, CmsResource source, String destination)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, source, CmsPermissionSet.ACCESS_READ, true, CmsResourceFilter.ALL);
            checkPermissions(dbc, source, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.ALL);
            checkSystemLocks(dbc, source);

            // no permissions are checked for subresources in case of moving a folder
            moveResource(dbc, source, destination);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_MOVE_RESOURCE_2,
                dbc.removeSiteRoot(source.getRootPath()),
                dbc.removeSiteRoot(destination)), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Moves a resource to the "lost and found" folder.<p>
     * 
     * The method can also be used to check get the name of a resource
     * in the "lost and found" folder only without actually moving the
     * the resource. To do this, the <code>returnNameOnly</code> flag
     * must be set to <code>true</code>.<p>
     * 
     * In general, it is the same name as the given resource has, the only exception is
     * if a resource in the "lost and found" folder with the same name already exists. 
     * In such case, a counter is added to the resource name.<p>
     * 
     * @param context the current request context
     * @param resource the resource to apply this operation to
     * @param returnNameOnly if <code>true</code>, only the name of the resource in the "lost and found" 
     *        folder is returned, the move operation is not really performed
     * 
     * @return the name of the resource inside the "lost and found" folder
     * 
     * @throws CmsException if something goes wrong
     * 
     * @see CmsObject#moveToLostAndFound(String)
     * @see CmsObject#getLostAndFoundName(String)
     */
    public String moveToLostAndFound(CmsRequestContext context, CmsResource resource, boolean returnNameOnly)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        String result = null;
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_READ, true, CmsResourceFilter.ALL);
            if (!returnNameOnly) {
                checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.ALL);
            }
            result = m_driverManager.moveToLostAndFound(dbc, resource, returnNameOnly);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_MOVE_TO_LOST_AND_FOUND_1,
                dbc.removeSiteRoot(resource.getRootPath())), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Publishes the resources of a specified publish list.<p>
     *
     * @param cms the current request context
     * @param publishList a publish list
     * @param report an instance of <code>{@link I_CmsReport}</code> to print messages
     * 
     * @return the publish history id of the published project
     * 
     * @throws CmsException if something goes wrong
     * 
     * @see #fillPublishList(CmsRequestContext, CmsPublishList)
     */
    public CmsUUID publishProject(CmsObject cms, CmsPublishList publishList, I_CmsReport report) throws CmsException {

        CmsRequestContext context = cms.getRequestContext();
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            // check if the current user has the required publish permissions
            checkPublishPermissions(dbc, publishList);
            m_driverManager.publishProject(cms, dbc, publishList, report);
        } finally {
            dbc.clear();
        }
        return publishList.getPublishHistoryId();
    }

    /**
     * Reads all file headers of a file.<br>
     * 
     * This method returns a list with the history of all file headers, i.e.
     * the file headers of a file, independent of the project they were attached to.<br>
     *
     * The reading excludes the file content.<p>
     *
     * @param context the current request context
     * @param resource the resource to be read
     * 
     * @return a list of file headers, as <code>{@link CmsBackupResource}</code> objects, read from the Cms
     * 
     * @throws CmsException if something goes wrong
     */
    public List readAllBackupFileHeaders(CmsRequestContext context, CmsResource resource) throws CmsException {

        List result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = m_driverManager.readAllBackupFileHeaders(dbc, resource);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_READ_ALL_BKP_FILE_HEADERS_1,
                context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads all propertydefinitions for the given mapping type.<p>
     *
     * @param context the current request context
     * 
     * @return a list with the <code>{@link CmsPropertyDefinition}</code> objects (may be empty)
     * 
     * @throws CmsException if something goes wrong
     */
    public List readAllPropertyDefinitions(CmsRequestContext context) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            result = m_driverManager.readAllPropertyDefinitions(dbc);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_ALL_PROPDEF_0), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns the first ancestor folder matching the filter criteria.<p>
     * 
     * If no folder matching the filter criteria is found, null is returned.<p>
     * 
     * @param context the context of the current request
     * @param resource the resource to start
     * @param filter the resource filter to match while reading the ancestors
     * 
     * @return the first ancestor folder matching the filter criteria or null if no folder was found
     * 
     * @throws CmsException if something goes wrong
     */
    public CmsFolder readAncestor(CmsRequestContext context, CmsResource resource, CmsResourceFilter filter)
    throws CmsException {

        // get the full folder path of the resource to start from
        String path = CmsResource.getFolderPath(resource.getRootPath());
        do {
            // check if the current folder matches the given filter
            if (existsResource(context, path, filter)) {
                // folder matches, return it
                return readFolder(context, path, filter);
            } else {
                // folder does not match filter criteria, go up one folder
                path = CmsResource.getParentFolder(path);
            }

            if (CmsStringUtil.isEmpty(path) || !path.startsWith(context.getSiteRoot())) {
                // site root or root folder reached and no matching folder found
                return null;
            }
        } while (true);
    }

    /**
     * Returns a file from the history.<br>
     * 
     * The reading includes the file content.<p>
     *
     * @param context the current request context
     * @param tagId the id of the tag of the file
     * @param resource the resource to be read
     * 
     * @return the file read
     * 
     * @throws CmsException if operation was not succesful
     */
    public CmsBackupResource readBackupFile(CmsRequestContext context, int tagId, CmsResource resource)
    throws CmsException {

        CmsBackupResource result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = m_driverManager.readBackupFile(dbc, tagId, resource);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_READ_BKP_FILE_2,
                context.getSitePath(resource),
                new Integer(tagId)), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns a backup project.<p>
     *
     * @param context the current request context
     * @param tagId the tagId of the project
     * 
     * @return the requested backup project
     * 
     * @throws CmsException if something goes wrong
     */
    public CmsBackupProject readBackupProject(CmsRequestContext context, int tagId) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsBackupProject result = null;
        try {
            result = m_driverManager.readBackupProject(dbc, tagId);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_READ_BKP_PROJECT_2,
                new Integer(tagId),
                dbc.currentProject().getName()), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads the list of <code>{@link CmsProperty}</code> objects that belong the the given backup resource.<p>
     * 
     * @param context the current request context
     * @param resource the backup resource to read the properties from
     * 
     * @return the list of <code>{@link CmsProperty}</code> objects that belong the the given backup resource
     * 
     * @throws CmsException if something goes wrong
     */
    public List readBackupPropertyObjects(CmsRequestContext context, CmsBackupResource resource) throws CmsException {

        List result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = m_driverManager.readBackupPropertyObjects(dbc, resource);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_READ_PROPS_FOR_RESOURCE_1,
                context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns the child resources of a resource, that is the resources
     * contained in a folder.<p>
     * 
     * With the parameters <code>getFolders</code> and <code>getFiles</code>
     * you can control what type of resources you want in the result list:
     * files, folders, or both.<p>
     * 
     * This method is mainly used by the workplace explorer.<p> 
     * 
     * @param context the current request context
     * @param resource the resource to return the child resources for
     * @param filter the resource filter to use
     * @param getFolders if true the child folders are included in the result
     * @param getFiles if true the child files are included in the result
     * 
     * @return a list of all child resources
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if the user has insufficient permission for the given resource (read is required).
     * 
     */
    public List readChildResources(
        CmsRequestContext context,
        CmsResource resource,
        CmsResourceFilter filter,
        boolean getFolders,
        boolean getFiles) throws CmsException, CmsSecurityException {

        List result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            // check the access permissions
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_READ, true, CmsResourceFilter.ALL);
            result = m_driverManager.readChildResources(dbc, resource, filter, getFolders, getFiles, true);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_READ_CHILD_RESOURCES_1,
                context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns the default file for the given folder.<p>
     * 
     * If the given resource is a file, then this file is returned.<p>
     * 
     * Otherwise, in case of a folder:<br> 
     * <ol>
     *   <li>the {@link CmsPropertyDefinition#PROPERTY_DEFAULT_FILE} is checked, and
     *   <li>if still no file could be found, the configured default files in the 
     *       <code>opencms-vfs.xml</code> configuration are iterated until a match is 
     *       found, and
     *   <li>if still no file could be found, <code>null</code> is retuned
     * </ol>
     * 
     * @param context the request context
     * @param resource the folder to get the default file for
     * 
     * @return the default file for the given folder
     * 
     * @throws CmsSecurityException if the user has no permissions to read the resulting file
     * 
     * @see CmsObject#readDefaultFile(String)
     * @see CmsDriverManager#readDefaultFile(CmsDbContext, CmsResource)
     */
    public CmsResource readDefaultFile(CmsRequestContext context, CmsResource resource) throws CmsSecurityException {

        CmsResource result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = m_driverManager.readDefaultFile(dbc, resource);
            if (result != null) {
                // check if the user has read access to the resource
                checkPermissions(dbc, result, CmsPermissionSet.ACCESS_READ, true, CmsResourceFilter.DEFAULT);
            }
        } catch (CmsSecurityException se) {
            // permissions deny access to the resource
            throw se;
        } catch (CmsException e) {
            // ignore all other exceptions
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads a file resource (including it's binary content) from the VFS,
     * using the specified resource filter.<p>
     * 
     * In case you do not need the file content, 
     * use <code>{@link #readResource(CmsRequestContext, String, CmsResourceFilter)}</code> instead.<p>
     * 
     * The specified filter controls what kind of resources should be "found" 
     * during the read operation. This will depend on the application. For example, 
     * using <code>{@link CmsResourceFilter#DEFAULT}</code> will only return currently
     * "valid" resources, while using <code>{@link CmsResourceFilter#IGNORE_EXPIRATION}</code>
     * will ignore the date release / date expired information of the resource.<p>
     *
     * @param context the current request context
     * @param resource the resource to be read
     * @param filter the filter object
     * 
     * @return the file read from the VFS
     * 
     * @throws CmsException if something goes wrong
     */
    public CmsFile readFile(CmsRequestContext context, CmsResource resource, CmsResourceFilter filter)
    throws CmsException {

        CmsFile result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = m_driverManager.readFile(dbc, resource, filter);
        } catch (Exception e) {
            dbc.report(
                null,
                Messages.get().container(Messages.ERR_READ_FILE_2, context.getSitePath(resource), filter),
                e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads a folder resource from the VFS,
     * using the specified resource filter.<p>
     *
     * The specified filter controls what kind of resources should be "found" 
     * during the read operation. This will depend on the application. For example, 
     * using <code>{@link CmsResourceFilter#DEFAULT}</code> will only return currently
     * "valid" resources, while using <code>{@link CmsResourceFilter#IGNORE_EXPIRATION}</code>
     * will ignore the date release / date expired information of the resource.<p>
     * 
     * @param context the current request context
     * @param resourcename the name of the folder to read (full path)
     * @param filter the resource filter to use while reading
     *
     * @return the folder that was read
     *
     * @throws CmsException if something goes wrong
     */
    public CmsFolder readFolder(CmsRequestContext context, String resourcename, CmsResourceFilter filter)
    throws CmsException {

        CmsFolder result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = readFolder(dbc, resourcename, filter);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_FOLDER_2, resourcename, filter), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads the group of a project.<p>
     *
     * @param context the current request context
     * @param project the project to read from
     * 
     * @return the group of a resource
     */
    public CmsGroup readGroup(CmsRequestContext context, CmsProject project) {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsGroup result = null;
        try {
            result = m_driverManager.readGroup(dbc, project);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads a group based on its id.<p>
     *
     * @param context the current request context
     * @param groupId the id of the group that is to be read
     *
     * @return the requested group
     * 
     * @throws CmsException if operation was not succesful
     */
    public CmsGroup readGroup(CmsRequestContext context, CmsUUID groupId) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsGroup result = null;
        try {
            result = m_driverManager.readGroup(dbc, groupId);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_GROUP_FOR_ID_1, groupId.toString()), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads a group based on its name.<p>
     * 
     * @param context the current request context
     * @param groupname the name of the group that is to be read
     *
     * @return the requested group
     * 
     * @throws CmsException if operation was not succesful
     */
    public CmsGroup readGroup(CmsRequestContext context, String groupname) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsGroup result = null;
        try {
            result = m_driverManager.readGroup(dbc, groupname);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_GROUP_FOR_NAME_1, groupname), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads the locks that were saved to the database in the previous run of OpenCms.<p>
     * 
     * @throws CmsException if something goes wrong
     */
    public void readLocks() throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext();
        try {
            m_driverManager.readLocks(dbc);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Reads the manager group of a project.<p>
     *
     * @param context the current request context
     * @param project the project to read from
     *
     * @return the group of a resource
     */
    public CmsGroup readManagerGroup(CmsRequestContext context, CmsProject project) {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsGroup result = null;
        try {
            result = m_driverManager.readManagerGroup(dbc, project);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads an organizational Unit based on its fully qualified name.<p>
     *
     * @param context the current request context
     * @param ouFqn the fully qualified name of the organizational Unit to be read
     * 
     * @return the organizational Unit that with the provided fully qualified name
     * 
     * @throws CmsException if something goes wrong
     */
    public CmsOrganizationalUnit readOrganizationalUnit(CmsRequestContext context, String ouFqn) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsOrganizationalUnit result = null;
        try {
            result = m_driverManager.readOrganizationalUnit(dbc, ouFqn);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_ORGUNIT_1, ouFqn), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads the owner of a project from the OpenCms.<p>
     *
     * @param context the current request context
     * @param project the project to get the owner from
     * 
     * @return the owner of a resource
     * 
     * @throws CmsException if something goes wrong
     */
    public CmsUser readOwner(CmsRequestContext context, CmsProject project) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsUser result = null;
        try {
            result = m_driverManager.readOwner(dbc, project);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_READ_OWNER_FOR_PROJECT_2,
                project.getName(),
                new Integer(project.getId())), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Builds a list of resources for a given path.<p>
     * 
     * @param context the current request context
     * @param projectId the project to lookup the resource
     * @param path the requested path
     * @param filter a filter object (only "includeDeleted" information is used!)
     * 
     * @return list of <code>{@link CmsResource}</code>s
     * 
     * @throws CmsException if something goes wrong
     */
    public List readPath(CmsRequestContext context, int projectId, String path, CmsResourceFilter filter)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            result = m_driverManager.readPath(dbc, projectId, path, filter);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_PATH_2, new Integer(projectId), path), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads a project given the projects id.<p>
     *
     * @param id the id of the project
     * 
     * @return the project read
     * 
     * @throws CmsException if something goes wrong
     */
    public CmsProject readProject(int id) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext();
        CmsProject result = null;
        try {
            result = m_driverManager.readProject(dbc, id);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_PROJECT_FOR_ID_1, new Integer(id)), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads a project.<p>
     *
     * Important: Since a project name can be used multiple times, this is NOT the most efficient 
     * way to read the project. This is only a convenience for front end developing.
     * Reading a project by name will return the first project with that name. 
     * All core classes must use the id version {@link #readProject(int)} to ensure the right project is read.<p>
     * 
     * @param name the name of the project
     * 
     * @return the project read
     * 
     * @throws CmsException if something goes wrong
     */
    public CmsProject readProject(String name) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext();
        CmsProject result = null;
        try {
            result = m_driverManager.readProject(dbc, name);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_PROJECT_FOR_NAME_1, name), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns the list of all resource names that define the "view" of the given project.<p>
     *
     * @param context the current request context
     * @param project the project to get the project resources for
     * 
     * @return the list of all resources, as <code>{@link String}</code> objects 
     *              that define the "view" of the given project.
     * 
     * @throws CmsException if something goes wrong
     */
    public List readProjectResources(CmsRequestContext context, CmsProject project) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            result = m_driverManager.readProjectResources(dbc, project);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_READ_PROJECT_RESOURCES_2,
                project.getName(),
                new Integer(project.getId())), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads all resources of a project that match a given state from the VFS.<p>
     * 
     * Possible values for the <code>state</code> parameter are:<br>
     * <ul>
     * <li><code>{@link CmsResource#STATE_CHANGED}</code>: Read all "changed" resources in the project</li>
     * <li><code>{@link CmsResource#STATE_NEW}</code>: Read all "new" resources in the project</li>
     * <li><code>{@link CmsResource#STATE_DELETED}</code>: Read all "deleted" resources in the project</li>
     * <li><code>{@link CmsResource#STATE_KEEP}</code>: Read all resources either "changed", "new" or "deleted" in the project</li>
     * </ul><p>
     * 
     * @param context the current request context
     * @param projectId the id of the project to read the file resources for
     * @param state the resource state to match 
     *
     * @return a list of <code>{@link CmsResource}</code> objects matching the filter criteria
     * 
     * @throws CmsException if something goes wrong
     * 
     * @see CmsObject#readProjectView(int, CmsResourceState)
     */
    public List readProjectView(CmsRequestContext context, int projectId, CmsResourceState state) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            result = m_driverManager.readProjectView(dbc, projectId, state);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_PROJECT_VIEW_1, new Integer(projectId)), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads a property definition.<p>
     *
     * If no property definition with the given name is found, 
     * <code>null</code> is returned.<p>
     * 
     * @param context the current request context
     * @param name the name of the property definition to read
     * 
     * @return the property definition that was read, 
     *          or <code>null</code> if there is no property definition with the given name.
     * 
     * @throws CmsException if something goes wrong
     */
    public CmsPropertyDefinition readPropertyDefinition(CmsRequestContext context, String name) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsPropertyDefinition result = null;
        try {
            result = m_driverManager.readPropertyDefinition(dbc, name);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_PROPDEF_1, name), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads a property object from a resource specified by a property name.<p>
     * 
     * Returns <code>{@link CmsProperty#getNullProperty()}</code> if the property is not found.<p>
     * 
     * @param context the context of the current request
     * @param resource the resource where the property is mapped to
     * @param key the property key name
     * @param search if <code>true</code>, the property is searched on all parent folders of the resource. 
     *      if it's not found attached directly to the resource.
     * 
     * @return the required property, or <code>{@link CmsProperty#getNullProperty()}</code> if the property was not found
     * 
     * @throws CmsException if something goes wrong
     */
    public CmsProperty readPropertyObject(CmsRequestContext context, CmsResource resource, String key, boolean search)
    throws CmsException {

        CmsProperty result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = m_driverManager.readPropertyObject(dbc, resource, key, search);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_READ_PROP_FOR_RESOURCE_2,
                key,
                context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads all property objects from a resource.<p>
     * 
     * Returns an empty list if no properties are found.<p>
     * 
     * If the <code>search</code> parameter is <code>true</code>, the properties of all 
     * parent folders of the resource are also read. The results are merged with the 
     * properties directly attached to the resource. While merging, a property
     * on a parent folder that has already been found will be ignored.
     * So e.g. if a resource has a property "Title" attached, and it's parent folder 
     * has the same property attached but with a differrent value, the result list will
     * contain only the property with the value from the resource, not form the parent folder(s).<p>
     * 
     * @param context the context of the current request
     * @param resource the resource where the property is mapped to
     * @param search <code>true</code>, if the properties should be searched on all parent folders  if not found on the resource
     * 
     * @return a list of <code>{@link CmsProperty}</code> objects
     * 
     * @throws CmsException if something goes wrong
     */
    public List readPropertyObjects(CmsRequestContext context, CmsResource resource, boolean search)
    throws CmsException {

        List result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = m_driverManager.readPropertyObjects(dbc, resource, search);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_READ_PROPS_FOR_RESOURCE_1,
                context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads the resources that were published in a publish task for a given publish history ID.<p>
     * 
     * @param context the current request context
     * @param publishHistoryId unique int ID to identify each publish task in the publish history
     * 
     * @return a list of <code>{@link org.opencms.db.CmsPublishedResource}</code> objects
     * 
     * @throws CmsException if something goes wrong
     */
    public List readPublishedResources(CmsRequestContext context, CmsUUID publishHistoryId) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            result = m_driverManager.readPublishedResources(dbc, publishHistoryId);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_READ_PUBLISHED_RESOURCES_FOR_ID_1,
                publishHistoryId.toString()), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads a resource from the VFS,
     * using the specified resource filter.<p>
     *
     * A resource may be of type <code>{@link CmsFile}</code> or 
     * <code>{@link CmsFolder}</code>. In case of
     * a file, the resource will not contain the binary file content. Since reading 
     * the binary content is a cost-expensive database operation, it's recommended 
     * to work with resources if possible, and only read the file content when absolutly
     * required. To "upgrade" a resource to a file, 
     * use <code>{@link CmsFile#upgrade(CmsResource, CmsObject)}</code>.<p> 
     *
     * The specified filter controls what kind of resources should be "found" 
     * during the read operation. This will depend on the application. For example, 
     * using <code>{@link CmsResourceFilter#DEFAULT}</code> will only return currently
     * "valid" resources, while using <code>{@link CmsResourceFilter#IGNORE_EXPIRATION}</code>
     * will ignore the date release / date expired information of the resource.<p>
     * 
     * @param context the current request context
     * @param structureID the ID of the structure which will be used)
     * @param filter the resource filter to use while reading
     *
     * @return the resource that was read
     *
     * @throws CmsException if the resource could not be read for any reason
     * 
     * @see CmsObject#readResource(CmsUUID, CmsResourceFilter)
     * @see CmsObject#readResource(CmsUUID)
     * @see CmsFile#upgrade(CmsResource, CmsObject)
     */
    public CmsResource readResource(CmsRequestContext context, CmsUUID structureID, CmsResourceFilter filter)
    throws CmsException {

        CmsResource result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = readResource(dbc, structureID, filter);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_RESOURCE_FOR_ID_1, structureID), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads a resource from the VFS,
     * using the specified resource filter.<p>
     *
     * A resource may be of type <code>{@link CmsFile}</code> or 
     * <code>{@link CmsFolder}</code>. In case of
     * a file, the resource will not contain the binary file content. Since reading 
     * the binary content is a cost-expensive database operation, it's recommended 
     * to work with resources if possible, and only read the file content when absolutly
     * required. To "upgrade" a resource to a file, 
     * use <code>{@link CmsFile#upgrade(CmsResource, CmsObject)}</code>.<p> 
     *
     * The specified filter controls what kind of resources should be "found" 
     * during the read operation. This will depend on the application. For example, 
     * using <code>{@link CmsResourceFilter#DEFAULT}</code> will only return currently
     * "valid" resources, while using <code>{@link CmsResourceFilter#IGNORE_EXPIRATION}</code>
     * will ignore the date release / date expired information of the resource.<p>
     * 
     * @param context the current request context
     * @param resourcePath the name of the resource to read (full path)
     * @param filter the resource filter to use while reading
     *
     * @return the resource that was read
     *
     * @throws CmsException if the resource could not be read for any reason
     * 
     * @see CmsObject#readResource(String, CmsResourceFilter)
     * @see CmsObject#readResource(String)
     * @see CmsFile#upgrade(CmsResource, CmsObject)
     */
    public CmsResource readResource(CmsRequestContext context, String resourcePath, CmsResourceFilter filter)
    throws CmsException {

        CmsResource result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = readResource(dbc, resourcePath, filter);
        } catch (Exception e) {
            dbc.report(
                null,
                Messages.get().container(Messages.ERR_READ_RESOURCE_1, dbc.removeSiteRoot(resourcePath)),
                e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads all resources below the given path matching the filter criteria,
     * including the full tree below the path only in case the <code>readTree</code> 
     * parameter is <code>true</code>.<p>
     * 
     * @param context the current request context
     * @param parent the parent path to read the resources from
     * @param filter the filter
     * @param readTree <code>true</code> to read all subresources
     * 
     * @return a list of <code>{@link CmsResource}</code> objects matching the filter criteria
     *  
     * @throws CmsSecurityException if the user has insufficient permission for the given resource (read is required).
     * @throws CmsException if something goes wrong
     * 
     */
    public List readResources(CmsRequestContext context, CmsResource parent, CmsResourceFilter filter, boolean readTree)
    throws CmsException, CmsSecurityException {

        List result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            // check the access permissions
            checkPermissions(dbc, parent, CmsPermissionSet.ACCESS_READ, true, CmsResourceFilter.ALL);
            result = m_driverManager.readResources(dbc, parent, filter, readTree);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_READ_RESOURCES_1,
                context.removeSiteRoot(parent.getRootPath())), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Reads all resources that have a value (containing the specified value) set 
     * for the specified property (definition) in the given path.<p>
     * 
     * If the <code>value</code> parameter is <code>null</code>, all resources having the
     * given property set are returned.<p>
     * 
     * Both individual and shared properties of a resource are checked.<p>
     *
     * @param context the current request context
     * @param folder the folder to get the resources with the property from
     * @param propertyDefinition the name of the property (definition) to check for
     * @param value the string to search in the value of the property
     * 
     * @return a list of all <code>{@link CmsResource}</code> objects 
     *          that have a value set for the specified property.
     * 
     * @throws CmsException if something goes wrong
     */
    public List readResourcesWithProperty(
        CmsRequestContext context,
        CmsResource folder,
        String propertyDefinition,
        String value) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            result = m_driverManager.readResourcesWithProperty(dbc, folder, propertyDefinition, value);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_READ_RESOURCES_FOR_PROP_VALUE_3,
                context.removeSiteRoot(folder.getRootPath()),
                propertyDefinition,
                value), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns a set of users that are responsible for a specific resource.<p>
     * 
     * @param context the current request context
     * @param resource the resource to get the responsible users from
     * 
     * @return the set of users that are responsible for a specific resource
     * 
     * @throws CmsException if something goes wrong
     */
    public Set readResponsiblePrincipals(CmsRequestContext context, CmsResource resource) throws CmsException {

        Set result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = m_driverManager.readResponsiblePrincipals(dbc, resource);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_RESPONSIBLE_USERS_1, resource.getRootPath()), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns a set of users that are responsible for a specific resource.<p>
     * 
     * @param context the current request context
     * @param resource the resource to get the responsible users from
     * 
     * @return the set of users that are responsible for a specific resource
     * 
     * @throws CmsException if something goes wrong
     */
    public Set readResponsibleUsers(CmsRequestContext context, CmsResource resource) throws CmsException {

        Set result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = m_driverManager.readResponsibleUsers(dbc, resource);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_RESPONSIBLE_USERS_1, resource.getRootPath()), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns a List of all siblings of the specified resource,
     * the specified resource being always part of the result set.<p>
     * 
     * @param context the request context
     * @param resource the specified resource
     * @param filter a filter object
     * 
     * @return a list of <code>{@link CmsResource}</code>s that 
     *          are siblings to the specified resource, 
     *          including the specified resource itself.
     * 
     * @throws CmsException if something goes wrong
     */
    public List readSiblings(CmsRequestContext context, CmsResource resource, CmsResourceFilter filter)
    throws CmsException {

        List result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = m_driverManager.readSiblings(dbc, resource, filter);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_SIBLINGS_1, context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns the parameters of a resource in the table of all published template resources.<p>
     *
     * @param context the current request context
     * @param rfsName the rfs name of the resource
     * 
     * @return the paramter string of the requested resource
     * 
     * @throws CmsException if something goes wrong
     */
    public String readStaticExportPublishedResourceParameters(CmsRequestContext context, String rfsName)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        String result = null;
        try {
            result = m_driverManager.readStaticExportPublishedResourceParameters(dbc, rfsName);
        } catch (Exception e) {
            dbc.report(
                null,
                Messages.get().container(Messages.ERR_READ_STATEXP_PUBLISHED_RESOURCE_PARAMS_1, rfsName),
                e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns a list of all template resources which must be processed during a static export.<p>
     * 
     * @param context the current request context
     * @param parameterResources flag for reading resources with parameters (1) or without (0)
     * @param timestamp for reading the data from the db
     * 
     * @return a list of template resources as <code>{@link String}</code> objects
     * 
     * @throws CmsException if something goes wrong
     */
    public List readStaticExportResources(CmsRequestContext context, int parameterResources, long timestamp)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        List result = null;
        try {
            result = m_driverManager.readStaticExportResources(dbc, parameterResources, timestamp);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_STATEXP_RESOURCES_1, new Date(timestamp)), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns a user object based on the id of a user.<p>
     *
     * @param context the current request context
     * @param id the id of the user to read
     *
     * @return the user read
     * 
     * @throws CmsException if something goes wrong
     */
    public CmsUser readUser(CmsRequestContext context, CmsUUID id) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsUser result = null;
        try {
            result = m_driverManager.readUser(dbc, id);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_USER_FOR_ID_1, id.toString()), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns a user object.<p>
     *
     * @param context the current request context
     * @param username the name of the user that is to be read
     *
     * @return user read form the cms
     * 
     * @throws CmsException if operation was not succesful
     */
    public CmsUser readUser(CmsRequestContext context, String username) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsUser result = null;
        try {
            result = m_driverManager.readUser(dbc, username);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_USER_FOR_NAME_1, username), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Returns a user object if the password for the user is correct.<p>
     *
     * If the user/pwd pair is not valid a <code>{@link CmsException}</code> is thrown.<p>
     *
     * @param context the current request context
     * @param username the username of the user that is to be read
     * @param password the password of the user that is to be read
     * 
     * @return user read
     * 
     * @throws CmsException if operation was not succesful
     */
    public CmsUser readUser(CmsRequestContext context, String username, String password) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsUser result = null;
        try {
            result = m_driverManager.readUser(dbc, username, password);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_READ_USER_FOR_NAME_1, username), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Removes an access control entry for a given resource and principal.<p>
     * 
     * @param context the current request context
     * @param resource the resource
     * @param principal the id of the principal to remove the the access control entry for
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if the user has insufficient permission for the given resource (conrol of access control is required).
     * 
     */
    public void removeAccessControlEntry(CmsRequestContext context, CmsResource resource, CmsUUID principal)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_CONTROL, true, CmsResourceFilter.ALL);
            m_driverManager.removeAccessControlEntry(dbc, resource, principal);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_REMOVE_ACL_ENTRY_2,
                context.getSitePath(resource),
                principal.toString()), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Removes a resource from the given organizational unit.<p>
     * 
     * @param context the current request context
     * @param orgUnit the organizational unit to remove the resource from
     * @param resourceName the root path of the resource that is to be removed from the organizational unit
     * 
     * @throws CmsException if something goes wrong
     * 
     * @see org.opencms.security.CmsOrgUnitManager#addResourceToOrgUnit(CmsObject, String, String)
     * @see org.opencms.security.CmsOrgUnitManager#addResourceToOrgUnit(CmsObject, String, String)
     */
    public void removeResourceFromOrgUnit(CmsRequestContext context, CmsOrganizationalUnit orgUnit, String resourceName)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkRoleForOrgUnit(dbc, CmsRole.ADMINISTRATOR, orgUnit.getName());
            checkOfflineProject(dbc);
            m_driverManager.removeResourceFromOrgUnit(dbc, orgUnit, resourceName);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_REMOVE_RESOURCE_FROM_ORGUNIT_2,
                orgUnit.getName(),
                dbc.removeSiteRoot(resourceName)), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Removes a resource from the current project of the user.<p>
     * 
     * @param context the current request context
     * @param resource the resource to apply this operation to
     * @throws CmsException if something goes wrong
     * @throws CmsRoleViolationException if the current user does not have management access to the project.
     * @see org.opencms.file.types.I_CmsResourceType#copyResourceToProject(CmsObject, CmsSecurityManager, CmsResource)
     */
    public void removeResourceFromProject(CmsRequestContext context, CmsResource resource)
    throws CmsException, CmsRoleViolationException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkManagerOfProjectRole(dbc, context.currentProject());

            if (dbc.currentProject().getFlags() != CmsProject.PROJECT_STATE_UNLOCKED) {
                throw new CmsLockException(org.opencms.lock.Messages.get().container(
                    org.opencms.lock.Messages.ERR_RESOURCE_LOCKED_1,
                    dbc.currentProject().getName()));
            }

            m_driverManager.removeResourceFromProject(dbc, resource);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_COPY_RESOURCE_TO_PROJECT_2,
                context.getSitePath(resource),
                context.currentProject().getName()), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Removes a user from a group.<p>
     *
     * @param context the current request context
     * @param username the name of the user that is to be removed from the group
     * @param groupname the name of the group
     * @param readRoles if to read roles or groups
     * 
     * @throws CmsException if operation was not succesful
     * @throws CmsRoleViolationException if the current user does not own the rule {@link CmsRole#ACCOUNT_MANAGER}
     * 
     */
    public void removeUserFromGroup(CmsRequestContext context, String username, String groupname, boolean readRoles)
    throws CmsException, CmsRoleViolationException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkRoleForOrgUnit(dbc, CmsRole.ACCOUNT_MANAGER, getParentOrganizationalUnit(groupname));
            m_driverManager.removeUserFromGroup(dbc, username, groupname, readRoles);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_REMOVE_USER_FROM_GROUP_2, username, groupname), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Replaces the content, type and properties of a resource.<p>
     * 
     * @param context the current request context
     * @param resource the name of the resource to apply this operation to
     * @param type the new type of the resource
     * @param content the new content of the resource
     * @param properties the new properties of the resource
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if the user has insufficient permission for the given resource (write access permission is required).
     * 
     * @see CmsObject#replaceResource(String, int, byte[], List)
     * @see org.opencms.file.types.I_CmsResourceType#replaceResource(CmsObject, CmsSecurityManager, CmsResource, int, byte[], List)
     */
    public void replaceResource(
        CmsRequestContext context,
        CmsResource resource,
        int type,
        byte[] content,
        List properties) throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.ALL);
            m_driverManager.replaceResource(dbc, resource, type, content, properties);
        } catch (Exception e) {
            dbc.report(
                null,
                Messages.get().container(Messages.ERR_REPLACE_RESOURCE_1, context.getSitePath(resource)),
                e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Resets the password for a specified user.<p>
     *
     * @param context the current request context
     * @param username the name of the user
     * @param oldPassword the old password
     * @param newPassword the new password
     * 
     * @throws CmsException if the user data could not be read from the database
     * @throws CmsSecurityException if the specified username and old password could not be verified
     */
    public void resetPassword(CmsRequestContext context, String username, String oldPassword, String newPassword)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            m_driverManager.resetPassword(dbc, username, oldPassword, newPassword);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_RESET_PASSWORD_1, username), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Returns the original path of given resource, that is the online path for the resource.<p>
     *  
     * If it differs from the offline path, the resource has been moved.<p>
     * 
     * @param context the current request context
     * @param resource the resource to get the path for
     * 
     * @return the online path
     * 
     * @throws CmsException if something goes wrong
     * 
     * @see org.opencms.workplace.commons.CmsUndoChanges#resourceOriginalPath(CmsObject, String)
     */
    public String resourceOriginalPath(CmsRequestContext context, CmsResource resource) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        String result = null;
        try {
            checkOfflineProject(dbc);
            result = m_driverManager.getVfsDriver().readResource(
                dbc,
                CmsProject.ONLINE_PROJECT_ID,
                resource.getStructureId(),
                true).getRootPath();
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_TEST_MOVED_RESOURCE_1,
                dbc.removeSiteRoot(resource.getRootPath())), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Restores a file in the current project with a version from the backup archive.<p>
     * 
     * @param context the current request context
     * @param resource the resource to restore from the archive
     * @param tag the tag (version) id to resource form the archive
     * 
     * @throws CmsException if something goes wrong
     * 
     * @see CmsObject#restoreResourceBackup(String, int)
     * @see org.opencms.file.types.I_CmsResourceType#restoreResourceBackup(CmsObject, CmsSecurityManager, CmsResource, int)
     * @throws CmsSecurityException if the user has insufficient permission for the given resource (write access permission is required).
     */
    public void restoreResource(CmsRequestContext context, CmsResource resource, int tag)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.ALL);
            m_driverManager.restoreResource(dbc, resource, tag);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_RESTORE_RESOURCE_2,
                context.getSitePath(resource),
                new Integer(tag)), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Changes the "expire" date of a resource.<p>
     * 
     * @param context the current request context
     * @param resource the resource to touch
     * @param dateExpired the new expire date of the changed resource
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if the user has insufficient permission for the given resource (write access permission is required).
     * 
     * @see CmsObject#setDateExpired(String, long, boolean)
     * @see org.opencms.file.types.I_CmsResourceType#setDateExpired(CmsObject, CmsSecurityManager, CmsResource, long, boolean)
     */
    public void setDateExpired(CmsRequestContext context, CmsResource resource, long dateExpired)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.IGNORE_EXPIRATION);
            m_driverManager.setDateExpired(dbc, resource, dateExpired);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_SET_DATE_EXPIRED_2,
                new Object[] {new Date(dateExpired), context.getSitePath(resource)}), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Changes the "last modified" timestamp of a resource.<p>
     * 
     * @param context the current request context
     * @param resource the resource to touch
     * @param dateLastModified timestamp the new timestamp of the changed resource
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if the user has insufficient permission for the given resource (write access permission is required).
     * 
     * @see CmsObject#setDateLastModified(String, long, boolean)
     * @see org.opencms.file.types.I_CmsResourceType#setDateLastModified(CmsObject, CmsSecurityManager, CmsResource, long, boolean)
     */
    public void setDateLastModified(CmsRequestContext context, CmsResource resource, long dateLastModified)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.IGNORE_EXPIRATION);
            m_driverManager.setDateLastModified(dbc, resource, dateLastModified);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_SET_DATE_LAST_MODIFIED_2,
                new Object[] {new Date(dateLastModified), context.getSitePath(resource)}), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Changes the "release" date of a resource.<p>
     * 
     * @param context the current request context
     * @param resource the resource to touch
     * @param dateReleased the new release date of the changed resource
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if the user has insufficient permission for the given resource (write access permission is required).
     * 
     * @see CmsObject#setDateReleased(String, long, boolean)
     * @see org.opencms.file.types.I_CmsResourceType#setDateReleased(CmsObject, CmsSecurityManager, CmsResource, long, boolean)
     */
    public void setDateReleased(CmsRequestContext context, CmsResource resource, long dateReleased)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.IGNORE_EXPIRATION);
            m_driverManager.setDateReleased(dbc, resource, dateReleased);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_SET_DATE_RELEASED_2,
                new Object[] {new Date(dateReleased), context.getSitePath(resource)}), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Sets a new parent-group for an already existing group.<p>
     *
     * @param context the current request context
     * @param groupName the name of the group that should be written
     * @param parentGroupName the name of the parent group to set, 
     *                      or <code>null</code> if the parent
     *                      group should be deleted.
     * 
     * @throws CmsException if operation was not succesful
     * @throws CmsRoleViolationException if the current user does not own the rule {@link CmsRole#ACCOUNT_MANAGER}
     * 
     */
    public void setParentGroup(CmsRequestContext context, String groupName, String parentGroupName)
    throws CmsException, CmsRoleViolationException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);

        try {
            checkRoleForOrgUnit(dbc, CmsRole.ACCOUNT_MANAGER, getParentOrganizationalUnit(groupName));
            m_driverManager.setParentGroup(dbc, groupName, parentGroupName);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_SET_PARENT_GROUP_2, parentGroupName, groupName), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Sets the password for a user.<p>
     *
     * @param context the current request context
     * @param username the name of the user
     * @param newPassword the new password
     * 
     * @throws CmsException if operation was not succesfull
     * @throws CmsRoleViolationException if the current user does not own the rule {@link CmsRole#ACCOUNT_MANAGER}
     */
    public void setPassword(CmsRequestContext context, String username, String newPassword)
    throws CmsException, CmsRoleViolationException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkRoleForOrgUnit(dbc, CmsRole.ACCOUNT_MANAGER, getParentOrganizationalUnit(username));
            m_driverManager.setPassword(dbc, username, newPassword);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_SET_PASSWORD_1, username), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Moves an user to the given organizational unit.<p>
     * 
     * @param context the current request context
     * @param orgUnit the organizational unit to add the principal to
     * @param user the user that is to be move to the organizational unit
     * 
     * @throws CmsException if something goes wrong
     * 
     * @see org.opencms.security.CmsOrgUnitManager#setUsersOrganizationalUnit(CmsObject, String, String)
     */
    public void setUsersOrganizationalUnit(CmsRequestContext context, CmsOrganizationalUnit orgUnit, CmsUser user)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkRoleForOrgUnit(dbc, CmsRole.ADMINISTRATOR, orgUnit.getName());
            checkOfflineProject(dbc);
            m_driverManager.setUsersOrganizationalUnit(dbc, orgUnit, user);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_SET_USERS_ORGUNIT_2,
                orgUnit.getName(),
                user.getName()), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Undelete the resource by resetting it's state.<p>
     * 
     * @param context the current request context
     * @param resource the name of the resource to apply this operation to
     * 
     * @throws CmsException if something goes wrong
     * 
     * @see CmsObject#undeleteResource(String, boolean)
     * @see org.opencms.file.types.I_CmsResourceType#undelete(CmsObject, CmsSecurityManager, CmsResource, boolean)
     */
    public void undelete(CmsRequestContext context, CmsResource resource) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.ALL);
            checkSystemLocks(dbc, resource);

            m_driverManager.undelete(dbc, resource);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_UNDELETE_FOR_RESOURCE_1,
                context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Undos all changes in the resource by restoring the version from the 
     * online project to the current offline project.<p>
     * 
     * @param context the current request context
     * @param resource the name of the resource to apply this operation to
     * @param mode the undo mode, one of the <code>{@link CmsResource}#UNDO_XXX</code> constants
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if the user has insufficient permission for the given resource (write access permission is required).
     * 
     * @see CmsObject#undoChanges(String, CmsResourceUndoMode)
     * @see org.opencms.file.types.I_CmsResourceType#undoChanges(CmsObject, CmsSecurityManager, CmsResource, CmsResourceUndoMode)
     */
    public void undoChanges(CmsRequestContext context, CmsResource resource, CmsResourceUndoMode mode)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.ALL);
            checkSystemLocks(dbc, resource);

            m_driverManager.undoChanges(dbc, resource, mode);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_UNDO_CHANGES_FOR_RESOURCE_1,
                context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Unlocks all resources in this project.<p>
     *
     * @param context the current request context
     * @param projectId the id of the project to be published
     * @param removeWfLocks if the workflow lock should be removed too
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsRoleViolationException if the current user does not own the rule {@link CmsRole#PROJECT_MANAGER} for the current project. 
     */
    public void unlockProject(CmsRequestContext context, int projectId, boolean removeWfLocks)
    throws CmsException, CmsRoleViolationException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsProject project = m_driverManager.readProject(dbc, projectId);

        try {
            checkManagerOfProjectRole(dbc, project);
            m_driverManager.unlockProject(project, removeWfLocks);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_UNLOCK_PROJECT_2,
                new Integer(projectId),
                dbc.currentUser().getName()), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Unlocks a resource.<p>
     * 
     * @param context the current request context
     * @param resource the resource to unlock
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if the user has insufficient permission for the given resource (write access permission is required).
     * 
     * @see CmsObject#unlockResource(String)
     * @see org.opencms.file.types.I_CmsResourceType#unlockResource(CmsObject, CmsSecurityManager, CmsResource)
     */
    public void unlockResource(CmsRequestContext context, CmsResource resource)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.ALL);
            m_driverManager.unlockResource(dbc, resource, false, false);
        } catch (CmsException e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_UNLOCK_RESOURCE_3,
                context.getSitePath(resource),
                dbc.currentUser().getName(),
                e.getLocalizedMessage(dbc.getRequestContext().getLocale())), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Updates/Creates the relations for the given resource.<p>
     * 
     * @param context the current user context
     * @param resource the resource to update the relations for
     * @param relations the relations to update
     * 
     * @throws CmsException if something goes wrong 
     * 
     * @see CmsDriverManager#updateRelationsForResource(CmsDbContext, CmsResource, List)
     */
    public void updateRelationsForResource(CmsRequestContext context, CmsResource resource, List relations)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            m_driverManager.updateRelationsForResource(dbc, resource, relations);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_UPDATE_RELATIONS_1,
                dbc.removeSiteRoot(resource.getRootPath())), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Tests if a user is member of the given group.<p>
     *
     * @param context the current request context
     * @param username the name of the user to check
     * @param groupname the name of the group to check
     * 
     * @return <code>true</code>, if the user is in the group; or <code>false</code> otherwise
     *
     * @throws CmsException if operation was not succesful
     */
    public boolean userInGroup(CmsRequestContext context, String username, String groupname) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        boolean result = false;
        try {
            result = m_driverManager.userInGroup(dbc, username, groupname, false);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_USER_IN_GROUP_2, username, groupname), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * This method checks if a new password follows the rules for
     * new passwords, which are defined by a Class implementing the 
     * <code>{@link org.opencms.security.I_CmsPasswordHandler}</code> 
     * interface and configured in the opencms.properties file.<p>
     * 
     * If this method throws no exception the password is valid.<p>
     *
     * @param password the new password that has to be checked
     * 
     * @throws CmsSecurityException if the password is not valid
     */
    public void validatePassword(String password) throws CmsSecurityException {

        m_driverManager.validatePassword(password);
    }

    /**
     * Validates the relations for the given resources.<p>
     * 
     * @param context the current request context
     * @param resources the resources to validate during publishing 
     *              or <code>null</code> for all in current project
     * @param report a report to write the messages to
     * 
     * @return a map with lists of invalid links 
     *          (<code>{@link org.opencms.relations.CmsRelation}}</code> objects) 
     *          keyed by resource names
     * 
     * @throws Exception if something goes wrong
     */
    public Map validateRelations(CmsRequestContext context, List resources, I_CmsReport report) throws Exception {

        Map result = null;
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            result = m_driverManager.validateRelations(dbc, resources, report);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_VALIDATE_RELATIONS_0), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Writes an access control entries to a given resource.<p>
     * 
     * @param context the current request context
     * @param resource the resource
     * @param ace the entry to write
     * 
     * @throws CmsSecurityException if the user has insufficient permission for the given resource ({@link CmsPermissionSet#ACCESS_CONTROL} required).
     * @throws CmsException if something goes wrong
     */
    public void writeAccessControlEntry(CmsRequestContext context, CmsResource resource, CmsAccessControlEntry ace)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_CONTROL, true, CmsResourceFilter.ALL);
            m_driverManager.writeAccessControlEntry(dbc, resource, ace);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_WRITE_ACL_ENTRY_1, context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Writes a resource to the OpenCms VFS, including it's content.<p>
     * 
     * Applies only to resources of type <code>{@link CmsFile}</code>
     * i.e. resources that have a binary content attached.<p>
     * 
     * Certain resource types might apply content validation or transformation rules 
     * before the resource is actually written to the VFS. The returned result
     * might therefore be a modified version from the provided original.<p>
     * 
     * @param context the current request context
     * @param resource the resource to apply this operation to
     * 
     * @return the written resource (may have been modified)
     *
     * @throws CmsSecurityException if the user has insufficient permission for the given resource ({@link CmsPermissionSet#ACCESS_WRITE} required).
     * @throws CmsException if something goes wrong
     * 
     * @see CmsObject#writeFile(CmsFile)
     * @see org.opencms.file.types.I_CmsResourceType#writeFile(CmsObject, CmsSecurityManager, CmsFile)
     */
    public CmsFile writeFile(CmsRequestContext context, CmsFile resource) throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        CmsFile result = null;
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.ALL);
            result = m_driverManager.writeFile(dbc, resource);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_WRITE_FILE_1, context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
        return result;
    }

    /**
     * Writes an already existing group.<p>
     *
     * The group id has to be a valid OpenCms group id.<br>
     * 
     * The group with the given id will be completely overriden
     * by the given data.<p>
     *
     * @param context the current request context
     * @param group the group that should be written
     *
     * @throws CmsRoleViolationException if the current user does not own the role {@link CmsRole#ACCOUNT_MANAGER} for the current project. 
     * @throws CmsException if operation was not succesfull
     */
    public void writeGroup(CmsRequestContext context, CmsGroup group) throws CmsException, CmsRoleViolationException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkRoleForOrgUnit(dbc, CmsRole.ACCOUNT_MANAGER, getParentOrganizationalUnit(group.getName()));
            m_driverManager.writeGroup(dbc, group);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_WRITE_GROUP_1, group.getName()), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Writes the locks that are currently stored in-memory to the database to allow restoring them in 
     * later startups.<p> 
     * 
     * This overwrites the locks previously stored in the underlying database table.<p>
     * 
     * @throws CmsException if something goes wrong 
     */
    public void writeLocks() throws CmsException {

        if (m_dbContextFactory == null) {
            // already shutdown
            return;
        }
        CmsDbContext dbc = m_dbContextFactory.getDbContext();
        try {
            m_driverManager.writeLocks(dbc);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Writes an already existing organizational unit.<p>
     *
     * The organizational unit id has to be a valid OpenCms organizational unit id.<p>
     * 
     * The organizational unit with the given id will be completely overriden
     * by the given data.<p>
     *
     * @param context the current request context
     * @param organizationalUnit the organizational unit that should be written
     * 
     * @throws CmsException if operation was not successful
     * 
     * @see org.opencms.security.CmsOrgUnitManager#writeOrganizationalUnit(CmsObject, CmsOrganizationalUnit)
     */
    public void writeOrganizationalUnit(CmsRequestContext context, CmsOrganizationalUnit organizationalUnit)
    throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkRoleForOrgUnit(dbc, CmsRole.ADMINISTRATOR, organizationalUnit.getName());
            checkOfflineProject(dbc);
            m_driverManager.writeOrganizationalUnit(dbc, organizationalUnit);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_WRITE_ORGUNIT_1, organizationalUnit.getName()), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Writes an already existing project.<p>
     *
     * The project id has to be a valid OpenCms project id.<br>
     * 
     * The project with the given id will be completely overriden
     * by the given data.<p>
     *
     * @param project the project that should be written
     * @param context the current request context
     * 
     * @throws CmsRoleViolationException if the current user does not own the role {@link CmsRole#PROJECT_MANAGER} for the current project. 
     * @throws CmsException if operation was not successful
     */
    public void writeProject(CmsRequestContext context, CmsProject project)
    throws CmsRoleViolationException, CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkRoleForOrgUnit(dbc, CmsRole.PROJECT_MANAGER, null);
            m_driverManager.writeProject(dbc, project);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_WRITE_PROJECT_1, project.getName()), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Writes a property for a specified resource.<p>
     * 
     * @param context the current request context
     * @param resource the resource to write the property for
     * @param property the property to write
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if the user has insufficient permission for the given resource ({@link CmsPermissionSet#ACCESS_WRITE} required).
     * 
     * @see CmsObject#writePropertyObject(String, CmsProperty)
     * @see org.opencms.file.types.I_CmsResourceType#writePropertyObject(CmsObject, CmsSecurityManager, CmsResource, CmsProperty)
     */
    public void writePropertyObject(CmsRequestContext context, CmsResource resource, CmsProperty property)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.IGNORE_EXPIRATION);
            m_driverManager.writePropertyObject(dbc, resource, property);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_WRITE_PROP_2,
                property.getName(),
                context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Writes a list of properties for a specified resource.<p>
     * 
     * Code calling this method has to ensure that the no properties 
     * <code>a, b</code> are contained in the specified list so that <code>a.equals(b)</code>, 
     * otherwise an exception is thrown.<p>
     * 
     * @param context the current request context
     * @param resource the resource to write the properties for
     * @param properties the list of properties to write
     * 
     * @throws CmsException if something goes wrong
     * @throws CmsSecurityException if the user has insufficient permission for the given resource ({@link CmsPermissionSet#ACCESS_WRITE} required).
     * 
     * @see CmsObject#writePropertyObjects(String, List)
     * @see org.opencms.file.types.I_CmsResourceType#writePropertyObjects(CmsObject, CmsSecurityManager, CmsResource, List)
     */
    public void writePropertyObjects(CmsRequestContext context, CmsResource resource, List properties)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.IGNORE_EXPIRATION);
            // write the properties
            m_driverManager.writePropertyObjects(dbc, resource, properties, true);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_WRITE_PROPS_1, context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Writes a resource to the OpenCms VFS.<p>
     * 
     * @param context the current request context
     * @param resource the resource to write
     *
     * @throws CmsSecurityException if the user has insufficient permission for the given resource ({@link CmsPermissionSet#ACCESS_WRITE} required).
     * @throws CmsException if something goes wrong
     */
    public void writeResource(CmsRequestContext context, CmsResource resource)
    throws CmsException, CmsSecurityException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkOfflineProject(dbc);
            checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.ALL);
            m_driverManager.writeResource(dbc, resource);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_WRITE_RESOURCE_1, context.getSitePath(resource)), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Inserts an entry in the published resource table.<p>
     * 
     * This is done during static export.<p>
     * 
     * @param context the current request context
     * @param resourceName The name of the resource to be added to the static export
     * @param linkType the type of resource exported (0= non-paramter, 1=parameter)
     * @param linkParameter the parameters added to the resource
     * @param timestamp a timestamp for writing the data into the db
     * 
     * @throws CmsException if something goes wrong
     */
    public void writeStaticExportPublishedResource(
        CmsRequestContext context,
        String resourceName,
        int linkType,
        String linkParameter,
        long timestamp) throws CmsException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            m_driverManager.writeStaticExportPublishedResource(dbc, resourceName, linkType, linkParameter, timestamp);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(
                Messages.ERR_WRITE_STATEXP_PUBLISHED_RESOURCES_3,
                resourceName,
                linkParameter,
                new Date(timestamp)), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Updates the user information. <p>
     * 
     * The user id has to be a valid OpenCms user id.<br>
     * 
     * The user with the given id will be completely overriden
     * by the given data.<p>
     *
     * @param context the current request context
     * @param user the user to be updated
     *
     * @throws CmsRoleViolationException if the current user does not own the rule {@link CmsRole#ACCOUNT_MANAGER} for the current project. 
     * @throws CmsException if operation was not succesful
     */
    public void writeUser(CmsRequestContext context, CmsUser user) throws CmsException, CmsRoleViolationException {

        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            if (!context.currentUser().equals(user)) {
                // a user is allowed to write his own data (e.g. for "change preferences")
                checkRoleForOrgUnit(dbc, CmsRole.ACCOUNT_MANAGER, getParentOrganizationalUnit(user.getName()));
            }
            m_driverManager.writeUser(dbc, user);
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_WRITE_USER_1, user.getName()), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Performs a blocking permission check on a resource.<p>
     *
     * If the required permissions are not satisfied by the permissions the user has on the resource,
     * an exception is thrown.<p>
     * 
     * @param dbc the current database context
     * @param resource the resource on which permissions are required
     * @param requiredPermissions the set of permissions required to access the resource
     * @param checkLock if true, the lock status of the resource is also checked 
     * @param filter the filter for the resource
     * 
     * @throws CmsException in case of any i/o error
     * @throws CmsSecurityException if the required permissions are not satisfied
     * 
     * @see #hasPermissions(CmsRequestContext, CmsResource, CmsPermissionSet, boolean, CmsResourceFilter)
     */
    protected void checkPermissions(
        CmsDbContext dbc,
        CmsResource resource,
        CmsPermissionSet requiredPermissions,
        boolean checkLock,
        CmsResourceFilter filter) throws CmsException, CmsSecurityException {

        // get the permissions
        int permissions = hasPermissions(dbc, resource, requiredPermissions, checkLock, filter);
        if (permissions != 0) {
            checkPermissions(dbc.getRequestContext(), resource, requiredPermissions, permissions);
        }
    }

    /**
     * Applies the permission check result of a previous call 
     * to {@link #hasPermissions(CmsRequestContext, CmsResource, CmsPermissionSet, boolean, CmsResourceFilter)}.<p>
     * 
     * @param context the current request context
     * @param resource the resource on which permissions are required
     * @param requiredPermissions the set of permissions required to access the resource
     * @param permissions the permissions to check
     * 
     * @throws CmsSecurityException if the required permissions are not satisfied
     * @throws CmsLockException if the lock status is not as required
     * @throws CmsVfsResourceNotFoundException if the required resource has been filtered
     */
    protected void checkPermissions(
        CmsRequestContext context,
        CmsResource resource,
        CmsPermissionSet requiredPermissions,
        int permissions) throws CmsSecurityException, CmsLockException, CmsVfsResourceNotFoundException {

        switch (permissions) {
            case PERM_FILTERED:
                throw new CmsVfsResourceNotFoundException(Messages.get().container(
                    Messages.ERR_PERM_FILTERED_1,
                    context.getSitePath(resource)));

            case PERM_DENIED:
                throw new CmsPermissionViolationException(Messages.get().container(
                    Messages.ERR_PERM_DENIED_2,
                    context.getSitePath(resource),
                    requiredPermissions.getPermissionString()));

            case PERM_NOTLOCKED:
                throw new CmsLockException(Messages.get().container(
                    Messages.ERR_PERM_NOTLOCKED_2,
                    context.getSitePath(resource),
                    context.currentUser().getName()));

            case PERM_ALLOWED:
            default:
                return;
        }
    }

    /**
     * Checks if the given resource contains a resource that has a system lock.<p>
     * 
     * @param dbc the current database context
     * @param resource the resource to check
     * 
     * @throws CmsException in case there is a system lock contained in the given resouce
     */
    protected void checkSystemLocks(CmsDbContext dbc, CmsResource resource) throws CmsException {

        if (m_lockManager.hasSystemLocks(dbc, resource)) {
            throw new CmsLockException(Messages.get().container(
                Messages.ERR_RESOURCE_LOCKED_IN_WORKFLOW_1,
                dbc.removeSiteRoot(resource.getRootPath())));
        }
    }

    /**
     * Clears the permission cache.<p>
     */
    protected void clearPermissionCache() {

        m_permissionCache.clear();
    }

    /**
     * @see java.lang.Object#finalize()
     */
    protected void finalize() throws Throwable {

        destroy();
        super.finalize();
    }

    /**
     * Performs a non-blocking permission check on a resource.<p>
     * 
     * This test will not throw an exception in case the required permissions are not
     * available for the requested operation. Instead, it will return one of the 
     * following values:<ul>
     * <li><code>{@link #PERM_ALLOWED}</code></li>
     * <li><code>{@link #PERM_FILTERED}</code></li>
     * <li><code>{@link #PERM_DENIED}</code></li></ul><p>
     * 
     * @param dbc the current database context
     * @param resource the resource on which permissions are required
     * @param requiredPermissions the set of permissions required for the operation
     * @param checkLock if true, a lock for the current user is required for 
     *      all write operations, if false it's ok to write as long as the resource
     *      is not locked by another user
     * @param filter the resource filter to use
     * 
     * @return <code>PERM_ALLOWED</code> if the user has sufficient permissions on the resource
     *      for the requested operation
     * 
     * @throws CmsException in case of i/o errors (NOT because of insufficient permissions)
     */
    protected int hasPermissions(
        CmsDbContext dbc,
        CmsResource resource,
        CmsPermissionSet requiredPermissions,
        boolean checkLock,
        CmsResourceFilter filter) throws CmsException {

        // check if the resource is valid according to the current filter
        // if not, throw a CmsResourceNotFoundException
        if (!filter.isValid(dbc.getRequestContext(), resource)) {
            return PERM_FILTERED;
        }

        // checking the filter is less cost intensive then checking the cache,
        // this is why basic filter results are not cached
        String cacheKey = m_keyGenerator.getCacheKeyForUserPermissions(
            filter.requireVisible() && checkLock ? "11" : (!filter.requireVisible() && checkLock ? "01"
            : (filter.requireVisible() && !checkLock ? "10" : "00")),
            dbc,
            resource,
            requiredPermissions);
        Integer cacheResult = (Integer)m_permissionCache.get(cacheKey);
        if (cacheResult != null) {
            return cacheResult.intValue();
        }

        int denied = 0;

        // if this is the onlineproject, write is rejected 
        if (dbc.currentProject().isOnlineProject()) {
            denied |= CmsPermissionSet.PERMISSION_WRITE;
        }

        // check if the current user is admin
        boolean canIgnorePermissions = hasRoleForResource(dbc, dbc.currentUser(), CmsRole.VFS_MANAGER, resource);

        // check lock status 
        boolean writeRequired = requiredPermissions.requiresWritePermission()
            || requiredPermissions.requiresControlPermission();

        // if the resource type is jsp
        // write is only allowed for administrators
        if (writeRequired && !canIgnorePermissions && (resource.getTypeId() == CmsResourceTypeJsp.getStaticTypeId())) {
            if (!hasRoleForResource(dbc, dbc.currentUser(), CmsRole.DEVELOPER, resource)) {
                denied |= CmsPermissionSet.PERMISSION_WRITE;
                denied |= CmsPermissionSet.PERMISSION_CONTROL;
            }
        }

        if (writeRequired && checkLock) {
            // check lock state only if required
            CmsLock lock = m_lockManager.getLock(dbc, resource, true);
            // if the resource is not locked by the current user, write and control 
            // access must cause a permission error that must not be cached
            if (lock.isUnlocked() || !lock.isLockableBy(dbc.currentUser())) {
                return PERM_NOTLOCKED;
            }
        }

        CmsPermissionSetCustom permissions;
        if (canIgnorePermissions) {
            // if the current user is administrator, anything is allowed
            permissions = new CmsPermissionSetCustom(~0);
        } else {
            // otherwise, get the permissions from the access control list
            permissions = m_driverManager.getPermissions(dbc, resource, dbc.currentUser());
        }

        // revoke the denied permissions
        permissions.denyPermissions(denied);

        if ((permissions.getPermissions() & CmsPermissionSet.PERMISSION_VIEW) == 0) {
            // resource "invisible" flag is set for this user
            if (filter.requireVisible()) {
                // filter requires visible permission - extend required permission set
                requiredPermissions = new CmsPermissionSet(requiredPermissions.getAllowedPermissions()
                    | CmsPermissionSet.PERMISSION_VIEW, requiredPermissions.getDeniedPermissions());
            } else {
                // view permissions can be ignored by filter
                permissions.setPermissions(
                // modify permissions so that view is allowed
                    permissions.getAllowedPermissions() | CmsPermissionSet.PERMISSION_VIEW,
                    permissions.getDeniedPermissions() & ~CmsPermissionSet.PERMISSION_VIEW);
            }
        }

        Integer result;
        if ((requiredPermissions.getPermissions() & (permissions.getPermissions())) == requiredPermissions.getPermissions()) {
            result = PERM_ALLOWED_INTEGER;
        } else {
            result = PERM_DENIED_INTEGER;
            if (LOG.isDebugEnabled()) {
                LOG.debug(Messages.get().getBundle().key(
                    Messages.LOG_NO_PERMISSION_RESOURCE_USER_4,
                    new Object[] {
                        dbc.getRequestContext().removeSiteRoot(resource.getRootPath()),
                        dbc.currentUser().getName(),
                        requiredPermissions.getPermissionString(),
                        permissions.getPermissionString()}));
            }
        }
        m_permissionCache.put(cacheKey, result);

        return result.intValue();
    }

    /**
     * Reads a folder from the VFS, using the specified resource filter.<p>
     * 
     * @param dbc the current database context
     * @param resourcename the name of the folder to read (full path)
     * @param filter the resource filter to use while reading
     *
     * @return the folder that was read
     *
     * @throws CmsException if something goes wrong
     */
    protected CmsFolder readFolder(CmsDbContext dbc, String resourcename, CmsResourceFilter filter) throws CmsException {

        CmsResource resource = readResource(dbc, resourcename, filter);
        return m_driverManager.convertResourceToFolder(resource);
    }

    /**
     * Reads a resource from the OpenCms VFS, using the specified resource filter.<p>
     * 
     * @param dbc the current database context
     * @param structureID the ID of the structure to read
     * @param filter the resource filter to use while reading
     *
     * @return the resource that was read
     *
     * @throws CmsException if something goes wrong
     * 
     * @see CmsObject#readResource(CmsUUID, CmsResourceFilter)
     * @see CmsObject#readResource(CmsUUID)
     * @see CmsFile#upgrade(CmsResource, CmsObject)
     */
    protected CmsResource readResource(CmsDbContext dbc, CmsUUID structureID, CmsResourceFilter filter)
    throws CmsException {

        // read the resource from the VFS
        CmsResource resource = m_driverManager.readResource(dbc, structureID, filter);

        // check if the user has read access to the resource
        checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_READ, true, filter);

        // access was granted - return the resource
        return resource;
    }

    /**
     * Reads a resource from the OpenCms VFS, using the specified resource filter.<p>
     * 
     * @param dbc the current database context
     * @param resourcePath the name of the resource to read (full path)
     * @param filter the resource filter to use while reading
     *
     * @return the resource that was read
     *
     * @throws CmsException if something goes wrong
     * 
     * @see CmsObject#readResource(String, CmsResourceFilter)
     * @see CmsObject#readResource(String)
     * @see CmsFile#upgrade(CmsResource, CmsObject)
     */
    protected CmsResource readResource(CmsDbContext dbc, String resourcePath, CmsResourceFilter filter)
    throws CmsException {

        // read the resource from the VFS
        CmsResource resource = m_driverManager.readResource(dbc, resourcePath, filter);

        // check if the user has read access to the resource
        checkPermissions(dbc, resource, CmsPermissionSet.ACCESS_READ, true, filter);

        // access was granted - return the resource
        return resource;
    }

    /**
     * Internal recursive method for deleting a resource.<p>
     * 
     * @param dbc the db context
     * @param resource the name of the resource to delete (full path)
     * @param siblingMode indicates how to handle siblings of the deleted resource
     * 
     * @throws CmsException if something goes wrong
     */
    private void deleteResource(CmsDbContext dbc, CmsResource resource, CmsResourceDeleteMode siblingMode)
    throws CmsException {

        if (resource.isFolder()) {
            // collect all resources in the folder (but exclude deleted ones)
            List resources = m_driverManager.readChildResources(
                dbc,
                resource,
                CmsResourceFilter.IGNORE_EXPIRATION,
                true,
                true,
                false);

            Set deletedResources = new HashSet();
            // now walk through all sub-resources in the folder
            for (int i = 0; i < resources.size(); i++) {
                CmsResource childResource = (CmsResource)resources.get(i);
                if ((siblingMode == CmsResource.DELETE_REMOVE_SIBLINGS)
                    && deletedResources.contains(childResource.getResourceId())) {
                    // sibling mode is "delete all siblings" and another sibling of the current child resource has already
                    // been deleted- do nothing and continue with the next child resource.
                    continue;
                }
                if (childResource.isFolder()) {
                    // recurse into this method for subfolders
                    deleteResource(dbc, childResource, siblingMode);
                } else {
                    // handle child resources
                    m_driverManager.deleteResource(dbc, childResource, siblingMode);
                }
                deletedResources.add(childResource.getResourceId());
            }
            deletedResources.clear();
        }
        // handle the resource itself
        m_driverManager.deleteResource(dbc, resource, siblingMode);
    }

    /**
     * Deletes a user, where all permissions and resources attributes of the user
     * were transfered to a replacement user, if given.<p>
     *
     * @param context the current request context
     * @param user the user to be deleted
     * @param replacement the user to be transfered, can be <code>null</code>
     * 
     * @throws CmsRoleViolationException if the current user does not own the rule {@link CmsRole#ACCOUNT_MANAGER}
     * @throws CmsSecurityException in case the user is a default user 
     * @throws CmsException if something goes wrong
     */
    private void deleteUser(CmsRequestContext context, CmsUser user, CmsUser replacement)
    throws CmsException, CmsSecurityException, CmsRoleViolationException {

        if (OpenCms.getDefaultUsers().isDefaultUser(user.getName())) {
            throw new CmsSecurityException(org.opencms.security.Messages.get().container(
                org.opencms.security.Messages.ERR_CANT_DELETE_DEFAULT_USER_1,
                user.getName()));
        }
        if (context.currentUser().equals(user)) {
            throw new CmsSecurityException(Messages.get().container(Messages.ERR_USER_CANT_DELETE_ITSELF_USER_0));
        }
        CmsDbContext dbc = m_dbContextFactory.getDbContext(context);
        try {
            checkRoleForOrgUnit(dbc, CmsRole.ACCOUNT_MANAGER, getParentOrganizationalUnit(user.getName()));
            // this is needed because 
            // I_CmsUserDriver#removeAccessControlEntriesForPrincipal(CmsDbContext, CmsProject, CmsProject, CmsUUID)
            // expects an offline project, if not data will become inconsistent
            checkOfflineProject(dbc);
            if (replacement == null) {
                m_driverManager.deleteUser(dbc, context.currentProject(), user.getName(), null);
            } else {
                m_driverManager.deleteUser(dbc, context.currentProject(), user.getName(), replacement.getName());
            }
        } catch (Exception e) {
            dbc.report(null, Messages.get().container(Messages.ERR_DELETE_USER_1, user.getName()), e);
        } finally {
            dbc.clear();
        }
    }

    /**
     * Returns the organizational unit for the parent of the given fully qualified name.<p>
     * 
     * @param fqn the fully qualified name to get the parent organizational unit for
     * 
     * @return the parent organizational unit for the fully qualified name
     */
    private String getParentOrganizationalUnit(String fqn) {

        String ouFqn = CmsOrganizationalUnit.getParentFqn(fqn);
        if (ouFqn == null) {
            ouFqn = CmsOrganizationalUnit.SEPARATOR;
        }
        return ouFqn;
    }

    /**
     * Returns <code>true</code> if at least one of the given group names is equal to a group name
     * of the given role in the given organizational unit.<p>
     * 
     * This checks the given list against the group of the given role as well as against the role group 
     * of all parent roles.<p>
     * 
     * If the organizational unit is <code>null</code>, this method will check if the
     * given user has the given role for at least one organizational unit.<p>
     *  
     * @param role the role to check
     * @param roles the groups to match the role groups against
     * @param orgUnitFqn the organizational unit to check the role for
     * 
     * @return <code>true</code> if at last one of the given group names is equal to a group name
     *      of this role
     */
    private boolean hasRole(CmsRole role, List roles, String orgUnitFqn) {

        // iterates the roles the user are in
        Iterator itGroups = roles.iterator();
        while (itGroups.hasNext()) {
            String groupName = ((CmsGroup)itGroups.next()).getName();
            // iterate the role hierarchie
            Iterator itDistinctGroupNames = role.getDistinctGroupNames().iterator();
            while (itDistinctGroupNames.hasNext()) {
                String distictGroupName = (String)itDistinctGroupNames.next();
                if (distictGroupName.startsWith(CmsOrganizationalUnit.SEPARATOR)) {
                    // this is a ou independent role 
                    // we need an exact match, and we ignore the ou param
                    if (groupName.equals(distictGroupName.substring(1)) || groupName.equals(distictGroupName)) {
                        return true;
                    }
                } else {
                    // first check if the user has the role at all
                    if (groupName.endsWith(CmsOrganizationalUnit.SEPARATOR + distictGroupName)
                        || groupName.equals(distictGroupName)) {
                        // this is a ou dependent role
                        if (orgUnitFqn == null) {
                            // ou param is null, so the user needs to have the role in at least one ou does not matter which
                            return true;
                        } else {
                            // the user needs to have the role in the given ou or in a parent ou
                            // now check that the ou matches
                            String groupFqn = CmsOrganizationalUnit.getParentFqn(groupName);
                            if (orgUnitFqn.startsWith(groupFqn)
                                || orgUnitFqn.startsWith(CmsOrganizationalUnit.SEPARATOR + groupFqn)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Internal recursive method to move a resource.<p>
     * 
     * @param dbc the db context
     * @param source the source resource
     * @param destination the destination path
     * 
     * @throws CmsException if something goes wrong
     */
    private void moveResource(CmsDbContext dbc, CmsResource source, String destination) throws CmsException {

        List resources = null;

        if (source.isFolder()) {
            if (!CmsResource.isFolder(destination)) {
                // ensure folder name end's with a /
                destination = destination.concat("/");
            }
            // collect all resources in the folder (including everything)
            resources = m_driverManager.readChildResources(dbc, source, CmsResourceFilter.ALL, true, true, false);
        }

        // target permissions will be checked later
        m_driverManager.moveResource(dbc, source, destination, false, true);

        // make sure lock is set
        CmsResource destinationResource = m_driverManager.readResource(dbc, destination, CmsResourceFilter.ALL);
        try {
            // the destination must always get a new lock
            m_driverManager.lockResource(dbc, destinationResource, CmsLockType.EXCLUSIVE);
        } catch (Exception e) {
            // could happen with workflow (and harder with shared) locks on single files
            if (LOG.isWarnEnabled()) {
                LOG.warn(e);
            }
        }

        if (resources != null) {
            // now walk through all sub-resources in the folder
            for (int i = 0; i < resources.size(); i++) {
                CmsResource childResource = (CmsResource)resources.get(i);
                String childDestination = destination.concat(childResource.getName());
                // recurse with child resource
                moveResource(dbc, childResource, childDestination);
            }
        }
    }
}
