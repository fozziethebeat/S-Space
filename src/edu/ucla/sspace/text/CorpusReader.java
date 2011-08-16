package edu.ucla.sspace.text;

import java.io.Reader;
import java.util.Iterator;


/**
 * A basic interface for setting up a {@link CorpusReader}, which reads un
 * cleaned text from corpus files and transforms them into an appropriately
 * cleaned {@link Document} instance.
 *
 * @author Keith Stevens
 */
public interface CorpusReader extends Iterator<Document> {

    /**
     * Initialzies the {@link CorpusReader} to read {@link Document}s from
     * {@code fileName}.
     */
    void initialize(String fileName);

    /**
     * Initialzies the {@link CorpusReader} to read {@link Document}s from
     * {@code baseReader}.
     */
    void initialize(Reader baseReader);
}

