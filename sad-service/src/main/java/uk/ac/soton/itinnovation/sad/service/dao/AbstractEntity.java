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

import com.google.code.morphia.annotations.Id;
import org.bson.types.ObjectId;

public class AbstractEntity {

    @Id
    protected ObjectId Id;

    public void setId(ObjectId Id) {
        this.Id = Id;
    }

    public ObjectId getId() {
        return this.Id;
    }

    public String getIdAsString() {
        return this.Id == null ? null : this.Id.toString();
    }
}