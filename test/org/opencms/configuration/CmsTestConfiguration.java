/*
 * File   : $Source: /alkacon/cvs/opencms/test/org/opencms/configuration/CmsTestConfiguration.java,v $
 * Date   : $Date: 2004/03/18 15:04:26 $
 * Version: $Revision: 1.6 $
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

package org.opencms.configuration;

import org.opencms.main.OpenCms;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.digester.Digester;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.xml.sax.SAXException;

/**
 * Dummy class for configuration testing.<p>
 * 
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * @since 5.3
 */
public class CmsTestConfiguration extends A_CmsXmlConfiguration implements I_CmsXmlConfiguration {
    
    /** The name of the DTD for this configuration */
    private static final String C_CONFIGURATION_DTD_NAME = "opencms-tests.dtd";
    
    /** The name of the default XML file for this configuration */
    private static final String C_DEFAULT_XML_FILE_NAME = "opencms-tests.xml";
    
    /** Test content 1 */
    private String m_content1;
    
    /** Test content 2 */
    private String m_content2;
    
    /**
     * The public contructor is hidden to prevent generation of instances of this class.<p> 
     */
    public CmsTestConfiguration() {
        setXmlFileName(C_DEFAULT_XML_FILE_NAME);
        if (OpenCms.getLog(this).isDebugEnabled()) {
            OpenCms.getLog(this).debug("Empty constructor called on " + this);
        }  
    }
    
    /**
     * Test method to add a value.<p>
     * 
     * @param name the name of the test
     * @param value the value of the test
     */
    public void addTest(String name, String value) {
        if ("test1".equals(name)) {
            m_content1 = value;
        }
        if ("test2".equals(name)) {
            m_content2 = value;
        }        
    }   

    /**
     * @see org.opencms.configuration.I_CmsXmlConfiguration#addXmlDigesterRules(org.apache.commons.digester.Digester)
     */
    public void addXmlDigesterRules(Digester digester) {
        // add test rules
        digester.addCallMethod("*/tests/test", "addTest", 2);
        digester.addCallParam("*/tests/test", 0, A_NAME);
        digester.addCallParam("*/tests/test", 1);
    }

    /**
     * @see org.opencms.configuration.I_CmsXmlConfiguration#generateXml(org.dom4j.Element)
     */
    public Element generateXml(Element parent) {
        Element testElement = parent.addElement("tests");
        if (m_content1 != null) {
            testElement.addElement("test").addAttribute(A_NAME, "test1").addText(m_content1);
        }
        if (m_content2 != null) {
            testElement.addElement("test").addAttribute(A_NAME, "test2").addText(m_content2);
        }
        return testElement;
    }
    
    /**
     * Main executable for testing purposes.<p>
     * 
     * @param args the arguments
     * @throws SAXException in case of XML parsing issues
     * @throws IOException in case of file IO issues
     */
    public static void main(String[] args) throws SAXException, IOException {

        // set "OpenCms" system property to "test" for allowing the logger to be used
        System.setProperty("OpenCmsLog", "opencms_test.log");
        
        // get URL of test input resource
        URL inputUrl = ClassLoader.getSystemResource("org/opencms/configuration/");
        // generate the configuration manager
        CmsConfigurationManager manager = new CmsConfigurationManager(inputUrl.getFile());
        // now digest the XML
        manager.loadXmlConfiguration();
       
        
        List allConfigurations = new ArrayList();
        allConfigurations.add(manager);
        allConfigurations.addAll(manager.getConfigurations());
        
        Iterator i = allConfigurations.iterator();
        while (i.hasNext()) {
            I_CmsXmlConfiguration config = (I_CmsXmlConfiguration)i.next();
            System.out.println("\n\nConfiguration instance: " + config + ":\n");
            
            // gernerate XML document for the configuration
            Document doc = manager.generateXml(config);
                                   
            // output the document
            XMLWriter writer;        
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setIndentSize(4);
            format.setTrimText(false);
            format.setEncoding("UTF-8");
            writer = new XMLWriter(System.out, format);
            writer.write(doc);                 
            
        }   
    }

    /**
     * @see org.opencms.configuration.I_CmsXmlConfiguration#getDtdFilename()
     */
    public String getDtdFilename() {
        return C_CONFIGURATION_DTD_NAME;
    }    
}
