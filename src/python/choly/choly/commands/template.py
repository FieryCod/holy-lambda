from .cli import cli
from choly.logic.template import gen_native_template
import logging
import click
import uuid


@cli.command(name='template',
             help='''Translates template.yml to native-template.yml''')
@click.option(
    '--template-file',
    '-t',
    help='AWS SAM template which will get translated to native-template.yml',
    default='./template.yml'
)
@click.option('--native-package',
              '-o',
              help='Path to native package',
              default='./latest.zip')
@click.option('--output_to',
              '-o',
              help='Location to which output the files',
              default='./resources')
def template(template_file, native_package, output_to):
    logger = logging.getLogger('template')

    logger.debug('Arguments passed: t=' + template_file + ', p=' + native_package + ', o=' + output_to)

    gen_native_template(template_file, native_package, output_to)

    return
