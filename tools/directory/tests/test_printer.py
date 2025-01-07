import textwrap

from molgenis_emx2.directory_client.errors import (
    DirectoryError,
    DirectoryWarning,
    ErrorReport,
)
from molgenis_emx2.directory_client.model import Node
from molgenis_emx2.directory_client.printer import Printer


def test_indentation(capsys):
    expected = textwrap.dedent(
        """\
        line1
            line2
                line3
                line4
            line5
        """
    )

    printer = Printer()
    printer.print("line1")
    printer.indent()
    printer.print("line2")
    printer.indent()
    printer.print("line3")
    printer.dedent()
    printer.print("line4", 1)
    printer.print("line5")

    captured = capsys.readouterr()
    assert captured.out == expected


def test_reset_indent(capsys):
    expected = textwrap.dedent(
        """\
                line1
        line2
        """
    )

    printer = Printer()
    printer.indent()
    printer.indent()
    printer.print("line1")
    printer.reset_indent()
    printer.print("line2")

    captured = capsys.readouterr()
    assert captured.out == expected


def test_print_node_title(capsys):
    node = Node("NL", "Netherlands")
    expected = textwrap.dedent(
        """\

        ========================
        🌍 Node NL (Netherlands)
        ========================
        """
    )

    printer = Printer()
    printer.print_node_title(node)

    captured = capsys.readouterr()
    assert captured.out == expected


def test_print_error_with_cause(capsys):
    expected = "❌ this is the message - Cause: this is the cause\n"

    try:
        raise DirectoryError("this is the message") from ValueError("this is the cause")
    except DirectoryError as e:
        Printer().print_error(e)

    captured = capsys.readouterr()
    assert captured.out == expected


def test_print_error_without_cause(capsys):
    expected = "❌ this is the message\n"

    try:
        raise DirectoryError("this is the message")
    except DirectoryError as e:
        Printer().print_error(e)

    captured = capsys.readouterr()
    assert captured.out == expected


def test_print_warning(capsys):
    expected = "⚠️ this is the message\n"
    warning = DirectoryWarning("this is the message")

    Printer().print_warning(warning)

    captured = capsys.readouterr()
    assert captured.out == expected


def test_print_summary(capsys):
    expected = textwrap.dedent(
        """\

        ==========
        📋 Summary
        ==========
        ✅ Node A finished successfully
        ❌ Node B failed
        ❌ Node C failed with 1 warning(s)
        ⚠️ Node D finished successfully with 2 warning(s)
        """
    )

    a = Node("A", "success", None)
    b = Node("B", "error", None)
    c = Node("C", "error and warnings", None)
    d = Node("D", "success with warnings", None)
    nodes = [a, b, c, d]
    report = ErrorReport(nodes)
    warning = DirectoryWarning("warning")
    error = DirectoryError("error")
    report.add_node_warnings(c, [warning])
    report.add_node_warnings(d, [warning, warning])
    report.add_node_error(b, error)
    report.add_node_error(c, error)

    Printer().print_summary(report)

    captured = capsys.readouterr()
    assert captured.out == expected


def test_with_indentation(capsys):
    expected = textwrap.dedent(
        """\
        line1
            line2
        line3
        """
    )

    printer = Printer()

    printer.print("line1")
    with printer.indentation():
        printer.print("line2")
    printer.print("line3")

    captured = capsys.readouterr()
    assert captured.out == expected
