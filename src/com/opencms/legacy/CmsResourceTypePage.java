/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/legacy/Attic/CmsResourceTypePage.java,v $
 * Date   : $Date: 2004/07/08 15:21:13 $
 * Version: $Revision: 1.8 $
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
 
package com.opencms.legacy;

import org.opencms.db.CmsDriverManager;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.file.types.A_CmsResourceType;
import org.opencms.importexport.A_CmsImport;

import java.util.List;

/**
 * Describes the resource type "page".<p>
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.8 $
 * @since 5.1
 * 
 * @deprecated Will not be supported past the OpenCms 6 release.
 */
public class CmsResourceTypePage extends A_CmsResourceType {

    /** The type id of this resource. */
    public static final int C_RESOURCE_TYPE_ID = A_CmsImport.C_RESOURCE_TYPE_PAGE_ID;
    
    /** The name of this resource. */
    public static final String C_RESOURCE_TYPE_NAME = A_CmsImport.C_RESOURCE_TYPE_PAGE_NAME;

    /**
     * @see org.opencms.file.types.I_CmsResourceType#getTypeId()
     */
    public int getTypeId() {
        return C_RESOURCE_TYPE_ID;
    }

    /**
     * @see org.opencms.file.types.A_CmsResourceType#getTypeName()
     */
    public String getTypeName() {
        return C_RESOURCE_TYPE_NAME;
    }

    /**
     * @see org.opencms.file.types.I_CmsResourceType#getLoaderId()
     */
    public int getLoaderId() {
        return CmsXmlTemplateLoader.C_RESOURCE_LOADER_ID;
    }     
    
    /**
     * @see org.opencms.file.types.I_CmsResourceType#createResource(org.opencms.file.CmsObject, CmsDriverManager, java.lang.String, byte[], List)
     */
    public CmsResource createResource(CmsObject cms, CmsDriverManager driverManager, String resourcename, byte[] content, List properties) {
        throw new RuntimeException("createResource(): The resource type 'page' is deprecated and not longer supported!");
    }
}
