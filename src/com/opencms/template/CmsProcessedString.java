/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/template/Attic/CmsProcessedString.java,v $
* Date   : $Date: 2001/07/31 15:50:16 $
* Version: $Revision: 1.3 $
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

package com.opencms.template;

/**
 * Special class for String results returned in template engine.
 * Template objects should use this class instead of <code>String</code>
 * if they have written their results to the response output stream
 * theirselves (if the system is in streaming mode).
 * <P>
 * If an object doesn't care about HTTP streaming and simply generates
 * a String for returning to the template engine, it really shouldn't
 * make use of this class and return the original String.
 *
 * @author Alexander Lucas <alexander.lucas@framfab.de>
 * @version $Revision: 1.3 $ $Date: 2001/07/31 15:50:16 $
 */
public class CmsProcessedString {

    /** Store for the original String */
    String m_orgString;

    /** Constructor for a new CmsProcessedString object */
    public CmsProcessedString(String s) {
        m_orgString = s;
    }

    /** Constructor for a new CmsProcessedString object */
    public CmsProcessedString(byte[] b) {
        if(b == null) {
            m_orgString = null;
        } else {
            m_orgString = new String(b);
        }
    }

    /** Get back the original String */
    public String toString() {
        return m_orgString;
    }
}
