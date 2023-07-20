
from .graphql import graphql
from . import utils as utils
import requests
import logging
import sys

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
        self.url = utils.parseUrl(url)
        self.api_graphql = self.url + "/apps/central/graphql"
        self.signin_status = 'unknown'

        self.session = requests.Session()

        if username is not None and password is not None:
            self.signin(username, password)

    def __str__(self):
        return self.url


    def signin(self, username: str, password: str):
        """Sign in to Molgenis and retrieve session cookie."""
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
            print(f"Server url {self.url} not available.")
            self.session.close()
            sys.exit()

        response_json: dict = response.json().get('data', {}).get('signin', {})
        
        if response_json.get('status') == 'SUCCESS':
            message = f"Success: Signed into {self.url} as {username}." 
            log.info(message)
            print(message)
            self.signin_status = 'success'
        elif response_json.get('status') == 'FAILED':
            log.error(message)
            print(f"Error: Unable to sign into {self.url} as {username}.\n{response_json.get('message')}")
            self.signin_status = 'failed'
        else:
            log.error('Error: sign in failed, exiting.')
            print(f"Unable to sign into {self.url} as {username}")
            self.signin_status = 'failed'

    def signout(self):
        """Signout
        Signout of the EMX2 instance
        
        @return status message; response
        """
        response = self.session.post(
            url=self.api_graphql,
            json={'query': graphql.signout()}
        )        
        
        data = response.json().get('data',{}).get('signout',{})
        if data.get('status') == 'SUCCESS':
            print(f"Signed out of {self.url}")
        else:
            error = data.get('message')
            print(f"Unable to sign out of {self.url}\n{error}")
            

    def list_databases(self):
        """List the databases present on the server."""
        query = """
        {
          _schemas {
            name
          }
        }"""

        response = self.session.post(
            url=f'{self.url}/apps/central/graphql',
            json={'query': query}
        )

        response_json: dict = response.json()

        databases = response_json['data']['_schemas']
        database_names = [db['name'] for db in databases]

        return database_names
    
    def _prep_data_or_file(self, file: str=None, data: list=None):
        if not bool(file) and not bool(data):
            print('No data to import. Specify a file location or a dataset.')
        
        if bool(file):
            return utils.readFile(file=file)
          
        if bool(data):
            return utils.toCsv(data = data)
    
    def add(self, schema: str, table: str, file: str=None, data: list=None):
        """Add
        Import or update records in a table of a named schema. 
        
        @param schema name of a schema
        @param table the name of the table
        @param file location of the file containing records to import or update
        @param data a dataset containing reocords to import or update (list of dictionaries)
        
        @return status message; response
        """
        importData = self._prep_data_or_file(file=file, data=data)
        response = self.session.post(
            url=f"{self.url}/{schema}/api/csv/{table}",
            headers={'Content-Type':'text/csv'},
            data = importData
        )
        
        if response.status_code == 200:
            log.info(f"Imported data into {schema}::{table}")
        else:
            errors = '\n'.join([err['message'] for err in response.json().get('errors')])
            log.error(f"Failed to import data into {schema}::{table}\n{errors}")

    def delete(self, schema: str, table: str, file: str=None, data: list=None):
        """Add
        Delete records from table.
        
        @param schema name of the schema
        @param table the name of the table
        @param file location of a file containing records to delete
        @param data dataset of records to delete (list of dictionaries)
        
        @return status message; response
        """
        importData = self._prep_data_or_file(file=file, data=data)
        response = self.session.delete(
            url = f"{self.url}/{schema}/api/csv/{table}",
            headers={'Content-Type':'text/csv'},
            data = importData
        )
        
        if response.status_code == 200:
            log.info(f"Deleted data from {schema}::{table}")
        else:
            errors = '\n'.join([err['message'] for err in response.json().get('errors')])
            log.error(f"Failed to delete data from {schema}::{table}\n{errors}")
    
    def get(self, schema: str, table: str, asDataFrame: bool=False):
        """Get
        Retrieve data from a schema and return as a list of dictionaries or as 
        a pandas DataFrame (as pandas is used to parse the response).
        
        @param schema name of the schema
        @param table name of the table
        @param asDataFrame if True, the response will be returned as a
          pandas DataFrame. Otherwise, a recordset will be returned.
        
        @return list of dictionaries; status message
        """
        if not bool(schema) or not bool(table):
            print('Incomplete table location. Enter a schema and table.')
        
        response = self.session.get(url=f"{self.url}/{schema}/api/csv/{table}")
        
        if response.status_code == 200:
            data = utils.parseCsvExport(content = response.text)
            return data if asDataFrame else data.to_dict('records')
        else:
            errors = '\n'.join([err['message'] for err in response.json().get('errors')])
            log.error(f"Failed to retrieve data from {schema}::{table}\n{errors}")
