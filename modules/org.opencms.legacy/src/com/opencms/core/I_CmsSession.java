/*
* File   : $Source: /alkacon/cvs/opencms/modules/org.opencms.legacy/src/com/opencms/core/Attic/I_CmsSession.java,v $
* Date   : $Date: 2005/05/16 17:45:08 $
* Version: $Revision: 1.1 $
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

package com.opencms.core;

/**
 * This interface defines an OpenCms session, a generic session object 
 * that is used by OpenCms and provides methods to access the current users
 * session data.<p>
 * 
 * @author Michael Emmerich
 * @version $Revision: 1.1 $ $Date: 2005/05/16 17:45:08 $  
 * 
 * @deprecated Will not be supported past the OpenCms 6 release.
 */
public interface I_CmsSession {
    
    /**
     * Returns a value from the session.<p>
     * 
     * @param key the key
     * @return the object the is mapped to this key
     */
    Object getValue(String key);
    
    /**
     * Stores a value in the session.<p>
     * 
     * @param key the key to map the value to
     * @param value an object to store for the key
     */
    void putValue(String key, Object value);
    
    /**
     * Removes a value from the session.<p>
     * 
     * @param key the key for the value to remove
     */
    void removeValue(String key);    
    
    /**
     * Returns the session id.<p>
     * 
     * @return the session id
     */
    String getId();
    
    /**
     * Invalidates the session.
     */
    void invalidate();
}
