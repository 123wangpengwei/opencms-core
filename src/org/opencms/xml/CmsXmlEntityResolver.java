/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/xml/CmsXmlEntityResolver.java,v $
 * Date   : $Date: 2004/06/10 12:32:53 $
 * Version: $Revision: 1.3 $
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

package org.opencms.xml;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.main.CmsEvent;
import org.opencms.main.I_CmsEventListener;
import org.opencms.main.OpenCms;
import org.opencms.xml.page.CmsXmlPage;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * Resolves XML entities (e.g. external DTDs) in the OpenCms VFS.<p>
 * 
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * @version $Revision: 1.3 $ 
 */
public class CmsXmlEntityResolver implements EntityResolver, I_CmsEventListener {

    /** The scheme to identify a file in the OpenCms VFS */
    public static final String C_OPENCMS_SCHEME = "opencms://";

    /** The location of the xmlpage DTD */
    private static final String C_XMLPAGE_DTD_LOCATION = "org/opencms/xml/page/xmlpage.dtd";
    
    /** The (old) DTD address of the OpenCms xmlpage (used until 5.3.5) */
    public static final String C_XMLPAGE_DTD_OLD_SYSTEM_ID = "/system/shared/page.dtd";    
    
    /** The static default entity resolver for reading / writing xml content */
    private static CmsXmlEntityResolver m_resolver;        

    /** A permanent cache to avoid multiple readings of often used files from the VFS */
    private Map m_cachePermanent;
    
    /** A temporary cache to avoid multiple readings of often used files from the VFS */
    private Map m_cacheTemporary;

    /** The cms object to use for VFS access (will be initialized with "Guest" permissions) */
    private CmsObject m_cms;
    
    /**
     * Creates a new entity resolver to read the xmlpage dtd.
     */
    public CmsXmlEntityResolver() {

        LRUMap lruMap = new LRUMap(128);
        m_cacheTemporary = Collections.synchronizedMap(lruMap);
        
        HashMap hashMap = new HashMap(32);
        m_cachePermanent = Collections.synchronizedMap(hashMap);
        
        // check required for unit tests where no OpenCms is available
        if (OpenCms.getRunLevel() > 1) {
            
            m_cms = OpenCms.initCmsObject(OpenCms.getDefaultUsers().getUserGuest());            
            if ((OpenCms.getMemoryMonitor() != null) && OpenCms.getMemoryMonitor().enabled()) {
                // map must be of type "LRUMap" so that memory monitor can acecss all information
                OpenCms.getMemoryMonitor().register(this.getClass().getName() + "." + "m_cacheTemporary", lruMap);
                // map must be of type "HashMap" so that memory monitor can acecss all information
                OpenCms.getMemoryMonitor().register(this.getClass().getName() + "." + "m_cachePermanent", hashMap);
            }
            // register this object as event listener
            OpenCms.addCmsEventListener(this, new int[] {
                I_CmsEventListener.EVENT_CLEAR_CACHES,
                I_CmsEventListener.EVENT_PUBLISH_PROJECT
            });            
        }
    }
    
    /**
     * Returns a static default entity resolver for reading / writing xml content
     * from the OpenCms VFS.<p>
     * 
     * @return a static default entity resolver
     */
    public static synchronized CmsXmlEntityResolver getResolver() {
        
        if (m_resolver == null) {
            try {
                m_resolver = new CmsXmlEntityResolver();
            } catch (Throwable t) {
                // might happen if OpenCms was not initialized 
                m_resolver = null;
            }
        }
        return m_resolver;
    }
    
    /**
     * Adds a sytem id URL to to internal cache.<p>
     * 
     * @param systemId the system id to add
     * @param content the content of the system id
     * @param permanent if truethe system id will be stored permanently, if false
     * it will be cleared after each publish or clear cache event
     */
    public void cacheSystemId(String systemId, byte[] content, boolean permanent) {
        
        if (permanent) {
            m_cachePermanent.put(systemId, content);
        } else {
            m_cacheTemporary.put(systemId, content);
        }
    }

    /**
     * @see org.opencms.main.I_CmsEventListener#cmsEvent(org.opencms.main.CmsEvent)
     */
    public void cmsEvent(CmsEvent event) {

        switch (event.getType()) {
            case I_CmsEventListener.EVENT_PUBLISH_PROJECT:
            case I_CmsEventListener.EVENT_CLEAR_CACHES:
                // flush cache   
                m_cacheTemporary.clear();
                if (OpenCms.getLog(this).isDebugEnabled()) {
                    OpenCms.getLog(this).debug("Xml entity resolver flushed caches after recieving clearcache event");
                }
                break;
            default:
                // no operation
        }
    } 

    /**
     * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String, java.lang.String)
     */
    public InputSource resolveEntity(String publicId, String systemId) {

        // lookup the system id caches first
        byte[] content;        
        content = (byte[])m_cacheTemporary.get(systemId);
        if (content != null) {
            return new InputSource(new ByteArrayInputStream(content));
        } 
        content = (byte[])m_cachePermanent.get(systemId);
        if (content != null) {
            return new InputSource(new ByteArrayInputStream(content));
        } 
        
        if (systemId.equals(CmsXmlPage.C_XMLPAGE_DTD_SYSTEM_ID) || systemId.endsWith(C_XMLPAGE_DTD_OLD_SYSTEM_ID)) {
            // xml page DTD
            try {
                return new InputSource(getClass().getClassLoader().getResourceAsStream(C_XMLPAGE_DTD_LOCATION));
            } catch (Throwable t) {
                OpenCms.getLog(this).error("Did not find CmsXmlPage DTD at " + C_XMLPAGE_DTD_LOCATION);
            }
        } else if (systemId.startsWith(C_OPENCMS_SCHEME)) {
            // opencms VFS reference
            String uri = systemId.substring(C_OPENCMS_SCHEME.length()-1);
            try {      
                // content not cached, read from VFS
                CmsFile file = m_cms.readFile(uri);                    
                content = file.getContents();
                // store content in cache
                cacheSystemId(systemId, content, false);
                return new InputSource(new ByteArrayInputStream(content));                
            } catch (Throwable t) {
                OpenCms.getLog(this).error("Could not resolve OpenCms xml entity reference '" + systemId + "'", t);
            }      
        }
        
        // use the default behaviour (i.e. resolve through external URL)
        return null;
    }
}