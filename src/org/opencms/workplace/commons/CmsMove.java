/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/commons/CmsMove.java,v $
 * Date   : $Date: 2005/05/31 14:38:39 $
 * Version: $Revision: 1.10 $
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
package org.opencms.workplace.commons;

import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.CmsVfsException;
import org.opencms.file.CmsVfsResourceAlreadyExistsException;
import org.opencms.file.CmsVfsResourceNotFoundException;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.I_CmsConstants;
import org.opencms.site.CmsSiteManager;
import org.opencms.staticexport.CmsLinkManager;
import org.opencms.workplace.CmsDialog;
import org.opencms.workplace.CmsWorkplaceSettings;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;

/**
 * Provides methods for the move resources dialog.<p> 
 * 
 * The following files use this class:
 * <ul>
 * <li>/commons/move.jsp
 * </ul>
 *
 * @author  Andreas Zahner (a.zahner@alkacon.com)
 * @version $Revision: 1.10 $
 * 
 * @since 5.1
 */
public class CmsMove extends CmsDialog {

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsMove.class);  
    
    /** Value for the action: move resource. */
    public static final int ACTION_MOVE = 100;
    
    /** The dialog type. */
    public static final String DIALOG_TYPE = "move";

    private String m_paramTarget;
    
    /**
     * Public constructor with JSP action element.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsMove(CmsJspActionElement jsp) {
        super(jsp);
    }
    
    
    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */
    public CmsMove(PageContext context, HttpServletRequest req, HttpServletResponse res) {
        this(new CmsJspActionElement(context, req, res));
    }        

    /**
     * Returns the value of the target parameter, 
     * or null if this parameter was not provided.<p>
     * 
     * The target parameter selects the target name 
     * of the operation.<p>
     * 
     * @return the value of the target parameter
     */    
    public String getParamTarget() {
        return m_paramTarget;
    }
    
    /**
     * Sets the value of the target parameter.<p>
     * 
     * @param value the value to set
     */
    public void setParamTarget(String value) {
        m_paramTarget = value;
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
            setAction(ACTION_MOVE);                            
        } else if (DIALOG_CONFIRMED.equals(getParamAction())) {
            setAction(ACTION_CONFIRMED);
        } else if (DIALOG_WAIT.equals(getParamAction())) {
            setAction(ACTION_WAIT);
        } else if (DIALOG_CANCEL.equals(getParamAction())) {          
            setAction(ACTION_CANCEL);
        } else {                        
            setAction(ACTION_DEFAULT);
            // build title for copy dialog     
            setParamTitle(key("title.move") + ": " + CmsResource.getName(getParamResource()));
        }      
    } 

    /**
     * Returns the current name of the resource without path information.<p>
     * 
     * This is used to preset the input text field with the current resource name.<p>
     * 
     * @return the current name of the resource without path information
     */
    public String getCurrentResourceName() {
        String resourceName = CmsResource.getName(getParamResource());
        if (resourceName.endsWith("/")) {
            resourceName = resourceName.substring(0, resourceName.length() - 1);
        }
        return resourceName;
    }
    
    /**
     * Performs the move action, will be called by the JSP page.<p>
     * 
     * @throws JspException if problems including sub-elements occur
     */
    public void actionMove() throws JspException {
        // save initialized instance of this class in request attribute for included sub-elements
        getJsp().getRequest().setAttribute(C_SESSION_WORKPLACE_CLASS, this);
        try {          
            CmsResource sourceRes = getCms().readResource(getParamResource(), CmsResourceFilter.ALL);
            boolean isFolder = sourceRes.isFolder();
            if (performMoveOperation(isFolder))  {
                // if no exception is caused and "true" is returned move operation was successful
                if (isFolder) {
                    // set request attribute to reload the explorer tree view
                    List folderList = new ArrayList(2);
                    String sourceParent = CmsResource.getParentFolder(getParamResource());
                    String targetParent = CmsResource.getParentFolder(getParamTarget());
                    if (!targetParent.equals(sourceParent)) {
                        // update target folder if its not the same as the source folder
                        folderList.add(targetParent);
                    }
                    folderList.add(CmsResource.getParentFolder(getParamResource()));
                    getJsp().getRequest().setAttribute(C_REQUEST_ATTRIBUTE_RELOADTREE, folderList);
                }
                actionCloseDialog();
            } else  {
                // "false" returned, display "please wait" screen
                getJsp().include(C_FILE_DIALOG_SCREEN_WAIT);
            }    
        } catch (CmsVfsResourceAlreadyExistsException e) {
            // prepare common message part
            String message = "<p>\n" 
                + key("source") + ": " + getParamResource() + "<br>\n" 
                + key("target") + ": " + getParamTarget() + "\n</p>\n";
            // file move operation but file already exists, show confirmation dialog
            setParamMessage(message + key("confirm.message.copy"));
            getJsp().include(C_FILE_DIALOG_SCREEN_CONFIRM);             
        } catch (Throwable e) {
            // error during move operation, show error dialog
            includeErrorpage(this, e);  
        }
    }
    
    /**
     * Performs the resource moving.<p>
     * 
     * @param isFolder true, if the resource to move is a folder, otherwise false
     * @return true, if the resource was successfully moved, otherwise false
     * @throws CmsException if copying is not successful
     */
    private boolean performMoveOperation(boolean isFolder) throws CmsException {

        // on folder move operation display "please wait" screen, not for simple file move operation
        if (isFolder && ! DIALOG_WAIT.equals(getParamAction())) {
            // return false, this will trigger the "please wait" screen
            return false;
        }

        // get the target name
        String target = getParamTarget();
        if (target == null) {
            target = "";
        }
        
        boolean restoreSiteRoot = false;
        try {
            // check if a site root was added to the target name
            String sitePrefix = "";
            if (CmsSiteManager.getSiteRoot(target) != null) { 
                String siteRootFolder = getCms().getRequestContext().getSiteRoot();
                if (siteRootFolder.endsWith("/")) {
                    siteRootFolder = siteRootFolder.substring(0, siteRootFolder.length()-1);
                }  
                sitePrefix = siteRootFolder;
                getCms().getRequestContext().saveSiteRoot();
                getCms().getRequestContext().setSiteRoot("/");
                restoreSiteRoot = true;
            }

            // get the source name 
            String source = getParamResource();
            
            // calculate the target name
            target = CmsLinkManager.getAbsoluteUri(target, CmsResource.getParentFolder(getParamResource()));
    
            if (target.equals(source)) {
                throw new CmsVfsException(Messages.get().container(Messages.ERR_MOVE_ONTO_ITSELF_1, target));
            }           
            
            try {
                CmsResource res = getCms().readResource(target, CmsResourceFilter.ALL);
                if (res.isFolder()) {
                    // target folder already exists, so we add the current folder name
                    if (! target.endsWith("/")) {
                        target += "/";
                    }
                    target = target + CmsResource.getName(getParamResource());
                    if (target.endsWith("/")) {
                        target = target.substring(0, target.length()-1);
                    }
                }
            } catch (CmsVfsResourceNotFoundException e) {
                // target folder does not already exist, so target name is o.k.
                if (LOG.isInfoEnabled()) {
                    LOG.info(e.getLocalizedMessage());
                }                
            }
            
            // set the target parameter value
            setParamTarget(target);        
            
            // check if target already exists, if so, throw exception to show confirmation dialog
            CmsResource targetRes = null;
            try {
                targetRes = getCms().readResource(target, CmsResourceFilter.ALL);
            } catch (CmsException e) { 
                // can usually be ignored
                if (LOG.isInfoEnabled()) {
                    LOG.info(e.getLocalizedMessage());
                }
            }
    
            if (targetRes != null) {
                if (DIALOG_CONFIRMED.equals(getParamAction())) {
                    // delete existing target resource if confirmed by the user
                    getCms().deleteResource(target, I_CmsConstants.C_DELETE_OPTION_PRESERVE_SIBLINGS);
                } else {
                    // throw exception to indicate that the target exists
                    throw new CmsException(Messages.get().container(
                        Messages.ERR_MOVE_FAILED_TARGET_EXISTS_2, getParamResource(), getJsp().getRequestContext().removeSiteRoot(target)));
                }
            } 
                    
            // lock resource if autolock is enabled
            checkLock(sitePrefix + getParamResource());              
            // move the resource
            getCms().moveResource(sitePrefix + getParamResource(), target);
        } finally {
            if (restoreSiteRoot) {
                getCms().getRequestContext().restoreSiteRoot();
            }
        }
        return true;
    }
}
