/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/editor/Attic/CmsEditorActionDefault.java,v $
 * Date   : $Date: 2004/01/06 12:26:42 $
 * Version: $Revision: 1.1 $
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
package org.opencms.workplace.editor;

import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.file.CmsObject;
import com.opencms.file.CmsResource;
import com.opencms.flex.jsp.CmsJspActionElement;
import com.opencms.util.Encoder;
import com.opencms.workplace.I_CmsWpConstants;

import org.opencms.security.CmsPermissionSet;
import org.opencms.workplace.CmsDialog;
import org.opencms.workplace.CmsWorkplaceAction;

import java.io.IOException;

import javax.servlet.jsp.JspException;

/**
 * Provides a method to perform a user defined action when editing a page.<p> 
 *
 * @author  Andreas Zahner (a.zahner@alkacon.com)
 * @version $Revision: 1.1 $
 * 
 * @since 5.3.0
 */
public class CmsEditorActionDefault implements I_CmsEditorActionHandler {

    /**
     * Default constructor needed for editor action handler implementation.<p>
     */
    public CmsEditorActionDefault() {
        // empty constructor
    }
    
    /**
     * @see org.opencms.workplace.editor.I_CmsEditorActionHandler#editorAction(org.opencms.workplace.editor.CmsDefaultPageEditor, com.opencms.flex.jsp.CmsJspActionElement)
     */
    public void editorAction(CmsDefaultPageEditor editor, CmsJspActionElement jsp) throws IOException, JspException {
        // save the edited content
        editor.actionSave();
        // delete temporary file and unlock resource in direct edit mode
        editor.actionClear();
        // create the publish link to redirect to
        String publishLink = jsp.link(I_CmsWpConstants.C_VFS_PATH_WORKPLACE + "jsp/dialogs/publishresource.html");
        String params = "?resource=" + editor.getParamResource() + "&action=" + CmsDialog.DIALOG_CONFIRMED;
        params += "&title=" + Encoder.escapeWBlanks(editor.key("messagebox.title.publishresource") + ": " + editor.getParamResource(), Encoder.C_UTF8_ENCODING) + "&oklink=";
        if ("true".equals(editor.getParamDirectedit())) {
            // append the parameters and the report "ok" button action to the link
            publishLink += params + Encoder.escapeWBlanks("onclick=\"location.href('" + jsp.link(editor.getParamResource()) + "');\"", Encoder.C_UTF8_ENCODING);
        } else {
            // append the parameters and the report "ok" button action to the link
            publishLink += params + Encoder.escapeWBlanks("onclick=\"location.href('" + jsp.link(CmsWorkplaceAction.C_JSP_WORKPLACE_URI) + "');\"", Encoder.C_UTF8_ENCODING);
       
        }
        // redirect to the publish dialog with all necessary parameters
        jsp.getResponse().sendRedirect(publishLink); 
    }
    
    /**
     * @see org.opencms.workplace.editor.I_CmsEditorActionHandler#getButtonName()
     */
    public String getButtonName() {
        return "explorer.context.publish";
    }
    
    /**
     * @see org.opencms.workplace.editor.I_CmsEditorActionHandler#getButtonUrl(java.lang.String, com.opencms.flex.jsp.CmsJspActionElement)
     */
    public String getButtonUrl(String prefix, CmsJspActionElement jsp) {
        String button = "publish.gif";
        if (prefix != null && !"".equals(prefix)) {
            button = prefix + button;
        }
        return jsp.link(button);
    }
    
    /**
     * @see org.opencms.workplace.editor.I_CmsEditorActionHandler#getEditMode(com.opencms.file.CmsObject, java.lang.String)
     */
    public String getEditMode(CmsObject cmsObject, String filename) {
    
        try {
            
            CmsResource res = cmsObject.readFileHeader(filename);
            int currentProject = cmsObject.getRequestContext().currentProject().getId();
        
            if (currentProject == I_CmsConstants.C_PROJECT_ONLINE_ID) {
                // don't render edit area in online project
                return null;
            } else if (CmsResource.getName(filename).startsWith(com.opencms.core.I_CmsConstants.C_TEMP_PREFIX)) {
                // don't show edit area on temporary file
                return C_EDITMODE_INACTIVE;
            } else if (!cmsObject.isInsideCurrentProject(res)) {
                // don't show edit area on files not belonging to the current project
                return C_EDITMODE_INACTIVE;
            } else if (!cmsObject.hasPermissions(res, new CmsPermissionSet(I_CmsConstants.C_PERMISSION_WRITE))) {
                // don't show edit area on files without write permissions
                return C_EDITMODE_INACTIVE;
            }  
    
            // check the lock state
            org.opencms.lock.CmsLock lock = cmsObject.getLock(filename);
            org.opencms.util.CmsUUID userId = cmsObject.getRequestContext().currentUser().getId();
            if (!(lock.isNullLock() || (lock.getUserId().equals(userId) && lock.getProjectId() == currentProject))) {
                // show disabled edit area on resources locked for other users
                return C_EDITMODE_DISABLED;
            }  
  
            // otherwise the resource is editable
            return C_EDITMODE_ENABLED;
            
        }  catch (CmsException exc) {
            
            // something went wrong - so the resource seems not to be editable
            return C_EDITMODE_INACTIVE;
        }
    }

}
