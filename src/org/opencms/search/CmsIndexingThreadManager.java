/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/search/CmsIndexingThreadManager.java,v $
 * Date   : $Date: 2004/02/13 11:27:46 $
 * Version: $Revision: 1.2 $
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
package org.opencms.search;

import org.opencms.main.OpenCms;
import org.opencms.report.I_CmsReport;

import org.apache.lucene.index.IndexWriter;

/**
 * @version $Revision: 1.2 $ $Date: 2004/02/13 11:27:46 $
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 */
public class CmsIndexingThreadManager extends Thread {

    // the index manager
    CmsSearchManager m_manager;
    
    // the report
    I_CmsReport m_report;
    
    // timeout for abandoning threads
    long m_timeout;
    
    // overall number of threads started
    int m_fileCounter;
    
    // number of threads abandoned
    int m_abandonedCounter;
    
    // number of thread returned
    int m_returnedCounter;
    
    /**
     * Creates and starts a thread manager for indexing threads<p>
     * 
     * @param manager the index manager
     * @param report the report to write out progress information
     * @param timeout timeout after a thread is abandoned
     */
    public CmsIndexingThreadManager(CmsSearchManager manager, I_CmsReport report, long timeout) {
    
        m_manager = manager;
        m_report = report;
        m_timeout = timeout;
        m_fileCounter = 0;
        m_abandonedCounter = 0;
        m_returnedCounter = 0;
        
        this.start();    
    }
    
    /**
     * Creates and starts a new indexing thread for a resource<p>
     * 
     * @param writer the write to write the index
     * @param res the resource
     * @param index the index
     */
    public void createIndexingThread(IndexWriter writer, CmsIndexResource res, CmsSearchIndex index) {

        CmsIndexingThread thread = new CmsIndexingThread(m_manager, writer, res, index, m_report, this);

        try {
            m_fileCounter++;
            thread.start();
            thread.join(m_timeout);
            
            if (thread.isAlive()) {
                
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Timeout while indexing file " + res.getRootPath() + ", abandoning thread");
                }
                   
                m_report.println();
                m_report.println(m_report.key("search.indexing_file_failed") + " : " + "Timeout while indexing file " + res.getRootPath() + ", abandoning thread",
                    I_CmsReport.C_FORMAT_WARNING);
                
                m_abandonedCounter++;    
                thread.interrupt();
            }         
        } catch (InterruptedException exc) {
            // noop
        }        
    }
    
    /**
     * Writes statistical information to the report<p>
     */
    public void reportStatistics() {

        StringBuffer stats = new StringBuffer();
        stats.append(m_report.key("search.indexing_stats"));
        stats.append(m_report.key("search.indexing_stats_files"));
        stats.append(m_fileCounter + ",");
        stats.append(m_report.key("search.indexing_stats_returned"));
        stats.append(m_returnedCounter + ",");  
        stats.append(m_report.key("search.indexing_stats_abandoned"));
        stats.append(m_abandonedCounter + ",");      
        stats.append(m_report.key("search.indexing_stats_duration"));
        stats.append(m_report.formatRuntime()); 
            
        if (OpenCms.getLog(this).isInfoEnabled()) {
            OpenCms.getLog(this).info(stats.toString());
        }
                        
        if (m_report != null) {
    
            m_report.println(m_report.key("search.indexing_end"), I_CmsReport.C_FORMAT_HEADLINE);
            m_report.println(stats.toString());
        }        
    }
    
    /**
     * Gets the current thread (file) count<p>
     * 
     * @return the current thread count
     */
    public int getCounter() {
        return m_fileCounter;
    }

    /**
     * Signals the thread manager that a thread has finished its job and will exit immediately<p>
     */
    public synchronized void finished() {
            m_returnedCounter++;    
    }   
         
    /**
     * Starts the thread manager to look for non-terminated threads<p>
     * The thread manager looks all 10 minutes if threads are not returned
     * and reports the number to the log file.
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {
        
        int max = 5;
        
        try {
            Thread.sleep(600000);
            
            while (m_fileCounter > m_returnedCounter && max-- > 0) {

                Thread.sleep(600000);
                if (OpenCms.getLog(this).isWarnEnabled()) {
                    OpenCms.getLog(this).warn("Waiting for abandoned threads: " 
                        + m_abandonedCounter + " threads abandoned, " + (m_fileCounter - m_returnedCounter) + " threads not returned until now");
                }
            }
        } catch (Exception exc) {
            // noop
        }
        
        if (max > 0) {
            if (OpenCms.getLog(this).isWarnEnabled()) {
                OpenCms.getLog(this).warn("All threads finished, terminating now.");
            }
        } else {
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Abandoned threads left: " + (m_fileCounter - m_returnedCounter));
            }
        }
    }
}
