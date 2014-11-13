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
//	Created Date :			2013-01-14
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import net.sf.json.JSONException;
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
import uk.ac.soton.itinnovation.sad.service.domain.ExecStatus;

/**
 * Custom Quartz plugin runner.
 */
@PersistJobDataAfterExecution
public class PluginRunner implements Job {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String fileSeparator = System.getProperty("file.separator");
    private Coordinator coordinator;
    private EccReportsHelper eccReportsHelper;
    private boolean emClientOK;
    private boolean isMetricModelSetup;
    private String numPluginsFailedMeasurementSetUuid, numPluginsSuccessMeasurementSetUuid, pluginExecutionDurationMeasurementSetUuid;

    /**
     * Runs a plugin based on Quartz Job context.
     *
     * @param jec
     * @throws org.quartz.JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {

        String pluginToRun = jec.getJobDetail().getJobDataMap().getString("pluginName");
        String pluginJarPath = jec.getJobDetail().getJobDataMap().getString("jar");
        String pluginDependenciesFolder = jec.getJobDetail().getJobDataMap().getString("dependenciesFolderPath");
        String pathToPlugins = jec.getJobDetail().getJobDataMap().getString("pathToPlugins");
        String pluginFolder = jec.getJobDetail().getJobDataMap().getString("pluginFolder");
        String eccPort = jec.getJobDetail().getJobDataMap().getString("eccPort");
        if (eccPort == null) { // Runtime.getRuntime().exec() doesn't like nulls.
            eccPort = "null";
        }
        String basepath = jec.getJobDetail().getJobDataMap().getString("basepath");
        emClientOK = jec.getJobDetail().getJobDataMap().getBoolean("emClientOK");
        isMetricModelSetup = jec.getJobDetail().getJobDataMap().getBoolean("isMetricModelSetup");

        // Only report to ECC if ECC is enabled
        if (emClientOK) {
            if (isMetricModelSetup) {
                eccReportsHelper = new EccReportsHelper(basepath);
                if (eccReportsHelper == null) {
                    logger.error("Failed to create EccReportsHelper to report SAD metrics from the Plugin Runner");
                } else {
                    JSONObject uuids = eccReportsHelper.getPluginRunnerMeasurementSetsUuids();
                    if (uuids == null) {
                        logger.error("Failed to get measurement sets UUIDs from the service");
                    } else {
                        try {
                            numPluginsSuccessMeasurementSetUuid = uuids.getString("numPluginsSuccessMeasurementSetUuid");
                        } catch (JSONException e1) {
                            logger.warn("Error retriving numPluginsSuccessMeasurementSetUuid value:" + e1.getMessage());
                        }
                        if (numPluginsSuccessMeasurementSetUuid == null) {
                            logger.warn("numPluginsSuccessMeasurementSetUuid is NULL");
                        }
                        try {
                            numPluginsFailedMeasurementSetUuid = uuids.getString("numPluginsFailedMeasurementSetUuid");
                        } catch (JSONException e1) {
                            logger.warn("Error retriving numPluginsFailedMeasurementSetUuid value:" + e1.getMessage());
                        }
                        if (numPluginsFailedMeasurementSetUuid == null) {
                            logger.warn("numPluginsFailedMeasurementSetUuid is NULL");
                        }
                        try {
                            pluginExecutionDurationMeasurementSetUuid = uuids.getString("pluginExecutionDurationMeasurementSetUuid");
                        } catch (JSONException e1) {
                            logger.warn("Error retriving pluginExecutionDurationMeasurementSetUuid value:" + e1.getMessage());
                        }
                        if (pluginExecutionDurationMeasurementSetUuid == null) {
                            logger.warn("pluginExecutionDurationMeasurementSetUuid is NULL");
                        }
                    }
                }
            } else {
                logger.error("NOT pushing reports to ECC as metric model has not been set up yet.");
            }
        }

        String jobId = jec.getJobDetail().getJobDataMap().getString("jobId");
        String pathToCoordinator = jec.getJobDetail().getJobDataMap().getString("pathToCoordinator");

        try {
            coordinator = new Coordinator(pathToCoordinator);
        } catch (Throwable ex) {
            throw new RuntimeException("Failed to initialise SAD Coordinator", ex);
        }

        if (!coordinator.isDatabaseInitialised()) {
            coordinator.setupDatabase();
        }

        // To avoid Java bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6511002
        // Explanation: http://stackoverflow.com/questions/697621/spaces-in-java-execute-path-for-os-x
        String[] command = new String[]{
            "java",
            "-cp",
            pathToPlugins + fileSeparator + pluginFolder + fileSeparator + pluginDependenciesFolder + fileSeparator + "*",
            "-jar",
            pathToPlugins + fileSeparator + pluginFolder + fileSeparator + pluginJarPath,
            "execute",
            pathToCoordinator,
            jobId,
            eccPort
        };

        executeSingleJar(jobId, pluginToRun, command, jec);

    }

    /**
     * Executes single plugin JAR with appropriate database updates and failure
     * handling.
     *
     * @param jobId database ID of the corresponding SADJob.
     * @param command full java plugin command line with all arguments.
     * @return true if the jar was executed successfully.
     */
    private boolean executeSingleJar(String jobId, String pluginToRun, String[] command, JobExecutionContext jec) {
        boolean success = false;

        StringBuilder pluginStdoutLog = new StringBuilder("");
        StringBuilder pluginErrorLog = new StringBuilder("");
        String stdOut, errOut;
        PrintWriter stdOutAsFile = null, errOutAsFile = null;
        File stdOutTempFile = null, stdErrTempFile = null;
        String commandAsString;

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < command.length; i++) {
            result.append(command[i]);
            result.append(" ");
        }
        commandAsString = result.toString();

        logger.debug("Executing plugin '" + pluginToRun + "' as job [" + jobId + "] with command (as an array, not plain string): " + commandAsString);
        Date now = new Date();
        long startTime = System.currentTimeMillis(), executionDuration;
        try {
            stdOutTempFile = File.createTempFile("log-" + pluginToRun + "-" + now.getTime(), ".txt");
            stdOutAsFile = new PrintWriter(new FileWriter(stdOutTempFile));
            logger.debug("STD OUT path: " + stdOutTempFile.getAbsolutePath());
        } catch (FileNotFoundException ex) {
            logger.error("Failed to initialise plugin file std out log, no file logging", ex);
        } catch (IOException ex) {
            logger.error("Failed to initialise plugin temporary file for std out log, no file logging", ex);
        }
        try {
            stdErrTempFile = File.createTempFile("error-" + pluginToRun + "-" + now.getTime(), ".txt");
            errOutAsFile = new PrintWriter(new FileWriter(stdErrTempFile));
            logger.debug("STD ERR path: " + stdErrTempFile.getAbsolutePath());
        } catch (FileNotFoundException ex) {
            logger.debug("Failed to initialise plugin file err out log, no file logging", ex);
        } catch (IOException ex) {
            logger.error("Failed to initialise plugin temporary file for err out log, no file logging", ex);
        }

        SADJob theJob = coordinator.getDatastore().get(SADJob.class, new ObjectId(jobId));

        try {

            theJob.setStatus(ExecStatus.RUNNING);
            coordinator.saveObject(theJob);

            Process p = Runtime.getRuntime().exec(command);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            while ((stdOut = stdInput.readLine()) != null) {
                pluginStdoutLog.append(stdOut);
                pluginStdoutLog.append("\n");
                if (stdOutAsFile != null) {
                    stdOutAsFile.println(stdOut);
                    stdOutAsFile.flush();
                }
            }

            while ((errOut = stdError.readLine()) != null) {
                pluginErrorLog.append(errOut);
                pluginErrorLog.append("\n");
                if (errOutAsFile != null) {
                    errOutAsFile.println(errOut);
                    errOutAsFile.flush();
                }
            }

            p.waitFor();

            if (p.exitValue() == 0) {
                success = true;
            }

        } catch (IOException | InterruptedException ex) {
            logger.error("Failed to execute plugin \'" + pluginToRun + "\'", ex);
        } finally {
            if (stdOutAsFile != null) {
                stdOutAsFile.flush();
                stdOutAsFile.close();
            }
            if (errOutAsFile != null) {
                errOutAsFile.flush();
                errOutAsFile.close();
            }
        }

        executionDuration = System.currentTimeMillis() - startTime;

        theJob.setWhenLastrun(new Timestamp(System.currentTimeMillis()));
        coordinator.saveObject(theJob);

        if (emClientOK) {
            if (isMetricModelSetup) {
                if (pluginExecutionDurationMeasurementSetUuid != null) {
                    eccReportsHelper.reportMeasurement(pluginExecutionDurationMeasurementSetUuid, Long.toString(executionDuration));
                } else {
                    logger.error("Will not submit ExecutionDurationMeasurement as corresponding measurement set UUID is NULL");
                }
            }
        }

        if (success) {
            logger.debug("Job executed successfully");
            if (jec.getTrigger().getNextFireTime() == null) {
                theJob.setStatus(ExecStatus.FINISHED);
                coordinator.saveObject(theJob);
            }

            if (emClientOK) {
                if (isMetricModelSetup) {
                    try {
                        String numFinished = String.valueOf(coordinator.createQuery(SADJobExecution.class).field("status").equal(ExecStatus.FINISHED).countAll());
                        if (numPluginsSuccessMeasurementSetUuid != null) {
                            eccReportsHelper.reportMeasurement(numPluginsSuccessMeasurementSetUuid, numFinished);
                        } else {
                            logger.error("Will not submit PluginsSuccessMeasurement as corresponding measurement set UUID is NULL");
                        }
                        if (numPluginsFailedMeasurementSetUuid != null) {
                            String numFailed = String.valueOf(coordinator.createQuery(SADJobExecution.class).field("status").equal(ExecStatus.FAILED).countAll());
                            eccReportsHelper.reportMeasurement(numPluginsFailedMeasurementSetUuid, numFailed);
                        } else {
                            logger.error("Will not submit PluginsFailedMeasurement as corresponding measurement set UUID is NULL");
                        }
                    } catch (Throwable ex) {
                        logger.error("Failed to report number of successful/failed job executions to ECC", ex);
                    }
                }
            }

        } else {
            logger.debug("Job execution failed");
            // Update job's status to failed
            theJob.setStatus(ExecStatus.FAILED);
            coordinator.saveObject(theJob);

            // Update last execution's status to failed
            List<SADJobExecution> jobExecutions = coordinator.createQuery(SADJobExecution.class).field("SADJobID").equal(new ObjectId(jobId)).order("-whenStarted").limit(1).asList();

            if (!jobExecutions.isEmpty()) {
                SADJobExecution execution = jobExecutions.get(0);
                execution.setStatus(ExecStatus.FAILED);
                execution.setWhenFinished(new Timestamp(System.currentTimeMillis()));
                coordinator.saveObject(execution);

            }

            if (emClientOK) {
                if (isMetricModelSetup) {
                    try {
                        if (numPluginsSuccessMeasurementSetUuid != null) {
                            String numFinished = String.valueOf(coordinator.createQuery(SADJobExecution.class).field("status").equal(ExecStatus.FINISHED).countAll());
                            eccReportsHelper.reportMeasurement(numPluginsSuccessMeasurementSetUuid, numFinished);
                        } else {
                            logger.error("Will not submit PluginsSuccessMeasurement as corresponding measurement set UUID is NULL");
                        }
                        if (numPluginsFailedMeasurementSetUuid != null) {
                            String numFailed = String.valueOf(coordinator.createQuery(SADJobExecution.class).field("status").equal(ExecStatus.FAILED).countAll());
                            eccReportsHelper.reportMeasurement(numPluginsFailedMeasurementSetUuid, numFailed);
                        } else {
                            logger.error("Will not submit PluginsFailedMeasurement as corresponding measurement set UUID is NULL");
                        }
                    } catch (Throwable ex) {
                        logger.error("Failed to report number of failed/successful job executions to ECC", ex);
                    }
                }
            }

        }

        logger.debug("Stdout output:\n" + pluginStdoutLog.toString());
        logger.debug("Stderror:\n" + pluginErrorLog.toString());

        if (jec.getTrigger().getNextFireTime() == null) {
            logger.debug("JOB EXECUTION FINISHED");
        } else {
            logger.debug("Final fire time: " + jec.getTrigger().getFinalFireTime());
        }

        if (stdOutTempFile != null) {
            logger.debug("STD OUT path again: " + stdOutTempFile.getAbsolutePath());
        }
        if (stdErrTempFile != null) {
            logger.debug("STD ERR path again: " + stdErrTempFile.getAbsolutePath());
        }

        coordinator.closeMongo();

        return success;
    }

}
