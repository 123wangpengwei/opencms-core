/*
 * File   : $Source: /alkacon/cvs/opencms/test/org/opencms/test/OpenCmsTestProperties.java,v $
 * Date   : $Date: 2005/04/13 07:37:02 $
 * Version: $Revision: 1.4 $
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

package org.opencms.test;

import java.io.IOException;

import org.apache.commons.collections.ExtendedProperties;
import org.opencms.util.CmsPropertyUtils;

/**
 * Reads and manages the test.properties file.<p>
 * 
 * @author Michael Moossen (m.moossen@alkacon.com)
 * @version $Revision: 1.4 $
 * 
 * @since 6.0.0
 */
public final class OpenCmsTestProperties {

    /**
     * the singleton instance.
     */
    private static OpenCmsTestProperties m_testSingleton;

    /**
     * the path to the test.properties file.
     */
    private String m_basePath;

    /**
     * the path to the data test folder.
     */
    private String m_testDataPath;

    /**
     * the path to the webapp test folder.
     */
    private String m_testWebappPath;

    /**
     * private default constructor.
     */
    private OpenCmsTestProperties() {

        // noop
    }

    /**
     * @return the singleton instance
     */
    public static OpenCmsTestProperties getInstance() {

        if (m_testSingleton == null) {
            throw new RuntimeException("You have to initialize the test properties.");
        }
        return m_testSingleton;
    }

    /**
     * Reads property file test.properties and fills singleton members.<p>
     * 
     * @param basePath the path where to find the test.properties file
     */
    public static void initialize(String basePath) {

        if (m_testSingleton != null) {
            return;
        }

        ExtendedProperties props = null;

        m_testSingleton = new OpenCmsTestProperties();

        m_testSingleton.m_basePath = basePath;
        if (!m_testSingleton.m_basePath.endsWith("/")) {
            m_testSingleton.m_basePath += "/";
        }

        try {
            props = CmsPropertyUtils.loadProperties(Thread.currentThread().getContextClassLoader().getResource("test.properties").getFile());
        } catch (IOException e) {
            e.printStackTrace(System.out);
            throw new RuntimeException(e);
        }

        m_testSingleton.m_testDataPath = props.getString("test.data.path");
        m_testSingleton.m_testWebappPath = props.getString("test.webapp.path");
    }

    /**
     * @return Returns the path to the test.properties file
     */
    public String getBasePath() {

        return m_basePath;
    }

    /**
     * @return the path to the data test directory
     */
    public String getTestDataPath() {

        return m_testDataPath;
    }

    /**
     * @return the path to the webapp test directory
     */
    public String getTestWebappPath() {

        return m_testWebappPath;
    }
}