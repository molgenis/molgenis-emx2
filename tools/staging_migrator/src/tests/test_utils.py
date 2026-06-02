"""
Test functions for the Staging Migrator utility functions.
"""
import io
import pandas as pd
import pytest

from staging_migrator.src.molgenis_emx2_staging_migrator.exceptions import MissingContactException, \
    MissingHRICoreException, DraftException
from staging_migrator.src.molgenis_emx2_staging_migrator.utils import process_contacts, check_hricore, check_draft


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

    with pytest.raises(MissingHRICoreException) as e:
        check_hricore(resources_df, "UMCGCohortsStaging")
    assert str(e.value) == "Message: Value 'hricore' not set to 'Yes' for resource 'B, C'\n"

def test_check_draft():
    """Tests the `check_draft` utility function."""
    resources_csv = (
        """hricore,id,name,mg_draft\n"""
        """true,A,A,\n"""
        """false,B,B,false\n"""
        """,C,C,true"""
    )
    resources_df = pd.read_csv(io.StringIO(resources_csv))
    with pytest.raises(DraftException) as e:
        check_draft(resources_df, "Resources")
    assert str(e.value) == "Message: Table 'Resources' contains 1 draft record.\n"
