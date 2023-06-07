# OntologyManager.py

import logging

from molgenis_emx2_client import Client
from requests import Response

from .constants import ontology_columns
from .exceptions import OntomanagerException, DuplicateKeyException, MissingPkeyException, NoSuchNameException, \
    NoSuchTableException, UpdateItemsException
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
        self.ontology_tables = self.__list_ontology_tables()

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

        if table not in self.ontology_tables:
            raise NoSuchTableException(f"Table '{table}' not found in CatalogueOntologies.")

        _kwargs = self.__parse_kwargs(kwargs)
        _table = self.__parse_table_name(table)

        query = Queries.insert(_table)
        variables = {"value": _kwargs}

        response = self.__perform_query(query, variables, action='add')

        return response

    def delete(self, table: str, **kwargs) -> Response:
        """Delete a term from an ontology."""
        print(f"Deleting from table {table}.")

        if table not in self.ontology_tables:
            raise NoSuchTableException(f"Table '{table}' not found in CatalogueOntologies.")

        _kwargs = self.__parse_kwargs(kwargs)
        _table = self.__parse_table_name(table)

        query = Queries.delete(_table)
        variables = {"pkey": {'name': _kwargs['name']}}

        if _kwargs['name'] not in self.__list_ontology_terms(table):
            raise NoSuchNameException(f"Name '{_kwargs['name']}' not found in table '{table}'.")

        response = self.__perform_query(query, variables, action='delete')

        return response

    def update(self, table: str, **kwargs):
        """Rename a term in an ontology."""
        # TODO: do the actual update
        print(f"Renaming in table {table}.")

        if table not in self.ontology_tables:
            raise NoSuchTableException(f"Table '{table}' not found in CatalogueOntologies.")

        try:
            old, new = kwargs['old'], kwargs['new']
        except KeyError:
            raise UpdateItemsException("Specify 'old' and 'new' terms.")

        if old not in self.__list_ontology_terms(table):
            raise NoSuchNameException(f"Name '{old}' not found in table '{table}'.")
        if new not in self.__list_ontology_terms(table):
            raise NoSuchNameException(f"Name '{new}' not found in table '{table}'.")

        _table = self.__parse_table_name(table)

        # Iterate over the other databases on the server and check in which tables the ontology terms are referenced
        databases = self.__list_databases()
        server_dict = dict()
        for db in databases:
            db_dict = dict()
            db_schema = self.__get_database_schema(db)
            for tb, values in db_schema.items():
                tb_dict = dict()
                if values.get('externalSchema') == 'CatalogueOntologies':
                    continue
                for col, specs in values['columns'].items():
                    if specs.get('refSchema') == 'CatalogueOntologies' and specs.get('refTable') == table:
                        tb_dict.update({col: specs['columnType']})
                        # TODO: perform update here
                if len(tb_dict.keys()) > 0:
                    db_dict.update({tb: tb_dict})
            if len(db_dict.keys()) > 0:
                server_dict.update({db: db_dict})
        print(server_dict)

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
            message = _response.json()['errors'][0]['message']
            if 'duplicate key value' in message:
                raise DuplicateKeyException(message)
            raise OntomanagerException(_response.json()['errors'][0]['message'])
        else:
            log.info(f"Successfully {verbs[1]} {variables[next(iter(variables))]['name']}.")

        return _response

    def __list_ontology_terms(self, table: str) -> list:
        """Returns a list of the terms in the specified ontology.
        :param table: the name of the table from which the terms are requested.
        """
        query = Queries.list_ontology_terms(table)

        response = self.client.session.post(
            self.graphql_endpoint,
            json={"query": query}
        )
        terms = [term['name'] for term in response.json()['data'][table]]
        return terms

    def __list_ontology_tables(self) -> list:
        """Returns a list of ontology tables in the CatalogueOntologies database."""
        query = Queries.list_ontology_tables()
        response = self.client.session.post(
            self.graphql_endpoint,
            json={"query": query}
        )
        tables = [table['name'] for table in response.json()['data']['_schema']['tables']]
        return tables

    def __list_databases(self) -> list:
        """Returns a list of the databases on the server."""
        query = Queries.list_databases()
        response = self.client.session.post(
            url=f'{self.client.url}/apps/graphql',
            json={"query": query}
        )
        databases = [db['name'] for db in response.json()['data']['_schemas']]
        return databases

    def __get_database_schema(self, database: str) -> dict:
        """Returns the schema of the specified database.
        :param database: the name of the database.
        """
        query = Queries.database_schema()
        response = self.client.session.post(
            url=f'{self.client.url}/{database}/graphql',
            json={"query": query}
        )
        _tables = response.json()['data']['_schema']['tables']
        tables = {tab['name']: {'externalSchema': tab['externalSchema'],
                                'inherit': tab.get('inherit'),
                                'tableType': tab['tableType'],
                                'columns': {col['name']: {key: value for (key, value) in col.items() if key != "name"}
                                            for col in tab['columns']}}
                  for tab in _tables}

        return tables

    @staticmethod
    def __parse_table_name(table_name: str) -> str:
        """Parse table names to capitalized names without spaces,
        e.g. 'Network features' -> 'NetworkFeatures'.
        """
        return "".join(word.capitalize() for word in table_name.split(' '))

    @staticmethod
    def __parse_kwargs(kwargs) -> dict:
        """Ensure the passed kwargs are in correct format."""
        if 'name' not in kwargs.keys():
            raise MissingPkeyException(f"Primary key missing on entry.")
        _kwargs = {key: value for (key, value) in kwargs.items() if key in ontology_columns}
        _kwargs = {key: {'name': value} if key == 'parent' else value
                   for (key, value) in _kwargs.items()}

        wrong_keywords = [key for key in kwargs.keys() if key not in ontology_columns]
        if len(wrong_keywords) > 0:
            log.error(f"Ignoring incorrect keywords supplied for operation: {', '.join(wrong_keywords)}.")

        return _kwargs
