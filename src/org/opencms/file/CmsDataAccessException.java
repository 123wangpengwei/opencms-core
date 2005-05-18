/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/file/CmsDataAccessException.java,v $
 * Date   : $Date: 2005/05/18 12:48:14 $
 * Version: $Revision: 1.4 $
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

import org.opencms.i18n.CmsMessageContainer;
import org.opencms.main.CmsException;

/**
 * Signals data access related issues, i.e. database access.<p> 
 * 
 * @author Michael Moossen (m.moossen@alkacon.com)
 * @version $Revision: 1.4 $
 * @since 5.7.3
 */
public class CmsDataAccessException extends CmsException {

    /**
     * Creates a new localized Exception.<p>
     * 
     * @param container the localized message container to use
     */
    public CmsDataAccessException(CmsMessageContainer container) {

        super(container);
    }

    /**
     * Creates a new localized Exception that also containes a root cause.<p>
     * 
     * @param container the localized message container to use
     * @param cause the Exception root cause
     */
    public CmsDataAccessException(CmsMessageContainer container, Throwable cause) {

        super(container, cause);
    }

    /**
     * Constructs a exception with the specified description message.<p>
     * 
     * @param message the error message
     */
    public CmsDataAccessException(String message) {

        super(message);
    }

    /**
     * Constructs a exception with the specified description message and type.<p>
     * 
     * @param message the description message
     * @param type the type of the exception
     */
    public CmsDataAccessException(String message, int type) {

        super(message, type);
    }

    /**
     * Constructs a exception with the specified description message, 
     * type and root exception.<p>
     * 
     * @param message the description message, may be <code>null</code>
     * @param type the type of the exception, may be <code>null</code> for the default sql error
     * @param rootCause the originating exception, may be <code>null</code>
     */
    public CmsDataAccessException(String message, int type, Throwable rootCause) {

        super(message, type, rootCause);
    }

    /**
     * Constructs a exception with the specified description message and root exception.<p>
     * 
     * @param message the error message
     * @param rootCause the root cause
     */
    public CmsDataAccessException(String message, Throwable rootCause) {

        super(message, rootCause);
    }

    /**
     * Constructs a exception with the specified root exception.<p>
     * 
     * @param rootCause the root cause
     */
    public CmsDataAccessException(Throwable rootCause) {

        super("Data Access Exception", rootCause);
    }

    /**
     * @see org.opencms.main.CmsException#createException(org.opencms.i18n.CmsMessageContainer, java.lang.Throwable)
     */
    public CmsException createException(CmsMessageContainer container, Throwable cause) {

        return new CmsDataAccessException(container, cause);
    }

}