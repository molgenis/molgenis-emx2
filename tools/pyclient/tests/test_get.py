"""
Tests the Pyclient `get` method.
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
token = os.environ.get("MOLGENIS_TOKEN")


def test_catalogue_demo_resources():
    """Tests the `get` method on the Resources table in the catalogue-demo schema."""
    with Client(url=server_url) as client:
        client.set_schema("catalogue-demo")
        client.get("Resources")

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

def test_table_signed_in_okay():
    """Tests getting information from `get` while signed in."""
    with Client(url=server_url) as client:
        client.signin(username, password)
        pets = client.get(schema="pet store", table="Pet")

        assert len(pets) == 9

def test_table_signed_out_okay():
    """Tests getting information from `get` while signed in."""
    with Client(url=server_url) as client:
        pets = client.get(schema="pet store", table="Pet")

        assert len(pets) == 9

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

    with Client(url=server_url) as client:
        client.signin(username, password)

        # Test on string
        pets = client.get(table="Pet", schema="pet store", query_filter="name == 'Henry'")
        assert len(pets) == 0

        pets = client.get(table="Pet", schema="pet store", query_filter="name == 'pooky'")
        assert len(pets) == 1

        # Test int
        orders = client.get(table="Order", schema="pet store", query_filter="quantity == 7")
        assert len(orders) == 1

        # Test float
        pets = client.get(table="Pet", schema="pet store", query_filter="weight == 9.4")
        assert len(pets) == 1

        # Test ref
        pets = client.get(table="Pet", schema="pet store", query_filter="category.name == cat")
        assert len(pets) == 3

        # Test ontology
        pets = client.get(table="Pet", schema="pet store", query_filter="tags.name == red")
        assert len(pets) == 4


def test_multiple_filters():
    """Tests the query filter parameter with multiple filters."""

    with Client(url=server_url) as client:
        client.signin(username, password)

        pets = client.get(table="Pet", schema="pet store",
                            query_filter="weight between [0, 2.5] and name != jerry")
        assert len(pets) == 4

        pets = client.get(table="Pet", schema="pet store",
                            query_filter="status == available and tags.name == red")
        assert len(pets) == 3
