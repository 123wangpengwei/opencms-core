/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/xml/types/Attic/CmsXmlSimpleLinkValue.java,v $
 * Date   : $Date: 2004/11/30 17:20:31 $
 * Version: $Revision: 1.4 $
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

import org.opencms.xml.I_CmsXmlDocument;

import java.util.Locale;

import org.dom4j.Element;

/**
 * Describes the XML content type "OpenCmsSimpleLink".<p>
 *
 * @author Andreas Zahner (a.zahner@alkacon.com)
 * 
 * @version $Revision: 1.4 $
 * @since 5.5.3
 */
public class CmsXmlSimpleLinkValue extends A_CmsXmlValueTextBase {

    /** The name of this type as used in the XML schema. */
    public static final String C_TYPE_NAME = "OpenCmsSimpleLink";

    /** The schema definition String is located in a text for easier editing. */
    private static String m_schemaDefinition;

    /**
     * Creates a new, empty schema type descriptor of type "OpenCmsSimpleLink".<p>
     */
    public CmsXmlSimpleLinkValue() {

        // empty constructor is required for class registration
    }

    /**
     * Creates a new XML content value of type "OpenCmsSimpleLink".<p>
     * 
     * @param document the XML content instance this value belongs to
     * @param element the XML element that contains this value
     * @param locale the locale this value is created for
     */
    public CmsXmlSimpleLinkValue(I_CmsXmlDocument document, Element element, Locale locale) {

        super(document, element, locale);
    }

    /**
     * Creates a new schema type descriptor for the type "OpenCmsSimpleLink".<p>
     * 
     * @param name the name of the XML node containing the value according to the XML schema
     * @param minOccurs minimum number of occurences of this type according to the XML schema
     * @param maxOccurs maximum number of occurences of this type according to the XML schema
     */
    public CmsXmlSimpleLinkValue(String name, String minOccurs, String maxOccurs) {

        super(name, minOccurs, maxOccurs);
    }

    /**
     * @see org.opencms.xml.types.A_CmsXmlContentValue#createValue(I_CmsXmlDocument, org.dom4j.Element, Locale)
     */
    public I_CmsXmlContentValue createValue(I_CmsXmlDocument document, Element element, Locale locale) {

        return new CmsXmlSimpleLinkValue(document, element, locale);
    }

    /**
     * @see org.opencms.xml.types.I_CmsXmlSchemaType#getSchemaDefinition()
     */
    public String getSchemaDefinition() {

        // the schema definition is located in a separate file for easier editing
        if (m_schemaDefinition == null) {
            m_schemaDefinition = readSchemaDefinition("org/opencms/xml/types/XmlSimpleLinkValue.xsd");
        }
        return m_schemaDefinition;
    }

    /**
     * @see org.opencms.xml.types.A_CmsXmlContentValue#getTypeName()
     */
    public String getTypeName() {

        return C_TYPE_NAME;
    }

    /**
     * @see org.opencms.xml.types.A_CmsXmlContentValue#newInstance(java.lang.String, java.lang.String, java.lang.String)
     */
    public I_CmsXmlSchemaType newInstance(String name, String minOccurs, String maxOccurs) {

        return new CmsXmlSimpleLinkValue(name, minOccurs, maxOccurs);
    }
}