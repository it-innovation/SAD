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
import java.sql.Timestamp;
import org.bson.types.ObjectId;

/**
 * Database access object to store SAD Job Executions metadata.
 */
@Entity("_executions")
public class SADJobExecution extends AbstractEntity {

    private int countID;
    private ObjectId SADJobID;
    private String description;
    private String status;
    private Timestamp whenStarted;
    private Timestamp whenFinished;

    public SADJobExecution() {
    }

    public SADJobExecution(int countID, ObjectId SADJobID, String description, String status, Timestamp whenStarted, Timestamp whenFinished) {
        this.countID = countID;
        this.SADJobID = SADJobID;
        this.description = description;
        this.status = status;
        this.whenStarted = whenStarted;
        this.whenFinished = whenFinished;
    }

    public int getCountID() {
        return countID;
    }

    public void setCountID(int CountID) {
        this.countID = CountID;
    }

    public ObjectId getSADJobID() {
        return SADJobID;
    }

    public void setSADJobID(ObjectId SADJobID) {
        this.SADJobID = SADJobID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getWhenStarted() {
        return whenStarted;
    }

    public void setWhenStarted(Timestamp whenStarted) {
        this.whenStarted = whenStarted;
    }

    public Timestamp getWhenFinished() {
        return whenFinished;
    }

    public void setWhenFinished(Timestamp whenFinished) {
        this.whenFinished = whenFinished;
    }
}
