#!/bin/bash

source ./config.sh

if [ 0 != 0 ];  then

for alg in $clusterAlgsList $consensusAlgList; do
    for k in $numClustersList; do
        hadoop fs -cat $hdfsOutputDir/$contextFilePrefix*.$alg.$k.prototype > $contextDir/wiki-space.$alg.$k.prototypes
    done
done

fi

for alg in $clusterAlgsList $consensusAlgList; do
    for k in $numClustersList; do
        echo "disambiguating with $alg and $k clusters"
        disambiguated=`echo $oneDocPerLineFile | sed "s/txt$/$alg.$k.txt/"`
        $run $base.DisambiguateCorpus $wordSimKeyWordsFile \
                                      $contextDir/wiki-space.$alg.$k.prototypes \
                                      $k \
                                      $topWordFile \
                                      $oneDocPerLineFile \
                                      $disambiguated
    done
done

exit
