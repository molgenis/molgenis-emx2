import csv
import io
import logging
from typing import TypeAlias, Literal

import pandas as pd
import requests

from . import graphql_queries as queries
from . import utils as utils
from .exceptions import NoSuchSchemaException, ServiceUnavailableError, SigninError, ServerNotFoundError, \
    PyclientException, NoSuchTableException, NoContextManagerException, GraphQLException

log = logging.getLogger("Molgenis EMX2 Pyclient")

OutputFormat: TypeAlias = Literal['csv', 'xlsx']


class Client:
    """
    Use the Client object to log in to a Molgenis server and perform operations on the server.
    Specify a default schema
    """
    def __init__(self, url: str, schema: str = None) -> None:
        """
        A Client class instances is created with a server url.
        """
        self._as_context_manager = False
        self.url: str = utils.parse_url(url)
        self.api_graphql = self.url + "/api/graphql"

        self.signin_status: str = 'unknown'
        self.username: str | None = None

        self.session: requests.Session = requests.Session()

        self.schemas: list = self.get_schemas()
        self.default_schema: str = self._set_schema(schema)

    def __str__(self):
        return self.url

    def __enter__(self):
        self._as_context_manager = True
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if exc_type or exc_val or exc_tb:
            print(exc_type, exc_val, exc_tb, sep="\n")
        self.signout()
        self.session.close()

    async def __aenter__(self):
        self._as_context_manager = True
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if exc_type or exc_val or exc_tb:
            print(exc_type, exc_val, exc_tb, sep="\n")
        self.signout()
        self.session.close()

    def signin(self, username: str, password: str):
        """Signs in to Molgenis and retrieves session cookie.

        :param username: the username or email address for an account on this server
        :type username: str
        :param password: the password corresponding to this username.
        :type username: str
        """
        self.username = username

        if not self._as_context_manager:
            raise NoContextManagerException("Ensure the Client is called as a context manager,\n"
                                            "e.g. `with Client(url) as client:`")
        query = queries.signin()
        variables = {'email': self.username, 'password': password}

        response = self.session.post(
            url=self.api_graphql,
            json={'query': query, 'variables': variables}
        )

        if response.status_code == 404:
            raise ServerNotFoundError(f"Server '{self.url}' could not be found. "
                                      f"Ensure the spelling of the url is correct.")
        if response.status_code == 503:
            raise ServiceUnavailableError(f"Server '{self.url}' not available. "
                                          f"Try again later.")
        if response.status_code != 200:
            raise PyclientException(f"Server '{self.url}' could not be reached due to a connection problem."
                                    f"\nStatus code: {response.status_code}. Reason: '{response.reason}'.")

        response_json: dict = response.json().get('data', {}).get('signin', {})
        
        if response_json.get('status') == 'SUCCESS':
            self.signin_status = 'success'
            message = f"User '{self.username}' is signed in to '{self.url}'."
            log.info(message)
            print(message)
        elif response_json.get('status') == 'FAILED':
            self.signin_status = 'failed'
            message = f"Error: Unable to sign in to {self.url} as {self.username}." \
                      f"\n{response_json.get('message')}"
            log.error(message)
            raise SigninError(message)
        else:
            self.signin_status = 'failed'
            message = f"Error: Unable to sign in to {self.url} as {self.username}." \
                      f"\n{response_json.get('message')}"
            log.error(message)
            raise SigninError(message)
        self.schemas = self.get_schemas()
        
    def signout(self):
        """Signs the client out of the EMX2 server."""
        response = self.session.post(
            url=self.api_graphql,
            json={'query': queries.signout()}
        )

        status = response.json().get('data', {}).get('signout', {}).get('status')
        if status == 'SUCCESS':
            print(f"User '{self.username}' is signed out of '{self.url}'.")
            self.signin_status = 'signed out'
        else:
            print(f"Unable to sign out of {self.url}.")
            message = response.json().get('errors')[0].get('message')
            print(message)
            
    @property
    def status(self):
        """View client information"""
        schemas = '\n\t'.join(self.schema_names)
        message = (
          f"Host: {self.url}\n"
          f"User: {self.username}\n"
          f"Status: {'Signed in' if self.signin_status == 'success' else 'Signed out'}\n"
          f"Schemas: \n\t{schemas}\n"
          f"Version: {self.version}\n"
        )
        return message

    def get_schemas(self):
        """Returns the schemas on the database for this user as a list of dictionaries
        containing for each schema the id, name, label and description.
        """
        query = queries.list_schemas()

        response = self.session.post(
            url=self.api_graphql,
            json={'query': query}
        )

        response_json: dict = response.json()
        schemas = response_json['data']['_schemas']
        return schemas

    @property
    def schema_names(self):
        """Returns a list of the names of the schemas."""
        return [schema['name'] for schema in self.schemas]

    @property
    def version(self):
        """List the current EMX2 version on the server"""
        query = queries.version_number()
        response = self.session.post(
            url=self.api_graphql,
            json={'query': query}
        )
        return response.json().get('data').get('_manifest').get('SpecificationVersion')
    
    @staticmethod
    def _prep_data_or_file(file_path: str = None, data: list = None) -> str:
        """Prepares the data from memory or loaded from disk for addition or deletion action.

        :param file_path: path to the file to be prepared
        :type file_path: str
        :param data: data to be prepared
        :type data: list

        :returns: prepared data in dataframe format
        :rtype: pd.DataFrame
        """
        if file_path is None and data is None:
            print("No data to import. Specify a file location or a dataset.")
        
        if file_path is not None:
            return utils.read_file(file_path=file_path)
          
        if data is not None:
            return pd.DataFrame(data).to_csv(index=False, quoting=csv.QUOTE_NONNUMERIC, encoding='UTF-8')

    def _set_schema(self, schema: str) -> str:
        """Sets the default schema to the schema supplied as argument.
        Raises NoSuchSchemaException if the schema cannot be found on the server.
        
        :param schema: name of a schema
        :type schema: str
        
        :returns: a schema name
        :rtype: str
        """
        if schema not in [*self.schema_names, None]:
            raise NoSuchSchemaException(f"Schema '{schema}' not found on server.")
        self.default_schema = schema

        return schema
    
    @staticmethod
    def _graphql_validate_response(response_json: dict, mutation: str, fallback_error_message: str):
        """Validates a GraphQL response and print the appropriate message
        
        :param response_json: a graphql response from the server
        :type response_json: dict
        :param mutation: the name of the graphql mutation executed
        :type mutation: str
        :param fallback_error_message: a fallback error message
        :type fallback_error_message: str
      
        :returns: a success or error message
        :rtype: string
        """
        response_keys = response_json.keys()
        if 'error' not in response_keys and 'data' not in response_keys:
            message = fallback_error_message
            log.error(message)
            print(message)

        elif 'error' in response_keys:
            message = response_json.get('error').errors[0].get('message')
            log.error(message)
            raise GraphQLException(message)
      
        else:
            if response_json.get('data').get(mutation).get('status') == 'SUCCESS':
                message = response_json.get('data').get(mutation).get('message')
                log.info(message)
                print(message)
            else:
                message = f"Failed to validate response for {mutation}"
                log.error(message)
                print(message)
            
    @staticmethod
    def _format_optional_params(params: dict = None):
        keys = params.keys()
        args = {key: params[key] for key in keys if (key != 'self') and (key is not None)}
        if 'schema' in args.keys():
            args['name'] = args.pop('schema')
        if 'include_demo_data' in args.keys():
            args['includeDemoData'] = args.pop('include_demo_data')
        return args

    def _table_in_schema(self, table: str, schema: str) -> bool:
        """Checks whether the requested table is present in the schema.

        :param table: the name of the table
        :type table: str
        :param schema: the name of the schema
        :type schema: str
        :returns: boolean indicating whether table is present
        :rtype: bool
        """
        response = self.session.post(
            url=f"{self.url}/{schema}/graphql",
            json={'query': queries.list_tables()}
        )
        schema_tables = [tab['name'] for tab in
                         response.json().get('data').get('_schema').get('tables')]
        return table in schema_tables
    
    def save(self, schema: str = None, table: str = None, file: str = None, data: list = None):
        """Imports or updates records in a table of a named schema.
        
        :param schema: name of a schema
        :type schema: str
        :param table: the name of the table
        :type table: str
        :param file: location of the file containing records to import or update
        :type file: str
        :param data: a dataset containing records to import or update (list of dictionaries)
        :type data: list
        
        :returns: status message or response
        :rtype: str
        """
        current_schema = schema
        if current_schema is None:
            current_schema = self.default_schema

        if current_schema not in self.schema_names:
            raise NoSuchSchemaException(f"Schema '{current_schema}' not found on server.")

        if not self._table_in_schema(table, current_schema):
            raise NoSuchTableException(f"Table '{table}' not found in schema '{current_schema}'.")

        import_data = self._prep_data_or_file(file_path=file, data=data)
        
        response = self.session.post(
            url=f"{self.url}/{current_schema}/api/csv/{table}",
            headers={'Content-Type': 'text/csv'},
            data=import_data
        )
        
        if response.status_code == 200:
            log.info(f"Imported data into {current_schema}::{table}.")
        else:
            errors = '\n'.join([err['message'] for err in response.json().get('errors')])
            log.error(f"Failed to import data into {current_schema}::{table}\n{errors}.")

    def delete(self, schema: str = None, table: str = None, file: str = None, data: list = None):
        """Deletes records from table.
        
        :param schema: name of a schema
        :type schema: str
        :param table: the name of the table
        :type table: str
        :param file: location of the file containing records to import or update
        :type file: str
        :param data: a dataset containing records to import or update (list of dictionaries)
        :type data: list

        :returns: status message or response
        :rtype: str
        """
        current_schema = schema
        if current_schema is None:
            current_schema = self.default_schema

        if current_schema not in self.schema_names:
            raise NoSuchSchemaException(f"Schema '{current_schema}' not found on server.")

        if not self._table_in_schema(table, current_schema):
            raise NoSuchTableException(f"Table '{table}' not found in schema '{current_schema}'.")

        import_data = self._prep_data_or_file(file_path=file, data=data)
        
        response = self.session.delete(
            url=f"{self.url}/{current_schema}/api/csv/{table}",
            headers={'Content-Type': 'text/csv'},
            data=import_data
        )
        
        if response.status_code == 200:
            log.info(f"Deleted data from {current_schema}::{table}.")
        else:
            errors = '\n'.join([err['message'] for err in response.json().get('errors')])
            log.error(f"Failed to delete data from {current_schema}::{table}\n{errors}.")
    
    def get(self, schema: str = None, table: str = None, as_df: bool = False) -> list | pd.DataFrame:
        """Retrieves data from a schema and returns as a list of dictionaries or as
        a pandas DataFrame (as pandas is used to parse the response).
        
        :param schema: name of a schema
        :type schema: str
        :param table: the name of the table
        :type table: str
        :param as_df: if True, the response will be returned as a
                      pandas DataFrame. Otherwise, a recordset will be returned.
        :type as_df: bool
        
        :returns: list of dictionaries, status message or data frame
        :rtype: list | pd.DataFrame
        """
        current_schema = schema
        if current_schema is None:
            current_schema = self.default_schema
        
        if current_schema not in self.schema_names:
            raise NoSuchSchemaException(f"Schema '{current_schema}' not found on server.")

        if not self._table_in_schema(table, current_schema):
            raise NoSuchTableException(f"Table '{table}' not found in schema '{current_schema}'.")

        response = self.session.get(url=f"{self.url}/{current_schema}/api/csv/{table}")

        if response.status_code != 200:
            message = f"Failed to retrieve data from '{current_schema}::{table}'." \
                      f"\nStatus code: {response.status_code}."
            log.error(message)
            raise PyclientException(message)

        response_data = pd.read_csv(io.BytesIO(response.content))

        if not as_df:
            return response_data.to_dict('records')
        return response_data

    def export(self, schema: str = None, table: str = None, fmt: OutputFormat = 'csv'):
        """Export data from a schema to a file in the desired format.
        
        :param schema: the name of the schema
        :type schema: str
        :param table: the name of the table
        :type table: str
        :param fmt: the export format of the schema and or table (csv or xlsx)
        :type fmt: str
        
        """
        current_schema = schema
        if current_schema is None:
            current_schema = self.default_schema

        if current_schema not in self.schema_names:
            raise NoSuchSchemaException(f"Schema '{current_schema}' not found on server.")

        if table is not None and not self._table_in_schema(table, current_schema):
            raise NoSuchTableException(f"Table '{table}' not found in schema '{current_schema}'.")

        if fmt == 'xlsx':
            if table is None:
                # Export the whole schema
                url = f"{self.url}/{current_schema}/api/excel"
                response = self.session.get(url)

                filename = f"{current_schema}.xlsx"
                with open(filename, "wb") as f:
                    f.write(response.content)
                log.info(f"Exported data from schema {current_schema} to '{filename}'.")
            else:
                # Export the single table
                url = f"{self.url}/{current_schema}/api/excel/{table}"
                response = self.session.get(url)

                filename = f"{table}.xlsx"
                with open(filename, "wb") as f:
                    f.write(response.content)
                log.info(f"Exported data from table {table} in schema {current_schema} to '{filename}'.")

        if fmt == 'csv':
            if table is None:
                url = f"{self.url}/{current_schema}/api/zip"
                response = self.session.get(url)

                filename = f"{current_schema}.zip"
                with open(filename, "wb") as f:
                    f.write(response.content)
                log.info(f"Exported data from schema {current_schema} to '{filename}'.")
            else:
                # Export the single table
                url = f"{self.url}/{current_schema}/api/csv/{table}"
                response = self.session.get(url)

                filename = f"{table}.csv"
                with open(filename, "wb") as f:
                    f.write(response.content)
                log.info(f"Exported data from table {table} in schema {current_schema} to '{filename}'.")

    def create_schema(self, schema: str = None, description: str = None, template: str = None,
                      include_demo_data: bool = None):
        """Create a new schema
        
        :param schema: the name of the new schema
        :type schema: str
        :param description: additional text that provides context for a schema
        :type description: str
        :param template: (optional) the name of a template to set as the schema
        :type template: str
        :param include_demo_data: If true and a template schema is selected, 
                                any example data will be loaded into the schema
        :type include_demo_data: bool
        
        :returns: a success or error message
        :rtype: string
        """
        query = queries.create_schema()
        variables = self._format_optional_params(params=locals())
        
        response = self.session.post(
           url=f"{self.url}/api/graphql",
           json={'query': query, 'variables': variables}
        )
        
        response_json = response.json()
        self._graphql_validate_response(
            response_json=response_json,
            mutation='createSchema',
            fallback_error_message=f"Failed to create schema '{schema}'"
        )
        self.schemas = self.get_schemas()
              
    def delete_schema(self, schema: str = None):
        """Delete a schema
        
        :param schema: the name of the new schema
        :type schema: str
        
        :returns: a success or error message
        :rtype: string
        """
        query = queries.delete_schema()
        variables = {'id': schema}
        response = self.session.post(
            url=f"{self.url}/api/graphql",
            json={'query': query, 'variables': variables}
        )
        
        response_json = response.json()
        self._graphql_validate_response(
            response_json=response_json,
            mutation='deleteSchema',
            fallback_error_message=f"Failed to delete schema '{schema}'"
        )
        self.schemas = self.get_schemas()

    def update_schema(self, schema: str = None, description: str = None):
        """Update a schema's description
        
        :param schema: the name of the new schema
        :type schema: str
        :param description: additional text that provides context for a schema
        :type description: str
        
        :returns: a success or error message
        :rtype: string
        """
        query = queries.update_schema()
        variables = {'name': schema, 'description': description}
        response = self.session.post(
            url=f"{self.url}/api/graphql",
            json={'query': query, 'variables': variables}
        )
        
        response_json = response.json()
        self._graphql_validate_response(
            response_json=response_json,
            mutation='updateSchema',
            fallback_error_message=f"Failed to update schema '{schema}'"
        )
        self.schemas = self.get_schemas()
                
    def recreate_schema(self, schema: str = None, description: str = None, template: str = None,
                        include_demo_data: bool = None):
        """Recreate a schema
        
        :param schema: the name of the new schema
        :type schema: str
        :param description: additional text that provides context for a schema
        :type description: str
        :param template: (optional) the name of a template to set as the schema
        :type template: str
        :param include_demo_data: If true and a template schema is selected,
                                any example data will be loaded into the schema
        :type include_demo_data: bool
        
        :returns: a success or error message
        :rtype: string
        """
        if schema not in self.schema_names:
            message = f"Schema '{schema}' does not exist"
            log.error(message)
            raise NoSuchSchemaException(message)
        
        schema_meta = [db for db in self.schemas if db['name'] == schema][0]
        schema_description = description if description else schema_meta.get('description', None)

        try:
            self.delete_schema(schema=schema)
            self.create_schema(
                schema=schema,
                description=schema_description,
                template=template,
                include_demo_data=include_demo_data
            )

        except GraphQLException:
            message = f"Failed to recreate '{schema}'"
            log.error(message)
            print(message)

        self.schemas = self.get_schemas()
        
    def get_schema_metadata(self, schema: str = None):
        """Retrieve a schema's metadata
        
        :param schema: the name of the new schema
        :type schema: str
        
        :returns: schema metadata
        :rtype: dict
        """
        current_schema = schema if schema is not None else self.default_schema
        if current_schema not in self.schema_names:
            raise NoSuchSchemaException(f"Schema '{current_schema}' not found on server.")
        
        query = queries.list_schema_meta()
        response = self.session.post(
           url=f"{self.url}/{current_schema}/api/graphql",
           json={'query': query}
        )
        
        response_json = response.json()

        if 'id' not in response_json.get('data').get('_schema'):
          message = f"Unable to retrieve metadata for schema '{current_schema}'"
          log.error(message)
          raise GraphQLException(message)
        
        metadata = response_json.get('data').get('_schema')
        return metadata
        
        
        
        