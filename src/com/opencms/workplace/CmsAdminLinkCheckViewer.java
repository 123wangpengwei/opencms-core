/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsAdminLinkCheckViewer.java,v $
* Date   : $Date: 2003/01/20 17:57:46 $
* Version: $Revision: 1.5 $
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
import com.opencms.file.CmsObject;
import com.opencms.template.CmsXmlTemplateFile;
import com.opencms.util.Encoder;
import com.opencms.util.Utils;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Template class for displaying OpenCms workplace administration synchronisation properties.
 *
 * @version $Revision: 1.5 $ $Date: 2003/01/20 17:57:46 $
 * @author: Edna Falkenhan
 */
public class CmsAdminLinkCheckViewer extends CmsWorkplaceDefault implements I_CmsConstants {

    /**
     * Gets the content of a defined section in a given template file and its subtemplates
     * with the given parameters.
     *
     * @see getContent(CmsObject cms, String templateFile, String elementName, Hashtable parameters)
     * @param cms CmsObject Object for accessing system resources.
     * @param templateFile Filename of the template file.
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     */
    public byte[] getContent(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {

        CmsXmlTemplateFile templateDocument = getOwnTemplateFile(cms, templateFile, elementName, parameters, templateSelector);
        long length = 0;

        if("ok".equalsIgnoreCase((String)parameters.get("step"))){
            templateSelector = "done";
        } else {
            String linkcheckdate = "";
            StringBuffer linkcheckcontent = new StringBuffer();
            try {
                Hashtable linkchecktable = cms.readLinkCheckTable();
                Enumeration keys = linkchecktable.keys();
                while(keys.hasMoreElements()) {
                    String curKey = (String)keys.nextElement();
                    String curValue = (String) linkchecktable.get(curKey);
                    if(curKey.equals(C_LINKCHECKTABLE_DATE)){
                        linkcheckdate = curValue;
                    } else {
                        linkcheckcontent.append(curKey+", failed checks: "+curValue+"\n");
                        length++;
                    }
                }
            } catch(Exception exc) {
                linkcheckcontent.append(Utils.getStackTrace(exc));
            }
            templateDocument.setData("logfiledate", linkcheckdate);
            //Gridnine AB Aug 8, 2002
            templateDocument.setData("logfile", Encoder.escape(linkcheckcontent.toString(),
                cms.getRequestContext().getEncoding()));
            templateDocument.setData("logfilesize", length + "");
        }
        // Now load the template file and start the processing
        return startProcessing(cms, templateDocument, elementName, parameters, templateSelector);
    }
}