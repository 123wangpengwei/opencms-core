/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/Attic/CmsResourceTypeNewPage.java,v $
 * Date   : $Date: 2003/07/14 20:12:41 $
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

package com.opencms.file;

import org.opencms.loader.CmsPageLoader;

import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.launcher.I_CmsLauncher;
import com.opencms.linkmanagement.CmsPageLinks;

import java.util.Hashtable;
import java.util.Map;

/**
 * Describes the resource type "newpage".<p>
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.2 $
 * @since 5.1
 */
public class CmsResourceTypeNewPage extends A_CmsResourceType {

    /** The type id of this resource */
    public static final int C_RESOURCE_TYPE_ID = 9;
    
    /** The name of this resource */
    public static final String C_RESOURCE_TYPE_NAME = "newpage";
    
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
     * @see com.opencms.file.I_CmsResourceType#getLauncherClass()
     */
    public String getLauncherClass() {
        return CmsPageLoader.class.getName();
    }

    /**
     * @see com.opencms.file.I_CmsResourceType#getLauncherType()
     */
    public int getLauncherType() {
        return I_CmsLauncher.C_TYPE_PAGE;
    }    
             
    /**
     * @see com.opencms.file.I_CmsResourceType#createResource(com.opencms.file.CmsObject, java.lang.String, java.util.Map, byte[], java.lang.Object)
     */
    public CmsResource createResource(CmsObject cms, String resourcename, Map properties, byte[] contents, Object parameter) throws CmsException {
        if (contents == null) {
            contents = (CmsResourceTypePage.getDefaultBodyStart() + new String(contents) + CmsResourceTypePage.getDefaultBodyEnd()).getBytes();
        }

        CmsFile file = cms.doCreateFile(resourcename, contents, C_RESOURCE_TYPE_NAME, properties);
        cms.doLockResource(resourcename, true);

        // linkmanagement: create the links of the new page (for the case that the content was not empty
        if (contents.length > 1) {
            CmsPageLinks linkObject = cms.getPageLinks(resourcename);
            cms.createLinkEntrys(linkObject.getResourceId(), linkObject.getLinkTargets());
        }
        return file;
    }  
    
    /**
     * Creates a resource for the specified template.<p>
     * 
     * @param cms the cms context
     * @param resourcename the name of the resource to create
     * @param properties properties for the new resource
     * @param contents content for the new resource
     * @param masterTemplate template for the new resource
     * @return the created resource 
     * @throws CmsException if something goes wrong
     */
    public CmsResource createResourceForTemplate(CmsObject cms, String resourcename, Hashtable properties, byte[] contents, String masterTemplate) throws CmsException {        
        properties.put(I_CmsConstants.C_XML_CONTROL_TEMPLATE_PROPERTY, masterTemplate);
        CmsFile resource = (CmsFile)createResource(cms, resourcename, properties, contents, null);                
        return resource;
    }
}