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

package org.opencms.gwt.client.ui.input;

import org.opencms.gwt.client.CmsCoreProvider;
import org.opencms.gwt.client.I_CmsHasInit;
import org.opencms.gwt.client.ui.CmsPushButton;
import org.opencms.gwt.client.ui.I_CmsAutoHider;
import org.opencms.gwt.client.ui.css.I_CmsInputLayoutBundle;
import org.opencms.gwt.client.ui.css.I_CmsLayoutBundle;
import org.opencms.gwt.client.ui.input.form.CmsWidgetFactoryRegistry;
import org.opencms.gwt.client.ui.input.form.I_CmsFormWidgetFactory;
import org.opencms.gwt.shared.CmsLinkBean;
import org.opencms.util.CmsStringUtil;

import java.util.Map;

import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Basic group selection.<p>
 * 
 * @since 8.0.0
 * 
 */
public class CmsGroupSelection extends Composite implements I_CmsFormWidget, I_CmsHasInit {

    /**
     * Event preview handler.<p>
     * 
     * To be used while popup open.<p>
     */
    protected class CloseEventPreviewHandler implements NativePreviewHandler {

        /**
         * @see com.google.gwt.user.client.Event.NativePreviewHandler#onPreviewNativeEvent(com.google.gwt.user.client.Event.NativePreviewEvent)
         */
        public void onPreviewNativeEvent(NativePreviewEvent event) {

            Event nativeEvent = Event.as(event.getNativeEvent());
            switch (DOM.eventGetType(nativeEvent)) {
                case Event.ONMOUSEMOVE:
                    break;
                case Event.ONMOUSEUP:
                    break;
                case Event.ONMOUSEDOWN:
                    break;
                case Event.ONKEYUP:
                    if (m_textBox.getValue().length() > 0) {
                        close();
                    } else {
                        if (m_popup == null) {
                            open();
                        } else if (m_popup.isShowing()) {
                            close();
                        } else {
                            open();
                        }
                    }
                    break;
                case Event.ONMOUSEWHEEL:
                    close();
                    break;
                default:
                    // do nothing
            }
        }

    }

    /** Inner class for the open button. */
    protected class OpenButton extends CmsPushButton {

        /**
         * Default constructor.<p>
         * @param imageClass 
         */
        public OpenButton(String imageClass) {

            super(imageClass);
            setStyleName(I_CmsLayoutBundle.INSTANCE.buttonCss().openVfsButton());

        }

    }

    /** The widget type identifier for this widget. */
    private static final String WIDGET_TYPE = "vfsselection";

    /** The faid panel. */
    protected Panel m_faidpanel = new SimplePanel();

    /** The old value. */
    protected String m_oldValue = "";

    /** The popup frame. */
    protected CmsFramePopup m_popup;

    /** The handler registration. */
    protected HandlerRegistration m_previewHandlerRegistration;

    /** The x-coords of the popup. */
    protected int m_xcoordspopup;

    /** The y-coords of the popup. */
    protected int m_ycoordspopup;

    /** The default rows set. */
    int m_defaultRows;

    /** A counter used for giving text box widgets ids. */
    private static int idCounter;

    /** The root panel containing the other components of this widget. */
    Panel m_panel = new FlowPanel();

    /** The internal text area widget used by this widget. */
    TextBox m_textBox;

    /** The container for the text area. */
    FlowPanel m_textBoxContainer = new FlowPanel();

    /** The error display for this widget. */
    private CmsErrorWidget m_error = new CmsErrorWidget();

    /** The button to to open the selection. */
    private OpenButton m_openSelection;

    /** The id for the windows. */
    private String m_id;

    /** The flag parameter. */
    private Integer m_flags;

    /** The ou parameter. */
    private String m_ouFqn;

    /** The user parameter. */
    private String m_userName;

    /**
     * VsfSelection widget to open the gallery selection.<p>
     * 
     */
    public CmsGroupSelection() {

        super();
    }

    /**
     * VsfSelection widget to open the gallery selection.<p>
     * @param iconImage the image of the icon shown in the 

     */
    public CmsGroupSelection(String iconImage) {

        super();
        m_textBox = new TextBox();
        m_openSelection = new OpenButton(iconImage);
        m_id = "CmsGroupSelection_" + (idCounter++);
        m_textBoxContainer.add(m_openSelection);
        creatFaider();
        initWidget(m_panel);
        m_panel.add(m_textBoxContainer);
        m_faidpanel.setStyleName(I_CmsInputLayoutBundle.INSTANCE.inputCss().vfsInputBoxFaider());
        m_faidpanel.getElement().getStyle().setRight(21, Unit.PX);
        m_faidpanel.getElement().getStyle().setCursor(Cursor.TEXT);
        m_faidpanel.getElement().getStyle().setBottom(7, Unit.PX);

        m_textBoxContainer.add(m_textBox);
        m_faidpanel.addDomHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {

                m_textBox.setFocus(true);
            }
        }, ClickEvent.getType());
        m_panel.add(m_error);
        m_textBoxContainer.addStyleName(I_CmsLayoutBundle.INSTANCE.generalCss().cornerAll());

        m_textBox.addMouseUpHandler(new MouseUpHandler() {

            public void onMouseUp(MouseUpEvent event) {

                m_textBoxContainer.remove(m_faidpanel);
                setTitle("");
                if (m_popup == null) {
                    open();
                } else if (m_popup.isShowing()) {
                    close();
                } else {
                    open();
                }

            }

        });
        m_textBox.addBlurHandler(new BlurHandler() {

            public void onBlur(BlurEvent event) {

                if ((m_textBox.getValue().length() * 6.88) > m_textBox.getOffsetWidth()) {
                    m_textBoxContainer.add(m_faidpanel);
                    setTitle(m_textBox.getValue());
                }
            }
        });

        m_openSelection.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {

                if (m_popup == null) {
                    open();
                } else if (m_popup.isShowing()) {
                    close();
                } else {
                    open();
                }

            }
        });
    }

    /**
     * Initializes this class.<p>
     */
    public static void initClass() {

        // registers a factory for creating new instances of this widget
        CmsWidgetFactoryRegistry.instance().registerFactory(WIDGET_TYPE, new I_CmsFormWidgetFactory() {

            /**
             * @see org.opencms.gwt.client.ui.input.form.I_CmsFormWidgetFactory#createWidget(java.util.Map)
             */
            public I_CmsFormWidget createWidget(Map<String, String> widgetParams) {

                return new CmsGroupSelection();
            }
        });
    }

    /**
     * @param handler
     */
    public void addValueChangeHandler(ValueChangeHandler<String> handler) {

        m_textBox.addValueChangeHandler(handler);
    }

    /**
     * @see org.opencms.gwt.client.ui.input.I_CmsFormWidget#getApparentValue()
     */
    public String getApparentValue() {

        return getFormValueAsString();
    }

    /**
     * @see org.opencms.gwt.client.ui.input.I_CmsFormWidget#getFieldType()
     */
    public FieldType getFieldType() {

        return I_CmsFormWidget.FieldType.STRING;
    }

    /**
     * @see org.opencms.gwt.client.ui.input.I_CmsFormWidget#getFormValue()
     */
    public Object getFormValue() {

        if (m_textBox.getText() == null) {
            return "";
        }
        return m_textBox.getValue();
    }

    /**
     * @see org.opencms.gwt.client.ui.input.I_CmsFormWidget#getFormValueAsString()
     */
    public String getFormValueAsString() {

        return (String)getFormValue();
    }

    /**
     * Returns the selected link as a bean.<p>
     * 
     * @return the selected link as a bean 
     */
    public CmsLinkBean getLinkBean() {

        String link = m_textBox.getValue();
        if (CmsStringUtil.isEmptyOrWhitespaceOnly(link)) {
            return null;
        }
        return new CmsLinkBean(m_textBox.getText(), true);
    }

    /**
     * Returns the text contained in the text area.<p>
     * 
     * @return the text in the text area
     */
    public String getText() {

        return m_textBox.getValue();
    }

    /**
     * Returns the textarea of this widget.<p>
     * 
     * @return the textarea
     */
    public TextBox getTextArea() {

        return m_textBox;
    }

    /**
     * Returns the text box container of this widget.<p>
     * 
     * @return the text box container
     */
    public FlowPanel getTextAreaContainer() {

        return m_textBoxContainer;
    }

    /**
     * @see org.opencms.gwt.client.ui.input.I_CmsFormWidget#isEnabled()
     */
    public boolean isEnabled() {

        return m_textBox.isEnabled();
    }

    /**
     * @see org.opencms.gwt.client.ui.input.I_CmsFormWidget#reset()
     */
    public void reset() {

        m_textBox.setText("");
    }

    /**
     * @see org.opencms.gwt.client.ui.input.I_CmsFormWidget#setAutoHideParent(org.opencms.gwt.client.ui.I_CmsAutoHider)
     */
    public void setAutoHideParent(I_CmsAutoHider autoHideParent) {

        // nothing to do
    }

    /**
     * @see org.opencms.gwt.client.ui.input.I_CmsFormWidget#setEnabled(boolean)
     */
    public void setEnabled(boolean enabled) {

        m_textBox.setEnabled(enabled);
    }

    /**
     * @see org.opencms.gwt.client.ui.input.I_CmsFormWidget#setErrorMessage(java.lang.String)
     */
    public void setErrorMessage(String errorMessage) {

        m_error.setText(errorMessage);
    }

    /**
     * Sets the value of the widget.<p>
     * 
     * @param value the new value 
     */
    public void setFormValue(Object value) {

        if (value == null) {
            value = "";
        }
        if (value instanceof String) {
            String strValue = (String)value;
            m_textBox.setText(strValue);
            creatFaider();
            setTitle(strValue);
        }

    }

    /**
     * @see org.opencms.gwt.client.ui.input.I_CmsFormWidget#setFormValueAsString(java.lang.String)
     */
    public void setFormValueAsString(String newValue) {

        setFormValue(newValue);
    }

    /**
     * Sets the link from a bean.<p>
     * 
     * @param link the link bean 
     */
    public void setLinkBean(CmsLinkBean link) {

        if (link == null) {
            link = new CmsLinkBean("", true);
        }
        m_textBox.setValue(link.getLink());
    }

    /**
     * Sets the name of the input field.<p>
     * 
     * @param name of the input field
     * */
    public void setName(String name) {

        m_textBox.setName(name);

    }

    /**
     * Sets the parameters for the popup.<p>
     * 
     * @param flags the flaq parameter 
     * @param ouFqn the ouFqn parameter
     * @param userName the user Name parameter
     * 
     * */
    public void setParameter(Integer flags, String ouFqn, String userName) {

        m_flags = flags;
        m_ouFqn = ouFqn;
        m_userName = userName;
    }

    /**
     * Sets the text in the text area.<p>
     * 
     * @param text the new text
     */
    public void setText(String text) {

        m_textBox.setValue(text);
    }

    /**
     * @see com.google.gwt.user.client.ui.UIObject#setTitle(java.lang.String)
     */
    @Override
    public void setTitle(String title) {

        if ((title.length() * 6.88) > m_panel.getOffsetWidth()) {
            m_textBox.getElement().setTitle(title);
        } else {
            m_textBox.getElement().setTitle("");
        }
    }

    /**
     * Creates the URL for the gallery dialog IFrame.<p>
     * 
     * @return the URL for the gallery dialog IFrame 
     */
    protected String buildGalleryUrl() {

        String basePath = "";

        basePath = "/system/workplace/commons/group_selection.jsp?type=groupwidget&fieldid=" + m_id;

        if (m_flags != null) {
            basePath += "&flags=" + m_flags;

        }
        if (m_userName != null) {
            basePath += "&user=" + m_userName;
        }
        if (m_ouFqn != null) {
            basePath += "&oufqn=" + m_ouFqn;
        }

        //basePath += "&gwt.codesvr=127.0.0.1:9996"; //to start the hosted mode just remove commentary  
        return CmsCoreProvider.get().link(basePath);
    }

    /**
     * Close the popup of this widget.<p>
     * */
    protected void close() {

        m_popup.hideDelayed();
        m_textBox.setFocus(true);
        m_textBox.setCursorPos(m_textBox.getText().length());
    }

    /**
     * Opens the popup of this widget.<p>
     * */
    protected void open() {

        m_oldValue = m_textBox.getValue();
        if (m_popup == null) {
            String title = org.opencms.gwt.client.Messages.get().key(
                org.opencms.gwt.client.Messages.GUI_GALLERY_SELECT_DIALOG_TITLE_0);
            m_popup = new CmsFramePopup(title, buildGalleryUrl());

            m_popup.setCloseHandler(new Runnable() {

                public void run() {

                    String textboxValue = m_textBox.getText();

                    if (!m_oldValue.equals(textboxValue)) {
                        m_textBox.setValue("", true);
                        m_textBox.setValue(textboxValue, true);
                    }

                    if (m_previewHandlerRegistration != null) {
                        m_previewHandlerRegistration.removeHandler();
                        m_previewHandlerRegistration = null;
                    }
                    m_textBox.setFocus(true);
                    m_textBox.setCursorPos(m_textBox.getText().length());
                }
            });
            m_popup.setModal(false);
            m_popup.setId(m_id);
            m_popup.setWidth(717);

            m_popup.getFrame().setSize("705px", "300px");

            m_popup.addDialogClose(new Command() {

                public void execute() {

                    close();

                }
            });
        } else {
            m_popup.getFrame().setUrl(buildGalleryUrl());
        }
        m_popup.setAutoHideEnabled(true);
        m_popup.showRelativeTo(m_textBox);
        if (m_previewHandlerRegistration == null) {
            m_previewHandlerRegistration = Event.addNativePreviewHandler(new CloseEventPreviewHandler());
        }

        m_xcoordspopup = m_popup.getPopupLeft();
        m_ycoordspopup = m_popup.getPopupTop();

    }

    /**
     * Adds the fader if necessary.<p> 
     * */
    private void creatFaider() {

        if ((m_textBox.getValue().length() * 6.88) > m_textBox.getOffsetWidth()) {
            m_textBoxContainer.add(m_faidpanel);
        }
    }
}
