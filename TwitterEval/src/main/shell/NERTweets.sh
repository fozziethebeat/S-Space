#!/bin/bash

# First, Download stanford NER code

# Extract the text for just the tweets.
tail -n +2 $1 > $1.text.tmp

# Now run a NER system over the tweets such that each one is a valid xml line
# with xml tags surrounding the entire named entity.
echo "Time Tweet" > $2
java -Xmx8g -cp lib/stanford-ner.jar edu.stanford.nlp.ie.crf.CRFClassifier \
     -loadClassifier data/english.all.3class.distsim.crf.ser.gz  \
     -textFile $1.text.tmp \
     -outputFormat inlineXML | \
     while read line; do 
         ts=`echo $line | cut -d " " -f 1`
         text=`echo $line | cut -d " " -f 1 --complement`
         echo $ts "<sentence>$text</sentence>"
     done >> $2

rm $1.text.tmp
