package edu.ucla.sspace.text;

import java.util.Set;


/**
 * A Inclusive or Exclusive token filter accepting/rejecting tokens.</p>
 *
 * An inclusive filter will accept only those tokens with which it was
 * initialized.  For an example, an inclusive filter initialized with all of the
 * words in the english dictionary would exclude all misspellings or foreign
 * words in a token stream.</p>
 *
 * An exclusive filter will aceept only those tokens that are not in set with
 * which it was initialized.  An exclusive filter is often used with a list of
 * common words that should be excluded, which is also known as a "stop
 * list."</p>
 *
 * @author David Jurgens
 */
public class IncludeExcludeTokenFilter implements TokenFilter {

    /**
     * The set of tokens used to filter the output
     */
    private final Set<String> tokens;

    /**
     * {@code true} if the returned tokens must not be in the filter set
     */
    private final boolean excludeTokens;

    /**
     * Constructs a filter that accepts only those tokens present in {@code
     * tokens}.
     */
    public IncludeExcludeTokenFilter(Set<String> tokens) {
        this(tokens, false);
    }

    /**
     * Constructs a filter using {@code tokens} that if {@code excludeTokens} is
     * {@code false} will accept those in {@code tokens}, or if {@code
     * excludeTokens} is {@code true}, will accept those that are <i>not</i> in
     * {@code tokens}.
     *
     * @param tokens the set of tokens to use in filtering the output
     * @param excludeTokens {@code true} if tokens in {@code tokens} should be
     *        excluded, {@code false} if only tokens in {@code tokens} should
     *        be included
     */
    public IncludeExcludeTokenFilter(Set<String> tokens, 
                                     boolean excludeTokens) {
        this.tokens = tokens;
        this.excludeTokens = excludeTokens;
    }

    public boolean accept(String token) {
        return tokens.contains(token) ^ excludeTokens;
    }

    /**
     * Loads a series of chained {@code TokenFilter} instances from the
     * specified configuration string using the provided {@link ResourceFinder}
     * to locate the resources.  This method is provided for applications that
     * need to load resources from a custom environment or file system.<p>
     * 
     * A configuration lists sets of files that contain tokens to be included or
     * excluded.  The behavior, {@code include} or {@code exclude} is specified
     * first, followed by one or more file names, each separated by colons.
     * Multiple behaviors may be specified one after the other using a {@code ,}
     * character to separate them.  For example, a typicaly configuration may
     * look like: "include=top-tokens.txt,test-words.txt:exclude=stop-words.txt"
     * <b>Note</b> behaviors are applied in the order they are presented on the
     * command-line.
     *
     * @param configuration a token filter configuration
     * @param finder the {@code ResourceFinder} used to locate the file
     *        resources specified in the configuration string.
     *
     * @return the chained TokenFilter instance made of all the specification,
     *         or {@code null} if the configuration did not specify any filters
     *
     * @throws IOError if any error occurs when reading the word list files
    public static TokenFilter loadFromSpecification(String configuration,
                                                    ResourceFinder finder) {

        TokenFilter toReturn = null;

        // multiple filter are separated by a ':'
        String[] filters = configuration.split(",");

        for (String s : filters) {
            String[] optionAndFiles = s.split("=");
            if (optionAndFiles.length != 2)
                throw new IllegalArgumentException(
                    "Invalid number of filter parameters: " + s);
            
            String behavior = optionAndFiles[0];
            boolean exclude = behavior.equals("exclude");
            // Sanity check that the behavior was include
            if (!exclude && !behavior.equals("include"))
                throw new IllegalArgumentException(
                    "Invalid filter behavior: " + behavior);
                
            String[] files = optionAndFiles[1].split(":");
            
            // Load the words in the file(s)
            Set<String> words = new HashSet<String>();
            try {
                for (String f : files) {
                    BufferedReader br = finder.open(f);
                    for (String line = null; (line = br.readLine()) != null; ) 
                        words.add(line);
                    br.close();
                }
            } catch (IOException ioe) {
                // rethrow since filter error is fatal to correct execution
                throw new IOError(ioe);
            }
            
            // Chain the filters on top of each other
            toReturn = new TokenFilter(words, exclude, toReturn);
        }

        return toReturn;
    }
     */    
}
