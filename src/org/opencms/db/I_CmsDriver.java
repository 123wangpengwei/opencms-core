/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/I_CmsDriver.java,v $
 * Date   : $Date: 2004/10/28 11:07:27 $
 * Version: $Revision: 1.7 $
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

import org.opencms.configuration.CmsConfigurationManager;
import org.opencms.main.CmsException;

import java.util.List;


/**
 * @version $Revision: 1.7 $ $Date: 2004/10/28 11:07:27 $
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 */
public interface I_CmsDriver {

    /**
     * Initializes the driver.<p>
     * 
     * @param configurationManager the configuration manager
     * @param successiveDrivers a list of successive drivers to be initialized
     * @param driverManager the initialized OpenCms driver manager
     * @param runtimeInfoFactory the runtime info factory configured in opencms-system.xml, if a driver needs to create his own runtime info objects
     * 
     * @throws CmsException if something goes wrong
     */
    void init(CmsConfigurationManager configurationManager, List successiveDrivers, CmsDriverManager driverManager, I_CmsRuntimeInfoFactory runtimeInfoFactory) throws CmsException;
    
    /**
     * Returns information about the driver.<p>
     * 
     * @return an information string
     */
    String toString();
    
}