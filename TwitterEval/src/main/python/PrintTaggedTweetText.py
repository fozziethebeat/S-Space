import pymongo
import sys
from cleaning import convertTime, removeLineBreaks, removeHttp

connection = pymongo.Connection()
db = connection.olympics_database
posts = db.posts
keyWord = sys.argv[1]

tweets = []
for tweet in posts.find( 
        { "user.lang" : "en" },
        { "text": 1, "created_at" : 1} ):
    ts = convertTime(tweet['created_at'])
    if keyWord in tweet['text']:
        tweets.append((ts, tweet['created_at'], tweet['text']))
tweets.sort()
print "TimeStamp Tweet"
for time, created_at, text in tweets:
    print time, removeHttp(removeLineBreaks(text)).encode('utf-8')
