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
//	Created By :			Sleiman Jneidi, Maxim Bashevoy
//	Created Date :			2013-08-14
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.services;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Key;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.calendar.BaseCalendar;
import org.quartz.spi.OperableTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.soton.itinnovation.sad.coordinator.Coordinator;
import uk.ac.soton.itinnovation.sad.coordinator.SADCollections;
import uk.ac.soton.itinnovation.sad.service.dao.SADJob;
import uk.ac.soton.itinnovation.sad.service.dao.SADJobData;
import uk.ac.soton.itinnovation.sad.service.dao.SADJobExecution;
import uk.ac.soton.itinnovation.sad.service.dao.SADWorkflow;
import uk.ac.soton.itinnovation.sad.service.domain.ExecStatus;
import uk.ac.soton.itinnovation.sad.service.helpers.CoordinatorHelper;
import uk.ac.soton.itinnovation.sad.service.helpers.DataHelper;
import uk.ac.soton.itinnovation.sad.service.helpers.EccReportsHelper;
import uk.ac.soton.itinnovation.sad.service.helpers.PluginRunner;
import uk.ac.soton.itinnovation.sad.service.helpers.WorkflowRunner;
import uk.ac.soton.itinnovation.sad.service.utils.Util;

/**
 * SAD component that manages SAD plugins execution.
 */
@Service("schedulingService")
public class SchedulingService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Scheduler scheduler;
    private final DataHelper dh = new DataHelper();

    @Value("${server.port}")
    int localPort;

    @Value("${server.context-path}")
    String contextPath;

    @Autowired
    @Qualifier("configurationService")
    ConfigurationService configurationService;

    @Autowired
    @Qualifier("pluginsService")
    PluginsService pluginsService;

    @Autowired
    @Qualifier("eccIntegrationService")
    EccIntegrationService eccIntegrationService;

    @Autowired
    @Qualifier("coordinatorHelper")
    private transient CoordinatorHelper helper;

    @Autowired
    @Qualifier("EccReportsHelper")
    EccReportsHelper eccReportsHelper;

    @Autowired
    @Qualifier("EMClient")
    EMClient emClient;

    public SchedulingService() {

    }

    @PostConstruct
    public void init() {
    }

    public boolean start() {
        logger.debug("Starting Scheduling service");

        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
        } catch (SchedulerException ex) {
            logger.error("Failed to initialise Quartz scheduler", ex);
            return false;
        }

        try {
            scheduler.start();
        } catch (SchedulerException ex) {
            logger.error("Failed to start Quartz scheduler", ex);
            return false;
        }

        if (configurationService.getConfiguration().isResetDatabaseOnStart()) {
            getCoordinator(true);
        } else {
            getCoordinator(false);
        }

        logger.debug("Scheduling service STARTED");
        return true;

    }

    public void stop() {

        logger.debug("Stopping Scheduling service");

        try {
            if (scheduler != null) {
                logger.debug("Shutting down Quartz scheduler");
                scheduler.shutdown();
                while (!scheduler.isShutdown()) {
                    Thread.sleep(1000);
                    logger.debug("Giving scheduler one more second");
                }
//            logger.debug("Giving scheduler 10 seconds to shut down");
//            Thread.sleep(10000);
                logger.debug("Scheduler shut down: " + scheduler.isShutdown());
            }
        } catch (Exception e) {
            logger.error("Failed to stop Scheduling service", e);
        }

        logger.debug("Closing Mongo connection");
        getCoordinator(false).closeMongo();

        logger.debug("Scheduling service STOPPED");

    }

    /**
     * Deletes a job from scheduler.
     *
     * @param job
     * @return
     * @throws SchedulerException
     */
    public boolean cancelJob(SADJob job) throws SchedulerException {
        JobKey jobKey = new JobKey(job.getPluginName(), job.getId().toString());
        logger.debug("Deleting job from scheduler: " + jobKey);
        job.setStatus(ExecStatus.CANCELLED);
        getCoordinator(false).saveObject(job);
        if (scheduler.checkExists(jobKey)) {
            return scheduler.deleteJob(jobKey);
        } else {
            logger.debug("Job " + jobKey + " was not found on scheduler");
            return true;
        }
    }

    /**
     * Removes all jobs from the scheduler.
     *
     * @throws SchedulerException
     */
    public void cancelAllJobs() throws SchedulerException {

        JobKey jobKey;
        for (JobExecutionContext jobCtx : scheduler.getCurrentlyExecutingJobs()) {
            jobKey = jobCtx.getJobDetail().getKey();
            logger.debug("Deleting job from scheduler: " + jobKey);
            scheduler.deleteJob(jobKey);
        }

    }

    /**
     * Deletes a job and all its data.
     *
     * @param job
     * @return
     * @throws SchedulerException
     */
    public boolean deleteJob(SADJob job) throws SchedulerException {
        logger.debug("Removing job [" + job.getIdAsString() + "] and its data");

        // remove from scheduler first
        try {
            cancelJob(job);
        } catch (Throwable e) {
            logger.error("Failed to cancel job", e);
        }

        // remove all data
        DBCollection collection = getCoordinator(false).getDBCollection(SADCollections.SADJobData);
        DBCursor cursor = collection.find(new BasicDBObject("SADJobID", job.getId()));
        while (cursor.hasNext()) {
            collection.remove(cursor.next());
        }

        // remove database entry
        collection = getCoordinator(false).getDBCollection(SADCollections.SADJobs);
        cursor = collection.find(new BasicDBObject("_id", job.getId()));
        while (cursor.hasNext()) {
            collection.remove(cursor.next());
        }
        return true;
    }

    /**
     * Pauses job execution schedule.
     *
     * @param job
     * @return
     * @throws SchedulerException
     */
    public boolean pauseJob(SADJob job) throws SchedulerException {
        JobKey jobKey = new JobKey(job.getPluginName(), job.getId().toString());
        logger.debug("Pausing job: " + jobKey);
        scheduler.pauseJob(jobKey);
        job.setStatus(ExecStatus.PAUSED);
        getCoordinator(false).saveObject(job);
        return true;
    }

    /**
     * Resumes job execution schedule.
     *
     * @param job
     * @return
     * @throws SchedulerException
     */
    public boolean resumeJob(SADJob job) throws SchedulerException {
        JobKey jobKey = new JobKey(job.getPluginName(), job.getId().toString());
        logger.debug("Resuming job: " + jobKey);
        scheduler.resumeJob(jobKey);
        job.setStatus(ExecStatus.RUNNING);
        getCoordinator(false).saveObject(job);
        return true;
    }

    /**
     * @return Coordinator instance in use by the SAD Service.
     */
    private Coordinator getCoordinator(boolean doDatabaseReset) {
        try {
            return helper.getCoordinator(configurationService.getConfiguration().getCoordinatorPath(), doDatabaseReset);
        } catch (Throwable ex) {
            throw new RuntimeException("Failed to initialise Coordinator", ex);
        }
    }

    /**
     * Creates new SAD Job.
     *
     * @param workflowId null for no workflow.
     * @param pluginName name of the plugin.
     * @param pluginConfigurationAsJsonString full plugin config.
     * @param jobName name of the new job.
     * @param arguments JSON arguments as String.
     * @param schedule JSON schedule as String.
     * @param outputs JSON outputs as String.
     * @param inputs JSON inputs as String.
     * @return ID of the new SAD Job.
     */
    public String addJob(String workflowId,
            String pluginName,
            String pluginConfigurationAsJsonString,
            String jobName,
            String arguments,
            String inputs,
            String outputs,
            String schedule) {

        ObjectId wrkFlowId = null;
        if (null != workflowId) {
            wrkFlowId = new ObjectId(workflowId);
        }

        SADJob sadj = new SADJob(
                wrkFlowId,
                pluginName,
                pluginConfigurationAsJsonString,
                jobName,
                "Job created by Scheduling service",
                arguments,
                inputs,
                outputs,
                schedule,
                "submitted",
                new Timestamp(System.currentTimeMillis()),
                null);

        long startTime = System.currentTimeMillis();
        Key<SADJob> key = getCoordinator(false).saveObject(sadj);
        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));
        String jobId = key.getId().toString();
        logger.debug("Created new job with ID: " + jobId);
        return jobId;

    }

    /**
     * Creates new SAD Workflow.
     *
     * @param name new workflow name.
     * @param pluginsAsJsonString config JSON.
     * @param scheduleAsJsonString schedule JSON.
     * @return ID of the new SAD Workflow.
     */
    public String addWorkflow(String name, String pluginsAsJsonString, String scheduleAsJsonString) {

        SADWorkflow workflow = new SADWorkflow(
                name,
                pluginsAsJsonString,
                scheduleAsJsonString,
                "submitted",
                new Timestamp(System.currentTimeMillis()),
                null);

        long startTime = System.currentTimeMillis();
        Key<SADWorkflow> key = getCoordinator(false).saveObject(workflow);
        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        String wfId = key.getId().toString();

        logger.debug("Created new workflow with ID: " + wfId);
        return wfId;

    }

    /**
     * @param workflowId ID of the workflow to return.
     * @return SAD Workflow metadata from the database.
     */
    public SADWorkflow getWorkflow(String workflowId) {
        SADWorkflow workflow = getCoordinator(false).getDatastore().get(SADWorkflow.class, new ObjectId(workflowId));

        if (workflow == null) {
            throw new RuntimeException("SAD Workflow with ID \'" + workflowId + "\' does not exist in the database");
        }

        return workflow;
    }

    /**
     * @param jobId ID of the job to return.
     * @return SAD Job metadata from the database.
     */
    public SADJob getJob(String jobId) {

        long startTime = System.currentTimeMillis();
        SADJob job = getCoordinator(false).getDatastore().get(SADJob.class, new ObjectId(jobId));
        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        if (null == job) {
            throw new RuntimeException("SAD job with ID \'" + jobId + "\' does not exist in the database");
        }

        return job;
    }

    /**
     * @return all SAD Workflows from the database.
     */
    public ArrayList<SADWorkflow> getWorkflows() {
        logger.debug("Returning a list of all workflows");
        long startTime = System.currentTimeMillis();
        ArrayList<SADWorkflow> result = new ArrayList<>(getCoordinator(false).createQuery(SADWorkflow.class).asList());
        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        logger.debug("Found " + result.size() + " workflows");
        return result;
    }

    /**
     * @return all SAD Jobs from the database.
     */
    public ArrayList<SADJob> getJobs() {
        logger.debug("Returning a list of all jobs");
        long startTime = System.currentTimeMillis();
        ArrayList<SADJob> result = new ArrayList<>(getCoordinator(false).createQuery(SADJob.class).order("-whenCreated").asList());
        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        logger.debug("Found " + result.size() + " jobs");
        return result;
    }

    /**
     * @param numJobs
     * @return all SAD Jobs from the database.
     */
    public ArrayList<SADJob> getNumJobs(int numJobs) {
        logger.debug("Returning a list of the last " + numJobs + " jobs");
        long startTime = System.currentTimeMillis();
        ArrayList<SADJob> result = new ArrayList<>(getCoordinator(false).createQuery(SADJob.class).order("-whenCreated").limit(numJobs).asList());
        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        logger.debug("Found " + result.size() + " jobs");
        return result;
    }

    /**
     * @return last created SAD Job from the database.
     */
    public SADJob getLastJob() {
        logger.debug("Returning the last job");
        long startTime = System.currentTimeMillis();
        SADJob lastJob = getCoordinator(false).createQuery(SADJob.class).order("-whenCreated").get();
        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        if (lastJob == null) {
            logger.warn("No jobs in the dabatase");
        }

        return lastJob;
    }

    /**
     * @param jobId ID of the job to return.
     * @return SAD Job's Data by SAD Job's ID.
     */
    public ArrayList<SADJobData> getDataForJobWithId(String jobId) {
        logger.debug("Returning data for job with ID: " + jobId);

        ArrayList<SADJobData> result = new ArrayList<>();
        ObjectId sadJobId = new ObjectId(jobId);
        DBCollection collection = getCoordinator(false).getDBCollection(SADCollections.SADJobData);

        BasicDBObject sorter = new BasicDBObject("whenCollected", -1);
        long startTime = System.currentTimeMillis();
        DBCursor cursor = collection.find(new BasicDBObject("SADJobID", sadJobId)).sort(sorter);

        while (cursor.hasNext()) {
            DBObject object = cursor.next();
            SADJobData sadjd = dh.dBObjectToSADJobData(object);
            result.add(sadjd);
        }
        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        return result;
    }

    /**
     * @param pluginName name of the plugin to search the database for data by.
     * @return SAD plugin's data by plugin name.
     */
    public ArrayList<SADJobData> getDataForPluginWithName(String pluginName) {
        logger.debug("Returning data for plugin with name: " + pluginName);

        ArrayList<SADJobData> result = new ArrayList<>();
        DBCollection collection = getCoordinator(false).getDBCollection(SADCollections.SADJobData);
        BasicDBObject sorter = new BasicDBObject("whenCollected", -1);
        long startTime = System.currentTimeMillis();
        DBCursor cursor = collection.find(new BasicDBObject("pluginName", pluginName)).sort(sorter);

        while (cursor.hasNext()) {
            DBObject object = cursor.next();
            SADJobData sadjd = dh.dBObjectToSADJobData(object);
            result.add(sadjd);
        }
        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        return result;

    }

    /**
     * @param jobId ID of the job.
     * @param limit number of data entries to return. Less than 1 returns
     * everything.
     * @return SAD job's data by job ID.
     */
    public ArrayList<SADJobData> getDataForJobWithId(String jobId, int limit) {

        logger.debug("Returning data for job with ID: " + jobId + " with limit " + limit);

        if (limit < 1) {
            return getDataForJobWithId(jobId);
        }

        ArrayList<SADJobData> result = new ArrayList<>();
        ObjectId sadJobId = new ObjectId(jobId);
        DBCollection collection = getCoordinator(false).getDBCollection(SADCollections.SADJobData);
        BasicDBObject sorter = new BasicDBObject("whenCollected", -1);
        long startTime = System.currentTimeMillis();
        DBCursor cursor = collection.find(new BasicDBObject("SADJobId", sadJobId)).sort(sorter).limit(limit);

        while (cursor.hasNext()) {
            DBObject object = cursor.next();
            SADJobData sadjd = dh.dBObjectToSADJobData(object);
            result.add(sadjd);
        }
        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        return result;
    }

    /**
     *
     * @param pluginName name of the plugin.
     * @param limit number of data entries to return. Less than 1 returns
     * everything.
     * @return SAD job's data by plugin name.
     */
    public ArrayList<SADJobData> getDataForPluginWithName(String pluginName, int limit) {

        logger.debug("Returning data for plugin with name: " + pluginName + " with limit " + limit);

        if (limit < 1) {
            return getDataForPluginWithName(pluginName);
        }

        ArrayList<SADJobData> result = new ArrayList<>();
        DBCollection collection = getCoordinator(false).getDBCollection(SADCollections.SADJobData);

        BasicDBObject sorter = new BasicDBObject("whenCollected", -1);
        long startTime = System.currentTimeMillis();
        DBCursor cursor = collection.find(new BasicDBObject("pluginName", pluginName)).sort(sorter).limit(limit);

        while (cursor.hasNext()) {
            DBObject object = cursor.next();
            SADJobData sadjd = dh.dBObjectToSADJobData(object);
            result.add(sadjd);
        }
        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        return result;
    }

    /**
     * @param jobId ID of the job.
     * @param type data type.
     * @return SAD Job's Data by SAD Job's ID and type.
     */
    public ArrayList<SADJobData> getDataForJobWithId(String jobId, String type) {

        logger.debug("Returning data for job with ID: " + jobId + " and of type " + type);

        if (type == null) {
            return getDataForJobWithId(jobId);
        } else if (type.trim().length() < 1) {
            return getDataForJobWithId(jobId);
        }

        ArrayList<SADJobData> result = new ArrayList<>();
        ObjectId sadJobId = new ObjectId(jobId);
        DBCollection collection = getCoordinator(false).getDBCollection(SADCollections.SADJobData);

        BasicDBObject filter = new BasicDBObject("SADJobId", sadJobId);
        filter.put("dataType", type);
        BasicDBObject sorter = new BasicDBObject("whenCollected", -1);
        long startTime = System.currentTimeMillis();
        DBCursor cursor = collection.find(filter).sort(sorter);

        while (cursor.hasNext()) {
            DBObject object = cursor.next();
            SADJobData sadjd = dh.dBObjectToSADJobData(object);
            result.add(sadjd);
        }

        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        return result;
    }

    /**
     * @param jobId ID of the job.
     * @param limit number of data entries to return. Less than 1 returns
     * everything.
     * @param type data type.
     * @return SAD Job's Data by SAD Job's ID and type.
     */
    public ArrayList<SADJobData> getDataForJobWithId(String jobId, int limit, String type) {

        logger.debug("Returning data for job with ID: " + jobId + " with limit " + limit + " and of type " + type);

        if (limit < 1) {
            return getDataForJobWithId(jobId, type);
        }

        ArrayList<SADJobData> result = new ArrayList<>();
        ObjectId sadJobId = new ObjectId(jobId);
        DBCollection collection = getCoordinator(false).getDBCollection(SADCollections.SADJobData);

        BasicDBObject filter = new BasicDBObject("SADJobId", sadJobId);
        BasicDBObject sorter = new BasicDBObject("whenCollected", -1);
        if (type == null) {

        } else if (type.trim().length() > 0) {
            filter.put("dataType", type);
        }
        long startTime = System.currentTimeMillis();
        DBCursor cursor = collection.find(filter).sort(sorter).limit(limit);

        while (cursor.hasNext()) {
            DBObject object = cursor.next();
            SADJobData sadjd = dh.dBObjectToSADJobData(object);
            result.add(sadjd);
        }

        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        return result;
    }

    /**
     * @param pluginName name of the plugin.
     * @param type data type.
     * @return SAD plugin data by SAD plugin name and type
     */
    public ArrayList<SADJobData> getDataForPluginWithName(String pluginName, String type) {

        logger.debug("Returning data for plugin with name: " + pluginName + " and of type " + type);

        if (type == null) {
            return getDataForPluginWithName(pluginName);
        } else if (type.trim().length() < 1) {
            return getDataForPluginWithName(pluginName);
        }

        ArrayList<SADJobData> result = new ArrayList<>();
        DBCollection collection = getCoordinator(false).getDBCollection(SADCollections.SADJobData);

        BasicDBObject filter = new BasicDBObject("pluginName", pluginName);
        filter.put("dataType", type);
        BasicDBObject sorter = new BasicDBObject("whenCollected", -1);
        long startTime = System.currentTimeMillis();
        DBCursor cursor = collection.find(filter).sort(sorter);

        while (cursor.hasNext()) {
            DBObject object = cursor.next();
            SADJobData sadjd = dh.dBObjectToSADJobData(object);
            result.add(sadjd);
        }

        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        return result;
    }

    /**
     * @param pluginName name of the plugin.
     * @param limit number of data entries to return. Less than 1 returns
     * everything.
     * @param type data type.
     * @return SAD plugin data by SAD plugin name and type.
     */
    public ArrayList<SADJobData> getDataForPluginWithName(String pluginName, int limit, String type) {

        logger.debug("Returning data for plugin with name: " + pluginName + " with limit " + limit + " and of type " + type);

        if (limit < 1) {
            return getDataForPluginWithName(pluginName, type);
        }

        ArrayList<SADJobData> result = new ArrayList<>();
        DBCollection collection = getCoordinator(false).getDBCollection(SADCollections.SADJobData);

        BasicDBObject filter = new BasicDBObject("pluginName", pluginName);
        if (type == null) {

        } else if (type.trim().length() > 0) {
            filter.put("dataType", type);
        }
        BasicDBObject sorter = new BasicDBObject("whenCollected", -1);
        long startTime = System.currentTimeMillis();
        DBCursor cursor = collection.find(filter).sort(sorter).limit(limit);

        while (cursor.hasNext()) {
            DBObject object = cursor.next();
            SADJobData sadjd = dh.dBObjectToSADJobData(object);
            result.add(sadjd);
        }

        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        return result;
    }

    /**
     * @return SAD Job Data for all SAD Jobs from the database.
     */
    public ArrayList<SADJobData> getDataForAllJobs() {
        logger.debug("Returning data for all jobs");

        Datastore datastore = getCoordinator(false).getDatastore();
        DBCollection collection = datastore.getDB().getCollection("SADJobData");

        ArrayList<SADJobData> result = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        DBCursor cursor = collection.find().sort(new BasicDBObject("whenCollected", -1));

        while (cursor.hasNext()) {
            DBObject object = cursor.next();
            SADJobData jobData = dh.dBObjectToSADJobData(object);
            result.add(jobData);
        }

        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        return result;
    }

    /**
     * @return SAD Job Data for last created SAD Job from the database.
     */
    public ArrayList<SADJobData> getLastJobData() {
        logger.debug("Returning data for the last job");

        ArrayList<SADJobData> result = new ArrayList<>();
        // TODO: just get job ID
        long startTime = System.currentTimeMillis();
        SADJob lastJob = getLastJob();

        if (lastJob != null) {
            result = getDataForJobWithId(lastJob.getIdAsString());

        }

        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        return result;
    }

    /**
     * @param pluginName name of the plugin.
     * @param dataType data type.
     * @return SAD Execution Data for last created SAD Job from the database.
     */
    public JSONArray getLastExecutionDataForPlugin(String pluginName, String dataType) {
        logger.debug("Returning data for the last execution of plugin '" + pluginName + "' of data type: '" + dataType + "'");

        JSONArray result = new JSONArray();

        DBCollection collection = getCoordinator(false).getDBCollection(SADCollections.SADJobData);
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.append("pluginName", pluginName);
        searchQuery.append("dataType", dataType);

        // Find 10 unique executions sorted by date
//        List executionsList = collection.a("SADExecutionDatabaseID", searchQuery, );
//        BasicDBObject sort = new BasicDBObject("whenCollected", -1);
//        sort.append("jsonData.buzz_score", -1);
//        .sort(new BasicDBObject("jsonData.buzz_score", -1))
        long startTime = System.currentTimeMillis();
        DBCursor cursor = collection.find(searchQuery).sort(new BasicDBObject("whenCollected", -1)).limit(5);
//        logger.debug(cursor.toString());

        ArrayList<JSONObject> tempArrayList = new ArrayList<>();
        JSONObject next;
        while (cursor.hasNext()) {
            next = JSONObject.fromObject(cursor.next().toString()).getJSONObject("jsonData");
            tempArrayList.add(next);
//            logger.debug(next.toString(2));
        }

        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        JSONObject[] tempArray = tempArrayList.toArray(new JSONObject[tempArrayList.size()]);

        // Reverse order
        Collections.reverse(Arrays.asList(tempArray));

        for (int i = 0; i < tempArray.length; i++) {
            result.add(tempArray[i]);
        }

        logger.debug("Found " + tempArrayList.size() + " data entries for the last execution of plugin " + pluginName + "' of data type: '" + dataType + "'");

        return result;
    }

    /**
     * @return all SAD Job Executions from the database.
     */
    public ArrayList<SADJobExecution> getExecutions() {
        long startTime = System.currentTimeMillis();
        ArrayList<SADJobExecution> result = new ArrayList<>(getCoordinator(false).createQuery(SADJobExecution.class).order("-whenStarted").asList());
        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        return result;
    }

    /**
     * @return all failed SAD Job Executions from the database.
     */
    public ArrayList<SADJobExecution> getFailedExecutions() {
        long startTime = System.currentTimeMillis();
        ArrayList<SADJobExecution> result = new ArrayList<>(getCoordinator(false).createQuery(SADJobExecution.class).field("status").equal(ExecStatus.FAILED).asList());
        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        return result;
    }

    /**
     * @return all successful SAD Job Executions from the database.
     */
    public ArrayList<SADJobExecution> getSuccessfulExecutions() {
        long startTime = System.currentTimeMillis();
        ArrayList<SADJobExecution> result = new ArrayList<>(getCoordinator(false).createQuery(SADJobExecution.class).field("status").equal(ExecStatus.FINISHED).asList());
        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        return result;
    }

    /**
     * @param jobId ID of the job.
     * @return all SAD Jobs Executions by SAD Job ID.
     */
    public ArrayList<SADJobExecution> getExecutionsForJob(String jobId) {
        long startTime = System.currentTimeMillis();
        ArrayList<SADJobExecution> result = new ArrayList<>(getCoordinator(false).createQuery(SADJobExecution.class).field("SADJobID").equal(new ObjectId(jobId)).asList());
        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));
        return result;
    }

    /**
     * Executes SAD Workflow using Quartz.
     *
     * @param workflowName name of the workflow.
     * @param workflowAsJsonArray workflow configuration.
     * @param workflowSchedule workflow schedule.
     * @return metadata of the workflow.
     */
    public JSONObject runWorkflow(String workflowName, JSONArray workflowAsJsonArray, JSONObject workflowSchedule) {
        JSONObject result = new JSONObject();

        if (workflowAsJsonArray.isEmpty()) {
            logger.warn("Input workflow is empty: nothing to do");
            return result;

        } else {
            long startTime = System.currentTimeMillis();
            String workflowId = addWorkflow(workflowName, workflowAsJsonArray.toString(), workflowSchedule.toString());
            pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));
            int workflowIdAsInt = Integer.parseInt(workflowId);

            int numItemsInWorkflow = workflowAsJsonArray.size();

            String pathToPlugins = pluginsService.getAbsolutePluginsPath();
            JSONObject tempWorkflowItem, tempWorkflowItemPlugin, pluginInfo, workflowItemArguments, workflowItemSchedule;
            String workflowItemName, pluginName, jobId;
            for (int i = 0; i < numItemsInWorkflow; i++) {
                tempWorkflowItem = workflowAsJsonArray.getJSONObject(i);
                logger.debug("Workflow item [" + i + "]: " + tempWorkflowItem.toString(2));

                workflowItemName = (String) tempWorkflowItem.keySet().iterator().next();
                logger.debug("Workflow item [" + i + "] description: " + workflowItemName);

                tempWorkflowItemPlugin = tempWorkflowItem.getJSONObject(workflowItemName);
                logger.debug("Workflow item [" + i + "] plugin: " + tempWorkflowItemPlugin.toString(2));

                pluginName = tempWorkflowItemPlugin.getString("pluginName");
                pluginInfo = pluginsService.getPluginByName(pluginName);
                workflowItemArguments = tempWorkflowItemPlugin.getJSONObject("arguments");
                workflowItemSchedule = tempWorkflowItemPlugin.getJSONObject("schedule");

                if (pluginInfo.isEmpty()) {
                    throw new RuntimeException("Plugin not found: " + pluginName);
                }

                if (!pluginInfo.containsKey("paths")) {
                    throw new RuntimeException("Plugin configuration must contain \'paths\'. Misconfigured plugin: " + pluginName);
                }

                if (!pluginInfo.containsKey("pluginFolder")) {
                    throw new RuntimeException("Plugin configuration must be loaded with pluginsService that fills in the \'pluginFolder\' field. Misconfigured plugin: " + pluginName);
                }

                JSONObject pluginPaths = pluginInfo.getJSONObject("paths");

                if (!pluginPaths.containsKey("jar")) {
                    throw new RuntimeException("Plugin configuration must contain \'paths/jar\'. Misconfigured plugin: " + pluginName);
                }
                if (!pluginPaths.containsKey("dependenciesFolder")) {
                    throw new RuntimeException("Plugin configuration must contain \'paths/dependenciesFolder\'. Misconfigured plugin: " + pluginName);
                }

                String jarPath = pluginPaths.getString("jar");
                String dependenciesFolderPath = pluginPaths.getString("dependenciesFolder");
                String inputs = pluginInfo.getString("inputs");
                String outputs = pluginInfo.getString("outputs");

                logger.debug("Plugin jar path from configuration: " + jarPath + ", dependenciesFolderPath: " + dependenciesFolderPath);
                logger.debug("Plugin folder configuration: " + pathToPlugins);

                jobId = addJob(workflowId,
                        pluginName,
                        pluginInfo.toString(),
                        "Job for plugin " + pluginName + " run by workflow [" + workflowId + "]",
                        workflowItemArguments.toString(),
                        inputs,
                        outputs,
                        workflowItemSchedule.toString());

            }

            JobDetail job = JobBuilder.newJob(WorkflowRunner.class).withIdentity("workflow:" + workflowName, "id:" + workflowId)
                    .usingJobData("id", workflowId)
                    .usingJobData("pathToCoordinator", configurationService.getConfiguration().getCoordinatorPath())
                    .usingJobData("pathToPlugins", pathToPlugins)
                    .build();

            Trigger trigger = jsonScheduleToTrigger(workflowSchedule, workflowName, workflowId);

            try {
                scheduler.scheduleJob(job, trigger);
            } catch (SchedulerException ex) {
                throw new RuntimeException("Failed to schedule Quartz job", ex);
            }

            SADWorkflow workflow = getCoordinator(false).getDatastore().get(SADWorkflow.class, new ObjectId(workflowId));
            workflow.setStatus("scheduled");
            getCoordinator(false).saveObject(workflow);

            SADWorkflow theWorkflow = getWorkflow(workflowId);

            result = workflowAsJson(theWorkflow);

            return result;
        }

    }

    /**
     * For controllers to report methods called on them.
     *
     * @param methodName
     */
    public void pushMethodCalledName(String methodName) {
        try {
            if (emClient.isEmClientOK()) {
                if (emClient.isMetricModelSetup()) {
                    emClient.pushMethodCalledToEcc(methodName);
                }
            }

        } catch (Throwable ex) {
            logger.error("Failed to report method call to ECC", ex);
        }
    }

    /**
     * For controllers to report execution times.
     *
     * @param methodCallDuration
     */
    public void pushTimeSpent(String methodCallDuration) {
        try {
            if (emClient.isEmClientOK()) {
                if (emClient.isMetricModelSetup()) {
                    emClient.pushMethodCallDuration(methodCallDuration);
                }
            }

        } catch (Throwable ex) {
            logger.error("Failed to report method call duration to ECC", ex);
        }
    }

    /**
     * For controllers to report database query times.
     *
     * @param queryDuration
     */
    public void pushDatabaseQueryDuration(String queryDuration) {
        try {
            if (emClient.isEmClientOK()) {
                if (emClient.isMetricModelSetup()) {
                    emClient.pushDatabaseQueryDuration(queryDuration);
                }
            }

        } catch (Throwable ex) {
            logger.error("Failed to report database query duration to ECC", ex);
        }
    }

    /**
     * Executes SAD Plugin using Quartz.
     *
     * @param pluginName name of the plugin.
     * @param arguments JSON arguments.
     * @param inputs JSON inputs.
     * @param outputs JSON outputs.
     * @param schedule JSON schedule.
     * @return metadata of the new SAD job.
     */
    public JSONObject runPlugin(String pluginName, JSONArray arguments, JSONArray inputs, JSONArray outputs, JSONObject schedule) {

        logger.debug("Running plugin by name: " + pluginName + ", with arguments: " + arguments.toString() + ", schedule: " + schedule.toString());

        JSONObject pluginInfo = pluginsService.getPluginByName(pluginName);

        if (pluginInfo.isEmpty()) {
            throw new RuntimeException("Plugin not found: " + pluginName);
        }

        if (!pluginInfo.containsKey("paths")) {
            throw new RuntimeException("Plugin configuration must contain \'paths\'. Misconfigured plugin: " + pluginName);
        }

        if (!pluginInfo.containsKey("pluginFolder")) {
            throw new RuntimeException("Plugin configuration must be loaded with pluginsService that fills in the \'pluginFolder\' field. Misconfigured plugin: " + pluginName);
        }

        JSONObject pluginPaths = pluginInfo.getJSONObject("paths");

        if (!pluginPaths.containsKey("jar")) {
            throw new RuntimeException("Plugin configuration must contain \'paths/jar\'. Misconfigured plugin: " + pluginName);
        }
        if (!pluginPaths.containsKey("dependenciesFolder")) {
            throw new RuntimeException("Plugin configuration must contain \'paths/dependenciesFolder\'. Misconfigured plugin: " + pluginName);
        }

        String jarPath = pluginPaths.getString("jar");
        String dependenciesFolderPath = pluginPaths.getString("dependenciesFolder");
        String inputsAsString = inputs.toString();
        String outputsAsString = outputs.toString();

        logger.debug("Plugin jar path from configuration: " + jarPath + ", dependenciesFolderPath: " + dependenciesFolderPath);

        String pathToPlugins = pluginsService.getAbsolutePluginsPath();

        logger.debug("Plugin folder configuration: " + pathToPlugins);

        // Create database record of the new job
        String jobId = addJob(null,
                pluginName,
                pluginInfo.toString(),
                "Job for plugin " + pluginName,
                arguments.toString(),
                inputsAsString,
                outputsAsString,
                schedule.toString());

        // Report number of plugins and the name to ECC
        if (emClient.isEmClientOK()) {
            if (emClient.isMetricModelSetup()) {
                int jobsNum = getJobs().size();
                logger.debug("Reporting new jobs run number [" + jobsNum + "] and plugin name: '" + pluginName + "' to ECC");
                emClient.pushNumPluginsRunMeasurementToEcc(jobsNum);
                emClient.pushPluginNameMeasurementToEcc(pluginName);
            } else {
                logger.error("NOT reporting new jobs run number and plugin name: '" + pluginName + "' to ECC because metric model is not set up yet"
                        + " (start discovery phase in ECC or connect to a running experiment)");
            }
        }

        JobBuilder jobBuilder = JobBuilder.newJob(PluginRunner.class).withIdentity(pluginName, jobId)
                .usingJobData("pluginName", pluginName)
                .usingJobData("jar", jarPath)
                .usingJobData("dependenciesFolderPath", dependenciesFolderPath)
                .usingJobData("pathToPlugins", pathToPlugins)
                .usingJobData("pluginFolder", pluginInfo.getString("pluginFolder"))
                .usingJobData("pathToCoordinator", configurationService.getConfiguration().getCoordinatorPath())
                .usingJobData("eccPort", eccIntegrationService.getPortForPluginName(pluginName))
                .usingJobData("jobId", jobId);

        if (emClient.isEmClientOK()) {
            if (emClient.isMetricModelSetup()) {
                jobBuilder = jobBuilder
                        //                        .usingJobData("basepath", configurationService.getConfiguration().getBasepath() + "/report")
                        .usingJobData("basepath", "http://127.0.0.1:" + localPort + contextPath + "/report")
                        .usingJobData("emClientOK", emClient.isEmClientOK())
                        .usingJobData("isMetricModelSetup", emClient.isMetricModelSetup());
            }
        }

        JobDetail job = jobBuilder.build();

        Trigger trigger = jsonScheduleToTrigger(schedule, pluginName, jobId);

        // Need to deal with times: 0 or negative
        logger.debug("Submitting job with start time: " + trigger.getStartTime() + ", final fire time: " + trigger.getFinalFireTime());
        logger.debug("First 20 scheduled executions:");
        List<Date> scheduledExecutions_20 = TriggerUtils.computeFireTimes((OperableTrigger) trigger, new BaseCalendar(), 20);
//        JSONArray scheduledExecutions_20AsJson = new JSONArray();
        for (Date d : scheduledExecutions_20) {
            logger.debug("\t" + d.toString());
//            scheduledExecutions_20AsJson.add(d.toString());
        }

        try {
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException ex) {
            throw new RuntimeException("Failed to schedule Quartz job", ex);
        }

        long startTime = System.currentTimeMillis();

        SADJob sadJob = getJob(jobId);
        sadJob.setStatus(ExecStatus.SCHEDULED);
        getCoordinator(false).saveObject(sadJob);

        pushDatabaseQueryDuration(Long.toString(System.currentTimeMillis() - startTime));

        SADJob theJob = getJob(jobId);
        JSONObject result = jobAsJson(theJob);

        return result;

    }

    /**
     * Converts schedule from JSON string to Quartz trigger.
     *
     * @return Plugin schedule as Quartz trigger.
     */
    private Trigger jsonScheduleToTrigger(JSONObject schedule, String triggerName, String triggerId) {
        Trigger trigger;
        Date timeNow = new Date();

        String startAt = null, endAt = null, times = null, withIntervalInMilliseconds = null;
        if (schedule.containsKey("startAt")) {
            startAt = schedule.getString("startAt");
            logger.debug("Quartz scheduler got \"startAt\": \"" + startAt + "\" (" + Util.i2d(startAt) + ")");
        } else {
            logger.debug("Quartz scheduler parameter \"startAt\": NOT SET");
        }
        if (schedule.containsKey("endAt")) {
            endAt = schedule.getString("endAt");
            logger.debug("Quartz scheduler got \"endAt\": \"" + endAt + "\" (" + Util.i2d(endAt) + ")");
        } else {
            logger.debug("Quartz scheduler parameter \"endAt\": NOT SET");
        }
        if (schedule.containsKey("times")) {
            times = schedule.getString("times");
            logger.debug("Quartz scheduler got \"times\": \"" + times + "\"");
        } else {
            logger.debug("Quartz scheduler parameter \"withRepeatCount\": NOT SET");
        }
        if (schedule.containsKey("withIntervalInMilliseconds")) {
            withIntervalInMilliseconds = schedule.getString("withIntervalInMilliseconds");
            logger.debug("Quartz scheduler got \"withIntervalInMilliseconds\": \"" + withIntervalInMilliseconds + "\"");
        } else {
            logger.debug("Quartz scheduler parameter \"withIntervalInMilliseconds\": NOT SET");
        }

        SimpleScheduleBuilder quartzSchedule = SimpleScheduleBuilder.simpleSchedule();
        logger.debug("Time now: " + timeNow);
        if ((startAt != null) && (endAt != null)) {
            List<Date> triggerDates = TriggerUtils.computeFireTimesBetween(
                    (OperableTrigger) TriggerBuilder.newTrigger().build(),
                    new BaseCalendar(),
                    new Date(Long.parseLong(startAt)),
                    new Date(Long.parseLong(endAt))
            );
            int newRepeatCount = triggerDates.size();
//            logger.warn("Setting new value for \"withRepeatCount\" based on start and end dates: \"" + newRepeatCount + "\"");
            logger.warn("Setting new value for \"withRepeatCount\" based on start and end dates: \"" + newRepeatCount + "\"");
            quartzSchedule = quartzSchedule.withRepeatCount(newRepeatCount);
            logger.debug("New Schedule:");
            for (Date d : triggerDates) {
                logger.debug("\t" + d.toString());

            }
        } else if ((startAt == null) && (endAt != null)) {
            List<Date> triggerDates = TriggerUtils.computeFireTimesBetween(
                    (OperableTrigger) TriggerBuilder.newTrigger().build(),
                    new BaseCalendar(),
                    timeNow,
                    new Date(Long.parseLong(endAt))
            );
            int newRepeatCount = triggerDates.size();
//            logger.warn("Setting new value for \"withRepeatCount\" based on start and end dates: \"" + newRepeatCount + "\"");
            logger.warn("Setting new value for \"withRepeatCount\" based on start and end dates: \"" + newRepeatCount + "\"");
            quartzSchedule = quartzSchedule.withRepeatCount(newRepeatCount);
            logger.debug("New Schedule:");
            for (Date d : triggerDates) {
                logger.debug("\t" + d.toString());

            }
        } else {
            if (times == null) {
                logger.debug("Using DEFAULT value of 0 for \"withRepeatCount\" for quartz scheduler");
                quartzSchedule = quartzSchedule.withRepeatCount(0);
            } else {
                logger.debug("Using \"withRepeatCount\" value for quartz scheduler: \"" + (Integer.parseInt(times) - 1) + "\"");
                quartzSchedule = quartzSchedule.withRepeatCount(Integer.parseInt(times) - 1);
            }
        }

        if (withIntervalInMilliseconds == null) {
            logger.warn("Using DEFAULT value of 5 minutes (300000 msec) for \"withIntervalInMilliseconds\"!");
            quartzSchedule = quartzSchedule.withIntervalInMilliseconds(300000L);
        } else {
            logger.debug("Using \"withIntervalInMilliseconds\" value for quartz scheduler: \"" + withIntervalInMilliseconds + "\"");
            quartzSchedule = quartzSchedule.withIntervalInMilliseconds(Long.parseLong(withIntervalInMilliseconds));
        }

        if (startAt == null) {
            trigger = TriggerBuilder.newTrigger().withIdentity("trigger:" + triggerName, "job: " + triggerId)
                    .startNow()
                    .withSchedule(quartzSchedule)
                    .build();
        } else {
            trigger = TriggerBuilder.newTrigger().withIdentity("trigger:" + triggerName, "job: " + triggerId)
                    .startAt(new Date(Integer.parseInt(startAt)))
                    .withSchedule(quartzSchedule)
                    .build();
        }

        return trigger;
    }

    /**
     * Converts SAD job with ID (database representation of a SAD job) into JSON
     *
     * @param jobId - ID of the job
     * @return job as JSON
     */
    public JSONObject jobAsJson(int jobId) {
        return jobAsJson(getJob(Integer.toString(jobId)));
    }

    /**
     * Converts SADJob (database representation of a SAD job) into JSON
     *
     * @param theJob - database representation of the job
     * @return job as JSON
     */
    public JSONObject jobAsJson(SADJob theJob) {
        JSONObject jobAsJson = new JSONObject();
        Timestamp tempTimestamp;
        ArrayList<SADJobExecution> executionsForJob;
        String jobId;
        String basePath = configurationService.getConfiguration().getBasepath();

        jobId = theJob.getIdAsString();
        jobAsJson.put("ID", jobId);
        jobAsJson.put("PluginName", theJob.getPluginName());
        jobAsJson.put("Name", theJob.getName());
        jobAsJson.put("Description", theJob.getDescription());
        jobAsJson.put("Arguments", JSONArray.fromObject(theJob.getArguments()));
        jobAsJson.put("Inputs", JSONArray.fromObject(theJob.getInputs()));
        jobAsJson.put("Outputs", JSONArray.fromObject(theJob.getOutputs()));
        jobAsJson.put("Schedule", JSONObject.fromObject(theJob.getSchedule()));
        jobAsJson.put("Status", theJob.getStatus());

        tempTimestamp = theJob.getWhenCreated();
        if (tempTimestamp == null) {
            jobAsJson.put("WhenCreated_as_string", "");
            jobAsJson.put("WhenCreated_in_msec", "");
        } else {
            jobAsJson.put("WhenCreated_as_string", Util.timestampToString(tempTimestamp));
            jobAsJson.put("WhenCreated_in_msec", tempTimestamp.getTime());
        }

        tempTimestamp = theJob.getWhenLastrun();
        if (tempTimestamp == null) {
            jobAsJson.put("WhenLastrun_as_string", "");
            jobAsJson.put("WhenLastrun_in_msec", "");
        } else {
            jobAsJson.put("WhenLastrun_as_string", Util.timestampToString(tempTimestamp));
            jobAsJson.put("WhenLastrun_in_msec", tempTimestamp.getTime());
        }

        executionsForJob = getExecutionsForJob(jobId);
        if (executionsForJob.isEmpty()) {
            jobAsJson.put("Executions_num", 0);
        } else {
            jobAsJson.put("Executions_num", executionsForJob.size());

        }

        jobAsJson.put("data_num", getDataNumForJob(theJob));
        jobAsJson.put("url_details", (basePath != null ? basePath : "") + "/jobs?jobId=" + jobId);
        jobAsJson.put("url_pause", (basePath != null ? basePath : "") + "/jobs?jobId=" + jobId + "&action=pause");
        jobAsJson.put("url_cancel", (basePath != null ? basePath : "") + "/jobs?jobId=" + jobId + "&action=cancel");
        jobAsJson.put("url_resume", (basePath != null ? basePath : "") + "/jobs?jobId=" + jobId + "&action=resume");
        jobAsJson.put("url_delete", (basePath != null ? basePath : "") + "/jobs?jobId=" + jobId + "&action=delete");

        jobAsJson.put("url_data", (basePath != null ? basePath : "") + "/jobs/" + jobId + "/data");
        jobAsJson.put("url_vis", (basePath != null ? basePath : "") + "/visualise/" + theJob.getPluginName() + "/data.html?jobid=" + jobId + "&num_results=20");

        return jobAsJson;
    }

    /**
     * Returns the number of data entries collected by the job.
     *
     * @param job
     * @return
     */
    public int getDataNumForJob(SADJob job) {
        return getCoordinator(false).getDBCollection(SADCollections.SADJobData).find(new BasicDBObject("SADJobID", job.getId())).count();
    }

    /**
     * Converts SADWorkflow (database representation of a SAD workflow) into
     * JSON
     *
     * @param theWorkflow - database representation of the workflow
     * @return workflow as JSON
     */
    public JSONObject workflowAsJson(SADWorkflow theWorkflow) {

        JSONObject workflowAsJson = new JSONObject();
        String workflowId = theWorkflow.getIdAsString();

        workflowAsJson.put("name", theWorkflow.getName());
        workflowAsJson.put("id", workflowId);
        workflowAsJson.put("status", theWorkflow.getStatus());
        workflowAsJson.put("plugins", theWorkflow.getPluginsAsJsonString());
        workflowAsJson.put("schedule", theWorkflow.getScheduleAsJsonString());

        String basePath = configurationService.getConfiguration().getBasepath();

        workflowAsJson.put("status_url", (basePath != null ? basePath : "") + "/service/workflows/" + workflowId);

        return workflowAsJson;
    }

}
