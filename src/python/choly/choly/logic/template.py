import os
import json
from cfn_flip import flip, to_yaml

def gen_native_template(template_file, native_package, output_to):
    template = None

    with open(os.path.join('.', template_file), 'r') as tfile:
        template = json.loads(flip(tfile.read()))

    template['Parameters']['Runtime']['Default'] = 'provided'
    template['Parameters']['CodeUri']['Default'] = native_package

    with open(os.path.join('.', output_to, 'native-template.yml'), 'w') as ntfile:
        ntfile.write(to_yaml(json.dumps(template)))

    return
