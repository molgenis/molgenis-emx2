"""
Tests for the Pyclient.
"""
import os
from pathlib import Path

import pytest
from dotenv import load_dotenv

from tools.pyclient.src.molgenis_emx2_pyclient import Client
from tools.pyclient.src.molgenis_emx2_pyclient.exceptions import SigninError, SignoutError, NoSuchSchemaException, \
    NoSuchTableException

load_dotenv()
server_url = os.environ.get("MG_SERVER")
username = os.environ.get("MG_USERNAME")
password = os.environ.get("MG_PASSWORD")

RESOURCES_DIR = Path(__file__).parent / "resources"

def test_signin():
    """Tests the `signin` method."""
    with pytest.raises(SigninError) as excinfo:
        with Client(url=server_url) as client:
            client.signin(username+username, password)
    assert excinfo.value.msg.endswith("Sign in as 'adminadmin' failed: user or password unknown")

    with Client(url=server_url) as client:
        client.signin(username, password)

        assert client.signin_status == "success"


def test_signout():
    """Tests the `signout` method."""
    with Client(url=server_url) as client:
        with pytest.raises(SignoutError) as excinfo:
            client.signout()
        assert excinfo.value.msg == "Could not sign out as user is not signed in."

        client.signin(username, password)
        client.signout()
        assert client.signin_status == "signed out"


def test_status():
    """Tests the `status` property."""
    with Client(url=server_url) as client:
        status: str = client.status
    assert status.split("Host: ")[-1].startswith(server_url)
    assert status.split("User: ")[-1].startswith("anonymous")
    assert status.split("Status: ")[-1].startswith("signed out")
    assert "pet store" in status.split("Schemas: ")[-1]


def test_get_schemas():
    """Tests the `get_schemas` method."""
    with Client(url=server_url) as client:
        schemas = client.get_schemas()
    assert "pet store" in map(lambda schema: schema.id, schemas)


def test_set_token():
    """Tests the `set_token` method."""
    token = "SAMPLE TOKEN"
    with Client(url=server_url) as client:
        client.set_token(token)

        assert client._token == token


def test_save_schema():
    """Tests the `save_schema` method."""
    # Test failing table name
    with Client(url=server_url) as client:
        client.signin(username, password)

        # Test failing schema name
        with pytest.raises(NoSuchSchemaException) as excinfo:
            client.save_schema(name="pat store", table="Pet",
                               file=str(RESOURCES_DIR / "Pet.csv"))
        assert excinfo.value.msg == "Schema 'pat store' not available."

        # Test failing table name
        with pytest.raises(NoSuchTableException) as excinfo:
            client.save_schema(name="pyt store", table="Pat",
                               file=str(RESOURCES_DIR / "Pet.csv"))
        assert excinfo.value.msg == "Table 'Pat' not found in schema 'pyt store'."

        # Test missing file and data
        with pytest.raises(FileNotFoundError) as excinfo:
            client.save_schema(name="pyt store", table="Pet")

        assert str(excinfo.value) == "No data to import. Specify a file location or a dataset."

        # Test file upload incorrect name
        with pytest.raises(FileNotFoundError) as excinfo:
            client.save_schema(name="pyt store", table="Pet",
                               file=str(RESOURCES_DIR / "Pat.csv"))

        assert excinfo.value.args[1] == "No such file or directory"
        assert str(excinfo.value.filename).endswith(str(RESOURCES_DIR / "Pat.csv"))

        # Test file upload
        client.save_schema(name="pyt store", table="Tag",
                           file=str(RESOURCES_DIR / "Tag.csv"))
        client.save_schema(name="pyt store", table="Pet",
                           file=str(RESOURCES_DIR / "Pet.csv"))

        client.save_schema(name="pyt store", table="Pet",
                           file=str(RESOURCES_DIR / "Pet_delete.csv"))
        client.save_schema(name="pyt store", table="Tag",
                           file=str(RESOURCES_DIR / "Tag_delete.csv"))
        assert 3 < 2

        # Test data upload as list

        # Test data upload as DataFrame


def test_upload_file():
    """Tests the `upload_file` method."""
    ...

def test_truncate():
    """Tests the `truncate` method."""
    ...

def test_upload_csv():
    """Tests the `_upload_csv` method."""
    ...

def test_delete_records():
    """Tests the `delete_records` method."""
    ...

def test_get():
    """Tests the `get` method."""
    ...

def test_get_graphql():
    """Tests the `get_graphql` method."""
    ...

def test_export():
    """Tests the `export` method."""
    ...

def test_create_schema():
    """Tests the `create_schema` method."""
    ...

def test_delete_schema():
    """Tests the `delete_schema` method."""
    ...

def test_update_schema():
    """Tests the `update_schema` method."""
    ...

def test_recreate_schema():
    """Tests the `recreate_schema` method."""
    ...

def test_get_schema_metadata():
    """Tests the `get_schema_metadata` method."""
    ...

def test_prepare_filter():
    """Tests the `_prepare_filter` method."""
    ...

def test_set_schema():
    """Tests the `set_schema` method."""
    ...

def test_report_task_progress():
    """Tests the `_report_task_progress` method."""
    ...

def test_validate_graphql_response():
    """Tests the `_validate_graphql_response` method."""
    ...



