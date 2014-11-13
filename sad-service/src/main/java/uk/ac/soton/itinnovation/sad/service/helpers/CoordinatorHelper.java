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
//	Created Date :			2013-01-16
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.soton.itinnovation.sad.coordinator.Coordinator;

/**
 * Ensures only one Coordinator instance is in use by the SAD Service.
 */
@Service
public class CoordinatorHelper {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static Coordinator coordinator;

    /**
     * Returns SAD Coordinator instance.
     *
     * @param pathToConfigurationFile location of the database configuration
     * file.
     * @param doDatabaseReset if to wipe the database completely on init.
     * @return the instance of the SAD coordinator in use.
     */
    public synchronized Coordinator getCoordinator(String pathToConfigurationFile, boolean doDatabaseReset) {
        return getStaticCoordinator(pathToConfigurationFile, doDatabaseReset);
    }

    /**
     * Ensures only one instance of the Coordinator is used by the service.
     *
     * @param pathToConfigurationFile location of the database configuration
     * file.
     * @param doDatabaseReset if to wipe the database completely on init.
     * @return new coordinator if it doesn't exist.
     */
    public Coordinator getStaticCoordinator(String pathToConfigurationFile, boolean doDatabaseReset) {

        if (coordinator == null || !coordinator.isDatabaseInitialised()) {
            logger.debug("Creating fresh instance of the Coordinator");

            coordinator = new Coordinator(pathToConfigurationFile);
            if (doDatabaseReset) {
                logger.warn("Resetting the database");
                coordinator.deleteDatabase();
            }
            coordinator.setupDatabase();
        }

        return coordinator;
    }
}
