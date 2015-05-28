# Stuff that checks the environemnt: hardware, languages (?), network (?)

import os

class HardwareCheckException(Exception): pass

def check_hardware(h):
	"Raises an exception if any of the requirements in req are not met by the current hardware. Raises a KeyError if an unknown \
	check is requested"

	# Check CPU
	if "cpuarch" in h:
		m = os.uname().machine
		a = h["cpuarch"]
		if a == "x86" and m == "x86_64":
			a = "x86_64"
		if m != a:
			raise HardwareCheckException("Invalid CPU architecture: want {}, have {}".format(a, m))

	if "cpufreq" in h:
		with open("/proc/cpuinfo") as f:
			for l in f.readlines():
				if not l.startswith("cpu MHz"):
					continue
				have = float(l.split(":", 2)[1])
				if have < h["cpufreq"]:
					raise HardwareCheckException("Invalid CPU frequency: want {} MHz, have {} MHz".format(h["cpufreq"], have))

	if "cpucores" in h:
		numcpu = 0
		with open("/proc/cpuinfo") as f:
			for l in f.readlines():
				if not l.startswith("processor"):
					continue
				numcpu += 1
		if numcpu < h["cpucores"]:
			raise HardwareCheckException("Invalid number of CPUs: want {}, have {}".format(h["cpucores"], numcpu))

	# Check Memory
	if "memory" in h:
		print("checking for memory")
		with open("/proc/meminfo") as f:
			for l in f.readlines():
				if not l.startswith("MemTotal:"):
					continue
				l = l.split(":", 1)[1].strip()
				have = int(l.split(" ", 1)[0]) // 1024
				if have < h["memory"]:
					raise HardwareCheckException("Invalid amount of memory: want {} MB, have {} MB".format(h["memory"], have))

	for k, v in h.items():
		if k not in ["cpuarch", "cpufreq", "cpucores", "memory"]:
			raise KeyError(k)
