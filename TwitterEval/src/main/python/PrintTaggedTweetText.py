import pymongo
import sys
import time

connection = pymongo.Connection()
db = connection.olympics_database
posts = db.posts
tagList = ["Olympics", "olympics", "Olympics2012", "Olympics12",
           "London2012", "london2012", "LONDON2012", "Olympicday"]
keyWord = sys.argv[1]

tweets = []
for tweet in posts.find(
        { "user.lang" : "en" },
        { "text": 1, "created_at" : 1} ):
    ts = time.mktime(time.strptime(tweet['created_at'],'%a %b %d %H:%M:%S +0000 %Y'))
    if keyWord in tweet['text']:
        tweets.append((ts, tweet['created_at'], tweet['text']))
tweets.sort()
print "TimeStamp Tweet"
for time, created_at, text in tweets:
    print time, text.replace("\n", " ").encode('utf-8')

        #   This is code to select only tweets using tags in the given set.
        #   Since we want all olympics tweets, we can ignore this.
        #  "entities.hashtags" : 
        #    { "$elemMatch" : 
        #        { "text": { "$in": tagList } } } },
