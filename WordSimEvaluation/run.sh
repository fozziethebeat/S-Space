#!/bin/bash

baseDir=$HOME/devel/S-Space/WordSimEvaluation/
stopWordFile=$HOME/devel/S-Space/data/english-stop-words-large.txt 

wordSimFile=$baseDir/wordSim65pairs.tab
wordSimKeyWordsFile=$baseDir/wordsim.keywords.txt
wackyFile=$baseDir/ukwac.sample.xml
oneDocFile=$baseDir/ukwac.sample.one-doc.txt
topWordFile=$baseDir/top.50k.words.ukwac.txt
contextDir=$baseDir/contexts/ukwac
contextFilePrefix=ukwac-contexts-

numTopWords=50000
windowSize=20
clusterAlgsList="hac eigen kmeans"
consensusAlgList="boem agglo bok"
numClustersList="1" #5 10 15 20"
simFunc=cosine
matchFunc=avg

jar="target/wordsim-experiment-1.0-jar-with-dependencies.jar"
run="scala -J-Xmx4g -cp $jar"
base="edu.ucla.sspace.experiment"

# Extract the key words that we will need to represent in our model.
echo "Extracting the key words to represent from $wordSimFile"
#$run $base.ExtractWordSimKeyWords $wordSimFile > $wordSimKeyWordsFile 

# Convert the ukwac or wackypedia corpora into a one doc per line format for
# easier use later on.
echo "Extracting the documents from $wackyFile"
#$run $base.ExtractParsedWackyCopora $wackyFile > $oneDocFile

# Extract the top N words from all contexts excluding stop words
echo "Computing the top $numTopWords words in the corpus"
#cat $oneDocFile | tr -s " " "\n" | sort | uniq -c > $topWordFile.tmp
#$run $base.SelectTopWords $stopWordFile $numTopWords $topWordFile.tmp $numTopWords > $topWordFile
#rm $topWordFile.tmp

# Extract contexts for each of the words in the word similarity test we are
# considerinng.
echo "Extracting n word sized contexts for each key word from $oneDocFile"
mkdir -p $contextDir
#$run $base.ExtractWordSimContexts $wordSimKeyWordsFile $topWordFile $windowSize \
#                                  $oneDocFile $contextDir/$contextFilePrefix

# Convert each corpus file into a sparse matrix recording the
# co-occurring content words.
echo "Conveting each word's contexts into vectors using a shared feature space"
for txt in $contextDir/*.txt; do
    newName=`echo $txt | sed "s/txt$/sparse_vector.mat/"`
    #$run $base.ExtractWordsiContexts $topWordFile $txt $newName
done

# Cluster each set of contexts for a multi-sense word using each of the
# specified clustering algorithms crossed with the specified number of possible
# clusterings.  Note: This Should Be Parallelized with Scream.
for mat in $contextDir/*.mat; do
    for m in $clusterAlgsList; do
        for k in $numClustersList; do
            echo "Clustering $mat with algorithm $m and $k clusters"
            newName=`echo $mat | sed "s/sparse_vector.mat/$m.$k.partition/"`
            #$run $base.ClusterContexts $m $k $mat $newName
        done
    done
done

# Iterate over every word and construct multiple consensus functions over the
# component solutions computed above.
for txt in $contextDir/*.txt; do
    partitionBase=`echo $txt | sed "s/\.txt$//"`
    for k in $numClustersList; do
        inputPartitions=`for c in $clusterAlgsList; do echo $partitionBase.$c.$k.partition; done | tr "\n" " "`
        for m in $consensusAlgList; do
            echo "Forming Consensus Partition for $partitionBase using method $m and $k clusters"
            #$run $base.ComposeConsensusSolution $k $m \
            #    $partitionBase.$m.$k.partition \
            #    $inputPartitions
        done
    done
done

# Iterate over all of the partitions and form them into prototype vectors for
# easy comparison in semantic similarity tests
for partition in $contextDir/*.partition; do
    mat=`echo $partition | sed "s/[a-z]\+\.[0-9]\+\.partition$/sparse_vector.mat/"`
    prototypeMat=`echo $partition | sed "s/partition/prototype/"`
    echo "Forming Prototypes for $partition and data matrix $mat"
    #$run $base.FormPrototypes $partition $mat $prototypeMat
done

for m in $clusterAlgsList $consensusAlgList; do
    for k in $numClustersList; do
        tail -n +2 $wordSimFile | grep -v "^#" | while read pairLine; do
            w1=`echo $pairLine | tr -s " " "\t" | cut -f 1`
            w2=`echo $pairLine | tr -s " " "\t" | cut -f 2`
            goldScore=`echo $pairLine | tr -s " " "\t" | cut -f 3`
            computedScore=`$run $base.ComparePrototypes \
                $simFunc $matchFunc \
                $contextDir/${contextFilePrefix}${w1}.$m.$k.prototype \
                $contextDir/${contextFilePrefix}${w2}.$m.$k.prototype`
            echo "$m $k $w1 $w2 $goldScore $computedScore"
        done
    done
done
