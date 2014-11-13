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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Communicates with EccMetricServer.
 *
 */
public class EccMetricClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Socket requestSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final String host;
    private String message;
    private final int port;
    private JSONObject serverMessageAsJson;
    private JSONArray measurementSetsMapAsJson;
    private final EccMetricProtocol protocol;

    public EccMetricClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.protocol = new EccMetricProtocol(null);

        // Make shutdown clean
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    /**
     * Creates a new ECC socket client to host and port in the constructor.
     */
    public void start() {
        try {
            logger.debug("Connecting to host '" + host + "' on port '" + port + "'");
            requestSocket = new Socket(host, port);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());

            try {
                message = (String) in.readObject();
                serverMessageAsJson = JSONObject.fromObject(message);
                measurementSetsMapAsJson = serverMessageAsJson.getJSONArray("metric_model");
                if (serverMessageAsJson != null) {
                    logger.debug("SERVER SAYS to me:\n" + serverMessageAsJson.toString(2));
                } else {
                    logger.error("Failed to convert server's message [" + message + "] to JSON");
                }
            } catch (ClassNotFoundException classNot) {
                logger.error("Data received in unknown format", classNot);
            }

        } catch (UnknownHostException unknownHost) {
            logger.error("Failed to connect to host '" + host + "': unknown host", unknownHost);
        } catch (IOException ioException) {
            logger.error("Failed to connect to host '" + host + "' on port '" + port + "'", ioException);
        }
    }

    /**
     * Sends a new metric measurement to the ECC socket server.
     *
     * @param entityName name of the entity the metric belongs to.
     * @param attributeName name of the attribute the metric belongs to.
     * @param value value of the measurement.
     */
    public void sendMetric(String entityName, String attributeName, String value) {
        JSONObject messageAsJson = protocol.getEmptyMeasurementMessage();
        messageAsJson.put("entity_name", entityName);
        messageAsJson.put("attribute_name", attributeName);
        messageAsJson.put("value", value);
        sendMessage(messageAsJson.toString(), true);
    }

    /**
     * Tells the ECC server to add a new entity to the ECC metric model.
     *
     * @param metricgenerator_name name of the metric generator to use.
     * @param entity_name name of the new entity.
     * @param description description of the new entity.
     */
    public void addEntity(String metricgenerator_name, String entity_name, String description) {
        JSONObject messageAsJson = protocol.getEmptyAddEntityMessage();
        messageAsJson.put("metricgenerator_name", metricgenerator_name);
        messageAsJson.put("entity_name", entity_name);
        messageAsJson.put("description", description);
        sendMessage(messageAsJson.toString(), true);
    }

    /**
     * Tells the ECC server to add a new attribute and metric to the ECC metric
     * model.
     *
     * @param entity_name name of the entity to add the attribute to.
     * @param attribute_name name of the new attribute.
     * @param description description of the new attribute.
     * @param type metric type behind the new attribute.
     * @param unit unit of the new metric behind the attribute.
     */
    public void addAttribute(String entity_name, String attribute_name, String description, String type, String unit) {
        JSONObject messageAsJson = protocol.getEmptyAddAttributeMessage();
        messageAsJson.put("entity_name", entity_name);
        messageAsJson.put("attribute_name", attribute_name);
        messageAsJson.put("description", description);
        messageAsJson.put("metric_type", type);
        messageAsJson.put("unit", unit);
        sendMessage(messageAsJson.toString(), true);
    }

    /**
     * Reports proc user actions.
     *
     * @param user_id unique user ID of the user performing the action.
     * @param user_action the name of the action.
     */
    public void reportProv(String user_id, String user_action) {
        JSONObject messageAsJson = protocol.getEmptyReportProvMessage();
        messageAsJson.put("user_id", user_id);
        messageAsJson.put("user_action", user_action);
        sendMessage(messageAsJson.toString(), true);
    }

    /**
     * Sends a message to the ECC socket server.
     *
     * @param msg message as string.
     * @param waitForResponse if the client should wait for the response from
     * the server.
     */
    public void sendMessage(String msg, boolean waitForResponse) {
        try {
            out.writeObject(msg);
            out.flush();
            logger.debug("I as CLIENT SAY: " + msg);

        } catch (IOException ioException) {
            logger.error("Failed to send message: '" + msg + "'", ioException);
        }

        if (waitForResponse) {
            try {
                message = (String) in.readObject();
                logger.debug("SERVER REPLIES to me: " + message);
            } catch (IOException | ClassNotFoundException classNot) {
                logger.error("Failed to get acknowledgement from server", classNot);
            }
        }
    }

    /**
     * Stop the client and performs a clean shutdown.
     */
    public void stop() {
        logger.info("Shutting down Client");

        if (out != null) {
            if (requestSocket != null) {
                sendMessage(EccMetricProtocol.END_CONNECTION, false);
            }
        }

        try {
            if (in != null) {
                logger.debug("Closing ObjectInputStream");
                in.close();
            }
        } catch (IOException ex) {
            logger.error("Failed to close ObjectInputStream whilst killing Client", ex);
        }
        try {
            if (out != null) {
                logger.debug("Closing ObjectOutputStream");
                out.close();
            }
        } catch (IOException ex) {
            logger.error("Failed to close ObjectOutputStream whilst kill Client", ex);
        }
        try {
            if (requestSocket != null) {
                logger.debug("Closing Socket");
                requestSocket.close();
            }
        } catch (IOException ex) {
            logger.error("Failed to close ServerSocket whilst killing Client", ex);
        }
        logger.info("Client shut down");
    }

    /**
     * Private shut-down hook.
     */
    private class ShutdownHook extends Thread {

        @Override
        public void run() {
            logger.debug("Executing safe shutdown");
            try {
                if (in != null) {
                    logger.debug("Closing ObjectInputStream");
                    in.close();
                }
            } catch (IOException ex) {
                logger.error("Failed to close ObjectInputStream whilst killing Client", ex);
            }
            try {
                if (out != null) {
                    logger.debug("Closing ObjectOutputStream");
                    out.close();
                }
            } catch (IOException ex) {
                logger.error("Failed to close ObjectOutputStream whilst kill Client", ex);
            }
            try {
                if (requestSocket != null) {
                    logger.debug("Closing Socket");
                    requestSocket.close();
                }
            } catch (IOException ex) {
                logger.error("Failed to close ServerSocket whilst killing Client", ex);
            }
        }
    }
}
