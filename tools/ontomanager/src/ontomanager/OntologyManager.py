# OntologyManager.py

import logging

from molgenis_emx2_client import Client
from requests import Response

from .constants import ontology_columns
from .graphql_queries import Queries

log = logging.getLogger(__name__)


class OntologyManager:
    """Class that manages the actions."""

    def __init__(self, url: str, username: str, password: str):
        """
        Create an OntologyManager object using a URL and login credentials to the server.

        :param url: the URL to the server
        :param username: the username to sign in to the server
        :param password: the password that belongs to the username for this server, default None
        """
        self.client = Client(url=url, username=username, password=password)
        self.graphql_endpoint = f'{self.client.url}/CatalogueOntologies/graphql'

    def perform(self, action: str, table: str, **kwargs):
        """Select the method to perform and pass any keyword arguments"""
        match action:
            case 'create':
                self.add(table, **kwargs)
            case 'delete':
                self.delete(table, **kwargs)
            case 'update':
                self.update(table, **kwargs)

    def add(self, table: str, **kwargs) -> Response:
        """Add a term to an ontology."""
        print(f"Adding to table {table}.")

        _kwargs = self.__parse_kwargs(kwargs)
        _table = self.__parse_table_name(table)

        query = Queries.insert(_table)
        variables = {"value": _kwargs}

        response = self.__perform_query(query, variables, action='add')

        return response

    def delete(self, table: str, **kwargs) -> Response:
        """Delete a term from an ontology."""
        print(f"Deleting from table {table}.")

        _kwargs = self.__parse_kwargs(kwargs)
        _table = self.__parse_table_name(table)

        query = Queries.delete(_table)
        variables = {"pkey": {'name': _kwargs['name']}}

        response = self.__perform_query(query, variables, action='delete')

        return response

    def update(self, table: str, **kwargs):
        """Rename a term in an ontology."""
        # TODO: create this function
        print(f"Renaming in table {table}.")

        _kwargs = self.__parse_kwargs(kwargs)
        _table = self.__parse_table_name(table)
        pass

    def __perform_query(self, query: str, variables: dict, action: str) -> Response:
        """Perform the query using the query and variables supplied."""

        _response = self.client.session.post(
            self.graphql_endpoint,
            json={"query": query, "variables": variables}
        )

        match action:
            case 'add':
                verbs = ['adding', 'added']
            case 'delete':
                verbs = ['deleting', 'deleted']
            case other:
                verbs = ['editing', 'edited']

        if _response.status_code != 200:
            log.error(f"Error while {verbs[0]} record, status code {_response.status_code}")
            log.error(_response.text)
        else:
            log.info(f"Successfully {verbs[1]} {variables[next(iter(variables))]['name']}.")

        return _response

    @staticmethod
    def __parse_table_name(table_name: str) -> str:
        """Parse table names to capitalized names without spaces,
        e.g. 'Network features' -> 'NetworkFeatures'.
        """
        return "".join(word.capitalize() for word in table_name.split(' '))

    @staticmethod
    def __parse_kwargs(kwargs) -> dict:
        """Ensure the passed kwargs are in correct format."""
        _kwargs = {key: value for (key, value) in kwargs.items() if key in ontology_columns}
        _kwargs = {key: {'name': value} if key == 'parent' else value
                   for (key, value) in _kwargs.items()}

        wrong_keywords = [key for key in kwargs.keys() if key not in ontology_columns]
        if len(wrong_keywords) > 0:
            log.error(f"Ignoring incorrect keywords supplied for operation: {', '.join(wrong_keywords)}.")

        return _kwargs
