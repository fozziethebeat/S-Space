#!/bin/bash

mvn install:install-file -DgroupId=jama -DartifactId=jama \
    -Dversion=1.0 -Dpackaging=jar -Dfile=lib/jama.jar

mvn install:install-file -DgroupId=jaws  -DartifactId=jaws \
    -Dversion=1.2 -Dpackaging=jar -Dfile=lib/jaws-bin-1.2.jar

