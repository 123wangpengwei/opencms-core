/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/oracle/CmsBackupDriver.java,v $
 * Date   : $Date: 2003/09/08 09:08:08 $
 * Version: $Revision: 1.10 $
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

import oracle.jdbc.driver.OracleResultSet;

import com.opencms.core.CmsException;
import com.opencms.file.CmsBackupProject;
import com.opencms.file.CmsBackupResource;
import com.opencms.flex.util.CmsUUID;
import com.opencms.util.SqlHelper;

import java.io.ByteArrayInputStream;
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
 * Oracle/OCI implementation of the backup driver methods.<p>
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @version $Revision: 1.10 $ $Date: 2003/09/08 09:08:08 $
 * @since 5.1
 */
public class CmsBackupDriver extends org.opencms.db.generic.CmsBackupDriver {   

    /**
     * @see org.opencms.db.I_CmsBackupDriver#getAllBackupProjects()
     */
    public Vector getAllBackupProjects() throws CmsException {
        Vector projects = new Vector();
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            // create the statement
            conn = m_sqlManager.getConnectionForBackup();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_ORACLE_PROJECTS_READLAST_BACKUP");
            stmt.setInt(1, 300);
            res = stmt.executeQuery();
            while (res.next()) {
                Vector resources = m_driverManager.getVfsDriver().readBackupProjectResources(res.getInt("TAG_ID"));
                projects.addElement(
                    new CmsBackupProject(
                        res.getInt("TAG_ID"),
                        res.getInt("PROJECT_ID"),
                        res.getString("PROJECT_NAME"),
                        res.getString("PROJECT_DESCRIPTION"),
                        res.getInt("TASK_ID"),
                        new CmsUUID(res.getString("USER_ID")),
                        new CmsUUID(res.getString("GROUP_ID")),
                        new CmsUUID(res.getString("MANAGERGROUP_ID")),
                        SqlHelper.getTimestamp(res, "PROJECT_CREATEDATE"),
                        res.getInt("PROJECT_TYPE"),
                        SqlHelper.getTimestamp(res, "PROJECT_PUBLISHDATE"),
                        new CmsUUID(res.getString("PROJECT_PUBLISHED_BY")),
                        res.getString("PROJECT_PUBLISHED_BY_NAME"),
                        res.getString("USER_NAME"),
                        res.getString("GROUP_NAME"),
                        res.getString("MANAGERGROUP_NAME"),
                        resources));
            }
        } catch (SQLException exc) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, exc, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
        }
        
        return (projects);
    }

    /**
     * @see org.opencms.db.I_CmsBackupDriver#initQueries(java.lang.String)
     */
    public org.opencms.db.generic.CmsSqlManager initQueries() {
        return new org.opencms.db.oracle.CmsSqlManager();
    }


    /**
     * @see org.opencms.db.I_CmsBackupDriver#deleteBackups(java.util.List)
     */
    public void deleteBackups(List existingBackups, int maxVersions) throws CmsException {        
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        PreparedStatement stmt3 = null;
        
        Connection conn = null;
        CmsBackupResource currentResource = null;
        int count = existingBackups.size() - maxVersions;

        try {
            conn = m_sqlManager.getConnectionForBackup();
            stmt1 = m_sqlManager.getPreparedStatement(conn, "C_ORACLE_BACKUP_DELETE_CONTENT");
            stmt2 = m_sqlManager.getPreparedStatement(conn, "C_ORACLE_BACKUP_DELETE_RESOURCES");
            stmt3 = m_sqlManager.getPreparedStatement(conn, "C_ORACLE_BACKUP_DELETE_STRUCTURE");

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
            }

            if (count > 0) {
                stmt1.executeBatch();
                stmt2.executeBatch();
                stmt3.executeBatch();
            }

        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_SQL_ERROR, e, false);
        } catch (Exception ex) {
            throw m_sqlManager.getCmsException(this, null, CmsException.C_UNKNOWN_EXCEPTION, ex, false);
        } finally {
            m_sqlManager.closeAll(conn, stmt1, null);
            m_sqlManager.closeAll(conn, stmt2, null);
            m_sqlManager.closeAll(conn, stmt3, null);
        }
    }

    /**
     * @see org.opencms.db.I_CmsBackupDriver#writeBackupFileContent(com.opencms.flex.util.CmsUUID, com.opencms.flex.util.CmsUUID, byte[], int)
     */
    public void writeBackupFileContent(
        CmsUUID backupId,
        CmsUUID fileId,
        byte[] fileContent,
        int tagId)
        throws CmsException {

        PreparedStatement stmt = null;
        PreparedStatement nextStatement = null;
        PreparedStatement trimStatement = null;
        Connection conn = null;
        ResultSet res = null;
        
        //this.createFileContent(fileId,fileContent,tagId,1,true);
        
        try {
            conn = m_sqlManager.getConnectionForBackup();
            stmt = m_sqlManager.getPreparedStatement(conn, "C_ORACLE_FILESFORINSERT_BACKUP");

            // first insert new file without file_content, then update the file_content
            // these two steps are necessary because of using BLOBs in the Oracle DB
            stmt.setString(1, fileId.toString());
            stmt.setInt(2, tagId);
            stmt.setString(3, backupId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(
                this,
                null,
                CmsException.C_SQL_ERROR,
                e,
                false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
            m_sqlManager.closeAll(null, nextStatement, null);
            m_sqlManager.closeAll(null, trimStatement, null);
        }
        
        
        
        try {

            conn = m_sqlManager.getConnectionForBackup();
            stmt =
                m_sqlManager.getPreparedStatement(
                    conn,
                    "C_ORACLE_FILESFORUPDATE_BACKUP");

            // update the file content in the FILES database.
            stmt.setString(1, fileId.toString());
            stmt.setString(2, backupId.toString());
            conn.setAutoCommit(false);
            //res = stmt.executeQuery();
            res = ((DelegatingResultSet)stmt.executeQuery()).getInnermostDelegate();
            try {
                while (res.next()) {
                    oracle.sql.BLOB blobnew =
                        ((OracleResultSet) res).getBLOB("FILE_CONTENT");
                    // first trim the blob to 0 bytes, otherwise there could be left some bytes
                    // of the old content
                    //trimStatement = conn.prepareStatement(m_sqlManager.get("C_TRIMBLOB"));
                    trimStatement =
                        m_sqlManager.getPreparedStatementForSql(
                            conn,
                            m_sqlManager.get("C_TRIMBLOB"));
                    trimStatement.setBlob(1, blobnew);
                    trimStatement.setInt(2, 0);
                    trimStatement.execute();
                    ByteArrayInputStream instream =
                        new ByteArrayInputStream(fileContent);
                    OutputStream outstream = blobnew.getBinaryOutputStream();
                    byte[] chunk = new byte[blobnew.getChunkSize()];
                    int i = -1;
                    while ((i = instream.read(chunk)) != -1) {
                        outstream.write(chunk, 0, i);
                    }
                    instream.close();
                    outstream.close();
                }
                // for the oracle-driver commit or rollback must be executed manually
                // because setAutoCommit = false in CmsDbPool.CmsDbPool
                //nextStatement = conn.prepareStatement(m_sqlManager.get("C_COMMIT"));
                nextStatement =
                    m_sqlManager.getPreparedStatementForSql(
                        conn,
                        m_sqlManager.get("C_COMMIT"));
                nextStatement.execute();
                nextStatement.close();
                conn.setAutoCommit(true);
            } catch (IOException e) {
                throw m_sqlManager.getCmsException(
                    this,
                    null,
                    CmsException.C_SERIALIZATION,
                    e,
                    false);
            }
        } catch (SQLException e) {
            throw m_sqlManager.getCmsException(
                this,
                null,
                CmsException.C_SQL_ERROR,
                e,
                false);
        } finally {
            m_sqlManager.closeAll(conn, stmt, res);
            m_sqlManager.closeAll(null, nextStatement, null);
            m_sqlManager.closeAll(null, trimStatement, null);
        }
    }
}

