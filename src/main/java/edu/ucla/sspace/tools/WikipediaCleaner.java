/*
 * Copyright 2009 David Jurgens 
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.text.DocumentPreprocessor;
import edu.ucla.sspace.text.IteratorFactory;
import edu.ucla.sspace.text.StringUtils;

import edu.ucla.sspace.util.LoggerUtil;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicBoolean;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A tool for converting <a
 * href="http://en.wikipedia.org/wiki/Wikipedia_database#Where_do_I_get...">Wikipedia
 * Snapshots</a> into a parsable corpus of documents.
 *
 * @author David Jurgens
 * @author Keith Stevens
 */
public class WikipediaCleaner {

    public enum CleanerOption {
        INCLUDE_TITLES, INCLUDE_CAPTIONS, INCLUDE_LINK_TEXT,
            FILTER_TOKENS, USE_PREPROCESSOR
    }

    private static final Logger LOGGER =
        Logger.getLogger(WikipediaCleaner.class.getName());

    /**
     * The file to where the processed articles will be written
     */
    private PrintWriter processedArticleWriter;

    /**
     * The set of options to use when processing the documents
     */
    private final Set<CleanerOption> options;
    
    /**
     * The minimum number of tokens per article
     */
    private final int minTokensPerArticle;

    /**
     * Create a new {@code WikipediaCleaner} which will read articles from
     * {@code outputFileName}, with the given thresholds for link requirements.
     */
    public WikipediaCleaner(String outputFile, Set<CleanerOption> options,
                            int minTokensPerArticle) { 
        this.options = options;
        this.minTokensPerArticle = minTokensPerArticle;       
        try {
            processedArticleWriter = new PrintWriter(
                new BufferedOutputStream(new FileOutputStream(outputFile)));
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Process the content of the given {@code WikiDoc}.
     *
     * @param doc The {@code WikiDoc} to process.
     *
     * @return True if document processing should continue, false if the cleaner
     *         has entered an invalid state.
     */
    public void processDocument(WikiDoc doc) {

        String rawArticleName = doc.name;        
        String articleName = StringUtils.unescapeHTML(rawArticleName);
        articleName = articleName.trim().toLowerCase();

        // skip articles that are not text-based or are
        // wikipedia-specific
        if (!isArticleLink(articleName)) {
            LOGGER.fine("skipping non-article document: " + articleName);
            return;
        } 
        // Skip processing articles that exist solely to direct the user to a
        // different article
        else if (articleName.indexOf("#REDIRECT") >= 0 ||
                   doc.text.indexOf("#REDIRECT") >= 0) {
            LOGGER.fine("skipping redirect: " + articleName);
            return;
        }
        LOGGER.log(Level.FINE, "Procesing article {0} with {1} characters",
                   new Object[] { articleName, doc.text.length() });
    
        // Gets the raw tokens contained in the article XML
        StringBuilder rawArticleText = doc.text;

        // Remove all the header and footer information from the article
        LOGGER.finer("extracting raw article text");        
        extractArticle(rawArticleText);

        // Remove any tables
        LOGGER.finer("removing tables");
        removeTables(rawArticleText);       

        // Remove the {{ text }} content from the document first as it sometimes
        // contains [[ text ]], which would be needlessly processed prior to
        // removal if this were called later.
        LOGGER.finer("removing {{text}} from article");
        removeDoubleBraceMarkup(rawArticleText);

        // Next remove the [[ link ]] markup
        LOGGER.finer("removing [[wiki-link]] from article");
        removeWikiLinkMarkup(rawArticleText, articleName);

        // Once the wiki-links are removed, go after external links, which are
        // only a single [link].  Occassinally, these will have text.
        LOGGER.finer("removing [external-link] from article");
        removeExternalLinkMarkup(rawArticleText);
                
        // Once all of the wiki markup has been removed, replace all of the
        // encoded HTML with its equivalents
        LOGGER.finer("unescaping HTML");
        StringUtils.unescapeHTML(rawArticleText);

        // Remove auto-generated Wikipedia warnings which appear in HTML comment
        // text
        LOGGER.finer("removing HTML comments");
        removeHtmlComments(rawArticleText);
        
        String article = rawArticleText.toString();

        // Being removing any tokens according to the options
        if (options.contains(CleanerOption.USE_PREPROCESSOR)) {
            LOGGER.finer("applying preprocessor");
            article = new DocumentPreprocessor().process(article);
        }
        if (options.contains(CleanerOption.FILTER_TOKENS)) {
            LOGGER.finer("filtering tokens");
            article = filterTokens(article);
        }
            
        // Count how many tokens remain in the document after all of the
        // processing stages.  If too few remain, do not write the document
        int finalTokenCount = getTokenCount(article);
        if (finalTokenCount < minTokensPerArticle) {
            LOGGER.log(Level.FINE, "Document {0} contained only {1} tokens"
                       + " and was not printed", new Object[] {
                           articleName, finalTokenCount });
            return;
        }

        if (options.contains(CleanerOption.INCLUDE_TITLES)) {
            processedArticleWriter.print(articleName);
            processedArticleWriter.print(" ");
        }
        
        // Write the remaining part of the article
        processedArticleWriter.println(article);
        processedArticleWriter.flush();
    }

    /**
     * Extract the article content from {@code text} markup tags.
     *
     * @param text Raw article text.
     *
     * @return Article text extracted from {@code text} tags.
     */
    private void extractArticle(StringBuilder article) {
        // remove all html tags before we unescape the text itself
        // and possibly introduce non-html < characters
        int startOfTextTag = article.indexOf("<text");
        int endOfStart  = article.indexOf(">", startOfTextTag);        
        int closingTextTag = article.indexOf("</text");
        // Remove the ending content.  Some rare, malformatted articles do not
        // contain the ending text tag, so just leave whatever was at the end of
        // the text.
        if (closingTextTag >= 0)
            article.delete(closingTextTag, article.length());
        // Then anything occurring before the text
        article.delete(0, endOfStart + 1);
    }

    /**
     * Removes any tokens not allowed by the {@link
     * edu.ucla.sspace.text.TokenFilter} in the article.
     */
    private String filterTokens(String article) {
        Iterator<String> filteredTokens = IteratorFactory.tokenize(article);
        StringBuilder sb = new StringBuilder(article.length());
        while (filteredTokens.hasNext()) {
            sb.append(filteredTokens.next());
            if (filteredTokens.hasNext())
                sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Remove wiki citations of the form "{{cite ... }}", which links to some
     * text or another wikipedia link.
     *
     * @param article The article text to clean.
     */
    private void removeDoubleBraceMarkup(StringBuilder article) {
        int braceStart = article.indexOf("{{");
        // Repeatedly loop while {{ }} text still exists in the document
        while (braceStart >= 0) {
            // Find the matching closing }} if it exists.  Some wikipedia
            // text is malformated, with no matching brace. so take no
            // action in this case.
            int braceEnd = article.indexOf("}}", braceStart);
        
            int nextBraceStart = article.indexOf("{{", braceStart + 1);
            // Some {{ content has embedded {{ content, which causes problems
            // for nearest matching.  Recursively search until a nearest-match
            // is found and then 
            while (nextBraceStart > braceStart && nextBraceStart < braceEnd) {
                removeEmbeddedBrace(article, nextBraceStart);
                // Recompute the ending brace, since removing the embedded {{
                // will have removed the }} as well.
                braceEnd = article.indexOf("}}", braceStart);
                nextBraceStart = article.indexOf("{{", braceStart + 1);
            }

            if (braceEnd < 0)
                break;

            article.delete(braceStart, braceEnd + 2);
            // Search for the next {{ if it exists
            braceStart = article.indexOf("{{", braceStart);            
        }
    }

    /**
     * Recursively searches for {{ }} markup that has no embedded {{ }} markup
     * itself, and removes it.
     */
    private void removeEmbeddedBrace(StringBuilder article, int startOffset) {
        int braceStart = startOffset;
        // Find the matching closing }} if it exists.  Some wikipedia
        // text is malformated, with no matching brace. so take no
        // action in this case.
        int braceEnd = article.indexOf("}}", braceStart);
        int nextBraceStart = article.indexOf("{{", braceStart + 1);
        while (nextBraceStart > braceStart && nextBraceStart < braceEnd) {
            removeEmbeddedBrace(article, nextBraceStart);
            // Recompute the ending brace, since removing the embedded {{
            // will have removed the }} as well.
            braceEnd = article.indexOf("}}", braceStart);
            nextBraceStart = article.indexOf("{{", braceStart + 1);
        }
        
        if (braceEnd < 0)
            return;
        
        article.delete(braceStart, braceEnd + 2);
    }

    /**
     * Remove wiki tables of the form "{| table |}".
     *
     * @param article The article text to clean.
     */
    private void removeTables(StringBuilder article) {
        int tableStart = article.indexOf("{|");
        // Repeatedly loop while {| |} table markup still exists in the document
        while (tableStart >= 0) {
            // Find the matching closing |} if it exists.  Some wikipedia
            // text is malformated, with no matching table end. so take no
            // action in this case.
            int tableEnd = article.indexOf("|}", tableStart);
            if (tableEnd > tableStart) 
                article.delete(tableStart, tableEnd + 2);
            else 
                break;
            // Search for the next {| if it exists
            tableStart = article.indexOf("{|", tableStart);
        }
    }

    /**
     * Removes HTML comments from the article text
     *
     * @param article The article text to clean.
     */
    private void removeHtmlComments(StringBuilder article) {
        int htmlCommentStart = article.indexOf("<!--");
        // Repeatedly loop while <!-- --> html comment markup still exists in
        // the document
        while (htmlCommentStart >= 0) {
            // Find the matching closing --> if it exists.  Some wikipedia text
            // is malformated, with no matching html comment end. so take no
            // action in this case.
            int htmlCommentEnd = article.indexOf("-->", htmlCommentStart);
            if (htmlCommentEnd > htmlCommentStart) 
                article.delete(htmlCommentStart, htmlCommentEnd + 3);
            else 
                    break;
            // Search for the next <!-- if it exists
            htmlCommentStart = article.indexOf("<!--", htmlCommentStart);
        }
    }

    /**
     * Replace [[link]] tags with link name and track what articles this article
     * links to.
     *
     * @param text The article text to clean and process link structure of.
     *
     * @return A Duple containing the cleaned text and the outgoing link count.
     */
    public void removeWikiLinkMarkup(StringBuilder article, String title) {
        int bracketStart = article.indexOf("[[");
        boolean includeLinkText = 
            options.contains(CleanerOption.INCLUDE_LINK_TEXT);
        while (bracketStart >= 0) {
            
            // grab the linked article name which is all text to the next ]], or
            // to
            int bracketEnd = article.indexOf("]]", bracketStart);
            // If there wasn't a matching closing bracket (i.e. malformatted
            // wiki), then abort the replacement
            if (bracketEnd < 0)
                break;
            
            // If the link text is supposed to be included in the document, then
            // strip out the pertinent text.  However, ensure that the link
            // points to an article, which filters out non-content links in the
            // article headers and footers
            if (includeLinkText && 
                isArticleLink(article.substring(bracketStart+2, 
                                                bracketEnd), title)) {

                // the link may also have a text description that replaces the
                // link text, e.g.  [[article title|link text]].
                int optionalLinkDescriptionStart = 
                    article.indexOf("|", bracketStart);
                // When selecting the optional text, ensure that the | delimeter
                // falls within the link structure itself
                int linkTextStart = 
                    (optionalLinkDescriptionStart >= 0 && 
                     optionalLinkDescriptionStart < bracketEnd)
                    ? optionalLinkDescriptionStart + 1
                    : bracketStart + 2;
                // Parse out the link text
                String linkText = article.substring(linkTextStart, bracketEnd);
                // Then replace the entire link with the desired text
                article.replace(bracketStart, bracketEnd+2, linkText);
            }
            // If the link text isn't to be used in the document, remove it
            // completely
            else {
                article.delete(bracketStart, bracketEnd + 2);
            }
            bracketStart = article.indexOf("[[", bracketStart);
        }   
    }

    /**
     * Replace [link] tags with link name and track what articles this article
     * links to.
     *
     * @param text The article text to clean and process link structure of.
     */
    public void removeExternalLinkMarkup(StringBuilder article) {
        int bracketStart = article.indexOf("[");
        boolean includeLinkText = 
            options.contains(CleanerOption.INCLUDE_LINK_TEXT);
        while (bracketStart >= 0) {            
            int bracketEnd = article.indexOf("]", bracketStart);
            // If there wasn't a matching closing bracket (i.e. malformatted
            // wiki), then abort the replacement
            if (bracketEnd < 0)
                break;
            
            // If the link text is supposed to be included in the document, then
            // strip out the pertinent text.
            if (includeLinkText) {
                // the link may also have a text description that replaces the
                // link text, e.g.  [link text].
                int optionalLinkDescriptionStart = 
                    article.indexOf(" ", bracketStart);
                // When selecting the optional text, ensure that the ' '
                // delimeter falls within the link structure itself
                int linkTextStart = 
                    (optionalLinkDescriptionStart >= 0 && 
                     optionalLinkDescriptionStart < bracketEnd)
                    ? optionalLinkDescriptionStart
                    : bracketStart + 1;
                // Parse out the link text
                String linkText = article.substring(linkTextStart, bracketEnd);
                // Then replace the entire link with the desired text
                article.replace(bracketStart, bracketEnd + 1, linkText);
            }
            // If the link text isn't to be used in the document, remove it
            // completely
            else {
                article.delete(bracketStart, bracketEnd + 1);
            }
            bracketStart = article.indexOf("[", bracketStart);
        }   
    }

    /**
     * Returns the number of tokens in the article.
     */
    private int getTokenCount(String article) {
        Pattern notWhiteSpace = Pattern.compile("\\S+");
        Matcher matcher = notWhiteSpace.matcher(article);
        int tokens = 0;
        while (matcher.find())
            tokens++;
        return tokens;
    }

    public static void main(String[] args) {
        ArgOptions options = new ArgOptions();
        options.addOption('t', "includeTitles",
                          "Prints article and section titles as a part of " +
                          "the document",
                          false, null, "Document Processing");
        options.addOption('c', "includeCaptions",
                          "Prints image and table captions as a part of " +
                          "the document",
                          false, null, "Document Processing");
        options.addOption('w', "includeLinkText",
                          "Prints text in the Wikipedia links as a part of " +
                          "the document",
                          false, null, "Document Processing");
        options.addOption('F', "tokenFilter",
                          "Specifies a filter to remove or retain certain " +
                          "tokens",
                          true, "FILTER_SPEC", "Filtering");
        options.addOption('M', "minTokens",
                          "Records only those documents with at least the " +
                          "minimum number of tokens",
                          true, "INT", "Filtering");
        options.addOption('P', "applyPreprocessor",
                          "Applies the DocumentPreprocessor to the documents",
                          false, null, "Filtering");
        options.addOption('v', "verbose",
                          "Print verbose output about article cleaning",
                          false, null, "Optional");
        options.addOption('V', "veryVerbose",
                          "Print lots of verbose output about article cleaning",
                          false, null, "Optional");


        options.parseOptions(args);

        if (options.numPositionalArgs() != 2) {
            System.out.println("usage java [OPTIONS] <wikifile> <output-file>\n"
                               + options.prettyPrint());
            return;
        }

        // If verbose output is enabled, update all the loggers in the S-Space
        // package logging tree to output at Level.FINE (normally, it is
        // Level.INFO).  This provides a more detailed view of how the execution
        // flow is proceeding.
        Level logLevel = null;
        if (options.hasOption("verbose")) 
            logLevel = Level.FINE;
        else if (options.hasOption("veryVerbose")) 
            logLevel = Level.FINER;
        if (logLevel != null) 
            LoggerUtil.setLevel(logLevel);
        
        // Set up the options for the cleaner
        Set<CleanerOption> cleanerOptions = EnumSet.noneOf(CleanerOption.class);
        if (options.hasOption("includeTitles"))
            cleanerOptions.add(CleanerOption.INCLUDE_TITLES);
        if (options.hasOption("includeCaptions"))
            cleanerOptions.add(CleanerOption.INCLUDE_CAPTIONS);
        if (options.hasOption("includeLinkText"))
            cleanerOptions.add(CleanerOption.INCLUDE_LINK_TEXT);
        if (options.hasOption("tokenFilter")) {
            // Set up the token filter based on the spec
            Properties props = new Properties();
            props.setProperty(IteratorFactory.TOKEN_FILTER_PROPERTY,
                              options.getStringOption("tokenFilter"));
            IteratorFactory.setProperties(props);
            cleanerOptions.add(CleanerOption.FILTER_TOKENS);
        }
        if (options.hasOption("applyPreprocessor"))
            cleanerOptions.add(CleanerOption.USE_PREPROCESSOR);
            
        int minTokens = (options.hasOption("minTokens"))
            ? options.getIntOption("minTokens")
            : 0;

        try {
            DocumentBufferedQueue docQueue = 
                new DocumentBufferedQueue(options.getPositionalArg(0));
            
            String outFileName = options.getPositionalArg(1);
            WikipediaCleaner cleaner = 
                new WikipediaCleaner(outFileName, cleanerOptions, minTokens);
            
            while (docQueue.hasNext()) {
                cleaner.processDocument(docQueue.next());
            } 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }    

    /**
     * A queue representing a series of wikipedia documents which have been
     * read.
     */
    private static class DocumentBufferedQueue {
        
        /**
         * The number of documents which will be cached in this Queue.
         */
        private static final int DOCS_TO_CACHE = 100;

        /**
         * The lenght of an html title line.
         */
        private static final int TITLE_HTML_LENGTH = "    <title>".length();

        /**
         * A {@code BufferedReader} for an opened wikipedia document.
         */
        private final BufferedReader wikiReader;

        /**
         * A thread safe queue of wikipedia documents which have been read into
         * memory.
         */
        private final BlockingQueue<WikiDoc> cachedDocs;

        /**
         * A flag signalling that {@code wikiReader} is open and ready to be
         * read from.
         */
        private final AtomicBoolean isReaderOpen;
        
        /**
         * Create a new {@code DocumentBufferedQueue} from a wikipedia file
         * name.
         */
        public DocumentBufferedQueue(String wikipediaFile) throws IOException {
            wikiReader = new BufferedReader(new FileReader(wikipediaFile));
            cachedDocs = new LinkedBlockingQueue<WikiDoc>();
            isReaderOpen = new AtomicBoolean(true);

            for (int i = 0; i < DOCS_TO_CACHE; ++i) {
                WikiDoc d = cacheDoc();
                if (d != null)
                    cachedDocs.offer(d);
            }
        }

        /**
         * Create a new {@code WikiDoc} from the the content provided by {@code
         * wikiReader}.
         */
        private synchronized WikiDoc cacheDoc() throws IOException {
            StringBuilder sb = new StringBuilder();
            String articleTitle = null;

            for (String line = null; (line = wikiReader.readLine()) != null;) {
                // Ignore wikipedia documents which are media pages.
                if (line.startsWith("</mediawiki>")) {
                    // end of input
                    isReaderOpen.set(false);
                } else if (line.startsWith("  <page>")) {
                    try {
                        // title immediately follows page declaration
                        String titleLine = wikiReader.readLine();

                        // titles start with '    <title>'            
                        String rem = titleLine.substring(TITLE_HTML_LENGTH);

                        int index = rem.indexOf("<");
                        if (index < 0)
                            throw new Error("Malformed title: " + line);

                        articleTitle = rem.substring(0, index);

                        // read in the rest of the page until we see the end tag
                        while ((line = wikiReader.readLine()) != null && 
                               !line.startsWith("  </page>")) {
                            // Append a space to each line to avoid creating a
                            // single token out of tokens that each appear on a
                            // subsequent lines with no padding.  This is common
                            // in lists and header text
                            sb.append(line).append(" ");
                        }

                        return new WikiDoc(articleTitle, sb);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        break;
                    }
                }
            }
            return null;
        }

        /**
         * Check that the queue has more documents to be read.
         */
        public boolean hasNext() {
            return cachedDocs.size() > 0 || isReaderOpen.get();
        }

        /**
         * Return the next available {@code WikiDoc} stored in the queue.  If
         * there are still documents which need to be put on the queue, read one
         * and add it to {@code cachedDocs}.
         */
        public WikiDoc next() throws InterruptedException {
            new Thread() {
                public void run() {
                    try {
                        WikiDoc d = cacheDoc();
                        if (d != null)
                            cachedDocs.offer(d);            
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }.start();
            // Don't block.  Wait up to 10 minutes (in case of GC) to poll. 
            return cachedDocs.poll(60 * 10 * 1000L, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * A simple struct storing a wikipedia article.
     */
    private static class WikiDoc {

        /**
         * The article's title.
         */
        public final String name;

        /**
         * The article's content.
         */
        public final StringBuilder text;

        /**
         * Create a new {@code WikiDoc} with the given name and content.
         */
        public WikiDoc(String name, StringBuilder text) {
            this.name = name;
            this.text = text;
        }
    }

    /**
     * Returns {@code true} if the article's title does not begin with a known
     * set of non-article prefixes.  This method acts as a rough heuristic for
     * assessing the type of link in a document.
     */
    private static boolean isArticleLink(String linkedArticleTitle) {
        String s = linkedArticleTitle.toLowerCase();
        return !(s.startsWith("image:") ||
                 s.startsWith("wikipedia:") ||
                 s.startsWith("template:") ||
                 s.startsWith("category:") ||
                 s.startsWith("portal:") ||
                 s.contains("(disambiguation)"));
    }

    /**
     * Returns {@code true} if the article's title does not begin with a known
     * set of non-article prefixes and the link does not match the foreign
     * language code tempate of [[languagcode:LinkingArticleName]].  This method
     * acts as a rough heuristic for assessing the type of link in a document.
     *
     * @param linkingArticleTitle the name of the article that contains the link
     */
    private static boolean isArticleLink(String linkedArticleTitle, 
                                         String linkingArticleTitle) {
        if (isArticleLink(linkedArticleTitle)) {
            int colonIndex = linkedArticleTitle.indexOf(":");
            if (colonIndex >= 0 &&
                Pattern.matches("[a-z]*", 
                                linkedArticleTitle.substring(0, colonIndex)))
                return false;
            else 
                return !linkedArticleTitle.endsWith(":" + linkingArticleTitle);
        }
        return false;
    }
}
