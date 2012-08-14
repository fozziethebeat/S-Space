import json
import pymongo

connection = pymongo.Connection()
db = connection.olympics_database

sportsList = ["all", "gymnastics", "tennis", "archery", "judo", "fencing"];
for sport in sportsList:
    tagList = []
    for entry in db.tags[sport].find():
        tagList.append( (entry['value'], entry['_id']) )
    tagList.sort()
    tagList.reverse()
    jsonList = [ {"key": key, "value": int(value)} for (value, key) in tagList[0:150] ]
    outFile = open("olympics.%s.tags.json"%sport, "w")
    print >>outFile, json.dumps(jsonList)
