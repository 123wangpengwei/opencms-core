/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/defaults/Attic/A_CmsBackoffice.java,v $
* Date   : $Date: 2001/10/19 15:03:09 $
* Version: $Revision: 1.18 $
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

package com.opencms.defaults;

import com.opencms.template.*;
import com.opencms.workplace.*;
import com.opencms.defaults.*;
import com.opencms.file.*;
import com.opencms.core.*;
import com.opencms.core.exceptions.*;
import java.util.*;
import java.lang.reflect.*;


/**
 * Abstract class for generic backoffice display. It automatically
 * generates the <ul><li>head section with filters and buttons,</li>
 *                  <li>body section with the table data,</li>
 *                  <li>lock states of the entries. if there are any,</li>
 *                  <li>delete dialog,</li>
 *                  <li>lock dialog.</li></ul>
 * calls the <ul><li>edit dialog of the calling backoffice class</li>
 *              <li>new dialog of the calling backoffice class</li></ul>
 * using the content definition class defined by the getContentDefinition method.
 * The methods and data provided by the content definition class
 * is accessed by reflection. This way it is possible to re-use
 * this class for any content definition class, that just has
 * to extend the A_CmsContentDefinition class!
 * Creation date: (27.10.00 10:04:42)
 * author: Michael Knoll
 * author: Michael Emmerich
 * version 2.0
 */
public abstract class A_CmsBackoffice extends CmsWorkplaceDefault implements I_CmsConstants{

  private static int C_NOT_LOCKED = -1;
  private static int C_NO_ACCESS = -2;

  private static String C_DEFAULT_SELECTOR="(default)";
  private static String C_DONE_SELECTOR="done";

    /** The style for unchanged files or folders */
    private final static String C_STYLE_UNCHANGED = "dateingeandert";


    /** The style for files or folders not in project*/
    private final static String C_STYLE_NOTINPROJECT = "dateintprojekt";


    /** The style for new files or folders */
    private final static String C_STYLE_NEW = "dateineu";


    /** The style for deleted files or folders */
    private final static String C_STYLE_DELETED = "dateigeloescht";


    /** The style for changed files or folders */
    private final static String C_STYLE_CHANGED = "dateigeaendert";

  /**
  * Gets the backoffice url of the module.
  * @returns A string with the backoffice url
  */
  abstract public String getBackofficeUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc,
                                 Object userObject) throws Exception ;
  /**
  * Gets the create url of the module.
  * @returns a string with the create url
  */
  abstract public String getCreateUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc,
                             Object userObject) throws Exception ;

  /**
  * Gets the edit url of the module.
  * @returns A string with the edit url
  */
  abstract public String getEditUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc,
                           Object userObject) throws Exception ;

  /**
  * Gets the edit url of the module.
  * @returns A string with the edit url
  */
  public String getDeleteUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc,
                             Object userObject) throws Exception {

    return getBackofficeUrl(cms, tagcontent,doc,userObject);
  }


  /**
  * Gets the redirect url of the module. This URL is called, when an entry of the file list is selected
  * @returns A string with the  url.
  */
  public String getUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws Exception {
    return "";
  }

  /**
  * Gets the setup url of the module. This is the url of the setup page for this module.
  * @returns A string with the  setup url.
  */
  public String getSetupUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws Exception {
    return "";
  }

/**
 * Gets the content of a given template file.
 * This method displays any content provided by a content definition
 * class on the template. The used backoffice class does not need to use a
 * special getContent method. It just has to extend the methods of this class!
 * Using reflection, this method creates the table headline and table content
 * with the layout provided by the template automatically!
 * @param cms A_CmsObject Object for accessing system resources
 * @param templateFile Filename of the template file
 * @param elementName <em>not used here</em>.
 * @param parameters <em>not used here</em>.
 * @param templateSelector template section that should be processed.
 * @return Processed content of the given template file.
 * @exception CmsException
 */

public byte[] getContent(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {

    //return var
    byte[] returnProcess = null;

        String error="";

        // the CD to be used
         A_CmsContentDefinition cd=null;

    // session will be created or fetched
    I_CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
    //create new workplace templatefile object
    CmsXmlWpTemplateFile template = new CmsXmlWpTemplateFile(cms, templateFile);
    //get parameters
    String selectBox = (String) parameters.get("selectbox");
    String filterParam = (String) parameters.get("filterparameter");
    String id = (String) parameters.get("id");
    String idlock = (String) parameters.get("idlock");
    String iddelete = (String) parameters.get("iddelete");
    String idedit = (String) parameters.get("idedit");
        String idview = (String) parameters.get("idview");
    String action = (String) parameters.get("action");
        String parentId = (String) parameters.get("parentId");
    String ok = (String) parameters.get("ok");
    String setaction = (String) parameters.get("setaction");

        // debug-code
/*
        System.err.println("### "+this.getContentDefinitionClass().getName());
        System.err.println("### PARAMETERS");
        Enumeration  enu=parameters.keys();
        while (enu.hasMoreElements()) {
          String a=(String)enu.nextElement();
          String b=parameters.get(a).toString();
          System.err.println("## "+a+" -> "+b);
        }

        System.err.println("");
        System.err.println("+++ SESSION");
        String[] ses=session.getValueNames();
        for (int i=0;i<ses.length;i++) {
          String a=ses[i];
          String b=session.getValue(a).toString();
          System.err.println("++ "+a+" -> "+b);
        }
        System.err.println("");
        System.err.println("-------------------------------------------------");
*/

	String hasFilterParam = (String) session.getValue("filterparameter");
	template.setData("filternumber","0");

	//change filter
	if ((hasFilterParam == null) && (filterParam == null) && (setaction == null)) {
	  if (selectBox != null) {
	    session.putValue("filter", selectBox);
	    template.setData("filternumber",selectBox);
	  }
	} else {
	  template.setData("filternumber", (String)session.getValue("filter"));
	}

	//move id values to id, remove old markers
	if (idlock != null) {
	  id = idlock;
	  session.putValue("idlock", idlock);
	  session.removeValue("idedit");
	  session.removeValue("idnew");
	  session.removeValue("iddelete");
	}
	if (idedit != null) {
	  id = idedit;
	  session.putValue("idedit", idedit);
	  session.removeValue("idlock");
	  session.removeValue("idnew");
	  session.removeValue("iddelete");
	}
	if (iddelete != null) {
	  id = iddelete;
	  session.putValue("iddelete", iddelete);
	  session.removeValue("idedit");
	  session.removeValue("idnew");
	  session.removeValue("idlock");
	}
	if ((id != null) && (id.equals("new"))) {
	  session.putValue("idnew", id);
	  session.removeValue("idedit");
	  session.removeValue("idnew");
	  session.removeValue("iddelete");
	  session.removeValue("idlock");
	}

	//get marker id from session
	String idsave = (String) session.getValue("idsave");
	if (ok == null) {

          idsave = null;
        }

        if(parentId != null) {
          session.putValue("parentId", parentId);
        }

    //get marker for accessing the new dialog
    String idnewsave = (String) session.getValue("idnew");


        // --- This is the part when getContentNew is called ---
    //access to new dialog
    if ((id != null) && (id.equals("new")) || ((idsave != null) && (idsave.equals("new")))) {
          if (idsave != null) {
        parameters.put("id", idsave);
          }
      if (id != null) {
        parameters.put("id", id);
            session.putValue("idsave", id);
          }
          return getContentNewInternal(cms,id,cd,session,template,elementName,parameters,templateSelector);

        // --- This was the part when getContentNew is called ---
    }

    //go to the appropriate getContent methods
    if ((id == null) && (idsave == null) && (action == null) && (idlock==null) && (iddelete == null) && (idedit == null))  {
      //process the head frame containing the filter
      returnProcess = getContentHead(cms, template, elementName, parameters, templateSelector);
      //finally return processed data
      return returnProcess;
    } else {
      //process the body frame containing the table
          if(action == null) {
            action = "";
          }
          if (action.equalsIgnoreCase("list")){
            //process the list output
            // clear "idsave" here in case user verification of data failed and input has to be shown again ...
            session.removeValue("idsave");
            if(isExtendedList()){
                returnProcess = getContentExtendedList(cms, template, elementName, parameters, templateSelector);
            } else {
                returnProcess = getContentList(cms, template, elementName, parameters, templateSelector);
            }
            //finally return processed data
            return returnProcess;
      } else {

           // --- This is the part where getContentEdit is called ---

        //get marker for accessing the edit dialog
            String ideditsave = (String) session.getValue("idedit");
            //go to the edit dialog
        if ((idedit != null) || (ideditsave != null)) {
              if (idsave != null) {
            parameters.put("id", idsave);
              }
          if (id != null) {
        parameters.put("id", id);
                session.putValue("idsave", id);
              }
              return getContentEditInternal(cms,id,cd,session,template,elementName,parameters,templateSelector);

           // --- This was the part where getContentEdit is called ---

      } else {
        //store id parameters for delete and lock
            if (idsave != null) {
          parameters.put("id", idsave);
              session.removeValue("idsave");
            } else {
              parameters.put("id", id);
              session.putValue("idsave", id);
        }
            //get marker for accessing the delete dialog
        String iddeletesave = (String) session.getValue("iddelete");
        //access delete dialog
        if (((iddelete != null) || (iddeletesave != null)) && (idlock == null)) {
          returnProcess = getContentDelete(cms, template, elementName, parameters, templateSelector);
              return returnProcess;
            }else {
          //access lock dialog
          returnProcess = getContentLock(cms, template, elementName, parameters, templateSelector);
          //finally return processed data
          return returnProcess;
        }
          }
    }
      }
}

  /**
  * Gets the content definition class
  * @returns class content definition class
  * Must be implemented in the extending backoffice class!
  */
  public abstract Class getContentDefinitionClass() ;

  /**
  * Gets the content definition class method constructor
  * @returns content definition object
  */
  protected Object getContentDefinition(CmsObject cms, Class cdClass, Integer id) {
    Object o = null;
      try {
        Constructor c = cdClass.getConstructor(new Class[] {CmsObject.class, Integer.class});
        o = c.newInstance(new Object[] {cms, id});
      } catch (InvocationTargetException ite) {
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
      A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: Invocation target exception!");
        }
      } catch (NoSuchMethodException nsm) {
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
          A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: Requested method was not found!");
    }
      } catch (InstantiationException ie) {
    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
      A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: the reflected class is abstract!");
        }
      } catch (Exception e) {
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
      A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: Other exception! "+e);
    }
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
         A_OpenCms.log(C_OPENCMS_INFO, e.getMessage() );
        }
      }
    return o;
  }

  /**
  * Gets the content definition class method constructor
  * @returns content definition object
  */
  protected Object getContentDefinition(CmsObject cms, Class cdClass) {
    Object o = null;
      try {
        Constructor c = cdClass.getConstructor(new Class[] {CmsObject.class});
    o = c.newInstance(new Object[] {cms});
      } catch (InvocationTargetException ite) {
    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
      A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: Invocation target exception!");
    }
      } catch (NoSuchMethodException nsm) {
    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
      A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: Requested method was not found!");
    }
      } catch (InstantiationException ie) {
    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
      A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: the reflected class is abstract!");
    }
      } catch (Exception e) {
    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
      A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: Other exception! "+e);
    }
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(C_OPENCMS_INFO, e.getMessage() );
        }
      }
    return o;
  }

  /**
  * Gets the content definition class method constructor
  * @returns content definition object
  */
  protected Object getContentDefinition(CmsObject cms, Class cdClass, String id) {
    Object o = null;
    try {
      Constructor c = cdClass.getConstructor(new Class[] {CmsObject.class, String.class});
      o = c.newInstance(new Object[] {cms, id});
    } catch (InvocationTargetException ite) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: Invocation target exception!");
      }
    } catch (NoSuchMethodException nsm) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
    A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: Requested method was not found!");
      }
    } catch (InstantiationException ie) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
    A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: the reflected class is abstract!");
      }
    } catch (Exception e) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: Other exception! "+e);
      }
    if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
      A_OpenCms.log(C_OPENCMS_INFO, e.getMessage() );
    }
  }
  return o;
  }

  /**
  * Gets the content of a given template file.
  * <P>
  * While processing the template file the table entry
  * <code>entryTitle<code> will be displayed in the delete dialog
  *
  * @param cms A_CmsObject Object for accessing system resources
  * @param templateFile Filename of the template file
  * @param elementName not used here
  * @param parameters get the parameters action for the button activity
  * and id for the used content definition instance object
  * @param templateSelector template section that should be processed.
  * @return Processed content of the given template file.
  * @exception CmsException
  */

  public byte[] getContentDelete(CmsObject cms, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {

    //return var
    byte[] processResult = null;

    // session will be created or fetched
    I_CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
    //get the class of the content definition
    Class cdClass = getContentDefinitionClass();

    //get (stored) id parameter
    String id = (String) parameters.get("id");
    if (id == null) {
      id = "";
    }

    // get value of hidden input field action
    String action = (String) parameters.get("action");

    //no button pressed, go to the default section!
    //delete dialog, displays the title of the entry to be deleted
    if (action == null || action.equals("")) {
      if (id != "") {
        //set template section
    templateSelector = "delete";

    //create appropriate class name with underscores for labels
/*  String moduleName = "";
    moduleName = (String) getClass().toString(); //get name
    moduleName = moduleName.substring(5); //remove 'class' substring at the beginning
    moduleName = moduleName.trim();
    moduleName = moduleName.replace('.', '_'); //replace dots with underscores
*/
        //create new language file object
    CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);

    //get the dialog from the langauge file and set it in the template
    template.setData("deletetitle", lang.getLanguageValue("messagebox.title.delete"));
    template.setData("deletedialog", lang.getLanguageValue("messagebox.message1.delete"));
    template.setData("newsentry", id);
    template.setData("setaction", "default");
      }
      // confirmation button pressed, process data!
    } else {
      //set template section
      templateSelector = "done";
      //remove marker
      session.removeValue("idsave");
      //delete the content definition instance
      Integer idInteger = null;
      try {
        idInteger = Integer.valueOf(id);
      } catch (Exception e) {
    //access content definition constructor by reflection
    Object o = null;
    o = getContentDefinition(cms, cdClass, id);
    //get delete method and delete content definition instance
    try {
         ((A_CmsContentDefinition) o).delete(cms);
    } catch (Exception e1) {
          if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
      A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice: delete method throwed an exception!");
    }
      templateSelector = "deleteerror";
      template.setData("deleteerror", e1.getMessage());
    }
    //finally start the processing
    processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
    return processResult;
      }

      //access content definition constructor by reflection
      Object o = null;
      o = getContentDefinition(cms, cdClass, idInteger);
      //get delete method and delete content definition instance
      try {
        ((A_CmsContentDefinition) o).delete(cms);
      }catch (Exception e) {
        templateSelector = "deleteerror";
        template.setData("deleteerror", e.getMessage());
      }
    }

    //finally start the processing
    processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
    return processResult;
  }


  /**
  * Gets the content of a given template file.
  * <P>
  *
  * @param cms A_CmsObject Object for accessing system resources
  * @param templateFile Filename of the template file
  * @param elementName not used here
  * @param parameters get the parameters action for the button activity
  * and id for the used content definition instance object
  * and the author, title, text content for setting the new/changed data
  * @param templateSelector template section that should be processed.
  * @return Processed content of the given template file.
  * @exception CmsException
  */

  private byte[] getContentHead(CmsObject cms, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {

    //return var
    byte[] processResult = null;
    //get the class of the content definition
    Class cdClass = getContentDefinitionClass();

    //init vars
    String singleSelection = "";
    String allSelections = "";

    //create new or fetch existing session
    CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
    String uri = cms.getRequestContext().getUri();
    String sessionSelectBoxValue = uri+"selectBoxValue";
    //get filter method from session
    String selectBoxValue = (String) parameters.get("selectbox");
    if(selectBoxValue == null) {
      // set default value
      if((String)session.getValue(sessionSelectBoxValue) != null) {
        // came back from edit or something ... redisplay last filter
          selectBoxValue = (String)session.getValue(sessionSelectBoxValue);
      } else {
        // the very first time here...
         selectBoxValue = "0";
      }
    }
    boolean filterChanged = true;
    if( selectBoxValue.equals((String)session.getValue(sessionSelectBoxValue)) ) {
        filterChanged = false;
    }else {
        filterChanged = true;
    }

    //get vector of filter names from the content definition
    Vector filterMethods = getFilterMethods(cms);

    if( Integer.parseInt(selectBoxValue) >=  filterMethods.size() ) {
        // the stored seclectBoxValue is does not exist any more, ...
        selectBoxValue = "0";
    }

    session.putValue(sessionSelectBoxValue, selectBoxValue); // store in session for Selectbox!
    session.putValue("filter",selectBoxValue);  // store filter in session for getContentList!

    String filterParam = (String) parameters.get("filterparameter");
    String action = (String) parameters.get("action");
    String setaction = (String) parameters.get("setaction");
    // create the key for the filterparameter in the session ... should be unique to avoid problems...
    String sessionFilterParam = uri+selectBoxValue+"filterparameter";
    //store filterparameter in the session, new enty for every filter of every url ...
    if (filterParam != null) {
      session.putValue(sessionFilterParam, filterParam);
    }

    //create appropriate class name with underscores for labels
    String moduleName = "";
    moduleName = (String) getClass().toString(); //get name
    moduleName = moduleName.substring(5); //remove 'class' substring at the beginning
    moduleName = moduleName.trim();
    moduleName = moduleName.replace('.', '_'); //replace dots with underscores

    //create new language file object
    CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);
    //set labels in the template
    template.setData("filterlabel", lang.getLanguageValue(moduleName + ".label.filter"));
    template.setData("filterparameterlabel", lang.getLanguageValue(moduleName + ".label.filterparameter"));

    //no filter selected so far, store a default filter in the session
    CmsFilterMethod filterMethod = null;
    if (selectBoxValue == null) {
      CmsFilterMethod defaultFilter = (CmsFilterMethod) filterMethods.firstElement();
      session.putValue("selectbox", defaultFilter.getFilterName());
    }

    // show param box ?
    CmsFilterMethod currentFilter = (CmsFilterMethod) filterMethods.elementAt(Integer.parseInt(selectBoxValue));
    if(currentFilter.hasUserParameter()) {
        if(filterChanged) {
            template.setData("filterparameter", currentFilter.getDefaultFilterParam());
            // access default in getContentList() ....
            session.putValue(sessionFilterParam, currentFilter.getDefaultFilterParam());
        } else if(filterParam!= null) {
            template.setData("filterparameter", filterParam);
        } else {
            // redisplay after edit or something like this ...
            template.setData("filterparameter", (String)session.getValue(sessionFilterParam));
        }
        // check if there is only one filtermethod, do not show the selectbox then
        if (filterMethods.size()<2) {
          // replace the selectbox with a simple text output
          CmsFilterMethod defaultFilter = (CmsFilterMethod) filterMethods.firstElement();
          template.setData("filtername",defaultFilter.getFilterName());
          template.setData("insertFilter", template.getProcessedDataValue("noSelectboxWithParam", this, parameters));
        } else {
          template.setData("insertFilter", template.getProcessedDataValue("selectboxWithParam", this, parameters));
          }
        template.setData("setfocus", template.getDataValue("focus"));
    }else{
        // check if there is only one filtermethod, do not show the selectbox then
        if (filterMethods.size()<2) {
          // replace the selectbox with a simple text output
          CmsFilterMethod defaultFilter = (CmsFilterMethod) filterMethods.firstElement();
          template.setData("filtername",defaultFilter.getFilterName());
          template.setData("insertFilter", template.getProcessedDataValue("noSelectbox", this, parameters));
        } else {
          template.setData("insertFilter", template.getProcessedDataValue("singleSelectbox", this, parameters));
        }
    }

    //if getCreateUrl equals null, the "create new entry" button
    //will not be displayed in the template
    String createButton = null;
    try {
      createButton = (String) getCreateUrl(cms, null, null, null);
    } catch (Exception e) {
    }
    if (createButton == null) {
      String cb = template.getDataValue("nowand");
      template.setData("createbutton", cb);
    } else {
      String cb = template.getProcessedDataValue("wand", this, parameters);
      template.setData("createbutton", cb);
    }

    //if getSetupUrl is empty, the module setup button will not be displayed in the template.
    String setupButton = null;
    try {
      setupButton = (String) getSetupUrl(cms, null, null, null);
    } catch (Exception e) {
    }
    if ((setupButton == null) || (setupButton.equals(""))){
      String sb = template.getDataValue("nosetup");
      template.setData("setupbutton", sb);
    } else {
      String sb= template.getProcessedDataValue("setup", this, parameters);
      template.setData("setupbutton",sb);
    }

    //finally start the processing
    processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
    return processResult;
  }
  /**
   * Gets the content of a given template file.
   * This method displays any content provided by a content definition
   * class on the template. The used backoffice class does not need to use a
   * special getContent method. It just has to extend the methods of this class!
   * Using reflection, this method creates the table headline and table content
   * with the layout provided by the template automatically!
   * @param cms CmsObjectfor accessing system resources
   * @param templateFile Filename of the template file
   * @param elementName <em>not used here</em>.
   * @param parameters <em>not used here</em>.
   * @param templateSelector template section that should be processed.
   * @return Processed content of the given template file.
   * @exception CmsException
   */
  private byte[] getContentList(CmsObject cms, CmsXmlWpTemplateFile template, String elementName,
                                Hashtable parameters, String templateSelector) throws CmsException {
    //return var
    byte[] processResult = null;
    // session will be created or fetched
    I_CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
    //get the class of the content definition
    Class cdClass = getContentDefinitionClass();

    String action = (String) parameters.get("action");

    //read value of the selected filter
    String filterMethodName = (String) session.getValue("filter");
    if (filterMethodName == null) {
      filterMethodName = "0";
    }

    String uri = cms.getRequestContext().getUri();
    String sessionFilterParam = uri+filterMethodName+"filterparameter";
    //read value of the inputfield filterparameter
    String filterParam = (String) session.getValue(sessionFilterParam);
    if (filterParam == "") {
      filterParam = null;
    }

    //change template to list section for data list output
    templateSelector = "list";

    //init vars
    String tableHead = "";
    String singleRow = "";
    String allEntrys = "";
    String entry = "";
    String url = "";
    int columns = 0;

    // get number of columns
    Vector columnsVector = new Vector();
    String fieldNamesMethod = "getFieldNames";
    Class paramClasses[] = {CmsObject.class};
    Object params[] = {cms};
    columnsVector = (Vector) getContentMethodObject(cms, cdClass, fieldNamesMethod, paramClasses, params);
    columns = columnsVector.size();

    //create appropriate class name with underscores for labels
    String moduleName = "";
    moduleName = (String) getClass().toString(); //get name
    moduleName = moduleName.substring(5); //remove 'class' substring at the beginning
    moduleName = moduleName.trim();
    moduleName = moduleName.replace('.', '_'); //replace dots with underscores

    //create new language file object
    CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);

    //create tableheadline
    for (int i = 0; i < columns; i++) {
      tableHead += (template.getDataValue("tabledatabegin"))
        + lang.getLanguageValue(moduleName + ".label." + columnsVector.elementAt(i).toString().toLowerCase().trim())
        + (template.getDataValue("tabledataend"));
    }
    //set template data for table headline content
    template.setData("tableheadline", tableHead);

    // get vector of filterMethods and select the appropriate filter method,
    // if no filter is appropriate, select a default filter get number of rows for output
    Vector tableContent = new Vector();
    try {
      Vector filterMethods = (Vector) cdClass.getMethod("getFilterMethods", new Class[] {CmsObject.class}).invoke(null, new Object[] {cms});
      CmsFilterMethod filterMethod = null;
      CmsFilterMethod filterName = (CmsFilterMethod) filterMethods.elementAt(Integer.parseInt(filterMethodName));
      filterMethodName = filterName.getFilterName();
      //loop trough the filter methods and set the chosen one
      for (int i = 0; i < filterMethods.size(); i++) {
        CmsFilterMethod currentFilter = (CmsFilterMethod) filterMethods.elementAt(i);
    if (currentFilter.getFilterName().equals(filterMethodName)) {
      filterMethod = currentFilter;
      break;
    }
      }

      // the chosen filter does not exist, use the first one!
      if (filterMethod == null) {
        filterMethod = (CmsFilterMethod) filterMethods.firstElement();
      }
      // now apply the filter with the cms object, the filter method and additional user parameters
      tableContent = (Vector) cdClass.getMethod("applyFilter", new Class[] {CmsObject.class, CmsFilterMethod.class, String.class}).invoke(null, new Object[] {cms, filterMethod, filterParam});
    } catch (InvocationTargetException ite) {
      //error occured while applying the filter
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice: apply filter throwed an InvocationTargetException!");
      }
      templateSelector = "error";
      template.setData("filtername", filterMethodName);
      while(ite.getTargetException() instanceof InvocationTargetException) {
        ite = ((InvocationTargetException) ite.getTargetException());
      }
      template.setData("filtererror", ite.getTargetException().getMessage());
      session.removeValue(sessionFilterParam);
      //session.removeValue("filter");
    } catch (NoSuchMethodException nsm) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice: apply filter method was not found!");
      }
      templateSelector = "error";
      template.setData("filtername", filterMethodName);
      template.setData("filtererror", nsm.getMessage());
      session.removeValue(sessionFilterParam);
      //session.removeValue("filterparameter");
    } catch (Exception e) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice: apply filter: Other Exception! "+e);
      }
      templateSelector = "error";
      template.setData("filtername", filterMethodName);
      template.setData("filtererror", e.getMessage());
      session.removeValue(sessionFilterParam);
      //session.removeValue("filterparameter");
    }

    //get the number of rows
    int rows = tableContent.size();

    // get the field methods from the content definition
    Vector fieldMethods = new Vector();
    try {
      fieldMethods = (Vector) cdClass.getMethod("getFieldMethods", new Class[] {CmsObject.class}).invoke(null, new Object[] {cms});
    } catch (Exception exc) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice getContentList: getFieldMethods throwed an exception");
      }
      templateSelector = "error";
      template.setData("filtername", filterMethodName);
      template.setData("filtererror", exc.getMessage());
    }

    // create output from the table data
    String fieldEntry = "";
    String id = "";
    for (int i = 0; i < rows; i++) {
      //init
      entry = "";
      singleRow = "";
      Object entryObject = new Object();
      entryObject = tableContent.elementAt(i); //cd object in row #i

      //set data of single row
      for (int j = 0; j < columns; j++) {
        fieldEntry = "+++ NO VALUE FOUND +++";
    // call the field methods
    Method getMethod = null;
    try {
      getMethod = (Method) fieldMethods.elementAt(j);

    } catch (Exception e) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Could not get field method for "+(String)columnsVector.elementAt(j)+" - check for correct spelling!");
      }
    }
    try {
      //apply methods on content definition object
      Object fieldEntryObject = null;

      fieldEntryObject = getMethod.invoke(entryObject, new Object[0]);

          if (fieldEntryObject != null) {
            fieldEntry = fieldEntryObject.toString();
            } else {
              fieldEntry = null;
            }
        } catch (InvocationTargetException ite) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice content definition object throwed an InvocationTargetException!");
      }
    } catch (Exception e) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice content definition object: Other exception! "+e);
      }
    }

    try {
          id = ((A_CmsContentDefinition)entryObject).getUniqueId(cms);
    } catch (Exception e) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice: getUniqueId throwed an Exception!");
          }
    }

    //insert unique id in contextmenue
    if (id != null) {
      template.setData("uniqueid", id);
        }
        //insert table entry
    if (fieldEntry != null) {
          try{
            Vector v = new Vector();
            v.addElement(new Integer(id));
            v.addElement(template);
            url = getUrl(cms, null, null,v);
          }catch (Exception e) {
            url = "";
          }
          if(!url.equals("")) {
            // enable url
            entry += (template.getDataValue("tabledatabegin")) + (template.getProcessedDataValue("url", this, parameters)) + fieldEntry + (template.getDataValue("tabledataend"));
          }else {
            // disable url
            entry += (template.getDataValue("tabledatabegin")) + fieldEntry + (template.getDataValue("tabledataend"));
          }
    } else {
      entry += (template.getDataValue("tabledatabegin"))  + "" + (template.getDataValue("tabledataend"));
    }
      }
      //get the unique id belonging to an entry
      try {
        id = ((A_CmsContentDefinition)entryObject).getUniqueId(cms);
      } catch (Exception e) {
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
      A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice: getUniqueId throwed an Exception!");
    }
      }

      //insert unique id in contextmenue
      if (id != null) {
        template.setData("uniqueid", id);
      }

      //set the lockstates for the actual entry
      setLockstates(cms, template, cdClass, entryObject, parameters);

      //insert single table row in template
      template.setData("entry", entry);

      // processed row from template
      singleRow = template.getProcessedDataValue("singlerow", this, parameters);
      allEntrys += (template.getDataValue("tablerowbegin")) + singleRow + (template.getDataValue("tablerowend"));
    }

    //insert tablecontent in template
    template.setData("tablecontent", "" + allEntrys);

    //save select box value into session
    session.putValue("selectbox", filterMethodName);

    //finally start the processing
    processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
  return processResult;
}

    /**
     * Gets the content of a given template file.
     * This method displays any content provided by a content definition
     * class on the template. The used backoffice class does not need to use a
     * special getContent method. It just has to extend the methods of this class!
     * Using reflection, this method creates the table headline and table content
     * with the layout provided by the template automatically!
     * @param cms CmsObjectfor accessing system resources
     * @param templateFile Filename of the template file
     * @param elementName <em>not used here</em>.
     * @param parameters <em>not used here</em>.
     * @param templateSelector template section that should be processed.
     * @return Processed content of the given template file.
     * @exception CmsException
     */
    private byte[] getContentExtendedList(CmsObject cms, CmsXmlWpTemplateFile template, String elementName,
                                Hashtable parameters, String templateSelector) throws CmsException {

        //return var
        byte[] processResult = null;
        // session will be created or fetched
        I_CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
        //get the class of the content definition
        Class cdClass = getContentDefinitionClass();

        String action = (String) parameters.get("action");

        //read value of the selected filter
        String filterMethodName = (String) session.getValue("filter");
        if (filterMethodName == null) {
            filterMethodName = "0";
        }
        String uri = cms.getRequestContext().getUri();
        String sessionFilterParam = uri+filterMethodName+"filterparameter";
        //read value of the inputfield filterparameter
        String filterParam = (String) session.getValue(sessionFilterParam);
        if (filterParam == "") {
            filterParam = null;
        }
        //change template to list section for data list output
        templateSelector = "list";

        //init vars
        String tableHead = "";
        String singleRow = "";
        String allEntrys = "";
        String entry = "";
        String url = "";
        int columns = 0;
        String style = ">";

        // get number of columns
        Vector columnsVector = new Vector();
        String fieldNamesMethod = "getFieldNames";
        Class paramClasses[] = {CmsObject.class};
        Object params[] = {cms};
        columnsVector = (Vector) getContentMethodObject(cms, cdClass, fieldNamesMethod, paramClasses, params);
        columns = columnsVector.size();
        //create appropriate class name with underscores for labels
        String moduleName = "";
        moduleName = (String) getClass().toString(); //get name
        moduleName = moduleName.substring(5); //remove 'class' substring at the beginning
        moduleName = moduleName.trim();
        moduleName = moduleName.replace('.', '_'); //replace dots with underscores

        //create new language file object
        CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);

        //create tableheadline
        for (int i = 0; i < columns; i++) {
            tableHead += (template.getDataValue("tabledatabegin"))
                        + style
                        + lang.getLanguageValue(moduleName + ".label."
                        + columnsVector.elementAt(i).toString().toLowerCase().trim())
                        + (template.getDataValue("tabledataend"));
        }
        //set template data for table headline content
        template.setData("tableheadline", tableHead);
        // get vector of filterMethods and select the appropriate filter method,
        // if no filter is appropriate, select a default filter get number of rows for output
        Vector tableContent = new Vector();
        try {
            Vector filterMethods = (Vector) cdClass.getMethod("getFilterMethods", new Class[] {CmsObject.class}).invoke(null, new Object[] {cms});
            CmsFilterMethod filterMethod = null;
            CmsFilterMethod filterName = (CmsFilterMethod) filterMethods.elementAt(Integer.parseInt(filterMethodName));
            filterMethodName = filterName.getFilterName();
            //loop trough the filter methods and set the chosen one
            for (int i = 0; i < filterMethods.size(); i++) {
                CmsFilterMethod currentFilter = (CmsFilterMethod) filterMethods.elementAt(i);
                if (currentFilter.getFilterName().equals(filterMethodName)) {
                    filterMethod = currentFilter;
                    break;
                }
            }
            // the chosen filter does not exist, use the first one!
            if (filterMethod == null) {
                filterMethod = (CmsFilterMethod) filterMethods.firstElement();
            }
            // now apply the filter with the cms object, the filter method and additional user parameters
            tableContent = (Vector) cdClass.getMethod("applyFilter", new Class[] {CmsObject.class, CmsFilterMethod.class, String.class}).invoke(null, new Object[] {cms, filterMethod, filterParam});
        } catch (InvocationTargetException ite) {
            //error occured while applying the filter
            if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice: apply filter throwed an InvocationTargetException!");
            }
            templateSelector = "error";
            template.setData("filtername", filterMethodName);
            while(ite.getTargetException() instanceof InvocationTargetException) {
                ite = ((InvocationTargetException) ite.getTargetException());
            }
            template.setData("filtererror", ite.getTargetException().getMessage());
            session.removeValue(sessionFilterParam);
            //session.removeValue("filter");
        } catch (NoSuchMethodException nsm) {
            if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice: apply filter method was not found!");
            }
            templateSelector = "error";
            template.setData("filtername", filterMethodName);
            template.setData("filtererror", nsm.getMessage());
            session.removeValue(sessionFilterParam);
            //session.removeValue("filterparameter");
        } catch (Exception e) {
            if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice: apply filter: Other Exception! "+e);
            }
            templateSelector = "error";
            template.setData("filtername", filterMethodName);
            template.setData("filtererror", e.getMessage());
            session.removeValue(sessionFilterParam);
            //session.removeValue("filterparameter");
        }

        //get the number of rows
        int rows = tableContent.size();
        // get the field methods from the content definition
        Vector fieldMethods = new Vector();
        try {
            fieldMethods = (Vector) cdClass.getMethod("getFieldMethods", new Class[] {CmsObject.class}).invoke(null, new Object[] {cms});
        } catch (Exception exc) {
            if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice getContentList: getFieldMethods throwed an exception");
            }
            templateSelector = "error";
            template.setData("filtername", filterMethodName);
            template.setData("filtererror", exc.getMessage());
        }

        // create output from the table data
        String fieldEntry = "";
        String id = "";
        for (int i = 0; i < rows; i++) {
            //init
            entry = "";
            singleRow = "";
            Object entryObject = new Object();
            entryObject = tableContent.elementAt(i); //cd object in row #i

            // set the fontformat of the current row
            // each entry is formated depending on the state of the cd object
            style = this.getStyle(cms, template, entryObject)+">";
            //style = ">";
            //set data of single row
            for (int j = 0; j < columns; j++) {
                fieldEntry = "+++ NO VALUE FOUND +++";
                // call the field methods
                Method getMethod = null;
                try {
                    getMethod = (Method) fieldMethods.elementAt(j);
                } catch (Exception e) {
                    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Could not get field method for "+(String)columnsVector.elementAt(j)+" - check for correct spelling!");
                    }
                }
                try {
                    //apply methods on content definition object
                    Object fieldEntryObject = null;
                    fieldEntryObject = getMethod.invoke(entryObject, new Object[0]);
                    if (fieldEntryObject != null) {
                        fieldEntry = fieldEntryObject.toString();
                    } else {
                        fieldEntry = null;
                    }
                } catch (InvocationTargetException ite) {
                    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice content definition object throwed an InvocationTargetException!");
                    }
                } catch (Exception e) {
                    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice content definition object: Other exception! "+e);
                    }
                }
                try {
                    id = ((A_CmsContentDefinition)entryObject).getUniqueId(cms);
                } catch (Exception e) {
                    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice: getUniqueId throwed an Exception!");
                    }
                }

                //insert unique id in contextmenue
                if (id != null) {
                    template.setData("uniqueid", id);
                }
                //insert table entry
                if (fieldEntry != null) {
                    try{
                        Vector v = new Vector();
                        v.addElement(new Integer(id));
                        v.addElement(template);
                        url = getUrl(cms, null, null,v);
                    }catch (Exception e) {
                        url = "";
                    }
                    if(!url.equals("")) {
                        // enable url
                        entry += (template.getDataValue("tabledatabegin"))
                                + style
                                + (template.getProcessedDataValue("url", this, parameters))
                                + fieldEntry
                                + (template.getDataValue("tabledataend"));
                    } else {
                        // disable url
                        entry += (template.getDataValue("tabledatabegin"))
                                + style
                                + fieldEntry
                                + (template.getDataValue("tabledataend"));
                    }
                } else {
                    entry += (template.getDataValue("tabledatabegin"))
                            + ""
                            + (template.getDataValue("tabledataend"));
                }
            }
            //get the unique id belonging to an entry
            try {
                id = ((A_CmsContentDefinition)entryObject).getUniqueId(cms);
            } catch (Exception e) {
                if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                    A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice: getUniqueId throwed an Exception!");
                }
            }

            //insert unique id in contextmenue
            if (id != null) {
                template.setData("uniqueid", id);
            }
            //set the lockstates for the current entry
            setLockstates(cms, template, cdClass, entryObject, parameters);
            //set the projectflag for the current entry
            setProjectFlag(cms, template, cdClass, entryObject, parameters);

            //insert single table row in template
            template.setData("entry", entry);
            // processed row from template
            singleRow = template.getProcessedDataValue("singlerow", this, parameters);
            allEntrys += (template.getDataValue("tablerowbegin"))
                        + singleRow
                        + (template.getDataValue("tablerowend"));
        }

        //insert tablecontent in template
        template.setData("tablecontent", "" + allEntrys);

        //save select box value into session
        session.putValue("selectbox", filterMethodName);

        //finally start the processing
        processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
        return processResult;
    }

  /**
  * Gets the content of a given template file.
  * <P>
  * While processing the template file the table entry
  * <code>entryTitle<code> will be displayed in the delete dialog
  *
  * @param cms A_CmsObject Object for accessing system resources
  * @param templateFile Filename of the template file
  * @param elementName not used here
  * @param parameters get the parameters action for the button activity
  * and id for the used content definition instance object
  * @param templateSelector template section that should be processed.
  * @return Processed content of the given template file.
  * @exception CmsException
  */

  private byte[] getContentLock(CmsObject cms, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
    //return var
    byte[] processResult = null;

    // now check if the "do you really want to lock" dialog should be shown.
    Hashtable startSettings = (Hashtable)cms.getRequestContext().currentUser().getAdditionalInfo(C_ADDITIONAL_INFO_STARTSETTINGS);
    String showLockDialog = "on";
    if(startSettings!=null){
      showLockDialog = (String)startSettings.get(C_START_LOCKDIALOG);
    }
    if (!showLockDialog.equalsIgnoreCase("on")) {
      parameters.put("action","go");
    }


    // session will be created or fetched
    I_CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
    //get the class of the content definition
    Class cdClass = getContentDefinitionClass();
    int actUserId = cms.getRequestContext().currentUser().getId();

    //get (stored) id parameter
    String id = (String) parameters.get("id");
    if (id == null)
    id = "";

    /*if (id != "") {
        session.putValue("idsave", id);
    } else {
        String idsave = (String) session.getValue("idsave");
        if (idsave == null)
            idsave = "";
        id = idsave;
        session.removeValue("idsave");
    } */

    parameters.put("idlock", id);

    // get value of hidden input field action
    String action = (String) parameters.get("action");

    //no button pressed, go to the default section!
    if (action == null || action.equals("")) {
      //lock dialog, displays the title of the entry to be changed in lockstate
      templateSelector = "lock";
      Integer idInteger = null;
      int ls = -1;
      try {
        idInteger = Integer.valueOf(id);
      } catch (Exception e) {
    ls = -1;
    //access content definition object specified by id through reflection
    String title = "no title";
    Object o = null;
    o = getContentDefinition(cms, cdClass, id);
        try {
          ls = ((A_CmsContentDefinition) o).getLockstate();
      /*Method getLockstateMethod = (Method) cdClass.getMethod("getLockstate", new Class[] {});
      ls = (int) getLockstateMethod.invoke(o, new Object[0]); */
    } catch (Exception exc) {
      exc.printStackTrace();
    }
      }
      //access content definition object specified by id through reflection
      String title = "no title";
      Object o = null;
      if (idInteger != null) {
        o = getContentDefinition(cms, cdClass, idInteger);
    try {
          ls = ((A_CmsContentDefinition) o).getLockstate();
    } catch (Exception e) {
          if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(C_OPENCMS_INFO, e.getMessage() );
          }
    }
      } else {
    o = getContentDefinition(cms, cdClass, id);
    try {
          ls = ((A_CmsContentDefinition) o).getLockstate();
    } catch (Exception e) {
      if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(C_OPENCMS_INFO, e.getMessage() );
          }
    }
      }
      //create appropriate class name with underscores for labels
      String moduleName = "";
      moduleName = (String) getClass().toString(); //get name
      moduleName = moduleName.substring(5); //remove 'class' substring at the beginning
      moduleName = moduleName.trim();
      moduleName = moduleName.replace('.', '_'); //replace dots with underscores
      //create new language file object
      CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);
      //get the dialog from the langauge file and set it in the template
      if (ls != C_NOT_LOCKED && ls != actUserId) {
        // "lock"
    template.setData("locktitle", lang.getLanguageValue("messagebox.title.lockchange"));
    template.setData("lockstate", lang.getLanguageValue("messagebox.message1.lockchange"));
      }
      if (ls == C_NOT_LOCKED) {
        // "nolock"
    template.setData("locktitle", lang.getLanguageValue("messagebox.title.lock"));
    template.setData("lockstate", lang.getLanguageValue("messagebox.message1.lock"));
      }
      if (ls == actUserId) {
        template.setData("locktitle", lang.getLanguageValue("messagebox.title.unlock"));
    template.setData("lockstate", lang.getLanguageValue("messagebox.message1.unlock"));
      }

      //set the title of the selected entry
      template.setData("newsentry", id);

      //go to default template section
      template.setData("setaction", "default");
      parameters.put("action", "done");

      // confirmation button pressed, process data!
    } else {
      templateSelector = "done";
      // session.removeValue("idsave");

      //access content definition constructor by reflection
      Integer idInteger = null;
      int ls = C_NOT_LOCKED;
      try {
        idInteger = Integer.valueOf(id);
      } catch (Exception e) {
        ls = C_NOT_LOCKED;

      //access content definition object specified by id through reflection
      String title = "no title";
      Object o = null;
      o = getContentDefinition(cms, cdClass, id);
      try {
        ls = ((A_CmsContentDefinition) o).getLockstate();
      } catch (Exception ex) {
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
          A_OpenCms.log(C_OPENCMS_INFO, ex.getMessage() );
        }
      }
    }

    //call the appropriate content definition constructor
    Object o = null;
    if (idInteger != null) {
      o = getContentDefinition(cms, cdClass, idInteger);
      try {
        ls = ((A_CmsContentDefinition) o).getLockstate();
      } catch (Exception e) {
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
         A_OpenCms.log(C_OPENCMS_INFO, e.getMessage() );
        }
      }
    } else {
      o = getContentDefinition(cms, cdClass, id);
      try {
        ls = ((A_CmsContentDefinition) o).getLockstate();
      } catch (Exception e) {
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
          A_OpenCms.log(C_OPENCMS_INFO, e.getMessage() );
        }
      }
    }
    try {
      ls = ((A_CmsContentDefinition) o).getLockstate();
    } catch (Exception e) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + " Backoffice getContentLock: Method getLockstate throwed an exception!");
      }
    }

    //show the possible cases of a lockstate in the template
    //and change lockstate in content definition (and in DB or VFS)
    if (ls == actUserId) {
      //steal lock (userlock -> nolock)
      try {
        ((A_CmsContentDefinition) o).setLockstate(C_NOT_LOCKED);
      } catch (Exception e) {
    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
          A_OpenCms.log(C_OPENCMS_INFO, getClassName() + " Backoffice getContentLock: Method setLockstate throwed an exception!");
    }
      }
      //write to DB
      try {
        ((A_CmsContentDefinition) o).write(cms);   // reflection is not neccessary!
      } catch (Exception e) {
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
      A_OpenCms.log(C_OPENCMS_INFO, getClassName() + " Backoffice getContentLock: Method write throwed an exception!");
    }
      }
      templateSelector = "done";
    } else {
      if ((ls != C_NOT_LOCKED) && (ls != actUserId)) {
      //unlock (lock -> userlock)
        try {
          ((A_CmsContentDefinition) o).setLockstate(actUserId);
        } catch (Exception e) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice getContentLock: Could not set lockstate!");
      }
    }
    //write to DB
    try {
          ((A_CmsContentDefinition) o).write(cms);
    } catch (Exception e) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice getContentLock: Could not set lockstate!");
      }
    }
    templateSelector = "done";
      } else {
      //lock (nolock -> userlock)
      try {
        ((A_CmsContentDefinition) o).setLockstate(actUserId);
      } catch (Exception e) {
    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
      A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice getContentLock: Could not set lockstate!");
    }
      }
      //write to DB/VFS
      try {
        ((A_CmsContentDefinition) o).write(cms);
      } catch (Exception e) {
    if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
      A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice getContentLock: Could not write to content definition!");
        }
      }
    }
  }
}
  //finally start the processing
  processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
  return processResult;
  }
/**
 * gets the content definition class method object
 * @returns object content definition class method object
 */

private Object getContentMethodObject(CmsObject cms, Class cdClass, String method, Class paramClasses[], Object params[]) {

    //return value
    Object retObject = null;
    if (method != "") {
        try {
            retObject = cdClass.getMethod(method, paramClasses).invoke(null, params);
        } catch (InvocationTargetException ite) {
            if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(C_OPENCMS_INFO, getClassName() + method + " throwed an InvocationTargetException!");
            }
            ite.getTargetException().printStackTrace();
        } catch (NoSuchMethodException nsm) {
            if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(C_OPENCMS_INFO, getClassName() + method + ": Requested method was not found!");
            }
        } catch (Exception e) {
            if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(C_OPENCMS_INFO, getClassName() + method + ": Other Exception!");
            }
        }
    }
    return retObject;
}


  /**
  * The old version of the getContentNew methos. Only available for compatilibility resons.
  * Per default, it uses the edit dialog. If you want to use a seperate input form, you have to create
  * a new one and write your own getContentNew method in your backoffice class.
  */
  public byte[] getContentNew(CmsObject cms, CmsXmlWpTemplateFile templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
    parameters.put("id", "new");
    return getContentEdit(cms, templateFile, elementName, parameters, templateSelector);
  }

  /**
  * The new version of the getContentNew methos. Only this one should be used from now on.
  * Per default, it uses the edit dialog. If you want to use a seperate input form, you have to create
  * a new one and write your own getContentNew method in your backoffice class.
  */
  public String getContentNew(CmsObject cms,CmsXmlWpTemplateFile template,  A_CmsContentDefinition cd, String elementName,Enumeration keys, Hashtable parameters, String templateSelector) throws CmsException {
    parameters.put("id", "new");
    return getContentEdit(cms, template, cd,elementName, keys ,parameters, templateSelector);
  }


  /**
  * Gets the content of a edited entry form.
  * Has to be overwritten in your backoffice class if the old way of writeing BackOffices is used.
  * Only available for compatibility reasons.
  */
  public byte[] getContentEdit(CmsObject cms,CmsXmlWpTemplateFile templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
    return null;
  }


  /**
  * Gets the content of a edited entry form.
  * Has to be overwritten in your backoffice class if the new way of writeing BackOffices is used.
  */
  public String getContentEdit(CmsObject cms,CmsXmlWpTemplateFile templateFile,  A_CmsContentDefinition cd, String elementName,Enumeration keys, Hashtable parameters, String templateSelector) throws CmsException {
   return null;
  }


        /**
     * Used for filling the values of a checkbox.
     * <P>
     * Gets the resources displayed in the Checkbox group on the new resource dialog.
     * @param cms The CmsObject.
     * @param lang The langauge definitions.
     * @param names The names of the new rescources (used for optional images).
     * @param values The links that are connected with each resource.
     * @param parameters Hashtable of parameters.
     * @returns The vectors names and values are filled with the information found in the
     * workplace.ini.
     * @exception Throws CmsException if something goes wrong.
     */
    /*public Integer setCheckbox(CmsObject cms, Vector names, Vector values, Hashtable parameters)
        throws CmsException {
            int returnValue = 0;
            CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
            String checkboxValue = (String) session.getValue("checkselect");
            if (checkboxValue == null){
                checkboxValue = "";
            }
            // add values for the checkbox
            values.addElement("contents");
            values.addElement("navigation");
            values.addElement("design");
            values.addElement("other");
            // add corresponding names for the checkboxvalues
            names.addElement("contents");
            names.addElement("navigation");
            names.addElement("design");
            names.addElement("other");
            // set the return values
            if (checkboxValue.equals("contents")) {
                returnValue = 0;
            }
            if (checkboxValue.equals("navigation")) {
                returnValue = 1;
            }
            if (checkboxValue.equals("design")) {
                returnValue = 2;
            }
            if (checkboxValue.equals("other")) {
                returnValue = 3;
            }
        return new Integer (returnValue);
    }*/



  /**
  * Set the correct lockstates in the list output.
  * Lockstates can be "unlocked", "locked", "locked by user" or "no access"
  * @param cms The current CmsObject.
  * @param template The actual template file.
  * @param cdClass The content defintion.
  * @param entryObject
  * @param paramters All template ands URL parameters.
  */
  private void setLockstates(CmsObject cms, CmsXmlWpTemplateFile template, Class cdClass,
                             Object entryObject, Hashtable parameters) {

    //init lock state vars
    String la = "false";
    Object laObject = new Object();
    int ls = -1;
    String lockString = null;
    int actUserId = cms.getRequestContext().currentUser().getId();
    String isLockedBy = null;

    //is the content definition object (i.e. the table entry) lockable?
    try {
      //get the method
      Method laMethod = cdClass.getMethod("isLockable", new Class[] {});
      //get the returned object
      laObject = laMethod.invoke(null, null);
    } catch (InvocationTargetException ite) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setLockstates: Method isLockable throwed an Invocation target exception!");
      }
    } catch (NoSuchMethodException nsm) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setLockstates: Requested method isLockable was not found!");
      }
    } catch (Exception e) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setLockstates: Method isLockable throwed an exception: "+e.toString());
      }
    }

    //cast the returned object to a string
    la = (String) laObject.toString();
    if (la.equals("false")) {
      try{
        //the entry is not lockable: use standard contextmenue
        template.setData("backofficecontextmenue", "backofficeedit");
    template.setData("lockedby", template.getDataValue("nolock"));
      } catch  (Exception e) {
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
      A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setLockstates:'not lockable' section hrowed an exception!");
    }
      }
    } else {
      //...get the lockstate of an entry
      try {
        //get the method lockstate
        ls = ((A_CmsContentDefinition) entryObject).getLockstate();
      } catch (Exception e) {
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
      A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setLockstates: Method getLockstate throwed an exception: "+e.toString());
        }
      }
      try {
        //show the possible cases of a lockstate in the template
    if (ls == actUserId) {
          // lockuser
          isLockedBy = cms.getRequestContext().currentUser().getName();
          template.setData("isLockedBy", isLockedBy);   // set current users name in the template
      lockString = template.getProcessedDataValue("lockuser", this, parameters);
      template.setData("lockedby", lockString);
      template.setData("backofficecontextmenue", "backofficelockuser");
    } else {
          if (ls != C_NOT_LOCKED) {
            // lock
            // set the name of the user who locked the file in the template ...
            if (ls==C_NO_ACCESS) {
              lockString = template.getProcessedDataValue("noaccess", this, parameters);
              template.setData("lockedby", lockString);
              template.setData("backofficecontextmenue", "backofficenoaccess");
            } else {
              isLockedBy = cms.readUser(ls).getName();
              template.setData("isLockedBy", isLockedBy);
              lockString = template.getProcessedDataValue("lock", this, parameters);
          template.setData("lockedby", lockString);
          template.setData("backofficecontextmenue", "backofficelock");
            }
      } else {
            // nolock
        lockString = template.getProcessedDataValue("nolock", this, parameters);
        template.setData("lockedby", lockString);
        template.setData("backofficecontextmenue", "backofficenolock");
      }
    }
      } catch (Exception e) {
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
      A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setLockstates throwed an exception: "+e.toString());
    }
      }
    }
  }

    /**
     * Set the correct project flag in the list output.
     * Lockstates can be "unlocked", "locked", "locked by user" or "no access"
     * @param cms The current CmsObject.
     * @param template The actual template file.
     * @param cdClass The content defintion.
     * @param entryObject
     * @param paramters All template ands URL parameters.
     */
    private void setProjectFlag(CmsObject cms, CmsXmlWpTemplateFile template, Class cdClass,
                             Object entryObject, Hashtable parameters) {

        //init project flag vars
        int state = 0;
        int projectId = 1;
        String projectFlag = null;
        int actProjectId = cms.getRequestContext().currentProject().getId();
        String isInProject = null;

        // get the state of an entry: if its unchanged do not show the flag
        try {
            state = ((I_CmsExtendedContentDefinition)entryObject).getState();
        } catch (Exception e) {
            if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setProjectFlag: Method getState throwed an exception: "+e.toString());
            }
        }

        if (state == 0) {
            try {
                //the entry is not changed, so do not set the project flag
                template.setData("projectflag", template.getDataValue("noproject"));
            } catch  (Exception e) {
                if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                    A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setProjectFlag:'no project' section throwed an exception!");
                }
            }
        } else {
            // the entry is new, changed or deleted
            //...get the project of the entry
            try {
                projectId = ((I_CmsExtendedContentDefinition)entryObject).getLockedInProject();
            } catch (Exception e) {
                if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                    A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setProjectFlag: Method getLockedInProject throwed an exception: "+e.toString());
                }
            }
            try {
                //show the possible cases of a lockstate in the template
                try{
                    isInProject = cms.readProject(projectId).getName();
                } catch (CmsException e){
                    isInProject = "";
                }
                // set project name in the template
                template.setData("isInProject", isInProject);
                if (projectId == actProjectId) {
                    // changed in this project
                    projectFlag = template.getProcessedDataValue("thisproject", this, parameters);
                    template.setData("projectflag", projectFlag);
                } else {
                    // changed in another project
                    projectFlag = template.getProcessedDataValue("otherproject", this, parameters);
                    template.setData("projectflag", projectFlag);
                }
            } catch (Exception e) {
                if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                    A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setLockstates throwed an exception: "+e.toString());
                }
            }
        }
    }

    /**
     * Return the format for the list output.
     * States can be "not in project", "unchanged", "changed", "new" or "deleted"
     *
     * @param cms The current CmsObject.
     * @param entryObject
     * @return String The format tag for the entry
     */
    private String getStyle(CmsObject cms, CmsXmlWpTemplateFile template, Object entryObject) {

        //init project flag vars
        int state = 0;
        int projectId = 1;
        int actProjectId = cms.getRequestContext().currentProject().getId();
        String style = new String();

        // get the projectid of the entry
        try{
            projectId = ((I_CmsExtendedContentDefinition)entryObject).getProjectId();
        } catch (Exception e) {
            if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setFontFormat: Method getProjectId throwed an exception: "+e.toString());
            }
        }

        if (projectId != actProjectId) {
            // is in this project
            style = this.C_STYLE_NOTINPROJECT;
        } else {
            // get the state of an entry: if its unchanged do not change the font
            try {
                state = ((I_CmsExtendedContentDefinition)entryObject).getState();
            } catch (Exception e) {
                if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                    A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setFontFormat: Method getState throwed an exception: "+e.toString());
                }
            }

            switch (state){
                case C_STATE_NEW:
                    style = this.C_STYLE_NEW;
                    break;
                case C_STATE_CHANGED:
                    style = this.C_STYLE_CHANGED;
                    break;
                case C_STATE_DELETED:
                    style = this.C_STYLE_DELETED;
                    break;
                default:
                    style = this.C_STYLE_UNCHANGED;
                    break;
            }
        }
        // if there was an exception return an empty string
        return style;
    }

    /**
     * Checks if the publishProject method exists in the cd class
     *
     * @return boolean Is true if the method exist in the class
     */
    private boolean isExtendedList() {
        // get the publishProject method of the cd class
        Class cdClass = this.getContentDefinitionClass();
        try{
            Boolean theValue = (Boolean)cdClass.getMethod("isExtendedList", new Class[]{}).invoke(null, new Object[]{});
            return theValue.booleanValue();
        } catch (Exception e){
            return false;
        }
    }
  /**
  * This method creates the selectbox in the head-frame
  * @author Tilo Kellermeier
  */
  public Integer getFilter(CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values,
                           Hashtable parameters) throws CmsException {
    CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
    int returnValue = 0;
    String uri = cms.getRequestContext().getUri();
    String sessionSelectBoxValue = uri+"selectBoxValue";
    Vector filterMethods = getFilterMethods(cms);
    String tmp = (String) session.getValue(sessionSelectBoxValue);
    if(tmp != null) {
      returnValue = Integer.parseInt(tmp);
    }
    for (int i = 0; i < filterMethods.size(); i++) {
      CmsFilterMethod currentFilter = (CmsFilterMethod) filterMethods.elementAt(i);
      //insert filter in the template selectbox
      names.addElement(currentFilter.getFilterName());
      values.addElement(""+ i);
    }
    return new Integer(returnValue);
  }


  /**
   * User method that handles a checkbox in the input form of the backoffice.
   */
  public Object handleCheckbox(CmsObject cms, String tagcontent, A_CmsXmlContent doc,
                               Object userObject) throws CmsException {
    Hashtable parameters = (Hashtable) userObject;
    String returnValue="";
    String selected = (String) parameters.get(tagcontent);
      if (selected != null && selected.equals("on")) {
        returnValue="checked";
      }
  return returnValue;
  }


  /**
  * Get the all available filter methods.
  * @param cms The actual CmsObject
  * @return Vector of Filter Methods
  */
  private Vector getFilterMethods(CmsObject cms) {
    Vector filterMethods = new Vector();
    Class cdClass = getContentDefinitionClass();
    try {
      filterMethods = (Vector) cdClass.getMethod("getFilterMethods", new Class[] {CmsObject.class}).invoke(null, new Object[] {cms});
    } catch (InvocationTargetException ite) {
      //error occured while applying the filter
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
        A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice getContentHead: InvocationTargetException!");
      }
    } catch (NoSuchMethodException nsm) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
    A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice getContentHead: Requested method was not found!");
      }
    } catch (Exception e) {
      if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
    A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice getContentHead: Problem occured with your filter methods: "+e.toString());
      }
    }
   return filterMethods;
  }

 /**
 * This method creates the selectbox with all avaiable Pages to select from.
 */
  public Integer getSelectedPage(CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values,
                           Hashtable parameters) throws CmsException {

  int returnValue = 0;
  CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
  // get all aviable template selectors
  Vector templateSelectors=(Vector)session.getValue("backofficepageselectorvector");
  // get the actual template selector
  String selectedPage=(String)parameters.get("backofficepage");
  if (selectedPage==null) {
    selectedPage="";
  }

  // now build the names and values for the selectbox
  for (int i = 0; i < templateSelectors.size(); i++) {
    String selector = (String) templateSelectors.elementAt(i);
    names.addElement(selector);
    values.addElement(selector);
    // check for the selected item
    if (selectedPage.equals(selector)) {
      returnValue=i;
    }
  }
  session.putValue("backofficeselectortransfer",values);
  session.putValue("backofficeselectedtransfer",new Integer(returnValue));
  return new Integer(returnValue);
}


  /**
  * This method contains the code used by the getContent method when the new form of the backoffice is processed.
  * It automatically gets all the data from the "getXYZ" methods and sets it in the correct datablock of the
  * template.
  * @param cms The actual CmsObject.
  * @param id The id of the content definition object to be edited.
  * @param cd the content definition.
  * @param session The current user session.
  * @param template The template file.
  * @param elementName The emelentName of the template mechanism.
  * @param parameters All parameters of this request.
  * @param templateSelector The template selector.
  * @returns Content of the template, as an array of bytes.
  */
  private byte[] getContentNewInternal(CmsObject cms,String id,A_CmsContentDefinition cd,
                                       I_CmsSession session,CmsXmlWpTemplateFile template,
                                       String elementName,Hashtable parameters,
                                       String templateSelector) throws CmsException{


    //return varlue
    byte[] returnProcess = null;
    //error storage
    String error="";
    // action value of backoffice buttons
    String action = (String)parameters.get("action");

     // get the correct template selector
    templateSelector=getTemplateSelector(cms,template,parameters,templateSelector);

    //process the template for entering the data
    // first try to do it in the old way to be compatible to the existing modules.
    returnProcess = getContentNew(cms, template, elementName, parameters, templateSelector);

    // there was no returnvalue, so the BO uses the new getContentNew method
    if (returnProcess == null) {

      //try to get the CD form the session
      cd = (A_CmsContentDefinition) session.getValue(this.getContentDefinitionClass().getName());
        if (cd == null) {
          //get a new, empty content definition
          cd = (A_CmsContentDefinition) this.getContentDefinition(cms,this.getContentDefinitionClass());
       }
      //get all existing parameters
      Enumeration keys=parameters.keys();

      //call the new getContentEdit method
      error = getContentNew(cms, template, cd,elementName, keys ,parameters, templateSelector);

      // get all getXVY methods to automatically get the data from the CD
      Vector methods=this.getGetMethods(cd);
       // get all setXVY methods to automatically set the data into the CD
      Hashtable setMethods=this.getSetMethods(cd);
      //set all data from the parameters into the CD
      this.fillContentDefinition(cms,cd,parameters,setMethods);
      //now set all the data from the CD into the template
      this.setDatablocks(template,cd,methods);

      // store the modified CD in the session
      session.putValue(this.getContentDefinitionClass().getName(),cd);

       // check if there was an error found in the input form
      if (!error.equals("")) {
       template.setData("error",template.getProcessedDataValue("errormsg")+error);
      } else {
       template.setData("error","");
      }

      // now check if one of the exit buttons is used
      returnProcess = getContentButtonsInternal(cms,cd,session,template,parameters,templateSelector,
                                                action,error);
    }

    //finally retrun processed data
    return returnProcess;
  }



 /**
  * This method contains the code used by the getContent method when the edit form of the backoffice is processed.
  * It automatically gets all the data from the "getXYZ" methods and sets it in the correct datablock of the
  * template.
  * @param cms The actual CmsObject.
  * @param id The id of the content definition object to be edited.
  * @param cd the content definition.
  * @param session The current user session.
  * @param template The template file.
  * @param elementName The emelentName of the template mechanism.
  * @param parameters All parameters of this request.
  * @param templateSelector The template selector.
  * @returns Content of the template, as an array of bytes.
  */
  private byte[] getContentEditInternal(CmsObject cms,String id,A_CmsContentDefinition cd,
                                        I_CmsSession session,CmsXmlWpTemplateFile template,
                                        String elementName,Hashtable parameters,
                                        String templateSelector) throws CmsException{

    //return varlue
    byte[] returnProcess = null;
    //error storage
    String error="";
    // action value of backoffice buttons
    String action = (String)parameters.get("action");

    // get the correct template selector
    templateSelector=getTemplateSelector(cms,template,parameters,templateSelector);

    //process the template for editing the data
    // first try to do it in the old way to be compatible to the existing modules.
    returnProcess = getContentEdit(cms, template, elementName, parameters, templateSelector);

    // there was no returnvalue, so the BO uses the new getContentEdit method
    if (returnProcess == null) {
    //try to get the CD form the session
      cd = (A_CmsContentDefinition) session.getValue(this.getContentDefinitionClass().getName());
      if (cd == null) {
        // not successful, so read new content definition with given id
        Integer idvalue=new Integer (id);
        cd = (A_CmsContentDefinition) this.getContentDefinition(cms,this.getContentDefinitionClass(),idvalue);
      }

      //get all existing parameters
      Enumeration keys=parameters.keys();
      //call the new getContentEdit method
      error = getContentEdit(cms, template, cd,elementName, keys ,parameters, templateSelector);

      // get all getXVY methods to automatically get the data from the CD
      Vector getMethods=this.getGetMethods(cd);

      // get all setXVY methods to automatically set the data into the CD
      Hashtable setMethods=this.getSetMethods(cd);

      //set all data from the parameters into the CD
      this.fillContentDefinition(cms,cd,parameters,setMethods);
      //now set all the data from the CD into the template
      this.setDatablocks(template,cd,getMethods);

      // store the modified CD in the session
      session.putValue(this.getContentDefinitionClass().getName(),cd);

      // check if there was an error found in the input form
      if (!error.equals("")) {
       template.setData("error",template.getProcessedDataValue("errormsg")+error);
      } else {
       template.setData("error","");
      }

      // now check if one of the exit buttons is used
      returnProcess = getContentButtonsInternal(cms,cd,session,template,parameters,templateSelector,
                                                action,error);

    }
    //finally return processed data
    return returnProcess;
  }


  /** This method checks the three function buttons on the backoffice edit/new template.
   *  The following actions are excecuted: <ul>
   *  <li>Save: Save the CD to the database if plausi shows no error, return to the edit/new template.</li>
   *  <li>Save & Exit: Save the CD to the database if plausi shows no error, return to the list view. </li>
   *  <li>Exit: Return to the list view without saving. </li>
   *  </ul>
   *  @param cms The actual CmsObject.
   *  @param cd The acutal content definition.
   *  @param session The current user session.
   *  @param template The template file.
   *  @param parameters All parameters of this request.
   *  @param templateSelector The template selector.
   *  @returns Content of the template, as an array of bytes or null if a rediect has happend.
   */
 private byte[] getContentButtonsInternal(CmsObject cms,A_CmsContentDefinition cd,
                                          I_CmsSession session, CmsXmlWpTemplateFile template,
                                          Hashtable parameters, String templateSelector,
                                          String action, String error) throws CmsException{

  // storage for possible errors during plausibilization
  Vector errorCodes=null;
  // storage for a single error code
  String errorCode=null;
  // storage for the field where an error occured
  String errorField=null;
  // storage for the error type
  String errorType=null;
  // the complete error text displayed on the template
  //String error=null;
  //  check if one of the exit buttons is used


  if (action!=null) {
    // there was no button selected, so the selectbox was used. Do a check of the input fileds.
    if ((!action.equals("save")) && (!action.equals("saveexit")) && (!action.equals("exit"))){
      try {
        cd.check(false);
      } catch (CmsPlausibilizationException plex) {
        // there was an error during plausibilization, so create an error text
        errorCodes=plex.getErrorCodes();
        //loop through all errors
        for (int i=0;i<errorCodes.size();i++) {
          errorCode=(String)errorCodes.elementAt(i);
          // try to get an error message that fits thos this error code exactly
          if (template.hasData(C_ERRPREFIX+errorCode)) {
            error+=template.getProcessedDataValue(C_ERRPREFIX+errorCode);
          } else {
            // now check if there is a general error message for this field
            errorField=errorCode.substring(0,errorCode.indexOf(A_CmsContentDefinition.C_ERRSPERATOR));
            if (template.hasData(C_ERRPREFIX+errorField)) {
              error+=template.getProcessedDataValue(C_ERRPREFIX+errorField);
            } else {
              // now check if there is at least a general error messace for the error type
              errorType=errorCode.substring(errorCode.indexOf(C_ERRSPERATOR)+1,errorCode.length());
              if (template.hasData(C_ERRPREFIX+errorType)) {
                error+=template.getProcessedDataValue(C_ERRPREFIX+errorType);
              } else {
                // no error dmessage was found, so generate a default one
                error+="["+errorCode+"]";
              }
            }
          }
        }
        //check if there is an introtext for the errors
        if (template.hasData("errormsg")) {
          error=template.getProcessedDataValue("errormsg")+error;
        }
        template.setData("error",error);
        if (session.getValue("backofficepagetemplateselector")!=null) {

          templateSelector=(String)session.getValue("backofficepagetemplateselector");
          parameters.put("backofficepage",templateSelector);

        } else {
          templateSelector=null;
        }

      } // catch
    }
    // the same or save&exit button were pressed, so save the content definition to the
    // database
    if (((action.equals("save")) || (action.equals("saveexit"))) && (error.equals(""))){
      try {
        // first check if all plausibilization was ok

        cd.check(true);

        // unlock resource if save&exit was selected
        if (action.equals("saveexit")) {
          cd.setLockstate(-1);
        }

        // write the data to the database
        cd.write(cms);

      } catch (CmsPlausibilizationException plex) {
        // there was an error during plausibilization, so create an error text
        errorCodes=plex.getErrorCodes();

        //loop through all errors
        for (int i=0;i<errorCodes.size();i++) {
          errorCode=(String)errorCodes.elementAt(i);
          // try to get an error message that fits thos this error code exactly
          if (template.hasData(C_ERRPREFIX+errorCode)) {
            error+=template.getProcessedDataValue(C_ERRPREFIX+errorCode);
          } else {
            // now check if there is a general error message for this field
            errorField=errorCode.substring(0,errorCode.indexOf(A_CmsContentDefinition.C_ERRSPERATOR));
            if (template.hasData(C_ERRPREFIX+errorField)) {
              error+=template.getProcessedDataValue(C_ERRPREFIX+errorField);
            } else {
              // now check if there is at least a general error messace for the error type
              errorType=errorCode.substring(errorCode.indexOf(C_ERRSPERATOR)+1,errorCode.length());
              if (template.hasData(C_ERRPREFIX+errorType)) {
                error+=template.getProcessedDataValue(C_ERRPREFIX+errorType);
              } else {
                // no error dmessage was found, so generate a default one
                error+="["+errorCode+"]";
              }
            }
          }
        }

        //check if there is an introtext for the errors
        if (template.hasData("errormsg")) {
          error=template.getProcessedDataValue("errormsg")+error;
        }
        template.setData("error",error);

      } catch (Exception ex) {

        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
          A_OpenCms.log(C_OPENCMS_CRITICAL, getClassName() + "Error while saving data to Content Definition "+ex.toString());
        }
        throw new CmsException(ex.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, ex);
      }
    } //if (save || saveexit)

    // only exit the form if there was no error
    if (errorCodes== null) {
      // the exit or save&exit buttons were pressed, so return to the list
      if ((action.equals("exit")) || ((action.equals("saveexit")) && (error.equals("")))) {
        try {
          // cleanup session
          session.removeValue(this.getContentDefinitionClass().getName());
          session.removeValue("backofficepageselectorvector");
          session.removeValue("backofficepagetemplateselector");
          //do the redirect
          // to do: replace the getUri method with getPathInfo if aviable
          String uri=  cms.getRequestContext().getUri();
          uri = "/"+uri.substring(1,uri.lastIndexOf("/"));
          cms.getRequestContext().getResponse().sendCmsRedirect(uri);
          return null;
        } catch (java.io.IOException e) {
           if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
             A_OpenCms.log(C_OPENCMS_CRITICAL, getClassName() + "Error while doing redirect "+e.toString());
          }
          throw new CmsException(e.getMessage(), CmsException.C_UNKNOWN_EXCEPTION, e);
        }
      } // if (exit || saveexit)
    } // noerror
  } // if (action!=null)
  return startProcessing(cms,template,"",parameters,templateSelector);
 }


  /**
   * This methods collects all "getXYZ" methods of the actual content definition. This is used
   * to automatically preset the datablocks of the template with the values stored in the
   * content definition.
   * @param contentDefinition The actual Content Definition class
   * @returns Vector of get-methods
   */
  private Vector getGetMethods (A_CmsContentDefinition contentDefinition) {

    Vector getMethods=new Vector();
    //get all methods of the CD
    Method[] methods = this.getContentDefinitionClass().getMethods();
    for (int i=0;i<methods.length;i++) {
      Method m=methods[i];
      String name=m.getName().toLowerCase();

      //now extract all methods whose name starts with a "get"
      if (name.startsWith("get")) {
        Class[] param = m.getParameterTypes();
        //only take those methods that have no parameter and return a String
        if (param.length==0) {

          Class retType=m.getReturnType();
          if (retType.equals(java.lang.String.class)) {
           getMethods.addElement(m);
         }
        }
      }
     } // for
    return getMethods;
  } //getGetMethods

  /**
   * This methods collects all "setXYZ" methods of the actual content definition. This is used
   * to automatically insert the form parameters into the content definition.
   * @param contentDefinition The actual Content Definition class
   * @returns Hashtable of set-methods.
   */
  private Hashtable getSetMethods (A_CmsContentDefinition contentDefinition) {
    Hashtable setMethods=new Hashtable();
    //get all methods of the CD
    Method[] methods = this.getContentDefinitionClass().getMethods();
    for (int i=0;i<methods.length;i++) {
      Method m=methods[i];
      String name=m.getName().toLowerCase();
      //now extract all methods whose name starts with a "set"
      if (name.startsWith("set")) {

        Class[] param = m.getParameterTypes();
        //only take those methods that have a single string parameter and return nothing
        if (param.length==1) {
          // check if the parameter is a string
          if (param[0].equals(java.lang.String.class)) {
            Class retType=m.getReturnType();
            // the return value must be void
            if (retType.toString().equals("void")) {
             setMethods.put(m.getName().toLowerCase(),m);
           }
          }
        }
      }
     } // for
    return setMethods;
  } //getsetMethods


  /**
   * This method austomatically fills all datablocks in the template that fit to a special name scheme.
   * A datablock named "xyz" is filled with the value of a "getXYZ" method form the content defintion.
   * @param template The template to set the datablocks in.
   * @param contentDefinition The actual content defintion.
   * @methods A vector with all "getXYZ" methods to be used.
   * @exception Throws CmsException if something goes wrong.
   */
  private void setDatablocks(CmsXmlWpTemplateFile template,
                             A_CmsContentDefinition contentDefinition,
                             Vector methods) throws CmsException {
    String methodName="";
    String datablockName="";
    String value="";
    Method method;

    for (int i=0;i<methods.size();i++){
      // get the method name
      method=(Method)methods.elementAt(i);
      methodName=method.getName();
      //get the datablock name - the methodname without the leading "get"
      datablockName=(methodName.substring(3,methodName.length())).toLowerCase();
      //check if the datablock name ends with a "string" if so, remove it from the datablockname
      if (datablockName.endsWith("string")) {
        datablockName=datablockName.substring(0,datablockName.indexOf("string"));
      }
      //now call the method to get the value
      try {
        Object result=(Object)method.invoke(contentDefinition,new Object[] {});
        // check if the get method returns null (default value of the CD), set the datablock to
        // to "" then.
        if (result==null) {
          value="";
        } else {
          // cast the result to a string, only strings can be set into datablocks.
          value=result+"";
        }
        template.setData(datablockName,value);
      } catch (Exception e) {
        if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(C_OPENCMS_CRITICAL, getClassName() + "Error during automatic call method '"+methodName+"':"+e.toString());
        }
      } // try
    } //for
  } //setDatablocks


  /**
   * This method automatically fills the content definition with the values read from the template.
   * A method "setXYZ(value)" of the content defintion is automatically called and filled with the
   * value of a template input field named "xyz".
   * @param cms The current CmsObject.
   * @param contentDefintion The actual content defintion.
   * @param parameters A hashtable with all template and url parameters.
   * @param setMethods A hashtable with all "setXYZ" methods
   * @exception Throws CmsException if something goes wrong.
   */
  private void fillContentDefinition(CmsObject cms,A_CmsContentDefinition contentDefinition,
                                     Hashtable parameters,
                                     Hashtable setMethods)throws CmsException {

    Enumeration keys = parameters.keys();
    // loop through all values that were returned
    while(keys.hasMoreElements()) {
          String key=(String) keys.nextElement();
          String value=parameters.get(key).toString();
          // not check if this parameter might be an uploaded file
          boolean isFile=false;
          // loop through all uploaded files and check if one of those is equal to the value of
          // the tested parameter.
          Enumeration uploadedFiles=cms.getRequestContext().getRequest().getFileNames();
          byte[] content=cms.getRequestContext().getRequest().getFile(value);
          while (uploadedFiles.hasMoreElements()){
            String filename=(String) uploadedFiles.nextElement();
            // there is a match, so this parameter represents a fileupload.
            if (filename.equals(value)) {
              isFile=true;
            }
          }
          // try to find a set method for this parameter
          Method m=(Method)setMethods.get("set"+key);

          // there was no method found with this name. So try the name "setXYZString" as an alternative.
          if (m==null) {
            m=(Method)setMethods.get("set"+key+"string");

          }
          // there was a method found
          if (m!=null) {
            //now call the method to set the value
            try {
              m.invoke(contentDefinition,new Object[] {value});
              // this parameter was the name of an uploaded file. Now try to set the file content
              // too.
              if (isFile) {
                // get the name of the method. It should be "setXYZContent"
                String methodName=m.getName()+"Content";
                // now get the method itself.
                Method contentMethod=this.getContentDefinitionClass().getMethod(methodName, new Class[] {byte[].class});
                //finally invoke it.
                contentMethod.invoke(contentDefinition, new Object[] {content});
              }
            } catch (Exception e) {
              if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                A_OpenCms.log(C_OPENCMS_CRITICAL, getClassName() + "Error during automatic call method '"+m.getName()+"':"+e.toString());
              }
            } // try
          } //if
    } // while
  } //fillContentDefiniton


  /**
   * Checks how many template selectors are available in the backoffice template and selects the
   * correct one.
   * @param cms The current CmsObject.
   * @param template The current template.
   * @param parameters All template and URL parameters.
   * @param templateSelector The current template selector.
   * @returns The new template selector.
   * @exception Throws CmsException if something goes wrong.
   */
  private String getTemplateSelector(CmsObject cms, CmsXmlWpTemplateFile template,
                                     Hashtable parameters,String templateSelector) throws CmsException {
   CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
    //store the old templateSelector in the session
    if (templateSelector!=null) {
      session.putValue("backofficepagetemplateselector",templateSelector);
    }
    // Vector to store all template selectors in the template
    Vector selectors=new Vector();
    // Vector to store all useful template selectory in the template
    Vector filteredSelectors=new Vector();
    selectors=template.getAllSections();
    // loop through all templateseletors and find the useful ones
    for (int i=0;i<selectors.size();i++) {
      String selector=(String)selectors.elementAt(i);
      if ((!selector.equals(C_DEFAULT_SELECTOR)) && (!selector.equals(C_DONE_SELECTOR))) {
        filteredSelectors.addElement(selector);
      }
    }
    String selectedPage=(String)parameters.get("backofficepage");

    // check if there is more than one useful template selector. If not, do not display the selectbox.
    if (selectors.size()<4) {
      template.setData("BOSELECTOR","");
    }

    if (selectedPage!=null) {
      templateSelector=selectedPage;
    } else if (filteredSelectors.size()>0) {
      templateSelector=(String)filteredSelectors.elementAt(0);
      session.putValue("backofficepagetemplateselector",templateSelector);
    }
      session.putValue("backofficepageselectorvector",filteredSelectors);

    return templateSelector;
  }


}
