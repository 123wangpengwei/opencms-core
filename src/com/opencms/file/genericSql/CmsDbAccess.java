/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/genericSql/Attic/CmsDbAccess.java,v $
* Date   : $Date: 2001/10/02 13:04:25 $
* Version: $Revision: 1.217 $
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
import java.sql.*;
import java.security.*;
import java.io.*;
import source.org.apache.java.io.*;
import source.org.apache.java.util.*;

import com.opencms.core.*;
import com.opencms.file.*;
import com.opencms.file.utils.*;
import com.opencms.util.*;
import com.opencms.launcher.*;


/**
 * This is the generic access module to load and store resources from and into
 * the database.
 *
 * @author Andreas Schouten
 * @author Michael Emmerich
 * @author Hanjo Riege
 * @author Anders Fugmann
 * @author Finn Nielsen
 * @version $Revision: 1.217 $ $Date: 2001/10/02 13:04:25 $ *
 */
public class CmsDbAccess implements I_CmsConstants, I_CmsLogChannels {

    /**
     * The name of the pool to use
     */
    protected String m_poolName;

    /**
     * The name of the online pool to use
     */
    protected String m_poolNameOnline;

    /**
     * The name of the backup pool to use
     */
    protected String m_poolNameBackup;

    /**
     * The session-timeout value:
     * currently six hours. After that time the session can't be restored.
     */
    public static long C_SESSION_TIMEOUT = 6 * 60 * 60 * 1000;

    /**
     * The maximum amount of tables.
     */
    protected static int C_MAX_TABLES = 18;

    /**
     * Table-key for max-id
     */
    protected static String C_TABLE_SYSTEMPROPERTIES = "CMS_SYSTEMPROPERTIES";

    /**
     * Table-key for max-id
     */
    protected static String C_TABLE_GROUPS = "CMS_GROUPS";

    /**
     * Table-key for max-id
     */
    protected static String C_TABLE_GROUPUSERS = "CMS_GROUPUSERS";

    /**
     * Table-key for max-id
     */
    protected static String C_TABLE_USERS = "CMS_USERS";

    /**
     * Table-key for max-id
     */
    protected static String C_TABLE_PROJECTS = "CMS_PROJECTS";

    /**
     * Table-key for max-id
     */
    protected static String C_TABLE_RESOURCES = "CMS_RESOURCES";

    /**
     * Table-key for max-id
     */
    protected static String C_TABLE_FILES = "CMS_FILES";

    /**
     * Table-key for max-id
     */
    protected static String C_TABLE_PROPERTYDEF = "CMS_PROPERTYDEF";

    /**
     * Table-key for max-id
     */
    protected static String C_TABLE_PROPERTIES = "CMS_PROPERTIES";

    /**
     * Table-key for max-id
     */
    protected static String C_TABLE_TASK = "CMS_TASKS";

    /**
     * Table-key for max-id
     */
    protected static String C_TABLE_TASKTYPE = "CMS_TASKTYPE";

    /**
     * Table-key for max-id
     */
    protected static String C_TABLE_TASKPAR = "CMS_TASKPAR";

    /**
     * Table-key for max-id
     */
    protected static String C_TABLE_TASKLOG = "CMS_TASKLOG";

    /**
     * Constant to get property from configurations.
     */
    protected static String C_CONFIGURATIONS_DIGEST = "digest";

    /**
     * Constant to get property from configurations.
     */
    protected static String C_CONFIGURATIONS_POOL = "pool";

    /**
     * A array containing all max-ids for the tables.
     */
    protected int[] m_maxIds;

    /**
     * A digest to encrypt the passwords.
     */
    protected MessageDigest m_digest = null;

    /**
     * Storage for all exportpoints
     */
    protected Hashtable m_exportpointStorage=null;

    /**
     * Dummy CmsObject for static export.
     */
    protected CmsObject m_cmsForStaticExport = null;

   /**
     * 'Constants' file.
     */
   protected com.opencms.file.genericSql.CmsQueries m_cq;

    /**
     * Instanciates the access-module and sets up all required modules and connections.
     * @param config The OpenCms configuration.
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsDbAccess(Configurations config)
        throws CmsException {

        // set configurations for the dbpool driver
        com.opencms.dbpool.CmsDriver.setConfigurations(config);

        m_cq = getQueries();

        String rbName = null;
        String digest = null;
        String exportpoint = null;
        String exportpath = null;
        boolean fillDefaults = true;


        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, "[CmsDbAccess] init the dbaccess-module.");
        }

        // read the name of the rb from the properties
        rbName = (String)config.getString(C_CONFIGURATION_RESOURCEBROKER);

        // read the exportpoints
        m_exportpointStorage = new Hashtable();
        int i = 0;
        while ((exportpoint = config.getString(C_EXPORTPOINT + Integer.toString(i))) != null){
            exportpath = config.getString(C_EXPORTPOINT_PATH + Integer.toString(i));
            if (exportpath != null){
                m_exportpointStorage.put(exportpoint, com.opencms.boot.CmsBase.getAbsoluteWebPath(exportpath));
            }
            i++;
        }

        // read all needed parameters from the configuration
        // all needed pools are read in the following method
        getConnectionPools(config, rbName);

        digest = config.getString(C_CONFIGURATION_RESOURCEBROKER + "." + rbName + "." + C_CONFIGURATIONS_DIGEST, "MD5");
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, "[CmsDbAccess] read digest from configurations: " + digest);
        }

        // create the digest
        try {
            m_digest = MessageDigest.getInstance(digest);
            if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, "[CmsDbAccess] digest created, using: " + m_digest.toString() );
            }
        } catch (NoSuchAlgorithmException e){
            if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, "[CmsDbAccess] error creating digest - using clear paswords: " + e.getMessage());
            }
        }

        // have we to fill the default resource like root and guest?
        try {
            if (readProject(C_PROJECT_ONLINE_ID) != null) {
                // online-project exists - no need of filling defaults
                fillDefaults = false;
            }
        } catch(CmsException exc) {
            // ignore the exception - the project was not readable so fill in the defaults
        }
        if(fillDefaults) {
            // YES!
            if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, "[CmsDbAccess] fill default resources");
            }
            fillDefaults();
        }
    }

    /**
     * Gets all necessary connection pools
     * This method can be adjusted for each resourcebroker
     * @param config The configuration
     */
    protected void getConnectionPools(Configurations config, String rbName){
        // get the standard pool
        m_poolName = config.getString(C_CONFIGURATION_RESOURCEBROKER + "." + rbName + "." + C_CONFIGURATIONS_POOL);
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, "[CmsDbAccess] read pool-name from configurations: " + m_poolName);
        }
        //get the pool for the online resources
        m_poolNameOnline = config.getString(C_CONFIGURATION_RESOURCEBROKER + "." + rbName + ".online." + C_CONFIGURATIONS_POOL);
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, "[CmsDbAccess] read online-pool-name from configurations: " + m_poolNameOnline);
        }
        //get the pool for the backup resources
        m_poolNameBackup = config.getString(C_CONFIGURATION_RESOURCEBROKER + "." + rbName + ".backup." + C_CONFIGURATIONS_POOL);
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, "[CmsDbAccess] read backup-pool-name from configurations: " + m_poolNameBackup);
        }
    }

    /**
     * Creates a serializable object in the systempropertys.
     *
     * @param name The name of the property.
     * @param object The property-object.
     *
     * @return object The property-object.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
     public Serializable addSystemProperty(String name, Serializable object)
         throws CmsException {

        byte[] value;
        Connection con = null;
        PreparedStatement statement = null;
         try    {
            // serialize the object
            ByteArrayOutputStream bout= new ByteArrayOutputStream();
            ObjectOutputStream oout=new ObjectOutputStream(bout);
            oout.writeObject(object);
            oout.close();
            value=bout.toByteArray();

            // create the object
            con = DriverManager.getConnection(m_poolName);
            statement=  con.prepareStatement(m_cq.get("C_SYSTEMPROPERTIES_WRITE"));
            statement.setInt(1,nextId(C_TABLE_SYSTEMPROPERTIES));
            statement.setString(2,name);
            statement.setBytes(3,value);
            statement.executeUpdate();
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (IOException e){
            throw new CmsException("["+this.getClass().getName()+"]"+CmsException. C_SERIALIZATION, e);
        }finally {
             // close all db-resources
             if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
             if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
         }
        return readSystemProperty(name);
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
     * @exception thorws CmsException if something goes wrong.
     */
    public CmsUser addUser(String name, String password, String description,
                          String firstname, String lastname, String email,
                          long lastlogin, long lastused, int flags, Hashtable additionalInfos,
                          CmsGroup defaultGroup, String address, String section, int type)
        throws CmsException {
        int id = nextId(C_TABLE_USERS);
        byte[] value=null;

        Connection con = null;
        PreparedStatement statement = null;

        try {
            // serialize the hashtable
            ByteArrayOutputStream bout= new ByteArrayOutputStream();
            ObjectOutputStream oout=new ObjectOutputStream(bout);
            oout.writeObject(additionalInfos);
            oout.close();
            value=bout.toByteArray();

            // write data to database
            con = DriverManager.getConnection(m_poolName);

            statement = con.prepareStatement(m_cq.get("C_USERS_ADD"));

            statement.setInt(1,id);
            statement.setString(2,name);
            // crypt the password with MD5
            statement.setString(3, digest(password));
            statement.setString(4, digest(""));
            statement.setString(5,checkNull(description));
            statement.setString(6,checkNull(firstname));
            statement.setString(7,checkNull(lastname));
            statement.setString(8,checkNull(email));
            statement.setTimestamp(9, new Timestamp(lastlogin));
            statement.setTimestamp(10, new Timestamp(lastused));
            statement.setInt(11,flags);
            statement.setBytes(12,value);
            statement.setInt(13,defaultGroup.getId());
            statement.setString(14,checkNull(address));
            statement.setString(15,checkNull(section));
            statement.setInt(16,type);
            statement.executeUpdate();
         }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        }
        catch (IOException e){
            throw new CmsException("[CmsAccessUserInfoMySql/addUserInformation(id,object)]:"+CmsException. C_SERIALIZATION, e);
        } finally {
            // close all db-resources
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return readUser(id);
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
     * @exception thorws CmsException if something goes wrong.
     */
    public CmsUser addImportUser(String name, String password, String recoveryPassword, String description,
                          String firstname, String lastname, String email,
                          long lastlogin, long lastused, int flags, Hashtable additionalInfos,
                          CmsGroup defaultGroup, String address, String section, int type)
        throws CmsException {
        int id = nextId(C_TABLE_USERS);
        byte[] value=null;

        Connection con = null;
        PreparedStatement statement = null;

        try {
            // serialize the hashtable
            ByteArrayOutputStream bout= new ByteArrayOutputStream();
            ObjectOutputStream oout=new ObjectOutputStream(bout);
            oout.writeObject(additionalInfos);
            oout.close();
            value=bout.toByteArray();

            // write data to database
            con = DriverManager.getConnection(m_poolName);

            statement = con.prepareStatement(m_cq.get("C_USERS_ADD"));

            statement.setInt(1,id);
            statement.setString(2,name);
            statement.setString(3, checkNull(password));
            statement.setString(4, checkNull(recoveryPassword));
            statement.setString(5,checkNull(description));
            statement.setString(6,checkNull(firstname));
            statement.setString(7,checkNull(lastname));
            statement.setString(8,checkNull(email));
            statement.setTimestamp(9, new Timestamp(lastlogin));
            statement.setTimestamp(10, new Timestamp(lastused));
            statement.setInt(11,flags);
            statement.setBytes(12,value);
            statement.setInt(13,defaultGroup.getId());
            statement.setString(14,checkNull(address));
            statement.setString(15,checkNull(section));
            statement.setInt(16,type);
            statement.executeUpdate();
         }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        }
        catch (IOException e){
            throw new CmsException("[CmsAccessUserInfoMySql/addUserInformation(id,object)]:"+CmsException. C_SERIALIZATION, e);
        } finally {
            // close all db-resources
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
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
     * @exception CmsException Throws CmsException if operation was not succesfull.
     */
    public void addUserToGroup(int userid, int groupid)
        throws CmsException {

        Connection con = null;
        PreparedStatement statement = null;

        // check if user is already in group
        if (!userInGroup(userid,groupid)) {
            // if not, add this user to the group
            try {
                // create statement
                con = DriverManager.getConnection(m_poolName);
                statement = con.prepareStatement(m_cq.get("C_GROUPS_ADDUSERTOGROUP"));
                // write the new assingment to the database
                statement.setInt(1,groupid);
                statement.setInt(2,userid);
                // flag field is not used yet
                statement.setInt(3,C_UNKNOWN_INT);
                statement.executeUpdate();

             } catch (SQLException e){

                 throw new CmsException("[" + this.getClass().getName() + "] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
             } finally {
                // close all db-resources
                if(statement != null) {
                     try {
                         statement.close();
                     } catch(SQLException exc) {
                         // nothing to do here
                     }
                }
                if(con != null) {
                     try {
                         con.close();
                     } catch(SQLException exc) {
                         // nothing to do here
                     }
                }
            }
        }
    }

    /**
     * Private helper method for publihing into the filesystem.
     * test if resource must be written to the filesystem
     *
     * @param filename Name of a resource in the OpenCms system.
     * @return key in m_exportpointStorage Hashtable or null.
     */
    protected String checkExport(String filename){

        String key = null;
        String exportpoint = null;
        Enumeration e = m_exportpointStorage.keys();

        while (e.hasMoreElements()) {
          exportpoint = (String)e.nextElement();
          if (filename.startsWith(exportpoint)){
            return exportpoint;
          }
        }
        return key;
    }

    /**
     * Checks, if the String was null or is empty. If this is so it returns " ".
     *
     * This is for oracle-issues, because in oracle an empty string is the same as null.
     * @param value the String to check.
     * @return the value, or " " if needed.
     */
    protected String checkNull(String value) {
        String ret = " ";
        if( (value != null) && (value.length() != 0) ) {
            ret = value;
        }
        return ret;
    }

    /**
     * Deletes all files in CMS_FILES without fileHeader in CMS_RESOURCES
     *
     *
     */
    protected void clearFilesTable() throws CmsException {
        Connection con = null;
        PreparedStatement statementDelete = null;

        try {
            con = DriverManager.getConnection(m_poolName);

            statementDelete = con.prepareStatement(m_cq.get("C_RESOURCES_DELETE_LOST_ID"));
            statementDelete.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            if(statementDelete != null) {
                 try {
                     statementDelete.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
    }

    /**
     * Copies the file.
     *
     * @param project The project in which the resource will be used.
     * @param onlineProject The online project of the OpenCms.
     * @param userId The id of the user who wants to copy the file.
     * @param source The complete path of the sourcefile.
     * @param parentId The parentId of the resource.
     * @param destination The complete path of the destinationfile.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
     public void copyFile(CmsProject project,
                          CmsProject onlineProject,
                          int userId,
                          String source,
                          int parentId,
                          String destination)
         throws CmsException {
         CmsFile file;

         // read sourcefile
         file=readFile(project.getId(),onlineProject.getId(),source);
         // create destination file
         createFile(project,onlineProject,file,userId,parentId,destination, true);
     }

    /**
     * Counts the locked resources in this project.
     *
     * @param project The project to be unlocked.
     * @return the amount of locked resources in this project.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public int countLockedResources(CmsProject project)
        throws CmsException {

        Connection con = null;
        PreparedStatement statement = null;
        ResultSet res = null;
        int retValue;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(project.getId()).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }

        try {
            // create the statement
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_COUNTLOCKED"+usedStatement));
            statement.setInt(1,project.getId());
            res = statement.executeQuery();
            if(res.next()) {
                retValue = res.getInt(1);
            } else {
                retValue=0;
            }
        } catch( Exception exc ) {
            throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                CmsException.C_SQL_ERROR, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return retValue;
    }

    /**
     * Returns the amount of properties for a propertydefinition.
     *
     * @param metadef The propertydefinition to test.
     *
     * @return the amount of properties for a propertydefinition.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    protected int countProperties(CmsPropertydefinition metadef)
        throws CmsException {
        ResultSet result = null;
        PreparedStatement statement = null;
        Connection con = null;

        int returnValue;
        try {
            // create statement
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_PROPERTIES_READALL_COUNT"));
            statement.setInt(1, metadef.getId());
            result = statement.executeQuery();

            if( result.next() ) {
                returnValue = result.getInt(1) ;
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + metadef.getName(),
                    CmsException.C_UNKNOWN_EXCEPTION);
            }
        } catch(SQLException exc) {
             throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                CmsException.C_SQL_ERROR, exc);
        }finally {
            // close all db-resources
            if(result != null) {
                 try {
                     result.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return returnValue;
    }

    /**
     * Creates a new file from an given CmsFile object and a new filename.
     *
     * @param project The project in which the resource will be used.
     * @param onlineProject The online project of the OpenCms.
     * @param file The file to be written to the Cms.
     * @param user The Id of the user who changed the resourse.
     * @param parentId The parentId of the resource.
     * @param filename The complete new name of the file (including pathinformation).
     *
     * @return file The created file.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
     public CmsFile createFile(CmsProject project,
                               CmsProject onlineProject,
                               CmsFile file,
                               int userId,
                               int parentId, String filename, boolean copy)

         throws CmsException {
        String usedPool = null;
        String usedStatement = null;
        Connection con = null;
        PreparedStatement statement = null;
        // check the resource name
        if (filename.length() > C_MAX_LENGTH_RESOURCE_NAME){
            throw new CmsException("["+this.getClass().getName()+"] "+"Resourcename too long(>"+C_MAX_LENGTH_RESOURCE_NAME+") ",CmsException.C_BAD_NAME);
        }
        int state=0;
        int modifiedBy = userId;
        long dateModified = System.currentTimeMillis();
        if (project.equals(onlineProject)) {
            state= file.getState();
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
            modifiedBy = file.getResourceLastModifiedBy();
            dateModified = file.getDateLastModified();
        } else {
            state=C_STATE_NEW;
            usedPool = m_poolName;
            usedStatement = "";
        }
        // Test if the file is already there and marked as deleted.
        // If so, delete it.
        // If the file exists already and is not marked as deleted then throw exception
        try {
            CmsFile exFile = readFileHeader(project.getId(),filename);
            throw new CmsException("["+this.getClass().getName()+"] ",CmsException.C_FILE_EXISTS);
        } catch (CmsException e) {
            // if the file is marked as deleted remove it!
            if (e.getType() == CmsException.C_RESOURCE_DELETED) {
                removeFile(project.getId(), filename);
                state=C_STATE_CHANGED;
                //throw new CmsException("["+this.getClass().getName()+"] ",CmsException.C_FILE_EXISTS);
            }
            if (e.getType() == CmsException.C_FILE_EXISTS) {
                throw e;
            }
        }
        // first write the file content
        int newFileId = nextId(m_cq.get("C_TABLE_FILES"+usedStatement));
        int resourceId = nextId(m_cq.get("C_TABLE_RESOURCES"+usedStatement));
        // now write the resource
        try {
            con = DriverManager.getConnection(usedPool);
            // first write the file content
            try {
                statement = con.prepareStatement(m_cq.get("C_FILES_WRITE"+usedStatement));
                statement.setInt(1, newFileId);
                statement.setBytes(2, file.getContents());
                statement.executeUpdate();
                statement.close();
            } catch (SQLException se) {
                if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
                    A_OpenCms.log(C_OPENCMS_CRITICAL, "[CmsAccessFileMySql] " + se.getMessage());
                }
            } finally {
                if(statement != null) {
                    try {
                        statement.close();
                    } catch(SQLException exc) {
                        // nothing to do here
                    }
                }
            }
            // now write the file header
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_WRITE"+usedStatement));
            statement.setInt(1, resourceId);
            statement.setInt(2, parentId);
            statement.setString(3, filename);
            statement.setInt(4, file.getType());
            statement.setInt(5, file.getFlags());
            statement.setInt(6, file.getOwnerId());
            statement.setInt(7, file.getGroupId());
            statement.setInt(8, project.getId());
            statement.setInt(9, newFileId);
            statement.setInt(10, file.getAccessFlags());
            statement.setInt(11, state);
            statement.setInt(12, file.isLockedBy());
            statement.setInt(13, file.getLauncherType());
            statement.setString(14, file.getLauncherClassname());
            statement.setTimestamp(15, new Timestamp(file.getDateCreated()));
            statement.setTimestamp(16, new Timestamp(dateModified));
            statement.setInt(17, file.getLength());
            statement.setInt(18, modifiedBy);
            statement.executeUpdate();
            statement.close();
         } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
         } finally {
            // close all db-resources
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return readFile(project.getId(), onlineProject.getId(), filename);
      }

    /**
     * Creates a new file with the given content and resourcetype.
     *
     * @param user The user who wants to create the file.
     * @param project The project in which the resource will be used.
     * @param onlineProject The online project of the OpenCms.
     * @param filename The complete name of the new file (including pathinformation).
     * @param flags The flags of this resource.
     * @param parentId The parentId of the resource.
     * @param contents The contents of the new file.
     * @param resourceType The resourceType of the new file.
     *
     * @return file The created file.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile createFile(CmsUser user, CmsProject project, CmsProject onlineProject, String filename, int flags, int parentId, byte[] contents, I_CmsResourceType resourceType) throws CmsException {

        String usedPool = null;
        String usedStatement = null;
        //check the resource name
        if (filename.length() > C_MAX_LENGTH_RESOURCE_NAME){
            throw new CmsException("["+this.getClass().getName()+"] "+"Resourcename too long(>"+C_MAX_LENGTH_RESOURCE_NAME+") ",CmsException.C_BAD_NAME);
        }
        // it is not allowed, that there is no content in the file
        // TODO: check if this can be done in another way:
        if (contents.length == 0) {
            contents = " ".getBytes();
        }
        int state = C_STATE_NEW;
        if (project.equals(onlineProject)) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        // Test if the file is already there and marked as deleted.
        // If so, delete it
        try {
            readFileHeader(project.getId(), filename);
            throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_FILE_EXISTS);
        } catch (CmsException e) {
            // if the file is maked as deleted remove it!
            if (e.getType() == CmsException.C_RESOURCE_DELETED) {
                removeFile(project.getId(), filename);
                state = C_STATE_CHANGED;
                //throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_FILE_EXISTS);
            }
            if (e.getType() == CmsException.C_FILE_EXISTS) {
                throw e;
            }
        }
        int resourceId = nextId(m_cq.get("C_TABLE_RESOURCES"+usedStatement));
        int fileId = nextId(m_cq.get("C_TABLE_FILES"+usedStatement));
        Connection con = null;
        PreparedStatement statement = null;
        PreparedStatement statementFileWrite = null;
        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_WRITE"+usedStatement));
            // write new resource to the database
            statement.setInt(1, resourceId);
            statement.setInt(2, parentId);
            statement.setString(3, filename);
            statement.setInt(4, resourceType.getResourceType());
            statement.setInt(5, flags);
            statement.setInt(6, user.getId());
            statement.setInt(7, user.getDefaultGroupId());
            statement.setInt(8, project.getId());
            statement.setInt(9, fileId);
            statement.setInt(10, C_ACCESS_DEFAULT_FLAGS);
            statement.setInt(11, state);
            statement.setInt(12, C_UNKNOWN_ID);
            statement.setInt(13, resourceType.getLauncherType());
            statement.setString(14, resourceType.getLauncherClass());
            statement.setTimestamp(15, new Timestamp(System.currentTimeMillis()));
            statement.setTimestamp(16, new Timestamp(System.currentTimeMillis()));
            statement.setInt(17, contents.length);
            statement.setInt(18, user.getId());
            statement.executeUpdate();
            statement.close();
            // write the file content
            statementFileWrite = con.prepareStatement(m_cq.get("C_FILES_WRITE"+usedStatement));
            statementFileWrite.setInt(1, fileId);
            statementFileWrite.setBytes(2, contents);
            statementFileWrite.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statementFileWrite != null) {
                 try {
                     statementFileWrite.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return readFile(project.getId(), onlineProject.getId(), filename);
    }

    /**
     * Creates a new folder
     *
     * @param user The user who wants to create the folder.
     * @param project The project in which the resource will be used.
     * @param parentId The parentId of the folder.
     * @param fileId The fileId of the folder.
     * @param foldername The complete path to the folder in which the new folder will
     * be created.
     * @param flags The flags of this resource.
     *
     * @return The created folder.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public CmsFolder createFolder(CmsUser user, CmsProject project, int parentId, int fileId, String foldername, int flags) throws CmsException {

        CmsFolder oldFolder = null;
        int state = C_STATE_NEW;
        String usedPool = null;
        String usedStatement = null;

        if (foldername.length() > C_MAX_LENGTH_RESOURCE_NAME){
            throw new CmsException("["+this.getClass().getName()+"] "+"Resourcename too long(>"+C_MAX_LENGTH_RESOURCE_NAME+") ",CmsException.C_BAD_NAME);
        }
        //int onlineProject = getOnlineProject(project.getId());
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        // Test if the folder is already there and marked as deleted.
        // If so, delete it
        // No, dont delete it, throw exception (h.riege, 04.01.01)
        try {
            oldFolder = readFolder(project.getId(), foldername);
            if (oldFolder.getState() == C_STATE_DELETED) {
                //removeFolder(oldFolder);
                //state = C_STATE_CHANGED;
                throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_FILE_EXISTS);
            } else {
                if (oldFolder != null){
                    throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_FILE_EXISTS);
                }
            }
        } catch (CmsException e) {
            if (e.getType() == CmsException.C_FILE_EXISTS) {
                throw e;
            }
        }
        int resourceId = nextId(m_cq.get("C_TABLE_RESOURCES"+usedStatement));
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = DriverManager.getConnection(usedPool);
            // write new resource to the database
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_WRITE"+usedStatement));
            statement.setInt(1, resourceId);
            statement.setInt(2, parentId);
            statement.setString(3, foldername);
            statement.setInt(4, C_TYPE_FOLDER);
            statement.setInt(5, flags);
            statement.setInt(6, user.getId());
            statement.setInt(7, user.getDefaultGroupId());
            statement.setInt(8, project.getId());
            statement.setInt(9, fileId);
            statement.setInt(10, C_ACCESS_DEFAULT_FLAGS);
            statement.setInt(11, state);
            statement.setInt(12, C_UNKNOWN_ID);
            statement.setInt(13, C_UNKNOWN_LAUNCHER_ID);
            statement.setString(14, C_UNKNOWN_LAUNCHER);
            statement.setTimestamp(15, new Timestamp(System.currentTimeMillis()));
            statement.setTimestamp(16, new Timestamp(System.currentTimeMillis()));
            statement.setInt(17, 0);
            statement.setInt(18, user.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        String parent = new String();
        if (!foldername.equals(C_ROOT)) {
                parent=foldername.substring(0, foldername.length()-1);
                parent=parent.substring(0, parent.lastIndexOf("/")+1);
         }
        // if this is the rootfolder or if the parentfolder is the rootfolder
        // try to create the projectresource
        if (parentId == C_UNKNOWN_ID || parent.equals(C_ROOT)){
            try {
                String rootFolder = null;
                try{
                    rootFolder = readProjectResource(project.getId(), C_ROOT);
                } catch (CmsException exc){
                }
                if (rootFolder == null){
                    createProjectResource(project.getId(), foldername);
                }
            } catch (CmsException e){
                if (e.getType() != CmsException.C_FILE_EXISTS){
                    throw e;
                }
            }
        }
        return readFolder(project.getId(), foldername);
    }

    /**
     * Creates a new folder from an existing folder object.
     *
     * @param user The user who wants to create the folder.
     * @param project The project in which the resource will be used.
     * @param onlineProject The online project of the OpenCms.
     * @param folder The folder to be written to the Cms.
     * @param parentId The parentId of the resource.
     *
     * @param foldername The complete path of the new name of this folder.
     *
     * @return The created folder.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public CmsFolder createFolder(CmsUser user, CmsProject project, CmsProject onlineProject, CmsFolder folder, int parentId, String foldername) throws CmsException {

        String usedPool = null;
        String usedStatement = null;

        if (foldername.length() > C_MAX_LENGTH_RESOURCE_NAME){
            throw new CmsException("["+this.getClass().getName()+"] "+"Resourcename too long(>"+C_MAX_LENGTH_RESOURCE_NAME+") ",CmsException.C_BAD_NAME);
        }

        CmsFolder oldFolder = null;
        int state = 0;
        int modifiedBy = user.getId();
        long dateModified = System.currentTimeMillis();
        if (project.equals(onlineProject)) {
            state = folder.getState();
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
            modifiedBy = folder.getResourceLastModifiedBy();
            dateModified = folder.getDateLastModified();
        } else {
            state = C_STATE_NEW;
            usedPool = m_poolName;
            usedStatement = "";
        }

        // Test if the file is already there and marked as deleted.
        // If so, delete it
        // No, dont delete it, throw exception (h.riege, 04.01.01)
        try {
            oldFolder = readFolder(project.getId(), foldername);
            if (oldFolder.getState() == C_STATE_DELETED) {
                //removeFolder(oldFolder);
                //state = C_STATE_CHANGED;
                throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_FILE_EXISTS);
            } else {
                if (oldFolder != null) {
                    throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_FILE_EXISTS);
                }
            }
        } catch (CmsException e) {
            if (e.getType() == CmsException.C_FILE_EXISTS) {
                throw e;
            }
        }
        int resourceId = nextId(m_cq.get("C_TABLE_RESOURCES"+usedStatement));
        //int fileId = nextId(m_cq.get("C_TABLE_FILES"+usedStatement));
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = DriverManager.getConnection(usedPool);
            // write new resource to the database
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_WRITE"+usedStatement));
            statement.setInt(1, resourceId);
            statement.setInt(2, parentId);
            statement.setString(3, foldername);
            statement.setInt(4, folder.getType());
            statement.setInt(5, folder.getFlags());
            statement.setInt(6, folder.getOwnerId());
            statement.setInt(7, folder.getGroupId());
            statement.setInt(8, project.getId());
            statement.setInt(9, C_UNKNOWN_ID);
            //statement.setInt(9, fileId);
            statement.setInt(10, folder.getAccessFlags());
            statement.setInt(11, state);
            statement.setInt(12, folder.isLockedBy());
            statement.setInt(13, folder.getLauncherType());
            statement.setString(14, folder.getLauncherClassname());
            statement.setTimestamp(15, new Timestamp(folder.getDateCreated()));
            statement.setTimestamp(16, new Timestamp(dateModified));
            statement.setInt(17, 0);
            statement.setInt(18, modifiedBy);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                     // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        // if this is the rootfolder or if the parentfolder is the rootfolder
        // try to create the projectresource
        if (parentId == C_UNKNOWN_ID || folder.getParent().equals(C_ROOT)){
            try {
                String rootFolder = null;
                try{
                    rootFolder = readProjectResource(project.getId(), C_ROOT);
                } catch (CmsException exc){
                }
                if (rootFolder == null){
                    createProjectResource(project.getId(), foldername);
                }
            } catch (CmsException e){
                if (e.getType() != CmsException.C_FILE_EXISTS){
                    throw e;
                }
            }
        }
        return readFolder(project.getId(), foldername);
    }

    /**
     * Creates a new projectResource from an given CmsResource object.
     *
     * @param project The project in which the resource will be used.
     * @param resource The resource to be written to the Cms.
     *
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public void createProjectResource(int projectId, String resourceName) throws CmsException {
        // do not create entries for online-project
        PreparedStatement statement = null;
        Connection con = null;
        try {
            String projectResource = readProjectResource(projectId, resourceName);
            throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_FILE_EXISTS);
        } catch (CmsException e) {
            if (e.getType() == CmsException.C_FILE_EXISTS) {
                throw e;
            }
        }
        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_PROJECTRESOURCES_CREATE"));
            // write new resource to the database
            statement.setInt(1, projectId);
            statement.setString(2, resourceName);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            if (statement != null) {
                try{
                    statement.close();
                } catch (SQLException e){
                }
            }
            if (con != null){
                try{
                    con.close();
                } catch (SQLException e){
                }
            }
        }
    }

    /**
     * Add a new group to the Cms.<BR/>
     *
     * Only the admin can do this.<P/>
     *
     * @param name The name of the new group.
     * @param description The description for the new group.
     * @int flags The flags for the new group.
     * @param name The name of the parent group (or null).
     *
     * @return Group
     *
     * @exception CmsException Throws CmsException if operation was not succesfull.
     */
     public CmsGroup createGroup(String name, String description, int flags,String parent)
         throws CmsException {

         int parentId=C_UNKNOWN_ID;
         CmsGroup group=null;

        Connection con = null;
       PreparedStatement statement = null;

         try{

            // get the id of the parent group if nescessary
            if ((parent != null) && (!"".equals(parent))) {
                parentId=readGroup(parent).getId();
            }

            con = DriverManager.getConnection(m_poolName);
            // create statement
            statement=con.prepareStatement(m_cq.get("C_GROUPS_CREATEGROUP"));

            // write new group to the database
            statement.setInt(1,nextId(C_TABLE_GROUPS));
            statement.setInt(2,parentId);
            statement.setString(3,name);
            statement.setString(4,checkNull(description));
            statement.setInt(5,flags);
            statement.executeUpdate();

            // create the user group by reading it from the database.
            // this is nescessary to get the group id which is generated in the
            // database.
            group=readGroup(name);
         } catch (SQLException e){
              throw new CmsException("[" + this.getClass().getName() + "] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
         }
         return group;
     }

     /**
     * Creates a project.
     *
     * @param owner The owner of this project.
     * @param group The group for this project.
     * @param managergroup The managergroup for this project.
     * @param task The task.
     * @param name The name of the project to create.
     * @param description The description for the new project.
     * @param flags The flags for the project (e.g. archive).
     * @param type the type for the project (e.g. normal).
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsProject createProject(CmsUser owner, CmsGroup group, CmsGroup managergroup,
                                    CmsTask task, String name, String description,
                                    int flags, int type)
        throws CmsException {


        if ((description==null) || (description.length()<1)) {
            description=" ";
        }

        Timestamp createTime = new Timestamp(new java.util.Date().getTime());
        Connection con = null;
        PreparedStatement statement = null;

        int id = nextId(C_TABLE_PROJECTS);

        try {
            con = DriverManager.getConnection(m_poolName);

            // write data to database
            statement = con.prepareStatement(m_cq.get("C_PROJECTS_CREATE"));

            statement.setInt(1,id);
            statement.setInt(2,owner.getId());
            statement.setInt(3,group.getId());
            statement.setInt(4,managergroup.getId());
            statement.setInt(5,task.getId());
            statement.setString(6,name);
            statement.setString(7,description);
            statement.setInt(8,flags);
            statement.setTimestamp(9,createTime);
            // no publish data
            //statement.setNull(10,Types.TIMESTAMP);
            //statement.setInt(11,C_UNKNOWN_ID);
            statement.setInt(10,type);
            statement.executeUpdate();
         }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return readProject(id);
    }

    /**
     * Creates the propertydefinitions for the resource type.<BR/>
     *
     * Only the admin can do this.
     *
     * @param name The name of the propertydefinitions to overwrite.
     * @param resourcetype The resource-type for the propertydefinitions.
     * @param type The type of the propertydefinitions (normal|optional)
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsPropertydefinition createPropertydefinition(String name,
                                                     I_CmsResourceType resourcetype)
        throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;

        try {
            // create the propertydefinition in the offline db
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_PROPERTYDEF_CREATE"));
            statement.setInt(1,nextId(m_cq.get("C_TABLE_PROPERTYDEF")));
            statement.setString(2,name);
            statement.setInt(3,resourcetype.getResourceType());
            statement.executeUpdate();
            statement.close();
            con.close();
            // create the propertydefinition in the online db
            con = DriverManager.getConnection(m_poolNameOnline);
            statement = con.prepareStatement(m_cq.get("C_PROPERTYDEF_CREATE_ONLINE"));
            statement.setInt(1,nextId(m_cq.get("C_TABLE_PROPERTYDEF_ONLINE")));
            statement.setString(2,name);
            statement.setInt(3,resourcetype.getResourceType());
            statement.executeUpdate();
            statement.close();
            con.close();
            // create the propertydefinition in the backup db
            con = DriverManager.getConnection(m_poolNameBackup);
            statement = con.prepareStatement(m_cq.get("C_PROPERTYDEF_CREATE_BACKUP"));
            statement.setInt(1,nextId(m_cq.get("C_TABLE_PROPERTYDEF_BACKUP")));
            statement.setString(2,name);
            statement.setInt(3,resourcetype.getResourceType());
            statement.executeUpdate();
            statement.close();
            con.close();
         } catch( SQLException exc ) {
             throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                CmsException.C_SQL_ERROR, exc);
         }finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
          }
         return(readPropertydefinition(name, resourcetype));
    }

   /**
    * Helper method to serialize the hashtable.
    * This method is used by updateSession() and createSession()
    */
    private byte[] serializeSession(Hashtable data) throws IOException {
        // serialize the hashtable
        byte[] value;
        Hashtable sessionData = (Hashtable) data.remove(C_SESSION_DATA);
        StringBuffer notSerializable = new StringBuffer();
        ByteArrayOutputStream bout= new ByteArrayOutputStream();
        ObjectOutputStream oout=new ObjectOutputStream(bout);

        // first write the user data
        oout.writeObject(data);
        if(sessionData != null) {
            Enumeration keys = sessionData.keys();
            while(keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object sessionValue = sessionData.get(key);
                if( sessionValue instanceof Serializable ) {
                    // this value is serializeable -> write it to the outputstream
                    oout.writeObject(key);
                    oout.writeObject(sessionValue);
                } else {
                    // this object is not serializeable -> remark for warning
                    notSerializable.append(key);
                    notSerializable.append("; ");
                }
            }
        }
        oout.close();
        value=bout.toByteArray();
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() && (notSerializable.length()>0)) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INFO, "[CmsDbAccess] warning, following entrys are not serializeable in the session: " + notSerializable.toString() + ".");
        }
        return value;
    }

    /**
     * This method creates a new session in the database. It is used
     * for sessionfailover.
     *
     * @param sessionId the id of the session.
     * @return data the sessionData.
     */
    public void createSession(String sessionId, Hashtable data)
        throws CmsException {
        byte[] value=null;

        Connection con = null;
        PreparedStatement statement = null;

        try {
                  value = serializeSession(data);

            con = DriverManager.getConnection(m_poolName);

            // write data to database
            statement = con.prepareStatement(m_cq.get("C_SESSION_CREATE"));

            statement.setString(1,sessionId);
            statement.setTimestamp(2,new java.sql.Timestamp(System.currentTimeMillis()));
            statement.setBytes(3,value);
            statement.executeUpdate();
        }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        }
        catch (IOException e){
            throw new CmsException("["+this.getClass().getName()+"]:"+CmsException.C_SERIALIZATION, e);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
    }

    /**
     * Creates a new task.
     * rootId Id of the root task project
     * parentId Id of the parent task
     * tasktype Type of the task
     * ownerId Id of the owner
     * agentId Id of the agent
     * roleId Id of the role
     * taskname Name of the Task
     * wakeuptime Time when the task will be wake up
     * timeout Time when the task times out
     * priority priority of the task
     *
     * @return The Taskobject  of the generated Task
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsTask createTask(int rootId, int parentId, int tasktype,
                               int ownerId, int agentId,int  roleId, String taskname,
                               java.sql.Timestamp wakeuptime, java.sql.Timestamp timeout,
                               int priority)
        throws CmsException {
        int newId = C_UNKNOWN_ID;
        CmsTask task = null;
        Connection con = null;
        PreparedStatement statement = null;
        try {
            newId = nextId(C_TABLE_TASK);
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_TASK_TYPE_COPY"));
            // create task by copying from tasktype table
            statement.setInt(1,newId);
            statement.setInt(2,tasktype);
            statement.executeUpdate();

            task = this.readTask(newId);
            task.setRoot(rootId);
            task.setParent(parentId);

            task.setName(taskname);
            task.setTaskType(tasktype);
            task.setRole(roleId);

            if(agentId==C_UNKNOWN_ID){
                agentId = findAgent(roleId);
            }
            if(agentId == C_UNKNOWN_ID) {
                throw new CmsException(CmsException.C_NO_USER);
            }
            task.setAgentUser(agentId);
            task.setOriginalUser(agentId);
            task.setWakeupTime(wakeuptime);
            task.setTimeOut(timeout);
            task.setPriority(priority);
            task.setPercentage(0);
            task.setState(C_TASK_STATE_STARTED);
            task.setInitiatorUser(ownerId);
            task.setStartTime(new java.sql.Timestamp(System.currentTimeMillis()));
            task.setMilestone(0);
            task = this.writeTask(task);
        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return task;
    }

    /**
     * Deletes all properties for a file or folder.
     *
     * @param resourceId The id of the resource.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public void deleteAllProperties(int projectId, CmsResource resource)
        throws CmsException {

        Connection con = null;
        PreparedStatement statement = null;
        String usedPool = null;
        String usedStatement = null;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            // create statement
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_cq.get("C_PROPERTIES_DELETEALL"+usedStatement));
            statement.setInt(1, resource.getResourceId());
            statement.executeQuery();
        } catch( SQLException exc ) {
            throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                CmsException.C_SQL_ERROR, exc);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
             if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
          }
    }

    /**
     * Deletes all properties for a file or folder.
     *
     * @param resourceId The id of the resource.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public void deleteAllProperties(int projectId, int resourceId)
        throws CmsException {

        Connection con = null;
        PreparedStatement statement = null;
        String usedPool = null;
        String usedStatement = null;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            // create statement
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_cq.get("C_PROPERTIES_DELETEALL"+usedStatement));
            statement.setInt(1, resourceId);
            statement.executeQuery();
        } catch( SQLException exc ) {
            throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                CmsException.C_SQL_ERROR, exc);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
             if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
          }
    }

    /**
     * Deletes the file.
     *
     * @param project The project in which the resource will be used.
     * @param filename The complete path of the file.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public void deleteFile(CmsProject project, String filename)
        throws CmsException {

        Connection con = null;
        PreparedStatement statement = null;
        String usedPool = null;
        String usedStatement = null;
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement =con.prepareStatement(m_cq.get("C_RESOURCES_REMOVE"+usedStatement));
            // mark the file as deleted
            statement.setInt(1,C_STATE_DELETED);
            statement.setInt(2,C_UNKNOWN_ID);
            statement.setString(3, filename);
            //statement.setInt(4,project.getId());
            statement.executeUpdate();
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
    }

    /**
     * Deletes the folder.
     *
     * Only empty folders can be deleted yet.
     *
     * @param project The project in which the resource will be used.
     * @param orgFolder The folder that will be deleted.
     * @param force If force is set to true, all sub-resources will be deleted.
     * If force is set to false, the folder will be deleted only if it is empty.
     * This parameter is not used yet as only empty folders can be deleted!
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public void deleteFolder(CmsProject project, CmsFolder orgFolder, boolean force)
        throws CmsException {
        String usedPool = null;
        String usedStatement = null;
        //CmsProject onlineProject = getOnlineProject(project.getId());
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        // the current implementation only deletes empty folders
        // check if the folder has any files in it
        Vector files= getFilesInFolder(project.getId(), orgFolder);
        files=getUndeletedResources(files);
        if (files.size()==0) {
            // check if the folder has any folders in it
            Vector folders= getSubFolders(project.getId(),orgFolder);
            folders=getUndeletedResources(folders);
            if (folders.size()==0) {
                //this folder is empty, delete it
                Connection con = null;
                PreparedStatement statement = null;
                try {
                    con = DriverManager.getConnection(usedPool);
                    // mark the folder as deleted
                    statement=con.prepareStatement(m_cq.get("C_RESOURCES_REMOVE"+usedStatement));
                    statement.setInt(1,C_STATE_DELETED);
                    statement.setInt(2,C_UNKNOWN_ID);
                    statement.setString(3, orgFolder.getAbsolutePath());
                    statement.executeUpdate();
                } catch (SQLException e){
                    throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
                } finally {
                    if(statement != null) {
                        try {
                            statement.close();
                        } catch(SQLException exc) {
                            // nothing to do here
                        }
                    }
                    if(con != null) {
                        try {
                            con.close();
                        } catch(SQLException exc) {
                            // nothing to do here
                        }
                    }
                }
            } else {
                throw new CmsException("["+this.getClass().getName()+"] "+orgFolder.getAbsolutePath(),CmsException.C_NOT_EMPTY);
            }
        } else {
            throw new CmsException("["+this.getClass().getName()+"] "+orgFolder.getAbsolutePath(),CmsException.C_NOT_EMPTY);
        }
    }

    /**
     * Delete a group from the Cms.<BR/>
     * Only groups that contain no subgroups can be deleted.
     *
     * Only the admin can do this.<P/>
     *
     * @param delgroup The name of the group that is to be deleted.
     * @exception CmsException  Throws CmsException if operation was not succesfull.
     */
    public void deleteGroup(String delgroup)
         throws CmsException {
        Connection con = null;
         PreparedStatement statement = null;
         try {
            con = DriverManager.getConnection(m_poolName);
             // create statement
             statement=con.prepareStatement(m_cq.get("C_GROUPS_DELETEGROUP"));
             statement.setString(1,delgroup);
             statement.executeUpdate();
        } catch (SQLException e){
             throw new CmsException("[" + this.getClass().getName() + "] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
         }
     }

    /**
     * Deletes a project from the cms.
     * Therefore it deletes all files, resources and properties.
     *
     * @param project the project to delete.
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void deleteProject(CmsProject project)
        throws CmsException {

        // delete the resources from project_resources
        deleteAllProjectResources(project.getId());

        // delete all lost files
        clearFilesTable();

        // finally delete the project
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            // create the statement
            statement = con.prepareStatement(m_cq.get("C_PROJECTS_DELETE"));
            statement.setInt(1,project.getId());
            statement.executeUpdate();
        } catch( Exception exc ) {
            throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                CmsException.C_SQL_ERROR, exc);
        } finally {
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
    }

    /**
     * Deletes all properties for a project.
     *
     * @param project The project to delete.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public void deleteProjectProperties(CmsProject project)
        throws CmsException {
/*
        // get all resources of the project
        Vector resources = readResources(project);

        for( int i = 0; i < resources.size(); i++) {
            // delete the properties for each resource in project
            deleteAllProperties( ((CmsResource) resources.elementAt(i)).getResourceId());
        }
*/
        // delete properies with one statement
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            // create statement
            statement = con.prepareStatement(m_cq.get("C_PROPERTIES_DELETEALLPROP"));
            statement.setInt(1, project.getId());
            statement.executeQuery();
        } catch( SQLException exc ) {
            throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                CmsException.C_SQL_ERROR, exc);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
          }

        }

    /**
     * Deletes a specified project
     *
     * @param project The project to be deleted.
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public void deleteProjectResources(CmsProject project)
        throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            // delete all project-resources.
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_DELETE_PROJECT"));
            statement.setInt(1,project.getId());
            statement.executeQuery();
            // delete all project-files.
            //clearFilesTable();
         } catch (SQLException e){
           throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        }finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
          }
    }

    /**
     * delete all projectResource from an given CmsProject object.
     *
     * @param project The project in which the resource is used.
     *
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public void deleteAllProjectResources(int projectId) throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_PROJECTRESOURCES_DELETEALL"));
            // delete all projectResources from the database
            statement.setInt(1, projectId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            if (statement != null) {
                try{
                    statement.close();
                } catch (SQLException e){
                }
            }
            if (con != null){
                try{
                    con.close();
                } catch (SQLException e){
                }
            }
        }
    }

    /**
     * delete a projectResource from an given CmsResource object.
     *
     * @param project The project in which the resource is used.
     * @param resource The resource to be deleted from the Cms.
     *
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public void deleteProjectResource(int projectId, String resourceName)
            throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_PROJECTRESOURCES_DELETE"));
            // delete resource from the database
            statement.setInt(1, projectId);
            statement.setString(2, resourceName);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            if (statement != null) {
                try{
                    statement.close();
                } catch (SQLException e){
                }
            }
            if (con != null){
                try{
                    con.close();
                } catch (SQLException e){
                }
            }
        }
    }

    /**
     * Deletes a property for a file or folder.
     *
     * @param meta The property-name of which the property has to be read.
     * @param resourceId The id of the resource.
     * @param resourceType The Type of the resource.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public void deleteProperty(String meta, int projectId, CmsResource resource, int resourceType)
        throws CmsException {
        String usedPool = null;
        String usedStatement = null;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        CmsPropertydefinition propdef = readPropertydefinition(meta, resourceType);
        if( propdef == null) {
            // there is no propdefinition with the overgiven name for the resource
            throw new CmsException("[" + this.getClass().getName() + "] " + meta,
                CmsException.C_NOT_FOUND);
        } else {
            // delete the metainfo in the db
            Connection con = null;
            PreparedStatement statement = null;
            try {
                // create statement
                con = DriverManager.getConnection(usedPool);
                statement = con.prepareStatement(m_cq.get("C_PROPERTIES_DELETE"+usedStatement));
                statement.setInt(1, propdef.getId());
                statement.setInt(2, resource.getResourceId());
                statement.executeUpdate();
            } catch(SQLException exc) {
                throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                    CmsException.C_SQL_ERROR, exc);
            } finally {
                if(statement != null) {
                    try {
                        statement.close();
                    } catch(SQLException exc) {
                        // nothing to do here
                    }
                }
                if(con != null) {
                    try {
                        con.close();
                    } catch(SQLException exc) {
                        // nothing to do here
                    }
                }
            }
        }
    }

    /**
     * Delete the propertydefinitions for the resource type.<BR/>
     *
     * Only the admin can do this.
     *
     * @param metadef The propertydefinitions to be deleted.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void deletePropertydefinition(CmsPropertydefinition metadef)
        throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;
        try {
            if(countProperties(metadef) != 0) {
                throw new CmsException("[" + this.getClass().getName() + "] " + metadef.getName(),
                    CmsException.C_UNKNOWN_EXCEPTION);
            }
            // delete the propertydef from offline db
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_PROPERTYDEF_DELETE"));
            statement.setInt(1, metadef.getId() );
            statement.executeUpdate();
            statement.close();
            con.close();
            // delete the propertydef from online db
            con = DriverManager.getConnection(m_poolNameOnline);
            statement = con.prepareStatement(m_cq.get("C_PROPERTYDEF_DELETE_ONLINE"));
            statement.setInt(1, metadef.getId() );
            statement.executeUpdate();
            statement.close();
            con.close();
            // delete the propertydef from backup db
            con = DriverManager.getConnection(m_poolNameBackup);
            statement = con.prepareStatement(m_cq.get("C_PROPERTYDEF_DELETE_BACKUP"));
            statement.setInt(1, metadef.getId() );
            statement.executeUpdate();
            statement.close();
            con.close();
         } catch( SQLException exc ) {
             throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                CmsException.C_SQL_ERROR, exc);
         }finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
          }
    }

    /**
     * Private helper method to delete a resource.
     *
     * @param id the id of the resource to delete.
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    protected void deleteResource(CmsResource resource)
        throws CmsException {
        String usedPool = null;
        String usedStatement = null;
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (resource.getProjectId() == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        Connection con = null;
        PreparedStatement statement = null;
        try {
            // delete resource data from database
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_DELETEBYID"+usedStatement));
            statement.setInt(1, resource.getResourceId());
            statement.executeUpdate();
            statement.close();
            // delete the file content
            statement = con.prepareStatement(m_cq.get("C_FILE_DELETE"+usedStatement));
            statement.setInt(1, resource.getFileId());
            statement.executeUpdate();
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
    }

    /**
     * Deletes old sessions.
     */
    public void deleteSessions() {
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_SESSION_DELETE"));
            statement.setTimestamp(1,new java.sql.Timestamp(System.currentTimeMillis() - C_SESSION_TIMEOUT ));

            statement.execute();
         }
        catch (Exception e){
            if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INFO, "[CmsDbAccess] error while deleting old sessions: " + com.opencms.util.Utils.getStackTrace(e));
            }
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
    }

    /**
     * Deletes a serializable object from the systempropertys.
     *
     * @param name The name of the property.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void deleteSystemProperty(String name)
        throws CmsException {

        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = DriverManager.getConnection(m_poolName);
           statement = con.prepareStatement(m_cq.get("C_SYSTEMPROPERTIES_DELETE"));
           statement.setString(1,name);
           statement.executeUpdate();
        }catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        }finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
             if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
          }
    }

    /**
     * Deletes a user from the database.
     *
     * @param userId The Id of the user to delete
     * @exception thorws CmsException if something goes wrong.
     */
    public void deleteUser(int id)
        throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_USERS_DELETEBYID"));
            statement.setInt(1,id);
            statement.executeUpdate();
        }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
    }

    /**
     * Deletes a user from the database.
     *
     * @param user the user to delete
     * @exception thorws CmsException if something goes wrong.
     */
    public void deleteUser(String name)
        throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_USERS_DELETE"));
            statement.setString(1,name);
            statement.executeUpdate();
        }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
             if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
        }
    }

    /**
     * Destroys this access-module
     * @exception throws CmsException if something goes wrong.
     */
    public void destroy()
        throws CmsException {
        try {
            ((com.opencms.dbpool.CmsDriver) DriverManager.getDriver(m_poolName)).destroy();
        } catch(SQLException exc) {
            // destroy not possible - ignoring the exception
        }

        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, "[CmsDbAccess] shutdown complete.");
        }
    }

    /**
     * Private method to encrypt the passwords.
     *
     * @param value The value to encrypt.
     * @return The encrypted value.
     */
    protected String digest(String value) {
        // is there a valid digest?
        if( m_digest != null ) {
            return new String(m_digest.digest(value.getBytes()));
        } else {
            // no digest - use clear passwords
            return value;
        }
    }

    /**
     * Ends a task from the Cms.
     *
     * @param taskid Id of the task to end.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void endTask(int taskId)
        throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;
        try{
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_TASK_END"));
            statement.setInt(1, 100);
            statement.setTimestamp(2,new java.sql.Timestamp(System.currentTimeMillis()));
            statement.setInt(3,taskId);
            statement.executeQuery();

        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
             if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
        }
    }

/**
 * Sets a special CmsObject for the static export.
 *
 * @param cms The cmsObject created for the export.
 */
public void setCmsObjectForStaticExport(CmsObject cms){
    m_cmsForStaticExport = cms;
}

/**
 * Creates a static export to the filesystem
 *
 * @param exportTo The Directory to where the files should be exported.
 * @param res The compleate path of the folder or the resource to be exported.
 * @param projectId the id of the current project.
 * @param onlineId The id of the online project.
 *
 * @exception CmsException if operation was not successful.
 */
public void exportStaticResources(String exportTo, String res, int projectId, int onlineId) throws CmsException {

    // first check if it is a file or a folder
    if (!res.endsWith("/")){
        // it is a file thats easy
        exportStaticResources(exportTo, readFileHeader(projectId, res));
    }else{
        // a folder. we have to export all files in it and all files in the subfolder and so on.
        CmsFolder offlineFolder = readFolder(projectId, res);
        Vector resources = getResourcesInFolder(projectId, offlineFolder);
        for(int i =0; i < resources.size(); i++){
            CmsResource resource = (CmsResource) resources.elementAt(i);
            if (resource.isFile()){
                // TODO: write a new methode for getAllFilesInFolder so we dont have to read them twice
                exportStaticResources(exportTo, readFileHeader(projectId, resource));
            }else{
                // it is a folder so call this again
                exportStaticResources(exportTo, resource.getAbsolutePath(), projectId, onlineId);
            }
        }
    }
}

/**
 * Exports one file to the filesystem.
 *
 * @param exportTo The path to where the file should be exported.
 * @param file The file to be exported.
 */
public void exportStaticResources(String exportTo, CmsFile file) throws CmsException {

    // first check the directory structure
    if(exportTo.endsWith("/")){
        exportTo = exportTo.substring(0, exportTo.length()-1);
    }
    String path = file.getAbsolutePath();

    // is the exportfoleder present?
    File discFolder = new File(exportTo + "/");
    if (!discFolder.exists()){
        throw new CmsException("[" + this.getClass().getName() + "] " + "the export folder does not exist", CmsException.C_BAD_NAME);
    }
    // now check all other folders and ceate them if nessesary
    String folder = path.substring(0, path.lastIndexOf('/'));
    discFolder = new File(exportTo + folder);
    if(!discFolder.exists()){
        if(!discFolder.mkdirs()){
            throw new CmsException("[" + this.getClass().getName() + "] " + "couldnt create all Folders ", CmsException.C_UNKNOWN_EXCEPTION);
        }
    }
    // all folders exist now create the file
    File discFile = new File(exportTo + path);
    try{
        OutputStream outStream = new FileOutputStream(discFile);
        // now put the stream in the faked response object
        ((CmsDummyResponse)m_cmsForStaticExport.getRequestContext().getResponse()).putOutputStream(outStream);
    }catch (Exception e){
        throw new CmsException("[" + this.getClass().getName() + "] " + "couldnt open file "+exportTo+path
            + "  " + e.getMessage(), CmsException.C_UNKNOWN_EXCEPTION);
    }

    // the method showResource from the OpenCms Class
    int launcherId = file.getLauncherType();
    String startTemplateClass = file.getLauncherClassname();
    I_CmsLauncher launcher = m_cmsForStaticExport.getLauncherManager().getLauncher(launcherId);
    if(launcher == null) {
        String errorMessage = "Could not launch file " + file.getName() + ". Launcher for requested launcher ID "
                + launcherId + " could not be found.";
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INFO, "[CmsDbAccess] exportStaticResource() " + errorMessage);
        }
        throw new CmsException(errorMessage, CmsException.C_UNKNOWN_EXCEPTION);
    }

    // set the filename in the dummy request
    ((CmsDummyRequest)m_cmsForStaticExport.getRequestContext().getRequest()).setRequestedResource(file.getAbsolutePath());
    launcher.initlaunch(m_cmsForStaticExport, file, startTemplateClass, null);

}

    /**
     * Private method to init all default-resources
     */
    protected void fillDefaults() throws CmsException
    {
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, "[CmsDbAccess] fillDefaults() starting NOW!");
        }

        // the resourceType "folder" is needed always - so adding it
        //Hashtable resourceTypes = new Hashtable(1);
        //resourceTypes.put(C_TYPE_FOLDER_NAME, new CmsResourceType(C_TYPE_FOLDER, 0, C_TYPE_FOLDER_NAME, ""));

        // sets the last used index of resource types.
        //resourceTypes.put(C_TYPE_LAST_INDEX, new Integer(C_TYPE_FOLDER));

        // add the resource-types to the database
        //addSystemProperty(C_SYSTEMPROPERTY_RESOURCE_TYPE, resourceTypes);

        // set the groups
        CmsGroup guests = createGroup(C_GROUP_GUEST, "the guest-group", C_FLAG_ENABLED, null);
        CmsGroup administrators = createGroup(C_GROUP_ADMIN, "the admin-group", C_FLAG_ENABLED | C_FLAG_GROUP_PROJECTMANAGER, null);
        CmsGroup users = createGroup(C_GROUP_USERS, "the users-group to access the workplace", C_FLAG_ENABLED | C_FLAG_GROUP_ROLE | C_FLAG_GROUP_PROJECTCOWORKER, C_GROUP_GUEST);
        CmsGroup projectleader = createGroup(C_GROUP_PROJECTLEADER, "the projectmanager-group", C_FLAG_ENABLED | C_FLAG_GROUP_PROJECTMANAGER | C_FLAG_GROUP_PROJECTCOWORKER | C_FLAG_GROUP_ROLE, users.getName());

        // add the users
        CmsUser guest = addUser(C_USER_GUEST, "", "the guest-user", " ", " ", " ", 0, 0, C_FLAG_ENABLED, new Hashtable(), guests, " ", " ", C_USER_TYPE_SYSTEMUSER);
        CmsUser admin = addUser(C_USER_ADMIN, "admin", "the admin-user", " ", " ", " ", 0, 0, C_FLAG_ENABLED, new Hashtable(), administrators, " ", " ", C_USER_TYPE_SYSTEMUSER);
        addUserToGroup(guest.getId(), guests.getId());
        addUserToGroup(admin.getId(), administrators.getId());
        writeTaskType(1, 0, "../taskforms/adhoc.asp", "Ad-Hoc", "30308", 1, 1);
        // create the online project
        CmsTask task = createTask(0, 0, 1, // standart project type,
        admin.getId(), admin.getId(), administrators.getId(), C_PROJECT_ONLINE, new java.sql.Timestamp(new java.util.Date().getTime()), new java.sql.Timestamp(new java.util.Date().getTime()), C_TASK_PRIORITY_NORMAL);
        CmsProject online = createProject(admin, guests, projectleader, task, C_PROJECT_ONLINE, "the online-project", C_FLAG_ENABLED, C_PROJECT_TYPE_NORMAL);

        // create the root-folder for the online project
        CmsFolder rootFolder = createFolder(admin, online, C_UNKNOWN_ID, C_UNKNOWN_ID, C_ROOT, 0);
        rootFolder.setGroupId(users.getId());
        writeFolder(online, rootFolder, false);
        // create the task for the setup project
        task = createTask(0, 0, 1, admin.getId(), admin.getId(), administrators.getId(),
                                    "_setupProject", new java.sql.Timestamp(new java.util.Date().getTime()),
                                    new java.sql.Timestamp(new java.util.Date().getTime()),
                                    C_TASK_PRIORITY_NORMAL);

        CmsProject setup = createProject(admin, administrators, administrators, task, "_setupProject",
                                           "Initial site import", C_FLAG_ENABLED, C_PROJECT_TYPE_TEMPORARY);

        // create the root-folder for the offline project
        rootFolder = createFolder(admin, setup, C_UNKNOWN_ID, C_UNKNOWN_ID, C_ROOT, 0);
        rootFolder.setGroupId(users.getId());
        rootFolder.setState(C_STATE_UNCHANGED);
        writeFolder(setup, rootFolder, false);
    }

    /**
     * Finds an agent for a given role (group).
     * @param roleId The Id for the role (group).
     *
     * @return A vector with the tasks
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    protected int findAgent(int roleid)
        throws CmsException {
        int result = C_UNKNOWN_ID;
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet res = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_TASK_FIND_AGENT"));
            statement.setInt(1,roleid);
            res = statement.executeQuery();

            if(res.next()) {
                result = res.getInt(1);
            }
        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } catch( Exception exc ) {
              throw new CmsException(exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
             if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
             if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
             if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
        }
        return result;
    }

    /**
     * Forwards a task to another user.
     *
     * @param taskId The id of the task that will be fowarded.
     * @param newRoleId The new Group the task belongs to
     * @param newUserId User who gets the task.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void forwardTask(int taskId, int newRoleId, int newUserId)
        throws CmsException {

        Connection con = null;
        PreparedStatement statement = null;
        try{
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_TASK_FORWARD"));
            statement.setInt(1,newRoleId);
            statement.setInt(2,newUserId);
            statement.setInt(3,taskId);
            statement.executeUpdate();
        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
    }

    /**
     * Returns all projects, which are accessible by a group.
     *
     * @param group The requesting group.
     *
     * @return a Vector of projects.
     */
     public Vector getAllAccessibleProjectsByGroup(CmsGroup group)
         throws CmsException {
         Vector projects = new Vector();
         ResultSet res = null;
         Connection con = null;
         PreparedStatement statement = null;

         try {
             // create the statement
             con = DriverManager.getConnection(m_poolName);

             statement = con.prepareStatement(m_cq.get("C_PROJECTS_READ_BYGROUP"));

             statement.setInt(1,group.getId());
             statement.setInt(2,group.getId());
             res = statement.executeQuery();

             while(res.next())
                 projects.addElement(new CmsProject(res,m_cq));
         } catch( Exception exc ) {
             throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                 CmsException.C_SQL_ERROR, exc);

         } finally {
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
         return(projects);
     }

    /**
     * Returns all projects, which are manageable by a group.
     *
     * @param group The requesting group.
     *
     * @return a Vector of projects.
     */
     public Vector getAllAccessibleProjectsByManagerGroup(CmsGroup group)
         throws CmsException {
         Vector projects = new Vector();
         ResultSet res = null;
         PreparedStatement statement = null;
         Connection con = null;

         try {
             // create the statement
             con = DriverManager.getConnection(m_poolName);

             statement = con.prepareStatement(m_cq.get("C_PROJECTS_READ_BYMANAGER"));

             statement.setInt(1,group.getId());
             res = statement.executeQuery();

             while(res.next())
                 projects.addElement(new CmsProject(res,m_cq));
         } catch( Exception exc ) {
             throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                 CmsException.C_SQL_ERROR, exc);
         } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
         return(projects);
     }

    /**
     * Returns all projects, which are owned by a user.
     *
     * @param user The requesting user.
     *
     * @return a Vector of projects.
     */
     public Vector getAllAccessibleProjectsByUser(CmsUser user)
         throws CmsException {
         Vector projects = new Vector();
         ResultSet res = null;
         PreparedStatement statement = null;
        Connection con = null;

         try {
             // create the statement
             con = DriverManager.getConnection(m_poolName);

             statement = con.prepareStatement(m_cq.get("C_PROJECTS_READ_BYUSER"));

             statement.setInt(1,user.getId());
             res = statement.executeQuery();

             while(res.next())
                 projects.addElement(new CmsProject(res,m_cq));
         } catch( Exception exc ) {
             throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                 CmsException.C_SQL_ERROR, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
         return(projects);
     }

    /**
     * Returns all projects, with the overgiven state.
     *
     * @param state The state of the projects to read.
     *
     * @return a Vector of projects.
     */
     public Vector getAllProjects(int state)
         throws CmsException {
         Vector projects = new Vector();
         ResultSet res = null;
         PreparedStatement statement = null;
         Connection con = null;

         try {
             // create the statement
             con = DriverManager.getConnection(m_poolName);

             statement = con.prepareStatement(m_cq.get("C_PROJECTS_READ_BYFLAG"));

             statement.setInt(1,state);
             res = statement.executeQuery();

             while(res.next()) {
                 projects.addElement( new CmsProject(res.getInt(m_cq.get("C_PROJECTS_PROJECT_ID")),
                                                    res.getString(m_cq.get("C_PROJECTS_PROJECT_NAME")),
                                                    res.getString(m_cq.get("C_PROJECTS_PROJECT_DESCRIPTION")),
                                                    res.getInt(m_cq.get("C_PROJECTS_TASK_ID")),
                                                    res.getInt(m_cq.get("C_PROJECTS_USER_ID")),
                                                    res.getInt(m_cq.get("C_PROJECTS_GROUP_ID")),
                                                    res.getInt(m_cq.get("C_PROJECTS_MANAGERGROUP_ID")),
                                                    res.getInt(m_cq.get("C_PROJECTS_PROJECT_FLAGS")),
                                                    SqlHelper.getTimestamp(res,m_cq.get("C_PROJECTS_PROJECT_CREATEDATE")),
                                                    res.getInt(m_cq.get("C_PROJECTS_PROJECT_TYPE"))));
             }
         } catch( SQLException exc ) {
             throw new CmsException("[" + this.getClass().getName() + ".getAllProjects(int)] " + exc.getMessage(),
                 CmsException.C_SQL_ERROR, exc);
         } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
         }
         return(projects);
     }

    /**
     * Returns all projects from the history.
     *
     *
     * @return a Vector of projects.
     */
     public Vector getAllBackupProjects() throws CmsException {
         Vector projects = new Vector();
         ResultSet res = null;
         PreparedStatement statement = null;
         Connection con = null;

         try {
             // create the statement
             con = DriverManager.getConnection(m_poolNameBackup);
             statement = con.prepareStatement(m_cq.get("C_PROJECTS_READLAST_BACKUP"));
             statement.setInt(1, 300);
             res = statement.executeQuery();
             while(res.next()) {
                 Vector resources = readBackupProjectResources(res.getInt("VERSION_ID"));
                 projects.addElement( new CmsBackupProject(res.getInt("VERSION_ID"),
                                                    res.getInt("PROJECT_ID"),
                                                    res.getString("PROJECT_NAME"),
                                                    SqlHelper.getTimestamp(res,"PROJECT_PUBLISHDATE"),
                                                    res.getInt("PROJECT_PUBLISHED_BY"),
                                                    res.getString("PROJECT_PUBLISHED_BY_NAME"),
                                                    res.getString("PROJECT_DESCRIPTION"),
                                                    res.getInt("TASK_ID"),
                                                    res.getInt("USER_ID"),
                                                    res.getString("USER_NAME"),
                                                    res.getInt("GROUP_ID"),
                                                    res.getString("GROUP_NAME"),
                                                    res.getInt("MANAGERGROUP_ID"),
                                                    res.getString("MANAGERGROUP_NAME"),
                                                    SqlHelper.getTimestamp(res,"PROJECT_CREATEDATE"),
                                                    res.getInt("PROJECT_TYPE"),
                                                    resources));
             }
         } catch( SQLException exc ) {
             throw new CmsException("[" + this.getClass().getName() + ".getAllBackupProjects()] " + exc.getMessage(),
                 CmsException.C_SQL_ERROR, exc);
         } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
         }
         return(projects);
     }
     /**
     * Returns all child groups of a groups<P/>
     *
     *
     * @param groupname The name of the group.
     * @return users A Vector of all child groups or null.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
     public Vector getChild(String groupname)
      throws CmsException {

         Vector childs = new Vector();
         CmsGroup group;
         CmsGroup parent;
         ResultSet res = null;
         PreparedStatement statement = null;
         Connection con = null;
         try {
             // get parent group
             parent=readGroup(groupname);
            // parent group exists, so get all childs
            if (parent != null) {
                // create statement
                con = DriverManager.getConnection(m_poolName);
                statement=con.prepareStatement(m_cq.get("C_GROUPS_GETCHILD"));

                statement.setInt(1,parent.getId());
                res = statement.executeQuery();
                // create new Cms group objects
                while ( res.next() ) {
                     group=new CmsGroup(res.getInt(m_cq.get("C_GROUPS_GROUP_ID")),
                                  res.getInt(m_cq.get("C_GROUPS_PARENT_GROUP_ID")),
                                  res.getString(m_cq.get("C_GROUPS_GROUP_NAME")),
                                  res.getString(m_cq.get("C_GROUPS_GROUP_DESCRIPTION")),
                                  res.getInt(m_cq.get("C_GROUPS_GROUP_FLAGS")));
                    childs.addElement(group);
                }
             }

         } catch (SQLException e){

            throw new CmsException("[" + this.getClass().getName() + "] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
         } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
         //check if the child vector has no elements, set it to null.
         if (childs.size() == 0) {
             childs=null;
         }
         return childs;
     }

    /**
     * Returns a Vector with all file headers of a folder.<BR/>
     *
     * @param parentFolder The folder to be searched.
     *
     * @return subfiles A Vector with all file headers of the folder.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getFilesInFolder(int projectId, CmsFolder parentFolder)
        throws CmsException {
        Vector files=new Vector();
        CmsResource file=null;
        ResultSet res =null;
        Connection con = null;
        PreparedStatement statement  = null;
        String usedPool = null;
        String usedStatement = null;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            //  get all files in folder
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_GET_FILESINFOLDER"+usedStatement));
            statement.setInt(1, parentFolder.getResourceId());
            statement.setInt(2, projectId);
            res = statement.executeQuery();

            // create new file objects
            while ( res.next() ) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                String resName=res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int projectID=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy= res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                int launcherType= res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                String launcherClass=  res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int resSize= res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");

                file=new CmsFile(resId,parentId,fileId,resName,resType,resFlags,userId,
                                groupId,projectID,accessFlags,state,lockedBy,
                                launcherType,launcherClass,created,modified,modifiedBy,
                                new byte[0],resSize, lockedInProject);

               files.addElement(file);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return files;
    }

    /**
     * Returns a Vector with all resource-names that have set the given property to the given value.
     *
     * @param projectid, the id of the project to test.
     * @param propertydef, the name of the propertydefinition to check.
     * @param property, the value of the property for the resource.
     *
     * @return Vector with all names of resources.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getFilesWithProperty(int projectId, String propertyDefinition, String propertyValue) throws CmsException {
        Vector names = new Vector();
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool = null;
        String usedStatement = null;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_GET_FILES_WITH_PROPERTY"+usedStatement));
            statement.setInt(1, projectId);
            statement.setString(2, propertyValue);
            statement.setString(3, propertyDefinition);
            res = statement.executeQuery();

            // store the result into the vector
            while (res.next()) {
                String result = res.getString(1);
                names.addElement(result);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception exc) {
            throw new CmsException("getFilesWithProperty" + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
             if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
             if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
             if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
             }
        }
        return names;
    }

    /**
     * Reads the complete folder-tree for this project.<BR>
     *
     * @param project The project in which the folders are.
     *
     * @return A Vecor of folders.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public Vector getFolderTree(int projectId) throws CmsException {
        Vector folders = new Vector();
        CmsFolder folder;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool = null;
        String usedStatement = null;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            // read file data from database
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_GET_FOLDERTREE"+usedStatement));
            statement.setInt(1, projectId);
            res = statement.executeQuery();

            // create new file
            while (res.next()) {
                int resId = res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId = res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                String resName = res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));
                int resType = res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags = res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId = res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId = res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int projectID = res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId = res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags = res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state = res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy = res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                /* not needed */ res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                /* not needed */ res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created = SqlHelper.getTimestamp(res, m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified = SqlHelper.getTimestamp(res, m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                /* not needed */ res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                int modifiedBy = res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                folder = new CmsFolder(resId, parentId, fileId, resName, resType, resFlags,
                                       userId, groupId, projectID, accessFlags, state, lockedBy,
                                       created, modified, modifiedBy, lockedInProject);
                folders.addElement(folder);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception ex) {
            throw new CmsException("[" + this.getClass().getName() + "]", ex);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return folders;
    }

     /**
     * Returns all groups<P/>
     *
     * @return users A Vector of all existing groups.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
     public Vector getGroups()
      throws CmsException {
         Vector groups = new Vector();
         CmsGroup group=null;
         ResultSet res = null;
         PreparedStatement statement = null;
         Connection con = null;
         try {
            // create statement
            con = DriverManager.getConnection(m_poolName);
            statement=con.prepareStatement(m_cq.get("C_GROUPS_GETGROUPS"));

            res = statement.executeQuery();

            // create new Cms group objects
            while ( res.next() ) {
                    group=new CmsGroup(res.getInt(m_cq.get("C_GROUPS_GROUP_ID")),
                                       res.getInt(m_cq.get("C_GROUPS_PARENT_GROUP_ID")),
                                       res.getString(m_cq.get("C_GROUPS_GROUP_NAME")),
                                       res.getString(m_cq.get("C_GROUPS_GROUP_DESCRIPTION")),
                                       res.getInt(m_cq.get("C_GROUPS_GROUP_FLAGS")));
                    groups.addElement(group);
             }

         } catch (SQLException e){
             throw new CmsException("[" + this.getClass().getName() + "] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
      return groups;
     }

    /**
     * Returns a list of groups of a user.<P/>
     *
     * @param name The name of the user.
     * @return Vector of groups
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public Vector getGroupsOfUser(String name)
        throws CmsException {
        CmsGroup group;
        Vector groups=new Vector();

        PreparedStatement statement = null;
        ResultSet res = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);

          //  get all all groups of the user
            statement = con.prepareStatement(m_cq.get("C_GROUPS_GETGROUPSOFUSER"));
            statement.setString(1,name);

            res = statement.executeQuery();

            while ( res.next() ) {
                 group=new CmsGroup(res.getInt(m_cq.get("C_GROUPS_GROUP_ID")),
                                  res.getInt(m_cq.get("C_GROUPS_PARENT_GROUP_ID")),
                                  res.getString(m_cq.get("C_GROUPS_GROUP_NAME")),
                                  res.getString(m_cq.get("C_GROUPS_GROUP_DESCRIPTION")),
                                  res.getInt(m_cq.get("C_GROUPS_GROUP_FLAGS")));
                 groups.addElement(group);
             }
         } catch (SQLException e){
              throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return groups;
    }

    /**
     * Retrieves the onlineproject from the database based on the given project.
     *
     * @author Jan Krag
     * @author Anders Fugmann
     * @return com.opencms.file.CmsProject the  onlineproject for the given project.
     * @param projectId int the project id for which to find the online project.
     * @exception CmsException Throws CmsException if the resource is not found, or the database communication went wrong.
     */
    public CmsProject getOnlineProject(int projectId) throws CmsException {
        return readProject(I_CmsConstants.C_PROJECT_ONLINE_ID);
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
        CmsResource res = readFileHeader(projectId, resource);
        int groupId = -1;
        boolean noGroupCanReadThis = false;
        do{
            int flags = res.getAccessFlags();
            if(!((flags & C_ACCESS_PUBLIC_READ ) == C_ACCESS_PUBLIC_READ)){
                if((flags & C_ACCESS_GROUP_READ) == C_ACCESS_GROUP_READ){
                    if((groupId == -1) || (groupId == res.getGroupId())){
                        groupId = res.getGroupId();
                    }else{
                        int result = checkGroupDependence(groupId, res.getGroupId());
                        if(result == -1){
                            noGroupCanReadThis = true;
                        }else{
                            groupId = result;
                        }
                    }
                }else{
                    noGroupCanReadThis = true;
                }
            }
            res = readFileHeader(projectId, res.getParentId());
        }while(!(noGroupCanReadThis || C_ROOT.equals(res.getAbsolutePath())));
        if (noGroupCanReadThis){
            return C_GROUP_ADMIN;
        }
        if(groupId == -1){
            return null;
        }else{
            return readGroup(groupId).getName();
        }
    }
    /**
     * helper for getReadingpermittedGroup. Returns the id of the group that is in
     * any way parent for the other group or -1 for no dependencies between the groups.
     */
    private int checkGroupDependence(int group1,int group2) throws CmsException {

        int id = group1;
        do{
            id = readGroup(id).getParentId();
            if(id == group2){
                return group1;
            }
        }while (id != C_UNKNOWN_ID);

        id = group2;
        do{
            id = readGroup(id).getParentId();
            if(id == group1){
                return group2;
            }
        }while (id != C_UNKNOWN_ID);

        return -1;
    }

    /**
     * checks a Vector of Groupids for the Group which can read all files
     *
     * @param groups A Vector with groupids (Integer).
     * @return The id of the group that is in any way parent of all other
     *       group or -1 for no dependencies between the groups.
     */
    public int checkGroupDependence(Vector groups) throws CmsException{
        if((groups == null) || (groups.size() == 0)){
            return -1;
        }
        int returnValue = ((Integer)groups.elementAt(0)).intValue();
        for (int i=1; i < groups.size(); i++){
            returnValue = checkGroupDependence(returnValue, ((Integer)groups.elementAt(i)).intValue());
            if (returnValue == -1){
                return -1;
            }
        }
        return returnValue;
    }

    /**
     * Reads all resources (including the folders) residing in a folder<BR>
     *
     * @param onlineResource the parent resource id of the online resoure.
     * @param offlineResource the parent resource id of the offline resoure.
     *
     * @return A Vecor of resources.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public Vector getResourcesInFolder(int projectId, CmsFolder offlineResource) throws CmsException {
        Vector resources = new Vector();
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        // first get the folders
        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_GET_FOLDERS_IN_FOLDER"+usedStatement));
            statement.setInt(1, offlineResource.getResourceId());
            statement.setInt(2, projectId);
            res = statement.executeQuery();
            getResourcesInFolderHelper(res, resources);

        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception ex) {
            throw new CmsException("[" + this.getClass().getName() + "]", ex);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }

        // then get the resources
        try {
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_GET_RESOURCES_IN_FOLDER"+usedStatement));
            statement.setInt(1, offlineResource.getResourceId());
            statement.setInt(2, projectId);
            res = statement.executeQuery();
            getResourcesInFolderHelper(res, resources);

        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception ex) {
            throw new CmsException("[" + this.getClass().getName() + "]", ex);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return resources;
    }

    /**
     * Helper for resources in folder to create the resources-vector from result-set.
     *
     * @param res the Result set to get information from.
     * @param resources a Vector to store the created resources in.
     * @throws SQLException if there is an sql-error.
     */
    void getResourcesInFolderHelper(ResultSet res, Vector resources) throws SQLException {
        String lastfile = null;
        CmsResource resource;

        // create new resources
        while (res.next()) {
            int resId = res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
            int parentId = res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
            String resName = res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));

            // only add this folder, if it was not in the last offline-project already
            if (!resName.equals(lastfile)) {
                int resType = res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags = res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId = res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId = res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int projectID = res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId = res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags = res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state = res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy = res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                int launcherType = res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                String launcherClass = res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created = SqlHelper.getTimestamp(res, m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified = SqlHelper.getTimestamp(res, m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int resSize = res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                int modifiedBy = res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                resource = new CmsResource(resId, parentId, fileId, resName, resType, resFlags, userId,
                                            groupId, projectID, accessFlags, state, lockedBy, launcherType,
                                            launcherClass, created, modified, modifiedBy, resSize, lockedInProject);
                resources.addElement(resource);
            }
            lastfile = resName;
        }
    }

    /**
     * Returns a Vector with all subfolders.<BR/>
     *
     * @param parentFolder The folder to be searched.
     *
     * @return Vector with all subfolders for the given folder.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getSubFolders(int projectId, CmsFolder parentFolder)
        throws CmsException {
        Vector folders=new Vector();
        CmsFolder folder=null;
        ResultSet res =null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedStatement;
        String usedPool;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
           usedPool = m_poolNameOnline;
           usedStatement = "_ONLINE";
        } else {
           usedPool = m_poolName;
           usedStatement = "";
        }
        try {
            //  get all subfolders
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_GET_SUBFOLDER"+usedStatement));
            statement.setInt(1, parentFolder.getResourceId());
            statement.setInt(2, projectId);
            res = statement.executeQuery();
            // create new folder objects
            while ( res.next() ) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                String resName=res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int projectID=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy= res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                folder = new CmsFolder(resId,parentId,fileId,resName,resType,resFlags,userId,
                                      groupId,projectID,accessFlags,state,lockedBy,created,
                                      modified,modifiedBy, lockedInProject);
                folders.addElement(folder);
            }

        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch( Exception exc ) {
            throw new CmsException("getSubFolders "+exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return folders;
    }

    /**
     * Get a parameter value for a task.
     *
     * @param task The task.
     * @param parname Name of the parameter.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public String getTaskPar(int taskId, String parname)
        throws CmsException {

        String result = null;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_TASKPAR_GET"));
            statement.setInt(1, taskId);
            statement.setString(2, parname);
            res = statement.executeQuery();
            if(res.next()) {
                result = res.getString(m_cq.get("C_PAR_VALUE"));
            }
        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return result;
    }

    /**
     * Get the template task id fo a given taskname.
     *
     * @param taskName Name of the TAsk
     *
     * @return id from the task template
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public int getTaskType(String taskName)
        throws CmsException {
        int result = 1;

        PreparedStatement statement = null;
        ResultSet res = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_TASK_GET_TASKTYPE"));
            statement.setString(1, taskName);
            res = statement.executeQuery();
            if (res.next()) {
                result = res.getInt("id");
            }
        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return result;
    }

        protected String getTaskTypeConditon(boolean first, int tasktype) {

        String result = "";
        // handle the tasktype for the SQL String
        if(!first){
            result = result+" AND ";
        }

        switch(tasktype)
        {
        case C_TASKS_ALL: {
                result = result + m_cq.get("C_TASK_ROOT") + "<>0";
                break;
            }
        case C_TASKS_OPEN: {
                result = result + m_cq.get("C_TASK_STATE") + "=" + C_TASK_STATE_STARTED;
                break;
            }
        case C_TASKS_ACTIVE: {
                result = result + m_cq.get("C_TASK_STATE") + "=" + C_TASK_STATE_STARTED;
                break;
            }
        case C_TASKS_DONE: {
                result = result + m_cq.get("C_TASK_STATE") + "=" + C_TASK_STATE_ENDED;
                break;
            }
        case C_TASKS_NEW: {
                result = result + m_cq.get("C_TASK_PERCENTAGE") + "='0' AND " +
                        m_cq.get("C_TASK_STATE") + "=" + C_TASK_STATE_STARTED;
                break;
            }
        default:{}
        }

        return result;
    }

    /**
     * Gets all resources that are marked as undeleted.
     * @param resources Vector of resources
     * @return Returns all resources that are markes as deleted
     */
    protected Vector getUndeletedResources(Vector resources) {
        Vector undeletedResources=new Vector();

        for (int i=0;i<resources.size();i++) {
            CmsResource res=(CmsResource)resources.elementAt(i);
            if (res.getState() != C_STATE_DELETED) {
                undeletedResources.addElement(res);
            }
        }

        return undeletedResources;
    }

    /**
     * Gets all users of a type.
     *
     * @param type The type of the user.
     * @exception thorws CmsException if something goes wrong.
     */
    public Vector getUsers(int type)
        throws CmsException {
        Vector users = new Vector();
        PreparedStatement statement = null;
        ResultSet res = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_USERS_GETUSERS"));
            statement.setInt(1,type);
            res = statement.executeQuery();
            // create new Cms user objects
            while( res.next() ) {
                // read the additional infos.
                byte[] value = res.getBytes(m_cq.get("C_USERS_USER_INFO"));
                // now deserialize the object
                ByteArrayInputStream bin= new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                Hashtable info=(Hashtable)oin.readObject();

                CmsUser user = new CmsUser(res.getInt(m_cq.get("C_USERS_USER_ID")),
                                           res.getString(m_cq.get("C_USERS_USER_NAME")),
                                           res.getString(m_cq.get("C_USERS_USER_PASSWORD")),
                                           res.getString(m_cq.get("C_USERS_USER_RECOVERY_PASSWORD")),
                                           res.getString(m_cq.get("C_USERS_USER_DESCRIPTION")),
                                           res.getString(m_cq.get("C_USERS_USER_FIRSTNAME")),
                                           res.getString(m_cq.get("C_USERS_USER_LASTNAME")),
                                           res.getString(m_cq.get("C_USERS_USER_EMAIL")),
                                           SqlHelper.getTimestamp(res,m_cq.get("C_USERS_USER_LASTLOGIN")).getTime(),
                                           SqlHelper.getTimestamp(res,m_cq.get("C_USERS_USER_LASTUSED")).getTime(),
                                           res.getInt(m_cq.get("C_USERS_USER_FLAGS")),
                                           info,
                                           new CmsGroup(res.getInt(m_cq.get("C_GROUPS_GROUP_ID")),
                                                        res.getInt(m_cq.get("C_GROUPS_PARENT_GROUP_ID")),
                                                        res.getString(m_cq.get("C_GROUPS_GROUP_NAME")),
                                                        res.getString(m_cq.get("C_GROUPS_GROUP_DESCRIPTION")),
                                                        res.getInt(m_cq.get("C_GROUPS_GROUP_FLAGS"))),
                                           res.getString(m_cq.get("C_USERS_USER_ADDRESS")),
                                           res.getString(m_cq.get("C_USERS_USER_SECTION")),
                                           res.getInt(m_cq.get("C_USERS_USER_TYPE")));

                users.addElement(user);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (Exception e) {
            throw new CmsException("["+this.getClass().getName()+"]", e);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return users;
    }

     /**
     * Gets all users of a type and namefilter.
     *
     * @param type The type of the user.
     * @param namestart The namefilter
     * @exception thorws CmsException if something goes wrong.
     */
    public Vector getUsers(int type, String namefilter)
        throws CmsException {
        Vector users = new Vector();
        Statement statement = null;
        ResultSet res = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.createStatement();

            //res = statement.executeQuery("SELECT * FROM CMS_USERS,CMS_GROUPS where USER_TYPE = "+type+" and USER_DEFAULT_GROUP_ID = GROUP_ID and USER_NAME like '"+namefilter+"%' ORDER BY USER_NAME");
            res = statement.executeQuery(m_cq.get("C_USERS_GETUSERS_FILTER1")+type+m_cq.get("C_USERS_GETUSERS_FILTER2")+namefilter+m_cq.get("C_USERS_GETUSERS_FILTER3"));

            // create new Cms user objects
            while( res.next() ) {
                // read the additional infos.
                byte[] value = res.getBytes(m_cq.get("C_USERS_USER_INFO"));
                // now deserialize the object
                ByteArrayInputStream bin= new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                Hashtable info=(Hashtable)oin.readObject();

                CmsUser user = new CmsUser(res.getInt(m_cq.get("C_USERS_USER_ID")),
                                           res.getString(m_cq.get("C_USERS_USER_NAME")),
                                           res.getString(m_cq.get("C_USERS_USER_PASSWORD")),
                                           res.getString(m_cq.get("C_USERS_USER_RECOVERY_PASSWORD")),
                                           res.getString(m_cq.get("C_USERS_USER_DESCRIPTION")),
                                           res.getString(m_cq.get("C_USERS_USER_FIRSTNAME")),
                                           res.getString(m_cq.get("C_USERS_USER_LASTNAME")),
                                           res.getString(m_cq.get("C_USERS_USER_EMAIL")),
                                           SqlHelper.getTimestamp(res,m_cq.get("C_USERS_USER_LASTLOGIN")).getTime(),
                                           SqlHelper.getTimestamp(res,m_cq.get("C_USERS_USER_LASTUSED")).getTime(),
                                           res.getInt(m_cq.get("C_USERS_USER_FLAGS")),
                                           info,
                                           new CmsGroup(res.getInt(m_cq.get("C_GROUPS_GROUP_ID")),
                                                        res.getInt(m_cq.get("C_GROUPS_PARENT_GROUP_ID")),
                                                        res.getString(m_cq.get("C_GROUPS_GROUP_NAME")),
                                                        res.getString(m_cq.get("C_GROUPS_GROUP_DESCRIPTION")),
                                                        res.getInt(m_cq.get("C_GROUPS_GROUP_FLAGS"))),
                                           res.getString(m_cq.get("C_USERS_USER_ADDRESS")),
                                           res.getString(m_cq.get("C_USERS_USER_SECTION")),
                                           res.getInt(m_cq.get("C_USERS_USER_TYPE")));

                users.addElement(user);
            }

        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (Exception e) {
            throw new CmsException("["+this.getClass().getName()+"]", e);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return users;
    }

    /**
     * Returns a list of users of a group.<P/>
     *
     * @param name The name of the group.
     * @param type the type of the users to read.
     * @return Vector of users
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public Vector getUsersOfGroup(String name, int type)
        throws CmsException {
        CmsGroup group;
        Vector users = new Vector();

        PreparedStatement statement = null;
        ResultSet res = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_GROUPS_GETUSERSOFGROUP"));
            statement.setString(1,name);
            statement.setInt(2,type);

            res = statement.executeQuery();

            while( res.next() ) {
                // read the additional infos.
                byte[] value = res.getBytes(m_cq.get("C_USERS_USER_INFO"));
                // now deserialize the object
                ByteArrayInputStream bin= new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                Hashtable info=(Hashtable)oin.readObject();

                CmsUser user = new CmsUser(res.getInt(m_cq.get("C_USERS_USER_ID")),
                                           res.getString(m_cq.get("C_USERS_USER_NAME")),
                                           res.getString(m_cq.get("C_USERS_USER_PASSWORD")),
                                           res.getString(m_cq.get("C_USERS_USER_RECOVERY_PASSWORD")),
                                           res.getString(m_cq.get("C_USERS_USER_DESCRIPTION")),
                                           res.getString(m_cq.get("C_USERS_USER_FIRSTNAME")),
                                           res.getString(m_cq.get("C_USERS_USER_LASTNAME")),
                                           res.getString(m_cq.get("C_USERS_USER_EMAIL")),
                                           SqlHelper.getTimestamp(res,m_cq.get("C_USERS_USER_LASTLOGIN")).getTime(),
                                           SqlHelper.getTimestamp(res,m_cq.get("C_USERS_USER_LASTUSED")).getTime(),
                                           res.getInt(m_cq.get("C_USERS_USER_FLAGS")),
                                           info,
                                            new CmsGroup(res.getInt(m_cq.get("C_USERS_USER_DEFAULT_GROUP_ID")),
                                                        res.getInt(m_cq.get("C_GROUPS_PARENT_GROUP_ID")),
                                                        res.getString(m_cq.get("C_GROUPS_GROUP_NAME")),
                                                        res.getString(m_cq.get("C_GROUPS_GROUP_DESCRIPTION")),
                                                        res.getInt(m_cq.get("C_GROUPS_GROUP_FLAGS"))),
                                           res.getString(m_cq.get("C_USERS_USER_ADDRESS")),
                                           res.getString(m_cq.get("C_USERS_USER_SECTION")),
                                           res.getInt(m_cq.get("C_USERS_USER_TYPE")));

                users.addElement(user);
            }
         } catch (SQLException e){
              throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (Exception e) {
            throw new CmsException("["+this.getClass().getName()+"]", e);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
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
     * @exception CmsException if operation was not successful.
     */
    public Vector getUsersByLastname(String lastname, int userType,
                                     int userStatus, int wasLoggedIn, int nMax)
                                     throws CmsException {
        Vector users = new Vector();
        PreparedStatement statement = null;
        ResultSet res = null;
        Connection con = null;
        int i = 0;
        // "" =  return (nearly) all users
        if(lastname == null) lastname = "";

        try {
            con = DriverManager.getConnection(m_poolName);
            //con = DriverManager.getConnection("jdbc:opencmspool:oracle");

            if(wasLoggedIn == C_AT_LEAST_ONCE)
                statement = con.prepareStatement(
                        m_cq.get("C_USERS_GETUSERS_BY_LASTNAME_ONCE"));
            else if(wasLoggedIn == C_NEVER)
                statement = con.prepareStatement(
                        m_cq.get("C_USERS_GETUSERS_BY_LASTNAME_NEVER"));
            else // C_WHATEVER or whatever else
                statement = con.prepareStatement(
                        m_cq.get("C_USERS_GETUSERS_BY_LASTNAME_WHATEVER"));

            statement.setString(1, lastname + "%");
            statement.setInt(2, userType);
            statement.setInt(3, userStatus);

            res = statement.executeQuery();
            // create new Cms user objects
            while( res.next() && (i++ < nMax)) {
                // read the additional infos.
                byte[] value = res.getBytes(m_cq.get("C_USERS_USER_INFO"));
                // now deserialize the object
                ByteArrayInputStream bin= new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                Hashtable info=(Hashtable)oin.readObject();
                CmsUser user = new CmsUser(
                        res.getInt(m_cq.get("C_USERS_USER_ID")),
                        res.getString(m_cq.get("C_USERS_USER_NAME")),
                        res.getString(m_cq.get("C_USERS_USER_PASSWORD")),
                        res.getString(m_cq.get("C_USERS_USER_RECOVERY_PASSWORD")),
                        res.getString(m_cq.get("C_USERS_USER_DESCRIPTION")),
                        res.getString(m_cq.get("C_USERS_USER_FIRSTNAME")),
                        res.getString(m_cq.get("C_USERS_USER_LASTNAME")),
                        res.getString(m_cq.get("C_USERS_USER_EMAIL")),
                        SqlHelper.getTimestamp(res,
                                m_cq.get("C_USERS_USER_LASTLOGIN")).getTime(),
                        SqlHelper.getTimestamp(res,
                                m_cq.get("C_USERS_USER_LASTUSED")).getTime(),
                        res.getInt(m_cq.get("C_USERS_USER_FLAGS")),
                        info,
                        new CmsGroup(res.getInt(m_cq.get("C_GROUPS_GROUP_ID")),
                            res.getInt(m_cq.get("C_GROUPS_PARENT_GROUP_ID")),
                            res.getString(m_cq.get("C_GROUPS_GROUP_NAME")),
                            res.getString(m_cq.get("C_GROUPS_GROUP_DESCRIPTION")),
                            res.getInt(m_cq.get("C_GROUPS_GROUP_FLAGS"))),
                            res.getString(m_cq.get("C_USERS_USER_ADDRESS")),
                            res.getString(m_cq.get("C_USERS_USER_SECTION")),
                            res.getInt(m_cq.get("C_USERS_USER_TYPE")));

                users.addElement(user);
            }

        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+
                    e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (Exception e) {
            throw new CmsException("["+this.getClass().getName()+"]", e);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return users;
    }

    protected int insertTaskPar(int taskId, String parname, String parvalue)
        throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;

        int newId = C_UNKNOWN_ID;

        try {
            con = DriverManager.getConnection(m_poolName);
            newId = nextId(C_TABLE_TASKPAR);
            statement = con.prepareStatement(m_cq.get("C_TASKPAR_INSERT"));
            statement.setInt(1, newId);
            statement.setInt(2, taskId);
            statement.setString(3, parname);
            statement.setString(4, parvalue);
            statement.executeUpdate();
        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            // close all db-resources
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return newId;
    }

    protected int insertTaskType(int autofinish, int escalationtyperef, String htmllink, String name, String permission, int priorityref, int roleref)
        throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;

        int newId = C_UNKNOWN_ID;

        try {
            con = DriverManager.getConnection(m_poolName);
            newId = nextId(C_TABLE_TASKPAR);
            statement = con.prepareStatement(m_cq.get("C_TASKTYPE_INSERT"));
            statement.setInt(1, autofinish);
            statement.setInt(2, escalationtyperef);
            statement.setString(3, htmllink);
            statement.setInt(4, newId);
            statement.setString(5, name);
            statement.setString(6, permission);
            statement.setInt(7, priorityref);
            statement.setInt(8, roleref);
            statement.executeUpdate();
        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            // close all db-resources
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return newId;
    }

    /**
     * Private method to get the next id for a table.
     * This method is synchronized, to generate unique id's.
     *
     * @param key A key for the table to get the max-id from.
     * @return next-id The next possible id for this table.
     */
    protected synchronized int nextId(String key)
        throws CmsException {
        // return the next id for this table
        return com.opencms.dbpool.CmsIdGenerator.nextId(m_poolName, key);
    }

    /**
     * Publishes a specified project to the online project. <br>
     *
     * @param project The project to be published.
     * @param onlineProject The online project of the OpenCms.
     * @return a vector of changed or deleted resources.
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public Vector publishProject(CmsUser user, int projectId, CmsProject onlineProject, boolean enableHistory) throws CmsException
    {
        CmsAccessFilesystem discAccess = new CmsAccessFilesystem(m_exportpointStorage);
        CmsFolder currentFolder = null;
        CmsFile currentFile = null;
        CmsFolder newFolder = null;
        CmsFile newFile = null;
        Vector offlineFolders;
        Vector offlineFiles;
        Vector deletedFolders = new Vector();
        // folderIdIndex:    offlinefolderId   |   onlinefolderId
        Hashtable folderIdIndex = new Hashtable();
        Vector changedResources = new Vector();

        CmsProject currentProject = readProject(projectId);
        int versionId = 1;
        long publishDate = System.currentTimeMillis();
        if (enableHistory){
            // get the version id for the backup
            versionId = getBackupVersionId();
            // store the projectdata to the backuptables for history
            backupProject(currentProject, versionId, publishDate, user);
        }

        // read all folders in offlineProject
        offlineFolders = readFolders(projectId, false, true);
        for (int i = 0; i < offlineFolders.size(); i++){
            currentFolder = ((CmsFolder) offlineFolders.elementAt(i));
            // do not publish the folder if it is locked in another project
            if (currentFolder.isLocked()){
              // in this case do nothing
            // C_STATE_DELETE
            } else if (currentFolder.getState() == C_STATE_DELETED){
                deletedFolders.addElement(currentFolder);
                changedResources.addElement(currentFolder.getAbsolutePath());
                // C_STATE_NEW
            } else if (currentFolder.getState() == C_STATE_NEW){
                // export to filesystem if necessary
                String exportKey = checkExport(currentFolder.getAbsolutePath());
                if (exportKey != null){
                    discAccess.createFolder(currentFolder.getAbsolutePath(), exportKey);
                }
                // get parentId for onlineFolder either from folderIdIndex or from the database
                Integer parentId = (Integer) folderIdIndex.get(new Integer(currentFolder.getParentId()));
                if (parentId == null){
                    CmsFolder currentOnlineParent = readFolder(onlineProject.getId(), currentFolder.getParent());
                    parentId = new Integer(currentOnlineParent.getResourceId());
                    folderIdIndex.put(new Integer(currentFolder.getParentId()), parentId);
                }
                // create the new folder and insert its id in the folderindex
                try {
                    newFolder = createFolder(user, onlineProject, onlineProject, currentFolder, parentId.intValue(), currentFolder.getAbsolutePath());
                    newFolder.setState(C_STATE_UNCHANGED);
                    updateResourcestate(newFolder);
                } catch (CmsException e) {
                    // if the folder already exists in the onlineProject then update the onlineFolder
                    if (e.getType() == CmsException.C_FILE_EXISTS) {
                        CmsFolder onlineFolder = null;
                        try {
                            onlineFolder = readFolder(onlineProject.getId(), currentFolder.getAbsolutePath());
                        } catch (CmsException exc) {
                            throw exc;
                        } // end of catch
                        PreparedStatement statement = null;
                        Connection con = null;
                        try {
                            con = DriverManager.getConnection(m_poolNameOnline);
                            // update the onlineFolder with data from offlineFolder
                            statement = con.prepareStatement(m_cq.get("C_RESOURCES_UPDATE_ONLINE"));
                            statement.setInt(1, currentFolder.getType());
                            statement.setInt(2, currentFolder.getFlags());
                            statement.setInt(3, currentFolder.getOwnerId());
                            statement.setInt(4, currentFolder.getGroupId());
                            statement.setInt(5, onlineFolder.getProjectId());
                            statement.setInt(6, currentFolder.getAccessFlags());
                            statement.setInt(7, C_STATE_UNCHANGED);
                            statement.setInt(8, currentFolder.isLockedBy());
                            statement.setInt(9, currentFolder.getLauncherType());
                            statement.setString(10, currentFolder.getLauncherClassname());
                            statement.setTimestamp(11, new Timestamp(System.currentTimeMillis()));
                            statement.setInt(12, currentFolder.getResourceLastModifiedBy());
                            statement.setInt(13, 0);
                            statement.setInt(14, onlineFolder.getFileId());
                            statement.setInt(15, onlineFolder.getResourceId());
                            statement.executeUpdate();
                            newFolder = readFolder(onlineProject.getId(), currentFolder.getAbsolutePath());
                        } catch (SQLException sqle) {
                            throw new CmsException("[" + this.getClass().getName() + "] " + sqle.getMessage(), CmsException.C_SQL_ERROR, sqle);
                        } finally {
                            if(statement != null) {
                                try {
                                    statement.close();
                                } catch(SQLException exc) {
                                    // nothing to do here
                                }
                            }
                            if(con != null) {
                                try {
                                    con.close();
                                } catch(SQLException exc) {
                                    // nothing to do here
                                }
                            }
                        }
                    } else {
                        throw e;
                    }
                }
                folderIdIndex.put(new Integer(currentFolder.getResourceId()), new Integer(newFolder.getResourceId()));
                // copy properties
                Hashtable props = new Hashtable();
                try {
                    props = readAllProperties(projectId, currentFolder, currentFolder.getType());
                    writeProperties(props, onlineProject.getId(), newFolder, newFolder.getType());
                } catch (CmsException exc) {
                    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
                        A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INFO, "[CmsDbAccess] error publishing, copy properties for " + newFolder.toString() + " Message= " + exc.getMessage());
                    }
                }
                if (enableHistory){
                    // backup the offline resource
                    backupResource(projectId, currentFolder, new byte[0], props, versionId, publishDate);
                }
                // set the state of current folder in the offline project to unchanged
                currentFolder.setState(C_STATE_UNCHANGED);
                updateResourcestate(currentFolder);
                // C_STATE_CHANGED
            } else if (currentFolder.getState() == C_STATE_CHANGED){
                changedResources.addElement(currentFolder.getAbsolutePath());
                // export to filesystem if necessary
                String exportKey = checkExport(currentFolder.getAbsolutePath());
                if (exportKey != null){
                    discAccess.createFolder(currentFolder.getAbsolutePath(), exportKey);
                }
                CmsFolder onlineFolder = null;
                try {
                    onlineFolder = readFolder(onlineProject.getId(), currentFolder.getAbsolutePath());
                } catch (CmsException exc){
                    // if folder does not exist create it
                    if (exc.getType() == CmsException.C_NOT_FOUND){
                        // get parentId for onlineFolder either from folderIdIndex or from the database
                        Integer parentId = (Integer) folderIdIndex.get(new Integer(currentFolder.getParentId()));
                        if (parentId == null){
                            CmsFolder currentOnlineParent = readFolder(onlineProject.getId(), currentFolder.getParent());
                            parentId = new Integer(currentOnlineParent.getResourceId());
                            folderIdIndex.put(new Integer(currentFolder.getParentId()), parentId);
                        }
                        // create the new folder
                        onlineFolder = createFolder(user, onlineProject, onlineProject, currentFolder, parentId.intValue(), currentFolder.getAbsolutePath());
                        onlineFolder.setState(C_STATE_UNCHANGED);
                        updateResourcestate(onlineFolder);
                    } else {
                        throw exc;
                    }
                } // end of catch
                Connection con = null;
                PreparedStatement statement = null;
                try {
                    con = DriverManager.getConnection(m_poolNameOnline);
                    // update the onlineFolder with data from offlineFolder
                    statement = con.prepareStatement(m_cq.get("C_RESOURCES_UPDATE_ONLINE"));
                    statement.setInt(1, currentFolder.getType());
                    statement.setInt(2, currentFolder.getFlags());
                    statement.setInt(3, currentFolder.getOwnerId());
                    statement.setInt(4, currentFolder.getGroupId());
                    statement.setInt(5, onlineFolder.getProjectId());
                    statement.setInt(6, currentFolder.getAccessFlags());
                    statement.setInt(7, C_STATE_UNCHANGED);
                    statement.setInt(8, currentFolder.isLockedBy());
                    statement.setInt(9, currentFolder.getLauncherType());
                    statement.setString(10, currentFolder.getLauncherClassname());
                    statement.setTimestamp(11, new Timestamp(System.currentTimeMillis()));
                    statement.setInt(12, currentFolder.getResourceLastModifiedBy());
                    statement.setInt(13, 0);
                    statement.setInt(14, onlineFolder.getFileId());
                    statement.setInt(15, onlineFolder.getResourceId());
                    statement.executeUpdate();
                } catch (SQLException e){
                    throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
                } finally {
                    if(statement != null) {
                        try {
                            statement.close();
                        } catch(SQLException exc) {
                            // nothing to do here
                        }
                    }
                    if(con != null) {
                        try {
                            con.close();
                        } catch(SQLException exc) {
                            // nothing to do here
                        }
                    }
                }
                folderIdIndex.put(new Integer(currentFolder.getResourceId()), new Integer(onlineFolder.getResourceId()));
                // copy properties
                Hashtable props = new Hashtable();
                try {
                    deleteAllProperties(onlineProject.getId(), onlineFolder);
                    props = readAllProperties(projectId, currentFolder, currentFolder.getType());
                    writeProperties(props, onlineProject.getId(), onlineFolder, currentFolder.getType());
                } catch (CmsException exc){
                    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()){
                        A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INFO, "[CmsDbAccess] error publishing, deleting properties for " + onlineFolder.toString() + " Message= " + exc.getMessage());
                    }
                }
                if (enableHistory){
                    // backup the offline resource
                    backupResource(projectId, currentFolder, new byte[0], props, versionId, publishDate);
                }
                // set the state of current folder in the offline project to unchanged
                currentFolder.setState(C_STATE_UNCHANGED);
                updateResourcestate(currentFolder);
            } // end of else if
        } // end of for(...

        // now read all FILES in offlineProject
        offlineFiles = readFiles(projectId, false, true);
        for (int i = 0; i < offlineFiles.size(); i++){
            currentFile = ((CmsFile) offlineFiles.elementAt(i));
            // do not publish files that are locked in another project
            if (currentFile.isLocked()){
                //in this case do nothing
            } else if (currentFile.getName().startsWith(C_TEMP_PREFIX)){
                deleteAllProperties(projectId, currentFile);
                removeFile(projectId, currentFile.getAbsolutePath());
                // C_STATE_DELETE
            } else if (currentFile.getState() == C_STATE_DELETED){
                changedResources.addElement(currentFile.getAbsolutePath());
                // delete in filesystem if necessary
                String exportKey = checkExport(currentFile.getAbsolutePath());
                if (exportKey != null){
                    try{
                        discAccess.removeResource(currentFile.getAbsolutePath(), exportKey);
                    }catch (Exception ex){
                    }
                }
                CmsFile currentOnlineFile = readFile(onlineProject.getId(), onlineProject.getId(), currentFile.getAbsolutePath());
                if (enableHistory){
                    // read the properties for backup
                    Hashtable props = readAllProperties(projectId, currentFile, currentFile.getType());
                    // backup the offline resource
                    backupResource(projectId, currentFile, currentFile.getContents(), props, versionId, publishDate);
                }
                try{
                    deleteAllProperties(onlineProject.getId(), currentOnlineFile);
                    deleteAllProperties(projectId, currentFile);
                }catch (CmsException exc){
                    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()){
                        A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INFO, "[CmsDbAccess] error publishing, deleting properties for " + currentOnlineFile.toString() + " Message= " + exc.getMessage());
                    }
                }try{
                    deleteResource(currentOnlineFile);
                    deleteResource(currentFile);
                }catch (CmsException exc){
                    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()){
                        A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INFO, "[CmsDbAccess] error publishing, deleting resource for " + currentOnlineFile.toString() + " Message= " + exc.getMessage());
                    }
                }
            // C_STATE_CHANGED
            }else if (currentFile.getState() == C_STATE_CHANGED){
                changedResources.addElement(currentFile.getAbsolutePath());
                // export to filesystem if necessary
                String exportKey = checkExport(currentFile.getAbsolutePath());
                if (exportKey != null){
                    discAccess.writeFile(currentFile.getAbsolutePath(), exportKey, currentFile.getContents());
                }
                CmsFile onlineFile = null;
                try{
                    onlineFile = readFileHeader(onlineProject.getId(), currentFile.getAbsolutePath());
                }catch (CmsException exc){
                    if (exc.getType() == CmsException.C_NOT_FOUND){
                        // get parentId for onlineFolder either from folderIdIndex or from the database
                        Integer parentId = (Integer) folderIdIndex.get(new Integer(currentFile.getParentId()));
                        if (parentId == null){
                            CmsFolder currentOnlineParent = readFolder(onlineProject.getId(), currentFolder.getParent());
                            parentId = new Integer(currentOnlineParent.getResourceId());
                            folderIdIndex.put(new Integer(currentFile.getParentId()), parentId);
                        }
                        // create a new File
                        currentFile.setState(C_STATE_UNCHANGED);
                        onlineFile = createFile(onlineProject, onlineProject, currentFile, user.getId(), parentId.intValue(), currentFile.getAbsolutePath(), false);
                    }
                } // end of catch
                Connection con = null;
                PreparedStatement statement = null;
                try{
                    con = DriverManager.getConnection(m_poolNameOnline);
                    // update the onlineFile with data from offlineFile
                    statement = con.prepareStatement(m_cq.get("C_RESOURCES_UPDATE_ONLINE"));
                    statement.setInt(1, currentFile.getType());
                    statement.setInt(2, currentFile.getFlags());
                    statement.setInt(3, currentFile.getOwnerId());
                    statement.setInt(4, currentFile.getGroupId());
                    statement.setInt(5, onlineFile.getProjectId());
                    statement.setInt(6, currentFile.getAccessFlags());
                    statement.setInt(7, C_STATE_UNCHANGED);
                    statement.setInt(8, currentFile.isLockedBy());
                    statement.setInt(9, currentFile.getLauncherType());
                    statement.setString(10, currentFile.getLauncherClassname());
                    statement.setTimestamp(11, new Timestamp(System.currentTimeMillis()));
                    statement.setInt(12, currentFile.getResourceLastModifiedBy());
                    statement.setInt(13, currentFile.getLength());
                    statement.setInt(14, onlineFile.getFileId());
                    statement.setInt(15, onlineFile.getResourceId());
                    statement.executeUpdate();
                    statement.close();
                    statement = con.prepareStatement(m_cq.get("C_FILES_UPDATE_ONLINE"));
                    statement.setBytes(1, currentFile.getContents());
                    statement.setInt(2, onlineFile.getFileId());
                    statement.executeUpdate();
                    statement.close();
                }catch (SQLException e){
                    throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
                }finally{
                    if(statement != null) {
                        try {
                            statement.close();
                        } catch(SQLException exc) {
                            // nothing to do here
                        }
                    }
                    if(con != null) {
                        try {
                            con.close();
                        } catch(SQLException exc) {
                            // nothing to do here
                        }
                    }
                }
                // copy properties
                Hashtable props = new Hashtable();
                try {
                    deleteAllProperties(onlineProject.getId(), onlineFile);
                    props = readAllProperties(projectId, currentFile, currentFile.getType());
                    writeProperties(props, onlineProject.getId(), onlineFile, currentFile.getType());
                } catch (CmsException exc) {
                    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
                        A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INFO, "[CmsDbAccess] error publishing, deleting properties for " + onlineFile.toString() + " Message= " + exc.getMessage());
                    }
                }
                if (enableHistory){
                    // backup the offline resource
                    backupResource(projectId, currentFile, currentFile.getContents(), props, versionId, publishDate);
                }
                // set the file state to unchanged
                currentFile.setState(C_STATE_UNCHANGED);
                updateResourcestate(currentFile);
            // C_STATE_NEW
            } else if (currentFile.getState() == C_STATE_NEW) {
                // export to filesystem if necessary
                String exportKey = checkExport(currentFile.getAbsolutePath());
                if (exportKey != null){
                    discAccess.writeFile(currentFile.getAbsolutePath(), exportKey, currentFile.getContents());
                }
                // get parentId for onlineFile either from folderIdIndex or from the database
                Integer parentId = (Integer) folderIdIndex.get(new Integer(currentFile.getParentId()));
                if (parentId == null){
                    CmsFolder currentOnlineParent = readFolder(onlineProject.getId(), currentFile.getParent());
                    parentId = new Integer(currentOnlineParent.getResourceId());
                    folderIdIndex.put(new Integer(currentFile.getParentId()), parentId);
                }
                // create the new file
                try {
                    newFile = createFile(onlineProject, onlineProject, currentFile, user.getId(), parentId.intValue(), currentFile.getAbsolutePath(), false);
                    newFile.setState(C_STATE_UNCHANGED);
                    updateResourcestate(newFile);
                } catch (CmsException e) {
                    if (e.getType() == CmsException.C_FILE_EXISTS) {
                        CmsFile onlineFile = null;
                        try {
                            onlineFile = readFileHeader(onlineProject.getId(), currentFile.getAbsolutePath());
                        } catch (CmsException exc) {
                            throw exc;
                        } // end of catch
                        Connection con = null;
                        PreparedStatement statement = null;
                        try {
                            con = DriverManager.getConnection(m_poolNameOnline);
                            // update the onlineFile with data from offlineFile
                            statement = con.prepareStatement(m_cq.get("C_RESOURCES_UPDATE_ONLINE"));
                            statement.setInt(1, currentFile.getType());
                            statement.setInt(2, currentFile.getFlags());
                            statement.setInt(3, currentFile.getOwnerId());
                            statement.setInt(4, currentFile.getGroupId());
                            statement.setInt(5, onlineFile.getProjectId());
                            statement.setInt(6, currentFile.getAccessFlags());
                            statement.setInt(7, C_STATE_UNCHANGED);
                            statement.setInt(8, currentFile.isLockedBy());
                            statement.setInt(9, currentFile.getLauncherType());
                            statement.setString(10, currentFile.getLauncherClassname());
                            statement.setTimestamp(11, new Timestamp(System.currentTimeMillis()));
                            statement.setInt(12, currentFile.getResourceLastModifiedBy());
                            statement.setInt(13, currentFile.getLength());
                            statement.setInt(14, onlineFile.getFileId());
                            statement.setInt(15, onlineFile.getResourceId());
                            statement.executeUpdate();
                            statement = con.prepareStatement(m_cq.get("C_FILES_UPDATE_ONLINE"));
                            statement.setBytes(1, currentFile.getContents());
                            statement.setInt(2, onlineFile.getFileId());
                            statement.executeUpdate();
                            statement.close();
                            newFile = readFile(onlineProject.getId(), onlineProject.getId(), currentFile.getAbsolutePath());
                        } catch (SQLException sqle) {
                            throw new CmsException("[" + this.getClass().getName() + "] " + sqle.getMessage(), CmsException.C_SQL_ERROR, sqle);
                        } finally {
                            if(statement != null) {
                                try {
                                    statement.close();
                                } catch(SQLException exc) {
                                    // nothing to do here
                                }
                            }
                            if(con != null) {
                                try {
                                    con.close();
                                } catch(SQLException exc) {
                                    // nothing to do here
                                }
                            }
                        }
                    } else {
                        throw e;
                    }
                }
                // copy properties
                Hashtable props = new Hashtable();
                try{
                    props = readAllProperties(projectId, currentFile, currentFile.getType());
                    writeProperties(props, onlineProject.getId(), newFile, newFile.getType());
                }catch (CmsException exc){
                    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()){
                        A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INFO, "[CmsDbAccess] error publishing, copy properties for " + newFile.toString() + " Message= " + exc.getMessage());
                    }
                }
                if (enableHistory){
                    // backup the offline resource
                    backupResource(projectId, currentFile, currentFile.getContents(), props, versionId, publishDate);
                }
                // set the file state to unchanged
                currentFile.setState(C_STATE_UNCHANGED);
                updateResourcestate(currentFile);
            }
        } // end of for(...
        // now delete the "deleted" folders
        for (int i = deletedFolders.size() - 1; i > -1; i--){
            currentFolder = ((CmsFolder) deletedFolders.elementAt(i));
            String exportKey = checkExport(currentFolder.getAbsolutePath());
            if (exportKey != null){
                discAccess.removeResource(currentFolder.getAbsolutePath(), exportKey);
            }
            if (enableHistory){
                Hashtable props = readAllProperties(projectId, currentFolder,currentFolder.getType());
                // backup the offline resource
                backupResource(projectId, currentFolder, new byte[0], props, versionId, publishDate);
            }
            CmsResource delOnlineFolder = readFolder(onlineProject.getId(),currentFolder.getAbsolutePath());
            try{
                deleteAllProperties(onlineProject.getId(), delOnlineFolder);
                deleteAllProperties(projectId, currentFolder);
            }catch (CmsException exc){
                if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ){
                    A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INFO, "[CmsDbAccess] error publishing, deleting properties for " + currentFolder.toString() + " Message= " + exc.getMessage());
                }
            }
            removeFolderForPublish(onlineProject.getId(), currentFolder.getAbsolutePath());
            removeFolderForPublish(projectId, currentFolder.getAbsolutePath());
        } // end of for
        //clearFilesTable();
        return changedResources;
    }

    /**
     * Get the next version id for the published backup resources
     *
     * @return int The new version id
     */
    public int getBackupVersionId(){
        PreparedStatement statement = null;
        Connection con = null;
        ResultSet res = null;
        int versionId = 1;
        int resVersionId = 1;
        try{
            // get the max version id
            con = DriverManager.getConnection(m_poolNameBackup);
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_BACKUP_MAXVER"));
            res = statement.executeQuery();
            if (res.next()){
                versionId = res.getInt(1)+1;
            }
            res.close();
            statement.close();
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_BACKUP_MAXVER_RESOURCE"));
            res = statement.executeQuery();
            if (res.next()){
                resVersionId = res.getInt(1)+1;
            }
            if (resVersionId > versionId){
                versionId = resVersionId;
            }
            return versionId;
        } catch (SQLException exc){
            return 1;
        } finally {
            if (res != null){
                try{
                    res.close();
                } catch (SQLException ex){
                }
            }
            if (statement != null){
                try{
                    statement.close();
                } catch (SQLException ex){
                }
            }
            if (con != null){
                try{
                    con.close();
                } catch (SQLException ex){
                }
            }
        }
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

    public void backupProject(CmsProject project, int versionId,
                              long publishDate, CmsUser currentUser) throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;
        CmsUser owner = readUser(project.getOwnerId());
        CmsGroup group = readGroup(project.getGroupId());
        CmsGroup managerGroup = readGroup(project.getManagerGroupId());
        Vector projectresources = readAllProjectResources(project.getId());
        // write backup project to the database
        try {
            con = DriverManager.getConnection(m_poolNameBackup);
            // first write the project
            statement = con.prepareStatement(m_cq.get("C_PROJECTS_CREATE_BACKUP"));
            statement.setInt(1, versionId);
            statement.setInt(2, project.getId());
            statement.setString(3, project.getName());
            statement.setTimestamp(4, new Timestamp(publishDate));
            statement.setInt(5, currentUser.getId());
            statement.setString(6, currentUser.getName()+" "+currentUser.getFirstname()+" "+currentUser.getLastname());
            statement.setInt(7, project.getOwnerId());
            statement.setString(8, owner.getName()+" "+owner.getFirstname()+" "+owner.getLastname());
            statement.setInt(9, project.getGroupId());
            statement.setString(10, group.getName());
            statement.setInt(11, project.getManagerGroupId());
            statement.setString(12, managerGroup.getName());
            statement.setString(13, project.getDescription());
            statement.setTimestamp(14, new Timestamp(project.getCreateDate()));
            statement.setInt(15, project.getType());
            statement.setInt(16, project.getTaskId());
            statement.executeUpdate();
            statement.close();
            // now write the projectresources
            for(int i = 0; i < projectresources.size(); i++){
                statement = con.prepareStatement(m_cq.get("C_PROJECTRESOURCES_CREATE_BACKUP"));
                statement.setInt(1, versionId);
                statement.setInt(2, project.getId());
                statement.setString(3, (String)projectresources.get(i));
                statement.executeUpdate();
                statement.close();
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                     // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
    }

    /**
     * Creates a backup of the published resource
     *
     * @param projectId The project in which the resource was published.
     * @param resource The published resource
     * @param content The file content if the resource is a file.
     * @param properties The properties of the resource.
     * @param versionId The version of the backup
     * @param publishDate The date of publishing
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */

    public void backupResource(int projectId, CmsResource resource, byte[] content,
                               Hashtable properties, int versionId, long publishDate) throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet res = null;
        CmsUser ownerName = readUser(resource.getOwnerId());
        String groupName = readGroup(resource.getGroupId()).getName();
        CmsUser lastModified = readUser(resource.getResourceLastModifiedBy());
        int resourceId = nextId(m_cq.get("C_TABLE_RESOURCES_BACKUP"));
        int fileId = C_UNKNOWN_ID;
        // write backup resource to the database
        try {
            con = DriverManager.getConnection(m_poolNameBackup);
            // if the resource is not a folder then backup the filecontent
            if (resource.getType() != C_TYPE_FOLDER){
                fileId = nextId(m_cq.get("C_TABLE_FILES_BACKUP"));
                // write new resource to the database
                statement = con.prepareStatement(m_cq.get("C_FILES_WRITE_BACKUP"));
                statement.setInt(1, fileId);
                statement.setBytes(2, content);
                statement.executeUpdate();
                statement.close();
            }
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_WRITE_BACKUP"));
            statement.setInt(1, resourceId);
            statement.setInt(2, C_UNKNOWN_ID);
            statement.setString(3, resource.getAbsolutePath());
            statement.setInt(4, resource.getType());
            statement.setInt(5, resource.getFlags());
            statement.setInt(6, resource.getOwnerId());
            statement.setString(7, ownerName.getName()+" "+ownerName.getFirstname()+" "+ownerName.getLastname());
            statement.setInt(8, resource.getGroupId());
            statement.setString(9, groupName);
            statement.setInt(10, projectId);
            statement.setInt(11, fileId);
            statement.setInt(12, resource.getAccessFlags());
            statement.setInt(13, resource.getState());
            statement.setInt(14, resource.getLauncherType());
            statement.setString(15, resource.getLauncherClassname());
            // set date created = publish date
            statement.setTimestamp(16, new Timestamp(publishDate));
            statement.setTimestamp(17, new Timestamp(resource.getDateLastModified()));
            statement.setInt(18, content.length);
            statement.setInt(19, resource.getResourceLastModifiedBy());
            statement.setString(20, lastModified.getName()+" "+lastModified.getFirstname()+" "+lastModified.getLastname());
            statement.setInt(21, versionId);
            statement.executeUpdate();
            statement.close();
            // now write the properties
            // get all metadefs
            Enumeration keys = properties.keys();
            // one metainfo-name:
            String key;
            while(keys.hasMoreElements()) {
                key = (String) keys.nextElement();
                CmsPropertydefinition propdef = readPropertydefinition(key, resource.getType());
                String value = (String) properties.get(key);
                if( propdef == null) {
                    // there is no propertydefinition for with the overgiven name for the resource
                    throw new CmsException("[" + this.getClass().getName() + "] " + key,
                    CmsException.C_NOT_FOUND);
                } else {
                    // write the property into the db
                    statement = con.prepareStatement(m_cq.get("C_PROPERTIES_CREATE_BACKUP"));
                    statement.setInt(1, nextId(m_cq.get("C_TABLE_PROPERTIES_BACKUP")));
                    statement.setInt(2, propdef.getId());
                    statement.setInt(3, resourceId);
                    statement.setString(4, checkNull(value));
                    statement.executeUpdate();
                    statement.close();
                }
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                     // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
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
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet res = null;
        Vector projectResources = new Vector();
        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_PROJECTRESOURCES_READALL"));
            // select all resources from the database
            statement.setInt(1, projectId);
            res = statement.executeQuery();
            while (res.next()) {
                projectResources.addElement(res.getString("RESOURCE_NAME"));
            }
            res.close();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            if (statement != null) {
                try{
                    statement.close();
                } catch (SQLException e){
                }
            }
            if (con != null) {
                try{
                    con.close();
                } catch (SQLException e){
                }
            }
        }
        return projectResources;
    }

    /**
     * Reads all file headers of a file in the OpenCms.<BR>
     * The reading excludes the filecontent.
     *
     * @param filename The name of the file to be read.
     *
     * @return Vector of file headers read from the Cms.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public Vector readAllFileHeaders(int projectId, String resourceName)
        throws CmsException {

        CmsFile file=null;
        ResultSet res =null;
        Vector allHeaders = new Vector();
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_READ_ALL"+usedStatement));
            // read file header data from database
            statement.setString(1, resourceName);
            res = statement.executeQuery();
            // create new file headers
            while(res.next()) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                String resName=res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int projectID=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy= res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                int launcherType= res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                String launcherClass=  res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int resSize= res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int lockedInProject=res.getInt("LOCKED_IN_PROJECT");
                file=new CmsFile(resId,parentId,fileId,resName,resType,resFlags,userId,
                                groupId,projectID,accessFlags,state,lockedBy,
                                launcherType,launcherClass,created,modified,modifiedBy,
                                new byte[0],resSize, lockedInProject);
                allHeaders.addElement(file);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch( Exception exc ) {
            throw new CmsException("readAllFileHeaders "+exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return allHeaders;
    }

    /**
     * Reads all file headers of a file in the OpenCms.<BR>
     * The reading excludes the filecontent.
     *
     * @param filename The name of the file to be read.
     *
     * @return Vector of file headers read from the Cms.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public Vector readAllFileHeadersForHist(int projectId, String resourceName)
        throws CmsException {

        CmsBackupResource file=null;
        ResultSet res =null;
        Vector allHeaders = new Vector();
        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolNameBackup);
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_READ_ALL_BACKUP"));
            // read file header data from database
            statement.setString(1, resourceName);
            res = statement.executeQuery();
            // create new file headers
            while(res.next()) {
                int versionId=res.getInt(m_cq.get("C_RESOURCES_VERSION_ID"));
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                String resName=res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                String userName=res.getString(m_cq.get("C_RESOURCES_USER_NAME"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                String groupName= res.getString(m_cq.get("C_RESOURCES_GROUP_NAME"));
                int projectID=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int launcherType= res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                String launcherClass=  res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int resSize= res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                String modifiedByName=res.getString(m_cq.get("C_RESOURCES_LASTMODIFIED_BY_NAME"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                file=new CmsBackupResource(versionId,resId,parentId,fileId,resName,resType,resFlags,
                                           userId,userName,groupId,groupName,projectID,accessFlags,
                                           state,launcherType,launcherClass,created,modified,
                                           modifiedBy,modifiedByName,new byte[0],resSize,
                                           lockedInProject);

                allHeaders.addElement(file);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch( Exception exc ) {
            throw new CmsException("readAllFileHeaders "+exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return allHeaders;
    }

    /**
     * Returns a list of all properties of a file or folder.
     *
     * @param resourceId The id of the resource.
     * @param resourceType The Type of the resource.
     *
     * @return Vector of properties as Strings.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public Hashtable readAllProperties(int projectId, CmsResource resource, int resourceType)
        throws CmsException {

        Hashtable returnValue = new Hashtable();
        ResultSet result = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        int resourceId = resource.getResourceId();
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
           con = DriverManager.getConnection(usedPool);
            // create project
            statement = con.prepareStatement(m_cq.get("C_PROPERTIES_READALL"+usedStatement));
            statement.setInt(1, resourceId);
            statement.setInt(2, resourceType);
            result = statement.executeQuery();
            while(result.next()) {
                 returnValue.put(result.getString(m_cq.get("C_PROPERTYDEF_NAME")),
                                 result.getString(m_cq.get("C_PROPERTY_VALUE")));
            }
        } catch( SQLException exc ) {
            throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                CmsException.C_SQL_ERROR, exc);
        } finally {
            // close all db-resources
            if(result != null) {
                try {
                    result.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return(returnValue);
    }

    /**
     * Reads all propertydefinitions for the given resource type.
     *
     * @param resourcetype The resource type to read the propertydefinitions for.
     *
     * @return propertydefinitions A Vector with propertydefefinitions for the resource type.
     * The Vector is maybe empty.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public Vector readAllPropertydefinitions(int resourcetype)
        throws CmsException {
         Vector metadefs = new Vector();
         ResultSet result = null;
         PreparedStatement statement = null;
         Connection con = null;

         try {
             con = DriverManager.getConnection(m_poolName);
             // create statement
             statement = con.prepareStatement(m_cq.get("C_PROPERTYDEF_READALL"));
             statement.setInt(1,resourcetype);
             result = statement.executeQuery();

             while(result.next()) {
                 metadefs.addElement( new CmsPropertydefinition( result.getInt(m_cq.get("C_PROPERTYDEF_ID")),
                                                             result.getString(m_cq.get("C_PROPERTYDEF_NAME")),
                                                             result.getInt(m_cq.get("C_PROPERTYDEF_RESOURCE_TYPE"))));
             }
         } catch( SQLException exc ) {
             throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                 CmsException.C_SQL_ERROR, exc);
         }finally {
            // close all db-resources
            if(result != null) {
                 try {
                     result.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
          }
        return(metadefs);
    }


    /**
     * Reads all propertydefinitions for the given resource type.
     *
     * @param resourcetype The resource type to read the propertydefinitions for.
     *
     * @return propertydefinitions A Vector with propertydefefinitions for the resource type.
     * The Vector is maybe empty.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public Vector readAllPropertydefinitions(I_CmsResourceType resourcetype)
        throws CmsException {
        return(readAllPropertydefinitions(resourcetype.getResourceType()));
    }

    /**
     * Reads a file from the Cms.<BR/>
     *
     * @param projectId The Id of the project in which the resource will be used.
     * @param onlineProjectId The online projectId of the OpenCms.
     * @param filename The complete name of the new file (including pathinformation).
     *
     * @return file The read file.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
     public CmsFile readFile(int projectId,
                             int onlineProjectId,
                             String filename)
         throws CmsException {
        CmsFile file = null;
        PreparedStatement statement = null;
        ResultSet res = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        if (projectId == onlineProjectId) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            // if the actual project is the online project read file header and content
            // from the online project
            statement = con.prepareStatement(m_cq.get("C_FILES_READ"+usedStatement));
            statement.setString(1, filename);
            statement.setInt(2, projectId);
            res = statement.executeQuery();
            if(res.next()) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy= res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                int launcherType= res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                String launcherClass=  res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int resSize= res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                byte[] content=res.getBytes(m_cq.get("C_RESOURCES_FILE_CONTENT"));
                int resProjectId=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                file = new CmsFile(resId,parentId,fileId,filename,resType,resFlags,userId,
                                   groupId,resProjectId,accessFlags,state,lockedBy,
                                   launcherType,launcherClass,created,modified,modifiedBy,
                                   content,resSize, lockedInProject);
            } else {
                throw new CmsException("["+this.getClass().getName()+"] "+filename,CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch( Exception exc ) {
            throw new CmsException("readFile "+exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return file;
     }

    /**
     * Reads a file in the project from the Cms.<BR/>
     *
     * @param projectId The Id of the project in which the resource will be used.
     * @param onlineProjectId The online projectId of the OpenCms.
     * @param filename The complete name of the new file (including pathinformation).
     *
     * @return file The read file.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
     public CmsFile readFileInProject(int projectId,
                                        int onlineProjectId,
                                        String filename)
         throws CmsException {

        CmsFile file = null;
        PreparedStatement statement = null;
        ResultSet res = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        if (projectId == onlineProjectId) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            // if the actual project is the online project read file header and content
            // from the online project
            statement = con.prepareStatement(m_cq.get("C_FILES_READINPROJECT"+usedStatement));
            statement.setString(1, filename);
            statement.setInt(2, projectId);
            res = statement.executeQuery();
            if(res.next()) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy= res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                int launcherType= res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                String launcherClass=  res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int resSize= res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                byte[] content=res.getBytes(m_cq.get("C_RESOURCES_FILE_CONTENT"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                file = new CmsFile(resId,parentId,fileId,filename,resType,resFlags,userId,
                                   groupId,projectId,accessFlags,state,lockedBy,
                                   launcherType,launcherClass,created,modified,modifiedBy,
                                   content,resSize, lockedInProject);
            } else {
                throw new CmsException("["+this.getClass().getName()+"] "+filename,CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch( Exception exc ) {
            throw new CmsException("readFile "+exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return file;
     }

    /**
     * Private helper method to read the fileContent for publishProject(export).
     *
     * @param fileId the fileId.
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    protected byte[] readFileContent(int projectId, int fileId)
        throws CmsException {
        PreparedStatement statement = null;
        ResultSet res = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        byte[] returnValue = null;
        try {
            con = DriverManager.getConnection(usedPool);
            // read fileContent from database
            statement = con.prepareStatement(m_cq.get("C_FILE_READ"+usedStatement));
            statement.setInt(1,fileId);
            res = statement.executeQuery();
            if (res.next()) {
                  returnValue = res.getBytes(m_cq.get("C_FILE_CONTENT"));
            } else {
                  throw new CmsException("["+this.getClass().getName()+"]"+fileId,CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        }finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return returnValue;
    }

    /**
     * Reads a file header from the Cms.<BR/>
     * The reading excludes the filecontent.
     *
     * @param projectId The Id of the project
     * @param resourceId The Id of the resource.
     *
     * @return file The read file.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile readFileHeader(int projectId, int resourceId)
        throws CmsException {

        CmsFile file=null;
        ResultSet res =null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement=con.prepareStatement(m_cq.get("C_RESOURCES_READBYID"+usedStatement));
            // read file data from database
            statement.setInt(1, resourceId);
            res = statement.executeQuery();
            // create new file
            if(res.next()) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                String resName=res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int projectID=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy= res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                int launcherType= res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                String launcherClass=  res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int resSize= res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                file=new CmsFile(resId,parentId,fileId,resName,resType,resFlags,userId,
                                groupId,projectID,accessFlags,state,lockedBy,
                                launcherType,launcherClass,created,modified,modifiedBy,
                                new byte[0],resSize, lockedInProject);
                         // check if this resource is marked as deleted
                        if (file.getState() == C_STATE_DELETED) {
                            throw new CmsException("["+this.getClass().getName()+"] "+file.getAbsolutePath(),CmsException.C_RESOURCE_DELETED);
                        }
            } else {
                throw new CmsException("["+this.getClass().getName()+"] "+resourceId,CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch( Exception exc ) {
            throw new CmsException("readFile "+exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return file;
    }

    /**
     * Reads a file header from the Cms.<BR/>
     * The reading excludes the filecontent.
     *
     * @param projectId The Id of the project
     * @param resourceId The Id of the resource.
     *
     * @return file The read file.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile readFileHeader(int projectId, CmsResource resource)
        throws CmsException {

        CmsFile file=null;
        ResultSet res =null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement=con.prepareStatement(m_cq.get("C_RESOURCES_READBYID"+usedStatement));
            // read file data from database
            statement.setInt(1, resource.getResourceId());
            res = statement.executeQuery();
            // create new file
            if(res.next()) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                String resName=res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int projectID=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy= res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                int launcherType= res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                String launcherClass=  res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int resSize= res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                file=new CmsFile(resId,parentId,fileId,resName,resType,resFlags,userId,
                                groupId,projectID,accessFlags,state,lockedBy,
                                launcherType,launcherClass,created,modified,modifiedBy,
                                new byte[0],resSize,lockedInProject);
                         // check if this resource is marked as deleted
                        if (file.getState() == C_STATE_DELETED) {
                            throw new CmsException("["+this.getClass().getName()+"] "+file.getAbsolutePath(),CmsException.C_RESOURCE_DELETED);
                        }
            } else {
                throw new CmsException("["+this.getClass().getName()+"] "+resource.getResourceId(),CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch( Exception exc ) {
            throw new CmsException("readFile "+exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return file;
    }

    /**
     * Reads a file header from the Cms.<BR/>
     * The reading excludes the filecontent.
     *
     * @param projectId The Id of the project in which the resource will be used.
     * @param filename The complete name of the new file (including pathinformation).
     *
     * @return file The read file.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile readFileHeader(int projectId, String filename)
        throws CmsException {

        CmsFile file=null;
        ResultSet res =null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement=con.prepareStatement(m_cq.get("C_RESOURCES_READ"+usedStatement));
            // read file data from database
            statement.setString(1, filename);
            statement.setInt(2, projectId);
            res = statement.executeQuery();
            // create new file
            if(res.next()) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                String resName=res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int projectID=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy= res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                int launcherType= res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                String launcherClass=  res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int resSize= res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                file=new CmsFile(resId,parentId,fileId,resName,resType,resFlags,userId,
                                groupId,projectID,accessFlags,state,lockedBy,
                                launcherType,launcherClass,created,modified,modifiedBy,
                                new byte[0],resSize,lockedInProject);

                // check if this resource is marked as deleted
                if (file.getState() == C_STATE_DELETED) {
                    throw new CmsException("["+this.getClass().getName()+"] "+file.getAbsolutePath(),CmsException.C_RESOURCE_DELETED);
                }
            } else {
                throw new CmsException("["+this.getClass().getName()+"] "+filename,CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch( Exception exc ) {
            throw new CmsException("readFile "+exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
          }

        return file;
       }

    /**
     * Reads a file header from the Cms.<BR/>
     * The reading excludes the filecontent.
     *
     * @param projectId The Id of the project in which the resource will be used.
     * @param filename The complete name of the new file (including pathinformation).
     *
     * @return file The read file.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile readFileHeader(int projectId, String filename, boolean includeDeleted)
        throws CmsException {

        CmsFile file=null;
        ResultSet res =null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement=con.prepareStatement(m_cq.get("C_RESOURCES_READ"+usedStatement));
            // read file data from database
            statement.setString(1, filename);
            statement.setInt(2, projectId);
            res = statement.executeQuery();
            // create new file
            if(res.next()) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                String resName=res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int projectID=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy= res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                int launcherType= res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                String launcherClass=  res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int resSize= res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                file=new CmsFile(resId,parentId,fileId,resName,resType,resFlags,userId,
                                groupId,projectID,accessFlags,state,lockedBy,
                                launcherType,launcherClass,created,modified,modifiedBy,
                                new byte[0],resSize,lockedInProject);
                // check if this resource is marked as deleted
                if ((file.getState() == C_STATE_DELETED) && !includeDeleted) {
                    throw new CmsException("["+this.getClass().getName()+"] "+file.getAbsolutePath(),CmsException.C_RESOURCE_DELETED);
                }
            } else {
                throw new CmsException("["+this.getClass().getName()+"] "+filename,CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch( Exception exc ) {
            throw new CmsException("readFile "+exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
          }

        return file;
       }

    /**
     * Reads a file header from the backup of the Cms.<BR/>
     * The reading excludes the filecontent.
     *
     * @param versionId The Id of the version of the resource.
     * @param filename The complete name of the new file (including pathinformation).
     *
     * @return file The read file.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public CmsBackupResource readFileHeaderForHist(int versionId, String filename)
        throws CmsException {

        CmsBackupResource file=null;
        ResultSet res =null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        try {
            con = DriverManager.getConnection(m_poolNameBackup);
            statement=con.prepareStatement(m_cq.get("C_RESOURCES_READ_BACKUP"));
            // read file data from database
            statement.setString(1, filename);
            statement.setInt(2, versionId);
            res = statement.executeQuery();
            // create new file
            if(res.next()) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                String userName = res.getString(m_cq.get("C_RESOURCES_USER_NAME"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                String groupName = res.getString(m_cq.get("C_RESOURCES_GROUP_NAME"));
                int projectID=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int launcherType= res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                String launcherClass= res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int resSize= res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                String modifiedByName=res.getString(m_cq.get("C_RESOURCES_LASTMODIFIED_BY_NAME"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                file=new CmsBackupResource(versionId,resId,parentId,fileId,filename,resType,
                                           resFlags,userId,userName,groupId,groupName,
                                           projectID,accessFlags,state,
                                           launcherType,launcherClass,created,modified,modifiedBy,
                                           modifiedByName,new byte[0],resSize, lockedInProject);
            } else {
                throw new CmsException("["+this.getClass().getName()+"] "+filename,CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch( Exception exc ) {
            throw new CmsException("readFile "+exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
          }

        return file;
       }

    /**
     * Reads a file from the history of the Cms.<BR/>
     *
     * @param versionId The versionId of the resource.
     * @param filename The complete name of the file (including pathinformation).
     *
     * @return file The read file.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
     public CmsBackupResource readFileForHist(int versionId, String filename)
         throws CmsException {
        CmsBackupResource file = null;
        PreparedStatement statement = null;
        ResultSet res = null;
        Connection con = null;
        try {
            con = DriverManager.getConnection(m_poolNameBackup);
            // if the actual project is the online project read file header and content
            // from the online project
            statement = con.prepareStatement(m_cq.get("C_FILES_READ_BACKUP"));
            statement.setString(1, filename);
            statement.setInt(2, versionId);
            res = statement.executeQuery();
            if(res.next()) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                String userName = res.getString(m_cq.get("C_RESOURCES_USER_NAME"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                String groupName = res.getString(m_cq.get("C_RESOURCES_GROUP_NAME"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int launcherType= res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                String launcherClass=  res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                String modifiedByName = res.getString(m_cq.get("C_RESOURCES_LASTMODIFIED_BY_NAME"));
                int resSize= res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                byte[] content=res.getBytes(m_cq.get("C_RESOURCES_FILE_CONTENT"));
                int resProjectId=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                file = new CmsBackupResource(versionId,resId,parentId,fileId,filename,resType,
                                             resFlags,userId,userName,groupId,groupName,
                                             resProjectId,accessFlags,state,
                                             launcherType,launcherClass,created,modified,modifiedBy,
                                             modifiedByName,content,resSize, lockedInProject);
            } else {
                throw new CmsException("["+this.getClass().getName()+"] "+filename,CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch( Exception exc ) {
            throw new CmsException("readFile "+exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return file;
     }

    /**
     * Reads a file header from the Cms.<BR/>
     * The reading excludes the filecontent.
     *
     * @param projectId The Id of the project in which the resource will be used.
     * @param filename The complete name of the new file (including pathinformation).
     *
     * @return file The read file.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile readFileHeaderInProject(int projectId, String filename)
        throws CmsException {

        CmsFile file=null;
        ResultSet res =null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement=con.prepareStatement(m_cq.get("C_RESOURCES_READINPROJECT"+usedStatement));
            // read file data from database
            statement.setString(1, filename);
            statement.setInt(2, projectId);
            res = statement.executeQuery();
            // create new file
            if(res.next()) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                String resName=res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int projectID=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy= res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                int launcherType= res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                String launcherClass=  res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int resSize= res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                file=new CmsFile(resId,parentId,fileId,resName,resType,resFlags,userId,
                                groupId,projectID,accessFlags,state,lockedBy,
                                launcherType,launcherClass,created,modified,modifiedBy,
                                new byte[0],resSize,lockedInProject);

                // check if this resource is marked as deleted
                if (file.getState() == C_STATE_DELETED) {
                    throw new CmsException("["+this.getClass().getName()+"] "+file.getAbsolutePath(),CmsException.C_RESOURCE_DELETED);
                }
            } else {
                throw new CmsException("["+this.getClass().getName()+"] "+filename,CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch( Exception exc ) {
            throw new CmsException("readFile "+exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
          }

        return file;
       }

    /**
     * Reads all files from the Cms, that are in one project.<BR/>
     *
     * @param project The project in which the files are.
     *
     * @return A Vecor of files.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public Vector readFiles(int projectId) throws CmsException {
        return readFiles(projectId, true, false);
    }
    /**
     * Reads all files from the Cms, that are in one project.<BR/>
     *
     * @param project The project in which the files are.
     *
     * @return A Vecor of files.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public Vector readFiles(int projectId, boolean includeUnchanged, boolean onlyProject)
        throws CmsException {

        Vector files = new Vector();
        CmsFile file;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        String onlyChanged = new String();
        String inProject = new String();
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
            if (onlyProject){
                inProject = " AND CMS_RESOURCES.PROJECT_ID = CMS_PROJECTRESOURCES.PROJECT_ID";
            }
        }
        if (!includeUnchanged){
            onlyChanged = " AND STATE != "+C_STATE_UNCHANGED;
        }
        try {
            con = DriverManager.getConnection(usedPool);
            // read file data from database
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_READFILESBYPROJECT"+usedStatement)+onlyChanged+inProject);
            statement.setInt(1,projectId);
            res = statement.executeQuery();
            // create new file
            while(res.next()) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                String resName=res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int projectID=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy= res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                int launcherType= res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                String launcherClass=  res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int resSize= res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                byte[] fileContent = res.getBytes(m_cq.get("C_FILE_CONTENT"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                //byte[] fileContent = new byte[0];
                file = new CmsFile(resId,parentId,fileId,resName,resType,resFlags,userId,
                                groupId,projectID,accessFlags,state,lockedBy,
                                launcherType,launcherClass,created,modified,modifiedBy,
                                fileContent,resSize,lockedInProject);

                files.addElement(file);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (Exception ex) {
            throw new CmsException("["+this.getClass().getName()+"]", ex);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return files;
    }

    /**
     * Reads a folder from the Cms.<BR/>
     *
     * @param project The project in which the resource will be used.
     * @param foldername The name of the folder to be read.
     *
     * @return The read folder.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public CmsFolder readFolder(int projectId, String foldername)
        throws CmsException {

        CmsFolder folder=null;
        ResultSet res =null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement=con.prepareStatement(m_cq.get("C_RESOURCES_READ"+usedStatement));
            statement.setString(1, foldername);
            statement.setInt(2,projectId);
            res = statement.executeQuery();
            // create new resource
            if(res.next()) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                String resName=res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int projectID=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy= res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                folder = new CmsFolder(resId,parentId,fileId,resName,resType,resFlags,userId,
                                      groupId,projectID,accessFlags,state,lockedBy,created,
                                      modified,modifiedBy,lockedInProject);
            } else {
                throw new CmsException("["+this.getClass().getName()+"] "+foldername,CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch(CmsException exc) {
            // just throw this exception
            throw exc;
        } catch( Exception exc ) {
            throw new CmsException("readFolder "+exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return folder;
    }

    /**
     * Reads a folder from the Cms that exists in the project.<BR/>
     *
     * @param project The project in which the resource will be used.
     * @param foldername The name of the folder to be read.
     *
     * @return The read folder.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public CmsFolder readFolderInProject(int projectId, String foldername)
        throws CmsException {

        CmsFolder folder=null;
        ResultSet res =null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement=con.prepareStatement(m_cq.get("C_RESOURCES_READINPROJECT"+usedStatement));
            statement.setString(1, foldername);
            statement.setInt(2,projectId);
            res = statement.executeQuery();
            // create new resource
            if(res.next()) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                String resName=res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int projectID=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy= res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                folder = new CmsFolder(resId,parentId,fileId,resName,resType,resFlags,userId,
                                      groupId,projectID,accessFlags,state,lockedBy,created,
                                      modified,modifiedBy,lockedInProject);
            } else {
                throw new CmsException("["+this.getClass().getName()+"] "+foldername,CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch(CmsException exc) {
            // just throw this exception
            throw exc;
        } catch( Exception exc ) {
            throw new CmsException("readFolder "+exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return folder;
    }

    /**
     * Reads all folders from the Cms, that are in one project.<BR/>
     *
     * @param project The project in which the folders are.
     *
     * @return A Vecor of folders.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public Vector readFolders(int projectId) throws CmsException {
        return readFolders(projectId, true, false);
    }

    /**
     * Reads all folders from the Cms, that are in one project.<BR/>
     *
     * @param project The project in which the folders are.
     *
     * @return A Vecor of folders.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public Vector readFolders(int projectId, boolean includeUnchanged, boolean onlyProject)
        throws CmsException {

        Vector folders = new Vector();
        CmsFolder folder;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        String onlyChanged = new String();
        String inProject = new String();
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
            if (onlyProject){
                inProject = " AND CMS_RESOURCES.PROJECT_ID = CMS_PROJECTRESOURCES.PROJECT_ID";
            }
        }
        if (!includeUnchanged){
            onlyChanged = " AND CMS_RESOURCES.STATE != "+C_STATE_UNCHANGED+" ORDER BY CMS_RESOURCES.RESOURCE_NAME";
        } else {
            onlyChanged = " ORDER BY CMS_RESOURCES.RESOURCE_NAME";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            // read folder data from database
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_READFOLDERSBYPROJECT"+usedStatement)+inProject+onlyChanged);
            statement.setInt(1,projectId);
            res = statement.executeQuery();
            // create new folder
            while(res.next()) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                String resName=res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int projectID=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy= res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                folder = new CmsFolder(resId,parentId,fileId,resName,resType,resFlags,userId,
                                      groupId,projectID,accessFlags,state,lockedBy,created,
                                      modified,modifiedBy,lockedInProject);
                folders.addElement(folder);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (Exception ex) {
            throw new CmsException("["+this.getClass().getName()+"]", ex);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return folders;
    }

     /**
     * Returns a group object.<P/>
     * @param groupname The id of the group that is to be read.
     * @return Group.
     * @exception CmsException  Throws CmsException if operation was not succesful
     */
     public CmsGroup readGroup(int id)
         throws CmsException {

         CmsGroup group=null;
         ResultSet res = null;
         PreparedStatement statement=null;
         Connection con = null;


         try{
             con = DriverManager.getConnection(m_poolName);

             // read the group from the database
             statement=con.prepareStatement(m_cq.get("C_GROUPS_READGROUP2"));
             statement.setInt(1,id);
             res = statement.executeQuery();
             // create new Cms group object
             if(res.next()) {
               group=new CmsGroup(res.getInt(m_cq.get("C_GROUPS_GROUP_ID")),
                                  res.getInt(m_cq.get("C_GROUPS_PARENT_GROUP_ID")),
                                  res.getString(m_cq.get("C_GROUPS_GROUP_NAME")),
                                  res.getString(m_cq.get("C_GROUPS_GROUP_DESCRIPTION")),
                                  res.getInt(m_cq.get("C_GROUPS_GROUP_FLAGS")));
             } else {
                 throw new CmsException("[" + this.getClass().getName() + "] "+id,CmsException.C_NO_GROUP);
             }

         } catch (SQLException e){

         throw new CmsException("[" + this.getClass().getName() + "] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }

         }
         return group;
     }

    /**
     * Returns a group object.<P/>
     * @param groupname The name of the group that is to be read.
     * @return Group.
     * @exception CmsException  Throws CmsException if operation was not succesful
     */
     public CmsGroup readGroup(String groupname)
         throws CmsException {

         CmsGroup group=null;
         ResultSet res = null;
         PreparedStatement statement=null;
         Connection con = null;

         try{
             con = DriverManager.getConnection(m_poolName);
             // read the group from the database
             statement=con.prepareStatement(m_cq.get("C_GROUPS_READGROUP"));
             statement.setString(1,groupname);
             res = statement.executeQuery();

             // create new Cms group object
             if(res.next()) {
               group=new CmsGroup(res.getInt(m_cq.get("C_GROUPS_GROUP_ID")),
                                  res.getInt(m_cq.get("C_GROUPS_PARENT_GROUP_ID")),
                                  res.getString(m_cq.get("C_GROUPS_GROUP_NAME")),
                                  res.getString(m_cq.get("C_GROUPS_GROUP_DESCRIPTION")),
                                  res.getInt(m_cq.get("C_GROUPS_GROUP_FLAGS")));
             } else {
                 throw new CmsException("[" + this.getClass().getName() + "] "+groupname,CmsException.C_NO_GROUP);
             }


         } catch (SQLException e){
             throw new CmsException("[" + this.getClass().getName() + "] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
         }
         return group;
     }

    /**
     * Reads a project.
     *
     * @param id The id of the project.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsProject readProject(int id)
        throws CmsException {

        PreparedStatement statement = null;
        CmsProject project = null;
        ResultSet res = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_PROJECTS_READ"));

            statement.setInt(1,id);
            res = statement.executeQuery();

            if(res.next()) {
                project = new CmsProject(res.getInt(m_cq.get("C_PROJECTS_PROJECT_ID")),
                                         res.getString(m_cq.get("C_PROJECTS_PROJECT_NAME")),
                                         res.getString(m_cq.get("C_PROJECTS_PROJECT_DESCRIPTION")),
                                         res.getInt(m_cq.get("C_PROJECTS_TASK_ID")),
                                         res.getInt(m_cq.get("C_PROJECTS_USER_ID")),
                                         res.getInt(m_cq.get("C_PROJECTS_GROUP_ID")),
                                         res.getInt(m_cq.get("C_PROJECTS_MANAGERGROUP_ID")),
                                         res.getInt(m_cq.get("C_PROJECTS_PROJECT_FLAGS")),
                                         SqlHelper.getTimestamp(res,m_cq.get("C_PROJECTS_PROJECT_CREATEDATE")),
                                         res.getInt(m_cq.get("C_PROJECTS_PROJECT_TYPE")));
            } else {
                // project not found!
                throw new CmsException("[" + this.getClass().getName() + "] " + id,
                    CmsException.C_NOT_FOUND);
            }
         }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (Exception e) {
            throw new CmsException("["+this.getClass().getName()+"]", e);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return project;
    }

    /**
     * Reads a project by task-id.
     *
     * @param task The task to read the project for.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsProject readProject(CmsTask task)
        throws CmsException {

        PreparedStatement statement = null;
        CmsProject project = null;
        ResultSet res = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);

            statement = con.prepareStatement(m_cq.get("C_PROJECTS_READ_BYTASK"));

            statement.setInt(1,task.getId());
            res = statement.executeQuery();

            if(res.next())
                 project = new CmsProject(res,m_cq);
          else
                // project not found!
                throw new CmsException("[" + this.getClass().getName() + "] " + task,
                    CmsException.C_NOT_FOUND);
         }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return project;
    }

    /**
     * Reads all resource from the Cms, that are in one project.<BR/>
     * A resource is either a file header or a folder.
     *
     * @param project The id of the project in which the resource will be used.
     * @param filter The filter for the resources to be read
     *
     * @return A Vecor of resources.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public Vector readProjectView(int currentProject, int project, String filter)
        throws CmsException {

        Vector resources = new Vector();
        CmsResource file;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String addStatement = filter+" ORDER BY RESOURCE_NAME";
        try {
            con = DriverManager.getConnection(m_poolName);
            // read resource data from database
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_PROJECTVIEW")+addStatement);
            statement.setInt(1,project);
            res = statement.executeQuery();
            // create new resource
            while(res.next()) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                String resName=res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                //int projectId=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int projectId=currentProject;
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy= res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                int launcherType= res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                String launcherClass=  res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int resSize= res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");

                file=new CmsResource(resId,parentId,fileId,resName,resType,resFlags,
                                     userId,groupId,projectId,accessFlags,state,lockedBy,
                                     launcherType,launcherClass,created,modified,modifiedBy,
                                     resSize,lockedInProject);
                resources.addElement(file);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (Exception ex) {
            throw new CmsException("["+this.getClass().getName()+"]", ex);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return resources;
    }

    /**
     * Reads a project from the backup tables.
     *
     * @param versionId The versionId of the backup project.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsBackupProject readBackupProject(int versionId)
        throws CmsException {

        PreparedStatement statement = null;
        CmsBackupProject project = null;
        ResultSet res = null;
        Connection con = null;
        try {
            con = DriverManager.getConnection(m_poolNameBackup);
            statement = con.prepareStatement(m_cq.get("C_PROJECTS_READBYVERSION_BACKUP"));

            statement.setInt(1,versionId);
            res = statement.executeQuery();

            if(res.next()) {
                Vector projectresources = readBackupProjectResources(versionId);
                project = new CmsBackupProject(res.getInt("VERSION_ID"),
                                         res.getInt(m_cq.get("C_PROJECTS_PROJECT_ID")),
                                         res.getString(m_cq.get("C_PROJECTS_PROJECT_NAME")),
                                         SqlHelper.getTimestamp(res,"PROJECT_PUBLISHDATE"),
                                         res.getInt("PROJECT_PUBLISHED_BY"),
                                         res.getString("PROJECT_PUBLISHED_BY_NAME"),
                                         res.getString(m_cq.get("C_PROJECTS_PROJECT_DESCRIPTION")),
                                         res.getInt(m_cq.get("C_PROJECTS_TASK_ID")),
                                         res.getInt(m_cq.get("C_PROJECTS_USER_ID")),
                                         res.getString("USER_NAME"),
                                         res.getInt(m_cq.get("C_PROJECTS_GROUP_ID")),
                                         res.getString("GROUP_NAME"),
                                         res.getInt(m_cq.get("C_PROJECTS_MANAGERGROUP_ID")),
                                         res.getString("MANAGERGROUP_NAME"),
                                         SqlHelper.getTimestamp(res,m_cq.get("C_PROJECTS_PROJECT_CREATEDATE")),
                                         res.getInt(m_cq.get("C_PROJECTS_PROJECT_TYPE")),
                                         projectresources);
            } else {
                // project not found!
                throw new CmsException("[" + this.getClass().getName() + "] version " + versionId,
                    CmsException.C_NOT_FOUND);
            }
         }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (Exception e) {
            throw new CmsException("["+this.getClass().getName()+"]", e);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return project;
    }

    /**
     * Reads log entries for a project.
     *
     * @param project The projec for tasklog to read.
     * @return A Vector of new TaskLog objects
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public Vector readProjectLogs(int projectid)
        throws CmsException {
        ResultSet res = null;
        Connection con = null;

        CmsTaskLog tasklog = null;
        Vector logs = new Vector();
        PreparedStatement statement = null;
        String comment = null;
        String externalusername = null;
        java.sql.Timestamp starttime = null;
        int id = C_UNKNOWN_ID;
        int task = C_UNKNOWN_ID;
        int user = C_UNKNOWN_ID;
        int type = C_UNKNOWN_ID;

        try {
            con = DriverManager.getConnection(m_poolName);

            statement = con.prepareStatement(m_cq.get("C_TASKLOG_READ_PPROJECTLOGS"));
            statement.setInt(1, projectid);
            res = statement.executeQuery();
            while(res.next()) {
                comment = res.getString(m_cq.get("C_LOG_COMMENT"));
                externalusername = res.getString(m_cq.get("C_LOG_EXUSERNAME"));
                id = res.getInt(m_cq.get("C_LOG_ID"));
                starttime = SqlHelper.getTimestamp(res,m_cq.get("C_LOG_STARTTIME"));
                task = res.getInt(m_cq.get("C_LOG_TASK"));
                user = res.getInt(m_cq.get("C_LOG_USER"));
                type = res.getInt(m_cq.get("C_LOG_TYPE"));

                tasklog =  new CmsTaskLog(id, comment, task, user, starttime, type);
                logs.addElement(tasklog);
            }
        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } catch( Exception exc ) {
              throw new CmsException(exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return logs;
    }

    /**
     * Returns a property of a file or folder.
     *
     * @param meta The property-name of which the property has to be read.
     * @param resourceId The id of the resource.
     * @param resourceType The Type of the resource.
     *
     * @return property The property as string or null if the property not exists.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public String readProperty(String meta, int projectId, CmsResource resource, int resourceType)
        throws CmsException {
        ResultSet result = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        String returnValue = null;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            // create statement
            statement = con.prepareStatement(m_cq.get("C_PROPERTIES_READ"+usedStatement));
            statement.setInt(1, resource.getResourceId());
            statement.setString(2, meta);
            statement.setInt(3, resourceType);
            result = statement.executeQuery();
            // if resultset exists - return it
            if(result.next()) {
                returnValue = result.getString(m_cq.get("C_PROPERTY_VALUE"));
            }
        } catch( SQLException exc ) {
            throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                CmsException.C_SQL_ERROR, exc);
        } finally {
            // close all db-resources
            if(result != null) {
                try {
                    result.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return returnValue;
    }

    /**
     * Reads a propertydefinition for the given resource type.
     *
     * @param name The name of the propertydefinition to read.
     * @param type The resource type for which the propertydefinition is valid.
     *
     * @return propertydefinition The propertydefinition that corresponds to the overgiven
     * arguments - or null if there is no valid propertydefinition.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsPropertydefinition readPropertydefinition(String name, int type)
        throws CmsException {
         CmsPropertydefinition propDef=null;
         ResultSet res = null;
         PreparedStatement statement = null;
         Connection con = null;

         try {
             con = DriverManager.getConnection(m_poolName);

             // create statement
             statement = con.prepareStatement(m_cq.get("C_PROPERTYDEF_READ"));
             statement.setString(1,name);
             statement.setInt(2,type);
             res = statement.executeQuery();

            // if resultset exists - return it
            if(res.next()) {
                propDef = new CmsPropertydefinition( res.getInt(m_cq.get("C_PROPERTYDEF_ID")),
                                            res.getString(m_cq.get("C_PROPERTYDEF_NAME")),
                                            res.getInt(m_cq.get("C_PROPERTYDEF_RESOURCE_TYPE")));
            } else {
                res.close();
                res = null;
                // not found!
                throw new CmsException("[" + this.getClass().getName() + "] " + name,
                    CmsException.C_NOT_FOUND);
            }
         } catch( SQLException exc ) {
            throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                 CmsException.C_SQL_ERROR, exc);
         }finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
          }
         return propDef;
    }

    /**
     * Reads a propertydefinition for the given resource type.
     *
     * @param name The name of the propertydefinition to read.
     * @param type The resource type for which the propertydefinition is valid.
     *
     * @return propertydefinition The propertydefinition that corresponds to the overgiven
     * arguments - or null if there is no valid propertydefinition.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsPropertydefinition readPropertydefinition(String name, I_CmsResourceType type)
        throws CmsException {
        return( readPropertydefinition(name, type.getResourceType() ) );
    }

    /**
     * Reads a resource from the Cms.<BR/>
     * A resource is either a file header or a folder.
     *
     * @param project The project in which the resource will be used.
     * @param filename The complete name of the new file (including pathinformation).
     *
     * @return The resource read.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    protected CmsResource readResource(CmsProject project, String filename)
        throws CmsException {

        CmsResource file = null;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(project.getId()).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            // read resource data from database
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_READ"+usedStatement));
            statement.setString(1,filename);
            statement.setInt(2,project.getId());
            res = statement.executeQuery();
            // create new resource
            if(res.next()) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                String resName=res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int projectId=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy= res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                int launcherType= res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                String launcherClass=  res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int resSize= res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
                file=new CmsResource(resId,parentId,fileId,resName,resType,resFlags,
                                     userId,groupId,projectId,accessFlags,state,lockedBy,
                                     launcherType,launcherClass,created,modified,modifiedBy,
                                     resSize,lockedInProject);
            } else {
                res.close();
                res = null;
                throw new CmsException("["+this.getClass().getName()+"] "+filename,CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch( Exception exc ) {
            throw new CmsException("readResource "+exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return file;
    }

    /**
     * Reads all resource from the Cms, that are in one project.<BR/>
     * A resource is either a file header or a folder.
     *
     * @param project The project in which the resource will be used.
     *
     * @return A Vecor of resources.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public Vector readResources(CmsProject project)
        throws CmsException {

        Vector resources = new Vector();
        CmsResource file;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(project.getId()).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            // read resource data from database
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_READBYPROJECT"+usedStatement));
            statement.setInt(1,project.getId());
            res = statement.executeQuery();
            // create new resource
            while(res.next()) {
                int resId=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_ID"));
                int parentId=res.getInt(m_cq.get("C_RESOURCES_PARENT_ID"));
                String resName=res.getString(m_cq.get("C_RESOURCES_RESOURCE_NAME"));
                int resType= res.getInt(m_cq.get("C_RESOURCES_RESOURCE_TYPE"));
                int resFlags=res.getInt(m_cq.get("C_RESOURCES_RESOURCE_FLAGS"));
                int userId=res.getInt(m_cq.get("C_RESOURCES_USER_ID"));
                int groupId= res.getInt(m_cq.get("C_RESOURCES_GROUP_ID"));
                int projectId=res.getInt(m_cq.get("C_RESOURCES_PROJECT_ID"));
                int fileId=res.getInt(m_cq.get("C_RESOURCES_FILE_ID"));
                int accessFlags=res.getInt(m_cq.get("C_RESOURCES_ACCESS_FLAGS"));
                int state= res.getInt(m_cq.get("C_RESOURCES_STATE"));
                int lockedBy= res.getInt(m_cq.get("C_RESOURCES_LOCKED_BY"));
                int launcherType= res.getInt(m_cq.get("C_RESOURCES_LAUNCHER_TYPE"));
                String launcherClass=  res.getString(m_cq.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
                long created=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_CREATED")).getTime();
                long modified=SqlHelper.getTimestamp(res,m_cq.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                int modifiedBy=res.getInt(m_cq.get("C_RESOURCES_LASTMODIFIED_BY"));
                int resSize= res.getInt(m_cq.get("C_RESOURCES_SIZE"));
                int lockedInProject = res.getInt("LOCKED_IN_PROJECT");

                file=new CmsResource(resId,parentId,fileId,resName,resType,resFlags,
                                     userId,groupId,projectId,accessFlags,state,lockedBy,
                                     launcherType,launcherClass,created,modified,modifiedBy,
                                     resSize,lockedInProject);
                resources.addElement(file);
            }
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } catch (Exception ex) {
            throw new CmsException("["+this.getClass().getName()+"]", ex);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
        return resources;
    }

    /**
     * select a projectResource from an given project and resourcename
     *
     * @param project The project in which the resource is used.
     * @param resource The resource to be read from the Cms.
     *
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public String readProjectResource(int projectId, String resourcename) throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;
        ResultSet res = null;
        String resName = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_PROJECTRESOURCES_READ"));
            // select resource from the database
            statement.setInt(1, projectId);
            statement.setString(2, resourcename);
            res = statement.executeQuery();
            if (res.next()) {
                resName = res.getString("RESOURCE_NAME");
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + resourcename, CmsException.C_NOT_FOUND);
            }
            res.close();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            if (statement != null) {
                try{
                    statement.close();
                } catch (SQLException e){
                }
            }
            if (con != null){
                try{
                    con.close();
                } catch (SQLException e){
                }
            }
        }
        return resName;
    }

    /**
     * select a projectResource from an given project and resourcename
     *
     * @param project The project in which the resource is used.
     * @param resource The resource to be read from the Cms.
     *
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    private Vector readBackupProjectResources(int versionId) throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;
        ResultSet res = null;
        Vector projectResources = new Vector();
        try {
            con = DriverManager.getConnection(m_poolNameBackup);
            statement = con.prepareStatement(m_cq.get("C_PROJECTRESOURCES_READ_BACKUP"));
            // select resource from the database
            statement.setInt(1, versionId);
            res = statement.executeQuery();
            while (res.next()) {
                projectResources.addElement(res.getString("RESOURCE_NAME"));
            }
            res.close();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            if (statement != null) {
                try{
                    statement.close();
                } catch (SQLException e){
                }
            }
            if (con != null){
                try{
                    con.close();
                } catch (SQLException e){
                }
            }
        }
        return projectResources;
    }

    /**
     * Reads a session from the database.
     *
     * @param sessionId, the id og the session to read.
     * @return the read session as Hashtable.
     * @exception thorws CmsException if something goes wrong.
     */
    public Hashtable readSession(String sessionId)
        throws CmsException {
        PreparedStatement statement = null;
        ResultSet res = null;
        Hashtable sessionData = new Hashtable();
        Hashtable data = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_SESSION_READ"));
            statement.setString(1,sessionId);
            statement.setTimestamp(2,new java.sql.Timestamp(System.currentTimeMillis() - C_SESSION_TIMEOUT ));

            res = statement.executeQuery();

            // create new Cms user object
            if(res.next()) {
                // read the additional infos.
                byte[] value = res.getBytes(1);
                // now deserialize the object
                ByteArrayInputStream bin= new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                data = (Hashtable) oin.readObject();
                                int a = 1;
                                try {
                      for(;;) {
                    Object key = oin.readObject();
                    Object sessionValue = oin.readObject();
                    sessionData.put(key, sessionValue);
                                  }
                                } catch(EOFException exc) {
                                  // reached eof - stop reading all is done now.
                }
                data.put(C_SESSION_DATA, sessionData);
            } else {
                deleteSessions();
            }
         }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        }
        catch (Exception e) {
            throw new CmsException("["+this.getClass().getName()+"]", e);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return data;
    }

     /**
     * Reads a serializable object from the systempropertys.
     *
     * @param name The name of the property.
     *
     * @return object The property-object.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public Serializable readSystemProperty(String name)
        throws CmsException {

        Serializable property=null;
        byte[] value;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;

        // create get the property data from the database
        try {
            con = DriverManager.getConnection(m_poolName);
          statement=con.prepareStatement(m_cq.get("C_SYSTEMPROPERTIES_READ"));
          statement.setString(1,name);
          res = statement.executeQuery();
          if(res.next()) {
                value = res.getBytes(m_cq.get("C_SYSTEMPROPERTY_VALUE"));
                // now deserialize the object
                ByteArrayInputStream bin= new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                property=(Serializable)oin.readObject();
            }
        }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        }
        catch (IOException e){
            throw new CmsException("["+this.getClass().getName()+"]"+CmsException. C_SERIALIZATION, e);
        }
        catch (ClassNotFoundException e){
            throw new CmsException("["+this.getClass().getName()+"]"+CmsException. C_SERIALIZATION, e);
        }finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
          }
        return property;
    }

/**
 * Reads a task from the Cms.
 *
 * @param id The id of the task to read.
 *
 * @return a task object or null if the task is not found.
 *
 * @exception CmsException Throws CmsException if something goes wrong.
 */
public CmsTask readTask(int id) throws CmsException {
    ResultSet res = null;
    CmsTask task = null;
    Statement statement = null;
    Connection con = null;


    try {
        con = DriverManager.getConnection(m_poolName);
        // At this place I use a statement instead of a prepared statement because for some odd reason
        // the prepared statement doesn't always work correctly. At the time the beans are spilled we should
        // switch back to the prepared statement. Mario Stanke
        statement = con.createStatement();
        res = statement.executeQuery(m_cq.get("C_TASK_READ_STATEMENT")+id);
        if (res.next()) {
            int autofinish = res.getInt(m_cq.get("C_TASK_AUTOFINISH"));
            java.sql.Timestamp endtime = SqlHelper.getTimestamp(res, m_cq.get("C_TASK_ENDTIME"));
            int escalationtype = res.getInt(m_cq.get("C_TASK_ESCALATIONTYPE"));
            id = res.getInt(m_cq.get("C_TASK_ID"));
            int initiatoruser = res.getInt(m_cq.get("C_TASK_INITIATORUSER"));
            int milestone = res.getInt(m_cq.get("C_TASK_MILESTONE"));
            String name = res.getString(m_cq.get("C_TASK_NAME"));
            int originaluser = res.getInt(m_cq.get("C_TASK_ORIGINALUSER"));
            int agentuser = res.getInt(m_cq.get("C_TASK_AGENTUSER"));
            int parent = res.getInt(m_cq.get("C_TASK_PARENT"));
            int percentage = res.getInt(m_cq.get("C_TASK_PERCENTAGE"));
            String permission = res.getString(m_cq.get("C_TASK_PERMISSION"));
            int priority = res.getInt(m_cq.get("C_TASK_PRIORITY"));
            int role = res.getInt(m_cq.get("C_TASK_ROLE"));
            int root = res.getInt(m_cq.get("C_TASK_ROOT"));
            java.sql.Timestamp starttime = SqlHelper.getTimestamp(res, m_cq.get("C_TASK_STARTTIME"));
            int state = res.getInt(m_cq.get("C_TASK_STATE"));
            int tasktype = res.getInt(m_cq.get("C_TASK_TASKTYPE"));
            java.sql.Timestamp timeout = SqlHelper.getTimestamp(res, m_cq.get("C_TASK_TIMEOUT"));
            java.sql.Timestamp wakeuptime = SqlHelper.getTimestamp(res, m_cq.get("C_TASK_WAKEUPTIME"));
            String htmllink = res.getString(m_cq.get("C_TASK_HTMLLINK"));
            task = new CmsTask(id, name, state, tasktype, root, parent, initiatoruser, role, agentuser, originaluser, starttime, wakeuptime, timeout, endtime, percentage, permission, priority, escalationtype, htmllink, milestone, autofinish);
        }
    } catch (SQLException exc) {
        throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
    } catch (Exception exc) {
        throw new CmsException(exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
    } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
    }
    return task;
}

    /**
     * Reads a log for a task.
     *
     * @param id The id for the tasklog .
     * @return A new TaskLog object
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsTaskLog readTaskLog(int id)
        throws CmsException {
        ResultSet res = null;
        CmsTaskLog tasklog = null;
        PreparedStatement statement = null;
        Connection con = null;


        try {
            con = DriverManager.getConnection(m_poolName);

            statement = con.prepareStatement(m_cq.get("C_TASKLOG_READ"));
            statement.setInt(1, id);
            res = statement.executeQuery();
            if(res.next()) {
                String comment = res.getString(m_cq.get("C_LOG_COMMENT"));
                String externalusername;
                id = res.getInt(m_cq.get("C_LOG_ID"));
                java.sql.Timestamp starttime = SqlHelper.getTimestamp(res,m_cq.get("C_LOG_STARTTIME"));
                int task = res.getInt(m_cq.get("C_LOG_TASK"));
                int user = res.getInt(m_cq.get("C_LOG_USER"));
                int type = res.getInt(m_cq.get("C_LOG_TYPE"));

                tasklog =  new CmsTaskLog(id, comment, task, user, starttime, type);
            }
        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } catch( Exception exc ) {
              throw new CmsException(exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }

        return tasklog;
    }

    /**
     * Reads log entries for a task.
     *
     * @param taskid The id of the task for the tasklog to read .
     * @return A Vector of new TaskLog objects
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public Vector readTaskLogs(int taskId)
        throws CmsException {
        Connection con = null;
        ResultSet res = null;
        CmsTaskLog tasklog = null;
        Vector logs = new Vector();
        PreparedStatement statement = null;
        String comment = null;
        String externalusername = null;
        java.sql.Timestamp starttime = null;
        int id = C_UNKNOWN_ID;
        int task = C_UNKNOWN_ID;
        int user = C_UNKNOWN_ID;
        int type = C_UNKNOWN_ID;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_TASKLOG_READ_LOGS"));
            statement.setInt(1, taskId);
            res = statement.executeQuery();
            while(res.next()) {
                comment = res.getString(m_cq.get("C_TASKLOG_COMMENT"));
                externalusername = res.getString(m_cq.get("C_TASKLOG_EXUSERNAME"));
                id = res.getInt(m_cq.get("C_TASKLOG_ID"));
                starttime = SqlHelper.getTimestamp(res,m_cq.get("C_TASKLOG_STARTTIME"));
                task = res.getInt(m_cq.get("C_TASKLOG_TASK"));
                user = res.getInt(m_cq.get("C_TASKLOG_USER"));
                type = res.getInt(m_cq.get("C_TASKLOG_TYPE"));
                tasklog =  new CmsTaskLog(id, comment, task, user, starttime, type);
                logs.addElement(tasklog);
            }
        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } catch( Exception exc ) {
              throw new CmsException(exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return logs;
    }

    /**
     * Reads all tasks of a user in a project.
     * @param project The Project in which the tasks are defined.
     * @param agent The task agent
     * @param owner The task owner .
     * @param group The group who has to process the task.
     * @tasktype C_TASKS_ALL, C_TASKS_OPEN, C_TASKS_DONE, C_TASKS_NEW
     * @param orderBy Chooses, how to order the tasks.
     * @param sort Sort Ascending or Descending (ASC or DESC)
     *
     * @return A vector with the tasks
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public Vector readTasks(CmsProject project, CmsUser agent, CmsUser owner,
                            CmsGroup role, int tasktype,
                            String orderBy, String sort)
        throws CmsException {
        boolean first = true;
        Vector tasks = new Vector(); // vector for the return result
        CmsTask task = null;         // tmp task for adding to vector
        ResultSet recset = null;
        Connection con = null;

        // create the sql string depending on parameters
        // handle the project for the SQL String
        String sqlstr = "SELECT * FROM " + m_cq.get("C_TABLENAME_TASK")+" WHERE ";
        if(project!=null){
            sqlstr = sqlstr + m_cq.get("C_TASK_ROOT") + "=" + project.getTaskId();
            first = false;
        }
        else
        {
            sqlstr = sqlstr + m_cq.get("C_TASK_ROOT") + "<>0 AND " + m_cq.get("C_TASK_PARENT") + "<>0";
            first = false;
        }

        // handle the agent for the SQL String
        if(agent!=null){
            if(!first){
                sqlstr = sqlstr + " AND ";
            }
            sqlstr = sqlstr + m_cq.get("C_TASK_AGENTUSER") + "=" + agent.getId();
            first = false;
        }

        // handle the owner for the SQL String
        if(owner!=null){
            if(!first){
                sqlstr = sqlstr + " AND ";
            }
            sqlstr = sqlstr + m_cq.get("C_TASK_INITIATORUSER") + "=" + owner.getId();
            first = false;
        }

        // handle the role for the SQL String
        if(role!=null){
            if(!first){
                sqlstr = sqlstr+" AND ";
            }
            sqlstr = sqlstr + m_cq.get("C_TASK_ROLE") + "=" + role.getId();
            first = false;
        }

        sqlstr = sqlstr + getTaskTypeConditon(first, tasktype);

        // handel the order and sort parameter for the SQL String
        if(orderBy!=null) {
            if(!orderBy.equals("")) {
                sqlstr = sqlstr + " ORDER BY " + orderBy;
                if(orderBy!=null) {
                    if(!orderBy.equals("")) {
                        sqlstr = sqlstr + " " + sort;
                    }
                }
            }
        }

        Statement statement = null;

        try {
            con = DriverManager.getConnection(m_poolName);

            statement = con.createStatement();
            recset = statement.executeQuery(sqlstr);

            // if resultset exists - return vector of tasks
            while(recset.next()) {
                task =  new CmsTask(recset.getInt(m_cq.get("C_TASK_ID")),
                                    recset.getString(m_cq.get("C_TASK_NAME")),
                                    recset.getInt(m_cq.get("C_TASK_STATE")),
                                    recset.getInt(m_cq.get("C_TASK_TASKTYPE")),
                                    recset.getInt(m_cq.get("C_TASK_ROOT")),
                                    recset.getInt(m_cq.get("C_TASK_PARENT")),
                                    recset.getInt(m_cq.get("C_TASK_INITIATORUSER")),
                                    recset.getInt(m_cq.get("C_TASK_ROLE")),
                                    recset.getInt(m_cq.get("C_TASK_AGENTUSER")),
                                    recset.getInt(m_cq.get("C_TASK_ORIGINALUSER")),
                                    SqlHelper.getTimestamp(recset,m_cq.get("C_TASK_STARTTIME")),
                                    SqlHelper.getTimestamp(recset,m_cq.get("C_TASK_WAKEUPTIME")),
                                    SqlHelper.getTimestamp(recset,m_cq.get("C_TASK_TIMEOUT")),
                                    SqlHelper.getTimestamp(recset,m_cq.get("C_TASK_ENDTIME")),
                                    recset.getInt(m_cq.get("C_TASK_PERCENTAGE")),
                                    recset.getString(m_cq.get("C_TASK_PERMISSION")),
                                    recset.getInt(m_cq.get("C_TASK_PRIORITY")),
                                    recset.getInt(m_cq.get("C_TASK_ESCALATIONTYPE")),
                                    recset.getString(m_cq.get("C_TASK_HTMLLINK")),
                                    recset.getInt(m_cq.get("C_TASK_MILESTONE")),
                                    recset.getInt(m_cq.get("C_TASK_AUTOFINISH")));


                tasks.addElement(task);
            }

        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } catch( Exception exc ) {
              throw new CmsException(exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            // close all db-resources
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }

        return tasks;
    }

    /**
     * Reads a user from the cms, only if the password is correct.
     *
     * @param id the id of the user.
     * @param type the type of the user.
     * @return the read user.
     * @exception thorws CmsException if something goes wrong.
     */
    public CmsUser readUser(int id)
        throws CmsException {
        PreparedStatement statement = null;
        ResultSet res = null;
        CmsUser user = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_USERS_READID"));
            statement.setInt(1,id);
            res = statement.executeQuery();

            // create new Cms user object
            if(res.next()) {
                // read the additional infos.
                byte[] value = res.getBytes(m_cq.get("C_USERS_USER_INFO"));
                // now deserialize the object
                ByteArrayInputStream bin= new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                Hashtable info=(Hashtable)oin.readObject();

                user = new CmsUser(res.getInt(m_cq.get("C_USERS_USER_ID")),
                                   res.getString(m_cq.get("C_USERS_USER_NAME")),
                                   res.getString(m_cq.get("C_USERS_USER_PASSWORD")),
                                   res.getString(m_cq.get("C_USERS_USER_RECOVERY_PASSWORD")),
                                   res.getString(m_cq.get("C_USERS_USER_DESCRIPTION")),
                                   res.getString(m_cq.get("C_USERS_USER_FIRSTNAME")),
                                   res.getString(m_cq.get("C_USERS_USER_LASTNAME")),
                                   res.getString(m_cq.get("C_USERS_USER_EMAIL")),
                                   SqlHelper.getTimestamp(res,m_cq.get("C_USERS_USER_LASTLOGIN")).getTime(),
                                   SqlHelper.getTimestamp(res,m_cq.get("C_USERS_USER_LASTUSED")).getTime(),
                                   res.getInt(m_cq.get("C_USERS_USER_FLAGS")),
                                   info,
                                   new CmsGroup(res.getInt(m_cq.get("C_GROUPS_GROUP_ID")),
                                                res.getInt(m_cq.get("C_GROUPS_PARENT_GROUP_ID")),
                                                res.getString(m_cq.get("C_GROUPS_GROUP_NAME")),
                                                res.getString(m_cq.get("C_GROUPS_GROUP_DESCRIPTION")),
                                                res.getInt(m_cq.get("C_GROUPS_GROUP_FLAGS"))),
                                   res.getString(m_cq.get("C_USERS_USER_ADDRESS")),
                                   res.getString(m_cq.get("C_USERS_USER_SECTION")),
                                   res.getInt(m_cq.get("C_USERS_USER_TYPE")));
            } else {
                res.close();
                res = null;
                throw new CmsException("["+this.getClass().getName()+"]"+id,CmsException.C_NO_USER);
            }
            return user;
         }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        }
        // a.lucas: catch CmsException here and throw it again.
        // Don't wrap another CmsException around it, since this may cause problems during login.
        catch (CmsException e) {
            throw e;
        }
        catch (Exception e) {
            throw new CmsException("["+this.getClass().getName()+"]", e);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
    }

    /**
     * Reads a user from the cms.
     *
     * @param name the name of the user.
     * @param type the type of the user.
     * @return the read user.
     * @exception thorws CmsException if something goes wrong.
     */
    public CmsUser readUser(String name, int type)
        throws CmsException {
        PreparedStatement statement = null;
        ResultSet res = null;
        CmsUser user = null;
        Connection con = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_USERS_READ"));
            statement.setString(1,name);
            statement.setInt(2,type);

            res = statement.executeQuery();

            // create new Cms user object
            if(res.next()) {
                // read the additional infos.
                byte[] value = res.getBytes(m_cq.get("C_USERS_USER_INFO"));
                // now deserialize the object
                ByteArrayInputStream bin= new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                Hashtable info=(Hashtable)oin.readObject();

                user = new CmsUser(res.getInt(m_cq.get("C_USERS_USER_ID")),
                                   res.getString(m_cq.get("C_USERS_USER_NAME")),
                                   res.getString(m_cq.get("C_USERS_USER_PASSWORD")),
                                   res.getString(m_cq.get("C_USERS_USER_RECOVERY_PASSWORD")),
                                   res.getString(m_cq.get("C_USERS_USER_DESCRIPTION")),
                                   res.getString(m_cq.get("C_USERS_USER_FIRSTNAME")),
                                   res.getString(m_cq.get("C_USERS_USER_LASTNAME")),
                                   res.getString(m_cq.get("C_USERS_USER_EMAIL")),
                                   SqlHelper.getTimestamp(res,m_cq.get("C_USERS_USER_LASTLOGIN")).getTime(),
                                   SqlHelper.getTimestamp(res,m_cq.get("C_USERS_USER_LASTUSED")).getTime(),
                                   res.getInt(m_cq.get("C_USERS_USER_FLAGS")),
                                   info,
                                   new CmsGroup(res.getInt(m_cq.get("C_GROUPS_GROUP_ID")),
                                                res.getInt(m_cq.get("C_GROUPS_PARENT_GROUP_ID")),
                                                res.getString(m_cq.get("C_GROUPS_GROUP_NAME")),
                                                res.getString(m_cq.get("C_GROUPS_GROUP_DESCRIPTION")),
                                                res.getInt(m_cq.get("C_GROUPS_GROUP_FLAGS"))),
                                   res.getString(m_cq.get("C_USERS_USER_ADDRESS")),
                                   res.getString(m_cq.get("C_USERS_USER_SECTION")),
                                   res.getInt(m_cq.get("C_USERS_USER_TYPE")));
            } else {
                res.close();
                res = null;
                throw new CmsException("["+this.getClass().getName()+"]"+name,CmsException.C_NO_USER);
            }

            return user;
         }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        }
        // a.lucas: catch CmsException here and throw it again.
        // Don't wrap another CmsException around it, since this may cause problems during login.
        catch (CmsException e) {
            throw e;
        }
        catch (Exception e) {
            throw new CmsException("["+this.getClass().getName()+"]", e);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
    }

    /**
     * Reads a user from the cms, only if the password is correct.
     *
     * @param name the name of the user.
     * @param password the password of the user.
     * @param type the type of the user.
     * @return the read user.
     * @exception thorws CmsException if something goes wrong.
     */
    public CmsUser readUser(String name, String password, int type)
        throws CmsException {
        PreparedStatement statement = null;
        ResultSet res = null;
        CmsUser user = null;
        Connection con = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_USERS_READPW"));
            statement.setString(1,name);
            statement.setString(2,digest(password));
            statement.setInt(3,type);
            res = statement.executeQuery();

            // create new Cms user object
            if(res.next()) {
                // read the additional infos.
                byte[] value = res.getBytes(m_cq.get("C_USERS_USER_INFO"));
                // now deserialize the object
                ByteArrayInputStream bin= new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                Hashtable info=(Hashtable)oin.readObject();

                user = new CmsUser(res.getInt(m_cq.get("C_USERS_USER_ID")),
                                   res.getString(m_cq.get("C_USERS_USER_NAME")),
                                   res.getString(m_cq.get("C_USERS_USER_PASSWORD")),
                                   res.getString(m_cq.get("C_USERS_USER_RECOVERY_PASSWORD")),
                                   res.getString(m_cq.get("C_USERS_USER_DESCRIPTION")),
                                   res.getString(m_cq.get("C_USERS_USER_FIRSTNAME")),
                                   res.getString(m_cq.get("C_USERS_USER_LASTNAME")),
                                   res.getString(m_cq.get("C_USERS_USER_EMAIL")),
                                   SqlHelper.getTimestamp(res,m_cq.get("C_USERS_USER_LASTLOGIN")).getTime(),
                                   SqlHelper.getTimestamp(res,m_cq.get("C_USERS_USER_LASTUSED")).getTime(),
                                   res.getInt(m_cq.get("C_USERS_USER_FLAGS")),
                                   info,
                                   new CmsGroup(res.getInt(m_cq.get("C_GROUPS_GROUP_ID")),
                                                res.getInt(m_cq.get("C_GROUPS_PARENT_GROUP_ID")),
                                                res.getString(m_cq.get("C_GROUPS_GROUP_NAME")),
                                                res.getString(m_cq.get("C_GROUPS_GROUP_DESCRIPTION")),
                                                res.getInt(m_cq.get("C_GROUPS_GROUP_FLAGS"))),
                                   res.getString(m_cq.get("C_USERS_USER_ADDRESS")),
                                   res.getString(m_cq.get("C_USERS_USER_SECTION")),
                                   res.getInt(m_cq.get("C_USERS_USER_TYPE")));
            } else {
                res.close();
                res = null;
                throw new CmsException("["+this.getClass().getName()+"]"+name,CmsException.C_NO_USER);
            }

            return user;
         }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        }
        // a.lucas: catch CmsException here and throw it again.
        // Don't wrap another CmsException around it, since this may cause problems during login.
        catch (CmsException e) {
            throw e;
        }
        catch (Exception e) {
            throw new CmsException("["+this.getClass().getName()+"]", e);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
    }

    /**
     * Sets the password, only if the user knows the recovery-password.
     *
     * @param user the user to set the password for.
     * @param recoveryPassword the recoveryPassword the user has to know to set the password.
     * @param password the password to set
     * @exception thorws CmsException if something goes wrong.
     */
    public void recoverPassword(String user, String recoveryPassword, String password )
        throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;

        int result;

        try {
            con = DriverManager.getConnection(m_poolName);

            statement = con.prepareStatement(m_cq.get("C_USERS_RECOVERPW"));

            statement.setString(1,digest(password));
            statement.setString(2,user);
            statement.setString(3,digest(recoveryPassword));
            result = statement.executeUpdate();
        }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            // close all db-resources
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        if(result != 1) {
            // the update wasn't succesfull -> throw exception
            throw new CmsException("["+this.getClass().getName()+"] the password couldn't be recovered.");
        }
    }

     /**
      * Deletes a file in the database.
      * This method is used to physically remove a file form the database.
      *
      * @param project The project in which the resource will be used.
      * @param filename The complete path of the file.
      * @exception CmsException Throws CmsException if operation was not succesful
      */
     public void removeFile(int projectId, String filename)
        throws CmsException{

        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        CmsResource resource = readFileHeader(projectId, filename, true);
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            // delete the file header
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_DELETE"+usedStatement));
            statement.setString(1, filename);
            statement.executeUpdate();
            statement.close();
            // delete the file content
            statement = con.prepareStatement(m_cq.get("C_FILE_DELETE"+usedStatement));
            statement.setInt(1, resource.getFileId());
            statement.executeUpdate();
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            // close all db-resources
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
    }

    /**
     * Deletes a folder in the database.
     * This method is used to physically remove a folder form the database.
     *
     * @param folder The folder.
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public void removeFolder(int projectId, CmsFolder folder)
        throws CmsException{

        // the current implementation only deletes empty folders
        // check if the folder has any files in it
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        Vector files= getFilesInFolder(projectId, folder);
        files=getUndeletedResources(files);
        if (files.size()==0) {
            // check if the folder has any folders in it
            Vector folders= getSubFolders(projectId, folder);
            folders=getUndeletedResources(folders);
            if (folders.size()==0) {
                //this folder is empty, delete it
                Connection con = null;
                PreparedStatement statement = null;
                try {
                    con = DriverManager.getConnection(usedPool);
                    // delete the folder
                    statement = con.prepareStatement(m_cq.get("C_RESOURCES_ID_DELETE"+usedStatement));
                    statement.setInt(1,folder.getResourceId());
                    statement.executeUpdate();
                } catch (SQLException e){
                    throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
                } finally {
                    if(statement != null) {
                        try {
                            statement.close();
                        } catch(SQLException exc) {
                            // nothing to do here
                        }
                    }
                    if(con != null) {
                        try {
                            con.close();
                        } catch(SQLException exc) {
                            // nothing to do here
                        }
                    }
                }
            } else {
                throw new CmsException("["+this.getClass().getName()+"] "+folder.getAbsolutePath(),CmsException.C_NOT_EMPTY);
            }
        } else {
            throw new CmsException("["+this.getClass().getName()+"] "+folder.getAbsolutePath(),CmsException.C_NOT_EMPTY);
        }
    }

    /**
     * Deletes a folder in the database.
     * This method is used to physically remove a folder form the database.
     * It is internally used by the publish project method.
     *
     * @param project The project in which the resource will be used.
     * @param foldername The complete path of the folder.
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    protected void removeFolderForPublish(int projectId, String foldername)
        throws CmsException{

        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if(projectId == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            // delete the folder
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_DELETE"+usedStatement));
            statement.setString(1, foldername);
            statement.executeUpdate();
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            // close all db-resources
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
    }

    /**
     * Removes a user from a group.
     *
     * Only the admin can do this.<P/>
     *
     * @param userid The id of the user that is to be added to the group.
     * @param groupid The id of the group.
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
     public void removeUserFromGroup(int userid, int groupid)
         throws CmsException {
         PreparedStatement statement = null;
         Connection con = null;
         try {
             con = DriverManager.getConnection(m_poolName);
             // create statement
             statement = con.prepareStatement(m_cq.get("C_GROUPS_REMOVEUSERFROMGROUP"));

             statement.setInt(1,groupid);
             statement.setInt(2,userid);
             statement.executeUpdate();

         } catch (SQLException e){

            throw new CmsException("[" + this.getClass().getName() + "] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            // close all db-resources
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
     }

    /**
     * Renames the file to the new name.
     *
     * @param project The project in which the resource will be used.
     * @param onlineProject The online project of the OpenCms.
     * @param userId The user id
     * @param oldfileID The id of the resource which will be renamed.
     * @param newname The new name of the resource.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public void renameFile(CmsProject project,
                            CmsProject onlineProject,
                            int userId,
                            int oldfileID,
                            String newname)
        throws CmsException {

        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        if (project.getId() == onlineProject.getId()){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try{
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_RENAMERESOURCE"+usedStatement));

            statement.setString(1,newname);
            statement.setInt(2,userId);
            statement.setInt(3,oldfileID);
            statement.executeUpdate();

        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
    }

    /**
     * Sets a new password for a user.
     *
     * @param user the user to set the password for.
     * @param password the password to set
     * @exception thorws CmsException if something goes wrong.
     */
    public void setPassword(String user, String password)
        throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_USERS_SETPW"));
            statement.setString(1,digest(password));
            statement.setString(2,user);
            statement.executeUpdate();
        }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
    }

    /**
     * Sets a new password for a user.
     *
     * @param user the user to set the password for.
     * @param password the recoveryPassword to set
     * @exception thorws CmsException if something goes wrong.
     */
    public void setRecoveryPassword(String user, String password)
        throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;
        int result;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_USERS_SETRECPW"));

            statement.setString(1,digest(password));
            statement.setString(2,user);
            result = statement.executeUpdate();
        }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            // close all db-resources
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        if(result != 1) {
            // the update wasn't succesfull -> throw exception
            throw new CmsException("["+this.getClass().getName()+"] new password couldn't be set.");
        }
    }

    /**
     * Set a Parameter for a task.
     *
     * @param task The task.
     * @param parname Name of the parameter.
     * @param parvalue Value if the parameter.
     *
     * @return The id of the inserted parameter or 0 if the parameter exists for this task.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public int setTaskPar(int taskId, String parname, String parvalue)
        throws CmsException {

        ResultSet res = null;
        int result = 0;
        Connection con = null;
        PreparedStatement statement = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            // test if the parameter already exists for this task
            statement = con.prepareStatement(m_cq.get("C_TASKPAR_TEST"));
            statement.setInt(1, taskId);
            statement.setString(2, parname);
            res = statement.executeQuery();

            if(res.next()) {
                //Parameter exisits, so make an update
                updateTaskPar(res.getInt(m_cq.get("C_PAR_ID")), parname, parvalue);
            }
            else {
                //Parameter is not exisiting, so make an insert
                result = insertTaskPar(taskId, parname, parvalue);

            }
        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return result;
    }

    /**
     * Sorts a vector of files or folders alphabetically.
     * This method uses an insertion sort algorithm.
     * NOT IN USE AT THIS TIME
     *
     * @param unsortedList Array of strings containing the list of files or folders.
     * @return Array of sorted strings.
     */
    protected Vector SortEntrys(Vector list)
    {
        int in, out;
        int nElem = list.size();
        CmsResource[] unsortedList = new CmsResource[list.size()];
        for (int i = 0; i < list.size(); i++)
        {
            unsortedList[i] = (CmsResource) list.elementAt(i);
        }
        for (out = 1; out < nElem; out++)
        {
            CmsResource temp = unsortedList[out];
            in = out;
            while (in > 0 && unsortedList[in - 1].getAbsolutePath().compareTo(temp.getAbsolutePath()) >= 0)
            {
                unsortedList[in] = unsortedList[in - 1];
                --in;
            }
            unsortedList[in] = temp;
        }
        Vector sortedList = new Vector();
        for (int i = 0; i < list.size(); i++)
        {
            sortedList.addElement(unsortedList[i]);
        }
        return sortedList;
    }

    /**
     * Undeletes the file.
     *
     * @param project The project in which the resource will be used.
     * @param filename The complete path of the file.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public void undeleteFile(CmsProject project, String filename)
        throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(project.getId()).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_REMOVE"+usedStatement));
            // mark the file as deleted
            statement.setInt(1,C_STATE_CHANGED);
            statement.setInt(2,C_UNKNOWN_ID);
            statement.setString(3, filename);
            statement.executeUpdate();
         } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
         }finally {
            // close all db-resources
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
         }
     }

    /**
     * Unlocks all resources in this project.
     *
     * @param project The project to be unlocked.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void unlockProject(CmsProject project)
        throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(project.getId()).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            // create the statement
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_UNLOCK"+usedStatement));
            statement.setInt(1,project.getId());
            statement.executeUpdate();
        } catch( Exception exc ) {
            throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                CmsException.C_SQL_ERROR, exc);
        } finally {
            // close all db-resources
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
    }

    /**
     * Updates the LOCKED_BY state of a Resource.
     * Creation date: (29.08.00 15:01:55)
     * @param res com.opencms.file.CmsResource
     * @exception com.opencms.core.CmsException The exception description.
     */
    public void updateLockstate(CmsResource res, int projectId) throws CmsException {

        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(res.getProjectId()).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_UPDATE_LOCK"+usedStatement));
            statement.setInt(1, res.isLockedBy());
            statement.setInt(2, projectId);
            statement.setInt(3, res.getResourceId());
            statement.executeUpdate();
        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            // close all db-resources
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
    }

    /**
     * Updates the state of a Resource.
     *
     * @param res com.opencms.file.CmsResource
     * @exception com.opencms.core.CmsException The exception description.
     */
    public void updateResourcestate(CmsResource res) throws CmsException {

        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(res.getProjectId()).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (res.getProjectId() == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_UPDATE_STATE"+usedStatement));
            statement.setInt(1, res.getState());
            statement.setInt(2, res.getResourceId());
            statement.executeUpdate();
        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            // close all db-resources
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
    }

    /**
     * This method updates a session in the database. It is used
     * for sessionfailover.
     *
     * @param sessionId the id of the session.
     * @return data the sessionData.
     */
    public int updateSession(String sessionId, Hashtable data)
        throws CmsException {
        byte[] value=null;
        PreparedStatement statement = null;
        Connection con = null;
        int retValue;

        try {
                  value = serializeSession(data);
            con = DriverManager.getConnection(m_poolName);

            // write data to database
            statement = con.prepareStatement(m_cq.get("C_SESSION_UPDATE"));

            statement.setTimestamp(1,new java.sql.Timestamp(System.currentTimeMillis()));
            statement.setBytes(2,value);
            statement.setString(3,sessionId);
            retValue = statement.executeUpdate();
        }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        }
        catch (IOException e){
            throw new CmsException("["+this.getClass().getName()+"]:"+CmsException.C_SERIALIZATION, e);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return retValue;
    }
    protected void updateTaskPar(int parid, String parname, String parvalue)
        throws CmsException {

        PreparedStatement statement = null;
        Connection con = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_TASKPAR_UPDATE"));
            statement.setString(1, parvalue);
            statement.setInt(2, parid);
            statement.executeUpdate();
        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
    }
    protected void updateTaskType(int taskId, int autofinish, int escalationtyperef, String htmllink, String name, String permission, int priorityref, int roleref)
        throws CmsException {

        PreparedStatement statement = null;
        Connection con = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_TASKTYPE_UPDATE"));
            statement.setInt(1, autofinish);
            statement.setInt(2, escalationtyperef);
            statement.setString(3, htmllink);
            statement.setString(4, name);
            statement.setString(5, permission);
            statement.setInt(6, priorityref);
            statement.setInt(7, roleref);
            statement.setInt(8, taskId);
            statement.executeUpdate();
        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
    }

    /**
     * Checks if a user is member of a group.<P/>
     *
     * @param nameid The id of the user to check.
     * @param groupid The id of the group to check.
     * @return True or False
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
     public boolean userInGroup(int userid, int groupid)
         throws CmsException {
         boolean userInGroup=false;
         PreparedStatement statement = null;
         ResultSet res = null;
         Connection con = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            // create statement
            statement = con.prepareStatement(m_cq.get("C_GROUPS_USERINGROUP"));

            statement.setInt(1,groupid);
            statement.setInt(2,userid);
            res = statement.executeQuery();
            if (res.next()){
                userInGroup=true;
            }
         } catch (SQLException e){
            throw new CmsException("[" + this.getClass().getName() + "] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
         } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
         return userInGroup;
     }

    /**
     * Writes a file to the Cms.<BR/>
     *
     * @param project The project in which the resource will be used.
     * @param onlineProject The online project of the OpenCms.
     * @param file The new file.
     * @param changed Flag indicating if the file state must be set to changed.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public void writeFile(CmsProject project,
                           CmsProject onlineProject,
                           CmsFile file, boolean changed)
        throws CmsException {
        writeFile(project, onlineProject, file, changed, file.getResourceLastModifiedBy());
    }

    /**
     * Writes a file to the Cms.<BR/>
     *
     * @param project The project in which the resource will be used.
     * @param onlineProject The online project of the OpenCms.
     * @param file The new file.
     * @param changed Flag indicating if the file state must be set to changed.
     * @param userId The id of the user who has changed the resource.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public void writeFile(CmsProject project,
                           CmsProject onlineProject,
                           CmsFile file, boolean changed, int userId)
        throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;
        String usedPool;
        String usedStatement;
        int modifiedBy = userId;
        if (project.getId() == onlineProject.getId()){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
            modifiedBy = file.getResourceLastModifiedBy();
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            // update the file header in the RESOURCE database.
            writeFileHeader(project, file, changed, userId);
            // update the file content in the FILES database.
            statement = con.prepareStatement(m_cq.get("C_FILES_UPDATE"+usedStatement));
            statement.setBytes(1,file.getContents());
            statement.setInt(2,file.getFileId());
            statement.executeUpdate();

        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
    }

     /**
     * Writes the fileheader to the Cms.
     *
     * @param project The project in which the resource will be used.
     * @param onlineProject The online project of the OpenCms.
     * @param file The new file.
     * @param changed Flag indicating if the file state must be set to changed.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
     public void writeFileHeader(CmsProject project, CmsFile file, boolean changed)
         throws CmsException {
         writeFileHeader(project, file, changed, file.getResourceLastModifiedBy());
     }

     /**
     * Writes the fileheader to the Cms.
     *
     * @param project The project in which the resource will be used.
     * @param onlineProject The online project of the OpenCms.
     * @param file The new file.
     * @param changed Flag indicating if the file state must be set to changed.
     * @param userId The id of the user who has changed the resource.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
     public void writeFileHeader(CmsProject project, CmsFile file, boolean changed, int userId)
         throws CmsException {
        ResultSet res = null;
        PreparedStatement statementResourceUpdate = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        int modifiedBy = userId;
        //int onlineProject = getOnlineProject(project.getId()).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject){
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
            modifiedBy = file.getResourceLastModifiedBy();
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            // update resource in the database
            statementResourceUpdate = con.prepareStatement(m_cq.get("C_RESOURCES_UPDATE"+usedStatement));
            statementResourceUpdate.setInt(1,file.getType());
            statementResourceUpdate.setInt(2,file.getFlags());
            statementResourceUpdate.setInt(3,file.getOwnerId());
            statementResourceUpdate.setInt(4,file.getGroupId());
            statementResourceUpdate.setInt(5,file.getProjectId());
            statementResourceUpdate.setInt(6,file.getAccessFlags());
            //STATE
            int state=file.getState();
            if ((state == C_STATE_NEW) || (state == C_STATE_CHANGED)) {
                statementResourceUpdate.setInt(7,state);
            } else {
                if (changed==true) {
                    statementResourceUpdate.setInt(7,C_STATE_CHANGED);
                } else {
                    statementResourceUpdate.setInt(7,file.getState());
                }
            }
            statementResourceUpdate.setInt(8,file.isLockedBy());
            statementResourceUpdate.setInt(9,file.getLauncherType());
            statementResourceUpdate.setString(10,file.getLauncherClassname());
            statementResourceUpdate.setTimestamp(11,new Timestamp(System.currentTimeMillis()));
            statementResourceUpdate.setInt(12,modifiedBy);
            statementResourceUpdate.setInt(13,file.getLength());
            statementResourceUpdate.setInt(14,file.getFileId());
            statementResourceUpdate.setInt(15,file.getResourceId());
            statementResourceUpdate.executeUpdate();
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            // close all db-resources
            if(res != null) {
                try {
                    res.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(statementResourceUpdate != null) {
                try {
                    statementResourceUpdate.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
    }

     /**
     * Writes a folder to the Cms.<BR/>
     *
     * @param project The project in which the resource will be used.
     * @param folder The folder to be written.
     * @param changed Flag indicating if the file state must be set to changed.
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public void writeFolder(CmsProject project, CmsFolder folder, boolean changed)
        throws CmsException {
        writeFolder(project, folder, changed, folder.getResourceLastModifiedBy());
    }

     /**
     * Writes a folder to the Cms.<BR/>
     *
     * @param project The project in which the resource will be used.
     * @param folder The folder to be written.
     * @param changed Flag indicating if the file state must be set to changed.
     * @param userId The user who has changed the resource
     *
     * @exception CmsException Throws CmsException if operation was not succesful.
     */
    public void writeFolder(CmsProject project, CmsFolder folder, boolean changed, int userId)
        throws CmsException {

        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        int modifiedBy = userId;
        //int onlineProject = getOnlineProject(project.getId()).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
            modifiedBy = folder.getResourceLastModifiedBy();
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            // update resource in the database
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_UPDATE"+usedStatement));
            statement.setInt(1,folder.getType());
            statement.setInt(2,folder.getFlags());
            statement.setInt(3,folder.getOwnerId());
            statement.setInt(4,folder.getGroupId());
            statement.setInt(5,folder.getProjectId());
            statement.setInt(6,folder.getAccessFlags());
            int state=folder.getState();
            if ((state == C_STATE_NEW) || (state == C_STATE_CHANGED)) {
                statement.setInt(7,state);
            } else {
                if (changed==true) {
                    statement.setInt(7,C_STATE_CHANGED);
                } else {
                    statement.setInt(7,folder.getState());
                }
            }
            statement.setInt(8,folder.isLockedBy());
            statement.setInt(9,folder.getLauncherType());
            statement.setString(10,folder.getLauncherClassname());
            statement.setTimestamp(11,new Timestamp(System.currentTimeMillis()));
            statement.setInt(12,modifiedBy);
            statement.setInt(13,0);
            statement.setInt(14,C_UNKNOWN_ID);
            statement.setInt(15,folder.getResourceId());
            statement.executeUpdate();
        } catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
        }finally {
            if(statement != null) {
                try {
                    statement.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
            if(con != null) {
                try {
                    con.close();
                } catch(SQLException exc) {
                    // nothing to do here
                }
            }
        }
    }

    /**
     * Writes an already existing group in the Cms.<BR/>
     *
     * Only the admin can do this.<P/>
     *
     * @param group The group that should be written to the Cms.
     * @exception CmsException  Throws CmsException if operation was not succesfull.
     */
     public void writeGroup(CmsGroup group)
         throws CmsException {
         PreparedStatement statement = null;
         Connection con = null;

         try {
            con = DriverManager.getConnection(m_poolName);
            if (group != null){
                // create statement
                statement=con.prepareStatement(m_cq.get("C_GROUPS_WRITEGROUP"));
                statement.setString(1,checkNull(group.getDescription()));
                statement.setInt(2,group.getFlags());
                statement.setInt(3,group.getParentId());
                statement.setInt(4,group.getId());
                statement.executeUpdate();

            } else {
                throw new CmsException("[" + this.getClass().getName() + "] ",CmsException.C_NO_GROUP);
            }
         } catch (SQLException e){
            throw new CmsException("[" + this.getClass().getName() + "] "+e.getMessage(),CmsException.C_SQL_ERROR, e);
         } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
         }
     }

     /**
      * Deletes a project from the cms.
      * Therefore it deletes all files, resources and properties.
      *
      * @param project the project to delete.
      * @exception CmsException Throws CmsException if something goes wrong.
      */
     public void writeProject(CmsProject project)
         throws CmsException {

         PreparedStatement statement = null;
         Connection con = null;

         try {
             con = DriverManager.getConnection(m_poolName);
             // create the statement
             statement = con.prepareStatement(m_cq.get("C_PROJECTS_WRITE"));

             statement.setInt(1,project.getOwnerId());
             statement.setInt(2,project.getGroupId());
             statement.setInt(3,project.getManagerGroupId());
             statement.setInt(4,project.getFlags());
             // no publishing data
             //statement.setTimestamp(5,new Timestamp(project.getPublishingDate()));
             //statement.setInt(6,project.getPublishedBy());
             statement.setInt(7,project.getId());
             statement.executeUpdate();
         } catch( Exception exc ) {
             throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                 CmsException.C_SQL_ERROR, exc);
         } finally {
            // close all db-resources
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
         }
     }

    /**
     * Writes a couple of Properties for a file or folder.
     *
     * @param propertyinfos A Hashtable with propertydefinition- property-pairs as strings.
     * @param resourceId The id of the resource.
     * @param resourceType The Type of the resource.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public void writeProperties(Hashtable propertyinfos, int projectId, CmsResource resource, int resourceType)
        throws CmsException {

        // get all metadefs
        Enumeration keys = propertyinfos.keys();
        // one metainfo-name:
        String key;

        while(keys.hasMoreElements()) {
            key = (String) keys.nextElement();
            writeProperty(key, projectId, (String) propertyinfos.get(key), resource, resourceType);
        }
    }

    /**
     * Writes a property for a file or folder.
     *
     * @param meta The property-name of which the property has to be read.
     * @param value The value for the property to be set.
     * @param resourceId The id of the resource.
     * @param resourceType The Type of the resource.
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
    public void writeProperty(String meta, int projectId, String value, CmsResource resource, int resourceType)
        throws CmsException {
        CmsPropertydefinition propdef = readPropertydefinition(meta, resourceType);
        if( propdef == null) {
            // there is no propertydefinition for with the overgiven name for the resource
            throw new CmsException("[" + this.getClass().getName() + "] " + meta,
                CmsException.C_NOT_FOUND);
        } else {
            String usedPool;
            String usedStatement;
            //int onlineProject = getOnlineProject(projectId).getId();
            int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
            if (projectId == onlineProject){
                usedPool = m_poolNameOnline;
                usedStatement = "_ONLINE";
            } else {
                usedPool = m_poolName;
                usedStatement = "";
            }
            // write the property into the db
            PreparedStatement statement = null;
            Connection con = null;
            boolean newprop=true;
            try {
                con = DriverManager.getConnection(usedPool);
                if( readProperty(propdef.getName(), projectId, resource, resourceType) != null) {
                    // property exists already - use update.
                    // create statement
                    statement = con.prepareStatement(m_cq.get("C_PROPERTIES_UPDATE"+usedStatement));
                    statement.setString(1, checkNull(value) );
                    statement.setInt(2, resource.getResourceId());
                    statement.setInt(3, propdef.getId());
                    statement.executeUpdate();
                    newprop=false;
                } else {
                    // property dosen't exist - use create.
                    // create statement
                    statement = con.prepareStatement(m_cq.get("C_PROPERTIES_CREATE"+usedStatement));
                    statement.setInt(1, nextId(m_cq.get("C_TABLE_PROPERTIES"+usedStatement)));
                    statement.setInt(2, propdef.getId());
                    statement.setInt(3, resource.getResourceId());
                    statement.setString(4, checkNull(value));
                    statement.executeUpdate();
                    newprop=true;
                }
            } catch(SQLException exc) {
                throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                    CmsException.C_SQL_ERROR, exc);
            }finally {
                if(statement != null) {
                     try {
                         statement.close();
                     } catch(SQLException exc) {
                         // nothing to do here
                     }
                }
                if(con != null) {
                     try {
                         con.close();
                     } catch(SQLException exc) {
                         // nothing to do here
                     }
                }
             }
        }
    }

    /**
     * Updates the name of the propertydefinition for the resource type.<BR/>
     *
     * Only the admin can do this.
     *
     * @param metadef The propertydef to be written.
     *
     * @return The propertydefinition, that was written.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsPropertydefinition writePropertydefinition(CmsPropertydefinition metadef)
        throws CmsException {
        PreparedStatement statement = null;
        CmsPropertydefinition returnValue = null;
        Connection con = null;
        try {
            // write the propertydef in the offline db
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_PROPERTYDEF_UPDATE"));
            statement.setString(1, metadef.getName() );
            statement.setInt(2, metadef.getId() );
            statement.executeUpdate();
            statement.close();
            con.close();
            // write the propertydef in the online db
            con = DriverManager.getConnection(m_poolNameOnline);
            statement = con.prepareStatement(m_cq.get("C_PROPERTYDEF_UPDATE_ONLINE"));
            statement.setString(1, metadef.getName() );
            statement.setInt(2, metadef.getId() );
            statement.executeUpdate();
            statement.close();
            con.close();
            // write the propertydef in the backup db
            con = DriverManager.getConnection(m_poolNameBackup);
            statement = con.prepareStatement(m_cq.get("C_PROPERTYDEF_UPDATE_BACKUP"));
            statement.setString(1, metadef.getName() );
            statement.setInt(2, metadef.getId() );
            statement.executeUpdate();
            statement.close();
            con.close();
            // read the propertydefinition
            returnValue = readPropertydefinition(metadef.getName(), metadef.getType());
         } catch( SQLException exc ) {
             throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(),
                CmsException.C_SQL_ERROR, exc);
         } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
          return returnValue;
    }

    /**
     * Writes a serializable object to the systemproperties.
     *
     * @param name The name of the property.
     * @param object The property-object.
     *
     * @return object The property-object.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public Serializable writeSystemProperty(String name, Serializable object)
        throws CmsException {

        byte[] value=null;
        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            // serialize the object
            ByteArrayOutputStream bout= new ByteArrayOutputStream();
            ObjectOutputStream oout=new ObjectOutputStream(bout);
            oout.writeObject(object);
            oout.close();
            value=bout.toByteArray();

            statement=con.prepareStatement(m_cq.get("C_SYSTEMPROPERTIES_UPDATE"));
            statement.setBytes(1,value);
            statement.setString(2,name);
            statement.executeUpdate();
         }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        }
        catch (IOException e){
            throw new CmsException("["+this.getClass().getName()+"]"+CmsException. C_SERIALIZATION, e);
        }finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
          }

          return readSystemProperty(name);
    }


    public void writeSystemTaskLog(int taskid, String comment)
        throws CmsException {
        this.writeTaskLog(taskid, C_UNKNOWN_ID,
                          new java.sql.Timestamp(System.currentTimeMillis()),
                          comment, C_TASKLOG_USER);
    }

    /**
     * Updates a task.
     *
     * @param task The task that will be written.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsTask writeTask(CmsTask task)
        throws CmsException {

        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_cq.get("C_TASK_UPDATE"));
            statement.setString(1,task.getName());
            statement.setInt(2,task.getState());
            statement.setInt(3,task.getTaskType());
            statement.setInt(4,task.getRoot());
            statement.setInt(5,task.getParent());
            statement.setInt(6,task.getInitiatorUser());
            statement.setInt(7,task.getRole());
            statement.setInt(8,task.getAgentUser());
            statement.setInt(9,task.getOriginalUser());
            statement.setTimestamp(10,task.getStartTime());
            statement.setTimestamp(11,task.getWakeupTime());
            statement.setTimestamp(12,task.getTimeOut());
            statement.setTimestamp(13,task.getEndTime());
            statement.setInt(14,task.getPercentage());
            statement.setString(15,task.getPermission());
            statement.setInt(16,task.getPriority());
            statement.setInt(17,task.getEscalationType());
            statement.setString(18,task.getHtmlLink());
            statement.setInt(19,task.getMilestone());
            statement.setInt(20,task.getAutoFinish());
            statement.setInt(21,task.getId());
            statement.executeUpdate();

        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return(readTask(task.getId()));
    }

    /**
     * Writes new log for a task.
     *
     * @param taskid The id of the task.
     * @param user User who added the Log.
     * @param starttime Time when the log is created.
     * @param comment Description for the log.
     * @param type Type of the log. 0 = Sytem log, 1 = User Log
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public void writeTaskLog(int taskId, int userid,
                             java.sql.Timestamp starttime, String comment, int type)
        throws CmsException {

        int newId = C_UNKNOWN_ID;
        PreparedStatement statement = null;
        Connection con = null;
        try{
            con = DriverManager.getConnection(m_poolName);
            newId = nextId(C_TABLE_TASKLOG);
            statement = con.prepareStatement(m_cq.get("C_TASKLOG_WRITE"));
            statement.setInt(1, newId);
            statement.setInt(2, taskId);
            if(userid!=C_UNKNOWN_ID){
                statement.setInt(3, userid);
            }
            else {
                // no user is specified so set to system user
                // is only valid for system task log
                statement.setInt(3, 1);
            }
            statement.setTimestamp(4, starttime);
            statement.setString(5, checkNull(comment));
            statement.setInt(6, type);

            statement.executeUpdate();

        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
    }

    /**
     * Creates a new tasktype set in the database.
     * @return The id of the inserted parameter or 0 if the parameter exists for this task.
     *
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public int writeTaskType(int autofinish, int escalationtyperef, String htmllink, String name, String permission, int priorityref, int roleref)
        throws CmsException {
        ResultSet res = null;
        int result = 0;
        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            // test if the parameter already exists for this task
            statement = con.prepareStatement(m_cq.get("C_TASK_GET_TASKTYPE"));
            statement.setString(1, name);
            res = statement.executeQuery();

            if(res.next()) {
                //Parameter exisits, so make an update
                updateTaskType(res.getInt(m_cq.get("C_PAR_ID")), autofinish, escalationtyperef, htmllink, name, permission, priorityref, roleref);

            }
            else {
                //Parameter is not exisiting, so make an insert
                result = insertTaskType(autofinish, escalationtyperef, htmllink, name, permission, priorityref, roleref);

            }
        } catch( SQLException exc ) {
            throw new CmsException(exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            // close all db-resources
            if(res != null) {
                 try {
                     res.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
        return result;
    }

    /**
     * Writes a user to the database.
     *
     * @param user the user to write
     * @exception thorws CmsException if something goes wrong.
     */
    public void writeUser(CmsUser user)
        throws CmsException {
        byte[] value=null;
        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            // serialize the hashtable
            ByteArrayOutputStream bout= new ByteArrayOutputStream();
            ObjectOutputStream oout=new ObjectOutputStream(bout);
            oout.writeObject(user.getAdditionalInfo());
            oout.close();
            value=bout.toByteArray();

            // write data to database
            statement = con.prepareStatement(m_cq.get("C_USERS_WRITE"));

            statement.setString(1,checkNull(user.getDescription()));
            statement.setString(2,checkNull(user.getFirstname()));
            statement.setString(3,checkNull(user.getLastname()));
            statement.setString(4,checkNull(user.getEmail()));
            statement.setTimestamp(5, new Timestamp(user.getLastlogin()));
            statement.setTimestamp(6, new Timestamp(user.getLastUsed()));
            statement.setInt(7,user.getFlags());
            statement.setBytes(8,value);
            statement.setInt(9, user.getDefaultGroupId());
            statement.setString(10,checkNull(user.getAddress()));
            statement.setString(11,checkNull(user.getSection()));
            statement.setInt(12,user.getType());
            statement.setInt(13,user.getId());
            statement.executeUpdate();
        }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        }
        catch (IOException e){
            throw new CmsException("[CmsAccessUserInfoMySql/addUserInformation(id,object)]:"+CmsException. C_SERIALIZATION, e);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }
    }

    /**
     * Changes the project-id of a resource to the new project
     * for publishing the resource directly
     *
     * @param newProjectId The new project-id
     * @param resourcename The name of the resource to change
     */
    public void changeLockedInProject(int newProjectId, String resourcename) throws CmsException{
        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            // write data to database
            statement = con.prepareStatement(m_cq.get("C_RESOURCES_UPDATE_PROJECTID"));
            statement.setInt(1, newProjectId);
            statement.setString(2, resourcename);
            statement.executeUpdate();
        }
        catch (SQLException e){
            throw new CmsException("["+this.getClass().getName()+"]"+e.getMessage(),CmsException.C_SQL_ERROR, e);
        } finally {
            if(statement != null) {
                 try {
                     statement.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
            if(con != null) {
                 try {
                     con.close();
                 } catch(SQLException exc) {
                     // nothing to do here
                 }
            }
        }

    }

    /**
     * retrieve the correct instance of the queries holder.
     * This method should be overloaded if other query strings should be used.
     */
    protected com.opencms.file.genericSql.CmsQueries getQueries() {
        return new com.opencms.file.genericSql.CmsQueries();
    }
}
