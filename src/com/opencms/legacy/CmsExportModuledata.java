/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/legacy/Attic/CmsExportModuledata.java,v $
* Date   : $Date: 2004/06/28 07:44:02 $
* Version: $Revision: 1.6 $
*
* This library is part of OpenCms -
* the Open Source Content Mananagement System
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

package com.opencms.legacy;

import org.opencms.file.CmsObject;
import org.opencms.importexport.CmsExport;
import org.opencms.main.CmsException;
import org.opencms.main.I_CmsConstants;
import org.opencms.main.OpenCms;
import org.opencms.report.I_CmsReport;
import org.opencms.util.CmsDateUtil;
import org.opencms.util.CmsXmlSaxWriter;

import com.opencms.defaults.master.CmsMasterContent;
import com.opencms.defaults.master.CmsMasterDataSet;
import com.opencms.defaults.master.CmsMasterMedia;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXWriter;
import org.xml.sax.SAXException;

/**
 * Holds the functionaility to export channels and modulemasters from the cms
 * to the filesystem.
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * @author Michael Emmerich (m.emmerich@alkacon.com)
 * 
 * @version $Revision: 1.6 $ $Date: 2004/06/28 07:44:02 $
 */
public class CmsExportModuledata extends CmsExport implements Serializable {

    /** Manifest tag: master. */   
    public static String C_EXPORT_TAG_MASTER = "master";
    
    /** Manifest tag: master ID. */   
    public static String C_EXPORT_TAG_MASTER_ID = "master_id";    

    /** Manifest tag: access_flags. */   
    public static String C_EXPORT_TAG_MASTER_ACCESSFLAGS = "access_flags";

    /** Manifest tag: channelname. */   
    public static String C_EXPORT_TAG_MASTER_CHANNELNAME = "channelname";

    /** Manifest tag: channelrelations. */   
    public static String C_EXPORT_TAG_MASTER_CHANNELREL = "channelrelations";

    /** Manifest tag: data_big_. */   
    public static String C_EXPORT_TAG_MASTER_DATABIG = "data_big_";

    /** Manifest tag: data_date_. */   
    public static String C_EXPORT_TAG_MASTER_DATADATE = "data_date_";

    /** Manifest tag: data_int_. */   
    public static String C_EXPORT_TAG_MASTER_DATAINT = "data_int_";

    /** Manifest tag: data_medium_. */   
    public static String C_EXPORT_TAG_MASTER_DATAMEDIUM = "data_medium_";

    /** Manifest tag: data_reference_. */   
    public static String C_EXPORT_TAG_MASTER_DATAREFERENCE = "data_reference_";

    /** Manifest tag: dataset. */   
    public static String C_EXPORT_TAG_MASTER_DATASET = "dataset";
    
    /** Manifest tag: data_small_. */   
    public static String C_EXPORT_TAG_MASTER_DATASMALL = "data_small_";
    
    /** Manifest tag: feed_filename. */   
    public static String C_EXPORT_TAG_MASTER_FEEDFILENAME = "feed_filename";
    
    /** Manifest tag: feed_id. */   
    public static String C_EXPORT_TAG_MASTER_FEEDID = "feed_id";
    
    /** Manifest tag: feed_reference. */   
    public static String C_EXPORT_TAG_MASTER_FEEDREFERENCE = "feed_reference";
    
    /** Manifest tag: flags. */   
    public static String C_EXPORT_TAG_MASTER_FLAGS = "flags";
    
    /** Manifest tag: group_name. */   
    public static String C_EXPORT_TAG_MASTER_GROUP = "group_name";
    
    /** Manifest tag: media. */   
    public static String C_EXPORT_TAG_MASTER_MEDIA = "media";
    
    /** Manifest tag: mediaset. */   
    public static String C_EXPORT_TAG_MASTER_MEDIASET = "mediaset";
    
    /** Manifest tag: publication_date. */   
    public static String C_EXPORT_TAG_MASTER_PUBLICATIONDATE = "publication_date";
    
    /** Manifest tag: purge_date. */   
    public static String C_EXPORT_TAG_MASTER_PURGEDATE = "purge_date";
    
    /** Manifest tag: sub_id. */   
    public static String C_EXPORT_TAG_MASTER_SUBID = "sub_id";
    
    /** Manifest tag: title. */   
    public static String C_EXPORT_TAG_MASTER_TITLE = "title";
    
    /** Manifest tag: user_name. */   
    public static String C_EXPORT_TAG_MASTER_USER = "user_name";
    
    /** Manifest tag: masters. */   
    public static String C_EXPORT_TAG_MASTERS = "masters";
    
    /** Manifest tag: media_content. */   
    public static String C_EXPORT_TAG_MEDIA_CONTENT = "media_content";
    
    /** Manifest tag: media_description. */   
    public static String C_EXPORT_TAG_MEDIA_DESCRIPTION = "media_description";
    
    /** Manifest tag: media_height. */   
    public static String C_EXPORT_TAG_MEDIA_HEIGHT = "media_height";
    
    /** Manifest tag: media_mimetype. */   
    public static String C_EXPORT_TAG_MEDIA_MIMETYPE = "media_mimetype";
    
    /** Manifest tag: media_name. */   
    public static String C_EXPORT_TAG_MEDIA_NAME = "media_name";
    
    /** Manifest tag: media_position. */   
    public static String C_EXPORT_TAG_MEDIA_POSITION = "media_position";
    
    /** Manifest tag: media_size. */   
    public static String C_EXPORT_TAG_MEDIA_SIZE = "media_size";
    
    /** Manifest tag: media_title. */   
    public static String C_EXPORT_TAG_MEDIA_TITLE = "media_title";
    
    /** Manifest tag: media_type. */   
    public static String C_EXPORT_TAG_MEDIA_TYPE = "media_type";
    
    /** Manifest tag: media_width. */   
    public static String C_EXPORT_TAG_MEDIA_WIDTH = "media_width";
    
    /** Holds information about contents that have already been exported. */
    private Vector m_exportedMasters = new Vector();

    /**
     * This constructs a new CmsExportModuledata-object which exports the channels and modulemasters.
     *
     * @param cms the cms-object to work with
     * @param exportFile the filename of the zip to export to
     * @param resourcesToExport the cos folders (channels) to export
     * @param modulesToExport the modules to export
     * @param report to write the progress information to
     * 
     * @throws CmsException if something goes wrong
     */
    public CmsExportModuledata(CmsObject cms, String exportFile, String[] resourcesToExport, String[] modulesToExport, I_CmsReport report) throws CmsException {
        setCms(cms);
        setReport(report);
        setExportFileName(exportFile);
        setExportingCosData(true);
        setExportedChannelIds(new HashSet());

        // save the site root
        getCms().getRequestContext().saveSiteRoot();

        // open the export file
        Element exportNode;
        try {
            exportNode = openExportFile();

            // first export the cos folders (ie. channels)               
            getReport().println(getReport().key("report.export_channels_begin"), I_CmsReport.C_FORMAT_HEADLINE);

            getCms().getRequestContext().setSiteRoot(I_CmsConstants.VFS_FOLDER_COS);
            // export all the resources
            exportAllResources(exportNode, resourcesToExport);
            getReport().println(getReport().key("report.export_channels_end"), I_CmsReport.C_FORMAT_HEADLINE);

            // get the modules to export
            Vector modules = new Vector();
            Vector moduleNames = new Vector();
            for (int i = 0; i < modulesToExport.length; i++) {
                String modName = modulesToExport[i];
                if (modName != null && !"".equals(modName)) {
                    moduleNames.addElement(modulesToExport[i]);
                }
            }

            Hashtable moduleExportables = new Hashtable();
            OpenCms.getRegistry().getModuleExportables(moduleExportables);
            // if there was no module selected then select all exportable modules,
            // else get only the modules from Hashtable that were selected
            if (moduleNames.size() == 0) {
                if (resourcesToExport.length > 0) {
                    Enumeration modElements = moduleExportables.elements();
                    while (modElements.hasMoreElements()) {
                        modules.add(modElements.nextElement());
                    }
                }
            } else {
                modules = moduleNames;
            }

            // now do the export for all modules with the given channel ids
            Enumeration enumModules = modules.elements();
            while (enumModules.hasMoreElements()) {
                // get the name of the content definition class
                String classname = (String)enumModules.nextElement();
                exportCos(exportNode, classname, getExportedChannelIds());
            }

            // close the export file
            closeExportFile(exportNode);

        } catch (SAXException se) {
            getReport().println(se);
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Error exporting to file " + getExportFileName(), se);
            }
            throw new CmsException(CmsException.C_UNKNOWN_EXCEPTION, se);
        } catch (IOException ioe) {
            getReport().println(ioe);
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Error exporting to file " + getExportFileName(), ioe);
            }
            throw new CmsException(CmsException.C_UNKNOWN_EXCEPTION, ioe);
        } finally {
            // restore the site root
            getCms().getRequestContext().restoreSiteRoot();
        }
    }

    /**
     * Exports the content definition data,
     * only content definition data from selected channels will be exported.<p>
     * 
     * @param parent the export root node
     * @param classname name of the content definition class 
     * @param exportedChannelIds set of channels that have been exported
     * @throws CmsException if something goes wrong
     * @throws SAXException if something goes wrong procesing the manifest.xml
     */
    private void exportCos(Element parent, String classname, Set exportedChannelIds) throws CmsException, SAXException {
        // output something to the report for the data
        getReport().print(getReport().key("report.export_moduledata_begin"), I_CmsReport.C_FORMAT_HEADLINE);
        getReport().print("<i>" + classname + "</i>", I_CmsReport.C_FORMAT_HEADLINE);
        getReport().println(getReport().key("report.dots"), I_CmsReport.C_FORMAT_HEADLINE);

        Iterator keys = exportedChannelIds.iterator();
        // get the subId of the module
        int subId = getContentDefinition(classname, new Class[] {CmsObject.class}, new Object[] {getCms()}).getSubId();
        // the number for identifying each master
        int masterNr = 1;

        Element masters = parent.addElement(C_EXPORT_TAG_MASTERS);
        getSaxWriter().writeOpen(masters);

        while (keys.hasNext()) {
            String channelId = (String)keys.next();
            try {
                Vector allDatasets = new Vector();
                // execute the static method readAllByChannel of the content definition class
                allDatasets = (Vector)Class.forName(classname).getMethod("readAllByChannel", new Class[] {CmsObject.class, String.class, Integer.class}).invoke(null, new Object[] {getCms(), channelId, new Integer(subId)});

                for (int i = 0; i < allDatasets.size(); i++) {
                    CmsMasterDataSet curDataset = (CmsMasterDataSet)allDatasets.elementAt(i);
                    if (!m_exportedMasters.contains("" + curDataset.m_masterId)) {
                        exportCosModule(masters, classname, curDataset, masterNr, subId);
                        m_exportedMasters.add("" + curDataset.m_masterId);
                        masterNr++;
                    }
                }
            } catch (InvocationTargetException e) {
                getReport().println(e);
                if (OpenCms.getLog(this).isErrorEnabled()) {
                    OpenCms.getLog(this).error("Error exporting COS data for channel id " + channelId, e);
                }
            } catch (NoSuchMethodException e) {
                getReport().println(e);
                if (OpenCms.getLog(this).isErrorEnabled()) {
                    OpenCms.getLog(this).error("Error exporting COS data for channel id " + channelId, e);
                }
            } catch (IllegalArgumentException e) {
                getReport().println(e);
                if (OpenCms.getLog(this).isErrorEnabled()) {
                    OpenCms.getLog(this).error("Error exporting COS data for channel id " + channelId, e);
                }
            } catch (SecurityException e) {
                getReport().println(e);
                if (OpenCms.getLog(this).isErrorEnabled()) {
                    OpenCms.getLog(this).error("Error exporting COS data for channel id " + channelId, e);
                }
            } catch (IllegalAccessException e) {
                getReport().println(e);
                if (OpenCms.getLog(this).isErrorEnabled()) {
                    OpenCms.getLog(this).error("Error exporting COS data for channel id " + channelId, e);
                }
            } catch (ClassNotFoundException e) {
                getReport().println(e);
                if (OpenCms.getLog(this).isErrorEnabled()) {
                    OpenCms.getLog(this).error("Error exporting COS data for channel id " + channelId, e);
                }
            }
        }

        getSaxWriter().writeClose(masters);
        parent.remove(masters);

        getReport().println(getReport().key("report.export_moduledata_end"), I_CmsReport.C_FORMAT_HEADLINE);
    }

    /**
     * Export a single content definition.<P>
     * 
     * @param parent the export root node
     * @param classname name of the content definition class
     * @param dataset data for the content definition object instance
     * @param masterNr id of master
     * @param subId id of content definition
     * 
     * @throws CmsException if something goes wrong
     * @throws SAXException if something goes wrong procesing the manifest.xml
     */
    private void exportCosModule(Element parent, String classname, CmsMasterDataSet dataset, int masterNr, int subId) throws CmsException, SAXException {
        // output something to the report for the resource
        getReport().print(getReport().key("report.exporting"), I_CmsReport.C_FORMAT_NOTE);
        getReport().print("'" + dataset.m_title + "' (id: " + dataset.m_masterId + ")");

        // the name of the XML-file where the dataset is stored
        String dataSetFile = "dataset_" + subId + "_" + masterNr + ".xml";
        // create new mastercontent for getting channels and media
        CmsMasterContent content = getContentDefinition(classname, new Class[] {CmsObject.class, CmsMasterDataSet.class}, new Object[] {getCms(), dataset});
        // write these informations to the xml-manifest
        Element master = parent.addElement(C_EXPORT_TAG_MASTER);

        master.addElement(C_EXPORT_TAG_MASTER_SUBID).addText(Integer.toString(subId));
        // add the name of the datasetfile and create the datasetfile
        // with the information from the dataset
        master.addElement(C_EXPORT_TAG_MASTER_DATASET).addText(dataSetFile);
        exportCosModuleData(dataset, dataSetFile, masterNr, subId);
        // add the channel relation of this master        
        Element channelrel = master.addElement(C_EXPORT_TAG_MASTER_CHANNELREL);
        Vector moduleChannels = content.getChannels();
        for (int i = 0; i < moduleChannels.size(); i++) {
            String channelname = (String)moduleChannels.elementAt(i);
            channelrel.addElement(C_EXPORT_TAG_MASTER_CHANNELNAME).addText(channelname);
        }
        // add the mediaset
        Element mediaset = master.addElement(C_EXPORT_TAG_MASTER_MEDIASET);
        Vector moduleMedia = content.getMedia();
        for (int i = 0; i < moduleMedia.size(); i++) {
            // for each media add the name of the xml-file for the mediadata to the manifest
            // and create the files for the media information
            String mediaFile = "media_" + subId + "_" + masterNr + "_" + i + ".xml";
            mediaset.addElement(C_EXPORT_TAG_MASTER_MEDIA).addText(mediaFile);
            exportCosModuleMedia((CmsMasterMedia)moduleMedia.elementAt(i), mediaFile, masterNr, subId, i);
        }
        // write the XML
        digestElement(parent, master);
        getReport().print(getReport().key("report.dots"));
        getReport().println(getReport().key("report.ok"), I_CmsReport.C_FORMAT_OK);
    }

    /**
     * Exports a content definition content in a "dataset_xxx.xml" file and a number of 
     * "datayyy_xxx.dat" files.<p>
     * 
     * @param dataset data for the content definition object instance
     * @param filename name of the zip file for the module data export
     * @param masterNr id of master
     * @param subId id of content definition
     * 
     * @throws CmsException if something goes wrong
     */
    private void exportCosModuleData(CmsMasterDataSet dataset, String filename, int masterNr, int subId) throws CmsException {

        // create a new xml document
        Document doc = DocumentHelper.createDocument();
        Element data = doc.addElement(I_CmsConstants.C_EXPORT_TAG_MODULEXPORT).addElement(C_EXPORT_TAG_MASTER_DATASET);

        // add the data of the contentdefinition
        // get the name of the owner
        String ownerName = "";
        try {
            ownerName = getCms().readUser(dataset.m_userId).getName();
        } catch (CmsException e) {
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Unable to read user with id " + dataset.m_userId, e);
            }
        }
        // get the name of the group
        String groupName = "";
        try {
            groupName = getCms().readGroup(dataset.m_groupId).getName();
        } catch (CmsException e) {
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Unable to read group with id " + dataset.m_groupId, e);
            }
        }
        
        data.addElement(C_EXPORT_TAG_MASTER_ID).addText(dataset.m_masterId.toString());
        data.addElement(C_EXPORT_TAG_MASTER_USER).addText(ownerName);
        data.addElement(C_EXPORT_TAG_MASTER_GROUP).addText(groupName);
        data.addElement(C_EXPORT_TAG_MASTER_ACCESSFLAGS).addText(Integer.toString(dataset.m_accessFlags));
        data.addElement(C_EXPORT_TAG_MASTER_PUBLICATIONDATE).addText(CmsDateUtil.getDateTimeShort(dataset.m_publicationDate));
        data.addElement(C_EXPORT_TAG_MASTER_PURGEDATE).addText(CmsDateUtil.getDateTimeShort(dataset.m_purgeDate));
        data.addElement(C_EXPORT_TAG_MASTER_FLAGS).addText(Integer.toString(dataset.m_flags));
        data.addElement(C_EXPORT_TAG_MASTER_FEEDID).addText(Integer.toString(dataset.m_feedId));
        data.addElement(C_EXPORT_TAG_MASTER_FEEDREFERENCE).addText(Integer.toString(dataset.m_feedReference));
        data.addElement(C_EXPORT_TAG_MASTER_FEEDFILENAME).addText(dataset.m_feedFilename!=null?dataset.m_feedFilename:"");
        data.addElement(C_EXPORT_TAG_MASTER_TITLE).addCDATA(dataset.m_title!=null?dataset.m_title:"");

        // get the values of data_big from the string array
        for (int i = 0; i < dataset.m_dataBig.length; i++) {
            String value = dataset.m_dataBig[i];
            String dataFile = new String();
            if (value != null && !"".equals(value)) {
                // the name of the file where the value of the field is stored
                dataFile = "databig_" + subId + "_" + masterNr + "_" + i + ".dat";
                writeExportFile(dataFile, value.getBytes());
            }
            data.addElement(C_EXPORT_TAG_MASTER_DATABIG + i).addText(dataFile);
        }
        // get the values of data_medium from the string array
        for (int i = 0; i < dataset.m_dataMedium.length; i++) {
            String value = dataset.m_dataMedium[i];
            String dataFile = new String();
            if (value != null && !"".equals(value)) {
                // the name of the file where the value of the field is stored
                dataFile = "datamedium_" + subId + "_" + masterNr + "_" + i + ".dat";
                writeExportFile(dataFile, value.getBytes());
            }
            data.addElement(C_EXPORT_TAG_MASTER_DATAMEDIUM + i).addText(dataFile);
        }
        // get the values of data_small from the string array
        for (int i = 0; i < dataset.m_dataSmall.length; i++) {
            String value = dataset.m_dataSmall[i];
            String dataFile = new String();
            if (value != null && !"".equals(value)) {
                // the name of the file where the value of the field is stored
                dataFile = "datasmall_" + subId + "_" + masterNr + "_" + i + ".dat";
                writeExportFile(dataFile, value.getBytes());
            }
            data.addElement(C_EXPORT_TAG_MASTER_DATASMALL + i).addText(dataFile);
        }
        // get the values of data_int from the int array
        for (int i = 0; i < dataset.m_dataInt.length; i++) {
            String value = "" + dataset.m_dataInt[i];
            data.addElement(C_EXPORT_TAG_MASTER_DATAINT + i).addText(value);
        }
        // get the values of data_reference from the int array
        for (int i = 0; i < dataset.m_dataReference.length; i++) {
            String value = "" + dataset.m_dataReference[i];
            data.addElement(C_EXPORT_TAG_MASTER_DATAREFERENCE + i).addText(value);
        }
        // get the values of data_reference from the int array
        for (int i = 0; i < dataset.m_dataDate.length; i++) {
            String value = CmsDateUtil.getDateTimeShort(dataset.m_dataDate[i]);
            data.addElement(C_EXPORT_TAG_MASTER_DATADATE + i).addText(value);
        }

        try {
            // set up new zip entry
            ZipEntry entry = new ZipEntry(filename);
            getExportZipStream().putNextEntry(entry);
            // generate the SAX XML writer 
            CmsXmlSaxWriter saxHandler = new CmsXmlSaxWriter(new OutputStreamWriter(getExportZipStream()), OpenCms.getSystemInfo().getDefaultEncoding());                    
            // write the document
            (new SAXWriter(saxHandler, saxHandler)).write(doc);
            // close zip entry
            getExportZipStream().closeEntry();
        } catch (SAXException e) {
            getReport().println(e);
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Unable to write ZIP dataset file " + filename, e);
            }
            throw new CmsException(CmsException.C_UNKNOWN_EXCEPTION, e);
        } catch (IOException e) {
            getReport().println(e);
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Unable to write ZIP dataset file " + filename, e);
            }
            throw new CmsException(CmsException.C_UNKNOWN_EXCEPTION, e);
        }
    }

    /**
     * Exports a media object, creates a "media_xxx.xml" and a "mediacontent_xxx.dat" data file.<p> 
     * 
     * @param media data for the media object instance
     * @param filename name of the xml file for the media data export
     * @param masterNr id of master
     * @param subId id of content definition
     * @param mediaId if od media object
     * 
     * @throws CmsException if something goes wrong
     */
    private void exportCosModuleMedia(CmsMasterMedia media, String filename, int masterNr, int subId, int mediaId) throws CmsException {
        // create a new xml document
        Document doc = DocumentHelper.createDocument();
        Element data = doc.addElement(I_CmsConstants.C_EXPORT_TAG_MODULEXPORT);
        // add the data element
        Element em = data.addElement(C_EXPORT_TAG_MASTER_MEDIA);
        // add the data of the contentdefinition
        em.addElement(C_EXPORT_TAG_MEDIA_POSITION).addText(Integer.toString(media.getPosition()));
        em.addElement(C_EXPORT_TAG_MEDIA_WIDTH).addText(Integer.toString(media.getWidth()));
        em.addElement(C_EXPORT_TAG_MEDIA_HEIGHT).addText(Integer.toString(media.getHeight()));
        em.addElement(C_EXPORT_TAG_MEDIA_SIZE).addText(Integer.toString(media.getSize()));
        em.addElement(C_EXPORT_TAG_MEDIA_MIMETYPE).addText(media.getMimetype()!=null?media.getMimetype():"");
        em.addElement(C_EXPORT_TAG_MEDIA_TYPE).addText(Integer.toString(media.getType()));
        em.addElement(C_EXPORT_TAG_MEDIA_TITLE).addCDATA(media.getTitle()!=null?media.getTitle():"");
        em.addElement(C_EXPORT_TAG_MEDIA_NAME).addCDATA(media.getName()!=null?media.getName():"");
        em.addElement(C_EXPORT_TAG_MEDIA_DESCRIPTION).addCDATA(media.getDescription()!=null?media.getDescription():"");
        // now add the name of the file where the media content is stored and write this file
        String contentFilename = "mediacontent_" + subId + "_" + masterNr + "_" + mediaId + ".dat";
        em.addElement(C_EXPORT_TAG_MEDIA_CONTENT).addText(contentFilename);
        writeExportFile(contentFilename, media.getMedia());
        try {
            // set up new zip entry
            ZipEntry entry = new ZipEntry(filename);
            getExportZipStream().putNextEntry(entry);
            // generate the SAX XML writer 
            CmsXmlSaxWriter saxHandler = new CmsXmlSaxWriter(new OutputStreamWriter(getExportZipStream()), OpenCms.getSystemInfo().getDefaultEncoding());                     
            // write the document
            (new SAXWriter(saxHandler, saxHandler)).write(doc);
            // close zip entry
            getExportZipStream().closeEntry();
        } catch (SAXException e) {
            getReport().println(e);
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Unable to write ZIP media content file " + contentFilename, e);
            }
            throw new CmsException(CmsException.C_UNKNOWN_EXCEPTION, e);
        } catch (IOException e) {
            getReport().println(e);
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Unable to write ZIP media content file " + contentFilename, e);
            }
            throw new CmsException(CmsException.C_UNKNOWN_EXCEPTION, e);
        }
    }

    /**
     * Returns a master content definition object instance created with the reflection API.<p>
     * 
     * @param classname name of the content definition class
     * @param classes required for constructor generation 
     * @param objects instances to be used as parameters for class instance generation
     * 
     * @return a master content definition object instance created with the reflection API
     */
    private CmsMasterContent getContentDefinition(String classname, Class[] classes, Object[] objects) {
        CmsMasterContent cd = null;
        try {
            Class cdClass = Class.forName(classname);
            Constructor co = cdClass.getConstructor(classes);
            cd = (CmsMasterContent)co.newInstance(objects);
        } catch (InvocationTargetException e) {
            getReport().println(e);
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Error generating instance for class " + classname, e);
            }
        } catch (NoSuchMethodException e) {
            getReport().println(e);
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Error generating instance for class " + classname, e);
            }
        } catch (InstantiationException e) {
            getReport().println(e);
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Error generating instance for class " + classname, e);
            }
        } catch (ClassNotFoundException e) {
            getReport().println(e);
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Error generating instance for class " + classname, e);
            }
        } catch (IllegalArgumentException e) {
            getReport().println(e);
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Error generating instance for class " + classname, e);
            }
        } catch (IllegalAccessException e) {
            getReport().println(e);
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Error generating instance for class " + classname, e);
            }
        }
        return cd;
    }

    /**
     * Writes a binary content to a "*.dat" file in the export zip.<p>
     * 
     * @param filename name of the file, usually ends with .dat
     * @param content contents to write to the file
     */
    private void writeExportFile(String filename, byte[] content) {
        try {
            // store the userinfo in zip-file
            ZipEntry entry = new ZipEntry(filename);
            getExportZipStream().putNextEntry(entry);
            getExportZipStream().write(content);
            getExportZipStream().closeEntry();
        } catch (IOException ioex) {
            getReport().println(ioex);
            if (OpenCms.getLog(this).isErrorEnabled()) {
                OpenCms.getLog(this).error("Unable to write ZIP for filename " + filename, ioex);
            }
        }
    }
}