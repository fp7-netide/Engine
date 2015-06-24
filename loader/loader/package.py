import inspect
import json
import logging
import os
import shutil
import tempfile
import zipfile

from loader import controllers
from loader.application import Application

class Package(object):
    requirements = {}
    controllers = {}
    cleanup = False

    def __init__(self, prefix, dataroot):
        self.path = os.path.abspath(prefix)
        if prefix.endswith(".zip") and os.path.isfile(self.path):
            p = tempfile.mkdtemp(prefix="netide-tmp")
            with zipfile.ZipFile(self.path) as zf:
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

    def applies(self):
        # FIXME: there's a lot of validation missing here: checking topology and what not
        for c in self.controllers:
            for a in c.applications:
                if not a.valid_requirements():
                    logging.error("Requirements for application {} not met".format(a))
                    return False

        return True

    def start(self):
        return {cls.__name__: { "procs": c.start(), "apps": [str(a) for a in c.applications] }
                for cls, c in self.controllers.items()}
