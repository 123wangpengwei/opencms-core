/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/site/CmsSiteMatcher.java,v $
 * Date   : $Date: 2003/09/26 16:00:00 $
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

package org.opencms.site;

/**
 * A matcher object to compare request data against the configured sites.<p>
 *
 * @author  Alexander Kandzior (a.kandzior@alkacon.com)
 *
 * @version $Revision: 1.3 $
 * @since 5.1
 */
public final class CmsSiteMatcher implements Cloneable {   
    
    /** Wildcard for string matching */
    private static final String C_WILDCARD = "*";
    
    /** The hostname (e.g. localhost) which is required to access this site */
    private String m_serverName;
    
    /** The protocol (e.g. "http", "https") which is required to access this site */
    private String m_serverProtocol;
    
    /** The port (e.g. 80) which is required to access this site */
    private int m_serverPort;

    /** Hashcode buffer to save multiple calculations*/
    private Integer m_hashCode;

    /** Default matcher that always matches all other Site matchers */
    public static final CmsSiteMatcher C_DEFAULT_MATCHER = new CmsSiteMatcher(C_WILDCARD, C_WILDCARD, 0);

    /**
     * Constructs a new site matcher object.<p>
     * 
     * @param serverProtocol to protocol required to access this site
     * @param serverName the server URL prefix to which this site is mapped
     * @param serverPort the port required to access this site
     */
    public CmsSiteMatcher(String serverProtocol, String serverName, int serverPort) {
        init(serverProtocol, serverName, serverPort);
    }
    
    /**
     * Construct a new site matcher from a String which should be in default URL notation.<p>
     * 
     * If no port is provided, the default port 80 will be used.
     * If no protocol is provided, the default protocol "http" will be used.
     * 
     * @param serverString the String, e.g. http://localhost:8080
     */
    public CmsSiteMatcher(String serverString) {
        if (serverString == null) {
            init(C_WILDCARD, C_WILDCARD, 0);
            return;
        }
        // remove whitespace
        serverString = serverString.trim();
        // cut trailing "/" 
        if (serverString.endsWith("/")) serverString = serverString.substring(0, serverString.length()-1);
        int pos, serverPort;
        String serverProtocol, serverName;
        // check for protocol
        pos = serverString.indexOf("://");
        if (pos >= 0) {
            serverProtocol = serverString.substring(0, pos);            
            serverString = serverString.substring(pos + 3);
        } else {
            serverProtocol = "http";
        }
        // check for server name and port
        pos = serverString.indexOf(":");
        if (pos >= 0) {
            serverName = serverString.substring(0, pos);
            try { 
                serverPort = Integer.valueOf(serverString.substring(pos+1)).intValue();
            } catch (NumberFormatException e) {
                serverPort = 80;
            }            
        } else {
            serverName = serverString;
            serverPort = 80;
        }        
        // initialize menmbers
        init(serverProtocol, serverName, serverPort);
    }
    
    /**
     * Inits the member variables.<p>
     * 
     * @param serverProtocol to protocol required to access this site
     * @param serverName the server URL prefix to which this site is mapped
     * @param serverPort the port required to access this site
     */
    private void init(String serverProtocol, String serverName, int serverPort) {
        setServerProtocol(serverProtocol);
        setServerName(serverName);
        setServerPort(serverPort);
    }

    /**
     * Returns the hostname (e.g. localhost) which is required to access this site.<p>
     * 
     * @return the hostname (e.g. localhost) which is required to access this site
     */
    public String getServerName() {
        return m_serverName;
    }

    /**
     * Sets the hostname (e.g. localhost) which is required to access this site.<p>
     * 
     * Setting the hostname to "*" is a wildcard that matches all hostnames
     * 
     * @param serverName the hostname (e.g. localhost) which is required to access this site
     */
    protected void setServerName(String serverName) {
        if ((serverName == null) || ("".equals(serverName)) || (C_WILDCARD.equals(serverName))) {
            m_serverName = C_WILDCARD;
        } else {
            m_serverName = serverName.trim();
        }        
    }

    /**
     * Returns the port (e.g. 80) which is required to access this site.<p>
     *
     * @return the port (e.g. 80) which is required to access this site
     */
    public int getServerPort() {
        return m_serverPort;
    }

    /**
     * Sets the port (e.g. 80) which is required to access this site.<p>
     *
     * Setting the port to 0 (zero) is a wildcard that matches all ports
     *
     * @param serverPort the port (e.g. 80) which is required to access this site
     */
    protected void setServerPort(int serverPort) {
        m_serverPort = serverPort;
        if (m_serverPort < 0) {
            m_serverPort = 0;
        }
    }

    /**
     * Returns the protocol (e.g. "http", "https") which is required to access this site.<p>
     * 
     * @return the protocol (e.g. "http", "https") which is required to access this site
     */
    public String getServerProtocol() {
        return m_serverProtocol;
    }

    /**
     * Sets the protocol (e.g. "http", "https") which is required to access this site.<p>
     * 
     * Setting the protocol to "*" is a wildcard that matches all protocols.<p>
     *      
     * @param serverProtocol the protocol (e.g. "http", "https") which is required to access this site
     */
    protected void setServerProtocol(String serverProtocol) {
        int pos;
        if ((serverProtocol == null) || ("".equals(serverProtocol)) || (C_WILDCARD.equals(serverProtocol))) {
            m_serverProtocol = C_WILDCARD;
        } else if ((pos = serverProtocol.indexOf("/")) > 0) {
            m_serverProtocol = serverProtocol.substring(0, pos).toLowerCase();        
        } else {
            m_serverProtocol = serverProtocol.toLowerCase().trim();
        }        
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer result = new StringBuffer(32);
        if ((m_serverProtocol != null) && !(C_WILDCARD.equals(m_serverProtocol))) {
            result.append(m_serverProtocol);
            result.append("://");
        }
        result.append(m_serverName);
        if (m_serverPort > 0) {
            result.append(":");
            result.append(m_serverPort);
        }
        return result.toString();
    }
    
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof CmsSiteMatcher)) return false;
        // if one of the object is the default matcher the result is true
        if ((this == C_DEFAULT_MATCHER) || (o == C_DEFAULT_MATCHER)) return true;
        if (o == this) return true;        

        CmsSiteMatcher matcher = (CmsSiteMatcher)o;            
        if (getServerPort() != matcher.getServerPort()) {
            return false;
        }                
        if (! getServerName().equals(matcher.getServerName())) {
            return false;
        }        
        if (! getServerProtocol().equals(matcher.getServerProtocol())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        if (m_hashCode == null) {
            m_hashCode = new Integer(toString().hashCode());
        }
        return m_hashCode.intValue();
    }
    
    /**
     * Returns a clone of this Objects instance.<p>
     * 
     * @return a clone of this instance
     */
    public Object clone() {
        return new CmsSiteMatcher(m_serverProtocol, m_serverName, m_serverPort);
    }
}
