/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/flex/util/Attic/CmsFlexLruCache.java,v $
 * Date   : $Date: 2002/09/16 11:45:02 $
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
 */

package com.opencms.flex.util;

import java.util.*;

/**
 * The idea of this cache is, to separate the caching policy from the data structure
 * where the cached objects are stored. The advantage of doing so is, that the CmsFlexLruCache
 * can identify the last-recently-used object in O(1), whereas you would need at least
 * O(n) to traverse the data structure that stores the cached objects. Second, you can
 * easily use the CmsFlexLruCache to get an LRU cache, no matter what data structure is used to
 * store your objects.
 * <p>
 * The cache policy is affected by the "costs" of the objects being cached. Valuable cache costs
 * might be the byte size of the cached objects for example.
 * <p>
 * To add/remove cached objects from the data structure that stores them, the objects have to
 * implement the methods defined in the interface I_CmsFlexLruCacheObject to be notified when they
 * are added/removed from the CmsFlexLruCache.
 *
 * @see com.opencms.flex.util.I_CmsFlexLruCacheObject
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @version $Revision: 1.4 $
 */
public class CmsFlexLruCache extends java.lang.Object {
    
    /** The head of the list of double linked LRU cache objects. */
    private I_CmsFlexLruCacheObject m_ListHead;
    
    /** The tail of the list of double linked LRU cache objects. */
    private I_CmsFlexLruCacheObject m_ListTail;
    
    /** Force a finalization after down-sizing the cache? */
    private boolean m_ForceFinalization;
    
    /** The max. sum of costs the cached objects might reach. */
    private int m_MaxCacheCosts;
    
    /** The avg. sum of costs the cached objects. */
    private int m_AvgCacheCosts;
    
    /** The max. costs of cacheable objects. */
    private int m_MaxObjectCosts;
    
    /** The costs of all cached objects. */
    private int m_ObjectCosts;
    
    /** The sum of all cached objects. */
    private int m_ObjectCount;
    
    /** Internal debug switch */
    private static final int DEBUG = 0;
    
    
    /**
     * The constructor with all options.
     *
     * @param theMaxCacheCosts the max. cache costs of all cached objects
     * @param theAvgCacheCosts the avg. cache costs of all cached objects
     * @param theMaxObjectCosts the max. allowed cache costs per object. Set theMaxObjectCosts to -1 if you don't want to limit the max. allowed cache costs per object
     * @param forceFinalization should be true if a system wide garbage collection/finalization is forced after objects were removed from the cache
     */
    public CmsFlexLruCache( int theMaxCacheCosts, int theAvgCacheCosts, int theMaxObjectCosts, boolean forceFinalization ) {
        this.m_ForceFinalization = forceFinalization;
        this.m_MaxCacheCosts = theMaxCacheCosts;
        this.m_AvgCacheCosts = theAvgCacheCosts;
        this.m_MaxObjectCosts = theMaxObjectCosts;
        
        this.m_ObjectCosts = this.m_ObjectCount = 0;
        this.m_ListHead = this.m_ListTail = null;
    }
    
    /**
     * Constructor for a LRU cache with forced garbage collection/finalization.
     *
     * @param theMaxCacheCosts the max. cache costs of all cached objects
     * @param theAvgCacheCosts the avg. cache costs of all cached objects
     * @param theMaxObjectCosts the max. allowed cache costs per object. Set theMaxObjectCosts to -1 if you don't want to limit the max. allowed cache costs per object
     */
    public CmsFlexLruCache( int theMaxCacheCosts, int theAvgCacheCosts, int theMaxObjectCosts ) {
        this( theMaxCacheCosts, theAvgCacheCosts, theMaxObjectCosts, false );
    }
    
    /**
     * Constructor for a LRU cache with forced garbage collection/finalization, the max. allowed costs of cacheable
     * objects is 1/4 of the max. costs of all cached objects.
     *
     * @param theMaxCacheCosts the max. cache costs of all cached objects
     * @param theAvgCacheCosts the avg. cache costs of all cached objects
     */
    public CmsFlexLruCache( int theMaxCacheCosts, int theAvgCacheCosts ) {
        this( theMaxCacheCosts, theAvgCacheCosts, theMaxCacheCosts/4, false );
    }
    
    /**
     * Constructor for a LRU cache where the max. allowed costs of cacheable objects is 1/4 of the max. costs of all cached objects.
     *
     * @param theMaxCacheCosts the max. cache costs of all cached objects
     * @param theAvgCacheCosts the avg. cache costs of all cached objects
     * @param forceFinalization should be true if a system wide garbage collection/finalization is forced after objects were removed from the cache
     */
    public CmsFlexLruCache( int theMaxCacheCosts, int theAvgCacheCosts, boolean forceFinalization ) {
        this( theMaxCacheCosts, theAvgCacheCosts, theMaxCacheCosts/4, forceFinalization );
    }
    
    /**
     * Returns a string representing the current state of the cache.
     */
    public String toString() {
        String objectInfo = "\n";
        
        objectInfo += "max. cache costs of all cached objects: " + this.m_MaxCacheCosts + "\n";
        objectInfo += "avg. cache costs of all cached objects: " + this.m_AvgCacheCosts + "\n";
        objectInfo += "max. cache costs per object: " + this.m_MaxObjectCosts + "\n";
        objectInfo += "costs of all cached objects: " + this.m_ObjectCosts + "\n";
        objectInfo += "sum of all cached objects: " + this.m_ObjectCount + "\n";
        
        if (!this.m_ForceFinalization) {
            objectInfo += "no ";
        }
        objectInfo += "system garbage collection is forced during clean up\n";
        
        return objectInfo;
    }
    
    /**
     * Add a new object to the cache. If you try to add the same object more than once,
     * the object is touched instead.
     *
     * @param theCacheObject the object being added to the cache
     * @return true if the object was added to the cache, false if the object was denied because its cache costs were higher than the allowed max. cache costs per object
     */
    public synchronized boolean add( I_CmsFlexLruCacheObject theCacheObject ) {
        boolean retVal = false;
        
        if (theCacheObject==null) {
            // null can't be added or touched in the cache 
            return false;
        }
        
        // only objects with cache costs < the max. allowed object cache costs can be cached!
        if ( (this.m_MaxObjectCosts!=-1) && (theCacheObject.getLruCacheCosts()>this.m_MaxObjectCosts) ) {
            this.log( "you are trying to cache objects with cache costs " + theCacheObject.getLruCacheCosts() + " which is bigger than the max. allowed costs " + this.m_MaxObjectCosts );
            return false;
        }
        
        if (!this.isCached(theCacheObject)) {
            // add the object to the list of all cached objects in the cache
            this.addHead( theCacheObject );
        }
        else {
            this.touch( theCacheObject );
        }
        
        // check if the cache has to trash the last-recently-used objects before adding a new object
        if (this.m_ObjectCosts>this.m_MaxCacheCosts) {
            this.gc();
        }
        
        return true;
    }
    
    /**
     * Test if a given object resides inside the cache.
     * @return true if the object is inside the cache, false otherwise
     */
    private boolean isCached( I_CmsFlexLruCacheObject theCacheObject ) {
        if (theCacheObject==null || this.m_ObjectCount==0) {
            // the cache is empty or the object is null (which is never cached)
            return false;
        }
        
        if (theCacheObject.getNextLruObject()!=null || theCacheObject.getPreviousLruObject()!=null) { 
            // the object has either a predecessor or successor in the linked 
            // list of all cached objects, so it is inside the cache
            return true;
        }
        
        if (theCacheObject.getNextLruObject()==null && theCacheObject.getPreviousLruObject()==null) {
            if (this.m_ObjectCount==1 && this.m_ListHead.equals(theCacheObject) && this.m_ListTail.equals(theCacheObject)) {
                // the object is the one and only object in the cache
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Touch an existing object in the cache in the sense that it's "last-recently-used" state
     * is updated.
     *
     * @param theCacheObject the object being touched
     */
    public synchronized boolean touch( I_CmsFlexLruCacheObject theCacheObject ) {
        if (!this.isCached(theCacheObject)) return false;
        
        // only objects with cache costs < the max. allowed object cache costs can be cached!
        if ( (this.m_MaxObjectCosts!=-1) && (theCacheObject.getLruCacheCosts()>this.m_MaxObjectCosts) ) {
            this.log( "you are trying to cache objects with cache costs " + theCacheObject.getLruCacheCosts() + " which is bigger than the max. allowed costs " + this.m_MaxObjectCosts );
            this.remove( theCacheObject );
            return false;
        }
                
        // set the list pointers correct
        if (theCacheObject.getNextLruObject()==null) {
            // case 1: the object is already at the head pos.
            return true;
        }
        else if (theCacheObject.getPreviousLruObject()==null) {
            // case 2: the object at the tail pos., remove it from the tail to put it to the front as the new head
            I_CmsFlexLruCacheObject newTail = theCacheObject.getNextLruObject();
            newTail.setPreviousLruObject( null );
            this.m_ListTail = newTail;
        }
        else {
            // case 3: the object is somewhere within the list, remove it to put it the front as the new head
            theCacheObject.getPreviousLruObject().setNextLruObject( theCacheObject.getNextLruObject() );
            theCacheObject.getNextLruObject().setPreviousLruObject( theCacheObject.getPreviousLruObject() );
        }
        
        // set the touched object as the new head in the linked list:
        I_CmsFlexLruCacheObject oldHead = this.m_ListHead;
        oldHead.setNextLruObject( theCacheObject );
        theCacheObject.setNextLruObject( null );
        theCacheObject.setPreviousLruObject( oldHead );
        this.m_ListHead = theCacheObject;
        
        return true;
    }
    
    /**
     * Adds a cache object as the new haed to the list of all cached objects.
     *
     * @param theCacheObject the object being added as the new head to the list of all cached objects
     */
    private void addHead( I_CmsFlexLruCacheObject theCacheObject ) {
        // set the list pointers correct
        if (this.m_ObjectCount>0) {
            // there is at least 1 object already in the list
            I_CmsFlexLruCacheObject oldHead = this.m_ListHead;
            oldHead.setNextLruObject( theCacheObject );
            theCacheObject.setPreviousLruObject( oldHead );
            this.m_ListHead = theCacheObject;
        }
        else {
            // it is the first object to be added to the list
            this.m_ListTail = this.m_ListHead = theCacheObject;
            theCacheObject.setPreviousLruObject( null );
        }
        theCacheObject.setNextLruObject( null );
        
        // update cache stats. and notify the cached object
        this.increaseCache( theCacheObject );
    }
    
    /**
     * Removes an object from the list of all cached objects,
     * no matter what position it has inside the list.
     *
     * @param theCacheObject the object being removed from the list of all cached objects
     * @return a reference to the object that was removed
     */
    public synchronized I_CmsFlexLruCacheObject remove( I_CmsFlexLruCacheObject theCacheObject ) {
        if (!this.isCached(theCacheObject)) {
            // theCacheObject is null or not inside the cache
            return null;
        }
        
        // set the list pointers correct
        if (theCacheObject.getNextLruObject()==null) {
            // remove the object from the head pos.
            I_CmsFlexLruCacheObject newHead = theCacheObject.getPreviousLruObject();
            
            if (newHead!=null) {
                // if newHead is null, theCacheObject 
                // was the only object in the cache
                newHead.setNextLruObject( null );
            }
            
            this.m_ListHead = newHead;
        }
        else if (theCacheObject.getPreviousLruObject()==null) {
            // remove the object from the tail pos.
            I_CmsFlexLruCacheObject newTail = theCacheObject.getNextLruObject();
            
            if (newTail!=null) {
                // if newTail is null, theCacheObject 
                // was the only object in the cache                
                newTail.setPreviousLruObject( null );
            }
            
            this.m_ListTail = newTail;
        }
        else {
            // remove the object from within the list
            theCacheObject.getPreviousLruObject().setNextLruObject( theCacheObject.getNextLruObject() );
            theCacheObject.getNextLruObject().setPreviousLruObject( theCacheObject.getPreviousLruObject() );
        }
        
        // update cache stats. and notify the cached object
        this.decreaseCache( theCacheObject );
        
        return theCacheObject;
    }
    
    /**
     * Removes the tailing object from the list of all cached objects.
     */
    private synchronized void removeTail() {
        I_CmsFlexLruCacheObject oldTail = null;
        
        if ((oldTail=this.m_ListTail)!=null) {
            I_CmsFlexLruCacheObject newTail = oldTail.getNextLruObject();
            
            // set the list pointers correct
            if (newTail!=null) {
                // there are still objects remaining in the list
                newTail.setPreviousLruObject( null );
                this.m_ListTail = newTail;
            }
            else {
                // we removed the last object from the list
                this.m_ListTail = this.m_ListHead = null;
            }
            
            // update cache stats. and notify the cached object
            this.decreaseCache( oldTail );
        }
    }
    
    /**
     * Decrease the cache stats. and notify the cached object that it was removed from the cache.
     *
     * @param theCacheObject the object being notified that it was removed from the cache
     */
    private void decreaseCache( I_CmsFlexLruCacheObject theCacheObject ) {
        // notify the object that it was now removed from the cache
        //theCacheObject.notify();
        theCacheObject.removeFromLruCache();
        
        // set the list pointers to null
        theCacheObject.setNextLruObject( null );
        theCacheObject.setPreviousLruObject( null );
        
        // update the cache stats.
        this.m_ObjectCosts -= theCacheObject.getLruCacheCosts();
        this.m_ObjectCount--;
    }
    
    /**
     * Increase the cache stats. and notify the cached object that it was added to the cache.
     *
     * @param theCacheObject the object being notified that it was added to the cache
     */
    private void increaseCache( I_CmsFlexLruCacheObject theCacheObject ) {
        // notify the object that it was now added to the cache
        //theCacheObject.notify();
        theCacheObject.addToLruCache();
        
        // update the cache stats.
        this.m_ObjectCosts += theCacheObject.getLruCacheCosts();
        this.m_ObjectCount++;
    }
    
    /**
     * Removes the last recently used objects from the list of all cached objects as long
     * as the costs of all cached objects are higher than the allowed avg. costs of the cache.
     */
    private void gc() {       
        I_CmsFlexLruCacheObject currentObject = this.m_ListTail;
        while (currentObject!=null) {
            if (this.m_ObjectCosts<this.m_AvgCacheCosts) break;
            currentObject = currentObject.getNextLruObject();
            this.removeTail();
        }
        
        if (m_ForceFinalization) {
            // force a finalization/system garbage collection optionally
            Runtime.getRuntime().runFinalization();
            System.gc();
        }
    }
    
    /**
     * Write a message to the log media.
     *
     * @param theLogMessage the message being logged
     */
    private void log( String theLogMessage ) {
        if (com.opencms.core.A_OpenCms.isLogging() && com.opencms.boot.I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING) {
            com.opencms.boot.CmsBase.log(com.opencms.boot.CmsBase.C_FLEX_CACHE, "[" + this.getClass().getName() + "] " + theLogMessage );
        }
    }
    
    /**
     * Returns the count of all cached objects.
     *
     * @return the count of all cached objects
     */
    public int size() {
        return this.m_ObjectCount;
    }
    
    /**
     * Clears the cache for finalization.
     */
    protected void finalize() throws java.lang.Throwable {
        this.clear();
        super.finalize();
    }
    
    /**
     * Removes all cached objects and resets the internal cache.
     */
    public void clear() {
        // remove all objects from the linked list from the tail to the head:
        I_CmsFlexLruCacheObject currentObject = this.m_ListTail;
        while (currentObject!=null) {
            currentObject = currentObject.getNextLruObject();
            this.removeTail();
        }
        
        // reset the data structure
        this.m_ObjectCosts = this.m_ObjectCount = 0;
        this.m_ListHead = this.m_ListTail = null;
        
        if (m_ForceFinalization) {
            // force a finalization/system garbage collection optionally
            Runtime.getRuntime().runFinalization();
            System.gc();
        }        
    }
    
	/**
	 * Returns the avg. costs of all cached objects.
	 * @return the avg. costs of all cached objects
	 */
	public int getAvgCacheCosts() {
		return this.m_AvgCacheCosts;
	}

	/**
	 * Returns the max. costs of all cached objects.
	 * @return the max. costs of all cached objects
	 */
	public int getMaxCacheCosts() {
		return this.m_MaxCacheCosts;
	}

	/**
	 * Returns the max. allowed costs per cached object.
	 * @return the max. allowed costs per cached object
	 */
	public int getMaxObjectCosts() {
		return this.m_MaxObjectCosts;
	}

	/**
	 * Returns the cur. costs of all cached objects.
	 * @return the cur. costs of all cached objects
	 */
	public int getObjectCosts() {
		return this.m_ObjectCosts;
	}

}
