/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/staticexport/Attic/CmsStaticExportJob.java,v $
 * Date   : $Date: 2004/07/08 15:24:39 $
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
 
package org.opencms.staticexport;

import org.opencms.file.CmsObject;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.report.CmsLogReport;
import org.opencms.report.I_CmsReport;
import org.opencms.scheduler.I_CmsScheduledJob;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.collections.ExtendedProperties;

/**
 * A schedulable OpenCms job to write a complete static export (e.g. nightly exports).<p>
 * 
 * To enable logging add the following line to <code>opencms.properties</code> in the logging
 * configuration section:
 * 
 * <pre>
 * log4j.logger.org.opencms.staticexport.CmsStaticExportJob=INFO
 * </pre>
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @version $Revision: 1.1 $
 */
public class CmsStaticExportJob implements I_CmsScheduledJob {
    
    /**
     * @see org.opencms.scheduler.I_CmsScheduledJob#launch(org.opencms.file.CmsObject, org.apache.commons.collections.ExtendedProperties)
     */
    public String launch(CmsObject cms, ExtendedProperties parameter) throws Exception {

        I_CmsReport report = null;
        
        try {
            report = new CmsLogReport(CmsStaticExportJob.class);
            OpenCms.getStaticExportManager().exportFullStaticRender(cms, true, report);
        } catch (CmsException e) {
            report.println(e);
        } catch (IOException e) {
            report.println(e);            
        } catch (ServletException e) {
            report.println(e);
        }  finally {
            // append runtime statistics to the report
            StringBuffer stats = new StringBuffer();
            stats.append(report.key("report.publish_stats"));
            stats.append(report.key("report.publish_stats_duration"));
            stats.append(report.formatRuntime());
            report.println(stats.toString());        
            report.println(report.key("report.staticexport_end"), I_CmsReport.C_FORMAT_HEADLINE);            
        }
        
        return null;
    }

}
