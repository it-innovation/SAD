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
package uk.ac.soton.itinnovation.sad.service.controllers;

import uk.ac.soton.itinnovation.sad.service.domain.JsonResponse;
import uk.ac.soton.itinnovation.sad.service.services.PluginsService;
import uk.ac.soton.itinnovation.sad.service.services.SchedulingService;
import uk.ac.soton.itinnovation.sad.service.utils.Util;
import java.text.SimpleDateFormat;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller implementing REST interface for SAD Plugins.
 */
@Controller
@RequestMapping("/plugins")
public class PluginsController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    @Qualifier("pluginsService")
    PluginsService pluginsService;

    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
    SimpleDateFormat dateFormatCustomRss = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
    SimpleDateFormat dateFormatFacebook = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+SSSS");

    @Autowired
    @Qualifier("schedulingService")
    SchedulingService schedulingService;

    /**
     * Returns all plugins on the service, url mapping: /plugins
     *
     * @return all plugins as JSON
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public JsonResponse getPlugins() {

        logger.debug("Returning all plugins");

        schedulingService.pushMethodCalledName("/plugins");

        long startTime = System.currentTimeMillis();

        try {

            JSONObject response = pluginsService.getLivePluginsInfo();

//            logger.debug(response.toString());
            return new JsonResponse("ok", response);

        } catch (Throwable ex) {
            return new JsonResponse("error", Util.dealWithException("Failed to return all plugins", ex, logger));
        } finally {
            schedulingService.pushTimeSpent(Long.toString(System.currentTimeMillis() - startTime));
        }
    }

    /**
     * Returns plugin configuration by plugin name, url mapping:
     * /plugins/{pluginName}
     *
     * @param pluginName
     * @return plugin configuration as JSON
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{pluginName}")
    @ResponseBody
    public JsonResponse getPluginByName(@PathVariable String pluginName) {

        logger.debug("Returning plugin by name: " + pluginName);

        schedulingService.pushMethodCalledName("/executions/" + pluginName);

        long startTime = System.currentTimeMillis();

        try {
            JSONObject response = new JSONObject();
            JSONObject pluginWithName = pluginsService.getPluginByName(pluginName);

            if (pluginWithName.isEmpty()) {
                logger.error("Plugin with name '" + pluginName + "' was not found.");
                response.put("message", "Plugin with name '" + pluginName + "' was not found.");
                return new JsonResponse("error", response);
            } else {
                return new JsonResponse("ok", pluginWithName);
            }

        } catch (Throwable ex) {
            return new JsonResponse("error", Util.dealWithException("Failed to return plugin by name", ex, logger));
        } finally {
            schedulingService.pushTimeSpent(Long.toString(System.currentTimeMillis() - startTime));
        }
    }
}
