"""
Script to publish a resource in a catalogue from a staging area.

Add this script as a script task (see https://molgenis.github.io/molgenis-emx2/#/molgenis/use_scripts_jobs),
supply the latest version of the StagingMigration package as follows: molgenis_emx2_staging_migrator>={latest version}.

Specify in the variables below the catalogue, server url and log level.
Supply the names of the staging areas as a comma-separated list in the parameters field upon submitting the script.
"""


import logging
import os
import sys

from molgenis_emx2_staging_migrator import StagingMigrator

CATALOGUE = "catalogue"
SERVER_URL = "http://localhost:8080/"
LOG_LEVEL = "INFO"

log = logging.getLogger('publisher')


def main(args):

    # Set up the logger
    logging.basicConfig(level=LOG_LEVEL, stream=sys.stdout)
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    token = os.environ.get('MOLGENIS_TOKEN')

    staging_areas = args[-1].split(',')

    with StagingMigrator(url=SERVER_URL, token=token, catalogue=CATALOGUE) as migrator:

        for sa in staging_areas:
            log.info(f"\nPublishing resources in staging area {sa!r} to {CATALOGUE!r}.")
            migrator.set_staging_area(sa)
            migrator.migrate()


if __name__ == '__main__':
    main(sys.argv)
