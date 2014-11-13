#!/bin/bash

echo "Installing SocialIntegrator JAR in local Maven repository"
mvn install:install-file -Dfile=./SocialIntegrator-core-api-1.6beta.jar -DgroupId=gr.ntua -DartifactId=socialintegrator -Dversion=1.6beta -Dpackaging=jar
echo "Done"