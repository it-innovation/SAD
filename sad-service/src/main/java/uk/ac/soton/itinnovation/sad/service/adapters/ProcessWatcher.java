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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessWatcher implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Process p;
    private String stdOut, errOut;
    private BufferedReader stdInput, stdError;

    public ProcessWatcher(Process p) {
        this.p = p;
        this.stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        this.stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

        // Make shutdown clean
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    @Override
    public void run() {

        try {
            while ((stdOut = stdInput.readLine()) != null) {
                logger.debug(stdOut);
            }
        } catch (IOException ex) {
        } finally {
            try {
                stdInput.close();
            } catch (IOException ioex) {
            }
        }

        try {
            while ((errOut = stdError.readLine()) != null) {
                logger.debug(errOut);
            }
        } catch (IOException ex) {
        } finally {
            try {
                stdError.close();
            } catch (IOException ioex) {
            }
        }
    }

    /**
     * Private shut-down hook.
     */
    private class ShutdownHook extends Thread {

        @Override
        public void run() {
            logger.debug("Executing clean shutdown");

            try {
                stdInput.close();
            } catch (IOException ex) {
            }

            try {
                stdError.close();
            } catch (IOException ex) {
            }

            p.destroy();
        }
    }

}
