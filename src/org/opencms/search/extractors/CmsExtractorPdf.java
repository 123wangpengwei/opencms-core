/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/search/extractors/CmsExtractorPdf.java,v $
 * Date   : $Date: 2005/03/27 20:37:38 $
 * Version: $Revision: 1.2 $
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
 
package org.opencms.search.extractors;

import org.opencms.util.CmsDateUtil;
import org.opencms.util.CmsStringUtil;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.pdfbox.encryption.DocumentEncryption;
import org.pdfbox.pdfparser.PDFParser;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.pdmodel.PDDocumentInformation;
import org.pdfbox.util.PDFTextStripper;


/**
 * Extracts the text form a PDF document.<p>
 * 
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @since 5.7.2
 */
public final class CmsExtractorPdf extends A_CmsTextExtractor {

    /** Static member instance of the extractor. */
    private static final CmsExtractorPdf m_instance = new CmsExtractorPdf();
    
    /**
     * Hide the public constructor.<p> 
     */
    private CmsExtractorPdf() {
        // noop
    }
    
    /**
     * Returns an instance of this text extractor.<p> 
     * 
     * @return an instance of this text extractor
     */
    public static I_CmsTextExtractor getExtractor() {
        return m_instance;
    }
    
    /**
     * @see org.opencms.search.extractors.I_CmsTextExtractor#extractText(java.io.InputStream, java.lang.String)
     */
    public I_CmsExtractionResult extractText(InputStream in, String encoding) throws Exception {

        PDDocument pdfDocument = null;

        try {
            PDFParser parser = new PDFParser(in);
            parser.parse();
            
            pdfDocument = parser.getPDDocument();
            
            // check for encryption
            if (pdfDocument.isEncrypted()) {
                DocumentEncryption decryptor = new DocumentEncryption(pdfDocument);
                // try using the default password
                decryptor.decryptDocument("");
            }

            // create PDF stripper
            PDFTextStripper stripper = new PDFTextStripper();
            PDDocumentInformation info = pdfDocument.getDocumentInformation();
            
            Map metaInfo = new HashMap();
            // append document meta data to content
            String meta;
            meta = info.getTitle();
            if (CmsStringUtil.isNotEmpty(meta)) {
                metaInfo.put(I_CmsExtractionResult.META_TITLE, meta);
            }
            meta = info.getKeywords();
            if (CmsStringUtil.isNotEmpty(meta)) {
                metaInfo.put(I_CmsExtractionResult.META_KEYWORDS, meta);                
            }
            meta = info.getSubject();
            if (CmsStringUtil.isNotEmpty(meta)) {
                metaInfo.put(I_CmsExtractionResult.META_SUBJECT, meta);                
            }
            // extract other available meta information
            meta = info.getAuthor();
            if (CmsStringUtil.isNotEmpty(meta)) {
                metaInfo.put(I_CmsExtractionResult.META_AUTHOR, meta);                
            }
            meta = info.getCreator();
            if (CmsStringUtil.isNotEmpty(meta)) {
                metaInfo.put(I_CmsExtractionResult.META_CREATOR, meta);                
            }
            meta = info.getProducer();
            if (CmsStringUtil.isNotEmpty(meta)) {
                metaInfo.put(I_CmsExtractionResult.META_PRODUCER, meta);                
            }
            if (info.getCreationDate() != null) {
                meta = CmsDateUtil.getHeaderDate(info.getCreationDate().getTime().getTime());
                metaInfo.put(I_CmsExtractionResult.META_DATE_CREATED, meta);                
            }
            if (info.getModificationDate() != null) {
                meta = CmsDateUtil.getHeaderDate(info.getModificationDate().getTime().getTime());
                metaInfo.put(I_CmsExtractionResult.META_DATE_LASTMODIFIED, meta);                
            }
            
            // add the main document text
            String result = stripper.getText(pdfDocument);
            
            // return the final result
            return new CmsExtractionResult(result, metaInfo);

        } finally {
            if (pdfDocument != null) {
                pdfDocument.close();
            }
        }
    }    
}
