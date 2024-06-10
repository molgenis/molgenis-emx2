import pytest
import requests.exceptions

from molgenis.bbmri_eric.errors import (
    EricError,
    EricWarning,
    ErrorReport,
    requests_error_handler,
)
from molgenis.bbmri_eric.model import Node


def test_warning():
    warning = EricWarning("test")
    assert warning.message == "test"


def test_error():
    error = EricError("test")
    assert str(error) == "test"


def test_error_report():
    a = Node("A", "A", None)
    b = Node("B", "B", None)
    report = ErrorReport([a, b])
    warning = EricWarning("warning")
    error = EricError("error")

    assert not report.has_errors()
    assert not report.has_warnings()

    report.add_node_error(a, error)

    assert report.node_errors[a] == error
    assert b not in report.node_errors
    assert report.has_errors()
    assert not report.has_warnings()

    report.add_node_warnings(b, [warning, warning])

    assert report.node_warnings[b] == [warning, warning]
    assert a not in report.node_warnings
    assert report.has_errors()
    assert report.has_warnings()


def test_error_report_global_error():
    report = ErrorReport([])
    assert not report.has_errors()
    report.set_global_error(EricError())
    assert report.has_errors()


def test_requests_error_handler():
    exception = requests.exceptions.ConnectionError()

    @requests_error_handler
    def raising_function():
        raise exception

    with pytest.raises(EricError) as exception_info:
        raising_function()

    assert exception_info.value.__cause__ == exception
