/*
 * File   : $Source: /alkacon/cvs/opencms/test/org/opencms/i18n/TestCmsModuleMessageBundles.java,v $
 * Date   : $Date: 2010/03/11 13:28:19 $
 * Version: $Revision: 1.6 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) 2002 - 2009 Alkacon Software GmbH (http://www.alkacon.com)
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

package org.opencms.i18n;

import org.opencms.gwt.I_CmsClientMessageBundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tests all {@link org.opencms.i18n.I_CmsMessageBundle} instances for the OpenCms 
 * module classes (folder src-modules, org.* packages). <p>
 * 
 * @author Achim Westermann 
 * 
 * @version $Revision: 1.6 $
 * 
 * @since 6.0.0
 */
public final class TestCmsModuleMessageBundles extends TestCmsMessageBundles {

    /**
     * @see org.opencms.i18n.TestCmsMessageBundles#getNotLocalizedBundles(Locale)
     */
    @Override
    protected List getNotLocalizedBundles(Locale locale) {

        List bundles = new ArrayList();
        bundles.add(org.opencms.workplace.demos.Messages.get());
        bundles.add(org.opencms.workplace.demos.list.Messages.get());
        bundles.add(org.opencms.workplace.demos.widget.Messages.get());
        return bundles;
    }

    /**
     * @see org.opencms.i18n.TestCmsMessageBundles#getTestClientMessageBundles()
     */
    @Override
    protected I_CmsClientMessageBundle[] getTestClientMessageBundles() throws Exception {

        return new I_CmsClientMessageBundle[] {
            org.opencms.ade.sitemap.ClientMessages.get(),
            org.opencms.gwt.ClientMessages.get()};
    }

    /**
     * @see org.opencms.i18n.TestCmsMessageBundles#getTestMessageBundles()
     */
    @Override
    protected I_CmsMessageBundle[] getTestMessageBundles() {

        return new I_CmsMessageBundle[] {
            org.opencms.editors.fckeditor.Messages.get(),
            org.opencms.frontend.layoutpage.Messages.get(),
            org.opencms.frontend.photoalbum.Messages.get(),
            org.opencms.frontend.templateone.Messages.get(),
            org.opencms.frontend.templateone.form.Messages.get(),
            org.opencms.frontend.templateone.modules.Messages.get(),
            org.opencms.workplace.administration.Messages.get(),
            org.opencms.workplace.tools.accounts.Messages.get(),
            org.opencms.workplace.tools.cache.Messages.get(),
            org.opencms.workplace.tools.content.Messages.get(),
            org.opencms.workplace.tools.content.check.Messages.get(),
            org.opencms.workplace.tools.content.propertyviewer.Messages.get(),
            org.opencms.workplace.tools.database.Messages.get(),
            org.opencms.workplace.tools.galleryoverview.Messages.get(),
            org.opencms.workplace.tools.history.Messages.get(),
            org.opencms.workplace.tools.link.Messages.get(),
            org.opencms.workplace.tools.modules.Messages.get(),
            org.opencms.workplace.tools.projects.Messages.get(),
            org.opencms.workplace.tools.publishqueue.Messages.get(),
            org.opencms.workplace.tools.scheduler.Messages.get(),
            org.opencms.workplace.tools.searchindex.Messages.get(),
            org.opencms.workplace.tools.workplace.Messages.get(),
            org.opencms.workplace.tools.workplace.broadcast.Messages.get(),
            org.opencms.workplace.tools.workplace.rfsfile.Messages.get()};
    }
}