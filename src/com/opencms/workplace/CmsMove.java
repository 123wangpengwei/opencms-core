/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsMove.java,v $
* Date   : $Date: 2003/07/30 13:22:24 $
* Version: $Revision: 1.61 $
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

import org.opencms.workplace.CmsWorkplaceAction;

import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.core.I_CmsSession;
import com.opencms.file.CmsFolder;
import com.opencms.file.CmsObject;
import com.opencms.file.CmsProject;
import com.opencms.file.CmsResource;
import com.opencms.util.Encoder;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Template class for displaying the move file screen of the OpenCms workplace.<P>
 * Reads template files of the content type <code>CmsXmlWpTemplateFile</code>.
 *
 * @author Michael Emmerich
 * @author Michaela Schleich
 * @version $Revision: 1.61 $ $Date: 2003/07/30 13:22:24 $
 */

public class CmsMove extends CmsWorkplaceDefault implements I_CmsWpConstants,I_CmsConstants {

    /**
     * Overwrites the getContent method of the CmsWorkplaceDefault.<br>
     * Gets the content of the move template and processed the data input.
     * @param cms The CmsObject.
     * @param templateFile The move template file
     * @param elementName not used
     * @param parameters Parameters of the request and the template.
     * @param templateSelector Selector of the template tag to be displayed.
     * @return Bytearre containgine the processed data of the template.
     * @throws Throws CmsException if something goes wrong.
     */
    public byte[] getContent(CmsObject cms, String templateFile, String elementName,
            Hashtable parameters, String templateSelector) throws CmsException {
        I_CmsSession session = cms.getRequestContext().getSession(true);
        CmsXmlWpTemplateFile xmlTemplateDocument = new CmsXmlWpTemplateFile(cms,
                templateFile);
        CmsXmlLanguageFile lang = xmlTemplateDocument.getLanguageFile();

        // the template to be displayed
        String template = null;

        // clear session values on first load
        String initial = (String)parameters.get(C_PARA_INITIAL);
        if(initial != null) {

            // remove all session values
            session.removeValue(C_PARA_FILE);
            session.removeValue(C_PARA_NEWFOLDER);
            session.removeValue(C_PARA_FLAGS);
            session.removeValue("lasturl");
        }

        // get the lasturl parameter
        String lasturl = getLastUrl(cms, parameters);

        // get the file to be copied
        String sourceFilename = (String)parameters.get(C_PARA_FILE);
        if(sourceFilename != null) {
            session.putValue(C_PARA_FILE, sourceFilename);
        }
        
        sourceFilename = (String)session.getValue(C_PARA_FILE);
        CmsResource source = cms.readFileHeader(sourceFilename);

        // read all request parameters
        String newFolder = new String();
        String newFile = new String();
        String destinationName = (String)parameters.get(C_PARA_NEWFOLDER);

        // the wholePath includes the folder and/or the filename
        if(destinationName != null && !("".equals(destinationName))){
            if(destinationName.startsWith("/")){
                // get the foldername
                newFolder = destinationName.substring(0, destinationName.lastIndexOf("/")+1);
                newFile = destinationName.substring(destinationName.lastIndexOf("/")+1);
                if (newFile == null || "".equals(newFile)){
                    newFile = source.getResourceName();
                }
            } else {
                newFolder = CmsResource.getParent(cms.readAbsolutePath(source));
                newFile = destinationName;
            }
        }
        if(newFolder != null && !("".equals(newFolder))) {
            session.putValue(C_PARA_NEWFOLDER, newFolder);
        }
        newFolder = (String)session.getValue(C_PARA_NEWFOLDER);

        if(newFile != null && !("".equals(newFile))) {
            session.putValue(C_PARA_NEWFILE, newFile);
        }
        newFile = (String)session.getValue(C_PARA_NEWFILE);

        String flags = (String)parameters.get(C_PARA_FLAGS);
        if(flags != null) {
            session.putValue(C_PARA_FLAGS, flags);
        }
        flags = (String)session.getValue(C_PARA_FLAGS);
        String action = (String)parameters.get("action");
        // modify the folderaname if nescessary (the root folder is always given
        // as a nice name)
        if(newFolder != null && !("".equals(newFolder))) {
            if(newFolder.equals(lang.getLanguageValue("title.rootfolder"))) {
                newFolder = "/";
            }

            // ednfal: check if the user try to move the resource into itself
            if(newFolder.equals(cms.readAbsolutePath(source))) {
                 // something went wrong, so remove all session parameters
                session.removeValue(C_PARA_FILE);
                session.removeValue(C_PARA_NEWFOLDER);
                session.removeValue(C_PARA_FLAGS);
                template = "error";
                xmlTemplateDocument.setData("details", "Can't move folder into itself");
                xmlTemplateDocument.setData("lasturl", lasturl);
                return startProcessing(cms, xmlTemplateDocument, "", parameters,
                            template);
            }

            // ednfal: try to read the destination folder
            try {
                cms.readFolder(newFolder);
            }
            catch(CmsException ex) {
                // something went wrong, so remove all session parameters
                session.removeValue(C_PARA_FILE);
                session.removeValue(C_PARA_NEWFOLDER);
                session.removeValue(C_PARA_FLAGS);

                template = "error";
                if(ex.getType() == CmsException.C_NOT_FOUND) {
                    xmlTemplateDocument.setData("details", "Destination folder not exists"+ex.getStackTraceAsString());
                } else {
                    xmlTemplateDocument.setData("details", ex.getStackTraceAsString());
                }
                xmlTemplateDocument.setData("lasturl", lasturl);
                return startProcessing(cms, xmlTemplateDocument, "", parameters,
                                template);
            }
        }

        // select the template to be displayed
        if(source.isFile()) {
            template = "file";
        }
        else {
            template = "folder";
        }

        //check if the newFolder parameter was included in the request
        //if not, the move page is shown for the first time
        if(newFolder != null && !("".equals(newFolder))) {
            if(action == null) {
                template = "wait";
            }
            else {
                if(source.isFile()) {

                    // this is a file, so move it
                    try {
                        cms.moveResource(cms.readAbsolutePath(source), newFolder + newFile);
                    }
                    catch(CmsException ex) {

                        // something went wrong, so remove all session parameters
                        session.removeValue(C_PARA_FILE);
                        session.removeValue(C_PARA_NEWFOLDER);
                        session.removeValue(C_PARA_FLAGS);

                        //throw ex;
                        template = "error";
                        xmlTemplateDocument.setData("details", ex.getStackTraceAsString());
                        xmlTemplateDocument.setData("lasturl", lasturl);
                        return startProcessing(cms, xmlTemplateDocument, "", parameters,
                                template);
                    }

                    // everything is done, so remove all session parameters
                    session.removeValue(C_PARA_FILE);
                    session.removeValue(C_PARA_NEWFOLDER);
                    session.removeValue(C_PARA_FLAGS);

                    // return to the calling page
                    try {
                        if(lasturl == null || "".equals(lasturl)) {
                            cms.getRequestContext().getResponse().sendCmsRedirect(getConfigFile(cms).getWorkplaceActionPath() + CmsWorkplaceAction.getExplorerFileUri(cms));
                        }
                        else {
                            cms.getRequestContext().getResponse().sendRedirect(lasturl);
                        }
                    }
                    catch(Exception e) {
                        throw new CmsException("Redirect fails :"
                                + getConfigFile(cms).getWorkplaceActionPath()
                                + CmsWorkplaceAction.getExplorerFileUri(cms), CmsException.C_UNKNOWN_EXCEPTION, e);
                    }
                    return null;
                }
                else {
                    // this is a folder
                    // get all subfolders and files
                    try {
                        cms.moveResource(cms.readAbsolutePath(source), newFolder + newFile);
                    } catch(CmsException e) {
                        // something went wrong, so remove all session parameters
                        session.removeValue(C_PARA_FILE);
                        session.removeValue(C_PARA_NEWFOLDER);
                        session.removeValue(C_PARA_FLAGS);

                        //throw e;
                        template = "error";
                        xmlTemplateDocument.setData("details", e.getStackTraceAsString());
                        xmlTemplateDocument.setData("lasturl", lasturl);
                        return startProcessing(cms, xmlTemplateDocument, "", parameters, template);
                    }

                    // everything is done, so remove all session parameters
                    session.removeValue(C_PARA_FILE);
                    session.removeValue(C_PARA_NEWFOLDER);
                    session.removeValue(C_PARA_FLAGS);
                    xmlTemplateDocument.setData("lasturl", lasturl);
                    template = "update";
                }
            }
        }

        // set the required datablocks
        if(action == null) {
            String title = cms.readProperty(cms.readAbsolutePath(source), C_PROPERTY_TITLE);
            if(title == null) {
                title = "";
            }
//			TODO fix this later
            // CmsUser owner = cms.readOwner(file);
            xmlTemplateDocument.setData("TITLE", Encoder.escapeXml(title));
            xmlTemplateDocument.setData("STATE", getState(cms, source, lang));
            xmlTemplateDocument.setData("OWNER", "" /* Utils.getFullName(owner) */);
            xmlTemplateDocument.setData("GROUP", "" /* cms.readGroup(file).getName() */);
            xmlTemplateDocument.setData("FILENAME", source.getResourceName());
        }

        // process the selected template
        return startProcessing(cms, xmlTemplateDocument, "", parameters, template);
    }

    /**
     * Gets all folders to move the selected file to.
     * <P>
     * The given vectors <code>names</code> and <code>values</code> will
     * be filled with the appropriate information to be used for building
     * a select box.
     * <P>
     * <code>names</code> will contain language specific view descriptions
     * and <code>values</code> will contain the correspondig URL for each
     * of these views after returning from this method.
     * <P>
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param lang reference to the currently valid language file
     * @param names Vector to be filled with the appropriate values in this method.
     * @param values Vector to be filled with the appropriate values in this method.
     * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
     * @return Index representing the available folders.
     * @throws CmsException
     */

    public Integer getFolder(CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values, Hashtable parameters) throws CmsException {
        Integer selected = new Integer(0);
        // get the root folder
        CmsFolder rootFolder = cms.rootFolder();
        // add the root folder
        names.addElement(lang.getLanguageValue("title.rootfolder"));
        values.addElement("/");
        getTree(cms, rootFolder, names, values);
        return selected;
    }

    /**
     * Gets a formated file state string.
     * @param cms The CmsObject.
     * @param file The CmsResource.
     * @param lang The content definition language file.
     * @return Formated state string.
     */

    private String getState(CmsObject cms, CmsResource file, CmsXmlLanguageFile lang) throws CmsException {
        StringBuffer output = new StringBuffer();
        //if(file.inProject(cms.getRequestContext().currentProject())) {
        if (cms.isInsideCurrentProject(file)) {
            int state = file.getState();
            output.append(lang.getLanguageValue("explorer.state" + state));
        }
        else {
            output.append(lang.getLanguageValue("explorer.statenip"));
        }
        return output.toString();
    }

    /**
     * Gets all folders of the filesystem. <br>
     * This method is used to create the selecebox for selecting the target directory.
     * @param cms The CmsObject.
     * @param root The root folder for the tree to be displayed.
     * @param names Vector for storing all names needed in the selectbox.
     * @param values Vector for storing values needed in the selectbox.
     */

    private void getTree(CmsObject cms, CmsFolder root, Vector names, Vector values) throws CmsException {
        List folders = cms.getSubFolders(cms.readAbsolutePath(root));
        //CmsProject currentProject = cms.getRequestContext().currentProject();
        Iterator enu = folders.iterator();
        while(enu.hasNext()) {
            CmsFolder folder = (CmsFolder)enu.next();

            // check if the current folder is part of the current project
            //if(folder.inProject(currentProject)) {
            if (cms.isInsideCurrentProject(folder)) {
                String name = cms.readAbsolutePath(folder);
                name = name.substring(1, name.length() - 1);
                names.addElement(name);
                values.addElement(cms.readAbsolutePath(folder));
            }
            getTree(cms, folder, names, values);
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
}
