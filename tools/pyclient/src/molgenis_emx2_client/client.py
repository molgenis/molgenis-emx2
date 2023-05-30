import logging

import requests

log = logging.getLogger(__name__)


class Client:
    """Use the Client object to log in to a Molgenis server and perform operations on the server.
    """
    def __init__(self, url: str, username: str, password: str) -> None:
        """
        A Client class instances is created with a server url, username and password.
        The object starts a network session and logs in to the server using the login credentials.
        """
        self.url = url

        self.session = requests.Session()

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

        response_json: dict = response.json()

        status: str = response_json['data']['signin']['status']
        message: str = response_json['data']['signin']['message']

        if status == 'SUCCESS':
            log.debug(f"Success: Signed into {self.url} as {username}.")
        elif status == 'FAILED':
            log.error(message)
        else:
            log.error('Error: sign in failed, exiting.')

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
