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

package org.opencms.ade.containerpage.client;

import org.opencms.ade.containerpage.client.ui.A_CmsToolbarOptionButton;
import org.opencms.ade.containerpage.client.ui.CmsContainerPageContainer;
import org.opencms.ade.containerpage.client.ui.CmsContainerPageElementPanel;
import org.opencms.ade.containerpage.client.ui.CmsElementOptionBar;
import org.opencms.ade.containerpage.client.ui.CmsGroupContainerElementPanel;
import org.opencms.ade.containerpage.client.ui.CmsMenuListItem;
import org.opencms.ade.containerpage.client.ui.I_CmsDropContainer;
import org.opencms.ade.containerpage.client.ui.css.I_CmsLayoutBundle;
import org.opencms.ade.containerpage.shared.CmsContainerElement;
import org.opencms.ade.containerpage.shared.CmsContainerElementData;
import org.opencms.gwt.client.ui.CmsErrorDialog;
import org.opencms.gwt.client.util.CmsDebugLog;
import org.opencms.gwt.client.util.CmsDomUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Utility class for the container-page editor.<p>
 * 
 * @since 8.0.0
 */
public class CmsContainerpageUtil {

    /** The container page controller. */
    private CmsContainerpageController m_controller;

    /** List of buttons of the tool-bar. */
    private A_CmsToolbarOptionButton[] m_optionButtons;

    /**
     * Constructor.<p>
     * 
     * @param controller the container page controller
     * @param optionButtons the tool-bar option buttons
     */
    public CmsContainerpageUtil(CmsContainerpageController controller, A_CmsToolbarOptionButton... optionButtons) {

        m_controller = controller;
        m_optionButtons = optionButtons;
    }

    /**
     * Adds an option bar to the given drag element.<p>
     * 
     * @param element the element
     */
    public void addOptionBar(CmsContainerPageElementPanel element) {

        // the view permission is required for any actions regarding this element
        if (element.hasViewPermission()) {
            CmsElementOptionBar optionBar = CmsElementOptionBar.createOptionBarForElement(
                element,
                m_controller.getDndHandler(),
                m_optionButtons);
            element.setElementOptionBar(optionBar);
        }
    }

    /**
     * Transforms all contained elements into {@link CmsContainerPageElementPanel}.<p>
     * 
     * @param container the container
     */
    public void consumeContainerElements(I_CmsDropContainer container) {

        // the drag element widgets are created from the existing DOM elements,
        Element child = (Element)container.getElement().getFirstChildElement();
        while (child != null) {
            boolean isContainerElement = CmsDomUtil.hasClass(
                CmsContainerElement.CLASS_CONTAINER_ELEMENT_START_MARKER,
                child);
            boolean isGroupcontainerElement = CmsDomUtil.hasClass(
                CmsContainerElement.CLASS_GROUP_CONTAINER_ELEMENT_MARKER,
                child);
            if (isContainerElement || isGroupcontainerElement) {
                String serializedData = child.getAttribute("rel");
                CmsContainerElement elementData = null;
                try {
                    elementData = m_controller.getSerializedElement(serializedData);
                } catch (Exception e) {
                    CmsErrorDialog.handleException(new Exception(
                        "Deserialization of element data failed. This may be caused by expired java-script resources, please clear your browser cache and try again.",
                        e));
                }
                if (isContainerElement) {

                    // searching for content element root
                    Element elementRoot = (Element)child.getNextSibling();
                    while ((elementRoot != null) && (elementRoot.getNodeType() != Node.ELEMENT_NODE)) {
                        Element temp = elementRoot;
                        elementRoot = (Element)elementRoot.getNextSibling();
                        temp.removeFromParent();
                    }
                    if (elementRoot == null) {
                        child.removeFromParent();
                        child = null;
                        continue;
                    }
                    if (CmsDomUtil.hasClass(CmsContainerElement.CLASS_CONTAINER_ELEMENT_START_MARKER, elementRoot)) {
                        // broken element, already at next start marker
                        if (elementData != null) {
                            alertParsingError(elementData.getSitePath());
                        }
                        child.removeFromParent();
                        child = elementRoot;
                        continue;
                    }
                    if (CmsDomUtil.hasClass(CmsContainerElement.CLASS_CONTAINER_ELEMENT_END_MARKER, elementRoot)) {
                        // broken element, no content element root
                        if (elementData != null) {
                            alertParsingError(elementData.getSitePath());
                        }
                        child.removeFromParent();
                        child = (Element)elementRoot.getNextSiblingElement();
                        elementRoot.removeFromParent();
                        continue;
                    } else {
                        // looking for the next marker that wraps the current element
                        Element endMarker = (Element)elementRoot.getNextSibling();
                        // only if the end marker node is not null and has neither the end-marker class or start-marker class
                        // remove the current node and check the next sibling 
                        while (!((endMarker == null) || ((endMarker.getNodeType() == Node.ELEMENT_NODE) && (CmsDomUtil.hasClass(
                            CmsContainerElement.CLASS_CONTAINER_ELEMENT_END_MARKER,
                            endMarker) || CmsDomUtil.hasClass(
                            CmsContainerElement.CLASS_CONTAINER_ELEMENT_START_MARKER,
                            endMarker))))) {
                            Element temp = endMarker;
                            endMarker = (Element)endMarker.getNextSibling();
                            temp.removeFromParent();
                        }
                        if (endMarker == null) {
                            if (elementData != null) {
                                alertParsingError(elementData.getSitePath());
                            }
                            // broken element, end marker missing
                            elementRoot.removeFromParent();
                            child.removeFromParent();
                            child = null;
                            continue;
                        }
                        if (CmsDomUtil.hasClass(CmsContainerElement.CLASS_CONTAINER_ELEMENT_START_MARKER, endMarker)) {
                            if (elementData != null) {
                                alertParsingError(elementData.getSitePath());
                            }
                            // broken element, end marker missing
                            elementRoot.removeFromParent();
                            child.removeFromParent();
                            child = endMarker;
                        }
                        if (elementData == null) {
                            // deserialization failed, remove whole element
                            child.removeFromParent();
                            elementRoot.removeFromParent();
                            child = (Element)endMarker.getNextSiblingElement();
                            endMarker.removeFromParent();
                            continue;
                        }
                        CmsDomUtil.removeScriptTags(elementRoot);
                        CmsContainerPageElementPanel containerElement = createElement(
                            elementRoot,
                            container,
                            elementData);
                        if (elementData.isNew()) {
                            containerElement.setNewType(elementData.getResourceType());
                        }
                        container.adoptElement(containerElement);
                        child.removeFromParent();
                        child = (Element)endMarker.getNextSiblingElement();
                        endMarker.removeFromParent();

                    }
                } else if (isGroupcontainerElement && (container instanceof CmsContainerPageContainer)) {
                    if (elementData == null) {
                        // deserialization failed, remove whole group container 
                        Element sibling = (Element)child.getNextSiblingElement();
                        DOM.removeChild((Element)container.getElement(), child);
                        child = sibling;
                        continue;
                    }
                    CmsDomUtil.removeScriptTags(child);
                    CmsGroupContainerElementPanel groupContainer = createGroupcontainer(child, container, elementData);
                    groupContainer.setContainerId(container.getContainerId());
                    container.adoptElement(groupContainer);
                    consumeContainerElements(groupContainer);
                    if (groupContainer.getWidgetCount() == 0) {
                        groupContainer.addStyleName(I_CmsLayoutBundle.INSTANCE.containerpageCss().emptyGroupContainer());
                    }
                    // important: adding the option-bar only after the group-containers have been consumed 
                    addOptionBar(groupContainer);
                    child = (Element)child.getNextSiblingElement();
                }
            } else {
                Element sibling = (Element)child.getNextSiblingElement();
                DOM.removeChild((Element)container.getElement(), child);
                child = sibling;
                continue;
            }
        }
    }

    /**
     * The method will create {@link CmsContainerPageContainer} object for all given containers
     * by converting the associated DOM elements. The contained elements will be transformed into {@link CmsContainerPageElementPanel}.<p>
     * 
     * @param containers the container data
     * 
     * @return the drag target containers
     */
    public Map<String, CmsContainerPageContainer> consumeContainers(Map<String, CmsContainerJso> containers) {

        Map<String, CmsContainerPageContainer> result = new HashMap<String, CmsContainerPageContainer>();
        Iterator<CmsContainerJso> it = containers.values().iterator();
        while (it.hasNext()) {
            CmsContainerJso container = it.next();
            try {
                CmsContainerPageContainer dragContainer = new CmsContainerPageContainer(container);
                consumeContainerElements(dragContainer);
                result.put(container.getName(), dragContainer);
            } catch (Exception e) {
                CmsErrorDialog.handleException(new Exception("Error parsing container "
                    + container.getName()
                    + ". Please check if your HTML is well formed.", e));
            }
        }

        return result;
    }

    /**
     * Creates an drag container element.<p>
     * 
     * @param containerElement the container element data 
     * @param container the container parent
     * 
     * @return the draggable element
     * 
     * @throws Exception if something goes wrong
     */
    public CmsContainerPageElementPanel createElement(
        CmsContainerElementData containerElement,
        I_CmsDropContainer container) throws Exception {

        if (containerElement.isGroupContainer() || containerElement.isInheritContainer()) {
            List<CmsContainerElementData> subElements = new ArrayList<CmsContainerElementData>();
            for (String subId : containerElement.getSubItems()) {
                CmsContainerElementData element = m_controller.getCachedElement(subId);
                if (element != null) {
                    subElements.add(element);
                } else {
                    CmsDebugLog.getInstance().printLine("Cached element not found");
                }
            }
            return createGroupcontainerElement(containerElement, subElements, container);
        }
        com.google.gwt.user.client.Element element = CmsDomUtil.createElement(containerElement.getContents().get(
            container.getContainerId()));
        // ensure any embedded flash players are set opaque so UI elements may be placed above them
        CmsDomUtil.fixFlashZindex(element);
        return createElement(element, container, containerElement);
    }

    /**
     * Creates a drag container element for group-container elements.<p>
     * 
     * @param containerElement the container element data 
     * @param subElements the sub-elements
     * @param container the drag parent
     * 
     * @return the draggable element
     * 
     * @throws Exception if something goes wrong
     */
    public CmsContainerPageElementPanel createGroupcontainerElement(
        CmsContainerElementData containerElement,
        List<CmsContainerElementData> subElements,
        I_CmsDropContainer container) throws Exception {

        com.google.gwt.user.client.Element element = DOM.createDiv();
        element.addClassName(CmsContainerElement.CLASS_GROUP_CONTAINER_ELEMENT_MARKER);
        CmsGroupContainerElementPanel groupContainer = createGroupcontainer(element, container, containerElement);
        groupContainer.setContainerId(container.getContainerId());
        //adding sub-elements
        Iterator<CmsContainerElementData> it = subElements.iterator();
        while (it.hasNext()) {
            CmsContainerElementData subElement = it.next();
            if (subElement.getContents().containsKey(container.getContainerId())) {
                CmsContainerPageElementPanel subDragElement = createElement(subElement, groupContainer);
                groupContainer.add(subDragElement);
            }
        }
        if (subElements.size() == 0) {
            groupContainer.addStyleName(I_CmsLayoutBundle.INSTANCE.containerpageCss().emptyGroupContainer());
        }
        addOptionBar(groupContainer);
        return groupContainer;
    }

    /**
     * Creates a list item.<p>
     * 
     * @param containerElement the element data
     * 
     * @return the list item widget
     */
    public CmsMenuListItem createListItem(CmsContainerElementData containerElement) {

        CmsMenuListItem listItem = new CmsMenuListItem(containerElement);
        listItem.initMoveHandle(m_controller.getDndHandler());
        return listItem;
    }

    /**
     * Displays the element parsing error dialog.<p>
     * 
     * @param sitePath the element site path
     */
    private void alertParsingError(String sitePath) {

        new CmsErrorDialog("Error parsing element "
            + sitePath
            + ". Please check if the HTML generated by the element formatter is well formed.", null).center();
    }

    /**
     * Creates an drag container element.<p>
     * 
     * @param element the DOM element
     * @param dragParent the drag parent
     * @param elementData the element data
     * 
     * @return the draggable element
     */
    private CmsContainerPageElementPanel createElement(
        com.google.gwt.user.client.Element element,
        I_CmsDropContainer dragParent,
        CmsContainerElement elementData) {

        CmsContainerPageElementPanel dragElement = new CmsContainerPageElementPanel(
            element,
            dragParent,
            elementData.getClientId(),
            elementData.getSitePath(),
            elementData.getNoEditReason(),
            elementData.hasSettings(),
            elementData.hasViewPermission(),
            elementData.isReleasedAndNotExpired());
        addOptionBar(dragElement);
        return dragElement;
    }

    /**
     * Creates a drag container element. This will not add an option-bar!<p>
     * 
     * @param element the DOM element
     * @param dragParent the drag parent
     * @param elementData the element data
     * 
     * @return the draggable element
     */
    private CmsGroupContainerElementPanel createGroupcontainer(
        com.google.gwt.user.client.Element element,
        I_CmsDropContainer dragParent,
        CmsContainerElement elementData) {

        CmsGroupContainerElementPanel groupContainer = new CmsGroupContainerElementPanel(
            element,
            dragParent,
            elementData.getClientId(),
            elementData.getSitePath(),
            elementData.getResourceType(),
            elementData.getNoEditReason(),
            elementData.hasSettings(),
            elementData.hasViewPermission(),
            elementData.isReleasedAndNotExpired());
        return groupContainer;
    }

}
