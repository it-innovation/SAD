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
//	Created Date :			2013-08-22
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.plugins.facebookcollector;

import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.Attribute;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.Entity;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MeasurementSet;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MetricGenerator;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MetricGroup;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MetricHelper;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.MetricType;
import uk.ac.soton.itinnovation.experimedia.arch.ecc.common.dataModel.metrics.Unit;
import uk.ac.soton.itinnovation.sad.service.adapters.GenericEccClient;

public class PluginEccClient extends GenericEccClient {

    public static final String METRIC_GENERATOR_MAIN = "Facebook plugin metric generator";
    public static final String ENTITY_Facebook = "Facebook";
    public static final String ENTITY_SocialNetworksDataGroup = "Social Networks Data Group";
    public static final String ENTITY_PLUGINS = "SAD Plugins";
    public static final String ATTRIBUTE_MESSAGES_FOUND = "Facebook posts found";
    public static final String ATTRIBUTE_USERS_FOUND = "Facebook users found";
    public static final String ATTRIBUTE_PAGES_SEARCHED = "Facebook channels";
    public static final String ATTRIBUTE_REQUESTS_COUNT = "Request count";
    public static final String ATTRIBUTE_REQUEST_LATENCY = "Request latency";
    public static final String ATTRIBUTE_EXECUTION_DURATION = "Successful execution duration (facebook collector)";

    public PluginEccClient(String name, String[] args) {
        super(name, args);
    }

    @Override
    public void defineExperimentMetrics() {

        mainMetricGenerator = new MetricGenerator();
        currentExperiment.addMetricGenerator(mainMetricGenerator);
        mainMetricGenerator.setName(METRIC_GENERATOR_MAIN);
        mainMetricGenerator.setDescription("Main metric generator for " + name);

        MetricGroup metricGroup = MetricHelper.createMetricGroup(name + "'s metric group", "Metric group for plugin " + name, mainMetricGenerator);

        Entity snDataGroupEntity = new Entity();
        mainMetricGenerator.addEntity(snDataGroupEntity);
        snDataGroupEntity.setName(ENTITY_SocialNetworksDataGroup);
        snDataGroupEntity.setDescription("Entity for Social Networks data");

        Attribute a_tweets_found = MetricHelper.createAttribute(ATTRIBUTE_MESSAGES_FOUND, "How many messages were found in the last collection", snDataGroupEntity);
        MeasurementSet ms_posts_found = MetricHelper.createMeasurementSet(a_tweets_found, MetricType.RATIO, new Unit("Posts"), metricGroup);

        Attribute a_users_found = MetricHelper.createAttribute(ATTRIBUTE_USERS_FOUND, "How many users were found in the last search", snDataGroupEntity);
        MeasurementSet ms_users_found = MetricHelper.createMeasurementSet(a_users_found, MetricType.RATIO, new Unit("Users"), metricGroup);

        Attribute a_keywords = MetricHelper.createAttribute(ATTRIBUTE_PAGES_SEARCHED, "Pages collected from", snDataGroupEntity);
        MeasurementSet ms_keywords = MetricHelper.createMeasurementSet(a_keywords, MetricType.NOMINAL, new Unit(""), metricGroup);

        measurementSetMap.put(ms_posts_found.getID(), ms_posts_found);
        measurementSetMap.put(ms_users_found.getID(), ms_users_found);
        measurementSetMap.put(ms_keywords.getID(), ms_keywords);

        Entity facebookEntity = new Entity();
        mainMetricGenerator.addEntity(facebookEntity);
        facebookEntity.setName(ENTITY_Facebook);
        facebookEntity.setDescription("Entity for Facebook");

        Attribute a_requests_count = MetricHelper.createAttribute(ATTRIBUTE_REQUESTS_COUNT, "Number of requests given to the Social Integrator", facebookEntity);
        MeasurementSet ms_requests = MetricHelper.createMeasurementSet(a_requests_count, MetricType.RATIO, new Unit("Times"), metricGroup);

        Attribute a_request_latency = MetricHelper.createAttribute(ATTRIBUTE_REQUEST_LATENCY, "Response time of Social Integrator and Facebook", facebookEntity);
        MeasurementSet ms_latency = MetricHelper.createMeasurementSet(a_request_latency, MetricType.RATIO, new Unit("mSec"), metricGroup);

        measurementSetMap.put(ms_requests.getID(), ms_requests);
        measurementSetMap.put(ms_latency.getID(), ms_latency);

        Entity sadPluginsEntity = new Entity();
        mainMetricGenerator.addEntity(sadPluginsEntity);
        sadPluginsEntity.setName(ENTITY_PLUGINS);
        sadPluginsEntity.setDescription("Entity for SAD plugins");

        Attribute a_successful_execution_duration = MetricHelper.createAttribute(ATTRIBUTE_EXECUTION_DURATION, "How long it took to execute the plugin", sadPluginsEntity);
        MeasurementSet ms_exec_duration = MetricHelper.createMeasurementSet(a_successful_execution_duration, MetricType.RATIO, new Unit("mSec"), metricGroup);

        measurementSetMap.put(ms_exec_duration.getID(), ms_exec_duration);

    }

}
