/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/workplace/tools/history/Attic/CmsAdminHistoryClear.java,v $
 * Date   : $Date: 2006/09/22 15:17:06 $
 * Version: $Revision: 1.18 $
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

import org.opencms.i18n.CmsMessages;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsIllegalArgumentException;
import org.opencms.main.OpenCms;
import org.opencms.widgets.CmsCalendarWidget;
import org.opencms.workplace.CmsReport;
import org.opencms.workplace.CmsWorkplaceSettings;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

/**
 * Provides methods for the history clear dialog.<p> 
 * 
 * The following files use this class:
 * <ul>
 * <li>/system/workplace/administration/history/clearhistory/index.jsp
 * </ul>
 * <p>
 *
 * @author  Andreas Zahner 
 * 
 * @version $Revision: 1.18 $ 
 * 
 * @since 6.0.0 
 */
public class CmsAdminHistoryClear extends CmsReport {

    /** Value for the action: clear history. */
    public static final int ACTION_SAVE_EDIT = 300;

    /** Request parameter value for the action: clear history. */
    public static final String DIALOG_SAVE_EDIT = "saveedit";

    /** The dialog type. */
    public static final String DIALOG_TYPE = "historyclear";

    /**
     * Public constructor with JSP action element.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsAdminHistoryClear(CmsJspActionElement jsp) {

        super(jsp);
    }

    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */
    public CmsAdminHistoryClear(PageContext context, HttpServletRequest req, HttpServletResponse res) {

        this(new CmsJspActionElement(context, req, res));
    }

    /**
     * Performs the change of the history settings, this method is called by the JSP.<p>
     * 
     * @throws JspException if something goes wrong
     */
    public void actionEdit() throws JspException {

        // save initialized instance of this class in request attribute for included sub-elements
        getJsp().getRequest().setAttribute(SESSION_WORKPLACE_CLASS, this);

        switch (getAction()) {
            case ACTION_REPORT_UPDATE:
                setParamAction(REPORT_UPDATE);
                getJsp().include(FILE_REPORT_OUTPUT);
                break;
            case ACTION_REPORT_END:
                actionCloseDialog();
                break;
            case ACTION_REPORT_BEGIN:
            case ACTION_SAVE_EDIT:
            default:
                Map params = new HashMap();
                try {
                    params = getBackupParams();
                    CmsAdminHistoryClearThread thread = new CmsAdminHistoryClearThread(getCms(), params);
                    setParamAction(REPORT_BEGIN);
                    setParamThread(thread.getUUID().toString());
                    getJsp().include(FILE_REPORT_OUTPUT);
                } catch (Throwable e) {
                    // error setting history values, show error dialog
                    includeErrorpage(this, e);
                }
                break;
        }
    }

    /**
     * Builds the HTML for the history settings input form.<p>
     * 
     * @return the HTML code for the history settings input form
     */
    public String buildClearForm() {

        CmsMessages messages = Messages.get().getBundle(getLocale());
        StringBuffer retValue = new StringBuffer(512);
        int maxVersions = OpenCms.getSystemInfo().getVersionHistoryMaxCount();

        // append settings info or disabled message if history is disabled
        retValue.append(dialogBlockStart(messages.key(Messages.GUI_LABEL_ADMNIN_HISTORY_SETTINGS_0)));
        if (isHistoryEnabled()) {
            retValue.append(messages.key(Messages.GUI_INPUT_HISTORY_CLEAR_VERSIONINFO_1, new Object[] {new Integer(
                maxVersions)}));
            retValue.append("<br>");
            retValue.append(messages.key(Messages.GUI_INPUT_HISTORY_CLEAR_SELECTINFO_0));
        } else {
            retValue.append(messages.key(Messages.GUI_INPUT_HISTORY_CLEAR_DISABLEDINFO_0));
        }
        retValue.append(dialogBlockEnd());
        retValue.append(dialogSpacer());

        // append input fields if history is enabled
        if (isHistoryEnabled()) {
            retValue.append("<table border=\"0\">\n");
            retValue.append("<tr>\n");
            retValue.append("<td>" + messages.key(Messages.GUI_INPUT_HISTORY_CLEAR_NUMBER_0) + "</td>\n");
            retValue.append("<td colspan=\"2\">" + buildSelectVersions(null) + "</td>\n");
            retValue.append("</tr>\n");
            retValue.append("<tr><td colspan=\"3\">&nbsp;</td></tr>\n");
            retValue.append("<tr>\n");
            retValue.append("<td>" + messages.key(Messages.GUI_INPUT_HISTORY_CLEAR_DATE_0) + "</td>\n");
            retValue.append("<td>");
            retValue.append("<input type=\"text\" name=\"date\" id=\"date\">");
            retValue.append("</td>\n<td>");
            retValue.append("<img src=\"" + getSkinUri() + "buttons/calendar.png\" id=\"triggercalendar\" ");
            retValue.append("alt=\""
                + messages.key(Messages.GUI_CALENDAR_INPUT_CHOOSEDATE_0)
                + "\" title=\""
                + messages.key(Messages.GUI_CALENDAR_INPUT_CHOOSEDATE_0)
                + "\">");
            retValue.append("</td>\n");
            retValue.append("</tr>\n");
            retValue.append("</table>\n");
        }

        return retValue.toString();
    }

    /**
     * Build the HTML code for a select box of versions to keep.<p>
     * 
     * @param attributes optional additional attributes of the select tag
     * @return the HTML code for a select box of versions
     */
    public String buildSelectVersions(String attributes) {

        return buildSelectNumbers("versions", attributes, 0, OpenCms.getSystemInfo().getVersionHistoryMaxCount());
    }

    /**
     * Creates the HTML JavaScript and stylesheet includes required by the calendar for the head of the page.<p>
     * 
     * @return the necessary HTML code for the js and stylesheet includes
     * 
     * @deprecated use {@link CmsCalendarWidget#calendarIncludes(java.util.Locale)}, this is just here so that old JSP still work
     */
    public String calendarIncludes() {

        return CmsCalendarWidget.calendarIncludes(getLocale());
    }

    /**
     * Generates the HTML to initialize the JavaScript calendar element on the end of a page.<p>
     * 
     * @param inputFieldId the ID of the input field where the date is pasted to
     * @param triggerButtonId the ID of the button which triggers the calendar
     * @param align initial position of the calendar popup element
     * @param singleClick if true, a single click selects a date and closes the calendar, otherwise calendar is closed by doubleclick
     * @param weekNumbers show the week numbers in the calendar or not
     * @param mondayFirst show monday as first day of week
     * @param dateStatusFunc name of the function which determines if/how a date should be disabled
     * @return the HTML code to initialize a calendar poup element
     * 
     * @deprecated use {@link CmsCalendarWidget#calendarInit(org.opencms.i18n.CmsMessages, String, String, String, boolean, boolean, boolean, String, boolean)}, this is just here so that old JSP still work
     */
    public String calendarInit(
        String inputFieldId,
        String triggerButtonId,
        String align,
        boolean singleClick,
        boolean weekNumbers,
        boolean mondayFirst,
        String dateStatusFunc) {

        return CmsCalendarWidget.calendarInit(
            getMessages(),
            inputFieldId,
            triggerButtonId,
            align,
            singleClick,
            weekNumbers,
            mondayFirst,
            dateStatusFunc,
            false);
    }

    /**
     * Returns true if the version history is enabled, otherwise false.<p>
     * 
     * @return true if the version history is enabled, otherwise false
     */
    public boolean isHistoryEnabled() {

        return OpenCms.getSystemInfo().isVersionHistoryEnabled();
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
            if (isHistoryEnabled()) {
                // history is enabled, start clearing
                setAction(ACTION_SAVE_EDIT);
            } else {
                // history is disabled, return to admin view
                setAction(ACTION_CANCEL);
            }
        } else if (REPORT_UPDATE.equals(getParamAction())) {
            setAction(ACTION_REPORT_UPDATE);
        } else if (REPORT_BEGIN.equals(getParamAction())) {
            setAction(ACTION_REPORT_BEGIN);
        } else if (REPORT_END.equals(getParamAction())) {
            setAction(ACTION_REPORT_END);
        } else if (DIALOG_CANCEL.equals(getParamAction())) {
            setAction(ACTION_CANCEL);
        } else {
            // set the default action               
            setAction(ACTION_DEFAULT);
            setParamTitle(Messages.get().getBundle(getLocale()).key(Messages.GUI_LABEL_ADMIN_HISTORY_CLEAR_0));
        }
    }

    /**
     * Creates the HTML code for a select box with integer values.<p>
     * 
     * @param fieldName the name of the select box
     * @param attributes the optional tag attributes
     * @param startValue the start integer value for the options
     * @param endValue the end integer value for the options
     * @return the HTML code for the select box
     */
    private String buildSelectNumbers(String fieldName, String attributes, int startValue, int endValue) {

        StringBuffer retValue = new StringBuffer(512);

        retValue.append("<select name=\"" + fieldName + "\"");
        if (attributes != null) {
            retValue.append(" " + attributes);
        }
        retValue.append(">\n");
        retValue.append("\t<option value=\"\" selected=\"selected\">"
            + Messages.get().getBundle(getLocale()).key(Messages.GUI_INPUT_HISTORY_CLEAR_SELECT_0)
            + "</option>\n");
        for (int i = startValue; i <= endValue; i++) {
            retValue.append("\t<option value=\"" + i + "\">" + i + "</option>\n");
        }
        retValue.append("</select>\n");

        return retValue.toString();
    }

    /**
     * Returns the necessary parameters to perform the backup deletion.<p>
     * 
     * @return a map with necessary parameters for the deleteBackups method
     * @throws CmsIllegalArgumentException if something goes wrong
     */
    private Map getBackupParams() throws CmsIllegalArgumentException {

        HttpServletRequest request = getJsp().getRequest();
        Map parameterMap = new HashMap();

        // get the delete information from the request parameters
        String paramVersions = request.getParameter("versions");
        String paramDate = request.getParameter("date");

        // check the submitted values        
        int versions = 0;
        long timeStamp = 0;
        boolean useVersions = false;
        try {
            versions = Integer.parseInt(paramVersions);
            useVersions = true;
        } catch (NumberFormatException e) {
            // no int value submitted, check date fields
            try {
                timeStamp = CmsCalendarWidget.getCalendarDate(getMessages(), paramDate, false);
            } catch (ParseException ex) {
                // no date values submitted, throw exception

                throw new CmsIllegalArgumentException(
                    Messages.get().container(Messages.ERR_INVALID_DATE_1, paramDate),
                    ex);
            }
        }

        // set the timeStamp one day to the future to delete versions
        if (useVersions) {
            timeStamp = System.currentTimeMillis() + 86400000;
        }
        if (DEBUG) {
            System.err.println("Versions: " + versions + "\nDate: " + timeStamp);
        }
        // add the correct values to the parameter map
        parameterMap.put("timeStamp", String.valueOf(timeStamp));
        parameterMap.put("versions", String.valueOf(versions));

        if (DEBUG) {
            System.err.println("Done");
        }
        return parameterMap;
    }

}
