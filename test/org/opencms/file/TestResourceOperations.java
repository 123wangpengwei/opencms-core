/*
 * File   : $Source: /alkacon/cvs/opencms/test/org/opencms/file/TestResourceOperations.java,v $
 * Date   : $Date: 2004/08/11 10:50:02 $
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
 
package org.opencms.file;

import org.opencms.file.types.CmsResourceTypeFolder;
import org.opencms.file.types.CmsResourceTypePlain;
import org.opencms.test.OpenCmsTestCase;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Unit tests for copy operation.<p>
 * 
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.1 $
 */
public class TestResourceOperations extends OpenCmsTestCase {
  
    /**
     * Default JUnit constructor.<p>
     * 
     * @param arg0 JUnit parameters
     */    
    public TestResourceOperations(String arg0) {
        super(arg0);
    }
    
    /**
     * Test suite for this test class.<p>
     * 
     * @return the test suite
     */
    public static Test suite() {
        
        TestSuite suite = new TestSuite();
        suite.setName(TestResourceOperations.class.getName());
        
        suite.addTest(new TestResourceOperations("testCreateResources"));
        suite.addTest(new TestResourceOperations("testCreateReadFile"));
        suite.addTest(new TestResourceOperations("testPublishFile"));
        suite.addTest(new TestResourceOperations("testCreateSibling"));
        
        TestSetup wrapper = new TestSetup(suite) {
            
            protected void setUp() {
                setupOpenCms(null, null);
            }
            
            protected void tearDown() {
                removeOpenCms();
            }
        };
        
        return wrapper;
    }     
    
    /**
     * Tests the "createResource" operation.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testCreateResources() throws Throwable {

        CmsObject cms = getCmsObject();     
        echo("Testing resource creation");
        
        // create a folder in the root directory
        cms.createResource("/folder1", CmsResourceTypeFolder.C_RESOURCE_TYPE_ID);
        
        // create an empty file in the root directory
        cms.createResource("/resource2", CmsResourceTypePlain.C_RESOURCE_TYPE_ID);
        
        // create an empty file in the created folder 
        cms.createResource("/folder1/resource3", CmsResourceTypePlain.C_RESOURCE_TYPE_ID);        
        
        // ensure first created resource is a folder
        assertIsFolder(cms, "/folder1");
        
        // ensure second created resource is a plain text file
        assertResourceType(cms, "/resource2", CmsResourceTypePlain.C_RESOURCE_TYPE_ID);
        
        // ensure third created resource is a plain text file
        assertResourceType(cms, "/folder1/resource3", CmsResourceTypePlain.C_RESOURCE_TYPE_ID); 

        
    }  

    /**
     * Tests the create and read file methods.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testCreateReadFile() throws Throwable {
        
        CmsObject cms = getCmsObject();
        echo("Testing file creation"); 
        
        String content = "this is a test content";
        
        // create a file in the root directory
        cms.createResource("/file1", CmsResourceTypePlain.C_RESOURCE_TYPE_ID, content.getBytes(), null);
        
        // read and check the content
        this.assertContent(cms, "/file1" , content.getBytes());        
    }

    /**
     * Tests the publish resource method for file.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testPublishFile() throws Throwable {

        CmsObject cms = getCmsObject();
        echo("Testing file publishing");

        String content = "this is a test content";
                
        // set the site root
        cms.getRequestContext().setSiteRoot("/");

        // create a file in the root directory
        cms.createResource("/file2", CmsResourceTypePlain.C_RESOURCE_TYPE_ID, content.getBytes(), null);
        
        // the reosurce must unlocked, otherwise it will not be published
        cms.unlockResource("/file2");
        
        // now publish the file
        cms.publishResource("/file2");
        
        // change the project to online
        cms.getRequestContext().setCurrentProject(cms.readProject("Online"));
        
        // read and check the content
        this.assertContent(cms, "/file2" , content.getBytes()); 
        
        // switch back to offline project
        cms.getRequestContext().setCurrentProject(cms.readProject("Offline")); 
    }
    
    /**
     * Tests the "createSibling" operation.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testCreateSibling() throws Throwable {
        
        CmsObject cms = getCmsObject();     
        echo("Testing sibling creation");
        
        // create an empty file in the root directory
        cms.createResource("/resource4", CmsResourceTypePlain.C_RESOURCE_TYPE_ID);
        
        // ensure that sibling count is zero
        assertSiblingCount(cms, "/resource4", 1);
        
        // create a sibling of res3 in root folder
        cms.createSibling("/resource4", "/sibling1", null);

        // ensure first created resource is a plain text file
        assertResourceType(cms, "/resource4", CmsResourceTypePlain.C_RESOURCE_TYPE_ID);
        
        // ensure sibling is also a plain text file
        assertResourceType(cms, "/sibling1", CmsResourceTypePlain.C_RESOURCE_TYPE_ID);
        
        // check the sibling count
        assertSiblingCount(cms, "/resource4", 2);
        assertSiblingCount(cms, "/sibling1", 2);
    }   
}