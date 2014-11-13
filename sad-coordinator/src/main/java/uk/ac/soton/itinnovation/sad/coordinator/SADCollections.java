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
//	Created Date :			2013-07-10
//	Created for Project :           Experimedia
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.coordinator;

public enum SADCollections {

    SADJobs {
                @Override
                public String getName() {
                    return "_jobs";
                }
            },
    SADJobData {
                @Override
                public String getName() {
                    return "_jobData";
                }
            },
    SADJobExecution {
                @Override
                public String getName() {
                    return "_executions";
                }
            },
    SADJobMetadata {
                @Override
                public String getName() {
                    return "_jobMetadata";
                }
            },
    SADJobStdError {
                @Override
                public String getName() {
                    return "SADJobStdError";
                }
            },
    SADJobStdOut {
                @Override
                public String getName() {
                    return "SADJobStdOut";
                }
            },
    SADWorkflow {
                @Override
                public String getName() {
                    return "SADWorkflow";
                }
            };

    public abstract String getName();
}
