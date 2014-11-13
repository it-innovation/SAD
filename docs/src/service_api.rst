```````````````
SAD Service API
```````````````

SAD service offers REST interface to interact with the service, plugins, jobs, executions and their data. Detailed list of SAD Service API calls and responses can be found below.

Service
=======

**Get configuration**

REQUEST:

 .. code-block:: javascript

  URL: <basepath>/service/q/getconfiguration
  Type: GET

RESPONSE (service configuration as JSON):

 .. code-block:: javascript

  {
     "resultType":"sadproperties",
     "resultValue":{
        "plugins":{
           "path":"../sad-plugins"
        },
        "coordinator":{
           "path":"src/main/resources/coordinator.json",
           "reset_database_on_start":"n"
        },
        "basepath":"http://localhost:8081/SAD",
        "ecc":{
           "enabled":"n",
           "Rabbit_IP":"127.0.0.1",
           "Rabbit_Port":"5672",
           "Monitor_ID":"00000000-0000-0000-0000-000000000000",
           "Client_Name":"Social Analytics Dashboard"
        },
        "edm":{
           "enabled":"n",
           "dbURL":"localhost:5432",
           "dbName":"agent-edm-metrics",
           "dbUsername":"postgres",
           "dbPassword":"sofia",
           "dbType":"postgresql"
        }
     }
  }

**Get last plugin run**

REQUEST:

 .. code-block:: javascript

  URL: <basepath>/service/q/getlastrun
  Type: GET

RESPONSE (plugin run details as JSON):

 .. code-block:: javascript

  {
     "result":"ok",
     "response":{
        "name":"Job for plugin twitter-searcher",
        "id":"1",
        "description":"Job created by Scheduling service",
        "arguments":[
           {
              "search_terms":"football"
           },
           {
              "num_posts":"99"
           }
        ],
        "status":"finished",
        "created":"2013-07-25 13:14:16.142",
        "lastrun":"2013-07-25 13:14:20.136"
     }
  }


**Get last plugin run data**

REQUEST:

 .. code-block:: javascript

  URL: <basepath>/service/q/getlastrundata
  Type: GET

RESPONSE (data as JSON):

 .. code-block:: javascript

  {
    "result": "ok",
    "response": {
      "jsonData": [
          {
            "retweeted": false,
            ...
          },
        "collected": "2013-07-25 13:14:19.704"
        }
      ]
    }
  }


**Get last plugin run logs**

REQUEST:

 .. code-block:: javascript

  URL: <basepath>/service/q/getlastrunlogs
  Type: GET

RESPONSE (logs as JSON):

 .. code-block:: javascript

  {
    "result": "ok",
    "response": {
      "stdOut": "2013-07-25 13:14:17,347 DEBUG \t[PluginsHelper:80] Using command line arguments:
      ...
      "stdError": ""
    }
  }

**Run a plugin**

REQUEST:

 .. code-block:: javascript

  URL: <basepath>/service/q/run
  Type: POST
  ContentType: application/json; charset=utf-8
  Data: <plugin start configuration, use control panel to help create>

RESPONSE (new job details as JSON):

 .. code-block:: javascript

  {
    "result": "ok",
    "response": {
      "ID": 1,
      "PluginName": "twitter-searcher",
      "Name": "Job for plugin twitter-searcher",
      "Description": "Job created by Scheduling service",
      "Arguments": [
        {
          "search_terms": "football"
        },
        {
          "num_posts": "99"
        }
      ],
      "Inputs": [],
      "Outputs": [
        {
          "type": "twitter-static-search-raw"
        }
      ],
      "Schedule": {
        "times": 1,
        "withIntervalInMilliseconds": 20000
      },
      "Status": "scheduled",
      "WhenCreated_as_string": "2013-07-25 13:14:16",
      "WhenCreated_in_msec": 1374754456142,
      "WhenLastrun_as_string": "",
      "WhenLastrun_in_msec": "",
      "Executions_num": 0,
      "status_url": "http://localhost:8081/SAD/service/jobs/1",
      "data_url": "http://localhost:8081/SAD/service/jobs/1/data",
      "visualised_data_url": "http://localhost:8081/SAD/visualise/twitter-searcher/data.html?jobid=1&num_results=20"
    }
  }

Plugins
=======

**List plugins**

REQUEST:

 .. code-block:: javascript

  URL: <basepath>/service/plugins
  Type: GET


RESPONSE (all plugins with configurations as JSON):

 .. code-block:: javascript

  {
     "result":"ok",
     "response":{
        "basic-sns-stats":{
           "enabled":"y",
           "name":"basic-sns-stats",
           "description":"Basic Social Network posts analytics plugin. Extracts basics stats from collections of tweets or Facebook posts",
           "paths":{
              "jar":"target/basic-sns-stats-2.6.jar",
              "dependenciesFolder":"target/lib"
           },
           "arguments":[
              {
                 "name":"num_posts",
                 "description":"Number of social network posts per social network to analyse",
                 "isOptional":true,
                 "defaultValue":"all",
                 "sample_values":"all|125"
              }
           ],
           "inputs":[
              {
                 "source":"job_id",
                 "defaultValue":"1",
                 "required_data_types":[
                    {
                       "twitter-static-search-raw":"JSON Twitter posts as returned by Twitter API"
                    },
                    {
                       "facebook-posts-raw":"JSON Facebook posts as returned by Facebook API"
                    }
                 ]
              },
              {
                 "source":"plugin_name",
                 "defaultValue":"twitter-searcher",
                 "required_data_types":[
                    {
                       "twitter-static-search-raw":"JSON Twitter posts as returned by Twitter API"
                    },
                    {
                       "facebook-posts-raw":"JSON Facebook posts as returned by Facebook API"
                    }
                 ]
              }
           ],
           "outputs":{
              "data":[
                 {
                    "type":"twitter-basic-stats",
                    "description":"JSON containing basic stats extracted from processed tweets"
                 },
                 {
                    "type":"twitter-static-search-raw",
                    "description":"JSON containing tweets used in the analysis"
                 },
                 {
                    "type":"facebook-basic-stats",
                    "description":"JSON containing basic stats extracted from processed Facebook posts"
                 },
                 {
                    "type":"facebook-posts-raw",
                    "description":"JSON containing Facebook posts used in the analysis"
                 }
              ]
           },
           "pluginFolder":"basic-sns-stats"
        },
        "facebook-collector":{
           "enabled":"y",
           "name":"facebook-collector",
           "description":"Facebook collector EXPERIMEDIA SAD Plugin. Collects Facebook posts from groups and pages",
           "paths":{
              "jar":"target/facebook-collector-2.6.jar",
              "dependenciesFolder":"target/lib"
           },
           "arguments":[
              {
                 "name":"page_name",
                 "description":"Facebook page name to collect posts from, multiple instances of this argument are supported",
                 "isOptional":false,
                 "defaultValue":"congressschladming",
                 "sample_values":"HausAichGoessenberg|HauserKaiblingFan"
              },
              {
                 "name":"max_posts",
                 "description":"Number of posts to request from each page",
                 "isOptional":false,
                 "defaultValue":"500",
                 "sample_values":"100|1200"
              }
           ],
           "inputs":[

           ],
           "outputs":{
              "data":[
                 {
                    "type":"facebook-posts-raw",
                    "description":"JSON containing a Facebook post"
                 }
              ]
           },
           "pluginFolder":"facebook-collector"
        },
        "twitter-searcher":{
           "enabled":"y",
           "name":"twitter-searcher",
           "description":"Twitter searcher EXPERIMEDIA SAD Plugin. Performs static Twitter searches for a keyword or a hashtag",
           "paths":{
              "jar":"target/twitter-searcher-2.6.jar",
              "dependenciesFolder":"target/lib"
           },
           "arguments":[
              {
                 "name":"search_terms",
                 "description":"Keyword or hashtag to search Twitter for",
                 "isOptional":false,
                 "defaultValue":"football",
                 "sample_values":"Schladming OR Planai OR Dachstein OR Rohrmoos OR MidEurope"
              },
              {
                 "name":"num_posts",
                 "description":"Number of tweets to find",
                 "isOptional":true,
                 "defaultValue":"99",
                 "sample_values":"all|125"
              }
           ],
           "inputs":[

           ],
           "outputs":{
              "data":[
                 {
                    "type":"twitter-static-search-raw",
                    "description":"JSON containing all processed tweets"
                 }
              ]
           },
           "pluginFolder":"twitter-searcher"
        }
     }
  }


**Get plugin configuration**

REQUEST:

 .. code-block:: javascript

  URL: <basepath>/service/plugins/<plugin name>
  Type: GET


RESPONSE (<plugin name> configuration as JSON):

 .. code-block:: javascript

  {
    "result":"ok",
    "response":{
       "enabled":"y",
       "name":"basic-sns-stats",
       "description":"Basic Social Network posts analytics plugin. Extracts basics stats from collections of tweets or Facebook posts",
       "paths":{
          "jar":"target/basic-sns-stats-2.6.jar",
          "dependenciesFolder":"target/lib"
       },
       "arguments":[
          {
             "name":"num_posts",
             "description":"Number of social network posts per social network to analyse",
             "isOptional":true,
             "defaultValue":"all",
             "sample_values":"all|125"
          }
       ],
       "inputs":[
          {
             "source":"job_id",
             "defaultValue":"1",
             "required_data_types":[
                {
                   "twitter-static-search-raw":"JSON Twitter posts as returned by Twitter API"
                },
                {
                   "facebook-posts-raw":"JSON Facebook posts as returned by Facebook API"
                }
             ]
          },
          {
             "source":"plugin_name",
             "defaultValue":"twitter-searcher",
             "required_data_types":[
                {
                   "twitter-static-search-raw":"JSON Twitter posts as returned by Twitter API"
                },
                {
                   "facebook-posts-raw":"JSON Facebook posts as returned by Facebook API"
                }
             ]
          }
       ],
       "outputs":{
          "data":[
             {
                "type":"twitter-basic-stats",
                "description":"JSON containing basic stats extracted from processed tweets"
             },
             {
                "type":"twitter-static-search-raw",
                "description":"JSON containing tweets used in the analysis"
             },
             {
                "type":"facebook-basic-stats",
                "description":"JSON containing basic stats extracted from processed Facebook posts"
             },
             {
                "type":"facebook-posts-raw",
                "description":"JSON containing Facebook posts used in the analysis"
             }
          ]
       },
       "pluginFolder":"basic-sns-stats"
    }
  }

Jobs
====

**List jobs**

REQUEST:

 .. code-block:: javascript

  URL: <basepath>/service/jobs
  Type: GET


RESPONSE (all jobs as JSON):

 .. code-block:: javascript

  {
     "result":"ok",
     "response":{
        "num":1,
        "list":[
           {
              "ID":1,
              "PluginName":"twitter-searcher",
              "Name":"Job for plugin twitter-searcher",
              "Description":"Job created by Scheduling service",
              "Arguments":[
                 {
                    "search_terms":"football"
                 },
                 {
                    "num_posts":"99"
                 }
              ],
              "Inputs":[

              ],
              "Outputs":[
                 {
                    "type":"twitter-static-search-raw"
                 }
              ],
              "Schedule":{
                 "times":1,
                 "withIntervalInMilliseconds":20000
              },
              "Status":"finished",
              "WhenCreated_as_string":"2013-07-25 13:14:16",
              "WhenCreated_in_msec":1374754456142,
              "WhenLastrun_as_string":"2013-07-25 13:14:20",
              "WhenLastrun_in_msec":1374754460136,
              "Executions_num":1,
              "status_url":"http://localhost:8081/SAD/service/jobs/1",
              "data_url":"http://localhost:8081/SAD/service/jobs/1/data",
              "visualised_data_url":"http://localhost:8081/SAD/visualise/twitter-searcher/data.html?jobid=1&num_results=20"
           }
        ]
     }
  }


**Get job details**

REQUEST:

 .. code-block:: javascript

  URL: <basepath>/service/jobs/<job id>
  Type: GET


RESPONSE (details of job with <job id> as JSON):

 .. code-block:: javascript

  {
     "result":"ok",
     "response":{
        "job":{
           "ID":1,
           "PluginName":"twitter-searcher",
           "Name":"Job for plugin twitter-searcher",
           "Description":"Job created by Scheduling service",
           "Arguments":[
              {
                 "search_terms":"football"
              },
              {
                 "num_posts":"99"
              }
           ],
           "Inputs":[

           ],
           "Outputs":[
              {
                 "type":"twitter-static-search-raw"
              }
           ],
           "Schedule":{
              "times":1,
              "withIntervalInMilliseconds":20000
           },
           "Status":"finished",
           "WhenCreated_as_string":"2013-07-25 13:14:16",
           "WhenCreated_in_msec":1374754456142,
           "WhenLastrun_as_string":"2013-07-25 13:14:20",
           "WhenLastrun_in_msec":1374754460136,
           "Executions_num":1,
           "status_url":"http://localhost:8081/SAD/service/jobs/1",
           "data_url":"http://localhost:8081/SAD/service/jobs/1/data",
           "visualised_data_url":"http://localhost:8081/SAD/visualise/twitter-searcher/data.html?jobid=1&num_results=20"
        }
     }
  }


**Get job data**

REQUEST:

 .. code-block:: javascript

  URL: <basepath>/service/jobs/<job id>/data
  Type: GET


RESPONSE (data saved by job with <job id> in the database as JSON):

 .. code-block:: javascript

  {
    "response": {
      "series": {
        "0": {
          "jsonData": {
          "retweeted": false,
          ...
      },
        "type": "twitter-static-search-raw",
        "WhenCreated_as_string": "2013-07-25 13:14:19",
        "WhenCreated_in_msec": 1374754459704
      }
    },
      "num": 99,
      "jobstatus": "finished"
    }
  }

Executions
==========

**List all executions**

REQUEST:

 .. code-block:: javascript

  URL: <basepath>/service/executions
  Type: GET


RESPONSE (all executions as JSON):

 .. code-block:: javascript

  {
     "result":"ok",
     "response":{
        "num":1,
        "list":[
           {
              "DatabaseId":1,
              "ID":0,
              "SADJobID":1,
              "Description":"Execution by job [1]",
              "Status":"success",
              "WhenStarted_as_string":"2013-07-25 13:14:17.759",
              "WhenStarted_in_msec":1374754457759,
              "WhenFinished_as_string":"2013-07-25 13:14:20.125",
              "WhenFinished_in_msec":1374754460125
           }
        ]
     }
  }

**Get last 5 data entries for the last execution for plugin and of data type**


 .. code-block:: javascript

  URL: <basepath>/service/executions/{pluginName}/{dataType}
  Type: GET

RESPONSE (hot tweets as an example, JSON):

 .. code-block:: javascript

  {
     "result":"ok",
     "response":[
        {
           "id":"443563887288270848",
           "text":"168 days until football is back...\uD83C\uDFC8\uD83D\uDE4C",
           "created_at":"Wed Mar 12 01:47:26 +0000 2014",
           "lang":"en",
           "buzz_score":"0.8921",
           "uid":"1195754833",
           "name":"I Live For Football",
           "screen_name":"LiveToPlayFB",
           "followers_count":"24629",
           "friends_count":"8363"
        },
        {
           "id":"443602053600800769",
           "text":"Retweet if you play any\nBasketball\uD83C\uDFC0\nFootball\uD83C\uDFC8\nVolleyball\uD83D\uDE4C\nBaseball⚾\nHockey❄\nSoccer⚽\nWrestling\nSoftball\nCheer\uD83C\uDF89\nTrack\nLacrosse\nGolf",
           "created_at":"Wed Mar 12 04:19:05 +0000 2014",
           "lang":"en",
           "buzz_score":"0.6642",
           "uid":"2334082075",
           "name":"Doubted Athlete",
           "screen_name":"DoubtedAthlete",
           "followers_count":"1406",
           "friends_count":"2"
        },
        {
           "id":"443585944805834752",
           "text":"i feel like family, close friends, football, hockey, &amp; baseball teams should go in first, then the seniors. &amp; leftover spots to anyone else.",
           "created_at":"Wed Mar 12 03:15:05 +0000 2014",
           "lang":"en",
           "buzz_score":"0.7189",
           "uid":"280063004",
           "name":"danyell oosterveld ☯",
           "screen_name":"danyelllynnn",
           "followers_count":"176",
           "friends_count":"106"
        },
        {
           "id":"382397178033213440",
           "text":"Play your Man like a football and Watch another Girl catch him like a goal keeper..you will regret watching the replay..",
           "created_at":"Tue Sep 24 06:52:46 +0000 2013",
           "lang":"en",
           "buzz_score":"0.4181",
           "uid":"795042152",
           "name":"COMEDIAN IGODYE",
           "screen_name":"i_go_dye",
           "followers_count":"66062",
           "friends_count":"2"
        },
        {
           "id":"443686928639356928",
           "text":"And the winner is.. @JonWalmach for the Cruz Bday contest. Some real classics!! Pick up your shirt in the football office. #pvbearsfootball",
           "created_at":"Wed Mar 12 09:56:21 +0000 2014",
           "lang":"en",
           "buzz_score":"0.3264",
           "uid":"2312070447",
           "name":"PV Football",
           "screen_name":"football_PV",
           "followers_count":"147",
           "friends_count":"139"
        }
     ]
  }

Next steps
==========

:doc:`Plugin development <plugin_dev>`