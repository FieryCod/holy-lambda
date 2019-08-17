import cfn_flip
import json
import sys
import choly.infrastructure.log
import choly.commands.core
import os

DEBUG = os.environ.get('DEBUG', '0')


def main():
    """
    Delegates the user cmd action to corresponding cmd handler
    """
    if DEBUG == '1':
        choly.infrastructure.log.setup()

    choly.commands.core.cli()

if __name__ == '__main__':
    main()
