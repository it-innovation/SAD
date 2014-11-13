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
//	Created Date :			2013-03-15
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.sad.coordinator.Coordinator;
import uk.ac.soton.itinnovation.sad.service.dao.SADJob;
import uk.ac.soton.itinnovation.sad.service.dao.SADJobExecution;
import uk.ac.soton.itinnovation.sad.service.dao.SADWorkflow;
import uk.ac.soton.itinnovation.sad.service.domain.ExecStatus;

/**
 * Custom Quartz workflow runner.
 */
@PersistJobDataAfterExecution
public class WorkflowRunner implements Job {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String fileSeparator = System.getProperty("file.separator");
    private Coordinator coordinator;

    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        String workflowId = jec.getJobDetail().getJobDataMap().getString("id");
        String pathToCoordinator = jec.getJobDetail().getJobDataMap().getString("pathToCoordinator");
        String pathToPlugins = jec.getJobDetail().getJobDataMap().getString("pathToPlugins");

        logger.debug("Running workflow [" + workflowId + "] with coordinator path: " + pathToCoordinator);

        coordinator = new Coordinator(pathToCoordinator);

        if (!coordinator.isDatabaseInitialised()) {
            coordinator.setupDatabase();
        }

        // Only set status to running if it's the first run and we have not already failed
        if (jec.getPreviousFireTime() == null) {
//                ArrayList<SADWorkflow> myWorkflowsRecent = coordinator.getMgtSchema().getAllWhere(new SADWorkflow(), "ID", Integer.parseInt(workflowId));
            List<SADWorkflow> myWorkflowsRecent = coordinator.createQuery(SADWorkflow.class).field("_id").equal(workflowId).asList();

            if (!myWorkflowsRecent.isEmpty()) {
                SADWorkflow myWorkflow = myWorkflowsRecent.get(0);
                if (!myWorkflow.getStatus().equalsIgnoreCase("failed")) {
                    logger.debug("Setting workflow [" + workflowId + "] status to \'running\'");
//                        coordinator.getMgtSchema().updateRow(new SADWorkflow(), "Status", "running", "ID", Integer.parseInt(workflowId));
                    myWorkflow.setStatus("running");
                    coordinator.saveObject(myWorkflow);
                }

            } else {
                logger.error("Failed to locate workflow [" + workflowId + "] in the database");
            }
        }

        List<SADJob> workflowJobs;
        try {
//            workflowJobs = coordinator.getMgtSchema().getAllWhere(new SADJob(), "SADWorkflowID", Integer.parseInt(workflowId));
            workflowJobs = coordinator.createQuery(SADJob.class).field("SADWorkflowID").equal(workflowId).asList();
        } catch (Throwable ex) {
            throw new RuntimeException("Failed to retrieve SAD Jobs for workflow [" + workflowId + "]", ex);
        }

        String pluginToRun, pluginJarPath, pluginDependenciesFolder, pluginFolder;
        String jobId;
        JSONObject pluginConfigurationAsJson, pluginPaths;
        boolean workflowSuccess = true;
        if (!workflowJobs.isEmpty()) {
            for (Iterator<SADJob> it = workflowJobs.iterator(); it.hasNext();) {
                SADJob job = it.next();

                jobId = job.getId().toString();
                pluginToRun = job.getPluginName();
                pluginConfigurationAsJson = JSONObject.fromObject(job.getPluginConfigurationAsJsonString());
                pluginPaths = pluginConfigurationAsJson.getJSONObject("paths");
                pluginJarPath = pluginPaths.getString("jar");
                pluginDependenciesFolder = pluginPaths.getString("dependenciesFolder");
                pluginFolder = pluginConfigurationAsJson.getString("pluginFolder");

//                String command = "java " +
//                        " -cp \"" + pathToPlugins + fileSeparator + pluginFolder + fileSeparator + pluginDependenciesFolder + fileSeparator + "*\"" +
//                        " -jar \"" + pathToPlugins + fileSeparator + pluginFolder + fileSeparator + pluginJarPath + "\" \"" + pathToCoordinator + "\" " + jobId;
                // To avoid Java bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6511002
                // Explanation: http://stackoverflow.com/questions/697621/spaces-in-java-execute-path-for-os-x
                String[] command = new String[]{
                    "java",
                    "-cp",
                    pathToPlugins + fileSeparator + pluginFolder + fileSeparator + pluginDependenciesFolder + fileSeparator + "*",
                    "-jar",
                    pathToPlugins + fileSeparator + pluginFolder + fileSeparator + pluginJarPath,
                    pathToCoordinator,
                    jobId
                };

                if (!executeSingleJar(jobId, pluginToRun, command)) {
                    workflowSuccess = false;
                }
            }
        }

        try {
            if (workflowSuccess) {
                // Only set status to success if it's a last run and we have not already failed
                if (jec.getNextFireTime() == null) {
//                    ArrayList<SADWorkflow> myWorkflowsRecent = coordinator.getMgtSchema().getAllWhere(new SADWorkflow(), "ID", Integer.parseInt(workflowId));
                    List<SADWorkflow> myWorkflowsRecent = coordinator.createQuery(SADWorkflow.class).field("_id").equal(workflowId).asList();

                    if (!myWorkflowsRecent.isEmpty()) {
                        SADWorkflow myWorkflow = myWorkflowsRecent.get(0);
                        if (!myWorkflow.getStatus().equalsIgnoreCase("failed")) {
                            logger.debug("Setting workflow [" + workflowId + "] status to \'success\'");
//                            coordinator.getMgtSchema().updateRow(new SADWorkflow(), "Status", "success", "ID", Integer.parseInt(workflowId));
                            myWorkflow.setStatus(ExecStatus.FINISHED);
                            coordinator.saveObject(myWorkflow);
                        }

                    } else {
                        logger.error("Failed to locate workflow [" + workflowId + "] in the database");
                    }

                }
            } else {
                logger.debug("Setting workflow [" + workflowId + "] status to \'failed\'");
                // TODO:
//                coordinator.getMgtSchema().updateRow(new SADWorkflow(), "Status", "failed", "ID", Integer.parseInt(workflowId));
            }
        } catch (Throwable ex) {
            logger.error("Failed to update workflow [" + workflowId + "] status in the database", ex);
        }
    }

    /**
     * Executes single plugin JAR with appropriate database updates and failure
     * handling.
     *
     * @param jobId database ID of the corresponding SADJob.
     * @param command full java plugin command line with all arguments.
     * @return true if the jar was executed successfully.
     */
    private boolean executeSingleJar(String jobId, String pluginToRun, String[] command) {
        boolean success = false;

        StringBuilder pluginStdoutLog = new StringBuilder("");
        StringBuilder pluginErrorLog = new StringBuilder("");
        String stdOut, errOut;
        String commandAsString;

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < command.length; i++) {
            result.append(command[i]);
            result.append(" ");
        }
        commandAsString = result.toString();

        logger.debug("Executing plugin '" + pluginToRun + "' as job [" + jobId + "] with command (as an array, not plain string): " + commandAsString);

        SADJob currentJob = coordinator.createQuery(SADJob.class).field("_id").equal(new ObjectId(jobId)).get();

        try {

//            coordinator.getMgtSchema().updateRow(new SADJob(), "Status", "running", "ID", jobId);
            currentJob.setStatus(ExecStatus.RUNNING);
            coordinator.saveObject(currentJob);

            Process p = Runtime.getRuntime().exec(command);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            while ((stdOut = stdInput.readLine()) != null) {
                pluginStdoutLog.append(stdOut);
                pluginStdoutLog.append("\n");

            }

            while ((errOut = stdError.readLine()) != null) {
                pluginErrorLog.append(errOut);
                pluginErrorLog.append("\n");

            }

            p.waitFor();

            if (p.exitValue() == 0) {
                success = true;
            }

        } catch (IOException | InterruptedException ex) {
            logger.error("Failed to execute plugin \'" + pluginToRun + "\'", ex);
        }

//        coordinator.getMgtSchema().updateRow(new SADJob(), "Lastrun", new Timestamp(System.currentTimeMillis()), "ID", jobId);
        currentJob.setWhenLastrun(new Timestamp(System.currentTimeMillis()));
        coordinator.saveObject(currentJob);

        if (success) {
            logger.debug("Job executed successfully");
            currentJob.setStatus(ExecStatus.FINISHED);
            coordinator.saveObject(currentJob);

        } else {
            logger.debug("Job execution failed");
            // Update job's status to failed
//                coordinator.getMgtSchema().updateRow(new SADJob(), "Status", "failed", "ID", jobId);
            currentJob.setStatus(ExecStatus.FAILED);
            coordinator.saveObject(currentJob);

            // Update last execution's status to failed
            HashMap<String, Object> map = new HashMap<>();
            map.put("SADJobID", jobId);
//                ArrayList<SADJobExecution> jobExecutions = coordinator.getMgtSchema().getAllWhereSortByAsc(new SADJobExecution(), map, "ID");
            List<SADJobExecution> jobExecutions = coordinator.createQuery(SADJobExecution.class).order("whenStarted").asList();
            if (!jobExecutions.isEmpty()) {
                SADJobExecution jobExecution = jobExecutions.get(0);
//                    int executionToUpdateId = jobExecutions.get(0).getDatabaseID();
//                    coordinator.getMgtSchema().updateRow(new SADJobExecution(), "Status", "failed", "DatabaseID", executionToUpdateId);
//                    coordinator.getMgtSchema().updateRow(new SADJobExecution(), "Finished", new Timestamp(System.currentTimeMillis()), "DatabaseID", executionToUpdateId);

                jobExecution.setStatus("failed");
                jobExecution.setWhenFinished(new Timestamp(System.currentTimeMillis()));
                coordinator.saveObject(jobExecution);

            }
        }

        logger.debug("Stdout output:\n" + pluginStdoutLog.toString());
        logger.debug("Stderror:\n" + pluginErrorLog.toString());

//        try {
//            coordinator.getMgtSchema().insertObject(new SADJobStdOut(jobId, pluginStdoutLog.toString(), new Timestamp(System.currentTimeMillis())));
//        } catch (SQLException ex) {
//            logger.error("Failed to store job standard output in the database", ex);
//        }
//
//        try {
//            coordinator.getMgtSchema().insertObject(new SADJobStdError(jobId, pluginErrorLog.toString(), new Timestamp(System.currentTimeMillis())));
//        } catch (SQLException ex) {
//            logger.error("Failed to store job standard output in the database", ex);
//        }
        return success;
    }
}
