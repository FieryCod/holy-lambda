import distutils.spawn
import subprocess


def with_sh(args, stdout=False):
    args = list(filter(None, args))
    sh_cmd_path = distutils.spawn.find_executable(args[0])

    if sh_cmd_path == None:
        print(
            'Command ' + args[0] +
            ' sh is not in your PATH. Please install the util on your own. Exiting..'
        )
        exit()

    return subprocess.Popen(args, universal_newlines=True, stdout=subprocess.PIPE if stdout else None).communicate()
