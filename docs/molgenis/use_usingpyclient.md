# Molgenis Pyclient
The MOLGENIS EMX2 Python client allows the user to retrieve, create, update and delete entities on a MOLGENIS EMX2 server using scripts or programs written in the Python language.


## Installation
The releases of the package are hosted at [PyPI](https://pypi.org/project/molgenis-emx2-pyclient/).
The recommended way to install the latest

```commandline
pip install molgenis-emx2-pyclient
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
    ...
```
or 
```python
client = Client('https://example.molgeniscloud.org', schema='My Schema', token=token)
```

## Methods and properties
This section outlines some of the methods that are supported by the Pyclient.
The results of these methods depend on the permission level the user has on the schemas involved in the method.

The user roles on MOLGENIS EMX2 and their respective permissions are described in the [Permissions](use_permissions.md) section.


### schema_names
```python
client.schema_names
```

A property that returns a list of the names of the schemas for which the user has at least _viewer_ permissions.

### status
```python
client.status
```
A property that returns a string with information, including the server URL, the user (if applicable), the sign-in status, the version of MOLGENIS EMX2 running on the server, and the result of `schema_names`.


### get_schemas
```python
client.get_schemas()
```
Retrieves the schemas for which the user has at least _viewer_ permissions as a list of dictionaries containing for each schema the id, name, label and description.
This method accepts no arguments.

### set_schema
```python
client.set_schema('My Schema')
```
Sets the default schema for the server. 
Throws the `NoSuchSchemaException` if the user does not have at least _viewer_ permissions or if the schema does not exist.

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


### get
```python
client.get(table='Data Table', schema='My Schema', as_df=True)
```
Retrieves data from a table on a schema and returns the result either as a list of dictionaries or as a pandas DataFrame (as pandas is used to parse the response).
Throws the `NoSuchSchemaException` if the user does not have at least _viewer_ permissions or if the schema does not exist.

| argument | type | description                                                                    | required | default |
|----------|------|--------------------------------------------------------------------------------|----------|---------|
| table    | str  | the name of a table                                                            | True     | None    |
| schema   | str  | the name of a schema                                                           | False    | None    |
| as_df    | bool | if true: returns data as pandas DataFrame <br/> else as a list of dictionaries | False    | False   |


### get_schema_metadata
```python
client.get_schema_metadata(name='My Schema')
```
Retrieves the metadata of a schema and returns it in the _metadata.Schema_ format.
See the description of the [Schema](use_usingpyclient.md#schema) metadata object below.


| argument | type | description          | required | default |
|----------|------|----------------------|----------|---------|
| name     | str  | the name of the schema | True     | None    |


### export
```python
client.export(schema='My Schema', table='Data Table', fmt='csv')
```
Exports data from a schema to a file in the desired format.
If the table is specified, only data from that table is exported, otherwise the export contains all tables on the schema.
If all tables from a schema are exported with given format `csv`, the data is exported as a zip file containing a csv file of each table.
Throws the `NoSuchSchemaException` if the user does not have at least _viewer_ permissions or if the schema does not exist.

| argument | type | description                               | required | default |
|----------|------|-------------------------------------------|----------|---------|
| table    | str  | the name of a table                       | False    | None    |
| schema   | str  | the name of a schema                      | False    | None    |
| fmt      | str  | the output format, either `csv` or `xlsx` | False    | `csv`   |


### save_schema
```python
client.save_schema(table='Data Table', name='My Schema',  
                   file='location/of/data/file.csv', data=[{'col1': value1, ...}, {'col2': value2, ...}, ...])
```
Imports or updates records in a table of a named schema.
The data either originates from a file on the disk, or is supplied by the user after, for example, preprocessing.
Either `file` or `data` must be supplied. The data must be compatible with the schema to which it is uploaded. 
Throws the `PermissionDeniedException` if the user does not have at least _editor_ permissions for this schema.
Throws the `NoSuchSchemaException` if the schema is not found on the server.

| argument | type | description                                        | required | default |
|----------|------|----------------------------------------------------|----------|---------|
| table    | str  | the name of a table                                | True     |         |
| schema   | str  | the name of a schema                               | False    | None    |
| file     | str  | the location of a `csv` file with data             | False    | None    |
| data     | list | data as a list of dictionaries or pandas DataFrame | False    | None    |


### delete_records
```python
client.delete_records(table='Data Table', schema='My Schema',  
                      file='location/of/data/file.csv', data=[{'col1': value1, ...}, {'col2': value2, ...}, ...])
```
Deletes data records from a table.
As in the `save_schema` method, the data either originates from disk or the program.
Throws the `PermissionDeniedException` if the user does not have at least _editor_ permissions.
Throws the `NoSuchSchemaException` if the schema is not found on the server.

[//]: # (&#40;In order to delete records from a table the data must specify the row values with principal keys)

| argument | type | description                                        | required | default |
|----------|------|----------------------------------------------------|----------|---------|
| table    | str  | the name of a table                                | True     |         |
| schema   | str  | the name of a schema                               | False    | None    |
| file     | str  | the location of a `csv` file with data             | False    | None    |
| data     | list | data as a list of dictionaries or pandas DataFrame | False    | None    |


### create_schema
```python
client.create_schema(name='New Schema', description='My new schema!', template='DATA_CATALOGUE', include_demo_data=True)
```
Creates a new schema on the server.
If no template is selected, an empty schema is created.
Only users with _admin_ privileges are able to perform this action.

| argument          | type | description                   | required | default |
|-------------------|------|-------------------------------|----------|---------|
| name              | str  | the name of the new schema    | True     |         |
| description       | str  | description of the new schema | False    | None    |
| template          | str  | the template for this schema  | False    | None    |
| include_demo_data | bool | whether to include demo data  | False    | False   |

[//]: # (The available templates are)

[//]: # ()
[//]: # (| template                       | description                                                   |)

[//]: # (|--------------------------------|---------------------------------------------------------------|)

[//]: # (| PET_STORE                      | example template                                              |)

[//]: # (| FAIR_DATA_HUB                  | see [FAIR Data Point]&#40;dev_fairdatapoint.md&#41;                   |)

[//]: # (| DATA_CATALOGUE                 | see [Data Catalogue]&#40;../catalogue/cat_cohort-data-manager.md&#41; |)

[//]: # (| DATA_CATALOGUE_COHORT_STAGING  | see [Data Catalogue]&#40;../catalogue/cat_cohort-data-manager.md&#41; |)

[//]: # (| DATA_CATALOGUE_NETWORK_STAGING | see [Data Catalogue]&#40;../catalogue/cat_cohort-data-manager.md&#41; |)

[//]: # (| RD3                            |                                                               |)

[//]: # (| JRC_COMMON_DATA_ELEMENTS       |                                                               | )

[//]: # (| FAIR_GENOMES                   |                                                               |)

[//]: # (| BEACON_V2                      | see [Beacon v2]&#40;dev_beaconv2.md&#41;                              |)

[//]: # (| ERN_DASHBOARD                  |                                                               |)

[//]: # (| ERN_CRANIO                     |                                                               |)

[//]: # (| BIOBANK_DIRECTORY              |                                                               |)

[//]: # (| SHARED_STAGING                 |                                                               |)

### delete_schema
```python
client.delete_schema(name='Old Schema')
```

Deletes a schema from the server. 
Only users with _admin_ privileges are able to perform this action.
Throws the `PermissionDeniedException` if execution of the method is attempted without _admin_ privileges.
Throws the `NoSuchSchemaException` if the schema is not found on the server.


| argument          | type | description                      | required | default |
|-------------------|------|----------------------------------|----------|---------|
| name              | str  | the name of the schema to delete | True     |         | 


### update_schema
```python
client.update_schema(name='My Schema', description='The new description of this schema.')
```
Updates a schema's description.
Only users with _admin_ privileges are able to perform this action.
Throws the `PermissionDeniedException` if execution of the method is attempted without _admin_ privileges.
Throws the `NoSuchSchemaException` if the schema is not found on the server.

| argument          | type | description             | required | default |
|-------------------|------|-------------------------|----------|---------|
| name              | str  | the name of the schema  | True     |         |
| description       | str  | the updated description | True     |         |

### recreate_schema
```python
client.recreate_schema(name='My Schema', description='Updated description', 
                       template='DATA_CATALOGUE', include_demo_data=False)
```
Recreates a schema on the EMX2 server by deleting and subsequently creating it without data on the EMX2 server.

If no template is selected, an empty schema is created.
Only users with _admin_ privileges are able to perform this action.
Throws the `PermissionDeniedException` if execution of the method is attempted without _admin_ privileges.
Throws the `NoSuchSchemaException` if the schema is not found on the server.

| argument          | type | description                   | required | default |
|-------------------|------|-------------------------------|----------|---------|
| name              | str  | the name of the schema        | True     |         |
| description       | str  | new description of the schema | False    | None    |
| template          | str  | the template for this schema  | False    | None    |
| include_demo_data | bool | whether to include demo data  | False    | False   |


## Additional classes
In addition to the Client class, the package contains classes to access metadata on the column, table, and schema level.
These classes can be imported from `metadata.py` as follows

```python
from molgenis_emx2_pyclient.metadata import Column, Table, Schema
```
### Column
This class contains information about a column of a table in a Molgenis EMX2 schema.
An object of this class can be constructed by supplying its attributes as keyword arguments
```python
column = Column(name='First column', columnType='string', key=1)
```
or by supplying a dictionary of attributes
```python
column_data = {'name': 'First column', 'columnType': 'STRING', 'key': 1}
column = Column(**column_data)
```
In both cases, the keyword `name` must be supplied. 
#### get
```python
column.get('columnType')
```
Its attributes can be retrieved by accessing them directly, e.g. `column.name` or by the `get` method.
#### to_dict
```python
column.to_dict()
```
A _Column_ object can be parsed to a dictionary object by calling the `to_dict` method.

### Table
A _Table_ object represents a table in an EMX2 schema. 
It can be constructed in a similar way as a Column object
```python
table = Table(name='My table', description='My table contains some data.', 
              columns=[Column(name='First column', columnType='STRING', key=1),
                       Column(name='organisations', columnType='REF_ARRAY', refTableName='Organisations')])
```
```python
table_data = {'name': 'My table', 'description': 'My table contains some data.',
              columns: [{'name': 'First column', 'columnType': 'STRING', 'key': 1},
                        {'name': 'organisations', 'columnType': 'REF_ARRAY', 'refTableName': 'Organisations'}]}
```
In both cases, the keyword `name` must be supplied. 
The `columns` attribute can be supplied either as list of _Column_ objects or as a list of dictionaries from which _Column_ objects can be constructed.

#### get
```python
table.get('description')
```
A _Table_'s attributes can be retrieved by accessing them directly, e.g. `table.name` or by the `get` method.

#### to_dict
```python
table.to_dict()
```
Analogous to _Column_, a _Table_ object can be parsed to a dictionary object by calling the `to_dict` method.
The columns in its `columns` attribute are also parsed to dictionaries.

#### get_column
```python
table.get_column(by='name', value='First column')
```
Gets a unique _Column_ object in its `columns` attribute by either its `name` or `id` attribute.
Raises a NoSuchColumnException if the column could not be found.

#### get_columns
```python
table.get_columns(by='refTableName', value='Organisations')
table.get_columns(by=['columnType', 'refTableName'], value=['REF_ARRAY', 'Organisations'])
```
Gets the columns of which an attribute matches a particular value.
It is possible to filter the columns by multiple conditions, the attributes and values are then supplied as lists. 
The length of the `by` argument must then match the length of the `value` argument.
Returns an empty list if no matching column can be found.


### Schema
A _Schema_ object represents the metadata of an EMX2 schema.
It can be constructed similarly to the _Table_ and _Column_ classes.

```python
schema = Schema(name='My schema', description='This is my schema!',
                tables=[
                    Table(name='My table', description='My table contains some data.', 
                          columns=[Column(name='First column', columnType='STRING', key=1),
                                   Column(name='organisations', columnType='REF_ARRAY', refTableName='Organisations')]),
                    Table(name='Organisations', description='A table containing data about organisations.',
                          columns=[Column(name='name', columnType='STRING', key=1, required=True),
                                   Column(name='acronym', columnType='STRING', description='The acronym for this organisation.')])
                ])
```

```python
schema_data = {'name': 'My schema', description: 'This is my schema!',
               'tables': [
                   {'name': 'My table', 'description': 'My table contains some data.',
                    'columns': [
                        {'name': 'First column', 'columnType': 'string', 'key': 1},
                        {'name': 'organisations', 'columnType': 'REF_ARRAY', 'refTableName': 'Organisations'}
                    ]},
                   {'name': 'Organisations', 'description': 'A table containing data about organisations.',
                    'columns': [
                        {'name': 'name', 'columnType': 'STRING', 'key': 1, 'required': True},
                        {'name': 'acronym', 'columnType': 'STRING', 'description': 'The acronym for this organisation.'}
                    ]}
               ]}
schema = Schema(**schema_data)
```
In both cases, the keyword `name` must be supplied. 
The `tables` attribute can be supplied either as list of _Table_ objects or as a list of dictionaries from which _Table_ objects can be constructed.


#### get
```python
schema.get('description')
```
A _Schema_'s attributes can be retrieved by accessing them directly, e.g. `schema.name` or by the `get` method.

#### to_dict
```python
schema.to_dict()
```
Analogous to _Table_ and _Column_, a _Schema_ object can be parsed to a dictionary object by calling the `to_dict` method.
The _Table_ objects in its `tables` attribute are also parsed to dictionaries.

#### get_table
```python
schema.get_table(by='name', value='My table')
```
Gets a unique _Table_ object in its `table` attribute by either its `name` or `id` attribute.
Raises a NoSuchTableException if the table could not be found.

#### get_tables
```python
table.get_tables(by='inheritName', value='Resources')
```
Gets the table of which an attribute matches a particular value.
It is possible to filter the tables by multiple conditions, the attributes and values are then supplied as lists. 
The length of the `by` argument must then match the length of the `value` argument.
Returns an empty list if no matching column can be found.
