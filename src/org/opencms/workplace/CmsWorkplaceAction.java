/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/CmsWorkplaceAction.java,v $
 * Date   : $Date: 2004/02/04 10:48:13 $
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
 */
 
package org.opencms.workplace;

import org.opencms.main.OpenCms;

import com.opencms.file.CmsObject;
import com.opencms.workplace.I_CmsWpConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Utility class with static methods, mainly intended for the transition of
 * functionality from the old XML based workplace to the new JSP workplace.<p>
 * 
 * @author  Alexander Kandzior (a.kandzior@alkacon.com)
 * @version $Revision: 1.20 $
 * 
 * @since 5.1
 */
public final class CmsWorkplaceAction { 
    
    /** Path to the JSP workplace */    
    public static final String C_PATH_JSP_WORKPLACE = I_CmsWpConstants.C_VFS_PATH_WORKPLACE + "jsp/";
    
    /** Path to the XML workplace */    
    public static final String C_PATH_XML_WORKPLACE = I_CmsWpConstants.C_VFS_PATH_WORKPLACE + "action/";
   
    /** Path to the JSP workplace frame loader file */    
    public static final String C_JSP_WORKPLACE_URI = C_PATH_JSP_WORKPLACE + "top_fs.html";
    
    /** Path to the XML workplace frame loader file */    
    public static final String C_XML_WORKPLACE_URI = C_PATH_XML_WORKPLACE + "index.html";   
    
    /** File name of explorer file list loader (same for JSP and XML) */
    public static final String C_FILE_WORKPLACE_FILELIST = "explorer_files.html";
    
    /** Path to the JSP explorer */
    public static final String C_JSP_WORKPLACE_FILELIST = "../jsp/" + C_FILE_WORKPLACE_FILELIST;
        
    /**
     * Constructor is private since there must be no intances of this class.<p>
     */
    private CmsWorkplaceAction() {
        // empty
    }
    
    /**
     * Updates the user preferences after changes have been made.<p>
     * 
     * @param cms the current cms context
     */
    public static void updatePreferences(CmsObject cms) {
        HttpSession session = extractSession(cms);
        if (session == null) {
            return;
        }
        CmsWorkplaceSettings settings = (CmsWorkplaceSettings)session.getAttribute(CmsWorkplace.C_SESSION_WORKPLACE_SETTINGS);
        if (settings == null) {
            return;
        }
        settings = CmsWorkplace.initWorkplaceSettings(cms, settings);    
    }
    
    /**
     * Returns the uri of the top workplace frameset.<p>
     * 
     * @param cms the current cms context
     * @return the uri of the top workplace frameset
     */    
    public static String getWorkplaceUri(CmsObject cms) {              
        HttpSession session = extractSession(cms);
        if (session == null) {
            return C_XML_WORKPLACE_URI;
        }
        CmsWorkplaceSettings settings = (CmsWorkplaceSettings)session.getAttribute(CmsWorkplace.C_SESSION_WORKPLACE_SETTINGS);
        if (settings == null) {
            return C_XML_WORKPLACE_URI;
        }
        return C_JSP_WORKPLACE_URI;        
    }    
    
    /**
     * Returns the folder currently selected in the explorer.<p>
     * 
     * @param cms the current cms context
     * @return the folder currently selected in the explorer
     */
    public static String getCurrentFolder(CmsObject cms) {
        HttpSession session = extractSession(cms);
        if (session == null) {
            return null;
        }
        CmsWorkplaceSettings settings = (CmsWorkplaceSettings)session.getAttribute(CmsWorkplace.C_SESSION_WORKPLACE_SETTINGS);
        if (settings != null) {
            return settings.getExplorerResource();
        } else {
            return (String)session.getAttribute(I_CmsWpConstants.C_PARA_FILELIST);
        }
    }
    
    /**
     * Sets the folder currently selected in the explorer
     * 
     * @param cms the current cms context
     * @param currentFolder the folder to set
     */
    public static void setCurrentFolder(CmsObject cms, String currentFolder) {
        HttpSession session = extractSession(cms);
        if (session == null) {
            return;
        }
        CmsWorkplaceSettings settings = (CmsWorkplaceSettings)session.getAttribute(CmsWorkplace.C_SESSION_WORKPLACE_SETTINGS);
        if (settings != null) {
            settings.setExplorerResource(currentFolder);
        }       
        session.setAttribute(I_CmsWpConstants.C_PARA_FILELIST, currentFolder);       
    }   
    
    /**
     * Returns the uri of the file explorer.<p>
     * 
     * @param cms the current cms context
     * @return the uri of the file explorer
     */    
    public static String getExplorerFileUri(CmsObject cms) {              
        HttpSession session = extractSession(cms);
        if (session == null) {
            return I_CmsWpConstants.C_WP_EXPLORER_FILELIST;
        }
        CmsWorkplaceSettings settings = (CmsWorkplaceSettings)session.getAttribute(CmsWorkplace.C_SESSION_WORKPLACE_SETTINGS);
        if (settings == null) {
            return I_CmsWpConstants.C_WP_EXPLORER_FILELIST;
        }
        return C_JSP_WORKPLACE_FILELIST;        
    }  
    
    /**
     * Returns the full uri (absoluth path with servlet context) of the file explorer.<p>
     * 
     * @param cms the current cms context
     * @return the uri of the file explorer
     */    
    public static String getExplorerFileFullUri(CmsObject cms) {              
        HttpSession session = extractSession(cms);
        String link = C_PATH_XML_WORKPLACE + C_FILE_WORKPLACE_FILELIST;
        if (session == null) {
            return OpenCms.getLinkManager().substituteLink(cms, link);
        }
        CmsWorkplaceSettings settings = (CmsWorkplaceSettings)session.getAttribute(CmsWorkplace.C_SESSION_WORKPLACE_SETTINGS);
        if (settings == null) {
            return OpenCms.getLinkManager().substituteLink(cms, link);
        }
        return OpenCms.getLinkManager().substituteLink(cms, C_PATH_JSP_WORKPLACE + C_FILE_WORKPLACE_FILELIST);        
    }    
        
    /**
     * Extracts the HttpSession from the provided OpenCms context 
     * 
     * @param cms the current cms context
     * @return the HttpSession from the provided context, or null if no session could be extracted 
     */
    private static HttpSession extractSession(CmsObject cms) {
        HttpSession session = null;
        try {
            HttpServletRequest request = (HttpServletRequest)cms.getRequestContext().getRequest().getOriginalRequest();
            session = request.getSession(false);
        } catch (Exception e) {
            // original request could not be extracted, use null return value 
        }
        return session;
    }

}
