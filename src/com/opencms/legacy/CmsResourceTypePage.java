/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/legacy/Attic/CmsResourceTypePage.java,v $
 * Date   : $Date: 2004/02/27 14:53:32 $
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
 
package com.opencms.legacy;

import org.opencms.file.A_CmsResourceType;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.importexport.A_CmsImport;

import java.util.Map;

/**
 * Describes the resource type "page".<p>
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.3 $
 * @since 5.1
 */
public class CmsResourceTypePage extends A_CmsResourceType {

    /** The type id of this resource */
    public static final int C_RESOURCE_TYPE_ID = A_CmsImport.C_RESOURCE_TYPE_PAGE_ID;
    
    /** The name of this resource */
    public static final String C_RESOURCE_TYPE_NAME = A_CmsImport.C_RESOURCE_TYPE_PAGE_NAME;

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
        return CmsXmlTemplateLoader.C_RESOURCE_LOADER_ID;
    }     
    
    /**
     * @see org.opencms.file.I_CmsResourceType#createResource(org.opencms.file.CmsObject, java.lang.String, java.util.Map, byte[], java.lang.Object)
     */
    public CmsResource createResource(CmsObject cms, String resourcename, Map properties, byte[] contents, Object parameter) {
        throw new RuntimeException("createResource(): The resource type 'page' is deprecated and must be transformed to 'newpage'");
    }
}
