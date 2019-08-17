from ..infrastructure.shell import with_sh
import os

def graal_native_cmd_exec(execution_str, dsettings_str, custom_image, destroy,
                          name):
    _, err1 = with_sh([
        'docker', 'run', '--name', name,
        '-v', os.getcwd() + '/:/project:Z', dsettings_str, '-it', custom_image,
        '/bin/bash', '-c', 'cd /project && ' + execution_str
    ])

    if err1:
        raise err1

    if destroy is True:
        with_sh(['docker', 'rm', name])
        print('Successfully removed container: ' + name)

    return
