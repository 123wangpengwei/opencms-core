/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsAdminLoggedInUsers.java,v $
* Date   : $Date: 2005/03/06 09:26:11 $
* Version: $Revision: 1.14 $
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

import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsUser;
import org.opencms.main.CmsException;
import org.opencms.main.CmsSessionInfo;
import org.opencms.main.I_CmsConstants;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsDateUtil;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.PrintfFormat;
import org.opencms.workplace.CmsReport;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Template class for displaying OpenCms workplace admin users screens.
 * <P>
 *
 * @author Mario Stanke
 * @version $Revision: 1.14 $ $Date: 2005/03/06 09:26:11 $
 * @see com.opencms.workplace.CmsXmlWpTemplateFile
 * 
 * @deprecated Will not be supported past the OpenCms 6 release.
 */

public class CmsAdminLoggedInUsers extends CmsWorkplaceDefault  {

    /**
     * Gets the content of a defined section in a given template file and its subtemplates
     * with the given parameters.
     *
     * @see #getContent(CmsObject, String, String, Hashtable, String)
     * @param cms CmsObject Object for accessing system resources.
     * @param templateFile Filename of the template file.
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     */

    public byte[] getContent(CmsObject cms, String templateFile, String elementName,
            Hashtable parameters, String templateSelector) throws CmsException {

        CmsXmlWpTemplateFile xmlTemplateDocument = new CmsXmlWpTemplateFile(cms, templateFile);

        if (parameters.get("message") != null) {
            // there is a message to all - send it
            OpenCms.getSessionManager().sendBroadcast(cms, (String)parameters.get("message"));
        }

        List sessionInfos = OpenCms.getSessionManager().getSessionInfos();
        // sort the session infos
        Collections.sort(sessionInfos);
        StringBuffer ret = new StringBuffer();
        Iterator i = sessionInfos.iterator();
        int count = 1;
        PrintfFormat format = new PrintfFormat("%3.3d");
        while (i.hasNext()) {
            try {
                CmsSessionInfo info = (CmsSessionInfo)i.next();
                CmsUser cmsUser = info.getUser();
                CmsProject cmsProject = cms.readProject(info.getProject().intValue());
                xmlTemplateDocument.setData("username", cmsUser.getName());
                xmlTemplateDocument.setData("firstname", cmsUser.getFirstname());
                xmlTemplateDocument.setData("lastname", cmsUser.getLastname());
                xmlTemplateDocument.setData("email", cmsUser.getEmail());
                // misuse of deprecated current group for session time, until we get a new interface
                xmlTemplateDocument.setData("currentgroup",     
                    format.sprintf(count)
                    + " - "
                    + CmsDateUtil.getDateTimeShort(info.getTimeCreated())
                    + " - "
                    + CmsStringUtil.formatRuntime(System.currentTimeMillis() - info.getTimeUpdated()));
                xmlTemplateDocument.setData("messagepending", 
                    String.valueOf(!info.getBroadcastQueue().isEmpty()));
                xmlTemplateDocument.setData("currentproject", 
                    cmsProject.getName()
                    + " - "
                    + info.getCurrentSiteRoot());
                ret.append(xmlTemplateDocument.getProcessedDataValue("line"));
                count++;
            } catch (Exception exc) {
                // ignore all exceptions - don't show this user
            }
        }

        xmlTemplateDocument.setData("all_lines", ret.toString());

        // Now load the template file and start the processing
        return startProcessing(cms, xmlTemplateDocument, elementName, parameters, templateSelector);
    }
}
