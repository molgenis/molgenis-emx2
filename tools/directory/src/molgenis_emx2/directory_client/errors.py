from collections import defaultdict
from dataclasses import dataclass, field
from typing import DefaultDict, List, Optional

import requests

from .model import Node


@dataclass(frozen=True)
class DirectoryWarning:
    """
    Class that contains a warning message. Use this when a problem occurs that
    shouldn't cancel the current action (for example staging or publishing).
    """

    message: str


class DirectoryError(Exception):
    """
    Raise this exception when an error occurs that we can not recover from.
    """

    pass


@dataclass
class ErrorReport:
    """
    Summary object. Stores errors and warnings that occur during staging or publishing.
    """

    nodes: List[Node]
    node_errors: DefaultDict[Node, DirectoryError] = field(
        default_factory=lambda: defaultdict(list)
    )
    node_warnings: DefaultDict[Node, List[DirectoryWarning]] = field(
        default_factory=lambda: defaultdict(list)
    )
    error: Optional[DirectoryError] = None

    def add_node_error(self, node: Node, error: DirectoryError):
        self.node_errors[node] = error

    def add_node_warnings(self, node: Node, warnings: List[DirectoryWarning]):
        if warnings:
            self.node_warnings[node].extend(warnings)

    def set_global_error(self, error: DirectoryError):
        self.error = error

    def has_errors(self) -> bool:
        return len(self.node_errors) > 0 or self.error

    def has_warnings(self) -> bool:
        return len(self.node_warnings) > 0


class MolgenisRequestError(Exception):
    def __init__(self, error, response=False):
        self.message = error
        if response:
            self.response = response


def requests_error_handler(func):
    """
    Decorator that catches RequestExceptions and wraps them in a DirectoryError.
    """

    def inner_function(*args, **kwargs):
        try:
            return func(*args, **kwargs)
        except (requests.exceptions.RequestException, MolgenisRequestError) as e:
            raise DirectoryError("Request failed") from e

    return inner_function
