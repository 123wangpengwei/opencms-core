/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/search/documents/Attic/CmsWordDocument.java,v $
 * Date   : $Date: 2005/03/04 13:42:45 $
 * Version: $Revision: 1.10 $
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


import org.opencms.main.CmsException;
import org.opencms.search.CmsIndexException;
import org.opencms.search.A_CmsIndexResource;
import org.opencms.util.CmsStringUtil;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;

import java.io.ByteArrayInputStream;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * Lucene document factory class to extract index data from a cms resource 
 * containing MS Word data.<p>
 * 
 * @version $Revision: 1.10 $ $Date: 2005/03/04 13:42:45 $
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 */
public class CmsWordDocument extends CmsVfsDocument {

    /**
     * Creates a new instance of this lucene document factory.<p>
     * 
     * @param name name of the documenttype
     */
    public CmsWordDocument(String name) {
        super(name);
    }
    
    /**
     * Returns the raw text content of a given vfs resource containing MS Word data.<p>
     * 
     * @see org.opencms.search.documents.CmsVfsDocument#getRawContent(org.opencms.file.CmsObject, org.opencms.search.A_CmsIndexResource, java.lang.String)
     */
    public String getRawContent(CmsObject cms, A_CmsIndexResource indexResource, String language) throws CmsException {
        
        CmsResource resource = (CmsResource)indexResource.getData();
        String rawContent = null;
        
        try {
            CmsFile file = cms.readFile(cms.getRequestContext().removeSiteRoot(resource.getRootPath()), CmsResourceFilter.IGNORE_EXPIRATION);
            if (!(file.getLength() > 0)) {
                throw new CmsIndexException("Resource " + resource.getRootPath() + " has no content.");
            }
 
            CmsWordExtractor extractor = new CmsWordExtractor();
            rawContent = extractor.extractText(new ByteArrayInputStream(file.getContents()));
            
            rawContent = CmsStringUtil.substitutePerl(rawContent, internalWordToken("PAGEREF"), "$1", "g");
            rawContent = CmsStringUtil.substitutePerl(rawContent, internalWordToken("REF"), "$1", "g");
            rawContent = CmsStringUtil.substitutePerl(rawContent, internalWordToken("HYPERLINK"), "$1", "g");
            rawContent = CmsStringUtil.substitutePerl(rawContent, internalWordToken("\\w*"), "$1", "g");
            rawContent = CmsStringUtil.substitutePerl(rawContent, "[^[:print:]]", " ", "g");
               
        } catch (Exception exc) {
            throw new CmsIndexException("Reading resource " + resource.getRootPath() + "failed.", exc);
        }
        
        return rawContent;
    }
    
    /**
     * Returns a pattern that matches an inline word element.<p>
     * 
     * @param name the name of the element
     * @return the pattern that matches the element
     */
    private String internalWordToken(String name) {
        return "\u0013" + "\\s*?" + name + ".*?" + "\u0014" + "(.*?)" + "\u0015"; 
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
}
