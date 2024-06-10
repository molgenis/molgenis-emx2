from unittest.mock import MagicMock

import pytest

from molgenis.bbmri_eric.model_fitting import ModelFitter


@pytest.fixture
def model_fitter():
    return ModelFitter(
        node_data=MagicMock(),
        printer=MagicMock(),
    )


def test_merge_covid19_capabilities(model_fitter):
    node_data = MagicMock()
    node_data.biobanks.rows = [
        {"id": "0"},
        {"id": "1", "covid19biobank": None, "capabilities": None},
        {"id": "2", "covid19biobank": None, "capabilities": ["a", "b"]},
        {"id": "3", "covid19biobank": ["c"], "capabilities": ["a", "b"]},
        {"id": "4", "covid19biobank": ["c"], "capabilities": None},
        {"id": "5", "covid19biobank": ["a"], "capabilities": ["a", "b"]},
    ]
    model_fitter.node_data = node_data

    model_fitter._merge_covid19_capabilities()

    assert node_data.biobanks.rows == [
        {"id": "0"},
        {"id": "1", "capabilities": None},
        {"id": "2", "capabilities": ["a", "b"]},
        {"id": "3", "capabilities": ["a", "b", "c"]},
        {"id": "4", "capabilities": ["c"]},
        {"id": "5", "capabilities": ["a", "b"]},
    ]


def test_move_head_info(node_data, model_fitter):
    model_fitter.node_data = node_data

    model_fitter._move_heads_to_persons()

    assert (
        node_data.persons.rows_by_id["bbmri-eric:contactID:NL_valid-personID-1"]["role"]
        == "Director"
    )
    # Separate checks as person has two roles and order differs per run

    assert (
        node_data.persons.rows_by_id["bbmri-eric:contactID:" "NL_geluk"][
            "title_before_name"
        ]
        == "Dr"
    )
    assert (
        node_data.persons.rows_by_id["bbmri-eric:contactID:NL_geluk"]["first_name"]
        == "Piet"
    )
    assert (
        node_data.persons.rows_by_id["bbmri-eric:contactID:NL_geluk"]["last_name"]
        == "Geluk"
    )
    assert (
        node_data.persons.rows_by_id["bbmri-eric:contactID:NL_geluk"]["email"]
        == "UNKNOWN@NL"
    )
    assert (
        node_data.persons.rows_by_id["bbmri-eric:contactID:NL_geluk"]["country"] == "NL"
    )
    assert (
        node_data.persons.rows_by_id["bbmri-eric:contactID:" "NL_geluk"][
            "national_node"
        ]
        == "NL"
    )
    assert (
        set(
            node_data.persons.rows_by_id["bbmri-eric:contactID:NL_geluk"]["role"].split(
                " and "
            )
        )
    ) == set("Collection head and Principal Investigator".split(" and "))
    assert node_data.persons.rows_by_id["bbmri-eric:contactID:NL_devries"] == {
        "id": "bbmri-eric:contactID:NL_devries",
        "title_after_name": "Msc",
        "title_before_name": "Prof",
        "first_name": "Bart",
        "last_name": "de Vries",
        "email": "UNKNOWN@NL",
        "role": "Principal Investigator",
        "country": "NL",
        "national_node": "NL",
    }

    assert (
        node_data.biobanks.rows_by_id["bbmri-eric:ID:NL_valid-biobankID-1"]["head"]
        == "bbmri-eric:contactID:NL_valid-personID-1"
    )
    assert (
        node_data.biobanks.rows_by_id["bbmri-eric:ID:NL_valid:biobankID-2"]["head"]
        == "bbmri-eric:contactID:NL_geluk"
    )
    assert (
        node_data.collections.rows_by_id["bbmri-eric:ID:" "NL_valid-collectionID-1"][
            "head"
        ]
        == "bbmri-eric:contactID:NL_valid-personID-1"
    )
    assert (
        node_data.collections.rows_by_id["bbmri-eric:ID:" "NL_valid:collectionID-2"][
            "head"
        ]
        == "bbmri-eric:contactID:NL_geluk"
    )
    assert (
        node_data.collections.rows_by_id["bbmri-eric:ID:" "NL_valid:collectionID-3"][
            "head"
        ]
        == "bbmri-eric:contactID:NL_devries"
    )
