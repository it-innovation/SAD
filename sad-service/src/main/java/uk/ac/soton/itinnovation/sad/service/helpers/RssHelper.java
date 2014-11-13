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
//	Created Date :			2013-07-02
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.helpers;

import com.jamesmurty.utils.XMLBuilder;
import uk.ac.soton.itinnovation.sad.service.dao.SADJobData;
import uk.ac.soton.itinnovation.sad.service.services.SchedulingService;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Coordinates RSS feeds.
 */
//@Service
public class RssHelper {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String JOB_RSS_LINK_NAME = "rss_link_job";
    public static final String JOB_CUSTOM_RSS_LINK_NAME = "rss_link_job_custom";
    public static final String PLUGIN_RSS_LINK_NAME = "rss_link_plugin";
    public static final String PLUGIN_CUSTOM_RSS_LINK_NAME = "rss_link_plugin_custom";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
    private final SimpleDateFormat dateFormatCustomRss = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
    private final SimpleDateFormat dateFormatFacebook = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+SSSS");

    private Properties outputProperties, outputPropertiesWithDeclaration, outputPropertiesWithoutDeclaration;
    private XMLBuilder builder, item;
    private String basePath;

    @Autowired
    @Qualifier("schedulingService")
    SchedulingService schedulingService;

//    @Autowired
//    @Qualifier("propertiesService")
//    PropertiesService propertiesService;
    @PostConstruct
    public void init() {
        outputProperties = new Properties();
        outputProperties.put(javax.xml.transform.OutputKeys.INDENT, "yes");
        outputProperties.put("{http://xml.apache.org/xslt}indent-amount", "2");

        outputPropertiesWithDeclaration = outputProperties;
        outputPropertiesWithDeclaration.put(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "no");

        outputPropertiesWithoutDeclaration = outputProperties;
        outputPropertiesWithDeclaration.put(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");

//        basePath = propertiesService.getServiceBasePath();
        basePath = "asd";
    }

    /**
     * Returns job data as custom RSS (Digital Schladming).
     *
     * @param jobId database ID of the job
     * @param num number of data items to return
     * @param type only return data of that type
     * @param keywordsForTaggingAsString words to use for tagging in query_word
     * @return RSS stream as java String
     * @throws ParserConfigurationException
     * @throws TransformerException
     * @throws ParseException
     */
    public String customRssForJob(int jobId, int num, String type, String keywordsForTaggingAsString) throws ParserConfigurationException, TransformerException, ParseException {

        ArrayList<SADJobData> jobData = schedulingService.getDataForJobWithId(Integer.toString(jobId), num, type);

        logger.debug("Building custom RSS feed for job [" + jobId + "] using " + jobData.size() + " data entries");

        return sadDataToCustomRss(jobData, type, keywordsForTaggingAsString, getCustomRssLinkForJob(Integer.toString(jobId), num, type, keywordsForTaggingAsString));
    }

    /**
     * Returns plugin data as custom RSS (Digital Schladming).
     *
     * @param pluginName name of the plugin
     * @param num number of data items to return
     * @param type only return data of that type
     * @param keywordsForTaggingAsString words to use for tagging in query_word
     * @return RSS stream as java String
     * @throws ParserConfigurationException
     * @throws TransformerException
     * @throws ParseException
     */
    public String customRssForPlugin(String pluginName, int num, String type, String keywordsForTaggingAsString) throws ParserConfigurationException, TransformerException, ParseException {

        ArrayList<SADJobData> pluginData = schedulingService.getDataForPluginWithName(pluginName, num, type);

        logger.debug("Building custom RSS feed for plugin '" + pluginName + "' using " + pluginData.size() + " data entries");

        return sadDataToCustomRss(pluginData, type, keywordsForTaggingAsString, getCustomRssLinkForPlugin(pluginName, num, type, keywordsForTaggingAsString));
    }

    /**
     * Converts SAD job data in the database to a custom RSS feed for Digital
     * Schladming experiment.
     *
     * @param jobData data entries.
     * @param type data type.
     * @param keywordsForTaggingAsString keywords for tagging.
     * @param href RSS href parameter value.
     * @return custom RSS feed for Digital Schladming experiment
     * @throws ParserConfigurationException
     * @throws ParseException
     * @throws TransformerException
     */
    public String sadDataToCustomRss(ArrayList<SADJobData> jobData, String type, String keywordsForTaggingAsString, String href) throws ParserConfigurationException, ParseException, TransformerException {

        if (type.equalsIgnoreCase("twitter-hot-tweets")) {
            return sadDataToSchladmingTwitterRss(jobData, keywordsForTaggingAsString, href);
        } else if (type.equalsIgnoreCase("facebook-posts-topics-tagged")) {
            return sadDataToSchladmingFacebookRss(jobData);
        } else {
            if (jobData.isEmpty()) {
                builder = XMLBuilder.create("rss")
                        .a("version", "2.0")
                        .a("xmlns:content", "http://purl.org/rss/1.0/modules/content/")
                        .a("xmlns:wfw", "http://wellformedweb.org/CommentAPI/")
                        .e("channel")
                        .e("title").t("Empty XML EXPERIMEDIA Feed").up()
                        .e("link").t("http://onmeedia.com/feeds/").up()
                        .e("updated").t(dateFormatFacebook.format(new Date())).up()
                        .e("author").e("name").t("http://onmeedia.com/feeds/");

                return builder.asString(outputProperties);

            } else {
                if (jobData.get(0).getDataType().equalsIgnoreCase("twitter-hot-tweets")) {
                    logger.warn("Custom RSS type autodetected (based on first data entry type) to be 'twitter-hot-tweets'. All other data entries will be ignored");
                    return sadDataToSchladmingTwitterRss(jobData, keywordsForTaggingAsString, href);
                } else if (jobData.get(0).getDataType().equalsIgnoreCase("facebook-posts-topics-tagged")) {
                    logger.warn("Custom RSS type autodetected (based on first data entry type) to be 'facebook-posts-topics-tagged'. All other data entries will be ignored");
                    return sadDataToSchladmingFacebookRss(jobData);
                } else {
                    builder = XMLBuilder.create("rss")
                            .a("version", "2.0")
                            .a("xmlns:content", "http://purl.org/rss/1.0/modules/content/")
                            .a("xmlns:wfw", "http://wellformedweb.org/CommentAPI/")
                            .e("channel")
                            .e("title").t("Unknown data type XML EXPERIMEDIA Feed").up()
                            .e("link").t("http://onmeedia.com/feeds/").up()
                            .e("updated").t(dateFormatFacebook.format(new Date())).up()
                            .e("author").e("name").t("http://onmeedia.com/feeds/");

                    return builder.asString(outputProperties);
                }
            }
        }
    }

    /**
     * Creates custom Twitter RSS feed for Digital Schladming experiment.
     *
     * @param data data entries.
     * @param keywordsForTaggingAsString keywords for tagging.
     * @param href RSS href parameter value.
     * @return custom Twitter RSS feed for Digital Schladming experiment.
     * @throws ParserConfigurationException
     * @throws ParseException
     * @throws TransformerException
     */
    public String sadDataToSchladmingTwitterRss(ArrayList<SADJobData> data, String keywordsForTaggingAsString, String href) throws ParserConfigurationException, ParseException, TransformerException {

        String[] keywordsForTagging = keywordsForTaggingAsString.split(",");

        builder = XMLBuilder.create("rss")
                .a("version", "2.0")
                .a("xmlns:atom", "http://www.w3.org/2005/Atom")
                .e("channel")
                .e("atom:link")
                .a("href", href)
                .a("rel", "self")
                .a("type", "application/rss+xml")
                .up()
                .e("lastBuildDate").t(dateFormatCustomRss.format(new Date(data.get(0).getWhenCollected().getTime()))).up()
                .e("language").t("en-gb").up()
                .e("title").t("Hot Twitter topics for keywords: " + keywordsForTaggingAsString.replaceAll(",", ", ")).up()
                .e("link").t("http://www.twitter.com/").up()
                .e("ttl").t("960").up()
                .e("generator").t("EXPERIMEDIA SAD").up()
                .e("category").t("Personal").up()
                .e("image")
                .e("title").t("EXPERIMEDIA Project").up()
                .e("link").t("https://twitter.com/ictexperimedia").up()
                .e("url").t("https://si0.twimg.com/profile_images/1630441075/logo_square_twitter.png")
                .up(2);

        JSONObject rawJsonData, user;
        String description, expandedText, query_word, photolink, urllink, hashtaglist;

        for (SADJobData dataEntry : data) {
            if (dataEntry.getDataType().equalsIgnoreCase("twitter-hot-tweets")) {
                rawJsonData = JSONObject.fromObject(dataEntry.getJsonData());
                user = rawJsonData.getJSONObject("user");
                item = builder.e("item");
                query_word = "";
                hashtaglist = "";

                item.e("title").t(rawJsonData.getString("text")).up();
                item.e("channel").t("Twitter").up();
                item.e("pubDate").t(dateFormatCustomRss.format(dateFormat.parse(rawJsonData.getString("created_at")))).up();
                item.e("link").t("https://twitter.com/" + user.getString("screen_name") + "/statuses/" + rawJsonData.getString("id")).up();
                item.e("guid").a("isPermaLink", "false")
                        .t(rawJsonData.getString("id")).up();

                description = "\n<![CDATA[\n";
                description += "\t<div style='float:left;margin: 0 6px 6px 0;'>\n";
                description += "\t\t<a href='https://twitter.com/" + user.getString("screen_name") + "/statuses/" + rawJsonData.getString("id") + "' border=0 target='blank'>\n";
                description += "\t\t\t<img src='" + user.getString("profile_image_url") + "' border=0>\n";
                description += "\t\t</a>\n";
                description += "\t</div>\n";
                description += "\t<strong>" + user.getString("screen_name") + "</strong> <a href='https://twitter.com/"
                        + user.getString("screen_name") + "' target='_blank'>@" + user.getString("screen_name") + "</a><br />\n";

                expandedText = rawJsonData.getString("text");

                photolink = user.getString("profile_image_url").replaceAll("_normal", "");
                urllink = user.getString("profile_image_url");
                if (rawJsonData.containsKey("entities")) {
                    if (rawJsonData.getJSONObject("entities").containsKey("urls")) {
                        JSONArray urls = rawJsonData.getJSONObject("entities").getJSONArray("urls");

                        JSONObject url;
                        String tempUrl, expandedUrl;
                        for (int i = 0; i < urls.size(); i++) {
                            url = urls.getJSONObject(i);

                            if (url.containsKey("url")) {
                                tempUrl = url.getString("url");

                                if (url.containsKey("expanded_url")) {
                                    expandedUrl = url.getString("expanded_url");

                                    if (expandedText.contains(tempUrl)) {
                                        expandedText = expandedText.replaceAll(
                                                tempUrl,
                                                "<a href=\"" + tempUrl + "\" target=\"_blank\">" + expandedUrl + "</a>");
                                    }
                                } else {
                                    if (expandedText.contains(tempUrl)) {
                                        expandedText = expandedText.replaceAll(
                                                tempUrl,
                                                "<a href=\"" + tempUrl + "\" target=\"_blank\">" + tempUrl + "</a>");
                                    }
                                }
                            }
                        }
                    }

                    if (rawJsonData.getJSONObject("entities").containsKey("media")) {
                        JSONArray media = rawJsonData.getJSONObject("entities").getJSONArray("media");
                        photolink = media.getJSONObject(0).getString("media_url");
                        urllink = photolink;
                    }

                    if (rawJsonData.getJSONObject("entities").containsKey("hashtags")) {
                        JSONArray hashtags = rawJsonData.getJSONObject("entities").getJSONArray("hashtags");
                        for (int k = 0; k < hashtags.size(); k++) {
                            hashtaglist += "#" + hashtags.getJSONObject(k).getString("text") + ", ";
                        }
                        if (hashtaglist.length() > 2) {
                            hashtaglist = hashtaglist.substring(0, hashtaglist.length() - 2);
                        }
                    }
                }

                description += expandedText + "\n";

                description += "]]>\n";

                item.e("description").t(description).up();
                item.e("url").t(urllink).up();
                item.e("photo").t(photolink).up();
                item.e("postauthor").t(user.getString("screen_name")).up();

                if (keywordsForTagging.length > 0) {
                    for (String tag : keywordsForTagging) {
                        if (expandedText.toLowerCase().contains(tag.toLowerCase())) {
                            query_word += tag + ", ";
                        }
                    }

                    if (query_word.length() > 2) {
                        query_word = query_word.substring(0, query_word.length() - 2);
                    } else {
                        query_word = "UNKNOWN";
                    }

                    item.e("query_word").t(query_word).up();
                } else {
                    item.e("query_word");
                }
                item.e("hashtag").t(hashtaglist).up();
                item.e("experimedia_buzzScore").t(rawJsonData.getString("buzzScore")).up();
            } else {
                logger.error("Ignoring data entry of type: " + dataEntry.getDataType());
            }
        }

        return builder.asString(outputPropertiesWithoutDeclaration).replaceAll("&lt;", "<").replaceAll("&gt;", ">");
    }

    /**
     * Creates custom Facebook RSS feed for Digital Schladming experiment.
     *
     * @param data data entries.
     * @return custom Facebook RSS feed for Digital Schladming experiment.
     * @throws TransformerException
     * @throws ParserConfigurationException
     */
    public String sadDataToSchladmingFacebookRss(ArrayList<SADJobData> data) throws TransformerException, ParserConfigurationException {

        builder = XMLBuilder.create("rss")
                .a("version", "2.0")
                .a("xmlns:content", "http://purl.org/rss/1.0/modules/content/")
                .a("xmlns:wfw", "http://wellformedweb.org/CommentAPI/")
                .e("channel")
                .e("title").t("XML Feed of topic tagged FB posts").up()
                .e("link").t("http://onmeedia.com/feeds/").up()
                .e("updated").t(dateFormatFacebook.format(new Date(data.get(0).getWhenCollected().getTime()))).up()
                .e("author").e("name").t("http://onmeedia.com/feeds/").up(3);

        JSONObject rawJsonData, from;
        String original_author, no_whitespace_author, renamed_author, typecategory;
        for (SADJobData dataEntry : data) {
            if (dataEntry.getDataType().equalsIgnoreCase("facebook-posts-topics-tagged")) {
                rawJsonData = JSONObject.fromObject(dataEntry.getJsonData());
                from = rawJsonData.getJSONObject("from");
                item = builder.e("item");

                item.e("title").t(rawJsonData.getString("message"));
                item.e("channel").t("Facebook").up();

                try {
                    item.e("pubDate").t(dateFormatFacebook.parse(rawJsonData.getString("created_time")).toString());
                } catch (ParseException ex) {
                    logger.error("Failed to parse Facebook date: " + rawJsonData.getString("created_time"));
                    item.e("pubDate").t(rawJsonData.getString("created_time"));
                }

                item.e("link").t("http://facebook.com/" + rawJsonData.getString("id"));

                if (rawJsonData.containsKey("picture")) {
                    item.e("photo").t(rawJsonData.getString("picture").replace("_s.", "_x."));
                } else {
                    item.e("photo").t("http://graph.facebook.com/" + from.getString("id") + "/picture?type=square");
                }

                original_author = from.getString("name");
                no_whitespace_author = original_author.replaceAll("\\s+", "_").trim();

                switch (no_whitespace_author) {
                    case "Schladming-Rohrmoos":
                        renamed_author = "Rohrmoos";
                        break;
                    case "Dachsteingletscher_Schladming":
                        renamed_author = "Dachsteingletscher";
                        break;
                    case "Haus_im_Ennstal_Aich_Goessenberg":
                        renamed_author = "Haus_Aich_Goessenberg";
                        break;
                    case "Haus_im_Ennstal_-_Aich_-_Gössenberg":
                        renamed_author = "Haus_Aich_Goessenberg";
                        break;
                    case "Tourismusverband_Pichl_Reiteralm":
                        renamed_author = "Pichl_Reiteralm";
                        break;
                    case "Tourismusverband_Pichl-Reiteralm":
                        renamed_author = "Pichl_Reiteralm";
                        break;
                    case "Planai_Schladming":
                        renamed_author = "Planai";
                        break;
                    case "Planai_-_Schladming":
                        renamed_author = "Planai";
                        break;
                    case "FIS_Alpine_Ski_WM_2013_SCHLADMING_official_site":
                        renamed_author = "Schladming2013";
                        break;
                    case "FIS_Alpine_Ski-WM_2013_SCHLADMING_official_site":
                        renamed_author = "Schladming2013";
                        break;
                    case "Schladming_2030":
                        renamed_author = "Schladming2030";
                        break;
                    case "THE_nightrace_Schladming_official_site":
                        renamed_author = "Nightrace";
                        break;
                    default:
                        renamed_author = no_whitespace_author;
                        break;
                }

                if (renamed_author.equals("Congress-Schladming") || renamed_author.equals("Dachsteingletscher") || renamed_author.equals("ENNSTAL_TV")) {
                    typecategory = "business";
                } else {
                    typecategory = "official";
                }
                item.e("typecategory").t(typecategory);

                item.e("author").t(renamed_author);

                if (rawJsonData.containsKey("experimedia_topics")) {
                    item.e("experimedia_topics").t(rawJsonData.getString("experimedia_topics"));
                } else {
                    item.e("experimedia_topics");
                }
            } else {
                logger.error("Ignoring data entry of type: " + dataEntry.getDataType());
            }
        }
        return builder.asString(outputPropertiesWithDeclaration).replaceAll("&lt;", "<").replaceAll("&gt;", ">");
    }

    public JSONObject getFullRssLinks(String jobId, String pluginName, int num, String type, String keywordsForTagging) {
        JSONObject result = new JSONObject();

        result.putAll(getFullRssLinksForJob(jobId, num, type, keywordsForTagging));
        result.putAll(getFullRssLinksForPlugin(pluginName, num, type, keywordsForTagging));

        return result;

    }

    public JSONObject getFullRssLinksForJob(String jobId, int num, String type, String keywordsForTagging) {
        return getFullRssLinksForJob(jobId, getUrlParametersAsString(num, type, keywordsForTagging));
    }

    public JSONObject getFullRssLinksForPlugin(String pluginName, int num, String type, String keywordsForTagging) {
        return getFullRssLinksForPlugin(pluginName, getUrlParametersAsString(num, type, keywordsForTagging));
    }

    public JSONObject getFullRssLinksForJob(String jobId, String urlParameters) {
        JSONObject result = new JSONObject();

        result.put(JOB_RSS_LINK_NAME, getRssLinkForJob(jobId, urlParameters));
        result.put(JOB_CUSTOM_RSS_LINK_NAME, getCustomRssLinkForJob(jobId, urlParameters));

        return result;
    }

    public JSONObject getFullRssLinksForPlugin(String pluginName, String urlParameters) {
        JSONObject result = new JSONObject();

        result.put(PLUGIN_RSS_LINK_NAME, getRssLinkForPlugin(pluginName, urlParameters));
        result.put(PLUGIN_CUSTOM_RSS_LINK_NAME, getCustomRssLinkForPlugin(pluginName, urlParameters));

        return result;
    }

    public String getRssLinkForJob(String jobId, int num, String type, String keywordsForTagging) {
        return getRssLinkForJob(jobId, getUrlParametersAsString(num, type, keywordsForTagging));
    }

    public String getCustomRssLinkForJob(String jobId, int num, String type, String keywordsForTagging) {
        return getCustomRssLinkForJob(jobId, getUrlParametersAsString(num, type, keywordsForTagging));
    }

    public String getRssLinkForPlugin(String pluginName, int num, String type, String keywordsForTagging) {
        return getRssLinkForPlugin(pluginName, getUrlParametersAsString(num, type, keywordsForTagging));
    }

    public String getCustomRssLinkForPlugin(String pluginName, int num, String type, String keywordsForTagging) {
        return getCustomRssLinkForPlugin(pluginName, getUrlParametersAsString(num, type, keywordsForTagging));
    }

    public String getRssLinkForJob(String jobId, String urlParameters) {
        return (basePath != null ? basePath : "") + "/service/jobs/" + jobId + "/rss/feed.xml" + urlParameters;
    }

    public String getCustomRssLinkForJob(String jobId, String urlParameters) {
        return (basePath != null ? basePath : "") + "/service/jobs/" + jobId + "/xrss/feed.xml" + urlParameters;
    }

    public String getRssLinkForPlugin(String pluginName, String urlParameters) {
        return (basePath != null ? basePath : "") + "/service/plugins/" + pluginName + "/rss/feed.xml" + urlParameters;
    }

    public String getCustomRssLinkForPlugin(String pluginName, String urlParameters) {
        return (basePath != null ? basePath : "") + "/service/plugins/" + pluginName + "/xrss/feed.xml" + urlParameters;
    }

    public String getUrlParametersAsString(int num, String type, String keywordsForTagging) {
        String urlParameters = "?";

        if (num > 0) {
            urlParameters += "num=" + num + "&";
        }

        if (type != null) {
            if (type.trim().length() > 0) {
                urlParameters += "type=" + type + "&";
            }
        }

        if (keywordsForTagging != null) {
            if (keywordsForTagging.trim().length() > 0) {
                urlParameters += "keywordsForTagging=" + keywordsForTagging;
            }
        }

        if (urlParameters.length() < 2) {
            urlParameters = "";
        } else {
            if (urlParameters.endsWith("&")) {
                urlParameters = urlParameters.substring(0, urlParameters.length() - 1);
            }
        }

        return urlParameters;
    }
}
