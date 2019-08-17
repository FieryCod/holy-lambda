import logging


def setup():
    logging.basicConfig(
        format='%(asctime)s %(levelname)s(CMD=\'%(name)s\'): %(message)s',
        level=logging.DEBUG,
        datefmt='%d-%m-%y_%H:%M:%S')
