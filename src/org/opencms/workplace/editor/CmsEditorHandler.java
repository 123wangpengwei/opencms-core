/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/editor/Attic/CmsEditorHandler.java,v $
 * Date   : $Date: 2004/06/21 11:45:59 $
 * Version: $Revision: 1.13 $
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
package org.opencms.workplace.editor;

import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.types.CmsResourceTypePlain;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.workplace.CmsDialog;
import org.opencms.workplace.CmsWorkplace;
import org.opencms.workplace.CmsWorkplaceSettings;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

/**
 * This editor handler class returns the editor URI depending on various factors.<p>
 * 
 * Editor selection criteria:
 * <ul>
 * <li>the user preferences</li>
 * <li>the users current browser</li>
 * <li>the resource type</li>
 * </ul>
 * 
 * @see org.opencms.workplace.editor.I_CmsEditorHandler
 * @see org.opencms.workplace.editor.CmsWorkplaceEditorManager
 *
 * @author  Andreas Zahner (a.zahner@alkacon.com)
 * @version $Revision: 1.13 $
 * 
 * @since 5.3.1
 */
public class CmsEditorHandler extends CmsWorkplace implements I_CmsEditorHandler {
    
    /**
     * Default constructor needed for editor handler implementation.<p>
     */
    public CmsEditorHandler() {
        super(null);
    }
    
    /**
     * @see org.opencms.workplace.editor.I_CmsEditorHandler#getEditorUri(java.lang.String, CmsJspActionElement)
     */
    public String getEditorUri(String resource, CmsJspActionElement jsp) {
        // first try to get the "edit as text" and "load default" parameters from the request
        boolean editAsText = Boolean.valueOf(jsp.getRequest().getParameter(CmsEditor.PARAM_EDITASTEXT)).booleanValue();
        boolean loadDefault = Boolean.valueOf(jsp.getRequest().getParameter(CmsEditor.PARAM_LOADDEFAULT)).booleanValue();
        // initialize resource type with -1 (unknown resource type)
        int resTypeId = -1;
        String resourceType = "";
        if (editAsText) {
            // the resource should be treated as text, set the plain resource id
            resTypeId = CmsResourceTypePlain.C_RESOURCE_TYPE_ID;
        } else {
            try {
                // get the resource type id of the edited resource
                CmsResource res = jsp.getCmsObject().readFileHeader(resource, CmsResourceFilter.ALL);
                resTypeId = res.getTypeId();
            } catch (CmsException e) {
                // resource could not be read, show error dialog
                return showErrorDialog(jsp, e);
            }
        }
        
        try {
            // get the resource type name
            resourceType = OpenCms.getResourceManager().getResourceType(resTypeId).getTypeName();
        } catch (CmsException e) {
            // resource type name can not be determined, show error dialog
            return showErrorDialog(jsp, e);
        }
        
        // get the editor URI from the editor manager
        String editorUri = null;
        
        // get the browser identification from the request
        String userAgent = jsp.getRequest().getHeader("user-agent");
        
        if (loadDefault) {
            // get default editor because loaddefault parameter was found
            editorUri = OpenCms.getWorkplaceManager().getWorkplaceEditorManager().getDefaultEditorUri(jsp.getRequestContext(), resourceType, userAgent);        
        } else {
            // get preferred editor
            editorUri = OpenCms.getWorkplaceManager().getWorkplaceEditorManager().getEditorUri(jsp.getRequestContext(), resourceType, userAgent);
        }
        
        try {
            // check the presence of the editor
            jsp.getCmsObject().readFileHeader(editorUri);
        } catch (Throwable t) {
            // preferred or selected editor not found, try default editor
            editorUri = OpenCms.getWorkplaceManager().getWorkplaceEditorManager().getDefaultEditorUri(jsp.getRequestContext(), resourceType, userAgent);
        }
        
        if (editorUri == null) {
            // no valid editor was found, show the error dialog
            return showErrorDialog(jsp, null);
        }
        return editorUri;
    }  
    
    /**
     * @see org.opencms.workplace.CmsWorkplace#initWorkplaceRequestValues(org.opencms.workplace.CmsWorkplaceSettings, javax.servlet.http.HttpServletRequest)
     */
    protected void initWorkplaceRequestValues(CmsWorkplaceSettings settings, HttpServletRequest request) {
        // empty
    }
    
    /**
     * Shows the error dialog when no valid editor is found and returns null for the editor URI.<p>
     * 
     * @param jsp the instanciated CmsJspActionElement
     * @param t a throwable object, can be null
     * @return always null
     */
    private static String showErrorDialog(CmsJspActionElement jsp, Throwable t)  {
        CmsDialog wp = new CmsDialog(jsp);
        
        wp.fillParamValues(jsp.getRequest());
        wp.setParamMessage(wp.key("error.message.editor.load"));
        wp.setParamReasonSuggestion(wp.key("error.reason.editor.load") + "<br>\n" + wp.key("error.suggestion.editor.load") + "\n");
        if (t != null) {
            // set the error stack parameter, if present
            wp.setParamErrorstack(t.toString());
        }
        // store initialized editor class as request attribute
        jsp.getRequest().setAttribute(CmsWorkplace.C_SESSION_WORKPLACE_CLASS, wp);
        try {
            jsp.include(CmsWorkplace.C_FILE_DIALOG_SCREEN_ERROR);
        } catch (JspException e) {
            // ignore
        }
        return null;
    }
    
}
