import inspect
import json
import os
import shutil
import tempfile
import zipfile

import controllers
import environment
from application import Application

class Package(object):
    requirements = {}
    controllers = {}
    cleanup = False

    def __init__(self, prefix, dataroot):
        self.path = os.path.abspath(prefix)
        if prefix.endswith(".zip") and os.path.isfile(self.path):
            p = tempfile.mkdtemp(prefix="netide-tmp")
            with zipfile.ZipFile(self.path) as zf:
                zf.printdir()
                zf.extractall(path=p)
            self.path = p
            self.cleanup = True

        p = os.path.join(self.path, "_system_requirements.json")
        if os.path.exists(p):
            with open(p) as f:
                self.requirements = json.load(f)

        p = os.path.join(self.path, "_apps")
        for d in os.listdir(p):
            app = os.path.join(p, d)
            ctrl = Application.get_controller(app)
            if ctrl not in self.controllers:
                self.controllers[ctrl] = ctrl(dataroot)
            self.controllers[ctrl].applications.append(Application(app))

    def __del__(self):
        if self.cleanup:
            shutil.rmtree(self.path, ignore_errors=True)


    def __str__(self):
        return 'Package("{}")'.format(self.path)

    def valid_requirements(self):
        # Return True if all requirements are met
        # Check Software
        # TODO: Allow wildcards in versions? re matching?
        for c in self.requirements.get("Software", {}).get("Controllers", {}):
            cls = {k.lower(): v for k, v in inspect.getmembers(controllers)}.get(c["name"].lower())
            if cls is None:
                print("Not checking for unknown controller {}".format(c))
                continue
            v = cls.version()
            if v != c["version"]:
                print("Expected {} version {}, got {}".format(cls.__name__, c["version"], v), file=sys.stderr)
                return False
        # TODO: Check libraries
        # TODO: Check languages
        try:
            environment.check_hardware(self.requirements.get("Hardware", {}))
        except environment.HardwareCheckException as e:
            print("Hardware configuration mismatch: {}".format(str(e)), file=sys.stderr)
            return False
        # TODO: Check network
        return True

    def applies(self):
        # FIXME: there's a lot of validation missing here: checking topology and what not
        if not self.valid_requirements():
            print("At least one requirement was not met")
            return False

        # Make sure all controllers listed in applications actually appear in our requirements file
        ctrls = { x.get("name") for x in self.requirements.get("Software", {}).get("Controllers", {}) }
        for c in self.controllers.keys():
            cname = c.__name__.lower()
            if cname not in ctrls:
                print("Could not find controller {} in the list of required controllers".format(cname), file=sys.stderr)
                return False

        # TODO: warn about unused controllers?
        return True

    def start(self):
        return {cls.__name__: { "procs": c.start(), "apps": [str(a) for a in c.applications] }
                for cls, c in self.controllers.items()}

