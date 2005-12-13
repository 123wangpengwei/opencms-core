/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/setup/Attic/CmsUpdateBean.java,v $
 * Date   : $Date: 2005/12/13 17:11:49 $
 * Version: $Revision: 1.1.2.6 $
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

package org.opencms.setup;

import org.opencms.i18n.CmsEncoder;
import org.opencms.main.CmsException;
import org.opencms.main.CmsSystemInfo;
import org.opencms.main.OpenCms;
import org.opencms.report.CmsShellReport;
import org.opencms.report.I_CmsReport;
import org.opencms.util.CmsFileUtil;
import org.opencms.util.CmsStringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.servlet.jsp.JspWriter;

import org.apache.commons.collections.ExtendedProperties;

/**
 * A java bean as a controller for the OpenCms update wizard.<p>
 * 
 * @author  Michael Moossen
 * 
 * @version $Revision: 1.1.2.6 $ 
 * 
 * @since 6.0.0 
 */
public class CmsUpdateBean extends CmsSetupBean {

    /** name of the jars folder. */
    public static final String FOLDER_JARS = "jars" + File.separatorChar;

    /** name of the update folder. */
    public static final String FOLDER_UPDATE = "update" + File.separatorChar;

    /** replace pattern constant for the cms script. */
    private static final String C_ADMIN_GROUP = "@ADMIN_GROUP@";

    /** replace pattern constant for the cms script. */
    private static final String C_ADMIN_PWD = "@ADMIN_PWD@";

    /** replace pattern constant for the cms script. */
    private static final String C_ADMIN_USER = "@ADMIN_USER@";

    /** replace pattern constant for the cms script. */
    private static final String C_UPDATE_PROJECT = "@UPDATE_PROJECT@";

    /** replace pattern constant for the cms script. */
    private static final String C_UPDATE_SITE = "@UPDATE_SITE@";

    /** property name. */
    private static final String PROP_JARS_TO_REMOVE = "jars.to.remove";

    /** The used admin user name. */
    private String m_adminGroup = "_tmpUpdateGroup" + (System.currentTimeMillis() % 1000);

    /** the admin user password. */
    private String m_adminPwd = "admin";

    /** The used admin user name. */
    private String m_adminUser = "Admin";

    /** the update project. */
    private String m_updateProject = "_tmpUpdateProject" + (System.currentTimeMillis() % 1000);

    /** the site for update. */
    private String m_updateSite = "/sites/default/";

    /** The workplace import thread. */
    private CmsUpdateThread m_workplaceUpdateThread;

    /** 
     * Default constructor.<p>
     */
    public CmsUpdateBean() {

        super();
        m_modulesFolder = FOLDER_UPDATE + CmsSystemInfo.FOLDER_MODULES;
        m_logFile = FOLDER_WEBINF + FOLDER_LOGS + "update.log";
    }

    /**
     * Returns the admin Pwd.<p>
     *
     * @return the admin Pwd
     */
    public String getAdminPwd() {

        return m_adminPwd;
    }

    /**
     * Returns the admin User.<p>
     *
     * @return the admin User
     */
    public String getAdminUser() {

        return m_adminUser;
    }

    /**
     * Returns the update Project.<p>
     *
     * @return the update Project
     */
    public String getUpdateProject() {

        return m_updateProject;
    }

    /**
     * Returns the update site.<p>
     *
     * @return the update site
     */
    public String getUpdateSite() {

        return m_updateSite;
    }

    /**
     * Returns the workplace update thread.<p>
     * 
     * @return the workplace update thread
     */
    public CmsUpdateThread getWorkplaceUpdateThread() {

        return m_workplaceUpdateThread;
    }

    /** 
     * Creates a new instance of the setup Bean.<p>
     * 
     * @param webAppRfsPath path to the OpenCms web application
     * @param servletMapping the OpenCms servlet mapping
     * @param defaultWebApplication the name of the default web application
     * 
     */
    public void init(String webAppRfsPath, String servletMapping, String defaultWebApplication) {

        try {
            super.init(webAppRfsPath, servletMapping, defaultWebApplication);

            if (m_workplaceUpdateThread != null) {
                if (m_workplaceUpdateThread.isAlive()) {
                    m_workplaceUpdateThread.kill();
                }
                m_workplaceUpdateThread = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Prepares step 4 of the update wizard.<p>
     */
    public void prepareUpdateStep4() {

        if (isInitialized()) {
            try {
                String fileName = getWebAppRfsPath() + FOLDER_UPDATE + "cmsupdate";
                // read the file
                FileInputStream fis = new FileInputStream(fileName + ".ori");
                String script = "";
                int readChar = fis.read();
                while (readChar > -1) {
                    script += (char)readChar;
                    readChar = fis.read();
                }
                // substitute macros
                script = CmsStringUtil.substitute(script, C_ADMIN_USER, getAdminUser());
                script = CmsStringUtil.substitute(script, C_ADMIN_PWD, getAdminPwd());
                script = CmsStringUtil.substitute(script, C_UPDATE_PROJECT, getUpdateProject());
                script = CmsStringUtil.substitute(script, C_UPDATE_SITE, getUpdateSite());
                script = CmsStringUtil.substitute(script, C_ADMIN_GROUP, getAdminGroup());
                // write the new script
                FileOutputStream fos = new FileOutputStream(fileName + ".txt");
                fos.write(script.getBytes());
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Prepares the update wizard.<p>
     */
    public void prepareUpdateStep4b() {

        if (!isInitialized()) {
            return;
        }

        if ((m_workplaceUpdateThread != null) && (m_workplaceUpdateThread.isFinished())) {
            // update is already finished, just wait for client to collect final data
            return;
        }

        if (m_workplaceUpdateThread == null) {
            m_workplaceUpdateThread = new CmsUpdateThread(this);
        }

        if (!m_workplaceUpdateThread.isAlive()) {
            m_workplaceUpdateThread.start();
        }
    }

    /**
     * Generates the output for the update wizard.<p>
     * 
     * @param out the JSP print stream
     * 
     * @throws IOException in case errors occur while writing to "out"
     */
    public void prepareUpdateStep4bOutput(JspWriter out) throws IOException {

        m_oldLoggingOffset = m_newLoggingOffset;
        m_newLoggingOffset = m_workplaceUpdateThread.getLoggingThread().getMessages().size();
        if (isInitialized()) {
            for (int i = m_oldLoggingOffset; i < m_newLoggingOffset; i++) {
                String str = m_workplaceUpdateThread.getLoggingThread().getMessages().get(i).toString();
                str = CmsEncoder.escapeWBlanks(str, "UTF-8");
                out.println("output[" + (i - m_oldLoggingOffset) + "] = \"" + str + "\";");
            }
        } else {
            out.println("output[0] = 'ERROR';");
        }

        boolean threadFinished = m_workplaceUpdateThread.isFinished();
        boolean allWritten = m_oldLoggingOffset >= m_workplaceUpdateThread.getLoggingThread().getMessages().size();

        out.println("function initThread() {");
        if (isInitialized()) {
            out.print("send();");
            if (threadFinished && allWritten) {
                out.println("setTimeout('top.display.finish()', 500);");
            } else {
                int timeout = 5000;
                if (getWorkplaceUpdateThread().getLoggingThread().getMessages().size() < 20) {
                    timeout = 1000;
                }
                out.println("setTimeout('location.reload()', " + timeout + ");");
            }
        }
        out.println("}");
    }

    /**
     * Prepares step 6 of the update wizard.<p>
     */
    public void prepareUpdateStep6() {

        if (isInitialized()) {
            // lock the wizard for further use 
            lockWizard();
            // save Properties to file "opencms.properties" 
            saveProperties(getProperties(), "opencms.properties", false);
        }
    }

    /**
     * Sets the admin Pwd.<p>
     *
     * @param adminPwd the admin Pwd to set
     */
    public void setAdminPwd(String adminPwd) {

        m_adminPwd = adminPwd;
    }

    /**
     * Sets the admin User.<p>
     *
     * @param adminUser the admin User to set
     */
    public void setAdminUser(String adminUser) {

        m_adminUser = adminUser;
    }

    /**
     * Sets the update Project.<p>
     *
     * @param updateProject the update Project to set
     */
    public void setUpdateProject(String updateProject) {

        m_updateProject = updateProject;
    }

    /**
     * Sets the update site.<p>
     *
     * @param site the update site to set
     */
    public void setUpdateSite(String site) {

        m_updateSite = site;
    }

    /**
     * @see org.opencms.main.I_CmsShellCommands#shellExit()
     */
    public void shellExit() {

        System.out.println();
        System.out.println();
        System.out.println("The update is finished!\nThe OpenCms system used for the update will now shut down.");
    }

    /**
     * @see org.opencms.main.I_CmsShellCommands#shellStart()
     */
    public void shellStart() {

        System.out.println();
        System.out.println("Starting Workplace update for OpenCms!");

        String[] copy = org.opencms.main.Messages.COPYRIGHT_BY_ALKACON;
        for (int i = copy.length - 1; i >= 0; i--) {
            System.out.println(copy[i]);
        }
        System.out.println("This is OpenCms " + OpenCms.getSystemInfo().getVersionName());
        System.out.println();
        System.out.println();
    }

    /**
     * Removes all jars indicated in the jars.to.remove property of file /update/jars/removejars.properties, and
     * copies all jars in the /update/jars/ folder to the /WEB-INF/lib/ folder.<p>
     * 
     * @throws IOException if something goes wrong 
     */
    public void updateJarsFromUpdateBean() throws IOException {

        String jarFolder = getWebAppRfsPath() + FOLDER_UPDATE + FOLDER_JARS;
        String libFolder = getWebAppRfsPath() + FOLDER_WEBINF + FOLDER_LIB;
        I_CmsReport report = new CmsShellReport(m_cms.getRequestContext().getLocale());
        report.println(Messages.get().container(Messages.RPT_BEGIN_DELETE_JARS_0), I_CmsReport.FORMAT_HEADLINE);
        ExtendedProperties props = loadProperties(jarFolder + "/removejars.properties");
        String[] jars = props.getStringArray(PROP_JARS_TO_REMOVE);
        if (jars != null) {
            for (int i = 0, n = jars.length; i < n; i++) {
                File jar = new File(libFolder + jars[i]);
                if (jar.exists() && jar.canWrite()) {
                    if (jar.delete()) {
                        report.println(Messages.get().container(
                            Messages.RPT_DELETE_JAR_FILE_3,
                            new Integer(i + 1),
                            new Integer(n),
                            jar.getAbsolutePath()));
                    } else {
                        report.println(Messages.get().container(
                            Messages.RPT_DELETE_JAR_FILE_FAILED_3,
                            new Integer(i + 1),
                            new Integer(n),
                            jar.getAbsolutePath()));
                    }
                } else {
                    report.println(Messages.get().container(
                        Messages.RPT_DELETE_JAR_SKIPPED_FILE_3,
                        new Integer(i + 1),
                        new Integer(n),
                        jar.getAbsolutePath()));
                }
            }
        } else {
            report.println(Messages.get().container(
                Messages.RPT_DELETE_JARS_FAILED_1,
                jarFolder + "/removejars.properties"), I_CmsReport.FORMAT_ERROR);
        }
        report.println(Messages.get().container(Messages.RPT_END_DELETE_JARS_0), I_CmsReport.FORMAT_HEADLINE);
        File folder = new File(jarFolder);
        report.println(Messages.get().container(Messages.RPT_BEGIN_UPDATE_JARS_0), I_CmsReport.FORMAT_HEADLINE);
        if (folder.exists()) {
            // list all child resources in the given folder
            File[] folderFiles = folder.listFiles();
            if (folderFiles != null) {
                for (int i = 0; i < folderFiles.length; i++) {
                    File jarFile = folderFiles[i];
                    if (jarFile.isFile() && !(jarFile.getAbsolutePath().toLowerCase().endsWith(".jar"))) {
                        report.println(Messages.get().container(
                            Messages.RPT_UPDATE_JAR_SKIPPED_FILE_3,
                            new Integer(i + 1),
                            new Integer(folderFiles.length),
                            jarFile.getAbsolutePath()));
                    } else {
                        try {
                            CmsFileUtil.copy(jarFile.getAbsolutePath(), libFolder + jarFile.getName());
                            report.println(Messages.get().container(
                                Messages.RPT_UPDATE_JAR_FILE_3,
                                new Integer(i + 1),
                                new Integer(folderFiles.length),
                                jarFile.getAbsolutePath()));
                        } catch (Exception e) {
                            report.println(Messages.get().container(
                                Messages.RPT_UPDATE_JAR_FILE_FAILED_3,
                                new Integer(i + 1),
                                new Integer(folderFiles.length),
                                jarFile.getAbsolutePath()), I_CmsReport.FORMAT_ERROR);
                        }
                    }
                }
            } else {
                report.println(
                    Messages.get().container(Messages.RPT_UPDATE_JARS_FAILED_1, folder.getAbsolutePath()),
                    I_CmsReport.FORMAT_ERROR);
            }
        } else {
            report.println(
                Messages.get().container(Messages.RPT_UPDATE_JARS_FAILED_1, folder.getAbsolutePath()),
                I_CmsReport.FORMAT_ERROR);
        }
        report.println(Messages.get().container(Messages.RPT_END_UPDATE_JARS_0), I_CmsReport.FORMAT_HEADLINE);
    }

    /**
     * Installed all modules that have been set using {@link #setInstallModules(String)}.<p>
     * 
     * This method is invoked as a shell command.<p>
     * 
     * @throws Exception if something goes wrong
     */
    public void updateModulesFromUpdateBean() throws Exception {

        // read here how the list of modules to be installed is passed from the setup bean to the
        // setup thread, and finally to the shell process that executes the setup script:
        // 1) the list with the package names of the modules to be installed is saved by setInstallModules
        // 2) the setup thread gets initialized in a JSP of the setup wizard
        // 3) the instance of the setup bean is passed to the setup thread by setAdditionalShellCommand
        // 4) the setup bean is passed to the shell by startSetup
        // 5) because the setup bean implements I_CmsShellCommands, the shell constructor can pass the shell's CmsObject back to the setup bean
        // 6) thus, the setup bean can do things with the Cms

        if (m_cms != null && m_installModules != null) {
            I_CmsReport report = new CmsShellReport(m_cms.getRequestContext().getLocale());
            for (int i = 0; i < m_installModules.size(); i++) {
                String name = (String)m_installModules.get(i);
                String filename = (String)m_moduleFilenames.get(name);
                try {
                    updateModule(name, filename, report);
                } catch (Exception e) {
                    // log a exception during module import, but make sure the next module is still imported
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Returns the admin Group.<p>
     *
     * @return the admin Group
     */
    protected String getAdminGroup() {

        return m_adminGroup;
    }

    /**
     * Sets the admin Group.<p>
     *
     * @param adminGroup the admin Group to set
     */
    protected void setAdminGroup(String adminGroup) {

        m_adminGroup = adminGroup;
    }

    /**
     * Imports a module (zipfile) from the default module directory, 
     * creating a temporary project for this.<p>
     * 
     * @param moduleName the name of the module to replace
     * @param importFile the name of the import .zip file located in the update module directory
     * @param report the shell report to write the output
     * 
     * @throws Exception if something goes wrong
     * 
     * @see org.opencms.importexport.CmsImportExportManager#importData(org.opencms.file.CmsObject, String, String, org.opencms.report.I_CmsReport)
     */
    protected void updateModule(String moduleName, String importFile, I_CmsReport report) throws Exception {

        String fileName = getModuleFolder() + importFile;

        report.println(
            Messages.get().container(Messages.RPT_BEGIN_UPDATE_MODULE_1, moduleName),
            I_CmsReport.FORMAT_HEADLINE);
        if (OpenCms.getModuleManager().getModule(moduleName) != null) {
            OpenCms.getModuleManager().deleteModule(m_cms, moduleName, true, report);
        }
        OpenCms.getImportExportManager().importData(m_cms, fileName, null, report);
        report.println(
            Messages.get().container(Messages.RPT_END_UPDATE_MODULE_1, moduleName),
            I_CmsReport.FORMAT_HEADLINE);
    }
}