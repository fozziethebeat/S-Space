#!/bin/bash

RESULT_HOME=/home/stevens35/sem07_cc_result
DATA_HOME=/data/senseEval2007/wordsi25
CLUST=edu.ucla.sspace.clustering

# Remove "s from the environment variable to work around a stupid bug in hadoop.
export HADOOP_CLIENT_OPTS=`echo $HADOOP_CLIENT_OPTS | tr -d '"'`

function localSize() {
   ls -l $1 | awk '{ print $5 }'
}

function hdfsSize() {
   s=`hadoop dfs -ls $1 | awk '{ print $5 }'`
   echo ${s:1}
}

function copyToLocal() {
    hadoop dfs -copyToLocal $2/$1 $1
    while [ ! -f $1 ]; do sleep 1; done
}

function copyFromLocal() {
    [ -f $1 ] || return
    hadoop dfs -rm $2/$1
    hadoop dfs -copyFromLocal $1 $2/$1
    while [ "`localSize $1`" != "`hdfsSize $2/$1`" ]; do sleep 1; done
}

function run() {
    echo "Starting $2"
    scala -J-Xmx4g -cp sspace-wordsi.jar ConsensusCluster.scala  \
          $CLUST.$1 $inputMat 20 250 .80 $inputMat.$2 
    copyFromLocal $inputMat.$2.delta.dat $RESULT_HOME
    copyFromLocal $inputMat.$2.cdf.dat $RESULT_HOME
    copyFromLocal $inputMat.$2.gap.dat $RESULT_HOME
    for i in $(seq -w 2 20); do
        copyFromLocal $inputMat.$2.cm$i $RESULT_HOME
        copyFromLocal $inputMat.$2.rca$i $RESULT_HOME
    done
}

while read inputMat
do
    copyToLocal $inputMat $DATA_HOME
    echo clustering $inputMat

    #run CKVWSpectralClustering03 sc03
    run CKVWSpectralClustering06 sc06
    run DirectClustering kmeans
    #run BisectingKMeans bi-kmeans
    run HierarchicalAgglomerativeClustering hac
done >&2
