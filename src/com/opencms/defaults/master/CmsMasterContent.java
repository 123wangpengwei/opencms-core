/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/defaults/master/Attic/CmsMasterContent.java,v $
* Date   : $Date: 2003/08/14 15:37:26 $
* Version: $Revision: 1.42 $
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

package com.opencms.defaults.master;

import org.opencms.loader.CmsXmlTemplateLoader;
import org.opencms.main.OpenCms;

import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.defaults.A_CmsContentDefinition;
import com.opencms.defaults.I_CmsExtendedContentDefinition;
import com.opencms.defaults.master.genericsql.CmsDbAccess;
import com.opencms.file.CmsObject;
import com.opencms.file.CmsResource;
import com.opencms.file.CmsUser;
import com.opencms.flex.util.CmsUUID;

import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This class is the master of several Modules. It carries a lot of generic
 * data-fileds which can be used for a special Module.
 *
 * The module creates a set of methods to support project-integration, history
 * and import - export.
 *
 * @author A. Schouten $
 * $Revision: 1.42 $
 * $Date: 2003/08/14 15:37:26 $
 */
public abstract class CmsMasterContent
    extends A_CmsContentDefinition
    implements I_CmsExtendedContentDefinition {

    /** The cms-object to get access to the cms-ressources */
    protected CmsObject m_cms = null;

    /** The dataset which holds all informations about this module */
    protected CmsMasterDataSet m_dataSet = null;

    /** Is set to true, if the lockstate changes */
    protected boolean m_lockstateWasChanged = false;

    /** A private HashMap to store all data access-objects. */
    private static HashMap c_accessObjects = new HashMap();

    /** Vector of currently selected channels */
    protected Vector m_selectedChannels = null;

    /** Vector of currently available channels */
    protected Vector m_availableChannels = null;

    /**
     * Registers a database access object for the contentdefinition type.
     * @param subId the id-type of the contentdefinition.
     * @param dBAccessObject the dBAccessObject that should be used to access
     * the databse.
     */
    protected static void registerDbAccessObject(int subId, CmsDbAccess dBAccessObject) {
        c_accessObjects.put(new Integer(subId), dBAccessObject);
    }

    /**
     * Returns a database access object for the contentdefinition type.
     * @param subId the id-type of the contentdefinition.
     * @return dBAccessObject the dBAccessObject that should be used to access
     * the databse.
     */
    protected static CmsDbAccess getDbAccessObject(int subId) {
        return (CmsDbAccess) c_accessObjects.get(new Integer(subId));
    }

    /**
     * Constructor to create a new contentdefinition. You can set data with your
     * set-Methods. After you have called the write-method this definition gets
     * a unique id.
     */
    public CmsMasterContent(CmsObject cms) {
        m_cms = cms;
        initValues();
    }

    /**
     * Constructor to create a new contentdefinition. You can set data with your
     * set-Methods. After you have called the write-method this definition gets
     * a unique id.
     */
    public CmsMasterContent(CmsObject cms, CmsMasterDataSet dataset) {
        this(cms);
        m_dataSet = dataset;
    }

    /**
     * Constructor to read a existing contentdefinition from the database. The
     * data read from the databse will be filled into the member-variables.
     * You can read them with the get- and modify them with the ser-methods.
     * Changes you have made must be written back to the database by calling
     * the write-method.
     * @param cms the cms-object for access to cms-resources.
     * @param id the master-id of the dataset to read.
     * @throws CmsException if the data couldn't be read from the database.
     */
    public CmsMasterContent(CmsObject cms, CmsUUID contentId) throws CmsException {
        m_cms = cms;
        initValues();
        getDbAccessObject(getSubId()).read(m_cms, this, m_dataSet, contentId);
    }
    
    /**
     * TESTFIX (a.kandzior@alkacon.com) New code:
     * Empty constructor needed for instanciating CDs as JavaBeans on JSPs.
     */    
    public CmsMasterContent() {}

    /**
     * This method initialises all needed members with default-values.
     */
    protected void initValues() {
        m_dataSet = new CmsMasterDataSet();
        m_dataSet.m_masterId = CmsUUID.getNullUUID();
        m_dataSet.m_subId = I_CmsConstants.C_UNKNOWN_ID;
        m_dataSet.m_lockedBy = CmsUUID.getNullUUID();
        m_dataSet.m_versionId = I_CmsConstants.C_UNKNOWN_ID;
        m_dataSet.m_userName = null;
        m_dataSet.m_groupName = null;
        m_dataSet.m_lastModifiedByName = null;
        m_dataSet.m_userId = CmsUUID.getNullUUID();
        setAccessFlags(I_CmsConstants.C_ACCESS_DEFAULT_FLAGS);
    }

    /**
     * Returns the title of this cd
     */
    public String getTitle() {
        return m_dataSet.m_title;
    }

    /**
     * Sets title of this cd
     */
    public void setTitle(String title) {
        m_dataSet.m_title = title;
    }

    /**
     * Returns a Vector of media-objects for this master cd.
     * @return a Vector of media-objects for this master cd.
     * @throws CmsException if the media couldn't be read.
     */
    public Vector getMedia() throws CmsException {
        if(m_dataSet.m_media == null) {
            // the media was not read yet
            // -> read them now from the db
            m_dataSet.m_media = getDbAccessObject(getSubId()).readMedia(m_cms, this);
        }
        return m_dataSet.m_media;
    }

    /**
     * Registers a new media, that should be written by calling write().
     * @param media - The mediaobject to register for writing.
     */
    public void addMedia(CmsMasterMedia media) {
        m_dataSet.m_mediaToAdd.add(media);
    }

    /**
     * Registers a media for deletion.
     * @param media - The mediaobject to register.
     */
    public void deleteMedia(CmsMasterMedia media) {
        m_dataSet.m_mediaToDelete.add(media);
    }

    /**
     * Registers a media for update
     * @param media - The mediaobject to register.
     */
    public void updateMedia(CmsMasterMedia media) {
        m_dataSet.m_mediaToUpdate.add(media);
    }

    /**
     * Returns a Vector of channels for this master cd.
     * @return a Vector of channel-names (String) for this master cd.
     * @throws CmsException if the channel couldn't be read.
     */
    public Vector getChannels() throws CmsException {
        if(m_dataSet.m_channel == null) {
            // the channels was not read yet
            // -> read them now from the db
            m_dataSet.m_channel = getDbAccessObject(getSubId()).readChannels(m_cms, this);
        }
        return m_dataSet.m_channel;
    }

    /**
     * Registers a new channel, that should be written by calling write().
     * @param channels - The channel to register for writing.
     */
    public void addChannel(String channel) {
        m_dataSet.m_channelToAdd.add(channel);
    }

    /**
     * Registers a channel for deletion.
     * @param channel - The channel to register for deleting.
     */
    public void deleteChannel(String channel) {
        m_dataSet.m_channelToDelete.add(channel);
    }

    /**
     * delete method
     * for delete instance of content definition
     * @param cms the CmsObject to use.
     */
    public void delete(CmsObject cms) throws Exception {
        getDbAccessObject(getSubId()).delete(m_cms, this, m_dataSet);
    }

    /**
     * change group method
     * for the permissions of content definition
     * @param cms the CmsObject to use.
     * @param group the id of the new group.
     */
    public void chgrp(CmsObject cms, CmsUUID group) throws Exception {
        m_dataSet.m_groupId = group;
        getDbAccessObject(getSubId()).changePermissions(m_cms, this, m_dataSet);
    }

    /**
     * change owner method
     * for the permissions of content definition
     * @param cms the CmsObject to use.
     * @param owner the id of the new owner.
     */
    public void chown(CmsObject cms, CmsUUID owner) throws Exception {
        m_dataSet.m_userId = owner;
        getDbAccessObject(getSubId()).changePermissions(m_cms, this, m_dataSet);
    }

    /**
     * change access flags method
     * for the permissions of content definition
     * @param cms the CmsObject to use.
     * @param accessflags the new access flags.
     */
    public void chmod(CmsObject cms, int accessflags) throws Exception {
        m_dataSet.m_accessFlags = accessflags;
        getDbAccessObject(getSubId()).changePermissions(m_cms, this, m_dataSet);
    }

    /**
     * copy method
     *
     * @param cms the CmsObject to use.
     * @return int The id of the new content definition
     */
    public CmsUUID copy(CmsObject cms) throws Exception {
        // insert the new cd with the copied dataset
        return getDbAccessObject(getSubId()).copy(cms, this, (CmsMasterDataSet)m_dataSet.clone(), this.getMedia(), this.getChannels());
    }

    /**
     * write method
     * to write the current content of the content definition to the database.
     * @param cms the CmsObject to use.
     */
    public void write(CmsObject cms) throws CmsException {
        // add or delete channels according to current selection
        updateChannels();
        // is this a new row or an existing row?
        if(m_dataSet.m_masterId.isNullUUID()) {
            // this is a new row - call the create statement
            getDbAccessObject(getSubId()).insert(m_cms, this, m_dataSet);
        } else {
            // this is a existing row - call the write statement
            if(m_lockstateWasChanged) {
                // update the locksyte
                getDbAccessObject(getSubId()).writeLockstate(m_cms, this, m_dataSet);
            } else {
                // write the changes to the database
                getDbAccessObject(getSubId()).write(m_cms, this, m_dataSet);
            }
        }
        // everything is written - so lockstate was updated
        m_lockstateWasChanged = false;
        // for next access to the media - clean them so they must be read again
        // from the db
        m_dataSet.m_media = null;
        m_dataSet.m_mediaToAdd = new Vector();
        m_dataSet.m_mediaToDelete = new Vector();
        m_dataSet.m_mediaToUpdate = new Vector();

        // for next access to the channels - clean them so they must be read again
        // from the db
        m_dataSet.m_channel = null;
        m_dataSet.m_channelToAdd = new Vector();
        m_dataSet.m_channelToDelete = new Vector();
    }

    /**
     * import method
     * to import the current content of the content definition to the database.
     */
    public void importMaster() throws Exception {
        getDbAccessObject(getSubId()).insert(m_cms, this, m_dataSet);
        // everything is written - so lockstate was updated
        m_lockstateWasChanged = false;
        // for next access to the media - clean them so they must be read again
        // from the db
        m_dataSet.m_media = null;
        m_dataSet.m_mediaToAdd = new Vector();
        m_dataSet.m_mediaToDelete = new Vector();
        m_dataSet.m_mediaToUpdate = new Vector();

        // for next access to the channels - clean them so they must be read again
        // from the db
        m_dataSet.m_channel = null;
        m_dataSet.m_channelToAdd = new Vector();
        m_dataSet.m_channelToDelete = new Vector();
    }

    /**
     * gets the unique Id of a content definition instance
     * @param cms the CmsObject to use.
     * @return a string with the Id
     */
    public String getUniqueId(CmsObject cms) {
        return getId() + "";
    }

    /**
     * gets the unique Id of a content definition instance
     * @param cms the CmsObject to use.
     * @return a int with the Id
     */
    public CmsUUID getId() {
        return m_dataSet.m_masterId;
    }

    /**
     * Gets the lockstate.
     * @return a int with the user who has locked the ressource.
     */
    public CmsUUID getLockstate() {        
//        CmsUUID lockedByUserId = CmsUUID.getNullUUID();
//        
//        try {
//            if(hasWriteAccess(m_cms)) {
//                lockedByUserId = m_dataSet.m_lockedBy;
//            }
//        } catch(CmsException exc) {
//            // NOOP
//        }
//        
//        return lockedByUserId;

        return m_dataSet.m_lockedBy;
    }

    /**
     * Sets the lockstates
     * @param the lockstate for the actual entry.
     */
    public void setLockstate(CmsUUID lockstate) {
        m_lockstateWasChanged = true;
        m_dataSet.m_lockedBy = lockstate;
    }

    /**
     * Gets the owner of this contentdefinition.
     */
    public CmsUUID getOwner() {
        return m_dataSet.m_userId;
    }

    /**
     * Gets the ownername
     */
    public String getOwnerName() {
        String retValue = m_dataSet.m_userId + "";
        if(m_dataSet.m_userName == null || "".equals(m_dataSet.m_userName.trim())) {
            try { // to read the real name of this user
                retValue = m_cms.readUser(m_dataSet.m_userId).getName();
            } catch(CmsException exc) {
                // ignore the exception - it was not possible to read the group
                // instead return the groupid
            }
        } else {
            // this is a history value - return it
            retValue = m_dataSet.m_userName;
        }
        return retValue;
    }

    /**
     * Sets the owner of this contentdefinition.
     */
    public void setOwner(CmsUUID id) {
        m_dataSet.m_userId = id;
    }

    /**
     * Gets the groupname
     */
    public String getGroup() {
        String retValue = m_dataSet.m_groupId + "";
        if(m_dataSet.m_groupName == null || "".equals(m_dataSet.m_groupName.trim())) {
            try { // to read the real name of this group
                retValue = m_cms.readGroup(m_dataSet.m_groupId).getName();
            } catch(CmsException exc) {
                // ignore the exception - it was not possible to read the group
                // instead return the groupid
            }
        } else {
            // this is historical data - return it
            retValue = m_dataSet.m_groupName;
        }
        return retValue;
    }

    /**
     * Gets the groupid
     */
    public CmsUUID getGroupId() {
        return m_dataSet.m_groupId;
    }

    /**
     * Sets the group.
     */
    public void setGroup(CmsUUID groupId) {
        m_dataSet.m_groupId = groupId;
    }

    /**
     * Returns the projectId of the content definition.
     * If the cd belongs to the current project the value
     * is the id of the current project otherwise its
     * the id of the online project
     *
     * @return int The project id
     */
    public int getProjectId() {
        return m_dataSet.m_projectId;
    }

    /**
     * Returns the state of the content definition:
     * unchanged, new, changed or deleted
     *
     * @return int The state of the cd
     */
    public int getState() {
        return m_dataSet.m_state;
    }

    /**
     * Returns the projectId of the content definition
     * that is stored in the cd table after the cd
     * was locked
     *
     * @return int The id of the cd
     */
    public int getLockedInProject() {
        return m_dataSet.m_lockedInProject;
    }

    /**
     * Returns the sub-id of this contentdefinition. You have to implement this
     * method so it returns a unique sunb-id that describes the type of the
     * contentdefinition. (E.g. article: sub-id=1; table: sub-id=2).
     */
    abstract public int getSubId();

    /**
     * Returns a String representation of this instance.
     * This can be used for debugging purposes.
     */
    public String toString() {
        StringBuffer returnValue = new StringBuffer();
        returnValue.append(this.getClass().getName() + "{");
        returnValue.append("UniqueId=" + getUniqueId(m_cms) + ";");
        returnValue.append("Lockstate=" + getLockstate() + ";");
        returnValue.append("AccessFlags=" + getAccessFlagsAsString() + ";");
        returnValue.append(m_dataSet.toString() + "}");
        return returnValue.toString();
    }

    /**
     * set the accessFlag for the CD
     * @param the accessFlag
     */
    public void setAccessFlags(int accessFlags) {
        m_dataSet.m_accessFlags = accessFlags;
    }

    /**
     * get the accessFlag for the CD
     * @return the accessFlag
     */
    public int getAccessFlags() {
        return m_dataSet.m_accessFlags;
    }

    /**
     * Convenience method to get the access-Flags as String representation.
     * @return String of access rights
     */
    public String getAccessFlagsAsString() {
//        int accessFlags = getAccessFlags();
        String str = "NOT YET";
        // TODO: reimplement using acl
        /*
        str += ((accessFlags & I_CmsConstants.C_PERMISSION_READ)>0?"r":"-");
        str += ((accessFlags & I_CmsConstants.C_PERMISSION_WRITE)>0?"w":"-");
        str += ((accessFlags & I_CmsConstants.C_PERMISSION_VIEW)>0?"v":"-");
        str += ((accessFlags & I_CmsConstants.C_ACCESS_GROUP_READ)>0?"r":"-");
        str += ((accessFlags & I_CmsConstants.C_ACCESS_GROUP_WRITE)>0?"w":"-");
        str += ((accessFlags & I_CmsConstants.C_ACCESS_GROUP_VISIBLE)>0?"v":"-");
        str += ((accessFlags & I_CmsConstants.C_ACCESS_PUBLIC_READ)>0?"r":"-");
        str += ((accessFlags & I_CmsConstants.C_ACCESS_PUBLIC_WRITE)>0?"w":"-");
        str += ((accessFlags & I_CmsConstants.C_ACCESS_PUBLIC_VISIBLE)>0?"v":"-");
        str += ((accessFlags & I_CmsConstants.C_ACCESS_INTERNAL_READ)>0?"i":"-");
        */
        return str;
    }

    /**
     * has the current user the right to view the CD
     * @return true if this cd is visible
     */
    public boolean isVisible() {
        CmsUser currentUser = m_cms.getRequestContext().currentUser();
        try {
            if(m_cms.isAdmin()) {
                return true;
            } else {
                if ( !accessOther(I_CmsConstants.C_ACCESS_PUBLIC_VISIBLE)
                    && !accessOwner(m_cms, currentUser, I_CmsConstants.C_PERMISSION_VIEW)
                    && !accessGroup(m_cms, currentUser, I_CmsConstants.C_ACCESS_GROUP_VISIBLE)) {
                    return false;
                } else {
                    return true;
                }
            }
        } catch(CmsException exc) {
            // no access to cms -> not visible
            return false;
        }
    }

    /**
     * returns true if the CD is readable for the current user
     * @return true if the cd is readable
     */
    public boolean isReadable() {
        try {
            if(m_cms.isAdmin()) {
                return true;
            } else {
                return hasReadAccess(m_cms);
            }
        } catch(CmsException exc) {
            // there was a cms-exception - no read-access!
            return false;
        }
    }

    /**
     * returns true if the CD is writeable for the current user
     * @return true if the cd is writeable
     */
    public boolean isWriteable() {
        try {
            if(m_cms.isAdmin()) {
                return true;
            } else {
                return this.hasWriteAccess(m_cms);
            }
        } catch(CmsException exc) {
            // there was a cms-exception - no write-access!
            return false;
        }
    }

    /**
     * Publishes the content definition directly
     *
     * @param cms The CmsObject
     */
    public void publishResource(CmsObject cms) throws Exception {
        Vector changedResources = new Vector();
        Vector changedModuleData = new Vector();
        int versionId = 0;
        long publishDate = System.currentTimeMillis();
        boolean enableHistory = cms.isHistoryEnabled();
        if (enableHistory){
            // Get the next version id
            versionId = cms.getBackupVersionId();
            // backup the current project
            cms.backupProject(cms.getRequestContext().currentProject().getId(), versionId, publishDate);
        }
        // now publish the content definition
        getDbAccessObject(getSubId()).publishResource(cms, m_dataSet, getSubId(), this.getClass().getName(),
        enableHistory, versionId, publishDate, changedResources, changedModuleData);
        // update the cache
        CmsXmlTemplateLoader.getOnlineElementCache().cleanupCache(changedResources, changedModuleData);
        OpenCms.fireCmsEvent(cms, com.opencms.flex.I_CmsEventListener.EVENT_PUBLISH_BO_RESOURCE, new HashMap(0));
    }

    /**
     * Undelete method
     * for undelete instance of content definition
     *
     * @param cms The CmsObject
     */
    public void undelete(CmsObject cms) throws Exception {
        getDbAccessObject(getSubId()).undelete(m_cms, this, m_dataSet);
    }
    
    /**
     * Overwrite this method in your subclasses to execute any tasks before the resources are published.
     */
    public static boolean beforePublish( CmsObject cms, Boolean enableHistory,
        Integer projectId, Integer versionId, Long publishingDate,
        Vector changedRessources, Vector changedModuleData, CmsMasterDataSet dataset ) {
        
        if (false && ((cms == null) && (enableHistory == null) && (projectId == null) &&
            (versionId == null) && (publishingDate == null) && (changedRessources == null) &&
            (changedModuleData == null) && (dataset == null))) {
            // silly test so that eclipse does not complain about unused method parameters
        }
        
        return true;
    }

    /**
     * Publishes all modified content definitions for this project.
     * @param cms The CmsObject
     * @param enableHistory set to true if backup tables should be filled.
     * @param projectId the Project that should be published.
     * @param versionId the versionId to save in the backup tables.
     * @param publishingDate the date and time of this publishing process.
     * @param subId the subId to publish cd's for.
     * @param contentDefinitionClassName the name of cd-class.
     * @param changedRessources a Vector of Ressources that were changed by this
     * publishing process.
     * @param changedModuleData a Vector of Ressource that were changed by this
     * publishing process. New published data will be add to this Vector to
     * return it.
     */
    protected static void publishProject(CmsObject cms, boolean enableHistory,
        int projectId, int versionId, long publishingDate, int subId,
        String contentDefinitionClassName, Vector changedRessources,
        Vector changedModuleData) throws CmsException {

         // now publish the project
         getDbAccessObject(subId).publishProject(cms, enableHistory, projectId,
             versionId, publishingDate, subId, contentDefinitionClassName,
             changedRessources, changedModuleData );
    }

    /**
     * Returns a Vector with the datasets of the contentdefinitions in the given channel.
     * @param subId the id-type of the contentdefinition.
     * @param channelId the id of the channel.
     * @return Vector the vector that includes the datasets
     */
    protected static Vector readAllByChannel(CmsObject cms, int channelId, int subId) throws CmsException{
        return getDbAccessObject(subId).readAllByChannel(cms, channelId, subId);
    }

    /**
     * Returns the date of the last modification of the content definition
     *
     * @return long The date of the last modification
     */
    public long getDateLastModified() {
        return m_dataSet.m_dateLastModified;
    }

    /**
     * Returns the date of the creation of the content definition
     *
     * @return long The date of the creation
     */
    public long getDateCreated() {
        return m_dataSet.m_dateCreated;
    }

    /**
     * Returns the id of the user who has modified the content definition
     *
     * @return int The id of the user who has modified the cd
     */
    public CmsUUID getLastModifiedBy() {
        return m_dataSet.m_lastModifiedBy;
    }

    /**
     * Returns the name of the user who has modified the content definition
     *
     * @return String The name of the user who has modified the cd
     */
    public String getLastModifiedByName() {
        String retValue = m_dataSet.m_lastModifiedBy + "";
        if(m_dataSet.m_lastModifiedByName == null) {
            try {
                retValue = m_cms.readUser(m_dataSet.m_lastModifiedBy).getName();
            } catch(CmsException exc) {
                // ignore this exception, return the id instead
            }
        } else {
            retValue = m_dataSet.m_lastModifiedByName;
        }
        return retValue;
    }

    /**
     * Returns the id of the version in the history of the content definition
     *
     * @return int The id of the version, or -1 if there is no version-info
     */
    public int getVersionId() {
        return m_dataSet.m_versionId;
    }

    /**
     * Restore method
     * for restore instance of content definition from the history
     *
     * @param cms The CmsObject
     * @param versionId The id of the version to restore
     */
    public void restore(CmsObject cms, int versionId) throws Exception {
        getDbAccessObject(this.getSubId()).restore(cms, this, m_dataSet, versionId);
    }

    /**
     * History method
     * returns the vector of the versions of content definition in the history
     *
     * @param cms The CmsObject
     * @return Vector The versions of the cd in the history
     */
    public Vector getHistory(CmsObject cms) throws Exception {
        return getDbAccessObject(this.getSubId()).getHistory(cms, this.getClass(), m_dataSet.m_masterId, this.getSubId());
    }

    /**
     * History method
     * returns the cd of the version with the given versionId
     *
     * @param cms The CmsObject
     * @param versionId The version id
     * @return Object The object with the version of the cd
     */
    public Object getVersionFromHistory(CmsObject cms, int versionId) throws Exception{
        return getDbAccessObject(this.getSubId()).getVersionFromHistory(cms, this.getClass(), m_dataSet.m_masterId, this.getSubId(), versionId);
    }


    /**
     * Get all currently selected channels
     * @return Vector of all currently selected channels
     */
     public Vector getSelectedChannels() throws CmsException{
        if (m_selectedChannels == null) {
            Vector dbChannels = getChannels();
            m_selectedChannels = new Vector();
            String rootChannel = getDbAccessObject(this.getSubId()).getRootChannel();
            int offset = rootChannel.length()-1;
            for (int i=0; i< dbChannels.size(); i++) {
                // remove the root channel name from the channel's name
                // and add to new Vector
                m_selectedChannels.add(((String)dbChannels.elementAt(i)).substring(offset));
            }
        }
        return m_selectedChannels;
     }

     /**
     * set Selected Channels
     * @param channels a String containing the channels names as a comma separated list
     */
    public void setSelectedChannels(String channels) {
        StringTokenizer tk = new StringTokenizer(channels, ",");
        Vector v = new Vector();
        int tokens = tk.countTokens();
        if (channels != null && channels.equals("empty")) {
            m_selectedChannels = v;
        }else if (tokens > 0) {
            for (int i=0; i<tokens; i++) {
                v.addElement(tk.nextToken());
            }
            m_selectedChannels = v;
        }
    }

     /**
      * Get all currently available channels
      * Note: the root channel of the module is not included in the returned
      * channelnames. For example if the root channel is /Jobs/ and a channel's
      * name is /Jobs/Education/Cologne/ the returned name for this channel will
      * be /Education/Cologne/.
      * @param cms object to access system resources
      * @return a Vector of all channels that can be selected
      */
      public Vector getAvailableChannels(CmsObject cms) throws CmsException {
        if (m_availableChannels == null) {
            Vector selectedChannels = getSelectedChannels();
            Vector subChannels = getAllSubChannelsOfRootChannel(cms);
            for (int i=0; i<subChannels.size(); i++) {
                for (int j=0; j<selectedChannels.size(); j++) {
                    if (subChannels.elementAt(i).equals(selectedChannels.elementAt(j))) {
                        subChannels.removeElementAt(i);
                        i--;
                        break;
                    }
                }
            }
            m_availableChannels = subChannels;
        }
        return m_availableChannels;
      }

    /**
     * Set the Available Channels
     * @param channels a String containing the channels to add as a comma separated list
     */
    public void setAvailableChannels(String channels) {
        StringTokenizer tk = new StringTokenizer(channels, ",");
        Vector v = new Vector();
        int tokens = tk.countTokens();
        if (channels != null && channels.equals("empty")) {
            m_availableChannels = v;
        } else if (tokens > 0) {
            for (int i=0; i<tokens; i++) {
                v.addElement(tk.nextToken());
            }
            m_availableChannels = v;
        }
    }

    /**
     * Get all subchannels of a channel.
     * Method returns only channels that doesn't have further subchannels because
     * it is it not intended to add contentdefinitions to channels that are not
     * endpoints of the channel folder structure. If different functionality
     * is needed this method has to be overridden in derived
     * contentdefinition classes.
     * @param cms object to access system resources
     * @param channel channel to be searched for subchannels
     * @return Vector with names of all subchannels
     * @throws com.opencms.core.CmsException in case of unrecoverable errors
     */
    public static Vector getAllSubChannelsOf (CmsObject cms, String channel)
            throws CmsException {
        Vector allChannels = new Vector();
        Vector subChannels = new Vector();
        try {
            subChannels = cms.getResourcesInFolder(I_CmsConstants.VFS_FOLDER_COS + channel);
        } catch (CmsException e) {
            // the channel is not present, so return empty Vector.
            return allChannels;
        }
        for (int i=0; i < subChannels.size(); i++) {
            CmsResource resource = (CmsResource)subChannels.get(i);
            if (resource.getState() != I_CmsConstants.C_STATE_DELETED) {            
                String folder = cms.readAbsolutePath(resource);
                Vector v = getAllSubChannelsOf(cms, folder);
                if (v.size() == 0 && hasWriteAccess(cms, resource)) {
                    allChannels.add(folder);
                }else {
                    for (int j=0; j < v.size(); j++) {
                        allChannels.add(v.get(j));
                    }
                }
            }
        }
        return allChannels;
    }


    /**
     * Get all subchannels of the module root channel without the root channel in the channel names
     * Method returns only channels that doesn't have further subchannels because
     * it is it not intended to add contentdefinitions to channels that are not
     * endpoints in the channel folder structure. If different functionality
     * is needed this method has to be overridden in derived
     * contentdefinition classes.
     * @param cms object to access system resources
     * @param channel channel to be searched for subchannels
     * @return Vector with names of all subchannels
     * @throws com.opencms.core.CmsException in case of unrecoverable errors
     */
    public Vector getAllSubChannelsOfRootChannel (CmsObject cms)
            throws CmsException {
        Vector allChannels = new Vector();
        String rootChannel = getDbAccessObject(this.getSubId()).getRootChannel();
        Vector subChannels = cms.getResourcesInFolder(I_CmsConstants.VFS_FOLDER_COS + rootChannel);
        int offset = rootChannel.length()-1;
        for (int i=0; i < subChannels.size(); i++) {
            CmsResource resource = (CmsResource)subChannels.get(i);
            if (resource.getState() != I_CmsConstants.C_STATE_DELETED) {
                String folder = cms.readAbsolutePath(resource);
                Vector v = getAllSubChannelsOf(cms, folder);
                if (v.size() == 0 && hasWriteAccess(cms, resource)) {
                    allChannels.add(folder.substring(offset));
                } else {
                    for (int j=0; j < v.size(); j++) {
                        allChannels.add(((String)v.get(j)).substring(offset));
                    }
                }
            }
        }
        return allChannels;
    }

    /**
     * Add or remove channels
     * compares the currently selected channels with the selected
     * channels stored in the database and adds or deletes channels if necessary
     */
    protected void updateChannels() throws CmsException{
        Vector dbChannels = getChannels();
        Vector selectedChannels = getSelectedChannels();
        String rootChannel = getDbAccessObject(this.getSubId()).getRootChannel();
        String prefix = rootChannel.substring(0, rootChannel.length()-1);
        // mark all channels to be deleted if not existing in m_selectedChannels but in datatabase
        for (int i=0; i < dbChannels.size(); i++) {
            boolean found = false;
            for (int j=0; j < selectedChannels.size(); j++) {
                if (dbChannels.elementAt(i).equals(prefix + ((String)selectedChannels.elementAt(j)))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                deleteChannel((String)dbChannels.elementAt(i));
            }
        }
        // mark all channels to be added if existing in m_selectedChannels but not in database
        for (int i=0; i < selectedChannels.size(); i++) {
            boolean found = false;
            for (int j=0; j < dbChannels.size(); j++) {
                if ((prefix + ((String)selectedChannels.elementAt(i))).equals(dbChannels.elementAt(j))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                addChannel(prefix + (String)selectedChannels.elementAt(i));
            }
        }
    }

    /**
     * Get the root channel of the module
     * @return the root channel of the module
     */
     public String getRootChannel() {
        return getDbAccessObject(this.getSubId()).getRootChannel();
     }

    /**
     * has the current user the right to write the resource
     * @return a boolean
     */
    protected static boolean hasWriteAccess(CmsObject cms, CmsResource resource) throws CmsException {
        // check the rights for the current resource
        return cms.hasPermissions(resource, I_CmsConstants.C_WRITE_ACCESS);
        /*
        if( ! ( accessOther(C_ACCESS_PUBLIC_WRITE, resource) ||
                accessOwner(cms, currentUser, C_PERMISSION_WRITE, resource) ||
                accessGroup(cms, currentUser, C_ACCESS_GROUP_WRITE, resource) ) ) {
            // no write access to this resource!
            return false;
        }
        return true;
        */
    }

    /**
     * Checks, if the owner may access this resource.
     *
     * @param cms the cmsObject
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param flags The flags to check.
     *
     * @return wether the user has access, or not.
     *//*
    protected static boolean accessOwner(CmsObject cms, CmsUser currentUser,
                                    int flags, CmsResource resource) throws CmsException {
        // The Admin has always access
        if( cms.isAdmin() ) {
            return(true);
        }
        // is the resource owned by this user?
        if(resource.getOwnerId().equals(currentUser.getId())) {
            if( (resource.getAccessFlags() & flags) == flags ) {
                return true ;
            }
        }
        // the resource isn't accesible by the user.
        return false;
    } */

    /**
     * Checks, if the group may access this resource.
     *
     * @param cms the cmsObject
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param flags The flags to check.
     *
     * @return wether the user has access, or not.
     *//*
    protected static boolean accessGroup(CmsObject cms, CmsUser currentUser,
                                  int flags, CmsResource resource) throws CmsException {
        return cms.
        // is the user in the group for the resource?
        // if(cms.userInGroup(currentUser.getName(), cms.readGroup(resource.getGroupId()).getName())) {
        //     if( (resource.getAccessFlags() & flags) == flags ) {
        //        return true;
        //    }
        //}
        // the resource isn't accesible by the user.
        // return false;
    }*/

    /**
     * Checks, if others may access this resource.
     *
     * @param currentUser The user who requested this method.
     * @param currentProject The current project of the user.
     * @param flags The flags to check.
     *
     * @return wether the user has access, or not.
     *//*
    protected static boolean accessOther( int flags, CmsResource resource) throws CmsException {
        if ((resource.getAccessFlags() & flags) == flags) {
            return true;
        } else {
            return false;
        }
    }*/
}