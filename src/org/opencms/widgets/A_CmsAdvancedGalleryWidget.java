/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/widgets/Attic/A_CmsAdvancedGalleryWidget.java,v $
 * Date   : $Date: 2010/02/26 10:38:31 $
 * Version: $Revision: 1.9 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2009 Alkacon Software (http://www.alkacon.com)
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
import org.opencms.i18n.CmsEncoder;
import org.opencms.json.JSONArray;
import org.opencms.util.CmsStringUtil;
import org.opencms.workplace.CmsWorkplace;
import org.opencms.workplace.galleries.CmsGallerySearchServer;
import org.opencms.xml.types.I_CmsXmlContentValue;

/**
 * Class for all advanced gallery widgets.<p>
 * 
 * Possible configuration is described in the configuration class:
 * 
 * @see org.opencms.widgets.CmsAdvancedGalleryWidgetConfiguration 
 *
 * @author Polina Smagina
 * 
 * @version $Revision: 1.9 $ 
 * 
 * @since 
 */
public abstract class A_CmsAdvancedGalleryWidget extends A_CmsWidget {

    /**
     * Creates a new advanced gallery widget.<p>
     */
    public A_CmsAdvancedGalleryWidget() {

        // empty constructor is required for class registration
        this("");
    }

    /**
     * Creates a new advanced gallery widget with the given configuration.<p>
     * 
     * @param configuration the configuration to use
     */
    public A_CmsAdvancedGalleryWidget(String configuration) {

        super(configuration);

    }

    /**
     * @see org.opencms.widgets.I_CmsWidget#getDialogIncludes(org.opencms.file.CmsObject, org.opencms.widgets.I_CmsWidgetDialog)
     */
    @Override
    public String getDialogIncludes(CmsObject cms, I_CmsWidgetDialog widgetDialog) {

        StringBuffer result = new StringBuffer(256);
        // import the JavaScript for the gallery widget
        result.append(getJSIncludeFile(CmsWorkplace.getSkinUri()
            + "components/widgets/"
            + getNameLower()
            + "advancedgallery.js"));
        return result.toString();
    }

    /**
     * @see org.opencms.widgets.I_CmsWidget#getDialogInitCall(org.opencms.file.CmsObject, org.opencms.widgets.I_CmsWidgetDialog)
     */
    @Override
    public String getDialogInitCall(CmsObject cms, I_CmsWidgetDialog widgetDialog) {

        return "\tinit" + getNameUpper() + "AdvancedGallery();\n";
    }

    /**
     * @see org.opencms.widgets.I_CmsWidget#getDialogInitMethod(org.opencms.file.CmsObject, org.opencms.widgets.I_CmsWidgetDialog)
     */
    @Override
    public String getDialogInitMethod(CmsObject cms, I_CmsWidgetDialog widgetDialog) {

        StringBuffer result = new StringBuffer(16);
        result.append("function init");
        result.append(getNameUpper());
        result.append("AdvancedGallery() {\n");
        result.append("\t");
        result.append(getNameLower());
        result.append("AdvancedGalleryPath = '");
        // path to advanced gallery
        result.append(CmsGallerySearchServer.ADVANCED_GALLERY_PATH);
        result.append("';\n");
        result.append("}\n");
        return result.toString();
    }

    /**
     * @see org.opencms.widgets.I_CmsWidget#getDialogWidget(org.opencms.file.CmsObject, org.opencms.widgets.I_CmsWidgetDialog, org.opencms.widgets.I_CmsWidgetParameter)
     */
    public String getDialogWidget(CmsObject cms, I_CmsWidgetDialog widgetDialog, I_CmsWidgetParameter param) {

        String id = param.getId();
        long idHash = id.hashCode();
        if (idHash < 0) {
            // negative hash codes will not work as JS variable names, so convert them
            idHash = -idHash;
            // add 2^32 to the value to ensure that it is unique
            idHash += 4294967296L;
        }

        // reads the configuration String for this widget
        CmsAdvancedGalleryWidgetConfiguration configuration = new CmsAdvancedGalleryWidgetConfiguration(
            cms,
            widgetDialog,
            param,
            getConfiguration());

        StringBuffer result = new StringBuffer(128);
        result.append("<td class=\"xmlTd\">");
        result.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tr><td>");
        result.append("<input class=\"xmlInputMedium");
        if (param.hasError()) {
            result.append(" xmlInputError");
        }
        result.append("\" value=\"");
        String value = param.getStringValue(cms);
        result.append(value);
        result.append("\" name=\"");
        result.append(id);
        result.append("\" id=\"");
        result.append(id);
        result.append("\" onkeyup=\"checkPreview('");
        result.append(id);
        result.append("');\"></td>");
        result.append(widgetDialog.dialogHorizontalSpacer(10));
        result.append("<td><table class=\"editorbuttonbackground\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tr>");

        result.append(widgetDialog.button(
            "javascript:open"
                + getNameUpper()
                + "AdvancedGallery('"
                + CmsGallerySearchServer.GalleryMode.widget.name()
                + "',  '"
                + id
                + "',  '"
                + idHash
                + "');return false;",
            null,
            configuration.getButtonPrefix() + "gallery",
            Messages.getButtonName(getNameLower()),
            widgetDialog.getButtonStyle()));
        // create preview button
        String previewClass = "hide";
        if (showPreview(value)) {
            // show button if preview is enabled
            previewClass = "show";
        }
        result.append("<td class=\"");
        result.append(previewClass);
        result.append("\" id=\"preview");
        result.append(id);
        result.append("\">");
        result.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tr>");

        result.append(widgetDialog.button(
            "javascript:preview" + getNameUpper() + "('" + id + "');return false;",
            null,
            "preview.png",
            Messages.GUI_BUTTON_PREVIEW_0,
            widgetDialog.getButtonStyle()));
        result.append("</tr></table>");

        result.append("</td></tr></table>");

        result.append("</td>");
        result.append("</tr></table>");

        result.append("</td>");

        // set the gallery configuration parameter
        result.append("\n<script type=\"text/javascript\">");

        String locale = "";
        if (param instanceof I_CmsXmlContentValue) {
            I_CmsXmlContentValue xmlContentValue = (I_CmsXmlContentValue)param;
            locale = xmlContentValue.getLocale().getLanguage();
        } else {
            locale = widgetDialog.getLocale().getLanguage();
        }

        // locale for the galleries
        result.append("\nvar locale").append(idHash).append(" = \"").append(locale).append("\";");

        // resource types
        result.append("\nvar resourceTypes").append(idHash).append(" = ").append(configuration.getResourceTypes()).append(
            ";");
        // tabs to be displayed
        result.append("\nvar galleryTabs").append(idHash).append(" = ").append(configuration.getTabs()).append(";");

        // set start folder for gallery and if it opens galleries or categories
        if ((configuration.getStartUpFolders() != null) && (configuration.getStartUpFolders().length() != 0)) {
            result.append("\nvar startupFolders").append(idHash).append(" = ").append(configuration.getStartUpFolders()).append(
                ";");
            result.append("\nvar startupFolder").append(idHash).append(" = null;");
            result.append("\nvar startupType").append(idHash).append(" = \"").append(configuration.getType()).append(
                "\";");
        } else if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(configuration.getStartup())) {
            result.append("\nvar startupFolder").append(idHash).append(" = \"").append(configuration.getStartup()).append(
                "\";");
            result.append("\nvar startupFolders").append(idHash).append(" = null;");
            result.append("\nvar startupType").append(idHash).append(" = \"").append(configuration.getType()).append(
                "\";");

        } else {
            result.append("\nvar startupFolder").append(idHash).append(" = null;");
            result.append("\nvar startupFolders").append(idHash).append(" = null;");
            result.append("\nvar startupType").append(idHash).append(" = null;");
        }
        if (configuration.getStartupTabId() != null) {
            result.append("\nvar startupTabId").append(idHash).append(" = \"").append(configuration.getStartupTabId()).append(
                "\";");
        } else {
            result.append("\nvar startupTabId").append(idHash).append(" = \"").append(
                CmsGallerySearchServer.TabId.cms_tab_results).append("\";");
        }

        // set the configuration parameter for imagegallery
        if (configuration.isImagegallery()) {
            if (configuration.isShowFormat()) {
                // create hidden field to store the matching image format value
                JSONArray formatsJson = new JSONArray(configuration.getFormatValues());
                result.append("\nvar imgFmts").append(idHash).append(" = ").append(formatsJson).append(";");
                result.append("\nvar imgFmtNames").append(idHash).append(" = \"").append(
                    CmsEncoder.escape(configuration.getSelectFormatString(), CmsEncoder.ENCODING_UTF_8)).append("\";");
                result.append("\nvar useFmts").append(idHash).append(" = true;");
            } else {
                result.append("\nvar useFmts").append(idHash).append(" = false;");
                result.append("\nvar imgFmts").append(idHash).append(" = null;");
                result.append("\nvar imgFmtNames").append(idHash).append(" = null;");
            }
        }
        result.append("\n</script>");

        return result.toString();
    }

    /**
     * Returns the lower case name of the gallery, for example <code>"html"</code>.<p>
     * 
     * @return the lower case name of the gallery
     */
    public abstract String getNameLower();

    /**
     * Returns the upper case name of the gallery, for example <code>"Html"</code>.<p>
     * 
     * @return the upper case name of the gallery
     */
    public abstract String getNameUpper();

    /**
     * Returns <code>true</code> if the preview button should be shown.<p>
     * 
     * @param value the current widget value
     * @return <code>true</code> if the preview button should be shown
     */
    public abstract boolean showPreview(String value);

}
