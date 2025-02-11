"""
Script to delete a resource in a staging area from a catalogue.

Add this script as a script task (see https://molgenis.github.io/molgenis-emx2/#/molgenis/use_scripts_jobs),
supply the latest version of the StagingMigration package as follows: molgenis_emx2_staging_migrator>={latest version}.

Specify in the variables below the catalogue, server url and log level.
Supply the names of the staging areas as a comma-separated list in the parameters field upon submitting the script.
"""

import logging
import os
import sys

from molgenis_emx2_staging_migrator import StagingMigrator

log = logging.getLogger('resource-deleter')

CATALOGUE = 'catalogue'
SERVER_URL = "http://localhost:8080/"
LOG_LEVEL = "INFO"

def delete_resource(args: list):

    # Set up the logger
    logging.basicConfig(level=LOG_LEVEL)
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    token = os.environ.get('MOLGENIS_TOKEN')

    staging_areas = args[-1].split(',')

    with StagingMigrator(url=SERVER_URL, token=token, catalogue=CATALOGUE, table="Resources") as migrator:

        for staging_area in staging_areas:
            log.info(f"\nDeleting resource in staging area {staging_area!r} from {CATALOGUE!r}.")
            migrator.set_staging_area(staging_area)
            migrator._delete_staging_from_catalogue()


if __name__ == '__main__':
    delete_resource(sys.argv)
