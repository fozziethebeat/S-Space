#!/bin/bash

# List the specific sport names to scrape.
sports="judo gymnastics-artistic fencing tennis archery"
# List the days for july and august.  We do this by creating the sequence of
# days of the month and add the month prefix.
julyDays=`seq  25 31 | while read day; do echo $day-july; done`
augDays=`seq 1 12 | while read day; do echo $day-august; done`
# Use a mozilla based user agent so that london2012 doesn't reject our wget
# request.  Without this, we'll get a 403 forbidden.
userAgent="Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.3) Gecko/2008092416 Firefox/3.0.3"
resultDir=tmpHtmlData

# Define a helper function to return the full url based on the sport and day.
# This is just to make changing the url easier.
function olympicsUrl() {
    echo http://www.london2012.com/$sport/schedule-and-results/day=$day/all-day.html 
}

# Now iterate through every sport.
for sport in $sports; do
    # And every day of the olympics.
    for day in $julyDays $augDays; do
        # And do a simple wget request.  Since each page has the same html file
        # name, the -O bit saves the html output to a file based on the sport
        # and day for easier management.
        wget --user-agent=$userAgent -O $tmpHtmlData/$sport-$day.html `olympicsUrl`
    done
done
