/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.sad.service.configuration;

/**
 *
 */
public interface SadConfiguration {

    String getBasepath();

    String getCoordinatorPath();

    EccConfiguration getEcc();

    String getPluginsPath();

    boolean isEccEnabled();

    boolean isResetDatabaseOnStart();

    void setBasepath(String basepath);

    void setCoordinatorPath(String coordinatorPath);

    void setEcc(EccConfiguration ecc);

    void setEccEnabled(boolean eccEnabled);

    void setPluginsPath(String pluginsPath);

    void setResetDatabaseOnStart(boolean resetDatabaseOnStart);

}
