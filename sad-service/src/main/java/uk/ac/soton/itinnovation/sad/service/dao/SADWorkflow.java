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

@Entity("_workflows")
public class SADWorkflow extends AbstractEntity {

    private String name;
    private String status;
    private String pluginsAsJsonString;
    private String scheduleAsJsonString;
    private Timestamp created;
    private Timestamp lastrun;

    public SADWorkflow() {
    }

    public SADWorkflow(String name, String status, String pluginsAsJsonString, String scheduleAsJsonString, Timestamp created, Timestamp lastrun) {
        this.name = name;
        this.status = status;
        this.pluginsAsJsonString = pluginsAsJsonString;
        this.scheduleAsJsonString = scheduleAsJsonString;
        this.created = created;
        this.lastrun = lastrun;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPluginsAsJsonString() {
        return pluginsAsJsonString;
    }

    public void setPluginsAsJsonString(String pluginsAsJsonString) {
        this.pluginsAsJsonString = pluginsAsJsonString;
    }

    public String getScheduleAsJsonString() {
        return scheduleAsJsonString;
    }

    public void setScheduleAsJsonString(String scheduleAsJsonString) {
        this.scheduleAsJsonString = scheduleAsJsonString;
    }

    public Timestamp getCreated() {
        return created;
    }

    public void setCreated(Timestamp created) {
        this.created = created;
    }

    public Timestamp getLastrun() {
        return lastrun;
    }

    public void setLastrun(Timestamp lastrun) {
        this.lastrun = lastrun;
    }
}
