/*
 * File   : $Source: /alkacon/cvs/opencms/test/org/opencms/scheduler/TestScheduledJob.java,v $
 * Date   : $Date: 2004/07/05 15:35:24 $
 * Version: $Revision: 1.1 $
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
 
package org.opencms.scheduler;

import org.opencms.file.CmsObject;

import org.apache.commons.collections.ExtendedProperties;

/**
 * Test class for OpenCms scheduled jobs.<p>
 */
public class TestScheduledJob implements I_CmsSchedulerJob {

    /** Indicates if this class was run. */
    public static int m_runCount = 0; 
    
    /** Static copy of the instance run count for easy test access. */
    public static int m_instanceCountCopy;
    
    /** Instance run count. */
    private int m_instanceRunCount;
    
    /**
     * Default constructor.<p> 
     */
    public TestScheduledJob() {        

        m_instanceRunCount = 0;
    }

    /**
     * @see org.opencms.scheduler.I_CmsSchedulerJob#launch(org.opencms.file.CmsObject, org.apache.commons.collections.ExtendedProperties)
     */
    public String launch(CmsObject cms, ExtendedProperties parameters) throws Exception {

        m_runCount++;
        m_instanceRunCount++;
        m_instanceCountCopy = m_instanceRunCount;
        return "OpenCms scheduler test job " + m_runCount + " was run (instance count: " + m_instanceRunCount + ").";
    }
}
