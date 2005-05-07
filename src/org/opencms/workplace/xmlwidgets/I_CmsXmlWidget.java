/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/xmlwidgets/Attic/I_CmsXmlWidget.java,v $
 * Date   : $Date: 2005/05/07 16:08:27 $
 * Version: $Revision: 1.10 $
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

package org.opencms.workplace.xmlwidgets;

import org.opencms.file.CmsObject;
import org.opencms.xml.CmsXmlException;

import java.util.Map;

/**
 * Describes an editor widget for use in the OpenCms workplace.<p>
 * 
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.10 $
 * @since 5.5.0
 */
public interface I_CmsXmlWidget {

    /**
     * Generates the html to include at the end of the dialog for this widget.<p>
     * 
     * The "help bubble" text div's are added using this method.<p>
     * 
     * @param cms the current users OpenCms context
     * @param widgetDialog the dialog where the widget is used on
     * @param param the widget parameter to generate the widget for
     * 
     * @return the html to include at the end of the dialog for this widget
     * 
     * @throws CmsXmlException if something goes wrong
     */
    String getDialogHtmlEnd(CmsObject cms, I_CmsWidgetDialog widgetDialog, I_CmsWidgetParameter param)
    throws CmsXmlException;

    /**
     * Generates the necessary JavaScript inclusion code for this widget.<p>
     * 
     * @param cms the current users OpenCms context
     * @param widgetDialog the dialog where the widget is used on
     * 
     * @return the JavaScript inclusion code
     * 
     * @throws CmsXmlException if something goes wrong
     */
    String getDialogIncludes(CmsObject cms, I_CmsWidgetDialog widgetDialog) throws CmsXmlException;

    /**
     * Generates the initialisation method JavaScript code for this widget.<p>
     * 
     * @param cms the current users OpenCms context
     * @param widgetDialog the dialog where the widget is used on
     * 
     * @return the initialisation method JavaScript code
     * 
     * @throws CmsXmlException if something goes wrong
     */
    String getDialogInitCall(CmsObject cms, I_CmsWidgetDialog widgetDialog) throws CmsXmlException;

    /**
     * Generates the initialization method JavaScript code for this widget.<p>
     * 
     * @param cms an initialized instance of a CmsObject
     * @param widgetDialog the dialog where the widget is used on
     * 
     * @return the initialization method JavaScript code
     * 
     * @throws CmsXmlException if something goes wrong
     */
    String getDialogInitMethod(CmsObject cms, I_CmsWidgetDialog widgetDialog) throws CmsXmlException;

    /**
     * Generates the widget HTML for the provided widget parameter.<p>
     * 
     * @param cms an initialized instance of a CmsObject
     * @param widgetDialog the dialog where the widget is used on
     * @param param the widget parameter to generate the widget for
     * 
     * @return the widget HTML for the provided widget parameter
     * 
     * @throws CmsXmlException if something goes wrong
     */
    String getDialogWidget(CmsObject cms, I_CmsWidgetDialog widgetDialog, I_CmsWidgetParameter param)
    throws CmsXmlException;

    /**
     * Creates the HTML code for the help bubble for this widget.<p>
     * 
     * @param cms the current users OpenCms context
     * @param widgetDialog the dialog where the widget is used on
     * @param param the widget parameter to generate the widget for
     * 
     * @return the HTML code for the help bubble for this widget
     */
    String getHelpBubble(CmsObject cms, I_CmsWidgetDialog widgetDialog, I_CmsWidgetParameter param);

    /**
     * Creates a HTML &lt;div&gt; containing the help text for this widget.<p>
     * 
     * @param widgetDialog the dialog where the widget is used on
     * @param value the value to create the help bubble for
     * 
     * @return a HTML &lt;div&gt; containing the help text for this widget
     */
    String getHelpText(I_CmsWidgetDialog widgetDialog, I_CmsWidgetParameter value);

    /**
     * Sets the value of in the given widget parameter by reading the "right" 
     * value from the offered map of parameters.<p>
     * 
     * @param cms the current users OpenCms context
     * @param formParameters the map of parameters to get the value from
     * @param widgetDialog the dialog where the widget is used on
     * @param param the widget parameter to generate the widget for
     * 
     * @throws CmsXmlException if something goes wrong
     */
    void setEditorValue(CmsObject cms, Map formParameters, I_CmsWidgetDialog widgetDialog, I_CmsWidgetParameter param)
    throws CmsXmlException;
}