/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/template/Attic/A_CmsCacheDirectives.java,v $
* Date   : $Date: 2004/02/13 13:41:44 $
* Version: $Revision: 1.10 $
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

package com.opencms.template;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsRequestContext;
import org.opencms.main.I_CmsConstants;

import com.opencms.template.cache.CmsTimeout;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

/**
 * Abstact class for all CacheDirectives classes.
 *
 * @author Hanjo Riege
 * @version 1.0
 */

public abstract class A_CmsCacheDirectives {

    /** Bitfield for storing external cache properties */
    protected int m_cd;

    // everthing to get the cache key

    // indicates if the username is part of the cache key
    private boolean m_user = false;
    // indicates if the groupname is part of the cache key
    private boolean m_group = false;
    //indicates if the uri is part of the cache key
    private boolean m_uri = false;
    //the parameters for which the element is cacheable
    private Vector m_cacheParameter = null;

    // if one of these parameters occures the element is dynamic
    private Vector m_dynamicParameter = null;

    // indicates if this element should removed from cache after publish even if the template is unchanged.
    private boolean renewAfterEveryPublish = false;

    // the timeout object
    protected CmsTimeout m_timeout;
    protected boolean m_timecheck = false;

    // indicates if the user has set the value or if it has to be generated
    protected boolean m_userSetProxyPrivate = false;
    protected boolean m_userSetProxyPublic = false;
    protected boolean m_userSetExport = false;


    /** Flag for internal cacheable */
    public static final int C_CACHE_INTERNAL = 1;
    /** Flag for cacheable in private proxies */
    public static final int C_CACHE_PROXY_PRIVATE  = 2;
    /** Flag for cacheable in public proxies */
    public static final int C_CACHE_PROXY_PUBLIC = 4;
    /** Flag for exportable */
    public static final int C_CACHE_EXPORT = 8;
    /** Flag for streamable */
    public static final int C_CACHE_STREAM = 16;

    /**
     * Get the state of the "internal cacheable" property.
     * @return <code>true</code> if internal caching is possible, <code>false</code> otherwise.
     */
    public boolean isInternalCacheable() {
        return (m_cd & C_CACHE_INTERNAL) == C_CACHE_INTERNAL;
    }

    /**
     * get information about the cacheKey.
     * @return <code>true</code> if user or group is in the key, <code>false</code> otherwise.
     */
    public boolean isUserPartOfKey(){
        return m_group || m_user;
    }
    /**
     * get information about the cacheKey.
     * @return <code>true</code> if parameters are in the key, <code>false</code> otherwise.
     */
    public boolean isParameterPartOfKey(){
        return (m_cacheParameter != null) && ( !m_cacheParameter.isEmpty());
    }

    /**
     * Get the timeout object(used if the element should be reloaded every x minutes.
     * @return timeout object.
     */
    public CmsTimeout getTimeout() {
        return m_timeout;
    }

    /**
     * Get information if the element is time critical.
     * @return <code>true</code> if a timeout object was set, <code>false</code> otherwise.
     */
    public boolean isTimeCritical(){
        return m_timecheck;
    }

    /**
     * set the timeout object(used if the element should be reloaded every x minutes.
     * @param timeout a CmsTimeout object.
     */
    public abstract void setTimeout(CmsTimeout timeout);

    /**
     * sets the renewAfterEveryPublish value to true. This means that this element
     * is removed from cache every time a project is published even if its template or class
     * was not changed in the project.
     */
    public void renewAfterEveryPublish(){
        renewAfterEveryPublish = true;
    }

    /**
     * Switches off the renewAfterPublish function. If someone needs the uri in the key
     * but don't need the renew function he has to use this to get rid of it. To stay
     * compatible to old versions we can't remove it from the setUri method.
     */
    public void noAutoRenewAfterPublish(){
        renewAfterEveryPublish = false;
    }

    /**
     * returns true if this element has to be deleted from cache every time a project
     * is published. This is when the uri is part of the cacheKey or if the user says so.
     */
    public boolean shouldRenew(){
        return renewAfterEveryPublish;
    }

    /**
     * calculates the cacheKey for the element.
     * @return The cache key or null if it is not cacheable
     */
    public String getCacheKey(CmsObject cms, Hashtable parameters) {

        if ( ! this.isInternalCacheable()){
            return null;
        }
        if (parameters == null){
            parameters = new Hashtable();
        }
        // first test the parameters which say it is dynamic
        if((m_dynamicParameter != null) && (!m_dynamicParameter.isEmpty())){
            for (int i=0; i < m_dynamicParameter.size(); i++){
                String testparameter = (String)m_dynamicParameter.elementAt(i);
                if(parameters.containsKey(testparameter)){
                    return null;
                }
            }
        }
        CmsRequestContext reqContext = cms.getRequestContext();
        String groupKey = "";
//        if(m_group){
//            groupKey = reqContext.currentGroup().getName();
//            if((m_cacheGroups != null) && (!m_cacheGroups.isEmpty())){
//                if(!m_cacheGroups.contains(groupKey)){
//                    return null;
//                }
//            }
//        }

        // ok, a cachekey exists. lets put it together
        // first we need the scheme of the request
        String scheme = "http";
        try{
            scheme = ((HttpServletRequest)cms.getRequestContext().getRequest().getOriginalRequest()).getScheme();
        }catch(Exception e){
        }
        String key = "key_"+scheme+"_";
        if(m_uri){
            key += reqContext.getUri();
        }
        if(m_user){
            key += reqContext.currentUser().getName();
        }
        key += groupKey;
        if((m_cacheParameter != null) && ( !m_cacheParameter.isEmpty())){
            for (int i=0; i < m_cacheParameter.size(); i++){
                String para = (String)m_cacheParameter.elementAt(i);
                if (parameters.containsKey(para)){
                    key += (String)parameters.get(para);
                }
            }
        }
        if(key.equals("")){
            return null;
        }
        // now add the elementdefinition-parameters
        String elementName = (String)parameters.get("_ELEMENT_");
        if(elementName == null) {
            // this is the root-template
            elementName = I_CmsConstants.C_ROOT_TEMPLATE_NAME;
        }
        Enumeration paramKeys = parameters.keys();
        while(paramKeys.hasMoreElements()) {
            String paramKey = (String) paramKeys.nextElement();
            if(paramKey.startsWith(elementName)) {
                key += paramKey + "=" + parameters.get(paramKey) + ";";
            }
        }
        return key;
    }

    /**
     * defines if the group is part of the cacheKey
     * @param groupCache if true the group will be part of the cache key.
     */
    public void setCacheGroups(boolean groupCache){
        m_group = groupCache;
    }

    /**
     * if this Vector is set the groups are part of the cache key and the element
     * will be cacheable only if the current group is in the Vector. If not it is dynamic.
     * @param groupNames A Vector with the names of the groups for which the element is cacheable.
     */
    public void setCacheGroups(Vector groupNames){
//        m_group = true;
//        m_cacheGroups = groupNames;
    }

    /**
     * includes the user in the cache key.
     * @param userCache if true the user will be part of the cachekey.
     */
    public void setCacheUser(boolean userCache){
        m_user = userCache;
        //autoSetExternalCache();
    }

    /**
     * includes the uri in the cache key.
     * @param uriCache if true the uri will be part of the cachekey.
     */
    public void setCacheUri(boolean uriCache){
        m_uri = uriCache;
        renewAfterEveryPublish = true;
    }

    /**
     * if this Vector is set the values of each of the parameters in the Vector
     * will appear in the cachekey.
     * @param parameterNames the parameter that should take part in the cachkey generation.
     */
    public void setCacheParameters(Vector parameterNames){
        m_cacheParameter = parameterNames;
    }

    /**
     * If one of this parameters apear in the request the element is dynamic! (no cachkey).
     * @param parameterNames the names of the parameter that make a element dynamic
     */
    public void setNoCacheParameters(Vector parameterNames){
        m_dynamicParameter = parameterNames;
    }

    /**
     * Method for setting all caching properties given boolean
     * values.
     * @param internal Initial value for "internal cacheable" property.
     * @param proxyPriv Initial value for "proxy private cacheable" property.
     * @param proxyPub Initial value for "internal cacheable" property.
     * @param export Initial value for "exportable" property.
     * @param stream Initial value for "streamable" property.
     */
    protected void setExternalCaching(boolean internal, boolean proxyPriv, boolean proxyPub, boolean export, boolean stream) {
        m_cd = 0;
        m_cd |= internal?C_CACHE_INTERNAL:0;
        m_cd |= proxyPriv?C_CACHE_PROXY_PRIVATE:0;
        m_cd |= proxyPub?C_CACHE_PROXY_PUBLIC:0;
        m_cd |= export?C_CACHE_EXPORT:0;
        m_cd |= stream?C_CACHE_STREAM:0;
    }

    /**
     * Merge the current CmsCacheDirective object with another cache directive.
     * Resulting properties will be build by a conjunction (logical AND) of
     * the two source properties.
     * @param cd CmsCacheDirectives to be merged.
     */
    public void merge(A_CmsCacheDirectives cd) {
        m_cd &= cd.m_cd;
    }

    /**
     * Get the state of the "proxy private cacheable" property.
     * @return <code>true</code> if proxy private caching is possible, <code>false</code> otherwise.
     */
    public boolean isProxyPrivateCacheable() {
        return (m_cd & C_CACHE_PROXY_PRIVATE) == C_CACHE_PROXY_PRIVATE;
    }

    /**
     * Get the state of the "proxy public cacheable" property.
     * @return <code>true</code> if proxy public caching is possible, <code>false</code> otherwise.
     */
    public boolean isProxyPublicCacheable() {
        return (m_cd & C_CACHE_PROXY_PUBLIC) == C_CACHE_PROXY_PUBLIC;
    }

    /**
     * Get the state of the "exporting ability" property.
     * @return <code>true</code> if exporting is possible, <code>false</code> otherwise.
     */
    public boolean isExportable() {
        return (m_cd & C_CACHE_EXPORT) == C_CACHE_EXPORT;
    }

    /**
     * Get the state of the "streaming ability" property.
     * @return <code>true</code> if streaming is possible, <code>false</code> otherwise.
     */
    public boolean isStreamable() {
        return (m_cd & C_CACHE_STREAM) == C_CACHE_STREAM;
    }

    /**
     * get the userSet information.
     * @return <code>true</code> if the proxyPrivate flag was set, <code>false</code> otherwise.
     */
    public boolean userSetProxyPrivate(){
        return m_userSetProxyPrivate;
    }
    /**
     * get the userSet information.
     * @return <code>true</code> if the proxyPublic flag was set, <code>false</code> otherwise.
     */
    public boolean userSetProxyPublic(){
        return m_userSetProxyPublic;
    }
    /**
     * get the userSet information.
     * @return <code>true</code> if the export flag was set, <code>false</code> otherwise.
     */
    public boolean userSetExport(){
        return m_userSetExport;
    }


}