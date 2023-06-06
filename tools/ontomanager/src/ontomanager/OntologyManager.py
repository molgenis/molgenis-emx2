# manager.py
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
        Create a Manager object using a URL and log in credentials to the server.

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

        _table = self.__parse_table_name(table)
        _kwargs = self.__parse_kwargs(kwargs)

        query = Queries.insert(_table)
        variables = {"value": [_kwargs]}

        response = self.client.session.post(
            self.graphql_endpoint,
            json={"query": query, "variables": variables}
        )

        if response.status_code != 200:
            log.error(f"Error while adding record, status code {response.status_code}")
            log.error(response.text)

        return response

    def delete(self, table: str, **kwargs):
        """Delete a term from an ontology."""
        print(f"Deleting from table {table}.")
        _table = self.__parse_table_name(table)
        _kwargs = self.__parse_kwargs(kwargs)
        pass

    def update(self, table: str, **kwargs):
        """Rename a term in an ontology."""
        print(f"Renaming in table {table}.")
        _table = self.__parse_table_name(table)
        _kwargs = self.__parse_kwargs(kwargs)
        pass

    @staticmethod
    def __parse_table_name(table_name: str):
        return "".join(word.capitalize() for word in table_name.split(' '))

    @staticmethod
    def __parse_kwargs(kwargs):
        """Ensure the passed kwargs are in correct format."""
        _kwargs = {key: value for (key, value) in kwargs.items() if key in ontology_columns}
        _kwargs = {key: {'name': value} if key == 'parent' else value
                   for (key, value) in _kwargs.items()}

        wrong_keywords = [key for key in kwargs.keys() if key not in ontology_columns]
        if len(wrong_keywords) > 0:
            log.error(f"Incorrect keywords supplied for operation: {', '.join(wrong_keywords)}.")

        return _kwargs
