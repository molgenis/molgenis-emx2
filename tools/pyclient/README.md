# README

The Molgenis EMX2 Pyclient is a Python package developed to be used for data management on Molgenis EMX2 servers.
A detailed overview of the capabilities is presented in the [MOLGENIS documentation](https://molgenis.github.io/molgenis-emx2/#/molgenis/use_usingpyclient).

## Installation

The releases of the package are hosted at [PyPI](https://pypi.org/project/molgenis-emx2-pyclient/).
The recommended way to install the latest version is through `pip`.

```console
pip install molgenis-emx2-pyclient
```

## Changelog

Releases of the Molgenis EMX2 Pyclient follow the release number of the accompanying release of the Molgenis EMX2 software.
Therefore, releases of the Pyclient are less frequent than those of EMX2 and the latest version of the Pyclient may differ
from the latest version of Molgenis EMX2.



### 13.77.1

- Improved error handling in 'columns' filter in `get` method
- Fixed asynchronous API calls in `export`
- Fixed issue with 'equals' filter for references in `get` method
- Fixed `greater_than` and `smaller_than` filters for LONG type values in `get` method
- Renamed `__prepare_unequal_filter` to `prepare_not_equals_filter` and added NotImplementedError for certain data types
- Fixed issue with incorrect column names in `get_graphql`
- Removed default value None for 'name' in `create_schema` as schema name cannot be null
- Fixed parameter `include_demo_data` in `recreate_schema` to default to `False` instead of `None`
- Improved error handling when creating schema with non-existent template

### 13.75.1

- Fixed: issue when running `get_schema_metadata` on a schema with insufficient permissions to see members
- Added: methods `get_schema_settings`
- Added: methods `get_schema_members`
- Added: methods `get_schema_roles`

### 13.55.4

Added: method `save_table`. Replaces `save_schema` which is now deprecated.

### 13.50.0

Fixed: issue with loading table data from `get_graphql` method

### 13.40.4

Fixed: no longer convert NA-like strings to pandas' NaN, only do this for missing values (i.e. empty strings).

### 11.61.2

Fixed: problem where the schema metadata could not be updated with `upload_file` and a `molgenis.csv` file.

### 11.57.0

- Added: feature 'truncate' to remove all entries from a table
- Added: option to filter results of `get` method by columns
- Added: method `get_graphql` implements the GraphQL API
- Improved: added additional parsing for data returned from the CSV API to pandas DataFrame in `get` method
- Fixed: log level was set to `DEBUG` without possibility to change this.
The user can now set the log level again at their preferred level

### 11.47.1

Fixed: updated GraphQL queries to be in line with EMX2 database metadata

### 11.23.0

Added: an optional `job` argument to the `Client` initialization,
allowing the Pyclient to run asynchronous methods within a job in EMX2."

### 11.11.1

Added: option to specify filename in method `export` and to return the exported data in a function

### 11.8.0

Breaking: introduced asynchronous methods. Users need to explicitly address the asynchronous methods

### 10.109.3

Added: the ability to use the Pyclient on an EMX2 instance running on a local machine.

### 10.98.0

Added: the option to filter the results obtained from the `get` method based on columns

### 10.92.1

Added: the method `upload_file` which allows the direct upload of files of multiple types to the API

### 10.72.1

Fixed: issue where `NA` values were not properly read in or returned by the API

### 10.48.9

Fixed: issue in managing data of a table where the table _id_ differs from the table _name_

### 10.48.0

Added: option to save data from the `get` method as a pandas DataFrame

### 10.47.2

Fixed: exceptions for metadata classes Column and Schema

### 10.47.0

Added: metadata classes for Column, Table, Schema

### 10.13.1

Added: methods `create_schema`, `delete_schema`, `update_schema`, `recreate_schema`, `get_schema_metadata`

## How to use

Within your Python project import the class Client and instantiate it as a context manager.
Operations and queries can then be executed from within the context.

```py
from molgenis_emx2_pyclient import Client

username = 'username'
password = '...'

with Client('https://example.molgeniscloud.org') as client:
    client.signin(username, password)

    # Retrieve signin information
    print(client.status)
    """ Output:
    Host: https://example.molgeniscloud.org
    Status: Signed in
    Schemas:
        CatalogueOntologies
        catalogue
        ExampleSchema
        ...
    Version: v10.10.1
    """

    # Retrieve data from a table on a schema
    data = client.get(schema='ExampleSchema', table='Cohorts')

    # Create a new schema on the server
    client.create_schema(name='New Schema')

    # Delete a schema from the server
    client.delete_schema(name='New Schema')

```

Instead of signing in with a username and password the client can also be used while authorized
by a (temporary) token that is generated on the server.
See the [MOLGENIS documentation for generating tokens](https://molgenis.github.io/molgenis-emx2/#/molgenis/use_tokens)

```py
from molgenis_emx2_pyclient import Client

token = '...'

with Client('https://example.molgeniscloud.org', token=token) as client:

    # Retrieve signin information
    print(client.status)
    """ Output:
    Host: https://example.molgeniscloud.org
    User: token
    Status: session-less
    Schemas:
        CatalogueOntologies
        catalogue
        ExampleSchema
        ...
    Version: v10.32.1
    """

    ...
    ...

```

## Development

The Pyclient requires a Python installation with version 3.10 or higher.
Clone the `molgenis-emx2` repository from GitHub

```console
git clone git@github.com:molgenis/molgenis-emx2.git
```

Change the working directory to `.../tools/pyclient`

### Create a virtual Python environment

On macOS:

```console
python -m venv venv
```

On Linux:

```console
python3.10 -m venv venv
```

On Windows:

```console
py -3.10 venv venv
```

### Activate the virtual environment

On macOS and Linux:

```console
source venv/bin/activate
```

On Windows:

```console
.venv\Scripts\activate.bat
```

### Install the script dependencies

```console
pip install -r requirements.txt
```

## Build

Before building the source, the package `build` needs to be installed.

```console
(venv) $ pip install build

(venv) $ python -m build

(venv) $ pip install dist/molgenis_emx2_pyclient*.whl
```

## Test

Tests for the Pyclient have been created with the Pytest framework.
In order to test the functionality of the Pyclient copy the '.env-example' file to '.env' and modify the parameters for the server you want to test on.

Then execute the following to run all the tests

```console
(venv) molgenis-emx2/tools/pyclient$ pytest
```
