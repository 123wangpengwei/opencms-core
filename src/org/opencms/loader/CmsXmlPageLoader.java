/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/loader/CmsXmlPageLoader.java,v $
 * Date   : $Date: 2004/03/22 16:40:40 $
 * Version: $Revision: 1.23 $
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

package org.opencms.loader;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.page.CmsXmlPage;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.ExtendedProperties;

/**
 * OpenCms loader class for xml pages.<p>
 *
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.23 $
 * @since 5.3
 */
public class CmsXmlPageLoader implements I_CmsResourceLoader {   
    
    /** The id of this loader */
    public static final int C_RESOURCE_LOADER_ID = 9;

    /**
     * @see org.opencms.configuration.I_CmsConfigurationParameterHandler#addConfigurationParameter(java.lang.String, java.lang.String)
     */
    public void addConfigurationParameter(String paramName, String paramValue) {
        // this resource loader requires no parameters     
    }

    /**
     * @see org.opencms.loader.I_CmsResourceLoader#destroy()
     */
    public void destroy() {
        // NOOP
    }

    /**
     * @see org.opencms.loader.I_CmsResourceLoader#dump(org.opencms.file.CmsObject, org.opencms.file.CmsResource, java.lang.String, java.util.Locale, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public byte[] dump(CmsObject cms, CmsResource resource, String element, Locale locale, HttpServletRequest req, HttpServletResponse res)
    throws CmsException {
        
        // get the absolute path of the resource
        String absolutePath = cms.readAbsolutePath(resource);
        
        // get the requested page
        CmsXmlPage page = (CmsXmlPage)req.getAttribute(absolutePath);
            
        if (page == null) {
            // make sure a page is only read once (not every time for each element)
            page = CmsXmlPage.read(cms, CmsFile.upgrade(resource, cms));
            req.setAttribute(absolutePath, page);
        }    

        // get the appropriate content and convert it to bytes
        return page.getContent(cms, element, locale).getBytes();
    }

    /**
     * @see org.opencms.loader.I_CmsResourceLoader#export(org.opencms.file.CmsObject, org.opencms.file.CmsResource, java.io.OutputStream, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void export(CmsObject cms, CmsResource resource, OutputStream exportStream, HttpServletRequest req, HttpServletResponse res) 
    throws ServletException, IOException, CmsException {        

        CmsTemplateLoaderFacade loaderFacade = OpenCms.getLoaderManager().getTemplateLoaderFacade(cms, resource);        
        loaderFacade.getLoader().export(cms, loaderFacade.getLoaderStartResource(), exportStream, req, res);
    }    

    /**
     * Will always return <code>null</code> since this loader does not 
     * need to be configured.<p>
     * 
     * @see org.opencms.configuration.I_CmsConfigurationParameterHandler#getConfiguration()
     */
    public ExtendedProperties getConfiguration() {
        return null;
    }
               
    /**
     * @see org.opencms.loader.I_CmsResourceLoader#getLoaderId()
     */
    public int getLoaderId() {
        return C_RESOURCE_LOADER_ID;
    }

    /**
     * Returns a String describing the ResourceLoader,
     * which is <code>"The OpenCms default resource loader for xml pages"</code><p>
     * 
     * @return a describing String for the ResourceLoader 
     */
    public String getResourceLoaderInfo() {
        return "The OpenCms default resource loader for xml pages";
    }

    /**
     * @see org.opencms.loader.I_CmsResourceLoader#initialize()
     */
    public void initialize() {
        if (OpenCms.getLog(CmsLog.CHANNEL_INIT).isInfoEnabled()) { 
            OpenCms.getLog(CmsLog.CHANNEL_INIT).info(". Loader init          : " + this.getClass().getName() + " initialized");
        }  
    }
    
    /**
     * @see org.opencms.loader.I_CmsResourceLoader#isStaticExportEnabled()
     */
    public boolean isStaticExportEnabled() {
        return true;
    }

    /**
     * @see org.opencms.loader.I_CmsResourceLoader#isUsableForTemplates()
     */
    public boolean isUsableForTemplates() {
        return false;
    }

    /**
     * @see org.opencms.loader.I_CmsResourceLoader#isUsingUriWhenLoadingTemplate()
     */
    public boolean isUsingUriWhenLoadingTemplate() {
        return false;
    }
    
    /**
     * @see org.opencms.loader.I_CmsResourceLoader#load(org.opencms.file.CmsObject, org.opencms.file.CmsResource, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void load(CmsObject cms, CmsResource resource, HttpServletRequest req, HttpServletResponse res) 
    throws ServletException, IOException, CmsException {        
              
        CmsTemplateLoaderFacade loaderFacade = OpenCms.getLoaderManager().getTemplateLoaderFacade(cms, resource);
        loaderFacade.getLoader().load(cms, loaderFacade.getLoaderStartResource(), req, res);
    }

    /**
     * @see org.opencms.loader.I_CmsResourceLoader#service(org.opencms.file.CmsObject, org.opencms.file.CmsResource, javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    public void service(CmsObject cms, CmsResource resource, ServletRequest req, ServletResponse res) 
    throws IOException, CmsException {
        
        // get the absolute path of the resource
        String absolutePath = cms.readAbsolutePath(resource);
        
        // get the requested page
        CmsXmlPage page = (CmsXmlPage)req.getAttribute(absolutePath);
            
        if (page == null) {
            // make sure a page is only read once (not every time for each element)
            page = CmsXmlPage.read(cms, CmsFile.upgrade(resource, cms));
            req.setAttribute(absolutePath, page);
        }        
        
        // get the element selector
        String elementName = req.getParameter(C_TEMPLATE_ELEMENT);
        
        // check the current locales
        Locale locale = OpenCms.getLocaleManager().getBestMatchingLocale(cms.getRequestContext().getLocale(), OpenCms.getLocaleManager().getDefaultLocales(cms, absolutePath), page.getLocales());
        
        // get the appropriate content and convert it to bytes
        byte[] result = page.getContent(cms, elementName, locale).getBytes(); 
        
        // append the result to the output stream
        if (result != null) {
            res.getOutputStream().write(result);
        }        
    }    
}
