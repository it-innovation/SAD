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
//	Created Date :			2013-02-12
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.sf.json.JSONObject;
import org.slf4j.Logger;

/**
 * Class with various useful utility methods.
 */
public class Util {

    private static final StringWriter sw = new StringWriter();

    /**
     * Converts millisecond string to java.util.Date.
     */
    public static Date i2d(String dateAsMsecString) {
        return new Date(Long.parseLong(dateAsMsecString));
    }

    /**
     * Makes sure that conrollers deal with exceptions correctly.
     */
    public static JSONObject dealWithException(String message, Throwable ex, Logger logger) {

        if (ex == null) {
            logger.error(message);
        } else {
            logger.error(message, ex);
        }

        JSONObject response = new JSONObject();

        response.put("message", message);
        if (ex != null) {
            ex.printStackTrace(new PrintWriter(sw)); // TODO: there is a better way
            response.put("stacktrace", sw.toString());
            response.put("description", ex.getMessage());
        }

        return response;
    }

    public static JSONObject dealWithException(String message, Logger logger) {
        return dealWithException(message, null, logger);
    }

    public static String timestampToString(Timestamp timestamp) {
        SimpleDateFormat dateFormatForJqplotGraphs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormatForJqplotGraphs.format(new Date(timestamp.getTime()));
    }
}
