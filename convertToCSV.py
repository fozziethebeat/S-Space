#!/usr/bin/python

import sys

sectionNames = {}
for line in open(sys.argv[1]):
    id, sections = line.split("\t", 1)
    sectionNames[id] = sections.strip()

docLabels = []
for line in open(sys.argv[2]):
    idRest = line.strip().split("\t", 1)
    if len(idRest) > 1:
        docLabels.append(idRest[0])

for i, line in enumerate(open(sys.argv[3])):
    line = line.strip().replace(" ", ",")
    docLabel = docLabels[i]
    if docLabel in sectionNames:
        sectionName = sectionNames[docLabel]
    else:
        sectionName = ""
    print "%s,%s,%s" %(sectionName, docLabel, line)

