/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/legacy/Attic/CmsJspTemplate.java,v $
 * Date   : $Date: 2004/02/21 17:11:43 $
 * Version: $Revision: 1.2 $
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
 
package com.opencms.legacy;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.loader.CmsJspLoader;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;

import com.opencms.template.CmsCacheDirectives;
import com.opencms.template.CmsDumpTemplate;

import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A simple dump class which enables
 * the use of JSP as sub-elements in the legacy OpenCms XMLTemplate 
 * mechanism.<p>
 *
 * @author  Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.2 $
 * @since 5.0 beta 1
 */
public class CmsJspTemplate extends CmsDumpTemplate {
    
    /**
     * The constructor of the class is empty and does nothing.<p>
     */
    public CmsJspTemplate() {
        // NOOP
    }

    /**
     * Gets the content of the given JSP file to include them
     * in the XMLTemplate.<p>
     *
     * @param cms for accessing system resources
     * @param jspFile filename of the JSP in the VFS
     * @param elementName <em>not used</em>
     * @param parameters <em>not used</em>
     * 
     * @return Content of the requested JSP page.
     * 
     * @throws CmsException in case something goes wrong
     */
    public byte[] getContent(CmsObject cms, String jspFile, String elementName, Hashtable parameters) throws CmsException {
        if (OpenCms.getLog(this).isDebugEnabled()) {
            OpenCms.getLog(this).debug("Loading contents of file " + jspFile);
        }

        byte[] s = null;
        try {
            CmsFile file = cms.readFile(jspFile);
            CmsJspLoader loader = (CmsJspLoader)OpenCms.getLoaderManager().getLoader(CmsJspLoader.C_RESOURCE_LOADER_ID);

            HttpServletRequest req = cms.getRequestContext().getRequest().getOriginalRequest();
            HttpServletResponse res = cms.getRequestContext().getResponse().getOriginalResponse();             

            s = loader.dump(cms, file, null, null, req, res);            
        } catch (java.lang.ClassCastException e) {
            s = null;
            throw new CmsException("[CmsJspTemplate] " + jspFile + " is not a JSP");
        } catch (org.opencms.main.CmsException e) {
            s = null;
            // File might not exist or no read permissions
            throw new CmsException("[CmsJspTemplate] Error while reading JSP " + jspFile + "\n" + e, e);
        } catch (Exception e) {
            s = null;
            String errorMessage = "[CmsJspTemplate] Error while loading jsp file " + jspFile + ": " + e;
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error(errorMessage, e);
            }
            if (e instanceof CmsException) {
                throw (CmsException)e;
            } else {
                throw new CmsException(errorMessage, CmsException.C_UNKNOWN_EXCEPTION);
            }
        }
        return s;
    }
    
    /**
     * Cache method required by the ElementCache to indicate if the
     * results of the page should be cached in the ElementCache.<p>
     * 
     * JSPs will be cached in the FlexCache and so 
     * we always return <code>false</code> here.
     * 
     * @return <code>false</code>
     */
    public boolean isTemplateCacheSet() {
        return false;
    }    
    
    /**
     * Method used by the ElementCache to check if the page 
     * should reload or not.<p>
     * 
     * JSPs will be cached in the FlexCache and so 
     * we always return <code>true</code> here.
     * 
     * @param cms default argument for element cache method
     * @param templateFile default argument for element cache method
     * @param elementName default argument for element cache method
     * @param parameters default argument for element cache method
     * @param templateSelector default argument for element cache method
     * @return <code>true</code>
     */
    public boolean shouldReload(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
        return true;
    } 
    
    /**
     * Returns the caching information from the current template class for the
     * ElementCache.<p>
     * 
     * JSPs will be cached in the FlexCache and so 
     * we always return directives that prevent caching here,
     * i.e. <code>new CmsCacheDirectives(false)</code>
     *
     * @param cms CmsObject Object for accessing system resources
     * @param templateFile filename of the template file
     * @param elementName element name of this template in our parent template
     * @param parameters Hashtable with all template class parameters
     * @param templateSelector template section that should be processed
     * @return <EM>true</EM> if this class may stream it's results, <EM>false</EM> otherwise.
     */
    public CmsCacheDirectives getCacheDirectives(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
        // Just set caching to false
        return new CmsCacheDirectives(false);
    }    
}
