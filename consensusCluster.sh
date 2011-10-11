#!/bin/bash

DFS_HOME=/data/semEval2010/train/wordsi25Context/
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
    hadoop dfs -rm $2/$1
    hadoop dfs -copyFromLocal $1 $2/$1
    while [ "`localSize $1`" != "`hdfsSize $2/$1`" ]; do sleep 1; done
}

function run() {
    scala -J-Xmx4g -cp sspace-wordsi.jar ConsensusCluster.scala  \
          $CLUST.$1 $inputMat 20 40 .80 $inputMat.$2 >&2
    # If any of the dat files are missing, exit pre-emptively
    [ -f $inputMat.$2.delta.dat ] || return
    [ -f $inputMat.$2.cdf.dat ] || return
    [ -f $inputMat.$2.gap.dat ] || return
    copyFromLocal $inputMat.$2.delta.dat $DFS_HOME
    copyFromLocal $inputMat.$2.cdf.dat $DFS_HOME
    copyFromLocal $inputMat.$2.gap.dat $DFS_HOME
}

while read inputMat
do
    copyToLocal $inputMat $DFS_HOME

    #run CKVWSpectralClustering03 sc03
    run CKVWSpectralClustering06 sc06
    run DirectClustering kmeans
    #run BisectingKMeans bi-kmeans
    #run HierarchicalAgglomerativeClustering hac
done
