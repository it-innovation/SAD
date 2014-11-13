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
//	Created By :			Sleiman Jneidi
//	Created Date :			2013-07-03
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.sad.service.dao;

import com.google.code.morphia.annotations.Entity;
import java.sql.Timestamp;
import org.bson.types.ObjectId;

/**
 * Database access object to store SAD Jobs metadata.
 */
@Entity("_jobs")
public class SADJob extends AbstractEntity {

    private ObjectId SADWorkflowId;
    private String pluginName;
    private String pluginConfigurationAsJsonString;
    private String name;
    private String description;
    private String arguments;
    private String inputs;
    private String outputs;
    private String schedule;
    private String status;
    private Timestamp whenCreated;
    private Timestamp whenLastrun;

    public SADJob() {

    }

    public SADJob(ObjectId SADWorkflowId, String pluginName, String pluginConfigurationAsJsonString, String name, String description, String arguments, String inputs, String outputs, String schedule, String status, Timestamp whenCreated, Timestamp whenLastrun) {
        this.SADWorkflowId = SADWorkflowId;
        this.pluginName = pluginName;
        this.pluginConfigurationAsJsonString = pluginConfigurationAsJsonString;
        this.name = name;
        this.description = description;
        this.arguments = arguments;
        this.inputs = inputs;
        this.outputs = outputs;
        this.schedule = schedule;
        this.status = status;
        this.whenCreated = whenCreated;
        this.whenLastrun = whenLastrun;
    }

    public ObjectId getSADWorkflowId() {
        return SADWorkflowId;
    }

    public void setSADWorkflowId(ObjectId SADWorkflowId) {
        this.SADWorkflowId = SADWorkflowId;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public String getPluginConfigurationAsJsonString() {
        return pluginConfigurationAsJsonString;
    }

    public void setPluginConfigurationAsJsonString(String pluginConfigurationAsJsonString) {
        this.pluginConfigurationAsJsonString = pluginConfigurationAsJsonString;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public String getInputs() {
        return inputs;
    }

    public void setInputs(String inputs) {
        this.inputs = inputs;
    }

    public String getOutputs() {
        return outputs;
    }

    public void setOutputs(String outputs) {
        this.outputs = outputs;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getWhenCreated() {
        return whenCreated;
    }

    public void setWhenCreated(Timestamp whenCreated) {
        this.whenCreated = whenCreated;
    }

    public Timestamp getWhenLastrun() {
        return whenLastrun;
    }

    public void setWhenLastrun(Timestamp whenLastrun) {
        this.whenLastrun = whenLastrun;
    }

}
