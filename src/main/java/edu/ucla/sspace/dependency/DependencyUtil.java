package edu.ucla.sspace.dependency;

import java.util.regex.Pattern;

/**
 * A collection of helpful utility methods for Dependency Parsed data.
 *
 * @author Keith Stevens
 */
public class DependencyUtil {

    /**
     * Returns an approximated reconstruction of the original document based on
     * the tokens in a given {@link DependencyTreeNode} array.
     */
    public static String prettyPrintTree(DependencyTreeNode[] nodes) {
        Pattern punctuation = Pattern.compile("[!,-.:;?`]");
        StringBuilder sb = new StringBuilder(nodes.length * 8);
        boolean evenSingleQuote = false;
        boolean evenDoubleQuote = false;
        // For quotations
        boolean skipSpace = false;
        for (int i = 0; i < nodes.length; ++i) {
            String token = nodes[i].word();
            // If the first token, append it to start the text
            if (i == 0)
                sb.append(token);
            // For all other tokens, decide whether to add a space between this
            // token and the preceding.
            else {
                // If the token is punctuation, or is a contraction, e.g., 's or
                // n't, then append it directly
                if (punctuation.matcher(nodes[i].pos()).matches()
                        || punctuation.matcher(token).matches()
                        || token.equals(".") // special case for end of sentence
                        || token.equals("n't")
                        || token.equals("'m")
                        || token.equals("'ll")
                        || token.equals("'re")
                        || token.equals("'ve")
                        || token.equals("'s"))
                    sb.append(token);
                else if (token.equals("'")) {
                    if (evenSingleQuote) 
                        sb.append(token);
                    else {
                        sb.append(' ').append(token);
                        skipSpace = true;
                    }
                    evenSingleQuote = !evenSingleQuote;
                }
                else if (token.equals("\"")) {
                    if (evenDoubleQuote)
                        sb.append(token);
                    else {
                        sb.append(' ').append(token);
                        skipSpace= true;
                    }
                    evenDoubleQuote = !evenDoubleQuote;
                }
                else if (token.equals("$")) {
                    sb.append(' ').append(token);
                    skipSpace= true;                    
                }
                // For non-punctuation tokens
                else {
                    if (skipSpace) {
                        sb.append(token);
                        skipSpace = false;
                    }
                    else 
                        sb.append(' ').append(token);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Returns a simple white space separated string containing each word in the
     * given {@link DependencyTreeNode} array.
     */
    public String toString(DependencyTreeNode[] nodes) {
        StringBuilder sb = new StringBuilder(nodes.length * 8);
        for (int i = 0; i < nodes.length; ++i) {
            String token = nodes[i].word();
            sb.append(token);
            if (i+1 < nodes.length)
                sb.append(' ');
        }
        return sb.toString();
    }
}
