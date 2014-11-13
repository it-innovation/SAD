``````````````````
Plugin development
``````````````````

Overview
========

SAD plugins are executable java libraries launched by the SAD service according to incoming requests. The following diagram illustrates access to Social Networks using plugins:

.. image:: _static/img/sad_plugins_diagram.png
 :width: 100 %

Plugin launch request is received by SAD service and based on the parameters, a plugin is scheduled for execution. Request parameters can be split in two parts (see :doc:`Using SAD Control panel <using_control_panel>` for more details):

1. Execution configuration:

 .. code-block:: javascript

  {
    "pluginName": "twitter-searcher",
    "arguments": [
      {
        "search_terms": "football"
      },
      {
        "num_posts": "99"
      }
    ],
    "inputs": [],
    "outputs": [
      {
        "type": "twitter-static-search-raw"
      }
    ]
  }

 The code above tells the service to launch plugin **twitter-searcher** and pass it two arguments: *search_terms* and *num_posts*. It also requests that the plugin outputs data of type *twitter-static-search-raw*.

2. Execution schedule:

 .. code-block:: javascript

  "schedule": {
    "times": 2,
    "withIntervalInMilliseconds": 20000
  }

 The code above schedules the plugin to be run twice with 20 second interval.

Both parts of the request are saved in the database and the plugin is launched using `Quartz scheduler`_. SAD Plugin executable has full access to the service's database which allows it to retrieve arguments, input and output parameters, and save own data.

.. _`Quartz scheduler`: http://quartz-scheduler.org

Plugins must be located in the folder set in ``plugins/path`` part of the service configuration file ``sad.properties`` and follow a simple file structure detailed in the following section.

File Structure
==============

Three parts constitue a SAD plugin:

.. image:: _static/img/sad_plugin_files.png

1. Configuration file **configuration.json**.

 .. code-block:: javascript

  {
    "enabled": "y",
    "name": "basic-sns-stats",
    "description": "Basic Social Network posts analytics plugin. Extracts basics stats from collections of tweets or Facebook posts",
    "paths": {
        "jar": "target/basic-sns-stats-2.6.jar",
        "dependenciesFolder": "target/lib"
    },
    "arguments": [
    {
        "name": "num_posts",
        "description": "Number of social network posts per social network to analyse",
        "isOptional": true,
        "defaultValue": "all",
        "sample_values": "all|125"
    }
    ],
    "inputs": [
    {
        "source": "job_id",
        "defaultValue": "1",
        "required_data_types": [
            {
                "twitter-static-search-raw": "JSON Twitter posts as returned by Twitter API"
            },
            {
                "facebook-posts-raw": "JSON Facebook posts as returned by Facebook API"
            },
        ]
    },
    {
        "source": "plugin_name",
        "defaultValue": "twitter-searcher",
        "required_data_types": [
            {
                "twitter-static-search-raw": "JSON Twitter posts as returned by Twitter API"
            },
            {
                "facebook-posts-raw": "JSON Facebook posts as returned by Facebook API"
            },
        ]
    }
    ],
    "outputs": {
        "data": [
        {
            "type": "twitter-basic-stats",
            "description": "JSON containing basic stats extracted from processed tweets"
        },
        {
            "type": "twitter-static-search-raw",
            "description": "JSON containing tweets used in the analysis"
        },
        {
            "type": "facebook-basic-stats",
            "description": "JSON containing basic stats extracted from processed Facebook posts"
        },
        {
            "type": "facebook-posts-raw",
            "description": "JSON containing Facebook posts used in the analysis"
        }
        ]
    }
  }

 All fields are required, apart from values of ``arguments``, ``inputs`` and ``outputs``. Each argument entry must have name, description, isOptional flag, default value and one or more sample values.

 Arguments describe what arguments can be passed to the plugin execution and what sort of values are expected.

 Inputs define if the plugin accepts data from individual plugin executions (jobs) using ``job_id`` or all data create by a plugin with ``plugin_name``.

 Outputs describe what data can be saved by the plugin in the database and give an option to select only required data types (see :doc:`Data types <data_types>`) when the plugin is run.


2. Java executable. Must be located as specified in ``path/jar`` configuration setting:

 .. code-block:: javascript

  "paths": {
    "jar": "target/twitter-searcher-2.6.jar"


3. Folder with dependencies. Must be located as specified in ``path/dependenciesFolder`` configuration setting:

 .. code-block:: javascript

  "paths": {
    "dependenciesFolder": "target/lib"

SAD service also supports custom HTML/CSS/JS visualisations of the data collected by plugins:

.. image:: _static/img/twitter_searcher_visualisation.png
 :width: 100 %

This is achieved by placing **data.html** file into the **visualise/<plugin name>** folder on the service:

 .. code-block:: javascript

  webapps/visualise/<plugin name>

All sample SAD plugins come with a visualisation that can be used as a template and is located in:

 .. code-block:: javascript

  <plugin folder>/src/main/resources/visualise

.. image:: _static/img/visualise_folder.png

For sample plugins the **data.html** file with the help of javascript (**javascript/data.js**) requests data for a SAD job from the SAD service and display limited number of results according to URL parameters:

 .. code-block:: javascript

  ?jobid=1&num_results=20

The layout of data.html can be fully customised using HTML, CSS and javascript.

Code structure
==============

SAD plugin code is expected to go through the following steps (without ECC integration):

1. Get argument values of the execution.

2. Get input data (if any).

3. Do something with that input data according to the arguments: **any Java code**.

4. Get requested output types.

5. Save output data to the database as requested.

6. Report successful execution.

To assist with all those steps a **PluginsHelper** class can be used. For example, **twitter-searcher** plugin's code is the following:

* Initialise PluginsHelper class and get hold of arguments:

 .. code-block:: java

  // Always get Plugins helper first
  PluginsHelper ph = new PluginsHelper(args);

  // Retrieve argument "search_terms"
  String search_terms = ph.getArgumentValue("search_terms");

  // Retrieve argument "num_posts"
  String num_posts = ph.getArgumentValue("num_posts");

* Since no input data is required, search Twitter for ``search_terms`` and only request ``num_posts`` tweets:

 .. code-block:: java

  // Initialise Social Integrator
  AuthProvider ap = initialiseSocialIntegratorForTwitter("oauth-twitter.properties");

  // Run Twitter search
  List<Message> tweets = searchTwitter(ap, search_terms, num_posts, since_id);

* Get hold of requested output types (see :doc:`Data types <data_types>`) and save tweets if needed:

 .. code-block:: java

  // Check requested output types
  ArrayList<String> requestedOutputTypes = ph.getRequestedOutputTypes();

  // Save tweets to the database if requested
  if (requestedOutputTypes.contains("twitter-static-search-raw")) {
    Timestamp whenCollected = new Timestamp(System.currentTimeMillis());
    for (Message tweet : tweets) {
        logger.debug("Saving tweet [" + tweet.getId() + "]: " + tweet.getMessage());
        ph.saveData("twitter-static-search-raw", tweet.getJson(), whenCollected);
    }
  }

* Report success:

 .. code-block:: java

  // Report success - failure reported automatically
  ph.reportExecutionSuccess();
  System.exit (0);

In order to avoid duplicates when searching Twitter multiple times, a ``since_id`` parameter can be exchanged between job executions. For example, schedule:

 .. code-block:: javascript

  "schedule": {
    "times": 2,
    "withIntervalInMilliseconds": 20000
  }

will create two executions of one job. In order to avoid duplicate tweets, the first job will set ``since_id`` value:

 .. code-block:: java

  // Save new sinceID
  if (tweets.size() > 0) {
    ph.putMetadataValueForKey("since_id", tweets.get(0).getId());
  }

for the second execution to find and use:

 .. code-block:: java

  // Get last found tweet ID from metadata (to avoid getting duplicate tweets in multiple executions), can be null
  String since_id = ph.getMetadataValueForKey("since_id");

If ECC integration is enabled, plugin's metric model (see :ref:`ECC integration: SAD Plugins <ecc-sad-plugins-label>`) has to be defined in method:

 .. code-block:: javascript

  PluginEccClient.defineExperimentMetrics()

PluginHelper's methods:

 .. code-block:: java

  public void addAttribute(String entityName, String attributeName, String description, String type, String unit)
  public void sendMetric(String entityName, String attributeName, String value)

can be used to report measurements or extend existing metric model dynamically.

For more details, please have a look at sample plugins code and PluginsHelper class in the SAD service javadocs.

Direct access to Mongo Database
===============================

PluginHelper provides access to Mongo's collections via SAD Coordinator class that has the following convenience methods to interact with Mongo Database:

 .. code-block:: java

  public Datastore getDatastore(); // returns the database
  public DBCollection getDBCollection(String name); // returns collection by name
  public void deleteDatabase(); // deletes current database
  public <T> Query<T> createQuery(Class<T> type); // creates custom queries
  public <T> Key<T> saveObject(T object); // saves objects to the database

To access SAD Coordinator from SAD plugin code, use:

 .. code-block:: java

  public Coordinator getCoordinator();

To get direct access to the Data collection (collection used by SAD plugins to store all output data), use:

 .. code-block:: java

  public DBCollection getDataCollection();

Once you have the collection object, use `Mongo Java API <http://api.mongodb.org/java/current/index.html>`_ to build database queries. To start with a query limiting data to the input data given to the current plugin execution, use PluginHelper's method:

 .. code-block:: java

  public BasicDBObject getInputDataQuery();

As an example of Mongo database access, here is a walkthough basic-sns-stat's plugin code that uses Mongo to:

* Count tweets, Facebook posts
* Count tweets in English
* Find media files and descriptions

in input data.

First, get the Data collection and input data query:

 .. code-block:: java

  DBCollection collection = ph.getDataCollection(); // database collection with all the data
  BasicDBObject inputDataQuery = ph.getInputDataQuery(); // query that limits data to input jobs/plugins

Clone the input data query and expand it to only include data entries of type "twitter-static-search-raw" (tweets), then count the number of the results:

 .. code-block:: java

  BasicDBObject tweetsQuery = (BasicDBObject) inputDataQuery.copy();
  tweetsQuery.append("dataType", "twitter-static-search-raw"); // initial query expanded to limit data by dataType
  logger.debug("Tweets: " + collection.find(tweetsQuery).count());

Similarly, count Facebook posts in input data:

 .. code-block:: java

  BasicDBObject fbPostsQuery = (BasicDBObject) inputDataQuery.copy();
  fbPostsQuery.append("dataType", "facebook-posts-raw");
  logger.debug("FB posts: " + collection.find(fbPostsQuery).count());

Count number of tweets with 'lang' = 'en' (Facebook posts don't have that field) in input data:

 .. code-block:: java

  BasicDBObject englishTweetsQuery = (BasicDBObject) inputDataQuery.copy();
  englishTweetsQuery.append("jsonData.lang", "en");
  logger.debug("Tweets in English: " + collection.find(englishTweetsQuery).count());

To find tweets with media files in them, reuse existing tweets-only query by requiring 'media' field to exist:

 .. code-block:: java

  tweetsQuery.append("jsonData.entities.media", new BasicDBObject("$exists", true));

Then, create a new object describing which data fields we need the query to return (we will only need the URL link to the media file and text of the tweet):

 .. code-block:: java

  BasicDBObject keysToReturn = new BasicDBObject();
  keysToReturn.append("jsonData.text", 1);
  keysToReturn.append("jsonData.entities.media.media_url", 1);

Using ``tweetsQuery`` and ``keysToReturn`` objects we query the database, convert each returned data entry to JSONObject (for convenience), and extract URLs and corresponding descriptions:

 .. code-block:: java

  DBCursor cursor = collection.find(tweetsQuery, keysToReturn);
  JSONObject next; String text, media_url;
  while(cursor.hasNext()) {
      next = JSONObject.fromObject(cursor.next().toString());
      text = next.getJSONObject("jsonData").getString("text");
      media_url = next.getJSONObject("jsonData").getJSONObject("entities").getJSONArray("media").getJSONObject(0).getString("media_url");
      logger.debug(media_url + " (" + text + ")");
  }

Facebook post's structure is slightly different, so exactly the same task can be accomplished like so:

 .. code-block:: java

  fbPostsQuery.append("jsonData.picture", new BasicDBObject("$exists", true));
  keysToReturn = new BasicDBObject();
  keysToReturn.append("jsonData.message", 1);
  keysToReturn.append("jsonData.story", 1);
  keysToReturn.append("jsonData.name", 1);
  keysToReturn.append("jsonData.picture", 1);

  cursor = collection.find(fbPostsQuery, keysToReturn);
  while(cursor.hasNext()) {
      next = JSONObject.fromObject(cursor.next().toString());
      if (next.getJSONObject("jsonData").containsKey("message")) {
          text = next.getJSONObject("jsonData").getString("message");
      } else {
          if (next.getJSONObject("jsonData").containsKey("name")) {
              text = next.getJSONObject("jsonData").getString("name");
          } else {
              if (next.getJSONObject("jsonData").containsKey("story")) {
                  text = next.getJSONObject("jsonData").getString("story");
              } else {
                  text = "no description";
              }
          }
      }
      media_url = next.getJSONObject("jsonData").getString("picture");
      logger.debug(media_url + " (" + text + ")");
  }

Detailed reference: http://docs.mongodb.org/manual/reference/

Mongo Java API: http://api.mongodb.org/java/current/index.html

Next steps
==========

:doc:`Data types <data_types>`