import logging
import sys

import requests

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
        self.url = self.__parse_url(url)
        self.signin_status = 'unknown'

        self.session = requests.Session()

        if username is not None and password is not None:
            self.signin(username, password)

    def __str__(self):
        return self.url

    @staticmethod
    def __parse_url(raw_url: str):
        """Ensure the url is in correct format."""
        if not raw_url.startswith('https://'):
            clean_url = 'https://' + raw_url
            return clean_url
        return raw_url

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

        response_json: dict = response.json()

        status: str = response_json['data']['signin']['status']
        message: str = response_json['data']['signin']['message']

        if status == 'SUCCESS':
            log.debug(f"Success: Signed into {self.url} as {username}.")
            self.signin_status = 'success'
        elif status == 'FAILED':
            log.error(message)
            self.signin_status = 'failed'
        else:
            log.error('Error: sign in failed, exiting.')
            self.signin_status = 'failed'

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
