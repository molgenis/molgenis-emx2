
import requests
from molgenis.graphql import graphql
from molgenis.utils import cleanUrl
from molgenis.utils import csvwriter

class Client:
    def __init__(self, host: str='http://localhost:8080/'):
        """Interact with a MOLGENIS EMX2 Instance
        
        @param host string containing the host (i.e., URL) of the EMX2 instance
            that you wish to connect to
        """
        self.session = requests.Session()
        self.host = cleanUrl(host)
        self.api_graphql = f"{self.host}/apps/central/graphql"
        self.token = None
        
    def signin(self, username:str=None, password:str=None):
        """Signin
        Sign in into an EMX2 instance with your username and password
        
        @param username your username
        @param password your password

        @return status message; response
        """
        self.username = username
        response = self.session.post(
            url=self.api_graphql,
            json={
                'query': graphql.signin(),
                'variables': {'email': username, 'password': password}
            }
        )
        response.raise_for_status()
        
        data = response.json().get('data', {}).get('signin', {})
        if data.get('status') == 'SUCCESS':
            print(f"Signed into {self.host} as {username}")
            self.token = data.get('token')
        else:
            error = data.get('message')
            print(f"Unable to connect to {self.host} as {username}\n{error}")
        
        return response
    
    def signout(self):
        """Signout
        Signout of the EMX2 instance
        
        @return status message; response
        """
        response = self.session.post(url=self.api_graphql,json={'query': graphql.signout()})
        response.raise_for_status()
        
        data = response.json().get('data',{}).get('signout',{})
        if data.get('status') == 'SUCCESS':
            print(f"Signed out of {self.host}")
        else:
            error = data.get('message')
            print(f"Unable to sign out of {self.host}\n{error}")
            
    def __prepareData__(self, file: str=None, data: list=[]):
        dataToImport = None

        if bool(file) and bool(data):
            raise ValueError('Unable to import data as both file and dataset are provided. Please select one.')

        elif not bool(file) and not bool(data):
            raise ValueError('Unable to import data. No file or dataset provided.')

        elif bool(file):
            with open(file, 'rb') as stream:
                dataToImport = stream.read()
                stream.close()

        elif bool(data):
            writer = csvwriter(data = data)
            writer.toString()
            return writer.csv
        
        else:
            raise ValueError('Unable to import data.')
        
        return dataToImport
            
    def add(self, schema: str=None, table: str=None, file: str=None, data: list=[]):
        """Add
        Import or update records in a table of a named schema. 
        
        @param schema name of a schema
        @param table the name of the table
        @param file location of the file containing records to import or update
        @param data a dataset containing reocords to import or update (list of dictionaries)
        
        @return status message; response
        """
        dataToImport = self.__prepareData__(file=file, data=data)
        
        if bool(dataToImport):
            response = self.session.post(
                url=f"{self.host}/{schema}/api/csv/{table}",
                headers={'Content-Type':'text/csv'},
                data = dataToImport
            )
            
            if response.status_code == 200:
                print(f"Imported data into {schema}::{table}")
            else:
                errors = '\n'.join([err['message'] for err in response.json().get('errors')])
                print(f"Failed to import data into {schema}::{table}\n{errors}")
                
            return response
    
    def delete(self, schema: str=None, table: str=None, file: str=None, data: list=[]):
        """Add
        Delete records from table.
        
        @param schema name of the schema
        @param table the name of the table
        @param file location of a file containing records to delete
        @param data dataset of records to delete (list of dictionaries)
        
        @return status message; response
        """
        dataToImport = self.__prepareData__(file=file, data=data)
        
        if bool(dataToImport):
            response = self.session.delete(
                url=f"{self.host}/{schema}/api/csv/{table}",
                headers={'Content-Type':'text/csv'},
                data = dataToImport
            )
            response.raise_for_status()
            
            if response.status_code == 200:
                print(f"Imported data into {schema}::{table}")
            else:
                errors = '\n'.join([err['message'] for err in response.json().get('errors')])
                print(f"Failed to import data into {schema}::{table}\n{errors}")
                
            return response
        
        
    def get(self, schema: str=None, table: str=None):
        """Get
        Retrieve data from a schema and return as a list of dictionaries
        
        @param schema name of the schema
        @param table name of the table
        
        @return list of dictionaries; status message
        """
        response = self.session.get(url = f"{self.host}/{schema}/api/csv/{table}")
        response.raise_for_status()
        return response
        