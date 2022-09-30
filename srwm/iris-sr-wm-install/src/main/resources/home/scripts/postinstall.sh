#!/usr/bin/env bash

# Export global vars
#-------------------------------------------
RPM_USER="${rpm.user}"
RPM_GROUP="${rpm.group}"

# Colorize the stdout
#-------------------------------------------
BLACK='\E[1;47m'
RED='\E[1;31m'
GREEN='\E[1;32m'
YELLOW='\E[1;33m'
BLUE='\E[1;34m'
MAGENTA='\E[1;35m'
CYAN='\E[1;36m'
WHITE='\E[1;37m'

function cecho  {
	echo -e $2
	echo ">> $1"
	tput sgr0
	return
}

# Set the environment
#-------------------------------------------
ENV_FILE="/usr/local/etc/iris/afp-env"
ENV=`cat $ENV_FILE`

AFP_DOMAINE=".afp.com"
HOSTNAME=`hostname`
if [ "${HOSTNAME#*$AFP_DOMAINE}" = "$HOSTNAME" ]; then
     HOSTNAME="${HOSTNAME}${AFP_DOMAINE}"
fi


# Move systemd unit file
mv /home/$RPM_USER/systemd/iris-wm.service /etc/systemd/system/
chmod 664 /etc/systemd/system/iris-wm.service
systemctl daemon-reload

# Making scripts executable
chmod 755 /home/$RPM_USER/bin/setenv.sh
chmod 755 /home/$RPM_USER/bin/wmctl.sh

# Moving the shortcut in the irisadm home and making it executable
mv -f /home/$RPM_USER/bin/irisadm-wmctl.sh /home/irisadm/bin/wmctl.sh
chmod +x /home/irisadm/bin/wmctl.sh

# Removing unnecessary properties files
cecho "Removing unnecessary properties files" $GREEN
find /home/$RPM_USER/config/ -type f ! -name "application.yml" -a ! -name "application-$ENV.yml" -a ! -name "logback-spring.xml" -delete

# Replace vars in config files
#-------------------------------------------

   #HOSTNAME
   
cecho "Replace Hostname " $BLUE
sed -i "s/##MYHOSTNAME##/$HOSTNAME/g" /home/$RPM_USER/config/application-$ENV.yml
cecho "Hostname is : $HOSTNAME" $GREEN

   #SERVER PORT
   
cecho "Replace server port" $BLUE
SERVER_PORT=$(cat /home/$RPM_USER/config/application-$ENV.yml | grep server.port | grep -v '{server.port}'| awk -F ':' '{print $2}' | tr -d ' ')
sed -i "s/##PORT##/$SERVER_PORT/g" /home/$RPM_USER/bin/setenv.sh
cecho "Server Port is : $SERVER_PORT" $GREEN 

   #TRUSTORE JAVA OPTS
   
cecho "Replace Trust Store Java Option" $BLUE
TRUSTORE_JAVA_OPTS=$(cat /home/$RPM_USER/config/application-$ENV.yml | grep trustore-java-opts | awk -F ':' '{print $2}' | tr -d ' ')
TRUSTORE_JAVA_OPTS=${TRUSTORE_JAVA_OPTS////\\/}
sed -i "s/##TRUSTORE_JAVA_OPTIONS##/$TRUSTORE_JAVA_OPTS/g" /home/$RPM_USER/bin/setenv.sh
cecho "Trust Store Java Option is : $TRUSTORE_JAVA_OPTS" $GREEN

    #CERTIFICAT FILE

cecho "Fetch Certificate File from profile: $ENV ,for Hostname:$HOSTNAME" $GREEN
CERTIFICAT_FILE=$(cat /home/$RPM_USER/config/application-$ENV.yml | grep certificate-file | grep -v '{certificate-file}' | awk -F ':' '{print $2}' | tr -d ' ')

# Find the environment type
if [ "x$CERTIFICAT_FILE" != "x" ]; then
  if [ -f "$CERTIFICAT_FILE" ] ; then
	cecho "Certificat file detected : $CERTIFICAT_FILE" $GREEN
  else 
	cecho "\n/!\ Missing certificat file : $CERTIFICAT_FILE" $RED
	exit -1
  fi
fi

# Create the data directory and link
#-------------------------------------------
if [ -a /var/data/$RPM_GROUP/$RPM_USER ]; then
	cecho "data directory already exists : /var/data/$RPM_GROUP/$RPM_USER" $MAGENTA
else
	cecho "Creating data directory : /var/data/$RPM_GROUP/$RPM_USER" $GREEN
	mkdir -p /var/data/$RPM_GROUP/$RPM_USER
fi
if [ -a /home/$RPM_USER/var/data ]; then
	cecho "data directory already exists : /home/$RPM_USER/var/data" $MAGENTA
else
	cecho "Creating data directory : /home/$RPM_USER/var/data" $GREEN
	mkdir -p /home/$RPM_USER/var/data
fi
# Linking data directory
chown -R $RPM_USER:$RPM_GROUP /var/data/$RPM_GROUP/$RPM_USER
cecho "Linking data directory : /home/$RPM_USER/var/data to /var/data/$RPM_GROUP/$RPM_USER" $GREEN
cecho "Linking command : ln -s /var/data/$RPM_GROUP/$RPM_USER /home/$RPM_USER/var/data" $YELLOW

ln -s /var/data/$RPM_GROUP/$RPM_USER /home/$RPM_USER/var/data

# Create the log directories and link
#-------------------------------------------
if [ -a /var/log/$RPM_GROUP/$RPM_USER ]; then
	cecho "Log directory already exists : /var/log/$RPM_GROUP/$RPM_USER" $MAGENTA
else
	cecho "Creating log directory : /var/log/$RPM_GROUP/$RPM_USER" $GREEN
	mkdir -p /var/log/$RPM_GROUP/$RPM_USER
fi
if [ -a /home/$RPM_USER/var/log ]; then
	cecho "Log directory already exists : /home/$RPM_USER/var/log" $MAGENTA
else
	cecho "Creating log directory : /home/$RPM_USER/var/log" $GREEN
	mkdir -p /home/$RPM_USER/var/log
fi
# Linking log directory
chown -R $RPM_USER:$RPM_GROUP /var/log/$RPM_GROUP/$RPM_USER
cecho "Linking log directory : /home/$RPM_USER/var/log to /var/log/$RPM_GROUP/$RPM_USER" $GREEN
cecho "ln -s /var/log/$RPM_GROUP/$RPM_USER /home/$RPM_USER/var/log" $YELLOW

ln -s /var/log/$RPM_GROUP/$RPM_USER /home/$RPM_USER/var/log

# Create the run directory
#-------------------------------------------
if [ -a /home/$RPM_USER/var/run ]; then
	cecho "Run directory already exists : /home/$RPM_USER/var/run" $MAGENTA
else
	cecho "Creating run directory : /home/$RPM_USER/var/run" $GREEN
	mkdir -p /home/$RPM_USER/var/run
	chown $RPM_USER:$RPM_GROUP /home/$RPM_USER/var/run
fi
