/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsAdminModuleExport.java,v $
* Date   : $Date: 2003/03/07 10:01:15 $
* Version: $Revision: 1.29 $
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

import com.opencms.boot.CmsBase;
import com.opencms.boot.I_CmsLogChannels;
import com.opencms.core.A_OpenCms;
import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.core.I_CmsSession;
import com.opencms.core.OpenCms;
import com.opencms.file.CmsObject;
import com.opencms.file.CmsRegistry;
import com.opencms.file.I_CmsRegistry;
import com.opencms.report.A_CmsReportThread;
import com.opencms.util.Utils;

import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * Template class for displaying OpenCms workplace administration module create.
 *
 * Creation date: (27.10.00 10:28:08)
 * @author Hanjo Riege
 * @author Thomas Weckert
 */
public class CmsAdminModuleExport extends CmsWorkplaceDefault implements I_CmsConstants {

	private final String C_MODULE = "module";
    private final String C_MODULENAME = "modulename";
	private final String C_ACTION = "action";
    private final String C_MODULE_THREAD = "modulethread";    

	private static final int C_MINIMUM_MODULE_RESOURCE_COUNT = C_VFS_NEW_STRUCTURE?1:3;

	private static final int DEBUG = 0;

	/**
	 * Collects all resources of a module to be exported in a string array. By setting the module property
	 * "additional_folders" as a folder list separated by ";", you can specify folders outside the 
	 * "system/modules" directory to be exported with the module!
	 *
	 * @see #getContent(CmsObject, String, String, Hashtable, String)
	 * @param cms CmsObject Object for accessing system resources.
	 * @param templateFile Filename of the template file.
	 * @param elementName Element name of this template in our parent template.
	 * @param parameters Hashtable with all template class parameters.
	 * @param templateSelector template section that should be processed.
	 */
	public byte[] getContent(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() && C_DEBUG) {
			A_OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "getting content of element " + ((elementName == null) ? "<root>" : elementName));
			A_OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "template file is: " + templateFile);
			A_OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "selected template section is: " + ((templateSelector == null) ? "<default>" : templateSelector));
		}
        
        CmsXmlWpTemplateFile xmlTemplateDocument = (CmsXmlWpTemplateFile)getOwnTemplateFile(cms, templateFile, elementName, parameters, templateSelector);
        I_CmsRegistry reg = cms.getRegistry();
        I_CmsSession session = cms.getRequestContext().getSession(true);

		String step = (String) parameters.get(C_ACTION);
        String moduleName = (String) parameters.get(C_MODULENAME);
               
		if (step == null) {
            // first call
            xmlTemplateDocument.setData("modulename", (String)parameters.get(C_MODULE));    
            
        } else if("showResult".equals(step)){
            if (DEBUG > 1) System.out.println("showResult for export");
                     
            // first look if there is already a thread running.
            A_CmsReportThread doTheWork = (A_CmsReportThread)session.getValue(C_MODULE_THREAD);
            if(doTheWork.isAlive()){
                if (DEBUG > 1) System.out.println("showResult: thread is still running");
                // thread is still running
                xmlTemplateDocument.setData("endMethod", "");
                xmlTemplateDocument.setData("text", "");
            }else{
                if (DEBUG > 1) System.out.println("showResult: thread is finished");
                // thread is finished, activate the buttons
                xmlTemplateDocument.setData("endMethod", xmlTemplateDocument.getDataValue("endMethod"));
                xmlTemplateDocument.setData("autoUpdate","");
                xmlTemplateDocument.setData("text", xmlTemplateDocument.getLanguageFile().getLanguageValue("module.lable.exportend"));
                session.removeValue(C_MODULE_THREAD);
            }
            xmlTemplateDocument.setData("data", doTheWork.getReportUpdate());
            return startProcessing(cms, xmlTemplateDocument, elementName, parameters, "updateReport");            
                  
        } else if ("ok".equals(step)) {
            // export is confirmed			
			String[] resourcen = null;
			int resourceCount = 0;
			int i = 0;

			if (reg.getModuleType(moduleName).equals(CmsRegistry.C_MODULE_TYPE_SIMPLE)) {
				// SIMPLE MODULE
				if (DEBUG > 0) {
					System.out.println(moduleName + " is a simple module");
				}

				// check if additional resources outside the system/modules/{exportName} folder were 
				// specified as module resources by reading the property {C_MODULE_PROPERTY_ADDITIONAL_RESOURCES}
				// to the module (in the module administration)
				String additionalResources = OpenCms.getRegistry().getModuleParameterString(moduleName, I_CmsConstants.C_MODULE_PROPERTY_ADDITIONAL_RESOURCES);
				StringTokenizer additionalResourceTokens = null;

				if (additionalResources != null && !additionalResources.equals("")) {
					// add each additonal folder plus its content folder under "content/bodys"
					additionalResourceTokens = new StringTokenizer(additionalResources, I_CmsConstants.C_MODULE_PROPERTY_ADDITIONAL_RESOURCES_SEPARATOR);

					resourceCount = (additionalResourceTokens.countTokens() * 2) + CmsAdminModuleExport.C_MINIMUM_MODULE_RESOURCE_COUNT;

					resourcen = new String[resourceCount];

					// add each resource plus its equivalent at content/bodys to 
					// the string array of all resources for the export
					while (additionalResourceTokens.hasMoreTokens()) {
						String currentResource = additionalResourceTokens.nextToken().trim();

						if (DEBUG > 0) {
							System.err.println("Adding resource: " + currentResource);
							System.err.println("Adding resource: " + C_VFS_PATH_BODIES.substring(0, C_VFS_PATH_BODIES.length() - 1) + currentResource);
						}

						resourcen[i++] = currentResource;
						resourcen[i++] = C_VFS_PATH_BODIES.substring(0, C_VFS_PATH_BODIES.length() - 1) + currentResource;
					}
				}
				else {
					// no additional resources were specified...
				    resourceCount = CmsAdminModuleExport.C_MINIMUM_MODULE_RESOURCE_COUNT;
					resourcen = new String[resourceCount];
					i = 0;
				}
			}
			else {
				// TRADITIONAL MODULE
				if (DEBUG > 0) {
					System.out.println(moduleName + " is a traditional module");
				}

				resourceCount = CmsAdminModuleExport.C_MINIMUM_MODULE_RESOURCE_COUNT;
				resourcen = new String[resourceCount];
				i = 0;
			}

			// finally, add the "standard" module resources to the string of all resources for the export
			// if you add or remove paths here, ensure to adjust CmsAdminModuleExport.C_MINIMUM_MODULE_RESOURCE_COUNT to the proper length!
			resourcen[i++] = C_VFS_PATH_MODULES + moduleName + "/";

			if (!C_VFS_NEW_STRUCTURE) {
				resourcen[i++] = C_VFS_PATH_MODULEDEMOS + moduleName + "/";
				resourcen[i++] = C_VFS_PATH_BODIES.substring(0, C_VFS_PATH_BODIES.length() - 1) + C_VFS_PATH_MODULEDEMOS + moduleName + "/";
			}

			// check if all resources exists and can be read
			for (i = 0; i < resourceCount; i++) {
				try {
					if (resourcen[i] != null) {
						if (DEBUG > 0) {
							System.err.println("reading file header of: " + resourcen[i]);
						}
						cms.readFileHeader(resourcen[i]);
					}
				}
				catch (CmsException e) {
                    // resource did not exist / could not be read
					if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
						A_OpenCms.log(I_CmsLogChannels.C_MODULE_DEBUG, "error exporting module: couldn't add " + resourcen[i] + " to Module\n" + Utils.getStackTrace(e));
					}
					resourcen[i] = resourcen[resourceCount - CmsAdminModuleExport.C_MINIMUM_MODULE_RESOURCE_COUNT];
				}
			}
			try {
				cms.readFileHeader(resourcen[resourceCount - CmsAdminModuleExport.C_MINIMUM_MODULE_RESOURCE_COUNT]);
			}
			catch (CmsException e) {
				if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging()) {
					A_OpenCms.log(I_CmsLogChannels.C_MODULE_DEBUG, "error exporting module: couldn't add " + resourcen[resourceCount - CmsAdminModuleExport.C_MINIMUM_MODULE_RESOURCE_COUNT] + " to Module\n" + "You dont have this module in this project!");
				}
				return startProcessing(cms, xmlTemplateDocument, elementName, parameters, "done");
			}


            /*
			reg.exportModule(exportName, resourcen, com.opencms.boot.CmsBase.getAbsolutePath(cms.readExportPath()) + "/" + I_CmsRegistry.C_MODULE_PATH + exportName + "_" + reg.getModuleVersion(exportName));
			templateSelector = "done";
            */

            String filename = CmsBase.getAbsolutePath(cms.readExportPath()) + "/" + I_CmsRegistry.C_MODULE_PATH + moduleName + "_" + reg.getModuleVersion(moduleName);
            A_CmsReportThread doExport = new CmsAdminModuleExportThread(cms, reg, moduleName, resourcen, filename);
            doExport.start();
            session.putValue(C_MODULE_THREAD, doExport);
            xmlTemplateDocument.setData("time", "5");
            templateSelector = "showresult";
		}

		// now load the template file and start the processing
		return startProcessing(cms, xmlTemplateDocument, elementName, parameters, templateSelector);
	}

	/**
	 * Indicates if the results of this class are cacheable.
	 *
	 * @param cms CmsObject Object for accessing system resources
	 * @param templateFile Filename of the template file
	 * @param elementName Element name of this template in our parent template.
	 * @param parameters Hashtable with all template class parameters.
	 * @param templateSelector template section that should be processed.
	 * @return <EM>true</EM> if cacheable, <EM>false</EM> otherwise.
	 */
	public boolean isCacheable(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
		return false;
	}
}
