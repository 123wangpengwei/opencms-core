/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/oracle/Attic/CmsWorkflowDriver.java,v $
 * Date   : $Date: 2003/08/20 13:14:51 $
 * Version: $Revision: 1.3 $
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

import org.opencms.db.I_CmsSqlManager;


/**
 * Oracle/OCI implementation of the workflow driver methods.<p>
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @version $Revision: 1.3 $ $Date: 2003/08/20 13:14:51 $
 * @since 5.1
 */
public class CmsWorkflowDriver extends org.opencms.db.generic.CmsWorkflowDriver {  

    /**
     * @see org.opencms.db.I_CmsWorkflowDriver#initQueries(java.lang.String)
     */
    public I_CmsSqlManager initQueries(String dbPoolUrl) {
        //return new org.opencms.db.oracle.CmsSqlManager(dbPoolUrl);
        return (I_CmsSqlManager) org.opencms.db.oracle.CmsSqlManager.getInstance(dbPoolUrl);
    }

}
