/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/defaults/master/Attic/CmsChannelContent.java,v $
* Date   : $Date: 2002/01/31 10:18:21 $
* Version: $Revision: 1.7 $
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

import com.opencms.core.*;
import com.opencms.core.exceptions.*;
import com.opencms.dbpool.*;
import com.opencms.defaults.*;
import com.opencms.file.*;
import com.opencms.template.*;
import java.util.*;
import java.lang.*;

/**
 * This class is the master of several Modules. It carries a lot of generic
 * data-fileds which can be used for a special Module.
 *
 * The module creates a set of methods to support project-integration, history
 * and import - export.
 *
 * @author E. Falkenhan $
 * $Revision: 1.7 $
 * $Date: 2002/01/31 10:18:21 $
 */
public class CmsChannelContent extends A_CmsContentDefinition
                               implements I_CmsContent, I_CmsLogChannels, I_CmsExtendedContentDefinition{

    /**
     * The name of the "tablekey" for the channel id
     */
    private static final String C_TABLE_CHANNELID = I_CmsConstants.C_TABLE_CHANNELID;

    // definition of the error codes used by this content defintion
    private static String C_CHANNELNAME_ERRFIELD="channelname";
    private static String C_OWNER_ERRFIELD="channelowner";
    private static String C_GROUP_ERRFIELD="channelgroup";
    private static String C_PARENT_ERRFIELD="channelparent";
    //error code for empty inputs
    private static String C_ERRCODE_EMPTY="empty";
    //error code for no text input
    private static String C_ERRCODE_NOTEXT="notext";

    /** The cms-object to get access to the cms-ressources */
    protected CmsObject m_cms = null;

    /**
     * The channel ID
     */
    private String m_channelId;

    /**
     * The resource object that contains the information for the channel
     */
    private CmsResource m_channel;

    /**
     * The name of the channel
     */
    private String m_channelname;

    /**
     * The name of the parent channel
     */
    private String m_parentchannel;

    /**
     * The properties of the channel
     */
    private Hashtable m_properties;

    /**
     * The groupid of the channel
     */
    private int m_groupid;

    /**
     * The userid of the channel
     */
    private int m_userid;

    /**
     * The accessflags of the channel
     */
    private int m_accessflags;

    /**
     * Constructor to create a new contentdefinition. You can set data with your
     * set-Methods. After you have called the write-method this definition gets
     * a unique id.
     */
    public CmsChannelContent(CmsObject cms) {
        m_cms = cms;
        initValues();
    }

    /**
     * Constructor to read a existing contentdefinition from the database. The
     * data read from the database will be filled into the member-variables.
     * You can read them with the get- and modify them with the set-methods.
     * Changes you have made must be written back to the database by calling
     * the write-method.
     * @param cms the cms-object for access to cms-resources.
     * @param resourceid the resource id of the channel to read.
     * @throws CmsException if the data couldn't be read from the database.
     */
    public CmsChannelContent(CmsObject cms, String resourceid) throws CmsException {
        new CmsChannelContent(cms, new Integer(resourceid));
    }
    /**
     * Constructor to read a existing contentdefinition from the database. The
     * data read from the database will be filled into the member-variables.
     * You can read them with the get- and modify them with the set-methods.
     * Changes you have made must be written back to the database by calling
     * the write-method.
     * @param cms the cms-object for access to cms-resources.
     * @param channelname the name of the channel to read.
     * @throws CmsException if the data couldn't be read from the database.
     */
    public CmsChannelContent(CmsObject cms, Integer resourceid) throws CmsException {
        m_cms = cms;
        initValues();
        m_cms.setContextToCos();
        try{
            m_channel = (CmsResource) m_cms.readFolder(resourceid.intValue(), true);
            m_channelname = m_channel.getName();
            m_parentchannel = m_channel.getParent();
            m_groupid = m_channel.getGroupId();
            m_userid = m_channel.getOwnerId();
            m_accessflags = m_channel.getAccessFlags();
            m_properties = m_cms.readAllProperties(m_channel.getAbsolutePath());
            m_channelId = (String)m_properties.get(I_CmsConstants.C_PROPERTY_CHANNELID);
        } catch (CmsException exc){
            if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(C_OPENCMS_INFO, "[CmsChannelContent] Could not get channel "+resourceid);
            }
        } finally {
            m_cms.setContextToVfs();
        }
    }

    /**
     * Constructor to create a new contentdefinition. You can set data with your
     * set-Methods. After you have called the write-method this definition gets
     * a unique id.
     */
    public CmsChannelContent(CmsObject cms, CmsResource resource) {
        String channelId = C_UNKNOWN_ID+"";
        String title = "";
        m_cms = cms;
        m_channel = resource;
        m_channelname = resource.getName();
        m_parentchannel = resource.getParent();
        m_groupid = resource.getGroupId();
        m_userid = resource.getOwnerId();
        m_accessflags = resource.getAccessFlags();
        try{
            m_properties = cms.readAllProperties(resource.getAbsolutePath());
            channelId = (String)m_properties.get(I_CmsConstants.C_PROPERTY_CHANNELID);
        } catch (CmsException exc){
            m_properties = new Hashtable();
            m_properties.put(I_CmsConstants.C_PROPERTY_CHANNELID, C_UNKNOWN_ID+"");
        } finally {
            if(channelId == null || "".equals(channelId)){
                channelId = C_UNKNOWN_ID+"";
            }
            m_channelId = channelId;
        }
    }

    /**
     * This method initialises all needed members with default-values.
     */
    protected void initValues() {
        m_channelId = I_CmsConstants.C_UNKNOWN_ID+"";
        m_channelname = "";
        m_parentchannel = "";
        m_groupid = m_cms.getRequestContext().currentGroup().getId();
        m_userid = m_cms.getRequestContext().currentUser().getId();
        m_accessflags = I_CmsConstants.C_ACCESS_DEFAULT_FLAGS;
        // create the resource object for the channel:
        // int resourceId, int parentId, int fileId, String resourceName, int resourceType,
        // int resourceFlags, int user, int group, int projectId, int accessFlags, int state,
        // int lockedBy, int launcherType, String launcherClassname, long dateCreated,
        // long dateLastModified, int resourceLastModifiedBy,int size, int lockedInProject
        m_channel = new CmsResource(I_CmsConstants.C_UNKNOWN_ID, I_CmsConstants.C_UNKNOWN_ID,
                                     I_CmsConstants.C_UNKNOWN_ID, "", I_CmsConstants.C_TYPE_FOLDER, 0,
                                     m_cms.getRequestContext().currentUser().getId(),
                                     m_cms.getRequestContext().currentGroup().getId(),
                                     m_cms.getRequestContext().currentProject().getId(),
                                     I_CmsConstants.C_ACCESS_DEFAULT_FLAGS, 1,
                                     m_cms.getRequestContext().currentUser().getId(),
                                     I_CmsConstants.C_UNKNOWN_ID, "",
                                     System.currentTimeMillis(), System.currentTimeMillis(),
                                     m_cms.getRequestContext().currentUser().getId(), 0,
                                     m_cms.getRequestContext().currentProject().getId());
        m_properties = new Hashtable();
    }

    /**
     * delete method
     * for delete instance of content definition
     * @param cms the CmsObject to use.
     */
    public void delete(CmsObject cms) throws Exception {
        cms.setContextToCos();
        try{
            cms.deleteResource(m_channel.getAbsolutePath());
        } catch (CmsException exc){
            throw exc;
            /*
            if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(C_OPENCMS_INFO, "[CmsChannelContent] Could not delete channel "+m_channel.getAbsolutePath());
            }
            */
        } finally {
            cms.setContextToVfs();
        }
    }

    /**
     * undelete method
     * for undelete instance of content definition
     * @param cms the CmsObject to use.
     */
    public void undelete(CmsObject cms) throws Exception {
        cms.setContextToCos();
        try{
            cms.undeleteResource(m_channel.getAbsolutePath());
        } catch (CmsException exc){
            if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(C_OPENCMS_INFO, "[CmsChannelContent] Could not undelete channel "+m_channel.getAbsolutePath());
            }
        } finally {
            cms.setContextToVfs();
        }
    }

    /**
     * publish method
     * for publish instance of content definition
     * @param cms the CmsObject to use.
     */
    public void publishResource(CmsObject cms) {
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(C_OPENCMS_INFO, "[CmsChannelContent] Channels can't be published directly!");
        }
    }

    /**
     * restore method
     * for restore instance of content definition from history
     * @param cms the CmsObject to use.
     * @param versionId The id of the version to restore
     */
    public void restore(CmsObject cms, int versionId) {
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(C_OPENCMS_INFO, "[CmsChannelContent] Channels can't be restored from history!");
        }
    }

    /**
     * Change owner method
     * for changing permissions of content definition
     * @param cms the CmsObject to use.
     * @param owner the new owner of the cd.
     */
    public void chown(CmsObject cms, int owner) {
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(C_OPENCMS_INFO, "[CmsChannelContent] Permissions of Channels can be changed only in EditBackoffice!");
        }
    }

    /**
     * Change group method
     * for changing permissions of content definition
     * @param cms the CmsObject to use.
     * @param group the new group of the cd.
     */
    public void chgrp(CmsObject cms, int group) {
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(C_OPENCMS_INFO, "[CmsChannelContent] Permissions of Channels can be changed only in EditBackoffice!");
        }
    }

    /**
     * Change access flags method
     * for changing permissions of content definition
     * @param cms the CmsObject to use.
     * @param accessflags the new access flags of the cd.
     */
    public void chmod(CmsObject cms, int accessflags) {
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(C_OPENCMS_INFO, "[CmsChannelContent] Permissions of Channels can be changed only in EditBackoffice!");
        }
    }
    /**
     * Copy method
     * for copying content definition
     * @param cms the CmsObject to use.
     * @return int The id of the new content definition
     */
    public int copy(CmsObject cms) {
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(C_OPENCMS_INFO, "[CmsChannelContent] Channels can be copied!");
        }
        return -1;
    }

    /**
     * write method
     * to write the current content of the content definition to the database.
     * @param cms the CmsObject to use.
     */
    public void write(CmsObject cms) throws Exception {
        CmsResource newChannel = null;
        // is this a new row or an existing row?
        cms.setContextToCos();
        try{
            if((I_CmsConstants.C_UNKNOWN_ID+"").equals(m_channelId)) {
                // this is a new row - call the create statement
                // first set the new channelId
                setNewChannelId();
                newChannel = cms.createResource(m_parentchannel, m_channelname, I_CmsConstants.C_TYPE_FOLDER_NAME, m_properties);
                cms.lockResource(newChannel.getAbsolutePath(), true);
            } else {
                newChannel = cms.readFolder(m_channel.getAbsolutePath());
                if (!newChannel.getAbsolutePath().equals(m_parentchannel+m_channelname+"/")){
                    // the parent and/or the channelname has changed,
                    // so move or rename the channel
                    if(!newChannel.getParent().equals(m_parentchannel)){
                        // move the channel to the new parent channel
                        cms.moveResource(newChannel.getAbsolutePath(), m_parentchannel+m_channelname);
                    } else if (!newChannel.getName().equals(m_channelname)){
                        // only rename the channel, the parent has not changed
                        cms.renameResource(newChannel.getAbsolutePath(), m_channelname);
                    }
                }
                // read the changed channel
                newChannel =  cms.readFolder(m_parentchannel+m_channelname+"/");
                // update the title of the channel
                String propTitle = cms.readProperty(newChannel.getAbsolutePath(), I_CmsConstants.C_PROPERTY_TITLE);
                if (propTitle == null){
                    propTitle = "";
                }
                if (!propTitle.equals(this.getTitle())){
                    cms.writeProperty(newChannel.getAbsolutePath(), I_CmsConstants.C_PROPERTY_TITLE, this.getTitle());
                }
                // check if the lockstate has changed
                if(newChannel.isLockedBy() != this.getLockstate() ||
                    newChannel.getLockedInProject() != cms.getRequestContext().currentProject().getId()){
                    if(this.getLockstate() == I_CmsConstants.C_UNKNOWN_ID){
                        // unlock the channel
                        cms.unlockResource(newChannel.getAbsolutePath());
                    } else {
                        // lock the channel
                        cms.lockResource(newChannel.getAbsolutePath(), true);
                    }
                };
                // check if the owner has changed
                if(newChannel.getOwnerId() != this.getOwner()){
                    cms.chown(newChannel.getAbsolutePath(), this.getOwnerName());
                };
                // check if the group has changed

                if(newChannel.getGroupId() != this.getGroupId()){
                    cms.chgrp(newChannel.getAbsolutePath(), this.getGroup());
                };
                // check if the accessflags has changed
                if(newChannel.getAccessFlags() != this.getAccessFlags()){
                    cms.chmod(newChannel.getAbsolutePath(), this.getAccessFlags());
                };
            }
            m_channel = cms.readFolder(newChannel.getAbsolutePath());
        } catch (CmsException exc){
            throw exc;
            /*
            if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(C_OPENCMS_INFO, "[CmsChannelContent] Could not write channel "+m_channel.getAbsolutePath());
            }
            */
        } finally {
            cms.setContextToVfs();
        }
    }

    /**
     * gets the unique Id of a content definition instance
     * @param cms the CmsObject to use.
     * @returns a string with the Id
     */
    public String getUniqueId(CmsObject cms) {
        return m_channel.getResourceId() + "";
    }

    /**
     * gets the unique Id of a content definition instance
     *
     * @returns int The unique id
     */
    public int getId() {
        return m_channel.getResourceId();
    }

    /**
     * Gets the projectId where the CD belongs to.
     * This method is required for the use of the abstract backoffice.
     * @returns int with the projectId.
     */
    public int getLockedInProject() {
        return m_channel.getLockedInProject();
    }

    /**
     * Gets the state of a CD.
     * This method is required for the use of the abstract backoffice.
     * @returns int with the state.
     */
    public int getState() {
        return m_channel.getState();
    }

    /**
     * Gets the projectId of a CD.
     * This method is required for the use of the abstract backoffice.
     * @returns int with the projectId.
     */
    public int getProjectId() {
        return m_channel.getProjectId();
    }

    /**
     * gets the unique channel Id of a content definition instance
     * @returns a string with the Id
     */
    public String getChannelId() {
        return m_channelId;
    }

    /**
     * sets the unique channel Id of a content definition instance
     */
    public void setChannelId(String id) {
        m_properties.put(I_CmsConstants.C_PROPERTY_CHANNELID, id);
    }

    /**
     * Gets the title of the channel
     */
    public String getTitle(){
        String title = (String)m_properties.get(I_CmsConstants.C_PROPERTY_TITLE);
        if (title == null){
            title = "";
        }
        return title;
    }

    /**
     * sets the title of a content definition instance
     */
    public void setTitle(String title) {
        m_properties.put(I_CmsConstants.C_PROPERTY_TITLE, title);
    }

    /**
     * Gets the lockstate.
     * @returns a int with the user who has locked the ressource.
     */
    public int getLockstate() {
        return m_channel.isLockedBy();
    }

    /**
     * Sets the lockstates
     * @param the lockstate for the actual entry.
     */
    public void setLockstate(int lockstate) {
        m_channel.setLocked(lockstate);
    }

    /**
     * Gets the ownername of this contentdefinition.
     */
    public String getOwnerName() {
        String ownername = "";
        try{
            ownername = m_cms.readUser(getOwner()).getName();
        } catch (CmsException exc){
            //nothing to do, return empty string
        }
        return ownername;
    }

    /**
     * Gets the owner of this contentdefinition.
     */
    public int getOwner() {
        return m_userid;
    }

    /**
     * Sets the owner of this contentdefinition.
     */
    public void setOwner(int id) {
        m_userid = id;
    }

    /**
     * Gets the groupname
     */
    public String getGroup() {
        String groupname = "";
        try{
            groupname = m_cms.readGroup(this.getGroupId()).getName();
        } catch (CmsException exc){
            // nothing to do, return empty string
        }
        return groupname;
    }

    /**
     * Gets the groupid
     */
    public int getGroupId() {
        return m_groupid;
    }

    /**
     * Sets the group.
     */
    public void setGroup(int id) {
        m_groupid = id;
    }

    /**
     * Gets the channelname
     */
    public String getChannelPath() {
        return m_channel.getParent()+m_channel.getName();
    }

    /**
     * Gets the channelname
     */
    public String getChannelName() {
        return m_channelname;
    }

    /**
     * Sets the channelname.
     */
    public void setChannelName(String name) {
        m_channelname = name;
    }

    /**
     * Sets the parentname of the channel.
     */
    public void setParentName(String name) {
        m_parentchannel = name;
    }

    /**
     * Gets the name of the parent channel
     */
    public String getParentName() {
        return m_parentchannel;
    }

    /**
     * Sets the accessflags of the channel.
     */
    public void setAccessFlags(int flags) {
        m_accessflags = flags;
    }

    /**
     * Gets the accessflags of the channel
     */
    public int getAccessFlags() {
        return m_accessflags;
    }

    /**
     * Gets the date of the last modification of the channel
     */
    public long getDateLastModified(){
        return m_channel.getDateLastModified();
    }

    /**
     * Gets the date of the creation of the channel
     */
    public long getDateCreated(){
        return m_channel.getDateCreated();
    }

    /**
     * Gets the id of the user who has modified the channel
     */
    public int getLastModifiedBy(){
        return m_channel.getResourceLastModifiedBy();
    }

    /**
     * Gets the name of the user who has modified the channel
     */
    public String getLastModifiedByName(){
        return "";
    }

    /**
     * Gets the version id of version the channel
     */
    public int getVersionId(){
        return C_UNKNOWN_ID;
    }

    /**
     * Gets all groups, that may work for a project.
     * <P>
     * The given vectors <code>names</code> and <code>values</code> will
     * be filled with the appropriate information to be used for building
     * a select box.
     *
     * @param cms CmsObject Object for accessing system resources.
     * @param names Vector to be filled with the appropriate values in this method.
     * @param values Vector to be filled with the appropriate values in this method.
     * @return Index representing the current value in the vectors.
     * @exception CmsException
     */

    public Integer getGroups(CmsObject cms, Vector names, Vector values) throws CmsException {

        // get all groups
        Vector groups = cms.getGroups();
        int retValue = -1;
        String defaultGroup = C_GROUP_USERS;
        I_CmsSession session = cms.getRequestContext().getSession(true);
        String enteredGroup = this.getGroup();
        if(enteredGroup != null && !enteredGroup.equals("")) {

            // if an error has occurred before, take the previous entry of the user
            defaultGroup = enteredGroup;
        }

        // fill the names and values
        int n = 0;
        for(int z = 0;z < groups.size();z++) {
            if(((CmsGroup)groups.elementAt(z)).getProjectCoWorker()) {
                String name = ((CmsGroup)groups.elementAt(z)).getName();
                if(defaultGroup.equals(name)) {
                    retValue = n;
                }
                names.addElement(name);
                values.addElement(name);
                n++; // count the number of ProjectCoWorkers
            }
        }
        return new Integer(retValue);
    }

    /**
     * history method
     * returns the history of content definition
     * @param cms the CmsObject to use.
     * @return Vector The history of a cd
     */
    public Vector getHistory(CmsObject cms) {
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(C_OPENCMS_INFO, "[CmsChannelContent] Channels have no history!");
        }
        return null;
    }

    /**
     * History method
     * returns the cd of the version with the given versionId
     *
     * @param cms The CmsObject
     * @param versionId The version id
     * @return Object The object with the version of the cd
     */
    public Object getVersionFromHistory(CmsObject cms, int versionId){
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(C_OPENCMS_INFO, "[CmsChannelContent] Channels have no history!");
        }
        return null;
    };
    /**
     * returns true if the CD is readable for the current user
     * @retruns true
     */
    public boolean isReadable() {
        m_cms.setContextToCos();
        try {
            return m_cms.accessRead(m_channel.getAbsolutePath());
        } catch(CmsException exc) {
            // there was a cms-exception - no read-access!
            return false;
        } finally {
            m_cms.setContextToVfs();
        }
    }

    /**
     * returns true if the CD is writeable for the current user
     * @retruns true
     */
    public boolean isWriteable() {
        m_cms.setContextToCos();
        try {
            return m_cms.accessWrite(m_channel.getAbsolutePath());
        } catch(CmsException exc) {
            // there was a cms-exception - no write-access!
            return false;
        } finally {
            m_cms.setContextToVfs();
        }
    }

    /**
     * Returns the sub-id of this contentdefinition. You have to implement this
     * method so it returns a unique sunb-id that describes the type of the
     * contentdefinition. (E.g. article: sub-id=1; table: sub-id=2).
     */
    //abstract public int getSubId();

    /**
     * Returns a String representation of this instance.
     * This can be used for debugging purposes.
     */
    public String toString() {
        StringBuffer returnValue = new StringBuffer();
        returnValue.append(this.getClass().getName() + "{");
        returnValue.append("ChannelId=" + getChannelId() + ";");
        returnValue.append("ChannelName=" + getChannelPath() + ";");
        returnValue.append("Lockstate=" + getLockstate() + ";");
        returnValue.append("AccessFlags=" + getAccessFlagsAsString() + ";");
        returnValue.append(m_channel.toString() + "}");
        return returnValue.toString();
    }

    /**
     * Convenience method to get the access-Flags as String representation.
     * @return String of access rights
     */
    public String getAccessFlagsAsString() {
        int accessFlags = m_channel.getAccessFlags();
        String str = "";
        str += ((accessFlags & I_CmsConstants.C_ACCESS_OWNER_READ)>0?"r":"-");
        str += ((accessFlags & I_CmsConstants.C_ACCESS_OWNER_WRITE)>0?"w":"-");
        str += ((accessFlags & I_CmsConstants.C_ACCESS_OWNER_VISIBLE)>0?"v":"-");
        str += ((accessFlags & I_CmsConstants.C_ACCESS_GROUP_READ)>0?"r":"-");
        str += ((accessFlags & I_CmsConstants.C_ACCESS_GROUP_WRITE)>0?"w":"-");
        str += ((accessFlags & I_CmsConstants.C_ACCESS_GROUP_VISIBLE)>0?"v":"-");
        str += ((accessFlags & I_CmsConstants.C_ACCESS_PUBLIC_READ)>0?"r":"-");
        str += ((accessFlags & I_CmsConstants.C_ACCESS_PUBLIC_WRITE)>0?"w":"-");
        str += ((accessFlags & I_CmsConstants.C_ACCESS_PUBLIC_VISIBLE)>0?"v":"-");
        str += ((accessFlags & I_CmsConstants.C_ACCESS_INTERNAL_READ)>0?"i":"-");
        return str;
    }

    /**
     * Get the permissions of Channel
     */
    public void setAccessFlagsAsString(String permissions){
        //change direction
        String perm=permissions;
        permissions="";
        for(int x=9;x >= 0;x--) {
            char temp=perm.charAt(x);
            permissions+=temp;
        }
        setAccessFlags(Integer.parseInt(permissions,2));
    }

    /**
     * Sets the channelId of a new channel
     */
    private void setNewChannelId() throws CmsException{
        int newChannelId = com.opencms.dbpool.CmsIdGenerator.nextId(this.C_TABLE_CHANNELID);
        m_properties.put(I_CmsConstants.C_PROPERTY_CHANNELID, newChannelId+"");
        m_channelId = newChannelId+"";
    }

    /**
     * This content definition is lockable. This class overwrited the isLockable method of the abstract
     * backoffice to flag that this content definition uses the lock feature of the backoffice.  *
     */
    public static boolean isLockable() {
        return true;
    }

    /**
     * Gets the names of the table columns displayed in the backoffice filelist.
     * This method is needed for the abstract backoffice class only.
     * @return Vector containing the columnnames.
     */
    public static Vector getFieldNames(CmsObject cms) {
        Vector names = new Vector();
        names.addElement("channelId");
        names.addElement("channelPath");
        names.addElement("title");
        names.addElement("ownerName");
        names.addElement("group");
        names.addElement("accessFlagsAsString");
        return names;
    }

    /**
     * Gets the methods used to display the dava values in the backoffice filelist.
     * This method is needed for the abstract backoffice class only.
     * @returns Vector with the nescessary get methods
     */
    public static Vector getFieldMethods(CmsObject cms) {
        Vector methods = new Vector();
        try {
            methods.addElement(CmsChannelContent.class.getMethod("getChannelId", new Class[0]));
            methods.addElement(CmsChannelContent.class.getMethod("getChannelPath", new Class[0]));
            methods.addElement(CmsChannelContent.class.getMethod("getTitle", new Class[0]));
            methods.addElement(CmsChannelContent.class.getMethod("getOwnerName", new Class[0]));
            methods.addElement(CmsChannelContent.class.getMethod("getGroup", new Class[0]));
            methods.addElement(CmsChannelContent.class.getMethod("getAccessFlagsAsString", new Class[0]));
        } catch(NoSuchMethodException exc) {
            // this exception should never occur. You know your own methods in this cd
        }
        return methods;
    }

    /**
     * Gets the filter methods
     * This method is needed for the abstract backoffice class only.
     * @returns a method array containing the methods
     */
    public static Vector getFilterMethods(CmsObject cms) {
        Vector filterMethods = new Vector();
        try {
            CmsFilterMethod filterUp = new CmsFilterMethod("All Channels",
                                      CmsChannelContent.class.getMethod("getChannelList",
                                      new Class[] {CmsObject.class} ) , new Object[0]);
            filterMethods.addElement(filterUp);
        } catch (NoSuchMethodException nsm) {
            // this exception should never occur because you know your filter methods.
        }
        return filterMethods;
    }

    /**
     * Returns a vector of CD objects, sorted descending by channelname.
     * This method is needed for the abstract backoffice class only.
     * @param cms The CmsObject.
     */
    public static Vector getChannelList(CmsObject cms) throws CmsException {
        Vector content = new Vector();
        cms.setContextToCos();
        try {
            getAllResources(cms, "/", content);
        } catch(CmsException e) {
            // ignore the exception
            if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
                A_OpenCms.log(A_OpenCms.C_MODULE_INFO, "[CmsChannelContent]: error when reading subfolders of cos root: "+e.getMessage());
            }
        } finally {
            cms.setContextToVfs();
        }
        return content;
    }
    /**
     * Helper for method getChannelList
     */
    private static void getAllResources(CmsObject cms, String rootFolder, Vector allFolders) throws CmsException {
        // get folders of this rootFolder
        Vector subFolders = new Vector();
        subFolders = cms.getSubFolders(rootFolder, true);

        //copy the values into the allFolders Vector
        for(int i = 0;i < subFolders.size();i++) {
            CmsResource curFolder = (CmsResource)subFolders.elementAt(i);
            CmsChannelContent curChannel = new CmsChannelContent(cms, curFolder);
            allFolders.addElement(curChannel);
            getAllResources(cms, curFolder.getAbsolutePath(), allFolders);
        }
    }
    /**
     * Plauzibilization method.
     * This method checks if all inputfields contain correct input data.
     * If an input field has no correct data, a CmsPlausibilizationException is thrown.
     * @exception Throws CmsPlausibilizationException containing a vector of error-codes.
     */
    public void check() throws CmsPlausibilizationException {
        // define the vector which will hold all error codes
        Vector errorCodes = new Vector();
        //check the channelname
        if (m_channelname == null || "".equals(m_channelname)) {
            errorCodes.addElement(C_CHANNELNAME_ERRFIELD+C_ERRSPERATOR+this.C_ERRCODE_EMPTY);
        }
        //check the parentchannel
        if (m_parentchannel == null || "".equals(m_parentchannel)) {
            errorCodes.addElement(C_PARENT_ERRFIELD+C_ERRSPERATOR+this.C_ERRCODE_EMPTY);
        }
        // now test if there was an error message and throw an exception
        if (errorCodes.size()>0) {
            throw new CmsPlausibilizationException(errorCodes);
        }
    }
}