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
import java.net.ServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EccMetricServer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ServerSocket providerSocket;
    private boolean listening = true;

    private int port;
    private GenericEccClient eccClient;

    EccMetricServer(int port, GenericEccClient eccClient) {
        this.port = port;
        this.eccClient = eccClient;

    }

    public void start() {
        try {
            providerSocket = new ServerSocket(port, 10);
            logger.debug("Provider waiting for a new connection on port " + port);

            while (listening) {
                (new EccMetricServerConnectionHandler(providerSocket.accept(), eccClient)).run();
                logger.debug("Provider RESTARTED LISTENING on port " + port);
            }

            logger.debug("Provider STOPPED LISTENING on port " + port);

        } catch (IOException ex) {
            throw new RuntimeException("Error in Provider main cycle", ex);
        }

        logger.debug("Provider STOPPED");

    }

    public void stop() {
        logger.info("Shutting down Provider");

        try {
            if (providerSocket != null) {
                logger.debug("Closing ServerSocket");
                providerSocket.close();
            }
        } catch (IOException ex) {
            logger.error("Failed to close ServerSocket whilst killing Provider", ex);
        }
        logger.info("Provider shut down");
    }

}
