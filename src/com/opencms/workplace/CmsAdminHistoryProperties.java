/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsAdminHistoryProperties.java,v $
* Date   : $Date: 2003/08/14 15:37:24 $
* Version: $Revision: 1.8 $
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

import org.opencms.main.OpenCms;

import com.opencms.core.CmsException;
import com.opencms.core.I_CmsCronJob;
import com.opencms.core.I_CmsSession;
import com.opencms.file.CmsObject;
import com.opencms.file.CmsRequestContext;
import com.opencms.file.I_CmsRegistry;
import com.opencms.template.CmsXmlTemplateFile;

import java.util.Hashtable;

/**
 * Template class for displaying OpenCms workplace administration synchronisation properties.
 *
 * Creation date: ()
 * @author Edna Falkenhan
 */
public class CmsAdminHistoryProperties extends CmsWorkplaceDefault implements I_CmsCronJob {

    private final String C_STEP = "step";

    /**
     * Gets the content of a defined section in a given template file and its subtemplates
     * with the given parameters.
     *
     * @see #getContent(CmsObject, String, String, Hashtable, String)
     * @param cms CmsObject Object for accessing system resources.
     * @param templateFile Filename of the template file.
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     */
    public byte[] getContent(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        if(OpenCms.isLogging(C_OPENCMS_DEBUG) && C_DEBUG) {
            OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "getting content of element " + ((elementName == null) ? "<root>" : elementName));
            OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "template file is: " + templateFile);
            OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "selected template section is: " + ((templateSelector == null) ? "<default>" : templateSelector));
        }

        CmsXmlTemplateFile templateDocument = getOwnTemplateFile(cms, templateFile, elementName, parameters, templateSelector);
        I_CmsRegistry reg = cms.getRegistry();
        CmsRequestContext reqCont = cms.getRequestContext();
        I_CmsSession session = reqCont.getSession(true);

        String weeks = new String();
        String enableHistory = new String();
        String enableDelete = new String();
        // get the properties for history from the registry
        Hashtable histproperties = reg.getSystemValues(C_REGISTRY_HISTORY);

        // clear session values on first load
        String step = (String)parameters.get(C_STEP);
        weeks = (String)parameters.get(C_WEEKS_HISTORY);
        enableHistory = (String)parameters.get(C_ENABLE_HISTORY);
        enableDelete = (String)parameters.get(C_DELETE_HISTORY);
        
        // if there are still values in the session (like after an error), use them
        if((weeks == null) || ("".equals(weeks))) {
            weeks = (String)session.getValue(C_WEEKS_HISTORY);
            if(weeks == null){
                weeks = new String();
            }
        }
        if((enableHistory == null) || ("".equals(enableHistory))) {
            enableHistory = (String)session.getValue(C_ENABLE_HISTORY);
        }
        if((enableDelete == null) || ("".equals(enableDelete))) {
            enableDelete = (String)session.getValue(C_DELETE_HISTORY);
        }
        
        if(step == null) {
            // if the dialog was opened the first time remove the session values
            if(session.getValue(C_STEP) == null){
                // remove all session values
                session.removeValue(C_WEEKS_HISTORY);
                session.removeValue(C_ENABLE_HISTORY);
                session.removeValue(C_DELETE_HISTORY);
                session.removeValue("lasturl");
                session.putValue(C_STEP, "nextstep");
                
                // if the values are still empty try to read them from the registry
                if((weeks == null) || ("".equals(weeks))) {
                    weeks = (String)histproperties.get(C_WEEKS_HISTORY);
                }
                if((enableHistory == null) || ("".equals(enableHistory))) {
                    enableHistory = (String)histproperties.get(C_ENABLE_HISTORY);
                }
                if((enableDelete == null) || ("".equals(enableDelete))) {
                    enableDelete = (String)histproperties.get(C_DELETE_HISTORY);
                }
            }
        } else {
            if("ok".equalsIgnoreCase(step)) {
                // the form has just been submitted
                try{
                    // now update the registry
                    histproperties.put(C_WEEKS_HISTORY, weeks);
                    if("true".equals(enableHistory)){
                        histproperties.put(C_ENABLE_HISTORY, "true");
                    } else {
                        histproperties.put(C_ENABLE_HISTORY, "false");
                    }
                    if("true".equals(enableDelete)){
                        histproperties.put(C_DELETE_HISTORY, "true");
                    } else {
                        histproperties.put(C_DELETE_HISTORY, "false");
                    }
                    reg.setSystemValues(C_REGISTRY_HISTORY, histproperties);
                    templateSelector = "done";
                    // remove the values from the session
                    // remove all session values
                    session.removeValue(C_WEEKS_HISTORY);
                    session.removeValue(C_ENABLE_HISTORY);
                    session.removeValue(C_DELETE_HISTORY);      
                } catch (CmsException e){
                    // there was an exception, store the data in the session
                    session.putValue(C_WEEKS_HISTORY, weeks);
                    if(enableHistory != null){
                        session.putValue(C_ENABLE_HISTORY, enableHistory);
                    }
                    if(enableDelete != null){
                        session.putValue(C_DELETE_HISTORY, enableDelete);
                    }
                    templateSelector = "errorhistproperties";
                    if("errorhistproperties".equals(templateSelector)){
                        // at least one of the choosen folders was not writeable
                        templateDocument.setData("details", "The data could not be stored in the registry:"
                                + e.getLocalizedMessage());
                    }
                }
            } else if("execute".equalsIgnoreCase(step)){
                try{
                    // now update the registry
                    histproperties.put(C_WEEKS_HISTORY, weeks);
                    if("true".equals(enableHistory)){
                        histproperties.put(C_ENABLE_HISTORY, "true");
                    } else {
                        histproperties.put(C_ENABLE_HISTORY, "false");
                    }
                    if("true".equals(enableDelete)){
                        histproperties.put(C_DELETE_HISTORY, "true");
                    } else {
                        histproperties.put(C_DELETE_HISTORY, "false");
                    }
                    reg.setSystemValues(C_REGISTRY_HISTORY, histproperties);                
                    executeDeleting(cms, null);
                } catch (CmsException e){
                    // there was an exception, store the data in the session
                    session.putValue(C_WEEKS_HISTORY, weeks);
                    if(enableHistory != null){
                        session.putValue(C_ENABLE_HISTORY, enableHistory);
                    }
                    if(enableDelete != null){
                        session.putValue(C_DELETE_HISTORY, enableDelete);
                    }
                    templateSelector = "errorhistproperties";
                    if("errorhistproperties".equals(templateSelector)){
                        // at least one of the choosen folders was not writeable
                        templateDocument.setData("details", "The history could not be cleaned:"
                                + e.getLocalizedMessage());
                    }
                }
            } else if("fromerrorpage".equals(step)) {
                templateSelector = "";
            } else if("cancel".equals(step)){
                // remove the values from the session
                // remove all session values
                session.removeValue(C_WEEKS_HISTORY);
                session.removeValue(C_ENABLE_HISTORY);
                session.removeValue(C_DELETE_HISTORY);
                templateSelector = "done";
            }
            session.removeValue(C_STEP);
        }
        
        // set the data in the template
        templateDocument.setData(C_WEEKS_HISTORY, weeks);
        if("true".equalsIgnoreCase(enableHistory)){
            templateDocument.setData(C_ENABLE_HISTORY, "checked");
        } else {
            templateDocument.setData(C_ENABLE_HISTORY, "");
        }
        if("true".equalsIgnoreCase(enableDelete)){
            templateDocument.setData(C_DELETE_HISTORY, "checked");
        } else {
            templateDocument.setData(C_DELETE_HISTORY, "");
        }

        // Now load the template file and start the processing
        return startProcessing(cms, templateDocument, elementName, parameters, templateSelector);
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
    
    /**
     * this method is called by the scheduler
     * @param CmsObject
     * @param parameter
     * @return String - String that is written to the opencms -log
     */
    public String launch(CmsObject cms, String parameter) throws CmsException {
        int version = executeDeleting(cms, parameter);
        return ("CmsAdminHistoryProperties.launch(): History cleaned, oldest version: "+version);
    }
    
    /**
     * this method executes the deleting of versions in the backup tables
     * 
     * @param CmsObject
     * @param parameter
     * @return int - The version-id of the oldest remaining version
     */
    private int executeDeleting(CmsObject cms, String parameter) throws CmsException{
        int versionId = 0;
        // get the number of weeks from the registry
        Hashtable histproperties = cms.getRegistry().getSystemValues(C_REGISTRY_HISTORY);
        String weeks = (String)histproperties.get(C_WEEKS_HISTORY);
        if((weeks != null) && !("".equals(weeks.trim()))){
            versionId = cms.deleteBackups(Integer.parseInt(weeks));
        }
        return versionId;
    } 
}