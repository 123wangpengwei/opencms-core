/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/report/Attic/CmsShellReport.java,v $
 * Date   : $Date: 2002/12/13 09:16:19 $
 * Version: $Revision: 1.3 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2001  The OpenCms Group
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about OpenCms, please see the
 * OpenCms Website: http://www.opencms.org
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.opencms.report;

import com.opencms.flex.util.CmsMessages;
import com.opencms.linkmanagement.CmsPageLinks;
import com.opencms.workplace.I_CmsWpConstants;

/**
 * Report class used for the shell.<p>
 * 
 * It stores nothing. It just prints everthing to <code>System.out</code>.
 * 
 * @author Hanjo Riege
 * @author Alexander Kandzior (a.kandzior@alkacon.com) 
 *  
 * @version $Revision: 1.3 $
 */
public class CmsShellReport implements I_CmsReport {

    /** Localized message access object */
    private CmsMessages m_messages;
    
    /** Flag to indicate if broken links have been reported */
    private boolean m_hasBrokenLinks = false;
    
    /**
     * Empty default constructor. 
     * 
     * @see java.lang.Object#Object()
     */
    public CmsShellReport() {
        // generate a message object with the default (english) language
        m_messages = new CmsMessages(C_BUNDLE_NAME, I_CmsWpConstants.C_DEFAULT_LANGUAGE);        
    }

    /**
     * @see com.opencms.report.I_CmsReport#addSeperator(java.lang.String)
     */
    public void addSeperator(String message) {
        this.println(m_messages.key(message), C_FORMAT_HEADLINE);
    }
    
    /**
     * @see com.opencms.report.I_CmsReport#addSeperator(java.lang.String, java.lang.String)
     */
    public void addSeperator(String message, String info) {
        StringBuffer buf = new StringBuffer(m_messages.key(message));
        if (info != null) buf.append(info);      
        this.println(new String(buf), C_FORMAT_HEADLINE);
    }
        
    /**
     * @see com.opencms.report.I_CmsReport#addSeperator()
     */
    public void addSeperator(){
        System.out.println();
    }

    /**
     * @see com.opencms.report.I_CmsReport#addString(java.lang.String)
     */
    public void print(String value){
        this.print(value, C_FORMAT_DEFAULT);
    }
    
    /**
     * @see com.opencms.report.I_CmsReport#println(java.lang.String)
     */
    public void println(String value) {
       this.println(value, C_FORMAT_DEFAULT);
    }
        
    /**
     * @see com.opencms.report.I_CmsReport#print(java.lang.String, int)
     */
    public void print(String value, int format) {
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
     * @see com.opencms.report.I_CmsReport#println(java.lang.String, int)
     */
    public void println(String value, int format) {
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
     * @see com.opencms.report.I_CmsReport#getReportUpdate()
     */
    public String getReportUpdate(){
        return "";
    }
    
    /**
     * @see com.opencms.report.I_CmsReport#println(java.lang.Throwable)
     */
    public void println(Throwable t) {
        StringBuffer buf = new StringBuffer();        
        buf.append(m_messages.key("report.exception"));   
        buf.append(t.getMessage());
        this.println(new String(buf), C_FORMAT_WARNING);
        t.printStackTrace(System.out);
    }
    
    /**
     * @see com.opencms.report.I_CmsReport#println(com.opencms.linkmanagement.CmsPageLinks)
     */
    public void println(CmsPageLinks value) {     
        m_hasBrokenLinks = true;           
        this.println((String)value.getResourceName());
        for(int index=0; index<value.getLinkTargets().size(); index++){
            this.println("     " + m_messages.key("report.broken_link_to") + (String)value.getLinkTargets().elementAt(index));
        }
    }
    
    /**
     * @see com.opencms.report.I_CmsReport#hasBrokenLinks()
     */
    public boolean hasBrokenLinks() {
        return m_hasBrokenLinks;
    }
    
    /**
     * @see com.opencms.report.I_CmsReport#key(java.lang.String)
     */
    public String key(String keyName) {
        return m_messages.key(keyName);
    }
}