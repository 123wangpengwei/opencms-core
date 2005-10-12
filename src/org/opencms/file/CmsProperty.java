/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/file/CmsProperty.java,v $
 * Date   : $Date: 2005/10/12 10:00:06 $
 * Version: $Revision: 1.32 $
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

package org.opencms.file;

import org.opencms.main.CmsRuntimeException;
import org.opencms.util.CmsStringUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;

/**
 * Represents a property (meta-information) mapped to a VFS resource.<p>
 * 
 * A property is an object that contains three string values: a name, a property value which is mapped
 * to the structure record of a resource, and a property value which is mapped to the resource
 * record of a resource. A property object is valid if it has both values or just one value set.
 * Each property needs at least a name and one value set.<p>
 * 
 * A property value mapped to the structure record of a resource is significant for a single
 * resource (sibling). A property value mapped to the resource record of a resource is significant
 * for all siblings of a resource record. This is possible by getting the "compound value" 
 * (see {@link #getValue()}) of a property in case a property object has both values set. The compound 
 * value of a property object is the value mapped to the structure record, because it's structure 
 * value is more significant than it's resource value. This allows to set a property only one time 
 * on the resource record, and the property takes effect on all siblings of this resource record.<p>
 * 
 * The ID of the structure or resource record where a property value is mapped to is represented by 
 * the "PROPERTY_MAPPING_ID" table attribute in the database. The "PROPERTY_MAPPING_TYPE" table 
 * attribute (see {@link #STRUCTURE_RECORD_MAPPING} and {@link #RESOURCE_RECORD_MAPPING})
 * determines whether the value of the "PROPERTY_MAPPING_ID" attribute of the current row is
 * a structure or resource record ID.<p>
 * 
 * Property objects are written to the database using {@link org.opencms.file.CmsObject#writePropertyObject(String, CmsProperty)}
 * or {@link org.opencms.file.CmsObject#writePropertyObjects(String, List)}, no matter
 * whether you want to save a new (non-existing) property, update an existing property, or delete an
 * existing property. To delete a property you would write a property object with either the
 * structure and/or resource record values set to {@link #DELETE_VALUE} to indicate that a
 * property value should be deleted in the database. Set property values to null if they should
 * remain unchanged in the database when a property object is written. As for example you want to
 * update just the structure value of a property, you would set the structure value to the new string,
 * and the resource value to null (which is already the case by default).<p>
 * 
 * Use {@link #setAutoCreatePropertyDefinition(boolean)} to set a boolean flag whether a missing property
 * definition should be created implicitly for a resource type when a property is written to the database.
 * The default value for this flag is <code>false</code>. Thus, you receive a CmsException if you try
 * to write a property of a resource with a resource type which lacks a property definition for
 * this resource type. It is not a good style to set {@link #setAutoCreatePropertyDefinition(boolean)}
 * on true to make writing properties to the database work in any case, because then you will loose
 * control about which resource types support which property definitions.<p>
 * 
 * @author Thomas Weckert  
 * 
 * @version $Revision: 1.32 $
 * 
 * @since 6.0.0 
 */
public class CmsProperty implements Serializable, Cloneable, Comparable {

    /**
     * Signals that the resource property values of a resource
     * should be deleted using deleteAllProperties.<p>
     */
    public static final int DELETE_OPTION_DELETE_RESOURCE_VALUES = 3;

    /**
     * Signals that both the structure and resource property values of a resource
     * should be deleted using deleteAllProperties.<p>
     */
    public static final int DELETE_OPTION_DELETE_STRUCTURE_AND_RESOURCE_VALUES = 1;

    /**
     * Signals that the structure property values of a resource
     * should be deleted using deleteAllProperties.<p>
     */
    public static final int DELETE_OPTION_DELETE_STRUCTURE_VALUES = 2;

    /**
     * An empty string to decide that a property value should be deleted when this
     * property object is written to the database.<p>
     */
    public static final String DELETE_VALUE = new String("");

    /**
     * Value of the "mapping-type" database attribute to indicate that a property value is mapped
     * to a resource record.<p>
     */
    public static final int RESOURCE_RECORD_MAPPING = 2;

    /**
     * Value of the "mapping-type" database attribute to indicate that a property value is mapped
     * to a structure record.<p>
     */
    public static final int STRUCTURE_RECORD_MAPPING = 1;

    /** Key used for a individual (structure) property value. */
    public static final String TYPE_INDIVIDUAL = "individual";

    /** Key used for a shared (resource) property value. */
    public static final String TYPE_SHARED = "shared";

    /** The delimiter value for separating values in a list, per default this is the <code>|</code> char. */
    public static final char VALUE_LIST_DELIMITER = '|';

    /** The null property object to be used in caches if a property is not found. */
    private static final CmsProperty NULL_PROPERTY = new CmsProperty();

    /** Serial version UID required for safe serialization. */
    private static final long serialVersionUID = 93613508924212782L;

    /**
     * Boolean flag to decide if the property definition for this property should be created 
     * implicitly on any write operation if doesn't exist already.<p>
     */
    private boolean m_autoCreatePropertyDefinition;

    /** Indicates if the property is frozen (required for <code>{@link #NULL_PROPERTY}</code>). */
    private boolean m_frozen;

    /** The name of this property. */
    private String m_name;

    /** The value of this property attached to the structure record. */
    private String m_resourceValue;

    /** The (optional) value list of this property attached to the structure record. */
    private List m_resourceValueList;

    /** The value of this property attached to the resource record. */
    private String m_structureValue;

    /** The (optional) value list of this property attached to the resource record. */
    private List m_structureValueList;

    /**
     * Creates a new CmsProperty object.<p>
     * 
     * The structure and resource property values are initialized to null. The structure and
     * resource IDs are initialized to {@link org.opencms.util.CmsUUID#getNullUUID()}.<p>
     */
    public CmsProperty() {

        // noting to do, all values will be initialized with "null" or "false" by default
    }

    /**
     * Creates a new CmsProperty object using the provided values.<p>
     *
     * If the property definition does not exist for the resource type it
     * is automatically created when this propery is written.
     * 
     * @param name the name of the property definition
     * @param structureValue the value to write as structure property
     * @param resourceValue the value to write as resource property 
     */
    public CmsProperty(String name, String structureValue, String resourceValue) {

        this(name, structureValue, resourceValue, true);
    }

    /**
     * Creates a new CmsProperty object using the provided values.<p>
     * 
     * @param name the name of the property definition
     * @param structureValue the value to write as structure property
     * @param resourceValue the value to write as resource property 
     * @param autoCreatePropertyDefinition true, if the property definition for this property should be created mplicitly on any write operation if doesn't exist already
     */
    public CmsProperty(String name, String structureValue, String resourceValue, boolean autoCreatePropertyDefinition) {

        m_name = name;
        m_structureValue = structureValue;
        m_resourceValue = resourceValue;
        m_autoCreatePropertyDefinition = autoCreatePropertyDefinition;
    }

    /**
     * Static initializer required for freezing the <code>{@link #NULL_PROPERTY}</code>.<p>
     */
    static {

        NULL_PROPERTY.m_frozen = true;
    }

    /**
     * Searches in a list for the first occurence of a Cms property object with the given name.<p> 
     *
     * To check if the "null property" has been returned if a property was 
     * not found, use {@link #isNullProperty()} on the result.<p> 
     *
     * @param name a property name
     * @param list a list of Cms property objects
     * @return the index of the first occurrence of the name in they specified list, or the "null-property" if the name is not found
     */
    public static final CmsProperty get(String name, List list) {

        CmsProperty property = null;

        // choose the fastest method to traverse the list
        if (list instanceof RandomAccess) {
            for (int i = 0, n = list.size(); i < n; i++) {
                property = (CmsProperty)list.get(i);
                if (property.m_name.equals(name)) {
                    return property;
                }
            }
        } else {
            Iterator i = list.iterator();
            while (i.hasNext()) {
                property = (CmsProperty)i.next();
                if (property.m_name.equals(name)) {
                    return property;
                }
            }
        }

        return NULL_PROPERTY;
    }

    /**
     * Returns the null property object.<p>
     * 
     * @return the null property object
     */
    public static final CmsProperty getNullProperty() {

        return NULL_PROPERTY;
    }

    /**
     * Calls <code>{@link #setAutoCreatePropertyDefinition(boolean)}</code> for each
     * property object in the given List with the given <code>value</code> parameter.<p>
     * 
     * This method will modify the objects in the input list directly.<p>
     * 
     * @param list a list of property objects
     * @param value boolean value
     * 
     * @return the modified list of properties
     * 
     * @see #setAutoCreatePropertyDefinition(boolean)
     */
    public static final List setAutoCreatePropertyDefinitions(List list, boolean value) {

        CmsProperty property;

        // choose the fastest method to traverse the list
        if (list instanceof RandomAccess) {
            for (int i = 0, n = list.size(); i < n; i++) {
                property = (CmsProperty)list.get(i);
                property.m_autoCreatePropertyDefinition = value;
            }
        } else {
            Iterator i = list.iterator();
            while (i.hasNext()) {
                property = (CmsProperty)i.next();
                property.m_autoCreatePropertyDefinition = value;
            }
        }

        return list;
    }

    /**
     * Calls <code>{@link #setFrozen(boolean)}</code> for each
     * property object in the given List if it is not already frozen.<p>
     * 
     * This method will modify the objects in the input list directly.<p>
     * 
     * @param list a list of property objects
     * 
     * @return the modified list of properties
     * 
     * @see #setFrozen(boolean)
     */
    public static final List setFrozen(List list) {

        CmsProperty property;

        // choose the fastest method to traverse the list
        if (list instanceof RandomAccess) {
            for (int i = 0, n = list.size(); i < n; i++) {
                property = (CmsProperty)list.get(i);
                if (!property.isFrozen()) {
                    property.setFrozen(true);
                }
            }
        } else {
            Iterator i = list.iterator();
            while (i.hasNext()) {
                property = (CmsProperty)i.next();
                if (!property.isFrozen()) {
                    property.setFrozen(true);
                }
            }
        }

        return list;
    }

    /**
     * Transforms a map with compound (String) values keyed by property names into a list of 
     * CmsProperty objects with structure values.<p>
     * 
     * This method is to prevent issues with backward incompatibilities in older code.
     * Use this method with caution, it might be removed without with being deprecated
     * before.<p>
     * 
     * @param map a map with compound (String) values keyed by property names
     * @return a list of CmsProperty objects
     */
    public static List toList(Map map) {

        String name = null;
        String value = null;
        CmsProperty property = null;
        List properties = null;
        Object[] names = null;

        if (map == null || map.size() == 0) {
            return Collections.EMPTY_LIST;
        }

        properties = new ArrayList(map.size());
        names = map.keySet().toArray();
        for (int i = 0; i < names.length; i++) {
            name = (String)names[i];
            value = (String)map.get(name);

            property = new CmsProperty();
            property.m_name = name;
            property.m_structureValue = value;
            properties.add(property);
        }

        return properties;
    }

    /**
     * Transforms a list of CmsProperty objects with structure and resource values into a map with
     * compound (String) values keyed by property names.<p>
     *
     * This method is to prevent issues with backward incompatibilities in older code.
     * Use this method with caution, it might be removed without with being deprecated
     * before.<p>
     * 
     * @param list a list of CmsProperty objects
     * @return a map with compound (String) values keyed by property names
     */
    public static Map toMap(List list) {

        Map result = null;
        String name = null;
        String value = null;
        CmsProperty property = null;

        if (list == null || list.size() == 0) {
            return Collections.EMPTY_MAP;
        }

        result = new HashMap();

        // choose the fastest method to traverse the list
        if (list instanceof RandomAccess) {
            for (int i = 0, n = list.size(); i < n; i++) {
                property = (CmsProperty)list.get(i);
                name = property.m_name;
                value = property.getValue();
                result.put(name, value);
            }
        } else {
            Iterator i = list.iterator();
            while (i.hasNext()) {
                property = (CmsProperty)i.next();
                name = property.m_name;
                value = property.getValue();
                result.put(name, value);
            }
        }

        return result;
    }

    /**
     * Checks if the property definition for this property should be 
     * created implicitly on any write operation if doesn't exist already.<p>
     * 
     * @return true, if the property definition for this property should be created implicitly on any write operation
     */
    public boolean autoCreatePropertyDefinition() {

        return m_autoCreatePropertyDefinition;
    }

    /**
     * Creates a clone of this property.<p>
     *  
     * @return a clone of this property
     * 
     * @see #cloneAsProperty()
     */
    public Object clone() {

        return cloneAsProperty();
    }

    /**
     * Creates a clone of this property that already is of type <code>{@link CmsProperty}</code>.<p>
     * 
     * The cloned property will not be frozen.<p>
     * 
     * @return a clone of this property that already is of type <code>{@link CmsProperty}</code>
     */
    public CmsProperty cloneAsProperty() {

        if (this == NULL_PROPERTY) {
            // null property must never be cloned
            return NULL_PROPERTY;
        }
        
        CmsProperty clone = new CmsProperty();

        clone.m_name = m_name;
        clone.m_structureValue = m_structureValue;
        clone.m_structureValueList = m_structureValueList;
        clone.m_resourceValue = m_resourceValue;
        clone.m_resourceValueList = m_resourceValueList;
        clone.m_autoCreatePropertyDefinition = m_autoCreatePropertyDefinition;
        // the value for m_frozen does not need to be set as it is false by default

        return clone;
    }

    /**
     * Compares this property to another Object.<p>
     * 
     * @param obj the other object to be compared
     * @return if the argument is a property object, returns zero if the name of the argument is equal to the name of this property object, 
     *      a value less than zero if the name of this property is lexicographically less than the name of the argument, 
     *      or a value greater than zero if the name of this property is lexicographically greater than the name of the argument 
     */
    public int compareTo(Object obj) {

        if (obj == this) {
            return 0;
        }
        if (obj instanceof CmsProperty) {
            return m_name.compareTo(((CmsProperty)obj).m_name);
        }
        return 0;
    }

    /**
     * Checks if the resource value of this property should be deleted when this
     * property object is written to the database.<p>
     * 
     * @return true, if the resource value of this property should be deleted
     * @see CmsProperty#DELETE_VALUE
     */
    public boolean deleteResourceValue() {

        checkFrozen();
        return (m_resourceValue == DELETE_VALUE) || (m_resourceValue != null && m_resourceValue.length() == 0);
    }

    /**
     * Checks if the structure value of this property should be deleted when this
     * property object is written to the database.<p>
     * 
     * @return true, if the structure value of this property should be deleted
     * @see CmsProperty#DELETE_VALUE
     */
    public boolean deleteStructureValue() {

        checkFrozen();
        return (m_structureValue == DELETE_VALUE) || (m_structureValue != null && m_structureValue.length() == 0);
    }

    /**
     * Tests if a specified object is equal to this CmsProperty object.<p>
     * 
     * Two property objecs are equal if their names are equal.<p>
     * 
     * @param obj another object
     * @return true, if the specified object is equal to this CmsProperty object
     */
    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }
        if (obj instanceof CmsProperty) {
            return ((CmsProperty)obj).m_name.equals(m_name);
        }
        return false;
    }

    /**
     * Returns the name of this property.<p>
     * 
     * @return name of this property
     * 
     * @deprecated use {@link #getName()} instead
     */
    public String getKey() {

        return getName();
    }

    /**
     * Returns the name of this property.<p>
     * 
     * @return the name of this property
     */
    public String getName() {

        return m_name;
    }

    /**
     * Returns the value of this property attached to the resource record.<p>
     * 
     * @return the value of this property attached to the resource record
     */
    public String getResourceValue() {

        return m_resourceValue;
    }

    /**
     * Returns the value of this property attached to the resource record, split as a list.<p>
     * 
     * This list is build form the resource value, which is split into separate values
     * using the <code>|</code> char as delimiter. If the delimiter is not found,
     * then the list will contain one entry which is equal to <code>{@link #getResourceValue()}</code>.<p>
     * 
     * @return the value of this property attached to the resource record, split as a (unmodifiable) list of Strings
     */
    public List getResourceValueList() {

        if ((m_resourceValueList == null) && (m_resourceValue != null)) {
            // use lazy initializing of the list
            m_resourceValueList = createListFromValue(m_resourceValue);
            m_resourceValueList = Collections.unmodifiableList(m_resourceValueList);
        }
        return m_resourceValueList;
    }

    /**
     * Returns the value of this property attached to the structure record.<p>
     * 
     * @return the value of this property attached to the structure record
     */
    public String getStructureValue() {

        return m_structureValue;
    }

    /**
     * Returns the value of this property attached to the structure record, split as a list.<p>
     * 
     * This list is build form the structure value, which is split into separate values
     * using the <code>|</code> char as delimiter. If the delimiter is not found,
     * then the list will contain one entry which is equal to <code>{@link #getStructureValue()}</code>.<p>
     * 
     * @return the value of this property attached to the structure record, split as a (unmodifiable) list of Strings
     */
    public List getStructureValueList() {

        if ((m_structureValueList == null) && (m_structureValue != null)) {
            // use lazy initializing of the list
            m_structureValueList = createListFromValue(m_structureValue);
            m_structureValueList = Collections.unmodifiableList(m_structureValueList);
        }
        return m_structureValueList;
    }

    /**
     * Returns the compound value of this property.<p>
     * 
     * The value returned is the value of {@link #getStructureValue()}, if it is not <code>null</code>.
     * Otherwise the value if {@link #getResourceValue()} is returned (which may also be <code>null</code>).<p>
     * 
     * @return the compound value of this property
     */
    public String getValue() {

        return (m_structureValue != null) ? m_structureValue : m_resourceValue;
    }

    /**
     * Returns the compound value of this property, or a specified default value,
     * if both the structure and resource values are null.<p>
     * 
     * In other words, this method returns the defaultValue if this property object 
     * is the null property (see {@link CmsProperty#getNullProperty()}).<p>
     * 
     * @param defaultValue a default value which is returned if both the structure and resource values are <code>null</code>
     * 
     * @return the compound value of this property, or the default value
     */
    public String getValue(String defaultValue) {

        if (this == CmsProperty.NULL_PROPERTY) {
            // return the default value if this property is the null property
            return defaultValue;
        }

        // somebody might have set both values to null manually
        // on a property object different from the null property...
        return (m_structureValue != null) ? m_structureValue : ((m_resourceValue != null) ? m_resourceValue
        : defaultValue);
    }

    /**
     * Returns the compound value of this property, split as a list.<p>
     * 
     * This list is build form the used value, which is split into separate values
     * using the <code>|</code> char as delimiter. If the delimiter is not found,
     * then the list will contain one entry.<p>
     * 
     * The value returned is the value of {@link #getStructureValueList()}, if it is not <code>null</code>.
     * Otherwise the value if {@link #getResourceValueList()} is returned (which may also be <code>null</code>).<p>
     * 
     * @return the compound value of this property, split as a (unmodifiable) list of Strings
     */
    public List getValueList() {

        return (m_structureValue != null) ? getStructureValueList() : getResourceValueList();
    }

    /**
     * Returns the compound value of this property, split as a list, or a specified default value list,
     * if both the structure and resource values are null.<p>
     * 
     * In other words, this method returns the defaultValue if this property object 
     * is the null property (see {@link CmsProperty#getNullProperty()}).<p>
     * 
     * @param defaultValue a default value list which is returned if both the structure and resource values are <code>null</code>
     * 
     * @return the compound value of this property, split as a (unmodifiable) list of Strings
     */
    public List getValueList(List defaultValue) {

        if (this == CmsProperty.NULL_PROPERTY) {
            // return the default value if this property is the null property
            return defaultValue;
        }

        // somebody might have set both values to null manually
        // on a property object different from the null property...
        return (m_structureValue != null) ? getStructureValueList()
        : ((m_resourceValue != null) ? getResourceValueList() : defaultValue);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {

        StringBuffer strBuf = new StringBuffer();

        strBuf.append(m_name);
        strBuf.append("_");
        strBuf.append(m_structureValue);
        strBuf.append("_");
        strBuf.append(m_resourceValue);

        return strBuf.toString().hashCode();
    }

    /**
     * Returns <code>true</code> if this property is frozen, that is read only.<p>
     *
     * @return <code>true</code> if this property is frozen, that is read only
     */
    public boolean isFrozen() {

        return m_frozen;
    }

    /**
     * Tests if a given CmsProperty is identical to this CmsProperty object.<p>
     * 
     * The property object are identical if their name, structure and 
     * resource values are all equals.<p>
     * 
     * @param property another property object
     * @return true, if the specified object is equal to this CmsProperty object
     */
    public boolean isIdentical(CmsProperty property) {

        boolean isEqual;

        // compare the name
        if (m_name == null) {
            isEqual = (property.getName() == null);
        } else {
            isEqual = m_name.equals(property.getName());
        }

        // compare the structure value
        if (m_structureValue == null) {
            isEqual &= (property.getStructureValue() == null);
        } else {
            isEqual &= m_structureValue.equals(property.getStructureValue());
        }

        // compare the resource value
        if (m_resourceValue == null) {
            isEqual &= (property.getResourceValue() == null);
        } else {
            isEqual &= m_resourceValue.equals(property.getResourceValue());
        }

        return isEqual;
    }

    /**
     * Checks if this property object is the null property object.<p>
     * 
     * @return true if this property object is the null property object
     */
    public boolean isNullProperty() {

        return this == NULL_PROPERTY;
    }

    /**
     * Sets the boolean flag to decide if the property definition for this property should be 
     * created implicitly on any write operation if doesn't exist already.<p>
     * 
     * @param value true, if the property definition for this property should be created implicitly on any write operation
     */
    public void setAutoCreatePropertyDefinition(boolean value) {

        checkFrozen();
        m_autoCreatePropertyDefinition = value;
    }

    /**
     * Sets the frozen state of the property, if set to <code>true</code> then this property is read only.<p>
     *
     * If the property is already frozen, then setting the frozen state to <code>true</code> again is allowed, 
     * but seeting the value to <code>false</code> causes a <code>{@link CmsRuntimeException}</code>.<p>
     *
     * @param frozen the frozen state to set
     */
    public void setFrozen(boolean frozen) {

        if (!frozen) {
            checkFrozen();
        }
        m_frozen = frozen;
    }

    /**
     * Sets the name of this property.<p>
     * 
     * @param name the name of this property
     * 
     * @deprecated use {@link #setName(String)} instead
     */
    public void setKey(String name) {

        checkFrozen();
        setName(name);
    }

    /**
     * Sets the name of this property.<p>
     * 
     * @param name the name to set
     */
    public void setName(String name) {

        checkFrozen();
        m_name = name;
    }

    /**
     * Sets the value of this property attached to the resource record.<p>
     * 
     * @param resourceValue the value of this property attached to the resource record
     */
    public void setResourceValue(String resourceValue) {

        checkFrozen();
        m_resourceValue = resourceValue;
        m_resourceValueList = null;
    }

    /**
     * Sets the value of this property attached to the resource record form the given list of Strings.<p>
     * 
     * The value will be created form the individual values of the given list, which are appended
     * using the <code>|</code> char as delimiter.<p>
     * 
     * @param valueList the list of value (Strings) to attach to the resource record
     */
    public void setResourceValueList(List valueList) {

        checkFrozen();
        if (valueList != null) {
            m_resourceValueList = new ArrayList(valueList);
            m_resourceValueList = Collections.unmodifiableList(m_resourceValueList);
            m_resourceValue = createValueFromList(m_resourceValueList);
        } else {
            m_resourceValueList = null;
            m_resourceValue = null;
        }
    }

    /**
     * Sets the value of this property attached to the structure record.<p>
     * 
     * @param structureValue the value of this property attached to the structure record
     */
    public void setStructureValue(String structureValue) {

        checkFrozen();
        m_structureValue = structureValue;
        m_structureValueList = null;
    }

    /**
     * Sets the value of this property attached to the structure record form the given list of Strings.<p>
     * 
     * The value will be created form the individual values of the given list, which are appended
     * using the <code>|</code> char as delimiter.<p>
     * 
     * @param valueList the list of value (Strings) to attach to the structure record
     */
    public void setStructureValueList(List valueList) {

        checkFrozen();
        if (valueList != null) {
            m_structureValueList = new ArrayList(valueList);
            m_structureValueList = Collections.unmodifiableList(m_structureValueList);
            m_structureValue = createValueFromList(m_structureValueList);
        } else {
            m_structureValueList = null;
            m_structureValue = null;
        }
    }

    /**
     * Sets the value of this property as either shared or 
     * individual value.<p>
     * 
     * If the given type equals {@link CmsProperty#TYPE_SHARED} then
     * the value is set as a shared (resource) value, otherwise it
     * is set as individual (structure) value.<p>
     * 
     * @param value the value to set
     * @param type the value type to set
     */
    public void setValue(String value, String type) {

        checkFrozen();
        setAutoCreatePropertyDefinition(true);
        if (TYPE_SHARED.equalsIgnoreCase(type)) {
            // set the provided value as shared (resource) value
            setResourceValue(value);
        } else {
            // set the provided value as individual (structure) value
            setStructureValue(value);
        }
    }

    /**
     * Returns a string representation of this property object.<p>
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {

        StringBuffer strBuf = new StringBuffer();

        strBuf.append("[").append(getClass().getName()).append(": ");
        strBuf.append("name: '").append(m_name).append("'");
        strBuf.append(", value: '").append(getValue()).append("'");
        strBuf.append(", structure value: '").append(m_structureValue).append("'");
        strBuf.append(", resource value: '").append(m_resourceValue).append("'");
        strBuf.append(", frozen: ").append(m_frozen);
        strBuf.append("]");

        return strBuf.toString();
    }

    /**
     * Checks if this property is frozen, that is read only.<p> 
     */
    private void checkFrozen() {

        if (m_frozen) {
            throw new CmsRuntimeException(Messages.get().container(Messages.ERR_PROPERTY_FROZEN_1, toString()));
        }
    }

    /**
     * Returns the list value representation for the given String.<p>
     * 
     * The given value is split along the <code>|</code> char.<p>
     * 
     * @param value the value list to create the list representation for
     * 
     * @return the list value representation for the given String
     */
    private List createListFromValue(String value) {

        if (value == null) {
            return null;
        }
        return CmsStringUtil.splitAsList(value, VALUE_LIST_DELIMITER);
    }

    /**
     * Returns the single String value representation for the given value list.<p>
     * 
     * @param valueList the value list to create the single String value for
     * 
     * @return the single String value representation for the given value list
     */
    private String createValueFromList(List valueList) {

        if (valueList == null) {
            return null;
        }
        StringBuffer result = new StringBuffer(valueList.size() * 32);
        Iterator i = valueList.iterator();
        while (i.hasNext()) {
            result.append(i.next().toString());
            if (i.hasNext()) {
                result.append(VALUE_LIST_DELIMITER);
            }
        }
        return result.toString();
    }
}