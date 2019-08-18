import os
import json
from cfn_flip import flip, to_yaml

def include_native_code_uri(template, native_package):
    resources = template['Resources']

    for k, v in resources.items():
        if v['Type'] == 'AWS::Serverless::Function' and ((v['Properties']).get('Runtime', None) == None or
                                                         (v['Properties']).get('Runtime', None) == 'provided'):
            v['Properties']['CodeUri'] = native_package

    template['Resources'] = resources

def gen_native_template(template_file, native_package, output_to):
    template = None

    with open(os.path.join('.', template_file), 'r') as tfile:
        template = json.loads(flip(tfile.read()))

    template['Parameters']['Runtime']['Default'] = 'provided'
    include_native_code_uri(template, native_package)

    with open(os.path.join('.', output_to, 'native-template.yml'), 'w') as ntfile:
        ntfile.write(to_yaml(json.dumps(template)))

    return
