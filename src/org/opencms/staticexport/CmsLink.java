/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/staticexport/Attic/CmsLink.java,v $
 * Date   : $Date: 2003/12/15 09:27:18 $
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
package org.opencms.staticexport;

import org.opencms.main.OpenCms;

/**
 * A single link entry in the link table.<p>
 * 
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 * 
 * @version $Revision: 1.1 $ 
 */
public class CmsLink {

    /** The internal name of the link */
    private String m_name;

    /** The type of the link */
    private String m_type;
    
    /** The link target (destination) */
    private String m_target;
    
    /** Indicates if the link is an internal link within the OpenCms VFS */
    private boolean m_internal;
    
    /**
     * Creates a new link object.<p>
     * 
     * @param name the internal name of this link
     * @param type the type of this link
     * @param target the link target (destination)
     * @param internal indicates if the link is intrenal within OpenCms 
     */
    public CmsLink(String name, String type, String target, boolean internal) {
        m_name = name;
        m_type = type;
        m_target = target;
        m_internal = internal;
    }
    
    /**
     * Returns the macro name of this link.<p>
     * 
     * @return the macro name name of this link
     */
    public String getName() {            
        return m_name;
    }
    
    /**
     * Returns the type of this link.<p>
     * 
     * @return the type of this link
     */
    public String getType() {            
        return m_type;
    }
    
    /**
     * Returns the target (destination) of this link.<p>
     * 
     * @return the target the target (destination) of this link
     */
    public String getTarget() {            
        return m_target;
    }
    
    /**
     * Convenience method to get a vfs link from the target.<p>
     * 
     * If the link is internal and starts with the context (i.e. /opencms/opencms),
     * the context is removed
     * 
     * @return the full link destination
     */
    public String getVfsTarget() {        
        String context = OpenCms.getOpenCmsContext();
        if (m_internal && m_target.startsWith(context)) {
            return m_target.substring(context.length());
        } else {
            return m_target;
        }
    } 
    
    /**
     * Returns if the link is internal.<p>
     * 
     * @return true if the link is a local link
     */
    public boolean isInternal() {           
        return m_internal;
    }   
}