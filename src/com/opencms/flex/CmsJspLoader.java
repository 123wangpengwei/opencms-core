/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/flex/Attic/CmsJspLoader.java,v $
* Date   : $Date: 2003/01/31 17:00:10 $
* Version: $Revision: 1.18 $
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


package com.opencms.flex;

import com.opencms.boot.I_CmsLogChannels;
import com.opencms.core.A_OpenCms;
import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.file.CmsFile;
import com.opencms.file.CmsObject;
import com.opencms.file.CmsRequestContext;
import com.opencms.file.CmsResource;
import com.opencms.flex.cache.CmsFlexCache;
import com.opencms.flex.cache.CmsFlexRequest;
import com.opencms.flex.cache.CmsFlexResponse;
import com.opencms.launcher.I_CmsLauncher;
import com.opencms.util.Encoder;
import com.opencms.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The JSP loader which enables the execution of JSP in OpenCms.<p>
 *
 * It does NOT extend {@link com.opencms.launcher.A_CmsLauncher}, since JSP are not related
 * to the OpenCms Template mechanism. However, it implements the
 * launcher interface so that JSP can be sub-elements in XMLTemplace pages.
 *
 * @author  Alexander Kandzior (a.kandzior@alkacon.com)
 *
 * @version $Revision: 1.18 $
 * @since FLEX alpha 1
 * 
 * @see I_CmsResourceLoader
 * @see com.opencms.launcher.I_CmsLauncher
 */
public class CmsJspLoader implements I_CmsLauncher, I_CmsResourceLoader {

    /** The directory to store the generated JSP pages in (absolute path) */
    private static String m_jspRepository = null;
    
    /** The directory to store the generated JSP pages in (relative path in web application */
    private static String m_jspWebAppRepository = null;
    
    /** The CmsFlexCache used to store generated cache entries in */
    private static CmsFlexCache m_cache;
    
    /** Export URL for JSP pages */
    private static String m_jspExportUrl;
    
    /** Flag to indicate if error pages are mared a "commited" */
    // TODO: This is a hack, investigate this issue with different runtime environments
    private static boolean m_errorPagesAreNotCommited = false; // should work for Tomcat 4.1

    /** Special JSP directive tag start (<code>&lt;%@</code>)*/
    public static final String C_DIRECTIVE_START = "<%@";

    /** Special JSP directive tag start (<code>%&gt;</code>)*/
    public static final String C_DIRECTIVE_END ="%>";
    
    /** Encoding to write JSP files to disk (<code>ISO-8859-1</code>) */
    public static final String C_DEFAULT_JSP_ENCODING = "ISO-8859-1";
    
    /** Extension for JSP managed by OpenCms (<code>.jsp</code>) */
    public static final String C_JSP_EXTENSION = ".jsp";      

    // Static export related stuff
    /** Parameter constant to indicate that the export is requested */
    private static final String C_EXPORT_PARAM = "_flex_export"; 
    
    /** Parameter constant to indicate a body previously discovered in an XMLTemplate */
    private static final String C_EXPORT_BODY = "_flex_export_body";       
    
    /** Parameter constant to indicate encoding used in calling template */
    private static final String C_EXPORT_ENCODING = "_flex_export_encoding";     
    
    /** Header constant to indicate the found links in the response return headers */
    private static final String C_EXPORT_HEADER = "_flex_export_links";

    /** Separator constant to separate return headers */
    private static final String C_EXPORT_HEADER_SEP = "/";
    
    /** Name of export URL runtime property */
    public static final String C_LOADER_JSPEXPORTURL = "flex.jsp.exporturl";
    
    /** Name of "error pages are commited or not" runtime property*/ 
    public static final String C_LOADER_ERRORPAGECOMMIT = "flex.jsp.errorpagecommit";
    
    /** Flag for debugging output. Set to 9 for maximum verbosity. */ 
    private static final int DEBUG = 0;
        
    /**
     * The constructor of the class is empty, the initial instance will be 
     * created by the launcher manager upon startup of OpenCms.<p>
     * 
     * To initilize the fields in this class, the <code>setOpenCms()</code>
     * method will be called by the launcher.
     * 
     * @see com.opencms.launcher.CmsLauncherManager
     * @see #setOpenCms(A_OpenCms openCms)
     */
    public CmsJspLoader() {
        // NOOP
    }
    
    // ---------------------------- Implementation of interface com.opencms.launcher.I_CmsLauncher          
    
    /**
     * This is part of the I_CmsLauncher interface, but for JSP so far this 
     * is a NOOP.
     */
    public void clearCache() {
        // NOOP
    }
    
    /**
     * This is part of the I_CmsLauncher interface, 
     * used here to call the init() method.
     * 
     * @see #init(A_OpenCms openCms)
     */
    public void setOpenCms(A_OpenCms openCms) {
        init(openCms);
    }    
    
    /** 
     * Returns the ID that indicates the type of the launcher.
     * 
     * The IDs for all launchers of the core distributions are constants 
     * in the I_CmsLauncher interface.
     * The value returned is <code>com.opencms.launcher.I_CmsLauncher.C_TYPE_JSP</code>.
     *
     * @return launcher ID
     * 
     * @see com.opencms.launcher.I_CmsLauncher
     */
    public int getLauncherId() {
        return com.opencms.launcher.I_CmsLauncher.C_TYPE_JSP;
    }
    
    /** 
     * Start launch method called by the OpenCms system to show a resource,
     * this basically processes the resource and returns the output.<p>
     * 
     * This is part of the Launcher interface.
     * All requests will be forwarded to the <code>load()</code> method of this 
     * class. That forms the link between the Launcher and Loader interfaces.<p>
     * 
     * Exceptions thrown in the <code>load()</code> method of this loader
     * will be handled here, usually by wrapping them in a CmsException
     * that will then be shown in the OpenCms error dialog.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param file CmsFile Object with the selected resource to be shown.
     * @param startTemplateClass Name of the template class to start with.
     * @param openCms a instance of A_OpenCms for redirect-needs
     * @throws CmsException all exeptions in the load process of a JSP will be caught here and wrapped to a CmsException
     * 
     * @see com.opencms.launcher.I_CmsLauncher
     * @see #load(CmsObject cms, CmsFile file, HttpServletRequest req, HttpServletResponse res) 
     */
    public void initlaunch(CmsObject cms, CmsFile file, String startTemplateClass, A_OpenCms openCms) throws CmsException {
        HttpServletRequest req;
        HttpServletResponse res;

        if (cms.getRequestContext().getRequest() instanceof com.opencms.core.CmsExportRequest) {
            // request is an export request 
            if (DEBUG > 1) System.err.println("FlexJspLoader: Export requested for " + file.getAbsolutePath());
            // get the contents of the exported page
            byte[] export = exportJsp(cms, file);
            // try to write the result to the current output stream
            try {
                OutputStream output = cms.getRequestContext().getResponse().getOutputStream();
                output.write(export);
            } catch (IOException e) {
                throw new CmsException("IOException writing contents of exported JSP for URI " + cms.getRequestContext().getUri(), 
                    CmsException.C_FLEX_LOADER, e);
            }
        } else {
            // wrap request and response
            req = (HttpServletRequest)cms.getRequestContext().getRequest().getOriginalRequest();
            res = (HttpServletResponse)cms.getRequestContext().getResponse().getOriginalResponse();            
            // check if this is an export request
            int oldMode = exportCheckMode(cms, req);            
            // load and process the JSP 
            try {
                // load the resource
                load(cms, file, req, res);
            } catch (Exception e) {
                // all Exceptions are caught here and get translated to a CmsException for display in the OpenCms error dialog
                if (DEBUG > 1) System.err.println("Error in Flex loader: " + e + Utils.getStackTrace(e));
                throw new CmsException("Error in Flex loader", CmsException.C_FLEX_LOADER, e, true);
            } finally {
                exportResetMode(cms, oldMode);
            }
        }        
    } 

    // ---------------------------- Static export related stuff
    
    /**
     * Checks if the request parameter C_EXPORT_PARAM is set, if so sets the CmsObject 
     * working mode to C_MODUS_EXPORT.
     * 
     * @param cms provides the current cms context
     * @param req the current request 
     * @return int the mode previously set in the CmsObject
     */
    private int exportCheckMode(CmsObject cms, HttpServletRequest req) {
        int oldMode = cms.getMode();
        String exportUri = req.getParameter(C_EXPORT_PARAM); 
        if (exportUri != null) {
            if (! exportUri.equals(cms.getRequestContext().getUri())) {
                // URI is not the same, so this is a sub - element
                cms.getRequestContext().setUri(exportUri);
            }
            cms.setMode(CmsObject.C_MODUS_EXPORT);
        }   
        // check body
        String body = req.getParameter(C_EXPORT_BODY);
        if (body != null) {
            cms.getRequestContext().setAttribute(I_CmsConstants.C_XML_BODY_ELEMENT, body);
        }
        // check encoding
        String encoding = req.getParameter(C_EXPORT_ENCODING);
        if (encoding != null) {
            cms.getRequestContext().setEncoding(encoding);
        }
        return oldMode;     
    }

    /**
     * Restores the mode stored in the <code>oldMode</code> paameter to the CmsObject.
     * 
     * @param cms provides the current cms context
     * @param oldMode the old mode to restore in the CmsObject
     */
    private void exportResetMode(CmsObject cms, int oldMode) {
        cms.setMode(oldMode);
    }

    /**
     * Returns the links found in the currently processed page as response headers,
     * so that the static export can pick them up later.
     *
     * @param cms provides the current cms context
     * @param res the response to set the headers in
     */
    private void exportSetLinkHeader(CmsObject cms, HttpServletResponse res) {
        // get the links found on the page from the current request context
        Vector v = cms.getRequestContext().getLinkVector();
        // making the vector a set removes the duplicate entries
        Set s = new HashSet(v);
        StringBuffer links = new StringBuffer(s.size() * 64);
        Iterator i = s.iterator();
        // build a string out of the found links
        while (i.hasNext()) {
            links.append(Encoder.encode((String)i.next()));
            if (i.hasNext()) links.append(C_EXPORT_HEADER_SEP);
        }
        // set the export header and we are finished
        res.setHeader(C_EXPORT_HEADER, new String(links));
    }
    
    /**
     * Perform an export of the requested JSP page.<p>
     * 
     * The export of a JSP is done in the following way:
     * <ul>
     * <li>A HttpURLConnection is openend to the address configured in the runtime property with
     * the name {@link #C_LOADER_JSPEXPORTURL}, which usually should be the current OpenCms server.
     * <li>The URI of the <code>file</code> is appended to the connection as path information, so
     * this will be the page requested and exported.
     * <li>All current request parameters are encoded and also added to the request as parameters.
     * <li>The currently requested URI is also appended as value of the special parameter {@link
     * #C_EXPORT_PARAM}.
     * <li>When processing this special request, the mode of the <code>CmsObject</code> will be
     * set to <code>C_MODUS_EXPORT</code>, which is the required mode if you want to generate
     * the result for an export.
     * <li>All links found while processing the exported JSP will be written in a special header
     * of the response, called {@link #C_EXPORT_HEADER}.
     * <li>The response result will be checked for the headers and all links found will be added to
     * the link vector of the currently processed page.
     * <li>The content of the resonse will be read into a byte array and returned as result of this
     * call.
     * </ul>
     *  
     * @param cms provides the current cms context
     * @param file the JSP file requested
     * @return the contents of the JSP page for the export
     */
    private byte[] exportJsp(CmsObject cms, CmsFile file) throws CmsException {
        
        // check if we are properly initialized
        if (m_jspExportUrl == null) {
            throw new CmsException("JSP export URL not set, can not export JSP", CmsException.C_FLEX_LOADER);
        }
        
        ByteArrayOutputStream bytes = null;        
        CmsRequestContext context = cms.getRequestContext();
        
        // generate export URL
        StringBuffer exportUrl = new StringBuffer(m_jspExportUrl); 
        exportUrl.append(file.getAbsolutePath());
        exportUrl.append("?");
        
        // add parameters to export call
        Enumeration params = context.getRequest().getParameterNames();
        while (params.hasMoreElements()) {
            String key = (String)params.nextElement();
            String values[] = (String[])context.getRequest().getParameterValues(key);
            for (int i=0; i<values.length; i++) {
                exportUrl.append(key);
                exportUrl.append("=");
                exportUrl.append(Encoder.encode(values[i]));
                exportUrl.append("&");
            }
        }
        // add the export parameter to the request
        exportUrl.append(C_EXPORT_PARAM);
        exportUrl.append("=");
        exportUrl.append(cms.getRequestContext().getUri());
        // add the original requested body file to the request
        String body = (String)cms.getRequestContext().getAttribute(I_CmsConstants.C_XML_BODY_ELEMENT);
        if (body != null) {
            exportUrl.append("&");
            exportUrl.append(C_EXPORT_BODY);
            exportUrl.append("=");
            exportUrl.append(Encoder.encode(body));            
        }
        // add the encoding used for the output page to the request
        String encoding = cms.getRequestContext().getEncoding();
        exportUrl.append("&");
        exportUrl.append(C_EXPORT_ENCODING);
        exportUrl.append("=");
        exportUrl.append(Encoder.encode(encoding));        
        
        if (DEBUG > 2) System.err.println("CmsJspLoader.exportJsp(): JSP export URL is " + exportUrl);

        // perform the export with an URLConnection
        URL export;
        HttpURLConnection urlcon;
        DataInputStream input;
                
        try {
            export = new URL(new String(exportUrl));
            urlcon = (HttpURLConnection) export.openConnection();
            // set request type to POST
            urlcon.setRequestMethod("POST");
            HttpURLConnection.setFollowRedirects(false);
            // input and output stream
            input = new DataInputStream(urlcon.getInputStream());
            bytes = new ByteArrayOutputStream(urlcon.getContentLength()>0?urlcon.getContentLength():1024);
        } catch (Exception e) {
            // all exceptions here will be IO related
            throw new CmsException("IO related error while exporting JSP for URI " + cms.getRequestContext().getUri(), 
                CmsException.C_FLEX_LOADER, e);
        }
        
        // check if links are present in the exported page 
        String cmslinks = urlcon.getHeaderField(C_EXPORT_HEADER);
        if (cmslinks != null) {
            // add all the links to the current cms context
            StringTokenizer tok = new StringTokenizer(cmslinks, C_EXPORT_HEADER_SEP);
            while (tok.hasMoreTokens()) {
                String link = Encoder.decode(tok.nextToken(), "UFT-8", true);
                cms.getRequestContext().addLink(link);
                if (DEBUG > 3) System.err.println("CmsJspLoader.exportJsp(): Extracted link " + link);
            }
        }
        // now read the page content and write it to the byte array
        try {
            int b;
            while ((b = input.read()) > 0) {
                bytes.write(b);
            }
        } catch (IOException e) {
            throw new CmsException("IO error writing bytes to buffer exporting JSP for URI " + cms.getRequestContext().getUri(),
                CmsException.C_FLEX_LOADER, e);            
        }

        return bytes.toByteArray();
    }

    // ---------------------------- Implementation of interface com.opencms.flex.I_CmsResourceLoader    
    
    /** Destroy this ResourceLoder, this is a NOOP so far.  */
    public void destroy() {
        // NOOP
    }
    
    /**
     * Return a String describing the ResourceLoader,
     * which is <code>"The OpenCms default resource loader for JSP"</code>
     * 
     * @return a describing String for the ResourceLoader 
     */
    public String getResourceLoaderInfo() {
        return "The OpenCms default resource loader for JSP";
    }
    
    /** 
     * Initialize the ResourceLoader,
     * here the configuration for the JSP repository (directories used) is set.
     *
     * @param openCms An OpenCms object to use for initalizing.
     */
    public void init(A_OpenCms openCms) {
        m_jspRepository = com.opencms.boot.CmsBase.getBasePath();
        if (m_jspRepository.indexOf("WEB-INF") >= 0) {
            // Should always be true, just make sure we don't generate an exception in untested environments
            m_jspRepository = m_jspRepository.substring(0, m_jspRepository.indexOf("WEB-INF")-1);
        }
        source.org.apache.java.util.Configurations c = openCms.getConfiguration();
        m_jspWebAppRepository = c.getString("flex.jsp.repository", "/WEB-INF/jsp");
        m_jspRepository += m_jspWebAppRepository.replace('/', File.separatorChar);
        if (! m_jspRepository.endsWith(File.separator)) m_jspRepository += File.separator;
        if (DEBUG > 0) System.err.println("JspLoader: Setting jsp repository to " + m_jspRepository);
        // Get the cache from the runtime properties
        m_cache = (CmsFlexCache)A_OpenCms.getRuntimeProperty(C_LOADER_CACHENAME);
        // Get the export URL from the runtime properties
        m_jspExportUrl = (String)A_OpenCms.getRuntimeProperty(C_LOADER_JSPEXPORTURL);
        if (I_CmsLogChannels.C_LOGGING && A_OpenCms.isLogging(I_CmsLogChannels.C_FLEX_LOADER)) {
            A_OpenCms.log(I_CmsLogChannels.C_FLEX_LOADER, "Initialized!");        
            A_OpenCms.log(I_CmsLogChannels.C_FLEX_LOADER, "JSP repository (absolute path): " + m_jspRepository);        
            A_OpenCms.log(I_CmsLogChannels.C_FLEX_LOADER, "JSP repository (web application path): " + m_jspWebAppRepository);              
            A_OpenCms.log(I_CmsLogChannels.C_FLEX_LOADER, "JSP export URL: " + m_jspExportUrl);
        }
        // Get the "error pages are commited or not" flag from the runtime properties
        Boolean errorPagesAreNotCommited = (Boolean)A_OpenCms.getRuntimeProperty(C_LOADER_ERRORPAGECOMMIT);
        if (errorPagesAreNotCommited != null) m_errorPagesAreNotCommited = errorPagesAreNotCommited.booleanValue();
    }
    
    /**
     * Set's the JSP export URL.<p>
     * 
     * This is required after <code>init()</code> called if the URL was not set in <code>opencms.
     * properties</code>.
     * 
     * @param url the JSP export URL
     */
    public static void setJspExportUrl(String value) {
        m_jspExportUrl = value;
    }
    
    /**
     * Basic top-page processing method for this I_CmsResourceLoader,
     * this method is called by <code>initlaunch()</code> if a JSP is requested and
     * the original request was from the launcher manager.
     *
     * @param cms The initialized CmsObject which provides user permissions
     * @param file The requested OpenCms VFS resource
     * @param req The original servlet request
     * @param res The original servlet response
     * 
     * @throws ServletException might be thrown in the process of including the JSP 
     * @throws IOException might be thrown in the process of including the JSP 
     * 
     * @see I_CmsResourceLoader
     * @see #initlaunch(CmsObject cms, CmsFile file, String startTemplateClass, A_OpenCms openCms)
     */
    public void load(CmsObject cms, CmsFile file, HttpServletRequest req, HttpServletResponse res) 
    throws ServletException, IOException {       

        long timer1 = 0;
        if (DEBUG > 0) {
            timer1 = System.currentTimeMillis();        
            System.err.println("========== JspLoader loading: " + file.getAbsolutePath());
        }

        boolean streaming = false;            
        boolean bypass = false;
        
        // check if export mode is active, if so "streaming" must be deactivated
        boolean exportmode = (cms.getMode() == CmsObject.C_MODUS_EXPORT);
        
        try {
            // Read caching property from requested VFS resource                                     
            String stream = cms.readProperty(file.getAbsolutePath(), I_CmsResourceLoader.C_LOADER_STREAMPROPERTY);                    
            if (stream != null) {
                if ("yes".equalsIgnoreCase(stream) || "true".equalsIgnoreCase(stream)) {
                    // streaming not allowed in export mode
                    streaming = !exportmode;                
                } else if ("bypass".equalsIgnoreCase(stream) || "bypasscache".equalsIgnoreCase(stream)) {
                    // bypass not allowed in export mode
                    bypass = !exportmode;
                }
            }
        } catch (CmsException e) {
            throw new ServletException("FlexJspLoader: Error while loading stream properties for " + file.getAbsolutePath() + "\n" + e, e);
        } 
        
        if (DEBUG > 1) System.err.println("========== JspLoader stream=" + streaming + " bypass=" + bypass);
        
        CmsFlexRequest w_req; 
        CmsFlexResponse w_res;
        if (req instanceof CmsFlexRequest) {
            w_req = (CmsFlexRequest)req; 
        } else {
            w_req = new CmsFlexRequest(req, file, m_cache, cms); 
        }        
        if (res instanceof CmsFlexResponse) {
            w_res = (CmsFlexResponse)res;              
        } else {
            w_res = new CmsFlexResponse(res, streaming, true, cms.getRequestContext().getEncoding());
        }
        
        if (bypass) {
            // Bypass Flex cache for this page (this solves some compatibility issues in BEA Weblogic)        
            if (DEBUG > 1) System.err.println("JspLoader.load() bypassing cache for file " + file.getAbsolutePath());
            // Update the JSP first if neccessary            
            String target = updateJsp(cms, file, w_req, new HashSet(11));
            // Dispatch to external JSP
            req.getRequestDispatcher(target).forward(w_req, res);              
            if (DEBUG > 1) System.err.println("JspLoader.load() cache was bypassed!");
        } else {
            // Flex cache not bypassed            
            try {
                w_req.getCmsRequestDispatcher(file.getAbsolutePath()).include(w_req, w_res);
            } catch (java.net.SocketException e) {        
                // Uncritical, might happen if client (browser) does not wait until end of page delivery
                if (DEBUG > 1) System.err.println("JspLoader.load() ignoring SocketException " + e);
            }            
            if (! streaming && ! w_res.isSuspended()) {
                try {      
                    if (! res.isCommitted() || m_errorPagesAreNotCommited) {
                        // If a JSP errorpage was triggered the response will be already committed here
                        byte[] result = w_res.getWriterBytes();
                        
                        // Encoding project:  
                        // The byte array will internally be encoded in the OpenCms 
                        // default encoding. In case another encoding is set 
                        // in the 'content-encoding' property of the file, 
                        // we need to re-encode the output here. 
                        result = Encoder.changeEncoding(result, A_OpenCms.getDefaultEncoding(), cms.getRequestContext().getEncoding());   
                                                                                                                              
                        // Check for export request links 
                        if (exportmode) {
                            exportSetLinkHeader(cms, w_res);
                        }
                              
                        // Process headers and write output                                          
                        res.setContentLength(result.length);
                        CmsFlexResponse.processHeaders(w_res.getHeaders(), res);                        
                        res.getOutputStream().write(result);
                        res.getOutputStream().flush();
                    } else if (DEBUG > 1) {
                        System.err.println("JspLoader.load() resource is already commited!");
                    }
                } catch (IllegalStateException e) {
                    // Uncritical, might happen if JSP error page was used
                    if (DEBUG > 1) System.err.println("JspLoader.load() ignoring IllegalStateException " + e);
                } catch (java.net.SocketException e) {        
                    // Uncritical, might happen if client (browser) does not wait until end of page delivery
                    if (DEBUG > 1) System.err.println("JspLoader.load() ignoring SocketException " + e);
                }       
            }
        }
        
        if (DEBUG > 0) {
            long timer2 = System.currentTimeMillis() - timer1;
            System.err.println("========== JspLoader time delivering JSP for " + file.getAbsolutePath() + ": " + timer2 + "ms");
        }        
    }
    
    /**
     * Method to enable JSPs to be used as sub-elements in XMLTemplates.
     *
     * @param cms The initialized CmsObject which provides user permissions
     * @param file The requested OpenCms VFS resource
     * 
     * @throws CmsException In case the Loader can not process the requested resource
     * 
     * @see CmsJspTemplate
     */
    public byte[] loadTemplate(CmsObject cms, CmsFile file) 
    throws CmsException {

        byte[] result = null;
        
        long timer1 = 0;
        if (DEBUG > 0) {
            timer1 = System.currentTimeMillis();        
            System.err.println("========== JspLoader (Template) loading: " + file.getAbsolutePath());
        }       

        if (cms.getRequestContext().getRequest() instanceof com.opencms.core.CmsExportRequest) {
            if (DEBUG > 1) System.err.println("FlexJspLoader.loadTemplate(): Export requested for " + file.getAbsolutePath());
            // export the JSP
            result = exportJsp(cms, file);
        } else {
            HttpServletRequest req = (HttpServletRequest)cms.getRequestContext().getRequest().getOriginalRequest();
            HttpServletResponse res = (HttpServletResponse)cms.getRequestContext().getResponse().getOriginalResponse();             
            
            CmsFlexRequest w_req; 
            CmsFlexResponse w_res;
            if (req instanceof CmsFlexRequest) {
                w_req = (CmsFlexRequest)req; 
            } else {
                w_req = new CmsFlexRequest(req, file, m_cache, cms); 
            }        
            if (res instanceof CmsFlexResponse) {
                w_res = (CmsFlexResponse)res;              
            } else {
                w_res = new CmsFlexResponse(res, false, false, cms.getRequestContext().getEncoding());
            }
            
            try {
                w_req.getCmsRequestDispatcher(file.getAbsolutePath()).include(w_req, w_res);
            } catch (java.net.SocketException e) {        
                // Uncritical, might happen if client (browser) does not wait until end of page delivery
                if (DEBUG > 1) System.err.println("JspLoader.loadTemplate() ignoring SocketException " + e);
            } catch (Exception e) {            
                System.err.println("Error in CmsJspLoader.loadTemplate() while loading: " + e.toString());
                if (DEBUG > 0) System.err.println(com.opencms.util.Utils.getStackTrace(e));
                throw new CmsException("Error in CmsJspLoader.loadTemplate() while loading " + file.getAbsolutePath() + "\n" + e, CmsException.C_LAUNCH_ERROR, e);
            } 
    
            if (! w_res.isSuspended()) {
                try {      
                    if ((res == null) || (! res.isCommitted())) {
                        // If a JSP errorpage was triggered the response will be already committed here
                        result = w_res.getWriterBytes();                                                
                        // Encoding project:
                        // The byte array will internally be encoded in the OpenCms
                        // default encoding. In case another encoding is set
                        // in the 'content-encoding' property of the file,
                        // we need to re-encode the output here
                        result = Encoder.changeEncoding(result, A_OpenCms.getDefaultEncoding(), cms.getRequestContext().getEncoding());                                              
                    }
                } catch (IllegalStateException e) {
                    // Uncritical, might happen if JSP error page was used
                    if (DEBUG > 1) System.err.println("JspLoader.loadTemplate() ignoring IllegalStateException " + e);
                } catch (Exception e) {
                    System.err.println("Error in CmsJspLoader.loadTemplate() while writing buffer to final stream: " + e.toString());
                    if (DEBUG > 0) System.err.println(com.opencms.util.Utils.getStackTrace(e));
                    throw new CmsException("Error in CmsJspLoader.loadTemplate() while writing buffer to final stream for " + file.getAbsolutePath() + "\n" + e, CmsException.C_LAUNCH_ERROR, e);
                }        
            }
        }
        
        if (DEBUG > 0) {
            long timer2 = System.currentTimeMillis() - timer1;
            System.err.println("========== JspLoader (Template) time delivering JSP for " + file.getAbsolutePath() + ": " + timer2 + "ms");
        }        
        
        return result;
    }
    
    /**
     * Translates the JSP file name for a OpenCms VFS resourcn 
     * to the name used in the "real" file system.<p>
     * 
     * The name given must be a absolute URI in the OpenCms VFS,
     * e.g. CmsFile.getAbsolutePath()
     *
     * @param name The file to calculate the JSP name for
     * @return The JSP name for the file
     */    
    public static String getJspName(String name) {
        return name.replace('\\', 'T').replace('/', 'T') + '.' + name.hashCode() + C_JSP_EXTENSION;
    }
    
    /**
     * Returns the uri for a given JSP in the "real" file system, 
     * i.e. the path in the file
     * system relative to the web application directory.
     *
     * @param name The name of the JSP file 
     * @param online Flag to check if this is request is online or not
     * @return The full uri to the JSP
     */
    public static String getJspUri(String name, boolean online) {
        return m_jspWebAppRepository + (online?"/online/":"/offline/") + getJspName(name);  
    }
    
    /**
     * Returns the absolute path in the "real" file system for a given JSP.
     *
     * @param name The name of the JSP file 
     * @param online Flag to check if this is request is online or not
     * @return The full path to the JSP
     */
    public static String getJspPath(String name, boolean online) {
        return m_jspRepository + (online?"online":"offline") + File.separator + name;
    }

    /**
     * Returns the absolute path in the "real" file system for the JSP repository
     * toplevel directory.
     *
     * @return The full path to the JSP repository
     */
    public static String getJspRepository() {        
        return m_jspRepository;
    }    
    
    /**
     * Updates a JSP page in the "real" file system in case the VFS resource has changed.<p>
     * 
     * Also processes the <code>&lt;%@ cms %&gt;</code> tags before the JSP is written to the real FS.
     * Also recursivly updates all files that are referenced by a <code>&lt;%@ cms %&gt;</code> tag 
     * on this page to make sure the file actually exists in the real FS. 
     * All <code>&lt;%@ include %&gt;</code> tags are parsed and the name in the tag is translated
     * from the OpenCms VFS path to the path in the real FS. 
     * The same is done for filenames in <code>&lt;%@ page errorPage=... %&gt;</code> tags.
     * 
     * @param cms Used to access the OpenCms VFS
     * @param file The reqested JSP file resource in the VFS
     * @param req The current request
     * @param res The current response
     * @param updates A Set containing all JSP pages that have been already updated
     * 
     * @return The file name of the updated JSP in the "real" FS
     * 
     * @throws ServletException might be thrown in the process of including the JSP 
     * @throws IOException might be thrown in the process of including the JSP 
     */
    private synchronized String updateJsp(CmsObject cms, CmsResource file, CmsFlexRequest req, Set updates) 
    throws IOException, ServletException {
        
        String jspTargetName = getJspName(file.getAbsolutePath());
        String jspPath = getJspPath(jspTargetName, req.isOnline());
        
        File d = new File(jspPath).getParentFile();
        if (! (d != null) && (d.exists() && d.isDirectory() && d.canRead())) {
            if (I_CmsLogChannels.C_LOGGING && A_OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) 
                A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, "Could not access directory for " + jspPath);
            throw new ServletException("JspLoader: Could not access directory for " + jspPath);
        }    
        
        if (updates.contains(jspTargetName)) return null;
        updates.add(jspTargetName);
        
        boolean mustUpdate = false;
        
        File f = new File(jspPath);        
        if (!f.exists()) {
            // File does not exist in FS
            mustUpdate = true;
        } else if (f.lastModified() <= file.getDateLastModified()) {
            // File in FS is older then file in VFS
            mustUpdate = true;
        } else if (req.isDoRecompile()) {
            // Recompile is forced with parameter
            mustUpdate = true;
        }

        String jspfilename = getJspUri(file.getAbsolutePath(), req.isOnline());               
        
        if (mustUpdate) {
            if (DEBUG > 2) System.err.println("JspLoader writing new file: " + jspfilename);         
            byte[] contents = null;
            String jspEncoding = null;
            try {
                contents = req.getCmsObject().readFile(file.getAbsolutePath()).getContents();
                // Encoding project:
                // Check the JSP "content-encoding" property
                jspEncoding = cms.readProperty(file.getAbsolutePath(), I_CmsConstants.C_PROPERTY_CONTENT_ENCODING, true);
                if (jspEncoding == null) jspEncoding = C_DEFAULT_JSP_ENCODING;
                jspEncoding = jspEncoding.trim().toLowerCase();
            } catch (CmsException e) {
                throw new ServletException("JspLoader: Could not read contents for file '" + file.getAbsolutePath() + "'", e);
            }
            
            try {
                FileOutputStream fs = new FileOutputStream(f);                
                // Encoding project:
                // We need to use some encoding to convert bytes to String
                // corectly. Internally a JSP will always be stored in the 
                // system default encoding since they are just a variation of
                // the "plain" resource type.
                String page = new String(contents, A_OpenCms.getDefaultEncoding());
                StringBuffer buf = new StringBuffer(contents.length);

                int p0 = 0, i2 = 0, slen = C_DIRECTIVE_START.length(), elen = C_DIRECTIVE_END.length();
                // Check if any jsp name references occur in the file
                int i1 = page.indexOf(C_DIRECTIVE_START);
                while (i1 >= 0) {
                    // Parse the file and replace jsp name references 
                    i2 = page.indexOf(C_DIRECTIVE_END, i1 + slen);
                    if (i2 > i1) {
                        String directive = page.substring(i1 + slen, i2);
                        if (DEBUG > 2) System.err.println("JspLoader: Detected " + C_DIRECTIVE_START + directive + C_DIRECTIVE_END);

                        int t1=0, t2=0, t3=0, t4=0, t5=0, t6=slen, t7=0;
                        while (directive.charAt(t1) == ' ') t1++;
                        String filename = null;                        
                        if (directive.startsWith("include", t1)) {            
                            if (DEBUG > 2) System.err.println("JspLoader: Detected 'include' directive!");                            
                            t2 = directive.indexOf("file", t1 + 7);
                            t5 = 6;
                        } else if (directive.startsWith("page", t1)) {
                            if (DEBUG > 2) System.err.println("JspLoader: Detected 'page' directive!");                            
                            t2 = directive.indexOf("errorPage", t1 + 4);
                            t5 = 11;
                        } else if (directive.startsWith("cms", t1)) {
                            if (DEBUG > 2) System.err.println("JspLoader: Detected 'cms' directive!");                            
                            t2 = directive.indexOf("file", t1 + 3);
                            t5 = 4; t6 = 0; t7 = elen; 
                        }
                        
                        if (t2 > 0) {
                            String sub = directive.substring(t2 + t5); 
                            char c1 = sub.charAt(t3);
                            while ((c1 == ' ') || (c1 == '=') || (c1 == '"')) c1 = sub.charAt(++t3);
                            t4 = t3;
                            while (c1 != '"') c1 = sub.charAt(++t4);
                            if (t4 > t3) filename=sub.substring(t3,t4);
                            if (DEBUG > 2) System.err.println("JspLoader: File given in directive is: " + filename);                            
                        }
                        
                        if (filename != null) {
                            // a file was found, changes have to be made
                            String pre = ((t7 == 0)?directive.substring(0,t2+t3+t5):"");                            ;
                            String suf = ((t7 == 0)?directive.substring(t2+t3+t5+filename.length()):"");
                            // Now try to update the referenced file 
                            String absolute = req.toAbsolute(filename);
                            if (DEBUG > 2) System.err.println("JspLoader: Absolute location=" + absolute);
                            String jspname = null;
                            try {
                                // Make sure the jsp referenced file is generated
                                CmsResource jsp = cms.readFileHeader(absolute);
                                updateJsp(cms, jsp, req, updates);
                                jspname = getJspUri(jsp.getAbsolutePath(), req.isOnline());
                            } catch (Exception e) {
                                jspname = null;
                                if (DEBUG > 2) System.err.println("JspLoader: Error while creating jsp file " + absolute + "\n" + e);
                            }
                            if (jspname != null) {
                                // Only change something in case no error had occured
                                if (DEBUG > 2) System.err.println("JspLoader: Name of jsp file is " + jspname);
                                directive = pre + jspname + suf;
                                if (DEBUG > 2) System.err.println("JspLoader: Changed directive to " + C_DIRECTIVE_START + directive + C_DIRECTIVE_END);                                                     
                            }
                        }
                        
                        buf.append(page.substring(p0, i1 + t6));
                        buf.append(directive);
                        p0 = i2 + t7;
                        i1 = page.indexOf(C_DIRECTIVE_START, p0);
                    }
                }
                if (i2 > 0) {
                    buf.append(page.substring(p0, page.length()));
                    // Encoding project:
                    // Now we are ready to store String data in file system.
                    // To convert String to bytes we also need to provide
                    // some encoding. The default (by the JSP standard) encoding 
                    // for JSP is ISO-8859-1.
                    contents = buf.toString().getBytes(jspEncoding);
                } else {
                    // Encoding project:
                    // Contents of original file where not modified,
                    // just translate to the required JSP encoding (if necessary)
                    contents = Encoder.changeEncoding(contents, A_OpenCms.getDefaultEncoding(), jspEncoding);                    
                }                                         
                fs.write(contents);                
                fs.close();
                
                if (I_CmsLogChannels.C_LOGGING && A_OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_INFO)) 
                    A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INFO, "Updated JSP file \"" + jspfilename + "\" for resource \"" + file.getAbsolutePath() + "\"") ;
            } catch (FileNotFoundException e) {
                throw new ServletException("JspLauncher: Could not write to file '" + f.getName() + "'\n" + e, e);
            }
        }                      
        return jspfilename;
    }    
        
    /**
	 * Does the job of including the JSP, 
	 * this method should usually be called from a <code>CmsFlexRequestDispatcher</code> only.<p>
     * 
     * This method is called directly if the element is 
     * called as a sub-element from another I_CmsResourceLoader.<p>
	 * 
	 * One of the tricky issues is the correct cascading of the Exceptions, 
	 * so that you are able to identify the true origin of the problem.
	 * This ia achived by imprinting a String C_EXCEPTION_PREFIX to the 
	 * exception message.
	 * 
	 * @param cms Used to access the OpenCms VFS
	 * @param file The reqested JSP file resource in the VFS
	 * @param req The current request
	 * @param res The current response
     * 
     * @throws ServletException might be thrown in the process of including the JSP 
     * @throws IOException might be thrown in the process of including the JSP 
     * 
     * @see com.opencms.flex.cache.CmsFlexRequestDispatcher
	 */
	public void service(CmsObject cms, CmsResource file, CmsFlexRequest req, CmsFlexResponse res)
	throws ServletException, IOException {              
	    try {	
	        // Get JSP target name on "real" file system
	        String target = updateJsp(cms, file, req, new HashSet(11));               
	        // Important: Indicate that all output must be buffered
	        res.setOnlyBuffering(true);   
	        // Dispatch to external file
	        req.getCmsRequestDispatcher(file.getAbsolutePath(), target).include(req, res);  	        
	    } catch (ServletException e) {          
	        // Check if this Exception has already been marked
	        String msg = e.getMessage();
	        if (DEBUG > 1) System.err.println("JspLauncher: Caught ServletException " + e );
	        if ((msg != null) && msg.startsWith(C_LOADER_EXCEPTION_PREFIX)) throw e;
	        // Not marked, imprint current JSP file and stack trace
	        throw new ServletException(C_LOADER_EXCEPTION_PREFIX + " '" + file.getAbsolutePath() + "'\n\nRoot cause:\n" + Utils.getStackTrace(e) + "\n--------------- End of root cause.\n", e);           
	    } catch (Exception e) {
	        // Imprint current JSP file and stack trace
	        throw new ServletException(C_LOADER_EXCEPTION_PREFIX + " '" + file.getAbsolutePath() + "'\n\nRoot cause:\n" + Utils.getStackTrace(e) + "\n--------------- End of root cause.\n", e);          
	    }
	} 
}
