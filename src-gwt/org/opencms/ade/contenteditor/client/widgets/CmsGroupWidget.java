/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) Alkacon Software GmbH (http://www.alkacon.com)
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

package org.opencms.ade.contenteditor.client.widgets;

import com.alkacon.acacia.client.widgets.I_EditWidget;

import org.opencms.ade.contenteditor.client.css.I_CmsLayoutBundle;
import org.opencms.gwt.client.ui.css.I_CmsImageBundle;
import org.opencms.gwt.client.ui.input.CmsGroupSelection;
import org.opencms.util.CmsStringUtil;

import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;

/**
 * Provides a display only widget, for use on a widget dialog.<br>
 * If there is no value in the content xml, the value<br>
 * set in the configuration string of the xsd is shown.<p>
 *  
 * */
public class CmsGroupWidget extends Composite implements I_EditWidget {

    /** Configuration parameter to set the flags of the groups to display, optional. */
    public static final String CONFIGURATION_FLAGS = "flags";

    /** Configuration parameter to set the organizational unit of the groups to display, optional. */
    public static final String CONFIGURATION_OUFQN = "oufqn";

    /** Configuration parameter to set the user of the groups to display, optional. */
    public static final String CONFIGURATION_USER = "user";

    /** Value of the activation. */
    private boolean m_active = true;

    /** The the flags used in the popup window. */
    private Integer m_flags;

    /** The the organizational unit used in the popup window. */
    private String m_ouFqn;

    /** The the user used in the popup window. */
    private String m_userName;

    /** The disabled textbox to show the value. */
    private CmsGroupSelection m_groupSelection = new CmsGroupSelection(
        new Image(I_CmsImageBundle.INSTANCE.groupImage()).toString());

    /**
     * Creates a new display widget.<p>
     * 
     * @param config The configuration string given from OpenCms XSD.
     */
    public CmsGroupWidget(String config) {

        parseConfiguration(config);
        // All composites must call initWidget() in their constructors.
        m_groupSelection.setParameter(m_flags, m_ouFqn, m_userName);
        m_groupSelection.getTextAreaContainer().setStyleName(I_CmsLayoutBundle.INSTANCE.widgetCss().vfsInputBox());
        initWidget(m_groupSelection);
    }

    /**
     * @see com.google.gwt.event.dom.client.HasFocusHandlers#addFocusHandler(com.google.gwt.event.dom.client.FocusHandler)
     */
    public HandlerRegistration addFocusHandler(FocusHandler handler) {

        return null;
    }

    /**
     * @see com.alkacon.acacia.client.widgets.I_EditWidget#addValueChangeHandler(com.google.gwt.event.logical.shared.ValueChangeHandler)
     */
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {

        return null;
    }

    /**
     * Represents a value change event.<p>
     * 
     */
    public void fireChangeEvent() {

        // ValueChangeEvent.fire(this, result);

    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#getValue()
     */
    public String getValue() {

        return m_groupSelection.getText();
    }

    /**
     * @see com.alkacon.acacia.client.widgets.I_EditWidget#isActive()
     */
    public boolean isActive() {

        return m_active;
    }

    /**
     * @see com.alkacon.acacia.client.widgets.I_EditWidget#onAttachWidget()
     */
    public void onAttachWidget() {

        super.onAttach();
    }

    /**
     * @see com.alkacon.acacia.client.widgets.I_EditWidget#setActive(boolean)
     */
    public void setActive(boolean active) {

        // check if the value has changed. If there is no change do nothing.
        if (m_active == active) {
            return;
        }
        m_groupSelection.setEnabled(active);
        if (!active) {
            m_groupSelection.setFormValueAsString("");
        }
        m_active = active;
        if (active) {
            fireChangeEvent();
        }
    }

    /**
     * Sets the color for the input box.<p>
     * 
     * @param color the color that should be set
     * */
    public void setColor(String color) {

        m_groupSelection.getElement().getStyle().setColor(color);
    }

    /**
     * @see com.alkacon.acacia.client.widgets.I_EditWidget#setName(java.lang.String)
     */
    public void setName(String name) {

        // no input field so nothing to do

    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#setValue(java.lang.Object)
     */
    public void setValue(String value) {

        setValue(value, false);
    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#setValue(java.lang.Object, boolean)
     */
    public void setValue(String value, boolean fireEvents) {

        // add the saved value to the display field
        if (value.equals("")) {
            // m_textbox.setFormValueAsString(m_default);
        } else {
            m_groupSelection.setFormValueAsString(value);
        }
        if (fireEvents) {
            fireChangeEvent();
        }
    }

    /**
     * Parse the configuration.<p>
     * 
     * @param config the configuration
     * */
    private void parseConfiguration(String config) {

        m_userName = null;
        m_flags = null;
        if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(config)) {
            int flagsIndex = config.indexOf(CONFIGURATION_FLAGS);
            if (flagsIndex != -1) {
                // user is given
                String flags = config.substring(CONFIGURATION_FLAGS.length() + 1);
                if (flags.indexOf('|') != -1) {
                    // cut eventual following configuration values
                    flags = flags.substring(0, flags.indexOf('|'));
                }
                try {
                    m_flags = Integer.valueOf(flags);
                } catch (Throwable t) {
                    // invalid flags
                }
            }
            int groupIndex = config.indexOf(CONFIGURATION_USER);
            if (groupIndex != -1) {
                // user is given
                String user = config.substring(groupIndex + CONFIGURATION_USER.length() + 1);
                if (user.indexOf('|') != -1) {
                    // cut eventual following configuration values
                    user = user.substring(0, user.indexOf('|'));
                }
                m_userName = user;
            }
            int oufqnIndex = config.indexOf(CONFIGURATION_OUFQN);
            if (oufqnIndex != -1) {
                // user is given
                String oufqn = config.substring(oufqnIndex + CONFIGURATION_OUFQN.length() + 1);
                if (oufqn.indexOf('|') != -1) {
                    // cut eventual following configuration values
                    oufqn = oufqn.substring(0, oufqn.indexOf('|'));
                }
                m_ouFqn = oufqn;
            }
        }

    }

}
