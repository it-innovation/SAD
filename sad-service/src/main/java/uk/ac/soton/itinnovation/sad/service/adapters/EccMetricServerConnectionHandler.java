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
//	Created Date :			2013-08-23
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.adapters;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MeasurementSet;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MetricGenerator;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MetricHelper;

public class EccMetricServerConnectionHandler implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Socket connection;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String message;
    private GenericEccClient eccClient;
    private EccMetricProtocol protocol;

    public EccMetricServerConnectionHandler(Socket connection, GenericEccClient eccClient) {
        this.connection = connection;
        this.eccClient = eccClient;
        this.protocol = new EccMetricProtocol(eccClient);
    }

    @Override
    public void run() {
        logger.debug("New connection received from " + connection.toString());

        try {

            out = new ObjectOutputStream(connection.getOutputStream());
            out.flush();
            in = new ObjectInputStream(connection.getInputStream());

            sendMetricGenerators();

            // Wait for metric reports
            boolean ifContinue = false;
            do {
                try {
                    message = (String) in.readObject();
                    ifContinue = protocol.processClientMessage(message);
                    sendMessage("ok", false);
                } catch (ClassNotFoundException classnot) {
                    logger.error("Data received in unknown format", classnot);
                }
            } while (ifContinue);

            logger.debug("Disconnecting client");

        } catch (IOException ex) {
            logger.error("Failed to initialise connection", ex);
        } finally {
            try {
                in.close();
                out.close();
                connection.close();
            } catch (IOException ex) {
                logger.error("Failed to close connection");
            }
        }
    }

    void sendMessage(String message, boolean waitForResponse) {
        try {
            out.writeObject(message);
            out.flush();
            logger.debug("I as SERVER SAY: " + message);
        } catch (IOException ex) {
            logger.error("Server sendMessage (" + message + ") error: " + ex);
        }

        if (waitForResponse) {
            try {
                message = (String) in.readObject();
                logger.debug("CLIENT REPLIED to me: " + message);
            } catch (IOException | ClassNotFoundException classNot) {
                logger.error("Failed to get acknowledgement from client", classNot);
            }
        }
    }

    private void sendMetricGenerators() {

        // Send metric generators on connection (if any)
        HashMap<UUID, MeasurementSet> measurementSetsMap = eccClient.getMeasurementSetMap();
        MetricGenerator mainGenerator = eccClient.getMainMetricGenerator();
        List<MetricGenerator> allMetricGenerators = new ArrayList<>();
        allMetricGenerators.add(mainGenerator);

        JSONObject welcomeMessage = new JSONObject();
        JSONArray measurementSetsMapAsJson = new JSONArray();

        Iterator<UUID> it = measurementSetsMap.keySet().iterator();
        UUID uuid;
        MeasurementSet ms;
        JSONObject tempEntry;
        while (it.hasNext()) {
            uuid = it.next();
            ms = measurementSetsMap.get(uuid);

            tempEntry = new JSONObject();
            tempEntry.put("uuid", uuid.toString());
            tempEntry.put("attribute_uuid", ms.getAttributeID().toString());
            tempEntry.put("attribute_name", MetricHelper.getAttributeFromID(ms.getAttributeID(), allMetricGenerators).getName());

            measurementSetsMapAsJson.add(tempEntry);
        }

        welcomeMessage.put("metric_model", measurementSetsMapAsJson);

        sendMessage(welcomeMessage.toString(), false);

    }

}
