/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/genericSql/Attic/CmsUserAccess.java,v $
 * Date   : $Date: 2003/05/20 13:25:18 $
 * Version: $Revision: 1.9 $
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
package com.opencms.file.genericSql;

import com.opencms.boot.I_CmsLogChannels;
import com.opencms.core.A_OpenCms;
import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.file.CmsGroup;
import com.opencms.file.CmsProject;
import com.opencms.file.CmsUser;
import com.opencms.file.I_CmsResourceBroker;
import com.opencms.flex.util.CmsUUID;
import com.opencms.util.SqlHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Hashtable;
import java.util.Vector;

import source.org.apache.java.util.Configurations;

/**
 * Generic, database server independent, implementation of the user access methods.
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @version $Revision: 1.9 $ $Date: 2003/05/20 13:25:18 $
 */
public class CmsUserAccess extends Object implements I_CmsConstants, I_CmsLogChannels, I_CmsUserAccess {

    /**
     * A digest to encrypt the passwords.
     */
    protected MessageDigest m_digest = null;

    /**
     * The file.encoding to code passwords after encryption with digest.
     */
    protected String m_digestFileEncoding = null;

    protected String m_poolName;
    protected String m_poolNameBackup;
    protected String m_poolNameOnline;

    protected I_CmsResourceBroker m_ResourceBroker;

    protected com.opencms.file.genericSql.CmsQueries m_SqlQueries;

    /**
     * Default constructor.
     * 
     * @param config the configurations objects (-> opencms.properties)
     * @param theResourceBroker the instance of the resource broker
     */
    public CmsUserAccess(Configurations config, String dbPoolUrl, I_CmsResourceBroker theResourceBroker) {
        m_SqlQueries = initQueries(dbPoolUrl);
        m_ResourceBroker = theResourceBroker;
        
        m_poolName = m_poolNameBackup = m_poolNameOnline = dbPoolUrl;

        String brokerName = (String) config.getString(com.opencms.core.I_CmsConstants.C_CONFIGURATION_RESOURCEBROKER);

        String digest = config.getString(C_CONFIGURATION_RESOURCEBROKER + "." + brokerName + ".digest", "MD5");
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". Digest configured    : " + digest);
        }

        m_digestFileEncoding = config.getString(C_CONFIGURATION_RESOURCEBROKER + "." + brokerName + ".digest.fileencoding", "ISO-8859-1");
        if (I_CmsLogChannels.C_LOGGING && A_OpenCms.isLogging()) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". Digest file encoding : " + m_digestFileEncoding);
        }

        // create the digest
        try {
            m_digest = MessageDigest.getInstance(digest);
            if (I_CmsLogChannels.C_LOGGING && A_OpenCms.isLogging()) {
                A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". Using digest encoding: " + m_digest.getAlgorithm() + " from " + m_digest.getProvider().getName() + " version " + m_digest.getProvider().getVersion());
            }
        } catch (NoSuchAlgorithmException e) {
            if (I_CmsLogChannels.C_LOGGING && A_OpenCms.isLogging()) {
                A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". Error setting digest : using clear passwords - " + e.getMessage());
            }
        }

        ///////////////////////////////////////////////

//        // TODO: the following code should be removed when all methods in this
//        // class are switched to the new CmsQueries methods
//
//        // get the standard pool
//        m_poolName = config.getString(com.opencms.core.I_CmsConstants.C_CONFIGURATION_RESOURCEBROKER + "." + brokerName + "." + com.opencms.core.I_CmsConstants.C_CONFIGURATIONS_POOL);
//        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
//            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". Database offline pool: " + m_poolName);
//        }
//
//        // get the pool for the online resources
//        m_poolNameOnline = config.getString(com.opencms.core.I_CmsConstants.C_CONFIGURATION_RESOURCEBROKER + "." + brokerName + ".online." + com.opencms.core.I_CmsConstants.C_CONFIGURATIONS_POOL);
//        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
//            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". Database online pool : " + m_poolNameOnline);
//        }
//
//        // get the pool for the backup resources
//        m_poolNameBackup = config.getString(com.opencms.core.I_CmsConstants.C_CONFIGURATION_RESOURCEBROKER + "." + brokerName + ".backup." + com.opencms.core.I_CmsConstants.C_CONFIGURATIONS_POOL);
//        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
//            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". Database backup pool : " + m_poolNameBackup);
//        }
//
//        // set the default pool for the id generator
//        com.opencms.dbpool.CmsIdGenerator.setDefaultPool(m_poolName);

        ///////////////////////////////////////////////////////        
    }

    /**
     * Adds a user to the database.
     *
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
     *
     * @return the created user.
     * @throws throws CmsException if something goes wrong.
     */
    public CmsUser addImportUser(String name, String password, String recoveryPassword, String description, String firstname, String lastname, String email, long lastlogin, long lastused, int flags, Hashtable additionalInfos, CmsGroup defaultGroup, String address, String section, int type) throws CmsException {
        byte[] value = null;
        //int id = m_SqlQueries.nextPkId("C_TABLE_USERS");
        CmsUUID id = new CmsUUID();

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            value = serializeAdditionalUserInfo(additionalInfos);

            // write data to database
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_USERS_ADD");

            stmt.setString(1, id.toString());
            stmt.setString(2, name);
            stmt.setString(3, m_SqlQueries.validateNull(password));
            stmt.setString(4, m_SqlQueries.validateNull(recoveryPassword));
            stmt.setString(5, m_SqlQueries.validateNull(description));
            stmt.setString(6, m_SqlQueries.validateNull(firstname));
            stmt.setString(7, m_SqlQueries.validateNull(lastname));
            stmt.setString(8, m_SqlQueries.validateNull(email));
            stmt.setTimestamp(9, new Timestamp(lastlogin));
            stmt.setTimestamp(10, new Timestamp(lastused));
            stmt.setInt(11, flags);
            m_SqlQueries.setBytes(stmt, 12, value);
            stmt.setString(13, defaultGroup.getId().toString());
            stmt.setString(14, m_SqlQueries.validateNull(address));
            stmt.setString(15, m_SqlQueries.validateNull(section));
            stmt.setInt(16, type);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } catch (IOException e) {
            throw m_SqlQueries.getCmsException(this, "[CmsAccessUserInfoMySql/addUserInformation(id,object)]:", CmsException.C_SERIALIZATION, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, null);
        }

        return readUser(id);
    }

    /**
     * Adds a user to the database.
     *
     * @param name username
     * @param password user-password
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
     *
     * @return the created user.
     * @throws thorws CmsException if something goes wrong.
     */
    public CmsUser addUser(String name, String password, String description, String firstname, String lastname, String email, long lastlogin, long lastused, int flags, Hashtable additionalInfos, CmsGroup defaultGroup, String address, String section, int type) throws CmsException {
        byte[] value = null;
        //int id = m_SqlQueries.nextPkId("C_TABLE_USERS");
        CmsUUID id = new CmsUUID();

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            value = serializeAdditionalUserInfo(additionalInfos);

            // write data to database
            
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_USERS_ADD");

            stmt.setString(1, id.toString());
            stmt.setString(2, name);
            // crypt the password with MD5
            stmt.setString(3, digest(password));
            stmt.setString(4, digest(""));
            stmt.setString(5, m_SqlQueries.validateNull(description));
            stmt.setString(6, m_SqlQueries.validateNull(firstname));
            stmt.setString(7, m_SqlQueries.validateNull(lastname));
            stmt.setString(8, m_SqlQueries.validateNull(email));
            stmt.setTimestamp(9, new Timestamp(lastlogin));
            stmt.setTimestamp(10, new Timestamp(lastused));
            stmt.setInt(11, flags);
            m_SqlQueries.setBytes(stmt, 12, value);
            stmt.setString(13, defaultGroup.getId().toString());
            stmt.setString(14, m_SqlQueries.validateNull(address));
            stmt.setString(15, m_SqlQueries.validateNull(section));
            stmt.setInt(16, type);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } catch (IOException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SERIALIZATION, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, null);
        }

        return readUser(id);
    }

    /**
     * Adds a user to a group.<BR/>
     *
     * Only the admin can do this.<P/>
     *
     * @param userid The id of the user that is to be added to the group.
     * @param groupid The id of the group.
     * @throws CmsException Throws CmsException if operation was not succesfull.
     */
    public void addUserToGroup(CmsUUID userid, CmsUUID groupid) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;

        // check if user is already in group
        if (!isUserInGroup(userid, groupid)) {
            // if not, add this user to the group
            try {
                // create statement
                conn = m_SqlQueries.getConnection();
                stmt = m_SqlQueries.getPreparedStatement(conn, "C_GROUPS_ADDUSERTOGROUP");               
                
                // write the new assingment to the database
                stmt.setString(1, groupid.toString());
                stmt.setString(2, userid.toString());
                // flag field is not used yet
                stmt.setInt(3, C_UNKNOWN_INT);
                stmt.executeUpdate();

            } catch (SQLException e) {
                throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
            } finally {
                m_SqlQueries.closeAll(conn, stmt, null);
            }
        }
    }

    /**
     * Changes the user type of the user
     *
     * @param userId The id of the user to change
     * @param userType The new usertype of the user
     */
    public void changeUserType(CmsUUID userId, int userType) throws CmsException {
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_USERS_UPDATE_USERTYPE");               

            // write data to database
            stmt.setInt(1, userType);
            stmt.setString(2, userId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, null);
        }
    }

    /**
     * helper for getReadingpermittedGroup. Returns the id of the group that is in
     * any way parent for the other group or -1 for no dependencies between the groups.
     */
    public CmsUUID checkGroupDependence(CmsUUID groupId1, CmsUUID groupId2) throws CmsException {
        CmsUUID currentGroupId = groupId1;
        do {
            currentGroupId = readGroup(currentGroupId).getParentId();
            if (currentGroupId.equals(groupId2)) {
                return groupId1;
            }
        } while (!currentGroupId.isNullUUID());

        currentGroupId = groupId2;
        do {
            currentGroupId = readGroup(currentGroupId).getParentId();
            if (currentGroupId.equals(groupId1)) {
                return groupId2;
            }
        } while (!currentGroupId.isNullUUID());

        return CmsUUID.getNullUUID();
    }

    /**
     * checks a Vector of Groupids for the Group which can read all files
     *
     * @param groups A Vector with groupids (Integer).
     * @return The id of the group that is in any way parent of all other
     *       group or -1 for no dependencies between the groups.
     */
    public CmsUUID checkGroupDependence(Vector groups) throws CmsException {
        if ((groups == null) || (groups.size() == 0)) {
            return CmsUUID.getNullUUID();
        }

        CmsUUID returnValue = (CmsUUID) groups.elementAt(0);
        for (int i = 1; i < groups.size(); i++) {
            returnValue = checkGroupDependence(returnValue, (CmsUUID) groups.elementAt(i));
            if (returnValue.isNullUUID()) {
                return CmsUUID.getNullUUID();
            }
        }

        return returnValue;
    }

    /**
     * Add a new group to the Cms.<BR/>
     *
     * Only the admin can do this.<P/>
     *
     * @param name The name of the new group.
     * @param description The description for the new group.
     * @param flags The flags for the new group.
     * @param name The name of the parent group (or null).
     *
     * @return Group
     *
     * @throws CmsException Throws CmsException if operation was not succesfull.
     */
    public CmsGroup createGroup(String groupName, String description, int flags, String parentGroupName) throws CmsException {
        CmsUUID parentId = CmsUUID.getNullUUID();
        CmsGroup group = null;
        CmsUUID groupId = new CmsUUID();

        Connection conn = null;
        PreparedStatement stmt = null;

        try {

            // get the id of the parent group if necessary
            if ((parentGroupName != null) && (!"".equals(parentGroupName))) {
                parentId = readGroup(parentGroupName).getId();
            }
            
            // create statement
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_GROUPS_CREATEGROUP");

            // write new group to the database
            stmt.setString(1, groupId.toString());
            stmt.setString(2, parentId.toString());
            stmt.setString(3, groupName);
            stmt.setString(4, m_SqlQueries.validateNull(description));
            stmt.setInt(5, flags);
            stmt.executeUpdate();

            // create the user group by reading it from the database.
            // this is necessary to get the group id which is generated in the
            // database.
            group = readGroup(groupName);
        } catch (SQLException e) {
            m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, null);
        }

        return group;
    }

    /**
     * Delete a group from the Cms.<BR/>
     * Only groups that contain no subgroups can be deleted.
     *
     * Only the admin can do this.<P/>
     *
     * @param delgroup The name of the group that is to be deleted.
     * @throws CmsException  Throws CmsException if operation was not succesfull.
     */
    public void deleteGroup(String delgroup) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            // create statement
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_GROUPS_DELETEGROUP");

            stmt.setString(1, delgroup);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, null);
        }
    }

    /**
     * Deletes a user from the database.
     *
     * @param userId The Id of the user to delete
     * @throws thorws CmsException if something goes wrong.
     */
    public void deleteUser(CmsUUID userId) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_USERS_DELETEBYID");
            stmt.setString(1, userId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, null);
        }
    }

    /**
     * Deletes a user from the database.
     *
     * @param user the user to delete
     * @throws thorws CmsException if something goes wrong.
     */
    public void deleteUser(String userName) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_USERS_DELETE");
            stmt.setString(1, userName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, null);
        }
    }

    /**
     * Private method to encrypt the passwords.
     *
     * @param value The value to encrypt.
     * @return The encrypted value.
     */
    public String digest(String value) {
        // is there a valid digest?
        if (m_digest != null) {
            try {
                byte[] bytesForDigest = value.getBytes(m_digestFileEncoding);
                byte[] bytesFromDigest = m_digest.digest(bytesForDigest);
                // to get a String out of the bytearray we translate every byte
                // in a hex value and put them together
                StringBuffer result = new StringBuffer();
                String addZerro;
                for (int i = 0; i < bytesFromDigest.length; i++) {
                    addZerro = Integer.toHexString(128 + bytesFromDigest[i]);
                    if (addZerro.length() < 2) {
                        addZerro = "0" + addZerro;
                    }
                    result.append(addZerro);
                }
                return result.toString();
            } catch (UnsupportedEncodingException exc) {
                if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
                    A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[CmsDbAccess] file.encoding " + m_digestFileEncoding + " for passwords not supported. Using the default one.");
                }
                return new String(m_digest.digest(value.getBytes()));
            }
        } else {
            // no digest - use clear passwords
            return value;
        }
    }

    /**
     * Returns all projects, which are accessible by a group.
     *
     * @param group The requesting group.
     *
     * @return a Vector of projects.
     */
    public Vector getAllAccessibleProjectsByGroup(CmsGroup group) throws CmsException {
        Vector projects = new Vector();
        ResultSet res = null;
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            // create the statement
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_PROJECTS_READ_BYGROUP");

            stmt.setString(1, group.getId().toString());
            stmt.setString(2, group.getId().toString());
            res = stmt.executeQuery();

            while (res.next()) {
                projects.addElement(new CmsProject(res, m_SqlQueries));
            }
        } catch (Exception exc) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, exc);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, res);
        }
        return (projects);
    }

    /**
     * Returns all projects, which are manageable by a group.
     *
     * @param group The requesting group.
     *
     * @return a Vector of projects.
     */
    public Vector getAllAccessibleProjectsByManagerGroup(CmsGroup group) throws CmsException {
        Vector projects = new Vector();
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            // create the statement
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_PROJECTS_READ_BYMANAGER");
            
            stmt.setString(1, group.getId().toString());
            res = stmt.executeQuery();

            while (res.next()) {
                projects.addElement(new CmsProject(res, m_SqlQueries));
            }
        } catch (Exception exc) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, exc);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, res);
        }
        return (projects);
    }

    /**
     * Returns all projects, which are owned by a user.
     *
     * @param user The requesting user.
     *
     * @return a Vector of projects.
     */
    public Vector getAllAccessibleProjectsByUser(CmsUser user) throws CmsException {
        Vector projects = new Vector();
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            // create the statement
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_PROJECTS_READ_BYUSER");

            stmt.setString(1, user.getId().toString());
            res = stmt.executeQuery();

            while (res.next()) {
                projects.addElement(new CmsProject(res, m_SqlQueries));
            }
        } catch (Exception exc) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, exc);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, res);
        }
        return (projects);
    }

    /**
    * Returns all groups<P/>
    *
    * @return users A Vector of all existing groups.
    * @throws CmsException Throws CmsException if operation was not succesful.
    */
    public Vector getGroups() throws CmsException {
        Vector groups = new Vector();
        //CmsGroup group = null;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            // create statement
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_GROUPS_GETGROUPS");

            res = stmt.executeQuery();

            // create new Cms group objects
            while (res.next()) {
                groups.addElement(createCmsGroupFromResultSet(res, true));
            }

        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, res);
        }
        return groups;
    }

    /**
     * Returns a list of groups of a user.<P/>
     *
     * @param name The name of the user.
     * @return Vector of groups
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Vector getGroupsOfUser(String name) throws CmsException {
        //CmsGroup group;
        Vector groups = new Vector();

        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;

        try {
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_GROUPS_GETGROUPSOFUSER");

            //  get all all groups of the user
            stmt.setString(1, name);

            res = stmt.executeQuery();

            while (res.next()) {
                groups.addElement(createCmsGroupFromResultSet(res, true));
            }
        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, res);
        }
        return groups;
    }

    /**
     * Gets all users of a type.
     *
     * @param type The type of the user.
     * @throws throws CmsException if something goes wrong.
     */
    public Vector getUsers(int type) throws CmsException {
        Vector users = new Vector();
        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;

        try {
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_USERS_GETUSERS");
            stmt.setInt(1, type);
            res = stmt.executeQuery();
            // create new Cms user objects
            while (res.next()) {
                users.addElement(createCmsUserFromResultSet(res, true));
            }
        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } catch (Exception e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, res);
        }
        return users;
    }

    /**
    * Gets all users of a type and namefilter.
    *
    * @param type The type of the user.
    * @param namestart The namefilter
    * @throws thorws CmsException if something goes wrong.
    */
    public Vector getUsers(int type, String namefilter) throws CmsException {
        Vector users = new Vector();
        Statement stmt = null;
        ResultSet res = null;
        Connection conn = null;

        try {
            conn = m_SqlQueries.getConnection();
            stmt = conn.createStatement();
            res = stmt.executeQuery(m_SqlQueries.get("C_USERS_GETUSERS_FILTER1") + type + m_SqlQueries.get("C_USERS_GETUSERS_FILTER2") + namefilter + m_SqlQueries.get("C_USERS_GETUSERS_FILTER3"));

            // create new Cms user objects
            while (res.next()) {
                users.addElement(createCmsUserFromResultSet(res, true));
            }

        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } catch (Exception e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, res);
        }

        return users;
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
     */
    public Vector getUsersByLastname(String lastname, int userType, int userStatus, int wasLoggedIn, int nMax) throws CmsException {
        Vector users = new Vector();
        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;
        int i = 0;
        // "" =  return (nearly) all users
        if (lastname == null)
            lastname = "";

        try {
            conn = m_SqlQueries.getConnection();
            
            if (wasLoggedIn == C_AT_LEAST_ONCE)
                stmt = m_SqlQueries.getPreparedStatement(conn, "C_USERS_GETUSERS_BY_LASTNAME_ONCE");
            else if (wasLoggedIn == C_NEVER)
                stmt = m_SqlQueries.getPreparedStatement(conn, "C_USERS_GETUSERS_BY_LASTNAME_NEVER");
            else // C_WHATEVER or whatever else
                stmt = m_SqlQueries.getPreparedStatement(conn, "C_USERS_GETUSERS_BY_LASTNAME_WHATEVER");

            stmt.setString(1, lastname + "%");
            stmt.setInt(2, userType);
            stmt.setInt(3, userStatus);

            res = stmt.executeQuery();
            // create new Cms user objects
            while (res.next() && (i++ < nMax)) {               
                users.addElement(createCmsUserFromResultSet(res, true));
            }

        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } catch (Exception e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, res);
        }

        return users;
    }

    /**
     * Returns a list of users of a group.<P/>
     *
     * @param name The name of the group.
     * @param type the type of the users to read.
     * @return Vector of users
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Vector getUsersOfGroup(String name, int type) throws CmsException {
        Vector users = new Vector();

        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;

        try {
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_GROUPS_GETUSERSOFGROUP");
            stmt.setString(1, name);
            stmt.setInt(2, type);

            res = stmt.executeQuery();

            while (res.next()) {
                users.addElement(createCmsUserFromResultSet(res, false));
            }
        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } catch (Exception e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, res);
        }

        return users;
    }

    public com.opencms.file.genericSql.CmsQueries initQueries(String dbPoolUrl) {
        return new com.opencms.file.genericSql.CmsQueries(dbPoolUrl);
    }

    /**
    * Returns a group object.<P/>
    * @param groupname The id of the group that is to be read.
    * @return Group.
    * @throws CmsException  Throws CmsException if operation was not succesful
    */
    public CmsGroup readGroup(CmsUUID groupId) throws CmsException {
        CmsGroup group = null;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_GROUPS_READGROUP2");

            // read the group from the database
            stmt.setString(1, groupId.toString());
            res = stmt.executeQuery();
            // create new Cms group object
            if (res.next()) {
                group = createCmsGroupFromResultSet(res, true);
            } else {
                throw m_SqlQueries.getCmsException(this, null, CmsException.C_NO_GROUP, new Exception());
            }

        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, res);
        }

        return group;
    }

    /**
     * Returns a group object.<P/>
     * @param groupname The name of the group that is to be read.
     * @return Group.
     * @throws CmsException  Throws CmsException if operation was not succesful
     */
    public CmsGroup readGroup(String groupName) throws CmsException {

        CmsGroup group = null;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_GROUPS_READGROUP");

            // read the group from the database
            stmt.setString(1, groupName);
            res = stmt.executeQuery();

            // create new Cms group object
            if (res.next()) {
                group = createCmsGroupFromResultSet(res, true);
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + groupName, CmsException.C_NO_GROUP);
            }

        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, res);
        }
        return group;
    }

    /**
     * Reads a user from the cms, only if the password is correct.
     *
     * @param id the id of the user.
     * @param type the type of the user.
     * @return the read user.
     * @throws thorws CmsException if something goes wrong.
     */
    public CmsUser readUser(CmsUUID id) throws CmsException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        CmsUser user = null;
        Connection conn = null;

        try {
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_USERS_READID");

            stmt.setString(1, id.toString());
            res = stmt.executeQuery();

            // create new Cms user object
            if (res.next()) {
                user = createCmsUserFromResultSet(res, true);
            } else {
                res.close();
                res = null;
                throw new CmsException("[" + this.getClass().getName() + "]" + id, CmsException.C_NO_USER);
            }

            return user;
        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        }
        // a.lucas: catch CmsException here and throw it again.
        // Don't wrap another CmsException around it, since this may cause problems during login.
        catch (CmsException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } catch (Exception e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, res);
        }
    }

    /**
     * Reads a user from the cms.
     *
     * @param name the name of the user.
     * @param type the type of the user.
     * @return the read user.
     * @throws throws CmsException if something goes wrong.
     */
    public CmsUser readUser(String name, int type) throws CmsException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        CmsUser user = null;
        Connection conn = null;
        try {
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_USERS_READ");
            stmt.setString(1, name);
            stmt.setInt(2, type);

            res = stmt.executeQuery();

            // create new Cms user object
            if (res.next()) {
                user = createCmsUserFromResultSet(res, true);
            } else {
                res.close();
                res = null;
                throw new CmsException("[" + this.getClass().getName() + "]" + name, CmsException.C_NO_USER);
            }

            return user;
        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        }
        catch (CmsException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } catch (Exception e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, res);
        }
    }

    /**
     * Reads a user from the cms, only if the password is correct.
     *
     * @param name the name of the user.
     * @param password the password of the user.
     * @param type the type of the user.
     * @return the read user.
     * @throws throws CmsException if something goes wrong.
     */
    public CmsUser readUser(String name, String password, int type) throws CmsException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        CmsUser user = null;
        Connection conn = null;
        try {
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_USERS_READPW");
            stmt.setString(1, name);
            stmt.setString(2, digest(password));
            stmt.setInt(3, type);
            res = stmt.executeQuery();

            // create new Cms user object
            if (res.next()) {
                user = createCmsUserFromResultSet(res, true);
            } else {
                res.close();
                res = null;
                throw new CmsException("[" + this.getClass().getName() + "]" + name, CmsException.C_NO_USER);
            }

            return user;
        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        }
        catch (CmsException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } catch (Exception e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, res);
        }
    }

    /**
     * Sets the password, only if the user knows the recovery-password.
     *
     * @param user the user to set the password for.
     * @param recoveryPassword the recoveryPassword the user has to know to set the password.
     * @param password the password to set
     * @throws throws CmsException if something goes wrong.
     */
    public void recoverPassword(String userName, String recoveryPassword, String password) throws CmsException {
        PreparedStatement stmt = null;
        Connection conn = null;

        int result;

        try {
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_USERS_RECOVERPW");

            stmt.setString(1, digest(password));
            stmt.setString(2, userName);
            stmt.setString(3, digest(recoveryPassword));
            result = stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, null);
        }

        if (result != 1) {
            // the update wasn't succesfull -> throw exception
            throw new CmsException("[" + this.getClass().getName() + "] the password couldn't be recovered.");
        }
    }

    /**
     * Removes a user from a group.
     *
     * Only the admin can do this.<P/>
     *
     * @param userid The id of the user that is to be added to the group.
     * @param groupid The id of the group.
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public void removeUserFromGroup(CmsUUID userId, CmsUUID groupId) throws CmsException {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            // create statement
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_GROUPS_REMOVEUSERFROMGROUP");

            stmt.setString(1, groupId.toString());
            stmt.setString(2, userId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, null);
        }
    }

    /**
     * Sets a new password for a user.
     *
     * @param user the user to set the password for.
     * @param password the password to set
     * @throws throws CmsException if something goes wrong.
     */
    public void setPassword(String userName, String password) throws CmsException {
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_USERS_SETPW");
            stmt.setString(1, digest(password));
            stmt.setString(2, userName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, null);
        }
    }

    /**
     * Sets a new password for a user.
     *
     * @param user the user to set the password for.
     * @param password the recoveryPassword to set
     * @throws throws CmsException if something goes wrong.
     */
    public void setRecoveryPassword(String userName, String password) throws CmsException {
        PreparedStatement stmt = null;
        Connection conn = null;
        int result;

        try {
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_USERS_SETRECPW");

            stmt.setString(1, digest(password));
            stmt.setString(2, userName);
            result = stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, null);
        }

        if (result != 1) {
            // the update wasn't succesfull -> throw exception
            throw new CmsException("[" + this.getClass().getName() + "] new password couldn't be set.");
        }
    }

    /**
     * Checks if a user is member of a group.<P/>
     *
     * @param nameid The id of the user to check.
     * @param groupid The id of the group to check.
     * @return True or False
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public boolean isUserInGroup(CmsUUID userId, CmsUUID groupId) throws CmsException {
        boolean userInGroup = false;
        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;

        try {
            // create statement
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_GROUPS_USERINGROUP");

            stmt.setString(1, groupId.toString());
            stmt.setString(2, userId.toString());
            res = stmt.executeQuery();
            if (res.next()) {
                userInGroup = true;
            }
        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, res);
        }

        return userInGroup;
    }

    /**
     * Writes an already existing group in the Cms.<BR/>
     *
     * Only the admin can do this.<P/>
     *
     * @param group The group that should be written to the Cms.
     * @throws CmsException  Throws CmsException if operation was not succesfull.
     */
    public void writeGroup(CmsGroup group) throws CmsException {
        PreparedStatement stmt = null;
        Connection conn = null;
        if (group != null) {
            try {        
                
                // create statement
                conn = m_SqlQueries.getConnection();
                stmt = m_SqlQueries.getPreparedStatement(conn, "C_GROUPS_WRITEGROUP");

                stmt.setString(1, m_SqlQueries.validateNull(group.getDescription()));
                stmt.setInt(2, group.getFlags());
                stmt.setString(3, group.getParentId().toString());
                stmt.setString(4, group.getId().toString());
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
            } finally {
                m_SqlQueries.closeAll(conn, stmt, null);
            }
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_NO_GROUP);
        }
    }

    /**
     * Writes a user to the database.
     *
     * @param user the user to write
     * @throws thorws CmsException if something goes wrong.
     */
    public void writeUser(CmsUser user) throws CmsException {
        byte[] value = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_SqlQueries.getConnection();
            stmt = m_SqlQueries.getPreparedStatement(conn, "C_USERS_WRITE");
            
            value = serializeAdditionalUserInfo(user.getAdditionalInfo());

            // write data to database
            stmt.setString(1, m_SqlQueries.validateNull(user.getDescription()));
            stmt.setString(2, m_SqlQueries.validateNull(user.getFirstname()));
            stmt.setString(3, m_SqlQueries.validateNull(user.getLastname()));
            stmt.setString(4, m_SqlQueries.validateNull(user.getEmail()));
            stmt.setTimestamp(5, new Timestamp(user.getLastlogin()));
            stmt.setTimestamp(6, new Timestamp(user.getLastUsed()));
            stmt.setInt(7, user.getFlags());
            m_SqlQueries.setBytes(stmt, 8, value);
            stmt.setString(9, user.getDefaultGroupId().toString());
            stmt.setString(10, m_SqlQueries.validateNull(user.getAddress()));
            stmt.setString(11, m_SqlQueries.validateNull(user.getSection()));
            stmt.setInt(12, user.getType());
            stmt.setString(13, user.getId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SQL_ERROR, e);
        } catch (IOException e) {
            throw m_SqlQueries.getCmsException(this, null, CmsException.C_SERIALIZATION, e);
        } finally {
            m_SqlQueries.closeAll(conn, stmt, null);
        }
    }

    /**
     * Semi-constructor to create a CmsGroup instance from a JDBC result set.
     * 
     * @param res the JDBC ResultSet
     * @param hasGroupIdInResultSet true if the SQL select query includes the GROUP_ID table attribute
     * @return CmsGroup the new CmsGroup object
     * @throws SQLException in case the result set does not include a requested table attribute
     */
    protected final CmsGroup createCmsGroupFromResultSet(ResultSet res, boolean hasGroupIdInResultSet) throws SQLException {
        // this method is final to allow the java compiler to inline this code!
        CmsUUID groupId = null;        
        
        if (hasGroupIdInResultSet) {
            groupId = new CmsUUID(res.getString(m_SqlQueries.get("C_GROUPS_GROUP_ID")));
        } else {
            groupId = new CmsUUID(res.getString(m_SqlQueries.get("C_USERS_USER_DEFAULT_GROUP_ID")));
        }
        
        return new CmsGroup(groupId, new CmsUUID(res.getString(m_SqlQueries.get("C_GROUPS_PARENT_GROUP_ID"))), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_NAME")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_DESCRIPTION")), res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_FLAGS")));
    }

    /**
     * Semi-constructor to create a CmsUser instance from a JDBC result set.
     * 
     * @param res the JDBC ResultSet
     * @return CmsUser the new CmsUser object
     * @throws SQLException in case the result set does not include a requested table attribute
     */
    protected final CmsUser createCmsUserFromResultSet(ResultSet res, boolean hasGroupIdInResultSet) throws SQLException, IOException, ClassNotFoundException {
        // this method is final to allow the java compiler to inline this code!
        
        // deserialize the additional userinfo hash
        byte[] value = m_SqlQueries.getBytes(res, m_SqlQueries.get("C_USERS_USER_INFO"));
        ByteArrayInputStream bin = new ByteArrayInputStream(value);
        ObjectInputStream oin = new ObjectInputStream(bin);
        Hashtable info = (Hashtable) oin.readObject();

        return new CmsUser(
            new CmsUUID(res.getString(m_SqlQueries.get("C_USERS_USER_ID"))),
            res.getString(m_SqlQueries.get("C_USERS_USER_NAME")),
            res.getString(m_SqlQueries.get("C_USERS_USER_PASSWORD")),
            res.getString(m_SqlQueries.get("C_USERS_USER_RECOVERY_PASSWORD")),
            res.getString(m_SqlQueries.get("C_USERS_USER_DESCRIPTION")),
            res.getString(m_SqlQueries.get("C_USERS_USER_FIRSTNAME")),
            res.getString(m_SqlQueries.get("C_USERS_USER_LASTNAME")),
            res.getString(m_SqlQueries.get("C_USERS_USER_EMAIL")),
            SqlHelper.getTimestamp(res, m_SqlQueries.get("C_USERS_USER_LASTLOGIN")).getTime(),
            SqlHelper.getTimestamp(res, m_SqlQueries.get("C_USERS_USER_LASTUSED")).getTime(),
            res.getInt(m_SqlQueries.get("C_USERS_USER_FLAGS")),
            info,
            createCmsGroupFromResultSet(res, hasGroupIdInResultSet),
            res.getString(m_SqlQueries.get("C_USERS_USER_ADDRESS")),
            res.getString(m_SqlQueries.get("C_USERS_USER_SECTION")),
            res.getInt(m_SqlQueries.get("C_USERS_USER_TYPE")));
    }

    /**
     * Serialize additional user information to write it as byte array in the database.<p>
     * 
     * @param additionalUserInfo the HashTable with additional information
     * @return byte[] the byte array which is written to the db
     * @throws IOException
     */
    protected final byte[] serializeAdditionalUserInfo(Hashtable additionalUserInfo) throws IOException {
        // this method is final to allow the java compiler to inline this code!
        
        // serialize the hashtable
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(additionalUserInfo);
        oout.close();

        return bout.toByteArray();
    }

}