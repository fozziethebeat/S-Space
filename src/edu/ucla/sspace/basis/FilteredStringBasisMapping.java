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
        String[] parts = key.split("-");
        String base = (parts.length == 0) ? key : parts[0];
        return excludedWords.contains(base) ? -1 : getDimensionInternal(key);
    }
}

