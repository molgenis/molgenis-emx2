import requests
import logging
import sys
import os
import time

log = logging.getLogger(__name__)

class Client:
    """
    
    """

    def __init__(self, url: str, database: str, email: str, password: str) -> None:
        self.url = url
        self.database = database
        self.email = email
        self.password = password
        self.session = requests.Session()
        self.graphqlEndpoint = self.url + '/' + self.database + '/graphql'
        self.apiEndpoint = self.url + '/' + self.database + '/api'
        
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
            log.debug(f"Success: Signed into {self.database} as {self.email}")
        elif status == 'FAILED':
            log.error(message)
        else:
            log.error('Error: sign in failed, exiting.')

    def query(self, query, variables = {}, database = None):
        """Query backend"""

        if database is None:
            endpoint = self.graphqlEndpoint
        else:
            endpoint = self.url + '/' + database + '/graphql'

        response = self.session.post(
            endpoint,
            json={"query": query, "variables": variables}
        )
                                
        if response.status_code != 200:
            log.error(f"Error while posting query, status code {response.status_code}")
            # TODO: add logging content > errors > message
        else: 
          return response.json()['data']

    def delete(self, table, pkey):
        """Delete row by key"""

        query = (""
            "mutation delete($pkey:[" + table + "Input]) {"
                "delete(" + table + ":$pkey){message}"
            "}"
        "")

        variables =  {'pkey': pkey}

        response = self.session.post(
            self.graphqlEndpoint,
            json={'query': query, 'variables': variables}
        )

        if response.status_code != 200:
            log.error(response)
            log.error(f"Error uploading csv, response.text: {response.text}")

        return response

    def add(self, table, data={}, draft=False):
        """Add record"""

        query = (""
            "mutation insert($value:[" + table + "Input]) {"
                "insert(" + table + ":$value){message}"
            "}"
        "")

        data['mg_draft'] = draft

        variables = {"value": [data]}

        response = self.session.post(
            self.graphqlEndpoint,
            json={'query': query, 'variables': variables}
        )

        if response.status_code != 200:
            log.error(f"Error while adding record, status code {response.status_code}")
            log.error(response)

        return response

    def addSetting(self, key, value):
        """Add setting to schema"""

        query = (""
            "mutation change($settings:[MolgenisSettingsInput]) {"
                " change(settings: $settings) {message}"
            "}"
        "")

        variables = {'settings': {'key': key, 'value': value}}

        response = self.session.post(
            self.graphqlEndpoint,
            json={'query': query, 'variables': variables}
        )

        if response.status_code != 200:
            log.error(f"Error while adding setting, status code {response.status_code}")
            log.error(response)

        return response
    
    def fields(self, table): 
        """ Fetch a field list as json array of name value pairs"""

        query = '{__type(name:"' + table + '") {fields { name } } }'

        response = self.session.post(self.graphqlEndpoint, json={'query': query} )

        if response.status_code != 200:
            log.error(f"Error while fetching table fields, status code {response.status_code}")
            log.error(response)

        return response.json()['data']['__type']['fields']

    def uploadCSV(self, table=None, data=None, database=None):
        """ Upload csv data ( string ) to table """

        if database is None:
            endpoint = self.apiEndpoint + '/csv'
        else:
            endpoint = self.url + '/' + database + '/api/csv'

        if table is not None:
            endpoint = endpoint + '/' + table

        response = self.session.post(
            endpoint,
            headers={"Content-Type": 'text/csv'},
            data=data
        )

        if response.status_code != 200:
            log.error(response)
            log.error(f"Error uploading csv, status code {response.text}")

        return response

    def downLoadCSV(self, table):
        """ Download csv data from table """
        resp = self.session.get(self.apiEndpoint + '/csv/'+ table, allow_redirects=True)
        if resp.content:
            return resp.content
        else:
            log.error('Error: download failed')
            
    def upload_zip(self, data) -> None:
        """ Upload zip """
        response = self.session.post(
            self.apiEndpoint + '/zip?async=true',
            files={'file': ('zip.zip', data.getvalue())},
        )

        def upload_zip_task_status(self, response) -> None:
            task_response = self.session.get(self.url + response.json()['url'])

            if task_response.json()['status'] == 'COMPLETED':
                log.info(f"{task_response.json()['status']}, {task_response.json()['description']}")
                return

            if task_response.json()['status'] == 'ERROR':
                log.error(f"{task_response.json()['status']}, {task_response.json()['description']}")
                return
            
            if task_response.json()['status'] == 'RUNNING':
                log.info(f"{task_response.json()['status']}, {task_response.json()['description']}")
                time.sleep(5)
                upload_zip_task_status(self, response)
        
        upload_zip_task_status(self, response)

    def download_zip(self) -> bytes:
        """ Download zip data from database """
        resp = self.session.get(self.apiEndpoint + '/zip', allow_redirects=True)
        if resp.content:
            return resp.content
        else:
            log.error('Error: download failed')
    
    def database_exists(self) -> None:
        """ Check if database exists on server, otherwise complain and exit """
        query = '{_session {schemas} }'

        response = self.session.post(self.graphqlEndpoint, json={'query': query} )
        if response.status_code != 200:
            log.error(f"Database schema does not exist, status code {response.status_code}")
            sys.exit()

    # Admin only function, do not use database from self
    def list_schemas(self):
        """ List schemas visible to this client """
        query = '{_session {schemas} }'

        response = self.session.post(
            self.url + '/apps/central/graphql',
            json={'query': query}
        )
        if response.status_code != 200:
            log.error(
                f"Failed to list schemas, status code {response.status_code}")
            sys.exit()
        
        return response.json()['data']['_session']['schemas']

    def post_gql_to_db(self, databaseName, gql, variables={}, path='/graphql'):
        """ Post a given gql statement to a given database  """

        response = self.session.post(
            self.url + '/' + databaseName + path,
            json={"query": gql, "variables": variables}
        )
        if response.status_code != 200:
            log.error(
                f"Failed to post gql to db ({databaseName}), status code {response.status_code}")
        else:
            log.info(
                f"Successfully posted gql to db ({databaseName}), msg {response.json()['data']['change']['message']}")
