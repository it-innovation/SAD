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
//	Created Date :			2013-10-18
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.sad.service.helpers;


public class ProvVideo {
    private String url, action, user_id, metadata, user_name;
    private long timestamp;

    public ProvVideo(String url, String action, String user_id, String user_name, String metadata, long timestamp) {
        this.url = url;
        this.action = action;
        this.user_id = user_id;
        this.metadata = metadata;
        this.user_name = user_name;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Action '" + action + "' (metadata='" + metadata + "') on '" + url + "' by [" + user_id + "] at " + timestamp;
    }

    public String getUrl() {
        return url;
    }

    public String getAction() {
        return action;
    }

    public String getUserId() {
        return user_id;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getUserName() {
        return user_name;
    }

    public long getTimestamp() {
        return timestamp;
    }
}