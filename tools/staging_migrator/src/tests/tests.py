"""
Tests the correct publication of an Organisation from a staging area onto a catalogue schema.
"""
import io
import logging
import numpy as np
import os
import pandas as pd
import pytest
from dotenv import load_dotenv

from staging_migrator.src.molgenis_emx2_staging_migrator import StagingMigrator

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

    if not server_url:
        raise ValueError("Did not get value for server url.")

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

    if not server_url:
        raise ValueError("Did not get value for server url.")

    with StagingMigrator(url=server_url, token=token, target=CATALOGUE) as migrator:
        migrator.set_source(STAGING_AREA)
        migrator.delete_resource()

        resources = migrator.get("Resources", schema=CATALOGUE, query_filter=f"id == {STAGING_AREA!r}")
        assert len(resources) == 0

def test_process_organisations():
    """Unit tests for the `process_organisations` method."""
    orgs_csv = ("""resource,id,type,name,organisation,other organisation,department,website,email,logo,role,is lead organisation\n"""
                """R1,O1,Organisation,Org1,Leiden University Medical Center,,,O1@O1.com,,,,true\n"""
                """R2,O2,Organisation,Org2,University Medical Center Groningen,,,O2@O2.com,,,,\n"""
                """R3,O3,Organisation,Org3,Hanze,,,O3@O3.com,,,,false\n"""
                """R4,O4,Organisation,Org4,,,,O4@O4.com,,,,true""")
    orgs_df = pd.read_csv(io.StringIO(orgs_csv), sep=",")

    server_url = os.environ.get('MG_URL')
    if not server_url:
        raise ValueError("Did not get value for server url.")

    with StagingMigrator(url=server_url) as migrator:
        processed_orgs = migrator.process_organisations(orgs_df)
    assert processed_orgs["organisation name"].values.tolist() == ['Leiden University Medical Center', 'University Medical Center Groningen', 'Hanze', np.nan]
    assert processed_orgs["organisation pid"].values.tolist() == ['ROR:05xvt9f17', 'ROR:03cv38k47', None, None]
    assert processed_orgs["organisation website"].values.tolist() == ['https://www.lumc.nl', 'https://www.umcg.nl', None, None]
    assert migrator.warnings == ['No organisation for (resource, id) = (R4, O4)']

def test_add_resource():
    """Tests the `add_resource` method."""
    load_dotenv()
    server_url = os.environ.get('MG_URL')
    token = os.environ.get('MG_TOKEN')

    if not server_url:
        raise ValueError("Did not get value for server url.")

    with StagingMigrator(url=server_url, token=token, target=CATALOGUE) as migrator:
        migrator.set_source(STAGING_AREA)
        migrator.add_data_resource("main catalogue")

