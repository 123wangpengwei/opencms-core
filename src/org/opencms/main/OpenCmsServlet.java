/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/main/OpenCmsServlet.java,v $
 * Date   : $Date: 2004/02/13 13:41:45 $
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

package org.opencms.main;

import org.opencms.staticexport.CmsStaticExportData;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
 * requested document. In case of an XMLTemplate the template mechanism will 
 * be started, in case of a JSP the JSP handling mechanism is invoked, 
 * in case of an image (or other static file) this will simply be returned etc.
 * </li></ol>
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * @author Michael Emmerich (m.emmerich@alkacon.com)
 * 
 * @version $Revision: 1.13 $
 */
public class OpenCmsServlet extends HttpServlet implements I_CmsRequestHandler {
    
    /** Handler prefix */
    private static final String C_HANDLE = "/handle";
    
    /** Handler implementation names */
    private static final String[] C_HANDLER_NAMES = {"404", "500"};

    /**
     * OpenCms servlet main request handling method.<p>
     * 
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        if (OpenCmsCore.getInstance().getRunLevel() < 3) {
            // check if setup was completed correctly
            init(getServletConfig());
            if (OpenCmsCore.getInstance().getRunLevel() < 3) {
                throw new ServletException("OpenCms not properly configured!");
            }
        }
        String path = req.getPathInfo();
        if ((path != null) && path.startsWith(C_HANDLE)) {
            OpenCmsCore.getInstance().initStartupClasses(req, res);            
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
    public void doPost (HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {            
        doGet(req, res);
    }
    
    /**
     * @see org.opencms.main.I_CmsRequestHandler#getHandlerNames()
     */
    public String[] getHandlerNames() {
        return C_HANDLER_NAMES;
    }

    /**
     * @see org.opencms.main.I_CmsRequestHandler#handle(HttpServletRequest, HttpServletResponse, String)
     */
    public void handle(HttpServletRequest req, HttpServletResponse res, String name) throws IOException, ServletException {
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
                    cms = OpenCmsCore.getInstance().initCmsObject(req, res, OpenCms.getDefaultUsers().getUserExport(), null);            
                    exportData = OpenCms.getStaticExportManager().getExportData(req, cms);
                } catch (CmsException e) {
                    // unlikley to happen 
                    if (OpenCms.getLog(this).isWarnEnabled()) {                    
                        OpenCms.getLog(this).warn("Error initializing CmsObject in " + name + " handler for '" + path + "'", e);
                    }
                }
                if (exportData != null) {
                    synchronized (this) {
                        try {
                            OpenCms.getStaticExportManager().export(req, res, cms, exportData);
                        } catch (Throwable t) {
                            if (OpenCms.getLog(this).isWarnEnabled()) {                    
                                OpenCms.getLog(this).warn("Error exporting " + exportData, t);
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
     * OpenCms an error code handler page inside the OpenCms VFS, 
     * or if such a page does not exist,
     * displays the default servlet container error code.<p>
     *  
     * @param req the current request
     * @param res the current response
     * @param errorCode the error code to display
     * @throws IOException if something goes wrong
     * @throws ServletException if something goes wrong
     */
    private void openErrorHandler(HttpServletRequest req, HttpServletResponse res, int errorCode) throws IOException, ServletException {
        String handlerUri = "/system/handler/handle" + errorCode + ".html";
        CmsObject cms = null;            
        try {
            cms = OpenCmsCore.getInstance().initCmsObject(req, res, OpenCms.getDefaultUsers().getUserGuest(), null);  
            cms.getRequestContext().setUri(handlerUri);
        } catch (CmsException e) {
            // unlikley to happen 
            if (OpenCms.getLog(this).isWarnEnabled()) {                    
                OpenCms.getLog(this).warn("Error initializing CmsObject in " + errorCode + " URI handler for '" + handlerUri + "'", e);
            }
        }     
        CmsFile file;
        try {
            file = cms.readFile(handlerUri);
        } catch (CmsException e) {
            // handler file does not exist, display default error code page
            res.sendError(errorCode);
            return;
        }
        OpenCmsCore.getInstance().setResponse(cms, file);
        OpenCmsCore.getInstance().showResource(req, res, cms, file);
    }
    
    /**
     * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
     */
    public synchronized void init(ServletConfig config) throws ServletException {
        super.init(config);
        // upgrade the runlevel
        OpenCmsCore.getInstance().upgradeRunlevel(config.getServletContext());  
        // add this as handler for 404 requests
        OpenCmsCore.getInstance().addRequestHandler(this);
    }
    
    /**
     * Manages request to internal OpenCms request handlers.<p>
     * 
     * @param req the current request
     * @param res the current response 
     * @throws ServletException
     * @throws ServletException in case an error occurs
     * @throws IOException in case an error occurs
     */
    private void invokeHandler(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String name = req.getPathInfo().substring(C_HANDLE.length());
        I_CmsRequestHandler handler = OpenCmsCore.getInstance().getRequestHandler(name);
        if (handler != null) {
            handler.handle(req, res, name);   
        } else {
            openErrorHandler(req, res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    

}

