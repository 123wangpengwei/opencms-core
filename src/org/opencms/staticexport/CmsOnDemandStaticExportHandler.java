/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/staticexport/CmsOnDemandStaticExportHandler.java,v $
 * Date   : $Date: 2005/02/17 12:44:32 $
 * Version: $Revision: 1.5 $
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

package org.opencms.staticexport;

import java.util.Collections;
import java.util.List;

/**
 * The <code>CmsOnDemandStaticExportHandler</code> is the default implementation
 * for the <code>{@link I_CmsStaticExportHandler}</code> interface.<p>
 * 
 * This handler is most suitable for dynamic sites that use the static export 
 * as optimization for non-dynamic content.<p>
 * 
 * @author <a href="mailto:m.moossen@alkacon.com">Michael Moossen</a> 
 * @version $Revision: 1.5 $
 * @since 6.0
 * @see I_CmsStaticExportHandler
 */
public class CmsOnDemandStaticExportHandler extends A_CmsOnDemandStaticExportHandler {

    /**
     * @see org.opencms.staticexport.A_CmsOnDemandStaticExportHandler#getRelatedFilesToPurge(java.lang.String)
     */
    protected List getRelatedFilesToPurge(String exportFileName) {

        return Collections.EMPTY_LIST;
    }

}