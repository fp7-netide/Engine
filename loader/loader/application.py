import inspect
import json
import os
import logging

from loader import controllers
from loader import environment

class Application(object):
    metadata = {}

    def __init__(self, prefix):
        self.path = prefix

        self.files = []
        for dirname, dirs, files in os.walk(self.path):
            for f in files:
                self.files.append(os.path.join(os.path.abspath(dirname), f))

        p = os.path.join(self.path, "_meta.json")
        if os.path.exists(p):
            with open(p) as f:
                self.metadata = json.load(f)

        self.enabled = self.metadata.get("enabled", True)

    def __str__(self):
        return self.path


    def valid_requirements(self):
        reqs = self.metadata.get("requirements", {})

        # Return True if all requirements are met
        # Check Software
        # TODO: Allow wildcards in versions? re matching?
        for c in reqs.get("Software", {}).get("Controllers", {}):
            cls = {k.lower(): v for k, v in inspect.getmembers(controllers)}.get(c["name"].lower())
            if cls is None:
                logging.warning("Not checking for unknown controller {}".format(c))
                continue
            v = cls.version()
            if v != c["version"]:
                logging.error("Expected {} version {}, got {}".format(cls.__name__, c["version"], v))
                return False
        # TODO: Check libraries
        # TODO: Check languages
        try:
            environment.check_languages(reqs.get("Software", {}).get("Languages", {}))
        except environment.LanguageCheckException as e:
            logging.error("Missing depency: {}".format(str(e)))
            return False

        try:
            environment.check_hardware(reqs.get("Hardware", {}))
        except environment.HardwareCheckException as e:
            logging.error("Hardware configuration mismatch: {}".format(str(e)))
            return False
        # TODO: Check network
        return True


    @classmethod
    def get_controller(cls, path):
        with open(os.path.join(path, "_meta.json")) as f:
            m = json.load(f)
            c = m.get("controller")
            if c is None:
                return None
            return {k.lower(): v for k, v in inspect.getmembers(controllers)}.get(c.lower())
