/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/genericSql/Attic/CmsVfsAccess.java,v $
 * Date   : $Date: 2003/05/15 15:18:35 $
 * Version: $Revision: 1.5 $
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
import com.opencms.file.CmsBackupResource;
import com.opencms.file.CmsFile;
import com.opencms.file.CmsFolder;
import com.opencms.file.CmsProject;
import com.opencms.file.CmsPropertydefinition;
import com.opencms.file.CmsResource;
import com.opencms.file.CmsUser;
import com.opencms.file.I_CmsResourceBroker;
import com.opencms.file.I_CmsResourceType;
import com.opencms.flex.util.CmsUUID;
import com.opencms.util.SqlHelper;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import source.org.apache.java.util.Configurations;

/**
 * Generic, database server independent, implementation of the VFS access methods.
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @version $Revision: 1.5 $ $Date: 2003/05/15 15:18:35 $
 */
public class CmsVfsAccess extends Object implements I_CmsConstants, I_CmsLogChannels {

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
    public CmsVfsAccess(Configurations config, I_CmsResourceBroker theResourceBroker) {
        m_SqlQueries = initQueries(config);

        m_ResourceBroker = theResourceBroker;

        ///////////////////////////////////////////////

        // TODO: the following code should be removed when all methods in this
        // class are switched to the new CmsQueries methods
        String brokerName = (String) config.getString(com.opencms.core.I_CmsConstants.C_CONFIGURATION_RESOURCEBROKER);

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
     * Deletes all files in CMS_FILES without fileHeader in CMS_RESOURCES
     */
    protected void clearFilesTable() throws CmsException {
        Connection con = null;
        PreparedStatement statementDelete = null;

        try {
            con = DriverManager.getConnection(m_poolName);

            statementDelete = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_DELETE_LOST_ID"));
            statementDelete.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statementDelete, null);
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
      * @throws CmsException Throws CmsException if operation was not succesful.
      */
    public void copyFile(CmsProject project, CmsProject onlineProject, CmsUUID userId, String source, CmsUUID parentId, String destination) throws CmsException {
        CmsFile file;

        // read sourcefile
        file = readFile(project.getId(), onlineProject.getId(), source);
        // create destination file
        createFile(project, onlineProject, file, userId, parentId, destination);
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
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile createFile(CmsProject project, CmsProject onlineProject, CmsFile file, CmsUUID userId, CmsUUID parentId, String filename) throws CmsException {
        String usedPool = null;
        String usedStatement = null;
        Connection con = null;
        PreparedStatement statement = null;
        // check the resource name
        if (filename.length() > C_MAX_LENGTH_RESOURCE_NAME) {
            throw new CmsException("[" + this.getClass().getName() + "] " + "Resourcename too long(>" + C_MAX_LENGTH_RESOURCE_NAME + ") ", CmsException.C_BAD_NAME);
        }

        int state = 0;
        CmsUUID modifiedByUserId = userId;
        long dateModified = System.currentTimeMillis();

        if (project.equals(onlineProject)) {
            state = file.getState();
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
            modifiedByUserId = file.getResourceLastModifiedBy();
            dateModified = file.getDateLastModified();
        } else {
            state = C_STATE_NEW;
            usedPool = m_poolName;
            usedStatement = "";
        }

        // Test if the file is already there and marked as deleted.
        // If so, delete it.
        // If the file exists already and is not marked as deleted then throw exception
        try {
            readFileHeader(project.getId(), filename, false);
            throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_FILE_EXISTS);
        } catch (CmsException e) {
            // if the file is marked as deleted remove it!
            if (e.getType() == CmsException.C_RESOURCE_DELETED) {
                removeFile(project.getId(), filename);
                state = C_STATE_CHANGED;
            }
            if (e.getType() == CmsException.C_FILE_EXISTS) {
                throw e;
            }
        }

        // first write the file content

        CmsUUID resourceId = new CmsUUID();
        CmsUUID newFileId = new CmsUUID();

        // now write the resource
        try {
            con = DriverManager.getConnection(usedPool);
            // first write the file content
            try {
                createFileContent(newFileId, file.getContents(), 0, usedPool, usedStatement);
            } catch (CmsException se) {
                if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
                    A_OpenCms.log(C_OPENCMS_CRITICAL, "[CmsAccessFileMySql] " + se.getMessage());
                }
            }
            // now write the file header
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_WRITE" + usedStatement));
            statement.setString(1, resourceId.toString());
            statement.setString(2, parentId.toString());
            statement.setString(3, filename);
            statement.setInt(4, file.getType());
            statement.setInt(5, file.getFlags());
            statement.setString(6, file.getOwnerId().toString());
            statement.setString(7, file.getGroupId().toString());
            statement.setInt(8, project.getId());
            statement.setString(9, newFileId.toString());
            statement.setInt(10, file.getAccessFlags());
            statement.setInt(11, state);
            statement.setString(12, file.isLockedBy().toString());
            statement.setInt(13, file.getLauncherType());
            statement.setString(14, file.getLauncherClassname());
            statement.setTimestamp(15, new Timestamp(file.getDateCreated()));
            statement.setTimestamp(16, new Timestamp(dateModified));
            statement.setInt(17, file.getLength());
            statement.setString(18, modifiedByUserId.toString());
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
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
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile createFile(CmsUser user, CmsProject project, CmsProject onlineProject, String filename, int flags, CmsUUID parentId, byte[] contents, I_CmsResourceType resourceType) throws CmsException {

        String usedPool = null;
        String usedStatement = null;
        //check the resource name
        if (filename.length() > C_MAX_LENGTH_RESOURCE_NAME) {
            throw new CmsException("[" + this.getClass().getName() + "] " + "Resourcename too long(>" + C_MAX_LENGTH_RESOURCE_NAME + ") ", CmsException.C_BAD_NAME);
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
            readFileHeader(project.getId(), filename, false);
            throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_FILE_EXISTS);
        } catch (CmsException e) {
            // if the file is maked as deleted remove it!
            if (e.getType() == CmsException.C_RESOURCE_DELETED) {
                removeFile(project.getId(), filename);
                state = C_STATE_CHANGED;
            }
            if (e.getType() == CmsException.C_FILE_EXISTS) {
                throw e;
            }
        }

        CmsUUID resourceId = new CmsUUID();
        CmsUUID fileId = new CmsUUID();

        Connection con = null;
        PreparedStatement statement = null;

        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_WRITE" + usedStatement));
            // write new resource to the database
            statement.setString(1, resourceId.toString());
            statement.setString(2, parentId.toString());
            statement.setString(3, filename);
            statement.setInt(4, resourceType.getResourceType());
            statement.setInt(5, flags);
            statement.setString(6, user.getId().toString());
            statement.setString(7, user.getDefaultGroupId().toString());
            statement.setInt(8, project.getId());
            statement.setString(9, fileId.toString());
            statement.setInt(10, C_ACCESS_DEFAULT_FLAGS);
            statement.setInt(11, state);
            statement.setString(12, CmsUUID.getNullUUID().toString());
            statement.setInt(13, resourceType.getLauncherType());
            statement.setString(14, resourceType.getLauncherClass());
            statement.setTimestamp(15, new Timestamp(System.currentTimeMillis()));
            statement.setTimestamp(16, new Timestamp(System.currentTimeMillis()));
            statement.setInt(17, contents.length);
            statement.setString(18, user.getId().toString());
            statement.executeUpdate();
            statement.close();
            // write the file content
            createFileContent(fileId, contents, 0, usedPool, usedStatement);
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }

        return readFile(project.getId(), onlineProject.getId(), filename);
    }

    /**
     * Creates the content entry for a file
     *
     * @param fileId The ID of the new file
     * @param fileContent The content of the new file
     * @param versionId For the content of a backup file you need to insert the versionId of the backup
     * @param usedPool The name of the databasepool to use
     * @param usedStatement Specifies which tables must be used: offline, online or backup
     *
     */
    public void createFileContent(CmsUUID fileId, byte[] fileContent, int versionId, String usedPool, String usedStatement) throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;

        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_SqlQueries.get("C_FILES_WRITE" + usedStatement));
            statement.setString(1, fileId.toString());

            if (fileContent.length < 2000) {
                statement.setBytes(2, fileContent);
            } else {
                statement.setBinaryStream(2, new ByteArrayInputStream(fileContent), fileContent.length);
            }
            if ("_BACKUP".equals(usedStatement)) {
                statement.setInt(3, versionId);
            }

            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
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
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public CmsFolder createFolder(CmsUser user, CmsProject project, CmsProject onlineProject, CmsFolder folder, CmsUUID parentId, String foldername) throws CmsException {

        if (foldername.length() > C_MAX_LENGTH_RESOURCE_NAME) {
            throw new CmsException("[" + this.getClass().getName() + "] " + "Resourcename too long(>" + C_MAX_LENGTH_RESOURCE_NAME + ") ", CmsException.C_BAD_NAME);
        }

        CmsFolder oldFolder = null;
        int state = 0;
        CmsUUID modifiedByUserId = user.getId();
        long dateModified = System.currentTimeMillis();

        if (project.equals(onlineProject)) {
            state = folder.getState();
            modifiedByUserId = folder.getResourceLastModifiedBy();
            dateModified = folder.getDateLastModified();
        } else {
            state = C_STATE_NEW;
        }

        // Test if the file is already there and marked as deleted.
        // If so, delete it
        // No, dont delete it, throw exception (h.riege, 04.01.01)
        try {
            oldFolder = readFolder(project.getId(), foldername);
            if (oldFolder.getState() == C_STATE_DELETED) {
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

        CmsUUID resourceId = new CmsUUID();

        Connection con = null;
        PreparedStatement statement = null;

        try {
            con = m_SqlQueries.getConnection(project);
            statement = m_SqlQueries.getPreparedStatement(con, project, "C_RESOURCES_WRITE");

            statement.setString(1, resourceId.toString());
            statement.setString(2, parentId.toString());
            statement.setString(3, foldername);
            statement.setInt(4, folder.getType());
            statement.setInt(5, folder.getFlags());
            statement.setString(6, folder.getOwnerId().toString());
            statement.setString(7, folder.getGroupId().toString());
            statement.setInt(8, project.getId());
            statement.setString(9, CmsUUID.getNullUUID().toString());
            statement.setInt(10, folder.getAccessFlags());
            statement.setInt(11, state);
            statement.setString(12, folder.isLockedBy().toString());
            statement.setInt(13, folder.getLauncherType());
            statement.setString(14, folder.getLauncherClassname());
            statement.setTimestamp(15, new Timestamp(folder.getDateCreated()));
            statement.setTimestamp(16, new Timestamp(dateModified));
            statement.setInt(17, 0);
            statement.setString(18, modifiedByUserId.toString());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }

        // if this is the rootfolder or if the parentfolder is the rootfolder
        // try to create the projectresource
        String parentFolderName = "/";
        if (!folder.getResourceName().equals(C_ROOT)) {
            parentFolderName = folder.getResourceName().substring(0, folder.getResourceName().length() - 1);
            parentFolderName = parentFolderName.substring(0, parentFolderName.lastIndexOf("/") + 1);
        }

        if (parentId.isNullUUID() || parentFolderName.equals(C_ROOT)) {
            try {
                String rootFolder = null;
                try {
                    rootFolder = readProjectResource(project.getId(), C_ROOT);
                } catch (CmsException exc) {
                    // NOOP
                }

                if (rootFolder == null) {
                    createProjectResource(project.getId(), foldername);
                }

                //createProjectResource(project.getId(), foldername);
            } catch (CmsException e) {
                if (e.getType() != CmsException.C_FILE_EXISTS) {
                    throw e;
                }
            }
        }

        return readFolder(project.getId(), foldername);
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
      * @throws CmsException Throws CmsException if operation was not succesful.
      */
    public CmsFolder createFolder(CmsUser user, CmsProject project, CmsUUID parentId, CmsUUID fileId, String folderName, int flags) throws CmsException {
        CmsFolder oldFolder = null;
        int state = C_STATE_NEW;

        if (folderName.length() > C_MAX_LENGTH_RESOURCE_NAME) {
            throw new CmsException("[" + this.getClass().getName() + "] " + "Resourcename too long(>" + C_MAX_LENGTH_RESOURCE_NAME + ") ", CmsException.C_BAD_NAME);
        }

       
        // Test if the folder is already there and marked as deleted.
        // If so, delete it
        // No, dont delete it, throw exception (h.riege, 04.01.01)

        try {
            oldFolder = readFolder(project.getId(), folderName);
            if (oldFolder.getState() == C_STATE_DELETED) {
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

        CmsUUID resourceId = new CmsUUID();

        Connection con = null;
        PreparedStatement statement = null;

        try {
            con = m_SqlQueries.getConnection(project);
            statement = m_SqlQueries.getPreparedStatement(con, project, "C_RESOURCES_WRITE");

            statement.setString(1, resourceId.toString());
            statement.setString(2, parentId.toString());
            statement.setString(3, folderName);
            statement.setInt(4, C_TYPE_FOLDER);
            statement.setInt(5, flags);
            statement.setString(6, user.getId().toString());
            statement.setString(7, user.getDefaultGroupId().toString());
            statement.setInt(8, project.getId());
            statement.setString(9, fileId.toString());
            statement.setInt(10, C_ACCESS_DEFAULT_FLAGS);
            statement.setInt(11, state);
            statement.setString(12, CmsUUID.getNullUUID().toString());
            statement.setInt(13, C_UNKNOWN_LAUNCHER_ID);
            statement.setString(14, C_UNKNOWN_LAUNCHER);
            statement.setTimestamp(15, new Timestamp(System.currentTimeMillis()));
            statement.setTimestamp(16, new Timestamp(System.currentTimeMillis()));
            statement.setInt(17, 0);
            statement.setString(18, user.getId().toString());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }

        String parent = new String();
        if (!folderName.equals(C_ROOT)) {
            parent = folderName.substring(0, folderName.length() - 1);
            parent = parent.substring(0, parent.lastIndexOf("/") + 1);
        }

        // if this is the rootfolder or if the parentfolder is the rootfolder
        // try to create the projectresource
        if (parentId.isNullUUID() || parent.equals(C_ROOT)) {
            try {
                String rootFolder = null;

                try {
                    rootFolder = readProjectResource(project.getId(), C_ROOT);
                } catch (CmsException exc) {
                    // NOOP
                }

                if (rootFolder == null) {
                    createProjectResource(project.getId(), folderName);
                }

                //createProjectResource(project.getId(), folderName);
            } catch (CmsException e) {
                if (e.getType() != CmsException.C_FILE_EXISTS) {
                    throw e;
                }
            }
        }

        return readFolder(project.getId(), folderName);
    }

    /**
     * Creates a new projectResource from an given CmsResource object.
     *
     * @param project The project in which the resource will be used.
     * @param resource The resource to be written to the Cms.
     *
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public void createProjectResource(int projectId, String resourceName) throws CmsException {
        // do not create entries for online-project
        PreparedStatement statement = null;
        Connection con = null;

        try {
            readProjectResource(projectId, resourceName);
            throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_FILE_EXISTS);
        } catch (CmsException e) {
            if (e.getType() == CmsException.C_FILE_EXISTS) {
                throw e;
            }
        }

        try {
            con = m_SqlQueries.getConnection();
            statement = m_SqlQueries.getPreparedStatement(con, "C_PROJECTRESOURCES_CREATE");

            // write new resource to the database
            statement.setInt(1, projectId);
            statement.setString(2, resourceName);

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
    }

    /**
     * Creates the propertydefinitions for the resource type.<BR/>
     *
     * Only the admin can do this.
     *
     * @param name The name of the propertydefinitions to overwrite.
     * @param resourcetype The resource-type for the propertydefinitions.
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public CmsPropertydefinition createPropertydefinition(String name, int resourcetype) throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;

        try {

            // create the propertydefinition in the offline db
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_SqlQueries.get("C_PROPERTYDEF_CREATE"));
            statement.setInt(1, nextId(m_SqlQueries.get("C_TABLE_PROPERTYDEF")));
            statement.setString(2, name);
            statement.setInt(3, resourcetype);
            statement.executeUpdate();
            statement.close();
            con.close();

            // create the propertydefinition in the online db
            con = DriverManager.getConnection(m_poolNameOnline);
            statement = con.prepareStatement(m_SqlQueries.get("C_PROPERTYDEF_CREATE_ONLINE"));
            statement.setInt(1, nextId(m_SqlQueries.get("C_TABLE_PROPERTYDEF_ONLINE")));
            statement.setString(2, name);
            statement.setInt(3, resourcetype);
            statement.executeUpdate();
            statement.close();
            con.close();

            // create the propertydefinition in the backup db
            con = DriverManager.getConnection(m_poolNameBackup);
            statement = con.prepareStatement(m_SqlQueries.get("C_PROPERTYDEF_CREATE_BACKUP"));
            statement.setInt(1, nextId(m_SqlQueries.get("C_TABLE_PROPERTYDEF_BACKUP")));
            statement.setString(2, name);
            statement.setInt(3, resourcetype);
            statement.executeUpdate();
            statement.close();
            con.close();

        } catch (SQLException exc) {
            throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
        return (readPropertydefinition(name, resourcetype));
    }

    /**
     * Creates a new resource from an given CmsResource object.
     *
     * @param project The project in which the resource will be used.
     * @param onlineProject The online project of the OpenCms.
     * @param newResource The resource to be written to the Cms.
     * @param filecontent The filecontent if the resource is a file
     * @param userId The ID of the current user.
     * @param parentId The parentId of the resource.
     *
     * @return resource The created resource.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsResource createResource(CmsProject project, CmsProject onlineProject, CmsResource newResource, byte[] filecontent, CmsUUID userId, boolean isFolder) throws CmsException {
        String usedPool = null;
        String usedStatement = null;
        Connection con = null;
        PreparedStatement statement = null;

        // check the resource name
        if (newResource.getResourceName().length() > C_MAX_LENGTH_RESOURCE_NAME) {
            throw new CmsException("[" + this.getClass().getName() + "] " + "Resourcename too long(>" + C_MAX_LENGTH_RESOURCE_NAME + ") ", CmsException.C_BAD_NAME);
        }

        int state = 0;
        CmsUUID modifiedByUserId = userId;
        long dateModified = newResource.isTouched() ? newResource.getDateLastModified() : System.currentTimeMillis();

        if (project.equals(onlineProject)) {
            state = newResource.getState();
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
            modifiedByUserId = newResource.getResourceLastModifiedBy();
            dateModified = newResource.getDateLastModified();
        } else {
            state = C_STATE_NEW;
            usedPool = m_poolName;
            usedStatement = "";
        }

        // Test if the file is already there and marked as deleted.
        // If so, delete it.
        // If the file exists already and is not marked as deleted then throw exception
        try {
            readResource(project, newResource.getResourceName());
            throw new CmsException("[" + this.getClass().getName() + "] ", CmsException.C_FILE_EXISTS);
        } catch (CmsException e) {
            // if the resource is marked as deleted remove it!
            if (e.getType() == CmsException.C_RESOURCE_DELETED) {
                if (isFolder) {
                    removeFolder(project.getId(), (CmsFolder) newResource);
                } else {
                    removeFile(project.getId(), newResource.getResourceName());
                }
                state = C_STATE_CHANGED;
                //throw new CmsException("["+this.getClass().getName()+"] ",CmsException.C_FILE_EXISTS);
            }
            if (e.getType() == CmsException.C_FILE_EXISTS) {
                throw e;
            }
        }

        CmsUUID newFileId = CmsUUID.getNullUUID();
        CmsUUID resourceId = new CmsUUID();

        // now write the resource
        try {
            con = DriverManager.getConnection(usedPool);
            if (!isFolder) {
                // first write the file content
                newFileId = new CmsUUID();
                try {
                    createFileContent(newFileId, filecontent, 0, usedPool, usedStatement);
                } catch (CmsException se) {
                    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
                        A_OpenCms.log(C_OPENCMS_CRITICAL, "[CmsDbAccess] " + se.getMessage());
                    }
                }
            }

            // now write the file header
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_WRITE" + usedStatement));
            statement.setString(1, resourceId.toString());
            statement.setString(2, newResource.getParentId().toString());
            statement.setString(3, newResource.getResourceName());
            statement.setInt(4, newResource.getType());
            statement.setInt(5, newResource.getFlags());
            statement.setString(6, newResource.getOwnerId().toString());
            statement.setString(7, newResource.getGroupId().toString());
            statement.setInt(8, project.getId());
            statement.setString(9, newFileId.toString());
            statement.setInt(10, newResource.getAccessFlags());
            statement.setInt(11, state);
            statement.setString(12, newResource.isLockedBy().toString());
            statement.setInt(13, newResource.getLauncherType());
            statement.setString(14, newResource.getLauncherClassname());
            statement.setTimestamp(15, new Timestamp(newResource.getDateCreated()));
            statement.setTimestamp(16, new Timestamp(dateModified));
            statement.setInt(17, newResource.getLength());
            statement.setString(18, modifiedByUserId.toString());

            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }

        return readResource(project, newResource.getResourceName());
    }

    /**
     * delete all projectResource from an given CmsProject object.
     *
     * @param project The project in which the resource is used.
     *
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public void deleteAllProjectResources(int projectId) throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_SqlQueries.get("C_PROJECTRESOURCES_DELETEALL"));
            // delete all projectResources from the database
            statement.setInt(1, projectId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
    }

    /**
     * Deletes the file.
     *
     * @param project The project in which the resource will be used.
     * @param filename The complete path of the file.
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public void deleteFile(CmsProject project, String filename) throws CmsException {

        Connection con = null;
        PreparedStatement statement = null;
        String usedPool = null;
        String usedStatement = null;
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_REMOVE" + usedStatement));
            // mark the file as deleted
            statement.setInt(1, com.opencms.core.I_CmsConstants.C_STATE_DELETED);
            statement.setString(2, CmsUUID.getNullUUID().toString());
            statement.setString(3, filename);
            //statement.setInt(4,project.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
    }

    /**
     * Deletes the folder.
     *
     * Only empty folders can be deleted yet.
     *
     * @param project The project in which the resource will be used.
     * @param orgFolder The folder that will be deleted.
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public void deleteFolder(CmsProject project, CmsFolder orgFolder) throws CmsException {
        String usedPool = null;
        String usedStatement = null;
        //CmsProject onlineProject = getOnlineProject(project.getId());
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        // the current implementation only deletes empty folders
        // check if the folder has any files in it
        Vector files = getFilesInFolder(project.getId(), orgFolder);
        files = getUndeletedResources(files);
        if (files.size() == 0) {
            // check if the folder has any folders in it
            Vector folders = getSubFolders(project.getId(), orgFolder);
            folders = getUndeletedResources(folders);
            if (folders.size() == 0) {
                //this folder is empty, delete it
                Connection con = null;
                PreparedStatement statement = null;
                try {
                    con = DriverManager.getConnection(usedPool);
                    // mark the folder as deleted
                    statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_REMOVE" + usedStatement));
                    statement.setInt(1, com.opencms.core.I_CmsConstants.C_STATE_DELETED);
                    statement.setInt(2, com.opencms.core.I_CmsConstants.C_UNKNOWN_ID);
                    statement.setString(3, orgFolder.getResourceName());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
                } finally {
                    m_SqlQueries.closeAll(con, statement, null);
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + orgFolder.getAbsolutePath(), CmsException.C_NOT_EMPTY);
            }
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + orgFolder.getAbsolutePath(), CmsException.C_NOT_EMPTY);
        }
    }

    /**
     * delete a projectResource from an given CmsResource object.
     *
     * @param project The project in which the resource is used.
     * @param resource The resource to be deleted from the Cms.
     *
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public void deleteProjectResource(int projectId, String resourceName) throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            statement = con.prepareStatement(m_SqlQueries.get("C_PROJECTRESOURCES_DELETE"));
            // delete resource from the database
            statement.setInt(1, projectId);
            statement.setString(2, resourceName);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
    }

    /**
     * Deletes a specified project
     *
     * @param project The project to be deleted.
     * @throws CmsException  Throws CmsException if operation was not succesful.
     */
    public void deleteProjectResources(CmsProject project) throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = DriverManager.getConnection(m_poolName);
            // delete all project-resources.
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_DELETE_PROJECT"));
            statement.setInt(1, project.getId());
            statement.executeQuery();
            // delete all project-files.
            //clearFilesTable();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
    }

    /**
     * Private helper method to delete a resource.
     *
     * @param id the id of the resource to delete.
     * @throws CmsException  Throws CmsException if operation was not succesful.
     */
    public void deleteResource(CmsResource resource) throws CmsException {
        String usedPool = null;
        String usedStatement = null;

        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (resource.getProjectId() == onlineProject) {
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
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_DELETEBYID" + usedStatement));
            statement.setString(1, resource.getResourceId().toString());
            statement.executeUpdate();
            statement.close();

            // delete the file content
            statement = con.prepareStatement(m_SqlQueries.get("C_FILE_DELETE" + usedStatement));
            statement.setString(1, resource.getFileId().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
    }

    /**
     * Fetch all VFS links pointing to other VFS resources.
     * 
     * @param theProject the resources in this project are updated
     * @param theResourceIDs reference to an ArrayList where the ID's of the fetched links are stored
     * @param theLinkContents reference to an ArrayList where the contents of the fetched links (= VFS resource names of the targets) are stored
     * @param theResourceTypeLinkID the ID of the link resource type
     * @return the count of affected rows
     */
    public int fetchAllVfsLinks(CmsProject theProject, ArrayList theResourceIDs, ArrayList theLinkContents, ArrayList theLinkResources, int theResourceTypeLinkID) throws CmsException {
        String query = "C_SELECT_VFS_LINK_RESOURCES";
        String pool = m_poolName;
        PreparedStatement stmnt = null;
        Connection con = null;
        int rowCount = 0;
        ResultSet result = null;

        // check if we need to use the same query working on the tables of the online project
        if (theProject.getId() == I_CmsConstants.C_PROJECT_ONLINE_ID) {
            pool = m_poolNameOnline;
            query += "_ONLINE";
        }

        try {
            // execute the query
            con = DriverManager.getConnection(pool);
            stmnt = con.prepareStatement(m_SqlQueries.get(query));
            stmnt.setInt(1, theResourceTypeLinkID);
            result = stmnt.executeQuery();

            while (result.next()) {
                theResourceIDs.add((String) result.getString(1));
                theLinkContents.add((String) new String(m_SqlQueries.getBytes(result, m_SqlQueries.get("C_FILE_CONTENT"))));
                theLinkResources.add((String) result.getString(3));
                rowCount++;
            }
        } catch (SQLException e) {
            rowCount = 0;
            throw new CmsException("[" + this.getClass().getName() + ".fetchAllVfsLinks()] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, stmnt, null);
        }

        return rowCount;
    }

    public long fetchDateFromResource(int theProjectId, int theResourceId, long theDefaultDate) throws CmsException {
        String query = "C_SELECT_RESOURCE_DATE_LASTMODIFIED";
        String pool = m_poolName;
        PreparedStatement stmnt = null;
        Connection con = null;
        ResultSet result = null;
        long date_lastModified = theDefaultDate;

        // check if we need to use the same query working on the tables of the online project
        if (theProjectId == I_CmsConstants.C_PROJECT_ONLINE_ID) {
            pool = m_poolNameOnline;
            query += "_ONLINE";
        }

        try {
            // execute the query
            con = DriverManager.getConnection(pool);
            stmnt = con.prepareStatement(m_SqlQueries.get(query));
            stmnt.setInt(1, theResourceId);
            result = stmnt.executeQuery();

            if (result.next()) {
                date_lastModified = SqlHelper.getTimestamp(result, m_SqlQueries.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
                //System.err.println( "date: " + result.getObject(1).toString() );
            } else {
                date_lastModified = theDefaultDate;
            }
        } catch (SQLException e) {
            //System.err.println( "\n[" + this.getClass().getName() + ".fetchDateFromResource()] " + e.toString() );
            date_lastModified = theDefaultDate;
            throw new CmsException("[" + this.getClass().getName() + ".fetchDateFromResource()] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, stmnt, null);
        }

        return date_lastModified;
    }

    /**
     * Fetches the RESOURCE_FLAGS attribute for a given resource name.
     * This method is slighty more efficient that calling readFileHeader().
     * 
     * @param theProject the current project to choose the right SQL query
     * @param theResourceName the name of the resource of which the resource flags are fetched
     * @return the value of the resource flag attribute.
     * @throws CmsException
     */
    public int fetchResourceFlags(CmsProject theProject, String theResourceName) throws CmsException {
        String query = "C_SELECT_RESOURCE_FLAGS";
        String pool = m_poolName;
        PreparedStatement stmnt = null;
        Connection con = null;
        int resourceFlags = 0;
        ResultSet result = null;

        // check if we need to use the same query working on the tables of the online project
        if (theProject.getId() == I_CmsConstants.C_PROJECT_ONLINE_ID) {
            pool = m_poolNameOnline;
            query += "_ONLINE";
        }

        try {
            // execute the query
            con = DriverManager.getConnection(pool);
            stmnt = con.prepareStatement(m_SqlQueries.get(query));
            stmnt.setString(1, theResourceName);
            result = stmnt.executeQuery();

            if (result.next()) {
                resourceFlags = result.getInt(1);
            } else {
                resourceFlags = 0;
            }
        } catch (SQLException e) {
            resourceFlags = 0;
            throw new CmsException("[" + this.getClass().getName() + ".fetchResourceID()] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, stmnt, null);
        }

        return resourceFlags;
    }

    /**
     * Fetch the ID for a given VFS link target.
     * 
     * @param theProject the CmsProject where the resource is fetched
     * @param theResourceName the name of the resource for which we fetch it's ID
     * @param skipResourceTypeID targets of this resource type are ignored
     * @return the ID of the resource, or -1
     */
    public int fetchResourceID(CmsProject theProject, String theResourceName, int skipResourceTypeID) throws CmsException {
        String query = "C_SELECT_RESOURCE_ID";
        String pool = m_poolName;
        PreparedStatement stmnt = null;
        Connection con = null;
        int resourceID = 0;
        ResultSet result = null;

        // check if we need to use the same query working on the tables of the online project
        if (theProject.getId() == I_CmsConstants.C_PROJECT_ONLINE_ID) {
            pool = m_poolNameOnline;
            query += "_ONLINE";
        }

        try {
            // execute the query
            con = DriverManager.getConnection(pool);
            stmnt = con.prepareStatement(m_SqlQueries.get(query));
            stmnt.setString(1, theResourceName);
            result = stmnt.executeQuery();

            if (result.next()) {
                int resourceTypeID = result.getInt(2);

                if (resourceTypeID != skipResourceTypeID) {
                    resourceID = result.getInt(1);
                } else {
                    resourceID = -1;
                }
            } else {
                resourceID = 0;
            }
        } catch (SQLException e) {
            resourceID = 0;
            throw new CmsException("[" + this.getClass().getName() + ".fetchResourceID()] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, stmnt, null);
        }

        return resourceID;
    }

    /**
     * Fetches all VFS links pointing to a given resource ID.
     * 
     * @param theProject the current project
     * @param theResourceID the ID of the resource of which the VFS links are fetched
     * @param theResourceTypeLinkID the resource type ID of VFS links
     * @return an ArrayList with the resource names of the fetched VFS links
     * @throws CmsException
     */
    public ArrayList fetchVfsLinksForResourceID(CmsProject theProject, int theResourceID, int theResourceTypeLinkID) throws CmsException {
        String query = "C_SELECT_VFS_LINKS";
        String pool = m_poolName;
        PreparedStatement stmnt = null;
        Connection con = null;
        ResultSet result = null;
        ArrayList vfsLinks = new ArrayList();

        // check if we need to use the same query working on the tables of the online project
        if (theProject.getId() == I_CmsConstants.C_PROJECT_ONLINE_ID) {
            pool = m_poolNameOnline;
            query += "_ONLINE";
        }

        try {
            // execute the query
            con = DriverManager.getConnection(pool);
            stmnt = con.prepareStatement(m_SqlQueries.get(query));
            stmnt.setInt(1, theResourceID);
            stmnt.setInt(2, theResourceTypeLinkID);
            stmnt.setInt(3, com.opencms.core.I_CmsConstants.C_STATE_DELETED);
            result = stmnt.executeQuery();

            while (result.next()) {
                CmsResource resource = this.readFileHeader(theProject.getId(), result.getString(1), false);

                if (resource != null) {
                    vfsLinks.add(resource);
                }
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + ".fetchVfsLinksForResourceID()] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, stmnt, null);
        }

        return vfsLinks;
    }

    /**
     * Returns a Vector with all file headers of a folder.<BR/>
     *
     * @param parentFolder The folder to be searched.
     *
     * @return subfiles A Vector with all file headers of the folder.
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getFilesInFolder(int projectId, CmsFolder parentFolder) throws CmsException {
        Vector files = new Vector();
        //CmsResource file = null;
        ResultSet res = null;
        Connection con = null;
        PreparedStatement statement = null;
        String usedPool = null;
        String usedStatement = null;
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            //  get all files in folder
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_GET_FILESINFOLDER" + usedStatement));
            statement.setString(1, parentFolder.getResourceId().toString());
            res = statement.executeQuery();
            
            // create new file objects
            while (res.next()) {
                files.addElement(createCmsFileFromResultSet(res, false, false));
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getFilesWithProperty(int projectId, String propertyDefinition, String propertyValue) throws CmsException {
        Vector names = new Vector();
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool = null;
        String usedStatement = null;
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
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_GET_FILES_WITH_PROPERTY" + usedStatement));
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
            m_SqlQueries.closeAll(con, statement, res);
        }
        return names;
    }

    /**
     * Reads the complete folder-tree for this project.<BR>
     *
     * @param project The project in which the folders are.
     * @param rootName The name of the root, e.g. /default/vfs
     *
     * @return A Vecor of folders.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Vector getFolderTree(int projectId, String rootName) throws CmsException {
        Vector folders = new Vector();
        //CmsFolder folder;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool = null;
        String usedStatement = null;
        String add1 = null;
        String add2 = null;
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        add1 = " " + m_SqlQueries.get("C_RESOURCES_GET_FOLDERTREE" + usedStatement + "_ADD1") + rootName;
        add2 = m_SqlQueries.get("C_RESOURCES_GET_FOLDERTREE" + usedStatement + "_ADD2");
        try {
            // read file data from database
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_GET_FOLDERTREE" + usedStatement) + add1 + add2);
            statement.setInt(1, projectId);
            res = statement.executeQuery();
            // create new file
            while (res.next()) {
                folders.addElement(createCmsFolderFromResultSet(res, true));
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception ex) {
            throw new CmsException("[" + this.getClass().getName() + "]", ex);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
        return folders;
    }

    /**
     * Reads all resources (including the folders) residing in a folder<BR>
     *
     * @param onlineResource the parent resource id of the online resoure.
     * @param offlineResource the parent resource id of the offline resoure.
     *
     * @return A Vecor of resources.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Vector getResourcesInFolder(int projectId, CmsFolder offlineResource) throws CmsException {
        Vector resources = new Vector();
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }

        // first get the folders
        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_GET_FOLDERS_IN_FOLDER" + usedStatement));
            statement.setString(1, offlineResource.getResourceId().toString());
            statement.setInt(2, projectId);
            res = statement.executeQuery();

            String lastfolder = null;
            while (res.next()) {
                CmsFolder folder = createCmsFolderFromResultSet(res, true);
                if (!folder.getName().equalsIgnoreCase(lastfolder)) {
                    resources.addElement(folder);
                }
                lastfolder = folder.getName();
            }

        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception ex) {
            throw new CmsException("[" + this.getClass().getName() + "]", ex);
        } finally {
            m_SqlQueries.closeAll(null, statement, res);
        }

        // then get the resources
        try {
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_GET_RESOURCES_IN_FOLDER" + usedStatement));
            statement.setString(1, offlineResource.getResourceId().toString());
            statement.setInt(2, projectId);
            res = statement.executeQuery();

            String lastresource = null;
            while (res.next()) {
                CmsResource resource = createCmsResourceFromResultSet(res, projectId);
                if (!resource.getName().equalsIgnoreCase(lastresource)) {
                    resources.addElement(resource);
                }
                lastresource = resource.getName();
            }

        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception ex) {
            throw new CmsException("[" + this.getClass().getName() + "]", ex);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }

        return resources;
    }

    /**
     * Returns a Vector with all resources of the given type
     * that have set the given property. For the start it is
     * only used by the static export so it reads the online project only.
     *
     * @param projectid, the id of the project to test.
     * @param propertyDefinition, the name of the propertydefinition to check.
     *
     * @return Vector with all resources.
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getResourcesWithProperty(int projectId, String propertyDefinition) throws CmsException {
        Vector resources = new Vector();
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool = null;
        String usedStatement = null;
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
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_GET_RESOURCE_WITH_PROPERTYDEF" + usedStatement));
            statement.setInt(1, projectId);
            statement.setString(2, propertyDefinition);
            res = statement.executeQuery();
            String lastResourcename = "";
            // store the result into the vector
            while (res.next()) {
                CmsResource resource = createCmsResourceFromResultSet(res, projectId);
                if (!resource.getName().equalsIgnoreCase(lastResourcename)) {
                    resources.addElement(resource);
                }
                lastResourcename = resource.getName();
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception exc) {
            throw new CmsException("getResourcesWithProperty" + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
        return resources;
    }

    /**
     * Returns a Vector with all resources of the given type
     * that have set the given property to the given value.
     *
     * @param projectid, the id of the project to test.
     * @param propertyDefinition, the name of the propertydefinition to check.
     * @param propertyValue, the value of the property for the resource.
     * @param resourceType, the value of the resourcetype.
     *
     * @return Vector with all resources.
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getResourcesWithProperty(int projectId, String propertyDefinition, String propertyValue, int resourceType) throws CmsException {
        Vector resources = new Vector();
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool = null;
        String usedStatement = null;
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
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_GET_RESOURCE_WITH_PROPERTY" + usedStatement));
            statement.setInt(1, projectId);
            statement.setString(2, propertyValue);
            statement.setString(3, propertyDefinition);
            statement.setInt(4, resourceType);
            res = statement.executeQuery();
            String lastResourcename = "";
            // store the result into the vector
            while (res.next()) {
                CmsResource resource = createCmsResourceFromResultSet(res, projectId);
                if (!resource.getName().equalsIgnoreCase(lastResourcename)) {
                    resources.addElement(resource);
                }
                lastResourcename = resource.getName();
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception exc) {
            throw new CmsException("getResourcesWithProperty" + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
        return resources;
    }

    /**
     * Returns a Vector with all subfolders.<BR/>
     *
     * @param parentFolder The folder to be searched.
     *
     * @return Vector with all subfolders for the given folder.
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public Vector getSubFolders(int projectId, CmsFolder parentFolder) throws CmsException {
        Vector folders = new Vector();
        CmsFolder folder = null;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedStatement;
        String usedPool;

        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }

        try {
            //  get all subfolders
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_GET_SUBFOLDER" + usedStatement));

            statement.setString(1, parentFolder.getResourceId().toString());
            res = statement.executeQuery();

            // create new folder objects
            while (res.next()) {
                folder = createCmsFolderFromResultSet(res, false);
                folders.addElement(folder);
            }
        } catch (SQLException e) {
            //e.printStackTrace(System.err);
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception exc) {
            //exc.printStackTrace(System.err);
            throw new CmsException("getSubFolders " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }

        return folders;
    }

    /**
     * Gets all resources that are marked as undeleted.
     * @param resources Vector of resources
     * @return Returns all resources that are markes as deleted
     */
    protected Vector getUndeletedResources(Vector resources) {
        Vector undeletedResources = new Vector();

        for (int i = 0; i < resources.size(); i++) {
            CmsResource res = (CmsResource) resources.elementAt(i);
            if (res.getState() != com.opencms.core.I_CmsConstants.C_STATE_DELETED) {
                undeletedResources.addElement(res);
            }
        }

        return undeletedResources;
    }

    public com.opencms.file.genericSql.CmsQueries initQueries(Configurations config) {
        com.opencms.file.genericSql.CmsQueries queries = new com.opencms.file.genericSql.CmsQueries();
        queries.initJdbcPoolUrls(config);

        return queries;
    }

    /**
     * Private method to get the next id for a table.
     * This method is synchronized, to generate unique id's.
     *
     * @param key A key for the table to get the max-id from.
     * @return next-id The next possible id for this table.
     * @deprecated
     */
    protected synchronized int nextId(String key) throws CmsException {
        // TODO: the following code should be removed when all methods in this
        // class are switched to the new CmsSqlQueries stuff
        return com.opencms.dbpool.CmsIdGenerator.nextId(m_poolName, key);
    }

    /**
     * Reads all file headers of a file in the OpenCms.<BR>
     * The reading excludes the filecontent.
     *
     * @param filename The name of the file to be read.
     * @return Vector of file headers read from the Cms.
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Vector readAllFileHeaders(int projectId, String resourceName) throws CmsException {
        CmsFile file = null;
        ResultSet res = null;
        Vector allHeaders = new Vector();
        PreparedStatement statement = null;
        Connection con = null;
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

        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_READ_ALL" + usedStatement));
            // read file header data from database
            statement.setString(1, resourceName);
            res = statement.executeQuery();
            // create new file headers
            while (res.next()) {
                file = createCmsFileFromResultSet(res, true, false);
                allHeaders.addElement(file);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception exc) {
            throw new CmsException("readAllFileHeaders " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Vector readAllFileHeadersForHist(String resourceName) throws CmsException {
        //CmsBackupResource file = null;
        ResultSet res = null;
        Vector allHeaders = new Vector();
        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolNameBackup);
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_READ_ALL_BACKUP"));
            // read file header data from database
            statement.setString(1, resourceName);
            res = statement.executeQuery();
            // create new file headers
            while (res.next()) {
                allHeaders.addElement(createCmsBackupResourceFromResultSet(res));
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception exc) {
            throw new CmsException("readAllFileHeaders " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
        return allHeaders;
    }

    /**
     * Reads all propertydefinitions for the given resource type.
     *
     * @param resourcetype The resource type to read the propertydefinitions for.
     *
     * @return propertydefinitions A Vector with propertydefefinitions for the resource type.
     * The Vector is maybe empty.
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public Vector readAllPropertydefinitions(I_CmsResourceType resourcetype) throws CmsException {
        return (readAllPropertydefinitions(resourcetype.getResourceType()));
    }

    /**
     * Reads all propertydefinitions for the given resource type.
     *
     * @param resourcetype The resource type to read the propertydefinitions for.
     *
     * @return propertydefinitions A Vector with propertydefefinitions for the resource type.
     * The Vector is maybe empty.
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public Vector readAllPropertydefinitions(int resourcetype) throws CmsException {
        Vector metadefs = new Vector();
        ResultSet result = null;
        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);
            // create statement
            statement = con.prepareStatement(m_SqlQueries.get("C_PROPERTYDEF_READALL"));
            statement.setInt(1, resourcetype);
            result = statement.executeQuery();

            while (result.next()) {
                metadefs.addElement(new CmsPropertydefinition(result.getInt(m_SqlQueries.get("C_PROPERTYDEF_ID")), result.getString(m_SqlQueries.get("C_PROPERTYDEF_NAME")), result.getInt(m_SqlQueries.get("C_PROPERTYDEF_RESOURCE_TYPE"))));
            }
        } catch (SQLException exc) {
            throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, result);
        }
        return (metadefs);
    }

    /**
     * select a projectResource from an given project and resourcename
     *
     * @param project The project in which the resource is used.
     * @param resource The resource to be read from the Cms.
     *
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Vector readBackupProjectResources(int versionId) throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;
        ResultSet res = null;
        Vector projectResources = new Vector();
        try {
            con = DriverManager.getConnection(m_poolNameBackup);
            statement = con.prepareStatement(m_SqlQueries.get("C_PROJECTRESOURCES_READ_BACKUP"));
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
            m_SqlQueries.closeAll(con, statement, res);
        }
        return projectResources;
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
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile readFile(int projectId, int onlineProjectId, String filename) throws CmsException {
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
            statement = con.prepareStatement(m_SqlQueries.get("C_FILES_READ" + usedStatement));

            statement.setString(1, filename);
            statement.setInt(2, projectId);
            res = statement.executeQuery();

            if (res.next()) {
                file = createCmsFileFromResultSet(res, projectId, filename);
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
                // check if this resource is marked as deleted
                if (file.getState() == com.opencms.core.I_CmsConstants.C_STATE_DELETED) {
                    throw new CmsException("[" + this.getClass().getName() + "] " + file.getAbsolutePath(), CmsException.C_RESOURCE_DELETED);
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + filename, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch (Exception exc) {
            throw new CmsException("readFile " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
        return file;
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
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile readFile(int projectId, int onlineProjectId, String filename, boolean includeDeleted) throws CmsException {
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
            statement = con.prepareStatement(m_SqlQueries.get("C_FILES_READ" + usedStatement));
            statement.setString(1, filename);
            statement.setInt(2, projectId);
            res = statement.executeQuery();
            if (res.next()) {
                file = createCmsFileFromResultSet(res, projectId, filename);
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
                // check if this resource is marked as deleted
                if (file.getState() == com.opencms.core.I_CmsConstants.C_STATE_DELETED && !includeDeleted) {
                    throw new CmsException("[" + this.getClass().getName() + "] " + file.getAbsolutePath(), CmsException.C_RESOURCE_DELETED);
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + filename, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] readFile " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch (Exception exc) {
            throw new CmsException("readFile " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
        return file;
    }

    /**
     * Private helper method to read the fileContent for publishProject(export).
     *
     * @param fileId the fileId.
     *
     * @throws CmsException  Throws CmsException if operation was not succesful.
     */
    public byte[] readFileContent(int projectId, int fileId) throws CmsException {
        PreparedStatement statement = null;
        ResultSet res = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject) {
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
            statement = con.prepareStatement(m_SqlQueries.get("C_FILE_READ" + usedStatement));
            statement.setInt(1, fileId);
            res = statement.executeQuery();
            if (res.next()) {
                returnValue = res.getBytes(m_SqlQueries.get("C_FILE_CONTENT"));
            } else {
                throw new CmsException("[" + this.getClass().getName() + "]" + fileId, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
        return returnValue;
    }

    /**
     * Reads a file from the history of the Cms.<BR/>
     *
     * @param versionId The versionId of the resource.
     * @param filename The complete name of the file (including pathinformation).
     *
     * @return file The read file.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsBackupResource readFileForHist(int versionId, String filename) throws CmsException {
        CmsBackupResource file = null;
        PreparedStatement statement = null;
        ResultSet res = null;
        Connection con = null;
        try {
            con = DriverManager.getConnection(m_poolNameBackup);
            // if the actual project is the online project read file header and content
            // from the online project
            statement = con.prepareStatement(m_SqlQueries.get("C_FILES_READ_BACKUP"));
            statement.setString(1, filename);
            statement.setInt(2, versionId);
            res = statement.executeQuery();
            if (res.next()) {
                file = createCmsBackupResourceFromResultSet(res);
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + filename, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch (Exception exc) {
            throw new CmsException("readFile " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile readFileHeader(int projectId, CmsResource resource) throws CmsException {

        CmsFile file = null;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
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
        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_READBYID" + usedStatement));
            // read file data from database
            statement.setString(1, resource.getResourceId().toString());
            res = statement.executeQuery();
            // create new file
            if (res.next()) {
                file = createCmsFileFromResultSet(res, true, false);
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
                // check if this resource is marked as deleted
                if (file.getState() == com.opencms.core.I_CmsConstants.C_STATE_DELETED) {
                    throw new CmsException("[" + this.getClass().getName() + "] " + file.getAbsolutePath(), CmsException.C_RESOURCE_DELETED);
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + resource.getResourceId(), CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch (Exception exc) {
            throw new CmsException("readFile " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile readFileHeader(int projectId, CmsUUID resourceId) throws CmsException {

        CmsFile file = null;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
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
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_READBYID" + usedStatement));
            // read file data from database
            statement.setString(1, resourceId.toString());
            res = statement.executeQuery();
            // create new file
            if (res.next()) {
                file = createCmsFileFromResultSet(res, true, false);
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
                // check if this resource is marked as deleted
                if (file.getState() == com.opencms.core.I_CmsConstants.C_STATE_DELETED) {
                    throw new CmsException("[" + this.getClass().getName() + "] " + file.getAbsolutePath(), CmsException.C_RESOURCE_DELETED);
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + resourceId, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch (Exception exc) {
            throw new CmsException("readFile " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile readFileHeader(int projectId, String filename, boolean includeDeleted) throws CmsException {

        CmsFile file = null;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
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
        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_READ" + usedStatement));
            // read file data from database
            statement.setString(1, filename);
            statement.setInt(2, projectId);
            res = statement.executeQuery();
            // create new file
            if (res.next()) {
                file = createCmsFileFromResultSet(res, true, false);
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
                // check if this resource is marked as deleted
                if ((file.getState() == com.opencms.core.I_CmsConstants.C_STATE_DELETED) && !includeDeleted) {
                    throw new CmsException("[" + this.getClass().getName() + "] " + file.getAbsolutePath(), CmsException.C_RESOURCE_DELETED);
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + filename, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch (Exception exc) {
            throw new CmsException("readFile " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsBackupResource readFileHeaderForHist(int versionId, String filename) throws CmsException {

        CmsBackupResource file = null;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        try {
            con = DriverManager.getConnection(m_poolNameBackup);
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_READ_BACKUP"));
            // read file data from database
            statement.setString(1, filename);
            statement.setInt(2, versionId);
            res = statement.executeQuery();
            // create new file
            if (res.next()) {
                file = createCmsBackupResourceFromResultSet(res);
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + filename, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch (Exception exc) {
            throw new CmsException("readFile " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile readFileHeaderInProject(int projectId, String filename) throws CmsException {

        CmsFile file = null;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
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
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_READINPROJECT" + usedStatement));
            // read file data from database
            statement.setString(1, filename);
            statement.setInt(2, projectId);
            res = statement.executeQuery();
            // create new file
            if (res.next()) {
                file = createCmsFileFromResultSet(res, true, false);
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
                // check if this resource is marked as deleted
                if (file.getState() == com.opencms.core.I_CmsConstants.C_STATE_DELETED) {
                    throw new CmsException("[" + this.getClass().getName() + "] " + file.getAbsolutePath(), CmsException.C_RESOURCE_DELETED);
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + filename, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch (Exception exc) {
            throw new CmsException("readFile " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsFile readFileInProject(int projectId, int onlineProjectId, String filename) throws CmsException {

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
            statement = con.prepareStatement(m_SqlQueries.get("C_FILES_READINPROJECT" + usedStatement));
            statement.setString(1, filename);
            statement.setInt(2, projectId);
            res = statement.executeQuery();
            if (res.next()) {
                file = createCmsFileFromResultSet(res, projectId, filename);
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + filename, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (CmsException ex) {
            throw ex;
        } catch (Exception exc) {
            throw new CmsException("readFile " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
     * @throws CmsException Throws CmsException if operation was not succesful
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
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Vector readFiles(int projectId, boolean includeUnchanged, boolean onlyProject) throws CmsException {

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
            if (onlyProject) {
                inProject = " AND CMS_RESOURCES.PROJECT_ID = CMS_PROJECTRESOURCES.PROJECT_ID";
            }
        }
        if (!includeUnchanged) {
            onlyChanged = " AND STATE != " + com.opencms.core.I_CmsConstants.C_STATE_UNCHANGED;
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_READFILESBYPROJECT" + usedStatement) + onlyChanged + inProject);
            statement.setInt(1, projectId);
            res = statement.executeQuery();

            while (res.next()) {
                file = createCmsFileFromResultSet(res, true, true);
                files.addElement(file);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception ex) {
            throw new CmsException("[" + this.getClass().getName() + "]", ex);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }

        return files;
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

        Vector files = new Vector();
        CmsFile file;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
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
            // read file data from database
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_READ_FILESBYTYPE" + usedStatement));
            statement.setInt(1, resourcetype);
            res = statement.executeQuery();
            // create new file
            while (res.next()) {
                file = createCmsFileFromResultSet(res, true, true);
                files.addElement(file);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CmsException("[" + this.getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception ex) {
            throw new CmsException("[" + this.getClass().getName() + "]", ex);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
        return files;
    }

    /**
     * Reads a folder from the Cms.<BR/>
     *
     * @param project The project in which the resource will be used.
     * @param folderid The id of the folder to be read.
     *
     * @return The read folder.
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public CmsFolder readFolder(int projectId, CmsUUID folderId) throws CmsException {

        CmsFolder folder = null;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
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
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_READBYID" + usedStatement));
            statement.setString(1, folderId.toString());
            res = statement.executeQuery();
            // create new resource
            if (res.next()) {
                folder = createCmsFolderFromResultSet(res, true);
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + folderId, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (CmsException exc) {
            // just throw this exception
            throw exc;
        } catch (Exception exc) {
            throw new CmsException("readFolder " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
        return folder;
    }

    /**
     * Reads a folder from the Cms.<BR/>
     *
     * @param project The project in which the resource will be used.
     * @param foldername The name of the folder to be read.
     *
     * @return The read folder.
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public CmsFolder readFolder(int projectId, String foldername) throws CmsException {
        CmsFolder folder = null;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = m_SqlQueries.getConnection(projectId);
            statement = m_SqlQueries.getPreparedStatement(con, projectId, "C_RESOURCES_READ");

            statement.setString(1, foldername);
            statement.setInt(2, projectId);
            res = statement.executeQuery();

            // create new resource
            if (res.next()) {
                folder = createCmsFolderFromResultSet(res, true);
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + foldername, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (CmsException exc) {
            // just throw this exception
            throw exc;
        } catch (Exception exc) {
            throw new CmsException("readFolder " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public CmsFolder readFolderInProject(int projectId, String foldername) throws CmsException {

        CmsFolder folder = null;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
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
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_READINPROJECT" + usedStatement));
            statement.setString(1, foldername);
            statement.setInt(2, projectId);
            res = statement.executeQuery();
            // create new resource
            if (res.next()) {
                folder = createCmsFolderFromResultSet(res, true);
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + foldername, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (CmsException exc) {
            // just throw this exception
            throw exc;
        } catch (Exception exc) {
            throw new CmsException("readFolder " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
     * @throws CmsException Throws CmsException if operation was not succesful
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
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Vector readFolders(int projectId, boolean includeUnchanged, boolean onlyProject) throws CmsException {

        Vector folders = new Vector();
        CmsFolder folder;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        String onlyChanged = new String();
        String inProject = new String();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
            if (onlyProject) {
                inProject = " AND CMS_RESOURCES.PROJECT_ID = CMS_PROJECTRESOURCES.PROJECT_ID";
            }
        }
        if (!includeUnchanged) {
            onlyChanged = " AND CMS_RESOURCES.STATE != " + com.opencms.core.I_CmsConstants.C_STATE_UNCHANGED + " ORDER BY CMS_RESOURCES.RESOURCE_NAME";
        } else {
            onlyChanged = " ORDER BY CMS_RESOURCES.RESOURCE_NAME";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            // read folder data from database
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_READFOLDERSBYPROJECT" + usedStatement) + inProject + onlyChanged);
            statement.setInt(1, projectId);
            res = statement.executeQuery();
            // create new folder
            while (res.next()) {
                folder = createCmsFolderFromResultSet(res, true);
                folders.addElement(folder);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception ex) {
            throw new CmsException("[" + this.getClass().getName() + "]", ex);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
        return folders;
    }

    /**
     * select a projectResource from an given project and resourcename
     *
     * @param project The project in which the resource is used.
     * @param resource The resource to be read from the Cms.
     *
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public String readProjectResource(int projectId, String resourcename) throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;
        ResultSet res = null;
        String resName = null;

        try {
            con = m_SqlQueries.getConnection();
            statement = m_SqlQueries.getPreparedStatement(con, "C_PROJECTRESOURCES_READ");

            // select resource from the database
            statement.setInt(1, projectId);
            statement.setString(2, resourcename);
            res = statement.executeQuery();

            if (res.next()) {
                resName = res.getString("RESOURCE_NAME");
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + resourcename, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
        return resName;
    }

    /**
     * Returns a list of all properties of a file or folder.<p>
     *
     * @param resourceId the id of the resource
     * @param resource the resource to read the properties from
     * @param resourceType the type of the resource
     *
     * @return a Map of Strings representing the properties of the resource
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public HashMap readProperties(int projectId, CmsResource resource, int resourceType) throws CmsException {

        HashMap returnValue = new HashMap();
        ResultSet result = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        CmsUUID resourceId = resource.getResourceId();

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
            // create project
            statement = con.prepareStatement(m_SqlQueries.get("C_PROPERTIES_READALL" + usedStatement));
            statement.setString(1, resourceId.toString());
            statement.setInt(2, resourceType);
            result = statement.executeQuery();
            while (result.next()) {
                returnValue.put(result.getString(m_SqlQueries.get("C_PROPERTYDEF_NAME")), result.getString(m_SqlQueries.get("C_PROPERTY_VALUE")));
            }
        } catch (SQLException exc) {
            throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, result);
        }
        return (returnValue);
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
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public String readProperty(String meta, int projectId, CmsResource resource, int resourceType) throws CmsException {
        ResultSet result = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(projectId).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        String returnValue = null;
        if (projectId == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_SqlQueries.get("C_PROPERTIES_READ" + usedStatement));
            statement.setString(1, resource.getResourceId().toString());
            statement.setString(2, meta);
            statement.setInt(3, resourceType);
            result = statement.executeQuery();

            if (result.next()) {
                returnValue = result.getString(m_SqlQueries.get("C_PROPERTY_VALUE"));
            }
        } catch (SQLException exc) {
            throw new CmsException("[" + this.getClass().getName() + ".readProperty/1] " + exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, result);
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
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public CmsPropertydefinition readPropertydefinition(String name, I_CmsResourceType type) throws CmsException {
        return (readPropertydefinition(name, type.getResourceType()));
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
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public CmsPropertydefinition readPropertydefinition(String name, int type) throws CmsException {
        CmsPropertydefinition propDef = null;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(m_poolName);

            // create statement
            statement = con.prepareStatement(m_SqlQueries.get("C_PROPERTYDEF_READ"));
            statement.setString(1, name);
            statement.setInt(2, type);
            res = statement.executeQuery();

            // if resultset exists - return it
            if (res.next()) {
                propDef = new CmsPropertydefinition(res.getInt(m_SqlQueries.get("C_PROPERTYDEF_ID")), res.getString(m_SqlQueries.get("C_PROPERTYDEF_NAME")), res.getInt(m_SqlQueries.get("C_PROPERTYDEF_RESOURCE_TYPE")));
            } else {
                res.close();
                res = null;
                // not found!
                throw new CmsException("[" + this.getClass().getName() + "] " + name, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException exc) {
            throw new CmsException("[" + this.getClass().getName() + "] " + exc.getMessage(), CmsException.C_SQL_ERROR, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
        return propDef;
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
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public CmsResource readResource(CmsProject project, String filename) throws CmsException {
        CmsResource file = null;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;

        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }

        try {
            con = DriverManager.getConnection(usedPool);
            // read resource data from database
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_READ" + usedStatement));

            statement.setString(1, filename);
            statement.setInt(2, project.getId());
            res = statement.executeQuery();

            // create new resource
            if (res.next()) {
                file = createCmsResourceFromResultSet(res, project.getId());
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
            } else {
                res.close();
                res = null;
                throw new CmsException("[" + this.getClass().getName() + "] " + filename, CmsException.C_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception exc) {
            throw new CmsException("readResource " + exc.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
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
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Vector readResources(CmsProject project) throws CmsException {

        Vector resources = new Vector();
        CmsResource file;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            // read resource data from database
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_READBYPROJECT" + usedStatement));
            statement.setInt(1, project.getId());
            res = statement.executeQuery();
            // create new resource
            while (res.next()) {
                file = createCmsResourceFromResultSet(res, project.getId());
                resources.addElement(file);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception ex) {
            throw new CmsException("[" + this.getClass().getName() + "]", ex);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
        return resources;
    }

    /**
     * Reads all resources that contains the given string in the resourcename
     * and exists in the current project.<BR/>
     * A resource is either a file header or a folder.
     *
     * @param project The project in which the resource will be used.
     * @param resourcename A part of the resourcename
     *
     * @return A Vecor of resources.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public Vector readResourcesLikeName(CmsProject project, String resourcename) throws CmsException {

        Vector resources = new Vector();
        CmsResource file;
        ResultSet res = null;
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(project.getId()).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            // read resource data from database
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_READ_LIKENAME_1" + usedStatement) + resourcename + m_SqlQueries.get("C_RESOURCES_READ_LIKENAME_2" + usedStatement));
            statement.setInt(1, project.getId());
            res = statement.executeQuery();
            // create new resource
            while (res.next()) {
                file = createCmsResourceFromResultSet(res, project.getId());
                resources.addElement(file);
            }
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } catch (Exception ex) {
            throw new CmsException("[" + this.getClass().getName() + "]", ex);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
        }
        return resources;
    }

    /**
     * Deletes a file in the database.
     * This method is used to physically remove a file form the database.
     *
     * @param project The project in which the resource will be used.
     * @param filename The complete path of the file.
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public void removeFile(int projectId, String filename) throws CmsException {

        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        CmsResource resource = readFileHeader(projectId, filename, true);
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
            // delete the file header
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_DELETE" + usedStatement));
            statement.setString(1, filename);
            statement.executeUpdate();
            statement.close();
            // delete the file content
            statement = con.prepareStatement(m_SqlQueries.get("C_FILE_DELETE" + usedStatement));
            statement.setString(1, resource.getFileId().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
    }

    /**
     * Deletes a folder in the database.
     * This method is used to physically remove a folder form the database.
     *
     * @param folder The folder.
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public void removeFolder(int projectId, CmsFolder folder) throws CmsException {

        // the current implementation only deletes empty folders
        // check if the folder has any files in it
        String usedPool;
        String usedStatement;
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (projectId == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        Vector files = getFilesInFolder(projectId, folder);
        files = getUndeletedResources(files);
        if (files.size() == 0) {
            // check if the folder has any folders in it
            Vector folders = getSubFolders(projectId, folder);
            folders = getUndeletedResources(folders);
            if (folders.size() == 0) {
                //this folder is empty, delete it
                Connection con = null;
                PreparedStatement statement = null;
                try {
                    con = DriverManager.getConnection(usedPool);
                    // delete the folder
                    statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_ID_DELETE" + usedStatement));
                    statement.setString(1, folder.getResourceId().toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
                } finally {
                    m_SqlQueries.closeAll(con, statement, null);
                }
            } else {
                throw new CmsException("[" + this.getClass().getName() + "] " + folder.getAbsolutePath(), CmsException.C_NOT_EMPTY);
            }
        } else {
            throw new CmsException("[" + this.getClass().getName() + "] " + folder.getAbsolutePath(), CmsException.C_NOT_EMPTY);
        }
    }

    /**
     * Deletes a folder in the database.
     * This method is used to physically remove a folder form the database.
     * It is internally used by the publish project method.
     *
     * @param project The project in which the resource will be used.
     * @param foldername The complete path of the folder.
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public void removeFolderForPublish(int projectId, String foldername) throws CmsException {

        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
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
            // delete the folder
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_DELETE" + usedStatement));
            statement.setString(1, foldername);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
    }

    /**
     * Removes the temporary files of the given resource
     *
     * @param file The file of which the remporary files should be deleted
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public void removeTemporaryFile(CmsFile file) throws CmsException {
        PreparedStatement statement = null;
        PreparedStatement statementCont = null;
        PreparedStatement statementProp = null;
        Connection con = null;
        ResultSet res = null;

        String tempFilename = file.getRootName() + file.getPath() + com.opencms.core.I_CmsConstants.C_TEMP_PREFIX + file.getName() + "%";
        try {
            con = DriverManager.getConnection(m_poolName);
            // get all temporary files of the resource
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_GETTEMPFILES"));
            statement.setString(1, tempFilename);
            res = statement.executeQuery();
            while (res.next()) {
                int fileId = res.getInt("FILE_ID");
                int resourceId = res.getInt("RESOURCE_ID");
                // delete the properties
                statementProp = con.prepareStatement(m_SqlQueries.get("C_PROPERTIES_DELETEALL"));
                statementProp.setInt(1, resourceId);
                statementProp.executeQuery();
                statementProp.close();

                // delete the file content
                statementCont = con.prepareStatement(m_SqlQueries.get("C_FILE_DELETE"));
                statementCont.setInt(1, fileId);
                statementCont.executeUpdate();
                statementCont.close();
            }
            res.close();
            statement.close();

            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_DELETETEMPFILES"));
            statement.setString(1, tempFilename);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, res);
            m_SqlQueries.closeAll(null, statementProp, null);
            m_SqlQueries.closeAll(null, statementCont, null);
        }
    }

    /**
     * Renames the file to the new name.
     *
     * @param project The prect in which the resource will be used.
     * @param onlineProject The online project of the OpenCms.
     * @param userId The user id
     * @param oldfileID The id of the resource which will be renamed.
     * @param newname The new name of the resource.
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public void renameFile(CmsProject project, CmsProject onlineProject, int userId, int oldfileID, String newname) throws CmsException {

        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        if (project.getId() == onlineProject.getId()) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_RENAMERESOURCE" + usedStatement));

            statement.setString(1, newname);
            statement.setInt(2, userId);
            statement.setInt(3, oldfileID);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
    }

    /**
     * Undeletes the file.
     *
     * @param project The project in which the resource will be used.
     * @param filename The complete path of the file.
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public void undeleteFile(CmsProject project, String filename) throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        //int onlineProject = getOnlineProject(project.getId()).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_REMOVE" + usedStatement));
            // mark the file as deleted
            statement.setInt(1, com.opencms.core.I_CmsConstants.C_STATE_CHANGED);
            statement.setInt(2, com.opencms.core.I_CmsConstants.C_UNKNOWN_ID);
            statement.setString(3, filename);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
    }

    /**
     * Update the resources flag attribute of all resources.
     * 
     * @param theProject the resources in this project are updated
     * @param theValue the new int value of the resource fags attribute
     * @return the count of affected rows
     */
    public int updateAllResourceFlags(CmsProject theProject, int theValue) throws CmsException {
        String query = "C_UPDATE_ALL_RESOURCE_FLAGS";
        String pool = m_poolName;
        PreparedStatement stmnt = null;
        Connection con = null;
        int rowCount = 0;

        // check if we need to use the same query working on the tables of the online project
        if (theProject.getId() == I_CmsConstants.C_PROJECT_ONLINE_ID) {
            pool = m_poolNameOnline;
            query += "_ONLINE";
        }

        try {
            // execute the query
            con = DriverManager.getConnection(pool);
            stmnt = con.prepareStatement(m_SqlQueries.get(query));
            stmnt.setInt(1, theValue);
            rowCount = stmnt.executeUpdate();
        } catch (SQLException e) {
            rowCount = 0;
            throw new CmsException("[" + this.getClass().getName() + ".updateAllResourceFlags()] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, stmnt, null);
        }

        return rowCount;
    }

    /**
     * Update the resource flag attribute for a given resource.
     * 
     * @param theProject the CmsProject where the resource is updated
     * @param theResourceID the ID of the resource which is updated
     * @param theValue the new value of the resource flag attribute
     * @return the count of affected rows (should be 1, unless an error occurred)
     */
    public int updateResourceFlags(CmsProject theProject, int theResourceID, int theValue) throws CmsException {
        String query = "C_UPDATE_RESOURCE_FLAGS";
        String pool = m_poolName;
        PreparedStatement stmnt = null;
        Connection con = null;
        int rowCount = 0;

        // check if we need to use the same query working on the tables of the online project
        if (theProject.getId() == I_CmsConstants.C_PROJECT_ONLINE_ID) {
            pool = m_poolNameOnline;
            query += "_ONLINE";
        }

        try {
            // execute the query
            con = DriverManager.getConnection(pool);
            stmnt = con.prepareStatement(m_SqlQueries.get(query));
            stmnt.setInt(1, theValue);
            stmnt.setInt(2, theResourceID);
            rowCount = stmnt.executeUpdate();
        } catch (SQLException e) {
            rowCount = 0;
            throw new CmsException("[" + this.getClass().getName() + ".updateResourceFlags()] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, stmnt, null);
        }

        return rowCount;
    }

    /**
     * Writes a file to the Cms.<BR/>
     *
     * @param project The project in which the resource will be used.
     * @param onlineProject The online project of the OpenCms.
     * @param file The new file.
     * @param changed Flag indicating if the file state must be set to changed.
     *
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public void writeFile(CmsProject project, CmsProject onlineProject, CmsFile file, boolean changed) throws CmsException {
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
     * @throws CmsException Throws CmsException if operation was not succesful.
     */
    public void writeFile(CmsProject project, CmsProject onlineProject, CmsFile file, boolean changed, CmsUUID userId) throws CmsException {
        String usedPool;
        String usedStatement;
        if (project.getId() == onlineProject.getId()) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        writeFileHeader(project, file, changed, userId);
        writeFileContent(file.getFileId(), file.getContents(), usedPool, usedStatement);
    }

    /**
    * Writes the file content of an existing file
    *
    * @param fileId The ID of the file to update
    * @param fileContent The new content of the file
    * @param usedPool The name of the database pool to use
    * @param usedStatement Specifies which tables must be used: offline, online or backup
    */
    public void writeFileContent(CmsUUID fileId, byte[] fileContent, String usedPool, String usedStatement) throws CmsException {
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = DriverManager.getConnection(usedPool);
            // update the file content in the FILES database.
            statement = con.prepareStatement(m_SqlQueries.get("C_FILES_UPDATE" + usedStatement));
            if (fileContent.length < 2000) {
                statement.setBytes(1, fileContent);
            } else {
                statement.setBinaryStream(1, new ByteArrayInputStream(fileContent), fileContent.length);
            }

            statement.setString(2, fileId.toString());
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
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
    * @throws CmsException Throws CmsException if operation was not succesful.
    */
    public void writeFileHeader(CmsProject project, CmsFile file, boolean changed) throws CmsException {
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
    * @throws CmsException Throws CmsException if operation was not succesful.
    */
    public void writeFileHeader(CmsProject project, CmsFile file, boolean changed, CmsUUID userId) throws CmsException {
        //ResultSet res = null;
        PreparedStatement statementResourceUpdate = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        CmsUUID modifiedByUserId = userId;
        long dateModified = file.isTouched() ? file.getDateLastModified() : System.currentTimeMillis();

        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
            modifiedByUserId = file.getResourceLastModifiedBy();
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }
        try {
            con = DriverManager.getConnection(usedPool);
            // update resource in the database
            statementResourceUpdate = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_UPDATE" + usedStatement));
            statementResourceUpdate.setInt(1, file.getType());
            statementResourceUpdate.setInt(2, file.getFlags());
            statementResourceUpdate.setString(3, file.getOwnerId().toString());
            statementResourceUpdate.setString(4, file.getGroupId().toString());
            statementResourceUpdate.setInt(5, file.getProjectId());
            statementResourceUpdate.setInt(6, file.getAccessFlags());
            //STATE
            int state = file.getState();
            if ((state == com.opencms.core.I_CmsConstants.C_STATE_NEW) || (state == com.opencms.core.I_CmsConstants.C_STATE_CHANGED)) {
                statementResourceUpdate.setInt(7, state);
            } else {
                if (changed == true) {
                    statementResourceUpdate.setInt(7, com.opencms.core.I_CmsConstants.C_STATE_CHANGED);
                } else {
                    statementResourceUpdate.setInt(7, file.getState());
                }
            }
            statementResourceUpdate.setString(8, file.isLockedBy().toString());
            statementResourceUpdate.setInt(9, file.getLauncherType());
            statementResourceUpdate.setString(10, file.getLauncherClassname());
            statementResourceUpdate.setTimestamp(11, new Timestamp(dateModified));
            statementResourceUpdate.setString(12, modifiedByUserId.toString());
            statementResourceUpdate.setInt(13, file.getLength());
            statementResourceUpdate.setString(14, file.getFileId().toString());
            statementResourceUpdate.setString(15, file.getResourceId().toString());
            statementResourceUpdate.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statementResourceUpdate, null);
        }
    }

    /**
    * Writes a folder to the Cms.<BR/>
    *
    * @param project The project in which the resource will be used.
    * @param folder The folder to be written.
    * @param changed Flag indicating if the file state must be set to changed.
    *
    * @throws CmsException Throws CmsException if operation was not succesful.
    */
    public void writeFolder(CmsProject project, CmsFolder folder, boolean changed) throws CmsException {
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
    * @throws CmsException Throws CmsException if operation was not succesful.
    */
    public void writeFolder(CmsProject project, CmsFolder folder, boolean changed, CmsUUID userId) throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        CmsUUID modifiedByUserId = userId;
        long dateModified = folder.isTouched() ? folder.getDateLastModified() : System.currentTimeMillis();

        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
            modifiedByUserId = folder.getResourceLastModifiedBy();
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }

        try {
            con = DriverManager.getConnection(usedPool);
            // update resource in the database
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_UPDATE" + usedStatement));
            statement.setInt(1, folder.getType());
            statement.setInt(2, folder.getFlags());
            statement.setString(3, folder.getOwnerId().toString());
            statement.setString(4, folder.getGroupId().toString());
            statement.setInt(5, folder.getProjectId());
            statement.setInt(6, folder.getAccessFlags());
            int state = folder.getState();
            if ((state == com.opencms.core.I_CmsConstants.C_STATE_NEW) || (state == com.opencms.core.I_CmsConstants.C_STATE_CHANGED)) {
                statement.setInt(7, state);
            } else {
                if (changed == true) {
                    statement.setInt(7, com.opencms.core.I_CmsConstants.C_STATE_CHANGED);
                } else {
                    statement.setInt(7, folder.getState());
                }
            }
            statement.setString(8, folder.isLockedBy().toString());
            statement.setInt(9, folder.getLauncherType());
            statement.setString(10, folder.getLauncherClassname());
            statement.setTimestamp(11, new Timestamp(dateModified));
            statement.setString(12, modifiedByUserId.toString());
            statement.setInt(13, 0);
            statement.setString(14, CmsUUID.getNullUUID().toString());
            statement.setString(15, folder.getResourceId().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }
    }

    /**
     * Writes a couple of Properties for a file or folder.
     *
     * @param propertyinfos A Hashtable with propertydefinition- property-pairs as strings.
     * @param projectId The id of the current project.
     * @param resource The CmsResource object of the resource that gets the properties.
     * @param resourceType The Type of the resource.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public void writeProperties(Map propertyinfos, int projectId, CmsResource resource, int resourceType) throws CmsException {
        this.writeProperties(propertyinfos, projectId, resource, resourceType, false);
    }

    /**
     * Writes a couple of Properties for a file or folder.
     *
     * @param propertyinfos A Hashtable with propertydefinition- property-pairs as strings.
     * @param projectId The id of the current project.
     * @param resource The CmsResource object of the resource that gets the properties.
     * @param resourceType The Type of the resource.
     * @param addDefinition If <code>true</code> then the propertydefinition is added if it not exists
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public void writeProperties(Map propertyinfos, int projectId, CmsResource resource, int resourceType, boolean addDefinition) throws CmsException {

        // get all metadefs
        Iterator keys = propertyinfos.keySet().iterator();
        // one metainfo-name:
        String key;

        while (keys.hasNext()) {
            key = (String) keys.next();
            writeProperty(key, projectId, (String) propertyinfos.get(key), resource, resourceType, addDefinition);
        }
    }

    /**
     * Writes a property for a file or folder.
     *
     * @param meta The property-name of which the property has to be read.
     * @param value The value for the property to be set.
     * @param resourceId The id of the resource.
     * @param resourceType The Type of the resource.
     * @param addDefinition If <code>true</code> then the propertydefinition is added if it not exists
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public void writeProperty(String meta, int projectId, String value, CmsResource resource, int resourceType, boolean addDefinition) throws CmsException {
        CmsPropertydefinition propdef = null;
        try {
            propdef = readPropertydefinition(meta, resourceType);
        } catch (CmsException ex) {
            // do nothing
        }
        if (propdef == null) {
            // there is no propertydefinition for with the overgiven name for the resource
            // add this definition or throw an exception
            if (addDefinition) {
                createPropertydefinition(meta, resourceType);
            } else {
                throw new CmsException("[" + this.getClass().getName() + ".writeProperty/1] " + meta, CmsException.C_NOT_FOUND);
            }
        } else {
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
            // write the property into the db
            PreparedStatement statement = null;
            Connection con = null;
            try {
                con = DriverManager.getConnection(usedPool);
                if (readProperty(propdef.getName(), projectId, resource, resourceType) != null) {
                    // property exists already - use update.
                    // create statement
                    statement = con.prepareStatement(m_SqlQueries.get("C_PROPERTIES_UPDATE" + usedStatement));
                    statement.setString(1, m_SqlQueries.validateNull(value));
                    statement.setString(2, resource.getResourceId().toString());
                    statement.setInt(3, propdef.getId());
                    statement.executeUpdate();
                } else {
                    // property dosen't exist - use create.
                    // create statement
                    statement = con.prepareStatement(m_SqlQueries.get("C_PROPERTIES_CREATE" + usedStatement));
                    statement.setInt(1, nextId(m_SqlQueries.get("C_TABLE_PROPERTIES" + usedStatement)));
                    statement.setInt(2, propdef.getId());
                    statement.setString(3, resource.getResourceId().toString());
                    statement.setString(4, m_SqlQueries.validateNull(value));
                    statement.executeUpdate();
                }
            } catch (SQLException exc) {
                throw new CmsException("[" + this.getClass().getName() + ".writeProperty/2] " + exc.getMessage(), CmsException.C_SQL_ERROR, exc);
            } finally {
                m_SqlQueries.closeAll(con, statement, null);
            }
        }
    }

    /**
    * Writes a folder to the Cms.<BR/>
    *
    * @param project The project in which the resource will be used.
    * @param folder The folder to be written.
    * @param changed Flag indicating if the file state must be set to changed.
    * @param userId The user who has changed the resource
    *
    * @throws CmsException Throws CmsException if operation was not succesful.
    */
    public void writeResource(CmsProject project, CmsResource resource, byte[] filecontent, boolean isChanged, CmsUUID userId) throws CmsException {
        PreparedStatement statement = null;
        Connection con = null;
        String usedPool;
        String usedStatement;
        CmsUUID modifiedByUserId = userId;
        long dateModified = resource.isTouched() ? resource.getDateLastModified() : System.currentTimeMillis();
        boolean isFolder = false;

        if (resource.getType() == C_TYPE_FOLDER) {
            isFolder = true;
        }
        if (filecontent == null) {
            filecontent = new byte[0];
        }
        //int onlineProject = getOnlineProject(project.getId()).getId();
        int onlineProject = I_CmsConstants.C_PROJECT_ONLINE_ID;
        if (project.getId() == onlineProject) {
            usedPool = m_poolNameOnline;
            usedStatement = "_ONLINE";
            modifiedByUserId = resource.getResourceLastModifiedBy();
        } else {
            usedPool = m_poolName;
            usedStatement = "";
        }

        try {
            con = DriverManager.getConnection(usedPool);
            // update resource in the database
            statement = con.prepareStatement(m_SqlQueries.get("C_RESOURCES_UPDATE" + usedStatement));
            statement.setInt(1, resource.getType());
            statement.setInt(2, resource.getFlags());
            statement.setString(3, resource.getOwnerId().toString());
            statement.setString(4, resource.getGroupId().toString());
            statement.setInt(5, resource.getProjectId());
            statement.setInt(6, resource.getAccessFlags());
            int state = resource.getState();
            if ((state == C_STATE_NEW) || (state == C_STATE_CHANGED)) {
                statement.setInt(7, state);
            } else {
                if (isChanged == true) {
                    statement.setInt(7, C_STATE_CHANGED);
                } else {
                    statement.setInt(7, resource.getState());
                }
            }
            statement.setString(8, resource.isLockedBy().toString());
            statement.setInt(9, resource.getLauncherType());
            statement.setString(10, resource.getLauncherClassname());
            statement.setTimestamp(11, new Timestamp(dateModified));
            statement.setString(12, modifiedByUserId.toString());
            statement.setInt(13, filecontent.length);
            statement.setString(14, resource.getFileId().toString());
            statement.setString(15, resource.getResourceId().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + this.getClass().getName() + "] " + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            m_SqlQueries.closeAll(con, statement, null);
        }

        // write the filecontent if this is a file
        if (!isFolder) {
            this.writeFileContent(resource.getFileId(), filecontent, usedPool, usedStatement);
        }
    }

    /**
     * Semi-constructor to create a CmsFile instance from a JDBC result set.
     * 
     * @param res the JDBC ResultSet
     * @param hasProjectIdInResultSet true if the SQL select query includes the PROJECT_ID table attribute
     * @param hasFileContentInResultSet true if the SQL select query includes the FILE_CONTENT attribute
     * @return CmsFile the new CmsFile object
     * @throws SQLException in case the result set does not include a requested table attribute
     * @throws CmsException if the CmsFile object cannot be created by its constructor
     */
    protected final CmsFile createCmsFileFromResultSet(ResultSet res, boolean hasProjectIdInResultSet, boolean hasFileContentInResultSet) throws SQLException, CmsException {
        // this method is final to allow the java compiler to inline this code!
                    
        byte[] content = null;
        int projectId = I_CmsConstants.C_UNKNOWN_ID;

        CmsUUID resId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_RESOURCE_ID")));
        CmsUUID parentId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_PARENT_ID")));
        String resName = res.getString(m_SqlQueries.get("C_RESOURCES_RESOURCE_NAME"));
        int resType = res.getInt(m_SqlQueries.get("C_RESOURCES_RESOURCE_TYPE"));
        int resFlags = res.getInt(m_SqlQueries.get("C_RESOURCES_RESOURCE_FLAGS"));
        CmsUUID userId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_USER_ID")));
        CmsUUID groupId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_GROUP_ID")));
        CmsUUID fileId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_FILE_ID")));
        int accessFlags = res.getInt(m_SqlQueries.get("C_RESOURCES_ACCESS_FLAGS"));
        int state = res.getInt(m_SqlQueries.get("C_RESOURCES_STATE"));
        CmsUUID lockedBy = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_LOCKED_BY")));
        int launcherType = res.getInt(m_SqlQueries.get("C_RESOURCES_LAUNCHER_TYPE"));
        String launcherClass = res.getString(m_SqlQueries.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
        long created = SqlHelper.getTimestamp(res, m_SqlQueries.get("C_RESOURCES_DATE_CREATED")).getTime();
        long modified = SqlHelper.getTimestamp(res, m_SqlQueries.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
        int resSize = res.getInt(m_SqlQueries.get("C_RESOURCES_SIZE"));
        CmsUUID modifiedBy = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_LASTMODIFIED_BY")));
        int lockedInProject = res.getInt("LOCKED_IN_PROJECT");

        if (hasFileContentInResultSet) {
            content = m_SqlQueries.getBytes(res, m_SqlQueries.get("C_RESOURCES_FILE_CONTENT"));
        } else {
            content = new byte[0];
        }

        if (hasProjectIdInResultSet) {
            projectId = res.getInt(m_SqlQueries.get("C_RESOURCES_PROJECT_ID"));
        } else {
            projectId = lockedInProject;
        }

        if (com.opencms.file.genericSql.CmsDbAccess.C_USE_TARGET_DATE && resType == com.opencms.file.genericSql.CmsDbAccess.C_RESTYPE_LINK_ID && resFlags > 0) {
            modified = fetchDateFromResource(projectId, resFlags, modified);
        }

        return new CmsFile(resId, parentId, fileId, resName, resType, resFlags, userId, groupId, projectId, accessFlags, state, lockedBy, launcherType, launcherClass, created, modified, modifiedBy, content, resSize, lockedInProject);
    }

    /**
     * Semi-constructor to create a CmsFile instance from a JDBC result set.
     * 
     * @param res the JDBC ResultSet
     * @param projectId the ID of the current project to adjust the modification date in case the file is a VFS link
     * @param resourceName the name of the selected file
     * @return CmsFile the new CmsFile object
     * @throws SQLException in case the result set does not include a requested table attribute
     * @throws CmsException if the CmsFile object cannot be created by its constructor
     */
    protected final CmsFile createCmsFileFromResultSet(ResultSet res, int projectId, String resourceName) throws SQLException, CmsException {
        // this method is final to allow the java compiler to inline this code!
        
        CmsUUID resId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_RESOURCE_ID")));
        CmsUUID parentId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_PARENT_ID")));
        int resType = res.getInt(m_SqlQueries.get("C_RESOURCES_RESOURCE_TYPE"));
        int resFlags = res.getInt(m_SqlQueries.get("C_RESOURCES_RESOURCE_FLAGS"));
        CmsUUID userId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_USER_ID")));
        CmsUUID groupId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_GROUP_ID")));
        CmsUUID fileId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_FILE_ID")));
        int accessFlags = res.getInt(m_SqlQueries.get("C_RESOURCES_ACCESS_FLAGS"));
        int state = res.getInt(m_SqlQueries.get("C_RESOURCES_STATE"));
        CmsUUID lockedBy = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_LOCKED_BY")));
        int launcherType = res.getInt(m_SqlQueries.get("C_RESOURCES_LAUNCHER_TYPE"));
        String launcherClass = res.getString(m_SqlQueries.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
        long created = SqlHelper.getTimestamp(res, m_SqlQueries.get("C_RESOURCES_DATE_CREATED")).getTime();
        long modified = SqlHelper.getTimestamp(res, m_SqlQueries.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
        int resSize = res.getInt(m_SqlQueries.get("C_RESOURCES_SIZE"));
        CmsUUID modifiedBy = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_LASTMODIFIED_BY")));
        int lockedInProject = res.getInt("LOCKED_IN_PROJECT");
        byte[] content = m_SqlQueries.getBytes(res, m_SqlQueries.get("C_RESOURCES_FILE_CONTENT"));
        int resProjectId = res.getInt(m_SqlQueries.get("C_RESOURCES_PROJECT_ID"));

        if (com.opencms.file.genericSql.CmsDbAccess.C_USE_TARGET_DATE && resType == com.opencms.file.genericSql.CmsDbAccess.C_RESTYPE_LINK_ID && resFlags > 0) {
            modified = fetchDateFromResource(projectId, resFlags, modified);
        }

        return new CmsFile(resId, parentId, fileId, resourceName, resType, resFlags, userId, groupId, resProjectId, accessFlags, state, lockedBy, launcherType, launcherClass, created, modified, modifiedBy, content, resSize, lockedInProject);
    }

    /**
     * Semi-constructor to create a CmsFolder instance from a JDBC result set.
     * 
     * @param res the JDBC ResultSet
     * @param hasProjectIdInResultSet true if the SQL select query includes the PROJECT_ID table attribute
     * @return CmsFolder the new CmsFolder object
     * @throws SQLException in case the result set does not include a requested table attribute
     */
    protected final CmsFolder createCmsFolderFromResultSet(ResultSet res, boolean hasProjectIdInResultSet) throws SQLException {
        // this method is final to allow the java compiler to inline this code!
        int projectId = I_CmsConstants.C_UNKNOWN_ID;

        CmsUUID resId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_RESOURCE_ID")));
        CmsUUID parentId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_PARENT_ID")));
        String resName = res.getString(m_SqlQueries.get("C_RESOURCES_RESOURCE_NAME"));
        int resType = res.getInt(m_SqlQueries.get("C_RESOURCES_RESOURCE_TYPE"));
        int resFlags = res.getInt(m_SqlQueries.get("C_RESOURCES_RESOURCE_FLAGS"));
        CmsUUID userId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_USER_ID")));
        CmsUUID groupId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_GROUP_ID")));
        CmsUUID fileId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_FILE_ID")));
        int accessFlags = res.getInt(m_SqlQueries.get("C_RESOURCES_ACCESS_FLAGS"));
        int state = res.getInt(m_SqlQueries.get("C_RESOURCES_STATE"));
        CmsUUID lockedBy = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_LOCKED_BY")));
        long created = SqlHelper.getTimestamp(res, m_SqlQueries.get("C_RESOURCES_DATE_CREATED")).getTime();
        long modified = SqlHelper.getTimestamp(res, m_SqlQueries.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
        CmsUUID modifiedBy = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_LASTMODIFIED_BY")));
        int lockedInProject = res.getInt("LOCKED_IN_PROJECT");

        if (hasProjectIdInResultSet) {
            projectId = res.getInt(m_SqlQueries.get("C_RESOURCES_PROJECT_ID"));
        } else {
            projectId = lockedInProject;
        }

        // TODO: check if these attribs are unneccessary for a folder, and if so, remove them from the corresponding SQL queries
        //        res.getInt(m_SqlQueries.get("C_RESOURCES_LAUNCHER_TYPE"));
        //        res.getString(m_SqlQueries.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
        //        res.getInt(m_SqlQueries.get("C_RESOURCES_SIZE"));        

        return new CmsFolder(resId, parentId, fileId, resName, resType, resFlags, userId, groupId, projectId, accessFlags, state, lockedBy, created, modified, modifiedBy, lockedInProject);
    }

    /**
     * Semi-constructor to create a CmsResource instance from a JDBC result set.
     * 
     * @param res the JDBC ResultSet
     * @param projectId the ID of the current project to adjust the modification date in case the resource is a VFS link
     * @return CmsResource the new CmsResource object
     * @throws SQLException in case the result set does not include a requested table attribute
     * @throws CmsException if the CmsFile object cannot be created by its constructor
     */
    protected final CmsResource createCmsResourceFromResultSet(ResultSet res, int projectId) throws SQLException, CmsException {
        // this method is final to allow the java compiler to inline this code!

        CmsUUID resId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_RESOURCE_ID")));
        CmsUUID parentId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_PARENT_ID")));
        String resName = res.getString(m_SqlQueries.get("C_RESOURCES_RESOURCE_NAME"));
        int resType = res.getInt(m_SqlQueries.get("C_RESOURCES_RESOURCE_TYPE"));
        int resFlags = res.getInt(m_SqlQueries.get("C_RESOURCES_RESOURCE_FLAGS"));
        CmsUUID userId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_USER_ID")));
        CmsUUID groupId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_GROUP_ID")));
        int projectID = res.getInt(m_SqlQueries.get("C_RESOURCES_PROJECT_ID"));
        CmsUUID fileId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_FILE_ID")));
        int accessFlags = res.getInt(m_SqlQueries.get("C_RESOURCES_ACCESS_FLAGS"));
        int state = res.getInt(m_SqlQueries.get("C_RESOURCES_STATE"));
        CmsUUID lockedBy = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_LOCKED_BY")));
        int launcherType = res.getInt(m_SqlQueries.get("C_RESOURCES_LAUNCHER_TYPE"));
        String launcherClass = res.getString(m_SqlQueries.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
        long created = SqlHelper.getTimestamp(res, m_SqlQueries.get("C_RESOURCES_DATE_CREATED")).getTime();
        long modified = SqlHelper.getTimestamp(res, m_SqlQueries.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
        int resSize = res.getInt(m_SqlQueries.get("C_RESOURCES_SIZE"));
        CmsUUID modifiedBy = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_LASTMODIFIED_BY")));
        int lockedInProject = res.getInt("LOCKED_IN_PROJECT");

        if (com.opencms.file.genericSql.CmsDbAccess.C_USE_TARGET_DATE && resType == com.opencms.file.genericSql.CmsDbAccess.C_RESTYPE_LINK_ID && resFlags > 0) {
            modified = fetchDateFromResource(projectId, resFlags, modified);
        }

        return new CmsResource(resId, parentId, fileId, resName, resType, resFlags, userId, groupId, projectID, accessFlags, state, lockedBy, launcherType, launcherClass, created, modified, modifiedBy, resSize, lockedInProject);
    }

    /**
     * Semi-constructor to create a CmsBackupResource instance from a JDBC result set.
     * 
     * @param res the JDBC ResultSet
     * @return CmsBackupResource the new CmsBackupResource object
     * @throws SQLException in case the result set does not include a requested table attribute
     */
    protected final CmsBackupResource createCmsBackupResourceFromResultSet(ResultSet res) throws SQLException {
        // this method is final to allow the java compiler to inline this code!
        
        int versionId = res.getInt(m_SqlQueries.get("C_RESOURCES_VERSION_ID"));
        CmsUUID resId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_RESOURCE_ID")));
        CmsUUID parentId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_PARENT_ID")));
        String resName = res.getString(m_SqlQueries.get("C_RESOURCES_RESOURCE_NAME"));
        int resType = res.getInt(m_SqlQueries.get("C_RESOURCES_RESOURCE_TYPE"));
        int resFlags = res.getInt(m_SqlQueries.get("C_RESOURCES_RESOURCE_FLAGS"));
        CmsUUID userId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_USER_ID")));
        String userName = res.getString(m_SqlQueries.get("C_RESOURCES_USER_NAME"));
        CmsUUID groupId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_GROUP_ID")));
        String groupName = res.getString(m_SqlQueries.get("C_RESOURCES_GROUP_NAME"));
        int projectID = res.getInt(m_SqlQueries.get("C_RESOURCES_PROJECT_ID"));
        CmsUUID fileId = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_FILE_ID")));
        int accessFlags = res.getInt(m_SqlQueries.get("C_RESOURCES_ACCESS_FLAGS"));
        int state = res.getInt(m_SqlQueries.get("C_RESOURCES_STATE"));
        int launcherType = res.getInt(m_SqlQueries.get("C_RESOURCES_LAUNCHER_TYPE"));
        String launcherClass = res.getString(m_SqlQueries.get("C_RESOURCES_LAUNCHER_CLASSNAME"));
        long created = SqlHelper.getTimestamp(res, m_SqlQueries.get("C_RESOURCES_DATE_CREATED")).getTime();
        long modified = SqlHelper.getTimestamp(res, m_SqlQueries.get("C_RESOURCES_DATE_LASTMODIFIED")).getTime();
        int resSize = res.getInt(m_SqlQueries.get("C_RESOURCES_SIZE"));
        CmsUUID modifiedBy = new CmsUUID(res.getString(m_SqlQueries.get("C_RESOURCES_LASTMODIFIED_BY")));
        String modifiedByName = res.getString(m_SqlQueries.get("C_RESOURCES_LASTMODIFIED_BY_NAME"));
        int lockedInProject = res.getInt("LOCKED_IN_PROJECT");

        return new CmsBackupResource(versionId, resId, parentId, fileId, resName, resType, resFlags, userId, userName, groupId, groupName, projectID, accessFlags, state, launcherType, launcherClass, created, modified, modifiedBy, modifiedByName, new byte[0], resSize, lockedInProject);
    }
}
