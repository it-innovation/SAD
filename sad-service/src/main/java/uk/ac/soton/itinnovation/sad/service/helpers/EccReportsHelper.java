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
//	Created Date :			2013-05-20
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import net.sf.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Helper class to assist with reporting ECC measurements (WIP).
 */
@Service("EccReportsHelper")
public class EccReportsHelper {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private HttpClient httpclient;
    private HttpPost httpPost;
    private HttpGet httpGet;
    private URL basepathAsURL;
    private boolean isInitialised = false;

    public EccReportsHelper() {

    }

    public EccReportsHelper(String basepath) {
        try {
            this.basepathAsURL = new URL(basepath);
            httpclient = new DefaultHttpClient();
            isInitialised = true;
        } catch (MalformedURLException ex) {
            logger.error("Failed to initialise EccReportsHelper", ex);
        }
    }

    public boolean isInitialised() {
        return isInitialised;
    }

    /**
     * Reports new measurement value to basepath/measurement.
     *
     * @param uuid measurement set that the measurement belongs to.
     * @param measurement measurement value.
     */
    public void reportMeasurement(String uuid, String measurement) {

        if (isInitialised) {

            String queryUrl = basepathAsURL.toString() + "/measurement";
            httpPost = new HttpPost(queryUrl);

            String parametersAsJsonString = "{\"uuid\":\"" + uuid + "\", \"measurement\":\"" + measurement + "\"}";

            logger.debug("Sending request '" + parametersAsJsonString + "' to URL: " + queryUrl);

            try {

                StringEntity params = new StringEntity(parametersAsJsonString, HTTP.UTF_8);

                httpPost.addHeader("content-type", "application/json; charset=utf-8");
                httpPost.setEntity(params);

                HttpResponse response = httpclient.execute(httpPost);
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    try (InputStream instream = entity.getContent()) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(instream));

                        String line;
                        while ((line = br.readLine()) != null) {
                            logger.debug("Service response: " + line);
                        }

                    }
                }
            } catch (IOException ex) {
                logger.error("Failed to send request: '" + parametersAsJsonString + "' to URL: " + queryUrl, ex);
            }
        } else {
            logger.error("Request 'reportMeasurement' was ignored: EccReportsHelper not initialised");
        }
    }

    /**
     * @return UUIDs of measurement sets required by Plugin Runner to report
     * metrics about SAD plugins to basepath/pluginRunnerMeasurementSetsUuids.
     */
    public JSONObject getPluginRunnerMeasurementSetsUuids() {
        if (isInitialised) {

            httpGet = new HttpGet(basepathAsURL.toString() + "/pluginRunnerMeasurementSetsUuids");
            try {
                HttpResponse response = httpclient.execute(httpGet);
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    try (InputStream instream = entity.getContent()) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(instream));
                        StringBuilder sb = new StringBuilder();

                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                            logger.debug("Service's response: " + line);
                        }

                        if (sb.length() < 1) {
                            return null;
                        } else {
                            JSONObject result = JSONObject.fromObject(sb.toString());
                            logger.debug("Parsed service's response:\n " + result.toString(2));

                            if (!result.containsKey("result")) {
                                return null;
                            } else {
                                if (result.getString("result").equals("ok")) {
                                    if (!result.containsKey("response")) {
                                        logger.debug("Malformed service reponse: expected key 'response'");
                                        return null;
                                    } else {
                                        return result.getJSONObject("response");
                                    }
                                } else {
                                    logger.error("Error response returned by the service in response to pluginRunnerMeasurementSetsUuids");
                                    return null;
                                }
                            }

                        }

                    } catch (IOException e) {
                        logger.error("Failed to request pluginRunnerMeasurementSetsUuids", e);
                        return null;
                    }
                } else {
                    logger.error("Failed to process response entity as it's NULL");
                    return null;
                }

            } catch (IOException ex) {
                logger.error("Failed to send pluginRunnerMeasurementSetsUuids request", ex);
                return null;
            }

        } else {
            logger.error("Request 'getPluginRunnerMeasurementSetsUuids' was ignored: EccReportsHelper not initialised");
            return null;
        }
    }

}
