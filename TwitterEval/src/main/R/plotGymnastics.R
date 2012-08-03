library(ggplot2)

plotTimeDensity <- function(partName) {
tData <- data.frame(read.table(paste("data/", partName, ".dat", sep=""), header=TRUE))
p <- ggplot(tData, aes(x=Time)) +
     #geom_histogram(binwidth=100) + 
     geom_freqpoly(binwidth=100) + 
     theme_bw()
ggsave(paste("plots/", partName, ".eps", sep=""))
}

gymnasticsFiles <- c("tweet.gymnastics.part.5",
                     "tweet.gymnastics.part.6",
                     "tweet.gymnastics.part.7",
                     "tweet.gymnastics.part.8",
                     "tweet.gymnastics.part.9",
                     "tweet.gymnastics.part.10")

for (gymnasticsFile in gymnasticsFiles) plotTimeDensity(gymnasticsFile)
