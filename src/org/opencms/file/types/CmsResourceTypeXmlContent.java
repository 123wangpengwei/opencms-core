/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/file/types/CmsResourceTypeXmlContent.java,v $
 * Date   : $Date: 2004/11/02 08:30:56 $
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

package org.opencms.file.types;

import org.opencms.configuration.CmsConfigurationException;
import org.opencms.db.CmsSecurityManager;
import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.loader.CmsXmlContentLoader;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.security.CmsPermissionSet;
import org.opencms.util.CmsHtmlConverter;
import org.opencms.xml.CmsXmlContentDefinition;
import org.opencms.xml.CmsXmlEntityResolver;
import org.opencms.xml.content.CmsXmlContent;
import org.opencms.xml.content.CmsXmlContentFactory;

import java.util.List;
import java.util.Locale;

import org.apache.commons.collections.ExtendedProperties;

/**
 * Resource type descriptor for the type "xmlcontent".<p>
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.7 $
 * 
 * @since 5.5
 */
public class CmsResourceTypeXmlContent extends A_CmsResourceType {

    /** Configuration key for the (optional) schema. */
    public static final String C_CONFIGURATION_SCHEMA = "schema";

    /** The type id of this resource. */
    private int m_resourceType;

    /** The name of this resource. */
    private String m_resourceTypeName;

    /** The (optional) schema of this resource. */
    private String m_schema;

    /**
     * @see org.opencms.file.types.A_CmsResourceType#addConfigurationParameter(java.lang.String, java.lang.String)
     */
    public void addConfigurationParameter(String paramName, String paramValue) {

        if (I_CmsResourceType.C_CONFIGURATION_RESOURCE_TYPE_ID.equalsIgnoreCase(paramName)) {
            m_resourceType = Integer.valueOf(paramValue).intValue();
        } else if (I_CmsResourceType.C_CONFIGURATION_RESOURCE_TYPE_NAME.equalsIgnoreCase(paramName)) {
            m_resourceTypeName = paramValue.trim();
        } else if (C_CONFIGURATION_SCHEMA.equalsIgnoreCase(paramName)) {
            m_schema = paramValue.trim();
        }
    }

    /**
     * @see org.opencms.file.types.I_CmsResourceType#createResource(org.opencms.file.CmsObject, org.opencms.db.CmsSecurityManager, java.lang.String, byte[], java.util.List)
     */
    public CmsResource createResource(
        CmsObject cms,
        CmsSecurityManager securityManager,
        String resourcename,
        byte[] content,
        List properties) throws CmsException {

        if ((m_schema != null) && ((content == null) || (content.length == 0))) {
            // unmarshal the content definition for the new resource
            CmsXmlContentDefinition contentDefinition = CmsXmlContentDefinition.unmarshal(cms, m_schema);

            // read the default locale for the new resource
            Locale locale = (Locale)OpenCms.getLocaleManager().getDefaultLocales(
                cms,
                CmsResource.getParentFolder(resourcename)
            ).get(0);

            // create the new content
            CmsXmlContent newContent = 
                new CmsXmlContent(contentDefinition, locale, OpenCms.getSystemInfo().getDefaultEncoding());
            content = newContent.marshal();
        }

        // create the new XML content resource
        CmsResource resource = securityManager.createResource(
            cms.getRequestContext(), 
            cms.getRequestContext().addSiteRoot(resourcename), 
            getTypeId(), 
            content, 
            properties);

        return resource;
    }

    /**
     * @see org.opencms.file.types.I_CmsResourceType#getCachePropertyDefault()
     */
    public String getCachePropertyDefault() {

        return "element;locale;";
    }

    /**
     * @see org.opencms.file.types.A_CmsResourceType#getConfiguration()
     */
    public ExtendedProperties getConfiguration() {

        ExtendedProperties result = new ExtendedProperties();
        result.put(I_CmsResourceType.C_CONFIGURATION_RESOURCE_TYPE_ID, new Integer(m_resourceType));
        result.put(I_CmsResourceType.C_CONFIGURATION_RESOURCE_TYPE_NAME, m_resourceTypeName);
        if (m_schema != null) {
            result.put(C_CONFIGURATION_SCHEMA, m_schema);
        }
        return result;
    }

    /**
     * @see org.opencms.file.types.I_CmsResourceType#getLoaderId()
     */
    public int getLoaderId() {

        return CmsXmlContentLoader.C_RESOURCE_LOADER_ID;
    }

    /**
     * @see org.opencms.file.types.I_CmsResourceType#getTypeId()
     */
    public int getTypeId() {

        return m_resourceType;
    }

    /**
     * @see org.opencms.file.types.A_CmsResourceType#getTypeName()
     */
    public String getTypeName() {

        return m_resourceTypeName;
    }

    /**
     * @see org.opencms.file.types.A_CmsResourceType#initConfiguration()
     */
    public void initConfiguration() throws CmsConfigurationException {

        // configuration must be complete for this resource type
        if ((m_resourceTypeName == null) || (m_resourceType <= 0)) {
            throw new CmsConfigurationException("Not all required configuration parameters available for resource type");
        }
    }

    /**
     * @see org.opencms.file.types.I_CmsResourceType#isDirectEditable()
     */
    public boolean isDirectEditable() {

        return true;
    }

    /**
     * @see org.opencms.file.types.I_CmsResourceType#writeFile(org.opencms.file.CmsObject, CmsSecurityManager, CmsFile)
     */
    public CmsFile writeFile(CmsObject cms, CmsSecurityManager securityManager, CmsFile resource) throws CmsException {

        // check if the user has write access and if resource is locked
        // done here so that all the XML operations are not performed if permissions not granted
        securityManager.checkPermissions(cms.getRequestContext(), resource, CmsPermissionSet.ACCESS_WRITE, true, CmsResourceFilter.ALL);
        // read the xml page, use the encoding set in the property       
        CmsXmlContent xmlContent = CmsXmlContentFactory.unmarshal(cms, resource, false);
        // validate the xml structure before writing the file         
        // an exception will be thrown if the structure is invalid
        xmlContent.validateXmlStructure(new CmsXmlEntityResolver(cms));
        // read the content-conversion property
        String contentConversion = CmsHtmlConverter.getConversionSettings(cms, resource);               
        xmlContent.setConversion(contentConversion);   
        // correct the HTML structure 
        resource = xmlContent.correctXmlStructure(cms);        
        // resolve the file mappings
        xmlContent.resolveAppInfo(cms);        
        // now write the file
        return super.writeFile(cms, securityManager, resource);
    }
}