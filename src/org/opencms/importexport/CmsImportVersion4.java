/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/importexport/CmsImportVersion4.java,v $
 * Date   : $Date: 2004/06/28 07:47:33 $
 * Version: $Revision: 1.47 $
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

import org.opencms.file.CmsObject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsResource;
import org.opencms.file.types.CmsResourceTypeFolder;
import org.opencms.file.types.CmsResourceTypeXmlPage;
import org.opencms.file.types.I_CmsResourceType;
import org.opencms.main.CmsException;
import org.opencms.main.I_CmsConstants;
import org.opencms.main.OpenCms;
import org.opencms.report.I_CmsReport;
import org.opencms.security.I_CmsPrincipal;
import org.opencms.util.CmsDateUtil;
import org.opencms.util.CmsUUID;
import org.opencms.xml.page.CmsXmlPage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.zip.ZipFile;

import org.dom4j.Document;
import org.dom4j.Element;

/**
 * Implementation of the OpenCms Import Interface ({@link org.opencms.importexport.I_CmsImport}) for 
 * the import version 4.<p>
 * 
 * This import format is used in OpenCms since 5.1.6.
 * @see org.opencms.importexport.A_CmsImport
 *
 * @author Michael Emmerich (m.emmerich@alkacon.com)
 * @author Thomas Weckert (t.weckert@alkacon.com)
 */
public class CmsImportVersion4 extends A_CmsImport {
    
    /** The version number of this import implementation.<p> */
    private static final int C_IMPORT_VERSION = 4;
    
    /**
     * Creates a new CmsImportVerion4 object.<p>
     */
    public CmsImportVersion4() {        
        m_convertToXmlPage = true;
    }
    
    /**
     * @see org.opencms.importexport.I_CmsImport#getVersion()
     * @return the version number of this import implementation
     */
    public int getVersion() {
        return CmsImportVersion4.C_IMPORT_VERSION;
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
    public synchronized void importResources(CmsObject cms, String importPath, I_CmsReport report, MessageDigest digest, File importResource, ZipFile importZip, Document docXml, Vector excludeList, Vector writtenFilenames, Vector fileCodes, String propertyName, String propertyValue) throws CmsException {
        // initialize the import       
        initialize();
        m_cms = cms;
        m_importPath = importPath;
        m_report = report;
        m_digest = digest;
        m_importResource = importResource;
        m_importZip = importZip;
        m_docXml = docXml;
        m_importingChannelData = false;
        m_linkStorage = new HashMap();
        m_linkPropertyStorage = new HashMap();
        
        // these lines make Eclipse happy...
        if (writtenFilenames != null) {
            writtenFilenames.size();
        }
        if (fileCodes != null) {
            fileCodes.size();
        }
        
        try {
            // first import the user information
            if (m_cms.isAdmin()) {
                importGroups();
                importUsers();
            }
            // now import the VFS resources
            readResourcesFromManifest(excludeList, propertyName, propertyValue);
            convertPointerToSiblings();
        } catch (CmsException e) {
            throw e;
        } finally {
            cleanUp();
        }
    }

    /**
     * Imports the groups and writes them to the cms.<p>
     * 
     * @throws CmsException if something goes wrong
     */
    private void importGroups() throws CmsException {
        List groupNodes;
        Element currentElement;
        String name, description, flags, parentgroup;
        try {
            // getAll group nodes
            groupNodes = m_docXml.selectNodes("//" + I_CmsConstants.C_EXPORT_TAG_GROUPDATA);
            // walk through all groups in manifest
            for (int i = 0; i < groupNodes.size(); i++) {
                currentElement = (Element)groupNodes.get(i);
                name = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_NAME);
                name = OpenCms.getImportExportManager().translateGroup(name);  
                description = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_DESCRIPTION);
                flags = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_FLAGS);
                parentgroup = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_PARENTGROUP);
                if ((parentgroup!=null) && (parentgroup.length()>0)) {
                    parentgroup = OpenCms.getImportExportManager().translateGroup(parentgroup);
                }
                // import this group
             
                importGroup(null, name, description, flags, parentgroup);
            }

            // now try to import the groups in the stack
            while (!m_groupsToCreate.empty()) {
                Stack tempStack = m_groupsToCreate;
                m_groupsToCreate = new Stack();
                while (tempStack.size() > 0) {
                    Hashtable groupdata = (Hashtable)tempStack.pop();
                    name = (String)groupdata.get(I_CmsConstants.C_EXPORT_TAG_NAME);
                    description = (String)groupdata.get(I_CmsConstants.C_EXPORT_TAG_DESCRIPTION);
                    flags = (String)groupdata.get(I_CmsConstants.C_EXPORT_TAG_FLAGS);
                    parentgroup = (String)groupdata.get(I_CmsConstants.C_EXPORT_TAG_PARENTGROUP);
                    // try to import the group
                    importGroup(null, name, description, flags, parentgroup);
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
      List userNodes;
      List groupNodes;
      Element currentElement, currentGroup;
      Vector userGroups;
      Hashtable userInfo = new Hashtable();
      sun.misc.BASE64Decoder dec;
      String  name, description, flags, password, recoveryPassword, firstname, lastname, email, address, section, defaultGroup, type, pwd, infoNode;
      // try to get the import resource
      //getImportResource();
      try {
          // getAll user nodes
          userNodes = m_docXml.selectNodes("//" + I_CmsConstants.C_EXPORT_TAG_USERDATA);
          // walk threw all groups in manifest
          for (int i = 0; i < userNodes.size(); i++) {
              currentElement = (Element)userNodes.get(i);
              name = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_NAME);
              name = OpenCms.getImportExportManager().translateUser(name);              
              // decode passwords using base 64 decoder
              dec = new sun.misc.BASE64Decoder();
              pwd = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_PASSWORD);
              password = new String(dec.decodeBuffer(pwd.trim()));
              dec = new sun.misc.BASE64Decoder();
              pwd = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_RECOVERYPASSWORD);
              recoveryPassword = new String(dec.decodeBuffer(pwd.trim()));

              description = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_DESCRIPTION);
              flags = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_FLAGS);
              firstname = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_FIRSTNAME);
              lastname = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_LASTNAME);
              email = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_EMAIL);
              address = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_ADDRESS);
              section = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_SECTION);
              defaultGroup = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_DEFAULTGROUP);
              type = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_TYPE);
              // get the userinfo and put it into the hashtable
              infoNode = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_USERINFO);
              try {
                  // read the userinfo from the dat-file
                  byte[] value = getFileBytes(infoNode);
                  // deserialize the object
                  ByteArrayInputStream bin = new ByteArrayInputStream(value);
                  ObjectInputStream oin = new ObjectInputStream(bin);
                  userInfo = (Hashtable)oin.readObject();
              } catch (IOException ioex) {
                  m_report.println(ioex);
              }

              // get the groups of the user and put them into the vector
              groupNodes = currentElement.selectNodes("*/" + I_CmsConstants.C_EXPORT_TAG_GROUPNAME);
              userGroups = new Vector();
              for (int j = 0; j < groupNodes.size(); j++) {
                  currentGroup = (Element)groupNodes.get(j);
                  String userInGroup=CmsImport.getChildElementTextValue(currentGroup, I_CmsConstants.C_EXPORT_TAG_NAME);
                  userInGroup = OpenCms.getImportExportManager().translateGroup(userInGroup);  
                  userGroups.addElement(userInGroup);
              }
              // import this user
              importUser(null, name, description, flags, password, recoveryPassword, firstname, lastname, email, address, section, defaultGroup, type, userInfo, userGroups);
          }
      } catch (Exception exc) {
          m_report.println(exc);
          throw new CmsException(CmsException.C_UNKNOWN_EXCEPTION, exc);
      }
  }
  
    /**
     * Reads all file nodes plus their meta-information (properties, ACL) 
     * from manifest.xml and imports them as Cms resources to the VFS.<p>
     * 
     * @param ignoredResources a list of resource names which should not be (over)written in the VFS, or null
     * @param propertyKey name of a property to be added to all resources, or null
     * @param propertyValue value of the property to be added to all resources, or null
     * @throws CmsException if something goes wrong
     */
    private void readResourcesFromManifest(Vector ignoredResources, String propertyKey, String propertyValue)
    throws CmsException {

        String source = null, destination = null, type = null, uuidresource = null, uuidcontent = null, userlastmodified = null, usercreated = null, flags = null, timestamp = null;
        long datelastmodified = 0, datecreated = 0, datereleased = 0, dateexpired = 0;
        int resType = I_CmsConstants.C_UNKNOWN_ID;
        int loaderId = I_CmsConstants.C_UNKNOWN_ID;

        List fileNodes = null, acentryNodes = null;
        Element currentElement = null, currentEntry = null;
        List properties = null;

        if (m_importingChannelData) {
            m_cms.getRequestContext().saveSiteRoot();
            m_cms.getRequestContext().setSiteRoot(I_CmsConstants.VFS_FOLDER_COS);
        }

        // clear some required structures at the init phase of the import      
        if (ignoredResources == null) {
            ignoredResources = new Vector();
        }

        // get list of ignored properties
        List ignoredProperties = OpenCms.getImportExportManager().getIgnoredProperties();
        if (ignoredProperties == null) {
            ignoredProperties = Collections.EMPTY_LIST;
        }

        // get list of immutable resources
        List immutableResources = OpenCms.getImportExportManager().getImmutableResources();
        if (immutableResources == null) {
            immutableResources = Collections.EMPTY_LIST;
        }

        // get the desired page type for imported pages
        m_convertToXmlPage = OpenCms.getImportExportManager().convertToXmlPage();

        try {
            // get all file-nodes
            fileNodes = m_docXml.selectNodes("//" + I_CmsConstants.C_EXPORT_TAG_FILE);
            int importSize = fileNodes.size();

            // walk through all files in manifest
            for (int i = 0; i < fileNodes.size(); i++) {
                m_report.print(" ( " + (i + 1) + " / " + importSize + " ) ", I_CmsReport.C_FORMAT_NOTE);
                currentElement = (Element)fileNodes.get(i);

                // <source>
                source = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_SOURCE);
                // <destination>

                destination = CmsImport.getChildElementTextValue(
                    currentElement,
                    I_CmsConstants.C_EXPORT_TAG_DESTINATION);

                // <type>
                type = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_TYPE);
                if (C_RESOURCE_TYPE_NEWPAGE_NAME.equals(type)) {
                    resType = C_RESOURCE_TYPE_NEWPAGE_ID;
                } else if (C_RESOURCE_TYPE_PAGE_NAME.equals(type)) {
                    resType = C_RESOURCE_TYPE_PAGE_ID;
                } else if (C_RESOURCE_TYPE_LINK_NAME.equals(type)) {
                    resType = C_RESOURCE_TYPE_LINK_ID;
                } else {
                    I_CmsResourceType rt = OpenCms.getResourceManager().getResourceType(type);
                    resType = rt.getTypeId();
                    loaderId = rt.getLoaderId();
                }

                if (resType != CmsResourceTypeFolder.C_RESOURCE_TYPE_ID) {
                    // <uuidresource>
                    uuidresource = CmsImport.getChildElementTextValue(
                        currentElement,
                        I_CmsConstants.C_EXPORT_TAG_UUIDRESOURCE);
                    // <uuidcontent>
                    uuidcontent = CmsImport.getChildElementTextValue(
                        currentElement,
                        I_CmsConstants.C_EXPORT_TAG_UUIDCONTENT);
                } else {
                    uuidresource = null;
                    uuidcontent = null;
                }

                // <datelastmodified>
                if ((timestamp = CmsImport.getChildElementTextValue(
                    currentElement,
                    I_CmsConstants.C_EXPORT_TAG_DATELASTMODIFIED)) != null) {
                    datelastmodified = convertTimestamp(timestamp);
                } else {
                    datelastmodified = System.currentTimeMillis();
                }

                // <userlastmodified>
                userlastmodified = CmsImport.getChildElementTextValue(
                    currentElement,
                    I_CmsConstants.C_EXPORT_TAG_USERLASTMODIFIED);
                userlastmodified = OpenCms.getImportExportManager().translateUser(userlastmodified);

                // <datecreated>
                if ((timestamp = CmsImport.getChildElementTextValue(
                    currentElement,
                    I_CmsConstants.C_EXPORT_TAG_DATECREATED)) != null) {
                    datecreated = convertTimestamp(timestamp);
                } else {
                    datecreated = System.currentTimeMillis();
                }

                // <usercreated>
                usercreated = CmsImport.getChildElementTextValue(
                    currentElement,
                    I_CmsConstants.C_EXPORT_TAG_USERCREATED);
                usercreated = OpenCms.getImportExportManager().translateUser(usercreated);

                // <datereleased>
                if ((timestamp = CmsImport.getChildElementTextValue(
                    currentElement,
                    I_CmsConstants.C_EXPORT_TAG_DATERELEASED)) != null) {
                    datereleased = convertTimestamp(timestamp);
                } else {
                    datereleased = CmsResource.DATE_RELEASED_DEFAULT;
                }
                
                // <dateexpired>
                if ((timestamp = CmsImport.getChildElementTextValue(
                    currentElement,
                    I_CmsConstants.C_EXPORT_TAG_DATEEXPIRED)) != null) {
                    dateexpired = convertTimestamp(timestamp);
                } else {
                    dateexpired = CmsResource.DATE_EXPIRED_DEFAULT;
                }
                
                // <flags>              
                flags = CmsImport.getChildElementTextValue(currentElement, I_CmsConstants.C_EXPORT_TAG_FLAGS);

                String translatedName = m_cms.getRequestContext().addSiteRoot(m_importPath + destination);
                if (CmsResourceTypeFolder.C_RESOURCE_TYPE_NAME.equals(type)) {
                    translatedName += I_CmsConstants.C_FOLDER_SEPARATOR;
                }

                // translate the name during import
                translatedName = m_cms.getRequestContext().getDirectoryTranslator().translateResource(translatedName);

                // check if this resource is immutable
                boolean resourceNotImmutable = checkImmutable(translatedName, immutableResources);
                translatedName = m_cms.getRequestContext().removeSiteRoot(translatedName);

                // if the resource is not immutable and not on the exclude list, import it
                if (resourceNotImmutable && (!ignoredResources.contains(translatedName))) {
                    // print out the information to the report
                    m_report.print(m_report.key("report.importing"), I_CmsReport.C_FORMAT_NOTE);
                    m_report.print(translatedName);
                    m_report.print(m_report.key("report.dots"));
                    // get all properties
                    properties = readPropertiesFromManifest(
                        currentElement,
                        propertyKey,
                        propertyValue,
                        ignoredProperties);

                    // import the resource               
                    CmsResource res = importResource(
                        source,
                        destination,
                        resType,
                        loaderId,
                        uuidresource,
                        uuidcontent,
                        datelastmodified,
                        userlastmodified,
                        datecreated,
                        usercreated,
                        datereleased,
                        dateexpired,
                        flags, 
                        properties);

                    // if the resource was imported add the access control entrys if available
                    if (res != null) {
                        // write all imported access control entries for this file
                        acentryNodes = currentElement.selectNodes("*/"
                            + I_CmsConstants.C_EXPORT_TAG_ACCESSCONTROL_ENTRY);

                        // collect all access control entries
                        for (int j = 0; j < acentryNodes.size(); j++) {
                            currentEntry = (Element)acentryNodes.get(j);

                            // get the data of the access control entry
                            String id = CmsImport.getChildElementTextValue(
                                currentEntry,
                                I_CmsConstants.C_EXPORT_TAG_ACCESSCONTROL_PRINCIPAL);
                            String principalId = new CmsUUID().toString();
                            String principal = id.substring(id.indexOf(".") + 1, id.length());

                            try {
                                if (id.startsWith(I_CmsPrincipal.C_PRINCIPAL_GROUP)) {
                                    principal = OpenCms.getImportExportManager().translateGroup(principal);
                                    principalId = m_cms.readGroup(principal).getId().toString();
                                } else {
                                    principal = OpenCms.getImportExportManager().translateUser(principal);
                                    principalId = m_cms.readUser(principal).getId().toString();
                                }

                                String acflags = CmsImport.getChildElementTextValue(
                                    currentEntry,
                                    I_CmsConstants.C_EXPORT_TAG_FLAGS);

                                String allowed = ((Element)currentEntry.selectNodes(
                                    "./"
                                        + I_CmsConstants.C_EXPORT_TAG_ACCESSCONTROL_PERMISSIONSET
                                        + "/"
                                        + I_CmsConstants.C_EXPORT_TAG_ACCESSCONTROL_ALLOWEDPERMISSIONS).get(0))
                                    .getTextTrim();

                                String denied = ((Element)currentEntry.selectNodes(
                                    "./"
                                        + I_CmsConstants.C_EXPORT_TAG_ACCESSCONTROL_PERMISSIONSET
                                        + "/"
                                        + I_CmsConstants.C_EXPORT_TAG_ACCESSCONTROL_DENIEDPERMISSIONS).get(0))
                                    .getTextTrim();

                                // add the entry to the list
                                addImportAccessControlEntry(res, principalId, allowed, denied, acflags);
                            } catch (CmsException e) {
                                // user or group of ACE might not exist in target system, ignore ACE
                                OpenCms.getLog(this).warn("Could not import ACE for resource " + translatedName, e);
                                m_report.println(e);
                            }
                        }
                        
                        importAccessControlEntries(res);
                    } else {
                        // resource import failed, since no CmsResource was created
                        m_report.print(m_report.key("report.skipping"), I_CmsReport.C_FORMAT_NOTE);
                        m_report.println(translatedName);
                    }

                } else {
                    // skip the file import, just print out the information to the report
                    m_report.print(m_report.key("report.skipping"), I_CmsReport.C_FORMAT_NOTE);
                    m_report.println(translatedName);
                }
            }
        } catch (Exception exc) {
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
     * @param resType the resource-type of the file
     * @param loaderId the loader id of the resource
     * @param uuidresource  the resource uuid of the resource
     * @param uuidcontent the file uuid of the resource
     * @param datelastmodified the last modification date of the resource
     * @param userlastmodified the user who made the last modifications to the resource
     * @param datecreated the creation date of the resource
     * @param usercreated the user who created 
     * @param datereleased the release date of the resource
     * @param dateexpired the expire date of the resource
     * @param flags the flags of the resource     
     * @param properties a hashtable with properties for this resource
     * @return imported resource
     */
    private CmsResource importResource(
        String source, 
        String destination,         
        int resType, 
        int loaderId, 
        String uuidresource, 
        String uuidcontent, 
        long datelastmodified, 
        String userlastmodified, 
        long datecreated, 
        String usercreated, 
        long datereleased, 
        long dateexpired, 
        String flags, 
        List properties) {

        byte[] content = null;
        CmsResource res = null;

        try {
            // get the file content
            if (source != null) {
                content = getFileBytes(source);
            }
            int size = 0;
            if (content != null) {
                size = content.length;
            }

            // get UUIDs for the user   
            CmsUUID newUserlastmodified;
            CmsUUID newUsercreated;
            // check if user created and user lastmodified are valid users in this system.
            // if not, use the current user
            try {
                newUserlastmodified = m_cms.readUser(userlastmodified).getId();
            } catch (CmsException e) {
                newUserlastmodified = m_cms.getRequestContext().currentUser().getId();
                // datelastmodified = System.currentTimeMillis();
            }

            try {
                newUsercreated = m_cms.readUser(usercreated).getId();
            } catch (CmsException e) {
                newUsercreated = m_cms.getRequestContext().currentUser().getId();
                // datecreated = System.currentTimeMillis();
            }

            // get UUIDs for the resource and content        
            CmsUUID newUuidresource = null;
            if ((uuidresource != null) && (resType != CmsResourceTypeFolder.C_RESOURCE_TYPE_ID)) {
                // create a UUID from the provided string
                newUuidresource = new CmsUUID(uuidresource);
            } else {
                // folders get always a new resource record UUID
                newUuidresource = new CmsUUID();
            }
            
            CmsUUID newUuidcontent = null;
            if (uuidcontent != null) {
                // create a UUID from the provided string
                newUuidcontent = new CmsUUID(uuidcontent);
            } else if (resType == CmsResourceTypeFolder.C_RESOURCE_TYPE_ID) {
                // folders don't have any "content"- use the Null UUID as the content UUID
                newUuidcontent = CmsUUID.getNullUUID();
            } else {
                // create a new UUID
                newUuidcontent = new CmsUUID();
            }
            
            // extract the name of the resource form the destination
            String resname = destination;
            if (resname.endsWith("/")) {
                resname = resname.substring(0, resname.length() - 1);
            }
            if (resname.lastIndexOf("/") > 0) {
                resname = resname.substring(resname.lastIndexOf("/") + 1, resname.length());
            }

            // convert to xml page if wanted
            if (m_convertToXmlPage 
            && (resType == A_CmsImport.C_RESOURCE_TYPE_PAGE_ID 
                || resType == C_RESOURCE_TYPE_NEWPAGE_ID)) {
                
                if (content != null) {
                    //get the encoding
                    String encoding = null;                    
                    encoding = CmsProperty.get(I_CmsConstants.C_PROPERTY_CONTENT_ENCODING, properties).getValue();
                    if (encoding == null) {
                        encoding = OpenCms.getSystemInfo().getDefaultEncoding();
                    }  
                    
                    CmsXmlPage xmlPage = CmsXmlPageConverter.convertToXmlPage(m_cms, new String(content, encoding), "body", getLocale(destination, properties), encoding);                     
                    content = xmlPage.marshal();
                }
                resType = CmsResourceTypeXmlPage.C_RESOURCE_TYPE_ID;
                loaderId = OpenCms.getResourceManager().getResourceType(resType).getLoaderId();
            }
            
            // create a new CmsResource                         
            CmsResource resource = new CmsResource(
                new CmsUUID(), // structure ID is always a new UUID
                newUuidresource, 
                CmsUUID.getNullUUID(),
                newUuidcontent, 
                resname, 
                resType, 
                new Integer(flags).intValue(), 
                m_cms.getRequestContext().currentProject().getId(), 
                I_CmsConstants.C_STATE_NEW, 
                loaderId, 
                datecreated, 
                newUsercreated, 
                datelastmodified, 
                newUserlastmodified, 
                datereleased,
                dateexpired,
                1, 
                size
            );
             
            if (C_RESOURCE_TYPE_LINK_ID == resType) {
                // store links for later conversion
                m_report.print(m_report.key("report.storing_link"), I_CmsReport.C_FORMAT_NOTE);
                m_linkStorage.put(m_importPath + destination, new String(content));
                m_linkPropertyStorage.put(m_importPath + destination, properties);
                res = resource;
            } else {             
                // import this resource in the VFS   
                res = m_cms.importResource(m_importPath + destination, resource, content, properties);
            }

            if (res != null) {
                if (C_RESOURCE_TYPE_PAGE_ID == resType) {
                    m_importedPages.add(I_CmsConstants.C_FOLDER_SEPARATOR + destination);
                }
                m_report.println(m_report.key("report.ok"), I_CmsReport.C_FORMAT_OK);
            }          
        } catch (Exception exc) {
            // an error while importing the file
            m_report.println(exc);
            try {
                // Sleep some time after an error so that the report output has a chance to keep up
                Thread.sleep(1000);
            } catch (Exception e) {
                // 
            }
        }
        return res;
    }


    /**
     * Copnvert a given timestamp in string format to a long value.<p>
     * 
     * The timestamp is either the string representation of a long value (old export format)
     * or a user-readable string format.
     * 
     * @param timestamp timestamp to convert
     * @return long value of the timestamp
     */
    private long convertTimestamp(String timestamp) {
        long value = 0;
        // try to parse the timestamp string
        // if it successes, its an old style long value
        try {
            value = Long.parseLong(timestamp);
            
        } catch (NumberFormatException e) {           
            // the timestamp was in in a user-readable string format, create the long value form it
            try {
                value = CmsDateUtil.parseHeaderDate(timestamp);
            } catch (ParseException pe) {
                value = System.currentTimeMillis();
            }
        }
        return value;
    }
    
    
}
