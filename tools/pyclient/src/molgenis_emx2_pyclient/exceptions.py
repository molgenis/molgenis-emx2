"""Exceptions that may occur while applying the Molgenis EMX2 Python client."""


class PyclientException(Exception):
    """Thrown when an ambiguous exception occurred."""

    def __init__(self, msg: str = None) -> None:
        super().__init__()
        self.msg = msg

    def __str__(self) -> str:
        exception_msg = f"Message: {self.msg}\n"
        return exception_msg


class SigninError(PyclientException):
    """Thrown when a signing in to a server failed."""


class NoSuchSchemaException(PyclientException):
    """Thrown when a schema is requested that is not found on the server."""


class NoSuchTableException(PyclientException):
    """Thrown when a table is requested that is not found in the schema."""


class ServerNotFoundError(PyclientException):
    """Thrown when a server cannot be found from the url."""


class ServiceUnavailableError(PyclientException):
    """Thrown when a server is not available for handling a request."""
