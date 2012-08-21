import json
import sys

from cleaning import convertTime

eventTimes = []
for input in sys.stdin.readlines():
    input = input.strip()
    if input.endswith("_"):
        input = input[7:]
        parts = input.split()
        start = parts[0]
        end = parts[1][2:] + "-0100"
    elif input.startswith("bar"):
        input = input[7:]
        start, end  = input.split()
        end = start[:11] + end + start[16:]
    else:
        start, end = input.split()
        start = start[2:] +"-0100"
        end = end[2:] + "-0100"
    eventTimes.append( { "end": convertTime(start), "start": convertTime(end)} )

print json.dumps(eventTimes)
