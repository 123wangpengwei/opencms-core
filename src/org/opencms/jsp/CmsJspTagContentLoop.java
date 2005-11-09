/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/jsp/CmsJspTagContentLoop.java,v $
 * Date   : $Date: 2005/11/09 14:41:19 $
 * Version: $Revision: 1.17.2.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
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

package org.opencms.jsp;

import org.opencms.i18n.CmsMessageContainer;
import org.opencms.xml.A_CmsXmlDocument;
import org.opencms.xml.CmsXmlUtils;

import java.util.List;
import java.util.Locale;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Used to loop through the element values of an XML content item.<p>
 * 
 * @author  Alexander Kandzior 
 * 
 * @version $Revision: 1.17.2.1 $ 
 * 
 * @since 6.0.0 
 */
public class CmsJspTagContentLoop extends TagSupport implements I_CmsJspTagContentContainer {

    /** Serial version UID required for safe serialization. */
    private static final long serialVersionUID = 8832749526732064836L;

    /** Reference to the looped content element. */
    private A_CmsXmlDocument m_content;

    /** Name of the current element (including the index). */
    private String m_currentElement;

    /** Name of the content node element to show. */
    private String m_element;

    /** Index of the content node element to show. */
    private int m_index = -1;

    /** Refenence to the currently selected locale. */
    private Locale m_locale;

    /** Reference to the parent 'contentload' tag. */
    private I_CmsJspTagContentContainer m_parentTag;

    /**
     * @see javax.servlet.jsp.tagext.TagSupport#doAfterBody()
     */
    public int doAfterBody() {

        if (m_content.hasValue(m_element, m_locale, m_index + 1)) {
            m_index++;
            m_currentElement = CmsXmlUtils.createXpath(m_element, m_index + 1);
            // one more element with the same name is available, loop again
            return EVAL_BODY_AGAIN;
        } else {
            // no more elements with this name available, finish loop
            return SKIP_BODY;
        }
    }

    /**
     * @see javax.servlet.jsp.tagext.Tag#doStartTag()
     */
    public int doStartTag() throws JspException {

        // get a reference to the parent "content container" class
        Tag ancestor = findAncestorWithClass(this, I_CmsJspTagContentContainer.class);
        if (ancestor == null) {
            CmsMessageContainer errMsgContainer = Messages.get().container(Messages.ERR_PARENTLESS_TAG_1, "contentloop");
            String msg = Messages.getLocalizedMessage(errMsgContainer, pageContext);
            throw new JspTagException(msg);
        }
        m_parentTag = (I_CmsJspTagContentContainer)ancestor;

        // append to parent element name (required for nested schemas)
        m_element = CmsXmlUtils.concatXpath(m_parentTag.getXmlDocumentElement(), m_element);

        // get loaded content from parent <contentload> tag
        m_content = m_parentTag.getXmlDocument();
        m_locale = m_parentTag.getXmlDocumentLocale();
        m_index = 0;

        if (m_content.hasValue(m_element, m_locale)) {
            // selected element is available at last once in content
            m_currentElement = CmsXmlUtils.createXpath(m_element, m_index + 1);
            return EVAL_BODY_INCLUDE;
        } else {
            // no value available for the selected element name, so we skip the whole body
            return SKIP_BODY;
        }
    }

    /**
     * @see org.opencms.jsp.I_CmsJspTagContentContainer#getCollectorName()
     */
    public String getCollectorName() {

        return m_parentTag.getCollectorName();
    }

    /**
     * @see org.opencms.jsp.I_CmsJspTagContentContainer#getCollectorParam()
     */
    public String getCollectorParam() {

        return m_parentTag.getCollectorParam();
    }

    /**
     * @see org.opencms.jsp.I_CmsJspTagContentContainer#getCollectorResult()
     */
    public List getCollectorResult() {

        return m_parentTag.getCollectorResult();
    }

    /**
     * Returns the name of the content node element to show.<p>
     * 
     * @return the name of the content node element to show
     */
    public String getElement() {

        return (m_element != null) ? m_element : "";
    }

    /**
     * @see org.opencms.jsp.I_CmsJspTagContentContainer#getResourceName()
     */
    public String getResourceName() {

        return m_parentTag.getResourceName();
    }

    /**
     * @see org.opencms.jsp.I_CmsJspTagContentContainer#getXmlDocument()
     */
    public A_CmsXmlDocument getXmlDocument() {

        return m_content;
    }

    /**
     * @see org.opencms.jsp.I_CmsJspTagContentContainer#getXmlDocumentElement()
     */
    public String getXmlDocumentElement() {

        return m_currentElement;
    }

    /**
     * @see org.opencms.jsp.I_CmsJspTagContentContainer#getXmlDocumentLocale()
     */
    public Locale getXmlDocumentLocale() {

        return m_locale;
    }

    /**
     * @see org.opencms.jsp.I_CmsJspTagContentContainer#isPreloader()
     */
    public boolean isPreloader() {

        return m_parentTag.isPreloader();
    }

    /**
     * @see javax.servlet.jsp.tagext.Tag#release()
     */
    public void release() {

        m_element = null;
        m_currentElement = null;
        m_content = null;
        m_locale = null;
        m_parentTag = null;
        m_index = 0;
        super.release();
    }

    /**
     * Sets the name of the content node element to show.<p>
     * 
     * @param element the name of the content node element to show
     */
    public void setElement(String element) {

        m_element = element;
    }
}