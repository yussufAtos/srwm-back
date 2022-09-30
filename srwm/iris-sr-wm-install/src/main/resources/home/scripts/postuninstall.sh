#!/usr/bin/env bash

# Export global vars
#-------------------------------------------
export RPM_USER="${rpm.user}"
export RPM_GROUP="${rpm.group}"

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

# Remove the user and the home directory
#-------------------------------------------
cecho "Removing unix user $RPM_USER" $GREEN
userdel -rf $RPM_USER

cecho "Application data has not been removed : /var/data/$RPM_GROUP/$RPM_USER" $GREEN
