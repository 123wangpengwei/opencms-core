/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsAdminProjectResent.java,v $
* Date   : $Date: 2003/01/20 17:57:47 $
* Version: $Revision: 1.16 $
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

import com.opencms.boot.I_CmsLogChannels;
import com.opencms.core.A_OpenCms;
import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.core.I_CmsSession;
import com.opencms.file.CmsObject;
import com.opencms.file.CmsProject;
import com.opencms.template.CmsXmlTemplateFile;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Template class for displaying OpenCms workplace admin project resent.
 * <P>
 * 
 * @author Andreas Schouten
 * @version $Revision: 1.16 $ $Date: 2003/01/20 17:57:47 $
 * @see com.opencms.workplace.CmsXmlWpTemplateFile
 */

public class CmsAdminProjectResent extends CmsWorkplaceDefault implements I_CmsConstants {
    
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
    
    public byte[] getContent(CmsObject cms, String templateFile, String elementName, 
            Hashtable parameters, String templateSelector) throws CmsException {
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() && C_DEBUG ) {
            A_OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "getting content of element " 
                    + ((elementName == null) ? "<root>" : elementName));
            A_OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "template file is: " 
                    + templateFile);
            A_OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "selected template section is: " 
                    + ((templateSelector == null) ? "<default>" : templateSelector));
        }
        CmsXmlTemplateFile xmlTemplateDocument = getOwnTemplateFile(cms, templateFile, 
                elementName, parameters, templateSelector);
        xmlTemplateDocument.setData("proId", ""+ cms.getRequestContext().currentProject().getId());
        // delete the oldProjectId
        I_CmsSession session = cms.getRequestContext().getSession(true);
        session.removeValue("oldProjectId");
        
        // Now load the template file and start the processing
        return startProcessing(cms, xmlTemplateDocument, elementName, parameters, 
                templateSelector);
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
    
    public boolean isCacheable(CmsObject cms, String templateFile, String elementName, 
            Hashtable parameters, String templateSelector) {
        return false;
    }
    
    /**
     * Gets the projects.
     * <P>
     * 
     * @param cms CmsObject Object for accessing system resources.
     * @param names Vector to be filled with the appropriate values in this method.
     * @param values Vector to be filled with the appropriate values in this method.
     * @return Vector representing the current projects.
     * @exception CmsException
     */
    
    //  public Vector projectList(CmsObject cms, CmsXmlLanguageFile lang)    
    //  throws CmsException {    
    //     get the manageable projects    
    //      return cms.getAllManageableProjects();    
    //  }
    
    public Vector projectList(CmsObject cms, CmsXmlLanguageFile lang) throws CmsException {
        Vector list = new Vector();
        
        // get the manageable projects
        Vector mp = cms.getAllManageableProjects();
        Hashtable temp = new Hashtable();
        for(int i = 0;i < mp.size();i++) {
            temp.put("" + ((CmsProject)mp.elementAt(i)).getId(), mp.elementAt(i));
        }
        
        //
        Vector ap = cms.getAllAccessibleProjects();
        for(int i = 0;i < ap.size();i++) {
            if(temp.containsKey("" + ((CmsProject)ap.elementAt(i)).getId())) {
                list.addElement(ap.elementAt(i));
            }
        }
        
        //
        return list;
    }
}
