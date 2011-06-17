/*
 * File   : $Source: /alkacon/cvs/opencms/test/org/opencms/setup/TestCmsSetupBean.java,v $
 * Date   : $Date: 2011/05/03 10:49:06 $
 * Version: $Revision: 1.4 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2011 Alkacon Software GmbH (http://www.alkacon.com)
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

package org.opencms.setup;

import org.opencms.main.CmsSystemInfo;
import org.opencms.test.OpenCmsTestCase;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.collections.ExtendedProperties;

/** 
 * @author Alexander Kandzior 
 * 
 * @version $Revision: 1.4 $
 * 
 * @since 6.0.0
 */
public class TestCmsSetupBean extends OpenCmsTestCase {

    // DEBUG flag
    // private static final boolean DEBUG = true;

    // private static final String PROPERTIES = "/opencms/etc/config/opencms.properties";
    // private static final String PROPERTIES = "/../OpenCms6-Setup/webapp/WEB-INF/config/opencms.properties";

    /**
     * Default JUnit constructor.<p>
     * 
     * @param arg0 JUnit parameters
     */
    public TestCmsSetupBean(String arg0) {

        super(arg0);
    }

    /**
     * Tests the method saveProperties.<p>
     * 
     * @throws IOException if something goes wrong
     */
    public void testSaveProperties() throws IOException {

        CmsSetupBean bean = new CmsSetupBean();
        bean.init("", null, null);

        String base = getTestDataPath(File.separator + "WEB-INF" + File.separator + "base");
        String inputFile = base + CmsSystemInfo.FILE_PROPERTIES;
        String outputFile = base + "output.properties";

        System.out.println("Reading properties from " + inputFile);
        ExtendedProperties oldProperties = bean.loadProperties(inputFile);

        System.out.println("Writing properties to " + outputFile);
        bean.copyFile(inputFile, outputFile);
        bean.saveProperties(oldProperties, outputFile, false);

        System.out.println("Checking properties from " + outputFile);
        ExtendedProperties newProperties = bean.loadProperties(outputFile);

        Iterator it = oldProperties.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry)it.next();
            String key = (String)e.getKey();
            Object obj = e.getValue();

            String oldValue = "", newValue = "";
            if (obj instanceof Vector) {
                StringBuffer buf;

                buf = new StringBuffer();
                for (Iterator j = ((Vector)obj).iterator(); j.hasNext();) {
                    buf.append("[" + (String)j.next() + "]");
                }
                oldValue = buf.toString();

                buf = new StringBuffer();
                for (Iterator j = ((Vector)newProperties.get(key)).iterator(); j.hasNext();) {
                    buf.append("[" + (String)j.next() + "]");
                }
                newValue = buf.toString();

            } else {
                oldValue = (String)obj;
                newValue = (String)newProperties.get(key);
            }
            System.out.println(key);
            System.out.println(oldValue);
            System.out.println(newValue);
            System.out.println("---");
            assertEquals(oldValue, newValue);
        }

        // clean up - remove generated file
        File output = new File(outputFile);
        output.delete();
    }
}
