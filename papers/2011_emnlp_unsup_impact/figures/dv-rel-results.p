set term postscript eps enhanced color lw 3 "Helvetica" 24
set output 'dv-rel-results.eps'
#set terminal png transparent nocrop enhanced font arial 8 size 500,350 
#set output 'dv-rel-results.png'

set pointsize 2
set multiplot 
set size 0.35,0.35

# FIXME: the key needs to go at the bottom of all the plots
unset key               
#set key outside bottom center vertical noreverse enhanced autotitles nobox

set xtics mirror
set format x "%2.0f"
set format y "%.2f"

set yrange [ 0.0 : 0.30 ] noreverse nowriteback
set ylabel "V-Measure"
set format x ''
set title "ukWaC"

set lmargin 0
set rmargin 0
set bmargin 0
set tmargin 0
set origin 0.13, 0.55

# UKWAC data
plot 'dv-wordsi-rel-scores-ukwac.dat' using 1:3 title "K-Means V-Measure"          ls 2 pt 3 lw 1 lc rgb "red" lt 1 axis x1y1 with linespoints, \
     'dv-wordsi-rel-scores-ukwac.dat' using 1:5 title "Spectra V-Measure"          ls 2 pt 11 lw 1 lc rgb "green" lt 2 axis x1y1 with linespoints, \
     'dv-wordsi-rel-scores-ukwac.dat' using 1:7 title "CBC V-Measure"              ls 2 pt 12 lw 1 lc rgb "orange" lt 3 axis x1y1 with linespoints, \
     'dv-wordsi-rel-scores-ukwac.dat' using 1:9 title "On-line K-Means V-Measure" ls 2 pt 13 lw 1 lc rgb "purple" lt 4 axis x1y1 with linespoints

unset ylabel
set format y ''        
set title "Wikipedia"
set origin 0.55, 0.55
        
# WIKIPEDIA
plot 'dv-wordsi-rel-scores-wiki.dat' using 1:3 title "K-Means V-Measure"          ls 2 pt 3 lw 1 lc rgb "red" lt 1 axis x1y1 with linespoints, \
     'dv-wordsi-rel-scores-wiki.dat' using 1:5 title "Spectra V-Measure"          ls 2 pt 11 lw 1 lc rgb "green" lt 2 axis x1y1 with linespoints, \
     'dv-wordsi-rel-scores-wiki.dat' using 1:7 title "CBC V-Measure"              ls 2 pt 12 lw 1 lc rgb "orange" lt 3 axis x1y1 with linespoints, \
     'dv-wordsi-rel-scores-wiki.dat' using 1:9 title "On-line K-Means V-Measure" ls 2 pt 13 lw 1 lc rgb "purple" lt 4 axis x1y1 with linespoints

# remove the ukWaC and Wikipedia titles for the lower graphs
unset title        
# add the xtic labeling back
set format x ' %g'
set format y ' %.2f'
   
set xtics auto
set ylabel "Paired FScore"
set xlabel 'Segment Number'
# NOTE: this range should stay fixed across all result plots
#       to ensure horizontal readability
set yrange [ 0.0 : 0.80 ] noreverse nowriteback


#set xrange [ 1 : 8 ] noreverse nowriteback
set ytics auto 

set origin 0.13, 0.13
   
# UKWAC
plot 'dv-wordsi-rel-scores-ukwac.dat' using 1:2 title "K-Means F-Score"            ls 1 pt 3 lw 1 lc rgb "red" lt 1 axis x1y1 with linespoints, \
     'dv-wordsi-rel-scores-ukwac.dat' using 1:4 title "Spectral F-Score"           ls 1 pt 11 lw 1 lc rgb "green" lt 2 axis x1y1 with linespoints, \
     'dv-wordsi-rel-scores-ukwac.dat' using 1:6 title "CBC F-Score"                ls 1 pt 12 lw 1 lc rgb "orange" lt 3 axis x1y1 with linespoints, \
     'dv-wordsi-rel-scores-ukwac.dat' using 1:8 title "On-line K-Means F-Score"   ls 1 pt 13 lw 1 lc rgb "purple" lt 4 axis x1y1 with linespoints

unset ylabel
set format y ''
set origin 0.55, 0.13
        
# WIKIPEDIA
plot 'dv-wordsi-rel-scores-wiki.dat' using 1:2 title "K-Means F-Score"            ls 1 pt 3 lw 1 lc rgb "red" lt 1 axis x1y1 with linespoints, \
     'dv-wordsi-rel-scores-wiki.dat' using 1:4 title "Spectral F-Score"           ls 1 pt 11 lw 1 lc rgb "green" lt 2 axis x1y1 with linespoints, \
     'dv-wordsi-rel-scores-wiki.dat' using 1:6 title "CBC F-Score"                ls 1 pt 12 lw 1 lc rgb "orange" lt 3 axis x1y1 with linespoints, \
     'dv-wordsi-rel-scores-wiki.dat' using 1:8 title "On-line K-Means F-Score"   ls 1 pt 13 lw 1 lc rgb "purple" lt 4 axis x1y1 with linespoints
        
        
        

        

