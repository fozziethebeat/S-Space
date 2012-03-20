package edu.ucla.sspace.text;

import java.util.Iterator;


/**
 * @author Keith Stevens
 */
public class StemmingFilter extends BaseFilter {

    private final Stemmer stemmer;

    public StemmingFilter(Stemmer stemmer) {
        this(stemmer, null);
    }

    public StemmingFilter(Stemmer stemmer, Filter base) {
        super(base);
        this.stemmer = stemmer;
    }

    protected Iterator<String> getIterator(Iterator<String> iter) {
        return new StemmingIterator(iter, stemmer);
    }
}
