/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/flex/jsp/Attic/CmsJspActionElement.java,v $
 * Date   : $Date: 2003/03/18 01:48:51 $
 * Version: $Revision: 1.13 $
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

import com.opencms.file.CmsObject;
import com.opencms.file.CmsRequestContext;
import com.opencms.flex.cache.CmsFlexRequest;
import com.opencms.flex.cache.CmsFlexResponse;
import com.opencms.flex.util.CmsMessages;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

/**
 * Bean to be used in JSP scriptlet code that provides direct 
 * access to the functionality offered by the opencms taglib.<p>
 * 
 * By instanciating a bean of this type and accessing the methods provided by 
 * the instance, all functionality of the OpenCms JSP taglib can be easily 
 * used from within JSP scriplet code,<p>
 * 
 * Initialize this bean at the beginning of your JSP like this:
 * <pre>
 * &lt;jsp:useBean id="cms" class="com.opencms.flex.jsp.CmsJspActionElement"&gt;
 * &lt% cms.init(pageContext, request, response); %&gt;
 * &lt;/jsp:useBean&gt;
 * </pre>
 * 
 * All exceptions that occur when calling any method of this class are catched 
 * and written to the log output only, so that a template still has a chance of
 * working at last in some elements.<p>
 *
 * @author  Alexander Kandzior (a.kandzior@alkacon.com)
 * @version $Revision: 1.13 $
 * 
 * @since 5.0 beta 2
 */
public class CmsJspActionElement {

    /** OpenCms JSP request */
    private CmsFlexRequest m_request;

    /** OpenCms JSP response */
    private CmsFlexResponse m_response;

    /** JSP page context */
    private PageContext m_context;
    
    /** JSP navigation builder */
    private CmsJspNavBuilder m_navigation = null;    
    
    /** Flag to indicate that this bean was properly initialized */
    private boolean m_notInitialized;
    
    /** Error message in case bean was not properly initialized */
    public final static String C_NOT_INITIALIZED = "+++ CmsJspActionElement not initialized +++";

    /**
     * Empty constructor, required for every JavaBean.
     */
    public CmsJspActionElement() {
        m_notInitialized = true;
    }
    
    /**
     * Constructor, with parameters.
     * 
     * @param content the JSP page context object
     * @param req the JSP request 
     * @param res the JSP response 
     */
    public CmsJspActionElement(PageContext context, HttpServletRequest req, HttpServletResponse res) {
        m_notInitialized = true;
        init(context, req, res);
    }    
    
    /**
     * Initialize the bean with the current page context, request and response.<p>
     * 
     * It is required to call one of the init() methods before you can use the 
     * instance of this bean.
     * 
     * @param content the JSP page context object
     * @param req the JSP request casted to a CmsFlexRequest
     * @param res the JSP response casted to a CmsFlexResponse
     */
    public void init(PageContext context, CmsFlexRequest req, CmsFlexResponse res) {
        m_context = context;
        m_request = req;
        m_response = res;
        m_notInitialized = false;
    }
    
    /**
     * Initialize the bean with the current page context, request and response.<p>
     * 
     * It is required to call one of the init() methods before you can use the 
     * instance of this bean.
     * 
     * @param content the JSP page context object
     * @param req the JSP request 
     * @param res the JSP response 
     */
    public void init(PageContext context, HttpServletRequest req, HttpServletResponse res) {
        try {
            this.init(context, (CmsFlexRequest)req, (CmsFlexResponse)res);
        } catch (ClassCastException e) {
            // probably request / response where not of the right type
            m_notInitialized = true;
        }
    }    

    /**
     * Returns the CmsObject from the wrapped request.<p>
     *
     * This is a convenience method in case you need access to
     * the CmsObject in your JSP scriplets.
     *
     * @return the CmsObject from the wrapped request
     *
     * @see com.opencms.flex.cache.CmsFlexRequest#getCmsObject()
     */
    public CmsObject getCmsObject() {
        if (m_notInitialized) return null;
        try { 
            return m_request.getCmsObject();
        } catch (Throwable t) {
            handleException(t);
        }      
        return null;            
    }
    
    /**
     * Include a sub-element without paramters from the OpenCms VFS, same as
     * using the <code>&lt;cms:include file="..." /&gt;</code> tag.
     * 
     * @param target the target uri of the file in the OpenCms VFS (can be relative or absolute)
     * @throws JspException in case there were problems including the target
     *
     * @see com.opencms.flex.jsp.CmsJspTagInclude
     */
    public void include(String target) throws JspException {
        this.include(target, null, null);
    }
    
    /**
     * Include a named sub-element without paramters from the OpenCms VFS, same as
     * using the <code>&lt;cms:include file="..." element="..." /&gt;</code> tag.
     * 
     * @param target the target uri of the file in the OpenCms VFS (can be relative or absolute)
     * @param element the element (template selector) to display from the target
     * @throws JspException in case there were problems including the target
     *
     * @see com.opencms.flex.jsp.CmsJspTagInclude
     */    
    public void include(String target, String element) throws JspException  {
        this.include(target, element, null);
    }

    /**
     * Include a named sub-element with paramters from the OpenCms VFS, same as
     * using the <code>&lt;cms:include file="..." element="..." /&gt;</code> tag
     * with parameters in the tag body.<p>
     * 
     * The parameter map should be a map where the keys are Strings 
     * (the parameter names) and the values are of type String[].
     * However, as a convenience feature,
     * in case you provide just a String for the parameter value, 
     * it will automatically be translated to a String[1].<p>
     * 
     * The handling of the <code>element</code> parameter depends on the 
     * included file type. Most often it is used as template selector.<p>
     * 
     * @param target the target uri of the file in the OpenCms VFS (can be relative or absolute)
     * @param element the element (template selector) to display from the target
     * @param parameterMap a map of the request parameters
     * @throws JspException in case there were problems including the target
     * 
     * @see com.opencms.flex.jsp.CmsJspTagInclude
     */
    public void include(String target, String element, Map parameterMap) throws JspException {
        if (m_notInitialized) return;
        if (parameterMap != null) {
            try {
                // ensure parameters are always of type String[] not just String
                Iterator i = parameterMap.keySet().iterator();
                while (i.hasNext()) {
                    String key = (String)i.next();
                    Object value = parameterMap.get(key);
                    if (value instanceof String) {
                        String[] newValue = new String[] {(String)value };
                        parameterMap.put(key, newValue);
                    }
                }
            } catch (UnsupportedOperationException e) {
                // parameter map is immutable, just use it "as is"
            }
        }
        CmsJspTagInclude.includeTagAction(m_context, target, element, parameterMap, m_request, m_response);
    }
            
    /**
     * Calculate a link with the OpenCms link management,
     * same as using the <code>&lt;cms:link&gt;...&lt;/cms:link&gt;</code> tag.<p>
     * 
     * This is important to get the right link for exported resources, 
     * e.g. for images in the online project.
     * 
     * @param link the uri in the OpenCms to link to
     * @return the translated link
     * 
     * @see com.opencms.flex.jsp.CmsJspTagLink
     */
    public String link(String link) {
        if (m_notInitialized) return C_NOT_INITIALIZED;
        try {        
            return CmsJspTagLink.linkTagAction(link, m_request);
        } catch (Throwable t) {
            handleException(t);
        }  
        return "+++ error generating link to '" + link + "' +++";             
    }
    
    /**
     * Returns a selected user property, i.e. information about the currently
     * logged in user, same as using 
     * the <code>&lt;cms:user property="..." /&gt;</code> tag.<p>
     * 
     * @param property the user property to display, please see the tag documentation for valid options
     * @return the value of the selected user property
     * 
     * @see com.opencms.flex.jsp.CmsJspTagUser
     */
    public String user(String property) {
        if (m_notInitialized) return C_NOT_INITIALIZED;
        try {
            return CmsJspTagUser.userTagAction(property, m_request);
        } catch (Throwable t) {
            handleException(t);
        }  
        return "+++ error reading user property '" + property + "' +++";            
    }
    
    /**
     * Returns a selected file property value, same as using 
     * the <code>&lt;cms:property name="..." /&gt;</code> tag or
     * calling {@link #property(String name, String null, String null, boolean false)}.<p>
     * 
     * @param name the name of the property to look for
     * @return the value of the property found, or null if the property could not be found
     * 
     * @see #property(String, String, String, boolean)
     * @see com.opencms.flex.jsp.CmsJspTagProperty
     */
    public String property(String name) {
        return this.property(name, null, null, false);       
    }
        
    /**
     * Returns a selected file property value, same as using 
     * the <code>&lt;cms:property name="..." file="..." /&gt;</code> tag or
     * calling {@link #property(String name, String file, String null, boolean false)}.<p>
     * 
     * @param name the name of the property to look for
     * @param file the file (or folder) to look at for the property
     * @return the value of the property found, or null if the property could not be found
     * 
     * @see #property(String, String, String, boolean)
     * @see com.opencms.flex.jsp.CmsJspTagProperty
     */
    public String property(String name, String file) {
        return this.property(name, file, null, false);       
    }

    /**
     * Returns a selected file property value, same as using
     * the <code>&lt;cms:property name="..." file="..." default="..." /&gt;</code> tag or
     * calling {@link #property(String name, String file, String defaultValue, boolean false)}.<p>
     *
     * @param name the name of the property to look for
     * @param file the file (or folder) to look at for the property
     * @param defaultValue a default value in case the property was not found
     * @return the value of the property found, or the value of defaultValue
     *     if the property could not be found
     *
     * @see #property(String, String, String, boolean)
     * @see com.opencms.flex.jsp.CmsJspTagProperty
     */
    public String property(String name, String file, String defaultValue) {
        return this.property(name, file, defaultValue, false);
    }
            
    /**
     * Returns a selected file property value with optional HTML escaping, same as using 
     * the <code>&lt;cms:property name="..." file="..." default="..." /&gt;</code> tag.<p>
     * 
     * Please see the description of the class {@link com.opencms.flex.jsp.CmsJspTagProperty} for
     * valid options of the <code>file</code> parameter.<p>
     * 
     * @param name the name of the property to look for
     * @param file the file (or folder) to look at for the property
     * @param defaultValue a default value in case the property was not found
     * @param escapeHtml if <code>true</code>, special HTML characters in the return value
     *     are escaped with their number representations (e.g. &amp; becomes &amp;#38;)
     * @return the value of the property found, or the value of defaultValue 
     *     if the property could not be found
     *
     * @see com.opencms.flex.jsp.CmsJspTagProperty
     */
    public String property(String name, String file, String defaultValue, boolean escapeHtml) {
        if (m_notInitialized) return C_NOT_INITIALIZED;
        try {
            if (file == null) file = m_request.getCmsObject().getRequestContext().getUri();
            return CmsJspTagProperty.propertyTagAction(name, file, defaultValue, escapeHtml, m_request);
        } catch (Throwable t) {
            handleException(t);
        }   
        if (defaultValue == null) {
            return "+++ error reading file property '" + name + "' on '" + file + "' +++";
        } else {
            return defaultValue;
        }
    }
    
    /**
     * Returns an OpenCms or JVM system info property value, same as using
     * the <code>&lt;cms:info property="..." /&gt;</code> tag.<p>
     * 
     * See the description of the class {@link CmsJspTagInfo} for a detailed list 
     * of available options for the property value.<p>
     *  
     * @param property the property to look up
     * @return String the value of the system property
     * @see com.opencms.flex.jsp.CmsJspTagInfo
     */
    public String info(String property) {
        try {        
            return CmsJspTagInfo.infoTagAction(property, m_request);   
        } catch (Throwable t) {
            handleException(t);
        }  
        return "+++ error reading info property '" + property + "' +++";
    }
    
    /**
     * Returns an OpenCms workplace label.<p>
     * 
     * You should consider using a standard 
     * {@link java.util.ResourceBundle java.util.ResourceBundle} instead of the 
     * OpenCms workplace language files.
     * 
     * @param label the label to look up
     * @return label the value of the label
     * 
     * @see com.opencms.flex.jsp.CmsJspTagLabel
     */
    public String label(String label) {
        if (m_notInitialized) return C_NOT_INITIALIZED;
        try {
            return CmsJspTagLabel.wpLabelTagAction(label, m_request);
        } catch (Throwable t) {
            handleException(t);
        }  
        return "+++ error reading workplace label '" + label + "' +++";
    }    
    
    /**
     * Checks if a template part should be used or not, same as using 
     * the <code>&lt;cms:template element="..." /&gt;</code> tag.<p>
     * 
     * @param part the template element to check 
     * @return <code>true</code> if the element is active, <code>false</code> otherwise
     * 
     * @see com.opencms.flex.jsp.CmsJspTagUser
     */
    public boolean template(String element) {
        if (m_notInitialized) return true;        
        try {
            return CmsJspTagTemplate.templateTagAction(element, m_request);
        } catch (Throwable t) {
            handleException(t);
        }
        return true;
    }
    
    /** 
     * Returns the current request context from the internal CmsObject.
     * 
     * @return the current request context from the internal CmsObject
     */
    public CmsRequestContext getRequestContext() {
        if (m_notInitialized) return null;
        try {
            return m_request.getCmsObject().getRequestContext();
        } catch (Throwable t) {
            handleException(t);
        }
        return null;
    }
    
    /**
     * Returns an initialized {@link CmsJspNavBuilder} instance.<p>
     *  
     * @return CmsJspNavBuilder an initialized <code>CmsJspNavBuilder</code>
     * 
     * @see com.opencms.flex.jsp.CmsJspNavBuilder
     */
    public CmsJspNavBuilder getNavigation() {
        if (m_notInitialized) return null;
        try {
            if (m_navigation == null) {
                m_navigation = new CmsJspNavBuilder(m_request.getCmsObject());
            }
            return m_navigation;
        } catch (Throwable t) {
            handleException(t);
        }
        return null;            
    }
    
    /**
     * Generates an initialized instance of {@link com.opencms.flex.util.CmsMessages} for 
     * convenient access to localized resource bundles.<p>
     * 
     * @param bundleName the name of the ResourceBundle to use
     * @param language language indentificator for the locale of the bundle
     * @return CmsMessages a message bundle initialized with the provided values
     */       
    public CmsMessages getMessages(String bundleName, String language) {
        return getMessages(bundleName, language, "", "", null);
    }
    
    /**
     * Generates an initialized instance of {@link com.opencms.flex.util.CmsMessages} for 
     * convenient access to localized resource bundles.<p>
     * 
     * @param bundleName the name of the ResourceBundle to use
     * @param language language indentificator for the locale of the bundle
     * @param defaultLanguage default for the language, will be used 
     *         if language is null or empty String "", and defaultLanguage is not null
     * @return CmsMessages a message bundle initialized with the provided values
     */    
    public CmsMessages getMessages(String bundleName, String language, String defaultLanguage) {
        return getMessages(bundleName, language, "", "", defaultLanguage);
    }
    
    /**
     * Generates an initialized instance of {@link com.opencms.flex.util.CmsMessages} for 
     * convenient access to localized resource bundles.<p>
     * 
     * @param bundleName the name of the ResourceBundle to use
     * @param language language indentificator for the locale of the bundle
     * @param country 2 letter country code for the locale of the bundle 
     * @param variant a vendor or browser-specific variant code
     * @param defaultLanguage default for the language, will be used 
     *         if language is null or empty String "", and defaultLanguage is not null
     * @return CmsMessages a message bundle initialized with the provided values
     * 
     * @see java.util.ResourceBundle
     * @see com.opencms.flex.util.CmsMessages
     */
    public CmsMessages getMessages(String bundleName, String language, String country, String variant, String defaultLanguage) {
        try {
            if ((defaultLanguage != null) && ((language == null) || ("".equals(language)))) {
                language = defaultLanguage;
            }
            if (language == null) language = "";
            if (country == null) country = "";
            if (variant == null) variant = "";
            return new CmsMessages(bundleName, language, country, variant);
        } catch (Throwable t) {
            handleException(t);
        }
        return null;
    }    
    
    /**
     * Handles any exception that might occur in the context of this element to 
     * ensure that templates are not disturbed.<p>
     * 
     * @param e the exception that was catched
     */
    private void handleException(Throwable t) {
        System.err.println("Exception in " + this.getClass().getName() + ":");
        t.printStackTrace(System.err);
    }
}
