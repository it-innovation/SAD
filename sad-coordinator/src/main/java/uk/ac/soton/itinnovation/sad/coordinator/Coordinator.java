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
//	Created Date :			2013-08-13
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.coordinator;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Key;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.logging.MorphiaLoggerFactory;
import com.google.code.morphia.query.Query;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Scanner;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.LoggerFactory;

/**
 * Database access class for SAD.
 */
public class Coordinator implements Closeable {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

    private static final String lineSeparator = System.getProperty("line.separator");
    private JSONObject configuration;
    private JSONArray configurationPojoPackages;

    private Mongo mongo;
    private Datastore datastore;

    private String configurationDatabaseName, configurationDatabaseMongoServerHost;
    private int configurationDatabaseMongoServerPort;

    private boolean databaseInitialised = false;

    /**
     * Requires valid path to configuration file.
     *
     * @param pathToConfigurationFile
     */
    public Coordinator(String pathToConfigurationFile) {

        logger.debug("New SAD coordinator initialising with the following path: " + pathToConfigurationFile);

        if (pathToConfigurationFile == null) {
            throw new RuntimeException("SAD coordinator configuration path can not be null");
        }

        if (pathToConfigurationFile.length() < 1) {
            throw new RuntimeException("SAD coordinator configuration path can not be empty");
        }

        File configurationFile = new File(pathToConfigurationFile);

        if (!configurationFile.exists()) {
            throw new RuntimeException("SAD coordinator configuration file does not exist on the following path: " + pathToConfigurationFile);
        }

        if (!configurationFile.isFile()) {
            throw new RuntimeException("SAD coordinator configuration file is not a file: " + pathToConfigurationFile);
        }

        String absPathToConfigurationFile;
        try {
            absPathToConfigurationFile = configurationFile.getCanonicalPath();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to resolve absolute path to Coordinator configuration file", ex);
        }

        logger.debug("Resolved SAD coordinator file path to: " + absPathToConfigurationFile);

        StringBuilder fileContents = new StringBuilder((int) configurationFile.length());
        Scanner scanner;
        try {
            scanner = new Scanner(configurationFile);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Coordinator configuration file not found", ex);
        }
        while (scanner.hasNextLine()) {
            fileContents.append(scanner.nextLine());
            fileContents.append(lineSeparator);
        }
        scanner.close();

        String configAsString = fileContents.toString();

        if (configAsString.length() < 1) {
            throw new RuntimeException("SAD coordinator configuration file can not be empty: " + absPathToConfigurationFile);
        }

        logger.debug("Loading SAD coordinator configuration: " + configAsString);

        configuration = JSONObject.fromObject(configAsString);

        if (!configuration.containsKey("database")) {
            throw new RuntimeException("SAD coordinator configuration file \'" + pathToConfigurationFile + "\' must contain field \'database\'");
        }

        if (!configuration.containsKey("pojo_packages")) {
            throw new RuntimeException("SAD coordinator configuration file \'" + pathToConfigurationFile + "\' must contain field \'pojo_packages\'");
        }

        JSONObject configurationDatabase = configuration.getJSONObject("database");

        if (!configurationDatabase.containsKey("mongo_server")) {
            throw new RuntimeException("SAD coordinator configuration file \'" + pathToConfigurationFile + "\' must contain field \'database/mongo_server\'");
        }
        if (!configurationDatabase.containsKey("name")) {
            throw new RuntimeException("SAD coordinator configuration file \'" + pathToConfigurationFile + "\' must contain field \'database/name\'");
        }

        JSONObject configurationDatabaseMongoServer = configurationDatabase.getJSONObject("mongo_server");

        if (!configurationDatabaseMongoServer.containsKey("host")) {
            throw new RuntimeException("SAD coordinator configuration file \'" + pathToConfigurationFile + "\' must contain field \'database/mongo_server/host\'");
        }
        if (!configurationDatabaseMongoServer.containsKey("port")) {
            throw new RuntimeException("SAD coordinator configuration file \'" + pathToConfigurationFile + "\' must contain field \'database/mongo_server/port\'");
        }

        configurationDatabaseMongoServerHost = configurationDatabaseMongoServer.getString("host");
        configurationDatabaseMongoServerPort = configurationDatabaseMongoServer.getInt("port");
        configurationDatabaseName = configurationDatabase.getString("name");

        configurationPojoPackages = configuration.getJSONArray("pojo_packages");

        if (configurationPojoPackages.isEmpty()) {
            logger.warn("No POJO package names detected");
        }

        logger.debug("Testing database connection");

        // connect to Mongo
        try {
            mongo = new Mongo(configurationDatabaseMongoServerHost, configurationDatabaseMongoServerPort);
        } catch (UnknownHostException ex) {
            throw new RuntimeException("Failed to connect to Mongo server, unknown host", ex);
        }

        logger.debug("Coordinator initialized successfully, call setupDatabase() or deleteDatabase() next");
    }

    /**
     * Sets up SAD database, this method has to be called for the Coordinator to
     * be usable (apart from deleteDatabase() method).
     */
    public void setupDatabase() {
        logger.debug("Setting up Mongo database \'" + configurationDatabaseName
                + "\' on server \'" + configurationDatabaseMongoServerHost + ":" + configurationDatabaseMongoServerPort + "\'");

        // Initialise Morphia
        MorphiaLoggerFactory.reset(); // TODO: look into this, only a temp fix
        MorphiaLoggerFactory.registerLogger(com.google.code.morphia.logging.slf4j.SLF4JLogrImplFactory.class);
        Morphia morphia = new Morphia();
        int numPackages = configurationPojoPackages.size();
        String packageName;

        for (int i = 0; i < numPackages; i++) {
            packageName = configurationPojoPackages.getJSONObject(i).getString("name");
            if (packageName != null) {
                logger.debug("Adding package: " + packageName + " to Morphia");
                morphia = morphia.mapPackage(packageName);
            }
        }

        datastore = morphia.createDatastore(mongo, configurationDatabaseName);
        databaseInitialised = true;

    }

    public <T> Query<T> createQuery(Class<T> type) {
        if (null == this.datastore) {
            setupDatabase();
        }
        Query<T> query = this.datastore.createQuery(type);
        return query;
    }

    /*
     * Saves object and updates @Id
     */
    public <T> Key<T> saveObject(T object) {
        if (null == this.datastore) {
            setupDatabase();
        }
        Key<T> key = this.datastore.save(object);
        return key;
    }

    public Datastore getDatastore() {
        if (null == this.datastore) {
            setupDatabase();
        }
        return this.datastore;
    }

    public DBCollection getDBCollection(SADCollections collection) {
        DBCollection coll = getDatastore().getDB().getCollection(collection.getName());
        return coll;
    }

    public DBCollection getDBCollection(String name) {
        DBCollection coll = getDatastore().getDB().getCollection(name);
        return coll;
    }

    public void closeMongo() {
        if (mongo != null) {
            logger.debug("Closing mongo connection");
            databaseInitialised = false;
            mongo.close();
            mongo.getConnector().close();
            logger.debug("Mongo connector isOpen: " + mongo.getConnector().isOpen());
        } else {
            logger.error("FAILED to close mongo connection because mongo=NULL");
        }
    }

    /**
     * DELETES main SAD database, can be called before setupDatabase() method.
     */
    public void deleteDatabase() {
        logger.warn("Database \'" + configurationDatabaseName + "\' on server \'" + configurationDatabaseMongoServerHost + ":" + configurationDatabaseMongoServerPort + "\' will be deleted now");
        mongo.dropDatabase(configurationDatabaseName);
    }

    /**
     * Indicates if setupDatabase() was ever called successfully.
     *
     * @return true if the database is initialised.
     */
    public boolean isDatabaseInitialised() {
        return databaseInitialised;
    }

    @Override
    public void close() throws IOException {
        closeMongo();
    }
}
