import subprocess

class Controller(object):
    cmd  = None
    name = None
    params   = None

    def valid(self):
        raise NotImplementedError()

    def install(self):
        raise NotImplementedError()

    def start(self):
        raise NotImplementedError()

class RyuController(Controller):
    name = "ryu"
    cmd  = "ryu-manager"
    params = "--ofp-tcp-listen-port={}"

    def __init__(self, port, entrypoint):
        self.port = port
        self.entrypoint = entrypoint

    def install(self):
        # TODO?
        pass

    def valid(self):
        # TODO: check if self.cmd exists
        return True

    def start(self):
        cmdline = ["sudo", self.cmd, self.params.format(self.port)]
        cmdline.append(self.entrypoint)
        print('Launching "{}" now'.format(cmdline))
        return subprocess.Popen(cmdline).pid
        # return -1

