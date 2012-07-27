library(ggplot2)

tData <- data.frame(read.table("tweet.timeline.groups.dat", header=TRUE))
sData <- data.frame(read.table("tweet.timeline.split.dat", header=TRUE))

p <- ggplot(tData, aes(x=Time, group=Group, colour=Group)) + 
     geom_histogram(binwidth=100) + 
     geom_vline(xintercept=as.numeric(sData$Time)) +
     theme_bw()
ggsave("tweet.lakers.partitioned.eps")

tData <- data.frame(read.table("tweet.timeline.batch.groups.dat", header=TRUE))
sData <- data.frame(read.table("tweet.timeline.batch.split.dat", header=TRUE))

p <- ggplot(tData, aes(x=Time, group=Group, colour=Group)) + 
     geom_histogram(binwidth=100) + 
     geom_vline(xintercept=as.numeric(sData$Time)) +
     theme_bw()
ggsave("tweet.lakers.batch.partitioned.eps")
