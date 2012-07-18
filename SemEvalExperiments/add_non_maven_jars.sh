#!/bin/bash

# Add apertium's morphological analyzer
mvn install:install-file -DgroupId=org.apertium -DartifactId=lttoolbox \
-Dversion=1.0.0 -Dpackaging=jar -Dfile=lib/lttoolbox.jar

# Rename the old trove4j package so that it doesn't confict with the newer
# version needed by the S-Space package.
mvn install:install-file -DgroupId=net.sf.trove4j -DartifactId=trove4j-evil \
-Dversion=2.0.2 -Dpackaging=jar -Dfile=lib/trove4j-2.0.2.jar
