#!/bin/bash

if [ ! -d ansibleEnvironment ]; then 
	
	sudo apt-get -y install python-dev
	
	sudo apt-get -y install libffi-dev
	sudo apt-get -y install libssl-dev
	
	sudo pip install virtualenv
	
	virtualenv -p /usr/bin/python2.7 ansibleEnvironment
	
fi

cd ansibleEnvironment


if [ ! -f bin/ansible ]; then
	source bin/activate 
	
	pip install ansible
	
	deactivate
fi

bin/ansible --version