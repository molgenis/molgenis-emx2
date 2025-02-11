"""
Script to delete a resource in a staging area from a catalogue.
"""

import logging
import os
import sys

from dotenv import load_dotenv

from tools.staging_migrator.src.molgenis_emx2_staging_migrator import StagingMigrator

log = logging.getLogger('resource-deleter')

CATALOGUE = 'catalogue'

def delete_resource(args: list):

    # Set up the logger
    logging.basicConfig(level='DEBUG')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    load_dotenv()
    server_url = os.environ.get('MG_URL')
    token = os.environ.get('MOLGENIS_TOKEN')

    staging_areas = args[-1].split(',')

    with StagingMigrator(url=server_url, token=token, catalogue=CATALOGUE, table="Resources") as migrator:

        for staging_area in staging_areas:
            log.info(f"\nDeleting resource in staging area {staging_area!r} from {CATALOGUE!r}.")
            migrator.set_staging_area(staging_area)
            migrator._delete_staging_from_catalogue()


if __name__ == '__main__':
    delete_resource(sys.argv)
