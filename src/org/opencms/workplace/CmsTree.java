/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/Attic/CmsTree.java,v $
 * Date   : $Date: 2003/08/25 10:28:42 $
 * Version: $Revision: 1.1 $
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
import com.opencms.file.CmsProject;
import com.opencms.file.CmsResource;
import com.opencms.flex.jsp.CmsJspActionElement;
import com.opencms.flex.util.CmsUUID;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

/**
 * Generates the tree view for the OpenCms Workplace.<p> 
 * 
 * The following Workplace files use this class:
 * <ul>
 * <li>/jsp/tree_fs.html
 * <li>/jsp/tree_files.html
 * </ul>
 *
 * @author  Alexander Kandzior (a.kandzior@alkacon.com)
 * @version $Revision: 1.1 $
 * 
 * @since 5.1
 */
public class CmsTree extends CmsWorkplace {

    /** Indicates if a complete new tree should be created */
    private boolean m_newTree;

    /** The name of the start folder (or "last known" folder) to be loaded */
    private String m_startFolder;
    
    /** The name of the target folder to be loaded */
    private String m_targetFolder;
    
    /**
     * Public constructor.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsTree(CmsJspActionElement jsp) {
        super(jsp);
    }
    
    /**
     * Creates the output for a tree node.<p>
     *
     * @param name the resource name 
     * @param id the resource id
     * @param parentId the resource parent id
     * @param grey if true, the node is displayed in grey
     * @return the output for a tree node
     */
    private String getNode(String name, CmsUUID id, CmsUUID parentId, boolean grey) {
        StringBuffer result = new StringBuffer(64);
        result.append("parent.aC(");
        // id
        result.append(id.hashCode());
        result.append(",\"");
        // name
        result.append(name);
        result.append("\",");
        // parent id
        result.append(parentId.hashCode());
        result.append(",");
        // project status
        result.append(grey);
        result.append(");\n");    
        return result.toString();    
    }
    
    /**
     * Creates a node entry for the root node of the current site.<p>
     *  
     * @return a node entry for the root node of the current site
     */
    private String getRootNode() {
        CmsResource resource = null;
        String title = null;
        try { 
            resource = getCms().readFolder("/");
            title = getCms().readProperty("/", I_CmsConstants.C_PROPERTY_TITLE);
            if (title == null) {
                getCms().readAbsolutePath(resource);
                title = resource.getFullResourceName();
            }
        } catch (CmsException e) {
            // should not happen
        }
        return getNode(title, resource.getId(), resource.getParentId(), false);
    }
    
    /**
     * Returns the name of the start folder (or "last known" folder) to be loaded.<p>
     *
     * @return the name of the start folder (or "last known" folder) to be loaded
     */
    private String getStartFolder() {
        return m_startFolder;
    }    
    
    /**
     * Returns the target folder name.<p>
     * 
     * @return the target folder name
     */
    private String getTargetFolder() {
        return m_targetFolder;
    }
        
    /**
     * Returns the html for the explorer tree.<p>
     *
     * @return the html for the explorer tree
     */
    public String getTree() {
        // TODO: 
        // 1. Create a version that also runs in a separate window (e.g. copy dialog)
        // 2. Do variation that also loads resources, not only folders
        // 3. Ensure "tree window" javascript is modified everywhere the tree is used

        String targetFolder = getTargetFolder();
        String startFolder = getStartFolder();

        // read the selected folder
        CmsFolder folder;
        try {
            folder = getCms().readFolder(targetFolder);
        } catch (CmsException e) {
            // return with error
            return printError(e);
        }        

        // read the list of project resource to select which resource is "inside" or "outside" 
        List projectResources;
        try {
            projectResources = getCms().readProjectResources(getCms().getRequestContext().currentProject());
        } catch (CmsException e) {
            // use an empty list (all resources are "outside")
            projectResources = new ArrayList();
        }
        
        boolean grey;
        List resources;        
        
        if ((startFolder == null) || (! targetFolder.startsWith(startFolder))) {
            // no (valid) start folder given, just load current folder        
            try {
                resources = getCms().getSubFolders(targetFolder);
            } catch (CmsException e) {
                // return with error
                return printError(e);
            }
        } else {
            // valid start folder given, load all folders between start and current folder
            resources = new ArrayList();
            try {
                resources.addAll(getCms().getSubFolders(startFolder));                                            
                StringTokenizer tok = new StringTokenizer(targetFolder.substring(startFolder.length()), "/");
                while (tok.hasMoreTokens()) {
                    startFolder += tok.nextToken() + "/";
                    resources.addAll(getCms().getSubFolders(startFolder));                         
                }                                             
            } catch (CmsException e) {
                // return with error 
                return printError(e);
            }            
        }

        StringBuffer result = new StringBuffer(2048);
        result.append("function init() {\n");
              
        if (newTree()) {
            // new tree must be reloaded
            result.append("parent.initTree();\n");
            result.append(getRootNode());
        }

        // now output all the tree nodes
        Iterator i = resources.iterator();
        while (i.hasNext()) {          
            CmsResource resource = (CmsResource)i.next();
            grey = !CmsProject.isInsideProject(projectResources, resource);
            result.append(getNode(resource.getResourceName(), resource.getId(), resource.getParentId(), grey));
        }
    
        if (newTree()) {
            // new tree 
            result.append("parent.showTree(parent.tree_display.document, \"");
            result.append(folder.getId().hashCode());
            result.append("\");\n");
        } else {
            // update the current tree with the childs of the selected node
            if (resources.size() == 0) {
                // the node had no childs 
                result.append("parent.setNoChilds(\"");
                result.append(folder.getId().hashCode());
                result.append("\");\n");
            } 
            result.append("parent.showLoadedNodes(parent.tree_display.document,\"");
            result.append(folder.getId().hashCode());
            result.append("\");\n");
        }
        
        result.append("}\n");
        return result.toString();
    }
    
    /**
     * @see org.opencms.workplace.CmsWorkplace#initWorkplaceRequestValues(org.opencms.workplace.CmsWorkplaceSettings, javax.servlet.http.HttpServletRequest)
     */
    protected synchronized void initWorkplaceRequestValues(CmsWorkplaceSettings settings, HttpServletRequest request) {
        boolean rootloaded = "true".equals(request.getParameter("rootloaded"));
        String resource = request.getParameter("resource");
        String lastknown = request.getParameter("lastknown");
        // both "resource" and "lastknown" must be folders
        if (resource != null) {
            if (! resource.endsWith("/")) {
                resource += "/";
            }
            if (resource.startsWith(I_CmsConstants.VFS_FOLDER_SYSTEM + "/") && (! resource.startsWith(getSettings().getSite()))) {
                // restrict access to /system/ 
                resource = "/";   
            }         
        }       
        if ((lastknown != null) && (! lastknown.endsWith("/"))) {
            lastknown += "/";
        }
        if ("/".equals(resource) && !"/".equals(getSettings().getExplorerResource()) && (lastknown == null) && !rootloaded) {
            // direct load of a new tree with subtree (e.g. when returning from an editor)
            lastknown = "/";
            resource = getSettings().getExplorerResource();
            setNewTree(true);
        } else if ("/".equals(resource)) {
            // load new tree if not already loaded
            setNewTree(! rootloaded);
        } else {
            setNewTree(false);
        } 
        
        setTargetFolder(resource);
        setStartFolder(lastknown);
    }

    /**
     * Returns true if a complete new tree must be loaded, false if an existing 
     * tree is updated or extended.<p>
     *  
     * @return true if a complete new tree must be loaded
     */
    private boolean newTree() {
        return m_newTree;
    }
    
    /**
     * Creates error information output.<p>
     * 
     * @param t an error that occured
     * @return error information output
     */
    private String printError(Throwable t) {
        StringBuffer result = new StringBuffer(1024);
        result.append("<!--\n");
        result.append(t.getMessage());
        result.append("-->");        
        result.append("function init() {\n");      
        result.append("}\n");      
        return result.toString();
    }        
    
    /**
     * Sets if a complete tree must be loaded.<p>
     * 
     * @param newTree if true, a complete tree must be loaded
     */
    private void setNewTree(boolean newTree) {
        m_newTree = newTree;
    }
    
    /**
     * Sets the name of the start folder (or "last known" folder) to be loaded.<p>
     * 
     * @param startFolder the name of the start folder (or "last known" folder) to be loaded
     */
    private void setStartFolder(String startFolder) {
        m_startFolder = startFolder;
    }    
    
    /**
     * Sets the target folder name.<p>
     * 
     * @param targetFolder the target folder name
     */
    private void setTargetFolder(String targetFolder) {
        m_targetFolder = targetFolder;
    }
}
