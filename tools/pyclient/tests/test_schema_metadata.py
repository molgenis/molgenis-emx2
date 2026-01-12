"""
Tests for methods concerning schema metadata.
"""

import os
from pathlib import Path

import pytest
from dotenv import load_dotenv

from src.molgenis_emx2_pyclient import Client
from src.molgenis_emx2_pyclient.exceptions import NoSuchSchemaException, \
    NoSuchTableException

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
