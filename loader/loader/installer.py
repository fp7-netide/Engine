import shutil
import subprocess as sp
import sys
import tempfile

from loader.package import Package

class InstallException(Exception): pass

def do_server_install(pkg):
    print("Doing server install for '{}' now".format(pkg), file=sys.stderr)
    t = tempfile.mkdtemp(prefix="netide-server-install")
    p = Package(pkg, t)
    if p.applies():
        print("Nothing to be done here for '{}'".format(pkg), file=sys.stderr)
    shutil.rmtree(t, ignore_errors=True)

def do_client_installs(pkg):
    # TODO:
    # [ ] Monitor progress

    # TODO: replace with list of client controllers from topology file
    clients = ['10.0.2.15']
    for c in clients:
        print("Doing client install for '{}' on host {} now".format(pkg, c), file=sys.stderr)
        # TODO:
        # [X] Copy self to application controller (SCP)
        where = sp.check_output(["ssh", c, "pwd"], stderr=sp.STDOUT).strip().decode('utf-8')
        print(where, file=sys.stderr)
        p = sp.check_output(["ssh", c, "mkdir -p {}/netide-loader".format(where)], stderr=sp.STDOUT)
        print(p, file=sys.stderr)
        p = sp.check_output(["scp", "-r", ".", "{}/netide-loader".format(where)], stderr=sp.STDOUT)
        print(p, file=sys.stderr)

        # [X] Run setup.sh on app controller to bootstrap ourselves
        p = sp.check_output(["ssh", c, "cd {}/netide-loader; ./setup.sh".format(where)], stderr=sp.STDOUT)
        print(p, file=sys.stderr)

        # [X] Copy package to app controller
        dir = sp.check_output(["ssh", c, "mktemp", "-d"], stderr=sp.STDOUT).strip().decode('utf-8')
        print(dir, file=sys.stderr)
        p = sp.check_output(["scp", "-r", pkg, "{host}:{dir}".format(host=c, dir=dir)], stderr=sp.STDOUT)
        print(p, file=sys.stderr)

        # [ ] Run self with arguments ['install-appcontroller', args.package]

        # [X] Remove package from app controller
        p = sp.check_output(["ssh", c, "rm -r {}".format(dir)], stderr=sp.STDOUT).strip().decode('utf-8')
        print(p)
