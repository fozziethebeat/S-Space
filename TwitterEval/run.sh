#!/bin/bash

keyWords="archery gymnastics judo fencing tennis"

run="scala -J-Xmx4g -cp target/TwitterEval-assembly-1.0.0.jar"
base="edu.ucla.sspace"
tokenizer=data/en-token.bin
summariesPerDay=200
resultDir=results

mkdir $resultDir

for word in $keyWords; do
    #if [ 0 != 0 ]; then
    #fi
    echo "Extracting tweets for $word"
    # Extract the tweets for the word of interest.
    python src/main/python/PrintTaggedTweetText.py $word > $resultDir/tweet.$word.raw.txt
    echo "Running the NER for $word"
    # Run Stanford's Named Entity Recognizer over the tweets.
    ./src/main/shell/NERTweets.sh $resultDir/tweet.$word.raw.txt $resultDir/tweet.$word.full_tagged.txt
    echo "Extracting basis mappings for $word"
    # Extract the basis mappings for each tweet.
    $run $base.ExtractBasisLists $resultDir/tweet.$word.full_tagged.txt \
                                 $tokenizer \
                                 $resultDir/tweet.$word.token_basis.dat \
                                 $resultDir/tweet.$word.ne_basis.dat
    echo "Split the tweets into day segments"
    # Split each tweet into days.  This part needs to be fixed/updated/replaced,
    # but sorta works well enough.
    $run $base.DaySplit $resultDir/tweet.$word.full_tagged.txt $resultDir/tweet.$word
    # Now batch cluster each part for the word using mean and median methods.
    for part in $resultDir/tweet.$word.part.*.dat; do
        partId=`echo $part | cut -d "." -f 4`
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
        $run $base.MergeDaySplitsLists $resultDir/tweet.$word.batch.$method.*.summary.dat > $resultDir/tweet.$word.batch.$method.all.summary.csv
    done
done
