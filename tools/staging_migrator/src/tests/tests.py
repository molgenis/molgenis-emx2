"""
Tests the correct publication of an Organisation from a staging area onto a catalogue schema.
"""
import io
import logging
import os

import numpy as np
import pandas as pd
import pytest
from dotenv import load_dotenv

from staging_migrator.src.molgenis_emx2_staging_migrator import StagingMigrator
from staging_migrator.src.molgenis_emx2_staging_migrator.exceptions import MissingContactException
from staging_migrator.src.molgenis_emx2_staging_migrator.utils import check_hricore, process_contacts

CATALOGUE = 'UMCG'
STAGING_AREA = 'testUMCGCohort'

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
    orgs_csv = ("""resource,id,type,name,organisation,other organisation,department,website,email,logo,role,is lead organisation\n"""
                """R1,O1,Organisation,Org1,Leiden University Medical Center,,,O1@O1.com,,,,true\n"""
                """R2,O2,Organisation,Org2,University Medical Center Groningen,,,O2@O2.com,,,,\n"""
                """R3,O3,Organisation,Org3,Hanze,,,O3@O3.com,,,,false\n"""
                """R4,O4,Organisation,Org4,,,,O4@O4.com,,,,true""")
    orgs_df = pd.read_csv(io.StringIO(orgs_csv), sep=",")

    server_url = os.environ.get('MG_URL')
    with StagingMigrator(url=server_url) as migrator:
        processed_orgs = migrator.process_organisations(orgs_df)
    assert processed_orgs["organisation name"].values.tolist() == ['Leiden University Medical Center', 'University Medical Center Groningen', 'Hanze', np.nan]
    assert processed_orgs["organisation pid"].values.tolist() == ['ROR:05xvt9f17', 'ROR:03cv38k47', None, None]
    assert processed_orgs["organisation website"].values.tolist() == ['https://www.lumc.nl', 'https://www.umcg.nl', None, None]
    assert migrator.warnings == ['No organisation for (resource, id) = (R4, O4)']

def test_process_contacts():
    """Unit test for the `process_contacts` method."""
    contacts_csv = ("""resource,role,first name,last name,statement of consent personal data,email\n"""
                    """R1,Principal Investigator,A1,B1,true,A1B1@R1.com\n"""
                    """R2,Primary contact,A2,B2,true,A2B2@R2.com\n"""
                    """R3,Participant,A3,B3,false,\n"""
                    """R4,Participant,A4,B4,false,A4B4@R4.com""")
    contacts_df = pd.read_csv(io.StringIO(contacts_csv))
    resources_csv = ("""id,contact point.resource,contact point.first name,contact point.last name\n"""
                     """R1,R1,A1,B1\n"""
                     """R2,R2,A2,B2\n"""
                     """R3,R3,A3,B3\n"""
                     """R4,R4,A4,B4\n""")
    resources_df = pd.read_csv(io.StringIO(resources_csv))

    with pytest.raises(MissingContactException) as e_info:
        process_contacts(contacts_df, resources_df)
    assert e_info.value.msg == "Cannot migrate resource due to missing email or consent for contact (Resource, first name, last name) = (R3, A3, B3), (R4, A4, B4)."

    resources_csv = ("""id,contact point.resource,contact point.first name,contact point.last name\n"""
                     """R1,R1,A1,B1\n"""
                     """R2,R2,A2,B2\n""")
    resources_df = pd.read_csv(io.StringIO(resources_csv))
    processed_contacts = process_contacts(contacts_df, resources_df)
    assert processed_contacts["mg_delete"].values.tolist() == [False, False, True, True]



def test_check_hricore():
    """Tests the `check_hricore` utility function."""
    resources_csv = (
        """hricore,id,name\n"""
        """true,A,A\n"""
        """false,B,B\n"""
        """,C,C"""
    )
    resources_df = pd.read_csv(io.StringIO(resources_csv))

    with pytest.raises(ValueError) as e:
        check_hricore(resources_df)
    assert str(e.value) == "Value 'hricore' not set to 'true' for resource B, C"

def test_add_resource():
    """Tests the `add_resource` method."""
    server_url = os.environ.get('MG_URL')
    token = os.environ.get('MG_TOKEN')

    with StagingMigrator(url=server_url, token=token, target=CATALOGUE) as migrator:
        migrator.set_source(STAGING_AREA)
        migrator.add_data_resource()

