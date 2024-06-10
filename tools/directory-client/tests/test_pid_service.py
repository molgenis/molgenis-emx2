from unittest import mock
from unittest.mock import MagicMock

import pytest

from molgenis.bbmri_eric.errors import EricError
from molgenis.bbmri_eric.pid_service import (
    DummyPidService,
    NoOpPidService,
    PidService,
    Status,
)


@pytest.fixture
def handle_client() -> MagicMock:
    return MagicMock()


@pytest.fixture
def pid_service(handle_client) -> PidService:
    return PidService(handle_client, "test", "test.nl")


def test_reverse_lookup(pid_service, handle_client):
    handle_client.search_handle.return_value = ["pid1", "pid2"]

    result = pid_service.reverse_lookup("my_url")

    handle_client.search_handle.assert_called_with(URL="my_url", prefix="test")
    assert result == ["pid1", "pid2"]


def test_reverse_lookup_no_auth(pid_service, handle_client):
    handle_client.search_handle.return_value = None

    with pytest.raises(EricError) as e:
        pid_service.reverse_lookup("my_url")

    assert str(e.value) == "Insufficient permissions for reverse lookup"


def test_register_pid(pid_service: PidService, handle_client):
    handle_client.register_handle.return_value = "test/pid"

    with mock.patch.object(PidService, "generate_pid") as generate_pid_mock:
        generate_pid_mock.return_value = "test/pid"
        result = pid_service.register_pid("url", "biobank1")

    handle_client.register_handle.assert_called_with(
        handle="test/pid", location="url", NAME="biobank1"
    )
    assert result == "test/pid"


def test_set_name(pid_service: PidService, handle_client):
    pid_service.set_name("pid1", "new_name")
    handle_client.modify_handle_value.assert_called_with("pid1", NAME="new_name")


def test_set_status(pid_service: PidService, handle_client):
    pid_service.set_status("pid1", Status.TERMINATED)
    handle_client.modify_handle_value.assert_called_with("pid1", STATUS="TERMINATED")


def test_remove_status(pid_service: PidService, handle_client):
    pid_service.remove_status("pid1")
    handle_client.delete_handle_value.assert_called_with("pid1", "STATUS")


def test_generate_pid(pid_service: PidService):
    pid = pid_service.generate_pid("test")

    pid = pid.replace("-", "")
    prefix, suffix = pid.split("/1.")

    assert prefix == "test"
    try:
        int(suffix, 16)
    except ValueError:
        pytest.fail("Couldn't parse hex id as int")


def test_dummy_service():
    dummy = DummyPidService()

    assert dummy.register_pid("", "").startswith("FAKE-PREFIX/1.")
    assert dummy.reverse_lookup("") is None


def test_noop_service():
    noop = NoOpPidService()

    assert noop.register_pid("", "") is None
    assert noop.reverse_lookup("") is None


def test_base_url(handle_client):
    service1 = PidService(handle_client, "test", "test1.nl")
    service2 = PidService(handle_client, "test", "test2.nl")

    assert service1.base_url == "test1.nl/"
    assert service2.base_url == "test2.nl/"
