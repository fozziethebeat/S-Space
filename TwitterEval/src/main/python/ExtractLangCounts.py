import json
import pymongo

connection = pymongo.Connection()
db = connection.olympics_database

sportsList = ["all", "gymnastics", "tennis", "archery", "judo", "fencing"];
for sport in sportsList:
    tagList = []
    for entry in db.langs[sport].find():
        tagList.append( (entry['value'], entry['_id']) )
    jsonList = [ {"user.lang": key, "count": int(value)} for (value, key) in tagList ]
    outFile = open("olympics.%s.langs.json"%sport, "w")
    print >>outFile, json.dumps(jsonList)
