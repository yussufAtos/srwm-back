#!/usr/bin/env bash

# Export global vars
#-------------------------------------------
export RPM_USER="${rpm.user}"

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

cecho "Removing $1" $WHITE
cecho "---------------------------------------------------" $WHITE

# Remove the shortcut in the irisadm
#-------------------------------------------
if [ -f /home/irisadm/bin/wmctl.sh ]; then
  rm /home/irisadm/bin/wmctl.sh
fi

