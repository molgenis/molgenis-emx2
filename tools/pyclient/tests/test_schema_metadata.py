"""
Tests for methods concerning schema metadata.
"""

import os

import pytest
from dotenv import load_dotenv

from src.molgenis_emx2_pyclient import Client
from src.molgenis_emx2_pyclient.exceptions import PermissionDeniedException

load_dotenv()
server_url = os.environ.get("MG_SERVER")
username = os.environ.get("MG_USERNAME")
password = os.environ.get("MG_PASSWORD")

def test_get_schema_metadata():
    """Tests the `get_schema_metadata` method."""
    with Client(url=server_url) as client:
        client.set_schema("catalogue-demo")
        schema_metadata = client.get_schema_metadata()
        assert len(schema_metadata.tables) == 24

def test_get_schema_settings():
    """Tests the `get_schema_settings` method."""
    with Client(url=server_url) as client:
        client.set_schema("catalogue-demo")
        schema_settings = client.get_schema_settings()
        assert len(schema_settings) == 2

def test_get_schema_members():
    """Tests the `get_schema_members` method."""
    with Client(url=server_url) as client:
        client.set_schema("catalogue-demo")
        with pytest.raises(PermissionDeniedException) as excinfo:
            schema_members = client.get_schema_members()
        assert excinfo.value.msg == "Cannot access members on this schema."

    with Client(url=server_url) as client:
        client.signin(username, password)
        client.set_schema("catalogue-demo")
        schema_members: list[dict] = client.get_schema_members()
        assert len(schema_members) == 1
        assert schema_members[0].get('email') == "anonymous"
        assert schema_members[0].get('role') == "Viewer"

def test_get_schema_roles():
    """Tests the `get_schema_roles` method."""

    with Client(url=server_url) as client:
        client.set_schema("catalogue-demo")
        schema_roles = client.get_schema_roles()
        assert len(schema_roles) == 8
