from unittest import mock
from unittest.mock import MagicMock, call

import pytest

from molgenis_emx2.directory_client.model import Table, TableType
from molgenis_emx2.directory_client.pid_manager import (
    NoOpPidManager,
    PidManager,
    PidManagerFactory,
)
from molgenis_emx2.directory_client.pid_service import (
    DummyPidService,
    NoOpPidService,
    Status,
)
from molgenis_emx2.directory_client.printer import Printer


@pytest.fixture
def pid_manager(pid_service, printer):
    return PidManager(pid_service, printer)


def test_assign_biobank_pids(pid_manager, pid_service):
    biobank_meta = MagicMock()
    biobank_meta.id_attribute = "id"
    biobanks = Table.of(
        table_type=TableType.BIOBANKS,
        meta=biobank_meta,
        rows=[
            {"id": "b1", "name": "biobank1", "pid": "pid1"},
            {"id": "b2", "name": "biobank2"},
            {"id": "b3", "name": "biobank3"},
            {"id": "b4", "name": "biobank4", "withdrawn": True},
        ],
    )

    pid_service.reverse_lookup.side_effect = [[], ["pid3"], []]
    pid_service.register_pid.return_value = None

    warnings = pid_manager.assign_biobank_pids(biobanks)

    pid_service.register_pid.assert_has_calls(
        [
            call(url="url/#/biobank/b2", name="biobank2"),
            call(url="url/#/biobank/b4", name="biobank4"),
        ]
    )

    pid_service.set_status.assert_called_with(None, Status.WITHDRAWN)

    assert len(warnings) == 1
    assert (
        warnings[0].message
        == "PID(s) already exist for new biobank \"biobank3\": ['pid3']. Please check "
        "the PID's contents!"
    )


def test_update_biobank_pids(pid_manager, pid_service):
    biobank_meta = MagicMock()
    biobank_meta.id_attribute = "id"
    biobanks = Table.of(
        table_type=TableType.BIOBANKS,
        meta=biobank_meta,
        rows=[
            {"id": "b1", "name": "biobank1", "pid": "pid1"},
            {"id": "b2", "name": "biobank2_renamed", "pid": "pid2"},
            {"id": "b3", "name": "biobank3", "pid": "pid3", "withdrawn": True},
            {"id": "b4", "name": "biobank4", "pid": "pid4", "withdrawn": False},
        ],
    )

    existing_biobanks = Table.of(
        table_type=TableType.BIOBANKS,
        meta=biobank_meta,
        rows=[
            {"id": "b1", "name": "biobank1", "pid": "pid1", "withdrawn": False},
            {"id": "b2", "name": "biobank2", "pid": "pid2", "withdrawn": False},
            {"id": "b3", "name": "biobank3", "pid": "pid3", "withdrawn": False},
            {"id": "b4", "name": "biobank4", "pid": "pid4", "withdrawn": True},
        ],
    )

    pid_manager.update_biobank_pids(biobanks, existing_biobanks)

    pid_service.set_name.assert_called_with("pid2", "biobank2_renamed")
    pid_service.set_status.assert_called_with("pid3", Status.WITHDRAWN)
    pid_service.remove_status.assert_called_with("pid4")


def test_terminate_biobanks(pid_manager, pid_service):
    pid_manager.terminate_biobanks(["pid1", "pid2"])
    assert pid_service.set_status.mock_calls == [
        mock.call("pid1", Status.TERMINATED),
        mock.call("pid2", Status.TERMINATED),
    ]


def test_noop_pid_manager():
    noop = NoOpPidManager()

    assert noop.assign_biobank_pids(MagicMock()) == []
    assert noop.update_biobank_pids(MagicMock(), MagicMock()) is None
    assert noop.terminate_biobanks(MagicMock()) is None


def test_pid_manager_factory():
    noop_pid_service = NoOpPidService()
    dummy_pid_service = DummyPidService()
    printer = Printer()

    manager1 = PidManagerFactory.create(noop_pid_service, printer)
    manager2 = PidManagerFactory.create(dummy_pid_service, printer)

    assert type(manager1) == NoOpPidManager
    assert type(manager2) == PidManager
