/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/threads/Attic/CmsDatabaseImportThread.java,v $
 * Date   : $Date: 2003/09/16 12:06:09 $
 * Version: $Revision: 1.3 $
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

package org.opencms.threads;

import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.report.A_CmsReportThread;
import org.opencms.report.I_CmsReport;

import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.file.CmsObject;

/**
 * Imports an OpenCms export file into the VFS.<p>
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.3 $
 * @since 5.1.10
 */
public class CmsDatabaseImportThread extends A_CmsReportThread {

    private String m_importFile;

    /**
     * Imports an OpenCms export file into the VFS.<p>
     * 
     * @param cms the current OpenCms context object
     * @param importFile the file to import
     */
    public CmsDatabaseImportThread(CmsObject cms, String importFile) {
        super(cms, "OpenCms: Database import from " + importFile);
        m_importFile = importFile;
        initHtmlReport();
    }

    /**
     * @see org.opencms.report.A_CmsReportThread#getReportUpdate()
     */
    public String getReportUpdate() {
        return getReport().getReportUpdate();
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        try {
            getReport().println(getReport().key("report.import_db_begin"), I_CmsReport.C_FORMAT_HEADLINE);
            getCms().importResources(m_importFile, I_CmsConstants.C_ROOT, getReport());
            getReport().println(getReport().key("report.import_db_end"), I_CmsReport.C_FORMAT_HEADLINE);
        } catch (CmsException e) {
            getReport().println(e);
            if (OpenCms.isLogging(CmsLog.C_OPENCMS_CRITICAL, CmsLog.LEVEL_WARN)) {
                OpenCms.log(CmsLog.C_OPENCMS_CRITICAL, CmsLog.LEVEL_WARN, e.getMessage());
            }
        }
    }
}
