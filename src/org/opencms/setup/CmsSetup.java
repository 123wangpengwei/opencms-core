/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/setup/Attic/CmsSetup.java,v $
 * Date   : $Date: 2004/02/19 14:12:24 $
 * Version: $Revision: 1.11 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2004 Alkacon Software (http://www.alkacon.com)
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
package org.opencms.setup;

import org.opencms.main.I_CmsConstants;
import org.opencms.main.OpenCmsCore;
import org.opencms.util.CmsStringSubstitution;
import org.opencms.util.CmsUUID;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.collections.ExtendedProperties;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * A java bean as a controller for the OpenCms setup wizard.<p>
 *
 * It is not allowed to customize this bean with methods for a specific database server setup!<p>
 * 
 * Database server specific settings should be set/read using get/setDbProperty, as for example like:
 * 
 * <pre>
 * setDbProperty("oracle.defaultTablespace", value);
 * </pre>
 *
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 * @version $Revision: 1.11 $ 
 */
public class CmsSetup extends Object implements Serializable, Cloneable {

    /** Required files per database server setup.<p> */
    public static final String[] requiredDbSetupFiles = {
            "step_4_database_setup.jsp",
            "database.properties",
            "create_db.sql",
            "create_tables.sql",
            "drop_db.sql",
            "drop_tables.sql"
    };    
    
    /** Contains the error messages to be displayed in the setup wizard.<p> */
    private static Vector errors;

    /** Contains the properties of "opencms.properties".<p> */
    private ExtendedProperties m_extProperties;

    /** Contains HTML fragments for the output in the JSP pages of the setup wizard.<p> */
    private Properties m_htmlProps;

    /** The absolute path to the home directory of the OpenCms webapp.<p> */
    private String m_basePath;

    /** Key of the selected database server (e.g. "mysql", "generic" or "oracle").<p> */
    private String m_databaseKey;

    /** Password used for the JDBC connection when the OpenCms database is created.<p> */
    private String m_dbCreatePwd;

    /** List of keys of all available database server setups (e.g. "mysql", "generic" or "oracle").<p> */
    private List m_databaseKeys;
    
    /** List of sorted keys by ranking of all available database server setups (e.g. "mysql", "generic" or "oracle").<p> */
    private List m_sortedDatabaseKeys;
    
    /** List of clear text names of all available database server setups (e.g. "MySQL", "Generic (ANSI) SQL").<p> */
    private List m_databaseNames;
    
    /** Map of database setup properties of all available database server setups keyed by their database keys.<p> */
    private Map m_databaseProperties;
    
    /** A map with tokens ${...} to be replaced in SQL scripts.<p> */
    private Map m_replacer;    
    
    /** A map with all available modules.<p> */
    private Map m_availableModules;
    
    /** A map with lists of dependent module package names keyed by module package names.<p> */
    private Map m_moduleDependencies;
    
    /** 
     * Default constructor.<p>
     */
    public CmsSetup() {
        m_databaseKeys = null;
        m_databaseNames = null;
        m_databaseProperties = null;
        errors = new Vector();
    }
    
    /** 
     * This method reads the properties from the htmlmsg.property file
     * and sets the HTML part properties with the matching values.<p>
     */
    public void initHtmlParts() {
        try {
            m_htmlProps = new Properties();
            m_htmlProps.load(getClass().getClassLoader().getResourceAsStream(OpenCmsCore.C_FILE_HTML_MESSAGES));
        } catch (Exception e) {
            e.printStackTrace();
            errors.add(e.toString());
        }
    }

    /** 
     * This method reads the properties from the opencms.property file
     * and sets the CmsSetup properties with the matching values.
     * This method should be called when the first page of the OpenCms
     * Setup Wizard is called, so the input fields of the wizard are pre-defined
     * 
     * @param props path to the properties file
     */
    public void initProperties(String props) {
        getDatabaseNames();
        initHtmlParts();
        
        String path = getConfigFolder() + props;
        try {
            m_extProperties = CmsSetupUtils.loadProperties(path);
        } catch (Exception e) {
            e.printStackTrace();
            errors.add(e.toString());
        }
    }
    
    /**
     * Checks the ethernet address value and generates a dummy address, if necessary.<p>     *
     */
    public void checkEthernetAddress() {
        // check the ethernet address in order to generate a random address, if not available                   
        if ("".equals(getEthernetAddress())) {
            setEthernetAddress(CmsUUID.getDummyEthernetAddress());
        }
    }

    /**
     * This method checks the validity of the given properties
     * and adds unset properties if possible
     * @return boolean true if all properties are set correctly
     */
    public boolean checkProperties() {

        // check if properties available
        if (getProperties() == null) {
            return false;
        }

        // check the maximum file size, set it to unlimited, if not valid 
        String size = getFileMaxUploadSize();
        if (size == null || "".equals(size)) {
            setFileMaxUploadSize("-1");
        } else {
            try {
                Integer.parseInt(size);
            } catch (Exception e) {
                setFileMaxUploadSize("-1");
            }
        }

        return true;
    }

    /** 
     * This method sets the value for a given key in the extended properties.
     * @param key The key of the property
     * @param value The value of the property
     */
    public void setExtProperty(String key, String value) {
        m_extProperties.put(key, value);
    }

    /**
     * Returns the value for a given key from the extended properties.
     * 
     * @param key the property key
     * @return the string value for a given key
     */
    public String getExtProperty(String key) {
        Object value = null;

        return ((value = m_extProperties.get(key)) != null) ? value.toString() : "";
    }

    /** 
     * This method sets the value for a given key in the database properties.
     * @param key The key of the property
     * @param value The value of the property
     */
    public void setDbProperty(String key, String value) {
        // extract the database key out of the entire key
        String databaseKey = key.substring(0, key.indexOf("."));
        Map databaseProperties = (Map) getDatabaseProperties().get(databaseKey);
        databaseProperties.put(key, value);
    }

    /**
     * Returns the value for a given key from the database properties.
     * 
     * @param key the property key
     * @return the string value for a given key
     */
    public String getDbProperty(String key) {
        Object value = null;
        
        // extract the database key out of the entire key
        String databaseKey = key.substring(0, key.indexOf("."));
        Map databaseProperties = (Map) getDatabaseProperties().get(databaseKey);

        return ((value = databaseProperties.get(key)) != null) ? (String) value : "";
    }

    /** 
     * Sets the path to the OpenCms home directory 
     *
     * @param basePath path to OpenCms home directory
     */
    public void setBasePath(String basePath) {
        m_basePath = basePath;
        if (!m_basePath.endsWith(File.separator)) {
            // make sure that Path always ends with a separator, not always the case in different 
            // environments since getServletContext().getRealPath("/") does not end with a "/" in 
            // all servlet runtimes
            m_basePath += File.separator;
        }
    }

    /** 
     * Returns the absolute path to the OpenCms home directory
     * 
     * @return the path to the OpenCms home directory 
     */
    public String getBasePath() {
        return m_basePath.replace('\\', '/').replace('/', File.separatorChar);
    }

    /** 
     * Gets the default pool
     * 
     * @return name of the default pool 
     */
    public String getPool() {
        StringTokenizer tok = new StringTokenizer(getExtProperty("db.pools"), ",[]");
        String pool = tok.nextToken();
        return pool;
    }

    /** 
     * Sets the database drivers to the given value 
     * 
     * @param databaseKey the key of the selected database server (e.g. "mysql", "generic" or "oracle")
     */
    public void setDatabase(String databaseKey) {
        m_databaseKey = databaseKey;

        String vfsDriver = getDbProperty(m_databaseKey + ".vfs.driver");
        String userDriver = getDbProperty(m_databaseKey + ".user.driver");
        String projectDriver = getDbProperty(m_databaseKey + ".project.driver");
        String workflowDriver = getDbProperty(m_databaseKey + ".workflow.driver");
        String backupDriver = getDbProperty(m_databaseKey + ".backup.driver");

        // Change/write configuration only if not available or database changed
        setExtProperty("db.name", m_databaseKey);
        if (getExtProperty("db.vfs.driver") == null || "".equals(getExtProperty("db.vfs.driver"))) {
            setExtProperty("db.vfs.driver", vfsDriver);
        }
        if (getExtProperty("db.user.driver") == null || "".equals(getExtProperty("db.user.driver"))) {
            setExtProperty("db.user.driver", userDriver);
        }
        if (getExtProperty("db.project.driver") == null || "".equals(getExtProperty("db.project.driver"))) {
            setExtProperty("db.project.driver", projectDriver);
        }
        if (getExtProperty("db.workflow.driver") == null || "".equals(getExtProperty("db.workflow.driver"))) {
            setExtProperty("db.workflow.driver", workflowDriver);
        }
        if (getExtProperty("db.backup.driver") == null || "".equals(getExtProperty("db.backup.driver"))) {
            setExtProperty("db.backup.driver", backupDriver);
        }
    }

    /** 
     * Returns the key of the selected database server (e.g. "mysql", "generic" or "oracle").<p>
     * 
     * @return the key of the selected database server (e.g. "mysql", "generic" or "oracle")
     */
    public String getDatabase() {
        if (m_databaseKey == null) {
            m_databaseKey = getExtProperty("db.name");
        }
        
        if (m_databaseKey == null || "".equals(m_databaseKey)) {
            m_databaseKey = (String) getSortedDatabases().get(0);
        }
        
        return m_databaseKey;
    }

    /** 
     * Returns a list with they keys (e.g. "mysql", "generic" or "oracle") of all available
     * database server setups found in "/setup/database/".<p>
     *
     * @return a list with they keys (e.g. "mysql", "generic" or "oracle") of all available database server setups
     */
    public List getDatabases() {
        File databaseSetupFolder = null;
        File[] childResources = null;
        File childResource = null;
        File setupFile = null;
        boolean hasMissingSetupFiles = false;

        if (m_databaseKeys != null) {
            return m_databaseKeys;
        }

        try {
            m_databaseKeys = (List) new ArrayList();
            databaseSetupFolder = new File(m_basePath + File.separator + "setup" + File.separator + "database");

            if (databaseSetupFolder.exists()) {
            childResources = databaseSetupFolder.listFiles();

            if (childResources != null) {
                for (int i = 0; i < childResources.length; i++) {
                    childResource = childResources[i];
                    hasMissingSetupFiles = false;

                    if (childResource.exists() && childResource.isDirectory() && childResource.canRead()) {
                        for (int j = 0; j < requiredDbSetupFiles.length; j++) {
                            setupFile = new File(childResource.getPath() + File.separator + requiredDbSetupFiles[j]);

                            if (!setupFile.exists() || !setupFile.isFile() || !setupFile.canRead()) {
                                hasMissingSetupFiles = true;
                                System.err.println("[" + getClass().getName() + "] missing or unreadable database setup file: " + setupFile.getPath());
                                break;
    }
                        }

                        if (!hasMissingSetupFiles) {
                            m_databaseKeys.add(childResource.getName().trim());
                        }
                    }
                }
            }
            }
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace(System.err);
        }

        return m_databaseKeys;
    }

    /** 
     * Returns a list with the clear text names (e.g. "MySQL", "Generic (ANSI) SQL") of all 
     * available database server setups in "/setup/database/".<p>
     * 
     * Second, this method stores the properties of all available database configurations in a
     * map keyed by their database key names (e.g. "mysql", "generic" or "oracle").<p>
     *
     * @return a list with the clear text names (e.g. "MySQL", "Generic (ANSI) SQL") of all available database server setups
     * @see #getDatabaseProperties()
     */
    public List getDatabaseNames() {
        List databaseKeys = null;
        String databaseKey = null;
        String databaseName = null;
        FileInputStream input = null;
        String configPath = null;
        Properties databaseProperties = null;

        if (m_databaseNames != null) {
            return m_databaseNames;
        }
        
        m_databaseNames = (List) new ArrayList();
        m_databaseProperties = (Map) new HashMap();
        databaseKeys = getDatabases();
        
        for (int i = 0; i < databaseKeys.size(); i++) {
            databaseKey = (String) databaseKeys.get(i);
            configPath = m_basePath + "setup" + File.separator + "database" + File.separator + databaseKey + File.separator + "database.properties";

            try {
                input = new FileInputStream(new File(configPath));
                databaseProperties = new Properties();
                databaseProperties.load(input);
                
                databaseName = databaseProperties.getProperty(databaseKey + ".name");                
                m_databaseNames.add(databaseName);
                m_databaseProperties.put(databaseKey, databaseProperties);
            } catch (Exception e) {
                System.err.println(e.toString());
                e.printStackTrace(System.err);
                continue;
            } finally {
                try {
                    if (input != null) {
                        input.close();
    }
                } catch (Exception e) {
                    // noop
                }
            }
        }                      

        return m_databaseNames;
    }
    
    /** 
     * Returns a sorted list with they keys (e.g. "mysql", "generic" or "oracle") of all available
     * database server setups found in "/setup/database/" sorted by their ranking property.<p>
     *
     * @return a sorted list with they keys (e.g. "mysql", "generic" or "oracle") of all available database server setups
     */
    public List getSortedDatabases() {
        if (m_sortedDatabaseKeys == null) {
            List databases = getDatabases();
            List sortedDatabases = new ArrayList(databases.size());
            SortedMap mappedDatabases = new TreeMap();
            for (int i=0; i<databases.size(); i++) {
                String key = (String)databases.get(i);
                Integer ranking = new Integer(0);
                try {
                    ranking = Integer.valueOf(getDbProperty(key + ".ranking"));
                } catch (Exception e) {
                    // ignore
                }
                mappedDatabases.put(ranking, key);
            }
           
            while (mappedDatabases.size() > 0) {
                // get database with highest ranking 
                Integer key = (Integer)mappedDatabases.lastKey();           
                String database = (String)mappedDatabases.get(key);
                sortedDatabases.add(database);
                mappedDatabases.remove(key);
            }
            m_sortedDatabaseKeys = new ArrayList(databases.size());
            m_sortedDatabaseKeys = sortedDatabases;
        }     
        return m_sortedDatabaseKeys;
    }
    
    /** 
     * Returns a map with the database properties of *all* available database configurations keyed
     * by their database keys (e.g. "mysql", "generic" or "oracle").<p>
     * 
     * @return a map with the database properties of *all* available database configurations
     */
    public Map getDatabaseProperties() {
        if (m_databaseProperties != null) {
            return m_databaseProperties;
        }
        
        getDatabaseNames();
        return m_databaseProperties;
    }
    
    /**
     * Returns the URI of a database config page (in step 3) for a specified database key.<p>
     * 
     * 
     * @param key the database key (e.g. "mysql", "generic" or "oracle")
     * @return the URI of a database config page
     */
    public String getDatabaseConfigPage(String key) {
        return "database" + I_CmsConstants.C_FOLDER_SEPARATOR + key + I_CmsConstants.C_FOLDER_SEPARATOR + "step_4_database_setup.jsp";
    }

    /** 
     * Sets the connection string to the database to the given value 
     *
     * @param dbWorkConStr the connection string used by the OpenCms core 
     */
    public void setDbWorkConStr(String dbWorkConStr) {

        String driver = getDbProperty(m_databaseKey + ".driver");

        // TODO: set the driver in own methods
        setExtProperty("db.pool." + getPool() + ".jdbcDriver", driver);
        setExtProperty("db.pool." + getPool() + ".jdbcUrl", dbWorkConStr);

        setTestQuery(getDbTestQuery());
    }

    /** 
     * Returns a connection string 
     *
     * @return the connection string used by the OpenCms core  
     */
    public String getDbWorkConStr() {
        String str = getExtProperty("db.pool." + getPool() + ".jdbcUrl");
        return str;
    }

    /** 
     * Sets the user of the database to the given value 
     *
     * @param dbWorkUser the database user used by the opencms core 
     */
    public void setDbWorkUser(String dbWorkUser) {

        setExtProperty("db.pool." + getPool() + ".user", dbWorkUser);
    }

    /** 
     * Returns the user of the database from the properties 
     *
     * @return the database user used by the opencms core  
     */
    public String getDbWorkUser() {
        return getExtProperty("db.pool." + getPool() + ".user");
    }

    /** 
     * Sets the password of the database to the given value 
     *
     * @param dbWorkPwd the password for the OpenCms database user  
     */
    public void setDbWorkPwd(String dbWorkPwd) {

        setExtProperty("db.pool." + getPool() + ".password", dbWorkPwd);
    }

    /** 
     * Returns the password of the database from the properties 
     * 
     * @return the password for the OpenCms database user 
     */
    public String getDbWorkPwd() {
        return getExtProperty("db.pool." + getPool() + ".password");
    }

    /** 
     * Returns the extended properties 
     *
     * @return the extended properties  
     */
    public ExtendedProperties getProperties() {
        return m_extProperties;
    }

    /** 
     * Adds a new error message to the vector 
     *
     * @param error the error message 
     */
    public static void setErrors(String error) {
        errors.add(error);
    }

    /** 
     * Returns the error messages 
     * 
     * @return a vector of error messages 
     */
    public Vector getErrors() {
        return errors;
    }

    /** 
     * Returns the path to the opencms config folder 
     *
     * @return the path to the config folder 
     */
    public String getConfigFolder() {
        return (m_basePath + "WEB-INF/config/").replace('\\', '/').replace('/', File.separatorChar);
    }

    /** 
     * Sets the database driver belonging to the database 
     * 
     * @param driver name of the opencms driver 
     */
    public void setDbDriver(String driver) {
        setDbProperty(m_databaseKey + ".driver", driver);
    }

    /** 
     * Returns the database driver belonging to the database
     * from the default configuration 
     *
     * @return name of the opencms driver 
     */
    public String getDbDriver() {
        return getDbProperty(m_databaseKey + ".driver");
    }

    /** 
     * Returns the validation query belonging to the database
     * from the default configuration 
     *
     * @return query used to validate connections 
     */
    public String getDbTestQuery() {
        return getDbProperty(m_databaseKey + ".testQuery");
    }

    /** 
     * Sets the validation query to the given value 
     *
     * @param query query used to validate connections   
     */
    public void setTestQuery(String query) {
        setExtProperty("db.pool." + getPool() + ".testQuery", query);
    }

    /** 
     * Returns the validation query 
     *
     * @return query used to validate connections 
     */
    public String getTestQuery() {
        return getExtProperty("db.pool." + getPool() + ".testQuery");
    }

    /** 
     * Sets the minimum connections to the given value 
     * 
     * @param minConn number of minimum connections
     */
    public void setMinConn(String minConn) {
        setExtProperty("db.pool." + getPool() + ".maxIdle", minConn);
    }

    /** 
     * Returns the min. connections.<p>
     * 
     * @return the min. connections
     */
    public String getMinConn() {
        return getExtProperty("db.pool." + getPool() + ".maxIdle");
    }

    /** 
     * Sets the maximum connections to the given value.<p>
     * 
     * @param maxConn maximum connection count
     */
    public void setMaxConn(String maxConn) {
        setExtProperty("db.pool." + getPool() + ".maxActive", maxConn);
    }

    /** 
     * Returns the max. connections.<p>
     * 
     * @return the max. connections
     */
    public String getMaxConn() {
        return getExtProperty("db.pool." + getPool() + ".maxActive");
    }

    /** 
     * Sets the timeout to the given value.<p>
     * 
     * @param timeout the timeout to set
     */
    public void setTimeout(String timeout) {
        setExtProperty("db.pool." + getPool() + ".maxWait", timeout);
    }

    /** 
     * Returns the timeout value.<p>
     * 
     * @return the timeout value
     */
    public String getTimeout() {
        return getExtProperty("db.pool." + getPool() + ".maxWait");
    }

    /** 
     * Set the mac ethernet address, required for UUID generation.<p>
     * 
     * @param ethernetAddress the mac addess to set
     */
    public void setEthernetAddress(String ethernetAddress) {
        setExtProperty("server.ethernet.address", ethernetAddress);
    }

    /** 
     * Return the mac ethernet address
     * 
     * @return the mac ethernet addess
     */
    public String getEthernetAddress() {
        return getExtProperty("server.ethernet.address");
    }
    
    /** 
     * Return the OpenCms server name
     * 
     * @return the OpenCms server name
     */
    public String getServerName() {
        return getExtProperty("server.name");
    }
    
    /** 
     * Set the OpenCms server name
     * 
     * @param name the OpenCms server name
     */
    public void setServerName(String name) {
        setExtProperty("server.name", name);
    }

    /** 
     * Set the maximum file upload size.<p>
     * 
     * @param size the size to set
     */
    public void setFileMaxUploadSize(String size) {
        setExtProperty("workplace.file.maxuploadsize", size);
    }

    /** 
     * Returns the maximum file upload size.<p>
     * 
     * @return the maximum file upload size
     */
    public String getFileMaxUploadSize() {
        return getExtProperty("workplace.file.maxuploadsize");
    }

    /**
     * Returns the database name.<p>
     * 
     * @return the database name
     */
    public String getDb() {
        return getDbProperty(m_databaseKey + ".dbname");
    }

    /**
     * Sets the database name.<p>
     * 
     * @param db the database name to set
     */
    public void setDb(String db) {
        setDbProperty(m_databaseKey + ".dbname", db);
    }

    /**
     * Returns the database create statement.<p>
     * 
     * @return the database create statement
     */
    public String getDbCreateConStr() {
        String str = null;
        str = getDbProperty(m_databaseKey + ".constr");
        return str;
    }

    /**
     * Sets the database create statement.<p>
     * 
     * @param dbCreateConStr the database create statement
     */
    public void setDbCreateConStr(String dbCreateConStr) {
        setDbProperty(m_databaseKey + ".constr", dbCreateConStr);
    }

    /**
     * Returns the database user that is used to connect to the database.<p>
     * 
     * @return the database user
     */
    public String getDbCreateUser() {
        return getDbProperty(m_databaseKey + ".user");
    }

    /**
     * Set the database user that is used to connect to the database.<p>
     * 
     * @param dbCreateUser the user to set
     */
    public void setDbCreateUser(String dbCreateUser) {
        setDbProperty(m_databaseKey + ".user", dbCreateUser);
    }

    /**
     * Returns the password used for database creation.<p>
     * 
     * @return the password used for database creation
     */
    public String getDbCreatePwd() {
        return (m_dbCreatePwd != null) ? m_dbCreatePwd : "";
    }

    /**
     * Sets the password used for the initial OpenCms database creation.<p>
     * 
     * This password will not be stored permanently, 
     * but used only in the setup wizard.<p>
     * 
     * @param dbCreatePwd the password used for the initial OpenCms database creation
     */
    public void setDbCreatePwd(String dbCreatePwd) {
        m_dbCreatePwd = dbCreatePwd;
    }

    /** 
     * Checks if the setup wizard is enabled.<p>
     * 
     * @return true if the setup wizard is enables, false otherwise
     */
    public boolean getWizardEnabled() {
        return "true".equals(getExtProperty("wizard.enabled"));
    }

    /**
     * Locks (i.e. disables) the setup wizard.<p>
     *
     */
    public void lockWizard() {
        setExtProperty("wizard.enabled", "false");
    }

    /**
     * Sets filename translation to enabled / disabled.<p>
     * 
     * @param value value to set (must be "true" or "false")
     */
    public void setFilenameTranslationEnabled(String value) {
        setExtProperty("filename.translation.enabled", value);
    }

    /** 
     * Returns "true" if filename translation is enabled.<p>
     * 
     * @return "true" if filename translation is enabled
     */
    public String getFilenameTranslationEnabled() {
        return getExtProperty("filename.translation.enabled");
    }
    
    /**
     * Returns the specified HTML part of the HTML property file to create the output.<p>
     * 
     * @param part the name of the desired part
     * @return the HTML part or an empty String, if the part was not found
     */
    public String getHtmlPart(String part) {
        return getHtmlPart(part, "");
    }
    
    /**
     * Returns the specified HTML part of the HTML property file to create the output.<p>
     * 
     * @param part the name of the desired part
     * @param replaceString String which is inserted in the found HTML part at the location of "$replace$"
     * @return the HTML part or an empty String, if the part was not found
     */
    public String getHtmlPart(String part, String replaceString) {
        String value = m_htmlProps.getProperty(part);
        if (value == null) {
            return "";
        } else {
            return CmsStringSubstitution.substitute(value, "$replace$", replaceString);
        }
    }

    /**
     * Sets directory translation to enabled / disabled.<p>
     * 
     * @param value value to set (must be "true" or "false")
     */    
    public void setDirectoryTranslationEnabled(String value) {
        setExtProperty("directory.translation.enabled", value);
    }

    /** 
     * Returns "true" if directory translation is enabled.<p>
     * 
     * @return "true" if directory translation is enabled
     */
    public String getDirectoryTranslationEnabled() {
        return getExtProperty("directory.translation.enabled");
    }

    /**
     * Sets the directory default index files.<p>
     * 
     * This must be a comma separated list of files.<p>
     *
     * @param value the value to set
     */
    public void setDirectoryIndexFiles(String value) {
        setExtProperty("directory.default.files", value);
    }

    /**
     * Returns the directory default index files as a comma separated list.<p>
     * 
     * @return the directory default index files as a comma separated list
     */
    public String getDirectoryIndexFiles() {
        Object value = null;
        value = m_extProperties.get("directory.default.files");

        if (value == null) {
            // could be null...
            return "";
        }

        if (value instanceof String) {
            // ...a string...
            return value.toString();
        }

        // ...or a vector!
        Enumeration allIndexFiles = ((Vector)value).elements();
        String indexFiles = "";

        while (allIndexFiles.hasMoreElements()) {
            indexFiles += (String)allIndexFiles.nextElement();

            if (allIndexFiles.hasMoreElements()) {
                indexFiles += ",";
            }
        }

        return indexFiles;
    }

    /**
     * Over simplistic helper to compare two strings to check radio buttons.
     * 
     * @param value1 the first value 
     * @param value2 the secound value
     * @return "checked" if both values are equal, the empty String "" otherwise
     */
    public String isChecked(String value1, String value2) {
        if (value1 == null || value2 == null) {
            return "";
        }

        if (value1.trim().equalsIgnoreCase(value2.trim())) {
            return "checked";
        }

        return "";
    }

    /**
     * Returns the defaultContentEncoding.
     * @return String
     */
    public String getDefaultContentEncoding() {
        return getExtProperty("defaultContentEncoding");
    }

    /**
     * Sets the defaultContentEncoding.
     * @param defaultContentEncoding The defaultContentEncoding to set
     */
    public void setDefaultContentEncoding(String defaultContentEncoding) {
        setExtProperty("defaultContentEncoding", defaultContentEncoding);
    }

    /**
     * Sets the webapp name
     * 
     * @param value the new webapp name
     */
    public void setAppName(String value) {
        setExtProperty("app.name", value);
    }

    /**
     * Returns the webapp name
     * 
     * @return the webapp name
     */
    public String getAppName() {
        return getExtProperty("app.name");
    }

    /**
     * Returns the replacer.<p>
     * 
     * @return the replacer
     */
    public Map getReplacer() {
        return m_replacer;
    }
    
    /**
     * Sets the replacer.<p>
     * 
     * @param map the replacer to set
     */
    public void setReplacer(Map map) {
        m_replacer = map;
    }
    
    /**
     * Returns the clear text name for a database server setup specified by a database key (e.g. "mysql", "generic" or "oracle").<p>
     * 
     * @param databaseKey a database key (e.g. "mysql", "generic" or "oracle")
     * @return the clear text name for a database server setup
     */
    public String getDatabaseName(String databaseKey) {
        return (String) ((Map) getDatabaseProperties().get(databaseKey)).get(databaseKey + ".name");
    }    
    
    /**
     * Returns a map with lists of dependent module package names keyed by module package names.<p>
     * 
     * @return a map with lists of dependent module package names keyed by module package names
     */
    public Map getModuleDependencies() {
        getAvailableModules();
        return m_moduleDependencies;
}
    /**
     * Returns a map with all available modules.<p>
     * 
     * The map contains maps keyed by module package names. Each of these maps contains various
     * information about the module such as the module name, version, description, and a list of 
     * it's dependencies. You should refer to the source code of this method to understand the data 
     * structure of the map returned by this method!<p>
     * 
     * @return a map with all available modules
     */
    public Map getAvailableModules() {
        File packagesFolder = null;
        File[] childResources = null;
        File childResource = null;
        Document manifest = null;
        String moduleName = null;
        String moduleNiceName = null;
        String moduleVersion = null;
        String moduleDescription = null;
        List dependencyNodes = null;
        List moduleDependencies = null;
        Element rootElement = null;
        Element moduleDependency = null;
        String moduleDependencyName = null;
        Map module = null;

        if (m_availableModules != null) {
            return m_availableModules;
        }

        try {
            m_availableModules = (Map) new HashMap();
            m_moduleDependencies = (Map) new HashMap();
            
            // open the folder "/WEB-INF/packages/modules/"
            packagesFolder = new File(m_basePath + "WEB-INF" + File.separator + "packages" + File.separator + "modules");

            if (packagesFolder.exists()) {
                // list all child resources in the packages folder
                childResources = packagesFolder.listFiles();

                if (childResources != null) {
                    for (int i = 0; i < childResources.length; i++) {
                        childResource = childResources[i];

                        // try to get manifest.xml either from a ZIP file or a subfolder
                        if (childResource.exists() && childResource.canRead() && (manifest = getManifest(childResource)) != null) {                           
                            // get the "export" node
                            rootElement = manifest.getRootElement();                            
                            // module package name
                            moduleName = ((Element) rootElement.selectNodes("//export/module/name").get(0)).getTextTrim();
                            // module nice name
                            moduleNiceName = ((Element) rootElement.selectNodes("//export/module/nicename").get(0)).getTextTrim();
                            // module version
                            moduleVersion = ((Element) rootElement.selectNodes("//export/module/version").get(0)).getTextTrim();
                            // module description
                            moduleDescription = ((Element) rootElement.selectNodes("//export/module/description").get(0)).getTextTrim();
                            // all module "dependency" sub nodes
                            dependencyNodes = rootElement.selectNodes("//export/module/dependencies/dependency");
                            
                             // if module a depends on module b, and module c depends also on module b:
                             // build a map with a list containing "a" and "c" keyed by "b" to get a 
                             // list of modules depending on module "b"...
                            for (int j = 0; j < dependencyNodes.size(); j++) {
                                moduleDependency = (Element) dependencyNodes.get(j);
                                
                                // module dependency package name
                                moduleDependencyName = ((Element) moduleDependency.selectNodes("./name").get(0)).getTextTrim();
                                // get the list of dependend modules ("b" in the example)
                                moduleDependencies = (List) m_moduleDependencies.get(moduleDependencyName);
                                
                                if (moduleDependencies == null) {
                                    // build a new list if "b" has no dependend modules yet
                                    moduleDependencies = (List) new ArrayList();
                                    m_moduleDependencies.put(moduleDependencyName, moduleDependencies);
                                }
                                
                                // add "a" as a module depending on "b"
                                moduleDependencies.add(moduleName);
                            }
                            
                            // create a map holding the collected module information
                            module = (Map) new HashMap();
                            module.put("name", moduleName);
                            module.put("niceName", moduleNiceName);
                            module.put("version", moduleVersion);
                            module.put("description", moduleDescription);
                            
                            // put the module information into a map keyed by the module packages names
                            m_availableModules.put(moduleName, module);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace(System.err);
        }

        return m_availableModules;
    }
    
    /**
     * Returns the "manifest.xml" of an available module as a dom4j document.<p>
     * 
     * The manifest is either read as a ZIP entry, or from a subfolder of the specified
     * file resource.<p>
     * 
     * @param resource a File resource
     * @return the "manifest.xml" as a dom4j document
     */
    protected Document getManifest(File resource) {
        Document manifest = null;
        ZipFile zipFile = null;
        ZipEntry zipFileEntry = null;
        InputStream input = null;
        Reader reader = null;
        SAXReader saxReader = null;
        File manifestFile = null;

        try {
            if (resource.isFile()) {
                // create a Reader either from a ZIP file's manifest.xml entry...
                zipFile = new ZipFile(resource);
                zipFileEntry = zipFile.getEntry("manifest.xml");
                input = zipFile.getInputStream(zipFileEntry);
                reader = new BufferedReader(new InputStreamReader(input));
            } else if (resource.isDirectory()) {
                // ...or from subresource inside a folder
                manifestFile = new File(resource, "manifest.xml");
                reader = new BufferedReader(new FileReader(manifestFile));                
            }
            
            // transform the manifest.xml file into a dom4j Document
            saxReader = new SAXReader();
            manifest = saxReader.read(reader);            
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace(System.err);
            manifest = null;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                // noop
            }
        }

        return manifest;
    }
    
}