/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/flex/jsp/Attic/CmsJspNavElement.java,v $
 * Date   : $Date: 2003/02/17 10:01:26 $
 * Version: $Revision: 1.9 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002  The OpenCms Group
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
 *
 * First created on 4. Mai 2002, 21:49
 */

package com.opencms.flex.jsp;

import com.opencms.core.I_CmsConstants;
import com.opencms.file.CmsResource;

import java.util.Hashtable;

/**
 * Bean to extract navigation information from the OpenCms VFS folder
 * structure.<p>
 *
 * @author  Alexander Kandzior (a.kandzior@alkacon.com)
 * @version $Revision: 1.9 $
 */
public class CmsJspNavElement implements Comparable {
    
    /** Property constant for <code>"locale"</code> */
    public final static String C_PROPERTY_LOCALE = "locale";    
    /** Property constant for <code>"NavImage"</code> */
    public final static String C_PROPERTY_NAVIMAGE = "NavImage";    
    /** Property constant for <code>"NavInfo"</code> */
    public final static String C_PROPERTY_NAVINFO = "NavInfo";    
    
    // Member variables for get / set methods:
    private String m_resource = null;
    private String m_fileName = null;
    private String m_text = null;
    private Hashtable m_properties = null;
    private float m_position;
    private int m_navTreeLevel = Integer.MIN_VALUE;
    private Boolean m_hasNav = null;

    /**
     * Empty constructor required for every JavaBean, does nothing.<p>
     * 
     * Call one of the init methods afer you have created an instance 
     * of the bean. Instead of using the constructor you should use 
     * the static factory methods provided by this class to create
     * navigation beans that are properly initialized with current 
     * OpenCms context.
     * 
     * @see com.opencms.flex.jsp.CmsJspNavBuilder#getNavigationForResource
     * (String)
     * @see com.opencms.flex.jsp.CmsJspNavBuilder#getNavigationForFolder
     * (String)
     * @see com.opencms.flex.jsp.CmsJspNavBuilder#getNavigationForFolder
     * (String, String)
     * @see com.opencms.flex.jsp.CmsJspNavBuilder#getNavigationForFolder
     * (String, int, String)
     * @see com.opencms.flex.jsp.CmsJspNavBuilder#getNavigationTreeForFolder
     * (String, int, int, String)
     */
    public CmsJspNavElement() {
    }
    
    /**
     * Create a new instance of the bean and calls the init method 
     * with the provided parametes.<p>
     * 
     * @param resource will be passed to <code>init</code>
     * @param properties will be passed to <code>init</code>
     * 
     * @see #init(String, Hashtable)
     */
    public CmsJspNavElement(String resource, Hashtable properties) {
        init(resource, properties, -1);
    }
    
    /**
     * Create a new instance of the bean and calls the init method 
     * with the provided parametes.<p>
     * 
     * @param resource will be passed to <code>init</code>
     * @param properties will be passed to <code>init</code>
     * @param navTreeLevel will be passed to <code>init</code>
     * 
     * @see #init(String, Hashtable, int)
     */    
    public CmsJspNavElement(String resource, Hashtable properties, int navTreeLevel) {
        init(resource, properties, navTreeLevel);
    }
    
    /**
     * Same as calling {@link #init(String, Hashtable, int) 
     * init(String, Hashtable, -1)}.<p>
     * 
     * @param resource the name of the resource to extract the navigation 
     *     information from
     * @param properties the properties of the resource read from the vfs
     */
    public void init(String resource, Hashtable properties) {
        init(resource, properties, -1);
    }

    /**
     * Initialized the member variables of this bean with the values 
     * provided.<p>
     * 
     * A resource will be in the nav if at least one of the two properties 
     * <code>I_CmsConstants.C_PROPERTY_NAVTEXT</code> or 
     * <code>I_CmsConstants.C_PROPERTY_NAVPOS</code> is set. Otherwise
     * it will be ignored.
     * 
     * This bean does provides static methods to create a new instance 
     * from the context of a current CmsObject. Call these static methods
     * in order to get a properly initialized bean.
     * 
     * @param resource the name of the resource to extract the navigation 
     *     information from
     * @param properties the properties of the resource read from the vfs
     * @param navTreeLevel tree level of this resource, for building 
     *     navigation trees
     * 
     * @see com.opencms.flex.jsp.CmsJspNavBuilder#getNavigationForResource
     * (String)
     */    
    public void init(String resource, Hashtable properties, int navTreeLevel) {
        m_resource = resource;
        m_properties = properties;
        m_navTreeLevel = navTreeLevel;
        // init the position value
        m_position = Float.MAX_VALUE;
        try {
            m_position = Float.parseFloat((String)m_properties.get(I_CmsConstants.C_PROPERTY_NAVPOS));
        } catch (Exception e) {
            // m_position will have Float.MAX_VALUE, so nevigation element will 
            // appear last in navigation
        }
    }
    
    /**
     * @see java.lang.Comparable#compareTo(Object)
     */
    public int compareTo(Object o) {
        if (o == null) return 0;
        if (! (o instanceof CmsJspNavElement)) return 0;
        float f = ((CmsJspNavElement)o).getNavPosition() - m_position;
        if (f > 0) return -1;
        if (f < 0) return 1;
        return 0;
    }
        
    /**
     * @see java.lang.Object#equals(Object)
     */
    public boolean equals(Object o) {
        if (o == null) return false;
        if (! (o instanceof CmsJspNavElement)) return false;
        return m_resource.equals(((CmsJspNavElement)o).getResourceName());
    }    

    /**
     * Returns the nav tree level of this resource.<p>
     * 
     * @return int the nav tree level of this resource
     */
    public int getNavTreeLevel() {
        if (m_navTreeLevel < 0) {
            // use "lazy initialiting"
            m_navTreeLevel = CmsResource.getPathLevel(m_resource);
        }
        return m_navTreeLevel;
    }
    
    /**
     * Returns the value of the property C_PROPERTY_NAVPOS converted to a <code>float</code>,
     * or a value of <code>Float.MAX_VALUE</code> if the nav position property is not 
     * set (or not a valid number) for this resource.<p>
     * 
     * @return float the value of the property C_PROPERTY_NAVPOS converted to a <code>float</code>,
     * or a value of <code>Float.MAX_VALUE</code> if the nav position property is not 
     * set (or not a valid number) for this resource
     */
    public float getNavPosition() {
        return m_position;
    }
    
    /**
     * Sets the value that will be returned by the {@link #getNavPosition()}
     * method of this class.<p>
     * 
     * @param value the value to set
     */
    public void setNavPosition(float value) {
        m_position = value;
    }
    
    /**
     * Returns the resource name this nav element was initalized with.<p>
     * 
     * @return String the resource name this nav element was initalized with
     */
    public String getResourceName() {
        return m_resource;
    }
    
    /**
     * Returns the filename of the nav element, i.e.
     * the name of the nav resource without any path information.<p>
     * 
     * @return String the filename of the nav element, i.e.
     * the name of the nav resource without any path information
     */
    public String getFileName() {        
        if (m_fileName == null) {
            // use "lazy initialiting"
            if (!m_resource.endsWith("/")) {
                m_fileName = m_resource.substring(m_resource.lastIndexOf("/") + 1, m_resource.length());
            } else {
                m_fileName =
                    m_resource.substring(
                        m_resource.substring(0, m_resource.length() - 1).lastIndexOf("/") + 1,
                        m_resource.length());
            }
        }
        return m_fileName;
    }

    /**
     * Returns the name of the parent folder of the resource of this nav element,<p>
     * 
     * @return String the name of the parent folder of the resource of this nav element
     */
    public String getParentFolderName() {
        return CmsResource.getParent(m_resource);
    }

    /**
     * Returns the value of the property C_PROPERTY_NAVTEXT of this nav element,
     * or a warning message if this property is not set 
     * (this method will never return <code>null</code>).<p> 
     * 
     * @return String the value of the property C_PROPERTY_NAVTEXT of this nav element,
     * or a warning message if this property is not set 
     * (this method will never return <code>null</code>)
     */
    public String getNavText() {
        if (m_text == null) {
            // use "lazy initialiting"
            m_text = (String)m_properties.get(I_CmsConstants.C_PROPERTY_NAVTEXT);
            if (m_text == null) m_text = "??? " + I_CmsConstants.C_PROPERTY_NAVTEXT + " ???";    
        }
        return m_text;
    }
    
    /**
     * Returns the value of the property C_PROPERTY_TITLE of this nav element,
     * or <code>null</code> if this property is not set.<p> 
     * 
     * @return String the value of the property C_PROPERTY_TITLE of this nav element
     * or <code>null</code> if this property is not set
     */
    public String getTitle() {
        return (String)m_properties.get(I_CmsConstants.C_PROPERTY_TITLE);
    }
    
    /**
     * Returns the value of the property C_PROPERTY_NAVINFO of this nav element,
     * or <code>null</code> if this property is not set.<p> 
     * 
     * @return String the value of the property C_PROPERTY_NAVINFO of this nav element
     * or <code>null</code> if this property is not set
     */
    public String getInfo() {
        return (String)m_properties.get(C_PROPERTY_NAVINFO);
    }

    /**
     * Returns the value of the property C_PROPERTY_LOCALE of this nav element,
     * or <code>null</code> if this property is not set.<p> 
     * 
     * @return String the value of the property C_PROPERTY_LOCALE of this nav element
     * or <code>null</code> if this property is not set
     */    
    public String getLocale() {
        return (String)m_properties.get(C_PROPERTY_LOCALE);
    }    
    
    /**
     * Returns the value of the property C_PROPERTY_NAVIMAGE of this nav element,
     * or <code>null</code> if this property is not set.<p> 
     * 
     * @return String the value of the property C_PROPERTY_NAVIMAGE of this nav element
     * or <code>null</code> if this property is not set
     */    
    public String getNavImage() {
        return (String)m_properties.get(C_PROPERTY_NAVIMAGE);
    }        
    
    /**
     * Returns the value of the property C_PROPERTY_DESCRIPTION of this nav element,
     * or <code>null</code> if this property is not set.<p> 
     * 
     * @return String the value of the property C_PROPERTY_DESCRIPTION of this nav element
     * or <code>null</code> if this property is not set
     */    
    public String getDescription() {
        return (String)m_properties.get(I_CmsConstants.C_PROPERTY_DESCRIPTION);
    }
    
    /**
     * Returns <code>true</code> if this nav element is in the navigation, <code>false</code>
     * otherwise.<p>
     * 
     * A resource is considered to be in the navigation, if <ol>
     * <li>it has the property C_PROPERTY_NAVTEXT set
     * <li><em>or</em> it has the property C_PROPERTY_NAVPOS set 
     * <li><em>and</em> it is not a temporary file that contains a '~' in it's filename.</ol> 
     * 
     * @return boolean <code>true</code> if this nav element is in the navigation, <code>false</code>
     * otherwise
     */
    public boolean isInNavigation() {
        if (m_hasNav == null) {
            // use "lazy initialiting"
            Object o1 = m_properties.get(I_CmsConstants.C_PROPERTY_NAVTEXT);
            Object o2 = m_properties.get(I_CmsConstants.C_PROPERTY_NAVPOS);
            m_hasNav = new Boolean(((o1 != null) || (o2 != null)) && (m_resource.indexOf('~') < 0));
        }
        return m_hasNav.booleanValue();
    }
    
    /**
     * Returns <code>true</code> if this nav element describes a folder, <code>false</code>
     * otherwise.<p>
     * 
     * @return boolean <code>true</code> if this nav element describes a folder, <code>false</code>
     * otherwise.<p>
     */
    public boolean isFolderLink() {
        return m_resource.endsWith("/");
    }
    
    /**
     * Returns the value of the selected property from this nav element.<p> 
     * 
     * The nav element contains a hash of all file properties of the resource that
     * the nav element belongs to.<p>
     * 
     * @param key the property name to look up
     * @return String the value of the selected property
     */
    public String getProperty(String key) {
        return (String)m_properties.get(key);
    }

    /**
     * Returns the original Hashtable of all file properties of the resource that
     * the nav element belongs to.<p>
     * 
     * Please note that the original reference is returned, so be careful when making 
     * changes to the Hashtable.<p>
     * 
     * @return Hashtable the original Hashtable of all file properties of the resource that
     * the nav element belongs to
     */    
    public Hashtable getProperties() {
        return m_properties;
    }    
}
