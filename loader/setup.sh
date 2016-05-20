#!/bin/bash

if [ ! -d loaderEnvironment ]; then 

	if !(hash pip 2>/dev/null); then
		sudo apt-get -y install python-pip
	fi

	if !(hash virtualenv 2>/dev/null); then
		sudo pip install virtualenv
	fi
	
	virtualenv -p /usr/bin/python3 loaderEnvironment	
fi

source loaderEnvironment/bin/activate 

pip install -r requirements.txt
	
deactivate
