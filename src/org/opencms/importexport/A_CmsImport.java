/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/importexport/A_CmsImport.java,v $
 * Date   : $Date: 2004/08/12 11:01:30 $
 * Version: $Revision: 1.44 $
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

import org.opencms.file.CmsGroup;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsResource;
import org.opencms.file.types.CmsResourceTypePointer;
import org.opencms.i18n.CmsLocaleManager;
import org.opencms.main.CmsException;
import org.opencms.main.I_CmsConstants;
import org.opencms.main.OpenCms;
import org.opencms.report.I_CmsReport;
import org.opencms.security.CmsAccessControlEntry;
import org.opencms.util.CmsUUID;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;

/**
 * Collection of common used methods for implementing OpenCms Import classes.<p>
 * 
 * This class does not implement a real OpenCms import, real import implmentation should be 
 * inherited form this class.<p>
 *
 * @author Michael Emmerich (m.emmerich@alkacon.com)
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * 
 * @see org.opencms.importexport.I_CmsImport
 */

public abstract class A_CmsImport implements I_CmsImport {

    /** The algorithm for the message digest. */
    public static final String C_IMPORT_DIGEST = "MD5";
    
    /** The id of the legacy resource type "page". */
    public static final int C_RESOURCE_TYPE_PAGE_ID = 1;
    
    /** The name of the legacy resource type "page". */
    public static final String C_RESOURCE_TYPE_PAGE_NAME = "page";
    
    /** The id of the legacy resource type "link". */
    public static final int C_RESOURCE_TYPE_LINK_ID = 2;
    
    /** The name of the legacy resource type "link". */
    public static final String C_RESOURCE_TYPE_LINK_NAME = "link";
    
    /** The id of the legacy resource type "newpage". */
    public static final int C_RESOURCE_TYPE_NEWPAGE_ID = 9;
    
    /** The name of the legacy resource type "newpage". */
    public static final String C_RESOURCE_TYPE_NEWPAGE_NAME = "newpage";
    
    /** Debug flag to show debug output. */
    protected static final int DEBUG = 0;

    /** Access control entries for a single resource. */
    private Vector m_acEntriesToCreate;

    /** The cms context to do the import operations with. */
    protected CmsObject m_cms;

    /** Digest for taking a fingerprint of the files. */
    protected MessageDigest m_digest;

    /** The xml manifest-file. */
    protected Document m_docXml;

    /** Groups to create during import are stored here. */
    protected Stack m_groupsToCreate;

    /**
     * In this vector we store the imported pages (as Strings from getAbsolutePath()),
     * after the import we check them all to update the link tables for the linkmanagement.
     */
    protected List m_importedPages;

    /** Indicates if module data is being imported. */
    protected boolean m_importingChannelData;

    /** The import-path to write resources into the cms. */
    protected String m_importPath;

    /** The import-resource (folder) to load resources from. */
    protected File m_importResource;

    /** Flag for conversion to xml pages. */
    protected boolean m_convertToXmlPage;

    /** The import-resource (zip) to load resources from. */
    protected ZipFile m_importZip;

    /** Storage for all pointer properties which must be converted into links. */
    protected HashMap m_linkPropertyStorage;

    /** Storage for all pointers which must be converted into links. */
    protected HashMap m_linkStorage;

    /** The object to report the log messages. */
    protected I_CmsReport m_report;

    /**
     * Initializes all member variables before the import is started.<p>
     * 
     * This is required since there is only one instance for
     * each import version that is kept in memory and reused.<p>
     */
    protected void initialize() {
        m_importedPages = new Vector();
        m_groupsToCreate = new Stack();
        m_acEntriesToCreate = new Vector();
    }
    
    /**
     * Cleans up member variables after the import is finished.<p>
     * 
     * This is required since there is only one instance for
     * each import version that is kept in memory and reused.<p>
     */
    protected void cleanUp() {
        m_importResource = null;
        m_importZip = null;
        m_report = null;
        m_linkStorage = null;
        m_linkPropertyStorage = null;
        m_importedPages = null;
        m_groupsToCreate = null;
        m_acEntriesToCreate = null; 
        m_cms = null;
    }
    
    /**
     * Creates a new access control entry and stores it for later write out.
     * 
     * @param res the resource
     * @param id the id of the principal
     * @param allowed the allowed permissions
     * @param denied the denied permissions
     * @param flags the flags
     */
    protected void addImportAccessControlEntry(CmsResource res, String id, String allowed, String denied, String flags) {

        CmsAccessControlEntry ace = new CmsAccessControlEntry(res.getResourceId(), new CmsUUID(id), Integer.parseInt(allowed), Integer.parseInt(denied), Integer.parseInt(flags));
        m_acEntriesToCreate.add(ace);
    }

    /**
     * Checks if the resources is in the list of immutalbe resources. <p>
     * 
     * @param translatedName the name of the resource
     * @param immutableResources the list of the immutable resources
     * @return true or false
     */
    protected boolean checkImmutable(String translatedName, List immutableResources) {

        boolean resourceNotImmutable = true;
        if (immutableResources.contains(translatedName)) {
            if (DEBUG > 1) {
                System.err.println("Import: Translated resource name is immutable");
            }
            // this resource must not be modified by an import if it already exists
            m_cms.getRequestContext().saveSiteRoot();
            try {
                m_cms.getRequestContext().setSiteRoot("/");
                m_cms.readResource(translatedName);
                resourceNotImmutable = false;
                if (DEBUG > 0) {
                    System.err.println("Import: Immutable flag set for resource");
                }
            } catch (CmsException e) {
                // resourceNotImmutable will be true
                if (DEBUG > 0) {
                    System.err.println("Import: Immutable test caused exception " + e);
                }
            } finally {
                m_cms.getRequestContext().restoreSiteRoot();
            }
        }
        return resourceNotImmutable;
    }

    /**
     * Converts old style pointers to siblings if possible.<p>
     * @throws CmsException if something goes wrong
     */
    protected void convertPointerToSiblings() throws CmsException {
        Set checkedProperties = new HashSet();
        Iterator keys = m_linkStorage.keySet().iterator();
        int linksSize = m_linkStorage.size();
        int i = 0;
        // loop through all links to convert
        while (keys.hasNext()) {
            String key = (String)keys.next();
            String link = (String)m_linkStorage.get(key);
            List properties = (List)m_linkPropertyStorage.get(key);
            m_report.print(" ( " + (++i) + " / " + linksSize + " ) ", I_CmsReport.C_FORMAT_NOTE);
            m_report.print(m_report.key("report.convert_link"), I_CmsReport.C_FORMAT_NOTE);
            m_report.print(key + " ");
            m_report.print(m_report.key("report.dots"));

            // check if this is an internal pointer
            if (link.startsWith("/")) {
                // check if the pointer target is existing
                try {
                    CmsResource target = m_cms.readResource(link);

                    // create a new sibling as CmsResource                         
                    CmsResource resource = new CmsResource(
                        new CmsUUID(), // structure ID is always a new UUID
                        target.getResourceId(), 
                        CmsUUID.getNullUUID(), 
                        CmsResource.getName(key), 
                        target.getTypeId(), 
                        0, // TODO: pass flags from import 
                        m_cms.getRequestContext().currentProject().getId(), 
                        I_CmsConstants.C_STATE_NEW,
                        target.getDateCreated(), 
                        target.getUserCreated(), 
                        target.getDateLastModified(), 
                        target.getUserLastModified(), 
                        CmsResource.DATE_RELEASED_DEFAULT, 
                        CmsResource.DATE_EXPIRED_DEFAULT,
                        1,
                        0
                    );
                    
                    m_cms.importResource(key, resource, null, properties);
                    m_report.println(m_report.key("report.ok"), I_CmsReport.C_FORMAT_OK);
                } catch (CmsException ex) {
                    m_report.println();
                    m_report.print(m_report.key("report.convert_link_notfound") + " " + link, I_CmsReport.C_FORMAT_WARNING);
                    
                    if (OpenCms.getLog(this).isDebugEnabled()) {
                        OpenCms.getLog(this).debug("Link conversion failed: " + key + " -> " + link, ex);
                    }
                }

            } else {
                // make sure all found properties are already defined
                for (int j = 0, n = properties.size(); j < n; j++) {
                    CmsProperty property = (CmsProperty)properties.get(j);
                    
                    if (!checkedProperties.contains(property)) {
                        // check the current property and create it, if necessary
                        checkPropertyDefinition(property.getKey());
                        checkedProperties.add(property);                        
                    }
                }
                m_cms.createResource(key, CmsResourceTypePointer.C_RESOURCE_TYPE_ID, link.getBytes(), properties);
                m_report.println(m_report.key("report.ok"), I_CmsReport.C_FORMAT_OK);
            }
        }
        m_linkStorage = null;
        m_linkPropertyStorage = null;
    }

    /**
     * Tries to read the property definition for a specified key.<p>
     * The property defintion gets created if it is missing.<p>
     *
     * @param key the key of the property
     * @throws CmsException if something goes wrong
     */
    private void checkPropertyDefinition(String key) throws CmsException {
        try {
            // try to read the property definition
            m_cms.readPropertydefinition(key);
        } catch (CmsException exc) {
            // create missing property definitions
            m_cms.createPropertydefinition(key);
        }
    }

    /**
     * Returns a byte array containing the content of the file.<p>
     *
     * @param filename the name of the file to read
     * @return a byte array containing the content of the file
     */
    protected byte[] getFileBytes(String filename) {
        try {
            // is this a zip-file?
            if (m_importZip != null) {
                // yes
                ZipEntry entry = m_importZip.getEntry(filename);
                InputStream stream = m_importZip.getInputStream(entry);

                int charsRead = 0;
                int size = new Long(entry.getSize()).intValue();
                byte[] buffer = new byte[size];
                while (charsRead < size) {
                    charsRead += stream.read(buffer, charsRead, size - charsRead);
                }
                stream.close();
                return buffer;
            } else {
                // no - use directory
                File file = new File(m_importResource, filename);
                FileInputStream fileStream = new FileInputStream(file);

                int charsRead = 0;
                int size = new Long(file.length()).intValue();
                byte[] buffer = new byte[size];
                while (charsRead < size) {
                    charsRead += fileStream.read(buffer, charsRead, size - charsRead);
                }
                fileStream.close();
                return buffer;
            }
        } catch (FileNotFoundException fnfe) {
            m_report.println(fnfe);
        } catch (IOException ioe) {
            m_report.println(ioe);
        }
        // this will only be returned in case there was an exception
        return "".getBytes();
    }

    /**
     * Returns the appropriate locale for the given destination.<p>
     * 
     * @param destination the destination path (parent must exist)
     * @param properties the properties to check at first
     * @return the locale
     * @throws CmsException if something goes wrong
     */
    protected Locale getLocale(String destination, List properties) throws CmsException {        
        String localeName = CmsProperty.get(I_CmsConstants.C_PROPERTY_LOCALE, properties).getValue();
        if (localeName == null) {
            localeName = m_cms.readPropertyObject(CmsResource.getParentFolder(destination), I_CmsConstants.C_PROPERTY_LOCALE, true).getValue();
        }
        if (localeName != null) {
            if (localeName.indexOf(",") >= 0) {
                localeName = localeName.substring(0, localeName.indexOf(","));
            }
            return CmsLocaleManager.getLocale(localeName);
        } else {            
            return (Locale)OpenCms.getLocaleManager().getDefaultLocales(m_cms, CmsResource.getParentFolder(destination)).get(0);
        }
    }
    
    /**
     * Reads all properties below a specified parent element from manifest.xml.<p>
     * 
     * @param parentElement the current file node
     * @param propertyKey key of a property to be added to all resources, or null
     * @param propertyValue value of the property to be added to all resources, or null
     * @param ignoredPropertyKeys a list of properies to be ignored
     * 
     * @return a list with all properties
     * @throws CmsException if something goes wrong
     */
    protected List readPropertiesFromManifest(
        Element parentElement,
        String propertyKey,
        String propertyValue,
        List ignoredPropertyKeys) throws CmsException {

        // all imported Cms property objects are collected in map first forfaster access
        Map properties = new HashMap();
        CmsProperty property = null;
        List propertyElements = parentElement.selectNodes("./"
            + I_CmsConstants.C_EXPORT_TAG_PROPERTIES
            + "/"
            + I_CmsConstants.C_EXPORT_TAG_PROPERTY);
        Element propertyElement = null;
        String key = null, value = null;
        Attribute attrib = null;

        if (propertyKey != null && propertyValue != null && !"".equals(propertyKey)) {
            checkPropertyDefinition(propertyKey);
            properties.put(propertyKey, propertyValue);
        }

        // iterate over all property elements
        for (int i = 0, n = propertyElements.size(); i < n; i++) {
            propertyElement = (Element)propertyElements.get(i);
            key = CmsImport.getChildElementTextValue(propertyElement, I_CmsConstants.C_EXPORT_TAG_NAME);

            if (key == null || ignoredPropertyKeys.contains(key)) {
                // continue if the current property (key) should be ignored or is null
                continue;
            }

            // all Cms properties are collected in a map keyed by their property keys
            if ((property = (CmsProperty)properties.get(key)) == null) {
                property = new CmsProperty();
                property.setKey(key);
                property.setAutoCreatePropertyDefinition(true);
                properties.put(key, property);
            }

            if ((value = CmsImport.getChildElementTextValue(propertyElement, I_CmsConstants.C_EXPORT_TAG_VALUE)) == null) {
                value = "";
            }

            if ((attrib = propertyElement.attribute(I_CmsConstants.C_EXPORT_TAG_PROPERTY_ATTRIB_TYPE)) != null
                && attrib.getValue().equals(I_CmsConstants.C_EXPORT_TAG_PROPERTY_ATTRIB_TYPE_SHARED)) {
                // it is a shared/resource property value
                property.setResourceValue(value);
            } else {
                // it is an individual/structure value
                property.setStructureValue(value);
            }

            checkPropertyDefinition(key);
        }

        return new ArrayList(properties.values());
    }

    /**
     * Writes alread imported access control entries for a given resource.
     * 
     * @param resource the resource assigned to the access control entries
     * @throws CmsException if something goes wrong
     */
    protected void importAccessControlEntries(CmsResource resource) throws CmsException {
        try {
            try {
                m_cms.importAccessControlEntries(resource, m_acEntriesToCreate);
            } catch (CmsException exc) {
                m_report.println(m_report.key("report.import_accesscontroldata_failed"), I_CmsReport.C_FORMAT_WARNING);
            }
        } catch (Exception exc) {
            throw new CmsException(CmsException.C_UNKNOWN_EXCEPTION, exc);
        } finally {
            m_acEntriesToCreate = new Vector();
        }
    }

    /**
     * Creates an imported group in the cms.<p>
     * 
     * @param id the uuid of this group
     * @param name the name of the group
     * @param description group description
     * @param flags group flags
     * @param parentgroupName name of the parent group
     * @throws CmsException if something goes wrong
     */
    protected void importGroup(String id, String name, String description, String flags, String parentgroupName) throws CmsException {
        if (id == null) {
            id = new CmsUUID().toString();
        }
        if (description == null) {
            description = "";
        }

        CmsGroup parentGroup = null;
        try {
            if ((parentgroupName != null) && (!"".equals(parentgroupName))) {
                try {
                    parentGroup = m_cms.readGroup(parentgroupName);
                } catch (CmsException exc) {
                    // parentGroup will be null
                }
            }

            if (((parentgroupName != null) && (!"".equals(parentgroupName))) && (parentGroup == null)) {
                // cannot create group, put on stack and try to create later
                Hashtable groupData = new Hashtable();
                groupData.put(I_CmsConstants.C_EXPORT_TAG_NAME, name);
                groupData.put(I_CmsConstants.C_EXPORT_TAG_DESCRIPTION, description);
                groupData.put(I_CmsConstants.C_EXPORT_TAG_FLAGS, flags);
                groupData.put(I_CmsConstants.C_EXPORT_TAG_PARENTGROUP, parentgroupName);
                m_groupsToCreate.push(groupData);
            } else {
                try {
                    m_report.print(m_report.key("report.importing_group"), I_CmsReport.C_FORMAT_NOTE);
                    m_report.print(name);
                    m_report.print(m_report.key("report.dots"));
                    m_cms.createGroup(id, name, description, Integer.parseInt(flags), parentgroupName);
                    m_report.println(m_report.key("report.ok"), I_CmsReport.C_FORMAT_OK);
                } catch (CmsException exc) {
                    m_report.println(m_report.key("report.not_created"), I_CmsReport.C_FORMAT_OK);
                }
            }

        } catch (Exception exc) {
            m_report.println(exc);
            throw new CmsException(CmsException.C_UNKNOWN_EXCEPTION, exc);
        }
    }

    /**
     * Creates an imported user in the cms.<p>
     * 
     * @param id user id or null
     * @param name user name
     * @param description user description
     * @param flags user flags
     * @param password user password 
     * @param recoveryPassword user recovery password
     * @param firstname firstname of the user
     * @param lastname lastname of the user
     * @param email user email
     * @param address user address 
     * @param section user section
     * @param defaultGroup user default group
     * @param type user type
     * @param userInfo user info
     * @param userGroups user groups
     * @throws CmsException in case something goes wrong
     */
    protected void importUser(String id, String name, String description, String flags, String password, String recoveryPassword, String firstname, String lastname, String email, String address, String section, String defaultGroup, String type, Hashtable userInfo, Vector userGroups) throws CmsException {

        // create a new user id if not available
        if (id == null) {
            id = new CmsUUID().toString();
        }

        try {
            try {
                m_report.print(m_report.key("report.importing_user"), I_CmsReport.C_FORMAT_NOTE);
                m_report.print(name);
                m_report.print(m_report.key("report.dots"));
                m_cms.addImportUser(id, name, password, recoveryPassword, description, firstname, lastname, email, Integer.parseInt(flags), userInfo, defaultGroup, address, section, Integer.parseInt(type));
                // add user to all groups vector
                for (int i = 0; i < userGroups.size(); i++) {
                    try {
                        m_cms.addUserToGroup(name, (String)userGroups.elementAt(i));
                    } catch (CmsException exc) {
                        // ignore
                    }
                }
                m_report.println(m_report.key("report.ok"), I_CmsReport.C_FORMAT_OK);
            } catch (CmsException exc) {
                m_report.println(m_report.key("report.not_created"), I_CmsReport.C_FORMAT_OK);
            }
        } catch (Exception exc) {
            throw new CmsException(CmsException.C_UNKNOWN_EXCEPTION, exc);
        }
    }
        

}
