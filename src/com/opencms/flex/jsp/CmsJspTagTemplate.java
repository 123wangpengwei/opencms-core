/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/flex/jsp/Attic/CmsJspTagTemplate.java,v $
* Date   : $Date: 2003/02/17 01:12:45 $
* Version: $Revision: 1.5 $
*
* This library is part of OpenCms -
* the Open Source Content Mananagement System
*
* Copyright (C) 2002  The OpenCms Group
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


package com.opencms.flex.jsp;

import com.opencms.flex.cache.CmsFlexRequest;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * Used to select various template elements form a JSP template that
 * is included in another file.<p>
 * 
 * @author  Alexander Kandzior (a.kandzior@alkacon.com)
 * @version $Revision: 1.5 $
 */
public class CmsJspTagTemplate extends BodyTagSupport { 
    
    // Attribute member variables
    private String m_element = null;

    /** Template part identifier */
    public static final String C_TEMPLATE_ELEMENT = "__element";
    
    /**
     * Sets the include page/file target.
     * @param target the target to set
     */
    public void setElement(String element) {
        if (element != null) {
            m_element = element.toLowerCase();
        }
    }
    
    /**
     * Returns the include page/file target.
     * @return String
     */
    public String getElement() {
        return m_element!=null?m_element:"";
    }

    /**
     * @see javax.servlet.jsp.tagext.Tag#release()
     */ 
    public void release() {
        super.release();
        m_element = null;
    }    

    /**
     * @see javax.servlet.jsp.tagext.Tag#doStartTag()
     */
    public int doStartTag() throws JspException {
        if (templateTagAction(m_element, (CmsFlexRequest)pageContext.getRequest())) {
            return EVAL_BODY_INCLUDE;
        } else {
            return SKIP_BODY;
        }
    }

    /**
     * Internal action method.<p>
     * 
     * @param element the selected element
     * @param req the current request 
     * @return boolean <code>true</code> if this element should be inclued, <code>false</code>
     * otherwise
     */    
    public static boolean templateTagAction(String element, CmsFlexRequest req) {
        String param =  req.getParameter(C_TEMPLATE_ELEMENT);        
        return ((param == null) || (param.equals(element)));
    }
 }