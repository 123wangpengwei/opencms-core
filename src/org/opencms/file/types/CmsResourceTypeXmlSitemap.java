/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/file/types/Attic/CmsResourceTypeXmlSitemap.java,v $
 * Date   : $Date: 2010/05/26 12:11:41 $
 * Version: $Revision: 1.14 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) 2002 - 2009 Alkacon Software GmbH (http://www.alkacon.com)
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

package org.opencms.file.types;

import org.opencms.configuration.CmsConfigurationException;
import org.opencms.db.CmsSecurityManager;
import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsRequestContext;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.loader.CmsXmlSitemapLoader;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.relations.CmsLink;
import org.opencms.relations.I_CmsLinkParseable;
import org.opencms.security.CmsPermissionSet;
import org.opencms.xml.CmsXmlContentDefinition;
import org.opencms.xml.sitemap.CmsXmlSitemap;
import org.opencms.xml.sitemap.CmsXmlSitemapFactory;

import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;

/**
 * Resource type descriptor for the type "sitemap".<p>
 *
 * It is just a xml content with a fixed schema and id.<p>
 * 
 * @author Michael Moossen 
 * 
 * @version $Revision: 1.14 $ 
 * 
 * @since 7.6 
 */
public class CmsResourceTypeXmlSitemap extends CmsResourceTypeXmlContent {

    /** Fixed detail page for sitemap pages. */
    public static final String DETAIL_PAGE = "/system/modules/org.opencms.ade.sitemap/sitemap.jsp";

    /** Fixed schema for sitemap pages. */
    public static final String SCHEMA = "/system/modules/org.opencms.ade.sitemap/schemas/sitemap.xsd";

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsResourceTypeXmlSitemap.class);

    /** Indicates that the static configuration of the resource type has been frozen. */
    private static boolean m_staticFrozen;

    /** The type id of this resource type. */
    private static final int RESOURCE_TYPE_ID = 15;

    /** The name of this resource type. */
    private static final String RESOURCE_TYPE_NAME = "sitemap";

    /**
     * Default constructor that sets the fixed schema for container pages.<p>
     */
    public CmsResourceTypeXmlSitemap() {

        super();
        m_typeName = RESOURCE_TYPE_NAME;
        m_typeId = CmsResourceTypeXmlSitemap.RESOURCE_TYPE_ID;
        addConfigurationParameter(CONFIGURATION_SCHEMA, SCHEMA);
        try {
            addDefaultProperty(new CmsProperty(CmsPropertyDefinition.PROPERTY_TEMPLATE_ELEMENTS, null, DETAIL_PAGE));
        } catch (CmsConfigurationException e) {
            // should never happen
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Returns the static type id of this (default) resource type.<p>
     * 
     * @return the static type id of this (default) resource type
     */
    public static int getStaticTypeId() {

        return RESOURCE_TYPE_ID;
    }

    /**
     * Returns the static type name of this (default) resource type.<p>
     * 
     * @return the static type name of this (default) resource type
     */
    public static String getStaticTypeName() {

        return RESOURCE_TYPE_NAME;
    }

    /**
     * Returns <code>true</code> in case the given resource is a sitemap.<p>
     * 
     * Internally this checks if the type id for the given resource is 
     * identical type id of the sitemap.<p>
     * 
     * @param resource the resource to check
     * 
     * @return <code>true</code> in case the given resource is a sitemap
     */
    public static boolean isSitemap(CmsResource resource) {

        boolean result = false;
        if (resource != null) {
            result = (resource.getTypeId() == RESOURCE_TYPE_ID);
        }
        return result;
    }

    /**
     * @see org.opencms.file.types.CmsResourceTypeXmlContent#createResource(org.opencms.file.CmsObject, org.opencms.db.CmsSecurityManager, java.lang.String, byte[], java.util.List)
     */
    @Override
    public CmsResource createResource(
        CmsObject cms,
        CmsSecurityManager securityManager,
        String resourcename,
        byte[] content,
        List<CmsProperty> properties) throws CmsException {

        boolean hasModelUri = false;
        CmsXmlSitemap newContent = null;
        if ((getSchema() != null) && ((content == null) || (content.length == 0))) {
            // unmarshal the content definition for the new resource
            CmsXmlContentDefinition contentDefinition = CmsXmlContentDefinition.unmarshal(cms, getSchema());

            // read the default locale for the new resource
            Locale locale = OpenCms.getLocaleManager().getDefaultLocales(cms, CmsResource.getParentFolder(resourcename)).get(
                0);

            String modelUri = (String)cms.getRequestContext().getAttribute(CmsRequestContext.ATTRIBUTE_MODEL);

            // must set URI of OpenCms user context to parent folder of created resource, 
            // in order to allow reading of properties for default values
            CmsObject newCms = OpenCms.initCmsObject(cms);
            newCms.getRequestContext().setUri(CmsResource.getParentFolder(resourcename));
            if (modelUri != null) {
                // create the new content from the model file
                newContent = CmsXmlSitemapFactory.createDocument(newCms, locale, modelUri);
                hasModelUri = true;
            } else {
                // create the new content from the content definition
                newContent = CmsXmlSitemapFactory.createDocument(
                    newCms,
                    locale,
                    OpenCms.getSystemInfo().getDefaultEncoding(),
                    contentDefinition);
            }
            // get the bytes from the created content
            content = newContent.marshal();
        }

        // now create the resource using the super class
        CmsResource resource = super.createResource(cms, securityManager, resourcename, content, properties);

        // a model file was used, call the content handler for post-processing
        if (hasModelUri) {
            newContent = CmsXmlSitemapFactory.unmarshal(cms, resource);
            resource = newContent.getContentDefinition().getContentHandler().prepareForWrite(
                cms,
                newContent,
                newContent.getFile());
        }

        return resource;
    }

    /**
     * @see org.opencms.file.types.CmsResourceTypeXmlContent#getLoaderId()
     */
    @Override
    public int getLoaderId() {

        return CmsXmlSitemapLoader.RESOURCE_LOADER_ID;
    }

    /**
     * @see org.opencms.file.types.A_CmsResourceType#initConfiguration(java.lang.String, java.lang.String, String)
     */
    @Override
    public void initConfiguration(String name, String id, String className) throws CmsConfigurationException {

        if ((OpenCms.getRunLevel() > OpenCms.RUNLEVEL_2_INITIALIZING) && m_staticFrozen) {
            // configuration already frozen
            throw new CmsConfigurationException(Messages.get().container(
                Messages.ERR_CONFIG_FROZEN_3,
                this.getClass().getName(),
                getStaticTypeName(),
                new Integer(getStaticTypeId())));
        }

        if (!RESOURCE_TYPE_NAME.equals(name)) {
            // default resource type MUST have default name
            throw new CmsConfigurationException(Messages.get().container(
                Messages.ERR_INVALID_RESTYPE_CONFIG_NAME_3,
                this.getClass().getName(),
                RESOURCE_TYPE_NAME,
                name));
        }

        // freeze the configuration
        m_staticFrozen = true;

        super.initConfiguration(RESOURCE_TYPE_NAME, id, className);
    }

    /**
     * @see org.opencms.file.types.CmsResourceTypeXmlContent#writeFile(org.opencms.file.CmsObject, org.opencms.db.CmsSecurityManager, org.opencms.file.CmsFile)
     */
    @Override
    public CmsFile writeFile(CmsObject cms, CmsSecurityManager securityManager, CmsFile resource) throws CmsException {

        // check if the user has write access and if resource is locked
        // done here so that all the XML operations are not performed if permissions not granted
        securityManager.checkPermissions(
            cms.getRequestContext(),
            resource,
            CmsPermissionSet.ACCESS_WRITE,
            true,
            CmsResourceFilter.ALL);
        // read the XML content, use the encoding set in the property       
        CmsXmlSitemap xmlContent = CmsXmlSitemapFactory.unmarshal(cms, resource, false);
        // call the content handler for post-processing
        resource = xmlContent.getContentDefinition().getContentHandler().prepareForWrite(cms, xmlContent, resource);

        // now write the file
        CmsFile file = securityManager.writeFile(cms.getRequestContext(), resource);
        I_CmsResourceType type = getResourceType(file);
        // update the relations after writing!!
        List<CmsLink> links = null;
        if (type instanceof I_CmsLinkParseable) { // this check is needed because of type change
            // if the new type is link parseable
            links = ((I_CmsLinkParseable)type).parseLinks(cms, file);
        }
        // this has to be always executed, even if not link parseable to remove old links
        securityManager.updateRelationsForResource(cms.getRequestContext(), file, links);
        return file;
    }
}