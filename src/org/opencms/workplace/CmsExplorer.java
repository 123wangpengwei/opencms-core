/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/Attic/CmsExplorer.java,v $
 * Date   : $Date: 2003/07/02 11:03:13 $
 * Version: $Revision: 1.3 $
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
package org.opencms.workplace;

import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.file.CmsFolder;
import com.opencms.file.CmsObject;
import com.opencms.file.CmsProject;
import com.opencms.file.CmsResource;
import com.opencms.flex.jsp.CmsJspActionElement;
import com.opencms.flex.util.CmsUUID;
import com.opencms.util.Encoder;
import com.opencms.workplace.I_CmsWpConstants;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

/**
 * Provides methods for building the main framesets of the OpenCms Workplace.<p> 
 * 
 * The following files use this class:
 * <ul>
 * <li>/jsp/explorer_html
 * <li>/jsp/explorer_files.html
 * </ul>
 *
 * @author  Alexander Kandzior (a.kandzior@alkacon.com)
 * @version $Revision: 1.3 $
 * 
 * @since 5.1
 */
public class CmsExplorer extends CmsWorkplace {
    
    private static final int C_ENTRYS_PER_PAGE = 50;
    
    /**
     * Public constructor.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsExplorer(CmsJspActionElement jsp) {
        super(jsp);
    }
    
    /**
     * @see org.opencms.workplace.CmsWorkplace#initWorkplaceRequestValues(org.opencms.workplace.CmsWorkplaceSettings, javax.servlet.http.HttpServletRequest)
     */
    protected synchronized void initWorkplaceRequestValues(CmsWorkplaceSettings settings, HttpServletRequest request) {
        String mode = request.getParameter("mode");
        settings.setExplorerMode(mode);

        String currentFolder = request.getParameter("folder");
        if ((currentFolder != null) && (currentFolder.startsWith("vfslink:"))) {
            // this is a link check, remove the prefix
            settings.setExplorerMode("vfslink");
            settings.setExplorerFolder(currentFolder.substring(8));
        } else {
            if ((currentFolder != null) && (!"".equals(currentFolder)) && folderExists(getCms(), currentFolder)) {
                settings.setExplorerFolder(currentFolder);                
            } else {
                currentFolder = settings.getExplorerFolder();
                if ((currentFolder == null) || (!folderExists(getCms(), currentFolder))) {
                    currentFolder = "/";
                    settings.setExplorerFolder(currentFolder);                
                }
            }
        }
        
        String selPage = request.getParameter("selPage");
        if(selPage != null) {
            int page = 1;
            try {
                page = Integer.parseInt(selPage);
            } catch (NumberFormatException e) {
                // defaul is 1
            }
            settings.setExplorerPage(page);
        }        
        
        // are context menues enabled?
        settings.setExplorerContext(! "false".equals(request.getParameter("kontext")));
        
        // the flaturl 
        settings.setExplorerFlaturl(request.getParameter("flaturl"));
        
        // the checksum
        int checksum = -1;
        String check = request.getParameter("check");
        if(check != null) {
            try {
                checksum = Integer.parseInt(check);
            } catch (NumberFormatException e) {
                // defaul is -1
            }
        }        
        settings.setExplorerChecksum(checksum);

    }
    
    /**
     * Checks if a folder with a given name exits in the VFS.<p>
     * 
     * @param getCms() the current cms context
     * @param folder the folder to check for
     * @return true if the folder exists in the VFS
     */
    private boolean folderExists(CmsObject cms, String folder) {
        try {
            CmsFolder test = cms.readFolder(folder);
            if (test.isFile()){
                return false;
            }
            return true;            
        } catch(Exception e) {
            return false;
        }
    }    
    
    /**
     * Sets the default preferences for the current user if those values are not available.<p>
     * 
     * @param cms the current cms context
     * @return the int value of the default preferences
     */
    private int getDefaultPreferences(CmsObject cms) {
        int filelist;
        String explorerSettings = (String)getSettings().getUser().getAdditionalInfo(I_CmsConstants.C_ADDITIONAL_INFO_EXPLORERSETTINGS);
        if(explorerSettings != null) {
            filelist = new Integer(explorerSettings).intValue();
        }else {
            filelist = I_CmsWpConstants.C_FILELIST_NAME 
                + I_CmsWpConstants.C_FILELIST_TITLE
                + I_CmsWpConstants.C_FILELIST_TYPE
                + I_CmsWpConstants.C_FILELIST_CHANGED;
        }
        return filelist;
    }
        
    /**
     * Returns the html for the explorer file list.<p>
     *
     * @return the html for the explorer file list
     */
    public String getFileListFunction() {
        
        // if mode is "listonly", only the list will be shown
        boolean listonly = "listonly".equals(getSettings().getExplorerMode()); 
        // if mode is "projectview", all changed files in that project will be shown
        boolean projectView = "projectview".equals(getSettings().getExplorerMode());
        // if mode is "vfslinks", the vfs links to a target file will be shown
        boolean vfslinkView = "vfslink".equals(getSettings().getExplorerMode());
        
        String currentFolder = getSettings().getExplorerFolder();
        if (vfslinkView) {            
            boolean found = true;
            try {
                getCms().readFileHeader(currentFolder);
            } catch (CmsException e) {
                // file was not readable
                found = false;
            }
            if (found) {
                // file / folder exists and is readable
                currentFolder = "vfslink:" + currentFolder;
            } else {
                // show the root folder in case of an error and reset the state
                currentFolder = "/";
                vfslinkView = false;
            }          
        }
        
        long check = getCms().getFileSystemFolderChanges();
        boolean newTreePlease = 
            getSettings().getExplorerChecksum() != check;

        // get the currentFolder Id
        CmsUUID currentFolderId = CmsUUID.getNullUUID();
        if (! vfslinkView) {
            try {
                currentFolderId = (getCms().readFolder(currentFolder)).getId();
            } catch (CmsException e) {
                // we use the null UUID
            }
        } 
        
        // start creating content
        StringBuffer content = new StringBuffer(2048);
        content.append("function initialize() {\n");

        if(listonly) {
            content.append("top.openfolderMethod='openthisfolderflat';\n");
        } else {
            content.append("top.openfolderMethod='openthisfolder';\n");
        }
        
        if(projectView || vfslinkView) {
            content.append("top.projectView=true;\n");
        } else {
            content.append("top.projectView=false;\n");
        }

        // show kontext
        if(getSettings().getExplorerContext()) {
            content.append("top.showKon=true;\n");
        } else {
            content.append("top.showKon=false;\n");
        }

        // the flaturl
        if(getSettings().getExplorerFlaturl() != null) {
            content.append("top.flaturl='");
            content.append(getSettings().getExplorerFlaturl());
            content.append("';\n");
        } else if (!listonly){
            content.append("top.flaturl='';\n");
        }

        // the help_url
        content.append("top.head.helpUrl='explorer/index.html';\n");
        // the project
        content.append("top.setProject(");
        content.append(getSettings().getProject());
        content.append(");\n");
        // the onlineProject
        content.append("top.setOnlineProject(");
        content.append(I_CmsConstants.C_PROJECT_ONLINE_ID);
        content.append(");\n");
        // set the checksum for the tree
        content.append("top.setChecksum(");
        content.append(check);
        content.append(");\n");
        // set the writeAccess for the current Folder
        boolean writeAccess = true;
        if (! vfslinkView) {        
            try {
                CmsFolder test = getCms().readFolder(currentFolder);
                writeAccess = test.getProjectId() == getSettings().getProject();
            } catch (CmsException e) {
                writeAccess = false;
            }
        }
        content.append("top.enableNewButton(");
        content.append(writeAccess);
        content.append(");\n");
        // the folder
        content.append("top.setDirectory(\"");
        content.append(currentFolderId.hashCode());
        content.append("\",\"");
        content.append(currentFolder);
        content.append("\");\n");
        content.append("top.rD();\n\n");

        // now look which filelist colums we want to show
        int filelist = getDefaultPreferences(getCms());
        boolean showTitle = (filelist & I_CmsWpConstants.C_FILELIST_TITLE) > 0;
        boolean showDateChanged = (filelist & I_CmsWpConstants.C_FILELIST_CHANGED) > 0;
        boolean showOwner = (filelist & I_CmsWpConstants.C_FILELIST_OWNER) > 0;
        boolean showGroup = (filelist & I_CmsWpConstants.C_FILELIST_GROUP) > 0;
        boolean showSize = (filelist & I_CmsWpConstants.C_FILELIST_SIZE) > 0;

        // now get the entries for the filelist
        Vector resources = getRessources(getCms(), getSettings().getExplorerMode(), getSettings().getExplorerFolder());

        // if a folder contains to much entrys we split them to pages of C_ENTRYS_PER_PAGE length
        int startat = 0;
        int stopat = resources.size();
        int selectedPage = 1;
        int numberOfPages = 0;
        int maxEntrys = C_ENTRYS_PER_PAGE;
        
        if (!(listonly || projectView || vfslinkView)) {
            selectedPage = getSettings().getExplorerPage();
            if (stopat > maxEntrys) {
                // we have to splitt
                numberOfPages = (stopat / maxEntrys) + 1;
                if (selectedPage > numberOfPages) {
                    // the user has changed the folder and then selected a page for the old folder
                    selectedPage = 1;
                }
                startat = (selectedPage - 1) * maxEntrys;
                if ((startat + maxEntrys) < stopat){
                    stopat = startat + maxEntrys;
                }
            }
        }

        for (int i = startat;i < stopat;i++) {
            CmsResource res = (CmsResource)resources.elementAt(i);
            content.append("top.aF(");
            // the name
            content.append("\"");
            content.append(res.getName());
            content.append("\",");
            // the path
            if(projectView || vfslinkView){
                content.append("\"");
                content.append(res.getPath());
                content.append("\",");
            }else{
                //is taken from top.setDirectory
                content.append("\"\",");
            }
            // the title
            if(showTitle){
                String title = "";
                try {
                    title = getCms().readProperty(getCms().readAbsolutePath(res), I_CmsConstants.C_PROPERTY_TITLE);
                }catch(CmsException e) {
                }
                if(title == null) {
                    title = "";
                }
                content.append("\"");
                if (title != null) content.append(Encoder.escapeHtml(title));
                content.append("\",");
                
            }else{
                content.append("\"\",");
            }
            // the type
            content.append(res.getType());
            content.append(",");
            // date of last change
            if(showDateChanged){
                content.append("\"");
                content.append(getSettings().getMessages().getDateTime(res.getDateLastModified()));
                content.append("\",");
                
            }else{
                content.append("\"\",");
            }
            // unused field, was intended for the user who changed the resource last
            content.append("\"\",");
            // date
            // unused field, was intended for: content.append("\"" + Utils.getNiceDate(res.getDateCreated()) + "\",");
            content.append("\"\",");
            // size
            if(res.isFolder() || (!showSize)) {
                content.append("\"\",");
            }else {
                content.append(res.getLength());
                content.append(",");                
            }
            // state
            content.append(res.getState());
            content.append(",");            
            // project
            content.append(res.getProjectId());
            content.append(",");            
            // owner
            if(showOwner){
                content.append("\"");                
                try {
                    content.append(getCms().readUser(res.getOwnerId()).getName());
                } catch (CmsException e) {
                    content.append(e.getMessage());
                }
                content.append("\",");                
            }else{
                content.append("\"\",");
            }
            // group
            if(showGroup){
                content.append("\"");  
                try {              
                    content.append(getCms().readGroup(res).getName());
                } catch (CmsException e) {
                   content.append(e.getMessage());
                }
                content.append("\",");                
            }else{
                content.append("\"\",");
            }
            // accessFlags
            content.append(res.getAccessFlags());
            content.append(",");            
            // locked by
            if(res.isLockedBy().isNullUUID()) {
                content.append("\"\",");
            }else {
                content.append("\"");                
                try {
                    content.append(getCms().lockedBy(res).getName());
                } catch (CmsException e) {
                    content.append(e.getMessage());
                }
                content.append("\",");                
            }
            // locked in project
            int lockedInProject = res.getLockedInProject();
            String lockedInProjectName = "";
            try {
                lockedInProjectName = getCms().readProject(lockedInProject).getName();
            } catch(CmsException exc) {
                // ignore the exception - this is an old project so ignore it
            }
            content.append("\"");            
            content.append(lockedInProjectName);
            content.append("\",");
            content.append(lockedInProject);
            content.append(");\n");
        }

        //  now the tree, only if changed
        if(newTreePlease && (!(listonly || vfslinkView))) {
            content.append("\ntop.rT();\n");
            List tree = null;
            try {
                tree = getCms().getFolderTree();
            } catch (CmsException e) {
                tree = new Vector();
            }
            int startAt = 1;
            CmsUUID parentId = CmsUUID.getNullUUID();
            boolean grey = false;

            if (CmsProject.isOnlineProject(getSettings().getProject())) {
                // all easy: we are in the onlineProject
                CmsFolder rootFolder = (CmsFolder)tree.get(0);
                content.append("top.aC(\"");
                content.append(rootFolder.getId().hashCode());
                content.append("\", ");
                content.append("\"");
                content.append(getSettings().getMessages().key("title.rootfolder"));
                content.append("\", \"");
                content.append(rootFolder.getParentId().hashCode());
                content.append("\", false);\n");
                for(int i = startAt;i < tree.size();i++) {
                    CmsFolder folder = (CmsFolder)tree.get(i);
                    content.append("top.aC(\"");
                    // id
                    content.append(folder.getId().hashCode());
                    content.append("\", ");
                    // name
                    content.append("\"");
                    content.append(folder.getName());
                    content.append("\", \"");
                    // parentId
                    content.append(folder.getParentId().hashCode());
                    content.append("\", false);\n");                    
                }
            } else {
                // offline Project
                Hashtable idMixer = new Hashtable();
                CmsFolder rootFolder = (CmsFolder)tree.get(0);
                String folderToIgnore = null;
                if(! CmsProject.isOnlineProject(rootFolder.getProjectId())) {
                    //startAt = 2;
                    grey = false;
                    idMixer.put((CmsFolder)tree.get(1), rootFolder.getId());
                }else {
                    grey = true;
                }
                content.append("top.aC(\"");
                content.append(rootFolder.getId().hashCode());
                content.append("\", ");
                content.append("\"");
                content.append(getSettings().getMessages().key("title.rootfolder"));
                content.append("\", \"");
                content.append(rootFolder.getParentId().hashCode());
                content.append("\", ");
                content.append(grey);
                content.append(");\n");
                for(int i = startAt;i < tree.size();i++) {
                    CmsFolder folder = (CmsFolder)tree.get(i);
                    if((folder.getState() == I_CmsConstants.C_STATE_DELETED) || (getCms().readAbsolutePath(folder).equals(folderToIgnore))) {

                        // if the folder is deleted - ignore it and the following online res
                        folderToIgnore = getCms().readAbsolutePath(folder);
                    }else {
                        if(! CmsProject.isOnlineProject(folder.getProjectId())) {
                            grey = false;
                            parentId = folder.getParentId();
                            try {
                                // the next res is the same res in the online-project: ignore it!
                                if(getCms().readAbsolutePath(folder).equals(getCms().readAbsolutePath((CmsFolder)tree.get(i + 1)))) {
                                    i++;
                                    idMixer.put((CmsFolder)tree.get(i), folder.getId());
                                }
                            }catch(IndexOutOfBoundsException exc) {
                            // ignore the exception, this was the last resource
                            }
                        } else {
                            grey = true;
                            parentId = folder.getParentId();
                            if(idMixer.containsKey(parentId)) {
                                parentId = (CmsUUID) idMixer.get(parentId);
                            }
                        }
                        content.append("top.aC(\"");
                        // id
                        content.append(folder.getId().hashCode());
                        content.append("\", ");
                        // name
                        content.append("\"");
                        content.append(folder.getName());
                        content.append("\", \"");
                        // parentId
                        content.append(parentId.hashCode());
                        content.append("\", ");
                        content.append(grey);
                        content.append(");\n");
                    }
                }
            }
        }
        
        if(listonly || projectView) {
            // only show the filelist
            content.append("top.dUL(document);\n");
        } else {
            // update all frames
            content.append("top.dU(document,");
            content.append(numberOfPages);
            content.append(",");
            content.append(selectedPage);
            content.append("); \n");
        }
        
        content.append("}\n");
        return content.toString();
    }
    
    /**
     * Returns the javascript initialize call for the filelist.<p>
     *  
     * @return the javascript initialize call for the filelist
     */
    public String getFileListInitializer() {
        return "initialize();";
    }
    
    /**
     * Get the resources in the folder stored in parameter param
     * or in the project shown in the projectview
     *
     * @param getCms() the current getCms() context
     * @param mode the mode to use
     * @param folder the fodler to read the files from
     * @return a vector with ressources to display
     */
    private Vector getRessources(CmsObject cms, String mode, String folder) {
        
        if ("vfslink".equals(mode)) {
            try {
                return new Vector(getCms().fetchVfsLinksForResource(folder));
            } catch (CmsException e) {
                return new Vector();
            }
        } else {
            try {
                return getCms().getResourcesInFolder(folder);
            } catch (CmsException e) {
                return new Vector();
            }                
        }
                
        /*
        // String mode = (String)parameters.get("mode")!=null?(String)parameters.get("mode"):"";        
        // String submode = (String)parameters.get("submode")!=null?(String)parameters.get("submode"):"";
        
        if("projectview".equals(mode)) {
            
            
            // I_CmsSession session = getCms().getRequestContext().getSession(true);
            
            
            if("search".equals(submode)){
                Vector resources = new Vector();
                // String currentFilter = (String)session.getValue("ocms_search.currentfilter");
                CmsSearchFormObject searchForm = null;
                if(currentFilter != null){
                    searchForm = (CmsSearchFormObject)((Hashtable)session.getValue("ocms_search.allfilter")).get(currentFilter);
                    if((currentFilter != null) && (searchForm != null)){
                        // flag for using lucene for search
                        I_CmsRegistry registry = getCms().getRegistry();
                        boolean luceneEnabled = "on".equals(registry.getSystemValue("searchbylucene"));
                        if("property".equals(currentFilter)){
                            String definition = searchForm.getValue02();
                            String value = searchForm.getValue03();
                            int type = Integer.parseInt(searchForm.getValue01());
                            resources = getCms().getVisibleResourcesWithProperty(definition, value, type);
                        } else if ("filename".equals(currentFilter)){
                            String filename = searchForm.getValue01();
                            // if lucene is enabled the use lucene for searching by filename
                            // else use the method that reads from the database
                            if(luceneEnabled){
                                // put here the lucene search for filenames
                            } else {
                                resources = getCms().readResourcesLikeName(filename);
                            }
                        } else if ("content".equals(currentFilter)){
                            // this search is only available if lucene is enabled
                            searchForm.getValue01();
                        }
                    }
                }
                // remove the channel resources
                for(int i=0; i<resources.size(); i++){
                    CmsResource curRes = (CmsResource)resources.elementAt(i);
                    if(curRes.getResourceName().startsWith(getCms().getRequestContext().getSiteName()+CmsObject.C_ROOTNAME_COS)){
                        resources.remove(i);
                    }
                }
                return resources;
            } else {
                String filter = new String();
                filter = (String) session.getValue("filter");
                String projectId = (String) session.getValue("projectid");
                int currentProjectId;
                if(projectId == null || "".equals(projectId)){
                    currentProjectId = getCms().getRequestContext().currentProject().getId();
                } else {
                    currentProjectId = Integer.parseInt(projectId);
                }
                session.removeValue("filter");
                return getCms().readProjectView(currentProjectId, filter);
            }
        } else 
        */       
    }   
}
