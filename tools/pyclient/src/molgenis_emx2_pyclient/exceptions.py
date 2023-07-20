"""Exceptions that may occur while applying the Molgenis EMX2 Python client."""


class PyclientException(Exception):
    """Base Pyclient exception."""

    def __init__(self, msg: str = None) -> None:
        super().__init__()
        self.msg = msg

    def __str__(self) -> str:
        exception_msg = f"Message: {self.msg}\n"
        return exception_msg


class NoSuchSchemaException(PyclientException):
    """Thrown when a schema is requested that is not found on the server."""
