import cfn_flip
import json
import os
import sys
from .shared import get_fns_definitions, update_dict
from functools import reduce


def exit_when_file_not_exists(tag, path):
    if os.path.exists(path) == False:
        print('File path provided by parameter: ' + tag +
              ' does not exists. Incorrect path: ' + path)
        exit()


def gen_docker_envs_list_file(output_to, envs, aws_access_key, aws_secret_key):
    output = 'AWS_ACCESS_KEY=' + aws_access_key + 'AWS_SECRET_KEY=' + aws_secret_key
    output = reduce(lambda acc, k: acc + k + '=' + str(envs[k]) + '\n', envs,
                    output)

    with open(os.path.join(output_to, 'envs.list'), 'w') as outf:
        outf.write(output)

    return

def gen_aws_sam_envs_file(output_to, envs, template_file_path):
    fns_dict = get_fns_definitions(template_file_path)

    if fns_dict == {}:
        print('No lambda function declarations found in ' +
              template_file_path + '. Skipping sam-envs.json generation!')
        exit()

    with open(os.path.join(output_to, 'sam-envs.json'), 'w') as fsam_file:
        output = reduce(
            lambda acc, k: update_dict(
                acc, {fns_dict[k]['Properties']['FunctionName']: envs})
            if 'FunctionName' in fns_dict[k]['Properties'] else update_dict(
                acc, {k: envs}), fns_dict, {})
        fsam_file.write(json.dumps(output, indent=4, sort_keys=True))

    return


def read_envs(envs_file_path):
    with open(envs_file_path, 'r') as efile:
        return json.loads(efile.read())


def gen_envs(output_to, template_file_path, envs_file_path, aws_access_key,
             aws_secret_key):
    exit_when_file_not_exists('template-file', template_file_path)
    exit_when_file_not_exists('envs-file', envs_file_path)

    envs = read_envs(envs_file_path)

    gen_docker_envs_list_file(output_to, envs, aws_access_key, aws_secret_key)
    gen_aws_sam_envs_file(output_to, envs, template_file_path)

    return
