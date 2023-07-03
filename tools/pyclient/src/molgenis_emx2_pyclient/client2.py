
from molgenis_emx2_pyclient.graphql import graphql
from molgenis_emx2_pyclient.utils import cleanUrl
import pandas as pd
import requests
import csv
import io

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
        variables={'email': username, 'password': password}
        response = self.session.post(
            url=self.api_graphql,
            json={'query': graphql.signin(),'variables': variables}
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
            
    def _toCsv_(self, data):
        return pd.DataFrame(data) \
          .to_csv(index=False, quoting=csv.QUOTE_ALL, encoding="UTF-8")
    
    def _fromFile_(self, file):
        with open(file, 'rb') as stream:
            data = stream.read()
            stream.close()
        return data
    
    def _prepData_(self, file: str=None, data:list=[]):
        if bool(file) and not bool(data):
            return self._fromFile_(file=file)
        
        elif not bool(file) and bool(data):
            return self._toCsv_(data=data)
        
        else:
            raise ValueError("unable to prepare data. Enter a file path or a add a dataset")
            
    def add(self, schema: str=None, table: str=None, file: str=None, data: list=[]):
        """Add
        Import or update records in a table of a named schema. 
        
        @param schema name of a schema
        @param table the name of the table
        @param file location of the file containing records to import or update
        @param data a dataset containing reocords to import or update (list of dictionaries)
        
        @return status message; response
        """
        dataToImport = self._prepData_(file=file, data=data)
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
        dataToImport = self._prepData_(file=file, data=data)
        response = self.session.delete(
            url=f"{self.host}/{schema}/api/csv/{table}",
            headers={'Content-Type':'text/csv'},
            data = dataToImport
        )
        response.raise_for_status()
        
        if response.status_code == 200:
            print(f"Deleted data from {schema}::{table}")
        else:
            errors = '\n'.join([err['message'] for err in response.json().get('errors')])
            print(f"Failed to delete data from {schema}::{table}\n{errors}")
            
        return response
        
        
    def get(self, schema: str=None, table: str=None, asDataFrame=False):
        """Get
        Retrieve data from a schema and return as a list of dictionaries
        
        @param schema name of the schema
        @param table name of the table
        @param asDataFrame if True, the response will be returned as a
          pandas DataFrame. Otherwise, a recordset will be returned.
        
        @return list of dictionaries; status message
        """
        response = self.session.get(url = f"{self.host}/{schema}/api/csv/{table}")
        response.raise_for_status()
        
        data = pd.read_csv(io.StringIO(response.text), sep=",")
        if asDataFrame:
            return data
        return data.to_dict('records')
        