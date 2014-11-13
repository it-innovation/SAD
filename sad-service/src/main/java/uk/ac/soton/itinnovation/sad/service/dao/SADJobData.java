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
//	Created Date :			2013-07-08
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.sad.service.dao;

import com.google.code.morphia.annotations.Entity;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import java.sql.Timestamp;
import org.bson.types.ObjectId;

/**
 * Database access object to store SAD Jobs data.
 */
@Entity("_jobData")
public class SADJobData extends AbstractEntity {

    private ObjectId SADJobID;
    private ObjectId SADExecutionID;
    private ObjectId SADExecutionDatabaseID;
    private String dataType;
    private String pluginName;
    private DBObject jsonData;
    private Timestamp whenCollected;

    public SADJobData() {
    }

    public SADJobData(ObjectId SADJobID, ObjectId SADExecutionID, ObjectId SADExecutionDatabaseID, String dataType, String pluginName, String jsonData, Timestamp whenCollected) {
        this.SADJobID = SADJobID;
        this.SADExecutionID = SADExecutionID;
        this.SADExecutionDatabaseID = SADExecutionDatabaseID;
        this.dataType = dataType;
        this.pluginName = pluginName;
        this.jsonData = (DBObject) JSON.parse(jsonData);
        this.whenCollected = whenCollected;
    }

    public SADJobData(ObjectId SADJobID, ObjectId SADExecutionID, ObjectId SADExecutionDatabaseID, String dataType, String pluginName, DBObject jsonData, Timestamp whenCollected) {
        this.SADJobID = SADJobID;
        this.SADExecutionID = SADExecutionID;
        this.SADExecutionDatabaseID = SADExecutionDatabaseID;
        this.dataType = dataType;
        this.pluginName = pluginName;
        this.jsonData = jsonData;
        this.whenCollected = whenCollected;
    }

    public ObjectId getSADJobID() {
        return SADJobID;
    }

    public void setSADJobID(ObjectId SADJobID) {
        this.SADJobID = SADJobID;
    }

    public ObjectId getSADExecutionID() {
        return SADExecutionID;
    }

    public void setSADExecutionID(ObjectId SADExecutionID) {
        this.SADExecutionID = SADExecutionID;
    }

    public ObjectId getSADExecutionDatabaseID() {
        return SADExecutionDatabaseID;
    }

    public void setSADExecutionDatabaseID(ObjectId SADExecutionDatabaseID) {
        this.SADExecutionDatabaseID = SADExecutionDatabaseID;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public DBObject getJsonData() {
        return jsonData;
    }

    public void setJsonData(DBObject jsonData) {
        this.jsonData = jsonData;
    }

    public Timestamp getWhenCollected() {
        return whenCollected;
    }

    public void setWhenCollected(Timestamp whenCollected) {
        this.whenCollected = whenCollected;
    }
}
