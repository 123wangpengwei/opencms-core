/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/Attic/CmsResourceTypePlain.java,v $
* Date   : $Date: 2003/07/18 19:03:49 $
* Version: $Revision: 1.36 $
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

package com.opencms.file;

import org.opencms.loader.CmsDumpLoader;

import com.opencms.core.CmsException;

import java.util.Map;

/**
 * Implementation of a "basic and plain" resource type for any kind of resource.
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 */
public class CmsResourceTypePlain extends A_CmsResourceType {
        
    /** The type id of this resource */
    public static final int C_RESOURCE_TYPE_ID = 3;
    
    /** The name of this resource */
    public static final String C_RESOURCE_TYPE_NAME = "plain";
    
    /**
     * @see com.opencms.file.I_CmsResourceType#getResourceType()
     */
    public int getResourceType() {
        return C_RESOURCE_TYPE_ID;
    }

    /**
     * @see com.opencms.file.A_CmsResourceType#getResourceTypeName()
     */
    public String getResourceTypeName() {
        return C_RESOURCE_TYPE_NAME;
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#getLoaderId()
     */
    public int getLoaderId() {
        return CmsDumpLoader.C_RESOURCE_LOADER_ID;
    }     
    
    /**
     * @see com.opencms.file.I_CmsResourceType#createResource(com.opencms.file.CmsObject, java.lang.String, java.util.Map, byte[], java.lang.Object)
     */
    public CmsResource createResource(CmsObject cms, String resourcename, Map properties, byte[] contents, Object parameter) throws CmsException {
        CmsResource res = cms.doCreateFile(resourcename, contents, getResourceTypeName(), properties);
        // TODO: Move locking of resource to CmsObject or CmsDriverManager
        cms.doLockResource(cms.readAbsolutePath(res), true);
        return res;
    }    
}
