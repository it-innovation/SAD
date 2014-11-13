```````````````
ECC integration
```````````````

SAD offers ECC integration for both the service and any SAD plugin. To enable ECC reporting, check "Use ECC" checkbox on SAD configuration page:

 .. image:: _static/img/sad_config_page_annotated.png
  :width: 100 %

SAD will create one ECC client for SAD service, and one for each plugin:

 .. image:: _static/img/ecc_dashboard_sad_clients.png
  :width: 100 %

Configuration of UUIDs of SAD ECC clients is explained in :doc:`Running & Configuring SAD Service <service_exec>`.

SAD Service
===========

The service will create its own client that will use the following ECC Metric model reporting technical data about itself:

* SAD Metric Generator (Metric generator for Social Analytics Dashboard)

  * Entity: SAD Service (Entity for Social Analytics Dashboard)

   * Attribute: Plugins Started (Number of plugins scheduled by SAD)

    * Metric type: RATIO, unit: Times

   * Attribute: Plugins Executed Successfully (Number of plugins executed successfully by SAD)

    * Metric type: RATIO, unit: Times

   * Attribute: Plugins failed (Number of plugins failed to execute (for whatever reason))

    * Metric type: RATIO, unit: Times

   * Attribute: Names of plugins scheduled using SAD (Number of times SAD plugins have executed successfully)

    * Metric type: NOMINAL

   * Attribute: Service methods called (Names of methods called in SAD API)

    * Metric type: NOMINAL

   * Attribute: Database query duration (How long it took for a database query to run)

    * Metric type: RATIO, unit: mSec

   * Attribute: Service query duration (How long it took for the service to respond as measured by the service)

    * Metric type: RATIO, unit: mSec

   * Attribute: Plugin execution duration (How long it took to execute the plugin)

    * Metric type: RATIO, unit: mSec

.. _ecc-sad-plugins-label:

SAD Plugins
===========

Since version 2.5 SAD plugins are expected to use Logback logger (http://logback.qos.ch). Using old log4j logger can **prevent plugins from executing**.

Since version 2.0-beta each SAD client has its own standalone ECC client by default located in class **PluginEccClient.java**:

 .. code-block:: java

  public class PluginEccClient extends GenericEccClient {

      public static final String METRIC_GENERATOR_MAIN = "Twitter searcher plugin metric generator";
      public static final String ENTITY_Twitter = "Twitter";
      public static final String ENTITY_SocialNetworksDataGroup = "Social Networks Data Group";
      public static final String ENTITY_PLUGINS = "SAD Plugins";
      public static final String ATTRIBUTE_TWEETS_FOUND = "Tweets count";
      public static final String ATTRIBUTE_USERS_FOUND = "Twitter users count";
      public static final String ATTRIBUTE_KEYWORDS = "Twitter channels";
      public static final String ATTRIBUTE_REQUESTS_COUNT = "Request count";
      public static final String ATTRIBUTE_REQUEST_LATENCY = "Request latency";
      public static final String ATTRIBUTE_EXECUTION_DURATION = "Successful execution duration (twitter searcher)";

      public PluginEccClient(String name, String[] args) {
          super(name, args);
      }

      @Override
      public void defineExperimentMetrics() {

          mainMetricGenerator = new MetricGenerator();
          currentExperiment.addMetricGenerator(mainMetricGenerator);
          mainMetricGenerator.setName(METRIC_GENERATOR_MAIN);
          mainMetricGenerator.setDescription("Main metric generator for " + name);

          MetricGroup metricGroup = MetricHelper.createMetricGroup(name + "'s metric group", "Metric group for plugin " + name, mainMetricGenerator);

          Entity snDataGroupEntity = new Entity();
          mainMetricGenerator.addEntity(snDataGroupEntity);
          snDataGroupEntity.setName(ENTITY_SocialNetworksDataGroup);
          snDataGroupEntity.setDescription("Entity for Social Networks data");

          Attribute a_tweets_found = MetricHelper.createAttribute(ATTRIBUTE_TWEETS_FOUND, "How many tweets were found in the last search", snDataGroupEntity);
          MeasurementSet ms_tweets_found = MetricHelper.createMeasurementSet(a_tweets_found, MetricType.RATIO, new Unit("Tweets"), metricGroup);

          Attribute a_users_found = MetricHelper.createAttribute(ATTRIBUTE_USERS_FOUND, "How many users were found in the last search", snDataGroupEntity);
          MeasurementSet ms_users_found = MetricHelper.createMeasurementSet(a_users_found, MetricType.RATIO, new Unit("Users"), metricGroup);

          Attribute a_keywords = MetricHelper.createAttribute(ATTRIBUTE_KEYWORDS, "Search terms used", snDataGroupEntity);
          MeasurementSet ms_keywords = MetricHelper.createMeasurementSet(a_keywords, MetricType.NOMINAL, new Unit(""), metricGroup);

          measurementSetMap.put(ms_tweets_found.getID(), ms_tweets_found);
          measurementSetMap.put(ms_users_found.getID(), ms_users_found);
          measurementSetMap.put(ms_keywords.getID(), ms_keywords);

          Entity twitterEntity = new Entity();
          mainMetricGenerator.addEntity(twitterEntity);
          twitterEntity.setName(ENTITY_Twitter);
          twitterEntity.setDescription("Entity for Twitter");

          Attribute a_requests_count = MetricHelper.createAttribute(ATTRIBUTE_REQUESTS_COUNT, "Number of requests given to the Social Integrator", twitterEntity);
          MeasurementSet ms_requests = MetricHelper.createMeasurementSet(a_requests_count, MetricType.RATIO, new Unit("Times"), metricGroup);

          Attribute a_request_latency = MetricHelper.createAttribute(ATTRIBUTE_REQUEST_LATENCY, "Response time of Social Integrator and Twitter", twitterEntity);
          MeasurementSet ms_latency = MetricHelper.createMeasurementSet(a_request_latency, MetricType.RATIO, new Unit("mSec"), metricGroup);

          measurementSetMap.put(ms_requests.getID(), ms_requests);
          measurementSetMap.put(ms_latency.getID(), ms_latency);

          Entity sadPluginsEntity = new Entity();
          mainMetricGenerator.addEntity(sadPluginsEntity);
          sadPluginsEntity.setName(ENTITY_PLUGINS);
          sadPluginsEntity.setDescription("Entity for SAD plugins");

          Attribute a_successful_execution_duration = MetricHelper.createAttribute(ATTRIBUTE_EXECUTION_DURATION, "How long it took to execute the plugin", sadPluginsEntity);
          MeasurementSet ms_exec_duration = MetricHelper.createMeasurementSet(a_successful_execution_duration, MetricType.RATIO, new Unit("mSec"), metricGroup);

          measurementSetMap.put(ms_exec_duration.getID(), ms_exec_duration);

      }
  }


and launched from the Main method of the plugin:

 .. code-block:: java

  PluginEccClient eccClient = new PluginEccClient(name, args);

The Plugin's ECC client runs in **ECC** mode that the plugin is launched in by the SAD Service when the service starts:

 .. image:: _static/img/sad_plugins_ecc_explained.png
  :width: 100 %

In this mode a new socket server is created (one per plugin) to accept metric model updates and new measurements from the main code when the plugin is run in **Execution** mode.

In **Execution** mode the main code of the plugin (the code that delivers the purpose functionality of the plugin) is run. A socket client is also created to communicate with the ECC client of the plugin. PluginHelper class provides the following methods to add new entities, attributes to the metric model and report new measurements:

 .. code-block:: java

  public void addEntity(String metrigeneratorName, String entityName, String description)
  public void addAttribute(String entityName, String attributeName, String description, String type, String unit)
  public void sendMetric(String entityName, String attributeName, String value)

As the metric model is defined in PluginEccClient.defineExperimentMetrics() method (see PluginEccClient class example above), it easy to report measurements on existing attributes:

 .. code-block:: java

  PluginsHelper ph = new PluginsHelper(args);
  ph.sendMetric(PluginEccClient.ENTITY_SocialNetworksDataGroup, PluginEccClient.ATTRIBUTE_TWEETS_FOUND, "100");

To add a new attribute to an existing Entity (for example, for new number of messages posted on a Facebook page to monitor):

 .. code-block:: java

  PluginsHelper ph = new PluginsHelper(args);
  String newAttribute = "Messages from page '" + page_name + "'";
  ph.addAttribute(PluginEccClient.ENTITY_STATS, newAttribute, "Number of messages collected from page '" + page_name + "'", "ratio", "Posts");

and report measurements for the new attribute:

 .. code-block:: java

  ph.sendMetric(PluginEccClient.ENTITY_STATS, newAttribute, "490");

To create an entirely new Entity and report measurements:

 .. code-block:: java

  PluginsHelper ph = new PluginsHelper(args);

  String searchTermEntityName = "Entity for search terms '" + search_terms + "'";
  ph.addEntity(PluginEccClient.METRIC_GENERATOR_MAIN, searchTermEntityName, "Dynamic entity");

  String newAttribute = "Tweets for search term '" + search_terms + "'";
  ph.addAttribute(searchTermEntityName, newAttribute, "Tweets found for search terms '" + search_terms + "'", "ratio", "Tweets");
  ph.sendMetric(searchTermEntityName, newAttribute, Integer.toString(tweets.size()));

For detailed examples see Main methods of default plugins in **sad-plugins** folder.


Twitter searcher (twitter-searcher)
-----------------------------------

Has the following metric model by default:

* Twitter searcher's metric generator (Main metric generator for Twitter searcher)

  * Entity: Twitter (Social Network Twitter as an Entity)

   * Attribute: Request count (Number of requests given to the Social Integrator)

    * Metric type: RATIO, unit: Times

   * Attribute: Request latency (Response time of Social Integrator and Twitter)

    * Metric type: RATIO, unit: mSec

  * Entity: Social Networks Data Group (fixed UUID ef8ef409-11c8-42d9-814a-0f041f3fcbad)

   * Attribute: Twitter channels (Search Terms Used)

    * Metric type: NOMINAL

   * Attribute: Tweets count (Tweets collected in a single plugin execution)

    * Metric type: RATIO, unit: Tweets

   * Attribute: Twitter user count (Distinct users collected in a single plugin execution)

    * Metric type: RATIO, unit: Users

  * Entity: SAD Plugins (fixed UUID ef8ef409-11c8-42d9-814a-0f041f3fcbaf)

   * Attribute: Successful execution duration (How long it took to execute the plugin)

    * Metric type: RATIO, unit: mSec

For each new search term it extends the model by adding a new attribute (tweets found for input search terms) to a new entity (search terms):

 * Entity: Twitter search term (dynamic entity)

  * Attribute: Tweets count (Tweets collected from a search term)

   * Metric type: RATIO, unit: Tweets

 .. code-block:: java

  String searchTermEntityName = "Entity for search terms '" + search_terms + "'";
  ph.addEntity(PluginEccClient.METRIC_GENERATOR_MAIN, searchTermEntityName, "Dynamic entity");

  String newAttribute = "Tweets for search term '" + search_terms + "'";
  ph.addAttribute(searchTermEntityName, newAttribute, "Tweets found for search terms '" + search_terms + "'", "ratio", "Tweets");
  ph.sendMetric(searchTermEntityName, newAttribute, Integer.toString(tweets.size()));

Facebook collector (facebook-collector)
---------------------------------------

Has the following metric model by default:

* Facebook collector's metric generator (Main metric generator for Facebook collector)

  * Entity: Facebook (Social Network Facebook as an Entity)

   * Attribute: Request count (Number of requests given to the Social Integrator)

    * Metric type: RATIO, unit: Times

   * Attribute: Request latency (Response time of Social Integrator and Twitter)

    * Metric type: RATIO, unit: mSec

  * Entity: Social Networks Data Group (fixed UUID ef8ef409-11c8-42d9-814a-0f041f3fcbad)

   * Attribute: Facebook channels (Facebook pages collected from)

    * Metric type: NOMINAL

   * Attribute: Facebook posts count (Number of Facebook posts collected)

    * Metric type: RATIO, unit: Tweets

   * Attribute: Facebook user count (Distinct Facebook users posting)

    * Metric type: RATIO, unit: Users

  * Entity: SAD Plugins (fixed UUID ef8ef409-11c8-42d9-814a-0f041f3fcbaf)

   * Attribute: Successful execution duration (How long it took to execute the plugin)

    * Metric type: RATIO, unit: mSec

For each new search term it extends the model by adding a new attribute (messages per Facebook page) to entity (new Facebook page):

 * Entity: Facebook page (dynamic entity)

  * Attribute: Facebook posts count (Facebook posts collected from the Facebook page)

   * Metric type: RATIO, unit: Posts

 .. code-block:: java

  String pageEntityName = "Entity for Facebook page '" + page_name + "'";
  ph.addEntity(PluginEccClient.METRIC_GENERATOR_MAIN, pageEntityName, "Dynamic entity");

  String newAttribute = "Messages from page '" + page_name + "'";
  ph.addAttribute(pageEntityName, newAttribute, "Number of messages collected from page '" + page_name + "'", "ratio", "Posts");
  ph.sendMetric(pageEntityName, newAttribute, Integer.toString(collectedMessages.size()));


Hot tweets (hot-tweets)
---------------------------------------

Has the following metric model by default:

* Hot tweets' metric generator (Main metric generator for Hot tweets)

  * Entity: Social Networks Data Group (fixed UUID ef8ef409-11c8-42d9-814a-0f041f3fcbad)

   * Attribute: Tweets analysed (How many tweets were analysed)

    * Metric type: RATIO, unit: Tweets

   * Attribute: Hot tweets generated (How many hot tweets were identified)

    * Metric type: RATIO, unit: Tweets

  * Entity: SAD Plugins (fixed UUID ef8ef409-11c8-42d9-814a-0f041f3fcbaf)

   * Attribute: Successful execution duration (How long it took to execute the plugin)

    * Metric type: RATIO, unit: mSec


Basic stats for Twitter/Facebook collected data (basic-sns-stats)
-----------------------------------------------------------------

Has the following metric model by default:

* Basic analytics's metric generator (Main metric generator for Basic analytics)

  * Entity: Social Networks Data Group (fixed UUID ef8ef409-11c8-42d9-814a-0f041f3fcbad)

   * Attribute: Tweets analysed (How many tweets were analysed)

    * Metric type: RATIO, unit: Tweets

   * Attribute: Facebook posts analysed (How many Facebook posts were analysed)

    * Metric type: RATIO, unit: Posts

   * Attribute: Media files with links (Number of media files (images mostly) with links were found in a social network)

    * Metric type: RATIO, unit: Files

  * Entity: SAD Plugins (fixed UUID ef8ef409-11c8-42d9-814a-0f041f3fcbaf)

   * Attribute: Successful execution duration (How long it took to execute the plugin)

    * Metric type: RATIO, unit: mSec

For each new search term it extends the model by adding a new attribute to entity "SAD Plugins":

 * Attribute: Output data types (dynamic, names of requested output data types)

  * Metric type: NOMINAL


 .. code-block:: java

  String newAttribute = "Requested output data types";
  ph.addAttribute(PluginEccClient.ENTITY_STATS, newAttribute, "Output data types", "nominal", "Data type");
  for (String dataType : requestedOutputTypes) {
      ph.sendMetric(PluginEccClient.ENTITY_STATS, newAttribute, dataType);
  }



Next steps
==========

:doc:`SAD Service API <service_api>`