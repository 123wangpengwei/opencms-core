/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsExplorerResources.java,v $
* Date   : $Date: 2001/09/13 09:05:23 $
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

import com.opencms.file.*;
import com.opencms.core.*;
import com.opencms.util.*;
import com.opencms.template.*;
import java.util.*;
import javax.servlet.http.*;

/**
 * Template class for generating the java script.
 * Creation date: (06.09.00 09:30:25)
 * @author: Hanjo Riege
 */

public class CmsExplorerResources extends CmsWorkplaceDefault implements I_CmsConstants {

    private final static String C_RESTYPES_FOLDER = "/system/workplace/restypes/";

    /**
     * Gets the content of a defined section in a given template file and its subtemplates
     * with the given parameters.
     *
     * @see getContent(CmsObject cms, String templateFile, String elementName, Hashtable parameters)
     * @param cms CmsObject Object for accessing system resources.
     * @param templateFile Filename of the template file.
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     */

    public byte[] getContent(CmsObject cms, String templateFile, String elementName,
            Hashtable parameters, String templateSelector) throws CmsException {
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() && C_DEBUG) {
            A_OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "getting content of element "
                    + ((elementName == null) ? "<root>" : elementName));
            A_OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "template file is: " + templateFile);
            A_OpenCms.log(C_OPENCMS_DEBUG, this.getClassName() + "selected template section is: "
                    + ((templateSelector == null) ? "<default>" : templateSelector));
        }
        CmsXmlWpTemplateFile templateDocument = new CmsXmlWpTemplateFile(cms, templateFile);
        CmsXmlLanguageFile lang = templateDocument.getLanguageFile();
        String servletPath = cms.getRequestContext().getRequest().getServletUrl();
        StringBuffer jsOutput = new StringBuffer();

        // function vars_resources
        jsOutput.append("function vars_resources() { \n this.stati = new Array(\"");
        jsOutput.append(lang.getLanguageValue("explorer.state0") + "\",\"");
        jsOutput.append(lang.getLanguageValue("explorer.state1") + "\",\"");
        jsOutput.append(lang.getLanguageValue("explorer.state2") + "\",\"");
        jsOutput.append(lang.getLanguageValue("explorer.state3") + "\");\n");
        jsOutput.append(" this.descr = new Array(\"");
        jsOutput.append(lang.getLanguageValue("title.name") + "\",\"");
        jsOutput.append(lang.getLanguageValue("title.title") + "\",\"");
        jsOutput.append(lang.getLanguageValue("title.type") + "\",\"");
        jsOutput.append(lang.getLanguageValue("title.changed") + "\",\"");
        jsOutput.append(lang.getLanguageValue("title.size") + "\",\"");
        jsOutput.append(lang.getLanguageValue("title.state") + "\",\"");
        jsOutput.append(lang.getLanguageValue("title.owner") + "\",\"");
        jsOutput.append(lang.getLanguageValue("title.group") + "\",\"");
        jsOutput.append(lang.getLanguageValue("title.access") + "\",\"");
        jsOutput.append(lang.getLanguageValue("title.locked") + "\");\n");
        jsOutput.append(" this.actProject; \n this.onlineProject;\n this.lockedBy=\"" + lang.getLanguageValue("title.locked"));
        jsOutput.append("\";\n this.titleString=\"" + lang.getLanguageValue("label.wptitle") + "\" \n this.actDirectory;");
        jsOutput.append("\n this.userName = \"" + cms.getRequestContext().currentUser().getName() + "\";\n");
        jsOutput.append("\n this.serverName = \"" + ((HttpServletRequest)cms.getRequestContext().getRequest().getOriginalRequest()).getServerName() + "\";\n");
        jsOutput.append(" this.langback=\"" + lang.getLanguageValue("button.back") + "\";\n");
        jsOutput.append(" this.langup=\"" + lang.getLanguageValue("button.parent") + "\";\n");
        jsOutput.append(" this.langnew=\"" + lang.getLanguageValue("button.new") + "\";\n");
        jsOutput.append(" this.langadress=\"" + lang.getLanguageValue("input.adress") + ":\";\n");
        jsOutput.append(" this.langpage=\"" + lang.getLanguageValue("input.page") + "\";\n");
        jsOutput.append(" this.langloading=\"" + lang.getLanguageValue("label.loading") + "\";\n");
        jsOutput.append(" this.altlockedby=\"" + lang.getLanguageValue("explorer.lockedby") + "\";\n");
        jsOutput.append(" this.altlockedin=\"" + lang.getLanguageValue("explorer.lockedin") + "\";\n");
        jsOutput.append(" this.altbelongto=\"" + lang.getLanguageValue("explorer.altbelongto") + "\";\n");
        jsOutput.append(" this.servpath=\"" + servletPath + "\";\n");
        int filelist = getDefaultPreferences(cms);
        jsOutput.append(" this.viewcfg = " + filelist + ";\n");
        jsOutput.append(" this.actDirId;\n} \n");

        // function initialize_resources
        jsOutput.append("function initialize_resources() {\n vi.iconPath=\"" + (String)picsUrl(cms, "", null, null) + "\";\n");

        // get the resources from /system/workplace/restypes/
        Vector resTypes = cms.getFilesInFolder(C_RESTYPES_FOLDER);
        for(int i = 0;i < resTypes.size();i++) {
            CmsFile resourceTyp = (CmsFile)resTypes.elementAt(i);
            try {
                int resId = (cms.getResourceType(resourceTyp.getName())).getResourceType();
                jsOutput.append(getResourceEntry(lang, new String(cms.readFile(resourceTyp.getAbsolutePath()).getContents()), resId));
            }
            catch(CmsException e) {

            }
        }
        jsOutput.append("}");
        templateDocument.setData("js", jsOutput.toString());

        // Now load the template file and start the processing
        return startProcessing(cms, templateDocument, elementName, parameters, templateSelector);
    }

    /**
     * Sets the default preferences for the current user if those values are not available.
     * @return Hashtable with default preferences.
     */

    private int getDefaultPreferences(CmsObject cms) {
        int filelist;
        String explorerSettings = (String)cms.getRequestContext().currentUser().getAdditionalInfo(C_ADDITIONAL_INFO_EXPLORERSETTINGS);
        if(explorerSettings != null) {
            filelist = new Integer(explorerSettings).intValue();
        }
        else {
            filelist = C_FILELIST_NAME + C_FILELIST_TITLE + C_FILELIST_TYPE + C_FILELIST_CHANGED;
        }
        return filelist;
    }

    /**
     * Insert the method's description here.
     * Creation date: (17.11.00 16:05:08)
     * @return java.lang.String
     * @param lang com.opencms.workplace.CmsXmlLanguageFile
     * @param data java.lang.String
     * @param name java.lang.String
     */

    private String getResourceEntry(CmsXmlLanguageFile lang, String data, int id) {
        StringBuffer result = new StringBuffer();
        String resId = Integer.toString(id);

        // first replace "resource_id" with the id
        int index = 0;
        String myToken = "resource_id";
        int foundAt = data.indexOf(myToken, index);
        while(foundAt != -1) {
            result.append(data.substring(index, foundAt) + resId);
            index = foundAt + myToken.length();
            foundAt = data.indexOf(myToken, index);
        }
        result.append(data.substring(index));

        // now set in the language values
        data = result.toString();
        result = new StringBuffer();
        myToken = "language_key";
        index = 0;
        foundAt = data.indexOf(myToken, index);
        while(foundAt != -1) {
            int endIndex = data.indexOf(")", foundAt);
            String langKey = data.substring(data.indexOf("(", foundAt) + 1, endIndex);
            try {
                langKey = lang.getDataValue(langKey);
            }
            catch(CmsException e) {
            }
            result.append(data.substring(index, foundAt) + langKey);
            index = endIndex + 1;
            foundAt = data.indexOf(myToken, index);
        }
        result.append(data.substring(index));

        // at last we have to remove the spaces in the rules parameter
        data = result.toString();
        result = new StringBuffer();
        myToken = "rules_key";
        index = 0;
        foundAt = data.indexOf(myToken, index);
        while(foundAt != -1) {
            int endIndex = data.indexOf(")", foundAt);
            String rulesKey = data.substring(data.indexOf("(", foundAt) + 1, endIndex).trim();
            int nextSpace = rulesKey.indexOf(" ");
            while(nextSpace > -1){
                rulesKey = rulesKey.substring(0, nextSpace)+rulesKey.substring(nextSpace+1);
                nextSpace = rulesKey.indexOf(" ");
            }
            result.append(data.substring(index, foundAt) + rulesKey);
            index = endIndex + 1;
            foundAt = data.indexOf(myToken, index);
        }
        result.append(data.substring(index));

        return result.toString();
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

    public boolean isCacheable(CmsObject cms, String templateFile, String elementName,
            Hashtable parameters, String templateSelector) {
        return false;
    }
}
