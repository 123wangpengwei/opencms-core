/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/Attic/CmsChaccBrowser.java,v $
 * Date   : $Date: 2003/07/02 13:40:26 $
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
package org.opencms.workplace;

import com.opencms.core.CmsException;
import com.opencms.file.CmsGroup;
import com.opencms.file.CmsUser;
import com.opencms.flex.jsp.CmsJspActionElement;

import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

/**
 * Provides methods for building the groups and users popup window.<p> 
 * 
 * The following files use this class:
 * <ul>
 * <li>/jsp/chaccbrowseusergroup.html
 * </ul>
 *
 * @author  Andreas Zahner (a.zahner@alkacon.com)
 * @version $Revision: 1.1 $
 * 
 * @since 5.1
 */
public class CmsChaccBrowser extends CmsDialog {

    /**
     * Public constructor.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsChaccBrowser(CmsJspActionElement jsp) {
        super(jsp);
    }

    /**
     * @see org.opencms.workplace.CmsWorkplace#initWorkplaceRequestValues(org.opencms.workplace.CmsWorkplaceSettings, javax.servlet.http.HttpServletRequest)
     */
    protected void initWorkplaceRequestValues(CmsWorkplaceSettings settings, HttpServletRequest request) {
    }
    
    /**
     * Builds a list of all groups and returns an html string.<p>
     * 
     * @return html code for a group list
     */
    public String buildGroupList() {
        Vector groups = new Vector();
        StringBuffer retValue = new StringBuffer(1024);
        try {
            groups = getCms().getGroups();
        } catch (CmsException e) {}
        
        for (int i=0; i<groups.size(); i++) {
            CmsGroup curGroup = (CmsGroup)groups.elementAt(i);
            retValue.append(buildEntryGroup(curGroup));
        }
        return retValue.toString();
    }
    
    /**
     * Builds a list of all users and returns an html string.<p>
     * 
     * @return html code for a user list
     */
    public String buildUserList() {
        Vector users = new Vector();
        StringBuffer retValue = new StringBuffer(1024);
        try {
            users = getCms().getUsers();
        } catch (CmsException e) {}

        for (int i=0; i<users.size(); i++) {
            CmsUser curUser = (CmsUser)users.elementAt(i);
            retValue.append(buildEntryUser(curUser));
        }
        return retValue.toString();
    }
    
    /**
     * Creates the html code for a single group entry.<p>
     * 
     * @param group the CmsGroup
     * @return the html code as StringBuffer
     */
    private StringBuffer buildEntryGroup(CmsGroup group) {
        StringBuffer retValue = new StringBuffer(256);
        retValue.append("<span class=\"dialogunmarked maxwidth\" onmouseover=\"className='dialogmarked maxwidth'\""             + " onmouseout=\"className='dialogunmarked maxwidth'\" onclick=\"javascript:top.selectForm('0','"
            + group.getName() + "');\">");
        retValue.append("<img src=\""+getSkinUri()+"buttons/group_sm.gif\">&nbsp;");
        retValue.append(group.getName());
        retValue.append("</span>");
        return retValue;
    }
    
    /**
     * Creates the html code for a single user entry.<p>
     * 
     * @param user the CmsUser
     * @return the html code as StringBuffer
     */
    private StringBuffer buildEntryUser(CmsUser user) {
        StringBuffer retValue = new StringBuffer(384);
        retValue.append("<span class=\"dialogunmarked maxwidth\" onmouseover=\"className='dialogmarked maxwidth'\"" 
            + " onmouseout=\"className='dialogunmarked maxwidth'\" onclick=\"javascript:top.selectForm('1','"
            + user.getName() + "');\">");
        retValue.append("<img src=\""+getSkinUri()+"buttons/user_sm.gif\">&nbsp;");
        retValue.append(user.getName());
        if (!"".equals(user.getFirstname()) || !"".equals(user.getLastname())) {
            retValue.append(" ("+user.getFirstname()+" "+user.getLastname()+")");
        }
        retValue.append("</span>");
        return retValue;
        }

}
