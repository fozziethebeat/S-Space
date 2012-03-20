package edu.ucla.sspace.text;

import java.util.Iterator;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class ExcludeFilter extends BaseFilter {

    private final Set<String> excludeTokens;

    public ExcludeFilter(Set<String> excludeTokens) {
        this(excludeTokens, null);
    }

    public ExcludeFilter(Set<String> excludeTokens, Filter base) {
        super(base);
        this.excludeTokens = excludeTokens;
    }

    public Iterator<String> getIterator(Iterator<String> iter) {
        return new FilteredIterator(
                iter, new IncludeExcludeTokenFilter(excludeTokens, true));
    }
}
