/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/search/CmsSearchManager.java,v $
 * Date   : $Date: 2005/07/28 15:53:10 $
 * Version: $Revision: 1.50 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
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

package org.opencms.search;

import org.opencms.db.CmsPublishedResource;
import org.opencms.file.CmsObject;
import org.opencms.main.CmsEvent;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.I_CmsEventListener;
import org.opencms.main.OpenCms;
import org.opencms.report.CmsLogReport;
import org.opencms.report.I_CmsReport;
import org.opencms.scheduler.I_CmsScheduledJob;
import org.opencms.search.documents.I_CmsDocumentFactory;
import org.opencms.search.documents.I_CmsTermHighlighter;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.CmsUUID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.Similarity;

/**
 * Implements the general management and configuration of the search and 
 * indexing facilities in OpenCms.<p>
 * 
 * @author Carsten Weinholz 
 * @author Thomas Weckert  
 * 
 * @version $Revision: 1.50 $ 
 * 
 * @since 6.0.0 
 */
public class CmsSearchManager implements I_CmsScheduledJob, I_CmsEventListener {

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsSearchManager.class);

    /** The Admin cms object to index Cms resources. */
    private CmsObject m_adminCms;

    /** Configured analyzers for languages using &lt;analyzer&gt;. */
    private HashMap m_analyzers;

    /** A map of document factory configurations. */
    private Map m_documentTypeConfigs;

    /** A map of document factories keyed by their matching Cms resource types and/or mimetypes. */
    private Map m_documentTypes;

    /** The class used to highlight the search terms in the excerpt of a search result. */
    private I_CmsTermHighlighter m_highlighter;

    /** A list of search indexes. */
    private List m_indexes;

    /** Configured index sources. */
    private Map m_indexSources;

    /** The max. char. length of the excerpt in the search result. */
    private int m_maxExcerptLength;

    /** Path to index files below WEB-INF/. */
    private String m_path;

    /** The cache for storing search results. */
    private Map m_resultCache;

    /** The result cache size. */
    private String m_resultCacheSize;

    /** Timeout for abandoning indexing thread. */
    private String m_timeout;

    /**
     * Default constructer when called as cron job.<p>
     */
    public CmsSearchManager() {

        m_documentTypes = new HashMap();
        m_documentTypeConfigs = new HashMap();
        m_analyzers = new HashMap();
        m_indexes = new ArrayList();
        m_indexSources = new HashMap();

        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().key(Messages.INIT_START_SEARCH_CONFIG_0));
        }
    }

    /**
     * Adds an analyzer.<p>
     * 
     * @param analyzer an analyzer
     */
    public void addAnalyzer(CmsSearchAnalyzer analyzer) {

        m_analyzers.put(analyzer.getLocale(), analyzer);

        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().key(
                Messages.INIT_ADD_ANALYZER_2,
                analyzer.getLocale(),
                analyzer.getClassName()));
        }
    }

    /**
     * Adds a document type.<p>
     * 
     * @param documentType a document type
     */
    public void addDocumentTypeConfig(CmsSearchDocumentType documentType) {

        m_documentTypeConfigs.put(documentType.getName(), documentType);

        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().key(
                Messages.INIT_SEARCH_DOC_TYPES_2,
                documentType.getName(),
                documentType.getClassName()));
        }
    }

    /**
     * Adds a search index configuration.<p>
     * 
     * @param searchIndex a search index configuration
     */
    public void addSearchIndex(CmsSearchIndex searchIndex) {

        m_indexes.add(searchIndex);

        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().key(
                Messages.INIT_ADD_SEARCH_INDEX_2,
                searchIndex.getName(),
                searchIndex.getProject()));
        }

    }

    /**
     * Adds a search index source configuration.<p>
     * 
     * @param searchIndexSource a search index source configuration
     */
    public void addSearchIndexSource(CmsSearchIndexSource searchIndexSource) {

        m_indexSources.put(searchIndexSource.getName(), searchIndexSource);

        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().key(
                Messages.INIT_SEARCH_INDEX_SOURCE_2,
                searchIndexSource.getName(),
                searchIndexSource.getIndexerClassName()));
        }

    }

    /**
     * Implements the event listener of this class.<p>
     * 
     * @see org.opencms.main.I_CmsEventListener#cmsEvent(org.opencms.main.CmsEvent)
     */
    public void cmsEvent(CmsEvent event) {

        switch (event.getType()) {
            case I_CmsEventListener.EVENT_CLEAR_CACHES:
                if (m_resultCache != null) {
                    m_resultCache.clear();
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug(Messages.get().key(Messages.LOG_EVENT_CLEAR_CACHES_0));
                }
                break;
            case I_CmsEventListener.EVENT_PUBLISH_PROJECT:
                // event data contains a list of the published resources
                CmsUUID publishHistoryId = new CmsUUID((String)event.getData().get(I_CmsEventListener.KEY_PUBLISHID));
                if (LOG.isDebugEnabled()) {
                    LOG.debug(Messages.get().key(Messages.LOG_EVENT_PUBLISH_PROJECT_1, publishHistoryId));
                }
                I_CmsReport report = (I_CmsReport)event.getData().get(I_CmsEventListener.KEY_REPORT);
                updateAllIndexes(m_adminCms, publishHistoryId, report);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(Messages.get().key(Messages.LOG_EVENT_PUBLISH_PROJECT_FINISHED_1, publishHistoryId));
                }
                break;
            default:
        // no operation
        }
    }

    /**
     * Returns an unmodifiable view (read-only) of the Analyzers Map.<p>
     *
     * @return an unmodifiable view (read-only) of the Analyzers Map
     */
    public Map getAnalyzers() {

        return Collections.unmodifiableMap(m_analyzers);
    }

    /**
     * Returns the CmsSearchAnalyzer Object.<p>
     * @param locale unique locale key to specify the CmsSearchAnalyzer in HashMap
     * @return the CmsSearchAnalyzer Object
     */
    public CmsSearchAnalyzer getCmsSearchAnalyzer(String locale) {

        return (CmsSearchAnalyzer)m_analyzers.get(locale);
    }

    /**
     * Returns the name of the directory below WEB-INF/ where the search indexes are stored.<p>
     * 
     * @return the name of the directory below WEB-INF/ where the search indexes are stored
     */
    public String getDirectory() {

        return m_path;
    }

    /**
     * Returns a document type config.<p>
     * 
     * @param name the name of the document type config
     * @return the document type config.
     */
    public CmsSearchDocumentType getDocumentTypeConfig(String name) {

        return (CmsSearchDocumentType)m_documentTypeConfigs.get(name);
    }

    /**
     * Returns an unmodifiable view (read-only) of the DocumentTypeConfigs Map.<p>
     *
     * @return an unmodifiable view (read-only) of the DocumentTypeConfigs Map
     */
    public Map getDocumentTypeConfigs() {

        return Collections.unmodifiableMap(m_documentTypeConfigs);
    }

    /**
     * Returns the highlighter.<p>
     * 
     * @return the highlighter
     */
    public I_CmsTermHighlighter getHighlighter() {

        return m_highlighter;
    }

    /**
     * Returns the index belonging to the passed name.<p>
     * The index must exist already.
     * 
     * @param indexName then name of the index
     * @return an object representing the desired index
     */
    public CmsSearchIndex getIndex(String indexName) {

        for (int i = 0, n = m_indexes.size(); i < n; i++) {
            CmsSearchIndex searchIndex = (CmsSearchIndex)m_indexes.get(i);

            if (indexName.equalsIgnoreCase(searchIndex.getName())) {
                return searchIndex;
            }
        }

        return null;
    }

    /**
     * Returns the names of all configured indexes.<p>
     * 
     * @return list of names
     */
    public List getIndexNames() {

        List indexNames = new ArrayList();
        for (int i = 0, n = m_indexes.size(); i < n; i++) {
            indexNames.add(((CmsSearchIndex)m_indexes.get(i)).getName());
        }

        return indexNames;
    }

    /**
     * Returns a search index source for a specified source name.<p>
     * 
     * @param sourceName the name of the index source
     * @return a search index source
     */
    public CmsSearchIndexSource getIndexSource(String sourceName) {

        return (CmsSearchIndexSource)m_indexSources.get(sourceName);
    }

    /**
     * Returns the max. excerpt length.<p>
     *
     * @return the max excerpt length
     */
    public int getMaxExcerptLength() {

        return m_maxExcerptLength;
    }

    /**
     * Returns the result cache size.<p>
     *
     * @return the result cache size
     */
    public String getResultCacheSize() {

        return m_resultCacheSize;
    }

    /**
     * Returns an unmodifiable list of all configured <code>{@link CmsSearchIndex}</code> instances.<p>
     * 
     * @return an unmodifiable list of all configured <code>{@link CmsSearchIndex}</code> instances
     */
    public List getSearchIndexes() {

        return Collections.unmodifiableList(m_indexes);
    }

    /**
     * Returns an unmodifiable view (read-only) of the SearchIndexSources Map.<p>
     * 
     * @return an unmodifiable view (read-only) of the SearchIndexSources Map
     */
    public Map getSearchIndexSources() {

        return Collections.unmodifiableMap(m_indexSources);
    }

    /**
     * Returns the timeout to abandon threads indexing a resource.<p>
     *
     * @return the timeout to abandon threads indexing a resource
     */
    public String getTimeout() {

        return m_timeout;
    }

    /**
     * Initializes the search manager.<p>
     * 
     * @param cms the cms object
     */
    public void initialize(CmsObject cms) {

        // store the Admin cms to index Cms resources
        m_adminCms = cms;

        // init. the search result cache
        LRUMap hashMap = new LRUMap(Integer.parseInt(m_resultCacheSize));
        m_resultCache = Collections.synchronizedMap(hashMap);

        if (OpenCms.getMemoryMonitor().enabled()) {
            OpenCms.getMemoryMonitor().register(this.getClass().getName() + ".m_resultCache", hashMap);
        }

        initAvailableDocumentTypes();
        initSearchIndexes();

        // register the modified default similarity implementation
        Similarity.setDefault(new CmsSearchSimilarity());

        // register this object as event listener
        OpenCms.addCmsEventListener(this, new int[] {
            I_CmsEventListener.EVENT_CLEAR_CACHES,
            I_CmsEventListener.EVENT_PUBLISH_PROJECT});
    }

    /**
     * Updates the indexes from as a scheduled job.<p> 
     * 
     * @param cms the OpenCms user context to use when reading resources from the VFS
     * @param parameters the parameters for the scheduled job
     * 
     * @throws Exception if something goes wrong
     * 
     * @return the String to write in the scheduler log
     * 
     * @see org.opencms.scheduler.I_CmsScheduledJob#launch(org.opencms.file.CmsObject, java.util.Map)
     */
    public final String launch(CmsObject cms, Map parameters) throws Exception {

        CmsSearchManager manager = OpenCms.getSearchManager();

        I_CmsReport report = null;
        boolean writeLog = Boolean.valueOf((String)parameters.get("writeLog")).booleanValue();

        if (writeLog) {
            report = new CmsLogReport(cms.getRequestContext().getLocale(), CmsSearchManager.class);
        }

        int todo = 0;
        // TODO: Add parameter to allow a list of index names to be specified

        long startTime = System.currentTimeMillis();
        manager.rebuildAllIndexes(report);
        long runTime = System.currentTimeMillis() - startTime;

        String finishMessage = Messages.get().key(
            Messages.LOG_REBUILD_INDEXES_FINISHED_1,
            CmsStringUtil.formatRuntime(runTime));
        if (LOG.isInfoEnabled()) {
            LOG.info(finishMessage);
        }
        return finishMessage;
    }

    /**
     * Rebuilds (if required creates) all configured indexes.<p>
     * 
     * @param report the report object to write messages (or <code>null</code>)
     * 
     * @throws CmsException if something goes wrong
     */
    public void rebuildAllIndexes(I_CmsReport report) throws CmsException {

        rebuildAllIndexes(report, false);
    }

    /**
     * Rebuilds (if required creates) all configured indexes.<p>
     * 
     * @param report the report object to write messages (or <code>null</code>)
     * @param wait signals to wait until all the indexing threads are finished
     * 
     * @throws CmsException if something goes wrong
     */
    public void rebuildAllIndexes(I_CmsReport report, boolean wait) throws CmsException {

        for (int i = 0, n = m_indexes.size(); i < n; i++) {
            // iterate all configured seach indexes
            CmsSearchIndex searchIndex = (CmsSearchIndex)m_indexes.get(i);
            // update the index 
            updateIndex(searchIndex, report, wait, null, null);
        }
    }

    /**
     * Rebuilds (if required creates) the index with the given name.<p>
     * 
     * @param indexName the name of the index to rebuild
     * @param report the report object to write messages (or <code>null</code>)
     * 
     * @throws CmsException if something goes wrong
     */
    public void rebuildIndex(String indexName, I_CmsReport report) throws CmsException {

        // get the search index by name
        CmsSearchIndex index = getIndex(indexName);
        // update the index 
        updateIndex(index, report, false, null, null);
    }

    /**
     * Sets the name of the directory below WEB-INF/ where the search indexes are stored.<p>
     * 
     * @param value the name of the directory below WEB-INF/ where the search indexes are stored
     */
    public void setDirectory(String value) {

        m_path = value;
    }

    /**
     * Sets the highlighter.<p>
     *
     * A highlighter is a class implementing org.opencms.search.documents.I_TermHighlighter.<p>
     *
     * @param highlighter the package/class name of the highlighter
     */
    public void setHighlighter(String highlighter) {

        try {
            m_highlighter = (I_CmsTermHighlighter)Class.forName(highlighter).newInstance();
        } catch (Exception exc) {
            m_highlighter = null;
        }
    }

    /**
     * Sets the max. excerpt length.<p>
     *
     * @param maxExcerptLength the max. excerpt length to set
     */
    public void setMaxExcerptLength(String maxExcerptLength) {

        try {
            m_maxExcerptLength = Integer.parseInt(maxExcerptLength);
        } catch (Exception e) {
            LOG.error(Messages.get().key(Messages.LOG_PARSE_EXCERPT_LENGTH_FAILED_1, maxExcerptLength), e);
            m_maxExcerptLength = 1024;
        }
    }

    /**
     * Sets the result cache size.<p>
     * 
     * @param value the result cache size
     */
    public void setResultCacheSize(String value) {

        m_resultCacheSize = value;
    }

    /**
     * Sets the timeout to abandon threads indexing a resource.<p>
     * 
     * @param value the timeout in milliseconds
     */
    public void setTimeout(String value) {

        m_timeout = value;
    }

    /**
     * Returns an analyzer for the given language.<p>
     * The analyzer is selected according to the analyzer configuration.
     * 
     * @param locale a language id, i.e. de, en, it
     * @return the appropriate lucene analyzer
     * @throws CmsIndexException if something goes wrong
     */
    protected Analyzer getAnalyzer(String locale) throws CmsIndexException {

        Analyzer analyzer = null;
        String className = null;

        CmsSearchAnalyzer analyzerConf = (CmsSearchAnalyzer)m_analyzers.get(locale);
        if (analyzerConf == null) {
            throw new CmsIndexException(Messages.get().container(Messages.ERR_ANALYZER_NOT_FOUND_1, locale));
        }

        try {
            className = analyzerConf.getClassName();
            Class analyzerClass = Class.forName(className);

            // added param for snowball analyzer
            String stemmerAlgorithm = analyzerConf.getStemmerAlgorithm();
            if (stemmerAlgorithm != null) {
                analyzer = (Analyzer)analyzerClass.getDeclaredConstructor(new Class[] {String.class}).newInstance(
                    new Object[] {stemmerAlgorithm});
            } else {
                analyzer = (Analyzer)analyzerClass.newInstance();
            }

        } catch (Exception e) {
            throw new CmsIndexException(Messages.get().container(Messages.ERR_LOAD_ANALYZER_1, className), e);
        }

        return analyzer;
    }

    /**
     * Returns a lucene document factory for given resource.<p>
     * The type of the document factory is selected by the type of the resource
     * and the mimetype of the resource content according to the documenttype configuration.
     * 
     * @param resource a cms resource
     * @return a lucene document factory or null
     */
    protected I_CmsDocumentFactory getDocumentFactory(A_CmsIndexResource resource) {

        String documentTypeKey = resource.getDocumentKey(true);

        I_CmsDocumentFactory factory = (I_CmsDocumentFactory)m_documentTypes.get(documentTypeKey);
        if (factory == null) {
            factory = (I_CmsDocumentFactory)m_documentTypes.get(resource.getDocumentKey(false));
        }

        return factory;
    }

    /**
     * Returns the set of names of all configured documenttypes.<p>
     * 
     * @return the set of names of all configured documenttypes
     */
    protected List getDocumentTypes() {

        List names = new ArrayList();
        for (Iterator i = m_documentTypes.values().iterator(); i.hasNext();) {
            I_CmsDocumentFactory factory = (I_CmsDocumentFactory)i.next();
            names.add(factory.getName());
        }

        return names;
    }

    /**
     * Returns the common cache for buffering search results.<p>
     * 
     * @return the cache
     */
    protected Map getResultCache() {

        return m_resultCache;
    }

    /**
     * Initializes the available Cms resource types to be indexed.<p>
     * 
     * A map stores document factories keyed by a string representing
     * a colon separated list of Cms resource types and/or mimetypes.<p>
     * 
     * The keys of this map are used to trigger a document factory to convert 
     * a Cms resource into a Lucene index document.<p>
     * 
     * A document factory is a class implementing the interface
     * {@link org.opencms.search.documents.I_CmsDocumentFactory}.<p>
     */
    protected void initAvailableDocumentTypes() {

        CmsSearchDocumentType documenttype = null;
        String className = null;
        String name = null;
        I_CmsDocumentFactory documentFactory = null;
        List resourceTypes = null;
        List mimeTypes = null;
        Class c = null;

        m_documentTypes = new HashMap();

        List keys = new ArrayList(m_documentTypeConfigs.keySet());
        for (int i = 0, n = keys.size(); i < n; i++) {

            documenttype = (CmsSearchDocumentType)(m_documentTypeConfigs.get(keys.get(i)));
            name = documenttype.getName();

            try {
                className = documenttype.getClassName();
                resourceTypes = documenttype.getResourceTypes();
                mimeTypes = documenttype.getMimeTypes();

                if (name == null) {
                    throw new CmsIndexException(Messages.get().container(Messages.ERR_DOCTYPE_NO_NAME_0));
                }
                if (className == null) {
                    throw new CmsIndexException(Messages.get().container(Messages.ERR_DOCTYPE_NO_CLASS_DEF_0));
                }

                if (resourceTypes.size() == 0) {
                    throw new CmsIndexException(Messages.get().container(Messages.ERR_DOCTYPE_NO_RESOURCETYPE_DEF_0));
                }

                try {
                    c = Class.forName(className);
                    documentFactory = (I_CmsDocumentFactory)c.getConstructor(new Class[] {String.class}).newInstance(
                        new Object[] {name});
                } catch (ClassNotFoundException exc) {
                    throw new CmsIndexException(
                        Messages.get().container(Messages.ERR_DOCCLASS_NOT_FOUND_1, className),
                        exc);
                } catch (Exception exc) {
                    throw new CmsIndexException(Messages.get().container(Messages.ERR_DOCCLASS_INIT_1, className), exc);
                }

                for (Iterator key = documentFactory.getDocumentKeys(resourceTypes, mimeTypes).iterator(); key.hasNext();) {
                    m_documentTypes.put(key.next(), documentFactory);
                }

            } catch (CmsException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(Messages.get().key(Messages.LOG_DOCTYPE_CONFIG_FAILED_1, name), e);
                }
            }
        }
    }

    /**
     * Initializes the configured search indexes.<p>
     * 
     * This initializes also the list of Cms resources types
     * to be indexed by an index source.<p>
     */
    protected void initSearchIndexes() {

        CmsSearchIndex index = null;

        for (int i = 0, n = m_indexes.size(); i < n; i++) {
            index = (CmsSearchIndex)m_indexes.get(i);
            try {
                index.initialize();
            } catch (CmsException exc) {
                if (CmsLog.INIT.isInfoEnabled()) {
                    CmsLog.INIT.info(Messages.get().key(Messages.INIT_SEARCH_INIT_FAILED_1, index.getName()), exc);
                }
            }
        }
    }

    /**
     * Incrementally updates all indexes that have their rebuild mode set to <code>"auto"</code>
     * after resources have been published.<p> 
     * 
     * @param adminCms an OpenCms user context with Admin permissions
     * @param publishHistoryId the history ID of the published project 
     * @param report the report to write the output to
     */
    protected synchronized void updateAllIndexes(CmsObject adminCms, CmsUUID publishHistoryId, I_CmsReport report) {

        List publishedResources;
        try {
            // read the list of all published resources
            publishedResources = adminCms.readPublishedResources(publishHistoryId);
        } catch (CmsException e) {
            LOG.error(Messages.get().key(Messages.LOG_READING_CHANGED_RESOURCES_FAILED_1, publishHistoryId), e);
            return;
        }

        List updateResources = new ArrayList();
        Iterator itPubRes = publishedResources.iterator();
        while (itPubRes.hasNext()) {
            CmsPublishedResource res = (CmsPublishedResource)itPubRes.next();
            if (res.isFolder() || res.isUnChanged() || !res.isVfsResource()) {
                // folders, unchanged resources and non vfs resources don't need to be indexed after publish
                continue;
            }
            if (res.isDeleted() || res.isNew() || res.isChanged()) {
                updateResources.add(res);
            }
        }

        // cache for the generated documents (to avoid multiple text extraction in case of overlapping indexes)
        Map documentCache = Collections.synchronizedMap(new LRUMap(256));

        if (!updateResources.isEmpty()) {
            // sort the resource to update
            Collections.sort(updateResources);
            // only update the indexes if the list of remaining published resources is not empty
            Iterator i = m_indexes.iterator();
            while (i.hasNext()) {
                CmsSearchIndex index = (CmsSearchIndex)i.next();
                if (CmsSearchIndex.REBUILD_MODE_AUTO.equals(index.getRebuildMode())) {
                    // only update indexes which have the rebuild mode set to "auto"
                    try {
                        updateIndex(index, report, false, updateResources, documentCache);
                    } catch (CmsException e) {
                        LOG.error(Messages.get().key(Messages.LOG_UPDATE_INDEX_FAILED_1, index.getName()), e);
                    }
                }
            }
        }
    }

    /**
     * Updates (if required creates) the index with the given name.<p>
     * 
     * If the optional List of <code>{@link CmsPublishedResource}</code> instances is provided, the index will be 
     * incrementally updated for these resources only. If this List is <code>null</code> or empty, 
     * the index will be fully rebuild.<p>
     * 
     * @param index the index to update or rebuild
     * @param report the report to write output messages to 
     * @param wait signals to wait until all the indexing threads are finished
     * @param resourcesToIndex an (optional) list of <code>{@link CmsPublishedResource}</code> objects to update in the index
     * @param documentCache a cache for the created search documents, to avoid multiple text extraction
     * 
     * @throws CmsException if something goes wrong
     */
    protected synchronized void updateIndex(
        CmsSearchIndex index,
        I_CmsReport report,
        boolean wait,
        List resourcesToIndex,
        Map documentCache) throws CmsException {

        // copy the stored admin context for the indexing
        CmsObject cms = OpenCms.initCmsObject(m_adminCms);
        // make sure a report is available
        if (report == null) {
            report = new CmsLogReport(cms.getRequestContext().getLocale(), CmsSearchManager.class);
        }
        // set site root and project for this index
        cms.getRequestContext().setSiteRoot("/");
        // switch to the index project
        cms.getRequestContext().setCurrentProject(cms.readProject(index.getProject()));

        if ((resourcesToIndex == null) || resourcesToIndex.isEmpty()) {
            // rebuild the complete index

            // create a new index writer
            IndexWriter writer = index.getIndexWriter(true);

            // create a new thread manager for the indexing threads
            // please note: document cache _must_ be null for full rebuild 
            //              since there may be diffeences between online and offline projects,
            //              which can only be ignored if a resource has just been published (then online=offline)
            CmsIndexingThreadManager threadManager = new CmsIndexingThreadManager(
                report,
                Long.parseLong(m_timeout),
                index.getName(),
                null);

            try {

                // ouput start information on the report
                report.println(
                    Messages.get().container(Messages.RPT_SEARCH_INDEXING_REBUILD_BEGIN_1, index.getName()),
                    I_CmsReport.FORMAT_HEADLINE);

                // iterate all configured index sources of this index
                Iterator sources = index.getSources().iterator();
                while (sources.hasNext()) {
                    // get the next index source
                    CmsSearchIndexSource source = (CmsSearchIndexSource)sources.next();
                    // create the indexer
                    I_CmsIndexer indexer = source.getIndexer().newInstance(cms, report, index);
                    // new index creation, use all resources from the index source
                    indexer.rebuildIndex(writer, threadManager, source);
                }

                // wait for indexing threads to finish
                while (wait && threadManager.isRunning()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // just continue with the loop after interruption
                    }
                }
                // optimize the generated index
                try {
                    writer.optimize();
                } catch (IOException e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn(Messages.get().key(
                            Messages.LOG_IO_INDEX_WRITER_OPTIMIZE_1,
                            index.getPath(),
                            index.getName()), e);
                    }
                }

                // ouput finish information on the report
                report.println(
                    Messages.get().container(Messages.RPT_SEARCH_INDEXING_REBUILD_END_1, index.getName()),
                    I_CmsReport.FORMAT_HEADLINE);

            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn(Messages.get().key(
                                Messages.LOG_IO_INDEX_WRITER_CLOSE_2,
                                index.getPath(),
                                index.getName()), e);
                        }
                    }
                }
            }

            // show information about indexing runtime
            threadManager.reportStatistics();

        } else {
            // update the existing index

            List updateCollections = new ArrayList();

            boolean hasResourcesToDelete = false;
            boolean hasResourcesToUpdate = false;

            // iterate all configured index sources of this index
            Iterator sources = index.getSources().iterator();
            while (sources.hasNext()) {
                // get the next index source
                CmsSearchIndexSource source = (CmsSearchIndexSource)sources.next();
                // create the indexer
                I_CmsIndexer indexer = source.getIndexer().newInstance(cms, report, index);
                // collect the resources to update
                CmsSearchIndexUpdateData updateData = indexer.getUpdateData(source, resourcesToIndex);
                if (!updateData.isEmpty()) {
                    // add the update collection to the internal pipeline
                    updateCollections.add(updateData);
                    hasResourcesToDelete = hasResourcesToDelete | updateData.hasResourcesToDelete();
                    hasResourcesToUpdate = hasResourcesToUpdate | updateData.hasResourceToUpdate();
                }
            }

            if (hasResourcesToDelete || hasResourcesToUpdate) {
                // ouput start information on the report
                report.println(
                    Messages.get().container(Messages.RPT_SEARCH_INDEXING_UPDATE_BEGIN_1, index.getName()),
                    I_CmsReport.FORMAT_HEADLINE);
            }

            if (hasResourcesToDelete) {
                // delete the resource from the index
                IndexReader reader = null;
                try {
                    reader = IndexReader.open(index.getPath());
                } catch (IOException e) {
                    LOG.error(
                        Messages.get().key(Messages.LOG_IO_INDEX_READER_OPEN_2, index.getPath(), index.getName()),
                        e);
                }
                if (reader != null) {
                    try {
                        Iterator i = updateCollections.iterator();
                        while (i.hasNext()) {
                            CmsSearchIndexUpdateData updateCollection = (CmsSearchIndexUpdateData)i.next();
                            if (updateCollection.hasResourcesToDelete()) {
                                updateCollection.getIndexer().deleteResources(
                                    reader,
                                    updateCollection.getResourcesToDelete());
                            }
                        }
                    } finally {
                        try {
                            // close the reader after all resources have been deleted
                            reader.close();
                        } catch (IOException e) {
                            LOG.error(Messages.get().key(
                                Messages.LOG_IO_INDEX_READER_CLOSE_2,
                                index.getPath(),
                                index.getName()), e);
                        }
                    }
                }
            }

            if (hasResourcesToUpdate) {

                // create an index writer that updates the current index
                IndexWriter writer = index.getIndexWriter(false);

                if (writer != null) {

                    // create a new thread manager
                    CmsIndexingThreadManager threadManager = new CmsIndexingThreadManager(
                        report,
                        Long.parseLong(m_timeout),
                        index.getName(),
                        documentCache);

                    try {
                        Iterator i = updateCollections.iterator();
                        while (i.hasNext()) {
                            CmsSearchIndexUpdateData updateCollection = (CmsSearchIndexUpdateData)i.next();
                            if (updateCollection.hasResourceToUpdate()) {
                                updateCollection.getIndexer().updateResources(
                                    writer,
                                    threadManager,
                                    updateCollection.getResourcesToUpdate());
                            }
                        }
                    } finally {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            LOG.error(Messages.get().key(
                                Messages.LOG_IO_INDEX_WRITER_CLOSE_2,
                                index.getPath(),
                                index.getName()), e);
                        }
                    }

                    // wait for indexing threads to finish
                    while (wait && threadManager.isRunning()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            // just continue with the loop after interruption
                        }
                    }
                }
            }

            if (hasResourcesToDelete || hasResourcesToUpdate) {
                // ouput finish information on the report
                report.println(
                    Messages.get().container(Messages.RPT_SEARCH_INDEXING_UPDATE_END_1, index.getName()),
                    I_CmsReport.FORMAT_HEADLINE);
            }

        }

        // clear the cache for search results
        m_resultCache.clear();
    }
}