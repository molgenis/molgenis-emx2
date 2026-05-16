"""
Script to publish staging areas Resources to a catalogue using the StagingMigrator class.
Supply the names of the staging areas to be published as command line arguments.
"""


import logging
import os
import sys
from dotenv import load_dotenv
from molgenis_emx2_pyclient.exceptions import NoSuchSchemaException, PyclientException

from staging_migrator.src.molgenis_emx2_staging_migrator import StagingMigrator
from staging_migrator.src.molgenis_emx2_staging_migrator.exceptions import DraftException, MissingContactException, \
    NoSuchResourceException, StagingMigratorException

CATALOGUE = 'catalogue'

log = logging.getLogger('publisher')


def main(args):

    # Set up the logger
    logging.basicConfig(level='INFO')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    load_dotenv()
    server_url = os.environ.get('MG_URL')
    token = os.environ.get('MG_TOKEN')
    target_resource = os.environ.get('MG_TARGET_RESOURCE')

    if not server_url:
        raise ValueError("Did not get value for server url.")

    staging_areas = args[-1].split(',')

    with StagingMigrator(url=server_url, token=token, target=CATALOGUE) as migrator:

        for sa in staging_areas:
            log.info(f"Publishing resources in staging area {sa!r} to {CATALOGUE!r}.")
            migrator.set_source(sa)
            try:
                migrator.migrate(keep_zips=True)
                if target_resource is not None:
                    migrator.add_data_resource(target_resource)
            except DraftException:
                for error in migrator.errors:
                    log.error(f"{sa}: DraftException({error})")
            except MissingContactException:
                for error in migrator.errors:
                    log.error(f"{sa}: MissingContactException({error})")
            except NoSuchSchemaException:
                for error in migrator.errors:
                    log.error(f"{sa}: NoSuchSchemaException({error})")
            except NoSuchResourceException:
                for error in migrator.errors:
                    log.error(f"{sa}: NoSuchResourceException({error})")
            except StagingMigratorException:
                for error in migrator.errors:
                    log.error(f"{sa}: StagingMigratorException({error})")
            except PyclientException:
                for error in migrator.errors:
                    log.error(f"{sa}: PyclientException({error})")
            except Exception:
                for error in migrator.errors:
                    log.error(f"{sa}: Exception({error})")
            finally:
                for w in migrator.warnings:
                    log.warning(f"{sa}: {w}")
                migrator.errors = []
                migrator.warnings = []


if __name__ == '__main__':
    main(sys.argv)
