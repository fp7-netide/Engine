#!/bin/bash

sudo locale-gen en_US.UTF-8
export LC_ALL=C

sudo apt-get -y update
sudo apt-get -y upgrade
    
if [ ! -d loaderEnvironment ]; then 
    sudo apt-get -y remove tmux

	if !(hash git 2>/dev/null); then
		sudo apt-get -y install git
	fi

	if !(hash pip 2>/dev/null); then
		sudo apt-get -y install python-pip
	fi

	if !(hash virtualenv 2>/dev/null); then
		sudo pip install virtualenv
	fi

    if !(hash python3-dev 2>/dev/null); then
        sudo apt-get -y install python3-dev
	fi

    if !(hash automake 2>/dev/null); then
        sudo apt-get -y install automake
    fi

    if !(hash pkg-config 2>/dev/null); then
        sudo apt-get -y install pkg-config
    fi

    sudo add-apt-repository ppa:ubuntu-desktop/ubuntu-make
    sudo apt-get update
    sudo apt-get install -y ubuntu-make

    sudo apt-get install -y exuberant-ctags cmake libevent-dev libncurses5-dev


    sudo apt-get install -y automake
    sudo apt-get install -y build-essential

    git clone https://github.com/tmux/tmux.git
    cd tmux
    sh autogen.sh
    ./configure && make
    sudo make install
    cd ..

    echo -e "# ~/.tmux.conf \n set -g mouse on" > ~/.tmux.conf


	virtualenv -p /usr/bin/python3 loaderEnvironment
fi

source loaderEnvironment/bin/activate


pip install -r requirements.txt

	
deactivate
