/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/oracle/CmsBackupDriver.java,v $
 * Date   : $Date: 2004/11/29 14:03:03 $
 * Version: $Revision: 1.42 $
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

package org.opencms.db.oracle;

import org.opencms.db.CmsDbContext;
import org.opencms.db.CmsDbUtil;
import org.opencms.db.generic.CmsSqlManager;
import org.opencms.file.CmsBackupProject;
import org.opencms.file.CmsBackupResource;
import org.opencms.file.CmsFile;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsResource;
import org.opencms.main.CmsException;
import org.opencms.util.CmsUUID;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import org.apache.commons.dbcp.DelegatingResultSet;

/**
 * Oracle implementation of the backup driver methods.<p>
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @author Michael Emmerich (m.emmerich@alkacon.com) 
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 * @version $Revision: 1.42 $ $Date: 2004/11/29 14:03:03 $
 * @since 5.1
 */
public class CmsBackupDriver extends org.opencms.db.generic.CmsBackupDriver {
    
    /**
     * @see org.opencms.db.I_CmsBackupDriver#deleteBackups(org.opencms.db.CmsDbContext, java.util.List, int)
     */
    public void deleteBackups(CmsDbContext dbc, List existingBackups, int maxVersions) throws CmsException {
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        PreparedStatement stmt3 = null;
        PreparedStatement stmt4 = null;

        Connection conn = null;
        CmsBackupResource currentResource = null;
        int count = existingBackups.size() - maxVersions;

        try {
            conn = m_sqlManager.getConnection(dbc);
            stmt1 = m_sqlManager.getPreparedStatement(conn, "C_ORACLE_BACKUP_DELETE_CONTENT");
            stmt2 = m_sqlManager.getPreparedStatement(conn, "C_ORACLE_BACKUP_DELETE_RESOURCES");
            stmt3 = m_sqlManager.getPreparedStatement(conn, "C_ORACLE_BACKUP_DELETE_STRUCTURE");
            stmt4 = m_sqlManager.getPreparedStatement(conn, "C_PROPERTIES_DELETEALL_BACKUP");

            for (int i = 0; i < count; i++) {
                currentResource = (CmsBackupResource)existingBackups.get(i);
                // add the values to delete the file table
                stmt1.setString(1, currentResource.getBackupId().toString());
                stmt1.addBatch();
                // add the values to delete the resource table
                stmt2.setString(1, currentResource.getBackupId().toString());
                stmt2.addBatch();
                // add the values to delete the structure table
                stmt3.setString(1, currentResource.getBackupId().toString());
                stmt3.addBatch();
                // delete the properties
                stmt4.setString(1, currentResource.getBackupId().toString());
                stmt4.setInt(2, currentResource.getTagId());
                stmt4.setString(3, currentResource.getStructureId().toString());
                stmt4.setInt(4, CmsProperty.C_STRUCTURE_RECORD_MAPPING);
                stmt4.setString(5, currentResource.getResourceId().toString());
                stmt4.setInt(6, CmsProperty.C_RESOURCE_RECORD_MAPPING);
                stmt4.addBatch();
            }

            if (count > 0) {
                stmt1.executeBatch();
                stmt2.executeBatch();
                stmt3.executeBatch();
                stmt4.executeBatch();
            }

        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception ex) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, ex, false);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt1, null);
            m_sqlManager.closeAll(dbc, conn, stmt2, null);
            m_sqlManager.closeAll(dbc, conn, stmt3, null);
            m_sqlManager.closeAll(dbc, conn, stmt4, null);
        }
    }
    
    /**
     * @see org.opencms.db.I_CmsBackupDriver#initSqlManager(String)
     */
    public org.opencms.db.generic.CmsSqlManager initSqlManager(String classname) {

        return CmsSqlManager.getInstance(classname);
    }

    /**
     * @see org.opencms.db.generic.CmsBackupDriver#internalWriteBackupFileContent(org.opencms.db.CmsDbContext, org.opencms.util.CmsUUID, org.opencms.file.CmsResource, int, int)
     */
    protected void internalWriteBackupFileContent(CmsDbContext dbc, CmsUUID backupId, CmsResource resource, int tagId, int versionId) throws CmsException {
                      
        PreparedStatement stmt = null, stmt2 = null;
        PreparedStatement commit = null;
        PreparedStatement rollback = null;
        Connection conn = null;
        ResultSet res = null;
        
        CmsUUID contentId;
        byte[] fileContent;
        if (resource instanceof CmsFile) {
            contentId = ((CmsFile)resource).getContentId();
            fileContent = ((CmsFile)resource).getContents();
        } else {
            contentId = CmsUUID.getNullUUID();
            fileContent = new byte[0];
        }
        
        try {
            conn = m_sqlManager.getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_ORACLE_CONTENTS_ADDBACKUP");

            // first insert new file without file_content, then update the file_content
            // these two steps are necessary because of using BLOBs in the Oracle DB
            stmt.setString(1, contentId.toString());
            stmt.setString(2, resource.getResourceId().toString());
            stmt.setInt(3, tagId);
            stmt.setInt(4, versionId);
            stmt.setString(5, backupId.toString());
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, "internalWriteBackupFileContent backupId=" + backupId.toString() + " contentId=" + contentId.toString(), CmsException.C_SQL_ERROR, e, false);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }
        
        try {
            conn = m_sqlManager.getConnection(dbc);
            
            if (dbc.isDefaultDbContext()) {
                conn.setAutoCommit(false);
            }
            
            // select the backup record for update            
            stmt = m_sqlManager.getPreparedStatement(conn, "C_ORACLE_CONTENTS_UPDATEBACKUP");
            stmt.setString(1, contentId.toString());
            stmt.setString(2, backupId.toString());
            
            res = ((DelegatingResultSet)stmt.executeQuery()).getInnermostDelegate();
            if (!res.next()) {
                throw new CmsException("internalWriteBackupFileContent backupId=" + backupId.toString() + " contentId=" + contentId.toString() + " content not found", CmsException.C_NOT_FOUND);
            }
        
            // write file content
            OutputStream output = CmsUserDriver.getOutputStreamFromBlob(res, "FILE_CONTENT");
            output.write(fileContent);
            output.close();
            res.close();
            res = null;
            fileContent = null;
            
            if (dbc.isDefaultDbContext()) {
                commit = m_sqlManager.getPreparedStatement(conn, "C_COMMIT");
                commit.execute();
                commit.close();
                commit = null;
            }
                        
            stmt.close();
            stmt = null;

            if (dbc.isDefaultDbContext()) {
                conn.setAutoCommit(true);    
            }
        } catch (IOException e) {
            throw m_sqlManager.getCmsException(this, "internalWriteBackupFileContent backupId=" + backupId.toString() + " contentId=" + contentId.toString(), CmsException.C_SERIALIZATION, e, false);
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, "internalWriteBackupFileContent backupId=" + backupId.toString() + " contentId=" + contentId.toString(), CmsException.C_SQL_ERROR, e, false);
        } finally {
            
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException exc) {
                    // ignore
                }                
            } 
            if (commit != null) {
                try {
                    commit.close();
                } catch (SQLException exc) {
                    // ignore
                }
            } 
            if (stmt2 != null) {
                try {
                    stmt2.close();
                } catch (SQLException exc) {
                    // ignore
                }
            }
            
            if (dbc.isDefaultDbContext()) {
                if (stmt != null) {
                    try {
                        rollback = m_sqlManager.getPreparedStatement(conn, "C_ROLLBACK");
                        rollback.execute();
                        rollback.close();
                    } catch (SQLException se) {
                        // ignore
                    }
                    try {
                        stmt.close();
                    } catch (SQLException exc) {
                        // ignore
                    }                
                }
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException se) {
                        // ignore
                    }
                }
            }
        }
    }

    /**
     * @see org.opencms.db.I_CmsBackupDriver#readBackupProjects(org.opencms.db.CmsDbContext)
     */
    public Vector readBackupProjects(CmsDbContext dbc) throws CmsException {
        Vector projects = new Vector();
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            // create the statement
            conn = m_sqlManager.getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_ORACLE_PROJECTS_READLAST_BACKUP");
            stmt.setInt(1, 300);
            res = stmt.executeQuery();
            while (res.next()) {
                Vector resources = m_driverManager.getBackupDriver().readBackupProjectResources(dbc, res.getInt("PUBLISH_TAG"));
                projects.addElement(
                    new CmsBackupProject(
                        res.getInt("PUBLISH_TAG"),
                        res.getInt("PROJECT_ID"),
                        res.getString("PROJECT_NAME"),
                        res.getString("PROJECT_DESCRIPTION"),
                        res.getInt("TASK_ID"),
                        new CmsUUID(res.getString("USER_ID")),
                        new CmsUUID(res.getString("GROUP_ID")),
                        new CmsUUID(res.getString("MANAGERGROUP_ID")),
                        res.getLong("DATE_CREATED"),
                        res.getInt("PROJECT_TYPE"),
                        CmsDbUtil.getTimestamp(res, "PROJECT_PUBLISHDATE"),
                        new CmsUUID(res.getString("PROJECT_PUBLISHED_BY")),
                        res.getString("PROJECT_PUBLISHED_BY_NAME"),
                        res.getString("USER_NAME"),
                        res.getString("GROUP_NAME"),
                        res.getString("MANAGERGROUP_NAME"),
                        resources));
            }
        } catch (SQLException exc) {
            throw m_sqlManager.getCmsException(this, "readBackupProjects", CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }

        return (projects);
    }
}
