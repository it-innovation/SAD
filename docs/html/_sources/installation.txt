````````````````````````````````````
SAD Service and Plugins Installation
````````````````````````````````````

Prerequisites
=============

* Java version 7
* Apache Maven version 3
* ECC version 2.1 or above *installed in Maven, see below*
* Social Integrator version 1.6 beta *installed in Maven, see below*
* MongoDB 2.4+ (http://www.mongodb.org/downloads)
* RabbitMQ Server version 2.8
* Apache Tomcat version 7 (optional) or similar with **Servlet 3 support**

Installing dependencies
=======================

**ECC version 2.1** and **Social Integrator version 1.6 beta** Java libraries are provided with this distribution in the **lib** folder. To install those dependencies in Maven, change into the **lib** folder and run the batch file provided:

 .. code-block:: sh

  cd lib
  install_into_maven (Linux/Mac)
  install_into_maven.bat (Windows)

Alternatively, run the following in command line to install Social Integrator version 1.6beta:

 .. code-block:: sh

  cd lib
  mvn install:install-file -Dfile=./SocialIntegrator-core-api-1.6beta.jar -DgroupId=gr.ntua -DartifactId=socialintegrator -Dversion=1.6beta -Dpackaging=jar


and ECC version 2.1:

 .. code-block:: sh

  mvn install:install-file -Dfile=./SocialIntegrator-core-api-1.6beta.jar -DgroupId=gr.ntua -DartifactId=socialintegrator -Dversion=1.6beta -Dpackaging=jar
  mvn install:install-file -Dfile=./experimedia-arch-ecc-amqpAPI-impl-2.1.jar -DgroupId=uk.ac.soton.itinnovation.experimedia -DartifactId=experimedia-arch-ecc-amqpAPI-impl -Dversion=2.1 -Dpackaging=jar
  mvn install:install-file -Dfile=./experimedia-arch-ecc-amqpAPI-spec-2.1.jar -DgroupId=uk.ac.soton.itinnovation.experimedia -DartifactId=experimedia-arch-ecc-amqpAPI-spec -Dversion=2.1 -Dpackaging=jar
  mvn install:install-file -Dfile=./experimedia-arch-ecc-common-dataModel-experiment-2.1.jar -DgroupId=uk.ac.soton.itinnovation.experimedia -DartifactId=experimedia-arch-ecc-common-dataModel-experiment -Dversion=2.1 -Dpackaging=jar
  mvn install:install-file -Dfile=./experimedia-arch-ecc-common-dataModel-metrics-2.1.jar -DgroupId=uk.ac.soton.itinnovation.experimedia -DartifactId=experimedia-arch-ecc-common-dataModel-metrics -Dversion=2.1 -Dpackaging=jar
  mvn install:install-file -Dfile=./experimedia-arch-ecc-common-dataModel-monitor-2.1.jar -DgroupId=uk.ac.soton.itinnovation.experimedia -DartifactId=experimedia-arch-ecc-common-dataModel-monitor -Dversion=2.1 -Dpackaging=jar
  mvn install:install-file -Dfile=./experimedia-arch-ecc-common-dataModel-provenance-2.1.jar -DgroupId=uk.ac.soton.itinnovation.experimedia -DartifactId=experimedia-arch-ecc-common-dataModel-provenance -Dversion=2.1 -Dpackaging=jar
  mvn install:install-file -Dfile=./experimedia-arch-ecc-edm-factory-2.1.jar -DgroupId=uk.ac.soton.itinnovation.experimedia -DartifactId=experimedia-arch-ecc-edm-factory -Dversion=2.1 -Dpackaging=jar
  mvn install:install-file -Dfile=./experimedia-arch-ecc-edm-impl-metrics-2.1.jar -DgroupId=uk.ac.soton.itinnovation.experimedia -DartifactId=experimedia-arch-ecc-edm-impl-metrics -Dversion=2.1 -Dpackaging=jar
  mvn install:install-file -Dfile=./experimedia-arch-ecc-edm-spec-2.1.jar -DgroupId=uk.ac.soton.itinnovation.experimedia -DartifactId=experimedia-arch-ecc-edm-spec -Dversion=2.1 -Dpackaging=jar
  mvn install:install-file -Dfile=./experimedia-arch-ecc-em-factory-2.1.jar -DgroupId=uk.ac.soton.itinnovation.experimedia -DartifactId=experimedia-arch-ecc-em-factory -Dversion=2.1 -Dpackaging=jar
  mvn install:install-file -Dfile=./experimedia-arch-ecc-em-impl-2.1.jar -DgroupId=uk.ac.soton.itinnovation.experimedia -DartifactId=experimedia-arch-ecc-em-impl -Dversion=2.1 -Dpackaging=jar
  mvn install:install-file -Dfile=./experimedia-arch-ecc-em-spec-2.1.jar -DgroupId=uk.ac.soton.itinnovation.experimedia -DartifactId=experimedia-arch-ecc-em-spec -Dversion=2.1 -Dpackaging=jar
  mvn install:install-file -Dfile=./experimedia-arch-ecc-samples-shared-2.1.jar -DgroupId=uk.ac.soton.itinnovation.experimedia -DartifactId=experimedia-arch-ecc-samples-shared -Dversion=2.1 -Dpackaging=jar


.. _service-configuration-label:

Service configuration
=====================

Since version 2.5 all default service settings can be found in **sad-service/src/main/resources/application.properties**:

 .. code-block:: sh

  # Used for metric reporting and mvn spring-boot:run
  server.port=8081
  server.context-path=/SAD

  # User login
  security.user.name=manager
  security.user.password=cZGmMqLkFE
  security.basic.realm=EXPERIMEDIA SAD

  # SAD default settings: basepath for external links, deployed path
  sad.basepath=http://localhost:8080/SAD
  sad.pluginsPath=../sad-plugins
  sad.coordinatorPath=src/main/resources/coordinator.json
  sad.resetDatabaseOnStart=false

  # ECC default settings - rabbit
  sad.eccEnabled=false
  sad.ecc.monitorId=00000000-0000-0000-0000-000000000000
  sad.ecc.rabbitIp=127.0.0.1
  sad.ecc.rabbitPort=5672
  sad.ecc.clientName=Social Analytics Dashboard
  sad.ecc.clientsUuuidSeed=3e85977c-f791-11e3-865e-b2227cce2b5

  # Unit tests
  spring.main.show_banner=false

Properties starting with **sad.** can be changed in the new service configuration UI. Make sure that regardless of your deployment choice **server.** properties are set correctly.

Local database access is configured in **sad.coordinatorPath** file as before:

 .. code-block:: javascript

  "database": {
   "mongo_server": {
    "host": "localhost",
    "port": "27017"
   }

Database with name:

 .. code-block:: javascript

  "name": "experimedia"

will be created automatically.


Plugins visualisation files
===========================

To visualise plugin output via SAD Dashboard, copy the contents of each **<sad.pluginsPath property value>/<plugin-name>/src/main/resources/visualise** folder into **sad-service/src/main/webapp/visualise/<plugin-name>** folder, so that all default (or custom) plugin visualisations exist in that folder:

 .. code-block:: javascript

  sad-service/src/main/webapp/visualise/basic-sns-stats
  sad-service/src/main/webapp/visualise/facebook-collector
  sad-service/src/main/webapp/visualise/hot-tweets
  sad-service/src/main/webapp/visualise/twitter-searcher


Build
=====

To build SAD service and plugins, in top folder of the distribution run:

 .. code-block:: javascript

  mvn install

This will create a WAR file with SAD service in:

 .. code-block:: html

  sad-service/target/sad-service-2.6.war

and also create default plugins' jars and configuration files:

 .. code-block:: javascript

  sad-plugins/<plugin name>/configuration.json
  sad-plugins/<plugin name>/target/<plugin name>-2.6.jar

Next steps
==========

:doc:`Running SAD Service <service_exec>`

:doc:`Using SAD Control panel <using_control_panel>`

:doc:`SAD configuration files explained <config_files>`