/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/site/CmsSite.java,v $
 * Date   : $Date: 2003/07/20 15:45:00 $
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

package org.opencms.site;

import com.opencms.flex.util.CmsUUID;

/**
 * Describes a configures site in OpenCms.<p>
 *
 * @author  Alexander Kandzior (a.kandzior@alkacon.com)
 *
 * @version $Revision: 1.3 $
 * @since 5.1
 */
public final class CmsSite implements Cloneable {   

    /** Name of the property to use for defining directories as site roots */
    public static final String C_PROPERTY_SITE = "siteroot";

    /** Root directory of this site in the OpenCms VFS */
    private String m_siteRoot;

    /** UUID of this site's root directory in the OpenCms VFS */
    private CmsUUID m_siteRootUUID;

    /** Display title of this site */    
    private String m_title;
    
    /** The site matcher that describes the site */ 
    private CmsSiteMatcher m_siteMatcher;

    /**
     * Constructs a new site object.<p>
     * 
     * @param siteRoot root directory of this site in the OpenCms VFS
     * @param siteRootUUID UUID of this site's root directory in the OpenCms VFS
     * @param title display name of this site
     * @param siteMatcher the site matcher for this site
     */
    public CmsSite(String siteRoot, CmsUUID siteRootUUID, String title, CmsSiteMatcher siteMatcher) {
        setSiteRoot(siteRoot);
        setSiteRootUUID(siteRootUUID);
        setTitle(title);
        setSiteMatcher(siteMatcher);
    }

    /**
     * Constructs a new site object without title and id information,
     * this is to be used for lookup purposes only.<p>
     * 
     * @param siteRoot root directory of this site in the OpenCms VFS
     * @param siteMatcher the site matcher for this site
     */
    public CmsSite(String siteRoot, CmsSiteMatcher siteMatcher) {
        setSiteRoot(siteRoot);
        setSiteRootUUID(CmsUUID.getNullUUID());
        setTitle(siteRoot);
        setSiteMatcher(siteMatcher);
    }
        
    /**
     * Constructs a new site object with a default (wildcard) a site matcher,
     * this is to be used for display purposes only.<p>
     * 
     * @param siteRoot root directory of this site in the OpenCms VFS
     * @param siteRootUUID UUID of this site's root directory in the OpenCms VFS
     * @param title display name of this site
     */
    public CmsSite(String siteRoot, CmsUUID siteRootUUID, String title) {
        setSiteRoot(siteRoot);
        setSiteRootUUID(siteRootUUID);
        setTitle(title);
        setSiteMatcher(CmsSiteMatcher.C_DEFAULT_MATCHER);
    }    

    /**
     * Returns the root directory of this site in the OpenCms VFS.<p>
     * 
     * @return the root directory of this site in the OpenCms VFS
     */
    public String getTitle() {
        return m_title;
    }

    /**
     * Sets the root directory of this site in the OpenCms VFS
     * 
     * @param name the root directory of this site in the OpenCms VFS
     */
    protected void setTitle(String name) {
        m_title = name;
    }

    /**
     * Returns the site matcher that describes the URL of this site.<p>
     * 
     * @return the site matcher that describes the URL of this site
     */
    public CmsSiteMatcher getSiteMatcher() {
        return m_siteMatcher;
    }

    /**
     * Sets the site matcher that describes the URL of this site.<p>
     * 
     * @param siteMatcher the site matcher that describes the URL of this site
     */
    protected void setSiteMatcher(CmsSiteMatcher siteMatcher) {
        m_siteMatcher = siteMatcher;
    }

    /**
     * Returns the server URL prefix to which this site is mapped.<p>
     * 
     * @return the server URL prefix to which this site is mapped
     */
    public String getSiteRoot() {
        return m_siteRoot;
    }

    /**
     * Sets the server URL prefix to which this site is mapped.<p>
     * 
     * @param siteRoot the server URL prefix to which this site is mapped
     */
    protected void setSiteRoot(String siteRoot) {
        // site roots must never end with a "/"
        if (siteRoot.endsWith("/")) {
            m_siteRoot = siteRoot.substring(0, siteRoot.length()-1);
        } else {        
            m_siteRoot = siteRoot;
        }
    }

    /**
     * Returns the UUID of this site's root directory in the OpenCms VFS.<p>
     * 
     * @return the UUID of this site's root directory in the OpenCms VFS
     */
    public CmsUUID getSiteRootUUID() {
        return m_siteRootUUID;
    }

    /**
     * Sets the UUID of this site's root directory in the OpenCms VFS.<p>
     * 
     * @param siteRootUUID the UUID of this site's root directory in the OpenCms VFS
     */
    protected void setSiteRootUUID(CmsUUID siteRootUUID) {
        m_siteRootUUID = siteRootUUID;
    }
    
    /**
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        return new CmsSite(
            getSiteRoot(), 
            (CmsUUID)getSiteRootUUID().clone(), 
            getTitle(), 
            (CmsSiteMatcher)getSiteMatcher().clone()
        );
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer result = new StringBuffer(128);
        result.append(m_siteMatcher.toString());
        result.append("|");
        result.append(m_siteRoot);
        result.append("|");
        result.append(m_title);
        return result.toString();
    }

}
