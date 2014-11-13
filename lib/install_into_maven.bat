echo off

echo "Installing Social Integrator JAR in local Maven repository"
call mvn install:install-file -Dfile=./SocialIntegrator-core-api-1.6beta.jar -DgroupId=gr.ntua -DartifactId=socialintegrator -Dversion=1.6beta -Dpackaging=jar
echo "Done"