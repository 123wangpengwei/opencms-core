/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/util/CmsStringUtil.java,v $
 * Date   : $Date: 2005/06/23 11:11:24 $
 * Version: $Revision: 1.33 $
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

import org.opencms.i18n.CmsEncoder;
import org.opencms.main.CmsLog;
import org.opencms.workplace.I_CmsWpConstants;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.oro.text.perl.MalformedPerl5PatternException;
import org.apache.oro.text.perl.Perl5Util;

/**
 * Provides String utility functions.<p>
 * 
 * @author  Andreas Zahner 
 * @author  Alexander Kandzior 
 * @author Thomas Weckert  
 * 
 * @version $Revision: 1.33 $ 
 * 
 * @since 6.0.0 
 */
public final class CmsStringUtil {

    /** Regular expression that matches the HTML body end tag. */
    public static final String C_BODY_END_REGEX = "<\\s*/\\s*body[^>]*>";

    /** Regular expression that matches the HTML body start tag. */
    public static final String C_BODY_START_REGEX = "<\\s*body[^>]*>";

    /** a convienient shorthand to the line separator constant. */
    public static final String C_LINE_SEPARATOR = System.getProperty("line.separator");

    /** a convienient shorthand for tabulations.  */
    public static final String C_TABULATOR = "  ";

    /** Regex pattern that matches an end body tag. */
    private static final Pattern C_BODY_END_PATTERN = Pattern.compile(C_BODY_END_REGEX, Pattern.CASE_INSENSITIVE);

    /** Regex pattern that matches a start body tag. */
    private static final Pattern C_BODY_START_PATTERN = Pattern.compile(C_BODY_START_REGEX, Pattern.CASE_INSENSITIVE);

    /** Day constant. */
    private static final long C_DAYS = 1000 * 60 * 60 * 24;

    /** Hour constant. */
    private static final long C_HOURS = 1000 * 60 * 60;

    /** Minute constant. */
    private static final long C_MINUTES = 1000 * 60;

    /** Second constant. */
    private static final long C_SECONDS = 1000;

    /** Regex that matches an encoding String in an xml head. */
    private static final Pattern C_XML_ENCODING_REGEX = Pattern.compile(
        "encoding\\s*=\\s*[\"'].+[\"']",
        Pattern.CASE_INSENSITIVE);

    /** Regex that matches an xml head. */
    private static final Pattern C_XML_HEAD_REGEX = Pattern.compile("<\\s*\\?.*\\?\\s*>", Pattern.CASE_INSENSITIVE);

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsStringUtil.class);

    /** OpenCms context replace String, static for performance reasons. */
    private static String m_contextReplace;

    /** OpenCms context search String, static for performance reasons. */
    private static String m_contextSearch;

    /** 
     * Default constructor (empty), private because this class has only 
     * static methods.<p>
     */
    private CmsStringUtil() {

        // empty
    }

    /**
     * Changes the filename suffix. 
     * 
     * @param filename the filename to be changed
     * @param suffix the new suffix of the file
     * @return the filename with the replaced suffix
     */
    public static String changeFileNameSuffixTo(String filename, String suffix) {

        int dotPos = filename.lastIndexOf('.');
        if (dotPos != -1) {
            return filename.substring(0, dotPos + 1) + suffix;
        } else {
            // the string has no suffix
            return filename;
        }
    }

    /**
     * Replaces occurences of special control characters in the given input with 
     * a HTML representation.<p>
     * 
     * This method currrently replaces line breaks to <code>&lt;br/&gt;</code> and special HTML chars 
     * like <code>&lt; &gt; &amp; &quot;</code> with their HTML entity representation.<p>
     * 
     * @param source the String to escape
     * @return the escaped String
     */
    public static String escapeHtml(String source) {

        if (source == null) {
            return null;
        }
        source = CmsEncoder.escapeXml(source);
        source = CmsStringUtil.substitute(source, "\r", "");
        source = CmsStringUtil.substitute(source, "\n", "<br/>\n");
        return source;
    }

    /**
     * Escapes a String so it may be used in JavaScript String definitions.<p>
     * 
     * This method replaces line breaks, quotationmarks and \ characters.<p>
     * 
     * @param source the String to escape
     * @return the escaped String
     */
    public static String escapeJavaScript(String source) {

        source = CmsStringUtil.substitute(source, "\\", "\\\\");
        source = CmsStringUtil.substitute(source, "\"", "\\\"");
        source = CmsStringUtil.substitute(source, "\'", "\\\'");
        source = CmsStringUtil.substitute(source, "\r\n", "\\n");
        source = CmsStringUtil.substitute(source, "\n", "\\n");
        return source;
    }

    /**
     * Escapes a String so it may be used as a Perl5 regular expression.<p>
     * 
     * This method replaces the following characters in a String:<br>
     * <code>{}[]()\$^.*+/</code>
     * 
     * 
     * @param source the string to escape
     * @return the escaped string
     */
    public static String escapePattern(String source) {

        if (source == null) {
            return null;
        }
        StringBuffer result = new StringBuffer(source.length() * 2);
        for (int i = 0; i < source.length(); ++i) {
            char ch = source.charAt(i);
            switch (ch) {
                case '\\':
                    result.append("\\\\");
                    break;
                case '/':
                    result.append("\\/");
                    break;
                case '$':
                    result.append("\\$");
                    break;
                case '^':
                    result.append("\\^");
                    break;
                case '.':
                    result.append("\\.");
                    break;
                case '*':
                    result.append("\\*");
                    break;
                case '+':
                    result.append("\\+");
                    break;
                case '|':
                    result.append("\\|");
                    break;
                case '?':
                    result.append("\\?");
                    break;
                case '{':
                    result.append("\\{");
                    break;
                case '}':
                    result.append("\\}");
                    break;
                case '[':
                    result.append("\\[");
                    break;
                case ']':
                    result.append("\\]");
                    break;
                case '(':
                    result.append("\\(");
                    break;
                case ')':
                    result.append("\\)");
                    break;
                default:
                    result.append(ch);
            }
        }
        return new String(result);
    }

    /**
     * This method takes a part of a html tag definition, an attribute to extend within the
     * given text and a default value for this attribute; and returns a <code>{@link Map}</code>
     * with 2 values: a <code>{@link String}</code> with key <code>"text"</code> with the new text
     * without the given attribute, and another <code>{@link String}</code> with key <code>"value"</code>
     * with the new extended value for the given attribute, this value is sourrounded by the same type of 
     * quotation marks as in the given text.<p> 
     * 
     * @param text the text to search in
     * @param attribute the attribute to remove and extend from the text
     * @param defValue a default value for the attribute, should not have any quotation mark
     * 
     * @return a map with the new text and the new value for the given attribute 
     */
    public static Map extendAttribute(String text, String attribute, String defValue) {

        Map retValue = new HashMap();
        retValue.put("text", text);
        retValue.put("value", "'" + defValue + "'");
        if (text != null && text.toLowerCase().indexOf(attribute.toLowerCase()) >= 0) {
            // this doesnot work for things like "att=method()" without quotations.
            String quotation = "\'";
            int pos1 = text.toLowerCase().indexOf(attribute.toLowerCase());
            // looking for the opening quotation mark
            int pos2 = text.indexOf(quotation, pos1);
            int test = text.indexOf("\"", pos1);
            if (test > -1 && (pos2 == -1 || test < pos2)) {
                quotation = "\"";
                pos2 = test;
            }
            // assuming there is a closing quotation mark
            int pos3 = text.indexOf(quotation, pos2 + 1);
            // building the new attribute value
            String newValue = quotation + defValue + text.substring(pos2 + 1, pos3 + 1);
            // removing the onload statement from the parameters
            String newText = text.substring(0, pos1);
            if (pos3 < text.length()) {
                newText += text.substring(pos3 + 1);
            }
            retValue.put("text", newText);
            retValue.put("value", newValue);
        }
        return retValue;
    }

    /**
     * Extracts the content of a &lt;body&gt tag in a HTML page.<p>
     * 
     * This method should be pretty robust and work even if the input HTML does not contains
     * a valid body tag.<p> 
     * 
     * @param content the content to extract the body from
     * @return the extracted body tag content
     */
    public static String extractHtmlBody(String content) {

        Matcher startMatcher = C_BODY_START_PATTERN.matcher(content);
        Matcher endMatcher = C_BODY_END_PATTERN.matcher(content);

        int start = 0;
        int end = content.length();

        if (startMatcher.find()) {
            start = startMatcher.end();
        }

        if (endMatcher.find(start)) {
            end = endMatcher.start();
        }

        return content.substring(start, end);
    }

    /**
     * Extracts the xml encoding setting from an xml file that is contained in a String by parsing 
     * the xml head.<p>
     * 
     * This is useful if you have a byte array that contains a xml String, 
     * but you do not know the xml encoding setting. Since the encoding setting 
     * in the xml head is usually encoded with standard US-ASCII, you usually
     * just create a String of the byte array without encoding setting,
     * and use this method to find the 'true' encoding. Then create a String
     * of the byte array again, this time using the found encoding.<p>   
     * 
     * This method will return <code>null</code> in case no xml head 
     * or encoding information is contained in the input.<p>
     * 
     * @param content the xml content to extract the encoding from
     * @return the extracted encoding, or null if no xml encoding setting was found in the input 
     */
    public static String extractXmlEncoding(String content) {

        String result = null;
        Matcher xmlHeadMatcher = C_XML_HEAD_REGEX.matcher(content);
        if (xmlHeadMatcher.find()) {
            String xmlHead = xmlHeadMatcher.group();
            Matcher encodingMatcher = C_XML_ENCODING_REGEX.matcher(xmlHead);
            if (encodingMatcher.find()) {
                String encoding = encodingMatcher.group();
                int pos1 = encoding.indexOf('=') + 2;
                String charset = encoding.substring(pos1, encoding.length() - 1);
                if (Charset.isSupported(charset)) {
                    result = charset;
                }
            }
        }
        return result;
    }

    /**
     * Formats a runtime in the format hh:mm:ss, to be used e.g. in reports.<p>
     * 
     * If the runtime is greater then 24 hours, the format dd:hh:mm:ss is used.<p> 
     * 
     * @param runtime the time to format
     * @return the formatted runtime
     */
    public static String formatRuntime(long runtime) {

        long seconds = (runtime / C_SECONDS) % 60;
        long minutes = (runtime / C_MINUTES) % 60;
        long hours = (runtime / C_HOURS) % 24;
        long days = runtime / C_DAYS;
        StringBuffer strBuf = new StringBuffer();

        if (days > 0) {
            if (days < 10) {
                strBuf.append('0');
            }
            strBuf.append(days);
            strBuf.append(':');
        }

        if (hours < 10) {
            strBuf.append('0');
        }
        strBuf.append(hours);
        strBuf.append(':');

        if (minutes < 10) {
            strBuf.append('0');
        }
        strBuf.append(minutes);
        strBuf.append(':');

        if (seconds < 10) {
            strBuf.append('0');
        }
        strBuf.append(seconds);

        return strBuf.toString();
    }

    /**
     * Returns <code>true</code> if the provided String is either <code>null</code>
     * or the empty String <code>""</code>.<p> 
     * 
     * @param value the value to check
     * @return true, if the provided value is null or the empty String, false otherwise
     */
    public static boolean isEmpty(String value) {

        return (value == null) || (value.length() == 0);
    }

    /**
     * Returns <code>true</code> if the provided String is either <code>null</code>
     * or contains only white spaces.<p> 
     * 
     * @param value the value to check
     * @return true, if the provided value is null or contains only white spaces, false otherwise
     */
    public static boolean isEmptyOrWhitespaceOnly(String value) {

        return isEmpty(value) || (value.trim().length() == 0);
    }

    /**
     * Returns <code>true</code> if the provided String is neither <code>null</code>
     * nor the empty String <code>""</code>.<p> 
     * 
     * @param value the value to check
     * @return true, if the provided value is not null and not the empty String, false otherwise
     */
    public static boolean isNotEmpty(String value) {

        return (value != null) && (value.length() != 0);
    }

    /**
     * Returns <code>true</code> if the provided String is neither <code>null</code>
     * nor contains only white spaces.<p> 
     * 
     * @param value the value to check
     * @return true, if the provided value is null or contains only white spaces, false otherwise
     */
    public static boolean isNotEmptyOrWhitespaceOnly(String value) {

        return (value != null) && (value.trim().length() > 0);
    }

    /**
     * Checks if the given class name is a valid Java class name.<p>
     * 
     * @param className the name to check
     * @return true if the given class name is a valid Java class name
     */
    public static boolean isValidJavaClassName(String className) {

        if (CmsStringUtil.isEmpty(className)) {
            return false;
        }
        int length = className.length();
        boolean nodot = true;
        for (int i = 0; i < length; i++) {
            char ch = className.charAt(i);
            if (nodot) {
                if (ch == '.') {
                    return false;
                } else if (Character.isJavaIdentifierStart(ch)) {
                    nodot = false;
                } else {
                    return false;
                }
            } else {
                if (ch == '.') {
                    nodot = true;
                } else if (Character.isJavaIdentifierPart(ch)) {
                    nodot = false;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Applies white space padding to the left of the given String.<p>
     * 
     * @param input the input to pad left
     * @param size the size of the padding
     * 
     * @return the input padded to the left
     */
    public static String padLeft(String input, int size) {

        return (new PrintfFormat("%" + size + "s")).sprintf(input);
    }

    /**
     * Applies white space padding to the right of the given String.<p>
     * 
     * @param input the input to pad right
     * @param size the size of the padding
     * 
     * @return the input padded to the right
     */
    public static String padRight(String input, int size) {

        return (new PrintfFormat("%-" + size + "s")).sprintf(input);
    }

    /**
     * Splits a String into substrings along the provided char delimiter and returns
     * the result as an Array of Substrings.<p>
     *
     * @param source the String to split
     * @param delimiter the delimiter to split at
     *
     * @return the Array of splitted Substrings
     */
    public static String[] splitAsArray(String source, char delimiter) {

        List result = splitAsList(source, delimiter);
        return (String[])result.toArray(new String[result.size()]);
    }

    /**
     * Splits a String into substrings along the provided String delimiter and returns
     * the result as an Array of Substrings.<p>
     *
     * @param source the String to split
     * @param delimiter the delimiter to split at
     *
     * @return the Array of splitted Substrings
     */
    public static String[] splitAsArray(String source, String delimiter) {

        List result = splitAsList(source, delimiter);
        return (String[])result.toArray(new String[result.size()]);
    }

    /**
     * Splits a String into substrings along the provided char delimiter and returns
     * the result as a List of Substrings.<p>
     *
     * @param source the String to split
     * @param delimiter the delimiter to split at
     *
     * @return the List of splitted Substrings
     */
    public static List splitAsList(String source, char delimiter) {

        return splitAsList(source, delimiter, false);
    }

    /**
     * Splits a String into substrings along the provided char delimiter and returns
     * the result as a List of Substrings.<p>
     *
     * @param source the String to split
     * @param delimiter the delimiter to split at
     * @param trim flag to indicate if leading and trailing whitespaces should be omitted
     *
     * @return the List of splitted Substrings
     */
    public static List splitAsList(String source, char delimiter, boolean trim) {

        List result = new ArrayList();
        int index = 0;
        int next = source.indexOf(delimiter);
        while (next != -1) {
            String item = source.substring(index, next);
            if (trim) {
                result.add(item.trim());
            } else {
                result.add(item);
            }
            index = next + 1;
            next = source.indexOf(delimiter, index);
        }
        if (trim) {
            result.add(source.substring(index).trim());
        } else {
            result.add(source.substring(index));
        }
        return result;
    }

    /**
     * Splits a String into substrings along the provided String delimiter and returns
     * the result as List of Substrings.<p>
     *
     * @param source the String to split
     * @param delimiter the delimiter to split at
     *
     * @return the Array of splitted Substrings
     */
    public static List splitAsList(String source, String delimiter) {

        return splitAsList(source, delimiter, false);
    }

    /**
     * Splits a String into substrings along the provided String delimiter and returns
     * the result as List of Substrings.<p>
     * 
     * @param source source the String to split
     * @param delimiter the delimiter to split at
     * @param trim flag to indicate if leading and trailing whitespaces should be omitted
     * 
     * @return the Array of splitted Substrings
     */
    public static List splitAsList(String source, String delimiter, boolean trim) {

        int len = delimiter.length();
        if (len == 1) {
            // optimize for short strings
            return splitAsList(source, delimiter.charAt(0), trim);
        }

        List result = new ArrayList();
        int index = 0;
        int next = source.indexOf(delimiter);
        while (next != -1) {
            String item = source.substring(index, next);
            if (trim) {
                result.add(item.trim());
            } else {
                result.add(item);
            }

            index = next + len;
            next = source.indexOf(delimiter, index);
        }
        if (trim) {
            result.add(source.substring(index).trim());
        } else {
            result.add(source.substring(index));
        }
        return result;
    }

    /**
     * Substitutes searchString in content with replaceItem.<p>
     * 
     * @param content the content which is scanned
     * @param searchString the String which is searched in content
     * @param replaceItem the new String which replaces searchString
     * @return String the substituted String
     */
    public static String substitute(String content, String searchString, String replaceItem) {

        // high performance implementation to avoid regular expression overhead
        int findLength;
        if (content == null) {
            return null;
        }
        int stringLength = content.length();
        if (searchString == null || (findLength = searchString.length()) == 0) {
            return content;
        }
        if (replaceItem == null) {
            replaceItem = "";
        }
        int replaceLength = replaceItem.length();
        int length;
        if (findLength == replaceLength) {
            length = stringLength;
        } else {
            int count;
            int start;
            int end;
            count = 0;
            start = 0;
            while ((end = content.indexOf(searchString, start)) != -1) {
                count++;
                start = end + findLength;
            }
            if (count == 0) {
                return content;
            }
            length = stringLength - (count * (findLength - replaceLength));
        }
        int start = 0;
        int end = content.indexOf(searchString, start);
        if (end == -1) {
            return content;
        }
        StringBuffer sb = new StringBuffer(length);
        while (end != -1) {
            sb.append(content.substring(start, end));
            sb.append(replaceItem);
            start = end + findLength;
            end = content.indexOf(searchString, start);
        }
        end = stringLength;
        sb.append(content.substring(start, end));
        return sb.toString();
    }

    /**
     * Substitutes the OpenCms context path (e.g. /opencms/opencms/) in a HTML page with a 
     * special variable so that the content also runs if the context path of the server changes.<p>
     * 
     * @param htmlContent the HTML to replace the context path in 
     * @param context the context path of the server
     * @return the HTML with the replaced context path
     */
    public static String substituteContextPath(String htmlContent, String context) {

        if (m_contextSearch == null) {
            m_contextSearch = "([^\\w/])" + context;
            m_contextReplace = "$1" + CmsStringUtil.escapePattern(I_CmsWpConstants.C_MACRO_OPENCMS_CONTEXT) + "/";
        }
        return substitutePerl(htmlContent, m_contextSearch, m_contextReplace, "g");
    }

    /**
     * Substitutes searchString in content with replaceItem.<p>
     * 
     * @param content the content which is scanned
     * @param searchString the String which is searched in content
     * @param replaceItem the new String which replaces searchString
     * @param occurences must be a "g" if all occurences of searchString shall be replaced
     * @return String the substituted String
     */
    public static String substitutePerl(String content, String searchString, String replaceItem, String occurences) {

        String translationRule = "s#" + searchString + "#" + replaceItem + "#" + occurences;
        Perl5Util perlUtil = new Perl5Util();
        try {
            return perlUtil.substitute(translationRule, content);
        } catch (MalformedPerl5PatternException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(Messages.get().key(Messages.LOG_MALFORMED_TRANSLATION_RULE_1, translationRule), e);
            }
        }
        return content;
    }

    /**
     * Validates a value against a regular expression.<p>
     * 
     * @param value the value to test
     * @param regex the regular expression
     * @param allowEmpty if an empty value is allowed
     * 
     * @return <code>true</code> if the value satisfies the validation
     */
    public static boolean validateRegex(String value, String regex, boolean allowEmpty) {
        
        if (CmsStringUtil.isEmptyOrWhitespaceOnly(value)) {
            return allowEmpty;
        }
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);
        return matcher.matches();
    }
    
    /**
     * Checks if the provided name is a valid resource name, that is contains only
     * valid characters.<p>
     *
     * PLEASE NOTE:
     * This logic is NOT yet used in the current release.<p>
     *
     * @param name the resource name to check
     * @return true if the resource name is vaild, false otherwise 
     */
    public static boolean validateResourceName(String name) {

        if (name == null) {
            return false;
        }
        int l = name.length();
        if (l == 0) {
            return false;
        }
        if (name.length() != name.trim().length()) {
            // leading or trainling white space are not allowed
            return false;
        }
        for (int i = 0; i < l; i++) {
            char ch = name.charAt(i);
            switch (ch) {
                case '/':
                    return false;
                case '\\':
                    return false;
                case ':':
                    return false;
                case '*':
                    return false;
                case '?':
                    return false;
                case '"':
                    return false;
                case '>':
                    return false;
                case '<':
                    return false;
                case '|':
                    return false;
                default:
                    // ISO control chars are not allowed
                    if (Character.isISOControl(ch)) {
                        return false;
                    }
                    // chars not defined in unicode are not allowed
                    if (!Character.isDefined(ch)) {
                        return false;
                    }
            }
        }

        return true;
    }
}