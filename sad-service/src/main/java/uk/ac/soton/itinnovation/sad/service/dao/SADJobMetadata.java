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
import org.bson.types.ObjectId;

/**
 * Allows SAD job executions to share data.
 */
@Entity("_jobMetadata")
public class SADJobMetadata extends AbstractEntity {

    private ObjectId SADJobID;
    private String key;
    private String value;

    public SADJobMetadata() {
    }

    public SADJobMetadata(ObjectId SADJobID, String key, String value) {
        this.SADJobID = SADJobID;
        this.key = key;
        this.value = value;
    }

    public ObjectId getSADJobID() {
        return SADJobID;
    }

    public void setSADJobID(ObjectId SADJobID) {
        this.SADJobID = SADJobID;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
