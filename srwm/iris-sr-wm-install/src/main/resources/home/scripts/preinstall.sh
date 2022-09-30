#!/usr/bin/env bash

# Export global vars
#-------------------------------------------
RPM_USER="${rpm.user}"
RPM_GROUP="${rpm.group}"
RPM_UID="${rpm.uid}"
RPM_DESCRIPTION="${rpm.description}"

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

cecho "Installing $1" $WHITE
cecho "---------------------------------------------------" $WHITE

# Check if it is an update
#-------------------------
if [ "$1" -gt 1 ]; then
    cecho "Preparing update:" $GREEN
    # Stop the wm server
    #-------------------------------------------
    cecho "Stoping WM" $GREEN
    systemctl stop iris-wm


    # Remove the shortcut in systemd services
    #-------------------------------------------
    if [ -f /etc/systemd/system/iris-wm.service ]; then
        systemctl disable iris-wm
        rm /etc/systemd/system/iris-wm.service
        systemctl daemon-reload
    fi
fi

# Check the env file
#-------------------------------------------
ENV_FILE="/usr/local/etc/iris/afp-env"

# Find the environment type
if [ -f $ENV_FILE ] ; then
   ENV=$(cat $ENV_FILE)
   cecho "Environment detected : $ENV" $GREEN
else
   cecho "/!\ Missing environment file : $ENV_FILE" $RED
   exit -1
fi

# Check the IRIS group
#-------------------------------------------
if ! getent group $RPM_GROUP >/dev/null; then
	groupadd -g 500 $RPM_GROUP
	cecho "Creating $RPM_GROUP unix group" $GREEN
else
	cecho "Unix group $RPM_GROUP already exists" $CYAN
fi

# Create the IRIS administrator user
#-------------------------------------------
if ! getent passwd irisadm >/dev/null; then

	cecho "Creating irisadm unix user" $GREEN

	useradd -g iris -u 510 -d /home/irisadm \
		-s /bin/bash -c "IRIS Admin" irisadm

	echo "iris4dm!" | passwd irisadm --stdin

	cecho "Adding irisadm in the sudoers" $GREEN

	echo "# Start of Iris setup -- DOT NOT REMOVE" >> /etc/sudoers
  echo "irisadm ALL=(%iris)   NOPASSWD:ALL"  >> /etc/sudoers
  echo "# End of Iris setup -- DOT NOT REMOVE" >> /etc/sudoers

	echo "export PATH=$PATH:/sbin" >> /home/irisadm/.bashrc

else
	cecho "User irisadm already exists" $CYAN
fi

# Create the IRIS administrator user's bin directory
if [ -a /home/irisadm/bin ]; then
	cecho "bin directory already exists : /home/irisadm/bin " $MAGENTA
else
	cecho "Creating bin directory : /home/irisadm/bin " $GREEN
	mkdir -p /home/irisadm/bin
fi

# Create the IRIS application user
#-------------------------------------------
if ! getent passwd $RPM_USER >/dev/null; then

	cecho "Creating $RPM_USER unix user" $GREEN

	useradd -g $RPM_GROUP -u $RPM_UID -d /home/$RPM_USER \
		-s /bin/bash -c "$RPM_DESCRIPTION" $RPM_USER

else
	cecho "Unix user $RPM_USER already exists" $CYAN

fi