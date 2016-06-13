import fcntl
import logging
import os
import shutil
import tempfile
import tarfile
import yaml

import subprocess as sp
from functools import reduce


dataroot = "/tmp/netide"
extractPath = os.path.join(dataroot, "extractPath.txt")


def extractPackage(path):
    os.makedirs(dataroot, exist_ok=True)
    
    if os.path.exists(extractPath):
        if os.path.isfile(extractPath): 
            f = open(extractPath)
            tmpPath = f.read()
    else:
        tmpPath = dataroot     
    #expect path to tar archive as args and extract content
    with tarfile.open(path) as tar:
        tar.extractall(tmpPath)
        tmpPath = os.path.join(tmpPath, tar.getnames()[0])
        
    print("Extracted to:" + tmpPath)
    return tmpPath

def setExtractionPath(path):
    os.makedirs(dataroot, exist_ok=True)
    
    with open(extractPath, 'w') as f:
        f.write(args.path)
    return 0


class Chdir(object):
    "Context manager for switching to a directory, doing something and switching back"
    oldcwd = None
    def __init__(self, newpath):
        self.newpath = newpath

    def __enter__(self):
        self.oldcwd = os.getcwd()
        os.chdir(self.newpath)

    def __exit__(self, *rest):
        os.chdir(self.oldcwd)


class FLock(object):
    "Context manager for locking file objects with flock"
    def __init__(self, f, shared=False):
        self.f = f
        if shared:
            self.t = fcntl.LOCK_SH
        else:
            self.t = fcntl.LOCK_EX

    def __enter__(self):
        fcntl.flock(self.f, self.t)
        return self.f

    def __exit__(self, exc_type, exc_value, traceback):
        fcntl.flock(self.f, fcntl.LOCK_UN)

class TempDir(object):
    "Context manager for temporary directories"
    d = None

    def __init__(self, prefix="", cleanup=True):
        self.p = prefix
        self.c = cleanup

    def __enter__(self):
        self.d = tempfile.mkdtemp("-" + self.p)
        return self.d

    def __exit__(self, exc_type, exc_value, traceback):
        if self.d is None or not self.c:
            return

        shutil.rmtree(self.d, ignore_errors=True)

def build_ssh_commands(c):
    """Build ssh command lists from client tuple"""
    # Tuple layout:
    # (name, sshhost, sshport, sshidentity)

    ssh = ["ssh"]

    if len(c) >= 3:
        ssh.extend(["-p", str(c[2])])
    if len(c) >= 4:
        ssh.extend(["-i", str(c[3])])
    ssh.extend(["-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile={}".format(tempfile.mktemp())]) # XXX
    ssh.append(c[1])

    return ssh

def spawn_logged(cmd):
    print(cmd)
    p = sp.Popen(cmd, stdout=sp.PIPE, stderr=sp.STDOUT)

    for l in p.stdout:
        l = l.decode('utf-8').rstrip()
        logging.debug(l)
    return p.wait()


def write_ansible_hosts(clients, path):
    # Tuple layout:
    # (name, ssh host, ssh port, ssh identity)
    with open(path, "w") as ah:
        ah.write("localhost ansible_connection=local\n")

        ah.write("\n[clients]\n")
        for c in clients:
            try:
                user, host = c[1].split("@", 1)
                ah.write("{} ansible_ssh_host={} ansible_ssh_user={}".format(c[0], host, user))
            except ValueError:
                ah.write("{} ansible_ssh_host={}".format(c[0], c[1]))
            if len(c) >= 3:
                ah.write(" ansible_ssh_port={}".format(c[2]))
            if len(c) >= 4:
                ah.write(" ansible_ssh_private_key_file={}".format(c[3]))
            ah.write("\n")
            
def editPlaybookClient(package):
     
    open("Playbook_Setup/siteClient.yml", 'w').close()
        
    currentContent = []    
    
    nameSet = set(package.controllerNames)
    names = []
    for name in nameSet:
        names.append(name.lower())
        print(name.lower())
    
    currentContent.append({'name' : 'install client localhost', 'hosts' : 'localhost', 'roles' : names})
        

    with open("Playbook_Setup/siteClient.yml", "w") as f:
        yaml.dump(currentContent, f, default_flow_style=False)


def editPlaybookServer(conf):
    open("Playbook_Setup/siteServer.yml", 'w').close()
    
    currentContent = []

        
    currentContent.append({'name' : 'install server', 'hosts' : conf["host"], 'roles' : ["prereq", "engine", "core", conf['type']]}) 
        
    


    with open("Playbook_Setup/siteServer.yml", "w") as f:
        yaml.dump(currentContent, f, default_flow_style=False)



def tmux_line(c):

    varcat = lambda x,y: x + " " + y
    cmdcat = lambda x,y: x + " && " + y
    cmdstring = ""
    if "commands" in c:
        cmdstring = reduce(cmdcat, c["commands"])
    return ["tmux", "new-window", cmdstring]
