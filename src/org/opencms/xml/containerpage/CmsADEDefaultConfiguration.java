/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/xml/containerpage/CmsADEDefaultConfiguration.java,v $
 * Date   : $Date: 2010/01/27 12:25:30 $
 * Version: $Revision: 1.5 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2009 Alkacon Software (http://www.alkacon.com)
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

package org.opencms.xml.containerpage;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.main.CmsException;
import org.opencms.main.CmsIllegalStateException;
import org.opencms.main.CmsLog;
import org.opencms.util.CmsStringUtil;

import java.util.List;

import javax.servlet.ServletRequest;

import org.apache.commons.logging.Log;

/**
 * Default implementation for the ADE configuration.<p>
 * 
 * List sizes are read from the user additional info, if not set used a fixed value of 10.<p>
 * 
 * New elements are read from a configuration file read by property.<p>
 * 
 * Search types are the same as the new elements.<p>
 * 
 * @author Michael Moossen
 * 
 * @version $Revision: 1.5 $ 
 * 
 * @since 7.6 
 */
public class CmsADEDefaultConfiguration implements I_CmsADEConfiguration {

    /** User additional info key constant. */
    public static final String ADDINFO_ADE_FAVORITE_LIST_SIZE = "ADE_FAVORITE_LIST_SIZE";

    /** User additional info key constant. */
    public static final String ADDINFO_ADE_RECENT_LIST_SIZE = "ADE_RECENT_LIST_SIZE";

    /** User additional info key constant. */
    public static final String ADDINFO_ADE_SEARCH_PAGE_SIZE = "ADE_SEARCH_PAGE_SIZE";

    /** Default favorite list size constant. */
    public static final int DEFAULT_FAVORITE_LIST_SIZE = 10;

    /** Default recent list size constant. */
    public static final int DEFAULT_RECENT_LIST_SIZE = 10;

    /** Default search page size constant. */
    public static final int DEFAULT_SEARCH_PAGE_SIZE = 10;

    /** The log to use (static for performance reasons).<p> */
    private static final Log LOG = CmsLog.getLog(CmsADEDefaultConfiguration.class);

    /**
     * @see org.opencms.xml.containerpage.I_CmsADEConfiguration#createNewElement(CmsObject, String, ServletRequest, String)
     */
    public CmsResource createNewElement(CmsObject cms, String cntPageUri, ServletRequest request, String type)
    throws CmsException {

        CmsConfigurationParser parser = getConfigurationParser(cms, cntPageUri);
        String newFileName = getNextNewFileName(cms, cntPageUri, request, type);
        cms.copyResource(parser.getConfiguration().get(type).getSourceFile(), newFileName);
        return cms.readResource(newFileName);
    }

    /**
     * @see org.opencms.xml.containerpage.I_CmsADEConfiguration#getCreatableElements(CmsObject, String, ServletRequest)
     */
    public List<CmsResource> getCreatableElements(CmsObject cms, String cntPageUri, ServletRequest request)
    throws CmsException {

        return getConfigurationParser(cms, cntPageUri).getNewElements();
    }

    /**
     * @see org.opencms.xml.containerpage.I_CmsADEConfiguration#getFavoriteListMaxSize(CmsObject)
     */
    public int getFavoriteListMaxSize(CmsObject cms) {

        Integer maxElems = (Integer)cms.getRequestContext().currentUser().getAdditionalInfo(
            ADDINFO_ADE_FAVORITE_LIST_SIZE);
        if (maxElems == null) {
            maxElems = new Integer(DEFAULT_FAVORITE_LIST_SIZE);
        }
        return maxElems.intValue();
    }

    /**
     * @see org.opencms.xml.containerpage.I_CmsADEConfiguration#getNextNewFileName(CmsObject, String, ServletRequest, String)
     */
    public synchronized String getNextNewFileName(CmsObject cms, String cntPageUri, ServletRequest request, String type)
    throws CmsException {

        CmsConfigurationParser parser = getConfigurationParser(cms, cntPageUri);
        return parser.getNewFileName(cms, parser.getConfiguration().get(type).getDestination());
    }

    /**
     * @see org.opencms.xml.containerpage.I_CmsADEConfiguration#getRecentListMaxSize(CmsObject)
     */
    public int getRecentListMaxSize(CmsObject cms) {

        Integer maxElems = (Integer)cms.getRequestContext().currentUser().getAdditionalInfo(
            ADDINFO_ADE_RECENT_LIST_SIZE);
        if (maxElems == null) {
            maxElems = new Integer(DEFAULT_RECENT_LIST_SIZE);
        }
        return maxElems.intValue();
    }

    /**
     * @see org.opencms.xml.containerpage.I_CmsADEConfiguration#getSearchableResourceTypes(CmsObject, String, ServletRequest)
     */
    public List<CmsResource> getSearchableResourceTypes(CmsObject cms, String cntPageUri, ServletRequest request)
    throws CmsException {

        return getCreatableElements(cms, cntPageUri, request);
    }

    /**
     * @see org.opencms.xml.containerpage.I_CmsADEConfiguration#getSearchPageSize(CmsObject)
     */
    public int getSearchPageSize(CmsObject cms) {

        Integer maxElems = (Integer)cms.getRequestContext().currentUser().getAdditionalInfo(
            ADDINFO_ADE_SEARCH_PAGE_SIZE);
        if (maxElems == null) {
            maxElems = new Integer(DEFAULT_SEARCH_PAGE_SIZE);
        }
        return maxElems.intValue();
    }

    /**
     * Returns the configuration file to use.<p>
     * 
     * @param cms the current cms context
     * @param containerPageUri the container page uri
     * 
     * @return the configuration file to use, or <code>null</code> if not found
     */
    protected CmsResource getConfigurationFile(CmsObject cms, String containerPageUri) {

        String cfgPath = null;
        try {
            // get the resource type configuration file from the vfs tree
            cfgPath = cms.readPropertyObject(containerPageUri, CmsPropertyDefinition.PROPERTY_ADE_CNTPAGE_CONFIG, true).getValue();
        } catch (CmsException e) {
            // should never happen 
            LOG.error(e.getLocalizedMessage(), e);
        }

        if (CmsStringUtil.isEmptyOrWhitespaceOnly(cfgPath)) {
            // if not found try at the template
            try {
                // retrieve the template uri
                String templateUri = cms.readPropertyObject(
                    containerPageUri,
                    CmsPropertyDefinition.PROPERTY_TEMPLATE,
                    true).getValue();
                // get the resource type configuration file from the template itself
                cfgPath = cms.readPropertyObject(templateUri, CmsPropertyDefinition.PROPERTY_ADE_CNTPAGE_CONFIG, true).getValue();
            } catch (CmsException e) {
                // should never happen
                LOG.error(e.getLocalizedMessage(), e);
            }
        }

        if (CmsStringUtil.isEmptyOrWhitespaceOnly(cfgPath)) {
            // configuration could not be found
            LOG.warn(Messages.get().getBundle().key(
                Messages.ERR_CONFIG_NOT_SET_2,
                containerPageUri,
                CmsPropertyDefinition.PROPERTY_ADE_CNTPAGE_CONFIG));
            return null;
        }

        try {
            // read configuration file
            return cms.readResource(cfgPath);
        } catch (Exception e) {
            throw new CmsIllegalStateException(Messages.get().container(
                Messages.ERR_CONFIG_NOT_FOUND_3,
                containerPageUri,
                CmsPropertyDefinition.PROPERTY_ADE_CNTPAGE_CONFIG,
                cfgPath));
        }
    }

    /**
     * Returns the configuration parser instance.<p>
     * 
     * @param cms the current CMS context
     * @param cntPageUri the container page URI
     * 
     * @return the configuration parser instance
     * 
     * @throws CmsException if something goes wrong
     */
    private CmsConfigurationParser getConfigurationParser(CmsObject cms, String cntPageUri) throws CmsException {

        CmsResource cfg = getConfigurationFile(cms, cntPageUri);
        return new CmsConfigurationParser(cms, cfg);
    }
}
