/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/search/extractors/CmsExtractorRtf.java,v $
 * Date   : $Date: 2005/03/26 11:37:38 $
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

import java.io.InputStream;

import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;

/**
 * Extracts the text form a RTF  document.<p>
 * 
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @since 5.7.2
 */
public final class CmsExtractorRtf extends A_CmsTextExtractor {

    /** Static member instance of the extractor. */
    private static final CmsExtractorRtf m_instance = new CmsExtractorRtf();

    /**
     * Hide the public constructor.<p> 
     */
    private CmsExtractorRtf() {

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

        // use build in RTF parser from Swing API
        RTFEditorKit rtfEditor = new RTFEditorKit();
        Document doc = rtfEditor.createDefaultDocument();
        rtfEditor.read(in, doc, 0);

        String result = doc.getText(0, doc.getLength());
        result = removeControlChars(result);

        return new CmsExtractionResult(result);
    }

}