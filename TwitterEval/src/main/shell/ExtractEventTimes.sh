#!/bin/bash

resultDir=tmpHtmlData
for html in $resultDir/*.html; do
    echo $html
    node src/main/javascript/ExtractTimes.js $html
done > olympics.event.times.txt
