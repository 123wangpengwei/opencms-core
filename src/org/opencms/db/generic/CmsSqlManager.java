/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/generic/CmsSqlManager.java,v $
 * Date   : $Date: 2003/06/13 14:48:16 $
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
 
package org.opencms.db.generic;

import org.opencms.db.CmsDbPool;

import com.opencms.boot.I_CmsLogChannels;
import com.opencms.core.A_OpenCms;
import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.core.exceptions.CmsResourceNotFoundException;
import com.opencms.file.CmsProject;
import com.opencms.flex.util.CmsStringSubstitution;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Handles SQL queries from query.properties of the generic (ANSI-SQL) driver package.<p>
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @version $Revision: 1.2 $ $Date: 2003/06/13 14:48:16 $
 * @since 5.1
 */
public class CmsSqlManager extends Object implements Serializable, Cloneable {
    
    /**
     * The filename/path of the SQL query properties file.
     */
    private static final String C_PROPERTY_FILENAME = "org/opencms/db/generic/query.properties";
    
    /**
     * The properties hash holding the SQL queries.
     */
    private static Properties c_queries = null;
    
    /**
     * The URL to access the correct connection pool.
     */
    protected String m_dbPoolUrl;
    
    /**
     * This map caches all queries with replaced expressions to minimize costs of regex/matching operations.
     */
    protected Map m_cachedQueries;

    /**
     * CmsSqlManager constructor.
     * 
     * @param dbPoolUrl the URL to access the connection pool
     */
    public CmsSqlManager(String dbPoolUrl) {
        m_dbPoolUrl = CmsDbPool.C_DBCP_JDBC_URL_PREFIX + dbPoolUrl;
        
        if (c_queries == null) {
            c_queries = loadProperties(C_PROPERTY_FILENAME);  
            precalculateQueries(c_queries);          
        }
        
        m_cachedQueries = (Map) new HashMap();
    }
    
    /**
     * CmsSqlManager constructor.
     * 
     * @param dbPoolUrl the URL to access the correct connection pool
     * @param loadQueries flag indicating whether the query.properties should be loaded during initialization
     */
    protected CmsSqlManager(String dbPoolUrl, boolean loadQueries) {
        m_dbPoolUrl = CmsDbPool.C_DBCP_JDBC_URL_PREFIX + dbPoolUrl;
    
        if (loadQueries && c_queries == null) {
            c_queries = loadProperties(C_PROPERTY_FILENAME);   
            precalculateQueries(c_queries);         
        }
    }

    /**
     * Attemts to close the connection, statement and result set after a statement has been executed.
     * 
     * @param con the JDBC connection
     * @param stmnt the statement
     * @param res the result set
     * @see com.opencms.dbpool.CmsConnection#close()
     */
    public void closeAll(Connection con, Statement stmnt, ResultSet res) {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }

            if (stmnt != null) {
                stmnt.close();
            }

            if (res != null) {
                res.close();
            }
        } catch (SQLException e) {
            if (A_OpenCms.isLogging() && I_CmsLogChannels.C_LOGGING) {
                A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + getClass().getName() + "] error closing JDBC connection/statement/result: " + e.toString());
            }
        } finally {
            res = null;
            stmnt = null;
            con = null;
        }
    }
    
    /**
     * Free any allocated resources when the garbage 
     * collection tries to trash this object.
     * 
     * @throws Throwable if something goes wrong
     */
    protected void finalize() throws Throwable {
        if (c_queries != null) {
            c_queries.clear();
        }

        if (m_cachedQueries != null) {
            m_cachedQueries.clear();
        }

        c_queries = null;
        m_cachedQueries = null;
        m_dbPoolUrl = null;

        super.finalize();
    }

    /**
     * Searches for the SQL query with the specified key and CmsProject.
     * 
     * @param project the specified CmsProject
     * @param queryKey the key of the SQL query
     * @return the the SQL query in this property list with the specified key
     */
    public String get(CmsProject project, String queryKey) {
        return get(project.getId(), queryKey);
    }

    /**
     * Searches for the SQL query with the specified key and project-ID.<p>
     * 
     * The pattern "_T_" in table names is replaced with "_ONLINE_" or 
     * "_OFFLINE_" to choose the right database tables for SQL queries 
     * that are project dependent!
     * 
     * @param projectId the ID of the specified CmsProject
     * @param queryKey the key of the SQL query
     * @return the the SQL query in this property list with the specified key
     */
    public String get(int projectId, String queryKey) {       
        // get the SQL statement from the properties hash
        String query = get(queryKey);        
        
        // replace control chars.
        query = CmsStringSubstitution.substitute(query,"\t"," ");
        query = CmsStringSubstitution.substitute(query,"\n"," ");         

        if (projectId < 0) {
            // a project ID < 0 is an internal indicator that a project-independent 
            // query was requested- further regex operations are not required then!
            return query;
        }
        
        if (!m_cachedQueries.containsKey(queryKey)) {
            String searchPattern = "_T_";
            
            // make the statement project dependent
            String replacePattern = (projectId == I_CmsConstants.C_PROJECT_ONLINE_ID) ? "_ONLINE_" : "_OFFLINE_";                                          
            query = CmsStringSubstitution.substitute(query,searchPattern,replacePattern);           
            
            // to minimize costs, all statements with replaced expressions are cached in a map
            queryKey += (projectId == I_CmsConstants.C_PROJECT_ONLINE_ID) ? "_ONLINE" : "_OFFLINE";
            m_cachedQueries.put(queryKey, query);
        } else {
            // use the statement where the pattern is already replaced
            query = (String) m_cachedQueries.get(queryKey);            
        }

        return query;
    }

    /**
     * Searches for the SQL query with the specified key.
     * 
     * @param queryKey the SQL query key
     * @return the the SQL query in this property list with the specified key
     */
    public String get(String queryKey) {              
        if (c_queries == null) {
            c_queries = loadProperties(C_PROPERTY_FILENAME);
            precalculateQueries(c_queries);
        }      

        String value = null;
        if ((value = c_queries.getProperty(queryKey)) == null) {
            if (A_OpenCms.isLogging() && I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING) {
                A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + getClass().getName() + "] query '" + queryKey + "' not found in " + C_PROPERTY_FILENAME);
            }
        }

        return value;
    }

    /**
     * Retrieves the value of the designated column in the current row of this ResultSet object as 
     * a byte array in the Java programming language. The bytes represent the raw values returned 
     * by the driver. Overwrite this method if another database server requires a different 
     * handling of byte attributes in tables.
     * 
     * @param res the result set
     * @param attributeName the name of the table attribute
     * @return byte[] the column value; if the value is SQL NULL, the value returned is null 
     * @throws SQLException if a database access error occurs
     */
    public byte[] getBytes(ResultSet res, String attributeName) throws SQLException {       
        return res.getBytes(attributeName);
    }
    
    /**
     * Wraps an exception in a new CmsException object. Optionally, a log message is
     * written to the "critical" OpenCms logging channel.
     * 
     * @param o the object caused the exception
     * @param message a message that is written to the log
     * @param exceptionType the type of the exception
     * @param rootCause the exception that was thrown
     * @param logSilent if TRUE, no entry to the log is written
     * @return CmsException
     */
    public CmsException getCmsException(Object o, String message, int exceptionType, Throwable rootCause, boolean logSilent) {
        String className = "";
        
        if (o!=null) {            
            className = "[" + o.getClass().getName() + "] ";
        }
        
        if (message == null && rootCause != null) {            
            StackTraceElement[] stackTraceElements = rootCause.getStackTrace();
            String stackTraceElement = "";
            
            // i want to see only the first stack trace element of 
            // my own OpenCms classes in the log message...
            for (int i=0;i<stackTraceElements.length;i++) {
                String currentStackTraceElement = stackTraceElements[i].toString();
                if (currentStackTraceElement.indexOf(".opencms.")!=-1) {
                    stackTraceElement = currentStackTraceElement;
                    break;
                }
            }
            
            // where did we crash?
            message = "where: " + stackTraceElement + ", ";
            // why did we crash?
            message += "why: " + rootCause.toString();
        } else {
            message = "";
        }
        
        message = className + message;

        if (!logSilent && A_OpenCms.isLogging() && I_CmsLogChannels.C_LOGGING) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, message);
        }

        switch (exceptionType) {
            case CmsException.C_NOT_FOUND :
                return new CmsResourceNotFoundException(message);
        }

        return new CmsException(message, exceptionType, rootCause);
    }
    
    /**
     * Receives a JDBC connection from the (offline) pool. Use this method with caution! 
     * Using this method to makes only sense to read/write project independent data such 
     * as user data!
     * 
     * @return a JDBC connection from the (offline) pool 
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        // To receive a JDBC connection from the offline pool, 
        // a non-existent dummy project-ID is used
        return getConnection(Integer.MIN_VALUE);
    }

    /**
     * Receives a JDBC connection from the pool specified by the given CmsProject.
     * 
     * @param project the specified CmsProject
     * @return a JDBC connection from the pool specified by the project-ID 
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection(CmsProject project) throws SQLException {
        return getConnection(project.getId());
    }

    /**
     * Receives a JDBC connection from the pool specified by the given project-ID.
     * 
     * @param projectId the ID of the specified CmsProject
     * @return a JDBC connection from the pool specified by the project-ID 
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection(int projectId) throws SQLException {
        return DriverManager.getConnection(m_dbPoolUrl);
    }

    /**
     * Receives a JDBC connection from the backup pool. Use this method with caution! 
     * Using this method to makes only sense to read/write data to backup data. 
     * 
     * @return a JDBC connection from the backup pool 
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnectionForBackup() throws SQLException {
        return DriverManager.getConnection(m_dbPoolUrl);
    }

    /**
     * Receives a PreparedStatement for a JDBC connection specified by the key of a SQL query
     * and the CmsProject.
     * 
     * @param con the JDBC connection
     * @param project the specified CmsProject
     * @param queryKey the key of the SQL query
     * @return PreparedStatement a new PreparedStatement containing the pre-compiled SQL statement 
     * @throws SQLException if a database access error occurs
     */
    public PreparedStatement getPreparedStatement(Connection con, CmsProject project, String queryKey) throws SQLException {
        return getPreparedStatement(con, project.getId(), queryKey);
    }

    /**
     * Receives a PreparedStatement for a JDBC connection specified by the key of a SQL query
     * and the project-ID.
     * 
     * @param con the JDBC connection
     * @param projectId the ID of the specified CmsProject
     * @param queryKey the key of the SQL query
     * @return PreparedStatement a new PreparedStatement containing the pre-compiled SQL statement 
     * @throws SQLException if a database access error occurs
     */
    public PreparedStatement getPreparedStatement(Connection con, int projectId, String queryKey) throws SQLException {
        String rawSql = get(projectId, queryKey);
        return getPreparedStatementForSql(con, rawSql);
    }

    /**
     * Receives a PreparedStatement for a JDBC connection specified by the key of a SQL query.
     * 
     * @param con the JDBC connection
     * @param queryKey the key of the SQL query
     * @return PreparedStatement a new PreparedStatement containing the pre-compiled SQL statement 
     * @throws SQLException if a database access error occurs
     */
    public PreparedStatement getPreparedStatement(Connection con, String queryKey) throws SQLException {
        String rawSql = get(Integer.MIN_VALUE, queryKey);
        return getPreparedStatementForSql(con, rawSql);
    }
    
    /**
     * Receives a PreparedStatement for a JDBC connection specified by the SQL query.
     * 
     * @param con the JDBC connection
     * @param query the kSQL query
     * @return PreparedStatement a new PreparedStatement containing the pre-compiled SQL statement 
     * @throws SQLException if a database access error occurs
     */
    public PreparedStatement getPreparedStatementForSql(Connection con, String query) throws SQLException {
        // unfortunately, this wrapper is essential, because some JDBC driver 
        // implementations don't accept the delegated objects of DBCP's connection pool. 
        return con.prepareStatement(query);
    }    
    
    /**
     * Loads a Java properties hash.
     * 
     * @param propertyFilename the package/filename of the properties hash
     * @return Properties the new properties instance.
     */
    protected Properties loadProperties(String propertyFilename) {
        Properties properties = new Properties();

        try {
            properties.load(getClass().getClassLoader().getResourceAsStream(propertyFilename));
        } catch (NullPointerException exc) {
            if (A_OpenCms.isLogging() && I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING) {
                A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + getClass().getName() + "] error loading " + propertyFilename);
            }

            properties = null;
        } catch (java.io.IOException exc) {
            if (A_OpenCms.isLogging() && I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING) {
                A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "[" + getClass().getName() + "] error loading " + propertyFilename);
            }

            properties = null;
        }

        return properties;
    }
    
    /**
     * Generates a new primary key for a given database table. IMPORTANT: this method makes only
     * sense for old-style tables where the primary key is NOT a CmsUUID!
     * 
     * @param tableName the table for which a new primary key should be generated.
     * @return int the new primary key
     * @throws CmsException if an error occurs
     */
    public synchronized int nextId(String tableName) throws CmsException {
        return org.opencms.db.CmsIdGenerator.nextId(m_dbPoolUrl, tableName);
    }

    /**
     * Sets the designated parameter to the given Java array of bytes. The driver converts this 
     * to an SQL VARBINARY or LONGVARBINARY (depending on the argument's size relative to the 
     * driver's limits on VARBINARY values) when it sends it to the database. 
     * 
     * @param statement the PreparedStatement where the content is set
     * @param posn the first parameter is 1, the second is 2, ...
     * @param content the parameter value 
     * @throws SQLException if a database access error occurs
     */
    public void setBytes(PreparedStatement statement, int posn, byte[] content) throws SQLException {
        if (content.length < 2000) {
            statement.setBytes(posn, content);
        } else {
            statement.setBinaryStream(posn, new ByteArrayInputStream(content), content.length);
        }
    }

    /**
     * Replaces null Strings by an empty string.
     * 
     * @param value the string to validate
     * @return String the validate string or an empty string if the validated string is null
     */
    public String validateNull(String value) {
        if (value != null && value.length() != 0) {
            return value;
        }

        return " ";
    }
    
    /**
     * Makes all changes permanent since the previous commit/rollback if auto-commit is turned off.
     * 
     * @param conn the connection to commit
     */
    public void commit(Connection conn) {
        try {
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
        } catch (SQLException e) {
            getCmsException(this, e.getMessage(), CmsException.C_SQL_ERROR, e, false);
        }
    }
    
    /**
     * Undoes all changes made in the current transaction, optionally after the given Savepoint object was set.
     * 
     * @param conn the connection to roll back
     * @param savepoint an optional savepoint after which all changes are rolled back
     */
    public void rollback(Connection conn, Savepoint savepoint) {
        try {
            if (!conn.getAutoCommit()) {
                if (savepoint!=null) {
                    conn.rollback(savepoint);
                } else {
                    conn.rollback();
                }
            }
        } catch (SQLException e) {
            getCmsException(this, e.getMessage(), CmsException.C_SQL_ERROR, e, false);
        }        
    } 
    
    /**
     * Removes the given Savepoint object from the current transaction.
     * 
     * @param conn the connection from which the savepoint object is removed
     * @param savepoint the Savepoint object to be removed 
     */
    public void releaseSavepoint(Connection conn, Savepoint savepoint) {
        try {
            if (!conn.getAutoCommit()) {
                conn.releaseSavepoint(savepoint);
            }
        } catch (SQLException e) {
            getCmsException(this, e.getMessage(), CmsException.C_SQL_ERROR, e, false);
        }        
    }
    
    /**
     * Replaces patterns ${XXX} by another property value, if XXX is a property key with a value.
     * 
     * @param properties a hash containt key/value coded SQL statements
     */
    protected void precalculateQueries(Properties properties) {
        String currentKey = null;
        String currentValue = null;
        int startIndex = 0;
        int endIndex = 0;
        int lastIndex = 0;

        Iterator allKeys = properties.keySet().iterator();
        while (allKeys.hasNext()) {
            currentKey = (String) allKeys.next();
            currentValue = (String) properties.get(currentKey);
            startIndex = endIndex = lastIndex = 0;

            while ((startIndex = currentValue.indexOf("${", lastIndex)) != -1) {
                if ((endIndex = currentValue.indexOf('}', startIndex)) != -1) {
                    String replaceKey = currentValue.substring(startIndex + 2, endIndex);
                    String searchPattern = currentValue.substring(startIndex, endIndex + 1);
                    String replacePattern = (String) this.get(replaceKey);

                    if (replacePattern != null) {
                        currentValue = CmsStringSubstitution.substitute(currentValue, searchPattern, replacePattern);
                    }

                    lastIndex = endIndex + 2;
                }
            }

            properties.put(currentKey, currentValue);
        }
    }  
    
}