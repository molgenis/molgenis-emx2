import requests
import logging
import sys
import os
import time

log = logging.getLogger(__name__)


class Session:
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

        self.sign_in(self.email, self.password)

    def sign_in(self, email, password):
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

    def query(self, query, variables={}):
        """Query backend"""

        response = self.session.post(
            self.graphqlEndpoint,
            json={"query": query, "variables": variables}
        )

        if response.status_code != 200:
            log.error(f"Error while posting query, status code {response.status_code}")
            # TODO: add logging content > errors > message
            sys.exit()

        return response.json()['data']

    def delete(self, table, pkey):
        """Delete row by key"""

        query = (""
                 "mutation delete($pkey:[" + table + "Input]) {"
                                                     "delete(" + table + ":$pkey){message}"
                                                                         "}"
                                                                         "")

        stepSize = 1000  # to make sure list is not to big which will make server give error 500

        for i in range(0, len(pkey), stepSize):
            variables = {'pkey': pkey[i:i + stepSize]}
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

    def fields(self, table):
        """ Fetch a field list as json array of name value pairs"""

        query = '{__type(name:"' + table + '") {fields { name } } }'

        response = self.session.post(self.graphqlEndpoint, json={'query': query})

        if response.status_code != 200:
            log.error(f"Error while fetching table fields, status code {response.status_code}")
            log.error(response)

        return response.json()['data']['__type']['fields']

    def uploadCSV(self, table, data):
        """ Upload csv data ( string ) to table """
        response = self.session.post(
            self.apiEndpoint + '/csv/' + table,
            headers={"Content-Type": 'text/csv'},
            data=data
        )

        if response.status_code != 200:
            log.error(response)
            log.error(f"Error uploading csv, status code {response.text}")

        return response

    def downLoadCSV(self, table):
        """ Download csv data from table """
        resp = self.session.get(self.apiEndpoint + '/csv/' + table, allow_redirects=True)
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

    def upload_zip_fallback(self, data) -> None:
        """ Upload zip, will falback on TARGET.zip if upload of SOURCE zip fails"""
        response = self.session.post(
            self.apiEndpoint + '/zip?async=true',
            files={'file': ('zip.zip', data.getvalue())},
        )

        def upload_zip_task_status(self, response) -> None:
            task_response = self.session.get(self.url + response.json()['url'])

            if task_response.json()['status'] == 'COMPLETED':
                log.info(f"{task_response.json()['status']}, {task_response.json()['description']}")
                filename = 'TARGET.zip'
                if os.path.exists(filename):
                    os.remove(filename)
                return

            if task_response.json()['status'] == 'ERROR':
                log.error(f"{task_response.json()['status']}, {task_response.json()['description']}")
                # fallback to TARGET.zip
                fallback_response = self.session.post(
                    self.apiEndpoint + '/zip?async=true',
                    files={'file': open('TARGET.zip', 'rb')},
                )
                log.info(f"TARGET.zip found, upload zip")
                upload_zip_task_status(self, fallback_response)  # endless loop ..
                sys.exit()

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

        response = self.session.post(self.graphqlEndpoint, json={'query': query})
        if response.status_code != 200:
            log.error(f"Database schema does not exist, status code {response.status_code}")
            sys.exit()

    def create_database(self) -> None:
        """ Create TARGET database if it doesn't exists"""
        query = '{_session {schemas} }'

        response = self.session.post(self.graphqlEndpoint, json={'query': query})
        if response.status_code == 200:
            log.warning(f"Database schema exists, status code {response.status_code}")

            # query = """
            #     mutation createSchema($name:String, $description:String, $template: String, $includeDemoData: Boolean){
            #         createSchema(name:$name, description:$description, template: $template, includeDemoData: $includeDemoData){
            #             message
            #         }
            #     }
            #     """
        else:
            query = """
                mutation createSchema($name:String){
                    createSchema(name:$name){
                        message
                    }
                }
                """
            # variables = {'name':self.database,'description':'null','template':'null','includeDemoData':'false'}
            variables = {'name': self.database}

            response = self.session.post(
                self.url + '/apps/central/graphql',
                json={'query': query, 'variables': variables}
            )
            print(response.json())
            if response.json()['data']['createSchema']['message']:
                log.info(response.json()['data']['createSchema']['message'])

    def drop_database(self) -> None:
        """ Delete database """

        # see if schema exists before deleting it
        query = '{_session {schemas} }'

        response = self.session.post(self.graphqlEndpoint, json={'query': query})

        if response.status_code == 200:
            # log.warning(f"Database schema does not exist, status code {response.status_code}")
            query = """
            mutation deleteSchema($name:String){
                deleteSchema(name:$name) {
                    message
                }
            }
            """

            variables = {'name': self.database}

            response = self.session.post(
                self.url + '/api/graphql',
                json={'query': query, 'variables': variables}
            )

            if response.json()['data']['deleteSchema']['message']:
                log.info(response.json()['data']['deleteSchema']['message'])
