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
//	Created Date :			2013-03-21
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.services;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import javax.annotation.PostConstruct;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.amqpAPI.impl.amqp.AMQPBasicChannel;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.amqpAPI.impl.amqp.AMQPConnectionFactory;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.experiment.Experiment;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.Attribute;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.Entity;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.Measurement;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MeasurementSet;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.Metric;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MetricGenerator;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MetricGroup;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MetricHelper;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MetricType;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.Report;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.Unit;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.monitor.EMDataBatch;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.monitor.EMPhase;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.monitor.EMPostReportSummary;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.samples.shared.EMIAdapterListener;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.samples.shared.EMInterfaceAdapter;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.samples.shared.ITakeMeasurement;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.samples.shared.MeasurementTask;
import uk.ac.soton.itinnovation.sad.service.configuration.EccConfiguration;
import uk.ac.soton.itinnovation.sad.service.configuration.SadConfiguration;

/**
 * SAD EMClient, based on
 * uk.ac.soton.itinnovation.experimedia.arch.ecc.samples.headlessECCClient.ECCHeadlessClient
 */
@Service("EMClient")
public class EMClient implements EMIAdapterListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String[] expectedEccProperties = new String[]{"enabled", "Rabbit_IP", "Rabbit_Port", "Monitor_ID", "Client_Name"};
    private static final String[] expectedEdmProperties = new String[]{"enabled", "dbURL", "dbName", "dbUsername", "dbPassword", "dbType"};

    // Connection to ECC
    private AMQPBasicChannel amqpChannel;
    private EMInterfaceAdapter emiAdapter;

    // Experiment and measurement information
    private HashMap<UUID, MetricGenerator> metricGenerators = new HashMap<>();
    private Experiment theExperiment;
    private HashMap<UUID, MeasurementSet> measurementSetMap = new HashMap<>();
    private HashSet<MeasurementTask> scheduledMeasurementTasks = new HashSet<>();
    private HashMap<UUID, ITakeMeasurement> instantMeasurers = new HashMap<>();

    private MetricGenerator theMetricGenerator;
    private UUID numPluginsRunMeasurementSetUuid;
    private UUID numPluginsFailedMeasurementSetUuid;
    private UUID numPluginsSuccessMeasurementSetUuid;
    private UUID pluginNameMeasurementSetUuid;
    private UUID methodsCalledMeasurementSetUuid;
    private UUID timeSpentOnServiceCallMeasurementSetUuid;
    private UUID timeSpentOnDatabaseQueryMeasurementSetUuid;
    private UUID pluginExecutionDurationMeasurementSetUuid;

    private HashMap<UUID, Report> pendingPushReports = new HashMap<>();
    private HashMap<UUID, Report> pendingPullReports = new HashMap<>();
    private AMQPConnectionFactory amqpFactory;

    private boolean emClientOK = false;
    private boolean localEdmOK = false;
    private boolean measurementSchedulerOK = false;
    private boolean pushingEnabled = false;
    private boolean metricsModelSetup = false;

    // Client state
    private boolean deRegisteringFromEM = false;

    @Autowired
    @Qualifier("configurationService")
    ConfigurationService configurationService;

    @Autowired
    @Qualifier("schedulingService")
    SchedulingService schedulingService;

    public EMClient() {

    }

    @PostConstruct
    public void init() {
    }

    public boolean start() {

        // Connect to EM
        logger.debug("Creating new SAD EM Client ...");

        metricGenerators = new HashMap<>();
        measurementSetMap = new HashMap<>();
        scheduledMeasurementTasks = new HashSet<>();
        instantMeasurers = new HashMap<>();
        pendingPushReports = new HashMap<>();
        pendingPullReports = new HashMap<>();

        emClientOK = false;
        localEdmOK = false;
        measurementSchedulerOK = false;
        pushingEnabled = false;
        metricsModelSetup = false;

        SadConfiguration configuration = configurationService.getConfiguration();
        EccConfiguration eccConfiguration = configuration.getEcc();

        if (!configuration.isEccEnabled()) {
            logger.debug("ECC integration disabled in configuration. Stopped creating SAD EM client");

        } else {
            logger.debug("Using ECC SAD configuration: " + JSONObject.fromObject(eccConfiguration).toString(2));

            String rabbitServerIP = eccConfiguration.getRabbitIp();
            int rabbitServerPort = Integer.parseInt(eccConfiguration.getRabbitPort());
            UUID expMonitorID = UUID.fromString(eccConfiguration.getMonitorId());
            String hostName = "";
            try {
                hostName = " - " + InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException uhe) {
            }
            String clientName = eccConfiguration.getClientName() + hostName;
            UUID clientID = UUID.fromString(eccConfiguration.getClientsUuuidSeed() + "0");
            logger.debug("Using seeded UUID: " + clientID.toString());

            amqpFactory = new AMQPConnectionFactory();
            amqpFactory.setAMQPHostIPAddress(rabbitServerIP);
            amqpFactory.setAMQPHostPort(rabbitServerPort);

            try {
                amqpFactory.connectToAMQPHost();
                amqpChannel = amqpFactory.createNewChannel();
            } catch (Throwable ex) {
                throw new RuntimeException("Failed to connect to Rabbit server [" + rabbitServerIP + ":" + rabbitServerPort + "]", ex);
            }

            emiAdapter = new EMInterfaceAdapter(this);

            try {
                emiAdapter.registerWithEM(clientName, amqpChannel, expMonitorID, clientID);
            } catch (Throwable ex) {
                throw new RuntimeException("Failed to register client [" + clientID + "] '" + clientName + "' with EM using experiment monitor ID [" + expMonitorID + "]", ex);
            }

            logger.debug("Successfully created new SAD EM Client");
            emClientOK = true;
        }

        logger.debug("EM Client init results:");
        logger.debug("\t- emClientOK: " + emClientOK);
        logger.debug("\t- localEdmOK: " + localEdmOK);
        logger.debug("\t- measurementSchedulerOK: " + measurementSchedulerOK);

        return true;

    }

    public void stop() {
        logger.debug("Stopping SAD ECC client");
        deRegisterFromEM();
    }

    /**
     * If EM Client was successfully created check.
     *
     * @return true if EM Client was successfully created.
     */
    public boolean isEmClientOK() {
        return emClientOK;
    }

    /**
     * If local EDM was successfully initialized.
     *
     * @return true if local EDM was successfully initialized.
     */
    public boolean isLocalEdmOK() {
        return localEdmOK;
    }

    /**
     * If ECC measurement scheduler was successfully created.
     *
     * @return true if ECC measurement scheduler was successfully created.
     */
    public boolean isMeasurementSchedulerOK() {
        return measurementSchedulerOK;
    }

    /**
     * If ECC enabled pushing.
     *
     * @return true if ECC enabled pushing.
     */
    public boolean isPushingEnabled() {
        return pushingEnabled;
    }

    /**
     * If ECC metric model was created (UUIDs for measurement reports have been
     * set).
     *
     * @return true if ECC metric model was created.
     */
    public boolean isMetricModelSetup() {
        return metricsModelSetup;
    }

    /**
     * Converts JSON into Properties object.
     */
    private Properties jsonToProperties(JSONObject propertiesAsJson) {
        Properties result = new Properties();

        logger.debug("Converting JSON to Properties: " + propertiesAsJson.toString(2));

        Iterator<String> it = propertiesAsJson.keys();
        String propertyName, propertyValue;
        while (it.hasNext()) {
            propertyName = it.next();
            propertyValue = propertiesAsJson.getString(propertyName);
            result.put(propertyName, propertyValue);
        }

        logger.debug("Convertion result: " + result.toString());

        return result;
    }

    /**
     * Checks if all necessary ECC properties are present in the configuration
     * file.
     */
    private void ensureAllEccPropertiesDefined(JSONObject eccProperties) {
        logger.debug("Checking if ECC SAD configuration has all required properties: ");

        for (String expectedProperty : expectedEccProperties) {
            if (!eccProperties.containsKey(expectedProperty)) {
                throw new RuntimeException("ECC configuration must have '" + expectedProperty + "' property. Your configuration: " + eccProperties.toString(2));
            } else {
                logger.debug("\t- " + expectedProperty + " OK");
            }
        }
    }

    /**
     * Checks if all necessary local EDM properties are present in the
     * configuration file.
     */
    private void ensureAllEdmPropertiesDefined(JSONObject edmProperties) {
        logger.debug("Checking if EDM SAD configuration has all required properties: ");

        for (String expectedProperty : expectedEdmProperties) {
            if (!edmProperties.containsKey(expectedProperty)) {
                throw new RuntimeException("EDM configuration must have '" + expectedProperty + "' property. Your configuration: " + edmProperties.toString(2));
            } else {
                logger.debug("\t- " + expectedProperty + " OK");
            }
        }
    }

    @Override
    public void onEMConnectionResult(boolean connected, Experiment e) {
        if (connected) {
            if (e == null) {
                logger.error("Successfully connected to EM, but linked to experiment NULL. De-registering from EM");
                deRegisterFromEM();

            } else {
                logger.debug("Successfully connected to EM, linked to experiment [" + e.getExperimentID() + "] " + e.getName() + " (" + e.getDescription() + ")");

                // Clone passed Experiment instance to save later in EDM Agent during discovery phase
                theExperiment = new Experiment();
                theExperiment.setName(e.getName());
                theExperiment.setDescription(e.getDescription());
                theExperiment.setStartTime(e.getStartTime());

            }
        } else {
            logger.error("Connection to EM was refused. De-registering from EM");
            deRegisterFromEM();
        }
    }

    /**
     * Attempts to disconnect (de-register) from EM.
     *
     * @return true if the client is in the process of disconnecting.
     */
    public synchronized boolean deRegisterFromEM() {

        if (emiAdapter != null) { // run only when ECC not is disabled
            logger.debug("Attempting to deregister from EM, already deregistering: " + deRegisteringFromEM);

            // Don't repeatedly try to deregister
            if (!deRegisteringFromEM) {
                try {
                    deRegisteringFromEM = true;
                    emiAdapter.disconnectFromEM();
                    boolean tempBoolean;

                    if (amqpChannel != null) {
                        logger.debug("Closing amqpChannel with 5 seconds wait time");
                        amqpChannel.close();
//                        Thread.sleep(5000);
                        tempBoolean = amqpChannel.isOpen();
                        logger.debug("amqpChannel open: " + tempBoolean);
                    }

                    if (amqpFactory != null) {
                        logger.debug("Closing amqpFactory with 5 seconds wait time");
                        amqpFactory.closeDownConnection();
//                        Thread.sleep(5000);
                        tempBoolean = amqpFactory.isConnectionValid();
                        logger.debug("amqpFactory is connection valid: " + tempBoolean);
                    }

                    deRegisteringFromEM = false;
                    return true;
                } catch (Throwable ex) {
                    logger.error("Failed to de-register with the EM", ex);
                    deRegisteringFromEM = false;
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void onEMDeregistration(String reason) {
        logger.warn("Got DISCONNECTED because of: " + reason);
        deRegisterFromEM();
    }

    @Override
    public void onDescribeSupportedPhases(EnumSet<EMPhase> phasesOUT) {
        logger.debug("Describing supported phases: Live monitoring and Post report only");

        // Skipping Set-up and Tear-down phases:
//        phasesOUT.add(EMPhase.eEMSetUpMetricGenerators);
        phasesOUT.add(EMPhase.eEMLiveMonitoring);
//        phasesOUT.add(EMPhase.eEMPostMonitoringReport);
//        phasesOUT.add(EMPhase.eEMTearDown);
    }

    @Override
    public void onDescribePushPullBehaviours(Boolean[] pushPullOUT) {
        logger.debug("Describing push pull behaviours: only pushing supported");

        pushPullOUT[0] = true; // Yes to pushing
        pushPullOUT[1] = false;  // No to pulling
    }

    @Override
    public void onPopulateMetricGeneratorInfo() {
        logger.debug("Populating metric generator info");

        // Time to start defining what metrics we can produce for this experiment
        if (theExperiment != null) {
            // Define all metric generators for this experiment
            createMetricModel(theExperiment);

            // Even if the EDMAgent isn't available, we can still send data, so
            // notify the adapter of our metrics
            emiAdapter.sendMetricGenerators(theExperiment.getMetricGenerators());

        } else {
            // Things are bad if we can't describe our metric generators - so disconnect
            logger.error("Trying to populate metric generator info - but current experiment is NULL. Disconnecting");
            deRegisterFromEM();
        }

    }

    /**
     * Defines SAD metric model.
     */
    private HashSet<MetricGenerator> createMetricModel(Experiment experiment) {

        measurementSetMap.clear(); // This map will be useful later for reporting measurement summaries
        metricGenerators.clear();

        theMetricGenerator = new MetricGenerator();
        theMetricGenerator.setName("SAD Metric Generator");
        theMetricGenerator.setDescription("Metric generator for Social Analytics Dashboard");

        experiment.addMetricGenerator(theMetricGenerator);

        MetricGroup theMetricGroup = new MetricGroup();
        theMetricGroup.setName("SAD Metric Group");
        theMetricGroup.setDescription("Metric group for all Social Analytics Dashboard metrics");
        theMetricGroup.setMetricGeneratorUUID(theMetricGenerator.getUUID());
        theMetricGenerator.addMetricGroup(theMetricGroup);

        Entity theEntity = new Entity();
        theEntity.setName("SAD Service");
        theEntity.setDescription("Entity for Social Analytics Dashboard");
        theMetricGenerator.addEntity(theEntity);

        Attribute totalPluginExecutions = new Attribute();
        totalPluginExecutions.setName("Number of jobs started");
        totalPluginExecutions.setDescription("Number of times SAD plugins have been submitted for execution");
        totalPluginExecutions.setEntityUUID(theEntity.getUUID());
        theEntity.addAttribute(totalPluginExecutions);

        numPluginsRunMeasurementSetUuid = setupMeasurementForAttribute(totalPluginExecutions,
                theMetricGroup,
                MetricType.RATIO,
                new Unit("Times"));

        Attribute failedPluginExecutions = new Attribute();
        failedPluginExecutions.setName("Failed plugin executions");
        failedPluginExecutions.setDescription("Number of times SAD plugins have failed to execute");
        failedPluginExecutions.setEntityUUID(theEntity.getUUID());
        theEntity.addAttribute(failedPluginExecutions);

        numPluginsFailedMeasurementSetUuid = setupMeasurementForAttribute(failedPluginExecutions,
                theMetricGroup,
                MetricType.RATIO,
                new Unit("Times"));

        Attribute successfulPluginExecutions = new Attribute();
        successfulPluginExecutions.setName("Successful plugin executions");
        successfulPluginExecutions.setDescription("Number of times SAD plugins have executed successfully");
        successfulPluginExecutions.setEntityUUID(theEntity.getUUID());
        theEntity.addAttribute(successfulPluginExecutions);

        numPluginsSuccessMeasurementSetUuid = setupMeasurementForAttribute(successfulPluginExecutions,
                theMetricGroup,
                MetricType.RATIO,
                new Unit("Times"));

        Attribute namesOfPluginsExecuted = new Attribute();
        namesOfPluginsExecuted.setName("Names of plugins executed");
        namesOfPluginsExecuted.setDescription("Names of plugins submitted for execution");
        namesOfPluginsExecuted.setEntityUUID(theEntity.getUUID());
        theEntity.addAttribute(namesOfPluginsExecuted);

        pluginNameMeasurementSetUuid = setupMeasurementForAttribute(namesOfPluginsExecuted,
                theMetricGroup,
                MetricType.NOMINAL,
                new Unit(""));

        Attribute namesOfMethodsCalled = new Attribute();
        namesOfMethodsCalled.setName("Names of methods called");
        namesOfMethodsCalled.setDescription("Names of service methods called");
        namesOfMethodsCalled.setEntityUUID(theEntity.getUUID());
        theEntity.addAttribute(namesOfMethodsCalled);

        methodsCalledMeasurementSetUuid = setupMeasurementForAttribute(namesOfMethodsCalled,
                theMetricGroup,
                MetricType.NOMINAL,
                new Unit(""));

        Attribute timeSpendOnServiceMethodCall = new Attribute();
        timeSpendOnServiceMethodCall.setName("Service method call duration");
        timeSpendOnServiceMethodCall.setDescription("Time spend on service method call");
        timeSpendOnServiceMethodCall.setEntityUUID(theEntity.getUUID());
        theEntity.addAttribute(timeSpendOnServiceMethodCall);

        timeSpentOnServiceCallMeasurementSetUuid = setupMeasurementForAttribute(timeSpendOnServiceMethodCall,
                theMetricGroup,
                MetricType.RATIO,
                new Unit("mSec"));

        Attribute timeSpendOnDatabaseQueryCall = new Attribute();
        timeSpendOnDatabaseQueryCall.setName("Database query duration");
        timeSpendOnDatabaseQueryCall.setDescription("Time spend on querying the database");
        timeSpendOnDatabaseQueryCall.setEntityUUID(theEntity.getUUID());
        theEntity.addAttribute(timeSpendOnDatabaseQueryCall);

        timeSpentOnDatabaseQueryMeasurementSetUuid = setupMeasurementForAttribute(timeSpendOnDatabaseQueryCall,
                theMetricGroup,
                MetricType.RATIO,
                new Unit("mSec"));

        Attribute pluginExecutionDuration = new Attribute();
        pluginExecutionDuration.setName("Plugin execution duration");
        pluginExecutionDuration.setDescription("How long it took for the plugin to run");
        pluginExecutionDuration.setEntityUUID(theEntity.getUUID());
        theEntity.addAttribute(pluginExecutionDuration);

        pluginExecutionDurationMeasurementSetUuid = setupMeasurementForAttribute(pluginExecutionDuration,
                theMetricGroup,
                MetricType.RATIO,
                new Unit("mSec"));

        metricGenerators.put(theMetricGenerator.getUUID(), theMetricGenerator);

        HashSet<MetricGenerator> mgSet = new HashSet<>();
        mgSet.addAll(metricGenerators.values());

        logger.debug("Reporting the following metric generator set to ECC: ");
        int counter = 0;
        for (MetricGenerator tempMg : mgSet) {
            printMetricGenerator(tempMg, counter);
            counter++;
        }

        metricsModelSetup = true;

        return mgSet;
    }

    /**
     *
     * @return UUID for the "number of plugins ran" measurement set.
     */
    public UUID getNumPluginsRunMeasurementSetUuid() {
        return numPluginsRunMeasurementSetUuid;
    }

    /**
     *
     * @return UUID for the "number of plugins failed" measurement set.
     */
    public UUID getNumPluginsFailedMeasurementSetUuid() {
        return numPluginsFailedMeasurementSetUuid;
    }

    /**
     *
     * @return UUID for the "number of plugins ran successfully" measurement
     * set.
     */
    public UUID getNumPluginsSuccessMeasurementSetUuid() {
        return numPluginsSuccessMeasurementSetUuid;
    }

    /**
     *
     * @return UUID for the "name of plugins ran" measurement set.
     */
    public UUID getPluginNameMeasurementSetUuid() {
        return pluginNameMeasurementSetUuid;
    }

    /**
     *
     * @return UUID for the "execution duration of a plugin" measurement set.
     */
    public UUID getPluginExecutionDurationMeasurementSetUuid() {
        return pluginExecutionDurationMeasurementSetUuid;
    }

    /**
     * @return UUIDs of measurements sets for the plugin runner.
     */
    public JSONObject getPluginRunnerMeasurementSetsUuids() {
        JSONObject result = new JSONObject();
        result.put("numPluginsSuccessMeasurementSetUuid", numPluginsSuccessMeasurementSetUuid.toString());
        result.put("numPluginsFailedMeasurementSetUuid", numPluginsFailedMeasurementSetUuid.toString());
        result.put("pluginExecutionDurationMeasurementSetUuid", pluginExecutionDurationMeasurementSetUuid.toString());

        return result;
    }

    /**
     * Pushes the new number of plugin runs measurement to ECC.
     *
     * @param newNumPluginsRun new number of plugin runs to report.
     */
    public void pushNumPluginsRunMeasurementToEcc(int newNumPluginsRun) {
        if (emClientOK) {
            if (pushingEnabled) {
                logger.debug("Pushing total number of plugins run measurement update (" + newNumPluginsRun + ") to the ECC");
                pushSingleMeasurementForMeasurementSetUuid(numPluginsRunMeasurementSetUuid, Integer.toString(newNumPluginsRun));
            } else {
                logger.debug("NOT pushing total number of plugins run measurement update to the ECC because pushing is DISABLED (wrong phase or was not told to start pushing by the ECC)");
            }
        } else {
            logger.debug("NOT pushing total number of plugins run measurement update to the ECC because EM client is NOT enabled");
        }
    }

    /**
     * Pushes the new number of successful plugin runs measurement to ECC.
     *
     * @param newNumPluginsSucceeded new number of successful plugin runs to
     * report.
     */
    public void pushNumPluginsSucceededMeasurementToEcc(int newNumPluginsSucceeded) {
        if (emClientOK) {
            if (pushingEnabled) {
                logger.debug("Pushing number of successful plugins measurement update (" + newNumPluginsSucceeded + ") to the ECC");
                pushSingleMeasurementForMeasurementSetUuid(numPluginsSuccessMeasurementSetUuid, Integer.toString(newNumPluginsSucceeded));

            } else {
                logger.debug("NOT pushing number of successful plugins measurement update to the ECC because pushing is DISABLED (wrong phase or was not told to start pushing by the ECC)");
            }
        } else {
            logger.debug("NOT pushing number of successful plugins measurement update to the ECC because EM client is NOT enabled");
        }
    }

    /**
     * Pushes the new number of failed plugins measurement to ECC.
     *
     * @param newNumPluginsFailed new number of failed plugins to report.
     */
    public void pushNumPluginsFailedMeasurementToEcc(int newNumPluginsFailed) {
        if (emClientOK) {
            if (pushingEnabled) {
                logger.debug("Pushing number of failed plugins measurement update (" + newNumPluginsFailed + ") to the ECC");
                pushSingleMeasurementForMeasurementSetUuid(numPluginsFailedMeasurementSetUuid, Integer.toString(newNumPluginsFailed));

            } else {
                logger.debug("NOT pushing number of failed plugins measurement update to the ECC because pushing is DISABLED (wrong phase or was not told to start pushing by the ECC)");
            }
        } else {
            logger.debug("NOT pushing number of failed plugins measurement update to the ECC because EM client is NOT enabled");
        }
    }

    /**
     * Pushes the new number of failed plugins measurement to ECC.
     *
     * @param newPluginName new number of failed plugins to report.
     */
    public void pushPluginNameMeasurementToEcc(String newPluginName) {
        if (emClientOK) {
            if (pushingEnabled) {
                logger.debug("Pushing new plugin name measurement update (" + newPluginName + ") to the ECC");
                pushSingleMeasurementForMeasurementSetUuid(pluginNameMeasurementSetUuid, newPluginName);

            } else {
                logger.debug("NOT pushing new plugin name measurement update to the ECC because pushing is DISABLED (wrong phase or was not told to start pushing by the ECC)");
            }
        } else {
            logger.debug("NOT pushing new plugin name measurement update to the ECC because EM client is NOT enabled");
        }
    }

    /**
     * Pushes names of called methods.
     *
     * @param methodName name of the method to report.
     */
    public void pushMethodCalledToEcc(String methodName) {
        if (emClientOK) {
            if (pushingEnabled) {
                logger.debug("Pushing service method call update (" + methodName + ") to the ECC");
                pushSingleMeasurementForMeasurementSetUuid(methodsCalledMeasurementSetUuid, methodName);

            } else {
                logger.debug("NOT pushing service method call measurement update to the ECC because pushing is DISABLED (wrong phase or was not told to start pushing by the ECC)");
            }
        } else {
            logger.debug("NOT pushing service method call measurement update to the ECC because EM client is NOT enabled");
        }
    }

    /**
     * Pushes service call method duration.
     *
     * @param methodDuration time in msec the call took.
     */
    public void pushMethodCallDuration(String methodDuration) {
        if (emClientOK) {
            if (pushingEnabled) {
                logger.debug("Pushing service method call duration update (" + methodDuration + ") to the ECC");
                pushSingleMeasurementForMeasurementSetUuid(timeSpentOnServiceCallMeasurementSetUuid, methodDuration);

            } else {
                logger.debug("NOT pushing service method call duration measurement update to the ECC because pushing is DISABLED (wrong phase or was not told to start pushing by the ECC)");
            }
        } else {
            logger.debug("NOT pushing service method call duration measurement update to the ECC because EM client is NOT enabled");
        }
    }

    /**
     * Pushes new database query duration measurement.
     *
     * @param queryDuration time in msec the query took.
     */
    public void pushDatabaseQueryDuration(String queryDuration) {
        if (emClientOK) {
            if (pushingEnabled) {
                logger.debug("Pushing database query duration update (" + queryDuration + ") to the ECC");
                pushSingleMeasurementForMeasurementSetUuid(timeSpentOnDatabaseQueryMeasurementSetUuid, queryDuration);

            } else {
                logger.debug("NOT pushing database query duration measurement update to the ECC because pushing is DISABLED (wrong phase or was not told to start pushing by the ECC)");
            }
        } else {
            logger.debug("NOT pushing database query duration measurement update to the ECC because EM client is NOT enabled");
        }
    }

    /**
     * Pushes single measurement to the ECC for a measurement set.
     *
     * @param uuid UUID of the measurement set.
     * @param measurementValueAsString measurement value as string.
     * @return true if the push was successful.
     */
    public boolean pushSingleMeasurementForMeasurementSetUuid(UUID uuid, String measurementValueAsString) {
        Report report = getReportForMeasurementSetUuid(uuid);

        if (report == null) {
            logger.error("Failed to push single measurement (" + measurementValueAsString + ") for measurement set [" + uuid.toString() + "] to the ECC because new report is NULL");
            return false;
        } else {
            pushReportWithSingleMeasurement(report, measurementValueAsString);
            logger.debug("PUSHED single measurement report: [" + report.getUUID().toString() + "] "
                    + "for measurement set [" + uuid.toString() + "] with measurement value '" + measurementValueAsString + "'");
            return true;
        }
    }

    /**
     * Creates new report for a measurement set.
     *
     * @param uuid UUID of the measurement set.
     * @return ECC report object.
     */
    private Report getReportForMeasurementSetUuid(UUID uuid) {
        MeasurementSet ms = MetricHelper.getMeasurementSet(theMetricGenerator, uuid); // null check has to be performed here with minimum debug output if true
        if (ms == null) {
            logger.error("Measurement set [" + uuid.toString() + "] does not exist, this will result in NULL report");
            return null;
        } else {
            Report report = MetricHelper.createEmptyMeasurementReport(ms);

            report.setNumberOfMeasurements(1);
            Date now = new Date();
            report.setReportDate(now);
            report.setFromDate(now);
            report.setToDate(now);

            return report;
        }
    }

    /**
     * Pushes new report to the ECC with single measurement.
     *
     * @param reportToPush which report to push.
     * @param measurementValue which measurement to include with the report
     * before pushing.
     */
    private void pushReportWithSingleMeasurement(Report reportToPush, String measurementValue) {
        Measurement sample = new Measurement(measurementValue);
        reportToPush.getMeasurementSet().addMeasurement(sample);

        // Push to ECC
        if (emClientOK) {
            if (pushingEnabled) {
                emiAdapter.pushMetric(reportToPush);
                pendingPushReports.put(reportToPush.getUUID(), reportToPush);
            } else {
                logger.debug("NOT pushing report to ECC as pushing is disabled");
            }
        } else {
            logger.warn("NOT pushing report to ECC as EM client DISABLED");
        }

    }

    @Override
    public void onDiscoveryTimeOut() {
        logger.warn("Received DISCOVERY TIMEOUT message");

    }

    @Override
    public void onSetupMetricGenerator(UUID uuid, Boolean[] resultOUT) {
        logger.debug("Reporting success setting up metric generator [" + uuid.toString() + "]");

        resultOUT[0] = true;
    }

    @Override
    public void onSetupTimeOut(UUID uuid) {
        logger.warn("Received SETUP TIMEOUT message");
    }

    @Override
    public void onLiveMonitoringStarted() {
        logger.debug("ECC has started live monitoring process");
        if (localEdmOK && measurementSchedulerOK) {
            startMeasuring();
        }
    }

    @Override
    public void onStartPushingMetricData() {
        logger.debug("ECC says: START pushing data, enabling pushing, pushing current data");
        pushingEnabled = true;
    }

    @Override
    public void onPushReportReceived(UUID reportID) {
        if (pendingPushReports.containsKey(reportID)) {
            logger.debug("ECC says push report [" + reportID.toString() + "] RECEIVED, removing from pending");
            pendingPushReports.remove(reportID);
        } else {
            logger.error("Unknown push report [" + reportID.toString() + "]");
        }
    }

    @Override
    public void onStopPushingMetricData() {
        logger.debug("ECC says: STOP pushing data");
        pushingEnabled = false;
    }

    @Override
    public void onPullReportReceived(UUID reportID) {
        logger.debug("ECC says pull report [" + reportID.toString() + "] RECEIVED");

        if (pendingPullReports.containsKey(reportID)) {
            pendingPullReports.remove(reportID);
        } else {
//            logger.error("Unknown pull report [" + reportID.toString() + "]");
        }
    }

    @Override
    public void onPullMetric(UUID measurementSetID, Report reportOUT) {

        // Otherwise, immediately generate the metric 'on-the-fly'
        ITakeMeasurement sampler = instantMeasurers.get(measurementSetID);
        MeasurementSet mSet = measurementSetMap.get(measurementSetID);

        if (sampler != null && mSet != null) {
            // Make an empty measurement set for this data first
            MeasurementSet emptySet = new MeasurementSet(mSet, false);
            reportOUT.setMeasurementSet(emptySet);

            sampler.takeMeasure(reportOUT);
        } else {
            logger.error("Could not find measurement sampler for measurement set with ID [" + measurementSetID.toString() + "]");
        }
    }

    @Override
    public void onPullMetricTimeOut(UUID measurementSetID) {
        logger.warn("Received PULL METRIC TIMEOUT message for Measurement Set [" + measurementSetID.toString() + "]");
    }

    @Override
    public void onPullingStopped() {
        logger.debug("ECC has stopped pulling");
        stopMeasuring();
    }

    @Override
    public void onPopulateSummaryReport(EMPostReportSummary summaryOUT) {

        // summary statistics
        logger.debug("Populating summary report WITHOUT local EDM agent using instant measurers");
        Iterator<UUID> msIDIt = instantMeasurers.keySet().iterator();
        while (msIDIt.hasNext()) {
            // Get measurement and measurement set for sampler
            UUID msID = msIDIt.next();
            ITakeMeasurement sampler = instantMeasurers.get(msID);
            MeasurementSet mset = measurementSetMap.get(msID);

            if (sampler != null && mset != null) {
                // Create a report for this measurement set + summary stats
                Report report = new Report();
                report.setMeasurementSet(mset);
                report.setFromDate(sampler.getFirstMeasurementDate());
                report.setToDate(sampler.getLastMeasurementDate());
                report.setNumberOfMeasurements(sampler.getMeasurementCount());

                summaryOUT.addReport(report);
            }
        }
    }

    @Override
    public void onPopulateDataBatch(EMDataBatch batchOUT) {

        logger.debug("Populating data batch");

        // If we have been storing metrics using the EDM & Scheduler, get some
        // previously unsent data
        UUID msID = batchOUT.getExpectedMeasurementSetID();
        if (msID == null) {
            logger.warn("Expected measurement set ID for this batch is NULL");
        } else {
            logger.debug("Expected measurement set ID for this batch: " + msID.toString());
        }

    }

    @Override
    public void onReportBatchTimeOut(UUID uuid) {
        logger.warn("Received REPORT BATCH TIMEOUT message");
    }

    @Override
    public void onGetTearDownResult(Boolean[] resultOUT) {
        logger.debug("Reporting successfull tear down result");
        resultOUT[0] = true;
    }

    @Override
    public void onTearDownTimeOut() {
        logger.warn("Received TEAR DOWN TIMEOUT message");
    }


    /*
     *
     * USEFUL METHODS
     *
     */
    /**
     * Run through all our measurement tasks and start them up.
     */
    private void startMeasuring() {
        Iterator<MeasurementTask> taskIt = scheduledMeasurementTasks.iterator();
        while (taskIt.hasNext()) {
            taskIt.next().startMeasuring();
        }
    }

    /**
     * Run through all our measurement tasks and stop them.
     */
    private void stopMeasuring() {
        Iterator<MeasurementTask> taskIt = scheduledMeasurementTasks.iterator();
        while (taskIt.hasNext()) {
            taskIt.next().stopMeasuring();
        }
    }

    /**
     * Creates a measurement set for an attribute.
     *
     * @param attr the attribute.
     * @param parentGroup the metric parent group.
     * @param type the metric type.
     * @param unit the metric unit.
     * @return UUID of the created measurement set.
     */
    private UUID setupMeasurementForAttribute(Attribute attr,
            MetricGroup parentGroup,
            MetricType type,
            Unit unit) {

        // Define the measurement set
        MeasurementSet ms = new MeasurementSet();
        parentGroup.addMeasurementSets(ms);
        ms.setMetricGroupUUID(parentGroup.getUUID());
        ms.setAttributeUUID(attr.getUUID());
        ms.setMetric(new Metric(UUID.randomUUID(), type, unit));

        // Map this measurement set for later
        measurementSetMap.put(ms.getID(), ms);

        return ms.getID();

    }

    /**
     * Simplifies the process of adding metrics to attributes and metric groups.
     */
    @Deprecated
    private void addMetricToAttributeAndMetricGroup(MetricGroup metricGroup, Attribute attribute, MetricType metricType, Unit metricUnit) {
        MeasurementSet theMeasuringSet = new MeasurementSet();
        theMeasuringSet.setMetricGroupUUID(metricGroup.getUUID());
        theMeasuringSet.setAttributeUUID(attribute.getUUID());
        metricGroup.addMeasurementSets(theMeasuringSet);

//        allMeasurementSets.add(theMeasuringSet);
//        measurementSetsAndAttributes.put(theMeasuringSet.getUUID(), attribute);
        Metric theMetric = new Metric();
        theMetric.setMetricType(metricType);
        theMetric.setUnit(metricUnit);
        theMeasuringSet.setMetric(theMetric);
    }

    /**
     * Prints a metric generator into logs as a tree.
     */
    private void printMetricGenerator(MetricGenerator mg, int numberInSet) {
        pr("[" + numberInSet + "] " + mg.getName() + " (" + mg.getDescription() + ") [" + mg.getUUID() + "]", 0);
        Set<MeasurementSet> allMeasurementSets = new HashSet<>();

        for (MetricGroup mgroup : mg.getMetricGroups()) {
            allMeasurementSets.addAll(mgroup.getMeasurementSets());
        }

        for (Entity e : mg.getEntities()) {
            pr("Entity: " + e.getName() + " (" + e.getDescription() + ") [" + e.getUUID() + "]", 1);
            for (Attribute a : e.getAttributes()) {
                pr("Attribute: " + a.getName() + " (" + a.getDescription() + ") [" + a.getUUID() + "]", 2);

                for (MeasurementSet ms : allMeasurementSets) {
                    if (ms.getAttributeID().toString().equals(a.getUUID().toString())) {
                        pr("Metric type: " + ms.getMetric().getMetricType() + ", unit: " + ms.getMetric().getUnit() + ", measurement set [" + ms.getID() + "]", 3);
                    }
                }
            }
        }
    }

    /**
     * Prints an object into logs with indent.
     */
    private void pr(Object o, int indent) {
        String indentString = "";
        for (int i = 0; i < indent; i++) {
            indentString += "\t";
        }

        if (indent > 0) {
            logger.debug(indentString + "- " + o);
        } else {
            logger.debug("" + o);
        }
    }

}
