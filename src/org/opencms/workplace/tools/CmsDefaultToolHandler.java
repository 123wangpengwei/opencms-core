/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/tools/CmsDefaultToolHandler.java,v $
 * Date   : $Date: 2005/04/29 16:05:53 $
 * Version: $Revision: 1.6 $
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

package org.opencms.workplace.tools;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsProperty;
import org.opencms.i18n.CmsMessages;
import org.opencms.jsp.CmsJspNavBuilder;
import org.opencms.jsp.CmsJspNavElement;
import org.opencms.main.CmsException;
import org.opencms.main.I_CmsConstants;
import org.opencms.util.CmsStringUtil;


/**
 * Default admin tool handler.<p>
 * 
 * It takes the icon path from <code>{@link org.opencms.jsp.CmsJspNavElement#C_PROPERTY_NAVIMAGE}</code> property, 
 * or uses a default icon if undefined, the name is taken from the 
 * <code>{@link org.opencms.main.I_CmsConstants#C_PROPERTY_NAVTEXT}</code> property, 
 * or uses the <code>{@link org.opencms.main.I_CmsConstants#C_PROPERTY_TITLE}</code> property if undefined, 
 * or an default text, if still undefined. The help text is taken from the 
 * <code>{@link org.opencms.main.I_CmsConstants#C_PROPERTY_DESCRIPTION}</code> property or a
 * default text if undefined.<p> 
 * 
 * The group is taken from the 
 * <code>{@link org.opencms.jsp.CmsJspNavElement#C_PROPERTY_NAVINFO}</code> property,
 * the position from the <code>{@link org.opencms.main.I_CmsConstants#C_PROPERTY_NAVPOS}</code>
 * and the install path is given by the folder structure.<p>
 * 
 * For the icon path you can specify 2 paths separated by a <code>{@link A_CmsToolHandler#C_VALUE_SEPARATOR}</code>, 
 * the first one will be used as a group icon (32x32), and the second as an menu icon (16x16). <p>
 * 
 * @author Michael Moossen (m.moossen@alkacon.com) 
 * @version $Revision: 1.6 $
 * @since 5.7.3
 */
public class CmsDefaultToolHandler extends A_CmsToolHandler {

    /**
     * @see org.opencms.workplace.tools.I_CmsToolHandler#setup(org.opencms.file.CmsObject, java.lang.String)
     */
    public void setup(CmsObject cms, String resourcePath) {

        CmsJspNavElement navElem = CmsJspNavBuilder.getNavigationForResource(cms, resourcePath);

        String name = navElem.getNavText();
        if (CmsMessages.isUnknownKey(name)) {
            name = navElem.getTitle();
        }
        if (CmsStringUtil.isEmpty(name)) {
            name = "${key." + Messages.GUI_TOOLS_DEFAULT_NAME_0 + "}";
        }
        setName(name);

        String iconPath = navElem.getNavImage();
        if (CmsStringUtil.isEmpty(iconPath)) {
            iconPath = "${key." + Messages.GUI_TOOLS_DEFAULT_ICON_0 + "}";
        }
        String smallIconPath = null;
        if (iconPath.indexOf(C_VALUE_SEPARATOR) < 0) {
            smallIconPath = iconPath;
        } else {
            smallIconPath = iconPath.substring(iconPath.indexOf(C_VALUE_SEPARATOR) + 1);
            iconPath = iconPath.substring(0, iconPath.indexOf(C_VALUE_SEPARATOR));
        }
        setIconPath(iconPath);
        setSmallIconPath(smallIconPath);

        String helpText = navElem.getDescription();
        if (CmsStringUtil.isEmpty(helpText)) {
            helpText = "${key." + Messages.GUI_TOOLS_DEFAULT_HELP_0 + "}";
        }
        setHelpText(helpText);

        String group = navElem.getInfo();
        if (CmsStringUtil.isEmpty(group)) {
            group = "${key." + Messages.GUI_TOOLS_DEFAULT_GROUP_0 + "}";
        }

        setLink(resourcePath);

        String path = resourcePath;
        try {
            // make sure the res is a folder 
            cms.readFolder(resourcePath);

            // adjust the path
            if (path.endsWith(CmsToolManager.C_TOOLPATH_SEPARATOR)) {
                path = path.substring(0, path.lastIndexOf(CmsToolManager.C_TOOLPATH_SEPARATOR));
            }

            // set admin page as link
            setLink(CmsToolManager.C_VIEW_JSPPAGE_LOCATION);

            // try to use the folder def file as link
            CmsProperty prop = cms.readPropertyObject(path, I_CmsConstants.C_PROPERTY_DEFAULT_FILE, true);
            if (!prop.isNullProperty()) {
                String defFile = prop.getValue();
                if (defFile.startsWith(CmsToolManager.C_TOOLPATH_SEPARATOR)) {
                    // try to use this absolute link
                    cms.readResource(defFile);
                    setLink(defFile);
                } else {
                    // try to use this relative link
                    defFile = path + CmsToolManager.C_TOOLPATH_SEPARATOR + defFile;
                    cms.readResource(defFile);
                    setLink(defFile);
                }
            }

        } catch (CmsException e) {
            // noop
        }
        path = resourcePath.substring(CmsToolManager.C_ADMINTOOLS_ROOT_LOCATION.length(), resourcePath
            .lastIndexOf(CmsToolManager.C_TOOLPATH_SEPARATOR));

        // install point
        setPath(path);
        setGroup(group);
        setPosition(navElem.getNavPosition());
    }

}