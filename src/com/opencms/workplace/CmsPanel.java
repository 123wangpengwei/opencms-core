/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsPanel.java,v $
 * Date   : $Date: 2000/03/09 16:46:17 $
 * Version: $Revision: 1.1 $
 *
 * Copyright (C) 2000  The OpenCms Group 
 * 
 * This File is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * For further information about OpenCms, please see the
 * OpenCms Website: http://www.opencms.com
 * 
 * You should have received a copy of the GNU General Public License
 * long with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.opencms.workplace;

import org.w3c.dom.*;
import org.xml.sax.*;

import com.opencms.core.*;
import com.opencms.template.*;
import com.opencms.file.*;

import java.util.*;
import java.lang.reflect.*;

import javax.servlet.http.*;

/**
 * Class for building workplace panel bars. <BR>
 * Called by CmsXmlTemplateFile for handling the special XML tag <code>&lt;PANELBAR&gt;</code>.
 * 
 * @author Alexander Lucas
 * @version $Revision: 1.1 $ $Date: 2000/03/09 16:46:17 $
 */
public class CmsPanel extends A_CmsWpElement implements I_CmsWpElement, I_CmsWpConstants  {    
    
    /** Workplace tag used for each panel in <code>&lt;PANELBAR&gt;</code> */
    public static final String C_WPTAG_PANEL = "panel";

    /** Attribute used for panelnames <code>C_WPTAG_PANEL</code> */
    public static final String C_WPTAG_ATTR_PANELNAME = "name";
    
    
    /**
     * Handling of the <CODE>&lt;PANELBAR&gt;</CODE> tags.
     * <P>
     * Reads the code of a panel from the panel definition file
     * and returns the processed code with the actual elements.
     * <P>
     * Panel bars can be referenced in any workplace template by <br>
     * // TODO: insert correct syntax here!
     * <code>&lt;PANELBAR&gt;<br>
     * &lt;PANEL&gt; name="..."/&gt;<br>
     * &lt;PANEL&gt; name="..."/&gt;<br> 
     * ...
     * </code>
     * For each <code>&lt;PANEL&gt;</code> element one panel will be created.
     * The text for this panel will be taken from the language file's
     * <code>&lt;PANEL&gt;</code> section using the panel name.
     * <p>
     * Each panel will be linked to the currently requested URL,
     * extended by the <code>?panel=name</code> parameter.
     * 
     * @param cms A_CmsObject Object for accessing resources.
     * @param n XML element containing the <code>&lt;INPUT&gt;</code> tag.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document.  
     * @param callingObject reference to the calling object.
     * @param parameters Hashtable containing all user parameters.
     * @param lang CmsXmlLanguageFile conataining the currently valid language file.
     * @return Processed button.
     * @exception CmsException
     */    
    public Object handleSpecialWorkplaceTag(A_CmsObject cms, Element n, A_CmsXmlContent doc, Object callingObject, Hashtable parameters, CmsXmlLanguageFile lang) throws CmsException {
        
        // Currently requested panel
        String selectedPanel = (String)parameters.get(C_PARA_PANEL);
        
        // panel definition file
        CmsXmlWpTemplateFile paneldef = getPanelDefinitions(cms);

        // base URL of the page, each panel is linked to.
        // This URL will be extendeb by "?panel=panelname" or "&panel=panelname"
        A_CmsRequestContext reqCont = cms.getRequestContext();
        
        String url = ((HttpServletRequest)reqCont.getRequest().getOriginalRequest()).getServletPath()
                + reqCont.getUri();
        if(url.indexOf("?") >= 0) {
            url = url + "&panel=";
        } else {
            url = url + "?panel=";
        }
        
        // Node reference used in loops
        Node nodeLoop;
        
        // Name of the currently processed panel
        String panelName = null;

        // Some StringBuffer for storing the results
        StringBuffer resultBg = new StringBuffer();
        StringBuffer resultTxt = new StringBuffer();
        StringBuffer resultAll = new StringBuffer();
        
        // Collect all available panels and put them into 
        // the Vector panels
        NodeList nl = n.getChildNodes();
        Vector panels = new Vector();
        for(int i=0; i<nl.getLength(); i++) {
            Element tempElem = null;
            nodeLoop = nl.item(i);
            if(nodeLoop.getNodeType() == nodeLoop.ELEMENT_NODE) {
                tempElem = (Element)nodeLoop;
                if(tempElem.getNodeName().toLowerCase().equals(C_WPTAG_PANEL)) {
                    panelName = tempElem.getAttribute(C_WPTAG_ATTR_PANELNAME);
                    if(panelName != null && !panelName.equals("")) {
                        panels.addElement(panelName);
                    }
                }
            }
        }
        
        // Get the index of the currently requested panel
        int currentPanelNo = panels.indexOf(selectedPanel);

        // Generate the output for each panel.
        // Panel background and text will be written separately        
        for(int i=0; i<panels.size(); i++) {
            panelName = (String)panels.elementAt(i);
            paneldef.setXmlData(C_PANEL_LINK, url + panelName);
            paneldef.setXmlData(C_PANEL_NAME, lang.getLanguageValue("panel." + panelName));
            if(i==currentPanelNo) {
                resultBg.append(paneldef.getXmlDataValue(C_TAG_PANEL_BGACTIVE));
                resultTxt.append(paneldef.getProcessedXmlDataValue(C_TAG_PANEL_TEXTACTIVE));
            } else {
                resultBg.append(paneldef.getXmlDataValue(C_TAG_PANEL_BGINACTIVE));
                resultTxt.append(paneldef.getProcessedXmlDataValue(C_TAG_PANEL_TEXTINACTIVE));
            }
        }
                
        // Now build the end result        
        resultAll.append(paneldef.getXmlDataValue(C_TAG_PANEL_STARTSEQ));
        resultAll.append(resultBg.toString());
        resultAll.append(paneldef.getXmlDataValue(C_TAG_PANEL_SEPBGTEXT));
        resultAll.append(resultTxt.toString());
        resultAll.append(paneldef.getXmlDataValue(C_TAG_PANEL_ENDSEQ));
                 
        return resultAll.toString(); 
    }                    
}
