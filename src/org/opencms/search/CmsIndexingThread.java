/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/search/CmsIndexingThread.java,v $
 * Date   : $Date: 2005/05/25 09:28:36 $
 * Version: $Revision: 1.15 $
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
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.report.I_CmsReport;
import org.opencms.search.documents.I_CmsDocumentFactory;

import org.apache.commons.logging.Log;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;

/**
 * Implements the indexing method for a single resource as thread.<p>
 * 
 * The indexing of a single resource was wrapped into a single thread
 * in order to prevent the indexer from hanging.<p>
 *  
 * @version $Revision: 1.15 $ $Date: 2005/05/25 09:28:36 $
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 * @since 5.3.1
 */
public class CmsIndexingThread extends Thread {

    /** Internal debug flag. */
    private static final boolean DEBUG = false;

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsIndexingThread.class);  
    
    /** The cms object. */
    CmsObject m_cms;
    
    /** The index writer. */
    IndexWriter m_writer;

    /** The resource to index. */
    A_CmsIndexResource m_res;

    /** The current index. */
    CmsSearchIndex m_index;

    /** The current report. */
    I_CmsReport m_report;

    /** The thread manager. */
    CmsIndexingThreadManager m_threadManager;

    /**
     * Creates a new indexing thread for a single resource.<p>
     * 
     * @param cms the cms object
     * @param writer the writer
     * @param res the resource to index
     * @param index the index
     * @param report the report to write out progress information
     * @param threadManager the thread manager
     */
    public CmsIndexingThread(
        CmsObject cms,
        IndexWriter writer,
        A_CmsIndexResource res,
        CmsSearchIndex index,
        I_CmsReport report,
        CmsIndexingThreadManager threadManager) {

        super("OpenCms: Indexing '" + res.getName() + "'");

        m_cms = cms;
        m_writer = writer;
        m_res = res;
        m_index = index;
        m_report = report;
        m_threadManager = threadManager;
    }

    /**
     * Starts the thread to index a single resource.<p>
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {

        I_CmsDocumentFactory documentFactory = OpenCms.getSearchManager().getDocumentFactory(m_res);
        if (documentFactory != null
            && !m_index.getDocumenttypes(m_res.getRootPath()).contains(documentFactory.getName())) {
            documentFactory = null;
        }

        if (LOG.isDebugEnabled()) {
            if (documentFactory != null) {
                LOG.debug(Messages.get().key(Messages.LOG_INDEXING_DOC_ROOT_1, documentFactory.getName()));
            } else {
                LOG.debug(Messages.get().key(Messages.LOG_INDEXING_0));
            }    
        }

        if (documentFactory != null) {
            try {

                if (DEBUG && LOG.isDebugEnabled()) {
                    LOG.debug(Messages.get().key(Messages.LOG_CREATING_INDEX_DOC_0));
                }

                Document doc = documentFactory.newInstance(m_cms, m_res, m_index.getLocale());
                if (doc == null) {
                    throw new CmsIndexException(Messages.get().container(Messages.ERR_CREATING_INDEX_DOC_0));
                }

                if (DEBUG && LOG.isDebugEnabled()) {
                    LOG.debug(Messages.get().key(Messages.LOG_WRITING_INDEX_TO_WRITER_1, ((m_writer != null) ? m_writer.toString() : "null")));
                }

                if (!isInterrupted()) {
                    m_writer.addDocument(doc);
                }

                if (m_report != null && !isInterrupted()) {
                    m_report.println(Messages.get().container(
                        Messages.RPT_SEARCH_INDEXING_FILE_END_0), I_CmsReport.C_FORMAT_OK);
                    if (DEBUG && LOG.isDebugEnabled()) {
                        LOG.debug(Messages.get().key(Messages.LOG_WRITE_SUCCESS_0));
                    }
                }

                if (isInterrupted() && LOG.isDebugEnabled()) {
                    LOG.debug(Messages.get().key(Messages.LOG_ABANDONED_THREAD_FINISHED_1, m_res.getRootPath()));
                }

            } catch (Exception exc) {

                if (m_report != null) {
                    m_report.println();
                    m_report.print(Messages.get().container(
                        Messages.RPT_SEARCH_INDEXING_FILE_FAILED_0), I_CmsReport.C_FORMAT_WARNING);
                    m_report.println(exc);
                    
                }
                if (LOG.isWarnEnabled()) {
                    LOG.warn(Messages.get().key(Messages.LOG_INDEX_FAILED_1, m_res.getRootPath()), exc);
                }
            }
        } else {

            if (m_report != null) {
                m_report.println(Messages.get().container(
                    Messages.RPT_SEARCH_INDEXING_FILE_SKIPPED_0), I_CmsReport.C_FORMAT_NOTE);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug(Messages.get().key(Messages.LOG_SKIPPED_1, m_res.getRootPath()));
            }    
            
        }

        m_threadManager.finished();
    }
}