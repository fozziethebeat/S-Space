library(ggplot2)
library(plyr)


zdata <- data.frame(read.table("data/zelligSolutions/zellig.test.dat", header=TRUE))
zelligs <- ddply(zData, .(Feature, Model, Test),
                 function(x) data.frame(Mean=mean(x$Score),
                                        Correct=sum(x$Correct), 
                                        Total=sum(x$Total)))
p <- ggplot(zelligs, aes(x=Feature, y=mean, group=Model)) +
     geom_point(aes(colour=Model, shape=Model)) + 
     geom_line(aes(colour=Model, linetype=Model)) + 
     facet_grid(.~Test) +
     theme_bw()
ggsave("zellig.mean.test.eps")
