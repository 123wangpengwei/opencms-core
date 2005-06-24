/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/commons/CmsLock.java,v $
 * Date   : $Date: 2005/06/24 14:15:19 $
 * Version: $Revision: 1.13 $
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
package org.opencms.workplace.commons;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.security.CmsPermissionSet;
import org.opencms.workplace.CmsDialog;
import org.opencms.workplace.CmsDialogSelector;
import org.opencms.workplace.CmsWorkplaceSettings;
import org.opencms.workplace.I_CmsDialogHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

/**
 * Creates the dialogs for locking, unlocking or steal lock operations on a resource.<p> 
 *
 * The following files use this class:
 * <ul>
 * <li>/commons/lock_standard.jsp
 * <li>/commons/lockchange_standard.jsp
 * <li>/commons/unlock_standard.jsp
 * </ul>
 * <p>
 * 
 * @author  Andreas Zahner 
 * 
 * @version $Revision: 1.13 $ 
 * 
 * @since 6.0.0 
 */
public class CmsLock extends CmsDialog implements I_CmsDialogHandler {
    
    /** Value for the action: confirmed. */
    public static final int ACTION_SUBMIT_NOCONFIRMATION = 200;
    
    /** Request parameter value for the action: submit form without user interaction. */
    public static final String DIALOG_SUBMIT_NOCONFIRMATION = "submitnoconfirmation";
    
    /** The dialog type: lock a resource. */
    public static final String DIALOG_TYPE_LOCK = "lock";
    /** The dialog type: Steal a lock. */
    public static final String DIALOG_TYPE_LOCKCHANGE = "lockchange";
    /** The dialog type: unlock a resource. */
    public static final String DIALOG_TYPE_UNLOCK = "unlock";
    
    /** Type of the operation which is performed: lock resource. */
    public static final int TYPE_LOCK = 1;
    /** Type of the operation which is performed: steal a lock. */
    public static final int TYPE_LOCKCHANGE = 2;
    /** Type of the operation which is performed: unlock resource. */
    public static final int TYPE_UNLOCK = 3;
    
    
    /** The lock dialog URI. */
    public static final String URI_LOCK_DIALOG = C_PATH_DIALOGS + "lock_standard.jsp";
    /** The steal lock dialog URI. */
    public static final String URI_LOCKCHANGE_DIALOG = C_PATH_DIALOGS + "lockchange_standard.jsp";
    /** The unlock dialog URI. */
    public static final String URI_UNLOCK_DIALOG = C_PATH_DIALOGS + "unlock_standard.jsp";
    
    /**
     * Default constructor needed for dialog handler implementation.<p>
     */
    public CmsLock() {
        super(null);
    }
    
    /**
     * Public constructor.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsLock(CmsJspActionElement jsp) {
        super(jsp);
    }
    
    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */
    public CmsLock(PageContext context, HttpServletRequest req, HttpServletResponse res) {
        this(new CmsJspActionElement(context, req, res));
    } 
    
    /**
     * Determines if the resource should be locked, unlocked or if the lock should be stolen.<p>
     * 
     * @param cms the CmsObject
     * @return the dialog action: lock, change lock (steal) or unlock
     */
    public static int getDialogAction(CmsObject cms) {
        String fileName = CmsResource.getName(cms.getRequestContext().getUri());
        if (fileName == null) {
            // file name could not be determined, return "unlock" action
            return TYPE_UNLOCK;
        } else if (fileName.equalsIgnoreCase("lock.jsp")) {
            // a "lock" action is requested
            return TYPE_LOCK;            
        } else if (fileName.indexOf("change") != -1) {
            // a "steal lock" action is requested
            return TYPE_LOCKCHANGE;            
        } else {
            // an "unlock" action is requested
            return TYPE_UNLOCK;            
        }
    }
    
    /**
     * Performs the lock/unlock operation, will be called by the JSP page.<p>
     * 
     * @throws JspException if problems including sub-elements occur
     */
    public void actionToggleLock() throws JspException {
        // save initialized instance of this class in request attribute for included sub-elements
        getJsp().getRequest().setAttribute(C_SESSION_WORKPLACE_CLASS, this);
    
        try {
            String resName = getParamResource();
            CmsResource res = getCms().readResource(resName, CmsResourceFilter.ALL);
            if (res.isFolder() && !resName.endsWith("/")) {
                resName += "/";
            }
            // perform action depending on dialog uri
            switch (getDialogAction(getCms())) {
            case TYPE_LOCK:
                getCms().lockResource(getParamResource());
                break;
            case TYPE_LOCKCHANGE:
                getCms().changeLock(resName);
                break;
            case TYPE_UNLOCK:
            default:               
                getCms().unlockResource(resName);
            }
            actionCloseDialog();
        } catch (Throwable e) {
            // exception occured, show error dialog
            includeErrorpage(this, e);  
        }
    }

    /**
     * @see org.opencms.workplace.I_CmsDialogHandler#getDialogHandler()
     */
    public String getDialogHandler() {
        return CmsDialogSelector.DIALOG_LOCK;
    }
    
    /**
     * @see org.opencms.workplace.I_CmsDialogHandler#getDialogUri(java.lang.String, CmsJspActionElement)
     */
    public String getDialogUri(String resource, CmsJspActionElement jsp) {
        switch (getDialogAction(jsp.getCmsObject())) {
            case TYPE_LOCK:
                return URI_LOCK_DIALOG;
            case TYPE_LOCKCHANGE:
                return URI_LOCKCHANGE_DIALOG;
            case TYPE_UNLOCK:
            default:
                return URI_UNLOCK_DIALOG;
        }
    }
    
    /**
     * Determines whether to show the lock dialog depending on the users settings.<p>
     * 
     * @return true if dialogs should be shown, otherwise false
     */
    public boolean showConfirmation() {
        return getSettings().getUserSettings().getDialogShowLock();
    }
        
    /**
     * @see org.opencms.workplace.CmsWorkplace#initWorkplaceRequestValues(org.opencms.workplace.CmsWorkplaceSettings, javax.servlet.http.HttpServletRequest)
     */
    protected void initWorkplaceRequestValues(CmsWorkplaceSettings settings, HttpServletRequest request) {
        // fill the parameter values in the get/set methods
        fillParamValues(request);
          
        // set the action for the JSP switch 
        if (DIALOG_CONFIRMED.equals(getParamAction())) {
            setAction(ACTION_CONFIRMED);
        } else if (DIALOG_CANCEL.equals(getParamAction())) {          
            setAction(ACTION_CANCEL);
        } else {
            switch (getDialogAction(getCms())) {
            case TYPE_LOCK:
                setParamTitle(key("messagebox.title.lock"));
                setParamDialogtype(DIALOG_TYPE_LOCK);
                // check the required permissions to lock/unlock the resource       
                if (! checkResourcePermissions(CmsPermissionSet.ACCESS_WRITE, false)) {
                    // no write permissions for the resource, set cancel action to close dialog
                    setAction(ACTION_CANCEL);
                    return;
                }
                break;
            case TYPE_LOCKCHANGE:
                setParamTitle(key("messagebox.title.lockchange"));
                setParamDialogtype(DIALOG_TYPE_UNLOCK);
                break;
            case TYPE_UNLOCK:
            default:
                setParamTitle(key("messagebox.title.unlock"));
                setParamDialogtype(DIALOG_TYPE_UNLOCK);
            }
            // set action depending on user settings
            if (showConfirmation()) {
                // show confirmation dialog
                setAction(ACTION_DEFAULT);
            } else {
                // lock/unlock resource without confirmation
                setAction(ACTION_SUBMIT_NOCONFIRMATION);
            }
        }                 
    }
    
}
