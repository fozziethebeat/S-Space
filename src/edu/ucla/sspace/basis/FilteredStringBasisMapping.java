package edu.ucla.sspace.basis;

import java.util.Set;


/**
 * @author Keith Stevens
 */
public class FilteredStringBasisMapping
        extends AbstractBasisMapping<String, String> {

    private static final long serialVersionUID = 1L;

    private final Set<String> excludedWords;

    public FilteredStringBasisMapping(Set<String> excludedWords) {
        this.excludedWords = excludedWords;
    }

    public int getDimension(String key) {
        return excludedWords.contains(key) ? -1 : getDimensionInternal(key);
    }
}

