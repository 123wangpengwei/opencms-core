/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/Attic/CmsFile.java,v $
 * Date   : $Date: 2003/08/29 16:12:04 $
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

package com.opencms.file;

import com.opencms.flex.util.CmsUUID;

import java.io.Serializable;

/**
 * A file object in OpenCms.<p>
 * 
 * A file object is a CmsResource that contains an additional byte[] array 
 * of binary data, which is the file content. 
 * A file object is not allowed to have sub-resources.<p>
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * @author Michael Emmerich (m.emmerich@alkacon.com)
 * 
 * @version $Revision: 1.34 $
 */
public class CmsFile extends CmsResource implements Cloneable, Serializable, Comparable {

    /** The content of this file */
    private byte[] m_fileContent;
    
    /**
     * Constructor, creates a new CmsFile Object from the given CmsResource with 
     * an empty byte array as file content.<p>
     * 
     * @param resource the base resource object to create a file from
     */
    public CmsFile(CmsResource resource) {
        this(
            resource.getId(),
            resource.getResourceId(),
            resource.getParentId(),
            resource.getFileId(),
            resource.getResourceName(),
            resource.getType(),
            resource.getFlags(),
            resource.getProjectId(),
            resource.getState(),
            resource.getLoaderId(),
            resource.getDateCreated(),
            resource.getUserCreated(),
            resource.getDateLastModified(),
            resource.getUserLastModified(),
            0,            
            resource.getLinkCount(), 
            new byte[0]
        );
        if (resource.hasFullResourceName()) {
            setFullResourceName(resource.getFullResourceName());
        }
    }
    
   /**
    * Constructor, creates a new CmsFile object.<p>
    *
    * @param structureId the id of the structure record
    * @param resourceId the id of the resource record
    * @param parentId the id of the parent folder
    * @param fileId the id of the content
    * @param name the name (including complete path) of the resouce
    * @param type the type of this resource
    * @param flags the flags of this resource
    * @param projectId the project id this resource belongs to.
    * @param state the state of this resource
    * @param loaderId the loader that is used to process this recource
    * @param dateCreated the creation date of this resource
    * @param userCreated the id of the user who created this resource
    * @param dateLastModified the date of the last modification of the resource
    * @param userLastModified the id of the user who did the last modification
    * @param size the size of the file content
    * @param linkCount the count of vfs links
    * @param content the content of the file
    */
    public CmsFile(
        CmsUUID structureId,
        CmsUUID resourceId,
        CmsUUID parentId,
        CmsUUID fileId,
        String name,
        int type,
        int flags,
        int projectId,
        int state,
        int loaderId,
        long dateCreated,
        CmsUUID userCreated,
        long dateLastModified,
        CmsUUID userLastModified,
        int size,
        int linkCount,
        byte[] content
    ) {
        // create the CmsResource.
        super(
            structureId, 
            resourceId, 
            parentId, 
            fileId, 
            name, 
            type, 
            flags, 
            projectId, 
            state, 
            loaderId, 
            dateCreated, 
            userCreated, 
            dateLastModified, 
            userLastModified, 
            size, 
            linkCount);
    
        // set content and size.
        m_fileContent = content;
    }
    
    /**
     * Clones this CmsFile.<p>
     * 
     * @return the colned CmsFile
     */
    public Object clone() {
        
        byte[] newContent = new byte[this.getContents().length];
        System.arraycopy(getContents(), 0, newContent, 0, getContents().length);

        CmsFile clone = new CmsFile(
            getId(),
            getResourceId(),
            getParentId(),
            getFileId(),
            getResourceName(),
            getType(),
            getFlags(),
            getProjectId(),
            getState(),
            getLoaderId(),
            getDateCreated(),
            getUserCreated(),
            getDateLastModified(),
            getUserLastModified(),
            getLength(),
            getLinkCount(),
            newContent);
            
        return clone;
    }
    
    /**
     * Returns the content of this file.<p>
     *
     * @return the content of this file.
     */
    public byte[] getContents() {
        return m_fileContent;
    }

    /**
     * Sets the contents of this file.<p>
     *
     * @param value the content of this file.
     */
    public void setContents(byte[] value) {
        m_fileContent = value;
        if (m_fileContent.length > 0) {
            m_length = m_fileContent.length;
        }
    } 
}
