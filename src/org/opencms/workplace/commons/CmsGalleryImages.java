/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/commons/Attic/CmsGalleryImages.java,v $
 * Date   : $Date: 2004/12/09 13:53:44 $
 * Version: $Revision: 1.8 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2004 Alkacon Software (http://www.alkacon.com)
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
import org.opencms.file.types.CmsResourceTypeImage;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsException;
import org.opencms.util.CmsStringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

/**
 * Generates the image gallery popup window which can be used in editors or as a dialog widget.<p>
 * 
 * @author Andreas Zahner (a.zahner@alkacon.com)
 * @author Armen Markarian (a.markarian@alkacon.com)
 * @version $Revision: 1.8 $
 * 
 * @since 5.5.2
 */
public class CmsGalleryImages extends CmsGallery {
    
    /** URI of the image gallery popup dialog. */
    public static final String C_URI_GALLERY = C_PATH_GALLERIES + "img_fs.jsp";

    /**
     * Public empty constructor, required for {@link CmsGallery#createInstance(String, CmsJspActionElement)}.<p>
     */
    public CmsGalleryImages() {

        // noop
    }

    /**
     * Public constructor with JSP action element.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsGalleryImages(CmsJspActionElement jsp) {

        super(jsp);
    }

    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */
    public CmsGalleryImages(PageContext context, HttpServletRequest req, HttpServletResponse res) {

        this(new CmsJspActionElement(context, req, res));
    }
    
    /**
     * @see org.opencms.workplace.commons.CmsGallery#applyButton()
     */
    public String applyButton() {
        if (MODE_VIEW.equals(getParamDialogMode())) {
            return button(null, null, "apply_in", "button.paste", 0); 
        } else {
            String uri = getParamResourcePath();
            if (CmsStringUtil.isEmpty(getParamDialogMode())) {
                uri = getJsp().link(uri);
            }
            return button("javascript:pasteResource('"+uri+"',document.form.title.value, document.form.title.value);", null, "apply", "button.paste", 0);
        }
    }
    
    /**
     * Builds the html String for the preview frame.<p>
     * 
     * @return the html String for the preview frame
     */
    public String buildGalleryItemPreview() {
        
        StringBuffer html = new StringBuffer();                   
        try {
            if (CmsStringUtil.isNotEmpty(getParamResourcePath())) {
                CmsResource res = getCms().readResource(getParamResourcePath());
                if (res != null) {
                    html.append("<img alt=\"\" src=\"");
                    html.append(getJsp().link(getParamResourcePath()));
                    html.append("\" border=\"0\">");                   
                }
            }
        } catch (CmsException e) {
            // ignore this exception
        }                
        
        return html.toString();
    }  
        
    /**
     * @see org.opencms.workplace.commons.CmsGallery#getGalleryItemsTypeId()
     */
    public int getGalleryItemsTypeId() {
        
        return CmsResourceTypeImage.C_RESOURCE_TYPE_ID;
    }     
    
    /**
     * @see org.opencms.workplace.commons.CmsGallery#getPreviewBodyStyle()
     */
    public String getPreviewBodyStyle() {
        
        return "";
    }
    
    /**
     * @see org.opencms.workplace.commons.CmsGallery#previewButton()
     */
    public String previewButton() {
        return "";        
    }
    
    /**
     * @see org.opencms.workplace.commons.CmsGallery#targetSelectBox()
     */
    public String targetSelectBox() {
        return "";
    }
}
