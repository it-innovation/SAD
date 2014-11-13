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
package uk.ac.soton.itinnovation.sad.plugins.facebookcollector;

import gr.ntua.experimedia.socialintegrator.objects.Message;
import gr.ntua.experimedia.socialintegrator.objects.ObjectType;
import gr.ntua.experimedia.socialintegrator.util.SocialUtil;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import net.sf.json.JSONObject;
import org.brickred.socialauth.AuthProvider;
import org.brickred.socialauth.SocialAuthConfig;
import org.brickred.socialauth.SocialAuthManager;
import org.brickred.socialauth.util.AccessGrant;
import org.brickred.socialauth.util.Constants;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.sad.service.helpers.PluginsHelper;

/**
 * Facebook collector EXPERIMEDIA SAD Plugin.
 *
 */
public class Main {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SimpleDateFormat dateFormatFacebook = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+SSSS");

    public void run(String[] args) {

        long startTime = System.currentTimeMillis();

        // Always get Plugins helper first
        PluginsHelper ph = new PluginsHelper(args);

        // Retrieve argument "search_terms"
        ArrayList<String> page_names = ph.getArgumentValues("page_name");
        if (page_names.isEmpty()) {
            ph.dealWithException("ERROR: Failed to get argument values for 'page_name'", null);
        }

        // Retrieve argument "num_posts"
        String max_posts = ph.getArgumentValue("max_posts");
        if (max_posts == null) {
            ph.dealWithException("ERROR: Failed to get argument value for 'max_posts'", null);
        }

        // Get last found post created_time per page name from metadata (to avoid getting duplicate posts in multiple executions), can be null
        String since_time = ph.getMetadataValueForKey("since_times");
        JSONObject since_times = new JSONObject();
        if (since_time != null) {
            since_times = JSONObject.fromObject(since_time);
        }

        logger.debug("Running with: page_name=[");
        for (String page : page_names) {
            logger.debug("\t- " + page);
        }
        logger.debug("], max_posts='" + max_posts + "', since_times='" + since_times.toString(2) + "'");

        // Initialise Social Integrator
        int requestsCounter = 1;
        AuthProvider ap = initialiseSocialIntegratorForFacebook("oauth-facebook.properties", ph);
        ph.sendMetric(PluginEccClient.ENTITY_Facebook, PluginEccClient.ATTRIBUTE_REQUESTS_COUNT, Integer.toString(requestsCounter));

        // Check requested output types
        ArrayList<String> requestedOutputTypes = ph.getRequestedOutputTypes();
        boolean ifSaveData = false;
        if (requestedOutputTypes.contains("facebook-posts-raw")) {
            ifSaveData = true;
        }

        // Search Facebook per page
        List<Message> collectedMessages;
        Timestamp whenCollected;
        Long new_since_time;
        int totalCollectedMessages = 0;
        long startSearchingFacebookTime;
        ArrayList<String> userIds = new ArrayList<>();
        for (String page_name : page_names) {
            startSearchingFacebookTime = System.currentTimeMillis();
            collectedMessages = collectFromFacebookPage(ap, page_name, max_posts, since_times);
            requestsCounter++;
            ph.sendMetric(PluginEccClient.ENTITY_Facebook, PluginEccClient.ATTRIBUTE_REQUESTS_COUNT, Integer.toString(requestsCounter));
            ph.sendMetric(PluginEccClient.ENTITY_Facebook, PluginEccClient.ATTRIBUTE_REQUEST_LATENCY, Long.toString(System.currentTimeMillis() - startSearchingFacebookTime));

            // Report the name of the page searched to ECC
            ph.sendMetric(PluginEccClient.ENTITY_SocialNetworksDataGroup, PluginEccClient.ATTRIBUTE_PAGES_SEARCHED, page_name);

            // Create new entity for the search term
            String pageEntityName = "Entity for Facebook page '" + page_name + "'";
            ph.addEntity(PluginEccClient.METRIC_GENERATOR_MAIN, pageEntityName, "Dynamic entity");

            // Create new attribute for the page searched in ECC
            // If the attribute already exists, it will not be created
            String newAttribute = "Messages from page '" + page_name + "'";
            ph.addAttribute(pageEntityName, newAttribute, "Number of messages collected from page '" + page_name + "'", "ratio", "Posts");

            if (collectedMessages != null) {
                whenCollected = new Timestamp(System.currentTimeMillis());
                totalCollectedMessages += collectedMessages.size();

                // Report the number of messages collected from this page to ECC
                ph.sendMetric(pageEntityName, newAttribute, Integer.toString(collectedMessages.size()));

                String userId;
                for (Message tempPost : collectedMessages) {
                    userId = tempPost.getSenderId();
                    if (!userIds.contains(userId)) {
                        userIds.add(userId);
                    }
                }

                // Save found posts to the database if requested
                if (ifSaveData) {
                    JSONObject reportAsJson, fbUserAsJson, fbCommentAsJson, postFbCommentAsJson, fbPageAsJson;
                    for (Message message : collectedMessages) {
                        if (!message.getJson().isNull("message")) {
                            logger.debug("Saving post [" + message.getId() + "]: " + message.getMessage());
                            ph.saveData("facebook-posts-raw", message.getJson(), whenCollected);

                            // report prov
                            reportAsJson = new JSONObject();
                            fbUserAsJson = new JSONObject();
                            fbCommentAsJson = new JSONObject();
                            postFbCommentAsJson = new JSONObject();
                            fbPageAsJson = new JSONObject();

                            fbUserAsJson.put("id", message.getSenderId());
                            try {
                                fbUserAsJson.put("name", message.getJson().getJSONObject("from").getString("name"));
                            } catch (Throwable ex) {
                                fbUserAsJson.put("name", "N/A");
                            }

                            fbCommentAsJson.put("id", message.getId());
                            fbCommentAsJson.put("name", "comment");

                            postFbCommentAsJson.put("name", "postComment");
                            postFbCommentAsJson.put("timestamp", message.getModifyTime().getTime() / 1000);

                            fbPageAsJson.put("name", page_name);

                            reportAsJson.put("fbUserAsJson", fbUserAsJson);
                            reportAsJson.put("fbCommentAsJson", fbCommentAsJson);
                            reportAsJson.put("postFbCommentAsJson", postFbCommentAsJson);
                            reportAsJson.put("fbPageAsJson", fbPageAsJson);
                            ph.reportProv(message.getSenderId() == null ? "n/a" : message.getSenderId(), reportAsJson.toString());
                        }
                    }
                }
                logger.debug("Collected " + collectedMessages.size() + " posts from page '" + page_name + "'");

                // Save new since_time for the page
                try {
                    if (collectedMessages.size() > 0) {
                        new_since_time = dateFormatFacebook.parse(collectedMessages.get(0).getJson().getString("created_time")).getTime() / 1000L;
                        logger.debug("Setting new since_time '" + new_since_time + "' for page '" + page_name + "'");
                        if (since_times.containsKey(page_name)) {
                            since_times.remove(page_name);
                        }
                        since_times.put(page_name, new_since_time);
                    }
                } catch (JSONException | ParseException ex) {
                    logger.error("Failed to update since_time for page '" + page_name + "'", ex);
                }
            } else {

                // If nothing was collected, report 0 messages for this page to ECC
                ph.sendMetric(pageEntityName, newAttribute, "0");
                logger.error("Failed to collect posts from page '" + page_name + "'");
            }
        }

        // Report total number of Facebook posts and users collected to ECC
        ph.sendMetric(PluginEccClient.ENTITY_SocialNetworksDataGroup, PluginEccClient.ATTRIBUTE_MESSAGES_FOUND, Integer.toString(totalCollectedMessages));
        ph.sendMetric(PluginEccClient.ENTITY_SocialNetworksDataGroup, PluginEccClient.ATTRIBUTE_USERS_FOUND, Integer.toString(userIds.size()));

        // Save new since_times
        if (!since_times.isEmpty()) {
            ph.putMetadataValueForKey("since_times", since_times.toString());
        }

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
        Main fc = new Main();

        // Normal plugin execution
        if (args[0].equals("execute")) {
            fc.run(args);

            // Start Facebook collector ECC client
        } else {
            fc.launchEccClient("Facebook collector", args);
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
     * Searches Facebook using "oauth-facebook.properties" file credentials
     * (should be on classpath).
     *
     * @param page_name Facebook page/group name or ID.
     * @param num_posts number of post to return from the page.
     * @param since_times only get posts posted after the post with last saved
     * time.
     * @return list of Facebook posts.
     */
    private List<Message> collectFromFacebookPage(AuthProvider ap, String page_name, String num_posts, JSONObject since_times) {

        String sincePostString = "", call_url, page_since_id;
        logger.debug("Fetching posts from page '" + page_name + "'");

        // Configure Facebook collection
        if (since_times != null) {
            if (since_times.containsKey(page_name)) {
                page_since_id = since_times.getString(page_name);
                logger.debug("Found since ID [" + page_since_id + "] for page '" + page_name + "'");
                sincePostString = "&since=" + page_since_id;
            }
        }
        call_url = "https://graph.facebook.com/" + page_name + "/feed?message=utf8&limit=" + num_posts + sincePostString;
        logger.debug("Fetching Facebook posts using URL: " + call_url);

        // Return search results
        return SocialUtil.getMessagesFromUrl(ap, call_url, ObjectType.UNDEFINED, true).getList();
    }

    /**
     * Initializes Social integrator using a file on classpath with Facebook
     * OAuth credentials.
     *
     * @param oathCredentialsFilename name of the file containing properties:
     * graph.facebook.com.consumer_key, graph.facebook.com.consumer_secret.
     * @return initialized AuthProvider required for SocialUtil searches.
     */
    private AuthProvider initialiseSocialIntegratorForFacebook(String oathCredentialsFilename, PluginsHelper ph) {

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(oathCredentialsFilename);

        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException ex) {
            ph.dealWithException("Failed to load properties from oauth file: '" + oathCredentialsFilename + "'", ex);
            throw new RuntimeException(ex); // to keep IDE happy
        }

        SocialAuthConfig config = new SocialAuthConfig();
        try {
            config.setApplicationProperties(properties);
        } catch (Exception ex) {
            ph.dealWithException("Failed to set SocialAuthConfig properties", ex);
            throw new RuntimeException(ex); // to keep IDE happy
        }

        SocialAuthManager socialAuthManager = new SocialAuthManager();
        try {
            socialAuthManager.setSocialAuthConfig(config);
        } catch (Exception ex) {
            ph.dealWithException("Failed to set SocialAuthConfig for SocialAuthManager", ex);
            throw new RuntimeException(ex); // to keep IDE happy
        }

        String token = SocialUtil.getAppTokenFacebook(properties.getProperty("graph.facebook.com.consumer_key"), properties.getProperty("graph.facebook.com.consumer_secret"));

        AccessGrant agrant = new AccessGrant();
        agrant.setKey(token);
        agrant.setProviderId(Constants.FACEBOOK);
        try {
            socialAuthManager.connect(agrant);
        } catch (Exception ex) {
            ph.dealWithException("Failed to connect to social network using AccessGrant provided", ex);
            throw new RuntimeException(ex); // to keep IDE happy
        }

        // Set time zone
        dateFormatFacebook.setTimeZone(TimeZone.getTimeZone("GMT")); // Facebook is always GMT

        return socialAuthManager.getCurrentAuthProvider();
    }
}
