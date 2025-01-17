from http import HTTPStatus
from unittest import mock
from unittest.mock import AsyncMock, MagicMock, patch

import pandas as pd
import pytest
from molgenis_emx2_pyclient.metadata import Schema

from molgenis_emx2.directory_client.directory_client import DirectorySession
from molgenis_emx2.directory_client.errors import DirectoryError, DirectoryWarning
from molgenis_emx2.directory_client.model import ExternalServerNode, NodeData
from molgenis_emx2.directory_client.printer import Printer
from molgenis_emx2.directory_client.stager import Stager

pytest_plugins = "pytest_asyncio"


@pytest.fixture
def external_server_init():
    with (
        patch(
            "molgenis_emx2.directory_client.stager.ExternalServerSession"
        ) as ext_session_mock
    ):
        yield ext_session_mock


@pytest.fixture
def directory_server_init():
    with (
        patch(
            "molgenis_emx2.directory_client.stager.DirectorySession"
        ) as dir_session_mock
    ):
        yield dir_session_mock


@pytest.mark.asyncio
async def test_stager(mocker):
    mocker.patch(
        "molgenis_emx2.directory_client.directory_client."
        "DirectorySession._validate_url",
        return_value=HTTPStatus(200),
    )
    mocker.patch(
        "molgenis_emx2.directory_client.directory_client.DirectorySession._"
        "validate_graphql_response",
        return_value=HTTPStatus(200),
    )
    mocker.patch(
        "molgenis_emx2.directory_client.directory_client.DirectorySession.get_schemas",
        return_value=[
            Schema(id="BBMRI-NL", name="None-NL", label="BBMRI-ERIC", description="")
        ],
    )
    mocker.patch(
        "molgenis_emx2.directory_client.directory_client.DirectorySession."
        "set_schema",
        return_value=["BBMRI-NL"],
    )

    stager = Stager(DirectorySession("https://url.nl", "BBMRI-NL"), Printer())
    source_data = MagicMock()
    stager._clear_staging_area = MagicMock(name="_clear_staging_area")
    stager._import_node = AsyncMock(name="_import_node")
    stager._get_source_data = MagicMock(name="_get_mock_data")
    stager._get_source_data.return_value = source_data
    node = ExternalServerNode("NL", "NL", url="url")

    await stager.stage(node)

    stager._get_source_data.assert_called_with(node)
    stager._clear_staging_area.assert_called_with(node)
    stager._import_node.assert_called_with(source_data)


def test_get_source_data(external_server_init):
    node_data = MagicMock()
    node = ExternalServerNode("NL", "Netherlands", url="url")

    source_session_mock_instance = external_server_init.return_value
    source_session_mock_instance.schemas = [
        Schema(id="BBMRI-NL", name="BBMRI-NL", label="BBMRI-NL", description="")
    ]
    source_session_mock_instance.node.get_schema_id.return_value = "BBMRI-NL"
    source_session_mock_instance.get_node_data.return_value = node_data

    source_data = Stager(MagicMock(), Printer())._get_source_data(node)

    external_server_init.assert_called_with(node=node)
    assert source_data == node_data


@pytest.mark.asyncio
async def test_check_permissions(external_server_init):
    node = ExternalServerNode("NL", "Netherlands", url="url.nl", token="stager_token")
    session = external_server_init.return_value
    session.schemas = [
        Schema(id="BBMRI-ERIC", name="BBMRI-ERIC", label="BBMRI-ERIC", description="")
    ]
    session.node = node
    stager = Stager(MagicMock(), Printer())

    with pytest.raises(DirectoryError) as e:
        await stager.stage(node)

    assert str(e.value) == (
        "The session user has invalid permissions\n       "
        "Please check the token and permissions of this user"
    )


@pytest.mark.asyncio
async def test_check_tables(mocker, external_server_init):
    mocker.patch(
        "molgenis_emx2.directory_client.directory_client.DirectorySession."
        "get_schema_metadata"
    )
    node = ExternalServerNode(
        "NL", "Netherlands", url="https://url.nl", token="stager_token"
    )
    session = external_server_init.return_value
    session.node = node
    session.get_schema_metadata.return_value = Schema(
        id="BBMRI-NL", name="BBMRI-NL", label="BBMRI-NL", description="", tables=[]
    )
    eric_session = AsyncMock(DirectorySession)
    eric_session.get.return_value = pd.DataFrame({})

    stager = Stager(eric_session, Printer())
    stager._check_permissions = MagicMock()

    warnings = await stager.stage(node)

    external_server_init.assert_called_with(node=node)

    # Can't get this to work:
    # assert session.get_schema_metadata.get_tables.call_count == 6
    assert session.get_schema_metadata.call_count == 1

    assert warnings[0] == DirectoryWarning("Node NL has no persons table")
    assert warnings[1] == DirectoryWarning("Node NL has no also_known_in table")
    assert warnings[2] == DirectoryWarning("Node NL has no networks table")
    assert warnings[3] == DirectoryWarning("Node NL has no biobanks table")
    assert warnings[4] == DirectoryWarning("Node NL has no services table")
    assert warnings[5] == DirectoryWarning("Node NL has no studies table")
    assert warnings[6] == DirectoryWarning("Node NL has no collections table")
    assert warnings[7] == DirectoryWarning("Node NL has no facts table")


def test_clear_staging_area(mocker):
    mocker.patch(
        "molgenis_emx2.directory_client.directory_client.DirectorySession."
        "_validate_url",
        return_value=HTTPStatus(200),
    )
    mocker.patch(
        "molgenis_emx2.directory_client.directory_client.DirectorySession."
        "_validate_graphql_response",
        return_value=HTTPStatus(200),
    )
    mocker.patch(
        "molgenis_emx2.directory_client.directory_client.DirectorySession."
        "get_schemas",
        return_value=[
            Schema(id="BBMRI-NL", name="BBMRI-NL", label="BBMRI-ERIC", description="")
        ],
    )
    mocker.patch(
        "molgenis_emx2.directory_client.directory_client.DirectorySession."
        "set_schema",
        return_value=["None-NL"],
    )
    session = DirectorySession("https://url.nl", "BBMRI-NL")
    session.get = MagicMock()
    session.get.return_value = pd.DataFrame([{"id": "All"}])
    session.delete_records = MagicMock(name="delete")
    node = ExternalServerNode("NL", "Netherlands", url="url.nl")

    Stager(session, Printer())._clear_staging_area(node)

    assert session.delete_records.mock_calls == [
        mock.call(schema="None-NL", table="CollectionFacts", data=[{"id": "All"}]),
        mock.call(schema="None-NL", table="Collections", data=[{"id": "All"}]),
        mock.call(schema="None-NL", table="Studies", data=[{"id": "All"}]),
        mock.call(schema="None-NL", table="Services", data=[{"id": "All"}]),
        mock.call(schema="None-NL", table="Biobanks", data=[{"id": "All"}]),
        mock.call(schema="None-NL", table="Networks", data=[{"id": "All"}]),
        mock.call(schema="None-NL", table="AlsoKnownIn", data=[{"id": "All"}]),
        mock.call(schema="None-NL", table="Persons", data=[{"id": "All"}]),
    ]


@pytest.mark.asyncio
async def test_import_node(async_session, external_server_init):
    node_data: NodeData = MagicMock()
    converted_data = MagicMock()
    node_data.convert_to_staging.return_value = converted_data

    await Stager(async_session, Printer())._import_node(node_data)

    async_session.upload_data.assert_called_with(
        schema=node_data.node.get_schema_id(), data=converted_data
    )
