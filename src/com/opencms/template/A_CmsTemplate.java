
/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/template/Attic/A_CmsTemplate.java,v $
* Date   : $Date: 2001/05/15 19:29:05 $
* Version: $Revision: 1.3 $
*
* Copyright (C) 2000  The OpenCms Group
*
* This File is part of OpenCms -
* the Open Source Content Mananagement System
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* For further information about OpenCms, please see the
* OpenCms Website: http://www.opencms.com
*
* You should have received a copy of the GNU General Public License
* long with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

package com.opencms.template;

import java.util.*;
import java.io.*;
import com.opencms.launcher.*;
import com.opencms.file.*;
import com.opencms.util.*;
import com.opencms.core.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.servlet.http.*;

/**
 * Abstract template class. Contains all commonly used methods for handling cache properties.
 *
 * @author Alexander Lucas
 * @version $Revision: 1.3 $ $Date: 2001/05/15 19:29:05 $
 */
public abstract class A_CmsTemplate implements I_CmsConstants, I_CmsTemplate, I_CmsLogChannels {

    /**
     * Help method to print nice classnames in error messages
     * @return class name in [ClassName] format
     */
    protected String getClassName() {
        String name = getClass().getName();
        return "[" + name.substring(name.lastIndexOf(".") + 1) + "] ";
    }

    /**
     * Indicates if the results of this class are cacheable in the internal caches.
     * By default all resources in the online project may be stored in the internal
     * cache.
     * <P>
     * Complex classes that are able to include other subtemplates
     * have to check the cacheability of their subclasses here!
     *
     * @param cms CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return <EM>true</EM> if cacheable, <EM>false</EM> otherwise.
     */
    public boolean isCacheable(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
        try {
            return cms.getRequestContext().currentProject().equals(cms.onlineProject());
        } catch(Exception e) {
            return false;
        }
    }

    /**
     * Indicates if the results of this class may be cached by private proxy caches (browsers).
     * <P>
     * Default conditions are:
     * <ul>
     * <li>Resource is cacheable in the internal cache</li>
      * <li>Caching key only consists of the URL</li>
     * </ul>
     * <P>
     * Complex classes that are able to include other subtemplates
     * have to check the cacheability of their subclasses here!
     *
     * @param cms CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return <EM>true</EM> if cacheable, <EM>false</EM> otherwise.
     */
    public boolean isProxyPrivateCacheable(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
        boolean result;
        try {
            result = isProxyPublicCacheable(cms, templateFile, elementName, parameters, templateSelector)
            && cms.getRequestContext().currentUser().equals(cms.anonymousUser());
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    /**
     * Indicates if the results of this class may be cached by public proxy caches.
     * <P>
     * Default conditions are:
     * <ul>
     * <li>Resource may be cached by private proxies</li>
     * <li>Current user is Guest (otherwise privat information may be stored)</li>
     * </ul>
     * <P>
     * Complex classes that are able to include other subtemplates
     * have to check the cacheability of their subclasses here!
     *
     * @param cms CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return <EM>true</EM> if cacheable, <EM>false</EM> otherwise.
     */
    public boolean isProxyPublicCacheable(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
        CmsRequestContext reqContext = cms.getRequestContext();
        String key = (String)getKey(cms, templateFile, parameters, templateSelector);
        String projId = "" + reqContext.currentProject().getId() + ":";
        String uri = reqContext.getUri();
        String uri2 = null;
        if(uri != null && uri.indexOf("?") > 1) {
            uri2 = uri.substring(0, uri.indexOf("?"));
        }

        boolean result = (key.equals(uri) || key.equals(uri2) || key.equals(templateFile)
                           || key.equals(projId + uri) || key.equals(projId + uri2) || key.equals(projId + templateFile));
        return result;
    }

    /**
     * Indicates if the results of this class are "static" and may be exported.
     * <P>
     * Default conditions are:
     * <ul>
     * <li>Resource may be cached by public proxies</li>
     * <li>There are no parameters in the URL</li>
     * <li>The resource's internal flag must not be set</li>
     * </ul>
     * <P>
     * Complex classes that are able to include other subtemplates
     * have to check the export ability of their subclasses here!
     *
     * @param cms CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return <EM>true</EM> if exportable, <EM>false</EM> otherwise.
     */
    public boolean isExportable(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
        String queryString = ((HttpServletRequest)cms.getRequestContext().getRequest().getOriginalRequest()).getQueryString();
        boolean result = isProxyPrivateCacheable(cms, templateFile, elementName, parameters, templateSelector)
            && (queryString == null || "".equals(queryString));
        try {
            CmsFile file = cms.readFile(templateFile);
            result = result && (file.getAccessFlags() & file.C_ACCESS_INTERNAL_READ) != file.C_ACCESS_INTERNAL_READ;
        } catch(Exception e) {
            result = false;
        }
        return result;
    }

    /**
     * Indicates if the current template class is able to stream it's results
     * directly to the response oputput stream.
     * <P>
     * Classes must not set this feature, if they might throw special
     * exception that cause HTTP errors (e.g. 404/Not Found), or if they
     * might send HTTP redirects.
     * <p>
     * If a class sets this feature, it has to check the
     * isStreaming() property of the RequestContext. If this is set
     * to <code>true</code> the results must be streamed directly
     * to the output stream. If it is <code>false</code> the results
     * must not be streamed.
     * <P>
     * Complex classes that are able to include other subtemplates
     * have to check the streaming ability of their subclasses here!
     *
     * @param cms CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return <EM>true</EM> if this class may stream it's results, <EM>false</EM> otherwise.
     */
    public boolean isStreamable(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
        return false;
    }

    /**
     * Collect caching informations from the current template class.
     * <P>
     * Complex classes that are able to include other subtemplates
     * have to check the streaming ability of their subclasses here!
     *
     * @param cms CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return <EM>true</EM> if this class may stream it's results, <EM>false</EM> otherwise.
     */
    public CmsCacheDirectives collectCacheDirectives(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
        boolean isCacheable = isCacheable(cms, templateFile, elementName, parameters, templateSelector);
        boolean isProxyPublicCacheable = isProxyPublicCacheable(cms, templateFile, elementName, parameters, templateSelector);
        boolean isProxyPrivateCacheable = isProxyPrivateCacheable(cms, templateFile, elementName, parameters, templateSelector);
        boolean isExportable = isExportable(cms, templateFile, elementName, parameters, templateSelector);
        boolean isStreamable = isStreamable(cms, templateFile, elementName, parameters, templateSelector);
        CmsCacheDirectives result = new CmsCacheDirectives(isCacheable, isProxyPrivateCacheable, isProxyPublicCacheable, isExportable, isStreamable);
        return result;
    }

    protected void debugPrint(String s, int i) {
        System.err.print("* " + s);
        for(int j=0; j<(15-s.length()); j++) {
            System.err.print(" ");
        }

        //INT PUB PRV EXP STR
        System.err.print(" " + ((i & 1) == 1?"X":" ") + "  ");
        System.err.print(" " + ((i & 2) == 2?"X":" ") + "  ");
        System.err.print(" " + ((i & 4) == 4?"X":" ") + "  ");
        System.err.print(" " + ((i & 8) == 8?"X":" ") + "  ");
        System.err.print(" " + ((i & 16) == 16?"X":" ") + "  ");
    }



    /**
     * Help method that handles any occuring error by writing
     * an error message to the OpenCms logfile and throwing a
     * CmsException of the type "unknown".
     * @param errorMessage String with the error message to be printed.
     * @exception CmsException
     */
    protected void throwException(String errorMessage) throws CmsException {
        throwException(errorMessage, CmsException.C_UNKNOWN_EXCEPTION);
    }

    /**
     * Help method that handles any occuring error by writing
     * an error message to the OpenCms logfile and throwing a
     * CmsException of the given type.
     * @param errorMessage String with the error message to be printed.
     * @param type Type of the exception to be thrown.
     * @exception CmsException
     */
    protected void throwException(String errorMessage, int type) throws CmsException {
        if((A_OpenCms.isLogging() && I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING)) {
            A_OpenCms.log(C_OPENCMS_CRITICAL, getClassName() + errorMessage);
        }
        throw new CmsException(errorMessage, type);
    }

    /**
     * Help method that handles any occuring error by writing
     * an error message to the OpenCms logfile and re-throwing a
     * caught exception.
     * @param errorMessage String with the error message to be printed.
     * @param e Exception to be re-thrown.
     * @exception CmsException
     */
    protected void throwException(String errorMessage, Exception e) throws CmsException {
        if((A_OpenCms.isLogging() && I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING)) {
            A_OpenCms.log(C_OPENCMS_CRITICAL, getClassName() + errorMessage);
            A_OpenCms.log(C_OPENCMS_CRITICAL, getClassName() + "Exception: " + e);
        }
        if(e instanceof CmsException) {
            throw (CmsException)e;
        }
        else {
            throw new CmsException(errorMessage, CmsException.C_UNKNOWN_EXCEPTION, e);
        }
    }
}
