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
package uk.ac.soton.itinnovation.sad.service.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic JSON response.
 */
@JsonInclude(Include.NON_NULL)
public class JsonResponse {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String result;
    private JsonNode response;

    public JsonResponse() {

    }

    public JsonResponse(String result, JSONObject response) {
        this.result = result;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        JsonFactory factory = mapper.getJsonFactory();
        try {
            JsonParser jp = factory.createJsonParser(response.toString());
            this.response = mapper.readTree(jp);
        } catch (IOException ex) {
            logger.error("Failed to parse JSONObject", ex);
        }
    }

    public JsonResponse(String result, JSONArray response) {
        this.result = result;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        JsonFactory factory = mapper.getJsonFactory();
        try {
            JsonParser jp = factory.createJsonParser(response.toString());
            this.response = mapper.readTree(jp);
        } catch (IOException ex) {
            logger.error("Failed to parse JSONArray", ex);
        }
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public JsonNode getResponse() {
        return response;
    }

    public void setResponse(JsonNode response) {
        this.response = response;
    }
}
