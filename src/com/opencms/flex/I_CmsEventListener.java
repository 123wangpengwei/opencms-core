/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/flex/Attic/I_CmsEventListener.java,v $
 * Date   : $Date: 2002/10/30 09:56:43 $
 * Version: $Revision: 1.4 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002  The OpenCms Group
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
 *
 * First created on 18. April 2002, 14:59
 */


package com.opencms.flex;

/**
 * Implement this interface in case your class has to react 
 * to CmsEvents that are thrown by system.<p>
 * 
 * In order to recieve system events, your class must register with 
 * the OpenCms event mechanism. This can be done in the constructor of a class
 * like this:
 * <pre>
 * com.opencms.core.A_OpenCms.addCmsEventListener(this);
 * </pre>
 * 
 * A typical implementation might look like this:
 * <pre>
 * public void cmsEvent(com.opencms.flex.CmsEvent event) {
 *     switch (event.getType()) {
 *         case com.opencms.flex.I_CmsEventListener.EVENT_PUBLISH_PROJECT:
 *         case com.opencms.flex.I_CmsEventListener.EVENT_CLEAR_CACHES:
 *             // do something
 *             break;
 *         case com.opencms.flex.I_CmsEventListener.EVENT_LOGIN_USER:
 *            // do something else
 *             break;
 *         }
 * }
 * </pre>
 * 
 * @author  Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.4 $
 * @since FLEX alpha 1
 * 
 * @see CmsEvent
 * @see com.opencms.core.A_OpenCms#addCmsEventListener(I_CmsEventListener)
 */
public interface I_CmsEventListener {
    
    /** 
     * Event "user has logged in" 
     *
     * @see com.opencms.file.CmsObject#loginUser(String, String) 
     */
    public static int EVENT_LOGIN_USER = 1;

    /**
     * Event "a project was published" 
     * 
     * @see com.opencms.file.CmsObject#publishProject(int, I_CmsReport)
     */    
    public static int EVENT_PUBLISH_PROJECT = 2;
    
    /** 
     * Event "a resource was published" 
     * 
     * @see com.opencms.file.CmsObject#publishResource(String, boolean)
     */    
    public static int EVENT_PUBLISH_RESOURCE = 3;    

    /** 
     * Event "a resource in the COS was published".
     * 
     * @see com.opencms.defaults.master.CmsMasterContent#publishResource(CmsObject)
     */    
    public static int EVENT_PUBLISH_BO_RESOURCE = 4;    
        
    /** 
     * Event "all caches mut be cleared".<p>
     *
     * Not thrown by the core classes, but might be used in modules.
     */
    public static int EVENT_CLEAR_CACHES = 5;

    /** 
     * Event used by the Flex Cluster Module
     */ 
    public static int EVENT_FLEX_CLUSTER_CHECK_SOURCE = 6;

    /** 
     * Event used by the Flex Cluster Module
     */      
    public static int EVENT_FLEX_CLUSTER_HOOK = 7;

    /** 
     * Event "delete all JSP pages in the "real" file system 
     * (so they will be rebuild next time the JSP is requested)".<p>
     * 
     * This is thrown on the "FlexCache Administration" page if you press
     * the button "Purge JSP repository", or if you use the <code>_flex=purge</code>
     * request parameter.
     */
    public static int EVENT_FLEX_PURGE_JSP_REPOSITORY = 8;
    
    /** 
     * Event "the FlexCache must be cleared".<p>
     * 
     * This is thrown on the "FlexCache Administration" page if you press
     * one ot the "Clear cache" buttons, or if you use the <code>_flex=clearcache</code>
     * request parameter.
     */
    public static int EVENT_FLEX_CACHE_CLEAR = 9;
    
    /** Event "static export has just happened"
     *
     * @see com.opencms.file.CmsObject#publishProject(int, I_CmsReport)
     */
    public static int EVENT_STATIC_EXPORT = 10;
   
    /**
     * Acknowledge the occurrence of the specified event, implement this 
     * method to check for CmsEvents in your class.
     *
     * @param event CmsEvent that has occurred
     */
    public void cmsEvent(CmsEvent event);
}

