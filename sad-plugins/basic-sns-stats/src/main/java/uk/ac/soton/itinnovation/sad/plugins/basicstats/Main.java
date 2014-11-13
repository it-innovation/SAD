/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2013
//
// Copyright in this library belongs to the University of Southampton
// IT Innovation Centre of Gamma House, Enterprise Road,
// Chilworth Science Park, Southampton, SO16 7NS, UK.
//
// This software may not be used, sold, licensed, transferred, copied
// or reproduced in whole or in part in any manner or form or in or
// on any media by any person other than in accordance with the terms
// of the Licence Agreement supplied with the software, or otherwise
// without the prior written consent of the copyright owners.
//
// This software is distributed WITHOUT ANY WARRANTY, without even the
// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
// PURPOSE, except where stated in the Licence Agreement supplied with
// the software.
//
//	Created By :			Maxim Bashevoy
//	Created Date :			2013-05-22
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.plugins.basicstats;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import java.sql.Timestamp;
import java.util.ArrayList;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.sad.service.helpers.PluginsHelper;

/**
 * Basic Social Network posts analytics plugin.
 *
 */
public class Main {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ArrayList<String> tweetIdsHolder = new ArrayList<>();
    private ArrayList<String> fbPostsIdsHolder = new ArrayList<>();

    public void run(String[] args) {

        long startTime = System.currentTimeMillis();

        // Always get Plugins helper first
        PluginsHelper ph = new PluginsHelper(args);

        // Retrieve argument "num_posts"
        String num_posts = ph.getArgumentValue("num_posts");
        if (num_posts == null) {
            ph.dealWithException("ERROR: Failed to get argument value for 'num_posts'", null);
        }

        logger.debug("Running with: num_posts='" + num_posts);

        // Get Twitter input data
        ArrayList<JSONObject> twitterInputData = ph.getInputData("twitter-static-search-raw", num_posts);

        // Report the number of tweets submitted to ECC
        ph.sendMetric(PluginEccClient.ENTITY_SocialNetworksDataGroup, PluginEccClient.ATTRIBUTE_TWEETS_ANALYSED, Integer.toString(twitterInputData.size()));

        // Check requested output types
        ArrayList<String> requestedOutputTypes = ph.getRequestedOutputTypes();

        if (twitterInputData.isEmpty()) {
            logger.debug("No Twitter input data found (of type 'twitter-static-search-raw')");
        } else {

            // Do Twitter analysis
            int[] analysisResults = doTwitterAnalysis(twitterInputData);

            // Save data as requested
            JSONObject dataAsJson;
            Timestamp whenCreated = new Timestamp(System.currentTimeMillis());
            if (requestedOutputTypes.contains("twitter-basic-stats")) {
                logger.debug("Saving data of type 'twitter-basic-stats'");
                dataAsJson = new JSONObject();
                dataAsJson.put("unique_tweets", analysisResults[0]);
                dataAsJson.put("unique_users", analysisResults[1]);
                dataAsJson.put("unique_languages", analysisResults[2]);
                dataAsJson.put("unique_sources", analysisResults[3]);
                ph.saveData("twitter-basic-stats", dataAsJson, whenCreated);
            }
            if (requestedOutputTypes.contains("twitter-static-search-raw")) {
                logger.debug("Saving data of type 'twitter-static-search-raw'");
                whenCreated = new Timestamp(System.currentTimeMillis());
                for (JSONObject tweet : twitterInputData) {
                    if (tweetIdsHolder.contains(tweet.getString("id"))) {
                        ph.saveData("twitter-static-search-raw", tweet, whenCreated);
                    }
                }
            }
        }

        // Get Facebook input data
        ArrayList<JSONObject> facebookInputData = ph.getInputData("facebook-posts-raw", num_posts);

        // Report the number of Facebook messages submitted to ECC
        ph.sendMetric(PluginEccClient.ENTITY_SocialNetworksDataGroup, PluginEccClient.ATTRIBUTE_MESSAGES_ANALYSED, Integer.toString(facebookInputData.size()));

        if (facebookInputData.isEmpty()) {
            logger.debug("No Facebook input data found (of type 'facebook-posts-raw')");
        } else {

            // Do Facebook analysis
            int[] analysisResults = doFacebookAnalysis(facebookInputData);

            // Save data as requested
            JSONObject dataAsJson;
            Timestamp whenCreated = new Timestamp(System.currentTimeMillis());
            if (requestedOutputTypes.contains("facebook-basic-stats")) {
                logger.debug("Saving data of type 'facebook-basic-stats'");
                dataAsJson = new JSONObject();
                dataAsJson.put("unique_posts", analysisResults[0]);
                dataAsJson.put("unique_users", analysisResults[1]);
                ph.saveData("facebook-basic-stats", dataAsJson, whenCreated);
            }
            if (requestedOutputTypes.contains("facebook-posts-raw")) {
                logger.debug("Saving data of type 'facebook-posts-raw'");
                whenCreated = new Timestamp(System.currentTimeMillis());
                for (JSONObject post : facebookInputData) {
                    if (fbPostsIdsHolder.contains(post.getString("id"))) {
                        ph.saveData("facebook-posts-raw", post, whenCreated);
                    }
                }
            }
        }

        // Searching database for various things
        DBCollection collection = ph.getDataCollection(); // database collection with all the data
        BasicDBObject inputDataQuery = ph.getInputDataQuery(); // query that limits data to input jobs/plugins
        logger.debug("Total posts in input: " + collection.find(inputDataQuery).count());

        // Count number of tweets in input data
        BasicDBObject tweetsQuery = (BasicDBObject) inputDataQuery.copy();
        tweetsQuery.append("dataType", "twitter-static-search-raw"); // initial query expanded to limit data by dataType
        logger.debug("Tweets: " + collection.find(tweetsQuery).count());

        // Count number of Facebook posts in input data
        BasicDBObject fbPostsQuery = (BasicDBObject) inputDataQuery.copy();
        fbPostsQuery.append("dataType", "facebook-posts-raw");
        logger.debug("FB posts: " + collection.find(fbPostsQuery).count());

        // Count number of tweets with 'lang' = 'en' (Facebook posts don't have that field) in input data
        BasicDBObject englishTweetsQuery = (BasicDBObject) inputDataQuery.copy();
        englishTweetsQuery.append("jsonData.lang", "en");
        logger.debug("Tweets in English: " + collection.find(englishTweetsQuery).count());

        // Find media files in tweets and save URLs with text
        tweetsQuery.append("jsonData.entities.media", new BasicDBObject("$exists", true));
        BasicDBObject keysToReturn = new BasicDBObject();
        keysToReturn.append("jsonData.text", 1);
        keysToReturn.append("jsonData.entities.media.media_url", 1);

        int mlFilesCounter = 0;
        DBCursor cursor = collection.find(tweetsQuery, keysToReturn);
        JSONObject next;
        JSONObject dataAsJson;
        String text, media_url;
        Timestamp whenCreated = new Timestamp(System.currentTimeMillis());
        while (cursor.hasNext()) {
            next = JSONObject.fromObject(cursor.next().toString());
            text = next.getJSONObject("jsonData").getString("text");
            media_url = next.getJSONObject("jsonData").getJSONObject("entities").getJSONArray("media").getJSONObject(0).getString("media_url");
            logger.debug(media_url + " (" + text + ")");
            if (requestedOutputTypes.contains("media-links-with-descriptions")) {
                logger.debug("Saving data of type 'media-links-with-descriptions'");
                dataAsJson = new JSONObject();
                dataAsJson.put("text", text);
                dataAsJson.put("media_url", media_url);
                ph.saveData("media-links-with-descriptions", dataAsJson, whenCreated);
                mlFilesCounter++;
            }
        }

        // Find media files in Facebook posts and save URLs with text
        fbPostsQuery.append("jsonData.picture", new BasicDBObject("$exists", true));
        keysToReturn = new BasicDBObject();
        keysToReturn.append("jsonData.message", 1);
        keysToReturn.append("jsonData.story", 1);
        keysToReturn.append("jsonData.name", 1);
        keysToReturn.append("jsonData.picture", 1);

        whenCreated = new Timestamp(System.currentTimeMillis());
        cursor = collection.find(fbPostsQuery, keysToReturn);
        while (cursor.hasNext()) {
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
            if (requestedOutputTypes.contains("media-links-with-descriptions")) {
                logger.debug("Saving data of type 'media-links-with-descriptions'");
                dataAsJson = new JSONObject();
                dataAsJson.put("text", text);
                dataAsJson.put("media_url", media_url);
                ph.saveData("media-links-with-descriptions", dataAsJson, whenCreated);
                mlFilesCounter++;
            }
        }

        // Report number of files with media links
        ph.sendMetric(PluginEccClient.ENTITY_SocialNetworksDataGroup, PluginEccClient.ATTRIBUTE_MEDIA_LINKS, Integer.toString(mlFilesCounter));

        // Create new attribute to record requested output data types, report to ECC
        // If the attribute already exists, it will not be created
        String newAttribute = "Requested output data types";
        ph.addAttribute(PluginEccClient.ENTITY_PLUGINS, newAttribute, "Output data types", "nominal", "Data type");
        for (String dataType : requestedOutputTypes) {
            ph.sendMetric(PluginEccClient.ENTITY_PLUGINS, newAttribute, dataType);
        }

        // Report duration
        ph.sendMetric(PluginEccClient.ENTITY_PLUGINS, PluginEccClient.ATTRIBUTE_EXECUTION_DURATION, Long.toString(System.currentTimeMillis() - startTime));

        // Report success - failure reported automatically
        ph.reportExecutionSuccess();
        System.exit(0);
    }

    /**
     * Main method that gets executed by the service.
     *
     * @param args list of arguments passed by the service.
     */
    public static void main(String[] args) {
        Main bs = new Main();

        // Normal plugin execution
        if (args[0].equals("execute")) {
            bs.run(args);

            // Start Basic analytics ECC client
        } else {
            bs.launchEccClient("Basic analytics", args);
        }
    }

    /**
     * Launches custom version of ECC client just for this plugin.
     *
     * @param name of the ECC client to be displayed in ECC Dashboard.
     * @param args command line arguments passed to the plugin.
     */
    private void launchEccClient(String name, String[] args) {
        PluginEccClient eccClient = new PluginEccClient(name, args);
    }

    /**
     * Creates basic Twitter stats from a list of raw tweets.
     *
     * @param twitterInputData list of tweets as JSON.
     * @return int array of four values: unique_tweets, unique_users,
     * unique_languages, unique_sources.
     */
    private int[] doTwitterAnalysis(ArrayList<JSONObject> twitterInputData) {
        int[] result = new int[4];

        ArrayList<String> twitterUsersIdsHolder = new ArrayList<>();
        ArrayList<String> twitterLanguagesHolder = new ArrayList<>();
        ArrayList<String> twitterSourcesHolder = new ArrayList<>();

        String tweet_id, user_id, tweet_language, tweet_source;
        JSONObject tweet_user;
        for (JSONObject tweet : twitterInputData) {
            tweet_id = tweet.getString("id");

            if (!tweetIdsHolder.contains(tweet_id)) {
                logger.debug("Processing new tweet [" + tweet_id + "]");
                tweetIdsHolder.add(tweet_id);

                tweet_user = tweet.getJSONObject("user");
                user_id = tweet_user.getString("id");

                if (!twitterUsersIdsHolder.contains(user_id)) {
                    logger.debug("\tFound new user [" + user_id + "]");
                    twitterUsersIdsHolder.add(user_id);
                }

                if (tweet.containsKey("lang")) {
                    tweet_language = tweet.getString("lang");
                    if (tweet_language != null) {
                        if (!twitterLanguagesHolder.contains(tweet_language)) {
                            logger.debug("\tFound new language [" + tweet_language + "]");
                            twitterLanguagesHolder.add(tweet_language);
                        }
                    }
                }

                if (tweet.containsKey("source")) {
                    tweet_source = tweet.getString("source");
                    if (tweet_source != null) {
                        if (!twitterSourcesHolder.contains(tweet_source)) {
                            logger.debug("\tFound new source [" + tweet_source + "]");
                            twitterSourcesHolder.add(tweet_source);
                        }
                    }
                }
            } else {
                logger.debug("SKIPPING tweet [" + tweet_id + "] as already processed");
            }
        }

        result[0] = tweetIdsHolder.size();
        result[1] = twitterUsersIdsHolder.size();
        result[2] = twitterLanguagesHolder.size();
        result[3] = twitterSourcesHolder.size();

        return result;

    }

    /**
     * Creates basic stats from a list of Facebook posts.
     *
     * @param facebookInputData list of Facebook posts as JSON.
     * @return int array of two values: unique_posts, unique_users.
     */
    private int[] doFacebookAnalysis(ArrayList<JSONObject> facebookInputData) {
        int[] result = new int[2];

        ArrayList<String> facebookUsersIdsHolder = new ArrayList<>();

        String post_id, user_id;
        JSONObject facebook_user;
        for (JSONObject tweet : facebookInputData) {
            post_id = tweet.getString("id");

            if (!fbPostsIdsHolder.contains(post_id)) {
                logger.debug("Processing new Facebook post [" + post_id + "]");
                fbPostsIdsHolder.add(post_id);

                facebook_user = tweet.getJSONObject("from");
                user_id = facebook_user.getString("id");

                if (!facebookUsersIdsHolder.contains(user_id)) {
                    logger.debug("\tFound new Facebook user [" + user_id + "]");
                    facebookUsersIdsHolder.add(user_id);
                }

            } else {
                logger.debug("SKIPPING Facebook post [" + post_id + "] as already processed");
            }
        }

        result[0] = fbPostsIdsHolder.size();
        result[1] = facebookUsersIdsHolder.size();

        return result;

    }

}
