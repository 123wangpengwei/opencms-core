/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/search/CmsSearchException.java,v $
 * Date   : $Date: 2004/07/06 08:39:39 $
 * Version: $Revision: 1.5 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2004 Alkacon Software (http://www.alkacon.com)
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

package org.opencms.search;

import org.opencms.main.CmsException;

/**
 * Signals an error during a search operation.<p>
 * 
 * This exception is thrown by various classes in the 
 * <code>org.opencms.search</code> package.
 * 
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 * @version $Revision: 1.5 $ $Date: 2004/07/06 08:39:39 $
 * @since 5.3.1
 */
public class CmsSearchException extends CmsException {

    /**
     * Constructs a CmsSearchException with the specified detail message.<p>
     * 
     * @param message the detail message
     */
    public CmsSearchException(String message) {

        super(message, 0);
    }

    /**
     * Constructs a CmsSearchException with the specified detail message
     * and adds the original exception as a delegated root cause.<p>
     * 
     * @param message the detail message
     * @param rootCause the delegated exception
     */
    public CmsSearchException(String message, Throwable rootCause) {

        super(message, 0, rootCause);
    }
}