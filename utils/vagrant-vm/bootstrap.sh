#!/usr/bin/env bash
# NetIDE Engine installation script for Vagrant VM

NEWUSER=netide    # username
PASSWORD='netide' # password

START_TIME=$SECONDS

echo "--"
echo "-- Creating new user $NEWUSER:"
echo "--"
useradd -m -p "$pass" -s "/bin/bash" $NEWUSER
echo "$NEWUSER:$PASSWORD" | chpasswd
adduser $NEWUSER sudo
echo "$NEWUSER ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/$NEWUSER
cp /root/.bashrc /home/netide
echo "[SeatDefaults]" > /etc/lightdm/lightdm.conf
echo "autologin-user=netide" >> /etc/lightdm/lightdm.conf

echo "--"
echo "-- Setting wallpaper:"
echo "--"
WALLPAPER=/vagrant/wallpaper/netide-desktop.png
cp $WALLPAPER /usr/share/backgrounds
echo "gsettings set org.gnome.desktop.background picture-uri file:///usr/share/backgrounds/netide-desktop.png" >> /home/netide/.profile
echo "gsettings set org.gnome.desktop.session idle-delay 0" >> /home/netide/.profile
echo "gsettings set org.gnome.desktop.screensaver lock-enabled false" >> /home/netide/.profile

echo "--"
echo "-- Updating package list:"
echo "--"
sed -i 's/http:\/\/us./http:\/\//g' /etc/apt/sources.list
apt-get update

echo "--"
echo "-- Upgrading packages:"
echo "--"
sudo DEBIAN_FRONTEND='noninteractive' apt-get -y -o Dpkg::Options::='--force-confdef' -o Dpkg::Options::='--force-confold' upgrade

echo "--"
echo "-- Setting NetIDE Engine:"
echo "--"
cd /home/netide
wget -q https://raw.githubusercontent.com/fp7-netide/Engine/master/utils/scripts/setEngine.sh
chmod +x setEngine.sh
su netide -c "./setEngine.sh"
rm -f setEngine.sh

echo "--"
echo "-- Updating NetIDE Engine:"
echo "--"
cd NetIDE
su netide -c "./updateEngine.sh -a"

echo "--"
echo "-- Cleaning package caches:"
echo "--"
sudo apt-get -y autoremove
sudo apt-get clean

ELAPSED_TIME=$(expr $SECONDS - $START_TIME)
MINUTES=$(expr $ELAPSED_TIME / 60)
SECONDS=$(expr $ELAPSED_TIME - $(expr $MINUTES \* 60))
echo "--"
echo "-- NetIDE VM created in $MINUTES minutes and $SECONDS seconds."
echo "-- Rebooting to finish installation..."
echo "--"
reboot
