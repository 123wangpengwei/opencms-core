/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/search/CmsSearch.java,v $
 * Date   : $Date: 2005/03/08 06:21:01 $
 * Version: $Revision: 1.20 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2005 Alkacon Software (http://www.alkacon.com)
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

package org.opencms.search;

import org.opencms.file.CmsObject;
import org.opencms.i18n.CmsEncoder;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsStringUtil;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Helper class to access the search facility within a jsp.<p>
 * 
 * Typically, the following fields are available for searching:
 * <ul>
 * <li>title - the title of a resource</li>
 * <li>keywords - the keywords of a resource</li>
 * <li>description - the description of a resource</li>
 * <li>content - the aggregated content of a resource</li>
 * <li>created - the creation date of a resource</li>
 * <li>lastmodified - the date of the last modification of a resource</li>
 * <li>path - the path to display the resource</li>
 * <li>channel - the channel of a resource</li>
 * <li>contentdefinition - the name of the content definition class of a resource</li>
 * </ul>
 * 
 * @version $Revision: 1.20 $ $Date: 2005/03/08 06:21:01 $
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @since 5.3.1
 */
public class CmsSearch implements Serializable, Cloneable {

    /** The cms object. */
    protected transient CmsObject m_cms;

    /** The list of fields to search. */
    protected String[] m_fields;
    
    /** The index to search. */
    protected CmsSearchIndex m_index;

    /** The name of the search index. */
    protected String m_indexName;

    /** The current query. */
    protected String m_query;

    /** The minimum length of the search query. */
    protected int m_queryLength;

    /** The current search result. */
    protected List m_result;

    /** The latest exception. */
    protected Exception m_lastException;

    /** The current result page. */
    protected int m_page;

    /** The number of matches per page. */
    protected int m_matchesPerPage;

    /** The number of pages for the result list. */
    protected int m_pageCount;

    /** The number of displayed pages returned by getPageLinks(). */
    protected int m_displayPages;

    /** The URL which leads to the previous result page. */
    protected String m_prevUrl;

    /** The URL which leads to the next result page. */
    protected String m_nextUrl;

    /** The search parameter String. */
    protected String m_searchParameters;

    /** The search root. */
    protected String m_searchRoot;
    
    /** The total number of search results matching the query. */
    protected int m_searchResultCount;

    /**
     * Default constructor, used to instanciate the search facility as a bean.<p>
     */
    public CmsSearch() {

        super();
        
        m_searchRoot = "";
        m_page = 1;
        m_searchResultCount = 0;
        m_matchesPerPage = 10;
        m_displayPages = 10;
        m_queryLength = -1;
    }

    /**
     * Returns the maximum number of pages which should be shown.<p> 
     * 
     * @return the maximum number of pages which should be shown
     */
    public int getDisplayPages() {

        return m_displayPages;
    }

    /**
     * Sets the maximum number of pages which should be shown.<p>
     * 
     * Enter an odd value to achieve a nice, "symmetric" output.<p> 
     * 
     * @param value the maximum number of pages which should be shown
     */
    public void setDisplayPages(int value) {

        m_displayPages = value;
    }

    /**
     * Gets the current fields list.<p>
     * 
     * @return the fields to search
     */
    public String getFields() {

        if (m_fields == null) {
            return "";
        }
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < m_fields.length; i++) {
            result.append(m_fields[i]);
            result.append(" ");
        }
        return result.toString();
    }

    /**
     * Gets the name of the current search index.<p>
     * 
     * @return the name of the index
     */
    public String getIndex() {

        return m_indexName;
    }

    /**
     * Gets the last exception after a search operation.<p>
     * 
     * @return the exception occured in a search operation or null
     */
    public Exception getLastException() {

        return m_lastException;
    }

    /**
     * Gets the number of matches displayed on each page.<p>
     * 
     * @return matches per result page
     */
    public int getMatchesPerPage() {

        return m_matchesPerPage;
    }

    /**
     * Gets the URL for the link to the next result page.<p>
     * 
     * @return the URL to the next result page
     */
    public String getNextUrl() {

        return m_nextUrl;
    }

    /**
     * Gets the current result page.<p>
     * 
     * @return the current result page
     */
    public int getPage() {

        return m_page;
    }

    /**
     * Creates a sorted map of URLs to link to other search result pages.<p>
     * 
     * The key values are Integers representing the page number, the entry 
     * holds the corresponding link.<p>
     *  
     * @return a map with String URLs
     */
    public Map getPageLinks() {

        Map links = new TreeMap();
        if (m_pageCount <= 1) {
            return links;
        }
        int startIndex, endIndex;
        String link = m_cms.getRequestContext().getUri() + this.getSearchParameters() + "&page=";
        if (getDisplayPages() < 1) {
            // number of displayed pages not limited, build a map with all available page links 
            startIndex = 1;
            endIndex = m_pageCount;
        } else {
            // limited number of displayed pages, calculate page range
            int currentPage = getPage();
            int countBeforeCurrent = getDisplayPages() / 2;
            int countAfterCurrent;
            if ((currentPage - countBeforeCurrent) < 1) {
                // set count before to number of available pages 
                countBeforeCurrent = currentPage - 1;
            }
            // set count after to number of remaining pages (- 1 for current page) 
            countAfterCurrent = getDisplayPages() - countBeforeCurrent - 1;
            // calculate start and end index
            startIndex = currentPage - countBeforeCurrent;
            endIndex = currentPage + countAfterCurrent;
            // check end index
            if (endIndex > m_pageCount) {
                int delta = endIndex - m_pageCount;
                // decrease start index with delta to get the right number of displayed pages
                startIndex -= delta;
                // check start index to avoid values < 1
                if (startIndex < 1) {
                    startIndex = 1;
                }
                endIndex = m_pageCount;
            }
        }

        // build the sorted tree map of page links
        for (int i = startIndex; i <= endIndex; i++) {
            links.put(new Integer(i), (link + i));
        }
        return links;
    }

    /**
     * Gets the URL for the link to the previous result page.<p>
     * 
     * @return the URL to the previous result page
     */
    public String getPreviousUrl() {

        return m_prevUrl;
    }

    /**
     * Gets the current search query.<p>
     * 
     * @return the current query string or null if no query was set before
     */
    public String getQuery() {

        return m_query;
    }

    /**
     * Gets the minimum search query length.<p>
     * 
     * @return the minimum search query length
     */
    public int getQueryLength() {

        return m_queryLength;
    }

    /**
     * Creates a String with the necessary search parameters for page links.<p>
     * 
     * @return String with search parameters
     */
    public String getSearchParameters() {

        if (m_searchParameters == null) {
            StringBuffer params = new StringBuffer(128);
            params.append("?action=search&query=");
            String query = CmsStringUtil.substitute(m_query, "+", "%2B");
            query = CmsStringUtil.substitute(query, "-", "%2D");
            params.append(CmsEncoder.encode(query, OpenCms.getSystemInfo().getDefaultEncoding()));
            params.append("&matchesPerPage=");
            params.append(this.getMatchesPerPage());
            params.append("&displayPages=");
            params.append(this.getDisplayPages());
            params.append("&index=");
            params.append(CmsEncoder.encode(m_indexName));
            params.append("&searchRoot=");
            params.append(CmsEncoder.encode(m_searchRoot));            
            m_searchParameters = params.toString();
            return m_searchParameters;
        } else {
            return m_searchParameters;
        }
    }

    /**
     * Gets the search result for the current query.<p>
     * 
     * @return the search result (may be empty) or null if no index or query was set before
     */
    public List getSearchResult() {

        if (m_cms != null && m_result == null && m_index != null && m_query != null && !"".equals(m_query.trim())) {

            if ((this.getQueryLength() > 0) && (m_query.trim().length() < this.getQueryLength())) {

                m_lastException = new CmsSearchException("Search query too short, enter at least "
                    + this.getQueryLength()
                    + " characters!");

                return m_result;
            }

            try {
                List result;
                
                if (m_fields != null) {
                    result = m_index.search(m_cms, m_searchRoot, m_query, m_fields, m_page, m_matchesPerPage);
                } else {
                    result = m_index.search(m_cms, m_searchRoot, m_query, m_page, m_matchesPerPage);
                }
                
                if (result.size() > 1) {
                    // the total number of search results matching the query is saved at the last index in the result 
                    // list. the search result list contains just m_matchesPerPage search results instead of all search 
                    // results due to performance reasons.
                    Integer searchResultCount = (Integer)result.get(result.size() - 1);
                    m_searchResultCount = searchResultCount.intValue();
                    m_result = result.subList(0, result.size() - 1);

                    // re-caluclate the number of pages for this search result
                    m_pageCount = m_searchResultCount / m_matchesPerPage;
                    if ((m_searchResultCount % m_matchesPerPage) != 0) {
                        m_pageCount++;
                    }

                    // re-calculate the URLs to browse forward and backward in the search result
                    String url = m_cms.getRequestContext().getUri() + getSearchParameters() + "&page=";
                    if (m_page > 1) {
                        m_prevUrl = url + (m_page - 1);
                    }
                    if (m_page < m_pageCount) {
                        m_nextUrl = url + (m_page + 1);
                    }
                } else {
                    m_result = Collections.EMPTY_LIST;
                    m_searchResultCount = 0;
                    m_pageCount = 0;
                    m_prevUrl = null;
                    m_nextUrl = null;
                }
            } catch (Exception exc) {

                if (OpenCms.getLog(this).isDebugEnabled()) {
                    OpenCms.getLog(this).debug("[" + this.getClass().getName() + "] " + "Searching failed", exc);
                }

                m_result = null;
                m_searchResultCount = 0;
                m_pageCount = 0;
                
                m_lastException = exc;
            }
        }

        return m_result;
    }

    /**
     * Initializes the bean with the cms object.<p>
     * 
     * @param cms the cms object
     */
    public void init(CmsObject cms) {

        m_cms = cms;
        m_result = null;
        m_lastException = null;
        m_pageCount = 0;
        m_nextUrl = null;
        m_prevUrl = null;
        m_searchParameters = null;

        if (m_indexName != null) {
            setIndex(m_indexName);
        }
    }

    /**
     * Sets the fields to search.<p>
     * 
     * If the fields are set to <code>null</code>, 
     * or not set at all, the default fields "content" and "meta" are used.<p>
     * 
     * For a list of valid field names, see the Interface constants of
     * <code>{@link org.opencms.search.documents.I_CmsDocumentFactory}</code>. 
     * 
     * @param fields the fields to search
     */
    public void setField(String[] fields) {

        m_fields = fields;
        m_result = null;
        m_lastException = null;
    }

    /**
     * Set the name of the index to search.<p>
     * 
     * A former search result will be deleted.<p>
     * 
     * @param indexName the name of the index
     */
    public void setIndex(String indexName) {

        m_indexName = indexName;
        m_result = null;
        m_index = null;
        m_lastException = null;

        if (m_cms != null && indexName != null && !"".equals(indexName)) {
            try {
                m_index = OpenCms.getSearchManager().getIndex(indexName);
                if (m_index == null) {
                    throw new CmsException("Index " + indexName + " not found");
                }
            } catch (Exception exc) {
                if (OpenCms.getLog(this).isDebugEnabled()) {
                    OpenCms.getLog(this).debug(
                        "[" + this.getClass().getName() + "] " + "Accessing index " + indexName + " failed",
                        exc);
                }
                m_lastException = exc;
            }
        }
    }

    /**
     * Sets the number of matches per page.<p>
     * 
     * @param matches the number of matches per page
     */
    public void setMatchesPerPage(int matches) {

        m_matchesPerPage = matches;
    }

    /**
     * Sets the current result page.<p>
     * 
     * @param page the current result page
     */
    public void setPage(int page) {

        m_page = page;
        m_result = null;
        m_lastException = null;
    }

    /**
     * Sets the search query.<p>
     * 
     * The syntax of the query depends on the search engine used. 
     * A former search result will be deleted.<p>
     * 
     * @param query the search query (escaped format)
     */
    public void setQuery(String query) {

        m_query = CmsEncoder.decode(query, OpenCms.getSystemInfo().getDefaultEncoding());
        m_result = null;
        m_lastException = null;
    }

    /**
     * Sets the minimum length of the search query.<p>
     * 
     * @param length the minimum search query length
     */
    public void setQueryLength(int length) {

        m_queryLength = length;
    }

    /**
     * Returns the search root.<p>
     * 
     * Only resource that are sub-resource of the search root
     * are included in the search result.<p>
     * 
     * Per default, the search root is an empty string.<p>
     *
     * @return the search root
     */
    public String getSearchRoot() {

        return m_searchRoot;
    }

    /**
     * Sets the search root.<p>
     * 
     * Only resource that are sub-resource of the search root
     * are included in the search result.<p>
     * 
     * Per default, the search root is an empty string.<p>
     *
     * @param searchRoot the search root to set
     */
    public void setSearchRoot(String searchRoot) {

        m_searchRoot = searchRoot;
    }
    
    /**
     * Returns the total number of search results matching the query.<p>
     * 
     * @return the total number of search results matching the query
     */
    public int getSearchResultCount() {
        
        return m_searchResultCount;
    }
    
}