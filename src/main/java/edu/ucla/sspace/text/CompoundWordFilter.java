package edu.ucla.sspace.text;

import java.util.Iterator;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class CompoundWordFilter extends BaseFilter {

    private final Set<String> compoundWords;

    public CompoundWordFilter(Set<String> compoundWords) {
        this(compoundWords, null);
    }

    public CompoundWordFilter(Set<String> compoundWords, Filter filter) {
        super(filter);
        this.compoundWords = compoundWords;
    }

    protected Iterator<String> getIterator(Iterator<String> iter) {
        return new CompoundWordIterator(iter, compoundWords);
    }
}
