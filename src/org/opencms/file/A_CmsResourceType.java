/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/file/Attic/A_CmsResourceType.java,v $
 * Date   : $Date: 2004/06/04 10:48:52 $
 * Version: $Revision: 1.10 $
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

package org.opencms.file;

import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsUUID;

import java.util.List;

import org.apache.commons.collections.ExtendedProperties;

/**
 * Base implementation for resource type classes.<p>
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.10 $
 * @since 5.1
 */
public abstract class A_CmsResourceType implements I_CmsResourceType {

    /**
     * @see org.opencms.configuration.I_CmsConfigurationParameterHandler#addConfigurationParameter(java.lang.String, java.lang.String)
     */
    public void addConfigurationParameter(String paramName, String paramValue) {
        // this configuration does not support parameters 
        if (OpenCms.getLog(this).isDebugEnabled()) {
            OpenCms.getLog(this).debug("addConfigurationParameter(" + paramName + ", " + paramValue + ") called on " + this);
        }            
    }        

    /**
     * @see org.opencms.file.I_CmsResourceType#changeLockedInProject(org.opencms.file.CmsObject, int, java.lang.String)
     */
    public void changeLockedInProject(CmsObject cms, int project, String resourcename) throws CmsException {
        cms.doChangeLockedInProject(resourcename);
    }

    /**
     * @see org.opencms.file.I_CmsResourceType#chtype(org.opencms.file.CmsObject, java.lang.String, int)
     */
    public void chtype(CmsObject cms, String resourcename, int newtype) throws CmsException {
        cms.doChtype(resourcename, newtype);
    }

    /**
     * @see org.opencms.file.I_CmsResourceType#copyResource(org.opencms.file.CmsObject, java.lang.String, java.lang.String, boolean, boolean, int)
     */
    public void copyResource(CmsObject cms, String resourcename, String destination, boolean keeppermissions, boolean lockCopy, int copyMode) throws CmsException {
        cms.doCopyFile(resourcename, destination, lockCopy, copyMode);
    }

    /**
     * @see org.opencms.file.I_CmsResourceType#copyResourceToProject(org.opencms.file.CmsObject, java.lang.String)
     */
    public void copyResourceToProject(CmsObject cms, String resourcename) throws CmsException {
        cms.doCopyResourceToProject(resourcename);
    }

    /**
     * @see org.opencms.file.I_CmsResourceType#copyToLostAndFound(org.opencms.file.CmsObject, java.lang.String, boolean)
     */
    public String copyToLostAndFound(CmsObject cms, String resourcename, boolean copyResource) throws CmsException {
        return cms.doCopyToLostAndFound(resourcename, copyResource);
    }

    /**
     * @see org.opencms.file.I_CmsResourceType#createResource(org.opencms.file.CmsObject, java.lang.String, List, byte[], java.lang.Object)
     */
    public abstract CmsResource createResource(CmsObject cms, String resourcename, List properties, byte[] contents, Object parameter) throws CmsException;

    /**
     * @see org.opencms.file.I_CmsResourceType#deleteResource(org.opencms.file.CmsObject, java.lang.String, int)
     */
    public void deleteResource(CmsObject cms, String resourcename, int deleteOption) throws CmsException {
        cms.doDeleteFile(resourcename, deleteOption);
    }

    /**
     * @see org.opencms.file.I_CmsResourceType#exportResource(org.opencms.file.CmsObject, org.opencms.file.CmsFile)
     */
    public CmsFile exportResource(CmsObject cms, CmsFile file) {
        // there are no link tags inside a simple resource
        return file;
    }
    
    /**
     * @see org.opencms.file.I_CmsResourceType#getCachePropertyDefault()
     */
    public String getCachePropertyDefault() {
        return null;
    }
    
    /**
     * @see org.opencms.configuration.I_CmsConfigurationParameterHandler#getConfiguration()
     */
    public ExtendedProperties getConfiguration() {
        // this configuration does not support parameters
        if (OpenCms.getLog(this).isDebugEnabled()) {
            OpenCms.getLog(this).debug("getConfiguration() called on " + this);
        }          
        return null;
    }
    
    /**
     * @see org.opencms.file.I_CmsResourceType#getLoaderId()
     */
    public abstract int getLoaderId();

    /**
     * @see org.opencms.file.I_CmsResourceType#getResourceType()
     */
    public abstract int getResourceType();

    /**
     * @see org.opencms.file.I_CmsResourceType#getResourceTypeName()
     */
    public abstract String getResourceTypeName();
    
    /**
     * @see org.opencms.file.I_CmsResourceType#importResource(org.opencms.file.CmsObject, org.opencms.file.CmsResource, byte[], List, java.lang.String)
     */
    public CmsResource importResource(CmsObject cms, CmsResource resource, byte[] content, List properties, String destination) throws CmsException {
        CmsResource importedResource = null;
        CmsResource existingResource = null;

        try {
            importedResource = cms.doImportResource(resource, content, properties, destination);
            cms.lockResource(destination);
        } catch (CmsException e) {
            if (e.getType() == CmsException.C_FILE_EXISTS) {
                // read the existing resource                
                existingResource = cms.readFileHeader(destination);
                
                // check if the resource uuids are the same, if so, update the content
                if (OpenCms.getImportExportManager().overwriteCollidingResources() || existingResource.getResourceId().equals(resource.getResourceId())) {
                    // resource with the same name and same uuid does exist, 
                    // update the existing resource
                    cms.lockResource(destination);
                    cms.doWriteResource(destination, properties, content);
                    importedResource = cms.readFileHeader(destination);
                    cms.touch(destination, resource.getDateLastModified(), false, resource.getUserLastModified());
                } else {
                    // a resource with the same name but different uuid does exist,
                    // copy the new resource to the lost+found folder 
                    String target = copyToLostAndFound(cms, destination, false);                             
                    CmsResource newRes = new CmsResource(resource.getStructureId(), resource.getResourceId(), resource.getParentStructureId(), resource.getFileId(),  CmsResource.getName(target), resource.getType(), resource.getFlags(), resource.getProjectLastModified(), resource.getState(), resource.getLoaderId(), resource.getDateLastModified(), resource.getUserLastModified(), resource.getDateCreated(), resource.getUserCreated(), resource.getDateReleased(), resource.getDateExpired(), 1, resource.getLength());                        
                    importedResource = cms.doImportResource(newRes, content, properties, target);
                }
            }
        }

        return importedResource;
    }
    
    
    /**
     * @see org.opencms.file.I_CmsResourceType#initConfiguration()
     */
    public void initConfiguration() throws CmsException {

        // most resource types do not require initialization
        if (OpenCms.getLog(this).isDebugEnabled()) {
            OpenCms.getLog(this).debug("initConfiguration called on " + this);
            if (this == null) {
                // this is just to supress a compiler warning
                throw new CmsException();
            }
        }
    }    

    /**
     * @see org.opencms.file.I_CmsResourceType#isDirectEditable()
     */
    public boolean isDirectEditable() {
        return false;
    }

    /**
     * @see org.opencms.file.I_CmsResourceType#lockResource(org.opencms.file.CmsObject, java.lang.String, boolean, int)
     */
    public void lockResource(CmsObject cms, String resourcename, boolean force, int mode) throws CmsException {
        cms.doLockResource(resourcename, mode);
    }

    /**
     * @see org.opencms.file.I_CmsResourceType#moveResource(org.opencms.file.CmsObject, java.lang.String, java.lang.String)
     */
    public void moveResource(CmsObject cms, String resourcename, String destination) throws CmsException {
        cms.doMoveResource(resourcename, destination);
    }

    /**
     * @see org.opencms.file.I_CmsResourceType#renameResource(org.opencms.file.CmsObject, java.lang.String, java.lang.String)
     */
    public void renameResource(CmsObject cms, String resourcename, String destination) throws CmsException {
        cms.doRenameResource(resourcename, destination);
    }

    /**
     * @see org.opencms.file.I_CmsResourceType#replaceResource(org.opencms.file.CmsObject, java.lang.String, List, byte[], int)
     */
    public void replaceResource(CmsObject cms, String resourcename, List properties, byte[] content, int type) throws CmsException {
        cms.doReplaceResource(resourcename, content, type, properties);
    }

    /**
     * @see org.opencms.file.I_CmsResourceType#restoreResource(org.opencms.file.CmsObject, int, java.lang.String)
     */
    public void restoreResource(CmsObject cms, int tag, String resourcename) throws CmsException {
        cms.doRestoreResource(tag, resourcename);
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer output = new StringBuffer();
        output.append("[ResourceType]:");
        output.append(getResourceTypeName());
        output.append(", Id=");
        output.append(getResourceType());
        output.append(", LoaderId=");
        output.append(getLoaderId());
        return output.toString();
    }

    /**
     * @see org.opencms.file.I_CmsResourceType#touch(CmsObject, String, long, boolean, CmsUUID)
     */
    public void touch(CmsObject cms, String resourcename, long timestamp, boolean recursive, CmsUUID user) throws CmsException {
        cms.doTouch(resourcename, timestamp, user);
    }

    /**
     * @see org.opencms.file.I_CmsResourceType#undeleteResource(org.opencms.file.CmsObject, java.lang.String)
     */
    public void undeleteResource(CmsObject cms, String resourcename) throws CmsException {
        cms.doUndeleteFile(resourcename);
    }

    /**
     * @see org.opencms.file.I_CmsResourceType#undoChanges(org.opencms.file.CmsObject, java.lang.String, boolean)
     */
    public void undoChanges(CmsObject cms, String resourcename, boolean recursive) throws CmsException {
        cms.doUndoChanges(resourcename);
    }

    /**
     * @see org.opencms.file.I_CmsResourceType#unlockResource(org.opencms.file.CmsObject, java.lang.String, boolean)
     */
    public void unlockResource(CmsObject cms, String resourcename, boolean recursive) throws CmsException {
        cms.doUnlockResource(resourcename);
    }
    
    /**
     * @see org.opencms.file.I_CmsResourceType#writeFile(org.opencms.file.CmsObject, org.opencms.file.CmsFile)
     */
    public CmsFile writeFile(CmsObject cms, CmsFile file) throws CmsException {
        cms.doWriteFile(file);
        return file;
    }
}
