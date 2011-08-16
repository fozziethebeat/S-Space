package edu.ucla.sspace.text.corpora;

import edu.ucla.sspace.text.CorpusReader;
import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.StringDocument;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.Reader;


/**
 * Reads full documents from a parsed UkWac or Wackypedia corpus.  The corpus is
 * expected to be in full XML, with text tags delmiting documents and s tags
 * delmiting sentences.  All sentences for a document will be returned in the
 * same document.  Any other additional information in the corpus is discarded.
 *
 * @author Keith Stevens
 */
public class PukWacCorpusReader implements CorpusReader {

    /**
     * The {@link BufferedReader} for reading lines in the corpus.
     */
    protected BufferedReader reader;

    /**
     * The text of the next document to return.
     */
    private String next;

    /**
     * {@inheritDoc}
     */
    public void initialize(String fileName) {
        try {
            initialize(new FileReader(fileName));
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void initialize(Reader baseReader) {
        reader = new BufferedReader(baseReader);
        next = advance();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        return next != null;
    }

    /**
     * {@inheritDoc}
     */
    public Document next() {
        Document doc = new StringDocument(next);
        next = advance();
        return doc;
    }

    /**
     * Unsupported.
     *
     * @throws UnsupportedOperationException When called.
     */
    public void remove() {
        throw new UnsupportedOperationException(
                "Cannot remove documents from a CorpusReader");
    }

    /**
     * Advances the {@link CorpusReader} one {@link Document} a head in the
     * text.  Returns {@code null} when no more documents are found in the file.
     */
    protected String advance() {
        StringBuilder sb = new StringBuilder();
        try {
            for (String line = null; (line = reader.readLine()) != null; ) {
                // Skip the tags that denote when the document starts, and the
                // sentence delimiters.  Also skip any empty lines.
                if (line.startsWith("<text") ||
                    line.startsWith("<s>") ||
                    line.startsWith("</s>") ||
                    line.length() == 0)
                    continue;
                // Break the loop when we read the deliminter for the document.
                if (line.startsWith("</text>"))
                break;

                // Split up the line into it's tokens.  Append the first token,
                // which is the real word of interest, to the builder and ignore
                // the rest of the line.  The lines will contain any needed
                // punctuation, so we can just include spaces between tokens to
                // simplify tokenization.
                String[] tokens = line.split("\\s+");
                sb.append(tokens[0]).append(" ");
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }


        // Return null if nothing was read, indicating that the above loop
        // reached the end of it's input.
        if (sb.length() == 0)
            return null;

        return sb.toString();
    }
}

