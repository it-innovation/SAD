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
//	Created Date :			2013-08-19
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.sad.service.helpers;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import uk.ac.soton.itinnovation.sad.service.dao.SADJobData;


public class DataHelper {

    public DataHelper() {
    }

    /**
     * Utility method that converts Mongo's DBObject to SADJobData
     */
    public SADJobData dBObjectToSADJobData(DBObject object) {
        Map map = object.toMap();
        SADJobData jobData = new SADJobData();

        // reading data
        ObjectId id = (ObjectId) object.get("_id");
        ObjectId sadJobId = (ObjectId) map.get("SADJobID");

        Timestamp whenCollected = new Timestamp(((Date) map.get("whenCollected")).getTime());

        ObjectId ExecutionId = (ObjectId) map.get("SADExecutionDatabaseID");
        String pluginName = (String) map.get("pluginName");
        String dataType = (String) map.get("dataType");
        DBObject jsonData = (DBObject) map.get("jsonData");

        //filling object
        jobData.setId(id);
        jobData.setSADJobID(sadJobId);
        jobData.setSADExecutionDatabaseID(ExecutionId);
        jobData.setWhenCollected(whenCollected);
        jobData.setPluginName(pluginName);
        jobData.setDataType(dataType);
        jobData.setJsonData(jsonData);

        return jobData;
    }

    public DBObject jsonToDBObject(
            JSONObject jsonData,
            ObjectId sadJobId,
            String sadExecutionId,
            ObjectId sadExecutionDatabaseId,
            String dataType,
            String pluginName,
            Date whenCollected) {

        DBObject document = new BasicDBObject();

        document.put("jsonData", JSON.parse(jsonData.toString()));
        document.put("SADJobID", sadJobId);
        document.put("SADExecutionID", sadExecutionId);
        document.put("SADExecutionDatabaseID", sadExecutionDatabaseId);
        document.put("dataType", dataType);
        document.put("pluginName", pluginName);
        document.put("whenCollected", whenCollected);

        return document;
    }
}