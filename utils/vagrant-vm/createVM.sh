#!/bin/bash

# Change to the script directory
cd `dirname \$0`

# Create temporary folder
mkdir -p tmp

# Move scripts from utils to temporary folder
cp -f ../scripts/setEngine.sh tmp
cp -f ../scripts/updateEngine.sh tmp
cp -f ../scripts/runEngine.sh tmp

# Run Vagrant
vagrant up

# Delete temporary folder
rm -dr tmp
