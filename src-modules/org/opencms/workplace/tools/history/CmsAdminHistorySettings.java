/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/workplace/tools/history/Attic/CmsAdminHistorySettings.java,v $
 * Date   : $Date: 2007/05/16 15:57:31 $
 * Version: $Revision: 1.12.4.1 $
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

package org.opencms.workplace.tools.history;

import org.opencms.configuration.CmsSystemConfiguration;
import org.opencms.i18n.CmsMessages;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsIllegalArgumentException;
import org.opencms.main.OpenCms;
import org.opencms.workplace.CmsDialog;
import org.opencms.workplace.CmsWorkplaceSettings;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

/**
 * Provides methods for the history settings dialog.<p> 
 * 
 * The following files use this class:
 * <ul>
 * <li>/system/workplace/administration/history/settings/index.jsp
 * </ul>
 * <p>
 *
 * @author  Andreas Zahner 
 * 
 * @version $Revision: 1.12.4.1 $ 
 * 
 * @since 6.0.0 
 */
public class CmsAdminHistorySettings extends CmsDialog {

    /** Value for the action: save the settings. */
    public static final int ACTION_SAVE_EDIT = 300;

    /** Request parameter value for the action: save the settings. */
    public static final String DIALOG_SAVE_EDIT = "saveedit";

    /** The dialog type. */
    public static final String DIALOG_TYPE = "historysettings";

    /**
     * Public constructor with JSP action element.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsAdminHistorySettings(CmsJspActionElement jsp) {

        super(jsp);
    }

    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */
    public CmsAdminHistorySettings(PageContext context, HttpServletRequest req, HttpServletResponse res) {

        this(new CmsJspActionElement(context, req, res));
    }

    /**
     * Performs the change of the history settings, this method is called by the JSP.<p>
     * 
     * @param request the HttpServletRequest
     * @throws JspException if something goes wrong
     */
    public void actionEdit(HttpServletRequest request) throws JspException {

        // save initialized instance of this class in request attribute for included sub-elements
        getJsp().getRequest().setAttribute(SESSION_WORKPLACE_CLASS, this);
        try {
            performEditOperation(request);
            // set the request parameters before returning to the overview
            actionCloseDialog();
        } catch (CmsIllegalArgumentException e) {
            // error setting history values, show error dialog

            includeErrorpage(this, e);
        }
    }

    /**
     * Builds the HTML for the history settings input form.<p>
     * 
     * @return the HTML code for the history settings input form
     */
    public String buildSettingsForm() {

        StringBuffer retValue = new StringBuffer(512);
        boolean histEnabled = OpenCms.getSystemInfo().isHistoryEnabled();
        int maxVersions = OpenCms.getSystemInfo().getHistoryVersions();
        CmsMessages messages = Messages.get().getBundle(getLocale());
        retValue.append("<table border=\"0\">\n");
        retValue.append("<tr>\n");
        retValue.append("<td>" + messages.key(Messages.GUI_INPUT_HISTENABLED_0) + "</td>\n");
        retValue.append("<td><input type=\"radio\" name=\"enable\" id=\"enabled\" value=\"true\" onclick=\"checkEnabled();\"");
        if (histEnabled) {
            retValue.append(" checked=\"checked\"");
        }
        retValue.append("></td>\n");
        retValue.append("<td>" + messages.key(Messages.GUI_INPUT_HISTENABLE_YES_0) + "</td>\n");
        retValue.append("<td>&nbsp;</td>\n");
        retValue.append("<td><input type=\"radio\" name=\"enable\" id=\"disabled\" value=\"false\" onclick=\"checkEnabled();\"");
        if (!histEnabled) {
            retValue.append(" checked=\"checked\"");
        }
        retValue.append("></td>\n");
        retValue.append("<td>" + messages.key(Messages.GUI_INPUT_HISTENABLE_NO_0) + "</td>\n");
        retValue.append("</tr>\n");
        retValue.append("</table>\n");

        retValue.append("<div class=\"hide\" id=\"settings\">\n");
        retValue.append("<table border=\"0\">\n");
        retValue.append("<tr>\n");
        retValue.append("<td>" + messages.key(Messages.GUI_INPUT_HISTNUMBER_0) + "</td>\n");
        retValue.append("<td colspan=\"5\"><input type=\"text\" name=\"versions\" value=\"");
        if (maxVersions != -1) {
            retValue.append(maxVersions);
        }
        retValue.append("\" onkeypress=\"event.returnValue=isDigit();\"></td>\n");
        retValue.append("</tr>\n");
        retValue.append("</table>\n");
        retValue.append("</div>\n");

        return retValue.toString();
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
        if (DIALOG_SAVE_EDIT.equals(getParamAction())) {
            setAction(ACTION_SAVE_EDIT);
        } else if (DIALOG_CANCEL.equals(getParamAction())) {
            setAction(ACTION_CANCEL);
        } else {
            // set the default action               
            setAction(ACTION_DEFAULT);
            setParamTitle(Messages.get().getBundle(getLocale()).key(Messages.GUI_LABEL_ADMNIN_HISTORY_SETTINGS_0));
        }
    }

    /**
     * Performs the change of the history settings.<p>
     * 
     * @param request the HttpServletRequest
     * @return true if everything was ok
     * @throws CmsIllegalArgumentException if the entered number is no positive integer
     */
    private boolean performEditOperation(HttpServletRequest request) throws CmsIllegalArgumentException {

        // get the new settings from the request parameters
        String paramEnabled = request.getParameter("enable");
        String paramVersions = request.getParameter("versions");
        String paramVersionsDeleted = request.getParameter("versionsDeleted");

        // check the submitted values
        boolean enabled = Boolean.valueOf(paramEnabled).booleanValue();
        int versions = 0;
        try {
            versions = Integer.parseInt(paramVersions);
        } catch (NumberFormatException e) {
            // no int value submitted, throw exception
            throw new CmsIllegalArgumentException(Messages.get().container(Messages.ERR_NO_INT_ENTERED_0), e);
        }
        if (versions < 0) {
            // version value too low, throw exception
            throw new CmsIllegalArgumentException(Messages.get().container(Messages.ERR_NO_POSITIVE_INT_0));
        }
        int versionsDeleted = 0;
        try {
            versionsDeleted = Integer.parseInt(paramVersionsDeleted);
        } catch (NumberFormatException e) {
            // no int value submitted, throw exception
            throw new CmsIllegalArgumentException(Messages.get().container(Messages.ERR_NO_INT_ENTERED_0), e);
        }
        if (versionsDeleted < 0) {
            // version value too low, throw exception
            throw new CmsIllegalArgumentException(Messages.get().container(Messages.ERR_NO_POSITIVE_INT_0));
        }


        OpenCms.getSystemInfo().setVersionHistorySettings(enabled, versions, versionsDeleted);
        OpenCms.writeConfiguration(CmsSystemConfiguration.class);

        return true;
    }

}
