import scala.io.Source
import java.io.PrintWriter

object ExtractTop10TermsScala {
    def main(args: Array[String]) {
        // Iterate through all top 10 term files and read the topic lines.  For
        // each line, split the terms and add them to the keep set.
        val keepSet = (for ( top10File <- args.slice(1, args.length);
                             line <- Source.fromFile(top10File).getLines;
                             term <- line.split("\\s+") )
                          yield term.toLowerCase.trim).toSet

        // Now pass through all the raw counts read from standard in.  Lines
        // come in two formats, "{word1, word2} count" or "{,word} count".  If
        // both two words occur and both are in the keepSet, write them to the
        // pairCounts file.  If the one word occurs in the keepSet, write that
        // to the single file.

        // First open the output files.
        val countWriter = new PrintWriter(args(0))

        // Create the regular expressions for matching our lines of interest.
        val pairCount = """\{(\S+),\s*([^}]+)\}\s*(\d+)""".r
        val singleCount = """\{\s*,\s*([^}]+)\}\s*(\d+)""".r
        for (line <- Source.fromInputStream(System.in).getLines) {
            line match {
                case pairCount(word1, word2, count) =>
                    if (keepSet.contains(word1) && keepSet.contains(word2))
                        countWriter.printf("%s %s %s\n", word1, word2, count)
                case singleCount(word1, count) =>
                    if (keepSet contains word1)
                        countWriter.printf("%s %s\n", word1, count)
            }
        }
        countWriter.close
    }
}
