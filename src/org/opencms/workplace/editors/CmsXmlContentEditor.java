/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/editors/CmsXmlContentEditor.java,v $
 * Date   : $Date: 2004/08/19 11:26:34 $
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
package org.opencms.workplace.editors;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsResourceFilter;
import org.opencms.i18n.CmsEncoder;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsStringUtil;
import org.opencms.workplace.CmsWorkplaceAction;
import org.opencms.workplace.CmsWorkplaceSettings;
import org.opencms.workplace.xmlwidgets.I_CmsXmlWidget;
import org.opencms.xml.CmsXmlContentDefinition;
import org.opencms.xml.CmsXmlContentTypeManager;
import org.opencms.xml.CmsXmlEntityResolver;
import org.opencms.xml.CmsXmlException;
import org.opencms.xml.content.CmsXmlContent;
import org.opencms.xml.content.CmsXmlContentFactory;
import org.opencms.xml.content.CmsXmlDefaultContentFilter;
import org.opencms.xml.types.I_CmsXmlContentValue;
import org.opencms.xml.types.I_CmsXmlSchemaType;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

/**
 * Creates the editor for XML content definitions.<p> 
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.1 $
 * @since 5.5.0
 */
public class CmsXmlContentEditor extends CmsEditor {
    
    /** Indicates a new file should be created. */
    public static final String EDITOR_ACTION_NEW = I_CmsEditorActionHandler.C_DIRECT_EDIT_OPTION_NEW;
    
    /** Action for new file creation. */
    public static final int ACTION_NEW = 151;
    
    /** Parameter to indicate if a new XML content resource should be created. */
    private String m_paramNewLink;
    
    /** Constant for the editor type, must be the same as the editors subfolder name in the VFS. */
    private static final String EDITOR_TYPE = "xmlcontent";

    /**
     * Public constructor.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsXmlContentEditor(CmsJspActionElement jsp) {
        super(jsp);
    }
    
    /**
     * @see org.opencms.workplace.CmsWorkplace#initWorkplaceRequestValues(org.opencms.workplace.CmsWorkplaceSettings, javax.servlet.http.HttpServletRequest)
     */
    protected void initWorkplaceRequestValues(CmsWorkplaceSettings settings, HttpServletRequest request) {
        // fill the parameter values in the get/set methods
        fillParamValues(request);        
        // set the dialog type
        setParamDialogtype(EDITOR_TYPE);
        
        if (getParamNewLink() != null) {
            setParamAction(EDITOR_ACTION_NEW);
        }

        // set the action for the JSP switch 
        if (EDITOR_SAVE.equals(getParamAction())) {
            setAction(ACTION_SAVE);
        } else if (EDITOR_SAVEEXIT.equals(getParamAction())) {
            setAction(ACTION_SAVEEXIT);         
        } else if (EDITOR_EXIT.equals(getParamAction())) { 
            setAction(ACTION_EXIT);
        } else if (EDITOR_SHOW.equals(getParamAction())) {
            setAction(ACTION_SHOW);
        } else if (EDITOR_SHOW_ERRORMESSAGE.equals(getParamAction())) {
            setAction(ACTION_SHOW_ERRORMESSAGE);
        } else if (EDITOR_ACTION_NEW.equals(getParamAction())) {
            setAction(ACTION_NEW);
            return;
        } else {
            // initial call of editor
            setAction(ACTION_DEFAULT);
            initContent();
        }
        
        setParamContent(encodeContent(getParamContent()));        
    }   
    
    
    /**
     * Returns the "new link" parameter.<p>
     *
     * @return the "new link" parameter
     */
    public String getParamNewLink() {

        return m_paramNewLink;
    }

    /**
     * Sets the "new link" parameter.<p>
     *
     * @param paramNewLink the "new link" parameter to set
     */
    public void setParamNewLink(String paramNewLink) {

        m_paramNewLink = CmsEncoder.decode(paramNewLink);
    } 
    
    /**
     * @see org.opencms.workplace.editors.CmsEditor#getEditorResourceUri()
     */
    public String getEditorResourceUri() {
        return getSkinUri() + "editors/" + EDITOR_TYPE + "/";   
    }
    
    /**
     * Initializes the editor content when opening the editor for the first time.<p>
     */
    protected void initContent() {
        // save initialized instance of this class in request attribute for included sub-elements
        getJsp().getRequest().setAttribute(C_SESSION_WORKPLACE_CLASS, this);
        // get the default encoding
        String content = getParamContent();
        if (CmsStringUtil.isNotEmpty(content)) {
            // content already read, must be decoded 
            setParamContent(decodeContent(content));
            return;
        } else {
            content = "";
        }
        
        try {
            // lock resource if autolock is enabled
            checkLock(getParamResource());
            CmsFile editFile = getCms().readFile(getParamResource(), CmsResourceFilter.ALL);
            try {
                content = new String(editFile.getContents(), getFileEncoding());
            } catch (UnsupportedEncodingException e) {
                throw new CmsException("Invalid content encoding encountered while editing file '" + getParamResource() + "'");
            }
        } catch (CmsException e) {
            // reading of file contents failed, show error dialog
            try {
                showErrorPage(this, e, "read");
            } catch (JspException exc) {
                // should usually never happen
                if (OpenCms.getLog(this).isInfoEnabled()) {
                    OpenCms.getLog(this).info(e);
                }
            }
        }
        setParamContent(content);
    }
    
    /**
     * Performs the exit editor action.<p>
     * 
     * @see org.opencms.workplace.editors.CmsEditor#actionExit()
     */
    public void actionExit() throws IOException, JspException {    
        if (getAction() == ACTION_CANCEL) {
            // save and exit was canceled
            return;
        }
        // close the editor
        actionClose();
    }

    /**
     * Creates a new XML content item for editing.<p>
     * 
     * @throws JspException in case something goes wrong
     */
    public void actionNew() throws JspException {
        // get the filter used to create the new content
        int pos = m_paramNewLink.indexOf('|');
        String filterName = m_paramNewLink.substring(0, pos);
        String param = m_paramNewLink.substring(pos+1);
        String newFileName;
        
        int todo = 0;
        // TODO: Need to improve this
        CmsXmlDefaultContentFilter filter = new CmsXmlDefaultContentFilter();       
        try {
            
            newFileName = filter.getCreateLink(getCms(), filterName, param);
            
            CmsFile templateFile = getCms().readFile(getParamResource(), CmsResourceFilter.IGNORE_EXPIRATION);
            CmsXmlContent template = CmsXmlContentFactory.unmarshal(getCms(), templateFile);            
            
            Locale defaultLocale = (Locale)OpenCms.getLocaleManager().getDefaultLocales(getCms(), getParamResource()).get(0);
            CmsXmlContent newContent = new CmsXmlContent(template.getContentDefinition(new CmsXmlEntityResolver(getCms())), defaultLocale, template.getEncoding());  
            
            getCms().createResource(newFileName, 11);
            CmsFile newFile = getCms().readFile(newFileName, CmsResourceFilter.IGNORE_EXPIRATION);
            newFile.setContents(newContent.marshal());
            getCms().writeFile(newFile);
            
            setParamNewLink(null);
            setParamContent(null);
            setParamAction(null);
            setParamResource(newFileName);
            setAction(ACTION_DEFAULT);
            initContent();
            setParamContent(encodeContent(getParamContent()));
            
        } catch (CmsException e) {
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Error accessing XML content filter '" + m_paramNewLink + "'" , e);
            }                
            throw new JspException(e);
        }
    }
    
    /**
     * Performs the save content ation.<p>
     * 
     * @see org.opencms.workplace.editors.CmsEditor#actionSave()
     */
    public void actionSave() throws JspException {
        CmsFile editFile = null;
        try {
            editFile = getCms().readFile(getParamResource(), CmsResourceFilter.ALL);            
            // ensure all chars in the content are valid for the selected encoding
            String decodedContent = CmsEncoder.adjustHtmlEncoding(decodeContent(getParamContent()), getFileEncoding());
            
            CmsXmlContent content = CmsXmlContentFactory.unmarshal(decodedContent, getFileEncoding(), new CmsXmlEntityResolver(getCms()));
            CmsXmlContentDefinition contentDefinition = content.getContentDefinition(new CmsXmlEntityResolver(getCms()));
            
            int todo = 0;
            // TODO: figure out the locale selected by the user
            Locale locale = (Locale)content.getLocales().get(0);             
            
            List typeSequence = contentDefinition.getTypeSequence();            
            Iterator i = typeSequence.iterator();
            while (i.hasNext()) {
                                                
                I_CmsXmlSchemaType schemaType = (I_CmsXmlSchemaType)i.next();                
                String name = schemaType.getNodeName();
                int count = content.getIndexCount(name, locale);
                for (int j=0; j<count; j++) {

                    I_CmsXmlContentValue value = content.getValue(name, locale, j);
                    I_CmsXmlWidget widget = CmsXmlContentTypeManager.getTypeManager().getEditorWidget(value.getTypeName());
                    widget.setEditorValue(getCms(), content, getJsp().getRequest().getParameterMap(), this, value);
                }               
            }
            decodedContent = content.toString();
            
            
            try {
                editFile.setContents(decodedContent.getBytes(getFileEncoding()));
            } catch (UnsupportedEncodingException e) {
                throw new CmsException("Invalid content encoding encountered while editing file '" + getParamResource() + "'");
            }        
            // the file content might have been modified during the write operation
            CmsFile writtenFile = getCms().writeFile(editFile);
            try {
                decodedContent = new String(writtenFile.getContents(), getFileEncoding());
            } catch (UnsupportedEncodingException e) {
                throw new CmsException("Invalid content encoding encountered while editing file '" + getParamResource() + "'");
            }
            setParamContent(encodeContent(decodedContent));            
        } catch (CmsXmlException e) {
            showErrorPage(e, "xml");
        } catch (CmsException e) {
            showErrorPage(e, "save");
        }
    
        if (getAction() != ACTION_CANCEL) {
            // save successful, set save action         
           setAction(ACTION_SAVE);
        }
    }
    
    /**
     * Closes the editor and redirects to the workplace or the resource depending on the editor mode.<p>
     * 
     * @throws IOException if a redirection fails
     * @throws JspException if including a JSP fails
     */
    protected void actionClose() throws IOException, JspException {
        if ("true".equals(getParamDirectedit())) {
            // editor is in direct edit mode
            if (!"".equals(getParamBacklink())) {
                // set link to the specified back link target
                setParamCloseLink(getJsp().link(getParamBacklink()));
            } else {
                // set link to the edited resource
                setParamCloseLink(getJsp().link(getParamResource()));
            }
            // save initialized instance of this class in request attribute for included sub-elements
            getJsp().getRequest().setAttribute(C_SESSION_WORKPLACE_CLASS, this);
            // load the common JSP close dialog
            getJsp().include(C_FILE_DIALOG_CLOSE);
        } else {
            // redirect to the workplace explorer view 
            sendCmsRedirect(CmsWorkplaceAction.C_JSP_WORKPLACE_URI);
        }
    }    
    
    /**
     * Generates HTML form for the XML content editor.<p> 
     * 
     * @return the HTML that generates the form for the XML editor
     */
    public String getXmlEditor() {
        
        StringBuffer result = new StringBuffer(128);

        try {
            String fileName = getParamResource();
            CmsFile file = getCms().readFile(fileName, CmsResourceFilter.IGNORE_EXPIRATION);
            
            CmsXmlContent content = CmsXmlContentFactory.unmarshal(getCms(), file);
            CmsXmlContentDefinition contentDefinition = content.getContentDefinition(new CmsXmlEntityResolver(getCms()));

            int todo = 0;
            // TODO: figure out the locale selected by the user
            Locale locale = (Locale)content.getLocales().get(0);

            result.append("<table class=\"xmlTable\">\n");
            
            List typeSequence = contentDefinition.getTypeSequence();            
            Iterator i = typeSequence.iterator();
            while (i.hasNext()) {
                                                
                I_CmsXmlSchemaType type = (I_CmsXmlSchemaType)i.next();                
                String name = type.getNodeName();
                int count = content.getIndexCount(name, locale);
                for (int j=0; j<count; j++) {

                    I_CmsXmlContentValue value = content.getValue(name, locale, j);
                    I_CmsXmlWidget widget = CmsXmlContentTypeManager.getTypeManager().getEditorWidget(value.getTypeName());
                    result.append(widget.getEditorWidget(getCms(), content, this, value));
                }               
            }
            
            result.append("</table>\n");
            
            
        } catch (Throwable t) {
            OpenCms.getLog(this).error("Error in XML editor", t);
        }
                
        return result.toString();
    }
}
