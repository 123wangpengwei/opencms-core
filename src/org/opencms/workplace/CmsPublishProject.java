/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/Attic/CmsPublishProject.java,v $
 * Date   : $Date: 2003/10/31 17:07:48 $
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
import com.opencms.core.I_CmsConstants;
import com.opencms.file.CmsResource;
import com.opencms.flex.jsp.CmsJspActionElement;
import com.opencms.util.Utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.opencms.main.OpenCms;
import org.opencms.threads.CmsPublishThread;

/**
 * Creates the dialogs for publishing a project or a resource.<p> 
 *
 * @author  Andreas Zahner (a.zahner@alkacon.com)
 * @version $Revision: 1.1 $
 * 
 * @since 5.1.12
 */
public class CmsPublishProject extends CmsReport {
        
    public static final String DIALOG_TYPE = "publishproject";
    // always start individual action id's with 100 to leave enough room for more default actions
    
    public static final int ACTION_UNLOCK_CONFIRMATION = 200;
    public static final int ACTION_UNLOCK_CONFIRMED = 210;
    
    public static final String DIALOG_UNLOCK_CONFIRMATION = "unlockconfirmation";
    public static final String DIALOG_UNLOCK_CONFIRMED = "unlockconfirmed";

    // member variables for publishing a project
    private String m_paramProjectid;
    private String m_paramProjectname;

    // member variables for direct publishing
    private String m_paramDirectpublish;
    private String m_paramResourcename;
    private String m_paramModifieddate;
    private String m_paramModifieduser;
    private String m_paramPublishsiblings;

    /**
     * Public constructor.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsPublishProject(CmsJspActionElement jsp) {
        super(jsp);
    }
    
    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */
    public CmsPublishProject(PageContext context, HttpServletRequest req, HttpServletResponse res) {
        this(new CmsJspActionElement(context, req, res));
    } 
    
    /**
     * Returns if a resource will be directly published.<p>
     * 
     * @return "true" if a resource will be directly published
     */
    public String getParamDirectpublish() {
        return m_paramDirectpublish;
    }  
    
    /**
     * Sets if a resource will be directly published.<p>
     * 
     * @param value "true" (String) if a resource will be directly published
     */
    public void setParamDirectpublish(String value) {
        m_paramDirectpublish = value;
    }
    
    /**
     * Returns the name of the resource which will be published.<p>
     * 
     * @return the name of the resource
     */
    public String getParamResourcename() {
        return m_paramResourcename;
    }

    /**
     * Sets the name of the resource which will be published.<p> 
     * 
     * @param value the name of the resource
     */
    public void setParamResourcename(String value) {
        m_paramResourcename = value;
    } 
    
    /**
     * Returns the last modification date of the resource which will be published.<p>
     * 
     * @return the last modification date of the resource
     */
    public String getParamModifieddate() {
        return m_paramModifieddate;
    }

    /**
     * Sets the last modification date of the resource which will be published.<p> 
     * 
     * @param value the last modification date of the resource
     */
    public void setParamModifieddate(String value) {
        m_paramModifieddate = value;
    } 
    
    /**
     * Returns the user who modified the resource which will be published.<p>
     * 
     * @return the user who modified the resource
     */
    public String getParamModifieduser() {
        return m_paramModifieduser;
    }

    /**
     * Sets the user who modified the resource which will be published.<p> 
     * 
     * @param value the user who modified the resource
     */
    public void setParamModifieduser(String value) {
        m_paramModifieduser = value;
    } 
    
    /**
     * Returns if siblings of the resource should be published.<p>
     * 
     * @return "true" (String) if siblings of the resource should be published
     */
    public String getParamPublishsiblings() {
        return m_paramPublishsiblings;
    }

    /**
     * Sets if siblings of the resource should be published.<p> 
     * 
     * @param value "true" (String) if siblings of the resource should be published
     */
    public void setParamPublishsiblings(String value) {
        m_paramPublishsiblings = value;
    } 
    
    /**
     * Returns the value of the project id which will be published.<p>
     * 
     * @return the String value of the project id
     */
    public String getParamProjectid() {
        return m_paramProjectid;
    }
    
    /**
     * Sets the value of the project id which will be published.<p> 
     * 
     * @param value the String value of the project id
     */
    public void setParamProjectid(String value) {
        m_paramProjectid = value;
    } 
    
    /**
     * Returns the value of the project name which will be published.<p>
     * 
     * @return the String value of the project name
     */
    public String getParamProjectname() {
        return m_paramProjectname;
    }

    /**
     * Sets the value of the project name which will be published.<p> 
     * 
     * @param value the String value of the project name
     */
    public void setParamProjectname(String value) {
        m_paramProjectname = value;
    } 
        
    /**
     * Performs the move report, will be called by the JSP page.<p>
     * 
     * @throws JspException if problems including sub-elements occur
     */
    public void actionReport() throws JspException {
        // save initialized instance of this class in request attribute for included sub-elements
        getJsp().getRequest().setAttribute(C_SESSION_WORKPLACE_CLASS, this);
        switch (getAction()) {
            case ACTION_REPORT_UPDATE:
                setParamAction(REPORT_UPDATE);   
                getJsp().include(C_FILE_REPORT_OUTPUT);  
                break;
            case ACTION_REPORT_BEGIN:
            case ACTION_CONFIRMED:
            default:
                if (showUnlockConfirmation()) {   
                    // some resources are locked, unlock them before publishing          
                    try {
                        if ("true".equals(getParamDirectpublish())) {
                            String folderName = getParamResource();
                            if (!folderName.endsWith("/")) {
                                folderName += "/";
                            }
                            getCms().lockResource(folderName);
                            getCms().unlockResource(folderName, false);
                        } else {
                            getCms().unlockProject(Integer.parseInt(getParamProjectid()));                               
                        } 
                    } catch (CmsException e) {
                        // error while unlocking resources, show error screen
                        setParamErrorstack(e.getStackTraceAsString());
                        setParamMessage(key("error.message.projectlockchange"));
                        setParamReasonSuggestion(key("error.reason.projectlockchange") + "<br>\n" + key("error.suggestion.projectlockchange"));
                        getJsp().include(C_FILE_DIALOG_SCREEN_ERROR);
                    }        
                }
                
                // start different publish threads for direct publish and publish project             
                CmsPublishThread thread = null;
                if ("true".equals(getParamDirectpublish())) {
                    thread = new CmsPublishThread(getCms(), getParamResource(), "true".equals(getParamPublishsiblings()));
                } else {
                    thread = new CmsPublishThread(getCms());
                }
                thread.start();
                setParamAction(REPORT_BEGIN);
                setParamThread(thread.getId().toString());
                getJsp().include(C_FILE_REPORT_OUTPUT);  
                break;
        }
    }
        
    /**
     * @see org.opencms.workplace.CmsWorkplace#initWorkplaceRequestValues(org.opencms.workplace.CmsWorkplaceSettings, javax.servlet.http.HttpServletRequest)
     */
    protected void initWorkplaceRequestValues(CmsWorkplaceSettings settings, HttpServletRequest request) {
        // fill the parameter values in the get/set methods
        fillParamValues(request);
        // set the dialog type
        setParamDialogtype(DIALOG_TYPE);
        // set the publishing type: publish project or direct publish
        if (getParamResource() != null && !"".equals(getParamResource())) {
            setParamDirectpublish("true");
        }       
        // set the action for the JSP switch 
        if (DIALOG_CONFIRMED.equals(getParamAction())) {
            if (showUnlockConfirmation()) {
                // show unlock confirmation dialog
                setAction(ACTION_UNLOCK_CONFIRMATION);
            } else {
                // skip unlock confirmation dialog
                setAction(ACTION_CONFIRMED);
            }
        } else if (DIALOG_UNLOCK_CONFIRMED.equals(getParamAction())) {
            setAction(ACTION_CONFIRMED);   
        } else if (REPORT_UPDATE.equals(getParamAction())) {
            setAction(ACTION_REPORT_UPDATE);         
        } else if (REPORT_BEGIN.equals(getParamAction())) {
            setAction(ACTION_REPORT_BEGIN);
        } else if (REPORT_END.equals(getParamAction())) {
            setAction(ACTION_REPORT_END);
        } else {                        
            setAction(ACTION_DEFAULT);
            // set parameters depending on publishing type
            if ("true".equals(getParamDirectpublish())) {
                // determine resource name, last modified date and last modified user of resource
                computePublishResource();
                // add the title for the direct publish dialog 
                setParamTitle(key("messagebox.title.publishresource") + ": " + getParamResourcename());
            } else {
                // add the title for the publish project dialog 
                setParamTitle(key("project.publish.title"));
                // determine the project id and name for publishing
                computePublishProject(); 
            }
        }                 
    }
    
    /**
     * Determine the right project id and name if no request parameter "projectid" is given.<p>
     */
    private void computePublishProject() {
        String projectId = getParamProjectid();
        int id;
        if (projectId == null || "".equals(projectId.trim())) {
            // projectid not found in request parameter, 
            id = getCms().getRequestContext().currentProject().getId();
            setParamProjectname(getCms().getRequestContext().currentProject().getName());
            setParamProjectid("" + id);
        } else {
            id = Integer.parseInt(projectId);
            try {
                setParamProjectname(getCms().readProject(id).getName());
            } catch (CmsException e) { }
        }
    }
    
    /**
     * Fills the resource information "resource name", "date last modified" and "last modified by" in parameter values.<p>
     */
    private void computePublishResource() {
        try {
            CmsResource res = getCms().readFileHeader(getParamResource(), true);
            setParamResourcename(res.getName());
            setParamModifieddate(Utils.getNiceDate(res.getDateLastModified()));
            setParamModifieduser(getCms().readUser(res.getUserLastModified()).getName());
        } catch (CmsException e) { }
    }
    
    /**
     * Checks if the unlock confirmation dialog should be displayed.<p>
     * 
     * @return true if some resources of the project are locked, otherwise false 
     */
    private boolean showUnlockConfirmation() {
        try {
            if ("true".equals(getParamDirectpublish())) {
                // direct publish: check sub resources of a folder
                CmsResource res = getCms().readFileHeader(getParamResource());
                if (res.getState() != I_CmsConstants.C_STATE_DELETED && res.isFolder()) {
                    return (getCms().countLockedResources(getParamResource()) > 0);
                }               
            } else {
                // publish project: check all project resources
                int id = Integer.parseInt(getParamProjectid());
                return (getCms().countLockedResources(id) > 0);
            }
        } catch (CmsException e) { }
        return false;
    }
    
    /**
     * Builds the HTML for the "publish siblings" checkbox when direct publishing a file.<p>
     * 
     * @return the HTMl for the "publish siblings" checkbox  
     */
    public String buildCheckSiblings() {
        CmsResource res = null;
        try {
            res = getCms().readFileHeader(getParamResource());
        } catch (CmsException e) { }
        if (res != null && res.isFile() && res.getLinkCount() > 1) {
            // resource is file and has siblings, so create checkbox
            StringBuffer retValue = new StringBuffer(128);
            retValue.append("<tr>\n\t<td>");
            retValue.append("<input type=\"checkbox\" name=\"publishsiblings\" value=\"true\"");
            // set the checkbox state to the default value defined in the opencms.properties
            String directPublishSiblings = (String)OpenCms.getRuntimeProperty("workplace.directpublish.siblings");
            if ("true".equals(directPublishSiblings)) {
                retValue.append(" checked=\"checked\"");
            }
            retValue.append(">&nbsp;");
            retValue.append(key("messagebox.message5.publishresource"));
            retValue.append("</td>\n</tr>\n");
            return retValue.toString();
        }
        return "";
    }
    
}
