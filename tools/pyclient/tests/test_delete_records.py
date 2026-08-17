"""
Tests the Pyclient `delete_records` method.
"""

import os
from pathlib import Path

import pandas as pd
import pytest
from dotenv import load_dotenv

from src.molgenis_emx2_pyclient import Client
from src.molgenis_emx2_pyclient.exceptions import NoSuchSchemaException, \
    PermissionDeniedException

load_dotenv()
server_url = os.environ.get("MG_SERVER")
username = os.environ.get("MG_USERNAME")
password = os.environ.get("MG_PASSWORD")

RESOURCES_DIR = Path(__file__).parent / "resources"

def test_delete_records():
    """Tests the `delete_records` method."""

    # Test fail without editor rights
    with Client(url=server_url) as client:
        with pytest.raises(PermissionDeniedException) as excinfo:
            client.delete_records(table="Pet", schema="pet store", file=RESOURCES_DIR / "insert" / "Pet.csv")
        assert str(excinfo.value) == "Message: Transaction failed: permission denied.\n"

        client.signin(username, password)

        # Test fail without schema
        with pytest.raises(NoSuchSchemaException) as excinfo:
            client.delete_records(table="Pet", file=RESOURCES_DIR / "insert" / "Pet.csv")

        assert excinfo.value.msg == "Select an existing schema for this operation."

        # Test fail without specifying file or data
        with pytest.raises(FileNotFoundError) as excinfo:
            client.delete_records(schema="pet store" , table="Pet")

        assert str(excinfo.value) == "No data to import. Specify a file location or a dataset."

        # Test delete with file
        tag_before = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))
        client.save_table(table="Tag", schema="pet store", file=RESOURCES_DIR / "insert" / "Tag.csv")
        tag_between = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))

        assert tag_between == tag_before + 2
        client.delete_records(schema="pet store" , table="Tag", file=RESOURCES_DIR / "insert" / "Tag.csv")

        tag_after = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))
        assert tag_after == tag_before

        # Test delete with data as list
        tags_df = pd.read_csv(RESOURCES_DIR / "insert" / "Tag.csv")
        tags_list = list(tags_df.to_dict(orient='index').values())

        tag_before = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))
        client.save_table(table="Tag", schema="pet store", file=RESOURCES_DIR / "insert" / "Tag.csv")
        tag_between = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))

        assert tag_between == tag_before + 2
        client.delete_records(schema="pet store" , table="Tag", data=tags_list)

        tag_after = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))
        assert tag_after == tag_before

        # Test delete with data as DataFrame
        tag_before = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))
        client.save_table(table="Tag", schema="pet store", file=RESOURCES_DIR / "insert" / "Tag.csv")
        tag_between = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))

        assert tag_between == tag_before + 2
        client.delete_records(schema="pet store" , table="Tag", data=tags_df)

        tag_after = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))
        assert tag_after == tag_before

