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
//	Created Date :			2013-10-15
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.controllers;

import java.util.Date;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.soton.itinnovation.sad.service.adapters.ProvEMClient;
import uk.ac.soton.itinnovation.sad.service.domain.JsonResponse;
import uk.ac.soton.itinnovation.sad.service.helpers.ProvVideo;
import uk.ac.soton.itinnovation.sad.service.utils.Util;

@Controller
@RequestMapping("/prov")
public class ProvController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    @Qualifier("ProvEMClient")
    ProvEMClient provEMClient;

    @RequestMapping(method = RequestMethod.POST, value = "/player")
    @ResponseBody
    public JsonResponse reportPlayerLoadedAction(@RequestBody final String inputData) throws InterruptedException {
        logger.debug("Video player loaded action received: " + inputData);

        try {
            JSONObject inputDataAsJSON = (JSONObject) JSONSerializer.toJSON(inputData);
            JSONObject response = new JSONObject();

            logger.debug(inputDataAsJSON.toString(2));

            if (!inputDataAsJSON.containsKey("video_url")) {
                response.put("message", "Missing field \'video_url\' in request data");
                return new JsonResponse("error", response);
            }

            if (!inputDataAsJSON.containsKey("user_name")) {
                response.put("message", "Missing field \'user_name\' in request data");
                return new JsonResponse("error", response);
            }

            if (!inputDataAsJSON.containsKey("timestamp")) {
                response.put("message", "Missing field \'timestamp\' in request data");
                return new JsonResponse("error", response);
            }

            if (!inputDataAsJSON.containsKey("user_id")) {
                response.put("message", "Missing field \'user_id\' in request data");
                return new JsonResponse("error", response);
            }

            Long timestamp = inputDataAsJSON.getLong("timestamp") / 1000;
            String user_id = inputDataAsJSON.getString("user_id");
            String user_name = inputDataAsJSON.getString("user_name");
            String video_url = inputDataAsJSON.getString("video_url");

            JSONObject event = new JSONObject();
            event.put("user_id", user_id);
            event.put("user_name", user_name);
            event.put("video_url", video_url);
            event.put("timestamp", timestamp);
            provEMClient.reportLoadPlayer(event);

            return new JsonResponse("ok", response);

        } catch (Throwable ex) {
            return new JsonResponse("error", Util.dealWithException("Failed to process player loaded prov event", ex, logger));
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/video")
    @ResponseBody
    public JsonResponse reportVideoAction(@RequestBody final String inputData) throws InterruptedException {

        logger.debug("Video action received: " + inputData);

        try {
            JSONObject inputDataAsJSON = (JSONObject) JSONSerializer.toJSON(inputData);
            JSONObject response = new JSONObject();

            logger.debug(inputDataAsJSON.toString(2));

            if (!inputDataAsJSON.containsKey("video_url")) {
                response.put("message", "Missing field \'video_url\' in request data");
                return new JsonResponse("error", response);
            }

            if (!inputDataAsJSON.containsKey("action")) {
                response.put("message", "Missing field \'action\' in request data");
                return new JsonResponse("error", response);
            }

            if (!inputDataAsJSON.containsKey("timestamp")) {
                response.put("message", "Missing field \'timestamp\' in request data");
                return new JsonResponse("error", response);
            }

            if (!inputDataAsJSON.containsKey("user_id")) {
                response.put("message", "Missing field \'user_id\' in request data");
                return new JsonResponse("error", response);
            }

            if (!inputDataAsJSON.containsKey("user_name")) {
                response.put("message", "Missing field \'user_name\' in request data");
                return new JsonResponse("error", response);
            }

            Long timeStamp_long = inputDataAsJSON.getLong("timestamp");
            Date timeStamp = new Date(timeStamp_long);
            String user_id = inputDataAsJSON.getString("user_id");
            String user_name = inputDataAsJSON.getString("user_name");
            String video_url = inputDataAsJSON.getString("video_url");

            if (inputDataAsJSON.getString("action").equals("play")) {
                ProvVideo video = new ProvVideo(video_url, "playVideo", user_id, user_name, "", timeStamp_long);
                provEMClient.reportActionByPerson(video);
                logger.debug("Video: " + video_url + ", action: PLAY, timestamp: " + timeStamp.toString() + ", by USER ID [" + user_id + "]");
                provEMClient.pushActionTakenMeasurementToEcc("playVideo");
            } else {

                if (!inputDataAsJSON.containsKey("pause_time")) {
                    response.put("message", "Missing field \'pause_time\' in request data for 'pause' action");
                    return new JsonResponse("error", response);
                }

                String pause_time = inputDataAsJSON.getString("pause_time");
                ProvVideo video = new ProvVideo(video_url, "pauseVideo", user_id, user_name, pause_time, timeStamp_long);

                provEMClient.reportActionByPerson(video);
                logger.debug("Video: " + video_url + ", action: PAUSE, timestamp: " + timeStamp.toString() + ", PAUSED AT: " + pause_time + ", by USER ID [" + user_id + "]");
                provEMClient.pushActionTakenMeasurementToEcc("pauseVideo");
            }

            if (inputDataAsJSON.containsKey("res")) {
                String resolution = inputDataAsJSON.getString("res");
                logger.debug("Pushing resolution measurement to ECC: " + resolution);
                provEMClient.pushVideoQualityMeasurementToEcc(resolution);
            }

            return new JsonResponse("ok", response);

        } catch (Throwable ex) {
            return new JsonResponse("error", Util.dealWithException("Failed to execute reportVideoAction method", ex, logger));
        }

    }
}
