/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/workplace/tools/scheduler/CmsSchedulerList.java,v $
 * Date   : $Date: 2005/05/23 15:40:38 $
 * Version: $Revision: 1.13 $
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

package org.opencms.workplace.tools.scheduler;

import org.opencms.configuration.CmsSystemConfiguration;
import org.opencms.i18n.CmsMessageContainer;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsException;
import org.opencms.main.CmsRuntimeException;
import org.opencms.main.OpenCms;
import org.opencms.scheduler.CmsScheduledJobInfo;
import org.opencms.security.CmsRoleViolationException;
import org.opencms.workplace.CmsDialog;
import org.opencms.workplace.list.A_CmsListDialog;
import org.opencms.workplace.list.CmsListColumnAlignEnum;
import org.opencms.workplace.list.CmsListColumnDefinition;
import org.opencms.workplace.list.CmsListDateMacroFormatter;
import org.opencms.workplace.list.CmsListDefaultAction;
import org.opencms.workplace.list.CmsListDirectAction;
import org.opencms.workplace.list.CmsListIndependentAction;
import org.opencms.workplace.list.CmsListItem;
import org.opencms.workplace.list.CmsListItemActionIconComparator;
import org.opencms.workplace.list.CmsListItemDefaultComparator;
import org.opencms.workplace.list.CmsListItemDetails;
import org.opencms.workplace.list.CmsListItemDetailsFormatter;
import org.opencms.workplace.list.CmsListMetadata;
import org.opencms.workplace.list.CmsListMultiAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

/**
 * Main scheduled jobs management list view.<p>
 * 
 * Defines the list columns and possible actions for scheduled jobs.<p>
 * 
 * @author Michael Moossen (m.moossen@alkacon.com)
 * @author Andreas Zahner (a.zahner@alkacon.com) 
 * @version $Revision: 1.13 $
 * @since 5.7.3
 */
public class CmsSchedulerList extends A_CmsListDialog {

    /** List action activate. */
    public static final String LIST_ACTION_ACTIVATE = "activate";
    
    /** List action copy. */
    public static final String LIST_ACTION_COPY = "copy";
    
    /** List action deactivate. */
    public static final String LIST_ACTION_DEACTIVATE = "deactivate";
    
    /** List action delete. */
    public static final String LIST_ACTION_DELETE = "delete";
    
    /** List action edit. */
    public static final String LIST_ACTION_EDIT = "edit";
    
    /** List action multi activate. */
    public static final String LIST_ACTION_MACTIVATE = "mactivate";
    
    /** List action multi deactivate. */
    public static final String LIST_ACTION_MDEACTIVATE = "mdeactivate";
    
    /** List action multi delete. */
    public static final String LIST_ACTION_MDELETE = "mdelete";
    
    /** List column activate. */
    public static final String LIST_COLUMN_ACTIVATE = "activate";
    
    /** List column class. */
    public static final String LIST_COLUMN_CLASS = "class";
    
    /** List column copy. */
    public static final String LIST_COLUMN_COPY = "copy";
    
    /** List column delete. */
    public static final String LIST_COLUMN_DELETE = "delete";
    
    /** List column edit. */
    public static final String LIST_COLUMN_EDIT = "editcol";
    
    /** List column last execution. */
    public static final String LIST_COLUMN_LASTEXE = "lastexe";
    
    /** List column name. */
    public static final String LIST_COLUMN_NAME = "name";
    
    /** List column next execution. */
    public static final String LIST_COLUMN_NEXTEXE = "nextexe";
    
    /** List detail context info. */
    public static final String LIST_DETAIL_CONTEXTINFO = "contextinfo";
    
    /** List detail parameter. */
    public static final String LIST_DETAIL_PARAMETER = "parameter";
    
    /** List ID. */
    public static final String LIST_ID = "jobs";
    
    /** Path to the list buttons. */
    public static final String PATH_BUTTONS = "tools/scheduler/buttons/";
    
    /** Path to the list icons. */
    public static final String PATH_ICONS = "tools/scheduler/icons/";

    /**
     * Public constructor.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsSchedulerList(CmsJspActionElement jsp) {

        super(
            jsp,
            LIST_ID,
            new CmsMessageContainer(Messages.get(), Messages.GUI_JOBS_LIST_NAME_0),
            LIST_COLUMN_NAME,
            LIST_COLUMN_NAME);
    }

    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */
    public CmsSchedulerList(PageContext context, HttpServletRequest req, HttpServletResponse res) {

        this(new CmsJspActionElement(context, req, res));
    }

    /**
     * This method should handle every defined list multi action,
     * by comparing <code>{@link #getParamListAction()}</code> with the id 
     * of the action to execute.<p> 
     * 
     * @throws CmsRuntimeException to signal that an action is not supported
     * 
     */
    public void executeListMultiActions() throws CmsRuntimeException {
        CmsListItem listItem = null;
        if (getParamListAction().equals(LIST_ACTION_MDELETE)) {
            // execute the delete multiaction
            try {
                Iterator itItems = getSelectedItems().iterator();
                while (itItems.hasNext()) {
                    listItem = (CmsListItem)itItems.next();
                    OpenCms.getScheduleManager().unscheduleJob(getCms(), listItem.getId());
                    getList().removeItem(listItem.getId());
                }
                // update the XML configuration
                writeConfiguration(false);
            } catch (CmsException e) {
                throw new CmsRuntimeException(Messages.get().container(
                    Messages.ERR_UNSCHEDULE_JOB_1,
                    (listItem == null) ? (Object)"?" : new Integer(listItem.getId())), e);
            }
        } else if (getParamListAction().equals(LIST_ACTION_MACTIVATE)
            || getParamListAction().equals(LIST_ACTION_MDEACTIVATE)) {
                // execute the activate or deactivate multiaction
                try {
                    Iterator itItems = getSelectedItems().iterator();
                    boolean activate = getParamListAction().equals(LIST_ACTION_MACTIVATE);
                    while (itItems.hasNext()) {
                        // toggle the active state of the selected item(s)
                        listItem = (CmsListItem)itItems.next();                     
                        CmsScheduledJobInfo job = (CmsScheduledJobInfo)OpenCms.getScheduleManager().getJob(listItem.getId()).clone();
                        job.setActive(activate);
                        OpenCms.getScheduleManager().scheduleJob(getCms(), job);
                    }
                    // update the XML configuration
                    writeConfiguration(true);
                } catch (CmsException e) {
                    throw new CmsRuntimeException(Messages.get().container(
                    Messages.ERR_SCHEDULE_JOB_1,
                    (listItem == null) ? (Object)"?" : new Integer(listItem.getId())), e);
                }
        } else {
            throwListUnsupportedActionException();
        }
        listSave();
    }

    /**
     * This method should handle every defined list single action,
     * by comparing <code>{@link #getParamListAction()}</code> with the id 
     * of the action to execute.<p> 
     * 
     * @throws CmsRuntimeException to signal that an action is not supported or failed
     * 
     */
    public void executeListSingleActions() throws CmsRuntimeException {

        if (getParamListAction().equals(LIST_ACTION_EDIT)) {
            // edit a job from the list
            String jobId = getSelectedItem().getId();
            try {
                // forward to the edit job screen with additional parameters               
                Map params = new HashMap();
                params.put(CmsEditScheduledJobInfoDialog.PARAM_JOBID, jobId);
                String jobName = (String)getSelectedItem().get(LIST_COLUMN_NAME);
                params.put(CmsEditScheduledJobInfoDialog.PARAM_JOBNAME, jobName);
                // set action parameter to initial dialog call
                params.put(CmsDialog.PARAM_ACTION, CmsDialog.DIALOG_INITIAL);
                getToolManager().jspRedirectTool(this, "/scheduler/edit", params);
            } catch (IOException e) {
                // should never happen
                throw new CmsRuntimeException(Messages.get().container(
                    Messages.ERR_EDIT_JOB_1,
                    jobId), e);
            }
        } else if (getParamListAction().equals(LIST_ACTION_COPY)) {
            // copy a job from the list
            String jobId = getSelectedItem().getId();
            try {
                // forward to the edit job screen with additional parameters
                Map params = new HashMap();
                params.put(CmsEditScheduledJobInfoDialog.PARAM_JOBID, jobId);
                // set action parameter to copy job action
                params.put(CmsDialog.PARAM_ACTION, CmsEditScheduledJobInfoDialog.DIALOG_COPYJOB);
                getToolManager().jspRedirectTool(this, "/scheduler/new", params);
            } catch (IOException e) {
                // should never happen
                throw new CmsRuntimeException(Messages.get().container(
                    Messages.ERR_COPY_JOB_1,
                    jobId), e);
            }
        } else if (getParamListAction().equals(LIST_ACTION_ACTIVATE)) {
            // activate a job from the list
            String jobId = getSelectedItem().getId();
            CmsScheduledJobInfo job = (CmsScheduledJobInfo)OpenCms.getScheduleManager().getJob(jobId).clone();
            job.setActive(true);
            try {
                OpenCms.getScheduleManager().scheduleJob(getCms(), job);
                // update the XML configuration
                writeConfiguration(true);
            } catch (CmsException e) {
                // should never happen
                throw new CmsRuntimeException(Messages.get().container(
                    Messages.ERR_SCHEDULE_JOB_1,
                    jobId), e);
            }
        } else if (getParamListAction().equals(LIST_ACTION_DEACTIVATE)) {
            // deactivate a job from the list
            String jobId = getSelectedItem().getId();
            CmsScheduledJobInfo job = (CmsScheduledJobInfo)OpenCms.getScheduleManager().getJob(jobId).clone();
            job.setActive(false);
            try {
                OpenCms.getScheduleManager().scheduleJob(getCms(), job);
                // update the XML configuration
                writeConfiguration(true);
            } catch (CmsException e) {
                // should never happen
                throw new CmsRuntimeException(Messages.get().container(
                    Messages.ERR_UNSCHEDULE_JOB_1,
                    jobId), e);
            }
        } else if (getParamListAction().equals(LIST_ACTION_DELETE)) {
            // delete a job from the list
            String jobId = getSelectedItem().getId();        
            try {
                OpenCms.getScheduleManager().unscheduleJob(getCms(), jobId);
                // update the XML configuration
                writeConfiguration(false);
                getList().removeItem(jobId);
            } catch (CmsRoleViolationException e) {
                // should never happen
                throw new CmsRuntimeException(Messages.get().container(
                    Messages.ERR_DELETE_JOB_1,
                    jobId), e);
            }
        } else {
            throwListUnsupportedActionException();
        }
        listSave();
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#getListItems()
     */
    protected List getListItems() {

        List items = new ArrayList();
        
        // get all scheduled jobs from manager
        Iterator i = OpenCms.getScheduleManager().getJobs().iterator();
        while (i.hasNext()) {
            CmsScheduledJobInfo job = (CmsScheduledJobInfo)i.next();
            CmsListItem item = getList().newItem(job.getId().toString());
            // set the contents of the columns
            item.set(LIST_COLUMN_NAME, job.getJobName());
            item.set(LIST_COLUMN_CLASS, job.getClassName());
            item.set(LIST_COLUMN_LASTEXE, job.getExecutionTimePrevious());
            item.set(LIST_COLUMN_NEXTEXE, job.getExecutionTimeNext());
            // job details: context info
            item.set(LIST_DETAIL_CONTEXTINFO, job.getContextInfo());
            // job details: parameter
            StringBuffer params = new StringBuffer(32);
            Iterator paramIt = job.getParameters().keySet().iterator();
            while (paramIt.hasNext()) {
                String param = (String)paramIt.next();
                String value = (String)job.getParameters().get(param);
                params.append(param).append("=");
                params.append(value).append("<br>");
            }
            item.set(LIST_DETAIL_PARAMETER, params);
            
            items.add(item);
        }
        
        return items;
    }
    
    /**
     * @see org.opencms.workplace.CmsWorkplace#initMessages()
     */
    protected void initMessages() {

        // add specific messages
        addMessages(Messages.get().getBundleName());
        // add default messages
        super.initMessages();
    }
    
    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#setColumns(org.opencms.workplace.list.CmsListMetadata)
     */
    protected void setColumns(CmsListMetadata metadata) {

        // add column for edit action
        CmsListColumnDefinition editCol = new CmsListColumnDefinition(LIST_COLUMN_EDIT);
        editCol.setName(Messages.get().container(Messages.GUI_JOBS_LIST_COL_EDIT_0));
        editCol.setHelpText(Messages.get().container(Messages.GUI_JOBS_LIST_COL_EDIT_HELP_0));
        editCol.setWidth("20");
        editCol.setAlign(CmsListColumnAlignEnum.ALIGN_CENTER);
        editCol.setSorteable(false);
        // create default edit action for edit column: edit job
        CmsListDirectAction editColAction = new CmsListDirectAction(LIST_ID, LIST_ACTION_EDIT);
        editColAction.setName(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_EDIT_NAME_0));
        editColAction.setIconPath(PATH_BUTTONS + "edit.png");
        editColAction.setHelpText(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_EDIT_HELP_0));
        editColAction.setConfirmationMessage(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_EDIT_CONF_0));
        // set action for the edit column
        editCol.addDirectAction(editColAction);
        metadata.addColumn(editCol);

        // add column for activate/deactivate action
        CmsListColumnDefinition activateCol = new CmsListColumnDefinition(LIST_COLUMN_ACTIVATE);
        activateCol.setName(Messages.get().container(Messages.GUI_JOBS_LIST_COL_ACTIVE_0));
        activateCol.setHelpText(Messages.get().container(Messages.GUI_JOBS_LIST_COL_ACTIVE_HELP_0));
        activateCol.setWidth("20");
        activateCol.setAlign(CmsListColumnAlignEnum.ALIGN_CENTER);
        activateCol.setListItemComparator(new CmsListItemActionIconComparator());
        // create direct action to activate/deactivate job
        CmsActionActivateJob activateJob = new CmsActionActivateJob(LIST_ID, LIST_ACTION_ACTIVATE, getCms());
        // direct action: activate job
        CmsListDirectAction userActAction = new CmsListDirectAction(LIST_ID, LIST_ACTION_ACTIVATE);
        userActAction.setName(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_ACTIVATE_NAME_0));
        userActAction.setConfirmationMessage(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_ACTIVATE_CONF_0));
        userActAction.setIconPath(PATH_BUTTONS + "inactive.png");
        userActAction.setHelpText(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_ACTIVATE_HELP_0));
        activateJob.setFirstAction(userActAction);
        // direct action: deactivate job
        CmsListDirectAction userDeactAction = new CmsListDirectAction(LIST_ID, LIST_ACTION_DEACTIVATE);
        userDeactAction.setName(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_DEACTIVATE_NAME_0));
        userDeactAction.setConfirmationMessage(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_DEACTIVATE_CONF_0));
        userDeactAction.setIconPath(PATH_BUTTONS + "active.png");
        userDeactAction.setHelpText(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_DEACTIVATE_HELP_0));
        activateJob.setSecondAction(userDeactAction);
        activateCol.addDirectAction(activateJob);
        metadata.addColumn(activateCol);
        
        // add column for copy action
        CmsListColumnDefinition copyCol = new CmsListColumnDefinition(LIST_COLUMN_COPY);
        copyCol.setName(Messages.get().container(Messages.GUI_JOBS_LIST_COL_COPY_0));
        copyCol.setHelpText(Messages.get().container(Messages.GUI_JOBS_LIST_COL_COPY_HELP_0));
        copyCol.setWidth("20");
        copyCol.setAlign(CmsListColumnAlignEnum.ALIGN_CENTER);
        copyCol.setListItemComparator(null);
        // direct action: copy job
        CmsListDirectAction copyJob = new CmsListDirectAction(LIST_ID, LIST_COLUMN_COPY);
        copyJob.setName(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_COPY_NAME_0));
        copyJob.setConfirmationMessage(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_COPY_CONF_0));
        copyJob.setIconPath(PATH_BUTTONS + "copy.png");
        copyJob.setHelpText(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_COPY_HELP_0));
        copyCol.addDirectAction(copyJob);
        metadata.addColumn(copyCol);
        
        // add column for delete action
        CmsListColumnDefinition delCol = new CmsListColumnDefinition(LIST_COLUMN_DELETE);
        delCol.setName(Messages.get().container(Messages.GUI_JOBS_LIST_COL_DELETE_0));
        delCol.setHelpText(Messages.get().container(Messages.GUI_JOBS_LIST_COL_DELETE_HELP_0));
        delCol.setWidth("20");
        delCol.setAlign(CmsListColumnAlignEnum.ALIGN_CENTER);
        delCol.setListItemComparator(null);
        // direct action: delete job
        CmsListDirectAction delJob = new CmsListDirectAction(LIST_ID, LIST_ACTION_DELETE);
        delJob.setName(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_DELETE_NAME_0));
        delJob.setConfirmationMessage(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_DELETE_CONF_0));
        delJob.setIconPath(PATH_BUTTONS + "delete.png");
        delJob.setHelpText(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_DELETE_HELP_0));
        delCol.addDirectAction(delJob);
        metadata.addColumn(delCol);
        
        // add column for name
        CmsListColumnDefinition nameCol = new CmsListColumnDefinition(LIST_COLUMN_NAME);
        nameCol.setName(Messages.get().container(Messages.GUI_JOBS_LIST_COL_NAME_0));
        nameCol.setWidth("30%");
        nameCol.setAlign(CmsListColumnAlignEnum.ALIGN_LEFT);
        nameCol.setListItemComparator(new CmsListItemDefaultComparator());
        // create default edit action for name column: edit job
        CmsListDefaultAction nameColAction = new CmsListDefaultAction (LIST_ID, LIST_ACTION_EDIT);
        nameColAction.setName(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_EDIT_NAME_0));
        nameColAction.setHelpText(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_EDIT_HELP_0));
        nameColAction.setConfirmationMessage(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_EDIT_CONF_0));
        // set action for the name column
        nameCol.setDefaultAction(nameColAction);
        metadata.addColumn(nameCol);
        
        // add column for class
        CmsListColumnDefinition classCol = new CmsListColumnDefinition(LIST_COLUMN_CLASS);
        classCol.setName(Messages.get().container(Messages.GUI_JOBS_LIST_COL_CLASS_0));
        classCol.setWidth("20%");
        classCol.setAlign(CmsListColumnAlignEnum.ALIGN_LEFT);
        classCol.setListItemComparator(new CmsListItemDefaultComparator());
        metadata.addColumn(classCol);
        
        // add column for last execution time
        CmsListColumnDefinition lastExecCol = new CmsListColumnDefinition(LIST_COLUMN_LASTEXE);
        lastExecCol.setName(Messages.get().container(Messages.GUI_JOBS_LIST_COL_LASTEXE_0));
        lastExecCol.setWidth("25%");
        lastExecCol.setAlign(CmsListColumnAlignEnum.ALIGN_LEFT);
        lastExecCol.setListItemComparator(new CmsListItemDefaultComparator());
        // create date formatter for last execution time
        CmsListDateMacroFormatter listDateFormatter =  new CmsListDateMacroFormatter(Messages.get().container(
            Messages.GUI_JOBS_LIST_COL_LASTEXE_FORMAT_1), Messages.get().container(
            Messages.GUI_JOBS_LIST_COL_LASTEXE_NEVER_0));
        lastExecCol.setFormatter(listDateFormatter);
        metadata.addColumn(lastExecCol);
        
        // add column for next execution time
        CmsListColumnDefinition nextExecCol = new CmsListColumnDefinition(LIST_COLUMN_NEXTEXE);
        nextExecCol.setName(Messages.get().container(Messages.GUI_JOBS_LIST_COL_NEXTEXE_0));
        nextExecCol.setWidth("25%");
        nextExecCol.setAlign(CmsListColumnAlignEnum.ALIGN_LEFT);
        nextExecCol.setListItemComparator(new CmsListItemDefaultComparator());
        // create date formatter for next execution time
        listDateFormatter = new CmsListDateMacroFormatter(Messages.get().container(
            Messages.GUI_JOBS_LIST_COL_NEXTEXE_FORMAT_1), Messages.get().container(
                Messages.GUI_JOBS_LIST_COL_NEXTEXE_NEVER_0));
        nextExecCol.setFormatter(listDateFormatter);
        metadata.addColumn(nextExecCol);
    }
    
    
    
    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#setIndependentActions(org.opencms.workplace.list.CmsListMetadata)
     */
    protected void setIndependentActions(CmsListMetadata metadata) {

        // add independent job context info button
        
        // create show context info action
        CmsListIndependentAction showContextInfoAction = new CmsListIndependentAction(LIST_ID, LIST_DETAIL_CONTEXTINFO);
        showContextInfoAction.setName(Messages.get().container(Messages.GUI_JOBS_DETAIL_SHOW_CONTEXTINFO_NAME_0));
        showContextInfoAction.setIconPath(PATH_BUTTONS + "details_show.png");
        showContextInfoAction.setHelpText(Messages.get().container(Messages.GUI_JOBS_DETAIL_SHOW_CONTEXTINFO_HELP_0));
        // create hide context info action
        CmsListIndependentAction hideContextInfoAction = new CmsListIndependentAction(LIST_ID, LIST_DETAIL_CONTEXTINFO);
        hideContextInfoAction.setName(Messages.get().container(Messages.GUI_JOBS_DETAIL_HIDE_CONTEXTINFO_NAME_0));
        hideContextInfoAction.setIconPath(PATH_BUTTONS + "details_hide.png");
        hideContextInfoAction.setHelpText(Messages.get().container(Messages.GUI_JOBS_DETAIL_HIDE_CONTEXTINFO_HELP_0));
        // create list item detail
        CmsListItemDetails jobsContextInfoDetails = new CmsListItemDetails(LIST_DETAIL_CONTEXTINFO);
        jobsContextInfoDetails.setAtColumn(LIST_COLUMN_NAME);
        jobsContextInfoDetails.setVisible(false);
        jobsContextInfoDetails.setShowAction(showContextInfoAction);
        jobsContextInfoDetails.setHideAction(hideContextInfoAction);
        // create formatter to display context info
        CmsContextInfoDetailsFormatter contextFormatter = new CmsContextInfoDetailsFormatter();
        contextFormatter.setUserMessage(Messages.get().container(Messages.GUI_JOBS_DETAIL_CONTEXTINFO_USER_0));
        contextFormatter.setProjectMessage(Messages.get().container(Messages.GUI_JOBS_DETAIL_CONTEXTINFO_PROJECT_0));
        contextFormatter.setLocaleMessage(Messages.get().container(Messages.GUI_JOBS_DETAIL_CONTEXTINFO_LOCALE_0));
        contextFormatter.setRootSiteMessage(Messages.get().container(Messages.GUI_JOBS_DETAIL_CONTEXTINFO_ROOTSITE_0));
        contextFormatter.setEncodingMessage(Messages.get().container(Messages.GUI_JOBS_DETAIL_CONTEXTINFO_ENCODING_0));
        contextFormatter.setRemoteAddrMessage(Messages.get().container(Messages.GUI_JOBS_DETAIL_CONTEXTINFO_REMADR_0));
        contextFormatter.setRequestedURIMessage(Messages.get().container(Messages.GUI_JOBS_DETAIL_CONTEXTINFO_REQURI_0));
        jobsContextInfoDetails.setFormatter(contextFormatter);
        // add context info item detail to meta data
        metadata.addItemDetails(jobsContextInfoDetails);
        
        // add independent job parameter button
        
        // create show parameter button
        CmsListIndependentAction showParameterAction = new CmsListIndependentAction(LIST_ID, LIST_DETAIL_PARAMETER);
        showParameterAction.setName(Messages.get().container(Messages.GUI_JOBS_DETAIL_SHOW_PARAMETER_NAME_0));
        showParameterAction.setIconPath(PATH_BUTTONS + "details_show.png");
        showParameterAction.setHelpText(Messages.get().container(Messages.GUI_JOBS_DETAIL_SHOW_PARAMETER_HELP_0));
        // create hide parameter button
        CmsListIndependentAction hideParameterAction = new CmsListIndependentAction(LIST_ID, LIST_DETAIL_PARAMETER);
        hideParameterAction.setName(Messages.get().container(Messages.GUI_JOBS_DETAIL_HIDE_PARAMETER_NAME_0));
        hideParameterAction.setIconPath(PATH_BUTTONS + "details_hide.png");
        hideParameterAction.setHelpText(Messages.get().container(Messages.GUI_JOBS_DETAIL_HIDE_PARAMETER_HELP_0));
        // create list item
        CmsListItemDetails jobsParameterDetails = new CmsListItemDetails(LIST_DETAIL_PARAMETER);
        jobsParameterDetails.setAtColumn(LIST_COLUMN_NAME);
        jobsParameterDetails.setVisible(false);
        jobsParameterDetails.setShowAction(showParameterAction);
        jobsParameterDetails.setHideAction(hideParameterAction); 
        // create formatter to display parameters
        jobsParameterDetails.setFormatter(new CmsListItemDetailsFormatter(Messages.get().container(
            Messages.GUI_JOBS_DETAIL_PARAMETER_FORMAT_0)));
        // add parameter item to metadata
        metadata.addItemDetails(jobsParameterDetails);
    }
    
    
    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#setMultiActions(org.opencms.workplace.list.CmsListMetadata)
     */
    protected void setMultiActions(CmsListMetadata metadata) {
        
        // add the activate job multi action
        CmsListMultiAction activateJob = new CmsListMultiAction(LIST_ID, LIST_ACTION_MACTIVATE);
        activateJob.setName(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_MACTIVATE_NAME_0));
        activateJob.setConfirmationMessage(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_MACTIVATE_CONF_0));
        activateJob.setIconPath(PATH_BUTTONS + "multi_activate.png");
        activateJob.setHelpText(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_MACTIVATE_HELP_0));
        metadata.addMultiAction(activateJob);
        
        // add the deactivate job multi action
        CmsListMultiAction deactivateJob = new CmsListMultiAction(LIST_ID, LIST_ACTION_MDEACTIVATE);
        deactivateJob.setName(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_MDEACTIVATE_NAME_0));
        deactivateJob.setConfirmationMessage(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_MDEACTIVATE_CONF_0));
        deactivateJob.setIconPath(PATH_BUTTONS + "multi_deactivate.png");
        deactivateJob.setHelpText(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_MDEACTIVATE_HELP_0));
        metadata.addMultiAction(deactivateJob);
        
        // add the delete job multi action
        CmsListMultiAction deleteJobs = new CmsListMultiAction(LIST_ID, LIST_ACTION_MDELETE);
        deleteJobs.setName(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_MDELETE_NAME_0));
        deleteJobs.setConfirmationMessage(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_MDELETE_CONF_0));
        deleteJobs.setIconPath(PATH_BUTTONS + "multi_delete.png");
        deleteJobs.setHelpText(Messages.get().container(Messages.GUI_JOBS_LIST_ACTION_MDELETE_HELP_0));
        metadata.addMultiAction(deleteJobs);
    }
    
    /**
     * Writes the updated scheduled job info back to the XML configuration file and refreshes the complete list.<p>
     * 
     * @param refresh if true, the list items are refreshed
     */
    protected void writeConfiguration(boolean refresh) {

        // update the XML configuration
        OpenCms.writeConfiguration(CmsSystemConfiguration.class);
        if (refresh) {
            refreshList();
        }
    }

}