#!/bin/bash

keyWords="gymnastics tennis archery judo fencing"

run="scala -J-Xmx4g -cp target/TwitterEval-assembly-1.0.0.jar"
base="edu.ucla.sspace"
tokenizer=data/en-token.bin
englishFilter=data/classifier.nb.english-filter.json
summariesPerDay=200
resultDir=results

mkdir $resultDir

if [ 0 != 0]; then
    echo "Already Run"
# Extract the first N tweets from the list (this should be a large numbe up to
# the time when the olympics began).  Then use those tweets to build an
# unsupervised classifier that will filter out blatantly non-english tweets.
python src/main/python/PrintFirstNTweets.py $trainingSize > $resultsDir/tweet.english-filter.train.txt
$run $base.TrainEnglishTweetFilter $resultsDir/tweet.english-filter.train.txt $englishFilter 
fi

for word in $keyWords; do
    if [ 0 != 0 ]; then
        echo "skipped"
    fi

    echo "Extracting tweets for $word"
    # Extract the tweets for the word of interest.
    python src/main/python/PrintTaggedTweetText.py $word > $resultsDir/tweet.$word.raw.txt

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
                                 $tokenizer \
                                 $resultDir/tweet.$word.token_basis.dat \
                                 $resultDir/tweet.$word.ne_basis.dat

    echo "Split the tweets into day segments"
    # Split each tweet into days.  This part needs to be fixed/updated/replaced,
    # but sorta works well enough.
    $run $base.DaySplit $resultDir/tweet.$word.full_tagged.txt $resultDir/tweet.$word

    for part in $resultDir/tweet.$word.part.*.dat; do
        partId=`echo $part | cut -d "." -f 4`

        # Now batch cluster each part for the word using mean and median methods.
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

        # Run the particle filter over each of the partitions for each word.
        $run $base.ParticleFilterTweets $part \
                                        $resultDir/tweet.$word.token_basis.dat \
                                        $resultDir/tweet.$word.ne_basis.dat \
                                        $resultDir/tweet.$word.particle.mean.$partId.groups.dat \
                                        $resultDir/tweet.$word.particle.mean.$partId.summary.dat \
                                        split 
    done

    # For each clustering method, smash together all the tweets into one large
    # group for the csv files.
    for method in batch.mean batch.median particle.mean; do
        $run $base.MergeDayGroupLists $resultDir/tweet.$word.$method.*.groups.dat > $resultDir/tweet.$word.$method.all.groups.csv
        $run $base.MergeDaySplitsLists $resultDir/tweet.$word.$method.*.summary.dat > $resultDir/tweet.$word.$method.all.summary.csv
    done
done
