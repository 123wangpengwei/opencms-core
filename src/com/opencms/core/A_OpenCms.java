/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/core/Attic/A_OpenCms.java,v $
* Date   : $Date: 2003/02/01 19:14:45 $
* Version: $Revision: 1.30 $
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

package com.opencms.core;

import com.opencms.boot.CmsBase;
import com.opencms.boot.I_CmsLogChannels;
import com.opencms.file.CmsFile;
import com.opencms.file.CmsObject;
import com.opencms.flex.CmsEvent;
import com.opencms.flex.I_CmsEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import source.org.apache.java.util.Configurations;

/**
 * Abstract class for the OpenCms "operating system" that provides 
 * public static methods which can be used by other classes to access 
 * basic features of OpenCms like logging etc.
 *
 * @see OpenCms
 * 
 * @author Alexander Lucas
 * @author Michael Emmerich
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.30 $ $Date: 2003/02/01 19:14:45 $
 */
public abstract class A_OpenCms implements I_CmsLogChannels {

    /** The filename of the log file */
    private static String m_logfile;

    /** List to save the event listeners in */
    private static java.util.ArrayList m_listeners = new ArrayList();
            
    /** A Map for the storage of various runtime properties */
    private static Map m_runtimeProperties = null;

    /** The OpenCms configuration read from <code>opencms.properties</code> */
    private Configurations m_conf = null;
    
    /** Default encoding, can be overwritten in "opencms.properties" */
    private static String m_defaultEncoding = "ISO-8859-1";    

    /** The version number of this OpenCms installation */
    private static String c_versionNumber = null;
        
    /**
     * Destructor, should be called when the the class instance is shut down.
     */
    abstract void destroy() throws CmsException;

    /**
     * Initializes the logging mechanism of OpenCms.
     * 
     * @param config The configurations read from <code>opencms.properties</code>
     */
    public static void initializeServletLogging(Configurations config) {
        m_logfile = config.getString("log.file");
        CmsBase.initializeServletLogging(config);
    }

    /**
     * Returns the filename of the logfile.
     * 
     * @return The filename of the logfile.
     */
    public static String getLogFileName() {
        return m_logfile;
    }

    /**
     * Checks if the system logging is active.
     * 
     * @return <code>true</code> if the logging is active, <code>false</code> otherwise.
     */
    public static boolean isLogging() {
        return CmsBase.isLogging();    
    }
    
    /**
     * Checks if the system logging is active for the selected channel.
     * 
     * @return <code>true</code> if the logging is active for the channel, <code>false</code> otherwise.
     */
    public static boolean isLogging(String channel) {
        return CmsBase.isLogging(channel);    
    }    
    
    /**
     * Logs a message into the OpenCms logfile.<p>
     * 
     * If the logfile was not initialized (e.g. due tue a missing
     * ServletConfig while working with the console)
     * any log output will be written to <code>System.err</code>.
     * 
     * @param channel The channel the message is logged into
     * @param message The message to be logged.
     */
    public static void log(String channel, String message) {
        CmsBase.log(channel, message);
    }

    /**
     * Sets the mimetype of the response.<br>
     * The mimetype is selected by the file extension of the requested document.
     * If no available mimetype is found, it is set to the default
     * "application/octet-stream".
     *
     * @param cms The actual OpenCms object.
     * @param file The requested document.
     */
    abstract void setResponse(CmsObject cms, CmsFile file);

    /**
     * Selects the appropriate launcher for a given file by analyzing the
     * file's launcher id and calls the initlaunch() method to initiate the
     * generating of the output.
     *
     * @param cms CmsObject containing all document and user information
     * @param file CmsFile object representing the selected file.
     * @throws CmsException In case of problems acessing the resource.
     */
    abstract public void showResource(CmsObject cms, CmsFile file) throws CmsException;

    /**
     * This method stores sessiondata into the database. It is used
     * for sessionfailover.
     *
     * @param sessionId the id of the session.
     * @param isNew determines, if the session is new or not.
     * @return data the sessionData.
     */
    abstract void storeSession(String sessionId, Hashtable sessionData) throws CmsException;

    /**
     * This method loads old sessiondata from the database. It is used
     * for sessionfailover.
     *
     * @param oldSessionId the id of the old session.
     * @return The restored sessiondata read from the database.
     * @throws CmsException In case of problems acessing the database.
     */
    abstract Hashtable restoreSession(String oldSessionId) throws CmsException;
    
    /**
     * Reads the current crontable entries from the database and updates the
     * crontable with them.
     */
    abstract void updateCronTable();

    /**
     * Starts a schedule job with a correct instantiated CmsObject.
     * 
     * @param entry the CmsCronEntry to start.
     */
    abstract void startScheduleJob(CmsCronEntry entry);
    
    /**       
     * This method adds an Object to the OpenCms runtime properties.
     * The runtime properties can be used to store Objects that are shared
     * in the whole system.<p>
     *
     * @since FLEX alpha 1
     * @param key The key to add the Object with.
     * @param value The value of the Object to add.
     */
    public void setRuntimeProperty(Object key, Object value) {
        if (m_runtimeProperties == null) {
            m_runtimeProperties = Collections.synchronizedMap(new HashMap());
        }
        m_runtimeProperties.put(key, value);
    }
    
    /** This method looks up a value in the runtime property Map.
     *
     * @since FLEX alpha 1
     * @param key The key to look up in the runtime properties.
     * @return The value for the key, or null if the key was not found.
     */
    public static Object getRuntimeProperty(Object key) {
        if (m_runtimeProperties == null) return null;
        return m_runtimeProperties.get(key);
    }
    
    /** This method returns the complete runtime property Map.
     *
     * @since FLEX alpha 1
     * @return The Map of runtime properties.
     */    
    public Map getRuntimePropertyMap() {
        return m_runtimeProperties;
    }

    /** This method sets the runtime configuration.
     *
     * @since FLEX alpha 1
     */       
    public void setConfiguration(Configurations conf) {
        m_conf = conf;
    }    
    
    /** This method returns the runtime configuration.
     *
     * @since FLEX alpha 1
     * @return The runtime configuration.
     */       
    public Configurations getConfiguration() {
        return m_conf;
    }

    /**
     * Notify all container event listeners that a particular event has
     * occurred for this Container.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @since FLEX alpha 1
     * @param cms An initialized CmsObject
     * @param type Event type
     * @param data Event data
     */
    public static void fireCmsEvent(CmsObject cms, int type, java.util.Map data) {
       OpenCms.fireCmsEvent( new CmsEvent(cms, type, data) );
    }    
    
    /**
     * Notify all container event listeners that a particular event has
     * occurred for this Container.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @since FLEX beta 1
     * @param cms An initialized CmsObject
     * @param event A CmsEvent
     */
    public static void fireCmsEvent(CmsEvent event) {
        if (m_listeners.size() < 1)
            return;
        I_CmsEventListener list[] = new I_CmsEventListener[0];
        synchronized (m_listeners) {
            list = (I_CmsEventListener[]) m_listeners.toArray(list);
        }
        for (int i = 0; i < list.length; i++)
            ((I_CmsEventListener) list[i]).cmsEvent(event);
    }    
    
    /**
     * Add a cms event listener.
     *
     * @since FLEX alpha 1
     * @param listener The listener to add
     */
    public static void addCmsEventListener(I_CmsEventListener listener) {
        synchronized (m_listeners) {
            m_listeners.add(listener);
        }        
    }    
    
    /**
     * Remove a cms event listener.
     *
     * @since FLEX alpha 1
     * @param listener The listener to add
     */
    public static void removeCmsEventListener(I_CmsEventListener listener) {
        synchronized (m_listeners) {
            m_listeners.remove(listener);
        }
    }
    
    /**
     * Return the OpenCms default character encoding.
     * The default is set in the "opencms.properties" file.
     * If this is not set in "opencms.properties" the default 
     * is "ISO-8859-1". 
     * 
     * @return The default encoding (e.g. "UTF-8" or "ISO-8859-1")
     */
    public static String getDefaultEncoding() {
        return m_defaultEncoding;
    }    
    
    /**
     * Sets the default encoding to the value specified.
     * 
     * @param encoding The value to set, e.g. "UTF-8" or "ISO-8859-1".
     */
    protected void setDefaultEncoding(String encoding) {
        m_defaultEncoding = encoding;
    }
    
    
    /**
     * Returns a String containing version information for this OpenCms.
     *
     * @return version a String containnig the version of OpenCms.
     */
    public static String version() {
        return c_versionNumber;
    }        
    
    /**
     * Initialized the version for this OpenCms, will be called by 
     * CmsHttpServlet or CmsShell upon system startup.
     * 
     * @param o instance of calling object
     */
     static void initVersion(Object o) {
        // read the version-informations from properties, if not done
        Properties props = new Properties();
        try {
            props.load(o.getClass().getClassLoader().getResourceAsStream("com/opencms/core/version.properties"));
        } catch(java.io.IOException exc) {
            // ignore this exception - no properties found
            c_versionNumber = "unknown";
            return;
        }
        c_versionNumber =
            props.getProperty("version.number", "??") + " " +
            props.getProperty("version.name", "??");
    }        
}
