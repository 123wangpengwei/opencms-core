/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/flex/cache/Attic/CmsFlexCacheEntry.java,v $
 * Date   : $Date: 2002/09/16 11:53:42 $
 * Version: $Revision: 1.7 $
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
 * First created on 9. April 2002, 12:42
 */


package com.opencms.flex.cache;

import com.opencms.flex.util.I_CmsFlexLruCacheObject;

import java.util.Iterator;
import java.util.Map;

/**
 * A CmsFlexCacheEntry describes a cached resource.
 * It is basically a list of pre-generated output,
 * include() calls to other resources and http headers that this 
 * resource requires to be set.<p>
 *
 * An CmsFlexCacheEntry might also describe a redirect-call, but in this case
 * nothing else will be cached.<p>
 *
 * The pre-generated output is saved in byte[] arrays.
 * The include() calls are saved as Strings of the included resource name.
 * The headers are saved in a HashMap.
 * In case of a redirect, the redircet target is cached in a String.<p>
 *
 * The CmsFlexCacheEntry can also have a timeout value, which indicates the time 
 * that his entry will become invalid and should thus be cleared from the cache.<p>
 *
 * @author  Alexander Kandzior (a.kandzior@alkacon.com)
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @see com.opencms.flex.util.I_CmsFlexLruCacheObject
 * @version $Revision: 1.7 $
 */
public class CmsFlexCacheEntry extends Object implements I_CmsFlexLruCacheObject {
    
    /** Initial size for lists */
    public static final int C_INITIAL_CAPACITY_LISTS = 11;
    // Alternatives: 2 3 5 7 11 13 17 19 23 29 31 37 41 43 47 53 59 61 67 71    
    
    /** The list of items for this resource */
    private java.util.List m_elements;
    
    /** A Map of cached headers for this resource */
    private java.util.Map m_headers;
  
    /** A redirection target (if redirection is set) */
    private String m_redirectTarget;
    
    /** Debug switch */
    private static final int DEBUG = 0;
    
    /** Age for timeout */
    private long m_timeout = -1;
    
    /** Indicates if this cache entry is completed */
    private boolean m_completed = false;      
    
    /** The CacheEntry's size in kBytes */
    private int m_byteSize;
    
    /** Pointer to the next cache entry in the LRU cache */
    private I_CmsFlexLruCacheObject m_Next;
    
    /** Pointer to the previous cache entry in the LRU cache. */
    private I_CmsFlexLruCacheObject m_Previous;
    
    /** The variation map where this cache entry is stored. */
    private Map m_VariationMap;
    
    /** The key under which this cache entry is stored in the variation map. */
    private String m_VariationKey;
    
    /** Static counter to give each entry a unique ID. */
    private static int ID_COUNTER = 0;
    
    /** The internal ID of this cache entry. */
    private int ID;
    
    /** 
     * Constructor for class CmsFlexCacheEntry.
     * The way to use this class is to first use this empty constructor 
     * and later add data with the various add methods.
     */
    public CmsFlexCacheEntry() {
        m_elements = new java.util.ArrayList(C_INITIAL_CAPACITY_LISTS);
        m_redirectTarget = null;
        m_headers = null;
        m_byteSize = 0;
        
        this.setNextLruObject( null );
        this.setPreviousLruObject( null );
        
        this.ID = CmsFlexCacheEntry.ID_COUNTER++;
    }
    
    /** 
     * Add an array of bytes to the cache.
     * This will usually be the result of some kind of output - stream.
     *
     * @param bytes The output to save in the cache.
     */    
    public void add(byte[] bytes) {
        if (m_completed) return;
        if (m_redirectTarget == null) {
            // Add only if not already redirected
            m_elements.add(bytes);
            m_byteSize += bytes.length;
        }
    }
    
    /** 
     * Add an include - call target resource to the cache entry.
     *
     * @param resource A name of a resource in the OpenCms VFS.
     * @param parameters A map of parameters specific to this include call.
     */    
    public void add(String resource, java.util.Map paramters) {
        if (m_completed) return;
        if (m_redirectTarget == null) {
            // Add only if not already redirected
            m_elements.add(resource);
            if (paramters == null) paramters = java.util.Collections.EMPTY_MAP;
            m_elements.add(paramters);
            m_byteSize += resource.getBytes().length;
        }
    }
    
    /** 
     * Add a map of headers to the cache entry.
     * The headers are usually collected in the class CmsFlexResponse first.
     *
     * @param headers The map of headers to add to the entry */
    public void addHeaders(java.util.Map headers) {
        if (m_completed) return;
        m_headers = headers;
        
        Iterator allHeaders = m_headers.keySet().iterator();
        while (allHeaders.hasNext()) {
            this.m_byteSize += ((String)allHeaders.next()).getBytes().length;
        }
    }
    
    /** 
     * Set a redirect target for this cache entry.<p>
     *
     * <b>Important:</b>
     * When a redirect target is set, all saved data is thrown away,
     * and new data will not be saved in the cache entry.
     * This is so since with a redirect nothing will be displayed
     * in the browser anyway, so there is no point in saving the data.
     * 
     * @param target The redirect target (must be a valid URL).
     */    
    public void setRedirect(String target) {
        if (m_completed) return;
        m_redirectTarget = target;
        this.m_byteSize = target.getBytes().length;
        // If we have a redirect we don't need any other output or headers
        m_elements = null;
        m_headers = null;
    }
    
    /**
     * Returns the list of data entries of this cache entry.
     * Data entries are byte arrays representing some kind of ouput
     * or Strings representing include calls to other resources.
     *
     * @return The list of data elements of this cache entry.
     */    
    public java.util.List elements() {
        return m_elements;
    }

    /** 
     * Processing method for the cached entry.<p>
     *
     * If this method is called, it delivers the contents of
     * the cached entry to the given request / response.
     * This includes calls to all included resources.<p>
     *
     * @param req The request from the client.
     * @param res The server response.
     * @throws CmsException Is thrown when problems writing to the response output-stream occur
     * @throws ServletException Might be thrown from call to RequestDispatcher.include()
     * @throws IOException Might be thrown from call to RequestDispatcher.include() or from Response.sendRedirect()
     */
    public void service(CmsFlexRequest req, CmsFlexResponse res) 
    throws com.opencms.core.CmsException, javax.servlet.ServletException, java.io.IOException {
        if (!m_completed) return;

        if (m_redirectTarget != null) {
            res.setOnlyBuffering(false);
            // Redirect the response, no further output required
            res.sendRedirect(m_redirectTarget);
        } else {      
            // Process cached headers first
            res.processHeaders(m_headers, res);
            // Check if this cache entry is a "leaf" (i.e. no further includes)            
            boolean hasNoSubElements = ((m_elements != null) && (m_elements.size() == 1));            
            // Write output to stream and process all included elements
            java.util.Iterator i = m_elements.iterator();
            while (i.hasNext()) {
                Object o = i.next();
                if (o instanceof String) {                    
                    // Handle cached parameters
                    java.util.Map map = (java.util.Map)i.next();                    
                    java.util.Map oldMap = null;
                    if (map.size() > 0) {
                        oldMap = req.getParameterMap();
                        req.addParameterMap(map);
                    }
                    // Do the include call
                    req.getCmsRequestDispatcher((String)o).include(req, res);
                    // Reset parameters if neccessary
                    if (oldMap != null) req.setParameterMap(oldMap);
                } else {
                    try {
                        res.writeToOutputStream((byte[])o, hasNoSubElements);
                    } catch (java.io.IOException e) {
                        String err = this.getClass().getName() + ": Could not write to response OutputStream. ";
                        if (DEBUG > 0) System.err.println(err);
                        throw new com.opencms.core.CmsException(err + "\n" + e, e);
                    }
                }
            }
        }
    }
    
    /** 
     * Returns the timeout - value of this cache entry.
     * This is set to the time when the entry becomes invalid.
     *
     * @return  The timeout value for this resource
     */ 
    public long getTimeout() {
        return m_timeout;
    }
    
    /**
     * Sets a timeout value to the entry. 
     * A timout value indicates the time the entry should become invalid.<p>
     *
     * The timeout parameter represents the minute - intervall in which the cache entry
     * is to be cleared. 
     * The intervall always starts at 0.00h. 
     * A value of 60 would indicate that this entry will reach it's timeout at the beginning of the next 
     * full hour, a timeout of 20 would indicate that the entry is invalidated at x.00, x.20 and x.40 of every hour etc.
     *
     * @param timeout The timeout value to be set
     */
    public synchronized void setTimeout(long timeout) {
        if (timeout < 0 || ! m_completed) return;
        
        long now = System.currentTimeMillis();
        long daytime = now % 86400000;
        m_timeout = now - (daytime % timeout) + timeout;
        if (DEBUG > 2) System.err.println("FlexCacheEntry: New entry timeout=" + m_timeout + " now=" + now + " remaining=" + (m_timeout - now) );
    } 
    
    /**
     * Completes this cache entry.
     * A completed cache entry is made "unmodifyable",
     * so that no further data can be added and existing data can not be changed.
     * This is to prevend the (unlikley) case that some user-written class 
     * tries to make changes to a cache entry.
     */
    public void complete() {        
        m_completed = true;
        // Prevent changing of the cached lists
        if (m_headers != null) {
            m_headers = java.util.Collections.unmodifiableMap(m_headers);
        }
        if (m_elements != null) {
            m_elements = java.util.Collections.unmodifiableList(m_elements);
        }
        if (DEBUG > 1) System.err.println("CmsFlexCacheEntry: New entry completed:\n" + this.toString());
    }
    
    /** 
     * Overloaded from Object.toString()
     *
     * @return A basic String representation of this CmsFlexCache entry.
     */    
    public String toString() {
        String str = null;
        if (m_redirectTarget == null) {
            str = "CmsFlexCacheEntry [" + m_elements.size() + " Elements/" + this.getLruCacheCosts() + " bytes]\n";
            java.util.Iterator i = m_elements.iterator();
            int count = 0;
            while (i.hasNext()) {
                count++;
                Object o = i.next();
                if (o instanceof String) {
                    str += "" + count + " - <cms:include target=" + o + ">\n";
                } else {
                    str += "" + count + " - <![CDATA[" + new String((byte[])o) + "]]>\n";
                }
            }
        } else {
            str = "CmsFlexCacheEntry [Redirect to target=" + m_redirectTarget + "]";
        }
        return str;
    }   
    
    /**
     * Stores a backward reference to the map and key where this cache entry is stored.
     */
    public void setVariationData( String theVariationKey, Map theVariationMap ) {
        this.m_VariationKey = theVariationKey;
        this.m_VariationMap = theVariationMap;
    }
    
    // implementation of the com.opencms.flex.util.I_CmsFlexLruCacheObject interface methods
    
    /** 
     * Set the next object in the double linked list of all cached objects. 
     */
    public void setNextLruObject( I_CmsFlexLruCacheObject theNextEntry ) {
        this.m_Next = theNextEntry;
    }
    
    /** 
     * Get the next object in the double linked list of all cached objects. 
     */
    public I_CmsFlexLruCacheObject getNextLruObject() {
        return this.m_Next;
    }
    
    /** 
     * Set the previous object in the double linked list of all cached objects. 
     */
    public void setPreviousLruObject( I_CmsFlexLruCacheObject thePreviousEntry ) {
        this.m_Previous = thePreviousEntry;
    }
    
    /** 
     * Get the previous object in the double linked list of all cached objects. 
     */
    public I_CmsFlexLruCacheObject getPreviousLruObject() {
        return this.m_Previous;
    }  
    
    /** 
     * This method is invoked after the object was added to the cache. 
     */
    public void addToLruCache() {
        // do nothing here...
        if (DEBUG>0) System.out.println( "Added cache entry with ID: " + this.ID + " to the LRU cache" );
    }
    
    /** 
     * This method is invoked after the object was removed to the cache. 
     */
    public void removeFromLruCache() {
        if (m_VariationMap!=null && this.m_VariationKey!=null) {
            this.m_VariationMap.remove( this.m_VariationKey );
            if (DEBUG>0) System.err.println( "Removed cache entry with ID: " + this.ID + " from the LRU cache" );
        }
    }
    
    /** 
     * Returns the byte size as the cache costs of this object. 
     */
    public int getLruCacheCosts() {
        return m_byteSize;
    }
    
    // methods to clean-up/finalize the object instance
    
    /**
     * Finalize this instance.
     */
    protected void finalize() throws java.lang.Throwable {
        if (DEBUG>0) System.err.println( "Finalizing cache entry with ID: " + this.ID );
        
        this.clear();
        
        this.m_elements = null;
        this.m_headers = null;
        
        this.m_VariationKey = null;
        this.m_VariationMap = null;        
        
        this.setNextLruObject( null );
        this.setPreviousLruObject( null );
        
        this.m_byteSize = 0;  
        
        super.finalize();      
    }
    
    /**
     * Clears the elements and headers HashMaps.
     */
    public void clear() {
        m_elements.clear();
        m_headers.clear();
    }
}
