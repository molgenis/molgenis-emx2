"""
Script to migrate staging areas to a catalogue using the StagingMigrator class.
Supply the cohorts to be migrated as command line arguments.
"""


import logging
import os
import sys

from dotenv import load_dotenv

from molgenis_emx2_staging_migrator import StagingMigrator


CATALOGUE = 'UMCG'


def main(args):

    # Set up the logger
    logging.basicConfig(level='DEBUG')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    load_dotenv()
    server_url = os.environ.get('MG_URL')
    token = os.environ.get('MG_TOKEN')

    staging_areas = args[-1].split(',')

    with StagingMigrator(url=server_url, token=token, catalogue=CATALOGUE) as migrator:
        print(migrator.status)

        for sa in staging_areas:
            migrator.set_staging_area(sa)

            migrator.migrate()


if __name__ == '__main__':
    main(sys.argv)
