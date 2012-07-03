#!/bin/bash

# Add stanford nlp (Not in trunk yet)
mvn install:install-file -DgroupId=org.apertium -DartifactId=lttoolbox \
-Dversion=1.0.0 -Dpackaging=jar -Dfile=lib/lttoolbox.jar
