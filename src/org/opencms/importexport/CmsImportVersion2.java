/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/importexport/CmsImportVersion2.java,v $
 * Date   : $Date: 2004/02/11 14:23:48 $
 * Version: $Revision: 1.34 $
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

package org.opencms.importexport;

import org.opencms.loader.CmsXmlPageLoader;
import org.opencms.main.OpenCms;
import org.opencms.page.CmsXmlPage;
import org.opencms.report.I_CmsReport;
import org.opencms.util.CmsStringSubstitution;
import org.opencms.util.CmsUUID;

import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.file.CmsFile;
import com.opencms.file.CmsObject;
import com.opencms.file.CmsPropertydefinition;
import com.opencms.file.CmsResource;
import com.opencms.file.CmsResourceTypeFolder;
import com.opencms.file.CmsResourceTypeLink;
import com.opencms.file.CmsResourceTypePage;
import com.opencms.file.CmsResourceTypePlain;
import com.opencms.file.CmsResourceTypeXmlPage;
import com.opencms.template.A_CmsXmlContent;
import com.opencms.workplace.I_CmsWpConstants;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implementation of the OpenCms Import Interface ({@link org.opencms.importexport.I_CmsImport}) for 
 * the import version 2.</p>
 * 
 * This import format was used in OpenCms 5.0.0 - 5.1.2.</p>
 *
 * @author Michael Emmerich (m.emmerich@alkacon.com)
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * 
 * @see org.opencms.importexport.A_CmsImport
 */
public class CmsImportVersion2 extends A_CmsImport {

    /** Web application names for conversion support */
    protected List m_webAppNames = new ArrayList();

    /** Old webapp URL for import conversion */
    protected String m_webappUrl = null;
    
    /** folder storage for page file and body coversion */
    private List m_folderStorage;

    /** page file storage for page file and body coversion */
    private List m_pageStorage;
    
    /**
     * Returns the import version of the import implementation.<p>
     * 
     * @return import version
     */
    public int getVersion() {
        return 2;
    }


    /**
     * Creates a new CmsImportVerion2 object.<p>
     */
    public CmsImportVersion2() {
        m_importVersion = 2;
        m_convertToXmlPage = true;
    }


    /**
     * Imports the resources for a module.<p>
     * @param cms the current cms object
     * @param importPath the path in the cms VFS to import into
     * @param report a report object to output the progress information to
     * @param digest digest for taking a fingerprint of the files
     * @param importResource  the import-resource (folder) to load resources from
     * @param importZip the import-resource (zip) to load resources from
     * @param docXml the xml manifest-file 
     * @param excludeList filenames of files and folders which should not 
     *      be (over)written in the virtual file system (not used when null)
     * @param writtenFilenames filenames of the files and folder which have actually been 
     *      successfully written (not used when null)
     * @param fileCodes code of the written files (for the registry)
     *      (not used when null)
     * @param propertyName name of a property to be added to all resources
     * @param propertyValue value of that property
     * @throws CmsException if something goes wrong
     */
    public void importResources(CmsObject cms, String importPath, I_CmsReport report, MessageDigest digest, File importResource, ZipFile importZip, Document docXml, Vector excludeList, Vector writtenFilenames, Vector fileCodes, String propertyName, String propertyValue) throws CmsException {
        // initialize the import
        m_cms = cms;
        m_importPath = importPath;
        m_report = report;
        m_digest = digest;
        m_importResource = importResource;
        m_importZip = importZip;
        m_docXml = docXml;
        m_importingChannelData = false;
        m_folderStorage = new LinkedList();
        m_pageStorage = new ArrayList();
        m_importedPages = new Vector();   
        m_linkStorage = new HashMap();
        m_linkPropertyStorage = new HashMap();
        
        try {
            // first import the user information
            if (m_cms.isAdmin()) {
                importGroups();
                importUsers();
            }
            // now import the VFS resources
            importAllResources(excludeList, writtenFilenames, fileCodes, propertyName, propertyValue);
            convertPointerToLinks();
        } catch (CmsException e) {
            throw e;
        }
    }

    /**
     * Imports the groups and writes them to the cms.<p>
     * 
     * @throws CmsException if something goes wrong
     */
    private void importGroups() throws CmsException {
        NodeList groupNodes;
        Element currentElement;
        String id, name, description, flags, parentgroup;
        try {
            // getAll group nodes
            groupNodes = m_docXml.getElementsByTagName(I_CmsConstants.C_EXPORT_TAG_GROUPDATA);
            // walk through all groups in manifest
            for (int i = 0; i < groupNodes.getLength(); i++) {
                currentElement = (Element)groupNodes.item(i);
                id = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_ID);
                name = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_NAME);
                description = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_DESCRIPTION);
                flags = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_FLAGS);
                parentgroup = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_PARENTGROUP);
                // import this group
                importGroup(id, name, description, flags, parentgroup);
            }

            // now try to import the groups in the stack
            while (!m_groupsToCreate.empty()) {
                Stack tempStack = m_groupsToCreate;
                m_groupsToCreate = new Stack();
                while (tempStack.size() > 0) {
                    Hashtable groupdata = (Hashtable)tempStack.pop();
                    id = (String)groupdata.get(I_CmsConstants.C_EXPORT_TAG_ID);
                    name = (String)groupdata.get(I_CmsConstants.C_EXPORT_TAG_NAME);
                    description = (String)groupdata.get(I_CmsConstants.C_EXPORT_TAG_DESCRIPTION);
                    flags = (String)groupdata.get(I_CmsConstants.C_EXPORT_TAG_FLAGS);
                    parentgroup = (String)groupdata.get(I_CmsConstants.C_EXPORT_TAG_PARENTGROUP);
                    // try to import the group
                    importGroup(id, name, description, flags, parentgroup);
                }
            }
        } catch (Exception exc) {
            m_report.println(exc);
            throw new CmsException(CmsException.C_UNKNOWN_EXCEPTION, exc);
        }
    }

    /**
      * Imports the users and writes them to the cms.<p>
      * 
      * @throws CmsException if something goes wrong
      */
    private void importUsers() throws CmsException {
        NodeList userNodes;
        NodeList groupNodes;
        Element currentElement, currentGroup;
        Vector userGroups;
        Hashtable userInfo = new Hashtable();
        sun.misc.BASE64Decoder dec;
        String id, name, description, flags, password, recoveryPassword, firstname, lastname, email, address, section, defaultGroup, type, pwd, infoNode;
        // try to get the import resource
        //getImportResource();
        try {
            // getAll user nodes
            userNodes = m_docXml.getElementsByTagName(I_CmsConstants.C_EXPORT_TAG_USERDATA);
            // walk threw all groups in manifest
            for (int i = 0; i < userNodes.getLength(); i++) {
                currentElement = (Element)userNodes.item(i);
                id = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_ID);
                name = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_NAME);
                // decode passwords using base 64 decoder
                dec = new sun.misc.BASE64Decoder();
                pwd = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_PASSWORD);
                password = new String(dec.decodeBuffer(pwd.trim()));
                dec = new sun.misc.BASE64Decoder();
                pwd = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_RECOVERYPASSWORD);
                recoveryPassword = new String(dec.decodeBuffer(pwd.trim()));

                description = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_DESCRIPTION);
                flags = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_FLAGS);
                firstname = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_FIRSTNAME);
                lastname = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_LASTNAME);
                email = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_EMAIL);
                address = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_ADDRESS);
                section = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_SECTION);
                defaultGroup = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_DEFAULTGROUP);
                type = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_TYPE);
                // get the userinfo and put it into the hashtable
                infoNode = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_USERINFO);
                try {
                    // read the userinfo from the dat-file
                    byte[] value = getFileBytes(infoNode);
                    // deserialize the object
                    ByteArrayInputStream bin = new ByteArrayInputStream(value);
                    ObjectInputStream oin = new ObjectInputStream(bin);
                    userInfo = (Hashtable)oin.readObject();
                    value = null;
                } catch (IOException ioex) {
                    m_report.println(ioex);
                }

                // get the groups of the user and put them into the vector
                groupNodes = currentElement.getElementsByTagName(I_CmsConstants.C_EXPORT_TAG_GROUPNAME);
                userGroups = new Vector();
                for (int j = 0; j < groupNodes.getLength(); j++) {
                    currentGroup = (Element)groupNodes.item(j);
                    userGroups.addElement(getTextNodeValue(currentGroup, I_CmsConstants.C_EXPORT_TAG_NAME));
                }
                // import this user
                importUser(id, name, description, flags, password, recoveryPassword, firstname, lastname, email, address, section, defaultGroup, type, userInfo, userGroups);
            }
        } catch (Exception exc) {
            m_report.println(exc);
            throw new CmsException(CmsException.C_UNKNOWN_EXCEPTION, exc);
        }
    }

    /**
     * Imports the resources and writes them to the cms.<p>
     * 
     * @param excludeList filenames of files and folders which should not 
     *      be (over)written in the virtual file system (not used when null)
     * @param writtenFilenames filenames of the files and folder which have actually been 
     *      successfully written (not used when null)
     * @param fileCodes code of the written files (for the registry)
     *      (not used when null)
     * @param propertyName name of a property to be added to all resources
     * @param propertyValue value of that property
     * @throws CmsException if something goes wrong
     */
    private void importAllResources(Vector excludeList, Vector writtenFilenames, Vector fileCodes, String propertyName, String propertyValue) throws CmsException {
        NodeList fileNodes, acentryNodes;
        Element currentElement, currentEntry;
        String source, destination, type, timestamp, uuid, uuidfile, uuidresource;
        long lastmodified = 0;
        int resType;
        Map properties = null;

        if (m_importingChannelData) {
            m_cms.getRequestContext().saveSiteRoot();
            m_cms.setContextToCos();
        }

        if (excludeList == null) {
            excludeList = new Vector();
        }

        m_webAppNames = (List)OpenCms.getRuntimeProperty("compatibility.support.webAppNames");
        if (m_webAppNames == null) {
            m_webAppNames = new ArrayList();
        }

        // get the old webapp url from the OpenCms properties
        m_webappUrl = (String)OpenCms.getRuntimeProperty("compatibility.support.import.old.webappurl");
        if (m_webappUrl == null) {
            // use a default value
            m_webappUrl = "http://localhost:8080/opencms/opencms";
        }
        // cut last "/" from webappUrl if present
        if (m_webappUrl.endsWith("/")) {
            m_webappUrl = m_webappUrl.substring(0, m_webappUrl.lastIndexOf("/"));
        }

        // get list of unwanted properties
        List deleteProperties = (List)OpenCms.getRuntimeProperty("compatibility.support.import.remove.propertytags");
        if (deleteProperties == null) {
            deleteProperties = new ArrayList();
        }

        // get list of immutable resources
        List immutableResources = (List)OpenCms.getRuntimeProperty("import.immutable.resources");
        if (immutableResources == null) {
            immutableResources = new ArrayList();
        }
        if (DEBUG > 0) {
            System.err.println("Import: Immutable resources size is " + immutableResources.size());
        }

        // get the wanted page type for imported pages
        String convertToXmlPage = (String)OpenCms.getRuntimeProperty("import.convert.xmlpage");
        if (convertToXmlPage != null) {
            m_convertToXmlPage = "true".equals(convertToXmlPage);
        }
        
        try {
            // get all file-nodes
            fileNodes = m_docXml.getElementsByTagName(I_CmsConstants.C_EXPORT_TAG_FILE);
            int importSize = fileNodes.getLength();

            // walk through all files in manifest
            for (int i = 0; i < fileNodes.getLength(); i++) {

                m_report.print(" ( " + (i + 1) + " / " + importSize + " ) ", I_CmsReport.C_FORMAT_NOTE);
                currentElement = (Element)fileNodes.item(i);

                // get all information for a file-import
                source = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_SOURCE);
                destination = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_DESTINATION);
                type = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_TYPE);
                if (C_RESOURCE_TYPE_NEWPAGE_NAME.equals(type)) {
                    resType = C_RESOURCE_TYPE_NEWPAGE_ID;
                } else {
                    resType = m_cms.getResourceTypeId(type);
                }
                uuid = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_UUIDSTRUCTURE);
                uuidfile = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_UUIDCONTENT);
                uuidresource = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_UUIDRESOURCE);

                if ((timestamp = getTextNodeValue(currentElement, I_CmsConstants.C_EXPORT_TAG_LASTMODIFIED)) != null) {
                    lastmodified = Long.parseLong(timestamp);
                } else {
                    lastmodified = System.currentTimeMillis();
                }

                // if the type is "script" set it to plain
                if ("script".equals(type)) {
                    type = CmsResourceTypePlain.C_RESOURCE_TYPE_NAME;
                }

                String translatedName = m_cms.getRequestContext().addSiteRoot(m_importPath + destination);
                if (CmsResourceTypeFolder.C_RESOURCE_TYPE_NAME.equals(type)) {
                    translatedName += I_CmsConstants.C_FOLDER_SEPARATOR;
                }
                translatedName = m_cms.getRequestContext().getDirectoryTranslator().translateResource(translatedName);
                if (DEBUG > 3) {
                    System.err.println("Import: Translated resource name is " + translatedName);
                }

                boolean resourceNotImmutable = checkImmutable(translatedName, immutableResources);

                translatedName = m_cms.getRequestContext().removeSiteRoot(translatedName);
                if (resourceNotImmutable && (!excludeList.contains(translatedName))) {

                    // print out the information to the report
                    m_report.print(m_report.key("report.importing"), I_CmsReport.C_FORMAT_NOTE);
                    m_report.print(translatedName);
                    m_report.print(m_report.key("report.dots"));

                    // get all properties
                    properties = getPropertiesFromXml(currentElement, resType, propertyName, propertyValue, deleteProperties);

                    // import the specified file 
                    CmsResource res = importResource(source, destination, uuid, uuidfile, uuidresource, type, lastmodified, properties, writtenFilenames, fileCodes);

                    if (res != null) {

                        // write all imported access control entries for this file
                        acentryNodes = currentElement.getElementsByTagName(I_CmsConstants.C_EXPORT_TAG_ACCESSCONTROL_ENTRY);
                        // collect all access control entries
                        //String resid = getTextNodeValue(currentElement, C_EXPORT_TAG_ID);
                        for (int j = 0; j < acentryNodes.getLength(); j++) {
                            currentEntry = (Element)acentryNodes.item(j);
                            // get the data of the access control entry
                            String id = getTextNodeValue(currentEntry, I_CmsConstants.C_EXPORT_TAG_ID);
                            String flags = getTextNodeValue(currentEntry, I_CmsConstants.C_EXPORT_TAG_FLAGS);
                            String allowed = getTextNodeValue(currentEntry, I_CmsConstants.C_EXPORT_TAG_ACCESSCONTROL_ALLOWEDPERMISSIONS);
                            String denied = getTextNodeValue(currentEntry, I_CmsConstants.C_EXPORT_TAG_ACCESSCONTROL_DENIEDPERMISSIONS);

                            // add the entry to the list
                            addImportAccessControlEntry(res, id, allowed, denied, flags);
                        }
                        importAccessControlEntries(res);

                    } else {
                        // resource import failed, since no CmsResource was created
                        m_report.print(m_report.key("report.skipping"), I_CmsReport.C_FORMAT_OK);
                    }
                } else {
                    // skip the file import, just print out the information to the report
                    m_report.print(m_report.key("report.skipping"), I_CmsReport.C_FORMAT_NOTE);
                    m_report.println(translatedName);
                }
            }

            // now merge the body and page control files. this only has to be done if the import
            // version is below version 3
            if (m_importVersion < 3) {
                // only do the conversions if the new resourcetype (CmsResourceTypeNewPage.) is available 
                CmsResource newpage = null;
                
                try {
                    newpage = m_cms.readFileHeader(I_CmsWpConstants.C_VFS_PATH_WORKPLACE + "restypes/" + CmsResourceTypeXmlPage.C_RESOURCE_TYPE_NAME);
                } catch (CmsException e1) {
                    // do nothing, 
                }
                
                boolean convertToNewPage=((newpage!=null) || !CmsResourceTypeFolder.C_BODY_MIRROR);
                
                if (convertToNewPage) {
                    mergePageFiles();
                    removeFolders();
                }
            }

        } catch (Exception exc) {
            exc.printStackTrace(System.err);
            m_report.println(exc);
            throw new CmsException(CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            if (m_importingChannelData) {
                m_cms.getRequestContext().restoreSiteRoot();
            }
        }
    }

    /**
      * Imports a resource (file or folder) into the cms.<p>
      * 
      * @param source the path to the source-file
      * @param destination the path to the destination-file in the cms
      * @param uuid  the structure uuid of the resource
      * @param uuidfile  the file uuid of the resource
      * @param uuidresource  the resource uuid of the resource
      * @param type the resource-type of the file
      * @param lastmodified the timestamp of the file
      * @param properties a hashtable with properties for this resource
      * @param writtenFilenames filenames of the files and folder which have actually been successfully written
      *       not used when null
      * @param fileCodes code of the written files (for the registry)
      *       not used when null
      * @return imported resource
      */
     private CmsResource importResource(String source, String destination, String uuid, String uuidfile, String uuidresource, String type, long lastmodified, Map properties, Vector writtenFilenames, Vector fileCodes) {

         boolean success = true;
         byte[] content = null;
         String fullname = null;
         CmsResource res = null;

         try {
          
             if (m_importingChannelData) {
                 // try to read an existing channel to get the channel id
                 String channelId = null;
                 try {
                     if ((type.equalsIgnoreCase(CmsResourceTypeFolder.C_RESOURCE_TYPE_NAME)) && (!destination.endsWith(I_CmsConstants.C_FOLDER_SEPARATOR))) {
                         destination += I_CmsConstants.C_FOLDER_SEPARATOR;
                     }
                     CmsResource channel = m_cms.readFileHeader(I_CmsConstants.C_ROOT + destination);
                     channelId = m_cms.readProperty(m_cms.readAbsolutePath(channel), I_CmsConstants.C_PROPERTY_CHANNELID);
                 } catch (Exception e) {
                     // ignore the exception, a new channel id will be generated
                 }
                 if (channelId == null) {
                     // the channel id does not exist, so generate a new one
                     int newChannelId = org.opencms.db.CmsDbUtil.nextId(I_CmsConstants.C_TABLE_CHANNELID);
                     channelId = "" + newChannelId;
                 }
                 properties.put(I_CmsConstants.C_PROPERTY_CHANNELID, channelId);
             }

             // get the file content
             if (source != null) {
                 content = getFileBytes(source);
             }
             
//             // check and convert old import files    
//             if (m_importVersion < 2) {
//
//                 // convert content from pre 5.x must be activated
//                 if ("page".equals(type) || ("plain".equals(type)) || ("XMLTemplate".equals(type))) {
//                     if (DEBUG > 0) {
//                         System.err.println("#########################");
//                         System.err.println("[" + this.getClass().getName() + ".importResource()]: starting conversion of \"" + type + "\" resource " + source + ".");
//                     }
//                     // change the filecontent for encoding if necessary
//                     content = convertFile(source, content);
//                 }
//                 // only check the file type if the version of the export is 0
//                 if (m_importVersion == 0) {
//                     // ok, a (very) old system exported this, check if the file is ok
//                     if (!(new CmsCompatibleCheck()).isTemplateCompatible(m_importPath + destination, content, type)) {
//                         type = CmsResourceTypeCompatiblePlain.C_RESOURCE_TYPE_NAME;
//                         m_report.print(m_report.key("report.must_set_to") + type + " ", I_CmsReport.C_FORMAT_WARNING);
//                     }
//                 }
//             }

//             // if the import is older than version 3, some additional conversions must be made
//             if (m_importVersion < 3) {
//                 if ("page".equals(type)) {
//
//                     // if the imported resource is a page, store its path inside the VFS for later
//                     // integration with its body
//                     m_pageStorage.add(destination);
//                 } else if ("folder".equals(type)) {
//                     // check if the imported resource is a folder. Folders created in the /system/bodies/ folder
//                     // must be remove since we do not use body files anymore.
//                     if (destination.startsWith(I_CmsWpConstants.C_VFS_PATH_BODIES.substring(1))) {
//                         m_folderStorage.add(destination);
//                     }
//                 }
//
//             } 
             
             content = convertContent(source, destination, content, type);
             
             // get all required information to create a CmsResource
             int resType=m_cms.getResourceTypeId(type);
             int size=0;
             if (content!=null) {
                 size=content.length;
             }
             // get the required UUIDs         
             CmsUUID curUser=m_cms.getRequestContext().currentUser().getId();            
             CmsUUID newUuidstructure= new CmsUUID();
             CmsUUID newUuidcontent = new CmsUUID();
             CmsUUID newUuidresource = new CmsUUID();             
             if (uuid!=null) {
                 newUuidstructure = new CmsUUID(uuid);       
             }
             if (uuidfile!=null) {
                 newUuidcontent = new CmsUUID(uuidfile);
             }
             if (uuidresource!=null) { 
                 newUuidresource = new CmsUUID(uuidresource);               
             }             
                    
             // extract the name of the resource form the destination
             String resname=destination;
             if (resname.endsWith("/")) {
                 resname=resname.substring(0, resname.length()-1);            
             } 
             if (resname.lastIndexOf("/")>0) {
                 resname=resname.substring(resname.lastIndexOf("/")+1, resname.length());
             }
                         
             // create a new CmsResource                         
             CmsResource resource=new CmsResource(newUuidstructure, newUuidresource,
                                                  CmsUUID.getNullUUID(),
                                                  newUuidcontent, resname, resType,
                                                  new Integer(0).intValue(), m_cms.getRequestContext().currentProject().getId(),
                                                  I_CmsConstants.C_STATE_NEW, m_cms.getResourceType(resType).getLoaderId(), lastmodified,
                                                  curUser,
                                                  lastmodified, curUser, size,
                                                  1);
            
             if (resType==CmsResourceTypeLink.C_RESOURCE_TYPE_ID) {
                 // store links for later conversion
                 m_report.print(m_report.key("report.storing_link"), I_CmsReport.C_FORMAT_NOTE);
                 m_linkStorage.put(m_importPath + destination, new String(content));
                 m_linkPropertyStorage.put(m_importPath + destination, properties);
                 res = resource;
             } else {                                                     
                                                  
                 //  import this resource in the VFS     
                     
                 res = m_cms.importResource(resource, content, properties, m_importPath+destination);
             }   
         
             if (res != null) {
                 if (CmsResourceTypePage.C_RESOURCE_TYPE_NAME.equals(type)) {
                     m_importedPages.add(I_CmsConstants.C_FOLDER_SEPARATOR + destination);
                 }
                 m_report.println(m_report.key("report.ok"), I_CmsReport.C_FORMAT_OK);
             }
            
         } catch (Exception exc) {
             // an error while importing the file
             success = false;
             m_report.println(exc);
             try {
                 // Sleep some time after an error so that the report output has a chance to keep up
                 Thread.sleep(1000);
             } catch (Exception e) {
                 // 
             }
         }

         byte[] digestContent = {0};
         if (content != null) {
             digestContent = m_digest.digest(content);
         }
         if (success && (fullname != null)) {
             if (writtenFilenames != null) {
                 writtenFilenames.addElement(fullname);
             }
             if (fileCodes != null) {
                 fileCodes.addElement(new String(digestContent));
             }
         }
         return res;
     }
     
     /**
      * Performs all required pre-import steps.<p>
      * 
      * The content is *NOT* changed in the implementation of this class.<p>
      * 
      * @param source the source path of the resource
      * @param destination the destination path of the resource
      * @param content the content of the resource
      * @param resType the type of the resource
      * @return the (prepared) content of the resource
      */
     protected byte[] convertContent(String source, String destination, byte[] content, String resType) {
        // if the import is older than version 3, some additional conversions must be made
        if (m_importVersion < 3) {
            if ("page".equals(resType)) {
                if (DEBUG > 0) {
                    System.err.println("#########################");
                    System.err.println("[" + this.getClass().getName() + ".convertContent()]: storing resource " + source + ".");
                }                
                // if the imported resource is a page, store its path inside the VFS for later
                // integration with its body
                m_pageStorage.add(destination);
            } else if ("folder".equals(resType)) {
                // check if the imported resource is a folder. Folders created in the /system/bodies/ folder
                if (destination.startsWith(I_CmsWpConstants.C_VFS_PATH_BODIES.substring(1))) {
                    // must be remove since we do not use body files anymore.
                    m_folderStorage.add(destination);
                }
            }
        }
        
        return content;
    }

    /**
     * Merges the page control files and their corresponding bodies into a single files.<p>
     * 
     * 
     * @throws CmsException if something goes wrong
     */
    private void mergePageFiles() throws CmsException {

        // check if the template property exists. If not, create it.
        try {
            m_cms.readPropertydefinition(I_CmsConstants.C_PROPERTY_TEMPLATE, CmsResourceTypeXmlPage.C_RESOURCE_TYPE_ID);
        } catch (CmsException e) {
            // the template propertydefintion does not exist. So create it.
            m_cms.createPropertydefinition(I_CmsConstants.C_PROPERTY_TEMPLATE, CmsResourceTypeXmlPage.C_RESOURCE_TYPE_ID);
        }
        // copy all propertydefinitions of the old page to the new page
        Vector definitions = m_cms.readAllPropertydefinitions(CmsResourceTypePage.C_RESOURCE_TYPE_ID);

        Iterator j = definitions.iterator();


        while (j.hasNext()) {
            CmsPropertydefinition definition = (CmsPropertydefinition)j.next();
            // check if this propertydef already exits
            try {
                m_cms.readPropertydefinition(definition.getName(), CmsResourceTypeXmlPage.C_RESOURCE_TYPE_ID);
            } catch (Exception e) {
                m_cms.createPropertydefinition(definition.getName(), CmsResourceTypeXmlPage.C_RESOURCE_TYPE_ID);
            }
        }

        // iterate through the list of all page controlfiles found during the import process
        int size = m_pageStorage.size();

        m_report.println(m_report.key("report.merge_start"), I_CmsReport.C_FORMAT_HEADLINE);

        Iterator i = m_pageStorage.iterator();
        int counter = 1;
        while (i.hasNext()) {
            String resname = (String)i.next();
            // adjust the resourcename if nescessary
            if (!resname.startsWith("/")) {
                resname = "/" + resname;
            }

            m_report.print("( " + counter + " / " + size + " ) ", I_CmsReport.C_FORMAT_NOTE);
            m_report.print(m_report.key("report.merge") + " " , I_CmsReport.C_FORMAT_NOTE);
            m_report.print(resname, I_CmsReport.C_FORMAT_DEFAULT);

            try {
                
                mergePageFile(resname);
                
            } catch (Exception e) {
                m_report.println(e);
                // throw new CmsException(e.toString());
            }

            counter++;

        }
        // free mem
        m_pageStorage.clear();

    }

    /**
     * Merges a single page.<p>
     * 
     * @param resname the resource name of the page
     * @throws Exception if something goes wrong
     */
    private void mergePageFile(String resname) throws Exception {

        String mastertemplate = "";
        String bodyname = "";
        String bodyclass = "";
        HashMap bodyparams = null;
        
        // get the header file
        CmsFile pagefile = m_cms.readFile(resname);
        // now parse the content of the headerfile to identify the master template used by this
        // page
        InputStream in = new ByteArrayInputStream(pagefile.getContents());
        Document contentXml;
        CmsFile bodyfile;
        
        // create DOM document
        contentXml = A_CmsXmlContent.getXmlParser().parse(in);
        // get the <masterTemplate> node to check the content.
        // this node contains the name of the template file
        NodeList masterTemplateNode = contentXml.getElementsByTagName("masterTemplate");
        // there is only one <masterTemplate> allowed
        if (masterTemplateNode.getLength() == 1) {
            // get the name of the mastertemplate
            mastertemplate = masterTemplateNode.item(0).getFirstChild().getNodeValue();
        }
        
        // get the <ELEMENTDEF> nodes to check the content.
        // this node contains the information for the body element.
        NodeList bodyNode = contentXml.getElementsByTagName("ELEMENTDEF");
        
        // there is only one <ELEMENTDEF> allowed
        if (bodyNode.getLength() == 1) {
            
            // get the elementdef
            Node bodyElement = bodyNode.item(0);
            NodeList nodes = bodyElement.getChildNodes();
            int i;
            
            // get the class of the body template
            for (i = 0; i < nodes.getLength(); i++) {
                if ("CLASS".equals(nodes.item(i).getNodeName())) {
                    bodyclass = nodes.item(i).getFirstChild().getNodeValue();
                    break;
                }
            }
            
            // get the name of the body template
            for (i = 0; i < nodes.getLength(); i++) {
                if ("TEMPLATE".equals(nodes.item(i).getNodeName())) {
                    bodyname = nodes.item(i).getFirstChild().getNodeValue();
                    if (!bodyname.startsWith("/")) {
                        bodyname = CmsResource.getFolderPath(resname) + bodyname;
                    }
                    break;
                }
            }
            
            // get body template parameters if defined
            for (i = 0; i < nodes.getLength(); i++) {
                if ("PARAMETER".equals(nodes.item(i).getNodeName())) {
                    Node paramElement = nodes.item(i);
                    if (bodyparams == null) {
                        bodyparams = new HashMap();
                    }
                    bodyparams.put(paramElement.getAttributes().getNamedItem("name").getNodeValue(), paramElement.getFirstChild().getNodeValue());
                }
            }
            
            // lock the resource, so that it can be manipulated
            m_cms.lockResource(resname);             
            // get all properties                   
            Map properties = m_cms.readProperties(resname);
            // now get the content of the bodyfile and insert it into the control file                   
            bodyfile = m_cms.readFile(bodyname);
            
            if (m_convertToXmlPage) {
                CmsXmlPage xmlPage = CmsXmlPageConverter.convertToXmlPage(m_cms, new String(bodyfile.getContents()), "body", getLocale(resname, properties)); 
                
                if (xmlPage != null) {
                    xmlPage.write(pagefile);
                    
                    // set the type to xml page
                    pagefile.setType(CmsResourceTypeXmlPage.C_RESOURCE_TYPE_ID);
                    pagefile.setLoaderId(CmsXmlPageLoader.C_RESOURCE_LOADER_ID);
                }
            }
 
            // write all changes                     
            m_cms.writeFile(pagefile);
            // add the template property to the controlfile
            m_cms.writeProperty(resname, I_CmsConstants.C_PROPERTY_TEMPLATE, mastertemplate, true);
            // if set, add the bodyclass as property
            if (bodyclass != null && !"".equals(bodyclass)) {
                m_cms.writeProperty(resname, I_CmsConstants.C_PROPERTY_BODY_CLASS, bodyclass, true);
            }
            // if set, add bodyparams as properties
            if (bodyparams != null) {
                for (Iterator p = bodyparams.keySet().iterator(); p.hasNext();) {
                    String key = (String)p.next();
                    m_cms.writeProperty(resname, key, (String)bodyparams.get(key), true);
                }
            }
            m_cms.writeProperties(resname, properties, true);
            m_cms.touch(resname, pagefile.getDateLastModified(), false, pagefile.getUserLastModified());
            // done, ulock the resource                   
            m_cms.unlockResource(resname, false);
            // finally delete the old body file, it is not needed anymore
            m_cms.lockResource(bodyname);
            m_cms.deleteResource(bodyname, I_CmsConstants.C_DELETE_OPTION_IGNORE_VFS_LINKS);
            m_report.println(" " + m_report.key("report.ok"), I_CmsReport.C_FORMAT_OK); 
            
        } else {
            
            // there are more than one template nodes in this control file
            // convert the resource into a plain text file
            // lock the resource, so that it can be manipulated
            m_cms.lockResource(resname);
            // set the type to plain
            pagefile.setType(CmsResourceTypePlain.C_RESOURCE_TYPE_ID);
            // write all changes                     
            m_cms.writeFile(pagefile);
            // don, ulock the resource                   
            m_cms.unlockResource(resname, false);
            m_report.println(" " + m_report.key("report.notconverted"), I_CmsReport.C_FORMAT_OK);
        }
    }
    
    /**
     * Deletes the folder structure which has been creating while importing the body files..<p>
     *
     *      * @throws CmsException if something goes wrong
     */
    private void removeFolders() throws CmsException {
        int size = m_folderStorage.size();

        m_report.println(m_report.key("report.delfolder_start"), I_CmsReport.C_FORMAT_HEADLINE);
        // iterate though all collected folders. Iteration must start at the end of the list,
        // as folders habe to be deleted in the reverse order.
        ListIterator i = m_folderStorage.listIterator(size);
        int counter = 1;
        while (i.hasPrevious()) {
            String resname = (String)i.previous();
            resname = "/" + resname + "/";
            // now check if the folder is really empty. Only delete empty folders
            List files = m_cms.getFilesInFolder(resname, false);

            if (files.size() == 0) {
                List folders = m_cms.getSubFolders(resname, false);
                if (folders.size() == 0) {
                    m_report.print("( " + counter + " / " + size + " ) ",  I_CmsReport.C_FORMAT_NOTE);
                    m_report.print(m_report.key("report.delfolder") + " " , I_CmsReport.C_FORMAT_NOTE);
                    m_report.print(resname, I_CmsReport.C_FORMAT_DEFAULT);
                    m_cms.lockResource(resname);
                    m_cms.deleteResource(resname, I_CmsConstants.C_DELETE_OPTION_IGNORE_VFS_LINKS);
                    m_report.println(m_report.key("report.ok"), I_CmsReport.C_FORMAT_OK);
                    counter++;
                }
            }
        }
    }

    /**
     * Gets the encoding from the &lt;?XML ...&gt; tag if present.<p>
     * 
     * @param content the file content
     * @return String the found encoding
     */
    protected String getEncoding(String content) {
        String encoding = content;
        int index = encoding.toLowerCase().indexOf("encoding=\"");
        // encoding attribute found, get the value
        if (index != -1) {
            encoding = encoding.substring(index + 10);
            if ((index = encoding.indexOf("\"")) != -1) {
                encoding = encoding.substring(0, index);
                return encoding.toUpperCase();
            }
        }
        // no encoding attribute found
        return "";
    }

    /** 
     * Sets the right encoding and returns the result.<p>
     * 
     * @param content the filecontent
     * @param encoding the encoding to use
     * @return modified content
     */
    protected String setEncoding(String content, String encoding) {
        if (content.toLowerCase().indexOf("<?xml") == -1) {
            return content;
        } else {
            // XML information present, replace encoding
            // set the encoding only if it does not exist
            String xmlTag = content.substring(0, content.indexOf(">") + 1);
            if (xmlTag.toLowerCase().indexOf("encoding") == -1) {
                content = content.substring(content.indexOf(">") + 1);
                content = "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>" + content;
            }
        }
        return content;
    }


    /** 
     * Translates directory Strings from OpenCms 4.x structure to new 5.0 structure.<p>
     * 
     * @param content the filecontent
     * @param rules the translation rules
     * @return String the manipulated file content
     */
    public static String setDirectories(String content, String[] rules) {
        // get translation rules
        for (int i = 0; i < rules.length; i++) {
            String actRule = rules[i];
            // cut String "/default/vfs/" from rule
            actRule = CmsStringSubstitution.substitute(actRule, "/default/vfs", "");
            // divide rule into search and replace parts and delete regular expressions
            StringTokenizer ruleT = new StringTokenizer(actRule, "#");
            ruleT.nextToken();
            String search = ruleT.nextToken();
            search = search.substring(0, search.lastIndexOf("(.*)"));
            String replace = ruleT.nextToken();
            replace = replace.substring(0, replace.lastIndexOf("$1"));
            // scan content for paths if the replace String is not present
            if (content.indexOf(replace) == -1 && content.indexOf(search) != -1) {
                // ensure subdirectories of the same name are not replaced
                search = "([}>\"'\\[]\\s*)" + search;
                replace = "$1" + replace;
                content = CmsStringSubstitution.substitutePerl(content, search, replace, "g");
            }
        }
        return content;
    }
     

    }
