/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/explorer/CmsNewResourceXmlPage.java,v $
 * Date   : $Date: 2006/08/24 06:43:25 $
 * Version: $Revision: 1.23.4.2 $
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

package org.opencms.workplace.explorer;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsFolder;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.types.CmsResourceTypeXmlPage;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsStringUtil;
import org.opencms.workplace.CmsWorkplace;
import org.opencms.workplace.CmsWorkplaceSettings;
import org.opencms.workplace.commons.CmsPropertyAdvanced;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;

/**
 * The new resource page dialog handles the creation of an xml page.<p>
 * 
 * The following files use this class:
 * <ul>
 * <li>/commons/newresource_xmlpage.jsp
 * </ul>
 * <p>
 * 
 * @author Andreas Zahner 
 * 
 * @version $Revision: 1.23.4.2 $ 
 * 
 * @since 6.0.0 
 */
public class CmsNewResourceXmlPage extends CmsNewResource {

    /** Request parameter name for the selected body. */
    public static final String PARAM_BODYFILE = "bodyfile";

    /** Request parameter name for the suffix check. */
    public static final String PARAM_SUFFIXCHECK = "suffixcheck";

    /** Request parameter name for the selected template. */
    public static final String PARAM_TEMPLATE = "template";

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsNewResourceXmlPage.class);
    private String m_paramBodyFile;
    private String m_paramDialogMode;
    private String m_paramSuffixCheck;
    private String m_paramTemplate;

    /**
     * Public constructor with JSP action element.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsNewResourceXmlPage(CmsJspActionElement jsp) {

        super(jsp);
    }

    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */
    public CmsNewResourceXmlPage(PageContext context, HttpServletRequest req, HttpServletResponse res) {

        this(new CmsJspActionElement(context, req, res));
    }

    /**
     * Returns a sorted Map of all available body files of the OpenCms modules.<p>
     * 
     * @param cms the current cms object
     * @param currWpPath the current path in the OpenCms workplace
     * @return a sorted map with the body file title as key and absolute path to the body file as value
     * @throws CmsException if reading a folder or file fails
     */
    public static TreeMap getBodies(CmsObject cms, String currWpPath) throws CmsException {

        return getElements(cms, CmsWorkplace.VFS_DIR_DEFAULTBODIES, currWpPath, true);
    }

    /**
     * Returns a sorted Map of all available body files of the OpenCms modules.<p>
     * 
     * @param cms the current cms object
     * @param currWpPath the current path in the OpenCms workplace
     * @param emptyMap flag indicating if it is OK to return a filtered empty Map
     * @return a sorted map with the body file title as key and absolute path to the body file as value
     * @throws CmsException if reading a folder or file fails
     */
    public static TreeMap getBodies(CmsObject cms, String currWpPath, boolean emptyMap) throws CmsException {

        return getElements(cms, CmsWorkplace.VFS_DIR_DEFAULTBODIES, currWpPath, emptyMap);
    }

    /**
     * Returns a sorted Map of all available templates of the OpenCms modules.<p>
     * 
     * @param cms the current cms object
     * @param currWpPath the current path in the OpenCms workplace
     * @return a sorted map with the template title as key and absolute path to the template as value
     * @throws CmsException if reading a folder or file fails
     */
    public static TreeMap getTemplates(CmsObject cms, String currWpPath) throws CmsException {

        return getElements(cms, CmsWorkplace.VFS_DIR_TEMPLATES, currWpPath, true);
    }

    /**
     * Returns a sorted Map of all available templates of the OpenCms modules.<p>
     * 
     * @param cms the current cms object
     * @param currWpPath the current path in the OpenCms workplace
     * @param emptyMap flag indicating if it is OK to return a filtered empty Map
     * @return a sorted map with the template title as key and absolute path to the template as value
     * @throws CmsException if reading a folder or file fails
     */
    public static TreeMap getTemplates(CmsObject cms, String currWpPath, boolean emptyMap) throws CmsException {

        return getElements(cms, CmsWorkplace.VFS_DIR_TEMPLATES, currWpPath, emptyMap);
    }

    /**
     * Returns a sorted Map of all available elements in the specified subfolder of the OpenCms modules.<p>
     * 
     * @param cms the current cms object
     * @param elementFolder the module subfolder to serach for elements
     * @param currWpPath the current path in the OpenCms workplace
     * @param emptyMap flag indicating if it is OK to return a filtered empty Map
     * @return a sorted map with the element title as key and absolute path to the element as value
     * @throws CmsException if reading a folder or file fails
     */
    protected static TreeMap getElements(CmsObject cms, String elementFolder, String currWpPath, boolean emptyMap)
    throws CmsException {

        TreeMap elements = new TreeMap();
        TreeMap allElements = new TreeMap();

        if (CmsStringUtil.isNotEmpty(currWpPath)) {
            // add site root to current workplace path
            currWpPath = cms.getRequestContext().addSiteRoot(currWpPath);
        }

        // get all visible template elements in the module folders
        List modules = cms.getSubFolders(CmsWorkplace.VFS_PATH_MODULES, CmsResourceFilter.IGNORE_EXPIRATION);
        for (int i = 0; i < modules.size(); i++) {
            List moduleTemplateFiles = new ArrayList();
            String folder = cms.getSitePath((CmsFolder)modules.get(i));
            try {
                moduleTemplateFiles = cms.getFilesInFolder(
                    folder + elementFolder,
                    CmsResourceFilter.DEFAULT.addRequireVisible());
            } catch (CmsException e) {
                // folder not available, list will be empty
                if (LOG.isDebugEnabled()) {
                    LOG.debug(e.getMessage(), e);
                }
            }
            for (int j = 0; j < moduleTemplateFiles.size(); j++) {
                // get the current template file
                CmsFile templateFile = (CmsFile)moduleTemplateFiles.get(j);
                String title = null;
                String folderProp = null;
                try {
                    title = cms.readPropertyObject(
                        cms.getSitePath(templateFile),
                        CmsPropertyDefinition.PROPERTY_TITLE,
                        false).getValue();
                    folderProp = cms.readPropertyObject(
                        templateFile,
                        CmsPropertyDefinition.PROPERTY_FOLDERS_AVAILABLE,
                        false).getValue();
                } catch (CmsException e) {
                    // property not available, will be null
                    if (LOG.isInfoEnabled()) {
                        LOG.info(e);
                    }
                }

                boolean isInFolder = false;
                // check template folders property value
                if (CmsStringUtil.isNotEmpty(currWpPath) && CmsStringUtil.isNotEmpty(folderProp)) {
                    // property value set on template, check if current workplace path fits
                    List folders = CmsStringUtil.splitAsList(folderProp, DELIM_PROPERTYVALUES);
                    for (int k = 0; k < folders.size(); k++) {
                        String checkFolder = (String)folders.get(k);
                        if (currWpPath.startsWith(checkFolder)) {
                            isInFolder = true;
                            break;
                        }
                    }
                } else {
                    isInFolder = true;
                }

                if (title == null) {
                    // no title property found, display the file name
                    title = templateFile.getName();
                }
                String path = cms.getSitePath(templateFile);
                if (isInFolder) {
                    // element is valid, add it to result
                    elements.put(title, path);
                }
                // also put element to overall result
                allElements.put(title, path);
            }
        }
        if (!emptyMap && (elements.size() < 1)) {
            // empty Map should not be returned, return all collected elements
            return allElements;
        }
        // return the filtered elements sorted by title
        return elements;
    }

    /**
     * Used to close the current JSP dialog.<p>
     * 
     * This method overwrites the close dialog method in the super class,
     * because in case a new folder was created before, after this dialog the tree view has to be refreshed.<p>
     *  
     * It tries to include the URI stored in the workplace settings.
     * This URI is determined by the frame name, which has to be set 
     * in the framename parameter.<p>
     * 
     * @throws JspException if including an element fails
     */
    public void actionCloseDialog() throws JspException {

        if (isCreateIndexMode()) {
            // set the current explorer resource to the new created folder
            String updateFolder = CmsResource.getParentFolder(getSettings().getExplorerResource());
            getSettings().setExplorerResource(updateFolder);
            List folderList = new ArrayList(1);
            if (updateFolder != null) {
                folderList.add(updateFolder);
            }
            getJsp().getRequest().setAttribute(REQUEST_ATTRIBUTE_RELOADTREE, folderList);
        }
        super.actionCloseDialog();
    }

    /**
     * Creates the xml page using the specified resource name.<p>
     * 
     * @throws JspException if inclusion of error dialog fails
     */
    public void actionCreateResource() throws JspException {

        try {
            // calculate the new resource Title property value
            String title = computeNewTitleProperty();
            // create the full resource name
            String fullResourceName = computeFullResourceName();

            // eventually append ".html" suffix to new file if not present
            boolean forceSuffix = false;
            if (CmsStringUtil.isEmpty(getParamSuffixCheck())) {
                // backward compatibility: append suffix every time
                forceSuffix = true;
            }
            fullResourceName = appendSuffixHtml(fullResourceName, forceSuffix);

            // get the body file content
            byte[] bodyFileBytes = null;
            if (CmsStringUtil.isEmpty(getParamBodyFile())) {
                // body file not specified, use empty body
                bodyFileBytes = ("").getBytes();
            } else {
                // get the specified body file
                bodyFileBytes = getCms().readFile(getParamBodyFile(), CmsResourceFilter.IGNORE_EXPIRATION).getContents();
            }

            // create the xml page   
            List properties = new ArrayList(4);
            // add the template property to the new file
            properties.add(new CmsProperty(CmsPropertyDefinition.PROPERTY_TEMPLATE, getParamTemplate(), null));
            properties.addAll(createResourceProperties(
                fullResourceName,
                CmsResourceTypeXmlPage.getStaticTypeName(),
                title));
            getCms().createResource(
                fullResourceName,
                CmsResourceTypeXmlPage.getStaticTypeId(),
                bodyFileBytes,
                properties);

            // set the resource parameter to full path for property dialog 
            setParamResource(fullResourceName);
            setResourceCreated(true);
        } catch (Throwable e) {
            // error creating folder, show error dialog
            includeErrorpage(this, e);
        }
    }

    /**
     * Forwards to the property dialog if the resourceeditprops parameter is true.<p>
     * 
     * If the parameter is not true, the dialog will be closed.<p>
     * 
     * @throws IOException if forwarding to the property dialog fails
     * @throws ServletException if forwarding to the property dialog fails
     * @throws JspException if an inclusion fails
     */
    public void actionEditProperties() throws IOException, JspException, ServletException {

        boolean editProps = Boolean.valueOf(getParamNewResourceEditProps()).booleanValue();
        if (editProps) {
            // edit properties checkbox checked, forward to property dialog
            Map params = new HashMap();
            params.put(PARAM_RESOURCE, getParamResource());
            if (isCreateIndexMode()) {
                params.put(CmsPropertyAdvanced.PARAM_DIALOGMODE, CmsPropertyAdvanced.MODE_WIZARD_INDEXCREATED);
            } else {
                params.put(CmsPropertyAdvanced.PARAM_DIALOGMODE, CmsPropertyAdvanced.MODE_WIZARD);
            }
            sendForward(CmsPropertyAdvanced.URI_PROPERTY_DIALOG_HANDLER, params);
        } else {
            // edit properties not checked, close the dialog
            actionCloseDialog();
        }
    }

    /**
     * Builds the html for the page body file select box.<p>
     * 
     * @param attributes optional attributes for the &lt;select&gt; tag
     * @return the html for the page body file select box
     */
    public String buildSelectBodyFile(String attributes) {

        List options = new ArrayList();
        List values = new ArrayList();
        TreeMap bodies = null;
        try {
            // get all available body files
            bodies = getBodies(getCms(), getParamCurrentFolder(), false);
        } catch (CmsException e) {
            // can usually be ignored
            if (LOG.isInfoEnabled()) {
                LOG.info(e);
            }
        }
        if (bodies == null) {
            // no body files found, return empty String
            return "";
        } else {
            // body files found, create option and value lists
            Iterator i = bodies.entrySet().iterator();
            int counter = 0;
            while (i.hasNext()) {
                Map.Entry entry = (Map.Entry)i.next();
                String key = (String)entry.getKey();
                String path = (String)entry.getValue();

                options.add(key);
                values.add(path);
                counter++;
            }
        }
        return buildSelect(attributes, options, values, -1, false);
    }

    /**
     * Builds the html for the page template select box.<p>
     * 
     * @param attributes optional attributes for the &lt;select&gt; tag
     * @return the html for the page template select box
     */
    public String buildSelectTemplates(String attributes) {

        List options = new ArrayList();
        List values = new ArrayList();
        TreeMap templates = null;
        try {
            // get all available templates
            templates = getTemplates(getCms(), getParamCurrentFolder(), false);
        } catch (CmsException e) {
            // can usually be ignored
            if (LOG.isInfoEnabled()) {
                LOG.info(e);
            }
        }
        if (templates == null) {
            // no templates found, return empty String
            return "";
        } else {
            // templates found, create option and value lists
            Iterator i = templates.entrySet().iterator();
            int counter = 0;
            while (i.hasNext()) {
                Map.Entry entry = (Map.Entry)i.next();
                String key = (String)entry.getKey();
                String path = (String)entry.getValue();

                options.add(key);
                values.add(path);
                counter++;
            }
        }
        return buildSelect(attributes, options, values, -1, false);
    }

    /**
     * Returns the body file parameter value.<p>
     * 
     * @return the body file parameter value
     */
    public String getParamBodyFile() {

        return m_paramBodyFile;
    }

    /**
     * Returns the value of the dialogmode parameter, 
     * or null if this parameter was not provided.<p>
     * 
     * The dialogmode parameter stores the different modes of the property dialog,
     * e.g. for displaying other buttons in the new resource wizard.<p>
     * 
     * @return the value of the usetempfileproject parameter
     */
    public String getParamDialogmode() {

        return m_paramDialogMode;
    }

    /**
     * Returns the request parameter flag inidicating if the suffix field is present or not.<p>
     * 
     * @return the request parameter flag inidicating if the suffix field is present or not
     */
    public String getParamSuffixCheck() {

        return m_paramSuffixCheck;
    }

    /**
     * Returns the template parameter value.<p>
     * 
     * @return the template parameter value
     */
    public String getParamTemplate() {

        return m_paramTemplate;
    }

    /**
     * Returns true if the current mode is: create an index page in a newly created folder.<p>
     * 
     * @return true if we are in wizard mode to create an index page, otherwise false
     */
    public boolean isCreateIndexMode() {

        return CmsPropertyAdvanced.MODE_WIZARD_CREATEINDEX.equals(getParamDialogmode());
    }

    /**
     * Overrides the super implementation to avoid problems with double reqource input fields.<p>
     * 
     * @see org.opencms.workplace.CmsWorkplace#paramsAsHidden()
     */
    public String paramsAsHidden() {

        String resourceName = getParamResource();
        // remove resource parameter from hidden params to avoid problems with double input fields in form
        setParamResource(null);
        String params = super.paramsAsHidden();
        // set resource parameter to stored value
        setParamResource(resourceName);
        return params;
    }

    /**
     * Sets the body file parameter value.<p>
     * 
     * @param bodyFile the body file parameter value
     */
    public void setParamBodyFile(String bodyFile) {

        m_paramBodyFile = bodyFile;
    }

    /**
     * Sets the value of the dialogmode parameter.<p>
     * 
     * @param value the value to set
     */
    public void setParamDialogmode(String value) {

        m_paramDialogMode = value;
    }

    /**
     * Sets the request parameter flag inidicating if the suffix field is present or not.<p>
     * 
     * @param paramSuffixCheck he request parameter flag inidicating if the suffix field is present or not
     */
    public void setParamSuffixCheck(String paramSuffixCheck) {

        m_paramSuffixCheck = paramSuffixCheck;
    }

    /**
     * Sets the template parameter value.<p>
     * 
     * @param template the template parameter value
     */
    public void setParamTemplate(String template) {

        m_paramTemplate = template;
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
        if (DIALOG_OK.equals(getParamAction())) {
            setAction(ACTION_OK);
        } else if (DIALOG_CANCEL.equals(getParamAction())) {
            setAction(ACTION_CANCEL);
        } else {
            // set resource name if we are in new folder wizard mode
            setInitialResourceName();
            setAction(ACTION_DEFAULT);
            // build title for new resource dialog     
            setParamTitle(key(Messages.GUI_NEWRESOURCE_XMLPAGE_0));
        }
    }

    /**
     * Sets the initial resource name of the new page.<p>
     * 
     * This is used for the "new" wizard after creating a new folder followed
     * by the "create index file" procedure.<p> 
     */
    private void setInitialResourceName() {

        if (isCreateIndexMode()) {
            // creation of an index file in a new folder, use default file name
            String defaultFile = "";
            try {
                defaultFile = (String)OpenCms.getDefaultFiles().get(0);
            } catch (IndexOutOfBoundsException e) {
                // list is empty, ignore    
            }
            if (CmsStringUtil.isEmpty(defaultFile)) {
                // make sure that the default file name is not empty
                defaultFile = "index.html";
            }
            setParamResource(defaultFile);
        } else {
            setParamResource("");
        }
    }

}