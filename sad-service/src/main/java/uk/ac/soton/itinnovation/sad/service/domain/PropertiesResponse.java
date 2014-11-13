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
//	Created Date :			2013-01-10
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.domain;

import net.sf.json.JSONObject;

/**
 * JSON response of Properties Service.
 */
public class PropertiesResponse {

    private String resultType;
    private JSONObject resultValue;

    public PropertiesResponse() {
    }

    public PropertiesResponse(String resultType, JSONObject resultValue) {
        this.resultType = resultType;
        this.resultValue = resultValue;
    }

    public JSONObject getResultValue() {
        return resultValue;
    }

    public void setResultValue(JSONObject resultValue) {
        this.resultValue = resultValue;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }
}
