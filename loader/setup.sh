#!/bin/bash

sudo apt-get update
sudo apt-get install --yes python3-pip git libzmq-dev
sudo pip3 install -r requirements.txt
