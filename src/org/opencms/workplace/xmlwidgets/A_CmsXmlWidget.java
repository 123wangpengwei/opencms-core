/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/xmlwidgets/Attic/A_CmsXmlWidget.java,v $
 * Date   : $Date: 2004/12/02 09:07:58 $
 * Version: $Revision: 1.11 $
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

package org.opencms.workplace.xmlwidgets;

import org.opencms.file.CmsObject;
import org.opencms.main.OpenCms;
import org.opencms.xml.CmsXmlContentDefinition;
import org.opencms.xml.CmsXmlException;
import org.opencms.xml.I_CmsXmlDocument;
import org.opencms.xml.types.I_CmsXmlContentValue;

import java.util.Map;

/**
 * Base class for XML editor widgets.<p>
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.11 $
 * @since 5.5.0
 */
public abstract class A_CmsXmlWidget implements I_CmsXmlWidget {
  
    /** Postfix for melp message locale. */
    static final String C_HELP_POSTFIX = "help";

    /** Prefix for message locales. */
    static final String C_MESSAGE_PREFIX = "editor.label.";
    
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {

        if (! (obj instanceof A_CmsXmlWidget)) {
            return false;
        }
        
        // widgets are equal if they use the same class
        return getClass().getName().equals(obj.getClass().getName());
    }
    
    
    /**
     * @see org.opencms.workplace.xmlwidgets.I_CmsXmlWidget#getDialogHtmlEnd(org.opencms.file.CmsObject, org.opencms.workplace.xmlwidgets.I_CmsWidgetDialog, org.opencms.xml.types.I_CmsXmlContentValue)
     */
    public String getDialogHtmlEnd(
        CmsObject cms,
        I_CmsWidgetDialog widgetDialog,
        I_CmsXmlContentValue value) {

        return getHelpText(widgetDialog, value.getDocument().getContentDefinition(), value.getElementName());
    }

    /**
     * @see org.opencms.workplace.xmlwidgets.I_CmsXmlWidget#getDialogIncludes(org.opencms.file.CmsObject, org.opencms.workplace.xmlwidgets.I_CmsWidgetDialog, org.opencms.xml.CmsXmlContentDefinition)
     */
    public String getDialogIncludes(
        CmsObject cms,
        I_CmsWidgetDialog widgetDialog,
        CmsXmlContentDefinition contentDefinition) {

        return "";
    }    
    
    /**
     * @see org.opencms.workplace.xmlwidgets.I_CmsXmlWidget#getDialogInitCall(org.opencms.file.CmsObject, org.opencms.workplace.xmlwidgets.I_CmsWidgetDialog)
     */
    public String getDialogInitCall(CmsObject cms, I_CmsWidgetDialog widgetDialog) {

        return "";
    }
    
    
    /**
     * @see org.opencms.workplace.xmlwidgets.I_CmsXmlWidget#getDialogInitMethod(org.opencms.file.CmsObject, org.opencms.workplace.xmlwidgets.I_CmsWidgetDialog, I_CmsXmlDocument)
     */
    public String getDialogInitMethod(
        CmsObject cms,
        I_CmsWidgetDialog widgetDialog,
        I_CmsXmlDocument document) throws CmsXmlException {
        
        if (document == null) {
            throw new CmsXmlException();
        }
        
        return "";
    }
    
    /**
     * @see org.opencms.workplace.xmlwidgets.I_CmsXmlWidget#getParameterName(org.opencms.xml.types.I_CmsXmlContentValue)
     */
    public String getParameterName(I_CmsXmlContentValue value) {

        StringBuffer result = new StringBuffer(128);
        result.append(value.getTypeName());
        result.append(".");
        result.append(value.getElementName());
        result.append(".");
        result.append(value.getIndex());
        return result.toString();
    }
    
    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {

        return getClass().getName().hashCode();
    }

    /**
     * @see org.opencms.workplace.xmlwidgets.I_CmsXmlWidget#setEditorValue(org.opencms.file.CmsObject, java.util.Map, org.opencms.workplace.xmlwidgets.I_CmsWidgetDialog, org.opencms.xml.types.I_CmsXmlContentValue)
     */
    public void setEditorValue(
        CmsObject cms,
        Map formParameters,
        I_CmsWidgetDialog widgetDialog,
        I_CmsXmlContentValue value) throws CmsXmlException {
        
        String[] values = (String[])formParameters.get(getParameterName(value));        
        if ((values != null) && (values.length > 0)) {
            value.setStringValue(cms, values[0]);
        }
    }
    
    /**
     * Creates a help bubble.<p>
     * 
     * @param cms the CmsObject
     * @param widgetDialog the dialog where the widget is used on
     * @param contentDefintion the ContentDefinition or null
     * @param value the value to create the help bubble for
     * @return HTML code for adding a help bubble
     */
    public static String getHelpBubble(CmsObject cms, I_CmsWidgetDialog widgetDialog, CmsXmlContentDefinition contentDefintion, String value) {
        StringBuffer result = new StringBuffer(128);
        String contentDefinitionName = new String();
        // get the name of the content defintion if there is one
        if (contentDefintion != null) {
            contentDefinitionName = contentDefintion.getName();
        }
        // calculate the key
        String locKey = C_MESSAGE_PREFIX + contentDefinitionName + "." + value + "." + C_HELP_POSTFIX;
        String locValue = widgetDialog.key(locKey);
        if (locValue.startsWith("???")) {
            // there was no help message found for this key, so return an empty string
            return "<td></td>";
        } else {
            result.append("<td>");
            result.append("<img id=\"img");
            result.append(locKey);
            result.append("\" name=\"img");
            result.append(value);
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
     * Creates a &lt;div&gt; containing a help text.<p>
     * 
     * @param widgetDialog the dialog where the widget is used on
     * @param contentDefintion the ContentDefinition or null
     * @param value the value to create the help bubble for
     * @return HTML code for adding a help text
     */
    protected String getHelpText(I_CmsWidgetDialog widgetDialog, CmsXmlContentDefinition contentDefintion, String value) {
        StringBuffer result = new StringBuffer(128);
        String contentDefinitionName = new String();
        // get the name of the content defintion if there is one
        if (contentDefintion != null) {
            contentDefinitionName = contentDefintion.getName();
        }
        // calculate the key
        String locKey = C_MESSAGE_PREFIX + contentDefinitionName + "." + value + "." + C_HELP_POSTFIX;
        String locValue = widgetDialog.key(locKey);
        if (locValue.startsWith("???")) {
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
    
    /**
     * Creates a value for message locale with the correct prefix.<p>
     * 
     * @param widgetDialog the dialog where the widget is used on
     * @param contentDefintion the ContentDefinition or null
     * @param value the value to create the message for
     * @return message key for message locales with the correct prefix
     */
    public static String getMessage(I_CmsWidgetDialog widgetDialog, CmsXmlContentDefinition contentDefintion, String value) {
        String contentDefinitionName = new String();
        // get the name of the content defintion if there is one
        if (contentDefintion != null) {
            contentDefinitionName = contentDefintion.getName();
        }
        // calculate the key
        String locKey = C_MESSAGE_PREFIX + contentDefinitionName + "." + value;
        String locValue = widgetDialog.key(locKey);
        if (locValue.startsWith("???")) {
            // there was no value found for this key, so use the unlocalised message key
            locValue = value;
        }
        return locValue;
    }    
}