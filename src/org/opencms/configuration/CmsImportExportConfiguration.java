/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/configuration/CmsImportExportConfiguration.java,v $
 * Date   : $Date: 2006/08/24 06:43:23 $
 * Version: $Revision: 1.25.4.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
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
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.configuration;

import org.opencms.importexport.CmsImportExportManager;
import org.opencms.importexport.I_CmsImportExportHandler;
import org.opencms.main.CmsLog;
import org.opencms.security.I_CmsPrincipal;
import org.opencms.staticexport.CmsStaticExportExportRule;
import org.opencms.staticexport.CmsStaticExportManager;
import org.opencms.staticexport.CmsStaticExportRfsRule;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.digester.Digester;

import org.dom4j.Element;

/**
 * Import / export master configuration class.<p>
 * 
 * @author Alexander Kandzior 
 * 
 * @version $Revision: 1.25.4.1 $
 * 
 * @since 6.0.0
 */
public class CmsImportExportConfiguration extends A_CmsXmlConfiguration implements I_CmsXmlConfiguration {

    /** The name of the DTD for this configuration. */
    public static final String CONFIGURATION_DTD_NAME = "opencms-importexport.dtd";

    /** The name of the default XML file for this configuration. */
    public static final String DEFAULT_XML_FILE_NAME = "opencms-importexport.xml";

    /** Node that indicates page conversion. */
    protected static final String N_CONVERT = "convert";

    /** Node that contains a list of properties ignored during import. */
    protected static final String N_IGNOREDPROPERTIES = "ignoredproperties";

    /** The import immutable resources node. */
    protected static final String N_IMMUTABLES = "immutables";

    /** The node name of the import subconfiguration. */
    protected static final String N_IMPORT = "import";

    /** The main configuration node name. */
    protected static final String N_IMPORTEXPORT = "importexport";

    /** The node name of an individual import/export handler. */
    protected static final String N_IMPORTEXPORTHANDLER = "importexporthandler";

    /** Master node for import/export handlers. */
    protected static final String N_IMPORTEXPORTHANDLERS = "importexporthandlers";

    /** The node name of an individual import version class. */
    protected static final String N_IMPORTVERSION = "importversion";

    /** Master node for import version class names. */
    protected static final String N_IMPORTVERSIONS = "importversions";

    /** Node the contains an optional URL of old web application. */
    protected static final String N_OLDWEBAPPURL = "oldwebappurl";

    /** The import overwrite node name. */
    protected static final String N_OVERWRITE = "overwrite";

    /** An individual principal translation node. */
    protected static final String N_PRINCIPALTRANSLATION = "principaltranslation";

    /** The principal translation node. */
    protected static final String N_PRINCIPALTRANSLATIONS = "principaltranslations";

    /**  The main configuration node for static export name. */
    protected static final String N_STATICEXPORT = "staticexport";

    /**  The node name of the static export acceptcharset node. */
    protected static final String N_STATICEXPORT_ACCEPTCHARSET = "acceptcharset";

    /**  The node name of the static export acceptlanguage node. */
    protected static final String N_STATICEXPORT_ACCEPTLANGUAGE = "acceptlanguage";

    /**  The node name of the static export default node. */
    protected static final String N_STATICEXPORT_DEFAULT = "defaultpropertyvalue";

    /**  The node name of the static export defualtsuffix node. */
    protected static final String N_STATICEXPORT_DEFAULTSUFFIXES = "defaultsuffixes";

    /**  The node name of the static export rule description nodes. */
    protected static final String N_STATICEXPORT_DESCRIPTION = "description";

    /**  The node name of the static export export-rule export node. */
    protected static final String N_STATICEXPORT_EXPORT = "export-resources";

    /**  The node name of the static export exportheaders node. */
    protected static final String N_STATICEXPORT_EXPORTHEADERS = "exportheaders";

    /**  The node name of the static export exportpath node. */
    protected static final String N_STATICEXPORT_EXPORTPATH = "exportpath";

    /**  The node name of the static export export-rule node. */
    protected static final String N_STATICEXPORT_EXPORTRULE = "export-rule";

    /**  The node name of the static export export-rules node. */
    protected static final String N_STATICEXPORT_EXPORTRULES = "export-rules";

    /**  The node name of the static export exporturl node. */
    protected static final String N_STATICEXPORT_EXPORTURL = "exporturl";

    /**  The node name of the static export handler node. */
    protected static final String N_STATICEXPORT_HANDLER = "staticexporthandler";

    /**  The node name of the static export header node. */
    protected static final String N_STATICEXPORT_HEADER = "header";

    /**  The node name of the static export export-rule modified node. */
    protected static final String N_STATICEXPORT_MODIFIED = "modified-resources";

    /**  The node name of the static export rule name nodes. */
    protected static final String N_STATICEXPORT_NAME = "name";

    /**  The node name of the static export plainoptimization node. */
    protected static final String N_STATICEXPORT_PLAINOPTIMIZATION = "plainoptimization";

    /**  The node name of the static export regex node. */
    protected static final String N_STATICEXPORT_REGEX = "regex";

    /**  The node name of the static export related-system-res node. */
    protected static final String N_STATICEXPORT_RELATED_SYSTEM_RES = "related-system-res";

    /**  The node name of the static export relativelinks node. */
    protected static final String N_STATICEXPORT_RELATIVELINKS = "userelativelinks";

    /**  The node name of the static export remoteaddr node. */
    protected static final String N_STATICEXPORT_REMOTEADDR = "remoteaddr";

    /**  The node name of the static export rendersettings node. */
    protected static final String N_STATICEXPORT_RENDERSETTINGS = "rendersettings";

    /**  The node name of the static export requestheaders node. */
    protected static final String N_STATICEXPORT_REQUESTHEADERS = "requestheaders";

    /**  The node name of the static export resourcestorender node. */
    protected static final String N_STATICEXPORT_RESOURCESTORENDER = "resourcestorender";

    /**  The node name of the static export rfx-prefix node. */
    protected static final String N_STATICEXPORT_RFS_PREFIX = "rfs-prefix";

    /**  The node name of the static export rfx-rule node. */
    protected static final String N_STATICEXPORT_RFS_RULE = "rfs-rule";

    /**  The node name of the static export rfx-rules node. */
    protected static final String N_STATICEXPORT_RFS_RULES = "rfs-rules";

    /**  The node name of the static export rfx-rule source node. */
    protected static final String N_STATICEXPORT_SOURCE = "source";

    /**  The node name of the static export suffix node. */
    protected static final String N_STATICEXPORT_SUFFIX = "suffix";

    /**  The node name of the static export testresource node. */
    protected static final String N_STATICEXPORT_TESTRESOURCE = "testresource";

    /**  The node name of the static export export-rule export uri node. */
    protected static final String N_STATICEXPORT_URI = "uri";

    /**  The node name of the static export vfx-prefix node. */
    protected static final String N_STATICEXPORT_VFS_PREFIX = "vfs-prefix";

    /** The configured import/export manager. */
    private CmsImportExportManager m_importExportManager;

    /** The configured static export manager. */
    private CmsStaticExportManager m_staticExportManager;

    /**
     * Public constructor, will be called by configuration manager.<p> 
     */
    public CmsImportExportConfiguration() {

        setXmlFileName(DEFAULT_XML_FILE_NAME);
        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_IMPORT_CONFIG_INIT_0));
        }
    }

    /**
     * @see org.opencms.configuration.I_CmsXmlConfiguration#addXmlDigesterRules(org.apache.commons.digester.Digester)
     */
    public void addXmlDigesterRules(Digester digester) {

        // add finish rule
        digester.addCallMethod("*/" + N_IMPORTEXPORT, "initializeFinished");

        // creation of the import/export manager        
        digester.addObjectCreate("*/" + N_IMPORTEXPORT, CmsImportExportManager.class);
        // import/export manager finished
        digester.addSetNext("*/" + N_IMPORTEXPORT, "setImportExportManager");

        // add rules for import/export handlers
        digester.addObjectCreate(
            "*/" + N_IMPORTEXPORT + "/" + N_IMPORTEXPORTHANDLERS + "/" + N_IMPORTEXPORTHANDLER,
            A_CLASS,
            CmsConfigurationException.class);
        digester.addSetNext(
            "*/" + N_IMPORTEXPORT + "/" + N_IMPORTEXPORTHANDLERS + "/" + N_IMPORTEXPORTHANDLER,
            "addImportExportHandler");

        // overwrite rule
        digester.addCallMethod(
            "*/" + N_IMPORTEXPORT + "/" + N_IMPORT + "/" + N_OVERWRITE,
            "setOverwriteCollidingResources",
            0);

        // convert rule
        digester.addCallMethod("*/" + N_IMPORTEXPORT + "/" + N_IMPORT + "/" + N_CONVERT, "setConvertToXmlPage", 0);

        // old webapp rule
        digester.addCallMethod("*/" + N_IMPORTEXPORT + "/" + N_IMPORT + "/" + N_OLDWEBAPPURL, "setOldWebAppUrl", 0);

        // add rules for the import versions
        digester.addObjectCreate("*/"
            + N_IMPORTEXPORT
            + "/"
            + N_IMPORT
            + "/"
            + N_IMPORTVERSIONS
            + "/"
            + N_IMPORTVERSION, A_CLASS, CmsConfigurationException.class);
        digester.addSetNext(
            "*/" + N_IMPORTEXPORT + "/" + N_IMPORT + "/" + N_IMPORTVERSIONS + "/" + N_IMPORTVERSION,
            "addImportVersionClass");

        // add rules for the import immutables
        digester.addCallMethod(
            "*/" + N_IMPORTEXPORT + "/" + N_IMPORT + "/" + N_IMMUTABLES + "/" + N_RESOURCE,
            "addImmutableResource",
            1);
        digester.addCallParam("*/" + N_IMPORTEXPORT + "/" + N_IMPORT + "/" + N_IMMUTABLES + "/" + N_RESOURCE, 0, A_URI);

        // add rules for the import princial translations
        digester.addCallMethod("*/"
            + N_IMPORTEXPORT
            + "/"
            + N_IMPORT
            + "/"
            + N_PRINCIPALTRANSLATIONS
            + "/"
            + N_PRINCIPALTRANSLATION, "addImportPrincipalTranslation", 3);
        digester.addCallParam("*/"
            + N_IMPORTEXPORT
            + "/"
            + N_IMPORT
            + "/"
            + N_PRINCIPALTRANSLATIONS
            + "/"
            + N_PRINCIPALTRANSLATION, 0, A_TYPE);
        digester.addCallParam("*/"
            + N_IMPORTEXPORT
            + "/"
            + N_IMPORT
            + "/"
            + N_PRINCIPALTRANSLATIONS
            + "/"
            + N_PRINCIPALTRANSLATION, 1, A_FROM);
        digester.addCallParam("*/"
            + N_IMPORTEXPORT
            + "/"
            + N_IMPORT
            + "/"
            + N_PRINCIPALTRANSLATIONS
            + "/"
            + N_PRINCIPALTRANSLATION, 2, A_TO);

        // add rules for the ignored properties
        digester.addCallMethod(
            "*/" + N_IMPORTEXPORT + "/" + N_IMPORT + "/" + N_IGNOREDPROPERTIES + "/" + N_PROPERTY,
            "addIgnoredProperty",
            1);
        digester.addCallParam(
            "*/" + N_IMPORTEXPORT + "/" + N_IMPORT + "/" + N_IGNOREDPROPERTIES + "/" + N_PROPERTY,
            0,
            A_NAME);

        // creation of the static export manager        
        digester.addObjectCreate("*/" + N_STATICEXPORT, CmsStaticExportManager.class);
        // static export manager finished
        digester.addSetNext("*/" + N_STATICEXPORT, "setStaticExportManager");
        // export enabled role
        digester.addCallMethod("*/" + N_STATICEXPORT, "setExportEnabled", 1);
        digester.addCallParam("*/" + N_STATICEXPORT, 0, A_ENABLED);
        // mode rule
        digester.addCallMethod("*/" + N_STATICEXPORT + "/" + N_STATICEXPORT_HANDLER, "setHandler", 0);
        // exportpath rule
        digester.addCallMethod("*/" + N_STATICEXPORT + "/" + N_STATICEXPORT_EXPORTPATH, "setExportPath", 0);
        // default property rule
        digester.addCallMethod("*/" + N_STATICEXPORT + "/" + N_STATICEXPORT_DEFAULT, "setDefault", 0);
        // export suffix rule
        digester.addCallMethod("*/"
            + N_STATICEXPORT
            + "/"
            + N_STATICEXPORT_DEFAULTSUFFIXES
            + "/"
            + N_STATICEXPORT_SUFFIX, "setExportSuffix", 1);
        digester.addCallParam("*/"
            + N_STATICEXPORT
            + "/"
            + N_STATICEXPORT_DEFAULTSUFFIXES
            + "/"
            + N_STATICEXPORT_SUFFIX, 0, A_KEY);
        // header rule
        digester.addCallMethod(
            "*/" + N_STATICEXPORT + "/" + N_STATICEXPORT_EXPORTHEADERS + "/" + N_STATICEXPORT_HEADER,
            "setExportHeader",
            0);
        // accept-language rule
        digester.addCallMethod("*/"
            + N_STATICEXPORT
            + "/"
            + N_STATICEXPORT_REQUESTHEADERS
            + "/"
            + N_STATICEXPORT_ACCEPTLANGUAGE, "setAcceptLanguageHeader", 0);
        // accept-charset rule
        digester.addCallMethod("*/"
            + N_STATICEXPORT
            + "/"
            + N_STATICEXPORT_REQUESTHEADERS
            + "/"
            + N_STATICEXPORT_ACCEPTCHARSET, "setAcceptCharsetHeader", 0);
        // accept-charset rule
        digester.addCallMethod("*/"
            + N_STATICEXPORT
            + "/"
            + N_STATICEXPORT_REQUESTHEADERS
            + "/"
            + N_STATICEXPORT_REMOTEADDR, "setRemoteAddr", 0);
        // rfs-prefix rule
        digester.addCallMethod("*/"
            + N_STATICEXPORT
            + "/"
            + N_STATICEXPORT_RENDERSETTINGS
            + "/"
            + N_STATICEXPORT_RFS_PREFIX, "setRfsPrefix", 0);
        // vfs-prefix rule
        digester.addCallMethod("*/"
            + N_STATICEXPORT
            + "/"
            + N_STATICEXPORT_RENDERSETTINGS
            + "/"
            + N_STATICEXPORT_VFS_PREFIX, "setVfsPrefix", 0);
        // relative links rule
        digester.addCallMethod("*/"
            + N_STATICEXPORT
            + "/"
            + N_STATICEXPORT_RENDERSETTINGS
            + "/"
            + N_STATICEXPORT_RELATIVELINKS, "setRelativeLinks", 0);
        // exporturl rule
        digester.addCallMethod("*/"
            + N_STATICEXPORT
            + "/"
            + N_STATICEXPORT_RENDERSETTINGS
            + "/"
            + N_STATICEXPORT_EXPORTURL, "setExportUrl", 0);
        // plain export optimization rule
        digester.addCallMethod("*/"
            + N_STATICEXPORT
            + "/"
            + N_STATICEXPORT_RENDERSETTINGS
            + "/"
            + N_STATICEXPORT_PLAINOPTIMIZATION, "setPlainExportOptimization", 0);
        // test resource rule
        digester.addCallMethod("*/"
            + N_STATICEXPORT
            + "/"
            + N_STATICEXPORT_RENDERSETTINGS
            + "/"
            + N_STATICEXPORT_TESTRESOURCE, "setTestResource", 1);
        digester.addCallParam("*/"
            + N_STATICEXPORT
            + "/"
            + N_STATICEXPORT_RENDERSETTINGS
            + "/"
            + N_STATICEXPORT_TESTRESOURCE, 0, A_URI);
        // resources to export rule
        digester.addCallMethod("*/"
            + N_STATICEXPORT
            + "/"
            + N_STATICEXPORT_RENDERSETTINGS
            + "/"
            + N_STATICEXPORT_RESOURCESTORENDER
            + "/"
            + N_STATICEXPORT_REGEX, "setExportFolderPattern", 0);

        // export-rules configuration
        String exportRulePath = "*/"
            + N_STATICEXPORT
            + "/"
            + N_STATICEXPORT_RENDERSETTINGS
            + "/"
            + N_STATICEXPORT_RESOURCESTORENDER
            + "/"
            + N_STATICEXPORT_EXPORTRULES
            + "/"
            + N_STATICEXPORT_EXPORTRULE;
        digester.addCallMethod(exportRulePath, "addExportRule", 2);
        digester.addCallParam(exportRulePath + "/" + N_STATICEXPORT_NAME, 0);
        digester.addCallParam(exportRulePath + "/" + N_STATICEXPORT_DESCRIPTION, 1);
        digester.addCallMethod(
            exportRulePath + "/" + N_STATICEXPORT_MODIFIED + "/" + N_STATICEXPORT_REGEX,
            "addExportRuleRegex",
            1);
        digester.addCallParam(exportRulePath + "/" + N_STATICEXPORT_MODIFIED + "/" + N_STATICEXPORT_REGEX, 0);
        digester.addCallMethod(
            exportRulePath + "/" + N_STATICEXPORT_EXPORT + "/" + N_STATICEXPORT_URI,
            "addExportRuleUri",
            1);
        digester.addCallParam(exportRulePath + "/" + N_STATICEXPORT_EXPORT + "/" + N_STATICEXPORT_URI, 0);

        // rfs-rules configuration
        String rfsRulePath = "*/"
            + N_STATICEXPORT
            + "/"
            + N_STATICEXPORT_RENDERSETTINGS
            + "/"
            + N_STATICEXPORT_RFS_RULES
            + "/"
            + N_STATICEXPORT_RFS_RULE;
        digester.addCallMethod(rfsRulePath, "addRfsRule", 6);
        digester.addCallParam(rfsRulePath + "/" + N_STATICEXPORT_NAME, 0);
        digester.addCallParam(rfsRulePath + "/" + N_STATICEXPORT_DESCRIPTION, 1);
        digester.addCallParam(rfsRulePath + "/" + N_STATICEXPORT_SOURCE, 2);
        digester.addCallParam(rfsRulePath + "/" + N_STATICEXPORT_RFS_PREFIX, 3);
        digester.addCallParam(rfsRulePath + "/" + N_STATICEXPORT_EXPORTPATH, 4);
        digester.addCallParam(rfsRulePath + "/" + N_STATICEXPORT_RELATIVELINKS, 5);
        // rfs-rule related system resources
        digester.addCallMethod(
            rfsRulePath + "/" + N_STATICEXPORT_RELATED_SYSTEM_RES + "/" + N_STATICEXPORT_REGEX,
            "addRfsRuleSystemRes",
            1);
        digester.addCallParam(rfsRulePath + "/" + N_STATICEXPORT_RELATED_SYSTEM_RES + "/" + N_STATICEXPORT_REGEX, 0);
    }

    /**
     * @see org.opencms.configuration.I_CmsXmlConfiguration#generateXml(org.dom4j.Element)
     */
    public Element generateXml(Element parent) {

        // generate import/export node and subnodes
        Element importexportElement = parent.addElement(N_IMPORTEXPORT);

        Element resourceloadersElement = importexportElement.addElement(N_IMPORTEXPORTHANDLERS);
        List handlers = m_importExportManager.getImportExportHandlers();
        Iterator i = handlers.iterator();
        while (i.hasNext()) {
            I_CmsImportExportHandler handler = (I_CmsImportExportHandler)i.next();
            // add the handler node
            Element loaderNode = resourceloadersElement.addElement(N_IMPORTEXPORTHANDLER);
            loaderNode.addAttribute(A_CLASS, handler.getClass().getName());
        }

        Element importElement = importexportElement.addElement(N_IMPORT);

        // <overwrite> node
        importElement.addElement(N_OVERWRITE).setText(
            String.valueOf(m_importExportManager.overwriteCollidingResources()));

        // <convert> node
        importElement.addElement(N_CONVERT).setText(String.valueOf(m_importExportManager.convertToXmlPage()));

        // <oldwebappurl> node
        if (m_importExportManager.getOldWebAppUrl() != null) {
            importElement.addElement(N_OLDWEBAPPURL).setText(m_importExportManager.getOldWebAppUrl());
        }

        // <importversions> node
        Element resourcetypesElement = importElement.addElement(N_IMPORTVERSIONS);
        i = m_importExportManager.getImportVersionClasses().iterator();
        while (i.hasNext()) {
            resourcetypesElement.addElement(N_IMPORTVERSION).addAttribute(A_CLASS, i.next().getClass().getName());
        }

        // <immutables> node
        Element immutablesElement = importElement.addElement(N_IMMUTABLES);
        i = m_importExportManager.getImmutableResources().iterator();
        while (i.hasNext()) {
            String uri = (String)i.next();
            immutablesElement.addElement(N_RESOURCE).addAttribute(A_URI, uri);
        }

        // <principaltranslations> node
        Element principalsElement = importElement.addElement(N_PRINCIPALTRANSLATIONS);
        i = m_importExportManager.getImportUserTranslations().keySet().iterator();
        while (i.hasNext()) {
            String from = (String)i.next();
            String to = (String)m_importExportManager.getImportUserTranslations().get(from);
            principalsElement.addElement(N_PRINCIPALTRANSLATION).addAttribute(A_TYPE, I_CmsPrincipal.PRINCIPAL_USER).addAttribute(
                A_FROM,
                from).addAttribute(A_TO, to);
        }
        i = m_importExportManager.getImportGroupTranslations().keySet().iterator();
        while (i.hasNext()) {
            String from = (String)i.next();
            String to = (String)m_importExportManager.getImportGroupTranslations().get(from);
            principalsElement.addElement(N_PRINCIPALTRANSLATION).addAttribute(A_TYPE, I_CmsPrincipal.PRINCIPAL_GROUP).addAttribute(
                A_FROM,
                from).addAttribute(A_TO, to);
        }

        // <ignoredproperties> node
        Element propertiesElement = importElement.addElement(N_IGNOREDPROPERTIES);
        i = m_importExportManager.getIgnoredProperties().iterator();
        while (i.hasNext()) {
            String property = (String)i.next();
            propertiesElement.addElement(N_PROPERTY).addAttribute(A_NAME, property);
        }

        // <staticexport> node
        Element staticexportElement = parent.addElement(N_STATICEXPORT);
        staticexportElement.addAttribute(A_ENABLED, m_staticExportManager.getExportEnabled());

        // <staticexporthandler> node
        staticexportElement.addElement(N_STATICEXPORT_HANDLER).addText(
            m_staticExportManager.getHandler().getClass().getName());

        // <exportpath> node
        String exportPathUnmodified = m_staticExportManager.getExportPathForConfiguration();
        // cut path seperator        
        if (exportPathUnmodified.endsWith(File.separator)) {
            exportPathUnmodified = exportPathUnmodified.substring(0, exportPathUnmodified.length() - 1);
        }
        staticexportElement.addElement(N_STATICEXPORT_EXPORTPATH).addText(exportPathUnmodified);

        // <defaultpropertyvalue> node
        staticexportElement.addElement(N_STATICEXPORT_DEFAULT).addText(m_staticExportManager.getDefault());

        // <defaultsuffixes> node and its <suffix> sub nodes
        Element defaultsuffixesElement = staticexportElement.addElement(N_STATICEXPORT_DEFAULTSUFFIXES);

        i = m_staticExportManager.getExportSuffixes().iterator();
        while (i.hasNext()) {
            String suffix = (String)i.next();
            Element suffixElement = defaultsuffixesElement.addElement(N_STATICEXPORT_SUFFIX);
            suffixElement.addAttribute(A_KEY, suffix);
        }

        // <exportheaders> node and its <header> sub nodes
        i = m_staticExportManager.getExportHeaders().iterator();
        if (i.hasNext()) {
            Element exportheadersElement = staticexportElement.addElement(N_STATICEXPORT_EXPORTHEADERS);
            while (i.hasNext()) {
                String header = (String)i.next();
                exportheadersElement.addElement(N_STATICEXPORT_HEADER).addText(header);
            }
        }
        // <requestheaders> node and the <acceptlanguage> and <acceptcharset> node
        String acceptlanguage = m_staticExportManager.getAcceptLanguageHeader();
        String acceptcharset = m_staticExportManager.getAcceptCharsetHeader();
        String remoteaddr = m_staticExportManager.getRemoteAddr();
        if ((acceptlanguage != null) || (acceptcharset != null) || (remoteaddr != null)) {
            Element requestheadersElement = staticexportElement.addElement(N_STATICEXPORT_REQUESTHEADERS);
            if (acceptlanguage != null) {
                requestheadersElement.addElement(N_STATICEXPORT_ACCEPTLANGUAGE).addText(acceptlanguage);
            }
            if (acceptcharset != null) {
                requestheadersElement.addElement(N_STATICEXPORT_ACCEPTCHARSET).addText(acceptcharset);
            }
            if (remoteaddr != null) {
                requestheadersElement.addElement(N_STATICEXPORT_REMOTEADDR).addText(remoteaddr);
            }
        }

        // <rendersettings> node
        Element rendersettingsElement = staticexportElement.addElement(N_STATICEXPORT_RENDERSETTINGS);

        // <rfsPrefix> node
        rendersettingsElement.addElement(N_STATICEXPORT_RFS_PREFIX).addText(
            m_staticExportManager.getRfsPrefixForConfiguration());

        // <vfsPrefix> node
        rendersettingsElement.addElement(N_STATICEXPORT_VFS_PREFIX).addText(
            m_staticExportManager.getVfsPrefixForConfiguration());

        // <userelativelinks> node
        rendersettingsElement.addElement(N_STATICEXPORT_RELATIVELINKS).addText(m_staticExportManager.getRelativeLinks());

        // <exporturl> node
        rendersettingsElement.addElement(N_STATICEXPORT_EXPORTURL).addText(
            m_staticExportManager.getExportUrlForConfiguration());

        // <plainoptimization> node
        rendersettingsElement.addElement(N_STATICEXPORT_PLAINOPTIMIZATION).addText(
            m_staticExportManager.getPlainExportOptimization());

        // <testresource> node
        Element testresourceElement = rendersettingsElement.addElement(N_STATICEXPORT_TESTRESOURCE);
        testresourceElement.addAttribute(A_URI, m_staticExportManager.getTestResource());

        // <resourcestorender> node and <regx> subnodes
        Element resourcetorenderElement = rendersettingsElement.addElement(N_STATICEXPORT_RESOURCESTORENDER);

        i = m_staticExportManager.getExportFolderPatterns().iterator();
        while (i.hasNext()) {
            String pattern = (String)i.next();
            resourcetorenderElement.addElement(N_STATICEXPORT_REGEX).addText(pattern);
        }

        if (!m_staticExportManager.getExportRules().isEmpty()) {
            // <export-rules> node
            Element exportRulesElement = resourcetorenderElement.addElement(N_STATICEXPORT_EXPORTRULES);

            i = m_staticExportManager.getExportRules().iterator();
            while (i.hasNext()) {
                CmsStaticExportExportRule rule = (CmsStaticExportExportRule)i.next();
                // <export-rule> node
                Element exportRuleElement = exportRulesElement.addElement(N_STATICEXPORT_EXPORTRULE);
                exportRuleElement.addElement(N_STATICEXPORT_NAME).addText(rule.getName());
                exportRuleElement.addElement(N_STATICEXPORT_DESCRIPTION).addText(rule.getDescription());
                // <modified-resources> node and <regex> subnodes
                Element modifiedElement = exportRuleElement.addElement(N_STATICEXPORT_MODIFIED);
                Iterator itMods = rule.getModifiedResources().iterator();
                while (itMods.hasNext()) {
                    Pattern regex = (Pattern)itMods.next();
                    modifiedElement.addElement(N_STATICEXPORT_REGEX).addText(regex.pattern());
                }
                // <export-resources> node and <uri> subnodes
                Element exportElement = exportRuleElement.addElement(N_STATICEXPORT_EXPORT);
                Iterator itExps = rule.getExportResourcePatterns().iterator();
                while (itExps.hasNext()) {
                    String uri = (String)itExps.next();
                    exportElement.addElement(N_STATICEXPORT_URI).addText(uri);
                }
            }
        }

        if (!m_staticExportManager.getRfsRules().isEmpty()) {
            // <rfs-rules> node
            Element rfsRulesElement = rendersettingsElement.addElement(N_STATICEXPORT_RFS_RULES);

            i = m_staticExportManager.getRfsRules().iterator();
            while (i.hasNext()) {
                CmsStaticExportRfsRule rule = (CmsStaticExportRfsRule)i.next();
                // <rfs-rule> node and subnodes
                Element rfsRuleElement = rfsRulesElement.addElement(N_STATICEXPORT_RFS_RULE);
                rfsRuleElement.addElement(N_STATICEXPORT_NAME).addText(rule.getName());
                rfsRuleElement.addElement(N_STATICEXPORT_DESCRIPTION).addText(rule.getDescription());
                rfsRuleElement.addElement(N_STATICEXPORT_SOURCE).addText(rule.getSource().pattern());
                rfsRuleElement.addElement(N_STATICEXPORT_RFS_PREFIX).addText(rule.getRfsPrefixConfigured());
                rfsRuleElement.addElement(N_STATICEXPORT_EXPORTPATH).addText(rule.getExportPathConfigured());
                if (rule.getUseRelativeLinks() != null) {
                    rfsRuleElement.addElement(N_STATICEXPORT_RELATIVELINKS).addText(
                        rule.getUseRelativeLinks().toString());
                }
                Element relatedSystemRes = rfsRuleElement.addElement(N_STATICEXPORT_RELATED_SYSTEM_RES);
                Iterator itSystemRes = rule.getRelatedSystemResources().iterator();
                while (itSystemRes.hasNext()) {
                    Pattern sysRes = (Pattern)itSystemRes.next();
                    relatedSystemRes.addElement(N_STATICEXPORT_REGEX).addText(sysRes.pattern());
                }
            }

        }
        // return the configured node
        return importexportElement;
    }

    /**
     * @see org.opencms.configuration.I_CmsXmlConfiguration#getDtdFilename()
     */
    public String getDtdFilename() {

        return CONFIGURATION_DTD_NAME;
    }

    /**
     * Returns the initialized import/export manager.<p>
     * 
     * @return the initialized import/export manager
     */
    public CmsImportExportManager getImportExportManager() {

        return m_importExportManager;
    }

    /**
     * Returns the initialized static export manager.<p>
     * 
     * @return the initialized static export manager
     */
    public CmsStaticExportManager getStaticExportManager() {

        return m_staticExportManager;
    }

    /**
     * Will be called when configuration of this object is finished.<p> 
     */
    public void initializeFinished() {

        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_IMPORT_CONFIG_FINISHED_0));
        }
    }

    /**
     * Sets the generated import/export manager.<p>
     * 
     * @param manager the import/export manager to set
     */
    public void setImportExportManager(CmsImportExportManager manager) {

        m_importExportManager = manager;
        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_IMPORT_MANAGER_0));
        }
    }

    /**
     * Sets the generated static export manager.<p>
     * 
     * @param manager the static export manager to set
     */
    public void setStaticExportManager(CmsStaticExportManager manager) {

        m_staticExportManager = manager;
        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_STATEXP_MANAGER_0));
        }
    }
}