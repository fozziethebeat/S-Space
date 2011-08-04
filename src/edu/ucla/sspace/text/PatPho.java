package edu.ucla.sspace.text;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * An implementation of the PatPho phonological representation system.   This
 * implementation is based on the following paper. 
 *
 * <ul>
 *     <li style="font-family:Garamond, Georgia, serif">
 *     Li, Ping and MacWhinney, Brian.  2001.  Proceedings of Behavior Research
 *     Methods, Instruments, \& Computers.  2002, Volme 34, Issue 3, 408-415.
 *     </li>
 * </ul>
 *
 * </p>
 *
 * This encoder is intended to transform phonemes into vectors for use in neural
 * networks.  This encoder will transform a phoneme into a vector of three real
 * values.  Given a list of phonemes, it will transform them into a syllbalic
 * templated set of real values.  Up to six phonemes may be passed to this
 * encoder.  This encoder is dependent on the IPA phonological representation
 * scheme.
 *
 * @author Keith Stevens
 */
public class PatPho {
    
    /**
     * A mapping from IPA encoded phonemes to 3 real values, uniquely encoding
     * the phoneme.
     */
    private static final Map<String, double[]> PHONEME_VALUES =
        new HashMap<String, double[]>();

    /**
     * The set of IPA vowel phonemes.
     */
    private static final Set<String> VOWELS = new HashSet<String>();

    /**
     * The set of IPA consonant phonemes.
     */
    private static final Set<String> CONSONANTS = new HashSet<String>();

    /**
     * The indices that correspond to consonants in a vector based phonological
     * representation.
     */
    private final int[] consonantIndices;

    /**
     * The indices that correspond to vowels in a vector based phonological
     * representation.
     */
    private final int[] vowelIndices;

    /**
     * Creates a new {@code PatPho} instance with a six syllablic template.
     */
    public PatPho() {
        this(true);
    }

    /**
     * Creates a new {@code PatPho} instance.  If {@code useSixSyllables} is
     * true, a six syllablic template will be used, otherwise a three syllbalic
     * template will be used.
     */
    public PatPho(boolean useSixSyllables) {
        // Setup the number of consonants.  For each syllable there will be 3
        // consonants.  At the end of the word there will be 3 extra consonants.
        int numConsonants = ((useSixSyllables) ? 6 * 3 : 3 * 3) + 3;

        // Setup the number of vowels.  For each syllable there will be 2
        // vowels.
        int numVowels = (useSixSyllables) ? 6 * 2 : 3 * 2;

        // Setup the vector indices for the consonants.  Each triple of
        // consonants will be separated by 2 vowels, such as CCCVVCCCVVCCC.
        consonantIndices = new int[numConsonants];
        int vectorOffset = 0;
        for (int i = 0; i < numConsonants; ++i, ++vectorOffset) {
            if (i % 3 == 0)
                vectorOffset += 2;
            consonantIndices[i] = vectorOffset;
        }

        // Setup the vector indcies for the vowels.  Each pair of vowels will be
        // separated by 3 consonants, such as in the example above.
        vowelIndices = new int[numVowels];
        vectorOffset = 4;
        for (int i = 0; i < numVowels; ++i, ++vectorOffset) {
            if (i % 2 == 0)
                vectorOffset += 3;
            vowelIndices[i] = vectorOffset;
        }
    }

    /**
     * Returns a copy of the double representation of the given phoneme.
     *
     * @param phoneme A string representation of a phoneme using the IPA format
     *
     * @return The three double values representing the given phoneme
     *
     * @throws NullPointerException If the requested phoneme does not have a
     *                              mapping
     */
    public double[] vectorize(String phoneme) {
        return Arrays.copyOfRange(PHONEME_VALUES.get(phoneme), 0, 3);
    }

    /**
     * Returns a left-justified syllablilic template representation of the given
     * list of phonemes.  Every three values correspond to a single phoneme
     * representation.  If six syllables are used, a vector of 99 values is
     * returned, otherwise a vector of 54 values is returned.
     *
     * @param phonemes A list of string phoneme representation using the IPA
     *                 format
     *
     * @return A vector representing the word
     *
     * @throws NullPointerException If any requested phoneme does not have a
     *                              mapping
     */
    public double[] vectorize(List<String> phonemes) {
        int nextConsonantIndex = 0;
        int nextVowelIndex = 0;
        double[] result = new double[(vowelIndices.length +
                                      consonantIndices.length) * 3];
        for (String phoneme : phonemes) {
            int offset = 3;
            if (VOWELS.contains(phoneme))
                offset *= vowelIndices[nextVowelIndex++];
            else
                offset *= consonantIndices[nextConsonantIndex++];
            double[] values = PHONEME_VALUES.get(phoneme);
            for (int i = 0; i < 3; ++i)
                result[i + offset] = values[i];
        }
        return result;
    }

    /**
     * Setups up the phoneme mappings, vowel set, and consonant set.
     */
    static {
        // Add the vowels to the phoneme mappings.
        PHONEME_VALUES.put("i", new double[]{.1, .1, .1});
        PHONEME_VALUES.put("I", new double[]{.1, .1, .185});
        PHONEME_VALUES.put("e", new double[]{.1, .1, .270});
        PHONEME_VALUES.put("E", new double[]{.1, .1, .355});
        PHONEME_VALUES.put("&", new double[]{.1, .1, .444});
        PHONEME_VALUES.put("@", new double[]{.1, .175, .185});
        PHONEME_VALUES.put("3", new double[]{.1, .175, .270});
        PHONEME_VALUES.put("V", new double[]{.1, .175, .355});
        PHONEME_VALUES.put("a", new double[]{.1, .175, .444});
        PHONEME_VALUES.put("u", new double[]{.1, .250, .1});
        PHONEME_VALUES.put("U", new double[]{.1, .250, .185});
        PHONEME_VALUES.put("O", new double[]{.1, .250, .270});
        PHONEME_VALUES.put("Q", new double[]{.1, .250, .355});
        PHONEME_VALUES.put("A", new double[]{.1, .250, .444});

        // add the vowes to the vowel set.
        VOWELS.add("i");
        VOWELS.add("I");
        VOWELS.add("e");
        VOWELS.add("E");
        VOWELS.add("&");
        VOWELS.add("@");
        VOWELS.add("3");
        VOWELS.add("V");
        VOWELS.add("a");
        VOWELS.add("u");
        VOWELS.add("U");
        VOWELS.add("O");
        VOWELS.add("Q");
        VOWELS.add("A");

        // Add the consonants to the phoneme mappings.
        PHONEME_VALUES.put("p", new double[]{1, .450, .733});
        PHONEME_VALUES.put("t", new double[]{1, .684, .733});
        PHONEME_VALUES.put("k", new double[]{1, .921, .733});
        PHONEME_VALUES.put("b", new double[]{.750, .450, .733});
        PHONEME_VALUES.put("d", new double[]{.750, .684, .733});
        PHONEME_VALUES.put("g", new double[]{.750, .921, .733});
        PHONEME_VALUES.put("m", new double[]{.750, .450, .644});
        PHONEME_VALUES.put("n", new double[]{.750, .684, .644});
        PHONEME_VALUES.put("N", new double[]{.750, .921, .644});
        PHONEME_VALUES.put("l", new double[]{.750, .684, 1});
        PHONEME_VALUES.put("r", new double[]{.750, .684, .911});
        PHONEME_VALUES.put("f", new double[]{1, .528, .822});
        PHONEME_VALUES.put("v", new double[]{.750, .528, .822});
        PHONEME_VALUES.put("s", new double[]{1, .684, .822});
        PHONEME_VALUES.put("z", new double[]{.750, .684, .822});
        PHONEME_VALUES.put("S", new double[]{1, .792, .822});
        PHONEME_VALUES.put("Z", new double[]{.750, .792, .822});
        PHONEME_VALUES.put("j", new double[]{.750, .841, .911});
        PHONEME_VALUES.put("h", new double[]{1, 1, .911});
        PHONEME_VALUES.put("w", new double[]{.750, .921, .911});
        PHONEME_VALUES.put("T", new double[]{1, .606, .822});
        PHONEME_VALUES.put("D", new double[]{.750, .606, .822});
        PHONEME_VALUES.put("C", new double[]{1, .841, .822});
        PHONEME_VALUES.put("J", new double[]{.750, .841, .822});

        // Add the consonants to the consonant set.
        CONSONANTS.add("p");
        CONSONANTS.add("t");
        CONSONANTS.add("k");
        CONSONANTS.add("b");
        CONSONANTS.add("d");
        CONSONANTS.add("g");
        CONSONANTS.add("m");
        CONSONANTS.add("n");
        CONSONANTS.add("N");
        CONSONANTS.add("l");
        CONSONANTS.add("r");
        CONSONANTS.add("f");
        CONSONANTS.add("v");
        CONSONANTS.add("s");
        CONSONANTS.add("z");
        CONSONANTS.add("S");
        CONSONANTS.add("Z");
        CONSONANTS.add("j");
        CONSONANTS.add("h");
        CONSONANTS.add("w");
        CONSONANTS.add("T");
        CONSONANTS.add("D");
        CONSONANTS.add("C");
        CONSONANTS.add("J");
    }
}
