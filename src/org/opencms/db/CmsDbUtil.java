/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/Attic/CmsDbUtil.java,v $
 * Date   : $Date: 2004/02/23 23:27:03 $
 * Version: $Revision: 1.7 $
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

import org.opencms.main.CmsException;
import org.opencms.main.I_CmsConstants;
import org.opencms.main.OpenCms;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Hashtable;

/**
 * This class is used to create primary keys as integers for Cms database tables that
 * don't have a UUID primary key.<p>
 * 
 * @version $Revision: 1.7 $ $Date: 2004/02/23 23:27:03 $
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 * @since 5.1
 */
public final class CmsDbUtil extends Object {

    /** Hashtable with next available id's */
    private static Hashtable c_currentId;

    /** Hashtable with border id's */
    private static Hashtable c_borderId;

    /** Grow value */
    private static final int C_GROW_VALUE = 10;

    /** The name of the default pool */
    private static String c_dbPoolUrl;

    /**
     * Default constructor.<p>
     * 
     * Nobody is allowed to create an instance of this class!
     */
    private CmsDbUtil() {
        super();
    }

    /**
     * Initilizes this DB utils.<p>
     */
    public static void init() {
        c_currentId = new Hashtable();
        c_borderId = new Hashtable();
        c_dbPoolUrl = "";
    }
    
    /**
     * Sets the URL of the connection pool.<p>
     * 
     * @param dbPoolUrl the URL to access the connection pool
     */
    public static void setDefaultPool(String dbPoolUrl) {
        c_dbPoolUrl = dbPoolUrl;
    }

    /**
     * Creates a new primary key ID for a given table.<p>
     * 
     * @param tableName the name of the table to create a new primary key ID
     * @return a new primary key ID for the given table
     * @throws CmsException if something goes wrong
     */
    public static synchronized int nextId(String tableName) throws CmsException {
        return nextId(c_dbPoolUrl, tableName);
    }

    /**
     * Creates a new primary key ID for a given table using JDBC connection specified by a pool URL.<p>
     * 
     * @param dbPoolUrl the URL to access the connection pool
     * @param tableName the name of the table to create a new primary key ID
     * @return a new primary key ID for the given table
     * @throws CmsException if something goes wrong
     */
    public static synchronized int nextId(String dbPoolUrl, String tableName) throws CmsException {
        String cacheKey = dbPoolUrl + "." + tableName;
        
        // generated primary keys are cached!
        if (c_currentId.containsKey(cacheKey)) {
            int id = ((Integer) c_currentId.get(cacheKey)).intValue();
            int borderId = ((Integer) c_borderId.get(cacheKey)).intValue();
            if (id < borderId) {
                int nextId = id + 1;
                c_currentId.put(cacheKey, new Integer(nextId));
                return id;
            }
        }

        // there is no primary key ID for the given table yet in the cache.
        // we generate a new primary key ID based on the last primary key
        // entry in the CMS_SYSTEMID table instead
        generateNextId(dbPoolUrl, tableName, cacheKey);
        
        // afterwards, return back to this method to take the new primary key 
        // ID out of the cache...
        return nextId(dbPoolUrl, tableName);
    }

    /**
     * Creates a new primary key ID for a given table based on the last primary key ID for this
     * table in the CMS_SYSTEMID table.<p>
     * 
     * @param dbPoolUrl the URL to access the connection pool
     * @param tableName the name of the table to create a new primary key ID
     * @param cacheKey the key to store the new primary key ID in the cache
     * @throws CmsException if something goes wrong
     */
    private static void generateNextId(String dbPoolUrl, String tableName, String cacheKey) throws CmsException {
        Connection con = null;
        int id;
        int borderId;
        
        try {
            if (!dbPoolUrl.startsWith(CmsDbPool.C_DBCP_JDBC_URL_PREFIX)) {
                dbPoolUrl = CmsDbPool.C_DBCP_JDBC_URL_PREFIX + dbPoolUrl;
            }

            con = DriverManager.getConnection(dbPoolUrl);
            // repeat this operation, until the nextId is valid and can be saved
            // (this is for clustering of several OpenCms)
            do {
                id = readId(con, tableName);
                if (id == I_CmsConstants.C_UNKNOWN_ID) {
                    // there was no entry - set it to 0
                    // EF: set id to 1 because the table contains
                    // the next available id
                    id = 1;
                    createId(con, tableName, id);
                }
                borderId = id + C_GROW_VALUE;
                // save the next id for future requests
            } while (!writeId(con, tableName, id, borderId));
            // store the generated values in the cache
            c_currentId.put(cacheKey, new Integer(id));
            c_borderId.put(cacheKey, new Integer(borderId));
        } catch (SQLException e) {
            throw new CmsException("[" + CmsDbUtil.class.getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            // close all db-resources
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException exc) {
                    // nothing to do here
                }
            }
        }
    }

    /**
     * Reads the last primary key ID for a given table.<p>
     * 
     * @param conn the connection to access the database
     * @param tableName the name of the table to read the primary key ID
     * @return the primary key ID or C_UNKNOWN_ID if there is no entry for the given table
     * @throws CmsException if something gows wrong
     */
    private static int readId(Connection conn, String tableName) throws CmsException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        try {
            stmt = conn.prepareStatement("SELECT CMS_SYSTEMID.ID FROM CMS_SYSTEMID WHERE CMS_SYSTEMID.TABLE_KEY=?");
            stmt.setString(1, tableName);
            res = stmt.executeQuery();
            if (res.next()) {
                return res.getInt(1);
            } else {
                return I_CmsConstants.C_UNKNOWN_ID;
            }
        } catch (SQLException e) {
            throw new CmsException("[" + CmsDbUtil.class.getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            // close all db-resources
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException exc) {
                    // nothing to do here
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException exc) {
                    // nothing to do here
                }
            }
        }
    }

    /**
     * Updates the CMS_SYSTEMID table with a new primary key ID for a given table.<p>
     * 
     * @param conn the connection to access the database
     * @param tableName the name of the table to read the primary key ID
     * @param oldId the last primary key ID
     * @param newId the new primary key ID
     * @return true if the number of affected rows is 1
     * @throws CmsException if something gows wrong
     */
    private static boolean writeId(Connection conn, String tableName, int oldId, int newId) throws CmsException {
        PreparedStatement statement = null;
        
        try {
            statement = conn.prepareStatement("UPDATE CMS_SYSTEMID SET CMS_SYSTEMID.ID=? WHERE CMS_SYSTEMID.TABLE_KEY=? AND CMS_SYSTEMID.ID=?");
            statement.setInt(1, newId);
            statement.setString(2, tableName);
            statement.setInt(3, oldId);
            int amount = statement.executeUpdate();
            // return, if the update had succeeded
            return (amount == 1);
        } catch (SQLException e) {
            throw new CmsException("[" + CmsDbUtil.class.getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException exc) {
                    // nothing to do here
                }
            }
        }
    }

    /**
     * Creates a new primary key ID for a given table in the CMS_SYSTEMID table.<p>
     * 
     * @param conn the connection to access the database
     * @param tableName the name of the table to read the primary key ID
     * @param newId the new primary key ID
     * @throws CmsException if something gows wrong
     */
    private static void createId(Connection conn, String tableName, int newId) throws CmsException {
        PreparedStatement statement = null;
        
        try {
            statement = conn.prepareStatement("INSERT INTO CMS_SYSTEMID (TABLE_KEY,ID) VALUES (?,?)");
            statement.setString(1, tableName);
            statement.setInt(2, newId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CmsException("[" + CmsDbUtil.class.getName() + "]" + e.getMessage(), CmsException.C_SQL_ERROR, e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException exc) {
                    // nothing to do here
                }
            }
        }
    }

    /**
     * This method tries to get the timestamp several times, because there
     * is a timing problem in the mysql driver.<p>
     *
     * @param result the resultset to get the stamp from
     * @param column the column to read the timestamp from
     * @return the timestamp
     * @throws SQLException if something goes wrong
     */
    public static final Timestamp getTimestamp(ResultSet result, String column)
            throws SQLException {
        int i = 0;
        for (;;) {
            try {
                return (result.getTimestamp(column));
            } catch (SQLException exc) {
                i++;
                if (i >= 10) {
                    throw exc;
                } else {
                    if (OpenCms.getLog(CmsDbUtil.class).isWarnEnabled()) {
                        OpenCms.getLog(CmsDbUtil.class).warn("Trying to get timestamp " + column + " #" + i);
                    }
                }
            }
        }
    }
}
