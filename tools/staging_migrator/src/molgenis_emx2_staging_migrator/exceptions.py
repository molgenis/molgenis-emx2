"""Exceptions that may occur while applying the Molgenis EMX2 Staging Migrator."""
from molgenis_emx2_pyclient.exceptions import PyclientException


class StagingMigratorException(PyclientException):
    """Thrown when an ambiguous exception occurred."""

    def __init__(self, msg: str = None) -> None:
        super().__init__()
        self.msg = msg

    def __str__(self) -> str:
        exception_msg = f"Message: {self.msg}\n"
        return exception_msg

class MissingContactException(StagingMigratorException):
    """Thrown when a contact is referenced that is missing in the Contacts table."""

class MissingHRICoreException(StagingMigratorException):
    """Thrown when `hricore` is not set to `True` for any record in the Resources table."""

class ReferenceDeleteError(StagingMigratorException):
    """Thrown when a record is to be deleted, but is still referenced by another record."""

class NoSuchResourceException(StagingMigratorException):
    """Thrown when a resource is to be edited that is missing in the target's Resources table."""

class DraftException(StagingMigratorException):
    """Thrown when a table contains one or more draft records."""
