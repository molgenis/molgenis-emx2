# OntologyManager.py

import logging

from molgenis_emx2_client import Client
from requests import Response

from .constants import ontology_columns
from .exceptions import OntomanagerException, DuplicateKeyException, MissingPkeyException, NoSuchNameException, \
    NoSuchTableException, UpdateItemsException, SigninError
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

        if self.client.signin_status == 'failed':
            self.client.session.close()
            raise SigninError('Signing in failed. Exiting.')

    def perform(self, action: str, table: str, **kwargs):
        """Select the method to perform and pass any keyword arguments"""
        match action:
            case 'add':
                self.add(table, **kwargs)
            case 'delete':
                self.delete(table, **kwargs)
            case 'update':
                self.update(table, **kwargs)

    def add(self, table: str, **kwargs) -> Response:
        """Add a term to an ontology table.
        """
        log.info(f"Adding to table {table}.")

        if table not in self.ontology_tables:
            raise NoSuchTableException(f"Table '{table}' not found in CatalogueOntologies.")

        _kwargs = self.__parse_kwargs(kwargs)
        _table = self.parse_table_name(table)

        query = Queries.insert(_table)
        variables = {"value": _kwargs}

        response = self.__perform_query(query, variables, action='add')

        return response

    def delete(self, table: str, **kwargs) -> Response:
        """Delete a term from an ontology."""
        log.info(f"Deleting from table {table}.")

        if table not in self.ontology_tables:
            raise NoSuchTableException(f"Table '{table}' not found in CatalogueOntologies.")

        _kwargs = self.__parse_kwargs(kwargs)
        _table = self.parse_table_name(table)

        query = Queries.delete(_table)
        variables = {"pkey": {'name': _kwargs['name']}}

        if _kwargs['name'] not in self.__list_ontology_terms(table):
            raise NoSuchNameException(f"Name '{_kwargs['name']}' not found in table '{table}'.")

        response = self.__perform_query(query, variables, action='delete')

        return response

    class Updater:
        """Class that handles the update of the terms, ontology tables, databases and database tables."""
        def __init__(self, manager, ontology_table: str, old: str, new: str):
            self.ontology_table = ontology_table
            self.manager = manager
            self.old = old
            self.new = new

            self.database = None
            self.table = None
            self.column = None

        def update(self) -> dict:
            """Perform the update."""

            # Iterate over the other databases on the server and check in which tables the ontology terms are referenced
            databases = self.manager.list_databases()
            server_dict = dict()
            for db in databases:
                self.database = db
                db_dict = self.__update_database(database=db)
                if len(db_dict.keys()) > 0:
                    server_dict.update({db: db_dict})

            return server_dict

        def __update_database(self, database: str) -> dict:
            """Update the tables in the database, replacing the 'old' term with the 'new' term.
            :param database: the name of the database
            """
            db_dict = dict()
            db_schema = self.manager.get_database_schema(database)

            for tb_name, tb_values in db_schema.items():
                self.table = tb_name
                if tb_values.get('externalSchema') != database:
                    continue
                tb_dict = self.__update_table(tb_name, tb_values)

                if len(tb_dict.keys()) > 0:
                    db_dict.update({tb_name: tb_dict})

            return db_dict

        def __update_table(self, tb_name: str, tb_values: dict) -> dict:
            """
            Update a data table in the database.
            """
            tb_dict = dict()

            tb_pkeys = [col for (col, col_values) in tb_values['columns'].items() if col_values.get('key') == 1]

            for col, col_values in tb_values['columns'].items():
                self.column = col
                if not (col_values.get('refSchema') == 'CatalogueOntologies'
                        and col_values.get('refTable') == self.ontology_table):
                    continue
                col_dict = self.__update_column(tb_pkeys)
                if len(col_dict) > 0:
                    tb_dict.update({col: col_dict})

            return tb_dict

        def __update_column(self, pkeys: list) -> list:
            """Update the values in the table column"""
            _table = self.manager.parse_table_name(self.table)
            query = Queries.column_values(_table, self.column, pkeys)
            variables = {'filter': {self.column: {'equals': {'name': self.old}}}}

            response = self.manager.client.session.post(
                f'{self.manager.client.url}/{self.database}/graphql',
                json={'query': query, 'variables': variables}
            )

            if response.status_code != 200:
                log.debug(f"Error {response.status_code} in database {self.database}, table {self.table}, "
                          f"column {self.column}.")
                log.debug(response.text)
                return []
            if len(response.json()['data']) == 0:
                return []
            column_values = response.json()['data'][_table]
            column_values_updated = [
                {col: (col_val
                       if col != self.column
                       else (([{'name': (item['name'] if item['name'] != self.old else self.new)
                                for item in col_val}]) if type(col_val) is list
                             else {'name': (col_val['name'] if col_val['name'] != self.old else self.new)}))
                 for (col, col_val) in row.items()}
                for row in column_values
            ]

            query = Queries.upload_mutation(_table)
            variables = {'value': column_values_updated}

            response = self.manager.client.session.post(
                f'{self.manager.client.url}/{self.database}/graphql',
                json={'query': query, 'variables': variables}
            )

            if response.status_code != 200:
                log.error(f"Update {self.old} to {self.new} in table {self.table} on database {self.database} failed.")
                log.debug(response.status_code)
                log.debug(response.text)
            else:
                log.info(f"Successfully updated term {self.old} to {self.new} in column {self.column}"
                         f" of table {self.table} on database {self.database} in {len(column_values)} rows.")
                return column_values_updated

    def update(self, table: str, **kwargs) -> dict:
        """Rename a term in an ontology."""
        ontology_table = table
        # TODO: do the actual update
        log.info(f"Renaming in table {ontology_table}.")

        if ontology_table not in self.ontology_tables:
            raise NoSuchTableException(f"Table '{ontology_table}' not found in CatalogueOntologies.")

        try:
            old, new = kwargs['old'], kwargs['new']
        except KeyError:
            raise UpdateItemsException("Specify 'old' and 'new' terms.")

        if old not in self.__list_ontology_terms(ontology_table):
            raise NoSuchNameException(f"Name '{old}' not found in table '{ontology_table}'.")
        if new not in self.__list_ontology_terms(ontology_table):
            raise NoSuchNameException(f"Name '{new}' not found in table '{ontology_table}'.")

        updater = self.Updater(self, ontology_table, old, new)
        update_results = updater.update()

        return update_results

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

    def list_databases(self) -> list:
        """Returns a list of the databases on the server."""
        query = Queries.list_databases()
        response = self.client.session.post(
            url=f'{self.client.url}/apps/graphql',
            json={"query": query}
        )
        databases = [db['name'] for db in response.json()['data']['_schemas']]
        return databases

    def get_database_schema(self, database: str) -> dict:
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
    def parse_table_name(table_name: str) -> str:
        """Parse table names to capitalized names without spaces,
        e.g. 'Network features' -> 'NetworkFeatures'.
        """
        if table_name == 'RWE resources':
            return 'RWEResources'
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
