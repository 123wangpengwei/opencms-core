/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsAdminVfsLinkManagement.java,v $
 * Date   : $Date: 2003/07/31 13:19:36 $
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

package com.opencms.workplace;

import com.opencms.core.CmsException;
import com.opencms.core.I_CmsSession;
import com.opencms.file.CmsObject;

import java.util.Hashtable;

/**
 * Workplace class for the Check Project / Check Filesystem Links backoffice item.
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @version $Revision: 1.3 $
 */
public final class CmsAdminVfsLinkManagement extends CmsWorkplaceDefault {
    
    /** Debugging flag, this is also used in other classes for the VFS link check */
    public static final boolean DEBUG = false;

    private static final String C_LINKCHECK_VFS_THREAD = "C_LINKCHECK_VFS_THREAD";

    public byte[] getContent(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        
        CmsXmlWpTemplateFile templateDocument = (CmsXmlWpTemplateFile) this.getOwnTemplateFile(cms, templateFile, elementName, parameters, templateSelector);
        I_CmsSession session = cms.getRequestContext().getSession(true);
        String action = (String) parameters.get("action");
        CmsAdminVfsLinkManagementThread vfsLinkManagementThread = null;
        CmsXmlLanguageFile lang = templateDocument.getLanguageFile();

        String text = lang.getLanguageValue("linkmanagement.label.text1")
                        + cms.getRequestContext().currentProject().getName()
                        + lang.getLanguageValue("linkmanagement.label.text3");
                        
        if ("start".equals(action)) {
            // first call - start checking
            vfsLinkManagementThread = new CmsAdminVfsLinkManagementThread(cms);
            vfsLinkManagementThread.start();
            session.putValue(C_LINKCHECK_VFS_THREAD, vfsLinkManagementThread);

            templateDocument.setData("text", text);
            templateDocument.setData("data", "");
            templateDocument.setData("endMethod", "");
        } else if ("working".equals(action)) {
            vfsLinkManagementThread = (CmsAdminVfsLinkManagementThread)session.getValue(C_LINKCHECK_VFS_THREAD);

            if (vfsLinkManagementThread.isAlive()) {
                templateDocument.setData("endMethod", "");
            } else {
                text += "<br>" + lang.getLanguageValue("linkmanagement.label.textende");                
                
                templateDocument.setData("autoUpdate", "");
                templateDocument.setData("text", text);

                session.removeValue(CmsAdminVfsLinkManagement.C_LINKCHECK_VFS_THREAD);
            }
            templateDocument.setData("data", vfsLinkManagementThread.getReportUpdate());
        }
        // now load the template file and start the processing
        return startProcessing(cms, templateDocument, elementName, parameters, templateSelector);
    }

}
