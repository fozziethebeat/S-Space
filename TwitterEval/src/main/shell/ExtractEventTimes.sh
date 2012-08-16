#!/bin/bash

sports="judo gymnastics fencing tennis archery"
resultDir=tmpHtmlData
for sport in $sports; do
    for html in $resultDir/*$sport*.html; do
        node src/main/javascript/ExtractTimes.js $html
    done | python src/main/python/ConvertEventTimes.py > olympics.$sport.times.json
done
