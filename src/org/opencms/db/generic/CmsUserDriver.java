/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/generic/CmsUserDriver.java,v $
 * Date   : $Date: 2003/06/25 16:21:43 $
 * Version: $Revision: 1.7 $
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
 
package org.opencms.db.generic;

import org.opencms.db.CmsDriverManager;
import org.opencms.db.I_CmsUserDriver;
import org.opencms.security.CmsAccessControlEntry;

import com.opencms.boot.I_CmsLogChannels;
import com.opencms.core.A_OpenCms;
import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.file.CmsGroup;
import com.opencms.file.CmsProject;
import com.opencms.file.CmsUser;
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
 * Generic (ANSI-SQL) database server implementation of the user driver methods.<p>
 * 
 * @version $Revision: 1.7 $ $Date: 2003/06/25 16:21:43 $
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 * @since 5.1
 */
public class CmsUserDriver extends Object implements I_CmsUserDriver {

    /**
     * A digest to encrypt the passwords.
     */
    protected MessageDigest m_digest = null;

    /**
     * The file.encoding to code passwords after encryption with digest.
     */
    protected String m_digestFileEncoding = null;

    protected org.opencms.db.generic.CmsSqlManager m_sqlManager;
    protected CmsDriverManager m_driverManager;

    /**
     * Adds a user to the database.
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
     *
     * @return the created user.
     * @throws CmsException if something goes wrong.
     */
    public CmsUser addImportUser(CmsUUID id, String name, String password, String recoveryPassword, String description, String firstname, String lastname, String email, long lastlogin, long lastused, int flags, Hashtable additionalInfos, CmsGroup defaultGroup, String address, String section, int type) throws CmsException {
        byte[] value = null;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            value = serializeAdditionalUserInfo(additionalInfos);

            // write data to database
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_ADD");

            stmt.setString(1, id.toString());
            stmt.setString(2, name);
            stmt.setString(3, m_sqlManager.validateNull(password));
            stmt.setString(4, m_sqlManager.validateNull(recoveryPassword));
            stmt.setString(5, m_sqlManager.validateNull(description));
            stmt.setString(6, m_sqlManager.validateNull(firstname));
            stmt.setString(7, m_sqlManager.validateNull(lastname));
            stmt.setString(8, m_sqlManager.validateNull(email));
            stmt.setTimestamp(9, new Timestamp(lastlogin));
            stmt.setTimestamp(10, new Timestamp(lastused));
            stmt.setInt(11, flags);
            m_sqlManager.setBytes(stmt, 12, value);
            stmt.setString(13, defaultGroup.getId().toString());
            stmt.setString(14, m_sqlManager.validateNull(address));
            stmt.setString(15, m_sqlManager.validateNull(section));
            stmt.setInt(16, type);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, "[CmsUser]: " + name + ", Id=" + id.toString(), CmsException.C_SQL_ERROR, e, false);
        } catch (IOException e) {
            throw m_sqlManager.getCmsException(this, "[CmsAccessUserInfoMySql/addUserInformation(id,object)]:", CmsException.C_SERIALIZATION, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
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
     * @throws CmsException if something goes wrong.
     */
    public CmsUser addUser(String name, String password, String description, String firstname, String lastname, String email, long lastlogin, long lastused, int flags, Hashtable additionalInfos, CmsGroup defaultGroup, String address, String section, int type) throws CmsException {
        byte[] value = null;
        //int id = m_sqlManager.nextPkId("C_TABLE_USERS");
        CmsUUID id = new CmsUUID();

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            value = serializeAdditionalUserInfo(additionalInfos);

            // write data to database
            
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_ADD");

            stmt.setString(1, id.toString());
            stmt.setString(2, name);
            // crypt the password with MD5
            stmt.setString(3, digest(password));
            stmt.setString(4, digest(""));
            stmt.setString(5, m_sqlManager.validateNull(description));
            stmt.setString(6, m_sqlManager.validateNull(firstname));
            stmt.setString(7, m_sqlManager.validateNull(lastname));
            stmt.setString(8, m_sqlManager.validateNull(email));
            stmt.setTimestamp(9, new Timestamp(lastlogin));
            stmt.setTimestamp(10, new Timestamp(lastused));
            stmt.setInt(11, flags);
            m_sqlManager.setBytes(stmt, 12, value);
            stmt.setString(13, defaultGroup.getId().toString());
            stmt.setString(14, m_sqlManager.validateNull(address));
            stmt.setString(15, m_sqlManager.validateNull(section));
            stmt.setInt(16, type);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (IOException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SERIALIZATION, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }

		// cw 20.06.2003 avoid calling upper level methods
        // return readUser(id);
        return this.readUser(id);
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
                conn = m_sqlManager.getConnection();
                stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_ADDUSERTOGROUP");               
                
                // write the new assingment to the database
                stmt.setString(1, groupid.toString());
                stmt.setString(2, userid.toString());
                // flag field is not used yet
                stmt.setInt(3, I_CmsConstants.C_UNKNOWN_INT);
                stmt.executeUpdate();

            } catch (SQLException e) {
                throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
            } finally {
                m_sqlManager.closeAll(conn, stmt, null);
            }
        }
    }

    /**
     * Changes the user type of the user
     *
     * @param userId The id of the user to change
     * @param userType The new usertype of the user
     * @throws CmsException if something goes wrong
     */
    public void changeUserType(CmsUUID userId, int userType) throws CmsException {
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_UPDATE_USERTYPE");               

            // write data to database
            stmt.setInt(1, userType);
            stmt.setString(2, userId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * helper for getReadingpermittedGroup. Returns the id of the group that is in
     * any way parent for the other group or -1 for no dependencies between the groups.
     * 
     * @param groupId1		id of the frist group
     * @param groupId2		id of the second group
     * @return				the id of the parent of both
     * @throws CmsException	if something goes wrong
     */
    private CmsUUID checkGroupDependence(CmsUUID groupId1, CmsUUID groupId2) throws CmsException {
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
     * @throws CmsException if something goes wrong
     */
    private CmsUUID checkGroupDependence(Vector groups) throws CmsException {
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
            groupId = new CmsUUID(res.getString(m_sqlManager.get("C_GROUPS_GROUP_ID")));
        } else {
            groupId = new CmsUUID(res.getString(m_sqlManager.get("C_USERS_USER_DEFAULT_GROUP_ID")));
        }
        
        return new CmsGroup(groupId, new CmsUUID(res.getString(m_sqlManager.get("C_GROUPS_PARENT_GROUP_ID"))), res.getString(m_sqlManager.get("C_GROUPS_GROUP_NAME")), res.getString(m_sqlManager.get("C_GROUPS_GROUP_DESCRIPTION")), res.getInt(m_sqlManager.get("C_GROUPS_GROUP_FLAGS")));
    }

    /**
     * Semi-constructor to create a CmsUser instance from a JDBC result set.
     * 
     * @param res the JDBC ResultSet
     * @param hasGroupIdInResultSet true, if a default group id is available
     * @return CmsUser the new CmsUser object
     * @throws SQLException in case the result set does not include a requested table attribute
     * @throws IOException if there is an error in deserializing the user info
     * @throws ClassNotFoundException if there is an error in deserializing the user info
     */
    protected final CmsUser createCmsUserFromResultSet(ResultSet res, boolean hasGroupIdInResultSet) throws SQLException, IOException, ClassNotFoundException {
        // this method is final to allow the java compiler to inline this code!
        
        // deserialize the additional userinfo hash
        byte[] value = m_sqlManager.getBytes(res, m_sqlManager.get("C_USERS_USER_INFO"));
        ByteArrayInputStream bin = new ByteArrayInputStream(value);
        ObjectInputStream oin = new ObjectInputStream(bin);
        Hashtable info = (Hashtable) oin.readObject();

        return new CmsUser(
            new CmsUUID(res.getString(m_sqlManager.get("C_USERS_USER_ID"))),
            res.getString(m_sqlManager.get("C_USERS_USER_NAME")),
            res.getString(m_sqlManager.get("C_USERS_USER_PASSWORD")),
            res.getString(m_sqlManager.get("C_USERS_USER_RECOVERY_PASSWORD")),
            res.getString(m_sqlManager.get("C_USERS_USER_DESCRIPTION")),
            res.getString(m_sqlManager.get("C_USERS_USER_FIRSTNAME")),
            res.getString(m_sqlManager.get("C_USERS_USER_LASTNAME")),
            res.getString(m_sqlManager.get("C_USERS_USER_EMAIL")),
            SqlHelper.getTimestamp(res, m_sqlManager.get("C_USERS_USER_LASTLOGIN")).getTime(),
            SqlHelper.getTimestamp(res, m_sqlManager.get("C_USERS_USER_LASTUSED")).getTime(),
            res.getInt(m_sqlManager.get("C_USERS_USER_FLAGS")),
            info,
            createCmsGroupFromResultSet(res, hasGroupIdInResultSet),
            res.getString(m_sqlManager.get("C_USERS_USER_ADDRESS")),
            res.getString(m_sqlManager.get("C_USERS_USER_SECTION")),
            res.getInt(m_sqlManager.get("C_USERS_USER_TYPE")));
    }

	/**
	 * Adds a new group to the Cms.<BR/>
	 *
	 * Only the admin can do this.<P/>
	 *
	 * @param groupId The unique id of the new group.
	 * @param groupName The name of the new group.
	 * @param description The description for the new group.
	 * @param flags The flags for the new group.
	 * @param parentGroupName The name of the parent group (or null).
	 *
	 * @return Group
	 *
	 * @throws CmsException Throws CmsException if operation was not succesfull.
	 */    
    public CmsGroup createGroup(CmsUUID groupId, String groupName, String description, int flags, String parentGroupName) throws CmsException {
        CmsUUID parentId = CmsUUID.getNullUUID();
        CmsGroup group = null;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {

            // get the id of the parent group if necessary
            if ((parentGroupName != null) && (!"".equals(parentGroupName))) {
                parentId = readGroup(parentGroupName).getId();
            }
            
            // create statement
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_CREATEGROUP");

            // write new group to the database
            stmt.setString(1, groupId.toString());
            stmt.setString(2, parentId.toString());
            stmt.setString(3, groupName);
            stmt.setString(4, m_sqlManager.validateNull(description));
            stmt.setInt(5, flags);
            stmt.executeUpdate();

            // TODO: remove this
            // create the user group by reading it from the database.
            // this is necessary to get the group id which is generated in the
            // database.
            // group = readGroup(groupName);
            group = new CmsGroup(groupId, parentId, groupName, description, flags);
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, "[CmsGroup]: " + groupName + ", Id=" + groupId.toString(), CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
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
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_DELETEGROUP");

            stmt.setString(1, delgroup);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Deletes a user from the database.
     *
     * @param userId The Id of the user to delete
     * @throws CmsException if something goes wrong.
     */
    public void deleteUser(CmsUUID userId) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_DELETEBYID");
            stmt.setString(1, userId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Deletes a user from the database.
     *
     * @param userName the user to delete
     * @throws CmsException if something goes wrong.
     */
    public void deleteUser(String userName) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_DELETE");
            stmt.setString(1, userName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Method to encrypt the passwords.
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
                    A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[CmsProjectDriver] file.encoding " + m_digestFileEncoding + " for passwords not supported. Using the default one.");
                }
                return new String(m_digest.digest(value.getBytes()));
            }
        } else {
            // no digest - use clear passwords
            return value;
        }
    }

   /**
    * Returns all child groups of a groups<P/>
    *
    *
    * @param groupname The name of the group.
    * @return users A Vector of all child groups or null.
    * @throws CmsException Throws CmsException if operation was not succesful.
    */
    public Vector getChild(String groupname) throws CmsException {
    
        Vector childs = new Vector();
        CmsGroup group;
        CmsGroup parent;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            // get parent group
            parent = readGroup(groupname);
            // parent group exists, so get all childs
            if (parent != null) {
                // create statement
                conn = m_sqlManager.getConnection();
                stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_GETCHILD");
                stmt.setString(1, parent.getId().toString());
                res = stmt.executeQuery();
                // create new Cms group objects
                while (res.next()) {
                    group = new CmsGroup(new CmsUUID(res.getString(m_sqlManager.get("C_GROUPS_GROUP_ID"))), new CmsUUID(res.getString(m_sqlManager.get("C_GROUPS_PARENT_GROUP_ID"))), res.getString(m_sqlManager.get("C_GROUPS_GROUP_NAME")), res.getString(m_sqlManager.get("C_GROUPS_GROUP_DESCRIPTION")), res.getInt(m_sqlManager.get("C_GROUPS_GROUP_FLAGS")));
                    childs.addElement(group);
                }
            }
    
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            // close all db-resources
            m_sqlManager.closeAll(conn, stmt, res);
        }
        //check if the child vector has no elements, set it to null.
        if (childs.size() == 0) {
            childs = null;
        }
        return childs;
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
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_GETGROUPS");

            res = stmt.executeQuery();

            // create new Cms group objects
            while (res.next()) {
                groups.addElement(createCmsGroupFromResultSet(res, true));
            }

        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return groups;
    }

    /**
     * Returns a list of groups of a user.<P/>
     *
     * @param userId The id of the user.
     * @return Vector of groups
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Vector getGroupsOfUser(CmsUUID userId) throws CmsException {
        //CmsGroup group;
        Vector groups = new Vector();

        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_GETGROUPSOFUSER");

            //  get all all groups of the user
            stmt.setString(1, userId.toString());

            res = stmt.executeQuery();

            while (res.next()) {
                groups.addElement(createCmsGroupFromResultSet(res, true));
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return groups;
    }

    /**
     * Gets all users of a type.
     *
     * @param type The type of the user.
     * @return list of users of this type
     * @throws CmsException if something goes wrong.
     */
    public Vector getUsers(int type) throws CmsException {
        Vector users = new Vector();
        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_GETUSERS");
            stmt.setInt(1, type);
            res = stmt.executeQuery();
            // create new Cms user objects
            while (res.next()) {
                users.addElement(createCmsUserFromResultSet(res, true));
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return users;
    }

   /**
    * Gets all users of a type and namefilter.
    *
    * @param type The type of the user.
    * @param namefilter The namefilter
    * @return list of users of this type matching the namefilter
    * @throws CmsException if something goes wrong.
    */
    public Vector getUsers(int type, String namefilter) throws CmsException {
        Vector users = new Vector();
        Statement stmt = null;
        ResultSet res = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection();
            stmt = conn.createStatement();
            res = stmt.executeQuery(m_sqlManager.get("C_USERS_GETUSERS_FILTER1") + type + m_sqlManager.get("C_USERS_GETUSERS_FILTER2") + namefilter + m_sqlManager.get("C_USERS_GETUSERS_FILTER3"));

            // create new Cms user objects
            while (res.next()) {
                users.addElement(createCmsUserFromResultSet(res, true));
            }

        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return users;
    }

    /**
     * Gets all users with a certain Lastname.
     *
     * @param lastname      the start of the users lastname
     * @param userType      webuser or systemuser
     * @param userStatus    enabled, disabled
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
            conn = m_sqlManager.getConnection();
            
            if (wasLoggedIn == I_CmsConstants.C_AT_LEAST_ONCE)
                stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_GETUSERS_BY_LASTNAME_ONCE");
            else if (wasLoggedIn == I_CmsConstants.C_NEVER)
                stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_GETUSERS_BY_LASTNAME_NEVER");
            else // C_WHATEVER or whatever else
                stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_GETUSERS_BY_LASTNAME_WHATEVER");

            stmt.setString(1, lastname + "%");
            stmt.setInt(2, userType);
            stmt.setInt(3, userStatus);

            res = stmt.executeQuery();
            // create new Cms user objects
            while (res.next() && (i++ < nMax)) {               
                users.addElement(createCmsUserFromResultSet(res, true));
            }

        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return users;
    }

    /**
     * Returns a list of users of a group.<P/>
     *
     * @param name the name of the group
     * @param type the type of the users to read
     * @return Vector of users
     * @throws CmsException if operation was not successful
     */
    public Vector getUsersOfGroup(String name, int type) throws CmsException {
        Vector users = new Vector();

        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_GETUSERSOFGROUP");
            stmt.setString(1, name);
            stmt.setInt(2, type);

            res = stmt.executeQuery();

            while (res.next()) {
                users.addElement(createCmsUserFromResultSet(res, false));
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return users;
    }
    
    protected void finalize() throws Throwable {
        if (m_sqlManager!=null) {
            m_sqlManager.finalize();
        }
        
        m_sqlManager = null;      
        m_driverManager = null;        
    }
    
    /**
     * @see org.opencms.db.I_CmsUserDriver#destroy()
     */
    public void destroy() throws Throwable {
        finalize();
                
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, "[" + this.getClass().getName() + "] destroyed!");
        }
    }    
	    
    /**
	 * @see org.opencms.db.I_CmsUserDriver#init(source.org.apache.java.util.Configurations, java.lang.String, org.opencms.db.CmsDriverManager)
	 */
	public void init(Configurations config, String dbPoolUrl, CmsDriverManager driverManager) {
        m_sqlManager = this.initQueries(dbPoolUrl);        
        m_driverManager = driverManager;

        String digest = config.getString(I_CmsConstants.C_CONFIGURATION_DB + ".user.digest.type", "MD5");
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". Digest configured    : " + digest);
        }

        m_digestFileEncoding = config.getString(I_CmsConstants.C_CONFIGURATION_DB + ".user.digest.encoding", "UTF-8");
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
        
        if (I_CmsLogChannels.C_LOGGING && A_OpenCms.isLogging()) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". User driver init     : ok");
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#initQueries(java.lang.String)
     */   
    public org.opencms.db.generic.CmsSqlManager initQueries(String dbPoolUrl) {
        return new org.opencms.db.generic.CmsSqlManager(dbPoolUrl);
    }

    /**
     * Checks if a user is member of a group.<P/>
     *
     * @param userId the id of the user to check
     * @param groupId the id of the group to check
     * @return true if user is member of group
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
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_USERINGROUP");

            stmt.setString(1, groupId.toString());
            stmt.setString(2, userId.toString());
            res = stmt.executeQuery();
            if (res.next()) {
                userInGroup = true;
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return userInGroup;
    }

   /**
    * Returns a group object.<P/>
    * @param groupId the id of the group that is to be read
    * @return the CmsGroup object.
    * @throws CmsException if operation was not successful
    */
    public CmsGroup readGroup(CmsUUID groupId) throws CmsException {
        CmsGroup group = null;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_READGROUP2");

            // read the group from the database
            stmt.setString(1, groupId.toString());
            res = stmt.executeQuery();
            // create new Cms group object
            if (res.next()) {
                group = createCmsGroupFromResultSet(res, true);
            } else {
                throw m_sqlManager.getCmsException(this, null, CmsException.C_NO_GROUP, new Exception(), false);
            }

        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
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
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_READGROUP");

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
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
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
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_READID");

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
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        }
        // a.lucas: catch CmsException here and throw it again.
        // Don't wrap another CmsException around it, since this may cause problems during login.
        catch (CmsException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
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
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_READ");
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
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        }
        catch (CmsException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
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
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_READPW");
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
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        }
        catch (CmsException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
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
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_RECOVERPW");

            stmt.setString(1, digest(password));
            stmt.setString(2, userName);
            stmt.setString(3, digest(recoveryPassword));
            result = stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
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
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_REMOVEUSERFROMGROUP");

            stmt.setString(1, groupId.toString());
            stmt.setString(2, userId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
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
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_SETPW");
            stmt.setString(1, digest(password));
            stmt.setString(2, userName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
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
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_SETRECPW");

            stmt.setString(1, digest(password));
            stmt.setString(2, userName);
            result = stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }

        if (result != 1) {
            // the update wasn't succesfull -> throw exception
            throw new CmsException("[" + this.getClass().getName() + "] new password couldn't be set.");
        }
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
                conn = m_sqlManager.getConnection();
                stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_WRITEGROUP");

                stmt.setString(1, m_sqlManager.validateNull(group.getDescription()));
                stmt.setInt(2, group.getFlags());
                stmt.setString(3, group.getParentId().toString());
                stmt.setString(4, group.getId().toString());
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
            } finally {
                m_sqlManager.closeAll(conn, stmt, null);
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
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_WRITE");
            
            value = serializeAdditionalUserInfo(user.getAdditionalInfo());

            // write data to database
            stmt.setString(1, m_sqlManager.validateNull(user.getDescription()));
            stmt.setString(2, m_sqlManager.validateNull(user.getFirstname()));
            stmt.setString(3, m_sqlManager.validateNull(user.getLastname()));
            stmt.setString(4, m_sqlManager.validateNull(user.getEmail()));
            stmt.setTimestamp(5, new Timestamp(user.getLastlogin()));
            stmt.setTimestamp(6, new Timestamp(user.getLastUsed()));
            stmt.setInt(7, user.getFlags());
            m_sqlManager.setBytes(stmt, 8, value);
            stmt.setString(9, user.getDefaultGroupId().toString());
            stmt.setString(10, m_sqlManager.validateNull(user.getAddress()));
            stmt.setString(11, m_sqlManager.validateNull(user.getSection()));
            stmt.setInt(12, user.getType());
            stmt.setString(13, user.getId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (IOException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SERIALIZATION, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

	//
	//	Access Control Entry
	//
	
	/**
	 * Creates an access control entry.
	 * 
	 * @param acEntry the new entry to write
	 */
	public void createAccessControlEntry(CmsProject project, CmsUUID resource, CmsUUID principal, int allowed, int denied, int flags) throws CmsException {
		
		PreparedStatement stmt = null;
		Connection conn = null;

		try {
			conn = m_sqlManager.getConnection();
			stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_CREATE");

			stmt.setString(1, resource.toString());
			stmt.setString(2, principal.toString());
			stmt.setInt(3, allowed);
			stmt.setInt(4, denied);
			stmt.setInt(5, flags);

			stmt.executeUpdate();
			
		} catch (SQLException e) {
			throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
		} finally {
			m_sqlManager.closeAll(conn, stmt, null);
		}
	}

	/**
	 * Deletes all access control entries belonging to a resource
	 * 
	 * @param resource	the id of the resource
	 * @throws CmsException
	 */
	public void deleteAllAccessControlEntries(CmsProject project, CmsUUID resource) throws CmsException {
		
		PreparedStatement stmt = null;
		Connection conn = null;

		try {
			conn = m_sqlManager.getConnection();
			stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_SETFLAGS_ALL");

			stmt.setInt(1, I_CmsConstants.C_ACCESSFLAGS_DELETED);
			stmt.setString(2, resource.toString());
			
			stmt.executeUpdate();

		} catch (SQLException e) {
			throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
		} finally {
			m_sqlManager.closeAll(conn, stmt, null);
		}		
	}

	/**
	 * Undeletes all access control entries belonging to a resource
	 * 
	 * @param resource	the id of the resource
	 * @throws CmsException
	 */
	public void undeleteAllAccessControlEntries(CmsProject project, CmsUUID resource) throws CmsException {
		
		PreparedStatement stmt = null;
		Connection conn = null;

		try {
			conn = m_sqlManager.getConnection();
			stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_RESETFLAGS_ALL");

			stmt.setInt(1, I_CmsConstants.C_ACCESSFLAGS_DELETED);
			stmt.setString(2, resource.toString());
			
			stmt.executeUpdate();

		} catch (SQLException e) {
			throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
		} finally {
			m_sqlManager.closeAll(conn, stmt, null);
		}		
	}
	
	/**
	 * Removes an access control entry from the database
	 * 
	 * @param resource		the id of the resource	
	 * @param principal		the id of the principal
	 * @throws CmsException
	 */
	public void removeAccessControlEntry(CmsProject project, CmsUUID resource, CmsUUID principal) throws CmsException {

		PreparedStatement stmt = null;
		Connection conn = null;
	
		try {
			conn = m_sqlManager.getConnection();
			stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_REMOVE");
	
			stmt.setString(1, resource.toString());
			stmt.setString(2, principal.toString());
				
			stmt.executeUpdate();
	
		} catch (SQLException e) {
			throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
		} finally {
			m_sqlManager.closeAll(conn, stmt, null);
		}							
	}
	
	/**
	 * Removes all access control entries belonging to a resource from the database
	 * 
	 * @param resource 		the id of the resource
	 * @throws CmsException
	 */
	public void removeAllAccessControlEntries(CmsProject project, CmsUUID resource) throws CmsException {
		
		PreparedStatement stmt = null;
		Connection conn = null;

		try {
			conn = m_sqlManager.getConnection();
			stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_REMOVE_ALL");

			stmt.setString(1, resource.toString());
			
			stmt.executeUpdate();

		} catch (SQLException e) {
			throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
		} finally {
			m_sqlManager.closeAll(conn, stmt, null);
		}			
	}
	
	/**
	 * Writes an access control entry to the cms.
	 * If the entry already exists in the database, it is updated with new values
	 * 
	 * @param acEntry the entry to write
	 */
	public void writeAccessControlEntry(CmsProject project, CmsAccessControlEntry acEntry) throws CmsException {
		
		PreparedStatement stmt = null;
		Connection conn = null;
		ResultSet res = null;
		
		try {
			conn = m_sqlManager.getConnection();
			stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_READ_ENTRY");

			stmt.setString(1, acEntry.getResource().toString());
			stmt.setString(2, acEntry.getPrincipal().toString());

			res = stmt.executeQuery();
			if (!res.next()) {
				
				createAccessControlEntry(project, acEntry.getResource(), acEntry.getPrincipal(), acEntry.getAllowedPermissions(), acEntry.getDeniedPermissions(), acEntry.getFlags());
				return;
			}

			// otherwise update the already existing entry

			stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_UPDATE");

			stmt.setInt(1, acEntry.getAllowedPermissions());
			stmt.setInt(2, acEntry.getDeniedPermissions());
			stmt.setInt(3, acEntry.getFlags());
			stmt.setString(4, acEntry.getResource().toString());
			stmt.setString(5, acEntry.getPrincipal().toString());


			stmt.executeUpdate();
			
		} catch (SQLException e) {
			throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
		} finally {
			m_sqlManager.closeAll(conn, stmt, null);
		}
	}
	
	/**
	 * Reads an access control entry from the cms.
	 * 
	 * @param resource	the id of the resource
	 * @param principal	the id of a group or a user any other entity
	 * @return			an access control entry that defines the permissions of the entity for the given resource
	 */	
	public CmsAccessControlEntry readAccessControlEntry(CmsProject project, CmsUUID resource, CmsUUID principal) throws CmsException {
		
		CmsAccessControlEntry ace = null;
		PreparedStatement stmt = null;
		Connection conn = null;
		ResultSet res = null;
		
		try {
			conn = m_sqlManager.getConnection();
			stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_READ_ENTRY");

			stmt.setString(1, resource.toString());
			stmt.setString(2, principal.toString());

			res = stmt.executeQuery();

			// create new CmsAccessControlEntry
			if (res.next()) {
				ace = createAceFromResultSet(res);
			} else {
				res.close();
				res = null;
				throw new CmsException("[" + this.getClass().getName() + "]", CmsException.C_NOT_FOUND);
			}
			
			return ace;
			
		} catch (SQLException e) {
			throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, true);
		} finally {
			m_sqlManager.closeAll(conn, stmt, null);
		}
	}
	
	/**
	 * Reads all relevant access control entries for a given resource.
	 * If an access control entry is inherited, additionally the flag C_ACCESSFLAGS_INHERITED will be set
	 * in order to signal that this entry does not belong directly to the resource.
	 * 
	 * @param resource		the id of the resource
	 * @param inheritedOnly if set, only entries with the inherit flag are returned
	 * @return				a vector of access control entries defining all permissions for the given resource
	 */
	public Vector getAccessControlEntries(CmsProject project, CmsUUID resource, boolean inheritedOnly) throws CmsException {

		Vector aceList = new Vector();
		PreparedStatement stmt = null;
		Connection conn = null;
		ResultSet res = null;
		
		try {
			conn = m_sqlManager.getConnection();
			stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_READ_ENTRIES");

            String resId = resource.toString();
			stmt.setString(1, resId);

			res = stmt.executeQuery();

			// create new CmsAccessControlEntry and add to list
			while(res.next()) {
				CmsAccessControlEntry ace = createAceFromResultSet(res);
				if ((ace.getFlags() & I_CmsConstants.C_ACCESSFLAGS_DELETED) > 0)
					continue;
					
				if (inheritedOnly && ((ace.getFlags() & I_CmsConstants.C_ACCESSFLAGS_INHERIT) == 0))
					continue;
				
				if (inheritedOnly && ((ace.getFlags() & I_CmsConstants.C_ACCESSFLAGS_INHERIT) > 0))
					ace.setFlags(I_CmsConstants.C_ACCESSFLAGS_INHERITED);
						
				aceList.add(ace);
			}
		
			return aceList;

		} catch (SQLException e) {
			throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
		} finally {
			m_sqlManager.closeAll(conn, stmt, null);
		}
	}
	
	/**
	 * Internal helper method to create an access control entry from a database record
	 * 
	 * @param res resultset of the current query
	 * @return a new CmsAccessControlEntry initialized with the values from the current database record.
	 */
    private CmsAccessControlEntry createAceFromResultSet(ResultSet res, CmsUUID newId) throws SQLException {
        // this method is final to allow the java compiler to inline this code!

        return new CmsAccessControlEntry(
            newId, new CmsUUID(res.getString(m_sqlManager.get("C_ACCESS_PRINCIPAL_ID"))),
            res.getInt(m_sqlManager.get("C_ACCESS_ACCESS_ALLOWED")),
            res.getInt(m_sqlManager.get("C_ACCESS_ACCESS_DENIED")),
            res.getInt(m_sqlManager.get("C_ACCESS_ACCESS_FLAGS"))
        );
    }	
    
    /**
     * Internal helper method to create an access control entry from a database record
     * 
     * @param res resultset of the current query
     * @return a new CmsAccessControlEntry initialized with the values from the current database record.
     */
    private CmsAccessControlEntry createAceFromResultSet(ResultSet res) throws SQLException {
        // this method is final to allow the java compiler to inline this code!

        return new CmsAccessControlEntry(
            new CmsUUID(res.getString(m_sqlManager.get("C_ACCESS_RESOURCE_ID"))),
            new CmsUUID(res.getString(m_sqlManager.get("C_ACCESS_PRINCIPAL_ID"))),
            res.getInt(m_sqlManager.get("C_ACCESS_ACCESS_ALLOWED")),
            res.getInt(m_sqlManager.get("C_ACCESS_ACCESS_DENIED")),
            res.getInt(m_sqlManager.get("C_ACCESS_ACCESS_FLAGS"))
        );
    }    
	
	/**
	 * Publish all access control entries of a resource from the given project to the online project.
	 * Within the given project, the resource is identified by its offlineId, in the online project,
	 * it is identified by the given onlineId.
	 * 
	 * @param project
	 * @param offlineId
	 * @param onlineId
	 * @throws CmsException
	 */
	public void publishAccessControlEntries(CmsProject offlineProject, CmsProject onlineProject, CmsUUID offlineId, CmsUUID onlineId) throws CmsException {
		PreparedStatement stmt = null;
		Connection conn = null;
		ResultSet res = null;
		
		try {
			conn = m_sqlManager.getConnection();
			stmt = m_sqlManager.getPreparedStatement(conn, offlineProject, "C_ACCESS_READ_ENTRIES");

			stmt.setString(1, offlineId.toString());

			res = stmt.executeQuery();
					
			while(res.next()) {
				
				CmsAccessControlEntry ace = createAceFromResultSet(res, onlineId);
				if ((ace.getFlags() & I_CmsConstants.C_ACCESSFLAGS_DELETED) == 0)
					writeAccessControlEntry(onlineProject, ace);
			}
			
		} catch (SQLException e) {
			throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
		} finally {
			m_sqlManager.closeAll(conn, stmt, null);
		}
	}
}