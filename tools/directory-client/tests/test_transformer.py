from unittest.mock import MagicMock, call

import pytest

from molgenis.bbmri_eric.errors import EricWarning
from molgenis.bbmri_eric.model import Node, QualityInfo, Table, TableType
from molgenis.bbmri_eric.transformer import Transformer


@pytest.fixture
def transformer():
    return Transformer(
        node_data=MagicMock(),
        quality=MagicMock(),
        printer=MagicMock(),
        existing_biobanks=MagicMock(),
        eu_node_data=MagicMock(),
        diseases=MagicMock(),
    )


def test_transformer_node_codes(node_data, transformer):
    for table in node_data.import_order:
        assert "national_node" not in table.rows[0]

    transformer.node_data = node_data
    transformer._set_national_node_code()

    for table in node_data.import_order:
        assert table.rows[0]["national_node"] == "NL"


def test_transformer_not_withdrawn(node_data, transformer):
    for table in node_data.import_order:
        assert "withdrawn" not in table.rows[0]

    transformer.node_data = node_data
    transformer._set_withdrawn()

    for table in node_data.import_order:
        assert "withdrawn" not in table.rows[0]


def test_transformer_withdrawn(node_data, transformer):
    node_data.node = Node("NL", "NL", "20200101")
    for table in node_data.import_order:
        assert "withdrawn" not in table.rows[0]

    transformer.node_data = node_data
    transformer._set_withdrawn()

    for table in node_data.import_order:
        assert table.rows[0]["withdrawn"]


def test_transformer_commercial_use(transformer):
    node_data = MagicMock()
    node_data.collections.rows = [
        {"biobank": "biobank1", "collaboration_commercial": True},
        {"biobank": "biobank1", "collaboration_commercial": False},
        {"biobank": "biobank1"},
        {"biobank": "biobank2", "collaboration_commercial": True},
        {"biobank": "biobank2", "collaboration_commercial": False},
        {"biobank": "biobank2"},
        {"biobank": "biobank3", "collaboration_commercial": True},
        {"biobank": "biobank3", "collaboration_commercial": False},
        {"biobank": "biobank3"},
    ]
    node_data.biobanks.rows_by_id = {
        "biobank1": dict(),
        "biobank2": {"collaboration_commercial": True},
        "biobank3": {"collaboration_commercial": False},
    }

    transformer.node_data = node_data
    transformer._set_commercial_use_bool()

    assert node_data.collections.rows[0]["commercial_use"] is True
    assert node_data.collections.rows[1]["commercial_use"] is False
    assert node_data.collections.rows[2]["commercial_use"] is True
    assert node_data.collections.rows[3]["commercial_use"] is True
    assert node_data.collections.rows[4]["commercial_use"] is False
    assert node_data.collections.rows[5]["commercial_use"] is True
    assert node_data.collections.rows[6]["commercial_use"] is False
    assert node_data.collections.rows[7]["commercial_use"] is False
    assert node_data.collections.rows[8]["commercial_use"] is False


def test_transformer_quality(node_data, transformer):
    q_info = QualityInfo(
        biobanks={
            "bbmri-eric:ID:NL_test_quality_biobank1": ["quality1", "quality2"],
            "bbmri-eric:ID:NL_test_quality_biobank2": ["quality3"],
        },
        biobank_levels={},
        collections={"bbmri-eric:ID:NL_bb1:collection:test_quality1": ["quality1"]},
        collection_levels={},
    )
    transformer.node_data = node_data
    transformer.quality = q_info

    transformer._set_quality_info()

    assert node_data.biobanks.rows_by_id["bbmri-eric:ID:NL_test_quality_biobank1"][
        "quality"
    ] == ["quality1", "quality2"]
    assert (
        "quality"
        not in node_data.biobanks.rows_by_id["bbmri-eric:ID:NL_biobank_noQual"]
    )
    assert node_data.biobanks.rows_by_id["bbmri-eric:ID:NL_test_quality_biobank2"][
        "quality"
    ] == ["quality3"]
    assert node_data.collections.rows_by_id[
        "bbmri-eric:ID:NL_bb1:collection:test_quality1"
    ]["quality"] == ["quality1"]
    assert (
        "quality"
        not in node_data.collections.rows_by_id[
            "bbmri-eric:ID:NL_bb1:collection:test_noQual"
        ]
    )


def test_transformer_replace_eu_rows_skip_eu(transformer):
    eu = Node("EU", "Europe", None)
    node_data = MagicMock()
    node_data.node = eu
    transformer.node_data = node_data
    transformer._replace_rows = MagicMock()

    transformer._replace_eu_rows()

    transformer._replace_rows.assert_not_called()


def test_transformer_replace_eu_rows(transformer):
    cy = Node("CY", "Cyprus", None)
    eu = Node("EU", "Europe", None)

    node_data = MagicMock()
    eu_node_data = MagicMock()

    persons_meta = MagicMock()
    persons = Table.of(
        TableType.PERSONS,
        persons_meta,
        [
            {"id": "bbmri-eric:contactID:CY_person1", "name": "person1"},
            {"id": "bbmri-eric:contactID:EU_person2", "name": "should be overwritten"},
            {"id": "bbmri-eric:contactID:EU_person4", "name": "person4"},
        ],
    )

    eu_persons_meta = MagicMock()
    eu_persons = Table.of(
        TableType.PERSONS,
        eu_persons_meta,
        [
            {"id": "bbmri-eric:contactID:EU_person2", "name": "person2"},
            {"id": "bbmri-eric:contactID:EU_person3", "name": "person3"},
        ],
    )

    node_data.node = cy
    eu_node_data.node = eu
    transformer.node_data = node_data
    transformer.eu_node_data = eu_node_data

    transformer._replace_rows(cy, persons, eu_persons)

    assert persons.rows_by_id["bbmri-eric:contactID:EU_person2"]["name"] == "person2"
    assert transformer.warnings == [
        EricWarning(
            message="bbmri-eric:contactID:EU_person4 is not present in "
            "eu_bbmri_eric_persons"
        )
    ]


def test_transformer_set_biobank_labels(transformer):
    node_data = MagicMock()
    node_data.collections.rows = [
        {"biobank": "biobank1", "name": "Collections1"},
        {"biobank": "biobank2", "name": "Collections2"},
    ]
    node_data.biobanks.rows_by_id = {
        "biobank1": {"name": "BIOBANK1"},
        "biobank2": {"name": ""},
    }
    transformer.node_data = node_data

    transformer._set_biobank_labels()

    assert node_data.collections.rows[0]["biobank_label"] == "BIOBANK1"

    assert node_data.collections.rows[1]["biobank_label"] == ""


def test_transformer_create_combined_networks(transformer):
    node_data = MagicMock()
    node_data.collections.rows = [
        {"biobank": "biobank1", "network": []},
        {"biobank": "biobank2", "network": ["network1"]},
        {"biobank": "biobank3", "network": ["network2"]},
        {"biobank": "biobank4", "network": []},
        {"biobank": "biobank5", "network": ["network1", "network2"]},
        {"biobank": "biobank6", "network": []},
    ]
    node_data.biobanks.rows_by_id = {
        "biobank1": {"network": ["network1", "network2"]},
        "biobank2": {"network": []},
        "biobank3": {"network": ["network1"]},
        "biobank4": {"network": ["network2"]},
        "biobank5": {"network": []},
        "biobank6": {"network": []},
    }
    transformer.node_data = node_data

    transformer._set_combined_networks()

    assert set(node_data.collections.rows[0]["combined_network"]) == {
        "network1",
        "network2",
    }
    assert set(node_data.collections.rows[1]["combined_network"]) == {"network1"}
    assert set(node_data.collections.rows[2]["combined_network"]) == {
        "network1",
        "network2",
    }
    assert set(node_data.collections.rows[3]["combined_network"]) == {"network2"}
    assert set(node_data.collections.rows[4]["combined_network"]) == {
        "network1",
        "network2",
    }
    assert set(node_data.collections.rows[5]["combined_network"]) == set()


def test_transformer_combined_quality(node_data, transformer):
    q_info = QualityInfo(
        biobanks={},
        biobank_levels={
            "bbmri-eric:ID:NL_test_quality_biobank1": ["level_bio1", "level_bio2"],
            "bbmri-eric:ID:NL_test_quality_biobank2": ["level_bio3"],
            "bbmri-eric:ID:NL_test_quality_biobank3": ["level_bio_col"],
        },
        collections={},
        collection_levels={
            "bbmri-eric:ID:NL_bb1:collection:test_quality1": ["level_col5"],
            "bbmri-eric:ID:NL_bb1:collection:test_quality2": ["level_col2"],
            "bbmri-eric:ID:NL_bb3:collection:test_quality3": ["level_bio_col"],
        },
    )
    transformer.node_data = node_data
    transformer.quality = q_info

    transformer._set_combined_qualities()

    assert (
        "combined_quality"
        not in node_data.biobanks.rows_by_id["bbmri-eric:ID:NL_biobank_noQual"]
    )
    assert sorted(
        node_data.collections.rows_by_id["bbmri-eric:ID:NL_bb1:collection:test_noQual"][
            "combined_quality"
        ]
    ) == sorted(["level_bio1", "level_bio2"])
    assert sorted(
        node_data.collections.rows_by_id[
            "bbmri-eric:ID:NL_bb1:collection:test_quality2"
        ]["combined_quality"]
    ) == sorted(["level_col2", "level_bio3"])
    assert node_data.collections.rows_by_id[
        "bbmri-eric:ID:NL_bb1:collection:test_quality1"
    ]["combined_quality"] == ["level_col5"]
    assert node_data.collections.rows_by_id[
        "bbmri-eric:ID:NL_bb3:collection:test_quality3"
    ]["combined_quality"] == ["level_bio_col"]
    assert (
        node_data.collections.rows_by_id["bbmri-eric:ID:NL_valid-collectionID-1"][
            "combined_quality"
        ]
        == []
    )


def test_map_categories(transformer):
    node_data = MagicMock()
    collection1 = MagicMock()
    collection2 = MagicMock()
    node_data.collections.rows = [collection1, collection2]
    category_mapper = MagicMock()
    transformer.node_data = node_data
    transformer.category_mapper = category_mapper

    transformer._set_collection_categories()

    category_mapper.map.assert_has_calls([call(collection1), call(collection2)])
