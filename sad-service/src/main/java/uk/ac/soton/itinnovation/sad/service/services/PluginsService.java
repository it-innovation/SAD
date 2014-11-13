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
//	Created Date :			2013-01-14
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import javax.annotation.PostConstruct;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * SAD component that manages SAD plugins.
 */
@Service("pluginsService")
public class PluginsService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String PLUGIN_PROPERTY_NAME = "plugins";
    private static final String PLUGIN_CONFIGURATION_FILE_NAME = "configuration.json";
    private final String fileSeparator = System.getProperty("file.separator");
    private final String lineSeparator = System.getProperty("line.separator");

    @Autowired
    @Qualifier("configurationService")
    ConfigurationService configurationService;

    public PluginsService() {

    }

    @PostConstruct
    public void init() {
    }

    public boolean start() {
        return true;
    }

    /**
     * Returns plugin metadata by plugin name.
     *
     * @param pluginName
     * @return
     */
    public final JSONObject getPluginByName(String pluginName) {

        logger.debug("Returning plugin by name: " + pluginName);

        JSONObject result = new JSONObject();
        boolean success = false;

        JSONObject allPlugins = getLivePluginsInfo();

        logger.debug("Searching all plugins: " + allPlugins);

        Iterator pluginNames = allPlugins.keys();

        String tempPluginName;
        while (pluginNames.hasNext()) {
            tempPluginName = (String) pluginNames.next();

            if (tempPluginName.equals(pluginName)) {
                result = allPlugins.getJSONObject(pluginName);
                success = true;
                break;
            }
        }

        if (success) {
            logger.debug("Found matching plugin for name \'" + pluginName + "\': " + result.toString());
        } else {
            logger.error("Failed to find plugin for name \'" + pluginName
                    + "\'. Check plugin path in server configuration and individual plugin configuration files (" + PLUGIN_CONFIGURATION_FILE_NAME
                    + ") and try again");
        }

        return result;
    }

    /**
     * Returns plugins folder location.
     *
     * @return
     */
    public final String getPluginsPath() {
        logger.debug("Returning path to plugins using property name: " + PLUGIN_PROPERTY_NAME);

        return configurationService.getConfiguration().getPluginsPath();
    }

    /**
     * Returns plugins folder absolute location.
     *
     * @return
     */
    public final String getAbsolutePluginsPath() {
        String relativePath = getPluginsPath();

        File pluginPath = new File(relativePath);

        String canonicalPath;
        try {
            canonicalPath = pluginPath.getCanonicalPath();
        } catch (IOException ex) {
            throw new RuntimeException("Plugins path does not exist", ex);
        }

        return canonicalPath;
    }

    public ArrayList<String> getPluginNames() {
        ArrayList<String> result = new ArrayList<>();

        JSONObject livePluginsInfo = getLivePluginsInfo();

        Iterator<String> it = livePluginsInfo.keys();

        while (it.hasNext()) {
            result.add(it.next());
        }

        return result;
    }

    /**
     * Returns all plugins metadata directly from the file system.
     *
     * @return
     */
    public final JSONObject getLivePluginsInfo() {

        logger.debug("Returning plugins using property name: " + PLUGIN_PROPERTY_NAME);

//        JSONObject pluginsRoot = propertiesService.getLiveProperties().getJSONObject(PLUGIN_PROPERTY_NAME);
        String pluginsPath = configurationService.getConfiguration().getPluginsPath();

        logger.debug("Plugins folder filepath from configuration: " + pluginsPath);

        File pluginsFolder = new File(pluginsPath);
        JSONObject result = new JSONObject();

        if (!pluginsFolder.isDirectory()) {
            throw new RuntimeException("Plugins path does not exist or not a folder: " + pluginsFolder.getAbsolutePath());

        } else {
            File filenamesInPluginsFolder[] = pluginsFolder.listFiles();

            File tempFile, testConfig;
            Scanner scanner;
            StringBuilder fileContents;
            String pluginConfigAsString;
            for (int i = 0; i < filenamesInPluginsFolder.length; i++) {

                tempFile = filenamesInPluginsFolder[i];

                if (tempFile.isDirectory()) {
                    logger.debug("Looking in: " + tempFile.getAbsolutePath());

                    testConfig = new File(tempFile.getAbsolutePath() + fileSeparator + PLUGIN_CONFIGURATION_FILE_NAME);

                    if (!testConfig.exists()) {
                        logger.debug("Missing configuration file in: " + tempFile.getAbsolutePath());

                    } else {
                        logger.debug("Looking for plugin configuration in: " + tempFile.getAbsolutePath());

                        pluginConfigAsString = "";
                        fileContents = new StringBuilder((int) testConfig.length());

                        try {

                            scanner = new Scanner(testConfig);

                            while (scanner.hasNextLine()) {
                                fileContents.append(scanner.nextLine());
                                fileContents.append(lineSeparator);
                            }

                            scanner.close();

                            pluginConfigAsString = fileContents.toString();

                        } catch (FileNotFoundException ex) {
                            throw new RuntimeException("Failed to read plugin configuration file", ex);
                        }

                        if (pluginConfigAsString.length() < 1) {
                            logger.error("No configuration found in: " + tempFile.getAbsolutePath());

                        } else {
                            logger.debug("Found plugin configuration: " + pluginConfigAsString);
                            JSONObject foundConfig = JSONObject.fromObject(pluginConfigAsString);

                            boolean addPluginToResult = true;
                            if (foundConfig.containsKey("enabled")) {
                                if (foundConfig.getString("enabled").equals("n")) {
                                    addPluginToResult = false;
                                }
                            }

                            if (addPluginToResult) {
                                logger.debug("Adding pluginFolder to configuration: " + tempFile.getName());
                                foundConfig.put("pluginFolder", tempFile.getName());

                                String pluginName;
                                if (foundConfig.containsKey("name")) {
                                    pluginName = foundConfig.getString("name");

                                    logger.debug("Saving plugin configuration with name: " + pluginName);

                                    result.put(pluginName, foundConfig);

                                } else {
                                    pluginName = tempFile.getName();

                                    logger.error("Plugin name not found in configuration! Saving with file name: " + pluginName);

                                    result.put(pluginName, foundConfig);
                                }
                            } else {
                                logger.debug("Plugin disabled in configuration, skipping: " + foundConfig.getString("name"));
                            }
                        }
                    }

                } else {
                    logger.debug("Ignoring: " + tempFile.getAbsolutePath());

                }
            }

        }

        return result;
    }

}
