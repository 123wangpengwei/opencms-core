/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/search/CmsSearchIndexSource.java,v $
 * Date   : $Date: 2004/07/02 16:05:08 $
 * Version: $Revision: 1.1 $
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
 
package org.opencms.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A search index source is a description of a list of Cms resources
 * to be indexed.<p>
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @version $Revision: 1.1 $
 * @since 5.3.5
 */
public class CmsSearchIndexSource implements Serializable, Cloneable {

    /** The logical key/name of this index. */
    private String m_name;
    
    /** The class name of the indexer. */
    private String m_indexerClassName;
    
    /** A map of optional key/value parameters. */
    private Map m_params;
    
    /** A list of Cms resources to be indexed. */
    private List m_resourcesNames;
    
    /** A list of Cms resource types to be indexed. */
    private List m_documentTypes;
    
    /**
     * Creates a new CmsSearchIndexSource.<p>
     */
    public CmsSearchIndexSource() {
        
        m_params = new HashMap();
        m_resourcesNames = new ArrayList();
        m_documentTypes = new ArrayList();
    }
    
    /**
     * Returns the list of Cms resource types to be indexed.<p>
     *
     * @return the list of Cms resource types to be indexed
     */
    public List getDocumentTypes() {

        return m_documentTypes;
    }
    
    /**
     * Returns the class name of the indexer.<p>
     *
     * @return the class name of the indexer
     */
    public String getIndexerClassName() {

        return m_indexerClassName;
    }
    
    /**
     * Returns the logical key/name of this search index source.<p>
     *
     * @return the logical key/name of this search index source
     */
    public String getName() {

        return m_name;
    }
    
    /**
     * Returns the map of optional key/value parameters.<p>
     *
     * @return the map of optional key/value parameters
     */
    public Map getParams() {

        return m_params;
    }
    
    /**
     * Returns the list of Cms resources to be indexed.<p>
     *
     * @return the list of Cms resources to be indexed
     */
    public List getResourcesNames() {

        return m_resourcesNames;
    }
    
    /**
     * Sets the list of Cms resource types to be indexed.<p>
     *
     * @param documentTypes the list of Cms resource types to be indexed
     */
    public void setDocumentTypes(List documentTypes) {

        m_documentTypes = documentTypes;
    }
    
    /**
     * Sets the class name of the indexer.<p>
     *
     * @param indexerClassName the class name of the indexer
     */
    public void setIndexerClassName(String indexerClassName) {

        m_indexerClassName = indexerClassName;
    }
    
    /**
     * Sets the logical key/name of this search index source.<p>
     *
     * @param name the logical key/name of this search index source
     */
    public void setName(String name) {

        m_name = name;
    }
    
    /**
     * Sets the map of optional key/value parameters.<p>
     *
     * @param params the map of optional key/value parameters
     */
    public void setParams(Map params) {

        m_params = params;
    }
    
    /**
     * Sets the list of Cms resources to be indexed.<p>
     *
     * @param resources the list of Cms resources to be indexed
     */
    public void setResourcesNames(List resources) {

        m_resourcesNames = resources;
    }
    
    /**
     * Adds a parameter.<p>
     * 
     * @param key the key/name of the parameter
     * @param value the value of the parameter
     */
    public void addConfigurationParameter(String key, String value) {
        
        m_params.put(key, value);
    }
    
    /**
     * Adds the path of a Cms resource.<p>
     * 
     * @param resourceName the path of a Cms resource
     */
    public void addResourceName(String resourceName) {
        
        m_resourcesNames.add(resourceName);
    }
    
    /**
     * Adds the key/name of a document type.<p>
     * 
     * @param key the key/name of a document type
     */
    public void addDocumentType(String key) {
        
        m_documentTypes.add(key);
    }
    
    /**
     * Returns the value for a specified parameter key.<p>
     * 
     * @param key the parameter key/name
     * @return the value for the specified parameter key
     */
    public String getParam(String key) {
        
        return (String)m_params.get(key);
    }
}
