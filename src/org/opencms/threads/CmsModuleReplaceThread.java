/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/threads/Attic/CmsModuleReplaceThread.java,v $
 * Date   : $Date: 2005/02/17 12:44:32 $
 * Version: $Revision: 1.10 $
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
import org.opencms.main.OpenCms;
import org.opencms.report.A_CmsReportThread;

import java.util.Vector;

/**
 * Replaces a module.<p>
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * 
 * @version $Revision: 1.10 $
 * 
 * @since 5.1.10
 */
public class CmsModuleReplaceThread extends A_CmsReportThread {

    private static final boolean DEBUG = false;
    private A_CmsReportThread m_deleteThread;
    private A_CmsReportThread m_importThread;
    private String m_moduleName;
    private int m_phase;
    private String m_reportContent;
    private String m_zipName;

    /**
     * Creates the module replace thread.<p>
     * @param cms the current cms context  
     * @param moduleName the name of the module 
     * @param zipName the name of the module ZIP file
     */
    public CmsModuleReplaceThread(CmsObject cms, String moduleName, String zipName) {

        super(cms, "OpenCms: Module replacement of " + moduleName);
        m_moduleName = moduleName;
        m_zipName = zipName;

        m_deleteThread = new CmsModuleDeleteThread(getCms(), m_moduleName, true);
        m_importThread = new CmsDatabaseImportThread(getCms(), m_zipName);
        if (DEBUG) {
            System.err.println("CmsAdminModuleReplaceThread() constructed");
        }
        m_phase = 0;
    }

    /**
     * Collects all resource names belonging to a module in a Vector.<p>
     * 
     * @param moduleName the name of the module
     * 
     * @return Vector with path Strings of resources
     */
    public static Vector getModuleResources(String moduleName) {

        Vector resNames = new Vector(OpenCms.getModuleManager().getModule(moduleName).getResources());
        return resNames;
    }

    /**
     * @see org.opencms.report.A_CmsReportThread#getReportUpdate()
     */
    public String getReportUpdate() {

        switch (m_phase) {
            case 1:
                return m_deleteThread.getReportUpdate();
            case 2:
                String content;
                if (m_reportContent != null) {
                    content = m_reportContent;
                    m_reportContent = null;
                } else {
                    content = "";
                }
                return content + m_importThread.getReportUpdate();
            default:
                // noop
        }
        return "";
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        if (DEBUG) {
            System.err.println("CmsAdminModuleReplaceThread() starting delete action ");
        }
        // phase 1: delete the existing module  
        m_phase = 1;
        m_deleteThread.run();
        // get remaining report contents
        m_reportContent = m_deleteThread.getReportUpdate();
        if (DEBUG) {
            System.err.println("CmsAdminModuleReplaceThread() starting import action ");
        }
        // phase 2: import the new module 
        m_phase = 2;
        m_importThread.run();
        if (DEBUG) {
            System.err.println("CmsAdminModuleReplaceThread() finished ");
        }
    }
}