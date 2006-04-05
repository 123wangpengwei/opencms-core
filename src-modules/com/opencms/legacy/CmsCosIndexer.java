/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/com/opencms/legacy/Attic/CmsCosIndexer.java,v $
 * Date   : $Date: 2006/03/27 14:53:03 $
 * Version: $Revision: 1.15 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002  Alkacon Software (http://www.alkacon.com)
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

package com.opencms.legacy;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsRequestContext;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.report.I_CmsReport;
import org.opencms.search.A_CmsIndexResource;
import org.opencms.search.CmsIndexException;
import org.opencms.search.CmsSearchIndexUpdateData;
import org.opencms.search.CmsIndexingThreadManager;
import org.opencms.search.CmsSearchDocumentType;
import org.opencms.search.CmsSearchIndex;
import org.opencms.search.CmsSearchIndexSource;
import org.opencms.search.I_CmsIndexer;
import org.opencms.search.documents.I_CmsDocumentFactory;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.CmsUUID;

import com.opencms.defaults.master.*;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

/**
 * Implements the indexing of cos data.<p>
 * 
 * @version $Revision: 1.15 $ $Date: 2006/03/27 14:53:03 $
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @since 5.3.1
 * 
 * @deprecated Will not be supported past the OpenCms 6 release.
 */
public class CmsCosIndexer extends CmsMasterContent implements I_CmsIndexer {

    /** Constant for display parameter. */
    public static final String C_PARAM_CHANNEL_DISPLAY_PARAM = "displayparam";

    /** Constant for display uri. */
    public static final String C_PARAM_CHANNEL_DISPLAY_URI = "displayuri";

    /** General class to handle module content. */
    private CmsMasterContent m_contentDefinition;

    /** The current index. */
    private CmsSearchIndex m_index;

    /** The search index source. */
    private CmsSearchIndexSource m_indexSource;

    /** The report. */
    private I_CmsReport m_report;

    /** The id for identifying module content. */
    private int m_subId;

    /** The thread manager. */
    private CmsIndexingThreadManager m_threadManager;

    /** The index writer. */
    private IndexWriter m_writer;

    /**
     * @see org.opencms.search.I_CmsIndexer#deleteResources(org.apache.lucene.index.IndexReader, java.util.List)
     */
    public void deleteResources(IndexReader reader, List resourcesToDelete) {

        // NYI
    }

    /**
     * @see org.opencms.search.I_CmsIndexer#getIndexResource(CmsObject, org.apache.lucene.document.Document)
     */
    public A_CmsIndexResource getIndexResource(CmsObject cms, Document doc) throws CmsException {

        Field f = null;
        String channel = null;
        A_CmsIndexResource result = null;

        if ((f = doc.getField(CmsCosDocument.DOC_CHANNEL)) != null) {
            channel = f.stringValue();

            if (channel != null) {
                result = readResource(cms, doc);
            }
        }

        return result;
    }

    /**
     * Just to fulfill implementation requirements of CmsMasterContent.<p>
     * 
     * @see com.opencms.defaults.master.CmsMasterContent#getSubId()
     */
    public int getSubId() {

        return 0;
    }

    /**
     * @see org.opencms.search.I_CmsIndexer#getUpdateData(org.opencms.search.CmsSearchIndexSource, java.util.List)
     */
    public CmsSearchIndexUpdateData getUpdateData(CmsSearchIndexSource source, List publishedResources) {

        return new CmsSearchIndexUpdateData(source, this);
    }

    /**
     * @see org.opencms.search.I_CmsIndexer#newInstance(org.opencms.file.CmsObject, org.opencms.report.I_CmsReport, org.opencms.search.CmsSearchIndex)
     */
    public I_CmsIndexer newInstance(CmsObject cms, I_CmsReport report, CmsSearchIndex index) {

        CmsCosIndexer result = new CmsCosIndexer();
        result.m_cms = cms;
        result.m_report = report;
        result.m_index = index;
        return result;
    }

    /**
     * Reads the data of a cos resource specified by the given search document.<p>
     * 
     * @param cms the cms object
     * @param doc the document retrived from search index
     * @return the cos data
     * @throws CmsException if something goes wrong
     */
    public A_CmsIndexResource readResource(CmsObject cms, Document doc) throws CmsException {

        try {
            String channel = doc.getField(CmsCosDocument.DOC_CHANNEL).stringValue();
            String path = doc.getField(I_CmsDocumentFactory.DOC_PATH).stringValue();
            String cdClass = doc.getField(CmsCosDocument.DOC_CONTENT_DEFINITION).stringValue();
            String contentId = doc.getField(CmsCosDocument.DOC_CONTENT_ID).stringValue();

            Class clazz = Class.forName(cdClass);
            CmsMasterContent contentDefinition = (CmsMasterContent)clazz.getDeclaredConstructor(
                new Class[] {org.opencms.file.CmsObject.class}).newInstance(new Object[] {cms});

            CmsMasterDataSet ds = new CmsMasterDataSet();
            CmsMasterContent.getDbAccessObject(contentDefinition.getSubId()).read(
                cms,
                contentDefinition,
                ds,
                new CmsUUID(contentId));

            if (ds != null) {
                return new CmsCosIndexResource(ds, path, channel, cdClass);
            }

            return null;

        } catch (Exception exc) {
            throw new CmsLegacyException("Instanciation of index resource failed", exc);
        }
    }

    /**
     * @see org.opencms.search.I_CmsIndexer#rebuildIndex(org.apache.lucene.index.IndexWriter, org.opencms.search.CmsIndexingThreadManager, org.opencms.search.CmsSearchIndexSource)
     */
    public void rebuildIndex(IndexWriter writer, CmsIndexingThreadManager threadManager, CmsSearchIndexSource source)
    throws CmsIndexException {

        m_writer = writer;
        m_indexSource = source;
        m_threadManager = threadManager;

        String resourceType = (String)m_indexSource.getDocumentTypes().get(0);
        CmsSearchDocumentType documentType = OpenCms.getSearchManager().getDocumentTypeConfig(resourceType);
        String cdClassName = (String)documentType.getResourceTypes().get(0);

        try {
            m_contentDefinition = (CmsMasterContent)Class.forName(cdClassName).newInstance();
        } catch (Exception e) {
            throw new CmsIndexException(Messages.get().container(
                Messages.ERR_COS_INDEXING_CONTENTS_OF_CLASS_1,
                cdClassName), e);
        }
        m_subId = m_contentDefinition.getSubId();

        List resourceNames = source.getResourcesNames();
        Iterator it = resourceNames.iterator();
        while (it.hasNext()) {
            // read the resources from all configured source folders
            String channel = (String)it.next();
            internalUpdateIndex(channel, channel);
        }
    }

    /**
     * @see org.opencms.search.I_CmsIndexer#updateResources(org.apache.lucene.index.IndexWriter, org.opencms.search.CmsIndexingThreadManager, java.util.List)
     */
    public void updateResources(IndexWriter writer, CmsIndexingThreadManager threadManager, List resourcesToUpdate) {

        // NYI
    }

    /**
     * Returns the uuid of the channel.<p>
     * 
     * @param cms the current user's CmsObject
     * @param channelName name of the channel 
     * @return the uuid of the channel
     * @throws CmsIndexException if something goes wrong
     */
    protected CmsUUID getChannelId(CmsObject cms, String channelName) throws CmsIndexException {

        String siteRoot = cms.getRequestContext().getSiteRoot();
        CmsUUID id = null;
        try {
            cms.getRequestContext().setSiteRoot(CmsResource.VFS_FOLDER_CHANNELS);
            CmsResource channel = cms.readFolder(channelName, CmsResourceFilter.IGNORE_EXPIRATION);
            id = channel.getResourceId();
        } catch (Exception exc) {
            throw new CmsIndexException(Messages.get().container(Messages.ERR_COS_ACCESS_CHANNEL_1, channelName), exc);
        } finally {
            cms.getRequestContext().setSiteRoot(siteRoot);
        }
        return id;
    }

    /**
     * Creates new index entries for all cos resources below the given path.<p>
     * 
     * @param channel the channel to index
     * @param root the root channel
     * 
     * @throws CmsIndexException if something goes wrong
     */
    protected void internalUpdateIndex(String channel, String root) throws CmsIndexException {

        boolean channelReported = false;
        CmsProject currentProject = null;
        CmsRequestContext context = m_cms.getRequestContext();

        try {
            // save the current project
            currentProject = context.currentProject();
            // switch to the configured project
            context.setCurrentProject(m_cms.readProject(m_index.getProject()));

            String channelId = getChannelId(m_cms, channel).toString();
            Vector subChannels = CmsMasterContent.getAllSubChannelsOf(m_cms, channel);

            // index subchannels
            for (int i = 0; i < subChannels.size(); i++) {

                String subChannel = (String)subChannels.get(i);
                internalUpdateIndex(subChannel, root);
            }

            // now index channel
            Vector resources = readAllByChannel(m_cms, channelId, m_subId);
            for (Iterator i = resources.iterator(); i.hasNext();) {

                CmsMasterDataSet ds = (CmsMasterDataSet)i.next();

                if (m_report != null && !channelReported) {

                    m_report.print(Messages.get().container(Messages.RPT_INDEX_CHANNEL_0), I_CmsReport.FORMAT_NOTE);
                    m_report.println(org.opencms.report.Messages.get().container(
                        org.opencms.report.Messages.RPT_ARGUMENT_1,
                        channel));
                    channelReported = true;
                }

                if (m_report != null) {
                    m_report.print(org.opencms.report.Messages.get().container(
                        org.opencms.report.Messages.RPT_SUCCESSION_1,
                        String.valueOf(m_threadManager.getCounter() + 1)), I_CmsReport.FORMAT_NOTE);
                    m_report.print(org.opencms.search.Messages.get().container(
                        org.opencms.search.Messages.RPT_SEARCH_INDEXING_FILE_BEGIN_0), I_CmsReport.FORMAT_NOTE);
                    if (ds.m_title != null) {
                        String title = ds.m_title;
                        title = CmsStringUtil.substitute(title, "'", "\\'");
                        title = CmsStringUtil.substitute(title, "\"", "\\\"");
                        m_report.print(org.opencms.report.Messages.get().container(
                            org.opencms.report.Messages.RPT_ARGUMENT_1,
                            title));
                    }
                    m_report.print(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_DOTS_0));
                }

                String path = m_indexSource.getParam(C_PARAM_CHANNEL_DISPLAY_URI)
                    + "?"
                    + m_indexSource.getParam(C_PARAM_CHANNEL_DISPLAY_PARAM)
                    + "="
                    + ds.m_masterId;

                A_CmsIndexResource ires = new CmsCosIndexResource(
                    ds,
                    path,
                    channel,
                    m_contentDefinition.getClass().getName());
                m_threadManager.createIndexingThread(m_cms, m_writer, ires, m_index);
            }
        } catch (Exception exc) {

            if (m_report != null) {
                m_report.println(
                    org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_FAILED_0),
                    I_CmsReport.FORMAT_WARNING);
            }
            if (CmsLog.getLog(this).isWarnEnabled()) {
                CmsLog.getLog(this).warn("Failed to index " + channel, exc);
            }

            throw new CmsIndexException(
                Messages.get().container(Messages.ERR_COS_INDEXING_CONTENTS_OF_CLASS_1, channel),
                exc);
        } finally {

            // switch back to the original project
            context.setCurrentProject(currentProject);
        }
    }
}