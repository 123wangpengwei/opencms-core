/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/main/OpenCmsServlet.java,v $
 * Date   : $Date: 2005/06/23 11:11:38 $
 * Version: $Revision: 1.51 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
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
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.main;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResourceFilter;
import org.opencms.i18n.CmsMessageContainer;
import org.opencms.staticexport.CmsStaticExportData;
import org.opencms.staticexport.CmsStaticExportRequest;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;

/**
 * This the main servlet of the OpenCms system.<p>
 * 
 * From here, all other operations are invoked.
 * Any incoming request is handled in multiple steps:
 * 
 * <ol><li>The requesting user is authenticated and a CmsObject with the user information
 * is created. The CmsObject is used to access all functions of OpenCms, limited by
 * the authenticated users permissions. If the user is not identified, it is set to the default (guest)
 * user.</li>
 * 
 * <li>The requested document is loaded into OpenCms and depending on its type 
 * (and the users persmissions to display or modify it), 
 * it is send to one of the OpenCms loaders do be processed.</li>
 * 
 * <li>
 * The loader will then decide what to do with the contents of the 
 * requested document. In case of a JSP the JSP handling mechanism is invoked, 
 * in case of a XmlPage the template mechanism is started,
 * in case of an image (or other static file) this will simply be returned etc.
 * </li></ol>
 *
 * @author Alexander Kandzior 
 * @author Michael Emmerich 
 * 
 * @version $Revision: 1.51 $ 
 * 
 * @since 6.0.0 
 */
public class OpenCmsServlet extends HttpServlet implements I_CmsRequestHandler {

    /** Name of the <code>DefaultWebApplication</code> parameter in the <code>web.xml</code> OpenCms servlet configuration. */
    public static final String SERVLET_PARAM_DEFAULT_WEB_APPLICATION = "DefaultWebApplication";

    /** Name of the <code>OpenCmsHome</code> parameter in the <code>web.xml</code> OpenCms servlet configuration. */
    public static final String SERVLET_PARAM_OPEN_CMS_HOME = "OpenCmsHome";

    /** Name of the <code>OpenCmsServlet</code> parameter in the <code>web.xml</code> OpenCms servlet configuration. */
    public static final String SERVLET_PARAM_OPEN_CMS_SERVLET = "OpenCmsServlet";

    /** Name of the <code>WebApplicationContext</code> parameter in the <code>web.xml</code> OpenCms servlet configuration. */
    public static final String SERVLET_PARAM_WEB_APPLICATION_CONTEXT = "WebApplicationContext";

    /** Handler prefix. */
    private static final String HANDLE_PATH = "/handle";

    /** Handler implementation names. */
    private static final String[] HANDLER_NAMES = {"404"};

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(OpenCmsServlet.class);

    /**
     * OpenCms servlet main request handling method.<p>
     * 
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        int runlevel = OpenCmsCore.getInstance().getRunLevel();

        // write OpenCms server identification in the response header
        res.setHeader(I_CmsConstants.C_HEADER_SERVER, OpenCmsCore.getInstance().getSystemInfo().getVersion());

        if (runlevel != OpenCms.RUNLEVEL_4_SERVLET_ACCESS) {
            if (runlevel == OpenCms.RUNLEVEL_3_SHELL_ACCESS) {
                // we have shell runlevel only, upgrade to servlet runlevel (required after setup wizard)
                init(getServletConfig());
            } else {
                // illegal runlevel, we can't process requests
                // sending status code 503, indicating that the HTTP server is temporarily overloaded, and unable to handle the request
                res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
        }
        String path = req.getPathInfo();
        if ((path != null) && path.startsWith(HANDLE_PATH)) {
            invokeHandler(req, res);
        } else {
            OpenCmsCore.getInstance().showResource(req, res);
        }
    }

    /**
     * OpenCms servlet request handling method, 
     * will just call {@link #doGet(HttpServletRequest, HttpServletResponse)}.<p>
     * 
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        doGet(req, res);
    }

    /**
     * @see org.opencms.main.I_CmsRequestHandler#getHandlerNames()
     */
    public String[] getHandlerNames() {

        return HANDLER_NAMES;
    }

    /**
     * @see org.opencms.main.I_CmsRequestHandler#handle(HttpServletRequest, HttpServletResponse, String)
     */
    public void handle(HttpServletRequest req, HttpServletResponse res, String name)
    throws IOException, ServletException {

        int errorCode;
        try {
            errorCode = Integer.valueOf(name).intValue();
        } catch (NumberFormatException nf) {
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        switch (errorCode) {
            case 404:
                String path = req.getPathInfo();
                CmsObject cms = null;
                CmsStaticExportData exportData = null;
                try {
                    cms = OpenCms.initCmsObject(OpenCms.getDefaultUsers().getUserExport());
                    exportData = OpenCms.getStaticExportManager().getExportData(req, cms);
                } catch (CmsException e) {
                    // unlikely to happen 
                    if (LOG.isWarnEnabled()) {
                        LOG.warn(Messages.get().key(Messages.LOG_INIT_CMSOBJECT_IN_HANDLER_2, name, path), e);
                    }
                }
                if (exportData != null) {
                    synchronized (this) {
                        try {
                            // generate a static export request wrapper
                            CmsStaticExportRequest exportReq = new CmsStaticExportRequest(req, exportData);
                            // export the resource and set the response status according to the result
                            res.setStatus(OpenCms.getStaticExportManager().export(exportReq, res, cms, exportData));
                        } catch (Throwable t) {
                            if (LOG.isWarnEnabled()) {
                                LOG.warn(Messages.get().key(Messages.LOG_ERROR_EXPORT_1, exportData), t);
                            }
                            openErrorHandler(req, res, errorCode);
                        }
                    }
                } else {
                    openErrorHandler(req, res, errorCode);
                }
                break;
            default:
                openErrorHandler(req, res, errorCode);
        }
    }

    /**
     * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
     */
    public synchronized void init(ServletConfig config) throws ServletException {

        super.init(config);
        try {
            // upgrade the runlevel 
            // usually this should have already been done by the context listener
            // however, after a fresh install / setup this will be done from here
            OpenCmsCore.getInstance().upgradeRunlevel(config.getServletContext());
            // finalize OpenCms initialization
            OpenCmsCore.getInstance().initServlet(this);
        } catch (CmsInitException e) {
            if (Messages.ERR_CRITICAL_INIT_WIZARD_0.equals(e.getMessageContainer().getKey())) {
                // if wizard is still enabled allow retry of initialization (required for setup wizard)
                // this means the servlet init() call must be terminated by an exception
                throw new ServletException(e.getMessage());
            }
        } catch (Throwable t) {
            LOG.error(Messages.get().key(Messages.LOG_ERROR_GENERIC_0), t);
        }
    }

    /**
     * Manages requests to internal OpenCms request handlers.<p>
     * 
     * @param req the current request
     * @param res the current response 
     * @throws ServletException
     * @throws ServletException in case an error occurs
     * @throws IOException in case an error occurs
     */
    protected void invokeHandler(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        String name = req.getPathInfo().substring(HANDLE_PATH.length());
        I_CmsRequestHandler handler = OpenCmsCore.getInstance().getRequestHandler(name);
        if (handler != null) {
            handler.handle(req, res, name);
        } else {
            openErrorHandler(req, res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Displays an error code handler loaded from the OpenCms VFS, 
     * or if such a page does not exist,
     * displays the default servlet container error code.<p>
     *  
     * @param req the current request
     * @param res the current response
     * @param errorCode the error code to display
     * @throws IOException if something goes wrong
     * @throws ServletException if something goes wrong
     */
    protected void openErrorHandler(HttpServletRequest req, HttpServletResponse res, int errorCode)
    throws IOException, ServletException {

        String handlerUri = "/system/handler/handle" + errorCode + ".html";
        CmsObject cms = null;
        try {
            cms = OpenCms.initCmsObject(OpenCms.getDefaultUsers().getUserGuest());
            cms.getRequestContext().setUri(handlerUri);
        } catch (CmsException e) {
            // unlikely to happen, comment: that's what they all say
            CmsMessageContainer container = Messages.get().container(
                Messages.LOG_INIT_CMSOBJECT_IN_HANDLER_2,
                new Integer(errorCode),
                handlerUri);
            if (LOG.isWarnEnabled()) {
                LOG.warn(org.opencms.jsp.Messages.getLocalizedMessage(container, req), e);
            }
        }
        CmsFile file;
        try {
            file = cms.readFile(handlerUri, CmsResourceFilter.IGNORE_EXPIRATION);
        } catch (CmsException e) {
            if (!res.isCommitted()) {
                // handler file does not exist, display default error code page
                res.sendError(errorCode);
            }
            return;
        }
        try {
            OpenCms.getResourceManager().loadResource(cms, file, req, res);
        } catch (CmsException e) {
            CmsMessageContainer container = Messages.get().container(
                Messages.ERR_SHOW_ERR_HANDLER_RESOURCE_2,
                new Integer(errorCode),
                handlerUri);
            throw new ServletException(org.opencms.jsp.Messages.getLocalizedMessage(container, req), e);
        }
    }
}
