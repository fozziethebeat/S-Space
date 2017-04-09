#set term postscript eps enhanced color lw 3 "Helvetica" 24
#set output 'cooccurrence-results.eps'
set terminal png transparent nocrop enhanced font arial 8 size 500,350 
set output 'cooccurrence-results.png'

set xtics mirror
set format x "%2.0f"
set format y "%.2f"
set format y2 "%.2f"   
   
# move the key out of the way
set key outside bottom center vertical noreverse enhanced autotitles nobox

set ylabel "F-Score"
set y2label "V-Measure"
set xlabel 'Segment Number'
set yrange [ 0.25 : 0.80 ] noreverse nowriteback
set y2range [ 0.0 : 0.30 ] noreverse nowriteback
#set ytics 0,.1

set xrange [ 1 : 8 ] noreverse nowriteback
set y2tics auto 
set ytics auto nomirror

plot 'cooccurrence-scores-ukwac.dat' using 1:2 title "K-Means F-Score"            ls 1 pt 4 lw 1 lc rgb "red" axis x1y1 with linespoints, \
     'cooccurrence-scores-ukwac.dat' using 1:3 title "K-Means V-Measure"          ls 2 pt 9 lw 1 lc rgb "red" lt 2 axis x1y2 with linespoints, \
     'cooccurrence-scores-ukwac.dat' using 1:4 title "Agglomerative F-Score"      ls 1 pt 4 lw 1 lc rgb "blue" axis x1y1 with linespoints, \
     'cooccurrence-scores-ukwac.dat' using 1:5 title "Agglomerative V-Measure"    ls 2 pt 9 lw 1 lc rgb "blue" lt 2 axis x1y2 with linespoints, \
     'cooccurrence-scores-ukwac.dat' using 1:6 title "Spectral F-Score"           ls 1 pt 4 lw 1 lc rgb "green" axis x1y1 with linespoints, \
     'cooccurrence-scores-ukwac.dat' using 1:7 title "Spectra V-Measure"          ls 2 pt 9 lw 1 lc rgb "green" lt 2 axis x1y2 with linespoints, \
     'cooccurrence-scores-ukwac.dat' using 1:8 title "CBC F-Score"                ls 1 pt 4 lw 1 lc rgb "orange" axis x1y1 with linespoints, \
     'cooccurrence-scores-ukwac.dat' using 1:9 title "CBC V-Measure"              ls 2 pt 9 lw 1 lc rgb "orange" lt 3 axis x1y2 with linespoints, \
     'cooccurrence-scores-ukwac.dat' using 1:10 title "On-line K-Means F-Score"   ls 1 pt 4 lw 1 lc rgb "purple" axis x1y1 with linespoints, \
     'cooccurrence-scores-ukwac.dat' using 1:11 title "Ob-line K-Means V-Measure" ls 2 pt 9 lw 1 lc rgb "purple" lt 3 axis x1y2 with linespoints
        
        

        

