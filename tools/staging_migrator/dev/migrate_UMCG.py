"""
Script to migrate UMCG staging areas to a catalogue using the StagingMigrator class.
Supply the cohorts to be migrated as command line arguments.
"""


import logging
import os
import sys
from datetime import datetime

from dotenv import load_dotenv

from tools.staging_migrator.src.molgenis_emx2_staging_migrator import StagingMigrator

CATALOGUE = 'UMCG'


def main(staging_areas: list = None, args=None):

    # Set up the logger
    logging.basicConfig(level='DEBUG')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    load_dotenv()
    server_url = os.environ.get('MG_URL')
    token = os.environ.get('MG_TOKEN')

    if staging_areas is None:
        staging_areas = args[-1].split(',')

    with StagingMigrator(url=server_url, token=token, catalogue=CATALOGUE, table='Resources') as migrator:

        for sa in staging_areas:
            migrator.set_staging_area(sa)

            if (datetime.now() - migrator.last_change()).total_seconds() < 23 * 60 * 60:
                migrator.migrate(keep_zips=True)
            else:
                logging.info(f"Skipping {sa}.")


if __name__ == '__main__':
    cohorts = None
    main(staging_areas=cohorts, args=sys.argv)
