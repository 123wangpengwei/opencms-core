/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/template/Attic/CmsXmlTemplate.java,v $
* Date   : $Date: 2001/11/02 08:50:37 $
* Version: $Revision: 1.83 $
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


package com.opencms.template;

import java.util.*;
import java.io.*;
import com.opencms.launcher.*;
import com.opencms.file.*;
import com.opencms.util.*;
import com.opencms.defaults.*;
import com.opencms.core.*;
import com.opencms.template.cache.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.servlet.http.*;

/**
 * Template class for displaying the processed contents of hierachical XML template files
 * that can include other subtemplates.
 *
 * @author Alexander Lucas
 * @version $Revision: 1.83 $ $Date: 2001/11/02 08:50:37 $
 */
public class CmsXmlTemplate extends A_CmsTemplate implements I_CmsXmlTemplate {
    public static final String C_FRAME_SELECTOR = "cmsframe";

    /** name of the special body element */
    public final static String C_BODY_ELEMENT = "body";

    /** Boolean for additional debug output control */
    public final static boolean C_DEBUG = false;

    /** Error string to be inserted for corrupt subtemplates for guest user requests. */
    private final static String C_ERRORTEXT = "ERROR!";

    /**
     * Template cache for storing cacheable results of the subtemplates.
     */
    protected static com.opencms.launcher.I_CmsTemplateCache m_cache = null;

    /**
     * For debugging purposes only.
     * Counts the number of re-uses od the instance of this class.
     */
    private int counter = 0;

    /**
     * For debugging purposes only.
     * Increments the class variable <code>counter</code> and
     * prints out its new value..
     * <P>
     * May be called from the template file using
     * <code>&lt;METHOD name="counter"&gt;</code>.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object the initiating XLM document.
     * @param userObj Hashtable with parameters.
     * @return Actual value of <code>counter</code>.
     */
    public Integer counter(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws CmsException {
        counter++;
        return new Integer(counter);
    }

    /**
     * Help method to print nice classnames in error messages
     * @return class name in [ClassName] format
     */
    protected String getClassName() {
        String name = getClass().getName();
        return "[" + name.substring(name.lastIndexOf(".") + 1) + "] ";
    }

    /**
     * Gets the content of a given template file and its subtemplates
     * with the given parameters. The default section in the template file
     * will be used.
     * <P>
     * Parameters are stored in a hashtable and can derive from
     * <UL>
     * <LI>Template file of the parent template</LI>
     * <LI>Body file clicked by the user</LI>
     * <LI>URL parameters</LI>
     * </UL>
     * Paramter names must be in "elementName.parameterName" format.
     *
     * @param cms CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file
     * @param elementName Element name of this template in our parent template
     * @param parameters Hashtable with all template class parameters.
     * @return Content of the template and all subtemplates.
     * @exception CmsException
     */
    public byte[] getContent(CmsObject cms, String templateFile, String elementName, Hashtable parameters) throws CmsException {
        return getContent(cms, templateFile, elementName, parameters, null);
    }

    /**
     * Gets the content of a defined section in a given template file and its subtemplates
     * with the given parameters.
     *
     * @see getContent(CmsObject cms, String templateFile, String elementName, Hashtable parameters)
     * @param cms CmsObject Object for accessing system resources.
     * @param templateFile Filename of the template file.
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     */
    public byte[] getContent(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() && C_DEBUG ) {
            A_OpenCms.log(C_OPENCMS_DEBUG, "[CmsXmlTemplate] getting content of element " + ((elementName == null) ? "<root>" : elementName));
            A_OpenCms.log(C_OPENCMS_DEBUG, "[CmsXmlTemplate] template file is: " + templateFile);
            A_OpenCms.log(C_OPENCMS_DEBUG, "[CmsXmlTemplate] selected template section is: " + ((templateSelector == null) ? "<default>" : templateSelector));
        }
        CmsXmlTemplateFile xmlTemplateDocument = getOwnTemplateFile(cms, templateFile, elementName, parameters, templateSelector);
        if(templateSelector == null || "".equals(templateSelector)) {
            templateSelector = (String)parameters.get(C_FRAME_SELECTOR);
        }
        return startProcessing(cms, xmlTemplateDocument, elementName, parameters, templateSelector);
    }

    /**
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document.
     * @param userObj Hashtable with parameters.
     * @return String or byte[] with the content of this subelement.
     * @exception CmsException
     */
    public Object getFileUri(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws CmsException {
        return cms.getRequestContext().getFileUri().getBytes();
    }

    /**
     * Returns the absolute path of a resource merged with the absolute path of the file and
     * the relative path in the tagcontent. This path is a intern OpenCms path (i.e. it starts
     * with a "/" ).
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent The relative path of the resource incl. name of the resource.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document.
     * @param userObj Hashtable with parameters.
     * @return String or byte[] with the content of this subelement.
     * @exception CmsException
     */
    public Object mergeAbsolutePath(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws CmsException {
        return Utils.mergeAbsolutePath(doc.getAbsoluteFilename(), tagcontent).getBytes();
    }

    /**
     * Returns the absolute path of a resource merged with the absolute path of the file and
     * the relative path in the tagcontent. This method adds the servlet path at the beginning
     * of the path.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent The relative path of the resource incl. name of the resource.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document.
     * @param userObj Hashtable with parameters.
     * @return String or byte[] with the content of this subelement.
     * @exception CmsException
     */
    public Object mergeAbsoluteUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws CmsException {
        String ocPath = new String((byte[])mergeAbsolutePath(cms,tagcontent, doc, userObject));
        String servletPath = cms.getRequestContext().getRequest().getServletUrl();
        return (servletPath + ocPath).getBytes();
    }
    /**
     * Gets the QueryString for CmsFrameTemplates.
     * <P>
     * This method can be called using <code>&lt;METHOD name="getCmsQueryString"&gt;</code>
     * in the template file.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document.
     * @param userObj Hashtable with parameters.
     * @return String or byte[] with the content of this subelement.
     * @exception CmsException
     */
    public Object getFrameQueryString(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws CmsException {
        String query = ((HttpServletRequest)cms.getRequestContext().getRequest().getOriginalRequest()).getQueryString();
        String frame = "", param = "";
        if(!tagcontent.equals("")) {
            if(!tagcontent.startsWith("&")) {
                if(tagcontent.indexOf(",") != -1) {
                    frame = tagcontent.substring(0, tagcontent.indexOf(","));
                    param = tagcontent.substring(tagcontent.indexOf(",") + 1);
                }
                else {
                    frame = tagcontent;
                }
            }
            else {
                param = tagcontent;
            }
        }
        StringBuffer encQuery = new StringBuffer();
        boolean notfirst = false;
        if(query != null) {

            // Fine. A lasturl parameter was found in session or parameter hashtable.
            // Check, if the URL parameters of the last url have to be encoded.
            int asteriskIdx = query.indexOf("?");
            if(asteriskIdx > -1 && (asteriskIdx < (query.length() - 1))) {

                // In fact, there are URL parameters
                encQuery.append(query.substring(0, asteriskIdx + 1));
                String queryString = query.substring(asteriskIdx + 1);
                StringTokenizer st = new StringTokenizer(queryString, "&");
                while(st.hasMoreTokens()) {

                    // Loop through all URL parameters
                    String currToken = st.nextToken();
                    if(currToken != null && !"".equals(currToken)) {

                        // Look for the "=" character to divide parameter name and value
                        int idx = currToken.indexOf("=");
                        if(notfirst) {
                            encQuery.append("&");
                        }
                        else {
                            notfirst = true;
                        }
                        if(idx > -1) {
                            // A parameter name/value pair was found.
                            // Encode the parameter value and write back!
                            String key = currToken.substring(0, idx);
                            String value = (idx < (currToken.length() - 1)) ? currToken.substring(idx + 1) : "";
                            encQuery.append(key);
                            encQuery.append("=");
                            encQuery.append(Encoder.escape(value));
                        }
                        else {
                            // Something strange happened.
                            // Maybe a parameter without "=" ?
                            // Write back without encoding!
                            encQuery.append(currToken);
                        }
                    }
                }
                query = encQuery.toString();
            }
        }
        query = (query == null ? "" : query);
        if(!query.equals("")) {
            if(query.indexOf("cmsframe=") != -1) {
                int start = query.indexOf("cmsframe=");
                int end = query.indexOf("&", start);
                String cmsframe = "";
                if(end != -1) {
                    cmsframe = query.substring(start + 9, end);
                }
                else {
                    cmsframe = query.substring(start + 9);
                }
                if(!cmsframe.equals("plain")) {
                    if(!frame.equals("")) {
                        if(end != -1) {
                            query = query.substring(0, start + 9) + frame + query.substring(end);
                        }
                        else {
                            query = query.substring(0, start + 9) + frame;
                        }
                    }
                    else {
                        if(end != -1) {
                            query = query.substring(0, start) + query.substring(end + 1);
                        }
                        else {
                            query = query.substring(0, start);
                        }
                    }
                }
            }
            else {
                if(!tagcontent.equals("")) {
                    query = query + "&cmsframe=" + frame;
                }
            }
            if(!query.equals("")) {
                query = "?" + query;
            }
        }
        else {
            if(!frame.equals("")) {
                query = "?cmsframe=" + frame;
            }
        }
        if(!query.equals("")) {
            query = query + param;
        }
        else {
            query = "?" + param.substring(param.indexOf("&") + 1);
        }
        if (query.trim().equals("?") || query.trim().equals("&") || query.trim().equals("?&")) {
            query="";
        }
        return query;
    }

    /**
     * Gets the target for a link.
     * <P>
     * This method can be called using <code>&lt;METHOD name="getCmsFrame"&gt;</code>
     * in the template file.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document.
     * @param userObj Hashtable with parameters.
     * @return String or byte[] with the content of this subelement.
     * @exception CmsException
     */
    public Object getFrameTarget(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws CmsException {
        String target = "";
        String cmsframe = (String)((Hashtable)userObject).get("cmsframe");
        cmsframe = (cmsframe == null ? "" : cmsframe);
        if(cmsframe.equals("plain")) {
            target = "";
        }
        else {
            if(tagcontent.equals("")) {
                target = "target=_top";
            }
            else {
                target = "target=" + tagcontent;
            }
        }
        return target;
    }

    /**
     * Gets the key that should be used to cache the results of
     * <EM>this</EM> template class.
     * <P>
     * Since our results may depend on the used template file,
     * the parameters and the requested body document, we must
     * build a complex key using this three arguments.
     *
     * @param cms CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return key that can be used for caching
     */
    public Object getKey(CmsObject cms, String templateFile, Hashtable parameters, String templateSelector) {

        //Vector v = new Vector();
        CmsRequestContext reqContext = cms.getRequestContext();

        //v.addElement(reqContext.currentProject().getName());
        //v.addElement(reqContext.getUri());
        //v.addElement(templateFile);
        //v.addElement(parameters);
        //v.addElement(templateSelector);
        //return v;
        String result = "" + reqContext.currentProject().getId() + ":" + reqContext.currentUser().getName() + reqContext.getUri() + templateFile;
        Enumeration keys = parameters.keys();
        while(keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            result = result + key + parameters.get(key);
        }
        result = result + templateSelector;
        return result;
    }

    /**
     * Reads in the template file and starts the XML parser for the expected
     * content type.
     * <P>
     * Every extending class using not CmsXmlTemplateFile as content type,
     * but any derived type should override this method.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param templateFile Filename of the template file.
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     */
    public CmsXmlTemplateFile getOwnTemplateFile(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        CmsXmlTemplateFile xmlTemplateDocument = new CmsXmlTemplateFile(cms, templateFile);
        return xmlTemplateDocument;
    }

    /**
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document.
     * @param userObj Hashtable with parameters.
     * @return String or byte[] with the content of this subelement.
     * @exception CmsException
     */
    public Object getPathUri(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws CmsException {
        String path = cms.getRequestContext().getUri();
        path = path.substring(0, path.lastIndexOf("/") + 1);
        path = cms.getRequestContext().getRequest().getServletUrl() + path;
        return path.getBytes();
    }

    /**
     * Inserts the correct servlet path title into the template.
     * <P>
     * This method can be called using <code>&lt;METHOD name="getTitle"&gt;</code>
     * in the template file.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document.
     * @param userObj Hashtable with parameters.
     * @return String or byte[] with the content of this subelement.
     * @exception CmsException
     */
    public Object getQueryString(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws CmsException {
        String query = ((HttpServletRequest)cms.getRequestContext().getRequest().getOriginalRequest()).getQueryString();
        if(query != null && !"".equals(query)) {
            query = "?" + query;
        }
        return query;
    }

    /**
     * Get the IP address of the current request.
     * <P>
     * This method can be called using <code>&lt;METHOD name="getRequestIp"&gt;</code>
     * in the template file.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document.
     * @param userObj Hashtable with parameters.
     * @return String or byte[] with the content of this subelement.
     * @exception CmsException
     */
    public String getRequestIp(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws CmsException {
        return ((HttpServletRequest)cms.getRequestContext().getRequest().getOriginalRequest()).getRemoteAddr();
    }

    /**
     * Inserts the correct servlet path title into the template.
     * <P>
     * This method can be called using <code>&lt;METHOD name="getTitle"&gt;</code>
     * in the template file.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document.
     * @param userObj Hashtable with parameters.
     * @return String or byte[] with the content of this subelement.
     * @exception CmsException
     */
    public Object getServletPath(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws CmsException {
        return cms.getRequestContext().getRequest().getServletUrl() + "/";
    }

    /**
     * Get the session id. If no session exists, a new one will be created.
     * <P>
     * This method can be called using <code>&lt;METHOD name="getSessionId"&gt;</code>
     * in the template file.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document.
     * @param userObj Hashtable with parameters.
     * @return String or byte[] with the content of this subelement.
     * @exception CmsException
     */
    public String getSessionId(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws CmsException {
        return ((HttpServletRequest)cms.getRequestContext().getRequest().getOriginalRequest()).getSession(true).getId();
    }

    /**
     * Inserts the correct stylesheet into the layout template.
     * <P>
     * This method can be called using <code>&lt;METHOD name="getStylesheet"&gt;</code>
     * in the template file.
     * <P>
     * When using this method follwing parameters should be defined
     * either in the page file or in the template file:
     * <ul>
     * <li><code>root.stylesheet-ie</code></li>
     * <li><code>root.stylesheet-ns</code></li>
     * </ul>
     * These parameters should contain the correct OpenCms path
     * for the Internet Explorer and Netscape Navigate
     * specific stylesheets.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document.
     * @param userObj Hashtable with parameters.
     * @return String or byte[] with the content of this subelement.
     * @exception CmsException
     */
    public String getStylesheet(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws CmsException {
        CmsXmlTemplateFile tempTemplateFile = (CmsXmlTemplateFile)doc;

        // Get the XML parsed content of the frametemplate file.
        // This can be done by calling the getOwnTemplateFile() method of the
        // mastertemplate class.
        // The content is needed to determine the HTML style of the body element.
        Object tempObj = CmsTemplateClassManager.getClassInstance(cms, tempTemplateFile.getSubtemplateClass("frametemplate"));
        CmsXmlTemplate frameTemplateClassObject = (CmsXmlTemplate)tempObj;
        CmsXmlTemplateFile templateFile = frameTemplateClassObject.getOwnTemplateFile(cms, tempTemplateFile.getSubtemplateFilename("frametemplate"), null, null, null);


        // Get the styles from the parameter hashtable
        String styleIE = null;
        String styleNS = null;
        if(templateFile.hasData("stylesheet-ie")) {
            styleIE = templateFile.getDataValue("stylesheet-ie");
        }
        else {
            if(templateFile.hasData("stylesheet")) {
                styleIE = templateFile.getDataValue("stylesheet");
            }
            else {
                styleIE = "";
            }
        }
        if(templateFile.hasData("stylesheet-ns")) {
            styleNS = templateFile.getDataValue("stylesheet-ns");
        }
        else {
            if(templateFile.hasData("stylesheet")) {
                styleNS = templateFile.getDataValue("stylesheet");
            }
            else {
                styleNS = "";
            }
        }
        HttpServletRequest orgReq = (HttpServletRequest)cms.getRequestContext().getRequest().getOriginalRequest();
        String servletPath = cms.getRequestContext().getRequest().getServletUrl() + "/";

        // Get the user's browser
        String browser = orgReq.getHeader("user-agent");
        if(browser == null) {

            // the browser is unknown - return the ns-style
            return styleNS;
        }
        if(browser.indexOf("MSIE") > -1) {
            return servletPath + styleIE;
        }
        else {
            return servletPath + styleNS;
        }
    }

    /**
     * Find the corresponding template class to be loaded.
     * this should be defined in the template file of the parent
     * template and can be overwritten in the body file.
     *
     * @param elementName Element name of this template in our parent template.
     * @param doc CmsXmlTemplateFile object of our template file including a subtemplate.
     * @param parameters Hashtable with all template class parameters.
     * @return Name of the class that should generate the output for the included template file.
     */
    protected String getTemplateClassName(String elementName, CmsXmlTemplateFile doc, Hashtable parameters) throws CmsException {
        String result = null;
        if(parameters.containsKey(elementName + "._CLASS_")) {
            result = (String)parameters.get(elementName + "._CLASS_");
        }else {
            if(doc.hasSubtemplateClass(elementName)) {
                result = doc.getSubtemplateClass(elementName);
            }else {

                // Fallback to "body" element
                if(parameters.containsKey("body._CLASS_")) {
                    result = (String)parameters.get("body._CLASS_");
                }
            }
        }
        if(result == null){
            CmsElementDefinitionCollection elDefs = (CmsElementDefinitionCollection)parameters.get("_ELDEFS_");
            if(elDefs != null){
                CmsElementDefinition elDef = elDefs.get(elementName);
                if(elDef != null){
                    result = elDef.getClassName();
                }
            }
        }
        return result;
    }

    /**
     * Find the corresponding template file to be loaded by the template class.
     * this should be defined in the template file of the parent
     * template and can be overwritten in the body file.
     *
     * @param elementName Element name of this template in our parent template.
     * @param doc CmsXmlTemplateFile object of our template file including a subtemplate.
     * @param parameters Hashtable with all template class parameters.
     * @return Name of the template file that should be included.
     */
    protected String getTemplateFileName(String elementName, CmsXmlTemplateFile doc, Hashtable parameters) throws CmsException {
        String result = null;
        if(parameters.containsKey(elementName + "._TEMPLATE_")) {
            result = (String)parameters.get(elementName + "._TEMPLATE_");
        }else {
            if(doc.hasSubtemplateFilename(elementName)) {
                result = doc.getSubtemplateFilename(elementName);
            }else {
                // Fallback to "body" element
                if(parameters.containsKey("body._TEMPLATE_")) {
                    result = (String)parameters.get("body._TEMPLATE_");
                }
            }
        }
        if(result == null){
            CmsElementDefinitionCollection elDefs = (CmsElementDefinitionCollection)parameters.get("_ELDEFS_");
            if(elDefs != null){
                CmsElementDefinition elDef = elDefs.get(elementName);
                if(elDef != null){
                    result = elDef.getTemplateName();
                }
            }
        }
        return result;
    }

    /**
     * Find the corresponding template selector to be activated.
     * This may be defined in the template file of the parent
     * template and can be overwritten in the body file.
     *
     * @param elementName Element name of this template in our parent template.
     * @param doc CmsXmlTemplateFile object of our template file including a subtemplate.
     * @param parameters Hashtable with all template class parameters.
     * @return Name of the class that should generate the output for the included template file.
     */
    protected String getTemplateSelector(String elementName, CmsXmlTemplateFile doc, Hashtable parameters) throws CmsException {
        if(parameters.containsKey(elementName + "._TEMPLATESELECTOR_")) {
            return (String)parameters.get(elementName + "._TEMPLATESELECTOR_");
        }
        else {
            if(doc.hasSubtemplateSelector(elementName)) {
                return doc.getSubtemplateSelector(elementName);
            }
            else {
                CmsElementDefinitionCollection elDefs = (CmsElementDefinitionCollection)parameters.get("_ELDEFS_");
                if(elDefs != null){
                    CmsElementDefinition elDef = elDefs.get(elementName);
                    if(elDef != null){
                        return elDef.getTemplateSelector();
                    }
                }
                return null;
            }
        }
    }

    /**
     * Inserts the correct document title into the template.
     * <P>
     * This method can be called using <code>&lt;METHOD name="getTitle"&gt;</code>
     * in the template file.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document.
     * @param userObj Hashtable with parameters.
     * @return String or byte[] with the content of this subelement.
     * @exception CmsException
     */
    public Object getTitle(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws CmsException {
        String requestedUri = cms.getRequestContext().getUri();
        String title = cms.readProperty(requestedUri, C_PROPERTY_TITLE);
        if(title == null) {
            title = "";
        }
        return title;
    }

    /**
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document.
     * @param userObj Hashtable with parameters.
     * @return String or byte[] with the content of this subelement.
     * @exception CmsException
     */
    public Object getUri(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws CmsException {
        return (cms.getRequestContext().getRequest().getServletUrl() + cms.getRequestContext().getUri()).getBytes();
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
     * Complex classes that are able top include other subtemplates
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
        return true;
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

        // Frist build our own cache directives.
        boolean isCacheable = isCacheable(cms, templateFile, elementName, parameters, templateSelector);
        boolean isProxyPrivateCacheable = isProxyPrivateCacheable(cms, templateFile, elementName, parameters, templateSelector);
        boolean isProxyPublicCacheable = isProxyPublicCacheable(cms, templateFile, elementName, parameters, templateSelector);
        boolean isExportable = isExportable(cms, templateFile, elementName, parameters, templateSelector);
        boolean isStreamable = isStreamable(cms, templateFile, elementName, parameters, templateSelector);
        CmsCacheDirectives result = new CmsCacheDirectives(isCacheable, isProxyPrivateCacheable, isProxyPublicCacheable, isExportable, isStreamable);

        // Collect all subelements of this page
        CmsXmlTemplateFile doc = null;
        Vector subtemplates = null;
        try {
            doc = this.getOwnTemplateFile(cms, templateFile, elementName, parameters, templateSelector);
            doc.init(cms, templateFile);
            subtemplates = doc.getAllSubElements();

            // Loop through all subelements and get their cache directives
            int numSubtemplates = subtemplates.size();
            for(int i = 0;i < numSubtemplates;i++) {
                String elName = (String)subtemplates.elementAt(i);
                String className = null;
                String templateName = null;

                className = getTemplateClassName(elName, doc, parameters);
                templateName = getTemplateFileName(elName, doc, parameters);

                if(className != null) {
                    I_CmsTemplate templClass = (I_CmsTemplate)CmsTemplateClassManager.getClassInstance(cms, className);
                    CmsCacheDirectives cd2 = templClass.collectCacheDirectives(cms, templateName, elName, parameters, null);
                    /*System.err.println("*                INT PUB PRV EXP STR");
                    debugPrint(elementName, result.m_cd);
                    System.err.println(" ");
                    debugPrint(elName, cd2.m_cd);
                    System.err.println(" " + templClass.getClass());
                    System.err.println("*                -------------------");*/

                    //result.merge(templClass.collectCacheDirectives(cms, templateName, elName, parameters, null));
                    result.merge(cd2);
                    /*debugPrint(elementName, result.m_cd);
                    System.err.println(" ");
                    System.err.println("* ");*/
                } else {
                    // This template file includes a subelement not exactly defined.
                    // The name of it's template class is missing at the moment, so
                    // we cannot say anything about the cacheablility.
                    // Set it to false.
                    return new CmsCacheDirectives(false);
                }
            }
        }
        catch(CmsException e) {
            if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(C_OPENCMS_INFO, getClassName() + "Cannot determine cache directives for my template file " + templateFile + " (" + e + "). ");
                A_OpenCms.log(C_OPENCMS_INFO, getClassName() + "Resuming normal operation, setting cacheability to false.");
                return new CmsCacheDirectives(false);
            }
        }
        return result;

    }

    /**
     * gets the caching information from the current template class.
     *
     * @param cms CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return <EM>true</EM> if this class may stream it's results, <EM>false</EM> otherwise.
     */
    public CmsCacheDirectives getCacheDirectives(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {

        // First build our own cache directives.
        CmsCacheDirectives result = new CmsCacheDirectives(true);
        Vector para = new Vector();
        para.add("cmsframe");
        result.setCacheParameters(para);
        return result;
    }

    /**
     *  gets the caching information for a specific methode.
     *  @param cms the cms object.
     *  @param methodName the name of the method for witch the MethodCacheDirectives are wanted.
     */
    public CmsMethodCacheDirectives getMethodCacheDirectives(CmsObject cms, String methodName){
        if("getTitle".equals(methodName) || "getUri".equals(methodName)
                                        ||"getFileUri".equals(methodName)
                                        ||"getPathUri".equals(methodName)){
            CmsMethodCacheDirectives mcd = new CmsMethodCacheDirectives(true);
            mcd.setCacheUri(true);
            return mcd;
        }
        if ("getFrameQueryString".equals(methodName)
                                || "getQueryString".equals(methodName)
                                || "getRequestIp".equals(methodName)
                                || "getSessionId".equals(methodName)
                                || "getStylesheet".equals(methodName)){
            return new CmsMethodCacheDirectives(false);
        }
        return null;
    }

    /**
     * Tests, if the template cache is setted.
     * @return <code>true</code> if setted, <code>false</code> otherwise.
     */
    public final boolean isTemplateCacheSet() {
        return m_cache != null;
    }

    /**
     * For debugging purposes only.
     * Prints out all parameters.
     * <P>
     * May be called from the template file using
     * <code>&lt;METHOD name="parameters"&gt;</code>.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object the initiating XLM document.
     * @param userObj Hashtable with parameters.
     * @return Debugging information about all parameters.
     */
    public String parameters(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) {
        Hashtable param = (Hashtable)userObject;
        Enumeration keys = param.keys();
        String s = "";
        while(keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            s = s + "<B>" + key + "</B>: " + param.get(key) + "<BR>";
        }
        s = s + "<B>" + tagcontent + "</B><BR>";
        return s;
    }

    /**
     * Set the instance of template cache that should be used to store
     * cacheable results of the subtemplates.
     * If the template cache is not set, caching will be disabled.
     * @param c Template cache to be used.
     */
    public final void setTemplateCache(I_CmsTemplateCache c) {
        m_cache = c;
    }

    /**
     * Indicates if a previous cached result should be reloaded.
     * <P>
     * <em>not implemented.</em> Returns always <code>false</code>.
     *
     * @param cms CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return <code>false</code>
     */
    public boolean shouldReload(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
        return false;
    }

    /**
     * Saves the dependencies for this elementvariante.
     * We save the deps two ways.
     * First we have an so called extern Hashtable where we use as key a resource
     * (represented by a String) and as values an Vector with all ElementVariants
     * that depend on this resource (again represented by a String in which we save
     * the variant and the element it is in)
     * The second saveplace is the elementvariant itselv. The intern way to save.
     * There we store which resoucess affect this variant.
     *
     * @param cms The cms object.
     * @param templateName.
     * @param elementName only needed for getCachDirectives, if it is not used there it may be null
     * @param templateSelector only needed for getCachDirectives, if it is not used there it may be null
     * @param parameters.
     * @param vfsDeps A vector (of CmsResource objects) with the resources that variant depends on.
     * @param cosDeps A vector (of CmsContentDefinitions) with the cd-resources that variant depends on.
     * @param cosClassDeps A vector (of Class objects) with the contentdefinitions that variant depends on.
     */
    protected void registerVariantDeps(CmsObject cms, String templateName, String elementName,
                        String templateSelector, Hashtable parameters, Vector vfsDeps,
                        Vector cosDeps, Vector cosClassDeps) throws CmsException {

        String cacheKey = getCacheDirectives(cms, templateName, elementName,
                                parameters, templateSelector).getCacheKey(cms, parameters);
        if(cms.getRequestContext().isElementCacheEnabled() && (cacheKey != null) &&
                (cms.getRequestContext().currentProject().equals(cms.onlineProject()) )) {
            Hashtable externVarDeps = cms.getVariantDependencies();
            long exTimeForVariant = Long.MAX_VALUE;
            long now = System.currentTimeMillis();
            // this will be the entry for the extern hashtable
            String variantEntry = getClass().getName() + "|"+ templateName +"|"+ cacheKey;

            // the vector for the intern variant store. it contains the keys for the extern Hashtable
            Vector allDeps = new Vector();
            // first the dependencies for the cos system
            if(cosDeps != null){
                for (int i = 0; i < cosDeps.size(); i++){
                    A_CmsContentDefinition contentDef = (A_CmsContentDefinition)cosDeps.elementAt(i);
                    String key = cms.getSiteName() + cms.C_ROOTNAME_COS + "/"+contentDef.getClass().getName() +"/"+contentDef.getUniqueId(cms);
                    allDeps.add(key);
                    if(contentDef.isTimedContent()){
                        long time = ((I_CmsTimedContentDefinition)cosDeps.elementAt(i)).getPublicationDate();
                        if (time > now && time < exTimeForVariant){
                            exTimeForVariant = time;
                        }
                        time = ((I_CmsTimedContentDefinition)cosDeps.elementAt(i)).getPurgeDate();
                        if (time > now && time < exTimeForVariant){
                            exTimeForVariant = time;
                        }
                    }
                }
            }
            // now for the Classes
            if(cosClassDeps != null){
                for(int i=0; i<cosClassDeps.size(); i++){
                    String key = cms.getSiteName() + cms.C_ROOTNAME_COS + "/" + ((Class)cosClassDeps.elementAt(i)).getName() +"/";
                    allDeps.add(key);
                }
            }
            // now for the vfs
            if(vfsDeps != null){
                for(int i = 0; i < vfsDeps.size(); i++){
                    allDeps.add(((CmsResource)vfsDeps.elementAt(i)).getResourceName());
                }
            }
            // now put them all in the extern store
            for(int i=0; i<allDeps.size(); i++){
                String key = (String)allDeps.elementAt(i);
                Vector variantsForDep = (Vector)externVarDeps.get(key);
                if (variantsForDep == null){
                    variantsForDep = new Vector();
                }
                if(!variantsForDep.contains(variantEntry)){
                    variantsForDep.add(variantEntry);
                }
                externVarDeps.put(key, variantsForDep);
            }
            // at last we have to fill the intern store. that means we have to
            // put the alldeps vector in our variant that will be created later
            // in the startproccessing method.
            // Get current element.
            CmsElementCache elementCache = cms.getRequestContext().getElementCache();
            CmsElementDescriptor elKey = new CmsElementDescriptor(getClass().getName(), templateName);
            A_CmsElement currElem = elementCache.getElementLocator().get(cms, elKey, parameters);
            // add an empty variant with the vector to the element
            CmsElementVariant emptyVar = new CmsElementVariant();
            emptyVar.addDependencies(allDeps);
            if(exTimeForVariant < Long.MAX_VALUE ){
                emptyVar.mergeNextTimeout(exTimeForVariant);
            }
            Vector removedVar = currElem.addVariant(cacheKey, emptyVar);
            if((removedVar != null) ){
                // adding a new variant deleted this variant so we have to update the extern store
                String key = (String)removedVar.firstElement();
                CmsElementVariant oldVar = (CmsElementVariant)removedVar.lastElement();
                Vector oldVarDeps = oldVar.getDependencies();
                if (oldVarDeps != null){
                    String oldVariantEntry = getClass().getName() + "|"+ templateName +"|"+ key;
                    for(int i=0; i<oldVarDeps.size(); i++){
                        Vector externEntrys = (Vector)externVarDeps.get(oldVarDeps.elementAt(i));
                        if(externEntrys != null){
                            externEntrys.removeElement(oldVariantEntry);
                        }
                    }
                }
            }
            // mark this element so it wont be deleted without updating the extern store
            currElem.thisElementHasDepVariants();

        }
    }

    /**
     * Starts the processing of the given template file by calling the
     * <code>getProcessedTemplateContent()</code> method of the content defintition
     * of the corresponding content type.
     * <P>
     * Any exceptions thrown while processing the template will be caught,
     * printed and and thrown again.
     * <P>
     * If element cache is enabled, <code>generateElementCacheVariant()</code>
     * will be called instead of <code>getProcessedTemplateContent()</code> for
     * generating a new element cache variant instead of the completely
     * processed output data.
     * This new variant will be stored in the current element using the cache key
     * given by the cache directives.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param xmlTemplateDocument XML parsed document of the content type "XML template file" or
     * any derived content type.
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return Content of the template and all subtemplates.
     * @exception CmsException
     */
    protected byte[] startProcessing(CmsObject cms, CmsXmlTemplateFile xmlTemplateDocument, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        byte[] result = null;

        if(cms.getRequestContext().isElementCacheEnabled()) {
            CmsElementDefinitionCollection mergedElDefs = (CmsElementDefinitionCollection)parameters.get("_ELDEFS_");
            // We are in element cache mode. Create a new variant instead of a completely processed subtemplate
            CmsElementVariant variant = xmlTemplateDocument.generateElementCacheVariant(this, parameters, elementName, templateSelector);
            // Get current element.
            CmsElementCache elementCache = cms.getRequestContext().getElementCache();
            CmsElementDescriptor elKey = new CmsElementDescriptor(getClass().getName(), xmlTemplateDocument.getAbsoluteFilename());
            A_CmsElement currElem = elementCache.getElementLocator().get(cms, elKey, parameters);

            // If this elemement is cacheable, store the new variant
            if(currElem.getCacheDirectives().isInternalCacheable()) {
                //currElem.addVariant(getKey(cms, xmlTemplateDocument.getAbsoluteFilename(), parameters, templateSelector), variant);
                Vector removedVar = currElem.addVariant(currElem.getCacheDirectives().getCacheKey(cms, parameters), variant);
                if((removedVar != null) && currElem.hasDependenciesVariants()){
                    // adding a new variant deleted this variant so we have to update the extern dependencies store
                    String key = (String)removedVar.firstElement();
                    CmsElementVariant oldVar = (CmsElementVariant)removedVar.lastElement();
                    Vector oldVarDeps = oldVar.getDependencies();
                    if (oldVarDeps != null){
                        String oldVariantEntry = getClass().getName() + "|"+ xmlTemplateDocument.getAbsoluteFilename() +"|"+ key;
                        for(int i=0; i<oldVarDeps.size(); i++){
                            Vector externEntrys = (Vector)cms.getVariantDependencies().get(oldVarDeps.elementAt(i));
                            if(externEntrys != null){
                                externEntrys.removeElement(oldVariantEntry);
                            }
                        }
                    }
                }
            }
            result = ((CmsElementXml)currElem).resolveVariant(cms, variant, elementCache, mergedElDefs, elementName, parameters);
        } else {
            // Classic way. Element cache is not activated, so let's genereate the template as usual
            // Try to process the template file
            try {
                result = xmlTemplateDocument.getProcessedTemplateContent(this, parameters, templateSelector).getBytes();
            }
            catch(Throwable e) {
                // There were errors while generating output for this template.
                // Clear HTML cache and then throw exception again
                xmlTemplateDocument.removeFromFileCache();
                if(isCacheable(cms, xmlTemplateDocument.getAbsoluteFilename(), elementName, parameters, templateSelector)) {
                    m_cache.clearCache(getKey(cms, xmlTemplateDocument.getAbsoluteFilename(), parameters, templateSelector));
                }
                if(e instanceof CmsException) {
                    throw (CmsException)e;
                }
                else {
                    // under normal cirumstances, this should not happen.
                    // any exception should be caught earlier and replaced by
                    // corresponding CmsExceptions.
                    String errorMessage = "Exception while getting content for (sub)template " + elementName + ". " + e;
                    if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                        A_OpenCms.log(C_OPENCMS_CRITICAL, getClassName() + errorMessage);
                    }
                    throw new CmsException(errorMessage);
                }
            }
        }
        return result;
    }

    /**
     * Handles any occurence of an <code>&lt;ELEMENT&gt;</code> tag.
     * <P>
     * Every XML template class should use CmsXmlTemplateFile as
     * the interface to the XML file. Since CmsXmlTemplateFile is
     * an extension of A_CmsXmlContent by the additional tag
     * <code>&lt;ELEMENT&gt;</code> this user method ist mandatory.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document.
     * @param userObj Hashtable with parameters.
     * @return String or byte[] with the content of this subelement.
     * @exception CmsException
     */
    public Object templateElement(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws CmsException {

        // Our own template file that wants to include a subelement
        CmsXmlTemplateFile templateFile = (CmsXmlTemplateFile)doc;

        // Indicates, if this is a request of a guest user. Needed for error outputs.
        boolean isAnonymousUser = cms.anonymousUser().equals(cms.getRequestContext().currentUser());

        // First create a copy of the parameter hashtable
        Hashtable parameterHashtable = (Hashtable)((Hashtable)userObject).clone();

        // Name of the template class that should be used to handle the subtemplate
        String templateClass = getTemplateClassName(tagcontent, templateFile, parameterHashtable);

        // Name of the subtemplate file.
        String templateFilename = getTemplateFileName(tagcontent, templateFile, parameterHashtable);

        // Name of the subtemplate template selector
        String templateSelector = getTemplateSelector(tagcontent, templateFile, parameterHashtable);

        // Results returned by the subtemplate class
        byte[] result = null;

        // Temporary object for loading the subtemplate class
        Object loadedObject = null;

        // subtemplate class to be used for the include
        I_CmsTemplate subTemplate = null;

        // Key for the cache
        Object subTemplateKey = null;

        // try to load the subtemplate class
        try {
            loadedObject = CmsTemplateClassManager.getClassInstance(cms, templateClass);
        }
        catch(CmsException e) {

            // There was an error. First remove the template file from the file cache
            templateFile.removeFromFileCache();
            if(isAnonymousUser) {

                // The current user is the anonymous user
                return C_ERRORTEXT;
            }
            else {
                // The current user is a system user, so we throw the exception again.
                throw e;
            }
        }

        // Check if the loaded object is really an instance of an OpenCms template class
        if(!(loadedObject instanceof I_CmsTemplate)) {
            String errorMessage = "Class " + templateClass + " is no OpenCms template class.";
            if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(C_OPENCMS_CRITICAL, "[CmsXmlTemplate] " + errorMessage);
            }
            throw new CmsException(errorMessage, CmsException.C_XML_NO_TEMPLATE_CLASS);
        }
        subTemplate = (I_CmsTemplate)loadedObject;

        // Template class is now loaded. Next try to read the parameters
        Vector parameterTags = null;
        parameterTags = templateFile.getParameterNames(tagcontent);
        if(parameterTags != null) {
            int numParameterTags = parameterTags.size();
            for(int i = 0;i < numParameterTags;i++) {
                String paramName = (String)parameterTags.elementAt(i);
                String paramValue = templateFile.getParameter(tagcontent, paramName);
                if(!parameterHashtable.containsKey(paramName)) {
                    parameterHashtable.put(tagcontent + "." + paramName, paramValue);
                }
            }
        }

        // all parameters are now parsed. Finally give the own subelement name
        // as parameter
        // TODO: replace _ELEMENT_ by a constant
        parameterHashtable.put("_ELEMENT_", tagcontent);

        // Try to get the result from the cache
        //if(subTemplate.isCacheable(cms, templateFilename, tagcontent, parameterHashtable, null)) {
        if(subTemplate.collectCacheDirectives(cms, templateFilename, tagcontent, parameterHashtable, null).isInternalCacheable()) {
            subTemplateKey = subTemplate.getKey(cms, templateFilename, parameterHashtable, null);
            if(m_cache != null && m_cache.has(subTemplateKey) && (!subTemplate.shouldReload(cms, templateFilename, tagcontent, parameterHashtable, null))) {
                result = m_cache.get(subTemplateKey);
                if(cms.getRequestContext().isStreaming()) {
                    try {
                        cms.getRequestContext().getResponse().getOutputStream().write(result);
                    } catch(Exception e) {
                        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                            A_OpenCms.log(C_OPENCMS_CRITICAL, getClassName() + "Error while streaming!");
                        }
                    }
                }
            }
        }

        // OK. let's call the subtemplate
        if(result == null) {
            try {
                result = subTemplate.getContent(cms, templateFilename, tagcontent, parameterHashtable, templateSelector);
            }
            catch(Exception e) {

                // Oh, oh..

                // There were errors while getting the content of the subtemplate
                if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                    A_OpenCms.log(C_OPENCMS_CRITICAL, getClassName() + "Could not generate output for template file \"" + templateFilename + "\" included as element \"" + tagcontent + "\".");
                    A_OpenCms.log(C_OPENCMS_CRITICAL, getClassName() + e);
                }

                // The anonymous user gets an error String instead of an exception
                if(isAnonymousUser) {
                    if(cms.getRequestContext().isStreaming()) {
                        try {
                            cms.getRequestContext().getResponse().getOutputStream().write(C_ERRORTEXT.getBytes());
                        } catch(Exception e2) {
                            if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                                A_OpenCms.log(C_OPENCMS_CRITICAL, getClassName() + "Error while streaming!");
                            }
                        }
                    }
                    return C_ERRORTEXT;
                }
                else {
                    if(e instanceof CmsException) {
                        throw (CmsException)e;
                    }
                    else {
                        throw new CmsException("Error while executing getContent for subtemplate \"" + tagcontent + "\". " + e);
                    }
                }
            }

            // Store the results in the template cache, if cacheable
            //if(subTemplate.isCacheable(cms, templateFilename, tagcontent, parameterHashtable, null)) {
            if(subTemplate.collectCacheDirectives(cms, templateFilename, tagcontent, parameterHashtable, null).isInternalCacheable() && m_cache != null) {

                // we don't need to re-get the caching-key here since it already exists
                m_cache.put(subTemplateKey, result);
            }
        }
        return new CmsProcessedString(result);
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
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
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
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
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

    /**
     * Create a new element for the element cache consisting of the current template
     * class and the given template file.
     * <P>
     * Complex template classes that are able to include other (sub-)templates
     * must generate a collection of element definitions for their possible
     * subtemplates. This collection is part of the new element.
     * @param cms CmsObject for accessing system resources.
     * @param templateFile Name of the template file for the new element
     * @param parameters All parameters of the current request
     * @return New element for the element cache
     */
    public A_CmsElement createElement(CmsObject cms, String templateFile, Hashtable parameters) {

        CmsElementDefinitionCollection subtemplateDefinitions = new CmsElementDefinitionCollection();
        String readAccessGroup = cms.C_GROUP_ADMIN;
        int variantCachesize = 100;
        // if the templateFile is null someone didnt set the Templatefile in the elementdefinition
        // in this case we have to use the aktual body template when resolving the variant.
        // In a body element there are no subelements and we dont care about access rights.
        // So if if the Exception occurs becource of the template == null it is no error and
        // we set the readAccessGroup = null (this will happen by getReadingpermittedGroup)
        try {
            CmsElementCache elementCache = cms.getRequestContext().getElementCache();
            variantCachesize = elementCache.getVariantCachesize();

            readAccessGroup = cms.getReadingpermittedGroup(cms.getRequestContext().currentProject().getId(),templateFile);
            CmsXmlTemplateFile xmlTemplateDocument = getOwnTemplateFile(cms, templateFile, null, parameters, null);

            Vector subtemplates = xmlTemplateDocument.getAllSubElementDefinitions();

            int numSubtemplates = subtemplates.size();
            for(int i = 0; i < numSubtemplates; i++) {
                String elName = (String)subtemplates.elementAt(i);
                String className = null;
                String templateName = null;
                String templateSelector = null;

                if(xmlTemplateDocument.hasSubtemplateClass(elName)) {
                    className = xmlTemplateDocument.getSubtemplateClass(elName);
                }

                if(xmlTemplateDocument.hasSubtemplateFilename(elName)) {
                    templateName = xmlTemplateDocument.getSubtemplateFilename(elName);
                }

                if(xmlTemplateDocument.hasSubtemplateSelector(elName)) {
                    templateSelector = xmlTemplateDocument.getSubtemplateSelector(elName);
                }
                Hashtable templateParameters = xmlTemplateDocument.getParameters(elName);
                if(className != null || templateName != null || templateSelector != null || templateParameters.size() > 0) {
                    if(className == null){
                        className = "com.opencms.template.CmsXmlTemplate";
                    }
                    if(templateName != null){
                        templateName = Utils.mergeAbsolutePath(templateFile, templateName);
                    }
                    CmsElementDefinition elDef = new CmsElementDefinition(elName, className, templateName, templateSelector, templateParameters);
                    subtemplateDefinitions.add(elDef);
                }
            }
        } catch(Exception e) {
            if(templateFile != null){
                if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                    A_OpenCms.log(C_OPENCMS_ELEMENTCACHE, getClassName() + "Could not generate my template cache element.");
                    A_OpenCms.log(C_OPENCMS_ELEMENTCACHE, getClassName() + e);
                }
            }else{
                // no templateFile given, so let everyone read this
                readAccessGroup = null;
            }
        }
        CmsElementXml result = new CmsElementXml(getClass().getName(),
                                                 templateFile, readAccessGroup,
                                                 getCacheDirectives(cms, templateFile, null, parameters, null),
                                                 subtemplateDefinitions, variantCachesize);
        return result;
    }
}
