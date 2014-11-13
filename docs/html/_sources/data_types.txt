``````````
Data Types
``````````

In SAD data types are used to identify different bits of data saved by the plugins so that that data can be visualised or easily processed by other plugins.

twitter-static-search-raw
=========================

Tweet as JSON in Twitter search results. Example (does not include all possible parameters):

.. code-block:: javascript

 {
   "contributors":null,
   "text":"2 Dudes at Dachstein - D-Stone seems like the place to be right now. Peep Lukas Brandauer &amp;amp; Roli Sharmer get D... http://t.co/yzXwEKJTf6",
   "geo":null,
   "retweeted":false,
   "in_reply_to_screen_name":null,
   "possibly_sensitive":false,
   "truncated":false,
   "lang":"en",
   "entities":{
      "symbols":[
      ],
      "urls":[
         {
            "expanded_url":"http://ow.ly/2yrlCX",
            "indices":[
               122,
               144
            ],
            "display_url":"ow.ly/2yrlCX",
            "url":"http://t.co/yzXwEKJTf6"
         }
      ],
      "hashtags":[
      ],
      "user_mentions":[
      ]
   },
   "in_reply_to_status_id_str":null,
   "id":357778831106510849,
   "source":"<a href=\"http://www.hootsuite.com\" rel=\"nofollow\">HootSuite</a>",
   "in_reply_to_user_id_str":null,
   "favorited":false,
   "in_reply_to_status_id":null,
   "retweet_count":0,
   "created_at":"Thu Jul 18 08:28:15 +0000 2013",
   "in_reply_to_user_id":null,
   "favorite_count":0,
   "id_str":"357778831106510849",
   "place":null,
   "user":{
      "location":"",
      "default_profile":false,
      "profile_background_tile":true,
      "statuses_count":3566,
      "lang":"en",
      "profile_link_color":"00B2D6",
      "id":21199176,
      "following":false,
      "protected":false,
      "favourites_count":1,
      "profile_text_color":"222222",
      "description":"Keep snowboarding real for over a decade....",
      "verified":false,
      "contributors_enabled":false,
      "profile_sidebar_border_color":"CCCCCC",
      "name":"METHODMAG",
      "profile_background_color":"EFEFEF",
      "created_at":"Wed Feb 18 14:18:12 +0000 2009",
      "default_profile_image":false,
      "followers_count":6154,
      "profile_image_url_https":"https://si0.twimg.com/profile_images/532638484/starstar_normal.jpg",
      "geo_enabled":false,
      "profile_background_image_url":"http://a0.twimg.com/profile_background_images/198136500/logo-bg.png",
      "profile_background_image_url_https":"https://si0.twimg.com/profile_background_images/198136500/logo-bg.png",
      "follow_request_sent":false,
      "entities":{
         "description":{
            "urls":[
            ]
         },
         "url":{
            "urls":[
               {
                  "expanded_url":"http://www.methodmag.com",
                  "indices":[
                     0,
                     22
                  ],
                  "display_url":"methodmag.com",
                  "url":"http://t.co/sxWnFNbFjh"
               }
            ]
         }
      },
      "url":"http://t.co/sxWnFNbFjh",
      "utc_offset":3600,
      "time_zone":"Vienna",
      "notifications":false,
      "profile_use_background_image":true,
      "friends_count":187,
      "profile_sidebar_fill_color":"CCCCCC",
      "screen_name":"methodmag",
      "id_str":"21199176",
      "profile_image_url":"http://a0.twimg.com/profile_images/532638484/starstar_normal.jpg",
      "listed_count":230,
      "is_translator":false
   },
   "coordinates":null,
   "metadata":{
      "result_type":"recent",
      "iso_language_code":"en"
   }
 }

facebook-posts-raw
==================

Facebook post as JSON in Facebook search results. Example (does not include all possible parameters):

.. code-block:: javascript

 {
   "icon":"http://static.ak.fbcdn.net/rsrc.php/v2/yz/r/StEh3RhPvjk.gif",
   "link":"http://www.facebook.com/photo.php?fbid=620595071293050&set=a.233615586657669.64346.206028732749688&type=1&relevant_count=1",
   "object_id":"620595071293050",
   "channelowner":"ENNSTALL_TV",
   "privacy":{
      "value":""
   },
   "from":{
      "id":"206028732749688",
      "category":"Tv channel",
      "name":"ENNSTAL TV"
   },
   "type":"photo",
   "updated_time":"2013-07-18T10:29:38+0000",
   "id":"206028732749688_620595077959716",
   "message":"Josef Bucher BZÖ Kameratechnisch begleitet von Ennstal TV - on tour  in der MFL Liezen ...",
   "picture":"http://photos-c.ak.fbcdn.net/hphotos-ak-ash4/1014159_620595071293050_441545934_s.jpg",
   "status_type":"added_photos",
   "created_time":"2013-07-18T10:29:38+0000"
 }


twitter-basic-stats
===================

Basic stats created from ``twitter-static-search-raw`` data by basis-sns-stats plugin v2.0-beta. Example:

.. code-block:: javascript

  {
    "unique_tweets": 99,
    "unique_users": 88,
    "unique_languages": 9,
    "unique_sources": 18
  }

facebook-basic-stats
====================

Basic stats created from ``facebook-posts-raw`` data by basis-sns-stats plugin v2.0-beta. Example:

.. code-block:: javascript

 {
   "unique_posts": 30,
   "unique_users": 2
 }

media-links-with-descriptions
=============================

Basic stats created from ``media-links-with-descriptions`` data by basis-sns-stats plugin v2.0. Example:

.. code-block:: javascript

 {
    "media_url": "http://pbs.twimg.com/media/BXa2oisCMAAG7nE.jpg"
    "text": "My favourite pair of football boots this year, the Nike Mercurial CR7 IX Galaxy Edition. http://t.co/beZsA2v0dR"
 }