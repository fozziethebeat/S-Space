#!/bin/bash

baseDir=$HOME/devel/S-Space/WordSimEvaluation/data
stopWordFile=$HOME/devel/S-Space/data/english-stop-words-large.txt 
topWordFile=$baseDir/top.50k.words.wiki.txt

wordSimDir="$baseDir/evals"
wordSimFiles="$wordSimDir/wordSim65pairs.tab $wordSimDir/Mtruk.csv $wordSimDir/wordsim_relatedness_goldstandard.txt $wordSimDir/wordsim_similarity_goldstandard.txt" 
wordSimFixedFiles="$wordSimDir/wordSim65pairs.fixed.txt $wordSimDir/Mtruk.fixed.txt $wordSimDir/wordsim_relatedness_goldstandard.fixed.txt $wordSimDir/wordsim_similarity_goldstandard.fixed.txt" 
wordSimFile=$baseDir/evals/wordSim65pairs.tab
wordSimKeyWordsFile=$baseDir/wordsim.keywords.txt
#wordSimKeyWordsFile=$baseDir/capitalized.keywords.txt
wordSimComparisonFile=$baseDir/wordsim.induced.comparison.dat
hfs=/hdfs/data/corpora
wackyFiles="$hfs/wackypedia/wikipedia-1.xml $hfs/wackypedia/wikipedia-2.xml $hfs/wackypedia/wikipedia-3.xml $hfs/wackypedia/wikipedia-4.xml"
oneDocFile=$hfs/wackypedia/wiki.full.one-doc.txt
contextDir=$hfs/contexts/wiki
contextFilePrefix=wiki-contexts-
hdfsOutputDir=/user/stevens35/wordsim-contexts
hdfsInputDir=/data/wackypedia/wiki-contexts
oneDocPerLineFile=$hfs/nyt/nyt03-one-doc-per-line.txt

numTopWords=50000
windowSize=20
#clusterAlgsList="eigen kmeans" #hac-avg hac-single hac-complete hac-median eigen kmeans"
clusterAlgsList="hac-avg hac-single hac-complete hac-median eigen kmeans"
consensusAlgList="boem agglo bok"
numClustersList="5 10 15 20"
simFunc=cosine
matchFunc=avg

jar="target/wordsim-experiment-1.0-jar-with-dependencies.jar"
run="scala -J-Xmx4g -cp $jar"
base="edu.ucla.sspace.experiment"
