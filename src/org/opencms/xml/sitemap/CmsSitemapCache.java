/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/xml/sitemap/Attic/CmsSitemapCache.java,v $
 * Date   : $Date: 2010/02/10 14:27:54 $
 * Version: $Revision: 1.9 $
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

package org.opencms.xml.sitemap;

import org.opencms.cache.CmsVfsCache;
import org.opencms.file.CmsResource;
import org.opencms.file.types.CmsResourceTypeXmlSitemap;
import org.opencms.main.CmsLog;
import org.opencms.monitor.CmsMemoryMonitor;
import org.opencms.util.CmsCollectionsGenericWrapper;
import org.opencms.util.CmsUUID;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;

/**
 * Cache object instance for simultaneously cache online and offline items.<p>
 * 
 * @author Michael Moossen
 * 
 * @version $Revision: 1.9 $ 
 * 
 * @since 7.6 
 */
public final class CmsSitemapCache extends CmsVfsCache {

    /** The log to use (static for performance reasons).<p> */
    private static final Log LOG = CmsLog.getLog(CmsSitemapCache.class);

    /** The offline default sitemap properties. */
    private Map<String, String> m_defPropsOffline;

    /** The online default sitemap properties. */
    private Map<String, String> m_defPropsOnline;

    /** Cache for offline sitemap documents. */
    private Map<String, CmsXmlSitemap> m_documentsOffline;

    /** Cache for online sitemap documents. */
    private Map<String, CmsXmlSitemap> m_documentsOnline;

    /** Cache for missing offline URIs. */
    private Map<String, Boolean> m_missingUrisOffline;

    /** Cache for missing online URIs. */
    private Map<String, Boolean> m_missingUrisOnline;

    /** Cache for offline site entries. */
    private Map<String, CmsSitemapEntry> m_urisOffline;

    /** Cache for online site entries. */
    private Map<String, CmsSitemapEntry> m_urisOnline;

    /**
     * Initializes the cache. Only intended to be called during startup.<p>
     * 
     * @param memMonitor the memory monitor instance
     * @param cacheSettings the system cache settings
     * 
     * @see org.opencms.main.OpenCmsCore#initConfiguration
     */
    public CmsSitemapCache(CmsMemoryMonitor memMonitor, CmsSitemapCacheSettings cacheSettings) {

        initCaches(memMonitor, cacheSettings);
        registerEventListener();
    }

    /**
     * Returns the cache key for the given parameters.<p>
     * 
     * @param structureId the sitemap's structure id
     * @param keepEncoding if to keep the encoding while unmarshalling
     * 
     * @return the cache key for the given sitemap and parameters
     */
    public String getCacheKey(CmsUUID structureId, boolean keepEncoding) {

        return structureId.toString() + "_" + keepEncoding;
    }

    /**
     * Returns the cached default properties.<p>
     * 
     * @param online if online or offline
     * 
     * @return the cached default properties
     */
    public Map<String, String> getDefaultProps(boolean online) {

        if (online) {
            return m_defPropsOnline;
        } else {
            return m_defPropsOffline;
        }
    }

    /**
     * Returns the cached sitemap under the given key and for the given project.<p>
     * 
     * @param key the cache key
     * @param online if cached in online or offline project
     * 
     * @return the cached sitemap or <code>null</code> if not found
     */
    public CmsXmlSitemap getDocument(String key, boolean online) {

        CmsXmlSitemap retValue;
        if (online) {
            retValue = m_documentsOnline.get(key);
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
            retValue = m_documentsOffline.get(key);
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
     * Returns the content of the missing URIs cache for the given parameters.<p>
     * 
     * @param path the cache key
     * @param online if online or offline
     * 
     * @return the content of the missing URIs cache
     */
    public Boolean getMissingUri(String path, boolean online) {

        if (online) {
            return m_missingUrisOnline.get(path);
        } else {
            return m_missingUrisOffline.get(path);
        }
    }

    /**
     * Returns the content of the URIs cache for the given parameters.<p>
     * 
     * @param path the cache key
     * @param online if online or offline
     * 
     * @return the content of the URIs cache
     */
    public CmsSitemapEntry getUri(String path, boolean online) {

        if (online) {
            return m_urisOnline.get(path);
        } else {
            return m_urisOffline.get(path);
        }
    }

    /**
     * Sets the cached default properties.<p>
     * 
     * @param props the properties to cache
     * @param online if online or offline
     */
    public void setDefaultProps(Map<String, String> props, boolean online) {

        if (online) {
            m_defPropsOnline = new HashMap<String, String>(props);
        } else {
            m_defPropsOffline = new HashMap<String, String>(props);
        }
    }

    /**
     * Caches the given sitemap under the given key and for the given project.<p>
     * 
     * @param key the cache key
     * @param sitemap the object to cache
     * @param online if to cache in online or offline project
     */
    public void setDocument(String key, CmsXmlSitemap sitemap, boolean online) {

        if (online) {
            m_documentsOnline.put(key, sitemap);
            if (LOG.isDebugEnabled()) {
                LOG.debug(Messages.get().getBundle().key(
                    Messages.LOG_DEBUG_CACHE_SET_ONLINE_2,
                    new Object[] {key, sitemap}));
            }
        } else {
            m_documentsOffline.put(key, sitemap);
            if (LOG.isDebugEnabled()) {
                LOG.debug(Messages.get().getBundle().key(
                    Messages.LOG_DEBUG_CACHE_SET_OFFLINE_2,
                    new Object[] {key, sitemap}));
            }
        }
    }

    /**
     * Sets the missing URIs cache for the given parameters.<p>
     * 
     * @param path the cache key
     * @param online if online or offline
     */
    public void setMissingUri(String path, boolean online) {

        if (online) {
            m_missingUrisOnline.put(path, Boolean.TRUE);
        } else {
            m_missingUrisOffline.put(path, Boolean.TRUE);
        }
    }

    /**
     * Sets the URI cache for the given parameters.<p>
     * 
     * @param path the cache key
     * @param entry the sitemap entry to cache
     * @param online if online or offline
     */
    public void setUri(String path, CmsSitemapEntry entry, boolean online) {

        if (online) {
            m_urisOnline.put(path, entry);
        } else {
            m_urisOffline.put(path, entry);
        }
        // if caching by path, also cache by id
        if (!CmsUUID.isValidUUID(path)) {
            setUri(entry.getId().toString(), entry, online);
        }
    }

    /**
     * @see org.opencms.cache.CmsVfsCache#flush(boolean)
     */
    @Override
    protected void flush(boolean online) {

        if (online) {
            m_documentsOnline.clear();
            m_missingUrisOnline.clear();
            m_urisOnline.clear();
            m_defPropsOnline = null;
        } else {
            m_documentsOffline.clear();
            m_missingUrisOffline.clear();
            m_urisOffline.clear();
            m_defPropsOffline = null;
        }
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

        // if sitemap schema changed
        if (resource.getRootPath().equals(CmsResourceTypeXmlSitemap.SCHEMA)) {
            // flush offline default properties 
            m_defPropsOffline = null;
            return;
        }

        // flush docs
        m_documentsOffline.remove(getCacheKey(resource.getStructureId(), true));
        m_documentsOffline.remove(getCacheKey(resource.getStructureId(), false));

        // we care only more if the modified resource is a sitemap
        if (!CmsResourceTypeXmlSitemap.isSitemap(resource)) {
            return;
        }

        // flush all uri's
        m_urisOffline.clear();
        m_missingUrisOffline.clear();
    }

    /**
     * Initializes the caches.<p>
     * 
     * @param memMonitor the memory monitor instance
     * @param cacheSettings the system cache settings
     */
    private void initCaches(CmsMemoryMonitor memMonitor, CmsSitemapCacheSettings cacheSettings) {

        Map<String, CmsXmlSitemap> lruMapDocs = CmsCollectionsGenericWrapper.createLRUMap(cacheSettings.getDocumentOfflineSize());
        m_documentsOffline = Collections.synchronizedMap(lruMapDocs);
        memMonitor.register(CmsSitemapCache.class.getName() + ".sitemapDocsOffline", lruMapDocs);

        lruMapDocs = CmsCollectionsGenericWrapper.createLRUMap(cacheSettings.getDocumentOnlineSize());
        m_documentsOnline = Collections.synchronizedMap(lruMapDocs);
        memMonitor.register(CmsSitemapCache.class.getName() + ".sitemapDocsOnline", lruMapDocs);

        Map<String, CmsSitemapEntry> lruMapUri = CmsCollectionsGenericWrapper.createLRUMap(cacheSettings.getUriOfflineSize());
        m_urisOffline = Collections.synchronizedMap(lruMapUri);
        memMonitor.register(CmsSitemapResourceHandler.class.getName() + ".urisOffline", lruMapUri);

        lruMapUri = CmsCollectionsGenericWrapper.createLRUMap(cacheSettings.getUriOnlineSize());
        m_urisOnline = Collections.synchronizedMap(lruMapUri);
        memMonitor.register(CmsSitemapResourceHandler.class.getName() + ".urisOnline", lruMapUri);

        Map<String, Boolean> lruMapMissed = CmsCollectionsGenericWrapper.createLRUMap(cacheSettings.getMissingUriOfflineSize());
        m_missingUrisOffline = Collections.synchronizedMap(lruMapMissed);
        memMonitor.register(CmsSitemapResourceHandler.class.getName() + ".missingUrisOffline", lruMapMissed);

        lruMapMissed = CmsCollectionsGenericWrapper.createLRUMap(cacheSettings.getMissingUriOnlineSize());
        m_missingUrisOnline = Collections.synchronizedMap(lruMapMissed);
        memMonitor.register(CmsSitemapResourceHandler.class.getName() + ".missingUrisOnline", lruMapMissed);
    }
}
