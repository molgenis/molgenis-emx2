import logging

import requests

log = logging.getLogger(__name__)


class Client:
    """

    """
    def __init__(self, url: str, database: str, email: str, password: str) -> None:
        self.url = url
        self.database = database

        self.session = requests.Session()
        self.graphqlEndpoint = f'{self.url}/{self.database}/graphql'
        self.apiEndpoint = f'{self.url}/{self.database}/api'

        self.signin(email, password)

    def signin(self, email: str, password: str):
        """Sign in to Molgenis and retrieve session cookie."""
        query = """
          mutation($email:String, $password: String) {
            signin(email: $email, password: $password) {
              status
              message
            }
          }
        """

        variables = {'email': email, 'password': password}

        response = self.session.post(
            url=f'{self.url}/apps/central/graphql',
            json={'query': query, 'variables': variables}
        )

        response_json: dict = response.json()

        status: str = response_json['data']['signin']['status']
        message: str = response_json['data']['signin']['message']

        if status == 'SUCCESS':
            log.debug(f"Success: Signed into {self.database} as {email}.")
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
