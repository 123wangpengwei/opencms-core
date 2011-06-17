/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/editors/ade/Attic/CmsDefaultFormatterHelper.java,v $
 * Date   : $Date: 2010/09/22 14:27:48 $
 * Version: $Revision: 1.9 $
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

package org.opencms.workplace.editors.ade;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.file.types.CmsResourceTypeImage;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.loader.CmsImageScaler;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.workplace.CmsWorkplace;
import org.opencms.workplace.CmsWorkplaceMessages;
import org.opencms.xml.containerpage.CmsADEManager;
import org.opencms.xml.containerpage.CmsContainerElementBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

/**
 * Helper bean to implement default formatters for list elements.<p>
 * 
 * @author Tobias Herrmann
 * 
 * @version $Revision: 1.9 $
 * 
 * @since 7.6
 */
public class CmsDefaultFormatterHelper extends CmsJspActionElement {

    /** The element-bean. */
    private CmsContainerElementBean m_elementBean;

    /** The formatter-info-bean. */
    private CmsFormatterInfoBean m_formatterInfo;

    /** Indicates if the formatter is using the container-bean or formatter-info-bean as data-source. */
    private boolean m_isContainerBeanMode;

    /** The ade manager. */
    private CmsADEManager m_manager;

    /** The element's resource. */
    private CmsResource m_resource;

    /** The workplace locale from the current user's settings. */
    private Locale m_wpLocale;

    /**
     * Constructor, with parameters.
     * 
     * @param context the JSP page context object
     * @param req the JSP request 
     * @param res the JSP response 
     * @throws CmsException 
     */
    public CmsDefaultFormatterHelper(PageContext context, HttpServletRequest req, HttpServletResponse res)
    throws CmsException {

        super(context, req, res);

        m_manager = OpenCms.getADEManager();
        m_formatterInfo = getFormatterInfo(req);

        if (m_formatterInfo == null) {
            m_elementBean = m_manager.getCurrentElement(req);
            m_isContainerBeanMode = true;
        }
    }

    /**
     * Returns a list of additional info.<p>
     * 
     * @return the additional info.
     * @throws CmsException if something goes wrong
     */
    public List<CmsFieldInfoBean> getAdditionalInfo() throws CmsException {

        if (m_isContainerBeanMode) {
            List<CmsFieldInfoBean> result = new ArrayList<CmsFieldInfoBean>();
            result.add(new CmsFieldInfoBean("path", Messages.get().getBundle(getWorkplaceLocale()).key(
                Messages.GUI_LABEL_PATH_0), getSitePath()));
            result.add(new CmsFieldInfoBean("type", Messages.get().getBundle(getWorkplaceLocale()).key(
                Messages.GUI_LABEL_TYPE_0), CmsWorkplaceMessages.getResourceTypeName(
                getWorkplaceLocale(),
                OpenCms.getResourceManager().getResourceType(getTypeName()).getTypeName())));
            result.add(new CmsFieldInfoBean(
                "lastModified",
                Messages.get().getBundle(getWorkplaceLocale()).key(Messages.GUI_LABEL_LAST_MODIFIED_0),
                OpenCms.getWorkplaceManager().getMessages(getWorkplaceLocale()).getDateTime(
                    getResource().getDateLastModified())));
            return result;
        }
        return m_formatterInfo.getAdditionalInfo();
    }

    /**
     * Gets the current formatter-info-bean from the request.<p>
     * 
     * @param req the servlet-request
     * @return the info-bean or null if not available
     * @throws CmsException if something goes wrong
     */
    public CmsFormatterInfoBean getFormatterInfo(ServletRequest req) throws CmsException {

        CmsFormatterInfoBean info = null;
        try {
            info = (CmsFormatterInfoBean)req.getAttribute(CmsADEManager.ATTR_FORMATTER_INFO);
        } catch (Exception e) {
            throw new CmsException(org.opencms.xml.containerpage.Messages.get().container(
                org.opencms.xml.containerpage.Messages.ERR_READING_FORMATTER_INFO_FROM_REQUEST_0), e);
        }
        return info;
    }

    /** 
     * Returns the icon-path.<p>
     * 
     * @return the icon-path
     * @throws CmsException if something goes wrong
     */
    public String getIcon() throws CmsException {

        if (m_isContainerBeanMode) {
            return CmsWorkplace.getResourceUri(CmsWorkplace.RES_PATH_FILETYPES
                + OpenCms.getWorkplaceManager().getExplorerTypeSetting(getTypeName()).getIcon());
        }
        return m_formatterInfo.getIcon();
    }

    /**
     * Generates an image info string containing dimensions and file size, returns an empty string if the resource type is not "image".<p>
     * 
     * @return the info string
     * @throws CmsException if something goes wrong reading the resource
     */
    public String getImageInfo() throws CmsException {

        String result = "";
        if (getTypeName().equals(CmsResourceTypeImage.getStaticTypeName())) {
            CmsImageScaler scaler = new CmsImageScaler(getCmsObject(), getResource());
            int width = -1;
            int height = -1;
            // 1: image width
            if (scaler.isValid()) {
                width = scaler.getWidth();
            }
            // 2: image height
            if (scaler.isValid()) {
                height = scaler.getHeight();
            }
            result = getTitle() + "  " + width + "x" + height + "  " + (getResource().getLength() / 1024) + "kb";
        }
        return result;
    }

    /**
     * Returns the container page manager.<p>
     * 
     * @return the container page manager
     */
    public CmsADEManager getManager() {

        return m_manager;
    }

    /**
     * Returns the element's resource.<p>
     * 
     * @return the element's resource
     * 
     * @throws CmsException if something goes wrong
     */
    public CmsResource getResource() throws CmsException {

        if (m_resource == null) {
            if (m_isContainerBeanMode) {
                m_resource = getCmsObject().readResource(m_elementBean.getElementId());
            } else {
                if (m_formatterInfo.getResource() != null) {
                    m_resource = m_formatterInfo.getResource();
                } else if (m_formatterInfo.getSitePath() != null) {
                    m_resource = getCmsObject().readResource(m_formatterInfo.getSitePath());
                } else if (m_formatterInfo.getResourceId() != null) {
                    m_resource = getCmsObject().readResource(m_formatterInfo.getResourceId());
                }
            }
        }
        return m_resource;
    }

    /**
     * The elements site-path.<p>
     * 
     * @return the site-path
     * @throws CmsException if something goes wrong
     */
    public String getSitePath() throws CmsException {

        if (m_isContainerBeanMode) {
            return getCmsObject().getSitePath(getResource());
        }
        return m_formatterInfo.getSitePath();
    }

    /**
     * Returns the sub-title info.<p>
     * 
     * @return the sub-title info
     * @throws CmsException if something goes wrong
     */
    public CmsFieldInfoBean getSubTitle() throws CmsException {

        if (m_isContainerBeanMode) {
            return new CmsFieldInfoBean("filename", "", getResource().getName());
        }
        return m_formatterInfo.getSubTitleInfo();
    }

    /**
     * Returns the title info.<p>
     * 
     * @return the title info
     * @throws CmsException if something goes wrong
     */
    public CmsFieldInfoBean getTitle() throws CmsException {

        if (m_isContainerBeanMode) {
            return new CmsFieldInfoBean(CmsPropertyDefinition.PROPERTY_TITLE, "", getCmsObject().readPropertyObject(
                getSitePath(),
                CmsPropertyDefinition.PROPERTY_TITLE,
                false).getValue(""));
        }
        return m_formatterInfo.getTitleInfo();
    }

    /**
     * Returns the resource type name of the element.<p>
     * 
     * @return the resource type name
     * @throws CmsException if something goes wrong
     */
    public String getTypeName() throws CmsException {

        if (m_isContainerBeanMode) {
            return OpenCms.getResourceManager().getResourceType(getResource().getTypeId()).getTypeName();
        }
        return m_formatterInfo.getResourceType().getTypeName();

    }

    /**
     * Checks if this element is a new or existing element, depending on the configuration file.<p>
     * 
     * @return <code>true</code> if this element is a new element, depending on the configuration file
     * 
     * @throws CmsException if something goes wrong
     */
    public boolean isNew() throws CmsException {

        CmsObject cms = getCmsObject();
        Collection<CmsResource> elems = getManager().getCreatableElements(
            cms,
            cms.getRequestContext().getUri(),
            getRequest());
        return elems.contains(getResource());
    }

    /**
     * Returns the workplace locale from the current user's settings.<p>
     * 
     * @return the workplace locale
     */
    protected Locale getWorkplaceLocale() {

        if (m_wpLocale == null) {
            m_wpLocale = OpenCms.getWorkplaceManager().getWorkplaceLocale(getCmsObject());
        }
        return m_wpLocale;
    }
}
