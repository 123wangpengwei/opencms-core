/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/xmlwidgets/Attic/CmsXmlBooleanWidget.java,v $
 * Date   : $Date: 2004/11/28 21:57:59 $
 * Version: $Revision: 1.7 $
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
import org.opencms.util.CmsStringUtil;
import org.opencms.xml.CmsXmlContentDefinition;
import org.opencms.xml.CmsXmlException;
import org.opencms.xml.I_CmsXmlDocument;
import org.opencms.xml.types.CmsXmlBooleanValue;
import org.opencms.xml.types.I_CmsXmlContentValue;

import java.util.Map;

/**
 * Provides an editor widget for {@link org.opencms.xml.types.CmsXmlBooleanValue}.<p>
 *
 * @author Andreas Zahner (a.zahner@alkacon.com)
 * 
 * @version $Revision: 1.7 $
 * @since 5.5.2
 */
public class CmsXmlBooleanWidget extends A_CmsXmlWidget {

    /**
     * Creates a new editor widget.<p>
     */
    public CmsXmlBooleanWidget() {

        // empty constructor is required for class registration
    }

    /**
     * @see org.opencms.workplace.xmlwidgets.I_CmsXmlWidget#getDialogWidget(org.opencms.file.CmsObject, org.opencms.xml.I_CmsXmlDocument, org.opencms.workplace.xmlwidgets.I_CmsWidgetDialog, org.opencms.xml.CmsXmlContentDefinition, org.opencms.xml.types.I_CmsXmlContentValue)
     */
    public String getDialogWidget(
        CmsObject cms,
        I_CmsXmlDocument document,
        I_CmsWidgetDialog widgetDialog,
        CmsXmlContentDefinition contentDefinition,
        I_CmsXmlContentValue value) throws CmsXmlException {

        StringBuffer result = new StringBuffer(128);
        result.append("<tr><td class=\"xmlLabel\">");
        result.append(getMessage(widgetDialog, contentDefinition, value.getElementName()));
        result.append(": </td>");
        result.append(getHelpBubble(cms, widgetDialog, contentDefinition, value.getElementName()));
        result.append("<td class=\"xmlTd\">");        
        
        String id = getParameterName(value);       
        result.append("<input type=\"checkbox\" name=\"");
        result.append(id);
        result.append("\" value=\"true\"");
        
        boolean booleanValue = CmsXmlBooleanValue.getBooleanValue(cms, document, value);
        if (booleanValue) {
            result.append(" checked=\"checked\"");    
        }
       
        result.append("></td></tr>\n");
        return result.toString();
    }

    /**
     * @see org.opencms.workplace.xmlwidgets.I_CmsXmlWidget#setEditorValue(org.opencms.file.CmsObject, org.opencms.xml.I_CmsXmlDocument, java.util.Map, org.opencms.workplace.xmlwidgets.I_CmsWidgetDialog, org.opencms.xml.types.I_CmsXmlContentValue)
     */
    public void setEditorValue (
        CmsObject cms,
        I_CmsXmlDocument document,
        Map formParameters,
        I_CmsWidgetDialog widgetDialog,
        I_CmsXmlContentValue value) throws CmsXmlException {

        String[] values = (String[])formParameters.get(getParameterName(value));
        if ((values != null) && (values.length > 0)) {

            // first get the current boolean value for the element
            boolean booleanValue = CmsXmlBooleanValue.getBooleanValue(cms, document, value);
            
            // now check if there's a new value in the form parameters
            String formValue = values[0].trim();
            if (CmsStringUtil.isNotEmpty(formValue)) {
                booleanValue = Boolean.valueOf(formValue).booleanValue();
            }
            
            // set the value
            value.setStringValue(cms, document, String.valueOf(booleanValue));
            
        } else {
            value.setStringValue(cms, document, Boolean.FALSE.toString());
        }
    }
}