# Molgenis Pyclient
The MOLGENIS EMX2 Python client allows the user to retrieve, create, update and delete entities on a MOLGENIS EMX2 server using the Python language.


## Installation
The releases of the package are hosted at [PyPI](https://pypi.org/project/molgenis-emx2-pyclient/).
The recommended way to install the latest

```commandline
pip install molgenis_emx2_pyclient
```

## Setting up the client
The Python client can be integrated in scripts authorized by either a username/password combination or a temporary token.

Signing in with a username/password combination requires using the client as context manager:
```python
from molgenis_emx2_pyclient import Client

username = 'username'
password = '********'

# Initialize the client as a context manager
with Client('https://example.molgeniscloud.org') as client:
    # Apply the 'signin' method with the username and password
    client.signin(username, password)
    
    # Perform other tasks
    ...
```

Before using the Pyclient with a token, this token should be generated in the UI, see [Tokens](use_tokens.md).
Using the Pyclient with a token requires supplying the token in the initialization of the Client object:
```python
from molgenis_emx2_pyclient import Client

token = '********************************'

client = Client(url='https://example.molgeniscloud.org', token=token)
    
# Perform other tasks
...

```

Additionally, if the Pyclient is to be used on a particular schema, this schema can be supplied in the initialization of the client, alongside the server URL:
```python
with Client('https://example.molgeniscloud.org', schema='My Schema') as client:
```
or 
```python
client = Client('https://example.molgeniscloud.org', schema='My Schema', token=token)
```

## Methods and properties
This section outlines some of the methods that are supported by the Pyclient.

### schema_names
```python
client.schema_names
```

Returns a list of the names of the schemas on the server that are available for the user.

### status
```python
client.status
```
The `status` property returns a string with information, including the server URL, the user (if applicable), the sign-in status, the version of MOLGENIS EMX2 running on the server, and the result of `schema_names`.


### get_schemas
```python
client.get_schemas()
```
Retrieves the schemas on the server for the user as a list of dictionaries containing for each schema the id, name, label and description.
This method accepts no arguments.


### set_schema
```python
client.set_schema('My Schema')
```
Sets the default schema for the server. Throws the `NoSuchSchemaException` if the schema is not available.

| argument | type | description          | required | default |
|----------|------|----------------------|----------|---------|
| name     | str  | the name of a schema | True     |         |

### set_token
```python
client.set_token(token='***************')
```
Sets the client's token in case no token was supplied in the initialization.
Raises the `TokenSigninException` when the client is already signed in with a username/password combination.

| argument | type | description | required | default |
|----------|------|-------------|----------|---------|
| token    | str  | the token   | True     |         |

### get_schema_metadata
```python
client.get_schema_metadata(name='My Schema')
```
Retrieves a schema's metadata, including the names of the tables, the metadata of the columns in the tables, schema settings, etc.
If no `name` is supplied the metadata for the default schema is queried, if applicable. Else the `NoSuchSchemaException` is thrown.

| argument | type | description          | required | default |
|----------|------|----------------------|----------|---------|
| name     | str  | the name of a schema | True     |         |

### get
```python
client.get(table='Data Table', schema='My Schema', as_df=True)
```
Retrieves data from a table on a schema and returns the result either as a list of dictionaries or as a pandas DataFrame (as pandas is used to parse the response).

| argument | type | description                                                                    | required | default |
|----------|------|--------------------------------------------------------------------------------|----------|---------|
| table    | str  | the name of a table                                                            | True     | None    |
| schema   | str  | the name of a schema                                                           | False    | None    |
| as_df    | bool | if true: returns data as pandas DataFrame <br/> else as a list of dictionaries | False    | False   |


### save_schema
```python
client.save_schema(table='Data Table', name='My Schema',  
                   file='location/of/data/file.csv', data=[{'col1': value1, ...}, {'col2': value2, ...}, ...])
```
Imports or updates records in a table of a named schema.
The data either originates from a file on the disk, or is supplied by the user after, for example, preprocessing.
Either `file` or `data` must be supplied. The data must be compatible with the schema to which it is uploaded. 

| argument | type | description                            | required | default |
|----------|------|----------------------------------------|----------|---------|
| table    | str  | the name of a table                    | True     |         |
| schema   | str  | the name of a schema                   | False    | None    |
| file     | str  | the location of a `csv` file with data | False    | None    |
| data     | list | data as a list of dictionaries         | False    | None    |


### delete_records
```python
client.delete_records(table='Data Table', name='My Schema',  
                      file='location/of/data/file.csv', data=[{'col1': value1, ...}, {'col2': value2, ...}, ...])
```
Deletes data records from a table.
As in the `save_schema` method, the data either originates from disk or the program.

[//]: # (In order to delete records from a table the data must specify the row values with principal keys

| argument | type | description                            | required | default |
|----------|------|----------------------------------------|----------|---------|
| table    | str  | the name of a table                    | True     |         |
| schema   | str  | the name of a schema                   | False    | None    |
| file     | str  | the location of a `csv` file with data | False    | None    |
| data     | list | data as a list of dictionaries         | False    | None    |


### export
```python
client.export(schema='My Schema', table='Data Table', fmt='csv')
```
Exports data from a schema to a file in the desired format.
If the table is specified, only data from that table is exported, otherwise the export contains all tables on the schema.
If all tables from a schema are exported with given format `csv`, the data is exported as a zip file containing a csv file of each table.

| argument | type | description                               | required | default |
|----------|------|-------------------------------------------|----------|---------|
| table    | str  | the name of a table                       | False    | None    |
| schema   | str  | the name of a schema                      | False    | None    |
| fmt      | str  | the output format, either `csv` or `xlsx` | False    | `csv`   |

### create_schema
```python
client.create_schema(name='New Schema', description='My new schema!', template='DATA_CATALOGUE', include_demo_data=True)
```
Creates a new schema on the server.
If no template is selected, an empty schema is created.

| argument          | type | description                   | required | default |
|-------------------|------|-------------------------------|----------|---------|
| name              | str  | the name of the new schema    | True     |         |
| description       | str  | description of the new schema | False    | None    |
| template          | str  | the template for this schema  | False    | None    |
| include_demo_data | bool | whether to include demo data  | False    | False   |

### delete_schema
TODO

### update_schema

### recreate_schema
