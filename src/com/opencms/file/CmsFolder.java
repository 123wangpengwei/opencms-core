/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/Attic/CmsFolder.java,v $
* Date   : $Date: 2003/08/01 13:57:22 $
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
 * Describes a folder in the Cms.
 *
 * @author Michael Emmerich
 * @version $Revision: 1.26 $ $Date: 2003/08/01 13:57:22 $
 */
public class CmsFolder extends CmsResource implements Cloneable, Serializable, Comparable {

   /**
    * Constructor, creates a new CmsFolder object.<p>
    *
    * @param structureId the id of the structure record
    * @param resourceId the database Id
    * @param parentId the database Id of the parent folder
    * @param fileId the id of the content
    * @param resourceName the name (including complete path) of the resouce
    * @param resourceType the type of this resource
    * @param resourceFlags the flags of the resource
    * @param projectId the project id this resource belongs to
    * @param state the state of this resource
    * @param dateCreated the creation date of this resource
    * @param createdByUser the user who created the file
    * @param dateLastModified the date of the last modification of the resource
    * @param lastModifiedByUser the user who changed the file
    */
    public CmsFolder(
        CmsUUID structureId,
        CmsUUID resourceId,
        CmsUUID parentId,
        CmsUUID fileId,
        String resourceName,
        int resourceType,
        int resourceFlags,
        int projectId,
        int state,
        long dateCreated,
        CmsUUID createdByUser,
        long dateLastModified,
        CmsUUID lastModifiedByUser,
        int linkCount
    ) {             
        super(
            structureId,
            resourceId,
            parentId,
            fileId,
            resourceName,
            resourceType,
            resourceFlags,
            projectId,
            state,
            -1,
            dateCreated,
            createdByUser,
            dateLastModified,
            lastModifiedByUser,
            -1,
            linkCount);
    }
 
    /**
    * Clones the CmsFolder by creating a new CmsFolder.
    * @return Cloned CmsFolder.
    */
    public Object clone() {
        return new CmsFolder(
            this.getId(),
            this.getResourceId(),
            this.getParentId(),
            this.getFileId(),
            new String(this.getResourceName()),
            this.getType(),
            this.getFlags(),
            this.getProjectId(),
            this.getState(),
            this.getDateCreated(),
            this.getUserCreated(),
            this.getDateLastModified(),
            this.getUserLastModified(),
            this.getLinkCount());
    }
}
