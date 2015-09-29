/*
 * Copyright 2015 David Jurgens
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

package edu.ucla.sspace.corenlp;

import java.util.Properties;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class CoreNlpUtils {

    private static final ConcurrentMap<String,ThreadLocal<StanfordCoreNLP>> PIPELINES =
        new ConcurrentHashMap<String,ThreadLocal<StanfordCoreNLP>>();

    public static StanfordCoreNLP pipeline(String annotators) {
        ThreadLocal<StanfordCoreNLP> tl =
            PIPELINES.computeIfAbsent(annotators, k -> new ThreadLocal<StanfordCoreNLP>());
        StanfordCoreNLP pipeline = tl.get();
        if (pipeline == null) {
            Properties props = new Properties();
            props.setProperty("annotators", annotators);
            // This property avoids the "WARNING: untokenizable" message being
            // printed frequently
            props.setProperty("tokenize.options", "untokenizable=noneDelete");
            pipeline = new StanfordCoreNLP(props);
            tl.set(pipeline);
        }
        return pipeline;
    }

    public static void unloadPipeline(String annotators) {
        PIPELINES.remove(annotators);
    }
    
    /*
    // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
    
    // read some text in the text variable
    String text = ... // Add your text here!
    
    // create an empty Annotation just with the given text
    Annotation document = new Annotation(text);
    
    // run all Annotators on this text
    pipeline.annotate(document);
    
    // these are all the sentences in this document
    // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
    
    for(CoreMap sentence: sentences) {
      // traversing the words in the current sentence
      // a CoreLabel is a CoreMap with additional token-specific methods
      for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
        // this is the text of the token
        String word = token.get(TextAnnotation.class);
        // this is the POS tag of the token
        String pos = token.get(PartOfSpeechAnnotation.class);
        // this is the NER label of the token
        String ne = token.get(NamedEntityTagAnnotation.class);       
      }
    }
    */    
}
