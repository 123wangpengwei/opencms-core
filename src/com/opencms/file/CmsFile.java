/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/Attic/CmsFile.java,v $
* Date   : $Date: 2003/07/16 16:34:49 $
* Version: $Revision: 1.26 $
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

import com.opencms.flex.util.CmsUUID;

import java.io.Serializable;

/**
 * Describes a file in the Cms.
 *
 * @author Michael Emmerich
 * @version $Revision: 1.26 $ $Date: 2003/07/16 16:34:49 $
 */
public class CmsFile extends CmsResource implements Cloneable, Serializable, Comparable {

    /*
     * The content of the file.
     */
    private byte[] m_fileContent;
    
    /*
     * The date of the last change.
     */
    private long m_dateLastModified;
    
    /*
     * The user who did the last change 
     */
    private CmsUUID m_lastModifiedByUser;

   /**
    * Constructor, creates a new CmsFile object.<p>
    *
    * @param structureId the id of the structure record
    * @param resourceId the id of the resource record
    * @param parentId the id of the parent folder
    * @param fileId the id of the content
    * @param resourceName the name (including complete path) of the resouce
    * @param resourceType the type of this resource
    * @param resourceFlags the flags of this resource
    * @param projectId the project id this resource belongs to.
    * @param accessFlags the access flags of this resource
    * @param state the state of this resource
    * @param lockedByUser the user id of the user who has locked this resource
    * @param launcherType the launcher that is used to process this recource
    * @param launcherClassname the name of the java class invoked by the launcher
    * @param dateCreated the creation date of this resource
    * @param createdByUser the id of the user who created this resource
    * @param dateLastModified the date of the last modification of the resource
    * @param lastModifiedByUser the id of the user who did the last modification
    * @param fileContent the content of the file
    * @param size the size of the file content
    * @param lockedInProject the id of the project the resource is locked in
    * @param vfsLinkType the link type
    */
    public CmsFile(
        CmsUUID structureId,
        CmsUUID resourceId,
        CmsUUID parentId,
        CmsUUID fileId,
        String resourceName,
        int resourceType,
        int resourceFlags,
        int projectId,
        int accessFlags,
        int state,
        CmsUUID lockedByUser,
        int launcherType,
        String launcherClassname,
        long dateCreated,
        CmsUUID createdByUser,
        long dateLastModified,
        CmsUUID lastModifiedByUser,
        byte[] fileContent,
        int size,
        int lockedInProject,
        int vfsLinkType
    ) {
        // create the CmsResource.
        super(
            structureId, 
            resourceId, 
            parentId, 
            fileId, 
            resourceName, 
            resourceType, 
            resourceFlags, 
            projectId, 
            accessFlags, 
            state, 
            lockedByUser, 
            launcherType, 
            launcherClassname, 
            dateCreated, 
            createdByUser, 
            dateLastModified, 
            lastModifiedByUser, 
            size, 
            lockedInProject, 
            vfsLinkType);
    
        // set content and size.
        m_fileContent = fileContent;
        
        // set date and user of last modification
        m_dateLastModified = dateLastModified;
        m_lastModifiedByUser = lastModifiedByUser;
    }
 
    /**
    * Clones the CmsFile by creating a new CmsFolder.
    * @return Cloned CmsFile.
    */
    public Object clone() {
        
        byte[] newContent = new byte[this.getContents().length];
        System.arraycopy(getContents(), 0, newContent, 0, getContents().length);

        CmsFile clone = new CmsFile(
            this.getId(),
            this.getResourceId(),
            this.getParentId(),
            this.getFileId(),
            new String(this.getResourceName()),
            this.getType(),
            this.getFlags(),
            this.getProjectId(),
            this.getAccessFlags(),
            this.getState(),
            this.isLockedBy(),
            this.getLauncherType(),
            new String(this.getLauncherClassname()),
            this.getDateCreated(),
            this.getUserCreated(),
            super.getDateLastModified(),    // set to resource data
            super.getUserLastModified(),    // set to resource data
            newContent,
            this.getLength(),
            this.getLockedInProject(),
            this.getVfsLinkType());
            
        clone.setDateLastModified(this.m_dateLastModified);
        clone.setUserLastModified(this.m_lastModifiedByUser);
        
        return clone;
    }
    
    /**
     * Gets the content of this file.
     *
     * @return the content of this file.
     */
    public byte[] getContents() {
      return m_fileContent;
    }
    /**
     * Gets the file-extension.
     *
     * @return the file extension. If this file has no extension, it returns
     * a empty string ("")
     */
    public String getExtension() {
        String name=null;
        String extension="";
        int dot;

        name=this.getResourceName();
        // check if this file has an extension.
        dot=name.lastIndexOf(".");
        if (dot> 0) {
            extension=name.substring(dot, name.length());
        }
        return extension;
    }
    
    /**
     * Sets the content of this file.
     *
     * @param value the content of this file.
     */
    public void setContents(byte[] value) {
        m_fileContent=value;
        if (m_fileContent.length >0) {
            m_size=m_fileContent.length;
        }
    }

    /**
     * Sets the user id of the user who changed the content in this file.<p>
     *
     * @param resourceLastModifiedByUserId the user id of the user who changed the resource
     */
    void setContentUserLastModified(CmsUUID resourceLastModifiedByUserId) {
        m_lastModifiedByUser = resourceLastModifiedByUserId;
    }
    
    /**
     * Gets the user id of the user who changed the content in this file.<p>
     */
    public CmsUUID getContentUserLastModified() {
        return m_lastModifiedByUser;    
    }
    
    /**
     * Returns the user id of the user who made the last change on this resource.<p>
     * Note: This is the user who did the latest change on the structure or the resource record.
     *
     * @return the user id of the user who made the last change<p>
     */
    public CmsUUID getUserLastModified() {
        if (super.getDateLastModified() > this.getContentDateLastModified())
            return super.getUserLastModified();
        else
            return this.getContentUserLastModified();    
    }

    /**
    * Sets the date of the last modification of the content of this resource.<p>
    * 
    * @param time the date to set
     */
    public void setContentDateLastModified(long time) {
       m_dateLastModified = time;
    }
    
    /**
     * Returns the date of the last change of the content of this resource.<p>
     */
    public long getContentDateLastModified() {
       return m_dateLastModified; 
    }
    
    /**
     * Returns the date of the last modification of this resource.<p>
     * Note: This is the latest date of either the date of the last change of the structure or the resource record.
     *
     * @return the date of the last modification of this resource
     */
     public long getDateLastModified() {
         return (super.getDateLastModified() > this.getContentDateLastModified()) ? super.getDateLastModified() : this.getContentDateLastModified();
     }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer output = new StringBuffer(super.toString());
        output.append(" : Content date lastmodified=");
        output.append(new java.util.Date(getContentDateLastModified()));
        output.append(" : Content user lastmodified=");
        output.append(getContentUserLastModified());
        return output.toString();        
    }    
}
