/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/report/CmsShellReport.java,v $
 * Date   : $Date: 2003/09/25 15:27:01 $
 * Version: $Revision: 1.4 $
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

package org.opencms.report;

import com.opencms.flex.util.CmsMessages;
import com.opencms.linkmanagement.CmsPageLinks;
import com.opencms.workplace.I_CmsWpConstants;

/**
 * Report class used for the shell.<p>
 * 
 * It stores nothing. It just prints everthing to <code>System.out</code>.
 * 
 * @author Alexander Kandzior (a.kandzior@alkacon.com)  
 * @version $Revision: 1.4 $
 */
public class CmsShellReport implements I_CmsReport {

    /** Localized message access object */
    private CmsMessages m_messages;

        
    /**
     * Empty default constructor. 
     * 
     * @see java.lang.Object#Object()
     */
    public CmsShellReport() {
        this(C_BUNDLE_NAME, I_CmsWpConstants.C_DEFAULT_LANGUAGE);    
    }

    /**
     * Constructs a new report using the provided locale and resource bundle
     * for the output language.<p>
     * 
     * @param locale a 2-letter language code according to ISO 639 
     * @param bundleName the name of the resource bundle with localized strings
     */      
    public CmsShellReport(String bundleName, String locale) {
        // generate a message object with the default (english) language
        m_messages = new CmsMessages(bundleName, locale);        
    }
    
    /**
     * @see org.opencms.report.I_CmsReport#getReportUpdate()
     */
    public synchronized String getReportUpdate() {
        return "";
    }
    
    /**
     * @see org.opencms.report.I_CmsReport#key(java.lang.String)
     */
    public String key(String keyName) {
        return m_messages.key(keyName);
    }

    /**
     * @see org.opencms.report.I_CmsReport#print(java.lang.String)
     */
    public synchronized void print(String value) {
        this.print(value, C_FORMAT_DEFAULT);
    }
        
    /**
     * @see org.opencms.report.I_CmsReport#print(java.lang.String, int)
     */
    public synchronized void print(String value, int format) {
        StringBuffer buf;
        switch (format) {
            case C_FORMAT_HEADLINE:
                buf = new StringBuffer();
                buf.append("------ ");
                buf.append(value);
                System.out.print(buf);
                break;
            case C_FORMAT_WARNING:
                buf = new StringBuffer();
                buf.append("!!! ");
                buf.append(value);
                System.out.print(buf);
                break;
            case C_FORMAT_NOTE:
            case C_FORMAT_OK:
            case C_FORMAT_DEFAULT:
            default:
            System.out.print(value);
        }       
    }
        
    /**
     * @see org.opencms.report.I_CmsReport#println()
     */
    public synchronized void println() {
        System.out.println();
    }
    
    /**
     * @see org.opencms.report.I_CmsReport#println(com.opencms.linkmanagement.CmsPageLinks)
     */
    public synchronized void println(CmsPageLinks value) {        
        this.println(value.getResourceName());
        for (int index=0; index<value.getLinkTargets().size(); index++) {
            this.println("     " + m_messages.key("report.broken_link_to") + (String)value.getLinkTargets().elementAt(index));
        }
    }
    
    /**
     * @see org.opencms.report.I_CmsReport#println(java.lang.String)
     */
    public synchronized void println(String value) {
       this.println(value, C_FORMAT_DEFAULT);
    }
    
    /**
     * @see org.opencms.report.I_CmsReport#println(java.lang.String, int)
     */
    public synchronized void println(String value, int format) {
        StringBuffer buf;
        switch (format) {
            case C_FORMAT_HEADLINE:
                buf = new StringBuffer();
                buf.append("------ ");
                buf.append(value);
                System.out.println(buf);
                break;
            case C_FORMAT_WARNING:
                buf = new StringBuffer();
                buf.append("   !!! ");
                buf.append(value);
                System.out.println(buf);
                break;
            case C_FORMAT_NOTE:
            case C_FORMAT_OK:
            case C_FORMAT_DEFAULT:
            default:
            System.out.println(value);
        }      
    }
    
    /**
     * @see org.opencms.report.I_CmsReport#println(java.lang.Throwable)
     */
    public synchronized void println(Throwable t) {
        StringBuffer buf = new StringBuffer();        
        buf.append(m_messages.key("report.exception"));   
        buf.append(t.getMessage());
        this.println(new String(buf), C_FORMAT_WARNING);
        t.printStackTrace(System.out);
    }
}