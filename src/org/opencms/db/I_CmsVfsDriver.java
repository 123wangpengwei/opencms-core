/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/I_CmsVfsDriver.java,v $
 * Date   : $Date: 2004/06/07 15:46:56 $
 * Version: $Revision: 1.78 $
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
import org.opencms.file.*;
import org.opencms.file.CmsFile;
import org.opencms.file.CmsFolder;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsPropertydefinition;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsUser;
import org.opencms.file.I_CmsResourceType;
import org.opencms.main.CmsException;
import org.opencms.util.CmsUUID;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

/**
 * Definitions of all required VFS driver methods.<p>
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @author Michael Emmerich (m.emmerich@alkacon.com)
 * @version $Revision: 1.78 $ $Date: 2004/06/07 15:46:56 $
 * @since 5.1
 */
public interface I_CmsVfsDriver {

    /**
     * Creates a new file in the database from a specified CmsFile instance.<p>
     *
     * @param project the project in which the resource will be used.
     * @param file the file to be written to the Cms.
     * @param userId the Id of the user who changed the resourse.
     * @param parentId the parentId of the resource.
     * @param filename the complete new name of the file (including pathinformation).
     * @return file the created file.
     * @throws CmsException f operation was not succesful
     */
    CmsFile createFile(CmsProject project, CmsFile file, CmsUUID userId, CmsUUID parentId, String filename) throws CmsException;

    /**
     * Creates a new file in the database from a list of arguments.<p>
     *
     * @param user the user who wants to create the file
     * @param project the project in which the resource will be used
     * @param filename the complete name of the new file (including pathinformation)
     * @param flags the flags of this resource
     * @param parentFolder the parent folder of the resource
     * @param contents the contents of the new file
     * @param resourceType the resourceType of the new file
     * @param dateReleased the release date of the new file
     * @param dateExpired the expiration date of the new file
     * @return file the created file.
     * @throws CmsException if operation was not successful
     */
    CmsFile createFile(CmsUser user, CmsProject project, String filename, int flags, CmsFolder parentFolder, byte[] contents, I_CmsResourceType resourceType, long dateReleased, long dateExpired) throws CmsException;
    
    /**
     * Creates a CmsFile instance from a JDBC ResultSet.<p>
     * 
     * @param res the JDBC ResultSet
     * @param projectId the project id
     * @return the new CmsFile
     * @throws SQLException in case the result set does not include a requested table attribute
     */
    CmsFile createFile(ResultSet res, int projectId) throws SQLException;
    
    /**
     * Creates a CmsFile instance from a JDBC ResultSet.<p>
     * 
     * @param res the JDBC ResultSet
     * @param projectId the project id
     * @param hasFileContentInResultSet flag to include the file content
     * @return the new CmsFile
     * @throws SQLException in case the result set does not include a requested table attribute
     */
    CmsFile createFile(ResultSet res, int projectId, boolean hasFileContentInResultSet) throws SQLException;

    /**
     * Creates a BLOB in the database for the content of a file.<p>
     * 
     * @param fileId the ID of the new file
     * @param fileContent the content of the new file
     * @param versionId for the content of a backup file you need to insert the versionId of the backup
     * @param projectId the ID of the current project
     * @param writeBackup true if the content should be written to the backup table
     * @throws CmsException if somethong goes wrong
     */
    void createFileContent(CmsUUID fileId, byte[] fileContent, int versionId, int projectId, boolean writeBackup) throws CmsException;

    /**
     * Creates a new folder in the database from a specified CmsFolder instance.<p>
     *
     * @param project the project in which the resource will be used
     * @param folder the folder to be written to the Cms
     * @param parentId the parentId of the resource
     * @return the created folder
     * @throws CmsException if operation was not succesful
     */
    CmsFolder createFolder(CmsProject project, CmsFolder folder, CmsUUID parentId) throws CmsException;

    /**
     * Creates a CmsFolder instance from a JDBC ResultSet.<p>
     * 
     * @param res the JDBC ResultSet
     * @param projectId the ID of the current project
     * @param hasProjectIdInResultSet true if the SQL select query includes the PROJECT_ID table attribute
     * @return CmsFolder the new CmsFolder
     * @throws SQLException in case the result set does not include a requested table attribute
     */
    CmsFolder createFolder(ResultSet res, int projectId, boolean hasProjectIdInResultSet) throws SQLException;

    /**
     * Creates a new property defintion in the databse.<p>
     *
     * Only the admin can do this.
     *
     * @param name the name of the propertydefinitions to overwrite
     * @param projectId the project in which the propertydefinition is created
     * @param resourcetype the resource-type for the propertydefinitions
     * @return the new propertydefinition
     * @throws CmsException if something goes wrong
     */
    CmsPropertydefinition createPropertyDefinition(String name, int projectId, int resourcetype) throws CmsException;

    /**
     * Creates a CmsResource instance from a JDBC ResultSet.<p>
     * 
     * @param res the JDBC ResultSet
     * @param projectId the ID of the current project to adjust the modification date in case the resource is a VFS link
     * @return CmsResource the new CmsResource object
     * @throws SQLException in case the result set does not include a requested table attribute
     */
    CmsResource createResource(ResultSet res, int projectId) throws SQLException;

    /**
     * Creates a new sibling for a specified resource.<p>
     * 
     * @param project the project where to create the link
     * @param resource the link prototype
     * @param userId the id of the user creating the link
     * @param parentId the id of the folder where the link is created
     * @param filename the name of the link
     * @return a valid link resource
     * @throws CmsException if something goes wrong
     */
    CmsResource createSibling(CmsProject project, CmsResource resource, CmsUUID userId, CmsUUID parentId, String filename) throws CmsException;
    
    /**
     * Deletes all property values of a file or folder.<p>
     * 
     * You may specify which whether just structure or resource property values should
     * be deleted, or both of them.<p>
     *
     * @param projectId the id of the project
     * @param resource the resource
     * @param deleteOption determines which property values should be deleted
     * @throws CmsException if operation was not successful
     * @see org.opencms.file.CmsProperty#C_DELETE_OPTION_DELETE_STRUCTURE_AND_RESOURCE_VALUES
     * @see org.opencms.file.CmsProperty#C_DELETE_OPTION_DELETE_STRUCTURE_VALUES
     * @see org.opencms.file.CmsProperty#C_DELETE_OPTION_DELETE_RESOURCE_VALUES
     */     
    void deleteProperties(int projectId, CmsResource resource, int deleteOption) throws CmsException;

    /**
     * Deletes a property defintion.<p>
     *
     * Only the admin can do this.
     *
     * @param metadef the propertydefinitions to be deleted.
     * @throws CmsException if something goes wrong
     */
    void deletePropertyDefinition(CmsPropertydefinition metadef) throws CmsException;

    /**
     * Destroys this driver.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    void destroy() throws Throwable;
    
    /**
     * Returns the SqlManager of this driver.<p>
     * 
     * @return the SqlManager of this driver
     */
    CmsSqlManager getSqlManager();

    /**
     * Creates a new resource from an given CmsResource instance while it is imported.<p>
     *
     * @param project the project in which the resource will be used
     * @param newResource the resource to be written to the Cms
     * @param filecontent the filecontent if the resource is a file
     * @param userId the ID of the current user
     * @param parentId the parentId of the resource
     * @param isFolder true to create a new folder
     * @return resource the created resource
     * @throws CmsException if operation was not succesful
     */
    CmsResource importResource(CmsProject project, CmsUUID parentId, CmsResource newResource, byte[] filecontent, CmsUUID userId, boolean isFolder) throws CmsException;

    /**
     * Initializes the SQL manager for this driver.<p>
     * 
     * To obtain JDBC connections from different pools, further 
     * {online|offline|backup} pool Urls have to be specified
     * 
     * @return the SQL manager for this driver
     * @see org.opencms.db.generic.CmsSqlManager#setPoolUrlOffline(String)
     * @see org.opencms.db.generic.CmsSqlManager#setPoolUrlOnline(String)
     * @see org.opencms.db.generic.CmsSqlManager#setPoolUrlBackup(String)
     */
    org.opencms.db.generic.CmsSqlManager initQueries();

    /**
     * Reads either all child-files or child-folders of a specified parent folder.<p>
     * 
     * @param currentProject the current project
     * @param parentFolder the parent folder
     * @param getFolders if true the child folders of the parent folder are returned in the result set
     * @param getFiles if true the child files of the parent folder are returned in the result set
     * @return a list of all sub folders or sub files
     * @throws CmsException if something goes wrong
     */
    List readChildResources(CmsProject currentProject, CmsFolder parentFolder, boolean getFolders, boolean getFiles) throws CmsException;

    /**
     * Reads a file specified by it's structure ID.<p>
     * 
     * @param projectId the ID of the current project
     * @param includeDeleted true if should be read even if it's state is deleted
     * @param resourceId the id of the file
     * @return CmsFile the file
     * @throws CmsException if something goes wrong
     */
    CmsFile readFile(int projectId, boolean includeDeleted, CmsUUID resourceId) throws CmsException;

    /**
     * Reads a file header specified by it's structure ID.<p>
     *
     * @param projectId the Id of the project
     * @param resourceId the Id of the resource.
     * @param includeDeleted true if already deleted files are included
     * @return file the read file.
     * @throws CmsException if operation was not succesful
     */
    CmsFile readFileHeader(int projectId, CmsUUID resourceId, boolean includeDeleted) throws CmsException;

    /**
     * Reads a file header specified by it's resource name and parent ID.<p>
     *
     * @param projectId the Id of the project in which the resource will be used
     * @param parentId the id of the parent folder
     * @param filename the name of the file
     * @param includeDeleted true if already deleted files are included
     * @return the read file.
     * @throws CmsException if operation was not succesful
     */
    CmsFile readFileHeader(int projectId, CmsUUID parentId, String filename, boolean includeDeleted) throws CmsException;

    /**
     * Reads all files that are either new, changed or deleted.<p>
     *
     * @param projectId a project id for reading online or offline resources
     * @return a list of files
     * @throws CmsException if operation was not succesful
     */
    List readFiles(int projectId) throws CmsException;

    /**
     * Reads all files of a given resource type that are either new, changed or deleted.<p>
     * 
     * @param projectId a project id for reading online or offline resources
     * @param resourcetype the resourcetype of the files
     * @return a vector of files
     * @throws CmsException if operation was not succesful
     */
    Vector readFiles(int projectId, int resourcetype) throws CmsException;

    /**
     * Reads a folder specified by it's structure ID.<p>
     *
     * @param projectId the project in which the resource will be used
     * @param folderId the id of the folder to be read
     * @return the read folder
     * @throws CmsException if operation was not succesful
     */
    CmsFolder readFolder(int projectId, CmsUUID folderId) throws CmsException;

    /**
     * Reads a folder specified by it's resource name and parent ID.<p>
     *
     * @param projectId the project in which the resource will be used
     * @param parentId the id of the parent folder
     * @param foldername the name of the folder to be read
     * @return The read folder.
     * @throws CmsException if operation was not succesful
     */
    CmsFolder readFolder(int projectId, CmsUUID parentId, String foldername) throws CmsException;

    /**
     * Reads all folders that are new, changed or deleted.<p>
     *
     * @param projectId the project in which the folders are
     * @return a Vecor of folders
     * @throws CmsException  if operation was not succesful
     */
    List readFolders(int projectId) throws CmsException;

    /**
     * Reads all folders in a DFS list view.<p>
     * 
     * @param currentProject the current project
     * @param parentResource the parent resource from where the tree is built (should be / usually)
     * @return List a DFS list view of all folders in the VFS
     * @throws CmsException if something goes wrong
     */
    List readFolderTree(CmsProject currentProject, CmsResource parentResource) throws CmsException;

    /**
     * Reads a property definition for the soecified resource type.<p>
     *
     * @param name the name of the propertydefinition to read
     * @param projectId the id of the project
     * @param type the resource type for which the propertydefinition is valid
     * @return the propertydefinition that corresponds to the overgiven arguments - or null if there is no valid propertydefinition.
     * @throws CmsException if something goes wrong
     */
    CmsPropertydefinition readPropertyDefinition(String name, int projectId, int type) throws CmsException;

    /**
     * Reads all property definitions for the specified resource type.<p>
     *
     * @param projectId the id of the project
     * @param resourcetype the resource type to read the propertydefinitions for
     * @return propertydefinitions a Vector with propertydefefinitions for the resource type (The Vector can be empty)
     * @throws CmsException if something goes wrong
     */
    Vector readPropertyDefinitions(int projectId, I_CmsResourceType resourcetype) throws CmsException;
    
    /**
     * Reads a property object from the database specified by it's key name mapped to a resource.<p>
     * 
     * The implementation must return {@link CmsProperty#getNullProperty()} if the property is not found.<p>
     * 
     * @param key the key of the property
     * @param project the current project
     * @param resource the resource where the property is attached to
     * @return a CmsProperty object containing both the structure and resource value of the property
     * @throws CmsException if something goes wrong
     * @see CmsProperty
     */
    CmsProperty readPropertyObject(String key, CmsProject project, CmsResource resource) throws CmsException;
    
    /**
     * Reads all property objects mapped to a specified resource from the database.<p>
     * 
     * The implementation must return an empty list if no properties are found at all.<p>
     * 
     * @param project the current project
     * @param resource the resource where the property is attached to
     * @return a list with CmsProperty objects containing both the structure and resource value of the property
     * @throws CmsException if something goes wrong
     * @see CmsProperty
     */
    List readPropertyObjects(CmsProject project, CmsResource resource) throws CmsException;   

    /**
     * Reads all resources inside a given project and with a given state.<p>
     * 
     * @param currentProject the current project
     * @param state the state to match
     * @param mode flag signaling the read mode. Valid values are C_READMODE_IGNORESTATE,
     * C_READMODE_MATCHSTATE, C_READMODE_UNMATCHSTATE. 
     * @return List with all resources
     * @throws CmsException if operation was not succesful 
     */
    List readResources(int currentProject, int state, int mode) throws CmsException;

    /**
     * Reads all resources with a modification date within a given time range.<p>
     * 
     * @param currentProject the current project
     * @param starttime the begin of the time range
     * @param endtime the end of the time range
     * @return List with all resources
     * @throws CmsException if operation was not succesful 
     */
    List readResources(int currentProject, long starttime, long endtime) throws CmsException;

    /**
     * Reads all resources that have set the specified property.<p>
     *
     * @param projectId the id of the project to test
     * @param propertyDefinition the name of the propertydefinition to check
     * @return Vector with all resources
     * @throws CmsException if operation was not succesful
     */
    Vector readResources(int projectId, String propertyDefinition) throws CmsException;

    /**
     * Reads all resources oa a given resource type that have set the given property to the specified value.<p>
     *
     * @param projectId the id of the project to test
     * @param propertyDefinition the name of the propertydefinition to check
     * @param propertyValue the value of the property for the resource
     * @param resourceType the value of the resourcetype
     * @return vector with all resources
     * @throws CmsException if operation was not succesful
     */
    Vector readResources(int projectId, String propertyDefinition, String propertyValue, int resourceType) throws CmsException;

    /**
     * Reads all siblings that point to the resource record of a specified resource.<p>
     * 
     * @param currentProject the current project
     * @param resource the specified resource
     * @param includeDeleted true if deleted siblings should be included in the result List
     * @return a List with the fileheaders
     * @throws CmsException if something goes wrong
     */
    List readSiblings(CmsProject currentProject, CmsResource resource, boolean includeDeleted) throws CmsException;
    
    /**
     * Removes a file physically in the database.<p>
     * 
     * @param currentProject the current project
     * @param resource the resource
     * @param removeFileContent if true, the content record is also removed; if false, only the structure/resource records are removed
     * @throws CmsException if something goes wrong
     */
    void removeFile(CmsProject currentProject, CmsResource resource, boolean removeFileContent) throws CmsException;

    /**
     * Removes a folder physically in the database.<p>
     *
     * @param currentProject the current project
     * @param folder the folder
     * @throws CmsException if something goes wrong
     */
    void removeFolder(CmsProject currentProject, CmsFolder folder) throws CmsException;
    
    /**
     * Replaces the content and properties of an existing resource.<p>
     * 
     * @param currentUser the current user
     * @param currentProject the current project
     * @param res the new resource
     * @param newResContent the new content
     * @param newResType the resource type
     * @param loaderId the new loader id
     * @throws CmsException if something goes wrong
     */
    void replaceResource(CmsUser currentUser, CmsProject currentProject, CmsResource res, byte[] newResContent, int newResType, int loaderId) throws CmsException;

    /**
     * Validates if the specified content ID in the tables of the specified project {offline|online} exists.<p>
     * 
     * @param projectId the ID of current project
     * @param contentId the content id
     * @return true, if the specified content ID in the tables of the specified project {offline|online} exists
     * @throws CmsException if something goes wrong
     */
    boolean validateContentIdExists(int projectId, CmsUUID contentId) throws CmsException;

    /**
     * Validates if the specified resource ID in the tables of the specified project {offline|online} exists.<p>
     * 
     * @param projectId the project id
     * @param resourceId the resource id to test for
     * @return true if a resource with the given id was found, false otherweise
     * @throws CmsException if something goes wrong
     */
    boolean validateResourceIdExists(int projectId, CmsUUID resourceId) throws CmsException;

    /**
     * Validates if the specified structure ID in the tables of the specified project {offline|online} exists.<p>
     * 
     * @param projectId the ID of current project
     * @param structureId the structure id
     * @return true, if the specified structure ID in the tables of the specified project {offline|online} exists
     * @throws CmsException if something goes wrong
     */
    boolean validateStructureIdExists(int projectId, CmsUUID structureId) throws CmsException;
    
    /**
     * Writes the file content of a specified file ID.<p>
     * 
     * @param projectId the ID of the current project
     * @param writeBackup true if the file content should be written to the backup table
     * @param fileId The ID of the file to update
     * @param fileContent The new content of the file
     * @throws CmsException if something goes wrong
     */
    void writeFileContent(CmsUUID fileId, byte[] fileContent, int projectId, boolean writeBackup) throws CmsException;

    /**
     * Writes the complete file header of an existing file.<p>
     * 
     * Common usages of writeFileHeader are saving the file header
     * information after creating, importing or restoring complete files
     * where all file header attribs. in both the structure and resource 
     * records get written. Thus, using writeFileHeader affects all siblings of
     * a resource! Use {@link #writeResourceState(CmsProject, CmsResource, int)}
     * instead if you just want to update the file state, e.g. of a single sibling.<p>
     * 
     * The file state is set to "changed", unless the current state is "new"
     * or "deleted". The "changed" argument allows to choose whether the structure 
     * or resource state, or none of them, is set to "changed".<p>
     * 
     * The rating of the file state values is as follows:<br>
     * unchanged < changed < new < deleted<p>
     * 
     * Second, the "state" of the resource is the structure state, if the structure state
     * has a higher file state value than the resource state. Otherwise the file state is
     * the resource state.<p>
     * 
     * @param project the current project
     * @param file the file to be updated
     * @param changed determines whether the structure or resource state, or none of them, is set to "changed"
     * @param userId the ID of the current user
     * @throws CmsException if something goes wrong
     * @see org.opencms.db.CmsDriverManager#C_UPDATE_RESOURCE_STATE
     * @see org.opencms.db.CmsDriverManager#C_UPDATE_STRUCTURE_STATE
     * @see org.opencms.db.CmsDriverManager#C_NOTHING_CHANGED
     * @see #writeResourceState(CmsProject, CmsResource, int)
     */
    void writeFileHeader(CmsProject project, CmsFile file, int changed, CmsUUID userId) throws CmsException;
    
    /**
     * Writes the complete file header of an existing folder.<p>
     * 
     * Common usages of writeFolder are saving the file header
     * information after creating, importing or restoring complete folder
     * where all folder header attribs. in both the structure and resource 
     * records get written.
     * 
     * Please refer to the javadoc of {@link #writeFileHeader(CmsProject, CmsFile, int, CmsUUID)} to read
     * how setting resource state values affects the file state.<p>
     * 
     * @param project the current project
     * @param folder the folder to update
     * @param changed determines whether the structure or resource state, or none of them, is set to "changed"
     * @param userId the ID of the current user
     * @throws CmsException if something goes wrong
     * @see org.opencms.db.CmsDriverManager#C_UPDATE_RESOURCE_STATE
     * @see org.opencms.db.CmsDriverManager#C_UPDATE_STRUCTURE_STATE
     * @see org.opencms.db.CmsDriverManager#C_NOTHING_CHANGED
     * @see #writeResourceState(CmsProject, CmsResource, int)
     */
    void writeFolder(CmsProject project, CmsFolder folder, int changed, CmsUUID userId) throws CmsException;

    /**
     * Writes the "last-modified-in-project" ID of a resource.<p>
     * 
     * @param project the resource record is updated with the ID of this project
     * @param projectId the project id to write into the reource
     * @param resource the resource that gets updated
     * @throws CmsException if something goes wrong
     */
    void writeLastModifiedProjectId(CmsProject project, int projectId, CmsResource resource) throws CmsException;

    /**
     * Writes a property object to the database mapped to a specified resource.<p>
     * 
     * @param project the current project
     * @param resource the resource where the property should be attached to
     * @param property a CmsProperty object containing both the structure and resource value of the property
     * @throws CmsException if something goes wrong
     * @see CmsProperty
     */
    void writePropertyObject(CmsProject project, CmsResource resource, CmsProperty property) throws CmsException;
    
    /**
     * Writes a list of property objects to the database mapped to a specified resource.<p>
     * 
     * @param project the current project
     * @param resource the resource where the property should be attached to
     * @param properties a list of CmsProperty objects
     * @throws CmsException if something goes wrong
     * @see CmsProperty
     */
    void writePropertyObjects(CmsProject project, CmsResource resource, List properties) throws CmsException;
    
    /**
     * Writes the complete structure and resource records of a file.<p>
     *
     * @param project the current project
     * @param resource the resource to write
     * @param filecontent the content of the resource
     * @param changed defines which state must be modified
     * @param userId the user who writes the file
     * @throws CmsException if something goes wrong
     */
    void writeResource(CmsProject project, CmsResource resource, byte[] filecontent, int changed, CmsUUID userId) throws CmsException;

    /**
     * Writes the structure and resource records of an existing offline resource into it's online counterpart while it is published.<p>
     * 
     * @param onlineProject the online project
     * @param onlineResource the online resource
     * @param offlineResource the offline resource
     * @param writeFileContent true, if also the content record of the specified offline resource should be written to the online table; false otherwise
     * @throws CmsException if somethong goes wrong
     */
    void writeResource(CmsProject onlineProject, CmsResource onlineResource, CmsResource offlineResource, boolean writeFileContent) throws CmsException;
    
    /**
     * Writes file state in either the structure or resource record, or both of them.<p>
     * 
     * This method allows to change the resource state to any state by setting the
     * desired state value in the specified CmsResource instance.<p>
     * 
     * This method is frequently used while resources are published to set the file state
     * back to "unchanged".<p>
     * 
     * Only file state attribs. get updated here. Use {@link #writeFileHeader(CmsProject, CmsFile, int, CmsUUID)}
     * or {@link #writeFolder(CmsProject, CmsFolder, int, CmsUUID)} instead to write the complete file header.<p>
     * 
     * Please refer to the javadoc of {@link #writeFileHeader(CmsProject, CmsFile, int, CmsUUID)} to read
     * how setting resource state values affects the file state.<p>
     * 
     * @param project the current project
     * @param resource the resource to be updated
     * @param changed determines whether the structure or resource state, or none of them, is set to "changed"
     * @throws CmsException if somethong goes wrong
     * @see org.opencms.db.CmsDriverManager#C_UPDATE_RESOURCE_STATE
     * @see org.opencms.db.CmsDriverManager#C_UPDATE_STRUCTURE_STATE
     * @see org.opencms.db.CmsDriverManager#C_UPDATE_ALL
     */
    void writeResourceState(CmsProject project, CmsResource resource, int changed) throws CmsException;

}