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
//	Created Date :			2013-05-16
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.controllers;

import uk.ac.soton.itinnovation.sad.service.services.EMClient;
import uk.ac.soton.itinnovation.sad.service.domain.JsonResponse;
import uk.ac.soton.itinnovation.sad.service.utils.Util;
import java.util.UUID;
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

/**
 * Controller to collect reports from plugins.
 */
@Controller
@RequestMapping("/report")
public class ReportsController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    @Qualifier("EMClient")
    EMClient emClient;

    /**
     * Processes new measurement for a measurement set with UUID and measurement
     * value with MEASUREMENT.
     *
     * @param inputData measurement data as JSON.
     * @return standard service response.
     * @throws java.lang.InterruptedException
     */
    @RequestMapping(method = RequestMethod.POST, value = "/measurement")
    @ResponseBody
    public JsonResponse processNewMeasurement(@RequestBody final String inputData) throws InterruptedException {

        logger.debug("Received measurement: " + inputData);

        try {

            JSONObject response = new JSONObject();
            if (emClient.isEmClientOK()) {
                if (emClient.isMetricModelSetup()) {

                    JSONObject inputDataAsJSON = (JSONObject) JSONSerializer.toJSON(inputData);

                    if (!inputDataAsJSON.containsKey("uuid")) {
                        return new JsonResponse("error", Util.dealWithException("Missing field \'uuid\' in request data", logger));
                    }

                    if (!inputDataAsJSON.containsKey("measurement")) {
                        return new JsonResponse("error", Util.dealWithException("Missing field \'measurement\' in request data", logger));
                    }

                    String uuid = inputDataAsJSON.getString("uuid");
                    String measurement = inputDataAsJSON.getString("measurement");
                    boolean result = emClient.pushSingleMeasurementForMeasurementSetUuid(UUID.fromString(uuid), measurement);

                    response.put("message", "Push success: " + result);

                    return new JsonResponse("ok", response);
                } else {
                    return new JsonResponse("error", Util.dealWithException("Received measurement will NOT be reported to ECC as "
                            + "metric model is yet to be created (start discovery phase or join running experiment)", logger));
                }
            } else {
                return new JsonResponse("error", Util.dealWithException("Received measurement will NOT be reported to ECC as "
                        + "EM client was not created, ECC reporting might be disabled", logger));
            }

        } catch (Throwable ex) {
            return new JsonResponse("error", Util.dealWithException("Failed to execute runPlugin method", ex, logger));
        }
    }

    /**
     * @return Returns all jobs on the service, url mapping:
     * /pluginRunnerMeasurementSetsUuids
     */
    @RequestMapping(method = RequestMethod.GET, value = "/pluginRunnerMeasurementSetsUuids")
    @ResponseBody
    public JsonResponse getPluginRunnerMeasurementSetsUuids() {

        logger.debug("Returning measurement sets UUIDs for plugin runner");

        try {
            if (emClient.isEmClientOK()) {
                if (emClient.isMetricModelSetup()) {
                    JSONObject response = emClient.getPluginRunnerMeasurementSetsUuids();
                    return new JsonResponse("ok", response);
                } else {
                    return new JsonResponse("error", Util.dealWithException("Received 'pluginRunnerMeasurementSetsUuids' will NOT be executed as "
                            + "metric model is yet to be created (start discovery phase or join running experiment)", logger));
                }
            } else {
                return new JsonResponse("error", Util.dealWithException("Received 'pluginRunnerMeasurementSetsUuids' will NOT be executed as "
                        + "EM client was not created, ECC reporting might be disabled", logger));
            }

        } catch (Throwable ex) {
            return new JsonResponse("error", Util.dealWithException("Failed to return measurement sets UUIDs for plugin runner", ex, logger));
        }
    }

}
