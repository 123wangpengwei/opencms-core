/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/Attic/CmsProperty.java,v $
 * Date   : $Date: 2003/07/11 13:03:31 $
 * Version: $Revision: 1.3 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2003 Alkacon Software (http://www.alkacon.com)
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

import com.opencms.core.CmsException;
import com.opencms.file.CmsPropertydefinition;
import com.opencms.file.CmsResource;
import com.opencms.file.I_CmsResourceType;
import com.opencms.flex.jsp.CmsJspActionElement;
import com.opencms.util.Encoder;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

/**
 * Provides methods for the properties dialog.<p> 
 * 
 * The following files use this class:
 * <ul>
 * <li>/jsp/dialogs/property_html
 * </ul>
 *
 * @author  Andreas Zahner (a.zahner@alkacon.com)
 * @version $Revision: 1.3 $
 * 
 * @since 5.1
 */
public class CmsProperty extends CmsDialog {

    // always start individual action id's with 100 to leave enough room for more default actions
    public static final int ACTION_SHOW_EDIT = 100;
    public static final int ACTION_SHOW_DEFINE = 200;
    public static final int ACTION_SAVE_EDIT = 300;
    public static final int ACTION_SAVE_DEFINE = 400;
    
    public static final String DIALOG_TYPE = "property";
    
    public static final String DIALOG_SHOW_EDIT = "edit";
    public static final String DIALOG_SHOW_DEFINE = "define";
    public static final String DIALOG_SHOW_DEFAULT = "default";
    
    public static final String DIALOG_SAVE_EDIT = "saveedit";
    public static final String DIALOG_SAVE_DEFINE = "savedefine";
    
    public static final String PREFIX_VALUE = "value-";
    public static final String PREFIX_HIDDEN = "hidden-";
    public static final String PREFIX_USEPROPERTY = "use-";
    
    public static final String PARAM_NEWPROPERTY = "newproperty";    

    private String m_paramNewproperty;
    
    /**
     * Public constructor with JSP action element.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsProperty(CmsJspActionElement jsp) {
        super(jsp);
    }
    
    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */
    public CmsProperty(PageContext context, HttpServletRequest req, HttpServletResponse res) {
        this(new CmsJspActionElement(context, req, res));
    }  
    
    /**
     * Returns the value of the new property parameter, 
     * or null if this parameter was not provided.<p>
     * 
     * The new property parameter stores the name of the 
     * new defined property.<p>
     * 
     * @return the value of the new property parameter
     */    
    public String getParamNewproperty() {
        return m_paramNewproperty;
    }

    /**
     * Sets the value of the new property parameter.<p>
     * 
     * @param value the value to set
     */
    public void setParamNewproperty(String value) {
        m_paramNewproperty = value;
    }
    
    /**
     * Returns all possible properties for the current resource type.<p>
     * 
     * @return all property definitions for te resource type
     * @throws CmsException if something goes wrong
     */
    public Vector getPropertyDefinitions() throws CmsException {
        CmsResource res = (CmsResource)getCms().readFileHeader(getParamFile());
        I_CmsResourceType type = getCms().getResourceType(res.getType());
        return getCms().readAllPropertydefinitions(type.getResourceTypeName());           
    }          

    /**
     * @see org.opencms.workplace.CmsWorkplace#initWorkplaceRequestValues(org.opencms.workplace.CmsWorkplaceSettings, javax.servlet.http.HttpServletRequest)
     */
    protected void initWorkplaceRequestValues(CmsWorkplaceSettings settings, HttpServletRequest request) {
        // fill the parameter values in the get/set methods
        fillParamValues(request);
        // set the dialog type
        setParamDialogtype(DIALOG_TYPE);
        // set the action for the JSP switch 
        if (DIALOG_SHOW_DEFAULT.equals(getParamAction())) {
            setAction(ACTION_DEFAULT);
            setParamTitle(key("title.property") + ": " + CmsResource.getName(getParamFile()));
        } else if (DIALOG_SHOW_EDIT.equals(getParamAction())) {
            setAction(ACTION_SHOW_EDIT);
            setParamTitle(key("title.editpropertyinfo") + ": " + CmsResource.getName(getParamFile()));                            
        } else if (DIALOG_SHOW_DEFINE.equals(getParamAction())) {
            setAction(ACTION_SHOW_DEFINE);
            setParamTitle(key("title.newpropertydef") + ": " + CmsResource.getName(getParamFile()));
        } else if (DIALOG_SAVE_EDIT.equals(getParamAction())) {
            setAction(ACTION_SAVE_EDIT);
        } else if (DIALOG_SAVE_DEFINE.equals(getParamAction())) {
            setAction(ACTION_SAVE_DEFINE);
        } else { 
            // TODO: check here if another default dialog must be displayed                       
            setAction(ACTION_DEFAULT); 
            setParamTitle(key("title.property") + ": " + CmsResource.getName(getParamFile()));
        }      
    } 
    
    /**
     * Creates the HTML String for the list of set properties of the selected resource.<p>
     * 
     * @return the HTML output String of the property list
     * @throws JspException if problems including sub-elements occur
     */
    public String buildPropertiesList() throws JspException {
        StringBuffer retValue = new StringBuffer(256);
        Map properties = null;
        try {
            properties = getCms().readProperties(getParamFile());
        } catch (CmsException e) {
            // error getting properties, show error dialog
            setParamErrorstack(e.getStackTraceAsString());
            String message = "Error reading properties from resource " + getParamFile();
            setParamMessage(message + key("error.message." + getParamDialogtype()));
            getJsp().include(C_FILE_DIALOG_SCREEN_ERROR);
        }        
        
        Iterator i = properties.keySet().iterator();
        boolean setProperties = i.hasNext();
        
        // create table if properties are present
        if (setProperties) {
            retValue.append("<table border=\"0\">\n");
        }
        
        // iterate over all set properties
        while (i.hasNext()) {
            String key = Encoder.escapeXml((String)i.next());           
            String value = Encoder.escapeXml((String)properties.get(key));
            retValue.append("<tr>\n\t<td>");
            retValue.append(key+":</td>\n");
            retValue.append("\t<td>"+value+"</td>\n");
            retValue.append("</tr>\n");
        }
        
        // close table if properties are present
        if (setProperties) {
            retValue.append("</table>");
        } else {
            // no propertis present, show error message
            retValue.append(key("error.message.noprop"));
        }
        
        return retValue.toString();
    }
    
    /**
     * Creates the HTML String for the active properties overview of the current resource.<p>
     * 
     * @return the HTML output String for active properties of the resource
     */
    public String buildActivePropertiesList() {
        StringBuffer retValue = new StringBuffer(256);
        Vector propertyDef = new Vector();
        try {
            propertyDef = getPropertyDefinitions();
        } catch (CmsException e) {}
        
        for (int i=0; i<propertyDef.size(); i++) {
            CmsPropertydefinition curProperty = (CmsPropertydefinition)propertyDef.elementAt(i);
            retValue.append(Encoder.escapeXml(curProperty.getName()));
            if ((i+1) < propertyDef.size()) {
                retValue.append("<br>");            
            }
        }
        
        return retValue.toString();
    }
    
    /**
     * Creates the HTML String for the edit properties form.<p>
     * 
     * @return the HTML output String for the edit properties form
     */
    public String buildEditForm() {
        StringBuffer retValue = new StringBuffer(1024);
        
        // get all properties for the resource
        Vector propertyDef = new Vector();
        try {
            propertyDef = getPropertyDefinitions();
        } catch (CmsException e) {}
        
        // get all used properties for the resource
        Map activeProperties = null;
        try {
            activeProperties = getCms().readProperties(getParamFile());
        } catch (CmsException e) {}
        boolean present = false;
        if (propertyDef.size() > 0) {
            present = true; 
        }
        
        if (present) {
            // there are properties defined for this resource, build the form list
            retValue.append("<table border=\"0\">\n");
            retValue.append("<tr>\n");
            retValue.append("\t<td class=\"textbold\">"+key("input.property")+"</td>\n");
            retValue.append("\t<td class=\"textbold\">"+key("label.value")+"</td>\n");
            retValue.append("\t<td class=\"textbold\" style=\"white-space: nowrap;\">"+key("input.usedproperty")+"</td>\n");            
            retValue.append("</tr>\n");
            retValue.append("<tr>\n\t<td>"+dialogSpacer()+"</td>\n</tr>\n");
            for (int i=0; i<propertyDef.size(); i++) {
                CmsPropertydefinition curProperty = (CmsPropertydefinition)propertyDef.elementAt(i);
                String propName = Encoder.escapeXml(curProperty.getName());
                retValue.append("<tr>\n");
                retValue.append("\t<td style=\"white-space: nowrap;\">"+propName);
                retValue.append("</td>\n");
                retValue.append("\t<td class=\"maxwidth\">");
                if (activeProperties.containsKey(curProperty.getName())) {
                    // the property is used, so create text field with value, checkbox and hidden field
                    String propValue = Encoder.escapeXml((String)activeProperties.get(curProperty.getName()));
                    retValue.append("<input type=\"text\" class=\"maxwidth\" value=\"");
                    retValue.append(propValue+"\" name=\""+PREFIX_VALUE+propName+"\" id=\""+PREFIX_VALUE+propName+"\" onKeyup=\"checkValue('"+propName+"');\">");
                    retValue.append("<input type=\"hidden\" name=\""+PREFIX_HIDDEN+propName+"\" id=\""+PREFIX_HIDDEN+propName+"\" value=\""+propValue+"\"></td>\n");
                    retValue.append("\t<td class=\"textcenter\">");
                    retValue.append("<input type=\"checkbox\" name=\""+PREFIX_USEPROPERTY+propName+"\" id=\""+PREFIX_USEPROPERTY+propName+"\" value=\"true\"");
                    retValue.append(" checked=\"checked\" onClick=\"toggleDelete('"+propName+"');\">");
                    retValue.append("</td>\n");
                } else {
                    // property is not used, create an empty text input field
                    retValue.append("<input type=\"text\" class=\"maxwidth\" ");
                    retValue.append("name=\""+PREFIX_VALUE+propName+"\"></td>\n");
                    retValue.append("\t<td>&nbsp;</td>");
                }
                retValue.append("</tr>\n");
            }
            retValue.append("</table>");
            
        } else {
            // there are no properties defined for this resource, show nothing
            retValue.append("no props defined!");
        }
        
        return retValue.toString();
    }
    
    /**
     * Creates the HTML String for the buttons "edit" and "define properties" depending on the lock state of the resource.<p>
     *  
     * @return the HTML output String for the buttons
     */
    public String buildActionButtons() {
        CmsResource curResource = null;
        try {
            curResource = getCms().readFileHeader(getParamFile());
        } catch (CmsException e) {
            return "";
        }
        if (curResource.isLocked()) {
            // resource is locked, show "define" & "modify" button
            StringBuffer retValue = new StringBuffer(256);
            
            retValue.append("<table border=\"0\">\n");
            retValue.append("<tr>\n\t<td>\n");
            
            //  create button to switch to the edit properties window
            setParamAction(DIALOG_SHOW_EDIT);
            retValue.append("<form action=\""+getDialogUri()+"\" method=\"post\" class=\"nomargin\" name=\"define\">\n");
            retValue.append(paramsAsHidden());
            retValue.append("<input type=\"submit\" class=\"dialogbutton\" style=\"margin-left: 0;\" name=\"ok\" value=\""+key("button.edit")+"\">\n");
            retValue.append("</form>\n");
            
            retValue.append("\t</td>\n");
            retValue.append("\t<td>\n");
            
            
            // create button to switch to the define property window
            setParamAction(DIALOG_SHOW_DEFINE);
            retValue.append("<form action=\""+getDialogUri()+"\" method=\"post\" class=\"nomargin\" name=\"define\">\n");
            retValue.append(paramsAsHidden());
            retValue.append("<input type=\"submit\" class=\"dialogbutton\" name=\"ok\" value=\""+key("button.newpropertydef")+"\">\n");
            retValue.append("</form>\n");
            
            retValue.append("\t</td>\n</tr>\n");
            retValue.append("</table>");
            
            return retValue.toString();
        } else {
            // resource is not locked, don't display edit buttons
            return "";
        }
    }

    /**
     * Performs the define property action, will be called by the JSP page.<p>
     * 
     * @throws JspException if problems including sub-elements occur
     */
    public void actionDefine() throws JspException {
        // save initialized instance of this class in request attribute for included sub-elements
        getJsp().getRequest().setAttribute(C_SESSION_WORKPLACE_CLASS, this);
        try {
            performDefineOperation();
            // set the request parameters before returning to the overview
            setParamAction(DIALOG_SHOW_DEFAULT);
            setParamNewproperty(null);
            getCms().getRequestContext().getResponse().sendCmsRedirect(getJsp().getRequestContext().getUri()+"?"+paramsAsRequest());              
        } catch (CmsException e) {
            // error defining property, show error dialog
            setParamErrorstack(e.getStackTraceAsString());
            setParamMessage(key("error.message.newprop"));
            setParamDialogtype("newprop");
            getJsp().include(C_FILE_DIALOG_SCREEN_ERROR);
         
        } catch (IOException exc){
            getJsp().include(C_FILE_EXPLORER_FILELIST);
        }
    }
    
    /**
     * Performs the definition of a new property.<p>
     * 
     * @return true, if the new property was created, otherwise false
     * @throws CmsException if creation is not successful
     */
    private boolean performDefineOperation() throws CmsException {
        CmsResource res = (CmsResource)getCms().readFileHeader(getParamFile());
        I_CmsResourceType type = getCms().getResourceType(res.getType());
        String newProperty = getParamNewproperty();
        if (newProperty != null && !"".equals(newProperty.trim())) {
            getCms().createPropertydefinition(newProperty, type.getResourceTypeName());
            return true;
        } else {
            throw new CmsException("You entered an invalid property name", CmsException.C_BAD_NAME); 
        } 
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
            performEditOperation(request);
            // set the request parameters before returning to the overview
            setParamAction(DIALOG_SHOW_DEFAULT);
            getCms().getRequestContext().getResponse().sendCmsRedirect(getJsp().getRequestContext().getUri()+"?"+paramsAsRequest());              
        } catch (CmsException e) {
            // error defining property, show error dialog
            setParamErrorstack(e.getStackTraceAsString());
            setParamMessage(key("error.message." + DIALOG_TYPE));
            setParamDialogtype("newprop");
                getJsp().include(C_FILE_DIALOG_SCREEN_ERROR);
     
            } catch (IOException exc){
                getJsp().include(C_FILE_EXPLORER_FILELIST);
            }
    }
    
    /**
     * Performs the editing of the resources properties.<p>
     * 
     * @param request the HttpServletRequest
     * @return true, if the properties were successfully changed, otherwise false
     * @throws CmsException if editing is not successful
     */
    private boolean performEditOperation(HttpServletRequest request) throws CmsException {
        Vector propertyDef = getPropertyDefinitions();
        Map activeProperties = getCms().readProperties(getParamFile());
        
        // check all property definitions of the resource for new values
        for (int i=0; i<propertyDef.size(); i++) {
            CmsPropertydefinition curProperty = (CmsPropertydefinition)propertyDef.elementAt(i);
            String propName = Encoder.escapeXml(curProperty.getName());
            String paramValue = (String)request.getParameter(PREFIX_VALUE+propName);
                        
            // check if there is a parameter value for the current property
            boolean emptyParam = true;
            if (paramValue != null) {
                if (!"".equals(paramValue.trim())) {
                    emptyParam = false;
                }
            }
            if (emptyParam) {
                // parameter is empty, check if the property has to be deleted
                if (activeProperties.containsKey(curProperty.getName())) {
                    getCms().deleteProperty(getParamFile(), curProperty.getName());
                }
            } else {
                // parameter is not empty, check if the value has changed
                String oldValue = (String)request.getParameter(PREFIX_HIDDEN+propName);
                if (!paramValue.equals(oldValue)) {
                    getCms().writeProperty(getParamFile(), curProperty.getName(), paramValue);
                }
            }
        }     
        return true;
    }
    
}
