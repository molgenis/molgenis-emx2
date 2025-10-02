"""
Tests for the Pyclient.
"""
import os

import pytest
from dotenv import load_dotenv

from tools.pyclient.src.molgenis_emx2_pyclient import Client
from tools.pyclient.src.molgenis_emx2_pyclient.exceptions import SigninError, SignoutError

load_dotenv()
server_url = os.environ.get("MG_SERVER")
username = os.environ.get("MG_USERNAME")
password = os.environ.get("MG_PASSWORD")

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
    ...

def test_get_schemas():
    """Tests the `get_schemas` method."""
    ...

def test_set_token():
    """Tests the `set_token` method."""
    ...

def test_save_schema():
    """Tests the `save_schema` method."""
    ...

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



