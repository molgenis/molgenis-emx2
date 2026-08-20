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
    PermissionDeniedException, PyclientException

load_dotenv()
server_url = os.environ.get("MG_SERVER", "http://localhost:8080/")
username = os.environ.get("MG_USERNAME", "admin")
password = os.environ.get("MG_PASSWORD", "admin")

RESOURCES_DIR = Path(__file__).parent / "resources"

def test_delete_records():
    """Tests the `delete_records` method."""

    # Test fail without editor rights
    with Client(url=server_url) as client:
        with pytest.raises(PermissionDeniedException) as exc_info:
            client.delete_records(table="Pet", schema="pet store", file=RESOURCES_DIR / "insert" / "Pet.csv")
        assert str(exc_info.value) == "Message: Transaction failed: permission denied.\n"

        client.signin(username, password)

        # Test fail without schema
        with pytest.raises(NoSuchSchemaException) as exc_info:
            client.delete_records(table="Pet", file=RESOURCES_DIR / "insert" / "Pet.csv")

        assert exc_info.value.msg == "Select an existing schema for this operation."

        # Test fail without specifying file or data
        with pytest.raises(FileNotFoundError) as exc_info:
            client.delete_records(schema="pet store" , table="Pet")

        assert str(exc_info.value) == "No data to import. Specify a file location or a dataset."

def test_format_fail():
    with Client(url=server_url) as client:
        client.signin(username, password)
        with pytest.raises(ValueError) as exc_info:
            client.delete_records(table="Pet", schema="pet store", data=["pooky"])
        assert str(exc_info.value) == "Cannot prepare row 'pooky'. Supply a list of dictionaries."
        client.delete_records(table="Pet", schema="pet store", data=[{"id": "pooky"}])


def test_delete_with_file():

    with Client(url=server_url) as client:
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