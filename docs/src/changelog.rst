`````````
Changelog
`````````

Changes since version 2.5
=========================

* Backend framework updated to Spring Boot (http://spring.io)
* Frontend framework updated to Foundation 5 (http://foundation.zurb.com)
* Service can now be fully configured and reset from the UI
* Pause, resume, cancel, delete controls have been added to jobs
* Added basic authentication and HTTPS proxy support
* Added configurable "fixed" ECC client UUIDs for all SAD clients
* Log4j logging framework updated to Logback for both the service and plugins
* Supports ECC API v2.1

Changes since version 2.0-beta
==============================

* Takes full advantage of ECC API 2.0
* Adds new ECC metrics to both SAD Service and all Plugins
* Adds new Hot tweets plugin (deployment release only)
* Exposes data behind job executions
* Adds Vagrant script for painless deployment
* Adds stability improvements

Changes since version 1.6
=========================

* Underlying database changed from PostgreSQL to Mongo
* SAD plugins integrated with ECC
* Database access for SAD plugins

Changes since version 1.5
=========================

* Default plugins completely rewritten from scratch:

 * Twitter searcher plugin - fetches tweets containing keywords or hashtags.
 * Facebook collector plugin - collects posts from Facebook pages.
 * Basic stats plugin - produces basic statistics on social networks data collected by the Twitter and Facebook plugins.

* Twitter and Facebook plugin using EXPERIMEDIA Social Integrator to connect to Social Networks.

* All default plugins written using developer-friendly Plugin Helper class that simplifies common plugin tasks:

 * Retrieval of arguments, input, requested outputs.
 * Storage and retrieval of job metadata.
 * Saving data into database.

* All default plugins come with HTML/CSS/JS visualisation view of output data.

* SAD java packages have been renamed from eu.experimedia.itinnovation* to uk.ac.soton.itinnovation.sad*.

* License for SAD core has been changed from EXPERIMEDIA project license to GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1.

* Default plugin licenses have been change from EXPERIMEDIA project license to Apache Software License, Version 2.0.

* Enhanced plugin configuration to include detailed information about input data and output data types.

* Default plugin logging has been switched to log4j.

* Individual plugin executions saved into log files.

* Significantly improved documentation.