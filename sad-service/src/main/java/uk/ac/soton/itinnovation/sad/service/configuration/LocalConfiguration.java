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
package uk.ac.soton.itinnovation.sad.service.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Grabs default project configuration properties from application.properties.
 */
@ConfigurationProperties(prefix = "sad")
public class LocalConfiguration implements SadConfiguration {

    private String basepath;
    private String pluginsPath;
    private String coordinatorPath;
    private boolean resetDatabaseOnStart;
    private boolean eccEnabled;

    private EccConfiguration ecc;

    @Override
    public String getBasepath() {
        return basepath;
    }

    @Override
    public void setBasepath(String basepath) {
        this.basepath = basepath;
    }

    @Override
    public String getPluginsPath() {
        return pluginsPath;
    }

    @Override
    public void setPluginsPath(String pluginsPath) {
        this.pluginsPath = pluginsPath;
    }

    @Override
    public String getCoordinatorPath() {
        return coordinatorPath;
    }

    @Override
    public void setCoordinatorPath(String coordinatorPath) {
        this.coordinatorPath = coordinatorPath;
    }

    @Override
    public boolean isResetDatabaseOnStart() {
        return resetDatabaseOnStart;
    }

    @Override
    public void setResetDatabaseOnStart(boolean resetDatabaseOnStart) {
        this.resetDatabaseOnStart = resetDatabaseOnStart;
    }

    @Override
    public boolean isEccEnabled() {
        return eccEnabled;
    }

    @Override
    public void setEccEnabled(boolean eccEnabled) {
        this.eccEnabled = eccEnabled;
    }

    @Override
    public EccConfiguration getEcc() {
        return ecc;
    }

    @Override
    public void setEcc(EccConfiguration ecc) {
        this.ecc = ecc;
    }

}
