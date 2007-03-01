/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/mysql3/Attic/CmsVfsDriver.java,v $
 * Date   : $Date: 2007/03/01 15:01:33 $
 * Version: $Revision: 1.1.8.2 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
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
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.db.mysql3;

import org.opencms.db.CmsDbContext;
import org.opencms.db.CmsDbSqlException;
import org.opencms.db.generic.Messages;
import org.opencms.file.CmsDataAccessException;
import org.opencms.file.CmsResource;
import org.opencms.util.CmsUUID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL3 implementation of the VFS driver methods.<p>
 * 
 * @author Michael Moossen
 * 
 * @version $Revision: 1.1.8.2 $
 * @since 6.0.0 
 */
public class CmsVfsDriver extends org.opencms.db.mysql.CmsVfsDriver {

    /**
     * @see org.opencms.db.I_CmsVfsDriver#readResourcesWithProperty(org.opencms.db.CmsDbContext, CmsUUID, org.opencms.util.CmsUUID, String, String)
     */
    public List readResourcesWithProperty(
        CmsDbContext dbc,
        CmsUUID projectId,
        CmsUUID propertyDef,
        String path,
        String value) throws CmsDataAccessException {

        List resources = new ArrayList();
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(dbc, projectId);
            if (value == null) {
                stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_GET_RESOURCE_WITH_PROPERTYDEF");
            } else {
                stmt = m_sqlManager.getPreparedStatement(conn, projectId, "C_RESOURCES_GET_RESOURCE_WITH_PROPERTYDEF_VALUE");
            }
            stmt.setString(1, propertyDef.toString());
            stmt.setString(2, path + "%");
            if (value != null) {
                stmt.setString(3, "%" + value + "%");
            }
            res = stmt.executeQuery();

            while (res.next()) {
                CmsResource resource = createResource(res, projectId);
                resources.add(resource);
            }
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }

        return resources;
    }

}