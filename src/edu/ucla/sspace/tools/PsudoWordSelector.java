/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
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

package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.ArgOptions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * A utility for selecting a set of pseudo words.  It takes in a list of words
 * and their scores, and optionally a list of words that should serve as the
 * basis for any psuedoword.  The output will be a list of mappings from the
 * original term to it's pseudo word.  This also supports separation by parts of
 * speech.  When using parts of speech, confounders will always have the same
 * part of speech.
 *
 * @author Keith Stevens
 */
public class PsudoWordSelector {

    private static boolean usePos;

    public static void usage(ArgOptions options) {
        System.out.println(
              "Usage: PseudoWordselector [options] <word_scores> <out>\n" +
              options.prettyPrint());
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        ArgOptions options = new ArgOptions();
        options.addOption('w', "wordList",
                          "Specifies the wods that should be used in a " +
                          "pseudo word list",
                          true, "FILE", "Required (One of)");
        options.addOption('n', "numberOfPseudoWords",
                          "Specifies the desired number of pseudo words " +
                          "to create. If usePartsOfSpeech is set, this " +
                          "number of words per part of speech will be selected",
                          true, "INT", "Requred (One of)");
        options.addOption('P', "usePartsOfSpeech",
                          "If set, all terms are expected to have their part " +
                          "of speech as a suffix.  Terms should have the " +
                          "form lemma-POS",
                          false, null, "Optional");
        options.addOption('t', "typeOfPseudoWord",
                          "Specifies the specificity of the selected pseudo " +
                          "word confounders.  high will pick the word with " +
                          "the closest score.  med will select a score " +
                          "randomly from the 100 closest scoring words and " +
                          "low will select any confounder at random", 
                          true, "high|med|low", "Required");
        options.parseOptions(args);

        if ((!options.hasOption('n') && !options.hasOption('w')) ||
            !options.hasOption('t') || 
            options.numPositionalArgs() != 2)
            usage(options);

        usePos = options.hasOption('P');

        List<Map<String, Double>> wordScores = loadScores(
                options.getPositionalArg(0));
        List<Set<String>> baseWords = (options.hasOption('w'))
            ? extractWordList(options.getStringOption('w'))
            : selectWord(wordScores, options.getIntOption('n'));

        String type = options.getStringOption('t');
        PrintWriter writer = new PrintWriter(options.getPositionalArg(1));
        for (int i = 0; i < baseWords.size(); ++i) {
            Map<String, Double> scores = wordScores.get(i);
            Set<String> keyWords = baseWords.get(i);

            Map<String, String> pseudoWordMap = null;
            if (type.equals("high"))
                pseudoWordMap = selectHigh(scores, keyWords);
            else if (type.equals("med"))
                pseudoWordMap = selectMed(scores, keyWords);
            else if (type.equals("low"))
                pseudoWordMap = selectLow(scores, keyWords);
            else
                usage(options);

            for (Map.Entry<String, String> e : pseudoWordMap.entrySet())
                writer.printf("%s %s\n", e.getKey(), e.getValue());
        }
        writer.close();
    }

    public static Map<String, String> selectHigh(Map<String, Double> wordScores,
                                                 Set<String> baseWords) {
        List<String> words = new ArrayList<String>(wordScores.keySet());
        Map<String, Integer> wordIndices = new HashMap<String, Integer>();
        for (String word : words)
            wordIndices.put(word, wordIndices.size());

        Map<String, String> pseudoWordMap = new HashMap<String, String>();
        Random rand = new Random();
        for (String word : baseWords) {
            int index = rand.nextInt(2);
            if (index == 0)
                index--;

            addWord(word, index, words, wordIndices, pseudoWordMap);
        }
        return pseudoWordMap;
    }

    public static void addWord(String word,
                               int index,
                               List<String> words,
                               Map<String, Integer> wordIndices,
                               Map<String, String> pseudoWordMap) {
        Integer baseWordIndex = wordIndices.get(word);
        if (baseWordIndex == null)
            return;
        System.out.printf("Skipped: %s\n", word);

        index = baseWordIndex + index;

        String confounder = words.get(index);
        String conflation = word + confounder;
        pseudoWordMap.put(word, conflation);
        pseudoWordMap.put(confounder, conflation);
    }


    public static Map<String, String> selectMed(Map<String, Double> wordScores,
                                                Set<String> baseWords) {
        List<String> words = new ArrayList<String>(wordScores.keySet());
        Map<String, Integer> wordIndices = new HashMap<String, Integer>();
        for (String word : words)
            wordIndices.put(word, wordIndices.size());

        Map<String, String> pseudoWordMap = new HashMap<String, String>();
        Random rand = new Random();
        for (String word : baseWords) {
            int index = rand.nextInt(2*100) - 100;
            if (index == 0)
                index++;

            int baseWordIndex = wordIndices.get(word);
            index = baseWordIndex + index;
            String confounder = words.get(index);
            String conflation = word + confounder;
            pseudoWordMap.put(word, conflation);
            pseudoWordMap.put(confounder, conflation);
        }

        return pseudoWordMap;
    }

    public static Map<String, String> selectLow(Map<String, Double> wordScores,
                                                Set<String> baseWords) {
        Set<String> words = new HashSet<String>(wordScores.keySet());
        words.removeAll(baseWords);
        List<String> shuffleWords = new ArrayList<String>(words);
        Collections.shuffle(shuffleWords);
        Iterator<String> confounders = shuffleWords.iterator();
        Map<String, String> pseudoWordMap = new HashMap<String, String>();
        for (String baseWord : baseWords) {
            String confounder = confounders.next();
            String conflation = baseWord+confounder;
            pseudoWordMap.put(baseWord, conflation);
            pseudoWordMap.put(confounder, conflation);
        }
        return pseudoWordMap;
    }

    public static List<Map<String, Double>> loadScores(String scoreFile)
            throws Exception{
        List<Map<String, Double>> wordScores =
            new ArrayList<Map<String, Double>>();
        int limit = (usePos) ? 3 : 1;
        for (int i = 0; i < limit; ++i)
            wordScores.add(new LinkedHashMap<String, Double>());

        BufferedReader br = new BufferedReader(new FileReader(scoreFile));
        for (String line = null; (line = br.readLine()) != null; ) {
            String[] tokens = line.split("\\s+");
            int index = 0;
            if (usePos) {
                String[] termPos = tokens[0].split("-");
                if (termPos[1].startsWith("N"))
                    index = 0;
                else if (termPos[1].startsWith("V"))
                    index = 1;
                else if (termPos[1].startsWith("J"))
                    index = 2;
                tokens[0] = termPos[0];
            }
            wordScores.get(index).put(tokens[0], Double.parseDouble(tokens[1]));
        }
        return wordScores;
    }

    public static List<Set<String>> extractWordList(String wordListFile)
            throws Exception{
        List<Set<String>> baseWords = new ArrayList<Set<String>>();
        int limit = (usePos) ? 3 : 1;
        for (int i = 0; i < limit; ++i)
            baseWords.add(new HashSet<String>());

        BufferedReader br = new BufferedReader(new FileReader(wordListFile));
        for (String line = null; (line = br.readLine()) != null; ) {
            if (usePos) {
                String[] termPos = line.trim().split("-");
                if (termPos[1].startsWith("N"))
                    baseWords.get(0).add(termPos[0]);
                else if (termPos[1].startsWith("V"))
                    baseWords.get(1).add(termPos[0]);
                else if (termPos[1].startsWith("J"))
                    baseWords.get(2).add(termPos[0]);
            } else {
                baseWords.get(0).add(line.trim());
            }
        }
        return baseWords;
    }

    public static List<Set<String>> selectWord(
            List<Map<String, Double>> wordScores,
            int numWords) {
        List<Set<String>> baseWords = new ArrayList<Set<String>>();
        for (Map<String, Double> scores : wordScores) {
            List<String> words = new ArrayList<String>(scores.keySet());
            Collections.shuffle(words);
            Set<String> keyWords = new HashSet<String>();
            for (int i = 0; i < numWords; ++i)
                keyWords.add(words.get(i));
            baseWords.add(keyWords);
        }
        return baseWords;
    }
}
