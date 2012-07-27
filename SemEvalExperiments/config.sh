#!/bin/bash

# Set the directory locations for data files and for new generated files.
# Set the base directory for most of the generated data files
baseDir=results
# Set the directory for the semeval data
semEval07Dir=$baseDir/SemEval/2007
# Set the corpus name.
corpusLabel=semeval07
# Set the name of the bigram matrix for the 2007 dataset
bigramMatrix=$baseDir/$corpusLabel.bigram.svdlibc-sparse.mat
bigramBasis=$baseDir/$corpusLabel.bigram.basis
bigramTrainMatrix=$baseDir/$corpusLabel.bigram.train.svdlibc-sparse.mat
bigramTrainBasis=$baseDir/$corpusLabel.bigram.train.basis

# Resources needed for extracting all of the contexts.  

# The stop words list will be used to completely reject features regarldess of
# frequency or other statistical values.
stopWords=$HOME/devel/S-Space/data/english-stop-words-large.txt
# The morphological analyzer allows for more a advanced stemming-like process.
analyzer=data/apertium-en.master.bin
# The wiki word counts allows for the collocation graphs to be pruned with
# respect to an external resource.
wikiWordCounts=data/wackyLikelihood.txt

wordAndSenseList=$baseDir/$corpusLabel.sense-count.txt
testKey=$semEval07Dir/key/keys/senseinduction_test.key
trainKey=$semEval07Dir/key/keys/senseinduction_train.key
fullKey=$semEval07Dir/key/keys/senseinduction.key

# Assignments useful for running jobs.
jar="target/semeval-experiment-1.0-jar-with-dependencies.jar"
run="scala -J-Xmx2g -cp $jar"
base="edu.ucla.sspace.experiment"
