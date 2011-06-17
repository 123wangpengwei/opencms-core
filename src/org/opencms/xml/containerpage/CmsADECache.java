/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/xml/containerpage/CmsADECache.java,v $
 * Date   : $Date: 2010/03/01 10:21:47 $
 * Version: $Revision: 1.6 $
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

import org.opencms.cache.CmsVfsCache;
import org.opencms.file.CmsResource;
import org.opencms.file.types.CmsResourceTypeXmlContainerPage;
import org.opencms.loader.CmsLoaderException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.monitor.CmsMemoryMonitor;
import org.opencms.util.CmsCollectionsGenericWrapper;
import org.opencms.util.CmsUUID;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;

/**
 * Cache object instance for simultaneously cache online and offline items.<p>
 * 
 * @author Michael Moossen
 * 
 * @version $Revision: 1.6 $ 
 * 
 * @since 7.6 
 */
public final class CmsADECache extends CmsVfsCache {

    /** The log to use (static for performance reasons).<p> */
    private static final Log LOG = CmsLog.getLog(CmsADECache.class);

    /** Container page type id. */
    private static int m_cntPageId;

    /** Cache for offline container pages. */
    private Map<String, CmsXmlContainerPage> m_containerPagesOffline;

    /** Cache for online container pages. */
    private Map<String, CmsXmlContainerPage> m_containerPagesOnline;

    /** Cache for offline sub containers. */
    private Map<String, CmsXmlSubContainer> m_subContainersOffline;

    /** Cache for online sub containers. */
    private Map<String, CmsXmlSubContainer> m_subContainersOnline;

    /**
     * Initializes the cache. Only intended to be called during startup.<p>
     * 
     * @param memMonitor the memory monitor instance
     * @param cacheSettings the system cache settings
     * 
     * @see org.opencms.main.OpenCmsCore#initConfiguration
     */
    public CmsADECache(CmsMemoryMonitor memMonitor, CmsADECacheSettings cacheSettings) {

        initialize(memMonitor, cacheSettings);
        registerEventListener();
    }

    /**
     * Returns the Container page type id.<p>
     * 
     * @return the Container page type id
     */
    private static int getCntPageId() {

        if (m_cntPageId == 0) {
            try {
                m_cntPageId = OpenCms.getResourceManager().getResourceType(
                    CmsResourceTypeXmlContainerPage.getStaticTypeName()).getTypeId();
            } catch (CmsLoaderException e) {
                // should not never ever happen
                m_cntPageId = CmsResourceTypeXmlContainerPage.getStaticTypeId();
            }
        }
        return m_cntPageId;
    }

    /**
     * Flushes the container pages cache.<p>
     * 
     * @param online if to flush the online or offline cache
     */
    public void flushContainerPages(boolean online) {

        if (online) {
            m_containerPagesOnline.clear();
        } else {
            m_containerPagesOffline.clear();
        }
    }

    /**
     * Flushes the sub containers cache.<p>
     * 
     * @param online if to flush the online or offline cache
     */
    public void flushSubContainers(boolean online) {

        if (online) {
            m_subContainersOnline.clear();
        } else {
            m_subContainersOffline.clear();
        }
    }

    /**
     * Returns the cached container page under the given key and for the given project.<p>
     * 
     * @param key the cache key
     * @param online if cached in online or offline project
     * 
     * @return the cached container page or <code>null</code> if not found
     */
    public CmsXmlContainerPage getCacheContainerPage(String key, boolean online) {

        CmsXmlContainerPage retValue;
        if (online) {
            retValue = m_containerPagesOnline.get(key);
            if (LOG.isDebugEnabled()) {
                if (retValue == null) {
                    LOG.debug(Messages.get().getBundle().key(
                        Messages.LOG_DEBUG_CACHE_MISSED_ONLINE_1,
                        new Object[] {key}));

                } else {
                    LOG.debug(Messages.get().getBundle().key(
                        Messages.LOG_DEBUG_CACHE_MATCHED_ONLINE_2,
                        new Object[] {key, retValue}));
                }
            }
        } else {
            retValue = m_containerPagesOffline.get(key);
            if (LOG.isDebugEnabled()) {
                if (retValue == null) {
                    LOG.debug(Messages.get().getBundle().key(
                        Messages.LOG_DEBUG_CACHE_MISSED_OFFLINE_1,
                        new Object[] {key}));

                } else {
                    LOG.debug(Messages.get().getBundle().key(
                        Messages.LOG_DEBUG_CACHE_MATCHED_OFFLINE_2,
                        new Object[] {key, retValue}));
                }
            }
        }
        return retValue;
    }

    /**
     * Returns the cache key for the given parameters.<p>
     * 
     * @param structureId the container page's structure id
     * @param keepEncoding if to keep the encoding while unmarshalling
     * 
     * @return the cache key for the given container page and parameters
     */
    public String getCacheKey(CmsUUID structureId, boolean keepEncoding) {

        return structureId.toString() + "_" + keepEncoding;
    }

    /**
     * Returns the cached sub container under the given key and for the given project.<p>
     * 
     * @param key the cache key
     * @param online if cached in online or offline project
     * 
     * @return the cached sub container or <code>null</code> if not found
     */
    public CmsXmlSubContainer getCacheSubContainer(String key, boolean online) {

        CmsXmlSubContainer retValue;
        if (online) {
            retValue = m_subContainersOnline.get(key);
            if (LOG.isDebugEnabled()) {
                if (retValue == null) {
                    LOG.debug(Messages.get().getBundle().key(
                        Messages.LOG_DEBUG_CACHE_MISSED_ONLINE_1,
                        new Object[] {key}));

                } else {
                    LOG.debug(Messages.get().getBundle().key(
                        Messages.LOG_DEBUG_CACHE_MATCHED_ONLINE_2,
                        new Object[] {key, retValue}));
                }
            }
        } else {
            retValue = m_subContainersOffline.get(key);
            if (LOG.isDebugEnabled()) {
                if (retValue == null) {
                    LOG.debug(Messages.get().getBundle().key(
                        Messages.LOG_DEBUG_CACHE_MISSED_OFFLINE_1,
                        new Object[] {key}));

                } else {
                    LOG.debug(Messages.get().getBundle().key(
                        Messages.LOG_DEBUG_CACHE_MATCHED_OFFLINE_2,
                        new Object[] {key, retValue}));
                }
            }
        }
        return retValue;
    }

    /**
     * Caches the given container page under the given key and for the given project.<p>
     * 
     * @param key the cache key
     * @param containerPage the object to cache
     * @param online if to cache in online or offline project
     */
    public void setCacheContainerPage(String key, CmsXmlContainerPage containerPage, boolean online) {

        if (online) {
            m_containerPagesOnline.put(key, containerPage);
            if (LOG.isDebugEnabled()) {
                LOG.debug(Messages.get().getBundle().key(
                    Messages.LOG_DEBUG_CACHE_SET_ONLINE_2,
                    new Object[] {key, containerPage}));
            }
        } else {
            m_containerPagesOffline.put(key, containerPage);
            if (LOG.isDebugEnabled()) {
                LOG.debug(Messages.get().getBundle().key(
                    Messages.LOG_DEBUG_CACHE_SET_OFFLINE_2,
                    new Object[] {key, containerPage}));
            }
        }
    }

    /**
     * Caches the given sub container under the given key and for the given project.<p>
     * 
     * @param key the cache key
     * @param subContainer the object to cache
     * @param online if to cache in online or offline project
     */
    public void setCacheSubContainer(String key, CmsXmlSubContainer subContainer, boolean online) {

        if (online) {
            m_subContainersOnline.put(key, subContainer);
            if (LOG.isDebugEnabled()) {
                LOG.debug(Messages.get().getBundle().key(
                    Messages.LOG_DEBUG_CACHE_SET_ONLINE_2,
                    new Object[] {key, subContainer}));
            }
        } else {
            m_subContainersOffline.put(key, subContainer);
            if (LOG.isDebugEnabled()) {
                LOG.debug(Messages.get().getBundle().key(
                    Messages.LOG_DEBUG_CACHE_SET_OFFLINE_2,
                    new Object[] {key, subContainer}));
            }
        }
    }

    /**
     * Removes the container page identified by its structure id from the cache.<p>
     * 
     * @param structureId the container page's structure id
     * @param online if online or offline
     */
    public void uncacheContainerPage(CmsUUID structureId, boolean online) {

        if (online) {
            m_containerPagesOnline.remove(getCacheKey(structureId, true));
            m_containerPagesOnline.remove(getCacheKey(structureId, false));
        } else {
            m_containerPagesOffline.remove(getCacheKey(structureId, true));
            m_containerPagesOffline.remove(getCacheKey(structureId, false));
        }
    }

    /**
     * Removes the sub container identified by its structure id from the cache.<p>
     * 
     * @param structureId the sub container's structure id
     * @param online if online or offline
     */
    public void uncacheSubContainer(CmsUUID structureId, boolean online) {

        if (online) {
            m_subContainersOnline.remove(getCacheKey(structureId, true));
            m_subContainersOnline.remove(getCacheKey(structureId, false));
        } else {
            m_subContainersOffline.remove(getCacheKey(structureId, true));
            m_subContainersOffline.remove(getCacheKey(structureId, false));
        }
    }

    /**
     * @see org.opencms.cache.CmsVfsCache#flush(boolean)
     */
    @Override
    protected void flush(boolean online) {

        flushContainerPages(online);
        flushSubContainers(online);
    }

    /**
     * @see org.opencms.cache.CmsVfsCache#uncacheResource(org.opencms.file.CmsResource)
     */
    @Override
    protected void uncacheResource(CmsResource resource) {

        if (resource == null) {
            LOG.warn(Messages.get().container(Messages.LOG_WARN_UNCACHE_NULL_0));
            return;
        }
        if (resource.getTypeId() == getCntPageId()) {
            // remove the resource cached by it's structure ID
            uncacheContainerPage(resource.getStructureId(), false);
        } else {
            uncacheSubContainer(resource.getStructureId(), false);
        }
    }

    /**
     * Initializes the caches.<p>
     * 
     * @param memMonitor the memory monitor instance
     * @param cacheSettings the system cache settings
     */
    private void initialize(CmsMemoryMonitor memMonitor, CmsADECacheSettings cacheSettings) {

        // container page caches
        Map<String, CmsXmlContainerPage> lruMapCntPage = CmsCollectionsGenericWrapper.createLRUMap(cacheSettings.getContainerPageOfflineSize());
        m_containerPagesOffline = Collections.synchronizedMap(lruMapCntPage);
        memMonitor.register(CmsADECache.class.getName() + ".containerPagesOffline", lruMapCntPage);

        lruMapCntPage = CmsCollectionsGenericWrapper.createLRUMap(cacheSettings.getContainerPageOnlineSize());
        m_containerPagesOnline = Collections.synchronizedMap(lruMapCntPage);
        memMonitor.register(CmsADECache.class.getName() + ".containerPagesOnline", lruMapCntPage);

        // container page caches
        Map<String, CmsXmlSubContainer> lruMapSubContainer = CmsCollectionsGenericWrapper.createLRUMap(cacheSettings.getSubContainerOfflineSize());
        m_subContainersOffline = Collections.synchronizedMap(lruMapSubContainer);
        memMonitor.register(CmsADECache.class.getName() + ".subContainersOffline", lruMapSubContainer);

        lruMapSubContainer = CmsCollectionsGenericWrapper.createLRUMap(cacheSettings.getSubContainerOnlineSize());
        m_subContainersOnline = Collections.synchronizedMap(lruMapSubContainer);
        memMonitor.register(CmsADECache.class.getName() + ".subContainersOnline", lruMapSubContainer);
    }
}
