/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/flex/cache/Attic/CmsFlexCacheKey.java,v $
 * Date   : $Date: 2002/11/16 13:16:32 $
 * Version: $Revision: 1.3 $
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
 * First created on 9. April 2002, 12:44
 */


package com.opencms.flex.cache;

import java.util.Iterator;

/**
 * This class implements the CmsFlexCacheKey.
 * This is a key used to describe the caching behaviour
 * of a specific resource.
 *
 * It has a lot of "public" variables (which isn't good style, I know)
 * to avoid method calling overhead (a cache is about speed, isn't it :)
 *
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * @version $Revision: 1.3 $
 */
public class CmsFlexCacheKey {
    
    /** The OpenCms resource that this key is used for. */    
    public String Resource = null;
    
    /** The cache behaviour description for the resource. */    
    public String Variation = null;

    /** Cache key variable: Determines if this resource can be cached alwys, never or under certain conditions. -1 = never, 0=check, 1=always */
    public int m_always = -1; // 
    
    /** Cache key variable: The uri of the original request */
    public String m_uri = null;
    
    /** Cache key variable: The user id */
    public int m_user = -1;
    
    /** Cache key variable: List of groups */
    public java.util.Set m_groups = null;

    /** Cache key variable: List of parameters */
    public java.util.Map m_params = null;
    
    /** Cache key variable: List of "blocking" parameters */
    public java.util.Set m_noparams = null;
    
    /** Cache key variable: Timeout of the resource */
    public long m_timeout = -1;
    
    /** Cache key variable: Determines if the resource sould be always cleared at publish time */
    public boolean m_publish = false;
        
    /** Cache key variable: Distinguishes request schemes (http, https etc.) */
    public java.util.Set m_schemes = null;
    
    /** Cache key variable: The request TCP/IP port */
    public java.util.Set m_ports = null;

    /** The list of keywords of the Flex cache language */
    private java.util.List cacheCmds = java.util.Arrays.asList(new String[] {
        "always", "never", "uri", "user", "groups", "params", "no-params", "timeout", "publish-clear", "schemes", "ports", "false", "parse-error"} );
    //   0         1        2      3       4         5         6            7          8                9          10       11       12
    
    /** Flag used to determine if this key is from a request or not */
    private boolean m_isRequest;
    
    /** Flag raised in case a key parse error occured */
    private boolean m_parseError = false;
    
    /** Debugging flag */
    private static final boolean DEBUG = false;
    
    /**
     * This constructor is used when building a cache key from a request.
     * The request contains several data items that are neccessary to construct
     * the output. These items are e.g. the Query-String, the requested resource,
     * the current time etc. etc.
     * All required items are saved in the constructed cache - key.
     *
     * @param target The requested resource in the OpenCms VFS
     * @param online Must be true for an online resource, false for offline resources
     * @param request The request to construct the key for.
     */    
    public CmsFlexCacheKey(CmsFlexRequest request, String target, boolean online) {
                
        Resource = getKeyName(target, online);     
        Variation = "never";
        
        m_isRequest = true;
        // Fetch the cms from the request
        com.opencms.file.CmsObject cms = request.getCmsObject();        
        // Get the top-level file name / uri
        // m_uri = request.getCmsFile().getAbsolutePath();
        m_uri = request.getCmsObject().getRequestContext().getUri();
        // Fetch user from the current cms
        m_user = cms.getRequestContext().currentUser().getId();        
        // Fetch group. Must have unique names, so the String is ok
        m_groups = java.util.Collections.singleton(cms.getRequestContext().currentGroup().getName().toLowerCase());
        // Get the params
        m_params = request.getParameterMap();
        if (m_params.size() == 0) m_params = null;
        // No-params are null for a request key
        m_noparams = null;
        // Save the request time 
        m_timeout = System.currentTimeMillis();
        // publish-clear is not related to the request
        m_publish = false;
        // alwalys is not related to the request 
        m_always = 0;
        // Save the request scheme
        m_schemes = java.util.Collections.singleton(request.getScheme().toLowerCase());
        // Save the request port
        m_ports = java.util.Collections.singleton(new Integer(request.getServerPort()));
        if (DEBUG) System.err.println("Creating CmsFlexCacheKey for Request:\n" + this.toString());        
    }
    
    /**
     * This constructor is used when building a cache key from a response.
     * A key for a response uses the properties of the requested resource and looks
     * there for a property called "cache". 
     * The value of this poperty that is passed in this constructor as "cacheDirectives" 
     * is parsed to build the keys data structure.<p>
     *
     * In case a parsing error occures, the value of this key is set to "cache=never", 
     * and the hadParseError() flag is set to true. 
     * This is done to ensure that a valid key is always constructed with the constructor.
     *
     * @param target The requested resource
     * @param cacheDirectives The cache directives of the resource (value of the property "cache")
     * @param online Must be true for an online resource, false for offline resources
     * @param response The response to construct the key from. 
     */        
    public CmsFlexCacheKey(CmsFlexResponse response, String target, String cacheDirectives, boolean online) {
        Resource = getKeyName(target, online);     
        Variation = "never";
        m_isRequest = false;
        if (cacheDirectives != null) parseFlexKey(cacheDirectives);
        if (DEBUG) System.err.println("CmsFlexCacheKey for response generated:\n" + this.toString());
    }
    
    /**
     * Calculates the cache key name that is used as key in 
     * the first level of the FlexCache.
     *
     * @param name The name of the resource
     * @param online Must be true for an online resource, false for offline resources
     * @return The FlexCache key name
     */
    public static String getKeyName(String name, boolean online) {
        return name + (online?CmsFlexCache.C_CACHE_ONLINESUFFIX:CmsFlexCache.C_CACHE_OFFLINESUFFIX);             
    }
    
    /**
     * This flag is used to indicate that a parse error had
     * occured, which can happen if the cache directives String
     * passed to the constructor using the response is
     * not build according to the Flex cache language syntax.
     * @return true if a parse error did occur, false otherwise
     */    
    public boolean hadParseError() {
        return m_parseError;
    }
        
     /**
      * This method is the "heart" of the key matching process.
      * This key is compared to the other key passed as parameter.
      * From comparing the two keys, a variation String is constructed.<p>
      *
      * The assumtion is that this key should be the one constructed for the response, 
      * while the parameter key should have been constructed from the request.<p>
      *
      * A short example how this works:
      * If the resource key is "cache=groups" and the request is done from a guest user
      * (which always belongs to the default group "guests"),
      * the constructed variation will be "cache=(guests)".
      * 
      * @param key The key to match this key with
      * @return null if not cachable, or the Variation String if cachable
      */
    public String matchRequestKey(CmsFlexCacheKey key) {
        
        StringBuffer str = new StringBuffer(100);
        if (m_always < 0) {
            if (DEBUG) System.err.println("keymatch: cache=never");
            return null;
        } 
               
        if (DEBUG) System.err.println("keymatch: Checking no-params");
        if ((m_noparams != null) && (key.m_params != null)) {
            if ((m_noparams.size() == 0) && (key.m_params.size() > 0)) return null;
            Iterator i = key.m_params.keySet().iterator();
            while (i.hasNext()) {
                if (m_noparams.contains(i.next())) return null;
            }
        }

        if (m_always > 0) {
            if (DEBUG) System.err.println("keymatch: cache=always");
            str.append("always");
            return str.toString();
        }
        
        if (DEBUG) System.err.println("keymatch: Checking groups");
        if (m_groups != null) {
            String g = (String)key.m_groups.iterator().next();
            if (DEBUG) System.err.println("keymatch: Request group is " + g);
            if ((m_groups.size() > 0) && ! m_groups.contains(g)) return null;
            str.append("groups=(");
            str.append(g);
            str.append(");");
        }
        
        if (m_uri != null) {
            str.append("uri=(");
            str.append(key.m_uri);
            str.append(");");
        }
        
        if (m_user > 0) {
            str.append("user=(");
            str.append(key.m_user);
            str.append(");");
        }
        
        if (m_params != null) {
            str.append("params=(");
            if (key.m_params != null) {
                if (m_params.size() > 0) {
                    // Match only params listed in cache directives
                    Iterator i = m_params.keySet().iterator();            
                    while (i.hasNext()) {
                        Object o = i.next();
                        if (key.m_params.containsKey(o)) {
                            str.append(o);
                            str.append("=");
                            // TODO: handle multiple occurences of the same parameter value
                            String[] values = (String[])key.m_params.get(o);
                            str.append(values[0]);
                            if (i.hasNext()) str.append(",");
                        }
                    }
                } else {
                    // Match all request params
                    Iterator i = key.m_params.keySet().iterator();            
                    while (i.hasNext()) {
                        Object o = i.next();
                        str.append(o);
                        str.append("=");
                        // TODO: handle multiple occurences of the same parameter value
                        String[] values = (String[])key.m_params.get(o);
                        str.append(values[0]);
                        if (i.hasNext()) str.append(",");
                    }                    
                }
            }
            str.append(")");
        }
        
        if (m_schemes != null) {
            String s = (String)key.m_schemes.iterator().next();
            if ((m_schemes.size() > 0) && (! m_schemes.contains(s.toLowerCase()))) return null;
            str.append("schemes=(");
            str.append(s);
            str.append(");");
        }
        
        if (m_ports != null) {
            Integer i = (Integer)key.m_ports.iterator().next();
            if ((m_ports.size() > 0) && (! m_ports.contains(i))) return null;
            str.append("ports=(");
            str.append(i);
            str.append(");");
        }
        
        if (m_timeout > 0) {
            str.append("timeout=(");
            str.append(m_timeout);
            str.append(");");
        }
        
        return str.toString();
    }
    
    /** 
     * Overloaded from Object.toString()
     *
     * @return A simple String representation for this key.
     */    
    public String toString() {
        StringBuffer str = new StringBuffer(100);        

        if (m_always < 0) {
            str.append("never"); 
            if (m_parseError) {
                str.append(";parse-error");
            }            
            return str.toString();
        }
        if (m_noparams != null) {
            // Add "no-cachable" parameters
            if (m_noparams.size() == 0) {
                str.append("no-params;");
            } else {
                str.append("no-params=(");
                Iterator i = m_noparams.iterator();
                while (i.hasNext()) {
                    Object o = i.next();
                    str.append(o);
                    if (i.hasNext()) str.append(",");
                }                      
                str.append(");");
            }
        }        
        if (m_always > 0) {
            str.append("always");
            if (m_parseError) {
                str.append(";parse-error");
            }            
            return str.toString();
        }
        if (m_uri != null) {
            if (m_uri.equals("uri")) {
                str.append("uri;");
            } else {
                str.append("uri=(");
                str.append(m_uri);
                str.append(");");
            }
        }
        if (m_user >= 0) {
            // Add user data
            if (m_user == Integer.MAX_VALUE) {
                str.append("user;");
            } else {
                str.append("user=(");
                str.append(m_user);
                str.append(");");
            }
        }
        if (m_groups != null) {
            // Add group data
            if (m_groups.size() == 0) {
                str.append("groups;");
            } else {
                str.append("groups=(");
                Iterator i = m_groups.iterator();
                while (i.hasNext()) {
                    str.append(i.next());
                    if (i.hasNext()) str.append(",");
                }
                str.append(");");
            }
        }               
        if (m_params != null) {
            // Add parameters
            if (m_params.size() == 0) {
                str.append("params;");
            } else {
                str.append("params=(");
                Iterator i = m_params.keySet().iterator();
                while (i.hasNext()) {
                    Object o = i.next();
                    str.append(o);
                    try {
                        // TODO: handle multiple occurences of the same parameter value
                        String param[] = (String[])m_params.get(o);
                        if (! "&?&".equals(param[0])) {
                            str.append("=");
                            str.append(param[0]);
                        }
                    } catch(Exception e) {                        
                        if (DEBUG) System.err.println("Exception! o=" + o + "  Exception is " + e );
                    }
                    if (i.hasNext()) str.append(",");
                }            
                str.append(");");
            }
        }
        if (m_timeout >= 0) {
            // Add timeout 
            str.append("timeout=(");
            str.append(m_timeout);
            str.append(");");
        }
        if (m_publish) {
            // Add publish parameters
            str.append("publish-clear;");
        }
        if (m_schemes != null) {
            // Add schemes
            if (m_schemes.size() == 0) {
                str.append("schemes;");
            } else {
                str.append("schemes=(");
                Iterator i = m_schemes.iterator();
                while (i.hasNext()) {
                    str.append(i.next());
                    if (i.hasNext()) str.append(",");
                }          
                str.append(");");
            }
        }
        if (m_ports != null) {
            // Add ports
            if (m_ports.size() == 0) {
                str.append("ports;");
            } else {
                str.append("ports=(");
                Iterator i = m_ports.iterator();
                while (i.hasNext()) {
                    str.append(i.next());
                    if (i.hasNext()) str.append(",");
                } 
                str.append(");");            
            }
        }        
        
        if (m_parseError) {
            str.append("parse-error;");
        }
        return str.toString();
    }
    
    /**
     * Parse a String in the Flex cache language and construct 
     * the key data structure from this.
     *
     * @param key The String to parse (usually read from a file property)
     */    
    private void parseFlexKey(String key) {
        java.util.StringTokenizer toker = new java.util.StringTokenizer(key,";");
        try {
            while (toker.hasMoreElements()) {
                String t = toker.nextToken();
                String k = null; 
                String v = null;
                int idx = t.indexOf("=");
                if (idx >= 0) {
                    k = t.substring(0, idx).trim();
                    if (t.length() > idx) v = t.substring(idx+1).trim();
                } else {
                    k = t.trim();
                }
                m_always = 0;
                if (DEBUG) System.err.println("Parsing token:" + t + " key=" + k + " value=" + v);
                switch (cacheCmds.indexOf(k)) {
                    case 0: // always
                        m_always = 1;
                        // Continue processing (make sure we find a "never" behind "always")
                        break;
                    case 1: // never
                    case 11:
                        m_always = -1;
                        // No need for any further processing
                        return;
                    case 2: // uri
                        m_uri = "uri";
                        // being != null is enough
                        break;
                    case 3: // user
                        m_user = Integer.MAX_VALUE;
                        // being > 0 is enough
                        break;
                    case 4: // groups
                        if (v != null) {
                            // A list of groups is present
                            m_groups = parseValueMap(v).keySet();
                        } else {
                            // Cache all groups
                            m_groups = new java.util.HashSet(0);
                        }
                        break;
                    case 5: // params
                        m_params = parseValueMap(v);
                        break;
                    case 6: // no-params
                        if (v != null) {
                            // No-params are present
                            m_noparams = parseValueMap(v).keySet();
                        } else {
                            // Never cache with parameters
                            m_noparams = new java.util.HashSet(0);
                        }
                        break;
                    case 7: // timeout
                        m_timeout = Integer.parseInt(v);
                        break;
                    case 8: // publish
                        m_publish = true;
                        break;
                    case 9: // schemes
                        m_schemes = parseValueMap(v).keySet();
                        break;
                    case 10: // ports
                        m_ports = parseValueMap(v).keySet();
                        break;
                    case 12: // previous parse error - ignore
                        break;
                    default: // unknown directive, throw error
                        m_parseError = true;
                }      
            }
        } catch (Exception e) {
            // Any Exception here indicates a parsing error
            if (DEBUG) System.err.println("----- Error in key parsing: " + e.toString());
            m_parseError = true;
        }
        if (m_parseError) {
            // If string is invalid set cache to "never"
            m_always = -1;
        }
    }

    /** 
     * A helper method for the parsing process which parses
     * Strings like groups=(a, b, c).
     *
     * @param value The String to parse 
     * @return A Map that contains of the parsed values, only the keyset of the Map is needed later
     */    
    private java.util.Map parseValueMap(String value) {
        if (value.charAt(0) == '(') value = value.substring(1);
        int len;
        if (value.charAt(len = (value.length()-1)) == ')') value = value.substring(0, len);
        if (value.charAt(len-1) == ',') value = value.substring(0, len-1);
        if (DEBUG) System.err.println("Parsing map: " + value);
        java.util.StringTokenizer toker = new java.util.StringTokenizer(value, ",");
        java.util.Map result = new java.util.HashMap();
        while (toker.hasMoreTokens()) {
            result.put(toker.nextToken().trim().toLowerCase(), new String[] { "&?&" } );
        }
        return result;
    }
    
}
