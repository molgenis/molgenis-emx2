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
URLs of EMX2 servers on remote servers are required to start with `https://`.
It is possible to use the Pyclient on a server running on a local machine. The URL should then be passed as `http://localhost:PORT`.

Signing in with a username/password combination requires using the client as context manager:
```python
from molgenis_emx2_pyclient import Client

username = 'username'
password = '********'

# Initialize the client as a context manager
with Client(url='https://example.molgeniscloud.org') as client:
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

### Scripts and Jobs
When using the client in a script that runs as part of a job via the [Task API](use_scripts_jobs.md), it is essential
to provide the job identifier to the client. This identifier allows the backend to associate its actions with the
specific job execution.

The job identifier can be passed into the script using `${jobId}`. To initialize the client with the job identifier,
the code should be structured as follows:
```python
Client('https://example.molgeniscloud.org', schema='My Schema', job='${jobId}')
```

## Methods and properties
This section outlines some of the methods that are supported by the Pyclient.
The results of these methods depend on the permission level the user has on the schemas involved in the method.

The user roles on MOLGENIS EMX2 and their respective permissions are described in the [Permissions](use_permissions.md) section.


### schema_names
```python
Client().schema_names
```

A property that returns a list of the names of the schemas for which the user has at least _viewer_ permissions.

### status
```python
Client().status
```
A property that returns a string with information, including the server URL, the user (if applicable), the sign-in status, the version of MOLGENIS EMX2 running on the server, and the result of `schema_names`.


### get_schemas
```python
def get_schemas(self) -> list[Schema]:
    ...
```
Retrieves the schemas for which the user has at least _viewer_ permissions as a list of dictionaries containing for each schema the id, name, label and description.
This method accepts no arguments.

### set_schema
```python
def set_schema(self, name: str) -> str:
    ...
```
Sets the default schema for the server in the property `default_schema`. 
Throws the `NoSuchSchemaException` if the user does not have at least _viewer_ permissions or if the schema does not exist.

| parameter | type | description          | required | default |
|-----------|------|----------------------|----------|---------|
| `name`    | str  | the name of a schema | True     |         |

### set_token
```python
def set_token(self, token: str):
    ...
```
Sets the client's token in case no token was supplied in the initialization.
Raises the `TokenSigninException` when the client is already signed in with a username/password combination.

| parameter | type | description | required | default |
|-----------|------|-------------|----------|---------|
| `token`   | str  | the token   | True     |         |


### get
```python
def get(self, 
        table: str, 
        columns: list[str] = None,
        query_filter: str = None, 
        schema: str = None, 
        as_df: bool = False) -> list | pandas.DataFrame:
    ...
```
Retrieves data from a table on a schema using the CSV API and returns the result either as a list of dictionaries or as a pandas DataFrame.
Use the `columns` parameter to specify which columns to retrieve. Note that in case `as_df=True` the column _names_ should be supplied, otherwise the column _ids_.
Use the `query_filter` parameter to filter the results based on filters applied to the columns.
This query requires a special syntax. 
Values in columns can be filtered on equality `==`, inequality `!=`, greater `>` and smaller `<` than.
Values within an interval can also be filtered by using the operand `between`, followed by list of the upper bound and lower bound.
The values of reference and ontology columns can also be filtered by joining the column id of the table with the column id of the reference/ontology table by a dot, as in the example `countries.name`, where `countries` is a column in the table `My table` and `name` is the column id of the referenced table specifying the names of countries. 
It is possible to add filters on multiple columns by separating the filter statements with _' and '_.
It is recommended to supply the filters that are compared as variables passed in an f-string.

Throws the `NoSuchSchemaException` if the user does not have at least _viewer_ permissions or if the schema does not exist.
Throws the `NoSuchColumnException` if the `columns` argument or query filter contains a column that is not present in the table.


| parameter      | type | description                                                                    | required | default |
|----------------|------|--------------------------------------------------------------------------------|----------|---------|
| `table`        | str  | the name of a table                                                            | True     | None    |
| `columns`      | list | a list of column names or ids to filter on                                     | False    | None    |
| `schema`       | str  | the name of a schema                                                           | False    | None    |
| `query_filter` | str  | a string to filter the results on                                              | False    | None    |
| `as_df`        | bool | if true: returns data as pandas DataFrame <br/> else as a list of dictionaries | False    | False   |

##### examples

```python
# Get all entries for the table 'Resources' on the schema 'MySchema'
table_data = client.get(table='Resources', schema='MySchema', columns=['name', 'collectionEvents'])

# Set the default schema to 'MySchema'
client.set_schema('MySchema')
# Get the same entries and return them as pandas DataFrame
table_data = client.get(table='Resources', columns=['name', 'collection events'], as_df=True)

# Get the entries where the value of a particular column 'number of participants' is greater than 10000
table_data = client.get(table='Resources', query_filter='numberOfParticipants > 10000')

# Get the entries where 'number of participants' is greater than 10000 and the resource type is a 'Population cohort'
# Store the information in variables, first
min_subpop = 10000
cohort_type = 'Population cohort'
table_data = client.get(table='Resources', query_filter=f'numberOfParticipants > {min_subpop}'
                                                        f'and cohortType == {cohort_type}')
```


### get_graphql
```python
def get_graphql(self, 
        table: str, 
        columns: list[str] = None,
        query_filter: str = None, 
        schema: str = None) -> list:
    ...
```
Retrieves data from a table on a schema using the GraphQL API and returns the result either as a list of dictionaries.
This method and its parameters behave similarly to `get` with option `as_df=False`. 
The results are returned in a slightly different way, however.
`get` retains the column _names_, whereas `get_graphql` returns column _id_s, which are in lower camel case.
Furthermore, the `get` method will return the values in columns with a reference type, while the results of `get_graphql` will also contain the primary keys for those columns.  

Throws the `NoSuchSchemaException` if the user does not have at least _viewer_ permissions or if the schema does not exist.
Throws the `NoSuchColumnException` if the `columns` argument or query filter contains a column that is not present in the table.


| parameter      | type | description                                                                    | required | default |
|----------------|------|--------------------------------------------------------------------------------|----------|---------|
| `table`        | str  | the name of a table                                                            | True     | None    |
| `columns`      | list | a list of column names or ids to filter on                                     | False    | None    |
| `schema`       | str  | the name of a schema                                                           | False    | None    |
| `query_filter` | str  | a string to filter the results on                                              | False    | None    |

##### examples

```python
# Get all entries for the table 'Resources' on the schema 'MySchema'
table_data = client.get_graphql(table='Resources', schema='MySchema', columns=['name', 'collectionEvents'])

# Set the default schema to 'MySchema'
client.set_schema('MySchema')

# Get the entries where the value of a particular column 'number of participants' is greater than 10000
table_data = client.get_graphql(table='Resources', query_filter='numberOfParticipants > 10000')

# Get the entries where 'number of participants' is greater than 10000 and the resource type is a 'Population cohort'
# Store the information in variables, first
min_subpop = 10000
cohort_type = 'Population cohort'
table_data = client.get_graphql(table='Resources', query_filter=f'numberOfParticipants > {min_subpop}'
                                                        f'and cohortType == {cohort_type}')
```


### get_schema_metadata
```python
def get_schema_metadata(self, name: str = None) -> Schema:
    ...
```
Retrieves the metadata of a schema and returns it in the _metadata.Schema_ format.
See the description of the [Schema](use_usingpyclient.md#schema) metadata object below.


| parameter | type | description            | required | default |
|-----------|------|------------------------|----------|---------|
| `name`    | str  | the name of the schema | True     | None    |


### export
```python
async def export(self, 
                 schema: str = None, 
                 table: str = None, 
                 filename: str = None,
                 as_excel: bool = False) -> BytesIO:
    ...
```
Asynchronously exports data from a schema to a file in the desired format and in a `BytesIO` object in memory.
If the table is specified, only data from that table is exported, otherwise the export contains all tables on the schema.

The name of the file to which the data is exported can be specified in the `filename` parameter.
If no file name is given a single table is by default exported using the _csv API_ and the collection of tables on the schema using the _zip API_.
The `as_excel` parameter specifies whether to export the data in Excel format. This is ignored if a filename is given.
Throws the `NoSuchSchemaException` if the user does not have at least _viewer_ permissions or if the schema does not exist.

| parameter  | type | description                                | required | default               |
|------------|------|--------------------------------------------|----------|-----------------------|
| `table`    | str  | the name of a table                        | False    | None                  |
| `schema`   | str  | the name of a schema                       | False    | client.default_schema |
| `filename` | str  | the name of the file to export the data to | False    | None                  |
| `as_excel` | bool | whether to return the data in Excel format | False    | False                 |


##### examples
```python

# Export the table 'Resources' on the schema 'MySchema' from the CSV API to a BytesIO object 
resources_raw: BytesIO = await client.export(schema='MySchema', table='Resources')  

# Export 'Resources' from the Excel API to the file 'Resources-export.xlsx' 
await client.export(schema='MySchema', table='Resources', filename='Resources-export.xlsx')
```


### save_schema
```python
def save_schema(self, 
                table: str, 
                name: str = None, 
                file: str = None, 
                data: list | pandas.DataFrame = None):
```
Imports or updates records in a table of a named schema.
The data either originates from a file on the disk, or is supplied by the user after, for example, preprocessing.
Either `file` or `data` must be supplied. The data must be compatible with the schema to which it is uploaded. 
Throws the `PermissionDeniedException` if the user does not have at least _editor_ permissions for this schema.
Throws the `NoSuchSchemaException` if the schema is not found on the server.

| parameter | type | description                                        | required | default               |
|-----------|------|----------------------------------------------------|----------|-----------------------|
| `table`   | str  | the name of a table                                | True     |                       |
| `schema`  | str  | the name of a schema                               | False    | client.default_schema |
| `file`    | str  | the location of a `csv` file with data             | False    | None                  |
| `data`    | list | data as a list of dictionaries or pandas DataFrame | False    | None                  |

##### examples
```python
# Save an edited table with Resources data from a CSV file to the Resources table
client.save_schema(table='Resources', file='Resources-edited.csv')

# Save an edited table with Resources data from memory to the Resources table
resources: pandas.DataFrame = ...
client.save_schema(table='Resources', data=resources)
```

### upload_file
```python
async def upload_file(self, file_path: str | pathlib.Path, schema: str = None):
    ...
```
Imports table data and/or metadata to a schema from a file on the disk.
This method supports `zip`, `xlsx`, and `csv` files.
When uploading multiple `csv` files it is recommended to archive them into a `zip` file first and upload that file using this method.
Throws the `PermissionDeniedException` if the user does not have at least _editor_ permissions for this schema.
Throws the `NoSuchSchemaException` if the schema is not found on the server.

| parameter   | type | description          | required | default               |
|-------------|------|----------------------|----------|-----------------------|
| `file_path` | str  | the name of a table  | True     |                       |
| `schema`    | str  | the name of a schema | False    | client.default_schema |

##### examples
```python
# Upload a file containing Resources data to a schema
await client.upload_file(file_path='data/Resources.csv')

# Upload a file containing members information to a schema
await client.upload_file(file_path='molgenis_members.csv', schema='MySchema')

# Upload a zipped file containing multiple tables to a schema
await client.upload_file(file_path='schema data.zip', schema='MySchema')
```


### delete_records
```python
def delete_records(self, 
                   table: str, 
                   schema: str = None, 
                   file: str = None, 
                   data: list | pandas.DataFrame = None):
    ...
```
Deletes data records from a table.
The records that are to be deleted are specified in either a CSV file, or in a list of primary key values, 
or a pandas DataFrame representing the table on the schema.
Throws the `PermissionDeniedException` if the user does not have at least _editor_ permissions.
Throws the `NoSuchSchemaException` if the schema is not found on the server.

[//]: # (&#40;In order to delete records from a table the data must specify the row values with principal keys)

| parameter | type | description                                        | required | default               |
|-----------|------|----------------------------------------------------|----------|-----------------------|
| `table`   | str  | the name of a table                                | True     |                       |
| `schema`  | str  | the name of a schema                               | False    | client.default_schema |
| `file`    | str  | the location of a `csv` file with data             | False    | None                  |
| `data`    | list | data as a list of dictionaries or pandas DataFrame | False    | None                  |

##### examples
```python
# Delete resources from a list of ids
resources = [{'name': 'Resource 1', 'name': 'Resource 2'}]
client.delete_records(schema='MySchema', table='Resources', data=resources)

# Delete resources from pandas DataFrame
resources_df = pandas.DataFrame(data=resources)
client.delete_records(schema='MySchema', table='Resources', data=resources_df)

# Delete resources from entries in a CSV file
client.delete_records(schema='MySchema', table='Resources', file='Resources-to-delete.csv')
```

### truncate
```python
client.truncate(table='My table', schema='My Schema')
```
Truncates the table and removes all its contents.
This will fail if entries in the table are referenced from other tables.

Throws the `ReferenceException` if entries in the table are referenced in other tables.

### create_schema
```python
async def create_schema(self, 
                        name: str = None,
                        description: str = None,
                        template: str = None,
                        include_demo_data: bool = False):
    ...
```
Creates a new schema on the server.
If no template is selected, an empty schema is created.
Only users with _admin_ privileges are able to perform this action.

| parameter           | type | description                   | required | default |
|---------------------|------|-------------------------------|----------|---------|
| `name`              | str  | the name of the new schema    | True     |         |
| `description`       | str  | description of the new schema | False    | None    |
| `template`          | str  | the template for this schema  | False    | None    |
| `include_demo_data` | bool | whether to include demo data  | False    | False   |

##### examples
```python
# Create a schema without specifying a template
await client.create_schema(name='New schema', description='My new schema')

# Create a new catalogue schema with demo data
await client.create_schema(name='New catalogue', description='My new catalogue',
                           template='DATA_CATALOGUE', include_demo_data=True)
```


### delete_schema
```python
async def delete_schema(self, name: str = None):
    ...
```

Deletes a schema from the server. 
Only users with _admin_ privileges are able to perform this action.
Throws the `PermissionDeniedException` if execution of the method is attempted without _admin_ privileges.
Throws the `NoSuchSchemaException` if the schema is not found on the server.


| parameter | type | description                      | required | default |
|-----------|------|----------------------------------|----------|---------|
| `name`    | str  | the name of the schema to delete | True     |         | 

##### examples
```python
# Delete a schema
await client.delete_schema('MySchema')
```

### update_schema
```python
def update_schema(self, name: str = None, description: str = None):
    ...
```
Updates a schema's description.
Only users with _admin_ privileges are able to perform this action.
Throws the `PermissionDeniedException` if execution of the method is attempted without _admin_ privileges.
Throws the `NoSuchSchemaException` if the schema is not found on the server.

| parameter     | type | description             | required | default |
|---------------|------|-------------------------|----------|---------|
| `name`        | str  | the name of the schema  | True     |         |
| `description` | str  | the updated description | True     |         |

##### examples
```python
# Update the description of a schema
client.update_schema(name='MySchema', description='The new description')
```


### recreate_schema
```python
async def recreate_schema(self, 
                          name: str = None,
                          description: str = None,
                          template: str = None,
                          include_demo_data: bool = None):
    ...
```
Recreates a schema on the EMX2 server by deleting and subsequently creating it without data on the EMX2 server.

If no template is selected, an empty schema is created.
Only users with _admin_ privileges are able to perform this action.
Throws the `PermissionDeniedException` if execution of the method is attempted without _admin_ privileges.
Throws the `NoSuchSchemaException` if the schema is not found on the server.

| parameter           | type | description                   | required | default |
|---------------------|------|-------------------------------|----------|---------|
| `name`              | str  | the name of the schema        | True     |         |
| `description`       | str  | new description of the schema | False    | None    |
| `template`          | str  | the template for this schema  | False    | None    |
| `include_demo_data` | bool | whether to include demo data  | False    | False   |

##### examples
```python
# Recreate a schema, replacing its content with a template and demo data
await client.recreate_schema(name='MySchema', description='An ',
                             template='DATA_CATALOGUE', include_demo_data=True)

```

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
schema_data = {'name': 'My schema', 'description': 'This is my schema!',
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
schema.get_tables(by='inheritName', value='Resources')
```
Gets the table of which an attribute matches a particular value.
It is possible to filter the tables by multiple conditions, the attributes and values are then supplied as lists. 
The length of the `by` argument must then match the length of the `value` argument.
Returns an empty list if no matching column can be found.
