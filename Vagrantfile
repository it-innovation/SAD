# -*- mode: ruby -*-
# vi: set ft=ruby :

# Deploy SAD into a VM with IP 10.0.0.11
# SAD service will be available at http://localhost:8081/SAD on host machine.
# Tomcat manager will be on http://localhost:8081/manager/html with username manager, password manager
# Tail the log file with: vagrant ssh -c "tail -f /var/log/tomcat7/catalina.out"

## Configuration for this script (this part is Ruby) ##

hostname = "SAD-26"
ram = "512"
# Deploying with a static IP like this means that VirtualBox will set up a virtual network card that listens on 10.0.0.0/48
# If you have other machines on that subnet then change the IP or you will have to delete the virtual card from the
# VirtualBox GUI afterwards.  The purpose of using a static IP is so that it can talk to the Rabbit/ECC deployed in the same way.
ip = "10.0.0.11"

rabbit_ip = "10.0.0.10"
#rabbit_ip = "rabbitmq.experimedia.eu"
uuid = "00000000-0000-0000-0000-000000000000"

# if deploying in Jetty:
#plugin_path = "..\\/sad-plugins"
#coordinator_path = "src\\/main\\/resources\\/coordinator.json"

# if deploying in Tomcat:
plugin_path = "\\/home\\/vagrant\\/experimedia-sad\\/sad-plugins"
coordinator_path = "webapps\\/SAD\\/WEB-INF\\/classes\\/coordinator.json"

## The following shell script is run once the VM is built (this part is bash) ##

$script = <<SCRIPT
apt-get update

## Install dependencies ##

# sort out switching to Java 7 before installing Tomcat
apt-get install -y openjdk-7-jdk
update-alternatives --set java /usr/lib/jvm/java-7-openjdk-i386/jre/bin/java
rm /usr/lib/jvm/default-java
ln -s /usr/lib/jvm/java-1.7.0-openjdk-i386 /usr/lib/jvm/default-java
apt-get install -y maven
apt-get install -y mongodb
apt-get install -y tomcat7
apt-get install -y tomcat7-admin

## Get the SAD code ##

mkdir experimedia-sad
rsync -a /vagrant/ experimedia-sad --exclude '.git' --exclude 'target' --exclude '.vagrant'
cd experimedia-sad
rm sad-service/src/main/resources/application.properties
mv sad-service/src/main/resources/application-vagrant.properties sad-service/src/main/resources/application.properties

## Set up Tomcat ##

# enable the tomcat manager webapp with user manager
echo "<?xml version='1.0' encoding='utf-8'?><tomcat-users><user rolename='manager-gui'/><user username='manager' password='cZGmMqLkFE' roles='manager-gui'/></tomcat-users>" > /etc/tomcat7/tomcat-users.xml
# set port to 8081
sed -i -e 's/8080/8081/g' /var/lib/tomcat7/conf/server.xml
# add more memory
sed -i '239i JAVA_OPTS="$JAVA_OPTS -Xmx256M -XX:MaxPermSize=256M"' /usr/share/tomcat7/bin/catalina.sh
service tomcat7 restart

# add in sample plugin visualisations
cp -a sad-plugins/basic-sns-stats/src/main/resources/visualise sad-service/src/main/webapp/visualise/basic-sns-stats
cp -a sad-plugins/facebook-collector/src/main/resources/visualise sad-service/src/main/webapp/visualise/facebook-collector
cp -a sad-plugins/twitter-searcher/src/main/resources/visualise sad-service/src/main/webapp/visualise/twitter-searcher

## Build ##

echo "**** Building SAD"
cd lib
chmod 777 install_into_maven.sh
./install_into_maven.sh
cd ..
mvn install |& tee /tmp/build.log

# Copy sad-plugins folder so that tomcat7 can run them
sudo cp -R /home/vagrant/experimedia-sad/sad-plugins /var/lib/tomcat7/sad-plugins
chown -R tomcat7:tomcat7 /var/lib/tomcat7/sad-plugins
chmod -R 777 /var/lib/tomcat7/sad-plugins

## Deployment ##

# deploy the SAD into Tomcat
echo "**** Deploying SAD into Tomcat"
cp sad-service/target/sad-service*.war /var/lib/tomcat7/webapps/SAD.war
echo "**** Finished: SAD deployed in Tomcat port running on port 8081.  Mapped to localhost:8081/SAD on host machine."
echo "SAD service: http://localhost:8081/SAD/"
echo "Username: manager"
echo "Password: cZGmMqLkFE"

# or run the SAD using Jetty launched from maven
#cd sad-service
#mvn jetty:run-war

SCRIPT

## Configuration of the VM (Ruby again) ##

Vagrant.configure("2") do |config|

    # build off ubuntu 12.04 LTS (32 bit)
    config.vm.box = "precise32"
    config.vm.box_url = "http://files.vagrantup.com/precise32.box"
    config.vm.hostname = hostname

	# Forward host port 8081 to guest port 8080 for Tomcat - has to be 8081!
	config.vm.network :forwarded_port, host: 8081, guest: 8081

  # Forward host port 8001 to guest port 8000 for Tomcat debugging
  config.vm.network :forwarded_port, host: 8001, guest: 8000


	# Set static private network address
	config.vm.network "private_network", ip: ip

    # configure virtualbox
    config.vm.provider :virtualbox do |vb|
        vb.customize [
            'modifyvm', :id,
            '--name', hostname,
            '--memory', ram
        ]
	end

	# Provision using shell script embedded above
	config.vm.provision :shell, :inline => $script

end
