"""
Script to publish staging areas Resources to a catalogue using the StagingMigrator class.
Supply the names of the staging areas to be published as command line arguments.
"""


import logging
import os
import sys

from dotenv import load_dotenv

from tools.staging_migrator.src.molgenis_emx2_staging_migrator import StagingMigrator

CATALOGUE = 'catalogue'

log = logging.getLogger('publisher')


def main(args):

    # Set up the logger
    logging.basicConfig(level='DEBUG')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    load_dotenv()
    server_url = os.environ.get('MG_URL')
    token = os.environ.get('MG_TOKEN')

    staging_areas = args[-1].split(',')

    with StagingMigrator(url=server_url, token=token, target=CATALOGUE) as migrator:

        for sa in staging_areas:
            log.info(f"\nPublishing resources in staging area {sa!r} to {CATALOGUE!r}.")
            migrator.set_source(sa)
            migrator.migrate()


if __name__ == '__main__':
    main(sys.argv)
