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
//	Created Date :			2014-06-27
//	Created for Project :           EXPERIMEDIA
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.services;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import uk.ac.soton.itinnovation.sad.service.configuration.LocalConfiguration;
import uk.ac.soton.itinnovation.sad.service.configuration.SadConfiguration;

/**
 * Keeps track of the configuration and starts other services.
 */
@Service("configurationService")
@Configuration
@EnableConfigurationProperties(LocalConfiguration.class)
public class ConfigurationService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private boolean inisialised = false;
    private boolean started = false;

    private SadConfiguration configuration;

    @Autowired
    LocalConfiguration localConfiguration;

    @Autowired
    @Qualifier("pluginsService")
    PluginsService pluginsService;

    @Autowired
    @Qualifier("schedulingService")
    SchedulingService schedulingService;

    @Autowired
    @Qualifier("eccIntegrationService")
    EccIntegrationService eccIntegrationService;

    @Autowired
    @Qualifier("EMClient")
    EMClient eMClient;

    public ConfigurationService() {
    }

    /**
     * Initialises the service.
     */
    @PostConstruct
    public void init() {
        logger.debug("Initialising SAD configuration service");

        if (localConfiguration == null) {
            logger.error("Failed to get local configuration, check 'application.properties' file is on classpath");
        } else {
            logger.debug(JSONObject.fromObject(localConfiguration).toString(2));
            configuration = localConfiguration;
            inisialised = true;
            started = false;
        }

        logger.debug("Finished initialising SAD configuration service");
    }

    /**
     * Ensures the service is shut down properly.
     */
    @PreDestroy
    public void shutdown() {
        logger.debug("Shutting down SAD configuration service");

        stopServices();

        logger.debug("SAD configuration shut down");
    }

    public void restart() {
        logger.debug("Restarting SAD configuration service");
        stopServices();
        init();
    }

    public SadConfiguration getConfiguration() {
        return configuration;
    }

    public boolean setConfiguration(SadConfiguration configuration) {
        this.configuration = configuration;
        return true;
    }

    public boolean isInisialised() {
        return inisialised;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean startServices() {

        if (!isInisialised()) {
            logger.error("Failed to start services because the Configuration is NOT INITIALISED");
            return false;
        }

        if (getConfiguration() == null) {
            logger.error("Failed to start services because the Configuration is NULL");
            return false;
        }

        boolean result = true;

        // plugins service - wait
        logger.debug("Starting services");
        if (pluginsService.start()) {
            // scheduling
            if (schedulingService.start()) {

            } else {
                logger.error("Failed to start scheduling service");
                result = false;
            }

            // ECC - only launch if the rest worked
            if (result) {
                if (getConfiguration().isEccEnabled()) {
                    logger.debug("Starting SAD ECC clients");
                    eMClient.start();
                    eccIntegrationService.start();
                }
            }

        } else {
            logger.error("Failed to start plugins service");
            result = false;
        }

        started = result;

        return result;
    }

    public boolean stopServices() {
        if (!isInisialised()) {
            logger.error("Failed to stop services because the Configuration is NOT INITIALISED");
            return false;
        }

        if (getConfiguration() == null) {
            logger.error("Failed to stop services because the Configuration is NULL");
            return false;
        }

        if (!started) {
            logger.error("Failed to stop services because the are NOT STARTED");
            return false;
        } else {

            schedulingService.stop();

            if (getConfiguration().isEccEnabled()) {
                logger.debug("Stopping SAD ECC clients");
                eMClient.stop();
                eccIntegrationService.stop();
            }

            started = false;
            return true;

        }

    }

}
