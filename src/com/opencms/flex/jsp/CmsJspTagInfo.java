/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/flex/jsp/Attic/CmsJspTagInfo.java,v $
 * Date   : $Date: 2003/07/12 11:29:22 $
 * Version: $Revision: 1.12 $
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
 
package com.opencms.flex.jsp;

import com.opencms.boot.CmsBase;
import com.opencms.core.A_OpenCms;
import com.opencms.flex.cache.CmsFlexController;

import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Provides access to OpenCms and System related information.<p>
 * 
 * This tag supports the following special "property" values:
 * <ul>
 * <li><code>opencms.version</code> returns the current OpenCms version, e.g. <i>5.0 Kaitain</i>
 * <li><code>opencms.url</code> returns the current request URL, e.g. 
 * <i>http://localhost:8080/opencms/opencms/index.jsp</i>
 * <li><code>opencms.uri</code> returns the current request URI, e.g. 
 * <i>/opencms/opencms/index.jsp</i>
 * <li><code>opencms.webapp</code> returns the name of the OpenCms web application, e.g. 
 * <i>opencms</i>
 * <li><code>opencms.webbasepath</code> returns the name of system path to the OpenCms web 
 * application, e.g. <i>C:\Java\Tomcat\webapps\opencms\</i> 
 * <li><code>opencms.request.uri</code> returns the name of the currently requested URI in 
 * the OpenCms VFS, e.g. <i>/index.jsp</i>
 * <li><code>opencms.request.element.uri</code> returns the name of the currently processed element, 
 * which might be a sub-element like a template part, 
 * in the OpenCms VFS, e.g. <i>/system/modules/org.opencms.welcome/jsptemplates/welcome.jsp</i>
 * <li><code>opencms.request.folder</code> returns the name of the parent folder of the currently
 * requested URI in the OpenCms VFS, e.g. <i>/</i>
 * <li><code>opencms.request.encoding</code> returns the content encoding that has been set
 * for the currently requested resource, e.g. <i>ISO-8859-1</i>
 * </ul>
 * 
 * All other property values that are passes to the tag as routed to a standard 
 * <code>System.getProperty(value)</code> call,
 * so you can also get information about the Java VM environment,
 * using values like <code>java.vm.version</code> or <code>os.name</code>.<p>
 * 
 * If the given property value does not match a key from the special OpenCms values
 * and also not the system values, a (String) message is returned with a formatted 
 * error message.<p>
 *  
 * @author  Alexander Kandzior (a.kandzior@alkacon.com)
 * @version $Revision: 1.12 $
 */
public class CmsJspTagInfo extends TagSupport {
    
    // member variables    
    private String m_property = null;

    /** Static array with allowed info property values */
    private static final String[] m_systemProperties =
        {
            "opencms.version", // 0
            "opencms.url", // 1
            "opencms.uri", // 2
            "opencms.webapp", // 3
            "opencms.webbasepath", // 4 
            "opencms.request.uri", // 5
            "opencms.request.element.uri", // 6
            "opencms.request.folder", // 7      
            "opencms.request.encoding" // 8      
        };                
            
    /** array list of allowed property values for more convenient lookup */
    private static final List m_userProperty =
        Arrays.asList(m_systemProperties);

    /**
     * Sets the info property name.
     * 
     * @param name the info property name to set
     */
    public void setProperty(String name) {
        if (name != null) {
            m_property = name.toLowerCase();
        }
    }

    /**
     * Returns the selected info property.
     * 
     * @return the selected info property 
     */
    public String getProperty() {
        return m_property != null ? m_property : "";
    }

    /**
     * @see javax.servlet.jsp.tagext.Tag#release()
     */
    public void release() {
        super.release();
        m_property = null;
    }

    /**
     * @see javax.servlet.jsp.tagext.Tag#doStartTag()
     */
    public int doStartTag() throws JspException {

        ServletRequest req = pageContext.getRequest();

        // This will always be true if the page is called through OpenCms 
        if (CmsFlexController.isCmsRequest(req)) {

            try {
                String result = infoTagAction(m_property, (HttpServletRequest)req);
                // Return value of selected property
                pageContext.getOut().print(result);
            } catch (Exception ex) {
                System.err.println("Error in Jsp 'info' tag processing: " + ex);
                System.err.println(com.opencms.util.Utils.getStackTrace(ex));
                throw new JspException(ex);
            }
        }
        return SKIP_BODY;
    }

    /**
     * Returns the selected info property value based on the provided 
     * parameters.<p>
     * 
     * @param property the info property to look up
     * @param req the currents request
     * @return the looked up property value 
     */    
    public static String infoTagAction(String property, HttpServletRequest req) {
        if (property == null)
            return "+++ Invalid info property selected: null +++";

        CmsFlexController controller = (CmsFlexController)req.getAttribute(CmsFlexController.ATTRIBUTE_NAME);

        String result = null;
        switch (m_userProperty.indexOf(property)) {
            case 0 : // opencms.version
                result = A_OpenCms.getVersionName();
                break;
            case 1 : // opencms.url
                result = req.getRequestURL().toString();
                break;
            case 2 : // opencms.uri
                result = req.getRequestURI();
                break;
            case 3 : // opencms.webapp
                result = CmsBase.getWebAppName();
                break;
            case 4 : // opencms.webbasepath
                result = CmsBase.getWebBasePath();
                break;
            case 5 : // opencms.request.uri
                result = controller.getCmsObject().getRequestContext().getUri();
                break;
            case 6 : // opencms.request.element.uri
                result = controller.getCurrentRequest().getElementUri();
                break;
            case 7 : // opencms.request.folder
                result = com.opencms.file.CmsResource.getParent(controller.getCmsObject().getRequestContext().getUri());
                break;
            case 8 : // opencms.request.encoding
                result = controller.getCmsObject().getRequestContext().getEncoding();
                break;
            default :
                result = System.getProperty(property);
                if (result == null) {
                    result = "+++ Invalid info property selected: " + property + " +++";
                }
        }

        return result;
    }

}
