import fcntl
import logging
import os
import shutil
import tempfile

import subprocess as sp

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

    def __init__(self, prefix=""):
        self.p = prefix

    def __enter__(self):
        self.d = tempfile.mkdtemp(self.p)
        return self.d

    def __exit__(self, exc_type, exc_value, traceback):
        if self.d is None:
            return

        shutil.rmtree(self.d, ignore_errors=True)

def build_ssh_commands(c):
    """Build ssh command lists from client tuple"""
    # Tuple layout:
    # (host, port, identity)

    # Let's see if we can use rsync instead of scp
    use_rsync = True
    try:
        sp.check_output(["rsync"], stderr=sp.DEVNULL)
    except sp.CalledProcessError:
        # We can use rsync
        pass
    except FileNotFoundError:
        # We have to use scp
        use_rsync = False

    ssh = ["ssh"]
    if use_rsync:
        scp = ["rsync", "-a", "-H", "-A", "-X"]
    else:
        scp = ["scp", "-B", "-C", "-r"]

    if len(c) >= 2:
        ssh.extend(["-p", str(c[1])])
        if not use_rsync:
            scp.extend(["-P", str(c[1])])
    if len(c) == 3:
        ssh.extend(["-i", str(c[2])])
        if not use_rsync:
            scp.extend(["-i", str(c[2])])
    ssh.extend(["-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile={}".format(tempfile.mktemp())]) # XXX
    ssh.append(c[0])

    if use_rsync:
        scp.extend(["-e", " ".join(ssh[:-1])])

    return (ssh, scp)

def spawn_logged(cmd):
    p = sp.Popen(cmd, stdout=sp.PIPE, stderr=sp.STDOUT)
    for l in p.stdout:
        l = l.decode('utf-8').rstrip()
        logging.debug(l)
    return p.wait()
