/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/db/mysql/Attic/CmsBackupDriver.java,v $
 * Date   : $Date: 2003/06/03 17:45:46 $
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
 
package com.opencms.db.mysql;

import com.opencms.core.CmsException;
import com.opencms.file.CmsBackupProject;
import com.opencms.flex.util.CmsUUID;
import com.opencms.util.SqlHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

/**
 * MySQL implementation of the backup driver methods.
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @version $Revision: 1.2 $ $Date: 2003/06/03 17:45:46 $
 */
public class CmsBackupDriver extends com.opencms.db.generic.CmsBackupDriver {
    
    /**
     * Returns all projects from the history.
     *
     * @return a Vector of projects.
     */
     public Vector getAllBackupProjects() throws CmsException {
         Vector projects = new Vector();
         ResultSet res = null;
         PreparedStatement stmt = null;
         Connection conn = null;
    
         try {
             // create the statement
             conn = m_sqlManager.getConnectionForBackup();
             stmt = m_sqlManager.getPreparedStatement(conn, "C_PROJECTS_READLAST_BACKUP");
             stmt.setInt(1, 300);
             res = stmt.executeQuery();
             while(res.next()) {
                 Vector resources = m_driverManager.getVfsDriver().readBackupProjectResources(res.getInt("VERSION_ID"));
                 projects.addElement( new CmsBackupProject(res.getInt("VERSION_ID"),
                                                    res.getInt("PROJECT_ID"),
                                                    res.getString("PROJECT_NAME"),
                                                    SqlHelper.getTimestamp(res,"PROJECT_PUBLISHDATE"),
                                                    new CmsUUID(res.getString("PROJECT_PUBLISHED_BY")),
                                                    res.getString("PROJECT_PUBLISHED_BY_NAME"),
                                                    res.getString("PROJECT_DESCRIPTION"),
                                                    res.getInt("TASK_ID"),
                                                    new CmsUUID(res.getString("USER_ID")),
                                                    res.getString("USER_NAME"),
                                                    new CmsUUID(res.getString("GROUP_ID")),
                                                    res.getString("GROUP_NAME"),
                                                    new CmsUUID(res.getString("MANAGERGROUP_ID")),
                                                    res.getString("MANAGERGROUP_NAME"),
                                                    SqlHelper.getTimestamp(res,"PROJECT_CREATEDATE"),
                                                    res.getInt("PROJECT_TYPE"),
                                                    resources));
             }
         } catch( SQLException exc ) {
             throw m_sqlManager.getCmsException(this, "getAllBackupProjects()", CmsException.C_SQL_ERROR, exc, false);
         } finally {
            // close all db-resources
            m_sqlManager.closeAll(conn, stmt, res);
         }
         return(projects);
     }
    
    public com.opencms.db.generic.CmsSqlManager initQueries(String dbPoolUrl) {
        return new com.opencms.db.mysql.CmsSqlManager(dbPoolUrl);
    }    

}
