/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/Attic/CmsDelete.java,v $
 * Date   : $Date: 2003/07/31 12:17:35 $
 * Version: $Revision: 1.9 $
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

import com.opencms.boot.I_CmsLogChannels;
import com.opencms.core.A_OpenCms;
import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.file.CmsResource;
import com.opencms.flex.jsp.CmsJspActionElement;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.opencms.lock.CmsLock;
import org.opencms.lock.CmsLockException;

/**
 * Provides methods for the delete resources dialog.<p> 
 * 
 * The following files use this class:
 * <ul>
 * <li>/jsp/delete_html
 * </ul>
 *
 * @author  Andreas Zahner (a.zahner@alkacon.com)
 * @version $Revision: 1.9 $
 * 
 * @since 5.1
 */
public class CmsDelete extends CmsDialog {

    // always start individual action id's with 100 to leave enough room for more default actions
    public static final int ACTION_DELETE = 100;
    
    public static final String DIALOG_TYPE = "delete";
    
    private String m_deleteVfsLinks;
    
    /**
     * Public constructor with JSP action element.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsDelete(CmsJspActionElement jsp) {
        super(jsp);
    }
    
    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */
    public CmsDelete(PageContext context, HttpServletRequest req, HttpServletResponse res) {
        this(new CmsJspActionElement(context, req, res));
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
        if (DIALOG_TYPE.equals(getParamAction())) {
            setAction(ACTION_DELETE);                            
        } else if (DIALOG_WAIT.equals(getParamAction())) {
            setAction(ACTION_WAIT);
        } else {                        
            setAction(ACTION_DEFAULT);
            // build title for delete dialog     
            setParamTitle(key("title.delete") + ": " + CmsResource.getName(getParamResource()));
        }      
    } 

    /**
     * Performs the delete action, will be called by the JSP page.<p>
     * 
     * @throws JspException if problems including sub-elements occur
     */
    public void actionDelete() throws JspException {
        // save initialized instance of this class in request attribute for included sub-elements
        getJsp().getRequest().setAttribute(C_SESSION_WORKPLACE_CLASS, this);
        try {
            if (performDeleteOperation())  {
                // if no exception is caused and "true" is returned delete operation was successful
                getJsp().include(C_FILE_EXPLORER_FILELIST);
            } else  {
                // "false" returned, display "please wait" screen
                getJsp().include(C_FILE_DIALOG_SCREEN_WAIT);
            }    
        } catch (CmsException e) {
            // prepare common message part

            String message = "<p>\n" + key("title.delete") + ": " + getParamResource() + "\n</p>\n";                 
            
            setParamErrorstack(e.getStackTraceAsString());
            setParamMessage(message + key("error.message." + getParamDialogtype()));
            
            if (e instanceof CmsLockException) {
                setParamReasonSuggestion(e.getMessage());
            } else {
                setParamReasonSuggestion(getErrorSuggestionDefault());
            }

            getJsp().include(C_FILE_DIALOG_SCREEN_ERROR); 
        }
    }
    
    /**
     * Performs the resource deletion.<p>
     * 
     * @return true, if the resource was deleted, otherwise false
     * @throws CmsException if deletion is not successful
     */
    private boolean performDeleteOperation() throws CmsException {
        int deleteOption = -1;     
        
        // on folder copy display "please wait" screen, not for simple file copy
        CmsResource sourceRes = getCms().readFileHeader(getParamResource());
        if (sourceRes.isFolder() && ! DIALOG_WAIT.equals(getParamAction())) {
            // return false, this will trigger the "please wait" screen
            return false;
        }
        
        // TODO: remove check finally
        //if (sourceRes.isHardLink()) {
            deleteOption = "true".equalsIgnoreCase(getParamDeleteVfsLinks()) ? I_CmsConstants.C_DELETE_OPTION_DELETE_VFS_LINKS : I_CmsConstants.C_DELETE_OPTION_PRESERVE_VFS_LINKS;
        //} else {
        //    deleteOption = I_CmsConstants.C_DELETE_OPTION_IGNORE_VFS_LINKS;
        //}
         
        // delete the resource
        getCms().deleteResource(getParamResource(), deleteOption);
        
        return true;
    }
    
    /**
     * Checks if VFS links are pointing to this resource.
     * 
     * @return true if one or more VFS links are pointing to this resource
     * @throws CmsException if something goes wrong
     */
    public boolean hasVfsLinks() throws CmsException {
        return getCms().getAllVfsSoftLinks(getParamResource()).size() > 0;
    }
    
    /**
     * Checks if the current resource has lock state exclusive or inherited.<p>
     * 
     * This is used to determine whether the dialog shows the option to delete all
     * siblings of the resource or not.
     * 
     * @return true if lock state is exclusive or inherited, otherwise false
     */
    public boolean hasCorrectLockstate() {
        CmsLock lock = null;
        try {
            // get the lock state for the current resource
            lock = getCms().getLock(getParamResource());
        } catch (CmsException e) {
            lock = CmsLock.getNullLock();
    
            if (I_CmsLogChannels.C_LOGGING && A_OpenCms.isLogging(I_CmsLogChannels.C_OPENCMS_CRITICAL)) { 
                A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, this.getClass().getName() + " error getting lock state for resource " + getParamResource() + " " + e.getMessage());
            }  
            return false;           
        }
        int type = lock.getType();
        return (type == CmsLock.C_TYPE_EXCLUSIVE || type == CmsLock.C_TYPE_INHERITED);
    }
    
    /**
     * Sets the value of the boolean option to delete VFS links.<p>
     * 
     * @param value the value of the boolean option to delete VFS links
     */
    public void setParamDeleteVfsLinks(String value) {
        m_deleteVfsLinks = value;
    }
    
    /**
     * Returns the value of the boolean option to delete VFS links.<p>
     * 
     * @return the value of the boolean option to delete VFS links as a lower case string
     */
    public String getParamDeleteVfsLinks() {
        return m_deleteVfsLinks;
    }
    
}
