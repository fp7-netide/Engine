import loader.environment as env

def test_check_hardware_arch():
    env.check_hardware({ "cpuarch": "x86_64" })
    env.check_hardware({ "cpuarch": "x86" })

    try:
        env.check_hardware({ "cpuarch": "FloopenShmitz" })
    except env.HardwareCheckException:
        pass
    else:
        assert False, "Test should have failed but didn't"

def test_check_hardware_cpufreq():
    env.check_hardware({ "cpufreq": 500 })

    try:
        env.check_hardware({ "cpufreq": float("inf") })
    except env.HardwareCheckException:
        pass
    else:
        assert False, "Test should have failed but didn't"

def test_check_hardware_cpucores():
    env.check_hardware({ "cpucores": 1 })

    try:
        env.check_hardware({ "cpucores": float("inf") })
    except env.HardwareCheckException:
        pass
    else:
        assert False, "Test should have failed but didn't"

def test_check_hardware_mem():
    env.check_hardware({ "memory": 128 })

    try:
        env.check_hardware({ "memory": float("inf") })
    except env.HardwareCheckException:
        pass
    else:
        assert False, "Test should have failed but didn't"

def test_check_hardware_unknown_key():
    try:
        env.check_hardware({ "fnord": "yes" })
    except KeyError:
        pass
    else:
        assert False, "Test should have failed but didn't"

def test_check_languages():
    env.check_languages([{"name": "python", "version": "3.4"}])
    env.check_languages([{"name": "java", "version": "1.7"}])
    env.check_languages([{"name": "sbcl", "version": "7.2"}])
