/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/search/documents/Attic/CmsHighlightExtractor.java,v $
 * Date   : $Date: 2004/02/17 12:10:52 $
 * Version: $Revision: 1.2 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2004 Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the m_terms of the GNU Lesser General Public
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
package org.opencms.search.documents;


import java.io.*;
import java.util.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.util.PriorityQueue;

/**
 * This class describes a fragment within a document.<p>
 */
class DocumentFragment {
    int m_fragNum;
    int m_score;
    int m_textEndPos;
    int m_textStartPos;
    HashSet m_uniqueTerms = new HashSet();

    /**
     * @param textStartPos textStartPos
     * @param fragNum fragNum
     */
    public DocumentFragment(int textStartPos, int fragNum) {
        this.m_textStartPos = textStartPos;
        this.m_fragNum = fragNum;
    }
    
    /**
     * @param term term
     */
    void addTerm(String term) {
        m_uniqueTerms.add(term);
    }
    
    /**
     * @return the score
     */
    int getScore() {
        return m_uniqueTerms.size();
    }
}

/**
 * This class implements a priority queue for document fragments.<p>
 */
class FragmentQueue extends PriorityQueue {
    
    /**
     * @param size size
     */
    public FragmentQueue(int size) {
        initialize(size);
    }

    /**
     * @see org.apache.lucene.util.PriorityQueue#lessThan(java.lang.Object, java.lang.Object)
     */
    public final boolean lessThan(Object a, Object b) {
        DocumentFragment fragA = (DocumentFragment) a;
        DocumentFragment fragB = (DocumentFragment) b;
        if (fragA.getScore() == fragB.getScore()) {
            return fragA.m_fragNum > fragB.m_fragNum;
        } else {
            return fragA.getScore() < fragB.getScore();
        }
    }
}

/**
 * Adapted from Maik Schreiber's LuceneTools.java,v 1.5 2001/10/16 07:25:55.
 * 
 * Alterations include:
 *  + Changed to support Lucene 1.3 release (requires no change to Lucene code
 * base but consequently no longer supports MultiTermQuery, RangeQuery and
 * PrefixQuery highlighting currently)
 *  + Performance enhancement - CmsHighlightExtractor caches m_query m_terms and can
 * therefore be called repeatedly to highlight multiple results more efficently
 *  + New feature: can extract the most relevant parts of large bodies of text -
 * with user defined size of extracts
 * 
 * @author Maik Schreiber (mailto: bZ@iq-computing.de) /Mark Harwood
 *         (mark@searcharea.co.uk)
 */
public final class CmsHighlightExtractor {
    Analyzer m_analyzer;
    I_TermHighlighter m_highlighter;

    Query m_query;
    HashSet m_terms = new HashSet();

    /**
     * @param highlighter
     *            I_TermHighlighter to use to highlight m_terms in the text
     * @param query
     *            Query which contains the m_terms to be highlighted in the text
     * @param analyzer
     *            Analyzer used to construct the Query
     * @throws IOException if something goes wrong
     */
    public CmsHighlightExtractor(
        I_TermHighlighter highlighter,
        Query query,
        Analyzer analyzer)
        throws IOException {
        this.m_highlighter = highlighter;
        this.m_query = query;
        this.m_analyzer = analyzer;
        // get m_terms in m_query
        getTerms(query, m_terms, false);
    }

    /**
     * Extracts all term texts of a given Query. Term texts will be returned in
     * lower-case.
     * 
     * @param query
     *            Query to extract term texts from
     * @param terms
     *            HashSet where extracted term texts should be put into
     *            (Elements: String)
     * @param prohibited
     *            <code>true</code> to extract "prohibited" m_terms, too
     * @throws IOException if something goes wrong
     */
    public static final void getTerms(
        Query query,
        HashSet terms,
        boolean prohibited)
        throws IOException {
        if (query instanceof BooleanQuery) {
            getTermsFromBooleanQuery((BooleanQuery) query, terms, prohibited);
        } else if (query instanceof PhraseQuery) {
            getTermsFromPhraseQuery((PhraseQuery) query, terms);
        } else if (query instanceof TermQuery) {
            getTermsFromTermQuery((TermQuery) query, terms);
        } else if (query instanceof PrefixQuery) {
            getTermsFromPrefixQuery((PrefixQuery) query, terms, prohibited);
        } else if (query instanceof RangeQuery) {
            getTermsFromRangeQuery((RangeQuery) query, terms, prohibited);
        } else if (query instanceof MultiTermQuery) {
            getTermsFromMultiTermQuery(
                (MultiTermQuery) query,
                terms,
                prohibited);
        }
    }

    /**
     * Extracts all term texts of a given BooleanQuery. Term texts will be
     * returned in lower-case.
     * 
     * @param query
     *            BooleanQuery to extract term texts from
     * @param terms
     *            HashSet where extracted term texts should be put into
     *            (Elements: String)
     * @param prohibited
     *            <code>true</code> to extract "prohibited" m_terms, too
     * @throws IOException if something goes wrong
     */
    private static final void getTermsFromBooleanQuery(
        BooleanQuery query,
        HashSet terms,
        boolean prohibited)
        throws IOException {
        BooleanClause[] queryClauses = query.getClauses();
        int i;

        for (i = 0; i < queryClauses.length; i++) {
            if (prohibited || !queryClauses[i].prohibited) {
                getTerms(queryClauses[i].query, terms, prohibited);
            }
        }
    }

    /**
     * Extracts all term texts of a given MultiTermQuery. Term texts will be
     * returned in lower-case.
     * 
     * @param query
     *            MultiTermQuery to extract term texts from
     * @param terms
     *            HashSet where extracted term texts should be put into
     *            (Elements: String)
     * @param prohibited
     *            <code>true</code> to extract "prohibited" m_terms, too
     */
    private static final void getTermsFromMultiTermQuery(
        MultiTermQuery query,
        HashSet terms,
        boolean prohibited) {
        
        //could use m_query.getEnum(IndexReader reader) if it wasn't protected
        // :(
        //MH getTerms(m_query.getQuery(), m_terms, prohibited);
        
        // just to avoid warnings
        if (org.opencms.main.OpenCms.getLog(org.opencms.search.documents.CmsHighlightExtractor.class).isDebugEnabled()) {
            org.opencms.main.OpenCms.getLog(org.opencms.search.documents.CmsHighlightExtractor.class).debug("Not implemented: getTermsFromPrefixQuery "
                    + "(" + query.toString() + "," + terms.toString() + "," + Boolean.toString(prohibited) + ")");
        }
    }
    
    /**
     * Extracts all term texts of a given PhraseQuery. Term texts will be
     * returned in lower-case.
     * 
     * @param query
     *            PhraseQuery to extract term texts from
     * @param terms
     *            HashSet where extracted term texts should be put into
     *            (Elements: String)
     */
    private static final void getTermsFromPhraseQuery(
        PhraseQuery query,
        HashSet terms) {
        Term[] queryTerms = query.getTerms();
        int i;

        for (i = 0; i < queryTerms.length; i++) {
            terms.add(getTermsFromTerm(queryTerms[i]));
        }
    }

    /**
     * Extracts all term texts of a given PrefixQuery. Term texts will be
     * returned in lower-case.
     * 
     * @param query
     *            PrefixQuery to extract term texts from
     * @param terms
     *            HashSet where extracted term texts should be put into
     *            (Elements: String)
     * @param prohibited
     *            <code>true</code> to extract "prohibited" m_terms, too
     */
    private static final void getTermsFromPrefixQuery(
        PrefixQuery query,
        HashSet terms,
        boolean prohibited) {

        //MH getTerms(m_query.getQuery(), m_terms, prohibited);
        
        // just to avoid warnings
        if (org.opencms.main.OpenCms.getLog(org.opencms.search.documents.CmsHighlightExtractor.class).isDebugEnabled()) {
            org.opencms.main.OpenCms.getLog(org.opencms.search.documents.CmsHighlightExtractor.class).debug("Not implemented: getTermsFromPrefixQuery "
                + "(" + query.toString() + "," + terms.toString() + "," + Boolean.toString(prohibited) + ")");
        }
    }

    /**
     * Extracts all term texts of a given RangeQuery. Term texts will be
     * returned in lower-case.
     * 
     * @param query
     *            RangeQuery to extract term texts from
     * @param terms
     *            HashSet where extracted term texts should be put into
     *            (Elements: String)
     * @param prohibited
     *            <code>true</code> to extract "prohibited" m_terms, too
     */
    private static final void getTermsFromRangeQuery(
        RangeQuery query,
        HashSet terms,
        boolean prohibited) {
        
        //MH getTerms(m_query.getQuery(), m_terms, prohibited);

        // just to avoid warnings
        if (org.opencms.main.OpenCms.getLog(org.opencms.search.documents.CmsHighlightExtractor.class).isDebugEnabled()) {
            org.opencms.main.OpenCms.getLog(org.opencms.search.documents.CmsHighlightExtractor.class).debug("Not implemented: getTermsFromPrefixQuery "
                    + "(" + query.toString() + "," + terms.toString() + "," + Boolean.toString(prohibited) + ")");
        }
    }

    /**
     * Extracts the term of a given Term. The term will be returned in
     * lower-case.
     * 
     * @param term
     *            Term to extract term from
     * 
     * @return the Term's term text
     */
    private static final String getTermsFromTerm(Term term) {
        return term.text().toLowerCase();
    }

    /**
     * Extracts all term texts of a given TermQuery. Term texts will be
     * returned in lower-case.
     * 
     * @param query
     *            TermQuery to extract term texts from
     * @param terms
     *            HashSet where extracted term texts should be put into
     *            (Elements: String)
     */
    private static final void getTermsFromTermQuery(
        TermQuery query,
        HashSet terms) {
        terms.add(getTermsFromTerm(query.getTerm()));
    }

    /**
     * Highlights a text in accordance to the given m_query, extracting the most
     * relevant sections. The document text is analysed in fragmentSize chunks
     * to record hit statistics across the document. After accumulating stats,
     * the fragments with the highest scores are returned as an array of
     * strings in order of m_score.
     * 
     * @param text
     *            text to highlight m_terms in
     * @param fragmentSize
     *            the size in bytes of each fragment to be returned
     * @param maxNumFragments
     *            the maximum number of fragments.
     * 
     * @return highlighted text fragments (between 0 and maxNumFragments number
     *         of fragments)
     * @throws IOException if something goes wrong
     */
    public final String[] getBestFragments(
        String text,
        int fragmentSize,
        int maxNumFragments)
        throws IOException {
        StringBuffer newText = new StringBuffer();
        TokenStream stream = null;

        ArrayList docFrags = new ArrayList();

        DocumentFragment currentFrag =
            new DocumentFragment(newText.length(), docFrags.size());
        docFrags.add(currentFrag);

        FragmentQueue fragQueue = new FragmentQueue(maxNumFragments + 1);

        try {
            org.apache.lucene.analysis.Token token;
            String tokenText;
            int startOffset;
            int endOffset;
            int lastEndOffset = 0;

            // long start=System.currentTimeMillis();
            stream = m_analyzer.tokenStream(null, new StringReader(text));
            while ((token = stream.next()) != null) {
                startOffset = token.startOffset();
                endOffset = token.endOffset();
                // make sure wildcards are removed else highlighting will not
                // work
                tokenText = text.substring(startOffset, endOffset);

                // append text between end of last token (or beginning of text)
                // and start of current token
                if (startOffset > lastEndOffset) {
                    newText.append(" ");
                    // newText.append(text.substring(lastEndOffset, startOffset));
                }
                
                // does m_query contain current token?
                if (m_terms.contains(token.termText())) {
                    newText.append(m_highlighter.highlightTerm(tokenText));
                    currentFrag.addTerm(token.termText());
                } else {
                    if (tokenText.length() > fragmentSize / 2) {
                        newText.append(
                            tokenText.substring(0, fragmentSize / 2));
                        newText.append("..");
                    } else {
                        newText.append(tokenText);
                    }
                }

                if (newText.length()
                    >= (fragmentSize * (docFrags.size() + 1))) {
                    //record stats for a new fragment
                    currentFrag.m_textEndPos = newText.length();
                    currentFrag =
                        new DocumentFragment(newText.length(), docFrags.size());
                    docFrags.add(currentFrag);
                }

                lastEndOffset = endOffset;
            }

            // append text after end of last token
            if (lastEndOffset < text.length()) {
                // int extend = lastEndOffset + fragmentSize;
                // extend = (extend > text.length()) ? text.length() : extend;
                // newText.append(text.substring(lastEndOffset, extend));
                newText.append(text.substring(lastEndOffset));
            }

            currentFrag.m_textEndPos = newText.length();

            //find the most relevant sections of the text
            int minScore = 0;
            for (Iterator i = docFrags.iterator(); i.hasNext();) {
                currentFrag = (DocumentFragment) i.next();
                if (currentFrag.getScore() >= minScore) {
                    fragQueue.put(currentFrag);
                    if (fragQueue.size() > maxNumFragments) {
                        // if hit queue overfull
                        fragQueue.pop();
                        // remove lowest in hit queue
                        minScore =
                            ((DocumentFragment) fragQueue.top()).getScore();
                        // reset minScore
                    }

                }
            }

            //extract the text
            String fragText[] = new String[fragQueue.size()];
            for (int i = fragText.length - 1; i >= 0; i--) {
                DocumentFragment frag = (DocumentFragment) fragQueue.pop();
                fragText[i] =
                    newText.substring(frag.m_textStartPos, frag.m_textEndPos);
            }
            return fragText;

        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    // noop
                }
            }
        }
    }

    /**
     * Highlights a text in accordance to the given m_query, extracting the most
     * relevant sections. and concatenating the chosen fragments with a
     * separator (typically "...") The document text is analysed in
     * fragmentSize chunks to record hit statistics across the document. After
     * accumulating stats, the fragments with the highest scores are returned
     * in order as "separator" delimited strings.
     * 
     * @param text
     *            text to highlight m_terms in
     * @param fragmentSize
     *            the size in bytes of each fragment to be returned
     * @param maxNumFragments
     *            the maximum number of fragments.
     * @param separator
     *            the separator used to intersperse the document fragments
     *            (typically "...")
     * 
     * @return highlighted text
     * @throws IOException if something goes wrong
     */
    public final String getBestFragments(
        String text,
        int fragmentSize,
        int maxNumFragments,
        String separator)
        throws IOException {
        String sections[] =
            getBestFragments(text, fragmentSize, maxNumFragments);
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < sections.length; i++) {
            if (i > 0) {
                result.append(separator);
            }
            result.append(sections[i]);
        }
        return result.toString();
    }
}
