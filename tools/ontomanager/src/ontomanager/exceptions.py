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
