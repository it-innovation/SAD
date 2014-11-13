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
//	Created Date :			2013-04-29
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.plugins.twittersearcher;

import gr.ntua.experimedia.socialintegrator.objects.Message;
import gr.ntua.experimedia.socialintegrator.objects.ObjectType;
import gr.ntua.experimedia.socialintegrator.util.SocialUtil;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.brickred.socialauth.AuthProvider;
import org.brickred.socialauth.SocialAuthConfig;
import org.brickred.socialauth.SocialAuthManager;
import org.brickred.socialauth.exception.AccessTokenExpireException;
import org.brickred.socialauth.exception.SocialAuthConfigurationException;
import org.brickred.socialauth.exception.SocialAuthException;
import org.brickred.socialauth.util.AccessGrant;
import org.brickred.socialauth.util.Constants;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.sad.service.helpers.PluginsHelper;

/**
 * Twitter searcher EXPERIMEDIA SAD Plugin.
 *
 */
public class Main {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void run(String[] args) {

        long startTime = System.currentTimeMillis();

        // Always get Plugins helper first
        PluginsHelper ph = new PluginsHelper(args);

        // Retrieve argument "search_terms"
        String search_terms = ph.getArgumentValue("search_terms");
        if (search_terms == null) {
            ph.dealWithException("ERROR: Failed to get argument value for 'search_terms'", null);
        }

        // Retrieve argument "num_posts"
        String num_posts = ph.getArgumentValue("num_posts");
        if (num_posts == null) {
            ph.dealWithException("ERROR: Failed to get argument value for 'num_posts'", null);
        }

        // Get last found tweet ID from metadata (to avoid getting duplicate tweets in multiple executions), can be null
        String since_id = ph.getMetadataValueForKey("since_id");

        logger.debug("Running with: search_terms='" + search_terms + "', num_posts='" + num_posts + "', since_id='" + since_id + "'");

        // Initialise Social Integrator & report SI call
        AuthProvider ap = initialiseSocialIntegratorForTwitter("oauth-twitter.properties", ph);
        ph.sendMetric(PluginEccClient.ENTITY_Twitter, PluginEccClient.ATTRIBUTE_REQUESTS_COUNT, "1");

        // Run Twitter search & report SI call & how long it took to search
        long startSearchingTwitterTime = System.currentTimeMillis();
        List<Message> tweets = searchTwitter(ap, search_terms, num_posts, since_id);
        ph.sendMetric(PluginEccClient.ENTITY_Twitter, PluginEccClient.ATTRIBUTE_REQUESTS_COUNT, "2");
        ph.sendMetric(PluginEccClient.ENTITY_Twitter, PluginEccClient.ATTRIBUTE_REQUEST_LATENCY, Long.toString(System.currentTimeMillis() - startSearchingTwitterTime));
        logger.debug("Found " + tweets.size() + " new tweets");

        // Create new entity for the search term
        String searchTermEntityName = "Entity for search terms '" + search_terms + "'";
        ph.addEntity(PluginEccClient.METRIC_GENERATOR_MAIN, searchTermEntityName, "Dynamic entity");

        // Create new attribute to record the number of tweets found per search term, report to ECC
        // If the attribute already exists, it will not be created
        String newAttribute = "Tweets for search term '" + search_terms + "'";
        ph.addAttribute(searchTermEntityName, newAttribute, "Tweets found for search terms '" + search_terms + "'", "ratio", "Tweets");
        ph.sendMetric(searchTermEntityName, newAttribute, Integer.toString(tweets.size()));

        // Report search terms and the number of tweets found to the ECC
        ph.sendMetric(PluginEccClient.ENTITY_SocialNetworksDataGroup, PluginEccClient.ATTRIBUTE_KEYWORDS, search_terms);
        ph.sendMetric(PluginEccClient.ENTITY_SocialNetworksDataGroup, PluginEccClient.ATTRIBUTE_TWEETS_FOUND, Integer.toString(tweets.size()));

        // Identify the number of unique users in collected tweets & send to ECC
        ArrayList<String> userIds = new ArrayList<>();
        String userId;
        for (Message tweet : tweets) {
            userId = tweet.getSenderId();
            if (!userIds.contains(userId)) {
                userIds.add(userId);
            }
        }
        ph.sendMetric(PluginEccClient.ENTITY_SocialNetworksDataGroup, PluginEccClient.ATTRIBUTE_USERS_FOUND, Integer.toString(userIds.size()));

        // Check requested output types
        ArrayList<String> requestedOutputTypes = ph.getRequestedOutputTypes();

        // Save tweets to the database if requested
        JSONObject tweetAsJson;
        if (requestedOutputTypes.contains("twitter-static-search-raw")) {
            Timestamp whenCollected = new Timestamp(System.currentTimeMillis());
            for (Message tweet : tweets) {
                tweetAsJson = tweet.getJson();
                try {
                    tweetAsJson.put("created_at_long", tweet.getModifyTime().getTime());
                } catch (JSONException ex) {
                    logger.error("Failed to save updated created_at for a tweet", ex);
                }
                ph.saveData("twitter-static-search-raw", tweetAsJson, whenCollected);
            }
        }

        // Save new sinceID
        if (tweets.size() > 0) {
            ph.putMetadataValueForKey("since_id", tweets.get(0).getId());
        }

        logger.debug("Found " + tweets.size() + " new tweets");

        // Report duration
        ph.sendMetric(PluginEccClient.ENTITY_PLUGINS, PluginEccClient.ATTRIBUTE_EXECUTION_DURATION, Long.toString(System.currentTimeMillis() - startTime));

        // Report success - failure reported automatically
        ph.reportExecutionSuccess();
        System.exit(0);

    }

    /**
     * Main method that gets executed by the service, leave as is.
     *
     * @param args list of arguments passed by the service.
     */
    public static void main(String[] args) {

        Main ts = new Main();

        // Normal plugin execution
        if (args[0].equals("execute")) {
            ts.run(args);

            // Start Twitter Searcher ECC client
        } else {
            ts.launchEccClient("Twitter searcher", args);
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
     * Searches Twitter using "oauth-twitter.properties" file credentials
     * (should be on classpath).
     *
     * @param search_terms what to search for.
     * @param num_posts number of post to return (up to 100 currently).
     * @param since_id only get tweets posted after the tweet with that ID
     * @return list of tweets.
     */
    private List<Message> searchTwitter(AuthProvider ap, String search_terms, String num_posts, String since_id) {

        // Configure Twitter search
        Map<String, String> parameters = new HashMap<>();
        parameters.put("locale", "en");
        parameters.put("result_type", "recent");
        parameters.put("count", num_posts);
        if (since_id != null) {
            parameters.put("since_id", since_id);
        }

        // Return search results
        return SocialUtil.search(ap, search_terms, ObjectType.STATUS, parameters, true).getList();
    }

    /**
     * Initializes Social integrator using a file on classpath with Twitter
     * OAuth credentials.
     *
     * @param oathCredentialsFilename name of the file containing properties:
     * oauth.consumerKey, oauth.consumerSecret, oauth.accessToken,
     * oauth.accessTokenSecret.
     * @return initialized AuthProvider required for SocialUtil searches.
     */
    private AuthProvider initialiseSocialIntegratorForTwitter(String oathCredentialsFilename, PluginsHelper ph) {

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(oathCredentialsFilename);

        Properties oauth_properties = new Properties();
        try {
            oauth_properties.load(inputStream);
        } catch (IOException ex) {
            ph.dealWithException("Failed to load properties from oauth file: '" + oathCredentialsFilename + "'", ex);
            throw new RuntimeException(ex); // to keep IDE happy
        }

        Properties si_properties = new Properties();
        si_properties.setProperty("twitter.com.consumer_key", oauth_properties.getProperty("oauth.consumerKey"));
        si_properties.setProperty("twitter.com.consumer_secret", oauth_properties.getProperty("oauth.consumerSecret"));

        SocialAuthConfig config = new SocialAuthConfig();
        try {
            config.setApplicationProperties(si_properties);
        } catch (Exception ex) {
            ph.dealWithException("Failed to set SocialAuthConfig properties", ex);
            throw new RuntimeException(ex); // to keep IDE happy
        }

        AccessGrant agrant = new AccessGrant();
        agrant.setKey(oauth_properties.getProperty("oauth.accessToken"));
        agrant.setSecret(oauth_properties.getProperty("oauth.accessTokenSecret"));
        agrant.setProviderId(Constants.TWITTER);

        SocialAuthManager socialAuthManager = new SocialAuthManager();
        try {
            socialAuthManager.setSocialAuthConfig(config);
        } catch (Exception ex) {
            ph.dealWithException("Failed to set SocialAuthConfig for SocialAuthManager", ex);
            throw new RuntimeException(ex); // to keep IDE happy
        }
        try {
            socialAuthManager.connect(agrant);
        } catch (SocialAuthConfigurationException | AccessTokenExpireException | SocialAuthException ex) {
            ph.dealWithException("Failed to connect to social network using AccessGrant provided", ex);
            throw new RuntimeException(ex); // to keep IDE happy
        }

        return socialAuthManager.getCurrentAuthProvider();
    }

}
