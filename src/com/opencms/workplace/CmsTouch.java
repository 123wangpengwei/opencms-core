/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsTouch.java,v $
 * Date   : $Date: 2003/07/12 12:49:03 $
 * Version: $Revision: 1.9 $
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

import org.opencms.workplace.CmsWorkplaceAction;

import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.core.I_CmsSession;
import com.opencms.file.CmsObject;
import com.opencms.file.CmsResource;
import com.opencms.util.Encoder;

import java.util.Hashtable;

/**
 * This class is invoked for the workplace "touch" function in the context menu.
 * 
 * @author Thomas Weckert (t.weckert@alkacon.com)
 * @version $Revision: 1.9 $
 */
public final class CmsTouch extends CmsWorkplaceDefault implements I_CmsWpConstants, I_CmsConstants {

	/** The key of the timestamp request parameter */
	private static final String C_PARA_NEWTIMESTAMP = "newtimestamp";

	/** Internal debugging flag */
	private static final int DEBUG = 0;

	private String m_ResourceName;
	private CmsResource m_Resource;
	private boolean m_TouchRecursive;
	private long m_NewTimestamp;
	private CmsXmlWpTemplateFile m_XmlTemplateDocument;
	private String m_TemplateSection;

	public byte[] getContent(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
		// read the submitted input parameters
		this.readInput(cms, parameters);

		if (m_NewTimestamp != -1) {
			// this, is a re-submission, process the input
			cms.touch(m_ResourceName, m_NewTimestamp, m_TouchRecursive);

			// leave the session clean
			this.clearSessionValues(cms.getRequestContext().getSession(true));

			// send the user back to the file listing after the work is done
			try {
				cms.getRequestContext().getResponse().sendCmsRedirect(getConfigFile(cms).getWorkplaceActionPath() + CmsWorkplaceAction.getExplorerFileUri(cms));
			}
			catch (Exception e) {
				throw new CmsException("Redirect failed :" + getConfigFile(cms).getWorkplaceActionPath() + CmsWorkplaceAction.getExplorerFileUri(cms), CmsException.C_UNKNOWN_EXCEPTION, e);
			}
			return null;
		}

		// initialize the XML template
		this.initTemplate(cms, parameters, templateFile);

		// pump the template with the input form out on the first submission
		return startProcessing(cms, m_XmlTemplateDocument, "", parameters, m_TemplateSection);
	}

	private void readInput(CmsObject cms, Hashtable theParameters) throws CmsException {
		I_CmsSession session = cms.getRequestContext().getSession(true);

		// reset all values first
		m_ResourceName = null;
		m_Resource = null;
		m_TouchRecursive = false;
		m_NewTimestamp = -1;

		// clear session values on the first load
		if (theParameters.get(I_CmsWpConstants.C_PARA_INITIAL) != null) {
			this.clearSessionValues(session);
		}

		// read the timestamp               
		try {
			m_NewTimestamp = Long.parseLong((String) theParameters.get(CmsTouch.C_PARA_NEWTIMESTAMP));
		}
		catch (Exception e) {
			m_NewTimestamp = -1;
		}

		// read the name of the resource
		m_ResourceName = (String) theParameters.get(I_CmsWpConstants.C_PARA_FILE);
		if (m_ResourceName != null) {
			// the form was submitted the first time, put the name of the 
			// resource (which isnt changed anymore) into the session
			session.putValue(I_CmsWpConstants.C_PARA_FILE, m_ResourceName);
		}
		else {
			// this is a re-submission, get the resource name from the session instead
			m_ResourceName = (String) session.getValue(I_CmsWpConstants.C_PARA_FILE);
		}

		// create a valid Cms resource for the given resource name
		m_Resource = (CmsResource) cms.readFileHeader(m_ResourceName);

		// check whether the all sub resources should be touched recusively
		String dummy = (String) theParameters.get(I_CmsWpConstants.C_PARA_FLAGS);
		if (dummy != null) {
			m_TouchRecursive = dummy.trim().equalsIgnoreCase("true");
		}

		if (DEBUG > 0) {
			System.out.println("[" + this.getClass().getName() + "] resource: " + m_ResourceName);
			System.out.println("[" + this.getClass().getName() + "] timestamp: " + m_NewTimestamp);
			System.out.println("[" + this.getClass().getName() + "] recursive: " + m_TouchRecursive);
		}
	}

	private void initTemplate(CmsObject cms, Hashtable theParameters, String theTemplateFile) throws CmsException {
		// reset all values first
		m_XmlTemplateDocument = null;
		m_TemplateSection = null;

		// set the template section of the output
		if (m_Resource.isFile()) {
			m_TemplateSection = "file";
		}
		else {
			m_TemplateSection = "folder";
		}

		// read the title of the resource
		String resourceTitle = cms.readProperty(cms.readAbsolutePath(m_Resource), I_CmsConstants.C_PROPERTY_TITLE);
		if (resourceTitle == null) {
			resourceTitle = "";
		}

		// create the XML output template file
		m_XmlTemplateDocument = new CmsXmlWpTemplateFile(cms, theTemplateFile);
		CmsXmlLanguageFile lang = m_XmlTemplateDocument.getLanguageFile();

		// read the current state of the resource
		String currentState = "";
		if (m_Resource.inProject(cms.getRequestContext().currentProject())) {
			int state = m_Resource.getState();
			currentState += lang.getLanguageValue("explorer.state" + state);
		}
		else {
			currentState += lang.getLanguageValue("explorer.statenip");
		}

		// prepare the XML template data    
		m_XmlTemplateDocument.setData("TITLE", Encoder.escapeXml(resourceTitle));
		m_XmlTemplateDocument.setData("STATE", currentState);
		m_XmlTemplateDocument.setData("FILENAME", m_Resource.getResourceName());
	}

	/**
	 * Removes the values cached in the session.
	 */
	private void clearSessionValues(I_CmsSession theSession) {
		// remove all session values
		theSession.removeValue(I_CmsWpConstants.C_PARA_FILE);
	}

	/**
	 * The element cache, programmer's best friend.
	 */
	public boolean isCacheable(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
		return false;
	}

}