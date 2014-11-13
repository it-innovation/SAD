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
//	Created Date :			2013-01-10
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.controllers;

import java.util.ArrayList;
import net.sf.json.JSONArray;
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
import uk.ac.soton.itinnovation.sad.service.dao.SADJob;
import uk.ac.soton.itinnovation.sad.service.dao.SADJobData;
import uk.ac.soton.itinnovation.sad.service.domain.JsonResponse;
import uk.ac.soton.itinnovation.sad.service.domain.PropertiesResponse;
import uk.ac.soton.itinnovation.sad.service.domain.SampleResponse;
import uk.ac.soton.itinnovation.sad.service.services.PluginsService;
import uk.ac.soton.itinnovation.sad.service.services.SchedulingService;
import uk.ac.soton.itinnovation.sad.service.utils.Util;

/**
 * Controller implementing various queries for SAD Service.
 */
@Controller
@RequestMapping("/q")
public class QueryController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    @Qualifier("schedulingService")
    SchedulingService schedulingService;

    @Autowired
    @Qualifier("pluginsService")
    PluginsService pluginsService;

    /**
     * GET method to get a sample response.
     *
     * @return 1: "test"
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public SampleResponse getSampleResponse() {

        logger.debug("Returning sample response");

        schedulingService.pushMethodCalledName("/q");

        return new SampleResponse(1, "test");
    }

    /**
     * GET method to fetch SAD Service's configuration.
     *
     * @return configuration as JSON.
     */
//    @RequestMapping(method = RequestMethod.GET, value = "/getconfiguration")
//    @ResponseBody
//    public PropertiesResponse getConfiguration() {
//
//        logger.debug("Returning configuration");
//
//        schedulingService.pushMethodCalledName("/q/getconfiguration");
//
//        return new PropertiesResponse("sadproperties", propertiesService.getLiveProperties());
//    }
    /**
     * POST method to submit a SAD plugin for execution.
     *
     * @param inputData JSON run plugin request contents.
     * @return JSON details of the new SAD job.
     */
    @RequestMapping(method = RequestMethod.POST, value = "/run")
    @ResponseBody
    public JsonResponse runPlugin(@RequestBody final String inputData) {

        logger.debug("Running plugin: " + inputData);

        schedulingService.pushMethodCalledName("/q/run");

        long startTime = System.currentTimeMillis();

        try {
            JSONObject inputDataAsJSON = (JSONObject) JSONSerializer.toJSON(inputData);
            JSONObject response = new JSONObject();

            if (!inputDataAsJSON.containsKey("pluginName")) {
                response.put("message", "Missing field \'pluginName\' in request data");
                return new JsonResponse("error", response);
            }

            if (!inputDataAsJSON.containsKey("arguments")) {
                response.put("message", "Missing field \'arguments\' in request data");
                return new JsonResponse("error", response);
            }

            if (!inputDataAsJSON.containsKey("inputs")) {
                response.put("message", "Missing field \'inputs\' in request data");
                return new JsonResponse("error", response);
            }

            if (!inputDataAsJSON.containsKey("outputs")) {
                response.put("message", "Missing field \'outputs\' in request data");
                return new JsonResponse("error", response);
            }

            if (!inputDataAsJSON.containsKey("schedule")) {
                response.put("message", "Missing field \'schedule\' in request data");
                return new JsonResponse("error", response);
            }

            String pluginName = inputDataAsJSON.getString("pluginName");
            JSONArray inputs = inputDataAsJSON.getJSONArray("inputs");
            JSONArray arguments = inputDataAsJSON.getJSONArray("arguments");
            JSONArray outputs = inputDataAsJSON.getJSONArray("outputs");
            JSONObject schedule = inputDataAsJSON.getJSONObject("schedule");

            try {
                response = schedulingService.runPlugin(pluginName, arguments, inputs, outputs, schedule);
            } catch (Throwable ex) {
                return new JsonResponse("error", Util.dealWithException("Failed to submit plugin to the scheduling service", ex, logger));
            }

            return new JsonResponse("ok", response);

        } catch (Throwable ex) {
            return new JsonResponse("error", Util.dealWithException("Failed to execute runPlugin method", ex, logger));
        } finally {
            schedulingService.pushTimeSpent(Long.toString(System.currentTimeMillis() - startTime));
        }

    }

    /**
     * POST method to submits SAD workflow for execution.
     *
     * @param inputData JSON run workflow request contents.
     * @return JSON details of the new SAD workflow.
     */
    @RequestMapping(method = RequestMethod.POST, value = "/runworkflow")
    @ResponseBody
    public JsonResponse runWorkflow(@RequestBody final String inputData) {

        logger.debug("Received workflow request: " + inputData);

        schedulingService.pushMethodCalledName("/q/runworkflow");

        try {
            JSONObject inputDataAsJSON = (JSONObject) JSONSerializer.toJSON(inputData);
            JSONObject response = new JSONObject();

            if (!inputDataAsJSON.containsKey("name")) {
                response.put("message", "Missing field \'name\' in request data");
                return new JsonResponse("error", response);
            }

            if (!inputDataAsJSON.containsKey("plugins")) {
                response.put("message", "Missing field \'plugins\' in request data");
                return new JsonResponse("error", response);
            }

            if (!inputDataAsJSON.containsKey("schedule")) {
                response.put("message", "Missing field \'schedule\' in request data");
                return new JsonResponse("error", response);
            }

            JSONArray inputDataAsJsonArray = inputDataAsJSON.getJSONArray("plugins");
            logger.debug("Parsing workflow: " + inputDataAsJsonArray.toString(2));

            String name = inputDataAsJSON.getString("name");
            JSONObject schedule = inputDataAsJSON.getJSONObject("schedule");

            try {
                response = schedulingService.runWorkflow(name, inputDataAsJsonArray, schedule);
            } catch (Throwable ex) {
                return new JsonResponse("error", Util.dealWithException("Failed to submit workflow to the scheduling service", ex, logger));
            }

            return new JsonResponse("ok", response);

        } catch (Throwable ex) {
            return new JsonResponse("error", Util.dealWithException("Failed to execute runPlugin method", ex, logger));
        }

    }

    /**
     * Returns metadata for the last ran SAD plugin.
     *
     * @return last created SAD job metadata as JSON.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/getlastrun")
    @ResponseBody
    public JsonResponse getLastRun() {

        logger.debug("Returning last created job");

        schedulingService.pushMethodCalledName("/q/getlastrun");

        SADJob theJob = schedulingService.getLastJob();
        JSONObject response = new JSONObject();

        if (theJob == null) {
            response.put("reason", "No jobs found");
            return new JsonResponse("error", response);

        } else {
            response.put("name", theJob.getName());
            response.put("id", theJob.getId());
            response.put("description", theJob.getDescription());
            response.put("arguments", theJob.getArguments());
            response.put("status", theJob.getStatus());
            response.put("created", theJob.getWhenCreated().toString());
            if (theJob.getWhenLastrun() == null) {
                response.put("lastrun", "N/A");
            } else {
                response.put("lastrun", theJob.getWhenLastrun().toString());
            }

            return new JsonResponse("ok", response);

        }
    }

    /**
     * Returns data generated during the last plugin run.
     *
     * @return data created by the last SAD job.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/getlastrundata")
    @ResponseBody
    public JsonResponse getLastRunData() {

        logger.debug("Returning last created job\'s data");

        schedulingService.pushMethodCalledName("/q/getlastrundata");

        ArrayList<SADJobData> jobDataArray = schedulingService.getLastJobData();
        JSONObject response = new JSONObject();

        if ((jobDataArray == null) || (jobDataArray.isEmpty())) {

            response.put("reason", "No data found");

            return new JsonResponse("error", response);

        } else {

            JSONArray dataEntries = new JSONArray();

            JSONObject jsonEntry;
            for (SADJobData entry : jobDataArray) {
                jsonEntry = JSONObject.fromObject(entry.getJsonData());
                jsonEntry.put("collected", entry.getWhenCollected().toString());
                dataEntries.add(jsonEntry);
            }

            response.put("jsonData", dataEntries);
            return new JsonResponse("ok", response);
        }
    }

}
