package com.opencms.file;

/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/Attic/CmsBackupResource.java,v $
 * Date   : $Date: 2001/07/09 08:34:54 $
 * Version: $Revision: 1.1 $
 *
 * Copyright (C) 2000  The OpenCms Group
 *
 * This File is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For further information about OpenCms, please see the
 * OpenCms Website: http://www.opencms.com
 *
 * You should have received a copy of the GNU General Public License
 * long with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.io.*;

/**
 * This class describes a backup resource in the Cms.
 *
 * @author Edna Falkenhan
 * @version $Revision: 1.1 $ $Date: 2001/07/09 08:34:54 $
 */
public class CmsBackupResource extends CmsResource implements Cloneable,Serializable {

	/**
	 * The id of the version.
	 */
	private int m_versionId = C_UNKNOWN_ID;

	/**
	 * The name of the owner.
	 */
	private String m_ownerName = "";

	/**
	 * The name of the group.
	 */
	private String m_groupName = "";

	/**
	 * The name of the last user who had modified the resource.
	 */
	private String m_lastModifiedByName = "";

    /**
     * The content of the file
     */
    private byte[] m_fileContent;

	 /**
	  * Constructor, creates a new CmsBackupResource object.
	  *
	  * @param versionId The versionId of the resource
      * @param resourceId The database Id.
	  * @param parentId The database Id of the parent folder.
	  * @param fileId The id of the content.
	  * @param resourceName The name (including complete path) of the resouce.
	  * @param resourceType The type of this resource.
	  * @param rescourceFlags The flags of thei resource.
	  * @param userId The id of the user of this resource.
      * @param userName The name of the user of this resource.
	  * @param groupId The id of the group of this resource.
      * @param groupName The name of the group of this resource.
	  * @param projectId The project id this resource belongs to.
	  * @param accessFlags The access flags of this resource.
	  * @param state The state of this resource.
	  * @param lockedBy The user id of the user who has locked this resource.
	  * @param launcherType The launcher that is require to process this recource.
	  * @param launcherClassname The name of the Java class invoked by the launcher.
	  * @param dateCreated The creation date of this resource.
	  * @param dateLastModified The date of the last modification of the resource.
	  * @param fileContent Then content of the file.
	  * @param resourceLastModifiedBy The user who changed the file.
      * @param lastModifiedByName The name of user who changed the file.
	  * @param size The size of the file content.
	  */
	 public CmsBackupResource(int versionId, int resourceId, int parentId, int fileId,
						      String resourceName, int resourceType, int resourceFlags,
                              int user, String userName, int group, String groupName,
                              int projectId, int accessFlags, int state,
						      int launcherType, String launcherClassname,
						      long dateCreated, long dateLastModified,
						      int resourceLastModifiedBy, String lastModifiedByName,
						      byte[] fileContent,int size){

		// create the CmsResource.
		super(resourceId, parentId, fileId,
			  resourceName,resourceType,resourceFlags,
			  user,group,projectId,
			  accessFlags,state,C_UNKNOWN_ID,
			  launcherType,launcherClassname,
			  dateCreated,dateLastModified,
			  resourceLastModifiedBy,size);

		// set content and size.
		m_fileContent=fileContent;

        // set version id
        m_versionId = versionId;

        // set owner name
        m_ownerName = userName;

        // set group name
        m_groupName = groupName;

        // set lastModifiedByName
        m_lastModifiedByName = lastModifiedByName;

   }
	/**
	* Clones the CmsFile by creating a new CmsFolder.
	* @return Cloned CmsFile.
	*/
	public Object clone() {
		byte[] newContent = new byte[ this.getContents().length ];
		System.arraycopy(getContents(), 0, newContent, 0, getContents().length);

		return new CmsBackupResource(this.getVersionId(), this.getResourceId(), this.getParentId(),
                                     this.getFileId(), new String(this.getAbsolutePath()),
                                     this.getType(), this.getFlags(), this.getOwnerId(),
                                     this.getOwnerName(), this.getGroupId(), this.getGroupName(),
                                     this.getProjectId(), this.getAccessFlags(), this.getState(),
                                     this.getLauncherType(),
							         new String(this.getLauncherClassname()), this.getDateCreated(),
                                     this.getDateLastModified(),this.getResourceLastModifiedBy(),
                                     this.getLastModifiedByName(),
                                     newContent, this.getLength());
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
	 * Gets the version id of this resource.
	 *
	 * @return the version id of this resource.
	 */
	public int getVersionId() {
	  return m_versionId;
	}

    /**
	 * Gets the name of the owner of this resource.
	 *
	 * @return the name of the owner of this resource.
	 */
	public String getOwnerName() {
	  return m_ownerName;
	}

    /**
	 * Gets the name of the group of this resource.
	 *
	 * @return the name of the group of this resource.
	 */
	public String getGroupName() {
	  return m_groupName;
	}

    /**
	 * Gets the name of the user who changed this resource.
	 *
	 * @return the name of the user who changed this resource.
	 */
	public String getLastModifiedByName() {
	  return m_lastModifiedByName;
	}

	/**
	 * Gets the file-extension.
	 *
	 * @return the file extension. If this file has no extension, it returns
	 * a empty string ("").
	 */
	public String getExtension(){
		String name=null;
		String extension="";
		int dot;

		name=this.getName();
		// check if this file has an extension.
		dot=name.lastIndexOf(".");
		if (dot> 0) {
			extension=name.substring(dot,name.length());
		}
		return extension;
	}
}
