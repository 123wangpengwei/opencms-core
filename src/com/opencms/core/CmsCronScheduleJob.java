/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/core/Attic/CmsCronScheduleJob.java,v $
* Date   : $Date: 2001/11/16 09:36:34 $
* Version: $Revision: 1.2 $
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

import com.opencms.file.*;
import com.opencms.util.*;
import com.opencms.boot.*;

/**
 * This thread launches one job in its own thread.
 */
class CmsCronScheduleJob extends Thread {

    /** The CmsObject to get access to the system */
    private CmsObject m_cms;

    /** The cron entry for this job */
    private CmsCronEntry m_entry;

    /**
     * Creates a new CmsCronScheduleJob.
     * @param cms the CmsObject with an logged in user.
     * @poaram entry the entry to launch.
     */
    CmsCronScheduleJob(CmsObject cms, CmsCronEntry entry) {
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
            I_CmsCronJob job = (I_CmsCronJob) module.newInstance();
            // invoke method launch
            String retValue = job.launch(m_cms, m_entry.getModuleParameter());
            // log the returnvalue to the logfile
            if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && CmsBase.isLogging()) {
                CmsBase.log(I_CmsLogChannels.C_OPENCMS_CRONSCHEDULER, "Successful launch of job " + m_entry +  (retValue != null ? " Message: " + retValue : "") );
            }
        } catch(Exception exc) {
            // log the exception
            if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && CmsBase.isLogging()) {
                CmsBase.log(I_CmsLogChannels.C_OPENCMS_CRONSCHEDULER, "Error running job for " + m_entry + " Error: " + Utils.getStackTrace(exc));
            }
        }
    }

}
