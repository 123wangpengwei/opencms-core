/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/report/Attic/CmsHtmlReport.java,v $
 * Date   : $Date: 2003/07/12 11:29:22 $
 * Version: $Revision: 1.10 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002  The OpenCms Group
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
 *
 * First created on 08.12.2002
 */
package com.opencms.report;

import com.opencms.file.CmsResource;
import com.opencms.flex.util.CmsMessages;
import com.opencms.flex.util.CmsStringSubstitution;
import com.opencms.linkmanagement.CmsPageLinks;
import com.opencms.util.Encoder;
import com.opencms.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * HTML report output to be used for import / export / publish operations 
 * in the entire OpenCms system.<p>
 * 
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * @version $Revision: 1.10 $
 * 
 * @since 5.0 rc 1
 */
public class CmsHtmlReport implements I_CmsReport {
    
    /** Constant for a HTML linebreak with added "real" line break) */
    private static final String C_LINEBREAK = "<br>\\n";
    
    /** Localized message access object */
    private CmsMessages m_messages;        
    
    /**
     * Constructs a new report using the provided locale for the output language.<p>
     * 
     * @param locale a 2-letter language code according to ISO 639 
     */    
    public CmsHtmlReport(String locale) {
        m_messages = new CmsMessages(C_BUNDLE_NAME, locale);
        m_content = new ArrayList(256);
    }
    
    /** The list of report objects e.g. String, CmsPageLink, Exception ... */
    private List m_content;

    /**
     * Counter to remember what is already shown,
     * indicates the next index of the m_content list that has to be reported
     */
    private int m_indexNext;
    
    /** Flag to indicate if broken links have been reported */
    private boolean m_hasBrokenLinks = false;
    
    /** Flag to indicate if exception should be displayed long or short */
    private boolean m_showExceptionStackTracke = true; 

    /**
     * @see com.opencms.report.I_CmsReport#println()
     */
    public void println() {
        this.print(C_LINEBREAK);
    }

    /**
     * @see com.opencms.report.I_CmsReport#print(java.lang.String)
     */
    public void print(String value) {
        this.print(value, C_FORMAT_DEFAULT);
    }

    /**
     * @see com.opencms.report.I_CmsReport#println(java.lang.String)
     */
    public void println(String value) {
        this.print(value + C_LINEBREAK, C_FORMAT_DEFAULT);
    }
    
    /**
     * @see com.opencms.report.I_CmsReport#print(java.lang.String, int)
     */
    public void print(String value, int format) {
        value = convertChars(value);
        StringBuffer buf;
        switch (format) { 
            case C_FORMAT_HEADLINE:
                buf = new StringBuffer();
                buf.append("<span style='color: #000099; font-weight: bold;'>");
                buf.append(value);
                buf.append("</span>");   
                m_content.add(buf);                         
                break;
            case C_FORMAT_WARNING:
                buf = new StringBuffer();
                buf.append("<span style='color: #990000; padding-left:40px;'>");
                buf.append(value);
                buf.append("</span>");
                m_content.add(buf);            
                break;
            case C_FORMAT_NOTE:
                buf = new StringBuffer();
                buf.append("<span style='color: #666666;'>");
                buf.append(value);
                buf.append("</span>");
                m_content.add(buf);
                break;        
            case C_FORMAT_OK:
                buf = new StringBuffer();
                buf.append("<span style='color: #009900;'>");
                buf.append(value);
                buf.append("</span>");
                m_content.add(buf);
                break;                        
            case C_FORMAT_DEFAULT:
            default:
                m_content.add(value);
        }                    
    }
    
    /**
     * @see com.opencms.report.I_CmsReport#println(java.lang.String, int)
     */
    public void println(String value, int format) {
        this.print(value + C_LINEBREAK, format);        
    }
    
    /**
     * @see com.opencms.report.I_CmsReport#println(com.opencms.linkmanagement.CmsPageLinks)
     */
    public void println(CmsPageLinks value) {
        m_hasBrokenLinks = true;
        m_content.add(value);
    }

    /**
     * @see com.opencms.report.I_CmsReport#println(java.lang.Throwable)
     */
    public void println(Throwable t) {
        m_content.add(t);
    }
    
    /**
     * Converts chars and removes linebreaks from a String.<p>
     * 
     * @param value the String to convert
     * @return the char converted String without linebreaks
     */
    private String convertChars(String value) {
        value = CmsStringSubstitution.substitute(value, "\"", "\\\"");
        StringBuffer buf = new StringBuffer();
        StringTokenizer tok = new StringTokenizer(value, "\r\n");
        while (tok.hasMoreTokens()) {
            buf.append(tok.nextToken());
            buf.append(" ");
        }
        return buf.toString(); 
    }

    /**
     * Output helper method to format a reported <code>Throwable</code> element.<p>
     * 
     * This method ensures that exception stack traces are properly escaped
     * when they are added to the report.<p>
     * 
     * There is a member variable {@link #m_showExceptionStackTracke} in this
     * class that controls if the stack track is shown or not.
     * In a later version this might be configurable on a per-user basis.<p>
     *      
     * @param throwable the exception to format
     * @return the formatted StringBuffer
     */
    private StringBuffer getExceptionElement(Throwable throwable) {        
        StringBuffer buf = new StringBuffer();
        if (m_showExceptionStackTracke) {
            buf.append("<span style='color: #990000;'>");
            buf.append(m_messages.key("report.exception"));
            String exception = Encoder.escapeXml(Utils.getStackTrace(throwable));
            exception = CmsStringSubstitution.substitute(exception, "\\", "\\\\");
            StringTokenizer tok = new StringTokenizer(exception, "\r\n");
            while (tok.hasMoreTokens()) {
                buf.append(tok.nextToken());
                buf.append(C_LINEBREAK);
            }            
            buf.append("</span>");
        } else {
            buf.append("<span style='color: #990000;'>");
            buf.append(m_messages.key("report.exception"));
            buf.append(throwable.toString());
            buf.append("</span>");
            buf.append(C_LINEBREAK);
        }        
        return buf;
    }
    
    /**
     * Output helper method to format a reported <code>CmsPageLinks</code> element.<p>
     * 
     * This method formats the link source.<p>
     *
     * @param link the link resource
     * @return the formatted StringBuffer
     */
    private StringBuffer getLinkElement(String link) {
        StringBuffer buf = new StringBuffer();
        buf.append("<span style='color: #666666;'>");
        buf.append(m_messages.key("report.checking"));
        buf.append("</span>");        
        buf.append(CmsResource.getAbsolutePath(link));
        buf.append(C_LINEBREAK);
        return buf;        
    }
    
    /**
     * Output helper method to format a reported <code>CmsPageLinks</code> element.<p>
     *
      * This method formats the link targets.<p>
      * 
     * @param target the link target resource
     * @return the formatted StringBuffer
     */    
    private StringBuffer getLinkTargetElement(String target) {
        StringBuffer buf = new StringBuffer("<span style='padding-left:40px; color: #666666;'>");
        buf.append(m_messages.key("report.broken_link_to"));
        buf.append("<span style='color: #990000;'>");
        buf.append(CmsResource.getAbsolutePath(target));
        buf.append("</span></span>");        
        buf.append(C_LINEBREAK);
        return buf;
    }    

    /**
     * @see com.opencms.report.I_CmsReport#getReportUpdate()
     */
    public synchronized String getReportUpdate() {
        StringBuffer result = new StringBuffer();
        int indexEnd = m_content.size();
        for (int i=m_indexNext; i<indexEnd; i++) {
            Object obj = m_content.get(i);
            if (obj instanceof CmsPageLinks) {
                CmsPageLinks links = (CmsPageLinks)m_content.get(i);
                result.append(getLinkElement((String)links.getResourceName()));                                
                for (int index=0; index<links.getLinkTargets().size(); index++) {                    
                    result.append(getLinkTargetElement((String)links.getLinkTargets().elementAt(index)));
                }                                
            } else if (obj instanceof String || obj instanceof StringBuffer) {
                result.append(obj);
            } else if (obj instanceof Throwable) {
                result.append(getExceptionElement((Throwable)obj));
            }
        }
        m_indexNext = indexEnd;

        return result.toString();
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
