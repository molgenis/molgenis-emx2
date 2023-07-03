# exceptions.py

"""Exceptions that may happen while executing OntologyManager code."""


class OntomanagerException(Exception):
    """Base OntologyManager exception."""

    def __init__(self, msg: str = None) -> None:
        super().__init__()
        self.msg = msg

    def __str__(self) -> str:
        exception_msg = f"Message: {self.msg}\n"
        return exception_msg


class DuplicateKeyException(OntomanagerException):
    """Thrown when a term is uploaded that already is present in the table."""


class NoSuchTableException(OntomanagerException):
    """Thrown when a CatalogueOntologies table could not be found."""


class NoSuchNameException(OntomanagerException):
    """Thrown when an ontology name could not be found."""


class MissingPkeyException(OntomanagerException):
    """Thrown when an action on an ontology term is requested without specifying the primary key."""


class UpdateItemsException(OntomanagerException):
    """Thrown when the 'old' and 'new' terms for the update method are not correctly specified."""


class SigninError(OntomanagerException):
    """Thrown when signing in to the client has failed."""


class InvalidDatabaseException(OntomanagerException):
    """Thrown when a database is requested that cannot be loaded, due to invalid database name."""


class ParentReferenceException(OntomanagerException):
    """Thrown when an ontology item to be deleted is referenced by a child term."""
