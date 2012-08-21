set term postscript eps enhanced color lw 3 "Helvetica" 24
set output 'chart-key2.eps'
#set terminal png transparent nocrop enhanced font arial 8 size 500,350 
#set output 'chart-key.png'

# FIXME: the key needs to go at the bottom of all the plots
#unset key               
set key outside bottom center vertical noreverse enhanced autotitles nobox

# UKWAC data
plot 'key.dat' using 1:3 title "K-Means"         ls 2 pt 3 lw 1 lc rgb "red" lt 1 axis x1y1 with linespoints, \
     'key.dat' using 1:5 title "Spectral"        ls 2 pt 11 lw 1 lc rgb "green" lt 2 axis x1y1 with linespoints, \
     'key.dat' using 1:7 title "CBC"             ls 2 pt 12 lw 1 lc rgb "orange" lt 3 axis x1y1 with linespoints, \
     'key.dat' using 1:9 title "Streaming K-Means" ls 2 pt 13 lw 1 lc rgb "purple" lt 4 axis x1y1 with linespoints
