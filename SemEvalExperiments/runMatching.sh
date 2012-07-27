#!/bin/bash

source ./config.sh

case $1 in
    "labeling") testType=train 
                evalType=test
                solutionType=labelingSolutions
                evalKey=$testKey
                ;;
    "matching") testType=full
                evalType=full
                solutionType=matchSolutions
                evalKey=$fullKey
                ;;
    "zellig")   testType=train
                evalType=test
                solutionType=zelligSolutions
                evalKey=$testKey
                ;;
esac

function clusterAllContexts() {
# Cluster each group of contexts for each feature  space crossed with each
# matching clutering algorithm
cat $wordAndSenseList | while read wordAndSenseCount; do
    word=`echo $wordAndSenseCount | cut -d " " -f 1`
    numSenses=`echo $wordAndSenseCount | cut -d " " -f 2`
    for f in woc morph pos dep sndord; do
        for m in hac kmeans eigen; do
            echo "Clustering $word with $numSenses using features $f and model $m"
            $run $base.ClusterFeatureVectors $m $numSenses \
                 $semEval07Dir/$corpusLabel.$word.$f.$testType.sparse_vector.mat \
                 $semEval07Dir/$corpusLabel.$word.$f.$m.opt.$testType.partition 
        done
    done
    for f in graph; do
        for m in ghac spec cw prc; do
            echo "Clustering $word with $numSenses using features $f and model $m"
            $run $base.ClusterGraphModel $m $numSenses \
                 $semEval07Dir/$corpusLabel.$word.$f.$testType.mat \
                 $semEval07Dir/$corpusLabel.$word.$f.$m.opt.$testType.partition 
        done
    done
done
}
clusterAllContexts

function combinePartitions() {
cat $wordAndSenseList | while read wordAndSenseCount; do
    word=`echo $wordAndSenseCount | cut -d " " -f 1`
    numSenses=`echo $wordAndSenseCount | cut -d " " -f 2`
    for f in woc morph pos dep sndord; do
        for m in bok agglo boem; do
            inputPartitions=`for c in hac eigen kmeans; do echo $semEval07Dir/$corpusLabel.$word.$f.$c.opt.$testType.partition; done | tr "\n" " "`
            echo "Combining partitions for $word with $numSenses using features $f and model $m"
            $run $base.CombinePartitions $numSenses $m \
                 $semEval07Dir/$corpusLabel.$word.$f.$m.opt.$testType.partition \
                 $inputPartitions
        done
    done

    for f in graph; do
        for m in bok agglo boem; do
            inputPartitions=`for c in ghac spec prc cw; do echo $semEval07Dir/$corpusLabel.$word.$f.$c.opt.$testType.partition; done | tr "\n" " "`
            echo "Combining partitions for $word with $numSenses using features $f and model $m"
            $run $base.CombinePartitions $numSenses $m \
                 $semEval07Dir/$corpusLabel.$word.$f.$m.opt.$testType.partition \
                 $inputPartitions 
        done
    done
done
}
combinePartitions

mkdir -p $baseDir/$solutionType
if [ "$1" != "zellig" ]; then
    function convertGraphSolution() {
    cat $wordAndSenseList | while read wordAndSenseCount; do
        word=`echo $wordAndSenseCount | cut -d " " -f 1`
        numSenses=`echo $wordAndSenseCount | cut -d " " -f 2`
        for f in graph; do
            for m in ghac spec prc cw agglo boem bok; do
                $run $base.ConvertGraphSolution \
                     $semEval07Dir/$corpusLabel.$word.$f.$m.opt.$testType.partition \
                     $semEval07Dir/$corpusLabel.$word.$f-final.$m.opt.$evalType.partition \
                     $semEval07Dir/$corpusLabel.$word.$f.$testType.basis \
                     $semEval07Dir/$corpusLabel.$word.conll
            done
        done
    done
    }
    convertGraphSolution

    if [ "$1" == "labeling" ]; then
    cat $wordAndSenseList | while read wordAndSenseCount; do
        word=`echo $wordAndSenseCount | cut -d " " -f 1`
        numSenses=`echo $wordAndSenseCount | cut -d " " -f 2`
        for f in woc pos morph dep sndord; do
            for m in hac kmeans eigen agglo boem bok; do
                $run $base.LabelFeatureVectors \
                     $semEval07Dir/$corpusLabel.$word.$f.$m.opt.$testType.partition \
                     $semEval07Dir/$corpusLabel.$word.$testType.sparse_vector.mat \
                     $semEval07Dir/$corpusLabel.$word.$evalType.sparse_vector.mat \
                     $semEval07Dir/$corpusLabel.$word.$f.$m.opt.$evalType.partition 
            done
        done
    done
    fi

    function extractSolution() {
    feature=$1
    model=$2
    cat $wordAndSenseList | while read wordAndSenseCount; do
        word=`echo $wordAndSenseCount | cut -d " " -f 1`
        numSenses=`echo $wordAndSenseCount | cut -d " " -f 2`
        $run $base.LabelPartition $word \
             $semEval07Dir/$corpusLabel.$word.$feature.$model.opt.$evalType.partition \
             $semEval07Dir/$corpusLabel.$word.headers
    done
    }

    for f in woc pos morph dep sndord; do
        for m in hac eigen kmeans bok agglo boem; do
            echo "Extracting semeval 07 labeling for feature $f with model $m"
            extractSolution $f $m > $baseDir/$solutionType/$f.$m.key
        done
    done

    for f in graph-final; do
        for m in ghac prc cw spec bok agglo boem; do
            echo "Extracting semeval 07 labeling for feature $f with model $m"
            extractSolution $f $m > $baseDir/$solutionType/$f.$m.key
        done
    done

    function scoreModels() {
    echo Feature Model Metric Score
    for key in $baseDir/$solutionType/*.key; do
        feature=`echo $key | sed "s/.*$solutionType\///" | cut -d "." -f 1`
        model=`echo $key | cut -d "." -f 2`
        for t in ami vmeasure fscore; do
            score=`$run $base.ScoreSemEval $t $key $evalKey all`
            echo $feature $model $t $score
        done
    done
    }
    scoreModels > $baseDir/ensemble.$1.scores.dat
else
    cat $wordAndSenseList | while read wordAndSenseCount; do
        word=`echo $wordAndSenseCount | cut -d " " -f 1`
        numSenses=`echo $wordAndSenseCount | cut -d " " -f 2`
        for f in woc pos morph sndord; do
            for m in hac kmeans eigen agglo boem bok; do
                echo "Forming prototypes for $word using $f and $m"
                continue
                $run $base.FormPrototypes \
                     $semEval07Dir/$corpusLabel.$word.$f.$m.opt.$testType.partition \
                     $semEval07Dir/$corpusLabel.$word.$f.$testType.sparse_vector.mat \
                     $semEval07Dir/$corpusLabel.$word.$f.$m.opt.$testType.sparse_vector.prototypes
            done
        done
    done

    echo "Word Feature Model Score" > $baseDir/$solutionType/zellig.test.dat
    cat $wordAndSenseList | while read wordAndSenseCount; do
        word=`echo $wordAndSenseCount | cut -d " " -f 1`
        numSenses=`echo $wordAndSenseCount | cut -d " " -f 2`
        for f in woc pos morph sndord; do
            for m in hac kmeans eigen agglo boem bok; do
                $run $base.ConfounderTest \
                     $semEval07Dir/$corpusLabel.$word.test.key \
                     $semEval07Dir/$corpusLabel.$word.$f.$evalType.sparse_vector.mat \
                     $semEval07Dir/$corpusLabel.$word.headers \
                     $semEval07Dir/$corpusLabel.$word.$f.$m.opt.$testType.sparse_vector.prototypes \
                     "$word $f $m Test"
            done
        done
    done >> $baseDir/$solutionType/zellig.test.dat
fi
