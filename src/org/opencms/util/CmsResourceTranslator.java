/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/util/CmsResourceTranslator.java,v $
 * Date   : $Date: 2006/03/25 22:42:36 $
 * Version: $Revision: 1.22.2.1 $
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

package org.opencms.util;

import org.opencms.main.CmsLog;

import org.apache.commons.logging.Log;
import org.apache.oro.text.PatternCache;
import org.apache.oro.text.PatternCacheFIFO;
import org.apache.oro.text.perl.MalformedPerl5PatternException;
import org.apache.oro.text.perl.Perl5Util;
import org.apache.oro.text.regex.MalformedPatternException;

/**
 * This class provides a resource name translation facility.<p>
 * 
 * Resource name translation is required for backward compatibility
 * to the old OpenCms (pre 5.0 beta 2) directory layout.
 * It can also be used to translate special characters or 
 * names automatically.<p>
 * 
 * It is also used for translating new resource names that contain
 * illegal chars to legal names. This feature is most useful (and currently
 * only used) for uploaded files. It is also applied to uploded ZIP directories
 * that are extracted after upload.<p> 
 * 
 * The translations can be configured in the opencms.properties.<p>
 * 
 * The default directory translation setting is:<br>
 * <pre>
 * directory.translation.rules=s#/default/vfs/content/bodys/(.*)#/default/vfs/system/bodies/$1#, \ 
 * s#/default/vfs/pics/system/(.*)#/default/vfs/system/workplace/resources/$1#, \ 
 * s#/default/vfs/pics/(.*)#/default/vfs/system/galleries/pics/$1#, \ 
 * s#/default/vfs/download/(.*)#/default/vfs/system/galleries/download/$1#, \ 
 * s#/default/vfs/externallinks/(.*)#/default/vfs/system/galleries/externallinks/$1#, \ 
 * s#/default/vfs/htmlgalleries/(.*)#/default/vfs/system/galleries/htmlgalleries/$1#, \ 
 * s#/default/vfs/content/(.*)#/default/vfs/system/modules/default/$1#, \ 
 * s#/default/vfs/moduledemos/(.*)#/default/vfs/system/moduledemos/$1#, \ 
 * s#/default/vfs/system/workplace/config/language/(.*)#/default/vfs/system/workplace/locales/$1#, \ 
 * s#/default/vfs/system/workplace/css/(.*)#/default/vfs/system/workplace/resources/$1#, \
 * s#/default/vfs/system/workplace/templates/js/(.*)#/default/vfs/system/workplace/scripts/$1#
 * </pre><p>
 * 
 * The default file name translation setting is:<br>
 * <pre>
 * filename.translation.rules=s#[\s]+#_#g, \
 * s#\\#/#g, \
 * s#[^0-9a-zA-Z_\.\-\/]#!#g, \
 * s#!+#x#g
 * </pre><p>
 *
 * @author  Alexander Kandzior 
 * 
 * @version $Revision: 1.22.2.1 $ 
 * 
 * @since 6.0.0 
 */
public class CmsResourceTranslator {

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsResourceTranslator.class);

    /** Flag to indicate if one or more matchings should be tried. */
    private boolean m_continueMatching;

    /** Perl5 patter cache to avoid unecessary re-parsing of properties. */
    private PatternCache m_perlPatternCache;

    /** Perl5 utility class. */
    private Perl5Util m_perlUtil;

    /** Internal array containing the translations from opencms.properties. */
    private String[] m_translations;

    /**
     * Constructor for the CmsResourceTranslator.
     * 
     * @param translations The array of translations read from the 
     *      opencms,properties
     * @param continueMatching if <code>true</code>, matching will continue after
     *      the first match was found
     */
    public CmsResourceTranslator(String[] translations, boolean continueMatching) {

        super();
        m_translations = translations;
        m_continueMatching = continueMatching;
        // pre-cache the patterns 
        m_perlPatternCache = new PatternCacheFIFO(m_translations.length + 1);
        for (int i = 0; i < m_translations.length; i++) {
            try {
                m_perlPatternCache.addPattern(m_translations[i]);
            } catch (MalformedPatternException e) {
                LOG.error(
                    Messages.get().getBundle().key(Messages.LOG_MALFORMED_TRANSLATION_RULE_1, m_translations[i]),
                    e);
            }
        }
        // initialize the Perl5Util
        m_perlUtil = new Perl5Util(m_perlPatternCache);
        if (LOG.isInfoEnabled()) {
            LOG.info(Messages.get().getBundle().key(
                Messages.LOG_NUM_TRANSLATION_RULES_INITIALIZED_1,
                new Integer(translations.length)));
        }
    }

    /**
     * Returns a copy of the initialized translation rules.<p>
     * 
     * @return String[] a copy of the initialized translation rules
     */
    public String[] getTranslations() {

        String[] copy = new String[m_translations.length];
        System.arraycopy(m_translations, 0, copy, 0, m_translations.length);
        return copy;
    }

    /**
     * Translate a resource name according to the expressions set in 
     * the opencms.properties. If no match is found, 
     * the resource name is returned unchanged.<p>
     * 
     * @param resourceName The resource name to translate
     * @return The translated name of the resource
     */
    public String translateResource(String resourceName) {

        if (m_translations.length == 0) {
            // no translations defined
            return resourceName;
        }
        if (resourceName == null) {
            return null;
        }

        StringBuffer result;
        String current = resourceName;
        int size = current.length() * 2;

        for (int i = 0; i < m_translations.length; i++) {
            result = new StringBuffer(size);
            try {
                if (m_perlUtil.substitute(result, m_translations[i], current) != 0) {

                    if (m_continueMatching) {
                        // continue matching
                        current = result.toString();
                    } else {
                        // first pattern matched, return the result
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(Messages.get().getBundle().key(
                                Messages.LOG_TRANSLATION_MATCH_3,
                                new Integer(i),
                                resourceName,
                                result));
                        }
                        // Return first match result
                        return result.toString();
                    }
                }
            } catch (MalformedPerl5PatternException e) {
                LOG.error(
                    Messages.get().getBundle().key(Messages.LOG_MALFORMED_TRANSLATION_RULE_1, m_translations[i]),
                    e);
            }
        }

        // the pattern matched, return the result
        if (LOG.isDebugEnabled()) {
            LOG.debug(Messages.get().getBundle().key(Messages.LOG_TRANSLATION_MATCH_2, resourceName, current));
        }
        // return last translation (or original if no matching translation found)
        return current;
    }
}