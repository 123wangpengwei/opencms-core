/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/list/CmsListColumnAlignEnum.java,v $
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
 *   <li>LeftAlign</li>
 *   <li>CenterAlign</li>
 *   <li>RightAlign</li>
 * </ul>
 * <p>
 * 
 * @author Michael Moossen (m.moossen@alkacon.com) 
 * @version $Revision: 1.1 $
 * @since 5.7.3
 */
public final class CmsListColumnAlignEnum {

    /** Constant for center alignment . */
    public static final CmsListColumnAlignEnum CenterAlign = new CmsListColumnAlignEnum("center");

    /** Constant for left alignment.  */
    public static final CmsListColumnAlignEnum LeftAlign = new CmsListColumnAlignEnum("left");

    /** Constant for right alignment.     */
    public static final CmsListColumnAlignEnum RightAlign = new CmsListColumnAlignEnum("right");

    /** Array constant for all available align types. */
    private static final CmsListColumnAlignEnum[] C_VALUES = {LeftAlign, CenterAlign, RightAlign};

    /** List of mode constants. */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(C_VALUES));

    /** Internal representation. */
    private final String m_align;

    /**
     * Private constructor.<p>
     * 
     * @param align html align value
     */
    private CmsListColumnAlignEnum(String align) {

        m_align = align;
    }

    /**
     * Parses an string into an element of this enumeration.<p>
     *
     * @param value the align to parse
     * 
     * @return the enumeration element
     */
    public static CmsListColumnAlignEnum valueOf(String value) {

        Iterator iter = VALUES.iterator();
        while (iter.hasNext()) {
            CmsListColumnAlignEnum target = (CmsListColumnAlignEnum)iter.next();
            if (value == target.getAlign()) {
                return target;
            }
        }
        throw new IllegalArgumentException(Messages.get().key(
            Messages.ERR_LIST_ENUM_PARSE_2,
            new Integer(value),
            CmsListColumnAlignEnum.class.getName()));
    }

    /**
     * Returns the align string.<p>
     * 
     * @return the align string
     */
    public String getAlign() {

        return m_align;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {

        return m_align;
    }
}