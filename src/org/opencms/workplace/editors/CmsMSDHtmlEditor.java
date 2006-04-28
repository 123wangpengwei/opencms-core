/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/editors/Attic/CmsMSDHtmlEditor.java,v $
 * Date   : $Date: 2006/04/28 14:00:46 $
 * Version: $Revision: 1.16.4.2 $
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

import org.opencms.i18n.CmsEncoder;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsStringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates the output for editing a CmsDefaultPage with the MS DHTML control editor.<p> 
 * 
 * The following editor uses this class:
 * <ul>
 * <li>/editors/msdhtml/editor.jsp
 * </ul>
 * <p>
 *
 * @author  Andreas Zahner 
 * 
 * @version $Revision: 1.16.4.2 $ 
 * 
 * @since 6.0.0 
 */
public class CmsMSDHtmlEditor extends CmsSimplePageEditor {

    /** Constant for the editor type, must be the same as the editors subfolder name in the VFS. */
    public static final String EDITOR_TYPE = "msdhtml";

    /** Option localization keys for editor view select boxes. */
    public static final String[] SELECTBOX_EDITORVIEWKEYS = {
        Messages.GUI_EDITOR_MODE_WYSIWYG_0,
        Messages.GUI_EDITOR_MODE_SOURCE_0};

    /** Option values for editor view select boxes. */
    public static final String[] SELECTBOX_EDITORVIEWS = {"edithtml", "edit"};

    /** values for editor view select boxes. */
    public static final int[] SELECTBOX_EDITORVIEWS_ALLOWED = {3, 2};

    /** regex pattern to find all src attribs in img tags, plus all href attribs in anchor tags. */
    private static final Pattern REGEX_LINKS = Pattern.compile(
        "<(img|a)(\\s+)(.*?)(src|href)=(\"|\')(.*?)(\"|\')(.*?)>",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    /**
     * Public constructor.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsMSDHtmlEditor(CmsJspActionElement jsp) {

        super(jsp);
    }

    /**
     * Builds the html String for the editor views available in the editor screens.<p>
     * 
     * @param attributes optional attributes for the &lt;select&gt; tag
     * @return the html for the editorview selectbox
     */
    public final String buildSelectViews(String attributes) {

        Vector names = new Vector();
        Vector values = new Vector();
        // get the available views fron the constant
        String[] contents = SELECTBOX_EDITORVIEWS;
        for (int i = 0; i < contents.length; i++) {
            String value = contents[i];
            values.addElement(value);
            String s = keyDefault(SELECTBOX_EDITORVIEWKEYS[i], value);
            names.addElement(s);
        }
        int browserId = 0;
        int loop = 1;
        int allowedEditors = SELECTBOX_EDITORVIEWS_ALLOWED[browserId];
        if ("script".equals(getParamElementname())) {
            allowedEditors = allowedEditors & 510;
        }
        List namesFinal = new ArrayList(names.size());
        List valuesFinal = new ArrayList(values.size());
        for (int i = 0; i < names.size(); i++) {
            if ((allowedEditors & loop) > 0) {
                valuesFinal.add(values.elementAt(i));
                namesFinal.add(names.elementAt(i));
            }
            loop <<= 1;
        }
        int currentIndex = valuesFinal.indexOf(getParamEditormode());
        return buildSelect(attributes, namesFinal, valuesFinal, currentIndex, false);
    }

    /**
     * @see org.opencms.workplace.editors.CmsDefaultPageEditor#escapeParams()
     */
    public void escapeParams() {

        // This code fixes a very strange bug in the MS Dhtml Control.
        // If the HTML source contains a link like this:
        // <a href=
        // "/some/url">Link</a>
        // i.e. with a linebreak between the = and the " of the link href attribute, this 
        // causes the Dhtml control to insert additional buggy attributes in the link.
        // Solution: Remove the linebreak in this case on the server (see next lines).
        String result = CmsStringUtil.substitute(getParamContent(), "\r\n", "\n");
        result = CmsStringUtil.substitute(result, "=\n\"", "=\"");
        // escape the content
        result = CmsEncoder.escapeWBlanks(result, CmsEncoder.ENCODING_UTF_8);
        setParamContent(result);
    }

    /**
     * @see org.opencms.workplace.editors.CmsEditor#getEditorResourceUri()
     */
    public final String getEditorResourceUri() {

        return getSkinUri() + "editors/" + EDITOR_TYPE + "/";
    }

    /**
     * Manipulates the content String for the different editor views and the save operation.<p>
     * 
     * @param save if set to true, the content parameter is not updated
     * @return the prepared content String
     */
    protected String prepareContent(boolean save) {

        String content = getParamContent();
        // extract content of <body>...</body> tag
        content = CmsStringUtil.extractHtmlBody(content);
        // remove unwanted "&amp;" from links
        content = filterAnchors(content);

        // ensure all chars in the content are valid for the selected encoding
        content = CmsEncoder.adjustHtmlEncoding(content, getFileEncoding());

        if (!("edit".equals(getParamEditormode()) || save)) {
            // editor is in html mode, add tags for stylesheet
            String stylesheet = getUriStyleSheet();

            // create a head with stylesheet for template and base URL to display images correctly
            String server = getJsp().getRequest().getScheme()
                + "://"
                + getJsp().getRequest().getServerName()
                + ":"
                + getJsp().getRequest().getServerPort();
            StringBuffer result = new StringBuffer(content.length() + 1024);
            result.append("<html><head>");
            if (!"".equals(stylesheet)) {
                stylesheet = getJsp().link(stylesheet);
                result.append("<link href=\"");
                result.append(server);
                result.append(stylesheet);
                result.append("\" rel=\"stylesheet\" type=\"text/css\">");
            }
            result.append("<base href=\"");
            result.append(server);
            result.append(OpenCms.getSystemInfo().getOpenCmsContext());
            result.append("\"></base></head><body>");
            result.append(content);
            result.append("</body></html>");
            content = result.toString();
        }
        if (!save) {
            // set the content parameter to the modified content
            setParamContent(content);
        }
        return content.trim();
    }

    /**
     * Filters the content String and removes unwanted "&amp;" Strings from anchor "href" or "src" attributes.<p>
     * 
     * These unwanted "&amp;" Strings are produced by the MS DHTML editing control.<p>
     * 
     * @param content the content of the editor
     * @return filtered content
     */
    private String filterAnchors(String content) {

        String anchor = null;
        String newAnchor = null;

        // don't forget to update the group index on the matcher after changing the regex below!      
        Matcher matcher = REGEX_LINKS.matcher(content);
        while (matcher.find()) {
            anchor = matcher.group(6);
            newAnchor = CmsStringUtil.substitute(anchor, "&amp;", "&");
            if (anchor.length() != newAnchor.length()) {
                // substitute only if anchor length has changed
                content = CmsStringUtil.substitute(content, anchor, newAnchor);
            }
        }
        return content;
    }

}
