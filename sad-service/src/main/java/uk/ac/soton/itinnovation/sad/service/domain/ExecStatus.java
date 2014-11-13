/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2014
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
//	Created Date :			2014-01-30
//	Created for Project :           Sense4us
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.domain;

/**
 * Status types for SAD Jobs and Executions.
 */
public final class ExecStatus {

    public static final String SUBMITTED = "submitted";
    public static final String SCHEDULED = "scheduled";
    public static final String RUNNING = "running";
    public static final String PAUSED = "paused";
    public static final String FINISHED = "finished";
    public static final String CANCELLED = "cancelled";
    public static final String FAILED = "failed";

}
