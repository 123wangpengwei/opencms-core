/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsExplorerTree.java,v $
 * Date   : $Date: 2000/04/13 22:05:12 $
 * Version: $Revision: 1.4 $
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

package com.opencms.workplace;

import com.opencms.file.*;
import com.opencms.core.*;
import com.opencms.util.*;
import com.opencms.template.*;

import javax.servlet.http.*;

import java.util.*;

/**
 * Template class for displaying the folder tree of the OpenCms workplace.<P>
 * Reads template files of the content type <code>CmsXmlWpTemplateFile</code>.
 * 
 * 
 * @author Michael Emmerich
 * @version $Revision: 1.4 $ $Date: 2000/04/13 22:05:12 $
 */
public class CmsExplorerTree extends CmsWorkplaceDefault implements I_CmsWpConstants  {

    
    /** Definition of the Datablock TREELINK */    
    private static final String C_TREELINK="TREELINK";

     /** Definition of the Datablock TREESTYLE */    
    private static final String C_TREESTYLE="TREESTYLE";

     /** Definition of the Datablock TREETAB */    
    private static final String C_TREETAB="TREETAB";
    
     /** Definition of the Datablock TREEENTRY */    
    private static final String C_TREEENTRY="TREEENTRY";
    
    /** Definition of the Datablock TREEFOLDER */    
    private static final String C_TREEFOLDER="TREEFOLDER";

     /** Definition of the Datablock TREESWITCH */    
    private static final String C_TREESWITCH="TREESWITCH";

     /** Definition of the Datablock TREELINE */    
    private static final String C_TREELINE="TREELINE";
    
     /** Definition of the Datablock TREEIMG_EMPTY0 */    
    private static final String C_TREEIMG_EMPTY0="TREEIMG_EMPTY0";
  
    /** Definition of the Datablock TREEIMG_EMPTY */    
    private static final String C_TREEIMG_EMPTY="TREEIMG_EMPTY";
    
     /** Definition of the Datablock TREEIMG_FOLDEROPEN */    
    private static final String C_TREEIMG_FOLDEROPEN="TREEIMG_FOLDEROPEN";
  
     /** Definition of the Datablock TREEIMG_FOLDERCLOSE */    
    private static final String C_TREEIMG_FOLDERCLOSE="TREEIMG_FOLDERCLOSE";
    
     /** Definition of the Datablock TREEIMG_MEND */    
    private static final String C_TREEIMG_MEND="TREEIMG_MEND";
  
     /** Definition of the Datablock TREEIMG_PEND */    
    private static final String C_TREEIMG_PEND="TREEIMG_PEND";
    
     /** Definition of the Datablock TREEIMG_END */    
    private static final String C_TREEIMG_END="TREEIMG_END";
        
     /** Definition of the Datablock TREEIMG_MCROSS */    
    private static final String C_TREEIMG_MCROSS="TREEIMG_MCROSS";
        
     /** Definition of the Datablock TREEIMG_PCROSS */    
    private static final String C_TREEIMG_PCROSS="TREEIMG_PCROSS";
        
     /** Definition of the Datablock TREEIMG_CROSS */    
    private static final String C_TREEIMG_CROSS="TREEIMG_CROSS";
        
     /** Definition of the Datablock TREEIMG_VERT */    
    private static final String C_TREEIMG_VERT="TREEIMG_VERT";
        
    /** Style for files in a project. */
    private static final String C_FILE_INPROJECT="treefolder";
        
    /** Style for files not in a project. */
    private static final String C_FILE_NOTINPROJECT="treefoldernip";     

    /** Definition of Treelist */    
    private static final String C_TREELIST="TREELIST";

    /** Definition of Treelist */    
    private static final String C_FILELIST="FILELIST";
    
    
    /**
     * Indicates if the results of this class are cacheable.
     * 
     * @param cms A_CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file 
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return <EM>true</EM> if cacheable, <EM>false</EM> otherwise.
     */
    public boolean isCacheable(A_CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
        return false;
    }
    
    /**
     * Overwrites the getContent method of the CmsWorkplaceDefault.<br>
     * Gets the content of the foldertree template and processe the data input.
     * @param cms The CmsObject.
     * @param templateFile The foldertree template file
     * @param elementName not used
     * @param parameters Parameters of the request and the template.
     * @param templateSelector Selector of the template tag to be displayed.
     * @return Bytearre containgine the processed data of the template.
     * @exception Throws CmsException if something goes wrong.
     */
    /*public byte[] getContent(A_CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
         
        CmsXmlWpTemplateFile xmlTemplateDocument = new CmsXmlWpTemplateFile(cms,templateFile);        
      
        // process the selected template
        return startProcessing(cms,xmlTemplateDocument,"",parameters,"template");
    }*/
    
    /**
     * Creates the folder tree in the workplace explorer.
     * @exception Throws CmsException if something goes wrong.
     */
     public Object getTree(A_CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObj) 
            throws CmsException {
            // TODO: check, if this is needed: Hashtable parameters = (Hashtable)userObj;
            StringBuffer output=new StringBuffer();  
            HttpSession session= ((HttpServletRequest)cms.getRequestContext().getRequest().getOriginalRequest()).getSession(true);
            
            String foldername=null;
            String filelist=null;
            String currentFolder;
            String oldFolder;
            String currentFilelist;
            String rootFolder;
            
            //check if a folder parameter was included in the request.
            // if a foldername was included, overwrite the value in the session for later use.
            foldername=cms.getRequestContext().getRequest().getParameter(C_PARA_FOLDER);
            if (foldername != null) {
                session.putValue(C_PARA_FOLDER,foldername);
            }

            // get the current folder to be displayed as maximum folder in the tree.
            currentFolder=(String)session.getValue(C_PARA_FOLDER);
            if (currentFolder == null) {
                 currentFolder=cms.rootFolder().getAbsolutePath();
            }
            
            
            //check if a filelist  parameter was included in the request.
            // if a filelist was included, overwrite the value in the session for later use.
            filelist=cms.getRequestContext().getRequest().getParameter(C_PARA_FILELIST);
            if (filelist != null) {
            session.putValue(C_PARA_FILELIST,filelist);
            }

            // get the current folder to be displayed as maximum folder in the tree.
            currentFilelist=(String)session.getValue(C_PARA_FILELIST);
            if (currentFilelist==null) {
                currentFilelist=cms.rootFolder().getAbsolutePath();
            }          
            
            // get current and root folder
            rootFolder=cms.rootFolder().getAbsolutePath();
            
            //get the template
            CmsXmlWpTemplateFile template=(CmsXmlWpTemplateFile)doc;
            if (filelist!=null) {
                template.setXmlData("PREVIOUS",filelist);
            } else {
                template.setXmlData("PREVIOUS",currentFilelist);  
            }
            
            String tab=template.getProcessedXmlDataValue(C_TREEIMG_EMPTY0,this);
            showTree(cms,rootFolder,currentFolder,currentFilelist,template,output,tab);
            return output.toString();
     }

     
    /**
     * Generates a subtree of the folder tree.
     * @param cms The CmsObject.
     * @param curFolder The rootfolder of ther subtree to display
     * @param endfolder The last folder to be displayed.
     * @param filelist The folder that is displayed in the file list
     * @param template The foldertree template file.
     * @param output The output buffer where all data is written to.
     * @param tab The prefix-HTML code fo this subtree.
     */
    private void showTree(A_CmsObject cms, String curfolder, String endfolder, String filelist,
                          CmsXmlWpTemplateFile template, StringBuffer output,
                          String tab) 
    throws CmsException {
        
        String newtab=new String();
        String folderimg=new String();
        String treeswitch=new String();
        CmsFolder lastFolder=null;
        Vector subfolders=new Vector();
        Vector list=new Vector();
        Vector untestedSubfolders = new Vector();
        
        // remove invisible folders
        Vector untestedlist=cms.getSubFolders(curfolder);
        for (int i=0;i<untestedlist.size();i++) {
            CmsFolder subfolder=(CmsFolder)untestedlist.elementAt(i);
            if (checkAccess(cms,subfolder)) {
                list.addElement(subfolder);
            }
        }     
        
        Enumeration enum =list.elements();
        if (list.size()>0) {
                lastFolder = (CmsFolder)list.lastElement();     
        } else {
                lastFolder=null;
        }
            
        while (enum.hasMoreElements()) { 
                CmsFolder folder=(CmsFolder)enum.nextElement();
                // check if this folder is visible
                if (checkAccess(cms,folder)) {
                   untestedSubfolders=cms.getSubFolders(folder.getAbsolutePath()); 
                    subfolders=new Vector();
                    // now filter all invisible subfolders
                    for (int i=0;i<untestedSubfolders.size();i++) {
                        CmsFolder subfolder=(CmsFolder)untestedSubfolders.elementAt(i);
                        if (checkAccess(cms,subfolder)) {
                            subfolders.addElement(subfolder);
                        }
                    }
                                  
                    // check if this folder must diplayes open
                    if (folder.getAbsolutePath().equals(filelist)) {
                        folderimg=template.getProcessedXmlDataValue(C_TREEIMG_FOLDEROPEN,this);   
                    } else {
                        folderimg=template.getProcessedXmlDataValue(C_TREEIMG_FOLDERCLOSE,this);   
                    }
                    
                    // now check if a treeswitch has to displayed
                    
                    // is this the last element of the current folder, so display the end image
                    if (folder.getAbsolutePath().equals(lastFolder.getAbsolutePath())) {
                        // if there are any subfolders extisintg, use the + or - box
                        if (subfolders.size() >0) {
                            // test if the + or minus must be displayed
                            if (endfolder.startsWith(folder.getAbsolutePath())) {
                                template.setXmlData(C_TREELINK,C_WP_EXPLORER_TREE+"?"+C_PARA_FOLDER+"="+Encoder.escape(curfolder));
                                treeswitch=template.getProcessedXmlDataValue(C_TREEIMG_MEND,this);    
                            } else {
                              template.setXmlData(C_TREELINK,C_WP_EXPLORER_TREE+"?"+C_PARA_FOLDER+"="+Encoder.escape(folder.getAbsolutePath()));
                                treeswitch=template.getProcessedXmlDataValue(C_TREEIMG_PEND,this); 
                            }
                        } else {
                            treeswitch=template.getProcessedXmlDataValue(C_TREEIMG_END,this);              
                        }
                    } else {
                        // use the cross image
                        
                        // if there are any subfolders extisintg, use the + or - box
                        if (subfolders.size() >0) {
                             // test if the + or minus must be displayed
                            if (endfolder.startsWith(folder.getAbsolutePath())) {
                                template.setXmlData(C_TREELINK,C_WP_EXPLORER_TREE+"?"+C_PARA_FOLDER+"="+Encoder.escape(curfolder));
                                treeswitch=template.getProcessedXmlDataValue(C_TREEIMG_MCROSS,this);                          
                            } else {   
                                template.setXmlData(C_TREELINK,C_WP_EXPLORER_TREE+"?"+C_PARA_FOLDER+"="+Encoder.escape(folder.getAbsolutePath()));
                                treeswitch=template.getProcessedXmlDataValue(C_TREEIMG_PCROSS,this);
                            }
                        } else {
                            treeswitch=template.getProcessedXmlDataValue(C_TREEIMG_CROSS,this);
                        }
                    }
    
                    if (folder.getAbsolutePath().equals(lastFolder.getAbsolutePath())) {
                        newtab=tab+template.getProcessedXmlDataValue(C_TREEIMG_EMPTY,this);     
                    } else {
                        newtab=tab+template.getProcessedXmlDataValue(C_TREEIMG_VERT,this);     
                    }
                
                    // test if the folder is in the current project
                    if (folder.inProject(cms.getRequestContext().currentProject())) {
                        template.setXmlData(C_TREESTYLE,C_FILE_INPROJECT);
                    } else {
                          template.setXmlData(C_TREESTYLE,C_FILE_NOTINPROJECT);
                    }

                    // set all data for the treeline tag
                    template.setXmlData(C_FILELIST,C_WP_EXPLORER_FILELIST+"?"+C_PARA_FILELIST+"="+folder.getAbsolutePath());
                    template.setXmlData(C_TREELIST,C_WP_EXPLORER_TREE+"?"+C_PARA_FILELIST+"="+folder.getAbsolutePath());
                    template.setXmlData(C_TREEENTRY,folder.getName());
                    template.setXmlData(C_TREETAB,tab);
                    template.setXmlData(C_TREEFOLDER,folderimg);
                    template.setXmlData(C_TREESWITCH,treeswitch);
                    output.append(template.getProcessedXmlDataValue(C_TREELINE,this));
                
                    //finally process all subfolders if nescessary
                    if (endfolder.startsWith(folder.getAbsolutePath())) {
                        showTree(cms,folder.getAbsolutePath(),endfolder,filelist,template,output,newtab);
                    }
                  }
            }
      }
    
     /** 
      * Check if this resource should be displayed in the filelist.
      * @param cms The CmsObject
      * @param res The resource to be checked.
      * @return True or false.
      * @exception CmsException if something goes wrong.
      */
     private boolean checkAccess(A_CmsObject cms, CmsResource res)
     throws CmsException {
         boolean access=false;
         int accessflags=res.getAccessFlags();
         
         // First check if the user may have access by one of his groups.
         boolean groupAccess = false;
         Enumeration allGroups = cms.getGroupsOfUser(cms.getRequestContext().currentUser().getName()).elements();
         while((!groupAccess) && allGroups.hasMoreElements()) {
             groupAccess = cms.readGroup(res).equals((A_CmsGroup)allGroups.nextElement());
         }
         
         if ( ((accessflags & C_ACCESS_PUBLIC_VISIBLE) > 0) ||
              (cms.readOwner(res).equals(cms.getRequestContext().currentUser()) && (accessflags & C_ACCESS_OWNER_VISIBLE) > 0) ||
              (groupAccess && (accessflags & C_ACCESS_GROUP_VISIBLE) > 0) ||
              (cms.getRequestContext().currentUser().getName().equals(C_USER_ADMIN))) {
             access=true;
         }
         if (res.getState()==C_STATE_DELETED) {
             access=false;
         }
         return access;
     }
  }
