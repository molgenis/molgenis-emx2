# noinspection PyProtectedMember
from unittest.mock import MagicMock

from molgenis_emx2.directory_client.model import (
    ExternalServerNode,
    Node,
    NodeData,
    Source,
    Table,
    TableType,
)


def test_table_type_order():
    assert TableType.get_import_order() == [
        TableType.PERSONS,
        TableType.ALSO_KNOWN,
        TableType.NETWORKS,
        TableType.BIOBANKS,
        TableType.SERVICES,
        TableType.STUDIES,
        TableType.COLLECTIONS,
        TableType.FACTS,
    ]


def test_table_type_base_ids():
    assert TableType.PERSONS.base_id == "Persons"
    assert TableType.ALSO_KNOWN.base_id == "AlsoKnownIn"
    assert TableType.NETWORKS.base_id == "Networks"
    assert TableType.BIOBANKS.base_id == "Biobanks"
    assert TableType.SERVICES.base_id == "Services"
    assert TableType.STUDIES.base_id == "Studies"
    assert TableType.COLLECTIONS.base_id == "Collections"
    assert TableType.FACTS.base_id == "CollectionFacts"


def test_table_factory_method():
    row1 = {"id": "1"}
    row2 = {"id": "2"}
    rows = [row1, row2]

    meta = MagicMock()
    meta.id_attribute = "id"

    table = Table.of(TableType.PERSONS, meta, rows)

    assert table.rows_by_id["2"] == row2
    assert table.rows[0] == row1
    assert table.rows[1] == row2


def test_node_staging_id():
    node = Node("NL", "NL", None)

    assert node.get_staging_id(TableType.PERSONS) == "Persons"
    assert node.get_staging_id(TableType.NETWORKS) == "Networks"
    assert node.get_staging_id(TableType.ALSO_KNOWN) == "AlsoKnownIn"
    assert node.get_staging_id(TableType.BIOBANKS) == "Biobanks"
    assert node.get_staging_id(TableType.SERVICES) == "Services"
    assert node.get_staging_id(TableType.STUDIES) == "Studies"
    assert node.get_staging_id(TableType.COLLECTIONS) == "Collections"
    assert node.get_staging_id(TableType.FACTS) == "CollectionFacts"


def test_node_id_prefix():
    node = Node("BE", "BE", None)

    assert node.get_id_prefix(TableType.PERSONS) == "bbmri-eric:contactID:BE_"
    assert node.get_id_prefix(TableType.NETWORKS) == "bbmri-eric:networkID:BE_"
    assert node.get_id_prefix(TableType.ALSO_KNOWN) == "bbmri-eric:akiID:BE_"
    assert node.get_id_prefix(TableType.BIOBANKS) == "bbmri-eric:ID:BE_"
    assert node.get_id_prefix(TableType.SERVICES) == "bbmri-eric:serviceID:BE_"
    assert node.get_id_prefix(TableType.STUDIES) == "bbmri-eric:studyID:BE_"
    assert node.get_id_prefix(TableType.COLLECTIONS) == "bbmri-eric:ID:BE_"
    assert node.get_id_prefix(TableType.FACTS) == "bbmri-eric:factID:BE_"


def test_node_eu_id_prefix():
    node = Node("BE", "BE", None)

    assert node.get_eu_id_prefix(TableType.PERSONS) == "bbmri-eric:contactID:EU_"
    assert node.get_eu_id_prefix(TableType.NETWORKS) == "bbmri-eric:networkID:EU_"
    assert node.get_eu_id_prefix(TableType.ALSO_KNOWN) == "bbmri-eric:akiID:EU_"
    assert node.get_eu_id_prefix(TableType.BIOBANKS) == "bbmri-eric:ID:EU_"
    assert node.get_eu_id_prefix(TableType.SERVICES) == "bbmri-eric:serviceID:EU_"
    assert node.get_eu_id_prefix(TableType.STUDIES) == "bbmri-eric:studyID:EU_"
    assert node.get_eu_id_prefix(TableType.COLLECTIONS) == "bbmri-eric:ID:EU_"
    assert node.get_eu_id_prefix(TableType.FACTS) == "bbmri-eric:factID:EU_"


def test_external_server_node():
    node = ExternalServerNode("NL", description="NL", date_end=None, url="test.nl")

    assert node.get_staging_id(TableType.PERSONS) == "Persons"
    assert node.url == "test.nl"


def test_node_data_order():
    meta = MagicMock()
    meta.id_attribute = "id"
    persons = Table.of(TableType.PERSONS, meta, [{"id": "1"}])
    networks = Table.of(TableType.NETWORKS, meta, [{"id": "1"}])
    also_known_in = Table.of(TableType.ALSO_KNOWN, meta, [{"id": "1"}])
    biobanks = Table.of(TableType.BIOBANKS, meta, [{"id": "1"}])
    services = Table.of(TableType.SERVICES, meta, [{"id": "1"}])
    studies = Table.of(TableType.STUDIES, meta, [{"id": "1"}])
    collections = Table.of(TableType.COLLECTIONS, meta, [{"id": "1"}])
    facts = Table.of(TableType.FACTS, meta, [{"id": "1"}])
    node = Node("NL", "NL")

    node_data = NodeData(
        node=node,
        source=Source.STAGING,
        persons=persons,
        networks=networks,
        also_known_in=also_known_in,
        biobanks=biobanks,
        services=services,
        studies=studies,
        collections=collections,
        facts=facts,
    )

    assert node_data.import_order == [
        persons,
        networks,
        also_known_in,
        biobanks,
        services,
        studies,
        collections,
        facts,
    ]
