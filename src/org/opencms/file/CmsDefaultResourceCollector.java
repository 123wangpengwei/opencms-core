/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/file/Attic/CmsDefaultResourceCollector.java,v $
 * Date   : $Date: 2005/03/11 15:41:53 $
 * Version: $Revision: 1.6 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2005 Alkacon Software (http://www.alkacon.com)
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

package org.opencms.file;

import org.opencms.jsp.CmsJspNavBuilder;
import org.opencms.jsp.CmsJspNavElement;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.PrintfFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A default resource collector to generate some example list of resources from the VFS.<p>
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * 
 * @version $Revision: 1.6 $
 * @since 5.5.2
 */
public class CmsDefaultResourceCollector extends A_CmsResourceCollector {

    /**
     * Data structure for the collctor, parsed form the collector parameters.<p>
     */
    protected class CmsCollectorData {

        /** The display count. */
        private int m_count;

        /** The file name. */
        private String m_fileName;

        /** The file type. */
        private int m_type;

        /**
         * Creates a new collector data set.<p>
         * 
         * @param data the data to parse
         */
        public CmsCollectorData(String data) {

            if (data == null) {
                throw new IllegalArgumentException(
                    "Collector requires a parameter in the form '/sites/default/myfolder/file_${number}.html|11|4'");
            }

            int pos1 = data.indexOf('|');
            if (pos1 == -1) {
                throw new IllegalArgumentException("Malformed collector parameter '" + data + "'");
            }

            int pos2 = data.indexOf('|', pos1 + 1);
            if (pos2 == -1) {
                pos2 = data.length();
                m_count = 0;
            } else {
                m_count = Integer.valueOf(data.substring(pos2 + 1)).intValue();
            }

            m_fileName = data.substring(0, pos1);
            m_type = Integer.valueOf(data.substring(pos1 + 1, pos2)).intValue();
        }

        /**
         * Returns the count.<p>
         *
         * @return the count
         */
        public int getCount() {

            return m_count;
        }

        /**
         * Returns the file name.<p>
         *
         * @return the file name
         */
        public String getFileName() {

            return m_fileName;
        }

        /**
         * Returns the type.<p>
         *
         * @return the type
         */
        public int getType() {

            return m_type;
        }
    }

    /** Format for create parameter. */
    private static final PrintfFormat C_FORMAT_NUMBER = new PrintfFormat("%0.4d");

    /** Static array of the collectors implemented by this class. */
    private static final String[] m_collectorNames = {
        "singleFile",
        "allInFolder",
        "allInFolderDateReleasedDesc",
        "allInFolderNavPos",
        "allInSubTree",
        "allInSubTreeDateReleasedDesc",
        "allInSubTreeNavPos"
    };

    /** Array list for fast collector name lookup. */
    private static final List m_collectors = Collections.unmodifiableList(Arrays.asList(m_collectorNames));

    /**
     * @see org.opencms.file.I_CmsResourceCollector#getCollectorNames()
     */
    public List getCollectorNames() {

        return m_collectors;
    }

    /**
     * @see org.opencms.file.I_CmsResourceCollector#getCreateLink(org.opencms.file.CmsObject, java.lang.String, java.lang.String)
     */
    public String getCreateLink(CmsObject cms, String collectorName, String param) throws CmsException {

        // if action is not set, use default action
        if (collectorName == null) {
            collectorName = m_collectorNames[0];
        }

        switch (m_collectors.indexOf(collectorName)) {
            case 0:
                // "singleFile"
                return null;
            case 1:
                // "allInFolder"
            case 2:
                // "allInFolderDateReleasedDesc"
            case 3:
                // "allInFolderNavPos"
                return getCreateInFolder(cms, param);                
            case 4:
                // "allInSubTree"
            case 5:
                // "allInSubTreeDateReleasedDesc"
            case 6:
                // "allInSubTreeNavPos"
                return null;
            default:
                throw new CmsException("Invalid resource collector selected: " + collectorName);
        }
    }

    /**
     * @see org.opencms.file.I_CmsResourceCollector#getCreateParam(org.opencms.file.CmsObject, java.lang.String, java.lang.String)
     */
    public String getCreateParam(CmsObject cms, String collectorName, String param) throws CmsException {

        // if action is not set, use default action
        if (collectorName == null) {
            collectorName = m_collectorNames[0];
        }

        switch (m_collectors.indexOf(collectorName)) {
            case 0:
                // "singleFile"
                return null;
            case 1:
                // "allInFolder"
            case 2:
                // "allInFolderDateReleasedDesc"
            case 3:
                // "allInFolderNavPos"
                return param;                
            case 4:
                // "allInSubTree"
            case 5:
                // "allInSubTreeDateReleasedDesc"
            case 6:
                // "allInSubTreeNavPos"
                return null;
            default:
                throw new CmsException("Invalid resource collector selected: " + collectorName);
        }
    }

    /**
     * @see org.opencms.file.I_CmsResourceCollector#getResults(org.opencms.file.CmsObject, java.lang.String, java.lang.String)
     */
    public List getResults(CmsObject cms, String collectorName, String param) throws CmsException {

        // if action is not set use default
        if (collectorName == null) {
            collectorName = m_collectorNames[0];
        }

        switch (m_collectors.indexOf(collectorName)) {
            case 0:
                // "singleFile"
                return getSingleFile(cms, param);
            case 1:
                // "allInFolder"
                return getAllInFolder(cms, param, false);
            case 2:
                // "allInFolderDateReleasedDesc"
                return allInFolderDateReleasedDesc(cms, param, false);
            case 3:
                // allInFolderNavPos"
                return allInFolderNavPos(cms, param, false);
            case 4:
                // "allInSubTree"
                return getAllInFolder(cms, param, true);
            case 5:
                // "allInSubTreeDateReleasedDesc"
                return allInFolderDateReleasedDesc(cms, param, true);
            case 6:
                // "allInSubTreeNavPos"
                return allInFolderNavPos(cms, param, true);
            default:
                throw new CmsException("Invalid resource collector selected: " + collectorName);
        }
    }

    /**
     * Returns a List of all resources in the folder pointed to by the parameter 
     * sorted by the release date, descending.<p>
     * 
     * @param cms the current CmsObject
     * @param param the folder name to use
     * @param tree if true, look in folder and all child folders, if false, look only in given folder
     * 
     * @return a List of all resources in the folder pointed to by the parameter 
     *      sorted by the release date, descending
     * 
     * @throws CmsException if something goes wrong
     */
    protected List allInFolderDateReleasedDesc(CmsObject cms, String param, boolean tree) throws CmsException {

        CmsCollectorData data = new CmsCollectorData(param);
        String foldername = CmsResource.getFolderPath(data.getFileName());

        CmsResourceFilter filter = CmsResourceFilter.DEFAULT.addRequireType(data.getType());
        List result = cms.readResources(foldername, filter, tree);

        Collections.sort(result, CmsResource.COMPARE_DATE_RELEASED);

        return shrinkToFit(result, data.getCount());
    }
    
    /**
     * Collects all resources in a folder (or subtree) sorted by the NavPos property.<p>
     * 
     * @param cms the current user's Cms object
     * @param param the collector's parameter(s)
     * @param readSubTree if true, collects all resources in the subtree
     * @return a List of Cms resources found by the collector
     * @throws CmsException if something goes wrong
     */
    protected List allInFolderNavPos(CmsObject cms, String param, boolean readSubTree) throws CmsException {

        CmsCollectorData data = new CmsCollectorData(param);
        String foldername = CmsResource.getFolderPath(data.getFileName());

        CmsResourceFilter filter = CmsResourceFilter.DEFAULT.addRequireType(data.getType());
        List foundResources = cms.readResources(foldername, filter, readSubTree);

        // the Cms resources are saved in a map keyed by their nav elements
        // to save time sorting the resources by the value of their NavPos property        
        Map navElementMap = new HashMap();
        for (int i = 0, n = foundResources.size(); i < n; i++) {

            CmsResource resource = (CmsResource)foundResources.get(i);
            CmsJspNavElement navElement = CmsJspNavBuilder.getNavigationForResource(cms, cms.getSitePath(resource));

            // check if the resource has the NavPos property set or not
            if (navElement != null && navElement.getNavPosition() != Float.MAX_VALUE) {
                navElementMap.put(navElement, resource);
            } else if (OpenCms.getLog(this).isInfoEnabled()) {                
                // printing a log messages makes it a little easier to indentify 
                // resources having not the NavPos property set
                OpenCms.getLog(this).info("Resource w/o nav. properties found: " + navElement.getResourceName());
            }
        }

        List result = null;
        if (navElementMap.size() == foundResources.size()) {
            // all found resources have the NavPos property set
            // sort the nav. elements, and pull the found Cms resources
            // from the map in the correct order into a list
            List navElementList = new ArrayList(navElementMap.keySet());
            result = new ArrayList();

            Collections.sort(navElementList);
            for (int i = 0, n = navElementList.size(); i < n; i++) {

                CmsJspNavElement navElement = (CmsJspNavElement)navElementList.get(i);
                result.add(navElementMap.get(navElement));
            }
        } else {
            // not all found resources have the NavPos property set
            // sort the resources by release date as usual
            result = foundResources;
            Collections.sort(result, CmsResource.COMPARE_DATE_RELEASED);
        }

        return shrinkToFit(result, data.getCount());
    }
    
    /**
     * Shrinks a List to fit a maximum size.<p>
     * 
     * @param result a List
     * @param maxSize the maximum size of the List
     * 
     * @return the shrinked list
     */
    protected List shrinkToFit(List result, int maxSize) {
        
        if ((maxSize > 0) && (result.size() > maxSize)) {
            // cut off all items > count
            result = result.subList(0, maxSize);
        }
        
        return result;
    }

    /**
     * Returns all resources in the folder pointed to by the parameter.<p>
     * 
     * @param cms the current CmsObject
     * @param param the folder name to use
     * @param tree if true, look in folder and all child folders, if false, look only in given folder
     * 
     * @return all resources in the folder matching the given criteria
     * 
     * @throws CmsException if something goes wrong
     */
    protected List getAllInFolder(CmsObject cms, String param, boolean tree) throws CmsException {

        CmsCollectorData data = new CmsCollectorData(param);
        String foldername = CmsResource.getFolderPath(data.getFileName());

        CmsResourceFilter filter = CmsResourceFilter.DEFAULT.addRequireType(data.getType());
        List result = cms.readResources(foldername, filter, tree);

        Collections.sort(result, CmsResource.COMPARE_ROOT_PATH);
        Collections.reverse(result);

        return shrinkToFit(result, data.getCount());
    }

    /**
     * Returns the link to create a new XML content item in the folder pointed to by the parameter.<p>
     * 
     * @param cms the current CmsObject
     * @param param the folder name to use
     * 
     * @return the link to create a new XML content item in the folder
     * 
     * @throws CmsException if something goes wrong
     */
    protected String getCreateInFolder(CmsObject cms, String param) throws CmsException {

        CmsCollectorData data = new CmsCollectorData(param);

        String foldername = CmsResource.getFolderPath(data.getFileName());

        // must check ALL resources in folder because name dosen't care for type
        List resources = cms.readResources(foldername, CmsResourceFilter.ALL, false);

        // now create a list of all resources that just contains the file names
        List result = new ArrayList(resources.size());
        for (int i = 0; i < resources.size(); i++) {
            CmsResource resource = (CmsResource)resources.get(i);
            result.add(resource.getRootPath());
        }

        String fileName = cms.getRequestContext().addSiteRoot(data.getFileName());
        String checkName;
        String number;

        int j = 0;
        do {
            number = C_FORMAT_NUMBER.sprintf(++j);
            checkName = CmsStringUtil.substitute(fileName, "${number}", number);
        } while (result.contains(checkName));

        return cms.getRequestContext().removeSiteRoot(checkName);
    }

    /**
     * Returns a List containing the resources pointed to by the parameter.<p>
     * 
     * @param cms the current CmsObject
     * @param param the name of the file to load
     * 
     * @return a List containing the resources pointed to by the parameter
     * 
     * @throws CmsException if something goes wrong
     */
    protected List getSingleFile(CmsObject cms, String param) throws CmsException {

        if ((param == null) || (cms == null)) {
            throw new IllegalArgumentException("Collector requires a parameter in the form '/myfolder/file.html'");
        }

        // create a list and return it
        ArrayList result = new ArrayList(1);
        result.add(cms.readFile(param));
        return result;
    }
}