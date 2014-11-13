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
//	Created Date :			2013-08-21
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.adapters;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.amqpAPI.impl.amqp.AMQPBasicChannel;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.amqpAPI.impl.amqp.AMQPConnectionFactory;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.experiment.Experiment;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.Attribute;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.Entity;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.Measurement;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MeasurementSet;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MetricGenerator;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MetricGroup;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MetricHelper;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MetricType;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.Report;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.Unit;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.monitor.EMDataBatch;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.monitor.EMPhase;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.monitor.EMPostReportSummary;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.provenance.EDMAgent;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.provenance.EDMEntity;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.provenance.EDMProvFactory;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.provenance.EDMProvReport;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.samples.shared.EMIAdapterListener;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.samples.shared.EMInterfaceAdapter;

public abstract class GenericEccClient implements EMIAdapterListener, DisposableBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private boolean tryingToDisconnect = false, socketServerOnline = true, liveMonitoringStarted = false, pushingEnabled = false;

    private JSONObject eccProperties;

    // Connection to the ECC
    private AMQPBasicChannel amqpChannel;
    private EMInterfaceAdapter emiAdapter;
    private AMQPConnectionFactory amqpFactory;

    // EccMetricServer
    private EccMetricServer server;
    public HashMap<UUID, MeasurementSet> measurementSetMap = new HashMap<>();
    public String name;
    public Experiment currentExperiment;
    public MetricGenerator mainMetricGenerator;
    private boolean emClientSetup = false;

    // PROV
    private EDMProvFactory factory;

    public GenericEccClient(String name, String[] args) {

        this.name = name;

        if (args.length < 4) {
            throw new RuntimeException("Failed to create GenericEccClient: expected 4 items in args argument, found " + args.length);
        }

        // Make shutdown clean
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        String serviceConfigurationFilepath = args[1];
        String port = args[2];
        String uuid = args[3];

        String result;
        try {
            result = readFile(serviceConfigurationFilepath);
        } catch (IOException ex) {
            throw new RuntimeException("ERROR: Failed to get service configuration from file: " + serviceConfigurationFilepath, ex);
        }

        JSONObject serviceConfiguration = JSONObject.fromObject(result);

        logger.debug("Returned SAD configuration:\n" + serviceConfiguration.toString(2));

        eccProperties = serviceConfiguration.getJSONObject("ecc");

        String rabbitServerIP = eccProperties.getString("rabbitIp");
        int rabbitServerPort = Integer.parseInt(eccProperties.getString("rabbitPort"));
        UUID expMonitorID = UUID.fromString(eccProperties.getString("monitorId"));
        UUID clientID = UUID.fromString(uuid);
        logger.debug("Using given UUID: " + clientID.toString());

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

        String hostName = "";
        try {
            hostName = " - " + InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
        }

        try {
            emiAdapter.registerWithEM(name + hostName, amqpChannel, expMonitorID, clientID);
        } catch (Throwable ex) {
            throw new RuntimeException("Failed to register client [" + clientID + "] '" + name + "' with EM using experiment monitor ID [" + expMonitorID + "]", ex);
        }

        factory = EDMProvFactory.getInstance();

        emClientSetup = true;
        logger.debug("Successfully created new EM Client");

        logger.debug("ECC client set up: " + emClientSetup);

        // EccMetricServer is used to report metric model to plugin code,
        // receive metric reports
        server = new EccMetricServer(Integer.parseInt(port), this);
        logger.debug("Metric Socket starting");
        server.start();

        // Nothing will be executed below server.start(); line above
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

    private String readFile(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");

        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }

        return stringBuilder.toString();
    }

    public boolean isSocketServerOnline() {
        return socketServerOnline;
    }

    public void setSocketServerOnline(boolean socketServerOnline) {
        this.socketServerOnline = socketServerOnline;
    }

    public HashMap<UUID, MeasurementSet> getMeasurementSetMap() {
        return measurementSetMap;
    }

    public void setMeasurementSetMap(HashMap<UUID, MeasurementSet> measurementSetMap) {
        this.measurementSetMap = measurementSetMap;
    }

    public MetricGenerator getMainMetricGenerator() {
        return mainMetricGenerator;
    }

    @Override
    public void onEMConnectionResult(boolean connected, Experiment expInfo) {
        if (connected) {
            if (expInfo != null) {
                currentExperiment = new Experiment();
                currentExperiment.setName(expInfo.getName());
                currentExperiment.setDescription(expInfo.getDescription());
                currentExperiment.setStartTime(expInfo.getStartTime());

            } else {
                logger.error("Experiment information is NULL, disconnecting");
                tryDisconnecting();
            }
        } else {
            logger.error("Connection refused by ECC, disconnecting");
            tryDisconnecting();
        }

    }

    @Override
    public void onEMDeregistration(String reason) {
        logger.debug("De-registered by ECC because: " + reason);
        tryDisconnecting();
    }

    @Override
    public void onDescribeSupportedPhases(EnumSet<EMPhase> phasesOUT) {
        phasesOUT.add(EMPhase.eEMLiveMonitoring);
//        phasesOUT.add(EMPhase.eEMPostMonitoringReport);
    }

    @Override
    public void onDescribePushPullBehaviours(Boolean[] pushPullOUT) {
        pushPullOUT[0] = true;   // Will support pushing
        pushPullOUT[1] = false;  // No pulling
    }

    @Override
    public void onPopulateMetricGeneratorInfo() {
        if (currentExperiment != null) {
            logger.debug("Populating metric generators");

            // Run custom metric model generator
            measurementSetMap.clear();
            defineExperimentMetrics();

            logger.debug("Finished populating metric generators");

            // Report metrics to ECC
            emiAdapter.sendMetricGenerators(currentExperiment.getMetricGenerators());

        } else {
            logger.error("Failed to populating metric generators as current experiment is NULL, disconnecting");
            tryDisconnecting();
        }
    }

    public void defineExperimentMetrics() {

    }

    /**
     * Creates new entity dynamically.
     *
     * @param metricGeneratorName
     * @param newEntityName
     * @param newEntityDescription
     */
    public void addNewEntity(String metricGeneratorName, String newEntityName, String newEntityDescription) {
        Set<MetricGenerator> mgs = currentExperiment.getMetricGenerators();

        MetricGenerator m = null;
        for (MetricGenerator mg : mgs) {
            if (mg.getName().equals(metricGeneratorName)) {
                m = mg;
                break;
            }
        }

        if (m == null) {
            logger.error("Metric generator with the name '" + metricGeneratorName + "' does not exist");
        } else {
            Entity entity = null;
            for (Entity e : m.getEntities()) {
                if (e.getName().equals(newEntityName)) {
                    entity = e;
                    break;
                }
            }

            if (entity != null) {
                logger.error("Entity '" + newEntityName + "' already exists");
            } else {
                entity = new Entity();
                m.addEntity(entity);
                entity.setName(newEntityName);
                entity.setDescription(newEntityDescription);

                emiAdapter.sendMetricGenerators(mgs);

                // The only way to wait for ECC to save the new model:
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    logger.error("You interrupted my sleep!", ex);
                }

                logger.debug("Entity '" + newEntityName + "' was reported to ECC");

            }
        }
    }

    /**
     * Creates and reports a new attribute to the ECC.
     */
    public void addNewAttribute(String entityName, String attributeName, String description, String type, String unit) {
        Set<MetricGenerator> mgs = currentExperiment.getMetricGenerators();

        Entity entity = null;
        MetricGenerator m = null;
        MetricGroup mGroup = null;
        for (MetricGenerator mg : mgs) {
            for (Entity e : mg.getEntities()) {
                if (e.getName().equals(entityName)) {
                    entity = e;
                    m = mg;
                    break;
                }
            }
        }

        if (m != null) {
            if (entity != null) {

                boolean attributeExists = false;

                for (Attribute a : entity.getAttributes()) {
                    if (a.getName().equals(attributeName)) {
                        attributeExists = true;
                        break;
                    }
                }

                if (!attributeExists) {
                    MetricType mt = MetricType.RATIO; // NOMINAL, ORDINAL, INTERVAL, RATIO
                    switch (type.toLowerCase()) {
                        case "nominal":
                            mt = MetricType.NOMINAL;
                            break;
                        case "ordinal":
                            mt = MetricType.ORDINAL;
                            break;
                        case "interval":
                            mt = MetricType.INTERVAL;
                            break;
                    }

                    Attribute newAttribute = MetricHelper.createAttribute(attributeName, description, entity);
                    MeasurementSet newMeasurementSet = MetricHelper.createMeasurementSet(newAttribute, mt, new Unit(unit), m.getMetricGroups().iterator().next());
                    measurementSetMap.put(newMeasurementSet.getID(), newMeasurementSet);

                    emiAdapter.sendMetricGenerators(mgs);

                    // The only way to wait for ECC to save the new model:
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        logger.error("You interrupted my sleep!", ex);
                    }

                    logger.debug("Attribute '" + newAttribute + "' was reported to ECC");

                } else {
                    logger.debug("Attribute '" + attributeName + "' already exists");
                }
            }
        }

        int counter = 0;
        for (MetricGenerator mgen : currentExperiment.getMetricGenerators()) {
            printMetricGenerator(mgen, counter);
            counter++;
        }
    }

    public void pushValueForAttribute(String entityName, String attributeName, String value) {
        if (emClientSetup) {
            if (pushingEnabled) {
                logger.debug("Pushing value '" + value + "' for attribute [" + attributeName + "] to ECC");

                Set<MetricGenerator> mgs = currentExperiment.getMetricGenerators();

                Entity entity = null;
                MetricGenerator m = null;
                for (MetricGenerator mg : mgs) {
                    for (Entity e : mg.getEntities()) {
                        if (e.getName().equals(entityName)) {
                            entity = e;
                            m = mg;
                            break;
                        }
                    }
                }

                if (m != null) {
                    if (entity != null) {
                        Attribute a = MetricHelper.getAttributeByName(attributeName, entity);
                        MeasurementSet ms = MetricHelper.getMeasurementSetForAttribute(a, m);
                        if (ms == null) {
                            logger.error("NOT pushing value '" + value + "' for attribute [" + attributeName + "] to ECC because measurement set does not exist");
                        } else {
                            pushValueForMeasurementSetUuid(ms, value);
                        }
                    }
                }
            } else {
                logger.debug("NOT pushing value '" + value + "' for attribute [" + attributeName + "] to ECC because pushing is DISABLED");
            }
        } else {
            logger.debug("NOT pushing value '" + value + "' for attribute [" + attributeName + "] to ECC because  EM client is NOT enabled");
        }
    }

    public void pushValueForMeasurementSetUuid(MeasurementSet ms, String value) {
        if (emClientSetup) {
            if (pushingEnabled) {
                logger.debug("Pushing value '" + value + "' for measurement set [" + ms.getID().toString() + "] to ECC");

                Measurement valueAsMeasurement = new Measurement(value);

                Date now = new Date();
                Report report = MetricHelper.createEmptyMeasurementReport(ms);
                report.setNumberOfMeasurements(1);
                report.setReportDate(now);
                report.setFromDate(now);
                report.setToDate(now);

                report.getMeasurementSet().addMeasurement(valueAsMeasurement);

                emiAdapter.pushMetric(report);

                logger.debug("FINISHED pushing value '" + value + "' for measurement set [" + ms.getID().toString() + "] to ECC");

            } else {
                logger.debug("NOT pushing value '" + value + "' for measurement set [" + ms.getID().toString() + "] to ECC because pushing is DISABLED");
            }
        } else {
            logger.debug("NOT pushing value '" + value + "' for measurement set [" + ms.getID().toString() + "] to ECC because EM client is NOT enabled");
        }
    }

    public void reportActionByPerson(String userId, String reportAsJson) {
        if (emClientSetup) {
            if (pushingEnabled) {
                try {

                    logger.debug("Sending prov data: userId='" + userId + "', reportAsJson:\n" + reportAsJson);

                    JSONObject parsedAction = JSONObject.fromObject(reportAsJson);

                    logger.debug(parsedAction.toString(2));

                    JSONObject fbUserAsJson = parsedAction.getJSONObject("fbUserAsJson");
                    JSONObject fbCommentAsJson = parsedAction.getJSONObject("fbCommentAsJson");
                    JSONObject postFbCommentAsJson = parsedAction.getJSONObject("postFbCommentAsJson");
                    JSONObject fbPageAsJson = parsedAction.getJSONObject("fbPageAsJson");

                    // This is a user
                    String fbUserName = fbUserAsJson.getString("name");
                    String agentIdentifier = "experimedia:" + fbUserName + "_" + userId;

                    EDMAgent fbUserAsAgent = factory.getAgent(agentIdentifier);
                    if (fbUserAsAgent == null) {
                        fbUserAsAgent = factory.createAgent(agentIdentifier, "fbUserName");
                        fbUserAsAgent.addOwlClass("foaf:Person");
                    }

                    // The user performs an action on the page
                    String actionName = postFbCommentAsJson.getString("name");
                    String actionTimestamp = postFbCommentAsJson.getString("timestamp");
                    String activityIdentifier = "experimedia:" + actionName + "_" + actionTimestamp;
//            EDMActivity commentActivity = fbUserAsAgent.doDiscreteActivity(actionTimestamp, actionTimestamp); - no way to check if activity exists at least on agent

                    // This is a facebook comment
                    String entityId = fbCommentAsJson.getString("id");
                    String entityName = fbCommentAsJson.getString("name");
//            EDMEntity fbCommentAsEntity = commentActivity.generateEntity("experimedia:" + entityName + "_" + entityId, entityName);

                    // This is Facebook page
                    String fbPageName = fbPageAsJson.getString("name");
                    String entityIdentifier = "experimedia:facebookPage_" + fbPageName;
                    EDMEntity fbPage = factory.getEntity(entityIdentifier);
                    if (fbPage == null) {
                        fbPage = factory.createEntity(entityIdentifier, fbPageName);
                        //            fbPage.addOwlClass("prov:Location");
//            fbCommentAsEntity.addProperty("prov:atLocation", fbPage.getIri());
                    }

                    // Get factory to create a report containing the above PROV elements
                    EDMProvReport report = factory.createProvReport();

                    emiAdapter.pushPROVStatement(report);

                    logger.debug("FINISHED sending prov data: userId='" + userId);

                } catch (Throwable ex) {
                    logger.debug("Failed to report action by person as prov data", ex);
                }
            } else {
                logger.debug("NOT sending prov data: userId='" + userId + "' to ECC because pushing is DISABLED");
            }
        } else {
            logger.debug("NOT sending prov data: userId='" + userId + "' to ECC because  EM client is NOT enabled");
        }
    }

    /**
     * Prints a metric generator into logs as a tree.
     */
    public void printMetricGenerator(MetricGenerator mg, int numberInSet) {
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

    @Override
    public void onDiscoveryTimeOut() {
        logger.debug("onDiscoveryTimeOut");
    }

    @Override
    public void onSetupMetricGenerator(UUID uuid, Boolean[] resultOUT) {
        logger.debug("Reporting success setting up metric generator [" + uuid.toString() + "]");
        resultOUT[0] = true;
    }

    @Override
    public void onSetupTimeOut(UUID uuid) {
        logger.debug("onSetupTimeOut for UUID [" + uuid.toString() + "]");
    }

    @Override
    public void onLiveMonitoringStarted() {
        logger.debug("onLiveMonitoringStarted");
        liveMonitoringStarted = true;
    }

    @Override
    public void onStartPushingMetricData() {
        logger.debug("onStartPushingMetricData");
        pushingEnabled = true;

        // checked stored data, send
    }

    @Override
    public void onPushReportReceived(UUID reportID) {
        logger.debug("ECC says push report [" + reportID.toString() + "] RECEIVED");
    }

    @Override
    public void onStopPushingMetricData() {
        logger.debug("onStopPushingMetricData");
        pushingEnabled = false;

        // start saving reported data
    }

    @Override
    public void onPullReportReceived(UUID reportID) {
        logger.debug("ECC says pull report [" + reportID.toString() + "] RECEIVED");
    }

    @Override
    public void onPullMetric(UUID uuid, Report report) {
        logger.debug("onPullMetric UUID [" + uuid.toString() + "], report " + report.getUUID());
    }

    @Override
    public void onPullMetricTimeOut(UUID uuid) {
        logger.debug("onPullMetricTimeOut UUID [" + uuid.toString() + "]");
    }

    @Override
    public void onPullingStopped() {
        logger.debug("onPullingStopped");
    }

    @Override
    public void onPopulateSummaryReport(EMPostReportSummary emprs) {
        logger.debug("onPopulateSummaryReport");

    }

    @Override
    public void onPopulateDataBatch(EMDataBatch batchOUT) {
        logger.debug("onPopulateDataBatch");

        UUID msID = batchOUT.getExpectedMeasurementSetID();
        if (msID == null) {
            logger.warn("Expected measurement set ID for this batch is NULL");
        } else {
            logger.debug("Expected measurement set ID for this batch: " + msID.toString());
        }

    }

    @Override
    public void onReportBatchTimeOut(UUID uuid) {
        logger.debug("onReportBatchTimeOut UUID [" + uuid.toString() + "]");
    }

    @Override
    public void onGetTearDownResult(Boolean[] resultOUT) {
        logger.debug("Reporting successfull tear down result");
        resultOUT[0] = true;
    }

    @Override
    public void onTearDownTimeOut() {
        logger.debug("onTearDownTimeOut");
    }

//    @Override
//    public void onEntityMetricCollectionEnabled(UUID uuid, UUID uuid1, boolean bln) {
//        logger.debug("onEntityMetricCollectionEnabled " + uuid.toString() + ", " + uuid1.toString() + ", " + bln);
//    }
    /**
     * Attempts to disconnect from ECC.
     *
     * @return true if succeeded.
     */
    private synchronized boolean tryDisconnecting() {
        if (!tryingToDisconnect) {
            logger.debug("Disconnecting from ECC");
            try {
                tryingToDisconnect = true;
                emiAdapter.disconnectFromEM();
                if (amqpChannel != null) {
                    logger.debug("Closing amqpChannel");
                    amqpChannel.close();
                    logger.debug("amqpChannel open: " + amqpChannel.isOpen());
                }
                if (amqpFactory != null) {
                    logger.debug("Closing amqpFactory");
                    amqpFactory.closeDownConnection();
                    logger.debug("amqpFactory is connection valid: " + amqpFactory.isConnectionValid());
                }
                tryingToDisconnect = false;
                return true;
            } catch (Exception ex) {
                logger.error("Could not de-register from the ECC", ex);
                return false;
            }
        } else {
            logger.warn("Already in the process of de-registering from the ECC, repeat attempt detected");
            return false;
        }
    }

    @Override
    public void destroy() throws Exception {
        logger.debug("Destorying SAD plugin ECC client as bean");
        tryDisconnecting();
        server.stop();
    }

    /**
     * Private shut-down hook.
     */
    private class ShutdownHook extends Thread {

        @Override
        public void run() {
            cleanShutdown();
        }
    }

    public void cleanShutdown() {
        logger.info("Destorying SAD plugin ECC client via ShutdownHook");
        tryDisconnecting();
        server.stop();
    }
}
