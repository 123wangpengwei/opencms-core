/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/I_CmsCacheKey.java,v $
 * Date   : $Date: 2003/09/18 09:50:57 $
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
package org.opencms.db;

import org.opencms.security.CmsPermissionSet;

import com.opencms.file.CmsRequestContext;
import com.opencms.file.CmsResource;
import com.opencms.file.CmsUser;

/**
 * @version $Revision: 1.2 $ $Date: 2003/09/18 09:50:57 $
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 */
public interface I_CmsCacheKey {

    /**
     * Returns the cache key for the permission cache.<p>
     * 
     * @param prefix to distinguish keys additionally
     * @param context the context
     * @param resource the resource
     * @param requiredPermissions the permissions to check
     * @return a cache key that is unique for the set of parameters
     */
    String getCacheKeyForUserPermissions (String prefix, CmsRequestContext context, CmsResource resource, CmsPermissionSet requiredPermissions);

    /**
     * Returns the cache key for the user groups cache.<p>
     * 
     * @param prefix to distinguish keys additionally
     * @param context the context
     * @param user the resource
     * @return a cache key that is unique for the set of parameters
     */
    String getCacheKeyForUserGroups (String prefix, CmsRequestContext context, CmsUser user);
}
