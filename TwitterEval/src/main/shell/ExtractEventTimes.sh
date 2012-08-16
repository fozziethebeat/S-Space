#!/bin/bash

resultDir=tmpHtmlData
for html in $resultDir/*.html; do
    node src/main/javascript/ExtractTimes.js $html
done | python src/main/python/ConvertEventTimes.py > olympics.event.times.txt
