package edu.ucla.sspace.text.corpora;

import java.io.IOException;
import java.io.IOError;


/**
 * Reads dependency parsed sentence lines, which are expected to be a the CoNNL
 * format, from a parsed UkWac or Wackypedia corpus.  The corpus is expected to
 * be in full XML, with text tags delmiting documents and s tags delmiting
 * sentences.  Each sentence for a text entry will be returned as it's own
 * document.  The parse format of the text is not altered in any way.  A {@link
 * DependencyExtractor} is expected to handle the processing of the parsed text.
 *
 * @author Keith Stevens
 */
public class PukWacDependencyCorpusReader extends PukWacCorpusReader {

    /**
     * Advances the {@link CorpusReader} one {@link Document} a head in the
     * text.  Returns {@code null} when no more documents are found in the file.
     */
    protected String advance() {
        StringBuilder sb = new StringBuilder();
        try {
            for (String line = null; (line = reader.readLine()) != null; ) {
                // Skip the tags that denote when the document starts, and the
                // sentence delimiters.  Also skip any empty lines.
                if (line.startsWith("<text") ||
                    line.startsWith("</text>") ||
                    line.startsWith("<s>") ||
                    line.length() == 0)
                    continue;
                // Break the loop when we read the deliminter for the sentence.
                if (line.startsWith("</s>"))
                    break;

                // Append the line as it is to the string builder. Assume that
                // the dependency extractor is able to handle whatever format
                // the lines are in. 
                sb.append(line).append("\n");
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

        // Return null if nothing was read, indicating that the above loop
        // reached the end of it's input.
        if (sb.length() == 0)
            return null;

        return sb.toString();
    }
}

