/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2014
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
//	Created Date :			2014-01-23
//	Created for Project :           Sense4us
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.controllers;

import javax.servlet.http.HttpServletRequest;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.ModelAttribute;
import uk.ac.soton.itinnovation.sad.service.domain.JsonResponse;
import uk.ac.soton.itinnovation.sad.service.utils.Util;

/**
 * Generic controller with some helpful methods
 */
public class GenericController {

    private final static String OK_RESPONSE_TEXT = "ok";
    private final static String ERROR_RESPONSE_TEXT = "error";

    @ModelAttribute("clientIpAddress")
    private String populateClientIpAddress(HttpServletRequest request) {

        return request.getRemoteAddr();
    }

    JsonResponse okResponse(JSONObject response) {
        return new JsonResponse(OK_RESPONSE_TEXT, response);
    }

    JsonResponse errorResponse(String messageDescribingFailedAction, Throwable exceptionCaught, Logger loggerForClass) {
        return new JsonResponse(ERROR_RESPONSE_TEXT, Util.dealWithException(messageDescribingFailedAction, exceptionCaught, loggerForClass));
    }

}
