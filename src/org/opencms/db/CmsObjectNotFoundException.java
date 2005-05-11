/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/Attic/CmsObjectNotFoundException.java,v $
 * Date   : $Date: 2005/05/11 12:58:29 $
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
 
package org.opencms.db;

import org.opencms.i18n.CmsMessageContainer;
import org.opencms.main.CmsException;

/**
 * Signals that an attempt to read an object from a data source, that
 * is supposed to exist, was not successfull.<p> 
 * 
 * @author Michael Moossen (m.moossen@alkacon.com)
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @version $Revision: 1.6 $ $Date: 2005/05/11 12:58:29 $
 * @since 5.7.3
 */
public class CmsObjectNotFoundException extends CmsDataAccessException {

    /**
     * Constructs an exception with the specified message.<p>
     * 
     * @param message the description message
     */
    public CmsObjectNotFoundException(String message) {
        this(message, null);
    }

    /**
     * Constructs a exception with the specified detail message
     * and the original exception.<p>
     * 
     * @param message the detail message
     * @param rootCause the exception
     */
    public CmsObjectNotFoundException(String message, Throwable rootCause) {
        super(message, C_DA_OBJECT_NOT_FOUND_EXCEPTION, rootCause);
    }
    
    /**
     * @see org.opencms.main.CmsException#CmsException(CmsMessageContainer)
     */
    public CmsObjectNotFoundException(CmsMessageContainer container) {

        super(container);
    }

    /**
     * @see org.opencms.main.CmsException#CmsException(CmsMessageContainer, Throwable)
     */
    public CmsObjectNotFoundException(CmsMessageContainer container, Throwable cause) {

        super(container, cause);
    }    
    
    /**
     * @see org.opencms.main.CmsException#createException(org.opencms.i18n.CmsMessageContainer, java.lang.Throwable)
     */
    public CmsException createException(CmsMessageContainer container, Throwable cause) {
        
        return new CmsObjectNotFoundException(container, cause);
    }
}
