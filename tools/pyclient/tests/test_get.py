"""
Tests the Pyclient `get` method.
"""

import os
from pathlib import Path

import pandas as pd
import pytest
from dotenv import load_dotenv

from src.molgenis_emx2_pyclient import Client
from src.molgenis_emx2_pyclient.exceptions import NoSuchSchemaException, \
    NoSuchTableException, NoSuchColumnException

load_dotenv()
server_url = os.environ.get("MG_SERVER")
username = os.environ.get("MG_USERNAME")
password = os.environ.get("MG_PASSWORD")

RESOURCES_DIR = Path(__file__).parent / "resources"


def test_schema_fail():
    """Tests failing get by giving no/incorrect schema name."""
    with Client(url=server_url) as client:
        client.signin(username, password)
        with pytest.raises(NoSuchSchemaException) as excinfo:
            client.get(schema="Pet Store", table="Pet")
        assert excinfo.value.msg == "Schema 'Pet Store' not available."

def test_table_fail():
    """Tests failing get by giving no/incorrect table name."""
    with Client(url=server_url) as client:
        client.signin(username, password)
        with pytest.raises(NoSuchTableException) as excinfo:
            client.get(schema="pet store", table="Pets")
        assert excinfo.value.msg == "Table 'Pets' not found in schema 'pet store'."

def test_columns_fail():
    """Tests failing get by giving incorrect column names."""
    with Client(url=server_url) as client:
        client.signin(username, password)
        with pytest.raises(NoSuchColumnException) as excinfo:
            client.get(schema="pet store", table="Pet", columns=["Name"])
        assert excinfo.value.msg == "Columns 'Name' not found."
        with pytest.raises(NoSuchColumnException) as excinfo:
            client.get(schema="pet store", table="Pet", columns=["Name", "name"])
        assert excinfo.value.msg == "Columns ['Name'] not in index"
        with pytest.raises(NoSuchColumnException) as excinfo:
            client.get(schema="pet store", table="Pet", columns=["Name", "name2"])
        assert excinfo.value.msg == "Columns 'Name', 'name2' not found."

def test_columns_okay():
    """Tests get with specifying columns."""
    with Client(url=server_url) as client:
        client.signin(username, password)

        pets_list = client.get(schema="pet store", table="Pet", columns=["name", "weight", "orders"])
        assert list(pets_list[0].keys()) == ["name", "weight", "orders"]

        pets_df = client.get(schema="pet store", table="Pet", columns=["name", "weight", "orders"], as_df=True)
        assert list(pets_df.columns) == ["name", "weight", "orders"]


def test_query_filter_fail():
    """Tests get fail with incorrect query filter."""

    with Client(url=server_url) as client:
        client.signin(username, password)
        with pytest.raises(ValueError) as excinfo:
            client.get(table="Pet", schema="pet store", query_filter="name")
        assert excinfo.value.args[0] == ("Cannot process statement 'name', ensure specifying one of the operators"
                                         " '==', '>', '<', '!=', 'between' in your statement.")

def test_equals_filter():
    """Tests the 'equals' filter for the query filter parameter."""
    ...

def test_greater_filter():
    """Tests the 'greater than' filter for the query filter parameter."""
    ...

def test_smaller_filter():
    """Tests the 'smaller than' filter for the query filter parameter."""
    ...

def test_unequal_filter():
    """Tests the 'unequal' filter for the query filter parameter."""
    ...

def test_between_filter():
    """Tests the 'between' filter for the query filter parameter."""
    ...

def test_multiple_filters():
    """Tests the query filter parameter with multiple filters."""
    ...

def test_as_df():
    """Tests the method where the results are returned as pandas DataFrame."""
    ...
