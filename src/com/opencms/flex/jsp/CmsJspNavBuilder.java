/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/flex/jsp/Attic/CmsJspNavBuilder.java,v $
 * Date   : $Date: 2003/06/05 19:02:04 $
 * Version: $Revision: 1.8 $
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
 
package com.opencms.flex.jsp;

import com.opencms.file.CmsFile;
import com.opencms.file.CmsObject;
import com.opencms.file.CmsResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * Bean to provide a convenient way to build navigation structures based on 
 * {@link com.opencms.flex.jsp.CmsJspNavElement}.<p>
 *
 * @author  Alexander Kandzior (a.kandzior@alkacon.com)
 * @version $Revision: 1.8 $
 * 
 * @see com.opencms.flex.jsp.CmsJspNavElement
 * 
 * @since 5.0
 */
public class CmsJspNavBuilder {
    
    // Member variables
    private CmsObject m_cms = null;
    private String m_requestUri = null;
    private String m_requestUriFolder = null;

    /**
     * Empty constructor, so that this bean can be initialized from a JSP.<p> 
     * 
     * @see java.lang.Object#Object()
     */    
    public CmsJspNavBuilder() {}

    /**
     * Default constructor.<p>
     * 
     * @param cms context provider for the current request
     */
    public CmsJspNavBuilder(CmsObject cms) { 
        init(cms);
    }
        
    /**
     * Initiliazes this bean.<p>
     * 
     * @param cms context provider for the current request
     */
    public void init(CmsObject cms) {
        m_cms = cms;
        m_requestUri = m_cms.getRequestContext().getUri();
        m_requestUriFolder = CmsResource.getPath(m_requestUri);
    }

    /**
     * Returns a CmsJspNavElement for the resource of the current request URI.<p>
     *  
     * @return CmsJspNavElement a CmsJspNavElement for the resource of the current request URI
     */
    public CmsJspNavElement getNavigationForResource() {
        return getNavigationForResource(m_cms, m_requestUri);
    }  
    
    /**
     * Returns a CmsJspNavElement for the named resource.<p>
     * 
     * @param resource the resource name to get the nav information for, 
     * must be a full path name, e.g. "/docs/index.html".
     * @return CmsJspNavElement a CmsJspNavElement for the given resource
     */
    public CmsJspNavElement getNavigationForResource(String resource) {
        return getNavigationForResource(m_cms, resource);
    }  
    
    /**
     * Returns a CmsJspNavElement for the named resource.<p>
     * 
     * @param cms context provider for the current request
     * @param resource the resource name to get the nav information for, 
     * must be a full path name, e.g. "/docs/index.html".
     * @return a CmsJspNavElement for the given resource
     */
    public static CmsJspNavElement getNavigationForResource(CmsObject cms, String resource) {
        Map properties;
        try {
            properties = cms.readProperties(resource);
        } catch (Exception e) {
            return null;
        }
        int level =  CmsResource.getPathLevel(resource);
        if (resource.endsWith("/")) level--;
        return new CmsJspNavElement(resource, properties, level);
    }    
 
    /**
     * Collect all navigation elements from the files of the folder of the current request URI,
     * navigation elements are of class CmsJspNavElement.<p>
     *
     * @return a sorted (ascending to nav position) ArrayList of navigation elements.
     */    
    public ArrayList getNavigationForFolder() {
        return getNavigationForFolder(m_cms, m_requestUriFolder);
    }
    
    /**
     * Collect all navigation elements from the files in the given folder,
     * navigation elements are of class CmsJspNavElement.<p>
     *
     * @param folder the selected folder
     * @return A sorted (ascending to nav position) ArrayList of navigation elements.
     */    
    public ArrayList getNavigationForFolder(String folder) {
        return getNavigationForFolder(m_cms, folder);
    }
        
    /**
     * Collect all navigation elements from the files in the given folder,
     * navigation elements are of class CmsJspNavElement.<p>
     *
     * @param cms context provider for the current request
     * @param folder the selected folder
     * @return a sorted (ascending to nav position) ArrayList of navigation elements
     */    
    public static ArrayList getNavigationForFolder(CmsObject cms, String folder) {
        folder = CmsFile.getPath(folder);
        ArrayList list = new ArrayList();
        Vector v = null, dir = null;
        try {
            // v = cms.getResourcesInFolder(folder);        
            v = cms.getFilesInFolder(folder);
            dir = cms.getSubFolders(folder);
        } catch (Exception e) {
            return new ArrayList(0);
        }        
        v.addAll(dir);
        
        Iterator i = v.iterator();
        while (i.hasNext()) {
            CmsResource r = (CmsResource)i.next();
            if (r.getState() != CmsResource.C_STATE_DELETED) {
                CmsJspNavElement element = getNavigationForResource(cms, r.getAbsolutePath());
                if ((element != null) && element.isInNavigation()) {
                    list.add(element);
                }
            }            
        }
        Collections.sort(list);
        return list;
    }
    
    /** 
     * Build a navigation for the folder that is either minus levels up 
     * from of the folder of the current request URI, or that is plus levels down from the 
     * root folder towards the current request URI.<p> 
     * 
     * If level is set to zero the root folder is used by convention.<p>
     * 
     * @param level if negative, walk this many levels up, if positive, walk this many 
     * levels down from root folder 
     * @return a sorted (ascending to nav position) ArrayList of navigation elements
     */
    public ArrayList getNavigationForFolder(int level) {
        return getNavigationForFolder(m_cms, m_requestUriFolder, level);
    }    

    /** 
     * Build a navigation for the folder that is either minus levels up 
     * from the given folder, or that is plus levels down from the 
     * root folder towards the given folder.<p> 
     * 
     * If level is set to zero the root folder is used by convention.<p>
     * 
     * @param folder the selected folder
     * @param level if negative, walk this many levels up, if positive, walk this many 
     * levels down from root folder 
     * @return a sorted (ascending to nav position) ArrayList of navigation elements
     */
    public ArrayList getNavigationForFolder(String folder, int level) {
        return getNavigationForFolder(m_cms, folder, level);
    }    
    
    /** 
     * Build a navigation for the folder that is either minus levels up 
     * from the given folder, or that is plus levels down from the 
     * root folder towards the given folder.<p> 
     * 
     * If level is set to zero the root folder is used by convention.<p>
     * 
     * @param cms context provider for the current request
     * @param folder the selected folder
     * @param level if negative, walk this many levels up, if positive, walk this many 
     * levels down from root folder 
     * @return a sorted (ascending to nav position) ArrayList of navigation elements
     */
    public static ArrayList getNavigationForFolder(CmsObject cms, String folder, int level) {
        folder = CmsFile.getPath(folder);
        // If level is one just use root folder
        if (level == 0) return getNavigationForFolder(cms, "/");
        String navfolder = CmsResource.getPathPart(folder, level);
        // If navfolder found use it to build navigation
        if (navfolder != null) return getNavigationForFolder(cms, navfolder);
        // Nothing found, return empty list
        return new ArrayList(0);
    }

    /**
     * @see #getNavigationTreeForFolder(CmsObject, String, int, int) 
     */    
    public ArrayList getNavigationTreeForFolder(int startlevel, int endlevel) {
        return getNavigationTreeForFolder(m_cms, m_requestUriFolder, startlevel, endlevel);
    }

    /**
     * @see #getNavigationTreeForFolder(CmsObject, String, int, int) 
     */
    public ArrayList getNavigationTreeForFolder(String folder, int startlevel, int endlevel) {
        return getNavigationTreeForFolder(m_cms, folder, startlevel, endlevel);
    }

    /**
     * Builds a tree navigation for the folders between the provided start and end level.<p>
     * 
     * A tree navigation includes all nav elements that are required to display a tree structure.
     * However, the data structure is a simple list.
     * Each of the nav elements in the list has the {@link CmsJspNavElement#getNavTreeLevel()} set
     * to the level it belongs to. Use this information to distinguish between the nav levels.<p>
     * 
     * @param cms context provider for the current request
     * @param folder the selected folder
     * @param startlevel the start level
     * @param endlevel the end level
     * @return a sorted list of nav elements with the nav tree level property set 
     */
    public static ArrayList getNavigationTreeForFolder(CmsObject cms, String folder, int startlevel, int endlevel) {
        folder = CmsFile.getPath(folder);
        // Make sure start and end level make sense
        if (endlevel < startlevel) return new ArrayList(0);
        int currentlevel = CmsResource.getPathLevel(folder);
        if (currentlevel < endlevel) endlevel = currentlevel;
        if (startlevel == endlevel) return getNavigationForFolder(cms, CmsResource.getPathPart(folder, startlevel), startlevel);
     
        ArrayList result = new ArrayList(0);
        Iterator it = null;
        float parentcount = 0;
        
        for (int i=startlevel; i<=endlevel; i++) {
            String currentfolder = CmsResource.getPathPart(folder, i);            
            ArrayList entries = getNavigationForFolder(cms, currentfolder);            
            // Check for parent folder
            if (parentcount > 0) {                
                it = entries.iterator();          
                while (it.hasNext()) {
                    CmsJspNavElement e = (CmsJspNavElement)it.next();
                    e.setNavPosition(e.getNavPosition() + parentcount);
                }                  
            }
            // Add new entries to result
            result.addAll(entries);
            Collections.sort(result);                      
            // Finally spread the values of the nav items so that there is enough room for further items.
            float pos = 0;
            int count = 0;            
            it = result.iterator();
            String nextfolder = CmsResource.getPathPart(folder, i+1);
            parentcount = 0;
            while (it.hasNext()) {
                pos = 10000 * (++count);
                CmsJspNavElement e = (CmsJspNavElement)it.next();
                e.setNavPosition(pos);
                if (e.getResourceName().startsWith(nextfolder)) parentcount = pos;
            }            
            if (parentcount == 0) parentcount = pos;
        }
        return result;
    }
    
    /**
     * @see #getNavigationBreadCrumb(String, int, int, boolean) 
     */
    public ArrayList getNavigationBreadCrumb() {
        return getNavigationBreadCrumb(m_requestUriFolder, 0, -1, true);
    }
    
    /**
     * @see #getNavigationBreadCrumb(String, int, int, boolean) 
     */
    public ArrayList getNavigationBreadCrumb(int startlevel, int endlevel) {
        return getNavigationBreadCrumb(m_requestUriFolder, startlevel, endlevel, true);
    }
    
    /**
     * @see #getNavigationBreadCrumb(String, int, int, boolean) 
     */
    public ArrayList getNavigationBreadCrumb(int startlevel, boolean currentFolder) {
        return getNavigationBreadCrumb(m_requestUriFolder, startlevel, -1, currentFolder);
    }
    
    /** 
     * Build a "bread crump" path navigation to the given folder.<p>
     * 
     * The startlevel marks the point where the navigation starts from, if negative, 
     * the count of steps to go down from the given folder.
     * The endlevel is the maximum level of the navigation path, set it to -1 to build the
     * complete navigation to the given folder.
     * You can include the given folder in the navigation by setting currentFolder to true,
     * otherwise false.<p> 
     * 
     * @param folder the selected folder
     * @param startlevel the start level, if negative, go down |n| steps from selected folder
     * @param endlevel the end level, if -1, build navigation to selected folder
     * @param currentFolder include the selected folder in navigation or not
     * @return ArrayList sorted list of navigation elements
     */
    public ArrayList getNavigationBreadCrumb(String folder, int startlevel, int endlevel, boolean currentFolder) {
        ArrayList result = new ArrayList(0);
               
        int level =  CmsResource.getPathLevel(folder);
        // decrease folder level if current folder is not displayed
        if (!currentFolder) {
            level -= 1;
        }
        // check current level and change endlevel if it is higher or -1
        if (level < endlevel || endlevel == -1) {
            endlevel = level;
        }
        
        // if startlevel is negative, display only |startlevel| links
        if (startlevel < 0) {
            startlevel = endlevel + startlevel +1;
            if (startlevel < 0) {
                startlevel = 0;
            }
        }
        
        // create the list of navigation elements     
        for (int i=startlevel; i<=endlevel; i++) {
            String navFolder = CmsResource.getPathPart(folder, i);
            CmsJspNavElement e = getNavigationForResource(navFolder);
            // add element to list
            result.add(e);
        }
        
        return result;
    }
    

    /**
     * Returns all subfolders of a sub channel that has 
     * the given parent channel, or an empty array if 
     * that combination does not exist or has no subfolders.<p>
     * 
     * @param parentChannel the parent channel
     * @param subChannel the sub channel
     * @return an unsorted list of CmsResources
     */
    public ArrayList getChannelSubFolders(String parentChannel, String subChannel) {
        return getChannelSubFolders(m_cms, parentChannel, subChannel);
    }    
    
    /**
     * Returns all subfolders of a sub channel that has 
     * the given parent channel, or an empty array if 
     * that combination does not exist or has no subfolders.<p>
     * 
     * @param cms context provider for the current request
     * @param parentChannel the parent channel
     * @param subChannel the sub channel
     * @return an unsorted list of CmsResources
     */
    public static ArrayList getChannelSubFolders(CmsObject cms, String parentChannel, String subChannel) {
        String channel = null;
        if (subChannel == null) {
            subChannel = "";
        } else if (subChannel.startsWith("/")) {
            subChannel = subChannel.substring(1);
        }
        if (parentChannel == null) parentChannel = "";        
        if (parentChannel.endsWith("/")) {
            channel = parentChannel + subChannel;
        } else {
            channel = parentChannel + "/" + subChannel;
        }
        return getChannelSubFolders(cms, channel);
    }
    
    /**
     * Returns all subfolders of a channel, or an empty array if 
     * the folder does not exist or has no subfolders.<p>
     * 
     * @param channel the channel to look for subfolders in
     * @return an unsorted list of CmsResources
     */    
    public ArrayList getChannelSubFolders(String channel) {
        return getChannelSubFolders(m_cms, channel);
    }

    /**
     * Returns all subfolders of a channel, or an empty array if 
     * the folder does not exist or has no subfolders.<p>
     * 
     * @param cms context provider for the current request
     * @param channel the channel to look for subfolders in
     * @return an unsorted list of CmsResources
     */    
    public static ArrayList getChannelSubFolders(CmsObject cms, String channel) {
        if (! channel.startsWith("/")) channel = "/" + channel;
        if (! channel.endsWith("/")) channel += "/";    

        // Now read all subchannels of this channel    
        java.util.Vector subChannels = new java.util.Vector();  
        try {
            cms.setContextToCos();
            subChannels = cms.getSubFolders(channel);
        } catch (Exception e) {
            System.err.println("Exception: " + e);
        } finally {
            cms.setContextToVfs();
        }           
        
        // Create an ArrayList out of the Vector        
        java.util.ArrayList list = new java.util.ArrayList(subChannels.size());
        list.addAll(subChannels);        
        return list;
    }

    /**
     * Returns all subfolders of a channel, 
     * sorted by "Title" property ascending, or an empty array if 
     * the folder does not exist or has no subfolders.
     * 
     * @param channel the parent channel
     * @param subChannel the sub channel
     * @return a sorted list of CmsResources
     */    
    public ArrayList getChannelSubFoldersSortTitleAsc(String channel, String subChannel) {
        return getChannelSubFoldersSortTitleAsc(m_cms, channel, subChannel);
    }    
    
    /**
     * Returns all subfolders of a channel, 
     * sorted by "Title" property ascending, or an empty array if 
     * the folder does not exist or has no subfolders.
     * 
     * @param cms context provider for the current request
     * @param channel the parent channel
     * @param subChannel the sub channel
     * @return a sorted list of CmsResources
     */
    public static ArrayList getChannelSubFoldersSortTitleAsc(CmsObject cms, String channel, String subChannel) {
        ArrayList subChannels = getChannelSubFolders(cms, channel, subChannel);
        // Create an ArrayList out of the Vector        
        java.util.ArrayList tmpList = new java.util.ArrayList(subChannels.size());
        Iterator i = subChannels.iterator();
        while (i.hasNext()) {
            CmsResource res = (CmsResource)i.next();
            ResourceTitleContainer container = new ResourceTitleContainer(cms, res);
            tmpList.add(container);
        }
        Collections.sort(tmpList);
        java.util.ArrayList list = new java.util.ArrayList(subChannels.size());
        i = tmpList.iterator();
        while (i.hasNext()) {
            ResourceTitleContainer container = (ResourceTitleContainer)i.next();
            list.add(container.m_res);
        }             
        return list;
    }    
    
    /**
     * Internal helper class to get a title - comparable CmsResource for channels.<p>
     */
    private static class ResourceTitleContainer implements Comparable {

        // member variables       
        public CmsResource m_res = null;
        public String m_title = null;

        /**
         * @param cms context provider for the current request
         * @param res the resource to compare
         */        
        ResourceTitleContainer(CmsObject cms, CmsResource res) {
            m_res = res;
            try {
                cms.setContextToCos();
                m_title = cms.readProperty(res.getAbsolutePath(), com.opencms.core.I_CmsConstants.C_PROPERTY_TITLE);
                cms.setContextToVfs();
            } catch (Exception e) {
                m_title = "";
            }
        }
        
        /**
         * @see java.lang.Comparable#compareTo(Object)
         */
        public int compareTo(Object obj) {
            if (! (obj instanceof ResourceTitleContainer)) return 0;
            if (m_title == null) return 1;
            return (m_title.toLowerCase().compareTo(((ResourceTitleContainer)obj).m_title.toLowerCase()));
        }
        
    }
}
