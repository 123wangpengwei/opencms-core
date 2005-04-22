/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/list/CmsListDefaultAction.java,v $
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

import org.opencms.util.CmsStringUtil;
import org.opencms.workplace.CmsWorkplace;

/**
 * Implementation of a default action in a html list column.<p>
 * 
 * @author Michael Moossen (m.moossen@alkacon.com) 
 * @version $Revision: 1.1 $
 * @since 5.7.3
 */
public class CmsListDefaultAction extends CmsListDirectAction {

    /** Name of the column. */
    private String m_column;

    /**
     * Default Constructor.<p>
     * 
     * @param list the list 
     * @param id unique id
     * @param name the name
     * @param iconPath the path to the icon
     * @param helpText the help text
     * @param enabled if enabled or not
     * @param confirmationMessage the confirmation message
     */
    public CmsListDefaultAction(
        CmsHtmlList list,
        String id,
        String name,
        String iconPath,
        String helpText,
        boolean enabled,
        String confirmationMessage) {

        super(list, id, name, iconPath, helpText, enabled, confirmationMessage);
    }

    /**
     * @see org.opencms.workplace.list.CmsListDirectAction#buttonHtml(CmsWorkplace)
     */
    public String buttonHtml(CmsWorkplace wp) {

        String id = getId() + getItem().getId();
        String name = (getItem().get(m_column) != null) ? getItem().get(m_column).toString() : getName();
        String onClic = getList().getId()
            + "ListAction('"
            + getId()
            + "', '"
            + CmsStringUtil.escapeJavaScript(wp.resolveMacros(getConfirmationMessage()))
            + "', '"
            + CmsStringUtil.escapeJavaScript(getItem().getId())
            + "');";

        return A_CmsHtmlIconButton.defaultButtonHtml(id, name, getHelpText(), isEnabled(), getIconPath(), onClic);
    }

    /**
     * The id of the column to use.<p>
     * 
     * @param column the column id
     */
    public void setColumn(String column) {

        m_column = column;
    }

}