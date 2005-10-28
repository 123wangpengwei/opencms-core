/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/threads/Attic/CmsXmlDocumentLinkValidatorThread.java,v $
 * Date   : $Date: 2005/10/28 12:07:37 $
 * Version: $Revision: 1.5.2.1 $
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

package org.opencms.workplace.threads;

import org.opencms.db.CmsPublishList;
import org.opencms.file.CmsObject;
import org.opencms.main.CmsLog;
import org.opencms.report.A_CmsReportThread;
import org.opencms.workplace.CmsWorkplaceSettings;

import org.apache.commons.logging.Log;

/**
 * A report thread for the HTML link validator.<p>
 * 
 * @version $Revision: 1.5.2.1 $ 
 * 
 * @since 6.0.0 
 */
public class CmsXmlDocumentLinkValidatorThread extends A_CmsReportThread {

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsXmlDocumentLinkValidatorThread.class);

    /** A list of cms resources to be published directly.<p> */
    private CmsPublishList m_publishList;

    /** Flag that indicates whether the publish list should be svaed in the workplace settings. */
    private boolean m_savePublishList;

    /** The current user's workplace settings.<p> */
    private CmsWorkplaceSettings m_settings;

    /**
     * Creates a thread that validates the HTML links (hrefs and images) in all unpublished Cms 
     * files of the current (offline) project, if the files resource types implement the interface 
     * {@link org.opencms.validation.I_CmsXmlDocumentLinkValidatable}.<p>
     * 
     * Please refer to the Javadoc of the I_CmsHtmlLinkValidatable interface to see which classes
     * implement this interface (and so, which file types get validated by the HTML link 
     * validator).<p>
     * 
     * The generated Cms publish list is *NOT* saved in the current user's workplace settings.<p>
     * 
     * @param cms the current OpenCms context object
     * @see org.opencms.file.CmsObject#getPublishList()
     */
    public CmsXmlDocumentLinkValidatorThread(CmsObject cms) {

        super(cms, Messages.get().key(
            cms.getRequestContext().getLocale(),
            Messages.GUI_HTML_LINK_VALIDATOR_THREAD_NAME_1,
            new Object[] {cms.getRequestContext().currentProject().getName()}));

        m_publishList = null;
        m_savePublishList = false;
        m_settings = null;

        initHtmlReport(cms.getRequestContext().getLocale());
        start();
    }

    /**
     * Creates a thread that validates the HTML links (hrefs and images) in the list of unpublished Cms 
     * file(s) of the current (offline) project, if the file's resource type implements the interface 
     * {@link org.opencms.validation.I_CmsXmlDocumentLinkValidatable}.<p>
     * 
     * Please refer to the Javadoc of the I_CmsHtmlLinkValidatable interface to see which classes
     * implement this interface (and so, which file types get validated by the HTML link 
     * validator).<p>
     * 
     * The Cms publish list *IS* saved in the current user's workplace settings for 
     * further processing by other threads. The last thread processing this publish list *MUST* 
     * ensure that the publish list gets removed from the current user's workplace settings!<p>
     * 
     * @param cms the current OpenCms context object
     * @param publishList the list of resources which will be directly published
     * @param settings the current user's workplace settings
     */
    public CmsXmlDocumentLinkValidatorThread(
        CmsObject cms,
        CmsPublishList publishList,
        CmsWorkplaceSettings settings) {

        super(cms, Messages.get().key(
            cms.getRequestContext().getLocale(),
            Messages.GUI_HTML_LINK_VALIDATOR_THREAD_NAME_1,
            new Object[] {cms.getRequestContext().currentProject().getName()}));
        m_publishList = publishList;
        m_savePublishList = true;
        m_settings = settings;

        initHtmlReport(cms.getRequestContext().getLocale());
        start();
    }

    /**
     * @see org.opencms.report.A_CmsReportThread#getReportUpdate()
     */
    public String getReportUpdate() {

        return getReport().getReportUpdate();
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        CmsPublishList publishList = null;

        try {
            // get the list of resources that actually get published
            if (m_publishList == null) {
                publishList = getCms().getPublishList();
            } else {
                publishList = m_publishList;
            }

            // validate the HTML links in these resources
            getCms().validateHtmlLinks(publishList, getReport());

            if (m_savePublishList && m_settings != null) {
                // save the publish list optionally to be processed by further workplace threads
                m_settings.setPublishList(publishList);
            }
        } catch (Exception e) {
            if (m_savePublishList && m_settings != null) {
                // overwrite the publish list in any case with null
                m_settings.setPublishList(null);
            }

            getReport().println(e);
            LOG.error(Messages.get().key(Messages.ERR_LINK_VALIDATION_0), e);
        }
    }

}
