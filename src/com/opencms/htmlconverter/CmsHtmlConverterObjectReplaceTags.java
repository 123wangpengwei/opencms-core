/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/htmlconverter/Attic/CmsHtmlConverterObjectReplaceTags.java,v $
* Date   : $Date: 2001/11/21 10:58:33 $
* Version: $Revision: 1.1 $
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

package com.opencms.htmlconverter;

/**
 * Object for replacing HTML tags.
 * @author Andreas Zahner
 * @version 1.0
 */
final class CmsHtmlConverterObjectReplaceTags {

    /** the prefix will be placed in front of every replaced tag */
    private String m_prefix;
    /** name of tag which will be replaced */
    private String m_tagName;
    /** Attribute of tag which will be replaced (optional) */
    private String m_tagAttrib;
    /** value of tag attribute which will be replaced (optional) */
    private String m_tagAttribValue;
    /** String which replaces the start tag */
    private String m_replaceStartTag;
    /** String which replaces the end tag (optional) */
    private String m_replaceEndTag;
    /** the suffix will be placed behind every replaced tag */
    private String m_suffix;
    /** if true, individual replaceStrings will be read from tag attributes */
    private boolean m_getReplaceFromAttrs = false;
    /** tag attribute where the replaceStartTag String is stored */
    private String m_startAttribute;
    /** tag attribute where the replaceEndTag String is stored */
    private String m_endAttribute;
    /** tag attribute where the parameter String is stored */
    private String m_parameter;
    /** if true, only tag attribute specified in parameter String is replaced */
    private boolean m_replaceParamAttr = false;
    /** true, if replaceEndTag is empty */
    private boolean m_inline = false;

    /**
     * default constructor creates object with empty Strings
     */
    protected CmsHtmlConverterObjectReplaceTags () {
        m_prefix = "";
        m_tagName = "";
        m_tagAttrib = "";
        m_tagAttribValue = "";
        m_replaceStartTag = "";
        m_replaceEndTag = "";
        m_suffix = "";
        m_getReplaceFromAttrs = false;
        m_startAttribute = "";
        m_endAttribute = "";
        m_parameter = "";
        m_replaceParamAttr = false;
    }

    /**
     * constructor creates object with parameters
     * @param p String prefix
     * @param tN String tagName
     * @param tA String tagAttribute
     * @param tAV String tagAttributeValue
     * @param rST String replaceStartTag
     * @param rET String replaceEndTag
     * @param s String suffix
     * @param gRFA boolean getReplaceFromAttrs
     * @param sA String startAttribute
     * @param eA String endAttribute
     * @param param String parameter
     */
    protected CmsHtmlConverterObjectReplaceTags (String p, String tN, String tA, String tAV, String rST, String rET, String s, boolean gRFA, String sA, String eA, String param, boolean rPA) {
        m_prefix = p;
        m_tagName = tN;
        m_tagAttrib = tA;
        m_tagAttribValue = tAV;
        m_replaceStartTag = rST;
        m_replaceEndTag = rET;
        m_suffix = s;
        m_getReplaceFromAttrs = gRFA;
        m_startAttribute = sA;
        m_endAttribute = eA;
        m_parameter = param;
        m_replaceParamAttr = rPA;
        if (rET.equals(null) || rET.equals("")) {
            m_inline = true;
        }
    }

    /**
     * returns prefix of actual object
     * @return String with prefix
     */
    protected String getPrefix() {
        return m_prefix;
    }

    /**
     * returns suffix of actual object
     * @return String with suffix
     */
    protected String getSuffix() {
        return m_suffix;
    }

    /**
     * returns tag name of tag which will be replaced
     * @return String with tag name
     */
    protected String getTagName() {
        return m_tagName;
    }

    /**
     * returns attribute of tag which will be replaced
     * @return String with attribute name
     */
    protected String getTagAttrib() {
        return m_tagAttrib;
    }

    /**
     * returns attribute value of tag which will be replaced
     * @return String with attribute value
     */
    protected String getTagAttribValue() {
        return m_tagAttribValue;
    }

    /**
     * returns String which replaces the start tag
     * @return String with prefix, content and suffix
     */
    protected String getReplaceStartTag() {
        return m_prefix + m_replaceStartTag + m_suffix;
    }

    /**
     * returns String which replaces the end tag
     * @return String with prefix, content and suffix
     */
    protected String getReplaceEndTag() {
        if (m_inline == true) {
            return "";
        }
        return m_prefix +m_replaceEndTag +m_suffix;
    }

    /**
     * checks if end tag will be replaced or deleted
     * @return true if m_replaceEndTag is "", otherwise false
     */
    protected boolean getInline() {
        return m_inline;
    }

    /**
     * checks if replacecontents are encoded in html code attributes
     * @return true if replacecontents are encoded, otherwise false
     */
    protected boolean getReplaceFromAttrs() {
        return m_getReplaceFromAttrs;
    }

    /**
     * returns name of start attribute of encoded replacecontents
     * @return String with attribute name
     */
    protected String getStartAttribute() {
        return m_startAttribute;
    }

    /**
     * returns name of end attribute of encoded replacecontents
     * @return String with attribute name
     */
    protected String getEndAttribute() {
        return m_endAttribute;
    }

    /**
     * returns attribute name of parameter
     * @return String with attribute name
     */
    protected String getParameter() {
        return m_parameter;
    }

    /**
     * returns if only an attribute must be replaced
     * @return true if only attribute has to be replaced
     */
    protected boolean getReplaceParamAttr() {
        return m_replaceParamAttr;
    }

}