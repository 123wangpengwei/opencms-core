/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/Attic/CmsGroup.java,v $
 * Date   : $Date: 2000/06/06 13:50:11 $
 * Version: $Revision: 1.13 $
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

package com.opencms.file;

import com.opencms.core.*;

/**
 * This class describes a Cms user group and the methods to access it.
 * 
 * @author Michael Emmerich
 * @version $Revision: 1.13 $ $Date: 2000/06/06 13:50:11 $
 */
 public class CmsGroup implements I_CmsConstants { 
    
    /**
     * The name of the user group.
     */
    private String m_name=null;
    
    /**
     * The id of the user group.
     */
    private int m_id=C_UNKNOWN_ID;
    
     /**
     * The parent id of the user group.
     */
    private int m_parentId=C_UNKNOWN_ID;
    
    /**
     * The description of the user group.
     */
	private String m_description=null;
    
    /**
     * The flags of the user group.
     */
	private int m_flags=C_UNKNOWN_INT;
	
		
     /**
     * Constructor, creates a new Cms group object.
     * 
     * @param id The id of the new group.
     * @param parent The parent group of the new group (or C_UNKNOWN_ID).
     * @param name The name of the new group.
     * @param description The description of the new group.
     * @param flags The flags of the new group.
     
     */
    public CmsGroup (int id, int parent,String name, String description, 
                     int flags) {
        m_id=id;
        m_name=name;
        m_description=description;
        m_flags=flags;
        m_parentId=parent;
       
    }  
    
    
	/**
	 * Returns the name of this group.
	 * 
	 * @return name The name of the group.
	 */
     public String getName(){
         return m_name;
     }
	
	/**
	 * Returns the id of a group. 
	 * 
	 * @return id The id of this group.
	 */
     public int getId(){
         return m_id;
     }
	
	/**
	 * Returns the description of this group.
	 * 
	 * @return description The description of this group.
	 */
     public String getDescription(){
         return m_description;
     }
	
	/**
	 * Sets the description of this group.
	 */
     public void setDescription(String description){
         m_description = description;
     }
	 
    /**
     * Decides, if this group is disabled.
     * 
     * @return GROUP_FLAGS == C_FLAG_DISABLED
     */
     public boolean getDisabled() {
         if ((m_flags & C_FLAG_DISABLED) != 0) 
             return true;
         else
			 return false;
     }
	 
	/**
     * Is the Flag Projectmanager set?
     * 
     * @return true if C_FLAG_GROUP_PROJECTMANAGER is set 
     */
	public boolean getProjectmanager() {
		if ((m_flags & C_FLAG_GROUP_PROJECTMANAGER) != 0) {
             return true;
         } else
			 return false;
     } 
 
	/**
     * Is the Flag ProjectCoWorker set?
     * 
     * @return true if C_FLAG_GROUP_PROJECTCOWORKER is set 
     */
	public boolean getProjectCoWorker(){
		if ((m_flags & C_FLAG_GROUP_PROJECTCOWORKER) != 0) {
             return true;
         } else
			 return false;
     }
 
	/**
     * Is the Flag Role set?
     * 
     * @return true if C_FLAG_GROUP_ROLE is set 
     */
	public boolean getRole(){
		if ((m_flags & C_FLAG_GROUP_ROLE) != 0) {
             return true;
         } else
			 return false;
     }  
	
	/**
	 * Returns the GROUP_FLAGS.
	 * 
	 * @return the GROUP_FLAGS.
	 */
     public int getFlags() {
         return m_flags;
     }

    /**
	 * Sets the GROUP_FLAGS.
	 * 
	 * @param flags The flags to be set.
	 * 
	 */
     void setFlags(int flags) {
         m_flags=flags;
     }
	 
    /**
     * Disables the group by setting the C_FLAG_DISABLED flag.
     */
    public void  setDisabled() {
        setFlags(C_FLAG_DISABLED);
    }
    
	/**
     * sets the PROJECTMANAGER flag of the group
     */
	public void setProjectManager(boolean f) {
		if (getProjectmanager() != f)
			setFlags(getFlags() ^ C_FLAG_GROUP_PROJECTMANAGER);
    } 

	/**
     * sets the PROJECTCOWORKER flag of the group
     */
	public void setProjectCoWorker(boolean f){
		if (getProjectCoWorker() != f) 
			setFlags(getFlags() ^ C_FLAG_GROUP_PROJECTCOWORKER);
    }
	
	/**
     *  sets the ROLE flag of the group
     */
	public void setRole(boolean f){
		if (getRole() != f) 
			setFlags(getFlags() ^ C_FLAG_GROUP_ROLE);
    }  
	
	
    /**
     * Enables the flags by setting them to C_FLAG_ENABLED.
     */
    public void  setEnabled() {
        setFlags(C_FLAG_ENABLED);
    }	 
     
	/**
	 * Returns the id of the parent group of the actual Cms group object, 
	 * or C_UNKNOWN_ID.
	 * 
	 * @return PARENT_GROUP_ID or C_UNKNOWN_ID
	 */
     public int getParentId() {
      return m_parentId;         
     }

	/**
	 * Sets the id of the parent group of the actual Cms group object.
	 * 
	 * @param id The parent-groupid
	 */
     void setParentId(int id) {
      m_parentId = id;
     }
		
	/**
	 * Returns a string-representation for this object.
	 * This can be used for debugging.
	 * 
	 * @return string-representation for this object.
	 */
     public String toString() {
        StringBuffer output=new StringBuffer();
        output.append("[Group]:");
        output.append(m_name);
        output.append(" , Id=");
        output.append(m_id);
        output.append(" :");
        output.append(m_description);
        return output.toString();
      }
	
	/**
	 * Compares the overgiven object with this object.
	 * 
	 * @return true, if the object is identically else it returns false.
	 */
     public boolean equals(Object obj) {
         boolean equal=false;
        // check if the object is a CmsGroup object
        if (obj instanceof CmsGroup) {
            // same ID than the current user?
            if (((CmsGroup)obj).getId() == m_id){
                equal = true;
            }
        }
        return equal;
     }

    /** 
    * Clones the CmsGroup by creating a new CmsGroup Object.
    * @return Cloned CmsGroup.
    */
    public Object clone() {
        CmsGroup group= new CmsGroup(m_id,m_parentId,new String(m_name),
                                     new String(m_description),m_flags); 
        return group;   
    }   
    
}