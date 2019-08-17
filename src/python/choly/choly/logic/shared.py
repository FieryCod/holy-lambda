import cfn_flip
import json
import os
import sys
from functools import reduce

def update_dict(adict, update):
    adict.update(update)

    return adict

def get_fns_definitions(template_file_path):
    template_file_handler = open(template_file_path)

    with open(template_file_path, 'r'):
        resources = json.loads(
            cfn_flip.to_json(template_file_handler))['Resources']

        return reduce(
            lambda acc, k: update_dict(acc, {k: resources[k]})
            if resources[k]['Type'] == 'AWS::Serverless::Function' else acc,
            resources, {})
