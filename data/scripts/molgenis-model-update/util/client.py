import os
import sys
import logging
import requests

log = logging.getLogger(__name__)


class Session:
    """
    """

    def __init__(self, url, email, password):
        self.url = url
        self.email = email
        self.password = password
        self.session = requests.Session()

        self.signin(self.email, self.password)

    def signin(self, email, password):
        """Sign into molgenis and retrieve session cookie"""
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
            self.url + '/apps/central/graphql',
            json={'query': query, 'variables': variables}
        )

        responseJson = response.json()

        status = responseJson['data']['signin']['status']
        message = responseJson['data']['signin']['message']

        if status == 'SUCCESS':
            self.cookies = response.cookies
            log.debug(f"Success: Signed into {self.url} as {self.email}")
        elif status == 'FAILED':
            log.error(message)
        else:
            log.error('Error: sign in failed, exiting.')

    def download_zip(self, database_name):
        """Download molgenis zip for given Database."""
        response = requests.get(
            self.url + database_name + '/api/zip?includeSystemColumns=true',
            auth=(self.email, self.password),
            allow_redirects=True,
            cookies=self.cookies
        )

        if response.content:
            fh = open('./files/' + database_name + '_data.zip', 'wb')
            fh.write(response.content)
            fh.close()
        else:
            print('Error: download failed, did you use the correct credentials?')
            exit(1)

    def upload_zip(self, database_name, data_to_upload):
        """Upload molgenis zip to fill Database"""

        query = 'mutation{signin(email: "%s", password: "%s"){status,message}}' % (self.email, self.password)
        response = requests.post(
            self.url + 'apps/graphql-playground/graphql',
            json={'query': query}
        )

        self.cookies = response.cookies

        zip = {'file': open('./files/' + data_to_upload + '_upload.zip', 'rb')}
        response = requests.post(
            self.url + database_name + '/api/zip?async=true',
            auth=(self.email, self.password),
            allow_redirects=True,
            cookies=self.cookies,
            files=zip
        )

        zip['file'].close()

        responseJson = response.json()
        try:
            id = responseJson['id']
            url = responseJson['url']
            print(f'Upload successful, id: {id}, url: {url}')
        except:
            errors = responseJson['errors'][0]
            print(f'Upload failed: {errors}')
        finally:
            try:
                if database_name not in ['UMCG', 'catalogue', 'DataCatalogue']:  # otherwise data to upload >>
                    # << deleted when schema is uploaded
                    if os.path.exists(database_name + '_upload.zip'):
                        os.remove(database_name + '_upload.zip')
            except PermissionError:
                sys.exit('Error deleting upload.zip')

    def get_database_description(self, database_name) -> str:
        """ Get description of database
        """
        graphqlEndpoint_server = self.url + '/apps/central/graphql'

        # get description for database
        query = '{_schemas {name description}}'
        variables = {'name': database_name}

        response = self.session.post(graphqlEndpoint_server, json={'query': query,
                                                                   'variables': variables})
        json_response = response.json()
        schema_list = json_response.get('data').get('_schemas')
        description = [s['description'] for s in schema_list if 'description' in s and s['name'] == database_name]
        if not description:
            database_description = ''
        else:
            database_description = description[0]

        return database_description

    def drop_database(self, database_name) -> None:
        """ Delete database """
        graphqlEndpoint = self.url + database_name + '/graphql'

        # delete database
        # see if schema exists before deleting it
        query = '{_session {schemas} }'
        response = self.session.post(graphqlEndpoint, json={'query': query})

        if response.status_code == 200:
            query = """
            mutation deleteSchema($name:String){
                deleteSchema(name:$name) {
                    message
                }
            }
            """

            variables = {'name': database_name}

            response = self.session.post(
                self.url + '/api/graphql',
                json={'query': query, 'variables': variables}
            )
            print(response.json())
            if response.json()['data']['deleteSchema']['message']:
                log.info(response.json()['data']['deleteSchema']['message'])
        else:
            log.warning(f"Database schema does not exist, status code {response.status_code}")

    def create_database(self, database_name, database_description) -> None:
        """ Create TARGET database if it doesn't exists"""
        graphqlEndpoint = self.url + database_name + '/graphql'

        query = '{_session {schemas} }'

        response = self.session.post(graphqlEndpoint, json={'query': query})
        if response.status_code != 200:

            query = """
                mutation createSchema($name:String, $description:String){
                    createSchema(name:$name, description:$description){
                        message
                    }
                }
                """
            variables = {'name': database_name, 'description': database_description}

            response = self.session.post(
                self.url + '/apps/central/graphql',
                json={'query': query, 'variables': variables}
            )
            print(response.json())

            if response.json()['data']['createSchema']['message']:
                log.info(response.json()['data']['createSchema']['message'])
