/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsNewResourceLink.java,v $
* Date   : $Date: 2002/05/17 11:15:29 $
* Version: $Revision: 1.22 $
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
import javax.servlet.http.*;
import java.util.*;

/**
 * Template class for displaying the new resource screen for a new link
 * of the OpenCms workplace.<P>
 * Reads template files of the content type <code>CmsXmlWpTemplateFile</code>.
 *
 * @author Michael Emmerich
 * @version $Revision: 1.22 $ $Date: 2002/05/17 11:15:29 $
 */

public class CmsNewResourceLink extends CmsWorkplaceDefault implements I_CmsWpConstants,I_CmsConstants {

    /**
     * Overwrites the getContent method of the CmsWorkplaceDefault.<br>
     * Gets the content of the new resource othertype template and processed the data input.
     * @param cms The CmsObject.
     * @param templateFile The lock template file
     * @param elementName not used
     * @param parameters Parameters of the request and the template.
     * @param templateSelector Selector of the template tag to be displayed.
     * @return Bytearry containing the processed data of the template.
     * @exception Throws CmsException if something goes wrong.
     */

    public byte[] getContent(CmsObject cms, String templateFile, String elementName,
            Hashtable parameters, String templateSelector) throws CmsException {

        String error = "";
        boolean checkurl = true;

        String filename = null;
        String link = null;
        String foldername = null;
        String type = null;
        I_CmsSession session = cms.getRequestContext().getSession(true);

        // get the document to display
        CmsXmlWpTemplateFile xmlTemplateDocument = new CmsXmlWpTemplateFile(cms, templateFile);
        CmsXmlLanguageFile lang = xmlTemplateDocument.getLanguageFile();

        // clear session values on first load
        String initial = (String)parameters.get(C_PARA_INITIAL);
        if(initial != null){
            // remove all session values
            session.removeValue(C_PARA_FILE);
            session.removeValue(C_PARA_LINK);
            session.removeValue(C_PARA_VIEWFILE);
            session.removeValue(C_PARA_NAVPOS);
            session.removeValue(C_PARA_NAVTEXT);
            session.removeValue("lasturl");
        }
        // get the lasturl from parameters or from session
        String lastUrl = getLastUrl(cms, parameters);
        if(lastUrl != null){
            session.putValue("lasturl", lastUrl);
        }
        // get the linkname and the linkurl
        filename = cms.getRequestContext().getRequest().getParameter(C_PARA_FILE);
        if(filename != null) {
            session.putValue(C_PARA_FILE, filename);
        }  else {
            // try to get the value from the session, e.g. after an error
            filename = (String)session.getValue(C_PARA_FILE)!=null?(String)session.getValue(C_PARA_FILE):"";
        }
        link = cms.getRequestContext().getRequest().getParameter(C_PARA_LINK);
        if(link != null) {
            session.putValue(C_PARA_LINK, link);
        } else {
            // try to get the value from the session, e.g. after an error
            link = (String)session.getValue(C_PARA_LINK)!=null?(String)session.getValue(C_PARA_LINK):"";
        }
        // get the parameters
        String navtitle = (String)parameters.get(C_PARA_NAVTEXT);
        if(navtitle != null) {
            session.putValue(C_PARA_NAVTEXT, navtitle);
        } else {
            // try to get the value from the session, e.g. after an error
            navtitle = (String)session.getValue(C_PARA_NAVTEXT)!=null?(String)session.getValue(C_PARA_NAVTEXT):"";
        }
        String navpos = (String)parameters.get(C_PARA_NAVPOS);
        if(navpos != null) {
            session.putValue(C_PARA_NAVPOS, navpos);
        } else {
            // try to get the value from the session, e.g. after an error
            navpos = (String)session.getValue(C_PARA_NAVPOS)!=null?(String)session.getValue(C_PARA_NAVPOS):"";
        }
        String notChange = (String)parameters.get("newlink");
        CmsResource linkResource = null;
        String step = cms.getRequestContext().getRequest().getParameter("step");

        // set the values e.g. after an error
        xmlTemplateDocument.setData("LINKNAME", filename);
        xmlTemplateDocument.setData("LINKVALUE", link);
        xmlTemplateDocument.setData("NAVTITLE", navtitle);

        // if an existing link should be edited show the change page
        if(notChange != null && notChange.equals("false") && step == null) {
            try{
                CmsFile currentFile = cms.readFile(filename);
                String content = new String(currentFile.getContents());
                xmlTemplateDocument.setData("LINKNAME", currentFile.getName());
                xmlTemplateDocument.setData("LINK", currentFile.getAbsolutePath());
                xmlTemplateDocument.setData("LINKVALUE", content);
                templateSelector = "change";
            } catch (CmsException e){
                error = e.getShortException();
            }
        }

        // get the current phase of this wizard
        if(step != null) {
            // step 1 - show the final selection screen
            if(step.equals("1") || step.equals("2")) {
                try{
                    // step 1 - create the link with checking http-link
                    // step 2 - create the link without link check
                    // get folder- and filename
                    foldername = (String)session.getValue(C_PARA_FILELIST);
                    if(foldername == null) {
                        foldername = cms.rootFolder().getAbsolutePath();
                    }

                    String title = lang.getLanguageValue("explorer.linkto") + " " + link;
                    type = "link";
                    if(notChange != null && notChange.equals("false")) {

                        // change old file
                        CmsFile editFile = cms.readFile(filename);
                        editFile.setContents(link.getBytes());

                        if(step.equals("1")){
                            if(!link.startsWith("/")){
                                checkurl = CmsLinkCheck.checkUrl(link);
                            }
                        }
                        if(checkurl){
                            cms.writeFile(editFile);
                            cms.writeProperty(filename, C_PROPERTY_TITLE, title);
                        }
                        linkResource = (CmsResource)editFile;
                    } else {
                        // create the new file
                        Hashtable prop = new Hashtable();
                        prop.put(C_PROPERTY_TITLE, title);
                        if(step.equals("1")){
                            if(!link.startsWith("/")){
                                checkurl = CmsLinkCheck.checkUrl(link);
                            }
                        }
                        if(checkurl){
                            linkResource = cms.createResource(foldername, filename, type, prop, link.getBytes());
                        }
                    }
                    // now check if navigation informations have to be added to the new page.
                    if((navtitle != null) && checkurl) {
                        cms.writeProperty(linkResource.getAbsolutePath(), C_PROPERTY_NAVTEXT, navtitle);
                        // update the navposition.
                        if(navpos != null) {
                            updateNavPos(cms, linkResource, navpos);
                        }
                    }

                    // remove values from session
                    session.removeValue(C_PARA_FILE);
                    session.removeValue(C_PARA_VIEWFILE);
                    session.removeValue(C_PARA_LINK);
                    session.removeValue(C_PARA_NAVTEXT);
                    session.removeValue(C_PARA_NAVPOS);

                    // now return to appropriate filelist
                } catch (CmsException e){
                    error = e.getShortException();
                }

                if(checkurl && ("".equals(error.trim()))){
                    try {
                        if(lastUrl != null) {
                            cms.getRequestContext().getResponse().sendRedirect(lastUrl);
                        } else {
                            cms.getRequestContext().getResponse().sendCmsRedirect(getConfigFile(cms).getWorkplaceActionPath()
                                    + C_WP_EXPLORER_FILELIST);
                        }
                    } catch(Exception e) {
                        throw new CmsException("Redirect fails :" + getConfigFile(cms).getWorkplaceActionPath()
                            + C_WP_EXPLORER_FILELIST, CmsException.C_UNKNOWN_EXCEPTION, e);
                    }
                    return null;
                }
            }
        } else {
            session.removeValue(C_PARA_FILE);
            session.removeValue(C_PARA_VIEWFILE);
            session.removeValue(C_PARA_LINK);
            session.removeValue(C_PARA_NAVTEXT);
        }
        // set lasturl
        if(lastUrl == null) {
            lastUrl = C_WP_EXPLORER_FILELIST;
        }
        xmlTemplateDocument.setData("lasturl", lastUrl);
        // set the templateselector if there was an error
        if(!checkurl){
            xmlTemplateDocument.setData("folder", foldername);
            xmlTemplateDocument.setData("newlink", notChange);
            session.putValue(C_PARA_LINK, link);
            session.putValue(C_PARA_FILE, filename);
            session.putValue(C_PARA_NAVTEXT, navtitle);
            session.putValue(C_PARA_NAVPOS, navpos);
            templateSelector = "errorcheckurl";
        }
        if(!"".equals(error.trim())){
            xmlTemplateDocument.setData("errordetails", error);
            session.putValue(C_PARA_LINK, link);
            session.putValue(C_PARA_FILE, filename);
            session.putValue(C_PARA_NAVTEXT, navtitle);
            session.putValue(C_PARA_NAVPOS, navpos);
            templateSelector = "error";
        }
        // process the selected template
        return startProcessing(cms, xmlTemplateDocument, elementName, parameters, templateSelector);
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
     * Sets the value of the new file input field of dialog.
     * This method is directly called by the content definiton.
     * @param Cms The CmsObject.
     * @param lang The language file.
     * @param parameters User parameters.
     * @return Value that is set into the new file dialod.
     * @exception CmsExeption if something goes wrong.
     */

    public String setValue(CmsObject cms, CmsXmlLanguageFile lang, Hashtable parameters) throws CmsException {
        I_CmsSession session = cms.getRequestContext().getSession(true);

        // get a previous value from the session
        String filename = (String)session.getValue(C_PARA_FILE);
        if(filename == null) {
            filename = "";
        }
        return filename;
    }

    /**
     * Gets the files displayed in the navigation select box.
     * @param cms The CmsObject.
     * @param lang The langauge definitions.
     * @param names The names of the new rescources.
     * @param values The links that are connected with each resource.
     * @param parameters Hashtable of parameters (not used yet).
     * @returns The vectors names and values are filled with data for building the navigation.
     * @exception Throws CmsException if something goes wrong.
     */
    public Integer getNavPos(CmsObject cms, CmsXmlLanguageFile lang, Vector names,
            Vector values, Hashtable parameters) throws CmsException {
        int retValue = -1;
        I_CmsSession session = cms.getRequestContext().getSession(true);
        String preselect = (String)session.getValue(C_PARA_NAVPOS);
        // get the nav information
        Hashtable storage = getNavData(cms);
        if(storage.size() > 0) {
            String[] nicenames = (String[])storage.get("NICENAMES");
            int count = ((Integer)storage.get("COUNT")).intValue();

            // finally fill the result vectors
            for(int i = 0;i <= count;i++) {
                names.addElement(nicenames[i]);
                values.addElement(nicenames[i]);
                if ((preselect != null) && (preselect.equals(nicenames[i]))){
                    retValue = values.size() -1;
                }
            }
        }
        else {
            values = new Vector();
        }
        if (retValue == -1){
            // set the default value to no change
            return new Integer(values.size() - 1);
        }else{
            return new Integer(retValue);
        }
    }

    /**
     * Gets all required navigation information from the files and subfolders of a folder.
     * A file list of all files and folder is created, for all those resources, the navigation
     * property is read. The list is sorted by their navigation position.
     * @param cms The CmsObject.
     * @return Hashtable including three arrays of strings containing the filenames,
     * nicenames and navigation positions.
     * @exception Throws CmsException if something goes wrong.
     */
    private Hashtable getNavData(CmsObject cms) throws CmsException {
        I_CmsSession session = cms.getRequestContext().getSession(true);
        CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);
        String[] filenames;
        String[] nicenames;
        String[] positions;
        Hashtable storage = new Hashtable();
        CmsFolder folder = null;
        CmsFile file = null;
        String nicename = null;
        String currentFilelist = null;
        int count = 1;
        float max = 0;

        // get the current folder
        currentFilelist = (String)session.getValue(C_PARA_FILELIST);
        if(currentFilelist == null) {
            currentFilelist = cms.rootFolder().getAbsolutePath();
        }

        // get all files and folders in the current filelist.
        Vector files = cms.getFilesInFolder(currentFilelist);
        Vector folders = cms.getSubFolders(currentFilelist);

        // combine folder and file vector
        Vector filefolders = new Vector();
        Enumeration enum = folders.elements();
        while(enum.hasMoreElements()) {
            folder = (CmsFolder)enum.nextElement();
            filefolders.addElement(folder);
        }
        enum = files.elements();
        while(enum.hasMoreElements()) {
            file = (CmsFile)enum.nextElement();
            filefolders.addElement(file);
        }
        if(filefolders.size() > 0) {

            // Create some arrays to store filename, nicename and position for the
            // nav in there. The dimension of this arrays is set to the number of
            // found files and folders plus two more entrys for the first and last
            // element.
            filenames = new String[filefolders.size() + 2];
            nicenames = new String[filefolders.size() + 2];
            positions = new String[filefolders.size() + 2];

            //now check files and folders that are not deleted and include navigation
            // information
            enum = filefolders.elements();
            while(enum.hasMoreElements()) {
                CmsResource res = (CmsResource)enum.nextElement();

                // check if the resource is not marked as deleted
                if(res.getState() != C_STATE_DELETED) {
                    String navpos = cms.readProperty(res.getAbsolutePath(), C_PROPERTY_NAVPOS);

                    // check if there is a navpos for this file/folder
                    if(navpos != null) {
                        nicename = cms.readProperty(res.getAbsolutePath(), C_PROPERTY_NAVTEXT);
                        if(nicename == null) {
                            nicename = res.getName();
                        }

                        // add this file/folder to the storage.
                        filenames[count] = res.getAbsolutePath();
                        nicenames[count] = nicename;
                        positions[count] = navpos;
                        if(new Float(navpos).floatValue() > max) {
                            max = new Float(navpos).floatValue();
                        }
                        count++;
                    }
                }
            }
        }
        else {
            filenames = new String[2];
            nicenames = new String[2];
            positions = new String[2];
        }

        // now add the first and last value
        filenames[0] = "FIRSTENTRY";
        nicenames[0] = lang.getDataValue("input.firstelement");
        positions[0] = "0";
        filenames[count] = "LASTENTRY";
        nicenames[count] = lang.getDataValue("input.lastelement");
        positions[count] = new Float(max + 1).toString();

        // finally sort the nav information.
        sort(cms, filenames, nicenames, positions, count);

        // put all arrays into a hashtable to return them to the calling method.
        storage.put("FILENAMES", filenames);
        storage.put("NICENAMES", nicenames);
        storage.put("POSITIONS", positions);
        storage.put("COUNT", new Integer(count));
        return storage;
    }

    /**
     * Sorts a set of three String arrays containing navigation information depending on
     * their navigation positions.
     * @param cms Cms Object for accessign files.
     * @param filenames Array of filenames
     * @param nicenames Array of well formed navigation names
     * @param positions Array of navpostions
     */
    private void sort(CmsObject cms, String[] filenames, String[] nicenames, String[] positions, int max) {

        // Sorting algorithm
        // This method uses an bubble sort, so replace this with something more
        // efficient
        for(int i = max - 1;i > 1;i--) {
            for(int j = 1;j < i;j++) {
                float a = new Float(positions[j]).floatValue();
                float b = new Float(positions[j + 1]).floatValue();
                if(a > b) {
                    String tempfilename = filenames[j];
                    String tempnicename = nicenames[j];
                    String tempposition = positions[j];
                    filenames[j] = filenames[j + 1];
                    nicenames[j] = nicenames[j + 1];
                    positions[j] = positions[j + 1];
                    filenames[j + 1] = tempfilename;
                    nicenames[j + 1] = tempnicename;
                    positions[j + 1] = tempposition;
                }
            }
        }
    }

    /**
     * Updates the navigation position of all resources in the actual folder.
     * @param cms The CmsObject.
     * @param newfile The new file added to the nav.
     * @param navpos The file after which the new entry is sorted.
     */
    private void updateNavPos(CmsObject cms, CmsResource newfile, String newpos) throws CmsException {
        float newPos = 0;

        // get the nav information
        Hashtable storage = getNavData(cms);
        if(storage.size() > 0) {
            String[] nicenames = (String[])storage.get("NICENAMES");
            String[] positions = (String[])storage.get("POSITIONS");
            int count = ((Integer)storage.get("COUNT")).intValue();

            // now find the file after which the new file is sorted
            int pos = 0;
            for(int i = 0;i < nicenames.length;i++) {
                if(newpos.equals((String)nicenames[i])) {
                    pos = i;
                }
            }
            if(pos < count) {
                float low = new Float(positions[pos]).floatValue();
                float high = new Float(positions[pos + 1]).floatValue();
                newPos = (high + low) / 2;
            }
            else {
                newPos = new Float(positions[pos]).floatValue() + 1;
            }
        }
        else {
            newPos = 1;
        }
        cms.writeProperty(newfile.getAbsolutePath(), C_PROPERTY_NAVPOS, new Float(newPos).toString());
    }
}
