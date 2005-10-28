/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/CmsFrameset.java,v $
 * Date   : $Date: 2005/10/28 12:07:36 $
 * Version: $Revision: 1.84.2.2 $
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

package org.opencms.workplace;

import org.opencms.file.CmsGroup;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsResourceFilter;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.site.CmsSite;
import org.opencms.site.CmsSiteManager;
import org.opencms.synchronize.CmsSynchronizeSettings;
import org.opencms.util.CmsRequestUtil;
import org.opencms.util.CmsStringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;

/**
 * Provides methods for building the main framesets of the OpenCms Workplace.<p> 
 * 
 * The following files use this class:
 * <ul>
 * <li>/views/top.html
 * <li>/views/top_foot.html
 * <li>/views/top_head.html
 * </ul>
 * <p> 
 * 
 * @author  Alexander Kandzior 
 * 
 * @version $Revision: 1.84.2.2 $ 
 * 
 * @since 6.0.0 
 */
public class CmsFrameset extends CmsWorkplace {

    /** The names of the supported frames. */
    public static final String[] FRAMES = {"top", "head", "body", "foot"};

    /** The names of the supported frames in a list. */
    public static final List FRAMES_LIST = Arrays.asList(FRAMES);

    /** Path to the JSP workplace frame loader file. */
    public static final String JSP_WORKPLACE_URI = CmsWorkplace.VFS_PATH_WORKPLACE + "views/workplace.jsp";

    /** The request parameter for the workplace start selection. */
    public static final String PARAM_WP_START = "wpStart";

    /** The request parameter for the workplace view selection. */
    public static final String PARAM_WP_VIEW = "wpView";

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsFrameset.class);

    /** Indicates if a reload of the main body frame is required. */
    private boolean m_reloadRequired;

    /** The request parameter for the selection of the frame. */
    public static final String PARAM_WP_FRAME = "wpFrame";

    /**
     * Public constructor.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsFrameset(CmsJspActionElement jsp) {

        super(jsp);
    }

    /**
     * Returns the javascript code for the broadcast message alert in the foot of the workplace.<p>
     * 
     * @return javascript code showing an alert box when the foot load
     */
    public String getBroadcastMessage() {

        StringBuffer result = new StringBuffer(512);
        String message = getBroadcastMessageString();

        if (CmsStringUtil.isNotEmpty(message)) {
            // create a javascript alert for the message 
            result.append("\n<script type=\"text/javascript\">\n<!--\n");
            // the timeout gives the frameset enough time to load before the alert is shown
            result.append("function showMessage() {\n");
            result.append("\talert(\"");
            // the user has pending messages, display them all
            result.append(CmsStringUtil.escapeJavaScript(message));
            result.append("\");\n}\n");
            result.append("setTimeout('showMessage();', 2000);");
            result.append("\n//-->\n</script>");
        }
        return result.toString();
    }

    /**
     * Returns a html select box filled with groups of the current user.<p>
     * 
     * @param htmlAttributes attributes that will be inserted into the generated html 
     * @return a html select box filled with groups of the current user
     */
    public String getGroupSelect(String htmlAttributes) {

        // get the users groups from the request context
        List allGroups = new Vector();
        try {
            allGroups = getCms().getGroupsOfUser(getSettings().getUser().getName());
        } catch (CmsException e) {
            // should usually never happen
            if (LOG.isInfoEnabled()) {
                LOG.info(e.getLocalizedMessage());
            }
        }

        List options = new ArrayList();
        List values = new ArrayList();

        // loop through all groups and build the result vectors
        int numGroups = allGroups.size();
        for (int i = 0; i < numGroups; i++) {
            CmsGroup loopGroup = (CmsGroup)allGroups.get(i);
            String loopGroupName = loopGroup.getName();
            values.add(loopGroupName);
            options.add(loopGroupName);
        }

        return buildSelect(htmlAttributes, options, values, 0);
    }

    /**
     * Returns the remote ip address of the current user.<p>
     * 
     * @return the remote ip address of the current user
     */
    public String getLoginAddress() {

        return getCms().getRequestContext().getRemoteAddress();
    }

    /**
     * Returns the last login time of the current user in localized format.<p>
     *
     * @return the last login time of the current user in localized format
     */
    public String getLoginTime() {

        return getMessages().getDateTime(getSettings().getUser().getLastlogin());
    }

    /**
     * Returns a html select box filled with the current users accessible projects.<p>
     * 
     * @param htmlAttributes attributes that will be inserted into the generated html 
     * @param htmlWidth additional "width" html attributes
     * @return a html select box filled with the current users accessible projects
     */
    public String getProjectSelect(String htmlAttributes, String htmlWidth) {

        // get all project information
        List allProjects;
        try {
            allProjects = getCms().getAllAccessibleProjects();
        } catch (CmsException e) {
            // should usually never happen
            if (LOG.isInfoEnabled()) {
                LOG.info(e.getLocalizedMessage());
            }
            allProjects = Collections.EMPTY_LIST;
        }

        List options = new ArrayList();
        List values = new ArrayList();
        int selectedIndex = 0;
        int maxNameLength = 0;

        // now loop through all projects and fill the result vectors
        for (int i = 0, n = allProjects.size(); i < n; i++) {
            CmsProject loopProject = (CmsProject)allProjects.get(i);
            String loopProjectName = loopProject.getName();
            String loopProjectId = Integer.toString(loopProject.getId());

            values.add(loopProjectId);
            options.add(loopProjectName);

            if (loopProject.getId() == getSettings().getProject()) {
                // this is the user's current project
                selectedIndex = i;
            }
            // check the length of the project name, to optionallly adjust the size of the selector
            maxNameLength = Math.max(loopProjectName.length(), maxNameLength);
        }
        if (maxNameLength <= 20) {
            StringBuffer buf = new StringBuffer(htmlAttributes.length() + htmlWidth.length() + 5);
            buf.append(htmlAttributes);
            buf.append(" ");
            buf.append(htmlWidth);
            htmlAttributes = buf.toString();
        }

        return buildSelect(htmlAttributes, options, values, selectedIndex);
    }

    /**
     * Returns a html select box filled with the current users accessible sites.<p>
     * 
     * @param htmlAttributes attributes that will be inserted into the generated html 
     * @return a html select box filled with the current users accessible sites
     */
    public String getSiteSelect(String htmlAttributes) {

        List options = new ArrayList();
        List values = new ArrayList();
        int selectedIndex = 0;

        List sites = CmsSiteManager.getAvailableSites(getCms(), true);

        Iterator i = sites.iterator();
        int pos = 0;
        while (i.hasNext()) {
            CmsSite site = (CmsSite)i.next();
            values.add(site.getSiteRoot());
            options.add(site.getTitle());
            if (site.getSiteRoot().equals(getSettings().getSite())) {
                // this is the user's current site
                selectedIndex = pos;
            }
            pos++;
        }

        return buildSelect(htmlAttributes, options, values, selectedIndex);
    }

    /**
     * Returns the startup URI for display in the main body frame, this can 
     * either be the user default view, or (if set) a sepcific startup resource.<p> 
     * 
     * @return the startup URI for display in the main body frame
     */
    public String getStartupUri() {

        String result = getSettings().getViewStartup();
        if (result == null) {
            // no specific startup URI is set, use view from user settings
            result = getSettings().getViewUri();
        } else {
            // reset the startup URI, so that it is not displayed again on reload of the frameset
            getSettings().setViewStartup(null);
        }
        return CmsRequestUtil.appendParameter(result, CmsFrameset.PARAM_WP_FRAME, FRAMES[2]);
    }

    /**
     * Returns a html select box filled with the views accessible by the current user.<p>
     * 
     * @param htmlAttributes attributes that will be inserted into the generated html 
     * @return a html select box filled with the views accessible by the current user
     */
    public String getViewSelect(String htmlAttributes) {

        List options = new ArrayList();
        List values = new ArrayList();
        int selectedIndex = 0;

        // loop through the vectors and fill the result vectors
        Iterator i = OpenCms.getWorkplaceManager().getViews().iterator();
        int count = -1;
        String currentView = getSettings().getViewUri();
        if (CmsStringUtil.isNotEmpty(currentView)) {
            // remove possible parameters from current view
            int pos = currentView.indexOf('?');
            if (pos >= 0) {
                currentView = currentView.substring(0, pos);
            }
        }
        while (i.hasNext()) {
            CmsWorkplaceView view = (CmsWorkplaceView)i.next();
            if (getCms().existsResource(view.getUri(), CmsResourceFilter.ONLY_VISIBLE_NO_DELETED)) {
                count++;
                // ensure the current user has +v+r permissions on the view
                String loopLink = getJsp().link(view.getUri());
                String localizedKey = resolveMacros(view.getKey());
                options.add(localizedKey);
                values.add(loopLink);

                if (loopLink.equals(currentView)) {
                    selectedIndex = count;
                }
            }
        }

        return buildSelect(htmlAttributes, options, values, selectedIndex);
    }

    /**
     * Returns the reload URI for the OpenCms workplace.<p>
     * 
     * @return the reload URI for the OpenCms workplace
     */
    public String getWorkplaceReloadUri() {

        return getJsp().link(CmsFrameset.JSP_WORKPLACE_URI);
    }

    /**
     * Returns true if the user has publish permissions for the current project.<p>
     * 
     * @return true if the user has publish permissions for the current project
     */
    public boolean isPublishEnabled() {

        return getCms().isManagerOfProject();
    }

    /**
     * Returns <code>true</code> if a reload of the main body frame is required.<p>
     * 
     * This value is modified with the select options (project, site or view) in the head frame of 
     * the Workplace. If a user changes one of these select values, the head frame is posted 
     * "against itself". The posted values will be processed by this class, causing
     * the internal Workplace settings to change. After these settings have been changed,
     * a reload of the main body frame is required in order to update it with the new values.
     * A JavaScript in the Workplace head frame will be executed in this case.<p>
     * 
     * @return <code>true</code> if a reload of the main body frame is required
     */
    public boolean isReloadRequired() {

        return m_reloadRequired;
    }

    /**
     * Returns true if the user has enabled synchronization.<p>
     * 
     * @return true if the user has enabled synchronization
     */
    public boolean isSyncEnabled() {

        CmsSynchronizeSettings syncSettings = getSettings().getUserSettings().getSynchronizeSettings();
        return (syncSettings != null) && syncSettings.isSyncEnabled();
    }

    /**
     * Indicates if the site selector should be shown in the top frame depending on the count of accessible sites.<p>
     * 
     * @return true if site selector should be shown, otherwise false
     */
    public boolean showSiteSelector() {

        if (getSettings().getUserSettings().getRestrictExplorerView()) {
            // restricted explorer view to site and folder, do not show site selector
            return false;
        }
        // count available sites
        int siteCount = CmsSiteManager.getAvailableSites(getCms(), true).size();
        return (siteCount > 1);
    }

    
    /**
     * @see org.opencms.workplace.CmsWorkplace#initWorkplaceRequestValues(org.opencms.workplace.CmsWorkplaceSettings, javax.servlet.http.HttpServletRequest)
     */
    protected synchronized void initWorkplaceRequestValues(CmsWorkplaceSettings settings, HttpServletRequest request) {

        // check if a startup page has been set
        String frame = CmsRequestUtil.getNotEmptyDecodedParameter(request, CmsFrameset.PARAM_WP_FRAME);
        if ((frame == null) || (FRAMES_LIST.indexOf(frame) < 0)) {
            // illegal or no frame selected, assume the "top" frame
            frame = FRAMES[0];
        }

        if (FRAMES[0].equals(frame)) {
            // top frame requested - execute special reload actions
            topFrameReload(settings);
        }
        
        // check if a startup page has been set
        String startup = CmsRequestUtil.getNotEmptyDecodedParameter(request, CmsFrameset.PARAM_WP_START);
        if (startup != null) {
            m_reloadRequired = true;
            settings.setViewStartup(startup);
        }
        
        // check if the user requested a view change
        String view = request.getParameter(CmsFrameset.PARAM_WP_VIEW);
        if (view != null) {
            m_reloadRequired = true;
            settings.setViewUri(view);
            // TODO: This is a workaround to make dialogs work in the legacy XMLTemplate views
            settings.getFrameUris().put("body", view);
            settings.getFrameUris().put("admin_content", "/system/workplace/action/administration_content_top.html");
        }
        
        m_reloadRequired = initSettings(settings, request) || m_reloadRequired;
    }

    /**
     * Performs certain clear cache actions if the top frame is reloaded.<p>
     * 
     * @param settings the current users workplace settings
     */
    protected void topFrameReload(CmsWorkplaceSettings settings) {

        // ensure to read the settings from the database
        initUserSettings(getCms(), settings, true);

        // reset the HTML list in order to force a full reload
        settings.setListObject(null);
    }
}
