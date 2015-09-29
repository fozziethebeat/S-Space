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

package edu.ucla.sspace.word2vec;


import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.corenlp.CoreNlpUtils;

import edu.ucla.sspace.text.Corpus;
import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.PassThroughTokenProcesser;
import edu.ucla.sspace.text.Sentence;
import edu.ucla.sspace.text.SimpleToken;
import edu.ucla.sspace.text.TokenProcesser;

import edu.ucla.sspace.util.Counter;
import edu.ucla.sspace.util.LoggerUtil;
import edu.ucla.sspace.util.ObjectCounter;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vectors;

import edu.stanford.nlp.ling.CoreLabel;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import edu.stanford.nlp.util.CoreMap;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;

import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;

import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

import org.deeplearning4j.models.word2vec.wordstore.VocabCache;

import org.deeplearning4j.models.word2vec.VocabWord;


/**
 */
public class Word2Vec implements SemanticSpace, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private org.deeplearning4j.models.word2vec.Word2Vec.Builder word2vecBuilder;

    private org.deeplearning4j.models.word2vec.Word2Vec word2vec;
    
    private int vectorLength;

    private TokenProcesser processer;
    
    /**
     *
     */
    public Word2Vec() {
        word2vecBuilder =
            new org.deeplearning4j.models.word2vec.Word2Vec.Builder();
        vectorLength = 0;
        processer = new PassThroughTokenProcesser();
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return "word2vec";
    }

    /**
     * {@inheritDoc}
     */
    public TokenProcesser getTokenProcessor() {
        return processer;
    }
    
    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return vectorLength;
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getVector(String word) {
        return (word2vec.hasWord(word))
            ? Vectors.asVector(word2vec.getWordVector(word))
            : null;
    }
    
    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        if (word2vec == null)
            throw new IllegalStateException("Model has not been built!");
        return new Vocab(word2vec.vocab());
    }
    
    /**
     * {@inheritDoc}
     */
    public void process(final Corpus corpus) {
        SentenceIterator iter = new CorpusSentenceIterator(corpus);
        TokenizerFactory fac = new CustomTokenizerFactory();
        word2vecBuilder = word2vecBuilder
            .iterate(iter)
            .tokenizerFactory(fac);
    }

    public void setTokenProcessor(TokenProcesser processer) {
        this.processer = processer;
    }
    
    /**
     * {@inheritDoc}
     *
     * @param properties {@inheritDoc} See this class's {@link
     *        LatentSemanticAnalysis javadoc} for the full list of supported
     *        properties.
     */
    public void build(Properties properties) {
        
    }
    
    public void build(int batchSize,
                      double sampling,
                      int minWordFrequency,
                      boolean useAdaGrad,
                      int layerSize,
                      int iterations,
                      double learningRate,
                      double minLearningRate,
                      int negativeSample) {

        vectorLength = layerSize;

        try {
            word2vec =
                word2vecBuilder.iterations(iterations)
                .batchSize(batchSize) //# words per minibatch. 
                .sampling(sampling) // negative sampling. drops words out
                .minWordFrequency(minWordFrequency) // 
                .useAdaGrad(useAdaGrad) //
                .layerSize(layerSize) // word feature vector size
                .iterations(iterations) // # iterations to train
                .learningRate(learningRate) // 
                .minLearningRate(minLearningRate) // learning rate decays wrt # words. floor learning
                .negativeSample(negativeSample) // sample size 10 words
                .build();
            
            word2vec.fit();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    class Vocab extends AbstractSet<String> {

        private final VocabCache vocab;

        public Vocab(VocabCache vocab) {
            this.vocab = vocab;
        }

        public boolean contains(Object o) {
            return o instanceof String
                && vocab.containsWord((String)o);
        }
        
        public Iterator<String> iterator() {
            return vocab.words().iterator();
        }
        
        /**
         * {@inheritDoc}
         */
        public int size() {
            return vocab.numWords();
        }
    }

    class CorpusSentenceIterator implements SentenceIterator {

        private final Corpus corpus;

        private Iterator<Document> docIter;

        private Iterator<Sentence> sentIter;
        
        private String next;
        
        public CorpusSentenceIterator(Corpus corpus) {
            this.corpus = corpus;
            reset();
        }
        
        private void advance() {
            // Advance until we find another sentence
            while ((sentIter != null && !sentIter.hasNext()) && docIter.hasNext()) {
                Document doc = docIter.next();
                sentIter = doc.iterator();
            }
            
            if (sentIter != null && sentIter.hasNext()) {
                Sentence sent = sentIter.next();
                next = sent.text();
            }
            else {
                next = null;
            }
        }
        
        public void finish() {
            // No-op
        }

        public SentencePreProcessor getPreProcessor() {
            return null;
        }

        public boolean hasNext() {
            return next != null;
        }

        public String nextSentence() {
            if (next == null)
                throw new NoSuchElementException("no more sentences");
            String n = next;
            advance();
            return n;
        }

        public void reset() {
            docIter = corpus.iterator();
            sentIter = (docIter.hasNext())
                ? docIter.next().iterator()
                : null;
            next = null;
            advance();
        }

        public void setPreProcessor(SentencePreProcessor preProcessor) {
            // No-op
        }
    }

    class CustomTokenizerFactory implements TokenizerFactory {

        public CustomTokenizerFactory() { }

        public Tokenizer create(java.io.InputStream toTokenize) {
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader br =
                    new BufferedReader(new InputStreamReader(toTokenize));
                for (String line = null; (line = br.readLine()) != null; ) {
                    sb.append(line).append('\n');
                }
                return new CustomTokenizer(sb.toString());
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }

        public Tokenizer create(String toTokenize) {
            return new CustomTokenizer(toTokenize);
        }

        public void setTokenPreProcessor(TokenPreProcess preProcessor) {
            // no-op
        }
    }

    class CustomTokenizer implements Tokenizer {
        
        private static final String ANNOTATORS = "tokenize, ssplit, pos, lemma";
        
        private List<CoreLabel> tokens;

        private int curIndex = 0;
        
        public CustomTokenizer(String sentence) {

            Annotation document = new Annotation(sentence);
            CoreNlpUtils.pipeline(ANNOTATORS).annotate(document);

            tokens = new ArrayList<CoreLabel>();
            for (CoreMap sent : document.get(SentencesAnnotation.class)) {
                tokens.addAll(sent.get(TokensAnnotation.class));
            }
        }
        
        public int countTokens() {
            return tokens.size();
        }

        public List<String> getTokens() {
            return new AbstractList<String>() {
                public String get(int i) {
                    return getToken(i);
                }
                public int size() {
                    return tokens.size();
                }
            };
        }

        public boolean hasMoreTokens() {
            return curIndex + 1 < tokens.size();
        }

        /**
         * Transforms a {@link CoreLabel} into the appropriate {@code String}
         * representation according to the configuration
         */
        private String getToken(int i) {
            CoreLabel token = tokens.get(i);
            return processer.process(new SimpleToken(token));
        }

        public String nextToken() {
            return getToken(curIndex++);
        }

        public void setTokenPreProcessor(TokenPreProcess tokenPreProcessor) {
            // no-op
        }

    }
}
