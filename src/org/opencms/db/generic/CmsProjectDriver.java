/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/generic/CmsProjectDriver.java,v $
 * Date   : $Date: 2003/09/12 17:38:06 $
 * Version: $Revision: 1.91 $
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
import org.opencms.db.CmsExportPointDriver;
import org.opencms.db.I_CmsDriver;
import org.opencms.db.I_CmsProjectDriver;
import org.opencms.lock.CmsLock;
import org.opencms.main.OpenCms;
import org.opencms.report.I_CmsReport;
import org.opencms.workflow.CmsTask;
import org.opencms.workflow.CmsTaskLog;

import com.opencms.boot.I_CmsLogChannels;
import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.file.CmsFile;
import com.opencms.file.CmsFolder;
import com.opencms.file.CmsGroup;
import com.opencms.file.CmsObject;
import com.opencms.file.CmsProject;
import com.opencms.file.CmsRequestContext;
import com.opencms.file.CmsResource;
import com.opencms.file.CmsUser;
import com.opencms.flex.CmsEvent;
import com.opencms.flex.I_CmsEventListener;
import com.opencms.flex.util.CmsUUID;
import com.opencms.linkmanagement.CmsPageLinks;
import com.opencms.util.SqlHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import source.org.apache.java.util.Configurations;

/**
 * Generic (ANSI-SQL) implementation of the project driver methods.<p>
 *
 * @version $Revision: 1.91 $ $Date: 2003/09/12 17:38:06 $
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 * @since 5.1
 */
public class CmsProjectDriver extends Object implements I_CmsDriver, I_CmsProjectDriver {

    /**
     * Constant to get property from configurations.
     */
    protected static String C_CONFIGURATIONS_DIGEST = "digest";

    /**
     * Constant to get property from configurations.
     */
    protected static String C_CONFIGURATIONS_DIGEST_FILE_ENCODING = "digest.fileencoding";

    /**
     * Constant to get property from configurations.
     */
    protected static String C_CONFIGURATIONS_POOL = "pool";

    /**
     * The maximum amount of tables.
     */
    protected static int C_MAX_TABLES = 18;

    public static int C_RESTYPE_LINK_ID = 2;

    /**
     * The session-timeout value:
     * currently six hours. After that time the session can't be restored.
     */
    public static long C_SESSION_TIMEOUT = 6 * 60 * 60 * 1000;

    /**
     * Table-key for max-id
     */
    protected static String C_TABLE_PROJECTS = "CMS_PROJECTS";

    /**
     * Table-key for max-id
     */
    protected static String C_TABLE_PROPERTIES = "CMS_PROPERTIES";

    /**
     * Table-key for max-id
     */
    protected static String C_TABLE_PROPERTYDEF = "CMS_PROPERTYDEF";

    /**
     * Table-key for max-id
     */
    protected static String C_TABLE_SYSTEMPROPERTIES = "CMS_SYSTEMPROPERTIES";
    public static boolean C_USE_TARGET_DATE = true;
    protected CmsDriverManager m_driverManager;

    /**
     * A array containing all max-ids for the tables.
     */
    protected int[] m_maxIds;

    protected org.opencms.db.generic.CmsSqlManager m_sqlManager;
    
    /** Internal debugging flag.<p> */
    private static final boolean C_DEBUG = true;

    /**
     * Creates a serializable object in the systempropertys.
     *
     * @param name The name of the property.
     * @param object The property-object.
     *
     * @return object The property-object.
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public Serializable addSystemProperty(String name, Serializable object) throws CmsException {

        byte[] value;
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            // serialize the object
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(bout);
            oout.writeObject(object);
            oout.close();
            value = bout.toByteArray();

            // create the object
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_SYSTEMPROPERTIES_WRITE");
            stmt.setInt(1, m_sqlManager.nextId(C_TABLE_SYSTEMPROPERTIES));
            stmt.setString(2, name);
            m_sqlManager.setBytes(stmt, 3, value);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (IOException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SERIALIZATION, e, false);
        } finally {
            // close all db-resources
            m_sqlManager.closeAll(conn, stmt, null);
        }
        return readSystemProperty(name);
    }

    /**
     * Private helper method for publihing into the filesystem.
     * test if resource must be written to the filesystem.<p>
     *
     * @param filename Name of a resource in the OpenCms system
     * @param exportpoints the exportpoints
     * @return key in exportpoints or null
     */
    protected String checkExport(String filename, Hashtable exportpoints) {

        String key = null;
        String exportpoint = null;
        Enumeration e = exportpoints.keys();

        while (e.hasMoreElements()) {
            exportpoint = (String) e.nextElement();
            if (filename.startsWith(exportpoint)) {
                return exportpoint;
            }
        }
        return key;
    }

    /**
     * creates a link entry for each of the link targets in the linktable.<p>
     *
     * @param pageId The resourceId (offline) of the page whose liks should be traced
     * @param linkTargets A vector of strings (the linkdestinations)
     * @throws CmsException if something goes wrong  
     */
    public void createLinkEntrys(CmsUUID pageId, Vector linkTargets) throws CmsException {
        //first delete old entrys in the database
        deleteLinkEntrys(pageId);
        if (linkTargets == null || linkTargets.size() == 0) {
            return;
        }
        // now write it
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            // TODO all the link management methods should be carefully turned into project dependent code!
            int dummyProjectId = Integer.MAX_VALUE;
            conn = m_sqlManager.getConnection(dummyProjectId);
            stmt = m_sqlManager.getPreparedStatement(conn, dummyProjectId, "C_LM_WRITE_ENTRY");
            stmt.setString(1, pageId.toString());
            for (int i = 0; i < linkTargets.size(); i++) {
                try {
                    stmt.setString(2, (String) linkTargets.elementAt(i));
                    stmt.executeUpdate();
                } catch (SQLException e) {
                }
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, "createLinkEntrys(CmsUUID, Vector)", CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * creates a link entry for each of the link targets in the online linktable.<p>
     *
     * @param pageId The resourceId (online) of the page whose liks should be traced
     * @param linkTargets A vector of strings (the linkdestinations)
     * @throws CmsException if something goes wrong
     */
    public void createOnlineLinkEntrys(CmsUUID pageId, Vector linkTargets) throws CmsException {
        //first delete old entrys in the database
        deleteLinkEntrys(pageId);
        if (linkTargets == null || linkTargets.size() == 0) {
            return;
        }
        // now write it
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            // TODO all the link management methods should be carefully turned into project dependent code!          
            int dummyProjectId = I_CmsConstants.C_PROJECT_ONLINE_ID;
            conn = m_sqlManager.getConnection(dummyProjectId);
            stmt = m_sqlManager.getPreparedStatement(conn, dummyProjectId, "C_LM_WRITE_ENTRY");
            stmt.setString(1, pageId.toString());
            for (int i = 0; i < linkTargets.size(); i++) {
                try {
                    stmt.setString(2, (String) linkTargets.elementAt(i));
                    stmt.executeUpdate();
                } catch (SQLException e) {
                }
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, "createOnlineLinkEntrys(CmsUUID, Vector)", CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
    * Creates a project.<p>
    * TODO: name x timestamp must be unique - since timestamp typically has a resulution
    * of one second, creation of several tasks with the same name may fail.
    *
    * @param owner The owner of this project
    * @param group The group for this project
    * @param managergroup The managergroup for this project
    * @param task The task
    * @param name The name of the project to create
    * @param description The description for the new project
    * @param flags The flags for the project (e.g. archive)
    * @param type the type for the project (e.g. normal)
    * @return the new CmsProject instance
    * @throws CmsException Throws CmsException if something goes wrong
    */
    public CmsProject createProject(CmsUser owner, CmsGroup group, CmsGroup managergroup, CmsTask task, String name, String description, int flags, int type) throws CmsException {

        if ((description == null) || (description.length() < 1)) {
            description = " ";
        }

        Timestamp createTime = new Timestamp(new java.util.Date().getTime());
        Connection conn = null;
        PreparedStatement stmt = null;

        int id = m_sqlManager.nextId(C_TABLE_PROJECTS);

        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_PROJECTS_CREATE");
            // write data to database
            stmt.setInt(1, id);
            stmt.setString(2, owner.getId().toString());
            stmt.setString(3, group.getId().toString());
            stmt.setString(4, managergroup.getId().toString());
            stmt.setInt(5, task.getId());
            stmt.setString(6, name);
            stmt.setString(7, description);
            stmt.setInt(8, flags);
            stmt.setTimestamp(9, createTime);
            // no publish data
            stmt.setInt(10, type);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }

        return readProject(id);
    }

    /**
     * This method creates a new session in the database. It is used
     * for sessionfailover.<p>
     *
     * @param sessionId the id of the session
     * @param data the session data
     * @throws CmsException if something goes wrong
     */
    public void createSession(String sessionId, Hashtable data) throws CmsException {
        byte[] value = null;

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            value = serializeSession(data);
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_SESSION_CREATE");
            // write data to database
            stmt.setString(1, sessionId);
            stmt.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
            m_sqlManager.setBytes(stmt, 3, value);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (IOException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SERIALIZATION, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Deletes all projectResource from an given CmsProject.<p>
     *
     * @param projectId The project in which the resource is used
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public void deleteAllProjectResources(int projectId) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_PROJECTRESOURCES_DELETEALL");
            // delete all projectResources from the database
            stmt.setInt(1, projectId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /****************     methods for link management            ****************************/

    /**
     * Deletes all entrys in the link table that belong to the pageId.<p>
     *
     * @param pageId The resourceId (offline) of the page whose links should be deleted
     * @throws CmsException if something goes wrong
     */
    public void deleteLinkEntrys(CmsUUID pageId) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            // TODO all the link management methods should be carefully turned into project dependent code!       
            int dummyProjectId = Integer.MAX_VALUE;            
            conn = m_sqlManager.getConnection(dummyProjectId);
            stmt = m_sqlManager.getPreparedStatement(conn, dummyProjectId, "C_LM_DELETE_ENTRYS");
            stmt.setString(1, pageId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, "deleteLinkEntrys(CmsUUID)", CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Deletes all entrys in the online link table that belong to the pageId.<p>
     *
     * @param pageId The resourceId (online) of the page whose links should be deleted
     * @throws CmsException if something goes wrong
     */
    public void deleteOnlineLinkEntrys(CmsUUID pageId) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            // TODO all the link management methods should be carefully turned into project dependent code!      
            int dummyProjectId = I_CmsConstants.C_PROJECT_ONLINE_ID;  
            conn = m_sqlManager.getConnection(dummyProjectId);
            stmt = m_sqlManager.getPreparedStatement(conn, dummyProjectId, "C_LM_DELETE_ENTRYS");
            // delete all project-resources.
            stmt.setString(1, pageId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, "deleteOnlineLinkEntrys(CmsUUID)", CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Deletes a project from the cms.
     * Therefore it deletes all files, resources and properties.
     *
     * @param project the project to delete.
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public void deleteProject(CmsProject project) throws CmsException {

        // delete the resources from project_resources
        deleteAllProjectResources(project.getId());

        // finally delete the project
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_PROJECTS_DELETE");
            // create the statement
            stmt.setInt(1, project.getId());
            stmt.executeUpdate();
        } catch (Exception exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Deletes all properties for a project.
     *
     * @param project The project to delete.
     *
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public void deleteProjectProperties(CmsProject project) throws CmsException {

        // delete properties with one statement
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_PROPERTIES_DELETEALLPROP");
            // create statement
            stmt.setInt(1, project.getId());
            stmt.executeQuery();
        } catch (SQLException exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }

    }

    /**
     * delete a projectResource from an given CmsResource object.<p>
     *
     * @param projectId id of the project in which the resource is used
     * @param resourceName name of the resource to be deleted from the Cms
     * @throws CmsException if something goes wrong
     */
    public void deleteProjectResource(int projectId, String resourceName) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_PROJECTRESOURCES_DELETE");
            // delete resource from the database
            stmt.setInt(1, projectId);
            stmt.setString(2, resourceName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Deletes a specified project
     *
     * @param project The project to be deleted.
     * @throws CmsException  Throws CmsException if operation was not succesful.
     */
    public void deleteProjectResources(CmsProject project) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = m_sqlManager.getConnection();
            // stmt = m_sqlManager.getPreparedStatement(conn, "C_RESOURCES_DELETE_PROJECT");
            // delete all project-resources.
            // stmt.setInt(1, project.getId());
            // stmt.executeQuery();
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_DELETE_BY_PROJECTID");
            stmt.setInt(1, project.getId());
            stmt.executeUpdate();
            
			m_sqlManager.closeAll(null, stmt, null);
			
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_STRUCTURE_DELETE_BY_PROJECTID");
            stmt.setInt(1, project.getId());
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Deletes old sessions.
     */
    public void deleteSessions() {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_SESSION_DELETE");
            stmt.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis() - C_SESSION_TIMEOUT));
            stmt.execute();
        } catch (Exception e) {
            if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_INFO)) {
                OpenCms.log(I_CmsLogChannels.C_OPENCMS_INFO, "[" + this.getClass().getName() + "] error while deleting old sessions: " + com.opencms.util.Utils.getStackTrace(e));
            }
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Deletes a serializable object from the systempropertys.
     *
     * @param name The name of the property.
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public void deleteSystemProperty(String name) throws CmsException {

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_SYSTEMPROPERTIES_DELETE");
            stmt.setString(1, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }
    
    /**
     * @see org.opencms.db.I_CmsProjectDriver#destroy()
     */
    public void destroy() throws Throwable {
        finalize();
                
        if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_INIT)) {
            OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, "[" + this.getClass().getName() + "] destroyed!");
        }
    }

    /**
     * Ends a task from the Cms.<p>
     *
     * @param taskId Id of the task to end
     * @throws CmsException if something goes wrong
     */
    public void endTask(int taskId) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_TASK_END");
            stmt.setInt(1, 100);
            stmt.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
            stmt.setInt(3, taskId);
            stmt.executeUpdate();

        } catch (SQLException exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Method to init all default-resources.<p>
     * 
     * @throws CmsException if something goes wrong
     */
    public void fillDefaults() throws CmsException {
        try {
            if (readProject(I_CmsConstants.C_PROJECT_ONLINE_ID) != null) {
                // online-project exists - no need of filling defaults
                return;
            }
        } catch (CmsException exc) {
            // ignore the exception - the project was not readable so fill in the defaults
        }
        
        if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_INIT)) {
            OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". Database init        : filling default values");
        }
              
        // set the groups
        CmsGroup guests = m_driverManager.getUserDriver().createGroup(CmsUUID.getConstantUUID(OpenCms.getDefaultUsers().getGroupGuests()), OpenCms.getDefaultUsers().getGroupGuests(), "The guest group", I_CmsConstants.C_FLAG_ENABLED, null);
        CmsGroup administrators = m_driverManager.getUserDriver().createGroup(CmsUUID.getConstantUUID(OpenCms.getDefaultUsers().getGroupAdministrators()), OpenCms.getDefaultUsers().getGroupAdministrators(), "The administrators group", I_CmsConstants.C_FLAG_ENABLED | I_CmsConstants.C_FLAG_GROUP_PROJECTMANAGER, null);
        CmsGroup users = m_driverManager.getUserDriver().createGroup(CmsUUID.getConstantUUID(OpenCms.getDefaultUsers().getGroupUsers()), OpenCms.getDefaultUsers().getGroupUsers(), "The users group", I_CmsConstants.C_FLAG_ENABLED | I_CmsConstants.C_FLAG_GROUP_ROLE | I_CmsConstants.C_FLAG_GROUP_PROJECTCOWORKER, null);
        CmsGroup projectmanager = m_driverManager.getUserDriver().createGroup(CmsUUID.getConstantUUID(OpenCms.getDefaultUsers().getGroupProjectmanagers()), OpenCms.getDefaultUsers().getGroupProjectmanagers(), "The projectmanager group", I_CmsConstants.C_FLAG_ENABLED | I_CmsConstants.C_FLAG_GROUP_PROJECTMANAGER | I_CmsConstants.C_FLAG_GROUP_PROJECTCOWORKER | I_CmsConstants.C_FLAG_GROUP_ROLE, users.getName());

        // add the users
        CmsUser guest = m_driverManager.getUserDriver().addImportUser(CmsUUID.getConstantUUID(OpenCms.getDefaultUsers().getUserGuest()), OpenCms.getDefaultUsers().getUserGuest(), m_driverManager.getUserDriver().digest(""), m_driverManager.getUserDriver().digest(""), "The guest user", " ", " ", " ", 0, 0, I_CmsConstants.C_FLAG_ENABLED, new Hashtable(), guests, " ", " ", I_CmsConstants.C_USER_TYPE_SYSTEMUSER);
        CmsUser admin = m_driverManager.getUserDriver().addImportUser(CmsUUID.getConstantUUID(OpenCms.getDefaultUsers().getUserAdmin()), OpenCms.getDefaultUsers().getUserAdmin(), m_driverManager.getUserDriver().digest("admin"), m_driverManager.getUserDriver().digest(""), "The admin user", " ", " ", " ", 0, 0, I_CmsConstants.C_FLAG_ENABLED, new Hashtable(), administrators, " ", " ", I_CmsConstants.C_USER_TYPE_SYSTEMUSER);
        m_driverManager.getUserDriver().addUserToGroup(guest.getId(), guests.getId());
        m_driverManager.getUserDriver().addUserToGroup(admin.getId(), administrators.getId());
        m_driverManager.getWorkflowDriver().writeTaskType(1, 0, "../taskforms/adhoc.asp", "Ad-Hoc", "30308", 1, 1);
        
        ////////////////////////////////////////////////////////////////////////////////////////////
        // online project stuff
        ////////////////////////////////////////////////////////////////////////////////////////////        

        // create the online project
        CmsTask task = m_driverManager.getWorkflowDriver().createTask(0, 0, 1, admin.getId(), admin.getId(), administrators.getId(), I_CmsConstants.C_PROJECT_ONLINE, new java.sql.Timestamp(new java.util.Date().getTime()), new java.sql.Timestamp(new java.util.Date().getTime()), I_CmsConstants.C_TASK_PRIORITY_NORMAL);
        CmsProject online = createProject(admin, users /* guests */, projectmanager, task, I_CmsConstants.C_PROJECT_ONLINE, "the online-project", I_CmsConstants.C_FLAG_ENABLED, I_CmsConstants.C_PROJECT_TYPE_NORMAL);

        // create the root-folder for the online project
        CmsFolder onlineRootFolder = m_driverManager.getVfsDriver().createFolder(online, CmsUUID.getNullUUID(), CmsUUID.getNullUUID(), "/", 0, 0, admin.getId(), 0, admin.getId());
        onlineRootFolder.setState(I_CmsConstants.C_STATE_UNCHANGED);
        m_driverManager.getVfsDriver().writeFolder(online, onlineRootFolder, CmsDriverManager.C_UPDATE_ALL, onlineRootFolder.getUserLastModified());        		
           
        ////////////////////////////////////////////////////////////////////////////////////////////
        // setup project stuff
        ////////////////////////////////////////////////////////////////////////////////////////////
        
        // create the task for the setup project
        task = m_driverManager.getWorkflowDriver().createTask(0, 0, 1, admin.getId(), admin.getId(), administrators.getId(), "_setupProject", new java.sql.Timestamp(new java.util.Date().getTime()), new java.sql.Timestamp(new java.util.Date().getTime()), I_CmsConstants.C_TASK_PRIORITY_NORMAL);
        CmsProject setup = createProject(admin, administrators, administrators, task, "_setupProject", "Initial site import", I_CmsConstants.C_FLAG_ENABLED, I_CmsConstants.C_PROJECT_TYPE_TEMPORARY);

        // create the root-folder for the offline project
        CmsFolder setupRootFolder = m_driverManager.getVfsDriver().createFolder(setup, onlineRootFolder, CmsUUID.getNullUUID());        
        setupRootFolder.setState(I_CmsConstants.C_STATE_UNCHANGED);
        m_driverManager.getVfsDriver().writeFolder(setup, setupRootFolder, CmsDriverManager.C_UPDATE_ALL, setupRootFolder.getUserLastModified());

    }

    /**
	 * @see java.lang.Object#finalize()
	 */
    protected void finalize() throws Throwable {
        if (m_sqlManager!=null) {
            m_sqlManager.finalize();
        }
        
        m_sqlManager = null;      
        m_driverManager = null;        
    }

    /**
     * Forwards a task to another user.
     *
     * @param taskId The id of the task that will be fowarded.
     * @param newRoleId The new Group the task belongs to
     * @param newUserId User who gets the task.
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public void forwardTask(int taskId, CmsUUID newRoleId, CmsUUID newUserId) throws CmsException {

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_TASK_FORWARD");
            stmt.setString(1, newRoleId.toString());
            stmt.setString(2, newUserId.toString());
            stmt.setInt(3, taskId);
            stmt.executeUpdate();
        } catch (SQLException exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }
    
    /**
     * Returns all projects, which are accessible by a group.<p>
     *
     * @param group the requesting group
     * @return a Vector of projects
     * @throws CmsException if something goes wrong
     */
    public Vector getAllAccessibleProjectsByGroup(CmsGroup group) throws CmsException {
        Vector projects = new Vector();
        ResultSet res = null;
        Connection conn = null;
        PreparedStatement stmt = null;
    
        try {
            // create the statement
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_PROJECTS_READ_BYGROUP");
    
            stmt.setString(1, group.getId().toString());
            stmt.setString(2, group.getId().toString());
            res = stmt.executeQuery();
    
            while (res.next()) {
                projects.addElement(new CmsProject(res, m_sqlManager));
            }
        } catch (Exception exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return (projects);
    }
    
    /**
     * Returns all projects, which are manageable by a group.<p>
     *
     * @param group The requesting group
     * @return a Vector of projects
     * @throws CmsException if something goes wrong
     */
    public Vector getAllAccessibleProjectsByManagerGroup(CmsGroup group) throws CmsException {
        Vector projects = new Vector();
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
    
        try {
            // create the statement
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_PROJECTS_READ_BYMANAGER");
            
            stmt.setString(1, group.getId().toString());
            res = stmt.executeQuery();
    
            while (res.next()) {
                projects.addElement(new CmsProject(res, m_sqlManager));
            }
        } catch (Exception exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return (projects);
    }
    
    /**
     * Returns all projects, which are owned by a user.<p>
     *
     * @param user The requesting user
     * @return a Vector of projects
     * @throws CmsException if something goes wrong
     */
    public Vector getAllAccessibleProjectsByUser(CmsUser user) throws CmsException {
        Vector projects = new Vector();
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
    
        try {
            // create the statement
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_PROJECTS_READ_BYUSER");
    
            stmt.setString(1, user.getId().toString());
            res = stmt.executeQuery();
    
            while (res.next()) {
                projects.addElement(new CmsProject(res, m_sqlManager));
            }
        } catch (Exception exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return (projects);
    }

    /**
     * Reads all export links.<p>
     *
     * @return a Vector(of Strings) with the links
     * @throws CmsException if something goes wrong
     */
    public Vector getAllExportLinks() throws CmsException {
        Vector retValue = new Vector();
        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;
        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_EXPORT_GET_ALL_LINKS");
            res = stmt.executeQuery();
            while (res.next()) {
                retValue.add(res.getString(m_sqlManager.get("C_EXPORT_LINK")));
            }
            return retValue;
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, "getAllExportLinks()", CmsException.C_SQL_ERROR, e, false);
        } catch (Exception e) {
            throw m_sqlManager.getCmsException(this, "getAllExportLinks()", CmsException.C_UNKNOWN_EXCEPTION, e, false);
        } finally {
            // close all db-resources
            m_sqlManager.closeAll(conn, stmt, res);
        }
    }

    /**
     * Returns all projects, with the overgiven state.<p>
     *
     * @param state The state of the projects to read
     * @return a Vector of projects
     * @throws CmsException if something goes wrong
     */
    public Vector getAllProjects(int state) throws CmsException {
        Vector projects = new Vector();
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            // create the statement
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_PROJECTS_READ_BYFLAG");

            stmt.setInt(1, state);
            res = stmt.executeQuery();

            while (res.next()) {
                projects.addElement(
                    new CmsProject(
                        res.getInt(m_sqlManager.get("C_PROJECTS_PROJECT_ID")),
                        res.getString(m_sqlManager.get("C_PROJECTS_PROJECT_NAME")),
                        res.getString(m_sqlManager.get("C_PROJECTS_PROJECT_DESCRIPTION")),
                        res.getInt(m_sqlManager.get("C_PROJECTS_TASK_ID")),
                        new CmsUUID(res.getString(m_sqlManager.get("C_PROJECTS_USER_ID"))),
                        new CmsUUID(res.getString(m_sqlManager.get("C_PROJECTS_GROUP_ID"))),
                        new CmsUUID(res.getString(m_sqlManager.get("C_PROJECTS_MANAGERGROUP_ID"))),
                        res.getInt(m_sqlManager.get("C_PROJECTS_PROJECT_FLAGS")),
                        SqlHelper.getTimestamp(res, m_sqlManager.get("C_PROJECTS_PROJECT_CREATEDATE")),
                        res.getInt(m_sqlManager.get("C_PROJECTS_PROJECT_TYPE"))));
            }
        } catch (SQLException exc) {
            throw m_sqlManager.getCmsException(this, "getAllProjects(int)", CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return (projects);
    }

    /**
    * Reads all export links that depend on the resource.<p>
    * 
    * @param resources vector of resources 
    * @return a Vector(of Strings) with the linkrequest names
    * @throws CmsException if something goes wrong
    */
    public Vector getDependingExportLinks(Vector resources) throws CmsException {
        Vector retValue = new Vector();
        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;
        try {
            Vector firstResult = new Vector();
            Vector secondResult = new Vector();
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_EXPORT_GET_ALL_DEPENDENCIES");
            res = stmt.executeQuery();
            while (res.next()) {
                firstResult.add(res.getString(m_sqlManager.get("C_EXPORT_DEPENDENCIES_RESOURCE")));
                secondResult.add(res.getString(m_sqlManager.get("C_EXPORT_LINK")));
            }
            // now we have all dependencies that are there. We can search now for
            // the ones we need
            for (int i = 0; i < resources.size(); i++) {
                for (int j = 0; j < firstResult.size(); j++) {
                    if (((String) firstResult.elementAt(j)).startsWith((String) resources.elementAt(i))) {
                        if (!retValue.contains(secondResult.elementAt(j))) {
                            retValue.add(secondResult.elementAt(j));
                        }
                    } else if (((String) resources.elementAt(i)).startsWith((String) firstResult.elementAt(j))) {
                        if (!retValue.contains(secondResult.elementAt(j))) {
                            // only direct subfolders count
                            int index = ((String) firstResult.elementAt(j)).length();
                            String test = ((String) resources.elementAt(i)).substring(index);
                            index = test.indexOf("/");
                            if (index == -1 || index + 1 == test.length()) {
                                retValue.add(secondResult.elementAt(j));
                            }
                        }
                    }
                }
            }
            return retValue;
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, "getDependingExportlinks(Vector)", CmsException.C_SQL_ERROR, e, false);
        } catch (Exception e) {
            throw m_sqlManager.getCmsException(this, "getDependingExportLinks(Vector)", CmsException.C_UNKNOWN_EXCEPTION, e, false);
        } finally {
            // close all db-resources
            m_sqlManager.closeAll(conn, stmt, res);
        }
    }

    /**
     * Searches for broken links in the online project.<p>
     *
     * @return A Vector with a CmsPageLinks object for each page containing broken links
     *          this CmsPageLinks object contains all links on the page withouth a valid target
     * @throws CmsException if something goes wrong
     */
    public Vector getOnlineBrokenLinks() throws CmsException {
        Vector result = new Vector();
        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;
        try {
            conn = m_sqlManager.getConnection(I_CmsConstants.C_PROJECT_ONLINE_ID);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_LM_GET_ONLINE_BROKEN_LINKS");
            res = stmt.executeQuery();
            CmsUUID current = CmsUUID.getNullUUID();
            CmsPageLinks links = null;
            while (res.next()) {
                CmsUUID next = new CmsUUID(res.getString(m_sqlManager.get("C_LM_PAGE_ID")));
                if (!next.equals(current)) {
                    if (links != null) {
                        result.add(links);
                    }
                    links = new CmsPageLinks(next);
                    links.addLinkTarget(res.getString(m_sqlManager.get("C_LM_LINK_DEST")));
                    try {
                        links.setResourceName((m_driverManager.getVfsDriver().readFileHeader(I_CmsConstants.C_PROJECT_ONLINE_ID, next, false)).getName());
                    } catch (CmsException e) {
                        links.setResourceName("id=" + next + ". Sorry, can't read resource. " + e.getMessage());
                    }
                    links.setOnline(true);
                } else {
                    links.addLinkTarget(res.getString(m_sqlManager.get("C_LM_LINK_DEST")));
                }
                current = next;
            }
            if (links != null) {
                result.add(links);
            }
            return result;
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, "getOnlineBrokenLinks()", CmsException.C_SQL_ERROR, e, false);
        } catch (Exception e) {
            throw m_sqlManager.getCmsException(this, "getOnlineBrokenLinks()", CmsException.C_UNKNOWN_EXCEPTION, e, false);
        } finally {
            // close all db-resources
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Retrieves the online project from the database.
     *
     * @return com.opencms.file.CmsProject the  onlineproject for the given project.
     * @throws CmsException Throws CmsException if the resource is not found, or the database communication went wrong.
     */
    public CmsProject getOnlineProject() throws CmsException {
        return readProject(I_CmsConstants.C_PROJECT_ONLINE_ID);
    }

    /**
     * @see org.opencms.db.I_CmsDriver#init(source.org.apache.java.util.Configurations, java.util.List, org.opencms.db.CmsDriverManager)
     */    
    public void init(Configurations config, List successiveDrivers, CmsDriverManager driverManager) {
        String poolUrl = config.getString("db.project.pool");

        m_sqlManager = this.initQueries();
        m_sqlManager.setOfflinePoolUrl(poolUrl);
        m_sqlManager.setOnlinePoolUrl(poolUrl);
        m_sqlManager.setBackupPoolUrl(poolUrl);        
      
        m_driverManager = driverManager;  

        if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_INIT)) {
            OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, ". Assigned pool        : " + poolUrl);
        }
                
        if (successiveDrivers != null && !successiveDrivers.isEmpty()) {
            if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_INIT)) {
                OpenCms.log(I_CmsLogChannels.C_OPENCMS_INIT, this.getClass().toString() + " does not support successive drivers.");
            }
        }
    }    

    /**
     * @see org.opencms.db.I_CmsProjectDriver#initQueries(java.lang.String)
     */
    public org.opencms.db.generic.CmsSqlManager initQueries() {
        return new org.opencms.db.generic.CmsSqlManager();
    }

    /**
     * Publishes a specified project to the online project.<p>
     *
     * @param context the context
     * @param onlineProject the online project of the OpenCms
     * @param backupEnabled flag if the backup is enabled
     * @param backupTagId the backup tag id
     * @param report a report object to provide the loggin messages
     * @param exportpoints the exportpoints
     * @param directPublishResource contains CmsResource when in publish directly mode
     * @param maxVersions maximum number of backup versions
     * @return a vector of changed or deleted resources
     * @throws CmsException if something goes wrong
     */
    public synchronized Vector publishProject(CmsRequestContext context, CmsProject onlineProject, boolean backupEnabled, int backupTagId, I_CmsReport report, Hashtable exportpoints, CmsResource directPublishResource, int maxVersions) throws Exception {
        CmsExportPointDriver discAccess = null;
        CmsFolder currentFolder = null;
        CmsFile currentFile = null;
        CmsResource currentFileHeader = null;
        CmsLock currentLock = null;
        List offlineFolders = null;
        List offlineFiles = null;
        List deletedFolders = (List) new ArrayList();
        Vector changedResources = new Vector();
        String currentExportKey = null;
        String currentResourceName = null;
        long publishDate = System.currentTimeMillis();
        Iterator i = null;
        boolean publishCurrentResource = false;
        List projectResources = null;
        Map sortedFolderMap = null;
        List sortedFolderList = null;
        byte[] contents = null;
        int publishHistoryId = nextPublishVersionId();
        String encoding = null;
        int m, n;

        try {
            discAccess = new CmsExportPointDriver(exportpoints);

            if (backupEnabled) {
                // write an entry in the publish project log
                m_driverManager.backupProject(context, context.currentProject(), backupTagId, publishDate);
            }

            // read the project resources of the project that gets published
            projectResources = m_driverManager.readProjectResources(context, context.currentProject());

            // read all changed/new/deleted folders
            offlineFolders = m_driverManager.getVfsDriver().readFolders(context.currentProject().getId());

            // ensure that the folders appear in the correct (DFS) tree order
            // sort out folders that will not be published
            sortedFolderMap = (Map) new HashMap();
            i = offlineFolders.iterator();
            while (i.hasNext()) {
                publishCurrentResource = false;

                currentFolder = (CmsFolder) i.next();
                currentResourceName = m_driverManager.readPath(context, currentFolder, true);
                currentFolder.setFullResourceName(currentResourceName);
                currentLock = m_driverManager.getLock(context, currentResourceName);

                // the resource must have either a new/deleted state in the link or a new/delete state in the resource record
                publishCurrentResource = currentFolder.getState() > I_CmsConstants.C_STATE_UNCHANGED;

                if (context.currentProject().getType() == I_CmsConstants.C_PROJECT_TYPE_DIRECT_PUBLISH && directPublishResource != null) {
                    // the resource must be a sub resource of the direct-publish-resource in case of a "direct publish"
                    publishCurrentResource = publishCurrentResource && currentResourceName.startsWith(directPublishResource.getRootPath());
                } else {
                    // the resource must have a changed state and must be changed in the project that is currently published
                    publishCurrentResource = publishCurrentResource && currentFolder.getProjectLastModified() == context.currentProject().getId();

                    // the resource must be in one of the paths defined for the project            
                    publishCurrentResource = publishCurrentResource && CmsProject.isInsideProject(projectResources, currentFolder);
                }

                // the resource must be unlocked
                publishCurrentResource = publishCurrentResource && currentLock.isNullLock();

                if (publishCurrentResource) {
                    sortedFolderMap.put(currentResourceName, currentFolder);
                }
            }

            sortedFolderList = (List) new ArrayList(sortedFolderMap.keySet());
            Collections.sort(sortedFolderList);

            offlineFolders.clear();
            offlineFolders = null;

            m = 1;
            n = sortedFolderList.size();
            i = sortedFolderList.iterator();

            if (n > 0) {
                report.println(report.key("report.publish_folders_begin"), I_CmsReport.C_FORMAT_HEADLINE);
            }

            while (i.hasNext()) {
                currentResourceName = (String) i.next();
                currentFolder = (CmsFolder) sortedFolderMap.get(currentResourceName);
                currentExportKey = checkExport(currentResourceName, exportpoints);

                if (currentFolder.getState() == I_CmsConstants.C_STATE_DELETED) {
                    // C_STATE_DELETE
                    deletedFolders.add(currentFolder);
                    changedResources.addElement(currentResourceName);
                } else if (currentFolder.getState() == I_CmsConstants.C_STATE_NEW) {
                    changedResources.addElement(currentResourceName);

                    // export to filesystem if necessary
                    if (currentExportKey != null) {
                        discAccess.createFolder(currentResourceName, currentExportKey);
                    }
                    
                    // bounce the current publish task through all project drivers
                    m_driverManager.getProjectDriver().publishFolder(context, report, m++, n, onlineProject, currentFolder, backupEnabled, publishDate, publishHistoryId, backupTagId, maxVersions);
                } else if (currentFolder.getState() == I_CmsConstants.C_STATE_CHANGED) {
                    changedResources.addElement(currentResourceName);
                    
                    // export to filesystem if necessary
                    if (currentExportKey != null) {
                        discAccess.createFolder(currentResourceName, currentExportKey);
                    }                    
                    
                    // bounce the current publish task through all project drivers
                    m_driverManager.getProjectDriver().publishFolder(context, report, m++, n, onlineProject, currentFolder, backupEnabled, publishDate, publishHistoryId, backupTagId, maxVersions);                   
                }

                i.remove();
            }

            if (n > 0) {
                report.println(report.key("report.publish_folders_end"), I_CmsReport.C_FORMAT_HEADLINE);
            }

            if (sortedFolderList != null) {
                sortedFolderList.clear();
                sortedFolderList = null;
            }

            if (sortedFolderMap != null) {
                sortedFolderMap.clear();
                sortedFolderMap = null;
            }

            ///////////////////////////////////////////////////////////////////////////////////////

            // now read all changed/new/deleted files
            offlineFiles = m_driverManager.getVfsDriver().readFiles(context.currentProject().getId());

            // sort out files that will not be published
            i = offlineFiles.iterator();
            while (i.hasNext()) {
                publishCurrentResource = false;

                currentFileHeader = (CmsResource) i.next();
                currentResourceName = m_driverManager.readPath(context, currentFileHeader, true);
                currentFileHeader.setFullResourceName(currentResourceName);
                currentLock = m_driverManager.getLock(context, currentResourceName);

                switch (currentFileHeader.getState()) {
                    // the current resource is deleted
                    case I_CmsConstants.C_STATE_DELETED :
                        // it is published, if it was changed to deleted in the current project
                        String delProject = m_driverManager.getVfsDriver().readProperty(I_CmsConstants.C_PROPERTY_INTERNAL, context.currentProject().getId(), currentFileHeader, currentFileHeader.getType());

                        if (delProject != null && delProject.equals("" + context.currentProject().getId())) {
                            publishCurrentResource = true;
                        } else {
                            publishCurrentResource = false;
                        }
                        break;

                        // the current resource is new   
                    case I_CmsConstants.C_STATE_NEW :
                        // it is published, if it was created in the current project
                        // or if it is a new sibling of another resource that is currently not changed in any project
                        publishCurrentResource = currentFileHeader.getProjectLastModified() == context.currentProject().getId() || currentFileHeader.getProjectLastModified() == 0;
                        break;

                        // the current resource is changed
                    case I_CmsConstants.C_STATE_CHANGED :
                        // it is published, if it was changed in the current project
                        publishCurrentResource = currentFileHeader.getProjectLastModified() == context.currentProject().getId();
                        break;

                        // the current resource is unchanged
                    case I_CmsConstants.C_STATE_UNCHANGED :
                    default :
                        // so it is not published
                        publishCurrentResource = false;
                        break;
                }

                if (context.currentProject().getType() == I_CmsConstants.C_PROJECT_TYPE_DIRECT_PUBLISH && directPublishResource != null) {
                    // the resource must be a sub resource of the direct-publish-resource in case of a "direct publish"
                    publishCurrentResource = publishCurrentResource && currentResourceName.startsWith(directPublishResource.getRootPath());
                } else {
                    // the resource must be in one of the paths defined for the project
                    publishCurrentResource = publishCurrentResource && CmsProject.isInsideProject(projectResources, currentFileHeader);
                }

                // do not publish resource that are locked
                publishCurrentResource = publishCurrentResource && currentLock.isNullLock();

                // handle temporary files immediately here coz they aren't "published" anyway
                if (currentLock.isNullLock()) {
                    // remove any possible temporary files for this resource
                    m_driverManager.getVfsDriver().removeTemporaryFile(currentFileHeader);
                }

                if (currentFileHeader.getName().startsWith(I_CmsConstants.C_TEMP_PREFIX)) {
                    // trash the current resource if it is a temporary file
                    m_driverManager.getVfsDriver().deleteAllProperties(context.currentProject().getId(), currentFileHeader);
                    m_driverManager.getVfsDriver().removeFile(context.currentProject(), currentFileHeader);
                }

                if (!publishCurrentResource) {
                    i.remove();
                }
            }

            m = 1;
            n = offlineFiles.size();
            i = offlineFiles.iterator();

            if (n > 0) {
                report.println(report.key("report.publish_files_begin"), I_CmsReport.C_FORMAT_HEADLINE);
            }

            while (i.hasNext()) {                
                currentFileHeader = (CmsResource) i.next();
                currentResourceName = currentFileHeader.getRootPath();
                currentExportKey = checkExport(currentResourceName, exportpoints);
                
                currentFile = m_driverManager.getVfsDriver().readFile(context.currentProject().getId(), true, currentFileHeader.getStructureId());
                currentFile.setFullResourceName(currentResourceName);                

                if (currentFileHeader.getState() == I_CmsConstants.C_STATE_DELETED) {
                    changedResources.addElement(currentResourceName);

                    if (currentExportKey != null) {
                        // delete the export point
                        try {
                            discAccess.removeResource(currentResourceName, currentExportKey);
                        } catch (CmsException e) {
                            if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                                OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishProject()] error deleting export point, type: " + e.getType() + ",  " + currentFile.toString());
                            }
                        } catch (Exception e) {
                            if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                                OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishProject()] error deleting export point " + currentFile.toString());
                            }
                        }
                    }
                    
                    // bounce the current publish task through all project drivers
                    m_driverManager.getProjectDriver().publishFile(context, report, m++, n, onlineProject, currentFileHeader, backupEnabled, publishDate, publishHistoryId, backupTagId, maxVersions);                   
                } else if (currentFileHeader.getState() == I_CmsConstants.C_STATE_CHANGED) {
                    changedResources.addElement(currentResourceName);

                    if (currentExportKey != null) {
                        // write the export point
                        try {
                            // make sure files are written in the right encoding 
                            contents = currentFile.getContents();
                            encoding = m_driverManager.getVfsDriver().readProperty(I_CmsConstants.C_PROPERTY_CONTENT_ENCODING, context.currentProject().getId(), currentFile, currentFile.getType());

                            if (encoding != null) {
                                // only files that have the encodig property set will be encoded,
                                // other files will be ignored. images etc. are not touched.                        
                                try {
                                    contents = (new String(contents, encoding)).getBytes();
                                } catch (UnsupportedEncodingException uex) {
                                    // noop
                                }
                            }

                            discAccess.writeFile(currentResourceName, currentExportKey, contents);
                        } catch (CmsException e) {
                            if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                                OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishProject()] error writing export point, type: " + e.getType() + ",  " + currentFile.toString());
                            }
                        } catch (Exception e) {
                            if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                                OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishProject()] error writing export point " + currentFile.toString());
                            }
                        }
                    }
                    
                    // bounce the current publish task through all project drivers
                    m_driverManager.getProjectDriver().publishFile(context, report, m++, n, onlineProject, currentFileHeader, backupEnabled, publishDate, publishHistoryId, backupTagId, maxVersions);                                                                      
                } else if (currentFileHeader.getState() == I_CmsConstants.C_STATE_NEW) {
                    changedResources.addElement(currentResourceName);

                    // export to filesystem if necessary
                    if (currentExportKey != null) {
                        // Encoding project: Make sure files are written in the right encoding 
                        contents = currentFile.getContents();
                        encoding = m_driverManager.getVfsDriver().readProperty(I_CmsConstants.C_PROPERTY_CONTENT_ENCODING, context.currentProject().getId(), currentFile, currentFile.getType());
                        if (encoding != null) {
                            // Only files that have the encodig property set will be encoded,
                            // the other files will be ignored. So images etc. are not touched.
                            try {
                                contents = (new String(contents, encoding)).getBytes();
                            } catch (UnsupportedEncodingException uex) {
                                // contents will keep original value
                            }
                        }
                        discAccess.writeFile(currentResourceName, currentExportKey, contents);
                    }
                    
                    // bounce the current publish task through all project drivers
                    m_driverManager.getProjectDriver().publishFile(context, report, m++, n, onlineProject, currentFileHeader, backupEnabled, publishDate, publishHistoryId, backupTagId, maxVersions);                                       
                }

                i.remove();
            }

            if (n > 0) {
                report.println(report.key("report.publish_files_end"), I_CmsReport.C_FORMAT_HEADLINE);
            }

            if (offlineFiles != null) {
                offlineFiles.clear();
                offlineFiles = null;
            }

            ////////////////////////////////////////////////////////////////////////////////////////

            // now delete the "deleted" folders       
            if (deletedFolders.isEmpty()) {
                return changedResources;
            }

            // ensure that the folders appear in the correct (DFS) tree order
            sortedFolderMap = (Map) new HashMap();
            i = deletedFolders.iterator();
            while (i.hasNext()) {
                currentFolder = (CmsFolder) i.next();
                currentResourceName = currentFolder.getRootPath();
                sortedFolderMap.put(currentResourceName, currentFolder);
            }
            sortedFolderList = (List) new ArrayList(sortedFolderMap.keySet());
            Collections.sort(sortedFolderList);

            // reverse the order of the folder to delete them in a bottom-up order
            Collections.reverse(sortedFolderList);

            if (deletedFolders != null) {
                deletedFolders.clear();
                deletedFolders = null;
            }

            m = 1;
            n = sortedFolderList.size();
            i = sortedFolderList.iterator();

            if (n > 0) {
                report.println(report.key("report.publish_delete_folders_begin"), I_CmsReport.C_FORMAT_HEADLINE);
            }

            while (i.hasNext()) {
                currentResourceName = (String) i.next();
                currentFolder = (CmsFolder) sortedFolderMap.get(currentResourceName);
                currentExportKey = checkExport(currentResourceName, exportpoints);

                if (currentExportKey != null) {
                    discAccess.removeResource(currentResourceName, currentExportKey);
                }
                
                // bounce the current publish task through all project drivers
                m_driverManager.getProjectDriver().publishDeletedFolder(context, report, m++, n, onlineProject, currentFolder, backupEnabled, publishDate, publishHistoryId, backupTagId, maxVersions);

                i.remove();
            }

            if (n > 0) {
                report.println(report.key("report.publish_delete_folders_end"), I_CmsReport.C_FORMAT_HEADLINE);
            }

            if (sortedFolderList != null) {
                sortedFolderList.clear();
                sortedFolderList = null;
            }

            if (sortedFolderMap != null) {
                sortedFolderMap.clear();
                sortedFolderMap = null;
            }
        } catch (Exception e) {
            // these are dummy catch blocks to have a finally block for clearing 
            // allocated resources. thus the exceptions are just logged and 
            // immediately thrown to the upper app. layer.
            
            if (C_DEBUG) {
                System.err.println(e.getMessage());
                e.printStackTrace(System.err);
            }            
                      
            throw e;
        } catch (OutOfMemoryError o) {
            if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishProject] out of memory error!");
            }                      

            // clear all caches to reclaim memory
            OpenCms.fireCmsEvent(new CmsEvent(new CmsObject(), I_CmsEventListener.EVENT_CLEAR_CACHES, new HashMap(), false));

            // force a complete object finalization and garbage collection 
            System.runFinalization();
            Runtime.getRuntime().runFinalization();
            System.gc();
            Runtime.getRuntime().gc();

            throw o;
        } finally {
            if (sortedFolderList != null) {
                sortedFolderList.clear();
                sortedFolderList = null;
            }

            if (sortedFolderMap != null) {
                sortedFolderMap.clear();
                sortedFolderMap = null;
            }

            if (deletedFolders != null) {
                deletedFolders.clear();
                deletedFolders = null;
            }

            if (offlineFiles != null) {
                offlineFiles.clear();
                offlineFiles = null;
            }

            currentFile = null;
            currentFileHeader = null;
            currentFolder = null;
            discAccess = null;
            currentExportKey = null;
            contents = null;
        }

        return changedResources;
    }

    /**
     * Select all projectResources from an given project.<p>
     *
     * @param projectId the project in which the resource is used
     * @return Vector of resources belongig to the project
     * @throws CmsException if something goes wrong
     */
    public Vector readAllProjectResources(int projectId) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet res = null;
        Vector projectResources = new Vector();
        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_PROJECTRESOURCES_READALL");
            // select all resources from the database
            stmt.setInt(1, projectId);
            res = stmt.executeQuery();
            while (res.next()) {
                projectResources.addElement(res.getString("RESOURCE_NAME"));
            }
            res.close();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
        return projectResources;
    }

    /**
     * Returns a Vector (Strings) with the link destinations of all links on the page with
     * the pageId.<p>
     *
     * @param pageId The resourceId (offline) of the page whose liks should be read
     * @return the vector of link destinations
     * @throws CmsException if something goes wrong
     */
    public Vector readLinkEntrys(CmsUUID pageId) throws CmsException {
        Vector result = new Vector();
        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;
        try {
            // TODO all the link management methods should be carefully turned into project dependent code!    
            int dummyProjectId = Integer.MAX_VALUE;              
            conn = m_sqlManager.getConnection(dummyProjectId);
            stmt = m_sqlManager.getPreparedStatement(conn, dummyProjectId, "C_LM_READ_ENTRYS");
            stmt.setString(1, pageId.toString());
            res = stmt.executeQuery();
            while (res.next()) {
                result.add(res.getString(m_sqlManager.get("C_LM_LINK_DEST")));
            }
            return result;
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, "readLinkEntrys(CmsUUID)", CmsException.C_SQL_ERROR, e, false);
        } catch (Exception e) {
            throw m_sqlManager.getCmsException(this, "readLinkEntrys(CmsUUID)", CmsException.C_UNKNOWN_EXCEPTION, e, false);
        } finally {
            // close all db-resources
            m_sqlManager.closeAll(conn, stmt, res);
        }
    }

    /**
     * Reads the online id of a offline file.<p>
     * 
     * @param filename name of the file
     * @return the id or -1 if not found (should not happen)
     * @throws CmsException if something goes wrong
     */
    private CmsUUID readOnlineId(String filename) throws CmsException {
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        CmsUUID resourceId = CmsUUID.getNullUUID();

        try {
            conn = m_sqlManager.getConnection(I_CmsConstants.C_PROJECT_ONLINE_ID);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_LM_READ_ONLINE_ID");
            // read file data from database
            stmt.setString(1, filename);
            res = stmt.executeQuery();
            // read the id
            if (res.next()) {
                resourceId = new CmsUUID(res.getString(m_sqlManager.get("C_RESOURCES_STRUCTURE_ID")));
                while (res.next()) {
                    // do nothing only move through all rows because of mssql odbc driver
                }
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return resourceId;
    }

    /**
     * Returns a Vector (Strings) with the link destinations of all links on the page with
     * the pageId.<p>
     *
     * @param pageId The resourceId (online) of the page whose liks should be read
     * @return the vector of link destinations
     * @throws CmsException if something goes wrong
     */
    public Vector readOnlineLinkEntrys(CmsUUID pageId) throws CmsException {
        Vector result = new Vector();
        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;
        try {
            // TODO all the link management methods should be carefully turned into project dependent code!        
            int dummyProjectId = I_CmsConstants.C_PROJECT_ONLINE_ID;               
            conn = m_sqlManager.getConnection(dummyProjectId);
            stmt = m_sqlManager.getPreparedStatement(conn, dummyProjectId, "C_LM_READ_ENTRYS");
            stmt.setString(1, pageId.toString());
            res = stmt.executeQuery();
            while (res.next()) {
                result.add(res.getString(m_sqlManager.get("C_LM_LINK_DEST")));
            }
            return result;
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, "readOnlineLinkEntrys(CmsUUID)", CmsException.C_SQL_ERROR, e, false);
        } catch (Exception e) {
            throw m_sqlManager.getCmsException(this, "readOnlineLinkEntrys(CmsUUID)", CmsException.C_UNKNOWN_EXCEPTION, e, false);
        } finally {
            // close all db-resources
            m_sqlManager.closeAll(conn, stmt, res);
        }
    }

    /**
     * Reads a project by task-id.<p>
     *
     * @param task the task to read the project for
     * @return the project the tasks belongs to
     * @throws CmsException if something goes wrong
     */
    public CmsProject readProject(CmsTask task) throws CmsException {
        PreparedStatement stmt = null;
        CmsProject project = null;
        ResultSet res = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_PROJECTS_READ_BYTASK");

            stmt.setInt(1, task.getId());
            res = stmt.executeQuery();

            if (res.next())
                project = new CmsProject(res, m_sqlManager);
            else
                // project not found!
                throw new CmsException("[" + this.getClass().getName() + "] " + task, CmsException.C_NOT_FOUND);
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, "readProject(CmsTask)", CmsException.C_SQL_ERROR, e, false);
        } finally {
            // close all db-resources
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return project;
    }

    /**
     * Reads a project.<p>
     *
     * @param id the id of the project
     * @return the project with the given id
     * @throws CmsException if something goes wrong
     */
    public CmsProject readProject(int id) throws CmsException {
        PreparedStatement stmt = null;
        CmsProject project = null;
        ResultSet res = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_PROJECTS_READ");

            stmt.setInt(1, id);
            res = stmt.executeQuery();

            if (res.next()) {
                project =
                    new CmsProject(
                        res.getInt(m_sqlManager.get("C_PROJECTS_PROJECT_ID")),
                        res.getString(m_sqlManager.get("C_PROJECTS_PROJECT_NAME")),
                        res.getString(m_sqlManager.get("C_PROJECTS_PROJECT_DESCRIPTION")),
                        res.getInt(m_sqlManager.get("C_PROJECTS_TASK_ID")),
                        new CmsUUID(res.getString(m_sqlManager.get("C_PROJECTS_USER_ID"))),
                        new CmsUUID(res.getString(m_sqlManager.get("C_PROJECTS_GROUP_ID"))),
                        new CmsUUID(res.getString(m_sqlManager.get("C_PROJECTS_MANAGERGROUP_ID"))),
                        res.getInt(m_sqlManager.get("C_PROJECTS_PROJECT_FLAGS")),
                        SqlHelper.getTimestamp(res, m_sqlManager.get("C_PROJECTS_PROJECT_CREATEDATE")),
                        res.getInt(m_sqlManager.get("C_PROJECTS_PROJECT_TYPE")));
            } else {
                // project not found!
                throw m_sqlManager.getCmsException(this, "project with ID " + id + " not found", CmsException.C_NOT_FOUND, null, true);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, "readProject(int)/1 ", CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return project;
    }

    /**
     * Reads log entries for a project.<p>
     *
     * @param projectid the project for tasklog to read
     * @return a vector of new TaskLog objects
     * @throws CmsException if something goes wrong
     */
    public Vector readProjectLogs(int projectid) throws CmsException {
        ResultSet res = null;
        Connection conn = null;

        CmsTaskLog tasklog = null;
        Vector logs = new Vector();
        PreparedStatement stmt = null;
        String comment = null;
        java.sql.Timestamp starttime = null;
        int id = I_CmsConstants.C_UNKNOWN_ID;
        CmsUUID user = CmsUUID.getNullUUID();
        int type = I_CmsConstants.C_UNKNOWN_ID;

        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_TASKLOG_READ_PPROJECTLOGS");
            stmt.setInt(1, projectid);
            res = stmt.executeQuery();
            while (res.next()) {
                comment = res.getString(m_sqlManager.get("C_LOG_COMMENT"));
                id = res.getInt(m_sqlManager.get("C_LOG_ID"));
                starttime = SqlHelper.getTimestamp(res, m_sqlManager.get("C_LOG_STARTTIME"));
                user = new CmsUUID(res.getString(m_sqlManager.get("C_LOG_USER")));
                type = res.getInt(m_sqlManager.get("C_LOG_TYPE"));

                tasklog = new CmsTaskLog(id, comment, user, starttime, type);
                logs.addElement(tasklog);
            }
        } catch (SQLException exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } catch (Exception exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, exc, false);
        } finally {
            // close all db-resources
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return logs;
    }

    /**
     * Reads all resource from the Cms, that are in one project.<BR/>
     * A resource is either a file header or a folder.
     *
     * @param project The id of the project in which the resource will be used.
     * @param filter The filter for the resources to be read
     * @return A Vecor of resources.
     * @throws CmsException Throws CmsException if operation was not succesful
     */
    public List readProjectView(int project, String filter) throws CmsException {
        List resources = (List) new ArrayList();
        CmsResource currentResource = null;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        String orderClause = " ORDER BY CMS_T_STRUCTURE.STRUCTURE_ID";        
        String whereClause = new String();

		// TODO: dangerous - move this somehow into query.properties
        if ("new".equalsIgnoreCase(filter)) {
            whereClause = " AND (CMS_T_STRUCTURE.STRUCTURE_STATE=" + I_CmsConstants.C_STATE_NEW + " OR CMS_T_RESOURCES.RESOURCE_STATE=" + I_CmsConstants.C_STATE_NEW + ")";
        } else if ("changed".equalsIgnoreCase(filter)) {
            whereClause = " AND (CMS_T_STRUCTURE.STRUCTURE_STATE=" + I_CmsConstants.C_STATE_CHANGED + " OR CMS_T_RESOURCES.RESOURCE_STATE=" + I_CmsConstants.C_STATE_CHANGED + ")";
        } else if ("deleted".equalsIgnoreCase(filter)) {
            whereClause = " AND (CMS_T_STRUCTURE.STRUCTURE_STATE=" + I_CmsConstants.C_STATE_DELETED + " OR CMS_T_RESOURCES.RESOURCE_STATE=" + I_CmsConstants.C_STATE_DELETED + ")";
        } else if ("locked".equalsIgnoreCase(filter)) {
            whereClause = "";
        } else {
            whereClause = " AND (CMS_T_STRUCTURE.STRUCTURE_STATE!=" + I_CmsConstants.C_STATE_UNCHANGED + " OR CMS_T_RESOURCES.RESOURCE_STATE!=" + I_CmsConstants.C_STATE_UNCHANGED + ")";
        }        

        try {
            // TODO make the getConnection and getPreparedStatement calls project-ID dependent
            conn = m_sqlManager.getConnection();
            String query = m_sqlManager.get("C_RESOURCES_PROJECTVIEW") + whereClause + orderClause;
            stmt = m_sqlManager.getPreparedStatementForSql(conn, CmsSqlManager.replaceTableKey(I_CmsConstants.C_PROJECT_ONLINE_ID+1,query));

            stmt.setInt(1, project);
            res = stmt.executeQuery();

            while (res.next()) {
                currentResource = m_driverManager.getVfsDriver().createCmsResourceFromResultSet(res, project);
                resources.add(currentResource);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception ex) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, ex, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }

        return resources;
    }

    /**
     * Reads a session from the database.<p>
     *
     * @param sessionId the id og the session to read
     * @return the session data as Hashtable
     * @throws CmsException if something goes wrong
     */
    public Hashtable readSession(String sessionId) throws CmsException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        Hashtable sessionData = new Hashtable();
        Hashtable data = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_SESSION_READ");
            stmt.setString(1, sessionId);
            stmt.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis() - C_SESSION_TIMEOUT));

            res = stmt.executeQuery();

            // create new Cms user object
            if (res.next()) {
                // read the additional infos.
                byte[] value = m_sqlManager.getBytes(res, "SESSION_DATA");
                // now deserialize the object
                ByteArrayInputStream bin = new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                data = (Hashtable) oin.readObject();
                try {
                    for (;;) {
                        Object key = oin.readObject();
                        Object sessionValue = oin.readObject();
                        sessionData.put(key, sessionValue);
                    }
                } catch (EOFException exc) {
                    // reached eof - stop reading all is done now.
                }
                data.put(I_CmsConstants.C_SESSION_DATA, sessionData);
            } else {
                deleteSessions();
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, e, false);
        } finally {
            // close all db-resources
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return data;
    }

    /**
     * Reads a serializable object from the systempropertys.
     *
     * @param name The name of the property.
     * @return object The property-object.
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public Serializable readSystemProperty(String name) throws CmsException {

        Serializable property = null;
        byte[] value;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        // create get the property data from the database
        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_SYSTEMPROPERTIES_READ");
            stmt.setString(1, name);
            res = stmt.executeQuery();
            if (res.next()) {
                value = m_sqlManager.getBytes(res, m_sqlManager.get("C_SYSTEMPROPERTY_VALUE"));
                // now deserialize the object
                ByteArrayInputStream bin = new ByteArrayInputStream(value);
                ObjectInputStream oin = new ObjectInputStream(bin);
                property = (Serializable) oin.readObject();
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (IOException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SERIALIZATION, e, false);
        } catch (ClassNotFoundException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_CLASSLOADER_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }
        return property;
    }

    /**
     * Helper method to serialize the hashtable.<p>
     * This method is used by updateSession() and createSession().
	 * @param data the data to be serialized
	 * @return byte array of serialized data
	 * @throws IOException if something goes wrong 
     */
    private byte[] serializeSession(Hashtable data) throws IOException {
        // serialize the hashtable
        byte[] value;
        Hashtable sessionData = (Hashtable) data.remove(I_CmsConstants.C_SESSION_DATA);
        StringBuffer notSerializable = new StringBuffer();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);

        // first write the user data
        oout.writeObject(data);
        if (sessionData != null) {
            Enumeration keys = sessionData.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object sessionValue = sessionData.get(key);
                if (sessionValue instanceof Serializable) {
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
        value = bout.toByteArray();
        if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_INFO) && (notSerializable.length() > 0)) {
            OpenCms.log(I_CmsLogChannels.C_OPENCMS_INFO, "[" + this.getClass().getName() + "] warning, following entrys are not serializeable in the session: " + notSerializable.toString() + ".");
        }
        return value;
    }

    /** 
     * Sorts a vector of files or folders alphabetically.
     * This method uses an insertion sort algorithm.
     * NOT IN USE AT THIS TIME
     *
     * @param list array of strings containing the list of files or folders
     * @return Vector of sorted strings
     */
    protected Vector SortEntrys(Vector list) {
        int in, out;
        int nElem = list.size();
        CmsResource[] unsortedList = new CmsResource[list.size()];
        for (int i = 0; i < list.size(); i++) {
            unsortedList[i] = (CmsResource) list.elementAt(i);
        }
        for (out = 1; out < nElem; out++) {
            CmsResource temp = unsortedList[out];
            in = out;
            while (in > 0 && unsortedList[in - 1].getName().compareTo(temp.getName()) >= 0) {
                unsortedList[in] = unsortedList[in - 1];
                --in;
            }
            unsortedList[in] = temp;
        }
        Vector sortedList = new Vector();
        for (int i = 0; i < list.size(); i++) {
            sortedList.addElement(unsortedList[i]);
        }
        return sortedList;
    }

    /**
     * Unlocks all resources in this project.
     *
     * @param project The project to be unlocked.
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public void unlockProject(CmsProject project) throws CmsException {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = m_sqlManager.getConnection(project);
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_RESOURCES_UNLOCK");
            stmt.setString(1, CmsUUID.getNullUUID().toString());
            stmt.setInt(2, project.getId());
            stmt.executeUpdate();
        } catch (Exception exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            // close all db-resources
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Update the online link table (after a project is published).<p>
     *
     * @param deleted vector (of CmsResources) with the deleted resources of the project
     * @param changed vector (of CmsResources) with the changed resources of the project
     * @param newRes vector (of CmsResources) with the newRes resources of the project
     * @param pageType the page type
     * @throws CmsException if something goes wrong
     */
    public void updateOnlineProjectLinks(Vector deleted, Vector changed, Vector newRes, int pageType) throws CmsException {
        if (deleted != null) {
            for (int i = 0; i < deleted.size(); i++) {
                // delete the old values in the online table
                if (((CmsResource) deleted.elementAt(i)).getType() == pageType) {
                    CmsUUID id = readOnlineId(((CmsResource) deleted.elementAt(i)).getName());
                    if (!id.isNullUUID()) {
                        deleteOnlineLinkEntrys(id);
                    }
                }
            }
        }
        if (changed != null) {
            for (int i = 0; i < changed.size(); i++) {
                // delete the old values and copy the new values from the project link table
                if (((CmsResource) changed.elementAt(i)).getType() == pageType) {
                    CmsUUID id = readOnlineId(((CmsResource) changed.elementAt(i)).getName());
                    if (!id.isNullUUID()) {
                        deleteOnlineLinkEntrys(id);
                        createOnlineLinkEntrys(id, readLinkEntrys(((CmsResource) changed.elementAt(i)).getResourceId()));
                    }
                }
            }
        }
        if (newRes != null) {
            for (int i = 0; i < newRes.size(); i++) {
                // copy the values from the project link table
                if (((CmsResource) newRes.elementAt(i)).getType() == pageType) {
                    CmsUUID id = readOnlineId(((CmsResource) newRes.elementAt(i)).getName());
                    if (!id.isNullUUID()) {
                        createOnlineLinkEntrys(id, readLinkEntrys(((CmsResource) newRes.elementAt(i)).getResourceId()));
                    }
                }
            }
        }
    }

    /**
     * This method updates a session in the database. It is used
     * for sessionfailover.<p>
     *
     * @param sessionId the id of the session
     * @param data the session data
     * @return the amount of data written to the database
     * @throws CmsException if something goes wrong
     */
    public int updateSession(String sessionId, Hashtable data) throws CmsException {
        byte[] value = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        int retValue;

        try {
            value = serializeSession(data);
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_SESSION_UPDATE");
            // write data to database
            stmt.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
            m_sqlManager.setBytes(stmt, 2, value);
            stmt.setString(3, sessionId);
            retValue = stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (IOException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SERIALIZATION, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
        return retValue;
    }

    /**
     * Deletes a project from the cms.
     * Therefore it deletes all files, resources and properties.
     *
     * @param project the project to delete.
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public void writeProject(CmsProject project) throws CmsException {
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_PROJECTS_WRITE");

            stmt.setString(1, project.getOwnerId().toString());
            stmt.setString(2, project.getGroupId().toString());
            stmt.setString(3, project.getManagerGroupId().toString());
            stmt.setInt(4, project.getFlags());
            // no publishing data
            stmt.setInt(7, project.getId());
            stmt.executeUpdate();
        } catch (Exception exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            // close all db-resources
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    /**
     * Writes a serializable object to the systemproperties.
     *
     * @param name The name of the property.
     * @param object The property-object.
     *
     * @return object The property-object.
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    public Serializable writeSystemProperty(String name, Serializable object) throws CmsException {
        byte[] value = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            // serialize the object
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(bout);
            oout.writeObject(object);
            oout.close();
            value = bout.toByteArray();

            conn = m_sqlManager.getConnection();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_SYSTEMPROPERTIES_UPDATE");
            m_sqlManager.setBytes(stmt, 1, value);
            stmt.setString(2, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (IOException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SERIALIZATION, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }

        return readSystemProperty(name);
    }

    /**
     * @see org.opencms.db.I_CmsProjectDriver#writePublishHistory(com.opencms.file.CmsProject, int, int, java.lang.String, com.opencms.file.CmsResource)
     */
    public void writePublishHistory(CmsProject currentProject, int publishId, int tagId, String resourcename, CmsResource resource) throws CmsException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = m_sqlManager.getConnectionForBackup();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_RESOURCES_WRITE_PUBLISH_HISTORY");
            stmt.setInt(1, tagId);
            stmt.setString(2, resource.getStructureId().toString());
            stmt.setString(3, resource.getResourceId().toString());
            stmt.setString(4, resource.getFileId().toString());
            stmt.setString(5, resourcename);
            stmt.setInt(6, resource.getState());
            stmt.setInt(7, resource.getType());
            stmt.setInt(8, publishId);
            stmt.executeUpdate();                
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, null);
        }
    }

    
    /**
     * @see org.opencms.db.I_CmsProjectDriver#nextPublishVersionId()
     */
    public int nextPublishVersionId() throws CmsException  {
        PreparedStatement stmt = null;
        Connection conn = null;
        ResultSet res = null;
        int versionId = 1;
        
        try {
            conn = m_sqlManager.getConnectionForBackup();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_RESOURCES_PUBLISH_MAXVER");
            res = stmt.executeQuery();
            
            if (res.next()) {
                versionId = res.getInt(1) + 1;
            }
        } catch (SQLException exc) {
            return 1;
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }    
        
        return versionId;
    }    
    
    /**
     * @see org.opencms.db.I_CmsProjectDriver#publishFolder(com.opencms.file.CmsRequestContext, org.opencms.report.I_CmsReport, int, int, com.opencms.file.CmsProject, com.opencms.file.CmsFolder, boolean, long, int, int, int)
     */
    public void publishFolder(CmsRequestContext context, I_CmsReport report, int m, int n, CmsProject onlineProject, CmsFolder offlineFolder, boolean backupEnabled, long publishDate, int publishHistoryId, int backupTagId, int maxVersions) throws Exception {
        CmsFolder newFolder = null;
        CmsFolder onlineFolder = null;
        Map offlineProperties = null;

        try {
            report.print("( " + m + " / " + n + " ) " + report.key("report.publishing.folder"), I_CmsReport.C_FORMAT_NOTE);
            report.println(context.removeSiteRoot(offlineFolder.getRootPath()));            
            
            if (offlineFolder.getState() == I_CmsConstants.C_STATE_NEW) {
                try {
                    // create the folder online
                    newFolder = (CmsFolder) offlineFolder.clone();
                    newFolder.setState(I_CmsConstants.C_STATE_UNCHANGED);
                    newFolder.setFullResourceName(offlineFolder.getRootPath());
                    m_driverManager.getVfsDriver().createFolder(onlineProject, newFolder, newFolder.getParentStructureId());
                } catch (CmsException e) {
                    if (e.getType() == CmsException.C_FILE_EXISTS) {
                        try {
                            onlineFolder = m_driverManager.getVfsDriver().readFolder(onlineProject.getId(), newFolder.getStructureId());
                            onlineFolder.setFullResourceName(offlineFolder.getRootPath());
                            m_driverManager.getVfsDriver().updateResource(onlineFolder, offlineFolder);
                        } catch (CmsException e1) {
                            if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                                OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFolder] error reading resource, type: " + e.getTypeText() + ",  " + offlineFolder.toString());
                            }

                            throw e1;
                        }
                    } else if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFolder] error creating resource, type: " + e.getTypeText() + ",  " + offlineFolder.toString());
                    }

                    throw e;
                }

                try {
                    // write the ACL online
                    m_driverManager.getUserDriver().publishAccessControlEntries(context.currentProject(), onlineProject, offlineFolder.getResourceId(), newFolder.getResourceId());
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFolder] error writing ACLs, type: " + e.getTypeText() + ",  " + offlineFolder.toString());
                    }
                    
                    throw e;
                }

                try {
                    // write the properties online
                    offlineProperties = m_driverManager.getVfsDriver().readProperties(context.currentProject().getId(), offlineFolder, offlineFolder.getType());
                    m_driverManager.getVfsDriver().writeProperties(offlineProperties, onlineProject.getId(), newFolder, newFolder.getType(), false);
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFolder] error writing properties, type: " + e.getTypeText() + ",  " + offlineFolder.toString());
                    }
                    
                    throw e;
                }
            } else if (offlineFolder.getState() == I_CmsConstants.C_STATE_CHANGED) {
                try {
                    // read the folder online
                    onlineFolder = m_driverManager.getVfsDriver().readFolder(onlineProject.getId(), offlineFolder.getStructureId());
                    onlineFolder.setFullResourceName(offlineFolder.getRootPath());
                } catch (CmsException e) {
                    if (e.getType() == CmsException.C_NOT_FOUND) {
                        try {
                            onlineFolder = m_driverManager.getVfsDriver().createFolder(onlineProject, offlineFolder, offlineFolder.getParentStructureId());
                            onlineFolder.setState(I_CmsConstants.C_STATE_UNCHANGED);
                            onlineFolder.setFullResourceName(offlineFolder.getRootPath());
                            m_driverManager.getVfsDriver().updateResourceState(context.currentProject(), onlineFolder, CmsDriverManager.C_UPDATE_ALL);
                        } catch (CmsException e1) {
                            if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                                OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFolder] error creating resource, type: " + e.getTypeText() + ",  " + offlineFolder.toString());
                            }

                            throw e1;
                        }
                    } else if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFolder] error reading resource, type: " + e.getTypeText() + ",  " + offlineFolder.toString());
                    }

                    throw e;
                }

                try {
                    // update the folder online
                    m_driverManager.getVfsDriver().updateResource(onlineFolder, offlineFolder);
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFolder] error updating resource, type: " + e.getTypeText() + ",  " + offlineFolder.toString());
                    }
                    
                    throw e;
                }

                try {
                    // write the ACL online
                    m_driverManager.getUserDriver().publishAccessControlEntries(context.currentProject(), onlineProject, offlineFolder.getResourceId(), onlineFolder.getResourceId());
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFolder] error writing ACLs, type: " + e.getTypeText() + ",  " + offlineFolder.toString());
                    }
                    
                    throw e;
                }

                try {
                    // write the properties online
                    m_driverManager.getVfsDriver().deleteAllProperties(onlineProject.getId(), onlineFolder);
                    offlineProperties = m_driverManager.getVfsDriver().readProperties(context.currentProject().getId(), offlineFolder, offlineFolder.getType());
                    m_driverManager.getVfsDriver().writeProperties(offlineProperties, onlineProject.getId(), onlineFolder, offlineFolder.getType(), false);
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFolder] error writing properties, type: " + e.getTypeText() + ",  " + offlineFolder.toString());
                    }
                    
                    throw e;
                }
            }

            try {
                // write the folder to the backup and publishing history
                if (backupEnabled) {
                    m_driverManager.getBackupDriver().writeBackupResource(context.currentUser(), context.currentProject(), offlineFolder, offlineProperties, backupTagId, publishDate, maxVersions);
                }
                writePublishHistory(context.currentProject(), publishHistoryId, backupTagId, offlineFolder.getRootPath(), offlineFolder);
            } catch (CmsException e) {
                if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                    OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFolder] error writing backup/publishing history, type: " + e.getTypeText() + ",  " + offlineFolder.toString());
                }
                
                throw e;
            }

            try {
                // reset the resource state and the last-modified-in-project ID offline
                if (offlineFolder.getState() != I_CmsConstants.C_STATE_UNCHANGED) {
                    offlineFolder.setState(I_CmsConstants.C_STATE_UNCHANGED);
                    m_driverManager.getVfsDriver().updateResourceState(context.currentProject(), offlineFolder, CmsDriverManager.C_UPDATE_ALL);
                }

                m_driverManager.getVfsDriver().resetProjectId(context.currentProject(), offlineFolder);
            } catch (CmsException e) {
                if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                    OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFolder] error reseting resource state, type: " + e.getTypeText() + ",  " + offlineFolder.toString());
                }
                
                throw e;
            }          
        } catch (Exception e) {    
            // this is a dummy try-catch block to have a finally clause here
                                
            if (C_DEBUG) {
                System.err.println(e.getMessage());
                e.printStackTrace(System.err);
            }   
                     
            throw e;
        } finally {
            // notify the app. that the published folder and it's properties have been modified offline
            OpenCms.fireCmsEvent(new CmsEvent(new CmsObject(), I_CmsEventListener.EVENT_PROPERTIES_MODIFIED, Collections.singletonMap("resource", offlineFolder)));
        }
    }
    
    /**
     * @see org.opencms.db.I_CmsProjectDriver#publishDeletedFolder(com.opencms.file.CmsRequestContext, org.opencms.report.I_CmsReport, int, int, com.opencms.file.CmsProject, com.opencms.file.CmsFolder, boolean, long, int, int, int)
     */
    public void publishDeletedFolder(CmsRequestContext context, I_CmsReport report, int m, int n, CmsProject onlineProject, CmsFolder currentFolder, boolean backupEnabled, long publishDate, int publishHistoryId, int backupTagId, int maxVersions) throws Exception {
        CmsFolder onlineFolder = null;
        Map offlineProperties = null;
        
        try {
            report.print("( " + m + " / " + n + " ) " + report.key("report.deleting.folder"), I_CmsReport.C_FORMAT_NOTE);
            report.println(context.removeSiteRoot(currentFolder.getRootPath()));
            
            try {
                // write the folder to the backup and publishing history
                if (backupEnabled) {
                    offlineProperties = m_driverManager.getVfsDriver().readProperties(context.currentProject().getId(), currentFolder, currentFolder.getType());
                    m_driverManager.getBackupDriver().writeBackupResource(context.currentUser(), context.currentProject(), currentFolder, offlineProperties, backupTagId, publishDate, maxVersions);
                }   
                writePublishHistory(context.currentProject(), publishHistoryId, backupTagId, currentFolder.getRootPath(), currentFolder);             
            } catch (CmsException e) {
                if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                    OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishDeletedFolder] error writing backup/publishing history, type: " + e.getTypeText() + ",  " + currentFolder.toString());
                }
                
                throw e;
            }   
            
            try {
                // read the folder online
                onlineFolder = m_driverManager.readFolder(context, currentFolder.getStructureId(), true);
                onlineFolder.setFullResourceName(currentFolder.getRootPath());                
            } catch (CmsException e) {
                if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                    OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishDeletedFolder] error reading resource, type: " + e.getTypeText() + ",  " + currentFolder.toString());
                }
                
                throw e;
            }   
            
            try {
                // delete the properties online and offline
                m_driverManager.getVfsDriver().deleteAllProperties(onlineProject.getId(), onlineFolder);
                m_driverManager.getVfsDriver().deleteAllProperties(context.currentProject().getId(), currentFolder);
            } catch (CmsException e) {
                if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                    OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishDeletedFolder] error deleting properties, type: " + e.getTypeText() + ",  " + currentFolder.toString());
                }
                
                throw e;
            }
            
            try {
                // remove the folder online and offline
                m_driverManager.getVfsDriver().removeFolder(onlineProject, currentFolder);
                m_driverManager.getVfsDriver().removeFolder(context.currentProject(), currentFolder);                
            } catch (CmsException e) {
                if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                    OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishDeletedFolder] error removing resource, type: " + e.getTypeText() + ",  " + currentFolder.toString());
                }

                throw e;                
            }
            
            try {
                // remove the ACL online and offline
                m_driverManager.getUserDriver().removeAllAccessControlEntries(onlineProject, onlineFolder.getResourceId());
                m_driverManager.getUserDriver().removeAllAccessControlEntries(context.currentProject(), currentFolder.getResourceId());                
            } catch (CmsException e) {
                if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                    OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishDeletedFolder] error removing ACLs, type: " + e.getTypeText() + ",  " + currentFolder.toString());
                }

                throw e;                 
            }             
        } catch (Exception e) {  
            // this is a dummy try-catch block to have a finally clause here
                                  
            if (C_DEBUG) {
                System.err.println(e.getMessage());
                e.printStackTrace(System.err);
            }   
                     
            throw e;
        } finally {
            // notify the app. that the published folder and it's properties have been modified offline
            OpenCms.fireCmsEvent(new CmsEvent(new CmsObject(), I_CmsEventListener.EVENT_PROPERTIES_MODIFIED, Collections.singletonMap("resource", currentFolder)));
        }
    }

    /**
     * @see org.opencms.db.I_CmsProjectDriver#publishFile(com.opencms.file.CmsRequestContext, org.opencms.report.I_CmsReport, int, int, com.opencms.file.CmsProject, com.opencms.file.CmsResource, boolean, long, int, int, int)
     */
    public void publishFile(CmsRequestContext context, I_CmsReport report, int m, int n, CmsProject onlineProject, CmsResource offlineFileHeader, boolean backupEnabled, long publishDate, int publishHistoryId, int backupTagId, int maxVersions) throws Exception {
        CmsFile onlineFile = null;
        CmsFile offlineFile = null;
        CmsFile newFile = null;
        Map offlineProperties = null;
        
        // TODO the file(-content) should be read only once while it is published
        
        /*
         * Never use offlineFile.getState() here!
         * Only use offlineFileHeader.getState() to determine the state of a resource!
         * 
         * In case a resource has siblings, after a sibling was published the structure
         * and resource states are reset to UNCHANGED -> the state of the corresponding
         * offlineFileHeader is still NEW, DELETED or CHANGED, but the state of offlineFile 
         * is UNCHANGED because offlineFile is read AFTER siblings already got published. 
         * Thus, using offlineFile.getState() will inevitably result in unpublished resources!
         */        

        try {
            offlineFile = m_driverManager.getVfsDriver().readFile(context.currentProject().getId(), true, offlineFileHeader.getStructureId());
            offlineFile.setFullResourceName(offlineFileHeader.getRootPath());

            if (offlineFileHeader.getState() == I_CmsConstants.C_STATE_DELETED) {
                report.print("( " + m + " / " + n + " ) " + report.key("report.deleting.file"), I_CmsReport.C_FORMAT_NOTE);
                report.println(context.removeSiteRoot(offlineFileHeader.getRootPath()));

                try {
                    // read the file online
                    onlineFile = m_driverManager.getVfsDriver().readFile(onlineProject.getId(), true, offlineFile.getStructureId());
                    onlineFile.setFullResourceName(offlineFileHeader.getRootPath());
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFile] error reading resource, type: " + e.getTypeText() + ",  " + offlineFile.toString());
                    }

                    throw e;
                }

                if (offlineFile.isLabeled() && !m_driverManager.hasLabeledLinks(context, context.currentProject(), offlineFile)) {
                    // update the resource flags to "unlabel" of the siblings of the offline resource
                    int flags = offlineFile.getFlags();
                    flags &= ~I_CmsConstants.C_RESOURCEFLAG_LABELLINK;
                    offlineFile.setFlags(flags);
                }

                if (onlineFile.isLabeled() && !m_driverManager.hasLabeledLinks(context, onlineProject, onlineFile)) {
                    // update the resource flags to "unlabel" of the siblings of the online resource
                    int flags = onlineFile.getFlags();
                    flags &= ~I_CmsConstants.C_RESOURCEFLAG_LABELLINK;
                    onlineFile.setFlags(flags);
                }

                try {
                    // delete the properties online and offline
                    m_driverManager.getVfsDriver().deleteAllProperties(onlineProject.getId(), onlineFile);
                    m_driverManager.getVfsDriver().deleteAllProperties(context.currentProject().getId(), offlineFile);

                    // if the offline file has a resource ID different from the online file
                    // (probably because a (deleted) file was replaced by a new file with the
                    // same name), the properties with the "old" resource ID have to be
                    // deleted also offline
                    if (!onlineFile.getResourceId().equals(offlineFile.getResourceId())) {
                        m_driverManager.getVfsDriver().deleteAllProperties(context.currentProject().getId(), onlineFile);
                    }
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFile] error deleting properties, type: " + e.getTypeText() + ",  " + offlineFile.toString());
                    }

                    throw e;
                }

                try {
                    // remove the file online and offline
                    m_driverManager.getVfsDriver().removeFile(onlineProject, onlineFile);
                    m_driverManager.getVfsDriver().removeFile(context.currentProject(), offlineFile);
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFile] error removing resource, type: " + e.getTypeText() + ",  " + offlineFile.toString());
                    }

                    throw e;
                }

                try {
                    // delete the ACL online and offline
                    m_driverManager.getUserDriver().removeAllAccessControlEntries(onlineProject, onlineFile.getResourceId());
                    m_driverManager.getUserDriver().removeAllAccessControlEntries(context.currentProject(), offlineFile.getResourceId());
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFile] error removing ACLs, type: " + e.getTypeText() + ",  " + offlineFile.toString());
                    }

                    throw e;
                }

                try {
                    // write the file to the backup and publishing history
                    if (backupEnabled) {
                        offlineProperties = m_driverManager.getVfsDriver().readProperties(context.currentProject().getId(), offlineFile, offlineFile.getType());
                        m_driverManager.getBackupDriver().writeBackupResource(context.currentUser(), context.currentProject(), offlineFile, offlineProperties, backupTagId, publishDate, maxVersions);
                    }
                    writePublishHistory(context.currentProject(), publishHistoryId, backupTagId, offlineFileHeader.getRootPath(), offlineFile);
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFile] error writing backup/publishing history, type: " + e.getTypeText() + ",  " + offlineFile.toString());
                    }
                    
                    throw e;
                }
            } else if (offlineFileHeader.getState() == I_CmsConstants.C_STATE_CHANGED) {
                report.print("( " + m + " / " + n + " ) " + report.key("report.publishing.file"), I_CmsReport.C_FORMAT_NOTE);
                report.println(context.removeSiteRoot(offlineFileHeader.getRootPath()));
                
                try {
                    // read the file online
                    onlineFile = m_driverManager.getVfsDriver().readFile(onlineProject.getId(), true, offlineFile.getStructureId());
                    onlineFile.setFullResourceName(offlineFileHeader.getRootPath());     
                        
                    // delete the properties online
                    m_driverManager.getVfsDriver().deleteAllProperties(onlineProject.getId(), onlineFile);
                        
                    // if the offline file has a resource ID different from the online file
                    // (probably because a (deleted) file was replaced by a new file with the
                    // same name), the properties with the "old" resource ID have to be
                    // deleted also offline
                    if (!onlineFile.getResourceId().equals(offlineFile.getResourceId())) {
                        m_driverManager.getVfsDriver().deleteAllProperties(context.currentProject().getId(), onlineFile);                                                                       
                    }
                        
                    // remove the file online
                    m_driverManager.getVfsDriver().removeFile(onlineProject, onlineFile);
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFile] error deleting properties, type: " + e.getTypeText() + ",  " + offlineFile.toString());
                    }
                    
                    throw e;
                }
                     
                try {           
                    // create the file online              
                    newFile = (CmsFile) offlineFile.clone();
                    newFile.setState(I_CmsConstants.C_STATE_UNCHANGED);
                    newFile.setFullResourceName(offlineFileHeader.getRootPath());
                    
                    m_driverManager.getVfsDriver().createFile(onlineProject, newFile, context.currentUser().getId(), newFile.getParentStructureId(), newFile.getName());
                    m_driverManager.getVfsDriver().updateResource(newFile,offlineFile);
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFile] error creating resource, type: " + e.getTypeText() + ",  " + newFile.toString());
                    }
                    
                    throw e;
                }          

                try {
                    // write the properties online
                    offlineProperties = m_driverManager.getVfsDriver().readProperties(context.currentProject().getId(), offlineFile, offlineFile.getType());
                    m_driverManager.getVfsDriver().writeProperties(offlineProperties, onlineProject.getId(), newFile, newFile.getType(), false);
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFile] error writing properties, type: " + e.getTypeText() + ",  " + newFile.toString());
                    }
                    
                    throw e;
                }

                try {
                    // write the ACL online
                    m_driverManager.getUserDriver().publishAccessControlEntries(context.currentProject(), onlineProject, newFile.getResourceId(), onlineFile.getResourceId());
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFile] error writing ACLs, type: " + e.getTypeText() + ",  " + offlineFile.toString());
                    }
                    
                    throw e;
                }
                
                try {
                    // write the file to the backup and publishing history
                    if (backupEnabled) {
                        m_driverManager.getBackupDriver().writeBackupResource(context.currentUser(), context.currentProject(), offlineFile, offlineProperties, backupTagId, publishDate, maxVersions);
                    }
                    m_driverManager.getBackupDriver().writeBackupResource(context.currentUser(), context.currentProject(), offlineFile, offlineProperties, backupTagId, publishDate, maxVersions);
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFile] error writing backup/publishing history, type: " + e.getTypeText() + ",  " + offlineFile.toString());
                    }
                    
                    throw e;
                }     
                
                try {
                    // reset the resource state and the last-modified-in-project ID offline
                    if (offlineFileHeader.getState() != I_CmsConstants.C_STATE_UNCHANGED) {
                        offlineFile.setState(I_CmsConstants.C_STATE_UNCHANGED);
                        m_driverManager.getVfsDriver().updateResourceState(context.currentProject(), offlineFile, CmsDriverManager.C_UPDATE_ALL);
                    }
                    m_driverManager.getVfsDriver().resetProjectId(context.currentProject(), offlineFile);
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFile] error reseting resource state, type: " + e.getTypeText() + ",  " + offlineFile.toString());
                    }
                    
                    throw e;
                } 
            } else if (offlineFileHeader.getState() == I_CmsConstants.C_STATE_NEW) {
                report.print("( " + m + " / " + n + " ) " + report.key("report.publishing.file"), I_CmsReport.C_FORMAT_NOTE);
                report.println(context.removeSiteRoot(offlineFileHeader.getRootPath()));
                
                try {
                    // create the file online
                    newFile = (CmsFile) offlineFile.clone();
                    newFile.setState(I_CmsConstants.C_STATE_UNCHANGED);
                    newFile.setFullResourceName(offlineFileHeader.getRootPath());
                    m_driverManager.getVfsDriver().createFile(onlineProject, newFile, context.currentUser().getId(), newFile.getParentStructureId(), newFile.getName());
                } catch (CmsException e) {
                    if (e.getType() == CmsException.C_FILE_EXISTS) {
                        try {
                            m_driverManager.getVfsDriver().removeFile(onlineProject, offlineFile);
                            m_driverManager.getVfsDriver().createFile(onlineProject, newFile, context.currentUser().getId(), newFile.getParentStructureId(), newFile.getName());
                        } catch (CmsException e1) {
                            if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                                OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFile] error creating resource, type: " + e.getTypeText() + ",  " + offlineFile.toString());
                            }

                            throw e1;
                        }
                    } else if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFile] error creating resource, type: " + e.getTypeText() + ",  " + offlineFile.toString());
                    }

                    throw e;
                }   
                
                try {
                    // write the properties online
                    offlineProperties = m_driverManager.getVfsDriver().readProperties(context.currentProject().getId(), offlineFile, offlineFile.getType());
                    m_driverManager.getVfsDriver().writeProperties(offlineProperties, onlineProject.getId(), newFile, newFile.getType(), false);
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFile] error writing properties, type: " + e.getTypeText() + ",  " + newFile.toString());
                    }
                    
                    throw e;
                }
                
                try {
                    // write the ACL online
                    m_driverManager.getUserDriver().publishAccessControlEntries(context.currentProject(), onlineProject, offlineFile.getResourceId(), newFile.getResourceId());
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFile] error writing ACLs, type: " + e.getTypeText() + ",  " + newFile.toString());
                    }
                    
                    throw e;                    
                }
                
                try {
                    // write the file to the backup and publishing history
                    if (backupEnabled) {
                        m_driverManager.getBackupDriver().writeBackupResource(context.currentUser(), context.currentProject(), offlineFile, offlineProperties, backupTagId, publishDate, maxVersions);
                    }
                    writePublishHistory(context.currentProject(), publishHistoryId, backupTagId, offlineFileHeader.getRootPath(), offlineFile);
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFile] error writing backup/publishing history, type: " + e.getTypeText() + ",  " + newFile.toString());
                    }
                    
                    throw e;                    
                }
                
                try {
                    // reset the resource state and the last-modified-in-project ID offline
                    if (offlineFileHeader.getState() != I_CmsConstants.C_STATE_UNCHANGED) {
                        offlineFile.setState(I_CmsConstants.C_STATE_UNCHANGED);
                        m_driverManager.getVfsDriver().updateResourceState(context.currentProject(), offlineFile, CmsDriverManager.C_UPDATE_ALL);
                    }
                    m_driverManager.getVfsDriver().resetProjectId(context.currentProject(), offlineFile);
                } catch (CmsException e) {
                    if (OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) {
                        OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + this.getClass().getName() + ".publishFile] error reseting resource state, type: " + e.getTypeText() + ",  " + offlineFile.toString());
                    }
                    
                    throw e;                    
                }
            }
        } catch (Exception e) {     
            // this is a dummy try-catch block to have a finally clause here 
                              
            if (C_DEBUG) {
                System.err.println(e.getMessage());
                e.printStackTrace(System.err);
            }  
                      
            throw e;
        } finally {
            // notify the app. that the published file and it's properties have been modified offline
            OpenCms.fireCmsEvent(new CmsEvent(new CmsObject(), I_CmsEventListener.EVENT_PROPERTIES_MODIFIED, Collections.singletonMap("resource", offlineFileHeader)));
        }
    }    

}
