/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/Attic/CmsResourceTypeXmlPage.java,v $
 * Date   : $Date: 2004/01/22 16:42:43 $
 * Version: $Revision: 1.7 $
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

import org.opencms.loader.CmsXmlPageLoader;
import org.opencms.lock.CmsLock;
import org.opencms.main.OpenCms;
import org.opencms.page.CmsPageException;
import org.opencms.page.CmsXmlPage;
import org.opencms.staticexport.CmsLink;
import org.opencms.staticexport.CmsLinkTable;
import org.opencms.validation.I_CmsHtmlLinkValidatable;

import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Describes the resource type "xmlpage".<p>
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.7 $
 * @since 5.1
 */
public class CmsResourceTypeXmlPage extends A_CmsResourceType implements I_CmsHtmlLinkValidatable {

    /** The type id of this resource */
    public static final int C_RESOURCE_TYPE_ID = 10;
    
    /** The name of this resource */
    public static final String C_RESOURCE_TYPE_NAME = "xmlpage";
    
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
        return CmsXmlPageLoader.C_RESOURCE_LOADER_ID;
    }    
             
    /**
     * @see com.opencms.file.I_CmsResourceType#createResource(com.opencms.file.CmsObject, java.lang.String, java.util.Map, byte[], java.lang.Object)
     */
    public CmsResource createResource(CmsObject cms, String resourcename, Map properties, byte[] contents, Object parameter) throws CmsException {
        CmsFile file = cms.doCreateFile(resourcename, contents, C_RESOURCE_TYPE_NAME, properties);
        cms.doLockResource(resourcename, false, CmsLock.C_MODE_COMMON);

        contents = null;
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
        properties.put(I_CmsConstants.C_PROPERTY_TEMPLATE, masterTemplate);
        CmsFile resource = (CmsFile)createResource(cms, resourcename, properties, contents, null);                
        return resource;
    }
    
    /**
     * @see com.opencms.file.I_CmsResourceType#isDirectEditable()
     */
    public boolean isDirectEditable() {
        return true;
    }
    
    /**
     * @see org.opencms.validation.I_CmsHtmlLinkValidatable#findLinks(com.opencms.file.CmsObject, com.opencms.file.CmsResource)
     */
    public List findLinks(CmsObject cms, CmsResource resource) {
        List links = (List) new ArrayList();
        CmsFile file = null;
        CmsXmlPage xmlPage = null;
        Set languages = null;
        String languageName = null;
        List elementNames = null;
        String elementName = null;
        CmsLinkTable linkTable = null;
        String linkName = null;
        CmsLink link = null;

        try {
            file = cms.readFile(cms.getRequestContext().removeSiteRoot(resource.getRootPath()));
        } catch (CmsException e) {
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Error reading file content of " + resource.getRootPath(), e);
            }

            return Collections.EMPTY_LIST;
        }

        try {
            xmlPage = CmsXmlPage.read(cms, file);
            languages = xmlPage.getLanguages();

            // iterate over all languages
            Iterator i = languages.iterator();
            while (i.hasNext()) {
                languageName = (String) i.next();
                elementNames = xmlPage.getNames(languageName);

                // iterate over all body elements per language
                Iterator j = elementNames.iterator();
                while (j.hasNext()) {
                    elementName = (String) j.next();
                    linkTable = xmlPage.getLinkTable(elementName, languageName);

                    // iterate over all links inside a body element
                    Iterator k = linkTable.iterator();
                    while (k.hasNext()) {
                        linkName = (String) k.next();
                        link = linkTable.getLink(linkName);

                        // external links are ommitted
                        if (link.isInternal()) {
                            links.add(link.getTarget());
                        }
                    }
                }
            }
        } catch (CmsPageException e) {
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Error processing HTML content of " + resource.getRootPath(), e);
            }

            return Collections.EMPTY_LIST;
        }

        return links;
    }
    
}