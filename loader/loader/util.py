import fcntl
import logging
import os
import shutil
import tempfile
import tarfile
import yaml
import pybars
import json
import io
import random
import re

import subprocess as sp
from subprocess import check_output, PIPE
from functools import reduce
from pybars import Compiler


dataroot = "/tmp/netide"
extractPath = os.path.join(dataroot, "extractPath.txt")


def getWindowList():
    windowList = ""
    try:
        windowList = sp.check_output(['tmux', 'list-windows'])
    except sp.CalledProcessError:
        print("No active Session found.")
        return 1

    windowList = windowList.decode("utf-8")

    splitted = []
    for s in windowList.split(":"):
        splitted.append(s.split("("))

    name = []
    for s in splitted:
        name.append(s[0].replace("-", "").replace("*", "").lstrip().rstrip())

    del(name[0])


    return name

def extractPackage(path):
    os.makedirs(dataroot, exist_ok=True)



    if os.path.exists(extractPath):
        if os.path.isfile(extractPath):
            f = open(extractPath)
            tmpPath = f.read()
    else:
        tmpPath = dataroot

    print(tmpPath)

    with tarfile.open(path) as tar:

        members = tar.getmembers()

        #extracts the top level folder
        prefix = []
        for m in members:
            if "/" in m.name:
                tmpPrefix = m.name.split("/")[0]
                prefix.append(tmpPrefix)

        noTopArchive = False

        #checks if the package content is in an top folder or directly in the archive
        for pre in prefix:
            if pre == "apps" or pre == "composition" or pre == "templates":
                noTopArchive = True

        topLevelFolderName = ""


        if noTopArchive:
            print("no top level folder was found in archive.")

            #sets top level folder name to archive name
            if "tar.gz" in path:
                archiveName = os.path.splitext(path)[0]
                archiveName = os.path.splitext(archiveName)[0]

            else:
                archiveName = os.path.splitext(path)[0]

            topLevelFolderName = os.path.basename(archiveName)

        #if there is a top level folder the data will be extracted there
        else:
            topLevelFolderName = os.path.commonprefix(tar.getnames())

        folderPath = os.path.join(tmpPath, topLevelFolderName)



        if os.path.exists(folderPath):
            # numberOfCopys = 1
            # higherCopyNumberNeeded = True
            #
            # while higherCopyNumberNeeded:
            #     tmpLevelFolderName = topLevelFolderName + "_copy(" + str(numberOfCopys) +")"
            #     copyPath = os.path.join(tmpPath, tmpLevelFolderName)
            #     numberOfCopys = numberOfCopys +1
            #     if not os.path.exists(copyPath):
            #         higherCopyNumberNeeded = False
            #         tmpPath = copyPath
            shutil.rmtree(folderPath)


        os.makedirs(folderPath)

        tar.extractall(folderPath)

    if not noTopArchive:
        folderPath = os.path.join(folderPath, topLevelFolderName)

    print("Extracted to: " + folderPath)
    return folderPath

def setExtractionPath(path):
    os.makedirs(dataroot, exist_ok=True)

    with open(extractPath, 'w') as f:
        f.write(args.path)
    return 0

def createParamFile(path, name):

    appPath = os.path.join(path, name)
    appParamPath = os.path.join(appPath, name + '.params')
    paramDict = {}
    with open(appParamPath, 'r') as appParamFile:

        content = json.load(appParamFile)

        for key, value in content["parameters"].items():
            newVaue = "<" + value.split("=")[0].lstrip().rstrip().upper() + ">"


            paramDict[key] = newVaue

        return paramDict


def compileHandlebar(path, appName, paramPath=""):
    compiler = Compiler()

    source = ""
    templatePath = os.path.join(path, "templates/" + appName + "/src/config.py.hbs")

    with open(templatePath, 'r') as myfile:
        data=myfile.read()
        template = compiler.compile(data)

    if paramPath == "":
       paramPath = os.path.join(path, "parameters.json")



    with open(paramPath, 'r') as parameterJson:
        #check if template is filled with actual data
        content = json.load(parameterJson)
        for key,value in content.items():
            for kkey, vvalue in value.items():
                if "<" in vvalue or ">" in vvalue:
                    raise ValueError ("The chosen param file does not contain all needed values.")

        appContent = content[appName]

    output = template(appContent)

    return output


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
        names.append(name)
        print(name)

    currentContent.append({'name' : 'install client localhost', 'hosts' : 'localhost', 'roles' : names})


    with open("Playbook_Setup/siteClient.yml", "w") as f:
        yaml.dump(currentContent, f, default_flow_style=False)


def editPlaybookServer(conf):
    open("Playbook_Setup/siteServer.yml", 'w').close()

    currentContent = []


    currentContent.append({'name' : 'install server', 'hosts' : conf["host"], 'roles' : ["prereq", "engine", "core", "mininet", conf['type']]})




    with open("Playbook_Setup/siteServer.yml", "w") as f:
        yaml.dump(currentContent, f, default_flow_style=False)

def stripFileContent(path):
        with open(path, 'r') as f:

            s = io.StringIO()

            st = reduce(lambda x, y: x + y, f)
            f.seek(0)

            enclosed = st.lstrip()[0] == "{"

            if not enclosed: s.write("{")
            for line in f: s.write(line.replace("\t", "  ").replace(":\"", ": "))
            if not enclosed: s.write("}")

            s.seek(0)

            return s

def tmux_line(c):

    varcat = lambda x,y: x + " " + y
    cmdcat = lambda x,y: x + " && " + y
    cmdstring = ""
    if "commands" in c:
        cmdstring = reduce(cmdcat, c["commands"])
    return cmdstring

def is_sw_installed(sw):

    d = dict(os.environ)
    path = d['PATH'].split(':')

    flag = False
    for route in path:
        try:
            ls_result = sp.check_output(["ls", route]).decode('utf-8')
        except sp.CalledProcessError as e:
            logging.warning(e.output)
        if sw in ls_result:
            if flag:
                logging.warning("Several versions of software: " + sw)
            flag = True
    return flag

def check_sw_version(sw_name, req_version):

    req_version = req_version.split('.')
    try:
        stream = open("known_software.yaml", 'r')
    except:
        stream = open("loader/known_software.yaml", 'r')

    try:
        known_sw = yaml.load(stream)["software"]
    except yaml.YAMLError as exc:
        logging.error(exc)
        return False

    sw_struct = None
    for sw in known_sw:
        if sw["name"] == sw_name:
            sw_struct = sw

    if sw_struct == None:
        logging.warning("could not verify software version for {}".format(sw_name))
        return True

    version_output = sp.check_output(sw_struct["command"].split(' '), stderr=sp.STDOUT).decode('utf-8')

    act_version = re.search(sw_struct["regex"], version_output).group(0).split('.')

    for i in list(range(0,len(req_version))):
        #if the actual version is greater than the required one, it fulfills the reqquirement
        if int(req_version[i]) < int(act_version[i]):
             return True
        #if the required version is greater than the actual one, it doesn't fulfill the requirement
        if int(req_version[i]) > int(act_version[i]):
             return False
        #if they're the same check subversion

    #At this point the actual version is greateror equal to the requirement
    return True
