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
package uk.ac.soton.itinnovation.sad.plugins.basicstats;

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

    public static final String METRIC_GENERATOR_MAIN = "Basic SNS stats plugin metric generator";
    public static final String ENTITY_SocialNetworksDataGroup = "Social Networks Data Group";
    public static final String ENTITY_PLUGINS = "SAD Plugins";
    public static final String ATTRIBUTE_TWEETS_ANALYSED = "Tweets analysed";
    public static final String ATTRIBUTE_MESSAGES_ANALYSED = "Facebook posts analysed";
    public static final String ATTRIBUTE_MEDIA_LINKS = "Media files with links";
    public static final String ATTRIBUTE_EXECUTION_DURATION = "Successful execution duration (basic stats)";

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

        Attribute a_tweets_found = MetricHelper.createAttribute(ATTRIBUTE_TWEETS_ANALYSED, "How many tweets were analysed", snDataGroupEntity);
        MeasurementSet ms_tweets_found = MetricHelper.createMeasurementSet(a_tweets_found, MetricType.RATIO, new Unit("Tweets"), metricGroup);
        Attribute a_messages_found = MetricHelper.createAttribute(ATTRIBUTE_MESSAGES_ANALYSED, "How many Facebook messages were analysed", snDataGroupEntity);
        MeasurementSet ms_messages_found = MetricHelper.createMeasurementSet(a_messages_found, MetricType.RATIO, new Unit("Messages"), metricGroup);
        Attribute a_media_links_found = MetricHelper.createAttribute(ATTRIBUTE_MEDIA_LINKS, "Number of media files (images mostly) with links were found in a social network", snDataGroupEntity);
        MeasurementSet ms_media_links_found = MetricHelper.createMeasurementSet(a_media_links_found, MetricType.RATIO, new Unit("Files"), metricGroup);

        measurementSetMap.put(ms_tweets_found.getID(), ms_tweets_found);
        measurementSetMap.put(ms_messages_found.getID(), ms_messages_found);
        measurementSetMap.put(ms_media_links_found.getID(), ms_media_links_found);

        Entity sadPluginsEntity = new Entity();
        mainMetricGenerator.addEntity(sadPluginsEntity);
        sadPluginsEntity.setName(ENTITY_PLUGINS);
        sadPluginsEntity.setDescription("Entity for SAD plugins");

        Attribute a_successful_execution_duration = MetricHelper.createAttribute(ATTRIBUTE_EXECUTION_DURATION, "How long it took to execute the plugin", sadPluginsEntity);
        MeasurementSet ms_exec_duration = MetricHelper.createMeasurementSet(a_successful_execution_duration, MetricType.RATIO, new Unit("mSec"), metricGroup);

        measurementSetMap.put(ms_exec_duration.getID(), ms_exec_duration);

    }

}
