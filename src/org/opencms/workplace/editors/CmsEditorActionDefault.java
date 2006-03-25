/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/editors/CmsEditorActionDefault.java,v $
 * Date   : $Date: 2006/03/25 22:42:36 $
 * Version: $Revision: 1.18.2.3 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
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
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.workplace.editors;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.types.CmsResourceTypeXmlPage;
import org.opencms.i18n.CmsEncoder;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.lock.CmsLock;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.security.CmsPermissionSet;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.CmsUUID;
import org.opencms.workplace.CmsDialog;
import org.opencms.workplace.CmsFrameset;
import org.opencms.workplace.CmsWorkplace;
import org.opencms.xml.I_CmsXmlDocument;
import org.opencms.xml.page.CmsXmlPageFactory;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.logging.Log;

/**
 * Provides a method to perform a user defined action when editing a page.<p> 
 *
 * @author  Andreas Zahner 
 * 
 * @version $Revision: 1.18.2.3 $ 
 * 
 * @since 6.0.0 
 */
public class CmsEditorActionDefault implements I_CmsEditorActionHandler {

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsEditorActionDefault.class);

    /**
     * Default constructor needed for editor action handler implementation.<p>
     */
    public CmsEditorActionDefault() {

        // empty constructor
    }

    /**
     * @see org.opencms.workplace.editors.I_CmsEditorActionHandler#editorAction(org.opencms.workplace.editors.CmsEditor, org.opencms.jsp.CmsJspActionElement)
     */
    public void editorAction(CmsEditor editor, CmsJspActionElement jsp) throws IOException, JspException {

        // save the edited content
        editor.actionSave();
        // delete temporary file and unlock resource in direct edit mode
        editor.actionClear(true);
        // create the publish link to redirect to
        String publishLink = jsp.link(CmsWorkplace.PATH_DIALOGS + "publishresource.jsp");
        // define the parameters which are necessary for publishing the resource 
        StringBuffer params = new StringBuffer(64);
        params.append("?resource=");
        params.append(editor.getParamResource());
        params.append("&action=");
        params.append(CmsDialog.DIALOG_CONFIRMED);
        params.append("&directpublish=true&publishsiblings=true");
        params.append("&title=");
        params.append(CmsEncoder.escapeWBlanks(editor.key(Messages.GUI_MESSAGEBOX_TITLE_PUBLISHRESOURCE_0)
            + ": "
            + editor.getParamResource(), CmsEncoder.ENCODING_UTF_8));
        params.append("&").append(CmsDialog.PARAM_REDIRECT).append("=").append(CmsStringUtil.TRUE);
        params.append("&closelink=");
        if (Boolean.valueOf(editor.getParamDirectedit()).booleanValue()) {
            String linkTarget;
            if (!"".equals(editor.getParamBacklink())) {
                linkTarget = jsp.link(editor.getParamBacklink());
            } else {
                linkTarget = jsp.link(editor.getParamResource());
            }
            // append the parameters and the report "ok" button action to the link
            publishLink += params.toString() + CmsEncoder.escapeWBlanks(linkTarget, CmsEncoder.ENCODING_UTF_8);
        } else {
            // append the parameters and the report "ok" button action to the link
            publishLink += params.toString()
                + CmsEncoder.escapeWBlanks(jsp.link(CmsFrameset.JSP_WORKPLACE_URI), CmsEncoder.ENCODING_UTF_8);

        }
        // redirect to the publish dialog with all necessary parameters
        jsp.getResponse().sendRedirect(publishLink);
    }

    /**
     * @see org.opencms.workplace.editors.I_CmsEditorActionHandler#getButtonName()
     */
    public String getButtonName() {

        return Messages.GUI_EXPLORER_CONTEXT_PUBLISH_0;
    }

    /**
     * @see org.opencms.workplace.editors.I_CmsEditorActionHandler#getButtonUrl(CmsJspActionElement, java.lang.String)
     */
    public String getButtonUrl(CmsJspActionElement jsp, String resourceName) {

        // get the button image
        String button = CmsWorkplace.VFS_PATH_RESOURCES + "buttons/publish.png";
        if (!isButtonActive(jsp, resourceName)) {
            // show disabled button if not active
            button = CmsWorkplace.VFS_PATH_RESOURCES + "buttons/publish_in.png";
        }
        return jsp.link(button);
    }

    /**
     * @see org.opencms.workplace.editors.I_CmsEditorActionHandler#getEditMode(org.opencms.file.CmsObject, java.lang.String, java.lang.String, javax.servlet.ServletRequest)
     */
    public String getEditMode(CmsObject cmsObject, String filename, String element, ServletRequest req) {

        try {

            CmsResource resource = cmsObject.readResource(filename, CmsResourceFilter.ALL);
            int currentProject = cmsObject.getRequestContext().currentProject().getId();
            CmsUUID userId = cmsObject.getRequestContext().currentUser().getId();
            CmsLock lock = cmsObject.getLock(filename);
            boolean locked = !(lock.isNullLock() || (lock.getUserId().equals(userId) && lock.getProjectId() == currentProject));

            if (currentProject == CmsProject.ONLINE_PROJECT_ID) {
                // don't render direct edit button in online project
                return null;
            } else if (!OpenCms.getResourceManager().getResourceType(resource.getTypeId()).isDirectEditable()) {
                // don't render direct edit button for non-editable resources 
                return null;
            } else if (CmsResource.getName(filename).startsWith(org.opencms.workplace.CmsWorkplace.TEMP_FILE_PREFIX)) {
                // don't show direct edit button on temporary file
                return DIRECT_EDIT_MODE_INACTIVE;
            } else if (!cmsObject.isInsideCurrentProject(filename)) {
                // don't show direct edit button on files not belonging to the current project
                return DIRECT_EDIT_MODE_INACTIVE;
            } else if (!cmsObject.hasPermissions(
                resource,
                CmsPermissionSet.ACCESS_WRITE,
                false,
                CmsResourceFilter.IGNORE_EXPIRATION)) {
                // don't show direct edit button on files without write permissions
                if (locked) {
                    return DIRECT_EDIT_MODE_DISABLED;
                } else {
                    return DIRECT_EDIT_MODE_INACTIVE;
                }
            } else if (locked) {
                return DIRECT_EDIT_MODE_DISABLED;
            }

            if ((element != null) && (resource.getTypeId() == CmsResourceTypeXmlPage.getStaticTypeId())) {
                // check if the desired element is available (in case of xml page)
                I_CmsXmlDocument document = CmsXmlPageFactory.unmarshal(cmsObject, filename, req);
                List locales = document.getLocales();
                Locale locale;
                if ((locales == null) || (locales.size() == 0)) {
                    locale = (Locale)OpenCms.getLocaleManager().getDefaultLocales(cmsObject, filename).get(0);
                } else {
                    locale = OpenCms.getLocaleManager().getBestMatchingLocale(
                        null,
                        OpenCms.getLocaleManager().getDefaultLocales(cmsObject, filename),
                        locales);
                }
                if (!document.hasValue(element, locale) || !document.isEnabled(element, locale)) {
                    return DIRECT_EDIT_MODE_INACTIVE;
                }
            }

            // otherwise the resource is editable
            return DIRECT_EDIT_MODE_ENABLED;

        } catch (CmsException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(Messages.get().getBundle().key(Messages.LOG_CALC_EDIT_MODE_FAILED_1, filename), e);
            }
            // something went wrong - so the resource seems not to be editable
            return DIRECT_EDIT_MODE_INACTIVE;
        }
    }

    /**
     * @see org.opencms.workplace.editors.I_CmsEditorActionHandler#isButtonActive(CmsJspActionElement, java.lang.String)
     */
    public boolean isButtonActive(CmsJspActionElement jsp, String resourceName) {

        return jsp.getCmsObject().hasPublishPermissions(resourceName);
    }

}
