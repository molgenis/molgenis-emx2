"""
Tests the Pyclient `get_graphql` method.
"""

import os
from pathlib import Path

import pytest
from dotenv import load_dotenv

from src.molgenis_emx2_pyclient import Client
from src.molgenis_emx2_pyclient.exceptions import NoSuchSchemaException, \
    NoSuchTableException, NoSuchColumnException

load_dotenv()
server_url = os.environ.get("MG_SERVER")
username = os.environ.get("MG_USERNAME")
password = os.environ.get("MG_PASSWORD")
token = os.environ.get("MOLGENIS_TOKEN")

RESOURCES_DIR = Path(__file__).parent / "resources"


def test_schema_fail():
    """Tests failing get by giving no/incorrect schema name."""
    with Client(url=server_url) as client:
        client.signin(username, password)
        with pytest.raises(NoSuchSchemaException) as excinfo:
            client.get_graphql(schema="Pet Store", table="Pet")
        assert excinfo.value.msg == "Schema 'Pet Store' not available."

def test_table_fail():
    """Tests failing get by giving no/incorrect table name."""
    with Client(url=server_url) as client:
        client.signin(username, password)
        with pytest.raises(NoSuchTableException) as excinfo:
            client.get_graphql(schema="pet store", table="Pets")
        assert excinfo.value.msg == "Table 'Pets' not found in schema 'pet store'."

def test_columns_fail():
    """Tests failing get by giving incorrect column names."""
    with Client(url=server_url) as client:
        client.signin(username, password)
        with pytest.raises(NoSuchColumnException) as excinfo:
            client.get_graphql(schema="pet store", table="Pet", columns=["Name"])
        assert excinfo.value.msg == "Columns 'Name' not found."
        with pytest.raises(NoSuchColumnException) as excinfo:
            client.get_graphql(schema="pet store", table="Pet", columns=["Name", "name"])
        assert excinfo.value.msg == "Columns 'Name' not found."
        with pytest.raises(NoSuchColumnException) as excinfo:
            client.get_graphql(schema="pet store", table="Pet", columns=["Name", "name2"])
        assert excinfo.value.msg == "Columns 'Name', 'name2' not found."

def test_columns_okay():
    """Tests get with specifying columns."""
    with Client(url=server_url) as client:
        client.signin(username, password)

        pets_list = client.get_graphql(schema="pet store", table="Pet", columns=["name", "weight", "orders"])
        assert list(pets_list[0].keys()) == ["name", "weight", "orders"]


def test_query_filter_fail():
    """Tests get fail with incorrect query filter."""

    with Client(url=server_url) as client:
        client.signin(username, password)
        with pytest.raises(ValueError) as excinfo:
            client.get_graphql(table="Pet", schema="pet store", query_filter="name")
        assert excinfo.value.args[0] == ("Cannot process statement 'name', ensure specifying one of the operators"
                                         " '==', '>', '<', '!=', 'between' in your statement.")

def test_equals_filter():
    """Tests the 'equals' filter for the query filter parameter."""

    with Client(url=server_url) as client:
        client.signin(username, password)

        # Test on string
        pets = client.get_graphql(table="Pet", schema="pet store", query_filter="name == 'Henry'")
        assert len(pets) == 0

        pets = client.get_graphql(table="Pet", schema="pet store", query_filter="name == 'pooky'")
        assert len(pets) == 1

        # Test int
        orders = client.get_graphql(table="Order", schema="pet store", query_filter="quantity == 7")
        assert len(orders) == 1

        # Test float
        pets = client.get_graphql(table="Pet", schema="pet store", query_filter="weight == 9.4")
        assert len(pets) == 1

        # Test ref
        pets = client.get_graphql(table="Pet", schema="pet store", query_filter="category.name == cat")
        assert len(pets) == 3

        # Test ontology
        pets = client.get_graphql(table="Pet", schema="pet store", query_filter="tags.name == red")
        assert len(pets) == 4



def test_greater_filter():
    """Tests the 'greater than' filter for the query filter parameter."""
    with Client(url=server_url) as client:
        client.signin(username, password)

        # Test int/long
        orders = client.get_graphql(table="Order", schema="pet store", query_filter="quantity > 5")
        assert len(orders) == 1

        # Test float
        pets = client.get_graphql(table="Pet", schema="pet store", query_filter="weight > 1.1")
        assert len(pets) == 4

def test_smaller_filter():
    """Tests the 'smaller than' filter for the query filter parameter."""
    with Client(url=server_url) as client:
        client.signin(username, password)

        # Test int/long
        orders = client.get_graphql(table="Order", schema="pet store", query_filter="quantity < 5")
        assert len(orders) == 1

        # Test float
        pets = client.get_graphql(table="Pet", schema="pet store", query_filter="weight < 1.1")
        assert len(pets) == 4

def test_not_equals_filter():
    """Tests the 'unequal' filter for the query filter parameter."""

    with Client(url=server_url) as client:
        client.signin(username, password)

        # Test boolean
        orders = client.get_graphql(table="Order", schema="pet store", query_filter="complete != True")
        assert len(orders) == 1

        # Test int/long
        orders = client.get_graphql(table="Order", schema="pet store", query_filter="quantity != 7")
        assert len(orders) == 1

        # Test float
        orders = client.get_graphql(table="Pet", schema="pet store", query_filter="weight != 1.337")
        assert len(orders) == 8

        # Test string
        pets = client.get_graphql(table="Pet", schema="pet store", query_filter="name != pooky")
        assert len(pets) == 8

        # Test ref
        with pytest.raises(NotImplementedError) as excinfo:
            client.get_graphql(table="Pet", schema="pet store", query_filter="category != cat")
        assert str(excinfo.value) == "The filter '!=' is not implemented for columns of type 'RADIO'."

        # Test ontology
        with pytest.raises(NotImplementedError) as excinfo:
            client.get_graphql(table="Pet", schema="pet store", query_filter="tags != 'red'")
        assert str(excinfo.value) == "The filter '!=' is not implemented for columns of type 'ONTOLOGY_ARRAY'."


def test_between_filter():
    """Tests the 'between' filter for the query filter parameter."""

    with Client(url=server_url) as client:
        client.signin(username, password)

        # Test int/long
        orders = client.get_graphql(table="Order", schema="pet store", query_filter="quantity between [5, 10]")
        assert len(orders) == 1

        # Test float
        orders = client.get_graphql(table="Pet", schema="pet store", query_filter="weight between [0, 2.5]")
        assert len(orders) == 5

        # Test string
        with pytest.raises(NotImplementedError) as excinfo:
            client.get_graphql(table="Pet", schema="pet store", query_filter="name between [0, 5]")
        assert str(excinfo.value) == "The filter 'between' is not implemented for columns of type 'STRING'."


def test_multiple_filters():
    """Tests the query filter parameter with multiple filters."""

    with Client(url=server_url) as client:
        client.signin(username, password)

        pets = client.get_graphql(table="Pet", schema="pet store",
                            query_filter="weight between [0, 2.5] and name != jerry")
        assert len(pets) == 4

        pets = client.get_graphql(table="Pet", schema="pet store",
                            query_filter="status == available and tags.name == red")
        assert len(pets) == 3
