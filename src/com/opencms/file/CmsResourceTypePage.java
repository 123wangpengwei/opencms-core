/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/Attic/CmsResourceTypePage.java,v $
* Date   : $Date: 2002/09/03 11:57:01 $
* Version: $Revision: 1.28 $
*
* This library is part of OpenCms -
* the Open Source Content Mananagement System
*
* Copyright (C) 2001  The OpenCms Group
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* For further information about OpenCms, please see the
* OpenCms Website: http://www.opencms.org
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package com.opencms.file;

import java.util.zip.*;


import com.opencms.core.*;
import com.opencms.template.*;
import com.opencms.util.*;
import com.opencms.linkmanagement.*;
import java.util.*;

import java.io.*;
import org.w3c.dom.*;
import com.opencms.file.genericSql.*;
//import com.opencms.file.genericSql.linkmanagement.*;

/**
 * Access class for resources of the type "Page".
 *
 * @author Alexander Lucas
 * @version $Revision: 1.28 $ $Date: 2002/09/03 11:57:01 $
 */
public class CmsResourceTypePage implements I_CmsResourceType, Serializable, I_CmsConstants, com.opencms.workplace.I_CmsWpConstants {

     /** Definition of the class */
     private final static String C_CLASSNAME="com.opencms.template.CmsXmlTemplate";

     private static final String C_DEFAULTBODY_START = "<?xml version=\"1.0\" encoding=\"" + I_CmsXmlParser.C_XML_ENCODING + "\"?>\n<XMLTEMPLATE>\n<TEMPLATE>\n<![CDATA[\n";
     private static final String C_DEFAULTBODY_END = "]]></TEMPLATE>\n</XMLTEMPLATE>";

     /**
      * The id of resource type.
      */
    private int m_resourceType;

    /**
     * The id of the launcher used by this resource.
     */
    private int m_launcherType;

    /**
     * The resource type name.
     */
    private String m_resourceTypeName;

    /**
     * The class name of the Java class launched by the launcher.
     */
    private String m_launcherClass;


    /**
     * inits a new CmsResourceType object.
     *
     * @param resourceType The id of the resource type.
     * @param launcherType The id of the required launcher.
     * @param resourceTypeName The printable name of the resource type.
     * @param launcherClass The Java class that should be invoked by the launcher.
     * This value is <b> null </b> if the default invokation class should be used.
     */
    public void init(int resourceType, int launcherType,
                           String resourceTypeName, String launcherClass){

        m_resourceType=resourceType;
        m_launcherType=launcherType;
        m_resourceTypeName=resourceTypeName;
        m_launcherClass=launcherClass;
    }
     /**
     * Returns the name of the Java class loaded by the launcher.
     * This method returns <b>null</b> if the default class for this type is used.
     *
     * @return the name of the Java class.
     */
     public String getLauncherClass() {
         if ((m_launcherClass == null) || (m_launcherClass.length()<1)) {
            return C_UNKNOWN_LAUNCHER;
         } else {
            return m_launcherClass;
         }
     }
     /**
     * Returns the launcher type needed for this resource-type.
     *
     * @return the launcher type for this resource-type.
     */
     public int getLauncherType() {
         return m_launcherType;
     }
    /**
     * Returns the name for this resource-type.
     *
     * @return the name for this resource-type.
     */
     public String getResourceTypeName() {
         return m_resourceTypeName;
     }
    /**
     * Returns the type of this resource-type.
     *
     * @return the type of this resource-type.
     */
    public int getResourceType() {
         return m_resourceType;
     }
    /**
     * Returns a string-representation for this object.
     * This can be used for debugging.
     *
     * @return string-representation for this object.
     */
     public String toString() {
        StringBuffer output=new StringBuffer();
        output.append("[ResourceType]:");
        output.append(m_resourceTypeName);
        output.append(" , Id=");
        output.append(m_resourceType);
        output.append(" , launcherType=");
        output.append(m_launcherType);
        output.append(" , launcherClass=");
        output.append(m_launcherClass);
        return output.toString();
      }

    /**
    * Changes the group of a resource.
    * <br>
    * Only the group of a resource in an offline project can be changed. The state
    * of the resource is set to CHANGED (1).
    * If the content of this resource is not existing in the offline project already,
    * it is read from the online project and written into the offline project.
    * <p>
    * <B>Security:</B>
    * Access is granted, if:
    * <ul>
    * <li>the user has access to the project</li>
    * <li>the user is owner of the resource or is admin</li>
    * <li>the resource is locked by the callingUser</li>
    * </ul>
    *
    * @param filename the complete path to the resource.
    * @param newGroup the name of the new group for this resource.
    * @param chRekursive only used by folders.
    *
    * @exception CmsException if operation was not successful.
    */
    public void chgrp(CmsObject cms, String filename, String newGroup, boolean chRekursive) throws CmsException{

        CmsFile file = cms.readFile(filename);
        // check if the current user has the right to change the group of the
        // resource. Only the owner of a file and the admin are allowed to do this.
        if ((cms.getRequestContext().currentUser().equals(cms.readOwner(file))) ||
            (cms.userInGroup(cms.getRequestContext().currentUser().getName(), C_GROUP_ADMIN))){
            cms.doChgrp(filename, newGroup);
            //check if the file type name is page
            String bodyPath = checkBodyPath(cms, (CmsFile)file);
            if (bodyPath != null){
                cms.doChgrp(bodyPath, newGroup);
            }
        }
    }

    /**
    * Changes the flags of a resource.
    * <br>
    * Only the flags of a resource in an offline project can be changed. The state
    * of the resource is set to CHANGED (1).
    * If the content of this resource is not existing in the offline project already,
    * it is read from the online project and written into the offline project.
    * The user may change the flags, if he is admin of the resource.
    * <p>
    * <B>Security:</B>
    * Access is granted, if:
    * <ul>
    * <li>the user has access to the project</li>
    * <li>the user can write the resource</li>
    * <li>the resource is locked by the callingUser</li>
    * </ul>
    *
    * @param filename the complete path to the resource.
    * @param flags the new flags for the resource.
    * @param chRekursive only used by folders.
    *
    * @exception CmsException if operation was not successful.
    * for this resource.
    */
    public void chmod(CmsObject cms, String filename, int flags, boolean chRekursive) throws CmsException{

        CmsFile file = cms.readFile(filename);
        // check if the current user has the right to change the group of the
        // resource. Only the owner of a file and the admin are allowed to do this.
        if ((cms.getRequestContext().currentUser().equals(cms.readOwner(file))) ||
           (cms.userInGroup(cms.getRequestContext().currentUser().getName(), C_GROUP_ADMIN))){

            // modify the access flags
            cms.doChmod(filename, flags);

            String bodyPath = checkBodyPath(cms, (CmsFile)file);
            if (bodyPath != null){
                // set the internal read flag if nescessary
                if ((flags & C_ACCESS_INTERNAL_READ) ==0 ) {
                    flags += C_ACCESS_INTERNAL_READ;
                }
                cms.doChmod(bodyPath, flags);
            }
        }
    }

    /**
    * Changes the owner of a resource.
    * <br>
    * Only the owner of a resource in an offline project can be changed. The state
    * of the resource is set to CHANGED (1).
    * If the content of this resource is not existing in the offline project already,
    * it is read from the online project and written into the offline project.
    * The user may change this, if he is admin of the resource.
    * <p>
    * <B>Security:</B>
    * Access is cranted, if:
    * <ul>
    * <li>the user has access to the project</li>
    * <li>the user is owner of the resource or the user is admin</li>
    * <li>the resource is locked by the callingUser</li>
    * </ul>
    *
    * @param filename the complete path to the resource.
    * @param newOwner the name of the new owner for this resource.
    * @param chRekursive only used by folders.
    *
    * @exception CmsException if operation was not successful.
    */
    public void chown(CmsObject cms, String filename, String newOwner, boolean chRekursive) throws CmsException{
        CmsFile file = cms.readFile(filename);
        // check if the current user has the right to change the group of the
        // resource. Only the owner of a file and the admin are allowed to do this.
        if ((cms.getRequestContext().currentUser().equals(cms.readOwner(file))) ||
            (cms.userInGroup(cms.getRequestContext().currentUser().getName(), C_GROUP_ADMIN))){
            cms.doChown(filename, newOwner);
            //check if the file type name is page
            String bodyPath = checkBodyPath(cms, (CmsFile)file);
            if (bodyPath != null){
                cms.doChown(bodyPath, newOwner);
            }
        }
    }

    /**
    * Changes the resourcetype of a resource.
    * <br>
    * Only the resourcetype of a resource in an offline project can be changed. The state
    * of the resource is set to CHANGED (1).
    * If the content of this resource is not exisiting in the offline project already,
    * it is read from the online project and written into the offline project.
    * The user may change this, if he is admin of the resource.
    * <p>
    * <B>Security:</B>
    * Access is granted, if:
    * <ul>
    * <li>the user has access to the project</li>
    * <li>the user is owner of the resource or is admin</li>
    * <li>the resource is locked by the callingUser</li>
    * </ul>
    *
    * @param filename the complete path to the resource.
    * @param newType the name of the new resourcetype for this resource.
    *
    * @exception CmsException if operation was not successful.
    */
    public void chtype(CmsObject cms, String filename, String newType) throws CmsException{
        CmsFile file = cms.readFile(filename);
        // check if the current user has the right to change the group of the
        // resource. Only the owner of a file and the admin are allowed to do this.
        if ((cms.getRequestContext().currentUser().equals(cms.readOwner(file))) ||
            (cms.userInGroup(cms.getRequestContext().currentUser().getName(), C_GROUP_ADMIN))){
            cms.doChtype(filename, newType);
            //check if the file type name is page
            String bodyPath = checkBodyPath(cms, (CmsFile)file);
            if (bodyPath != null){
                cms.doChtype(bodyPath, newType);
            }
        }
    }


    /**
    * Copies a Resource.
    *
    * @param source the complete path of the sourcefile.
    * @param destination the complete path of the destinationfolder.
    * @param keepFlags <code>true</code> if the copy should keep the source file's flags,
    *        <code>false</code> if the copy should get the user's default flags.
    *
    * @exception CmsException if the file couldn't be copied, or the user
    * has not the appropriate rights to copy the file.
    */
    public void copyResource(CmsObject cms, String source, String destination, boolean keepFlags) throws CmsException{
        // Read and parse the source page file
        CmsFile file = cms.readFile(source);
        CmsXmlControlFile hXml=new CmsXmlControlFile(cms, file);

        // Check the path of the body file.
        // Don't use the checkBodyPath method here to avaoid overhead.
        String bodyPath=(C_CONTENTBODYPATH.substring(0, C_CONTENTBODYPATH.lastIndexOf("/")))+(source);
        if (bodyPath.equals(hXml.getElementTemplate("body"))){

            // Evaluate some path information
            String destinationFolder = destination.substring(0,destination.lastIndexOf("/")+1);
            checkFolders(cms, destinationFolder);
            String newbodyPath=(C_CONTENTBODYPATH.substring(0, C_CONTENTBODYPATH.lastIndexOf("/")))+ destination;

            // we don't want to use the changeContent method here
            // to avoid overhead by copying, readig, parsing, setting XML and writing again.
            // Instead, we re-use the already parsed XML content of the source
            hXml.setElementTemplate("body", newbodyPath);
            cms.doCopyFile(source, destination);
            CmsFile newPageFile = cms.readFile(destination);
            newPageFile.setContents(hXml.getXmlText().getBytes());
            cms.writeFile(newPageFile);

            // Now the new page file is created. Copy the body file
            cms.doCopyFile(bodyPath, newbodyPath);
            // linkmanagement: copy the links of the page
            cms.createLinkEntrys(newPageFile.getResourceId(), cms.readLinkEntrys(file.getResourceId()));
        } else {
            // The body part of the source was not found at
            // the default place. Leave it there, don't make
            // a copy and simply make a copy of the page file.
            // So the new page links to the old body.
            cms.doCopyFile(source, destination);
        }
        // set access flags, if neccessary
        if(!keepFlags) {
            setDefaultFlags(cms, destination);
        }
    }

    /**
    * Copies a resource from the online project to a new, specified project.
    * <br>
    * Copying a resource will copy the file header or folder into the specified
    * offline project and set its state to UNCHANGED.
    *
    * @param resource the name of the resource.
    * @exception CmsException if operation was not successful.
    */
    public void copyResourceToProject(CmsObject cms, String resourceName) throws CmsException {
        //String resourceName = linkManager.getResourceName(resourceId);
        CmsFile file = cms.readFile(resourceName, true);
        cms.doCopyResourceToProject(resourceName);
        //check if the file type name is page
        String bodyPath = checkBodyPath(cms, (CmsFile)file);
        if (bodyPath != null){
            cms.doCopyResourceToProject(bodyPath);
        }
    }

    /**
     * Creates a new resource
     *
     * @param cms The CmsObject
     * @param folder The name of the parent folder
     * @param name The name of the file
     * @param properties The properties of the file
     * @param contents The file content
     *
     * @exception CmsException if operation was not successful.
     */
    public CmsResource createResource(CmsObject cms, String folder, String name, Hashtable properties, byte[] contents) throws CmsException{

        // Scan for mastertemplates
        Vector allMasterTemplates = cms.getFilesInFolder(C_CONTENTTEMPLATEPATH);

        // Select the first mastertemplate as default
        String masterTemplate = "";
        if(allMasterTemplates.size() > 0) {
            masterTemplate = ((CmsFile)allMasterTemplates.elementAt(0)).getAbsolutePath();
        }

        // Evaluate the absolute path to the new body file
        String bodyFolder =(C_CONTENTBODYPATH.substring(0, C_CONTENTBODYPATH.lastIndexOf("/"))) + folder;

        // Create the new page file
        //CmsFile file = cms.doCreateFile(folder, name, "".getBytes(), I_CmsConstants.C_TYPE_PAGE_NAME, properties);
        CmsFile file = cms.doCreateFile(folder, name, "".getBytes(), m_resourceTypeName, properties);
        cms.doLockResource(folder + name, true);
        CmsXmlControlFile pageXml = new CmsXmlControlFile(cms, file);
        pageXml.setTemplateClass(C_CLASSNAME);
        pageXml.setMasterTemplate(masterTemplate);
        pageXml.setElementClass("body", C_CLASSNAME);
        pageXml.setElementTemplate("body", bodyFolder + name);
        pageXml.write();

        // Check, if the body path exists and create missing folders, if neccessary
        checkFolders(cms, folder);

        // Create the new body file
        //CmsFile bodyFile = cms.doCreateFile(bodyFolder, name, (C_DEFAULTBODY_START + new String(contents) + C_DEFAULTBODY_END).getBytes(), I_CmsConstants.C_TYPE_BODY_NAME, new Hashtable());
        CmsFile bodyFile = cms.doCreateFile(bodyFolder, name, (C_DEFAULTBODY_START + new String(contents) + C_DEFAULTBODY_END).getBytes(), I_CmsConstants.C_TYPE_PLAIN_NAME, new Hashtable());
        cms.doLockResource(bodyFolder + name, true);
        int flags = bodyFile.getAccessFlags();
        if ((flags & C_ACCESS_INTERNAL_READ) ==0 ) {
            flags += C_ACCESS_INTERNAL_READ;
        }
        cms.chmod(bodyFile.getAbsolutePath(), flags);
        // linkmanagement: create the links of the new page (for the case that the content was not empty
        if(contents.length > 1){
            CmsPageLinks linkObject = cms.getPageLinks(folder+name);
            cms.createLinkEntrys(linkObject.getResourceId(), linkObject.getLinkTargets());
        }
        return file;
    }

    public CmsResource createResource(CmsObject cms, String folder, String name, Hashtable properties, byte[] contents, String masterTemplate) throws CmsException{
        CmsFile resource = (CmsFile)createResource(cms, folder, name, properties, contents);
        CmsXmlControlFile pageXml = new CmsXmlControlFile(cms, resource);
        pageXml.setMasterTemplate(masterTemplate);
        pageXml.write();
        return resource;
    }

    /**
    * Deletes a resource.
    *
    * @param filename the complete path of the file.
    *
    * @exception CmsException if the file couldn't be deleted, or if the user
    * has not the appropriate rights to delete the file.
    */
    public void deleteResource(CmsObject cms, String filename) throws CmsException{
        CmsFile file = cms.readFile(filename);
        cms.doDeleteFile(filename);
        // linkmanagement: delete the links on the page
        cms.deleteLinkEntrys(file.getResourceId());
        String bodyPath = checkBodyPath(cms, (CmsFile)file);
        if (bodyPath != null){
            try{
                cms.doDeleteFile(bodyPath);
            } catch (CmsException e){
                if(e.getType() != CmsException.C_NOT_FOUND){
                    throw e;
                }
            }
        }

        // The page file contains XML.
        // So there could be some data in the parser's cache.
        // Clear it!
        String currentProject = cms.getRequestContext().currentProject().getName();
        CmsXmlControlFile.clearFileCache(currentProject + ":" + filename);
    }

    /**
    * Undeletes a resource.
    *
    * @param filename the complete path of the file.
    *
    * @exception CmsException if the file couldn't be undeleted, or if the user
    * has not the appropriate rights to undelete the file.
    */
    public void undeleteResource(CmsObject cms, String filename) throws CmsException{
        CmsFile file = cms.readFile(filename, true);
        cms.doUndeleteFile(filename);
        String bodyPath = checkBodyPath(cms, (CmsFile)file);
        if (bodyPath != null){
            try{
                cms.doUndeleteFile(bodyPath);
            } catch (CmsException e){
                if(e.getType() != CmsException.C_NOT_FOUND){
                    throw e;
                }
            }
        }

        // The page file contains XML.
        // So there could be some data in the parser's cache.
        // Clear it!
        String currentProject = cms.getRequestContext().currentProject().getName();
        CmsXmlControlFile.clearFileCache(currentProject + ":" + filename);

        // linkmanagement: create the links of the restored page
        CmsPageLinks linkObject = cms.getPageLinks(file.getAbsolutePath());
        cms.createLinkEntrys(linkObject.getResourceId(), linkObject.getLinkTargets());
    }

    /**
     * When a resource has to be exported, the ID�s inside the
     * Linkmanagement-Tags have to be changed to the corresponding URL�s
     *
     * @param file is the file that has to be changed
     */
    public CmsFile exportResource(CmsObject cms, CmsFile file) throws CmsException {
        //nothing to do here, because there couldn�t be any Linkmanagement-Tags in a page-file (control-file)
        return file;
    }


    /**
     * When a resource has to be imported, the URL�s of the
     * Links inside the resources have to be saved and changed to the corresponding ID�s
     *
     * @param file is the file that has to be changed
     */
    public CmsResource importResource(CmsObject cms, String source, String destination, String type, String user, String group, String access, Hashtable properties, String launcherStartClass, byte[] content, String importPath) throws CmsException {
        CmsFile file = null;

        String path = importPath + destination.substring(0, destination.lastIndexOf("/") + 1);
        String name = destination.substring((destination.lastIndexOf("/") + 1), destination.length());
        int state = C_STATE_NEW;
        // this is a file
        // first delete the file, so it can be overwritten
        try {
            cms.doLockResource(path + name, true);
            cms.doDeleteFile(path + name);
            state = C_STATE_CHANGED;
        } catch (CmsException exc) {
            state = C_STATE_NEW;
            // ignore the exception, the file dosen't exist
        }
        // now create the file

        // do not use createResource because then there will the body-file be created too.
        // that would cause an exception while importing because of trying to
        // duplicate an entry
        file = (CmsFile)cms.doCreateFile(path, name, content, type, properties);
        String fullname = file.getAbsolutePath();
        lockResource(cms, fullname, true);
        try{
            cms.doChmod(fullname, Integer.parseInt(access));
        }catch(CmsException e){
            System.out.println("chmod(" + access + ") failed ");
        }
        try{
            cms.doChgrp(fullname, group);
        }catch(CmsException e){
            System.out.println("chgrp(" + group + ") failed ");
        }
        try{
            cms.doChown(fullname, user);
        }catch(CmsException e){
            System.out.println("chown((" + user + ") failed ");
        }
        if(launcherStartClass != null){
            file.setLauncherClassname(launcherStartClass);
            cms.writeFile(file);
        }

        return file;
    }

    /**
    * Locks a given resource.
    * <br>
    * A user can lock a resource, so he is the only one who can write this
    * resource.
    *
    * @param resource the complete path to the resource to lock.
    * @param force if force is <code>true</code>, a existing locking will be overwritten.
    *
    * @exception CmsException if the user has not the rights to lock this resource.
    * It will also be thrown, if there is a existing lock and force was set to false.
    */
    public void lockResource(CmsObject cms, String resource, boolean force) throws CmsException{
        // First read the page file.
        CmsFile pageFile = cms.readFile(resource);

        CmsUser pageLocker = null;
        CmsUser bodyLocker = null;
        // Check any locks on th page file
        pageLocker = getLockedBy(cms, resource);
        CmsUser currentUser = cms.getRequestContext().currentUser();
        boolean pageLockedAndSelf = pageLocker != null && currentUser.equals(pageLocker);
        CmsResource bodyFile = null;
        String bodyPath = null;
        // Try to fetch the body file.
        try {
            bodyPath = readBodyPath(cms, pageFile);
            bodyFile = cms.readFileHeader(bodyPath);
        } catch(Exception e) {
            bodyPath = null;
            bodyFile = null;
        }
        // first lock the page file
        cms.doLockResource(resource, force);

        if(bodyFile != null) {
            // Everything with the page file is ok. We have write access. XML is valid.
            // Body file could be determined and fetched.
            // Now check further body file details (is it locked already, WHO has locked it, etc.)
            bodyLocker = getLockedBy(cms, bodyPath);
            // Lock the body, if neccessary
            //if((bodyLocker == null && (pageLocker == null || pageLockedAndSelf || force))
            //        || (bodyLocker != null && !currentUser.equals(bodyLocker)
            //            && !(pageLocker != null && !currentUser.equals(pageLocker) && !force))) {
                cms.doLockResource(bodyPath, force);
            //}
        }
/*
        // Lock the page file, if neccessary
        if(!(pageLockedAndSelf && (bodyFile != null && ((bodyLocker == null)
           || !currentUser.equals(bodyLocker))))) {
            cms.doLockResource(resource, force);
        }
*/
    }

    /**
    * Moves a resource to the given destination.
    *
    * @param source the complete path of the sourcefile.
    * @param destination the complete path of the destinationfile.
    *
    * @exception CmsException if the user has not the rights to move this resource,
    * or if the file couldn't be moved.
    */
    public void moveResource(CmsObject cms, String source, String destination) throws CmsException{
        CmsFile file = cms.readFile(source);
        String bodyPath = checkBodyPath(cms, file);
        if(bodyPath != null) {
            String hbodyPath = C_CONTENTBODYPATH.substring(0, C_CONTENTBODYPATH.lastIndexOf("/")) + destination;
            checkFolders(cms, destination.substring(0, destination.lastIndexOf("/")));
            cms.doMoveFile(bodyPath, hbodyPath);
            changeContent(cms, source, hbodyPath);
        }
        cms.doMoveFile(source, destination);
        // linkmanagement: delete the links of the old page and create them for the new one
        int oldId = file.getResourceId();
        int newId = cms.readFileHeader(destination).getResourceId();
        cms.createLinkEntrys(newId, cms.readLinkEntrys(oldId));
        cms.deleteLinkEntrys(oldId);

    }

    /**
    * Renames the file to the new name.
    *
    * @param oldname the complete path to the file which will be renamed.
    * @param newname the new name of the file.
    *
    * @exception CmsException if the user has not the rights
    * to rename the file, or if the file couldn't be renamed.
    */
    public void renameResource(CmsObject cms, String oldname, String newname) throws CmsException{
        CmsFile file = cms.readFile(oldname);
        String bodyPath = readBodyPath(cms, file);
        int help = C_CONTENTBODYPATH.lastIndexOf("/");
        String hbodyPath=(C_CONTENTBODYPATH.substring(0,help)) + oldname;
        if(hbodyPath.equals(bodyPath)) {
            cms.doRenameFile(bodyPath, newname);
            help=bodyPath.lastIndexOf("/") + 1;
            hbodyPath = bodyPath.substring(0,help) + newname;
            changeContent(cms, oldname, hbodyPath);
        }
        cms.doRenameFile(oldname,newname);
        // linkmanagement: delete the links of the old page and create them for the new one
        int oldId = file.getFileId();
        int newId = cms.readFileHeader(file.getParent()+newname).getFileId();
        cms.createLinkEntrys(newId, cms.readLinkEntrys(oldId));
        cms.deleteLinkEntrys(oldId);
    }

    /**
     * Restores a file in the current project with a version in the backup
     *
     * @param cms The CmsObject
     * @param versionId The version id of the resource
     * @param filename The name of the file to restore
     *
     * @exception CmsException  Throws CmsException if operation was not succesful.
     */
    public void restoreResource(CmsObject cms, int versionId, String filename) throws CmsException{
        if(!cms.accessWrite(filename)){
            throw new CmsException(filename, CmsException.C_NO_ACCESS);
        }
        CmsFile file = cms.readFile(filename);
        cms.doRestoreResource(versionId, filename);
        String bodyPath = checkBodyPath(cms, (CmsFile)file);
        if (bodyPath != null){
            try{
                cms.doRestoreResource(versionId, bodyPath);
            } catch(CmsException e){
                // do not throw an exception when there is no body for this version
                // maybe only the control file was changed
                if(e.getType() == CmsException.C_NOT_FOUND){
                    if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
                        A_OpenCms.log(A_OpenCms.C_OPENCMS_INFO,"[CmsResourceTypePage] version "+versionId+" of "+bodyPath+" not found!");
                    }
                } else {
                    throw e;
                }
            }
        }
    // linkmanagement: create the links of the restored page
    CmsPageLinks linkObject = cms.getPageLinks(file.getAbsolutePath());
    cms.createLinkEntrys(linkObject.getResourceId(), linkObject.getLinkTargets());
    }

    /**
    * Undo changes in a resource.
    * <br>
    *
    * @param resource the complete path to the resource to be restored.
    *
    * @exception CmsException if the user has not the rights
    * to write this resource.
    */
    public void undoChanges(CmsObject cms, String resource) throws CmsException{
        if(!cms.accessWrite(resource)){
            throw new CmsException(resource, CmsException.C_NO_ACCESS);
        }
        CmsFile file = cms.readFile(resource);
        cms.doUndoChanges(resource);
        String bodyPath = checkBodyPath(cms, (CmsFile)file);
        if (bodyPath != null){
            cms.doUndoChanges(bodyPath);
        }
        // linkmanagement: create the links of the restored page
        CmsPageLinks linkObject = cms.getPageLinks(file.getAbsolutePath());
        cms.createLinkEntrys(linkObject.getResourceId(), linkObject.getLinkTargets());
    }

    /**
    * Unlocks a resource.
    * <br>
    * A user can unlock a resource, so other users may lock this file.
    *
    * @param resource the complete path to the resource to be unlocked.
    *
    * @exception CmsException if the user has not the rights
    * to unlock this resource.
    */
    public void unlockResource(CmsObject cms, String resource) throws CmsException{
        // First read the page file.
        CmsFile pageFile = cms.readFile(resource);

        CmsUser pageLocker = null;
        CmsUser bodyLocker = null;

        // Check any locks on th page file
        pageLocker = getLockedBy(cms, resource);
        CmsUser currentUser = cms.getRequestContext().currentUser();

        CmsResource bodyFile = null;
        String bodyPath = null;
        // Try to fetch the body file.
        try {
            bodyPath = readBodyPath(cms, pageFile);
            bodyFile = cms.readFileHeader(bodyPath);
        } catch(Exception e) {
            bodyPath = null;
            bodyFile = null;
        }

        cms.doUnlockResource(resource);

        if(bodyFile != null) {
            // Everything with the page file is ok. We have write access. XML is valid.
            // Body file could be determined and fetched.
            // Now check further body file details (is it locked already, WHO has locked it, etc.)
            bodyLocker = getLockedBy(cms, bodyPath);
            // Unlock the body, if neccessary
            //if((pageLocker == null || pageLocker.equals(currentUser)) && (bodyLocker != null)) {
                cms.doUnlockResource(bodyPath);
            //}
        }

        // Unlock the page file, if neccessary
        //if(pageLocker != null || bodyLocker == null) {
        //cms.doUnlockResource(resource);
        //}
    }

    /**
     * method to check get the real body path from the content file
     *
     * @param cms The CmsObject, to access the XML read file.
     * @param file File in which the body path is stored. This should really
     *      be a CmsFile object an not a file header. This won't be checked for
     *      performance reasons.
     */
    private String readBodyPath(CmsObject cms, CmsFile file)
        throws CmsException{
        CmsXmlControlFile hXml=new CmsXmlControlFile(cms, file);
        String body = "";
        try{
            body = hXml.getElementTemplate("body");
        } catch (CmsException exc){
            // could not read body
        }
        return body;
    }

    /**
     * method to check get the real body path from the content file
     *
     * @param cms The CmsObject, to access the XML read file.
     * @param file File in which the body path is stored.
     */
    private String checkBodyPath(CmsObject cms, CmsFile file) throws CmsException {
        String result =(C_CONTENTBODYPATH.substring(0, C_CONTENTBODYPATH.lastIndexOf("/")))+(file.getAbsolutePath());
        if (!result.equals(readBodyPath(cms, (CmsFile)file))){
            result = null;
        }
        return result;
    }

    private CmsUser getLockedBy(CmsObject cms, String filename) {
        CmsUser result = null;
        try {
            result = cms.lockedBy(filename);
            if(result.getId() == -1) {
                result = null;
            }
        } catch(Exception e) {
            result = null;
        }
        return result;
    }

      /**
       * This method changes the path of the body file in the xml conten file
       * if file type name is page
       *
       * @param cms The CmsObject
       * @param file The XML content file
       * @param bodypath the new XML content entry
       * @exception Exception if something goes wrong.
       */
      private void changeContent(CmsObject cms, String filename, String bodypath)
          throws CmsException {
          CmsFile file=cms.readFile(filename);
          CmsXmlControlFile hXml=new CmsXmlControlFile(cms, file);
          hXml.setElementTemplate("body", bodypath);
          hXml.write();
      }

    /**
       * This method checks if all nescessary folders are exisitng in the content body
       * folder and creates the missing ones. <br>
       * All page contents files are stored in the content body folder in a mirrored directory
       * structure of the OpenCms filesystem. Therefor it is nescessary to create the
       * missing folders when a new page document is createg.
       * @param cms The CmsObject
       * @param path The path in the CmsFilesystem where the new page should be created.
       * @exception CmsException if something goes wrong.
       */
     private void checkFolders(CmsObject cms, String path)
          throws CmsException {

          String completePath=C_CONTENTBODYPATH;
          StringTokenizer t=new StringTokenizer(path,"/");
          String correspFolder = "/";
          // check if all folders are there
          while (t.hasMoreTokens()) {
              String foldername=t.nextToken();
              correspFolder = correspFolder+foldername+"/";
               try {
                // try to read the folder. if this fails, an exception is thrown

                cms.readFolder(completePath+foldername+"/");
              } catch (CmsException e) {
                  // the folder could not be read, so create it.
                  String orgFolder=completePath+foldername+"/";
                  orgFolder=orgFolder.substring(C_CONTENTBODYPATH.length()-1);
                  CmsFolder newfolder=cms.doCreateFolder(completePath,foldername);
                  CmsFolder folder=cms.readFolder(orgFolder);
                  cms.doLockResource(newfolder.getAbsolutePath(),false);
                  cms.doChgrp(newfolder.getAbsolutePath(),cms.readGroup(folder).getName());
                  cms.doChmod(newfolder.getAbsolutePath(),folder.getAccessFlags());
                  cms.doChown(newfolder.getAbsolutePath(),cms.readOwner(folder).getName());
                  try{
                    CmsFolder correspondingFolder = cms.readFolder(correspFolder);
                    if(!correspondingFolder.isLocked()){
                        cms.doUnlockResource(newfolder.getAbsolutePath());
                    }
                  } catch (CmsException ex){
                    // unable to unlock folder if parent folder is locked
                  }
              }
              completePath+=foldername+"/";
          }
     }

    /**
     * Set the access flags of the copied resource to the default values.
     * @param cms The CmsObject.
     * @param filename The name of the file.
     * @exception Throws CmsException if something goes wrong.
     */
    private void setDefaultFlags(CmsObject cms, String filename)
        throws CmsException {

        Hashtable startSettings=null;
        Integer accessFlags=null;
        startSettings=(Hashtable)cms.getRequestContext().currentUser().getAdditionalInfo(C_ADDITIONAL_INFO_STARTSETTINGS);
        if (startSettings != null) {
            accessFlags=(Integer)startSettings.get(C_START_ACCESSFLAGS);
        }
        if (accessFlags == null) {
            accessFlags = new Integer(C_ACCESS_DEFAULT_FLAGS);
        }
        chmod(cms, filename, accessFlags.intValue(), false);
    }

    /**
     * Changes the project-id of the resource to the new project
     * for publishing the resource directly
     *
     * @param newProjectId The Id of the new project
     * @param resourcename The name of the resource to change
     */
    public void changeLockedInProject(CmsObject cms, int newProjectId, String resourcename)
        throws CmsException{
        CmsFile file = cms.readFile(resourcename, true);
        cms.doChangeLockedInProject(newProjectId, resourcename);
        String bodyPath = checkBodyPath(cms, (CmsFile)file);
        if (bodyPath != null){
            cms.doChangeLockedInProject(newProjectId, bodyPath);
        }

        // The page file contains XML.
        // So there could be some data in the parser's cache.
        // Clear it!
        String currentProject = cms.getRequestContext().currentProject().getName();
        CmsXmlControlFile.clearFileCache(currentProject + ":" + resourcename);
    }
}
