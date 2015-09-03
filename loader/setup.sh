#!/bin/bash

sudo apt-get update
sudo apt-get install --yes python-pip python3-pip git libzmq-dev ansible
sudo pip3 install -r requirements.txt

cd ~
git clone git://github.com/ansible/ansible.git --recursive
cd ansible
sudo python setup.py install
