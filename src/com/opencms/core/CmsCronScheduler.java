/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/core/Attic/CmsCronScheduler.java,v $
* Date   : $Date: 2003/09/17 08:31:30 $
* Version: $Revision: 1.11 $
*
* This library is part of OpenCms -
* the Open Source Content Mananagement System
*
* Copyright (C) 2001  The OpenCms Group
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* For further information about OpenCms, please see the
* OpenCms Website: http://www.opencms.org
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package com.opencms.core;

import org.opencms.main.*;


import java.util.*;

/**
 * This is the CronScheduler. It a deamon-thread that will be stopped
 * after the system has stopped. It awaiks every minute and starts
 * cronjobs at issue.
 */
public class CmsCronScheduler extends Thread {

    /** The crontable to use */
    private CmsCronTable m_table;

    /** The A_OpenCms to get access to the system */
    private OpenCmsCore m_opencms;
    
    /** Flag to indicate if OpenCms has already been shut down */
    private boolean m_destroyed = false;

    /**
     * Constructs a new scheduler.
     * @param opencms to get access to A_OpenCms
     * @param table the CmsCronTable with all CmsCronEntry's to launch.
     */
    public CmsCronScheduler(OpenCmsCore opencms, CmsCronTable table) {
        super("OpenCms: CronScheduler");
        // store the crontable
        m_table = table;
        // store the OpenCms to get access to the system
        m_opencms = opencms;

        // set to deamon thread - so java will stop correctly
        setDaemon(true);
        // start the thread
        start();
    }

    /**
     * The run-method of this thread awakes every minute to launch jobs at issue.
     */
    public void run() {
        Calendar lastRun = new GregorianCalendar();
        Calendar thisRun;
        CmsCronScheduleJobStarter jobStarter;
        while(! m_destroyed) { // do this as long as OpenCms runs
            try {
                Calendar tmp = new GregorianCalendar(lastRun.get(Calendar.YEAR),
                                                     lastRun.get(Calendar.MONTH),
                                                     lastRun.get(Calendar.DATE),
                                                     lastRun.get(Calendar.HOUR_OF_DAY),
                                                     lastRun.get(Calendar.MINUTE)+1,
                                                     0);
                long sleeptime = tmp.getTime().getTime() - new GregorianCalendar().getTime().getTime();
                if(sleeptime > 0) {
                    // sleep til next minute plus ten seconds to get to the next minute
                    sleep(sleeptime + 10000);
                }
            } catch(InterruptedException exc) {
                // ignore this exception - we are interrupted
            }
            if(! m_destroyed) { 
                // if not destroyed, read the current values for the crontable from the system
                m_opencms.updateCronTable();
                thisRun = new GregorianCalendar();
                jobStarter = new CmsCronScheduleJobStarter(m_opencms, m_table, thisRun, lastRun);
                jobStarter.start();
                lastRun = thisRun;
            }
        }
    }
    
    /** 
     * Shut down this instance of the CronScheduler Thread.<p>
     */
    public void shutDown() {
        m_destroyed = true;
        interrupt(); 
        if(OpenCms.getLog(CmsLog.CHANNEL_INIT).isInfoEnabled()) {
            OpenCms.getLog(CmsLog.CHANNEL_INIT).info("[" + this.getClass().getName() + "] destroyed!");
        }
    } 
}
