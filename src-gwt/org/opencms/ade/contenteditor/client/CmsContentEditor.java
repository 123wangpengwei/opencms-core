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

package org.opencms.ade.contenteditor.client;

import com.alkacon.acacia.client.EditorBase;
import com.alkacon.acacia.client.HighlightingHandler;
import com.alkacon.acacia.client.I_InlineFormParent;
import com.alkacon.acacia.client.css.I_LayoutBundle;
import com.alkacon.acacia.shared.TabInfo;
import com.alkacon.vie.shared.I_Entity;

import org.opencms.ade.contenteditor.client.css.I_CmsLayoutBundle;
import org.opencms.ade.contenteditor.shared.CmsContentDefinition;
import org.opencms.ade.contenteditor.shared.rpc.I_CmsContentService;
import org.opencms.ade.contenteditor.shared.rpc.I_CmsContentServiceAsync;
import org.opencms.gwt.client.CmsCoreProvider;
import org.opencms.gwt.client.rpc.CmsRpcAction;
import org.opencms.gwt.client.rpc.CmsRpcPrefetcher;
import org.opencms.gwt.client.ui.CmsConfirmDialog;
import org.opencms.gwt.client.ui.CmsErrorDialog;
import org.opencms.gwt.client.ui.CmsInfoHeader;
import org.opencms.gwt.client.ui.CmsModelSelectDialog;
import org.opencms.gwt.client.ui.CmsPushButton;
import org.opencms.gwt.client.ui.CmsToggleButton;
import org.opencms.gwt.client.ui.CmsToolbar;
import org.opencms.gwt.client.ui.I_CmsButton;
import org.opencms.gwt.client.ui.I_CmsButton.ButtonStyle;
import org.opencms.gwt.client.ui.I_CmsButton.Size;
import org.opencms.gwt.client.ui.I_CmsConfirmDialogHandler;
import org.opencms.gwt.client.ui.I_CmsModelSelectHandler;
import org.opencms.gwt.client.ui.css.I_CmsToolbarButtonLayoutBundle;
import org.opencms.gwt.client.ui.input.CmsLabel;
import org.opencms.gwt.client.ui.input.CmsSelectBox;
import org.opencms.gwt.client.util.I_CmsSimpleCallback;
import org.opencms.gwt.shared.CmsIconUtil;
import org.opencms.util.CmsUUID;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.VerticalAlign;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * The in-line content editor.<p>
 */
public final class CmsContentEditor {

    /** The in-line editor instance. */
    private static CmsContentEditor INSTANCE;

    /** The editor base. */
    protected CmsEditorBase m_editor;

    /** The on close call back. */
    protected Command m_onClose;

    /** The edit tool-bar. */
    protected CmsToolbar m_toolbar;

    /** The available locales. */
    private Map<String, String> m_availableLocales;

    /** The form editing base panel. */
    private FlowPanel m_basePanel;

    /** The cancel button. */
    private CmsPushButton m_cancelButton;

    /** The id's of the changed entities. */
    private Set<String> m_changedEntityIds;

    /** The window closing handler registration. */
    private HandlerRegistration m_closingHandlerRegistration;

    /** The locales present within the edited content. */
    private Set<String> m_contentLocales;

    /** The copy locale button. */
    private CmsPushButton m_copyLocaleButton;

    /** The entities to delete. */
    private Set<String> m_deletedEntities;

    /** The delete locale button. */
    private CmsPushButton m_deleteLocaleButton;

    /** Flag indicating the resource needs to removed on cancel. */
    private boolean m_deleteOnCancel;

    /** The in-line edit overlay hiding other content. */
    private CmsInlineEditOverlay m_editOverlay;

    /** The id of the edited entity. */
    private String m_entityId;

    /** The hide help bubbles button. */
    private CmsToggleButton m_hideHelpBubblesButton;

    /** Flag indicating the editor was opened as the stand alone version, not from within any other module. */
    private boolean m_isStandAlone;

    /** The current content locale. */
    private String m_locale;

    /** The locale select label. */
    private CmsLabel m_localeLabel;

    /** The locale select box. */
    private CmsSelectBox m_localeSelect;

    /** The open form button. */
    private CmsPushButton m_openFormButton;

    /** The registered entity id's. */
    private Set<String> m_registeredEntities;

    /** The resource type name. */
    private String m_resourceTypeName;

    /** The save button. */
    private CmsPushButton m_saveButton;

    /** The save and exit button. */
    private CmsPushButton m_saveExitButton;

    /** The resource site path. */
    private String m_sitePath;

    /** The tab informations for this form. */
    private List<TabInfo> m_tabInfos;

    /** The resource title. */
    private String m_title;

    /**
     * Constructor.<p>
     */
    private CmsContentEditor() {

        // set the acacia editor message bundle
        EditorBase.setDictionary(Messages.get().getDictionary());
        I_CmsLayoutBundle.INSTANCE.editorCss().ensureInjected();
        I_CmsLayoutBundle.INSTANCE.widgetCss().ensureInjected();
        I_CmsContentServiceAsync service = GWT.create(I_CmsContentService.class);
        String serviceUrl = CmsCoreProvider.get().link("org.opencms.ade.contenteditor.CmsContentService.gwt");
        ((ServiceDefTarget)service).setServiceEntryPoint(serviceUrl);
        m_editor = new CmsEditorBase(service);
        m_changedEntityIds = new HashSet<String>();
        m_registeredEntities = new HashSet<String>();
        m_availableLocales = new HashMap<String, String>();
        m_contentLocales = new HashSet<String>();
        m_deletedEntities = new HashSet<String>();
    }

    /**
     * Returns the in-line editor instance.<p>
     * 
     * @return the in-line editor instance
     */
    public static CmsContentEditor getInstance() {

        if (INSTANCE == null) {
            INSTANCE = new CmsContentEditor();
        }
        return INSTANCE;
    }

    /**
     * Opens the content editor dialog.<p>
     * 
     * @param locale the content locale
     * @param elementId the element id
     * @param newLink the new link
     * @param modelFileId the model file id
     * @param onClose the command executed on dialog close
     */
    public void openFormEditor(String locale, String elementId, String newLink, CmsUUID modelFileId, Command onClose) {

        m_onClose = onClose;
        CmsUUID structureId = new CmsUUID(elementId);
        if (CmsCoreProvider.get().lock(structureId)) {
            m_editor.loadDefinition(
                CmsContentDefinition.uuidToEntityId(structureId, locale),
                newLink,
                modelFileId,
                new I_CmsSimpleCallback<CmsContentDefinition>() {

                    public void execute(CmsContentDefinition contentDefinition) {

                        if (contentDefinition.isModelInfo()) {
                            openModelSelectDialog(contentDefinition);
                        } else {
                            initEditor(contentDefinition, null, false);
                        }
                    }
                });
        } else {
            showLockedResourceMessage();
        }

    }

    /**
     * Renders the in-line editor for the given element.<p>
     * 
     * @param elementId the element id
     * @param locale the content locale
     * @param panel the element panel
     * @param onClose the command to execute on close
     */
    public void openInlineEditor(CmsUUID elementId, String locale, final I_InlineFormParent panel, Command onClose) {

        String entityId = CmsContentDefinition.uuidToEntityId(elementId, locale);
        m_locale = locale;
        m_onClose = onClose;
        if (CmsCoreProvider.get().lock(elementId)) {
            m_editor.loadDefinition(entityId, new I_CmsSimpleCallback<CmsContentDefinition>() {

                public void execute(CmsContentDefinition contentDefinition) {

                    initEditor(contentDefinition, panel, true);
                }
            });
        } else {
            showLockedResourceMessage();
        }
    }

    /**
     * Opens the form based editor. Used within the stand alone contenteditor.jsp.<p>
     */
    public void openStandAloneFormEditor() {

        CmsContentDefinition definition = null;
        try {
            definition = (CmsContentDefinition)CmsRpcPrefetcher.getSerializedObjectFromDictionary(
                m_editor.getService(),
                I_CmsContentService.DICT_CONTENT_DEFINITION);
        } catch (SerializationException e) {
            RootPanel.get().add(new Label(e.getMessage()));
            return;
        }
        m_isStandAlone = true;
        if (definition.isModelInfo()) {
            openModelSelectDialog(definition);
        } else {
            if (CmsCoreProvider.get().lock(CmsContentDefinition.entityIdToUuid(definition.getEntityId()))) {

                m_editor.registerContentDefinition(definition);
                initEditor(definition, null, false);
            } else {
                showLockedResourceMessage();
            }
        }
    }

    /**
     * Closes the editor.<p>
     */
    protected void clearEditor() {

        if (m_editOverlay != null) {
            m_editOverlay.removeFromParent();
            m_editOverlay = null;
        }
        if (m_toolbar != null) {
            m_toolbar.removeFromParent();
            m_toolbar = null;
        }
        m_cancelButton = null;
        m_localeSelect = null;
        m_openFormButton = null;
        m_saveButton = null;
        m_entityId = null;
        m_onClose = null;
        m_locale = null;
        if (m_basePanel != null) {
            m_basePanel.removeFromParent();
            m_basePanel = null;
        }
        m_changedEntityIds.clear();
        m_registeredEntities.clear();
        m_availableLocales.clear();
        m_contentLocales.clear();
        m_deletedEntities.clear();
        m_title = null;
        m_sitePath = null;
        m_resourceTypeName = null;
        if (m_closingHandlerRegistration != null) {
            m_closingHandlerRegistration.removeHandler();
            m_closingHandlerRegistration = null;
        }
        if (m_isStandAlone) {
            closeEditorWidow();
        } else {
            RootPanel.getBodyElement().removeClassName(I_CmsLayoutBundle.INSTANCE.editorCss().integratedEditor());
        }
    }

    /**
     * Cancels the editing process.<p>
     */
    void cancelEdit() {

        unlockResource();
        if (m_onClose != null) {
            m_onClose.execute();
        }
        m_editor.destroyFrom(true);
        clearEditor();
    }

    /**
     * Copies the current entity values to the given locales.<p>
     * 
     * @param targetLocales the target locales
     */
    void copyLocales(Set<String> targetLocales) {

        for (String targetLocale : targetLocales) {
            String targetId = getIdForLocale(targetLocale);
            if (!m_entityId.equals(targetId)) {
                if (m_registeredEntities.contains(targetId)) {
                    m_editor.unregistereEntity(targetId);
                }
                m_editor.registerClonedEntity(m_entityId, targetId);
                m_registeredEntities.add(targetId);
                m_changedEntityIds.add(targetId);
                m_contentLocales.add(targetLocale);
                m_deletedEntities.remove(targetId);
                enableSave();
            }
        }
        initLocaleSelect();
    }

    /**
     * Deletes the current locale.<p>
     */
    void deleteCurrentLocale() {

        // there has to remain at least one content locale
        if (m_contentLocales.size() > 1) {
            String deletedLocale = m_locale;
            m_contentLocales.remove(deletedLocale);
            m_registeredEntities.remove(m_entityId);
            m_changedEntityIds.remove(m_entityId);
            m_deletedEntities.add(m_entityId);
            m_editor.unregistereEntity(m_entityId);
            enableSave();
            String nextLocale = null;
            if (m_registeredEntities.isEmpty()) {
                nextLocale = m_contentLocales.iterator().next();
            } else {
                nextLocale = CmsContentDefinition.getLocaleFromId(m_registeredEntities.iterator().next());
            }
            switchLocale(nextLocale);
        }
    }

    /**
     * Leaves the editor saving the content if necessary.<p>
     */
    void exitWithSaving() {

        if (m_saveExitButton.isEnabled()) {
            saveAndExit();
        } else {
            cancelEdit();
        }
    }

    /**
     * Hides the editor help bubbles.<p>
     * 
     * @param hide <code>true</code> to hide the help bubbles
     */
    void hideHelpBubbles(boolean hide) {

        m_editor.setShowEditorHelp(!hide);
        HighlightingHandler.getInstance().hideHelpBubbles(RootPanel.get(), hide);
        if (!hide) {
            m_hideHelpBubblesButton.setTitle(Messages.get().key(Messages.GUI_TOOLBAR_HELP_BUBBLES_SHOWN_0));
        } else {
            m_hideHelpBubblesButton.setTitle(Messages.get().key(Messages.GUI_TOOLBAR_HELP_BUBBLES_HIDDEN_0));
        }
    }

    /**
     * Initializes the editor.<p>
     * 
     * @param contentDefinition the content definition
     * @param formParent the inline form parent panel, used for inline editing only
     * @param inline <code>true</code> to render the editor for inline editing
     */
    void initEditor(CmsContentDefinition contentDefinition, I_InlineFormParent formParent, boolean inline) {

        m_locale = contentDefinition.getLocale();
        m_entityId = contentDefinition.getEntityId();
        m_deleteOnCancel = contentDefinition.isDeleteOnCancel();
        initClosingHandler();
        setContentDefinition(contentDefinition);
        initToolbar();
        if (inline && (formParent != null)) {
            m_editOverlay = new CmsInlineEditOverlay(formParent.getElement());
            RootPanel.get().add(m_editOverlay);
            m_editOverlay.updatePosition();
            m_editOverlay.checkZIndex();
            m_editOverlay.addClickHandler(new ClickHandler() {

                public void onClick(ClickEvent event) {

                    exitWithSaving();
                }
            });
            m_hideHelpBubblesButton.setVisible(false);
            setNativeResourceInfo(m_sitePath, m_locale);
            m_editor.renderInlineEntity(m_entityId, formParent);
        } else {
            initFormPanel();
            renderFormContent();
        }
    }

    /**
     * Opens the form based editor.<p>
     */
    void initFormPanel() {

        if (m_editOverlay != null) {
            m_editOverlay.removeFromParent();
            m_editOverlay = null;
        }
        m_openFormButton.setVisible(false);
        m_saveButton.setVisible(true);
        m_hideHelpBubblesButton.setVisible(true);
        m_basePanel = new FlowPanel();
        m_basePanel.addStyleName(I_CmsLayoutBundle.INSTANCE.editorCss().basePanel());
        // insert base panel before the tool bar to keep the tool bar visible 
        RootPanel.get().insert(m_basePanel, RootPanel.get().getWidgetIndex(m_toolbar));
        if (m_isStandAlone) {
            RootPanel.getBodyElement().addClassName(I_CmsLayoutBundle.INSTANCE.editorCss().standAloneEditor());
        } else {
            RootPanel.getBodyElement().addClassName(I_CmsLayoutBundle.INSTANCE.editorCss().integratedEditor());
        }
    }

    /**
     * Opens the copy locale dialog.<p>
     */
    void openCopyLocaleDialog() {

        CmsCopyLocaleDialog dialog = new CmsCopyLocaleDialog(m_availableLocales, m_contentLocales, m_locale, this);
        dialog.center();
    }

    /**
     * Opens the model file select dialog.<p>
     * 
     * @param definition the content definition
     */
    void openModelSelectDialog(final CmsContentDefinition definition) {

        I_CmsModelSelectHandler handler = new I_CmsModelSelectHandler() {

            public void onModelSelect(CmsUUID modelStructureId) {

                if (modelStructureId == null) {
                    modelStructureId = CmsUUID.getNullUUID();
                }
                openFormEditor(
                    definition.getLocale(),
                    definition.getReferenceResourceId().toString(),
                    definition.getNewLink(),
                    modelStructureId,
                    m_onClose);
            }
        };
        String title = org.opencms.gwt.client.Messages.get().key(
            org.opencms.gwt.client.Messages.GUI_MODEL_SELECT_TITLE_0);
        String message = org.opencms.gwt.client.Messages.get().key(
            org.opencms.gwt.client.Messages.GUI_MODEL_SELECT_MESSAGE_0);
        CmsModelSelectDialog dialog = new CmsModelSelectDialog(handler, definition.getModelInfos(), title, message);
        dialog.center();
    }

    /**
     * Renders the form content.<p>
     */
    void renderFormContent() {

        initLocaleSelect();
        setNativeResourceInfo(m_sitePath, m_locale);
        CmsInfoHeader header = new CmsInfoHeader(
            m_title,
            null,
            m_sitePath,
            m_locale,
            CmsIconUtil.getResourceIconClasses(m_resourceTypeName, m_sitePath, false));
        m_basePanel.add(header);
        SimplePanel content = new SimplePanel();
        content.addStyleName(I_CmsLayoutBundle.INSTANCE.editorCss().contentPanel());
        content.addStyleName(I_LayoutBundle.INSTANCE.form().formParent());
        m_basePanel.add(content);

        m_editor.renderEntityForm(m_entityId, m_tabInfos, content, m_basePanel.getElement());
    }

    /**
     * Saves the content and closes the editor.<p> 
     */
    void save() {

        m_editor.saveAndDeleteEntities(m_changedEntityIds, m_deletedEntities, false, new Command() {

            public void execute() {

                setSaved();
                setUnchanged();
            }
        });
    }

    /**
     * Saves the content and closes the editor.<p> 
     */
    void saveAndExit() {

        m_editor.saveAndDeleteEntities(m_changedEntityIds, m_deletedEntities, true, new Command() {

            public void execute() {

                setSaved();
                if (m_onClose != null) {
                    m_onClose.execute();
                }
                clearEditor();
            }
        });
    }

    /**
     * Sets the has changed flag and enables the save button.<p>
     */
    void setChanged() {

        enableSave();
        m_changedEntityIds.add(m_entityId);
        m_deletedEntities.remove(m_entityId);
        if (m_editOverlay != null) {
            m_editOverlay.updatePosition();
        }
    }

    /**
     * Sets the content definition.<p>
     * 
     * @param definition the content definition
     */
    void setContentDefinition(CmsContentDefinition definition) {

        if (m_availableLocales.isEmpty()) {
            // only set the locales when initially setting the content definition
            m_availableLocales.putAll(definition.getAvailableLocales());
            m_contentLocales.addAll(definition.getContentLocales());
        }
        m_title = definition.getTitle();
        m_sitePath = definition.getSitePath();
        m_resourceTypeName = definition.getResourceType();
        m_registeredEntities.add(definition.getEntityId());
        m_tabInfos = definition.getTabInfos();
        m_editor.addEntityChangeHandler(definition.getEntityId(), new ValueChangeHandler<I_Entity>() {

            public void onValueChange(ValueChangeEvent<I_Entity> event) {

                setChanged();
            }
        });
    }

    /**
     * Removes the delete on cancel flag for new resources.<p>
     */
    void setSaved() {

        m_deleteOnCancel = false;
    }

    /**
     * Call after save.<p>
     */
    void setUnchanged() {

        m_changedEntityIds.clear();
        m_deletedEntities.clear();
        m_saveButton.disable(Messages.get().key(Messages.GUI_TOOLBAR_NOTHING_CHANGED_0));
        m_saveExitButton.disable(Messages.get().key(Messages.GUI_TOOLBAR_NOTHING_CHANGED_0));
    }

    /**
     * Switches to the selected locale. Will save changes first.<p>
     * 
     * @param locale the locale to switch to
     */
    void switchLocale(final String locale) {

        if (locale.equals(m_locale)) {
            return;
        }
        m_locale = locale;
        m_basePanel.clear();
        m_editor.destroyFrom(false);
        m_entityId = getIdForLocale(locale);
        // if the content does not contain the requested locale yet, a new node will be created 
        final boolean addedNewLocale = !m_contentLocales.contains(locale);
        if (!m_registeredEntities.contains(m_entityId)) {
            I_CmsSimpleCallback<CmsContentDefinition> callback = new I_CmsSimpleCallback<CmsContentDefinition>() {

                public void execute(CmsContentDefinition contentDefinition) {

                    setContentDefinition(contentDefinition);
                    renderFormContent();
                    if (addedNewLocale) {
                        setChanged();
                    }
                }
            };
            if (addedNewLocale) {
                m_editor.loadNewDefinition(m_entityId, callback);
            } else {
                m_editor.loadDefinition(m_entityId, callback);
            }
        } else {
            renderFormContent();
        }
    }

    /**
     * Unlocks the edited resource.<p>
     */
    void unlockResource() {

        if (m_entityId != null) {
            final CmsUUID structureId = CmsContentDefinition.entityIdToUuid(m_entityId);
            if (m_deleteOnCancel) {
                CmsRpcAction<Void> action = new CmsRpcAction<Void>() {

                    @Override
                    public void execute() {

                        CmsCoreProvider.getVfsService().syncDeleteResource(structureId, this);
                    }

                    @Override
                    protected void onResponse(Void result) {

                        // nothing to do
                    }
                };
                action.executeSync();
            } else {
                CmsCoreProvider.get().unlock(structureId);
            }
        }
    }

    /**
     * Closes the editor.<p>
     */
    private native void closeEditorWidow() /*-{
        if ($wnd.top.cms_ade_closeEditorDialog) {
            $wnd.top.cms_ade_closeEditorDialog();
        } else {
            var backlink = $wnd[@org.opencms.ade.contenteditor.shared.rpc.I_CmsContentService::PARAM_BACKLINK];
            if (backlink) {
                $wnd.top.location.href = backlink;
            }
        }
    }-*/;

    /**
     * Creates a push button for the edit tool-bar.<p>
     * 
     * @param title the button title
     * @param imageClass the image class
     * 
     * @return the button
     */
    private CmsPushButton createButton(String title, String imageClass) {

        CmsPushButton result = new CmsPushButton();
        result.setTitle(title);
        result.setImageClass(imageClass);
        result.setButtonStyle(ButtonStyle.IMAGE, null);
        result.setSize(Size.big);
        return result;
    }

    /**
     * Enables the save buttons.<p>
     */
    private void enableSave() {

        m_saveButton.enable();
        m_saveExitButton.enable();
    }

    /**
     * Returns the entity id for the given locale.<p>
     * 
     * @param locale the locale
     * 
     * @return the entity id
     */
    private String getIdForLocale(String locale) {

        return CmsContentDefinition.uuidToEntityId(CmsContentDefinition.entityIdToUuid(m_entityId), locale);
    }

    /** 
     * Initializes the window closing handler to ensure the resource will be unlocked when leaving the editor.<p>
     */
    private void initClosingHandler() {

        m_closingHandlerRegistration = Window.addWindowClosingHandler(new ClosingHandler() {

            /**
             * @see com.google.gwt.user.client.Window.ClosingHandler#onWindowClosing(com.google.gwt.user.client.Window.ClosingEvent)
             */
            public void onWindowClosing(ClosingEvent event) {

                unlockResource();
            }
        });
    }

    /**
     * Initializes the locale selector.<p>
     */
    private void initLocaleSelect() {

        if (m_availableLocales.size() < 2) {
            return;
        }
        if (m_localeLabel == null) {
            m_localeLabel = new CmsLabel();
            m_localeLabel.addStyleName(I_CmsLayoutBundle.INSTANCE.generalCss().inlineBlock());
            m_localeLabel.addStyleName(I_CmsLayoutBundle.INSTANCE.generalCss().textBig());
            m_localeLabel.setText(Messages.get().key(Messages.GUI_TOOLBAR_LANGUAGE_0));
            m_toolbar.addLeft(m_localeLabel);
        }
        Map<String, String> selectOptions = new HashMap<String, String>();
        for (Entry<String, String> localeEntry : m_availableLocales.entrySet()) {
            if (m_contentLocales.contains(localeEntry.getKey())) {
                selectOptions.put(localeEntry.getKey(), localeEntry.getValue());
            } else {
                selectOptions.put(localeEntry.getKey(), localeEntry.getValue() + " [-]");
            }
        }
        if (m_localeSelect == null) {
            m_localeSelect = new CmsSelectBox(selectOptions);
            m_toolbar.addLeft(m_localeSelect);
            m_localeSelect.addStyleName(I_CmsLayoutBundle.INSTANCE.generalCss().inlineBlock());
            m_localeSelect.getElement().getStyle().setWidth(100, Unit.PX);
            m_localeSelect.getElement().getStyle().setVerticalAlign(VerticalAlign.MIDDLE);
            m_localeSelect.addValueChangeHandler(new ValueChangeHandler<String>() {

                public void onValueChange(ValueChangeEvent<String> event) {

                    switchLocale(event.getValue());
                }
            });
        } else {
            m_localeSelect.setItems(selectOptions);
        }
        m_localeSelect.setFormValueAsString(m_locale);
        if (m_deleteLocaleButton == null) {
            m_deleteLocaleButton = createButton(
                Messages.get().key(Messages.GUI_TOOLBAR_DELETE_LOCALE_0),
                I_CmsToolbarButtonLayoutBundle.INSTANCE.toolbarButtonCss().toolbarDeleteLocale());
            m_deleteLocaleButton.addClickHandler(new ClickHandler() {

                public void onClick(ClickEvent event) {

                    deleteCurrentLocale();
                }
            });
            m_toolbar.addLeft(m_deleteLocaleButton);
        }
        if (m_contentLocales.size() > 1) {
            m_deleteLocaleButton.enable();
        } else {
            m_deleteLocaleButton.disable(Messages.get().key(Messages.GUI_TOOLBAR_CANT_DELETE_LAST_LOCALE_0));
        }
        if (m_copyLocaleButton == null) {
            m_copyLocaleButton = createButton(
                I_CmsButton.ButtonData.COPY_LOCALE.getTitle(),
                I_CmsButton.ButtonData.COPY_LOCALE.getIconClass());
            m_copyLocaleButton.addClickHandler(new ClickHandler() {

                public void onClick(ClickEvent event) {

                    openCopyLocaleDialog();
                }
            });
            m_toolbar.addLeft(m_copyLocaleButton);
        }
    }

    /**
     * Generates the button bar displayed beneath the editable fields.<p>
     */
    private void initToolbar() {

        m_toolbar = new CmsToolbar();
        m_saveExitButton = createButton(
            Messages.get().key(Messages.GUI_TOOLBAR_SAVE_AND_EXIT_0),
            I_CmsToolbarButtonLayoutBundle.INSTANCE.toolbarButtonCss().toolbarSaveExit());
        m_saveExitButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {

                saveAndExit();
            }
        });
        m_saveExitButton.disable(Messages.get().key(Messages.GUI_TOOLBAR_NOTHING_CHANGED_0));
        m_toolbar.addLeft(m_saveExitButton);
        m_saveButton = createButton(
            Messages.get().key(Messages.GUI_TOOLBAR_SAVE_0),
            I_CmsButton.ButtonData.SAVE.getIconClass());
        m_saveButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {

                save();
            }
        });
        m_saveButton.disable(Messages.get().key(Messages.GUI_TOOLBAR_NOTHING_CHANGED_0));
        m_saveButton.setVisible(false);
        m_toolbar.addLeft(m_saveButton);

        m_openFormButton = createButton(
            Messages.get().key(Messages.GUI_TOOLBAR_OPEN_FORM_0),
            I_CmsButton.ButtonData.EDIT.getIconClass());
        m_openFormButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {

                initFormPanel();
                renderFormContent();
            }
        });
        m_toolbar.addLeft(m_openFormButton);

        m_hideHelpBubblesButton = new CmsToggleButton();
        m_hideHelpBubblesButton.setTitle(Messages.get().key(Messages.GUI_TOOLBAR_HELP_BUBBLES_SHOWN_0));
        m_hideHelpBubblesButton.setImageClass(I_CmsButton.ButtonData.TOGGLE_HELP.getIconClass());
        m_hideHelpBubblesButton.setButtonStyle(ButtonStyle.IMAGE, null);
        m_hideHelpBubblesButton.setSize(Size.big);
        m_hideHelpBubblesButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {

                CmsToggleButton button = (CmsToggleButton)event.getSource();
                hideHelpBubbles(!button.isDown());
            }
        });
        if (!CmsCoreProvider.get().isShowEditorHelp()) {
            m_hideHelpBubblesButton.setDown(false);
            HighlightingHandler.getInstance().hideHelpBubbles(RootPanel.get(), true);
            m_hideHelpBubblesButton.setTitle(Messages.get().key(Messages.GUI_TOOLBAR_HELP_BUBBLES_HIDDEN_0));
        }
        m_toolbar.addRight(m_hideHelpBubblesButton);

        m_cancelButton = createButton(
            Messages.get().key(Messages.GUI_TOOLBAR_RESET_0),
            I_CmsButton.ButtonData.RESET.getIconClass());
        m_cancelButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {

                confirmCancel();
            }
        });
        m_toolbar.addRight(m_cancelButton);
        RootPanel.get().add(m_toolbar);
    }

    /**
     * Asks the user to confirm resetting all changes.<p>
     */
    void confirmCancel() {

        if (m_saveButton.isEnabled()) {
            CmsConfirmDialog dialog = new CmsConfirmDialog(org.opencms.gwt.client.Messages.get().key(
                org.opencms.gwt.client.Messages.GUI_DIALOG_RESET_TITLE_0), org.opencms.gwt.client.Messages.get().key(
                org.opencms.gwt.client.Messages.GUI_DIALOG_RESET_TEXT_0));
            dialog.setHandler(new I_CmsConfirmDialogHandler() {

                public void onClose() {

                    // nothing to do
                }

                public void onOk() {

                    cancelEdit();
                }
            });
            dialog.center();
        } else {
            cancelEdit();
        }
    }

    /**
     * Sets the resource info to native window context variables.<p>
     * 
     * @param sitePath the site path
     * @param locale the content locale
     */
    private native void setNativeResourceInfo(String sitePath, String locale)/*-{
        $wnd._editResource = sitePath;
        $wnd._editLanguage = locale;
    }-*/;

    /**
     * Shows the locked resource error message.<p>
     */
    private void showLockedResourceMessage() {

        CmsErrorDialog dialog = new CmsErrorDialog(Messages.get().key(
            Messages.ERR_RESOURCE_ALREADY_LOCKED_BY_OTHER_USER_0), null);
        dialog.addCloseHandler(new CloseHandler<PopupPanel>() {

            public void onClose(CloseEvent<PopupPanel> event) {

                cancelEdit();
            }
        });
        dialog.center();
    }
}
