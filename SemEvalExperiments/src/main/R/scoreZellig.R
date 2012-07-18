library(ggplot2)
zdata <- data.frame(read.table("data/zelligSolutions/zellig.test.dat", header=TRUE))

p <- ggplot(zdata, aes(x=Word, y=Score, group=Model)) +
     geom_point(aes(colour=Model, shape=Model)) + 
     geom_line(aes(colour=Model, linetype=Model)) + 
     facet_grid(Test~Feature) + theme_bw()
ggsave("zellig.test.eps")
