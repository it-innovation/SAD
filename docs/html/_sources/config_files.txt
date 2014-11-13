`````````````````````````````````
SAD configuration files explained
`````````````````````````````````

SAD Service Database access
===========================

SAD Service is using SAD Coordinator as a framework for PosgreSQL database access. Configuration file: **sad-service/src/main/resources/coordinator.json**.

 .. code-block:: javascript

  {
     "database":{
        "server":{
           "url":"jdbc:postgresql://localhost:5432/",
           "username":"postgres",
           "password":"sofia"
        },
        "name":"experimedia-sad",
        "schemas":{
           "management":"management",
           "data":"data"
        }
     }
  }

``server``: full PostgreSQL server URL including port.

``username``, ``password``: valid credentials for database user (preferably with admin priviledges).

``name``: name of the database to be used.

``management``, ``data``: internal names for datbase schemas to store management information and all data.


SAD Service
===========

See :doc:`Running & Configuring SAD Service <service_exec>`.


Sample SAD Plugins
==================

Configuration files for sample SAD plugins **sad-plugins/<plugin name>/src/main/resources/configuration.json** are used to generate main configuration file for corresponding plugins during Maven build.

 .. code-block:: javascript

  {
     "enabled": "y",
     "name": "basic-sns-stats",
     "description": "Basic Social Network posts analytics plugin. Extracts basics stats from collections of tweets or Facebook posts",
     "paths": {
         "jar": "target/${project.build.finalName}.jar",
         "dependenciesFolder": "target/${sad.plugin.lib.folder}"
     },
     "arguments": [ ],
     "inputs": [ ],
     "outputs": { }
  }

``enabled``: if set to ``n``, the plugin will be omitted from installed plugins list by the service.

``name``: the name of the plugin. Can not contain spaces or symbols as used in visualisation URLs.

``description``: human-readable description of the plugin.

``paths/jar``: location of the plugin jar with Main method.

``paths/dependenciesFolder``: location of the folder with all plugin's java dependencies.

``arguments``, ``inputs``, ``outputs``: describe plugin's interaction with the world.