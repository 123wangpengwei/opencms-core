/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/I_CmsUserDriver.java,v $
 * Date   : $Date: 2004/01/14 12:55:58 $
 * Version: $Revision: 1.30 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2003 Alkacon Software (http://www.alkacon.com)
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

package org.opencms.db;

import org.opencms.db.generic.CmsSqlManager;
import org.opencms.security.CmsAccessControlEntry;
import org.opencms.util.CmsUUID;

import com.opencms.core.CmsException;
import com.opencms.file.CmsGroup;
import com.opencms.file.CmsProject;
import com.opencms.file.CmsUser;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Definitions of all required user driver methods.
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @author Michael Emmerich (m.emmerich@alkacon.com)
 * @version $Revision: 1.30 $ $Date: 2004/01/14 12:55:58 $
 * @since 5.1
 */
public interface I_CmsUserDriver extends I_CmsDriver {

    /**
     * Creates an access control entry.<p>
     * 
     * @param project the project to write the entry
     * @param resource the id of the resource
     * @param principal the id of the principal (user or group)
     * @param allowed the bitset of allowed permissions
     * @param denied the bitset of denied permissions
     * @param flags flags
     * @throws CmsException if something goes wrong
     */
    void createAccessControlEntry(CmsProject project, CmsUUID resource, CmsUUID principal, int allowed, int denied, int flags) throws CmsException;

    /**
     * Creates a new group.<p>
     *
     * @param groupId The id of the new group
     * @param groupName The name of the new group
     * @param description The description for the new group
     * @param flags The flags for the new group
     * @param parentGroupName The name of the parent group (or null)
     * @param reservedParam reserved optional parameter, should be null on standard OpenCms installations
     * @return Group the new group
     * @throws CmsException if operation was not succesfull
     */
    CmsGroup createGroup(CmsUUID groupId, String groupName, String description, int flags, String parentGroupName, Object reservedParam) throws CmsException;

    /**
     * Adds a user to the database.<p>
     *
     * @param name username
     * @param password user-password
     * @param description user-description
     * @param firstname user-firstname
     * @param lastname user-lastname
     * @param email user-email
     * @param lastlogin user-lastlogin
     * @param flags user-flags
     * @param additionalInfos user-additional-infos
     * @param defaultGroup user-defaultGroup
     * @param address user-defauladdress
     * @param section user-section
     * @param type user-type
     * @return the created user.
     * @throws CmsException if something goes wrong.
     */
    CmsUser createUser(String name, String password, String description, String firstname, String lastname, String email, long lastlogin, int flags, Hashtable additionalInfos, CmsGroup defaultGroup, String address, String section, int type) throws CmsException;

    /**
     * Adds a user to a group.<p>
     *
     * Only the admin can do this.
     * 
     * @param userid the id of the user that is to be added to the group
     * @param groupid the id of the group
     * @param reservedParam reserved optional parameter, should be null on standard OpenCms installations
     * @throws CmsException if operation was not succesfull
     */
    void createUserInGroup(CmsUUID userid, CmsUUID groupid, Object reservedParam) throws CmsException;

    /**
     * Removes an access control entry from the database.<p>
     * 
     * @param project the project to write the entry
     * @param resource the id of the resource
     * @param principal the id of the principal
     * @throws CmsException if something goes wrong
     */

    /**
     * Deletes all access control entries belonging to a resource.<p>
     * 
     * @param project the project to write the entry
     * @param resource the id of the resource
     * @throws CmsException if something goes wrong
     */
    void deleteAccessControlEntries(CmsProject project, CmsUUID resource) throws CmsException;

    /**
     * Delete a group from the Cms.<p>
     * 
     * Only groups that contain no subgroups can be deleted.
     * Only the admin can do this.
     *
     * @param delgroup the name of the group that is to be deleted
     * @throws CmsException if operation was not succesfull
     */
    void deleteGroup(String delgroup) throws CmsException;

    /**
     * Deletes a user from the database.<p>
     *
     * @param userName the user to delete
     * @throws CmsException if something goes wrong
     */
    void deleteUser(String userName) throws CmsException;

    /**
     * Removes a user from a group.<p>
     *
     * Only the admin can do this.
     * 
     * @param userId the id of the user that is to be added to the group
     * @param groupId the id of the group
     * @throws CmsException if something goes wrong
     */
    void deleteUserInGroup(CmsUUID userId, CmsUUID groupId) throws CmsException;

    /**
     * Destroys this driver.<p>
     * 
     * @throws Throwable if something goes wrong
     * @throws CmsException if something else goes wrong
     */
    void destroy() throws Throwable, CmsException;

    /**
     * Method to encrypt the passwords.<p>
     *
     * @param value The value to encrypt
     * @return the encrypted value
     */
    String encryptPassword(String value);

    /**
     * Adds a user to the database.<p>
     *
     * @param id user id
     * @param name username
     * @param password user-password
     * @param recoveryPassword user-recoveryPassword
     * @param description user-description
     * @param firstname user-firstname
     * @param lastname user-lastname
     * @param email user-email
     * @param lastlogin user-lastlogin
     * @param lastused user-lastused
     * @param flags user-flags
     * @param additionalInfos user-additional-infos
     * @param defaultGroup user-defaultGroup
     * @param address user-defauladdress
     * @param section user-section
     * @param type user-type
     * @param reservedParam reserved optional parameter, should be null on standard OpenCms installations
     * @return the created user.
     * @throws CmsException if something goes wrong
     */
    CmsUser importUser(CmsUUID id, String name, String password, String recoveryPassword, String description, String firstname, String lastname, String email, long lastlogin, long lastused, int flags, Hashtable additionalInfos, CmsGroup defaultGroup, String address, String section, int type, Object reservedParam) throws CmsException;

    /**
     * Initializes the SQL manager for this driver.<p>
     * 
     * To obtain JDBC connections from different pools, further 
     * {online|offline|backup} pool Urls have to be specified.
     * 
     * @return the SQL manager for this driver
     * @see org.opencms.db.generic.CmsSqlManager#setPoolUrlOffline(String)
     * @see org.opencms.db.generic.CmsSqlManager#setPoolUrlOnline(String)
     * @see org.opencms.db.generic.CmsSqlManager#setPoolUrlBackup(String)
     */
    org.opencms.db.generic.CmsSqlManager initQueries();

    /**
     * Publish all access control entries of a resource from the given offline project to the online project.<p>
     * 
     * Within the given project, the resource is identified by its offlineId, in the online project,
     * it is identified by the given onlineId.
     * 
     * @param offlineProject an offline project
     * @param onlineProject the onlie project
     * @param offlineId the offline resource id
     * @param onlineId the online resource id
     * @throws CmsException if something goes wrong
     */
    void publishAccessControlEntries(CmsProject offlineProject, CmsProject onlineProject, CmsUUID offlineId, CmsUUID onlineId) throws CmsException;

    /**
     * Reads all relevant access control entries for a given resource.<p>
     * 
     * @param project the project to write the entry
     * @param resource the id of the resource
     * @param inheritedOnly flag to indicate that only inherited entries should be returned
     * @return a vector of access control entries defining all permissions for the given resource
     * @throws CmsException if something goes wrong
     */
    Vector readAccessControlEntries(CmsProject project, CmsUUID resource, boolean inheritedOnly) throws CmsException;

    /**
     * Reads an access control entry from the cms.<p>
     * 
     * @param project the project to write the entry
     * @param resource the id of the resource
     * @param principal the id of a group or a user any other entity
     * @return an access control entry that defines the permissions of the entity for the given resource
     * @throws CmsException if something goes wrong
     */
    CmsAccessControlEntry readAccessControlEntry(CmsProject project, CmsUUID resource, CmsUUID principal) throws CmsException;

    /**
     * Returns all child groups of a groups.<p>
     *
     *
     * @param groupname the name of the group
     * @return users a Vector of all child groups or null
     * @throws CmsException if operation was not succesful
     */
    Vector readChildGroups(String groupname) throws CmsException;

    /**
     * Returns a group object.<p>
     * 
     * @param groupId the id of the group that is to be read
     * @return the CmsGroup object.
     * @throws CmsException if operation was not successful
     */
    CmsGroup readGroup(CmsUUID groupId) throws CmsException;

    /**
     * Returns a group object.<p>
     * 
     * @param groupName the name of the group
     * @return the group with the given name
     * @throws CmsException if something goes wrong
     */
    CmsGroup readGroup(String groupName) throws CmsException;

    /**
     * Returns all groups.<p>
     *
     * @return a Vector of all existing groups.
     * @throws CmsException if operation was not succesful
     */
    Vector readGroups() throws CmsException;

    /**
     * Returns a list of groups of a user.<p>
     *
     * @param userId the id of the user
     * @param paramStr additional parameter
     * @return vector of groups
     * @throws CmsException if operation was not succesful
     */
    Vector readGroupsOfUser(CmsUUID userId, String paramStr) throws CmsException;

    /**
     * Reads a user from the database.<p>
     *
     * @param id the id of the user
     * @return the user object
     * @throws CmsException if something goes wrong
     */
    CmsUser readUser(CmsUUID id) throws CmsException;

    /**
     * Reads a user from the database.<p>
     *
     * @param name the name of the user
     * @param type the type of the user
     * @return the read user
     * @throws CmsException if something goes wrong
     */
    CmsUser readUser(String name, int type) throws CmsException;

    /**
     * Reads a user from the database, only if the password is correct.<p>
     *
     * @param name the name of the user
     * @param password the password of the user
     * @param type the type of the user
     * @return the read user
     * @throws CmsException if something goes wrong
     */
    CmsUser readUser(String name, String password, int type) throws CmsException;

    /**
     * Reads a user from the database, only if the password is correct.<p>
     *
     * @param name the name of the user
     * @param password the password of the user
     * @param remoteAddress the remote address of the request
     * @param type the type of the user
     * @return the read user
     * @throws CmsException if something goes wrong
     */
    CmsUser readUser(String name, String password, String remoteAddress, int type) throws CmsException;

    /**
     * Gets all users of a type.<p>
     *
     * @param type the type of the user
     * @return list of users of this type
     * @throws CmsException if something goes wrong
     */
    Vector readUsers(int type) throws CmsException;

    /**
     * Gets all users of a type and namefilter.<p>
     *
     * @param type the type of the user
     * @param namefilter the namefilter
     * @return list of users of this type matching the namefilter
     * @throws CmsException if something goes wrong
     */
    Vector readUsers(int type, String namefilter) throws CmsException;

    /**
     * Gets all users with a certain Lastname.<p>
     *
     * @param lastname      the start of the users lastname
     * @param userType      webuser or systemuser
     * @param userStatus    enabled, disabled
     * @param wasLoggedIn   was the user ever locked in?
     * @param nMax          max number of results
     * @return the users
     * @throws CmsException if operation was not successful
     */
    Vector readUsers(String lastname, int userType, int userStatus, int wasLoggedIn, int nMax) throws CmsException;

    /**
     * Returns a list of users of a group.<p>
     *
     * @param name the name of the group
     * @param type the type of the users to read
     * @return Vector of users
     * @throws CmsException if operation was not successful
     */
    Vector readUsersOfGroup(String name, int type) throws CmsException;

    /**
     * Removes all access control entries belonging to a resource from the database.<p>
     * 
     * @param project the project to write the entry
     * @param resource the id of the resource
     * @throws CmsException if something goes wrong
     */
    void removeAccessControlEntries(CmsProject project, CmsUUID resource) throws CmsException;

    /**
     * Removes an access control entry from the database.<p>
     * 
     * @param project the project to write the entry
     * @param resource the id of the resource
     * @param principal the id of the principal
     * @throws CmsException if something goes wrong
     */
    void removeAccessControlEntry(CmsProject project, CmsUUID resource, CmsUUID principal) throws CmsException;

    /**
     * Undeletes all access control entries belonging to a resource.<p>
     * 
     * @param project the project to write the entry
     * @param resource the id of the resource
     * @throws CmsException if something goes wrong
     */
    void undeleteAccessControlEntries(CmsProject project, CmsUUID resource) throws CmsException;

    /**
     * Checks if a user is member of a group.<P/>
     *
     * @param userId the id of the user to check
     * @param groupId the id of the group to check
     * @param reservedParam reserved optional parameter, should be null on standard OpenCms installations
     * @return true if user is member of group
     * @throws CmsException if operation was not succesful
     */
    //boolean validateUserInGroup(CmsUUID userId, CmsUUID groupId, Object reservedParam) throws CmsException;

    /**
     * Writes an access control entry to the cms.<p>
     * 
     * @param project the project to write the entry
     * @param acEntry the entry to write
     * @throws CmsException if something goes wrong
     */
    void writeAccessControlEntry(CmsProject project, CmsAccessControlEntry acEntry) throws CmsException;

    /**
     * Writes an already existing group in the Cms.<p>
     *
     * Only the admin can do this.
     *
     * @param group The group that should be written to the Cms.
     * @throws CmsException  Throws CmsException if operation was not succesfull.
     */
    void writeGroup(CmsGroup group) throws CmsException;

    /**
     * Sets a new password for a user.<p>
     *
     * @param userName the user to set the password for
     * @param password the password to set
     * @throws CmsException if something goes wrong
     */
    void writePassword(String userName, String password) throws CmsException;

    /**
     * Sets the password, only if the user knows the recovery-password.<p>
     *
     * @param userName the user to set the password for
     * @param recoveryPassword the recoveryPassword the user has to know to set the password
     * @param password the password to set
     * @throws CmsException if something goes wrong
     */
    void writePassword(String userName, String recoveryPassword, String password) throws CmsException;

    /**
     * Sets a new password for a user.<p>
     *
     * @param userName the user to set the password for.
     * @param password the recoveryPassword to set
     * @throws CmsException if something goes wrong
     */
    void writeRecoveryPassword(String userName, String password) throws CmsException;

    /**
     * Writes a user to the database.<p>
     *
     * @param user the user to write
     * @throws CmsException if something goes wrong
     */
    void writeUser(CmsUser user) throws CmsException;

    /**
     * Changes the user type of the user.<p>
     *
     * @param userId the id of the user to change
     * @param userType the new usertype of the user
     * @throws CmsException if something goes wrong
     */
    void writeUserType(CmsUUID userId, int userType) throws CmsException;

    /**
     * Returns the SqlManager of this driver.<p>
     * 
     * @return the SqlManager of this driver
     */
    CmsSqlManager getSqlManager();

    /**
     * Returns all groups with a name like the specified pattern.<p>
     *
     * All users are granted, except the anonymous user.<p>
     *
     * @param namePattern pattern for the group name
     * @return a Vector of all groups with a name like the given pattern
     * @throws CmsException if something goes wrong
     */
    Vector readGroupsLike(String namePattern) throws CmsException;

    /**
     * Checks if a user is a direct member of a group having a name like the specified pattern.<p>
     *
     * All users are granted.<p>
     *
     * @param userId the id of the user
     * @param paramStr additional parameter
     * @param groupNamePattern pattern for group name
     * @return <code>true</code> if the given user is a direct member of a group having a name like the specified pattern
     * @throws CmsException if something goes wrong
     */
    boolean hasGroupsOfUserLike(CmsUUID userId, String paramStr, String groupNamePattern) throws CmsException;

}