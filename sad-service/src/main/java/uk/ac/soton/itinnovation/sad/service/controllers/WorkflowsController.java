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
//	Created Date :			2013-03-27
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.controllers;

import uk.ac.soton.itinnovation.sad.service.dao.SADWorkflow;
import uk.ac.soton.itinnovation.sad.service.domain.JsonResponse;
import uk.ac.soton.itinnovation.sad.service.services.SchedulingService;
import uk.ac.soton.itinnovation.sad.service.utils.Util;
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

/**
 * Controller implementing REST interface for SAD Jobs.
 */
@Controller
@RequestMapping("/workflows")
public class WorkflowsController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    @Qualifier("schedulingService")
    SchedulingService schedulingService;

    /**
     * Returns all workflows on the service, url mapping: /workflows
     *
     * @return all workflows as JSON
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public JsonResponse getWorkflows() {

        logger.debug("Returning all workflows");

        try {
            JSONObject response = new JSONObject();
            ArrayList<SADWorkflow> allWorkflows = schedulingService.getWorkflows();

            if (allWorkflows.isEmpty()) {
                logger.debug("No workflows were found.");
                response.put("num", 0);
                return new JsonResponse("ok", response);

            } else {
                int allWorkflowsSize = allWorkflows.size();
                response.put("num", allWorkflowsSize);
                JSONArray allWorkflowsAsJsonArray = new JSONArray();

                for (SADWorkflow theWorkflow : allWorkflows) {
                    allWorkflowsAsJsonArray.add(schedulingService.workflowAsJson(theWorkflow));
                }

                if (allWorkflowsSize < 2) {
                    logger.debug("Returning " + allWorkflows.size() + " workflow");
                } else {
                    logger.debug("Returning " + allWorkflows.size() + " workflows");
                }
                response.put("list", allWorkflowsAsJsonArray);
                return new JsonResponse("ok", response);
            }

        } catch (Throwable ex) {
            return new JsonResponse("error", Util.dealWithException("Failed to return all workflows", ex, logger));
        }
    }

    /**
     * Returns workflow with workflow ID, sample url mapping: /workflows/1
     *
     * @return SAD workflow as JSON
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{workflowId:.*[0-9].*}")
    @ResponseBody
    public JsonResponse getJob(@PathVariable String workflowId) {

        logger.debug("Returning workflows with ID: " + workflowId);

        try {
            JSONObject response = new JSONObject();
            SADWorkflow theWorkflow = schedulingService.getWorkflow(workflowId);

            response.put(workflowId, schedulingService.workflowAsJson(theWorkflow));

            return new JsonResponse("ok", response);

        } catch (Throwable ex) {
            return new JsonResponse("error", Util.dealWithException("Failed to return workflow with requested ID", ex, logger));
        }
    }

}
