/*
 * File   : $Source: /alkacon/cvs/opencms/test/com/opencms/flex/util/Attic/CmsStringSubstitutionTest.java,v $
 * Date   : $Date: 2003/03/07 15:16:36 $
 * Version: $Revision: 1.8 $
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

package com.opencms.flex.util;

import junit.framework.TestCase;

/** 
 * Test cases for the class "CmsStringSubstitution"
 * 
 * @author Andreas Zahner (a.zahner@alkacon.com)
 * @version $Revision: 1.8 $
 * 
 * @since 5.0
 */
public class CmsStringSubstitutionTest extends TestCase {

	/**
	 * Constructor for CmsStringSubstitutionTest.
	 * @param arg0
	 */
	public CmsStringSubstitutionTest(String arg0) {
		super(arg0);
	}

	public void testSubstitute() {
		String test, result;
        
		String content = "<a href=\"/opencms/opencms/test.jpg\">";
		String search = "/opencms/opencms/";
		String replace = "\\${path}";
		
		test = CmsStringSubstitution.substitute(content, search, replace);
        System.err.println(test);
		assertEquals(test,"<a href=\"\\${path}test.jpg\">");
		
		test = CmsStringSubstitution.substitute(test, replace, search);	
		assertEquals(test,"<a href=\"/opencms/opencms/test.jpg\">");
        
        content = "[0-9]$1/[^a]|/([}>\"'\\[]\\s*)/pics/";
        result  = "[0-9]$1/[^a]|/([}>\"'\\[]\\s*)/pucs/";
        test = CmsStringSubstitution.substitute(content, "i", "u");   
        assertEquals(test, result);   
	}

	public void testEscapePattern() {
		String test;
		test = CmsStringSubstitution.escapePattern("/opencms/opencms");
		assertEquals(test, "\\/opencms\\/opencms");	
		test = CmsStringSubstitution.escapePattern("/opencms/$");
		assertEquals(test, "\\/opencms\\/\\$");			
	}
	
	public void testCombined() {
		String test;
		String content = "<p>A paragraph with text...<img src=\"/opencms/opencms/empty.gif\"></p>\n<a href=\"/opencms/opencms/test.jpg\">";
		String search = "/opencms/opencms/";
		String replace = "${path}";
		test = CmsStringSubstitution.substitute(content, search, replace);	
		assertEquals(test,"<p>A paragraph with text...<img src=\"${path}empty.gif\"></p>\n<a href=\"${path}test.jpg\">");
		test = CmsStringSubstitution.substitute(test, replace, search);;		
		assertEquals(test,"<p>A paragraph with text...<img src=\"/opencms/opencms/empty.gif\"></p>\n<a href=\"/opencms/opencms/test.jpg\">");
	}
    
    public void testLine() {
        String content = "<edittemplate><![CDATA[<H4><IMG style=\"WIDTH: 77px; HEIGHT: 77px\" alt=\"Homepage animation\" hspace=8 src=\"/opencms/opencms/pics/alkacon/x_hp_ani04.gif\" align=right vspace=8 border=0><IMG style=\"WIDTH: 307px; HEIGHT: 52px\" alt=\"Homepage animation\" hspace=0 src=\"/opencms/opencms/pics/alkacon/x_hp_ani05.gif\" vspace=8 border=0></H4>\n<P>Alkacon Software provides software development services for the digital business. We are specialized in web - based content management solutions build on open source Java Software. </P>\n<P>Alkacon Software is a major contributor to the <A href=\"http://www.opencms.org\" target=_blank>OpenCms Project</A>. OpenCms is an enterprise - ready content management platform build in Java from open source components. OpenCms can easily be deployed on almost any existing IT infrastructure and provides powerful features especially suited for large enterprise internet or intranet applications. </P>\n<P>Alkacon Software offers standard <A href=\"/alkacon/en/services/opencms/index.html\" target=_self>service and support </A>packages for OpenCms, providing an optional layer of security and convenience often required for mission critical OpenCms installations.</P>\n<UL>\n<LI><IMG style=\"WIDTH: 125px; HEIGHT: 34px\" alt=OpenCms hspace=3 src=\"/opencms/opencms/pics/alkacon/logo_opencms_125.gif\" align=right border=0>Learn more about our <A href=\"/alkacon/en/services/index.html\" target=_self>Services</A> \n<LI>Subscribe to our&nbsp;<A href=\"/alkacon/en/company/contact/newsletter.html\" target=_self>Company Newsletter</A> \n<LI>Questions? <A href=\"/alkacon/en/company/contact/index.html\" target=_self>Contact us</A></LI></UL>\n<P>&nbsp;</P>]]></edittemplate>";
        String search = "/pics/";
        String replace = "/system/galleries/pics/";
        String test = CmsStringSubstitution.substitute(content, search, replace);
        assertEquals(test,"<edittemplate><![CDATA[<H4><IMG style=\"WIDTH: 77px; HEIGHT: 77px\" alt=\"Homepage animation\" hspace=8 src=\"/opencms/opencms/system/galleries/pics/alkacon/x_hp_ani04.gif\" align=right vspace=8 border=0><IMG style=\"WIDTH: 307px; HEIGHT: 52px\" alt=\"Homepage animation\" hspace=0 src=\"/opencms/opencms/system/galleries/pics/alkacon/x_hp_ani05.gif\" vspace=8 border=0></H4>\n<P>Alkacon Software provides software development services for the digital business. We are specialized in web - based content management solutions build on open source Java Software. </P>\n<P>Alkacon Software is a major contributor to the <A href=\"http://www.opencms.org\" target=_blank>OpenCms Project</A>. OpenCms is an enterprise - ready content management platform build in Java from open source components. OpenCms can easily be deployed on almost any existing IT infrastructure and provides powerful features especially suited for large enterprise internet or intranet applications. </P>\n<P>Alkacon Software offers standard <A href=\"/alkacon/en/services/opencms/index.html\" target=_self>service and support </A>packages for OpenCms, providing an optional layer of security and convenience often required for mission critical OpenCms installations.</P>\n<UL>\n<LI><IMG style=\"WIDTH: 125px; HEIGHT: 34px\" alt=OpenCms hspace=3 src=\"/opencms/opencms/system/galleries/pics/alkacon/logo_opencms_125.gif\" align=right border=0>Learn more about our <A href=\"/alkacon/en/services/index.html\" target=_self>Services</A> \n<LI>Subscribe to our&nbsp;<A href=\"/alkacon/en/company/contact/newsletter.html\" target=_self>Company Newsletter</A> \n<LI>Questions? <A href=\"/alkacon/en/company/contact/index.html\" target=_self>Contact us</A></LI></UL>\n<P>&nbsp;</P>]]></edittemplate>");     
    }
    
    public void testComplexPatternForImport() {
        String content = 
            "<cms:link>/pics/test.gif</cms:link> <img src=\"/pics/test.gif\"> script = '/pics/test.gif' <cms:link> /pics/othertest.gif </cms:link>\n" +
            "<cms:link>/mymodule/pics/test.gif</cms:link> <img src=\"/mymodule/pics/test.gif\"> script = '/mymodule/pics/test.gif' <cms:link> /mymodule/system/galleries/pics/othertest.gif </cms:link>";
        String search = "([>\"']\\s*)/pics/";
        String replace = "$1/system/galleries/pics/";
        String test = CmsStringSubstitution.substitutePerl(content, search, replace, "g");
        assertEquals(test, 
            "<cms:link>/system/galleries/pics/test.gif</cms:link> <img src=\"/system/galleries/pics/test.gif\"> script = '/system/galleries/pics/test.gif' <cms:link> /system/galleries/pics/othertest.gif </cms:link>\n" +
            "<cms:link>/mymodule/pics/test.gif</cms:link> <img src=\"/mymodule/pics/test.gif\"> script = '/mymodule/pics/test.gif' <cms:link> /mymodule/system/galleries/pics/othertest.gif </cms:link>");    
    }
}
