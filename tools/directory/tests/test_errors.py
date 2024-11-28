import pytest
import requests.exceptions

from molgenis_emx2.directory_client.errors import (
    DirectoryError,
    DirectoryWarning,
    ErrorReport,
    requests_error_handler,
)
from molgenis_emx2.directory_client.model import Node


def test_warning():
    warning = DirectoryWarning("test")
    assert warning.message == "test"


def test_error():
    error = DirectoryError("test")
    assert str(error) == "test"


def test_error_report():
    a = Node("A", "A", None)
    b = Node("B", "B", None)
    report = ErrorReport([a, b])
    warning = DirectoryWarning("warning")
    error = DirectoryError("error")

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
    report.set_global_error(DirectoryError())
    assert report.has_errors()


def test_requests_error_handler():
    exception = requests.exceptions.ConnectionError()

    @requests_error_handler
    def raising_function():
        raise exception

    with pytest.raises(DirectoryError) as exception_info:
        raising_function()

    assert exception_info.value.__cause__ == exception


# def test_molgenisrequesterror(session):
#     def get_table_meta(self, table_name: str,  schema: str = None, ):
#         schema_meta = self.get_schema_metadata(schema)
#         for table in schema_meta.tables:
#             if table.name == table_name:
#                 return table.columns
#         raise MolgenisRequestError(f"Unknown table: {table_name}")
#
#
#     with pytest.raises(DirectoryError) as e:
#         await stager.stage(node)
#
#     assert str(e.value) == (
#         "The session user has invalid permissions\n       "
#         "Please check the token and permissions of this user"
#     )

#
#
#
# exception = requests.exceptions.ConnectionError()
#
# @requests_error_handler
# def raising_function():
#     raise exception
#
# with pytest.raises(MolgenisRequestError) as exception_info:
#    raise MolgenisRequestError("error")
#
# assert exception_info.value.__cause__ == exception
