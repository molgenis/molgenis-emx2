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
        schema_metadata = client.get_schema_metadata("pet store")
        assert len(schema_metadata.tables) == 5

def test_get_schema_settings():
    """Tests the `get_schema_settings` method."""
    with Client(url=server_url) as client:
        schema_settings = client.get_schema_settings("pet store")
        assert len(schema_settings) == 1
        assert len(schema_settings[0].values()) == 2

def test_get_schema_members():
    """Tests the `get_schema_members` method."""
    with Client(url=server_url) as client:
        with pytest.raises(PermissionDeniedException) as excinfo:
            schema_members = client.get_schema_members("pet store")
        assert excinfo.value.msg == "Cannot access members on this schema."

    with Client(url=server_url) as client:
        client.signin(username, password)
        client.set_schema("pet store")
        schema_members: list[dict] = client.get_schema_members()
        assert len(schema_members) == 5
        assert schema_members[0].get('email') == "anonymous"
        assert schema_members[0].get('role') == "Viewer"

def test_get_schema_roles():
    """Tests the `get_schema_roles` method."""

    with Client(url=server_url) as client:
        schema_roles = client.get_schema_roles("pet store")
        assert len(schema_roles) == 8
