"""
Tests the Pyclient `save_table` method.
"""

import os
from pathlib import Path

import pandas as pd
import pytest
from dotenv import load_dotenv

from src.molgenis_emx2_pyclient import Client
from src.molgenis_emx2_pyclient.exceptions import NoSuchSchemaException, \
    NoSuchTableException

load_dotenv()
server_url = os.environ.get("MG_SERVER")
username = os.environ.get("MG_USERNAME")
password = os.environ.get("MG_PASSWORD")

RESOURCES_DIR = Path(__file__).parent / "resources"


def test_schema_fail():
    """Tests failing upload by giving incorrect schema name."""
    with Client(url=server_url) as client:
        client.signin(username, password)
        with pytest.raises(NoSuchSchemaException) as excinfo:
            client.save_table(schema="Pet store", table="Pet",
                               file=RESOURCES_DIR / "insert" / "Pet.csv")
        assert excinfo.value.msg == "Schema 'Pet store' not available."

def test_table_fail():
    """Tests failing upload by giving incorrect table name."""
    with Client(url=server_url) as client:
        client.signin(username, password)

        # Test failing table name
        with pytest.raises(NoSuchTableException) as excinfo:
            client.save_table(schema="pet store", table="Pets",
                               file=RESOURCES_DIR / "insert" / "Pet.csv")
        assert excinfo.value.msg == "Table 'Pets' not found in schema 'pet store'."

def test_missing_file():
    """Tests failing upload by not supplying file or data."""
    with Client(url=server_url) as client:
        client.signin(username, password)

        # Test missing file and data
        with pytest.raises(FileNotFoundError) as excinfo:
            client.save_table(schema="pet store", table="Pet")

        assert str(excinfo.value) == "No data to import. Specify a file location or a dataset."

def test_incorrect_file_name():
    """Tests failing upload by giving incorrect file name."""
    with Client(url=server_url) as client:
        client.signin(username, password)

        # Test file upload incorrect name
        with pytest.raises(FileNotFoundError) as excinfo:
            client.save_table(schema="pet store", table="Pet",
                               file=RESOURCES_DIR / "insert" / "Pat.csv")

        assert excinfo.value.args[1] == "No such file or directory"
        assert str(excinfo.value.filename).endswith(str(RESOURCES_DIR / "insert" / "Pat.csv"))

def test_upload_file():
    """Tests uploading files."""
    with Client(url=server_url) as client:
        client.signin(username, password)

        # Get the number of records before
        pet_before = len(client.get_graphql(schema="pet store", table="Pet", columns=["name"]))
        tag_before = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))

        client.save_table(schema="pet store", table="Tag",
                           file=RESOURCES_DIR / "insert" / "Tag.csv")
        client.save_table(schema="pet store", table="Pet",
                           file=RESOURCES_DIR / "insert" / "Pet.csv")

        # Number of records between
        pet_between = len(client.get_graphql(schema="pet store", table="Pet", columns=["name"]))
        tag_between = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))

        assert pet_between == pet_before + 2
        assert tag_between == tag_before + 2

        client.save_table(schema="pet store", table="Pet",
                           file=RESOURCES_DIR / "delete" / "Pet.csv")
        client.save_table(schema="pet store", table="Tag",
                           file=RESOURCES_DIR / "delete" / "Tag.csv")

        # Number of records after
        pet_after = len(client.get_graphql(schema="pet store", table="Pet", columns=["name"]))
        tag_after = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))

        assert pet_after == pet_before
        assert tag_after == tag_before

def test_save_upload_list_data():
    """Tests uploading data in list format."""
    with Client(url=server_url) as client:
        client.signin(username, password)

        # Get the number of records before
        pet_before = len(client.get_graphql(schema="pet store", table="Pet", columns=["name"]))
        tag_before = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))

        tag_insert = pd.read_csv(RESOURCES_DIR / "insert" / "Tag.csv").to_dict(orient='index').values()
        pet_insert = pd.read_csv(RESOURCES_DIR / "insert" / "Pet.csv", keep_default_na=False).to_dict(orient='index').values()

        client.save_table(schema="pet store", table="Tag",
                           data=list(tag_insert))
        client.save_table(schema="pet store", table="Pet",
                           data=list(pet_insert))

        # Number of records between
        pet_between = len(client.get_graphql(schema="pet store", table="Pet", columns=["name"]))
        tag_between = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))

        assert pet_between == pet_before + 2
        assert tag_between == tag_before + 2

        tag_delete = pd.read_csv(RESOURCES_DIR / "delete" / "Tag.csv").to_dict(orient='index').values()
        pet_delete = pd.read_csv(RESOURCES_DIR / "delete" / "Pet.csv", keep_default_na=False).to_dict(orient='index').values()

        client.save_table(schema="pet store", table="Pet",
                           data=list(pet_delete))
        client.save_table(schema="pet store", table="Tag",
                           data=list(tag_delete))

        # Number of records after
        pet_after = len(client.get_graphql(schema="pet store", table="Pet", columns=["name"]))
        tag_after = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))

        assert pet_after == pet_before
        assert tag_after == tag_before

def test_save_upload_pandas():
    """Tests uploading data as pandas DataFrame."""
    with Client(url=server_url) as client:
        client.signin(username, password)

        # Get the number of records before
        pet_before = len(client.get_graphql(schema="pet store", table="Pet", columns=["name"]))
        tag_before = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))

        tag_insert = pd.read_csv(RESOURCES_DIR / "insert" / "Tag.csv")
        pet_insert = pd.read_csv(RESOURCES_DIR / "insert" / "Pet.csv", keep_default_na=False)

        client.save_table(schema="pet store", table="Tag",
                           data=tag_insert)
        client.save_table(schema="pet store", table="Pet",
                           data=pet_insert)

        # Number of records between
        pet_between = len(client.get_graphql(schema="pet store", table="Pet", columns=["name"]))
        tag_between = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))

        assert pet_between == pet_before + 2
        assert tag_between == tag_before + 2

        tag_delete = pd.read_csv(RESOURCES_DIR / "delete" / "Tag.csv")
        pet_delete = pd.read_csv(RESOURCES_DIR / "delete" / "Pet.csv", keep_default_na=False)

        client.save_table(schema="pet store", table="Pet",
                           data=pet_delete)
        client.save_table(schema="pet store", table="Tag",
                           data=tag_delete)

        # Number of records after
        pet_after = len(client.get_graphql(schema="pet store", table="Pet", columns=["name"]))
        tag_after = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))

        assert pet_after == pet_before
        assert tag_after == tag_before
