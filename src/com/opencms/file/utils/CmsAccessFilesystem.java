/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/utils/Attic/CmsAccessFilesystem.java,v $
* Date   : $Date: 2002/12/06 23:16:58 $
* Version: $Revision: 1.10 $
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

package com.opencms.file.utils;

import com.opencms.core.A_OpenCms;
import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.core.I_CmsLogChannels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Hashtable;


public class CmsAccessFilesystem implements I_CmsConstants {

    /**
    * This is the Hashtable of exportpoints.
    */
    private Hashtable m_exportpointStorage  = null;

    /**
     * Constructor, creartes a new CmsAccessFilefilesystem object.
     *
     * @param mountpoint The mountpoint of this filesystem access module.
     *
     * @exception CmsException Throws CmsException if connection fails.
     *
     */
    public CmsAccessFilesystem(Hashtable exportpoints)
        throws CmsException {
        m_exportpointStorage = exportpoints;
    }
    /**
     * Calculates the absolute path in the filesystem.
     *
     * @param filename Name of a file in the OpenCms system.
     * @param exportpoint the key in the Hashtable m_exportpointStorage.
     * @return Absolute path of a the file in the disk filesystem.
     */
    private String absoluteName(String filename, String exportpoint){

        String path = null;
        String exportpath = (String) m_exportpointStorage.get(exportpoint);
        path = exportpath + filename.substring(exportpoint.length());
        return path;
    }
/**
 * Creates a new folder
 *
 * @param foldername The complete path to the folder.
 *
 *
 * @exception CmsException Throws CmsException if operation was not succesful.
 */
public void createFolder(String foldername, String key) throws CmsException {

    // create folder
    File discFolder = new File(absoluteName(foldername, key));
    // check if this folder already exits
    if (!discFolder.exists()) {
        boolean success = discFolder.mkdirs();
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() && (!success)) {
            A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_INFO, "[CmsAccessFilesystem] Couldn't create folder " + absoluteName(foldername, key) + ".");
        }
        //  throw new CmsException("[" + this.getClass().getName() + "] "+foldername,CmsException.C_FILESYSTEM_ERROR);
    }
}
     /**
      * Deletes a file or folder in the filesytem.
      *
      * @param filename The complete path of the file or folder.
      * @exception CmsException Throws CmsException if operation was not succesful
      */
     public void removeResource(String filename, String key)
        throws CmsException{

         File discFile=new File(absoluteName(filename,key));
         // check if file exists
         if (discFile.exists()){
            boolean success=discFile.delete();
            if (!success) {
            //       throw new CmsException("[" + this.getClass().getName() + "] "+filename,CmsException.C_FILESYSTEM_ERROR);
            }
         }
     }
    /**
     * Creates or writes a file with the given content.
     *
     * @param filename The complete name of the new file (including pathinformation).
     * @param contents The contents of the new file.
     *
     *
     * @exception CmsException Throws CmsException if operation was not succesful
     */
     public void writeFile(String filename, String key, byte[] contents)

         throws CmsException {

             File discFile= new File (absoluteName(filename,key));
             try {
                 // write the new file to disk
                 OutputStream s = new FileOutputStream(discFile);
                 s.write(contents);
                 s.close();
             } catch (Exception e) {
              // throw new CmsException("[" + this.getClass().getName() + "] "+e.getMessage());
             }
     }
}
