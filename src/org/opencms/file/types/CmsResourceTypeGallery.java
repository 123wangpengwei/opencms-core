/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/file/types/Attic/CmsResourceTypeGallery.java,v $
 * Date   : $Date: 2004/10/31 21:30:17 $
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

package org.opencms.file.types;

import org.opencms.configuration.CmsConfigurationException;

import org.apache.commons.collections.ExtendedProperties;

/**
 * Resource type descriptor for all gallery folder types.<p>
 *
 * A gallery extends a folder but has a configurable type id and type name.<p>
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.1 $
 */
public class CmsResourceTypeGallery extends CmsResourceTypeFolder {

    /** The type id of this resource. */
    private int m_resourceType;

    /** The name of this resource. */
    private String m_resourceTypeName;

    /**
     * @see org.opencms.file.types.A_CmsResourceType#addConfigurationParameter(java.lang.String, java.lang.String)
     */
    public void addConfigurationParameter(String paramName, String paramValue) {

        if (I_CmsResourceType.C_CONFIGURATION_RESOURCE_TYPE_ID.equalsIgnoreCase(paramName)) {
            m_resourceType = Integer.valueOf(paramValue).intValue();
        } else if (I_CmsResourceType.C_CONFIGURATION_RESOURCE_TYPE_NAME.equalsIgnoreCase(paramName)) {
            m_resourceTypeName = paramValue.trim();
        }
    }
    
    /**
     * @see org.opencms.file.types.A_CmsResourceType#getConfiguration()
     */
    public ExtendedProperties getConfiguration() {

        ExtendedProperties result = new ExtendedProperties();
        result.put(I_CmsResourceType.C_CONFIGURATION_RESOURCE_TYPE_ID, new Integer(m_resourceType));
        result.put(I_CmsResourceType.C_CONFIGURATION_RESOURCE_TYPE_NAME, m_resourceTypeName);
        return result;
    }
    
    /**
     * @see org.opencms.file.types.I_CmsResourceType#getTypeId()
     */
    public int getTypeId() {

        return m_resourceType;
    }

    /**
     * @see org.opencms.file.types.A_CmsResourceType#getTypeName()
     */
    public String getTypeName() {

        return m_resourceTypeName;
    }

    /**
     * @see org.opencms.file.types.A_CmsResourceType#initConfiguration()
     */
    public void initConfiguration() throws CmsConfigurationException {

        // configuration must be complete for this resource type
        if ((m_resourceTypeName == null) || (m_resourceType <= 0)) {
            throw new CmsConfigurationException("Not all required configuration parameters available for resource type");
        }
    }
}