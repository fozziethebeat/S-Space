#!/bin/bash

source config.sh

mkdir -p $baseDir/supervisedSolutions
for m in nb me dt c45 bag; do
    for f in woc pos dep morph sndord; do
        cat $wordAndSenseList | while read wordAndSenseCount; do
            word=`echo $wordAndSenseCount | cut -d " " -f 1`
            $run $base.SchiselClassify $m \
                 $semEval07Dir/$corpusLabel.$word.$f.test.sparse_vector.mat \
                 $semEval07Dir/$corpusLabel.$word.headers \
                 $semEval07Dir/$corpusLabel.$word.headers.labels \
                 $testKey | grep "^$word" 
        done > $baseDir/supervisedSolutions/$f.$m.key
    done 
done

function scoreModels() {
echo Feature Model Metric Score
for key in $baseDir/supervisedSolutions/*.key; do
    feature=`echo $key | sed "s/.*supervisedSolutions\///" | cut -d "." -f 1`
    model=`echo $key | cut -d "." -f 2`
    for t in ami vmeasure fscore; do
        score=`$run $base.ScoreSemEval $t $key $testKey all`
        echo $feature $model $t $score
    done
done
}
scoreModels > $baseDir/supervised.labelling.scores.dat
