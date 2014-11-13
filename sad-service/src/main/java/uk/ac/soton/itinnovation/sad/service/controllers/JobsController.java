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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.soton.itinnovation.sad.service.dao.SADJob;
import uk.ac.soton.itinnovation.sad.service.dao.SADJobData;
import uk.ac.soton.itinnovation.sad.service.domain.JsonResponse;
import uk.ac.soton.itinnovation.sad.service.services.SchedulingService;
import uk.ac.soton.itinnovation.sad.service.utils.Util;

/**
 * Controller implementing REST interface for SAD Jobs.
 */
@Controller
@RequestMapping("/jobs")
public class JobsController extends GenericController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
    SimpleDateFormat dateFormatCustomRss = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
    SimpleDateFormat dateFormatFacebook = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+SSSS");

    @Autowired
    @Qualifier("schedulingService")
    SchedulingService schedulingService;

    /**
     * Returns all jobs on the service, url mapping: /jobs
     *
     * @param clientIpAddress
     * @param jobId negative value returns all jobs
     * @param numJobs
     * @param action
     * @return all jobs as JSON
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public JsonResponse getJobs(
            @ModelAttribute("clientIpAddress") String clientIpAddress,
            @RequestParam(value = "jobId", defaultValue = "") String jobId,
            @RequestParam(value = "num", defaultValue = "10") int numJobs, // -1 returns all jobs
            @RequestParam(value = "action", defaultValue = "") String action
    ) {

        schedulingService.pushMethodCalledName("/jobs");
        long startTime = System.currentTimeMillis();

        JSONObject response = new JSONObject();
        if (jobId.equals("")) {
            switch (action) {
                case "":
                    logger.debug("Returning " + numJobs + (numJobs < 0 ? " (all)" : "") + " jobs to [" + clientIpAddress + "]");
                    try {

                        JSONArray tempArray = new JSONArray();

                        if (numJobs < 0) {
                            for (SADJob job : schedulingService.getJobs()) {
                                tempArray.add(schedulingService.jobAsJson(job));
                            }
                        } else {
                            for (SADJob job : schedulingService.getNumJobs(numJobs)) {
                                tempArray.add(schedulingService.jobAsJson(job));
                            }
                        }

                        response.put("list", tempArray);
                        response.put("num", tempArray.size());
                        return okResponse(response);

                    } catch (Throwable e) {
                        return errorResponse("Failed to return all jobs", e, logger);
                    } finally {
                        schedulingService.pushTimeSpent(Long.toString(System.currentTimeMillis() - startTime));
                    }
                case "cancelAll":
                    logger.warn("Deleting ALL JOBS on request of [" + clientIpAddress + "]");
                    try {
                        schedulingService.cancelAllJobs();
                        return okResponse(response);
                    } catch (Throwable e) {
                        return errorResponse("Failed to cancel all jobs", e, logger);
                    } finally {
                        schedulingService.pushTimeSpent(Long.toString(System.currentTimeMillis() - startTime));
                    }
                default:
                    return errorResponse("Unknown job action '" + action + "'", null, logger);
            }

        } else {
            if (action.equals("")) {
                logger.debug("Returning job [" + jobId + "] to [" + clientIpAddress + "]");
                try {

                    SADJob theJob = schedulingService.getJob(jobId);
                    if (theJob != null) {
                        return okResponse(schedulingService.jobAsJson(theJob));
                    } else {
                        return errorResponse("Job [" + jobId + "] does not exist", null, logger);
                    }

                } catch (Throwable e) {
                    return errorResponse("Failed to return job [" + jobId + "]", e, logger);
                } finally {
                    schedulingService.pushTimeSpent(Long.toString(System.currentTimeMillis() - startTime));
                }
            } else {
                try {
                    SADJob theJob;
                    switch (action) {
                        case "cancel":
                            logger.debug("CANCELLING job [" + jobId + "] as requested by [" + clientIpAddress + "]");
                            theJob = schedulingService.getJob(jobId);
                            if (theJob == null) {
                                return errorResponse("Job [" + jobId + "] does not exist", null, logger);
                            } else {
                                response.put("success", schedulingService.cancelJob(theJob));
                                return okResponse(response);
                            }
                        case "pause":
                            logger.debug("PAUSING job [" + jobId + "] as requested by [" + clientIpAddress + "]");
                            theJob = schedulingService.getJob(jobId);
                            if (theJob == null) {
                                return errorResponse("Job [" + jobId + "] does not exist", null, logger);
                            } else {
                                response.put("success", schedulingService.pauseJob(theJob));
                                return okResponse(response);
                            }
                        case "resume":
                            logger.debug("RESUMING job [" + jobId + "] as requested by [" + clientIpAddress + "]");
                            theJob = schedulingService.getJob(jobId);
                            if (theJob == null) {
                                return errorResponse("Job [" + jobId + "] does not exist", null, logger);
                            } else {
                                response.put("success", schedulingService.resumeJob(theJob));
                                return okResponse(response);
                            }
                        case "delete":
                            logger.debug("DELETING job [" + jobId + "] and all data as requested by [" + clientIpAddress + "]");
                            theJob = schedulingService.getJob(jobId);
                            if (theJob == null) {
                                return errorResponse("Job [" + jobId + "] does not exist", null, logger);
                            } else {
                                response.put("success", schedulingService.deleteJob(theJob));
                                return okResponse(response);
                            }
                        default:
                            return errorResponse("Unknown job action '" + action + "'", null, logger);
                    }
                } catch (Throwable e) {
                    return errorResponse("Failed to perform action '" + action + "' on job [" + jobId + "]", e, logger);
                } finally {
                    schedulingService.pushTimeSpent(Long.toString(System.currentTimeMillis() - startTime));
                }
            }

        }

    }

    /**
     * Returns job with job ID, sample url mapping: /jobs/1
     *
     * @param jobId
     * @return SAD job as JSON
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{jobId:.*[0-9].*}")
    @ResponseBody
    public JsonResponse getJob(@PathVariable String jobId) {

        logger.debug("Returning jobs with ID: " + jobId);

        schedulingService.pushMethodCalledName("/jobs/" + jobId);

        long startTime = System.currentTimeMillis();

        try {
            JSONObject response = new JSONObject();
            SADJob theJob = schedulingService.getJob(jobId);

            response.put("job", schedulingService.jobAsJson(theJob));

            return new JsonResponse("ok", response);

        } catch (Throwable ex) {
            return new JsonResponse("error", Util.dealWithException("Failed to return job with requested ID", ex, logger));
        } finally {
            schedulingService.pushTimeSpent(Long.toString(System.currentTimeMillis() - startTime));
        }
    }

    /**
     * Returns data for job with job ID, sample url mapping: /jobs/1/data
     *
     * @param jobId
     * @return SAD job data as JSON
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/{jobId}/data")
    @ResponseBody
    public JsonResponse getDataForJobWithId(@PathVariable String jobId) {

        logger.debug("Returning data for job with ID: " + jobId);

        schedulingService.pushMethodCalledName("/jobs/" + jobId + "/data");

        long startTime = System.currentTimeMillis();

        try {

            JSONObject response = new JSONObject();
            JSONObject series = new JSONObject();

            SADJob theJob = schedulingService.getJob(jobId);
            ArrayList<SADJobData> jobData = schedulingService.getDataForJobWithId(jobId);
            logger.debug("Found: " + jobData.size() + " job data entries");

            // return data as is
            JSONObject dataEntryAsJson, rawJsonData;
            Timestamp tempTimestamp;
            int counter = 0;
            for (SADJobData dataEntry : jobData) {
                dataEntryAsJson = new JSONObject();

                rawJsonData = JSONObject.fromObject(dataEntry.getJsonData());
                dataEntryAsJson.put("jsonData", rawJsonData);
                dataEntryAsJson.put("type", dataEntry.getDataType());

                tempTimestamp = dataEntry.getWhenCollected();
                if (tempTimestamp == null) {
                    dataEntryAsJson.put("WhenCreated_as_string", "");
                    dataEntryAsJson.put("WhenCreated_in_msec", "");
                } else {
                    dataEntryAsJson.put("WhenCreated_as_string", Util.timestampToString(tempTimestamp));
                    dataEntryAsJson.put("WhenCreated_in_msec", tempTimestamp.getTime());
                }
                series.put(counter, dataEntryAsJson);
                counter++;
            }
            response.put("series", series);

            response.put("num", jobData.size());
            response.put("jobstatus", theJob.getStatus());

            return new JsonResponse("ok", response);

        } catch (Throwable ex) {
            return new JsonResponse("error", Util.dealWithException("Failed to return data for job with requested ID", ex, logger));
        } finally {
            schedulingService.pushTimeSpent(Long.toString(System.currentTimeMillis() - startTime));
        }
    }
}
