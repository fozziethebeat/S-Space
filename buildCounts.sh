#!/bin/bash

function nytWords() {

hadoop dfs -cat /stats/nyt_2003/termDocOccurence/part-* \
  | scala ExtractTop10Terms.scala \
      nyt03_${1}_counts.txt \
      /hdfs/tmp/factor_eval/${1}/*.top10
}

function wikiWords() {

hadoop dfs -cat /stats/wackypedia/woc_nyt03_list/part-* \
  | scala ExtractTop10Terms.scala \
      wiki_${1}_counts.txt \
      /hdfs/tmp/factor_eval/${1}/*.top10
}

#wikiWords nmf
#nytWords nmf

wikiWords svd
nytWords svd

#wikiWords lda
#nytWords lda

#wikiWords ldahp
#nytWords ldahp

#wikiWords ldanh
#nytWords ldanh
