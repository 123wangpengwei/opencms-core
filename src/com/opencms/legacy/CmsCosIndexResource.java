/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/legacy/Attic/CmsCosIndexResource.java,v $
 * Date   : $Date: 2004/07/05 11:58:21 $
 * Version: $Revision: 1.2 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2004 Alkacon Software (http://www.alkacon.com)
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
package com.opencms.legacy;

import org.opencms.search.A_CmsIndexResource;

import com.opencms.defaults.master.CmsMasterDataSet;


/**
 * Wrapper class to hide the concrete type of a data object.<p>
 * The type is either <code>CmsResource</code> while indexing vfs data,
 * or <code>CmsMasterDataSet</code> while indexing cos data.
 * 
 * @version $Revision: 1.2 $ $Date: 2004/07/05 11:58:21 $
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 * @since 5.3.1
 */
public class CmsCosIndexResource extends A_CmsIndexResource {
    
    /**
     * Creates a new instance to wrap the given <code>CmsMasterDataSet</code>.<p>
     * 
     * @param ds the data object
     * @param path access path of the data object
     * @param channel channel of the data object
     * @param contentDefinition content definition of the data object
     */
    public CmsCosIndexResource(CmsMasterDataSet ds, String path, String channel, String contentDefinition) {
        m_data = ds;
        m_id = ds.m_masterId;
        m_name = ds.m_title;
        m_type = ds.m_subId;
        m_mimeType = null;
        m_path = path;
        m_channel = channel;
        m_contentDefinition = contentDefinition;
    }

    /**
     * @see com.opencms.legacy.CmsCosIndexResource#getDocumentKey()
     */
    public String getDocumentKey() {
        return "COS" + getType();
    }    
}
