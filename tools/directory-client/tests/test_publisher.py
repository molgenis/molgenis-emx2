from unittest import mock
from unittest.mock import MagicMock, patch

import pytest

from molgenis.bbmri_eric.errors import EricWarning, ErrorReport
from molgenis.bbmri_eric.model import (
    MixedData,
    Node,
    NodeData,
    QualityInfo,
    Source,
    Table,
    TableType,
)
from molgenis.bbmri_eric.publisher import Publisher, PublishingState


@pytest.fixture
def pid_manager_factory():
    with patch(
        "molgenis.bbmri_eric.publisher.PidManagerFactory"
    ) as pid_manager_factory_mock:
        yield pid_manager_factory_mock


@pytest.fixture
def publisher(session, printer, pid_service) -> Publisher:
    return Publisher(session, printer, pid_service)


def test_publish(publisher, session):
    publisher._delete_rows = MagicMock()

    state = PublishingState(
        nodes=[Node.of("NL"), Node.of("BE")],
        existing_data=MixedData(
            source=Source.TRANSFORMED,
            persons=Table.of_empty(TableType.PERSONS, MagicMock()),
            networks=Table.of_empty(TableType.NETWORKS, MagicMock()),
            also_known_in=Table.of_empty(TableType.ALSO_KNOWN, MagicMock()),
            biobanks=Table.of_empty(TableType.BIOBANKS, MagicMock()),
            collections=Table.of_empty(TableType.COLLECTIONS, MagicMock()),
            facts=Table.of_empty(TableType.FACTS, MagicMock()),
        ),
        eu_node_data=MagicMock(),
        quality_info=MagicMock(),
        report=MagicMock(),
        diseases=MagicMock(),
    )

    publisher.publish(state)

    session.upload_data.assert_called_with(state.data_to_publish)
    assert publisher._delete_rows.mock_calls == [
        mock.call(state.data_to_publish.facts, state.existing_data.facts, state),
        mock.call(
            state.data_to_publish.collections, state.existing_data.collections, state
        ),
        mock.call(state.data_to_publish.biobanks, state.existing_data.biobanks, state),
        mock.call(
            state.data_to_publish.also_known_in,
            state.existing_data.also_known_in,
            state,
        ),
        mock.call(state.data_to_publish.networks, state.existing_data.networks, state),
        mock.call(state.data_to_publish.persons, state.existing_data.persons, state),
    ]


def test_delete_rows(publisher, pid_service, node_data: NodeData, session):
    existing_biobanks_table = Table.of(
        table_type=TableType.BIOBANKS,
        meta=MagicMock(),
        rows=[
            {
                "id": "bbmri-eric:ID:NL_valid-biobankID-1",
                "pid": "pid1",
                "national_node": "NL",
            },
            {"id": "delete_this_row", "pid": "pid2", "national_node": "NL"},
            {"id": "undeletable_id", "pid": "pid3", "national_node": "NL"},
        ],
    )

    state: PublishingState = MagicMock()
    state.quality_info = QualityInfo(
        biobanks={"undeletable_id": ["quality"]},
        collections={},
        biobank_levels={},
        collection_levels={},
    )

    state.report = ErrorReport([node_data.node])

    warning1 = EricWarning("ID delete_this_row is deleted")

    warning2 = EricWarning(
        "Prevented the deletion of a row that is referenced from "
        "the quality info: biobanks undeletable_id."
    )

    publisher._delete_rows(node_data.biobanks, existing_biobanks_table, state)

    publisher.pid_manager.terminate_biobanks.assert_called_with(["pid2"])
    session.delete_list.assert_called_with(
        "eu_bbmri_eric_biobanks", ["delete_this_row"]
    )

    assert state.report.node_warnings[node_data.node] == [warning1, warning2]
