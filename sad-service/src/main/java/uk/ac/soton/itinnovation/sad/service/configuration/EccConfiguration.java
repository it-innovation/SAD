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
//	Created Date :			2014-06-27
//	Created for Project :           Sense4us
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.sad.service.configuration;

/**
 *
 */
public class EccConfiguration {

    private String monitorId;
    private String rabbitIp;
    private String rabbitPort;
    private String clientName;
    private String clientsUuuidSeed;

    public EccConfiguration() {
    }

    public EccConfiguration(String monitorId, String rabbitIp, String rabbitPort, String clientName, String clientsUuuidSeed) {
        this.monitorId = monitorId;
        this.rabbitIp = rabbitIp;
        this.rabbitPort = rabbitPort;
        this.clientName = clientName;
        this.clientsUuuidSeed = clientsUuuidSeed;
    }

    public String getMonitorId() {
        return monitorId;
    }

    public void setMonitorId(String monitorId) {
        this.monitorId = monitorId;
    }

    public String getRabbitIp() {
        return rabbitIp;
    }

    public void setRabbitIp(String rabbitIp) {
        this.rabbitIp = rabbitIp;
    }

    public String getRabbitPort() {
        return rabbitPort;
    }

    public void setRabbitPort(String rabbitPort) {
        this.rabbitPort = rabbitPort;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientsUuuidSeed() {
        return clientsUuuidSeed;
    }

    public void setClientsUuuidSeed(String clientsUuuidSeed) {
        this.clientsUuuidSeed = clientsUuuidSeed;
    }

}
