/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/xml/I_CmsXmlDocument.java,v $
 * Date   : $Date: 2004/10/22 11:05:22 $
 * Version: $Revision: 1.3 $
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

package org.opencms.xml;

import org.opencms.file.CmsObject;
import org.opencms.xml.types.I_CmsXmlContentValue;

import java.util.List;
import java.util.Locale;

import org.xml.sax.EntityResolver;

/**
 * Describes the API to access the values of a XML content document.<p>
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.3 $
 * @since 5.5.0
 */
public interface I_CmsXmlDocument {

    /**
     * Adds the given locale to this XML document.
     * 
     * @param locale the locale to add
     * 
     * @throws CmsXmlException in case the locale already existed, or if something else goes wrong
     */
    void addLocale(Locale locale) throws CmsXmlException;

    /**
     * Returns the content definition object for this xml content object.<p>
     * 
     * @param resolver the XML entitiy resolver to use
     * 
     * @return the content definition object for this xml content object
     * 
     * @throws CmsXmlException if something goes wrong
     */
    CmsXmlContentDefinition getContentDefinition(EntityResolver resolver) throws CmsXmlException;

    /**
     * Returns the encoding used for this XML document.<p>
     * 
     * @return the encoding used for this XML document
     */
    String getEncoding();

    /**
     * Returns the index count of existing values for the given key name,
     * or <code>-1</code> if no such value exists.<p>
     * 
     * @param name the key to get the index count for
     * @param locale the locale to get the index count for
     * 
     * @return the index count for the given key name
     */
    int getIndexCount(String name, Locale locale);

    /**
     * Returns a List of all locales that have at last one element in 
     * this XML document.<p>
     * 
     * @return a List of all locales that have at last one element in this XML document
     */
    List getLocales();

    /**
     * Returns a List of all locales that have the given element set in this XML document.<p>
     * 
     * If no locale for the given element name is available, an empty list is returned.<p>
     * 
     * @param element the element to look up the locale List for
     * @return a List of all Locales that have the given element set in this XML document
     */
    List getLocales(String element);

    /**
     * Returns the first content value for the given key name as a String,
     * or <code>null</code> if no such value exists.<p>.<p>
     * 
     * @param cms an initialized instance of a CmsObject
     * @param name the key to get the content value for
     * @param locale the locale to get the content value for
     * 
     * @return the content value for the given key name
     * 
     * @throws CmsXmlException if something goes wrong
     */
    String getStringValue(CmsObject cms, String name, Locale locale) throws CmsXmlException;

    /**
     * Returns the content value for the given key name from the selected index as a String,
     * or <code>null</code> if no such value exists.<p>
     * 
     * @param cms an initialized instance of a CmsObject
     * @param name the key to get the content value for
     * @param locale the locale to get the content value for
     * @param index the index position to get the value from
     * 
     * @return the content value for the given key name
     * 
     * @throws CmsXmlException if something goes wrong
     */
    String getStringValue(CmsObject cms, String name, Locale locale, int index) throws CmsXmlException;

    /**
     * Returns the content value Object for the given key name,
     * or <code>null</code> if no such value exists.<p>.<p>
     * 
     * You can provide an index for the value by appending a numer in aquare brackets 
     * to the name parameter like this "Title[1]". 
     * If no index is provided, 0 is used for the index position.<p>
     * 
     * @param name the key to get the content value for
     * @param locale the locale to get the content value for
     * 
     * @return the content value for the given key name
     */
    I_CmsXmlContentValue getValue(String name, Locale locale);

    /**
     * Returns the content value Object for the given key name from 
     * the selected index, or <code>null</code> if no such value exists.<p>
     * 
     * @param name the key to get the content value for
     * @param locale the locale to get the content value for
     * @param index the index position to get the value from
     * 
     * @return the content value for the given key name
     */
    I_CmsXmlContentValue getValue(String name, Locale locale, int index);

    /**
     * Returns all content value Objects for the given key name in a List,
     * or <code>null</code> if no such value exists.<p>
     * 
     * @param name the key to get the content values for
     * @param locale the locale to get the content values for
     * 
     * @return the content value for the given key name
     */
    List getValues(String name, Locale locale);

    /**
     * Checks if the given locale exists in this XML document.<p>
     * 
     * @param locale the locale to check
     * 
     * @return true if the given locale exists in this XML document, false otherwise
     */
    boolean hasLocale(Locale locale);

    /**
     * Returns <code>true</code> if a value exists with the given key name, 
     * <code>false</code> otherwise.<p> 
     * 
     * You can provide an index for the value by appending a numer in aquare brackets 
     * to the name parameter like this "Title[1]". 
     * If no index is provided, 0 is used for the index position.<p>
     * 
     * @param name the key to check
     * @param locale the locale to check
     * 
     * @return true if a value exists with the given key name, false otherwise
     */
    boolean hasValue(String name, Locale locale);

    /**
     * Returns <code>true</code> if a value exists with the given key name at the selected index, 
     * <code>false</code> otherwise.<p> 
     * 
     * @param name the key to check
     * @param locale the locale to check
     * @param index the index position to check
     * 
     * @return true if a value exists with the given key name at the selected index, 
     *      false otherwise
     */
    boolean hasValue(String name, Locale locale, int index);

    /**
     * Returns <code>true</code> if a value exists with the given key name,
     * and that value is enabled, 
     * <code>false</code> otherwise.<p> 
     * 
     * You can provide an index for the value by appending a numer in aquare brackets 
     * to the name parameter like this "Title[1]". 
     * If no index is provided, 0 is used for the index position.<p>
     * 
     * @param name the key to check
     * @param locale the locale to check
     * 
     * @return true if a value exists with the given key name, and that value is enabled, 
     *      false otherwise
     */
    boolean isEnabled(String name, Locale locale);

    /**
     * Returns <code>true</code> if a value exists with the given key name at the selected index,
     * and that value is enabled, 
     * <code>false</code> otherwise.<p> 
     * 
     * @param name the key to check
     * @param locale the locale to check
     * @param index the index position to check
     * 
     * @return true if a value exists with the given key name at the selected index, 
     *      and that value is enabled, false otherwise
     */
    boolean isEnabled(String name, Locale locale, int index);

    /**
     * Removes the given locale from this XML document.
     * 
     * @param locale the locale to remove
     * 
     * @throws CmsXmlException in case the locale did not exist in the document, or if something else goes wrong
     */
    void removeLocale(Locale locale) throws CmsXmlException;    
}