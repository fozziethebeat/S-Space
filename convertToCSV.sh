#!/bin/bash
# This script will convert a transposed document by topic matrix into a csv file
# with the document's name and categories as the first two column values based
# on line input.  
#
# Input lines should be of the following format
#  modelName MODELNAME numTopics
# 1. modelName is a short name for the model that also specifies the
# directory on hdfs that will house the factor matrices.
# 2. MODELNAME is an upper case name that is used in the transpose file names.
# 3. numTopics specifies the number of reduced dimensions (topics)
#
# For example, 
# svd SVD 100
# will convert
# $BASE/svd_topic/nyt03_SVD_100-ds.dat.transpose 
# into 
# $BASE/svd_topic/nyt03_SVD_100-ds.dat.csv

# Change this to store the matrices in a different base directory
BASE=/home/stevens35

#
# Remove "s from the environment variable to work around a stupid bug in hadoop.
export HADOOP_CLIENT_OPTS=`echo $HADOOP_CLIENT_OPTS | tr -d '"'`

# A helper method to determine the local size of a file.
function localSize() {
   ls -l $1 | awk '{ print $5 }'
}

# A helper method to determine the size of a file on hdfs.
function hdfsSize() {
   s=`hadoop dfs -ls $1 | awk '{ print $5 }'`
   echo ${s:1}
}

# A helper method to copy files from hdfs to local disk.
function copyToLocal() {
    hadoop dfs -copyToLocal $2/$1 $1
    while [ ! -f $1 ]; do sleep 1; done
}
# A helper method to copy files from local disk to hdfs.
function copyFromLocal() {
    hadoop dfs -rm $2/$1
    hadoop dfs -copyFromLocal $1 $2/$1
    while [ "`localSize $1`" != "`hdfsSize $2/$1`" ]; do sleep 1; done
}

while read line
do
    model=`echo $line | cut -d " " -f 1`
    MODEL=`echo $line | cut -d " " -f 2`
    numTopics=`echo $line | cut -d " " -f 3`

    ds=nyt03_${MODEL}_$numTopics-ds.dat
    tsp=$ds.transpose
    csv=$ds.csv

    copyToLocal $tsp $BASE/${model}_topic

    python convertToCSV.py nyt03DocSections.txt nyt03OneLinePerDoc.txt $tsp > $csv
    copyFromLocal $csv $BASE/${model}_topic
done >&2
