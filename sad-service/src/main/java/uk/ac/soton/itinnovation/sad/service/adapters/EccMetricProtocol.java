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
//	Created Date :			2013-08-27
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.adapters;

import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EccMetricProtocol {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String MEASUREMENT_REPORT = "measurement";
    public static final String ADD_ATTRIBUTE = "add_attribute";
    public static final String ADD_ENTITY = "add_entity";
    public static final String REPORT_PROV = "report_prov";
    public static final String END_CONNECTION = "bye";
    private GenericEccClient eccClient;

    public EccMetricProtocol(GenericEccClient eccClient) {
        this.eccClient = eccClient;
    }

    public boolean processClientMessage(String message) {

        logger.debug("CLIENT SAYS to me: " + message);

        // Client finished reporting for now
        if (message.equals(END_CONNECTION)) {

            return false;

            // Process message and keep listening
        } else {
            JSONObject mesgAsJson = JSONObject.fromObject(message);

            if (mesgAsJson == null) {
                logger.error("Failed to parse message into JSON: " + message);
            } else {

                if (!mesgAsJson.containsKey("type")) {
                    logger.error("Message must contain 'type' field. The following message will be ignored:\n" + mesgAsJson.toString(2));
                } else {
                    String type = mesgAsJson.getString("type");

                    switch (type) {
                        case MEASUREMENT_REPORT:
                            logger.debug("Processing new measurement report message");
                            processNewMeasurementReportMessage(mesgAsJson);
                            break;
                        case ADD_ENTITY:
                            logger.debug("Processing add new entity message");
                            processAddNewEntityMessage(mesgAsJson);
                            break;
                        case ADD_ATTRIBUTE:
                            logger.debug("Processing add new attribute message");
                            processAddNewAttributeMessage(mesgAsJson);
                            break;
                        case REPORT_PROV:
                            logger.debug("Processing report prov message");
                            processReportProvMessage(mesgAsJson);
                            break;
                        default:
                            logger.error("Message of unknown type: '" + type + "', ignoring");
                            break;
                    }
                }
            }

            return true;
        }
    }

    private void processNewMeasurementReportMessage(JSONObject mesgAsJson) {
        if (mesgAsJson.containsKey("entity_name")) {
            if (mesgAsJson.containsKey("attribute_name")) {
                if (mesgAsJson.containsKey("value")) {
                    try {
                        eccClient.pushValueForAttribute(mesgAsJson.getString("entity_name"), mesgAsJson.getString("attribute_name"), mesgAsJson.getString("value"));
                    } catch (Throwable ex) {
                        logger.error("Failed to push value for attribute", ex);
                    }
                } else {
                    logger.error("Failed to get measurement value from message: key 'value' not found");
                }
            } else {
                logger.error("Failed to get measurement value from message: key 'attribute_name' not found");
            }
        } else {
            logger.error("Failed to get measurement value from message: key 'entity_name' not found");
        }
    }

    private void processAddNewEntityMessage(JSONObject mesgAsJson) {
        try {
            eccClient.addNewEntity(
                    mesgAsJson.getString("metricgenerator_name"),
                    mesgAsJson.getString("entity_name"),
                    mesgAsJson.getString("description"));
        } catch (Throwable ex) {
            logger.error("Failed to add new entity", ex);
        }
    }

    private void processAddNewAttributeMessage(JSONObject mesgAsJson) {
        try {
            eccClient.addNewAttribute(
                    mesgAsJson.getString("entity_name"),
                    mesgAsJson.getString("attribute_name"),
                    mesgAsJson.getString("description"),
                    mesgAsJson.getString("metric_type"),
                    mesgAsJson.getString("unit"));
        } catch (Throwable ex) {
            logger.error("Failed to add new attribute", ex);
        }
    }

    private void processReportProvMessage(JSONObject mesgAsJson) {
        try {
            eccClient.reportActionByPerson(
                    mesgAsJson.getString("user_id"),
                    mesgAsJson.getString("user_action"));
        } catch (Throwable ex) {
            logger.error("Failed to report action by person", ex);
        }
    }

    public JSONObject getEmptyMeasurementMessage() {
        JSONObject result = new JSONObject();

        result.put("type", MEASUREMENT_REPORT);

        return result;
    }

    public JSONObject getEmptyAddEntityMessage() {
        JSONObject result = new JSONObject();

        result.put("type", ADD_ENTITY);

        return result;
    }

    public JSONObject getEmptyAddAttributeMessage() {
        JSONObject result = new JSONObject();

        result.put("type", ADD_ATTRIBUTE);

        return result;
    }

    public JSONObject getEmptyReportProvMessage() {
        JSONObject result = new JSONObject();

        result.put("type", REPORT_PROV);

        return result;
    }
}
