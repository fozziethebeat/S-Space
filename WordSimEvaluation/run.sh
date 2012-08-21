#!/bin/bash

source ./config.sh

if [ 0 != 0 ]; then
for wordSimTest in $wordSimFiles; do
    newName=`echo $wordSimTest | cut -d "." -f 1`.fixed.txt
    echo $newName
    cat $wordSimTest | grep -v "^#" | tr "," " " | tr -s " " "\t" >$newName
done

# Extract the key words that we will need to represent in our model.
echo "Extracting the key words to represent from each of the evaluation data sets"
cat $wordSimFixedFiles | cut -f 1,2 | tr "\t" "\n" | sort -u > $wordSimKeyWordsFile

# Convert the ukwac or wackypedia corpora into a one doc per line format for
# easier use later on.
echo "Extracting the documents from $wackyFiles"
$run $base.ExtractParsedWackyCopora $wackyFiles > $oneDocFile

# Extract the top N words from all contexts excluding stop words
echo "Computing the top $numTopWords words in the corpus"
cat $oneDocFile | tr -s " " "\n" | sort | uniq -c > $topWordFile.tmp
$run $base.SelectTopWords $stopWordFile $numTopWords $topWordFile.tmp $numTopWords > $topWordFile
rm $topWordFile.tmp

# Extract contexts for each of the words in the word similarity test we are
# considerinng.
echo "Extracting n word sized contexts for each key word from $oneDocFile"
mkdir -p $contextDir
$run $base.ExtractWordSimContexts $wordSimKeyWordsFile $topWordFile $windowSize \
                                  $oneDocFile $contextDir/$contextFilePrefix

# Convert each corpus file into a sparse matrix recording the
# co-occurring content words.
echo "Conveting each word's contexts into vectors using a shared feature space"
for txt in $contextDir/*.txt; do
    newName=`echo $txt | sed "s/txt$/sparse_vector.mat/"`
    $run $base.ExtractWordsiContexts $topWordFile $txt $newName
done

# Cluster each set of contexts for a multi-sense word using each of the
# specified clustering algorithms crossed with the specified number of possible
# clusterings.  Note: This Should Be Parallelized with Scream.
echo "Clustering data with many algorithms and many groupings"
for mat in $contextDir/*.mat; do
    for m in $clusterAlgsList; do
        for k in $numClustersList; do
            mat=`echo $mat | sed "s/.*$contextFilePrefix/$contextFilePrefix/"`
            newName=`echo $mat | sed "s/sparse_vector.mat/$m.$k.partition/"`
            echo $mat $newName $m $k $hdfsInputDir $hdfsOutputDir
            #$run $base.ClusterContexts $m $k $mat $newName
        done
    done
done > screamInput/wordsim-contexts
scream src/main/scream/ClusterContexts.json screamInput/wordsim-contexts

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
for w in `cat $wordSimKeyWordsFile`; do
    for k in $numClustersList; do
        inputPartitions=`for c in $clusterAlgsList; do echo $contextFilePrefix$w.$c.$k.partition; done | tr "\n" ";"`
        for c in $consensusAlgList; do
            echo $c $k $hdfsOutputDir $inputPartitions $contextFilePrefix$w.$c.$k.partition
        done
    done
done > screamInput/Consensus
scream src/main/scream/Consensus.json screamInput/Consensus

fi

for w in `cat $wordSimKeyWordsFile`; do
    for k in $numClustersList; do
        for alg in $clusterAlgsList $consensusAlgList; do
            echo $hdfsOutputDir \
                 $contextFilePrefix$w.$alg.$k.partition \
                 $hdfsInputDir \
                 $contextFilePrefix$w.sparse_vector.mat \
                 $contextFilePrefix$w.$alg.$k.prototype
        done
    done
done > screamInput/formPrototypes
scream src/main/scream/FormPrototypes.json screamInput/formPrototypes

exit
# Iterate over all of the partitions and form them into prototype vectors for
# easy comparison in semantic similarity tests
for partition in $contextDir/*.partition; do
    mat=`echo $partition | sed "s/[a-z]\+\.[0-9]\+\.partition$/sparse_vector.mat/"`
    prototypeMat=`echo $partition | sed "s/partition/prototype/"`
    echo "Forming Prototypes for $partition and data matrix $mat"
    #$run $base.FormPrototypes $partition $mat $prototypeMat
done

echo "Computing word similarity scores for each word pair in $wordSimFile"
function genereteSimilarityScores() {
echo "Model Clusters Word1 Word2 Known Computed" 
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
}
#genereteSimilarityScores > $wordSimComparisonFile

#R CMD BATCH src/main/R/plotCorrelations.R
