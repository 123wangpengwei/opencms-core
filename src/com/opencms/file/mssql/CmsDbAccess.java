/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/mssql/Attic/CmsDbAccess.java,v $
* Date   : $Date: 2001/11/14 10:13:25 $
* Version: $Revision: 1.1 $
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

package com.opencms.file.mssql;


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



/**
 * This is the mssql access module to load and store resources from and into
 * the database.
 *
 * @author Edna Falkenhan
 * @version $Revision: 1.1 $ $Date: 2001/11/14 10:13:25 $ *
 */
public class CmsDbAccess extends com.opencms.file.genericSql.CmsDbAccess implements I_CmsConstants, I_CmsLogChannels {
    /**
     * Instanciates the access-module and sets up all required modules and connections.
     * @param config The OpenCms configuration.
     * @exception CmsException Throws CmsException if something goes wrong.
     */
    public CmsDbAccess(Configurations config)
        throws CmsException {

        super(config);
    }

    /**
     * Destroys this access-module
     * @exception throws CmsException if something goes wrong.
     */
    public void destroy() throws CmsException {
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
     * Returns all projects from the history.
     * For MS SQL Server the max. number of rows must be specified in the statement
     * and cannot be set by parameter
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
             res = statement.executeQuery();
             while(res.next()) {
                int versionId = res.getInt("VERSION_ID");
                Vector resources = new Vector();
                try{
                    resources = readBackupProjectResources(versionId);
                } catch (CmsException ex){
                    // no resources do nothing
                }
                 projects.addElement( new CmsBackupProject(versionId,
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
 * retrieve the correct instance of the queries holder.
 * This method should be overloaded if other query strings should be used.
 */
protected com.opencms.file.genericSql.CmsQueries getQueries()
{
    return new com.opencms.file.mssql.CmsQueries();
}
}
