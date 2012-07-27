#/bin/bash

source config.sh

# First parse each context and store the contexts for each word in separate
# files.
#$run $base.ParseSemEval2007 $semEval07Dir/test/English_sense_induction.debug.xml \
#                            $semEval07Dir/semeval07.

# Extract the headers for each word.  This will simply be the lines in each
# conll file that don't start with a digit. At the same time, extract the
# training contexts and store them in a separate file.
for conll in `ls $semEval07Dir/semeval07.*.{n,v}.conll`; do
    newName=`echo $conll | sed "s/conll$/headers/"`
    cat $conll | grep "^[a-z]" > $newName
    trainName=`echo $conll | sed "s/conll$/train.conll/"`
    $run $base.ExtractTrainContexts $conll $trainKey > $trainName
done

# Extract the number of word senses for each word based on the gold label key
cut -d " " -f 3 $fullKey | sort -u | cut -d "." -f 1,2 | uniq -c | awk '{ print $2 " " $1 }' > $wordAndSenseList

# Define two functions, one for converting the training contexts into feature
# vectors and then projecting the full dataset into that feature space and a
# second function for just converting the full dataset into feature vectors.
# These are placed in functions to easily comment them out.

function extractTestTrainSplit() {
# Before creating the per context feature space, extract a bigram matrix for the
# second order feature space.
echo "Building a bigram matrix covering the entire dataset"
$run $base.BigramMain 5.0 $stopWords $bigramTrainMatrix $bigramTrainBasis $semEval07Dir/*.train.conll

# Convert each word's contexts, in CoNLL format, into lists of sparse vectors.
# These will have disjoint feature sets.
for data in $semEval07Dir/*.train.conll; do
    echo "Processing $data with feature extractors"

    # Extract the word co-occurrence, part of speech, and dependency
    # relationship feature spaces.
    for f in "woc 25" "pos 25" "dep 5"; do
        echo "Extracting feature vectors for $f"
        feature=`echo $f | cut -d " " -f 1`
        window=`echo $f | cut -d " " -f 2`

        trainMat=`echo $data  | sed "s/train.conll/$feature.train.sparse_vector.mat/"`
        testMat=`echo $data  | sed "s/train.conll/$feature.test.sparse_vector.mat/"`
        basisName=`echo $data | sed "s/train.conll/$feature.train.basis/"`
        testData=`echo $data  | sed "s/train.conll/conll/"`

        $run $base.ExtractWordsiContexts $feature $window $stopWords $data $trainMat $basisName
        $run $base.ExtractWordsiContexts -l $feature $window $stopWords $testData $testMat $basisName
    done
    # Extract the morphological analysis feature space
    for f in "morph 25"; do
        echo "Extracting feature vectors for $f"
        feature=`echo $f | cut -d " " -f 1`
        window=`echo $f | cut -d " " -f 2`

        trainMat=`echo $data  | sed "s/train.conll/$feature.train.sparse_vector.mat/"`
        testMat=`echo $data  | sed "s/train.conll/$feature.test.sparse_vector.mat/"`
        basisName=`echo $data | sed "s/train.conll/$feature.train.basis/"`
        testData=`echo $data  | sed "s/train.conll/conll/"`

        $run $base.MorphAnalysisWordsi $analyzer $window $stopWords $data $trainMat $basisName
        $run $base.MorphAnalysisWordsi -l $analyzer $window $stopWords $testData $testMat $basisName
    done
    # Extract the second order feature space
    for f in sndord; do
        echo "Extracting second order feature vectors for $f"
        trainMat=`echo $data | sed "s/train.conll/$f.train.sparse_vector.mat/"`
        testMat=`echo $data  | sed "s/train.conll/$f.test.sparse_vector.mat/"`
        testData=`echo $data | sed "s/train.conll/conll/"`
        $run $base.SecondOrderWordsi $bigramTrainMatrix $bigramTrainBasis $data $trainMat
        $run $base.SecondOrderWordsi $bigramTrainMatrix $bigramTrainBasis $testData $testMat
    done
    # Extract the graph feature space
    for f in graph; do
        echo "Extracting graph featues"
        baseName=`echo $data | sed "s/train.conll/$f.train/"`
        $run $base.ExtractGraphWordsi $stopWords $wikiWordCounts $data $baseName
    done
done
}

function extractFullSplits() {
# Before creating the per context feature space, extract a bigram matrix for the
# second order feature space.
echo "Building a bigram matrix covering the entire dataset"
$run $base.BigramMain 5.0 $stopWords $bigramMatrix $bigramBasis $semEval07Dir/*.{v,n}.conll

# Convert each word's contexts, in CoNLL format, into lists of sparse vectors.
# These will have disjoint feature sets.
for data in `ls $semEval07Dir/*.{v,n}.conll`; do
    echo "Processing $data with feature extractors"

    # Extract the word co-occurrence, part of speech, and dependency
    # relationship feature spaces.
    for f in "woc 25" "pos 25" "dep 5"; do
        echo "Extracting feature vectors for $f"
        feature=`echo $f | cut -d " " -f 1`
        window=`echo $f | cut -d " " -f 2`
        matName=`echo $data  | sed "s/conll/$feature.full.sparse_vector.mat/"`
        basisName=`echo $data | sed "s/conll/$feature.full.basis/"`
        $run $base.ExtractWordsiContexts $feature $window $stopWords $data $matName $basisName
    done
    # Extract the morphological analysis feature space
    for f in "morph 25"; do
        echo "Extracting feature vectors for $f"
        feature=`echo $f | cut -d " " -f 1`
        window=`echo $f | cut -d " " -f 2`
        matName=`echo $data | sed "s/conll/$feature.full.sparse_vector.mat/"`
        basisName=`echo $data | sed "s/conll/$feature.full.basis/"`
        $run $base.MorphAnalysisWordsi $analyzer $window $stopWords $data $matName $basisName
    done
    # Extract the second order feature space
    for f in sndord; do
        echo "Extracting second order feature vectors for $f"
        matName=`echo $data | sed "s/conll/$f.full.sparse_vector.mat/"`
        $run $base.SecondOrderWordsi $bigramMatrix $bigramBasis $data $matName
    done
    # Extract the graph based representation
    for f in graph; do
        echo "Extracting a graph model for $f"
        baseName=`echo $data | sed "s/conll/$f.full/"`
        $run $base.ExtractGraphWordsi $stopWords $wikiWordCounts $data $baseName
    done
done
}

extractTestTrainSplit
extractFullSplits

$run $base.SplitKey $testKey $semEval07Dir/semeval07 test
$run $base.SplitKey $fullKey $semEval07Dir/semeval07 full
cat $fullKey | cut -d " " -f 3 | \
               sort -u | \
               cut -d "." -f 1,2 | \
               uniq -c | \
               awk '{ print $2 " " $1}' > $wordAndSenseList
