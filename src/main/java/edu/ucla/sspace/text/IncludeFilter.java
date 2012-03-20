package edu.ucla.sspace.text;

import java.util.Iterator;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class IncludeFilter extends BaseFilter {

    private final Set<String> includeTokens;

    public IncludeFilter(Set<String> includeTokens) {
        this(includeTokens, null);
    }

    public IncludeFilter(Set<String> includeTokens, Filter base) {
        super(base);
        this.includeTokens = includeTokens;
    }

    public Iterator<String> getIterator(Iterator<String> iter) {
        return new FilteredIterator(
                iter, new IncludeExcludeTokenFilter(includeTokens, false));
    } 
}
