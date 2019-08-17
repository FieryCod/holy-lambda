from .cli import cli
from choly.logic.graal import graal_native_cmd_exec
import logging
import os
import click
import uuid


@cli.command(name='graal',
             help='''Enables cross platform GraalVM utilities usage''')
@click.option('--execution-str', '-e', help='Arguments passed to GraalVM tool')
@click.option('--dsettings-str',
              '-s',
              default='',
              help='Pass custom docker settings as a string to the utility')
@click.option('--custom-image',
              '-c',
              default='fierycod/graalvm-native-image:latest',
              help='Specify custom docker image')
@click.option('--destroy',
              '-d',
              default=True,
              help='Destroy the container after usage')
def graal(execution_str, dsettings_str, custom_image, destroy):
    logger = logging.getLogger('graal')
    name = uuid.uuid4().hex

    logger.debug('Arguments passed: e=' + execution_str + ', s=' +
                 dsettings_str + ', c=' + str(custom_image))
    logger.debug('Name of the container: ' + name)

    graal_native_cmd_exec(execution_str, dsettings_str, custom_image, destroy,
                          name)

    return
