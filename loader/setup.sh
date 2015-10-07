#!/bin/bash

sudo apt-get update
sudo apt-get install --yes python-pip python3-pip git libzmq-dev
sudo pip3 install -r requirements.txt

# URL=https://releases.ansible.com/ansible/ansible-latest.tar.gz
# At least ansible 2.0.0 is required
URL=https://releases.ansible.com/ansible/ansible-2.0.0-0.3.beta1.tar.gz

a=`mktemp -d`
cd "$a"
wget -O - "$URL" | gzip -dc | tar x
cd ansible-*
sudo python setup.py install
cd
sudo rm -r "$a"
