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
//	Created Date :			2013-02-12
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.controllers;

import java.sql.Timestamp;
import java.util.ArrayList;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.soton.itinnovation.sad.service.dao.SADJobExecution;
import uk.ac.soton.itinnovation.sad.service.domain.JsonResponse;
import uk.ac.soton.itinnovation.sad.service.services.SchedulingService;
import uk.ac.soton.itinnovation.sad.service.utils.Util;

/**
 * Controller implementing REST interface for SAD Job Executions.
 */
@Controller
@RequestMapping("/executions")
public class ExecutionsController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    @Qualifier("schedulingService")
    SchedulingService schedulingService;

    /**
     * Returns all executions on the service, url mapping: /executions
     *
     * @return all executions as JSON
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public JsonResponse getExecutions() {

        logger.debug("Returning all executions");

        schedulingService.pushMethodCalledName("executions");

        long startTime = System.currentTimeMillis();

        try {
            JSONObject response = new JSONObject();

            ArrayList<SADJobExecution> allExecutions = schedulingService.getExecutions();

            if (allExecutions.isEmpty()) {
                logger.debug("No executions were found.");
                response.put("message", "No executions were found.");
                return new JsonResponse("error", response);
            } else {
                int allExecutionsSize = allExecutions.size();
                response.put("num", allExecutionsSize);
                JSONArray allExecutionsAsJsonArray = new JSONArray();

                JSONObject executionAsJson;
                Timestamp tempTimestamp;
                for (SADJobExecution execution : allExecutions) {
                    executionAsJson = new JSONObject();

                    executionAsJson.put("DatabaseId", execution.getCountID());
                    executionAsJson.put("ID", execution.getId());
                    executionAsJson.put("SADJobID", execution.getSADJobID());
                    executionAsJson.put("Description", execution.getDescription());
                    executionAsJson.put("Status", execution.getStatus());

                    tempTimestamp = execution.getWhenStarted();
                    if (tempTimestamp == null) {
                        executionAsJson.put("WhenStarted_as_string", "");
                        executionAsJson.put("WhenStarted_in_msec", "");
                    } else {
                        executionAsJson.put("WhenStarted_as_string", tempTimestamp.toString());
                        executionAsJson.put("WhenStarted_in_msec", tempTimestamp.getTime());
                    }

                    tempTimestamp = execution.getWhenFinished();
                    if (tempTimestamp == null) {
                        executionAsJson.put("WhenFinished_as_string", "");
                        executionAsJson.put("WhenFinished_in_msec", "");
                    } else {
                        executionAsJson.put("WhenFinished_as_string", tempTimestamp.toString());
                        executionAsJson.put("WhenFinished_in_msec", tempTimestamp.getTime());
                    }

                    allExecutionsAsJsonArray.add(executionAsJson);
                }

                if (allExecutionsSize < 2) {
                    logger.debug("Returning " + allExecutions.size() + " execution");
                } else {
                    logger.debug("Returning " + allExecutions.size() + " executions");
                }
                response.put("list", allExecutionsAsJsonArray);
                return new JsonResponse("ok", response);
            }
        } catch (Throwable ex) {
            return new JsonResponse("error", Util.dealWithException("Failed to return all executions", ex, logger));
        } finally {
            schedulingService.pushTimeSpent(Long.toString(System.currentTimeMillis() - startTime));
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{pluginName}/{dataType}")
    @ResponseBody
    public JsonResponse getExecutionDataByName(@PathVariable String pluginName, @PathVariable String dataType) {

        logger.debug("Returning execution data by plugin name: '" + pluginName + "', data type: '" + dataType + "'");

        schedulingService.pushMethodCalledName("/executions/" + pluginName + "/" + dataType);

        long startTime = System.currentTimeMillis();

        try {
            JSONArray response = schedulingService.getLastExecutionDataForPlugin(pluginName, dataType);
//            JSONObject pluginWithName = pluginsService.getPluginByName(pluginName);
//
//            if (pluginWithName.isEmpty()) {
//                logger.error("Plugin with name '" + pluginName + "' was not found.");
//                response.put("message", "Plugin with name '" + pluginName + "' was not found.");
//                return new JsonResponse("error", response);
//            } else {
//                return new JsonResponse("ok", pluginWithName);
//            }

            JsonResponse serviceResponse = new JsonResponse("ok", response);

            logger.debug(serviceResponse.toString());

            return serviceResponse;

        } catch (Throwable ex) {
            return new JsonResponse("error", Util.dealWithException("Failed to return plugin by name", ex, logger));
        } finally {
            schedulingService.pushTimeSpent(Long.toString(System.currentTimeMillis() - startTime));
        }
    }
}
