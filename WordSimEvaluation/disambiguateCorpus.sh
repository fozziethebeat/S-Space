#!/bin/bash


clusterAlgsList="hac eigen kmeans"
consensusAlgList="boem agglo bok"
numClustersList="5 10 15 20"
hdfsOutputDir=/user/stevens35/wordsim-contexts
contextFilePrefix=wiki-contexts-
wordSimKeyWordsFile=$baseDir/wordsim.keywords.txt
topWordFile=$baseDir/top.50k.words.wiki.txt
oneDocPerLineFile=something.txt

for alg in $clusterAlgsList $consensusAlgList; do
    for k in $numClustersList; do
        hadoop fs -cat $hdfsOutputDir/$contextFilePrefix*.$alg.$k.prototype > wiki-space.$alg.$k.prototypes
    done
done

for alg in $clusterAlgsList $consensusAlgList; do
    for k in $numClustersList; do
        disambiguated=`echo $oneDocPerLineFile | sed "s/txt$/$alg.$k.txt/"`
        $run $base.DisambiguateCorpus $wordSimKeyWordsFile \
                                      wiki-space.$alg.$k.prototypes \
                                      $k \
                                      $topWordFile \
                                      $oneDocPerLineFile \
                                      $disambiguated
    done
done
