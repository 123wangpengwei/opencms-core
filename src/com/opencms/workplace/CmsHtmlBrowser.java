/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsHtmlBrowser.java,v $
* Date   : $Date: 2002/11/07 19:33:56 $
* Version: $Revision: 1.4 $
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


package com.opencms.workplace;

import com.opencms.file.*;
import com.opencms.core.*;
import com.opencms.util.*;
import com.opencms.template.*;

import java.io.UnsupportedEncodingException;
import java.util.*;
import javax.servlet.http.*;

/**
 * Template class for displaying OpenCms html snippet browser.
 * <P>
 * Reads template files of the content type <code>CmsXmlWpTemplateFile</code>.
 *
 * @author magnus meurer
 * @version $Revision: 1.4 $ $Date: 2002/11/07 19:33:56 $
 * @see com.opencms.workplace.CmsXmlWpTemplateFile
 */

public class CmsHtmlBrowser extends CmsWorkplaceDefault {

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
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() && C_DEBUG) {
            A_OpenCms.log(C_OPENCMS_DEBUG, getClassName() + "getting content of element "
                    + ((elementName == null) ? "<root>" : elementName));
            A_OpenCms.log(C_OPENCMS_DEBUG, getClassName()
                    + "template file is: " + templateFile);
            A_OpenCms.log(C_OPENCMS_DEBUG, getClassName()
                    + "selected template section is: " + ((templateSelector == null) ? "<default>" : templateSelector));
        }
        I_CmsSession session = cms.getRequestContext().getSession(true);
        CmsXmlWpTemplateFile xmlTemplateDocument = (CmsXmlWpTemplateFile)getOwnTemplateFile(cms,
                templateFile, elementName, parameters, templateSelector);

        // test whether the links folder exists at all
        try {
            cms.readFileHeader(C_VFS_GALLERY_EXTERNALLINKS);
        } catch(CmsException e) {
            xmlTemplateDocument.setData("ERRORDETAILS", Utils.getStackTrace(e));
            templateSelector = "error";
        }
        if(!"error".equals(templateSelector)) {
            if(parameters.get(C_PARA_INITIAL) != null) {
                session.removeValue(C_PARA_FOLDER);
                session.removeValue("htmlBrowser_for_ext_nav");
            }
            String setOnClick = (String)parameters.get("setonclick");
            if(setOnClick != null){
                session.putValue("htmlBrowser_for_ext_nav", setOnClick);
            }
            setOnClick = (String)session.getValue("htmlBrowser_for_ext_nav");
            String folder = (String)parameters.get(C_PARA_FOLDER);
            if(folder != null) {
                session.putValue(C_PARA_FOLDER, folder);
            }
            folder = (String)session.getValue(C_PARA_FOLDER);
            if(folder == null || "".equals(folder)) {
                Vector galleries = cms.getSubFolders(getConfigFile(cms).getHtmlGalleryPath());
                if(galleries.size() > 0) {

                    // take the first gallery
                    folder = ((CmsResource)galleries.elementAt(0)).getAbsolutePath();
                    session.putValue(C_PARA_FOLDER, folder);
                } else {

                    // there was a C_VFS_GALLERY_EXTERNALLINKS - folder but no galery in it
                    templateSelector = "error_no_gallery";
                }
            }
            if(!"error_no_gallery".equals(templateSelector)) {
                String pageText = (String)parameters.get(C_PARA_PAGE);
                String filter = (String)parameters.get(C_PARA_FILTER);

                // Check if the user requested a special page
                if(pageText == null || "".equals(pageText)) {
                    pageText = "1";
                    parameters.put(C_PARA_PAGE, pageText);
                }

                // Check if the user requested a filter
                if(filter == null) {
                    filter = "";
                    parameters.put(C_PARA_FILTER, filter);
                }

                // Compute the maximum page number
                Vector filteredLinks = getFilteredHtmlList(cms, folder, filter);
                int maxpage = ((filteredLinks.size() - 1) / C_PICBROWSER_MAXIMAGES) + 1;

                // Now set the appropriate datablocks
                //Gridnine AB Aug 8, 2002
                xmlTemplateDocument.setData(C_PARA_FOLDER, Encoder.escape(folder,
                    cms.getRequestContext().getEncoding()));
                xmlTemplateDocument.setData(C_PARA_PAGE, pageText);
                xmlTemplateDocument.setData(C_PARA_FILTER, filter);
                xmlTemplateDocument.setData(C_PARA_MAXPAGE, "" + maxpage);
                if(setOnClick == null || !"true".equals(setOnClick)){
                    xmlTemplateDocument.setData("setonclick", "");
                }else{
                    xmlTemplateDocument.setData("setonclick", "true");
                }
                parameters.put("_HTMLLIST_", filteredLinks);
            }
        }

        // Start the processing
        return startProcessing(cms, xmlTemplateDocument, elementName, parameters, templateSelector);
    }

    /**
     * Internal method for getting a vector of all links using
     * a given filter.
     * @param cms Cms object for accessing system resources.
     * @param folder Folder to look for links.
     * @param filter Search pattern that should be used.
     * @return Vector of CmsFile objects.
     */

    private Vector getFilteredHtmlList(CmsObject cms, String folder, String filter) throws CmsException {

        // Get all links in the given folder using the cms object
        Vector allLinks = cms.getFilesInFolder(folder);

        // Filter the links
        Vector filteredLinks = new Vector();
        for(int i = 0;i < allLinks.size();i++) {
            CmsFile file = (CmsFile)allLinks.elementAt(i);
            String filename = file.getName();
            String title = cms.readProperty(file.getAbsolutePath(), C_PROPERTY_TITLE);
            boolean filenameFilter = inFilter(filename, filter);
            boolean titleFilter = ((title == null) || ("".equals(title))) ? false : inFilter(title, filter);
            if((filenameFilter || titleFilter)) {
                filteredLinks.addElement(file);
            }
        }
        return filteredLinks;
    }

    /**
     * Gets the filenames of all html galleries
     * <P>
     * The given vectors <code>names</code> and <code>values</code> will
     * be filled with the appropriate information to be used for building
     * a select box. The values will be the paths to the galleries.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param names Vector to be filled with the appropriate values in this method.
     * @param values Vector to be filled with the appropriate values in this method.
     * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
     * @return Index of the selected Gallery
     * @exception CmsException
     */

    public Integer getHtmlGalleryNames(CmsObject cms, CmsXmlLanguageFile lang,
            Vector names, Vector values, Hashtable parameters) throws CmsException {
        int ret = -1;
        I_CmsSession session = cms.getRequestContext().getSession(true);

        // which folder is the gallery?
        String chosenFolder = (String)parameters.get(C_PARA_FOLDER);
        if(chosenFolder == null) {
            chosenFolder = (String)session.getValue(C_PARA_FOLDER);
        }
        if(chosenFolder == null) {
            chosenFolder = "";
        }
        Vector folders = cms.getSubFolders(getConfigFile(cms).getHtmlGalleryPath());
        int numFolders = folders.size();
        for(int i = 0;i < numFolders;i++) {
            CmsResource currFolder = (CmsResource)folders.elementAt(i);
            String name = currFolder.getName();
            if(chosenFolder.equals(currFolder.getAbsolutePath())) {
                ret = i;
            }
            values.addElement(currFolder.getAbsolutePath());
            names.addElement(name);
        }
        return new Integer(ret);
    }

    /**
     * Checks, if the given filename matches the filter.
     * @param filename filename to be checked.
     * @param filter filter to be checked.
     * @return <code>true</code> if the filename matches the filter, <code>false</code> otherwise.
     */

    private boolean inFilter(String name, String filter) {
        String compareName = name.toLowerCase();
        String compareFilter = filter.toLowerCase();
        if("".equals(compareFilter) || (compareName.indexOf(compareFilter) != -1)) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Indicates if the results of this class are cacheable.
     *
     * @param cms CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return <EM>true</EM> if cacheable, <EM>false</EM> otherwise.
     */

    public boolean isCacheable(CmsObject cms, String templateFile, String elementName,
            Hashtable parameters, String templateSelector) {
        return false;
    }

    /**
     * User method to generate an URL for the pics folder.
     * <P>
     * All pictures should reside in the docroot of the webserver for
     * performance reasons. This folder can be mounted into the OpenCms system to
     * make it accessible for the OpenCms explorer.
     * <P>
     * The path to the docroot can be set in the workplace ini.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document <em>(not used here)</em>.
     * @param userObj Hashtable with parameters <em>(not used here)</em>.
     * @return String with the pics URL.
     * @exception CmsException
     */

    public Object htmlList(CmsObject cms, String tagcontent, A_CmsXmlContent doc,
            Object userObj) throws CmsException {
        I_CmsSession session = cms.getRequestContext().getSession(true);
        Hashtable parameters = (Hashtable)userObj;
        CmsXmlWpTemplateFile xmlTemplateDocument = (CmsXmlWpTemplateFile)doc;
        CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);
        StringBuffer result = new StringBuffer();
        String pageText = (String)parameters.get(C_PARA_PAGE);

        // Filter the links
        Vector filteredLinks = (Vector)parameters.get("_HTMLLIST_");
        int numLinks = filteredLinks.size();

        // Get limits for the requested page
        int page = new Integer(pageText).intValue();
        int from = (page - 1) * C_PICBROWSER_MAXIMAGES;
        int to = ((from + C_PICBROWSER_MAXIMAGES) > numLinks) ? numLinks : (from + C_PICBROWSER_MAXIMAGES);
        String folder = (String)parameters.get(C_PARA_FOLDER);
        if(folder == null) {
            folder = (String)session.getValue(C_PARA_FOLDER);
        }
        if(folder == null || "".equals(folder)) {
            folder = getConfigFile(cms).getHtmlGalleryPath();
            parameters.put(C_PARA_FOLDER, folder);
        }

        HttpServletRequest req = (HttpServletRequest)(cms.getRequestContext().getRequest().getOriginalRequest());
        String hostName = ""; // no need for host information in editor any more
        String linkUrl = cms.getRequestContext().getRequest().getServletUrl() + folder;

        // Generate the link list for all links on the selected page
        for(int i = from;i < to;i++) {
            CmsFile file = cms.readFile(((CmsFile)filteredLinks.elementAt(i)).getAbsolutePath());
            String filename = file.getName();
            String title = cms.readProperty(file.getAbsolutePath(), C_PROPERTY_TITLE);

            // If no "Title" property is given, the title will be set to the filename
            // without its postfix
            int dotIndex = filename.lastIndexOf(".");
            if(title == null) {
                if(dotIndex > 0) {
                    title = filename.substring(0, dotIndex);
                }
                else {
                    title = filename;
                }
            }

            // Set all datablocks for the current picture list entry
            xmlTemplateDocument.setData("linksource", hostName + linkUrl + file.getName());
            xmlTemplateDocument.setData("filename", file.getAbsolutePath());
            xmlTemplateDocument.setData("title", filename);
            xmlTemplateDocument.setData("linktext", filename);
            xmlTemplateDocument.setData("snippetid", "" + i);
            xmlTemplateDocument.setData("filecontent", new String(file.getContents()));
            //Gridnine AB Aug 8, 2002
            try {
                xmlTemplateDocument.setData("filecontent_escaped", Encoder.escape(
                    new String(file.getContents(), cms.getRequestContext().getEncoding()),
                    cms.getRequestContext().getEncoding()));
            } catch (UnsupportedEncodingException e) {
                xmlTemplateDocument.setData("filecontent_escaped", Encoder.escape(
                    new String(file.getContents()),
                    cms.getRequestContext().getEncoding()));
            }

            // look if the onclick event must be set
            String paraSetOnClick = (String)session.getValue("htmlBrowser_for_ext_nav");
            String setOnClick = "";
            if ("true".equals(paraSetOnClick)){
                setOnClick = xmlTemplateDocument.getProcessedDataValue("clickentry");
            }
            xmlTemplateDocument.setData("toclickornot", setOnClick);
            result.append(xmlTemplateDocument.getProcessedDataValue("htmllistentry", this, userObj));
        }
        return result.toString();
    }

    /**
     * Used by the workplace "back" button to decide whether the icon should
     * be activated or not. A button will use this method if the attribute <code>method="showBackButton"</code>
     * is defined in the <code>&lt;BUTTON&gt;</code> tag.
     * <P>
     * This method returns <code>false</code> if the currently displayed page is
     * the first page.
     *
     * @param cms CmsObject Object for accessing system resources <em>(not used here)</em>.
     * @param lang reference to the currently valid language file <em>(not used here)</em>.
     * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
     * @return <code>true</code> if the button should be enabled, <code>false</code> otherwise.
     */

    public Boolean showBackButton(CmsObject cms, CmsXmlLanguageFile lang, Hashtable parameters) {

        // Get the current page number
        String pageText = (String)parameters.get(C_PARA_PAGE);
        int page = new Integer(pageText).intValue();
        return new Boolean(page > 1);
    }

    /**
     * Used by the workplace "next" button to decide whether the icon should
     * be activated or not. A button will use this method if the attribute <code>method="showNextButton"</code>
     * is defined in the <code>&lt;BUTTON&gt;</code> tag.
     * <P>
     * This method returns <code>false</code> if the currently displayed page is
     * the last page.
     *
     * @param cms CmsObject Object for accessing system resources <em>(not used here)</em>.
     * @param lang reference to the currently valid language file <em>(not used here)</em>.
     * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
     * @return <code>true</code> if the button should be enabled, <code>false</code> otherwise.
     */

    public Boolean showNextButton(CmsObject cms, CmsXmlLanguageFile lang, Hashtable parameters) {

        // Get the current page number
        String pageText = (String)parameters.get(C_PARA_PAGE);
        int page = new Integer(pageText).intValue();

        // get the number of links
        Vector filteredLinks = (Vector)parameters.get("_HTMLLIST_");
        int numLinks = filteredLinks.size();

        // Get the maximum page number
        int maxpage = ((numLinks - 1) / C_PICBROWSER_MAXIMAGES) + 1;
        return new Boolean(page < maxpage);
    }
}
