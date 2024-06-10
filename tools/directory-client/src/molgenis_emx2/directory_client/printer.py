from contextlib import contextmanager

from molgenis.bbmri_eric.errors import EricError, EricWarning, ErrorReport
from molgenis.bbmri_eric.model import Node


class Printer:
    """
    Simple printer that keeps track of indentation levels. Also has utility methods
    for printing some Eric objects.
    """

    def __init__(self):
        self.indents = 0

    def indent(self):
        self.indents += 1

    def dedent(self):
        self.indents = max(0, self.indents - 1)

    def reset_indent(self):
        self.indents = 0

    def print(self, value: str = None, indent: int = 0):
        self.indents += indent
        if value:
            print(f"{'    ' * self.indents}{value}")
        else:
            print()
        self.indents -= indent

    def print_node_title(self, node: Node):
        self.print_header(f"🌍 Node {node.code} ({node.description})")

    def print_header(self, text: str):
        title = f"{text}"
        border = "=" * (len(title) + 1)
        self.reset_indent()
        self.print()
        self.print(border)
        self.print(title)
        self.print(border)

    def print_error(self, error: EricError):
        message = str(error)
        if error.__cause__:
            message += f" - Cause: {str(error.__cause__)}"
        self.print(f"❌ {message}")

    def print_warning(self, warning: EricWarning, indent: int = 0):
        self.print(f"⚠️ {warning.message}", indent)

    def print_summary(self, report: ErrorReport):
        self.reset_indent()
        self.print()
        self.print("==========")
        self.print("📋 Summary")
        self.print("==========")

        for node in report.nodes:
            if node in report.node_errors or report.error:
                message = f"❌ Node {node.code} failed"
                if node in report.node_warnings:
                    message += f" with {len(report.node_warnings[node])} warning(s)"
            elif node in report.node_warnings:
                message = (
                    f"⚠️ Node {node.code} finished successfully with "
                    f"{len(report.node_warnings[node])} warning(s)"
                )
            else:
                message = f"✅ Node {node.code} finished successfully"
            self.print(message)

    @contextmanager
    def indentation(self):
        self.indent()
        yield
        self.dedent()
