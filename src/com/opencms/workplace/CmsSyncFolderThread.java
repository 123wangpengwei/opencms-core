/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsSyncFolderThread.java,v $
* Date   : $Date: 2003/07/23 09:58:55 $
* Version: $Revision: 1.14 $
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


package com.opencms.workplace;

import com.opencms.boot.I_CmsLogChannels;
import com.opencms.core.A_OpenCms;
import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.core.I_CmsSession;
import com.opencms.file.CmsObject;
import com.opencms.report.A_CmsReportThread;
import com.opencms.util.Utils;

import java.util.Vector;

/**
 * Thread for create a new project.
 *
 * @author Edna Falkenhan
 */

public class CmsSyncFolderThread extends A_CmsReportThread {

    private Vector m_folders;

    private CmsObject m_cms;

    private boolean m_newProject;

    private I_CmsSession m_session;

    public CmsSyncFolderThread(CmsObject cms, Vector folders, boolean newProject, I_CmsSession session) {
        super("OpenCms: Synchronizing foldes in project " + cms.getRequestContext().currentProject().getName());
        m_cms = cms;
        m_folders = folders;
        m_newProject = newProject;
        m_session = session;
    }

    public void run() {
         // Dont try to get the session this way in a thread!
         // It will result in a NullPointerException sometimes.
         // !I_CmsSession session = m_cms.getRequestContext().getSession(true);
        try {
            // synchronize the resource
            for(int i = 0;i < m_folders.size();i++) {
                // if a new project was created for synchronisation, copy the resource to the project
                if (m_newProject){
                    m_cms.copyResourceToProject((String)m_folders.elementAt(i));
                }
        m_cms.syncFolder((String)m_folders.elementAt(i));
            }
        }
        catch(CmsException e) {
            m_session.putValue(I_CmsConstants.C_SESSION_THREAD_ERROR, Utils.getStackTrace(e));
            if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(I_CmsLogChannels.C_OPENCMS_CRITICAL, e.getMessage());
            }
        }
    }
    
    /**
     * Returns the part of the report that is ready.
     * 
     * @return the part of the report that is ready
     */
    public String getReportUpdate(){
        return "";
    }     
}