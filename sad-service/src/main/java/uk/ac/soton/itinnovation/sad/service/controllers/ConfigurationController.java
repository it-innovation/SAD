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
//	Created Date :			2014-07-07
//	Created for Project :           Sense4us
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.soton.itinnovation.sad.service.configuration.LocalConfiguration;
import uk.ac.soton.itinnovation.sad.service.configuration.SadConfiguration;
import uk.ac.soton.itinnovation.sad.service.services.ConfigurationService;

/**
 * Exposes SAD configuration endpoints.
 */
@Controller
@RequestMapping("/configuration")
public class ConfigurationController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    ConfigurationService configurationService;

    /**
     * @return configuration of this service, null if not yet configured.
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public SadConfiguration getSelectedConfiguration() {
        logger.debug("Returning selected service configuration");

        SadConfiguration configuration = configurationService.getConfiguration();

        if (configuration == null) {
            logger.error("Current SAD configuration is NULL");
        } else {
            logger.debug(JSONObject.fromObject(configuration).toString(2));
        }

        return configuration;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/ifinitialised")
    @ResponseBody
    public boolean ifInitialised() {
        boolean result = configurationService.isInisialised();
        logger.debug("Returning service initialisation status: " + result);
        return result;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/ifstarted")
    @ResponseBody
    public boolean ifStarted() {
        boolean result = configurationService.isStarted();
        logger.debug("Returning service start status: " + result);
        return result;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/restart")
    @ResponseBody
    public boolean restart() {
        configurationService.restart();
        return true;
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public boolean setConfiguration(@RequestBody LocalConfiguration newSadConfiguration) {
        logger.debug("Setting new configuration:");
        try {
            logger.debug(JSONObject.fromObject(mapper.writeValueAsString(newSadConfiguration)).toString(2));
        } catch (JsonProcessingException ex) {
            logger.error("Failed to parse submitted configuration to JSON", ex);
        }

        if (configurationService.setConfiguration(newSadConfiguration)) {
            return configurationService.startServices();
        } else {
            return false;
        }

    }
}
