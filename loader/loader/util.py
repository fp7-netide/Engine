import logging
import shutil
import tempfile

import subprocess as sp

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

    ssh = ["ssh"]
    scp = ["scp", "-B", "-C", "-r"]
    if len(c) >= 2:
        ssh.extend(["-p", str(c[1])])
        scp.extend(["-P", str(c[1])])
    if len(c) == 3:
        ssh.extend(["-i", str(c[2])])
        scp.extend(["-i", str(c[2])])
    ssh.append(c[0])

    return (ssh, scp)

def spawn_logged(cmd):
    p = sp.Popen(cmd, stdout=sp.PIPE, stderr=sp.STDOUT)
    for l in p.stdout:
        l = l.decode('utf-8').rstrip()
        logging.debug(l)
    return p.wait()
