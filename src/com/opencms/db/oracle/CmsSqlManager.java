/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/db/oracle/Attic/CmsSqlManager.java,v $
 * Date   : $Date: 2003/05/22 10:14:37 $
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
 
package com.opencms.db.oracle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import oracle.jdbc.driver.OracleResultSet;

/**
 * Reads SQL queries from query.properties of this driver package.
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @version $Revision: 1.2 $ $Date: 2003/05/22 10:14:37 $ 
 */
public class CmsSqlManager extends com.opencms.db.generic.CmsSqlManager {
    
    private static Properties m_queries = null;    
    private static final String C_PROPERTY_FILENAME = "com/opencms/db/oracle/query.properties";
    
    /**
     * CmsSqlManager constructor.
     */
    public CmsSqlManager(String dbPoolUrl) {
        super(dbPoolUrl);
        
        if (m_queries == null) {
            m_queries = loadProperties(C_PROPERTY_FILENAME);
        }
    }

    /**
     * Get the value for the query name
     *
     * @param queryName the name of the property
     * @return The value of the property
     */
    public String get(String queryName) {
        if (m_queries == null) {
            m_queries = loadProperties(C_PROPERTY_FILENAME);
        }
        
        String value = m_queries.getProperty(queryName);
        if (value == null || "".equals(value)) {
            value = super.get(queryName);
        }
        
        return value;
    }

    /**
     * Returns a byte array for a given table attribute in the result set.
     * 
     * @param res the result set
     * @param columnName the name of the table attribute
     * @return byte[]
     * @throws SQLException
     * @see com.opencms.db.generic.CmsSqlManager#getBytes(ResultSet, String)
     */
    public byte[] getBytes(ResultSet res, String columnName) throws SQLException {
        oracle.sql.BLOB blob = ((OracleResultSet) res).getBLOB(columnName);
        byte[] content = new byte[(int) blob.length()];
        content = blob.getBytes(1, (int) blob.length());

        return content;
    }
    
}
