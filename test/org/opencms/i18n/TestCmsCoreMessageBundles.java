/*
 * File   : $Source: /alkacon/cvs/opencms/test/org/opencms/i18n/TestCmsCoreMessageBundles.java,v $
 * Date   : $Date: 2007/07/04 16:57:37 $
 * Version: $Revision: 1.8 $
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
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tests all {@link org.opencms.i18n.I_CmsMessageBundle} instances for the OpenCms 
 * core classes (folder src, org.* packages). <p>
 * 
 * @author Achim Westermann 
 * @author Jan Baudisch 
 * 
 * @version $Revision: 1.8 $
 * 
 * @version $Revision: 1.8 $
 * 
 * @since 6.0.0
 */
public final class TestCmsCoreMessageBundles extends TestCmsMessageBundles {

    /**
     * @see org.opencms.i18n.TestCmsMessageBundles#getTestMessageBundles()
     */
    protected I_CmsMessageBundle[] getTestMessageBundles() {

        return A_CmsMessageBundle.getOpenCmsMessageBundles();
    }

    /**
     * @see org.opencms.i18n.TestCmsMessageBundles#getNotLocalizedBundles(Locale)
     */
    protected List getNotLocalizedBundles(Locale locale) {

        List bundles = new ArrayList();
        bundles.add(org.opencms.setup.Messages.get());
        bundles.add(org.opencms.setup.xml.Messages.get());
        return bundles;
    }
}
