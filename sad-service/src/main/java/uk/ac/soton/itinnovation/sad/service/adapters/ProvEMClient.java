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
package uk.ac.soton.itinnovation.sad.service.adapters;

import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.amqpAPI.impl.amqp.AMQPBasicChannel;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.amqpAPI.impl.amqp.AMQPConnectionFactory;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.experiment.Experiment;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.*;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.monitor.EMDataBatch;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.monitor.EMPhase;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.monitor.EMPostReportSummary;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.provenance.*;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.samples.shared.*;
import uk.ac.soton.itinnovation.sad.service.helpers.ProvVideo;
import uk.ac.soton.itinnovation.sad.service.services.SchedulingService;

import javax.annotation.PostConstruct;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SAD EMClient, based on
 * uk.ac.soton.itinnovation.experimedia.arch.ecc.samples.headlessECCClient.ECCHeadlessClient
 */
@Service("ProvEMClient")
public class ProvEMClient implements EMIAdapterListener {

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
    private UUID actionsTakenUuid;
    private UUID videoQualityUuid;

    private ArrayList<UUID> pendingPushReports = new ArrayList<>();
    private HashMap<UUID, Report> pendingPullReports = new HashMap<>();

    private boolean emClientOK = false;
    private boolean measurementSchedulerOK = false;
    private boolean pushingEnabled = false;
    private boolean metricsModelSetup = false;

    // PROV
    private EDMProvFactory factory;

    // Client state
    private boolean deRegisteringFromEM = false;

    @Autowired
    @Qualifier("schedulingService")
    SchedulingService schedulingService;

    public ProvEMClient() {

    }

    @PostConstruct
    public void init() {
    }

    public boolean start() {

        // Connect to EM
        logger.debug("Creating new PROV EM Client ...");

        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        metricGenerators = new HashMap<>();
        measurementSetMap = new HashMap<>();
        scheduledMeasurementTasks = new HashSet<>();
        instantMeasurers = new HashMap<>();
        pendingPushReports = new ArrayList<>();
        pendingPullReports = new HashMap<>();

        emClientOK = false;
        measurementSchedulerOK = false;
        pushingEnabled = false;
        metricsModelSetup = false;

//        JSONObject eccProperties = propertiesService.getPropertyAsJSONObject("ecc");
        JSONObject eccProperties = new JSONObject();

        ensureAllEccPropertiesDefined(eccProperties);

        if (!eccProperties.getString("enabled").equals("y")) {
            logger.debug("ECC integration disabled in configuration. Stopped creating SAD EM client");

        } else {
            logger.debug("Using ECC SAD configuration: " + eccProperties.toString(2));

            String rabbitServerIP = eccProperties.getString("Rabbit_IP");
            int rabbitServerPort = Integer.parseInt(eccProperties.getString("Rabbit_Port"));
            UUID expMonitorID = UUID.fromString(eccProperties.getString("Monitor_ID"));
            String clientName = "PROV ECC Client";
            UUID clientID = UUID.randomUUID();
            logger.debug("Created new client UUID: " + clientID.toString());

            AMQPConnectionFactory amqpFactory = new AMQPConnectionFactory();
            amqpFactory.setAMQPHostIPAddress(rabbitServerIP);
            amqpFactory.setAMQPHostPort(rabbitServerPort);

            try {
                amqpFactory.connectToAMQPHost();
                amqpChannel = amqpFactory.createNewChannel();
            } catch (Exception ex) {
                throw new RuntimeException("Failed to connect to Rabbit server [" + rabbitServerIP + ":" + rabbitServerPort + "]", ex);
            }

            emiAdapter = new EMInterfaceAdapter(this);

            try {
                emiAdapter.registerWithEM(clientName, amqpChannel, expMonitorID, clientID);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to register client [" + clientID + "] '" + clientName + "' with EM using experiment monitor ID [" + expMonitorID + "]", ex);
            }

            factory = EDMProvFactory.getInstance();

            logger.debug("Successfully created new SAD EM Client");
            emClientOK = true;
        }

        logger.debug("EM Client init results:");
        logger.debug("\t- emClientOK: " + emClientOK);
        logger.debug("\t- measurementSchedulerOK: " + measurementSchedulerOK);

        return true;
    }

    /**
     * Returns true if EM Client was successfully created.
     */
    public boolean isEmClientOK() {
        return emClientOK;
    }

    /**
     * Returns true if ECC measurement scheduler was successfully created.
     */
    public boolean isMeasurementSchedulerOK() {
        return measurementSchedulerOK;
    }

    /**
     * Returns true if ECC enabled pushing.
     */
    public boolean isPushingEnabled() {
        return pushingEnabled;
    }

    /**
     * Returns true if ECC metric model was created (UUIDs for measurement
     * reports have been set).
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
     * Attempts to disconnect (de-register) from EM
     */
    public synchronized boolean deRegisterFromEM() {

        if (emiAdapter != null) { // run only when ECC not is disabled
            logger.debug("Attempting to deregister from EM, already deregistering: " + deRegisteringFromEM);

            // Don't repeatedly try to deregister
            if (!deRegisteringFromEM) {
                try {
                    deRegisteringFromEM = true;
                    emiAdapter.disconnectFromEM();
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
     * Defines SAD metric model
     */
    private HashSet<MetricGenerator> createMetricModel(Experiment experiment) {

        measurementSetMap.clear(); // This map will be useful later for reporting measurement summaries

//        MetricHelper.
        theMetricGenerator = new MetricGenerator();
        theMetricGenerator.setName("PROV Metric Generator");
        theMetricGenerator.setDescription("Metric generator for AVCC Sim");

        experiment.addMetricGenerator(theMetricGenerator);

        MetricGroup theMetricGroup = new MetricGroup();
        theMetricGroup.setName("PROV Metric Group");
        theMetricGroup.setDescription("Metric group for all AVCC Sim metircs");
        theMetricGroup.setMetricGeneratorUUID(theMetricGenerator.getUUID());
        theMetricGenerator.addMetricGroup(theMetricGroup);

        Entity theEntity = new Entity();
        theEntity.setName("User actions");
        theEntity.setDescription("Entity for PROV user actions metrics");
        theMetricGenerator.addEntity(theEntity);

        Attribute actionsTaken = new Attribute();
        actionsTaken.setName("Actions taken");
        actionsTaken.setDescription("Names of actions by user at sample video page");
        actionsTaken.setEntityUUID(theEntity.getUUID());
        theEntity.addAttribute(actionsTaken);

        actionsTakenUuid = setupMeasurementForAttribute(actionsTaken,
                theMetricGroup,
                MetricType.NOMINAL,
                new Unit(""));

        Attribute videoQuality = new Attribute();
        videoQuality.setName("Video Quality");
        videoQuality.setDescription("Video resolution of the video player selected by user");
        videoQuality.setEntityUUID(theEntity.getUUID());
        theEntity.addAttribute(videoQuality);

        videoQualityUuid = setupMeasurementForAttribute(videoQuality,
                theMetricGroup,
                MetricType.NOMINAL,
                new Unit(""));

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

    public UUID getNumPluginsRunMeasurementSetUuid() {
        return numPluginsRunMeasurementSetUuid;
    }

    public UUID getNumPluginsFailedMeasurementSetUuid() {
        return numPluginsFailedMeasurementSetUuid;
    }

    public UUID getNumPluginsSuccessMeasurementSetUuid() {
        return numPluginsSuccessMeasurementSetUuid;
    }

    public UUID getActionsTakenMeasurementSetUuid() {
        return actionsTakenUuid;
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
     * Pushes the action to ECC.
     *
     * @param actionTaken taken to report.
     */
    public void pushActionTakenMeasurementToEcc(String actionTaken) {
        if (emClientOK) {
            if (pushingEnabled) {
                logger.debug("Pushing new action taken measurement update (" + actionTaken + ") to the ECC");
                pushSingleMeasurementForMeasurementSetUuid(actionsTakenUuid, actionTaken);

            } else {
                logger.debug("NOT pushing new plugin name measurement update to the ECC because pushing is DISABLED (wrong phase or was not told to start pushing by the ECC)");
            }

        } else {
            logger.debug("NOT pushing new plugin name measurement update to the ECC because EM client is NOT enabled");
        }
    }

    /**
     * Pushes the video quality to ECC.
     *
     * @param videoQuality taken to report.
     */
    public void pushVideoQualityMeasurementToEcc(String videoQuality) {
        if (emClientOK) {
            if (pushingEnabled) {
                logger.debug("Pushing new action taken measurement update (" + videoQuality + ") to the ECC");
                pushSingleMeasurementForMeasurementSetUuid(videoQualityUuid, videoQuality);

            } else {
                logger.debug("NOT pushing new plugin name measurement update to the ECC because pushing is DISABLED (wrong phase or was not told to start pushing by the ECC)");
            }

        } else {
            logger.debug("NOT pushing new plugin name measurement update to the ECC because EM client is NOT enabled");
        }
    }

    /**
     * Pushes single measurement to the ECC for a measurement set.
     *
     * @param uuid UUID of the measurement set.
     * @param measurementValueAsString measurement value as string.
     */
    public boolean pushSingleMeasurementForMeasurementSetUuid(UUID uuid, String measurementValueAsString) {
        Report report = getReportForMeasurementSetUuid(uuid);
        report.setNumberOfMeasurements(1);
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
        MeasurementSet ms = MetricHelper.getMeasurementSet(theMetricGenerator, uuid);
        Report report = MetricHelper.createEmptyMeasurementReport(ms);

        report.setNumberOfMeasurements(1);
        Date now = new Date();
        report.setReportDate(now);
        report.setFromDate(now);
        report.setToDate(now);

        return report;
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
            emiAdapter.pushMetric(reportToPush);
            pendingPushReports.add(reportToPush.getUUID());
        } else {
            logger.warn("Not pushing report to ECC as EM client DISABLED");
        }

    }

    public void reportLoadPlayer(JSONObject event) {
        try {
            logger.debug("Creating prov report:\n" + event.toString(2));

            String userName = event.getString("user_name");
            String userId = event.getString("user_id");
            String timestamp = event.getString("timestamp");
            String url = event.getString("video_url");

            factory = EDMProvFactory.getInstance();

            // User
            EDMAgent userAsAgent = factory.createAgent("experimedia:" + userName + "_" + userId, userName);
            userAsAgent.addOwlClass("foaf:Person");

            // Load page activity
            EDMActivity loadPageAsActivity = userAsAgent.doDiscreteActivity("experimedia:loadPage_" + userId + "_" + timestamp, timestamp);

            // Video
            EDMEntity videoAsEntity = factory.createEntity("experimedia:video_" + url, url);

            // Video player
            EDMEntity videoPlayerAsEntity = loadPageAsActivity.generateEntity("experimedia:videoPlayer_" + userId, userId);
//            videoPlayerAsEntity.addOwlClass("prov:Location");
//            videoAsEntity.addProperty("prov:atLocation", videoPlayerAsEntity.getIri());

            EDMProvReport report = factory.createProvReport();
            pendingPushReports.add(report.getID());
            emiAdapter.pushPROVStatement(report);
        } catch (Throwable ex) {
            logger.debug("Failed to report action by person as prov data", ex);
        }
    }

    public void reportActionByPerson(ProvVideo input_video) {
        try {

            logger.debug("Sending prov video data: " + input_video.toString());
            factory = EDMProvFactory.getInstance();

            EDMAgent user = factory.createAgent("experimedia:" + input_video.getUserName() + "_" + input_video.getUserId(), input_video.getUserName());
            user.addOwlClass("foaf:Person");

            EDMActivity videoActivity = user.doDiscreteActivity("experimedia:" + input_video.getAction() + "_" + input_video.getUserId() + "_" + input_video.getTimestamp(), Long.toString(input_video.getTimestamp() / 1000));

            EDMEntity videoPlayer = factory.createEntity("experimedia:videoPlayer_" + input_video.getUserId(), input_video.getUserId());
//            videoActivity.addProperty("prov:used", videoPlayer.getIri());

            EDMProvReport report = factory.createProvReport();
            pendingPushReports.add(report.getID());
            emiAdapter.pushPROVStatement(report);
        } catch (Throwable ex) {
            logger.debug("Failed to report action by person as prov data", ex);
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
        // If we have an EDM Agent and scheduling, then start taking periodic measurements
        if (measurementSchedulerOK) {
            startMeasuring();
        }
    }

    @Override
    public void onStartPushingMetricData() {
        logger.debug("ECC says: START pushing data, enabling pushing, pushing current data");
        pushingEnabled = true;
//        pushNumPluginsRunMeasurementToEcc(leaderboardService.getAllLeaderboardContestants().size());
    }

    @Override
    public void onPushReportReceived(UUID reportID) {

        if (pendingPushReports.contains(reportID)) {
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
     * Correct way to setup measurements from ~ ECC version 1.1
     */
//    private void setupMeasurementForAttribute(Attribute attr,
//            MetricGroup parentGroup,
//            MetricType type,
//            Unit unit,
//            ITakeMeasurement listener,
//            long intervalMS) {
//
//        // Define the measurement set
//        MeasurementSet ms = new MeasurementSet();
//        parentGroup.addMeasurementSets(ms);
//        ms.setMetricGroupUUID(parentGroup.getUUID());
//        ms.setAttributeUUID(attr.getUUID());
//        ms.setMetric(new Metric(UUID.randomUUID(), type, unit));
//
//        // Map this measurement set for later
//        measurementSetMap.put( ms.getID(), ms );
//
//        // If available, create an automatic measurement task to periodically take measurements
//        if (localEdmOK && measurementSchedulerOK) {
//            try {
//                // Must keep hold of task reference to ensure continued sampling
//                MeasurementTask task = measurementScheduler.createMeasurementTask(
//                        ms, // MeasurementSet
//                        listener, // Listener that will take measurement
//                        -1, // Monitor indefinitely...
//                        intervalMS); // ... each 'X' milliseconds
//
//                scheduledMeasurementTasks.add(task);
//            } catch (Throwable ex) {
//                logger.error("Could not define measurement task for attribute " + attr.getName(), ex);
//            }
//        } else {
//            // If we can't schedule & store measurements, just have the samplers handy
//            instantMeasurers.put(ms.getID(), listener);
//        }
//    }
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

    /**
     * Private shut-down hook.
     */
    private class ShutdownHook extends Thread {

        @Override
        public void run() {
            logger.debug("Executing clean shutdown");
            deRegisterFromEM();
        }
    }

}
