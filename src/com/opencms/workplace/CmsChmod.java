/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsChmod.java,v $
 * Date   : $Date: 2003/09/12 17:38:05 $
 * Version: $Revision: 1.45 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
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

import org.opencms.main.OpenCms;
import org.opencms.workplace.CmsWorkplaceAction;

import com.opencms.core.CmsException;
import com.opencms.core.I_CmsSession;
import com.opencms.file.CmsObject;
import com.opencms.file.CmsRequestContext;
import com.opencms.file.CmsResource;
import com.opencms.util.Encoder;

import java.util.Hashtable;

/**
 * Template class for displaying the chmod screen of the OpenCms workplace.<P>
 *
 * @author Michael Emmerich
 * @version $Revision: 1.45 $ $Date: 2003/09/12 17:38:05 $
 */

public class CmsChmod extends CmsWorkplaceDefault {
    
	/**
	 * Gets the content of the chmod template and processes the data input.<p>
     * 
	 * @param cms The CmsObject.
	 * @param templateFile The chmod template file
	 * @param elementName not used
	 * @param parameters Parameters of the request and the template.
	 * @param templateSelector Selector of the template tag to be displayed.
	 * @return Bytearray containing the processed data of the template.
	 * @throws CmsException if something goes wrong.
	 */
	public byte[] getContent(CmsObject cms, String templateFile, String elementName,
	Hashtable parameters, String templateSelector) throws CmsException {
		I_CmsSession session = cms.getRequestContext().getSession(true);

		// the template to be displayed
		String template = null;

		// clear session values on first load
		String initial = (String)parameters.get(C_PARA_INITIAL);
		if(initial != null) {
			// remove all session values
			session.removeValue(C_PARA_RESOURCE);
			session.removeValue("lasturl");
		}

		// get the lasturl parameter
		String lasturl = getLastUrl(cms, parameters);
		CmsXmlWpTemplateFile xmlTemplateDocument = new CmsXmlWpTemplateFile(cms, templateFile);
		String newaccess = (String)parameters.get(C_PARA_NEWACCESS);

		// get the filename
		String filename = (String)parameters.get(C_PARA_RESOURCE);
		if(filename != null) session.putValue(C_PARA_RESOURCE, filename);
		filename = (String)session.getValue(C_PARA_RESOURCE);
		CmsResource file = cms.readFileHeader(filename);

		// get all access flags from the request
//		String ur = (String)parameters.get("ur");
//		String uw = (String)parameters.get("uw");
//		String uv = (String)parameters.get("uv");
//		String gr = (String)parameters.get("gr");
//		String gw = (String)parameters.get("gw");
//		String gv = (String)parameters.get("gv");
//		String pr = (String)parameters.get("pr");
//		String pw = (String)parameters.get("pw");
//		String pv = (String)parameters.get("pv");
//		String ir = (String)parameters.get("ir");
		String allflag = (String)parameters.get(C_PARA_FLAGS);
		if(allflag == null) allflag = "false";

		// select the template to be displayed
		if(file.isFile()) {
			template = "file";
        } else {
			template = "folder";
        }

		// check if the newaccess parameter is available. This parameter is set when
		// the access flags are modified.
		if(newaccess != null) {

			// check if the current user has the right to change the group of the
			// resource. Only the owner of a file and the admin are allowed to do this.
			CmsRequestContext requestContext = cms.getRequestContext();
			if((requestContext.currentUser().equals(cms.readOwner(file)))
			|| (cms.userInGroup(requestContext.currentUser().getName(),
            OpenCms.getDefaultUsers().getGroupAdministrators()))) {

				// calculate the new access flags
//				TODO: check how to set the appropriate access using acl
				// int flag = 0;
/*
				if(ur != null && ur.equals("true"))
					flag += C_PERMISSION_READ;
				if(uw != null && uw.equals("true"))
					flag += C_PERMISSION_WRITE;
				if(uv != null && uv.equals("true"))
					flag += C_PERMISSION_VIEW;
				if(gr != null && gr.equals("true"))
					flag += C_ACCESS_GROUP_READ;
				if(gw != null && gw.equals("true"))
					flag += C_ACCESS_GROUP_WRITE;
				if(gv != null && gv.equals("true"))
					flag += C_ACCESS_GROUP_VISIBLE;
				if(pr != null && pr.equals("true"))
					flag += C_ACCESS_PUBLIC_READ;
				if(pw != null && pw.equals("true"))
					flag += C_ACCESS_PUBLIC_WRITE;
				if(pv != null && pv.equals("true"))
					flag += C_ACCESS_PUBLIC_VISIBLE;
				if(ir != null && ir.equals("true"))
					flag += C_ACCESS_INTERNAL_READ;
*/

				// modify the access flags
				// boolean rekursive = (file.isFolder() && allflag.equals("true"));
				// cms.chmod(cms.readAbsolutePath(file), flag, rekursive);

				session.removeValue(C_PARA_RESOURCE);

				// return to filelist
				try {
					if(lasturl == null || "".equals(lasturl))
						requestContext.getResponse().sendCmsRedirect(getConfigFile(cms).getWorkplaceActionPath() + CmsWorkplaceAction.getExplorerFileUri(cms));
					else
						requestContext.getResponse().sendRedirect(lasturl);
				}
				catch(Exception e) {
					throw new CmsException("Redirect fails :"
					+ getConfigFile(cms).getWorkplaceActionPath()
					+ CmsWorkplaceAction.getExplorerFileUri(cms), CmsException.C_UNKNOWN_EXCEPTION, e);
				}
				return null;
			}else {

				// the current user is not allowed to change the file owner
				xmlTemplateDocument.setData("details",
				"the current user is not allowed to change the file owner");
				xmlTemplateDocument.setData("lasturl", lasturl);
				template = "error";
				session.removeValue(C_PARA_RESOURCE);
			}
		}

		// set the required datablocks
		String title = cms.readProperty(cms.readAbsolutePath(file), C_PROPERTY_TITLE);
		if(title == null)
			title = "";
		CmsXmlLanguageFile lang = xmlTemplateDocument.getLanguageFile();
//		TODO fix this later
		// CmsUser owner = cms.readOwner(file);
        xmlTemplateDocument.setData("TITLE", Encoder.escapeXml(title));
		xmlTemplateDocument.setData("STATE", getState(cms, file, lang));
		xmlTemplateDocument.setData("OWNER", "" /* Utils.getFullName(owner) */);
		xmlTemplateDocument.setData("GROUP", "" /* cms.readGroup(file).getName() */);
		xmlTemplateDocument.setData("FILENAME", file.getName());

		// now set the actual access flags i the dialog
//		int flags = file.getAccessFlags();
// TODO: replace with new dialog
//		if((flags & C_PERMISSION_READ) > 0)
//			xmlTemplateDocument.setData("CHECKUR", "CHECKED");
//		else
			xmlTemplateDocument.setData("CHECKUR", " ");
//		if((flags & C_PERMISSION_WRITE) > 0)
//			xmlTemplateDocument.setData("CHECKUW", "CHECKED");
//		else
			xmlTemplateDocument.setData("CHECKUW", " ");
//		if((flags & C_PERMISSION_VIEW) > 0)
//			xmlTemplateDocument.setData("CHECKUV", "CHECKED");
//		else
			xmlTemplateDocument.setData("CHECKUV", " ");
//		if((flags & C_ACCESS_GROUP_READ) > 0)
//			xmlTemplateDocument.setData("CHECKGR", "CHECKED");
//		else
			xmlTemplateDocument.setData("CHECKGR", " ");
//		if((flags & C_ACCESS_GROUP_WRITE) > 0)
//			xmlTemplateDocument.setData("CHECKGW", "CHECKED");
//		else
			xmlTemplateDocument.setData("CHECKGW", " ");
//		if((flags & C_ACCESS_GROUP_VISIBLE) > 0)
//			xmlTemplateDocument.setData("CHECKGV", "CHECKED");
//		else
			xmlTemplateDocument.setData("CHECKGV", " ");
//		if((flags & C_ACCESS_PUBLIC_READ) > 0)
//			xmlTemplateDocument.setData("CHECKPR", "CHECKED");
//		else
			xmlTemplateDocument.setData("CHECKPR", " ");
//		if((flags & C_ACCESS_PUBLIC_WRITE) > 0)
//			xmlTemplateDocument.setData("CHECKPW", "CHECKED");
//		else
			xmlTemplateDocument.setData("CHECKPW", " ");
//		if((flags & C_ACCESS_PUBLIC_VISIBLE) > 0)
//			xmlTemplateDocument.setData("CHECKPV", "CHECKED");
//		else
			xmlTemplateDocument.setData("CHECKPV", " ");
//		if((flags & C_ACCESS_INTERNAL_READ) > 0)
//			xmlTemplateDocument.setData("CHECKIF", "CHECKED");
//		else
			xmlTemplateDocument.setData("CHECKIF", " ");

		// process the selected template
		return startProcessing(cms, xmlTemplateDocument, "", parameters, template);
	}

	/**
	 * Gets a formatted file state string.<p>
     * 
	 * @param cms The CmsObject.
	 * @param file The CmsResource.
	 * @param lang The content definition language file.
	 * @return Formatted state string.
	 */
	private String getState(CmsObject cms, CmsResource file, CmsXmlLanguageFile lang)
	throws CmsException {
		//if(file.inProject(cms.getRequestContext().currentProject())) {
        if (cms.isInsideCurrentProject(file)) {
			int state = file.getState();
			return lang.getLanguageValue("explorer.state" + state);
		}
		return lang.getLanguageValue("explorer.statenip");
	}

    /**
     * Indicates if the results of this class are cacheable,
     * which is not the case for this class.<p>
     *
     * @param cms CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return <code>false</code>
     */
    public boolean isCacheable(CmsObject cms, String templateFile, String elementName,
        Hashtable parameters, String templateSelector) {
        return false;
    }
}
