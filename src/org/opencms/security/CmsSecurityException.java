/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/security/CmsSecurityException.java,v $
 * Date   : $Date: 2005/05/10 07:50:56 $
 * Version: $Revision: 1.13 $
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
 
package org.opencms.security;

import org.opencms.i18n.CmsMessageContainer;
import org.opencms.main.CmsException;

/**
 * Used to signal security related issues, for example example during file access and login.<p> 
 * 
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * @version $Revision: 1.13 $
 * @since 5.1.4
 */
public class CmsSecurityException extends CmsException {
    
    /**
     * Creates a new localized Exception.<p>
     * 
     * @param container the localized message container to use
     */
    public CmsSecurityException(CmsMessageContainer container) {

        super(container);
    }

    /**
     * Creates a new localized Exception that also containes a root cause.<p>
     * 
     * @param container the localized message container to use
     * @param cause the Exception root cause
     */
    public CmsSecurityException(CmsMessageContainer container, Throwable cause) {

        super(container, cause);
    }    
    
    /**
     * @see org.opencms.main.CmsException#createException(org.opencms.i18n.CmsMessageContainer, java.lang.Throwable)
     */
    public CmsException createException(CmsMessageContainer container, Throwable cause) {
        
        return new CmsSecurityException(container, cause);
    }
    
    
    // the allowed type range for this exception is >=300 and <400    
    
    /** Administrator privileges required. */
    public static final int C_SECURITY_ADMIN_PRIVILEGES_REQUIRED = 300;
    
    /** Project manager (or Administrator) privileges required. */
    public static final int C_SECURITY_PROJECTMANAGER_PRIVILEGES_REQUIRED = 301;
    
    /** No read / write access allowed in online project. */
    public static final int C_SECURITY_NO_MODIFY_IN_ONLINE_PROJECT = 302;
    
    /** No permissions to perform operation. */
    public static final int C_SECURITY_NO_PERMISSIONS = 303;
    
    /** No permissions to change registry values. */    
    public static final int C_SECURITY_NO_REGISTRY_PERMISSIONS = 304;
       
    /** Invalid password (only for password change and validation of password). */    
    public static final int C_SECURITY_INVALID_PASSWORD = 305;
    
    /** Login failed. */
    public static final int C_SECURITY_LOGIN_FAILED = 306;

    /**
     * Default constructor for a CmsSecurityException.<p>
     */
    public CmsSecurityException() {
        super();
    }
    
    /**
     * Constructs a CmsSecurityException with the specified description message and type.<p>
     * 
     * @param type the type of the exception
     */
    public CmsSecurityException(int type) {
        super(type);
    }
        
    /**
     * Constructs a CmsSecurityException with the specified description message and type.<p>
     * 
     * @param message the description message
     * @param type the type of the exception
     */
    public CmsSecurityException(String message, int type) {
        super(message, type);
    }
    
    /**
     * Constructs a CmsSecurityException with the specified description message and root exception.<p>
     * 
     * @param type the type of the exception
     * @param rootCause root cause exception
     */
    public CmsSecurityException(int type, Throwable rootCause) {
        super(type, rootCause);
    }
    
    /**
     * Returns the description String for the provided CmsException type.<p>
     * 
     * @param type exception error code 
     * @return the description String for the provided CmsException type
     */    
    protected String getErrorDescription(int type) {
        switch (type) {
            case C_SECURITY_ADMIN_PRIVILEGES_REQUIRED:
                return "Administrator priviledges are required to perform this operation";
            case C_SECURITY_PROJECTMANAGER_PRIVILEGES_REQUIRED:
                return "Project manager priviledges are required to perform this operation"; 
            case C_SECURITY_NO_MODIFY_IN_ONLINE_PROJECT:
                return "Modify operation not allowed in 'Online' project";
            case C_SECURITY_NO_PERMISSIONS:
                return "No permissions to perform this operation";
            case C_SECURITY_NO_REGISTRY_PERMISSIONS:
                return "No permissions to modify the registry";
            case C_SECURITY_INVALID_PASSWORD:
                return "Invalid password";                
            case C_SECURITY_LOGIN_FAILED:
                return "OpenCms login validation failed";                
            default:
                return super.getErrorDescription(type);
        }
    }
}
