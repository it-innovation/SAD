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
package uk.ac.soton.itinnovation.sad.service.services;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import javax.annotation.PostConstruct;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.soton.itinnovation.sad.service.adapters.ProcessWatcher;

@Service("eccIntegrationService")
public class EccIntegrationService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String fileSeparator = System.getProperty("file.separator");
    private final HashMap<String, String> pluginNamesAndPorts = new HashMap<>();
    private ArrayList<Process> createdProcesses;

    @Autowired
    @Qualifier("configurationService")
    ConfigurationService configurationService;

    @Autowired
    @Qualifier("pluginsService")
    PluginsService pluginsService;

    public EccIntegrationService() {
    }

    @PostConstruct
    public void init() {
    }

    public boolean start() {

        logger.debug("Initialising ECC Integration service");
        createdProcesses = new ArrayList<>();

        boolean result = true;

        // If ECC intagration enabled
        if (!configurationService.getConfiguration().isEccEnabled()) {
            logger.debug("Nothing to do, ECC integration disabled");

        } else {

            // Discover plugins
            JSONObject pluginInfo;
            String pathToPlugins = pluginsService.getAbsolutePluginsPath();
            JSONObject currentSadConfig = JSONObject.fromObject(configurationService.getConfiguration());
            String configurationFilePath;
            try {
                File tmp = File.createTempFile("sadconfiguration", ".json");
                tmp.deleteOnExit();

                File coordinatorPath = new File(configurationService.getConfiguration().getCoordinatorPath());

                currentSadConfig.put("coordinatorPathAbs", coordinatorPath.getAbsolutePath());

                FileWriter fileWriter = new FileWriter(tmp);
                fileWriter.write(currentSadConfig.toString());
                fileWriter.close();
                configurationFilePath = tmp.getAbsolutePath();
                logger.debug("Stored current configuration for plugins\n" + currentSadConfig.toString(2) + "\nin: " + configurationFilePath);
            } catch (IOException e) {
                logger.error("Failed to create temporary configuration file", e);
                return false;
            }
            ServerSocket s;

            Thread tempThread;
            int pluginCounter = 0;
            for (String pluginName : pluginsService.getPluginNames()) {

                pluginCounter++;

                logger.debug("Processing plugin " + pluginCounter + ": " + pluginName);
                pluginInfo = pluginsService.getPluginByName(pluginName);

                if (pluginInfo.isEmpty()) {
                    logger.error("Plugin not found: " + pluginName);
                    continue;
                }

                if (!pluginInfo.containsKey("paths")) {
                    logger.error("Plugin configuration must contain \'paths\'. Misconfigured plugin: " + pluginName);
                    continue;
                }

                if (!pluginInfo.containsKey("pluginFolder")) {
                    logger.error("Plugin configuration must be loaded with pluginsService that fills in the \'pluginFolder\' field. Misconfigured plugin: " + pluginName);
                    continue;
                }

                JSONObject pluginPaths = pluginInfo.getJSONObject("paths");

                if (!pluginPaths.containsKey("jar")) {
                    logger.error("Plugin configuration must contain \'paths/jar\'. Misconfigured plugin: " + pluginName);
                    continue;
                }
                if (!pluginPaths.containsKey("dependenciesFolder")) {
                    logger.error("Plugin configuration must contain \'paths/dependenciesFolder\'. Misconfigured plugin: " + pluginName);
                    continue;
                }
                if (!pluginInfo.containsKey("pluginFolder")) {
                    logger.error("Plugin configuration must contain \'pluginFolder\'. Misconfigured plugin: " + pluginName);
                    continue;
                }

                //                portNumber++;
                String jarPath = pluginPaths.getString("jar");
                String dependenciesFolderPath = pluginPaths.getString("dependenciesFolder");
                String pluginFolder = pluginInfo.getString("pluginFolder");
                String port = null;
                try {
                    s = new ServerSocket(0);
                    port = Integer.toString(s.getLocalPort());
                    s.close();
                } catch (IOException ex) {
                    logger.error("Failed to assign a new free port", ex);
                    throw new RuntimeException(ex);
                }

                String uuid = configurationService.getConfiguration().getEcc().getClientsUuuidSeed() + Integer.toHexString(pluginCounter);
                logger.debug("Using seeded UUID: " + uuid);

                logger.debug("Plugin jar path from configuration: " + jarPath + ", dependenciesFolderPath: " + dependenciesFolderPath);
                logger.debug("Plugin folder configuration: " + pathToPlugins);
                logger.debug("Socket port: " + port);
                logger.debug("ECC client UUID: " + uuid);

                String[] command = new String[]{
                    "java",
                    "-cp",
                    pathToPlugins + fileSeparator + pluginFolder + fileSeparator + dependenciesFolderPath + fileSeparator + "*",
                    "-jar",
                    pathToPlugins + fileSeparator + pluginFolder + fileSeparator + jarPath,
                    "ecc",
                    configurationFilePath,
                    port,
                    uuid
                };

                StringBuilder commandSb = new StringBuilder();
                for (String c : command) {
                    commandSb.append(c);
                    commandSb.append(" ");
                }
                String commandAsString = commandSb.toString();

                logger.debug("Launching ECC part of '" + pluginName + "' plugin with command: " + commandAsString + " on port: " + port);

                pluginNamesAndPorts.put(pluginName, port);

                try {
                    Process p = Runtime.getRuntime().exec(command);
                    tempThread = new Thread(new ProcessWatcher(p));
                    tempThread.start();
                    createdProcesses.add(p);

                } catch (Throwable ex) {
                    logger.error("Failed to start ECC part of '" + pluginName + "' plugin", ex);
                    result = false;
                }
            }

        }

        return result;
    }

    public HashMap<String, String> getPluginNamesAndPorts() {
        return pluginNamesAndPorts;
    }

    public String getPortForPluginName(String pluginName) {
        if (pluginNamesAndPorts.containsKey(pluginName)) {
            return pluginNamesAndPorts.get(pluginName);
        } else {
            return null;
        }
    }

    public void stop() {
        logger.debug("Shutting down ECC client threads");
        try {
            int returnValue;
            for (Process p : createdProcesses) {
                p.destroy();
                logger.debug("Process shutdown signal sent, waiting for it to finish");
                returnValue = p.waitFor();
                logger.debug("Process return value: " + returnValue);
            }
        } catch (InterruptedException e) {
            logger.error("Failed to stop ECC client thread", e);
        }
    }

}
