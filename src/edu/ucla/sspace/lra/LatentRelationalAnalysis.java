/*
 * Copyright 2009 Sky Lin
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.sspace.lra;

import static edu.ucla.sspace.common.Similarity.cosineSimilarity;

import edu.ucla.sspace.matrix.LogEntropyTransform;
import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.SVD;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.BoundedSortedMap;
import edu.ucla.sspace.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.lang.Float;
import java.lang.Integer;
import java.lang.Math;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Scanner;
import java.util.SortedMap;

import edu.smu.tspell.wordnet.*;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document; 
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * An implementation of Latent Relational Analysis (LRA).  This implementation is
 * based on two papers.
 * <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"> Peter D. Turney
 *     (2004).  Human-Level Performance on Word Analogy Questions by
 *     Latent Relational Analysis. Available <a
 *     href="http://iit-iti.nrc-cnrc.gc.ca/iit-publications-iti/docs/NRC-47422.pdf">here</a> </li>
 * 
 * <li style="font-family:Garamond, Georgia, serif"> Peter D. Turney (2005).
 *    Measuring Semantic Similarity by Latent Relational Analysis.
 *    Available
 *    <a href="http://portal.acm.org/citation.cfm?id=1174523">here</a>
 *    </li>
 *
 * </ul>
 * <p>
 *
 * LRA uses three main components to analyze a large corpus of text in order to
 * measure relational similarity between pairs of words (i.e. analogies). LRA
 * uses the search engine to find patterns based on the input set as well as its
 * corresponding alternates (see {@link #loadAnalogiesFromFile(String)}). A
 * sparse matrix is then generated, where each value in the matrix is the number
 * of times the row's word pair occurs with the column's pattern between
 * them.<p>
 *
 * After the matrix has been built, the <a
 * href="http://en.wikipedia.org/wiki/Singular_value_decomposition">Singular
 * Value Decomposition</a> (SVD) is used to reduce the dimensionality of the
 * original word-document matrix, denoted as <span style="font-family:Garamond,
 * Georgia, serif">A</span>. The SVD is a way of factoring any matrix A into
 * three matrices <span style="font-family:Garamond, Georgia, serif">U &Sigma;
 * V<sup>T</sup></span> such that <span style="font-family:Garamond, Georgia,
 * serif"> &Sigma; </span> is a diagonal matrix containing the singular values
 * of <span style="font-family:Garamond, Georgia, serif">A</span>. The singular
 * values of <span style="font-family:Garamond, Georgia, serif"> &Sigma; </span>
 * are ordered according to which causes the most variance in the values of
 * <span style="font-family:Garamond, Georgia, serif">A</span>. The original
 * matrix may be approximated by recomputing the matrix with only <span
 * style="font-family:Garamond, Georgia, serif">k</span> of these singular
 * values and setting the rest to 0. The approximated matrix <span
 * style="font-family:Garamond, Georgia, serif"> &Acirc; = U<sub>k</sub>
 * &Sigma;<sub>k</sub> V<sub>k</sub><sup>T</sup></span> is the least squares
 * best-fit rank-<span style="font-family:Garamond, Georgia, serif">k</span>
 * approximation of <span style="font-family:Garamond, Georgia, serif">A</span>.
 * LRA reduces the dimensions by keeping only the first <span
 * style="font-family:Garamond, Georgia, serif">k</span> dimensions from the row
 * vectors of <span style="font-family:Garamond, Georgia, serif">U</span> and the
 * <span style="font-family:Garamond, Georgia, serif">k</span> dimensions from the
 * column vectors of <span style="font-family:Garamond, Georgia, serif">&Sigma;</span>.
 * The projection matrix <span style="font-family:Garamond, Georgia, serif">U&Sigma;
 * </span> is then used to calculate the relational similarities between pairs using
 * the row vectors corresponding to the word pairs.<p>
 *
 * This class uses the <a href="http://lucene.apache.org/java/docs/">Apache
 * Lucune Search Engine</a> for optimal indexing and filtering of word pairs
 * using any given corpus.  This class also uses Wordnet through the <a
 * href="http://lyle.smu.edu/~tspell/jaws/index.html">JAWS</a> interface in
 * order to find alternate word pairs from given input pairs.
 *
 *
 * @author Sky Lin
 **/
public class LatentRelationalAnalysis {

    //TODO: have a way to set these values
    public static final String LRA_DIMENSIONS_PROPERTY =
	"edu.ucla.sspace.lra.LRA.dimensions";
    public static final String LRA_INDEX_DIR =
	"edu.ucla.sspace.lra.LRA.index_dir";
    public static final String LRA_SKIP_INDEX =
	"edu.ucla.sspace.lra.LRA.skip_index";
    public static final String LRA_READ_MATRIX_FILE =
	"edu.ucla.sspace.lra.LRA.readMatrixFile";
    public static final String LRA_WRITE_MATRIX_FILE =
	"edu.ucla.sspace.lra.LRA.writeMatrixFile";

    private static final int NUM_SIM = 10; 
    private static final int MAX_PHRASE = 5; 
    private static final int NUM_FILTER = 3;
    private static final int MAX_INTER = 3;
    private static final int MIN_INTER = 1;
    private static final int NUM_PATTERNS = 4000;

    private List<String> original_pairs;
    private List<String> filtered_phrases;
    private Map<String,ArrayList<String>> original_to_alternates;
    private BoundedSortedMap<InterveningWordsPattern, Integer> pattern_list;
    private Map<Integer,String> matrix_row_map; 
    private Map<Integer,InterveningWordsPattern> matrix_column_map; 

    private String INDEX_DIR;
    private String DATA_DIR;

    /**
     * Constructor for {@code LatentRelationalAnalysis}.
     *
     * @param corpus_directory a {@code String} containing the absolute path to
     *        the directory containing the corpus
     * @param index_directory a {@code String} containing the absolute path to
     *        the directory where the index created by Lucene will be stored
     * @param do_index {@code true} if the index step should be performed.
     *        {@code false} if the index file already exists under
     *        index_directory (will skip indexing step).
     *
     */
    public LatentRelationalAnalysis(String corpus_directory, 
				    String index_directory, 
				    boolean do_index) {
        //set system property for Wordnet database directory
        Properties sysProps = System.getProperties();
        sysProps.setProperty("wordnet.database.dir","/usr/share/wordnet");

        System.err.println("starting LRA...\n");

        INDEX_DIR = index_directory;
        DATA_DIR = corpus_directory;
        if (do_index) {
            initializeIndex(INDEX_DIR, DATA_DIR);
        }

        original_pairs = new ArrayList<String>();
        filtered_phrases = new ArrayList<String>();
        original_to_alternates = new HashMap<String, ArrayList<String>>();
        pattern_list = new BoundedSortedMap<InterveningWordsPattern, Integer>(NUM_PATTERNS);
        matrix_column_map = new HashMap<Integer, InterveningWordsPattern>();
        matrix_row_map = new HashMap<Integer, String>();
    } 

    /**
     * Loads the analogies from an input file.
     * The file must contain word pairs in the form of A:B separated by newlines.
     *
     * @param analogy_file a {@code String} containing the absolute path to the analogy file. 
     */
    public void loadAnalogiesFromFile(String analogy_file) {

            try {
                Scanner sc = new Scanner(new File(analogy_file));
                while (sc.hasNext()) {
                    String analogy = sc.next();
                    if (!isAnalogyFormat(analogy)) {
                        System.err.println("\"" + analogy + "\" not in proper format.");
                        continue;
                    }
                    String analogy_pair[] = analogy.split(":");
                    String A = analogy_pair[0];
                    String B = analogy_pair[1];

                    //1. Find alternates for A and B
                    Synset[] A_prime = findAlternatives(A);
                    Synset[] B_prime = findAlternatives(B);
                    
                    //2. Filter phrases
                    ArrayList<String> tmp = new ArrayList<String>(filterPhrases(INDEX_DIR,A,B,A_prime,B_prime));
                    filtered_phrases.addAll(tmp);
                    original_to_alternates.put(A+":"+B, tmp);
                }
                sc.close();
            } catch (Exception e) {
                System.err.println("Could not read file.");
            }
    }

    /**
     * Returns the synonyms for the specified term.  The synonyms will be taken
     * directly from the WordNet database.  This is used by LRA to find
     * alternative pairs. Given an input set of A:B.  For each A' that is
     * similar to A, make a new pair A':B.  Likewise for B.
     *
     * @param term a {@code String} containing a single word
     * @return an array of all the synonyms 
     */
    public static Synset[] findAlternatives(String term) {
        WordNetDatabase database = WordNetDatabase.getFileInstance();   
        Synset[] all = database.getSynsets(term);
        return all;
    }

    /**
     * Initializes an index given the index directory and data directory.
     *
     * @param indexDir a {@code String} containing the directory where the index
     *        will be stored
     * @param dataDir a {@code String} containing the directory where the data
     *        is found
     */
    public static void initializeIndex(String indexDir, String dataDir) {
        File indexDir_f = new File(indexDir);
        File dataDir_f = new File(dataDir);

        long start = new Date().getTime();
        try {
            int numIndexed = index(indexDir_f, dataDir_f);
            long end = new Date().getTime();

            System.err.println("Indexing " + numIndexed + " files took " + (end -start) + " milliseconds");
        } catch (IOException e) {
            System.err.println("Unable to index "+indexDir_f+": "+e.getMessage());
        }
    }

    //creates the index files
    private static int index(File indexDir, File dataDir) 
        throws IOException {
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            throw new IOException(dataDir
                    + " does not exist or is not a directory");
        }

        IndexWriter writer = new IndexWriter(indexDir,
                    new StandardAnalyzer(), true, IndexWriter.MaxFieldLength.UNLIMITED);
        writer.setUseCompoundFile(false);

        indexDirectory(writer, dataDir);

        int numIndexed = writer.numDocs();
        writer.optimize();
        writer.close();
        return numIndexed;
    }

    /**
     * recursive method that finds interleving patterns between A and B in all files
     * within a given directory
     **/
    private static HashSet<String> searchDirectoryForPattern(File dir,String A, String B) 
        throws Exception {
        
        File[] files = dir.listFiles();

        HashSet<String> pattern_set = new HashSet<String>();

        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                pattern_set.addAll(searchDirectoryForPattern(f, A, B));
            } else if (f.getName().endsWith(".txt")) {
                Scanner sc = new Scanner(f);
                while (sc.hasNext()) {
                    if (A.equals(sc.next())) {
                        String pattern = "";
                        int count = 0;
                        while (count <= MAX_INTER && sc.hasNext()) { 
                            String curr = sc.next();
                            if (count >= MIN_INTER && B.equals(curr)) {
                                //add the String onto a Set of Strings containing the patterns
                                //System.err.println("adding pattern: " + pattern);
                                pattern_set.add(pattern);
                                break;
                                /*
                                for (int j = 0; j < count; j++) {
                                    System.err.print(pattern[j] + " ");
                                }
                                    System.err.print("\n");
                                */
                            } else {
                                if (count > 0) {
                                    pattern += " ";
                                }
                                pattern += curr;
                                count++;
                            }
                        }
                    }
                }
            }
        }
        return pattern_set;
    }

    /**
     * recursive method that calls itself when it finds a directory, or indexes if
     * it is at a file ending in ".txt"
     **/
    private static void indexDirectory(IndexWriter writer, File dir)
        throws IOException {
        
        File[] files = dir.listFiles();

        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                indexDirectory(writer, f);
            } else if (f.getName().endsWith(".txt")) {
                indexFile(writer, f);
            }
        }
    }
   
   /**
    * method to actually index a file using Lucene, adds a document
    * onto the index writer
    **/
   private static void indexFile(IndexWriter writer, File f)
        throws IOException {

    if (f.isHidden() || !f.exists() || !f.canRead()) {
        System.err.println("Could not write "+f.getName());
        return;
    }

    System.err.println("Indexing " + f.getCanonicalPath());

    Document doc = new Document();

    doc.add(new Field("path", f.getCanonicalPath(), Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc.add(new Field("modified",DateTools.timeToString(f.lastModified(), DateTools.Resolution.MINUTE),Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc.add(new Field("contents", new FileReader(f)));
                
    writer.addDocument(doc);
   }

    /**
     * Searches an index given the index directory and counts up the frequncy of the two words used in a phrase.
     *
     * @param indexDir a String containing the directory where the index is stored
     * @param A a {@code String} containing the first word of the phrase
     * @param B a {@code String} containing the last word of the phrase
     * @return float 
     */
    public static float countPhraseFrequencies(String indexDir, String A, String B) {
        File indexDir_f = new File(indexDir);

        if (!indexDir_f.exists() || !indexDir_f.isDirectory()) {
            System.err.println("Search failed: index directory does not exist");
        } else {
            try {
                return searchPhrase(indexDir_f, A, B);
            } catch (Exception e) {
                System.err.println("Unable to search "+indexDir);
                return 0;
            }
        }
        return 0;
    }

    //method that actually does the searching
    private static float searchPhrase(File indexDir, String A, String B) 
        throws Exception {
        Directory fsDir = FSDirectory.getDirectory(indexDir);
        IndexSearcher searcher = new IndexSearcher(fsDir);

        long start = new Date().getTime();
        QueryParser parser = new QueryParser("contents",new StandardAnalyzer());
        //System.err.println("searching for: '\"" + A + " " + B + "\"~"+MAX_PHRASE+"'");
        parser.setPhraseSlop(MAX_PHRASE);
        String my_phrase = "\"" + A + " " + B + "\"";
        Query query = parser.parse(my_phrase);
        //System.err.println("total hits: " + results.totalHits);

        //set similarity to use only the frequencies
        //score is based on frequency of phrase only
        searcher.setSimilarity(
                new Similarity() {
                   public static final long serialVersionUID = 1L;
                   public float coord(int overlap, int maxOverlap) {
                      return 1;
                   } 
                   public float queryNorm(float sumOfSquaredWeights) {
                      return 1;
                   } 
                   public float tf(float freq) {
                      return freq;
                   } 
                   public float idf(int docFreq, int numDocs) {
                      return 1;
                   } 
                   public float lengthNorm(String fieldName, int numTokens) {
                      return 1;
                   } 
                   public float sloppyFreq(int distance) {
                       return 1;
                   }
        });
        TopDocs results = searcher.search(query,10);

        ScoreDoc[] hits = results.scoreDocs;
        float total_score = 0;
        //add up the scores
        for (ScoreDoc hit : hits) {
            Document doc = searcher.doc(hit.doc);
            //System.err.printf("%5.3f %sn\n",
             //   hit.score, doc.get("contents"));
            total_score += hit.score;
        }

        long end = new Date().getTime();
        searcher.close();

        return total_score;
    }


    /**
     * Returns an ArrayList of phrases with the greatest frequencies in the corpus.
     * For each alternate pair, send a phrase query to the Lucene search engine
     * containing the corpus.  The phrase query will find the frequencies of phrases
     * that begin with one member of the pair and end with the other.  The phrases
     * cannot have more than MAX_PHRASE words.
     * Select the top NUM_FILTER (current NUM_FILTER=3) most frequent phrases and 
     *
     * return them along with the original pairs.
     *
     * NOTE: should be called before {@link #findPatterns()}.
     *
     * @param A a {@code String} containing the first member in the original pair 
     * @param B a {@code String} containing the second member in the original pair 
     * @param A_prime a {@code Synset} array containing the alternates for A 
     * @param B_prime a {@code Synset} array containing the alternates for B 
     * @return  an ArrayList of {@code String} with the top NUM_FILTER pairs along with the original pairs 
     */
    public static ArrayList<String> filterPhrases (String INDEX_DIR, String A, String B, Synset[] A_prime, Synset[] B_prime) {
        HashMultiMap<Float,Pair<String>> phrase_frequencies  = new HashMultiMap<Float,Pair<String>>();
        //Search corpus... A:B
        //phrase_frequencies.put(new Float(countPhraseFrequencies(INDEX_DIR, A, B)),new Pair<String>(A,B)); 
        //System.err.println("Top 10 Similar words:");
        int count = 0;
        for (int i = 0; (i < NUM_SIM && i < A_prime.length); i++) {
            String[] wordForms = A_prime[i].getWordForms();
            for (int j = 0; j < wordForms.length; j++)
            {
                if (wordForms[j].compareTo(A) != 0) {
                    //Search corpus... A':B
                    Float score = new Float(countPhraseFrequencies(INDEX_DIR, wordForms[j], B));
                    phrase_frequencies.put(score,new Pair<String>(wordForms[j],B)); 
                    count++;
                }

                if(count >= NUM_SIM)
                    break;
            }
            if(count >= NUM_SIM)
                break;
        }
        count = 0;
        for (int i = 0; (i < NUM_SIM && i < B_prime.length); i++) {
            String[] wordForms = B_prime[i].getWordForms();
            for (int j = 0; j < wordForms.length; j++)
            {
                if (wordForms[j].compareTo(B) != 0) {
                    //Search corpus... A:B'
                    Float score = new Float(countPhraseFrequencies(INDEX_DIR,A, wordForms[j]));
                    phrase_frequencies.put(score,new Pair<String>(A,wordForms[j])); 
                    count++;
                }

                if(count >= NUM_SIM)
                    break;
            }
            if(count >= NUM_SIM)
                break;
        }
        
        // filter out the phrases and add the top 3 to the ArrayList, and return it
        Iterator iter = phrase_frequencies.keySet().iterator();
        //TODO: make number of filters dynamic
        //create Array with size = num filters
        ArrayList<String> filtered_phrases = new ArrayList<String>();
        Float filter1 = new Float(0.0);
        Float filter2 = new Float(0.0); 
        Float filter3 = new Float(0.0);
        while (iter.hasNext()) {
            Float curr_key = (Float)iter.next();
            //this will bump the filters up each time a greater value comes along
            //so that filter1 will be the greatest key and filter3 the 3rd greatest
            if (curr_key > filter1) {
                filter3 = filter2;
                filter2 = filter1; 
                filter1 = curr_key;
            } else if (curr_key > filter2) {
                filter3 = filter2;
                filter2 = curr_key;
            } else if (curr_key > filter3) {
                filter3 = curr_key;
            }
        }
        int filter_count = 0;
        Iterator val_iter = phrase_frequencies.get(filter1).iterator();
        while (val_iter.hasNext() && filter_count < 3) {
            String alternative_pair = val_iter.next().toString();
            String pair_arr[] = parsePair(alternative_pair);
            filtered_phrases.add(pair_arr[0]+":"+pair_arr[1]);
            filter_count++;
        }
        val_iter = phrase_frequencies.get(filter2).iterator();
        while (val_iter.hasNext() && filter_count < 3) {
            String alternative_pair = val_iter.next().toString();
            String pair_arr[] = parsePair(alternative_pair);
            filtered_phrases.add(pair_arr[0]+":"+pair_arr[1]);
            filter_count++;
        }
        val_iter = phrase_frequencies.get(filter3).iterator();
        while (val_iter.hasNext() && filter_count < 3) {
            String alternative_pair = val_iter.next().toString();
            String pair_arr[] = parsePair(alternative_pair);
            filtered_phrases.add(pair_arr[0]+":"+pair_arr[1]);
            filter_count++;
        }
        //throw in the original pair also
        filtered_phrases.add(A+":"+B);

        return filtered_phrases;
    }

    /**
     * Makes patterns by replacing words in str with wildcards based on the binary value of c. 
     */
    private static String combinatorialPatternMaker(String[] str, int str_size, int c) {
        String comb_pattern = "";
        int curr_comb = 1;
        for (int i = 0; i < str_size; i++) {
            if ((c & curr_comb) != 0) {
                comb_pattern += str[i] + "\\s";
            } else {
                comb_pattern += "[\\w]+\\s";
            }
            curr_comb = curr_comb << 1;
        }
        //System.err.println(comb_pattern);
        return comb_pattern;
    }

    /**
     * Searches through all the .txt files in a directory and returns the total number
     * of occurrences of a pattern.
     */
    private static int countWildcardPhraseFrequencies(File dir, String pattern)
        throws Exception {
        
        File[] files = dir.listFiles();

        int total = 0;

        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                total += countWildcardPhraseFrequencies(f, pattern);
            } else if (f.getName().endsWith(".txt")) {
                Scanner sc = new Scanner(f);
                while (sc.hasNext()) {
                    String line = sc.nextLine();
                    if (line.matches(pattern)) {
                        total++;
                    }
                }
            }
        }
        return total;
    }

    /**
     * parses a pair in the form {A, B}
     **/
    private static String[] parsePair (String pair) {
        String[] tmp = new String[2];
        int indexOfA = pair.indexOf('{')+1;
        int indexOfB = pair.indexOf(',');
        tmp[0] = pair.substring(indexOfA,indexOfB);
        tmp[1] = pair.substring(indexOfB+2,pair.length()-1);

        return tmp;
    }
    
    /**
     * Finds patterns using the filtered phrases.  Should be called after {@link
     * #filterPhrases(String,String,Syntex[],Synset[]) filterPhrases}.
     **/
    public void findPatterns() 
        throws Exception {
        HashSet<String> patterns = new HashSet<String>();
        for (String phrase : filtered_phrases) {
            String phrase_arr[] = phrase.split(":");
            String A = phrase_arr[0];
            String B = phrase_arr[1];
            //System.err.println(A + ": " + B);

            patterns.addAll(searchDirectoryForPattern(new File(DATA_DIR), A, B));
        }
        Iterator iter = patterns.iterator();
        while (iter.hasNext()) {
            String curr_pattern_str = (String)iter.next();
            String[] curr_pattern = curr_pattern_str.split("\\s");
            int curr_length = curr_pattern.length;
            //System.err.println("length of pattern: " + curr_length);
            //do a for loop with all combinatorials of wildcard patterns
            //for each iteration do a wildcard search
            for (int comb = 0; comb < (int)Math.pow(2.0,(double)curr_length); comb++) {
                String comb_pattern = "\\s" + combinatorialPatternMaker(curr_pattern, curr_length, comb);
                try {
                    int score = countWildcardPhraseFrequencies(new File(DATA_DIR), ".*" + comb_pattern + ".*");
                    InterveningWordsPattern db_pattern = new InterveningWordsPattern(comb_pattern);
                    db_pattern.setOccurrences(score);
                    pattern_list.put(db_pattern, score); //insert the pattern into database (only if it has a high enough score)

                    //System.err.println(comb_pattern + ": " + score);
                } catch (Exception e) {
                    System.err.println("could not perform wildcard search");
                }
            }
        }
    }

    /**
     * Maps a list of patterns to the columns of the sparse matrix.  Takes the
     * results of {@link #findPattern()} and maps it to the column indeces of a sparse
     * matrix.
     */
    public void mapColumns() {
            //System.err.print("Patterns found: ");
            //System.err.println(pattern_list.size());
        
            int index = 0;
            //NOTE: occurrences can be used as Sigma X<k,j> when calculating Entropy
            for (InterveningWordsPattern a_pattern : pattern_list.keySet()) {
                //int val = a_pattern.getOccurrences();
                //System.err.println(a_pattern.getPattern() + " " + val);
                matrix_column_map.put(new Integer(index), a_pattern);
                index++;
                InterveningWordsPattern b_pattern = new InterveningWordsPattern(a_pattern.getPattern());
                b_pattern.setOccurrences(a_pattern.getOccurrences());
                b_pattern.setReverse(true);
                matrix_column_map.put(new Integer(index), b_pattern);
                index++;
            }
    }
    
    /**
     * Maps a list of phrases to the rows of the sparse matrix.
     * Takes an ArrayList containing the filtered phrases (originals and alternates) and maps them to the sparse matrix.
     *
     * @param phrases an ArrayList containing the filtered phrases 
     * @return void
     */
    public void mapRows() {
            int index = 0;
            for (String a_phrase : filtered_phrases) {
                String[] curr = a_phrase.split(":");
                String A = curr[0];
                String B = curr[1];
                matrix_row_map.put(new Integer(index), A + ":" + B);
                index++;
                //add reverse pair as well
                matrix_row_map.put(new Integer(index), B + ":" + A);
                index++;
            }
    }

    /**
     * Creates the sparse matrix.  Should be called after {@link
     * #findPatterns()}, {@link #mapRows()}, and {@link #mapColumns()}.  The
     * returned {@code Matrix} should be used in the SVD process.
     *
     * @return the sparse Matrix.
     **/
    public Matrix createSparseMatrix() {

        Matrix m = Matrices.create(matrix_row_map.size(), matrix_column_map.size(), false);
        for (int row_num = 0; row_num < matrix_row_map.size(); row_num++) { // for each pattern
            String p = matrix_row_map.get(new Integer(row_num));
            String[] p_sp = p.split(":");
            String a = p_sp[0];
            String b = p_sp[1];
            for (int col_num = 0; col_num < matrix_column_map.size(); col_num++) { // for each phrase
                InterveningWordsPattern col_pattern = matrix_column_map.get(new Integer(col_num));
                String pattern = col_pattern.getPattern();
                String comb_patterns;
                if (col_pattern.getReverse()) { //if the column is a reverse pattern...word2 P word1
                    comb_patterns = ".*\\s" + b + pattern + a + "\\s.*";
                } else {
                    comb_patterns = ".*\\s" + a + pattern + b + "\\s.*";
                }
                try {
                    m.set(row_num, col_num, (double)countWildcardPhraseFrequencies(new File(DATA_DIR), comb_patterns));
                } catch (Exception e) {
                    System.err.println("could not perform wildcard search");
                }
            }
        }
        System.err.println("\nCompleted matrix generation.");
        //System.err.println("Number of rows: " + m.rows());
        //System.err.println("Number of cols: " + m.columns());
        return m;
    }

    /**
     * Applies log and entropy transformations to the sparse matrix [Landauer and Dumais, 1997].
     *
     * @return the sparse Matrix after log and entropy transformations.
     **/
    public Matrix applyEntropyTransformations(Matrix mat) {
        int n = mat.columns();
        int m = mat.rows();
        for (int col_num = 0; col_num < n; col_num++) {
            double col_total = 0.0;
            for (int row_num = 0; row_num < m; row_num++) {
                col_total += mat.get(row_num,col_num);
            }
            //System.err.println("coltotal: " + col_total);
            if (col_total == 0.0) 
                continue;

            double entropy = 0.0;
            for (int row_num = 0; row_num < m; row_num++) {
                double p = mat.get(row_num,col_num)/col_total;
                //System.err.print(p + " ");
                if (p==0.0)
                    continue;
                entropy += p * Math.log10(p);
            }
            //System.err.println("entropy: " + entropy);
            entropy *= -1;
            double w = 1 - entropy/Math.log10(m);
            //System.err.println("w: " + w);
            for (int row_num = 0; row_num < m; row_num++) {
                mat.set(row_num, col_num, w*Math.log10(mat.get(row_num, col_num) + 1.0));
            }
        }
        return mat;
    }

    /**
     * returns the index of the String in the HashMap, or -1 if value was not found.
     **/
    private static int getIndexOfPair(String value, Map<Integer, String> row_data) {
        for(Integer i : row_data.keySet()) {
            if(row_data.get(i).equals(value)) {
                return i.intValue();
            }
        } 
        return -1;
    }
    
    /**
     * Computes the cosine similarity of an analogy using the projection matrix.
     * The relational similarity between A:B and C:D is the average of the cosines
     * values between combinations of the similar pairs.  The cosines from 
     * the similar pairs must be greater than or equal to the cosine of the 
     * original pairs, A:B and C:D.
     *
     * @param analogy a String containing the two pairs to compare.  The analogy must be in the form A:B::C:D, where A:B and C:D are two analogies from the input set
     * @param m the projection Matrix
     * @return a double value containing the cosine similarity value of the analogy 
     **/
    public double computeCosineSimilarity(String analogy, Matrix m) {

        double cosineVals = 0.0;
        int totalVals = 0;
        if (!isAnalogyFormat(analogy, true)) {
            System.err.println("Analogy: \"" + analogy + "\" not in proper format");
            return 0.0;
        }
        String pairs[] = analogy.split("::");
        String pair1 = pairs[0];
        String pair2 = pairs[1];
        if (!isAnalogyFormat(pair1) || !isAnalogyFormat(pair2)) {
            System.err.println("Analogy: \"" + analogy + "\" not in proper format");
            return 0.0;
        }

        if(!original_to_alternates.containsKey(pair1) || !original_to_alternates.containsKey(pair2)) {
            //check if the reverse pair exists
            String pair1_pair[] = pair1.split(":");
            String pair1_a = pair1_pair[1];
            String pair1_b = pair1_pair[0];
            String pair2_pair[] = pair2.split(":");
            String pair2_a = pair2_pair[1];
            String pair2_b = pair2_pair[0];
            pair1 = pair1_a+":"+pair1_b; 
            pair2 = pair2_a+":"+pair2_b; 
            if(!original_to_alternates.containsKey(pair1) || !original_to_alternates.containsKey(pair2)) {
                System.err.println("Analogy: \"" + analogy + "\" not included in original pairs");
                return 0.0;
            }
        }
        double original_cosineVal = cosineSimilarity(m.getRow(getIndexOfPair(pair1, matrix_row_map)), m.getRow(getIndexOfPair(pair2, matrix_row_map)));
        cosineVals += original_cosineVal;
        totalVals++;
        //System.err.println("orig cos: " + cosineVals);
        ArrayList<String> alternates1 = original_to_alternates.get(pair1);
        ArrayList<String> alternates2 = original_to_alternates.get(pair2);
        for (String a : alternates1) {
            for (String b : alternates2) {
                int a_index = getIndexOfPair(a, matrix_row_map);
                int b_index = getIndexOfPair(b, matrix_row_map);
                if(a_index != -1 && b_index != -1) {
                    double alternative_cosineVal = cosineSimilarity(m.getRow(a_index),m.getRow(b_index));
                    //System.err.println("adding cos: " + alternative_cosineVal);
                    if (alternative_cosineVal >= original_cosineVal) {
                        cosineVals += alternative_cosineVal;
                        totalVals++;
                    }
                }
            }
        }

        if (totalVals > 0) {
            return cosineVals/totalVals;
        } else {
            return 0.0;
        }
    }

    /**
     * Does the Singular Value Decomposition using the generated sparse matrix.
     * The dimensions used cannot exceed the number of columns in the original matrix. 
     *
     * @param sparse_matrix the sparse {@code Matrix}
     * @param dimensions the number of singular values to calculate
     * @return a {@code double} containing the cosine similarity value of the analogy 
     **/
    public static Matrix[] computeSVD(Matrix sparse_matrix, int dimensions) {
            try {
            File rawTermDocMatrix = 
                File.createTempFile("lra-term-document-matrix", ".dat");
            MatrixIO.writeMatrix(sparse_matrix, rawTermDocMatrix, MatrixIO.Format.SVDLIBC_SPARSE_TEXT); 
            Matrix[] usv = SVD.svd(rawTermDocMatrix, SVD.Algorithm.SVDLIBC, MatrixIO.Format.SVDLIBC_SPARSE_TEXT, dimensions);

            if (usv[1].rows() < usv[0].columns()) { //can't do projection, if the dimensions don't match up...redo SVD with updated dimensions
                dimensions = usv[1].rows();
                System.err.println("Default dimensions too big...redoing SVD with new dimensions, k" + "=" + dimensions + " ...");
                usv = SVD.svd(rawTermDocMatrix, SVD.Algorithm.SVDLIBC, MatrixIO.Format.SVDLIBC_SPARSE_TEXT, dimensions);
            }
            return usv;
            } catch (Exception e){
                System.err.println("could not compute SVD\n");
                return null;
            }
    }

    /**
     * Reads analogies from file and outputs their cosine similarities to another file.
     *
     * @param projection the projection {@code Matrix}
     * @param inputFileName the input file containing analogies in the proper format
     * separated by newlines
     * @param outputFileName the output file where the results will be stored
     * @return void
     *
     * @see #computeCosineSimilarity(String,Matrix)
     **/
    public void evaluateAnalogies(Matrix projection, String inputFileName, String outputFileName) {
            try {
                Scanner sc = new Scanner(new File(inputFileName));
                PrintStream out = new PrintStream(new FileOutputStream(outputFileName));
                while (sc.hasNext()) {
                    String analogy = sc.next();
                    if (!isAnalogyFormat(analogy,true)) {
                        System.err.println("\"" + analogy + "\" not in proper format.");
                        continue;
                    }
                    double cosineVal = computeCosineSimilarity(analogy, projection); //does the actual cosine value calculations and comparisons
                    out.println(analogy + " = " + cosineVal);
                }
                sc.close();
                out.close();
            } catch (Exception e) {
                System.err.println("Could not read file.");
            }
    }

    /**
     * Reads analogies from Standard In and outputs their cosine similarities to Standard Out.
     *
     * @param projection the projection {@code Matrix}
     *
     * @see #computeCosineSimilarity(String,Matrix)
     **/
    public void evaluateAnalogies(Matrix projection) {
            try {
                Scanner sc = new Scanner(System.in);
                while (sc.hasNext()) {
                    String analogy = sc.next();
                    if (!isAnalogyFormat(analogy,true)) {
                        System.err.println("\"" + analogy + "\" not in proper format.");
                        continue;
                    }
                    double cosineVal = computeCosineSimilarity(analogy, projection); //does the actual cosine value calculations and comparisons
                    System.out.println(analogy + " = " + cosineVal);
                }
                sc.close();
            } catch (Exception e) {
                System.err.println("Could not read file.");
            }
    }

    /**
     * prints the {@code Matrix} to standard out.
     *
     * @param rows an {@code int} containing the number of rows in m
     * @param cols an {@code int} containing the number of cols in m
     * @param m the {@code Matrix} to print
     * @return void
     **/
    public static void printMatrix(int rows, int cols, Matrix m) {
        for(int col_num = 0; col_num < cols; col_num++) {
            for (int row_num = 0; row_num < rows; row_num++) {
                System.out.print(m.get(row_num,col_num) + " ");
            }
            System.out.print("\n");
        }
        System.out.print("\n");
    }

    /**
     * Checks whether the analogy is in the proper format.
     * An analogy is in proper format if it contains two {@code Strings} separated by
     * a colon (:)
     *
     * @param analogy a {@code String} containing the two pairs to compare.  The analogy should be in the form A:B 
     * @return true if the analogy is in proper format
     **/
    public static boolean isAnalogyFormat(String analogy) {
        return isAnalogyFormat(analogy,false);
    }

    /**
     * Checks whether the analogy is in the proper format.
     * An analogy is in proper format if it contains two {@code Strings} separated by
     * a colon (:), or two colons (::) if it is a pair of analogies.
     *
     * @param analogy a {@code String} containing the two pairs to compare.  
     * The analogy should be in the form A:B if it is not a pair, or 
     * A:B::C:D if it is a pair of analogies. 
     * @param pair true if it is a pair of analogies
     * @return true if the analogy is in proper format
     **/
    public static boolean isAnalogyFormat(String analogy, boolean pair) {
        if (pair) {
            return analogy.matches("[\\w]+:[\\w]+::[\\w]+:[\\w]+");
        } else {
            return analogy.matches("[\\w]+:[\\w]+");    
        }
    }

    /*
    //sample main function
    public static void main(String[] args) {

        System.err.println("skipping indexing step...");

        String index= "/argos/lra/index_textbooks/"; 
        String data= "/bigdisk/corpora/textbooks/";
        LRA lra = new LRA(data,index,false);

        try {
            //load analogy input
            lra.loadAnalogiesFromFile("/home/chippoc/analogies.txt");

            //3. Get patterns 4. Filter top NUM_PATTERNS
            lra.findPatterns();

            //5. Map phrases to rows 
            lra.mapRows();
            //6. Map patterns to columns 
            lra.mapColumns();


            //7. Create sparse matrix 
            Matrix sparse_matrix = lra.createSparseMatrix();

            //8. Calculate entropy

            System.err.println("Calculating entropy...");
            sparse_matrix = lra.applyEntropyTransformations(sparse_matrix);

            //printMatrix(sparse_matrix.rows(), sparse_matrix.columns(), sparse_matrix);
            

            //Matrix tmp_matrix = MatrixIO.readMatrix(rawTermDocMatrix, MatrixIO.Format.SVDLIBC_SPARSE_TEXT,Matrix.Type.SPARSE_IN_MEMORY); 
            //printMatrix(tmp_matrix.rows(), tmp_matrix.columns(), tmp_matrix);

            //9. Compute SVD on the pre-processed matrix.
            int dimensions = 300;
            Matrix[] usv = lra.computeSVD(sparse_matrix, dimensions);

            //10. Compute projection matrix from U and S.
            Matrix projection = Matrices.multiply(usv[0],usv[1]);

            printMatrix(projection.rows(), projection.columns(), projection);

            System.err.println("Completed LRA...\n");

            //11. Get analogy input and Evaluate Alternatives
            String inputFile = "/home/chippoc/testIn.txt";
            String outputFile= "/home/chippoc/testOut.txt";
            lra.evaluateAnalogies(projection, inputFile, outputFile);

        } catch (Exception e) {
            System.err.println("FAILURE");
        } 
    }
    */
}

