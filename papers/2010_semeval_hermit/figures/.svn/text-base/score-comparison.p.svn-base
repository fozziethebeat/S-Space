set term postscript eps enhanced color lw 3 "Helvetica" 24
set output 'hermit-score-comparison.eps'
#set terminal png transparent nocrop enhanced font arial 8 size 500,350 
#set output 'hermit-score-comparison.png'

set multiplot
set bmargin at screen 0.95
set tmargin at screen 0.8
set lmargin at screen 0.15
set rmargin at screen 0.8  # need room for labels


set origin 0.0, 0.7
#set size 0.878, 0.3
set size 1.0, 0.25

#unset xtics
set format x "" 
set yrange [0 : 12] noreverse nowriteback
set xrange[1: 15] noreverse nowriteback 
set ytics 0, 4
set ylabel 'Clusters'
set xtics nomirror

plot 'results.dat' using 1:4 title 'Clusters' with linespoints lc rgb "blue" pt 8

set origin 0.0, 0.0
set size 1.0, .5
set bmargin at screen 0.75
set tmargin at screen 0.2
set xtics mirror
set format x "%2.0f"
set format y "%.2f"
set format y2 "%.2f"   
   
# move the key out of the way
set key inside right center vertical noreverse enhanced autotitles nobox

#set style histogram clustered gap 1 title  offset character 0, 0, 0

#set title "F-Score and V-Measure comparison for different window sizes"
set ylabel "F-Score"
set y2label "V-Measure"
set xlabel 'Window Size'
set yrange [ 0.25 : 0.64 ] noreverse nowriteback
set y2range [ 0.0 : 0.18 ] noreverse nowriteback
#set ytics 0,.1

set xrange [ 1 : 15 ] noreverse nowriteback
set y2tics auto 
set ytics auto nomirror

plot 'results.dat' using 1:2 title "F-Score" ls 1 pt 4 lw 1 axis x1y1 with linespoints, \
'results.dat' using 1:3 title "V-Measure" ls 2 pt 6 lw 1 axis x1y2 with linespoints

