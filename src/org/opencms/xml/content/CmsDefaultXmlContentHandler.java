/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/xml/content/CmsDefaultXmlContentHandler.java,v $
 * Date   : $Date: 2004/12/09 16:45:54 $
 * Version: $Revision: 1.18 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2004 Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.xml.content;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsResource;
import org.opencms.i18n.CmsMessages;
import org.opencms.main.CmsException;
import org.opencms.main.I_CmsConstants;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsHtmlConverter;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.I_CmsStringMapper;
import org.opencms.workplace.xmlwidgets.I_CmsXmlWidget;
import org.opencms.xml.CmsXmlContentDefinition;
import org.opencms.xml.CmsXmlEntityResolver;
import org.opencms.xml.CmsXmlException;
import org.opencms.xml.types.I_CmsXmlContentValue;
import org.opencms.xml.types.I_CmsXmlSchemaType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.dom4j.Element;

/**
 * Default implementation for the XML content handler, will be used by all XML contents that do not
 * provide their own handler.<p>
 * 
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.18 $
 * @since 5.5.4
 */
public class CmsDefaultXmlContentHandler implements I_CmsXmlContentHandler {

    /** Constant for the "element" appinfo attribute name. */
    public static final String APPINFO_ATTR_ELEMENT = "element";

    /** Constant for the "mapto" appinfo attribute name. */
    public static final String APPINFO_ATTR_MAPTO = "mapto";

    /** Constant for the "message" appinfo attribute name. */
    public static final String APPINFO_ATTR_MESSAGE = "message";

    /** Constant for the "name" appinfo attribute name. */
    public static final String APPINFO_ATTR_NAME = "name";

    /** Constant for the "regex" appinfo attribute name. */
    public static final String APPINFO_ATTR_REGEX = "regex";

    /** Constant for the "type" appinfo attribute name. */
    public static final String APPINFO_ATTR_TYPE = "type";

    /** Constant for the "warning" appinfo attribute value. */
    public static final String APPINFO_ATTR_TYPE_WARNING = "warning";

    /** Constant for the "uri" appinfo attribute name. */
    public static final String APPINFO_ATTR_URI = "uri";

    /** Constant for the "value" appinfo attribute name. */
    public static final String APPINFO_ATTR_VALUE = "value";

    /** Constant for the "widget" appinfo attribute name. */
    public static final String APPINFO_ATTR_WIDGET = "widget";

    /** Constant for the "default" appinfo element name. */
    public static final String APPINFO_DEFAULT = "default";

    /** Constant for the "defaults" appinfo element name. */
    public static final String APPINFO_DEFAULTS = "defaults";

    /** Constant for the "layout" appinfo element name. */
    public static final String APPINFO_LAYOUT = "layout";

    /** Constant for the "layouts" appinfo element name. */
    public static final String APPINFO_LAYOUTS = "layouts";

    /** Constant for the "mapping" appinfo element name. */
    public static final String APPINFO_MAPPING = "mapping";

    /** Constant for the "mappings" appinfo element name. */
    public static final String APPINFO_MAPPINGS = "mappings";

    /** Constant for the "preview" appinfo element name. */
    public static final String APPINFO_PREVIEW = "preview";

    /** Constant for the "resourcebundle" appinfo element name. */
    public static final String APPINFO_RESOURCEBUNDLE = "resourcebundle";

    /** Constant for the "rule" appinfo element name. */
    public static final String APPINFO_RULE = "rule";

    /** Constant for the "validationrules" appinfo element name. */
    public static final String APPINFO_VALIDATIONRULES = "validationrules";

    /** Key prefix used to specify the value of a localized key as macro value. */ 
    public static final String KEY_LOCALIZED_PREFIX = "key:";
    
    /** Key used to specify the validation path as macro value. */
    public static final String KEY_VALIDATION_PATH = "validation:path";
    
    /** Key used to specify the validation regex as macro value. */
    public static final String KEY_VALIDATION_REGEX = "validation:regex";
    
    /** Key used to specifiy the validation value as macro value. */
    public static final String KEY_VALIDATION_VALUE = "validation:value";
    
    /** Key used to specify the current time as macro value. */
    public static final String KEY_CURRENT_TIME = "currenttime";
    
    /** Key used to specify the username of the current user as macro value. */
    public static final String KEY_CURRENT_USER_NAME = "currentuser:name";
    
    /** Key used to specify the first name of the current user as macro value. */
    public static final String KEY_CURRENT_USER_FIRSTNAME = "currentuser:firstname";
    
    /** Key used to specify the last name of the current user as macro value. */
    public static final String KEY_CURRENT_USER_LASTNAME = "currentuser:lastname";
    
    /** Key used to specify the full name of the current user as macro value. */
    public static final String KEY_CURRENT_USER_FULLNAME = "currentuser:fullname";
    
    /** Key used to specify the street of the current user as macro value. */
    public static final String KEY_CURRENT_USER_STREET = "currentuser:street";
    
    /** Key used to specify the zip code of the current user as macro value. */
    public static final String KEY_CURRENT_USER_ZIP = "currentuser:zip";
    
    /** Key used to specify the city of the current user as macro value. */
    public static final String KEY_CURRENT_USER_CITY = "currentuser:city";
    
    /** Key used to specify the email address of the current user as macro value. */
    public static final String KEY_CURRENT_USER_EMAIL = "currentuser:email";
    
    /** Key used to specify the request uri as macro value. */
    public static final String KEY_REQUEST_URI = "request:uri";
    
    /** Key used to specify the folder of the request uri as macro value. */
    public static final String KEY_REQUEST_FOLDER = "request:folder";
    
    /** Key used to specify the request encoding as macro value. */
    public static final String KEY_REQUEST_ENCODING = "request:encoding";
    
    /** Key user to specify the request locale as macro value. */
    public static final String KEY_REQUEST_LOCALE = "request:locale";
    
    /** Default message for validation warnings. */
    protected static final String MESSAGE_VALIDATION_DEFAULT_WARNING = 
        "${validation:path}: " +
        "${key:editor.xmlcontent.validation.warning.1}${validation:value}${key:editor.xmlcontent.validation.warning.2}" +
        "[${validation:regex}]";
    
    /** Default message for validation errors. */
    protected static final String MESSAGE_VALIDATION_DEFAULT_ERROR = 
        "${validation:path}: " +
        "${key:editor.xmlcontent.validation.error.1}${validation:value}${key:editor.xmlcontent.validation.error.2}" +
        "[${validation:regex}]";

    /** The default values for the elements (as defined in the annotations). */
    protected Map m_defaultValues;

    /** The element mappings (as defined in the annotations). */
    protected Map m_elementMappings;

    /** The widgets used for the elements (as defined in the annotations). */
    protected Map m_elementWidgets;

    /** The resource bundle name to be used for localization of this content handler. */
    protected String m_messageBundleName;

    /** The preview location (as defined in the annotations). */
    protected String m_previewLocation;

    /** The messages for the error rules. */
    protected Map m_validationErrorMessages;

    /** The validation rules that cause an error (as defined in the annotations). */
    protected Map m_validationErrorRules;

    /** The messages for the warning rules. */
    protected Map m_validationWarningMessages;

    /** The validation rules that cause a warning (as defined in the annotations). */
    protected Map m_validationWarningRules;

    /** The default macro string substitutions. */
    class XmlStringMapper implements I_CmsStringMapper {
        
        private Map m_values;
        
        private CmsObject m_cms;
        
        private Locale m_locale;
        
        /**
         * Create a new String mapper based on the given parameters.<p>
         * 
         * @param values map of additional values
         * @param locale locale to use
         * @param cms a cms object
         */
        public XmlStringMapper (Map values, Locale locale, CmsObject cms) {
            m_values = values;
            m_locale = locale;
            m_cms = cms;
        }
        
        /**
         * @see org.opencms.util.I_CmsStringMapper#getValue(java.lang.String)
         */
        public String getValue(String key) {

            if (KEY_CURRENT_TIME.equals(key)) { 
                return String.valueOf(System.currentTimeMillis());
            }
            
            if (KEY_CURRENT_USER_NAME.equals(key) && m_cms != null) {
                return m_cms.getRequestContext().currentUser().getName();
            }

            if (KEY_CURRENT_USER_FIRSTNAME.equals(key) && m_cms != null) {
                return m_cms.getRequestContext().currentUser().getFirstname();
            }
            
            if (KEY_CURRENT_USER_LASTNAME.equals(key) && m_cms != null) {
                return m_cms.getRequestContext().currentUser().getLastname();
            }
            
            if (KEY_CURRENT_USER_FULLNAME.equals(key) && m_cms != null) {
                return m_cms.getRequestContext().currentUser().getFullName();
            }
            
            if (KEY_CURRENT_USER_EMAIL.equals(key) && m_cms != null) {
                return m_cms.getRequestContext().currentUser().getEmail();
            }
            
            if (KEY_CURRENT_USER_STREET.equals(key) && m_cms != null) {
                return m_cms.getRequestContext().currentUser().getAddress();
            }      

            if (KEY_CURRENT_USER_ZIP.equals(key) && m_cms != null) {
                return (String)m_cms.getRequestContext().currentUser().getAdditionalInfo(I_CmsConstants.C_ADDITIONAL_INFO_ZIPCODE);
            }  
            
            if (KEY_CURRENT_USER_CITY.equals(key) && m_cms != null) {
                return (String)m_cms.getRequestContext().currentUser().getAdditionalInfo(I_CmsConstants.C_ADDITIONAL_INFO_TOWN);
            }  
            
            if (KEY_REQUEST_URI.equals(key) && m_cms != null) {
                return m_cms.getRequestContext().getUri();
            }
            
            if (KEY_REQUEST_FOLDER.equals(key) && m_cms != null) {
                return CmsResource.getParentFolder(m_cms.getRequestContext().getUri());
            }
            
            if (KEY_REQUEST_ENCODING.equals(key) && m_cms != null) {
                return m_cms.getRequestContext().getEncoding();
            }
            
            if (KEY_REQUEST_LOCALE.equals(key) && m_cms != null) {
                return m_cms.getRequestContext().getLocale().toString();
            }
            
            if (key.startsWith(KEY_LOCALIZED_PREFIX) && m_locale != null) {
                return key(key.substring(KEY_LOCALIZED_PREFIX.length()), m_locale);
            } 
            
            if (key.startsWith(KEY_LOCALIZED_PREFIX) && m_locale == null) {
                // leave macros for localized keys unchanged if no locale available
                return CmsStringUtil.C_MACRO_DELIMITER + CmsStringUtil.C_MACRO_START + key + CmsStringUtil.C_MACRO_END;
            } 
            
            if (m_values != null){
                return (String)m_values.get(key);
            }
            
            return null;
        }
    }
    
    /**
     * Creates a new instance of the default XML content handler.<p>  
     */
    public CmsDefaultXmlContentHandler() {

        init();
    }

    /**
     * @see org.opencms.xml.content.I_CmsXmlContentHandler#getDefault(org.opencms.file.CmsObject, org.opencms.xml.types.I_CmsXmlSchemaType, java.util.Locale)
     */
    public String getDefault(CmsObject cms, I_CmsXmlSchemaType type, Locale locale) {
      
        String defaultValue = (String)m_defaultValues.get(type.getElementName());
        if (defaultValue != null) {
            // return the string set in the appinfo with processed macros
            return CmsStringUtil.substituteMacros(defaultValue, new XmlStringMapper(null, locale, cms));
        }

        // default implementation currently just uses the "getDefault" mehod of the given value
        return type.getDefault(locale);
    }

    /**
     * Returns the mapping defined for the given element name.<p>
     * 
     * @param elementName the element name to use
     * @return the mapping defined for the given element name
     */
    public String getMapping(String elementName) {

        return (String)m_elementMappings.get(elementName);
    }

    /**
     * @see org.opencms.xml.content.I_CmsXmlContentHandler#getMessages(java.util.Locale)
     */
    public CmsMessages getMessages(Locale locale) {

        if (m_messageBundleName == null) {
            // no message bundle was initialized
            return null;
        }

        return new CmsMessages(m_messageBundleName, locale);
    }

    /**
     * @see org.opencms.xml.content.I_CmsXmlContentHandler#getPreview(org.opencms.file.CmsObject, org.opencms.xml.content.CmsXmlContent, java.lang.String)
     */
    public String getPreview(CmsObject cms, CmsXmlContent content, String resourcename) {

        return m_previewLocation;
    }

    /**
     * @see org.opencms.xml.content.I_CmsXmlContentHandler#getWidget(org.opencms.xml.types.I_CmsXmlContentValue)
     */
    public I_CmsXmlWidget getWidget(I_CmsXmlContentValue value) {

        // try the specific widget settings first
        I_CmsXmlWidget result = (I_CmsXmlWidget)m_elementWidgets.get(value.getElementName());
        if (result != null) {
            return result;
        }

        // use default widget mappings
        return OpenCms.getXmlContentTypeManager().getDefaultWidget(value.getTypeName());
    }

    /**
     * @see org.opencms.xml.content.I_CmsXmlContentHandler#initialize(org.dom4j.Element, org.opencms.xml.CmsXmlContentDefinition)
     */
    public synchronized void initialize(Element appInfoElement, CmsXmlContentDefinition contentDefinition)
    throws CmsXmlException {

        if (appInfoElement == null) {
            // no appinfo provided, so no mapping is required
            return;
        }

        // re-initialize the local variables
        init();

        Iterator i = appInfoElement.elements().iterator();
        while (i.hasNext()) {
            // iterate all elements in the appinfo node
            Element element = (Element)i.next();
            String nodeName = element.getName();
            if (nodeName.equals(APPINFO_MAPPINGS)) {
                initMappings(element, contentDefinition);
            } else if (nodeName.equals(APPINFO_LAYOUTS)) {
                initLayouts(element, contentDefinition);
            } else if (nodeName.equals(APPINFO_VALIDATIONRULES)) {
                initValidationRules(element, contentDefinition);
            } else if (nodeName.equals(APPINFO_DEFAULTS)) {
                initDefaultValues(element, contentDefinition);
            } else if (nodeName.equals(APPINFO_PREVIEW)) {
                initPreview(element, contentDefinition);
            } else if (nodeName.equals(APPINFO_RESOURCEBUNDLE)) {
                initResourceBundle(element, contentDefinition);
            }
        }
    }

    /**
     * @see org.opencms.xml.content.I_CmsXmlContentHandler#prepareForWrite(org.opencms.file.CmsObject, org.opencms.xml.content.CmsXmlContent, org.opencms.file.CmsFile)
     */
    public CmsFile prepareForWrite(CmsObject cms, CmsXmlContent content, CmsFile file) throws CmsException {

        // validate the xml structure before writing the file         
        // an exception will be thrown if the structure is invalid
        content.validateXmlStructure(new CmsXmlEntityResolver(cms));
        // read the content-conversion property
        String contentConversion = CmsHtmlConverter.getConversionSettings(cms, file);
        content.setConversion(contentConversion);
        // correct the HTML structure 
        file = content.correctXmlStructure(cms);
        content.setFile(file);
        // resolve the file mappings
        content.resolveMappings(cms);

        return file;
    }

    /**
     * @see org.opencms.xml.content.I_CmsXmlContentHandler#resolveMapping(org.opencms.file.CmsObject, org.opencms.xml.content.CmsXmlContent, org.opencms.xml.types.I_CmsXmlContentValue)
     */
    public void resolveMapping(CmsObject cms, CmsXmlContent content, I_CmsXmlContentValue value) throws CmsException {

        // TODO: this implememtation does not handle multiple mappings to a key,
        // e.g. it may be possible for several nested schema to map somehting to the "Title" property
        // in the current implementation the result (i.e. which mappings "wins") is undefined

        if (!value.isSimpleType()) {
            // no mappings for a nested schema are possible
            // note that the sub-elemenets of the nested schema ARE mapped by the node visitor,
            // it's just the nested schema value itself that does not support mapping
            return;
        }

        if (value.getIndex() > 1) {
            // this implementation currently just supports mapping of the first element 
            return;
        }

        // get the original VFS file from the content
        CmsFile file = content.getFile();
        if (file == null) {
            throw new CmsXmlException("File not available to resolve element mappings");
        }

        // get filename
        String filename = cms.getSitePath(content.getFile());

        // get the mapping for the element name
        String mapping = getMapping(value.getElementName());

        if (CmsStringUtil.isNotEmpty(mapping)) {

            // get the string value of the current node
            String stringValue = value.getStringValue(cms);

            if (mapping.startsWith(C_MAPTO_PROPERTY)) {

                // this is a property mapping
                String property = mapping.substring(C_MAPTO_PROPERTY.length());
                // just store the string value in the selected property
                cms.writePropertyObject(filename, new CmsProperty(property, stringValue, null));

            } else if (mapping.startsWith(C_MAPTO_ATTRIBUTE)) {

                // this is an attribute mapping                        
                String attribute = mapping.substring(C_MAPTO_ATTRIBUTE.length());
                switch (C_ATTRIBUTES_LIST.indexOf(attribute)) {
                    case 0: // datereleased
                        long date;
                        date = Long.valueOf(stringValue).longValue();
                        if (date == 0) {
                            date = CmsResource.DATE_RELEASED_DEFAULT;
                        }
                        file.setDateReleased(date);
                        break;
                    case 1: // dateexpired
                        date = Long.valueOf(stringValue).longValue();
                        if (date == 0) {
                            date = CmsResource.DATE_EXPIRED_DEFAULT;
                        }
                        file.setDateExpired(date);
                        break;
                    default: // TODO: handle invalid / other mappings                                
                }
            }
        }
    }

    /**
     * @see org.opencms.xml.content.I_CmsXmlContentHandler#resolveValidation(org.opencms.file.CmsObject, org.opencms.xml.types.I_CmsXmlContentValue, org.opencms.xml.content.CmsXmlContentErrorHandler)
     */
    public CmsXmlContentErrorHandler resolveValidation(
        CmsObject cms,
        I_CmsXmlContentValue value,
        CmsXmlContentErrorHandler errorHandler) {

        if (errorHandler == null) {
            // init a new error handler if required
            errorHandler = new CmsXmlContentErrorHandler();
        }

        if (!value.isSimpleType()) {
            // no validation for a nested schema is possible
            // note that the sub-elemenets of the nested schema ARE validated by the node visitor,
            // it's just the nested schema value itself that does not support validation
            return errorHandler;
        }

        // validate the error rules
        errorHandler = validateValue(cms, value, errorHandler, m_validationErrorRules, false);
        // validate the warning rules
        errorHandler = validateValue(cms, value, errorHandler, m_validationWarningRules, true);

        // return the result
        return errorHandler;
    }

    /**
     * Adds a default value for an element.<p>
     * 
     * @param contentDefinition the XML content definition this XML content handler belongs to
     * @param elementName the element name to map
     * @param defaultValue the default value to use
     * 
     * @throws CmsXmlException in case an unknown element name is used
     */
    protected void addDefault(CmsXmlContentDefinition contentDefinition, String elementName, String defaultValue)
    throws CmsXmlException {

        if (contentDefinition.getSchemaType(elementName) == null) {
            throw new CmsXmlException("Unregistered XML content type " + elementName + " used for default value");
        }

        m_defaultValues.put(elementName, defaultValue);
    }

    /**
     * Adds an element mapping.<p>
     * 
     * @param contentDefinition the XML content definition this XML content handler belongs to
     * @param elementName the element name to map
     * @param mapping the mapping to use
     * 
     * @throws CmsXmlException in case an unknown element name is used
     */
    protected void addMapping(CmsXmlContentDefinition contentDefinition, String elementName, String mapping)
    throws CmsXmlException {

        if (contentDefinition.getSchemaType(elementName) == null) {
            throw new CmsXmlException("Unregistered XML content type " + elementName + " used for mapping");
        }

        m_elementMappings.put(elementName, mapping);
    }

    /**
     * Adds a validation rule for a specified element.<p> 
     * 
     * @param contentDefinition the XML content definition this XML content handler belongs to
     * @param elementName the element name to add the rule to 
     * @param regex the validation rule regular expression
     * @param message the message in case validation fails (may be null)
     * @param isWarning if true, this rule is used for warnings, otherwise it's an error
     * 
     * @throws CmsXmlException in case an unknown element name is used
     */
    protected void addValidationRule(
        CmsXmlContentDefinition contentDefinition,
        String elementName,
        String regex,
        String message,
        boolean isWarning) throws CmsXmlException {

        if (contentDefinition.getSchemaType(elementName) == null) {
            throw new CmsXmlException("Unregistered XML content type " + elementName + " used for validation ruls");
        }

        if (isWarning) {
            m_validationWarningRules.put(elementName, regex);
            if (message != null) {
                m_validationWarningMessages.put(elementName, message);
            }
        } else {
            m_validationErrorRules.put(elementName, regex);
            if (message != null) {
                m_validationErrorMessages.put(elementName, message);
            }
        }
    }

    /**
     * Adds a GUI widget for a soecified element.<p> 
     * 
     * @param contentDefinition the XML content definition this XML content handler belongs to
     * @param elementName the element name to map
     * @param className the name of the widget class to use as GUI for the element
     * 
     * @throws CmsXmlException in case an unknown element name is used
     */
    protected void addWidget(CmsXmlContentDefinition contentDefinition, String elementName, String className)
    throws CmsXmlException {

        if (contentDefinition.getSchemaType(elementName) == null) {
            throw new CmsXmlException("Unregistered XML content type " + elementName + " used for layout widget");
        }

        I_CmsXmlWidget widget = OpenCms.getXmlContentTypeManager().getWidget(className);

        if (widget == null) {
            throw new CmsXmlException("Unregistered XML widget '"
                + className
                + "' configured as GUI for element "
                + elementName
                + " in content definition "
                + contentDefinition.getSchemaLocation());
        }
        m_elementWidgets.put(elementName, widget);
    }

    /**
     * Returns the validation message to be displayed if a certain rule was violated.<p> 
     * 
     * @param cms the current users OpenCms context
     * @param value the value to validate
     * @param regex the rule that was vialoted
     * @param valueStr the string value of the given value
     * @param matchResult if false, the rule was negated
     * @param isWarning if true, this validation indicate a warning, otherwise an error
     * 
     * @return the validation message to be displayed 
     */
    protected String getValidationMessage(
        CmsObject cms,
        I_CmsXmlContentValue value,
        String regex,
        String valueStr,
        boolean matchResult,
        boolean isWarning) {

        String message = null;
        if (isWarning) {
            message = (String)m_validationWarningMessages.get(value.getElementName());
        } else {
            message = (String)m_validationErrorMessages.get(value.getElementName());
        }

        if (message == null) {
            if (isWarning) {
                message = MESSAGE_VALIDATION_DEFAULT_WARNING;    
            } else {
                message = MESSAGE_VALIDATION_DEFAULT_ERROR;
            }
        }    

        Map additionalValues = new HashMap();
        additionalValues.put(KEY_VALIDATION_VALUE, valueStr);
        additionalValues.put(KEY_VALIDATION_REGEX, ((!matchResult)?"!":"") + regex);
        additionalValues.put(KEY_VALIDATION_PATH, value.getPath());
        
        return CmsStringUtil.substituteMacros(message, new XmlStringMapper(additionalValues, null, cms));
    }

    /**
     * Called when this content handler is initialized.<p> 
     */
    protected void init() {

        m_elementMappings = new HashMap();
        m_elementWidgets = new HashMap();
        m_validationErrorRules = new HashMap();
        m_validationErrorMessages = new HashMap();
        m_validationWarningRules = new HashMap();
        m_validationWarningMessages = new HashMap();
        m_defaultValues = new HashMap();
        m_previewLocation = null;
    }

    /**
     * Initializes the default values for this content handler.<p>
     * 
     * Using the default values from the appinfo node, it's possible to have more 
     * sophisticated logic for generating the defaults then just using the XML schema "default"
     * attribute.<p> 
     * 
     * @param root the "defaultvalues" element from the appinfo node of the XML content definition
     * @param contentDefinition the content definition the validation rules belong to
     * @throws CmsXmlException if something goes wrong
     */
    protected void initDefaultValues(Element root, CmsXmlContentDefinition contentDefinition) throws CmsXmlException {

        Iterator i = root.elementIterator(APPINFO_DEFAULT);
        while (i.hasNext()) {
            // iterate all "default" elements in the "defaults" node
            Element element = (Element)i.next();
            String elementName = element.attributeValue(APPINFO_ATTR_ELEMENT);
            String defaultValue = element.attributeValue(APPINFO_ATTR_VALUE);
            if ((elementName != null) && (defaultValue != null)) {
                // add a widget mapping for the element
                addDefault(contentDefinition, elementName, defaultValue);
            }
        }
    }

    /**
     * Initializes the layout for this content handler.<p>
     * 
     * Unless otherwise instructed, the editor uses one specific GUI widget for each 
     * XML value schema type. For example, for a {@link org.opencms.xml.types.CmsXmlStringValue} 
     * the default widget is the {@link org.opencms.workplace.xmlwidgets.CmsXmlStringWidget}.
     * However, certain values can also use more then one widget, for example you may 
     * also use a {@link org.opencms.workplace.xmlwidgets.CmsXmlBooleanWidget} for a String value,
     * and as a result the Strings possible values would be eithe "false" or "true",
     * bit nevertheless be a String.<p>
     * 
     * @param root the "layouts" element from the appinfo node of the XML content definition
     * @param contentDefinition the content definition the layout belings to
     * @throws CmsXmlException if something goes wrong
     */
    protected void initLayouts(Element root, CmsXmlContentDefinition contentDefinition) throws CmsXmlException {

        Iterator i = root.elementIterator(APPINFO_LAYOUT);
        while (i.hasNext()) {
            // iterate all "layout" elements in the "layouts" node
            Element element = (Element)i.next();
            String elementName = element.attributeValue(APPINFO_ATTR_ELEMENT);
            String widgetClass = element.attributeValue(APPINFO_ATTR_WIDGET);
            if ((elementName != null) && (widgetClass != null)) {
                // add a widget mapping for the element
                addWidget(contentDefinition, elementName, widgetClass);
            }
        }
    }

    /**
     * Initializes the element mappings for this content handler.<p>
     * 
     * Element mappings allow storing values from the XML content in other locations.
     * For example, if you have an elemenet called "Title", it's likley a good idea to 
     * store the value of this element also in the "Title" property of a XML content resource.<p>
     * 
     * @param root the "mappings" element from the appinfo node of the XML content definition
     * @param contentDefinition the content definition the mappings belong to
     * @throws CmsXmlException if something goes wrong
     */
    protected void initMappings(Element root, CmsXmlContentDefinition contentDefinition) throws CmsXmlException {

        Iterator i = root.elementIterator(APPINFO_MAPPING);
        while (i.hasNext()) {
            // iterate all "mapping" elements in the "mappings" node
            Element element = (Element)i.next();
            // this is a mapping node
            String elementName = element.attributeValue(APPINFO_ATTR_ELEMENT);
            String maptoName = element.attributeValue(APPINFO_ATTR_MAPTO);
            if ((elementName != null) && (maptoName != null)) {
                // add the element mapping 
                addMapping(contentDefinition, elementName, maptoName);
            }
        }
    }

    /**
     * Initializes the preview location for this content handler.<p>
     * 
     * @param root the "preview" element from the appinfo node of the XML content definition
     * @param contentDefinition the content definition the validation rules belong to
     * @throws CmsXmlException if something goes wrong
     */
    protected void initPreview(Element root, CmsXmlContentDefinition contentDefinition) throws CmsXmlException {

        String preview = root.attributeValue(APPINFO_ATTR_URI);
        if (preview == null) {
            throw new CmsXmlException("Missing preview uri for element "
                + root.getName()
                + " in content definition "
                + contentDefinition.getSchemaLocation());
        }
        m_previewLocation = preview;
    }

    /**
     * Initializes the resource bundle to use for localized messages in this content handler.<p>
     * 
     * @param root the "resourcebundle" element from the appinfo node of the XML content definition
     * @param contentDefinition the content definition the validation rules belong to
     * 
     * @throws CmsXmlException if something goes wrong
     */
    protected void initResourceBundle(Element root, CmsXmlContentDefinition contentDefinition) throws CmsXmlException {

        String name = root.attributeValue(APPINFO_ATTR_NAME);
        if (name == null) {
            throw new CmsXmlException("Missing resource bundle name for element "
                + root.getName()
                + " in content definition "
                + contentDefinition.getSchemaLocation());
        }
        m_messageBundleName = name;
    }

    /**
     * Initializes the validation rules this content handler.<p>
     * 
     * OpenCms always performs XML schema validation for all XML contents. However,
     * for most projects in the real world a more fine-grained control over the validation process is
     * required. For these cases, individual validation rules can be defined for the appinfo node.<p>
     * 
     * @param root the "validationrules" element from the appinfo node of the XML content definition
     * @param contentDefinition the content definition the validation rules belong to
     * @throws CmsXmlException if something goes wrong
     */
    protected void initValidationRules(Element root, CmsXmlContentDefinition contentDefinition) throws CmsXmlException {

        Iterator i = root.elementIterator(APPINFO_RULE);
        while (i.hasNext()) {
            // iterate all "layout" elements in the "layouts" node
            Element element = (Element)i.next();
            String elementName = element.attributeValue(APPINFO_ATTR_ELEMENT);
            String regex = element.attributeValue(APPINFO_ATTR_REGEX);
            String type = element.attributeValue(APPINFO_ATTR_TYPE);
            String message = element.attributeValue(APPINFO_ATTR_MESSAGE);
            if ((elementName != null) && (regex != null)) {
                // add a validation ruls for the element
                addValidationRule(contentDefinition, elementName, regex, message, APPINFO_ATTR_TYPE_WARNING
                    .equals(type));
            }
        }
    }

    /**
     * Returns the localized resource string for a given message key according to the configured resource bundle
     * of this content handler.<p>
     * 
     * If the key was not found in the configuredd bundle, or no bundle is configured for this 
     * content handler, the return value is
     * <code>"??? " + keyName + " ???"</code>.<p>
     * 
     * @param keyName the key for the desired string 
     * @param locale the locale to get the key from
     * 
     * @return the resource string for the given key 
     */
    protected String key(String keyName, Locale locale) {

        CmsMessages messages = getMessages(locale);
        if (messages != null) {
            return messages.key(keyName);
        }
        return CmsMessages.formatUnknownKey(keyName);
    }

    /**
     * Validates the given rules against the given value.<p> 
     * 
     * @param cms the current users OpenCms context
     * @param value the value to validate
     * @param errorHandler the error handler to use in case errors or warnings are detected
     * @param rules the rules to validate the value against
     * @param isWarning if true, this validation should be stored as a warning, otherwise as an error
     * 
     * @return the updated error handler
     */
    protected CmsXmlContentErrorHandler validateValue(
        CmsObject cms,
        I_CmsXmlContentValue value,
        CmsXmlContentErrorHandler errorHandler,
        Map rules,
        boolean isWarning) {

        String valueStr;
        try {
            valueStr = value.getStringValue(cms);
        } catch (Exception e) {
            // if the value can not be accessed it's useless to continue
            errorHandler.addError(value, e.getMessage());
            return errorHandler;
        }

        String regex = (String)rules.get(value.getElementName());
        if (regex == null) {
            // no customized rule, check default XML schema validation rules
            return validateValue(cms, value, valueStr, errorHandler, isWarning);
        }

        boolean matchResult = true;
        if (regex.charAt(0) == '!') {
            // negate the pattern
            matchResult = false;
            regex = regex.substring(1);
        }

        // use the custom validation pattern
        if (matchResult != Pattern.matches(regex, valueStr)) {
            // generate the message
            String message = getValidationMessage(cms, value, regex, valueStr, matchResult, isWarning);
            if (isWarning) {
                errorHandler.addWarning(value, message);
            } else {
                errorHandler.addError(value, message);
                // if an error was found, the default XML schema validation is not applied
                return errorHandler;
            }
        }

        // no error found, check default XML schema validation rules
        return validateValue(cms, value, valueStr, errorHandler, isWarning);
    }

    /**
     * Checks the default XML schema vaildation rules.<p>
     * 
     * These rules should only be tested if this is not a test for warnings.<p>
     * 
     * @param cms the current users OpenCms context
     * @param value the value to validate
     * @param valueStr the string value of the given value
     * @param errorHandler the error handler to use in case errors or warnings are detected
     * @param isWarning if true, this validation should be stored as a warning, otherwise as an error
     * 
     * @return the updated error handler
     */
    protected CmsXmlContentErrorHandler validateValue(
        CmsObject cms,
        I_CmsXmlContentValue value,
        String valueStr,
        CmsXmlContentErrorHandler errorHandler,
        boolean isWarning) {

        if (isWarning) {
            // default schema validation only applies to errors
            return errorHandler;
        }

        if (!value.validateValue(valueStr)) {
            // value is not valid, add an error to the handler
            String message = getValidationMessage(cms, value, value.getTypeName(), valueStr, true, false);
            errorHandler.addError(value, message);
        }

        return errorHandler;
    }
}