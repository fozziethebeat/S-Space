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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import edu.ucla.sspace.text.Corpus;
import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.PassThroughTokenProcesser;
import edu.ucla.sspace.text.Sentence;
import edu.ucla.sspace.text.SimpleDocument;
import edu.ucla.sspace.text.Token;
import edu.ucla.sspace.text.TokenProcesser;

import edu.stanford.nlp.ling.CoreLabel;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;


/**
 *
 */
public class CoreNlpProcessedCorpus implements Corpus {

    private static final String DEFAULT_CORENLP_ANNOTATORS = "tokenize, ssplit";
    
    private final Iterable<String> documents;

    private final CoreMap corpusAnnotations;

    private final String coreNlpAnnotators;

    private TokenProcesser processer;
    
    
    public CoreNlpProcessedCorpus(Iterable<String> documents) {
        this(documents, new PassThroughTokenProcesser(),
             DEFAULT_CORENLP_ANNOTATORS);
    }

    public CoreNlpProcessedCorpus(Iterable<String> documents,
                                  String coreNlpAnnotators) {
        this(documents, new PassThroughTokenProcesser(), coreNlpAnnotators);
    }

    public CoreNlpProcessedCorpus(Iterable<String> documents,
                                  TokenProcesser processer) {
        this(documents, processer, DEFAULT_CORENLP_ANNOTATORS);
    }

    public CoreNlpProcessedCorpus(Iterable<String> documents,
                                  TokenProcesser processer,
                                  String coreNlpAnnotators) {
        this.documents = documents;
        this.processer = processer;
        this.corpusAnnotations = new ArrayCoreMap();
        this.coreNlpAnnotators = coreNlpAnnotators;
    }

    
    /**
     * {@inheritDoc}
     */
    public CoreMap annotations() {
        return corpusAnnotations;
    }
    
    /**
     * {@inheritDoc}
     */
    public TokenProcesser getTokenProcesser() {
        return processer;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Document> iterator() {
        return new DocIterator();
    }

    /**
     * {@inheritDoc}
     */
    public void setTokenProcesser(TokenProcesser processer) {
        this.processer = processer;
    }
    
    private class DocIterator implements Iterator<Document> {

        private final Iterator<String> iter;

        public DocIterator() {
            iter = documents.iterator();
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public Document next() {
            String doc = iter.next();
            StanfordCoreNLP pipeline = CoreNlpUtils.pipeline(coreNlpAnnotators);
            Annotation document = new Annotation(doc);
            pipeline.annotate(document);
            List<CoreMap> sentences = document.get(SentencesAnnotation.class);

            List<Sentence> sents = new ArrayList<Sentence>(sentences.size());

            for (CoreMap sent : sentences)
                sents.add(new StanfordSentence(sent));
            
            return new SimpleDocument(sents);
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }        
        
    }

    static class StanfordSentence implements Sentence {

        final CoreMap sent;

        List<CoreLabel> tokens; // computed as needed
        
        public StanfordSentence(CoreMap sent) {
            this.sent = sent;
            this.tokens = null;
        }

        public CoreMap annotations() {
            return sent;
        }
        
        public Iterator<Token> iterator() {
            if (tokens == null) {
                
                Annotation document = new Annotation(text());
                CoreNlpUtils.pipeline("tokenize, ssplit, pos, lemma")
                    .annotate(document);
                List<CoreMap> sentences = document.get(SentencesAnnotation.class);
                tokens = sentences.get(0).get(TokensAnnotation.class);
            }

            return new TokensIterator(tokens.iterator());
        }
        
        public String text() {
            return sent.get(TextAnnotation.class);
        }
    }
    
    static class TokensIterator implements Iterator<Token> {

        private final Iterator<CoreLabel> tokens;

        public TokensIterator(Iterator<CoreLabel> tokens) {
            this.tokens = tokens;
        }

        public boolean hasNext() {
            return tokens.hasNext();
        }
        
        public Token next() {
            return new StanfordToken(tokens.next());
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
    static class StanfordToken implements Token {

        private CoreLabel token;

        public StanfordToken(CoreLabel token) {
            this.token = token;
        }
        
        public CoreMap annotations() {
            return token;
        }
    
        public String text() {
            return token.get(TextAnnotation.class);
        }
        
    }
    
}
