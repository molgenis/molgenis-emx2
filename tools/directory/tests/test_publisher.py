from unittest import mock
from unittest.mock import MagicMock, patch

import pytest

from molgenis_emx2.directory_client.errors import DirectoryWarning, ErrorReport
from molgenis_emx2.directory_client.model import (
    MixedData,
    Node,
    NodeData,
    QualityInfo,
    Source,
    Table,
    TableType,
)
from molgenis_emx2.directory_client.publisher import Publisher, PublishingState

pytest_plugins = ("pytest_asyncio",)


@pytest.fixture
def pid_manager_factory():
    with patch(
        "molgenis_emx2.directory_client.publisher.PidManagerFactory"
    ) as pid_manager_factory_mock:
        yield pid_manager_factory_mock


@pytest.fixture
def async_publisher(async_session, printer, pid_service) -> Publisher:
    return Publisher(async_session, printer, pid_service)


@pytest.fixture
def publisher(session, printer, pid_service) -> Publisher:
    return Publisher(session, printer, pid_service)


@pytest.mark.asyncio
async def test_publish(async_publisher, async_session):
    async_publisher._delete_rows = MagicMock()

    state = PublishingState(
        nodes=[Node.of("NL"), Node.of("BE")],
        existing_data=MixedData(
            source=Source.TRANSFORMED,
            persons=Table.of_empty(TableType.PERSONS, MagicMock()),
            networks=Table.of_empty(TableType.NETWORKS, MagicMock()),
            also_known_in=Table.of_empty(TableType.ALSO_KNOWN, MagicMock()),
            biobanks=Table.of_empty(TableType.BIOBANKS, MagicMock()),
            services=Table.of_empty(TableType.BIOBANKS, MagicMock()),
            studies=Table.of_empty(TableType.BIOBANKS, MagicMock()),
            collections=Table.of_empty(TableType.COLLECTIONS, MagicMock()),
            facts=Table.of_empty(TableType.FACTS, MagicMock()),
        ),
        eu_node_data=MagicMock(),
        quality_info=MagicMock(),
        report=MagicMock(),
        diseases=MagicMock(),
    )

    await async_publisher.publish(state)

    async_session.upload_data.assert_called_with(
        schema=async_session.directory_schema, data=state.data_to_publish
    )
    assert async_publisher._delete_rows.mock_calls == [
        mock.call(state.data_to_publish.facts, state.existing_data.facts, state),
        mock.call(
            state.data_to_publish.collections, state.existing_data.collections, state
        ),
        mock.call(state.data_to_publish.studies, state.existing_data.studies, state),
        mock.call(state.data_to_publish.services, state.existing_data.services, state),
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
    biobank_meta = MagicMock()
    biobank_meta.id_attribute = "id"
    existing_biobanks_table = Table.of(
        table_type=TableType.BIOBANKS,
        meta=biobank_meta,
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
        services={},
    )

    state.report = ErrorReport([node_data.node])

    warning1 = DirectoryWarning("ID delete_this_row is deleted")

    warning2 = DirectoryWarning(
        "Prevented the deletion of a row that is referenced from "
        "the quality info: biobanks undeletable_id."
    )

    publisher._delete_rows(node_data.biobanks, existing_biobanks_table, state)

    publisher.pid_manager.terminate_biobanks.assert_called_with(["pid2"])
    session.delete_records.assert_called_with(
        table="Biobanks", data=[{"id": "delete_this_row"}]
    )

    assert state.report.node_warnings[node_data.node] == [warning1, warning2]
