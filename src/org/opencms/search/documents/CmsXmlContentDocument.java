/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/search/documents/Attic/CmsXmlContentDocument.java,v $
 * Date   : $Date: 2005/03/04 13:42:45 $
 * Version: $Revision: 1.4 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2005 Alkacon Software (http://www.alkacon.com)
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
package org.opencms.search.documents;

import org.opencms.i18n.CmsLocaleManager;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.search.CmsIndexException;
import org.opencms.search.A_CmsIndexResource;
import org.opencms.xml.A_CmsXmlDocument;
import org.opencms.xml.content.CmsXmlContentFactory;
import org.opencms.xml.types.I_CmsXmlContentValue;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.file.types.CmsResourceTypeXmlContent;
import org.opencms.file.types.I_CmsResourceType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * Lucene document factory class to extract index data from a cms resource 
 * of type <code>CmsResourceTypeXmlContent</code>.<p>
 * 
 * @version $Revision: 1.4 $ $Date: 2005/03/04 13:42:45 $
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 */
public class CmsXmlContentDocument extends CmsVfsDocument {

    /**
     * Creates a new instance of this lucene document factory.<p>
     * 
     * @param name name of the documenttype
     */
    public CmsXmlContentDocument (String name) {
        super(name);
    }
    
    /**
     * Returns the raw text content of a given vfs resource of type <code>CmsResourceTypeXmlContent</code>.<p>
     * 
     * @see org.opencms.search.documents.CmsVfsDocument#getRawContent(org.opencms.file.CmsObject, org.opencms.search.A_CmsIndexResource, java.lang.String)
     */
    public String getRawContent(CmsObject cms, A_CmsIndexResource indexResource, String language) throws CmsException {

        CmsResource resource = (CmsResource)indexResource.getData();
        String rawContent = null;
        
        try {
            CmsFile file = CmsFile.upgrade(resource, cms);
            String absolutePath = cms.getSitePath(file);
            A_CmsXmlDocument xmlContent = CmsXmlContentFactory.unmarshal(cms, file);
            
            List locales = xmlContent.getLocales();
            if (locales.size() == 0) {
                locales = OpenCms.getLocaleManager().getDefaultLocales(cms, absolutePath);
            }
            Locale locale = OpenCms.getLocaleManager().getBestMatchingLocale(
                    CmsLocaleManager.getLocale(language), 
                    OpenCms.getLocaleManager().getDefaultLocales(cms, absolutePath), 
                    locales);
            
            List elements = xmlContent.getNames(locale);
            StringBuffer content = new StringBuffer();
            for (Iterator i = elements.iterator(); i.hasNext();) {
                I_CmsXmlContentValue value = xmlContent.getValue((String)i.next(), locale);
                String plainText = value.getPlainText(cms);
                if (plainText != null) {
                    content.append(plainText);
                    content.append('\n');
                }
            }                
            
            rawContent = content.toString();
            // CmsHtmlExtractor extractor = new CmsHtmlExtractor();
            //rawContent = extractor.extractText(content.toString(), page.getEncoding());
            
        } catch (Exception exc) {
            throw new CmsIndexException("Reading resource " + resource.getRootPath() + " failed", exc);
        }
        
        return rawContent;
    }
    
    /**
     * Generates a new lucene document instance from contents of the given resource.<p>
     * 
     * @see org.opencms.search.documents.I_CmsDocumentFactory#newInstance(org.opencms.file.CmsObject, org.opencms.search.A_CmsIndexResource, java.lang.String)
     */
    public Document newInstance (CmsObject cms, A_CmsIndexResource resource, String language) throws CmsException {
                   
        Document document = super.newInstance(cms, resource, language);
        document.add(Field.Text(I_CmsDocumentFactory.DOC_CONTENT, getRawContent(cms, resource, language)));
        
        return document;
    }
    
    /**
     * @see org.opencms.search.documents.I_CmsDocumentFactory#getDocumentKeys(java.util.List, java.util.List)
     */
    public List getDocumentKeys(List resourceTypes, List mimeTypes) throws CmsException {
     
        if (resourceTypes.contains("*")) {
            ArrayList allTypes = new ArrayList();
            for (Iterator i = OpenCms.getResourceManager().getResourceTypes().iterator(); i.hasNext();) {
                I_CmsResourceType resourceType = (I_CmsResourceType)i.next();
                if (resourceType instanceof CmsResourceTypeXmlContent 
                    && ((CmsResourceTypeXmlContent)resourceType).getConfiguration().containsKey(CmsResourceTypeXmlContent.C_CONFIGURATION_SCHEMA)) {
                    allTypes.add(resourceType.getTypeName());
                }
            }
            resourceTypes = allTypes;
        }
        
        return super.getDocumentKeys(resourceTypes, mimeTypes);
    }
}
