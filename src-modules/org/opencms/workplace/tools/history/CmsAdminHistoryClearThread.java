/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/workplace/tools/history/Attic/CmsAdminHistoryClearThread.java,v $
 * Date   : $Date: 2006/03/25 22:42:43 $
 * Version: $Revision: 1.8.2.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
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
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.workplace.tools.history;

import org.opencms.file.CmsObject;
import org.opencms.main.CmsException;
import org.opencms.report.A_CmsReportThread;
import org.opencms.report.I_CmsReport;

import java.util.Map;

/**
 * Clears the file history of the OpenCms database.<p>
 * 
 * @author  Andreas Zahner 
 * 
 * @version $Revision: 1.8.2.1 $ 
 * 
 * @since 6.0.0 
 */
public class CmsAdminHistoryClearThread extends A_CmsReportThread {

    private Throwable m_error;
    private Map m_params;

    /**
     * Creates the history clear Thread.<p>
     * 
     * @param cms the current OpenCms context object
     * @param params the necessary parameters to delete the backup versions
     */
    public CmsAdminHistoryClearThread(CmsObject cms, Map params) {

        super(cms, Messages.get().getBundle().key(
            Messages.GUI_ADMIN_HISTORY_CLEAR_THREAD_NAME_1,
            cms.getRequestContext().currentProject().getName()));
        m_params = params;
        initHtmlReport(cms.getRequestContext().getLocale());
        start();
    }

    /**
     * @see org.opencms.report.A_CmsReportThread#getError()
     */
    public Throwable getError() {

        return m_error;
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

        getReport().println(Messages.get().container(Messages.RPT_DELETE_HISTORY_BEGIN_0), I_CmsReport.FORMAT_HEADLINE);

        // get the necessary parameters from the map
        int versions = Integer.parseInt((String)m_params.get("versions"));
        long timeStamp = Long.parseLong((String)m_params.get("timeStamp"));

        // delete the backup files
        try {
            getCms().deleteBackups(timeStamp, versions, getReport());
        } catch (CmsException e) {
            getReport().println(e);
        }
        getReport().println(Messages.get().container(Messages.RPT_DELETE_HISTORY_END_0), I_CmsReport.FORMAT_HEADLINE);
    }
}