/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/explorer/CmsNewCsvFile.java,v $
 * Date   : $Date: 2005/12/08 11:53:50 $
 * Version: $Revision: 1.23.2.5 $
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

import org.opencms.file.CmsProperty;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.types.CmsResourceTypePlain;
import org.opencms.i18n.CmsEncoder;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsStringUtil;
import org.opencms.workplace.CmsWorkplace;
import org.opencms.workplace.CmsWorkplaceException;
import org.opencms.xml.CmsXmlException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;

/**
 * The new resource upload dialog handles the upload of cvs files. They are converted in a first step to xml
 * and in a second step transformed via a xsl stylesheet.<p>
 * 
 * The following files use this class:
 * <ul>
 * <li>/commons/newcvsfile_upload.jsp
 * </ul>
 * <p>
 * 
 * @author Jan Baudisch 
 * 
 * @version $Revision: 1.23.2.5 $ 
 * 
 * @since 6.0.0 
 */
public class CmsNewCsvFile extends CmsNewResourceUpload {

    /** Constant for automatically selecting the best fitting delimiter. */
    public static final String BEST_DELIMITER = "best";

    /** Constant for the height of the dialog frame. */
    public static final String FRAMEHEIGHT = "450";

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

    /** The delimiter to start a tag */
    public static final String TAG_START_DELIMITER = "<";
    
    /** The delimiter to end a tag */
    public static final String TAG_END_DELIMITER = ">";  
    
    /** the delimiters, the csv data can be separated with.*/
    static final String[] DELIMITERS = {";", ",", "\t"};

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsNewCsvFile.class);

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
     * Converts a delimiter separated format string int o colgroup html fragment.<p>
     * @param formatString the formatstring to convert
     * @param delimiter the delimiter the formats (l,r or c) are delimited with
     * 
     * @return the resulting colgroup HTML
     */
    private static String getColGroup(String formatString, String delimiter) {

        StringBuffer colgroup = new StringBuffer(128);
        String[] formatStrings = formatString.split(delimiter);
        colgroup.append("<colgroup>");
        for (int i = 0; i < formatStrings.length; i++) {
            colgroup.append("<col align=\"");
            char align = formatStrings[i].trim().charAt(0);
            switch (align) {
                case 'l':
                    colgroup.append("left");
                    break;
                case 'c':
                    colgroup.append("center");
                    break;
                case 'r':
                    colgroup.append("right");
                    break;
                default:
                    throw new RuntimeException("invalid format option");
            }
            colgroup.append("\"/>");
        }
        return colgroup.append("</colgroup>").toString();
    }

    /**
     * Tests if the given string is a <code>delimiter</code> separated list of Formatting Information.<p>
     * 
     * @param formatString the string to check
     * @param delimiter the list separators
     * 
     * @return true if the string is a <code>delimiter</code> separated list of Formatting Information
     */
    private static boolean isFormattingInformation(String formatString, String delimiter) {

        String[] formatStrings = formatString.split(delimiter);
        for (int i = 0; i < formatStrings.length; i++) {
            if (!formatStrings[i].trim().matches("[lcr]")) {
                return false;
            }
        }
        return true;
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
        // replace excel protected quotations marks ("") by single quotation marks
        k = CmsStringUtil.substitute(k, "\"\"", "\"");
        return k;
    }

    /**
     * Embeds the given content as cdata if neccessary.<p>
     * Contents starting with "<" and ending with ">" are NOT embedded in order to allow content with tags.
     * 
     * @param content the content
     * @return the embedded content
     */
    static String toXmlBody(String content) {

        StringBuffer xmlBody = new StringBuffer(1024);
        content = content.trim();

        if (content.startsWith("<") && content.endsWith(">")) {
            return content;
        } else {
            xmlBody.append("<![CDATA[");
            xmlBody.append(content);
            xmlBody.append("]]>");
        }

        return xmlBody.toString();
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
                setParamCsvContent(new String(getFileContentFromUpload(), CmsEncoder.ENCODING_ISO_8859_1));
                newResname = getCms().getRequestContext().getFileTranslator().translateResource(
                    CmsResource.getName(getParamResource().replace('\\', '/')));
                newResname = CmsStringUtil.changeFileNameSuffixTo(newResname, "html");
                setParamNewResourceName(newResname);
            }

            setParamResource(newResname);
            setParamResource(computeFullResourceName());
            int resTypeId = OpenCms.getResourceManager().getDefaultTypeForName(newResname).getTypeId();

            CmsProperty styleProp = CmsProperty.getNullProperty();

            // set the delimiter
            String delimiter = getParamDelimiter();
            if (TABULATOR.equals(delimiter)) {
                delimiter = "\t";
            } else if (BEST_DELIMITER.equals(delimiter)) {
                delimiter = getPreferredDelimiter(getParamCsvContent());
            }
            setParamDelimiter(delimiter);

            // transform csv to html
            String xmlContent = "";
            try {
                xmlContent = getTableHtml();
            } catch (IOException e) {
                throw new CmsXmlException(Messages.get().container(Messages.ERR_CSV_XML_TRANSFORMATION_FAILED_0));
            }

            // if xslt file parameter is set, transform the raw html and set the css stylesheet property
            // of the converted file to that of the stylesheet
            if (CmsStringUtil.isNotEmpty(getParamXsltFile())) {
                xmlContent = applyXslTransformation(getParamXsltFile(), xmlContent);
                styleProp = getCms().readPropertyObject(
                    getParamXsltFile(),
                    CmsPropertyDefinition.PROPERTY_STYLESHEET,
                    true);
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
     * Applies a XSLT Transformation to the xmlContent.<p>
     * 
     * The method does not use DOM4J, because iso-8859-1 code ist not transformed correctly.
     * 
     * @param xsltFile the XSLT transformation file
     * @param xmlContent the XML content to transform
     * 
     * @return the transformed xml
     * 
     * @throws Exception if something goes wrong
     */
    public String applyXslTransformation(String xsltFile, String xmlContent) throws Exception {

        // JAXP reads data
        Source xmlSource = new StreamSource(new StringReader(xmlContent));
        String xsltString = new String(getCms().readFile(xsltFile).getContents());
        Source xsltSource = new StreamSource(new StringReader(xsltString));

        TransformerFactory transFact = TransformerFactory.newInstance();
        Transformer trans = transFact.newTransformer(xsltSource);

        StringWriter writer = new StringWriter();
        trans.transform(xmlSource, new StreamResult(writer));
        String result = writer.toString();
        // cut of the prefacing declaration '<?xml version="1.0" encoding="UTF-8"?>'
        if (result.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")) {
            return result.substring(38);
        } else {
            return result;
        }
    }

    /**
     * Builds a html select for Delimiters.
     * 
     * @return html select code with the possible available xslt files
     */
    public String buildDelimiterSelect() {

        Object[] optionStrings = new Object[] {
            key(Messages.GUI_NEWRESOURCE_CONVERSION_DELIM_BEST_0),
            key(Messages.GUI_NEWRESOURCE_CONVERSION_DELIM_SEMICOLON_0),
            key(Messages.GUI_NEWRESOURCE_CONVERSION_DELIM_COMMA_0),
            key(Messages.GUI_NEWRESOURCE_CONVERSION_DELIM_TAB_0)};
        List options = new ArrayList(Arrays.asList(optionStrings));
        List values = new ArrayList(Arrays.asList(new Object[] {"best", ";", ",", "tab"}));
        String parameters = "name=\"" + PARAM_DELIMITER + "\" class=\"maxwidth\"";
        return buildSelect(parameters, options, values, 0);
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

            options.add(key(Messages.GUI_NEWRESOURCE_CONVERSION_NOSTYLE_0));
            values.add("");

            CmsResource resource;
            CmsProperty titleProp = null;

            Iterator i = xsltFiles.iterator();
            while (i.hasNext()) {

                resource = (CmsResource)i.next();
                try {
                    titleProp = getCms().readPropertyObject(
                        resource.getRootPath(),
                        CmsPropertyDefinition.PROPERTY_TITLE,
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
            result.append(key(Messages.GUI_NEWRESOURCE_CONVERSION_XSLTFILE_0));
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
                    Messages.ERR_UPLOAD_FILE_SIZE_TOO_HIGH_1,
                    new Long(maxFileSizeBytes / 1024)));
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
        for (int i = 0; i < DELIMITERS.length; i++) {
            int currentCount = csvData.split(DELIMITERS[i]).length;
            if (currentCount > bestMatchCount) {
                bestMatch = DELIMITERS[i];
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
            // find all files of generic xmlcontent in the modules folder
            Iterator xmlFiles = getCms().readResources(CmsWorkplace.VFS_PATH_MODULES,
                CmsResourceFilter.DEFAULT_FILES.addRequireType(CmsResourceTypePlain.getStaticTypeId()),
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

    /**
     * Converts CSV data to xml.<p>
     * 
     * @return a XML representation of the csv data
     * 
     * @param csvData the csv data to convert
     * @param colGroup the format definitions for the table columns, can be null
     * @param delimiter the delimiter to separate the values with
     * 
     * @throws IOException if there is an IO problem
     */
    private String getTableHtml() throws IOException {

        String csvData = getParamCsvContent();
        String lineSeparator = System.getProperty("line.separator");
        int index = csvData.indexOf(lineSeparator);
        if (index == -1) {
            // e.g. on Windows machines, the line separator is \r\n, but the lines of
            // pasted data are separated by \n
            lineSeparator = "\n";
            index = csvData.indexOf("\n");
        }
        String formatString = csvData.substring(0, index);
        String delimiter = getParamDelimiter();

        StringBuffer xml = new StringBuffer("<table>");
        if (isFormattingInformation(formatString, delimiter)) {
            // transform formatting to HTML colgroup
            xml.append(getColGroup(formatString, delimiter));
            // cut of first line
            csvData = csvData.substring(formatString.length() + lineSeparator.length());
        }

        String line;
        BufferedReader br = new BufferedReader(new StringReader(csvData));
        while ((line = br.readLine()) != null) {
            xml.append("<tr>\n");
            String[] words = CmsStringUtil.splitAsArray(line, delimiter);
            for (int i = 0; i < words.length; i++) {
                xml.append("\t<td>");
                // in order to allow links, lines starting and ending with tag delimiters (< ...>) remains unescaped
                if (words[i].startsWith(TAG_START_DELIMITER) && words[i].endsWith(TAG_END_DELIMITER)) {
                    xml.append(removeStringDelimiters(words[i]));
                } else {
                    xml.append(CmsStringUtil.escapeHtml(removeStringDelimiters(words[i])));
                }
                xml.append("</td>\n");
            }
            xml.append("</tr>\n");
        }

        return xml.append("</table>").toString();
    }

}