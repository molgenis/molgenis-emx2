"""
Tests the Pyclient `save_schema` method.
"""

import os
from pathlib import Path

import pytest
from dotenv import load_dotenv

from src.molgenis_emx2_pyclient import Client
from src.molgenis_emx2_pyclient.exceptions import SigninError, SignoutError, NoSuchSchemaException, \
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
            client.save_schema(name="Pet store", table="Pet",
                               file=str(RESOURCES_DIR / "insert" / "Pet.csv"))
        assert excinfo.value.msg == "Schema 'Pet store' not available."

def test_table_fail():
    """Tests failing upload by giving incorrect table name."""
    with Client(url=server_url) as client:
        client.signin(username, password)

        # Test failing table name
        with pytest.raises(NoSuchTableException) as excinfo:
            client.save_schema(name="pet store", table="Pets",
                               file=str(RESOURCES_DIR / "insert" / "Pet.csv"))
        assert excinfo.value.msg == "Table 'Pets' not found in schema 'pet store'."



def test_save_schema():
    """Tests the `save_schema` method."""
    # Test failing table name
    with Client(url=server_url) as client:
        client.signin(username, password)


        # Test failing table name
        with pytest.raises(NoSuchTableException) as excinfo:
            client.save_schema(name="pet store", table="Pat",
                               file=str(RESOURCES_DIR / "insert" / "Pet.csv"))
        assert excinfo.value.msg == "Table 'Pat' not found in schema 'pet store'."

        # Test missing file and data
        with pytest.raises(FileNotFoundError) as excinfo:
            client.save_schema(name="pet store", table="Pet")

        assert str(excinfo.value) == "No data to import. Specify a file location or a dataset."

        # Test file upload incorrect name
        with pytest.raises(FileNotFoundError) as excinfo:
            client.save_schema(name="pet store", table="Pet",
                               file=str(RESOURCES_DIR / "insert" / "Pat.csv"))

        assert excinfo.value.args[1] == "No such file or directory"
        assert str(excinfo.value.filename).endswith(str(RESOURCES_DIR / "insert" / "Pat.csv"))

        # Test file upload
        # Get the number of records before
        pet_before = len(client.get_graphql(schema="pet store", table="Pet", columns=["name"]))
        tag_before = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))

        client.save_schema(name="pet store", table="Tag",
                           file=str(RESOURCES_DIR / "insert" / "Tag.csv"))
        client.save_schema(name="pet store", table="Pet",
                           file=str(RESOURCES_DIR / "insert" / "Pet.csv"))

        # Number of records between
        pet_between = len(client.get_graphql(schema="pet store", table="Pet", columns=["name"]))
        tag_between = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))

        assert pet_between == pet_before + 2
        assert tag_between == tag_before + 2

        client.save_schema(name="pet store", table="Pet",
                           file=str(RESOURCES_DIR / "delete" / "Pet.csv"))
        client.save_schema(name="pet store", table="Tag",
                           file=str(RESOURCES_DIR / "delete" / "Tag.csv"))

        # Number of records after
        pet_after = len(client.get_graphql(schema="pet store", table="Pet", columns=["name"]))
        tag_after = len(client.get_graphql(schema="pet store", table="Tag", columns=["name"]))

        assert pet_after == pet_before
        assert tag_after == tag_before

        assert 3 < 2

        # Test data upload as list

        # Test data upload as DataFrame