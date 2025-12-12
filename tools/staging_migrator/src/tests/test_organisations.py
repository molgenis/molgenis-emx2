"""
Tests the correct publication of an Organisation from a staging area onto a catalogue schema.
"""


import logging
import os

import pytest
import sys

from dotenv import load_dotenv

from staging_migrator.src.molgenis_emx2_staging_migrator import StagingMigrator
from staging_migrator.src.tests.utils import RESOURCES_PATH, zip_folder

CATALOGUE = 'catalogue'
STAGING_AREA = 'cohortStaging'

log = logging.getLogger('publisher')

# Set up the logger
logging.basicConfig(level='DEBUG')
logging.getLogger("requests").setLevel(logging.WARNING)
logging.getLogger("urllib3").setLevel(logging.WARNING)
load_dotenv()

@pytest.mark.asyncio
async def test_organisations():
    """
    Creates a staging area schema.
    Uploads data to this staging area.
    Migrates the data using the StagingMigrator.
    Asserts the data has been copied correctly to the catalogue.
    Deletes the entries from the catalogue.
    Deletes the staging area schema.
    :return:
    """

    server_url = os.environ.get('MG_URL')
    token = os.environ.get('MG_TOKEN')

    zip_folder("test_organisations")

    with StagingMigrator(url=server_url, token=token, target=CATALOGUE) as migrator:

        # if STAGING_AREA in migrator.schema_names:
        #     await migrator.delete_schema(STAGING_AREA)
        # await migrator.create_schema(name=STAGING_AREA, template="DATA_CATALOGUE_COHORT_STAGING")
        migrator.set_source(STAGING_AREA)
        # await migrator.upload_file(schema=STAGING_AREA, file_path=RESOURCES_PATH /"test_organisations.zip")

        assert len(migrator.get(schema=STAGING_AREA, table="Resources")) == 1

        migrator.migrate(keep_zips=True)
        # Raises error
        # 'Import failed: Transaction failed: insert or update on table "Resources" violates foreign key (ref_array) constraint. Details: Key ("creator.resource","creator.id")=(XYZ,org1) is not present in table "Organisations", column(s)("resource","id") in 163ms'

        # migrator.delete_resource()


