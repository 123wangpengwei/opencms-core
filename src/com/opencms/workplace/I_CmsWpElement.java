/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/I_CmsWpElement.java,v $
* Date   : $Date: 2003/01/20 23:59:18 $
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

import com.opencms.core.CmsException;
import com.opencms.file.CmsObject;
import com.opencms.template.A_CmsXmlContent;

import java.util.Hashtable;

import org.w3c.dom.Element;

/**
 * Interface for all workplace elements.
 * <P>
 * Any class called by CmsXmlTemplateFile for handling special workplace
 * XML tags (e.g. <code>&lt;BUTTON&gt;</code> or <code>&lt;LABEL&gt;</code>)
 * has to implement this interface.
 * 
 * @author Alexander Lucas
 * @version $Revision: 1.14 $ $Date: 2003/01/20 23:59:18 $
 * @see com.opencms.workplace.CmsXmlWpTemplateFile
 */

public interface I_CmsWpElement {
    
    /**
     * Method for handling the corresponding special workplace XML tag and generating the
     * appropriate output.
     * 
     * @param cms CmsObject Object for accessing resources.
     * @param n XML element containing the current tag.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document.  
     * @param callingObject reference to the calling object.
     * @param parameters Hashtable containing all user parameters.
     * @param lang CmsXmlLanguageFile conataining the currently valid language file.
     * @return Processed special workplace XML tag.
     * @throws CmsException 
     */
    
    public Object handleSpecialWorkplaceTag(CmsObject cms, Element n, A_CmsXmlContent doc, Object callingObject, 
            Hashtable parameters, CmsXmlLanguageFile lang) throws CmsException;
}
