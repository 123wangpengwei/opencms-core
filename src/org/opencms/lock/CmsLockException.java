/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/lock/CmsLockException.java,v $
 * Date   : $Date: 2003/07/22 07:55:30 $
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
 
package org.opencms.lock;

import com.opencms.core.CmsException;

/**
 * Signals that a particular action was invoked on a locked resource.<p>
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @version $Revision: 1.1 $ $Date: 2003/07/22 07:55:30 $
 * @since 5.1.4
 */
public class CmsLockException extends CmsException {
    
    /** The resource is locked by the current user */
    public static final int C_RESOURCE_LOCKED_BY_CURRENT_USER = 1;
    
    /** The resource is locked by a user different from the current user */
    public static final int C_RESOURCE_LOCKED_BY_OTHER_USER = 2;
    
    /**
     * Default constructor for a CmsLockException.
     */
    public CmsLockException() {
        super();
    }
    
    /**
     * Constructs a CmsLockException with the specified detail message and type.
     * 
     * @param message the detail message
     * @param type the type of the exception
     */
    public CmsLockException(String message, int type) {
        super(message, type, null);
    }

}
