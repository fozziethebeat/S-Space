package edu.ucla.sspace.text;


/**
 * An interface for filtering streams of tokens.
 *
 * @author Keith Stevens
 */
public interface Filter {

    /**
     * Creates a new {@link Iterable} that filters the elements in {@code
     * tokens}.  This filtering may eliminate some elements, alter elements, or
     * add new elements.
     */
    Iterable<String> filter(String tokens);

    /**
     * Creates a new {@link Iterable} that filters the elements in {@code
     * tokens}.  This filtering may eliminate some elements, alter elements, or
     * add new elements.
     */
    Iterable<String> filter(String[] tokens);

    /**
     * Creates a new {@link Iterable} that filters the elements in {@code
     * tokens}.  This filtering may eliminate some elements, alter elements, or
     * add new elements.
     */
    Iterable<String> filter(Iterable<String> tokens);
}
