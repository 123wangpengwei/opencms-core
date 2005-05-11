/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/tools/CmsTool.java,v $
 * Date   : $Date: 2005/05/11 10:51:42 $
 * Version: $Revision: 1.9 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2005 Alkacon Software (http://www.alkacon.com)
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

package org.opencms.workplace.tools;

import org.opencms.main.OpenCms;
import org.opencms.util.CmsIdentifiableObjectContainer;
import org.opencms.workplace.CmsWorkplace;

import java.util.Iterator;
import java.util.List;

/**
 * Implementation of an administration tool.<p>
 * 
 * An admin tool can be a link to itself through 
 * the <code>{@link #buttonHtml(CmsWorkplace)}</code> method,
 * as also a group of <code>{@link CmsToolGroup}</code>s through the 
 * <code>{@link #groupHtml(CmsWorkplace)}</code> method.<p>
 * 
 * @author Michael Moossen (m.moossen@alkacon.com) 
 * @version $Revision: 1.9 $
 * @since 5.7.3
 */
public class CmsTool {

    /** Sub-tools container. */
    private final CmsIdentifiableObjectContainer m_container = new CmsIdentifiableObjectContainer(true, true);

    /** Dhtml id, from name. */
    private final String m_id;

    /** Handler that represents this tool. */
    private I_CmsToolHandler m_handler;

    /**
     * Default Constructor.<p> 
     * 
     * @param id a unique id
     * @param handler the handler that represents this tool
     */
    public CmsTool(String id, I_CmsToolHandler handler) {

        m_id = id;
        m_handler = handler;

    }

    /**
     * Adds a group.<p>
     * 
     * @param group the group
     * 
     * @see org.opencms.util.I_CmsIdentifiableObjectContainer#addIdentifiableObject(String, Object)
     */
    public void addToolGroup(CmsToolGroup group) {

        m_container.addIdentifiableObject(group.getName(), group);
    }

    /**
     * Adds a group at the given position.<p>
     * 
     * @param group the group
     * @param position the position
     * 
     * @see org.opencms.util.I_CmsIdentifiableObjectContainer#addIdentifiableObject(String, Object, float)
     */
    public void addToolGroup(CmsToolGroup group, float position) {

        m_container.addIdentifiableObject(group.getName(), group, position);
    }

    /**
     * Returns the necessary html code for a link to this tool.<p>
     * 
     * @param wp the jsp page to write the code to
     * 
     * @return html code
     */
    public String buttonHtml(CmsWorkplace wp) {

        if (!m_handler.isVisible(wp.getCms())) {
            return "";
        }
        String link = OpenCms.getWorkplaceManager().getToolManager().cmsLinkForPath(
            wp.getJsp(),
            this.getHandler().getPath(), null);
        String onClic = "openPage('" + link + "');";
        return A_CmsHtmlIconButton.defaultBigButtonHtml(
            getId(),
            m_handler.getName(),
            m_handler.isEnabled(wp.getCms())?m_handler.getHelpText():m_handler.getDisabledHelpText(),
            m_handler.isEnabled(wp.getCms()),
            m_handler.getIconPath(),
            onClic);
    }

    /**
     * Compares two tools by name.<p>
     * 
     * @param that the other tool
     * 
     * @return <code>true</code> if the tools have the same name
     */
    public boolean equals(Object that) {

        if (!this.getClass().isInstance(that)) {
            return false;
        }
        return this.getId().equals(((CmsTool)that).getId());
    }

    /**
     * Returns the dhtml unique id.<p>
     *
     * @return the dhtml unique id
     */
    public String getId() {

        return m_id;
    }

    /**
     * Returns the handler.<p>
     *
     * @return the handler
     */
    public I_CmsToolHandler getHandler() {

        return m_handler;
    }

    /**
     * Returns the requested group.<p>
     * 
     * @param name the name of the group
     * 
     * @return the group
     * 
     * @see org.opencms.util.I_CmsIdentifiableObjectContainer#getObject(String)
     */
    public CmsToolGroup getToolGroup(String name) {

        return (CmsToolGroup)m_container.getObject(name);
    }

    /**
     * Retuns a list of groups.<p>
     * 
     * @return a list of <code>{@link CmsToolGroup}</code>
     */
    public List getToolGroups() {

        return m_container.elementList();
    }

    /**
     * Returns the necessary html code for the tool subgroups.<p>
     * 
     * @param wp the jsp page to write the code to
     * 
     * @return html code
     */
    public String groupHtml(CmsWorkplace wp) {

        List subTools = OpenCms.getWorkplaceManager().getToolManager().getToolsForPath(getHandler().getPath(), false);
        Iterator itSubTools = subTools.iterator();
        m_container.clear();
        while (itSubTools.hasNext()) {
            String subToolPath = (String)itSubTools.next();
            CmsTool subTool = OpenCms.getWorkplaceManager().getToolManager().resolveAdminTool(subToolPath);
            // locate group
            CmsToolGroup group = null;
            String groupName = wp.resolveMacros(subTool.getHandler().getGroup());

            // in the parent tool
            group = getToolGroup(groupName);
            if (group == null) {
                // if does not exist, create it
                String gid = "group" + getToolGroups().size();
                group = new CmsToolGroup(gid, groupName);
                addToolGroup(group, subTool.getHandler().getPosition());
            }

            // add to group
            group.addAdminTool(subTool, subTool.getHandler().getPosition());

        }

        StringBuffer html = new StringBuffer(512);
        Iterator itHtml = getToolGroups().iterator();
        while (itHtml.hasNext()) {
            CmsToolGroup group = (CmsToolGroup)itHtml.next();
            html.append(group.groupHtml(wp));
        }
        return wp.resolveMacros(html.toString());
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {

        return m_handler.getName().hashCode();
    }

}