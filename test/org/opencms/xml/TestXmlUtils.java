/*
 * File   : $Source: /alkacon/cvs/opencms/test/org/opencms/xml/TestXmlUtils.java,v $
 * Date   : $Date: 2004/12/07 13:43:59 $
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
 
package org.opencms.xml;

import junit.framework.TestCase;

/**
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * @version $Revision: 1.1 $
 * 
 * @since 5.5.4
 */
public class TestXmlUtils extends TestCase {

    /**
     * Test case for the Xpath generation methods.
     * 
     * @throws Exception in case the test fails
     */
    public void testCreateXpath() throws Exception {
        
        assertEquals("Title[1]", CmsXmlUtils.createXpath("Title", 1));
        assertEquals("Title[1]/Test[1]", CmsXmlUtils.createXpath("Title/Test", 1));
        assertEquals("Title[1]/Test[1]/Toast[1]", CmsXmlUtils.createXpath("Title/Test/Toast", 1));
        assertEquals("Title[4]/Test[2]/Toast[1]", CmsXmlUtils.createXpath("Title[4]/Test[2]/Toast[1]", 1));        
        assertEquals("Title[1]/Test[2]/Toast[2]", CmsXmlUtils.createXpath("Title/Test[2]/Toast", 2));
        assertEquals("Title[1]/Test[1]/Toast[1]/Toll[5]", CmsXmlUtils.createXpath("Title/Test/Toast/Toll", 5));
    }
}
