/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/core/Attic/CmsCronScheduleJob.java,v $
* Date   : $Date: 2003/09/16 12:06:10 $
* Version: $Revision: 1.6 $
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

import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;

import com.opencms.file.CmsObject;
import com.opencms.util.Utils;

/**
 * This thread launches one job in its own thread.
 */
public class CmsCronScheduleJob extends Thread {

    /** The CmsObject to get access to the system */
    private CmsObject m_cms;

    /** The cron entry for this job */
    private CmsCronEntry m_entry;

    /**
     * Creates a new CmsCronScheduleJob.
     * @param cms the CmsObject with an logged in user.
     * @param entry the entry to launch.
     */
    public CmsCronScheduleJob(CmsObject cms, CmsCronEntry entry) {
        m_cms = cms;
        m_entry = entry;
    }

    /**
     * The run method of this thread loads the module-class and launches the Method
     * launch() on this module.
     */
    public void run() {
        try {
            // load the job class
            Class module = getClass().getClassLoader().loadClass(m_entry.getModuleName());
            // create an instance
            I_CmsCronJob job = (I_CmsCronJob)module.newInstance();
            // invoke method launch
            String retValue = job.launch(m_cms, m_entry.getModuleParameter());
            // log the returnvalue to the logfile
            if (OpenCms.isLogging(CmsLog.CHANNEL_CRON, CmsLog.LEVEL_WARN)) {
                OpenCms.log(CmsLog.CHANNEL_CRON, CmsLog.LEVEL_WARN, "Successful launch of job " + m_entry + (retValue != null ? " Message: " + retValue : ""));
            }
        } catch (Exception exc) {
            // log the exception
            if (OpenCms.isLogging(CmsLog.CHANNEL_CRON, CmsLog.LEVEL_WARN)) {
                OpenCms.log(CmsLog.CHANNEL_CRON, CmsLog.LEVEL_WARN, "Error running job for " + m_entry + " Error: " + Utils.getStackTrace(exc));
            }
        }
    }

}
