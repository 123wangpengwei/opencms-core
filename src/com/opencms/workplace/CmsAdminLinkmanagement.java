/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsAdminLinkmanagement.java,v $
* Date   : $Date: 2003/03/05 18:43:10 $
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

package com.opencms.workplace;

import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.core.I_CmsSession;
import com.opencms.file.CmsObject;
import com.opencms.report.A_CmsReportThread;

import java.util.Hashtable;

/**
 * Workplace class for the Check Project / Check HTML Links backoffice item.
 * 
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * @author Hanjo Riege
 * @version $Revision: 1.6 $
 */
public class CmsAdminLinkmanagement extends CmsWorkplaceDefault implements I_CmsConstants{

    private final String C_LINKCHECK_HTML_THREAD = "C_LINKCHECK_HTML_THREAD";

    public byte[] getContent(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {

        CmsXmlWpTemplateFile templateDocument = (CmsXmlWpTemplateFile)getOwnTemplateFile(cms, templateFile, elementName, parameters, templateSelector);
        I_CmsSession session = cms.getRequestContext().getSession(true);
        String action = (String)parameters.get("action");
        A_CmsReportThread doCheck = null;
        CmsXmlLanguageFile lang = templateDocument.getLanguageFile();

        String text = lang.getLanguageValue("linkmanagement.label.text1")
                        + cms.getRequestContext().currentProject().getName()
                        + lang.getLanguageValue("linkmanagement.label.text2");
                        
        if("start".equals(action)){
            // first call - start checking
            doCheck = new CmsAdminLinkmanagementThread(cms, cms.getRequestContext().currentProject().getId());
            doCheck.start();
            session.putValue(C_LINKCHECK_HTML_THREAD, doCheck);

            templateDocument.setData("text", text);            
            templateDocument.setData("data", "");
            templateDocument.setData("endMethod", "");
        }else if("working".equals(action)){
            doCheck = (CmsAdminLinkmanagementThread)session.getValue(C_LINKCHECK_HTML_THREAD);
            
            if(doCheck.isAlive()){
                templateDocument.setData("text", text);                                
                templateDocument.setData("endMethod", "");
            }else{
                text += "<br>" + lang.getLanguageValue("linkmanagement.label.textende");                                                
                
                templateDocument.setData("autoUpdate","");
                templateDocument.setData("text", text);

                session.removeValue(C_LINKCHECK_HTML_THREAD);
            }
            templateDocument.setData("data", doCheck.getReportUpdate());
        }
        // now load the template file and start the processing
        return startProcessing(cms, templateDocument, elementName, parameters, templateSelector);
    }
}