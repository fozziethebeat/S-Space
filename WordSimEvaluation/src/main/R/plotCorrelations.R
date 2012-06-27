library(plyr)
library(ggplot2)

compareData <- data.frame(read.table("data/wordsim.induced.comparison.dat", header=TRUE))
correlate <- function(x) {
    cor(x[,5], x[,6], method="spearman")
}
correlationData <- ddply(compareData, .(Model, Clusters), correlate)
p <- ggplot(correlationData, aes(x=Clusters, y=V1, group=Model)) + 
     geom_point(aes(colour=Model, style=Model)) + 
     geom_line(aes(colour=Model, linetype=Model)) +
     theme_bw() +
     xlab("Number of prototypes per word") +
     ylab("Correlation to human judgements")

ggsave("wordsim.induced.comparison.eps")
