/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/genericSql/Attic/CmsUserAccess.java,v $
 * Date   : $Date: 2003/05/07 15:32:08 $
 * Version: $Revision: 1.2 $
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
import java.sql.DriverManager;
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
 * @version $Revision: 1.2 $ $Date: 2003/05/07 15:32:08 $
 */
public class CmsUserAccess extends Object implements I_CmsConstants, I_CmsLogChannels, I_CmsUserAccess {

    private static final int DEBUG = 1;

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
    public CmsUserAccess(Configurations config, I_CmsResourceBroker theResourceBroker) {
        m_SqlQueries = initQueries(config);

        m_ResourceBroker = theResourceBroker;

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

        // TODO: the following code should be removed when all methods in this
        // class are switched to the new CmsQueries methods

        // get the standard pool
        m_poolName = config.getString(com.opencms.core.I_CmsConstants.C_CONFIGURATION_RESOURCEBROKER + "." + brokerName + "." + com.opencms.core.I_CmsConstants.C_CONFIGURATIONS_POOL);
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". Database offline pool: " + m_poolName);
        }

        // get the pool for the online resources
        m_poolNameOnline = config.getString(com.opencms.core.I_CmsConstants.C_CONFIGURATION_RESOURCEBROKER + "." + brokerName + ".online." + com.opencms.core.I_CmsConstants.C_CONFIGURATIONS_POOL);
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". Database online pool : " + m_poolNameOnline);
        }

        // get the pool for the backup resources
        m_poolNameBackup = config.getString(com.opencms.core.I_CmsConstants.C_CONFIGURATION_RESOURCEBROKER + "." + brokerName + ".backup." + com.opencms.core.I_CmsConstants.C_CONFIGURATIONS_POOL);
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". Database backup pool : " + m_poolNameBackup);
        }

        // set the default pool for the id generator
        com.opencms.dbpool.CmsIdGenerator.setDefaultPool(m_poolName);

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
     * @throws thorws CmsException if something goes wrong.
     */
    public CmsUser addImportUser(String name, String password, String recoveryPassword, String description, String firstname, String lastname, String email, long lastlogin, long lastused, int flags, Hashtable additionalInfos, CmsGroup defaultGroup, String address, String section, int type) throws CmsException {
        int id = m_SqlQueries.nextPkId("C_TABLE_USERS");
        byte[] value = null;

        Connection con = null;
        PreparedStatement statement = null;

        try {
            // serialize the hashtable
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(bout);
            oout.writeObject(additionalInfos);
            oout.close();
            value = bout.toByteArray();

            // write data to database
            con = DriverManager.getConnection(m_poolName);

            statement = con.prepareStatement(m_SqlQueries.get("C_USERS_ADD"));

            statement.setInt(1, id);
            statement.setString(2, (new CmsUUID()).toString());
            statement.setString(3, name);
            statement.setString(4, m_SqlQueries.validateNull(password));
            statement.setString(5, m_SqlQueries.validateNull(recoveryPassword));
            statement.setString(6, m_SqlQueries.validateNull(description));
            statement.setString(7, m_SqlQueries.validateNull(firstname));
            statement.setString(8, m_SqlQueries.validateNull(lastname));
            statement.setString(9, m_SqlQueries.validateNull(email));
            statement.setTimestamp(10, new Timestamp(lastlogin));
            statement.setTimestamp(11, new Timestamp(lastused));
            statement.setInt(12, flags);
            // TESTFIX (mfoley@iee.org) Old Code: statement.setBytes(12,value);
            m_SqlQueries.setBytes(statement, 13, value);
            statement.setInt(14, defaultGroup.getId());
            statement.setString(15, m_SqlQueries.validateNull(address));
            statement.setString(16, m_SqlQueries.validateNull(section));
            statement.setInt(17, type);
            
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (IOException e) {
            throw new CmsException("[CmsAccessUserInfoMySql/addUserInformation(id,object)]:" + CmsException.C_SERIALIZATION, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
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
        int id = m_SqlQueries.nextPkId("C_TABLE_USERS");
        byte[] value = null;

        Connection con = null;
        PreparedStatement statement = null;

        try {
            // serialize the hashtable
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(bout);
            oout.writeObject(additionalInfos);
            oout.close();
            value = bout.toByteArray();

            // user data is project independent- use a "dummy" project ID to receive
            // a JDBC connection from the offline connection pool
            con = m_SqlQueries.getConnection();

            statement = con.prepareStatement(m_SqlQueries.get("C_USERS_ADD"));

            statement.setInt(1, id);
            statement.setString(2, (new CmsUUID()).toString());
            statement.setString(3, name);

            // crypt the password with MD5
            statement.setString(4, digest(password));

            statement.setString(5, digest(""));
            statement.setString(6, m_SqlQueries.validateNull(description));
            statement.setString(7, m_SqlQueries.validateNull(firstname));
            statement.setString(8, m_SqlQueries.validateNull(lastname));
            statement.setString(9, m_SqlQueries.validateNull(email));
            statement.setTimestamp(10, new Timestamp(lastlogin));
            statement.setTimestamp(11, new Timestamp(lastused));
            statement.setInt(12, flags);

            // TESTFIX (mfoley@iee.org) Old Code: statement.setBytes(12,value);
            m_SqlQueries.setBytes(statement, 13, value);

            statement.setInt(14, defaultGroup.getId());
            statement.setString(15, m_SqlQueries.validateNull(address));
            statement.setString(16, m_SqlQueries.validateNull(section));
            statement.setInt(17, type);

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (IOException e) {
            throw new CmsException("[CmsAccessUserInfoMySql/addUserInformation(id,object)]:" + CmsException.C_SERIALIZATION, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
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
    public void addUserToGroup(int userid, int groupid) throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;

        // check if user is already in group
        if (!userInGroup(userid, groupid)) {
            // if not, add this user to the group
            try {
                // user data is project independent- use a "dummy" project ID to receive
                // a JDBC connection from the offline connection pool
                con = m_SqlQueries.getConnection();

                // create statement
                statement = con.prepareStatement(m_SqlQueries.get("C_GROUPS_ADDUSERTOGROUP"));

                // write the new assingment to the database
                statement.setInt(1, groupid);
                statement.setInt(2, userid);

                // flag field is not used yet
                statement.setInt(3, C_UNKNOWN_INT);

                statement.executeUpdate();
            } catch (SQLException e) {
                throw new CmsException("[" + getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
            } finally {
                m_SqlQueries.closeAll(con, statement, null);
            }
        }
    }

    /**
     * Changes the user type of the user
     *
     * @param userId The id of the user to change
     * @param userType The new usertype of the user
     */
    public void changeUserType(int userId, int userType) throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            // write data to database
            statement = con.prepareStatement(m_SqlQueries.get("C_USERS_UPDATE_USERTYPE"));
            statement.setInt(1, userType);
            statement.setInt(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
    }

    /**
     * helper for getReadingpermittedGroup. Returns the id of the group that is in
     * any way parent for the other group or -1 for no dependencies between the groups.
     */
    public int checkGroupDependence(int group1, int group2) throws CmsException {

        int id = group1;
        do {
            id = readGroup(id).getParentId();
            if (id == group2) {
                return group1;
            }
        } while (id != C_UNKNOWN_ID);

        id = group2;
        do {
            id = readGroup(id).getParentId();
            if (id == group1) {
                return group2;
            }
        } while (id != C_UNKNOWN_ID);

        return -1;
    }

    /**
     * checks a Vector of Groupids for the Group which can read all files
     *
     * @param groups A Vector with groupids (Integer).
     * @return The id of the group that is in any way parent of all other
     *       group or -1 for no dependencies between the groups.
     */
    public int checkGroupDependence(Vector groups) throws CmsException {
        if ((groups == null) || (groups.size() == 0)) {
            return -1;
        }
        int returnValue = ((Integer) groups.elementAt(0)).intValue();
        for (int i = 1; i < groups.size(); i++) {
            returnValue = checkGroupDependence(returnValue, ((Integer) groups.elementAt(i)).intValue());
            if (returnValue == -1) {
                return -1;
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
    public CmsGroup createGroup(String name, String description, int flags, String parent) throws CmsException {

        int parentId = C_UNKNOWN_ID;
        CmsGroup group = null;

        Connection con = null;
        PreparedStatement statement = null;

        try {

            // get the id of the parent group if nescessary
            if ((parent != null) && (!"".equals(parent))) {
                parentId = readGroup(parent).getId();
            }

            con = DriverManager.getConnection(m_poolName);
            // create statement
            statement = con.prepareStatement(m_SqlQueries.get("C_GROUPS_CREATEGROUP"));

            // write new group to the database
            statement.setInt(1, m_SqlQueries.nextPkId("C_TABLE_GROUPS"));
            statement.setInt(2, parentId);
            statement.setString(3, name);
            statement.setString(4, m_SqlQueries.validateNull(description));
            statement.setInt(5, flags);
            statement.executeUpdate();

            // create the user group by reading it from the database.
            // this is nescessary to get the group id which is generated in the
            // database.
            group = readGroup(name);
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
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
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            // create statement
            statement = con.prepareStatement(m_SqlQueries.get("C_GROUPS_DELETEGROUP"));
            statement.setString(1, delgroup);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
    }

    /**
     * Deletes a user from the database.
     *
     * @param userId The Id of the user to delete
     * @throws thorws CmsException if something goes wrong.
     */
    public void deleteUser(int id) throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_SqlQueries.get("C_USERS_DELETEBYID"));
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
    }

    /**
     * Deletes a user from the database.
     *
     * @param user the user to delete
     * @throws thorws CmsException if something goes wrong.
     */
    public void deleteUser(String name) throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_SqlQueries.get("C_USERS_DELETE"));
            statement.setString(1, name);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
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
        Connection con = null;
        PreparedStatement statement = null;

        try {
            // create the statement
            con = DriverManager.getConnection(m_poolName);

            statement = con.prepareStatement(m_SqlQueries.get("C_PROJECTS_READ_BYGROUP"));

            statement.setInt(1, group.getId());
            statement.setInt(2, group.getId());
            res = statement.executeQuery();

            while (res.next())
                projects.addElement(new CmsProject(res, m_SqlQueries));
        } catch (Exception exc) {
            throw new CmsException("[" + getClass().getName() + "] " + exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
        PreparedStatement statement = null;
        Connection con = null;

        try {
            // create the statement
            con = DriverManager.getConnection(m_poolName);

            statement = con.prepareStatement(m_SqlQueries.get("C_PROJECTS_READ_BYMANAGER"));

            statement.setInt(1, group.getId());
            res = statement.executeQuery();

            while (res.next())
                projects.addElement(new CmsProject(res, m_SqlQueries));
        } catch (Exception exc) {
            throw new CmsException("[" + getClass().getName() + "] " + exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
        PreparedStatement statement = null;
        Connection con = null;

        try {
            // create the statement
            con = DriverManager.getConnection(m_poolName);

            statement = con.prepareStatement(m_SqlQueries.get("C_PROJECTS_READ_BYUSER"));

            statement.setInt(1, user.getId());
            res = statement.executeQuery();

            while (res.next())
                projects.addElement(new CmsProject(res, m_SqlQueries));
        } catch (Exception exc) {
            throw new CmsException("[" + getClass().getName() + "] " + exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
        CmsGroup group = null;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        try {
            // create statement
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_SqlQueries.get("C_GROUPS_GETGROUPS"));

            res = statement.executeQuery();

            // create new Cms group objects
            while (res.next()) {
                group = new CmsGroup(res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_ID")), res.getInt(m_SqlQueries.get("C_GROUPS_PARENT_GROUP_ID")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_NAME")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_DESCRIPTION")), res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_FLAGS")));
                groups.addElement(group);
            }

        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
        CmsGroup group;
        Vector groups = new Vector();

        PreparedStatement statement = null;
        ResultSet res = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);

            //  get all all groups of the user
            statement = con.prepareStatement(m_SqlQueries.get("C_GROUPS_GETGROUPSOFUSER"));
            statement.setString(1, name);

            res = statement.executeQuery();

            while (res.next()) {
                group = new CmsGroup(res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_ID")), res.getInt(m_SqlQueries.get("C_GROUPS_PARENT_GROUP_ID")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_NAME")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_DESCRIPTION")), res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_FLAGS")));
                groups.addElement(group);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
        return groups;
    }

    /**
     * Gets all users of a type.
     *
     * @param type The type of the user.
     * @throws thorws CmsException if something goes wrong.
     */
    public Vector getUsers(int type) throws CmsException {
        Vector users = new Vector();
        PreparedStatement statement = null;
        ResultSet res = null;
        Connection con = null;

        try {
            // user data is project independent- use a "dummy" project ID to receive
            // a JDBC connection from the offline connection pool
            con = m_SqlQueries.getConnection();
            statement = con.prepareStatement(m_SqlQueries.get("C_USERS_GETUSERS"));

            statement.setInt(1, type);
            res = statement.executeQuery();

            // create new Cms user objects
            while (res.next()) {
                // read the additional infos.
                byte[] value = m_SqlQueries.getBytes(res, m_SqlQueries.get("C_USERS_USER_INFO"));
                // now deserialize the object
                ByteArrayInputStream bin = new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                Hashtable info = (Hashtable) oin.readObject();

                CmsUser user =
                    new CmsUser(
                        res.getInt(m_SqlQueries.get("C_USERS_USER_ID")),
                        new CmsUUID(res.getString(m_SqlQueries.get("C_USERS_USER_UUID"))),
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
                        new CmsGroup(res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_ID")), res.getInt(m_SqlQueries.get("C_GROUPS_PARENT_GROUP_ID")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_NAME")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_DESCRIPTION")), res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_FLAGS"))),
                        res.getString(m_SqlQueries.get("C_USERS_USER_ADDRESS")),
                        res.getString(m_SqlQueries.get("C_USERS_USER_SECTION")),
                        res.getInt(m_SqlQueries.get("C_USERS_USER_TYPE")));

                users.addElement(user);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception e) {
            throw new CmsException("[" + getClass().getName() + "]", e);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
        Statement statement = null;
        ResultSet res = null;
        Connection con = null;

        try {
            // user data is project independent- use a "dummy" project ID to receive
            // a JDBC connection from the offline connection pool
            con = m_SqlQueries.getConnection();
            statement = con.createStatement();

            res = statement.executeQuery(m_SqlQueries.get("C_USERS_GETUSERS_FILTER1") + type + m_SqlQueries.get("C_USERS_GETUSERS_FILTER2") + namefilter + m_SqlQueries.get("C_USERS_GETUSERS_FILTER3"));

            // create new Cms user objects
            while (res.next()) {
                // read the additional infos.
                byte[] value = m_SqlQueries.getBytes(res, m_SqlQueries.get("C_USERS_USER_INFO"));
                // now deserialize the object
                ByteArrayInputStream bin = new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                Hashtable info = (Hashtable) oin.readObject();

                CmsUser user =
                    new CmsUser(
                        res.getInt(m_SqlQueries.get("C_USERS_USER_ID")),
                        new CmsUUID(res.getString(m_SqlQueries.get("C_USERS_USER_UUID"))),
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
                        new CmsGroup(res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_ID")), res.getInt(m_SqlQueries.get("C_GROUPS_PARENT_GROUP_ID")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_NAME")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_DESCRIPTION")), res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_FLAGS"))),
                        res.getString(m_SqlQueries.get("C_USERS_USER_ADDRESS")),
                        res.getString(m_SqlQueries.get("C_USERS_USER_SECTION")),
                        res.getInt(m_SqlQueries.get("C_USERS_USER_TYPE")));

                users.addElement(user);
            }

        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception e) {
            throw new CmsException("[" + getClass().getName() + "]", e);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
        PreparedStatement statement = null;
        ResultSet res = null;
        Connection con = null;
        int i = 0;
        // "" =  return (nearly) all users
        if (lastname == null)
            lastname = "";

        try {
            // user data is project independent- use a "dummy" project ID to receive
            // a JDBC connection from the offline connection pool
            con = m_SqlQueries.getConnection();

            if (wasLoggedIn == C_AT_LEAST_ONCE)
                statement = con.prepareStatement(m_SqlQueries.get("C_USERS_GETUSERS_BY_LASTNAME_ONCE"));
            else if (wasLoggedIn == C_NEVER)
                statement = con.prepareStatement(m_SqlQueries.get("C_USERS_GETUSERS_BY_LASTNAME_NEVER"));
            else // C_WHATEVER or whatever else
                statement = con.prepareStatement(m_SqlQueries.get("C_USERS_GETUSERS_BY_LASTNAME_WHATEVER"));

            statement.setString(1, lastname + "%");
            statement.setInt(2, userType);
            statement.setInt(3, userStatus);

            res = statement.executeQuery();
            // create new Cms user objects
            while (res.next() && (i++ < nMax)) {
                // read the additional infos.
                byte[] value = m_SqlQueries.getBytes(res, m_SqlQueries.get("C_USERS_USER_INFO"));
                // now deserialize the object
                ByteArrayInputStream bin = new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                Hashtable info = (Hashtable) oin.readObject();
                CmsUser user =
                    new CmsUser(
                        res.getInt(m_SqlQueries.get("C_USERS_USER_ID")),
                        new CmsUUID(res.getString(m_SqlQueries.get("C_USERS_USER_UUID"))),
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
                        new CmsGroup(res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_ID")), res.getInt(m_SqlQueries.get("C_GROUPS_PARENT_GROUP_ID")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_NAME")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_DESCRIPTION")), res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_FLAGS"))),
                        res.getString(m_SqlQueries.get("C_USERS_USER_ADDRESS")),
                        res.getString(m_SqlQueries.get("C_USERS_USER_SECTION")),
                        res.getInt(m_SqlQueries.get("C_USERS_USER_TYPE")));

                users.addElement(user);
            }

        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception e) {
            throw new CmsException("[" + getClass().getName() + "]", e);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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

        PreparedStatement statement = null;
        ResultSet res = null;
        Connection con = null;

        try {
            // user data is project independent- use a "dummy" project ID to receive
            // a JDBC connection from the offline connection pool
            con = m_SqlQueries.getConnection();

            statement = con.prepareStatement(m_SqlQueries.get("C_GROUPS_GETUSERSOFGROUP"));
            statement.setString(1, name);
            statement.setInt(2, type);

            res = statement.executeQuery();

            while (res.next()) {
                // read the additional infos.
                byte[] value = m_SqlQueries.getBytes(res, m_SqlQueries.get("C_USERS_USER_INFO"));
                // now deserialize the object
                ByteArrayInputStream bin = new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                Hashtable info = (Hashtable) oin.readObject();

                CmsUser user =
                    new CmsUser(
                        res.getInt(m_SqlQueries.get("C_USERS_USER_ID")),
                        new CmsUUID(res.getString(m_SqlQueries.get("C_USERS_USER_UUID"))),
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
                        new CmsGroup(res.getInt(m_SqlQueries.get("C_USERS_USER_DEFAULT_GROUP_ID")), res.getInt(m_SqlQueries.get("C_GROUPS_PARENT_GROUP_ID")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_NAME")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_DESCRIPTION")), res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_FLAGS"))),
                        res.getString(m_SqlQueries.get("C_USERS_USER_ADDRESS")),
                        res.getString(m_SqlQueries.get("C_USERS_USER_SECTION")),
                        res.getInt(m_SqlQueries.get("C_USERS_USER_TYPE")));

                users.addElement(user);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception e) {
            throw new CmsException("[" + getClass().getName() + "]", e);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
        return users;
    }

    public com.opencms.file.genericSql.CmsQueries initQueries(Configurations config) {
        com.opencms.file.genericSql.CmsQueries queries = new com.opencms.file.genericSql.CmsQueries();
        queries.initJdbcPoolUrls(config);

        return queries;
    }

    /**
    * Returns a group object.<P/>
    * @param groupname The id of the group that is to be read.
    * @return Group.
    * @throws CmsException  Throws CmsException if operation was not succesful
    */
    public CmsGroup readGroup(int id) throws CmsException {

        CmsGroup group = null;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);

            // read the group from the database
            statement = con.prepareStatement(m_SqlQueries.get("C_GROUPS_READGROUP2"));
            statement.setInt(1, id);
            res = statement.executeQuery();
            // create new Cms group object
            if (res.next()) {
                group = new CmsGroup(res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_ID")), res.getInt(m_SqlQueries.get("C_GROUPS_PARENT_GROUP_ID")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_NAME")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_DESCRIPTION")), res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_FLAGS")));
            } else {
                throw new CmsException("[" + getClass().getName() + "] " + id, CmsException.C_NO_GROUP);
            }

        } catch (SQLException e) {

            throw new CmsException("[" + getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
        return group;
    }

    /**
     * Returns a group object.<P/>
     * @param groupname The name of the group that is to be read.
     * @return Group.
     * @throws CmsException  Throws CmsException if operation was not succesful
     */
    public CmsGroup readGroup(String groupname) throws CmsException {

        CmsGroup group = null;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            // read the group from the database
            statement = con.prepareStatement(m_SqlQueries.get("C_GROUPS_READGROUP"));
            statement.setString(1, groupname);
            res = statement.executeQuery();

            // create new Cms group object
            if (res.next()) {
                group = new CmsGroup(res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_ID")), res.getInt(m_SqlQueries.get("C_GROUPS_PARENT_GROUP_ID")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_NAME")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_DESCRIPTION")), res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_FLAGS")));
            } else {
                throw new CmsException("[" + getClass().getName() + "] " + groupname, CmsException.C_NO_GROUP);
            }

        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
    public CmsUser readUser(int id) throws CmsException {
        PreparedStatement statement = null;
        ResultSet res = null;
        CmsUser user = null;
        Connection con = null;

        try {
            // user data is project independent- use a "dummy" project ID to receive
            // a JDBC connection from the offline connection pool
            con = m_SqlQueries.getConnection();

            statement = con.prepareStatement(m_SqlQueries.get("C_USERS_READID"));
            statement.setInt(1, id);
            res = statement.executeQuery();

            // create new Cms user object
            if (res.next()) {
                // read the additional infos.
                byte[] value = m_SqlQueries.getBytes(res, m_SqlQueries.get("C_USERS_USER_INFO"));
                // now deserialize the object
                ByteArrayInputStream bin = new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                Hashtable info = (Hashtable) oin.readObject();
                user =
                    new CmsUser(
                        res.getInt(m_SqlQueries.get("C_USERS_USER_ID")),
                        new CmsUUID(res.getString(m_SqlQueries.get("C_USERS_USER_UUID"))),
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
                        new CmsGroup(res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_ID")), res.getInt(m_SqlQueries.get("C_GROUPS_PARENT_GROUP_ID")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_NAME")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_DESCRIPTION")), res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_FLAGS"))),
                        res.getString(m_SqlQueries.get("C_USERS_USER_ADDRESS")),
                        res.getString(m_SqlQueries.get("C_USERS_USER_SECTION")),
                        res.getInt(m_SqlQueries.get("C_USERS_USER_TYPE")));
            } else {
                res.close();
                res = null;
                throw new CmsException("[" + getClass().getName() + "]" + id, CmsException.C_NO_USER);
            }
            return user;
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        }
        // a.lucas: catch CmsException here and throw it again.
        // Don't wrap another CmsException around it, since this may cause problems during login.
        catch (CmsException e) {
            throw e;
        } catch (Exception e) {
            throw new CmsException("[" + getClass().getName() + "]", e);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
    }

    /**
     * Reads a user from the cms.
     *
     * @param name the name of the user.
     * @param type the type of the user.
     * @return the read user.
     * @throws thorws CmsException if something goes wrong.
     */
    public CmsUser readUser(String name, int type) throws CmsException {
        PreparedStatement statement = null;
        ResultSet res = null;
        CmsUser user = null;
        Connection con = null;

        try {
            // user data is project independent- use a "dummy" project ID to receive
            // a JDBC connection from the offline connection pool
            con = m_SqlQueries.getConnection();

            statement = con.prepareStatement(m_SqlQueries.get("C_USERS_READ"));
            statement.setString(1, name);
            statement.setInt(2, type);

            res = statement.executeQuery();

            // create new Cms user object
            if (res.next()) {
                // read the additional infos.
                byte[] value = m_SqlQueries.getBytes(res, m_SqlQueries.get("C_USERS_USER_INFO"));
                // now deserialize the object
                ByteArrayInputStream bin = new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                Hashtable info = (Hashtable) oin.readObject();

                user =
                    new CmsUser(
                        res.getInt(m_SqlQueries.get("C_USERS_USER_ID")),
                        new CmsUUID(res.getString(m_SqlQueries.get("C_USERS_USER_UUID"))),
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
                        new CmsGroup(res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_ID")), res.getInt(m_SqlQueries.get("C_GROUPS_PARENT_GROUP_ID")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_NAME")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_DESCRIPTION")), res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_FLAGS"))),
                        res.getString(m_SqlQueries.get("C_USERS_USER_ADDRESS")),
                        res.getString(m_SqlQueries.get("C_USERS_USER_SECTION")),
                        res.getInt(m_SqlQueries.get("C_USERS_USER_TYPE")));
            } else {
                res.close();
                res = null;
                throw new CmsException("[" + getClass().getName() + "]" + name, CmsException.C_NO_USER);
            }

            return user;
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        }
        // a.lucas: catch CmsException here and throw it again.
        // Don't wrap another CmsException around it, since this may cause problems during login.
        catch (CmsException e) {
            throw e;
        } catch (Exception e) {
            throw new CmsException("[" + getClass().getName() + "]", e);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
    }

    /**
     * Reads a user from the cms, only if the password is correct.
     *
     * @param name the name of the user.
     * @param password the password of the user.
     * @param type the type of the user.
     * @return the read user.
     * @throws thorws CmsException if something goes wrong.
     */
    public CmsUser readUser(String name, String password, int type) throws CmsException {
        PreparedStatement statement = null;
        ResultSet res = null;
        CmsUser user = null;
        Connection con = null;

        try {
            // user data is project independent- use a "dummy" project ID to receive
            // a JDBC connection from the offline connection pool
            con = m_SqlQueries.getConnection();

            statement = con.prepareStatement(m_SqlQueries.get("C_USERS_READPW"));
            statement.setString(1, name);
            statement.setString(2, digest(password));
            statement.setInt(3, type);
            res = statement.executeQuery();

            // create new Cms user object
            if (res.next()) {
                // read the additional infos.
                byte[] value = m_SqlQueries.getBytes(res, m_SqlQueries.get("C_USERS_USER_INFO"));
                // now deserialize the object
                ByteArrayInputStream bin = new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                Hashtable info = (Hashtable) oin.readObject();

                user =
                    new CmsUser(
                        res.getInt(m_SqlQueries.get("C_USERS_USER_ID")),
                        new CmsUUID(res.getString(m_SqlQueries.get("C_USERS_USER_UUID"))),
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
                        new CmsGroup(res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_ID")), res.getInt(m_SqlQueries.get("C_GROUPS_PARENT_GROUP_ID")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_NAME")), res.getString(m_SqlQueries.get("C_GROUPS_GROUP_DESCRIPTION")), res.getInt(m_SqlQueries.get("C_GROUPS_GROUP_FLAGS"))),
                        res.getString(m_SqlQueries.get("C_USERS_USER_ADDRESS")),
                        res.getString(m_SqlQueries.get("C_USERS_USER_SECTION")),
                        res.getInt(m_SqlQueries.get("C_USERS_USER_TYPE")));
            } else {
                res.close();
                res = null;
                throw new CmsException("[" + getClass().getName() + "]" + name, CmsException.C_NO_USER);
            }

            return user;
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        }
        // a.lucas: catch CmsException here and throw it again.
        // Don't wrap another CmsException around it, since this may cause problems during login.
        catch (CmsException e) {
            throw e;
        } catch (Exception e) {
            throw new CmsException("[" + getClass().getName() + "]", e);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
    }

    /**
     * Sets the password, only if the user knows the recovery-password.
     *
     * @param user the user to set the password for.
     * @param recoveryPassword the recoveryPassword the user has to know to set the password.
     * @param password the password to set
     * @throws thorws CmsException if something goes wrong.
     */
    public void recoverPassword(String user, String recoveryPassword, String password) throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;

        int result;

        try {
            con = DriverManager.getConnection(m_poolName);

            statement = con.prepareStatement(m_SqlQueries.get("C_USERS_RECOVERPW"));

            statement.setString(1, digest(password));
            statement.setString(2, user);
            statement.setString(3, digest(recoveryPassword));
            result = statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }

        if (result != 1) {
            // the update wasn't succesfull -> throw exception
            throw new CmsException("[" + getClass().getName() + "] the password couldn't be recovered.");
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
    public void removeUserFromGroup(int userid, int groupid) throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            // create statement
            statement = con.prepareStatement(m_SqlQueries.get("C_GROUPS_REMOVEUSERFROMGROUP"));

            statement.setInt(1, groupid);
            statement.setInt(2, userid);
            statement.executeUpdate();

        } catch (SQLException e) {

            throw new CmsException("[" + getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
    }

    /**
     * Sets a new password for a user.
     *
     * @param user the user to set the password for.
     * @param password the password to set
     * @throws thorws CmsException if something goes wrong.
     */
    public void setPassword(String user, String password) throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_SqlQueries.get("C_USERS_SETPW"));
            statement.setString(1, digest(password));
            statement.setString(2, user);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
    }

    /**
     * Sets a new password for a user.
     *
     * @param user the user to set the password for.
     * @param password the recoveryPassword to set
     * @throws thorws CmsException if something goes wrong.
     */
    public void setRecoveryPassword(String user, String password) throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;
        int result;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_SqlQueries.get("C_USERS_SETRECPW"));

            statement.setString(1, digest(password));
            statement.setString(2, user);
            result = statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }

        if (result != 1) {
            // the update wasn't succesfull -> throw exception
            throw new CmsException("[" + getClass().getName() + "] new password couldn't be set.");
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
    public boolean userInGroup(int userid, int groupid) throws CmsException {
        boolean userInGroup = false;
        PreparedStatement statement = null;
        ResultSet res = null;
        Connection con = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            // create statement
            statement = con.prepareStatement(m_SqlQueries.get("C_GROUPS_USERINGROUP"));

            statement.setInt(1, groupid);
            statement.setInt(2, userid);
            res = statement.executeQuery();
            if (res.next()) {
                userInGroup = true;
            }
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            if (group != null) {
                // create statement
                statement = con.prepareStatement(m_SqlQueries.get("C_GROUPS_WRITEGROUP"));
                statement.setString(1, m_SqlQueries.validateNull(group.getDescription()));
                statement.setInt(2, group.getFlags());
                statement.setInt(3, group.getParentId());
                statement.setInt(4, group.getId());
                statement.executeUpdate();

            } else {
                throw new CmsException("[" + getClass().getName() + "] ", CmsException.C_NO_GROUP);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
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
        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            // serialize the hashtable
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(bout);
            oout.writeObject(user.getAdditionalInfo());
            oout.close();
            value = bout.toByteArray();

            // write data to database
            statement = con.prepareStatement(m_SqlQueries.get("C_USERS_WRITE"));

            statement.setString(1, m_SqlQueries.validateNull(user.getDescription()));
            statement.setString(2, m_SqlQueries.validateNull(user.getFirstname()));
            statement.setString(3, m_SqlQueries.validateNull(user.getLastname()));
            statement.setString(4, m_SqlQueries.validateNull(user.getEmail()));
            statement.setTimestamp(5, new Timestamp(user.getLastlogin()));
            statement.setTimestamp(6, new Timestamp(user.getLastUsed()));
            statement.setInt(7, user.getFlags());
            // TESTFIX (mfoley@iee.org) Old Code: statement.setBytes(8,value);
            m_SqlQueries.setBytes(statement, 8, value);
            statement.setInt(9, user.getDefaultGroupId());
            statement.setString(10, m_SqlQueries.validateNull(user.getAddress()));
            statement.setString(11, m_SqlQueries.validateNull(user.getSection()));
            statement.setInt(12, user.getType());
            statement.setInt(13, user.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (IOException e) {
            throw new CmsException("[CmsAccessUserInfoMySql/addUserInformation(id,object)]:" + CmsException.C_SERIALIZATION, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
    }
}
