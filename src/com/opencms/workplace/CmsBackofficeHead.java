/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsBackofficeHead.java,v $
* Date   : $Date: 2001/10/31 17:09:34 $
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

package com.opencms.workplace;

import com.opencms.file.*;
import com.opencms.core.*;
import com.opencms.util.*;
import com.opencms.template.*;

import java.util.*;

/**
 * Template class for displaying the head frame of the generic backoffice input forms
 *
 * Creation date: (17.10.2001)
 * @author: Michael Emmerich
 */

public class CmsBackofficeHead extends CmsWorkplaceDefault implements I_CmsConstants {

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
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() && C_DEBUG) {
            A_OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "getting content of element " + ((elementName == null) ? "<root>" : elementName));
            A_OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "template file is: " + templateFile);
            A_OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "selected template section is: " + ((templateSelector == null) ? "<default>" : templateSelector));
        }
        CmsXmlTemplateFile xmlTemplateDocument = getOwnTemplateFile(cms, templateFile, elementName, parameters, templateSelector);
        CmsSession session = (CmsSession)cms.getRequestContext().getSession(true);
        Vector selector = (Vector)session.getValue("backofficeselectortransfer");
        if(selector != null && selector.size() <2){
            //set the proccessTag = ""
            xmlTemplateDocument.setData("BOSELECTOR","");
            //remove the unused values from the session
            session.removeValue("backofficeselectortransfer");
            session.removeValue("backofficeselectedtransfer");
        }

        //care about the previewbutton
        String prevLink = (String)session.getValue("weShallDisplayThePreviewButton");
        if(prevLink != null && !prevLink.equals("") ){
            //we got a link so we display it
            xmlTemplateDocument.setData("link", prevLink);
            xmlTemplateDocument.setData("prevbut", xmlTemplateDocument.getProcessedDataValue("preview", this, parameters));
            session.removeValue("weShallDisplayThePreviewButton");
        }else{
            xmlTemplateDocument.setData("prevbut","");// xmlTemplateDocument.getDataValue("nopreview"));
        }


        return startProcessing(cms, xmlTemplateDocument, elementName, parameters, templateSelector);
    }


 /**
 * This method creates the selectbox with all avaiable Pages to select from.
 */
  public Integer getSelectedPage(CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values,
                           Hashtable parameters) throws CmsException {
    // get the session
    CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
    // get all aviable template selectors
    Vector selector=(Vector)session.getValue("backofficeselectortransfer");

    // get the actual template selector
    Integer retValue =(Integer)session.getValue("backofficeselectedtransfer");
    // copy the data into the value and name vectors
    for (int i = 0; i < selector.size(); i++) {
      String sel = (String) selector.elementAt(i);
      names.addElement(sel);
      values.addElement(sel);
    }

    session.removeValue("backofficeselectortransfer");
    session.removeValue("backofficeselectedtransfer");
    return retValue;
  }

}