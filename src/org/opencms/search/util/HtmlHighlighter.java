/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/search/util/Attic/HtmlHighlighter.java,v $
 * Date   : $Date: 2004/02/11 15:01:01 $
 * Version: $Revision: 1.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2004 Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the m_terms of the GNU Lesser General Public
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
package org.opencms.search.util;

/**
 * @version $Revision: 1.1 $ $Date: 2004/02/11 15:01:01 $
 * @author Carsten Weinholz (c.weinholz@alkacon.com)
 */
public class HtmlHighlighter implements I_TermHighlighter {

    /**
     * @see org.opencms.search.util.I_TermHighlighter#highlightTerm(java.lang.String)
     */
    public String highlightTerm(String term) {
        return "<b>" + term + "</b>";
    }

}
