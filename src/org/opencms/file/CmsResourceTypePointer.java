/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/file/Attic/CmsResourceTypePointer.java,v $
 * Date   : $Date: 2004/06/14 14:25:57 $
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

package org.opencms.file;

import org.opencms.loader.CmsPointerLoader;
import org.opencms.lock.CmsLock;
import org.opencms.main.CmsException;

import java.util.List;

/**
 * Implementation of a resource type for external links.<p>
 *
 * @author Michael Emmerich (m.emmerich@alkacon.com)
 * @version $Revision: 1.10 $
 */
public class CmsResourceTypePointer extends A_CmsResourceType {

    /** The type id of this resource type. */
    public static final int C_RESOURCE_TYPE_ID = 99;

    /** The name of this resource type. */
    public static final String C_RESOURCE_TYPE_NAME = "pointer";

    /**
     * @see org.opencms.file.I_CmsResourceType#getResourceType()
     */
    public int getResourceType() {
        return C_RESOURCE_TYPE_ID;
    }

    /**
     * @see org.opencms.file.A_CmsResourceType#getResourceTypeName()
     */
    public String getResourceTypeName() {
        return C_RESOURCE_TYPE_NAME;
    }

    /**
     * @see org.opencms.file.I_CmsResourceType#getLoaderId()
     */
    public int getLoaderId() {
        return CmsPointerLoader.C_RESOURCE_LOADER_ID;
    } 
    
    /**
     * @see org.opencms.file.I_CmsResourceType#createResource(org.opencms.file.CmsObject, java.lang.String, List, byte[], java.lang.Object)
     */
    public CmsResource createResource(CmsObject cms, String resourcename, List properties, byte[] contents, Object parameter) throws CmsException {
        // create the new pointer
        CmsResource res = cms.doCreateFile(resourcename, contents, getResourceTypeName(), properties);
        contents = null;
        // TODO: Move locking of resource to CmsObject or CmsDriverManager
        cms.doLockResource(cms.readAbsolutePath(res), CmsLock.C_MODE_COMMON);
        return res;
    }    
}