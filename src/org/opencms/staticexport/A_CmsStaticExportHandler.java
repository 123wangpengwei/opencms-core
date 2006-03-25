/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/staticexport/A_CmsStaticExportHandler.java,v $
 * Date   : $Date: 2006/03/25 22:42:37 $
 * Version: $Revision: 1.1.2.2 $
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

package org.opencms.staticexport;

import org.opencms.db.CmsPublishedResource;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.CmsVfsResourceNotFoundException;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.report.I_CmsReport;
import org.opencms.security.CmsSecurityException;
import org.opencms.util.CmsFileUtil;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.CmsUUID;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;

/**
 * Abstract base implementation for the <code>{@link I_CmsStaticExportHandler}</code> interface.<p>
 * 
 * This class provides several util methods to be used by static export handlers.
 * 
 * @author Michael Emmerich
 * 
 * @version $Revision: 1.1.2.2 $ 
 * 
 * @since 6.1.7 
 * 
 * @see I_CmsStaticExportHandler
 *
 */
public abstract class A_CmsStaticExportHandler implements I_CmsStaticExportHandler {

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(A_CmsStaticExportHandler.class);

    /** Indicates if this content handler is busy. */
    protected boolean m_busy;

    /**
     * @see org.opencms.staticexport.I_CmsStaticExportHandler#isBusy()
     */
    public boolean isBusy() {

        return m_busy;
    }

    /**
     * @see org.opencms.staticexport.I_CmsStaticExportHandler#performEventPublishProject(org.opencms.util.CmsUUID, org.opencms.report.I_CmsReport)
     */
    public abstract void performEventPublishProject(CmsUUID publishHistoryId, I_CmsReport report);

    /**
     * Scrubs all files from the export folder that might have been changed,
     * so that the export is newly created after the next request to the resource.<p>
     * 
     * @param publishHistoryId id of the last published project
     */
    public void scrubExportFolders(CmsUUID publishHistoryId) {

        if (LOG.isDebugEnabled()) {
            LOG.debug(Messages.get().getBundle().key(Messages.LOG_SCRUBBING_EXPORT_FOLDERS_1, publishHistoryId));
        }

        Set scrubedFolders = new HashSet();
        Set scrubedFiles = new HashSet();

        // get a export user cms context        
        CmsObject cms;
        try {
            cms = OpenCms.initCmsObject(OpenCms.getDefaultUsers().getUserExport());
        } catch (CmsException e) {
            // this should never happen
            LOG.error(Messages.get().getBundle().key(Messages.LOG_INIT_FAILED_0), e);
            return;
        }

        List publishedResources;
        try {
            publishedResources = cms.readPublishedResources(publishHistoryId);
        } catch (CmsException e) {

            LOG.error(
                Messages.get().getBundle().key(Messages.LOG_READING_CHANGED_RESOURCES_FAILED_1, publishHistoryId),
                e);
            return;
        }

        Iterator itPubRes = publishedResources.iterator();
        while (itPubRes.hasNext()) {
            CmsPublishedResource res = (CmsPublishedResource)itPubRes.next();
            if (res.isUnChanged() || !res.isVfsResource()) {
                // unchanged resources and non vfs resources don't need to be deleted
                continue;
            }

            // ensure all siblings are scrubbed if the resource has one
            String resPath = cms.getRequestContext().removeSiteRoot(res.getRootPath());
            List siblings = getSiblingsList(cms, resPath);

            Iterator itSibs = siblings.iterator();
            while (itSibs.hasNext()) {
                String vfsName = (String)itSibs.next();

                // get the link name for the published file 
                String rfsName = OpenCms.getStaticExportManager().getRfsName(cms, vfsName);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(Messages.get().getBundle().key(Messages.LOG_CHECKING_STATIC_EXPORT_2, vfsName, rfsName));
                }
                if (rfsName.startsWith(OpenCms.getStaticExportManager().getRfsPrefix(vfsName))
                    && (!scrubedFiles.contains(rfsName))
                    && (!scrubedFolders.contains(CmsResource.getFolderPath(rfsName)))) {

                    if (res.isFolder()) {
                        if (res.isDeleted()) {
                            String exportFolderName = CmsFileUtil.normalizePath(OpenCms.getStaticExportManager().getExportPath(
                                vfsName)
                                + rfsName.substring(OpenCms.getStaticExportManager().getRfsPrefix(vfsName).length()));
                            try {
                                File exportFolder = new File(exportFolderName);
                                // check if export folder exists, if so delete it
                                if (exportFolder.exists() && exportFolder.canWrite()) {
                                    CmsFileUtil.purgeDirectory(exportFolder);
                                    // write log message
                                    if (LOG.isInfoEnabled()) {
                                        LOG.info(Messages.get().getBundle().key(
                                            Messages.LOG_FOLDER_DELETED_1,
                                            exportFolderName));
                                    }
                                    scrubedFolders.add(rfsName);
                                    continue;
                                }
                            } catch (Throwable t) {
                                // ignore, nothing to do about this
                                if (LOG.isWarnEnabled()) {
                                    LOG.warn(Messages.get().getBundle().key(
                                        Messages.LOG_FOLDER_DELETION_FAILED_2,
                                        vfsName,
                                        exportFolderName));
                                }
                            }
                        }
                        // add index.html to folder name
                        rfsName += CmsStaticExportManager.EXPORT_DEFAULT_FILE;
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(Messages.get().getBundle().key(Messages.LOG_FOLDER_1, rfsName));
                        }

                    }

                    String rfsExportFileName = CmsFileUtil.normalizePath(OpenCms.getStaticExportManager().getExportPath(
                        vfsName)
                        + rfsName.substring(OpenCms.getStaticExportManager().getRfsPrefix(vfsName).length()));

                    purgeFile(rfsExportFileName, vfsName);
                    scrubedFiles.add(rfsName);

                    if (!res.isFolder()) {
                        List fileList = getRelatedFilesToPurge(rfsExportFileName, vfsName);
                        Iterator iter = fileList.iterator();
                        while (iter.hasNext()) {
                            File file = (File)iter.next();
                            purgeFile(file.getAbsolutePath(), vfsName);
                            rfsName = CmsFileUtil.normalizePath(OpenCms.getStaticExportManager().getRfsPrefix(vfsName)
                                + "/"
                                + file.getAbsolutePath().substring(
                                    OpenCms.getStaticExportManager().getExportPath(vfsName).length()));
                            rfsName = CmsStringUtil.substitute(
                                rfsName,
                                new String(new char[] {File.separatorChar}),
                                "/");
                            scrubedFiles.add(rfsName);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns a list of related files to purge.<p>
     * 
     * @param exportFileName the previous exported rfs filename (already purged)
     * @param vfsName the vfs name of the resource (to be used to compute more sofisticated sets of related files to purge 
     * 
     * @return a list of related files to purge
     */
    protected abstract List getRelatedFilesToPurge(String exportFileName, String vfsName);

    /**
     * Returns a list containing the root paths of all siblings of a resource.<p> 
     * 
     * @param cms the export user context
     * @param resPath the path of the resource to get the siblings for
     * @return a list containing the root paths of all siblings of a resource
     */
    protected List getSiblingsList(CmsObject cms, String resPath) {

        List siblings = new ArrayList();
        try {
            List li = cms.readSiblings(resPath, CmsResourceFilter.ALL);
            for (int i = 0, l = li.size(); i < l; i++) {
                String vfsName = ((CmsResource)li.get(i)).getRootPath();
                siblings.add(vfsName);
            }
        } catch (CmsVfsResourceNotFoundException e) {
            // resource not found, probably because the export user has no read permission on the resource, ignore
        } catch (CmsSecurityException e) {
            // security exception, probably because the export user has no read permission on the resource, ignore
        } catch (CmsException e) {
            // ignore, nothing to do about this
            if (LOG.isWarnEnabled()) {
                LOG.warn(Messages.get().getBundle().key(Messages.LOG_FETCHING_SIBLINGS_FAILED_1, resPath), e);
            }
        }
        if (!siblings.contains(resPath)) {
            // always add the resource itself, this has to be done because if the resource was
            // deleted during publishing, the sibling lookup above will produce no results
            siblings.add(resPath);
        }
        return siblings;
    }

    /**
     * Deletes the given file from the RFS if it exists.<p>
     * 
     * @param vfsName the vfs name of the file to delete
     * @param exportFileName the file to delete
     */
    protected void purgeFile(String exportFileName, String vfsName) {

        String rfsName = CmsFileUtil.normalizePath(OpenCms.getStaticExportManager().getRfsPrefix(vfsName)
            + exportFileName.substring(OpenCms.getStaticExportManager().getExportPath(vfsName).length()));
        rfsName = CmsStringUtil.substitute(rfsName, new String(new char[] {File.separatorChar}), "/");

        try {
            File exportFile = new File(exportFileName);
            // check if export file exists, if so delete it
            if (exportFile.exists() && exportFile.canWrite()) {
                exportFile.delete();
                // write log message
                if (LOG.isInfoEnabled()) {
                    LOG.info(Messages.get().getBundle().key(Messages.LOG_FILE_DELETED_1, rfsName));
                }
            }
        } catch (Throwable t) {
            // ignore, nothing to do about this
            if (LOG.isWarnEnabled()) {
                LOG.warn(Messages.get().getBundle().key(Messages.LOG_FILE_DELETION_FAILED_1, rfsName), t);
            }
        }
    }
}