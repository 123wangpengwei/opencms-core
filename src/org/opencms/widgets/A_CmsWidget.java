/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/widgets/A_CmsWidget.java,v $
 * Date   : $Date: 2005/05/19 16:35:47 $
 * Version: $Revision: 1.2 $
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

package org.opencms.widgets;

import org.opencms.file.CmsObject;
import org.opencms.main.OpenCms;

import java.util.Map;

/**
 * Base class for XML editor widgets.<p>
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.2 $
 * @since 5.5.0
 */
public abstract class A_CmsWidget implements I_CmsWidget {

    /** Postfix for melp message locale. */
    static final String HELP_POSTFIX = "help";

    /** Prefix for message locales. */
    static final String LABEL_PREFIX = "editor.label.";

    /** The configuration options of this widget. */
    protected String m_configuration;

    /**
     * Returns the localized help key for the provided widget parameter.<p>
     * @param param the widget parameter to return the localized help key for
     * 
     * @return the localized help key for the provided widget parameter
     */
    public static String getHelpKey(I_CmsWidgetParameter param) {

        // calculate the key
        StringBuffer result = new StringBuffer(64);
        result.append(LABEL_PREFIX);
        result.append(param.getKey());
        result.append('.');
        result.append(HELP_POSTFIX);

        return result.toString();
    }

    /**
     * Returns the localized label key for the provided widget parameter.<p>
     * @param param the widget parameter to return the localized label key for
     * 
     * @return the localized label key for the provided widget parameter
     */
    public static String getLabelKey(I_CmsWidgetParameter param) {

        // calculate the key
        StringBuffer result = new StringBuffer(64);
        result.append(LABEL_PREFIX);
        result.append(param.getKey());

        return result.toString();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {

        if (!(obj instanceof A_CmsWidget)) {
            return false;
        }

        // widgets are equal if they use the same class
        return getClass().getName().equals(obj.getClass().getName());
    }

    /**
     * @see org.opencms.widgets.I_CmsWidget#getDialogHtmlEnd(org.opencms.file.CmsObject, I_CmsWidgetDialog, I_CmsWidgetParameter)
     */
    public String getDialogHtmlEnd(CmsObject cms, I_CmsWidgetDialog widgetDialog, I_CmsWidgetParameter value) {

        return getHelpText(widgetDialog, value);
    }

    /**
     * @see org.opencms.widgets.I_CmsWidget#getDialogIncludes(org.opencms.file.CmsObject, I_CmsWidgetDialog)
     */
    public String getDialogIncludes(CmsObject cms, I_CmsWidgetDialog widgetDialog) {

        return "";
    }

    /**
     * @see org.opencms.widgets.I_CmsWidget#getDialogInitCall(org.opencms.file.CmsObject, I_CmsWidgetDialog)
     */
    public String getDialogInitCall(CmsObject cms, I_CmsWidgetDialog widgetDialog) {

        return "";
    }

    /**
     * @see org.opencms.widgets.I_CmsWidget#getDialogInitMethod(org.opencms.file.CmsObject, org.opencms.widgets.I_CmsWidgetDialog)
     */
    public String getDialogInitMethod(CmsObject cms, I_CmsWidgetDialog widgetDialog) {

        return "";
    }

    /**
     * @see org.opencms.widgets.I_CmsWidget#getHelpBubble(org.opencms.file.CmsObject, I_CmsWidgetDialog, I_CmsWidgetParameter)
     */
    public String getHelpBubble(CmsObject cms, I_CmsWidgetDialog widgetDialog, I_CmsWidgetParameter param) {

        StringBuffer result = new StringBuffer(128);
        String locKey = getHelpKey(param);
        String locValue = widgetDialog.getMessages().key(locKey, true);
        if (locValue == null) {
            // there was no help message found for this key, so return a spacer cell
            return widgetDialog.dialogHorizontalSpacer(16);
        } else {
            result.append("<td>");
            result.append("<img name=\"img");
            result.append(locKey);
            result.append("\" id=\"img");
            result.append(locKey);
            result.append("\" src=\"");
            result.append(OpenCms.getLinkManager().substituteLink(cms, "/system/workplace/resources/commons/help.gif"));
            result.append("\" border=\"0\" onmouseout=\"hideHelp('");
            result.append(locKey);
            result.append("');\" onmouseover=\"showHelp('");
            result.append(locKey);
            result.append("');\">");
            result.append("</td>");
            return result.toString();
        }
    }

    /**
     * @see org.opencms.widgets.I_CmsWidget#getHelpText(I_CmsWidgetDialog, I_CmsWidgetParameter)
     */
    public String getHelpText(I_CmsWidgetDialog widgetDialog, I_CmsWidgetParameter param) {

        StringBuffer result = new StringBuffer(128);
        // calculate the key
        String locKey = getHelpKey(param);
        String locValue = widgetDialog.getMessages().key(locKey, true);
        if (locValue == null) {
            // there was no help message found for this key, so return an empty string
            return "";
        } else {
            result.append("<div class=\"help\" name=\"help");
            result.append(locKey);
            result.append("\" id=\"help");
            result.append(locKey);
            result.append("\" onmouseout=\"hideHelp('");
            result.append(locKey);
            result.append("');\" onmouseover=\"showHelp('");
            result.append(locKey);
            result.append("');\">");
            result.append(locValue);
            result.append("</div>");
            return result.toString();
        }
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {

        return getClass().getName().hashCode();
    }

    /**
     * @see org.opencms.widgets.I_CmsWidget#setConfiguration(java.lang.String)
     */
    public void setConfiguration(String configuration) {

        m_configuration = configuration;
    }

    /**
     * @see org.opencms.widgets.I_CmsWidget#setEditorValue(org.opencms.file.CmsObject, java.util.Map, org.opencms.widgets.I_CmsWidgetDialog, org.opencms.widgets.I_CmsWidgetParameter)
     */
    public void setEditorValue(
        CmsObject cms,
        Map formParameters,
        I_CmsWidgetDialog widgetDialog,
        I_CmsWidgetParameter param) {

        String[] values = (String[])formParameters.get(param.getId());
        if ((values != null) && (values.length > 0)) {
            param.setStringValue(cms, values[0]);
        }
    }

    /**
     * Creates the tags to include external javascript files.<p>
     *  
     * @param fileName the absolute path to the javascript file
     * @return the tags to include external javascript files
     */
    protected String getJSIncludeFile(String fileName) {

        StringBuffer result = new StringBuffer(8);
        result.append("<script type=\"text/javascript\" src=\"");
        result.append(fileName);
        result.append("\"></script>");
        return result.toString();
    }
}