/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/main/CmsShellCommands.java,v $
 * Date   : $Date: 2003/09/05 16:11:48 $
 * Version: $Revision: 1.10 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2003 Alkacon Software (http://www.alkacon.com)
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

package org.opencms.main;

import org.opencms.db.CmsDriverManager;
import org.opencms.db.I_CmsDriver;
import org.opencms.db.I_CmsVfsDriver;
import org.opencms.security.CmsAccessControlEntry;
import org.opencms.security.CmsAccessControlList;
import org.opencms.security.I_CmsPrincipal;
import org.opencms.workflow.CmsTask;

import com.opencms.boot.CmsBase;
import com.opencms.core.CmsCronEntry;
import com.opencms.core.CmsCronTable;
import com.opencms.core.CmsException;
import com.opencms.core.I_CmsConstants;
import com.opencms.file.CmsGroup;
import com.opencms.file.CmsObject;
import com.opencms.file.CmsProject;
import com.opencms.file.CmsRegistry;
import com.opencms.file.CmsResource;
import com.opencms.file.CmsResourceTypeFolder;
import com.opencms.file.CmsUser;
import com.opencms.flex.util.CmsUUID;
import org.opencms.report.CmsShellReport;
import com.opencms.workplace.I_CmsWpConstants;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This class is a commad line interface to OpenCms which 
 * can be used for the initial setup and to test the system.<p>
 * 
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * 
 * @version $Revision: 1.10 $ $Date: 2003/09/05 16:11:48 $ 
 * @see com.opencms.file.CmsObject
 */
class CmsShellCommands {

    /**
     * The CmsObject object which provides access to the VFS..
     */
    private CmsObject m_cms;

    /**
     * The wrapped OpenCms object which provides the system environment.
     */
    private OpenCmsCore m_openCms;

    /**
     * The driver manager to provide low-level access.<p>
     */
    private CmsDriverManager m_driverManager;
    
    /**
     * Generate a new instance of CmsShellCommands.<p>
     * 
     * @param openCms an initialized OpenCms object (i.e. "operating system")
     * @param cms an initialized CmsObject (i.e. "command shell")
     * @param driverManager the driver manager
     * @throws Exception if something goes wrong
     */
    public CmsShellCommands(OpenCmsCore openCms, CmsObject cms, CmsDriverManager driverManager) throws Exception {
        m_openCms = openCms;
        m_cms = cms;
        m_driverManager = driverManager;
        m_openCms.initUser(m_cms, null, null, OpenCms.getDefaultUsers().getUserGuest(), OpenCms.getSiteManager().getDefaultSite().getSiteRoot(), I_CmsConstants.C_PROJECT_ONLINE_ID, null);

        // print the version-string
        version();
        copyright();
        printHelpText();
    }

    /**
     * Accept a task from the Cms.
     *
     * @param taskId the id of the task to accept.
     */
    public void acceptTask(String taskId) {
        try {
            int id = Integer.parseInt(taskId);
            m_cms.acceptTask(id);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Tests if the user can access the project.
     *
     * @param id the id of the project.
     */
    public void accessProject(String id) {
        try {
            int projectId = Integer.parseInt(id);
            System.out.println(m_cms.accessProject(projectId));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Adds a file extension.<p>
     * 
     * @param resourceType name of a resource type, e.g. 'plain'
     * @param extension a file extension, e.g. 'html'
     */
    public void addFileExtension(String resourceType, String extension) {
        try {
            m_cms.addFileExtension(extension, resourceType);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Adds a Group to the cms.
     *
     * @param name The name of the new group.
     * @param description The description for the new group.
     */
    public void addGroup(String name, String description) {
        try {
            m_cms.createGroup(name, description, I_CmsConstants.C_FLAG_ENABLED, null);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Adds a Group to the cms.
     *
     * @param name The name of the new group.
     * @param description The description for the new group.
     * @param flags The flags for the new group.
     * @param parent The name of the parent group (or null).
     */
    public void addGroup(String name, String description, String flags, String parent) {
        try {
            m_cms.createGroup(name, description, Integer.parseInt(flags), parent);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Adds a user to the cms.
     *
     * @param name The new name for the user.
     * @param password The new password for the user.
     * @param group The default groupname for the user.
     * @param description The description for the user.
     */
    public void addUser(String name, String password, String group, String description) {
        try {
            System.out.println(m_cms.addUser(name, password, group, description, new Hashtable(), I_CmsConstants.C_FLAG_ENABLED));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Adds a user to the cms.
     *
     * @param name The new name for the user.
     * @param password The new password for the user.
     * @param group The default groupname for the user.
     * @param description The description for the user.
     * @param flags The flags for the user.
     */
    public void addUser(String name, String password, String group, String description, String flags) {
        try {
            System.out.println(m_cms.addUser(name, password, group, description, new Hashtable(), Integer.parseInt(flags)));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Adds a user to the cms.
     *
     * @param name The new name for the user.
     * @param password The new password for the user.
     * @param group The default groupname for the user.
     * @param description The description for the user.
     * @param firstname The first name of the user
     * @param lastname The last name of the user
     * @param email The email address of the user
     */
    public void addUser(String name, String password, String group, String description, String firstname, String lastname, String email) {
        try {
            CmsUser user = m_cms.addUser(name, password, group, description, new Hashtable(), I_CmsConstants.C_FLAG_ENABLED);
            user.setEmail(email);
            user.setFirstname(firstname);
            user.setLastname(lastname);
            m_cms.writeUser(user);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Adds a user to a group.
     *
     * @param username The name of the user that is to be added to the group.
     * @param groupname The name of the group.
     */
    public void addUserToGroup(String username, String groupname) {
        try {
            m_cms.addUserToGroup(username, groupname);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Adds a web user to the Cms.
     * <br>
     * A web user has no access to the workplace but is able to access personalized
     * functions controlled by the OpenCms.
     *
     * @param name the new name for the user.
     * @param password the new password for the user.
     * @param group the default groupname for the user.
     * @param description the description for the user.
     * @param flags the flags for a user (e.g. C_FLAG_ENABLED)
     *
     */
    public void addWebUser(String name, String password, String group, String description, String flags) {
        try {
            int intFlags = Integer.parseInt(flags);
            System.out.println(m_cms.addWebUser(name, password, group, description, new Hashtable(), intFlags));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Adds a web user to the Cms.
     * <br>
     * A web user has no access to the workplace but is able to access personalized
     * functions controlled by the OpenCms.
     *
     * @param name the new name for the user.
     * @param password the new password for the user.
     * @param group the default groupname for the user.
     * @param additionalGroup the additional group for the user.
     * @param description the description for the user.
     * @param flags the flags for a user (e.g. C_FLAG_ENABLED)
     *
     */
    public void addWebUser(String name, String password, String group, String additionalGroup, String description, String flags) {
        try {
            int intFlags = Integer.parseInt(flags);
            System.out.println(m_cms.addWebUser(name, password, group, additionalGroup, description, new Hashtable(), intFlags));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Changes the access control for a given resource and a given principal(user/group).<p>
     * 
     * @param resourceName the name of the resource
     * @param principalType the principalType (user or group)
     * @param principalName the name of the principal
     * @param allowedPermissions bitset of allowed permissions
     * @param deniedPermissions bitset of denied permissions
     * @param flags flags
     */
    public void chacc(String resourceName, String principalType, String principalName, int allowedPermissions, int deniedPermissions, int flags) {
        try {
            m_cms.chacc(resourceName, principalType, principalName, allowedPermissions, deniedPermissions, flags);
        } catch (Exception e) {
            CmsShell.printException(e);
        }
    }

    /**
     * Changes the access control for a given resource and a given principal(user/group).<p>
     * 
     * @param resourceName the name of the reosurce
     * @param principalType the type of the principal (user or group)
     * @param principalName the name of the principal
     * @param permissionString the permissions in the format ((+|-)(r|w|v|c|i))*
     */
    public void chacc(String resourceName, String principalType, String principalName, String permissionString) {
        try {
            m_cms.chacc(resourceName, principalType, principalName, permissionString);
        } catch (Exception e) {
            CmsShell.printException(e);
        }
    }

    /**
     * Changes the type of the user to webusertype
     *
     * @param username The name of the user to change
     * @param userType The new type of the user
     */
    public void changeUserType(String username, String userType) {
        try {
            m_cms.changeUserType(username, Integer.parseInt(userType));
            CmsUser user = null;
            try {
                user = m_cms.readWebUser(username);
            } catch (CmsException ex) {
                if (ex.getType() == CmsException.C_NO_USER) {
                    user = m_cms.readUser(username);
                }
            }
            System.out.println(user.toString());
        } catch (Exception e) {
            CmsShell.printException(e);
        }
    }

    /**
     * Changes the type of the user
     *
     * @param userId The id of the user to change
     * @param userType The new type of the user
     */
    public void changeUserTypeByUserid(String userId, String userType) {
        try {
            m_cms.changeUserType(new CmsUUID(userId), Integer.parseInt(userType));
            CmsUser user = m_cms.readUser(new CmsUUID(userId));
            System.out.println(user.toString());
        } catch (Exception e) {
            CmsShell.printException(e);
        }
    }

    /**
     * Changes the resourcetype for this resource<BR/>
     *
     * The user may change this, if he is admin of the resource.
     *
     * @param filename The complete path to the resource.
     * @param newType The name of the new resourcetype for this resource.
     */
    public void chtype(String filename, String newType) {
        try {
            m_cms.chtype(filename, m_cms.getResourceTypeId(newType));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Clears all internal DB-Caches.
     */
    public void clearcache() {
        try {
            m_cms.clearcache();
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Copies the file.
     *
     * @param source The complete path of the sourcefile.
     * @param destination The complete path of the destination.
     */
    public void copyFile(String source, String destination) {
        try {
            m_cms.copyResource(source, destination);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Copies a folder.
     *
     * @param source the complete path of the sourcefolder.
     * @param destination the complete path of the destinationfolder.
     */
    public void copyFolder(String source, String destination) {
        try {
            // copy the folder with keeping the flags
            m_cms.copyResource(source, destination, true, true, I_CmsConstants.C_COPY_PRESERVE_LINK);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Copies a resource.
     *
     * @param source the complete path of the sourcefolder.
     * @param destination the complete path of the destinationfolder.
     */
    public void copyResource(String source, String destination) {
        try {
            m_cms.copyResource(source, destination);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Copies a resource from the online project to a new, specified project.<br>
     * Copying a resource will copy the file header or folder into the specified
     * offline project and set its state to UNCHANGED.
     *
     * @param resource The name of the resource.
     */
    public void copyResourceToProject(String resource) {
        try {
            m_cms.copyResourceToProject(resource);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns a copyright-string for this OpenCms.
     */
    public void copyright() {
        String[] copy = I_CmsConstants.C_COPYRIGHT;
        for (int i = 0; i < copy.length; i++) {
            System.out.println(copy[i]);
        }
    }

    /**
     * Counts the locked resources in a project.
     *
     * @param id the id of the project
     */
    public void countLockedResources(String id) {
        try {
            int intId = Integer.parseInt(id);
            System.out.println(m_cms.countLockedResources(intId));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Creates a default cms project.
     * This default project has the following properties:<ul>
     * <li>The users groups is the default user group
     * <li>The project managers group is the default project manager group
     * <li>All resources are contained in the project
     * <li>The project will remain after publishing</ul>
     * 
     * @param name The name of the project to create
     * @param description The description for the new project
     * 
     */
    public void createDefaultProject(String name, String description) {
        try {
            m_cms.getRequestContext().saveSiteRoot();
            m_cms.getRequestContext().setSiteRoot("/");
            CmsProject project = m_cms.createProject(
                name, 
                description, 
                OpenCms.getDefaultUsers().getGroupUsers(), 
                OpenCms.getDefaultUsers().getGroupProjectmanagers(), 
                I_CmsConstants.C_PROJECT_TYPE_NORMAL
            );
            m_cms.getRequestContext().setCurrentProject(project.getId());
            // copy the VFS folders to the project
            m_cms.copyResourceToProject("/");
        } catch (Exception exc) {
            CmsShell.printException(exc);
        } finally {
            m_cms.getRequestContext().restoreSiteRoot();
        }
    }

    /**
     * Creates a new folder.
     *
     * @param folder The complete path to the folder in which the new folder
     * will be created.
     * @param newFolderName The name of the new folder (No pathinformation allowed).
     */
    public void createFolder(String folder, String newFolderName) {
        try {
            System.out.println(m_cms.createResource(folder, newFolderName, CmsResourceTypeFolder.C_RESOURCE_TYPE_ID));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * This method creates a new module in the repository.
     *
     * @param modulename the name of the module.
     * @param niceModulename another name of the module.
     * @param description the description of the module.
     * @param author the name of the author.
     * @param type the type of the module.
     * @param createDate the creation date of the module
     * @param version the version number of the module.
     */
    public void createModule(String modulename, String niceModulename, String description, String author, String type, String createDate, String version) {

        // create the module
        try {
            CmsRegistry reg = m_cms.getRegistry();
            int ver = Integer.parseInt(version);
            long date = Long.parseLong(createDate);
            reg.createModule(modulename, niceModulename, description, author, type, new HashMap(), date, ver);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Creates a project.
     *
     * @param name The name of the project to create
     * @param description The description for the new project
     * @param groupname the name of the group to be set
     * @param managergroupname the name of the manager group
     */
    public void createProject(String name, String description, String groupname, String managergroupname) {
        try {
            System.out.println(m_cms.createProject(name, description, groupname, managergroupname));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Creates a project.
     *
     * @param name The name of the project to create
     * @param description The description for the new project
     * @param groupname the name of the group to be set
     * @param managergroupname the name of the managing group
     * @param projecttype the type of the project
     */
    public void createProject(String name, String description, String groupname, String managergroupname, String projecttype) {
        try {
            System.out.println(m_cms.createProject(name, description, groupname, managergroupname, Integer.parseInt(projecttype)));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Creates a property definition for the resource type.<p>
     *
     * @param name the name of the propertydefinition to create
     * @param resourcetype the name of the resource type for the property definition
     *
     * @throws CmsException if something goes wrong
     */
    public void createPropertydefinition(String resourcetype, String name) throws CmsException {
        try {
            System.out.println(m_cms.createPropertydefinition(name, m_cms.getResourceTypeId(resourcetype)));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Creates a new task.
     * <p>
     * <B>Security:</B>
     * All users can create a new task.
     * @param agentName the User who will edit the task.
     * @param roleName a Usergroup for the task.
     * @param taskname the name of the task.
     * @param taskcomment a description of the task.
     * @param timeout the time when the task must finished.
     * @param priority the Id for the priority of the task.
     */
    public void createTask(String agentName, String roleName, String taskname, String taskcomment, String timeout, String priority) {
        try {
            int intPriority = Integer.parseInt(priority);
            long longTimeout = Long.parseLong(timeout);
            System.out.println(m_cms.createTask(agentName, roleName, taskname, taskcomment, longTimeout, intPriority));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Creates a new task.
     * <p>
     * <B>Security:</B>
     * All users can create a new task.
     *
     * @param projectid the Id of the current project task of the user.
     * @param agentName the User who will edit the task.
     * @param roleName a Usergroup for the task.
     * @param taskname a Name of the task.
     * @param tasktype the type of the task.
     * @param taskcomment a description of the task.
     * @param timeout the time when the task must finished.
     * @param priority the Id for the priority of the task.
     */
    public void createTask(String projectid, String agentName, String roleName, String taskname, String taskcomment, String tasktype, String timeout, String priority) {
        try {
            System.out.println(m_cms.createTask(Integer.parseInt(projectid), agentName, roleName, taskname, taskcomment, Integer.parseInt(tasktype), Long.parseLong(timeout), Integer.parseInt(priority)));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Creates a new project for the temporary files.
     *
     * @throws CmsException if operation was not successful.
     */
    public void createTempfileProject() throws CmsException {
        try {
            System.out.println(m_cms.createTempfileProject());
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Deletes all propertyinformation for a file or folder.
     *
     * @param resource The name of the resource of which the propertyinformations
     * have to be deleted.
     */
    public void deleteAllProperties(String resource) {
        try {
            m_cms.deleteAllProperties(resource);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Deletes the versions from the backup tables that are older then the given weeks
     * 
     * @param weeks The number of weeks: the max age of the remaining versions
     */
    public void deleteBackups(String weeks) {
        try {
            System.out.println("Oldest remaining version: " + m_cms.deleteBackups(Integer.parseInt(weeks)));
        } catch (Exception e) {
            CmsShell.printException(e);
        }
    }

    /**
     * Deletes the file.
     *
     * @param filename The complete path of the file.
     */
    public void deleteFile(String filename) {
        try {
            m_cms.deleteResource(filename, I_CmsConstants.C_DELETE_OPTION_IGNORE_VFS_LINKS);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }
    

    /**
     * Displays the classes of the configured drivers.<p>
     */
    public void getDriverInfo() {
        try {
            Map drivers = m_cms.getDrivers();
            for (Iterator i = drivers.keySet().iterator(); i.hasNext();) {
                System.out.println(i.next());    
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }
    
    /**
     * Displays further information of a driver class.<p>
     * 
     * @param driverName the driver class name
     */
    public void getDriverInfo(String driverName) {
        try {
            Map drivers = m_cms.getDrivers();
            System.out.println(((I_CmsDriver)drivers.get(driverName)).toString());
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }    

    /**
     * Deletes the folder.
     *
     * @param foldername The complete path of the folder.
     */
    public void deleteFolder(String foldername) {
        try {
            m_cms.deleteResource(foldername, I_CmsConstants.C_DELETE_OPTION_IGNORE_VFS_LINKS);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Delete a group from the Cms.<BR/>
     *
     * @param delgroup The name of the group that is to be deleted.
     */
    public void deleteGroup(String delgroup) {
        try {
            m_cms.deleteGroup(delgroup);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Deletes a module from the cms.
     *
     * @param module The name of module to delete.
     */
    public void deleteModule(String module) {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            reg.deleteModule(module, new Vector(), false, new CmsShellReport());
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * This method deletes the view for an module.
     *
     * @param modulename the name of the module.
     */
    public void deleteModuleView(String modulename) {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            reg.deleteModuleView(modulename);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Deletes a project.
     *
     * @param id The id of the project to delete.
     */
    public void deleteProject(String id) {
        try {
            m_cms.deleteProject(Integer.parseInt(id));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Deletes a propertyinformation for a file or folder.
     *
     * @param resourcename The resource-name of which the propertyinformation has to be delteted.
     * @param property The propertydefinition-name of which the propertyinformation has to be set.
     */
    public void deleteProperty(String resourcename, String property) {
        try {
            m_cms.deleteProperty(resourcename, property);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Delete the propertydefinition for the resource type.<BR/>
     *
     * @param name The name of the propertydefinition to overwrite.
     * @param resourcetype The name of the resource-type for the propertydefinition.
     */
    public void deletepropertydefinition(String name, String resourcetype) {
        try {
            m_cms.deletePropertydefinition(name, m_cms.getResourceTypeId(resourcetype));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Deletes a Resource.
     *
     * @param filename the complete path of the sourcefolder.
     */
    public void deleteResource(String filename) {
        try {
            m_cms.deleteResource(filename, I_CmsConstants.C_DELETE_OPTION_IGNORE_VFS_LINKS);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Deletes a user from the Cms.
     *
     * @param name The name of the user to be deleted.
     */
    public void deleteUser(String name) {
        try {
            m_cms.deleteUser(name);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Deletes a web user from the Cms.
     *
     * @param userId the id of the user to be deleted.
     */
    public void deleteWebUser(String userId) {
        try {
            m_cms.deleteWebUser(new CmsUUID(userId));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Echos the input to output.
     *
     * @param echo The echo to be written to output.
     */
    public void echo(String echo) {
        if (echo.toLowerCase().equals("on")) {
            CmsShell.m_echo = true;
        } else {
            if (echo.toLowerCase().equals("off")) {
                CmsShell.m_echo = false;
            } else {
                System.out.println(echo);
            }
        }
    }

    /**
     * Ends a task of the Cms.
     *
     * @param taskid the ID of the task to end.
     */
    public void endTask(String taskid) {
        try {
            m_cms.endTask(Integer.parseInt(taskid));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Exits the commandline-interface
     */
    public void exit() {
        try {
            m_openCms.destroy();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        CmsShell.m_exitCalled = true;
    }

    /**
     * Exports cms-resources to zip. In the zip-file the system - path will be included.
     *
     * @param exportFile the name (absolute Path) of the export resource (zip)
     *
     * @throws CmsException if something goes wrong.
     */
    public void exportAllResources(String exportFile) throws CmsException {

        // export the resources
        String[] exportPaths = {I_CmsConstants.C_ROOT};
        try {
            m_cms.exportResources(exportFile, exportPaths, false, false);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Exports cms-resources to zip. In the zip-file the system - path will be included.
     * Unchanged resources will be ignored.
     *
     * @param exportFile the name (absolute Path) of the export resource (zip)
     *
     * @throws CmsException if something goes wrong.
     */
    public void exportAllResourcesOnlyChanged(String exportFile) throws CmsException {

        // export the resources
        String[] exportPaths = {I_CmsConstants.C_ROOT};
        try {
            m_cms.exportResources(exportFile, exportPaths, false, true);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Exports a module.
     *
     * @param modulename the name of the module to export
     * @param resource the folder to export.
     * @param filename the name of the file to export to.
     */
    public void exportModule(String modulename, String resource, String filename) {
        try {
            String[] resources = {resource};
            CmsRegistry reg = m_cms.getRegistry();
            reg.exportModule(modulename, resources, filename, new CmsShellReport());
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Exports cms-resources to zip.
     *
     * @param exportFile the name (absolute Path) of the export resource (zip)
     * @param pathList the names (absolute Path) of folders from which should be exported
     *          separated by semicolons
     *
     * @throws CmsException if something goes wrong.
     */
    public void exportResources(String exportFile, String pathList) throws CmsException {

        // export the resources
        StringTokenizer tok = new StringTokenizer(pathList, ";");
        Vector paths = new Vector();
        while (tok.hasMoreTokens()) {
            paths.addElement(tok.nextToken());
        }
        String exportPaths[] = new String[paths.size()];
        for (int i = 0; i < paths.size(); i++) {
            exportPaths[i] = (String)paths.elementAt(i);
        }
        boolean excludeSystem = true;
        if (pathList.startsWith(I_CmsWpConstants.C_VFS_PATH_SYSTEM) || (pathList.indexOf(";" + I_CmsWpConstants.C_VFS_PATH_SYSTEM) > -1)) {
            excludeSystem = false;
        }
        try {
            m_cms.exportResources(exportFile, exportPaths, excludeSystem, false);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Exports cms-resources to zip.
     *
     * @param exportFile the name (absolute Path) of the export resource (zip)
     * @param pathList the names (absolute Path) of folders from which should be exported
     *          separated by semicolons
     *
     * @throws CmsException if something goes wrong.
     */
    public void exportResourcesAndUserdata(String exportFile, String pathList) throws CmsException {

        // export the resources
        StringTokenizer tok = new StringTokenizer(pathList, ";");
        Vector paths = new Vector();
        while (tok.hasMoreTokens()) {
            paths.addElement(tok.nextToken());
        }
        String exportPaths[] = new String[paths.size()];
        for (int i = 0; i < paths.size(); i++) {
            exportPaths[i] = (String)paths.elementAt(i);
        }
        boolean excludeSystem = true;
        if (pathList.startsWith(I_CmsWpConstants.C_VFS_PATH_SYSTEM) || (pathList.indexOf(";" + I_CmsWpConstants.C_VFS_PATH_SYSTEM) > -1)) {
            excludeSystem = false;
        }
        try {
            m_cms.exportResources(exportFile, exportPaths, excludeSystem, false, true);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Exports cms-resources to zip.
     * Unchanged resources will be ignored.
     *
     * @param exportFile the name (absolute Path) of the export resource (zip)
     * @param pathList the names (absolute Path) of folders from which should be exported
     *          separated by semicolons
     *
     * @throws CmsException if something goes wrong.
     */
    public void exportResourcesOnlyChanged(String exportFile, String pathList) throws CmsException {

        // export the resources
        StringTokenizer tok = new StringTokenizer(pathList, ";");
        Vector paths = new Vector();
        while (tok.hasMoreTokens()) {
            paths.addElement(tok.nextToken());
        }
        String exportPaths[] = new String[paths.size()];
        for (int i = 0; i < paths.size(); i++) {
            exportPaths[i] = (String)paths.elementAt(i);
        }
        boolean excludeSystem = true;
        if (pathList.startsWith(I_CmsWpConstants.C_VFS_PATH_SYSTEM) || (pathList.indexOf(";" + I_CmsWpConstants.C_VFS_PATH_SYSTEM) > -1)) {
            excludeSystem = false;
        }
        try {
            m_cms.exportResources(exportFile, exportPaths, excludeSystem, true);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Forwards a task to a new user.
     *
     * @param taskid the id of the task which will be forwarded.
     * @param newRoleName the new group for the task.
     * @param newUserName the new user who gets the task.
     */
    public void forwardTask(String taskid, String newRoleName, String newUserName) {
        try {
            m_cms.forwardTask(Integer.parseInt(taskid), newRoleName, newUserName);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Displays the access control list of a given resource.<p>
     * 
     * @param resourceName the name of the resource
     */
    public void getAcl(String resourceName) {
        try {
            CmsAccessControlList acList = m_cms.getAccessControlList(resourceName);
            Iterator principals = acList.getPrincipals().iterator();
            while (principals.hasNext()) {
                I_CmsPrincipal p = m_cms.lookupPrincipal((CmsUUID)principals.next());
                System.out.println(p.getName() + ": " + acList.getPermissions(p).getPermissionString());
            }
        } catch (Exception e) {
            CmsShell.printException(e);
        }
    }

    /**
     * Displays all projects, which the user may access.
     */
    public void getAllAccessibleProjects() {
        try {
            Vector projects = m_cms.getAllAccessibleProjects();
            for (int i = 0; i < projects.size(); i++) {
                System.out.println(projects.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns the project history
     */
    public void getAllBackupProjects() {
        try {
            Vector projects = m_cms.getAllBackupProjects();
            for (int i = 0; i < projects.size(); i++) {
                System.out.println(projects.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns all projects which are owned by the current user or which are manageable
     * for the group of the user.
     */
    public void getAllManageableProjects() {
        try {
            Vector projects = m_cms.getAllManageableProjects();
            for (int i = 0; i < projects.size(); i++) {
                CmsProject project = (CmsProject)projects.elementAt(i);
                System.out.println(project);
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns  all I_CmsResourceTypes.
     */
    public void getAllResourceTypes() {
        try {
            List resourceTypes = m_cms.getAllResourceTypes();
            Iterator i = resourceTypes.iterator();
            while (i.hasNext()) {
                System.out.println(i.next());
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Gets information about the cache size.
     * <br>
     * The size of the following caching areas is returned:
     * <ul>
     *  <li>GroupCache</li>
     *  <li>UserGroupCache</li>
     *  <li>ResourceCache</li>
     *  <li>SubResourceCache</li>
     *  <li>ProjectCache</li>
     *  <li>PropertyCache</li>
     *  <li>PropertyDefinitionCache</li>
     *  <li>PropertyDefinitionVectorCache</li>
     * </ul>
     */
    public void getCacheInfo() {
        try {
            Hashtable cacheInfo = m_cms.getCacheInfo();
            Enumeration keys = cacheInfo.keys();
            while (keys.hasMoreElements()) {
                String key = (String)keys.nextElement();
                String info = (String)cacheInfo.get(key);
                System.out.println("\t" + key + ": " + info);
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns all child groups of a group<P/>
     *
     * @param groupname The name of the group.
     */
    public void getChild(String groupname) {
        try {
            Vector groups = m_cms.getChild(groupname);
            for (int i = 0; i < groups.size(); i++) {
                System.out.println(groups.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns all child groups of a group<P/>
     * This method also returns all sub-child groups of the current group.
     *
     * @param groupname The name of the group.
     */
    public void getChilds(String groupname) {
        try {
            Vector groups = m_cms.getChilds(groupname);
            for (int i = 0; i < groups.size(); i++) {
                System.out.println(groups.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns the current project for the user.
     */
    public void getCurrentProject() {
        System.out.println(m_cms.getRequestContext().currentProject().toString());
    }

    /**
     * Displays all groups to which a given user directly belongs.
     *
     * @param username the name of the user to get all groups for.
     */
    public void getDirectGroupsOfUser(String username) {
        try {
            Vector groups = m_cms.getDirectGroupsOfUser(username);
            for (int i = 0; i < groups.size(); i++) {
                System.out.println(groups.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns a Vector with all subfiles.<BR/>
     *
     * @param foldername the complete path to the folder.
     */
    public void getFilesInFolder(String foldername) {
        try {
            List files = m_cms.getFilesInFolder(foldername);
            for (int i = 0; i < files.size(); i++) {
                System.out.println(files.get(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns a Vector with all resource-names of the resources that have set the given property to the given value.
     *
     * @param propertyDefinition the name of the property-definition to check.
     * @param propertyValue the value of the property for the resource.
     */
    public void getFilesWithProperty(String propertyDefinition, String propertyValue) {
        try {
            Vector files = m_cms.getFilesWithProperty(propertyDefinition, propertyValue);
            for (int i = 0; i < files.size(); i++) {
                System.out.println(files.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }
        
    /**
     * Returns all users of the cms.
     */
    public void getGroups() {
        try {
            Vector groups = m_cms.getGroups();
            for (int i = 0; i < groups.size(); i++) {
                System.out.println(groups.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns all groups of a user.
     *
     * @param username The name of the user.
     */
    public void getGroupsOfUser(String username) {
        try {
            Vector groups = m_cms.getGroupsOfUser(username);
            for (int i = 0; i < groups.size(); i++) {
                System.out.println(groups.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Prints out all files for a module.
     *
     * @param name The file-name of module to import.
     */
    public void getModuleFiles(String name) {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            Vector names = new Vector();
            Vector codes = new Vector();
            reg.getModuleFiles(name, names, codes);
            for (int i = 0; i < names.size(); i++) {
                System.out.print(names.elementAt(i));
                System.out.print(" -> ");
                System.out.println(codes.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Displays information about modules.
     */
    public void getModuleInfo() {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            java.util.Enumeration names = reg.getModuleNames();

            // print out the available modules
            while (names.hasMoreElements()) {
                String name = (String)names.nextElement();
                getModuleInfo(name);
            }
            System.out.println("\ngeneral stuff");
            System.out.println("\tall repositories: ");
            String[] repositories = reg.getRepositories();
            for (int i = 0; i < repositories.length; i++) {
                System.out.println("\t\t" + repositories[i]);
            }
            System.out.println("\tall views: ");
            java.util.Vector views = new java.util.Vector();
            java.util.Vector urls = new java.util.Vector();
            int max = reg.getViews(views, urls);
            for (int i = 0; i < max; i++) {
                System.out.println("\t\t" + views.elementAt(i) + " -> " + urls.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Prints out informations about a module.
     *
     * @param name the name of the module to get infos about.
     */
    public void getModuleInfo(String name) {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            if (reg.moduleExists(name)) {
                System.out.println("\nModule: " + name + " v" + reg.getModuleVersion(name));
                System.out.println("\tNice Name: " + reg.getModuleNiceName(name));
                System.out.println("\tDescription: " + reg.getModuleDescription(name));
                System.out.println("\tAuthor: " + reg.getModuleAuthor(name));
                System.out.println("\tUploaded by: " + reg.getModuleUploadedBy(name));
                System.out.println("\tEmail: " + reg.getModuleAuthorEmail(name));
                System.out.println("\tCreationdate: " + new java.util.Date(reg.getModuleCreateDate(name)));
                System.out.println("\tUploaddate: " + new java.util.Date(reg.getModuleUploadDate(name)));
                System.out.println("\tDocumentationPath: " + reg.getModuleDocumentPath(name));
                String[] repositories = reg.getModuleRepositories(name);
                System.out.println("\trepositories: ");
                if (repositories != null) {
                    for (int i = 0; i < repositories.length; i++) {
                        System.out.println("\t\t" + repositories[i]);
                    }
                }
                String[] parameters = reg.getModuleParameterNames(name);
                System.out.println("\tparameters: ");
                if (parameters != null) {
                    for (int i = 0; i < parameters.length; i++) {
                        System.out.print("\t\t" + parameters[i]);
                        System.out.print(" = " + reg.getModuleParameter(name, parameters[i]));
                        System.out.print(" is " + reg.getModuleParameterType(name, parameters[i]));
                        System.out.println("  (" + reg.getModuleParameterDescription(name, parameters[i]) + ")");
                    }
                }
                System.out.println("\tWiew name: " + reg.getModuleViewName(name));
                System.out.println("\tWiew url: " + reg.getModuleViewUrl(name));
                System.out.println("\tDependencies");
                java.util.Vector modules = new java.util.Vector();
                java.util.Vector min = new java.util.Vector();
                java.util.Vector max = new java.util.Vector();
                reg.getModuleDependencies(name, modules, min, max);
                for (int i = 0; i < modules.size(); i++) {
                    System.out.println("\t\t" + modules.elementAt(i) + ": min v" + min.elementAt(i) + " max v" + max.elementAt(i));
                }
            } else {
                System.out.println("No module with name " + name);
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns the parent group of a group.
     *
     * @param groupname the name of the group.
     */
    public void getParent(String groupname) {
        try {
            System.out.println(m_cms.getParent(groupname));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Displays the permissions of the current user on a given resource
     * 
     * @param resourceName the name of the resource
     */
    public void getPermissions(String resourceName) {
        try {
            System.out.println(m_cms.getPermissions(resourceName).getPermissionString());
        } catch (Exception e) {
            CmsShell.printException(e);
        }
    }

    /**
     * Displays the current permissions of a user on a given resource.<p>
     * 
     * @param resourceName the name of the resource
     * @param userName the name of the user
     */
    public void getPermissions(String resourceName, String userName) {
        try {
            System.out.println(m_cms.getPermissions(resourceName, userName).getPermissionString());
        } catch (Exception e) {
            CmsShell.printException(e);
        }
    }

    /**
     * Returns a Vector with the subresources for a folder.<br>
     *
     * @param folder The name of the folder to get the subresources from.
     */
    public void getResourcesInFolder(String folder) {
        try {
            Vector res = m_cms.getResourcesInFolder(folder);
            for (int i = 0; i < res.size(); i++) {
                System.out.println(res.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns a Vector with all resources of the given type that have set the given property
     * to the given value.
     *
     * @param propertyDefinition the name of the property-definition to check.
     * @param propertyValue the value of the property for the resource.
     * @param resourceType the type of the resource
     */
    public void getResourcesWithProperty(String propertyDefinition, String propertyValue, String resourceType) {
        try {
            Vector resources = m_cms.getResourcesWithPropertyDefintion(propertyDefinition, propertyValue, m_cms.getResourceTypeId(resourceType));
            for (int i = 0; i < resources.size(); i++) {
                System.out.println(resources.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns a Vector with all subfolders.<BR/>
     *
     * @param foldername the complete path to the folder.
     */
    public void getSubFolders(String foldername) {
        try {
            List folders = m_cms.getSubFolders(foldername);
            for (int i = 0; i < folders.size(); i++) {
                System.out.println(folders.get(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns a value for a system-key.<p>
     * 
     * E.g. <code>&lt;system&gt;&lt;mailserver&gt;mail.server.com&lt;/mailserver&gt;&lt;/system&gt;</code>
     * can be requested via <code>getSystemValue("mailserver");</code> and returns "mail.server.com.
     *
     * @param key the key of the system-value
     */
    public void getSystemValue(String key) {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            System.out.println(reg.getSystemValue(key));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns all values for a system-key.
     *
     * @param sysKey the key of the system-value.
     */
    public void getSystemValues(String sysKey) {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            Hashtable res = reg.getSystemValues(sysKey);
            Enumeration keys = res.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                System.out.println(key + "->" + res.get(key));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Get a parameter value for a task.
     *
     * @param taskid the id of the task.
     * @param parname the name of the parameter.
     */
    public void getTaskPar(String taskid, String parname) {
        try {
            System.out.println(m_cms.getTaskPar(Integer.parseInt(taskid), parname));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Get the template task id fo a given taskname.
     *
     * @param taskname the name of the task.
     */
    public void getTaskType(String taskname) {
        try {
            System.out.println(m_cms.getTaskType(taskname));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns all users of the cms.
     */
    public void getUsers() {
        try {
            Vector users = m_cms.getUsers();
            for (int i = 0; i < users.size(); i++) {
                System.out.println(users.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns all users of the given type in the Cms.
     *
     * @param type the type of the users.
     */
    public void getUsers(String type) {
        try {
            Vector users = m_cms.getUsers(Integer.parseInt(type));
            for (int i = 0; i < users.size(); i++) {
                System.out.println(users.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns all users matching the given criteria.<p>
     *
     * @param Lastname the lastname of the users
     * @param userType either "systemuser" or "webuser"
     * @param userStatus either "enabled" or "disabled"
     * @param wasLoggedIn "never" or "once" or "whatever"
     * @param nMax maximum number of user entries to return
     */
    public void getUsersByLastname(String Lastname, String userType, String userStatus, String wasLoggedIn, String nMax) {
        int iUserType = 0;
        int iUserStatus = 0;
        int iWasLoggedIn = 0;
        int iNMax = 0;

        if (userType.equalsIgnoreCase("webuser")) {
            iUserType = I_CmsConstants.C_USER_TYPE_WEBUSER;
        } else if (userType.equalsIgnoreCase("systemuser")) {
            iUserType = I_CmsConstants.C_USER_TYPE_SYSTEMUSER;
        } else {
            System.out.println("second parameter has to be a \"webuser\" or" + " \"systemuser\"!");
            return;
        }

        if (userStatus.equalsIgnoreCase("enabled")) {
            iUserStatus = I_CmsConstants.C_FLAG_ENABLED;
        } else if (userStatus.equalsIgnoreCase("disabled")) {
            iUserStatus = I_CmsConstants.C_FLAG_DISABLED;
        } else {
            System.out.println("third parameter has to be a \"enabled\" or" + " \"disabled\"!");
            return;
        }

        if (wasLoggedIn.equalsIgnoreCase("never")) {
            iWasLoggedIn = I_CmsConstants.C_NEVER;
        } else if (wasLoggedIn.equalsIgnoreCase("once")) {
            iWasLoggedIn = I_CmsConstants.C_AT_LEAST_ONCE;
        } else if (wasLoggedIn.equalsIgnoreCase("whatever")) {
            iWasLoggedIn = I_CmsConstants.C_WHATEVER;
        } else {
            System.out.println("fourth parameter has to be a \"never\"," + " \"once\" or \"whatever\"!");
            return;
        }

        try {
            iNMax = Integer.parseInt(nMax);
        } catch (NumberFormatException e) {
            System.out.println("last parameter has to be a number!");
            return;
        }

        try {
            Vector users = m_cms.getUsersByLastname(Lastname, iUserType, iUserStatus, iWasLoggedIn, iNMax);
            for (int i = 0; i < users.size(); i++) {
                System.out.println(users.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns all groups of a user.
     *
     * @param groupname The name of the group.
     */
    public void getUsersOfGroup(String groupname) {
        try {
            Vector users = m_cms.getUsersOfGroup(groupname);
            for (int i = 0; i < users.size(); i++) {
                System.out.println(users.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns the mapping of views to actions.
     */
    public void getViews() {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            java.util.Vector views = new java.util.Vector();
            java.util.Vector urls = new java.util.Vector();
            int max = reg.getViews(views, urls);
            for (int i = 0; i < max; i++) {
                System.out.println(views.elementAt(i) + " -> " + urls.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Prints all possible commands.
     */
    public void help() {
        Method meth[] = getClass().getMethods();
        for (int z = 0; z < meth.length; z++) {
            if ((meth[z].getDeclaringClass() == getClass()) && (meth[z].getModifiers() == Modifier.PUBLIC)) {
                CmsShell.printMethod(meth[z]);
            }
        }
    }

    /**
     * Prints signature of all possible commands containing a certain string.<br>
     * May also be used to print signature of a specific command by giving full command name.
     *
     * @param searchString The String to search for in the commands
     */
    public void help(String searchString) {
        if (searchString.equals("help")) {
            printHelpText();
        } else {
            Method meth[] = getClass().getMethods();
            for (int z = 0; z < meth.length; z++) {
                if ((meth[z].getDeclaringClass() == getClass()) && (meth[z].getModifiers() == Modifier.PUBLIC) && (meth[z].getName().toLowerCase().indexOf(searchString.toLowerCase()) > -1)) {
                    CmsShell.printMethod(meth[z]);
                }
            }
        }
    }

    /**
     * Reads a given file from the local harddisk and uploads
     * it to the OpenCms system.
     * Used in the OpenCms console only.
     *
     * @param filename Local file to be uploaded.
     * @return Byte array containing the file content.
     * @throws CmsException if something goes wrong
     */
    private byte[] importFile(String filename) throws CmsException {
        File file = null;
        long len = 0;
        FileInputStream importInput = null;
        byte[] result;

        // First try to load the file
        try {
            file = new File(filename);
        } catch (Exception e) {
            file = null;
        }
        if (file == null) {
            throw new CmsException("Could not load local file " + filename, CmsException.C_NOT_FOUND);
        }

        // File was loaded successfully.

        // Now try to read the content.
        try {
            len = file.length();
            result = new byte[(int)len];
            importInput = new FileInputStream(file);
            importInput.read(result);
            importInput.close();
        } catch (Exception e) {
            throw new CmsException(e.toString(), CmsException.C_UNKNOWN_EXCEPTION);
        }
        return result;
    }

    /**
     * Imports a import-resource (folder or zipfile) to the cms.
     *
     * @param importFile the name (absolute Path) of the import resource (zip or folder)
     * @param importPath the name (absolute Path) of folder in which should be imported
     */
    public void importFolder(String importFile, String importPath) {

        // import the resources
        try {
            m_cms.importFolder(importFile, importPath);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Checks for conflicting file names for a import.
     *
     * @param moduleZip The file-name of module to import.
     */
    public void importGetConflictingFileNames(String moduleZip) {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            Vector conflicts = reg.importGetConflictingFileNames(moduleZip);
            System.out.println("Conflicts: " + conflicts.size());
            for (int i = 0; i < conflicts.size(); i++) {
                System.out.println(conflicts.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Checks for resources that should be copied to the import-project.
     *
     * @param moduleZip The file-name of module to import.
     */
    public void importGetResourcesForProject(String moduleZip) {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            Vector resources = reg.importGetResourcesForProject(moduleZip);
            System.out.println("Resources: " + resources.size());
            for (int i = 0; i < resources.size(); i++) {
                System.out.println(resources.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Imports a module (zipfile) to the cms.
     *
     * @param importFile the name (complete Path) of the import module
     */
    public void importModule(String importFile) {

        // import the module
        try {
            CmsRegistry reg = m_cms.getRegistry();
            reg.importModule(importFile, new Vector(), new CmsShellReport());
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Imports a module (zipfile) from the default module directory, creating a temporary project
     * for this.
     *
     * @param importFile the name of the import module located in the default module directory
     */
    public void importModuleFromDefault(String importFile) {
        // build the complete filename
        String exportPath = null;
        try {
            exportPath = m_cms.readPackagePath();
        } catch (CmsException e) {
            CmsShell.printException(e);
            return;
        }
        String fileName = CmsBase.getAbsolutePath(exportPath) + CmsRegistry.C_MODULE_PATH + importFile;
        System.out.println("Importing module: " + fileName);
        // import the module
        try {
            // create a temporary project for the import
            CmsProject project = m_cms.createProject(
                "ModuleImport", 
                "A temporary project to import the module " + importFile, 
                OpenCms.getDefaultUsers().getGroupAdministrators(), 
                OpenCms.getDefaultUsers().getGroupAdministrators(), 
                I_CmsConstants.C_PROJECT_TYPE_TEMPORARY
            );
            int id = project.getId();
            m_cms.getRequestContext().setCurrentProject(id);
            m_cms.getRequestContext().saveSiteRoot();
            m_cms.getRequestContext().setSiteRoot("/");
            m_cms.copyResourceToProject("/");
            m_cms.getRequestContext().restoreSiteRoot();
            // import the module
            CmsRegistry reg = m_cms.getRegistry();
            reg.importModule(fileName, new Vector(), new CmsShellReport());
            // finally publish the project
            m_cms.unlockProject(id);
            m_cms.publishProject();
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Imports an import-resource (folder or zipfile) to the cms.
     * Creation date: (09.08.00 16:28:48)
     * @param importFile java.lang.String the name (absolute Path) of the import resource (zip or folder)
     */
    public void importResources(String importFile) {

        // import the resources
        try {
            m_cms.importResources(importFile, I_CmsConstants.C_ROOT);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Imports an import-resource (folder or zipfile) to the cms.
     *
     * @param importFile the name (absolute Path) of the import resource (zip or folder)
     * @param importPath the name (absolute Path) of folder in which should be imported
     */
    public void importResources(String importFile, String importPath) {

        // import the resources
        try {
            m_cms.importResources(importFile, importPath);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Imports an import-resource (folder or zipfile) to the cms,
     * and generates a special temp project for that import which is published right 
     * after the file has successfully been imported.
     * @param importFile java.lang.String the name (absolute Path) of the import resource (zip or folder)
     */
    public void importResourcesWithTempProject(String importFile) {
        // import the resources
        try {
            CmsProject project = m_cms.createProject(
                "SystemUpdate", 
                "A temporary project for a system update", 
                OpenCms.getDefaultUsers().getGroupAdministrators(), 
                OpenCms.getDefaultUsers().getGroupAdministrators(), 
                I_CmsConstants.C_PROJECT_TYPE_TEMPORARY
            );
            int id = project.getId();
            m_cms.getRequestContext().setCurrentProject(id);
            m_cms.copyResourceToProject(I_CmsConstants.C_ROOT);
            m_cms.importResources(importFile, I_CmsConstants.C_ROOT);
            m_cms.unlockProject(id);
            m_cms.publishProject();
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Determines, if the user is Admin.
     */
    public void isAdmin() {
        try {
            System.out.println(m_cms.getRequestContext().isAdmin());
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Determines, if the user is Projectleader.
     */
    public void isProjectManager() {
        try {
            System.out.println(m_cms.getRequestContext().isProjectManager());
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns the user, who has locked a given resource.
     * <br>
     * A user can lock a resource, so he is the only one who can write this
     * resource. This methods checks, who has locked a resource.
     *
     * @param resource The complete path to the resource.
     *
     */
    public void lockedBy(String resource) {
        try {
            System.out.println(m_cms.lockedBy(resource));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Locks a resource<BR/>
     *
     * A user can lock a resource, so he is the only one who can write this
     * resource.
     *
     * @param resource The complete path to the resource to lock.
     */
    public void lockResource(String resource) {
        try {
            m_cms.lockResource(resource);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Locks a given resource.
     * <br>
     * A user can lock a resource, so he is the only one who can write this
     * resource.
     *
     * @param resource the complete path to the resource to lock.
     * @param force if force is <code>true</code>, a existing locking will be overwritten.
     */
    public void lockResource(String resource, String force) {
        try {

            //m_cms.lockResource(resource, Boolean.getBoolean(force));
            if (force.toLowerCase().equals("true")) {
                m_cms.lockResource(resource, true);
            } else {
                m_cms.lockResource(resource, false);
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Logs a user into the system.
     *
     * @param username The name of the user to log in.
     * @param password The password.
     */
    public void login(String username, String password) {
        loginUser(username, password);
    }

    /**
     * Logs a user into the system.<p>
     *
     * @param username the name of the user to log in
     * @param password the password
     */
    public void loginUser(String username, String password) {
        try {
            m_cms.loginUser(username, password);
            whoami();
        } catch (Exception exc) {
            CmsShell.printException(exc);
            System.out.println("Login failed!");
        }
    }

    /**
     * Logs a user with a given remote address into the system.<p>
     *
     * @param username the name of the user to log in
     * @param password the password
     * @param remoteAddress the ip address
     */
    public void loginUser(String username, String password, String remoteAddress) {
        try {
            m_cms.loginUser(username, password, remoteAddress);
            whoami();
        } catch (Exception exc) {
            CmsShell.printException(exc);
            System.out.println("Login failed!");
        }
    }
    
    /**
     * Logs a web user into the Cms, if the password is correct.
     *
     * @param username the name of the user.
     * @param password the password of the user.
     */
    public void loginWebUser(String username, String password) {
        try {
            System.out.println(m_cms.loginWebUser(username, password));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Lists the access control entries of a given resource.<p>
     * 
     * @param resourceName the name of the resource
     */
    public void lsacc(String resourceName) {
        try {
            Vector acList = m_cms.getAccessControlEntries(resourceName);
            for (int i = 0; i < acList.size(); i++) {
                CmsAccessControlEntry ace = (CmsAccessControlEntry)acList.elementAt(i);
                I_CmsPrincipal acePrincipal = m_cms.lookupPrincipal(ace.getPrincipal());
                if (true) {
                    String pName = (acePrincipal != null) ? acePrincipal.getName() : ace.getPrincipal().toString();
                    System.out.println(pName + ": " + ace.getPermissions().getPermissionString() + " " + ace);
                }
            }
        } catch (Exception e) {
            CmsShell.printException(e);
        }
    }

    /**
     * Lists the access control entries belonging to the given principal.<p>
     *  
     * @param resourceName the name of the resource
     * @param principalName the name of the principal
     */
    public void lsacc(String resourceName, String principalName) {
        try {
            I_CmsPrincipal principal = m_cms.lookupPrincipal(principalName);
            Vector acList = m_cms.getAccessControlEntries(resourceName);
            for (int i = 0; i < acList.size(); i++) {
                CmsAccessControlEntry ace = (CmsAccessControlEntry)acList.elementAt(i);
                I_CmsPrincipal acePrincipal = m_cms.lookupPrincipal(ace.getPrincipal());
                if (acePrincipal.equals(principal)) {
                    String pName = (acePrincipal != null) ? acePrincipal.getName() : ace.getPrincipal().toString();
                    System.out.println(pName + ": " + ace.getPermissions().getPermissionString() + " " + ace);
                }
            }
        } catch (Exception e) {
            CmsShell.printException(e);
        }
    }

    /**
     * Moves a file to the given destination.
     *
     * @param source the complete path of the sourcefile.
     * @param destination the complete path of the destinationfile.
     */
    public void moveFile(String source, String destination) {
        try {
            m_cms.moveResource(source, destination);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Moves a resource to the given destination.
     *
     * @param source the complete path of the sourcefile.
     * @param destination the complete path of the destinationfile.
     */
    public void moveResource(String source, String destination) {
        try {
            m_cms.moveResource(source, destination);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Prints help text when Shell is startet.
     */
    public void printHelpText() {
        System.out.println("help              Gives a list of available commands with signature");
        System.out.println("help <command>    Shows signature of command");
        System.out.println("help <substring>  Lists only those commands containing this substring");
        System.out.println("help help         Prints this text");
        System.out.println("exit or quit      Leaves the Shell");
        System.out.println("");
    }

    /**
     * Publishes a project.
     *
     * @param id The id of the project to be published.
     */
    public void publishProject(String id) {
        try {
            int projectId = Integer.parseInt(id);
            m_cms.unlockProject(projectId);
            m_cms.publishProject();
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Publishes a resource in the current project.
     *
     * @param resourceName The name of the resource to be published.
     */
    public void publishResource(String resourceName) {
        try {
            m_cms.publishResource(resourceName);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    // All methods, that may be called by the user:

    /**
     * Exits the commandline-interface
     */
    public void quit() {
        exit();
    }

    /**
     * Reads the agent of a task from the OpenCms.
     *
     * @param taskId the task to read the agent from.
     */
    public void readAgent(String taskId) {
        try {
            CmsTask task = m_cms.readTask(Integer.parseInt(taskId));
            if (task == null) {
                System.out.println("No task: " + task);
            } else {
                System.out.println(m_cms.readAgent(task));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads all file headers of a file in the OpenCms.<BR>
     * This method returns a vector with the histroy of all file headers, i.e.
     * the file headers of a file, independent of the project they were attached to.<br>
     *
     * The reading excludes the filecontent.
     *
     * @param filename The name of the file to be read.
     */
    public void readAllFileHeadersForHist(String filename) {
        try {
            List files = m_cms.readAllBackupFileHeaders(filename);
            for (int i = 0; i < files.size(); i++) {
                System.out.println(files.get(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads all propertydefinitions for the given resource type.<p>
     *
     * @param resourcetype The name of the resource type to read the
     *         propertydefinitions for.
     */
    public void readAllPropertydefinitions(String resourcetype) {
        try {
            Vector propertydefs = m_cms.readAllPropertydefinitions(resourcetype);
            for (int i = 0; i < propertydefs.size(); i++) {
                System.out.println(propertydefs.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Gets the Crontable.
     */
    public void readCronTable() {
        try {
            System.out.println(m_cms.readCronTable());
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads the export-path for the system.
     * This path is used for db-export and db-import.
     *
     * @returns the exportpath.
     */
    public void readExportPath() {
        try {
            System.out.println(m_cms.readPackagePath());
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads a file from the Cms.<BR/>
     *
     * @param filename The complete path to the file
     */
    public void readFile(String filename) {
        try {
            System.out.println(m_cms.readFile(filename));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads a file from the Cms.<BR/>
     *
     * @param filename The complete path to the file
     */
    public void readFileContent(String filename) {
        try {
            System.out.println(m_cms.readFile(filename));
            System.out.println(new String(m_cms.readFile(filename).getContents()));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Gets the known file extensions (=suffixes).
     */
    public void readFileExtensions() {
        try {
            Hashtable extensions = m_cms.readFileExtensions();
            Enumeration keys = extensions.keys();
            while (keys.hasMoreElements()) {
                String key = (String)keys.nextElement();
                String ext = (String)extensions.get(key);
                System.out.println(key + ": " + ext);
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads a file header from the Cms.<BR/>
     * The reading excludes the filecontent.
     *
     * @param filename The complete path of the file to be read.
     */
    public void readFileHeader(String filename) {
        try {
            System.out.println(m_cms.readFileHeader(filename));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads all file headers of a project from the Cms.
     *
     * @param projectId the id of the project to read the file headers for.
     */
    public void readFileHeaders(String projectId) {
        try {
            Vector files = m_cms.readFileHeaders(Integer.parseInt(projectId));
            for (int i = 0; i < files.size(); i++) {
                System.out.println(files.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads a folder from the Cms.<BR/>
     *
     * @param folder The complete path to the folder that will be read.
     */
    public void readFolder(String folder) {
        try {
            System.out.println(m_cms.readFolder(folder));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads all given tasks from a user for a project.
     *
     * @param projectId the id of the project in which the tasks are defined.
     * @param ownerName the owner of the task.
     * @param taskType the type of task you want to read: C_TASKS_ALL, C_TASKS_OPEN, C_TASKS_DONE, C_TASKS_NEW.
     * @param orderBy specifies how to order the tasks.
     * @param sort specifies the sort order
     */
    public void readGivenTasks(String projectId, String ownerName, String taskType, String orderBy, String sort) {
        try {
            Vector tasks = m_cms.readGivenTasks(Integer.parseInt(projectId), ownerName, Integer.parseInt(taskType), orderBy, sort);
            for (int i = 0; i < tasks.size(); i++) {
                System.out.println(tasks.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns a group in the Cms.
     *
     * @param groupname The name of the group to be returned.
     */
    public void readGroup(String groupname) {
        try {
            System.out.println(m_cms.readGroup(groupname));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns a group in the Cms.
     *
     * @param groupId The id of the group to be returned.
     */
    public void readGroupById(String groupId) {
        try {
            System.out.println(m_cms.readGroup(new CmsUUID(groupId)));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads the group of a project from the OpenCms.
     * 
     * @param project the id of the project
     */
    public void readGroupOfProject(String project) {
        try {
            System.out.println(m_cms.readGroup(m_cms.readProject(Integer.parseInt(project))));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads the group (role) of a task from the Cms.
     *
     * @param task the task to read the role from.
     */
    public void readGroupOfTask(String task) {
        try {
            System.out.println(m_cms.readGroup(m_cms.readTask(Integer.parseInt(task))));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads the managergroup of a project from the Cms.
     * 
     * @param project the id of the project
     */
    public void readManagerGroup(String project) {
        try {
            System.out.println(m_cms.readManagerGroup(m_cms.readProject(Integer.parseInt(project))));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Gets all mime types.
     */
    public void readMimeTypes() {
        try {
            Hashtable mimeTypes = m_cms.readMimeTypes();
            Enumeration keys = mimeTypes.keys();
            String key;
            while (keys.hasMoreElements()) {
                key = (String)keys.nextElement();
                System.out.println(key + " : " + mimeTypes.get(key));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads the original agent of a task from the Cms.
     *
     * @param task the task to read the original agent from.
     */
    public void readOriginalAgent(String task) {
        try {
            System.out.println(m_cms.readOriginalAgent(m_cms.readTask(Integer.parseInt(task))));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads the owner of a project from the Cms.
     * 
     * @param project the id of the project
     */
    public void readOwnerOfProject(String project) {
        try {
            System.out.println(m_cms.readOwner(m_cms.readProject(Integer.parseInt(project))));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads the owner of a resource from the Cms.
     * 
     * @param resource the name of the resource
     */
    public void readOwnerOfResource(String resource) {
        try {
            System.out.println(m_cms.readOwner(m_cms.readFileHeader(resource)));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads the owner (initiator) of a task from the Cms.
     *
     * @param task The task to read the owner from.
     */
    public void readOwnerOfTask(String task) {
        try {
            System.out.println(m_cms.readOwner(m_cms.readTask(Integer.parseInt(task))));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads a project from the Cms.
     *
     * @param id The id of the project to read.
     */
    public void readProject(String id) {
        try {
            int projectId = Integer.parseInt(id);
            System.out.println(m_cms.readProject(projectId).toString());
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads log entries for a project.
     *
     * @param projectId the id of the project for which the tasklog will be read.
     */
    public void readProjectLogs(String projectId) {
        try {
            Vector logs = m_cms.readProjectLogs(Integer.parseInt(projectId));
            for (int i = 0; i < logs.size(); i++) {
                System.out.println(logs.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads a project from the Cms.
     *
     * @param res The resource to read the project for.
     */
    public void readProjectOfResource(String res) {
        try {
            System.out.println(m_cms.readProject(m_cms.readFileHeader(res)));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads a project from the Cms.
     *
     * @param task the task for which the project will be read.
     */
    public void readProjectOfTask(String task) {
        try {
            System.out.println(m_cms.readProject(m_cms.readTask(Integer.parseInt(task))));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns a list of all propertyinformations of a file or folder.
     *
     * @param resource The name of the resource of which the propertyinformation has to be
     * read.
     */
    public void readProperties(String resource) {
        try {
            Map properties = m_cms.readProperties(resource);
            Iterator i = properties.keySet().iterator();
            Object key;
            while (i.hasNext()) {
                key = i.next();
                System.out.print(key + "=");
                System.out.println(properties.get(key));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns a propertyinformation of a file or folder.
     *
     * @param name The resource-name of which the propertyinformation has to be read.
     * @param property The propertydefinition-name of which the propertyinformation has to be read.
     */
    public void readProperty(String name, String property) {
        try {
            System.out.println(m_cms.readProperty(name, property));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads the propertydefinition for the resource type.<BR/>
     *
     * @param name The name of the propertydefinition to read.
     * @param resourcetype The name of the resource type for the propertydefinition.
     */
    public void readPropertydefinition(String name, String resourcetype) {
        try {
            System.out.println(m_cms.readPropertydefinition(name, m_cms.getResourceTypeId(resourcetype)));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns the resources that contains the given part in the resourcename.<br>
     *
     * <B>Security:</B>
     * Access is granted, if:
     * <ul>
     * <li>the user has access to the project</li>
     * <li>the user can read and view this resource</li>
     * </ul>
     *
     * @param resourcename A part of resourcename
     */
    public void readResourcesLikeName(String resourcename) {
        try {
            Vector resources = m_cms.readResourcesLikeName(resourcename);
            for (int i = 0; i < resources.size(); i++) {
                CmsResource res = (CmsResource)resources.elementAt(i);
                System.out.println(res.toString());
            }
        } catch (Exception e) {
            CmsShell.printException(e);
        }
    }

    /**
     * Reads the task with the given id.
     *
     * @param id the id of the task to be read.
     */
    public void readTask(String id) {
        try {
            System.out.println(m_cms.readTask(Integer.parseInt(id)));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads log entries for a task.
     *
     * @param taskid the task for which the tasklog will be read.
     */
    public void readTaskLogs(String taskid) {
        try {
            Vector logs = m_cms.readTaskLogs(Integer.parseInt(taskid));
            for (int i = 0; i < logs.size(); i++) {
                System.out.println(logs.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads all tasks for a project.
     *
     * @param projectId the id of the project in which the tasks are defined. Can be null to select all tasks.
     * @param tasktype the type of task you want to read: C_TASKS_ALL, C_TASKS_OPEN, C_TASKS_DONE, C_TASKS_NEW
     * @param orderBy specifies how to order the tasks.
     * @param sort sort order: C_SORT_ASC, C_SORT_DESC, or null.
     */
    public void readTasksForProject(String projectId, String tasktype, String orderBy, String sort) {
        try {
            Vector tasks = m_cms.readTasksForProject(Integer.parseInt(projectId), Integer.parseInt(tasktype), orderBy, sort);
            for (int i = 0; i < tasks.size(); i++) {
                System.out.println(tasks.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads all tasks for a role in a project.
     *
     * @param projectId the id of the Project in which the tasks are defined.
     * @param roleName the role who has to process the task.
     * @param tasktype the type of task you want to read: C_TASKS_ALL, C_TASKS_OPEN, C_TASKS_DONE, C_TASKS_NEW.
     * @param orderBy specifies how to order the tasks.
     * @param sort sort order C_SORT_ASC, C_SORT_DESC, or null
     */
    public void readTasksForRole(String projectId, String roleName, String tasktype, String orderBy, String sort) {
        try {
            Vector tasks = m_cms.readTasksForRole(Integer.parseInt(projectId), roleName, Integer.parseInt(tasktype), orderBy, sort);
            for (int i = 0; i < tasks.size(); i++) {
                System.out.println(tasks.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Reads all tasks for a user in a project.
     *
     * @param projectId the id of the Project in which the tasks are defined.
     * @param userName the user who has to process the task.
     * @param tasktype the type of task you want to read: C_TASKS_ALL, C_TASKS_OPEN, C_TASKS_DONE, C_TASKS_NEW.
     * @param orderBy specifies how to order the tasks.
     * @param sort sort order C_SORT_ASC, C_SORT_DESC, or null
     */
    public void readTasksForUser(String projectId, String userName, String tasktype, String orderBy, String sort) {
        try {
            Vector tasks = m_cms.readTasksForUser(Integer.parseInt(projectId), userName, Integer.parseInt(tasktype), orderBy, sort);
            for (int i = 0; i < tasks.size(); i++) {
                System.out.println(tasks.elementAt(i));
            }
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns a user object.<P/>
     *
     * @param username The name of the user that is to be read.
     */
    public void readUser(String username) {
        try {
            System.out.println(m_cms.readUser(username));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns a user object.<P/>
     *
     * @param username The name of the user that is to be read.
     * @param password the password of this user
     */
    public void readUser(String username, String password) {
        try {
            System.out.println(m_cms.readUser(username, password));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns a user object.<P/>
     *
     * @param username The name of the user that is to be read.
     */
    public void readWebUser(String username) {
        try {
            System.out.println(m_cms.readWebUser(username));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns a user object.<P/>
     *
     * @param username The name of the user that is to be read.
     * @param password the password of this user
     */
    public void readWebUser(String username, String password) {
        try {
            System.out.println(m_cms.readWebUser(username, password));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }
    /**
     * Reactivates a task from the Cms.
     *
     * @param taskId the Id of the task to accept.
     */
    public void reaktivateTask(String taskId) {
        try {
            m_cms.reaktivateTask(Integer.parseInt(taskId));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Recovers the password for a user.
     *
     * @param username The name of the user.
     * @param recPassword The recover password to check the access.
     * @param newPassword The new password.
     */
    public void recoverPassword(String username, String recPassword, String newPassword) {
        try {
            m_cms.recoverPassword(username, recPassword, newPassword);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Removes a user from a group.
     *
     * @param username The name of the user that is to be removed from the group.
     * @param groupname The name of the group.
     */
    public void removeUserFromGroup(String username, String groupname) {
        try {
            m_cms.removeUserFromGroup(username, groupname);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Renames the file to the new name.
     *
     * @param oldname The complete path to the resource which will be renamed.
     * @param newname The new name of the resource (No path information allowed).
     */
    public void renameFile(String oldname, String newname) {
        try {
            m_cms.renameResource(oldname, newname);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Renames the resource to the new name.
     *
     * @param oldname The complete path to the resource which will be renamed.
     * @param newname The new name of the resource (No path information allowed).
     */
    public void renameResource(String oldname, String newname) {
        try {
            m_cms.renameResource(oldname, newname);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Restores a file in the current project with a version in the backup
     *
     * @param versionId The version id of the resource
     * @param filename The name of the file to restore
     *
     * @throws CmsException  Throws CmsException if operation was not succesful.
     */
    public void restoreResource(String versionId, String filename) throws CmsException {
        try {
            m_cms.restoreResource(Integer.parseInt(versionId), filename);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Removes an access control entry of a given resource for a given user/group.<p>
     * 
     * @param resourceName the name of the resource
     * @param principalType the principalType (user or group)
     * @param principalName the name of the principal
     */
    public void rmacc(String resourceName, String principalType, String principalName) {
        try {
            m_cms.rmacc(resourceName, principalType, principalName);
        } catch (Exception e) {
            CmsShell.printException(e);
        }
    }

    /**
     * Returns the root-folder object.
     */
    public void rootFolder() {
        try {
            System.out.println(m_cms.rootFolder());
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Starts a new cron job with the given index.<p>
     * 
     * @param no the job index within the cron table
     */
    public void startCronJob(String no) {
        try {
            int index = Integer.parseInt(no);
            CmsCronTable cronTable = new CmsCronTable(m_cms.readCronTable());
            CmsCronEntry cronJob = cronTable.get(index);
            OpenCmsCore.getInstance().startScheduleJob(cronJob);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }    
    } 
        
    /**
     * Sets the name of the current site root
     * of the content objects system
     */
    public void setContextToCos() {
        m_cms.setContextToCos();
    }

    /**
     * Sets the current project for the user.
     *
     * @param id The id of the project to be set as current.
     */
    public void setCurrentProject(String id) {
        try {
            int projectId = Integer.parseInt(id);
            System.out.println(m_cms.getRequestContext().setCurrentProject(projectId).toString());
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * This method sets the author of the module.
     *
     * @param modulename the name of the module.
     * @param author the name of the author.
     */
    public void setModuleAuthor(String modulename, String author) {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            reg.setModuleAuthor(modulename, author);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * This method sets the email of author of the module.
     *
     * @param modulename the name of the module.
     * @param email the email of author of the module.
     */
    public void setModuleAuthorEmail(String modulename, String email) {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            reg.setModuleAuthorEmail(modulename, email);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Sets the create date of the module.
     *
     * @param modulname the name of the module.
     * @param createdate the create date of the module.
     */
    public void setModuleCreateDate(String modulname, String createdate) {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            long date = Long.parseLong(createdate);
            reg.setModuleCreateDate(modulname, date);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Sets the description of the module.
     *
     * @param module the name of the module.
     * @param description the description of the module.
     */
    public void setModuleDescription(String module, String description) {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            reg.setModuleDescription(module, description);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Sets the url to the documentation of the module.
     *
     * @param modulename the name of the module.
     * @param url the url to the documentation of the module.
     */
    public void setModuleDocumentPath(String modulename, String url) {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            reg.setModuleDocumentPath(modulename, url);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Sets the classname, that receives all maintenance-events for the module.
     *
     * @param modulname the name of the module.
     * @param classname class that receives all maintenance-events for the module.
     */
    public void setModuleMaintenanceEventClass(String modulname, String classname) {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            reg.setModuleMaintenanceEventClass(modulname, classname);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Sets the nice name of the module.
     *
     * @param module the name of the module.
     * @param nicename the nice name of the module.
     */
    public void setModuleNiceName(String module, String nicename) {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            reg.setModuleNiceName(module, nicename);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * This method sets the version of the module.
     *
     * @param modulename the name of the module.
     * @param version the version of the module.
     */
    public void setModuleVersion(String modulename, String version) {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            reg.setModuleVersion(modulename, version);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Sets a view for a module
     *
     * @param modulename the name of the module.
     * @param viewname the name of the view, that is implemented by the module.
     * @param viewurl the url of the view, that is implemented by the module.
     */
    public void setModuleView(String modulename, String viewname, String viewurl) {
        try {
            CmsRegistry reg = m_cms.getRegistry();
            reg.setModuleView(modulename, viewname, viewurl);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Set a new name for a task.
     *
     * @param taskId the id of the task.
     * @param name the new name of the task.
     */
    public void setName(String taskId, String name) {
        try {
            m_cms.setName(Integer.parseInt(taskId), name);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Sets a new parent-group for an already existing group in the Cms.
     *
     * @param groupName the name of the group that should be written to the Cms.
     * @param parentGroupName the name of the parentGroup to set, or null if the parent
     * group should be deleted.
     */
    public void setParentGroup(String groupName, String parentGroupName) {
        try {
            m_cms.setParentGroup(groupName, parentGroupName);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Sets the password for a user.
     *
     * @param username The name of the user.
     * @param newPassword The new password.
     */
    public void setPassword(String username, String newPassword) {
        try {
            m_cms.setPassword(username, newPassword);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Sets the password for a user.
     *
     * @param username The name of the user.
     * @param oldPassword The old password.
     * @param newPassword The new password.
     */
    public void setPassword(String username, String oldPassword, String newPassword) {
        try {
            m_cms.setPassword(username, oldPassword, newPassword);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Sets the priority of a task.
     *
     * @param taskId the id of the task.
     * @param priority the new priority value.
     */
    public void setPriority(String taskId, String priority) {
        try {
            m_cms.setPriority(Integer.parseInt(taskId), Integer.parseInt(priority));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Sets the recovery password for a user.
     *
     * @param username The name of the user.
     * @param oldPassword The password.
     * @param newPassword The new recovery password.
     */
    public void setRecoveryPassword(String username, String oldPassword, String newPassword) {
        try {
            m_cms.setRecoveryPassword(username, oldPassword, newPassword);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Set the site root.<p>
     * 
     * @param siteRoot the site root to set
     */
    public void setSiteRoot(String siteRoot) {
        m_cms.getRequestContext().setSiteRoot(siteRoot);
    }

    /**
     * Set a parameter for a task.
     *
     * @param taskid the Id of the task.
     * @param parname the ame of the parameter.
     * @param parvalue the value of the parameter.
     */
    public void setTaskPar(String taskid, String parname, String parvalue) {
        try {
            m_cms.setTaskPar(Integer.parseInt(taskid), parname, parvalue);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Sets the timeout of a task.
     *
     * @param taskId the id of the task.
     * @param timeout the new timeout value.
     */
    public void setTimeout(String taskId, String timeout) {
        try {
            m_cms.setTimeout(Integer.parseInt(taskId), Long.parseLong(timeout));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Echos the input to output.
     *
     * @param param The echo to be written to output.
     */
    public void shortException(String param) {
        if (param.toLowerCase().equals("on")) {
            CmsShell.m_shortException = true;
        } else {
            if (param.toLowerCase().equals("off")) {
                CmsShell.m_shortException = false;
            }
        }
    }

    /**
    * Synchronize cms-resources on virtual filesystem with the server filesystem.
    *
    * @param resourceName the name (absolute Path) of the resource that should be synchronized.
    */
    public void syncFolder(String resourceName) {
        // synchronize the resources
        try {
            m_cms.syncFolder(resourceName);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }
    /**
     * Undeletes the resource.
     *
     * @param resourcename The complete path of the resource.
     */
    public void undeleteResource(String resourcename) {
        try {
            m_cms.undeleteResource(resourcename);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Undo changes in a file by copying the online file.
     *
     * @param filename the complete path of the file.
     *
     * @throws CmsException if the file couldn't be deleted, or if the user
     * has not the appropriate rights to write the file.
     */
    public void undoChanges(String filename) throws CmsException {
        try {
            m_cms.undoChanges(filename);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Unlocks all resources of a project.
     *
     * @param id the id of the project to be unlocked.
     */
    public void unlockProject(String id) {
        try {
            m_cms.unlockProject(Integer.parseInt(id));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Unlocks a resource<BR/>
     *
     * A user can unlock a resource, so other users may lock this file.
     *
     * @param resource The complete path to the resource to lock.
     */
    public void unlockResource(String resource) {
        try {
            m_cms.unlockResource(resource, false);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Loads a File up to the cms from the lokal disc.
     *
     * @param lokalfile The lokal file to load up.
     * @param folder The folder in the cms to put the new file
     * @param filename The name of the new file.
     * @param type the filetype of the new file in the cms.
     */
    public void uploadFile(String lokalfile, String folder, String filename, String type) {
        try {
            System.out.println(m_cms.createResource(folder, filename, m_cms.getResourceTypeId(type), null, importFile(lokalfile)));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Checks if a user is member of a group.<P/>
     *
     * @param username The name of the user to check.
     * @param groupname The name of the group to check.
     */
    public void userInGroup(String username, String groupname) {
        try {
            System.out.println(m_cms.userInGroup(username, groupname));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Returns a version-string for this OpenCms.
     */
    public void version() {
        System.out.println(OpenCms.getVersionName());
    }

    /**
     * Returns the current user.
     */
    public void whoami() {
        System.out.println(m_cms.getRequestContext().currentUser());
    }

    /**
     * Writes the Crontable.
     * 
     * @param crontable a crontable entry
     */
    public void writeCronTable(String crontable) {
        try {
            m_cms.writeCronTable(crontable);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Writes the export-path for the system.
     * This path is used for db-export and db-import.
     *
     * @param path The new export path.
     */
    public void writeExportPath(String path) {
        try {
            m_cms.writePackagePath(path);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Writes a group to the Cms.
     *
     * @param name The name of the group to be written.
     * @param flags The flags of the user to be written.
     */
    public void writeGroup(String name, String flags) {
        try {

            // get the group, which has to be written
            CmsGroup group = m_cms.readGroup(name);
            if (Integer.parseInt(flags) == I_CmsConstants.C_FLAG_DISABLED) {
                group.setDisabled();
            } else {
                group.setEnabled();
            }

            // write it back
            m_cms.writeGroup(group);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Writes a propertyinformation for a file or folder.
     *
     * @param name The resource-name of which the propertyinformation has to be set.
     * @param property The propertydefinition-name of which the propertyinformation has to be set.
     * @param value The value for the propertyinfo to be set.
     */
    public void writeProperty(String name, String property, String value) {
        try {
            m_cms.writeProperty(name, property, value);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Writes a new user tasklog for a task.
     *
     * @param taskid the Id of the task.
     * @param comment the description for the log.
     */
    public void writeTaskLog(String taskid, String comment) {
        try {
            m_cms.writeTaskLog(Integer.parseInt(taskid), comment);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Writes a new user tasklog for a task.
     *
     * @param taskid the Id of the task .
     * @param comment the description for the log
     * @param taskType the type of the tasklog. User tasktypes must be greater than 100.
     */
    public void writeTaskLog(String taskid, String comment, String taskType) {
        try {
            m_cms.writeTaskLog(Integer.parseInt(taskid), comment, Integer.parseInt(taskType));
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Writes a user to the Cms.
     *
     * @param name The name of the user to be written.
     * @param flags The flags of the user to be written.
     */
    public void writeUser(String name, String flags) {
        try {

            // get the user, which has to be written
            CmsUser user = m_cms.readUser(name);
            if (Integer.parseInt(flags) == I_CmsConstants.C_FLAG_DISABLED) {
                user.setDisabled();
            } else {
                user.setEnabled();
            }

            // write it back
            m_cms.writeUser(user);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }

    /**
     * Writes a webuser to the Cms.
     *
     * @param name The name of the user to be written.
     * @param flags The flags of the user to be written.
     */
    public void writeWebUser(String name, String flags) {
        try {

            // get the user, which has to be written
            CmsUser user = m_cms.readUser(name, I_CmsConstants.C_USER_TYPE_WEBUSER);
            if (Integer.parseInt(flags) == I_CmsConstants.C_FLAG_DISABLED) {
                user.setDisabled();
            } else {
                user.setEnabled();
            }

            // write it back
            m_cms.writeUser(user);
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }
    
    /**
     * Does some performance measurements of the OpenCms core.<p>
     */
    public void perf() {
        
        final int MAX_TESTS = 10000;
        
        try {
        
            I_CmsVfsDriver vfsDriver = m_driverManager.getVfsDriver();
            Random random = new Random();
            int projectId = m_cms.getRequestContext().currentProject().getId();

            List testResources = vfsDriver.getResourcesInTimeRange(projectId, 0, System.currentTimeMillis());
            int numResources = testResources.size();
            System.out.println("#Resources:\t" + numResources);
            long totalTime = 0, minTime = 0, maxTime = 0, t, tt;
            
            // readFileHeader
            totalTime = 0; 
            minTime = 0;
            maxTime = 0;
            System.out.print("readFileHeader:\t");
            for (int i = MAX_TESTS; i > 0; --i) {
                int index = random.nextInt(numResources);
                CmsResource res = (CmsResource)testResources.get(index);
                t = System.currentTimeMillis();
                    vfsDriver.readFileHeader(projectId, res.getParentId(), res.getResourceName(), true);            
                tt = System.currentTimeMillis() - t;
                totalTime += tt;
                minTime = (minTime==0 | minTime > tt) ? tt : minTime;
                maxTime = (maxTime==0 | maxTime < tt) ? tt : maxTime;
                if ((i % 100) == 0) System.out.print(".");
            }
            System.out.println("\rreadFileHeader:\t" + minTime + "\t" + maxTime + "\t" + (((float)totalTime) / MAX_TESTS) + " ms");
            
        } catch (Exception exc) {
            CmsShell.printException(exc);
        }
    }
}