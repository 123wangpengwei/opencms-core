/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/Attic/CmsPropertyCustom.java,v $
 * Date   : $Date: 2004/06/28 11:18:09 $
 * Version: $Revision: 1.15 $
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
package org.opencms.workplace;

import org.opencms.file.CmsProperty;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.i18n.CmsEncoder;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsException;
import org.opencms.main.I_CmsConstants;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsStringSubstitution;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

/**
 * Provides methods for the customized property dialog.<p> 
 * 
 * This is a special dialog that is used for the different resource types in the workplace.<p>
 * For the xmlpage resource type, this class is extended in the editor subpackage.<p>
 * 
 * The following files use this class:
 * <ul>
 * <li>/jsp/dialogs/property_custom.html
 * </ul>
 * 
 * @author Andreas Zahner (a.zahner@alkacon.com)
 * @version $Revision: 1.15 $
 * 
 * @since 5.3.3
 */
public class CmsPropertyCustom extends CmsPropertyAdvanced {
    
    /** Value for the action: edit the properties. */
    public static final int ACTION_EDIT = 500;
    
    /** Holds all active properties for the current resource. */
    private Map m_activeProperties;
    
    /** Flag to determine if navigation properties are shown. */
    private boolean m_showNavigation;
    
    /** Helper object holding the information about the customized properties. */
    private CmsExplorerTypeSettings m_explorerTypeSettings;
    
    /**
     * Public constructor with JSP action element.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsPropertyCustom(CmsJspActionElement jsp) {
        super(jsp);
    }
    
    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */
    public CmsPropertyCustom(PageContext context, HttpServletRequest req, HttpServletResponse res) {
        this(new CmsJspActionElement(context, req, res));
    }
    
    /**
     * Performs the edit properties action, will be called by the JSP page.<p>
     * 
     * @param request the HttpServletRequest
     * @throws JspException if problems including sub-elements occur
     */
    public void actionEdit(HttpServletRequest request) throws JspException {
        // save initialized instance of this class in request attribute for included sub-elements
        getJsp().getRequest().setAttribute(C_SESSION_WORKPLACE_CLASS, this);
        try {
            // save the changes only if resource is properly locked
            if (isEditable()) {
                performEditOperation(request);    
            }    
        } catch (CmsException e) {
            // Cms error defining property, show error dialog
            setParamErrorstack(e.getStackTraceAsString());
            setParamReasonSuggestion(getErrorSuggestionDefault());
            getJsp().include(C_FILE_DIALOG_SCREEN_ERROR);
        } catch (Exception e) {
            // other error defining property, show error dialog
            setParamErrorstack(e.getStackTrace().toString());
            setParamReasonSuggestion(getErrorSuggestionDefault());
            getJsp().include(C_FILE_DIALOG_SCREEN_ERROR);
        } 
    }
    
    /**
     * Creates the HTML String for the edit properties form.<p>
     * 
     * @return the HTML output String for the edit properties form
     */
    public String buildEditForm() {
        StringBuffer result = new StringBuffer(2048);
        
        // check if the properties are editable
        boolean editable =  isEditable();
                
        // create the column heads
        result.append("<table border=\"0\">\n");
        result.append("<tr>\n");
        result.append("\t<td class=\"textbold\">" + key("input.property") + "</td>\n");
        result.append("\t<td class=\"textbold\">" + key("label.value") + "</td>\n");   
        result.append("\t<td class=\"textbold\" style=\"white-space: nowrap;\">" + key("input.usedproperty") + "</td>\n");    
        result.append("</tr>\n");
        result.append("<tr><td><span style=\"height: 6px;\"></span></td></tr>\n");
        
        // create the text property input rows from explorer type settings
        result.append(buildTextInput(editable));
        
        // show navigation properties if enabled in explorer type settings
        if (showNavigation()) {
            result.append(buildNavigationProperties(editable));
        }
        result.append("</table>");       
       
        return result.toString();
    }
       
    /**
     * Builds the HTML code for the special properties of an xmlpage resource.<p>
     * 
     * @param editable indicates if the properties are editable
     * @return the HTML code for the special properties of a file resource
     */
    protected StringBuffer buildNavigationProperties(boolean editable) {
        StringBuffer result = new StringBuffer(1024);
        
        // create "disabled" attribute if properties are not editable
        String disabled = "";
        if (!editable) {
            disabled = " disabled=\"disabled\"";
        }    
        
        // create "add to navigation" checkbox
        result.append(buildTableRowStart(key("input.addtonav")));
        result.append("<input type=\"checkbox\" name=\"enablenav\" id=\"enablenav\" value=\"true\" onClick=\"toggleNav();\"");
        if (getActiveProperties().containsKey(I_CmsConstants.C_PROPERTY_NAVTEXT) || getActiveProperties().containsKey(I_CmsConstants.C_PROPERTY_NAVPOS)) {
            result.append(" checked=\"checked\"");
        }
        result.append(disabled + ">");
        result.append("</td>\n");
        result.append("\t<td class=\"textcenter\">");       
        result.append("&nbsp;");
        result.append(buildTableRowEnd());
        
        // create NavText input row
        result.append(buildPropertyEntry(I_CmsConstants.C_PROPERTY_NAVTEXT, key("input.navtitle"), editable));
        
        // create NavPos select box row
        result.append(buildTableRowStart(key("input.insert")));
        synchronized (this) {
            result.append(CmsChnav.buildNavPosSelector(getCms(), getParamResource(), disabled + " id=\"navpos\" class=\"maxwidth noborder\"", getSettings().getMessages()));
        }
        // get the old NavPos value and store it in hidden field
        String navPos = null;
        try {
            navPos = getCms().readPropertyObject(getParamResource(), I_CmsConstants.C_PROPERTY_NAVPOS, false).getValue();
        } catch (CmsException e) {
            // should usually never happen
            if (OpenCms.getLog(this).isInfoEnabled()) {
                OpenCms.getLog(this).info(e);
            }
        }
        if (navPos == null) {
            navPos = "";
        }
        result.append("<input type=\"hidden\" name=\"" + PREFIX_HIDDEN + I_CmsConstants.C_PROPERTY_NAVPOS +  "\" value=\"" + navPos + "\">");
        result.append("</td>\n");
        result.append("\t<td class=\"textcenter\">");       
        result.append("&nbsp;");
        result.append(buildTableRowEnd());
 
        return result;
    }
    
    /**
     * Builds the html for a single text input property row.<p>
     * 
     * The html does not include the value of the created property,
     * the values are set delayed (see buildSetFormValues() for details).<p>
     * 
     * @param propertyName the name of the property
     * @param propertyTitle the nice name of the property
     * @param editable indicates if the properties are editable
     * @return the html for a single text input property row
     */
    protected StringBuffer buildPropertyEntry(String propertyName, String propertyTitle, boolean editable) {
        StringBuffer result = new StringBuffer(256);
        // create "disabled" attribute if properties are not editable
        String disabled = "";
        if (!editable) {
            disabled = " disabled=\"disabled\"";
        }
        result.append(buildTableRowStart(propertyTitle));
        if (getActiveProperties().containsKey(propertyName)) {
            // the property is used, so create text field with checkbox and hidden field
            CmsProperty currentProperty = (CmsProperty)getActiveProperties().get(propertyName);
            String propValue = currentProperty.getValue();
            if (propValue != null) {
                propValue = propValue.trim();   
            }
            propValue = CmsEncoder.escapeXml(propValue);
            result.append("<input type=\"text\" class=\"maxwidth\"");
            result.append(" name=\"" + PREFIX_VALUE + propertyName + "\" id=\"" + PREFIX_VALUE + propertyName + "\"");
            if (editable) {
                result.append(" onKeyup=\"checkValue('" + propertyName + "');\"");
            }
            result.append(disabled+">");
            result.append("<input type=\"hidden\" name=\"" + PREFIX_HIDDEN + propertyName + "\" id=\"" + PREFIX_HIDDEN + propertyName + "\" value=\"" + propValue + "\">");
            result.append("</td>\n");
            result.append("\t<td class=\"textcenter\">");
            result.append("<input type=\"checkbox\" name=\"" + PREFIX_USEPROPERTY + propertyName + "\" id=\"" + PREFIX_USEPROPERTY + propertyName + "\" value=\"true\"");
            result.append(" checked=\"checked\"");
            if (editable) {
                result.append(" onClick=\"toggleDelete('" + propertyName + "');\"");
            }
            result.append(disabled + ">");
        } else {
            // property is not used, create an empty text input field
            result.append("<input type=\"text\" class=\"maxwidth\" ");
            result.append("name=\""+PREFIX_VALUE+propertyName+"\" id=\"" + PREFIX_VALUE + propertyName + "\""+disabled+"></td>\n");
            result.append("\t<td class=\"textcenter\">&nbsp;");
        }
        result.append(buildTableRowEnd());
        return result;
    }
    
    /**
     * Builds the JavaScript to set the property form values delayed.<p>
     * 
     * The values of the properties are not inserted directly in the &lt;input&gt; tag,
     * because there is a display issue when the property values are very long.
     * This method creates JavaScript to set the property input field values delayed.
     * On the JSP, the code which is created from this method has to be executed delayed after 
     * the creation of the html form, e.g. in the &lt;body&gt; tag with the attribute
     * onload="window.setTimeout('doSet()',50);".<p>
     * 
     * @return the JavaScript to set the property form values delayed
     */
    public String buildSetFormValues() {
        StringBuffer result = new StringBuffer(1024);
        Iterator i = getExplorerTypeSettings().getProperties().iterator();
        // iterate over the customized properties
        while (i.hasNext()) {
            String curProperty = (String)i.next();
            if (getActiveProperties().containsKey(curProperty)) {
                CmsProperty property = (CmsProperty)getActiveProperties().get(curProperty);
                String propValue = property.getValue();
                if (propValue != null) {
                    propValue = propValue.trim(); 
                    propValue = CmsStringSubstitution.escapeJavaScript(propValue);
                    // create the JS output for a single property
                    result.append("\tdocument.getElementById(\"");    
                    result.append(PREFIX_VALUE + curProperty + "\").value = \"");               
                    result.append(propValue);
                    result.append("\";\n");
                }
            }
        }
        // check if the navigation text property value has to be added
        if (showNavigation() && getActiveProperties().containsKey(I_CmsConstants.C_PROPERTY_NAVTEXT)) {
            CmsProperty property = (CmsProperty)getActiveProperties().get(I_CmsConstants.C_PROPERTY_NAVTEXT);
            String propValue = property.getValue();
            if (propValue != null) {
                propValue = propValue.trim(); 
                propValue = CmsStringSubstitution.escapeJavaScript(propValue);
                // create the JS output for a single property
                result.append("\tdocument.getElementById(\"");    
                result.append(PREFIX_VALUE + I_CmsConstants.C_PROPERTY_NAVTEXT + "\").value = \"");               
                result.append(propValue);
                result.append("\";\n");
            }    
        }
        return result.toString();
    }
    
    /**
     * Builds the HTML for the end of a table row for a single property.<p>
     * 
     * @return the HTML code for a table row end
     */
    protected String buildTableRowEnd() {
        return "</td>\n</tr>\n";
    }
    
    /**
     * Builds the HTML for the start of a table row for a single property.<p>
     * 
     * @param propertyName the name of the current property
     * @return the HTML code for the start of a table row
     */
    protected StringBuffer buildTableRowStart(String propertyName) {
        StringBuffer result = new StringBuffer(96);
        result.append("<tr>\n");
        result.append("\t<td style=\"white-space: nowrap;\" unselectable=\"on\">" + propertyName);
        result.append("</td>\n");
        result.append("\t<td class=\"maxwidth\">");
        return result; 
    }
    
    /**
     * Builds the HTML for the common text input property values stored in the String array "PROPERTIES".<p>
     * 
     * @param editable indicates if the properties are editable
     * @return the HTML code for the common text input fields
     */
    protected StringBuffer buildTextInput(boolean editable) {
        StringBuffer result = new StringBuffer(256);        
        Iterator i = getExplorerTypeSettings().getProperties().iterator();
        // iterate over the properties
        while (i.hasNext()) {
            String curProperty = (String)i.next();
            result.append(buildPropertyEntry(curProperty, curProperty, editable));
        }
        return result;
    }
    
    /**
     * Builds a button row with an "ok", a "cancel" and an "advanced" button.<p>
     * 
     * @param okAttributes additional attributes for the "ok" button
     * @param cancelAttributes additional attributes for the "cancel" button
     * @param advancedAttributes additional attributes for the "advanced" button
     * @return the button row 
     */
    public String dialogButtonsOkCancelAdvanced(String okAttributes, String cancelAttributes, String advancedAttributes) {
        if (isEditable()) {
            int okButton = BUTTON_OK;
            if (getParamDialogmode() != null && getParamDialogmode().startsWith(MODE_WIZARD)) {
                // in wizard mode, display finish button instead of ok button
                okButton = BUTTON_FINISH;
            }
            return dialogButtons(new int[] {okButton, BUTTON_CANCEL, BUTTON_ADVANCED}, new String[] {okAttributes, cancelAttributes, advancedAttributes});
        } else {
            return dialogButtons(new int[] {BUTTON_CLOSE, BUTTON_ADVANCED}, new String[] {cancelAttributes, advancedAttributes});          
        }
    }
    
    /**
     * Initializes the explorer type settings for the current resource type.<p>
     */
    protected void initExplorerTypeSettings() {
        try {
            CmsResource res = getCms().readResource(getParamResource(), CmsResourceFilter.ALL);        
            String resTypeName = OpenCms.getResourceManager().getResourceType(res.getTypeId()).getTypeName();
            setExplorerTypeSettings(OpenCms.getWorkplaceManager().getExplorerTypeSetting(resTypeName));
            setShowNavigation(getExplorerTypeSettings().isShowNavigation());
        } catch (CmsException e) {
            // error reading file, show error dialog
            getJsp().getRequest().setAttribute(C_SESSION_WORKPLACE_CLASS, this);
            setParamErrorstack(e.getStackTraceAsString());
            setParamReasonSuggestion(getErrorSuggestionDefault());
            try {
                getJsp().include(C_FILE_DIALOG_SCREEN_ERROR);
            } catch (JspException exc) {
                if (OpenCms.getLog(this).isErrorEnabled()) {
                    OpenCms.getLog(this).error("Error including common error dialog " + C_FILE_DIALOG_SCREEN_ERROR);
                }      
            }
        }
    }
    
    /**
     * @see org.opencms.workplace.CmsWorkplace#initWorkplaceRequestValues(org.opencms.workplace.CmsWorkplaceSettings, javax.servlet.http.HttpServletRequest)
     */
    protected void initWorkplaceRequestValues(CmsWorkplaceSettings settings, HttpServletRequest request) {
        // fill the parameter values in the get/set methods
        fillParamValues(request);
        // get the explorer type settings for the current resource
        initExplorerTypeSettings();
        // set the dialog type
        setParamDialogtype(DIALOG_TYPE);
        boolean isPopup = Boolean.valueOf(getParamIsPopup()).booleanValue();
        // set the action for the JSP switch 
        if (DIALOG_SHOW_DEFAULT.equals(getParamAction())) {
            // save changed properties and redirect to the default OpenCms dialog
            setAction(ACTION_DEFAULT);
            try {
                actionEdit(request);
                sendCmsRedirect(CmsPropertyAdvanced.URI_PROPERTY_DIALOG + "?" + paramsAsRequest());
            } catch (Exception e) {
                // should usually never happen
                if (OpenCms.getLog(this).isInfoEnabled()) {
                    OpenCms.getLog(this).info(e);
                }
            }          
        } else if (DIALOG_SAVE_EDIT.equals(getParamAction())) {
            // save the edited properties
            if (isPopup) {
                setAction(ACTION_CLOSEPOPUP_SAVE);
            } else {
                setAction(ACTION_SAVE_EDIT);
            }
        } else if (DIALOG_CANCEL.equals(getParamAction())) {
            // save the edited properties
            if (isPopup) {
                setAction(ACTION_CLOSEPOPUP);
            } else {
                setAction(ACTION_CANCEL);
            }
        } else {                   
            setAction(ACTION_EDIT); 
            String resName = CmsResource.getName(getParamResource());
            if (resName.startsWith(I_CmsConstants.C_TEMP_PREFIX)) {
                resName = resName.substring(1);
            }
            setParamTitle(key("title.property") + ": " + resName);
        }         
    } 

    /**
     * Performs the editing of the resources properties.<p>
     * 
     * @param request the HttpServletRequest
     * @return true, if the properties were successfully changed, otherwise false
     * @throws CmsException if editing is not successful
     */
    protected boolean performEditOperation(HttpServletRequest request) throws CmsException {
        boolean useTempfileProject = "true".equals(getParamUsetempfileproject());
        try {
            if (useTempfileProject) {
                switchToTempProject();
            }
            // write the common properties defined in the explorer type settings
            Iterator i = getExplorerTypeSettings().getProperties().iterator();
            // iterate over the properties
            while (i.hasNext()) {
                String curProperty = (String)i.next();
                String paramValue = CmsEncoder.decode(request.getParameter(PREFIX_VALUE + curProperty));
                String oldValue = request.getParameter(PREFIX_HIDDEN + curProperty);
                writeProperty(curProperty, paramValue, oldValue);
            }
            
            // write the navigation properties if enabled
            if (showNavigation()) {
                // get the navigation enabled parameter
                String paramValue = request.getParameter("enablenav");
                String oldValue = null;
                if ("true".equals(paramValue)) {
                    // navigation enabled, update params
                    paramValue = request.getParameter("navpos");
                    if (!"-1".equals(paramValue)) {
                        // update the property only when it is different from "-1" (meaning no change)
                        oldValue = request.getParameter(PREFIX_HIDDEN + I_CmsConstants.C_PROPERTY_NAVPOS);
                        writeProperty(I_CmsConstants.C_PROPERTY_NAVPOS, paramValue, oldValue);
                    }
                    paramValue = request.getParameter(PREFIX_VALUE + I_CmsConstants.C_PROPERTY_NAVTEXT);
                    oldValue = request.getParameter(PREFIX_HIDDEN + I_CmsConstants.C_PROPERTY_NAVTEXT);
                    writeProperty(I_CmsConstants.C_PROPERTY_NAVTEXT, paramValue, oldValue);
                } else {
                    // navigation disabled, delete property values
                    writeProperty(I_CmsConstants.C_PROPERTY_NAVPOS, null, null);
                    writeProperty(I_CmsConstants.C_PROPERTY_NAVTEXT, null, null);
                }                  
            }
        } finally {
            if (useTempfileProject) {
                switchToCurrentProject();
            }
        }
        return true;
    }
    
    /**
     * Writes a property value for a resource, if the value was changed.<p>
     * 
     * If a property definition for the resource does not exist,
     * it is automatically created by this method.<p>
     * 
     * @param propName the name of the property
     * @param propValue the new value of the property
     * @param oldValue the old value of the property
     * @throws CmsException if something goes wrong
     */
    protected void writeProperty(String propName, String propValue, String oldValue) throws CmsException {
        // get the current property object
        CmsProperty currentProperty = (CmsProperty)getActiveProperties().get(propName);
        if (currentProperty == null) {
            // new property, create new property object
            currentProperty = new CmsProperty();
            currentProperty.setKey(propName);
        }
    
        // check if there is a parameter value for the current property
        boolean emptyParam = true;
        if (propValue != null && !"".equals(propValue.trim())) {
                emptyParam = false;
        }
        
        if (emptyParam) {
            // parameter is empty, check if the property has to be deleted
            if (getActiveProperties().containsKey(propName)) {
                // lock resource if autolock is enabled
                checkLock(getParamResource());
                // determine the value to delete
                if (currentProperty.getStructureValue() != null) {
                    currentProperty.setStructureValue(CmsProperty.C_DELETE_VALUE); 
                    currentProperty.setResourceValue(null);
                } else {
                    currentProperty.setResourceValue(CmsProperty.C_DELETE_VALUE);
                    currentProperty.setStructureValue(null);
                }
                // write the updated property object
                getCms().writePropertyObject(getParamResource(), currentProperty);
            }
        } else {
            // parameter is not empty, check if the value has changed
            if (!propValue.equals(oldValue)) {
                // lock resource if autolock is enabled
                checkLock(getParamResource());
                if (currentProperty.getStructureValue() == null && currentProperty.getResourceValue() == null) {
                    // new property, determine setting from OpenCms workplace configuration
                    if (OpenCms.getWorkplaceManager().isDefaultPropertiesOnStructure()) {
                        currentProperty.setStructureValue(propValue);
                        currentProperty.setResourceValue(null);
                    } else {
                        currentProperty.setResourceValue(propValue);
                        currentProperty.setStructureValue(null);
                    }                   
                } else if (currentProperty.getStructureValue() != null) {
                    // structure value has to be updated
                    currentProperty.setStructureValue(propValue);
                    currentProperty.setResourceValue(null);
                } else {
                    // resource value has to be updated
                    currentProperty.setResourceValue(propValue);  
                    currentProperty.setStructureValue(null);
                }
                // set auto-creation of the property to true
                currentProperty.setAutoCreatePropertyDefinition(true);
                // write the updated property object
                getCms().writePropertyObject(getParamResource(), currentProperty);               
            }
        }
    }
    
    /**
     * Returns if navigation properties are shown.<p>
     * 
     * @return true, if navigation properties are shown, otherwise false
     */
    public boolean showNavigation() {
        return m_showNavigation;
    }
    
    /**
     * Sets if navigation properties are shown.<p>
     * 
     * @param showNav true, if navigation properties are shown, otherwise false
     */
    public void setShowNavigation(boolean showNav) {
        m_showNavigation = showNav;
    }
    
    /**
     * Returns a map with CmsProperty object values keyed by property keys.<p>
     * 
     * @return a map with CmsProperty object values
     */
    protected Map getActiveProperties() {

        // get all used properties for the resource
        if (m_activeProperties == null) {
            try {
                m_activeProperties = CmsPropertyAdvanced.getPropertyMap(getCms().readPropertyObjects(
                    getParamResource(),
                    false));
            } catch (CmsException e) {
                // create an empty list
                if (OpenCms.getLog(this).isInfoEnabled()) {
                    OpenCms.getLog(this).info(e);
                }
                m_activeProperties = new HashMap();
            }
        }
        return m_activeProperties;
    }

    /**
     * Returns the explorer type settings for the current resource type.<p>
     * 
     * @return the explorer type settings for the current resource type
     */
    public CmsExplorerTypeSettings getExplorerTypeSettings() {
        return m_explorerTypeSettings;
    }

    /**
     * Sets the explorer type settings for the current resource type.<p>
     * 
     * @param typeSettings the explorer type settings for the current resource type
     */
    public void setExplorerTypeSettings(CmsExplorerTypeSettings typeSettings) {
        m_explorerTypeSettings = typeSettings;
    }

}
