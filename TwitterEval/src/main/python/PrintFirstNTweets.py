import pymongo
import sys
from cleaning import convertTime, removeLineBreaks, removeHttp, removeOddities

connection = pymongo.Connection()
db = connection.olympics_database
posts = db.posts
nTweets = int(sys.argv[1])

tweets = []
for tweet in posts.find(
        { "user.lang" : "en" },
        { "text": 1, "created_at" : 1} ).limit(nTweets):
    ts = convertTime(tweet['created_at'])
    tweets.append((ts, tweet['created_at'], tweet['text']))
tweets.sort()
print "TimeStamp Tweet"
for time, created_at, text in tweets:
    print time, removeOddities(removeHttp(removeLineBreaks(text))).encode('utf-8')
