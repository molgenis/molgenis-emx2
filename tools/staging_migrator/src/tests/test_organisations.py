"""
Tests the correct publication of an Organisation from a staging area onto a catalogue schema.
"""
import io
import logging
import os

import pandas as pd
import pytest
import sys

from dotenv import load_dotenv

from staging_migrator.src.molgenis_emx2_staging_migrator import StagingMigrator
from staging_migrator.src.molgenis_emx2_staging_migrator.utils import process_statement
from staging_migrator.src.tests.utils import RESOURCES_PATH, zip_folder

CATALOGUE = 'catalogue'
STAGING_AREA = 'testCohort'

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

        migrator.migrate_cohort_staging(keep_zips=True)
        # Raises error
        # 'Import failed: Transaction failed: insert or update on table "Resources" violates foreign key (ref_array) constraint. Details: Key ("creator.resource","creator.id")=(XYZ,org1) is not present in table "Organisations", column(s)("resource","id") in 163ms'

        # migrator.delete_resource()


def test_process_organisations():
    """Unit tests for the `process_organisations` method."""
    orgs_csv = """resource,id,type,name,organisation,other organisation,department,website,email,logo,role,is lead organisation
    R1,O1,Organisation,Org1,Leiden University Medical Center,,,O1@O1.com,,,,true
    R2,O2,Organisation,Org2,University Medical Center Groningen,,,O2@O2.com,,,,
    R3,O3,Organisation,Org3,Hanze,,,O3@O3.com,,,,false
    """
    orgs_df = pd.read_csv(io.StringIO(orgs_csv), sep=",")

    server_url = os.environ.get('MG_URL')
    with StagingMigrator(url=server_url) as migrator:
        processed_orgs = migrator.process_organisations(orgs_df)
    assert processed_orgs["organisation name"].values.tolist() == ['Leiden University Medical Center', 'University Medical Center Groningen', 'Hanze']
    assert processed_orgs["organisation pid"].values.tolist() == ['ROR:05xvt9f17', 'ROR:03cv38k47', None]
    assert processed_orgs["organisation website"].values.tolist() == ['https://www.lumc.nl', 'https://www.umcg.nl', None]

def test_process_contacts():
    """Unit test for the `process_contacts` method."""
    contacts_csv = """resource,role,first name,last name,statement of consent personal data,email
    R1,Principal Investigator,A1,B1,true,A1B1@R1.com
    R2,Primary contact,A2,B2,true,A2B2@R2.com
    R3,Participant,A3,B3,false,
    R4,Participant,A4,B4,false,A4B4@R4.com"""
    contacts_df = pd.read_csv(io.StringIO(contacts_csv))

    processed_contacts = process_statement(contacts_df)
    assert "A3" not in processed_contacts["first name"].values
    assert processed_contacts["mg_delete"].values.tolist() == [False, False, True]

