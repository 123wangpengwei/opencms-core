/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/file/CmsFolder.java,v $
 * Date   : $Date: 2004/10/31 21:30:18 $
 * Version: $Revision: 1.13 $
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

import org.opencms.loader.CmsLoaderException;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsUUID;

import java.io.Serializable;

/**
 * A folder object in OpenCms.<p>
 * 
 * A folder object is a CmsResource object that can contain sub-resources.<p>
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * @author Michael Emmerich (m.emmerich@alkacon.com)
 * 
 * @version $Revision: 1.26 
 */
public class CmsFolder extends CmsResource implements Cloneable, Serializable, Comparable {

    /**
     * Constructor, creates a new CmsFolder Object from the given CmsResource.<p> 
     * 
     * @param resource the base resource object to create a folder from
     */
    public CmsFolder(CmsResource resource) {

        this(
            resource.getStructureId(),
            resource.getResourceId(),
            resource.getRootPath(),
            resource.getTypeId(),
            resource.getFlags(),
            resource.getProjectLastModified(),
            resource.getState(),
            resource.getDateCreated(),
            resource.getUserCreated(),
            resource.getDateLastModified(),
            resource.getUserLastModified(),
            resource.getSiblingCount(),
            resource.getDateReleased(),
            resource.getDateExpired());
    }

    /**
     * Constructor, creates a new CmsFolder object.<p>
     * @param structureId the id of this resources structure record
     * @param resourceId the id of this resources resource record
     * @param path the filename of this resouce
     * @param type the type of this resource
     * @param flags the flags of this resource
     * @param projectId the project id this resource was last modified in
     * @param state the state of this resource
     * @param dateCreated the creation date of this resource
     * @param userCreated the id of the user who created this resource
     * @param dateLastModified the date of the last modification of this resource
     * @param userLastModified the id of the user who did the last modification of this resource    * @param size the size of the file content of this resource
     * @param linkCount the count of all siblings of this resource 
     * @param dateReleased the release date of this resource
     * @param dateExpired the expiration date of this resource
     */
    public CmsFolder(
        CmsUUID structureId,
        CmsUUID resourceId,
        String path,
        int type,
        int flags,
        int projectId,
        int state,
        long dateCreated,
        CmsUUID userCreated,
        long dateLastModified,
        CmsUUID userLastModified,
        int linkCount,
        long dateReleased,
        long dateExpired) {

        super(
            structureId,
            resourceId,
            path,
            type,
            true,
            flags,
            projectId,
            state,
            dateCreated,
            userCreated,
            dateLastModified,
            userLastModified,
            dateReleased,
            dateExpired,
            linkCount,
            -1);
    }

    /**
     * Returns <code>true</code> if the given resource type id describes a folder type.<p>
     * 
     * @param typeId the resource type id to check 
     * 
     * @return true if the given resource type id describes a folder type
     */
    public static final boolean isFolderType(int typeId) {

        try {
            return OpenCms.getResourceManager().getResourceType(typeId).isFolder();
        } catch (CmsLoaderException e) {
            throw new RuntimeException("Unable to resolve resource type: " + typeId, e);
        }
    }

    /**
     * Returns <code>true</code> if the given resource type name describes a folder type.<p>
     * 
     * @param typeName the resource type name to check 
     * 
     * @return true if the given resource type name describes a folder type
     */
    public static final boolean isFolderType(String typeName) {

        try {
            return OpenCms.getResourceManager().getResourceType(typeName).isFolder();
        } catch (CmsLoaderException e) {
            throw new RuntimeException("Unable to resolve resource type: " + typeName, e);
        }
    }

    /**
     * Returns a clone of this Objects instance.<p>
     * 
     * @return a clone of this instance
     */
    public Object clone() {

        CmsResource clone = new CmsFolder(
            getStructureId(),
            getResourceId(),
            getRootPath(),
            getTypeId(),
            getFlags(),
            getProjectLastModified(),
            getState(),
            getDateCreated(),
            getUserCreated(),
            getDateLastModified(),
            getUserLastModified(),
            getSiblingCount(),
            getDateReleased(),
            getDateExpired());

        if (isTouched()) {
            clone.setDateLastModified(getDateLastModified());
        }

        return clone;
    }

    /**
     * @see org.opencms.file.CmsResource#getLength()
     */
    public int getLength() {

        return -1;
    }

    /**
     * @see org.opencms.file.CmsResource#isFile()
     */
    public boolean isFile() {

        return false;
    }

    /**
     * @see org.opencms.file.CmsResource#isFolder()
     */
    public boolean isFolder() {

        return true;
    }
}