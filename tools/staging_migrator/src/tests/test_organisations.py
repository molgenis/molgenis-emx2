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
async def test_migration():
    """
    Migrates data from 'testCohort' to 'catalogue' using the StagingMigrator.
    Asserts the data has been copied correctly to the catalogue.
    :return:
    """

    server_url = os.environ.get('MG_URL')
    token = os.environ.get('MG_TOKEN')

    with StagingMigrator(url=server_url, token=token, target=CATALOGUE) as migrator:
        migrator.set_source(STAGING_AREA)
        migrator.migrate(keep_zips=True)

        resources = migrator.get("Resources", schema=CATALOGUE, query_filter=f"id == {STAGING_AREA!r}")
        assert len(resources) == 1

@pytest.mark.asyncio
async def test_delete_resource():
    """Tests the `delete_resource` method."""

    server_url = os.environ.get('MG_URL')
    token = os.environ.get('MG_TOKEN')

    with StagingMigrator(url=server_url, token=token, target=CATALOGUE) as migrator:
        migrator.set_source(STAGING_AREA)
        migrator.delete_resource()

        resources = migrator.get("Resources", schema=CATALOGUE, query_filter=f"id == {STAGING_AREA!r}")
        assert len(resources) == 0




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

