/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/core/exceptions/Attic/CmsPlausibilizationException.java,v $
* Date   : $Date: 2001/07/31 15:50:13 $
* Version: $Revision: 1.2 $
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

package com.opencms.core.exceptions;

import java.io.*;
import java.util.*;
import com.opencms.core.*;

/**
 * This exception is thrown to signalize plausibilization errors in backoffice modules.
 *
 * @author Michael Emmerich
 * @version $Revision: 1.2 $ $Date: 2001/07/31 15:50:13 $
 */
public class CmsPlausibilizationException extends CmsException {


  private Vector m_error=new Vector();

  /**
  * Constructs a CmsPlauzibilizationException.
  * @param error A Vector of error codes.
  */
  public  CmsPlausibilizationException(Vector error) {
    m_error=error;
  }

  /**
   * Retuens a Vector of all error codes of the Exception.
   * This vector consits of single Strings.
   * @return Vector of error codes.
   */
  public Vector getErrorCodes() {
    return m_error;
  }

  /**
  * Overwrites the standard toString method.
  */
  public String toString() {
    StringBuffer output = new StringBuffer();
    output.append("[CmsPlauzibilizationException]: ");
    //loop through all error codes
    for (int i=0;i<m_error.size();i++) {
      output.append("("+(String)m_error.elementAt(i)+") ");
    }

    return output.toString();
  }
}
