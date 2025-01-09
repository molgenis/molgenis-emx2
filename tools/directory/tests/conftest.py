import json
import os
from importlib import resources
from typing import List
from unittest.mock import AsyncMock, MagicMock

import pytest

from molgenis_emx2.directory_client.model import (
    Node,
    NodeData,
    Source,
    Table,
    TableType,
)


def get_data(table_type) -> List[dict]:
    file_name = table_type + ".json"
    file = open(str(resources.files("tests.resources") / file_name), "r")

    data = json.load(file)
    file.close()
    return data


@pytest.fixture
def node_data() -> NodeData:
    """
    Reads json files with the six data sources and
    returns NodeData to test with.
    """
    persons_meta = MagicMock()
    persons_meta.id = "eu_bbmri_eric_NL_persons"
    persons_meta.id_attribute = "id"
    persons = Table.of(
        TableType.PERSONS,
        persons_meta,
        get_data("persons"),
    )

    networks_meta = MagicMock()
    networks_meta.id = "eu_bbmri_eric_NL_networks"
    networks_meta.id_attribute = "id"
    networks = Table.of(
        TableType.NETWORKS,
        networks_meta,
        get_data("networks"),
    )

    also_known_meta = MagicMock()
    also_known_meta.id = "eu_bbmri_eric_NL_also_known_in"
    also_known_meta.id_attribute = "id"
    also_known = Table.of(
        TableType.ALSO_KNOWN,
        also_known_meta,
        get_data("also_known"),
    )

    biobanks_meta = MagicMock()
    biobanks_meta.id = "eu_bbmri_eric_NL_biobanks"
    biobanks_meta.id_attribute = "id"
    biobanks_meta.hyperlinks = ["url"]
    biobanks = Table.of(
        TableType.BIOBANKS,
        biobanks_meta,
        get_data("biobanks"),
    )

    collection_meta = MagicMock()
    collection_meta.id = "eu_bbmri_eric_NL_collections"
    collection_meta.id_attribute = "id"
    collections = Table.of(
        TableType.COLLECTIONS,
        collection_meta,
        get_data("collections"),
    )

    facts_meta = MagicMock()
    facts_meta.id = "eu_bbmri_eric_NL_facts"
    facts_meta.id_attribute = "id"
    facts = Table.of(
        TableType.FACTS,
        facts_meta,
        get_data("facts"),
    )

    return NodeData.from_dict(
        Node("NL", "NL"),
        Source.STAGING,
        {
            TableType.PERSONS.value: persons,
            TableType.NETWORKS.value: networks,
            TableType.ALSO_KNOWN.value: also_known,
            TableType.BIOBANKS.value: biobanks,
            TableType.COLLECTIONS.value: collections,
            TableType.FACTS.value: facts,
        },
    )


@pytest.fixture
def async_session() -> AsyncMock:
    session = AsyncMock()
    session.url = "url"
    session.directory_schema = "BBMRI-ERIC"
    return session


@pytest.fixture
def session() -> MagicMock:
    session = MagicMock()
    session.url = "url"
    return session


@pytest.fixture
def printer() -> MagicMock:
    return MagicMock()


@pytest.fixture
def pid_service() -> MagicMock:
    service = MagicMock()
    service.base_url = "url/"
    return service


os.environ["SCHEMA_PREFIX"] = "BBMRI"
