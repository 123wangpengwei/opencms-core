/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/threads/Attic/CmsDatabaseImportThread.java,v $
 * Date   : $Date: 2005/02/17 12:44:32 $
 * Version: $Revision: 1.12 $
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

package org.opencms.threads;

import org.opencms.file.CmsObject;
import org.opencms.main.CmsException;
import org.opencms.main.I_CmsConstants;
import org.opencms.main.OpenCms;
import org.opencms.report.A_CmsReportThread;

/**
 * Imports an OpenCms export file into the VFS.<p>
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @version $Revision: 1.12 $
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
        super(cms, "OpenCms: Import from " + importFile);
        m_importFile = importFile;
        initOldHtmlReport(cms.getRequestContext().getLocale());
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
            OpenCms.getImportExportManager().importData(getCms(), m_importFile, I_CmsConstants.C_ROOT, getReport());
        } catch (CmsException e) {
            getReport().println(e);
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Error importing the database", e);
            }
        }
    }
}
