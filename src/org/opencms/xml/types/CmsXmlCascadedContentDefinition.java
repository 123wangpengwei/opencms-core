/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/xml/types/Attic/CmsXmlCascadedContentDefinition.java,v $
 * Date   : $Date: 2004/11/08 15:06:43 $
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
 
package org.opencms.xml.types;

import org.opencms.file.CmsObject;
import org.opencms.util.CmsStringUtil;
import org.opencms.xml.CmsXmlContentDefinition;
import org.opencms.xml.CmsXmlException;
import org.opencms.xml.I_CmsXmlDocument;

import org.dom4j.Element;

/**
 * A cascaded content XML definition is included (nested) in another XML content definition.<p>
 * 
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.1 $
 * @since 5.5.4
 */
public class CmsXmlCascadedContentDefinition extends A_CmsXmlContentValue implements I_CmsXmlSchemaType {

    /** The cascaded content definition. */
    CmsXmlContentDefinition m_contentDefinition;
        
    /**
     * Returns the cascaded content definition.<p>
     *
     * @return the cascaded content definition
     */
    public CmsXmlContentDefinition getContentDefinition() {

        return m_contentDefinition;
    }
    
    
    /**
     * Creates a new cascaded content definition.<p>
     * 
     * @param contentDefinition the content definition to cascade
     * @param name the type name of the content definition in the containing document
     * @param minOccurs the minimum occurences
     * @param maxOccurs the maximum occurences
     */
    public CmsXmlCascadedContentDefinition(CmsXmlContentDefinition contentDefinition, String name, String minOccurs, String maxOccurs) {
        
        m_contentDefinition = contentDefinition;
        
        m_name = name;
        m_minOccurs = 1;
        if (CmsStringUtil.isNotEmpty(minOccurs)) {
            try {
                m_minOccurs = Integer.valueOf(minOccurs).intValue();
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        m_maxOccurs = 1;
        if (CmsStringUtil.isNotEmpty(maxOccurs)) {
            if (CmsXmlContentDefinition.XSD_ATTRIBUTE_VALUE_UNBOUNDED.equals(maxOccurs)) {
                m_maxOccurs = Integer.MAX_VALUE;
            } else {
                try {
                    m_maxOccurs = Integer.valueOf(maxOccurs).intValue();
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }        
    }

    /**
     * @see org.opencms.xml.types.I_CmsXmlSchemaType#createValue(org.dom4j.Element, java.lang.String, int)
     */
    public I_CmsXmlContentValue createValue(Element element, String name, int index) {

        // TODO: Auto-generated method stub
        return null;
    }

    /**
     * @see org.opencms.xml.types.I_CmsXmlContentValue#getStringValue(org.opencms.file.CmsObject, org.opencms.xml.I_CmsXmlDocument)
     */
    public String getStringValue(CmsObject cms, I_CmsXmlDocument document) throws CmsXmlException {

        // TODO: Auto-generated method stub
        return null;
    }

    /**
     * @see org.opencms.xml.types.I_CmsXmlContentValue#setStringValue(java.lang.String)
     */
    public void setStringValue(String value) throws CmsXmlException {

        // TODO: Auto-generated method stub
        
    }    
    
    /**
     * @see org.opencms.xml.types.I_CmsXmlSchemaType#getSchemaDefinition()
     */
    public String getSchemaDefinition() {

        throw new RuntimeException("Unable to get the schema definition of a cascaded XML content definition"); 
    }

    /**
     * @see org.opencms.xml.types.I_CmsXmlSchemaType#getTypeName()
     */
    public String getTypeName() {

        return m_contentDefinition.getTypeName();
    }

    /**
     * @see org.opencms.xml.types.I_CmsXmlSchemaType#newInstance(java.lang.String, java.lang.String, java.lang.String)
     */
    public I_CmsXmlSchemaType newInstance(String name, String minOccurs, String maxOccurs) {

        throw new RuntimeException("Unable to create a new instance of a cascaded XML content definition"); 
    }

    /**
     * @see org.opencms.xml.types.I_CmsXmlSchemaType#isSimpleType()
     */
    public boolean isSimpleType() {

        // this is a cascaded type
        return false;
    }
}
