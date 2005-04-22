/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/list/CmsListOrderEnum.java,v $
 * Date   : $Date: 2005/04/22 08:38:52 $
 * Version: $Revision: 1.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2005 Alkacon Software (http://www.alkacon.com)
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

package org.opencms.workplace.list;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Wrapper class for
 * the different types of table cell horizontal alignments.<p>
 * 
 * The possibles values are:<br>
 * <ul>
 *   <li>AscendingOrder</li>
 *   <li>DescendingOrder</li>
 *   <li>NoneOrder</li>
 * </ul>
 * <p>
 * 
 * @author Michael Moossen (m.moossen@alkacon.com) 
 * @version $Revision: 1.1 $
 * @since 5.7.3
 */
public final class CmsListOrderEnum {

    /** Constant for ascending ordering.     */
    public static final CmsListOrderEnum AscendingOrder = new CmsListOrderEnum("asc");

    /** Constant for descending ordering.     */
    public static final CmsListOrderEnum DescendingOrder = new CmsListOrderEnum("des");

    /** Constant for none ordering. */
    public static final CmsListOrderEnum NoneOrder = new CmsListOrderEnum("none");

    /** Array constant for column sorting. */
    private static final CmsListOrderEnum[] C_VALUES = {AscendingOrder, DescendingOrder, NoneOrder};

    /** List of ordering constants.     */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(C_VALUES));

    /** Internal representation. */
    private final String m_order;

    /**
     * Private constructor.<p>
     * 
     * @param order internal representation
     */
    private CmsListOrderEnum(String order) {

        m_order = order;
    }

    /**
     * Parses an string into an element of this enumeration.<p>
     *
     * @param value the align to parse
     * 
     * @return the enumeration element
     */
    public static CmsListOrderEnum valueOf(String value) {

        Iterator iter = VALUES.iterator();
        while (iter.hasNext()) {
            CmsListOrderEnum target = (CmsListOrderEnum)iter.next();
            if (value == target.getOrder()) {
                return target;
            }
        }
        throw new IllegalArgumentException(Messages.get().key(
            Messages.ERR_LIST_ENUM_PARSE_2,
            new Integer(value),
            CmsListOrderEnum.class.getName()));
    }

    /**
     * Returns the order string.<p>
     * 
     * @return the order string
     */
    public String getOrder() {

        return m_order;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {

        return m_order;
    }

}