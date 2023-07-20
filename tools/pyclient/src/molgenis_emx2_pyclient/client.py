
import logging
import sys

import pandas as pd
import requests

from . import utils as utils
from . import graphql_queries as queries
from .exceptions import NoSuchSchemaException

log = logging.getLogger(__name__)


class Client:
    """
    Use the Client object to log in to a Molgenis server and perform operations on the server.
    """
    def __init__(self, url: str, username: str = None, password: str = None) -> None:
        """
        A Client class instances is created with a server url, username and password.
        The object starts a network session and logs in to the server using the login credentials.
        """
        self.url = utils.parse_url(url)
        self.api_graphql = self.url + "/apps/central/graphql"
        self.signin_status = 'unknown'

        self.schemas: list = []

        self.session = requests.Session()

        # Sign in when user credentials are supplied
        if username is not None and password is not None:
            self.sign_in(username, password)

    def __str__(self):
        return self.url

    def sign_in(self, username: str, password: str):
        """Signs in to Molgenis and retrieves session cookie.

        :param username: the username or email address for an account on this server
        :type username: str
        :param password: the password corresponding to this username.
        :type username: str
        """
        query = """
          mutation($email:String, $password: String) {
            signin(email: $email, password: $password) {
              status
              message
            }
          }
        """

        variables = {'email': username, 'password': password}

        response = self.session.post(
            url=f'{self.url}/apps/central/graphql',
            json={'query': query, 'variables': variables}
        )

        if response.status_code == 503:
            print(f"Server '{self.url}' not available.")
            self.session.close()
            sys.exit()

        response_json: dict = response.json().get('data', {}).get('signin', {})
        
        if response_json.get('status') == 'SUCCESS':
            message = f"Success: Signed into {self.url} as {username}." 
            log.info(message)
            print(message)
            self.signin_status = 'success'
        elif response_json.get('status') == 'FAILED':
            message = f"Error: Unable to sign into {self.url} as {username}.\n{response_json.get('message')}"
            log.error(message)
            print(message)
            self.signin_status = 'failed'
        else:
            log.error(f"Error: sign in failed, exiting. Sign in status: {response_json.get('status')}")
            print(f"Unable to sign into {self.url} as {username}")
            self.signin_status = 'failed'

        self.schemas = self.list_schemas()

    def sign_out(self):
        """Signs the client out of the EMX2 server."""
        response = self.session.post(
            url=self.api_graphql,
            json={'query': queries.sign_out()}
        )        
        
        status = response.json().get('data', {}).get('signout', {}).get('status')
        message = response.json().get('data', {}).get('signout', {}).get('message')
        if status == 'SUCCESS':
            print(f"Signed out of {self.url}")
        else:
            print(f"Unable to sign out of {self.url}.")
            print(message)

    def list_schemas(self):
        """List the databases present on the server."""
        query = queries.list_schemas()

        response = self.session.post(
            url=f'{self.url}/apps/central/graphql',
            json={'query': query}
        )

        response_json: dict = response.json()

        databases = response_json['data']['_schemas']
        database_names = [db['name'] for db in databases]

        return database_names
    
    @staticmethod
    def _prep_data_or_file(file: str = None, data: list = None) -> pd.DataFrame:
        """Prepares the data from memory or loaded from disk for addition or deletion action.

        :param file: path to the file to be prepared
        :type file: str
        :param data: data to be prepared
        :type data: list

        :returns: prepared data in dataframe format
        :rtype: pd.DataFrame
        """
        if not bool(file) and not bool(data):
            print('No data to import. Specify a file location or a dataset.')
        
        if bool(file):
            return utils.read_file(file=file)
          
        if bool(data):
            return utils.to_csv(data=data)
    
    def add(self, schema: str, table: str, file: str = None, data: list = None):
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
        import_data = self._prep_data_or_file(file=file, data=data)

        if schema not in self.schemas:
            raise NoSuchSchemaException(f"Schema '{schema}' not found on server.")

        response = self.session.post(
            url=f"{self.url}/{schema}/api/csv/{table}",
            headers={'Content-Type': 'text/csv'},
            data=import_data
        )
        
        if response.status_code == 200:
            log.info(f"Imported data into {schema}::{table}.")
        else:
            errors = '\n'.join([err['message'] for err in response.json().get('errors')])
            log.error(f"Failed to import data into {schema}::{table}\n{errors}.")

    def delete(self, schema: str, table: str, file: str = None, data: list = None):
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
        import_data = self._prep_data_or_file(file=file, data=data)

        if schema not in self.schemas:
            raise NoSuchSchemaException(f"Schema '{schema}' not found on server.")

        response = self.session.delete(
            url=f"{self.url}/{schema}/api/csv/{table}",
            headers={'Content-Type': 'text/csv'},
            data=import_data
        )
        
        if response.status_code == 200:
            log.info(f"Deleted data from {schema}::{table}.")
        else:
            errors = '\n'.join([err['message'] for err in response.json().get('errors')])
            log.error(f"Failed to delete data from {schema}::{table}\n{errors}.")
    
    def get(self, schema: str, table: str, as_df: bool = False) -> list | pd.DataFrame:
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
        if not bool(schema) or not bool(table):
            print("Incomplete table location. Enter a schema and table.")

        if schema not in self.schemas:
            raise NoSuchSchemaException(f"Schema '{schema}' not found on server.")
        
        response = self.session.get(url=f"{self.url}/{schema}/api/csv/{table}")
        
        if response.status_code == 200:
            data = utils.parse_csv_export(content=response.text)
            return data if as_df else data.to_dict('records')
        elif response.status_code == 404:
            log.error(f"Failed to retrieve data from '{schema}::{table}'.")
        else:
            errors = '\n'.join([err['message'] for err in response.json().get('errors')])
            log.error(f"Failed to retrieve data from {schema}::{table}\n{errors}")
