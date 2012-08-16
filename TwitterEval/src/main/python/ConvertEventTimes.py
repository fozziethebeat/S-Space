import json
import sys

from cleaning import convertTime

eventTimes = []
for input in sys.stdin.readlines():
    if input.startswith("bar"):
        input = input[7:]
        start, end  = input.split()
        end = start[:11] + end + start[16:]
    else:
        start, end = input.split()
        start = start[2:]
        end = end[2:]
    eventTimes.append( { "start": convertTime(start), "end": convertTime(end)} )

print json.dumps(eventTimes)
