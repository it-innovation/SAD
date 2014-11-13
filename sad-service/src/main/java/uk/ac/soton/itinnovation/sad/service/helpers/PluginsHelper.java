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
//	Created Date :			2013-02-28
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.helpers;

import com.google.code.morphia.Datastore;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.io.Closeable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.sad.coordinator.Coordinator;
import uk.ac.soton.itinnovation.sad.coordinator.SADCollections;
import uk.ac.soton.itinnovation.sad.service.adapters.EccMetricClient;
import uk.ac.soton.itinnovation.sad.service.dao.SADJob;
import uk.ac.soton.itinnovation.sad.service.dao.SADJobData;
import uk.ac.soton.itinnovation.sad.service.dao.SADJobExecution;
import uk.ac.soton.itinnovation.sad.service.dao.SADJobMetadata;
import uk.ac.soton.itinnovation.sad.service.dao.SADWorkflow;
import uk.ac.soton.itinnovation.sad.service.domain.ExecStatus;

/**
 * Helper class for SAD plugins' Executions, DO NOT use anywhere but in plugins
 * as it kills threads on failure (on purpose).
 *
 */
public class PluginsHelper implements Closeable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final StringWriter sw = new StringWriter();
    private static PrintWriter pw;
    private final DataHelper dh = new DataHelper();

    private String coordinatorPath, mode;
    private final String pluginName;
    private String jobId, eccPort;
    private final JSONArray jobArguments, jobInputs, jobOutputs;
    private final Coordinator coordinator;
    private final ObjectId myExecutionDatabaseID;
    private int myExecutionID;
    private SADWorkflow workflow;
    private final SADJob sadJob;
    private final ObjectId workflowId;
    private EccMetricClient metricReporter;

    /**
     * Uses arguments passed to the main java method of the SAD plugin to
     * initialise the plugin within the framework.
     *
     * @param args arguments passed to the main java method of the plugin.
     */
    public PluginsHelper(String[] args) {

        // For printing exceptions
        pw = new PrintWriter(sw);

        // Default values
        coordinatorPath = "./src/main/resources/coordinator.json";
        jobId = "";
        eccPort = "";

        if (args.length > 2) {
            mode = args[0];
            coordinatorPath = args[1];
            jobId = args[2];
            eccPort = args[3];

            logger.debug("Using command line arguments: coordinatorPath='" + coordinatorPath + "', jobId=" + jobId + ", mode='" + mode + "', eccPort: '" + eccPort + "'");

        } else {
            logger.error("Invalid command line arguments, exiting");
            System.exit(0);
        }

        // Init Coordinator
        coordinator = new Coordinator(coordinatorPath);

        // Setup database
        coordinator.setupDatabase();

        Datastore datastore = coordinator.getDatastore();
        sadJob = datastore.get(SADJob.class, new ObjectId(jobId));
        if (null == sadJob) {
            logger.error("ERROR: Failed to find SADJob with ID [" + jobId + "] in the database");
            System.exit(1);
        }

        // Arguments
        jobArguments = JSONArray.fromObject(sadJob.getArguments());
        jobInputs = JSONArray.fromObject(sadJob.getInputs());
        jobOutputs = JSONArray.fromObject(sadJob.getOutputs());

        // PluginName
        pluginName = sadJob.getPluginName();

        // Find my workflow
        workflowId = sadJob.getSADWorkflowId();
        if (workflowId != null) {
            this.workflow = datastore.get(SADWorkflow.class, workflowId);
        }

        // Set SAD Job execution
        List<SADJobExecution> allExecutionsSoFar = coordinator.createQuery(SADJobExecution.class).field("SADJobID").equal(sadJob.getId()).asList();

        myExecutionID = 0;
        if (!allExecutionsSoFar.isEmpty()) {
            myExecutionID = allExecutionsSoFar.size();
        }

        myExecutionDatabaseID = (ObjectId) coordinator.saveObject(new SADJobExecution(
                myExecutionID,
                sadJob.getId(),
                "Execution by job [" + sadJob.getId() + "]",
                "started",
                new Timestamp(System.currentTimeMillis()),
                null)).getId();

        // Setup ECC metrics reporter
        boolean enableMetricReporter = true;
        if (eccPort == null) {
            enableMetricReporter = false;
        } else if (eccPort.equals("null")) {
            enableMetricReporter = false;
        }

        if (!enableMetricReporter) {
            logger.debug("Not creating ECCMetric reporter as ECC reporting is disabled");
        } else {
            logger.debug("Creating ECC Metric reporter on port " + eccPort);
            metricReporter = new EccMetricClient("localhost", Integer.parseInt(eccPort));

            // Receive measurement sets
            metricReporter.start();
        }
    }

    /**
     *
     * @return ECC socket client.
     */
    public EccMetricClient getMetricReporter() {
        return metricReporter;
    }

    /**
     * Reports a new measurement for a metric.
     *
     * @param entityName name of the entity associated with the metric.
     * @param attributeName name of the attribute associated with the metric.
     * @param value measured value.
     */
    public void sendMetric(String entityName, String attributeName, String value) {
        if (metricReporter == null) {
            logger.debug("ECC is disabled, value '" + value + "' for atribute with name '" + attributeName + "' will not be reported");

        } else {
            logger.debug("ECC: reporting value '" + value + "' for atribute with name '" + attributeName + "'");
            metricReporter.sendMetric(entityName, attributeName, value);

        }
    }

    /**
     * Creates new metric attribute.
     *
     * @param entityName name of the entity to associate the attribute with.
     * @param attributeName name of the new attribute.
     * @param description description of the new attribute.
     * @param type metric type behind the new attribute.
     * @param unit unit of the new metric behind the attribute.
     */
    public void addAttribute(String entityName, String attributeName, String description, String type, String unit) {
        if (metricReporter == null) {
            logger.debug("ECC is disabled, new attribute with name '" + attributeName + "' and description '" + description + "' will not be created");

        } else {
            logger.debug("ECC: creating new attribute '" + attributeName + "' (" + description + " : " + type + " : " + unit + ") for entity '" + entityName + "'");
            metricReporter.addAttribute(entityName, attributeName, description, type, unit);
        }
    }

    /**
     * Creates new entity.
     *
     * @param metricGeneratorName name of the metric generator to use.
     * @param entityName name of the new entity.
     * @param entityDescription description of the new entity.
     */
    public void addEntity(String metricGeneratorName, String entityName, String entityDescription) {
        if (metricReporter == null) {
            logger.debug("ECC is disabled, new entity with name '" + entityName + "' and description '" + entityDescription + "' will not be created");

        } else {
            logger.debug("ECC: creating new entity '" + entityName + "' (" + entityDescription + ") for metric generator '" + metricGeneratorName + "'");
            metricReporter.addEntity(metricGeneratorName, entityName, entityDescription);
        }
    }

    /**
     * Reports prov action by a user.
     *
     * @param user_id unique user ID of the user performing the action.
     * @param reportAsJson the name of the action.
     */
    public void reportProv(String user_id, String reportAsJson) {
        if (metricReporter == null) {
            logger.debug("ECC is disabled, prov data '" + user_id + "' - '" + reportAsJson + "' will not be created");

        } else {
            logger.debug("ECC: reporting prov data '" + user_id + "' (" + reportAsJson + ")");
            metricReporter.reportProv(user_id, reportAsJson);
        }
    }

    /**
     * Sets status of SAD Job Execution "success" and finished timestamp to now.
     */
    public void reportExecutionSuccess() {
        SADJobExecution jobExecution = coordinator.getDatastore().get(SADJobExecution.class, myExecutionDatabaseID);

        jobExecution.setStatus(ExecStatus.FINISHED);
        jobExecution.setWhenFinished(new Timestamp(System.currentTimeMillis()));

        coordinator.saveObject(jobExecution);

        if (metricReporter != null) {
            metricReporter.stop();
        }

        coordinator.closeMongo();
    }

    /**
     * Writes org.json.JSON data into the job's data database table.
     *
     * @param dataType database data type.
     * @param dataAsJson data as a JSON object.
     * @param whenCollected timestamp of the data collection.
     */
    public void saveData(String dataType, org.json.JSONObject dataAsJson, Timestamp whenCollected) {
        saveData(dataType, JSONObject.fromObject(dataAsJson.toString()), whenCollected);
    }

    /**
     * Writes com.restfb.json.JsonObject data into the job's data database
     * table.
     *
     * @param dataType database data type.
     * @param dataAsJson data as a JSON object.
     * @param whenCollected timestamp of the data collection.
     */
    public void saveData(String dataType, com.restfb.json.JsonObject dataAsJson, Timestamp whenCollected) {
        saveData(dataType, JSONObject.fromObject(dataAsJson.toString()), whenCollected);
    }

    /**
     * Writes net.sf.json.JSONObject data into the job's data database table.
     *
     * @param dataType database data type.
     * @param dataAsJson data as a JSON object.
     * @param whenCollected timestamp of the data collection.
     */
    public void saveData(String dataType, JSONObject dataAsJson, Timestamp whenCollected) {
        List<JSONObject> asList = Arrays.asList(dataAsJson);
        saveData(dataType, asList, whenCollected);
    }

    /**
     * Saves a list of JSON objects of the same type into the database.
     *
     * @param dataType
     * @param dataAsJson
     * @param whenCollected
     */
    public void saveData(String dataType, List<JSONObject> dataAsJson, Timestamp whenCollected) {
        DBCollection collection = coordinator.getDBCollection(SADCollections.SADJobData);
        List<DBObject> documents = new ArrayList<>();

        for (JSONObject object : dataAsJson) {
            documents.add(
                    dh.jsonToDBObject(object, this.sadJob.getId(), Integer.toString(this.myExecutionID), this.myExecutionDatabaseID, dataType, this.pluginName, whenCollected)
            );
        }
        collection.insert(documents);
    }

    /**
     * @return path to the Coordinator's configuration file.
     */
    public String getCoordinatorPath() {
        return coordinatorPath;
    }

    /**
     * @return the name of the plugin.
     */
    public String getPluginName() {
        return pluginName;
    }

    /**
     * @return SAD Job's database ID for this execution.
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * @return SAD Execution ID.
     */
    public int getExecutionID() {
        return myExecutionID;
    }

    /**
     * @return SAD Execution Database ID.
     */
    public ObjectId getExecutionDatabaseID() {
        return myExecutionDatabaseID;
    }

    /**
     * @return arguments of the SAD Job being executed.
     */
    public JSONArray getJobArguments() {
        return jobArguments;
    }

    /**
     *
     * @param argumentName name of the argument.
     * @return the first value of the argument as String.
     */
    public String getArgumentValue(String argumentName) {

        if (argumentName == null) {
            logger.error("Requested argument's name is NULL");
            return null;
        }

        if (argumentName.replaceAll("\\s+", "").length() < 1) {
            logger.error("Requested argument's name is an empty string (minus the whitespace)");
            return null;
        }

        if (jobArguments.size() < 1) {
            logger.warn("Requested argument '" + argumentName + "' can not be found because no arguments were passed to this execution.");
            return null;
        } else {
            boolean foundArgumentWithName = false;
            JSONObject tempArgumentAsJson;
            String argumentValue = "";
            for (int i = 0; i < jobArguments.size(); i++) {
                tempArgumentAsJson = jobArguments.getJSONObject(i);
                if (tempArgumentAsJson.containsKey(argumentName)) {
                    argumentValue = tempArgumentAsJson.getString(argumentName);
                    foundArgumentWithName = true;
                    break;
                }
            }

            if (foundArgumentWithName) {
                logger.debug("Returning value '" + argumentValue + "' for argument '" + argumentName + "'");
                return argumentValue;
            } else {
                logger.debug("Requested argument '" + argumentName + "' was not found in arguments passed to this execution.");
                return null;
            }
        }
    }

    /**
     *
     * @param argumentName name of the argument.
     * @return all values of an argument by name.
     */
    public ArrayList<String> getArgumentValues(String argumentName) {

        ArrayList<String> result = new ArrayList<>();

        if (argumentName == null) {
            logger.error("Requested argument's name is NULL");
            return result;
        }

        if (argumentName.replaceAll("\\s+", "").length() < 1) {
            logger.error("Requested argument's name is an empty string (minus the whitespace)");
            return result;
        }

        if (jobArguments.size() < 1) {
            logger.warn("Requested argument '" + argumentName + "' can not be found because no arguments were passed to this execution.");
        } else {
            JSONObject tempArgumentAsJson;
            for (int i = 0; i < jobArguments.size(); i++) {
                tempArgumentAsJson = jobArguments.getJSONObject(i);
                if (tempArgumentAsJson.containsKey(argumentName)) {
                    result.add(tempArgumentAsJson.getString(argumentName));
                }
            }

            if (result.size() > 0) {
                logger.debug("Returning '" + result.size() + "' values for argument '" + argumentName + "'");
            } else {
                logger.warn("Requested argument '" + argumentName + "' was not found in arguments passed to this execution.");
            }
        }
        return result;
    }

    /**
     *
     * @param keyName name of the key in the metadata.
     * @return metadata value for the job given a key.
     */
    public String getMetadataValueForKey(String keyName) {
        String value = null;
        logger.debug("Returning metadata value for key '" + keyName + "'");

        SADJobMetadata jobMetadata = coordinator.createQuery(SADJobMetadata.class).field("SADJobID").equal(new ObjectId(getJobId())).field("key").equal(keyName).get();

        if (jobMetadata != null) {
            value = jobMetadata.getValue();
            logger.debug("Metadata value for key '" + keyName + "' is '" + value + "'");
        } else {
            logger.debug("Metadata value for key '" + keyName + "' is NULL");
        }

        return value;
    }

    /**
     * Saves or updates job metadata key and value.
     *
     * @param keyName name of the key to save.
     * @param keyValue corresponding value to save.
     */
    public void putMetadataValueForKey(String keyName, String keyValue) {
        if (keyName == null) {
            logger.error("Failed to save job metadata: NULL key");
            return;
        }

        if (keyValue == null) {
            logger.error("Failed to save job metadata: '" + keyValue + "' value is NULL!");
            return;
        }

        logger.debug("Setting key '" + keyName + "' with value '" + keyValue + "'");

        SADJobMetadata jobMetadata = coordinator.createQuery(SADJobMetadata.class).field("SADJobID").equal(new ObjectId(getJobId())).field("key").equal(keyName).get();
        if (jobMetadata != null) {
            jobMetadata.setValue(keyValue);
        } else {
            jobMetadata = new SADJobMetadata(sadJob.getId(), keyName, keyValue);
        }
        coordinator.saveObject(jobMetadata);

    }

    /**
     * @return all requested output types.
     */
    public ArrayList<String> getRequestedOutputTypes() {

        logger.debug("Request: output types for job [" + getJobId() + "]");

        ArrayList<String> result = new ArrayList<>();

        if (jobOutputs == null) {
            logger.error("Job outputs are NULL");
            return result;
        }

        if (jobOutputs.isEmpty()) {
            logger.error("Job outputs are EMPTY");
            return result;
        }

        int numOutputs = jobOutputs.size();
        logger.debug("Processing " + numOutputs + " output(s)");

        String tempValue;
        JSONObject tempOutput;
        for (int i = 0; i < numOutputs; i++) {
            tempOutput = jobOutputs.getJSONObject(i);
            if (tempOutput.containsKey("type")) {
                tempValue = tempOutput.getString("type");
                result.add(tempValue);
                logger.debug("Added output type " + tempValue);
            }
        }

        logger.debug("Response: found " + result.size() + " output types");

        return result;
    }

    /**
     * @return list of input job IDs.
     */
    public ArrayList<String> getInputJobIds() {
        ArrayList<String> result = new ArrayList<>();

        logger.debug("Returning input job IDs");

        if (jobInputs == null) {
            logger.error("Job inputs are NULL");
            return result;
        }

        if (jobInputs.isEmpty()) {
            logger.error("Job inputs are EMPTY");
            return result;
        }

        int numInputs = jobInputs.size();
        JSONObject tempInput;
        String temp_input_jobId;

        for (int i = 0; i < numInputs; i++) {
            tempInput = jobInputs.getJSONObject(i);
            if (tempInput.containsKey("job_id")) {
                temp_input_jobId = tempInput.getString("job_id");
                result.add(temp_input_jobId);
                logger.debug("Detected input 'job_id': " + temp_input_jobId);
            }
        }

        return result;
    }

    /**
     * @return input job IDs as array of ObjectIds for easy mongo integration.
     */
    public ObjectId[] getInputJobIdsAsObjectIds() {
        ArrayList<String> inputIds = getInputJobIds();
        ObjectId[] result = new ObjectId[inputIds.size()];

        if (!inputIds.isEmpty()) {
            int i = 0;
            for (String id : inputIds) {
                result[i] = new ObjectId(id);
                i++;
            }
        }

        return result;
    }

    /**
     * @return list of input plugin names
     */
    public ArrayList<String> getInputPluginNames() {
        ArrayList<String> result = new ArrayList<>();

        logger.debug("Returning input plugin names");

        if (jobInputs == null) {
            logger.error("Job inputs are NULL");
            return result;
        }

        if (jobInputs.isEmpty()) {
            logger.error("Job inputs are EMPTY");
            return result;
        }

        int numInputs = jobInputs.size();
        JSONObject tempInput;
        String temp_input_pluginName;

        for (int i = 0; i < numInputs; i++) {
            tempInput = jobInputs.getJSONObject(i);
            if (tempInput.containsKey("plugin_name")) {
                temp_input_pluginName = tempInput.getString("plugin_name");
                result.add(temp_input_pluginName);
                logger.debug("Detected input 'plugin_name': " + temp_input_pluginName);
            }
        }

        return result;
    }

    /**
     * @return input job IDs as array of ObjectIds for easy mongo integration.
     */
    public String[] getInputPluginNamesAsStringArray() {
        ArrayList<String> pluginNames = getInputPluginNames();

        return pluginNames.toArray(new String[pluginNames.size()]);
    }

    /**
     * Use to start searching only input data for the job.
     *
     * @return database object with job IDs/plugin names selected.
     */
    public BasicDBObject getInputDataQuery() {
        BasicDBObject searchQuery = new BasicDBObject();

        // Only search in input data for this job
        ObjectId[] jobIds = getInputJobIdsAsObjectIds();
        String[] pluginNames = getInputPluginNamesAsStringArray();
        BasicDBList or = new BasicDBList();
        or.add(new BasicDBObject("pluginName", new BasicDBObject("$in", pluginNames)));
        or.add(new BasicDBObject("SADJobID", new BasicDBObject("$in", jobIds)));
        searchQuery.append("$or", or);

        logger.debug("Returning query based on input data: " + searchQuery.toString());

        return searchQuery;
    }

    /**
     * @param type data type.
     * @param num_posts number of ites to retrieve from the database.
     * @return all input data items as JSON from the database.
     */
    public ArrayList<JSONObject> getInputData(String type, String num_posts) {

        logger.debug("Request: input data for job [" + getJobId() + "] of type '" + type + "' with limit of '" + num_posts + "' posts");

        ArrayList<JSONObject> result = new ArrayList<>();

        if (jobInputs == null) {
            logger.error("Job inputs are NULL");
            return result;
        }

        if (jobInputs.isEmpty()) {
            logger.error("Job inputs are EMPTY");
            return result;
        }

        int numInputs = jobInputs.size();
        ArrayList<String> input_jobIds = new ArrayList<>();
        ArrayList<String> input_pluginNames = new ArrayList<>();
        JSONObject tempInput;
        String temp_input_pluginName, temp_input_jobId;

        for (int i = 0; i < numInputs; i++) {
            tempInput = jobInputs.getJSONObject(i);
            if (tempInput.containsKey("job_id")) {
                temp_input_jobId = tempInput.getString("job_id");
                input_jobIds.add(temp_input_jobId);
                logger.debug("Detected input 'job_id': " + temp_input_jobId);
            }
            if (tempInput.containsKey("plugin_name")) {
                temp_input_pluginName = tempInput.getString("plugin_name");
                input_pluginNames.add(temp_input_pluginName);
                logger.debug("Detected input 'plugin_name': " + temp_input_pluginName);
            }
        }

        if (input_pluginNames.isEmpty() && input_jobIds.isEmpty()) {
            logger.error("No input values found for keys 'job_id' or 'plugin_name'");
            return result;
        }

        if (!input_jobIds.isEmpty()) {
            logger.debug("Using " + input_jobIds.size() + " job IDs to get job data");
        }

        if (!input_pluginNames.isEmpty()) {
            logger.debug("Using " + input_pluginNames.size() + " plugin names to get job data");
        }

        int num_posts_int;
        if (num_posts == null) {
            num_posts_int = -1;
        } else if (num_posts.replace("\\s+", "").trim().length() < 1) {
            num_posts_int = -1;
        } else if (num_posts.equals("all")) {
            num_posts_int = -1;
        } else {
            try {
                num_posts_int = Integer.parseInt(num_posts);
            } catch (NumberFormatException ex) {
                logger.error("Failed to convert input string num_posts '" + num_posts + "' to integer");
                num_posts_int = -1;
            }
        }

        logger.debug("Searching database using " + num_posts_int + " as the number of posts. If '-1', returning all posts.");

        DBCollection collection = coordinator.getDBCollection(SADCollections.SADJobData);
        BasicDBObject filter, sorter = new BasicDBObject("whenCollected", -1);
        DBCursor cursor;
        DBObject object;
        SADJobData sadjd;
        ArrayList<SADJobData> jobData = new ArrayList<>();
        if (!input_pluginNames.isEmpty()) {
            for (String input_pluginName : input_pluginNames) {
                filter = new BasicDBObject("pluginName", input_pluginName);
                cursor = collection.find(filter).sort(sorter);
                if (num_posts_int > 0) {
                    cursor = cursor.limit(num_posts_int);
                }
                while (cursor.hasNext()) {
                    object = cursor.next();
                    sadjd = dh.dBObjectToSADJobData(object);
                    jobData.add(sadjd);
                }
            }
        }
        if (!input_jobIds.isEmpty()) {
            // Using job ids as an input
            for (String input_jobId : input_jobIds) {
                filter = new BasicDBObject("SADJobID", new ObjectId(input_jobId));
                cursor = collection.find(filter).sort(sorter);
                if (num_posts_int > 0) {
                    cursor = cursor.limit(num_posts_int);
                }
                while (cursor.hasNext()) {
                    object = cursor.next();
                    sadjd = dh.dBObjectToSADJobData(object);
                    jobData.add(sadjd);
                }
            }
        }

        if (jobData.isEmpty()) {
            logger.debug("No database entries where found");
            return result;
        } else {
            logger.debug("Processing " + jobData.size() + " data entries");

            String returnResultsOfType;
            if (type == null) {
                returnResultsOfType = null;
            } else if (type.replace("\\s+", "").trim().length() < 1) {
                returnResultsOfType = null;
            } else {
                returnResultsOfType = type;
            }

            if (returnResultsOfType == null) {
                logger.debug("Looking for data of any type");
            } else {
                logger.debug("Looking for data of type '" + returnResultsOfType + "'");
            }

            for (SADJobData dataEntry : jobData) {
                if (returnResultsOfType != null) {
                    if (dataEntry.getDataType().equalsIgnoreCase(returnResultsOfType)) {
                        result.add(JSONObject.fromObject(dataEntry.getJsonData()));
                    }
                } else {
                    result.add(JSONObject.fromObject(dataEntry.getJsonData()));
                }
            }

            logger.debug("Returning " + result.size() + " input data items");

            return result;
        }

    }

    /**
     * @return connection to data collection.
     */
    public DBCollection getDataCollection() {
        return coordinator.getDBCollection(SADCollections.SADJobData);
    }

    /**
     * @return database coordinator object.
     */
    public Coordinator getCoordinator() {
        return coordinator;
    }

    /**
     * @return inputs of the SAD Job being executed.
     */
    public JSONArray getJobInputs() {
        return jobInputs;
    }

    /**
     * @return outputs of the SAD Job being executed.
     */
    public JSONArray getJobOutputs() {
        return jobOutputs;
    }

    /**
     * Into stdout prints: whatHappened, ex stacktrace, then calls
     * System.exit(1).
     *
     * @param whatHappened human readable description of what went wrong.
     * @param ex exception caught.
     */
    public void dealWithException(String whatHappened, Throwable ex) {
        System.err.println(whatHappened);
        if (ex != null) {
            ex.printStackTrace(pw);
            System.err.println(sw.toString());
        }
        try {
            coordinator.closeMongo();
        } catch (Throwable ex1) {
        }
        try {
            if (metricReporter != null) {
                metricReporter.stop();
            }
        } catch (Throwable ex1) {
        }
        System.exit(1); // Important: PluginRunner needs to know that something went wrong
    }

    /**
     * @return the workflow for the job, null if it's a standalone job.
     */
    public ObjectId getWorkflowId() {
        return workflowId;
    }

    /**
     * @return the workflow ID for the job, -1 if it's a standalone job.
     */
    public SADWorkflow getWorkflow() {
        return workflow;
    }

    /**
     * Attempts clean object removal.
     */
    @Override
    public void close() {
        if (metricReporter != null) {
            metricReporter.stop();
        }

        coordinator.closeMongo();
    }

}
