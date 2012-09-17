/*
 * File   : $Source$
 * Date   : $Date$
 * Version: $Revision$
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

package org.opencms.search.solr;

import org.opencms.configuration.CmsConfigurationException;
import org.opencms.configuration.CmsParameterConfiguration;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.file.types.CmsResourceTypeXmlContainerPage;
import org.opencms.file.types.CmsResourceTypeXmlContent;
import org.opencms.i18n.CmsEncoder;
import org.opencms.main.CmsException;
import org.opencms.main.CmsIllegalArgumentException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.report.I_CmsReport;
import org.opencms.search.A_CmsSearchIndex;
import org.opencms.search.CmsSearchException;
import org.opencms.search.CmsSearchManager;
import org.opencms.search.CmsSearchParameters;
import org.opencms.search.CmsSearchResource;
import org.opencms.search.CmsSearchResultList;
import org.opencms.search.I_CmsIndexWriter;
import org.opencms.search.I_CmsSearchDocument;
import org.opencms.search.documents.I_CmsDocumentFactory;
import org.opencms.search.fields.I_CmsSearchField;
import org.opencms.util.CmsRequestUtil;
import org.opencms.util.CmsStringUtil;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.common.util.FastWriter;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.ReplicationHandler;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.BinaryQueryResponseWriter;
import org.apache.solr.response.QueryResponseWriter;
import org.apache.solr.response.SolrQueryResponse;

/**
 * Implements the search within an Solr index.<p>
 * 
 * @since 8.5.0 
 */
public class CmsSolrIndex extends A_CmsSearchIndex {

    /** The name of the default Solr Offline index. */
    public static final String DEFAULT_INDEX_NAME_OFFLINE = "Solr Offline";

    /** The name of the default Solr Online index. */
    public static final String DEFAULT_INDEX_NAME_ONLINE = "Solr Online";

    /** Constant for additional parameter to set the post processor class name. */
    public static final String POST_PROCESSOR = "search.solr.postProcessor";

    /** The solr exclude property. */
    public static final String PROPERTY_SEARCH_EXCLUDE_VALUE_SOLR = "solr";

    /** A constant for debug formatting outpu. */
    protected static final int DEBUG_PADDING_RIGHT = 50;

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsSolrIndex.class);

    /** Indicates the maximum number of documents from the complete result set to return. */
    private static final int ROWS_MAX = 50;

    /** A constant for UTF-8 charset. */
    private static final Charset UTF8 = Charset.forName("UTF-8");

    /** The embedded Solr server for this index. */
    SolrServer m_solr;

    /** The post document manipulator. */
    private I_CmsSolrPostSearchProcessor m_postProcessor;

    /**
     * Default constructor.<p>
     */
    public CmsSolrIndex() {

        super();
    }

    /**
     * Public constructor to create a Solr index.<p>
     * 
     * @param name the name for this index.<p>
     * 
     * @throws CmsIllegalArgumentException if something goes wrong
     */
    public CmsSolrIndex(String name)
    throws CmsIllegalArgumentException {

        super(name);
    }

    /**
     * Returns the resource type for the given root path.<p>
     * 
     * @param cms the current CMS context
     * @param rootPath the root path of the resource to get the type for
     * 
     * @return the resource type for the given root path
     */
    public static final String getType(CmsObject cms, String rootPath) {

        String type = null;
        CmsSolrIndex index = CmsSearchManager.getIndexSolr(cms, null);
        if (index != null) {
            I_CmsSearchDocument doc = index.getDocument(I_CmsSearchField.FIELD_PATH, rootPath);
            if (doc != null) {
                type = doc.getFieldValueAsString(I_CmsSearchField.FIELD_TYPE);
            }
        }
        return type;
    }

    /**
     * @see org.opencms.search.A_CmsSearchIndex#addConfigurationParameter(java.lang.String, java.lang.String)
     */
    @Override
    public void addConfigurationParameter(String key, String value) {

        if (POST_PROCESSOR.equals(key)) {
            if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(value)) {
                try {
                    setPostProcessor((I_CmsSolrPostSearchProcessor)Class.forName(value).newInstance());
                } catch (Exception e) {
                    CmsException ex = new CmsException(Messages.get().container(
                        Messages.ERR_POST_PROCESSOR_CLASS_NOT_EXIST_1,
                        value), e);
                    LOG.error(ex.getMessage(), ex);
                }
            }
        }
        super.addConfigurationParameter(key, value);
    }

    /**
     * @see org.opencms.search.A_CmsSearchIndex#createIndexWriter(boolean, org.opencms.report.I_CmsReport)
     */
    @Override
    public I_CmsIndexWriter createIndexWriter(boolean create, I_CmsReport report) {

        return new CmsSolrIndexWriter(m_solr, this);
    }

    /**
     * @see org.opencms.search.A_CmsSearchIndex#getConfiguration()
     */
    @Override
    public CmsParameterConfiguration getConfiguration() {

        CmsParameterConfiguration result = super.getConfiguration();
        if (getPostProcessor() != null) {
            result.put(POST_PROCESSOR, getPostProcessor().getClass().getName());
        }
        return result;
    }

    /**
     * @see org.opencms.search.A_CmsSearchIndex#getDocument(java.lang.String, java.lang.String)
     */
    @Override
    public I_CmsSearchDocument getDocument(String fieldname, String term) {

        try {
            SolrQuery query = new SolrQuery();
            query.setQuery(fieldname + ":" + term);
            QueryResponse res = m_solr.query(query);
            if (res != null) {
                SolrDocumentList sdl = m_solr.query(query).getResults();
                if ((sdl.getNumFound() == 1L) && (sdl.get(0) != null)) {
                    return new CmsSolrDocument(ClientUtils.toSolrInputDocument(sdl.get(0)));
                }
            }
        } catch (Exception e) {
            // ignore and assume that the document could not be found
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * @see org.opencms.search.CmsLuceneIndex#getDocumentFactory(org.opencms.file.CmsResource)
     */
    @Override
    public I_CmsDocumentFactory getDocumentFactory(CmsResource res) {

        if ((res != null) && (getSources() != null)) {
            if (CmsResourceTypeXmlContainerPage.isContainerPage(res)) {
                return OpenCms.getSearchManager().getDocumentFactory(
                    CmsSolrDocumentContainerPage.TYPE_CONTAINERPAGE_SOLR,
                    "text/html");
            }
            if (CmsResourceTypeXmlContent.isXmlContent(res)) {
                return OpenCms.getSearchManager().getDocumentFactory(
                    CmsSolrDocumentXmlContent.TYPE_XMLCONTENT_SOLR,
                    "text/html");
            } else {
                return super.getDocumentFactory(res);
            }
        }
        return null;
    }

    /**
     * Returns the language locale for the given resource in this index.<p>
     * 
     * @param cms the current OpenCms user context
     * @param resource the resource to check
     * @param availableLocales a list of locales supported by the resource
     * 
     * @return the language locale for the given resource in this index
     */
    @Override
    public Locale getLocaleForResource(CmsObject cms, CmsResource resource, List<Locale> availableLocales) {

        Locale result;
        List<Locale> defaultLocales = OpenCms.getLocaleManager().getDefaultLocales(cms, resource);
        if ((availableLocales != null) && (availableLocales.size() > 0)) {
            result = OpenCms.getLocaleManager().getBestMatchingLocale(
                defaultLocales.get(0),
                defaultLocales,
                availableLocales);
        } else {
            result = defaultLocales.get(0);
        }
        return result;
    }

    /**
     * Returns the search post processor.<p>
     *
     * @return the post processor to use
     */
    public I_CmsSolrPostSearchProcessor getPostProcessor() {

        return m_postProcessor;
    }

    /**
     * @see org.opencms.search.A_CmsSearchIndex#initialize()
     */
    @Override
    public void initialize() throws CmsSearchException {

        super.initialize();
        try {
            m_solr = OpenCms.getSearchManager().registerSolrIndex(this);
        } catch (CmsConfigurationException ex) {
            LOG.error(ex.getMessage());
            setEnabled(false);
        }
    }

    /**
     * Not yet implemented for Solr.<p>
     * 
     * <code>
     * #################<br>
     * ### DON'T USE ###<br>
     * #################<br>
     * </code>
     * 
     * @Deprecated Use {@link #search(CmsObject, SolrQuery)} or {@link #search(CmsObject, String)} instead
     */
    @Override
    @Deprecated
    public CmsSearchResultList search(CmsObject cms, CmsSearchParameters params) {

        throw new UnsupportedOperationException();
    }

    /**
     * Default search method.<p>
     * 
     * @param cms the current CMS object 
     * @param query the query
     * 
     * @return the results
     * 
     * @throws CmsSearchException if something goes wrong
     * 
     * @see #search(CmsObject, String)
     */
    public CmsSolrResultList search(CmsObject cms, CmsSolrQuery query) throws CmsSearchException {

        return search(cms, query, false);
    }

    /**
     * <h4>Performs a search on the Solr index</h4>
     * 
     * Returns a list of 'OpenCms resource documents' 
     * ({@link CmsSearchResource}) encapsulated within the class  {@link CmsSolrResultList}.
     * This list can be accessed exactly like an {@link List} which entries are 
     * {@link CmsSearchResource} that extend {@link CmsResource} and holds the Solr 
     * implementation of {@link I_CmsSearchDocument} as member. <b>This enables you to deal 
     * with the resulting list as you do with well known {@link List} and work on it's entries
     * like you do on {@link CmsResource}.</b>
     * 
     * <h4>What will be done with the Solr search result?</h4>
     * <ul>
     * <li>Although it can happen, that there are less results returned than rows were requested
     * (imagine an index containing less documents than requested rows) we try to guarantee
     * the requested amount of search results and to provide a working pagination with
     * security check.</li>
     * 
     * <li>To be sure we get enough documents left even the permission check reduces the amount
     * of found documents, the rows are multiplied by <code>'5'</code> and the current page 
     * additionally the offset is added. The count of documents we don't have enough 
     * permissions for grows with increasing page number, that's why we also multiply 
     * the rows by the current page count.</li>
     * 
     * <li>Also make sure we perform the permission check for all found documents, so start with
     * the first found doc.</li>
     * </ul>
     * 
     * <b>NOTE:</b> If latter pages than the current one are containing protected documents the
     * total hit count will be incorrect, because the permission check ends if we have 
     * enough results found for the page to display. With other words latter pages than 
     * the current can contain documents that will first be checked if those pages are 
     * requested to be displayed, what causes a incorrect hit count.<p>
     * 
     * @param cms the current OpenCms context
     * @param ignoreMaxRows <code>true</code> to return all all requested rows, <code>false</code> to use max rows
     * @param query the OpenCms Solr query
     * 
     * @return the list of found documents
     * 
     * @throws CmsSearchException if something goes wrong
     * 
     * @see org.opencms.search.solr.CmsSolrResultList
     * @see org.opencms.search.CmsSearchResource
     * @see org.opencms.search.I_CmsSearchDocument
     * @see org.opencms.search.solr.CmsSolrQuery
     */
    public synchronized CmsSolrResultList search(CmsObject cms, final CmsSolrQuery query, boolean ignoreMaxRows)
    throws CmsSearchException {

        String currentTime = DateFormat.getTimeInstance(DateFormat.FULL, Locale.ENGLISH).format(new Date());
        debug("### START SRARCH with " + query.getRows() + " requested rows at", currentTime, 0);

        int previousPriority = Thread.currentThread().getPriority();
        long startTime = System.currentTimeMillis();

        // remember the initial query
        SolrQuery initQuery = query.clone();

        query.setHighlight(false);

        try {

            // initialize the search context
            CmsObject searchCms = OpenCms.initCmsObject(cms);

            // change thread priority in order to reduce search impact on overall system performance
            if (getPriority() > 0) {
                Thread.currentThread().setPriority(getPriority());
            }

            // the lists storing the found documents that will be returned
            List<CmsSearchResource> resourceDocumentList = new ArrayList<CmsSearchResource>();
            SolrDocumentList solrDocumentList = new SolrDocumentList();

            // Initialize rows, offset, end and the current page.
            int rows = query.getRows() != null ? query.getRows().intValue() : CmsSolrQuery.DEFAULT_ROWS.intValue();
            if (!ignoreMaxRows && (rows > ROWS_MAX)) {
                rows = ROWS_MAX;
            }
            int start = query.getStart() != null ? query.getStart().intValue() : 0;
            int end = start + rows;
            int page = 0;
            if (rows > 0) {
                page = Math.round(start / rows) + 1;
            }

            // set the start to '0' and expand the rows before performing the query
            query.setStart(new Integer(0));
            query.setRows(new Integer((5 * rows * page) + start));

            // perform the Solr query and remember the original Solr response
            QueryResponse queryResponse = m_solr.query(query);

            // initialize the hit count and the max score
            long hitCount = queryResponse.getResults().getNumFound();
            debug("--- Query Executed " + "(hitCount=" + hitCount + ")", null, System.currentTimeMillis() - startTime);
            // page = params.getSearchPage();
            start = -1;
            end = -1;
            if ((rows > 0) && (page > 0) && (hitCount > 0)) {
                // calculate the final size of the search result
                start = rows * (page - 1);
                end = start + rows;
                // ensure that both i and n are inside the range of foundDocuments.size()
                start = new Long((start > hitCount) ? hitCount : start).intValue();
                end = new Long((end > hitCount) ? hitCount : end).intValue();
            } else {
                // return all found documents in the search result
                start = 0;
                end = new Long(hitCount).intValue();
            }
            long visibleHitCount = hitCount;
            float maxScore = 0;

            List<CmsSearchResource> allDocs = new ArrayList<CmsSearchResource>();

            // iterate over found documents
            int cnt = 0;
            for (int i = 0; (i < queryResponse.getResults().size()) && (cnt < end); i++) {
                try {
                    SolrDocument doc = queryResponse.getResults().get(i);
                    CmsSolrDocument searchDoc = new CmsSolrDocument(doc);
                    if (needsPermissionCheck(searchDoc)) {
                        // only if the document is an OpenCms internal resource perform the permission check
                        CmsResource resource = getResource(searchCms, searchDoc);
                        if (resource != null) {
                            // permission check performed successfully: the user has read permissions!
                            if (cnt >= start) {
                                if (m_postProcessor != null) {
                                    doc = m_postProcessor.process(
                                        searchCms,
                                        resource,
                                        (SolrInputDocument)searchDoc.getDocument());
                                }
                                resourceDocumentList.add(new CmsSearchResource(resource, searchDoc));
                                solrDocumentList.add(doc);
                                maxScore = maxScore < searchDoc.getScore() ? searchDoc.getScore() : maxScore;
                            }
                            allDocs.add(new CmsSearchResource(resource, searchDoc));
                            cnt++;
                        } else {
                            visibleHitCount--;
                        }
                    }
                } catch (Exception e) {
                    // should not happen, but if it does we want to go on with the next result nevertheless                        
                    LOG.warn(Messages.get().getBundle().key(Messages.LOG_RESULT_ITERATION_FAILED_0), e);
                }
            }

            // the last documents were all secret so let's take the last found docs
            if (resourceDocumentList.isEmpty() && (allDocs.size() > 0)) {
                page = Math.round(allDocs.size() / rows) + 1;
                int showCount = allDocs.size() % rows;
                showCount = showCount == 0 ? rows : showCount;
                start = allDocs.size() - new Long(showCount).intValue();
                end = allDocs.size();
                if (allDocs.size() > start) {
                    resourceDocumentList = allDocs.subList(start, end);
                    for (CmsSearchResource r : resourceDocumentList) {
                        maxScore = maxScore < r.getDocument().getScore() ? r.getDocument().getScore() : maxScore;
                        solrDocumentList.add(((CmsSolrDocument)r.getDocument()).getSolrDocument());
                    }
                }
            }

            SolrCore core = null;
            if (m_solr instanceof EmbeddedSolrServer) {
                core = ((EmbeddedSolrServer)m_solr).getCoreContainer().getCore(getName());
            }
            debug("--- Permissions Checked for " + cnt + " Resources", null, System.currentTimeMillis() - startTime);
            // create and return the result
            CmsSolrResultList result = new CmsSolrResultList(
                core,
                initQuery,
                queryResponse,
                solrDocumentList,
                resourceDocumentList,
                start,
                new Integer(rows),
                end,
                page,
                visibleHitCount,
                new Float(maxScore),
                startTime);
            debug("### FINISH SEARCH in", null, System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            throw new CmsSearchException(Messages.get().container(
                Messages.ERR_SEARCH_INVALID_SEARCH_1,
                CmsEncoder.decode(query.toString())), e);
        } finally {
            // re-set thread to previous priority
            Thread.currentThread().setPriority(previousPriority);
        }
    }

    /**
     * Default search method.<p>
     * 
     * @param cms the current CMS object 
     * @param query the query
     * 
     * @return the results
     * 
     * @throws CmsSearchException if something goes wrong
     * 
     * @see #search(CmsObject, String)
     */
    public CmsSolrResultList search(CmsObject cms, SolrQuery query) throws CmsSearchException {

        return search(cms, CmsEncoder.decode(query.toString()));
    }

    /**
     * Performs a search.<p>
     * 
     * @param cms the cms object
     * @param solrQuery the Solr query
     * 
     * @return a list of documents
     * 
     * @throws CmsSearchException if something goes wrong
     * 
     * @see #search(CmsObject, CmsSolrQuery, boolean)
     */
    public CmsSolrResultList search(CmsObject cms, String solrQuery) throws CmsSearchException {

        return search(cms, new CmsSolrQuery(null, CmsRequestUtil.createParameterMap(solrQuery)), false);
    }

    /**
     * Sets the search post processor.<p>
     *
     * @param postProcessor the search post processor to set
     */
    public void setPostProcessor(I_CmsSolrPostSearchProcessor postProcessor) {

        m_postProcessor = postProcessor;
    }

    /**
     * @see org.opencms.search.A_CmsSearchIndex#shutDown()
     */
    @Override
    public void shutDown() {

        // noop
    }

    /**
     * Writes the response into the writer.<p>
     * 
     * NOTE: Currently not available for HTTP server.<p>
     * 
     * @param response the servlet response
     * @param result the result to print
     * 
     * @throws Exception if there is no embedded server
     */
    public void writeResponse(ServletResponse response, CmsSolrResultList result) throws Exception {

        if (m_solr instanceof EmbeddedSolrServer) {

            long start = System.currentTimeMillis();
            SolrCore core = ((EmbeddedSolrServer)m_solr).getCoreContainer().getCore(getName());
            SolrQueryRequest queryRequest = result.getSolrQueryRequest();
            SolrQueryResponse queryResponse = result.getSolrQueryResponse();
            QueryResponseWriter responseWriter = core.getQueryResponseWriter(queryRequest);

            final String ct = responseWriter.getContentType(queryRequest, queryResponse);
            if (null != ct) {
                response.setContentType(ct);
            }

            if (responseWriter instanceof BinaryQueryResponseWriter) {
                BinaryQueryResponseWriter binWriter = (BinaryQueryResponseWriter)responseWriter;
                binWriter.write(response.getOutputStream(), queryRequest, queryResponse);
            } else {
                String charset = ContentStreamBase.getCharsetFromContentType(ct);
                Writer out = ((charset == null) || charset.equalsIgnoreCase(UTF8.toString())) ? new OutputStreamWriter(
                    response.getOutputStream(),
                    UTF8) : new OutputStreamWriter(response.getOutputStream(), charset);
                out = new FastWriter(out);
                responseWriter.write(out, queryRequest, queryResponse);
                out.flush();
            }
            debug(
                "### WRITE RESPONSE (" + (System.currentTimeMillis() - start) + " ms)",
                null,
                System.currentTimeMillis() - result.getStartTime());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @see org.opencms.search.A_CmsSearchIndex#createIndexBackup()
     */
    @Override
    protected String createIndexBackup() {

        if (!isBackupReindexing()) {
            // if no backup is generated we don't need to do anything
            return null;
        }
        if (m_solr instanceof EmbeddedSolrServer) {
            EmbeddedSolrServer ser = (EmbeddedSolrServer)m_solr;
            CoreContainer con = ser.getCoreContainer();
            SolrCore core = con.getCore(getName());
            if (core != null) {
                SolrRequestHandler h = core.getRequestHandler("/replication");
                if (h instanceof ReplicationHandler) {
                    h.handleRequest(
                        new LocalSolrQueryRequest(core, CmsRequestUtil.createParameterMap("?command=backup")),
                        new SolrQueryResponse());
                }
            }
        }
        return null;
    }

    /**
     * Writes a formatted log message.<p>
     * 
     * @param keyMessage the message
     * @param valueMessage optional value
     * @param time a timestamp in ms
     */
    protected void debug(String keyMessage, String valueMessage, long time) {

        if (valueMessage != null) {
            LOG.debug(CmsStringUtil.padRight(keyMessage, DEBUG_PADDING_RIGHT) + ": " + valueMessage);
        } else {
            LOG.debug(CmsStringUtil.padRight(keyMessage, DEBUG_PADDING_RIGHT) + ": " + time + " ms");
        }
    }

    /**
     * @see org.opencms.search.A_CmsSearchIndex#excludeFromIndex(CmsObject, CmsResource)
     */
    @Override
    protected boolean excludeFromIndex(CmsObject cms, CmsResource resource) {

        if (resource.isFolder() || resource.isTemporaryFile()) {
            // don't index  folders or temporary files for galleries, but pretty much everything else
            return true;
        }
        boolean excludeFromIndex = false;
        try {
            // do property lookup with folder search
            String propValue = cms.readPropertyObject(resource, CmsPropertyDefinition.PROPERTY_SEARCH_EXCLUDE, true).getValue();
            if (propValue != null) {
                propValue = propValue.trim();
                // property value was neither "true" nor null, must check for "all"
                excludeFromIndex = PROPERTY_SEARCH_EXCLUDE_VALUE_ALL.equalsIgnoreCase(propValue)
                    || PROPERTY_SEARCH_EXCLUDE_VALUE_SOLR.equalsIgnoreCase(propValue);
            }
        } catch (CmsException e) {
            LOG.debug(e.getMessage(), e);
        }
        return excludeFromIndex;
    }

    /**
     * @see org.opencms.search.A_CmsSearchIndex#indexSearcherClose()
     */
    @Override
    protected void indexSearcherClose() {

        // nothing to do here
    }

    /**
     * @see org.opencms.search.A_CmsSearchIndex#indexSearcherOpen(java.lang.String)
     */
    @Override
    protected void indexSearcherOpen(String path) {

        // nothing to do here
    }

    /**
     * @see org.opencms.search.A_CmsSearchIndex#indexSearcherUpdate()
     */
    @Override
    protected void indexSearcherUpdate() {

        // nothing to do here
    }
}
