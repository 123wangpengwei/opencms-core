/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsUnlock.java,v $
* Date   : $Date: 2002/07/10 15:01:55 $
* Version: $Revision: 1.38 $
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

import com.opencms.file.*;
import com.opencms.core.*;
import com.opencms.util.*;
import com.opencms.template.*;
import javax.servlet.http.*;
import java.util.*;

/**
 * Template class for displaying the unlock screen of the OpenCms workplace.<P>
 * Reads template files of the content type <code>CmsXmlWpTemplateFile</code>.
 * <P>
 *
 * HACK: class uses news example package for handling special news locking features.
 * @author Michael Emmerich
 * @author Michaela Schleich
 * @author Alexander Lucas
 * @version $Revision: 1.38 $ $Date: 2002/07/10 15:01:55 $
 */

public class CmsUnlock extends CmsWorkplaceDefault implements I_CmsWpConstants,I_CmsConstants {

    /**
     * Overwrties the getContent method of the CmsWorkplaceDefault.<br>
     * Gets the content of the unlock templated and processed the data input.
     * @param cms The CmsObject.
     * @param templateFile The unlock template file
     * @param elementName not used
     * @param parameters Parameters of the request and the template.
     * @param templateSelector Selector of the template tag to be displayed.
     * @return Bytearre containgine the processed data of the template.
     * @exception Throws CmsException if something goes wrong.
     */

    public byte[] getContent(CmsObject cms, String templateFile, String elementName,
            Hashtable parameters, String templateSelector) throws CmsException {
        I_CmsSession session = cms.getRequestContext().getSession(true);

        // the template to be displayed
        String template = null;

        // clear session values on first load
        String initial = (String)parameters.get(C_PARA_INITIAL);
        if(initial != null) {

            // remove all session values
            session.removeValue(C_PARA_FILE);
            session.removeValue("lasturl");
        }

        // get the lasturl parameter
        String lasturl = getLastUrl(cms, parameters);
        String unlock = (String)parameters.get(C_PARA_UNLOCK);
        String filename = (String)parameters.get(C_PARA_FILE);
        if(filename != null) {
            session.putValue(C_PARA_FILE, filename);
        }

        //check if the user wants the lock dialog
        // if yes, the lock page is shown for the first time
        filename = (String)session.getValue(C_PARA_FILE);
        CmsResource file = (CmsResource)cms.readFileHeader(filename);

        // select the template to be displayed
        if(file.isFile()) {
            template = "file";
        }
        else {
            template = "folder";
        }
        Hashtable startSettings = (Hashtable)cms.getRequestContext().currentUser().getAdditionalInfo(C_ADDITIONAL_INFO_STARTSETTINGS);
        String showLockDialog = "off";
        if(startSettings!=null){
            showLockDialog = (String)startSettings.get(C_START_LOCKDIALOG);
        }
        if(unlock == null && !"on".equals(showLockDialog)) {
            unlock = "true";
        }
        if(unlock != null) {
            if(unlock.equals("true")) {
                try {
                    cms.unlockResource(filename);
                    session.removeValue(C_PARA_FILE);
                }
                catch(CmsException e) {
                    CmsXmlWpTemplateFile xmlTemplateDocument = new CmsXmlWpTemplateFile(cms, templateFile);
                    if(e.getType() == CmsException.C_NO_ACCESS) {
                        template = "erroraccessdenied";
                        xmlTemplateDocument.setData("details", file.getName());
                    }
                    else {
                        template = "error";
                        xmlTemplateDocument.setData("details", e.toString());
                    }
                    xmlTemplateDocument.setData("lasturl", lasturl);
                    return startProcessing(cms, xmlTemplateDocument, "", parameters, template);
                }
            }

            // return to filelist
            try {
                if(lasturl == null || "".equals(lasturl)) {
                    cms.getRequestContext().getResponse().sendCmsRedirect(getConfigFile(cms).getWorkplaceActionPath()
                            + C_WP_EXPLORER_FILELIST);
                }
                else {
                    cms.getRequestContext().getResponse().sendRedirect(lasturl);
                }
            }
            catch(Exception e) {
                throw new CmsException("Redirect fails :" + getConfigFile(cms).getWorkplaceActionPath()
                        + C_WP_EXPLORER_FILELIST, CmsException.C_UNKNOWN_EXCEPTION, e);
            }
            return null;
        }
        CmsXmlWpTemplateFile xmlTemplateDocument = new CmsXmlWpTemplateFile(cms, templateFile);
        xmlTemplateDocument.setData("FILENAME", file.getName());

        // process the selected template
        return startProcessing(cms, xmlTemplateDocument, "", parameters, template);
    }


    /**
     * Indicates if the results of this class are cacheable.
     *
     * @param cms CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return <EM>true</EM> if cacheable, <EM>false</EM> otherwise.
     */

    public boolean isCacheable(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
        return false;
    }
}

