/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/Attic/I_CmsWorkflowDriver.java,v $
 * Date   : $Date: 2003/08/20 13:16:17 $
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

package org.opencms.db;


import com.opencms.core.CmsException;
import com.opencms.file.CmsGroup;
import com.opencms.file.CmsProject;
import com.opencms.file.CmsTask;
import com.opencms.file.CmsTaskLog;
import com.opencms.file.CmsUser;
import com.opencms.flex.util.CmsUUID;

import java.util.Vector;

/**
 * Definitions of all required workflow driver methods.
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @version $Revision: 1.5 $ $Date: 2003/08/20 13:16:17 $
 * @since 5.1
 */
public interface I_CmsWorkflowDriver {

    /**
     * Destroys this driver.<p>
     * 
     * @throws Throwable if something goes wrong
     * @throws CmsException if something else goes wrong
     */  
    void destroy() throws Throwable, CmsException;

    /**
     * Creates a new task.<p>
     * 
     * @param rootId id of the root task project
     * @param parentId id of the parent task
     * @param tasktype type of the task
     * @param ownerId id of the owner
     * @param agentId id of the agent
     * @param roleId id of the role
     * @param taskname name of the task
     * @param wakeuptime time when the task will be wake up
     * @param timeout time when the task times out
     * @param priority priority of the task
     *
     * @return the Task object of the generated task
     *
     * @throws CmsException if something goes wrong.
     */    
    CmsTask createTask(int rootId, int parentId, int tasktype, CmsUUID ownerId, CmsUUID agentId, CmsUUID roleId, String taskname, java.sql.Timestamp wakeuptime, java.sql.Timestamp timeout, int priority) throws CmsException;

    /**
     * Finds an agent for a given role (group).
     * @param roleId The Id for the role (group).
     *
     * @return A vector with the tasks
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    CmsUUID findAgent(CmsUUID roleId) throws CmsException;

    /**
     * Get a parameter value for a task.<p>
     *
     * @param taskId the id of the task
     * @param parname Name of the parameter
     * @return the parameter value
     * @throws CmsException if something goes wrong
     */
    String getTaskPar(int taskId, String parname) throws CmsException;

    /**
     * Get the template task id fo a given taskname.
     *
     * @param taskName Name of the Task
     *
     * @return id from the task template
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    int getTaskType(String taskName) throws CmsException;

    /**
     * Initializes the workflow driver with the given configuration.<p>
     * 
     * @param config the configuration
     * @param dbPoolUrl the url of the connection pool to use
     * @param driverManager the driver manager
     */
    // void init(Configurations config, String dbPoolUrl, CmsDriverManager driverManager);
    
    /**
     * Initializes the SQL manager for this package.<p>
     * 
     * @param dbPoolUrl the URL of the connection pool
     * @return the SQL manager for this package
     */     
    I_CmsSqlManager initQueries(String dbPoolUrl);
    
    
    /**
     * Reads a task.<p>
     *
     * @param id the id of the task to read
     * @return a task object or null if the task is not found
     * @throws CmsException if something goes wrong
     */    
    CmsTask readTask(int id) throws CmsException;
    
    /**
     * Reads a log for a task.<p>
     *
     * @param id The id for the tasklog .
     * @return a new TaskLog object
     * @throws CmsException Throws CmsException if something goes wrong.
     */    
    CmsTaskLog readTaskLog(int id) throws CmsException;
    
    /**
     * Reads log entries for a task.<p>
     *
     * @param taskId the ID of the task for the tasklog to read
     * @return A Vector of new TaskLog objects
     * @throws CmsException Throws CmsException if something goes wrong.
     */    
    Vector readTaskLogs(int taskId) throws CmsException;
    
    /**
     * Reads all tasks of a user in a project.<p>
     * 
     * @param project the Project in which the tasks are defined
     * @param agent the task agent
     * @param owner the task owner
     * @param role the task role
     * @param tasktype one of C_TASKS_ALL, C_TASKS_OPEN, C_TASKS_DONE, C_TASKS_NEW
     * @param orderBy selects filter how to order the tasks
     * @param sort select to sort ascending or descending ("ASC" or "DESC")
     * @return a vector with the tasks read
     * @throws CmsException if something goes wrong.
     */    
    Vector readTasks(CmsProject project, CmsUser agent, CmsUser owner, CmsGroup role, int tasktype, String orderBy, String sort) throws CmsException;

    /**
     * Set a Parameter for a task.<p>
     *
     * @param taskId the task
     * @param parname the name of the parameter
     * @param parvalue the value of the parameter
     *
     * @return The id of the inserted parameter or 0 if the parameter exists for this task.
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    int setTaskPar(int taskId, String parname, String parvalue) throws CmsException;

    /**
     * Writes a system task log entry.<p>
     * 
     * @param taskid the id of the task
     * @param comment the log entry
     * @throws CmsException if something goes wrong
     */
    void writeSystemTaskLog(int taskid, String comment) throws CmsException;
    
    /**
     * Writes a task.<p>
     *
     * @param task the task to write
     * @return written task object
     * @throws CmsException if something goes wrong
     */    
    CmsTask writeTask(CmsTask task) throws CmsException;

    /**
     * Writes new log for a task.<p>
     *
     * @param taskId The id of the task.
     * @param userId User who added the Log.
     * @param starttime Time when the log is created.
     * @param comment Description for the log.
     * @param type Type of the log. 0 = Sytem log, 1 = User Log
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */    
    void writeTaskLog(int taskId, CmsUUID userId, java.sql.Timestamp starttime, String comment, int type) throws CmsException;

    /**
     * Creates a new tasktype set in the database.<p>
     * 
     * @param autofinish tbd
     * @param escalationtyperef tbd
     * @param htmllink tbd
     * @param name tbd
     * @param permission tbd
     * @param priorityref tbd
     * @param roleref tbd
     * @return The id of the inserted parameter or 0 if the parameter exists for this task.
     *
     * @throws CmsException Throws CmsException if something goes wrong.
     */
    int writeTaskType(int autofinish, int escalationtyperef, String htmllink, String name, String permission, int priorityref, int roleref) throws CmsException;
}