/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/security/CmsPasswordEncryptionException.java,v $
 * Date   : $Date: 2005/06/23 10:47:25 $
 * Version: $Revision: 1.5 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
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

package org.opencms.security;

import org.opencms.i18n.CmsMessageContainer;
import org.opencms.main.CmsException;

/**
 * Signals that an attempt to encrypt a password was not successful.<p>
 *  
 * @author Thomas Weckert  
 * 
 * @version $Revision: 1.5 $ 
 * 
 * @since 6.0.0 
 */
public class CmsPasswordEncryptionException extends CmsException {

    /**
     * @see org.opencms.main.CmsException#CmsException(CmsMessageContainer)
     */
    public CmsPasswordEncryptionException(CmsMessageContainer container) {

        super(container);
    }

    /**
     * @see org.opencms.main.CmsException#CmsException(CmsMessageContainer, Throwable)
     */
    public CmsPasswordEncryptionException(CmsMessageContainer container, Throwable cause) {

        super(container, cause);
    }

    /**
     * @see org.opencms.main.CmsException#createException(org.opencms.i18n.CmsMessageContainer, java.lang.Throwable)
     */
    public CmsException createException(CmsMessageContainer container, Throwable cause) {

        return new CmsPasswordEncryptionException(container, cause);
    }
}
