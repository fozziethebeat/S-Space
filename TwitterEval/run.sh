#!/bin/bash

keyWords="archery olympics"

run="scala -Xmx2g -cp target/TwitterEval-assembly-1.0.0.jar"
base="edu.ucla.sspace"
tokenizer=data/en-token.bin
englishFilter=data/classifier.nb.english-filter.json
summariesPerDay=200
resultDir=results

for word in $keyWords; do
    echo "Extracting tweets for $word"
    # Extract the tweets for the word of interest.
    python PrintTaggedTweetText.py $word > $resultsDir/tweet.$word.raw.txt


    echo "Filtering out non-english tweets using the unsupervised classifier"
    # Remove any non-english tweets as decided by the classifier trained on the
    # 500,000 tweets before the Olympic games officially started.
    $run $base.FilterEnglishTweets $englishFilter $resultsDir/tweet.$word.english.txt

    echo "Running the NER for $word"
    # Run Stanford's Named Entity Recognizer over the tweets.
    ./src/main/shell/NERTweets.sh $resultsDir/tweet.$word.english.txt $resultDir/tweet.$word.full_tagged.txt

    echo "Extracting basis mappings for $word"
    # Extract the basis mappings for each tweet.
    $run $base.ExtractBasisLists $resultDir/tweet.$word.full_tagged.txt \
                                 $tokenizer
                                 $resultDir/tweet.$word.token_basis.dat \
                                 $resultDir/tweet.$word.ne_basis.dat

    echo "Split the tweets into day segments"
    # Split each tweet into days.  This part needs to be fixed/updated/replaced,
    # but sorta works well enough.
    $run $base.DaySplit $resultDir/tweet.$word.full_tagged.txt $resultDir/tweet.$word
    # Now batch cluster each part for the word using mean and median methods.
    for part in $resultDir/tweet.$word.part.*.dat; do
        partId=`echo $part | cut -d "." 4`
        for method in mean median; do
            echo "Clustering $word part $partId using $method"
            $run $base.BatchClusterTweets $part \
                                          $resultDir/tweet.$word.token_basis.dat \
                                          $resultDir/tweet.$word.ne_basis.dat \
                                          $summariesPerDay \
                                          $resultDir/tweet.$word.batch.$method.$partId.groups.dat \
                                          $resultDir/tweet.$word.batch.$method.$partId.summary.dat \
                                          split $method
        done
    done

    for method in mean median; do
        $run $base.MergeDayGroupLists $resultDir/tweet.$word.batch.$method.*.groups.dat > $resultDir/tweet.$word.batch.$method.all.groups.csv
        $run $base.MergeDaySplitLists $resultDir/tweet.$word.batch.$method.*.summary.dat > $resultDir/tweet.$word.batch.$method.all.summary.csv
    done
done
