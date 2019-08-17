from choly.logic.envs import gen_envs
from ..infrastructure.shell import with_sh
from .cli import cli
import logging
import os
import click


@cli.command(
    name='envs',
    help='''Creates envs files for both docker and aws sam out of envs.json''')
@click.option('--output-to',
              '-o',
              default='./resources/',
              help='Location to which output the files')
@click.option('--envs-file',
              '-e',
              default='./resources/envs.json',
              help='Path to environment variables JSON file')
@click.option('--template-file',
              '-t',
              default='template.yml',
              help='SAM template file from which the function names are taken')
@click.option(
    '--profile',
    '-p',
    default='default',
    help=
    'AWS profile to use (in docker environment the AWS_KEY_ID & AWS_SECRET_KEY need to exists)'
)
def envs(
        output_to,
        envs_file,
        template_file,
        profile,
):
    logger = logging.getLogger('envs')
    logger.debug('Arguments passed: e=' + envs_file + ', t=' + template_file +
                 ', p=' + profile)

    aws_access_key, err1 = with_sh(
        ['aws', 'configure', 'get', 'aws_access_key_id', '--profile', profile],
        True)
    if err1:
        raise err1
    logger.debug(('aws_access_key_id: ' + '*' * 15 + aws_access_key[15:]).rstrip())

    aws_secret_key, err2 = with_sh([
        'aws', 'configure', 'get', 'aws_secret_access_key', '--profile',
        profile
    ], True)
    if err2:
        raise err2
    logger.debug(('aws_secret_key_id: ' + '*' * 25 + aws_secret_key[25:]).rstrip())

    logger.debug('Invoking...')
    return gen_envs(output_to, template_file, envs_file, aws_access_key,
                    aws_secret_key)
