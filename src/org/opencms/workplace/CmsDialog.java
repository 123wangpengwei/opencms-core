/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/CmsDialog.java,v $
 * Date   : $Date: 2003/09/11 12:04:49 $
 * Version: $Revision: 1.20 $
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
 *  
*/
package org.opencms.workplace;

import com.opencms.core.CmsException;
import com.opencms.file.CmsResource;
import com.opencms.flex.jsp.CmsJspActionElement;
import com.opencms.flex.util.CmsStringSubstitution;
import com.opencms.workplace.I_CmsWpConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

/**
 * Provides methods for building the dialog windows of OpenCms.<p> 
 * 
 * @author  Andreas Zahner (a.zahner@alkacon.com)
 * @version $Revision: 1.20 $
 * 
 * @since 5.1
 */
public class CmsDialog extends CmsWorkplace {

    public static final int ACTION_DEFAULT = 0;
    public static final int ACTION_CONFIRMED = 1;
    public static final int ACTION_WAIT = 2;
    
    // note: action values 90 - 99 are reserved for reports
    public static final int ACTION_REPORT_BEGIN  = 90;
    public static final int ACTION_REPORT_UPDATE = 91;
    public static final int ACTION_REPORT_END    = 92;
    
    public static final String REPORT_BEGIN  = "reportbegin";
    public static final String REPORT_UPDATE = "reportupdate";
    public static final String REPORT_END    = "reportend";    
    
    public static final int BUTTON_OK = 0;
    public static final int BUTTON_CANCEL = 1;
    public static final int BUTTON_CLOSE = 2;
    public static final int BUTTON_ADVANCED = 3;
    
    public static final String DIALOG_CONFIRMED = "confirmed";
    public static final String DIALOG_WAIT = "wait";
        
    public static final String PARAM_ACTION = "action";
    public static final String PARAM_FILE = "file";
    public static final String PARAM_TARGET = "target";
    public static final String PARAM_DIALOGTYPE = "dialogtype";
    public static final String PARAM_ERRORSTACK = "errorstack";
    public static final String PARAM_TITLE = "title";
    public static final String PARAM_MESSAGE = "message";
    public static final String PARAM_THREAD = "thread";

    private String m_paramAction;
    private String m_paramResource;
    private String m_paramDialogtype;
    private String m_paramErrorstack;
    private String m_paramMessage;
    private String m_paramTitle;

    private int m_action;  
    
    private String m_paramReasonSuggestion = null;  
   
    /**
     * Public constructor with JSP action element.<p>
     * 
     * @param jsp an initialized JSP action element
     */    
    public CmsDialog(CmsJspActionElement jsp) {
        super(jsp);
    }
       
    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */    
    public CmsDialog(PageContext context, HttpServletRequest req, HttpServletResponse res) {
        this(new CmsJspActionElement(context, req, res));
    }
    
    /**
     * Returns an initialized CmsDialog instance that is read from the request attributes.<p>
     * 
     * This method is used by dialog elements. 
     * The dialog elements do not initialize their own workplace class, 
     * but use the initialized instance of the "master" class.
     * This is required to ensure that parameters of the "master" class
     * can properly be kept on the dialog elements.<p>
     * 
     * To prevent null pointer exceptions, an empty dialog is returned if 
     * nothing is found in the request attributes.<p>
     *  
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     * @return an initialized CmsDialog instance that is read from the request attributes
     */
    public static CmsDialog initCmsDialog(PageContext context, HttpServletRequest req, HttpServletResponse res) {
        CmsDialog wp = (CmsDialog)req.getAttribute(CmsWorkplace.C_SESSION_WORKPLACE_CLASS);
        if (wp == null) {
            // ensure that we don't get null pointers if the page is directly called
            wp = new CmsDialog(new CmsJspActionElement(context, req, res));
        }           
        return wp;
    }    

    /**
     * @see org.opencms.workplace.CmsWorkplace#initWorkplaceRequestValues(org.opencms.workplace.CmsWorkplaceSettings, javax.servlet.http.HttpServletRequest)
     */
    protected void initWorkplaceRequestValues(CmsWorkplaceSettings settings, HttpServletRequest request) {
    }
   
    /**
     * Returns the action value.<p>
     * 
     * The action value is used on JSP pages to select the proper action 
     * in a large "switch" statement.<p>
     * 
     * @return the action value
     */ 
    public int getAction() {
        return m_action;
    }   
    
    /**
     * Sets the action value.<p>
     * 
     * @param value the action value
     */
    protected void setAction(int value)  {
        m_action = value;
    }
        
    /**
     * Returns the value of the action parameter, 
     * or null if this parameter was not provided.<p>
     * 
     * The action parameter is very important, 
     * it will select the dialog action to perform.
     * The value of the {@link #getAction()} method will be
     * initilized form the action parameter.<p>
     * 
     * @return the value of the action parameter
     */    
    public String getParamAction() {
        return m_paramAction;
    }
    
    /**
     * Sets the value of the action parameter.<p>
     * 
     * @param value the value to set
     */
    public void setParamAction(String value) {
        m_paramAction = value;
    }    

    /**
     * Returns the value of the file parameter, 
     * or null if this parameter was not provided.<p>
     * 
     * The file parameter selects the file on which the dialog action
     * is to be performed.<p>
     * 
     * @return the value of the file parameter
     */    
    public String getParamResource() {
        return m_paramResource;
    }
    
    /**
     * Sets the value of the file parameter.<p>
     * 
     * @param value the value to set
     */
    public void setParamResource(String value) {
        m_paramResource = value;
    }
    
    /**
     * Returns the value of the dialogtype parameter, 
     * or null if this parameter was not provided.<p>
     * 
     * This parameter is very important. 
     * It must match to the localization keys,
     * e.g. "copy" for the copy dialog.<p>
     * 
     * This parameter must be set manually by the subclass during 
     * first initialization.<p> 
     * 
     * @return the value of the dialogtype parameter
     */
    public String getParamDialogtype() {
        return m_paramDialogtype;
    }
    
    /**
     * Sets the value of the dialogtype parameter.<p>
     * 
     * @param value the value to set
     */
    public void setParamDialogtype(String value) {
        m_paramDialogtype = value;
    }
    
    /**
     * Returns the value of the title parameter, 
     * or null if this parameter was not provided.<p>
     * 
     * This parameter is used to build the title 
     * of the dialog. It is a parameter so that the title 
     * can be passed to included elements.<p>
     * 
     * @return the value of the title parameter
     */
    public String getParamTitle() {
        return m_paramTitle;
    }
    
    /**
     * Sets the value of the title parameter.<p>
     * 
     * @param value the value to set
     */
    public void setParamTitle(String value) {
        m_paramTitle = value;
    }
    
    /**
     * Returns the value of the message parameter, 
     * or null if this parameter was not provided.<p>
     * 
     * The message parameter is used on dialogs to 
     * show any text message.<p>
     * 
     * @return the value of the message parameter
     */
    public String getParamMessage() {
        return m_paramMessage;
    }
    
    /**
     * Sets the value of the message parameter.<p>
     * 
     * @param value the value to set
     */
    public void setParamMessage(String value) {
        m_paramMessage = value;
    }
    
    /**
     * Returns the value of the reasonSuggestion parameter.<p>
     *  
     * @return the value of the reasonSuggestion parameter
     */
    public String getParamReasonSuggestion() {
        return m_paramReasonSuggestion;
    }
    
    /**
     * Sets the value of the reasonSuggestion parameter.<p>
     * 
     * @param value the value of the reasonSuggestion parameter
     */
    public void setParamReasonSuggestion(String value) {
        m_paramReasonSuggestion = value;
    }
    
    /**
     * Returns the default error suggestion String for error messages.<p>
     * 
     * @return the default error suggestion String
     */
    public String getErrorSuggestionDefault() {
        return key("error.reason." + getParamDialogtype()) + "<br>\n" + key("error.suggestion." + getParamDialogtype()) + "\n";        
    }
    
    
    /**
     * Returns the value of the errorstack parameter, 
     * or null if this parameter was not provided.<p>
     * 
     * The error stack is used by the common error screen 
     * that is displayed if an error occurs.<p>
     * 
     * @return the value of the errorstack parameter
     */    
    public String getParamErrorstack()  {
        return CmsStringSubstitution.substitute(m_paramErrorstack, "\n", "\n<br>");
    }
    
    /**
     * Sets the value of the errorstack parameter,
     * this should be a stack trace as String.<p>
     * 
     * @param value the value to set
     */
    public void setParamErrorstack(String value) {
        m_paramErrorstack = value;
    }

    /**
     * Returns the http URI of the current dialog, to be used
     * as value for the "action" attribute of a html form.<p>
     * 
     * @return the http URI of the current dialog
     */
    public String getDialogUri() {
        return getJsp().link(getJsp().getRequestContext().getUri());
    }
    
    /**
     * Returns the default action for a "cancel" button.<p>
     * 
     * Always use this value, do not write anything directly in the html page.<p>
     * 
     * @return the default action for a "cancel" button
     */
    public String buttonActionCancel() {
        if (getSettings().isViewAdministration()) {
            return "onclick=\"location.href='"+ getJsp().link(getAdministrationBackLink()) + "';\"";
        }
        return "onClick=\"location.href='" + getExplorerFileListFullUri() + "';\"";
    }
    
    /**
     * Returns the link URL to get back one folder in the administration view.<p>
     * 
     * @return the link URL to get back one folder in the administration view
     */
    protected String getAdministrationBackLink() {
        return I_CmsWpConstants.C_VFS_PATH_WORKPLACE + "action/administration_content_top.html" + "?sender=" + CmsResource.getParent(getJsp().getRequestContext().getFolderUri());
    }

    /**
     * Returns the default action for a "close" button.<p>
     * 
     * Always use this value, do not write anything directly in the html page.<p>
     * 
     * @return the default action for a "close" button
     */    
    public String buttonActionClose() {
        return buttonActionCancel();
    }    

    /**
     * Returns the start html for the outer dialog window border.<p>
     * 
     * @return the start html for the outer dialog window border
     */                
    public String dialogStart() {
        return dialog(HTML_START, null);
    }
    
    /**
     * Returns the start html for the outer dialog window border.<p>
     * 
     * @param attributes optional html attributes to insert
     * @return the start html for the outer dialog window border
     */                
    public String dialogStart(String attributes) {
        return dialog(HTML_START, attributes);
    }
    
    /**
     * Returns the end html for the outer dialog window border.<p>
     * 
     * @return the end html for the outer dialog window border
     */                
    public String dialogEnd() {
        return dialog(HTML_END, null);
    }
    
    /**
     * Builds the outer dialog window border.<p>
     * 
     * @param segment the HTML segment (START / END)
     * @param attributes optional additional attributes for the opening dialog table
     * @return a dialog window start / end segment
     */
    public String dialog(int segment, String attributes) {
        if (segment == HTML_START) {
            StringBuffer result = new StringBuffer(256);
            result.append("<table class=\"dialog\" cellpadding=\"0\" cellspacing=\"0\"");
            if (attributes != null) {
                result.append(" ");
                result.append(attributes);
            }
            result.append("<tr><td>\n<table class=\"dialogbox\" cellpadding=\"0\" cellspacing=\"0\">\n");
            result.append("<tr><td>\n");
            return result.toString();
        } else {
            return "</td></tr></table>\n</td></tr></table>\n<p>&nbsp;</p>\n";
        }
    }
    
    /**
     * Returns the start html for the content area of the dialog window.<p>
     * 
     * @param title the title for the dialog
     * @return the start html for the content area of the dialog window
     */                
    public String dialogContentStart(String title) {
        return dialogContent(HTML_START, title);
    }

    /**
     * Returns the end html for the content area of the dialog window.<p>
     * 
     * @return the end html for the content area of the dialog window
     */                
    public String dialogContentEnd() {
        return dialogContent(HTML_END, null);
    }
    
    /**
     * Builds the content area of the dialog window.<p>
     * 
     * @param segment the HTML segment (START / END)
     * @param title the title String for the dialog window
     * @return a content area start / end segment
     */
    public String dialogContent(int segment, String title) {
        if (segment == HTML_START) {
            StringBuffer result = new StringBuffer(512);
            // null title is ok, we always want the title headline
            result.append("<div class=\"dialoghead\" unselectable=\"on\">");
            result.append(title);
            result.append("</div>");
            result.append("<div class=\"dialogcontent\" unselectable=\"on\">\n");
            result.append("<!-- dialogcontent start -->\n");
            return result.toString();
        } else {
            return "<!-- dialogcontent end -->\n</div>";
        }
    }
    
    /**
     * Builds the end HTML for a block with 3D border in the dialog content area.<p>
     * 
     * @return 3D block start / end segment
     */
    public String dialogBlockEnd() {
        return dialogBlock(HTML_END, null, false);
    }
    
    /**
     * Builds the start HTML for a block with 3D border and optional subheadline in the dialog content area.<p>
     * 
     * @param headline the headline String for the block
     * @return 3D block start / end segment
     */
    public String dialogBlockStart(String headline) {
        return dialogBlock(HTML_START, headline, false);
    }
    
    /**
     * Builds a block with 3D border and optional subheadline in the dialog content area.<p>
     * 
     * @param segment the HTML segment (START / END)
     * @param headline the headline String for the block
     * @param error if true, an error block will be created
     * @return 3D block start / end segment
     */
    public String dialogBlock(int segment, String headline, boolean error) {
        if (segment == HTML_START) {
            StringBuffer retValue = new StringBuffer(512);
            String errorStyles = "";
            if (error) {
                errorStyles = " dialogerror textbold";
            }
            retValue.append("<!-- 3D block start -->\n");
            if (headline != null && !"".equals(headline)) {
                retValue.append("<div class=\"dialogblockborder dialogblockborderheadline\" unselectable=\"on\">\n");
                retValue.append("<div class=\"dialogblock"+errorStyles+"\" unselectable=\"on\">\n");
                retValue.append("<span class=\"dialogblockhead"+errorStyles+"\" unselectable=\"on\">");
                retValue.append(headline);
                retValue.append("</span>\n");
            } else {
                retValue.append("<div class=\"dialogblockborder\" unselectable=\"on\">\n");
                retValue.append("<div class=\"dialogblock"+errorStyles+"\" unselectable=\"on\">\n");
            }
            return retValue.toString();
        } else {
            return "</div>\n</div>\n<!-- 3D block end -->";
        }
    }
    
    /**
     * Builds the start of a white box in the dialog content area.<p>
     * 
     * @return the white box start segment
     */
    public String dialogWhiteBoxStart() {
        return dialogWhiteBox(HTML_START);
    }
    
    /**
     * Builds the end of a white box in the dialog content area.<p>
     * 
     * @return the white box end segment
     */
    public String dialogWhiteBoxEnd() {
        return dialogWhiteBox(HTML_END);
    }
    
    /**
     * Builds a white box in the dialog content area.<p>
     * 
     * @param segment the HTML segment (START / END)
     * @return the white box start / end segment
     */
    public String dialogWhiteBox(int segment) {
        if (segment == HTML_START) {
            return "<!-- white box start -->\n"
                + "<div class=\"dialoginnerboxborder\">\n"
                + "<div class=\"dialoginnerbox\" unselectable=\"off\">\n";
        } else {
            return "</div>\n</div>\n<!-- white box end -->\n";
        }        
    }
    
    /**
     * Builds the start of the button row under the dialog content area without the buttons.<p>
     * 
     * @return the button row start segment
     */
    public String dialogButtonRowStart() {
        return dialogButtonRow(HTML_START);
    }
    
    /**
     * Builds the end of the button row under the dialog content area without the buttons.<p>
     * 
     * @return the button row end segment
     */
    public String dialogButtonRowEnd() {
        return dialogButtonRow(HTML_END);
    }
    
    /**
     * Builds the button row under the dialog content area without the buttons.<p>
     * 
     * @param segment the HTML segment (START / END)
     * @return the button row start / end segment
     */
    public String dialogButtonRow(int segment) {
        if (segment == HTML_START) {
            return "<!-- button row start -->\n<div class=\"dialogbuttons\" unselectable=\"on\">\n";
        } else {
            return "</div>\n<!-- button row end -->\n";
        }
    }
    
    /**
     * Builds the html for the button row under the dialog content area, including buttons.<p>
     * 
     * @param buttons array of constants of which buttons to include in the row
     * @param attributes array of Strings for additional button attributes
     * @return the html for the button row under the dialog content area, including buttons
     */
    public String dialogButtonRow(int[] buttons, String[] attributes) {
        StringBuffer result = new StringBuffer(256);
        result.append(dialogButtonRow(HTML_START));
        for (int i=0; i<buttons.length; i++)  {
            String attribute = attributes[i];
            String curAttributes = writeAttribute(attribute);
            switch (buttons[i]) {
                case BUTTON_OK:
                    result.append("<input name=\"ok\" type=\"submit\" value=\"");
                    result.append(key("button.ok"));
                    result.append("\" class=\"dialogbutton\"");
                    result.append(curAttributes);
                    result.append(">\n");
                    break;
                case BUTTON_CANCEL:
                    result.append("<input name=\"cancel\" type=\"button\" value=\"");
                    result.append(key("button.cancel")+"\"");
                    if ("".equals(curAttributes)) {
                        result.append(" ");
                        result.append(buttonActionCancel());
                    }
                    result.append(" class=\"dialogbutton\"");
                    result.append(curAttributes);
                    result.append(">\n");
                    break;
                case BUTTON_CLOSE:
                    result.append("<input name=\"close\" type=\"button\" value=\"");
                    result.append(key("button.close")+"\"");
                    if ("".equals(curAttributes)) {
                        result.append(" ");
                        result.append(buttonActionClose());
                    }
                    result.append(" class=\"dialogbutton\"");
                    result.append(curAttributes);
                    result.append(">\n");
                    break;
                case BUTTON_ADVANCED:
                    result.append("<input name=\"advanced\" type=\"button\" value=\"");
                    result.append(key("button.advanced")+"\"");
                    result.append(" class=\"dialogbutton\"");
                    result.append(curAttributes);
                    result.append(">\n");
                    break;
                default:
                    // not a valid button code, just insert a warning in the HTML
                    result.append("<!-- invalid button code: ");
                    result.append(buttons[i]);
                    result.append(" -->\n");
            }
        }
        
        result.append(dialogButtonRow(HTML_END));
        return result.toString();
    }
    
    /**
     * Builds a button row with an "ok" and a "cancel" button.<p>
     * 
     * @return the button row 
     */
    public String dialogButtonRowOkCancel() {
        return dialogButtonRow(new int[] {BUTTON_OK, BUTTON_CANCEL}, new String[2]);
    }
    
    /**
     * Builds a button row with an "ok" and a "cancel" button.<p>
     * 
     * @param okAttributes additional attributes for the "ok" button
     * @param cancelAttributes additional attributes for the "cancel" button
     * @return the button row 
     */
    public String dialogButtonRowOkCancel(String okAttributes, String cancelAttributes) {
        return dialogButtonRow(new int[] {BUTTON_OK, BUTTON_CANCEL}, new String[] {okAttributes, cancelAttributes});
    }
    
    /**
     * Builds a button row with an "ok", a "cancel" and an "advanced" button.<p>
     * 
     * @param okAttributes additional attributes for the "ok" button
     * @param cancelAttributes additional attributes for the "cancel" button
     * @param advancedAttributes additional attributes for the "advanced" button
     * @return the button row 
     */
    public String dialogButtonRowOkCancelAdvanced(String okAttributes, String cancelAttributes, String advancedAttributes) {
        return dialogButtonRow(new int[] {BUTTON_OK, BUTTON_CANCEL, BUTTON_ADVANCED}, new String[] {okAttributes, cancelAttributes, advancedAttributes});
    }

    /**
     * Builds a button row with a single "cancel" button.<p>
     * 
     * @return the button row 
     */
    public String dialogButtonRowCancel() {
        return dialogButtonRow(new int[] {BUTTON_CANCEL}, new String[1]);
    }

    /**
     * Builds a button row with a single "ok" button.<p>
     * 
     * @return the button row 
     */
    public String dialogButtonRowOk() {
        return dialogButtonRow(new int[] {BUTTON_OK}, new String[1]);
    }
    
    /**
     * Builds a button row with a single "ok" button.<p>
     * 
     * @param okAttribute additional attributes for the "ok" button
     * @return the button row 
     */
    public String dialogButtonRowOk(String okAttribute) {
        return dialogButtonRow(new int[] {BUTTON_OK}, new String[] {okAttribute});
    }
    
    /**
     * Builds a button row with a single "close" button.<p>
     * 
     * @return the button row 
     */    
    public String dialogButtonRowClose() {
        return dialogButtonRow(new int[] {BUTTON_CLOSE}, new String[1]);
    }
    
    /**
     * Builds a button row with a single "close" button.<p>
     * 
     * @param closeAttribute additional attributes for the "close" button
     * @return the button row 
     */    
    public String dialogButtonRowClose(String closeAttribute) {
        return dialogButtonRow(new int[] {BUTTON_CLOSE}, new String[] {closeAttribute});
    }
        
    /**
     * Builds a subheadline in the dialog content area.<p>
     * 
     * @param headline the desired headline string
     * @return a subheadline element
     */
    public String dialogSubheadline(String headline) {
        StringBuffer retValue = new StringBuffer(128);
        retValue.append("<div class=\"dialogsubheader\" unselectable=\"on\">");
        retValue.append(headline);
        retValue.append("</div>\n");
        return retValue.toString();
    }
    
    /**
     * Builds a horizontal separator line in the dialog content area.<p>
     * 
     * @return a separator element
     */
    public String dialogSeparator() {
        return "<div class=\"dialogseparator\" unselectable=\"on\"></div>";
    }
    
    /**
     * Builds a space between two elements in the dialog content area.<p>
     * 
     * @return a space element
     */
    public String dialogSpacer() {
        return "<div class=\"dialogspacer\" unselectable=\"on\">&nbsp;</div>";
    }
    
    /**
     * Builds the start of a dialog line without break (display: block).<p>
     * 
     * @return the row start segment
     */
    public String dialogRowStart() {
        return dialogRow(HTML_START);
    }
    
    /**
     * Builds the end of a dialog line without break (display: block).<p>
     * 
     * @return the row end segment
     */
    public String dialogRowEnd() {
        return dialogRow(HTML_END);
    }
    
    /**
     * Builds a dialog line without break (display: block).<p>
     * 
     * @param segment the HTML segment (START / END)
     * @return a row start / end segment
     */
    public String dialogRow(int segment) {
        if (segment == HTML_START) {
            return "<div class=\"dialogrow\">";
        } else {
            return "</div>\n";
        }
    }
    
    /**
     * Checks the String parameter and returns it if its not null or empty.<p>
     * 
     * @param attribute the attribute String 
     * @return the attribute if the String is not empty
     */
    private String writeAttribute(String attribute) {
        if (attribute != null && !"".equals(attribute)) {
            return " "+attribute;
        } else {
            return ""; 
        }
    }
    
    /**
     * Gets a formatted file state string.<p>
     * 
     * @param file the CmsResource
     * @return formatted state string
     */
    public String getState(CmsResource file) {  
        //if (file.inProject(getCms().getRequestContext().currentProject())) {
        if (getCms().isInsideCurrentProject(file)) {
            int state = file.getState();
            return key("explorer.state" + state);
        } else {
            return key("explorer.statenip");
        }
    }
    
    /**
     * Gets a formatted file state string.<p>
     * 
     * @return formatted state string
     * @throws CmsException if something goes wrong
     */
    public String getState() throws CmsException { 
        if (getParamResource() != null) {        
            CmsResource file = getCms().readFileHeader(getParamResource());
            return getState(file);
        } else  {
            return "+++ file parameter not found +++";
        }
    }

    /**
     * Builds the start html of the page, including setting of DOCTYPE and 
     * inserting a header with the content-type.<p>
     * 
     * This overloads the default method of the parent class.<p>
     * 
     * @param helpUrl the key for the online help to include on the page
     * @return the start html of the page
     */
    public String htmlStart(String helpUrl) {
        return pageHtml(HTML_START, helpUrl);
    }
    
    /**
     * Builds the start html of the page, including setting of DOCTYPE and 
     * inserting a header with the content-type.<p>
     * 
     * This overloads the default method of the parent class.<p>
     * 
     * @return the start html of the page
     */
    public String htmlStart() {
        return pageHtml(HTML_START, null);
    }

    /**
     * Builds the start html of the page, including setting of DOCTYPE and 
     * inserting a header with the content-type.<p>
     * 
     * This overloads the default method of the parent class.<p>
     * 
     * @param segment the HTML segment (START / END)
     * @param helpUrl the url for the online help to include on the page
     * @return the start html of the page
     */
    public String pageHtml(int segment, String helpUrl) {        
        if (segment == HTML_START) {
            StringBuffer result = new StringBuffer(super.pageHtml(segment, null));
            if (getSettings().isViewExplorer()) {
                result.append("<script type=\"text/javascript\" src=\"");
                result.append(getSkinUri());
                result.append("files/explorer.js\"></script>\n");
            }
            if (helpUrl != null) {                result.append("<script type=\"text/javascript\">\n");
                result.append("top.head.helpUrl='");
                result.append(helpUrl);
                result.append("';\n</script>\n");
            }
            return result.toString();
        } else {
            return super.pageHtml(segment, null);
        }
    }
}
