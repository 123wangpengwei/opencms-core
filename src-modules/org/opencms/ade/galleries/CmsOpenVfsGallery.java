/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/ade/galleries/Attic/CmsOpenVfsGallery.java,v $
 * Date   : $Date: 2010/03/19 10:11:54 $
 * Version: $Revision: 1.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2009 Alkacon Software (http://www.alkacon.com)
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

package org.opencms.ade.galleries;

import org.opencms.ade.galleries.shared.I_CmsGalleryProviderConstants;
import org.opencms.file.CmsResource;
import org.opencms.i18n.CmsMessageContainer;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.CmsRuntimeException;
import org.opencms.main.OpenCms;
import org.opencms.workplace.CmsDialog;
import org.opencms.workplace.CmsWorkplaceSettings;
import org.opencms.workplace.galleries.Messages;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;

/**
 * Provides methods for open gwt-based gallery dialog.<p> 
 * 
 * @author Polina Smagina
 * 
 * @version $Revision: 1.1 $ 
 * 
 * @since 8.0
 */
public class CmsOpenVfsGallery extends CmsDialog {

    /** The dialog type. */
    public static final String DIALOG_TYPE = "opengallery";

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsOpenVfsGallery.class);

    /** Path to the host page. */
    public static final String VFS_OPEN_GALLERY_PATH = "/system/modules/org.opencms.ade.galleries/testVfs.jsp";

    /**
     * Public constructor with JSP action element.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsOpenVfsGallery(CmsJspActionElement jsp) {

        super(jsp);

    }

    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */
    public CmsOpenVfsGallery(PageContext context, HttpServletRequest req, HttpServletResponse res) {

        this(new CmsJspActionElement(context, req, res));

    }

    /**
     * Generates a javascript window open for the requested gallery type.<p>
     * 
     * @return a javascript window open for the requested gallery type
     */
    public String openGallery() {

        StringBuffer jsOpener = new StringBuffer(32);
        String galleryType = null;
        try {
            CmsResource res = getCms().readResource(getParamResource());
            if (res != null) {
                // get gallery path
                String galleryPath = getParamResource();
                if (!galleryPath.endsWith("/")) {
                    galleryPath += "/";
                }
                // get the matching gallery type name
                galleryType = OpenCms.getResourceManager().getResourceType(res.getTypeId()).getTypeName();
                String width = "660";
                String height = "510";
                StringBuffer galleryUri = new StringBuffer(256);
                // path to the gallery dialog with the required request parameters
                galleryUri.append(VFS_OPEN_GALLERY_PATH);
                galleryUri.append("?");
                galleryUri.append(I_CmsGalleryProviderConstants.ReqParam.dialogmode.toString());
                galleryUri.append("=");
                galleryUri.append(I_CmsGalleryProviderConstants.GalleryMode.view.name());
                galleryUri.append("&");
                galleryUri.append(I_CmsGalleryProviderConstants.ReqParam.gallerypath.name());
                galleryUri.append("=");
                galleryUri.append(galleryPath);
                galleryUri.append("&");
                galleryUri.append(I_CmsGalleryProviderConstants.ReqParam.types.name());
                galleryUri.append("=");
                galleryUri.append("");
                galleryUri.append("&");
                galleryUri.append(I_CmsGalleryProviderConstants.ReqParam.tabs.toString());
                galleryUri.append("=");
                // tabs configuration
                StringBuffer tabConfig = new StringBuffer(256);
                tabConfig.append(I_CmsGalleryProviderConstants.GalleryTabId.cms_tab_types.name());
                tabConfig.append(",");
                tabConfig.append(I_CmsGalleryProviderConstants.GalleryTabId.cms_tab_galleries.name());
                tabConfig.append(",");
                tabConfig.append(I_CmsGalleryProviderConstants.GalleryTabId.cms_tab_categories.name());
                tabConfig.append(",");
                tabConfig.append(I_CmsGalleryProviderConstants.GalleryTabId.cms_tab_search.name());
                galleryUri.append(tabConfig);

                // open new gallery dialog
                jsOpener.append("window.open('");
                jsOpener.append(getJsp().link(galleryUri.toString()));
                //TODO: do we need gallery type by gallery opening
                jsOpener.append("', '");
                jsOpener.append(galleryType);
                jsOpener.append("','width=");
                //jsOpener.append("'width=");
                jsOpener.append(width);
                jsOpener.append(", height=");
                jsOpener.append(height);
                jsOpener.append(", resizable=yes, top=100, left=270, status=yes');");
            }
        } catch (CmsException e) {
            // requested type is not configured
            CmsMessageContainer message = Messages.get().container(Messages.ERR_OPEN_GALLERY_1, galleryType);
            LOG.error(message.key(), e);
            throw new CmsRuntimeException(message, e);
        }

        return jsOpener.toString();
    }

    /**
     * @see org.opencms.workplace.CmsWorkplace#initWorkplaceRequestValues(org.opencms.workplace.CmsWorkplaceSettings, javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected void initWorkplaceRequestValues(CmsWorkplaceSettings settings, HttpServletRequest request) {

        // fill the parameter values in the get/set methods
        fillParamValues(request);
        // set the dialog type
        setParamDialogtype(DIALOG_TYPE);
    }

}
