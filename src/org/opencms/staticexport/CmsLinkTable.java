/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/staticexport/CmsLinkTable.java,v $
 * Date   : $Date: 2003/12/17 17:46:37 $
 * Version: $Revision: 1.2 $
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
package org.opencms.staticexport;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Maintains a table of links for an element of a CmsXmlPage.<p>
 *  
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 * 
 * @version $Revision: 1.2 $
 * @since 5.3
 */
public class CmsLinkTable {

    /** Prefix to identify a link in the content */
    private static final String C_LINK_PREFIX = "link";
    
    /** The map to store the link table in */
    private HashMap m_linkTable;
        
    /**
     * Creates a new CmsLinkTable.<p>
     */
    public CmsLinkTable () {        
        m_linkTable = new HashMap();
    }
    
    /**
     * Adds a new link to the link table.<p>
     * 
     * @param type type of the link
     * @param target link destination
     * @param internal flag to indicate if the link is a local link
     * @return the new link entry
     */
    public CmsLink addLink (String type, String target, boolean internal) {
        CmsLink link = new CmsLink(C_LINK_PREFIX + m_linkTable.size(), type, target, internal);
        m_linkTable.put(link.getName(), link);
        return link;
    }

    /**
     * Adds a new link with a given internal name and internal flag to the link table.<p>
     * 
     * @param name the internal name of the link
     * @param type the type of the link
     * @param target the destination of the link
     * @param internal flag to indicate if the link is a local link
     * @return the new link entry
     */
    public CmsLink addLink (String name, String type, String target, boolean internal) {
        CmsLink link = new CmsLink(name, type, target, internal);
        m_linkTable.put(link.getName(), link);
        return link;
    }
    
    /**
     * Returns the CmsLink Entry for a given name.<p>
     * 
     * @param name the internal name of the link
     * @return the CmsLink entry
     */
    public CmsLink getLink (String name) {        
        return (CmsLink)m_linkTable.get(name);
    }

    /**
     * Returns if the link table is empty.<p>
     * 
     * @return true if the link table is empty, false otherwise
     */
    public boolean isEmpty() {        
        return m_linkTable.isEmpty();
    }
    
    /**
     * Returns an iterator over the internal names for links in the table.<p>
     * 
     * @return a string iterator for internal link names
     */
    public Iterator iterator() {    
        return m_linkTable.keySet().iterator();
    }
    
    /**
     * Checks if a given link target is an internal link inside the OpenCms VFS.<p>
     * 
     * @param target the link target
     * @return true if the target is identidfied as local
     *//*
    private boolean isInternal(String target) {       
        URI targetURI = URI.create(target);
        String context = OpenCms.getOpenCmsContext();
        
        int pos = context.indexOf("/", 1);
        String webapp  = (pos > 0) ? context.substring(0, pos) : context;
        
        if (targetURI.isOpaque()) {
            // an opaque uri (i.e. mailto:name@host) is not internal
            return false;
        }

        if (targetURI.isAbsolute()) {
            // an absolute uri is internal if it starts with a site root or if its part starts with the opencms context
            return OpenCms.getSiteManager().isMatching(new CmsSiteMatcher(targetURI.toString())) || targetURI.getPath().startsWith(context);
        }
        
        if (targetURI.getPath().startsWith(context)) {
            // a relative uri that starts with the opencms context (/opencms/opencms as default) is internal
            return true;
        }
        
        if (targetURI.getPath().startsWith(webapp)) {
            // a relative uri that starts with the webapp context (but not with the opencms context) is not internal
            return false;
        }    
        
        // every other relative uri is internal
        return true;
    }*/
}
