/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/explorer/CmsNewCsvFile.java,v $
 * Date   : $Date: 2005/06/02 13:57:07 $
 * Version: $Revision: 1.7 $
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

package org.opencms.workplace.explorer;

import org.opencms.file.CmsProperty;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.I_CmsConstants;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsStringUtil;
import org.opencms.workplace.CmsWorkplaceException;
import org.opencms.workplace.I_CmsWpConstants;
import org.opencms.xml.CmsXmlException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;

/**
 * The new resource upload dialog handles the upload of cvs files. They are converted in a first step to xml
 * and in a second step transformed via a xsl stylesheet.<p>
 * 
 * The following files use this class:
 * <ul>
 * <li>/commons/newcvsfile_upload.jsp
 * </ul>
 * 
 * @author Jan Baudisch (j.baudisch@alkacon.com)
 * @version $Revision: 1.7 $
 * 
 * @since 5.7.3
 */
public class CmsNewCsvFile extends CmsNewResourceUpload {

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsNewCsvFile.class);  
    
    /** Constant for automatically selecting the best fitting delimiter. */
    public static final String BEST_DELIMITER = "best";

    /** Constant for the charset with which the csv-data is encoded. */
    public static final String CHARSET = "ISO-8859-1";

    /** Request parameter name for the CSV content. */
    public static final String PARAM_CSVCONTENT = "csvcontent";

    /** Request parameter name for the delimiter. */
    public static final String PARAM_DELIMITER = "delimiter";

    /** Request parameter name for the XSLT file. */
    public static final String PARAM_XSLTFILE = "xsltfile";

    /** Constant for the xslt file suffix for table transformations. */
    public static final String TABLE_XSLT_SUFFIX = ".table.xslt";

    /** Constant for the tab-value inside delimiter the select. */
    public static final String TABULATOR = "tab";

    /** The delimiter to separate the text. */
    public static final char TEXT_DELIMITER = '"';

    /** Constant for the height of the dialog frame. */
    public static final String FRAMEHEIGHT = "450";

    /** the delimiters, the csv data can be separated with.*/
    static final String[] C_DELIMITERS = {";", ",", "\t"};

    /**
     * Converts CSV data to xml.<p>
     * 
     * @return a XML representation of the csv data
     * 
     * @param csvData the csv data to convert
     * @param delimiter the delimiter to separate the values with
     * 
     * @throws IOException if there is an IO problem
     */
    public static String convertCsvToXml(String csvData, String delimiter) throws IOException {

        StringBuffer xml = new StringBuffer(64);
        xml.append("<table>\n");
        String line;
        BufferedReader br = new BufferedReader(new StringReader(csvData));
        while ((line = br.readLine()) != null) {
            xml.append("<tr>\n");
            String[] words = CmsStringUtil.splitAsArray(line, delimiter);
            for (int i = 0; i < words.length; i++) {
                xml.append("\t<td>").append(removeStringDelimiters(words[i])).append("</td>\n");
            }
            xml.append("</tr>\n");
        }
        return xml.append("</table>").toString();
    }

    /**                                                                                                                              
     * Removes the string delimiters from a key (as well as any white space                                                          
     * outside the delimiters).<p>                                                                                                      
     *                                                                                                                               
     * @param key the key (including delimiters)                                                                                   
     *                                                                                                                               
     * @return the key without delimiters                                                                                           
     */
    private static String removeStringDelimiters(String key) {

        String k = key.trim();
        if (CmsStringUtil.isNotEmpty(k)) {
            if (k.charAt(0) == TEXT_DELIMITER) {
                k = k.substring(1);
            }
            if (k.charAt(k.length() - 1) == TEXT_DELIMITER) {
                k = k.substring(0, k.length() - 1);
            }
        }
        return k;
    }

    /** The pasted CSV content. */
    private String m_paramCsvContent;

    /** The delimiter to separate the CSV values. */
    private String m_paramDelimiter;

    /** The XSLT File to transform the table with. */
    private String m_paramXsltFile;

    /**
     * Public constructor with JSP action element.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsNewCsvFile(CmsJspActionElement jsp) {

        super(jsp);
    }

    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */
    public CmsNewCsvFile(PageContext context, HttpServletRequest req, HttpServletResponse res) {

        this(new CmsJspActionElement(context, req, res));
    }

    /**
     * Uploads the specified file and transforms it to HTML.<p>
     * 
     * @throws JspException if inclusion of error dialog fails
     */
    public void actionUpload() throws JspException {

        String newResname = "";

        try {
            if (CmsStringUtil.isNotEmpty(getParamCsvContent())) {
                // csv content is pasted in the textarea
                newResname = "csvcontent.html";
                setParamNewResourceName("");
            } else {
                setParamCsvContent(new String(getFileContentFromUpload(), CHARSET));
                newResname = getCms().getRequestContext().getFileTranslator().translateResource(
                    CmsResource.getName(getParamResource().replace('\\', '/')));
                newResname = CmsStringUtil.changeFileNameSuffixTo(newResname, "html");
                setParamNewResourceName(newResname);
            }

            setParamResource(newResname);
            setParamResource(computeFullResourceName());
            int resTypeId = OpenCms.getResourceManager().getDefaultTypeForName(newResname).getTypeId();

            String xmlContent = "";
            CmsProperty styleProp = CmsProperty.getNullProperty();
            if (TABULATOR.equals(getParamDelimiter())) {
                setParamDelimiter("\t");
            } else if (BEST_DELIMITER.equals(getParamDelimiter())) {
                setParamDelimiter(getPreferredDelimiter(getParamCsvContent()));
            }

            try {
                xmlContent = convertCsvToXml(getParamCsvContent(), getParamDelimiter());
            } catch (IOException e) {
                throw new CmsXmlException(Messages.get().container(Messages.ERR_CSV_XML_TRANSFORMATION_FAILED_0));    
            }    

            if (CmsStringUtil.isNotEmpty(getParamXsltFile())) {

                xmlContent = applyXslTransformation(getParamXsltFile(), xmlContent);
                styleProp = getCms().readPropertyObject(getParamXsltFile(), I_CmsConstants.C_PROPERTY_STYLESHEET, true);
            }
            byte[] content = xmlContent.getBytes();

            try {
                // create the resource
                getCms().createResource(getParamResource(), resTypeId, content, Collections.EMPTY_LIST);
            } catch (CmsException e) {
                // resource was present, overwrite it
                getCms().lockResource(getParamResource());
                getCms().replaceResource(getParamResource(), resTypeId, content, null);
            }
            // copy xslt stylesheet-property to the new resource
            if (!styleProp.isNullProperty()) { 
                getCms().writePropertyObject(getParamResource(), styleProp);
            }
        } catch (Throwable e) {
            // error uploading file, show error dialog
            setParamMessage(Messages.get().getBundle(getLocale()).key(Messages.ERR_TABLE_IMPORT_FAILED_0));
            includeErrorpage(this, e);  
        }
    }

    /**
     * Returns the content of the file upload and sets the resource name.<p>
     * 
     * @return the byte content of the uploaded file
     * @throws CmsWorkplaceException if the filesize if greater that maxFileSizeBytes or if the upload file cannot be found
     */
    public byte[] getFileContentFromUpload() throws CmsWorkplaceException {

        byte[] content;
        // get the file item from the multipart request
        Iterator i = getMultiPartFileItems().iterator();
        FileItem fi = null;
        while (i.hasNext()) {
            fi = (FileItem)i.next();
            if (fi.getName() != null) {
                // found the file object, leave iteration
                break;
            } else {
                // this is no file object, check next item
                continue;
            }
        }

        if (fi != null) {
            long size = fi.getSize();
            if (size == 0) {
                throw new CmsWorkplaceException(Messages.get().container(Messages.ERR_UPLOAD_FILE_NOT_FOUND_0));
            }
            long maxFileSizeBytes = OpenCms.getWorkplaceManager().getFileBytesMaxUploadSize(getCms());
            // check file size
            if (maxFileSizeBytes > 0 && size > maxFileSizeBytes) {
                throw new CmsWorkplaceException(Messages.get().container(
                    Messages.ERR_UPLOAD_FILE_SIZE_TOO_HIGH_1, new Long(maxFileSizeBytes / 1024)));
            }
            content = fi.get();
            fi.delete();
            setParamResource(fi.getName());

        } else {
            throw new CmsWorkplaceException(Messages.get().container(Messages.ERR_UPLOAD_FILE_NOT_FOUND_0));
        }
        return content;
    }

    /**
     * Applies a XSLT Transformation to the xmlContent.<p>
     * 
     * @param xsltFile the XSLT transformation file
     * @param xmlContent the XML content to transform
     * @return the transformed xml
     */
    public String applyXslTransformation(String xsltFile, String xmlContent) {

        try {
            TransformerFactory factory = TransformerFactory.newInstance();

            InputStream stylesheet = new ByteArrayInputStream(getCms().readFile(xsltFile).getContents());
            Transformer transformer = factory.newTransformer(new StreamSource(stylesheet));
            Document document = DocumentHelper.parseText(xmlContent);
            DocumentSource source = new DocumentSource(document);
            DocumentResult streamResult = new DocumentResult();
            // transform the xml with the xslt stylesheet
            transformer.transform(source, streamResult);
            return streamResult.getDocument().asXML();

        } catch (Exception e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(e);
            }
            return "";
        }
    }

    /**
     * Builds a html select for the XSLT files.
     * 
     * @return html select code with the possible available xslt files
     */
    public String buildXsltSelect() {

        // read all xslt files
        List xsltFiles = getXsltFiles();
        if (xsltFiles.size() > 0) {
            List options = new ArrayList();
            List values = new ArrayList();

            options.add(key("input.nostyle"));
            values.add("");

            CmsResource resource;
            CmsProperty titleProp = null;

            Iterator i = xsltFiles.iterator();
            while (i.hasNext()) {

                resource = (CmsResource)i.next();
                try {
                    titleProp = getCms().readPropertyObject(
                        resource.getRootPath(),
                        I_CmsConstants.C_PROPERTY_TITLE,
                        false);
                } catch (CmsException e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn(e);
                    }    
                }
                values.add(resource.getRootPath());
                // display the title if set or otherwise the filename
                if (titleProp.isNullProperty()) {
                    options.add("[" + resource.getName() + "]");
                } else {
                    options.add(titleProp.getValue());
                }
            }

            StringBuffer result = new StringBuffer(512);
            // build a select box and a table row around
            result.append("<tr><td style=\"white-space: nowrap;\" unselectable=\"on\">");
            result.append(key("input.xsltfile"));
            result.append("</td><td class=\"maxwidth\">");
            String parameters = "class=\"maxwidth\" name=\"" + PARAM_XSLTFILE + "\"";
            result.append(buildSelect(parameters, options, values, 0));
            result.append("</td><tr>");
            return result.toString();
        } else {
            return "";
        }
    }

    /**
     * Builds a html select for Delimiters.
     * 
     * @return html select code with the possible available xslt files
     */
    public String buildDelimiterSelect() {

        Object[] optionStrings = new Object[] {
            key("input.bestmatching"),
            key("input.semicolon"),
            key("input.comma"),
            key("input.tab")};
        List options = new ArrayList(Arrays.asList(optionStrings));
        List values = new ArrayList(Arrays.asList(new Object[] {"best", ";", ",", "tab"}));
        String parameters = "name=\"" + PARAM_DELIMITER + "\" class=\"maxwidth\"";
        return buildSelect(parameters, options, values, 0);
    }

    /**
     * Returns the height of the head frameset.<p>
     * 
     * @return the height of the head frameset
     */
    public String getHeadFrameSetHeight() {

        return FRAMEHEIGHT;
    }

    /**
     * Returns the pasted csv content.<p>
     *
     * @return the csv content
     */
    public String getParamCsvContent() {

        return m_paramCsvContent;
    }

    /**
     * Returns the delimiter to separate the CSV values.<p>
     * 
     * @return the delimiter to separate the CSV values
     */
    public String getParamDelimiter() {

        return m_paramDelimiter;
    }

    /**
     * Returns the xslt file to transform the xml with.<p>
     * 
     * @return the path to the xslt file to transform the xml with or null if it is not set
     */
    public String getParamXsltFile() {

        return m_paramXsltFile;
    }

    /**
     * returns the Delimiter that most often occures in the CSV content.<p>
     * 
     * @param csvData the comma separated values
     * 
     * @return the delimiter, that is best applicable for the csvData
     */
    public String getPreferredDelimiter(String csvData) {

        String bestMatch = "";
        int bestMatchCount = 0;
        // find for each delimiter, how often it occures in the String csvData
        for (int i = 0; i < C_DELIMITERS.length; i++) {
            int currentCount = csvData.split(C_DELIMITERS[i]).length;
            if (currentCount > bestMatchCount) {
                bestMatch = C_DELIMITERS[i];
                bestMatchCount = currentCount;
            }
        }
        return bestMatch;
    }

    /**
     * Returns a list of CmsResources with the xslt files in the modules folder.<p>
     * 
     * @return a list of the available xslt files
     */
    public List getXsltFiles() {

        List result = new ArrayList();
        try {
            int resourceTypeGenericXmlContent = OpenCms.getResourceManager().getResourceType("xmlcontent").getTypeId();
            // find all files of generic xmlcontent in the modules folder
            Iterator xmlFiles = getCms().readResources(
                I_CmsWpConstants.C_VFS_PATH_MODULES,
                CmsResourceFilter.DEFAULT_FILES.addRequireType(resourceTypeGenericXmlContent),
                true).iterator();
            while (xmlFiles.hasNext()) {
                CmsResource xmlFile = (CmsResource)xmlFiles.next();
                // filter all files with the suffix .table.xml
                if (xmlFile.getName().endsWith(TABLE_XSLT_SUFFIX)) {
                    result.add(xmlFile);
                }
            }
        } catch (CmsException e) {
            LOG.error(e);
        }
        return result;

    }

    /**
     * Sets the pasted csv content.<p>
     *
     * @param csvContent the csv content to set
     */
    public void setParamCsvContent(String csvContent) {

        m_paramCsvContent = csvContent;
    }

    /**
     * Sets the delimiter to separate the CSV values.<p>
     * 
     * @param delimiter the delimiter to separate the CSV values.
     */
    public void setParamDelimiter(String delimiter) {

        m_paramDelimiter = delimiter;
    }

    /**
     * Sets the path to the xslt file.<p>
     * 
     * @param xsltFile the file to transform the xml with.
     */
    public void setParamXsltFile(String xsltFile) {

        m_paramXsltFile = xsltFile;
    }

}