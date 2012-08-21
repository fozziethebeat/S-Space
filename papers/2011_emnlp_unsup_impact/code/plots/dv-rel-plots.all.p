set term postscript eps enhanced color lw 3 "Helvetica" 28
set output 'semEval-2010-dv-wordsi-rel-stkm-test.key.eps'
#set terminal png transparent nocrop enhanced font arial 8 size 500,350 
#set output 'semEval-2010-dv-wordsi-rel-stkm-test.key.png'

#set style histogram #clustered gap 1
#set style fill solid border 0
set pointsize 2

#binwidth=1
#set boxwidth binwidth

set xrange [0.01 : .32]

set ylabel "Frequency of Sense Confusion"
set xlabel "Ontonote Sense Similarity"

plot 'semEval-2010-dv-wordsi-rel-stkm-test.key.avg.actual' using 1:2 title 'Actual (avg)' with linespoints , \
   'semEval-2010-dv-wordsi-rel-stkm-test.key.avg.baseline' using 1:2 title 'Baseline (avg)' lt 2 lc 2 with linespoints, \
   'semEval-2010-dv-wordsi-rel-stkm-test.key.max.actual' using 1:2 title 'Actual (max)' lt 1 lc 3 with linespoints , \
   'semEval-2010-dv-wordsi-rel-stkm-test.key.max.baseline' using 1:2 title 'Baseline (max)' lt 2 lc 4 with linespoints

#
#     
# CBC
#
#
      
set term postscript eps enhanced color lw 3 "Helvetica" 28
set output 'semEval-2010-dv-wordsi-rel-cbc-test.key.eps'
#set terminal png transparent nocrop enhanced font arial 8 size 500,350 
#set output 'semEval-2010-dv-wordsi-rel-cbc-test.key.png'

#set style histogram #clustered gap 1
#set style fill solid border 0
 
#binwidth=1
#set boxwidth binwidth

set xrange [0.01 : .32]

set ylabel "Frequency of Sense Confusion"
set xlabel "Ontonote Sense Similarity"

plot 'semEval-2010-dv-wordsi-rel-cbc-test.key.avg.actual' using 1:2 title 'Actual (avg)' with linespoints , \
   'semEval-2010-dv-wordsi-rel-cbc-test.key.avg.baseline' using 1:2 title 'Baseline (avg)' lt 2 lc 2 with linespoints, \
   'semEval-2010-dv-wordsi-rel-cbc-test.key.max.actual' using 1:2 title 'Actual (max)' lt 1 lc 3 with linespoints , \
   'semEval-2010-dv-wordsi-rel-cbc-test.key.max.baseline' using 1:2 title 'Baseline (max)' with linespoints

#
#
# sc06
#
#
      
set term postscript eps enhanced color lw 3 "Helvetica" 28
set output 'semEval-2010-dv-wordsi-rel-sc06-test.key.eps'
#set terminal png transparent nocrop enhanced font arial 8 size 500,350 
#set output 'semEval-2010-dv-wordsi-rel-sc06-test.key.png'

#set style histogram #clustered gap 1
#set style fill solid border 0
 
#binwidth=1
#set boxwidth binwidth

set xrange [0.01 : .32]

set ylabel "Frequency of Sense Confusion"
set xlabel "Ontonote Sense Similarity"

plot 'semEval-2010-dv-wordsi-rel-sc06-test.key.avg.actual' using 1:2 title 'Actual (avg)' with linespoints , \
   'semEval-2010-dv-wordsi-rel-sc06-test.key.avg.baseline' using 1:2 title 'Baseline (avg)' lt 2 lc 2 with linespoints, \
   'semEval-2010-dv-wordsi-rel-sc06-test.key.max.actual' using 1:2 title 'Actual (max)' lt 1 lc 3 with linespoints , \
   'semEval-2010-dv-wordsi-rel-sc06-test.key.max.baseline' using 1:2 title 'Baseline (max)' with linespoints

      
#
#
# gs-kmeans
#
#
      
set term postscript eps enhanced color lw 3 "Helvetica" 28
set output 'semEval-2010-dv-wordsi-rel-gs-kmeans-test.key.eps'
#set terminal png transparent nocrop enhanced font arial 8 size 500,350 
#set output 'semEval-2010-dv-wordsi-rel-gs-kmeans-test.key.png'

#set style histogram #clustered gap 1
#set style fill solid border 0
 
#binwidth=1
#set boxwidth binwidth

set xrange [0.01 : .32]

set ylabel "Frequency of Sense Confusion"
set xlabel "Ontonote Sense Similarity"

plot 'semEval-2010-dv-wordsi-rel-gs-kmeans-test.key.avg.actual' using 1:2 title 'Actual (avg)' with linespoints , \
   'semEval-2010-dv-wordsi-rel-gs-kmeans-test.key.avg.baseline' using 1:2 title 'Baseline (avg)' lt 2 lc 2! with linespoints, \
   'semEval-2010-dv-wordsi-rel-gs-kmeans-test.key.max.actual' using 1:2 title 'Actual (max)' lt 1 lc 3 with linespoints , \
   'semEval-2010-dv-wordsi-rel-gs-kmeans-test.key.max.baseline' using 1:2 title 'Baseline (max)' with linespoints       