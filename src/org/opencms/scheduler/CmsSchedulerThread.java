/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/scheduler/CmsSchedulerThread.java,v $
 * Date   : $Date: 2004/07/08 12:19:02 $
 * Version: $Revision: 1.3 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2004 Alkacon Software (http://www.alkacon.com)
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

package org.opencms.scheduler;

import org.opencms.main.OpenCms;

/**
 * A worker thread for the OpenCms scheduler.<p>
 * 
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 *  
 * @version $Revision: 1.3 $
 * @since 5.3
 */
public class CmsSchedulerThread extends Thread {

    /** The scheduler thread pool this thread belongs to. */
    private CmsSchedulerThreadPool m_pool;

    /** A flag that signals the thread to terminate. */
    private boolean m_run;

    /** A runnable class. */
    private Runnable m_runnable;

    /**
     * Create a scheduler thread that runs continuosly,
     * waiting for new runnables to be provided by the scheduler thread pool.<p>
     * 
     * @param pool the pool to use
     * @param threadGroup the thread group to use
     * @param threadName the name for the thread
     * @param prio the priority of the thread
     * @param isDaemon controls if this should be a deamon thread or not
     */
    CmsSchedulerThread(
        CmsSchedulerThreadPool pool,
        ThreadGroup threadGroup,
        String threadName,
        int prio,
        boolean isDaemon) {

        this(pool, threadGroup, threadName, prio, isDaemon, null);
    }

    /**
     * Create a scheduler thread that runs the specified runnable exactly once.<p>
     * 
     * @param pool the pool to use
     * @param threadGroup the thread group to use
     * @param threadName the name for the thread
     * @param prio the priority of the thread
     * @param isDaemon controls if this should be a deamon thread or not
     * @param runnable the runnable to run
     */
    CmsSchedulerThread(
        CmsSchedulerThreadPool pool,
        ThreadGroup threadGroup,
        String threadName,
        int prio,
        boolean isDaemon,
        Runnable runnable) {

        super(threadGroup, threadName);
        m_run = true;
        m_pool = pool;
        m_runnable = runnable;
        setPriority(prio);
        setDaemon(isDaemon);
        start();
    }

    /**
     * Loop, executing targets as they are received.<p>
     */
    public void run() {

        boolean runOnce = (m_runnable != null);

        while (m_run) {
            setPriority(m_pool.getThreadPriority());
            try {
                if (m_runnable == null) {
                    m_runnable = m_pool.getNextRunnable();
                }

                if (m_runnable != null) {
                    m_runnable.run();
                }
            } catch (InterruptedException e) {
                OpenCms.getLog(this).error("Scheduler thread '" + getName() + "' interrupted", e);
            } catch (Throwable t) {
                OpenCms.getLog(this).error("Scheduler error in thread '" + getName() + "' while executing", t);
            } finally {
                if (runOnce) {
                    m_run = false;
                }
                m_runnable = null;
            }
        }
        if (OpenCms.getLog(this).isDebugEnabled()) {
            OpenCms.getLog(this).debug("Scheduler thread '" + getName() + "' is shutting down");
        }
    }

    /**
     * Signal the thread that it should terminate.<p>
     */
    void shutdown() {

        m_run = false;
    }
}